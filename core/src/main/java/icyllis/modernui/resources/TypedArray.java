/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;

public class TypedArray {

    static TypedArray obtain(Resources res, int len) {
        TypedArray attrs = res.mTypedArrayPool.acquire();
        if (attrs == null) {
            attrs = new TypedArray(res);
        }

        attrs.mRecycled = false;
        attrs.mMetrics = res.getDisplayMetrics();
        attrs.resize(len);
        return attrs;
    }

    static final int STYLE_NUM_ENTRIES = 4;
    static final int STYLE_TYPE = 0;
    static final int STYLE_DATA = 1;
    static final int STYLE_COOKIE = 2;
    static final int STYLE_FLAGS = 3;

    private final Resources mResources;
    private DisplayMetrics mMetrics;

    private boolean mRecycled;

    Resources.Theme mTheme;

    int[] mData;
    int[] mIndices;
    int mLength;
    final TypedValue mValue = new TypedValue();

    final BagAttributeFinder mDefStyleAttrFinder = new BagAttributeFinder();

    private void resize(int len) {
        mLength = len;
        final int dataLen = len * STYLE_NUM_ENTRIES;
        final int indicesLen = len + 1;
        if (mData == null || mData.length < dataLen) {
            mData = new int[dataLen];
            mIndices = new int[indicesLen];
        }
    }


