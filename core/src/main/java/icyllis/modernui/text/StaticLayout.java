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

import icyllis.modernui.graphics.font.FontMetricsInt;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StaticLayout extends TextLayout {

    private static final int DEFAULT_MAX_LINE_HEIGHT = -1;

    private static final Pool<Builder> sPool = Pools.concurrent(1);

    /**
     * Obtain a builder for constructing StaticLayout objects.
     *
     * @param source The text to be laid out, optionally with spans
     * @param start  The index of the start of the text
     * @param end    The index + 1 of the end of the text
     * @param paint  The base paint used for layout
     * @param width  The width in pixels
     * @return a builder object used for constructing the StaticLayout
     */
    @Nonnull
    public static Builder builder(@Nonnull CharSequence source, int start, int end,
                                  @Nonnull TextPaint paint, int width) {
        Builder b = sPool.acquire();
        if (b == null) {
            b = new Builder();
        }

        b.mText = source;
        b.mStart = start;
        b.mEnd = end;
        b.mPaint = paint;
        b.mWidth = width;
        b.mAlignment = Alignment.ALIGN_NORMAL;
        b.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        b.mFallbackLineSpacing = true; // default true
        return b;
    }

    /**
     * Builder for static layouts. The builder is the preferred pattern for constructing
     * StaticLayout objects and should be preferred over the constructors, particularly to access
     * newer features. To build a static layout, first call {@link #builder} with the required
     * arguments (text, paint, and width), then call setters for optional parameters, and finally
     * {@link #build} to build the StaticLayout object. Parameters not explicitly set will get
     * default values.
     */
    public final static class Builder {

        // cached instance
        private final FontMetricsInt mFontMetricsInt = new FontMetricsInt();

        private CharSequence mText;
        private int mStart;
        private int mEnd;
        private TextPaint mPaint;
        private int mWidth;
        private Alignment mAlignment;
        private TextDirectionHeuristic mTextDir;
        private boolean mFallbackLineSpacing;

        private Builder() {
        }

        /**
         * This method should be called after the layout is finished getting constructed and the
         * builder needs to be cleaned up and returned to the pool.
         */
        private void recycle() {
            release();
            sPool.release(this);
        }

        // release heavy buffers
        void release() {
            mText = null;
            mPaint = null;
        }

        /**
         * Set the text. Only useful when re-using the builder, which is done for
         * the internal implementation of {@link DynamicLayout} but not as part
         * of normal {@link StaticLayout} usage.
         *
         * @param source The text to be laid out, optionally with spans
         * @param start  The index of the start of the text
         * @param end    The index + 1 of the end of the text
         * @return this builder, useful for chaining
         */
        @Nonnull
        Builder setText(@Nonnull CharSequence source, int start, int end) {
            mText = source;
            mStart = start;
            mEnd = end;
            return this;
        }

        /**
         * Set the paint. Internal for reuse cases only.
         *
         * @param paint The base paint used for layout
         * @return this builder, useful for chaining
         */
        @Nonnull
        Builder setPaint(@Nonnull TextPaint paint) {
            mPaint = paint;
            return this;
        }

        /**
         * Set the width. Internal for reuse cases only.
         *
         * @param width The width in pixels
         * @return this builder, useful for chaining
         */
        @Nonnull
        Builder setWidth(int width) {
            mWidth = width;
            /*if (mEllipsize == null) {
                mEllipsizedWidth = width;
            }*/
            return this;
        }

        /**
         * Set the alignment. The default is {@link TextLayout.Alignment#ALIGN_NORMAL}.
         *
         * @param alignment Alignment for the resulting {@link StaticLayout}
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setAlignment(@Nonnull Alignment alignment) {
            mAlignment = alignment;
            return this;
        }

        /**
         * Set the text direction heuristic. The text direction heuristic is used to
         * resolve text direction per-paragraph based on the input text. The default is
         * {@link TextDirectionHeuristics#FIRSTSTRONG_LTR}.
         *
         * @param textDir text direction heuristic for resolving bidi behavior.
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setTextDirection(@Nonnull TextDirectionHeuristic textDir) {
            mTextDir = textDir;
            return this;
        }

        /**
         * Set whether to respect the ascent and descent of the fallback fonts that are used in
         * displaying the text (which is needed to avoid text from consecutive lines running into
         * each other). If set, fallback fonts that end up getting used can increase the ascent
         * and descent of the lines that they are used on.
         * <p>
         * The default is {@code true}. It is required to be true if text could be in
         * languages like Burmese or Tibetan where text is typically much taller or deeper than
         * Latin text.
         *
         * @param useLineSpacingFromFallbacks whether to expand line spacing based on fallback fonts
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks) {
            mFallbackLineSpacing = useLineSpacingFromFallbacks;
            return this;
        }

        /**
         * Build the {@link StaticLayout} after options have been set.
         * <p>
         * Note: the builder object must not be reused in any way after calling this
         * method. Setting parameters after calling this method, or calling it a second
         * time on the same builder object, will likely lead to unexpected results.
         *
         * @return the newly constructed {@link StaticLayout} object
         */
        @Nonnull
        public StaticLayout build() {
            StaticLayout result = new StaticLayout(this);
            recycle();
            return result;
        }
    }

    private static final int COLUMNS_NORMAL = 5;
    private static final int COLUMNS_ELLIPSIZE = 7;

    private static final int START = 0;
    private static final int DIR = START;
    private static final int TAB = START;
    private static final int TOP = 1;
    private static final int DESCENT = 2;
    private static final int EXTRA = 3;
    private static final int HYPHEN = 4;
    private static final int ELLIPSIS_START = 5;
    private static final int ELLIPSIS_COUNT = 6;

    // 29 bits
    private static final int START_MASK = 0x1FFFFFFF;
    // 31 bit
    private static final int DIR_SHIFT = 30;
    // 30 bit
    private static final int TAB_MASK = 0x20000000;


    private int mLineCount;
    private int mTopPadding, mBottomPadding;
    private final int mColumns;
    private int mEllipsizedWidth;

    /**
     * Keeps track if ellipsize is applied to the text.
     */
    private boolean mEllipsized;

    /**
     * If maxLines is set, ellipsize is not set, and the actual line count of text is greater than
     * or equal to maxLine, this variable holds the ideal visual height of the maxLine'th line
     * starting from the top of the layout. If maxLines is not set its value will be -1.
     * <p>
     * The value is the same as getLineTop(maxLines) for ellipsized version where structurally no
     * more than maxLines is contained.
     */
    private int mMaxLineHeight = DEFAULT_MAX_LINE_HEIGHT;

    // line data, see mColumns
    private int[] mLines;
    private Directions[] mLineDirections;
    private int mMaximumVisibleLineCount = Integer.MAX_VALUE;

    @Nullable
    private int[] mLeftIndents;
    @Nullable
    private int[] mRightIndents;

    // Used by DynamicLayout
    StaticLayout(@Nullable CharSequence text) {
        super(text);
        mColumns = COLUMNS_ELLIPSIZE;
    }

    private StaticLayout(@Nonnull Builder b) {
        super(b.mText);
        mColumns = COLUMNS_NORMAL;
        generate(b);
    }

    void generate(@Nonnull Builder b) {
        final CharSequence source = b.mText;
        final int bufStart = b.mStart;
        final int bufEnd = b.mEnd;
        final TextPaint paint = b.mPaint;
        final int outerWidth = b.mWidth;
        final TextDirectionHeuristic textDir = b.mTextDir;
        final boolean fallbackLineSpacing = b.mFallbackLineSpacing;

        int lineBreakCapacity = 0;
        int[] breaks = null;
        float[] lineWidths = null;
        float[] ascents = null;
        float[] descents = null;
        boolean[] hasTabs = null;

        mLineCount = 0;

        // current height in pixels
        int v = 0;

        FontMetricsInt fm = b.mFontMetricsInt;

        LineBreaker.ParagraphConstraints constraints =
                new LineBreaker.ParagraphConstraints();

        PrecomputedText.ParagraphInfo[] paragraphInfo;
        final Spanned spanned = (source instanceof Spanned) ? (Spanned) source : null;

        paragraphInfo = PrecomputedText.createMeasuredParagraphs(paint, source, bufStart, bufEnd, textDir);

        for (int paraIndex = 0, paraStart = 0, paraEnd;
             paraIndex < paragraphInfo.length;
             paraIndex++, paraStart = paraEnd) {
            paraEnd = paragraphInfo[paraIndex].paragraphEnd;

            int firstWidth = outerWidth;
            int restWidth = outerWidth;

            if (spanned != null) {

            }

            final MeasuredParagraph measuredPara = paragraphInfo[paraIndex].measured;
            final int[] spanEndCache = measuredPara.getSpanEndCache().elements();
            final int[] fmCache = measuredPara.getFontMetrics().elements();

            constraints.setWidth(restWidth);
            constraints.setIndent(firstWidth);
            constraints.setTabStops(null, 20);

            LineBreaker.Result res = LineBreaker.computeLineBreaks(
                    measuredPara.getMeasuredText(), constraints, null, 0);

            final int remainingLineCount = mMaximumVisibleLineCount - mLineCount;

            int breakCount = res.getLineCount();
            if (breakCount > lineBreakCapacity) {
                lineBreakCapacity = breakCount;
                breaks = new int[lineBreakCapacity];
                lineWidths = new float[lineBreakCapacity];
                ascents = new float[lineBreakCapacity];
                descents = new float[lineBreakCapacity];
                hasTabs = new boolean[lineBreakCapacity];
            }

            for (int i = 0; i < breakCount; ++i) {
                breaks[i] = res.getLineBreakOffset(i);
                lineWidths[i] = res.getLineWidth(i);
                ascents[i] = res.getLineAscent(i);
                descents[i] = res.getLineDescent(i);
                hasTabs[i] = res.hasLineTab(i);
            }

            //TODO
            boolean ellipsisMayBeApplied = false;

            //noinspection ConstantConditions
            if (0 < remainingLineCount && remainingLineCount < breakCount
                    && ellipsisMayBeApplied) {
                // Calculate width
                float width = 0;
                boolean hasTab = false;  // XXX May need to also have starting hyphen edit
                for (int i = remainingLineCount - 1; i < breakCount; i++) {
                    if (i == breakCount - 1) {
                        width += lineWidths[i];
                    } else {
                        for (int j = (i == 0 ? 0 : breaks[i - 1]); j < breaks[i]; j++) {
                            width += measuredPara.getMeasuredText().mAdvances[j];
                        }
                    }
                    hasTab |= hasTabs[i];
                }
                // Treat the last line and overflowed lines as a single line.
                breaks[remainingLineCount - 1] = breaks[breakCount - 1];
                lineWidths[remainingLineCount - 1] = width;
                hasTabs[remainingLineCount - 1] = hasTab;

                breakCount = remainingLineCount;
            }

            // here is the offset of the starting character of the line we are currently
            // measuring
            int here = paraStart;

            int fmAscent = 0, fmDescent = 0;
            int breakIndex = 0;
            for (int spanStart = paraStart, spanEnd, spanIndex = 0;
                 spanStart < paraEnd;
                 spanStart = spanEnd, spanIndex++) {

                spanEnd = spanEndCache[spanIndex];

                fm.mAscent = fmCache[spanIndex * 2];
                fm.mDescent = fmCache[spanIndex * 2 + 1];

                fmAscent = Math.max(fmAscent, fm.mAscent);
                fmDescent = Math.max(fmDescent, fm.mDescent);

                // skip breaks ending before current span range
                while (breakIndex < breakCount && paraStart + breaks[breakIndex] < spanStart) {
                    breakIndex++;
                }

                while (breakIndex < breakCount && paraStart + breaks[breakIndex] <= spanEnd) {
                    int endPos = paraStart + breaks[breakIndex];

                    boolean moreChars = (endPos < bufEnd);

                    final int ascent = fallbackLineSpacing
                            ? Math.max(fmAscent, Math.round(ascents[breakIndex]))
                            : fmAscent;
                    final int descent = fallbackLineSpacing
                            ? Math.max(fmDescent, Math.round(descents[breakIndex]))
                            : fmDescent;

                    v = out(v);

                    if (endPos < spanEnd) {
                        // preserve metrics for current span
                        fmAscent = fm.mAscent;
                        fmDescent = fm.mDescent;
                    } else {
                        fmAscent = fmDescent = 0;
                    }

                    here = endPos;
                    breakIndex++;

                    if (mLineCount >= mMaximumVisibleLineCount && mEllipsized) {
                        return;
                    }
                }
            }

            if (paraEnd == bufEnd) {
                break;
            }
        }


    }

    private int out(int v) {
        final int j = mLineCount;
        final int off = j * mColumns;
        final int want = off + mColumns + TOP;

        if (want >= mLines.length) {
            mLines = IntArrays.forceCapacity(mLines, want, mLines.length);
        }

        if (j >= mLineDirections.length) {
            mLineDirections = ObjectArrays.forceCapacity(mLineDirections, j, mLineDirections.length);
        }

        int[] lines = mLines;

        return v;
    }
}
