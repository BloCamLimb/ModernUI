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

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

import com.ibm.icu.text.BreakIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides automatic line breaking for a <em>single</em> paragraph.
 */
public class LineBreaker {

    private static final int NOWHERE = 0xFFFFFFFF;

    private static BreakIterator sBreaker = BreakIterator.getLineInstance();

    /**
     * Break paragraph into lines.
     * <p>
     * The result is filled to out param.
     *
     * @param measuredPara a result of the text measurement
     * @param constraints  constraints for a single paragraph
     * @param indents      the supplied array provides the total amount of indentation per
     *                     line, in pixel. This amount is the sum of both left and right
     *                     indentations. For lines past the last element in the array, the
     *                     indentation amount of the last element is used.
     * @param lineNumber   a line number (offset) of this paragraph
     * @return the result of line break
     */
    @Nonnull
    public static Result computeLineBreaks(@Nonnull MeasuredText measuredPara, @Nonnull ParagraphConstraints constraints,
                                           @Nullable int[] indents, int lineNumber) {
        if (measuredPara.getTextBuf().length == 0)
            return new Result();
        final float[] floatIndents;
        if (indents == null)
            floatIndents = null;
        else {
            floatIndents = new float[indents.length];
            for (int i = 0; i < indents.length; i++)
                floatIndents[i] = indents[i];
        }
        DefaultLineWidth lineWidth = new DefaultLineWidth(constraints.mFirstWidth, constraints.mWidth, floatIndents, lineNumber);
        TabStops tabStops = new TabStops(constraints.mVariableTabStops, constraints.mDefaultTabStop);
        return new LineBreaker(measuredPara.getTextBuf(), measuredPara, lineWidth, tabStops).getResult();
    }

    // This function determines whether a character is a space that disappears at end of line.
    // It is the Unicode set: [[:General_Category=Space_Separator:]-[:Line_Break=Glue:]], plus '\n'.
    // Note: all such characters are in the BMP, so it's ok to use code units for this.
    public static boolean isLineEndSpace(char c) {
        return c == '\n' || c == ' '                           // SPACE
                || c == 0x1680                                  // OGHAM SPACE MARK
                || (0x2000 <= c && c <= 0x200A && c != 0x2007)  // EN QUAD, EM QUAD, EN SPACE, EM SPACE,
                // THREE-PER-EM SPACE, FOUR-PER-EM SPACE,
                // SIX-PER-EM SPACE, PUNCTUATION SPACE,
                // THIN SPACE, HAIR SPACE
                || c == 0x205F  // MEDIUM MATHEMATICAL SPACE
                || c == 0x3000;
    }

    // change breaker locale
    static void setLocale(Locale locale) {
        sBreaker = BreakIterator.getLineInstance(locale);
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
    private boolean mHasTabChar = false;

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
        process();
    }

    private void process() {
        BreakIterator breaker = sBreaker;
        breaker.setText(new CharArrayIterator(mTextBuf));

        int nextBoundary = NOWHERE;
        for (MeasuredText.Run run : mMeasuredText.mRuns) {
            if (nextBoundary == NOWHERE)
                nextBoundary = breaker.following(run.mStart);

            for (int i = run.mStart; i < run.mEnd; i++) {
                updateLineWidth(mTextBuf[i], mMeasuredText.mAdvances[i]);

                if (i + 1 == nextBoundary) {
                    if (run.canBreak() || nextBoundary == run.mEnd) {
                        processLineBreak(i + 1);
                    }
                    nextBoundary = breaker.next();
                    if (nextBoundary == BreakIterator.DONE)
                        nextBoundary = run.mEnd;
                }
            }
        }
    }

    private void processLineBreak(int offset) {
        while (mLineWidth > mLineWidthLimit) {
            int start = getPrevLineBreakOffset();
            // The word in the new line may still be too long for the line limit.
            // Try general line break first, otherwise try grapheme boundary or out of the line width
            if (!tryLineBreak() && doLineBreakWithGraphemeBounds(start, offset))
                return;
        }


        if (mPrevBoundaryOffset == NOWHERE) {
            mPrevBoundaryOffset = offset;
            mLineWidthAtPrevBoundary = mLineWidth;
            mCharsAdvanceAtPrevBoundary = mCharsAdvance;
        }
    }

