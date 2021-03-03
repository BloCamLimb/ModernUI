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

import com.ibm.icu.text.Bidi;
import icyllis.modernui.text.style.MetricAffectingSpan;
import icyllis.modernui.text.style.ReplacementSpan;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;

@NotThreadSafe
public class MeasuredParagraph {

    private static final Pool<MeasuredParagraph> sPool = Pools.concurrent(1);

    // The casted original text.
    //
    // This may be null if the passed text is not a Spanned.
    @Nullable
    private Spanned mSpanned;

    // The start offset of the target range in the original text (mSpanned);
    private int mTextStart;

    // The length of the target range in the original text.
    private int mTextLength;

    // The copied character buffer for measuring text.
    //
    // The length of this array is mTextLength.
    private char[] mCopiedBuffer;

    // The first paragraph direction. Either Bidi.DIRECTION_LEFT_TO_RIGHT
    // or Bidi.DIRECTION_RIGHT_TO_LEFT
    private int mParaDir;

    // True if the text is LTR direction and doesn't contain any bidi characters.
    private boolean mLtrWithoutBidi;

    // The bidi level for individual characters.
    //
    // This is empty if mLtrWithoutBidi is true.
    @Nonnull
    private final ByteArrayList mLevels = new ByteArrayList();

    /*@Deprecated
    @Nonnull
    private final FloatArrayList mAdvances = new FloatArrayList();*/

    // The span end positions.
    // See getSpanEndCache comments.
    @Nonnull
    private final IntArrayList mSpanEndCache = new IntArrayList();

    // The font metrics.
    // See getFontMetrics comments.
    @Nonnull
    private final IntArrayList mFontMetrics = new IntArrayList();

    //@Nullable
    private MeasuredText mMeasuredText;

    @Nonnull
    private final TextPaint mCachedPaint = new TextPaint();

    private MeasuredParagraph() {
    }

    /**
     * Release internal arrays.
     */
    public void release() {
        reset();
        mLevels.trim();
        //mAdvances.trim();
        mSpanEndCache.trim();
        mFontMetrics.trim();
    }

    /**
     * Resets the internal state for starting new text.
     */
    private void reset() {
        mSpanned = null;
        mCopiedBuffer = null;
        //mWholeWidth = 0;
        mLevels.clear();
        //mAdvances.clear();
        mSpanEndCache.clear();
        mFontMetrics.clear();
        mMeasuredText = null;
    }

    /**
     * Returns the length of the paragraph.
     * <p>
     * This is always available.
     */
    public int getTextLength() {
        return mTextLength;
    }

    /**
     * Returns the characters to be measured. This will be the same value
     * as {@link MeasuredText#getTextBuf()} if {@link #getMeasuredText()} available.
     * <p>
     * This is always available.
     */
    @Nonnull
    public char[] getChars() {
        return mCopiedBuffer;
    }

    /**
     * Returns the first paragraph direction. Either {@link Bidi#DIRECTION_LEFT_TO_RIGHT}
     * or {@link Bidi#DIRECTION_RIGHT_TO_LEFT)
     * <p>
     * This is always available.
     */
    public int getParagraphDir() {
        return mParaDir;
    }

    /**
     * Returns the MetricsAffectingSpan end indices.
     * <p>
     * If the input text is not a spanned string, this has one value that is the length of the text.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout.
     * Returns empty array in other cases.
     */
    @Nonnull
    public IntArrayList getSpanEndCache() {
        return mSpanEndCache;
    }

    /**
     * Returns the int array which holds FontMetrics.
     * <p>
     * This array holds the repeat of ascent, descent of font metrics value.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout.
     * Returns empty array in other cases.
     */
    @Nonnull
    public IntArrayList getFontMetrics() {
        return mFontMetrics;
    }

    /**
     * Returns the result of the MeasuredParagraph.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout.
     * Returns null in other cases.
     */
    public MeasuredText getMeasuredText() {
        return mMeasuredText;
    }

    @Nonnull
    private static MeasuredParagraph obtain() {
        final MeasuredParagraph c = sPool.acquire();
        return c == null ? new MeasuredParagraph() : c;
    }

