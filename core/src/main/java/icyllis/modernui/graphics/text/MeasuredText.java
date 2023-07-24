/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import icyllis.modernui.annotation.*;
import icyllis.modernui.text.TextPaint;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Text shaping result object for multi-style text, so there are multiple style runs
 * of positioned glyphs.
 * <p>
 * This object is immutable, internal buffers may be shared between threads.
 *
 * @see ShapedText
 * @see LayoutCache
 */
@Immutable
public class MeasuredText {

    private final char[] mTextBuf;
    private final Run[] mRuns;

    private MeasuredText(@NonNull char[] textBuf, @NonNull Run[] runs, boolean computeLayout) {
        //assert (textBuf.length == 0) == (runs.length == 0);
        mTextBuf = textBuf;
        mRuns = runs;
        for (Run run : runs) {
            run.measure(textBuf, computeLayout);
        }
    }

    /**
     * Returns the text buffer. Elements may change if recycled at higher level,
     * keep it synchronized with MeasuredText.
     *
     * @return the backend buffer of the text
     */
    @NonNull
    public char[] getTextBuf() {
        return mTextBuf;
    }

    /**
     * Returns runs of text. Successive style runs may remain the same font paint
     * under optimization consideration.
     *
     * @return all text runs, may empty if text buf is empty
     */
    @NonNull
    public Run[] getRuns() {
        return mRuns;
    }

    /**
     * Expands the font metrics with those of the chars in the given range of the text buffer.
     *
     * @param start  the start index
     * @param end    the end index
     * @param extent receives the metrics
     */
    public void getExtent(int start, int end, @NonNull FontMetricsInt extent) {
        if (start >= end) {
            return;
        }
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
     * @return advance
     */
    public float getAdvance(int pos) {
        Run run = searchRun(pos);
        if (run == null) {
            return 0f;
        }
        return run.getAdvance(pos);
    }

    /**
     * Returns the advance of the chars in the given range of the text buffer.
     *
     * @param start the start index
     * @param end   the end index
     * @return advance
     * @see #getAdvance(int)
     */
    public float getAdvance(int start, int end) {
        if (start >= end) {
            return 0;
        }
        float a = 0;
        for (Run run : mRuns) {
            if (start < run.mEnd && end > run.mStart) {
                a += run.getAdvance(Math.max(start, run.mStart), Math.min(end, run.mEnd));
            }
        }
        return a;
    }

    /**
     * Returns the layout piece for a single style run with the given range
     * <strong>only for rendering purposes</strong>.
     *
     * @param start start of range
     * @param end   end of range
     * @return the layout or nothing to draw with characters
     */
    @Nullable
    public LayoutPiece getLayoutPiece(int start, int end) {
        if (start >= end) {
            return null;
        }
        Run run = searchRun(start);
        if (run != null) {
            return run.getLayout(mTextBuf, start, end);
        }
        return null;
    }

    /**
     * Binary search with ranges.
     *
     * @param pos char index
     * @return the run index
     */
    public int search(int pos) {
        if (pos < 0 || pos >= mTextBuf.length) {
            return -1;
        }
        int low = 0;
        int high = mRuns.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Run run = mRuns[mid];

            if (run.mEnd <= pos)
                low = mid + 1;
            else if (run.mStart > pos)
                high = mid - 1;
            else
                return mid;
        }
        return -(low + 1);
    }