    /**
     * Returns the number of values in this array.
     *
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public int length() {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        return mLength;
    }

    /**
     * Returns the number of indices in the array that actually have data. Attributes with a value
     * of @empty are included, as this is an explicit indicator.
     *
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public int getIndexCount() {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        return mIndices[0];
    }

    /**
     * Returns an index in the array that has data. Attributes with a value of @empty are included,
     * as this is an explicit indicator.
     *
     * @param at The index you would like to returned, ranging from 0 to
     *           {@link #getIndexCount()}.
     *
     * @return The index at the given offset, which can be used with
     *         {@link #getValue} and related APIs.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public int getIndex(int at) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        return mIndices[1+at];
    }

    /**
     * Returns the Resources object this array was loaded from.
     *
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public Resources getResources() {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        return mResources;
    }

    /**
     * Retrieve the boolean value for the attribute at <var>index</var>.
     * <p>
     * If the attribute is an integer value, this method returns false if the
     * attribute is equal to zero, and true otherwise.
     * If the attribute is not a boolean or integer value,
     * this method will attempt to coerce it to an integer using
     * {@link Integer#decode(String)} and return whether it is equal to zero.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 cannot be coerced to an integer.
     *
     * @return Boolean value of the attribute, or defValue if the attribute was
     *         not defined or could not be coerced to an integer.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public boolean getBoolean(@StyleableRes int index, boolean defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        index *= STYLE_NUM_ENTRIES;
        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return data[index + STYLE_DATA] != 0;
        }

        throw new RuntimeException("getBoolean of bad type: 0x" + Integer.toHexString(type));
    }

    /**
     * Retrieve the integer value for the attribute at <var>index</var>.
     * <p>
     * If the attribute is not an integer, this method will attempt to coerce
     * it to an integer using {@link Integer#decode(String)}.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 cannot be coerced to an integer.
     *
     * @return Integer value of the attribute, or defValue if the attribute was
     *         not defined or could not be coerced to an integer.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public int getInt(@StyleableRes int index, int defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        index *= STYLE_NUM_ENTRIES;
        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return data[index + STYLE_DATA];
        }

        throw new RuntimeException("getInt of bad type: 0x" + Integer.toHexString(type));
    }

    /**
     * Retrieve the float value for the attribute at <var>index</var>.
     * <p>
     * If the attribute is not a float or an integer, this method will attempt
     * to coerce it to a float using {@link Float#parseFloat(String)}.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return Attribute float value, or defValue if the attribute was
     *         not defined or could not be coerced to a float.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public float getFloat(@StyleableRes int index, float defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        index *= STYLE_NUM_ENTRIES;
        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type == TypedValue.TYPE_FLOAT) {
            return Float.intBitsToFloat(data[index + STYLE_DATA]);
        } else if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return data[index + STYLE_DATA];
        }

        throw new RuntimeException("getFloat of bad type: 0x" + Integer.toHexString(type));
    }

    /**
     * Retrieve the color value for the attribute at <var>index</var>.  If
     * the attribute references a color resource holding a complex
     * {@link ColorStateList}, then the default color from
     * the set is returned.
     * <p>
     * This method will throw an exception if the attribute is defined but is
     * not an integer color or color state list.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute color value, or defValue if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not an integer color or color state list.
     */
    @ColorInt
    public int getColor(@StyleableRes int index, @ColorInt int defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final int attrIndex = index;
        index *= STYLE_NUM_ENTRIES;

        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return data[index + STYLE_DATA];
        } else if (type == TypedValue.TYPE_STRING) {
            final TypedValue value = mValue;
            if (getValueAt(index, value)) {
                final ColorStateList csl = mResources.loadColorStateList(
                        value, null, mTheme);
                return csl.getDefaultColor();
            }
            return defValue;
        } else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to color: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
    }

    /**
     * Retrieve the ColorStateList for the attribute at <var>index</var>.
     * The value may be either a single solid color or a reference to
     * a color or complex {@link ColorStateList}
     * description.
     * <p>
     * This method will return {@code null} if the attribute is not defined or
     * is not an integer color or color state list.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return ColorStateList for the attribute, or {@code null} if not
     *         defined.
     * @throws RuntimeException if the attribute if the TypedArray has already
     *         been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not an integer color or color state list.
     */
    @Nullable
    public ColorStateList getColorStateList(@StyleableRes int index) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final TypedValue value = mValue;
        if (getValueAt(index * STYLE_NUM_ENTRIES, value)) {
            if (value.type == TypedValue.TYPE_ATTRIBUTE) {
                throw new UnsupportedOperationException(
                        "Failed to resolve attribute at index " + index + ": " + value
                                + ", theme=" + mTheme);
            }
            return mResources.loadColorStateList(value, null, mTheme);
        }
        return null;
    }

    /**
     * Retrieve the integer value for the attribute at <var>index</var>.
     * <p>
     * Unlike {@link #getInt(int, int)}, this method will throw an exception if
     * the attribute is defined but is not an integer.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute integer value, or defValue if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not an integer.
     */
    public int getInteger(@StyleableRes int index, int defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final int attrIndex = index;
        index *= STYLE_NUM_ENTRIES;

        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return data[index + STYLE_DATA];
        } else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to integer: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
    }

    /**
     * Retrieve a dimensional unit attribute at <var>index</var>. Unit
     * conversions are based on the current {@link DisplayMetrics}
     * associated with the resources this {@link TypedArray} object
     * came from.
     * <p>
     * This method will throw an exception if the attribute is defined but is
     * not a dimension.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute dimension value multiplied by the appropriate
     *         metric, or defValue if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not an integer.
     *
     * @see #getDimensionPixelOffset
     * @see #getDimensionPixelSize
     */
    public float getDimension(@StyleableRes int index, float defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final int attrIndex = index;
        index *= STYLE_NUM_ENTRIES;

        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimension(data[index + STYLE_DATA], mMetrics);
        } else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to dimension: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
    }

    /**
     * Retrieve a dimensional unit attribute at <var>index</var> for use
     * as an offset in raw pixels.  This is the same as
     * {@link #getDimension}, except the returned value is converted to
     * integer pixels for you.  An offset conversion involves simply
     * truncating the base value to an integer.
     * <p>
     * This method will throw an exception if the attribute is defined but is
     * not a dimension.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute dimension value multiplied by the appropriate
     *         metric and truncated to integer pixels, or defValue if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not an integer.
     *
     * @see #getDimension
     * @see #getDimensionPixelSize
     */
    public int getDimensionPixelOffset(@StyleableRes int index, int defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final int attrIndex = index;
        index *= STYLE_NUM_ENTRIES;

        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelOffset(data[index + STYLE_DATA], mMetrics);
        } else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to dimension: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
    }

    /**
     * Retrieve a dimensional unit attribute at <var>index</var> for use
     * as a size in raw pixels.  This is the same as
     * {@link #getDimension}, except the returned value is converted to
     * integer pixels for use as a size.  A size conversion involves
     * rounding the base value, and ensuring that a non-zero base value
     * is at least one pixel in size.
     * <p>
     * This method will throw an exception if the attribute is defined but is
     * not a dimension.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute dimension value multiplied by the appropriate
     *         metric and truncated to integer pixels, or defValue if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not a dimension.
     *
     * @see #getDimension
     * @see #getDimensionPixelOffset
     */
    public int getDimensionPixelSize(@StyleableRes int index, int defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final int attrIndex = index;
        index *= STYLE_NUM_ENTRIES;

        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelSize(data[index + STYLE_DATA], mMetrics);
        } else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to dimension: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
    }

    /**
     * Retrieves a fractional unit attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param base The base value of this fraction.  In other words, a
     *             standard fraction is multiplied by this value.
     * @param pbase The parent base value of this fraction.  In other
     *             words, a parent fraction (nn%p) is multiplied by this
     *             value.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute fractional value multiplied by the appropriate
     *         base value, or defValue if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not a fraction.
     */
    public float getFraction(@StyleableRes int index, int base, int pbase, float defValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final int attrIndex = index;
        index *= STYLE_NUM_ENTRIES;

        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type == TypedValue.TYPE_FRACTION) {
            return TypedValue.complexToFraction(data[index + STYLE_DATA], base, pbase);
        } else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to fraction: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
    }

    /**
     * Retrieve the Drawable for the attribute at <var>index</var>.
     * <p>
     * This method will throw an exception if the attribute is defined but is
     * not a color or drawable resource.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return Drawable for the attribute, or {@code null} if not defined.
     * @throws RuntimeException if the TypedArray has already been recycled.
     * @throws UnsupportedOperationException if the attribute is defined but is
     *         not a color or drawable resource.
     */
    @Nullable
    public Drawable getDrawable(@StyleableRes int index) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        final TypedValue value = mValue;
        if (getValueAt(index * STYLE_NUM_ENTRIES, value)) {
            if (value.type == TypedValue.TYPE_ATTRIBUTE) {
                throw new UnsupportedOperationException(
                        "Failed to resolve attribute at index " + index + ": " + value
                                + ", theme=" + mTheme);
            }

            return mResources.loadDrawable(value, null, mTheme);
        }
        return null;
    }

    /**
     * Retrieve the raw TypedValue for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param outValue TypedValue object in which to place the attribute's
     *                 data.
     *
     * @return {@code true} if the value was retrieved and not @empty, {@code false} otherwise.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public boolean getValue(@StyleableRes int index, TypedValue outValue) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        return getValueAt(index * STYLE_NUM_ENTRIES, outValue);
    }

    /**
     * Returns the type of attribute at the specified index.
     *
     * @param index Index of attribute whose type to retrieve.
     *
     * @return Attribute type.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public int getType(@StyleableRes int index) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        index *= STYLE_NUM_ENTRIES;
        return mData[index + STYLE_TYPE];
    }

    /**
     * Determines whether there is an attribute at <var>index</var>.
     * <p>
     * <strong>Note:</strong> If the attribute was set to {@code @empty} or
     * {@code @undefined}, this method returns {@code false}.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return True if the attribute has a value, false otherwise.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public boolean hasValue(@StyleableRes int index) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        index *= STYLE_NUM_ENTRIES;
        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        return type != TypedValue.TYPE_NULL;
    }

    /**
     * Determines whether there is an attribute at <var>index</var>, returning
     * {@code true} if the attribute was explicitly set to {@code @empty} and
     * {@code false} only if the attribute was undefined.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return True if the attribute has a value or is empty, false otherwise.
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public boolean hasValueOrEmpty(@StyleableRes int index) {
        if (mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }

        index *= STYLE_NUM_ENTRIES;
        final int[] data = mData;
        final int type = data[index + STYLE_TYPE];
        return type != TypedValue.TYPE_NULL
                || data[index + STYLE_DATA] == TypedValue.DATA_NULL_EMPTY;
    }

    /**
     * Recycles the TypedArray, to be re-used by a later caller. After calling
     * this function you must not ever touch the typed array again.
     *
     * @throws RuntimeException if the TypedArray has already been recycled.
     */
    public void recycle() {
        if (mRecycled) {
            throw new RuntimeException(this + " recycled twice!");
        }

        mRecycled = true;

        // These may have been set by the client.
        mTheme = null;
        mValue.object = null;

        mResources.mTypedArrayPool.release(this);
    }

    private boolean getValueAt(int offset, @NonNull TypedValue outValue) {
        final int[] data = mData;
        final int type = data[offset + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return false;
        }
        outValue.type = type;
        outValue.data = data[offset + STYLE_DATA];
        outValue.cookie = data[offset + STYLE_COOKIE];
        outValue.flags = data[offset + STYLE_FLAGS];
        outValue.object = (type == TypedValue.TYPE_STRING) ? loadStringValueAt(offset) : null;
        return true;
    }

    @Nullable
    private CharSequence loadStringValueAt(int index) {
        final int[] data = mData;
        final int cookie = data[index + STYLE_COOKIE];
        CharSequence value = null;
        if (cookie >= 0) {
            value = mResources.getPooledStringForCookie(cookie, data[index + STYLE_DATA]);
        }
        return value;
    }

    protected TypedArray(Resources resources) {
        mResources = resources;
        mMetrics = mResources.getDisplayMetrics();
    }
}
