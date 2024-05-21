/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.ColorInfo;
import org.lwjgl.system.NativeType;

import javax.annotation.concurrent.Immutable;

/**
 * Contains backend-specific parameters used to create GPU images, which are:
 * mip level count, sample count, image format, image view type,
 * image create flags, image usage flags, width, height, depth, and array
 * layer count.
 * <p>
 * All parameters are validated by {@link Caps}.
 */
@Immutable
public class ImageDesc {

    /**
     * {@link ImageDesc} is always non-null, an empty instance may be used
     * to represent invalid image info.
     *
     * @see #isValid()
     */
    public static final ImageDesc EMPTY = new ImageDesc();

    protected final int mWidth;
    protected final int mHeight;
    protected final short mDepth;
    protected final short mArraySize;
    protected final byte mMipLevelCount;
    protected final byte mSampleCount;
    protected final int mFlags;

    private ImageDesc() {
        mWidth = mHeight = mDepth = 0;
        mArraySize = mMipLevelCount = mSampleCount = 1;
        mFlags = 0;
    }

    protected ImageDesc(int width, int height,
                        int depth, int arraySize,
                        int mipLevelCount, int sampleCount,
                        int flags) {
        assert width > 0 && height > 0 &&
                depth > 0 && arraySize > 0 &&
                mipLevelCount > 0 && sampleCount > 0;
        assert mipLevelCount == 1 || sampleCount == 1;
        mWidth = width;
        mHeight = height;
        mDepth = (short) depth;
        mArraySize = (short) arraySize;
        mMipLevelCount = (byte) mipLevelCount;
        mSampleCount = (byte) sampleCount;
        mFlags = flags;
    }

    public final boolean isValid() {
        return this != EMPTY;
    }

    /**
     * @return see {@link Engine.BackendApi}
     */
    public int getBackend() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the default image view type.
     *
     * @return see {@link Engine.ImageType}
     */
    public byte getImageType() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the width in texels
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * @return the height in texels
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * @return the depth in texels
     */
    public final int getDepth() {
        return mDepth;
    }

    public final int getArraySize() {
        return mArraySize;
    }

    public final boolean isMipmapped() {
        return mMipLevelCount > 1;
    }

    public final boolean isMultisampled() {
        return mSampleCount > 1;
    }

    /**
     * Returns the number of mip levels, greater than 1 if mipmapped
     */
    public final int getMipLevelCount() {
        return mMipLevelCount;
    }

    /**
     * Returns the number of samples, greater than 1 if multisampled.
     */
    public final int getSampleCount() {
        return mSampleCount;
    }

    /**
     * @return true if this image can be used as textures
     */
    public final boolean isSampledImage() {
        return (mFlags & ISurface.FLAG_SAMPLED_IMAGE) != 0;
    }

    /**
     * @return true if this image can be used as storage images.
     */
    public final boolean isStorageImage() {
        return (mFlags & ISurface.FLAG_STORAGE_IMAGE) != 0;
    }

    /**
     * @return true if this image can be used as color and depth/stencil attachments
     */
    public final boolean isRenderable() {
        return (mFlags & ISurface.FLAG_RENDERABLE) != 0;
    }

    public boolean isProtected() {
        throw new UnsupportedOperationException();
    }

    /**
     * If the backend API is OpenGL this gets the format as a GLenum.
     */
    @NativeType("GLenum")
    public int getGLFormat() {
        throw new IllegalStateException();
    }

    /**
     * If the backend API is Vulkan this gets the format as a VkFormat.
     */
    @NativeType("VkFormat")
    public int getVkFormat() {
        throw new IllegalStateException();
    }

    /**
     * Gets the channels present in the format as a bitfield of ColorChannelFlag values.
     *
     * @see Color#COLOR_CHANNEL_FLAGS_RGBA
     */
    public int getChannelFlags() {
        throw new UnsupportedOperationException();
    }

    public boolean isSRGB() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ColorInfo#COMPRESSION_NONE
     */
    @ColorInfo.CompressionType
    public int getCompressionType() {
        throw new UnsupportedOperationException();
    }

    public final boolean isCompressed() {
        return getCompressionType() != ColorInfo.COMPRESSION_NONE;
    }

    /**
     * @return if compressed, bytes per block, otherwise bytes per pixel
     */
    public int getBytesPerBlock() {
        return 0;
    }

    public int getDepthBits() {
        return 0;
    }

    public int getStencilBits() {
        return 0;
    }

    // No hashCode() and equals() implementation here
    // because the only instance of this base class is EMPTY singleton
}
