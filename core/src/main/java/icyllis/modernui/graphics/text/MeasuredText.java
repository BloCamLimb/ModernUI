/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.FloatRange;
import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.util.AlgorithmUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Text shaping result object for multi-style text, so there are multiple style runs
 * of positioned glyphs.
 *
 * @see ShapedText
 * @see LayoutCache
 */
public class MeasuredText {

    private final char[] mTextBuf;
    private final Run[] mRuns;
    private final float[] mAdvances;

    private MeasuredText(@NonNull char[] textBuf,
                         @NonNull Run[] runs,
                         boolean computeLayout) {
        //assert (textBuf.length == 0) == (runs.length == 0);
        final float[] advances;

        if (runs.length > 0) {
            advances = new float[textBuf.length];
            for (Run run : runs) {
                run.measure(textBuf, advances, computeLayout);
            }
        } else {
            advances = null;
        }

        mTextBuf = textBuf;
        // put final fields here to safely publish measure results
        mRuns = runs;
        mAdvances = advances;
    }

    /**
     * Returns the text buffer. Elements may change if recycled at higher level,
     * keep it synchronized with MeasuredText.
     *
     * @return the backend buffer of the text
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public char[] getTextBuf() {
        return mTextBuf;
    }

    /**
     * Returns runs of text. Successive style runs may remain the same font paint
     * under optimization consideration.
     *
     * @return all text runs, may empty if text buf is empty
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public Run[] getRuns() {
        return mRuns;
    }

    /**
     * Returns the advance of the char at the given index of the text buffer.
     * This is per-cluster advance and font-dependent.
     *
     * @param pos the char index
     * @return advance
     */
    public float getAdvance(int pos) {
        return mAdvances[pos];
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
        float advance = 0;
        for (int i = start; i < end; i++) {
            advance += mAdvances[i];
        }
        return advance;
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
        int index = findRunIndex(start);
        Run run = mRuns[index];
        for (;;) {
            if (start < run.mEnd && end > run.mStart) {
                run.getExtent(mTextBuf,
                        Math.max(start, run.mStart),
                        Math.min(end, run.mEnd),
                        extent);
            }
            if (run.mEnd >= end) {
                break;
            }
            run = mRuns[++index];
        }
    }

