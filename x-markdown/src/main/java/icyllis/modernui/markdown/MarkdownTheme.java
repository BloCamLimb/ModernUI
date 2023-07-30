/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markdown;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.text.Typeface;

import javax.annotation.concurrent.Immutable;
import java.util.function.IntUnaryOperator;

/**
 * Controlling the styled attributes for rendering Markdown.
 */
@Immutable
public final class MarkdownTheme {

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    public static Builder builderWithDefaults(@NonNull Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        final IntUnaryOperator dp = value -> (int) (value * density + .5F);
        return new Builder()
                .setListItemMargin(dp.applyAsInt(24))
                .setBlockQuoteMargin(dp.applyAsInt(16))
                .setCodeBlockMargin(dp.applyAsInt(12));
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
        }

        @NonNull
        public Builder setBlockQuoteMargin(int blockQuoteMargin) {
            mBlockQuoteMargin = blockQuoteMargin;
            return this;
        }

        @NonNull
        public Builder setBlockQuoteWidth(int blockQuoteWidth) {
            mBlockQuoteWidth = blockQuoteWidth;
            return this;
        }

        @NonNull
        public Builder setBlockQuoteColor(int blockQuoteColor) {
            mBlockQuoteColor = blockQuoteColor;
            return this;
        }

        @NonNull
        public Builder setListItemMargin(int listItemMargin) {
            mListItemMargin = listItemMargin;
            return this;
        }

        @NonNull
        public Builder setListItemColor(int listItemColor) {
            mListItemColor = listItemColor;
            return this;
        }

        @NonNull
        public Builder setCodeTextColor(int codeTextColor) {
            mCodeTextColor = codeTextColor;
            return this;
        }

        @NonNull
        public Builder setCodeBlockTextColor(int codeBlockTextColor) {
            mCodeBlockTextColor = codeBlockTextColor;
            return this;
        }

        @NonNull
        public Builder setCodeBackgroundColor(int codeBackgroundColor) {
            mCodeBackgroundColor = codeBackgroundColor;
            return this;
        }

        @NonNull
        public Builder setCodeBlockBackgroundColor(int codeBlockBackgroundColor) {
            mCodeBlockBackgroundColor = codeBlockBackgroundColor;
            return this;
        }

        @NonNull
        public Builder setCodeBlockMargin(int codeBlockMargin) {
            mCodeBlockMargin = codeBlockMargin;
            return this;
        }

        @NonNull
        public Builder setCodeTypeface(Typeface codeTypeface) {
            mCodeTypeface = codeTypeface;
            return this;
        }

        @NonNull
        public Builder setCodeBlockTypeface(Typeface codeBlockTypeface) {
            mCodeBlockTypeface = codeBlockTypeface;
            return this;
        }

        @NonNull
        public Builder setCodeTextSize(int codeTextSize) {
            mCodeTextSize = codeTextSize;
            return this;
        }

        @NonNull
        public Builder setCodeBlockTextSize(int codeBlockTextSize) {
            mCodeBlockTextSize = codeBlockTextSize;
            return this;
        }

        @NonNull
        public Builder setHeadingBreakColor(int headingBreakColor) {
            mHeadingBreakColor = headingBreakColor;
            return this;
        }

        @NonNull
        public Builder setHeadingTypeface(Typeface headingTypeface) {
            mHeadingTypeface = headingTypeface;
            return this;
        }

        @NonNull
        public Builder setHeadingTextSizeMultipliers(float[] headingTextSizeMultipliers) {
            mHeadingTextSizeMultipliers = headingTextSizeMultipliers;
            return this;
        }

        @NonNull
        public Builder setThematicBreakColor(int thematicBreakColor) {
            mThematicBreakColor = thematicBreakColor;
            return this;
        }

        @NonNull
        public MarkdownTheme build() {
            return new MarkdownTheme(this);
        }
    }

    // we use 14 as base
    private static final float[] HEADING_SIZES = {
            32f / 14f, 24f / 14f, 18.66f / 14f, 16f / 14f, 1.0f, 12f / 14f
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

    private MarkdownTheme(@NonNull Builder b) {
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
        return mBlockQuoteColor != 0
                ? mBlockQuoteColor
                : 0x30FFFFFF;
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
