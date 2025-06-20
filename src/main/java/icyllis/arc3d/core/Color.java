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

package icyllis.arc3d.core;

import org.jspecify.annotations.NonNull;

public final class Color {

    /**
     * Represents fully transparent Color. May be used to initialize a destination
     * containing a mask or a non-rectangular image.
     */
    @ColorInt
    public static final int TRANSPARENT = 0;

    /**
     * Represents fully opaque black.
     */
    @ColorInt
    public static final int BLACK = 0xFF000000;

    /**
     * Represents fully opaque dark gray.
     * Note that SVG dark gray is equivalent to 0xFFA9A9A9.
     */
    @ColorInt
    public static final int DKGRAY = 0xFF444444;

    /**
     * Represents fully opaque gray.
     * Note that HTML gray is equivalent to 0xFF808080.
     */
    @ColorInt
    public static final int GRAY = 0xFF888888;

    /**
     * Represents fully opaque light gray. HTML silver is equivalent to 0xFFC0C0C0.
     * Note that SVG light gray is equivalent to 0xFFD3D3D3.
     */
    @ColorInt
    public static final int LTGRAY = 0xFFCCCCCC;

    /**
     * Represents fully opaque white.
     */
    @ColorInt
    public static final int WHITE = 0xFFFFFFFF;

    /**
     * Represents fully opaque red.
     */
    @ColorInt
    public static final int RED = 0xFFFF0000;

    /**
     * Represents fully opaque green. HTML lime is equivalent.
     * Note that HTML green is equivalent to 0xFF008000.
     */
    @ColorInt
    public static final int GREEN = 0xFF00FF00;

    /**
     * Represents fully opaque blue.
     */
    @ColorInt
    public static final int BLUE = 0xFF0000FF;

    /**
     * Represents fully opaque yellow.
     */
    @ColorInt
    public static final int YELLOW = 0xFFFFFF00;

    /**
     * Represents fully opaque cyan. HTML aqua is equivalent.
     */
    @ColorInt
    public static final int CYAN = 0xFF00FFFF;

