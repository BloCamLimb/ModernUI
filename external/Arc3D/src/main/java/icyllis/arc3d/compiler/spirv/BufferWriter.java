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
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Writes code to a native buffer.
 */
class BufferWriter implements Writer {
    private final ByteBuffer mBuffer;

    public BufferWriter(int size) {
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
    public void writeString8(Context context, String s) {
        int len = s.length();
        ByteBuffer buffer = mBuffer;
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
                buffer.putInt(word);
                word = 0;
                shift = 0;
            }
        }
        // null-terminator and padding
        buffer.putInt(word);
    }
}
