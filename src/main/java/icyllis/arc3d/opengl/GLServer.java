/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static icyllis.arc3d.engine.Engine.*;
import static icyllis.arc3d.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.memPutInt;

public final class GLServer extends Server {

    private final GLCaps mCaps;

    private final GLCommandBuffer mMainCmdBuffer;

    private final GLPipelineStateCache mProgramCache;
    private final GLResourceProvider mResourceProvider;

    private final CpuBufferCache mCpuBufferCache;

    private final BufferAllocPool mVertexPool;
    private final BufferAllocPool mInstancePool;

    // unique ptr
    private GLOpsRenderPass mCachedOpsRenderPass;

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();
    private final LongArrayFIFOQueue mFinishedFences = new LongArrayFIFOQueue();

    private boolean mNeedsFlush;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        mMainCmdBuffer = new GLCommandBuffer(this);
        mProgramCache = new GLPipelineStateCache(this, 256);
        mResourceProvider = new GLResourceProvider(this);
        mCpuBufferCache = new CpuBufferCache(6);
        mVertexPool = BufferAllocPool.makeVertexPool(this);
        mInstancePool = BufferAllocPool.makeInstancePool(this);
    }

    /**
     * Create a GLServer with OpenGL context current in the current thread.
     *
     * @param context the owner context
     * @param options the context options
     * @return the server or null if failed to create
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static GLServer make(DirectContext context, ContextOptions options) {
        GLCapabilities capabilities;
        try {
            // checks may be disabled
            capabilities = Objects.requireNonNullElseGet(GL.getCapabilities(), GL::createCapabilities);
        } catch (Exception e) {
            // checks may be enabled
            capabilities = GL.createCapabilities();
        }
        if (capabilities == null) {
            return null;
        }
        try {
            return new GLServer(context, new GLCaps(options, capabilities));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GLCaps getCaps() {
        return mCaps;
    }

    @Override
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        mVertexPool.reset();
        mInstancePool.reset();
        mCpuBufferCache.releaseAll();

        if (cleanup) {
            mProgramCache.destroy();
            mResourceProvider.destroy();
        } else {
            mProgramCache.discard();
            mResourceProvider.discard();
        }

        callAllFinishedCallbacks(cleanup);
    }

    public GLCommandBuffer currentCommandBuffer() {
        return mMainCmdBuffer;
    }

    @Override
    public GLPipelineStateCache getPipelineBuilder() {
        return mProgramCache;
    }

    public GLResourceProvider getResourceProvider() {
        return mResourceProvider;
    }

    public CpuBufferCache getCpuBufferCache() {
        return mCpuBufferCache;
    }

    @Override
    public BufferAllocPool getVertexPool() {
        return mVertexPool;
    }

    @Override
    public BufferAllocPool getInstancePool() {
        return mInstancePool;
    }

    @Override
    protected void onResetContext(int resetBits) {
        currentCommandBuffer().resetStates(resetBits);

        // we assume these values
        if ((resetBits & GLBackendState.kPixelStore) != 0) {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        }

        if ((resetBits & GLBackendState.kRaster) != 0) {
            glDisable(GL_LINE_SMOOTH);
            glDisable(GL_POLYGON_SMOOTH);

            glDisable(GL_DITHER);
            glEnable(GL_MULTISAMPLE);
        }

        if ((resetBits & GLBackendState.kBlend) != 0) {
            glDisable(GL_COLOR_LOGIC_OP);
        }

        if ((resetBits & GLBackendState.kMisc) != 0) {
            // we don't use the z-buffer at all
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            glDisable(GL_POLYGON_OFFSET_FILL);

            // We don't use face culling.
            glDisable(GL_CULL_FACE);
            // We do use separate stencil. Our algorithms don't care which face is front vs. back so
            // just set this to the default for self-consistency.
            glFrontFace(GL_CCW);

            // we only ever use lines in hairline mode
            glLineWidth(1);
            glPointSize(1);
            glDisable(GL_PROGRAM_POINT_SIZE);
        }
    }

    @Nullable
    @Override
    protected Texture onCreateTexture(int width, int height,
                                      BackendFormat format,
                                      int levelCount,
                                      int sampleCount,
                                      int surfaceFlags) {
        assert (levelCount > 0 && sampleCount > 0);
        // We don't support protected textures in OpenGL.
        if ((surfaceFlags & SurfaceFlags.Protected) != 0) {
            return null;
        }
        if (format.isExternal()) {
            return null;
        }
        int glFormat = format.getGLFormat();
        int texture = createTexture(width, height, glFormat, levelCount);
        if (texture == 0) {
            return null;
        }
        Function<GLTexture, GLRenderTarget> target = null;
        if ((surfaceFlags & SurfaceFlags.Renderable) != 0) {
            target = createRenderTargetObjects(
                    texture,
                    width, height,
                    glFormat,
                    sampleCount);
            if (target == null) {
                glDeleteTextures(texture);
                return null;
            }
        }
        final GLTextureInfo info = new GLTextureInfo();
        info.mTexture = texture;
        info.mFormat = format.getGLFormat();
        info.mLevelCount = levelCount;
        return new GLTexture(this,
                width, height,
                info,
                format,
                (surfaceFlags & SurfaceFlags.Budgeted) != 0,
                target);
    }

    @Nullable
    @Override
    protected Texture onWrapRenderableBackendTexture(BackendTexture texture,
                                                     int sampleCount,
                                                     boolean ownership) {
        if (texture.isProtected()) {
            // Not supported in GL backend at this time.
            return null;
        }
        final GLTextureInfo info = new GLTextureInfo();
        if (!texture.getGLTextureInfo(info) || info.mTexture == 0 || info.mFormat == 0) {
            return null;
        }
        /*if (info.mTarget != GL_TEXTURE_2D) {
            return null;
        }*/
        int format = info.mFormat;
        if (!glFormatIsSupported(format)) {
            return null;
        }
        assert mCaps.isFormatRenderable(format, sampleCount);
        assert mCaps.isFormatTexturable(format);

        sampleCount = mCaps.getRenderTargetSampleCount(sampleCount, format);
        assert sampleCount > 0;

        var objects = createRenderTargetObjects(info.mTexture,
                texture.getWidth(), texture.getHeight(),
                format,
                sampleCount);
        if (objects != null) {

            //TODO create wrapped texture
            /*return new GLRenderTarget(this, texture.getWidth(), texture.getHeight(),
                format, sampleCount, framebuffer, msaaFramebuffer,
                texture, msaaColorBuffer, ownership);*/
        }

        return null;
    }

    @Override
    protected boolean onWritePixels(Texture texture,
                                    int x, int y,
                                    int width, int height,
                                    int dstColorType,
                                    int srcColorType,
                                    int rowBytes, long pixels) {
        assert (!texture.isExternal());
        assert (!texture.getBackendFormat().isCompressed());
        GLTexture glTexture = (GLTexture) texture;
        int glFormat = glTexture.getFormat();
        assert (mCaps.isFormatTexturable(glFormat));

        GLTextureParameters parameters = glTexture.getParameters();
        if (parameters.mBaseMipMapLevel != 0) {
            glTextureParameteri(glTexture.getTextureID(), GL_TEXTURE_BASE_LEVEL, 0);
            parameters.mBaseMipMapLevel = 0;
        }
        int maxLevel = glTexture.getMaxMipmapLevel();
        if (parameters.mMaxMipmapLevel != maxLevel) {
            glTextureParameteri(glTexture.getTextureID(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            parameters.mMaxMipmapLevel = maxLevel;
        }

        int srcFormat = mCaps.getPixelsExternalFormat(glFormat, dstColorType, srcColorType, /*write*/true);
        if (srcFormat == 0) {
            return false;
        }
        int srcType = mCaps.getPixelsExternalType(glFormat, dstColorType, srcColorType);
        if (srcType == 0) {
            return false;
        }

        assert (x >= 0 && y >= 0 && width > 0 && height > 0);
        assert (pixels != 0);
        int bpp = ColorType.bytesPerPixel(srcColorType);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        boolean restoreRowLength = false;

        int trimRowBytes = width * bpp;
        if (rowBytes != trimRowBytes) {
            int rowLength = rowBytes / bpp;
            glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
            restoreRowLength = true;
        }

        glTextureSubImage2D(glTexture.getTextureID(), 0,
                x, y, width, height, srcFormat, srcType, pixels);

        if (restoreRowLength) {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }

        return true;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView,
                                               Rect2i contentBounds,
                                               byte colorOps,
                                               byte stencilOps,
                                               float[] clearColor,
                                               Set<TextureProxy> sampledTextures,
                                               int pipelineFlags) {
        mStats.incRenderPasses();
        if (mCachedOpsRenderPass == null) {
            mCachedOpsRenderPass = new GLOpsRenderPass(this);
        }
        return mCachedOpsRenderPass.set(writeView.getProxy().peekRenderTarget(),
                contentBounds,
                writeView.getOrigin(),
                colorOps,
                stencilOps,
                clearColor);
    }

    public GLCommandBuffer beginRenderPass(GLRenderTarget renderTarget,
                                           byte colorOps,
                                           byte stencilOps,
                                           float[] clearColor) {
        handleDirtyContext();

        GLCommandBuffer cmdBuffer = currentCommandBuffer();

        boolean colorLoadClear = LoadStoreOps.loadOp(colorOps) == LoadOp.Clear;
        boolean stencilLoadClear = LoadStoreOps.loadOp(stencilOps) == LoadOp.Clear;
        if (colorLoadClear || stencilLoadClear) {
            int framebuffer = renderTarget.getRenderFramebuffer();
            cmdBuffer.flushScissorTest(false);
            if (colorLoadClear) {
                cmdBuffer.flushColorWrite(true);
                glClearNamedFramebufferfv(framebuffer,
                        GL_COLOR,
                        0,
                        clearColor);
            }
            if (stencilLoadClear) {
                glStencilMask(0xFFFFFFFF); // stencil will be flushed later
                glClearNamedFramebufferfi(framebuffer,
                        GL_DEPTH_STENCIL,
                        0,
                        1.0f, 0);
            }
        }
        cmdBuffer.flushRenderTarget(renderTarget);

        return cmdBuffer;
    }

    public void endRenderPass(GLRenderTarget renderTarget,
                              byte colorOps,
                              byte stencilOps) {
        handleDirtyContext();

        boolean colorStoreDiscard = LoadStoreOps.storeOp(colorOps) == StoreOp.DontCare;
        boolean stencilStoreDiscard = LoadStoreOps.storeOp(stencilOps) == StoreOp.DontCare;
        if (colorStoreDiscard || stencilStoreDiscard) {
            int framebuffer = renderTarget.getRenderFramebuffer();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                final long pAttachments = stack.nmalloc(4, 8);
                int numAttachments = 0;
                if (colorStoreDiscard) {
                    int attachment = renderTarget.getRenderFramebuffer() == 0
                            ? GL_COLOR
                            : GL_COLOR_ATTACHMENT0;
                    memPutInt(pAttachments, attachment);
                    numAttachments++;
                }
                if (stencilStoreDiscard) {
                    int attachment = renderTarget.getRenderFramebuffer() == 0
                            ? GL_STENCIL
                            : GL_STENCIL_ATTACHMENT;
                    memPutInt(pAttachments + (numAttachments << 2), attachment);
                    numAttachments++;
                }
                nglInvalidateNamedFramebufferData(framebuffer, numAttachments, pAttachments);
            }
        }
    }

    @Override
    protected void onResolveRenderTarget(RenderTarget renderTarget,
                                         int resolveLeft, int resolveTop,
                                         int resolveRight, int resolveBottom) {
        GLRenderTarget glRenderTarget = (GLRenderTarget) renderTarget;

        int framebuffer = glRenderTarget.getRenderFramebuffer();
        int resolveFramebuffer = glRenderTarget.getResolveFramebuffer();

        // We should always have something to resolve
        assert (framebuffer != 0 && framebuffer != resolveFramebuffer);

        // BlitFramebuffer respects the scissor, so disable it.
        currentCommandBuffer().flushScissorTest(false);
        glBlitNamedFramebuffer(framebuffer, resolveFramebuffer, // MSAA to single
                resolveLeft, resolveTop, resolveRight, resolveBottom, // src rect
                resolveLeft, resolveTop, resolveRight, resolveBottom, // dst rect
                GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    private void flush(boolean forceFlush) {
        if (mNeedsFlush || forceFlush) {
            glFlush();
            mNeedsFlush = false;
        }
    }

    @Override
    public long insertFence() {
        long fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        mNeedsFlush = true;
        return fence;
    }

    @Override
    public boolean checkFence(long fence) {
        int result = glClientWaitSync(fence, 0, 0L);
        return (result == GL_CONDITION_SATISFIED || result == GL_ALREADY_SIGNALED);
    }

    @Override
    public void deleteFence(long fence) {
        glDeleteSync(fence);
    }

    @Override
    public void addFinishedCallback(FlushInfo.FinishedCallback callback) {
        mFinishedCallbacks.addLast(callback);
        mFinishedFences.enqueue(insertFence());
        assert (mFinishedCallbacks.size() == mFinishedFences.size());
    }

    @Override
    public void checkFinishedCallbacks() {
        // Bail after the first unfinished sync since we expect they signal in the order inserted.
        while (!mFinishedCallbacks.isEmpty() && checkFence(mFinishedFences.firstLong())) {
            // While we are processing a proc we need to make sure to remove it from the callback list
            // before calling it. This is because the client could trigger a call (e.g. calling
            // flushAndSubmit(/*sync=*/true)) that has us process the finished callbacks. We also must
            // process deleting the fence before a client may abandon the context.
            deleteFence(mFinishedFences.dequeueLong());
            mFinishedCallbacks.removeFirst().onFinished();
        }
        assert (mFinishedCallbacks.size() == mFinishedFences.size());
    }

    private void callAllFinishedCallbacks(boolean cleanup) {
        while (!mFinishedCallbacks.isEmpty()) {
            // While we are processing a proc we need to make sure to remove it from the callback list
            // before calling it. This is because the client could trigger a call (e.g. calling
            // flushAndSubmit(/*sync=*/true)) that has us process the finished callbacks. We also must
            // process deleting the fence before a client may abandon the context.
            if (cleanup) {
                deleteFence(mFinishedFences.dequeueLong());
            }
            mFinishedCallbacks.removeFirst().onFinished();
        }
        if (!cleanup) {
            mFinishedFences.clear();
        } else {
            assert (mFinishedFences.isEmpty());
        }
    }

    @Override
    public void waitForQueue() {
        glFinish();
    }

    private int createTexture(int width, int height, int format, int levels) {
        assert (glFormatIsSupported(format));
        assert (!glFormatIsCompressed(format));

        int internalFormat = mCaps.getTextureInternalFormat(format);
        if (internalFormat == 0) {
            return 0;
        }

        assert (mCaps.isFormatTexturable(format));
        assert (mCaps.isTextureStorageCompatible(format));
        int texture = glCreateTextures(GL_TEXTURE_2D);
        if (texture == 0) {
            return 0;
        }

        if (mCaps.skipErrorChecks()) {
            glTextureStorage2D(texture, levels, internalFormat, width, height);
        } else {
            glClearErrors();
            glTextureStorage2D(texture, levels, internalFormat, width, height);
            if (glGetError() != GL_NO_ERROR) {
                glDeleteTextures(texture);
                return 0;
            }
        }

        return texture;
    }

    @Nullable
    private Function<GLTexture, GLRenderTarget> createRenderTargetObjects(int texture,
                                                                          int width, int height,
                                                                          int format,
                                                                          int samples) {
        assert texture != 0;
        assert glFormatIsSupported(format);
        assert !glFormatIsCompressed(format);

        // There's an NVIDIA driver bug that creating framebuffer via DSA with attachments of
        // different dimensions will report GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT.
        // The workaround is to use traditional glGen* and glBind* (validate).
        final int framebuffer = glGenFramebuffers();
        if (framebuffer == 0) {
            return null;
        }
        // Below here we may bind the FBO.
        // Create the state vector of the framebuffer.
        currentCommandBuffer().bindFramebuffer(framebuffer);

        final int msaaFramebuffer;
        final GLAttachment msaaColorBuffer;
        // If we are using multisampling we will create two FBOs. We render to one and then resolve to
        // the texture bound to the other. The exception is the IMG multisample extension. With this
        // extension the texture is multisampled when rendered to and then auto-resolves it when it is
        // rendered from.
        if (samples <= 1) {
            msaaFramebuffer = 0;
            msaaColorBuffer = null;
        } else {
            msaaFramebuffer = glGenFramebuffers();
            if (msaaFramebuffer == 0) {
                glDeleteFramebuffers(framebuffer);
                return null;
            }
            // Create the state vector of the framebuffer.
            currentCommandBuffer().bindFramebuffer(msaaFramebuffer);

            msaaColorBuffer = GLAttachment.makeMSAA(this,
                    width, height,
                    samples,
                    format);
            if (msaaColorBuffer == null) {
                glDeleteFramebuffers(framebuffer);
                glDeleteFramebuffers(msaaFramebuffer);
                return null;
            }

            glNamedFramebufferRenderbuffer(msaaFramebuffer,
                    GL_COLOR_ATTACHMENT0,
                    GL_RENDERBUFFER,
                    msaaColorBuffer.getRenderbufferID());
            if (!mCaps.skipErrorChecks()) {
                int status = glCheckNamedFramebufferStatus(msaaFramebuffer, GL_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    glDeleteFramebuffers(framebuffer);
                    glDeleteFramebuffers(msaaFramebuffer);
                    msaaColorBuffer.unref();
                    return null;
                }
            }
        }

        glNamedFramebufferTexture(framebuffer,
                GL_COLOR_ATTACHMENT0,
                texture,
                0);
        if (!mCaps.skipErrorChecks()) {
            int status = glCheckNamedFramebufferStatus(framebuffer, GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                glDeleteFramebuffers(framebuffer);
                glDeleteFramebuffers(msaaFramebuffer);
                if (msaaColorBuffer != null) {
                    msaaColorBuffer.unref();
                }
                return null;
            }
        }

        return glTexture -> new GLRenderTarget(this,
                glTexture.getWidth(), glTexture.getHeight(),
                glTexture.getFormat(), samples,
                framebuffer,
                msaaFramebuffer,
                glTexture,
                msaaColorBuffer);
    }
}
