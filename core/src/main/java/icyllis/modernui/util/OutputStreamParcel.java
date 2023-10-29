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
import java.io.OutputStream;

//TODO TEST ONLY, WILL BE REMOVED
public class OutputStreamParcel extends Parcel implements AutoCloseable {

    private final OutputStream mStream;

    public OutputStreamParcel(OutputStream stream) {
        mStream = stream;
    }

    @Override
    public void writeBytes(byte[] src, int off, int len) {
        try {
            mStream.write(src, off, len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeByte(int v) {
        try {
            mStream.write(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeShort(int v) {
        mTmpBuffer[0] = (byte) (v >>> 8);
        mTmpBuffer[1] = (byte) (v);
        try {
            mStream.write(mTmpBuffer, 0, 2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeInt(int v) {
        mTmpBuffer[0] = (byte) (v >>> 24);
        mTmpBuffer[1] = (byte) (v >>> 16);
        mTmpBuffer[2] = (byte) (v >>> 8);
        mTmpBuffer[3] = (byte) (v);
        try {
            mStream.write(mTmpBuffer, 0, 4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final byte[] mTmpBuffer = new byte[8];

    @Override
    public void writeLong(long v) {
        mTmpBuffer[0] = (byte) (v >>> 56);
        mTmpBuffer[1] = (byte) (v >>> 48);
        mTmpBuffer[2] = (byte) (v >>> 40);
        mTmpBuffer[3] = (byte) (v >>> 32);
        mTmpBuffer[4] = (byte) (v >>> 24);
        mTmpBuffer[5] = (byte) (v >>> 16);
        mTmpBuffer[6] = (byte) (v >>> 8);
        mTmpBuffer[7] = (byte) (v);
        try {
            mStream.write(mTmpBuffer, 0, 8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeChar(int v) {
        mTmpBuffer[0] = (byte) (v >>> 8);
        mTmpBuffer[1] = (byte) (v);
        try {
            mStream.write(mTmpBuffer, 0, 2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) {
        throw new RuntimeException("Write only");
    }

    @Override
    public byte readByte() {
        throw new RuntimeException("Write only");
    }

    @Override
    public short readShort() {
        throw new RuntimeException("Write only");
    }

    @Override
    public int readInt() {
        throw new RuntimeException("Write only");
    }

    @Override
    public long readLong() {
        throw new RuntimeException("Write only");
    }

    @Override
    public char readChar() {
        throw new RuntimeException("Write only");
    }

    @Override
    public float readFloat() {
        throw new RuntimeException("Write only");
    }

    @Override
    public double readDouble() {
        throw new RuntimeException("Write only");
    }

    @Override
    public void close() throws IOException {
        mStream.close();
    }
}
