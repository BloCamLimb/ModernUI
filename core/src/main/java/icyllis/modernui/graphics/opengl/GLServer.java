/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.engine.*;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.*;
import java.util.function.Function;

import static icyllis.modernui.graphics.opengl.GLCore.*;

public final class GLServer extends Server {

    private final GLCaps mCaps;

    private final GLCommandBuffer mMainCmdBuffer;

    //private final GLPipelineStateCache mProgramCache;
    private final GLResourceProvider mResourceProvider;

    private final CpuBufferCache mCpuBufferCache;

    private final BufferAllocPool mVertexPool;
    private final BufferAllocPool mInstancePool;

    // unique ptr
    //private GLOpsRenderPass mCachedOpsRenderPass;

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();
    private final LongArrayFIFOQueue mFinishedFences = new LongArrayFIFOQueue();

    private boolean mNeedsFlush;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        mMainCmdBuffer = new GLCommandBuffer(this);
        //mProgramCache = new GLPipelineStateCache(this, 256);
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
    public ThreadSafePipelineBuilder getPipelineBuilder() {
        return null;
    }

    @Override
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        mVertexPool.reset();
        mInstancePool.reset();
        mCpuBufferCache.releaseAll();

        if (cleanup) {
            //mProgramCache.destroy();
            mResourceProvider.destroy();
        } else {
            //mProgramCache.discard();
            mResourceProvider.discard();
        }

        //callAllFinishedCallbacks(cleanup);
    }

    public GLCommandBuffer currentCommandBuffer() {
        return mMainCmdBuffer;
    }

    public GLResourceProvider getResourceProvider() {
        return mResourceProvider;
    }

    public CpuBufferCache getCpuBufferCache() {
        return mCpuBufferCache;
    }

    @Override
    public BufferAllocPool getVertexPool() {
        return null;
    }

    @Override
    public BufferAllocPool getInstancePool() {
        return null;
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
        if ((surfaceFlags & Surface.FLAG_PROTECTED) != 0) {
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
        if ((surfaceFlags & Surface.FLAG_RENDERABLE) != 0) {
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
        info.texture = texture;
        info.format = format.getGLFormat();
        info.levelCount = levelCount;
        return new GLTexture(this,
                width, height,
                info,
                format,
                (surfaceFlags & Surface.FLAG_BUDGETED) != 0,
                target);
    }

    @icyllis.modernui.annotation.Nullable
    @Override
    protected Texture onWrapRenderableBackendTexture(BackendTexture texture, int sampleCount, boolean ownership) {
        return null;
    }

    @Override
    protected boolean onWritePixels(Texture texture, int x, int y, int width, int height, int dstColorType, int srcColorType, int rowBytes, long pixels) {
        return false;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView, Rect contentBounds, byte colorOps,
                                               byte stencilOps, float[] clearColor, Set<TextureProxy> sampledTextures
            , int pipelineFlags) {
        return null;
    }

    @Override
    protected void onResolveRenderTarget(RenderTarget renderTarget, int resolveLeft, int resolveTop, int resolveRight
            , int resolveBottom) {

    }

    @Override
    public long insertFence() {
        return 0;
    }

    @Override
    public boolean checkFence(long fence) {
        return false;
    }

    @Override
    public void deleteFence(long fence) {

    }

    @Override
    public void addFinishedCallback(FlushInfo.FinishedCallback callback) {

    }

    @Override
    public void checkFinishedCallbacks() {

    }

    @Override
    public void waitForQueue() {

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

    @javax.annotation.Nullable
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
