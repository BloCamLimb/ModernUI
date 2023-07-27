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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.Layout;

/**
 * A paragraph style affecting the leading margin. There can be multiple leading
 * margin spans on a single paragraph; they will be rendered in order, each
 * adding its margin to the ones before it. The leading margin is on the right
 * for lines in a right-to-left paragraph.
 * <p>
 * LeadingMarginSpans should be attached from the first character to the last
 * character of a single paragraph.
 */
public interface LeadingMarginSpan extends ParagraphStyle {

    /**
     * Returns the amount by which to adjust the leading margin. Positive values
     * move away from the leading edge of the paragraph, negative values move
     * towards it.
     *
     * @param first true if the request is for the first line of a paragraph,
     *              false for subsequent lines
     * @return the offset for the margin.
     */
    int getLeadingMargin(boolean first);

    /**
     * Renders the leading margin.  This is called before the margin has been
     * adjusted by the value returned by {@link #getLeadingMargin(boolean)}.
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
    void drawLeadingMargin(Canvas c, Paint p,
                           int x, int dir,
                           int top, int baseline, int bottom,
                           CharSequence text, int start, int end,
                           boolean first, Layout layout);

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
}
