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

import icyllis.arc3d.core.Size;
import org.jetbrains.annotations.Contract;

/**
 * Represents a color component mapping. It's packed as a <code>short</code> value.
 * <p>
 * <b>Do NOT change the packing format, there are inlined code in other places</b>.
 */
//TODO Project Valhalla, make this as primitive (record) class
public final class Swizzle {

    // default value
    public static final short RGBA = 0x3210;
    public static final short BGRA = 0x3012;
    public static final short RGB1 = 0x5210;
    public static final short BGR1 = 0x5012;
    public static final short AAAA = 0x3333;

    public static final short INVALID = (short) 0xFFFF;

    static {
        // make them inline at compile-time
        assert make('r', 'g', 'b', 'a') == RGBA;
        assert make('b', 'g', 'r', 'a') == BGRA;
        assert make('r', 'g', 'b', '1') == RGB1;
        assert make('b', 'g', 'r', '1') == BGR1;
        assert make('a', 'a', 'a', 'a') == AAAA;
        assert concat(make('1', '1', '1', 'r'), AAAA) == make('r', 'r', 'r', 'r');
    }

    // r...a must map to 0...3 because other methods use them as indices into mSwiz.
    public static final int
            COMPONENT_R    = 0,
            COMPONENT_G    = 1,
            COMPONENT_B    = 2,
            COMPONENT_A    = 3,
            COMPONENT_ZERO = 4,
            COMPONENT_ONE  = 5;

    @Contract(pure = true)
    public static int charToIndex(char c) {
        return switch (c) {
            case 'r' -> COMPONENT_R;
            case 'g' -> COMPONENT_G;
            case 'b' -> COMPONENT_B;
            case 'a' -> COMPONENT_A;
            case '0' -> COMPONENT_ZERO;
            case '1' -> COMPONENT_ONE;
            default -> throw new AssertionError(c);
        };
    }

    @Contract(pure = true)
    public static char indexToChar(int idx) {
        return switch (idx) {
            case COMPONENT_R    -> 'r';
            case COMPONENT_G    -> 'g';
            case COMPONENT_B    -> 'b';
            case COMPONENT_A    -> 'a';
            case COMPONENT_ZERO -> '0';
            case COMPONENT_ONE  -> '1';
            default -> throw new AssertionError(idx);
        };
    }

    /**
     * Compact representation of the swizzle suitable for a key. Letters must be lowercase.
     */
    @Contract(pure = true)
    public static short make(CharSequence s) {
        return make(s.charAt(0), s.charAt(1), s.charAt(2), s.charAt(3));
    }

    /**
     * Compact representation of the swizzle suitable for a key. Letters must be lowercase.
     */
    @Contract(pure = true)
    public static short make(char r, char g, char b, char a) {
        return make(charToIndex(r), charToIndex(g), charToIndex(b), charToIndex(a));
    }

    @Contract(pure = true)
    public static short make(int r, int g, int b, int a) {
        return (short) (r | (g << 4) | (b << 8) | (a << 12));
    }

    @Contract(pure = true)
    public static int getR(short swizzle) {
        return swizzle & 0xF;
    }

    @Contract(pure = true)
    public static int getG(short swizzle) {
        return (swizzle >> 4) & 0xF;
    }

    @Contract(pure = true)
    public static int getB(short swizzle) {
        return (swizzle >> 8) & 0xF;
    }

    @Contract(pure = true)
    public static int getA(short swizzle) {
        return swizzle >>> 12;
    }

    /**
     * Concatenates two swizzles (e.g. concat("111R", "AAAA") -> "RRRR").
     */
    public static short concat(short a, short b) {
        int swizzle = 0;
        for (int i = 0; i < 4; ++i) {
            int idx = (b >> (4 * i)) & 0xF;
            if (idx != COMPONENT_ZERO && idx != COMPONENT_ONE) {
                assert idx < 4;
                // Get the index value stored in 'a' at location 'idx'.
                idx = (a >> (4 * idx)) & 0xF;
            }
            swizzle |= (idx << (4 * i));
        }
        return (short) swizzle;
    }

    /**
     * Applies this swizzle to the input color and returns the swizzled color.
     */
    public static void apply(short swizzle,
                             @Size(4) float[] inColor,
                             @Size(4) float[] outColor) {
        final float r = inColor[0], g = inColor[1], b = inColor[2], a = inColor[3];
        for (int i = 0; i < 4; ++i) {
            outColor[i] = switch (swizzle & 0xF) {
                case COMPONENT_R    -> r;
                case COMPONENT_G    -> g;
                case COMPONENT_B    -> b;
                case COMPONENT_A    -> a;
                case COMPONENT_ZERO -> 0.0f;
                case COMPONENT_ONE  -> 1.0f;
                default -> throw new AssertionError();
            };
            swizzle >>= 4;
        }
    }

    public static String toString(short swizzle) {
        return ""
                + indexToChar(getR(swizzle))
                + indexToChar(getG(swizzle))
                + indexToChar(getB(swizzle))
                + indexToChar(getA(swizzle));
    }
}
