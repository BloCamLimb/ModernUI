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
 * Class to hold <i>theming</i> information for rending of markdown.
 */
@Immutable
public final class MarkdownTheme {

    @NonNull
    public static MarkdownTheme create(@NonNull Context context) {
        return builderWithDefaults(context).build();
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    public static Builder builderWithDefaults(@NonNull Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        final IntUnaryOperator dp = value -> (int) (value * density + .5F);
        return new Builder()
                .setCodeBlockMargin(dp.applyAsInt(12));
    }

    public static final class Builder {

        private int mCodeTextColor;
        private int mCodeBlockTextColor;
        private int mCodeBackgroundColor;
        private int mCodeBlockBackgroundColor;
        private int mCodeBlockMargin;
        private Typeface mCodeTypeface;
        private Typeface mCodeBlockTypeface;
        private int mCodeTextSize;
        private int mCodeBlockTextSize;
        private float[] mHeadingTextSizeMultipliers;

        public Builder setCodeTextColor(int codeTextColor) {
            mCodeTextColor = codeTextColor;
            return this;
        }

        public Builder setCodeBlockTextColor(int codeBlockTextColor) {
            mCodeBlockTextColor = codeBlockTextColor;
            return this;
        }

        public Builder setCodeBackgroundColor(int codeBackgroundColor) {
            mCodeBackgroundColor = codeBackgroundColor;
            return this;
        }

        public Builder setCodeBlockBackgroundColor(int codeBlockBackgroundColor) {
            mCodeBlockBackgroundColor = codeBlockBackgroundColor;
            return this;
        }

        public Builder setCodeBlockMargin(int codeBlockMargin) {
            mCodeBlockMargin = codeBlockMargin;
            return this;
        }

        @NonNull
        public MarkdownTheme build() {
            return new MarkdownTheme(this);
        }
    }

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

    private final float[] mHeadingTextSizeMultipliers;

    private MarkdownTheme(@NonNull Builder b) {
        mCodeTextColor = b.mCodeTextColor;
        mCodeBlockTextColor = b.mCodeBlockTextColor;
        mCodeBackgroundColor = b.mCodeBackgroundColor;
        mCodeBlockBackgroundColor = b.mCodeBlockBackgroundColor;
        mCodeBlockMargin = b.mCodeBlockMargin;
        mCodeTypeface = b.mCodeTypeface;
        mCodeBlockTypeface = b.mCodeBlockTypeface;
        mCodeTextSize = b.mCodeTextSize;
        mCodeBlockTextSize = b.mCodeBlockTextSize;
        mHeadingTextSizeMultipliers = b.mHeadingTextSizeMultipliers;
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
}
