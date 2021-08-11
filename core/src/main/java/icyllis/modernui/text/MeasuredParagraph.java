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
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;

/**
 * MeasuredParagraph provides text information for rendering purpose.
 */
@NotThreadSafe
public class MeasuredParagraph {

    private static final Pool<MeasuredParagraph> sPool = Pools.concurrent(1);

    /**
     * The casted original text.
     * This may be null if the passed text is not a Spanned.
     */
    @Nullable
    private Spanned mSpanned;

    /**
     * The start offset of the target range in the original text (mSpanned)
     */
    private int mTextStart;

    /**
     * The copied character buffer for measuring text.
     * The length of this array is that of the target range in the original text.
     */
    private char[] mCopiedBuffer;

    /**
     * The base paragraph direction. Either {@link Bidi#DIRECTION_LEFT_TO_RIGHT}
     * or {@link Bidi#DIRECTION_RIGHT_TO_LEFT}
     */
    private int mParaDir;

    /**
     * The bidi level for individual characters.
     * This is null if the text is LTR direction and doesn't contain any bidi characters.
     */
    @Nullable
    private byte[] mLevels;

    // The span end positions.
    // See getSpanEndCache comments.
    @Nonnull
    private final IntArrayList mSpanEndCache = new IntArrayList();

    // The font metrics.
    // See getFontMetrics comments.
    @Nonnull
    private final IntArrayList mFontMetrics = new IntArrayList();

    // The measurement result.
    @Nullable
    private MeasuredText mMeasuredText;

    @Nonnull
    private final TextPaint mCachedPaint = new TextPaint();
    @Nullable
    private FontMetricsInt mCachedFm;

    private MeasuredParagraph() {
    }

    /**
     * Release internal arrays.
     */
    public void release() {
        reset();
        mSpanEndCache.trim();
        mFontMetrics.trim();
    }

    /**
     * Resets the internal state for starting new text.
     */
    private void reset() {
        mSpanned = null;
        mCopiedBuffer = null;
        mLevels = null;
        mSpanEndCache.clear();
        mFontMetrics.clear();
        mMeasuredText = null;
    }

    /**
     * Returns the characters to be measured. This will be the same value
     * as {@link MeasuredText#getTextBuf()} if {@link #getMeasuredText()} available.
     */
    @Nonnull
    public char[] getChars() {
        return mCopiedBuffer;
    }

    /**
     * Returns the base paragraph direction. Either {@link Bidi#DIRECTION_LEFT_TO_RIGHT}
     * or {@link Bidi#DIRECTION_RIGHT_TO_LEFT)
     */
    public int getParagraphDir() {
        return mParaDir;
    }

    /**
     * Returns the directions.
     */
    @Nonnull
    public Directions getDirections(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        if (start == end || mLevels == null) {
            return Directions.ALL_LEFT_TO_RIGHT;
        }

        int baseLevel = mParaDir == Bidi.DIRECTION_LEFT_TO_RIGHT ? 0 : 1;
        byte[] levels = mLevels;

        int curLevel = levels[start];
        int minLevel = curLevel;
        int runCount = 1;
        for (int i = start + 1; i < end; ++i) {
            int level = levels[i];
            if (level != curLevel) {
                curLevel = level;
                ++runCount;
            }
        }

        // add final run for trailing counter-directional whitespace
        int visLen = end - start;
        if ((curLevel & 1) != (baseLevel & 1)) {
            // look for visible end
            while (--visLen >= 0) {
                char ch = mCopiedBuffer[start + visLen];

                if (ch == '\n') {
                    --visLen;
                    break;
                }

                if (ch != ' ' && ch != '\t') {
                    break;
                }
            }
            ++visLen;
            if (visLen != end - start) {
                ++runCount;
            }
        }

        if (runCount == 1 && minLevel == baseLevel) {
            // we're done, only one run on this line
            if ((minLevel & 1) != 0) {
                return Directions.ALL_RIGHT_TO_LEFT;
            }
            return Directions.ALL_LEFT_TO_RIGHT;
        }

        int[] ld = new int[runCount * 2];
        int maxLevel = minLevel;
        int levelBits = minLevel << Directions.RUN_LEVEL_SHIFT;

        // Start of first pair is always 0, we write
        // length then start at each new run, and the
        // last run length after we're done.
        int n = 1;
        int prev = start;
        curLevel = minLevel;
        for (int i = start, e = start + visLen; i < e; ++i) {
            int level = levels[i];
            if (level != curLevel) {
                curLevel = level;
                if (level > maxLevel) {
                    maxLevel = level;
                } else if (level < minLevel) {
                    minLevel = level;
                }
                // XXX ignore run length limit of 2^RUN_LEVEL_SHIFT
                ld[n++] = (i - prev) | levelBits;
                ld[n++] = i - start;
                levelBits = curLevel << Directions.RUN_LEVEL_SHIFT;
                prev = i;
            }
        }
        ld[n] = (start + visLen - prev) | levelBits;
        if (visLen < end - start) {
            ld[++n] = visLen;
            ld[++n] = (end - start - visLen) | (baseLevel << Directions.RUN_LEVEL_SHIFT);
        }

        // See if we need to swap any runs.
        // If the min level run direction doesn't match the base
        // direction, we always need to swap (at this point
        // we have more than one run).
        // Otherwise, we don't need to swap the lowest level.
        // Since there are no logically adjacent runs at the same
        // level, if the max level is the same as the (new) min
        // level, we have a series of alternating levels that
        // is already in order, so there's no more to do.
        final boolean swap;
        if ((minLevel & 1) == baseLevel) {
            minLevel += 1;
            swap = maxLevel > minLevel;
        } else {
            swap = runCount > 1;
        }
        if (swap) {
            for (int level = maxLevel - 1; level >= minLevel; --level) {
                for (int i = 0; i < ld.length; i += 2) {
                    if (levels[ld[i]] >= level) {
                        int e = i + 2;
                        while (e < ld.length && levels[ld[e]] >= level) {
                            e += 2;
                        }
                        for (int low = i, hi = e - 2; low < hi; low += 2, hi -= 2) {
                            int x = ld[low];
                            ld[low] = ld[hi];
                            ld[hi] = x;
                            x = ld[low + 1];
                            ld[low + 1] = ld[hi + 1];
                            ld[hi + 1] = x;
                        }
                        i = e + 2;
                    }
                }
            }
        }
        return new Directions(ld);
    }

