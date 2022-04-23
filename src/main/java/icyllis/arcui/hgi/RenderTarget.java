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

import javax.annotation.Nullable;

/**
 * RenderTarget is mostly GLFramebuffer or VkFramebuffer plus VkRenderPass, which
 * contains a set of attachments. This is the place where the rendering pipeline will
 * eventually draw to, and associated with a Surface and a Layer.
 */
public abstract class RenderTarget extends ManagedResource {

    private final int mWidth;
    private final int mHeight;

    private final int mSampleCount;

    private Attachment mStencilAttachment;
    private Attachment mMSAAStencilAttachment;

    // determined by subclass constructor
    protected int mFlags;

    public RenderTarget(Server server, int width, int height, int sampleCount) {
        this(server, width, height, sampleCount, null);
    }

    public RenderTarget(Server server, int width, int height, int sampleCount, @Nullable Attachment stencilBuffer) {
        super(server);
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
        if (sampleCount > 1) {
            mMSAAStencilAttachment = stencilBuffer;
        } else {
            mStencilAttachment = stencilBuffer;
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
    public abstract BackendFormat getBackendFormat();

    /**
     * Describes the backend render target of this render target.
     */
    public abstract BackendRenderTarget getBackendRenderTarget();

    public final Attachment getStencilAttachment(boolean useMSAA) {
        return useMSAA ? mMSAAStencilAttachment : mStencilAttachment;
    }

    public final Attachment getStencilAttachment() {
        return mSampleCount > 1 ? mMSAAStencilAttachment : mStencilAttachment;
    }

    /**
     * Checked when this object is asked to attach a stencil buffer.
     */
    public abstract boolean canSetStencilAttachment(boolean useMSAA);

    public final void setStencilAttachment(@Nullable Attachment stencilBuffer, boolean useMSAA) {
        if (stencilBuffer == null) {
            if (useMSAA) {
                if (mMSAAStencilAttachment == null) {
                    return;
                }
            } else {
                if (mStencilAttachment == null) {
                    return;
                }
            }
        }

        if (!onSetStencilAttachment(stencilBuffer, useMSAA)) {
            return;
        }

        if (useMSAA) {
            mMSAAStencilAttachment = stencilBuffer;
        } else {
            mStencilAttachment = stencilBuffer;
        }
    }

    public final int getStencilBits(boolean useMSAA) {
        return getStencilAttachment(useMSAA).getBackendFormat().getStencilBits();
    }

    @Override
    protected void onFree() {
        mStencilAttachment = null;
        mMSAAStencilAttachment = null;
    }

    /**
     * Allows the backends to perform any additional work that is required for attaching an
     * Attachment. When this is called, the Attachment has already been put onto the RenderTarget.
     * This method must return false if any failures occur when completing the stencil attachment.
     *
     * @return completed or not
     */
    protected abstract boolean onSetStencilAttachment(@Nullable Attachment stencilBuffer, boolean useMSAA);
}
