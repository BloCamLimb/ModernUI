/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text;

import com.ibm.icu.text.Bidi;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.text.style.CharacterStyle;

import javax.annotation.Nonnull;

/**
 * Controls the layout of a single line of styled text, for measuring in visual
 * order and rendering. It can be used in any view without a TextView to draw a
 * line of text.
 */
public class TextLine {

    // the base paint
    private TextPaint mPaint;
    private CharSequence mText;

    // start offset to the text
    private int mStart;
    private int mLen;

    // the base direction
    private int mDir;
    private Directions mDirections;

    private MeasuredParagraph mMeasuredParagraph;
    private MeasuredText mMeasuredText;

    private Spanned mSpanned;

    public TextLine(@Nonnull CharSequence text) {
        mPaint = new TextPaint();
        mPaint.setColor(~0);
        mText = text;
        mLen = text.length();
        mMeasuredParagraph = MeasuredParagraph.buildForStaticLayout(mPaint, text, 0, mLen,
                TextDirectionHeuristics.FIRSTSTRONG_LTR, false, null);
        mDir = mMeasuredParagraph.getParagraphDir();
        mDirections = mMeasuredParagraph.getDirections(0, mLen);
        mMeasuredText = mMeasuredParagraph.getMeasuredText();
        if (text instanceof Spanned) {
            mSpanned = (Spanned) text;
        }
    }

    /**
     * Draw the text line, based on visual order. If paragraph (base) direction is RTL,
     * then x should be on the opposite side (i.e. leading) corresponding to normal one.
     *
     * @param canvas canvas to draw on
     * @param x      the leading margin position
     * @param y      the baseline, invariant in this text line
     */
    public void draw(@Nonnull Canvas canvas, float x, float y) {
        final TextPaint wp = TextPaint.obtain();
        final SpanSet ss = SpanSet.obtain();
        final int runCount = mDirections.getRunCount();
        for (int runIndex = 0; runIndex < runCount; runIndex++) {
            final int runStart = mDirections.getRunStart(runIndex);
            if (runStart >= mLen) {
                break;
            }
            final int runLimit = Math.min(runStart + mDirections.getRunLength(runIndex), mLen);
            final boolean runIsRtl = mDirections.isRunRtl(runIndex);

            final float advance = mMeasuredParagraph.getAdvance(runStart, runLimit);
            final boolean diffDir = (mDir == Bidi.DIRECTION_LEFT_TO_RIGHT) == runIsRtl;
            if (diffDir) {
                if (runIsRtl) {
                    x += advance;
                } else {
                    x -= advance;
                }
                drawBidiRun(canvas, x, y, runStart, runLimit, runIsRtl, wp, ss);
            } else {
                drawBidiRun(canvas, x, y, runStart, runLimit, runIsRtl, wp, ss);
                if (runIsRtl) {
                    x -= advance;
                } else {
                    x += advance;
                }
            }
        }
        wp.recycle();
        ss.recycle();
    }

    /**
     * Draws a unidirectional (but possibly multi-styled) run of text.
     *
     * @param canvas   the canvas to draw on
     * @param x        the position of the run that is closest to the leading margin
     * @param y        the baseline
     * @param start    the line-relative start
     * @param limit    the line-relative limit
     * @param runIsRtl true if the run is right-to-left
     * @param wp       working paint
     * @param ss       recyclable span set
     */
    private void drawBidiRun(@Nonnull Canvas canvas, float x, float y, int start, int limit,
                             boolean runIsRtl, @Nonnull TextPaint wp, @Nonnull SpanSet ss) {
        final boolean plain;
        if (mSpanned == null) {
            plain = true;
        } else {
            ss.init(mSpanned, mStart + start, mStart + limit);
            plain = ss.mSize == 0;
        }
        if (plain) {
            // reset to base paint
            wp.set(mPaint);
            drawStyleRun(wp, start, limit, runIsRtl, canvas, x, y);
        } else {
            int runIndex = mMeasuredText.search(mStart + start);
            assert runIndex >= 0;
            final MeasuredText.Run[] runs = mMeasuredText.getRuns();
            while (runIndex < runs.length) {
                final MeasuredText.Run run = runs[runIndex++];
                final int runStart = run.mStart;
                final int runEnd = run.mEnd;
                // reset to base paint
                wp.set(mPaint);

                for (int k = 0; k < ss.mSize; k++) {
                    // Intentionally using >= and <= as explained above
                    if ((ss.mSpanStarts[k] >= runEnd) ||
                            (ss.mSpanEnds[k] <= runStart)) continue;

                    final CharacterStyle span = ss.mSpans[k];
                    span.updateDrawState(wp);
                }

                if (runIsRtl) {
                    x -= drawStyleRun(wp, runStart, runEnd, true, canvas, x, y);
                } else {
                    x += drawStyleRun(wp, runStart, runEnd, false, canvas, x, y);
                }
            }
        }
    }

    private float drawStyleRun(@Nonnull TextPaint paint, int start, int end, boolean runIsRtl,
                               @Nonnull Canvas canvas, float x, float y) {
        assert start != end;
        float advance = mMeasuredText.getAdvance(start, end);

        final float left, right;
        if (runIsRtl) {
            left = x - advance;
            right = x;
        } else {
            left = x;
            right = x + advance;
        }

        canvas.drawTextRun(mMeasuredText, start, end, left, y, paint);

        return advance;
    }
}
