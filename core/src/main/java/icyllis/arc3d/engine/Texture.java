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

package icyllis.arc3d.engine;

import icyllis.modernui.graphics.SharedPtr;
import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.RefCnt;

/**
 * Represents 2D textures can be sampled by shaders, can also be used as attachments
 * of render targets.
 * <p>
 * By default, a Texture is not renderable (not implements {@link RenderTarget}), all
 * mipmaps (including the base level) are dirty. But it can be renderable on
 * creation, then we call it a RenderTexture or TextureRenderTarget. The texture
 * will be the main color buffer of the single sample framebuffer of the render target.
 * So we can cache these framebuffers with texture. Additionally, it may create more
 * surfaces and attach them to it. These surfaces are budgeted but cannot be reused.
 * In most cases, we reuse textures, so these surfaces are reused together, see
 * {@link FramebufferSet}.
 */
public abstract class Texture extends Resource implements Surface {

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

    protected Texture(Device device, int width, int height) {
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

    /**
     * @return true if this surface has mipmaps and have been allocated
     */
    public final boolean isMipmapped() {
        return (mFlags & FLAG_MIPMAPPED) != 0;
    }

    /**
     * The pixel values of this surface cannot be modified (e.g. doesn't support write pixels or
     * mipmap regeneration). To be exact, only wrapped textures, external textures, stencil
     * attachments and MSAA color attachments can be read only.
     *
     * @return true if pixels in this surface are read-only
     */
    public final boolean isReadOnly() {
        return (mFlags & FLAG_READ_ONLY) != 0;
    }

    /**
     * @return true if we are working with protected content
     */
    public final boolean isProtected() {
        return (mFlags & FLAG_PROTECTED) != 0;
    }

    /**
     * Surface flags, but no render target level flags.
     *
     * <ul>
     * <li>{@link #FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link Texture} only.
     * </li>
     *
     * <li>{@link #FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Texture} only.
     * </li>
     *
     * <li>{@link #FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #FLAG_PROTECTED} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link #FLAG_READ_ONLY} -
     *  Means the pixels in the texture are read-only. {@link Texture} only.
     * </li>
     *
     * @return combination of the above flags
     */
    @Override
    public final int getSurfaceFlags() {
        int flags = mFlags;
        if (getBudgetType() == Engine.BudgetType.Budgeted) {
            flags |= Surface.FLAG_BUDGETED;
        }
        return flags;
    }

    /**
     * @return external texture
     */
    public abstract boolean isExternal();

    /**
     * @return the backend texture of this texture
     */
    @NonNull
    public abstract BackendTexture getBackendTexture();

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

    public abstract int getMaxMipmapLevel();

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
        assert (getBudgetType() == Engine.BudgetType.Budgeted);
        return new ScratchKey().compute(
                format,
                mWidth, mHeight,
                1,
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
            width = ResourceProvider.makeApprox(width);
            height = ResourceProvider.makeApprox(height);
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
            width = ResourceProvider.makeApprox(width);
            height = ResourceProvider.makeApprox(height);
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
     * Storage key of {@link Texture}, may be compared with {@link TextureProxy}.
     */
    public static final class ScratchKey {

        public int mWidth;
        public int mHeight;
        public int mFormat;
        public int mFlags;

        /**
         * Compute a {@link Texture} key, format can not be compressed.
         *
         * @return the scratch key
         */
        @NonNull
        public ScratchKey compute(BackendFormat format,
                                  int width, int height,
                                  int sampleCount,
                                  int surfaceFlags) {
            assert (width > 0 && height > 0);
            assert (!format.isCompressed());
            mWidth = width;
            mHeight = height;
            mFormat = format.getFormatKey();
            mFlags = (surfaceFlags & (Surface.FLAG_MIPMAPPED |
                    Surface.FLAG_RENDERABLE |
                    Surface.FLAG_PROTECTED)) | (sampleCount << 16);
            return this;
        }

        /**
         * Keep {@link TextureProxy#hashCode()} sync with this.
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
         * Keep {@link TextureProxy#equals(Object)}} sync with this.
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
