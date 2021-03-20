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

import com.google.common.base.Preconditions;
import icyllis.modernui.graphics.font.FontMetricsInt;
import icyllis.modernui.graphics.font.MeasureEngine;
import icyllis.modernui.graphics.font.LayoutPieces;
import icyllis.modernui.graphics.font.MinikinPaint;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Holds the result of text shaping and layout of the single paragraph.
 */
public class MeasuredText {

    @Nonnull
    private final char[] mTextBuf;
    @Nonnull
    public final Run[] mRuns;

    /**
     * Follows grapheme cluster break, for example: there are 6 chars,
     * the first two are the first grapheme, the last four are the second one.
     * Then mAdvances[0] = first grapheme, mAdvances[2] = second grapheme,
     * others are zero. This is in logical order of textBuf.
     */
    public final float[] mAdvances;

    public final LayoutPieces mLayoutPieces = new LayoutPieces();

    private MeasuredText(@Nonnull char[] textBuf, @Nonnull Run[] runs) {
        mTextBuf = textBuf;
        mRuns = runs;
        mAdvances = new float[textBuf.length];
        measure(textBuf);
    }

    private void measure(@Nonnull char[] textBuf) {
        if (textBuf.length == 0)
            return;
        for (Run run : mRuns) {
            run.getMetrics(textBuf, mAdvances, mLayoutPieces);
        }
    }

    // the raw text buf array
    @Nonnull
    public char[] getTextBuf() {
        return mTextBuf;
    }

    public void getExtent(int start, int end, FontMetricsInt extent) {
        for (Run run : mRuns) {
            if (start < run.mEnd && end > run.mStart) {
                run.getExtent(mTextBuf, Math.max(start, run.mStart), Math.min(end, run.mEnd), mLayoutPieces, extent);
            }
        }
    }

    /**
     * For creating a MeasuredText.
     */
    public static class Builder {

        private final List<Run> mRuns = new ArrayList<>();

        @Nonnull
        private final char[] mText;
        private int mCurrentOffset = 0;

        /**
         * Construct a builder.
         * <p>
         * The MeasuredText returned by build method will hold a reference of the text.
         *
         * @param text a text
         */
        public Builder(@Nonnull char[] text) {
            mText = text;
        }

        /**
         * Apply styles to the given length.
         * <p>
         * Keeps an internal offset which increases at every append. The initial value for this
         * offset is zero. After the style is applied the internal offset is moved to {@code offset
         * + length}, and next call will start from this new position.
         *
         * @param paint  a paint
         * @param length a length to be applied with a given paint, can not exceed the length of the
         *               text
         * @param isRtl  true if the text is in RTL context, otherwise false.
         */
        public void addStyleRun(@Nonnull TextPaint paint, int length, boolean isRtl) {
            Preconditions.checkArgument(length > 0, "length can not be negative");
            final int end = mCurrentOffset + length;
            Preconditions.checkArgument(end <= mText.length, "Style exceeds the text length");
            mRuns.add(new StyleRun(mCurrentOffset, end, paint.toMinikin(), isRtl));
            mCurrentOffset = end;
        }

        /**
         * Used to inform the text layout that the given length is replaced with the object of given
         * width.
         * <p>
         * Keeps an internal offset which increases at every append. The initial value for this
         * offset is zero. After the style is applied the internal offset is moved to {@code offset
         * + length}, and next call will start from this new position.
         * <p>
         * Informs the layout engine that the given length should not be processed, instead the
         * provided width should be used for calculating the width of that range.
         *
         * @param paint  a paint
         * @param length a length to be replaced with the object, can not exceed the length of the
         *               text
         * @param width  a replacement width of the range in pixels
         */
        public void addReplacementRun(@Nonnull TextPaint paint, int length, float width) {
            Preconditions.checkArgument(length > 0, "length can not be negative");
            final int end = mCurrentOffset + length;
            Preconditions.checkArgument(end <= mText.length, "Replacement exceeds the text length");
            mRuns.add(new ReplacementRun(mCurrentOffset, end, width, paint.getTextLocale()));
            mCurrentOffset = end;
        }

