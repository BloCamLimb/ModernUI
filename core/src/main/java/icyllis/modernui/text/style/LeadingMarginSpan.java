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

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.text.*;
import icyllis.modernui.util.Parcel;
import org.jetbrains.annotations.ApiStatus;

/**
 * A paragraph style affecting the leading margin. There can be multiple leading
 * margin spans on a single paragraph; they will be rendered in order, each
 * adding its margin to the ones before it. The leading margin is on the right
 * for lines in a right-to-left paragraph.
 * <p>
 * LeadingMarginSpans should be attached from the first character to the last
 * character of a single paragraph.
 * <p>
 * Since 3.8, there's <code>TrailingMarginSpan</code> affecting the trailing margin.
 * Since 3.12, the <code>TrailingMarginSpan</code> is merged into this interface.
 */
public interface LeadingMarginSpan extends ParagraphStyle {

    /**
     * Returns the amount by which to adjust the leading margin. Positive values
     * move away from the leading edge of the paragraph, negative values move
     * towards it.
     * <p>
     * This method is obsolete in favor of {@link #getLeadingMargin(TextPaint, boolean)}.
     *
     * @param first true if the request is for the first line of a paragraph,
     *              false for subsequent lines
     * @return the offset for the margin.
     */
    @ApiStatus.Obsolete
    @ApiStatus.OverrideOnly
    default int getLeadingMargin(boolean first) {
        return 0;
    }

    /**
     * Returns the amount by which to adjust the leading margin. Positive values
     * move away from the leading edge of the paragraph, negative values move
     * towards it.
     * <p>
     * The leading margin is on the left for lines in a left-to-right paragraph,
     * and on the right for lines in a right-to-left paragraph. The default
     * implementation is to call {@link #getLeadingMargin(boolean)}.
     * <p>
     * Added by Modern UI.
     *
     * @param paint the base paint (read-only)
     * @param first true if the request is for the first line of a paragraph,
     *              false for subsequent lines
     * @return the offset for the margin.
     */
    default int getLeadingMargin(@NonNull TextPaint paint, boolean first) {
        return getLeadingMargin(first);
    }

    /**
     * Returns the amount by which to adjust the trailing margin. Positive values
     * move away from the trailing edge of the paragraph, negative values move
     * towards it.
     * <p>
     * The trailing margin is on the right for lines in a left-to-right paragraph,
     * and on the left for lines in a right-to-left paragraph.
     * <p>
     * Added by Modern UI.
     *
     * @param paint the base paint (read-only)
     * @return the offset for the margin.
     */
    default int getTrailingMargin(@NonNull TextPaint paint) {
        return 0;
    }

    /**
     * Renders the leading margin.  This is called before the margin has been
     * adjusted by the value returned by {@link #getLeadingMargin(TextPaint, boolean)}.
     *
     * @param c        the canvas
     * @param p        the paint. This should be left unchanged on exit.
     * @param x        the current position of the margin
     * @param dir      the base direction of the paragraph; if negative, the margin
     *                 is to the right of the text, otherwise it is to the left.
     * @param top      the top of the line
     * @param baseline the baseline of the line
     * @param bottom   the bottom of the line
     * @param text     the text
     * @param start    the start of the line
     * @param end      the end of the line
     * @param first    true if this is the first line of its paragraph
     * @param layout   the layout containing this line
     */
    @ApiStatus.OverrideOnly
    default void drawLeadingMargin(@NonNull Canvas c, @NonNull TextPaint p,
                                   int x, int dir,
                                   int top, int baseline, int bottom,
                                   @NonNull CharSequence text, int start, int end,
                                   boolean first, @NonNull Layout layout) {
    }

    /**
     * Called when drawing the margin.
     * <p>
     * The default implementation is to call {@link #drawLeadingMargin}, where
     * <var>x</var> is either <var>left</var> or <var>right</var>, depending on
     * <var>dir</var>. This method allows to draw the trailing margin at the same time,
     * not just the leading margin.
     * <p>
     * Added by Modern UI.
     *
     * @param c        the canvas
     * @param p        the paint. This should be left unchanged on exit.
     * @param left     the current left position of the margin
     * @param right    the current right position of the margin
     * @param dir      the base direction of the paragraph; if negative, the margin
     *                 is to the right of the text, otherwise it is to the left.
     * @param top      the top of the line
     * @param baseline the baseline of the line
     * @param bottom   the bottom of the line
     * @param text     the text
     * @param start    the start of the line
     * @param end      the end of the line
     * @param first    true if this is the first line of its paragraph
     * @param layout   the layout containing this line
     */
    default void drawMargin(@NonNull Canvas c, @NonNull TextPaint p,
                            int left, int right, int dir,
                            int top, int baseline, int bottom,
                            @NonNull Spanned text, int start, int end,
                            boolean first, @NonNull Layout layout) {
        int x = dir == Layout.DIR_RIGHT_TO_LEFT ? right : left;
        drawLeadingMargin(c, p, x, dir,
                top, baseline, bottom,
                text, start, end,
                first, layout);
    }

    /**
     * An extended version of {@link LeadingMarginSpan}, which allows the
     * implementor to specify the number of lines of the paragraph to which
     * this object is attached that the "first line of paragraph" margin width
     * will be applied to.
     * <p>
     * There should only be one LeadingMarginSpan2 per paragraph. The leading
     * margin line count affects all LeadingMarginSpans in the paragraph,
     * adjusting the number of lines to which the first line margin is applied.
     * <p>
     * As with LeadingMarginSpans, LeadingMarginSpan2s should be attached from
     * the beginning to the end of a paragraph.
     */
    interface LeadingMarginSpan2 extends LeadingMarginSpan, WrapTogetherSpan {

        /**
         * Returns the number of lines of the paragraph to which this object is
         * attached that the "first line" margin will apply to.
         */
        int getLeadingMarginLineCount();
    }

    /**
     * The standard implementation of LeadingMarginSpan, which adjusts the
     * margin but does not do any rendering.
     */
    class Standard implements LeadingMarginSpan, ParcelableSpan {

        private final int mFirst, mRest;

        /**
         * Constructor taking separate indents for the first and subsequent
         * lines.
         *
         * @param first the indent for the first line of the paragraph
         * @param rest  the indent for the remaining lines of the paragraph
         */
        public Standard(int first, int rest) {
            mFirst = first;
            mRest = rest;
        }

        /**
         * Constructor taking an indent for all lines.
         *
         * @param every the indent of each line
         */
        public Standard(int every) {
            this(every, every);
        }

        public Standard(@NonNull Parcel src) {
            mFirst = src.readInt();
            mRest = src.readInt();
        }

        @Override
        public int getSpanTypeId() {
            return TextUtils.LEADING_MARGIN_SPAN;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(mFirst);
            dest.writeInt(mRest);
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return first ? mFirst : mRest;
        }
    }
}
