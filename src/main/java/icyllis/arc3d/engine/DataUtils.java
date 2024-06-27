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

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.MathUtil;

//TODO rename to ImageUtils?
public final class DataUtils {

    public static boolean compressionTypeIsOpaque(@ColorInfo.CompressionType int compression) {
        return switch (compression) {
            case ColorInfo.COMPRESSION_NONE,
                    ColorInfo.COMPRESSION_BC1_RGB8_UNORM,
                    ColorInfo.COMPRESSION_ETC2_RGB8_UNORM -> true;
            case ColorInfo.COMPRESSION_BC1_RGBA8_UNORM -> false;
            default -> throw new AssertionError(compression);
        };
    }

    public static int num4x4Blocks(int size) {
        return (size + 3) >> 2;
    }

    public static long numBlocks(@ColorInfo.CompressionType int compression, int width, int height) {
        return switch (compression) {
            case ColorInfo.COMPRESSION_NONE -> (long) width * height;
            case ColorInfo.COMPRESSION_ETC2_RGB8_UNORM,
                    ColorInfo.COMPRESSION_BC1_RGB8_UNORM,
                    ColorInfo.COMPRESSION_BC1_RGBA8_UNORM -> {
                long numBlocksWidth = num4x4Blocks(width);
                long numBlocksHeight = num4x4Blocks(height);
                yield numBlocksWidth * numBlocksHeight;
            }
            default -> throw new AssertionError(compression);
        };
    }

    public static int numBlocks(@ColorInfo.CompressionType int compression, int size) {
        return switch (compression) {
            case ColorInfo.COMPRESSION_NONE -> size;
            case ColorInfo.COMPRESSION_ETC2_RGB8_UNORM,
                    ColorInfo.COMPRESSION_BC1_RGB8_UNORM,
                    ColorInfo.COMPRESSION_BC1_RGBA8_UNORM -> num4x4Blocks(size);
            default -> throw new AssertionError(compression);
        };
    }

    public static long computeSize(ImageDesc desc) {
        long size = numBlocks(desc.getCompressionType(), desc.mWidth, desc.mHeight) *
                desc.getBytesPerBlock();
        assert size > 0;
        if (desc.mMipLevelCount > 1) {
            // geometric sequence, S=a1(1-q^n)/(1-q), q=2^(-2)
            size = ((size - (size >> (desc.mMipLevelCount << 1))) << 2) / 3;
        } else if (desc.mSampleCount > 1) {
            size *= desc.mSampleCount;
        }
        assert size > 0;
        return size;
    }

    public static int computeMipLevelCount(int width, int height, int depth) {
        return MathUtil.floorLog2(Math.max(Math.max(width, height), depth)) + 1; // +1 base level 0
    }

    /**
     * Compute the size of the buffer required to hold all the mipLevels of the specified type
     * of data when all rowBytes are tight.
     * <p>
     * Note there may still be padding between the mipLevels to meet alignment requirements.
     * <p>
     * Returns total buffer size to allocate, and required offset alignment of that allocation.
     * Updates 'mipOffsetsAndRowBytes' with offsets relative to start of the allocation, as well as
     * the aligned destination rowBytes for each level.
     * <p>
     * The last pair of 'mipOffsetsAndRowBytes' holds combined buffer size and required alignment.
     */
    public static long computeCombinedBufferSize(
            int mipLevelCount,
            int bytesPerBlock,
            int width, int height,
            int compressionType,
            long[] mipOffsetsAndRowBytes
    ) {
        assert mipLevelCount >= 1;
        assert mipOffsetsAndRowBytes.length >= (mipLevelCount + 1) * 2;

        // transfer buffer requires 4-byte aligned
        long minTransferBufferAlignment = Math.max(bytesPerBlock, 4);

        long combinedBufferSize = 0;

        for (int mipLevel = 0; mipLevel < mipLevelCount; mipLevel++) {

            int compressedBlockWidth = numBlocks(compressionType,
                    width);
            int compressedBlockHeight = numBlocks(compressionType,
                    height);

            long rowBytes = (long) compressedBlockWidth * bytesPerBlock;
            long alignedSize = MathUtil.alignTo(
                    rowBytes * compressedBlockHeight,
                    minTransferBufferAlignment
            );

            mipOffsetsAndRowBytes[mipLevel * 2] = combinedBufferSize;
            mipOffsetsAndRowBytes[mipLevel * 2 + 1] = rowBytes;
            combinedBufferSize += alignedSize;

            width = Math.max(1, width >> 1);
            height = Math.max(1, height >> 1);
        }

        mipOffsetsAndRowBytes[mipLevelCount * 2] = combinedBufferSize;
        mipOffsetsAndRowBytes[mipLevelCount * 2 + 1] = minTransferBufferAlignment;
        return combinedBufferSize;
    }

    private DataUtils() {
    }
}
