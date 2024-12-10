/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.glsl;

import it.unimi.dsi.fastutil.bytes.ByteArrays;
import org.jspecify.annotations.NonNull;

/**
 * Write to a UTF-8 string stream.
 */
interface Output {
    void write(char c);

    // write UTF-8 encoded string
    void writeString(byte[] str, int n);

    // write a string as UTF-8 encoded
    // however, our compiler only outputs ASCII characters, so this can be simplified
    boolean writeString8(@NonNull String s);
}

/**
 * Writes UTF-8 code to a heap array.
 */
class CodeBuffer implements Output {
    private byte[] a;
    private int size;

    CodeBuffer() {
        a = new byte[64];
    }

    public int size() {
        return size;
    }

    public byte[] elements() {
        return a;
    }

    public void clear() {
        size = 0;
    }

    @Override
    public void write(char c) {
        assert c <= 0x7F;
        int s = size;
        grow(s + 1)[s] = (byte) c;
        size = s + 1;
    }

    @Override
    public void writeString(byte[] str, int n) {
        if (n == 0) return;
        int newSize = size + n;
        System.arraycopy(str, 0,
                grow(newSize), size, n);
        size = newSize;
    }

    @Override
    public boolean writeString8(@NonNull String s) {
        int p = size;
        final int len = s.length();
        final byte[] a = grow(p + len);
        int check = 0;
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            a[p++] = (byte) c;
            check |= c;
        }
        size = p;
        return (check & ~0x7F) == 0;
    }

    private byte[] grow(int minCapacity) {
        if (minCapacity > a.length) {
            // double the buffer if small, otherwise grow 50%, overflow will throw exception
            int oldCapacity = a.length;
            int newCapacity = Math.max(minCapacity, oldCapacity < 1024
                    ? oldCapacity << 1
                    : oldCapacity + (oldCapacity >> 1));
            a = ByteArrays.forceCapacity(a, newCapacity, size);
        }
        return a;
    }
}