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
import icyllis.modernui.ModernUI;
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

    private final SpanSet<CharacterStyle> mCharacterStyleSpanSet = new SpanSet<>(CharacterStyle.class);

    public TextLine(@Nonnull CharSequence text) {
        mPaint = new TextPaint();
        mPaint.color = ~0;
        mText = text;
        mLen = text.length();
        mMeasuredParagraph = MeasuredParagraph.buildForStaticLayout(mPaint, text, 0, mLen, TextDirectionHeuristics.FIRSTSTRONG_LTR, null);
        mDir = mMeasuredParagraph.getParagraphDir();
        mDirections = mMeasuredParagraph.getDirections(0, mLen);
        mMeasuredText = mMeasuredParagraph.getMeasuredText();
        if (text instanceof Spanned) {
            mSpanned = (Spanned) text;
        }
        ModernUI.LOGGER.info(mMeasuredParagraph.getMemoryUsage());
    }

    /**
     * Draw the text line. If paragraph direction is RTL, then x should be on the opposite side.
     *
     * @param canvas canvas to draw on
     * @param x      the leading margin position
     * @param y      the baseline
     */
    public void draw(@Nonnull Canvas canvas, float x, float y) {
        final int runCount = mDirections.getRunCount();
        for (int runIndex = 0; runIndex < runCount; runIndex++) {
            final int runStart = mDirections.getRunStart(runIndex);
            if (runStart > mLen) {
                break;
            }
            final int runLimit = Math.min(runStart + mDirections.getRunLength(runIndex), mLen);
            final boolean runIsRtl = mDirections.isRunRtl(runIndex);

            final float advance = mMeasuredText.getAdvance(runStart, runLimit);
            if (mDir == Bidi.DIRECTION_LEFT_TO_RIGHT == runIsRtl) {
                if (runIsRtl) {
                    x += advance;
                } else {
                    x -= advance;
                }
                drawBidiRun(runStart, runLimit, runLimit, runIsRtl, canvas, x, y);
            } else {
                drawBidiRun(runStart, runLimit, runLimit, runIsRtl, canvas, x, y);
                if (runIsRtl) {
                    x -= advance;
                } else {
                    x += advance;
                }
            }
        }
    }

    private void drawBidiRun(int start, int measureLimit, int limit, boolean runIsRtl,
                             @Nonnull Canvas canvas, float x, float y) {
        TextPaint wp = TextPaint.obtain();
        boolean hasStyleSpan = false;
        if (mSpanned != null) {
            mCharacterStyleSpanSet.init(mSpanned, mStart + start, mStart + limit);
            hasStyleSpan = mCharacterStyleSpanSet.numberOfSpans != 0;
        }

        if (!hasStyleSpan) {
            // reset to base paint
            wp.set(mPaint);
            drawStyleRun(wp, start, limit, runIsRtl, canvas, x, y);
        } else {
            for (int i = start, inext; i < measureLimit; i = inext) {
                // reset to base paint
                wp.set(mPaint);
                MeasuredText.Run run = mMeasuredText.searchRun(i);
                if (run != null) {
                    inext = run.mEnd;
                    int mlimit = Math.min(inext, measureLimit);

                    /*for (int j = i, jnext; j < mlimit; j = jnext) {
                        jnext = mCharacterStyleSpanSet.getNextTransition(mStart + j, mStart + inext) -
                                mStart;

                        final int offset = Math.min(jnext, mlimit);
                        wp.set(mPaint);
                        for (int k = 0; k < mCharacterStyleSpanSet.numberOfSpans; k++) {
                            // Intentionally using >= and <= as explained above
                            if ((mCharacterStyleSpanSet.spanStarts[k] >= mStart + offset) ||
                                    (mCharacterStyleSpanSet.spanEnds[k] <= mStart + j)) continue;

                            final CharacterStyle span = mCharacterStyleSpanSet.spans[k];
                            span.updateDrawState(wp);
                        }
                    }*/
                    if (runIsRtl) {
                        x -= drawStyleRun(wp, i, mlimit, true, canvas, x, y);
                    } else {
                        x += drawStyleRun(wp, i, mlimit, false, canvas, x, y);
                    }
                } else {
                    break;
                }
            }
        }
        wp.recycle();
    }

    private float drawStyleRun(@Nonnull TextPaint paint, int start, int end, boolean runIsRtl,
                               @Nonnull Canvas canvas, float x, float y) {
        if (end == start) {
            return 0f;
        }
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
