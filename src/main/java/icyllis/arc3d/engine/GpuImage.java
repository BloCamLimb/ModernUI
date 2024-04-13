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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arc3d.engine.Engine.BudgetType;

/**
 * Represents GPU images, which may be 2D or 3D. This class also represents the image view
 * type, see {@link Engine.ImageType}.
 * <p>
 * A {@link GpuImage} may be used as textures (sampled in fragment shaders), may be used as
 * storage images (load and store in compute shaders), may be used as color and depth/stencil
 * attachments of a framebuffer. See {@link ISurface#FLAG_TEXTURABLE} and {@link ISurface#FLAG_RENDERABLE}.
 * Texture (sampled image) is a specialization of GPU images.
 */
public abstract non-sealed class GpuImage extends GpuSurface {

    protected final int mWidth;
    protected final int mHeight;

    /**
     * Note: budgeted is a dynamic state, it can be returned by {@link #getSurfaceFlags()}.
     * This field is OR-ed only and immutable when created.
     */
    protected int mFlags;

    /**
     * Only valid when isMipmapped=true.
     * By default, we can't say mipmaps dirty or not, since texel data is undefined.
     */
    private boolean mMipmapsDirty;

    @SharedPtr
    private ReleaseCallback mReleaseCallback;

    protected GpuImage(GpuDevice device, int width, int height) {
        super(device);
        assert width > 0 && height > 0;
        mWidth = width;
        mHeight = height;
    }

    /**
     * @return the width of the texture
     */
    @Override
    public final int getWidth() {
        return mWidth;
    }

    /**
     * @return the height of the texture
     */
    @Override
    public final int getHeight() {
        return mHeight;
    }

    @Override
    public int getDepthBits() {
        return getBackendFormat().getDepthBits();
    }

    @Override
    public int getStencilBits() {
        return getBackendFormat().getStencilBits();
    }

    /**
     * @return true if this surface has mipmaps and have been allocated
     */
    public final boolean isMipmapped() {
        return (mFlags & ISurface.FLAG_MIPMAPPED) != 0;
    }

    /**
     * @return true if this image can be used as textures and storage images
     */
    public final boolean isTexturable() {
        return (mFlags & ISurface.FLAG_TEXTURABLE) != 0;
    }

    /**
     * @return true if this image can be used as color and depth/stencil attachments
     */
    public final boolean isRenderable() {
        return (mFlags & ISurface.FLAG_RENDERABLE) != 0;
    }

    /**
     * @return number of mipmap levels, greater than 1 if mipmapped
     */
    public abstract int getMipLevelCount();

    /**
     * The pixel values of this surface cannot be modified (e.g. doesn't support write pixels or
     * mipmap regeneration). To be exact, only wrapped textures, external textures, stencil
     * attachments and MSAA color attachments can be read only.
     *
     * @return true if pixels in this surface are read-only
     */
    public final boolean isReadOnly() {
        return (mFlags & ISurface.FLAG_READ_ONLY) != 0;
    }

    /**
     * @return true if we are working with protected content
     */
    public final boolean isProtected() {
        return (mFlags & ISurface.FLAG_PROTECTED) != 0;
    }

    /**
     * Surface flags, but no render target level flags.
     *
     * <ul>
     * <li>{@link ISurface#FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link GpuImage} only.
     * </li>
     *
     * <li>{@link ISurface#FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link GpuImage} only.
     * </li>
     *
     * <li>{@link ISurface#FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link ISurface#FLAG_PROTECTED} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link ISurface#FLAG_READ_ONLY} -
     *  Means the pixels in the texture are read-only. {@link GpuImage} only.
     * </li>
     *
     * @return combination of the above flags
     */
    @Override
    public final int getSurfaceFlags() {
        int flags = mFlags;
        if (getBudgetType() == BudgetType.Budgeted) {
            flags |= ISurface.FLAG_BUDGETED;
        }
        return flags;
    }

    /**
     * Return <code>true</code> if mipmaps are dirty and need to regenerate before sampling.
     * The value is valid only when {@link #isMipmapped()} returns <code>true</code>.
     *
     * @return whether mipmaps are dirty
     */
    public final boolean isMipmapsDirty() {
        assert isMipmapped();
        return mMipmapsDirty && isMipmapped();
    }

    /**
     * Set whether mipmaps are dirty or not. Call only when {@link #isMipmapped()} returns <code>true</code>.
     *
     * @param mipmapsDirty whether mipmaps are dirty
     */
    public final void setMipmapsDirty(boolean mipmapsDirty) {
        assert isMipmapped();
        mMipmapsDirty = mipmapsDirty;
    }

    /**
     * @return the backend texture of this texture
     */
    @Nonnull
    public abstract BackendImage getBackendTexture();

    /**
     * @return external texture
     */
    public abstract boolean isExternal();

    /**
     * Unmanaged backends (e.g. Vulkan) may want to specially handle the release proc in order to
     * ensure it isn't called until GPU work related to the resource is completed.
     */
    public void setReleaseCallback(@SharedPtr ReleaseCallback callback) {
        mReleaseCallback = RefCnt.move(mReleaseCallback, callback);
    }

    @Override
    public final GpuImage asImage() {
        return this;
    }

    @Override
    protected void onRelease() {
        if (mReleaseCallback != null) {
            mReleaseCallback.unref();
        }
        mReleaseCallback = null;
    }

    @Override
    protected void onDiscard() {
        if (mReleaseCallback != null) {
            mReleaseCallback.unref();
        }
        mReleaseCallback = null;
    }

    @Nullable
    @Override
    protected ScratchKey computeScratchKey() {
        BackendFormat format = getBackendFormat();
        if (format.isCompressed()) {
            return null;
        }
        assert (getBudgetType() != BudgetType.WrapCacheable);
        return new ScratchKey().compute(
                format,
                mWidth, mHeight,
                getSampleCount(),
                mFlags); // budgeted flag is not included, this method is called only when budgeted
    }

    public static long computeSize(BackendFormat format,
                                   int width, int height,
                                   int sampleCount,
                                   boolean mipmapped,
                                   boolean approx) {
        assert width > 0 && height > 0;
        assert sampleCount > 0;
        assert sampleCount == 1 || !mipmapped;
        // For external formats we do not actually know the real size of the resource, so we just return
        // 0 here to indicate this.
        if (format.isExternal()) {
            return 0;
        }
        if (approx) {
            width = ISurface.getApproxSize(width);
            height = ISurface.getApproxSize(height);
        }
        long size = DataUtils.numBlocks(format.getCompressionType(), width, height) *
                format.getBytesPerBlock();
        assert size > 0;
        if (mipmapped) {
            size = (size << 2) / 3;
        } else {
            size *= sampleCount;
        }
        assert size > 0;
        return size;
    }

    public static long computeSize(BackendFormat format,
                                   int width, int height,
                                   int sampleCount,
                                   int levelCount) {
        return computeSize(format, width, height, sampleCount, levelCount, false);
    }

    public static long computeSize(BackendFormat format,
                                   int width, int height,
                                   int sampleCount,
                                   int levelCount,
                                   boolean approx) {
        assert width > 0 && height > 0;
        assert sampleCount > 0 && levelCount > 0;
        assert sampleCount == 1 || levelCount == 1;
        // For external formats we do not actually know the real size of the resource, so we just return
        // 0 here to indicate this.
        if (format.isExternal()) {
            return 0;
        }
        if (approx) {
            width = ISurface.getApproxSize(width);
            height = ISurface.getApproxSize(height);
        }
        long size = DataUtils.numBlocks(format.getCompressionType(), width, height) *
                format.getBytesPerBlock();
        assert size > 0;
        if (levelCount > 1) {
            // geometric sequence, S=a1(1-q^n)/(1-q), q=2^(-2)
            size = ((size - (size >> (levelCount << 1))) << 2) / 3;
        } else {
            size *= sampleCount;
        }
        assert size > 0;
        return size;
    }

    /**
     * Storage key of {@link GpuImage}, may be compared with {@link ImageProxy}.
     */
    public static final class ScratchKey implements IScratchKey {

        public int mWidth;
        public int mHeight;
        public int mFormat;
        public int mFlags;

        /**
         * Update this key with the given arguments, format can not be compressed.
         *
         * @return this
         */
        @Nonnull
        public ScratchKey compute(BackendFormat format,
                                  int width, int height,
                                  int sampleCount,
                                  int surfaceFlags) {
            assert (width > 0 && height > 0);
            assert (!format.isCompressed());
            mWidth = width;
            mHeight = height;
            mFormat = format.getFormatKey();
            mFlags = (surfaceFlags & (ISurface.FLAG_MIPMAPPED |
                    ISurface.FLAG_TEXTURABLE |
                    ISurface.FLAG_RENDERABLE |
                    ISurface.FLAG_MEMORYLESS |
                    ISurface.FLAG_PROTECTED)) | (sampleCount << 16);
            return this;
        }

        /**
         * Keep {@link ImageProxy#hashCode()} sync with this.
         */
        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mFormat;
            result = 31 * result + mFlags;
            return result;
        }

        /**
         * Keep {@link ImageProxy#equals(Object)}} sync with this.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof ScratchKey key &&
                    mWidth == key.mWidth &&
                    mHeight == key.mHeight &&
                    mFormat == key.mFormat &&
                    mFlags == key.mFlags;
        }
    }
}
