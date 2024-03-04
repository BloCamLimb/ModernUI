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

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.Position;
import it.unimi.dsi.fastutil.ints.IntArrays;

/**
 * Word buffer that backed by a heap array.
 */
class WordBuffer implements Writer {
    // JVM int primitive is in host endianness, see Unsafe
    // transferring to native buffer is just a memory copy
    private int[] a;
    private int size;

    WordBuffer() {
        a = new int[16];
    }

    public int size() {
        return size;
    }

    public int[] elements() {
        return a;
    }

    public void clear() {
        size = 0;
    }

    @Override
    public void writeWord(int word) {
        int s = size;
        grow(s + 1)[s] = word;
        size = s + 1;
    }

    @Override
    public void writeWords(int[] words, int n) {
        if (n == 0) return;
        int newSize = size + n;
        System.arraycopy(words, 0,
                grow(newSize), size, n);
        size = newSize;
    }

    @Override
    public void writeString8(Context context, String s) {
        int p = size;
        int len = s.length();
        int[] a = grow(p +
                (len + 4 >> 2)); // +1 null-terminator
        int word = 0;
        int shift = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == 0 || c >= 0x80) {
                context.error(Position.NO_POS, "unexpected character '" + c + "'");
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
            // double the buffer if small, otherwise grow 50%, overflow will throw exception
            int oldCapacity = a.length;
            int newCapacity = Math.max(minCapacity, oldCapacity < 1024
                    ? oldCapacity << 1
                    : oldCapacity + (oldCapacity >> 1));
            a = IntArrays.forceCapacity(a, newCapacity, size);
        }
        return a;
    }
}
