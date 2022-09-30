/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.Rect2i;
import icyllis.akashigi.engine.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.util.function.Function;

import static icyllis.akashigi.engine.Engine.*;
import static icyllis.akashigi.opengl.GLCore.*;
import static org.lwjgl.system.MemoryUtil.memPutInt;

public final class GLServer extends Server {

    private final GLCaps mCaps;

    private final GLPipelineStateCache mProgramCache;

    private GLCommandBuffer mMainCmdBuffer;

    private final CpuBufferCache mCpuBufferCache;

    private final GLBufferAllocPool mVertexPool;
    private final GLBufferAllocPool mInstancePool;

    // unique ptr
    private GLOpsRenderPass mCachedOpsRenderPass;
    private GLResourceProvider mResourceProvider;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        mMainCmdBuffer = new GLCommandBuffer(this);
        mProgramCache = new GLPipelineStateCache(this, 256);
        mCpuBufferCache = new CpuBufferCache(6);
        mVertexPool = GLBufferAllocPool.makeVertex(this, mCpuBufferCache);
        mInstancePool = GLBufferAllocPool.makeInstance(this, mCpuBufferCache);
        mResourceProvider = new GLResourceProvider(this);
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
            capabilities = GL.getCapabilities();
            if (capabilities == null) {
                // checks may be disabled
                capabilities = GL.createCapabilities();
            }
        } catch (IllegalStateException e) {
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
    }

    @Override
    public GLPipelineStateCache getPipelineBuilder() {
        return mProgramCache;
    }

    @Override
    public BufferAllocPool getVertexPool() {
        return mVertexPool;
    }

    @Override
    public BufferAllocPool getInstancePool() {
        return mInstancePool;
    }

    public GLCommandBuffer currentCommandBuffer() {
        return mMainCmdBuffer;
    }

    public GLResourceProvider getResourceProvider() {
        return mResourceProvider;
    }

    @Override
    protected void onResetContext(int resetBits) {
        currentCommandBuffer().resetStates(resetBits);

        // we assume these values
        if ((resetBits & GLBackendState_PixelStore) != 0) {
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        }

        if ((resetBits & GLBackendState_Raster) != 0) {
            glDisable(GL_LINE_SMOOTH);
            glDisable(GL_POLYGON_SMOOTH);

            glDisable(GL_DITHER);
            glEnable(GL_MULTISAMPLE);
        }

        if ((resetBits & GLBackendState_Blend) != 0) {
            glDisable(GL_COLOR_LOGIC_OP);
        }

        if ((resetBits & GLBackendState_Misc) != 0) {
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
        if ((surfaceFlags & SurfaceFlag_Protected) != 0) {
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
        if ((surfaceFlags & SurfaceFlag_Renderable) != 0) {
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
        info.mFormat = format.getGLFormatEnum();
        info.mLevelCount = levelCount;
        return new GLTexture(this,
                width, height,
                info,
                format,
                (surfaceFlags & SurfaceFlag_Budgeted) != 0,
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
        int format = glFormatFromEnum(info.mFormat);
        if (format == GLTypes.FORMAT_UNKNOWN) {
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
        int bpp = colorTypeBytesPerPixel(srcColorType);

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
    public OpsRenderPass getOpsRenderPass(RenderTarget renderTarget,
                                          boolean useStencil,
                                          int origin,
                                          Rect2i bounds,
                                          int colorLoadOp, int colorStoreOp,
                                          int stencilLoadOp, int stencilStoreOp,
                                          float[] clearColor) {
        mStats.incRenderPasses();
        if (mCachedOpsRenderPass == null) {
            mCachedOpsRenderPass = new GLOpsRenderPass(this);
        }
        return mCachedOpsRenderPass.set(renderTarget,
                bounds,
                origin,
                colorLoadOp, colorStoreOp,
                stencilLoadOp, stencilStoreOp,
                clearColor);
    }

    public GLCommandBuffer beginRenderPass(GLRenderTarget renderTarget,
                                           int colorLoadOp, int stencilLoadOp,
                                           float[] clearColor) {
        handleDirtyContext();

        GLCommandBuffer cmdBuffer = currentCommandBuffer();

        if (colorLoadOp == LoadOp_Clear || stencilLoadOp == LoadOp_Clear) {
            int framebuffer = renderTarget.getFramebuffer();
            cmdBuffer.flushScissorTest(false);
            if (colorLoadOp == LoadOp_Clear) {
                cmdBuffer.flushColorWrite(true);
                glClearNamedFramebufferfv(framebuffer,
                        GL_COLOR,
                        0,
                        clearColor);
            }
            if (stencilLoadOp == LoadOp_Clear) {
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
                              int colorStoreOp, int stencilStoreOp) {
        handleDirtyContext();

        if (colorStoreOp == StoreOp_Discard || stencilStoreOp == StoreOp_Discard) {
            int framebuffer = renderTarget.getFramebuffer();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                final long pAttachments = stack.nmalloc(4, 8);
                int numAttachments = 0;
                if (colorStoreOp == StoreOp_Discard) {
                    int attachment = renderTarget.getFramebuffer() == 0
                            ? GL_COLOR
                            : GL_COLOR_ATTACHMENT0;
                    memPutInt(pAttachments, attachment);
                    numAttachments++;
                }
                if (stencilStoreOp == StoreOp_Discard) {
                    int attachment = renderTarget.getFramebuffer() == 0
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

        int framebuffer = glRenderTarget.getFramebuffer();
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

    private int createTexture(int width, int height, int format, int levels) {
        assert (format != GLTypes.FORMAT_UNKNOWN);
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
        assert format != GLTypes.FORMAT_UNKNOWN;
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
                    msaaColorBuffer.getRenderbuffer());
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
