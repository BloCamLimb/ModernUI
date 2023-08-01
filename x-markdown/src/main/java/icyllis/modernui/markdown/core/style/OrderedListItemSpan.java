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

package icyllis.modernui.markdown.core.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.text.ShapedText;
import icyllis.modernui.markdown.MarkdownTheme;
import icyllis.modernui.text.*;
import icyllis.modernui.text.style.LeadingMarginSpan;
import icyllis.modernui.widget.TextView;

import java.util.List;

public class OrderedListItemSpan implements LeadingMarginSpan {

    /**
     * Process supplied `text` argument and supply TextView paint to all OrderedListItemSpans
     * in order for them to measure number.
     * <p>
     * NB, this method must be called <em>before</em> setting text to a TextView (`TextView#setText`
     * internally can trigger new Layout creation which will ask for leading margins right away)
     *
     * @param textView to which markdown will be applied
     * @param markdown parsed markdown to process
     */
    public static void measure(@NonNull TextView textView, @NonNull Spanned markdown) {
        final List<OrderedListItemSpan> spans = markdown.getSpans(
                0,
                markdown.length(),
                OrderedListItemSpan.class);

        final TextDirectionHeuristic dir = textView.getTextDirectionHeuristic();
        final TextPaint paint = textView.getPaint();
        for (OrderedListItemSpan span : spans) {
            span.shape(dir, paint);
        }
    }

    private final MarkdownTheme mTheme;
    private final String mNumber;

    private ShapedText mShapedNumber;

    public OrderedListItemSpan(MarkdownTheme theme, String number) {
        mTheme = theme;
        mNumber = number;
    }

    private void shape(@NonNull TextDirectionHeuristic dir, @NonNull TextPaint paint) {
        mShapedNumber = TextShaper.shapeText(
                mNumber,
                0, mNumber.length(),
                0, mNumber.length(),
                dir, paint
        );
    }

    @Override
    public int getLeadingMargin(boolean first) {
        int margin = mTheme.getListItemMargin();
        if (mShapedNumber != null) {
            int adv = Math.round(mShapedNumber.getAdvance());
            if (adv > margin) {
                int mid = (margin + 1) / 2;
                return (int) Math.ceil((float) adv / mid) * mid;
            }
        }
        return margin;
    }

    @Override
    public void drawLeadingMargin(Canvas c, TextPaint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout) {
        if (first && ((Spanned) text).getSpanStart(this) == start) {
            if (mShapedNumber == null) {
                shape(TextDirectionHeuristics.FIRSTSTRONG_LTR, p);
            }
            int width = getLeadingMargin(false);
            if (dir > 0) {
                x += width - mShapedNumber.getAdvance();
            } else {
                x -= width;
            }
            int oldColor = 0;
            int newColor = mTheme.getListItemColor();
            if (newColor != 0) {
                oldColor = p.getColor();
                p.setColor(newColor);
            }
            c.drawShapedText(mShapedNumber, x, baseline, p);
            if (newColor != 0) {
                p.setColor(oldColor);
            }
        }
    }
}
