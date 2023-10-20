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

import icyllis.arc3d.core.*;
import org.lwjgl.system.libc.LibCString;

public final class DataUtils {

    public static boolean compressionTypeIsOpaque(int compression) {
        return switch (compression) {
            case ImageInfo.COMPRESSION_NONE,
                    ImageInfo.COMPRESSION_BC1_RGB8_UNORM,
                    ImageInfo.COMPRESSION_ETC2_RGB8_UNORM -> true;
            case ImageInfo.COMPRESSION_BC1_RGBA8_UNORM -> false;
            default -> throw new AssertionError(compression);
        };
    }

    public static int num4x4Blocks(int size) {
        return (size + 3) >> 2;
    }

    public static long numBlocks(int compression, int width, int height) {
        return switch (compression) {
            case ImageInfo.COMPRESSION_NONE -> (long) width * height;
            case ImageInfo.COMPRESSION_ETC2_RGB8_UNORM,
                    ImageInfo.COMPRESSION_BC1_RGB8_UNORM,
                    ImageInfo.COMPRESSION_BC1_RGBA8_UNORM -> {
                long numBlocksWidth = num4x4Blocks(width);
                long numBlocksHeight = num4x4Blocks(height);
                yield numBlocksWidth * numBlocksHeight;
            }
            default -> throw new AssertionError(compression);
        };
    }

    /**
     * Mem copy row by row.
     */
    public static void copyImage(long src, long srcRowBytes,
                                 long dst, long dstRowBytes,
                                 long minRowBytes, int rows) {
        if (srcRowBytes == minRowBytes && dstRowBytes == minRowBytes) {
            LibCString.nmemcpy(dst, src, minRowBytes * rows);
        } else {
            assert srcRowBytes >= minRowBytes;
            assert dstRowBytes >= minRowBytes;
            while (rows-- != 0) {
                LibCString.nmemcpy(dst, src, minRowBytes);
                src += srcRowBytes;
                dst += dstRowBytes;
            }
        }
    }

    private DataUtils() {
    }
}
