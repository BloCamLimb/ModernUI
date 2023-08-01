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

package icyllis.modernui.graphics.text;

import icyllis.modernui.annotation.NonNull;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Indicates the strategies can be used when calculating the text wrapping.
 * <p>
 * See <a href="https://www.w3.org/TR/css-text-3/#line-break-property">the line-break property</a>
 */
public final class LineBreakConfig {

    /**
     * No line break style specified.
     */
    public static final int LINE_BREAK_STYLE_NONE = 0;

    /**
     * Use the least restrictive rule for line-breaking. This is usually used for short lines.
     */
    public static final int LINE_BREAK_STYLE_LOOSE = 1;

    /**
     * Indicate breaking text with the most comment set of line-breaking rules.
     */
    public static final int LINE_BREAK_STYLE_NORMAL = 2;

    /**
     * Indicates breaking text with the most strictest line-breaking rules.
     */
    public static final int LINE_BREAK_STYLE_STRICT = 3;

    @ApiStatus.Internal
    @MagicConstant(intValues = {
            LINE_BREAK_STYLE_NONE, LINE_BREAK_STYLE_LOOSE, LINE_BREAK_STYLE_NORMAL,
            LINE_BREAK_STYLE_STRICT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LineBreakStyle {
    }

    /**
     * No line break word style specified.
     */
    public static final int LINE_BREAK_WORD_STYLE_NONE = 0;

    /**
     * Indicates the line breaking is based on the phrased. This makes text wrapping only on
     * meaningful words. The support of the text wrapping word style varies depending on the
     * locales. If the locale does not support the phrase based text wrapping,
     * there will be no effect.
     */
    public static final int LINE_BREAK_WORD_STYLE_PHRASE = 1;

    @ApiStatus.Internal
    @MagicConstant(intValues = {
            LINE_BREAK_WORD_STYLE_NONE, LINE_BREAK_WORD_STYLE_PHRASE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LineBreakWordStyle {
    }

    /**
     * A builder for creating {@link LineBreakConfig}.
     */
    public static final class Builder {
        // The line break style for the LineBreakConfig.
        private @LineBreakStyle int mLineBreakStyle = LineBreakConfig.LINE_BREAK_STYLE_NONE;

        // The line break word style for the LineBreakConfig.
        private @LineBreakWordStyle int mLineBreakWordStyle =
                LineBreakConfig.LINE_BREAK_WORD_STYLE_NONE;

        /**
         * Builder constructor with line break parameters.
         */
        public Builder() {
        }

        /**
         * Set the line break style.
         *
         * @param lineBreakStyle the new line break style.
         * @return this Builder
         */
        public @NonNull Builder setLineBreakStyle(@LineBreakStyle int lineBreakStyle) {
            mLineBreakStyle = lineBreakStyle;
            return this;
        }

        /**
         * Set the line break word style.
         *
         * @param lineBreakWordStyle the new line break word style.
         * @return this Builder
         */
        public @NonNull Builder setLineBreakWordStyle(@LineBreakWordStyle int lineBreakWordStyle) {
            mLineBreakWordStyle = lineBreakWordStyle;
            return this;
        }

        /**
         * Build the {@link LineBreakConfig}
         *
         * @return the LineBreakConfig instance.
         */
        @NonNull
        public LineBreakConfig build() {
            return new LineBreakConfig(mLineBreakStyle, mLineBreakWordStyle);
        }
    }

    /**
     * Create the LineBreakConfig instance.
     *
     * @param lineBreakStyle     the line break style for text wrapping.
     * @param lineBreakWordStyle the line break word style for text wrapping.
     * @return the {@link LineBreakConfig} instance.
     */
    @ApiStatus.Internal
    @NonNull
    public static LineBreakConfig getLineBreakConfig(@LineBreakStyle int lineBreakStyle,
                                                     @LineBreakWordStyle int lineBreakWordStyle) {
        LineBreakConfig.Builder builder = new LineBreakConfig.Builder();
        return builder.setLineBreakStyle(lineBreakStyle)
                .setLineBreakWordStyle(lineBreakWordStyle)
                .build();
    }

    @ApiStatus.Internal
    public static final LineBreakConfig NONE =
            new Builder().setLineBreakStyle(LINE_BREAK_STYLE_NONE)
                    .setLineBreakWordStyle(LINE_BREAK_WORD_STYLE_NONE).build();

    private final @LineBreakStyle int mLineBreakStyle;
    private final @LineBreakWordStyle int mLineBreakWordStyle;

    /**
     * Constructor with the line break parameters.
     * Use the {@link LineBreakConfig.Builder} to create the LineBreakConfig instance.
     */
    private LineBreakConfig(@LineBreakStyle int lineBreakStyle,
                            @LineBreakWordStyle int lineBreakWordStyle) {
        mLineBreakStyle = lineBreakStyle;
        mLineBreakWordStyle = lineBreakWordStyle;
    }

    /**
     * Get the line break style.
     *
     * @return The current line break style to be used for the text wrapping.
     */
    public @LineBreakStyle int getLineBreakStyle() {
        return mLineBreakStyle;
    }

    /**
     * Get the line break word style.
     *
     * @return The current line break word style to be used for the text wrapping.
     */
    public @LineBreakWordStyle int getLineBreakWordStyle() {
        return mLineBreakWordStyle;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof LineBreakConfig that)) return false;
        return (mLineBreakStyle == that.mLineBreakStyle)
                && (mLineBreakWordStyle == that.mLineBreakWordStyle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLineBreakStyle, mLineBreakWordStyle);
    }
}
