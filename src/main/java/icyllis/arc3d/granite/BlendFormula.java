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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.engine.BlendInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps the shader outputs and HW blend state that comprise a Porter Duff blend mode with coverage.
 */
public final class BlendFormula {

    /**
     * Values the shader can write to primary and secondary outputs. These are all modulated by
     * coverage. We will ignore the multiplies when not using coverage.
     */
    public static final byte
            OUTPUT_TYPE_ZERO = 0,                           // 0
            OUTPUT_TYPE_COVERAGE = 1,                       // inputCoverage
            OUTPUT_TYPE_MODULATE = 2,                       // inputColor * inputCoverage
            OUTPUT_TYPE_SRC_ALPHA_MODULATE = 3,             // inputColor.a * inputCoverage
            OUTPUT_TYPE_ONE_MINUS_SRC_ALPHA_MODULATE = 4,   // (1 - inputColor.a) * inputCoverage
            OUTPUT_TYPE_ONE_MINUS_SRC_COLOR_MODULATE = 5;   // (1 - inputColor) * inputCoverage

    public final byte mPrimaryOutput;
    public final byte mSecondaryOutput;
    public final byte mEquation;
    public final byte mSrcFactor;
    public final byte mDstFactor;

    private static final byte
            PROPERTY_MODIFIES_DST = 1;

    private final byte mProperties;

    public BlendFormula(byte primaryOutput,
                        byte secondaryOutput,
                        byte equation,
                        byte srcFactor,
                        byte dstFactor) {
        mPrimaryOutput = primaryOutput;
        mSecondaryOutput = secondaryOutput;
        mEquation = equation;
        mSrcFactor = srcFactor;
        mDstFactor = dstFactor;
        // The provided formula should already be optimized before a BlendFormula is constructed.
        // Assert that here while setting up the properties in the constexpr constructor.
        assert ((primaryOutput == OUTPUT_TYPE_ZERO) ==
                !BlendInfo.blendCoeffsUseSrcColor(srcFactor, dstFactor));
        assert (!BlendInfo.blendCoeffRefsSrc1(srcFactor));
        assert ((secondaryOutput == OUTPUT_TYPE_ZERO) == !BlendInfo.blendCoeffRefsSrc1(dstFactor));
        assert (primaryOutput != secondaryOutput || primaryOutput == OUTPUT_TYPE_ZERO);
        assert (primaryOutput != OUTPUT_TYPE_ZERO || secondaryOutput == OUTPUT_TYPE_ZERO);
        mProperties = (BlendInfo.blendModifiesDst(equation, srcFactor, dstFactor)
                ? PROPERTY_MODIFIES_DST
                : 0);
    }

    public boolean hasSecondaryOutput() {
        return mSecondaryOutput != OUTPUT_TYPE_ZERO;
    }

    public boolean modifiesDst() {
        return (mProperties & PROPERTY_MODIFIES_DST) != 0;
    }

    /**
     * When there is no coverage, or the blend mode can tweak alpha for coverage, we use the standard
     * Porter Duff formula.
     */
    private static BlendFormula makeCoeffFormula(byte srcFactor, byte dstFactor) {
        // When the factors are (Zero, Zero) or (Zero, One) we set the primary output to zero.
        if ((BlendInfo.FACTOR_ZERO == srcFactor &&
                (BlendInfo.FACTOR_ZERO == dstFactor || BlendInfo.FACTOR_ONE == dstFactor))) {
            return new BlendFormula(OUTPUT_TYPE_ZERO, OUTPUT_TYPE_ZERO,
                    BlendInfo.EQUATION_ADD, srcFactor, dstFactor);
        } else {
            return new BlendFormula(OUTPUT_TYPE_MODULATE, OUTPUT_TYPE_ZERO,
                    BlendInfo.EQUATION_ADD, srcFactor, dstFactor);
        }
    }

    /**
     * When there is coverage, the equation with f=coverage is:
     * <p>
     * D' = f * (S * srcFactor + D * dstFactor) + (1-f) * D
     * <p>
     * This can be rewritten as:
     * <p>
     * D' = f * S * srcFactor + D * (1 - [f * (1 - dstFactor)])
     * <p>
     * To implement this formula, we output [f * (1 - dstFactor)] for the secondary color and replace the
     * HW dst factor with IS2C.
     * <p>
     * Xfer modes: dst-atop (Sa!=1)
     */
    private static BlendFormula makeCoverageFormula(byte oneMinusDstFactorModulateOutput,
                                                    byte srcFactor) {
        return new BlendFormula(OUTPUT_TYPE_MODULATE, oneMinusDstFactorModulateOutput,
                BlendInfo.EQUATION_ADD, srcFactor, BlendInfo.FACTOR_ONE_MINUS_SRC1_COLOR);
    }

