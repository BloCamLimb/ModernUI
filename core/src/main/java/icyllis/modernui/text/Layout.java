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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.text.style.ParagraphStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A base class that manages text layout in visual elements on the screen,
 * which is designed for text pages at a high-level.
 * <p>
 * For text that will be edited, use a {@link DynamicLayout},
 * which will be updated as the text changes.
 * For text that will not change, use a {@link StaticLayout}.
 *
 * @see StaticLayout
 * @see DynamicLayout
 * @since 3.0
 */
public abstract class Layout {

    public static final int DIR_LEFT_TO_RIGHT = 1;
    public static final int DIR_RIGHT_TO_LEFT = -1;

    private static final ParagraphStyle[] NO_PARA_SPANS = {};

    private CharSequence mText;
    private TextPaint mPaint;
    private int mWidth;
    private Alignment mAlignment;
    private boolean mSpannedText;
    private TextDirectionHeuristic mTextDir;

    /**
     * Subclasses of Layout use this constructor to set the display text,
     * width, and other standard properties.
     *
     * @param text  the text to render
     * @param paint the default paint for the layout.  Styles can override
     *              various attributes of the paint.
     * @param width the wrapping width for the text.
     * @param align whether to left, right, or center the text.  Styles can
     *              override the alignment.
     */
    protected Layout(CharSequence text, TextPaint paint,
                     int width, Alignment align) {
        this(text, paint, width, align, TextDirectionHeuristics.FIRSTSTRONG_LTR);
    }

    /**
     * Subclasses of Layout use this constructor to set the display text,
     * width, and other standard properties.
     *
     * @param text    the text to render
     * @param paint   the default paint for the layout.  Styles can override
     *                various attributes of the paint.
     * @param width   the wrapping width for the text.
     * @param align   whether to left, right, or center the text.  Styles can
     *                override the alignment.
     * @param textDir the text direction algorithm
     */
    protected Layout(CharSequence text, TextPaint paint,
                     int width, Alignment align, TextDirectionHeuristic textDir) {
        if (width < 0) {
            throw new IllegalArgumentException("Layout: " + width + " < 0");
        }

        // We probably should re-evaluate bgColor.
        if (paint != null) {
            paint.bgColor = 0;
        }

        mText = text;
        mPaint = paint;
        mWidth = width;
        mAlignment = align;
        mSpannedText = text instanceof Spanned;
        mTextDir = textDir;
    }

    /**
     * Draw this Layout on the specified Canvas.
     * <p>
     * Note that this method just calls {@link #drawBackground(Canvas, int, int)}
     * and then {@link #drawText(Canvas, int, int)}. If you need to draw something between the two,
     * such as blinking cursor and selection highlight, you may manually call them separately.
     *
     * @param canvas the canvas to draw on
     * @see #drawBackground(Canvas, int, int)
     * @see #drawText(Canvas, int, int)
     */
    public final void draw(@Nonnull Canvas canvas) {
        final long range = getLineRangeForDraw(canvas);
        if (range < 0) return;
        int firstLine = (int) (range >>> 32);
        int lastLine = (int) (range & 0xFFFFFFFFL);
        drawBackground(canvas, firstLine, lastLine);
        drawText(canvas, firstLine, lastLine);
    }

    /**
     * Draw the visible area of this Layout background layer on the specified canvas.
     * <p>
     * Significantly, visible area given by <code>firstLine</code> and
     * <code>lastLine</code> is computed by {@link #getLineRangeForDraw(Canvas)}.
     * You may never just call this method without that method.
     *
     * @param canvas    the canvas to draw on
     * @param firstLine first line index (inclusive)
     * @param lastLine  last line index (inclusive)
     * @see #drawText(Canvas, int, int)
     */
    public final void drawBackground(@Nonnull Canvas canvas, int firstLine, int lastLine) {
        if (!mSpannedText) return;
        assert firstLine >= 0 && lastLine >= firstLine;
        Spanned buffer = (Spanned) mText;
    }

