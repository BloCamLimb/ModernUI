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
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.Typeface;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
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
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    public static final class Builder {

        private int mBlockQuoteTextColor;
        private int mBlockQuoteMargin;
        private int mBlockQuoteWidth;
        private int mBlockQuoteColor;
        private int mListItemMargin;
        private int mListItemColor;
        private int mBulletWidth;
        private int mCodeTextColor;
        private int mCodeBlockTextColor;
        private int mCodeBackgroundColor;
        private int mCodeBlockBackgroundColor;
        private int mCodeBlockMargin;
        private Typeface mCodeTypeface;
        private Typeface mCodeBlockTypeface;
        private int mCodeTextSize;
        private float mCodeTextSizeMultiplier = 0.875f;
        private int mCodeBlockTextSize;
        private float mCodeBlockTextSizeMultiplier;
        private int mHeadingBreakColor;
        private int mHeadingBreakHeight;
        private Typeface mHeadingTypeface;
        private int mHeadingTextStyle = Typeface.BOLD;
        private int[] mHeadingTextSizes;
        private float[] mHeadingTextSizeMultipliers;
        private int mThematicBreakColor;
        private int mThematicBreakHeight;

        private transient IntUnaryOperator mDpToPx;

        Builder() {
            mBlockQuoteColor = 0xFF666666;
            mCodeBackgroundColor = 0x40000000;
            mCodeBlockBackgroundColor = 0x40000000;
            mHeadingBreakColor = 0x50FFFFFF;
            mThematicBreakColor = 0x30FFFFFF;
        }

        Builder(@NonNull Context context) {
            final float density = context.getResources().getDisplayMetrics().density;
            final IntUnaryOperator dp = value -> (int) (value * density + .5F);
            mDpToPx = dp;
            final var value = new TypedValue();
            final var theme = context.getTheme();

            if (theme.resolveAttribute(R.ns, R.attr.colorOnSurface, value, true)) {
                // M3 theme
                final int colorOnSurface = value.data;
                theme.resolveAttribute(R.ns, R.attr.colorOnSurfaceVariant, value, true);
                final int colorOnSurfaceVariant = value.data;
                mBlockQuoteTextColor = colorOnSurfaceVariant;
                mBlockQuoteColor = (colorOnSurfaceVariant & 0xFFFFFF) | 0x33000000;
                mCodeBackgroundColor = (colorOnSurfaceVariant & 0xFFFFFF) | 0x1F000000;
                mCodeBlockBackgroundColor = (colorOnSurface & 0xFFFFFF) | 0x1F000000;
                mHeadingBreakColor = (colorOnSurfaceVariant & 0xFFFFFF) | 0xB3000000;
            } else {
                theme.resolveAttribute(R.ns, R.attr.isLightTheme, value, true);
                final boolean isLightTheme = value.data != 0;
                if (isLightTheme) {
                    mBlockQuoteColor = 0x33194164;
                    mCodeBackgroundColor = 0x1F818B98;
                    mCodeBlockBackgroundColor = 0x1FB4C4D5;
                    mHeadingBreakColor = 0xB3D1D9E0;
                } else {
                    mBlockQuoteColor = 0x4DAAC2DF;
                    mCodeBackgroundColor = 0x33656C76;
                    mCodeBlockBackgroundColor = 0x336987AF;
                    mHeadingBreakColor = 0xB33D444D;
                }
            }
            mThematicBreakColor = mHeadingBreakColor;

            mListItemMargin = dp.applyAsInt(24);
            mBlockQuoteMargin = dp.applyAsInt(16);
            mCodeBlockMargin = dp.applyAsInt(12);
        }

        /**
         * The default text color in blockquotes.
         * If 0 is supplied, the main text color will be used.
         */
        @NonNull
        public Builder blockQuoteTextColor(@ColorInt int blockQuoteTextColor) {
            mBlockQuoteTextColor = blockQuoteTextColor;
            return this;
        }

        /**
         * Blockquote indentation in pixels.
         */
        @NonNull
        public Builder blockQuoteMargin(int blockQuoteMargin) {
            mBlockQuoteMargin = blockQuoteMargin;
            return this;
        }

        /**
         * Blockquote indentation in density-independent pixels.
         */
        @NonNull
        public Builder blockQuoteMarginDp(int blockQuoteMarginDp) {
            mBlockQuoteMargin = mDpToPx.applyAsInt(blockQuoteMarginDp);
            return this;
        }

        /**
         * The width of the vertical stripe of blockquotes in pixels.
         * If 0 is supplied, then 1/4 of block margin will be used.
         */
        @NonNull
        public Builder blockQuoteWidth(int blockQuoteWidth) {
            mBlockQuoteWidth = blockQuoteWidth;
            return this;
        }

        /**
         * The width of the vertical stripe of blockquotes in density-independent pixels.
         * If 0 is supplied, then 1/4 of block margin will be used.
         */
        @NonNull
        public Builder blockQuoteWidthDp(int blockQuoteWidthDp) {
            mBlockQuoteWidth = mDpToPx.applyAsInt(blockQuoteWidthDp);
            return this;
        }

        /**
         * The color of the vertical stripe of blockquotes.
         */
        @NonNull
        public Builder blockQuoteColor(@ColorInt int blockQuoteColor) {
            mBlockQuoteColor = blockQuoteColor;
            return this;
        }

        /**
         * List block indentation in pixels.
         */
        @NonNull
        public Builder listItemMargin(int listItemMargin) {
            mListItemMargin = listItemMargin;
            return this;
        }

        /**
         * List block indentation in density-independent pixels.
         */
        @NonNull
        public Builder listItemMarginDp(int listItemMarginDp) {
            mListItemMargin = mDpToPx.applyAsInt(listItemMarginDp);
            return this;
        }

        /**
         * The color of the bullet/number of list items.
         * If 0 is supplied, then text color will be used.
         */
        @NonNull
        public Builder listItemColor(@ColorInt int listItemColor) {
            mListItemColor = listItemColor;
            return this;
        }

        /**
         * The width of the bullet of list items in pixels.
         * If 0 is supplied, then 1/4 of line height will be used.
         */
        @NonNull
        public Builder bulletWidth(int bulletWidth) {
            mBulletWidth = bulletWidth;
            return this;
        }

        /**
         * The width of the bullet of list items in density-independent pixels.
         * If 0 is supplied, then 1/4 of line height will be used.
         */
        @NonNull
        public Builder bulletWidthDp(int bulletWidthDp) {
            mBulletWidth = mDpToPx.applyAsInt(bulletWidthDp);
            return this;
        }

        /**
         * The color of the inline code text.
         * If 0 is supplied, then main text color will be used.
         */
        @NonNull
        public Builder codeTextColor(@ColorInt int codeTextColor) {
            mCodeTextColor = codeTextColor;
            return this;
        }

        /**
         * The color of the text in code blocks.
         * If 0 is supplied, then code text color will be used.
         */
        @NonNull
        public Builder codeBlockTextColor(@ColorInt int codeBlockTextColor) {
            mCodeBlockTextColor = codeBlockTextColor;
            return this;
        }

        /**
         * The color of the background of inline code.
         * If 0 is supplied, then 0.12 alpha of text color will be used.
         */
        @NonNull
        public Builder codeBackgroundColor(@ColorInt int codeBackgroundColor) {
            mCodeBackgroundColor = codeBackgroundColor;
            return this;
        }

        /**
         * The color of the background of code blocks.
         * If 0 is supplied, then 0.12 alpha of text color will be used.
         */
        @NonNull
        public Builder codeBlockBackgroundColor(@ColorInt int codeBlockBackgroundColor) {
            mCodeBlockBackgroundColor = codeBlockBackgroundColor;
            return this;
        }

        /**
         * The padding of the text in code blocks in pixels.
         */
        @NonNull
        public Builder codeBlockMargin(int codeBlockMargin) {
            mCodeBlockMargin = codeBlockMargin;
            return this;
        }

        /**
         * The padding of the text in code blocks in density-independent pixels.
         */
        @NonNull
        public Builder codeBlockMarginDp(int codeBlockMarginDp) {
            mCodeBlockMargin = mDpToPx.applyAsInt(codeBlockMarginDp);
            return this;
        }

        /**
         * The typeface of the inline code.
         * If null is supplied, then MONOSPACED will be used.
         */
        @NonNull
        public Builder codeTypeface(@Nullable Typeface codeTypeface) {
            mCodeTypeface = codeTypeface;
            return this;
        }

        /**
         * The typeface of the text in code blocks.
         * If null is supplied, then code typeface will be used.
         */
        @NonNull
        public Builder codeBlockTypeface(@Nullable Typeface codeBlockTypeface) {
            mCodeBlockTypeface = codeBlockTypeface;
            return this;
        }

        /**
         * The text size of the inline code in pixels.
         * If 0 is supplied, then code text size multiplier will be used.
         */
        @NonNull
        public Builder codeTextSize(int codeTextSize) {
            mCodeTextSize = codeTextSize;
            return this;
        }

        /**
         * The relative text size of the inline code.
         * The {@link #codeTextSize(int)} will override this value.
         * If 0 is supplied, then code text size will be unchanged.
         */
        @NonNull
        public Builder codeTextSizeMultiplier(float codeTextSizeMultiplier) {
            mCodeTextSizeMultiplier = codeTextSizeMultiplier;
            return this;
        }

        /**
         * The text size of the code block in pixels.
         * If 0 is supplied, then code block text size multiplier will be used.
         */
        @NonNull
        public Builder codeBlockTextSize(int codeBlockTextSize) {
            mCodeBlockTextSize = codeBlockTextSize;
            return this;
        }

        /**
         * The relative text size of the code blocks.
         * The {@link #codeBlockTextSize(int)} will override this value.
         * If 0 is supplied, then code text size will be used.
         */
        @NonNull
        public Builder codeBlockTextSizeMultiplier(float codeBlockTextSizeMultiplier) {
            mCodeBlockTextSizeMultiplier = codeBlockTextSizeMultiplier;
            return this;
        }

        /**
         * The color of heading break.
         */
        @NonNull
        public Builder headingBreakColor(@ColorInt int headingBreakColor) {
            mHeadingBreakColor = headingBreakColor;
            return this;
        }

        /**
         * The thickness of heading break in pixels.
         * If 0 is supplied, then an internal strategy will be used.
         */
        @NonNull
        public Builder headingBreakHeight(int headingBreakHeight) {
            mHeadingBreakHeight = headingBreakHeight;
            return this;
        }

        /**
         * The thickness of heading break in density-independent pixels.
         * If 0 is supplied, then an internal strategy will be used.
         */
        @NonNull
        public Builder headingBreakHeightDp(int headingBreakHeightDp) {
            mHeadingBreakHeight = mDpToPx.applyAsInt(headingBreakHeightDp);
            return this;
        }

        /**
         * The typeface of all headings.
         */
        @NonNull
        public Builder headingTypeface(@Nullable Typeface headingTypeface) {
            mHeadingTypeface = headingTypeface;
            return this;
        }

        /**
         * The text style of all headings, possible values are defined in {@link Typeface}.
         */
        @NonNull
        public Builder headingTextStyle(@TextPaint.TextStyle int headingTextStyle) {
            mHeadingTextStyle = headingTextStyle;
            return this;
        }

        /**
         * The text size for headings at levels 1 to 6, in pixels.
         * If 0 is supplied, the corresponding multiplier will be used.
         */
        @NonNull
        public Builder headingTextSizes(@Nullable int... headingTextSizes) {
            mHeadingTextSizes = headingTextSizes;
            return this;
        }

        /**
         * The relative text size for headings at levels 1 to 6.
         * The {@link #headingTextSizes(int...)} will override these values.
         * If 0 is supplied, the default multiplier will be used.
         */
        @NonNull
        public Builder headingTextSizeMultipliers(@Nullable float... headingTextSizeMultipliers) {
            mHeadingTextSizeMultipliers = headingTextSizeMultipliers;
            return this;
        }

        /**
         * The color of thematic break.
         */
        @NonNull
        public Builder thematicBreakColor(@ColorInt int thematicBreakColor) {
            mThematicBreakColor = thematicBreakColor;
            return this;
        }

        /**
         * The thickness of thematic break in pixels.
         * If 0 is supplied, then an internal strategy will be used.
         */
        @NonNull
        public Builder thematicBreakHeight(int thematicBreakHeight) {
            mThematicBreakHeight = thematicBreakHeight;
            return this;
        }

        /**
         * The thickness of thematic break in density-independent pixels.
         * If 0 is supplied, then an internal strategy will be used.
         */
        @NonNull
        public Builder thematicBreakHeightDp(int thematicBreakHeightDp) {
            mThematicBreakHeight = mDpToPx.applyAsInt(thematicBreakHeightDp);
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

    private final int mBlockQuoteTextColor; // 0 = absent
    private final int mBlockQuoteMargin; // mandatory
    private final int mBlockQuoteWidth; // <=0 = absent
    private final int mBlockQuoteColor; // mandatory

    private final int mListItemMargin; // mandatory
    private final int mListItemColor; // 0 = absent
    private final int mBulletWidth; // <=0 = absent

    private final int mCodeTextColor; // 0 = absent
    private final int mCodeBlockTextColor; // 0 = absent
    private final int mCodeBackgroundColor; // 0 = absent
    private final int mCodeBlockBackgroundColor; // 0 = absent
    private final int mCodeBlockMargin; // mandatory

    private final Typeface mCodeTypeface; // null = absent
    private final Typeface mCodeBlockTypeface; // null = absent

    private final int mCodeTextSize; // <=0 = absent
    private final float mCodeTextSizeMultiplier; // <=0 = absent
    private final int mCodeBlockTextSize; // <=0 = absent
    private final float mCodeBlockTextSizeMultiplier; // <=0 = absent

    private final int mHeadingBreakColor; // mandatory
    private final int mHeadingBreakHeight; // <=0 = absent
    private final Typeface mHeadingTypeface; // null = absent
    private final int mHeadingTextStyle; // mandatory
    private final int[] mHeadingTextSizes; // <=0 = absent
    private final float[] mHeadingTextSizeMultipliers; // <=0 = absent

    private final int mThematicBreakColor; // mandatory
    private final int mThematicBreakHeight; // <=0 = absent

    private MarkflowTheme(@NonNull Builder b) {
        mBlockQuoteTextColor = b.mBlockQuoteTextColor;
        mBlockQuoteMargin = b.mBlockQuoteMargin;
        mBlockQuoteWidth = b.mBlockQuoteWidth > 0 ?
                b.mBlockQuoteWidth : mBlockQuoteMargin >> 2;
        mBlockQuoteColor = b.mBlockQuoteColor;
        mListItemMargin = b.mListItemMargin;
        mListItemColor = b.mListItemColor;
        mBulletWidth = Math.max(b.mBulletWidth, 0);
        mCodeTextColor = b.mCodeTextColor;
        mCodeBlockTextColor = b.mCodeBlockTextColor != 0 ?
                b.mCodeBlockTextColor : mCodeTextColor;
        mCodeBackgroundColor = b.mCodeBackgroundColor;
        mCodeBlockBackgroundColor = b.mCodeBlockBackgroundColor != 0 ?
                b.mCodeBlockBackgroundColor : mCodeBackgroundColor;
        mCodeBlockMargin = b.mCodeBlockMargin;
        mCodeTypeface = b.mCodeTypeface != null ?
                b.mCodeTypeface : Typeface.MONOSPACED;
        mCodeBlockTypeface = b.mCodeBlockTypeface != null ?
                b.mCodeBlockTypeface : mCodeTypeface;
        mCodeTextSize = b.mCodeTextSize;
        mCodeTextSizeMultiplier = b.mCodeTextSizeMultiplier;
        if (!(b.mCodeBlockTextSize > 0) && !(b.mCodeBlockTextSizeMultiplier > 0)) {
            mCodeBlockTextSize = mCodeTextSize;
            mCodeBlockTextSizeMultiplier = mCodeTextSizeMultiplier;
        } else {
            mCodeBlockTextSize = b.mCodeBlockTextSize;
            mCodeBlockTextSizeMultiplier = b.mCodeBlockTextSizeMultiplier;
        }
        mHeadingBreakColor = b.mHeadingBreakColor;
        mHeadingBreakHeight = b.mHeadingBreakHeight;
        mHeadingTypeface = b.mHeadingTypeface;
        mHeadingTextStyle = b.mHeadingTextStyle;
        mHeadingTextSizes = b.mHeadingTextSizes != null ?
                Arrays.copyOf(b.mHeadingTextSizes, 6) : null;
        mHeadingTextSizeMultipliers = b.mHeadingTextSizeMultipliers != null ?
                Arrays.copyOf(b.mHeadingTextSizeMultipliers, 6) : null;
        mThematicBreakColor = b.mThematicBreakColor;
        mThematicBreakHeight = b.mThematicBreakHeight;
    }

    public int getBlockQuoteTextColor() {
        return mBlockQuoteTextColor;
    }

    public int getBlockQuoteMargin() {
        return mBlockQuoteMargin;
    }

    public int getBlockQuoteWidth() {
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

    public int getBulletWidth() {
        return mBulletWidth;
    }

    public int getCodeTextColor() {
        return mCodeTextColor;
    }

    public int getCodeBackgroundColor() {
        return mCodeBackgroundColor;
    }

    @NonNull
    public Typeface getCodeTypeface() {
        return mCodeTypeface;
    }

    public int getCodeTextSize() {
        return mCodeTextSize;
    }

    public float getCodeTextSizeMultiplier() {
        return mCodeTextSizeMultiplier;
    }

    public int getCodeBlockTextColor() {
        return mCodeBlockTextColor;
    }

    public int getCodeBlockBackgroundColor() {
        return mCodeBlockBackgroundColor;
    }

    @NonNull
    public Typeface getCodeBlockTypeface() {
        return mCodeBlockTypeface;
    }

    public int getCodeBlockTextSize() {
        return mCodeBlockTextSize;
    }

    public float getCodeBlockTextSizeMultiplier() {
        return mCodeBlockTextSizeMultiplier;
    }

    public int getCodeBlockMargin() {
        return mCodeBlockMargin;
    }

    public int getHeadingBreakColor() {
        return mHeadingBreakColor;
    }

    public int getHeadingBreakHeight() {
        return mHeadingBreakHeight;
    }

    @Nullable
    public Typeface getHeadingTypeface() {
        return mHeadingTypeface;
    }

    @TextPaint.TextStyle
    public int getHeadingTextStyle() {
        return mHeadingTextStyle;
    }

    public int getHeadingTextSize(@IntRange(from = 1, to = 6) int level) {
        if (mHeadingTextSizes != null) {
            return mHeadingTextSizes[level - 1];
        }
        return 0;
    }

    public float getHeadingTextSizeMultiplier(@IntRange(from = 1, to = 6) int level) {
        if (mHeadingTextSizeMultipliers != null) {
            float value = mHeadingTextSizeMultipliers[level - 1];
            if (value > 0) {
                return value;
            }
        }
        return HEADING_SIZES[level - 1];
    }

    public int getThematicBreakColor() {
        return mThematicBreakColor;
    }

    public int getThematicBreakHeight() {
        return mThematicBreakHeight;
    }
}
