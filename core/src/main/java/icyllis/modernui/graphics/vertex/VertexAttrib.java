/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.vertex;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Defines an immutable vertex attribute.
 */
public class VertexAttrib {

    private final int mLocation;
    private final int mBinding;
    private final Src mSrc;
    private final Dst mDst;
    private final boolean mNormalized;

    /**
     * Creates a new VertexAttrib to define an immutable vertex attribute.
     *
     * @param location   explicit layout location
     * @param binding    vertex buffer binding index
     * @param src        source data type
     * @param dst        destination data type
     * @param normalized If normalized, then integer data is normalized to the
     *                   range [-1, 1] or [0, 1] if it is signed or unsigned, respectively.
     *                   If not, then integer data is directly converted to floating point.
     */
    public VertexAttrib(int location, int binding, Src src, Dst dst, boolean normalized) {
        mLocation = location;
        mBinding = binding;
        mSrc = src;
        mDst = dst;
        mNormalized = normalized;
    }

    /**
     * Returns the base attribute index, (layout = index).
     * <p>
     * Actually, this attribute takes up locations from <code>getLocation()</code> (inclusive)
     * to <code>getLocation()+getRepeat()</code> (exclusive)
     *
     * @return attribute index
     * @see #getRepeat()
     */
    public int getLocation() {
        return mLocation;
    }

    /**
     * Returns the binding point index.
     *
     * @return binding index
     */
    public int getBinding() {
        return mBinding;
    }

    /**
     * The repeat count. For example, mat4 is split into 4 vec4.
     *
     * @return repeat count
     */
    public int getRepeat() {
        return mDst.mRepeat;
    }

    /**
     * Enables this attribute in the array and specify attribute format.
     *
     * @param array   vertex array object
     * @param pointer current pointer in the binding point
     * @return next pointer (relative offset)
     */
    public int setFormat(int array, int pointer) {
        for (int i = 0; i < getRepeat(); i++) {
            int location = mLocation + i;
            glEnableVertexArrayAttrib(array, location);
            glVertexArrayAttribFormat(array, location, mDst.mSize, mSrc.mType, mNormalized, pointer);
            pointer += mSrc.mSize * mDst.mSize;
        }
        return pointer;
    }

    /**
     * Describes the data type in Vertex Buffer
     */
    public enum Src {
        FLOAT(4, GL_FLOAT),
        BYTE(1, GL_BYTE),
        UBYTE(1, GL_UNSIGNED_BYTE),
        SHORT(2, GL_SHORT),
        USHORT(2, GL_UNSIGNED_SHORT),
        INT(4, GL_INT),
        UINT(4, GL_UNSIGNED_INT),
        HALF(2, GL_HALF_FLOAT);

        // in bytes
        private final int mSize;
        private final int mType;

        Src(int size, int type) {
            mSize = size;
            mType = type;
        }
    }

    /**
     * Describes the data type in Vertex Shader
     */
    public enum Dst {
        FLOAT(1, 1),
        VEC2(2, 1),
        VEC3(3, 1),
        VEC4(4, 1),
        MAT4(4, 4);

        /**
         * The number of components
         */
        private final int mSize;
        private final int mRepeat;

        Dst(int size, int repeat) {
            mSize = size;
            mRepeat = repeat;
        }
    }
}
