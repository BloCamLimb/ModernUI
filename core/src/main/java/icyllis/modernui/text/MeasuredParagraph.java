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
import icyllis.modernui.annotation.*;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.text.*;
import icyllis.modernui.text.style.*;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * MeasuredParagraph provides text information for rendering purpose.
 * <p>
 * This class can identify text directions, and break the styled text into text runs
 * of range decreasing step by step (style runs, replacement runs, character style runs,
 * font runs, bidi runs). These text runs compute all characters' font metrics,
 * shaping, measurements and glyph graphs, which are enough to render a paragraph of
 * a rich text under Unicode specification and internationalization standards.
 *
 * @see MeasuredText
 */
@NotThreadSafe
public class MeasuredParagraph {

    private static final Pools.Pool<MeasuredParagraph> sPool = Pools.newSynchronizedPool(1);

    /**
     * The cast original text.
     * <p>
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
     * <p>
     * The length of this array is that of the target range in the original text.
     */
    private char[] mCopiedBuffer;

    /**
     * The base paragraph direction. Either {@link Layout#DIR_LEFT_TO_RIGHT}
     * or {@link Layout#DIR_RIGHT_TO_LEFT)
     */
    private int mParaDir;

    /**
     * The bidi level for individual characters.
     * <p>
     * This is null if the text is LTR direction and doesn't contain any bidi characters.
     */
    @Nullable
    private byte[] mLevels;

    /**
     * @see #getSpanEndCache()
     */
    @NonNull
    private final IntArrayList mSpanEndCache = new IntArrayList();

    /**
     * @see #getFontMetrics()
     */
    @NonNull
    private final IntArrayList mFontMetrics = new IntArrayList();

    @Nullable
    private MeasuredText mMeasuredText;

    @NonNull
    private final FontMetricsInt mCachedFm = new FontMetricsInt();

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
     * Returns the start offset of the paragraph to the original text.
     */
    public int getTextStart() {
        return mTextStart;
    }

    /**
     * Returns the length of the paragraph. This will be the same value
     * as the length of {@link #getChars()}.
     */
    public int getTextLength() {
        return mCopiedBuffer.length;
    }

    /**
     * Returns the characters to be measured. This will be the same value
     * as {@link MeasuredText#getTextBuf()} if {@link #getMeasuredText()} available.
     *
     * @return backend text buffer
     */
    @NonNull
    public char[] getChars() {
        return mCopiedBuffer;
    }

    /**
     * Returns the base paragraph direction.
     *
     * @return either {@link Layout#DIR_LEFT_TO_RIGHT} or {@link Layout#DIR_RIGHT_TO_LEFT)
     */
    public int getParagraphDir() {
        return mParaDir;
    }