    /**
     * Returns the {@link MetricAffectingSpan} end indices.
     * <p>
     * If the input text is not a spanned string, this has one value that is the length of the text.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout.
     * and the text is not empty. Returns empty array in other cases.
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
     * and the text is not empty. Returns empty array in other cases.
     */
    @Nonnull
    public IntArrayList getFontMetrics() {
        return mFontMetrics;
    }

    /**
     * Returns the result of the MeasuredParagraph.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout
     * and the text is not empty. Returns null in other cases.
     */
    @Nullable
    public MeasuredText getMeasuredText() {
        return mMeasuredText;
    }

    @Nonnull
    private static MeasuredParagraph obtain() {
        final MeasuredParagraph c = sPool.acquire();
        return c == null ? new MeasuredParagraph() : c;
    }

    @Nonnull
    public static MeasuredParagraph buildForStaticLayout(@Nonnull TextPaint paint, @Nonnull CharSequence text,
                                                         int start, int end, @Nonnull TextDirectionHeuristic dir,
                                                         @Nullable MeasuredParagraph recycle) {
        final MeasuredParagraph c = recycle == null ? obtain() : recycle;
        c.resetAndAnalyzeBidi(text, start, end, dir);
        if (end > start) {
            final MeasuredText.Builder builder = new MeasuredText.Builder(c.mCopiedBuffer);
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
        final int length = end - start;

        if (mCopiedBuffer == null || mCopiedBuffer.length != length) {
            mCopiedBuffer = new char[length];
        }
        if (length <= 0) {
            return;
        }
        TextUtils.getChars(text, start, end, mCopiedBuffer, 0);

        // Replace characters associated with ReplacementSpan to U+FFFC.
        if (mSpanned != null) {
            final ReplacementSpan[] spans = mSpanned.getSpans(start, end, ReplacementSpan.class);
            for (ReplacementSpan span : spans) {
                int startInPara = mSpanned.getSpanStart(span) - start;
                int endInPara = mSpanned.getSpanEnd(span) - start;
                // The span interval may be larger and must be restricted to [start, end)
                if (startInPara < 0)
                    startInPara = 0;
                if (endInPara > length)
                    endInPara = length;
                Arrays.fill(mCopiedBuffer, startInPara, endInPara, '\uFFFC');
            }
        }

        if ((dir == TextDirectionHeuristics.LTR
                || dir == TextDirectionHeuristics.FIRSTSTRONG_LTR
                || dir == TextDirectionHeuristics.ANYRTL_LTR)
                && !Bidi.requiresBidi(mCopiedBuffer, 0, length)) {
            mLevels = null;
            mParaDir = Bidi.DIRECTION_LEFT_TO_RIGHT;
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
                final boolean isRtl = dir.isRtl(mCopiedBuffer, 0, length);
                paraLevel = isRtl ? Bidi.RTL : Bidi.LTR;
            }
            Bidi bidi = new Bidi(length, 0);
            bidi.setPara(mCopiedBuffer, paraLevel, null);
            mLevels = bidi.getLevels();
            mParaDir = (bidi.getParaLevel() & 0x1) == 0 ? Bidi.DIRECTION_LEFT_TO_RIGHT : Bidi.DIRECTION_RIGHT_TO_LEFT;
        }
    }

    private void applyMetricsAffectingSpan(@Nonnull TextPaint paint, @Nullable MetricAffectingSpan[] spans,
                                           int start, int end, @Nonnull MeasuredText.Builder builder) {
        mCachedPaint.set(paint);

        if (mCachedFm == null) {
            mCachedFm = new FontMetricsInt();
        }

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

        mCachedPaint.getFontMetrics(mCachedFm);

        if (replacement != null) {
            applyReplacementRun(replacement, runStart, runEnd, builder);
        } else {
            applyStyleRun(runStart, runEnd, builder);
        }

        mFontMetrics.add(mCachedFm.mAscent);
        mFontMetrics.add(mCachedFm.mDescent);
    }

    private void applyReplacementRun(@Nonnull ReplacementSpan replacement, int start, int end,
                                     @Nonnull MeasuredText.Builder builder) {
        //TODO get replacement width
        builder.addReplacementRun(mCachedPaint, end - start, 0);
    }

    private void applyStyleRun(int start, int end, @Nonnull MeasuredText.Builder builder) {
        if (mLevels == null) {
            // If the whole text is LTR direction, just apply whole region.
            builder.addStyleRun(mCachedPaint, end - start, false);
        } else {
            // If there is multiple bidi levels, split into individual bidi level and apply style.
            byte level = mLevels[start];
            // Note that the empty text or empty range won't reach this method.
            // Safe to search from start + 1.
            for (int levelStart = start, levelEnd = start + 1; ; ++levelEnd) {
                if (levelEnd == end || mLevels[levelEnd] != level) { // bidi run
                    final boolean isRtl = (level & 0x1) != 0;
                    builder.addStyleRun(mCachedPaint, levelEnd - levelStart, isRtl);
                    if (levelEnd == end) {
                        break;
                    }
                    levelStart = levelEnd;
                    level = mLevels[levelEnd];
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
