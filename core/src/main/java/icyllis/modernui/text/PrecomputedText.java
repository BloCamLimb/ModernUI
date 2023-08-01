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

import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.LineBreakConfig;
import icyllis.modernui.text.style.MetricAffectingSpan;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

/**
 * A text which has the character metrics data.
 * <p>
 * A text object that contains the character metrics data and can be used to improve the performance
 * of text layout operations. When a PrecomputedText is created with a given {@link CharSequence},
 * it will measure the text metrics during the creation. This PrecomputedText instance can be set on
 * {@link icyllis.modernui.widget.TextView} or {@link StaticLayout}. Since the text layout information will
 * be included in this instance, {@link icyllis.modernui.widget.TextView} or {@link StaticLayout} will not
 * have to recalculate this information.
 * <p>
 * Note that the {@link PrecomputedText} created from different parameters of the target {@link
 * icyllis.modernui.widget.TextView} will be rejected internally and compute the text layout again with the
 * current {@link icyllis.modernui.widget.TextView} parameters.
 *
 * <pre>
 * An example usage is:
 * {@code
 *  static void asyncSetText(TextView textView, final String longString, Executor bgExecutor) {
 *      // construct precompute related parameters using the TextView that we will set the text on.
 *      final PrecomputedText.Params params = textView.getTextMetricsParams();
 *      final Reference textViewRef = new WeakReference<>(textView);
 *      bgExecutor.submit(() -> {
 *          TextView textView = textViewRef.get();
 *          if (textView == null) return;
 *          final PrecomputedText precomputedText = PrecomputedText.create(longString, params);
 *          textView.post(() -> {
 *              TextView textView = textViewRef.get();
 *              if (textView == null) return;
 *              textView.setText(precomputedText);
 *          });
 *      });
 *  }
 * }
 * </pre>
 * <p>
 * Note that the {@link PrecomputedText} created from different parameters of the target
 * {@link icyllis.modernui.widget.TextView} will be rejected.
 * <p>
 * Note that any {@link NoCopySpan} attached to the original text won't be passed to
 * PrecomputedText.
 */
public class PrecomputedText implements Spannable {

    /**
     * The information required for building {@link PrecomputedText}.
     * <p>
     * Contains information required for precomputing text measurement metadata, so it can be done
     * in isolation of a {@link icyllis.modernui.widget.TextView} or {@link StaticLayout}, when final layout
     * constraints are not known.
     */
    public static final class Params {
        // The TextPaint used for measurement.
        private final @NonNull TextPaint mPaint;

        // The requested text direction.
        private final @NonNull TextDirectionHeuristic mTextDir;

        // The line break configuration for calculating text wrapping.
        private final @NonNull LineBreakConfig mLineBreakConfig;

        /**
         * A builder for creating {@link Params}.
         */
        public static class Builder {
            // The TextPaint used for measurement.
            private final @NonNull TextPaint mPaint;

            // The requested text direction.
            private TextDirectionHeuristic mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;

            // The line break configuration for calculating text wrapping.
            private @NonNull LineBreakConfig mLineBreakConfig = LineBreakConfig.NONE;

            /**
             * Builder constructor.
             *
             * @param paint the paint to be used for drawing
             */
            public Builder(@NonNull TextPaint paint) {
                mPaint = paint;
            }

            /**
             * Builder constructor from existing params.
             */
            public Builder(@NonNull Params params) {
                mPaint = params.mPaint;
                mTextDir = params.mTextDir;
                mLineBreakConfig = params.mLineBreakConfig;
            }

            /**
             * Set the text direction heuristic.
             * <p>
             * The default value is {@link TextDirectionHeuristics#FIRSTSTRONG_LTR}.
             *
             * @param textDir the text direction heuristic for resolving bidi behavior
             * @return this builder, useful for chaining
             * @see StaticLayout.Builder#setTextDirection
             */
            public Builder setTextDirection(@NonNull TextDirectionHeuristic textDir) {
                mTextDir = textDir;
                return this;
            }

