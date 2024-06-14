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
 * BlendInfo is an immutable object holding info for setting-up GPU blend states.
 */
public final class BlendInfo {
    // this class can be packed into an int, waiting for Value Types

    /**
     * Blend factors. Same in OpenGL and Vulkan.
     */
    public static final byte
            FACTOR_ZERO = 0,
            FACTOR_ONE = 1,
            FACTOR_SRC_COLOR = 2,
            FACTOR_ONE_MINUS_SRC_COLOR = 3,
            FACTOR_DST_COLOR = 4,
            FACTOR_ONE_MINUS_DST_COLOR = 5,
            FACTOR_SRC_ALPHA = 6,
            FACTOR_ONE_MINUS_SRC_ALPHA = 7,
            FACTOR_DST_ALPHA = 8,
            FACTOR_ONE_MINUS_DST_ALPHA = 9,
            FACTOR_CONSTANT_COLOR = 10,
            FACTOR_ONE_MINUS_CONSTANT_COLOR = 11,
            FACTOR_CONSTANT_ALPHA = 12,
            FACTOR_ONE_MINUS_CONSTANT_ALPHA = 13,
            FACTOR_SRC_ALPHA_SATURATE = 14,
            FACTOR_SRC1_COLOR = 15,
            FACTOR_ONE_MINUS_SRC1_COLOR = 16,
            FACTOR_SRC1_ALPHA = 17,
            FACTOR_ONE_MINUS_SRC1_ALPHA = 18;
    public static final byte
            FACTOR_UNKNOWN = -1;

    /**
     * Basic blend equations.
     */
    public static final byte
            EQUATION_ADD = 0,
            EQUATION_SUBTRACT = 1,
            EQUATION_REVERSE_SUBTRACT = 2;
    /**
     * Advanced blend equations.
     */
    public static final byte
            EQUATION_MULTIPLY = 3,
            EQUATION_SCREEN = 4,
            EQUATION_OVERLAY = 5,
            EQUATION_DARKEN = 6,
            EQUATION_LIGHTEN = 7,
            EQUATION_COLORDODGE = 8,
            EQUATION_COLORBURN = 9,
            EQUATION_HARDLIGHT = 10,
            EQUATION_SOFTLIGHT = 11,
            EQUATION_DIFFERENCE = 12,
            EQUATION_EXCLUSION = 13,
            EQUATION_HSL_HUE = 14,
            EQUATION_HSL_SATURATION = 15,
            EQUATION_HSL_COLOR = 16,
            EQUATION_HSL_LUMINOSITY = 17;
    /**
     * Advanced blend equations (extended).
     */
    //TODO
    public static final byte
            EQUATION_UNKNOWN = -1;

    public static final BlendInfo SRC = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ONE,
            FACTOR_ZERO,
            true
    );

    public static final BlendInfo SRC_OVER = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ONE,
            FACTOR_ONE_MINUS_SRC_ALPHA,
            true
    );

    public final byte mEquation;
    public final byte mSrcFactor;
    public final byte mDstFactor;
    public final boolean mColorWrite;

    public BlendInfo(byte equation,
                     byte srcFactor,
                     byte dstFactor,
                     boolean colorWrite) {
        mEquation = equation;
        mSrcFactor = srcFactor;
        mDstFactor = dstFactor;
        mColorWrite = colorWrite;
    }

    public boolean shouldDisableBlend() {
        return (mEquation == EQUATION_ADD || mEquation == EQUATION_SUBTRACT) &&
                mSrcFactor == FACTOR_ONE && mDstFactor == FACTOR_ZERO;
    }
}
