/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.spirv;

import java.util.Arrays;

/**
 * Word list that backed by a heap array.
 */
class WordBuffer implements Output {
    // JVM int primitive is in host endianness, see Unsafe
    // transferring to native buffer is just a memory copy
    int[] a;
    int size;

    WordBuffer() {
        a = new int[16];
    }

    @Override
    public void writeWord(int word) {
        int s = size;
        grow(s + 1)[s] = word;
        size = s + 1;
    }

    @Override
    public void writeWords(int[] words, int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeString8(String s) {
        int p = size;
        int len = s.length();
        int[] a = grow(p +
                (len + 4 >> 2)); // +1 null-terminator
        int word = 0;
        int shift = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == 0 || c >= 0x80) {
                throw new AssertionError(c);
            }
            word |= c << shift;
            shift += 8;
            if (shift == 32) {
                a[p++] = word;
                word = 0;
                shift = 0;
            }
        }
        // null-terminator and padding
        a[p++] = word;
        size = p;
    }

    private int[] grow(int minCapacity) {
        if (minCapacity > a.length) {
            // double the buffer, overflow will throw exception
            int newCapacity = Math.max(minCapacity, a.length << 1);
            a = Arrays.copyOf(a, newCapacity);
        }
        return a;
    }
}
