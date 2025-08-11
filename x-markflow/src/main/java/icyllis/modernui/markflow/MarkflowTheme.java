/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markflow;

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.Typeface;

import javax.annotation.concurrent.Immutable;
import java.util.function.IntUnaryOperator;

/**
 * Controlling the styled attributes for rendering Markdown.
 * This class only holds information for core Markdown features.
 */
@Immutable
public final class MarkflowTheme {

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    public static Builder builderWithDefaults(@NonNull Context context) {
        return new Builder(context);
    }

    public static final class Builder {

        private int mBlockQuoteMargin;
        private int mBlockQuoteWidth;
        private int mBlockQuoteColor;
        private int mListItemMargin;
        private int mListItemColor;
        private int mCodeTextColor;
        private int mCodeBlockTextColor;
        private int mCodeBackgroundColor;
        private int mCodeBlockBackgroundColor;
        private int mCodeBlockMargin;
        private Typeface mCodeTypeface;
        private Typeface mCodeBlockTypeface;
        private int mCodeTextSize;
        private int mCodeBlockTextSize;
        private int mHeadingBreakColor;
        private Typeface mHeadingTypeface;
        private float[] mHeadingTextSizeMultipliers;
        private int mThematicBreakColor;

        Builder() {
            mBlockQuoteColor = 0xFF666666;
        }

        Builder(@NonNull Context context) {
            final float density = context.getResources().getDisplayMetrics().density;
            final IntUnaryOperator dp = value -> (int) (value * density + .5F);
            final TypedValue value = new TypedValue();
            context.getTheme().resolveAttribute(R.ns, R.attr.isLightTheme, value, true);
            final boolean isLightTheme = value.data != 0;

            if (isLightTheme) {
                mBlockQuoteColor = 0xFF4A4A4A;
                mListItemColor = 0xFF212121;
                mCodeBackgroundColor = 0xFFF5F5F5;
                mCodeBlockBackgroundColor = 0xFFEDEDED;
                mHeadingBreakColor = 0xFFE0E0E0;
                mThematicBreakColor = 0xFFBDBDBD;
            } else {
                mBlockQuoteColor = 0xFFCCCCCC;
                mListItemColor = 0xFFE0E0E0;
                mCodeBackgroundColor = 0xFF2A2A2A;
                mCodeBlockBackgroundColor = 0xFF1E1E1E;
                mHeadingBreakColor = 0xFF3A3A3A;
                mThematicBreakColor = 0xFF444444;
            }

            mListItemMargin = dp.applyAsInt(24);
            mBlockQuoteMargin = dp.applyAsInt(16);
            mCodeBlockMargin = dp.applyAsInt(12);
        }

        @NonNull
        public Builder blockQuoteMargin(int blockQuoteMargin) {
            mBlockQuoteMargin = blockQuoteMargin;
            return this;
        }

        @NonNull
        public Builder blockQuoteWidth(int blockQuoteWidth) {
            mBlockQuoteWidth = blockQuoteWidth;
            return this;
        }

        @NonNull
        public Builder blockQuoteColor(int blockQuoteColor) {
            mBlockQuoteColor = blockQuoteColor;
            return this;
        }

        @NonNull
        public Builder listItemMargin(int listItemMargin) {
            mListItemMargin = listItemMargin;
            return this;
        }

        @NonNull
        public Builder listItemColor(int listItemColor) {
            mListItemColor = listItemColor;
            return this;
        }

        @NonNull
        public Builder codeTextColor(int codeTextColor) {
            mCodeTextColor = codeTextColor;
            return this;
        }

        @NonNull
        public Builder codeBlockTextColor(int codeBlockTextColor) {
            mCodeBlockTextColor = codeBlockTextColor;
            return this;
        }

        @NonNull
        public Builder codeBackgroundColor(int codeBackgroundColor) {
            mCodeBackgroundColor = codeBackgroundColor;
            return this;
        }

        @NonNull
        public Builder codeBlockBackgroundColor(int codeBlockBackgroundColor) {
            mCodeBlockBackgroundColor = codeBlockBackgroundColor;
            return this;
        }

        @NonNull
        public Builder codeBlockMargin(int codeBlockMargin) {
            mCodeBlockMargin = codeBlockMargin;
            return this;
        }

        @NonNull
        public Builder codeTypeface(Typeface codeTypeface) {
            mCodeTypeface = codeTypeface;
            return this;
        }

        @NonNull
        public Builder codeBlockTypeface(Typeface codeBlockTypeface) {
            mCodeBlockTypeface = codeBlockTypeface;
            return this;
        }

        @NonNull
        public Builder codeTextSize(int codeTextSize) {
            mCodeTextSize = codeTextSize;
            return this;
        }

        @NonNull
        public Builder codeBlockTextSize(int codeBlockTextSize) {
            mCodeBlockTextSize = codeBlockTextSize;
            return this;
        }

        @NonNull
        public Builder headingBreakColor(int headingBreakColor) {
            mHeadingBreakColor = headingBreakColor;
            return this;
        }

        @NonNull
        public Builder headingTypeface(Typeface headingTypeface) {
            mHeadingTypeface = headingTypeface;
            return this;
        }

        @NonNull
        public Builder headingTextSizeMultipliers(float[] headingTextSizeMultipliers) {
            mHeadingTextSizeMultipliers = headingTextSizeMultipliers;
            return this;
        }

