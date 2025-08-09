/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.text.*;
import icyllis.modernui.text.style.*;
import icyllis.modernui.text.style.LeadingMarginSpan.LeadingMarginSpan2;
import icyllis.modernui.util.GrowingArrayUtils;
import icyllis.modernui.util.Log;
import icyllis.modernui.util.Pools;
import icyllis.modernui.widget.TextView;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.*;

/**
 * StaticLayout is a Layout for text that will not be edited after it
 * is laid out.  Use {@link DynamicLayout} for text that may change.
 * <p>This is used by widgets to control text layout. You should not need
 * to use this class directly unless you are implementing your own widget
 * or custom display object.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class StaticLayout extends Layout {

    public static final Marker MARKER = MarkerFactory.getMarker("StaticLayout");

    private static final Pools.Pool<Builder> sPool = Pools.newSynchronizedPool(2);

    /**
     * Obtain a builder for constructing StaticLayout objects.
     *
     * @param source The text to be laid out, optionally with spans
     * @param start  The start index (inclusive) of the text
     * @param end    The end index (exclusive) of the text
     * @param paint  The base (default) paint used for layout
     * @param width  The width in pixels for each line
     * @return a builder object used for constructing the StaticLayout
     */
    @NonNull
    public static Builder builder(@NonNull CharSequence source, @IntRange(from = 0) int start,
                                  @IntRange(from = 0) int end, @NonNull TextPaint paint,
                                  @IntRange(from = 0) int width) {
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
        b.mSpacingMult = 1.0f;
        b.mSpacingAdd = 0.0f;
        b.mIncludePad = true;
        b.mFallbackLineSpacing = true; // default true
        b.mEllipsizedWidth = width;
        b.mEllipsize = null;
        b.mMaxLines = Integer.MAX_VALUE;
        b.mLineBreakConfig = LineBreakConfig.NONE;
        return b;
    }

    /**
     * Builder for static layouts. The builder is the preferred pattern for constructing
     * StaticLayout objects and should be preferred over the constructors, particularly to access
     * newer features. To build a static layout, first call {@link #builder(CharSequence, int, int, TextPaint, int)}
     * with the required arguments (text, paint, and width), then call setters for optional parameters, and finally
     * {@link #build()} to build the StaticLayout object. Parameters not explicitly set will get
     * default values.
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        // cached instance
        private final FontMetricsInt mFontMetricsInt = new FontMetricsInt();

        private CharSequence mText;
        private int mStart;
        private int mEnd;
        private TextPaint mPaint;
        private int mWidth;
        private Alignment mAlignment;
        private TextDirectionHeuristic mTextDir;
        private float mSpacingMult;
        private float mSpacingAdd;
        private boolean mIncludePad;
        private boolean mFallbackLineSpacing;
        private int mEllipsizedWidth;
        @Nullable
        private TextUtils.TruncateAt mEllipsize;
        private int mMaxLines;
        @Nullable
        private int[] mLeftIndents;
        @Nullable
        private int[] mRightIndents;
        private boolean mAddLastLineLineSpacing;
        private LineBreakConfig mLineBreakConfig = LineBreakConfig.NONE;

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
            mLeftIndents = null;
            mRightIndents = null;
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
        @NonNull
        Builder setText(@NonNull CharSequence source, int start, int end) {
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
        @NonNull
        Builder setPaint(@NonNull TextPaint paint) {
            mPaint = paint;
            return this;
        }

        /**
         * Set the width. Internal for reuse cases only.
         *
         * @param width The width in pixels
         * @return this builder, useful for chaining
         */
        @NonNull
        Builder setWidth(int width) {
            mWidth = width;
            if (mEllipsize == null) {
                mEllipsizedWidth = width;
            }
            return this;
        }

        /**
         * Set the alignment. The default is {@link Layout.Alignment#ALIGN_NORMAL}.
         *
         * @param alignment Alignment for the resulting {@link StaticLayout}
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setAlignment(@NonNull Alignment alignment) {
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
        @NonNull
        public Builder setTextDirection(@NonNull TextDirectionHeuristic textDir) {
            mTextDir = textDir;
            return this;
        }

        /**
         * Set line spacing parameters. Each line will have its line spacing multiplied by
         * {@code spacingMult} and then increased by {@code spacingAdd}. The default is 0.0 for
         * {@code spacingAdd} and 1.0 for {@code spacingMult}.
         *
         * @param spacingAdd the amount of line spacing addition
         * @param spacingMult the line spacing multiplier
         * @return this builder, useful for chaining
         * @see TextView#setLineSpacing
         */
        @NonNull
        public Builder setLineSpacing(float spacingAdd, @FloatRange(from = 0.0) float spacingMult) {
            mSpacingAdd = spacingAdd;
            mSpacingMult = spacingMult;
            return this;
        }

        /**
         * Set whether to include extra space beyond font ascent and descent (which is
         * needed to avoid clipping in some languages, such as Arabic and Kannada). The
         * default is {@code true}.
         *
         * @param includePad whether to include padding
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setIncludePad(boolean includePad) {
            mIncludePad = includePad;
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
         * @param fallbackLineSpacing whether to expand line spacing based on fallback fonts
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setFallbackLineSpacing(boolean fallbackLineSpacing) {
            mFallbackLineSpacing = fallbackLineSpacing;
            return this;
        }

        /**
         * Set the width as used for ellipsizing purposes, if it differs from the
         * normal layout width. The default is the {@code width}
         * passed to {@link #build()}.
         *
         * @param ellipsizedWidth width used for ellipsizing, in pixels
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setEllipsizedWidth(int ellipsizedWidth) {
            mEllipsizedWidth = ellipsizedWidth;
            return this;
        }

        /**
         * Set ellipsizing on the layout. Causes words that are longer than the view
         * is wide, or exceeding the number of lines (see #setMaxLines) in the case
         * of {@link TextUtils.TruncateAt#END} or
         * {@link TextUtils.TruncateAt#MARQUEE}, to be ellipsized instead
         * of broken. The default is {@code null}, indicating no ellipsis is to be applied.
         *
         * @param ellipsize type of ellipsis behavior
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setEllipsize(@Nullable TextUtils.TruncateAt ellipsize) {
            mEllipsize = ellipsize;
            return this;
        }

        /**
         * Set maximum number of lines. This is particularly useful in the case of
         * ellipsizing, where it changes the layout of the last line. The default is
         * unlimited.
         *
         * @param maxLines maximum number of lines in the layout
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setMaxLines(int maxLines) {
            mMaxLines = maxLines;
            return this;
        }

        /**
         * Set indents. Arguments are arrays holding an indent amount, one per line, measured in
         * pixels. For lines past the last element in the array, the last element repeats.
         *
         * @param leftIndents  array of indent values for left margin, in pixels
         * @param rightIndents array of indent values for right margin, in pixels
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setIndents(@Nullable int[] leftIndents, @Nullable int[] rightIndents) {
            mLeftIndents = leftIndents;
            mRightIndents = rightIndents;
            return this;
        }

        /**
         * Sets whether the line spacing should be applied for the last line. Default value is
         * {@code false}.
         *
         * @hidden
         */
        @NonNull
        Builder setAddLastLineLineSpacing(boolean value) {
            mAddLastLineLineSpacing = value;
            return this;
        }

        /**
         * Set the line break configuration. The line break will be passed to native used for
         * calculating the text wrapping. The default value of the line break style is
         * {@link LineBreakConfig#LINE_BREAK_STYLE_NONE}
         *
         * @param lineBreakConfig the line break configuration for text wrapping.
         * @return this builder, useful for chaining.
         * @see TextView#setLineBreakStyle
         * @see TextView#setLineBreakWordStyle
         */
        @NonNull
        public Builder setLineBreakConfig(@NonNull LineBreakConfig lineBreakConfig) {
            mLineBreakConfig = lineBreakConfig;
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
        @NonNull
        public StaticLayout build() {
            final StaticLayout result = new StaticLayout(this);
            recycle();
            return result;
        }
    }

    private static final int COLUMNS_NORMAL = 4;
    private static final int COLUMNS_ELLIPSIZE = 6;

    private static final int START = 0;
    private static final int DIR = START;
    private static final int TAB = START;
    private static final int TOP = 1;
    private static final int DESCENT = 2;
    private static final int EXTRA = 3;
    private static final int ELLIPSIS_START = 4;
    private static final int ELLIPSIS_COUNT = 5;

    private static final int START_MASK = 0x1FFFFFFF; // 29 bits
    private static final int DIR_SHIFT = 30;
    private static final int TAB_MASK = 0x20000000;

    private static final int DEFAULT_MAX_LINE_HEIGHT = -1;

    //// Member Variables \\\\

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

    /**
     * Used by DynamicLayout.
     */
    StaticLayout(@Nullable CharSequence text) {
        super(text, null, 0, null, 1, 0);

        mColumns = COLUMNS_ELLIPSIZE;
        mLineDirections = new Directions[2];
        mLines = new int[2 * mColumns];
    }

    private StaticLayout(@NonNull Builder b) {
        super(b.mEllipsize == null
                ? b.mText
                : (b.mText instanceof Spanned)
                ? new SpannedEllipsizer(b.mText)
                : new Ellipsizer(b.mText),
                b.mPaint, b.mWidth, b.mAlignment, b.mTextDir, b.mSpacingMult, b.mSpacingAdd);
        if (b.mEllipsize != null) {
            Ellipsizer e = (Ellipsizer) getText();

            e.mLayout = this;
            e.mWidth = b.mEllipsizedWidth;
            e.mMethod = b.mEllipsize;
            mEllipsizedWidth = b.mEllipsizedWidth;

            mColumns = COLUMNS_ELLIPSIZE;
        } else {
            mColumns = COLUMNS_NORMAL;
            mEllipsizedWidth = b.mWidth;
        }

        mLineDirections = new Directions[2];
        mLines = new int[2 * mColumns];
        mMaximumVisibleLineCount = b.mMaxLines;

        mLeftIndents = b.mLeftIndents;
        mRightIndents = b.mRightIndents;

        generate(b, b.mIncludePad, b.mIncludePad);
    }

    void generate(@NonNull Builder b, boolean includePad, boolean trackPad) {
        final CharSequence source = b.mText;
        final int bufStart = b.mStart;
        final int bufEnd = b.mEnd;
        TextPaint paint = b.mPaint;
        int outerWidth = b.mWidth;
        TextDirectionHeuristic textDir = b.mTextDir;
        float spacingmult = b.mSpacingMult;
        float spacingadd = b.mSpacingAdd;
        final boolean fallbackLineSpacing = b.mFallbackLineSpacing;
        float ellipsizedWidth = b.mEllipsizedWidth;
        TextUtils.TruncateAt ellipsize = b.mEllipsize;
        final boolean addLastLineSpacing = b.mAddLastLineLineSpacing;

        int lineBreakCapacity = 0;
        int[] breaks = null;
        float[] lineWidths = null;
        float[] ascents = null;
        float[] descents = null;
        boolean[] hasTabs = null;

        mLineCount = 0;
        mEllipsized = false;
        mMaxLineHeight = mMaximumVisibleLineCount < 1 ? 0 : DEFAULT_MAX_LINE_HEIGHT;

        // current height
        int v = 0;
        boolean needMultiply = (spacingmult != 1 || spacingadd != 0);

        FontMetricsInt fm = b.mFontMetricsInt;
        int[] chooseHtv = null;

        final int[] indents;
        if (mLeftIndents != null || mRightIndents != null) {
            final int leftLen = mLeftIndents == null ? 0 : mLeftIndents.length;
            final int rightLen = mRightIndents == null ? 0 : mRightIndents.length;
            final int indentsLen = Math.max(leftLen, rightLen);
            indents = new int[indentsLen];
            if (leftLen > 0) {
                System.arraycopy(mLeftIndents, 0, indents, 0, leftLen);
            }
            for (int i = 0; i < rightLen; i++) {
                indents[i] += mRightIndents[i];
            }
        } else {
            indents = null;
        }

        LineBreaker.ParagraphConstraints constraints =
                new LineBreaker.ParagraphConstraints();

        PrecomputedText.ParagraphInfo[] paragraphInfo = null;
        final Spanned spanned = (source instanceof Spanned) ? (Spanned) source : null;
        if (source instanceof PrecomputedText) {
            PrecomputedText precomputed = (PrecomputedText) source;
            final @PrecomputedText.Params.CheckResultUsableResult int checkResult =
                    precomputed.checkResultUsable(bufStart, bufEnd, textDir, paint,
                            b.mLineBreakConfig);
            switch (checkResult) {
                case PrecomputedText.Params.UNUSABLE:
                    break;
                case PrecomputedText.Params.NEED_RECOMPUTE:
                    final PrecomputedText.Params newParams =
                            new PrecomputedText.Params.Builder(paint)
                                    .setTextDirection(textDir)
                                    .setLineBreakConfig(b.mLineBreakConfig)
                                    .build();
                    precomputed = PrecomputedText.create(precomputed, newParams);
                    paragraphInfo = precomputed.getParagraphInfo();
                    break;
                case PrecomputedText.Params.USABLE:
                    // Some parameters are different from the ones when measured text is created.
                    paragraphInfo = precomputed.getParagraphInfo();
                    break;
            }
        }

        if (paragraphInfo == null) {
            final PrecomputedText.Params param = new PrecomputedText.Params(paint,
                    b.mLineBreakConfig, textDir);
            paragraphInfo = PrecomputedText.createMeasuredParagraphs(source, param, bufStart,
                    bufEnd, false /* computeLayout */);
        }

        for (int paraIndex = 0; paraIndex < paragraphInfo.length; paraIndex++) {
            final int paraStart = paraIndex == 0
                    ? bufStart : paragraphInfo[paraIndex - 1].paragraphEnd;
            final int paraEnd = paragraphInfo[paraIndex].paragraphEnd;

            int firstWidthLineCount = 1;
            int firstWidth = outerWidth;
            int restWidth = outerWidth;

            List<LineHeightSpan> chooseHt = Collections.emptyList();
            if (spanned != null) {
                List<LeadingMarginSpan> leadingMarginSpans = getParagraphSpans(spanned, paraStart, paraEnd,
                        LeadingMarginSpan.class);
                for (int i = 0; i < leadingMarginSpans.size(); i++) {
                    LeadingMarginSpan lms = leadingMarginSpans.get(i);
                    firstWidth -= lms.getLeadingMargin(true);
                    restWidth -= lms.getLeadingMargin(false);

                    // LeadingMarginSpan2 is odd.  The count affects all
                    // leading margin spans, not just this particular one
                    if (lms instanceof LeadingMarginSpan2) {
                        firstWidthLineCount = Math.max(firstWidthLineCount,
                                ((LeadingMarginSpan2) lms).getLeadingMarginLineCount());
                    }
                }

                List<TrailingMarginSpan> trailingMarginSpans = getParagraphSpans(spanned, paraStart, paraEnd,
                        TrailingMarginSpan.class);
                for (int i = 0; i < trailingMarginSpans.size(); i++) {
                    TrailingMarginSpan tms = trailingMarginSpans.get(i);
                    int margin = tms.getTrailingMargin();
                    firstWidth -= margin;
                    restWidth -= margin;
                }

                chooseHt = getParagraphSpans(spanned, paraStart, paraEnd, LineHeightSpan.class);

                if (!chooseHt.isEmpty()) {
                    if (chooseHtv == null || chooseHtv.length < chooseHt.size()) {
                        chooseHtv = new int[chooseHt.size()];
                    }

                    for (int i = 0; i < chooseHt.size(); i++) {
                        int o = spanned.getSpanStart(chooseHt.get(i));

                        if (o < paraStart) {
                            // starts in this layout, before the
                            // current paragraph

                            chooseHtv[i] = getLineTop(getLineForOffset(o));
                        } else {
                            // starts in this paragraph

                            chooseHtv[i] = v;
                        }
                    }
                }
            }

            // tab stop locations
            float[] variableTabStops = null;
            if (spanned != null) {
                List<TabStopSpan> spans = getParagraphSpans(spanned, paraStart,
                        paraEnd, TabStopSpan.class);
                if (!spans.isEmpty()) {
                    float[] stops = new float[spans.size()];
                    for (int i = 0; i < spans.size(); i++) {
                        stops[i] = (float) spans.get(i).getTabStop();
                    }
                    Arrays.sort(stops, 0, stops.length);
                    variableTabStops = stops;
                }
            }

            final MeasuredParagraph measuredPara = paragraphInfo[paraIndex].measured;
            final int[] spanEndCache = measuredPara.getSpanEndCache().elements();
            final int[] fmCache = measuredPara.getFontMetrics().elements();

            constraints.setWidth(restWidth);
            constraints.setIndent(firstWidth);
            constraints.setTabStops(variableTabStops, TAB_INCREMENT);

            LineBreaker.Result res = LineBreaker.computeLineBreaks(
                    measuredPara.getMeasuredText(), constraints, indents, mLineCount);
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

            final int remainingLineCount = mMaximumVisibleLineCount - mLineCount;
            final boolean ellipsisMayBeApplied = ellipsize != null
                    && (ellipsize == TextUtils.TruncateAt.END
                    || (mMaximumVisibleLineCount == 1
                    && ellipsize != TextUtils.TruncateAt.MARQUEE));

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
                            width += measuredPara.getAdvance(j);
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
            int cacheIndex = 0;
            int breakIndex = 0;
            for (int spanStart = paraStart, spanEnd; spanStart < paraEnd; spanStart = spanEnd) {
                // retrieve end of span
                spanEnd = spanEndCache[cacheIndex];

                fm.ascent = fmCache[cacheIndex * 2];
                fm.descent = fmCache[cacheIndex * 2 + 1];
                cacheIndex++;

                if (fm.ascent < fmAscent) {
                    fmAscent = fm.ascent;
                }
                if (fm.descent > fmDescent) {
                    fmDescent = fm.descent;
                }

                // skip breaks ending before current span range
                while (breakIndex < breakCount && paraStart + breaks[breakIndex] < spanStart) {
                    breakIndex++;
                }

                while (breakIndex < breakCount && paraStart + breaks[breakIndex] <= spanEnd) {
                    int endPos = paraStart + breaks[breakIndex];

                    boolean moreChars = (endPos < bufEnd);

                    final int ascent = fallbackLineSpacing
                            ? Math.min(fmAscent, Math.round(ascents[breakIndex]))
                            : fmAscent;
                    final int descent = fallbackLineSpacing
                            ? Math.max(fmDescent, Math.round(descents[breakIndex]))
                            : fmDescent;

                    //FIXME top, bottom
                    v = out(source, here, endPos,
                            ascent, descent, ascent, descent,
                            v, spacingmult, spacingadd, chooseHt, chooseHtv, fm,
                            hasTabs[breakIndex], needMultiply,
                            measuredPara, bufEnd, includePad, trackPad, addLastLineSpacing,
                            paraStart, ellipsize, ellipsizedWidth, lineWidths[breakIndex],
                            paint, moreChars);

                    if (endPos < spanEnd) {
                        // preserve metrics for current span
                        fmAscent = fm.ascent;
                        fmDescent = fm.descent;
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

            if (paraEnd >= bufEnd) {
                assert paraEnd == bufEnd;
                break;
            }
        }

        if ((bufEnd == bufStart || source.charAt(bufEnd - 1) == '\n')
                && mLineCount < mMaximumVisibleLineCount) {
            final MeasuredParagraph measuredPara =
                    MeasuredParagraph.buildForBidi(source, bufEnd, bufEnd, textDir, null);
            paint.getFontMetricsInt(fm);
            out(source, bufEnd, bufEnd,
                    fm.ascent, fm.descent, fm.ascent, fm.descent,
                    v, spacingmult, spacingadd,
                    Collections.emptyList(), null, fm, false,
                    needMultiply, measuredPara, bufEnd,
                    includePad, trackPad, addLastLineSpacing,
                    bufStart, ellipsize,
                    ellipsizedWidth, 0, paint, false);
        }
    }

    private int out(final CharSequence text, final int start, final int end, int above, int below,
                    int top, int bottom, int v, final float spacingmult, final float spacingadd,
                    final List<LineHeightSpan> chooseHt, final int[] chooseHtv,
                    final FontMetricsInt fm, final boolean hasTab, final boolean needMultiply,
                    @NonNull final MeasuredParagraph measured,
                    final int bufEnd, final boolean includePad, final boolean trackPad,
                    final boolean addLastLineLineSpacing,
                    final int widthStart, final TextUtils.TruncateAt ellipsize, final float ellipsisWidth,
                    final float textWidth, final TextPaint paint, final boolean moreChars) {
        final int j = mLineCount;
        final int off = j * mColumns;
        final int want = off + mColumns + TOP;
        final int dir = measured.getParagraphDir();

        if (want >= mLines.length) {
            mLines = Arrays.copyOf(mLines, GrowingArrayUtils.growSize(want));
        }

        if (j >= mLineDirections.length) {
            mLineDirections = Arrays.copyOf(mLineDirections, GrowingArrayUtils.growSize(j));
        }

        if (!chooseHt.isEmpty()) {
            fm.ascent = above;
            fm.descent = below;
            /*fm.top = top;
            fm.bottom = bottom;*/

            for (int i = 0; i < chooseHt.size(); i++) {
                chooseHt.get(i).chooseHeight(text, start, end, chooseHtv[i], v, fm, paint);
            }

            above = fm.ascent;
            below = fm.descent;
            /*top = fm.top;
            bottom = fm.bottom;*/
        }

        boolean firstLine = (j == 0);
        boolean currentLineIsTheLastVisibleOne = (j + 1 == mMaximumVisibleLineCount);

        if (ellipsize != null) {
            // If there is only one line, then do any type of ellipsis except when it is MARQUEE
            // if there are multiple lines, just allow END ellipsis on the last line
            boolean forceEllipsis = moreChars && (mLineCount + 1 == mMaximumVisibleLineCount);

            boolean doEllipsis =
                    (((mMaximumVisibleLineCount == 1 && moreChars) || (firstLine && !moreChars)) &&
                            ellipsize != TextUtils.TruncateAt.MARQUEE) ||
                            (!firstLine && (currentLineIsTheLastVisibleOne || !moreChars) &&
                                    ellipsize == TextUtils.TruncateAt.END);
            if (doEllipsis) {
                calculateEllipsis(start, end, measured, widthStart,
                        ellipsisWidth, ellipsize, j,
                        textWidth, paint, forceEllipsis);
            }
        }

        final boolean lastLine;
        if (mEllipsized) {
            lastLine = true;
        } else {
            final boolean lastCharIsNewLine = widthStart != bufEnd && bufEnd > 0
                    && text.charAt(bufEnd - 1) == '\n';
            if (end == bufEnd && !lastCharIsNewLine) {
                lastLine = true;
            } else {
                lastLine = start == bufEnd && lastCharIsNewLine;
            }
        }

        if (firstLine) {
            if (trackPad) {
                mTopPadding = top - above;
            }

            if (includePad) {
                above = top;
            }
        }

        if (lastLine) {
            if (trackPad) {
                mBottomPadding = bottom - below;
            }

            if (includePad) {
                below = bottom;
            }
        }

        int extra;

        if (needMultiply && (addLastLineLineSpacing || !lastLine)) {
            double ex = (below - above) * (spacingmult - 1) + spacingadd;
            if (ex >= 0) {
                extra = (int)(ex + 0.5);
            } else {
                extra = -(int)(-ex + 0.5);
            }
        } else {
            extra = 0;
        }

        int[] lines = mLines;

        lines[off + START] = start;
        lines[off + TOP] = v;
        lines[off + DESCENT] = below + extra;
        lines[off + EXTRA] = extra;

        // special case for non-ellipsized last visible line when maxLines is set
        // store the height as if it was ellipsized
        if (!mEllipsized && currentLineIsTheLastVisibleOne) {
            // below calculation as if it was the last line
            int maxLineBelow = includePad ? bottom : below;
            // similar to the calculation of v below, without the extra.
            mMaxLineHeight = v + (maxLineBelow - above);
        }

        v += (below - above) + extra;
        lines[off + mColumns + START] = end;
        lines[off + mColumns + TOP] = v;

        lines[off + TAB] |= hasTab ? TAB_MASK : 0;
        lines[off + DIR] |= dir << DIR_SHIFT;
        mLineDirections[j] = measured.getDirections(start - widthStart, end - widthStart);

        mLineCount++;
        return v;
    }

    private void calculateEllipsis(int lineStart, int lineEnd,
                                   MeasuredParagraph measured, int widthStart,
                                   float avail, TextUtils.TruncateAt where,
                                   int line, float textWidth, TextPaint paint,
                                   boolean forceEllipsis) {
        avail -= getTotalInsets(line);
        if (textWidth <= avail && !forceEllipsis) {
            // Everything fits!
            mLines[mColumns * line + ELLIPSIS_START] = 0;
            mLines[mColumns * line + ELLIPSIS_COUNT] = 0;
            return;
        }

        // NB: ellipsis string is considered as Force LTR
        char[] ellipsisChars = TextUtils.getEllipsisChars(where);
        float ellipsisWidth = LayoutCache.getOrCreate(ellipsisChars, 0, ellipsisChars.length,
                0, ellipsisChars.length, false, paint.getInternalPaint(), 0).getAdvance();
        int ellipsisStart = 0;
        int ellipsisCount = 0;
        int len = lineEnd - lineStart;

        // We only support start ellipsis on a single line
        if (where == TextUtils.TruncateAt.START) {
            if (mMaximumVisibleLineCount == 1) {
                float sum = 0;
                int i;

                for (i = len; i > 0; i--) {
                    float w = measured.getAdvance(i - 1 + lineStart - widthStart);
                    if (w + sum + ellipsisWidth > avail) {
                        while (i < len
                                && measured.getAdvance(i + lineStart - widthStart) == 0.0f) {
                            i++;
                        }
                        break;
                    }

                    sum += w;
                }

                ellipsisStart = 0;
                ellipsisCount = i;
            } else {
                Log.LOGGER.warn(MARKER, "Start Ellipsis only supported with one line");
            }
        } else if (where == TextUtils.TruncateAt.END || where == TextUtils.TruncateAt.MARQUEE) {
            float sum = 0;
            int i;

            for (i = 0; i < len; i++) {
                float w = measured.getAdvance(i + lineStart - widthStart);

                if (w + sum + ellipsisWidth > avail) {
                    break;
                }

                sum += w;
            }

            ellipsisStart = i;
            ellipsisCount = len - i;
            if (forceEllipsis && ellipsisCount == 0 && len > 0) {
                ellipsisStart = len - 1;
                ellipsisCount = 1;
            }
        } else {
            // where = TextUtils.TruncateAt.MIDDLE We only support middle ellipsis on a single line
            if (mMaximumVisibleLineCount == 1) {
                float lsum = 0, rsum = 0;
                int left, right;

                float ravail = (avail - ellipsisWidth) / 2;
                for (right = len; right > 0; right--) {
                    float w = measured.getAdvance(right - 1 + lineStart - widthStart);

                    if (w + rsum > ravail) {
                        while (right < len
                                && measured.getAdvance(right + lineStart - widthStart)
                                == 0.0f) {
                            right++;
                        }
                        break;
                    }
                    rsum += w;
                }

                float lavail = avail - ellipsisWidth - rsum;
                for (left = 0; left < right; left++) {
                    float w = measured.getAdvance(left + lineStart - widthStart);

                    if (w + lsum > lavail) {
                        break;
                    }

                    lsum += w;
                }

                ellipsisStart = left;
                ellipsisCount = right - left;
            } else {
                Log.LOGGER.warn(MARKER, "Middle Ellipsis only supported with one line");
            }
        }
        mEllipsized = true;
        mLines[mColumns * line + ELLIPSIS_START] = ellipsisStart;
        mLines[mColumns * line + ELLIPSIS_COUNT] = ellipsisCount;
    }

    private float getTotalInsets(int line) {
        int totalIndent = 0;
        if (mLeftIndents != null) {
            totalIndent = mLeftIndents[Math.min(line, mLeftIndents.length - 1)];
        }
        if (mRightIndents != null) {
            totalIndent += mRightIndents[Math.min(line, mRightIndents.length - 1)];
        }
        return totalIndent;
    }

    // Override the base class so we can directly access our members,
    // rather than relying on member functions.
    // The logic mirrors that of Layout.getLineForVertical
    // FIXME: It may be faster to do a linear search for layouts without many lines.
    @Override
    public int getLineForVertical(int vertical) {
        int high = mLineCount;
        int low = -1;
        int guess;
        int[] lines = mLines;
        while (high - low > 1) {
            guess = (high + low) >> 1;
            if (lines[mColumns * guess + TOP] > vertical) {
                high = guess;
            } else {
                low = guess;
            }
        }
        return Math.max(low, 0);
    }

    @Override
    public int getLineCount() {
        return mLineCount;
    }

    @Override
    public int getLineTop(int line) {
        return mLines[mColumns * line + TOP];
    }

    @Override
    public int getLineExtra(int line) {
        return mLines[mColumns * line + EXTRA];
    }

    @Override
    public int getLineDescent(int line) {
        return mLines[mColumns * line + DESCENT];
    }

    @Override
    public int getLineStart(int line) {
        return mLines[mColumns * line + START] & START_MASK;
    }

    @Override
    public int getParagraphDirection(int line) {
        return mLines[mColumns * line + DIR] >> DIR_SHIFT;
    }

    @Override
    public boolean getLineContainsTab(int line) {
        return (mLines[mColumns * line + TAB] & TAB_MASK) != 0;
    }

    @Override
    public final Directions getLineDirections(int line) {
        if (line > getLineCount()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return mLineDirections[line];
    }

    @Override
    public int getTopPadding() {
        return mTopPadding;
    }

    @Override
    public int getBottomPadding() {
        return mBottomPadding;
    }

    @Override
    public int getIndentAdjust(int line, Alignment align) {
        if (align == Alignment.ALIGN_LEFT) {
            if (mLeftIndents == null) {
                return 0;
            } else {
                return mLeftIndents[Math.min(line, mLeftIndents.length - 1)];
            }
        } else if (align == Alignment.ALIGN_RIGHT) {
            if (mRightIndents == null) {
                return 0;
            } else {
                return -mRightIndents[Math.min(line, mRightIndents.length - 1)];
            }
        } else if (align == Alignment.ALIGN_CENTER) {
            int left = 0;
            if (mLeftIndents != null) {
                left = mLeftIndents[Math.min(line, mLeftIndents.length - 1)];
            }
            int right = 0;
            if (mRightIndents != null) {
                right = mRightIndents[Math.min(line, mRightIndents.length - 1)];
            }
            return (left - right) >> 1;
        } else {
            throw new AssertionError("unhandled alignment " + align);
        }
    }

    @Override
    public int getEllipsisCount(int line) {
        if (mColumns < COLUMNS_ELLIPSIZE) {
            return 0;
        }

        return mLines[mColumns * line + ELLIPSIS_COUNT];
    }

    @Override
    public int getEllipsisStart(int line) {
        if (mColumns < COLUMNS_ELLIPSIZE) {
            return 0;
        }

        return mLines[mColumns * line + ELLIPSIS_START];
    }

    @Override
    public int getEllipsizedWidth() {
        return mEllipsizedWidth;
    }

    /**
     * Return the total height of this layout.
     *
     * @param cap if true and max lines is set, returns the height of the layout at the max lines.
     */
    @Override
    public int getHeight(boolean cap) {
        if (cap && mLineCount > mMaximumVisibleLineCount && mMaxLineHeight == -1) {
            Log.LOGGER.warn(MARKER, "maxLineHeight should not be -1. "
                    + " maxLines: {} lineCount: {}", mMaximumVisibleLineCount, mLineCount);
        }

        return cap && mLineCount > mMaximumVisibleLineCount && mMaxLineHeight != -1
                ? mMaxLineHeight : super.getHeight();
    }
}
