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
import icyllis.modernui.graphics.text.MeasuredText;
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
 *  static void asyncSetText(TextView textView, final CharSequence longString, Executor bgExecutor) {
 *      // construct precompute related parameters using the TextView that we will set the text on.
 *      final PrecomputedText.Params params = textView.getTextMetricsParams();
 *      final WeakReference<TextView> textViewRef = new WeakReference<>(textView);
 *      bgExecutor.execute(() -> {
 *          final PrecomputedText precomputedText = PrecomputedText.create(longString, params);
 *          TextView textView = textViewRef.get();
 *          if (textView == null) return;
 *          textView.post(() -> textView.setText(precomputedText));
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
 * <p>
 * Note that the same {@link PrecomputedText} instance cannot be set to multiple TextViews
 * at the same time. Use {@link #create(CharSequence, Params)} to copy the internal spans.
 */
//TODO Not fully utilized
public final class PrecomputedText extends SpannableString {
    // AOSP's PrecomputedText is buggy everywhere, we fixed a lot

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

        /**
         * Use {@link Builder} instead.
         *
         * @hidden
         */
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

        /**
         * @hidden
         */
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

    /**
     * @hidden
     */
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
     * created PrecomputedText. Additionally, if a {@link MetricAffectingSpan} implements
     * {@link NoCopySpan}, it will not affect the measurement results.
     * <p>
     * Note that calling this method with a {@link PrecomputedText} and its originating
     * {@link Params} still yields a new {@link PrecomputedText} object. But only its internal
     * span data is cloned, allowing you to attach span watchers without affecting each other,
     * and both the text (string) itself and measurement information will be reused.
     *
     * @param text   the text to be measured
     * @param params parameters that define how text will be precomputed
     * @return A {@link PrecomputedText}
     */
    @NonNull
    public static PrecomputedText create(@NonNull CharSequence text, @NonNull Params params) {
        ParagraphInfo[] paraInfo = null;
        if (text instanceof final PrecomputedText hintPct) {
            final PrecomputedText.Params hintParams = hintPct.getParams();
            final @Params.CheckResultUsableResult int checkResult =
                    hintParams.checkResultUsable(params.mPaint, params.mTextDir,
                            params.mLineBreakConfig);
            switch (checkResult) {
                case Params.USABLE:
                    // Modern UI changed: we can't simply return hintPct, instead,
                    // reuse measure results, but copy spans and remove no-copy spans
                    paraInfo = hintPct.getParagraphInfo();
                    break;
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

        return new PrecomputedText(text, params, paraInfo);
    }

    @NonNull
    private static ParagraphInfo[] createMeasuredParagraphsFromPrecomputedText(
            @NonNull PrecomputedText pct, @NonNull Params params, boolean computeLayout) {
        ParagraphInfo[] result = new ParagraphInfo[pct.getParagraphCount()];
        for (int i = 0; i < pct.getParagraphCount(); ++i) {
            final int paraStart = pct.getParagraphStart(i);
            final int paraEnd = pct.getParagraphEnd(i);
            result[i] = new ParagraphInfo(paraEnd, MeasuredParagraph.buildForStaticLayout(
                    params.getTextPaint(), params.getLineBreakConfig(), pct, paraStart, paraEnd,
                    params.getTextDirection(), computeLayout,
                    null /* no recycle */));
        }
        return result;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
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
    private PrecomputedText(@NonNull CharSequence text, @NonNull Params params,
                            @Nullable ParagraphInfo[] paraInfo) {
        super(text);    // <- no-copy spans will be removed
        if (paraInfo == null) {
            // Modern UI fixed: google bug, if the source text has NoCopy-MetricAffectingSpan,
            // then measure results from 'text' and 'this' are different.
            // Therefore, we use 'this' to measure.
            paraInfo = createMeasuredParagraphs(
                    this, params, 0, length(), true /* computeLayout */);
        }
        mParams = params;
        mParagraphInfo = paraInfo;
    }

    private PrecomputedText(@NonNull CharSequence text, @IntRange(from = 0) int start,
                            @IntRange(from = 0) int end, @NonNull Params params) {
        super(text, start, end);    // <- no-copy spans will be removed
        ParagraphInfo[] paraInfo = createMeasuredParagraphs(
                this, params, 0, length(), true /* computeLayout */);
        mParams = params;
        mParagraphInfo = paraInfo;
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
        return paraIndex == 0 ? 0 : getParagraphEnd(paraIndex - 1);
    }

    /**
     * Returns the paragraph end offset of the text.
     */
    public @IntRange(from = 0) int getParagraphEnd(@IntRange(from = 0) int paraIndex) {
        Objects.checkIndex(paraIndex, getParagraphCount());
        return mParagraphInfo[paraIndex].paragraphEnd;
    }

    /**
     * Returns the measured text of the paragraph of the text.
     */
    public @NonNull MeasuredText getMeasuredText(@IntRange(from = 0) int paraIndex) {
        MeasuredText mt = mParagraphInfo[paraIndex].measured.getMeasuredText();
        assert mt != null;
        return mt;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public @NonNull MeasuredParagraph getMeasuredParagraph(@IntRange(from = 0) int paraIndex) {
        return mParagraphInfo[paraIndex].measured;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public @NonNull ParagraphInfo[] getParagraphInfo() {
        return mParagraphInfo;
    }

    /**
     * Returns value if the given TextPaint gives the same result of text layout for this text.
     * @hidden
     */
    @ApiStatus.Internal
    public @Params.CheckResultUsableResult int checkResultUsable(
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end, @NonNull TextDirectionHeuristic textDir,
            @NonNull TextPaint paint, @NonNull LineBreakConfig lbConfig) {
        if (start != 0 || end != length()) {
            // subrange is not reusable
            return Params.UNUSABLE;
        } else {
            return mParams.checkResultUsable(paint, textDir, lbConfig);
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public int findParaIndex(@IntRange(from = 0) int pos) {
        int low = 0;
        int high = mParagraphInfo.length - 1;
        if (high < 8) {
            // linear
            for (; low <= high; ++low) {
                if (mParagraphInfo[low].paragraphEnd > pos) break;
            }
        } else {
            // upper_bound
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (mParagraphInfo[mid].paragraphEnd > pos) high = mid - 1;
                else low = mid + 1;
            }
        }
        return low;
    }

    /**
     * Returns a width of a character at offset
     *
     * @param offset an offset of the text.
     * @return a width of the character.
     * @hidden
     */
    @ApiStatus.Internal
    public float getCharWidthAt(@IntRange(from = 0) int offset) {
        Objects.checkIndex(offset, length());
        final int paraIndex = findParaIndex(offset);
        final int paraStart = getParagraphStart(paraIndex);
        return getMeasuredText(paraIndex).getAdvance(offset - paraStart);
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
        Objects.checkFromToIndex(start, end, length());

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
        return getMeasuredText(paraIndex).getAdvance(start - paraStart, end - paraStart);
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
        Objects.checkFromToIndex(start, end, length());
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
        getMeasuredText(paraIndex).getExtent(start - paraStart,
                end - paraStart, outMetrics);
    }

    /**
     * Returns the size of PrecomputedText memory usage.
     * <p>
     * Note that this is not guaranteed to be accurate. Must be used only for testing purposes.
     * @hidden
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
    public void setSpan(@NonNull Object span, int start, int end, int flags) {
        if (span instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be set to PrecomputedText.");
        }
        super.setSpan(span, start, end, flags);
    }

    /**
     * @throws IllegalArgumentException if {@link MetricAffectingSpan} is specified.
     */
    @Override
    public void removeSpan(@NonNull Object span) {
        if (span instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be removed from PrecomputedText.");
        }
        super.removeSpan(span);
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @Override
    public void removeSpan(@NonNull Object span, int flags) {
        if (span instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be removed from PrecomputedText.");
        }
        super.removeSpan(span, flags);
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start == 0 && end == length()) {
            // reuse measure results, but copy spans and remove no-copy spans
            return new PrecomputedText(this, mParams, mParagraphInfo);
        }
        // subrange is not reusable, re-measure
        return new PrecomputedText(this, start, end, mParams);
    }
}
