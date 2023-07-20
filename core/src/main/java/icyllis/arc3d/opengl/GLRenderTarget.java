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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;

import static icyllis.arc3d.opengl.GLCore.*;

public final class GLRenderTarget extends RenderTarget {

    /**
     * The GL format for all color attachments.
     */
    private final int mFormat;

    // the main color buffer, raw ptr
    // if this texture is deleted, then this framebuffer set is deleted as well
    // null for wrapped render targets
    private GLTexture mColorBuffer;
    // the renderbuffer used as MSAA color buffer
    // null for wrapped render targets
    @SharedPtr
    private GLAttachment mMultisampleColorBuffer;

    private int mSampleFramebuffer;
    private int mResolveFramebuffer;

    // if we need bind stencil buffers on next framebuffer bind call
    private boolean mRebindStencilBuffer;

    // should we delete framebuffers ourselves?
    private final boolean mOwnership;

    private BackendFormat mBackendFormat;
    private BackendRenderTarget mBackendRenderTarget;

    // Constructor for instances created by our engine. (has texture access)
    GLRenderTarget(GLEngine engine,
                   int width, int height,
                   int format,
                   int sampleCount,
                   int framebuffer,
                   int msaaFramebuffer,
                   GLTexture colorBuffer,
                   GLAttachment msaaColorBuffer) {
        super(engine, width, height, sampleCount);
        assert (sampleCount > 0);
        mFormat = format;
        mSampleFramebuffer = sampleCount > 1 ? msaaFramebuffer : framebuffer;
        mResolveFramebuffer = framebuffer;
        mOwnership = true;
        mColorBuffer = colorBuffer;
        mMultisampleColorBuffer = msaaColorBuffer;
    }

    // Constructor for instances wrapping backend objects. (no texture access)
    private GLRenderTarget(GLEngine engine,
                           int width, int height,
                           int format,
                           int sampleCount,
                           int framebuffer,
                           boolean ownership,
                           @SharedPtr GLAttachment stencilBuffer) {
        super(engine, width, height, sampleCount);
        assert (sampleCount > 0);
        assert (framebuffer != 0 || !ownership);
        mFormat = format;
        mSampleFramebuffer = framebuffer;
        mResolveFramebuffer = framebuffer;
        mOwnership = ownership;
        mStencilBuffer = stencilBuffer; // std::move
        if (framebuffer == 0) {
            mSurfaceFlags |= Surface.FLAG_GL_WRAP_DEFAULT_FB;
        }
    }

    /**
     * Make a {@link GLRenderTarget} that wraps existing framebuffers without
     * accessing their backing buffers (texture and stencil).
     *
     * @param width  the effective width of framebuffer
     * @param height the effective height of framebuffer
     */
    @Nonnull
    @SharedPtr
    public static GLRenderTarget makeWrapped(GLEngine engine,
                                             int width, int height,
                                             int format,
                                             int sampleCount,
                                             int framebuffer,
                                             int stencilBits,
                                             boolean ownership) {
        assert (sampleCount > 0);
        assert (framebuffer != 0 || !ownership);
        GLAttachment stencilBuffer = null;
        if (stencilBits > 0) {
            // We pick a "fake" actual format that matches the number of stencil bits. When wrapping
            // an FBO with some number of stencil bits all we care about in the future is that we have
            // a format with the same number of stencil bits. We don't even directly use the format or
            // any other properties. Thus, it is fine for us to just assign an arbitrary format that
            // matches the stencil bit count.
            int stencilFormat = switch (stencilBits) {
                // We pick the packed format here so when we query total size we are at least not
                // underestimating the total size of the stencil buffer. However, in reality this
                // rarely matters since we usually don't care about the size of wrapped objects.
                case 8 -> GL_DEPTH24_STENCIL8;
                case 16 -> GL_STENCIL_INDEX16;
                default -> 0;
            };

            // We don't have the actual renderbufferID, but we need to make an attachment for the stencil,
            // so we just set it to an invalid value of 0 to make sure we don't explicitly use it or try
            // and delete it.
            stencilBuffer = GLAttachment.makeWrapped(engine,
                    width, height,
                    sampleCount,
                    stencilFormat,
                    0);
        }
        return new GLRenderTarget(engine,
                width, height,
                format,
                sampleCount,
                framebuffer,
                ownership,
                stencilBuffer);
    }

