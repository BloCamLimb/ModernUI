/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.util;

import java.io.IOException;
import java.io.InputStream;

//TODO TEST ONLY, WILL BE REMOVED
public class InputStreamParcel extends Parcel implements AutoCloseable {

    private final InputStream mStream;

    public InputStreamParcel(InputStream stream) {
        mStream = stream;
    }

    @Override
    public void writeBytes(byte[] src, int off, int len) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeByte(int v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeShort(int v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeInt(int v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeLong(long v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeChar(int v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeFloat(float v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void writeDouble(double v) {
        throw new RuntimeException("Read only");
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) {
        try {
            int n = 0;
            while (n < len) {
                int count = mStream.read(dst, off + n, len - n);
                if (count < 0)
                    throw new RuntimeException("Not enough data");
                n += count;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte readByte() {
        try {
            int ch = mStream.read();
            if (ch < 0)
                throw new RuntimeException("Not enough data");
            return (byte) (ch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short readShort() {
        try {
            int ch1 = mStream.read();
            int ch2 = mStream.read();
            if ((ch1 | ch2) < 0)
                throw new RuntimeException("Not enough data");
            return (short) ((ch1 << 8) + (ch2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readInt() {
        try {
            int ch1 = mStream.read();
            int ch2 = mStream.read();
            int ch3 = mStream.read();
            int ch4 = mStream.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new RuntimeException("Not enough data");
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final byte[] mTmpBuffer = new byte[8];

    @Override
    public long readLong() {
        readBytes(mTmpBuffer, 0, 8);
        return (((long) mTmpBuffer[0] << 56) +
                ((long) (mTmpBuffer[1] & 255) << 48) +
                ((long) (mTmpBuffer[2] & 255) << 40) +
                ((long) (mTmpBuffer[3] & 255) << 32) +
                ((long) (mTmpBuffer[4] & 255) << 24) +
                ((mTmpBuffer[5] & 255) << 16) +
                ((mTmpBuffer[6] & 255) << 8) +
                ((mTmpBuffer[7] & 255)));
    }

    @Override
    public char readChar() {
        try {
            int ch1 = mStream.read();
            int ch2 = mStream.read();
            if ((ch1 | ch2) < 0)
                throw new RuntimeException("Not enough data");
            return (char) ((ch1 << 8) + (ch2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public void close() throws IOException {
        mStream.close();
    }
}
