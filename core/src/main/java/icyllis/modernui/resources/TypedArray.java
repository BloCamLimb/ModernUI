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

import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.TypedValue;

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
    static final int STYLE_ASSET_COOKIE = 2;
    static final int STYLE_CHANGING_CONFIGURATIONS = 3;

    private final Resources mResources;
    private DisplayMetrics mMetrics;

    private boolean mRecycled;

    Resources.Theme mTheme;

    int[] mData;
    int[] mIndices;
    int mLength;
    TypedValue mValue = new TypedValue();

    private void resize(int len) {
        mLength = len;
        final int dataLen = len * STYLE_NUM_ENTRIES;
        final int indicesLen = len + 1;
        if (mData == null || mData.length < dataLen) {
            mData = new int[dataLen];
            mIndices = new int[indicesLen];
        }
    }

    public int getDimensionPixelSize(int index, int defValue) {
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
        }/* else if (type == TypedValue.TYPE_ATTRIBUTE) {
            final TypedValue value = mValue;
            getValueAt(index, value);
            throw new UnsupportedOperationException(
                    "Failed to resolve attribute at index " + attrIndex + ": " + value
                            + ", theme=" + mTheme);
        }*/

        throw new UnsupportedOperationException("Can't convert value at index " + attrIndex
                + " to dimension: type=0x" + Integer.toHexString(type) + ", theme=" + mTheme);
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

        mResources.mTypedArrayPool.release(this);
    }

    private boolean getValueAt(int offset, TypedValue outValue) {
        final int[] data = mData;
        final int type = data[offset + STYLE_TYPE];
        if (type == TypedValue.TYPE_NULL) {
            return false;
        }
        outValue.type = type;
        outValue.data = data[offset + STYLE_DATA];
        outValue.assetCookie = data[offset + STYLE_ASSET_COOKIE];
        outValue.changingConfigurations = data[offset + STYLE_CHANGING_CONFIGURATIONS];
        outValue.string = (type == TypedValue.TYPE_STRING) ? loadStringValueAt(offset) : null;
        return true;
    }

    @Nullable
    private CharSequence loadStringValueAt(int index) {
        final int[] data = mData;
        final int cookie = data[index + STYLE_ASSET_COOKIE];
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
