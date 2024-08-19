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

import icyllis.arc3d.core.BlendMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    public boolean blendShouldDisable() {
        return (mEquation == EQUATION_ADD || mEquation == EQUATION_SUBTRACT) &&
                mSrcFactor == FACTOR_ONE && mDstFactor == FACTOR_ZERO;
    }

    public static boolean blendCoeffRefsSrc(byte factor) {
        return factor == FACTOR_SRC_COLOR || factor == FACTOR_ONE_MINUS_SRC_COLOR ||
                factor == FACTOR_SRC_ALPHA || factor == FACTOR_ONE_MINUS_SRC_ALPHA;
    }

    public static boolean blendCoeffRefsDst(byte factor) {
        return factor == FACTOR_DST_COLOR || factor == FACTOR_ONE_MINUS_DST_COLOR ||
                factor == FACTOR_DST_ALPHA || factor == FACTOR_ONE_MINUS_DST_ALPHA;
    }

    public static boolean blendCoeffRefsSrc1(byte factor) {
        return factor == FACTOR_SRC1_COLOR || factor == FACTOR_ONE_MINUS_SRC1_COLOR ||
                factor == FACTOR_SRC1_ALPHA || factor == FACTOR_ONE_MINUS_SRC1_ALPHA;
    }

    public static boolean blendCoeffsUseSrcColor(byte srcFactor, byte dstFactor) {
        return srcFactor != FACTOR_ZERO || blendCoeffRefsSrc(dstFactor);
    }

    public static boolean blendCoeffsUseDstColor(byte srcFactor,
                                                 byte dstFactor,
                                                 boolean srcColorIsOpaque) {
        return blendCoeffRefsDst(srcFactor) ||
                (dstFactor != FACTOR_ZERO && !(dstFactor == FACTOR_ONE_MINUS_SRC_ALPHA && srcColorIsOpaque));
    }

    public static boolean blendModifiesDst(byte equation, byte srcFactor, byte dstFactor) {
        return (equation != EQUATION_ADD && equation != EQUATION_REVERSE_SUBTRACT) ||
                srcFactor != FACTOR_ZERO || dstFactor != FACTOR_ONE;
    }

    public static final BlendInfo BLEND_CLEAR = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ZERO,
            FACTOR_ZERO,
            true
    );
    public static final BlendInfo BLEND_SRC = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ONE,
            FACTOR_ZERO,
            true
    );
    public static final BlendInfo BLEND_DST = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ZERO,
            FACTOR_ONE,
            false
    );
    public static final BlendInfo BLEND_SRC_OVER = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ONE,
            FACTOR_ONE_MINUS_SRC_ALPHA,
            true
    );
    public static final BlendInfo BLEND_DST_OVER = new BlendInfo(
            EQUATION_ADD,
            FACTOR_ONE_MINUS_DST_ALPHA,
            FACTOR_ONE,
            true
    );
    public static final BlendInfo BLEND_SRC_IN = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_DST_ALPHA, BlendInfo.FACTOR_ZERO,
            true
    );
    public static final BlendInfo BLEND_DST_IN = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_SRC_ALPHA,
            true
    );
    public static final BlendInfo BLEND_SRC_OUT = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ZERO,
            true
    );
    public static final BlendInfo BLEND_DST_OUT = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA,
            true
    );
    public static final BlendInfo BLEND_SRC_ATOP = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA,
            true
    );
    public static final BlendInfo BLEND_DST_ATOP = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_SRC_ALPHA,
            true
    );
    public static final BlendInfo BLEND_XOR = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA,
            true
    );
    public static final BlendInfo BLEND_PLUS = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE,
            true
    );
    public static final BlendInfo BLEND_MINUS = new BlendInfo(
            EQUATION_REVERSE_SUBTRACT,
            BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE,
            true
    );
    public static final BlendInfo BLEND_MODULATE = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_SRC_COLOR,
            true
    );
    public static final BlendInfo BLEND_SCREEN = new BlendInfo(
            EQUATION_ADD,
            BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_COLOR,
            true
    );

    /**
     * Returns the standard HW blend info for the given Porter Duff blend mode.
     */
    @Nullable
    public static BlendInfo getSimpleBlendInfo(@Nonnull BlendMode mode) {
        return switch (mode) {
            case CLEAR -> BLEND_CLEAR;
            case SRC -> BLEND_SRC;
            case DST -> BLEND_DST;
            case SRC_OVER -> BLEND_SRC_OVER;
            case DST_OVER -> BLEND_DST_OVER;
            case SRC_IN -> BLEND_SRC_IN;
            case DST_IN -> BLEND_DST_IN;
            case SRC_OUT -> BLEND_SRC_OUT;
            case DST_OUT -> BLEND_DST_OUT;
            case SRC_ATOP -> BLEND_SRC_ATOP;
            case DST_ATOP -> BLEND_DST_ATOP;
            case XOR -> BLEND_XOR;
            case PLUS, PLUS_CLAMPED -> BLEND_PLUS;
            case MINUS, MINUS_CLAMPED -> BLEND_MINUS;
            case MODULATE -> BLEND_MODULATE;
            case SCREEN -> BLEND_SCREEN;
            default -> null;
        };
    }
}
