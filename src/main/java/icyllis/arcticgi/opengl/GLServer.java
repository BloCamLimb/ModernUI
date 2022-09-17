/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.*;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nullable;

import static icyllis.arcticgi.opengl.GLCore.*;

public final class GLServer extends Server {

    private static final Function<GLTexture, RenderTarget[]> RENDER_TARGET_FACTORY =
            t -> new RenderTarget[MAX_NUM_SAMPLES_COUNT];

    final GLCaps mCaps;

    private final GLPipelineStateCache mProgramCache;

    // This map holds all render targets. The texture and render target are mutually exclusive.
    // The texture is single sampled, the render targets may be multisampled.
    private final Object2ObjectOpenHashMap<GLTexture, RenderTarget[]> mRenderTargetMap;

    private GLCommandBuffer mMainCmdBuffer = new GLCommandBuffer(this);

    private final RenderTargetObjects mTmpRTObjects = new RenderTargetObjects();

    private final CpuBufferCache mCpuBufferCache;

    private final GLBufferAllocPool mVertexPool;
    private final GLBufferAllocPool mInstancePool;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        mProgramCache = new GLPipelineStateCache(this, 256);
        mRenderTargetMap = new Object2ObjectOpenHashMap<>();
        mCpuBufferCache = new CpuBufferCache(6);
        mVertexPool = GLBufferAllocPool.makeVertex(this, mCpuBufferCache);
        mInstancePool = GLBufferAllocPool.makeInstance(this, mCpuBufferCache);
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
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        mVertexPool.reset();
        mInstancePool.reset();
        mCpuBufferCache.releaseAll();

        if (cleanup) {
            mProgramCache.reset();
        } else {
            mProgramCache.abandon();
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

    @Nullable
    @Override
    protected Texture onCreateTexture(int width, int height,
                                      BackendFormat format,
                                      int levelCount,
                                      boolean budgeted,
                                      boolean isProtected) {
        assert levelCount > 0;
        // We don't support protected textures in OpenGL core profile.
        if (isProtected) {
            return null;
        }
        // We only support TEXTURE_2D.
        if (format.textureType() != EngineTypes.TextureType_2D) {
            return null;
        }
        int texture = createTextureObject(width, height, format.getGLFormat(), levelCount);
        if (texture == 0) {
            return null;
        }
        final GLTextureInfo info = new GLTextureInfo();
        info.mTexture = texture;
        info.mFormat = format.getGLFormatEnum();
        info.mLevelCount = levelCount;
        return new GLTexture(this, width, height, info, format, budgeted, true);
    }

    @Nullable
    @Override
    protected RenderTarget onFindOrCreateRenderTarget(@SharedPtr Texture texture,
                                                      int sampleCount) {
        assert sampleCount > 0;
        if (texture instanceof GLTexture glTexture) {
            assert mCaps.isFormatTexturable(glTexture.getFormat());
            assert mCaps.isFormatRenderable(glTexture.getFormat(), sampleCount);

            final RenderTarget[] renderTargets = mRenderTargetMap.computeIfAbsent(
                    glTexture, RENDER_TARGET_FACTORY);
            for (RenderTarget renderTarget : renderTargets) {
                if (renderTarget != null && renderTarget.getSampleCount() == sampleCount) {
                    renderTarget.ref();
                    return renderTarget;
                }
            }

            final RenderTargetObjects objects = createRenderTargetObjects(
                    glTexture.getTexture(),
                    glTexture.getWidth(), glTexture.getHeight(),
                    glTexture.getFormat(),
                    sampleCount);
            if (objects != null) {
                final RenderTarget renderTarget = new GLRenderTarget(this,
                        glTexture.getWidth(), glTexture.getHeight(),
                        glTexture.getFormat(), sampleCount,
                        objects.mFramebuffer,
                        objects.mMSAAFramebuffer,
                        glTexture,
                        objects.mMSAAColorBuffer,
                        true);
                boolean inserted = false;
                for (int i = 0; i < renderTargets.length; i++) {
                    if (renderTargets[i] == null) {
                        renderTargets[i] = renderTarget;
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    throw new UnsupportedOperationException();
                }
                return renderTarget;
            }
        }
        return null;
    }

    void onTextureDestroyed(GLTexture texture) {
        final RenderTarget[] renderTargets = mRenderTargetMap.get(texture);
        if (renderTargets != null) {
            for (RenderTarget renderTarget : renderTargets) {
                if (renderTarget != null) {
                    renderTarget.unref();
                    // We cannot reference RenderTarget without reference the texture, so it should be freed.
                    assert renderTarget.getRefCnt() == 0;
                }
            }
            mRenderTargetMap.remove(texture);
        }
    }

    @Nullable
    @Override
    protected RenderTarget onWrapRenderableBackendTexture(BackendTexture texture,
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

        RenderTargetObjects objects = createRenderTargetObjects(info.mTexture,
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
    protected void onResolveRenderTarget(RenderTarget renderTarget,
                                         int resolveLeft, int resolveTop,
                                         int resolveRight, int resolveBottom) {
        GLRenderTarget glRenderTarget = (GLRenderTarget) renderTarget;
        int framebuffer = glRenderTarget.getFramebuffer(false);
        int msaaFramebuffer = glRenderTarget.getFramebuffer(true);

        // If the multisample FBO is nonzero, it means we always have something to resolve (even if the
        // single sample buffer is FBO 0). If it's zero, then there's nothing to resolve.
        assert (msaaFramebuffer != 0);

        // In the EXT_multisampled_render_to_texture case, we shouldn't be resolving anything.
        assert (framebuffer == 0 || framebuffer != msaaFramebuffer);

        // BlitFramebuffer respects the scissor, so disable it.
        currentCommandBuffer().flushScissorTest(false);
        glBlitNamedFramebuffer(msaaFramebuffer, framebuffer, // MSAA to single
                resolveLeft, resolveTop, resolveRight, resolveBottom, // src rect
                resolveLeft, resolveTop, resolveRight, resolveBottom, // dst rect
                GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    private int createTextureObject(int width, int height, int format, int levels) {
        assert format != GLTypes.FORMAT_UNKNOWN;
        assert !glFormatIsCompressed(format);

        int internalFormat = mCaps.getTextureInternalFormat(format);
        if (internalFormat == 0) {
            return 0;
        }

        assert (mCaps.getFormatInfo(format).mFlags & FormatInfo.TEXTURABLE_FLAG) != 0;
        assert (mCaps.getFormatInfo(format).mFlags & FormatInfo.TEXTURE_STORAGE_FLAG) != 0;
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
    private RenderTargetObjects createRenderTargetObjects(int texture,
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
        final GLRenderbuffer msaaColorBuffer;
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

            msaaColorBuffer = GLRenderbuffer.makeMSAA(this,
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

        return mTmpRTObjects.set(framebuffer, msaaFramebuffer, msaaColorBuffer);
    }

    private static final class RenderTargetObjects {

        int mFramebuffer;
        int mMSAAFramebuffer;
        GLRenderbuffer mMSAAColorBuffer;

        RenderTargetObjects set(int framebuffer, int msaaFramebuffer, GLRenderbuffer msaaColorBuffer) {
            mFramebuffer = framebuffer;
            mMSAAFramebuffer = msaaFramebuffer;
            mMSAAColorBuffer = msaaColorBuffer;
            return this;
        }
    }
}