    /**
     * Binary search with ranges.
     *
     * @param pos char index
     * @return the run
     */
    @Nullable
    public Run searchRun(int pos) {
        if (pos < 0 || pos >= mTextBuf.length) {
            return null;
        }
        int low = 0;
        int high = mRuns.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Run run = mRuns[mid];

            if (run.mEnd <= pos)
                low = mid + 1;
            else if (run.mStart > pos)
                high = mid - 1;
            else
                return run;
        }
        return null;
    }

    /**
     * Note: The text buffer is not within the calculation range.
     *
     * @return memory usage in bytes
     */
    public int getMemoryUsage() {
        int size = 12 + 8 + 8 + 16;
        for (Run run : mRuns) {
            size += run.getMemoryUsage();
        }
        return size;
    }

    @Override
    public String toString() {
        return "MeasuredText{" +
                Arrays.stream(mRuns).map(Objects::toString).collect(Collectors.joining("\n")) +
                '}';
    }

    /**
     * For creating a MeasuredText.
     */
    @NotThreadSafe
    public static class Builder {

        private final List<Run> mRuns = new ArrayList<>();

        @NonNull
        private final char[] mText;
        private boolean mComputeLayout = true;
        private int mCurrentOffset = 0;

        /**
         * Construct a builder.
         * <p>
         * The MeasuredText returned by build method will hold a reference of the text,
         * then the char array <b>must</b> be immutable.
         *
         * @param text a text, with full range, can be empty
         */
        public Builder(@NonNull char[] text) {
            Objects.requireNonNull(text);
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
        @NonNull
        public Builder appendStyleRun(@NonNull TextPaint paint,
                                      @IntRange(from = 0) int length,
                                      boolean isRtl) {
            addStyleRun(paint.createInternalPaint(), length, isRtl);
            return this;
        }

        public void addStyleRun(@NonNull FontPaint paint, @IntRange(from = 0) int length,
                                boolean isRtl) {
            Objects.requireNonNull(paint);
            if (length <= 0) {
                throw new IllegalArgumentException("length can not be negative");
            }
            final int end = mCurrentOffset + length;
            if (end > mText.length) {
                throw new IllegalArgumentException("Style exceeds the text length");
            }
            mRuns.add(new StyleRun(mCurrentOffset, end, paint, isRtl));
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
        @NonNull
        public Builder appendReplacementRun(@NonNull TextPaint paint,
                                            @IntRange(from = 0) int length,
                                            @FloatRange(from = 0) float width) {
            addReplacementRun(paint.getTextLocale(), length, width);
            return this;
        }

        public void addReplacementRun(@NonNull Locale locale, @IntRange(from = 0) int length,
                                      @FloatRange(from = 0) float width) {
            if (length <= 0) {
                throw new IllegalArgumentException("length can not be negative");
            }
            final int end = mCurrentOffset + length;
            if (end > mText.length) {
                throw new IllegalArgumentException("Replacement exceeds the text length");
            }
            mRuns.add(new ReplacementRun(mCurrentOffset, end, width, locale));
            mCurrentOffset = end;
        }

        /**
         * By passing true to this method, the build method will compute all full layout
         * information.
         * <p>
         * If you don't want to render the text soon, you can pass false to this method and
         * save the memory spaces. The default value is true.
         * <p>
         * Even if you pass false to this method, you can still render the text but it becomes
         * slower.
         *
         * @param computeLayout true if you want to retrieve full layout info, e.g. glyphs position
         */
        @NonNull
        public Builder setComputeLayout(boolean computeLayout) {
            mComputeLayout = computeLayout;
            return this;
        }

        /**
         * Starts laying-out the text and creates a MeasuredText for the result.
         * <p>
         * Once you called this method, you can't touch this Builder again.
         *
         * @return text measurement result
         */
        @NonNull
        public MeasuredText build() {
            if (mCurrentOffset < 0) {
                throw new IllegalStateException("Builder can not be reused.");
            }
            if (mCurrentOffset != mText.length) {
                throw new IllegalStateException("Style info has not been provided for all text.");
            }
            mCurrentOffset = -1;
            return new MeasuredText(mText, mRuns.toArray(new Run[0]), mComputeLayout);
        }
    }

    // a logical run, subrange of bidi run
    public static abstract class Run {

        // range in context
        public final int mStart;
        public final int mEnd;

        private Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        // Compute metrics
        abstract void measure(@NonNull char[] text, boolean computeLayout);

        // Extend extent
        abstract void getExtent(@NonNull char[] text, int start, int end, @NonNull FontMetricsInt extent);

        @Nullable
        abstract LayoutPiece getLayout(@NonNull char[] text, int start, int end);

        abstract float getAdvance(int pos);

        abstract float getAdvance(int start, int end);

        // Returns true if this run is RTL. Otherwise returns false.
        public abstract boolean isRtl();

        // Returns true if this run can be broken into multiple pieces for line breaking.
        public abstract boolean canBreak();

        // Returns the locale for this run.
        @NonNull
        public abstract Locale getLocale();

        public abstract int getMemoryUsage();
    }

    public static class StyleRun extends Run {

        // maybe a shared pointer, but its contents must be immutable (read only)
        public final FontPaint mPaint;
        private final boolean mIsRtl;

        // obtained from cache or newly created, but may be removed from cache later
        private LayoutPiece mLayoutPiece;
        private boolean mComputedLayout;

        private StyleRun(int start, int end, FontPaint paint, boolean isRtl) {
            super(start, end);
            mPaint = paint;
            mIsRtl = isRtl;
        }

        @Override
        void measure(@NonNull char[] text, boolean computeLayout) {
            mLayoutPiece = LayoutCache.getOrCreate(text, mStart, mEnd, mStart, mEnd, mIsRtl, mPaint,
                    LayoutCache.COMPUTE_CLUSTER_ADVANCES);
            mComputedLayout = computeLayout;
        }

        @Override
        void getExtent(@NonNull char[] text, int start, int end,
                       @NonNull FontMetricsInt extent) {
            if (start == mStart && end == mEnd) {
                mLayoutPiece.getExtent(extent);
            } else {
                LayoutCache.getOrCreate(text, mStart, mEnd, start, end, mIsRtl, mPaint,
                        LayoutCache.COMPUTE_CLUSTER_ADVANCES).getExtent(extent);
            }
        }

        @Override
        LayoutPiece getLayout(@NonNull char[] text, int start, int end) {
            if (start == mStart && end == mEnd) {
                if (!mComputedLayout) {
                    mLayoutPiece = LayoutCache.getOrCreate(text, mStart, mEnd, start, end, mIsRtl, mPaint,
                            LayoutCache.COMPUTE_CLUSTER_ADVANCES);
                    mComputedLayout = true;
                }
                return mLayoutPiece;
            }
            return LayoutCache.getOrCreate(text, mStart, mEnd, start, end, mIsRtl, mPaint,
                    LayoutCache.COMPUTE_CLUSTER_ADVANCES);
        }

        @Override
        float getAdvance(int pos) {
            return mLayoutPiece.getAdvances()[pos - mStart];
        }

        @Override
        float getAdvance(int start, int end) {
            if (start == mStart && end == mEnd) {
                return mLayoutPiece.getAdvance();
            }
            float a = 0;
            for (int i = start - mStart, e = end - mStart; i < e; i++) {
                a += mLayoutPiece.getAdvances()[i];
            }
            return a;
        }

        @Override
        public boolean isRtl() {
            return mIsRtl;
        }

        @Override
        public boolean canBreak() {
            return true;
        }

        @NonNull
        @Override
        public Locale getLocale() {
            return mPaint.getLocale();
        }

        @Override
        public int getMemoryUsage() {
            // 12 + 4 + 4 + (12 + 8 + 8 + 4 + 4) + 1 + 1 + 8
            // here assumes paint is partially shared (one third)
            // don't worry layout piece is null, see MeasuredText constructor
            return 48 + mLayoutPiece.getMemoryUsage();
        }

        @Override
        public String toString() {
            return "StyleRun{" +
                    "mStart=" + mStart +
                    ", mEnd=" + mEnd +
                    ", mPaint=" + mPaint +
                    ", mIsRtl=" + mIsRtl +
                    ", mLayoutPiece=" + mLayoutPiece +
                    '}';
        }
    }

    public static class ReplacementRun extends Run {

        private final float mWidth;
        private final Locale mLocale;

        private ReplacementRun(int start, int end, float width, Locale locale) {
            super(start, end);
            mWidth = width;
            mLocale = locale;
        }

        @Override
        void measure(@NonNull char[] text, boolean computeLayout) {
            //TODO: Get the extents information from the caller.
        }

        @Override
        void getExtent(@NonNull char[] text, int start, int end, @NonNull FontMetricsInt extent) {

        }

        @Nullable
        @Override
        LayoutPiece getLayout(@NonNull char[] text, int start, int end) {
            return null;
        }

        @Override
        float getAdvance(int pos) {
            if (pos == mStart) {
                return mWidth;
            }
            return 0;
        }

        @Override
        float getAdvance(int start, int end) {
            if (start <= mStart) {
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

        @NonNull
        @Override
        public Locale getLocale() {
            return mLocale;
        }

        @Override
        public int getMemoryUsage() {
            // 12 + 4 + 4 + 8 + 4
            return 32;
        }

        @Override
        public String toString() {
            return "ReplacementRun{" +
                    "mStart=" + mStart +
                    ", mEnd=" + mEnd +
                    ", mWidth=" + mWidth +
                    ", mLocale=" + mLocale +
                    '}';
        }
    }
}
