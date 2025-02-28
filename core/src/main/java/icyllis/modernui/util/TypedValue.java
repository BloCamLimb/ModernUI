/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import icyllis.modernui.resources.Resources;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container for a dynamically typed data value.
 * <br>Primarily used with {@link Resources}
 * for holding resource values.
 */
@SuppressWarnings("MagicConstant")
public class TypedValue {

    /**
     * The value contains no data.
     */
    public static final int TYPE_NULL = 0x00;

    /**
     * The <var>data</var> field holds a resource identifier.
     */
    public static final int TYPE_REFERENCE = 0x01;
    /**
     * The <var>data</var> field holds an attribute resource
     * identifier (referencing an attribute in the current theme
     * style, not a resource entry).
     */
    public static final int TYPE_ATTRIBUTE = 0x02;
    /**
     * The <var>string</var> field holds string data.  In addition, if
     * <var>data</var> is non-zero then it is the string block
     * index of the string and <var>assetCookie</var> is the set of
     * assets the string came from.
     */
    public static final int TYPE_STRING = 0x03;
    /**
     * The <var>data</var> field holds an IEEE 754 floating-point number.
     */
    public static final int TYPE_FLOAT = 0x04;
    /**
     * The <var>data</var> field holds a complex number encoding a dimension value.
     */
    public static final int TYPE_DIMENSION = 0x05;
    /**
     * The <var>data</var> field holds a complex number encoding a fraction of a container.
     */
    public static final int TYPE_FRACTION = 0x06;
    /**
     * The <var>data</var> field holds an index to runtime-specified supplier table.
     */
    public static final int TYPE_SUPPLIER = 0x0F;

    /**
     * Identifies the start of plain integer values.  Any type value
     * from this to {@link #TYPE_LAST_INT} means the
     * <var>data</var> field holds a generic integer value.
     */
    public static final int TYPE_FIRST_INT = 0x10;

    /**
     * The <var>data</var> field holds a number that was
     * originally specified in decimal.
     */
    public static final int TYPE_INT_DEC = 0x10;
    /**
     * The <var>data</var> field holds a number that was
     * originally specified in hexadecimal (0xn).
     */
    public static final int TYPE_INT_HEX = 0x11;
    /**
     * The <var>data</var> field holds 0 or 1 that was originally
     * specified as "false" or "true".
     */
    public static final int TYPE_INT_BOOLEAN = 0x12;

    /**
     * Identifies the start of integer values that were specified as
     * color constants (starting with '#').
     */
    public static final int TYPE_FIRST_COLOR_INT = 0x1c;

    /**
     * The <var>data</var> field holds a color that was originally
     * specified as #aarrggbb.
     */
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
    /**
     * The <var>data</var> field holds a color that was originally
     * specified as #rrggbb.
     */
    public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
    /**
     * The <var>data</var> field holds a color that was originally
     * specified as #argb.
     */
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
    /**
     * The <var>data</var> field holds a color that was originally
     * specified as #rgb.
     */
    public static final int TYPE_INT_COLOR_RGB4 = 0x1f;

    /**
     * Identifies the end of integer values that were specified as color
     * constants.
     */
    public static final int TYPE_LAST_COLOR_INT = 0x1f;

    /**
     * Identifies the end of plain integer values.
     */
    public static final int TYPE_LAST_INT = 0x1f;

    /**
     * Type: mask to extract single type information.
     * If masked type is {@link #TYPE_REFERENCE}, then the next 8 bits
     * hold the type index of the resource it refers to. The type string
     * can be found in type string table indexed by {@link #assetCookie}.
     */
    public static final int TYPE_MASK = 0xff;

    /**
     * Complex data: bit shift of unit information.
     */
    public static final int COMPLEX_UNIT_SHIFT = 0;
    /**
     * Complex data: mask to extract unit information (after shifting by
     * {@link #COMPLEX_UNIT_SHIFT}). This gives us 16 possible types, as
     * defined below.
     */
    public static final int COMPLEX_UNIT_MASK = 0xf;

