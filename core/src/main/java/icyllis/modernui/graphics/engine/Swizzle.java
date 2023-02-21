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

package icyllis.modernui.graphics.engine;

import javax.annotation.Nonnull;

/**
 * Represents a rgba swizzle. It's packed as a <code>short</code>.
 * <p>
 * Note: max swizzle value is 0x5555, so (AND 0xFFFF) is not required when implicitly cast to int.
 */
public final class Swizzle {

    // default value
    public static final short RGBA = 0x3210;
    public static final short BGRA = 0x3012;
    public static final short RGB1 = 0x5210;
    public static final short AAAA = 0x3333;

    static {
        // make them inline at compile-time
        assert make('r', 'g', 'b', 'a') == RGBA;
        assert make('b', 'g', 'r', 'a') == BGRA;
        assert make('r', 'g', 'b', '1') == RGB1;
        assert make('a', 'a', 'a', 'a') == AAAA;
    }

    public static int charToIndex(char c) {
        return switch (c) {
            // r...a must map to 0...3 because other methods use them as indices into mSwiz.
            case 'r' -> 0;
            case 'g' -> 1;
            case 'b' -> 2;
            case 'a' -> 3;
            case '0' -> 4;
            case '1' -> 5;
            default -> throw new IllegalArgumentException();
        };
    }

    public static char indexToChar(int idx) {
        return switch (idx) {
            case 0 -> 'r';
            case 1 -> 'g';
            case 2 -> 'b';
            case 3 -> 'a';
            case 4 -> '0';
            case 5 -> '1';
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Compact representation of the swizzle suitable for a key. Letters must be lowercase.
     */
    public static short make(@Nonnull String s) {
        return make(s.charAt(0), s.charAt(1), s.charAt(2), s.charAt(3));
    }

    /**
     * Compact representation of the swizzle suitable for a key. Letters must be lowercase.
     */
    public static short make(char r, char g, char b, char a) {
        return (short) (charToIndex(r) | (charToIndex(g) << 4) | (charToIndex(b) << 8) | (charToIndex(a) << 12));
    }

    public static short concat(short a, short b) {
        short swizzle = 0;
        for (int i = 0; i < 4; ++i) {
            int idx = (b >> (4 * i)) & 0xF;
            if (idx != 4 && idx != 5) {
                assert idx < 4;
                // Get the index value stored in 'a' at location 'idx'.
                idx = (a >> (4 * idx)) & 0xF;
            }
            swizzle |= (idx << (4 * i));
        }
        return swizzle;
    }

    /**
     * Applies this swizzle to the input color and returns the swizzled color.
     */
    public static void apply(short swizzle, @Nonnull float[] color) {
        float r = color[0], g = color[1], b = color[2], a = color[3];
        for (int i = 0; i < 4; ++i) {
            color[i] = switch (swizzle & 0xF) {
                case 0 -> r;
                case 1 -> g;
                case 2 -> b;
                case 3 -> a;
                case 4 -> 0.0f;
                case 5 -> 1.0f;
                default -> throw new IllegalStateException();
            };
            swizzle >>= 4;
        }
    }

    @Nonnull
    public static String toString(short swizzle) {
        return String.valueOf(indexToChar(swizzle & 0xF)) +
                indexToChar((swizzle >> 4) & 0xF) +
                indexToChar((swizzle >> 8) & 0xF) +
                indexToChar(swizzle >>> 12);
    }
}