        @NonNull
        public Builder thematicBreakColor(int thematicBreakColor) {
            mThematicBreakColor = thematicBreakColor;
            return this;
        }

        /**
         * Build an instance. This builder can be reused and the state will be preserved.
         */
        @NonNull
        public MarkflowTheme build() {
            return new MarkflowTheme(this);
        }
    }

    private static final float[] HEADING_SIZES = {
            //2.0f, 1.5f, 1.17f, 1.0f, 0.83f, 0.67f
            2.0f, 1.5f, 1.25f, 1.125f, 1.0f, 0.875f
    };

    private final int mBlockQuoteMargin;

    private final int mBlockQuoteWidth;

    private final int mBlockQuoteColor;

    private final int mListItemMargin;

    private final int mListItemColor;

    // by default - main text color
    private final int mCodeTextColor;

    // by default - codeTextColor
    private final int mCodeBlockTextColor;

    // by default 0.1 alpha of textColor/codeTextColor
    private final int mCodeBackgroundColor;

    // by default codeBackgroundColor
    private final int mCodeBlockBackgroundColor;

    // by default `width` of a space char... it's fun and games, but span doesn't have access to paint in
    // `getLeadingMargin`
    // so, we need to set this value explicitly (think of an utility method, that takes TextView/TextPaint and
    // measures space char)
    private final int mCodeBlockMargin;

    private final Typeface mCodeTypeface;

    private final Typeface mCodeBlockTypeface;

    private final int mCodeTextSize;

    private final int mCodeBlockTextSize;

    private final int mHeadingBreakColor;

    private final Typeface mHeadingTypeface;

    private final float[] mHeadingTextSizeMultipliers;

    private final int mThematicBreakColor;

    private MarkflowTheme(@NonNull Builder b) {
        mBlockQuoteMargin = b.mBlockQuoteMargin;
        mBlockQuoteWidth = b.mBlockQuoteWidth;
        mBlockQuoteColor = b.mBlockQuoteColor;
        mListItemMargin = b.mListItemMargin;
        mListItemColor = b.mListItemColor;
        mCodeTextColor = b.mCodeTextColor;
        mCodeBlockTextColor = b.mCodeBlockTextColor;
        mCodeBackgroundColor = b.mCodeBackgroundColor;
        mCodeBlockBackgroundColor = b.mCodeBlockBackgroundColor;
        mCodeBlockMargin = b.mCodeBlockMargin;
        mCodeTypeface = b.mCodeTypeface;
        mCodeBlockTypeface = b.mCodeBlockTypeface;
        mCodeTextSize = b.mCodeTextSize;
        mCodeBlockTextSize = b.mCodeBlockTextSize;
        mHeadingBreakColor = b.mHeadingBreakColor;
        mHeadingTypeface = b.mHeadingTypeface;
        mHeadingTextSizeMultipliers = b.mHeadingTextSizeMultipliers;
        mThematicBreakColor = b.mThematicBreakColor;
    }

    // always
    public int getBlockQuoteMargin() {
        return mBlockQuoteMargin;
    }

    public int getBlockQuoteWidth() {
        if (mBlockQuoteWidth == 0) {
            return mBlockQuoteMargin >> 2;
        }
        return mBlockQuoteWidth;
    }

    public int getBlockQuoteColor() {
        return mBlockQuoteColor;
    }

    public int getListItemMargin() {
        return mListItemMargin;
    }

    public int getListItemColor() {
        return mListItemColor;
    }

    public int getCodeTextColor() {
        return mCodeTextColor;
    }

    public int getCodeBackgroundColor() {
        return mCodeBackgroundColor != 0
                ? mCodeBackgroundColor
                : 0x40000000;
    }

    @NonNull
    public Typeface getCodeTypeface() {
        return (mCodeTypeface != null)
                ? mCodeTypeface
                : Typeface.MONOSPACED;
    }

    public int getCodeTextSize() {
        return mCodeTextSize;
    }

    public int getCodeBlockTextColor() {
        return mCodeBlockTextColor;
    }

    public int getCodeBlockBackgroundColor() {
        return mCodeBlockBackgroundColor != 0
                ? mCodeBlockBackgroundColor
                : getCodeBackgroundColor();
    }

    @NonNull
    public Typeface getCodeBlockTypeface() {
        return (mCodeBlockTypeface != null)
                ? mCodeBlockTypeface
                : getCodeTypeface();
    }

    public int getCodeBlockTextSize() {
        return mCodeBlockTextSize;
    }

    public int getCodeBlockMargin() {
        return mCodeBlockMargin;
    }

    public int getHeadingBreakColor() {
        return mHeadingBreakColor != 0
                ? mHeadingBreakColor
                : 0x50FFFFFF;
    }

    public Typeface getHeadingTypeface() {
        return mHeadingTypeface;
    }

    public float getHeadingTextSizeMultiplier(int level) {
        if (mHeadingTextSizeMultipliers != null) {
            return mHeadingTextSizeMultipliers[level - 1];
        } else {
            return HEADING_SIZES[level - 1];
        }
    }

    public int getThematicBreakColor() {
        return mThematicBreakColor != 0
                ? mThematicBreakColor
                : 0x30FFFFFF;
    }
}
