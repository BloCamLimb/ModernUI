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

package icyllis.arc3d.compiler.spirv;

import icyllis.arc3d.compiler.Context;

/**
 * Write to a SPIR-V token stream.
 */
interface Writer {
    // write a 4-byte word
    void writeWord(int word);

    // write a sequence of 4-byte words
    void writeWords(int[] words, int n);

    // write a string as UTF-8 encoded, null-terminated and 4-byte aligned in LITTLE-ENDIAN order
    // however, our compiler only outputs ASCII characters, so this can be simplified
    void writeString8(Context context, String s);
}
