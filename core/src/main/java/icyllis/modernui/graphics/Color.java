/*
 * Modern UI.
 * Copyright (C) 2022-2025 BloCamLimb. All rights reserved.
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
import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.Size;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

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
 * which is why color ints are called ARGB colors. Note that this packing format
 * is exactly mapped to {@link Bitmap.Format#BGRA_8888_PACK32}.</p>
 *
 * <h4>Usage in code</h4>
 * <p>To avoid confusing color ints with arbitrary integer values, it is a
 * good practice to annotate them with the <code>@ColorInt</code> annotation.</p>
 *
 * <h4>Encoding</h4>
 * <p>The four components of a color int are encoded in the following way:</p>
 * <pre class="prettyprint">
 * int color = (A &amp; 0xff) &lt;&lt; 24 | (R &amp; 0xff) &lt;&lt; 16 | (G &amp; 0xff) &lt;&lt; 8 | (B &amp; 0xff);
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
 * int A = (color >> 24) &amp; 0xff; // or color >>> 24
 * int R = (color >> 16) &amp; 0xff;
 * int G = (color >>  8) &amp; 0xff;
 * int B = (color      ) &amp; 0xff;
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

    private final float comp1;
    private final float comp2;
    private final float comp3;
    private final float comp4;
    private final float alpha;
    private final ColorSpace cs;

    private Color(float r, float g, float b, float a) {
        this(r, g, b, a, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    private Color(float r, float g, float b, float a, @NonNull ColorSpace colorSpace) {
        comp1 = r;
        comp2 = g;
        comp3 = b;
        comp4 = 0;
        alpha = a;
        cs = colorSpace;
    }

    private Color(float c1, float c2, float c3, float c4, float a, @NonNull ColorSpace colorSpace) {
        comp1 = c1;
        comp2 = c2;
        comp3 = c3;
        comp4 = c4;
        alpha = a;
        cs = colorSpace;
    }

    /**
     * Returns this color's color space.
     *
     * @return A non-null instance of {@link ColorSpace}
     */
    @NonNull
    public ColorSpace getColorSpace() {
        return cs;
    }

    /**
     * Returns the color model of this color.
     *
     * @return A non-null {@link ColorSpace.Model}
     */
    @NonNull
    public ColorSpace.Model getModel() {
        return cs.getModel();
    }

    /**
     * Indicates whether this color color is in a wide-gamut color space.
     * See {@link ColorSpace#isWideGamut()} for a definition of a wide-gamut
     * color space.
     *
     * @return True if this color is in a wide-gamut color space, false otherwise
     * @see #isSrgb()
     * @see ColorSpace#isWideGamut()
     */
    public boolean isWideGamut() {
        return cs.isWideGamut();
    }

    /**
     * Indicates whether this color is in the {@link ColorSpace.Named#SRGB sRGB}
     * color space.
     *
     * @return True if this color is in the sRGB color space, false otherwise
     * @see #isWideGamut()
     */
    public boolean isSrgb() {
        return cs.isSrgb();
    }

    /**
     * Returns the number of components that form a color value according
     * to this color space's color model, plus one extra component for
     * alpha.
     *
     * @return The integer 4 or 5
     */
    @IntRange(from = 4, to = 5)
    public int getComponentCount() {
        return cs.getComponentCount() + 1;
    }

    /**
     * Converts this color from its color space to the specified color space.
     * The conversion is done using the default rendering intent as specified
     * by {@link ColorSpace#connect(ColorSpace, ColorSpace)}.
     *
     * @param colorSpace The destination color space, cannot be null
     * @return A non-null color instance in the specified color space
     */
    @NonNull
    public Color convert(@NonNull ColorSpace colorSpace) {
        float[] color = new float[]{
                comp1, comp2, comp3, comp4
        };
        ColorSpace.connect(cs, colorSpace).transform(color);
        return new Color(color[0], color[1], color[2], color[3], alpha, colorSpace);
    }

    /**
     * Converts this color to an ARGB color int. A color int is always in
     * the {@link ColorSpace.Named#SRGB sRGB} color space. This implies
     * a color space conversion is applied if needed.
     *
     * @return An ARGB color in the sRGB color space
     */
    @ColorInt
    public int toArgb() {
        if (cs.isSrgb()) {
            return ((int) (alpha * 255.0f + 0.5f) << 24) |
                    ((int) (comp1 * 255.0f + 0.5f) << 16) |
                    ((int) (comp2 * 255.0f + 0.5f) << 8) |
                    (int) (comp3 * 255.0f + 0.5f);
        }

        float[] color = new float[]{
                comp1, comp2, comp3, comp4
        };
        // The transformation saturates the output
        ColorSpace.connect(cs).transform(color);

        return ((int) (color[3] * 255.0f + 0.5f) << 24) |
                ((int) (color[0] * 255.0f + 0.5f) << 16) |
                ((int) (color[1] * 255.0f + 0.5f) << 8) |
                (int) (color[2] * 255.0f + 0.5f);
    }

    /**
     * <p>Returns the value of the red component in the range defined by this
     * color's color space (see {@link ColorSpace#getMinValue(int)} and
     * {@link ColorSpace#getMaxValue(int)}).</p>
     *
     * <p>If this color's color model is not {@link ColorSpace.Model#RGB RGB},
     * calling this method is equivalent to <code>getComponent(0)</code>.</p>
     *
     * @see #alpha()
     * @see #red()
     * @see #green
     * @see #getComponents()
     */
    public float red() {
        return comp1;
    }

    /**
     * <p>Returns the value of the green component in the range defined by this
     * color's color space (see {@link ColorSpace#getMinValue(int)} and
     * {@link ColorSpace#getMaxValue(int)}).</p>
     *
     * <p>If this color's color model is not {@link ColorSpace.Model#RGB RGB},
     * calling this method is equivalent to <code>getComponent(1)</code>.</p>
     *
     * @see #alpha()
     * @see #red()
     * @see #green
     * @see #getComponents()
     */
    public float green() {
        return comp2;
    }

    /**
     * <p>Returns the value of the blue component in the range defined by this
     * color's color space (see {@link ColorSpace#getMinValue(int)} and
     * {@link ColorSpace#getMaxValue(int)}).</p>
     *
     * <p>If this color's color model is not {@link ColorSpace.Model#RGB RGB},
     * calling this method is equivalent to <code>getComponent(2)</code>.</p>
     *
     * @see #alpha()
     * @see #red()
     * @see #green
     * @see #getComponents()
     */
    public float blue() {
        return comp3;
    }

    /**
     * Returns the value of the alpha component in the range \([0..1]\).
     * Calling this method is equivalent to
     * <code>getComponent(getComponentCount() - 1)</code>.
     *
     * @see #red()
     * @see #green()
     * @see #blue()
     * @see #getComponents()
     * @see #getComponent(int)
     */
    public float alpha() {
        return alpha;
    }

    /**
     * Returns this color's components as a new array. The last element of the
     * array is always the alpha component.
     *
     * @return A new, non-null array whose size is equal to {@link #getComponentCount()}
     * @see #getComponent(int)
     */
    @NonNull
    @Size(min = 4, max = 5)
    public float[] getComponents() {
        if (cs.getComponentCount() == 4) {
            return new float[]{
                    comp1, comp2, comp3, comp4, alpha
            };
        } else {
            return new float[]{
                    comp1, comp2, comp3, alpha
            };
        }
    }

    /**
     * Copies this color's components in the supplied array. The last element of the
     * array is always the alpha component.
     *
     * @param components An array of floats whose size must be at least
     *                   {@link #getComponentCount()}, can be null
     * @return The array passed as a parameter if not null, or a new array of length
     * {@link #getComponentCount()}
     * @throws IllegalArgumentException If the specified array's length is less than
     *                                  {@link #getComponentCount()}
     * @see #getComponent(int)
     */
    @NonNull
    @Size(min = 4)
    public float[] getComponents(@Nullable @Size(min = 4) float[] components) {
        if (components == null) {
            return getComponents();
        }

        int length = getComponentCount();
        if (components.length < length) {
            throw new IllegalArgumentException("The specified array's length must be at "
                    + "least " + length);
        }

        components[0] = comp1;
        components[1] = comp2;
        components[2] = comp3;
        if (length == 5) {
            components[3] = comp4;
            components[4] = alpha;
        } else {
            assert length == 4;
            components[3] = alpha;
        }
        return components;
    }

    /**
     * <p>Returns the value of the specified component in the range defined by
     * this color's color space (see {@link ColorSpace#getMinValue(int)} and
     * {@link ColorSpace#getMaxValue(int)}).</p>
     *
     * <p>If the requested component index is {@link #getComponentCount()},
     * this method returns the alpha component, always in the range
     * \([0..1]\).</p>
     *
     * @throws ArrayIndexOutOfBoundsException If the specified component index
     *                                        is < 0 or >= {@link #getComponentCount()}
     * @see #getComponents()
     */
    public float getComponent(@IntRange(from = 0, to = 4) int component) {
        return switch (component) {
            case 0 -> comp1;
            case 1 -> comp2;
            case 2 -> comp3;
            case 3 -> cs.getComponentCount() == 4 ? comp4 : alpha;
            case 4 -> {
                if (cs.getComponentCount() == 4) {
                    yield alpha;
                } else {
                    throw new ArrayIndexOutOfBoundsException(component);
                }
            }
            default -> throw new ArrayIndexOutOfBoundsException(component);
        };
    }

    /**
     * <p>Returns the relative luminance of this color.</p>
     *
     * <p>Based on the formula for relative luminance defined in WCAG 2.0,
     * W3C Recommendation 11 December 2008.</p>
     *
     * @return A value between 0 (darkest black) and 1 (lightest white)
     * @throws IllegalArgumentException If this color's color space
     *                                  does not use the {@link ColorSpace.Model#RGB RGB} color model
     */
    public float luminance() {
        if (cs.getModel() != ColorSpace.Model.RGB) {
            throw new IllegalArgumentException("The specified color must be encoded in an RGB " +
                    "color space. The supplied color space is " + cs.getModel());
        }

        DoubleUnaryOperator eotf = ((ColorSpace.Rgb) cs).getEotf();
        double r = eotf.applyAsDouble(comp1);
        double g = eotf.applyAsDouble(comp2);
        double b = eotf.applyAsDouble(comp3);

        return MathUtil.pin((float) ((0.2126 * r) + (0.7152 * g) + (0.0722 * b)), 0f, 1f);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Color color = (Color) o;
        return Float.floatToIntBits(comp1) == Float.floatToIntBits(color.comp1) &&
                Float.floatToIntBits(comp2) == Float.floatToIntBits(color.comp2) &&
                Float.floatToIntBits(comp3) == Float.floatToIntBits(color.comp3) &&
                Float.floatToIntBits(comp4) == Float.floatToIntBits(color.comp4) &&
                Float.floatToIntBits(alpha) == Float.floatToIntBits(color.alpha) &&
                cs.equals(color.cs);
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(comp1);
        result = 31 * result + Float.hashCode(comp2);
        result = 31 * result + Float.hashCode(comp3);
        result = 31 * result + Float.hashCode(comp4);
        result = 31 * result + Float.hashCode(alpha);
        result = 31 * result + cs.hashCode();
        return result;
    }

    /**
     * <p>Returns a string representation of the object. This method returns
     * a string equal to the value of:</p>
     *
     * <pre class="prettyprint">
     * "Color(" + r + ", " + g + ", " + b + ", " + a +
     *         ", " + getColorSpace().getName + ')'
     * </pre>
     *
     * <p>For instance, the string representation of opaque black in the sRGB
     * color space is equal to the following value:</p>
     *
     * <pre>
     * Color(0.0, 0.0, 0.0, 1.0, sRGB IEC61966-2.1)
     * </pre>
     *
     * @return A non-null string representation of the object
     */
    @Override
    @NonNull
    public String toString() {
        StringBuilder b = new StringBuilder("Color(");
        int length = getComponentCount();
        for (int i = 0; i < length; i++) {
            b.append(getComponent(i)).append(", ");
        }
        b.append(cs.getName());
        b.append(')');
        return b.toString();
    }

    /**
     * Creates a new <code>Color</code> instance from an ARGB color int.
     * The resulting color is in the {@link ColorSpace.Named#SRGB sRGB}
     * color space.
     *
     * @param color The ARGB color int to create a <code>Color</code> from
     * @return A non-null instance of {@link Color}
     */
    @NonNull
    public static Color valueOf(@ColorInt int color) {
        float r = ((color >> 16) & 0xff) / 255.0f;
        float g = ((color >>  8) & 0xff) / 255.0f;
        float b = ((color      ) & 0xff) / 255.0f;
        float a = ((color >> 24) & 0xff) / 255.0f;
        return new Color(r, g, b, a);
    }

    /**
     * Creates a new opaque <code>Color</code> in the {@link ColorSpace.Named#SRGB sRGB}
     * color space with the specified red, green and blue component values. The component
     * values must be in the range \([0..1]\).
     *
     * @param r The red component of the opaque sRGB color to create, in \([0..1]\)
     * @param g The green component of the opaque sRGB color to create, in \([0..1]\)
     * @param b The blue component of the opaque sRGB color to create, in \([0..1]\)
     * @return A non-null instance of {@link Color}
     */
    @NonNull
    public static Color valueOf(float r, float g, float b) {
        return new Color(r, g, b, 1.0f);
    }

    /**
     * Creates a new <code>Color</code> in the {@link ColorSpace.Named#SRGB sRGB}
     * color space with the specified red, green, blue and alpha component values.
     * The component values must be in the range \([0..1]\).
     *
     * @param r The red component of the sRGB color to create, in \([0..1]\)
     * @param g The green component of the sRGB color to create, in \([0..1]\)
     * @param b The blue component of the sRGB color to create, in \([0..1]\)
     * @param a The alpha component of the sRGB color to create, in \([0..1]\)
     * @return A non-null instance of {@link Color}
     */
    @NonNull
    public static Color valueOf(float r, float g, float b, float a) {
        return new Color(
                MathUtil.pin(r, 0.0f, 1.0f),
                MathUtil.pin(g, 0.0f, 1.0f),
                MathUtil.pin(b, 0.0f, 1.0f),
                MathUtil.pin(a, 0.0f, 1.0f)
        );
    }

    /**
     * Creates a new <code>Color</code> in the specified color space with the
     * specified red, green, blue and alpha component values. The range of the
     * components is defined by {@link ColorSpace#getMinValue(int)} and
     * {@link ColorSpace#getMaxValue(int)}. The values passed to this method
     * must be in the proper range.
     *
     * @param r The red component of the color to create
     * @param g The green component of the color to create
     * @param b The blue component of the color to create
     * @param a The alpha component of the color to create, in \([0..1]\)
     * @param colorSpace The color space of the color to create
     * @return A non-null instance of {@link Color}
     *
     * @throws IllegalArgumentException If the specified color space uses a
     * color model with more than 3 components
     */
    @NonNull
    public static Color valueOf(float r, float g, float b, float a, @NonNull ColorSpace colorSpace) {
        if (colorSpace.getComponentCount() > 3) {
            throw new IllegalArgumentException("The specified color space must use a color model " +
                    "with at most 3 color components");
        }
        return new Color(r, g, b, a, colorSpace);
    }

    /**
     * <p>Creates a new <code>Color</code> in the specified color space with the
     * specified component values. The range of the components is defined by
     * {@link ColorSpace#getMinValue(int)} and {@link ColorSpace#getMaxValue(int)}.
     * The values passed to this method must be in the proper range. The alpha
     * component is always in the range \([0..1]\).</p>
     *
     * <p>The length of the array of components must be at least
     * <code>{@link ColorSpace#getComponentCount()} + 1</code>. The component at index
     * {@link ColorSpace#getComponentCount()} is always alpha.</p>
     *
     * @param components The components of the color to create, with alpha as the last component
     * @param colorSpace The color space of the color to create
     * @return A non-null instance of {@link Color}
     *
     * @throws IllegalArgumentException If the array of components is smaller than
     * required by the color space
     */
    @NonNull
    public static Color valueOf(@NonNull @Size(min = 4, max = 5) float[] components,
                                @NonNull ColorSpace colorSpace) {
        int length = colorSpace.getComponentCount() + 1;
        if (components.length < length) {
            throw new IllegalArgumentException("Received a component array of length " +
                    components.length + " but the color model requires " +
                    length + " (including alpha)");
        }
        if (length == 5) {
            return new Color(components[0], components[1], components[2], components[3], components[4], colorSpace);
        } else {
            assert length == 4;
            return new Color(components[0], components[1], components[2], components[3], colorSpace);
        }
    }

    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    @IntRange(from = 0, to = 255)
    public static int alpha(@ColorInt int color) {
        return color >>> 24;
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    @IntRange(from = 0, to = 255)
    public static int red(@ColorInt int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    @IntRange(from = 0, to = 255)
    public static int green(@ColorInt int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    @IntRange(from = 0, to = 255)
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
    public static int rgb(@IntRange(from = 0, to = 255) int red,
                          @IntRange(from = 0, to = 255) int green,
                          @IntRange(from = 0, to = 255) int blue) {
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
    public static int argb(@IntRange(from = 0, to = 255) int alpha,
                           @IntRange(from = 0, to = 255) int red,
                           @IntRange(from = 0, to = 255) int green,
                           @IntRange(from = 0, to = 255) int blue) {
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
     * If the string cannot be parsed, throws an {@link IllegalArgumentException}
     * exception. Supported formats are:</p>
     *
     * <ul>
     *   <li><code>#RRGGBB</code></li>
     *   <li><code>#AARRGGBB</code></li>
     *   <li><code>0xRRGGBB</code></li>
     *   <li><code>0xAARRGGBB</code></li>
     * </ul>
     *
     * <p>The following names are also accepted: <code>red</code>, <code>blue</code>,
     * <code>green</code>, <code>black</code>, <code>white</code>, <code>gray</code>,
     * <code>cyan</code>, <code>magenta</code>, <code>yellow</code>, <code>lightgray</code>,
     * <code>darkgray</code>, <code>grey</code>, <code>lightgrey</code>, <code>darkgrey</code>,
     * <code>aqua</code>, <code>fuchsia</code>, <code>lime</code>, <code>maroon</code>,
     * <code>navy</code>, <code>olive</code>, <code>purple</code>, <code>silver</code>,
     * and <code>teal</code>.</p>
     */
    @ColorInt
    public static int parseColor(@NonNull String colorString) {
        final int index;
        if (colorString.startsWith("#")) {
            index = 1;
        } else if (colorString.startsWith("0x") || colorString.startsWith("0X")) {
            index = 2;
        } else {
            return switch (colorString.toLowerCase(Locale.ROOT)) {
                case "black" -> BLACK;
                case "darkgray", "darkgrey" -> DKGRAY;
                case "gray", "grey" -> GRAY;
                case "lightgray", "lightgrey" -> LTGRAY;
                case "white" -> WHITE;
                case "red" -> RED;
                case "green" -> GREEN;
                case "blue" -> BLUE;
                case "yellow" -> YELLOW;
                case "cyan" -> CYAN;
                case "magenta" -> MAGENTA;
                case "aqua" -> 0xFF00FFFF;
                case "fuchsia" -> 0xFFFF00FF;
                case "lime" -> 0xFF00FF00;
                case "maroon" -> 0xFF800000;
                case "navy" -> 0xFF000080;
                case "olive" -> 0xFF808000;
                case "purple" -> 0xFF800080;
                case "silver" -> 0xFFC0C0C0;
                case "teal" -> 0xFF008080;
                default -> throw new IllegalArgumentException("Unknown color: " + colorString);
            };
        }
        int length = colorString.length() - index;
        if (length != 6 && length != 8) {
            throw new IllegalArgumentException("Unknown color: " + colorString);
        }
        // Java 9
        int color = Integer.parseUnsignedInt(colorString, index, index + length, 16);
        if (length == 6) {
            // Set the alpha value
            color |= 0xFF000000;
        }
        return color;
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
    public static void RGBToHSV(@IntRange(from = 0, to = 255) int r,
                                @IntRange(from = 0, to = 255) int g,
                                @IntRange(from = 0, to = 255) int b,
                                @NonNull @Size(3) float[] hsv) {
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
    public static void RGBToHSV(@ColorInt int color, @NonNull @Size(3) float[] hsv) {
        RGBToHSV((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
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
    @ColorInt
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
    @ColorInt
    public static int HSVToColor(@NonNull @Size(3) float[] hsv) {
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
    public static void GammaToLinear(@NonNull @Size(min=3) float[] col) {
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
    public static void LinearToGamma(@NonNull @Size(min=3) float[] col) {
        col[0] = LinearToGamma(col[0]);
        col[1] = LinearToGamma(col[1]);
        col[2] = LinearToGamma(col[2]);
    }

    /**
     * Returns the relative luminance of a color.
     * <p>
     * Assumes sRGB encoding. Based on the formula for relative luminance
     * defined in WCAG 2.0, W3C Recommendation 11 December 2008.
     *
     * @return a value between 0 (darkest black) and 1 (lightest white)
     */
    public static float luminance(@ColorInt int color) {
        float r = GammaToLinear(red(color) / 255F);
        float g = GammaToLinear(green(color) / 255F);
        float b = GammaToLinear(blue(color) / 255F);

        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
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
    public static float luminance(@NonNull @Size(min=3) float[] col) {
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
    @ApiStatus.Internal
    @ColorInt
    public static int blend(@NonNull BlendMode mode, @ColorInt int src, @ColorInt int dst) {
        return mode.getNativeBlendMode().blend(src, dst);
    }
}