    /*@Deprecated
    @Nonnull
    private static MeasuredParagraph buildForMeasurement(@Nonnull TextPaint paint, @Nonnull CharSequence text,
                                                         int start, int end, @Nonnull TextDirectionHeuristic dir,
                                                         @Nullable MeasuredParagraph recycle) {
        final MeasuredParagraph c = recycle == null ? obtain() : recycle;
        c.resetAndAnalyzeBidi(text, start, end, dir);
        c.mAdvances.size(c.mTextLength);
        if (c.mTextLength == 0) {
            return c;
        }
        if (c.mSpanned == null) {
            // No style change by MetricsAffectingSpan. Just measure all text.
            c.applyMetricsAffectingSpan(
                    paint, null, start, end, null);
        } else {
            // There may be a MetricsAffectingSpan. Split into span transitions and apply styles.
            int spanEnd;
            for (int spanStart = start; spanStart < end; spanStart = spanEnd) {
                spanEnd = c.mSpanned.nextSpanTransition(spanStart, end, MetricAffectingSpan.class);
                MetricAffectingSpan[] spans = c.mSpanned.getSpans(spanStart, spanEnd,
                        MetricAffectingSpan.class);
                spans = TextUtils.removeEmptySpans(spans, c.mSpanned, MetricAffectingSpan.class);
                c.applyMetricsAffectingSpan(
                        paint, spans, spanStart, spanEnd, null);
            }
        }
        return c;
    }*/

    @Nonnull
    public static MeasuredParagraph buildForStaticLayout(@Nonnull TextPaint paint, @Nonnull CharSequence text,
                                                         int start, int end, @Nonnull TextDirectionHeuristic dir,
                                                         @Nullable MeasuredParagraph recycle) {
        final MeasuredParagraph c = recycle == null ? obtain() : recycle;
        c.resetAndAnalyzeBidi(text, start, end, dir);
        final MeasuredText.Builder builder = new MeasuredText.Builder(c.mCopiedBuffer);
        if (c.mTextLength == 0) {
            //TODO review
            return c;
        } else {
            if (c.mSpanned == null) {
                // No style change by MetricsAffectingSpan. Just measure all text.
                c.applyMetricsAffectingSpan(paint, null /* spans */, start, end, builder);
                c.mSpanEndCache.add(end);
            } else {
                // There may be a MetricsAffectingSpan. Split into span transitions and apply
                // styles.
                int spanEnd;
                for (int spanStart = start; spanStart < end; spanStart = spanEnd) {
                    spanEnd = c.mSpanned.nextSpanTransition(spanStart, end,
                            MetricAffectingSpan.class);
                    MetricAffectingSpan[] spans = c.mSpanned.getSpans(spanStart, spanEnd,
                            MetricAffectingSpan.class);
                    spans = TextUtils.removeEmptySpans(spans, c.mSpanned,
                            MetricAffectingSpan.class);
                    c.applyMetricsAffectingSpan(paint, spans, spanStart, spanEnd, builder);
                    c.mSpanEndCache.add(spanEnd);
                }
            }
            c.mMeasuredText = builder.build();
        }
        return c;
    }