    /**
     * Returns the directions, based on {@link #getParagraphDir()}}, with given range.
     *
     * @param start start char index, with start offset
     * @param end   end char index, with start offset
     * @return new calculated directions of this paragraph
     */
    @NonNull
    public Directions getDirections(int start, int end) {
        if (start == end || mLevels == null) {
            return Directions.ALL_LEFT_TO_RIGHT;
        }

        int baseLevel = mParaDir == Layout.DIR_LEFT_TO_RIGHT ? 0 : 1;
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
    @NonNull
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
    @NonNull
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

    /**
     * Returns the advance of the char at the given index of the text buffer.
     * <p>
     * This follows grapheme cluster break. For example: there are 6 chars (uint_16),
     * the first two are the first grapheme, the last four are the second one.
     * Then mAdvances[0] is for the first grapheme, mAdvances[2] for the second one,
     * other elements are zero. It's in the same order of {@link #getChars()}
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForMeasurement.
     * Returns empty array in other cases.
     *
     * @param offset the char index with start offset
     * @return advance
     */
    public float getAdvance(int offset) {
        if (mMeasuredText == null) {
            return 0f;
        } else {
            return mMeasuredText.getAdvance(offset);
        }
    }

    /**
     * Returns the advance of the given range.
     * <p>
     * This is not available if the MeasuredParagraph is computed with buildForBidi.
     * Returns 0 if the MeasuredParagraph is computed with buildForBidi.
     *
     * @param start the inclusive start offset of the target region in the text
     * @param end   the exclusive end offset of the target region in the text
     * @return advance
     * @see #getAdvance(int)
     */
    public float getAdvance(int start, int end) {
        if (mMeasuredText == null) {
            return 0f;
        } else {
            return mMeasuredText.getAdvance(start, end);
        }
    }

    /**
     * Retrieves the font metrics for the given range.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout.
     */
    public void getExtent(@IntRange(from = 0) int start, @IntRange(from = 0) int end,
                          @NonNull FontMetricsInt fmi) {
        if (mMeasuredText != null) {
            mMeasuredText.getExtent(start, end, fmi);
        }
    }

    @NonNull
    private static MeasuredParagraph obtain() {
        final MeasuredParagraph c = sPool.acquire();
        return c == null ? new MeasuredParagraph() : c;
    }

    /**
     * Generates new MeasuredParagraph for Bidi computation.
     * <p>
     * If recycle is null, this returns new instance. If recycle is not null, this fills computed
     * result to recycle and returns recycle.
     *
     * @param text    the character sequence to be measured
     * @param start   the inclusive start offset of the target region in the text
     * @param end     the exclusive end offset of the target region in the text
     * @param textDir the text direction algorithm
     * @param recycle pass existing MeasuredParagraph if you want to recycle it.
     * @return measured text
     */
    @NonNull
    public static MeasuredParagraph buildForBidi(@NonNull CharSequence text, int start, int end,
                                                 @NonNull TextDirectionHeuristic textDir,
                                                 @Nullable MeasuredParagraph recycle) {
        if ((start | end | end - start | text.length() - end) < 0) {
            throw new IllegalArgumentException();
        }
        final MeasuredParagraph c = recycle == null ? obtain() : recycle;
        c.resetAndAnalyzeBidi(text, start, end, textDir);
        return c;
    }

    /**
     * Generates new MeasuredParagraph for StaticLayout.
     * <p>
     * If recycle is null, this returns new instance. If recycle is not null, this fills computed
     * result to recycle and returns recycle.
     *
     * @param paint      the base paint to be used for drawing the text
     * @param text       the character sequence to be measured
     * @param start      the inclusive start offset of the target region in the text
     * @param end        the exclusive end offset of the target region in the text
     * @param textDir    the text direction algorithm
     * @param fullLayout true to compute full layout, for rendering the text soon
     * @param recycle    pass existing MeasuredParagraph if you want to recycle it
     * @return measured text
     */
    @NonNull
    public static MeasuredParagraph buildForStaticLayout(
            @NonNull TextPaint paint,
            @Nullable LineBreakConfig lineBreakConfig,
            @NonNull CharSequence text,
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end,
            @NonNull TextDirectionHeuristic textDir,
            boolean fullLayout,
            @Nullable MeasuredParagraph recycle) {
        if ((start | end | end - start | text.length() - end) < 0) {
            throw new IllegalArgumentException();
        }
        final MeasuredParagraph c = recycle == null ? obtain() : recycle;
        c.resetAndAnalyzeBidi(text, start, end, textDir);
        if (end > start) {
            final MeasuredText.Builder builder = new MeasuredText.Builder(c.mCopiedBuffer)
                    .setComputeLayout(fullLayout);
            if (c.mSpanned == null) {
                // No style change by MetricsAffectingSpan. Just measure all text.
                c.applyMetricsAffectingSpan(paint, lineBreakConfig, /*spans*/Collections.emptyList(), start, end,
                        builder);
                c.mSpanEndCache.add(end);
            } else {
                // There may be a MetricsAffectingSpan. Split into span transitions and apply
                // styles.
                int spanEnd;
                for (int spanStart = start; spanStart < end; spanStart = spanEnd) {
                    spanEnd = c.mSpanned.nextSpanTransition(spanStart, end,
                            MetricAffectingSpan.class);
                    List<MetricAffectingSpan> spans = c.mSpanned.getSpans(spanStart, spanEnd,
                            MetricAffectingSpan.class);
                    spans = TextUtils.removeEmptySpans(spans, c.mSpanned);
                    c.applyMetricsAffectingSpan(paint, lineBreakConfig, spans, spanStart, spanEnd,
                            builder);
                    c.mSpanEndCache.add(spanEnd);
                }
            }
            c.mMeasuredText = builder.build();
        }
        return c;
    }

    private void resetAndAnalyzeBidi(@NonNull CharSequence text, int start, int end,
                                     @NonNull TextDirectionHeuristic dir) {
        reset();
        mSpanned = text instanceof Spanned ? (Spanned) text : null;
        mTextStart = start;
        int length = end - start;

        if (mCopiedBuffer == null || mCopiedBuffer.length != length) {
            mCopiedBuffer = new char[length];
        }
        TextUtils.getChars(text, start, end, mCopiedBuffer, 0);

        // Replace characters associated with ReplacementSpan to U+FFFC.
        if (mSpanned != null) {
            final List<ReplacementSpan> spans = mSpanned.getSpans(start, end, ReplacementSpan.class);
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
            mParaDir = Layout.DIR_LEFT_TO_RIGHT;
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
            mParaDir = (bidi.getParaLevel() & 0x1) == 0 ? Layout.DIR_LEFT_TO_RIGHT : Layout.DIR_RIGHT_TO_LEFT;
        }
    }

    private void applyMetricsAffectingSpan(
            @NonNull TextPaint paint,
            @Nullable LineBreakConfig lineBreakConfig,
            @NonNull List<MetricAffectingSpan> spans,
            @IntRange(from = 0) int start,
            @IntRange(from = 0) int end,
            @NonNull MeasuredText.Builder builder) {
        assert start != end;
        TextPaint tp = TextPaint.obtain();
        tp.set(paint);

        ReplacementSpan replacement = null;
        for (MetricAffectingSpan span : spans) {
            if (span instanceof ReplacementSpan) {
                // The last ReplacementSpan is effective for backward compatibility reasons.
                replacement = (ReplacementSpan) span;
            } else {
                span.updateMeasureState(tp);
            }
        }

        tp.getFontMetricsInt(mCachedFm);

        if (replacement != null) {
            final float width = replacement.getSize(
                    tp, mSpanned, start + mTextStart, end + mTextStart, mCachedFm);
            builder.addReplacementRun(tp.getTextLocale(), end - start, width);
        } else {
            final int offset = mTextStart;
            final FontPaint base = tp.createInternalPaint();
            applyStyleRun(base, start - offset, end - offset, lineBreakConfig, builder);
        }

        mFontMetrics.add(mCachedFm.ascent);
        mFontMetrics.add(mCachedFm.descent);
        tp.recycle();
    }

    private void applyStyleRun(@NonNull FontPaint paint, int start, int end,
                               @Nullable LineBreakConfig config,
                               @NonNull MeasuredText.Builder builder) {
        if (mLevels == null) {
            // If the whole text is LTR direction, just apply whole region.
            builder.addStyleRun(paint, config, end - start, false);
        } else {
            // If there is multiple bidi levels, split into individual bidi level and apply style.
            byte level = mLevels[start];
            // Note that the empty text or empty range won't reach this method.
            // Safe to search from start + 1.
            for (int levelStart = start, levelEnd = start + 1; ; ++levelEnd) {
                if (levelEnd == end || mLevels[levelEnd] != level) { // transition point
                    final boolean isRtl = (level & 0x1) != 0;
                    builder.addStyleRun(paint, config, levelEnd - levelStart, isRtl);
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
     * Returns the maximum index that the accumulated width not exceeds the width.
     * <p>
     * If forward=false is passed, returns the minimum index from the end instead.
     * <p>
     * This only works if the MeasuredParagraph is computed with buildForMeasurement.
     * Undefined behavior in other case.
     */
    int breakText(int limit, boolean forwards, float width) {
        MeasuredText mt = mMeasuredText;
        assert mt != null;
        if (forwards) {
            int i = 0;
            while (i < limit) {
                width -= mt.getAdvance(i);
                if (width < 0.0f) break;
                i++;
            }
            while (i > 0 && mCopiedBuffer[i - 1] == ' ') i--;
            return i;
        } else {
            int i = limit - 1;
            while (i >= 0) {
                width -= mt.getAdvance(i);
                if (width < 0.0f) break;
                i--;
            }
            while (i < limit - 1 && (mCopiedBuffer[i + 1] == ' ' || mt.getAdvance(i + 1) == 0.0f)) {
                i++;
            }
            return limit - i - 1;
        }
    }

    /**
     * Recycle the MeasuredParagraph.
     */
    public void recycle() {
        release();
        sPool.release(this);
    }

    /**
     * Note: This includes the text buffer, and be considered to be recycled later.
     * Input CharSequence is not considered.
     *
     * @return memory usage in bytes
     */
    public int getMemoryUsage() {
        return MathUtil.align8(12 + 8 + 4 + 8 + (mCopiedBuffer == null ?
                0 : 16 + (mCopiedBuffer.length << 1)) + 4 + (mLevels == null ?
                0 : 16 + mLevels.length) + 16 + (mSpanEndCache.size() << 2) +
                16 + (mFontMetrics.size() << 2) + 8) +
                (mMeasuredText == null ? 0 : mMeasuredText.getMemoryUsage());
    }
}
