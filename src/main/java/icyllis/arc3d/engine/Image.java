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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents GPU image resources, which may be 2D or 3D. This class also represents a
 * default image view, see {@link Engine.ImageType}.
 * <p>
 * {@link Image} can be used for various purposes. It may be used as textures (sampled in
 * fragment shaders), may be used as storage images (load and store in compute shaders),
 * may be used as color and depth/stencil attachments of framebuffers (render targets).
 * See {@link ISurface#FLAG_SAMPLED_IMAGE}, {@link ISurface#FLAG_STORAGE_IMAGE} and
 * {@link ISurface#FLAG_RENDERABLE}.
 * <p>
 * An {@link Image} is created with device-local memory, its contents may be updated via
 * a staging buffer.
 */
public abstract class Image extends Resource {

    private final ImageDesc mDesc;
    private final ImageMutableState mMutableState;

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

    protected Image(Context context,
                    boolean budgeted,
                    boolean wrapped,
                    ImageDesc desc,
                    ImageMutableState mutableState) {
        super(context, budgeted, wrapped, DataUtils.computeSize(desc));
        mDesc = desc;
        mMutableState = mutableState;
    }

    @NonNull
    public final ImageDesc getDesc() {
        return mDesc;
    }

    public final ImageMutableState getMutableState() {
        return mMutableState;
    }

    /**
     * Returns the default image view type.
     *
     * @return see {@link Engine.ImageType}
     */
    public final int getImageType() {
        return mDesc.getImageType();
    }

    /**
     * @return the width of the image in texels, greater than zero
     */
    public final int getWidth() {
        return mDesc.mWidth;
    }

    /**
     * @return the height of the image in texels, greater than zero
     */
    public final int getHeight() {
        return mDesc.mHeight;
    }

    /**
     * @return the depth of the image in texels, greater than zero
     */
    public final int getDepth() {
        return mDesc.mDepth;
    }

    public final int getArraySize() {
        return mDesc.mArraySize;
    }

    public int getDepthBits() {
        return mDesc.getDepthBits();
    }

    public int getStencilBits() {
        return mDesc.getStencilBits();
    }

    /**
     * @return true if this image has mipmaps and have been allocated
     */
    public final boolean isMipmapped() {
        return mDesc.isMipmapped();
    }

    /**
     * @return true if this image can be used as textures
     */
    public final boolean isSampledImage() {
        return mDesc.isSampledImage();
    }

    /**
     * @return true if this image can be used as storage images.
     */
    public final boolean isStorageImage() {
        return mDesc.isStorageImage();
    }

    /**
     * @return true if this image can be used as color and depth/stencil attachments
     */
    public final boolean isRenderable() {
        return mDesc.isRenderable();
    }

    /**
     * @return number of mipmap levels, greater than 1 if mipmapped
     */
    public final int getMipLevelCount() {
        return mDesc.mMipLevelCount;
    }

    /**
     * Returns the number of samples per pixel.
     *
     * @return the number of samples, greater than (multi-sampled) or equal to one
     */
    public final int getSampleCount() {
        return mDesc.mSampleCount;
    }

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
        return mDesc.isProtected();
    }

    /**
     * Surface flags, but no render target level flags.
     *
     * <ul>
     * <li>{@link ISurface#FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link Image} only.
     * </li>
     *
     * <li>{@link ISurface#FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Image} only.
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
     *  Means the pixels in the texture are read-only. {@link Image} only.
     * </li>
     *
     * @return combination of the above flags
     */
    @Deprecated
    final int getSurfaceFlags() {
        int flags = mFlags;
        if (isBudgeted()) {
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
     * Unmanaged backends (e.g. Vulkan) may want to specially handle the release proc in order to
     * ensure it isn't called until GPU work related to the resource is completed.
     */
    public void setReleaseCallback(@SharedPtr ReleaseCallback callback) {
        mReleaseCallback = RefCnt.move(mReleaseCallback, callback);
    }

    @Override
    protected void onRelease() {
        if (mReleaseCallback != null) {
            mReleaseCallback.unref();
        }
        mReleaseCallback = null;
    }

    /*@Nullable
    @Override
    protected ScratchKey computeScratchKey() {
        BackendFormat format = getBackendFormat();
        if (format.isCompressed()) {
            return null;
        }
        assert (getBudgetType() != BudgetType.WrapCacheable);
        return new ScratchKey().compute(
                format,
                getWidth(), getHeight(),
                getSampleCount(),
                mFlags); // budgeted flag is not included, this method is called only when budgeted
    }*/

    /**
     * Storage key of {@link Image}, may be compared with {@link ImageViewProxy}.
     */
    public static final class ResourceKey implements IResourceKey {

        public int mWidth;
        public int mHeight;
        public int mFormat;
        public int mFlags;

        /**
         * Update this key with the given arguments, format can not be compressed.
         *
         * @return this
         */
        @NonNull
        public ResourceKey compute(BackendFormat format,
                                   int width, int height,
                                   int sampleCount,
                                   int surfaceFlags) {
            assert (width > 0 && height > 0);
            assert (!format.isCompressed());
            mWidth = width;
            mHeight = height;
            mFormat = format.getFormatKey();
            mFlags = (surfaceFlags & (ISurface.FLAG_MIPMAPPED |
                    ISurface.FLAG_SAMPLED_IMAGE |
                    ISurface.FLAG_RENDERABLE |
                    ISurface.FLAG_MEMORYLESS |
                    ISurface.FLAG_PROTECTED)) | (sampleCount << 16);
            return this;
        }

        @Override
        public IResourceKey copy() {
            return null;
        }

        /**
         * Keep {@link ImageViewProxy#hashCode()} sync with this.
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
         * Keep {@link ImageViewProxy#equals(Object)}} sync with this.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof ResourceKey key &&
                    mWidth == key.mWidth &&
                    mHeight == key.mHeight &&
                    mFormat == key.mFormat &&
                    mFlags == key.mFlags;
        }
    }
}
