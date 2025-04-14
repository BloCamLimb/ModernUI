/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch.shaders;

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.sketch.Matrix;
import icyllis.arc3d.sketch.Matrixc;
import icyllis.arc3d.core.SharedPtr;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.CheckReturnValue;

/**
 * The angular gradient, also known as sweep gradient, conic gradient.
 */
public final class AngularGradient extends Gradient1DShader {

    private final float mCenterX;
    private final float mCenterY;
    private final float mTBias;
    private final float mTScale;

    @VisibleForTesting
    public AngularGradient(float centerX, float centerY,
                           float tStart, float tEnd,
                           float @NonNull [] colors,
                           @Nullable ColorSpace colorSpace,
                           float @Nullable [] positions,
                           int colorCount,
                           int tileMode,
                           int interpolation) {
        super(colors, colorSpace, positions, colorCount, tileMode, interpolation,
                Matrix.makeTranslate(-centerX, -centerY));
        assert tStart < tEnd;
        mCenterX = centerX;
        mCenterY = centerY;
        mTBias = -tStart;
        mTScale = 1.0f / (tEnd - tStart);
    }

    /**
     * Create an angular gradient shader.
     * <p>
     * The shader accepts negative angles and angles larger than 360, draws
     * between 0 and 360 degrees, similar to the CSS conic-gradient
     * semantics. 0 degrees means horizontal positive x-axis. The start angle
     * must be less than the end angle, otherwise a null pointer is
     * returned. If color stops do not contain 0 and 1 but are within this
     * range, the respective outer color stop is repeated for 0 and 1. Color
     * stops less than 0 are clamped to 0, and greater than 1 are clamped to 1.
     * <p>
     * The <var>colors</var> array holds repeated R,G,B,A values of
     * source colors to interpolate, they are un-premultiplied and in
     * the given <var>colorSpace</var>.
     * <p>
     * The <var>positions</var> array specifies a number of stops, all values
     * are between 0 and 1 and monotonic increasing. Colors will be linearly
     * interpolated in each stop. Null means that they are uniformly distributed.
     * If the first position is not 0 or the last position is not 1, an implicit
     * stop will be added.
     * <p>
     * Only the first <var>colorCount</var> entries in array will be taken, then
     * <code>colors.length >= colorCount * 4 && positions.length >= colorCount</code>.
     * <p>
     * The <var>tileMode</var> specifies the behavior when local coords are out of
     * bounds.
     * <p>
     * The <var>interpolation</var> specifies the color interpolation method, see
     * {@link GradientShader.Interpolation}.
     * <p>
     * The <var>localMatrix</var> specifies an additional local matrix for this
     * gradient, null means identity.
     * <p>
     * All given arguments will be simplified and copied into the return shader.
     * Null is returned if there are any illegal arguments (NaN, Inf, non-invertible
     * matrix, etc.).
     *
     * @param centerX       x of center point
     * @param centerY       y of center point
     * @param startAngle    start angle in degrees
     * @param endAngle      end angle in degrees
     * @param colors        color array
     * @param colorSpace    color space, null will use the default one (sRGB)
     * @param positions     position array
     * @param colorCount    number of stops
     * @param tileMode      tile mode
     * @param interpolation interpolation method
     * @param localMatrix   local matrix
     * @return a gradient shader, or degenerate shader, or null
     */
    @CheckReturnValue
    @Nullable
    @SharedPtr
    public static Shader make(float centerX, float centerY,
                              float startAngle, float endAngle,
                              float @NonNull [] colors,
                              @Nullable ColorSpace colorSpace,
                              float @Nullable [] positions,
                              int colorCount,
                              int tileMode,
                              int interpolation,
                              @Nullable Matrixc localMatrix) {
        if (!Float.isFinite(startAngle) || !Float.isFinite(endAngle)) {
            return null;
        }
        if (startAngle > endAngle) {
            return null;
        }
        if (!Float.isFinite(centerX) || !Float.isFinite(centerY)) {
            return null;
        }
        if (!checkGradient1D(colors, positions, colorCount, tileMode)) {
            return null;
        }
        if (colorCount == 1) {
            return ColorShader.make(colors[0], colors[1], colors[2], colors[3], colorSpace);
        }
        if (localMatrix != null && !localMatrix.invert(null)) {
            return null;
        }
        if (Math.abs(endAngle - startAngle) <= kDegenerateTolerance) {
            // Degenerate gradient, which should follow default degenerate behavior unless it is
            // clamped and the angle is greater than 0.
            if (tileMode == TILE_MODE_CLAMP && endAngle > kDegenerateTolerance) {
                // In this case, the first color is repeated from 0 to the angle, then a hardstop
                // switches to the last color (all other colors are compressed to the infinitely thin
                // interpolation region).
                float[] newPos = {0, 1, 1};
                int i = (colorCount - 1) * 4;
                float[] newColor = {colors[0], colors[1], colors[2], colors[3],
                        colors[0], colors[1], colors[2], colors[3],
                        colors[i], colors[i + 1], colors[i + 2], colors[i + 3]};
                return make(centerX, centerY, 0, endAngle,
                        newColor, colorSpace, newPos, 3, tileMode, interpolation, localMatrix);
            }
            return makeDegenerateGradient(colors, colorSpace, positions, colorCount, tileMode);
        }

        if (startAngle <= 0 && endAngle >= 360) {
            // If the t-range includes [0,1], then we can always use clamping (presumably faster).
            tileMode = TILE_MODE_CLAMP;
        }

        float tStart = startAngle / 360;
        float tEnd = endAngle / 360;

        @SharedPtr
        Shader s = new AngularGradient(centerX, centerY,
                tStart, tEnd,
                colors,
                colorSpace,
                positions,
                colorCount,
                tileMode,
                interpolation);
        Matrix lm = localMatrix != null ? new Matrix(localMatrix) : new Matrix();
        return new LocalMatrixShader(s, // move
                lm);
    }

    @Override
    public int asGradient() {
        return GRADIENT_TYPE_ANGULAR;
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }

    public float getTBias() {
        return mTBias;
    }

    public float getTScale() {
        return mTScale;
    }
}
