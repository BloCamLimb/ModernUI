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

/**
 * Base class for shaders that generate gradient colors.
 */
public abstract sealed class GradientShader implements Shader
        permits Gradient1DShader, Gradient2DShader {

    //TODO currently only 1D gradients are implemented

    public static final float kDegenerateTolerance = 1F / (1 << 16);

    @Override
    public void ref() {
    }

    @Override
    public void unref() {
    }

    /**
     * Color interpolation method, is packed into an int.
     */
    public static class Interpolation {

        /**
         * Interpolation color space.
         */
        public static final byte
                // interpolate in the color space of the destination surface
                kDestination_ColorSpace = 0,
                kSRGB_ColorSpace = 1,
                kSRGBLinear_ColorSpace = 2,
                kLab_ColorSpace = 3,
                kOKLab_ColorSpace = 4,
                kOKLabGamutMap_ColorSpace = 5,  // same as Skia
                kHSL_ColorSpace = 6,
                kHWB_ColorSpace = 7,
                kLCH_ColorSpace = 8,
                kOKLCH_ColorSpace = 9,
                kOKLCHGamutMap_ColorSpace = 10; // same as Skia
        public static final byte
                kLast_ColorSpace = kOKLCHGamutMap_ColorSpace;
        public static final int kColorSpaceCount = kLast_ColorSpace + 1;

        /**
         * Hue interpolation method.
         */
        public static final byte
                kShorter_HueMethod = 0,
                kLonger_HueMethod = 1,
                kIncreasing_HueMethod = 2,
                kDecreasing_HueMethod = 3;
        public static final byte
                kLast_HueMethod = kDecreasing_HueMethod;
        public static final int kHueMethodCount = kLast_HueMethod + 1;

        /**
         * Make a packed color interpolation method.
         * <p>
         * By default, gradients will interpolate their colors in unpremul space
         * and then premultiply each of the results. By setting <var>inPremul</var>
         * to true, the gradients will premultiply their colors first, and then
         * interpolate between them.
         *
         * @param inPremul   whether interpolate colors in premul space
         * @param colorSpace the interpolation color space
         * @param hueMethod  the hue interpolation method, for LCH, OKLCH, HSL, or HWB
         * @return a packed color interpolation method
         */
        public static int make(boolean inPremul,
                               byte colorSpace,
                               byte hueMethod) {
            assert colorSpace >= 0 && colorSpace <= kLast_ColorSpace;
            assert hueMethod >= 0 && hueMethod <= kLast_HueMethod;
            return (inPremul ? 1 : 0) | (colorSpace << 8) | (hueMethod << 16);
        }

        public static boolean isInPremul(int interpolation) {
            return (interpolation & 0x1) != 0;
        }

        public static byte getColorSpace(int interpolation) {
            return (byte) (interpolation >> 8);
        }

        public static byte getHueMethod(int interpolation) {
            return (byte) (interpolation >> 16);
        }
    }
}
