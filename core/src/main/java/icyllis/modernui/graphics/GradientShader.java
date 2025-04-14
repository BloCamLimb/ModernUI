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

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Base class for shaders that generate multiple gradient colors.
 *
 * @since 3.11
 */
public abstract class GradientShader extends Shader {

    /**
     * Interpolation color space.
     * See <a href="https://www.w3.org/TR/css-color-4/#interpolation-space">CSS Color Space for Interpolation</a>.
     */
    public enum InterpolationColorSpace {
        /**
         * Interpolate in the color space of the render target.
         */
        DESTINATION(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kDestination_ColorSpace),
        SRGB(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kSRGB_ColorSpace),
        SRGB_LINEAR(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kSRGBLinear_ColorSpace),
        LAB(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kLab_ColorSpace),
        OKLAB(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kOKLab_ColorSpace),
        HSL(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kHSL_ColorSpace),
        HWB(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kHWB_ColorSpace),
        LCH(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kLCH_ColorSpace),
        OKLCH(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kOKLCH_ColorSpace);

        final byte nativeByte;

        InterpolationColorSpace(byte nativeByte) {
            this.nativeByte = nativeByte;
        }
    }

    /**
     * Hue interpolation method.
     * See <a href="https://www.w3.org/TR/css-color-4/#hue-interpolation">CSS Hue Interpolation</a>
     */
    public enum HueInterpolationMethod {
        /**
         * Hue angles are interpolated to take the shorter of the two arcs between the
         * starting and ending hues.
         */
        SHORTER(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kShorter_HueMethod),
        /**
         * Hue angles are interpolated to take the longer of the two arcs between the
         * starting and ending hues.
         */
        LONGER(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kLonger_HueMethod),
        /**
         * Hue angles are interpolated so that, as they progress from the first color to
         * the second, the angle is always increasing. If the angle increases to 360 it
         * is reset to zero, and then continues increasing.
         * <p>
         * Depending on the difference between the two angles, this will either look the
         * same as shorter or as longer. However, if one of the hue angles is being animated,
         * and the hue angle difference passes through 180 degrees, the interpolation will
         * not flip to the other arc.
         */
        INCREASING(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kIncreasing_HueMethod),
        /**
         * Hue angles are interpolated so that, as they progress from the first color to
         * the second, the angle is always decreasing. If the angle decreases to 0 it
         * is reset to 360, and then continues decreasing.
         * <p>
         * Depending on the difference between the two angles, this will either look the
         * same as shorter or as longer. However, if one of the hue angles is being animated,
         * and the hue angle difference passes through 180 degrees, the interpolation will
         * not flip to the other arc.
         */
        DECREASING(icyllis.arc3d.sketch.shaders.GradientShader.Interpolation.kDecreasing_HueMethod);

        final byte nativeByte;

        HueInterpolationMethod(byte nativeByte) {
            this.nativeByte = nativeByte;
        }
    }

    @ApiStatus.Internal
    protected static float[] convertColors(@NonNull @ColorInt int[] colors) {
        if (colors.length < 1) {
            throw new IllegalArgumentException("needs >= 1 number of colors");
        }
        if (colors.length > Integer.MAX_VALUE / 4) {
            throw new IllegalArgumentException("needs <= 536,870,911 number of colors");
        }

        float[] result = new float[colors.length * 4];
        for (int i = 0, j = 0; i < colors.length; i += 1, j += 4) {
            int color = colors[i];
            result[j] = ((color >> 16) & 0xff) * (1 / 255.0f);
            result[j|1] = ((color >> 8) & 0xff) * (1 / 255.0f);
            result[j|2] = (color & 0xff) * (1 / 255.0f);
            result[j|3] = (color >>> 24) * (1 / 255.0f);
        }

        return result;
    }

    public static abstract class Builder {

        boolean mInterpolationInPremul = true;
        @NonNull
        InterpolationColorSpace mInterpolationColorSpace = InterpolationColorSpace.DESTINATION;
        @NonNull
        HueInterpolationMethod mHueInterpolationMethod = HueInterpolationMethod.SHORTER;

        /**
         * @see #setInterpolationInPremul(boolean)
         */
        public boolean isInterpolationInPremul() {
            return mInterpolationInPremul;
        }

        /**
         * If false, gradients will interpolate their colors in unpremul space
         * and then premultiply each of the results. If true, gradients will
         * premultiply their colors first, and then interpolate between them.
         * <p>
         * Interpolating colors using the premultiplied representations tends to
         * produce more attractive transitions than the non-premultiplied representations,
         * particularly when transitioning from a fully opaque color to fully transparent.
         * <p>
         * The default is true to match CSS requirements.
         *
         * @param interpolationInPremul whether interpolate colors in premul space
         */
        public Builder setInterpolationInPremul(boolean interpolationInPremul) {
            mInterpolationInPremul = interpolationInPremul;
            return this;
        }

        /**
         * @see #setInterpolationColorSpace(InterpolationColorSpace)
         */
        @NonNull
        public InterpolationColorSpace getInterpolationColorSpace() {
            return mInterpolationColorSpace;
        }

        /**
         * Set the color space for interpolation.
         * <p>
         * The default is {@link InterpolationColorSpace#DESTINATION}.
         */
        public Builder setInterpolationColorSpace(@NonNull InterpolationColorSpace interpolationColorSpace) {
            mInterpolationColorSpace = interpolationColorSpace;
            return this;
        }

        /**
         * @see #setHueInterpolationMethod(HueInterpolationMethod)
         */
        @NonNull
        public HueInterpolationMethod getHueInterpolationMethod() {
            return mHueInterpolationMethod;
        }

        /**
         * For color functions with a hue angle (LCH, HSL, HWB etc.), this specifies a way
         * to interpolate hue values.
         * <p>
         * The default is {@link HueInterpolationMethod#SHORTER}.
         */
        public Builder setHueInterpolationMethod(@NonNull HueInterpolationMethod hueInterpolationMethod) {
            mHueInterpolationMethod = hueInterpolationMethod;
            return this;
        }

        @NonNull
        public abstract GradientShader build();
    }
}
