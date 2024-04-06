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
 * The {@link GpuFramebuffer} manages all objects used by a renderable primary surface,
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
    public abstract GpuImage getResolveAttachment();

    @RawPtr
    @Nullable
    public abstract GpuImage getResolveAttachment(int index);

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
    public long getMemorySize() {
        return 0;
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
}