    /**
     * Find the run for the given char index.
     *
     * @param pos char index
     * @return the run
     * @hidden
     */
    @ApiStatus.Internal
    public int findRunIndex(int pos) {
        int low = 0;
        int high = mRuns.length - 1;
        if (high < 8) {
            // linear
            for (; low <= high; ++low) {
                if (mRuns[low].mEnd > pos) break;
            }
        } else {
            // upper_bound
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (mRuns[mid].mEnd > pos) high = mid - 1;
                else low = mid + 1;
            }
        }
        return low;
    }

    /**
     * Note: The text buffer is not within the calculation range.
     *
     * @return memory usage in bytes
     */
    public int getMemoryUsage() {
        int size = 12 + 8 + 8 + 8 + 16;
        for (Run run : mRuns) {
            size += run.getMemoryUsage();
        }
        if (mAdvances != null) {
            size += 16 + (mAdvances.length << 2);
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
        public Builder appendStyleRun(@NonNull FontPaint paint,
                                      @IntRange(from = 0) int length,
                                      boolean isRtl) {
            addStyleRun(new FontPaint(paint), null, length, isRtl);
            return this;
        }

        /**
         * Apply styles to the given length.
         * <p>
         * Keeps an internal offset which increases at every append. The initial value for this
         * offset is zero. After the style is applied the internal offset is moved to {@code offset
         * + length}, and next call will start from this new position.
         *
         * @param paint           a paint
         * @param lineBreakConfig a line break configuration.
         * @param length          a length to be applied with a given paint, can not exceed the length of the
         *                        text
         * @param isRtl           true if the text is in RTL context, otherwise false.
         */
        @NonNull
        public Builder appendStyleRun(@NonNull FontPaint paint,
                                      @Nullable LineBreakConfig lineBreakConfig,
                                      @IntRange(from = 0) int length,
                                      boolean isRtl) {
            addStyleRun(new FontPaint(paint), lineBreakConfig, length, isRtl);
            return this;
        }

        /**
         * Internal method that does not copy the paint. Caller must ensure that it is immutable.
         *
         * @hidden
         */
        @ApiStatus.Internal
        public void addStyleRun(@NonNull FontPaint paint,
                                @Nullable LineBreakConfig lineBreakConfig,
                                @IntRange(from = 0) int length,
                                boolean isRtl) {
            Objects.requireNonNull(paint);
            if (length <= 0) {
                throw new IllegalArgumentException("length can not be negative");
            }
            final int end = mCurrentOffset + length;
            if (end > mText.length) {
                throw new IllegalArgumentException("Style exceeds the text length");
            }
            int lbStyle = (lineBreakConfig != null) ? lineBreakConfig.getLineBreakStyle() :
                    LineBreakConfig.LINE_BREAK_STYLE_NONE;
            int lbWordStyle = (lineBreakConfig != null) ? lineBreakConfig.getLineBreakWordStyle() :
                    LineBreakConfig.LINE_BREAK_WORD_STYLE_NONE;
            mRuns.add(new StyleRun(mCurrentOffset, end, paint, lbStyle, lbWordStyle, isRtl));
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
        public Builder appendReplacementRun(@NonNull FontPaint paint,
                                            @IntRange(from = 0) int length,
                                            @FloatRange(from = 0) float width) {
            addReplacementRun(paint.getLocale(), length, width);
            return this;
        }

        @ApiStatus.Internal
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
         * information, which is for long-lived MeasuredText.
         *
         * @param computeLayout true if you want to retrieve full layout info
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

    /**
     * A logical run, subrange of bidi run.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static abstract class Run {

        // range in context
        public final int mStart;
        public final int mEnd;

        private Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        // Compute metrics
        public abstract void measure(@NonNull char[] text, @NonNull float[] advances, boolean computeLayout);

        // Extend extent
        public abstract void getExtent(@NonNull char[] text, int start, int end, @NonNull FontMetricsInt extent);

        // Returns true if this run is RTL. Otherwise returns false.
        public abstract boolean isRtl();

        // Returns true if this run can be broken into multiple pieces for line breaking.
        public abstract boolean canBreak();

        // Returns the locale for this run.
        @NonNull
        public abstract Locale getLocale();

        public abstract int getLineBreakStyle();

        public abstract int getLineBreakWordStyle();

        public abstract int getMemoryUsage();
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final class StyleRun extends Run {

        // maybe a shared pointer, but its contents must be immutable (read only)
        private final FontPaint mPaint;
        private final int mLineBreakStyle;
        private final int mLineBreakWordStyle;
        private final boolean mIsRtl;

        // end (exclusive) offsets of each piece
        private int[] mOffsets;
        private LayoutPiece[] mPieces;

        private float mAdvance;

        private StyleRun(int start, int end, FontPaint paint,
                         int lineBreakStyle, int lineBreakWordStyle,
                         boolean isRtl) {
            super(start, end);
            mPaint = paint;
            mLineBreakStyle = lineBreakStyle;
            mLineBreakWordStyle = lineBreakWordStyle;
            mIsRtl = isRtl;
        }

        @Override
        public void measure(@NonNull char[] text, @NonNull float[] advances,
                            boolean computeLayout) {
            ArrayList<LayoutPiece> pieces;
            IntArrayList offsets;
            ShapedText.RunConsumer builder;
            if (computeLayout) {
                pieces = new ArrayList<>();
                offsets = new IntArrayList();
                builder = (piece, start, end, isRtl, paint, offsetX) -> {
                    pieces.add(piece);
                    offsets.add(end);
                };
            } else {
                pieces = null;
                offsets = null;
                builder = null;
            }

            mAdvance = ShapedText.doLayoutRun(text,
                    mStart, mEnd,
                    mStart, mEnd,
                    mIsRtl, mPaint,
                    0,
                    advances, 0.0F,
                    null, builder);

            // save full layout for fast lookup
            if (computeLayout) {
                if (mIsRtl) {
                    // reverse
                    mPieces = new LayoutPiece[pieces.size()];
                    for (int i = 0; i < mPieces.length; i++) {
                        mPieces[i] = pieces.get(mPieces.length - i - 1);
                    }
                    mOffsets = new int[offsets.size()];
                    for (int i = 0; i < mOffsets.length; i++) {
                        mOffsets[i] = offsets.getInt(mOffsets.length - i - 1);
                    }
                } else {
                    mPieces = pieces.toArray(new LayoutPiece[0]);
                    mOffsets = offsets.toIntArray();
                }
            }
        }

        @Override
        public void getExtent(@NonNull char[] text, int start, int end,
                              @NonNull FontMetricsInt extent) {
            if (mPieces == null) {
                ShapedText.doLayoutRun(text, mStart, mEnd,
                        start, end, mIsRtl,
                        mPaint, 0, null, 0.0F,
                        extent, null);
                return;
            }
            final int[] offsets = mOffsets;
            final LayoutPiece[] pieces = mPieces;
            int i;
            // the word context range is the same as StyleRun's context range
            int itContextStart;
            int itContextEnd;
            if (start < offsets[0]) {
                i = 0;
                itContextStart = mStart;
                itContextEnd = offsets[0];
            } else {
                i = AlgorithmUtils.higher(offsets, start);
                itContextStart = offsets[i - 1];
                itContextEnd = i == offsets.length ? mEnd : offsets[i];
            }
            for (;;) {
                int itPieceStart = Math.max(itContextStart, start);
                int itPieceEnd = Math.min(itContextEnd, end);
                if (itPieceStart == itContextStart &&
                        itPieceEnd == itContextEnd) {
                    pieces[i].getExtent(extent);
                } else {
                    LayoutCache.getOrCreate(
                                    text,
                                    itContextStart,
                                    itContextEnd,
                                    itPieceStart,
                                    itPieceEnd,
                                    mIsRtl,
                                    mPaint,
                                    0)
                            .getExtent(extent);
                }
                if (itPieceEnd == end) {
                    break;
                }
                itContextStart = itContextEnd;
                itContextEnd = offsets[++i];
            }
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
        public int getLineBreakStyle() {
            return mLineBreakStyle;
        }

        @Override
        public int getLineBreakWordStyle() {
            return mLineBreakWordStyle;
        }

        @Override
        public int getMemoryUsage() {
            // 12 + 4 + 4 + (12 + 8 + 8 + 4 + 4) + 1 + 1 + 8
            // here assumes paint is partially shared (one third)
            int size = 40 + 8;
            if (mPieces != null) {
                size += 16 + 16;
                size += (mOffsets.length << 2);
                for (var piece : mPieces) {
                    size += piece.getMemoryUsage();
                }
            }
            return size;
        }

        @Override
        public String toString() {
            return "StyleRun{" +
                    "mPaint=" + mPaint +
                    ", mLineBreakStyle=" + mLineBreakStyle +
                    ", mLineBreakWordStyle=" + mLineBreakWordStyle +
                    ", mIsRtl=" + mIsRtl +
                    ", mAdvance=" + mAdvance +
                    ", mStart=" + mStart +
                    ", mEnd=" + mEnd +
                    '}';
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final class ReplacementRun extends Run {

        private final float mWidth;
        private final Locale mLocale;

        private ReplacementRun(int start, int end, float width, Locale locale) {
            super(start, end);
            mWidth = width;
            mLocale = locale;
        }

        @Override
        public void measure(@NonNull char[] text, @NonNull float[] advances,
                            boolean computeLayout) {
            advances[mStart] = mWidth;
        }

        @Override
        public void getExtent(@NonNull char[] text, int start, int end, @NonNull FontMetricsInt extent) {
            //TODO: Get the extents information from the caller.
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
        public int getLineBreakStyle() {
            return LineBreakConfig.LINE_BREAK_STYLE_NONE;
        }

        @Override
        public int getLineBreakWordStyle() {
            return LineBreakConfig.LINE_BREAK_WORD_STYLE_NONE;
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