    @ApiStatus.Internal
    @MagicConstant(intValues = {
            COMPLEX_UNIT_PX,
            COMPLEX_UNIT_DP,
            COMPLEX_UNIT_SP,
            COMPLEX_UNIT_PT,
            COMPLEX_UNIT_IN,
            COMPLEX_UNIT_MM,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ComplexDimensionUnit {
    }

    /**
     * {@link #TYPE_DIMENSION} complex unit: Value is raw pixels.
     */
    public static final int COMPLEX_UNIT_PX = 0;
    /**
     * {@link #TYPE_DIMENSION} complex unit: Value is device-independent pixels.
     */
    public static final int COMPLEX_UNIT_DP = 1;
    /**
     * {@link #TYPE_DIMENSION} complex unit: Value is scale-independent pixels.
     */
    public static final int COMPLEX_UNIT_SP = 2;
    /**
     * {@link #TYPE_DIMENSION} complex unit: Value is in points.
     */
    public static final int COMPLEX_UNIT_PT = 3;
    /**
     * {@link #TYPE_DIMENSION} complex unit: Value is in inches.
     */
    public static final int COMPLEX_UNIT_IN = 4;
    /**
     * {@link #TYPE_DIMENSION} complex unit: Value is in millimeters.
     */
    public static final int COMPLEX_UNIT_MM = 5;

    /**
     * {@link #TYPE_FRACTION} complex unit: A fraction of the view size.
     */
    public static final int COMPLEX_UNIT_FRACTION = 0;
    /**
     * {@link #TYPE_FRACTION} complex unit: A fraction of the parent size.
     */
    public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;

    /**
     * Complex data: where the radix information is, telling where the decimal
     * place appears in the mantissa.
     */
    public static final int COMPLEX_RADIX_SHIFT = 4;
    /**
     * Complex data: mask to extract radix information (after shifting by
     * {@link #COMPLEX_RADIX_SHIFT}). This give us 4 possible fixed point
     * representations as defined below.
     */
    public static final int COMPLEX_RADIX_MASK = 0x3;

    /**
     * Complex data: the mantissa is an integral number -- i.e., 0xnnnnnn.0
     */
    public static final int COMPLEX_RADIX_23p0 = 0;
    /**
     * Complex data: the mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
     */
    public static final int COMPLEX_RADIX_16p7 = 1;
    /**
     * Complex data: the mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
     */
    public static final int COMPLEX_RADIX_8p15 = 2;
    /**
     * Complex data: the mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
     */
    public static final int COMPLEX_RADIX_0p23 = 3;

    /**
     * Complex data: bit shift of mantissa information.
     */
    public static final int COMPLEX_MANTISSA_SHIFT = 8;
    /**
     * Complex data: mask to extract mantissa information (after shifting by
     * {@link #COMPLEX_MANTISSA_SHIFT}). This gives us 23 bits of precision;
     * the top bit is the sign.
     */
    public static final int COMPLEX_MANTISSA_MASK = 0xffffff;

    /**
     * The type held by this value, as defined by the constants here.
     * This tells you how to interpret the other fields in the object.
     * <p>
     * 0..7 bits: one of type constants listed above.<br>
     * 8..15 bits: type index of the referent resource, if value type is REFERENCE.
     */
    public int type;

    /**
     * If the value holds a string, this is it.
     */
    public CharSequence string;

    /**
     * Basic data in the value, interpreted according to {@link #type}
     */
    public int data;

    /**
     * Additional information about where the value came from; only
     * set for strings.
     */
    public int assetCookie;

    public int changingConfigurations = -1;

    /**
     * Return the data for this value as a float.  Only use for values
     * whose type is {@link #TYPE_FLOAT}.
     */
    public final float getFloat() {
        return Float.intBitsToFloat(data);
    }

    private static final float MANTISSA_MULT =
            1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
    private static final float[] RADIX_MULTS = {
            MANTISSA_MULT,
            MANTISSA_MULT / (1 <<  7),
            MANTISSA_MULT / (1 << 15),
            MANTISSA_MULT / (1 << 23)
    };

    /**
     * Determine if a value is a color.
     * <p>
     * This works by comparing {@link #type} to {@link #TYPE_FIRST_COLOR_INT}
     * and {@link #TYPE_LAST_COLOR_INT}.
     *
     * @return true if this value is a color
     */
    public boolean isColorType() {
        return (type >= TYPE_FIRST_COLOR_INT && type <= TYPE_LAST_COLOR_INT);
    }

    /**
     * Determine if a value is a reference.
     */
    public boolean isReferenceType() {
        return (type & TYPE_MASK) == TYPE_REFERENCE;
    }

    /**
     * Retrieve the base value from a complex data integer.
     * <br>This uses the {@link #COMPLEX_MANTISSA_MASK} and {@link #COMPLEX_RADIX_MASK}
     * fields of the data to compute a floating point representation of the number they
     * describe.
     * <br>The units are ignored.
     *
     * @param complex A complex data value.
     * @return A floating point value corresponding to the complex data.
     */
    public static float complexToFloat(int complex) {
        return (complex & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
                * RADIX_MULTS[(complex >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK];
    }

    /**
     * Converts a complex data value holding a dimension to its final floating
     * point value. The given <var>data</var> must be structured as a
     * {@link #TYPE_DIMENSION}.
     *
     * @param data    A complex data value holding a unit, magnitude, and
     *                mantissa.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     * @return The complex floating point value multiplied by the appropriate
     * metrics depending on its unit.
     */
    public static float complexToDimension(int data, DisplayMetrics metrics) {
        return applyDimension(
                (data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK,
                complexToFloat(data),
                metrics);
    }

    /**
     * Converts a complex data value holding a dimension to its final value
     * as an integer pixel offset.
     * <br>This is the same as {@link #complexToDimension}, except the raw
     * floating point value is truncated to an integer (pixel) value.
     * The given <var>data</var> must be structured as a {@link #TYPE_DIMENSION}.
     *
     * @param data    A complex data value holding a unit, magnitude, and
     *                mantissa.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     * @return The number of pixels specified by the data and its desired
     * multiplier and units.
     */
    public static int complexToDimensionPixelOffset(int data,
                                                    DisplayMetrics metrics) {
        return (int) applyDimension(
                (data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK,
                complexToFloat(data),
                metrics);
    }

    /**
     * Converts a complex data value holding a dimension to its final value
     * as an integer pixel size.
     * <br>This is the same as {@link #complexToDimension}, except the raw
     * floating point value is converted to an integer (pixel) value for use
     * as a size.
     * <br>A size conversion involves rounding the base value, and ensuring
     * that a non-zero base value is at least one pixel in size.
     *
     * <p>The given <var>data</var> must be structured as a
     * {@link #TYPE_DIMENSION}.
     *
     * @param data    A complex data value holding a unit, magnitude, and
     *                mantissa.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     * @return The number of pixels specified by the data and its desired
     * multiplier and units.
     */
    public static int complexToDimensionPixelSize(int data,
                                                  DisplayMetrics metrics) {
        final float value = complexToFloat(data);
        final float f = applyDimension(
                (data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK,
                value,
                metrics);
        final int res = (int) (f >= 0 ? f + 0.5f : f - 0.5f);
        if (res != 0) return res;
        if (value == 0) return 0;
        if (value > 0) return 1;
        return -1;
    }

    /**
     * Return the complex unit type for the given complex dimension. For example, a dimen type
     * with value 12sp will return {@link #COMPLEX_UNIT_SP}. Use with values created with {@link
     * #createComplexDimension(int, int)} etc.
     *
     * @return The complex unit type.
     */
    public static int getUnitFromComplexDimension(int complexDimension) {
        return COMPLEX_UNIT_MASK & (complexDimension >> TypedValue.COMPLEX_UNIT_SHIFT);
    }

    /**
     * Converts an unpacked complex data value holding a dimension to its final
     * floating-point value. The two parameters <var>unit</var> and <var>value</var>
     * are as in {@link #TYPE_DIMENSION}.
     *
     * @param unit    The unit to convert from.
     * @param value   The value to apply the unit to.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     * @return The complex floating point value multiplied by the appropriate
     * metrics depending on its unit.
     */
    public static float applyDimension(@ComplexDimensionUnit int unit, float value,
                                       DisplayMetrics metrics) {
        return switch (unit) {
            case COMPLEX_UNIT_PX -> value;
            case COMPLEX_UNIT_DP -> value * metrics.density;
            case COMPLEX_UNIT_SP -> value * metrics.scaledDensity;
            case COMPLEX_UNIT_PT -> value * metrics.xdpi * (1.0f / 72);
            case COMPLEX_UNIT_IN -> value * metrics.xdpi;
            case COMPLEX_UNIT_MM -> value * metrics.xdpi * (1.0f / 25.4f);
            default -> 0;
        };
    }

    /**
     * Convert a base value to a complex data integer.
     * <br>This sets the {@link #COMPLEX_MANTISSA_MASK} and {@link #COMPLEX_RADIX_MASK}
     * fields of the data to create a floating point representation of the given value.
     * The units are not set.
     *
     * <p>This is the inverse of {@link #complexToFloat(int)}.
     *
     * @param value An integer value.
     * @return A complex data integer representing the value.
     */
    @ApiStatus.Internal
    public static int intToComplex(int value) {
        if (value < -0x800000 || value >= 0x800000) {
            throw new IllegalArgumentException("Magnitude of the value is too large: " + value);
        }
        return (COMPLEX_RADIX_23p0 << COMPLEX_RADIX_SHIFT) | (value << COMPLEX_MANTISSA_SHIFT);
    }

    /**
     * Convert a base value to a complex data integer.
     * <br>This sets the {@link #COMPLEX_MANTISSA_MASK} and {@link #COMPLEX_RADIX_MASK}
     * fields of the data to create a floating point representation of the given value.
     * The units are not set.
     *
     * <p>This is the inverse of {@link #complexToFloat(int)}.
     *
     * @param value A floating point value.
     * @return A complex data integer representing the value.
     */
    @ApiStatus.Internal
    public static int floatToComplex(float value) {
        if (value < (float) -0x800000 - .5f || value >= (float) 0x800000 - .5f) {
            throw new IllegalArgumentException("Magnitude of the value is too large: " + value);
        }
        int bits = Float.floatToRawIntBits(value) + (23 << 23);
        long mag = (long) (Float.intBitsToFloat(bits & 0x7FFFFFFF) + .5f);
        final int radix, shift;
        if ((mag & 0x7FFFFF) == 0) {
            radix = COMPLEX_RADIX_23p0;
            shift = 23;
        } else if ((mag & 0x7FFFFFFF_FF800000L) == 0) {
            radix = COMPLEX_RADIX_0p23;
            shift = 0;
        } else if ((mag & 0x7FFFFFFF_80000000L) == 0) {
            radix = COMPLEX_RADIX_8p15;
            shift = 8;
        } else if ((mag & 0x7FFFFF80_00000000L) == 0) {
            radix = COMPLEX_RADIX_16p7;
            shift = 16;
        } else {
            radix = COMPLEX_RADIX_23p0;
            shift = 23;
        }
        int mantissa = (int) (mag >>> shift) & COMPLEX_MANTISSA_MASK;
        if ((bits & 0x80000000) != 0) mantissa = -mantissa;
        assert (mantissa >= -0x800000 && mantissa < 0x800000);
        return (radix << COMPLEX_RADIX_SHIFT) | (mantissa << COMPLEX_MANTISSA_SHIFT);
    }

    /**
     * <p>Creates a complex data integer that stores a dimension value and units.
     *
     * <p>The resulting value can be passed to e.g.
     * {@link #complexToDimensionPixelOffset(int, DisplayMetrics)} to calculate the pixel
     * value for the dimension.
     *
     * @param value the value of the dimension
     * @param units the units of the dimension, e.g. {@link #COMPLEX_UNIT_DP}
     * @return A complex data integer representing the value and units of the dimension.
     */
    @ApiStatus.Internal
    public static int createComplexDimension(int value, int units) {
        if (units < TypedValue.COMPLEX_UNIT_PX || units > TypedValue.COMPLEX_UNIT_MM) {
            throw new IllegalArgumentException("Must be a valid COMPLEX_UNIT_*: " + units);
        }
        return intToComplex(value) | units;
    }

    /**
     * <p>Creates a complex data integer that stores a dimension value and units.
     *
     * <p>The resulting value can be passed to e.g.
     * {@link #complexToDimensionPixelOffset(int, DisplayMetrics)} to calculate the pixel
     * value for the dimension.
     *
     * @param value the value of the dimension
     * @param units the units of the dimension, e.g. {@link #COMPLEX_UNIT_DP}
     * @return A complex data integer representing the value and units of the dimension.
     */
    @ApiStatus.Internal
    public static int createComplexDimension(float value, int units) {
        if (units < TypedValue.COMPLEX_UNIT_PX || units > TypedValue.COMPLEX_UNIT_MM) {
            throw new IllegalArgumentException("Must be a valid COMPLEX_UNIT_*: " + units);
        }
        return floatToComplex(value) | units;
    }

    /**
     * Converts a complex data value holding a fraction to its final floating
     * point value. The given <var>data</var> must be structured as a
     * {@link #TYPE_FRACTION}.
     *
     * @param data  A complex data value holding a unit, magnitude, and
     *              mantissa.
     * @param base  The base value of this fraction.  In other words, a
     *              standard fraction is multiplied by this value.
     * @param pbase The parent base value of this fraction.  In other
     *              words, a parent fraction (nn%p) is multiplied by this
     *              value.
     * @return The complex floating point value multiplied by the appropriate
     * base value depending on its unit.
     */
    public static float complexToFraction(int data, float base, float pbase) {
        return switch ((data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK) {
            case COMPLEX_UNIT_FRACTION -> complexToFloat(data) * base;
            case COMPLEX_UNIT_FRACTION_PARENT -> complexToFloat(data) * pbase;
            default -> 0;
        };
    }
}
