/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import javax.annotation.Nonnull;

public class Color {

    @ColorInt
    public static final int TRANSPARENT = 0;

    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    public static int alpha(@ColorInt int color) {
        return color >>> 24;
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    public static int red(@ColorInt int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    public static int green(@ColorInt int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    public static int blue(@ColorInt int color) {
        return color & 0xFF;
    }

    /**
     * Return a color-int from red, green, blue components.
     * The alpha component is implicitly 255 (fully opaque).
     * These component values should be \([0..255]\), but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     *
     * @param red   Red component \([0..255]\) of the color
     * @param green Green component \([0..255]\) of the color
     * @param blue  Blue component \([0..255]\) of the color
     */
    @ColorInt
    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /**
     * Return a color-int from red, green, blue float components
     * in the range \([0..1]\). The alpha component is implicitly
     * 1.0 (fully opaque). If the components are out of range, the
     * returned color is undefined.
     *
     * @param red   Red component \([0..1]\) of the color
     * @param green Green component \([0..1]\) of the color
     * @param blue  Blue component \([0..1]\) of the color
     */
    @ColorInt
    public static int rgb(float red, float green, float blue) {
        return 0xFF000000 |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }

    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be \([0..255]\), but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     *
     * @param alpha Alpha component \([0..255]\) of the color
     * @param red   Red component \([0..255]\) of the color
     * @param green Green component \([0..255]\) of the color
     * @param blue  Blue component \([0..255]\) of the color
     */
    @ColorInt
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Return a color-int from alpha, red, green, blue float components
     * in the range \([0..1]\). If the components are out of range, the
     * returned color is undefined.
     *
     * @param alpha Alpha component \([0..1]\) of the color
     * @param red   Red component \([0..1]\) of the color
     * @param green Green component \([0..1]\) of the color
     * @param blue  Blue component \([0..1]\) of the color
     */
    @ColorInt
    public static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255.0f + 0.5f) << 24) |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }

    /**
     * </p>Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:</p>
     *
     * <ul>
     *   <li><code>#RRGGBB</code></li>
     *   <li><code>#AARRGGBB</code></li>
     * </ul>
     */
    @ColorInt
    public static int parseColor(@Nonnull String colorString) {
        if (colorString.charAt(0) == '#') {
            int color = Integer.parseUnsignedInt(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0xFF000000;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color");
            }
            return color;
        }
        throw new IllegalArgumentException("Unknown color");
    }

    /**
     * Blends the two colors using premultiplied alpha on CPU side. This is to simulate
     * the color blending on GPU side, but this is only used for color filtering (tinting).
     * Do NOT premultiply the src and dst colors with alpha on CPU side. The returned
     * color is un-premultiplied by alpha. This method will not lose precision,
     * color components are still 8-bit.
     *
     * @param mode the blend mode that determines blending factors
     * @param src  the source color (straight) to be blended into the destination color
     * @param dst  the destination color (straight) on which the source color is to be blended
     * @return the color (straight) resulting from the color blending
     */
    @ColorInt
    public static int blend(@Nonnull BlendMode mode, @ColorInt int src, @ColorInt int dst) {
        return switch (mode) {
            case CLEAR -> TRANSPARENT;
            case SRC -> src;
            case DST -> dst;
            case SRC_OVER -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0xFF)
                    yield src;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float outA = srcA + oneMinusSrcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR + oneMinusSrcA * dstR;
                float outG = srcG + oneMinusSrcA * dstG;
                float outB = srcB + oneMinusSrcA * dstB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case DST_OVER -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0xFF)
                    yield dst;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusDstA = 1.0f - dstA;
                float outA = dstA + oneMinusDstA * srcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = dstR + oneMinusDstA * srcR;
                float outG = dstG + oneMinusDstA * srcG;
                float outB = dstB + oneMinusDstA * srcB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SRC_IN -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0xFF)
                    yield src;
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                // blend
                float outA = srcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * dstA;
                float outG = srcG * dstA;
                float outB = srcB * dstA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case DST_IN -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0xFF)
                    yield dst;
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = dstA * srcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = dstR * srcA;
                float outG = dstG * srcA;
                float outB = dstB * srcA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SRC_OUT -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                // blend
                float oneMinusDstA = 1.0f - dstA;
                float outA = srcA * oneMinusDstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * oneMinusDstA;
                float outG = srcG * oneMinusDstA;
                float outB = srcB * oneMinusDstA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case DST_OUT -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float outA = srcA * oneMinusSrcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = dstR * oneMinusSrcA;
                float outG = dstG * oneMinusSrcA;
                float outB = dstB * oneMinusSrcA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SRC_ATOP -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float outR = srcR * dstA + dstR * oneMinusSrcA;
                float outG = srcG * dstA + dstG * oneMinusSrcA;
                float outB = srcB * dstA + dstB * oneMinusSrcA;
                // un-premultiply the out color
                float invA = 1.0f / dstA;
                yield argb(dstA, outR * invA, outG * invA, outB * invA);
            }
            case DST_ATOP -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusDstA = 1.0f - dstA;
                float outR = dstR * srcA + srcR * oneMinusDstA;
                float outG = dstG * srcA + srcG * oneMinusDstA;
                float outB = dstB * srcA + srcB * oneMinusDstA;
                // un-premultiply the out color
                float invA = 1.0f / srcA;
                yield argb(srcA, outR * invA, outG * invA, outB * invA);
            }
            case XOR -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float oneMinusSrcA = 1.0f - srcA;
                float oneMinusDstA = 1.0f - dstA;
                float outA = srcA * oneMinusDstA + dstA * oneMinusSrcA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * oneMinusDstA + dstR * oneMinusSrcA;
                float outG = srcG * oneMinusDstA + dstG * oneMinusSrcA;
                float outB = srcB * oneMinusDstA + dstB * oneMinusSrcA;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case PLUS -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = Math.min(srcA + dstA, 1);
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = Math.min(srcR + dstR, 1);
                float outG = Math.min(srcG + dstG, 1);
                float outB = Math.min(srcB + dstB, 1);
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case MULTIPLY -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield TRANSPARENT;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield TRANSPARENT;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = srcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR * dstR;
                float outG = srcG * dstG;
                float outB = srcB * dstB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            case SCREEN -> {
                int srcAlpha = alpha(src);
                if (srcAlpha == 0)
                    yield dst;
                int dstAlpha = alpha(dst);
                if (dstAlpha == 0)
                    yield src;
                // premultiply the src and dst colors
                float srcA = srcAlpha / 255.0f;
                float srcR = red(src) / 255.0f * srcA;
                float srcG = green(src) / 255.0f * srcA;
                float srcB = blue(src) / 255.0f * srcA;
                float dstA = dstAlpha / 255.0f;
                float dstR = red(dst) / 255.0f * dstA;
                float dstG = green(dst) / 255.0f * dstA;
                float dstB = blue(dst) / 255.0f * dstA;
                // blend
                float outA = srcA + dstA - srcA * dstA;
                if (outA == 0.0f)
                    yield TRANSPARENT;
                float outR = srcR + dstR - srcR * dstR;
                float outG = srcG + dstG - srcG * dstG;
                float outB = srcB + dstB - srcB * dstB;
                // un-premultiply the out color
                float invA = 1.0f / outA;
                yield argb(outA, outR * invA, outG * invA, outB * invA);
            }
            default -> src;
        };
    }
}
