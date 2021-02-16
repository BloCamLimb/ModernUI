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

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StaticLayout extends TextLayout {

    private static final int DEFAULT_MAX_LINE_HEIGHT = -1;

    /**
     * Builder for static layouts. The builder is the preferred pattern for constructing
     * StaticLayout objects and should be preferred over the constructors, particularly to access
     * newer features. To build a static layout, first call {@link #obtain} with the required
     * arguments (text, paint, and width), then call setters for optional parameters, and finally
     * {@link #build} to build the StaticLayout object. Parameters not explicitly set will get
     * default values.
     */
    public final static class Builder {

        private static final Pool<Builder> sPool = Pools.concurrent(3);

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
        public static Builder obtain(@Nonnull CharSequence source, int start, int end,
                                     @Nonnull TextPaint paint, int width) {
            Builder b = sPool.acquire();
            if (b == null)
                b = new Builder();

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
         * This method should be called after the layout is finished getting constructed and the
         * builder needs to be cleaned up and returned to the pool.
         */
        private static void recycle(@Nonnull Builder b) {
            b.release();
            sPool.release(b);
        }

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
            Builder.recycle(this);
            return result;
        }
    }


    private int mLineCount;
    private int mTopPadding, mBottomPadding;
    private int mColumns;
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

    private int[] mLines;
    private Directions[] mLineDirections;
    private int mMaximumVisibleLineCount = Integer.MAX_VALUE;

    @Nullable
    private int[] mLeftIndents;
    @Nullable
    private int[] mRightIndents;

    private StaticLayout(Builder b) {
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

        int v = 0;

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
                /*ascents[i] = res.getLineAscent(i);
                descents[i] = res.getLineDescent(i);*/
                hasTabs[i] = res.hasLineTab(i);
            }

            // here is the offset of the starting character of the line we are currently
            // measuring
            int here = paraStart;
        }
    }
}