            /**
             * Set the line break config for the text wrapping.
             *
             * @param lineBreakConfig the newly line break configuration.
             * @return this builder, useful for chaining.
             * @see StaticLayout.Builder#setLineBreakConfig
             */
            public @NonNull Builder setLineBreakConfig(@NonNull LineBreakConfig lineBreakConfig) {
                mLineBreakConfig = lineBreakConfig;
                return this;
            }

            /**
             * Build the {@link Params}.
             *
             * @return the layout parameter
             */
            public @NonNull Params build() {
                return new Params(mPaint, mLineBreakConfig, mTextDir);
            }
        }

        @ApiStatus.Internal
        public Params(@NonNull TextPaint paint,
                      @NonNull LineBreakConfig lineBreakConfig,
                      @NonNull TextDirectionHeuristic textDir) {
            mPaint = paint;
            mTextDir = textDir;
            mLineBreakConfig = lineBreakConfig;
        }

        /**
         * Returns the {@link TextPaint} for this text.
         *
         * @return A {@link TextPaint}
         */
        public @NonNull TextPaint getTextPaint() {
            return mPaint;
        }

        /**
         * Returns the {@link TextDirectionHeuristic} for this text.
         *
         * @return A {@link TextDirectionHeuristic}
         */
        public @NonNull TextDirectionHeuristic getTextDirection() {
            return mTextDir;
        }

        /**
         * Returns the {@link LineBreakConfig} for this text.
         *
         * @return the current line break configuration. The {@link LineBreakConfig} with default
         * values will be returned if no line break configuration is set.
         */
        public @NonNull LineBreakConfig getLineBreakConfig() {
            return mLineBreakConfig;
        }