    /**
     * When there is coverage and the src factor is zero, the equation with f=coverage becomes:
     * <p>
     * D' = f * D * dstFactor + (1-f) * D
     * <p>
     * This can be rewritten as:
     * <p>
     * D' = D - D * [f * (1 - dstFactor)]
     * <p>
     * To implement this formula, we output [f * (1 - dstFactor)] for the primary color and use a reverse
     * subtract HW blend equation with factors of (DC, One).
     * <p>
     * Xfer modes: clear, dst-out (Sa=1), dst-in (Sa!=1), modulate (Sc!=1)
     */
    private static BlendFormula makeCoverageSrcCoeffZeroFormula(
            byte oneMinusDstFactorModulateOutput) {
        return new BlendFormula(oneMinusDstFactorModulateOutput, OUTPUT_TYPE_ZERO,
                BlendInfo.EQUATION_REVERSE_SUBTRACT, BlendInfo.FACTOR_DST_COLOR, BlendInfo.FACTOR_ONE);
    }

    /**
     * When there is coverage and the dst factor is zero, the equation with f=coverage becomes:
     * <p>
     * D' = f * S * srcFactor + (1-f) * D
     * <p>
     * To implement this formula, we output [f] for the secondary color and replace the HW dst factor
     * with IS2A. (Note that we can avoid dual source blending when Sa=1 by using ISA.)
     * <p>
     * Xfer modes (Sa!=1): src, src-in, src-out
     */
    private static BlendFormula makeCoverageDstCoeffZeroFormula(byte srcFactor) {
        return new BlendFormula(OUTPUT_TYPE_MODULATE, OUTPUT_TYPE_COVERAGE,
                BlendInfo.EQUATION_ADD, srcFactor, BlendInfo.FACTOR_ONE_MINUS_SRC1_ALPHA);
    }

    /**
     * This table outlines the blend formulas we will use with each blend mode, with and without coverage,
     * with and without an opaque input color.
     */
    @Nullable
    public static BlendFormula getBlendFormula(boolean isOpaque, boolean hasCoverage, @Nonnull BlendMode mode) {
        if (!hasCoverage) {
            // No coverage, input color unknown
            switch (mode) {
                case CLEAR:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ZERO);
                case SRC:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ZERO);
                case DST:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ONE);
                case SRC_OVER:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case DST_OVER:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE);
                case SRC_IN:
                    return makeCoeffFormula(BlendInfo.FACTOR_DST_ALPHA, BlendInfo.FACTOR_ZERO);
                case DST_IN:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_SRC_ALPHA);
                case SRC_OUT:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ZERO);
                case DST_OUT:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case SRC_ATOP:
                    return makeCoeffFormula(BlendInfo.FACTOR_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case DST_ATOP:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_SRC_ALPHA);
                case XOR:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case PLUS:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE);
                case MODULATE:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_SRC_COLOR);
                case SCREEN:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_COLOR);
            }
        } else if (!isOpaque) {
            // Has coverage, input color unknown
            switch (mode) {
                case CLEAR:
                    return makeCoverageSrcCoeffZeroFormula(OUTPUT_TYPE_COVERAGE);
                case SRC:
                    return makeCoverageDstCoeffZeroFormula(BlendInfo.FACTOR_ONE);
                case DST:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ONE);
                case SRC_OVER:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case DST_OVER:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE);
                case SRC_IN:
                    return makeCoverageDstCoeffZeroFormula(BlendInfo.FACTOR_DST_ALPHA);
                case DST_IN:
                    return makeCoverageSrcCoeffZeroFormula(OUTPUT_TYPE_ONE_MINUS_SRC_ALPHA_MODULATE);
                case SRC_OUT:
                    return makeCoverageDstCoeffZeroFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA);
                case DST_OUT:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case SRC_ATOP:
                    return makeCoeffFormula(BlendInfo.FACTOR_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case DST_ATOP:
                    return makeCoverageFormula(OUTPUT_TYPE_ONE_MINUS_SRC_ALPHA_MODULATE,
                            BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA);
                case XOR:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case PLUS:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE);
                case MODULATE:
                    return makeCoverageSrcCoeffZeroFormula(OUTPUT_TYPE_ONE_MINUS_SRC_COLOR_MODULATE);
                case SCREEN:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_COLOR);
            }
        } else {
            // Has coverage, input color opaque
            switch (mode) {
                case CLEAR, DST_OUT:
                    return makeCoverageSrcCoeffZeroFormula(OUTPUT_TYPE_COVERAGE);
                case SRC, SRC_OVER:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case DST, DST_IN:
                    return makeCoeffFormula(BlendInfo.FACTOR_ZERO, BlendInfo.FACTOR_ONE);
                case DST_OVER, DST_ATOP:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE);
                case SRC_IN, SRC_ATOP:
                    return makeCoeffFormula(BlendInfo.FACTOR_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case SRC_OUT, XOR:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA, BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA);
                case PLUS:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE);
                case MODULATE:
                    return makeCoverageSrcCoeffZeroFormula(OUTPUT_TYPE_ONE_MINUS_SRC_COLOR_MODULATE);
                case SCREEN:
                    return makeCoeffFormula(BlendInfo.FACTOR_ONE, BlendInfo.FACTOR_ONE_MINUS_SRC_COLOR);
            }
        }
        return null;
    }
}