    /**
     * Draw all visible text lines of this Layout on the specified canvas.
     * <p>
     * Significantly, visible area given by <code>firstLine</code> and
     * <code>lastLine</code> is computed by {@link #getLineRangeForDraw(Canvas)}.
     * You may never just call this method without that method.
     *
     * @param canvas    the canvas to draw on
     * @param firstLine first line index (inclusive)
     * @param lastLine  last line index (inclusive)
     * @see #drawBackground(Canvas, int, int)
     */
    public void drawText(@Nonnull Canvas canvas, int firstLine, int lastLine) {
        assert firstLine >= 0 && lastLine >= firstLine && lastLine < getLineCount();
        int previousLineBottom = getLineTop(firstLine);
        int previousLineEnd = getLineStart(firstLine);
        ParagraphStyle[] spans = NO_PARA_SPANS;
        int spanEnd = 0;
        final TextPaint paint = TextPaint.obtain();
        paint.set(mPaint);
        CharSequence buf = mText;

        Alignment paraAlign = mAlignment;
        TabStops tabStops = null;
        boolean tabStopsIsInitialized = false;

        // Draw the lines, one at a time.
        // The baseline is the top of the following line minus the current line's descent.
        for (int lineNum = firstLine; lineNum <= lastLine; lineNum++) {
            int start = previousLineEnd;
            previousLineEnd = getLineStart(lineNum + 1);
            int end = getLineVisibleEnd(lineNum, start, previousLineEnd);

            int ltop = previousLineBottom;
            int lbottom = getLineTop(lineNum + 1);
            previousLineBottom = lbottom;
            int lbaseline = lbottom - getLineDescent(lineNum);

            int dir = getParagraphDirection(lineNum);
            int left = 0;
            int right = mWidth;

            if (mSpannedText) {
                Spanned sp = (Spanned) buf;
                spans = getParagraphSpans(sp, start, spanEnd, ParagraphStyle.class);
            }

            boolean hasTab = getLineContainsTab(lineNum);
            // Can't tell if we have tabs for sure, currently
            if (hasTab && !tabStopsIsInitialized) {
                if (tabStops == null) {
                    tabStops = new TabStops(20, spans);
                } else {
                    tabStops.reset(20, spans);
                }
                tabStopsIsInitialized = true;
            }

            // Determine whether the line aligns to normal, opposite, or center.
            Alignment align = paraAlign;
            if (align == Alignment.ALIGN_LEFT) {
                align = (dir == DIR_LEFT_TO_RIGHT) ?
                        Alignment.ALIGN_NORMAL : Alignment.ALIGN_OPPOSITE;
            } else if (align == Alignment.ALIGN_RIGHT) {
                align = (dir == DIR_LEFT_TO_RIGHT) ?
                        Alignment.ALIGN_OPPOSITE : Alignment.ALIGN_NORMAL;
            }

            int x;
            final int indentWidth;
            if (align == Alignment.ALIGN_NORMAL) {
                if (dir == DIR_LEFT_TO_RIGHT) {
                    indentWidth = getIndentAdjust(lineNum, Alignment.ALIGN_LEFT);
                    x = left + indentWidth;
                } else {
                    indentWidth = -getIndentAdjust(lineNum, Alignment.ALIGN_RIGHT);
                    x = right - indentWidth;
                }
            } else {
                int max = (int) getLineExtent(lineNum, tabStops, false);
                if (align == Alignment.ALIGN_OPPOSITE) {
                    if (dir == DIR_LEFT_TO_RIGHT) {
                        indentWidth = -getIndentAdjust(lineNum, Alignment.ALIGN_RIGHT);
                        x = right - max - indentWidth;
                    } else {
                        indentWidth = getIndentAdjust(lineNum, Alignment.ALIGN_LEFT);
                        x = left - max + indentWidth;
                    }
                } else { // Alignment.ALIGN_CENTER
                    indentWidth = getIndentAdjust(lineNum, Alignment.ALIGN_CENTER);
                    max = max & ~1;
                    x = ((right + left - max) >> 1) + indentWidth;
                }
            }
        }
    }

