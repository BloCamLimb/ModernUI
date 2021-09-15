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

import com.ibm.icu.text.BreakIterator;
import it.unimi.dsi.fastutil.ints.IntArrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides automatic line breaking for a <em>single</em> paragraph.
 */
public class LineBreaker {

    private static final int NOWHERE = 0xFFFFFFFF;

    private static final BreakIterator sBreaker = BreakIterator.getLineInstance(Locale.ROOT);

    // This function determines whether a character is a space that disappears at end of line.
    // It is the Unicode set: [[:General_Category=Space_Separator:]-[:Line_Break=Glue:]], plus '\n'.
    // Note: all such characters are in the BMP, so it's ok to use code units for this.
    public static boolean isLineEndSpace(char c) {
        return c == '\n' || c == ' '                            // SPACE
                || c == 0x1680                                  // OGHAM SPACE MARK
                || (0x2000 <= c && c <= 0x200A && c != 0x2007)  // EN QUAD, EM QUAD, EN SPACE, EM SPACE,
                // THREE-PER-EM SPACE, FOUR-PER-EM SPACE,
                // SIX-PER-EM SPACE, PUNCTUATION SPACE,
                // THIN SPACE, HAIR SPACE
                || c == 0x205F  // MEDIUM MATHEMATICAL SPACE
                || c == 0x3000;
    }

    @Nonnull
    private final char[] mTextBuf;
    @Nonnull
    private final MeasuredText mMeasuredText;
    @Nonnull
    private final LineWidth mLineWidthLimits;
    @Nonnull
    private final TabStops mTabStops;

    private int mLineNum = 0;
    private float mLineWidth = 0;
    private float mCharsAdvance = 0;
    private float mLineWidthLimit;

    private int mPrevBoundaryOffset = NOWHERE;
    private float mLineWidthAtPrevBoundary = 0;
    private float mCharsAdvanceAtPrevBoundary = 0;

    private final List<BreakPoint> mBreakPoints = new ArrayList<>();

    public LineBreaker(@Nonnull char[] textBuf, @Nonnull MeasuredText measuredText, @Nonnull LineWidth lineWidthLimits,
                       @Nonnull TabStops tabStops) {
        mTextBuf = textBuf;
        mMeasuredText = measuredText;
        mLineWidthLimits = lineWidthLimits;
        mTabStops = tabStops;
        mLineWidthLimit = lineWidthLimits.getAt(0);
    }

    /**
     * Break paragraph into lines.
     * <p>
     * The result is filled to out param.
     *
     * @param measuredText a result of the text measurement
     * @param constraints  constraints for a single paragraph
     * @param indents      the supplied array provides the total amount of indentation per
     *                     line, in pixel. This amount is the sum of both left and right
     *                     indentations. For lines past the last element in the array, the
     *                     indentation amount of the last element is used.
     * @param lineNumber   a line number (index offset) of this paragraph
     * @return the result of line break
     */
    @Nonnull
    public static Result computeLineBreaks(@Nullable MeasuredText measuredText,
                                           @Nonnull ParagraphConstraints constraints,
                                           @Nullable int[] indents, int lineNumber) {
        if (measuredText == null || measuredText.getTextBuf().length == 0) {
            return new Result();
        }
        DefaultLineWidth lineWidth = new DefaultLineWidth(constraints.mFirstWidth, constraints.mWidth, indents,
                lineNumber);
        TabStops tabStops = new TabStops(constraints.mVariableTabStops, constraints.mDefaultTabStop);
        LineBreaker breaker = new LineBreaker(measuredText.getTextBuf(), measuredText, lineWidth, tabStops);
        breaker.process();
        return breaker.getResult();
    }

    private void process() {
        BreakIterator breaker = sBreaker;
        CharacterIterator iterator = new CharArrayIterator(mTextBuf);

        Locale locale = null;
        int nextBoundary = 0;
        for (var run : mMeasuredText.getRuns()) {

            Locale newLocale = run.getLocale();
            if (locale != newLocale) {
                breaker = BreakIterator.getLineInstance(locale);
                breaker.setText(iterator);
                nextBoundary = breaker.following(run.mStart);
                locale = newLocale;
            }

            for (int i = run.mStart; i < run.mEnd; i++) {
                updateLineWidth(mTextBuf[i], mMeasuredText.getAdvance(i));

                if (i + 1 == nextBoundary) {
                    if (run.canBreak() || nextBoundary == run.mEnd) {
                        processLineBreak(i + 1);
                    }
                    nextBoundary = breaker.next();
                    if (nextBoundary == BreakIterator.DONE) {
                        nextBoundary = mTextBuf.length;
                    }
                }
            }
        }
    }

