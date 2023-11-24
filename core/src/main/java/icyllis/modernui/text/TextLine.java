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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.text.*;
import icyllis.modernui.text.style.*;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

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
@ApiStatus.Internal
public class TextLine {

    private static final Pools.Pool<TextLine> sPool = Pools.newSynchronizedPool(3);

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

    private final ArrayList<LayoutPiece> mCachedPieces = new ArrayList<>();
    private final FloatArrayList mCachedXOffsets = new FloatArrayList();
    private final FontMetricsInt mCachedFontExtent = new FontMetricsInt();

    private final ShapedText.RunConsumer mBuildCachedPieces = (piece, offsetX) -> {
        mCachedPieces.add(piece);
        mCachedXOffsets.add(offsetX);
    };

    private TextLine() {
    }

    /**
     * Returns a new TextLine from the shared pool.
     *
     * @return an uninitialized TextLine
     */
    @NonNull
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
    public void set(@NonNull TextPaint paint, @NonNull CharSequence text, int start, int limit, int dir,
                    @NonNull Directions directions, boolean hasTabs, @Nullable TabStops tabStops,
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
            if (!mComputed.getParams().getTextPaint().equalsForTextMeasurement(paint)) {
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
    public void draw(@NonNull Canvas canvas, float x, int top, int y, int bottom) {
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
     * Returns metrics information for the entire line.
     *
     * @param fmi receives font metrics information, can be null
     * @return the signed width of the line
     */
    public float metrics(@Nullable FontMetricsInt fmi) {
        return measure(mLen, false, fmi);
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
     * @return The measure results for all possible offsets
     * @see #measure(int, boolean, FontMetricsInt)
     */
    public float[] measureAllOffsets(boolean[] trailing, FontMetricsInt fmi) {
        float[] measurement = new float[mLen + 1];

        int[] target = new int[mLen + 1];
        for (int offset = 0; offset < target.length; ++offset) {
            target[offset] = trailing[offset] ? offset - 1 : offset;
        }
        if (target[0] < 0) {
            measurement[0] = 0;
        }

        float h = 0;
        for (int runIndex = 0; runIndex < mDirections.getRunCount(); runIndex++) {
            final int runStart = mDirections.getRunStart(runIndex);
            if (runStart > mLen) break;
            final int runLimit = Math.min(runStart + mDirections.getRunLength(runIndex), mLen);
            final boolean runIsRtl = mDirections.isRunRtl(runIndex);

            int segStart = runStart;
            for (int j = mHasTabs ? runStart : runLimit; j <= runLimit; ++j) {
                if (j == runLimit || charAt(j) == TAB_CHAR) {
                    final float oldh = h;
                    final boolean advance = (mDir == Layout.DIR_RIGHT_TO_LEFT) == runIsRtl;
                    final float w = measureRun(segStart, j, j, runIsRtl, fmi);
                    h += advance ? w : -w;

                    final float baseh = advance ? oldh : h;
                    FontMetricsInt crtfmi = advance ? fmi : null;
                    for (int offset = segStart; offset <= j && offset <= mLen; ++offset) {
                        if (target[offset] >= segStart && target[offset] < j) {
                            measurement[offset] =
                                    baseh + measureRun(segStart, offset, j, runIsRtl, crtfmi);
                        }
                    }

                    if (j != runLimit) {  // charAt(j) == TAB_CHAR
                        if (target[j] == j) {
                            measurement[j] = h;
                        }
                        h = mDir * nextTab(h * mDir);
                        if (target[j + 1] == j) {
                            measurement[j + 1] = h;
                        }
                    }

                    segStart = j + 1;
                }
            }
        }
        if (target[mLen] == mLen) {
            measurement[mLen] = h;
        }

        return measurement;
    }

    /**
     * Shape the TextLine.
     */
    void shape(@NonNull TextShaper.GlyphsConsumer consumer) {
        float horizontal = 0;
        float x = 0;
        final int runCount = mDirections.getRunCount();
        for (int runIndex = 0; runIndex < runCount; runIndex++) {
            final int runStart = mDirections.getRunStart(runIndex);
            if (runStart > mLen) break;
            final int runLimit = Math.min(runStart + mDirections.getRunLength(runIndex), mLen);
            final boolean runIsRtl = mDirections.isRunRtl(runIndex);

            int segStart = runStart;
            for (int j = mHasTabs ? runStart : runLimit; j <= runLimit; j++) {
                if (j == runLimit || charAt(j) == TAB_CHAR) {
                    horizontal += shapeRun(consumer, segStart, j, runIsRtl, x + horizontal,
                            runIndex != (runCount - 1) || j != mLen);

                    if (j != runLimit) {  // charAt(j) == TAB_CHAR
                        horizontal = mDir * nextTab(horizontal * mDir);
                    }
                    segStart = j + 1;
                }
            }
        }
    }

    /**
     * Walk the cursor through this line, skipping conjuncts and
     * zero-width characters.
     *
     * <p>This function cannot properly walk the cursor off the ends of the line
     * since it does not know about any shaping on the previous/following line
     * that might affect the cursor position. Callers must either avoid these
     * situations or handle the result specially.
     *
     * @param cursor the starting position of the cursor, between 0 and the
     *               length of the line, inclusive
     * @param toLeft true if the caret is moving to the left.
     * @return the new offset.  If it is less than 0 or greater than the length
     * of the line, the previous/following line should be examined to get the
     * actual offset.
     */
    public int getOffsetToLeftRightOf(int cursor, boolean toLeft) {
        // 1) The caret marks the leading edge of a character. The character
        // logically before it might be on a different level, and the active caret
        // position is on the character at the lower level. If that character
        // was the previous character, the caret is on its trailing edge.
        // 2) Take this character/edge and move it in the indicated direction.
        // This gives you a new character and a new edge.
        // 3) This position is between two visually adjacent characters.  One of
        // these might be at a lower level.  The active position is on the
        // character at the lower level.
        // 4) If the active position is on the trailing edge of the character,
        // the new caret position is the following logical character, else it
        // is the character.

        int lineStart = 0;
        int lineEnd = mLen;
        boolean paraIsRtl = mDir == -1;
        int[] runs = mDirections.mDirections;

        int runIndex, runLevel = 0, runStart = lineStart, runLimit = lineEnd, newCaret = -1;
        boolean trailing = false;

        if (cursor == lineStart) {
            runIndex = -2;
        } else if (cursor == lineEnd) {
            runIndex = runs.length;
        } else {
            // First, get information about the run containing the character with
            // the active caret.
            for (runIndex = 0; runIndex < runs.length; runIndex += 2) {
                runStart = lineStart + runs[runIndex];
                if (cursor >= runStart) {
                    runLimit = runStart + (runs[runIndex + 1] & Directions.RUN_LENGTH_MASK);
                    if (runLimit > lineEnd) {
                        runLimit = lineEnd;
                    }
                    if (cursor < runLimit) {
                        runLevel = (runs[runIndex + 1] >>> Directions.RUN_LEVEL_SHIFT) &
                                Directions.RUN_LEVEL_MASK;
                        if (cursor == runStart) {
                            // The caret is on a run boundary, see if we should
                            // use the position on the trailing edge of the previous
                            // logical character instead.
                            int prevRunIndex, prevRunLevel, prevRunStart, prevRunLimit;
                            int pos = cursor - 1;
                            for (prevRunIndex = 0; prevRunIndex < runs.length; prevRunIndex += 2) {
                                prevRunStart = lineStart + runs[prevRunIndex];
                                if (pos >= prevRunStart) {
                                    prevRunLimit = prevRunStart +
                                            (runs[prevRunIndex + 1] & Directions.RUN_LENGTH_MASK);
                                    if (prevRunLimit > lineEnd) {
                                        prevRunLimit = lineEnd;
                                    }
                                    if (pos < prevRunLimit) {
                                        prevRunLevel = (runs[prevRunIndex + 1] >>> Directions.RUN_LEVEL_SHIFT)
                                                & Directions.RUN_LEVEL_MASK;
                                        if (prevRunLevel < runLevel) {
                                            // Start from logically previous character.
                                            runIndex = prevRunIndex;
                                            runLevel = prevRunLevel;
                                            runStart = prevRunStart;
                                            runLimit = prevRunLimit;
                                            trailing = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // caret might be == lineEnd.  This is generally a space or paragraph
            // separator and has an associated run, but might be the end of
            // text, in which case it doesn't.  If that happens, we ran off the
            // end of the run list, and runIndex == runs.length.  In this case,
            // we are at a run boundary so we skip the below test.
            if (runIndex != runs.length) {
                boolean runIsRtl = (runLevel & 0x1) != 0;
                boolean advance = toLeft == runIsRtl;
                if (cursor != (advance ? runLimit : runStart) || advance != trailing) {
                    // Moving within or into the run, so we can move logically.
                    newCaret = getOffsetBeforeAfter(runIndex, runStart, runLimit,
                            runIsRtl, cursor, advance);
                    // If the new position is internal to the run, we're at the strong
                    // position already so we're finished.
                    if (newCaret != (advance ? runLimit : runStart)) {
                        return newCaret;
                    }
                }
            }
        }

        // If newCaret is -1, we're starting at a run boundary and crossing
        // into another run. Otherwise we've arrived at a run boundary, and
        // need to figure out which character to attach to.  Note we might
        // need to run this twice, if we cross a run boundary and end up at
        // another run boundary.
        while (true) {
            boolean advance = toLeft == paraIsRtl;
            int otherRunIndex = runIndex + (advance ? 2 : -2);
            if (otherRunIndex >= 0 && otherRunIndex < runs.length) {
                int otherRunStart = lineStart + runs[otherRunIndex];
                int otherRunLimit = otherRunStart +
                        (runs[otherRunIndex + 1] & Directions.RUN_LENGTH_MASK);
                if (otherRunLimit > lineEnd) {
                    otherRunLimit = lineEnd;
                }
                int otherRunLevel = (runs[otherRunIndex + 1] >>> Directions.RUN_LEVEL_SHIFT) &
                        Directions.RUN_LEVEL_MASK;
                boolean otherRunIsRtl = (otherRunLevel & 1) != 0;

                advance = toLeft == otherRunIsRtl;
                if (newCaret == -1) {
                    newCaret = getOffsetBeforeAfter(otherRunIndex, otherRunStart,
                            otherRunLimit, otherRunIsRtl,
                            advance ? otherRunStart : otherRunLimit, advance);
                    if (newCaret == (advance ? otherRunLimit : otherRunStart)) {
                        // Crossed and ended up at a new boundary,
                        // repeat a second and final time.
                        runIndex = otherRunIndex;
                        runLevel = otherRunLevel;
                        continue;
                    }
                    break;
                }

                // The new caret is at a boundary.
                if (otherRunLevel < runLevel) {
                    // The strong character is in the other run.
                    newCaret = advance ? otherRunStart : otherRunLimit;
                }
                break;
            }

            if (newCaret == -1) {
                // We're walking off the end of the line.  The paragraph
                // level is always equal to or lower than any internal level, so
                // the boundaries get the strong caret.
                newCaret = advance ? mLen + 1 : -1;
                break;
            }

            // Else we've arrived at the end of the line.  That's a strong position.
            // We might have arrived here by crossing over a run with no internal
            // breaks and dropping out of the above loop before advancing one final
            // time, so reset the caret.
            // Note, we use '<=' below to handle a situation where the only run
            // on the line is a counter-directional run.  If we're not advancing,
            // we can end up at the 'lineEnd' position but the caret we want is at
            // the lineStart.
            if (newCaret <= lineEnd) {
                newCaret = advance ? lineEnd : lineStart;
            }
            break;
        }

        return newCaret;
    }

    /**
     * Returns the next valid offset within this directional run, skipping
     * conjuncts and zero-width characters.  This should not be called to walk
     * off the end of the line, since the returned values might not be valid
     * on neighboring lines.  If the returned offset is less than zero or
     * greater than the line length, the offset should be recomputed on the
     * preceding or following line, respectively.
     *
     * @param runIndex the run index
     * @param runStart the start of the run
     * @param runLimit the limit of the run
     * @param runIsRtl true if the run is right-to-left
     * @param offset   the offset
     * @param after    true if the new offset should logically follow the provided
     *                 offset
     * @return the new offset
     */
    private int getOffsetBeforeAfter(int runIndex, int runStart, int runLimit,
                                     boolean runIsRtl, int offset, boolean after) {
        if (runIndex < 0 || offset == (after ? mLen : 0)) {
            // Walking off end of line.  Since we don't know
            // what cursor positions are available on other lines, we can't
            // return accurate values.  These are a guess.
            final CharSequence text = mText;
            offset += mStart;
            if (after) {
                int len = text.length();

                if (offset == len || offset == len - 1) {
                    return len - mStart;
                }

                char c = text.charAt(offset);

                if (c >= '\uD800' && c <= '\uDBFF') {
                    char c1 = text.charAt(offset + 1);

                    if (c1 >= '\uDC00' && c1 <= '\uDFFF')
                        offset += 2;
                    else
                        offset += 1;
                } else {
                    offset += 1;
                }

                if (mSpanned != null) {
                    mReplacementSpanSpanSet.init(mSpanned, offset, offset);

                    for (int i = 0; i < mReplacementSpanSpanSet.size(); i++) {
                        int start = mReplacementSpanSpanSet.mSpanStarts[i];
                        int end = mReplacementSpanSpanSet.mSpanEnds[i];

                        if (start < offset && end > offset)
                            offset = end;
                    }
                }
            } else {
                if (offset == 0 || offset == 1) {
                    return -mStart;
                }

                char c = text.charAt(offset - 1);

                if (c >= '\uDC00' && c <= '\uDFFF') {
                    char c1 = text.charAt(offset - 2);

                    if (c1 >= '\uD800' && c1 <= '\uDBFF')
                        offset -= 2;
                    else
                        offset -= 1;
                } else {
                    offset -= 1;
                }

                if (mSpanned != null) {
                    mReplacementSpanSpanSet.init(mSpanned, offset, offset);

                    for (int i = 0; i < mReplacementSpanSpanSet.size(); i++) {
                        int start = mReplacementSpanSpanSet.mSpanStarts[i];
                        int end = mReplacementSpanSpanSet.mSpanEnds[i];

                        if (start < offset && end > offset)
                            offset = start;
                    }
                }
            }
            return offset - mStart;
        }

        TextPaint wp = mWorkPaint;
        wp.set(mPaint);

        int spanStart = runStart;
        int spanLimit;
        if (mSpanned == null) {
            spanLimit = runLimit;
        } else {
            int target = after ? offset + 1 : offset;
            int limit = mStart + runLimit;
            if (mMetricAffectingSpanSpanSet.init(mSpanned, mStart + spanStart, limit)) {
                while (true) {
                    spanLimit = mMetricAffectingSpanSpanSet.getNextTransition(mStart + spanStart, limit) - mStart;
                    if (spanLimit >= target) {
                        break;
                    }
                    spanStart = spanLimit;
                }

                ReplacementSpan replacement = null;
                for (int j = 0; j < mMetricAffectingSpanSpanSet.size(); j++) {
                    MetricAffectingSpan span = mMetricAffectingSpanSpanSet.get(j);

                    if ((mMetricAffectingSpanSpanSet.mSpanStarts[j] >= mStart + spanLimit) ||
                            (mMetricAffectingSpanSpanSet.mSpanEnds[j] <= mStart + spanStart)) continue;

                    if (span instanceof ReplacementSpan) {
                        replacement = (ReplacementSpan) span;
                    } else {
                        span.updateMeasureState(wp);
                    }
                }

                if (replacement != null) {
                    // If we have a replacement span, we're moving either to
                    // the start or end of this span.
                    return after ? spanLimit : spanStart;
                }
            } else {
                spanLimit = runLimit;
            }
        }

        int cursorOpt = after ? GraphemeBreak.AFTER : GraphemeBreak.BEFORE;
        if (mCharsValid) {
            return wp.getTextRunCursor(mChars, spanStart,
                    spanLimit - spanStart, offset, cursorOpt);
        } else {
            return wp.getTextRunCursor(mText, mStart + spanStart,
                    mStart + spanLimit, mStart + offset, cursorOpt) - mStart;
        }
    }

    /**
     * Draws a unidirectional (but possibly multi-styled) run of text.
     *
     * @param c         the canvas to draw on
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
    private float drawRun(@NonNull Canvas c,
                          int start, int limit,
                          boolean runIsRtl,
                          float x, int top,
                          int y, int bottom,
                          boolean needWidth) {

        if ((mDir == Layout.DIR_LEFT_TO_RIGHT) == runIsRtl) {
            float w = -measureRun(start, limit, limit, runIsRtl, null);
            handleRun(start, limit, limit, runIsRtl, c, null, x + w, top,
                    y, bottom, null, false);
            return w;
        }

        return handleRun(start, limit, limit, runIsRtl, c, null, x, top,
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
        return handleRun(start, offset, limit, runIsRtl, null, null, 0, 0, 0, 0, fmi, true);
    }

    /**
     * Shape a unidirectional (but possibly multi-styled) run of text.
     *
     * @param consumer  the consumer of the shape result
     * @param start     the line-relative start
     * @param limit     the line-relative limit
     * @param runIsRtl  true if the run is right-to-left
     * @param x         the position of the run that is closest to the leading margin
     * @param needWidth true if the width value is required.
     * @return the signed width of the run, based on the paragraph direction.
     * Only valid if needWidth is true.
     */
    private float shapeRun(@NonNull TextShaper.GlyphsConsumer consumer, int start,
                           int limit, boolean runIsRtl, float x, boolean needWidth) {

        if ((mDir == Layout.DIR_LEFT_TO_RIGHT) == runIsRtl) {
            float w = -measureRun(start, limit, limit, runIsRtl, null);
            handleRun(start, limit, limit, runIsRtl, null, consumer, x + w, 0, 0, 0, null, false);
            return w;
        }

        return handleRun(start, limit, limit, runIsRtl, null, consumer, x, 0, 0, 0, null,
                needWidth);
    }

    /**
     * Utility function for handling a unidirectional run.  The run must not
     * contain tabs but can contain styles.
     *
     * @param start        the line-relative start of the run
     * @param measureLimit the offset to measure to, between start and limit inclusive
     * @param limit        the limit of the run
     * @param runIsRtl     true if the run is right-to-left
     * @param c            the canvas, can be null
     * @param consumer     the output positioned glyphs, can be null
     * @param x            the end of the run closest to the leading margin
     * @param top          the top of the line
     * @param y            the baseline
     * @param bottom       the bottom of the line
     * @param fmi          receives metrics information, can be null
     * @param needWidth    true if the width is required
     * @return the signed width of the run based on the run direction; only
     * valid if needWidth is true
     */
    private float handleRun(int start, int measureLimit,
                            int limit, boolean runIsRtl,
                            @Nullable Canvas c,
                            @Nullable TextShaper.GlyphsConsumer consumer,
                            float x, int top, int y, int bottom,
                            @Nullable FontMetricsInt fmi, boolean needWidth) {
        if (measureLimit < start || measureLimit > limit) {
            throw new IndexOutOfBoundsException("measureLimit (" + measureLimit + ") is out of "
                    + "start (" + start + ") and limit (" + limit + ") bounds");
        }

        // Case of an empty line, make sure we update fmi according to mPaint
        if (start == measureLimit) {
            if (fmi != null) {
                final TextPaint wp = mWorkPaint;
                wp.set(mPaint);
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
            return handleText(wp, start, limit, start, limit, runIsRtl, c, consumer, x, top,
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

            for (int j = 0; j < mMetricAffectingSpanSpanSet.size(); j++) {
                // Both intervals [spanStarts..spanEnds] and [mStart + i..mStart + mlimit] are NOT
                // empty by construction. This special case in getSpans() explains the >= & <= tests
                if ((mMetricAffectingSpanSpanSet.mSpanStarts[j] >= mStart + mlimit)
                        || (mMetricAffectingSpanSpanSet.mSpanEnds[j] <= mStart + i)) continue;

                final MetricAffectingSpan span = mMetricAffectingSpanSpanSet.get(j);
                if (span instanceof ReplacementSpan) {
                    boolean insideEllipsis =
                            mStart + mEllipsisStart <= mMetricAffectingSpanSpanSet.mSpanStarts[j]
                                    && mMetricAffectingSpanSpanSet.mSpanEnds[j] <= mStart + mEllipsisEnd;
                    replacement = insideEllipsis ? null : (ReplacementSpan) span;
                } else {
                    // We might have a replacement that uses the draw
                    // state, otherwise measure state would suffice.
                    span.updateDrawState(wp);
                }
            }

            if (replacement != null) {
                x += handleReplacement(replacement, wp, i, mlimit, runIsRtl, c, x, top, y,
                        bottom, fmi, needWidth || mlimit < measureLimit);
                continue;
            }

            // Get metrics first (even for empty strings or "0" width runs)
            if (fmi != null) {
                expandMetricsFromPaint(fmi, wp);
            }

            final TextPaint activePaint = mActivePaint;
            for (int j = i, jnext; j < mlimit; j = jnext) {
                jnext = mCharacterStyleSpanSet.getNextTransition(mStart + j, mStart + inext) -
                        mStart;

                final int offset = Math.min(jnext, mlimit);
                activePaint.set(mPaint);
                for (int k = 0; k < mCharacterStyleSpanSet.size(); k++) {
                    // Intentionally using >= and <= as explained above
                    if ((mCharacterStyleSpanSet.mSpanStarts[k] >= mStart + offset) ||
                            (mCharacterStyleSpanSet.mSpanEnds[k] <= mStart + j)) continue;

                    final CharacterStyle span = mCharacterStyleSpanSet.get(k);
                    span.updateDrawState(activePaint);
                }

                final int flags =
                        activePaint.getFontFlags() & (TextPaint.UNDERLINE_FLAG | TextPaint.STRIKETHROUGH_FLAG);
                activePaint.setFontFlags(activePaint.getFontFlags() & ~(TextPaint.UNDERLINE_FLAG | TextPaint.STRIKETHROUGH_FLAG));

                x += handleText(activePaint, j, jnext, i, inext, runIsRtl, c, consumer, x,
                        top, y, bottom, needWidth || jnext < measureLimit,
                        offset, flags);
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
     * @param consumer  the output positioned glyph list, can be null if not necessary
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
    private float handleText(@NonNull TextPaint wp, int start, int end,
                             int contextStart, int contextEnd, boolean runIsRtl,
                             @Nullable Canvas c, @Nullable TextShaper.GlyphsConsumer consumer,
                             float x, int top, int y, int bottom,
                             boolean needWidth, int offset, int flags) {
        // No need to do anything if the run width is "0"
        if (end == start) {
            return 0f;
        }

        if (consumer != null) {
            assert c == null;
            return shapeTextRun(consumer, wp, start, end, contextStart, contextEnd, runIsRtl, x);
        }

        float totalWidth = 0;

        if (c != null || needWidth) {
            mCachedFontExtent.reset();
            if (mCharsValid) {
                totalWidth = ShapedText.doLayoutRun(
                        mChars, contextStart, contextEnd, start, offset,
                        runIsRtl, wp.getInternalPaint(),
                        mCachedFontExtent,
                        mBuildCachedPieces
                );
            } else {
                final int delta = mStart;
                final int len = contextEnd - contextStart;
                final char[] buf = TextUtils.obtain(len);
                TextUtils.getChars(mText, contextStart + delta, contextEnd + delta, buf, 0);
                totalWidth = ShapedText.doLayoutRun(
                        buf, 0, len, start - contextStart, offset - contextStart,
                        runIsRtl, wp.getInternalPaint(),
                        mCachedFontExtent,
                        mBuildCachedPieces
                );
                TextUtils.recycle(buf);
            }
            //TODO take use of PrecomputedText
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

            Paint paint = null;
            if (wp.bgColor != 0) {
                paint = Paint.obtain();
                paint.setColor(wp.bgColor);
                paint.setStyle(Paint.FILL);
                c.drawRect(leftX, top, rightX, bottom, paint);
            }

            ArrayList<LayoutPiece> pieces = mCachedPieces;
            FloatArrayList xOffsets = mCachedXOffsets;
            assert pieces.size() == xOffsets.size();
            for (int i = 0, count = pieces.size(); i < count; i++) {
                TextUtils.drawTextRun(c,
                        pieces.get(i), leftX + xOffsets.getFloat(i), y, wp);
            }

            if (flags != 0) {
                if (paint == null) {
                    paint = Paint.obtain();
                } else {
                    paint.reset();
                }
                //TODO we assume these values for now, should we extract these values from TrueType file?
                // Also, TextPaint is not yet synchronized with this
                if ((flags & TextPaint.UNDERLINE_FLAG) != 0) {
                    float thickness = wp.getFontSize() / 18f;
                    float strokeTop = y + wp.getFontSize() * (1f / 9f) - thickness * 0.5f;
                    paint.setColor(wp.getColor());
                    c.drawRect(leftX, strokeTop, rightX, strokeTop + thickness, paint);
                }
                if ((flags & TextPaint.STRIKETHROUGH_FLAG) != 0) {
                    float thickness = wp.getFontSize() / 18f;
                    float strokeTop = y - wp.getFontSize() * (1f / 3f) - thickness * 0.5f;
                    paint.setColor(wp.getColor());
                    c.drawRect(leftX, strokeTop, rightX, strokeTop + thickness, paint);
                }
            }
            if (paint != null) {
                paint.recycle();
            }
        }

        mCachedPieces.clear();
        mCachedXOffsets.clear();

        return runIsRtl ? -totalWidth : totalWidth;
    }

    /**
     * Shape a text run with the set-up paint.
     *
     * @param consumer     the output positioned glyphs list
     * @param paint        the paint used to render the text
     * @param start        the start of the run
     * @param end          the end of the run
     * @param contextStart the start of context for the run
     * @param contextEnd   the end of the context for the run
     * @param runIsRtl     true if the run is right-to-left
     * @param x            the x position of the left edge of the run
     */
    private float shapeTextRun(TextShaper.GlyphsConsumer consumer, TextPaint paint,
                               int start, int end, int contextStart, int contextEnd, boolean runIsRtl, float x) {

        int count = end - start;
        int contextCount = contextEnd - contextStart;
        ShapedText glyphs;
        if (mCharsValid) {
            glyphs = TextShaper.shapeTextRun(
                    mChars,
                    start, count,
                    contextStart, contextCount,
                    runIsRtl,
                    paint
            );
        } else {
            glyphs = TextShaper.shapeTextRun(
                    mText,
                    mStart + start, count,
                    mStart + contextStart, contextCount,
                    runIsRtl,
                    paint
            );
        }
        float totalWidth = glyphs.getAdvance();
        if (runIsRtl) {
            x -= totalWidth;
        }
        consumer.accept(start, count, glyphs, paint, x, 0);

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
    private float handleReplacement(@NonNull ReplacementSpan replacement, @NonNull TextPaint wp,
                                    int start, int limit, boolean runIsRtl, @Nullable Canvas canvas,
                                    float x, int top, int y, int bottom, @Nullable FontMetricsInt fmi,
                                    boolean needWidth) {

        float ret = 0;

        int textStart = mStart + start;
        int textLimit = mStart + limit;

        if (needWidth || (canvas != null && runIsRtl)) {
            int previousAscent = 0;
            int previousDescent = 0;

            boolean needUpdateMetrics = (fmi != null);

            if (needUpdateMetrics) {
                previousAscent = fmi.ascent;
                previousDescent = fmi.descent;
            }

            ret = replacement.getSize(wp, mText, textStart, textLimit, fmi);

            if (needUpdateMetrics) {
                fmi.extendBy(previousAscent, previousDescent);
            }
        }

        if (canvas != null) {
            if (runIsRtl) {
                x -= ret;
            }
            replacement.draw(canvas, mText, textStart, textLimit,
                    x, top, y, bottom, wp);
        }

        return runIsRtl ? -ret : ret;
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

    private static void expandMetricsFromPaint(@NonNull FontMetricsInt fmi, @NonNull TextPaint wp) {
        final int previousAscent = fmi.ascent;
        final int previousDescent = fmi.descent;
        final int previousLeading = fmi.leading;

        wp.getFontMetricsInt(fmi);

        fmi.extendBy(previousAscent, previousDescent);
        fmi.leading = Math.max(fmi.leading, previousLeading);
    }

    private void expandMetricsFromPaint(@NonNull TextPaint wp, int start, int end,
                                        int contextStart, int contextEnd, boolean runIsRtl,
                                        @NonNull FontMetricsInt fmi) {
        final int previousAscent = fmi.ascent;
        final int previousDescent = fmi.descent;
        final int previousLeading = fmi.leading;

        if (mComputed == null) {
            //TODO this is incorrect
            wp.getFontMetricsInt(fmi);
        } else {
            mComputed.getFontMetricsInt(mStart + start, mStart + end, fmi);
        }

        fmi.extendBy(previousAscent, previousDescent);
        fmi.leading = Math.max(fmi.leading, previousLeading);
    }
}
