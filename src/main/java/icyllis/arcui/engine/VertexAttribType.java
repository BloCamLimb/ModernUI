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

package icyllis.arcui.engine;

/**
 * Types used to describe format of vertices in arrays.
 */
public final class VertexAttribType {

    public static final byte
            FLOAT = 0,
            FLOAT2 = 1,
            FLOAT3 = 2,
            FLOAT4 = 3,
            HALF = 4,
            HALF2 = 5,
            HALF4 = 6;
    public static final byte
            INT2 = 7,   // vector of 2 32-bit ints
            INT3 = 8,   // vector of 3 32-bit ints
            INT4 = 9;   // vector of 4 32-bit ints
    public static final byte
            BYTE = 10,   // signed byte
            BYTE2 = 11,  // vector of 2 8-bit signed bytes
            BYTE4 = 12,  // vector of 4 8-bit signed bytes
            UBYTE = 13,  // unsigned byte
            UBYTE2 = 14, // vector of 2 8-bit unsigned bytes
            UBYTE4 = 15; // vector of 4 8-bit unsigned bytes
    public static final byte
            UBYTE_NORM = 16,  // unsigned byte, e.g. coverage, 0 -> 0.0f, 255 -> 1.0f.
            UBYTE4_NORM = 17; // vector of 4 unsigned bytes, e.g. colors, 0 -> 0.0f, 255 -> 1.0f.
    public static final byte
            SHORT2 = 18,       // vector of 2 16-bit shorts.
            SHORT4 = 19;       // vector of 4 16-bit shorts.
    public static final byte
            USHORT2 = 20,      // vector of 2 unsigned shorts. 0 -> 0, 65535 -> 65535.
            USHORT2_NORM = 21; // vector of 2 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
    public static final byte
            INT = 22,
            UINT = 23;
    public static final byte
            USHORT_NORM = 24;
    public static final byte
            USHORT4_NORM = 25; // vector of 4 unsigned shorts. 0 -> 0.0f, 65535 -> 1.0f.
    public static final byte LAST = USHORT4_NORM;

    /**
     * @return size in bytes
     */
    public static int getSize(byte type) {
        switch (type) {
            case FLOAT:
                return Float.BYTES;
            case FLOAT2:
                return 2 * Float.BYTES;
            case FLOAT3:
                return 3 * Float.BYTES;
            case FLOAT4:
                return 4 * Float.BYTES;
            case HALF:
            case USHORT_NORM:
                return Short.BYTES;
            case HALF2:
            case SHORT2:
            case USHORT2:
            case USHORT2_NORM:
                return 2 * Short.BYTES;
            case HALF4:
            case SHORT4:
            case USHORT4_NORM:
                return 4 * Short.BYTES;
            case INT2:
                return 2 * Integer.BYTES;
            case INT3:
                return 3 * Integer.BYTES;
            case INT4:
                return 4 * Integer.BYTES;
            case BYTE:
            case UBYTE:
            case UBYTE_NORM:
                return Byte.BYTES;
            case BYTE2:
            case UBYTE2:
                return 2 * Byte.BYTES;
            case BYTE4:
            case UBYTE4:
            case UBYTE4_NORM:
                return 4 * Byte.BYTES;
            case INT:
            case UINT:
                return Integer.BYTES;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    private VertexAttribType() {
    }
}
