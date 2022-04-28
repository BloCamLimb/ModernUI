/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.hgi;

/**
 * Represents a rgba swizzle. It can be converted into a short.
 */
public final class Swizzle {

    public static final short RGBA = pack('r', 'g', 'b', 'a');
    public static final short BGRA = pack('b', 'g', 'r', 'a');
    public static final short RRRA = pack('r', 'r', 'r', 'a');
    public static final short RGB1 = pack('r', 'g', 'b', '1');

    public static int CToI(char c) {
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

    public static char IToC(int idx) {
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
     * Compact representation of the swizzle suitable for a key.
     */
    public static short pack(String rgba) {
        if (rgba.length() != 4) {
            throw new IllegalArgumentException();
        }
        return (short) (CToI(rgba.charAt(0)) | (CToI(rgba.charAt(1)) << 4) |
                (CToI(rgba.charAt(2)) << 8) | (CToI(rgba.charAt(3)) << 12));
    }

    /**
     * Compact representation of the swizzle suitable for a key.
     */
    public static short pack(char r, char g, char b, char a) {
        return (short) (CToI(r) | (CToI(g) << 4) | (CToI(b) << 8) | (CToI(a) << 12));
    }
}