    /**
     * Represents fully opaque magenta. HTML fuchsia is equivalent.
     */
    @ColorInt
    public static final int MAGENTA = 0xFFFF00FF;

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
     * Returns un-premultiplied color with red, blue, and green set from <code>color</code>;
     * and alpha set from <code>alpha</code>. The alpha component of <code>color</code> is
     * ignored and is replaced by <code>alpha</code> in result.
     *
     * @param color packed RGB, eight bits per component
     * @param alpha alpha: transparent at zero, fully opaque at 255
     * @return color with transparency
     */
    @ColorInt
    public static int alpha(@ColorInt int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * Return a color-int from 8-bit alpha, red, green, blue components.
     * The alpha component is implicitly set fully opaque to 255.
     * These component values should be \([0..255]\), but there is no
     * range check performed, so if they are out of range, the returned
     * color is undefined.
     *
     * @param red   red component \([0..255]\) of the color
     * @param green green component \([0..255]\) of the color
     * @param blue  blue component \([0..255]\) of the color
     * @return color and alpha, un-premultiplied
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
     * @param red   red component \([0..1]\) of the color
     * @param green green component \([0..1]\) of the color
     * @param blue  blue component \([0..1]\) of the color
     * @return color and alpha, un-premultiplied
     */
    @ColorInt
    public static int rgb(float red, float green, float blue) {
        return 0xFF000000 |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }

    /**
     * Return a color-int from 8-bit alpha, red, green, blue components.
     * These component values should be \([0..255]\), but there is no
     * range check performed, so if they are out of range, the returned
     * color is undefined. Since color is un-premultiplied, alpha may be
     * smaller than the largest of red, green, and blue.
     *
     * @param alpha alpha component \([0..255]\) of the color
     * @param red   red component \([0..255]\) of the color
     * @param green green component \([0..255]\) of the color
     * @param blue  blue component \([0..255]\) of the color
     * @return color and alpha, un-premultiplied
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
     * @param alpha alpha component \([0..1]\) of the color
     * @param red   red component \([0..1]\) of the color
     * @param green green component \([0..1]\) of the color
     * @param blue  blue component \([0..1]\) of the color
     * @return color and alpha, un-premultiplied
     */
    @ColorInt
    public static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255.0f + 0.5f) << 24) |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }

    /**
     * Describes different color channels one can manipulate.
     */
    public static final int
            COLOR_CHANNEL_R = 0, // the red channel
            COLOR_CHANNEL_G = 1, // the green channel
            COLOR_CHANNEL_B = 2, // the blue channel
            COLOR_CHANNEL_A = 3; // the alpha channel

    /**
     * Used to represent the channels available in a color type or texture format as a mask.
     */
    public static final int
            COLOR_CHANNEL_FLAG_RED = 1 << COLOR_CHANNEL_R,
            COLOR_CHANNEL_FLAG_GREEN = 1 << COLOR_CHANNEL_G,
            COLOR_CHANNEL_FLAG_BLUE = 1 << COLOR_CHANNEL_B,
            COLOR_CHANNEL_FLAG_ALPHA = 1 << COLOR_CHANNEL_A,
            COLOR_CHANNEL_FLAG_GRAY = 0x10;

    // Convenience values
    public static final int
            COLOR_CHANNEL_FLAGS_RG = COLOR_CHANNEL_FLAG_RED | COLOR_CHANNEL_FLAG_GREEN,
            COLOR_CHANNEL_FLAGS_RGB = COLOR_CHANNEL_FLAGS_RG | COLOR_CHANNEL_FLAG_BLUE,
            COLOR_CHANNEL_FLAGS_RGBA = COLOR_CHANNEL_FLAGS_RGB | COLOR_CHANNEL_FLAG_ALPHA;

    private Color() {
    }

    /**
     * Converts RGB to its HSV components.
     * hsv[0] contains hsv hue, a value from zero to less than 360.
     * hsv[1] contains hsv saturation, a value from zero to one.
     * hsv[2] contains hsv value, a value from zero to one.
     *
     * @param r   red component value from zero to 255
     * @param g   green component value from zero to 255
     * @param b   blue component value from zero to 255
     * @param hsv three element array which holds the resulting HSV components
     */
    public static void RGBToHSV(int r, int g, int b, float[] hsv) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));

        int delta = max - min;

        if (delta == 0) {
            hsv[0] = 0;
            hsv[1] = 0;
            hsv[2] = max / 255.0f;
            return;
        }

        float h;

        if (max == r) {
            h = (float) (g - b) / delta;
        } else if (max == g) {
            h = 2.0f + (float) (b - r) / delta;
        } else { // max == blue
            h = 4.0f + (float) (r - g) / delta;
        }

        h *= 60;
        if (h < 0) {
            h += 360;
        }

        hsv[0] = h;
        hsv[1] = (float) delta / max;
        hsv[2] = max / 255.0f;
    }

    /**
     * Converts RGB to its HSV components. Alpha in ARGB (if it has) is ignored.
     * hsv[0] contains hsv hue, and is assigned a value from zero to less than 360.
     * hsv[1] contains hsv saturation, a value from zero to one.
     * hsv[2] contains hsv value, a value from zero to one.
     *
     * @param color RGB or ARGB color to convert
     * @param hsv   three element array which holds the resulting HSV components
     */
    public static void RGBToHSV(int color, float[] hsv) {
        RGBToHSV(red(color), green(color), blue(color), hsv);
    }

    /**
     * Converts HSV components to an RGB color. Alpha is NOT implicitly set.
     * <p>
     * Out of range hsv values are clamped.
     *
     * @param h hsv hue, an angle from zero to less than 360
     * @param s hsv saturation, and varies from zero to one
     * @param v hsv value, and varies from zero to one
     * @return RGB equivalent to HSV, without alpha
     */
    public static int HSVToColor(float h, float s, float v) {
        s = MathUtil.pin(s, 0.0f, 1.0f);
        v = MathUtil.pin(v, 0.0f, 1.0f);

        if (s <= 1.0f / 1024.0f) {
            int i = (int) (v * 255.0f + 0.5f);
            return (i << 16) | (i << 8) | i;
        }

        float hx = (h < 0 || h >= 360.0f) ? 0 : h / 60.0f;
        int w = (int) hx;
        float f = hx - w;

        float p = v * (1.0f - s);
        float q = v * (1.0f - (s * f));
        float t = v * (1.0f - (s * (1.0f - f)));

        float r, g, b;

        switch (w) {
            case 0 -> {
                r = v;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = v;
                b = p;
            }
            case 2 -> {
                r = p;
                g = v;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = v;
            }
            case 4 -> {
                r = t;
                g = p;
                b = v;
            }
            default -> {
                r = v;
                g = p;
                b = q;
            }
        }
        return ((int) (r * 255.0f + 0.5f) << 16) |
                ((int) (g * 255.0f + 0.5f) << 8) |
                (int) (b * 255.0f + 0.5f);
    }

    /**
     * Converts HSV components to an RGB color. Alpha is NOT implicitly set.
     * hsv[0] represents hsv hue, an angle from zero to less than 360.
     * hsv[1] represents hsv saturation, and varies from zero to one.
     * hsv[2] represents hsv value, and varies from zero to one.
     * <p>
     * Out of range hsv values are clamped.
     *
     * @param hsv three element array which holds the input HSV components
     * @return RGB equivalent to HSV, without alpha
     */
    public static int HSVToColor(float @NonNull[] hsv) {
        return HSVToColor(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * Converts a color component from the sRGB space to the linear RGB space,
     * using the sRGB transfer function.
     *
     * @param x a color component
     * @return transformed color component
     */
    public static float GammaToLinear(float x) {
        return x < 0.04045
                ? x / 12.92f
                : (float) Math.pow((x + 0.055) / 1.055, 2.4);
    }

    /**
     * Converts a color component from the linear RGB space to the sRGB space,
     * using the inverse of sRGB transfer function.
     *
     * @param x a color component
     * @return transformed color component
     */
    public static float LinearToGamma(float x) {
        return x < 0.04045 / 12.92
                ? x * 12.92f
                : (float) Math.pow(x, 1.0 / 2.4) * 1.055f - 0.055f;
    }

    /**
     * Converts a color from the sRGB space to the linear RGB space,
     * using the sRGB transfer function.
     *
     * @param col the color components
     */
    public static void GammaToLinear(float @NonNull[] col) {
        col[0] = GammaToLinear(col[0]);
        col[1] = GammaToLinear(col[1]);
        col[2] = GammaToLinear(col[2]);
    }

    /**
     * Converts a color from the linear RGB space to the sRGB space,
     * using the inverse of sRGB transfer function.
     *
     * @param col the color components
     */
    public static void LinearToGamma(float @NonNull[] col) {
        col[0] = LinearToGamma(col[0]);
        col[1] = LinearToGamma(col[1]);
        col[2] = LinearToGamma(col[2]);
    }

    /**
     * Converts a linear RGB color to a luminance value.
     */
    public static float luminance(float r, float g, float b) {
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    /**
     * Converts a linear RGB color to a luminance value.
     *
     * @param col the color components
     */
    public static float luminance(float[] col) {
        return luminance(col[0], col[1], col[2]);
    }

    /**
     * Coverts a luminance value to a perceptual lightness value.
     */
    public static float lightness(float lum) {
        return lum <= 216.0 / 24389.0
                ? lum * 24389.0f / 27.0f
                : (float) Math.pow(lum, 1.0 / 3.0) * 116.0f - 16.0f;
    }

    public static float @NonNull[] load_and_premul(int col) {
        float[] col4 = new float[4];
        float a = (col4[3] = (col >>> 24) * (1 / 255.0f)) * (1 / 255.0f);
        col4[0] = ((col >> 16) & 0xFF) * a;
        col4[1] = ((col >> 8) & 0xFF) * a;
        col4[2] = (col & 0xFF) * a;
        return col4;
    }
}
