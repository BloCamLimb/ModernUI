/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.ColorSpace;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.Size;
import icyllis.modernui.core.Core;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.util.Objects;

/**
 * AngularGradient generates gradient colors linearly interpolated in the
 * angular direction of a circle, also known as sweep gradient, conic gradient.
 */
public class AngularGradient extends GradientShader {

    private final Matrix mLocalMatrix;

    /**
     * Simplified constructor that takes two 0xAARRGGBB colors in sRGB color space,
     * and uses a full circle.
     *
     * @see #AngularGradient(float, float, float, float, float[], ColorSpace, float[], TileMode, Matrix)
     */
    public AngularGradient(float centerX, float centerY,
                           @ColorInt int startColor,
                           @ColorInt int endColor,
                           @Nullable Matrix localMatrix) {
        this(centerX, centerY, 0, 360,
                new int[]{startColor, endColor}, null,
                TileMode.CLAMP, localMatrix);
    }

    /**
     * Simplified constructor that takes an array of 0xAARRGGBB colors in sRGB color space,
     * and uses a full circle.
     *
     * @see #AngularGradient(float, float, float, float, float[], ColorSpace, float[], TileMode, Matrix)
     */
    public AngularGradient(float centerX, float centerY,
                           @NonNull @ColorInt int[] colors,
                           @Nullable float[] positions,
                           @Nullable Matrix localMatrix) {
        this(centerX, centerY, 0, 360,
                colors, positions,
                TileMode.CLAMP, localMatrix);
    }

    /**
     * Simplified constructor that takes two 0xAARRGGBB colors in sRGB color space.
     *
     * @see #AngularGradient(float, float, float, float, float[], ColorSpace, float[], TileMode, Matrix)
     */
    public AngularGradient(float centerX, float centerY,
                           float startAngle, float endAngle,
                           @ColorInt int centerColor,
                           @ColorInt int edgeColor,
                           @NonNull TileMode tileMode,
                           @Nullable Matrix localMatrix) {
        this(centerX, centerY, startAngle, endAngle,
                new int[]{centerColor, edgeColor}, null,
                tileMode, localMatrix);
    }

