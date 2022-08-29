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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arcticgi.opengl.GLCore.*;

//TODO don't create new backend format
public final class GLRenderTarget extends RenderTarget {

    // the render target format, same as main color buffer
    private final int mFormat;

    // single sample framebuffer, may be resolved by MSAA framebuffer
    private int mFramebuffer;
    private int mMSAAFramebuffer;

    // the main color buffer, raw ptr
    // if this texture is deleted, then this render target is deleted as well
    // always null for wrapped render targets
    private GLTexture mColorBuffer;
    // the renderbuffer used as MSAA color buffer
    // always null for wrapped render targets
    @SharedPtr
    private GLRenderbuffer mMSAAColorBuffer;

    // if we need bind stencil buffers on next framebuffer bind call
    private boolean mRebindStencilBuffer;
    private boolean mRebindMSAAStencilBuffer;

    // should we delete framebuffers ourselves?
    private final boolean mOwnership;

    private BackendFormat mBackendFormat;
    private BackendRenderTarget mBackendRenderTarget;

    public GLRenderTarget(GLServer server,
                          int width, int height,
                          int format,
                          int sampleCount,
                          int framebuffer,
                          int msaaFramebuffer,
                          GLTexture colorBuffer,
                          @SharedPtr GLRenderbuffer msaaColorBuffer,
                          boolean ownership) {
        super(server, width, height, sampleCount);
        assert sampleCount > 0;
        // we always resolve MSAA framebuffer manually
        assert framebuffer == 0 || framebuffer != msaaFramebuffer;
        mFormat = format;
        mFramebuffer = framebuffer;
        mMSAAFramebuffer = msaaFramebuffer;
        mColorBuffer = colorBuffer;
        mMSAAColorBuffer = msaaColorBuffer;
        mOwnership = ownership;
        if ((framebuffer | msaaFramebuffer) == 0) {
            mFlags |= EngineTypes.InternalSurfaceFlag_GLWrapsDefaultFramebuffer;
        }
    }

    // Constructor for wrapped render targets.
    private GLRenderTarget(GLServer server,
                           int width, int height,
                           int format,
                           int sampleCount,
                           int framebuffer,
                           int msaaFramebuffer,
                           GLTexture colorBuffer,
                           @SharedPtr GLRenderbuffer msaaColorBuffer,
                           @SharedPtr GLRenderbuffer stencilBuffer,
                           boolean ownership) {
        super(server, width, height, sampleCount, stencilBuffer);
        assert sampleCount > 0;
        // we always resolve MSAA framebuffer manually
        assert framebuffer == 0 || framebuffer != msaaFramebuffer;
        mFormat = format;
        mFramebuffer = framebuffer;
        mMSAAFramebuffer = msaaFramebuffer;
        mColorBuffer = colorBuffer;
        mMSAAColorBuffer = msaaColorBuffer;
        mOwnership = ownership;
        if ((framebuffer | msaaFramebuffer) == 0) {
            mFlags |= EngineTypes.InternalSurfaceFlag_GLWrapsDefaultFramebuffer;
        }
    }

    @Nonnull
    public static GLRenderTarget makeWrapped(GLServer server,
                                             int width, int height,
                                             int format,
                                             int sampleCount,
                                             int framebuffer,
                                             int msaaFramebuffer,
                                             GLTexture colorBuffer,
                                             @SharedPtr GLRenderbuffer msaaColorBuffer,
                                             int stencilBits,
                                             boolean ownership) {
        GLRenderbuffer stencilBuffer = null;
        if (stencilBits > 0) {
            // We pick a "fake" actual format that matches the number of stencil bits. When wrapping
            // an FBO with some number of stencil bits all we care about in the future is that we have
            // a format with the same number of stencil bits. We don't even directly use the format or
            // any other properties. Thus, it is fine for us to just assign an arbitrary format that
            // matches the stencil bit count.
            int stencilFormat = switch (stencilBits) {
                case 8 -> GLTypes.FORMAT_STENCIL_INDEX8;
                case 16 -> GLTypes.FORMAT_STENCIL_INDEX16;
                default -> throw new IllegalArgumentException();
            };

            // We don't have the actual renderbufferID, but we need to make an attachment for the stencil,
            // so we just set it to an invalid value of 0 to make sure we don't explicitly use it or try
            // and delete it.
            stencilBuffer = GLRenderbuffer.makeWrapped(server, width, height, sampleCount, stencilFormat,
                    /*renderbufferID=*/0);
        }
        return new GLRenderTarget(server, width, height, format, sampleCount,
                framebuffer, msaaFramebuffer, colorBuffer, msaaColorBuffer, stencilBuffer, ownership);
    }

    public int getFormat() {
        return mFormat;
    }

    public boolean isDefaultFramebuffer(boolean useMSAA) {
        return (useMSAA ? mMSAAFramebuffer : mFramebuffer) == 0;
    }

    /**
     * Binds the render target to GL_FRAMEBUFFER for rendering.
     *
     * @param useMSAA whether do MSAA rendering
     */
    public void bind(boolean useMSAA) {
        bindInternal(GL_FRAMEBUFFER, useMSAA);
    }

    /**
     * Binds the render target for copying, reading, or clearing pixel values. If we are an MSAA
     * render target we bind the multisample framebuffer. Otherwise, we bind the single sample
     * framebuffer. Because we always need manual MSAA resolving.
     *
     * @param target read, write or both
     */
    public void bindForPixelOps(int target) {
        bindInternal(target, getSampleCount() > 1);
    }

