/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RawPtr;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The {@link GpuRenderTarget} manages all objects used by a rendering pipeline,
 * which are framebuffers, render passes and a set of attachments. This is the target
 * of {@link OpsRenderPass}, and may be associated with {@link icyllis.arc3d.core.Surface}.
 * <p>
 * A {@link GpuRenderTarget} may be associated with one or more renderable {@link Image}s
 * or a wrapped presentable object.
 * This class is used by the pipeline internally. Use {@link RenderTargetProxy} for
 * high-level operations.
 */
@Deprecated
public abstract class GpuRenderTarget extends GpuSurface {

    private final int mWidth;
    private final int mHeight;

    private final int mSampleCount;
    private final int mNumColorTargets;

    /*
     * The stencil buffer is set at first only with wrapped <code>GLRenderTarget</code>,
     * the stencil attachment is fake and made beforehand (renderbuffer id 0). For example,
     * wrapping OpenGL default framebuffer (framebuffer id 0).
     */
    /*@SharedPtr
    protected Attachment mStencilBuffer;*/

    // determined by subclass constructors
    protected int mSurfaceFlags = ISurface.FLAG_RENDERABLE;

    protected GpuRenderTarget(Context context,
                              int width, int height,
                              int sampleCount,
                              int numColorTargets) {
        super(context, true, false, 0);
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
        mNumColorTargets = numColorTargets;
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
    @NonNull
    @Override
    public abstract BackendFormat getBackendFormat();

    /**
     * Describes the backend render target of this render target.
     */
    @NonNull
    public abstract BackendRenderTarget getBackendRenderTarget();

    @Override
    public Image asImage() {
        Image att = getResolveAttachment();
        if (att != null) {
            return att;
        }
        return getColorAttachment();
    }

    @Override
    public final GpuRenderTarget asRenderTarget() {
        return this;
    }

    @Override
    public int getSurfaceFlags() {
        return mSurfaceFlags;
    }

    public final int numColorTargets() {
        return mNumColorTargets;
    }

    @RawPtr
    @Nullable
    public abstract Image getColorAttachment();

    @RawPtr
    @Nullable
    public abstract Image getColorAttachment(int index);

    @RawPtr
    protected abstract Image @Nullable[] getColorAttachments();

    @RawPtr
    @Nullable
    public abstract Image getResolveAttachment();

    @RawPtr
    @Nullable
    public abstract Image getResolveAttachment(int index);

    @RawPtr
    protected abstract Image @Nullable[] getResolveAttachments();

    /**
     * Get the dynamic or implicit stencil buffer, or null if no stencil.
     */
    @RawPtr
    @Nullable
    public abstract Image getDepthStencilAttachment();

    /**
     * Get the number of implicit depth bits, or 0 if no depth.
     */
    public abstract int getDepthBits();

    /**
     * Get the number of implicit stencil bits, or 0 if no stencil.
     */
    public abstract int getStencilBits();

    /*@Nullable
    @Override
    protected IResourceKey computeScratchKey() {
        if (mNumColorTargets > 1) {
            // MRT is only used in specific scenarios, cannot be scratch
            return null;
        }
        Image colorAtt = getColorAttachment();
        Image resolveAtt = getResolveAttachment();
        Image depthStencilAtt = getDepthStencilAttachment();
        return new ResourceKey().compute(
                mWidth, mHeight,
                colorAtt != null ? colorAtt.getBackendFormat() : null,
                colorAtt != null ? colorAtt.getSurfaceFlags() : 0,
                resolveAtt != null ? resolveAtt.getBackendFormat() : null,
                resolveAtt != null ? resolveAtt.getSurfaceFlags() : 0,
                depthStencilAtt != null ? depthStencilAtt.getBackendFormat() : null,
                depthStencilAtt != null ? depthStencilAtt.getSurfaceFlags() : 0,
                mSampleCount,
                mSurfaceFlags
        );
    }*/

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
     * Scratch key of {@link GpuRenderTarget}.
     */
    public static final class ResourceKey implements IResourceKey {

        public int mWidth;
        public int mHeight;
        public int mColorFormat;
        public int mResolveFormat;
        public int mDepthStencilFormat;
        public int mColorFlags;
        public int mResolveFlags;
        public int mDepthStencilFlags;
        public int mSurfaceFlags;

        /**
         * Update this key with the given arguments.
         *
         * @return this
         */
        @NonNull
        public ResourceKey compute(int width, int height,
                                   BackendFormat colorFormat,
                                   int colorSurfaceFlags,
                                   BackendFormat resolveFormat,
                                   int resolveSurfaceFlags,
                                   BackendFormat depthStencilFormat,
                                   int depthStencilSurfaceFlags,
                                   int sampleCount,
                                   int surfaceFlags) {
            assert (width > 0 && height > 0);
            mWidth = width;
            mHeight = height;
            mColorFormat = colorFormat != null ? colorFormat.getFormatKey() : 0;
            mResolveFormat = resolveFormat != null ? resolveFormat.getFormatKey() : 0;
            mDepthStencilFormat = depthStencilFormat != null ? depthStencilFormat.getFormatKey() : 0;
            mColorFlags = colorSurfaceFlags;
            mResolveFlags = resolveSurfaceFlags;
            mDepthStencilFlags = depthStencilSurfaceFlags;
            mSurfaceFlags = (surfaceFlags & 0) | (sampleCount << 16);
            return this;
        }

        @Override
        public IResourceKey copy() {
            return null;
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
            result = 31 * result + mSurfaceFlags;
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof ResourceKey key &&
                    mWidth == key.mWidth &&
                    mHeight == key.mHeight &&
                    mColorFormat == key.mColorFormat &&
                    mResolveFormat == key.mResolveFormat &&
                    mDepthStencilFormat == key.mDepthStencilFormat &&
                    mColorFlags == key.mColorFlags &&
                    mResolveFlags == key.mResolveFlags &&
                    mDepthStencilFlags == key.mDepthStencilFlags &&
                    mSurfaceFlags == key.mSurfaceFlags;
        }
    }
}
