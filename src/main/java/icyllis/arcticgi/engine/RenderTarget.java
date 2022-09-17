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

import icyllis.arcticgi.opengl.GLRenderbuffer;

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
 * {@link RenderTargetProxy} and {@link TextureRenderTargetProxy} for deferred operations.
 */
public abstract class RenderTarget extends ManagedResource {

    private final int mWidth;
    private final int mHeight;

    private final int mSampleCount;

    private Surface mStencilBuffer;
    private Surface mMSAAStencilBuffer;

    // determined by subclass constructors
    protected int mFlags;

    private final int mUniqueID;

    protected RenderTarget(int width, int height, int sampleCount) {
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
        mUniqueID = GpuResource.createUniqueID();
    }

    /**
     * This constructor is only used with wrapped <code>GLRenderTarget</code>, the stencil
     * attachment is fake and made beforehand (renderbuffer id 0). For example, wrapping
     * OpenGL default framebuffer (id 0).
     *
     * @param stencilBuffer an intrinsic stencil buffer
     */
    protected RenderTarget(int width, int height, int sampleCount,
                           GLRenderbuffer stencilBuffer) {
        this(width, height, sampleCount);
        if (sampleCount > 1) {
            mMSAAStencilBuffer = stencilBuffer;
        } else {
            mStencilBuffer = stencilBuffer;
        }
    }

    /**
     * Gets an id that is unique for this Resource object. It is static in that it does
     * not change when the content of the Resource object changes. This will never return 0.
     */
    public final int getUniqueID() {
        return mUniqueID;
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
     * This method is always {@link EngineTypes#TextureType_2D}.
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
    public abstract Texture getColorBuffer();

    /**
     * May get a dynamic stencil buffer, or null if no stencil.
     */
    public final Surface getStencilBuffer(boolean useMSAA) {
        return useMSAA ? mMSAAStencilBuffer : mStencilBuffer;
    }

    /**
     * May get an intrinsic stencil buffer, or null if no stencil.
     */
    public final Surface getStencilBuffer() {
        return mSampleCount > 1 ? mMSAAStencilBuffer : mStencilBuffer;
    }

    /**
     * Get dynamic stencil bits, return 0 if stencil buffer is not set.
     */
    public final int getStencilBits(boolean useMSAA) {
        final Surface stencilBuffer;
        if ((stencilBuffer = getStencilBuffer(useMSAA)) != null) {
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
        if ((stencilBuffer = getStencilBuffer()) != null) {
            return stencilBuffer.getBackendFormat().getStencilBits();
        } else {
            return 0;
        }
    }

    @Override
    protected void dispose() {
        if (mStencilBuffer != null) {
            mStencilBuffer.unref();
        }
        if (mMSAAStencilBuffer != null) {
            mMSAAStencilBuffer.unref();
        }
        mStencilBuffer = null;
        mMSAAStencilBuffer = null;
    }

    /**
     * @return whether a stencil buffer can be attached to this render target.
     */
    protected abstract boolean isStencilAttachable();

    /**
     * @see ResourceProvider
     */
    protected final void attachStencilBuffer(Surface stencilBuffer, boolean useMSAA) {
        if (stencilBuffer == null && (useMSAA ? mMSAAStencilBuffer : mStencilBuffer) == null) {
            // No need to do any work since we currently don't have a stencil attachment,
            // and we're not actually adding one.
            return;
        }

        if (!completeStencilBuffer(stencilBuffer, useMSAA)) {
            return;
        }

        if (useMSAA) {
            mMSAAStencilBuffer = stencilBuffer;
        } else {
            mStencilBuffer = stencilBuffer;
        }
    }

    /**
     * Allows the backends to perform any additional work that is required for attaching an
     * Attachment. When this is called, the Attachment has already been put onto the RenderTarget.
     * This method must return false if any failures occur when completing the stencil attachment.
     *
     * @return if false, the stencil attachment will not be set to this render target
     */
    protected abstract boolean completeStencilBuffer(Surface stencilBuffer, boolean useMSAA);

    /**
     * Compute a {@link RenderTarget} key. Parameters are the same as Surface key, just
     * class types are different. RenderTarget key may be used in resource allocator.
     *
     * @return a new scratch key
     * @see Surface#computeScratchKey()
     */
    @Nonnull
    public static ResourceKey computeScratchKey(BackendFormat format,
                                                int width, int height,
                                                int sampleCount,
                                                boolean mipmapped,
                                                boolean isProtected,
                                                Key key) {
        assert width > 0 && height > 0;
        assert sampleCount > 0;
        // we can have both multisample and mipmapped as key for render targets
        key.mWidth = width;
        key.mHeight = height;
        key.mFormat = format.getFormatKey();
        key.mFlags = (mipmapped ? 1 : 0) | (isProtected ? 2 : 0) | (sampleCount << 2);
        return key;
    }

    public static class Key extends ResourceKey {

        int mWidth;
        int mHeight;
        int mFormat;
        int mFlags;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            if (mWidth != key.mWidth) return false;
            if (mHeight != key.mHeight) return false;
            if (mFormat != key.mFormat) return false;
            return mFlags == key.mFlags;
        }

        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mFormat;
            result = 31 * result + mFlags;
            return result;
        }
    }
}