    private void processLineBreak(int offset) {
        while (mLineWidth > mLineWidthLimit) {
            int start = getPrevLineBreakOffset();
            // The word in the new line may still be too long for the line limit.
            // Try general line break first, otherwise try grapheme boundary or out of the line width
            if (!tryLineBreak() && doLineBreakWithGraphemeBounds(start, offset)) {
                return;
            }
        }


        if (mPrevBoundaryOffset == NOWHERE) {
            mPrevBoundaryOffset = offset;
            mLineWidthAtPrevBoundary = mLineWidth;
            mCharsAdvanceAtPrevBoundary = mCharsAdvance;
        }
    }

    // general line break, use ICU line break iterator, not word breaker
    private boolean tryLineBreak() {
        if (mPrevBoundaryOffset == NOWHERE) {
            return false;
        }

        breakLineAt(mPrevBoundaryOffset, mLineWidthAtPrevBoundary,
                mLineWidth - mCharsAdvanceAtPrevBoundary,
                mCharsAdvance - mCharsAdvanceAtPrevBoundary);
        return true;
    }

    //TODO: Respect trailing line end spaces.
    private boolean doLineBreakWithGraphemeBounds(int start, int end) {
        float width = mMeasuredText.getAdvance(start);

        // Starting from + 1 since at least one character needs to be assigned to a line.
        for (int i = start + 1; i < end; i++) {
            final float w = mMeasuredText.getAdvance(i);
            if (w == 0) {
                // w == 0 means here is not a grapheme bounds. Don't break here.
                continue;
            }
            if (width + w > mLineWidthLimit) {
                // Okay, here is the longest position.
                breakLineAt(i, width, mLineWidth - width, mCharsAdvance - width);
                // This method only breaks at the first longest offset, since we may want to hyphenate
                // the rest of the word.
                return false;
            } else {
                width += w;
            }
        }

        // Reaching here means even one character (or cluster) doesn't fit the line.
        // Give up and break at the end of this range.
        breakLineAt(end, mLineWidth, 0, 0);
        return true;
    }

    // Add a break point
    private void breakLineAt(int offset, float lineWidth, float remainingNextLineWidth,
                             float remainingNextCharsAdvance) {
        mBreakPoints.add(new BreakPoint(offset, lineWidth));

        mLineWidthLimit = mLineWidthLimits.getAt(++mLineNum);
        mLineWidth = remainingNextLineWidth;
        mCharsAdvance = remainingNextCharsAdvance;
        mPrevBoundaryOffset = NOWHERE;
        mLineWidthAtPrevBoundary = 0;
        mCharsAdvanceAtPrevBoundary = 0;
    }

    private void updateLineWidth(char c, float adv) {
        // U+0009 Horizontal tabulation char
        if (c == '\u0009') {
            mCharsAdvance = mTabStops.nextTab(mCharsAdvance);
            mLineWidth = mCharsAdvance;
        } else {
            mCharsAdvance += adv;
            if (!isLineEndSpace(c)) {
                mLineWidth = mCharsAdvance;
            }
        }
    }

    private int getPrevLineBreakOffset() {
        return mBreakPoints.isEmpty() ? 0 : mBreakPoints.get(mBreakPoints.size() - 1).mOffset;
    }

    @Nonnull
    private Result getResult() {
        int prevBreakOffset = 0;
        final int size = mBreakPoints.size();
        final int[] ascents = new int[size];
        final int[] descents = new int[size];
        FontMetricsInt fm = new FontMetricsInt();
        for (int i = 0; i < size; i++) {
            BreakPoint breakPoint = mBreakPoints.get(i);
            for (int j = prevBreakOffset; j < breakPoint.mOffset; j++)
                if (mTextBuf[j] == '\u0009') {
                    breakPoint.mHasTabChar = true;
                    break;
                }
            fm.reset();
            mMeasuredText.getExtent(prevBreakOffset, breakPoint.mOffset, fm);
            ascents[i] = fm.mAscent;
            descents[i] = fm.mDescent;
            prevBreakOffset = breakPoint.mOffset;
        }
        return new Result(mBreakPoints.toArray(new BreakPoint[0]), ascents, descents);
    }

    // 24 bytes (with compressed oops)
    private static final class BreakPoint {

        private final int mOffset;
        private final float mLineWidth;
        private boolean mHasTabChar = false;

        public BreakPoint(int offset, float lineWidth) {
            mOffset = offset;
            mLineWidth = lineWidth;
        }
    }

    /**
     * Line breaking constraints for single paragraph.
     */
    public static class ParagraphConstraints {

        private float mWidth = 0;
        private float mFirstWidth = 0;
        @Nullable
        private float[] mVariableTabStops = null;
        private float mDefaultTabStop = 0;

        public ParagraphConstraints() {
        }

        /**
         * Set width for this paragraph.
         *
         * @see #getWidth()
         */
        public void setWidth(float width) {
            mWidth = width;
        }

        /**
         * Set indent for this paragraph.
         *
         * @param firstWidth the line width of the starting of the paragraph
         * @see #getFirstWidth()
         */
        public void setIndent(float firstWidth) {
            mFirstWidth = firstWidth;
        }

