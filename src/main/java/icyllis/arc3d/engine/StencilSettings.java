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

package icyllis.arc3d.engine;

/**
 * This class defines concrete stencil settings that map directly to the underlying hardware. It
 * is deduced from user stencil settings, stencil clip status, and the number of bits in the
 * target stencil buffer.
 */
@Deprecated
public final class StencilSettings {

    // Internal flag for backends to optionally mark their tracked stencil state as invalid.
    // NOTE: This value is outside the declared range of GrStencilFlags, but since that type is
    // explicitly backed by 'int', it can still represent this constant. clang 11 complains about
    // mixing enum types in bit operations, so this works around that.
    private static final short INVALID_PRIVATE_FLAG = UserStencilSettings.LAST_STENCIL_FLAG << 1;

    /**
     * StencilTest
     */
    public static final short
            STENCIL_TEST_ALWAYS = 0,
            STENCIL_TEST_NEVER = 1,
            STENCIL_TEST_GREATER = 2,
            STENCIL_TEST_GEQUAL = 3,
            STENCIL_TEST_LESS = 4,
            STENCIL_TEST_LEQUAL = 5,
            STENCIL_TEST_EQUAL = 6,
            STENCIL_TEST_NOTEQUAL = 7;
    public static final int STENCIL_TEST_COUNT = 1 + STENCIL_TEST_NOTEQUAL;

    /**
     * StencilOp
     */
    public static final byte
            STENCIL_OP_KEEP = 0,
            STENCIL_OP_ZERO = 1,
            STENCIL_OP_REPLACE = 2, // Replace stencil value with fRef (only the bits enabled in fWriteMask).
            STENCIL_OP_INVERT = 3,
            STENCIL_OP_INC_WRAP = 4,
            STENCIL_OP_DEC_WRAP = 5;
    // NOTE: clamping occurs before the write mask. So if the MSB is zero and masked out, stencil
    // values will still wrap when using clamping ops.
    public static final byte
            STENCIL_OP_INC_CLAMP = 6,
            STENCIL_OP_DEC_CLAMP = 7;
    public static final int STENCIL_OP_COUNT = 1 + STENCIL_OP_DEC_CLAMP;

    private int mFlags;
    private StencilFaceSettings mCWFace;
    private StencilFaceSettings mCCWFace;
}
