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

package icyllis.arc3d.engine;

/**
 * Specifies a single region for copying, either from buffer to image, or vice versa
 */
public class BufferImageCopyData {

    public long mBufferOffset;
    public long mBufferRowBytes;

    public int mMipLevel;
    public int mArraySlice;
    public int mNumSlices;

    public int mX, mY, mZ;
    public int mWidth, mHeight, mDepth;

    public BufferImageCopyData() {
    }

    public BufferImageCopyData(long bufferOffset,
                               long bufferRowBytes,
                               int mipLevel,
                               int arraySlice,
                               int numSlices,
                               int x, int y, int z,
                               int width, int height, int depth) {
        mBufferOffset = bufferOffset;
        mBufferRowBytes = bufferRowBytes;
        mMipLevel = mipLevel;
        mArraySlice = arraySlice;
        mNumSlices = numSlices;
        mX = x;
        mY = y;
        mZ = z;
        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }
}
