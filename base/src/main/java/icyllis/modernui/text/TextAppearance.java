/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.text;

import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.resources.AttributeSlots;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.ColorStateList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    private static final String[] ATTRS = {
            /*0*/"textColor",
            /*1*/"textColorHighlight",
            /*2*/"textColorHint",
            /*3*/"textColorLink",
            /*4*/"textFontWeight",
            /*5*/"textSize",
            /*6*/"textStyle",
    };
    public static final AttributeSlots ATTR_SLOTS = new AttributeSlots(null, ATTRS);

    public TextAppearance() {
    }

    public void applyStyle(Resources.@NonNull Theme theme,
                           @Nullable AttributeSet attrs,
                           @Nullable String defStyleAttr,
                           @Nullable @StyleRes ResourceId defStyleRes) {
        final TypedArray a = theme.obtainStyledAttributes(
                attrs, defStyleAttr, defStyleRes, ATTR_SLOTS);

        applyStyle(a, 0, 0);

        a.recycle();
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public int applyStyle(@NonNull TypedArray a, int ii, int baseAttr) {
        final int n = a.getIndexCount();
        ITER:for (; ii < n; ii++) {
            final int attr = a.getIndex(ii);
            switch (attr - baseAttr) {
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
                default:
                    break ITER;
            }
        }
        return ii;
    }
}
