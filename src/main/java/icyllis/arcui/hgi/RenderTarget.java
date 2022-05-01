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

package icyllis.arcui.hgi;

import icyllis.arcui.gl.GLRenderbuffer;
import icyllis.arcui.gl.GLServer;

import javax.annotation.Nonnull;

/**
 * RenderTarget is mostly GLFramebuffer or VkFramebuffer and VkRenderPass, which
 * contains a set of attachments. This is the place where the rendering pipeline will
 * eventually draw to, and associated with a Surface and a Layer.
 */
public abstract class RenderTarget extends ManagedResource {

    private final int mWidth;
    private final int mHeight;

    private final int mSampleCount;

    private Surface mStencilBuffer;
    private Surface mMSAAStencilBuffer;

    // determined by subclass constructor
    protected int mFlags;

    public RenderTarget(Server server, int width, int height, int sampleCount) {
        super(server);
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
    }

    /**
     * This constructor is only used with wrapped <code>GLRenderTarget</code>, the stencil
     * attachment is fake and made beforehand (renderbuffer id 0). For example, wrapping
     * OpenGL default framebuffer (id 0).
     *
     * @param stencilBuffer an intrinsic stencil buffer
     */
    public RenderTarget(GLServer server, int width, int height, int sampleCount,
                        GLRenderbuffer stencilBuffer) {
        super(server);
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
        if (sampleCount > 1) {
            mMSAAStencilBuffer = stencilBuffer;
        } else {
            mStencilBuffer = stencilBuffer;
        }
    }

    /**
     * Returns the effective width (intersection) of color buffers.
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * Returns the effective height (intersection) of color buffers.
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * Returns the number of samples/pixel in color buffers (One if non-MSAA).
     */
    public final int getSampleCount() {
        return mSampleCount;
    }

    /**
     * Describes the backend format of color buffers.
     * <p>
     * Unlike {@link BackendRenderTarget#getBackendFormat()} which returns texture type of NONE.
     * This method is always {@link Types#TEXTURE_TYPE_2D}.
     */
    @Nonnull
    public abstract BackendFormat getBackendFormat();

    /**
     * Describes the backend render target of this render target.
     */
    @Nonnull
    public abstract BackendRenderTarget getBackendRenderTarget();

    /**
     * May get a dynamic stencil buffer, or null.
     */
    public final Surface getStencilBuffer(boolean useMSAA) {
        return useMSAA ? mMSAAStencilBuffer : mStencilBuffer;
    }

    /**
     * May get an intrinsic stencil buffer, or null.
     */
    public final Surface getStencilBuffer() {
        return mSampleCount > 1 ? mMSAAStencilBuffer : mStencilBuffer;
    }

    /**
     * Get dynamic stencil bits, return 0 if stencil buffer is not set.
     */
    public final int getStencilBits(boolean useMSAA) {
        final Surface stencilBuffer;
        if ((stencilBuffer = useMSAA ? mMSAAStencilBuffer : mStencilBuffer) != null) {
            return stencilBuffer.getBackendFormat().getStencilBits();
        } else {
            return 0;
        }
    }

    /**
     * Get intrinsic stencil bits, return 0 if stencil buffer is not set.
     */
    public final int getStencilBits() {
        final Surface stencilBuffer;
        if ((stencilBuffer = mSampleCount > 1 ? mMSAAStencilBuffer : mStencilBuffer) != null) {
            return stencilBuffer.getBackendFormat().getStencilBits();
        } else {
            return 0;
        }
    }

    @Override
    protected void onFree() {
        if (mStencilBuffer != null) {
            mStencilBuffer.unref();
        }
        mStencilBuffer = null;
        if (mMSAAStencilBuffer != null) {
            mMSAAStencilBuffer.unref();
        }
        mMSAAStencilBuffer = null;
    }

    /**
     * Returns whether a stencil buffer <b>can</b> be attached to this render target.
     * There may already be a stencil attachment.
     */
    protected abstract boolean canSetStencilBuffer(boolean useMSAA);

    /**
     * Allows the backends to perform any additional work that is required for attaching an
     * Attachment. When this is called, the Attachment has already been put onto the RenderTarget.
     * This method must return false if any failures occur when completing the stencil attachment.
     *
     * @return if false, the stencil attachment will not be set to this render target
     */
    protected abstract boolean onSetStencilBuffer(Surface stencilBuffer, boolean useMSAA);

    final void setStencilBuffer(Surface stencilBuffer, boolean useMSAA) {
        if (stencilBuffer == null && (useMSAA ? mMSAAStencilBuffer : mStencilBuffer) == null) {
            // No need to do any work since we currently don't have a stencil attachment,
            // and we're not actually adding one.
            return;
        }

        if (!onSetStencilBuffer(stencilBuffer, useMSAA)) {
            return;
        }

        if (useMSAA) {
            mMSAAStencilBuffer = stencilBuffer;
        } else {
            mStencilBuffer = stencilBuffer;
        }
    }
}