    /**
     * Computes the range of visible lines that will be drawn on the specified canvas.
     * It will be used for {@link #drawText(Canvas, int, int)}. The higher 32 bits represent
     * the first line number, while the lower 32 bits represent the last line number.
     * Note that if the range is empty, then the method returns <code>~0L</code>.
     *
     * @param canvas the canvas used to draw this Layout
     * @return the range of lines that need to be drawn, possibly empty.
     */
    public final long getLineRangeForDraw(@Nonnull Canvas canvas) {
        final int lineCount = getLineCount();
        if (lineCount <= 0) {
            return ~0L;
        }
        final int bottom = getLineTop(lineCount);
        if (canvas.quickReject(0, 0, mWidth, bottom)) {
            return ~0L;
        }
        int lineNum = 0, lineTop = 0, lineBottom;
        int firstLine = -1, lastLine = -1;
        do {
            lineBottom = getLineTop(lineNum + 1);
            if (firstLine == -1 && !canvas.quickReject(0, lineTop, mWidth, lineBottom)) {
                firstLine = lineNum;
            } else if (canvas.quickReject(0, lineTop, mWidth, lineBottom)) {
                lastLine = lineNum - 1;
                break;
            }
            lineTop = lineBottom;
        } while (++lineNum < lineCount);

        assert firstLine != -1;
        if (lastLine == -1) {
            assert lineNum == lineCount;
            lastLine = lineCount - 1;
        }

        return (long) firstLine << 32 | lastLine;
    }

    /**
     * Get the line number corresponding to the specified vertical position.
     * If you ask for a position above 0, you get 0; if you ask for a position
     * below the bottom of the text, you get the last line.
     */
    // FIXME: It may be faster to do a linear search for layouts without many lines.
    public int getLineForVertical(int vertical) {
        int high = getLineCount(), low = -1, guess;

        while (high - low > 1) {
            guess = (high + low) / 2;

            if (getLineTop(guess) > vertical)
                high = guess;
            else
                low = guess;
        }

        return Math.max(low, 0);
    }

    /**
     * Return the text that is displayed by this Layout.
     */
    public final CharSequence getText() {
        return mText;
    }

    /**
     * Return the base Paint properties for this layout.
     * Do NOT change the paint, which may result in funny
     * drawing for this layout.
     */
    public final TextPaint getPaint() {
        return mPaint;
    }

    /**
     * Return the width of this layout.
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * Return the width to which this Layout is ellipsizing, or
     * {@link #getWidth} if it is not doing anything special.
     */
    public int getEllipsizedWidth() {
        return mWidth;
    }

    /**
     * Increase the width of this layout to the specified width.
     * Be careful to use this only when you know it is appropriate&mdash;
     * it does not cause the text to reflow to use the full new width.
     */
    public final void increaseWidthTo(int wid) {
        if (wid < mWidth) {
            throw new RuntimeException("attempted to reduce Layout width");
        }

        mWidth = wid;
    }

    /**
     * Return the total height of this layout.
     */
    public int getHeight() {
        return getLineTop(getLineCount());
    }

    /**
     * Return the total height of this layout.
     *
     * @param cap if true and max lines is set, returns the height of the layout at the max lines.
     */
    public int getHeight(boolean cap) {
        return getHeight();
    }

    /**
     * Return the base alignment of this layout.
     */
    public final Alignment getAlignment() {
        return mAlignment;
    }

    /**
     * Return the heuristic used to determine paragraph text direction.
     */
    public final TextDirectionHeuristic getTextDirectionHeuristic() {
        return mTextDir;
    }

    /**
     * Return the number of lines of text in this layout.
     */
    public abstract int getLineCount();

    /**
     * Return the vertical position of the top of the specified line
     * (0&hellip;getLineCount()).
     * If the specified line is equal to the line count, returns the
     * bottom of the last line.
     */
    public abstract int getLineTop(int line);

    /**
     * Return the descent of the specified line(0&hellip;getLineCount() - 1).
     */
    public abstract int getLineDescent(int line);

    /**
     * Return the text offset of the beginning of the specified line (
     * 0&hellip;getLineCount()). If the specified line is equal to the line
     * count, returns the length of the text.
     */
    public abstract int getLineStart(int line);

