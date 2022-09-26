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

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.opengl.GLRenderTarget;

import javax.annotation.Nonnull;

/**
 * The {@link RenderTarget} manages framebuffers and render passes, which consisted
 * of a set of attachments. This is the place where the rendering pipeline will
 * eventually draw to, and may be associated with a {@link icyllis.arcticgi.core.Surface}.
 * <p>
 * This type of resource can be recycled by {@link Server}. When recycled, the texture
 * may be used separately, or retrieve this {@link RenderTarget} from {@link Server}.
 * However, static MSAA color buffer and stencil buffer cannot be recycled. This behavior
 * is controlled by {@link ResourceAllocator}.
 * <p>
 * Use {@link ResourceProvider} to obtain {@link RenderTarget RenderTargets} directly. Use
 * {@link RenderSurfaceProxy} and {@link RenderTextureProxy} for deferred operations.
 */
public abstract class RenderTarget extends ManagedResource implements Surface {

    private final int mWidth;
    private final int mHeight;

    private final int mSampleCount;

    /**
     * The stencil buffer is set at first only with wrapped <code>GLRenderTarget</code>,
     * the stencil attachment is fake and made beforehand (renderbuffer id 0). For example,
     * wrapping OpenGL default framebuffer (framebuffer id 0).
     */
    @SharedPtr
    protected Attachment mStencilBuffer;

    // determined by subclass constructors
    protected int mSurfaceFlags;

    protected RenderTarget(int width, int height, int sampleCount) {
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
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
     * Returns the number of samples per pixel in color buffers (One if non-MSAA).
     *
     * @return the number of samples, greater than (multisample) or equal to one
     */
    public final int getSampleCount() {
        return mSampleCount;
    }

    /**
     * Describes the backend format of color buffers.
     * <p>
     * Unlike {@link BackendRenderTarget#getBackendFormat()} which returns texture type of NONE.
     * This method is always {@link Engine#TextureType_2D}.
     */
    @Nonnull
    public abstract BackendFormat getBackendFormat();

    /**
     * Describes the backend render target of this render target.
     */
    @Nonnull
    public abstract BackendRenderTarget getBackendRenderTarget();

    /**
     * May get the single sample texture, or null if wrapped.
     */
    public Texture getColorBuffer() {
        return null;
    }

    /**
     * Get the dynamic or implicit stencil buffer, or null if no stencil.
     */
    public final Attachment getStencilBuffer() {
        return mStencilBuffer;
    }

    /**
     * Get the number of dynamic or implicit stencil bits, or 0 if no stencil.
     */
    public final int getStencilBits() {
        return mStencilBuffer != null ? mStencilBuffer.getBackendFormat().getStencilBits() : 0;
    }

    @Override
    protected void dispose() {
        if (mStencilBuffer != null) {
            mStencilBuffer.unref();
        }
        mStencilBuffer = null;
    }

    /**
     * @return whether a stencil buffer can be attached to this render target.
     */
    protected abstract boolean canAttachStencil();

    /**
     * Allows the backends to perform any additional work that is required for attaching an
     * Attachment. When this is called, the Attachment has already been put onto the RenderTarget.
     * This method must return false if any failures occur when completing the stencil attachment.
     *
     * @see ResourceProvider
     */
    protected abstract void attachStencilBuffer(@SharedPtr Attachment stencilBuffer);
}
