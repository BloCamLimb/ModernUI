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

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.MathUtil;

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

    public static long computeSize(ImageInfo info) {
        long size = numBlocks(info.getCompressionType(), info.mWidth, info.mHeight) *
                info.getBytesPerBlock();
        assert size > 0;
        if (info.mMipLevelCount > 1) {
            // geometric sequence, S=a1(1-q^n)/(1-q), q=2^(-2)
            size = ((size - (size >> (info.mMipLevelCount << 1))) << 2) / 3;
        } else {
            size *= info.mSampleCount;
        }
        assert size > 0;
        return size;
    }

    public static int computeMipLevelCount(int width, int height, int depth) {
        return MathUtil.floorLog2(Math.max(Math.max(width, height), depth)) + 1; // +1 base level 0
    }

    private DataUtils() {
    }
}
