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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.GeometryProcessor;

import javax.annotation.Nonnull;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Defines an immutable vertex attribute.
 *
 * @see GeometryProcessor.Attribute
 */
@Deprecated
public class GLVertexAttrib {

    private final int mBinding;
    private final SrcType mSrcType;
    private final DstType mDstType;
    private final boolean mNormalized;

    /**
     * Creates a new VertexAttrib to define an immutable vertex attribute.
     *
     * @param binding    vertex buffer binding index
     * @param srcType    source data type
     * @param dstType    destination data type
     * @param normalized If normalized, then integer data is normalized to the
     *                   range [-1, 1] or [0, 1] if it is signed or unsigned, respectively.
     *                   If not, then integer data is directly converted to floating point.
     */
    public GLVertexAttrib(int binding, @Nonnull SrcType srcType, @Nonnull DstType dstType, boolean normalized) {
        mBinding = binding;
        mSrcType = srcType;
        mDstType = dstType;
        mNormalized = normalized;
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
     * The location count. For example, mat4 is split into four vec4.
     *
     * @return location count
     */
    public int getLocationSize() {
        return mDstType.mLocationSize;
    }

    /**
     * Enables this attribute in the array and specify attribute format.
     *
     * @param array  vertex array object
     * @param offset current offset in the binding point
     * @return next relative offset
     */
    public int setFormat(int array, int location, int offset) {
        for (int i = 0; i < getLocationSize(); i++) {
            glEnableVertexArrayAttrib(array, location);
            glVertexArrayAttribFormat(array, location, mDstType.mSize, mSrcType.mType, mNormalized, offset);
            glVertexArrayAttribBinding(array, location, mBinding);
            location++;
            offset += getStep();
        }
        return offset;
    }

    /**
     * @return the size of the source data in bytes
     */
    public int getStep() {
        return mSrcType.mSize * mDstType.mSize;
    }

    /**
     * @return the total size for this attribute in bytes
     */
    public int getTotalSize() {
        return getStep() * getLocationSize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GLVertexAttrib that = (GLVertexAttrib) o;

        if (mBinding != that.mBinding) return false;
        if (mNormalized != that.mNormalized) return false;
        if (mSrcType != that.mSrcType) return false;
        return mDstType == that.mDstType;
    }

    @Override
    public int hashCode() {
        int result = mBinding;
        result = 31 * result + mSrcType.hashCode();
        result = 31 * result + mDstType.hashCode();
        result = 31 * result + (mNormalized ? 1 : 0);
        return result;
    }

    /**
     * Describes the data type in Vertex Buffer
     */
    public enum SrcType {
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

        SrcType(int size, int type) {
            mSize = size;
            mType = type;
        }
    }

    /**
     * Describes the data type in Vertex Shader
     */
    public enum DstType {
        FLOAT(1, 1),
        VEC2(2, 1),
        VEC3(3, 1),
        VEC4(4, 1),
        MAT4(4, 4);

        /**
         * The number of components
         */
        private final int mSize;
        private final int mLocationSize;

        DstType(int size, int locationSize) {
            mSize = size;
            mLocationSize = locationSize;
        }
    }
}
