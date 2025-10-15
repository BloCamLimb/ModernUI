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

package icyllis.modernui.text;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.text.Bidi;
import icyllis.modernui.annotation.IntRange;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.text.CharUtils;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.FontPaint;
import icyllis.modernui.graphics.text.LineBreakConfig;
import icyllis.modernui.graphics.text.MeasuredText;
import icyllis.modernui.text.style.MetricAffectingSpan;
import icyllis.modernui.text.style.ReplacementSpan;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private static final Pools.Pool<MeasuredParagraph> sPool = Pools.newSynchronizedPool(2);

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
     * The bidi level for individual characters.
     * <p>
     * This is null if the text is LTR direction and doesn't contain any bidi characters.
     */
    @Nullable
    private Bidi mBidi;

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
        if (mSpanEndCache.elements().length > 10000) {
            mSpanEndCache.trim();
        }
        if (mFontMetrics.elements().length > 10000) {
            mFontMetrics.trim();
        }
    }

    /**
     * Resets the internal state for starting new text.
     */
    private void reset() {
        mSpanned = null;
        mCopiedBuffer = null;
        mBidi = null;
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
     * @return backed text buffer
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
        if (mBidi == null) {
            return Layout.DIR_LEFT_TO_RIGHT;
        }
        return (mBidi.getParaLevel() & 0x01) == 0
                ? Layout.DIR_LEFT_TO_RIGHT : Layout.DIR_RIGHT_TO_LEFT;
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
        // Easy case: mBidi == null means the text is all LTR and no bidi support is needed.
        if (mBidi == null) {
            return Directions.ALL_LEFT_TO_RIGHT;
        }

        // Easy case: If the original text only contains single directionality run, the
        // substring is only single run.
        if (start == end) {
            if ((mBidi.getParaLevel() & 0x01) == 0) {
                return Directions.ALL_LEFT_TO_RIGHT;
            } else {
                return Directions.ALL_RIGHT_TO_LEFT;
            }
        }

        // Okay, now we need to generate the line instance.
        Bidi bidi = mBidi.createLineBidi(start, end);

        // Easy case: If the line instance only contains single directionality run, no need
        // to reorder visually.
        if (bidi.getRunCount() == 1) {
            if (bidi.getRunLevel(0) == 1) {
                return Directions.ALL_RIGHT_TO_LEFT;
            } else if (bidi.getRunLevel(0) == 0) {
                return Directions.ALL_LEFT_TO_RIGHT;
            } else {
                return new Directions(new int[]{
                        0, bidi.getRunLevel(0) << Directions.RUN_LEVEL_SHIFT | (end - start)});
            }
        }

        // Reorder directionality run visually.
        byte[] levels = new byte[bidi.getRunCount()];
        for (int i = 0; i < bidi.getRunCount(); ++i) {
            levels[i] = (byte) bidi.getRunLevel(i);
        }
        int[] visualOrders = Bidi.reorderVisual(levels);

        int[] dirs = new int[bidi.getRunCount() * 2];
        for (int i = 0; i < bidi.getRunCount(); ++i) {
            int vIndex;
            if ((mBidi.getBaseLevel() & 0x01) == 1) {
                // For the historical reasons, if the base directionality is RTL, TextLine
                // draws from the right, i.e. the visually reordered run needs to be reversed.
                vIndex = visualOrders[bidi.getRunCount() - i - 1];
            } else {
                vIndex = visualOrders[i];
            }

            // Special packing of dire
            dirs[i * 2] = bidi.getRunStart(vIndex);
            dirs[i * 2 + 1] = bidi.getRunLevel(vIndex) << Directions.RUN_LEVEL_SHIFT
                    | (bidi.getRunLimit(vIndex) - dirs[i * 2]);
        }

        return new Directions(dirs);
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
     * This is per-cluster advance and font-dependent.
     * <p>
     * This is available only if the MeasuredParagraph is computed with buildForStaticLayout.
     * Returns 0 in other cases.
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
     * This is per-cluster advance and font-dependent.
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
        CharUtils.getChars(text, start, end, mCopiedBuffer, 0);

        // Replace characters associated with ReplacementSpan to U+FFFC.
        if (mSpanned != null) {
            final List<ReplacementSpan> spans = mSpanned.getSpans(start, end, ReplacementSpan.class);
            for (int i = 0, e = spans.size(); i < e; i++) {
                ReplacementSpan span = spans.get(i);
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
                && !TextUtils.couldAffectRtl(mCopiedBuffer, 0, length)) {
            mBidi = null;
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
            mBidi = new Bidi(length, 0);
            mBidi.setPara(mCopiedBuffer, paraLevel, null);

            if (length > 0
                    && mBidi.getParagraphIndex(length - 1) != 0) {
                // Historically, the MeasuredParagraph does not treat the CR letters as paragraph
                // breaker but ICU BiDi treats it as paragraph breaker. In the MeasureParagraph,
                // the given range always represents a single paragraph, so if the BiDi object has
                // multiple paragraph, it should contain a CR letters in the text. Using CR is not
                // common and also it should not penalize the easy case, e.g. all LTR,
                // check the paragraph count here and replace the CR letters and re-calculate
                // BiDi again.
                for (int i = 0; i < length; ++i) {
                    if (Character.isSurrogate(mCopiedBuffer[i])) {
                        // All block separators are in BMP.
                        continue;
                    }
                    if (UCharacter.getDirection(mCopiedBuffer[i])
                            == UCharacterDirection.BLOCK_SEPARATOR) {
                        mCopiedBuffer[i] = '\uFFFC';
                    }
                }
                mBidi.setPara(mCopiedBuffer, paraLevel, null);
            }
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
        tp.baselineShift = 0;

        ReplacementSpan replacement = null;
        for (int i = 0, e = spans.size(); i < e; i++) {
            MetricAffectingSpan span = spans.get(i);
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

        if (tp.baselineShift < 0) {
            mCachedFm.ascent += tp.baselineShift;
        } else {
            mCachedFm.descent += tp.baselineShift;
        }
        mFontMetrics.add(mCachedFm.ascent);
        mFontMetrics.add(mCachedFm.descent);
        tp.recycle();
    }

    private void applyStyleRun(@NonNull FontPaint paint, int start, int end,
                               @Nullable LineBreakConfig config,
                               @NonNull MeasuredText.Builder builder) {
        if (mBidi == null) {
            // If the whole text is LTR direction, just apply whole region.
            builder.addStyleRun(paint, config, end - start, false);
        } else {
            // If there is multiple bidi levels, split into individual bidi level and apply style.
            byte level = mBidi.getLevelAt(start);
            // Note that the empty text or empty range won't reach this method.
            // Safe to search from start + 1.
            for (int levelStart = start, levelEnd = start + 1; ; ++levelEnd) {
                if (levelEnd == end || mBidi.getLevelAt(levelEnd) != level) { // transition point
                    final boolean isRtl = (level & 0x1) != 0;
                    builder.addStyleRun(paint, config, levelEnd - levelStart, isRtl);
                    if (levelEnd == end) {
                        break;
                    }
                    levelStart = levelEnd;
                    level = mBidi.getLevelAt(levelEnd);
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

    /**
     * Note: This includes the text buffer, and be considered to be recycled later.
     * Input CharSequence is not considered.
     *
     * @return memory usage in bytes
     */
    public int getMemoryUsage() {
        return MathUtil.align8(12 + 8 + 4 + 8 + (mCopiedBuffer == null ?
                0 : 16 + (mCopiedBuffer.length << 1)) + 4 + 16 + (mSpanEndCache.size() << 2) +
                16 + (mFontMetrics.size() << 2) + 8) +
                (mMeasuredText == null ? 0 : mMeasuredText.getMemoryUsage());
    }
}
