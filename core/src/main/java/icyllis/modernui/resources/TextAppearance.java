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

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.util.ColorStateList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

/**
 * Utility class that contains the data from parsing a TextAppearance style resource.
 *
 * @hidden
 */
@ApiStatus.Internal
public class TextAppearance {

    public int mTextColorHighlight = 0;
    public int mSearchResultHighlightColor = 0;
    public int mFocusedSearchResultHighlightColor = 0;
    @Nullable
    public ColorStateList mTextColor = null;
    @Nullable
    public ColorStateList mTextColorHint = null;
    @Nullable
    public ColorStateList mTextColorLink = null;
    public int mTextSize = -1; // in pixels
    @TypedValue.ComplexDimensionUnit
    public int mTextSizeUnit = TypedValue.COMPLEX_UNIT_PX;
    public Locale mTextLocale = null;
    public String mFontFamily = null;
    public Typeface mFontTypeface = null;
    public boolean mFontFamilyExplicit = false;
    public int mTypefaceIndex = -1;
    public int mTextStyle = Typeface.NORMAL;
    public boolean mAllCaps = false;
    public int mShadowColor = 0;
    public float mShadowDx = 0, mShadowDy = 0, mShadowRadius = 0;
    public boolean mHasElegant = false;
    public boolean mElegant = false;
    public boolean mHasFallbackLineSpacing = false;
    public boolean mFallbackLineSpacing = false;
    public boolean mHasLetterSpacing = false;
    public float mLetterSpacing = 0;
    public String mFontFeatureSettings = null;
    public String mFontVariationSettings = null;
    public boolean mHasLineBreakStyle = false;
    public boolean mHasLineBreakWordStyle = false;
    public int mLineBreakStyle = 0;
    public int mLineBreakWordStyle = 0;

    public static final String[] STYLEABLE = {
            /*0*/ R.ns, R.attr.textColor,
            /*1*/ R.ns, R.attr.textColorHighlight,
            /*2*/ R.ns, R.attr.textColorHint,
            /*3*/ R.ns, R.attr.textColorLink,
            /*4*/ R.ns, R.attr.textFontWeight,
            /*5*/ R.ns, R.attr.textSize,
            /*6*/ R.ns, R.attr.textStyle,
    };

    public TextAppearance() {
    }

    public TextAppearance(@NonNull Context context,
                          ResourceId resId) {
        final TypedArray a;
        if (resId != null && resId.type().equals("attr")) {
            a = context.getTheme().obtainStyledAttributes(
                    null, resId, null, STYLEABLE);
        } else {
            a = context.getTheme().obtainStyledAttributes(
                    resId, STYLEABLE);
        }

        read(a, null);

        a.recycle();
    }

    public void read(@NonNull TypedArray a, @Nullable Int2IntFunction indexMap) {
        final int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            final int attr = a.getIndex(i);
            int index = attr;
            // Translate style array index ids to TextAppearance ids.
            if (indexMap != null) {
                index = indexMap.get(attr);
                if (index == -1) {
                    // This value is not part of a Text Appearance and should be ignored.
                    continue;
                }
            }
            switch (index) {
                case 0: // textColor
                    mTextColor = a.getColorStateList(attr);
                    break;
                case 1: // textColorHighlight
                    mTextColorHighlight = a.getColor(attr, mTextColorHighlight);
                    break;
                case 2: // textColorHint
                    mTextColorHint = a.getColorStateList(attr);
                    break;
                case 3: // textColorLink
                    mTextColorLink = a.getColorStateList(attr);
                    break;
                case 4: // textFontWeight
                    //TODO
                    break;
                case 5: // textSize
                    mTextSize = a.getDimensionPixelSize(attr, mTextSize);
                    //noinspection DataFlowIssue
                    mTextSizeUnit = a.peekValue(attr).getComplexUnit();
                    break;
                case 6: // textStyle
                    mTextStyle = a.getInt(attr, mTextStyle);
                    break;
            }
        }
    }
}
