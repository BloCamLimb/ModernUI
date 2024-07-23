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

package icyllis.arc3d.core.shaders;

import icyllis.arc3d.core.*;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.*;

/**
 * LinearGradient generates gradient colors linearly interpolated between two points.
 */
public final class LinearGradient extends Gradient1DShader {

    private final float mStartX;
    private final float mStartY;
    private final float mEndX;
    private final float mEndY;

    @VisibleForTesting
    public LinearGradient(float startX, float startY,
                          float endX, float endY,
                          @Nonnull float[] colors,
                          @Nullable ColorSpace colorSpace,
                          @Nullable float[] positions,
                          int colorCount,
                          int tileMode,
                          int interpolation) {
        super(colors, colorSpace, positions, colorCount, tileMode, interpolation,
                pts_to_unit_matrix(startX, startY, endX, endY));
        mStartX = startX;
        mStartY = startY;
        mEndX = endX;
        mEndY = endY;
    }

    private static Matrix pts_to_unit_matrix(float startX, float startY,
                                             float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float mag = Point.length(dx, dy);
        float inv = mag != 0 ? 1 / mag : 0;
        dx *= inv;
        dy *= inv;
        var matrix = new Matrix();
        matrix.setSinCos(-dy, dx, startX, startY);
        matrix.postTranslate(-startX, -startY);
        matrix.postScale(inv, inv);
        return matrix;
    }

    /**
     * Create a linear gradient shader.
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
     * {@link icyllis.arc3d.core.shaders.GradientShader.Interpolation}.
     * <p>
     * The <var>localMatrix</var> specifies an additional local matrix for this
     * gradient, null means identity.
     * <p>
     * All given arguments will be simplified and copied into the return shader.
     * Null is returned if there are any illegal arguments (NaN, Inf, non-invertible
     * matrix, etc.).
     *
     * @param startX        x of start point
     * @param startY        y of start point
     * @param endX          x of end point
     * @param endY          y of end point
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
    public static Shader makeLinear(float startX, float startY,
                                    float endX, float endY,
                                    @Nonnull float[] colors,
                                    @Nullable ColorSpace colorSpace,
                                    @Nullable float[] positions,
                                    int colorCount,
                                    int tileMode,
                                    int interpolation,
                                    @Nullable Matrixc localMatrix) {
        float dist = Point.distanceTo(endX, endY, startX, startY);
        if (!Float.isFinite(dist)) {
            return null;
        }
        if (!checkGradient1D(colors, positions, colorCount, tileMode)) {
            return null;
        }
        if (colorCount == 1) {
            return new Color4fShader(colors[0], colors[1], colors[2], colors[3], colorSpace);
        }
        if (localMatrix != null && !localMatrix.invert(null)) {
            return null;
        }
        if (Math.abs(dist) <= kDegenerateTolerance) {
            return makeDegenerateGradient(colors, colorSpace, positions, colorCount, tileMode);
        }

        @SharedPtr
        Shader s = new LinearGradient(startX, startY,
                endX, endY,
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
        return GRADIENT_TYPE_LINEAR;
    }

    public float getStartX() {
        return mStartX;
    }

    public float getStartY() {
        return mStartY;
    }

    public float getEndX() {
        return mEndX;
    }

    public float getEndY() {
        return mEndY;
    }
}
