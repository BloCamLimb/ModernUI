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

import java.util.Objects;

/**
 * This class defines concrete depth and stencil settings that map directly to the
 * underlying 3D API.
 */
public final class DepthStencilSettings {

    /**
     * CompareOp for depth test and stencil test.
     */
    // same declaration order as OpenGL and Vulkan
    public static final byte
            COMPARE_OP_NEVER = 0,
            COMPARE_OP_LESS = 1,
            COMPARE_OP_EQUAL = 2,
            COMPARE_OP_LEQUAL = 3,
            COMPARE_OP_GREATER = 4,
            COMPARE_OP_NOTEQUAL = 5,
            COMPARE_OP_GEQUAL = 6,
            COMPARE_OP_ALWAYS = 7;

    /**
     * StencilOp.
     */
    // same declaration order as Vulkan
    public static final byte
            STENCIL_OP_KEEP = 0,
            STENCIL_OP_ZERO = 1,
            STENCIL_OP_REPLACE = 2,     // Replace stencil value with mReference (only the bits enabled in mWriteMask).
            STENCIL_OP_INC_CLAMP = 3,   // NOTE: clamping occurs before the write mask. So if the MSB is zero and
            STENCIL_OP_DEC_CLAMP = 4,   // masked out, stencil values will still wrap when using clamping ops.
            STENCIL_OP_INVERT = 5,
            STENCIL_OP_INC_WRAP = 6,
            STENCIL_OP_DEC_WRAP = 7;

    /**
     * Per-face stencil settings.
     */
    public static final class Face {

        public final byte mFailOp;        // Op to perform when the stencil test fails.
        public final byte mPassOp;        // Op to perform when the stencil and depth test passes.
        public final byte mDepthFailOp;   // Op to perform when the stencil test passes but depth test fails.
        public final byte mCompareOp;     // Stencil test function, where mReference is on the left side.
        public final short mReference;    // Reference value for stencil test and ops.
        public final short mCompareMask;  // Bitwise "and" to perform on mReference and stencil values before testing.
        // (e.g. (mReference & mCompareMask) < (stencil & mCompareMask))
        public final short mWriteMask;    // Indicates which bits in the stencil buffer should be updated.
        // (e.g. stencil = (newValue & mWriteMask) | (stencil & ~mWriteMask))

        public Face(byte failOp, byte passOp, byte depthFailOp, byte compareOp,
                    short reference, short compareMask, short writeMask) {
            mFailOp = failOp;
            mPassOp = passOp;
            mDepthFailOp = depthFailOp;
            mCompareOp = compareOp;
            mReference = reference;
            mCompareMask = compareMask;
            mWriteMask = writeMask;
        }

        @Override
        public int hashCode() {
            int result = mFailOp;
            result = 31 * result + (int) mPassOp;
            result = 31 * result + (int) mDepthFailOp;
            result = 31 * result + (int) mCompareOp;
            result = 31 * result + (int) mReference;
            result = 31 * result + (int) mCompareMask;
            result = 31 * result + (int) mWriteMask;
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Face face) {
                return mFailOp == face.mFailOp &&
                        mPassOp == face.mPassOp &&
                        mDepthFailOp == face.mDepthFailOp &&
                        mCompareOp == face.mCompareOp &&
                        mReference == face.mReference &&
                        mCompareMask == face.mCompareMask &&
                        mWriteMask == face.mWriteMask;
            }
            return false;
        }
    }

    public final Face mFrontFace;   // CCW
    public final Face mBackFace;    // CW
    public final byte mDepthCompareOp;
    public final boolean mDepthWrite;
    public final boolean mStencilTest;
    public final boolean mDepthTest;

    /**
     * If stencil test is disabled, then two faces must be null.
     * If depth test is disabled, then depth compare op must be Never.
     */
    public DepthStencilSettings(Face frontFace, Face backFace,
                                byte depthCompareOp, boolean depthWrite,
                                boolean stencilTest, boolean depthTest) {
        mFrontFace = frontFace;
        mBackFace = backFace;
        mDepthCompareOp = depthCompareOp;
        mDepthWrite = depthWrite;
        mStencilTest = stencilTest;
        mDepthTest = depthTest;
    }

    public boolean isTwoSided() {
        return !mFrontFace.equals(mBackFace);
    }

    @Override
    public int hashCode() {
        int result = mFrontFace != null ? mFrontFace.hashCode() : 0;
        result = 31 * result + (mBackFace != null ? mBackFace.hashCode() : 0);
        result = 31 * result + (int) mDepthCompareOp;
        result = 31 * result + (mDepthWrite ? 1 : 0);
        result = 31 * result + (mStencilTest ? 1 : 0);
        result = 31 * result + (mDepthTest ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DepthStencilSettings that) {
            return mDepthCompareOp == that.mDepthCompareOp &&
                    mDepthWrite == that.mDepthWrite &&
                    mStencilTest == that.mStencilTest &&
                    mDepthTest == that.mDepthTest &&
                    Objects.equals(mFrontFace, that.mFrontFace) &&
                    Objects.equals(mBackFace, that.mBackFace);
        }
        return false;
    }
}