        @ApiStatus.Internal
        @MagicConstant(intValues = {UNUSABLE, NEED_RECOMPUTE, USABLE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface CheckResultUsableResult {
        }

        /**
         * Constant for returning value of checkResultUsable indicating that given parameter is not
         * compatible.
         */
        @ApiStatus.Internal
        public static final int UNUSABLE = 0;

        /**
         * Constant for returning value of checkResultUsable indicating that given parameter is not
         * compatible but partially usable for creating new PrecomputedText.
         */
        @ApiStatus.Internal
        public static final int NEED_RECOMPUTE = 1;

        /**
         * Constant for returning value of checkResultUsable indicating that given parameter is
         * compatible.
         */
        @ApiStatus.Internal
        public static final int USABLE = 2;

        @ApiStatus.Internal
        public @CheckResultUsableResult int checkResultUsable(
                @NonNull TextPaint paint, @NonNull TextDirectionHeuristic textDir,
                @NonNull LineBreakConfig lbConfig) {
            if (mLineBreakConfig.equals(lbConfig)
                    && mPaint.equalsForTextMeasurement(paint)) {
                return mTextDir == textDir ? USABLE : NEED_RECOMPUTE;
            } else {
                return UNUSABLE;
            }
        }

        /**
         * Check if the same text layout.
         *
         * @return true if this and the given param result in the same text layout
         */
        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Params param)) {
                return false;
            }
            return checkResultUsable(param.mPaint, param.mTextDir, param.mLineBreakConfig) == Params.USABLE;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mPaint.getInternalPaint(), mTextDir, mLineBreakConfig);
        }

        @Override
        public String toString() {
            return "{"
                    + mPaint.getInternalPaint()
                    + ", textDir=" + mTextDir
                    + ", " + mLineBreakConfig
                    + "}";
        }
    }

    @ApiStatus.Internal
    public static class ParagraphInfo {
        public final @IntRange(from = 0) int paragraphEnd;
        public final @NonNull MeasuredParagraph measured;

        /**
         * @param paraEnd  the end offset of this paragraph
         * @param measured a measured paragraph
         */
        public ParagraphInfo(@IntRange(from = 0) int paraEnd, @NonNull MeasuredParagraph measured) {
            this.paragraphEnd = paraEnd;
            this.measured = measured;
        }
    }


    // The original text.
    private final @NonNull SpannableString mText;

    // The inclusive start offset of the measuring target.
    private final @IntRange(from = 0) int mStart;

    // The exclusive end offset of the measuring target.
    private final @IntRange(from = 0) int mEnd;

    private final @NonNull Params mParams;

    // The list of measured paragraph info.
    private final @NonNull ParagraphInfo[] mParagraphInfo;

    /**
     * Create a new {@link PrecomputedText} which will pre-compute text measurement and glyph
     * positioning information.
     * <p>
     * This can be expensive, so computing this on a background thread before your text will be
     * presented can save work on the UI thread.
     * </p>
     * <p>
     * Note that any {@link NoCopySpan} attached to the text won't be passed to the
     * created PrecomputedText.
     *
     * @param text   the text to be measured
     * @param params parameters that define how text will be precomputed
     * @return A {@link PrecomputedText}
     */
    public static PrecomputedText create(@NonNull CharSequence text, @NonNull Params params) {
        ParagraphInfo[] paraInfo = null;
        if (text instanceof final PrecomputedText hintPct) {
            final PrecomputedText.Params hintParams = hintPct.getParams();
            final @Params.CheckResultUsableResult int checkResult =
                    hintParams.checkResultUsable(params.mPaint, params.mTextDir,
                            params.mLineBreakConfig);
            switch (checkResult) {
                case Params.USABLE:
                    return hintPct;
                case Params.NEED_RECOMPUTE:
                    // To be able to use PrecomputedText for new params, at least break strategy and
                    // hyphenation frequency must be the same.
                    paraInfo = createMeasuredParagraphsFromPrecomputedText(
                            hintPct, params, true /* compute layout */);
                    break;
                case Params.UNUSABLE:
                    // Unable to use anything in PrecomputedText. Create PrecomputedText as the
                    // normal text input.
            }

        }
        if (paraInfo == null) {
            paraInfo = createMeasuredParagraphs(
                    text, params, 0, text.length(), true /* computeLayout */);
        }
        return new PrecomputedText(text, 0, text.length(), params, paraInfo);
    }

    private static ParagraphInfo[] createMeasuredParagraphsFromPrecomputedText(
            @NonNull PrecomputedText pct, @NonNull Params params, boolean computeLayout) {
        ArrayList<ParagraphInfo> result = new ArrayList<>();
        for (int i = 0; i < pct.getParagraphCount(); ++i) {
            final int paraStart = pct.getParagraphStart(i);
            final int paraEnd = pct.getParagraphEnd(i);
            result.add(new ParagraphInfo(paraEnd, MeasuredParagraph.buildForStaticLayout(
                    params.getTextPaint(), params.getLineBreakConfig(), pct, paraStart, paraEnd,
                    params.getTextDirection(), computeLayout,
                    null /* no recycle */)));
        }
        return result.toArray(new ParagraphInfo[0]);
    }

    @ApiStatus.Internal
    public static ParagraphInfo[] createMeasuredParagraphs(
            @NonNull CharSequence text, @NonNull Params params,
            @IntRange(from = 0) int start, @IntRange(from = 0) int end, boolean computeLayout) {
        ArrayList<ParagraphInfo> result = new ArrayList<>();

        Objects.requireNonNull(text);
        Objects.requireNonNull(params);

        int paraEnd;
        for (int paraStart = start; paraStart < end; paraStart = paraEnd) {
            paraEnd = TextUtils.indexOf(text, '\n', paraStart, end);
            if (paraEnd < 0) {
                // No LINE_FEED(U+000A) character found. Use end of the text as the paragraph
                // end.
                paraEnd = end;
            } else {
                paraEnd++;  // Includes LINE_FEED(U+000A) to the prev paragraph.
            }

            result.add(new ParagraphInfo(paraEnd, MeasuredParagraph.buildForStaticLayout(
                    params.getTextPaint(), params.getLineBreakConfig(), text, paraStart, paraEnd,
                    params.getTextDirection(), computeLayout, null /* no recycle */)));
        }
        return result.toArray(new ParagraphInfo[0]);
    }

    // Use PrecomputedText.create instead.
    private PrecomputedText(@NonNull CharSequence text, @IntRange(from = 0) int start,
                            @IntRange(from = 0) int end, @NonNull Params params,
                            @NonNull ParagraphInfo[] paraInfo) {
        mText = new SpannableString(text, true /* ignoreNoCopySpan */);
        mStart = start;
        mEnd = end;
        mParams = params;
        mParagraphInfo = paraInfo;
    }

    /**
     * Return the underlying text.
     */
    @ApiStatus.Internal
    public @NonNull CharSequence getText() {
        return mText;
    }

    /**
     * Returns the inclusive start offset of measured region.
     */
    @ApiStatus.Internal
    public @IntRange(from = 0) int getStart() {
        return mStart;
    }

    /**
     * Returns the exclusive end offset of measured region.
     */
    @ApiStatus.Internal
    public @IntRange(from = 0) int getEnd() {
        return mEnd;
    }

    /**
     * Returns the layout parameters used to measure this text.
     */
    public @NonNull Params getParams() {
        return mParams;
    }

    /**
     * Returns the count of paragraphs.
     */
    public @IntRange(from = 0) int getParagraphCount() {
        return mParagraphInfo.length;
    }

    /**
     * Returns the paragraph start offset of the text.
     */
    public @IntRange(from = 0) int getParagraphStart(@IntRange(from = 0) int paraIndex) {
        Objects.checkIndex(paraIndex, getParagraphCount());
        return paraIndex == 0 ? mStart : getParagraphEnd(paraIndex - 1);
    }

    /**
     * Returns the paragraph end offset of the text.
     */
    public @IntRange(from = 0) int getParagraphEnd(@IntRange(from = 0) int paraIndex) {
        Objects.checkIndex(paraIndex, getParagraphCount());
        return mParagraphInfo[paraIndex].paragraphEnd;
    }

    @ApiStatus.Internal
    public @NonNull MeasuredParagraph getMeasuredParagraph(@IntRange(from = 0) int paraIndex) {
        return mParagraphInfo[paraIndex].measured;
    }

    @ApiStatus.Internal
    public @NonNull ParagraphInfo[] getParagraphInfo() {
        return mParagraphInfo;
    }

    /**
     * Returns value if the given TextPaint gives the same result of text layout for this text.
     */
    @ApiStatus.Internal
    public @Params.CheckResultUsableResult int checkResultUsable(
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end, @NonNull TextDirectionHeuristic textDir,
            @NonNull TextPaint paint, @NonNull LineBreakConfig lbConfig) {
        if (mStart != start || mEnd != end) {
            return Params.UNUSABLE;
        } else {
            return mParams.checkResultUsable(paint, textDir, lbConfig);
        }
    }

    @ApiStatus.Internal
    public int findParaIndex(@IntRange(from = 0) int pos) {
        // TODO: Maybe good to remove paragraph concept from PrecomputedText and add substring
        //       layout support to StaticLayout.
        for (int i = 0; i < mParagraphInfo.length; ++i) {
            if (pos < mParagraphInfo[i].paragraphEnd) {
                return i;
            }
        }
        throw new IndexOutOfBoundsException(
                "pos must be less than " + mParagraphInfo[mParagraphInfo.length - 1].paragraphEnd
                        + ", gave " + pos);
    }

    /**
     * Returns text width for the given range.
     * Both {@code start} and {@code end} offset need to be in the same paragraph, otherwise
     * IllegalArgumentException will be thrown.
     *
     * @param start the inclusive start offset in the text
     * @param end   the exclusive end offset in the text
     * @return the text width
     * @throws IllegalArgumentException if start and end offset are in the different paragraph.
     */
    public @FloatRange(from = 0) float getWidth(@IntRange(from = 0) int start,
                                                @IntRange(from = 0) int end) {
        Objects.checkFromToIndex(start, end, mText.length());

        if (start == end) {
            return 0;
        }
        final int paraIndex = findParaIndex(start);
        final int paraStart = getParagraphStart(paraIndex);
        final int paraEnd = getParagraphEnd(paraIndex);
        if (start < paraStart || paraEnd < end) {
            throw new IllegalArgumentException("Cannot measured across the paragraph:"
                    + "para: (" + paraStart + ", " + paraEnd + "), "
                    + "request: (" + start + ", " + end + ")");
        }
        return getMeasuredParagraph(paraIndex).getAdvance(start - paraStart, end - paraStart);
    }

    /**
     * Retrieves the text font metrics for the given range.
     * Both {@code start} and {@code end} offset need to be in the same paragraph, otherwise
     * IllegalArgumentException will be thrown.
     *
     * @param start      the inclusive start offset in the text
     * @param end        the exclusive end offset in the text
     * @param outMetrics the output font metrics
     * @throws IllegalArgumentException if start and end offset are in the different paragraph.
     */
    public void getFontMetricsInt(@IntRange(from = 0) int start, @IntRange(from = 0) int end,
                                  @NonNull FontMetricsInt outMetrics) {
        Objects.checkFromToIndex(start, end, mText.length());
        Objects.requireNonNull(outMetrics);
        if (start == end) {
            mParams.getTextPaint().getFontMetricsInt(outMetrics);
            return;
        }
        final int paraIndex = findParaIndex(start);
        final int paraStart = getParagraphStart(paraIndex);
        final int paraEnd = getParagraphEnd(paraIndex);
        if (start < paraStart || paraEnd < end) {
            throw new IllegalArgumentException("Cannot measured across the paragraph:"
                    + "para: (" + paraStart + ", " + paraEnd + "), "
                    + "request: (" + start + ", " + end + ")");
        }
        getMeasuredParagraph(paraIndex).getExtent(start - paraStart,
                end - paraStart, outMetrics);
    }

    /**
     * Returns the size of PrecomputedText memory usage.
     * <p>
     * Note that this is not guaranteed to be accurate. Must be used only for testing purposes.
     */
    @ApiStatus.Internal
    public int getMemoryUsage() {
        int r = 0;
        for (var info : mParagraphInfo) {
            r += info.measured.getMemoryUsage();
        }
        return r;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Spannable overrides
    //
    // Do not allow to modify MetricAffectingSpan

    /**
     * @throws IllegalArgumentException if {@link MetricAffectingSpan} is specified.
     */
    @Override
    public void setSpan(@NonNull Object what, int start, int end, int flags) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be set to PrecomputedText.");
        }
        mText.setSpan(what, start, end, flags);
    }

    /**
     * @throws IllegalArgumentException if {@link MetricAffectingSpan} is specified.
     */
    @Override
    public void removeSpan(@NonNull Object what) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be removed from PrecomputedText.");
        }
        mText.removeSpan(what);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Spanned overrides
    //
    // Just proxy for underlying mText if appropriate.

    @NonNull
    @Override
    public <T> List<T> getSpans(int start, int end, @Nullable Class<? extends T> type,
                                @Nullable List<T> dest) {
        return mText.getSpans(start, end, type, dest);
    }

    @Override
    public int getSpanStart(@NonNull Object tag) {
        return mText.getSpanStart(tag);
    }

    @Override
    public int getSpanEnd(@NonNull Object tag) {
        return mText.getSpanEnd(tag);
    }

    @Override
    public int getSpanFlags(@NonNull Object tag) {
        return mText.getSpanFlags(tag);
    }

    @Override
    public int nextSpanTransition(int start, int limit, @Nullable Class<?> type) {
        return mText.nextSpanTransition(start, limit, type);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // CharSequence overrides.
    //
    // Just proxy for underlying mText.

    @Override
    public int length() {
        return mText.length();
    }

    @Override
    public char charAt(int index) {
        return mText.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return PrecomputedText.create(mText.subSequence(start, end), mParams);
    }

    @Override
    public String toString() {
        return mText.toString();
    }
}