    /**
     * Simplified constructor that takes an array of 0xAARRGGBB colors in sRGB color space.
     *
     * @see #AngularGradient(float, float, float, float, float[], ColorSpace, float[], TileMode, Matrix)
     */
    public AngularGradient(float centerX, float centerY,
                           float startAngle, float endAngle,
                           @NonNull @ColorInt int[] colors,
                           @Nullable float[] positions,
                           @NonNull TileMode tileMode,
                           @Nullable Matrix localMatrix) {
        this(centerX, centerY, startAngle, endAngle,
                convertColors(colors), null, positions,
                tileMode, localMatrix);
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
     * source colors to interpolate, they have non-premultiplied alpha and are in
     * the given <var>colorSpace</var>.
     * <p>
     * The <var>positions</var> array specifies a number of stops, all values
     * are between 0 and 1 and monotonic increasing. Colors will be linearly
     * interpolated in each stop. Null means that they are uniformly distributed.
     * If the first position is not 0 or the last position is not 1, an implicit
     * stop will be added.
     * <p>
     * The <var>tileMode</var> specifies the behavior when local coords are out of
     * bounds.
     * <p>
     * The <var>localMatrix</var> specifies an additional local matrix for this
     * gradient, it transforms gradient's local coordinates to geometry's local
     * coordinates, null means identity.
     * <p>
     * All given arguments will be simplified and copied into the return shader.
     *
     * @param centerX     x of center point
     * @param centerY     y of center point
     * @param startAngle  start angle in degrees
     * @param endAngle    end angle in degrees
     * @param colors      color array
     * @param colorSpace  color space, null will use the default one (sRGB)
     * @param positions   position array
     * @param tileMode    tile mode
     * @param localMatrix local matrix
     * @throws IllegalArgumentException NaN, infinity, non-invertible matrix, etc.
     */
    public AngularGradient(float centerX, float centerY,
                           float startAngle, float endAngle,
                           @Size(multiple = 4) @NonNull float[] colors,
                           @Nullable ColorSpace colorSpace,
                           @Nullable float[] positions,
                           @NonNull TileMode tileMode,
                           @Nullable Matrix localMatrix) {
        this(centerX, centerY, startAngle, endAngle,
                colors, colorSpace, positions, colors.length / 4, tileMode,
                icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.make(
                        true,
                        icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kDestination_ColorSpace,
                        icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kShorter_HueMethod
                ),
                localMatrix);
    }

    private AngularGradient(float centerX, float centerY,
                            float startAngle, float endAngle,
                            @Size(multiple = 4) @NonNull float[] colors,
                            @Nullable ColorSpace colorSpace,
                            @Nullable float[] positions,
                            int colorCount,
                            @NonNull TileMode tileMode,
                            int interpolation,
                            @Nullable Matrix localMatrix) {
        if (colorCount < 1) {
            throw new IllegalArgumentException("needs >= 1 number of colors");
        }
        var shader = icyllis.arc3d.sketch.shaders.AngularGradient.make(
                centerX, centerY,
                startAngle, endAngle,
                colors,
                colorSpace != null ? colorSpace : ColorSpace.get(ColorSpace.Named.SRGB),
                positions,
                colorCount,
                tileMode.nativeInt,
                interpolation,
                localMatrix
        );
        if (shader == null) {
            throw new IllegalArgumentException("incomplete arrays, points are NaN, infinity, or matrix is singular");
        }
        if (localMatrix != null && !localMatrix.isIdentity()) {
            mLocalMatrix = new Matrix(localMatrix);
        } else {
            mLocalMatrix = null;
        }
        if (!shader.isTriviallyCounted()) {
            assert false;
            mCleanup = Core.registerNativeResource(this, shader);
        }
        mShader = shader;
    }

    private AngularGradient(@NonNull icyllis.arc3d.sketch.shaders.Shader newShader,
                            @Nullable Matrix newLocalMatrix) {
        if (newLocalMatrix != null && !newLocalMatrix.isIdentity()) {
            mLocalMatrix = new Matrix(newLocalMatrix);
        } else {
            mLocalMatrix = null;
        }
        if (!newShader.isTriviallyCounted()) {
            assert false;
            mCleanup = Core.registerNativeResource(this, newShader);
        }
        mShader = newShader;
    }

    /**
     * Return true if the shader has a non-identity local matrix.
     *
     * @param localMatrix Set to the local matrix of the shader, if the shader's matrix is non-null.
     * @return true if the shader has a non-identity local matrix
     */
    public boolean getLocalMatrix(@NonNull Matrix localMatrix) {
        if (mLocalMatrix != null) {
            localMatrix.set(mLocalMatrix);
            return true; // presence of mLocalMatrix means it's not identity
        }
        return false;
    }

    /**
     * Create a new AngularGradient that replaces its local matrix with a new local matrix.
     * This method is much faster than re-creating with the constructor when you only want to
     * modify the local matrix.
     *
     * @param newLocalMatrix the new local matrix that replaces this
     * @return a newly created shader
     */
    @NonNull
    public AngularGradient copyWithLocalMatrix(@Nullable Matrix newLocalMatrix) {
        var shader = mShader;
        if (shader == null) {
            throw new IllegalStateException("AngularGradient is already released");
        }
        var newShader = shader.makeWithLocalMatrix(newLocalMatrix != null ? newLocalMatrix :
                icyllis.arc3d.sketch.Matrix.identity(), icyllis.arc3d.sketch.shaders.Shader.LOCAL_MATRIX_REPLACE);
        return new AngularGradient(newShader, newLocalMatrix);
    }

    /**
     * Builder pattern of {@link AngularGradient}.
     */
    public static class Builder extends GradientShader.Builder {

        final float mCenterX, mCenterY;
        final float mStartAngle, mEndAngle;
        @NonNull
        final TileMode mTileMode;
        @Nullable
        final ColorSpace mColorSpace;

        @NonNull
        final FloatArrayList mColors;
        @Nullable
        FloatArrayList mPositions;

        @Nullable
        Matrix mLocalMatrix;

        public Builder(float centerX, float centerY,
                       float startAngle, float endAngle,
                       @NonNull TileMode tileMode) {
            this(centerX, centerY, startAngle, endAngle, tileMode, null);
        }

        public Builder(float centerX, float centerY,
                       float startAngle, float endAngle,
                       @NonNull TileMode tileMode,
                       @Nullable ColorSpace colorSpace) {
            mCenterX = centerX;
            mCenterY = centerY;
            mStartAngle = startAngle;
            mEndAngle = endAngle;
            mTileMode = Objects.requireNonNull(tileMode);
            mColorSpace = colorSpace;
            mColors = new FloatArrayList();
        }

        public Builder(float centerX, float centerY,
                       float startAngle, float endAngle,
                       @NonNull TileMode tileMode,
                       @Nullable ColorSpace colorSpace,
                       int colorCount) {
            if (colorCount < 1) {
                throw new IllegalArgumentException("needs >= 1 number of colors");
            }
            if (colorCount > Integer.MAX_VALUE / 4) {
                throw new IllegalArgumentException("needs <= 536,870,911 number of colors");
            }
            mCenterX = centerX;
            mCenterY = centerY;
            mStartAngle = startAngle;
            mEndAngle = endAngle;
            mTileMode = Objects.requireNonNull(tileMode);
            mColorSpace = colorSpace;
            mColors = new FloatArrayList(colorCount * 4);
        }

        /**
         * Add a color representing the color of the i-th stop.
         */
        @NonNull
        public Builder addColor(@ColorInt int color) {
            mColors.add(((color >> 16) & 0xff) * (1 / 255.0f));
            mColors.add(((color >> 8) & 0xff) * (1 / 255.0f));
            mColors.add((color & 0xff) * (1 / 255.0f));
            mColors.add((color >>> 24) * (1 / 255.0f));
            return this;
        }

        /**
         * Add a color representing the color of the i-th stop.
         */
        @NonNull
        public Builder addColor(float r, float g, float b, float a) {
            mColors.add(r);
            mColors.add(g);
            mColors.add(b);
            mColors.add(a);
            return this;
        }

        /**
         * Add a number between 0 and 1, inclusive, representing the position of the i-th color stop.
         * 0 represents the start of the gradient and 1 represents the end.
         * <p>
         * A gradient can be created with implicit positions (by assuming they are uniformly distributed).
         * Once you call this method, the number of colors must be equal to the number of positions.
         */
        @NonNull
        public Builder addPosition(float position) {
            if (mPositions == null) {
                mPositions = new FloatArrayList(mColors.elements().length / 4);
            }
            mPositions.add(position);
            return this;
        }

        /**
         * Helper version of {@link #addColor} and {@link #addPosition}.
         */
        @NonNull
        public Builder addColorStop(float offset, @ColorInt int color) {
            return addColor(color)
                    .addPosition(offset);
        }

        /**
         * Returns the initial number of color stops.
         */
        public int getColorCount() {
            return mColors.size() / 4;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder setInterpolationInPremul(boolean interpolationInPremul) {
            super.setInterpolationInPremul(interpolationInPremul);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder setInterpolationColorSpace(@NonNull InterpolationColorSpace interpolationColorSpace) {
            super.setInterpolationColorSpace(interpolationColorSpace);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder setHueInterpolationMethod(@NonNull HueInterpolationMethod hueInterpolationMethod) {
            super.setHueInterpolationMethod(hueInterpolationMethod);
            return this;
        }

        /**
         * This specifies an additional local matrix for the gradient, it transforms
         * gradient's local coordinates to geometry's local coordinates, null means identity.
         *
         * @param localMatrix the local matrix to set
         */
        @NonNull
        public Builder setLocalMatrix(@Nullable Matrix localMatrix) {
            if (localMatrix == null || localMatrix.isIdentity()) {
                if (mLocalMatrix != null) {
                    mLocalMatrix.setIdentity();
                }
            } else {
                if (mLocalMatrix == null) {
                    mLocalMatrix = new Matrix(localMatrix);
                } else {
                    mLocalMatrix.set(localMatrix);
                }
            }
            return this;
        }

        /**
         * Create the angular gradient, this builder cannot be reused anymore.
         *
         * @throws IllegalArgumentException no color, NaN, infinity, non-invertible matrix, etc.
         */
        @NonNull
        @Override
        public AngularGradient build() {
            int colorCount = getColorCount();
            if (mPositions != null && colorCount != mPositions.size()) {
                throw new IllegalArgumentException("color and position arrays must be of equal length");
            }
            return new AngularGradient(
                    mCenterX, mCenterY,
                    mStartAngle, mEndAngle,
                    mColors.elements(),
                    mColorSpace,
                    mPositions != null ? mPositions.elements() : null,
                    colorCount,
                    mTileMode,
                    icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.make(
                            mInterpolationInPremul,
                            mInterpolationColorSpace.nativeByte,
                            mHueInterpolationMethod.nativeByte
                    ),
                    mLocalMatrix
            );
        }
    }
}
