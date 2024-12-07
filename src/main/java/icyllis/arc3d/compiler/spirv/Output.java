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
import icyllis.arc3d.compiler.Position;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Write to a SPIR-V token stream.
 */
interface Output {
    // write a 4-byte word
    void writeWord(int word);

    // write a sequence of 4-byte words
    void writeWords(int[] words, int n);

    // write a string as UTF-8 encoded, null-terminated and 4-byte aligned in LITTLE-ENDIAN order
    // however, our compiler only outputs ASCII characters, so this can be simplified
    void writeString8(Context context, @NonNull String s);
}

/**
 * Writes code to a heap array.
 */
class WordBuffer implements Output {
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
    public void writeString8(Context context, @NonNull String s) {
        int p = size;
        final int len = s.length();
        final int[] a = grow(p +
                (len + 4 >> 2)); // +1 null-terminator
        int word = 0;
        int shift = 0;
        int check = 0;
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            word |= c << shift;
            shift += 8;
            if (shift == 32) {
                a[p++] = word;
                word = 0;
                shift = 0;
            }
            check |= c;
        }
        // null-terminator and padding
        a[p++] = word;
        size = p;
        if ((check & ~0x7F) != 0) {
            context.error(Position.NO_POS, "invalid string '" + s + "'");
        }
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

/**
 * Writes code to a native buffer.
 */
class NativeOutput implements Output {
    private final ByteBuffer mBuffer;

    NativeOutput(int size) {
        mBuffer = BufferUtils.createByteBuffer(size);
    }

    public ByteBuffer detach() {
        return mBuffer.flip();
    }

    @Override
    public void writeWord(int word) {
        mBuffer.putInt(word);
    }

    @Override
    public void writeWords(int[] words, int n) {
        if (n == 0) return;
        // int array is in host endianness (native byte order)
        ByteBuffer buffer = mBuffer;
        buffer.asIntBuffer().put(words, 0, n); // copyMemory
        buffer.position(buffer.position() + (n << 2));
    }

    @Override
    public void writeString8(Context context, @NonNull String s) {
        int len = s.length();
        ByteBuffer buffer = mBuffer;
        int word = 0;
        int shift = 0;
        int check = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            word |= c << shift;
            shift += 8;
            if (shift == 32) {
                buffer.putInt(word);
                word = 0;
                shift = 0;
            }
            check |= c;
        }
        // null-terminator and padding
        buffer.putInt(word);
        if ((check & ~0x7F) != 0) {
            context.error(Position.NO_POS, "invalid string '" + s + "'");
        }
    }
}