    private void resetAndAnalyzeBidi(@Nonnull CharSequence text, int start, int end, @Nonnull TextDirectionHeuristic dir) {
        reset();
        mSpanned = text instanceof Spanned ? (Spanned) text : null;
        mTextStart = start;
        mTextLength = end - start;

        if (mCopiedBuffer == null || mCopiedBuffer.length != mTextLength) {
            mCopiedBuffer = new char[mTextLength];
        }
        TextUtils.getChars(text, start, end, mCopiedBuffer, 0);

        // Replace characters associated with ReplacementSpan to U+FFFC.
        if (mSpanned != null) {
            final ReplacementSpan[] spans = mSpanned.getSpans(start, end, ReplacementSpan.class);
            for (ReplacementSpan span : spans) {
                int startInPara = mSpanned.getSpanStart(span) - start;
                int endInPara = mSpanned.getSpanEnd(span) - start;
                // The span interval may be larger and must be restricted to [start, end)
                if (startInPara < 0) startInPara = 0;
                if (endInPara > mTextLength) endInPara = mTextLength;
                Arrays.fill(mCopiedBuffer, startInPara, endInPara, '\uFFFC');
            }
        }

        if ((dir == TextDirectionHeuristics.LTR
                || dir == TextDirectionHeuristics.FIRSTSTRONG_LTR
                || dir == TextDirectionHeuristics.ANYRTL_LTR)
                && !Bidi.requiresBidi(mCopiedBuffer, 0, mTextLength)) {
            mLevels.clear();
            mParaDir = Bidi.DIRECTION_LEFT_TO_RIGHT;
            mLtrWithoutBidi = true;
        } else {
            final byte paraLevel;
            if (dir == TextDirectionHeuristics.LTR) {
                paraLevel = Bidi.LTR;
            } else if (dir == TextDirectionHeuristics.RTL) {
                paraLevel = Bidi.RTL;
            } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
                paraLevel = Bidi.LEVEL_DEFAULT_LTR;
            } else if (dir == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
                paraLevel = Bidi.LEVEL_DEFAULT_RTL;
            } else {
                final boolean isRtl = dir.isRtl(mCopiedBuffer, 0, mTextLength);
                paraLevel = isRtl ? Bidi.RTL : Bidi.LTR;
            }
            mLevels.size(mTextLength);
            final Bidi icuBidi = new Bidi(mTextLength, 0);
            icuBidi.setPara(mCopiedBuffer, paraLevel, null);
            for (int i = 0; i < mTextLength; i++) {
                mLevels.set(i, icuBidi.getLevelAt(i));
            }
            // odd numbers indicate RTL
            mParaDir = (icuBidi.getParaLevel() & 0x1) == 0 ? Bidi.DIRECTION_LEFT_TO_RIGHT : Bidi.DIRECTION_RIGHT_TO_LEFT;
            mLtrWithoutBidi = false;
        }
    }

    private void applyMetricsAffectingSpan(@Nonnull TextPaint paint, @Nullable MetricAffectingSpan[] spans,
                                           int start, int end, @Nonnull MeasuredText.Builder builder) {
        mCachedPaint.set(paint);

        ReplacementSpan replacement = null;
        if (spans != null) {
            for (MetricAffectingSpan span : spans) {
                if (span instanceof ReplacementSpan) {
                    // The last ReplacementSpan is effective for backward compatibility reasons.
                    replacement = (ReplacementSpan) span;
                } else {
                    span.updateMeasureState(mCachedPaint);
                }
            }
        }

        final int runStart = start - mTextStart;
        final int runEnd = end - mTextStart;

        final long fontMetrics = mCachedPaint.getFontMetrics();

        if (replacement != null) {
            applyReplacementRun(replacement, runStart, runEnd, builder);
        } else {
            applyStyleRun(runStart, runEnd, builder);
        }

        mFontMetrics.add((int) (fontMetrics >> 32));
        mFontMetrics.add((int) fontMetrics);
    }

    private void applyReplacementRun(@Nonnull ReplacementSpan replacement, int start, int end,
                                     @Nonnull MeasuredText.Builder builder) {
        //TODO get replacement width
        builder.addReplacementRun(mCachedPaint, end - start, 0);
    }

    private void applyStyleRun(int start, int end, @Nonnull MeasuredText.Builder builder) {
        if (mLtrWithoutBidi) {
            // If the whole text is LTR direction, just apply whole region.
            builder.addStyleRun(mCachedPaint, end - start, false);
        } else {
            // If there is multiple bidi levels, split into individual bidi level and apply style.
            byte level = mLevels.getByte(start);
            // Note that the empty text or empty range won't reach this method.
            // Safe to search from start + 1.
            for (int levelStart = start, levelEnd = start + 1; ; ++levelEnd) {
                if (levelEnd == end || mLevels.getByte(levelEnd) != level) { // bidi run
                    final boolean isRtl = (level & 0x1) != 0;
                    builder.addStyleRun(mCachedPaint, levelEnd - levelStart, isRtl);
                    if (levelEnd == end) {
                        break;
                    }
                    levelStart = levelEnd;
                    level = mLevels.getByte(levelEnd);
                }
            }
        }
    }

    /**
     * Recycle the MeasuredParagraph.
     */
    public void recycle() {
        release();
        sPool.release(this);
    }
}
