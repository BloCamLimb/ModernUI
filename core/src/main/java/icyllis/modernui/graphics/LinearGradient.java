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
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.util.Objects;

/**
 * LinearGradient generates gradient colors linearly interpolated between two points.
 */
public class LinearGradient extends GradientShader {

    private final Matrix mLocalMatrix;

    /**
     * Simplified constructor that takes two 0xAARRGGBB colors in sRGB color space.
     *
     * @see #LinearGradient(float, float, float, float, float[], ColorSpace, float[], TileMode, Matrix)
     */
    public LinearGradient(float startX, float startY,
                          float endX, float endY,
                          @ColorInt int startColor,
                          @ColorInt int endColor,
                          @NonNull TileMode tileMode,
                          @Nullable Matrix localMatrix) {
        this(startX, startY, endX, endY, new int[]{startColor, endColor}, null,
                tileMode, localMatrix);
    }

    /**
     * Simplified constructor that takes an array of 0xAARRGGBB colors in sRGB color space.
     *
     * @see #LinearGradient(float, float, float, float, float[], ColorSpace, float[], TileMode, Matrix)
     */
    public LinearGradient(float startX, float startY,
                          float endX, float endY,
                          @NonNull @ColorInt int[] colors,
                          @Nullable float[] positions,
                          @NonNull TileMode tileMode,
                          @Nullable Matrix localMatrix) {
        this(startX, startY, endX, endY, convertColors(colors), null, positions,
                tileMode, localMatrix);
    }

    /**
     * Create a linear gradient shader.
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
     * All given arguments will be simplified and copied into the internal shader.
     *
     * @param startX      x-coordinate for the start point
     * @param startY      y-coordinate for the start point
     * @param endX        x-coordinate for the end point
     * @param endY        y-coordinate for the end point
     * @param colors      color array
     * @param colorSpace  color space, null will use the default one (sRGB)
     * @param positions   position array
     * @param tileMode    tile mode
     * @param localMatrix local matrix
     * @throws IllegalArgumentException NaN, infinity, non-invertible matrix, etc.
     */
    public LinearGradient(float startX, float startY,
                          float endX, float endY,
                          @Size(multiple = 4) @NonNull float[] colors,
                          @Nullable ColorSpace colorSpace,
                          @Nullable float[] positions,
                          @NonNull TileMode tileMode,
                          @Nullable Matrix localMatrix) {
        this(startX, startY, endX, endY,
                colors, colorSpace, positions, colors.length / 4, tileMode,
                icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.make(
                        true,
                        icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kDestination_ColorSpace,
                        icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kShorter_HueMethod
                ),
                localMatrix);
    }

    private LinearGradient(float startX, float startY,
                           float endX, float endY,
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
        var shader = icyllis.arc3d.sketch.shaders.LinearGradient.make(
                startX, startY,
                endX, endY,
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
        assert shader.isTriviallyCounted();
        //mCleanup = Core.registerNativeResource(this, shader);
        mShader = shader;
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
     * Builder pattern of {@link LinearGradient}.
     */
    public static class Builder extends GradientShader.Builder {

        final float mStartX, mStartY;
        final float mEndX, mEndY;
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

        public Builder(float startX, float startY,
                       float endX, float endY,
                       @NonNull TileMode tileMode) {
            this(startX, startY, endX, endY, tileMode, null);
        }

        public Builder(float startX, float startY,
                       float endX, float endY,
                       @NonNull TileMode tileMode,
                       @Nullable ColorSpace colorSpace) {
            mStartX = startX;
            mStartY = startY;
            mEndX = endX;
            mEndY = endY;
            mTileMode = Objects.requireNonNull(tileMode);
            mColorSpace = colorSpace;
            mColors = new FloatArrayList();
        }

        public Builder(float startX, float startY,
                       float endX, float endY,
                       @NonNull TileMode tileMode,
                       @Nullable ColorSpace colorSpace,
                       int colorCount) {
            if (colorCount < 1) {
                throw new IllegalArgumentException("needs >= 1 number of colors");
            }
            if (colorCount > Integer.MAX_VALUE / 4) {
                throw new IllegalArgumentException("needs <= 536,870,911 number of colors");
            }
            mStartX = startX;
            mStartY = startY;
            mEndX = endX;
            mEndY = endY;
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
         * Create the linear gradient, this builder cannot be reused anymore.
         *
         * @throws IllegalArgumentException no color, NaN, infinity, non-invertible matrix, etc.
         */
        @NonNull
        @Override
        public LinearGradient build() {
            int colorCount = getColorCount();
            if (mPositions != null && colorCount != mPositions.size()) {
                throw new IllegalArgumentException("color and position arrays must be of equal length");
            }
            return new LinearGradient(
                    mStartX, mStartY,
                    mEndX, mEndY,
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