    /**
     * The render target format for all color attachments.
     */
    public int getFormat() {
        return mFormat;
    }

    public int getSampleFramebuffer() {
        return mSampleFramebuffer;
    }

    public int getResolveFramebuffer() {
        return mResolveFramebuffer;
    }

    /**
     * Make sure the stencil attachment is valid. Even though a color buffer op doesn't use stencil,
     * our FBO still needs to be "framebuffer complete". If this render target is already bound,
     * this method ensures the stencil attachment is valid (attached or detached), if stencil buffers
     * reset right before.
     */
    public void bindStencil() {
        if (!mRebindStencilBuffer) {
            return;
        }
        int framebuffer = mSampleFramebuffer;
        GLAttachment stencilBuffer = (GLAttachment) mStencilBuffer;
        if (stencilBuffer != null) {
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    stencilBuffer.getRenderbufferID());
            if (GLCore.glFormatIsPackedDepthStencil(stencilBuffer.getFormat())) {
                glNamedFramebufferRenderbuffer(framebuffer,
                        GL_DEPTH_ATTACHMENT,
                        GL_RENDERBUFFER,
                        stencilBuffer.getRenderbufferID());
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
        mRebindStencilBuffer = false;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        if (mBackendFormat == null) {
            mBackendFormat = GLBackendFormat.make(mFormat);
        }
        return mBackendFormat;
    }

    @Override
    public GLTexture getColorBuffer() {
        return mColorBuffer;
    }

    @Nonnull
    @Override
    public BackendRenderTarget getBackendRenderTarget() {
        if (mBackendRenderTarget == null) {
            final GLFramebufferInfo info = new GLFramebufferInfo();
            info.mFramebuffer = mSampleFramebuffer;
            info.mFormat = mFormat;
            mBackendRenderTarget = new GLBackendRenderTarget(
                    getWidth(), getHeight(), getSampleCount(), getStencilBits(), info);
        }
        return mBackendRenderTarget;
    }

    @Override
    protected boolean canAttachStencil() {
        // Only modify the framebuffer attachments if we have created it.
        // Public APIs do not currently allow for wrap-only ownership,
        // so we can safely assume that if an object is owner, we created it.
        return mOwnership;
    }

    @Override
    protected void attachStencilBuffer(@SharedPtr Attachment stencilBuffer) {
        if (stencilBuffer == null && mStencilBuffer == null) {
            // No need to do any work since we currently don't have a stencil attachment,
            // and we're not actually adding one.
            return;
        }

        // We defer attaching the new stencil buffer until the next time our framebuffer is bound.
        if (mStencilBuffer != stencilBuffer) {
            mRebindStencilBuffer = true;
        }

        mStencilBuffer = Resource.move(mStencilBuffer, stencilBuffer);
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        if (mOwnership) {
            if (mSampleFramebuffer != 0) {
                glDeleteFramebuffers(mSampleFramebuffer);
            }
            if (mSampleFramebuffer != mResolveFramebuffer) {
                assert (mResolveFramebuffer != 0);
                glDeleteFramebuffers(mResolveFramebuffer);
            }
        }
        mSampleFramebuffer = 0;
        mResolveFramebuffer = 0;
    }

    @Override
    public String toString() {
        return "GLRenderTarget{" +
                "mRenderFramebuffer=" + mSampleFramebuffer +
                ", mResolveFramebuffer=" + mResolveFramebuffer +
                ", mFormat=" + GLCore.glFormatName(mFormat) +
                ", mSampleCount=" + getSampleCount() +
                ", mMultisampleColorBuffer=" + mMultisampleColorBuffer +
                ", mOwnership=" + mOwnership +
                ", mBackendFormat=" + mBackendFormat +
                '}';
    }
}
