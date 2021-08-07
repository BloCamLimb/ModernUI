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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * For the result of text shaping and layout of the single paragraph.
 */
@ThreadSafe
public class MeasuredText {

    private final char[] mTextBuf;
    private final Run[] mRuns;

    private MeasuredText(@Nonnull char[] textBuf, @Nonnull Run[] runs) {
        mTextBuf = textBuf;
        mRuns = runs;
        if (textBuf.length != 0) {
            for (Run run : runs) {
                run.measure(textBuf);
            }
        }
    }

    /**
     * @return the backend buffer of the text, elements may change if recycled at higher level
     */
    @Nonnull
    public char[] getTextBuf() {
        return mTextBuf;
    }

    @Nonnull
    public Run[] getRuns() {
        return mRuns;
    }

    public void getExtent(int start, int end, FontMetricsInt extent) {
        for (Run run : mRuns) {
            if (start < run.mEnd && end > run.mStart) {
                run.getExtent(mTextBuf, Math.max(start, run.mStart), Math.min(end, run.mEnd), extent);
            }
        }
    }

    /**
     * Returns the advance of the char at the given index of the text buffer.
     * <p>
     * This follows grapheme cluster break. For example: there are 6 chars (uint_16),
     * the first two are the first grapheme, the last four are the second one.
     * Then mAdvances[0] is for the first grapheme, mAdvances[2] for the second one,
     * other elements are zero. It's in the same order of {@link #getTextBuf()}
     *
     * @param pos the char index
     */
    public float getAdvanceAt(int pos) {
        Run run = search(pos);
        if (run == null) {
            return 0;
        }
        return run.getAdvanceAt(pos);
    }

    /**
     * Returns the layout piece for a single style run with the given range.
     *
     * @param start start of range
     * @param end   end of range
     * @return the layout or nothing to draw
     */
    @Nullable
    public LayoutPiece getLayoutPiece(int start, int end) {
        Run run = search(start);
        if (run != null) {
            return run.getLayout(mTextBuf, start, end);
        }
        return null;
    }

    // binary search with ranges
    @Nullable
    private Run search(int pos) {
        int low = 0;
        int high = mRuns.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Run run = mRuns[mid];

            if (run.mEnd < pos)
                low = mid + 1;
            else if (run.mStart > pos)
                high = mid - 1;
            else
                return run;
        }
        return null;
    }

    public int getMemoryUsage() {
        int size = 12 + 8 + 8 + 16;
        for (Run run : mRuns) {
            size += run.getMemoryUsage();
        }
        return size;
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
            if (length <= 0) {
                throw new IllegalArgumentException("length can not be negative");
            }
            final int end = mCurrentOffset + length;
            if (end > mText.length) {
                throw new IllegalArgumentException("Style exceeds the text length");
            }
            mRuns.add(new StyleRun(mCurrentOffset, end, paint.copyAsBase(), isRtl));
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
            if (length <= 0) {
                throw new IllegalArgumentException("length can not be negative");
            }
            final int end = mCurrentOffset + length;
            if (end > mText.length) {
                throw new IllegalArgumentException("Replacement exceeds the text length");
            }
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
            if (mCurrentOffset < 0) {
                throw new IllegalStateException("Builder can not be reused.");
            }
            if (mCurrentOffset != mText.length) {
                throw new IllegalStateException("Style info has not been provided for all text.");
            }
            mCurrentOffset = -1;
            return new MeasuredText(mText, mRuns.toArray(new Run[0]));
        }
    }

    // a logical run, subrange of bidi run
    public static abstract class Run {

        // range in context
        public final int mStart;
        public final int mEnd;

        public Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        // Compute metrics
        public abstract void measure(@Nonnull char[] text);

        // Extend extent
        public abstract void getExtent(@Nonnull char[] text, int start, int end,
                                       @Nonnull FontMetricsInt extent);

        @Nullable
        public abstract LayoutPiece getLayout(@Nonnull char[] text, int start, int end);

        public abstract float getAdvanceAt(int pos);

        // Returns true if this run is RTL. Otherwise returns false.
        public abstract boolean isRtl();

        // Returns true if this run can be broken into multiple pieces for line breaking.
        public abstract boolean canBreak();

        // Returns the locale for this run.
        @Nonnull
        public abstract Locale getLocale();

        public abstract int getMemoryUsage();
    }

    public static class StyleRun extends Run {

        public final FontPaint mPaint;
        private final boolean mIsRtl;

        private LayoutPiece mLayoutPiece;

        public StyleRun(int start, int end, FontPaint paint, boolean isRtl) {
            super(start, end);
            mPaint = paint;
            mIsRtl = isRtl;
        }

        @Override
        public void measure(@Nonnull char[] text) {
            mLayoutPiece = LayoutCache.getOrCreate(text, mStart, mEnd, mIsRtl, mPaint);
        }

        @Override
        public void getExtent(@Nonnull char[] text, int start, int end,
                              @Nonnull FontMetricsInt extent) {
            if (start == mStart && end == mEnd) {
                mLayoutPiece.getExtent(extent);
            } else {
                LayoutCache.getOrCreate(text, start, end, mIsRtl, mPaint).getExtent(extent);
            }
        }

        @Override
        public LayoutPiece getLayout(@Nonnull char[] text, int start, int end) {
            if (start == mStart && end == mEnd) {
                return mLayoutPiece;
            }
            return LayoutCache.getOrCreate(text, start, end, mIsRtl, mPaint);
        }

        @Override
        public float getAdvanceAt(int pos) {
            return mLayoutPiece.getAdvances()[pos - mStart];
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

        @Override
        public int getMemoryUsage() {
            // 12 + 4 + 4 + (12 + 8 + 8 + 4 + 4) + 1 + 8
            return 72 + mLayoutPiece.getMemoryUsage();
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
        public void measure(@Nonnull char[] text) {
            //TODO: Get the extents information from the caller.
        }

        @Override
        public void getExtent(@Nonnull char[] text, int start, int end, @Nonnull FontMetricsInt extent) {

        }

        @Nullable
        @Override
        public LayoutPiece getLayout(@Nonnull char[] text, int start, int end) {
            return null;
        }

        @Override
        public float getAdvanceAt(int pos) {
            if (pos == mStart) {
                return mWidth;
            }
            return 0;
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

        @Override
        public int getMemoryUsage() {
            // 12 + 4 + 4 + 8 + 4
            return 32;
        }
    }
}