    /**
     * Returns the primary directionality of the paragraph containing the
     * specified line, either 0 for left-to-right lines, or 1 for right-to-left
     * lines (see {@link #DIR_LEFT_TO_RIGHT}, {@link #DIR_RIGHT_TO_LEFT}).
     */
    public abstract int getParagraphDirection(int line);

    /**
     * Returns whether the specified line contains one or more
     * characters that need to be handled specially, like tabs.
     */
    public abstract boolean getLineContainsTab(int line);

    /**
     * Returns the directional run information for the specified line.
     * The array alternates counts of characters in left-to-right
     * and right-to-left segments of the line.
     *
     * <p>NOTE: this is inadequate to support bidirectional text, and will change.
     */
    public abstract Directions getLineDirections(int line);

    /**
     * Returns the (negative) number of extra pixels of ascent padding in the
     * top line of the Layout.
     */
    public abstract int getTopPadding();

    /**
     * Returns the number of extra pixels of descent padding in the
     * bottom line of the Layout.
     */
    public abstract int getBottomPadding();

    /**
     * Returns the left indent for a line.
     */
    public int getIndentAdjust(int line, Alignment alignment) {
        return 0;
    }

    /**
     * Return the offset of the first character to be ellipsized away,
     * relative to the start of the line.  (So 0 if the beginning of the
     * line is ellipsized, not getLineStart().)
     */
    public abstract int getEllipsisStart(int line);

    /**
     * Returns the number of characters to be ellipsized away, or 0 if
     * no ellipsis is to take place.
     */
    public abstract int getEllipsisCount(int line);

    /**
     * Return the text offset after the last character on the specified line.
     */
    public final int getLineEnd(int line) {
        return getLineStart(line + 1);
    }

    /**
     * Return the text offset after the last visible character (so whitespace
     * is not counted) on the specified line.
     */
    public int getLineVisibleEnd(int line) {
        return getLineVisibleEnd(line, getLineStart(line), getLineStart(line + 1));
    }

    private int getLineVisibleEnd(int line, int start, int end) {
        CharSequence text = mText;
        char ch;
        if (line == getLineCount() - 1) {
            return end;
        }

        for (; end > start; end--) {
            ch = text.charAt(end - 1);

            if (ch == '\n') {
                return end - 1;
            }

            if (!LineBreaker.isLineEndSpace(ch)) {
                break;
            }

        }

        return end;
    }

    /**
     * Returns the same as <code>text.getSpans()</code>, except where
     * <code>start</code> and <code>end</code> are the same and are not
     * at the very beginning of the text, in which case an empty array
     * is returned instead.
     * <p>
     * This is needed because of the special case that <code>getSpans()</code>
     * on an empty range returns the spans adjacent to that range, which is
     * primarily for the sake of <code>TextWatchers</code> so they will get
     * notifications when text goes from empty to non-empty.  But it also
     * has the unfortunate side effect that if the text ends with an empty
     * paragraph, that paragraph accidentally picks up the styles of the
     * preceding paragraph (even though those styles will not be picked up
     * by new text that is inserted into the empty paragraph).
     * <p>
     * The reason it just checks whether <code>start</code> and <code>end</code>
     * is the same is that the only time a line can contain 0 characters
     * is if it is the final paragraph of the Layout; otherwise any line will
     * contain at least one printing or newline character.  The reason for the
     * additional check if <code>start</code> is greater than 0 is that
     * if the empty paragraph is the entire content of the buffer, paragraph
     * styles that are already applied to the buffer will apply to text that
     * is inserted into it.
     */
    @Nullable
    static <T> T[] getParagraphSpans(@Nonnull Spanned text, int start, int end, Class<T> type) {
        if (start == end && start > 0) {
            return null;
        }

        if (text instanceof SpannableStringBuilder) {
            return null/*((SpannableStringBuilder) text).getSpans(start, end, type, false)*/;
        } else {
            return text.getSpans(start, end, type, null);
        }
    }

    public enum Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        // internal use
        ALIGN_LEFT,
        // internal use
        ALIGN_RIGHT
    }
}