        /**
         * Set tab stops for this paragraph.
         *
         * @param tabStops       the array of pixels of tap stopping position
         * @param defaultTabStop pixels of the default tab stopping position
         * @see #getTabStops()
         * @see #getDefaultTabStop()
         */
        public void setTabStops(@Nullable float[] tabStops, float defaultTabStop) {
            mVariableTabStops = tabStops;
            mDefaultTabStop = defaultTabStop;
        }

        /**
         * Return the width for this paragraph in pixels.
         *
         * @see #setWidth(float)
         */
        public float getWidth() {
            return mWidth;
        }

        /**
         * Return the first line's width for this paragraph in pixel.
         *
         * @see #setIndent(float)
         */
        public float getFirstWidth() {
            return mFirstWidth;
        }

        /**
         * Returns the array of tab stops in pixels.
         *
         * @see #setTabStops
         */
        @Nullable
        public float[] getTabStops() {
            return mVariableTabStops;
        }

        /**
         * Returns the default tab stops in pixels.
         *
         * @see #setTabStops
         */
        public float getDefaultTabStop() {
            return mDefaultTabStop;
        }
    }

    /**
     * The methods in this interface may be called several times. The implementation
     * must return the same value for the same input.
     */
    public interface LineWidth {

        /**
         * Find out the width for the line. This must not return negative values.
         *
         * @param line the line index
         * @return the line width in pixels
         */
        default float getAt(int line) {
            return 0;
        }

        /**
         * Find out the minimum line width. This mut not return negative values.
         *
         * @return the minimum line width in pixels
         */
        default float getMin() {
            return 0;
        }
    }

    private static class DefaultLineWidth implements LineWidth {

        // for the first line
        private final float mFirstWidth;
        // for rest lines
        private final float mRestWidth;
        @Nullable
        private final int[] mIndents;
        // the offset in mIndents
        private final int mOffset;

        public DefaultLineWidth(float firstWidth, float restWidth, @Nullable int[] indents, int offset) {
            mFirstWidth = firstWidth;
            mRestWidth = restWidth;
            mIndents = indents;
            mOffset = offset;
        }

        @Override
        public float getAt(int line) {
            final float width = line < 1 ? mFirstWidth : mRestWidth;
            return Math.max(0.0f, width - getIndent(mIndents, line));
        }

        @Override
        public float getMin() {
            float minWidth = Math.min(getAt(0), getAt(1));
            if (mIndents != null) {
                final int end = mIndents.length - mOffset;
                for (int line = 1; line < end; line++)
                    minWidth = Math.min(minWidth, getAt(line));
            }
            return minWidth;
        }

        private float getIndent(@Nullable int[] indents, int line) {
            if (indents == null || indents.length == 0)
                return 0;
            final int index = line + mOffset;
            if (index < indents.length)
                return indents[index];
            else
                return indents[indents.length - 1];
        }
    }

    /**
     * Holds the result of the line breaking algorithm.
     *
     * @see LineBreaker#computeLineBreaks
     */
    public static class Result {

        private static final BreakPoint[] EMPTY_ARRAY = {};

        @Nonnull
        private final BreakPoint[] mBreakPoints;
        private final int[] mAscents;
        private final int[] mDescents;

        private Result() {
            mBreakPoints = EMPTY_ARRAY;
            mAscents = IntArrays.EMPTY_ARRAY;
            mDescents = IntArrays.EMPTY_ARRAY;
        }

        private Result(@Nonnull BreakPoint[] breakPoints, int[] ascents, int[] descents) {
            mBreakPoints = breakPoints;
            mAscents = ascents;
            mDescents = descents;
        }

        /**
         * Returns the number of lines in the paragraph.
         *
         * @return number of lines
         */
        public int getLineCount() {
            return mBreakPoints.length;
        }

        /**
         * Returns character offset of the break for a given line.
         *
         * @param lineIndex an index of the line.
         * @return the break offset.
         */
        public int getLineBreakOffset(int lineIndex) {
            return mBreakPoints[lineIndex].mOffset;
        }

        /**
         * Returns width of a given line in pixels.
         *
         * @param lineIndex an index of the line.
         * @return width of the line in pixels
         */
        public float getLineWidth(int lineIndex) {
            return mBreakPoints[lineIndex].mLineWidth;
        }

        /**
         * Returns font ascent of the line in pixels.
         *
         * @param lineIndex an index of the line.
         * @return font ascent of the line in pixels.
         */
        public float getLineAscent(int lineIndex) {
            return mAscents[lineIndex];
        }

        /**
         * Returns font descent of the line in pixels.
         *
         * @param lineIndex an index of the line.
         * @return font descent of the line in pixels.
         */
        public float getLineDescent(int lineIndex) {
            return mDescents[lineIndex];
        }

        /**
         * Returns true if the line has a TAB character.
         *
         * @param lineIndex an index of the line.
         * @return true if the line has a TAB character
         */
        public boolean hasLineTab(int lineIndex) {
            return mBreakPoints[lineIndex].mHasTabChar;
        }
    }
}
