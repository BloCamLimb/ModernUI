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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.text.style.CharacterStyle;
import icyllis.modernui.text.style.MetricAffectingSpan;
import icyllis.modernui.text.style.ReplacementSpan;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a line of styled text, for measuring in visual order and
 * for rendering.
 * <p>
 * Get a new instance using {@link #obtain()}, and when finished with it, return it
 * to the pool using {@link #recycle()}.
 * <p>
 * Call set to prepare the instance for use, then either draw, measure,
 * metrics, or caretToLeftRightOf.
 */
public class TextLine {

    private static final Pool<TextLine> sPool = Pools.concurrent(1);

    private static final char TAB_CHAR = '\t';

    private TextPaint mPaint;
    private CharSequence mText;
    private int mStart;
    private int mLen;
    private int mDir;
    private Directions mDirections;
    private boolean mHasTabs;
    private TabStops mTabs;
    private char[] mChars;
    private boolean mCharsValid;
    private Spanned mSpanned;
    private PrecomputedText mComputed;

    // The start and end of a potentially existing ellipsis on this text line.
    // We use them to filter out replacement and metric affecting spans on ellipsized away chars.
    private int mEllipsisStart;
    private int mEllipsisEnd;

    private final TextPaint mWorkPaint = new TextPaint();
    private final TextPaint mActivePaint = new TextPaint();
    private final SpanSet<MetricAffectingSpan> mMetricAffectingSpanSpanSet =
            new SpanSet<>(MetricAffectingSpan.class);
    private final SpanSet<CharacterStyle> mCharacterStyleSpanSet =
            new SpanSet<>(CharacterStyle.class);
    private final SpanSet<ReplacementSpan> mReplacementSpanSpanSet =
            new SpanSet<>(ReplacementSpan.class);

    private TextLine() {
    }

    /**
     * Returns a new TextLine from the shared pool.
     *
     * @return an uninitialized TextLine
     */
    @Nonnull
    public static TextLine obtain() {
        TextLine tl = sPool.acquire();
        if (tl == null) {
            tl = new TextLine();
        }
        return tl;
    }

    /**
     * Puts a TextLine back into the shared pool. Do not use this TextLine once
     * it has been returned.
     */
    public void recycle() {
        mText = null;
        mPaint = null;
        mDirections = null;
        mSpanned = null;
        mTabs = null;
        //mChars = null;
        mComputed = null;

        mMetricAffectingSpanSpanSet.recycle();
        mCharacterStyleSpanSet.recycle();
        mReplacementSpanSpanSet.recycle();

        sPool.release(this);
    }

    /**
     * Initializes a TextLine and prepares it for use.
     *
     * @param paint         the base paint for the line
     * @param text          the text, can be Styled
     * @param start         the start of the line relative to the text
     * @param limit         the limit of the line relative to the text
     * @param dir           the paragraph direction of this line
     * @param directions    the directions information of this line
     * @param hasTabs       true if the line might contain tabs
     * @param tabStops      the tabStops. Can be null
     * @param ellipsisStart the start of the ellipsis relative to the line
     * @param ellipsisEnd   the end of the ellipsis relative to the line. When there
     *                      is no ellipsis, this should be equal to ellipsisStart.
     */
    public void set(@Nonnull TextPaint paint, @Nonnull CharSequence text, int start, int limit, int dir,
                    @Nonnull Directions directions, boolean hasTabs, @Nullable TabStops tabStops,
                    int ellipsisStart, int ellipsisEnd) {
        mPaint = paint;
        mText = text;
        mStart = start;
        mLen = limit - start;
        mDir = dir;
        mDirections = directions;
        mHasTabs = hasTabs;
        mSpanned = null;

        if (text instanceof Spanned) {
            mSpanned = (Spanned) text;
            mCharsValid = mReplacementSpanSpanSet.init(mSpanned, start, limit);
        } else {
            mCharsValid = false;
        }

        mComputed = null;
        if (text instanceof PrecomputedText) {
            mComputed = (PrecomputedText) text;
            if (mComputed.getPaint().isMetricAffecting(paint)) {
                mComputed = null;
            }
        }

        if (mCharsValid) {
            if (mChars == null || mChars.length < mLen) {
                mChars = new char[mLen];
            }
            TextUtils.getChars(text, start, limit, mChars, 0);
            // Handle these all at once so we don't have to do it as we go.
            // Replace the first character of each replacement run with the
            // object-replacement character and the remainder with zero width
            // non-break space aka BOM.  Cursor movement code skips these
            // zero-width characters.
            char[] chars = mChars;
            for (int i = start, inext; i < limit; i = inext) {
                inext = mReplacementSpanSpanSet.getNextTransition(i, limit);
                if (mReplacementSpanSpanSet.hasSpansIntersecting(i, inext)
                        && (i - start >= ellipsisEnd || inext - start <= ellipsisStart)) {
                    // transition into a span
                    chars[i - start] = '\uFFFC';
                    for (int j = i - start + 1, e = inext - start; j < e; ++j) {
                        chars[j] = '\uFEFF'; // used as ZWNBS, marks positions to skip
                    }
                }
            }
        }
        mTabs = tabStops;

        if (ellipsisStart != ellipsisEnd) {
            mEllipsisStart = ellipsisStart;
            mEllipsisEnd = ellipsisEnd;
        } else {
            mEllipsisStart = mEllipsisEnd = 0;
        }
    }

    private char charAt(int i) {
        return mCharsValid ? mChars[i] : mText.charAt(i + mStart);
    }

    /**
     * Draw the text line, based on visual order. If paragraph (base) direction is RTL,
     * then x should be on the opposite side (i.e. leading) corresponding to normal one.
     *
     * @param canvas canvas to draw on
     * @param x      the leading margin position
     * @param top    the top of the line
     * @param y      the baseline
     * @param bottom the bottom of the line
     */
    public void draw(@Nonnull Canvas canvas, float x, int top, int y, int bottom) {
        float h = 0;
        final int runCount = mDirections.getRunCount();
        for (int runIndex = 0; runIndex < runCount; runIndex++) {
            final int runStart = mDirections.getRunStart(runIndex);
            if (runStart >= mLen) {
                break;
            }
            final int runLimit = Math.min(runStart + mDirections.getRunLength(runIndex), mLen);
            final boolean runIsRtl = mDirections.isRunRtl(runIndex);

            int segStart = runStart;
            for (int j = mHasTabs ? runStart : runLimit; j <= runLimit; j++) {
                if (j == runLimit || charAt(j) == TAB_CHAR) {
                    h += drawRun(canvas, segStart, j, runIsRtl, x + h, top, y, bottom,
                            runIndex != (runCount - 1) || j != mLen);

                    if (j != runLimit) {  // charAt(j) == TAB_CHAR
                        h = mDir * nextTab(h * mDir);
                    }
                    segStart = j + 1;
                }
            }
        }
    }

    /**
     * Returns the signed graphical offset from the leading margin.
     * <p>
     * Following examples are all for measuring offset=3. LX(e.g. L0, L1, ...) denotes a
     * character which has LTR BiDi property. On the other hand, RX(e.g. R0, R1, ...) denotes a
     * character which has RTL BiDi property. Assuming all character has 1em width.
     * <p>
     * Example 1: All LTR chars within LTR context
     * Input Text (logical)  :   L0 L1 L2 L3 L4 L5 L6 L7 L8
     * Input Text (visual)   :   L0 L1 L2 L3 L4 L5 L6 L7 L8
     * Output(trailing=true) :  |--------| (Returns 3em)
     * Output(trailing=false):  |--------| (Returns 3em)
     * <p>
     * Example 2: All RTL chars within RTL context.
     * Input Text (logical)  :   R0 R1 R2 R3 R4 R5 R6 R7 R8
     * Input Text (visual)   :   R8 R7 R6 R5 R4 R3 R2 R1 R0
     * Output(trailing=true) :                    |--------| (Returns -3em)
     * Output(trailing=false):                    |--------| (Returns -3em)
     * <p>
     * Example 3: BiDi chars within LTR context.
     * Input Text (logical)  :   L0 L1 L2 R3 R4 R5 L6 L7 L8
     * Input Text (visual)   :   L0 L1 L2 R5 R4 R3 L6 L7 L8
     * Output(trailing=true) :  |-----------------| (Returns 6em)
     * Output(trailing=false):  |--------| (Returns 3em)
     * <p>
     * Example 4: BiDi chars within RTL context.
     * Input Text (logical)  :   L0 L1 L2 R3 R4 R5 L6 L7 L8
     * Input Text (visual)   :   L6 L7 L8 R5 R4 R3 L0 L1 L2
     * Output(trailing=true) :           |-----------------| (Returns -6em)
     * Output(trailing=false):                    |--------| (Returns -3em)
     *
     * @param offset   the line-relative character offset, between 0 and the line length, inclusive
     * @param trailing no effect if the offset is not on the BiDi transition offset. If the offset
     *                 is on the BiDi transition offset and true is passed, the offset is regarded
     *                 as the edge of the trailing run's edge. If false, the offset is regarded as
     *                 the edge of the preceding run's edge. See example above.
     * @param fmi      receives metrics information about the requested character, can be null
     * @return the signed graphical offset from the leading margin to the requested character edge.
     * The positive value means the offset is right from the leading edge. The negative
     * value means the offset is left from the leading edge.
     */
    public float measure(int offset, boolean trailing, @Nullable FontMetricsInt fmi) {
        if (offset > mLen) {
            throw new IndexOutOfBoundsException(
                    "offset(" + offset + ") should be less than line limit(" + mLen + ")");
        }
        final int target = trailing ? offset - 1 : offset;
        if (target < 0) {
            return 0;
        }

        float h = 0;
        for (int runIndex = 0; runIndex < mDirections.getRunCount(); runIndex++) {
            final int runStart = mDirections.getRunStart(runIndex);
            if (runStart > mLen) break;
            final int runLimit = Math.min(runStart + mDirections.getRunLength(runIndex), mLen);
            final boolean runIsRtl = mDirections.isRunRtl(runIndex);

            int segStart = runStart;
            for (int j = mHasTabs ? runStart : runLimit; j <= runLimit; j++) {
                if (j == runLimit || charAt(j) == TAB_CHAR) {
                    final boolean targetIsInThisSegment = target >= segStart && target < j;
                    final boolean sameDirection = (mDir == Layout.DIR_RIGHT_TO_LEFT) == runIsRtl;

                    if (targetIsInThisSegment && sameDirection) {
                        return h + measureRun(segStart, offset, j, runIsRtl, fmi);
                    }

                    final float segmentWidth = measureRun(segStart, j, j, runIsRtl, fmi);
                    h += sameDirection ? segmentWidth : -segmentWidth;

                    if (targetIsInThisSegment) {
                        return h + measureRun(segStart, offset, j, runIsRtl, null);
                    }

                    if (j != runLimit) {  // charAt(j) == TAB_CHAR
                        if (offset == j) {
                            return h;
                        }
                        h = mDir * nextTab(h * mDir);
                        if (target == j) {
                            return h;
                        }
                    }

                    segStart = j + 1;
                }
            }
        }

        return h;
    }

    /**
     * Draws a unidirectional (but possibly multi-styled) run of text.
     *
     * @param canvas    the canvas to draw on
     * @param start     the line-relative start
     * @param limit     the line-relative limit
     * @param runIsRtl  true if the run is right-to-left
     * @param x         the position of the run that is closest to the leading margin
     * @param top       the top of the line
     * @param y         the baseline
     * @param bottom    the bottom of the line
     * @param needWidth true if the width value is required.
     * @return the signed width of the run, based on the paragraph direction.
     * Only valid if needWidth is true.
     */
    private float drawRun(@Nonnull Canvas canvas, int start, int limit, boolean runIsRtl,
                          float x, int top, int y, int bottom, boolean needWidth) {
        if ((mDir == Layout.DIR_LEFT_TO_RIGHT) == runIsRtl) {
            float w = -measureRun(start, limit, limit, runIsRtl, null);
            handleRun(start, limit, limit, runIsRtl, canvas, x + w, top,
                    y, bottom, null, false);
            return w;
        }
        return handleRun(start, limit, limit, runIsRtl, canvas, x, top,
                y, bottom, null, needWidth);
    }

    /**
     * Measures a unidirectional (but possibly multi-styled) run of text.
     *
     * @param start    the line-relative start of the run
     * @param offset   the offset to measure to, between start and limit inclusive
     * @param limit    the line-relative limit of the run
     * @param runIsRtl true if the run is right-to-left
     * @param fmi      receives metrics information about the requested
     *                 run, can be null.
     * @return the signed width from the start of the run to the leading edge
     * of the character at offset, based on the run (not paragraph) direction
     */
    private float measureRun(int start, int offset, int limit, boolean runIsRtl,
                             @Nullable FontMetricsInt fmi) {
        return handleRun(start, offset, limit, runIsRtl, null, 0, 0, 0, 0, fmi, true);
    }

    /**
     * Utility function for handling a unidirectional run.  The run must not
     * contain tabs but can contain styles.
     *
     * @param start        the line-relative start of the run
     * @param measureLimit the offset to measure to, between start and limit inclusive
     * @param limit        the limit of the run
     * @param runIsRtl     true if the run is right-to-left
     * @param canvas       the canvas, can be null
     * @param x            the end of the run closest to the leading margin
     * @param top          the top of the line
     * @param y            the baseline
     * @param bottom       the bottom of the line
     * @param fmi          receives metrics information, can be null
     * @param needWidth    true if the width is required
     * @return the signed width of the run based on the run direction; only
     * valid if needWidth is true
     */
    private float handleRun(int start, int measureLimit, int limit, boolean runIsRtl,
                            @Nullable Canvas canvas, float x, int top, int y, int bottom,
                            @Nullable FontMetricsInt fmi, boolean needWidth) {
        if (measureLimit < start || measureLimit > limit) {
            throw new IndexOutOfBoundsException("measureLimit (" + measureLimit + ") is out of "
                    + "start (" + start + ") and limit (" + limit + ") bounds");
        }

        // Case of an empty line, make sure we update fmi according to mPaint
        if (start == measureLimit) {
            final TextPaint wp = mWorkPaint;
            wp.set(mPaint);
            if (fmi != null) {
                expandMetricsFromPaint(fmi, wp);
            }
            return 0f;
        }

        final boolean needsSpanMeasurement;
        if (mSpanned == null) {
            needsSpanMeasurement = false;
        } else {
            needsSpanMeasurement =
                    mMetricAffectingSpanSpanSet.init(mSpanned, mStart + start, mStart + limit) |
                            mCharacterStyleSpanSet.init(mSpanned, mStart + start, mStart + limit);
        }

        if (!needsSpanMeasurement) {
            final TextPaint wp = mWorkPaint;
            wp.set(mPaint);
            if (fmi != null) {
                expandMetricsFromPaint(fmi, wp);
            }
            return handleText(wp, start, limit, start, limit, runIsRtl, canvas, x, top,
                    y, bottom, needWidth, measureLimit, 0);
        }

        // Shaping needs to take into account context up to metric boundaries,
        // but rendering needs to take into account character style boundaries.
        // So we iterate through metric runs to get metric bounds,
        // then within each metric run iterate through character style runs
        // for the run bounds.
        final float originalX = x;
        for (int i = start, inext; i < measureLimit; i = inext) {
            final TextPaint wp = mWorkPaint;
            wp.set(mPaint);

            inext = mMetricAffectingSpanSpanSet.getNextTransition(mStart + i, mStart + limit) -
                    mStart;
            int mlimit = Math.min(inext, measureLimit);

            ReplacementSpan replacement = null;

            for (int j = 0; j < mMetricAffectingSpanSpanSet.mSpans.size(); j++) {
                // Both intervals [spanStarts..spanEnds] and [mStart + i..mStart + mlimit] are NOT
                // empty by construction. This special case in getSpans() explains the >= & <= tests
                if ((mMetricAffectingSpanSpanSet.mSpanStarts[j] >= mStart + mlimit)
                        || (mMetricAffectingSpanSpanSet.mSpanEnds[j] <= mStart + i)) continue;

                final MetricAffectingSpan span = mMetricAffectingSpanSpanSet.mSpans.get(j);
                if (span instanceof ReplacementSpan) {
                    boolean insideEllipsis =
                            mStart + mEllipsisStart <= mMetricAffectingSpanSpanSet.mSpanStarts[j]
                                    && mMetricAffectingSpanSpanSet.mSpanEnds[j] <= mStart + mEllipsisEnd;
                    replacement = insideEllipsis ? null : (ReplacementSpan) span;
                } else {
                    // We might have a replacement that uses the draw
                    // state, otherwise measure state would suffice.
                    span.updateMeasureState(wp);
                }
            }

            if (replacement != null) {
                x += handleReplacement(replacement, wp, i, mlimit, runIsRtl, canvas, x, top, y,
                        bottom, fmi, needWidth || mlimit < measureLimit);
                continue;
            }

            // Get metrics first (even for empty strings or "0" width runs)
            if (fmi != null) {
                expandMetricsFromPaint(fmi, wp);
            }

            TextPaint activePaint = mActivePaint;
            for (int j = i, jnext; j < mlimit; j = jnext) {
                jnext = mCharacterStyleSpanSet.getNextTransition(mStart + j, mStart + inext) -
                        mStart;

                final int offset = Math.min(jnext, mlimit);
                activePaint.set(wp);
                for (int k = 0; k < mCharacterStyleSpanSet.mSpans.size(); k++) {
                    // Intentionally using >= and <= as explained above
                    if ((mCharacterStyleSpanSet.mSpanStarts[k] >= mStart + offset) ||
                            (mCharacterStyleSpanSet.mSpanEnds[k] <= mStart + j)) continue;

                    final CharacterStyle span = mCharacterStyleSpanSet.mSpans.get(k);
                    span.updateDrawState(activePaint);
                }

                final int flags = activePaint.mFlags & (TextPaint.UNDERLINE_FLAG | TextPaint.STRIKETHROUGH_FLAG);
                activePaint.mFlags &= ~(TextPaint.UNDERLINE_FLAG | TextPaint.STRIKETHROUGH_FLAG);

                if (jnext - j <= LayoutCache.MAX_PIECE_LENGTH) {
                    x += handleText(activePaint, j, jnext, i, inext, runIsRtl, canvas, x,
                            top, y, bottom, needWidth || jnext < measureLimit,
                            offset, flags);
                } else {
                    int s = j, e = s;
                    do {
                        e = Math.min(e + LayoutCache.MAX_PIECE_LENGTH, jnext);
                        x += handleText(activePaint, s, e, i, inext, runIsRtl, canvas, x,
                                top, y, bottom, needWidth || jnext < measureLimit,
                                offset, flags);
                        s = e;
                    } while (s < jnext);
                }
            }
        }

        return x - originalX;
    }

    /**
     * Utility function for measuring and rendering text.  The text must
     * not include a tab.
     *
     * @param wp        the working paint
     * @param start     the start of the text
     * @param end       the end of the text
     * @param runIsRtl  true if the run is right-to-left
     * @param c         the canvas, can be null if rendering is not needed
     * @param x         the edge of the run closest to the leading margin
     * @param top       the top of the line
     * @param y         the baseline
     * @param bottom    the bottom of the line
     * @param needWidth true if the width of the run is needed
     * @param offset    the offset for the purpose of measuring
     * @param flags     the decoration flags
     * @return the signed width of the run based on the run direction; only
     * valid if needWidth is true
     */
    private float handleText(@Nonnull TextPaint wp, int start, int end,
                             int contextStart, int contextEnd, boolean runIsRtl,
                             @Nullable Canvas c, float x, int top, int y, int bottom,
                             boolean needWidth, int offset, int flags) {
        // No need to do anything if the run width is "0"
        if (end == start) {
            return 0f;
        }
        float totalWidth = 0;

        LayoutPiece piece = null;
        if (c != null || needWidth) {
            if (mCharsValid) {
                piece = LayoutCache.getOrCreate(mChars, start, offset,
                        runIsRtl, wp, false, c != null);
            } else {
                final int delta = mStart;
                piece = LayoutCache.getOrCreate(mText, start + delta, offset + delta,
                        runIsRtl, wp, false, c != null);
            }
            totalWidth = piece.getAdvance();
        }

        if (c != null) {
            final float leftX, rightX;
            if (runIsRtl) {
                leftX = x - totalWidth;
                rightX = x;
            } else {
                leftX = x;
                rightX = x + totalWidth;
            }

            if (wp.bgColor != 0) {
                Paint paint = Paint.take();

                paint.setColor(wp.bgColor);
                paint.setStyle(Paint.Style.FILL);
                c.drawRect(leftX, top, rightX, bottom, paint);
            }

            c.drawTextRun(piece, leftX, y, wp);

            if (flags != 0) {
                if ((flags & TextPaint.UNDERLINE_FLAG) != 0) {
                    float thickness = piece.getAscent() / 12f;
                    float strokeTop = y + piece.getDescent() / 3f;
                    Paint paint = Paint.take();
                    paint.setColor(wp.getColor());
                    c.drawRect(leftX, strokeTop, rightX, strokeTop + thickness, paint);
                }
                if ((flags & TextPaint.STRIKETHROUGH_FLAG) != 0) {
                    float thickness = piece.getAscent() / 12f;
                    float strokeTop = y + piece.getAscent() / -2f;
                    Paint paint = Paint.take();
                    paint.setColor(wp.getColor());
                    c.drawRect(leftX, strokeTop, rightX, strokeTop + thickness, paint);
                }
            }
        }
        return runIsRtl ? -totalWidth : totalWidth;
    }

    /**
     * Utility function for measuring and rendering a replacement.
     *
     * @param replacement the replacement
     * @param wp          the work paint
     * @param start       the start of the run
     * @param limit       the limit of the run
     * @param runIsRtl    true if the run is right-to-left
     * @param canvas      the canvas, can be null if not rendering
     * @param x           the edge of the replacement closest to the leading margin
     * @param top         the top of the line
     * @param y           the baseline
     * @param bottom      the bottom of the line
     * @param fmi         receives metrics information, can be null
     * @param needWidth   true if the width of the replacement is needed
     * @return the signed width of the run based on the run direction; only
     * valid if needWidth is true
     */
    private float handleReplacement(@Nonnull ReplacementSpan replacement, @Nonnull TextPaint wp,
                                    int start, int limit, boolean runIsRtl, @Nullable Canvas canvas,
                                    float x, int top, int y, int bottom, @Nullable FontMetricsInt fmi,
                                    boolean needWidth) {

        float ret = 0;

        //FIXME replacement
        /*int textStart = mStart + start;
        int textLimit = mStart + limit;

        if (needWidth || (canvas != null && runIsRtl)) {
            int previousTop = 0;
            int previousAscent = 0;
            int previousDescent = 0;
            int previousBottom = 0;
            int previousLeading = 0;

            boolean needUpdateMetrics = (fmi != null);

            if (needUpdateMetrics) {
                previousTop     = fmi.top;
                previousAscent  = fmi.ascent;
                previousDescent = fmi.descent;
                previousBottom  = fmi.bottom;
                previousLeading = fmi.leading;
            }

            ret = replacement.getSize(wp, mText, textStart, textLimit, fmi);

            if (needUpdateMetrics) {
                updateMetrics(fmi, previousTop, previousAscent, previousDescent, previousBottom,
                        previousLeading);
            }
        }

        if (canvas != null) {
            if (runIsRtl) {
                x -= ret;
            }
            replacement.draw(canvas, mText, textStart, textLimit,
                    x, top, y, bottom, wp);
        }*/

        return runIsRtl ? -ret : ret;
    }

    /**
     * Draws a unidirectional (but possibly multi-styled) run of text.
     *
     * @param canvas   the canvas to draw on
     * @param x        the position of the run that is closest to the leading margin
     * @param y        the baseline
     * @param start    the line-relative start
     * @param limit    the line-relative limit
     * @param runIsRtl true if the run is right-to-left
     * @param wp       working paint
     * @param ss       recyclable span set
     */
    private void drawBidiRun(@Nonnull Canvas canvas, float x, float y, int start, int limit,
                             boolean runIsRtl, @Nonnull TextPaint wp, @Nonnull SpanSet ss) {
        final boolean plain;
        if (mSpanned == null) {
            plain = true;
        } else {
            ss.init(mSpanned, mStart + start, mStart + limit);
            plain = ss.mSpans.isEmpty();
        }
        if (plain) {
            // reset to base paint
            wp.set(mPaint);
            drawStyleRun(wp, start, limit, runIsRtl, canvas, x, y);
        } else {
            /*int runIndex = mMeasuredText.search(mStart + start);
            assert runIndex >= 0;
            final MeasuredText.Run[] runs = mMeasuredText.getRuns();
            while (runIndex < runs.length) {
                final MeasuredText.Run run = runs[runIndex++];
                final int runStart = run.mStart;
                final int runEnd = run.mEnd;
                // reset to base paint
                wp.set(mPaint);

                for (int k = 0; k < ss.mSpans.size(); k++) {
                    // Intentionally using >= and <= as explained above
                    if ((ss.mSpanStarts[k] >= runEnd) ||
                            (ss.mSpanEnds[k] <= runStart)) continue;

                    final CharacterStyle span = ss.mSpans.get(k);
                    span.updateDrawState(wp);
                }

                if (runIsRtl) {
                    x -= drawStyleRun(wp, runStart, runEnd, true, canvas, x, y);
                } else {
                    x += drawStyleRun(wp, runStart, runEnd, false, canvas, x, y);
                }
            }*/
        }
    }

    private float drawStyleRun(@Nonnull TextPaint paint, int start, int end, boolean runIsRtl,
                               @Nonnull Canvas canvas, float x, float y) {
        assert start != end;
        float advance = 0;//mMeasuredText.getAdvance(start, end);

        final float left, right;
        if (runIsRtl) {
            left = x - advance;
            right = x;
        } else {
            left = x;
            right = x + advance;
        }

        //canvas.drawTextRun(mMeasuredText, start, end, left, y, paint);

        return advance;
    }

    /**
     * Returns the next tab position.
     *
     * @param h the (unsigned) offset from the leading margin
     * @return the (unsigned) tab position after this offset
     */
    public float nextTab(float h) {
        if (mTabs != null) {
            return mTabs.nextTab(h);
        }
        return TabStops.nextDefaultStop(h, 20);
    }

    private static void expandMetricsFromPaint(@Nonnull FontMetricsInt fmi, TextPaint wp) {
        final int previousAscent = fmi.mAscent;
        final int previousDescent = fmi.mDescent;

        GlyphManager.getInstance().getFontMetrics(wp, fmi);

        fmi.extendBy(previousAscent, previousDescent);
    }
}
