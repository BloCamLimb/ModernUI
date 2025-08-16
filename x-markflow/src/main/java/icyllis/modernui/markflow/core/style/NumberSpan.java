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

package icyllis.modernui.markflow.core.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.text.ShapedText;
import icyllis.modernui.text.Layout;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.TextDirectionHeuristic;
import icyllis.modernui.text.TextDirectionHeuristics;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.TextShaper;
import icyllis.modernui.text.style.LeadingMarginSpan;
import icyllis.modernui.widget.TextView;

import java.util.List;

public class NumberSpan implements LeadingMarginSpan {

    /**
     * Process supplied `text` argument and supply TextView paint to all NumberSpans
     * in order for them to measure number.
     * <p>
     * NB, this method must be called <em>before</em> setting text to a TextView (`TextView#setText`
     * internally can trigger new Layout creation which will ask for leading margins right away)
     *
     * @param textView to which markdown will be applied
     * @param markdown parsed markdown to process
     */
    public static void measure(@NonNull TextView textView, @NonNull Spanned markdown) {
        final List<NumberSpan> spans = markdown.getSpans(
                0,
                markdown.length(),
                NumberSpan.class);

        final TextDirectionHeuristic dir = textView.getTextDirectionHeuristic();
        final TextPaint paint = textView.getPaint();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < spans.size(); i++) {
            spans.get(i).shapeText(dir, paint);
        }
    }

    private final int mBlockMargin;
    private final int mColor;
    private final String mNumber;

    private ShapedText mShapedNumber;

    public NumberSpan(int blockMargin, int color, String number) {
        mBlockMargin = blockMargin;
        mColor = color;
        mNumber = number;
    }

    private void shapeText(@NonNull TextDirectionHeuristic dir, @NonNull TextPaint paint) {
        mShapedNumber = TextShaper.shapeText(
                mNumber,
                0, mNumber.length(),
                dir, paint
        );
    }

    //TODO when text rendering is optimized, we just TextPaint to measure & draw (no cache),
    // making this span not dynamic; move to core framework; also add letter & roman support

    @Override
    public int getLeadingMargin(@NonNull TextPaint paint, boolean first) {
        int margin = mBlockMargin;
        if (mShapedNumber == null) {
            shapeText(TextDirectionHeuristics.FIRSTSTRONG_LTR, paint);
        }
        int adv = Math.round(mShapedNumber.getAdvance());
        if (adv > margin) {
            int mid = (margin + 1) / 2;
            return (int) Math.ceil((float) adv / mid) * mid;
        }
        return margin;
    }

    @Override
    public void drawLeadingMargin(@NonNull Canvas c, @NonNull TextPaint p,
                                  int x, int dir,
                                  int top, int baseline, int bottom,
                                  @NonNull CharSequence text, int start, int end,
                                  boolean first, @NonNull Layout layout) {
        if (first && ((Spanned) text).getSpanStart(this) == start) {
            int width = getLeadingMargin(p, false);
            if (dir > 0) {
                x += width - (int) mShapedNumber.getAdvance();
            } else {
                x -= width;
            }
            int oldColor = 0;
            int newColor = mColor;
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
