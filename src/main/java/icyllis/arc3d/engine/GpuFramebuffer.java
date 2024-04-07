/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RawPtr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The {@link GpuFramebuffer} manages all objects used by a rendering pipeline,
 * which are framebuffers, render passes and a set of attachments. This is the target
 * of {@link OpsRenderPass}, and may be associated with {@link icyllis.arc3d.core.Surface}.
 * <p>
 * A {@link GpuFramebuffer} is always associated with a renderable primary surface, which
 * can be either a renderable {@link GpuImage} or a wrapped {@link BackendRenderTarget}.
 * This class is used by the pipeline internally. Use {@link RenderTextureProxy}
 * and {@link RenderTargetProxy} for high-level operations.
 */
public abstract non-sealed class GpuFramebuffer extends GpuResourceBase implements GpuSurface {

    private final int mWidth;
    private final int mHeight;

    private final int mSampleCount;
    private final int mNumRenderTargets;

    /*
     * The stencil buffer is set at first only with wrapped <code>GLRenderTarget</code>,
     * the stencil attachment is fake and made beforehand (renderbuffer id 0). For example,
     * wrapping OpenGL default framebuffer (framebuffer id 0).
     */
    /*@SharedPtr
    protected Attachment mStencilBuffer;*/

    // determined by subclass constructors
    protected int mSurfaceFlags = ISurface.FLAG_RENDERABLE;

    protected GpuFramebuffer(GpuDevice device,
                             int width, int height,
                             int sampleCount,
                             int numRenderTargets) {
        super(device);
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
        mNumRenderTargets = numRenderTargets;
    }

    /**
     * Returns the effective width (intersection) of color buffers.
     */
    @Override
    public final int getWidth() {
        return mWidth;
    }

    /**
     * Returns the effective height (intersection) of color buffers.
     */
    @Override
    public final int getHeight() {
        return mHeight;
    }

    /**
     * Returns the number of samples per pixel in color buffers (One if non-MSAA).
     *
     * @return the number of samples, greater than (multisample) or equal to one
     */
    @Override
    public final int getSampleCount() {
        return mSampleCount;
    }

    /**
     * Describes the backend format of color buffers.
     */
    @Nonnull
    @Override
    public abstract BackendFormat getBackendFormat();

    /**
     * Describes the backend render target of this render target.
     */
    @Nonnull
    public abstract BackendRenderTarget getBackendRenderTarget();

    @Override
    public GpuImage asImage() {
        GpuImage attr = getResolveAttachment();
        if (attr != null) {
            return attr;
        }
        return getColorAttachment();
    }

    @Override
    public GpuTexture asTexture() {
        return asImage().asTexture();
    }

    @Override
    public final GpuFramebuffer asFramebuffer() {
        return this;
    }

    @Override
    public int getSurfaceFlags() {
        return mSurfaceFlags;
    }

    public final int numRenderTargets() {
        return mNumRenderTargets;
    }

    @RawPtr
    @Nullable
    public abstract GpuImage getColorAttachment();

    @RawPtr
    @Nullable
    public abstract GpuImage getColorAttachment(int index);

    @RawPtr
    @Nullable
    protected abstract GpuImage[] getColorAttachments();

    @RawPtr
    @Nullable
    public abstract GpuImage getResolveAttachment();

    @RawPtr
    @Nullable
    public abstract GpuImage getResolveAttachment(int index);

    @RawPtr
    @Nullable
    protected abstract GpuImage[] getResolveAttachments();

    /**
     * Get the dynamic or implicit stencil buffer, or null if no stencil.
     */
    @RawPtr
    @Nullable
    public abstract GpuImage getDepthStencilAttachment();

    /**
     * Get the number of implicit depth bits, or 0 if no depth.
     */
    public abstract int getDepthBits();

    /**
     * Get the number of implicit stencil bits, or 0 if no stencil.
     */
    public abstract int getStencilBits();

    @Override
    public final long getMemorySize() {
        // framebuffer is a container object
        return 0;
    }

    @Nullable
    @Override
    protected IScratchKey computeScratchKey() {
        if (mNumRenderTargets > 1) {
            // MRT is only used in specific scenarios, cannot be scratch
            return null;
        }
        GpuImage colorAttr = getColorAttachment();
        GpuImage resolveAttr = getResolveAttachment();
        GpuImage depthStencilAttr = getDepthStencilAttachment();
        return new ScratchKey().compute(
                mWidth, mHeight,
                colorAttr != null ? colorAttr.getBackendFormat() : null,
                colorAttr != null ? colorAttr.getSurfaceFlags() : 0,
                resolveAttr != null ? resolveAttr.getBackendFormat() : null,
                resolveAttr != null ? resolveAttr.getSurfaceFlags() : 0,
                depthStencilAttr != null ? depthStencilAttr.getBackendFormat() : null,
                depthStencilAttr != null ? depthStencilAttr.getSurfaceFlags() : 0,
                mSampleCount,
                mSurfaceFlags
        );
    }

    /**
     * @return whether a stencil buffer can be attached to this render target.
     */
    protected abstract boolean canAttachStencil();

    /*
     * Allows the backends to perform any additional work that is required for attaching an
     * Attachment. When this is called, the Attachment has already been put onto the RenderTarget.
     * This method must return false if any failures occur when completing the stencil attachment.
     *
     * @see ResourceProvider
     */
    //protected abstract void attachStencilBuffer(@SharedPtr Attachment stencilBuffer);

    /**
     * Scratch key of {@link GpuFramebuffer}.
     */
    public static final class ScratchKey implements IScratchKey {

        public int mWidth;
        public int mHeight;
        public int mColorFormat;
        public int mResolveFormat;
        public int mDepthStencilFormat;
        public int mColorFlags;
        public int mResolveFlags;
        public int mDepthStencilFlags;
        public int mFramebufferFlags;

        /**
         * Update this key with the given arguments.
         *
         * @return this
         */
        @Nonnull
        public ScratchKey compute(int width, int height,
                                  BackendFormat colorFormat,
                                  int colorSurfaceFlags,
                                  BackendFormat resolveFormat,
                                  int resolveSurfaceFlags,
                                  BackendFormat depthStencilFormat,
                                  int depthStencilSurfaceFlags,
                                  int sampleCount,
                                  int framebufferFlags) {
            assert (width > 0 && height > 0);
            mWidth = width;
            mHeight = height;
            mColorFormat = colorFormat != null ? colorFormat.getFormatKey() : 0;
            mResolveFormat = resolveFormat != null ? resolveFormat.getFormatKey() : 0;
            mDepthStencilFormat = depthStencilFormat != null ? depthStencilFormat.getFormatKey() : 0;
            mColorFlags = colorSurfaceFlags;
            mResolveFlags = resolveSurfaceFlags;
            mDepthStencilFlags = depthStencilSurfaceFlags;
            mFramebufferFlags = (framebufferFlags & 0) | (sampleCount << 16);
            return this;
        }

        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mColorFormat;
            result = 31 * result + mResolveFormat;
            result = 31 * result + mDepthStencilFormat;
            result = 31 * result + mColorFlags;
            result = 31 * result + mResolveFlags;
            result = 31 * result + mDepthStencilFlags;
            result = 31 * result + mFramebufferFlags;
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof ScratchKey key &&
                    mWidth == key.mWidth &&
                    mHeight == key.mHeight &&
                    mColorFormat == key.mColorFormat &&
                    mResolveFormat == key.mResolveFormat &&
                    mDepthStencilFormat == key.mDepthStencilFormat &&
                    mColorFlags == key.mColorFlags &&
                    mResolveFlags == key.mResolveFlags &&
                    mDepthStencilFlags == key.mDepthStencilFlags &&
                    mFramebufferFlags == key.mFramebufferFlags;
        }
    }
}
