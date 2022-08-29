/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.Image;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class DataUtils {

    public static final Unsafe UNSAFE;

    static {
        try {
            Field unsafe = MemoryUtil.class.getDeclaredField("UNSAFE");
            unsafe.setAccessible(true);
            UNSAFE = (Unsafe) unsafe.get(null);
        } catch (Exception e) {
            throw new UnsupportedOperationException("No UNSAFE", e);
        }
    }

    public static boolean compressionTypeIsOpaque(int compression) {
        return switch (compression) {
            case Image.COMPRESSION_NONE,
                    Image.COMPRESSION_BC1_RGB8_UNORM,
                    Image.COMPRESSION_ETC2_RGB8_UNORM -> true;
            case Image.COMPRESSION_BC1_RGBA8_UNORM -> false;
            default -> throw new IllegalArgumentException();
        };
    }

    public static int num4x4Blocks(int size) {
        return ((size + 3) & ~3) >> 2;
    }

    public static long numBlocks(int compression, int width, int height) {
        return switch (compression) {
            case Image.COMPRESSION_NONE -> (long) width * height;
            case Image.COMPRESSION_ETC2_RGB8_UNORM,
                    Image.COMPRESSION_BC1_RGB8_UNORM,
                    Image.COMPRESSION_BC1_RGBA8_UNORM -> {
                long numBlocksWidth = num4x4Blocks(width);
                long numBlocksHeight = num4x4Blocks(height);
                yield numBlocksWidth * numBlocksHeight;
            }
            default -> throw new IllegalArgumentException();
        };
    }

    private DataUtils() {
    }
}
