/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.shaderc;

/**
 * Represents a position that encapsulates the offset and length.
 * It's packed as a <code>int</code> value to reduce object allocation.
 * This class provides static methods to pack and unpack positions.
 */
public final class Position {

    public static final int NO_POS = -1;
    public static final int MAX_OFFSET = 0x7FFFFF;

    /**
     * Pack a range into a position, the position is valid only if
     * {@code start >= 0 && start <= end && end <= 0x7FFFFF}.
     * <ul>
     * <li>0-24 bits: offset, signed, -1 means invalid</li>
     * <li>24-32 bits: length, unsigned, saturate at 0xFF</li>
     * </ul>
     *
     * @return the position
     */
    public static int range(int start, int end) {
        if (start < 0 || start > end || end > MAX_OFFSET) {
            return NO_POS;
        }
        return start | Math.min(end - start, 0xFF) << 24;
    }

    public static int getStartOffset(int pos) {
        if (pos == NO_POS) {
            return -1;
        }
        return (pos & 0xFFFFFF);
    }

    public static int getEndOffset(int pos) {
        if (pos == NO_POS) {
            return -1;
        }
        return (pos & 0xFFFFFF) + (pos >>> 24);
    }

    public static int getLine(int pos, String source) {
        if (pos == NO_POS || source == null) {
            return -1;
        }
        int offset = Math.min(pos & 0xFFFFFF, source.length());
        int line = 1;
        for (int i = 0; i < offset; ++i) {
            if (source.charAt(i) == '\n') {
                ++line;
            }
        }
        return line;
    }

    private Position() {
    }
}
