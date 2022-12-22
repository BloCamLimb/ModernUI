/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.Core;
import icyllis.akashigi.core.FMath;

public final class DataUtils {

    public static boolean compressionTypeIsOpaque(int compression) {
        return switch (compression) {
            case Core.CompressionType.kNone,
                    Core.CompressionType.kBC1_RGB8_UNORM,
                    Core.CompressionType.kETC2_RGB8_UNORM -> true;
            case Core.CompressionType.kBC1_RGBA8_UNORM -> false;
            default -> throw new AssertionError(compression);
        };
    }

    public static int num4x4Blocks(int size) {
        return FMath.align4(size) >> 2;
    }

    public static long numBlocks(int compression, int width, int height) {
        return switch (compression) {
            case Core.CompressionType.kNone -> (long) width * height;
            case Core.CompressionType.kETC2_RGB8_UNORM,
                    Core.CompressionType.kBC1_RGB8_UNORM,
                    Core.CompressionType.kBC1_RGBA8_UNORM -> {
                long numBlocksWidth = num4x4Blocks(width);
                long numBlocksHeight = num4x4Blocks(height);
                yield numBlocksWidth * numBlocksHeight;
            }
            default -> throw new AssertionError(compression);
        };
    }

    private DataUtils() {
    }
}
