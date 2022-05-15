/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.gl;

import icyllis.arcui.hgi.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nullable;

import static icyllis.arcui.gl.GLCore.*;

public final class GLServer extends Server {

    final GLCaps mCaps;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
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
        // get or create
        GLCapabilities caps;
        try {
            caps = GL.getCapabilities();
            if (caps == null) {
                // checks may be disabled
                caps = GL.createCapabilities();
            }
        } catch (IllegalStateException e) {
            // checks may be enabled
            caps = GL.createCapabilities();
        }
        if (caps == null) {
            return null;
        }
        return new GLServer(context, new GLCaps(options, caps));
    }

    public void bindFramebuffer(int target, int framebuffer) {
        glBindFramebuffer(target, framebuffer);
    }

    public void deleteFramebuffer(int framebuffer) {
        // We're relying on the GL state shadowing being correct in the workaround code below, so we
        // need to handle a dirty context.
        handleDirty();
        glDeleteFramebuffers(framebuffer);
    }

    @Override
    public ThreadSafePipelineBuilder getPipelineBuilder() {
        return new ThreadSafePipelineBuilder() {
            @Override
            public void close() throws Exception {

            }
        };
    }

    @Nullable
    @Override
    protected Texture onCreateTexture(int width, int height,
                                      BackendFormat format,
                                      boolean budgeted,
                                      boolean isProtected,
                                      int mipLevels) {
        // We don't support protected textures in core profile.
        if (isProtected) {
            return null;
        }
        // We only support TEXTURE_2D.
        if (format.getTextureType() != Types.TEXTURE_TYPE_2D) {
            return null;
        }
        int f = format.getGLFormat();
        int tex = createTexture(width, height, f, mipLevels);
        if (tex == 0) {
            return null;
        }
        return new GLTexture(this, width, height, tex, format, mipLevels > 1 ? Types.MIPMAP_STATUS_DIRTY :
                Types.MIPMAP_STATUS_NONE, budgeted, true);
    }

    @Nullable
    @Override
    protected RenderTarget onWrapRenderableBackendTexture(BackendTexture texture, int sampleCount, boolean ownership) {
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

        // There's an NVIDIA driver bug that creating framebuffer via DSA with attachments of
        // different dimensions will give you GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT.
        // The workaround is to use traditional glGen* and glBind* (binding make it valid).
        final int framebuffer = glGenFramebuffers();
        if (framebuffer == 0) {
            return null;
        }
        // Create the state vector of the framebuffer, not bind, because we are not DSA.
        bindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);

        final int msaaFramebuffer;
        final GLRenderbuffer msaaColorBuffer;
        // If we are using multisampling we will create two FBOs. We render to one and then resolve to
        // the texture bound to the other. The exception is the IMG multisample extension. With this
        // extension the texture is multisampled when rendered to and then auto-resolves it when it is
        // rendered from.
        if (sampleCount <= 1) {
            msaaFramebuffer = 0;
            msaaColorBuffer = null;
        } else {
            msaaFramebuffer = glGenFramebuffers();
            if (msaaFramebuffer == 0) {
                deleteFramebuffer(framebuffer);
                return null;
            }
            // Create the state vector of the framebuffer, not bind, because we are not DSA.
            bindFramebuffer(GL_DRAW_FRAMEBUFFER, msaaFramebuffer);

            msaaColorBuffer = GLRenderbuffer.makeMSAA(this,
                    texture.getWidth(), texture.getHeight(),
                    sampleCount,
                    format);
            if (msaaColorBuffer == null) {
                deleteFramebuffer(framebuffer);
                deleteFramebuffer(msaaFramebuffer);
                return null;
            }

            glNamedFramebufferRenderbuffer(msaaFramebuffer,
                    GL_COLOR_ATTACHMENT0,
                    GL_RENDERBUFFER,
                    msaaColorBuffer.getRenderbuffer());
            if (!mCaps.skipErrorChecks()) {
                int status = glCheckNamedFramebufferStatus(msaaFramebuffer, GL_DRAW_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    deleteFramebuffer(framebuffer);
                    deleteFramebuffer(msaaFramebuffer);
                    msaaColorBuffer.unref();
                    return null;
                }
            }
        }

        glNamedFramebufferTexture(framebuffer,
                GL_COLOR_ATTACHMENT0,
                info.mTexture,
                0);
        if (!mCaps.skipErrorChecks()) {
            int status = glCheckNamedFramebufferStatus(framebuffer, GL_DRAW_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                deleteFramebuffer(framebuffer);
                deleteFramebuffer(msaaFramebuffer);
                if (msaaColorBuffer != null) {
                    msaaColorBuffer.unref();
                }
                return null;
            }
        }


        return null;
    }

    private int createTexture(int width, int height, int format, int levels) {
        assert width > 0;
        assert height > 0;
        assert format != GLTypes.FORMAT_UNKNOWN;
        assert !glFormatIsCompressed(format);
        assert levels > 0;

        int internalFormat = mCaps.getTextureInternalFormat(format);

        if (internalFormat != 0) {
            assert (mCaps.mFormatTable[format].mFlags & FormatInfo.TEXTURE_FLAG) != 0;
            assert (mCaps.mFormatTable[format].mFlags & FormatInfo.USE_TEX_STORAGE_FLAG) != 0;
            int texture = glCreateTextures(GL_TEXTURE_2D);
            if (texture == 0) {
                return 0;
            }
            glTextureStorage2D(texture, levels, internalFormat, width, height);
            return texture;
        }
        return 0;
    }
}
