/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.core;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * <p>The <code>Color</code> class provides methods for creating, converting and
 * manipulating colors.</p>
 *
 * <h3>Color int</h3>
 * <p>Color int is the most common representation.</p>
 *
 * <p>A color int always defines a color in the sRGB color space using 4 components
 * packed in a single 32 bit integer value:</p>
 *
 * <table summary="Color int definition">
 *     <tr>
 *         <th>Component</th><th>Name</th><th>Size</th><th>Range</th>
 *     </tr>
 *     <tr><td>A</td><td>Alpha</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 *     <tr><td>R</td><td>Red</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 *     <tr><td>G</td><td>Green</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 *     <tr><td>B</td><td>Blue</td><td>8 bits</td><td>\([0..255]\)</td></tr>
 * </table>
 *
 * <p>The components in this table are listed in encoding order (see below),
 * which is why color ints are called ARGB colors.</p>
 *
 * <h4>Usage in code</h4>
 * <p>To avoid confusing color ints with arbitrary integer values, it is a
 * good practice to annotate them with the <code>@ColorInt</code> annotation.</p>
 *
 * <h4>Encoding</h4>
 * <p>The four components of a color int are encoded in the following way:</p>
 * <pre class="prettyprint">
 * int color = (A & 0xff) << 24 | (R & 0xff) << 16 | (G & 0xff) << 8 | (B & 0xff);
 * </pre>
 *
 * <p>Because of this encoding, color ints can easily be described as an integer
 * constant in source. For instance, opaque blue is <code>0xff0000ff</code>
 * and yellow is <code>0xffffff00</code>.</p>
 *
 * <p>To easily encode color ints, it is recommended to use the static methods
 * {@link #argb(int, int, int, int)} and {@link #rgb(int, int, int)}. The second
 * method omits the alpha component and assumes the color is opaque (alpha is 255).
 * As a convenience this class also offers methods to encode color ints from components
 * defined in the \([0..1]\) range: {@link #argb(float, float, float, float)} and
 * {@link #rgb(float, float, float)}.</p>
 *
 * <h4>Decoding</h4>
 * <p>The four ARGB components can be individually extracted from a color int
 * using the following expressions:</p>
 * <pre class="prettyprint">
 * int A = (color >> 24) & 0xff; // or color >>> 24
 * int R = (color >> 16) & 0xff;
 * int G = (color >>  8) & 0xff;
 * int B = (color      ) & 0xff;
 * </pre>
 *
 * <p>This class offers convenience methods to easily extract these components:</p>
 * <ul>
 *     <li>{@link #alpha(int)} to extract the alpha component</li>
 *     <li>{@link #red(int)} to extract the red component</li>
 *     <li>{@link #green(int)} to extract the green component</li>
 *     <li>{@link #blue(int)} to extract the blue component</li>
 * </ul>
 *
 * <h3>Color4f</h3>
 * <p>Color4f is a representation of RGBA color values in the sRGB color space,
 * holding four floating-point components, to store colors with more precision than color
 * ints. Color components are always in a known order. RGB components may be premultiplied
 * by alpha or not, but public API always uses un-premultiplied colors.</p>
 *
 * <h3>Alpha and transparency</h3>
 * <p>The alpha component of a color defines the level of transparency of a
 * color. When the alpha component is 0, the color is completely transparent.
 * When the alpha is component is 1 (in the \([0..1]\) range) or 255 (in the
 * \([0..255]\) range), the color is completely opaque.</p>
 *
 * <p>The color representations described above do not use pre-multiplied
 * color components (a pre-multiplied color component is a color component
 * that has been multiplied by the value of the alpha component).
 * For instance, the color int representation of opaque red is
 * <code>0xffff0000</code>. For semi-transparent (50%) red, the
 * representation becomes <code>0x80ff0000</code>. The equivalent color
 * instance representations would be <code>(1.0, 0.0, 0.0, 1.0)</code>
 * and <code>(1.0, 0.0, 0.0, 0.5)</code>.</p>
 */
@SuppressWarnings("unused")
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
            R_CHANNEL = 0, // the red channel
            G_CHANNEL = 1, // the green channel
            B_CHANNEL = 2, // the blue channel
            A_CHANNEL = 3; // the alpha channel

    /**
     * Used to represent the channels available in a color type or texture format as a mask.
     */
    public static final int
            RED_CHANNEL_FLAG = 1 << R_CHANNEL,
            GREEN_CHANNEL_FLAG = 1 << G_CHANNEL,
            BLUE_CHANNEL_FLAG = 1 << B_CHANNEL,
            ALPHA_CHANNEL_FLAG = 1 << A_CHANNEL,
            GRAY_CHANNEL_FLAG = 1 << 4; // deprecated by modern APIs

    // Convenience values
    public static final int
            GRAY_ALPHA_CHANNEL_FLAGS = GRAY_CHANNEL_FLAG | ALPHA_CHANNEL_FLAG,
            RG_CHANNEL_FLAGS = RED_CHANNEL_FLAG | GREEN_CHANNEL_FLAG,
            RGB_CHANNEL_FLAGS = RG_CHANNEL_FLAGS | BLUE_CHANNEL_FLAG,
            RGBA_CHANNEL_FLAGS = RGB_CHANNEL_FLAGS | ALPHA_CHANNEL_FLAG;

    @ApiStatus.Internal
    public float
            mR,
            mG,
            mB,
            mA;

    /**
     * Creates a new transparent <code>Color</code> instance in the sRGB color space.
     */
    public Color() {
    }

    /**
     * Creates a new <code>Color</code> instance from an ARGB color int.
     * The resulting color is in the sRGB color space.
     *
     * @param color the ARGB color int to create a <code>Color</code> from
     */
    public Color(@ColorInt int color) {
        mR = ((color >> 16) & 0xff) / 255.0f;
        mG = ((color >> 8) & 0xff) / 255.0f;
        mB = (color & 0xff) / 255.0f;
        mA = (color >>> 24) / 255.0f;
    }

    /**
     * Creates a new <code>Color</code> instance in the sRGB color space.
     *
     * @param r the value of the red channel, must be in [0..1] range
     * @param g the value of the green channel, must be in [0..1] range
     * @param b the value of the blue channel, must be in [0..1] range
     * @param a the value of the alpha channel, must be in [0..1] range
     */
    public Color(float r, float g, float b, float a) {
        mR = r;
        mG = g;
        mB = b;
        mA = a;
    }

    /**
     * Creates a new <code>Color</code> instance from an existing color instance.
     *
     * @param color an existing color instance to create a new color from
     */
    public Color(Color color) {
        mR = color.mR;
        mG = color.mG;
        mB = color.mB;
        mA = color.mA;
    }

    /**
     * Set this color values from an ARGB color int in the sRGB color space.
     *
     * @param color the ARGB color int to set this from
     */
    public void set(@ColorInt int color) {
        mR = ((color >> 16) & 0xff) / 255.0f;
        mG = ((color >> 8) & 0xff) / 255.0f;
        mB = (color & 0xff) / 255.0f;
        mA = (color >>> 24) / 255.0f;
    }

    /**
     * Set this color values in the sRGB color space.
     *
     * @param r the value of the red channel, must be in [0..1] range
     * @param g the value of the green channel, must be in [0..1] range
     * @param b the value of the blue channel, must be in [0..1] range
     * @param a the value of the alpha channel, must be in [0..1] range
     */
    public void set(float r, float g, float b, float a) {
        mR = r;
        mG = g;
        mB = b;
        mA = a;
    }

    /**
     * Set this color values from an existing color instance.
     *
     * @param color an existing color instance
     */
    public void set(Color color) {
        mR = color.mR;
        mG = color.mG;
        mB = color.mB;
        mA = color.mA;
    }

    /**
     * Converts this color to an ARGB color int. A color int is always in
     * the sRGB color space. This implies a color space conversion is applied if needed.
     *
     * @return an ARGB color in the sRGB color space
     */
    @ColorInt
    public int toArgb() {
        return ((int) (mA * 255.0f + 0.5f) << 24) |
                ((int) (mR * 255.0f + 0.5f) << 16) |
                ((int) (mG * 255.0f + 0.5f) << 8) |
                (int) (mB * 255.0f + 0.5f);
    }

    /**
     * Returns the value of the red component in the range \([0..1]\).
     *
     * @see #alpha()
     * @see #green()
     * @see #blue()
     */
    public float red() {
        return mR;
    }

    /**
     * Returns the value of the green component in the range \([0..1]\).
     *
     * @see #alpha()
     * @see #red()
     * @see #blue()
     */
    public float green() {
        return mG;
    }

    /**
     * Returns the value of the blue component in the range \([0..1]\).
     *
     * @see #alpha()
     * @see #red()
     * @see #green()
     */
    public float blue() {
        return mB;
    }

    /**
     * Returns the value of the alpha component in the range \([0..1]\).
     *
     * @see #red()
     * @see #green()
     * @see #blue()
     */
    public float alpha() {
        return mA;
    }

    /**
     * Sets the value of the red component in the range \([0..1]\).
     *
     * @see #alpha(float)
     * @see #green(float)
     * @see #blue(float)
     */
    public void red(float red) {
        mR = red;
    }

    /**
     * Sets the value of the green component in the range \([0..1]\).
     *
     * @see #alpha(float)
     * @see #red(float)
     * @see #blue(float)
     */
    public void green(float green) {
        mG = green;
    }

    /**
     * Sets the value of the blue component in the range \([0..1]\).
     *
     * @see #alpha(float)
     * @see #red(float)
     * @see #green(float)
     */
    public void blue(float blue) {
        mB = blue;
    }

    /**
     * Returns the value of the alpha component in the range \([0..1]\).
     *
     * @see #red(float)
     * @see #green(float)
     * @see #blue(float)
     */
    public void alpha(float alpha) {
        mA = alpha;
    }

    /**
     * Returns true if the color is an opaque color (i.e. alpha() == 1.0f).
     *
     * @return true if the color is opaque
     */
    public boolean isOpaque() {
        return mA == 1.0f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return Float.floatToIntBits(color.mR) == Float.floatToIntBits(mR) &&
                Float.floatToIntBits(color.mG) == Float.floatToIntBits(mG) &&
                Float.floatToIntBits(color.mB) == Float.floatToIntBits(mB) &&
                Float.floatToIntBits(color.mA) == Float.floatToIntBits(mA);
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(mR);
        result = 31 * result + Float.floatToIntBits(mG);
        result = 31 * result + Float.floatToIntBits(mB);
        result = 31 * result + Float.floatToIntBits(mA);
        return result;
    }

    /**
     * <p>Returns a string representation of the object. This method returns
     * a string equal to the value of:</p>
     *
     * <pre class="prettyprint">
     * "Color(" + r + ", " + g + ", " + b + ", " + a + ')'
     * </pre>
     *
     * <p>For instance, the string representation of opaque black in the sRGB
     * color space is equal to the following value:</p>
     *
     * <pre>
     * Color(0.0, 0.0, 0.0, 1.0)
     * </pre>
     *
     * @return A non-null string representation of the object
     */
    @Nonnull
    @Override
    public String toString() {
        return "Color(" + mR +
                ", " + mG +
                ", " + mB +
                ", " + mA +
                ')';
    }

    /**
     * </p>Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:</p>
     *
     * <ul>
     *   <li><code>#RRGGBB</code></li>
     *   <li><code>#AARRGGBB</code></li>
     *   <li><code>0xRRGGBB</code></li>
     *   <li><code>0xAARRGGBB</code></li>
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
                throw new IllegalArgumentException("Unknown color: " + colorString);
            }
            return color;
        } else if (colorString.startsWith("0x")) { // do not support upper case
            int color = Integer.parseUnsignedInt(colorString.substring(2), 16);
            if (colorString.length() == 8) {
                // Set the alpha value
                color |= 0xFF000000;
            } else if (colorString.length() != 10) {
                throw new IllegalArgumentException("Unknown color: " + colorString);
            }
            return color;
        }
        throw new IllegalArgumentException("Unknown color prefix: " + colorString);
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
    public static void rgbToHsv(int r, int g, int b, float[] hsv) {
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
    public static void rgbToHsv(int color, float[] hsv) {
        rgbToHsv(red(color), green(color), blue(color), hsv);
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
    public static int hsvToRgb(float h, float s, float v) {
        s = MathUtil.clamp(s, 0.0f, 1.0f);
        v = MathUtil.clamp(v, 0.0f, 1.0f);

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
    public static int hsvToRgb(float[] hsv) {
        return hsvToRgb(hsv[0], hsv[1], hsv[2]);
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
    //TODO move to other places
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