    /**
     * Binds the multisample framebuffer and the single sample framebuffer, one to GL_DRAW_FRAMEBUFFER
     * and the other to GL_READ_FRAMEBUFFER, depending on ResolveDirection.
     *
     * @param downSample true for down-sampling (MSAA to single) or for up-sampling (single to MSAA)
     */
    public void bindForResolve(boolean downSample) {
        // If the multisample FBO is nonzero, it means we always have something to resolve (even if the
        // single sample buffer is FBO 0). If it's zero, then there's nothing to resolve.
        assert mMSAAFramebuffer != 0;
        if (downSample) {
            bindInternal(GL_READ_FRAMEBUFFER, true);
            bindInternal(GL_DRAW_FRAMEBUFFER, false);
        } else {
            // Core-profile allows up-sampling
            bindInternal(GL_READ_FRAMEBUFFER, false);
            bindInternal(GL_DRAW_FRAMEBUFFER, true);
        }
    }

    /**
     * Binds the render target to the given target and ensures its stencil attachment is valid.
     */
    private void bindInternal(int target, boolean useMSAA) {
        final int framebuffer = useMSAA ? mMSAAFramebuffer : mFramebuffer;
        getServer().bindFramebuffer(target, framebuffer);
        // Make sure the stencil attachment is valid. Even though a color buffer op doesn't use stencil,
        // our FBO still needs to be "framebuffer complete".
        if ((useMSAA && mRebindMSAAStencilBuffer) || (!useMSAA && mRebindStencilBuffer)) {
            bindStencilInternal(framebuffer, useMSAA);
        }
    }

    /**
     * If this render target is already bound, this method ensures the stencil attachment is valid
     * (attached or detached), if stencil buffer is reset right before.
     */
    public void bindStencil(boolean useMSAA) {
        if ((useMSAA && mRebindMSAAStencilBuffer) || (!useMSAA && mRebindStencilBuffer)) {
            final int framebuffer = useMSAA ? mMSAAFramebuffer : mFramebuffer;
            bindStencilInternal(framebuffer, useMSAA);
        }
    }

    private void bindStencilInternal(int framebuffer, boolean useMSAA) {
        if (getStencilBuffer(useMSAA) instanceof GLRenderbuffer stencilBuffer) {
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    stencilBuffer.getRenderbuffer());
            if (glFormatIsPackedDepthStencil(stencilBuffer.getFormat())) {
                glNamedFramebufferRenderbuffer(framebuffer,
                        GL_DEPTH_ATTACHMENT,
                        GL_RENDERBUFFER,
                        stencilBuffer.getRenderbuffer());
            } else {
                glNamedFramebufferRenderbuffer(framebuffer,
                        GL_DEPTH_ATTACHMENT,
                        GL_RENDERBUFFER,
                        0);
            }
        } else {
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    0);
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_DEPTH_ATTACHMENT,
                    GL_RENDERBUFFER,
                    0);
        }
        if (useMSAA) {
            mRebindMSAAStencilBuffer = false;
        } else {
            mRebindStencilBuffer = false;
        }
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = BackendFormat.makeGL(glFormatToEnum(mFormat), EngineTypes.TextureType_2D);
        }
        return mBackendFormat;
    }

    @Nonnull
    @Override
    public BackendRenderTarget getBackendRenderTarget() {
        if (mBackendRenderTarget == null) {
            final GLFramebufferInfo info = new GLFramebufferInfo();
            info.mFramebuffer = getSampleCount() > 1 ? mMSAAFramebuffer : mFramebuffer;
            info.mFormat = glFormatToEnum(mFormat);
            mBackendRenderTarget = new GLBackendRenderTarget(
                    getWidth(), getHeight(), getSampleCount(), getStencilBits(), info);
        }
        return mBackendRenderTarget;
    }

    @Override
    public Texture getColorBuffer() {
        return mColorBuffer;
    }

    @Override
    protected boolean canAttachStencil(boolean useMSAA) {
        // Only modify the framebuffer attachments if we have created it.
        // Public APIs do not currently allow for wrap-only ownership,
        // so we can safely assume that if an object is owner, we created it.
        return mOwnership ||
                // The dynamic msaa attachment is always owner and always supports adding stencil.
                (useMSAA && getSampleCount() == 1);
    }

    @Override
    protected boolean onAttachStencilBuffer(@Nullable Surface stencilBuffer, boolean useMSAA) {
        // We defer attaching the new stencil buffer until the next time our framebuffer is bound.
        if (getStencilBuffer(useMSAA) != stencilBuffer) {
            if (useMSAA) {
                mRebindMSAAStencilBuffer = true;
            } else {
                mRebindStencilBuffer = true;
            }
        }
        return true;
    }

    @Override
    protected void onFree() {
        super.onFree();
        if (mOwnership) {
            final GLServer server = getServer();
            if (mFramebuffer != 0) {
                server.deleteFramebuffer(mFramebuffer);
            }
            if (mMSAAFramebuffer != 0) {
                server.deleteFramebuffer(mMSAAFramebuffer);
            }
            if (mMSAAColorBuffer != null) {
                mMSAAColorBuffer.unref();
            }
        }
        mFramebuffer = 0;
        mMSAAFramebuffer = 0;
        mMSAAColorBuffer = null;
    }

    @Override
    public void onRecycle() {
        if (mColorBuffer != null) {
            mColorBuffer.unref();
        }
        mColorBuffer = null;
    }

    @Override
    protected GLServer getServer() {
        return (GLServer) super.getServer();
    }
}
