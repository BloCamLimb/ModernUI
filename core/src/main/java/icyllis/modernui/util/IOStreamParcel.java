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

import org.jetbrains.annotations.ApiStatus;

import java.io.*;

//TODO TEST ONLY, WILL BE REMOVED
@ApiStatus.Internal
public class IOStreamParcel extends Parcel implements AutoCloseable {

    private final InputStream mIn;
    private final OutputStream mOut;

    public IOStreamParcel(InputStream in, OutputStream out) {
        mIn = in;
        mOut = out;
    }

    @Override
    protected void ensureCapacity(int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void position(int newPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void limit(int newLimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] src, int off, int len) {
        try {
            mOut.write(src, off, len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeByte(int v) {
        try {
            mOut.write(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeShort(int v) {
        mTmpBuffer[0] = (byte) (v >>> 8);
        mTmpBuffer[1] = (byte) (v);
        try {
            mOut.write(mTmpBuffer, 0, 2);
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
            mOut.write(mTmpBuffer, 0, 4);
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
            mOut.write(mTmpBuffer, 0, 8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) {
        try {
            int n = 0;
            while (n < len) {
                int count = mIn.read(dst, off + n, len - n);
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
            int ch = mIn.read();
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
            int ch1 = mIn.read();
            int ch2 = mIn.read();
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
            int ch1 = mIn.read();
            int ch2 = mIn.read();
            int ch3 = mIn.read();
            int ch4 = mIn.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0)
                throw new RuntimeException("Not enough data");
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
    public void close() throws IOException {
        // not correct
        mIn.close();
        mOut.close();
    }
}