    // general line break, use ICU line break iterator, not word breaker
    private boolean tryLineBreak() {
        if (mPrevBoundaryOffset == NOWHERE)
            return false;

        breakLineAt(mPrevBoundaryOffset, mLineWidthAtPrevBoundary,
                mLineWidth - mCharsAdvanceAtPrevBoundary,
                mCharsAdvance - mCharsAdvanceAtPrevBoundary);
        return true;
    }

    //TODO: Respect trailing line end spaces.
    private boolean doLineBreakWithGraphemeBounds(int start, int end) {
        float width = mMeasuredText.mAdvances[start];

        // Starting from + 1 since at least one character needs to be assigned to a line.
        for (int i = start + 1; i < end; i++) {
            final float w = mMeasuredText.mAdvances[i];
            if (w == 0)
                continue; // w == 0 means here is not a grapheme bounds. Don't break here.
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
    private void breakLineAt(int offset, float lineWidth, float remainingNextLineWidth, float remainingNextCharsAdvance) {
        mBreakPoints.add(new BreakPoint(offset, lineWidth, mHasTabChar));

        mLineWidthLimit = mLineWidthLimits.getAt(++mLineNum);
        mLineWidth = remainingNextLineWidth;
        mCharsAdvance = remainingNextCharsAdvance;
        mPrevBoundaryOffset = NOWHERE;
        mLineWidthAtPrevBoundary = 0;
        mCharsAdvanceAtPrevBoundary = 0;
        mHasTabChar = false;
    }

    private void updateLineWidth(char c, float adv) {
        // U+0009 Horizontal tabulation char
        if (c == '\u0009') {
            mCharsAdvance = mTabStops.nextTab(mCharsAdvance);
            mLineWidth = mCharsAdvance;
            mHasTabChar = true;
        } else {
            mCharsAdvance += adv;
            if (!isLineEndSpace(c)) {
                mLineWidth = mCharsAdvance;
            }
        }
    }

    private int getPrevLineBreakOffset() {
        return mBreakPoints.isEmpty() ? 0 :
                mBreakPoints.get(mBreakPoints.size() - 1).mCpxOffset & BreakPoint.OFFSET_MASK;
    }

    @Nonnull
    private Result getResult() {
        return new Result();
    }

    private static class BreakPoint {

        private static final int OFFSET_MASK = 0x7fffffff;
        private static final int HAS_TAB_MASK = 0x80000000;

        private final int mCpxOffset;
        private final float mLineWidth;

        public BreakPoint(int offset, float lineWidth, boolean hasTabChar) {
            if (hasTabChar)
                offset |= HAS_TAB_MASK;
            mCpxOffset = offset;
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
         * @param line the line number
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

    public static class DefaultLineWidth implements LineWidth {

        // for the first line
        private final float mFirstWidth;
        // for rest lines
        private final float mRestWidth;
        @Nullable
        private final float[] mIndents;
        // the offset in mIndents
        private final int mOffset;

        public DefaultLineWidth(float firstWidth, float restWidth, @Nullable float[] indents, int offset) {
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

        private float getIndent(@Nullable float[] indents, int line) {
            if (indents == null || indents.length == 0)
                return 0;
            final int index = line + mOffset;
            if (index < indents.length)
                return indents[index];
            else
                return indents[indents.length - 1];
        }
    }

    public static class TabStops {

        @Nullable
        private final float[] mStops;
        private final float mTabWidth;

        public TabStops(@Nullable float[] stops, float tabWidth) {
            mStops = stops;
            mTabWidth = tabWidth;
        }

        // return next tab width
        public float nextTab(float currWidth) {
            if (mStops != null)
                for (float stop : mStops)
                    if (stop > currWidth)
                        return stop;
            return (int) (currWidth / mTabWidth + 1) * mTabWidth;
        }
    }

    public static class Result {

    }
}