        /**
         * Starts laying-out the text and creates a MeasuredText for the result.
         * <p>
         * Once you called this method, you can't touch this Builder again.
         *
         * @return text measurement result
         */
        @Nonnull
        public MeasuredText build() {
            Preconditions.checkState(mCurrentOffset >= 0, "Builder can not be reused.");
            Preconditions.checkState(mCurrentOffset == mText.length, "Style info has not been provided for all text.");
            mCurrentOffset = -1;
            return new MeasuredText(mText, mRuns.toArray(new Run[0]));
        }
    }

    // a logical run, child of bidi run
    public static abstract class Run {

        // range in context
        public final int mStart;
        public final int mEnd;

        public Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        // Compute metrics
        public abstract void getMetrics(@Nonnull char[] text, @Nonnull float[] advances,
                                        @Nonnull LayoutPieces outPieces);

        // Extend extent
        public abstract void getExtent(@Nonnull char[] text, int start, int end, @Nonnull LayoutPieces pieces,
                                       @Nonnull FontMetricsInt extent);

        // Returns true if this run is RTL. Otherwise returns false.
        public abstract boolean isRtl();

        // Returns true if this run can be broken into multiple pieces for line breaking.
        public abstract boolean canBreak();

        // Returns the locale for this run.
        @Nonnull
        public abstract Locale getLocale();
    }

    public static class StyleRun extends Run {

        public final MinikinPaint mPaint;
        private final boolean mIsRtl;

        public StyleRun(int start, int end, MinikinPaint paint, boolean isRtl) {
            super(start, end);
            mPaint = paint;
            mIsRtl = isRtl;
        }

        @Override
        public void getMetrics(@Nonnull char[] text, @Nonnull float[] advances, @Nonnull LayoutPieces outPieces) {
            GraphemeBreak.getTextRuns(text, getLocale(), mStart, mEnd,
                    (st, en) -> MeasureEngine.getInstance().create(text, st, en, mPaint, mIsRtl,
                            (lp, pt) -> {
                                advances[st] = lp.mAdvance;
                                outPieces.insert(st, en, lp, mIsRtl, pt);
                            }));
        }

        @Override
        public void getExtent(@Nonnull char[] text, int start, int end, @Nonnull LayoutPieces pieces,
                              @Nonnull FontMetricsInt extent) {
            final int paintId = pieces.findPaintId(mPaint);
            GraphemeBreak.getTextRuns(text, getLocale(), start, end,
                    (st, en) -> pieces.getOrCreate(text, st, en, mPaint, mIsRtl, paintId,
                            (lp, pt) -> extent.extendBy(lp.mAscent, lp.mDescent)));
        }

        @Override
        public boolean isRtl() {
            return mIsRtl;
        }

        @Override
        public boolean canBreak() {
            return true;
        }

        @Nonnull
        @Override
        public Locale getLocale() {
            return mPaint.getTextLocale();
        }
    }

    public static class ReplacementRun extends Run {

        private final float mWidth;
        private final Locale mLocale;

        public ReplacementRun(int start, int end, float width, Locale locale) {
            super(start, end);
            mWidth = width;
            mLocale = locale;
        }

        @Override
        public void getMetrics(@Nonnull char[] text, @Nonnull float[] advances, @Nonnull LayoutPieces outPieces) {
            advances[mStart] = mWidth;
            //TODO: Get the extents information from the caller.
        }

        @Override
        public void getExtent(@Nonnull char[] text, int start, int end, @Nonnull LayoutPieces pieces, @Nonnull FontMetricsInt extent) {

        }

        @Override
        public boolean isRtl() {
            return false;
        }

        @Override
        public boolean canBreak() {
            return false;
        }

        @Nonnull
        @Override
        public Locale getLocale() {
            return mLocale;
        }
    }
}
