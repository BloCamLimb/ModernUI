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

import icyllis.modernui.text.style.CharacterStyle;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * A cached set of spans. Caches the result of {@link Spanned#getSpans(int, int, Class)} and then
 * provides faster access to {@link Spanned#nextSpanTransition(int, int, Class)}.
 * <p>
 * Fields are public for a convenient direct access (read only).
 * <p>
 * Note that empty spans are ignored by this class.
 */
public class SpanSet {

    private static final Pool<SpanSet> sPool = Pools.concurrent(1);

    public int mSize;
    public CharacterStyle[] mSpans;
    public int[] mSpanStarts;
    public int[] mSpanEnds;
    public int[] mSpanFlags;

    public SpanSet() {
    }

    @Nonnull
    public static SpanSet obtain() {
        SpanSet spanSet = sPool.acquire();
        if (spanSet == null) {
            return new SpanSet();
        }
        return spanSet;
    }

    public void init(@Nonnull Spanned spanned, int start, int limit) {
        final CharacterStyle[] allSpans = spanned.getSpans(start, limit, CharacterStyle.class);
        final int length = allSpans == null ? 0 : allSpans.length;

        if (length > 0 && (mSpans == null || mSpans.length < length)) {
            // These arrays may end up being too large because of the discarded empty spans
            mSpans = (CharacterStyle[]) Array.newInstance(CharacterStyle.class, length);
            mSpanStarts = new int[length];
            mSpanEnds = new int[length];
            mSpanFlags = new int[length];
        }

        int oldSize = mSize;
        mSize = 0;
        if (allSpans != null) {
            for (final CharacterStyle span : allSpans) {
                final int spanStart = spanned.getSpanStart(span);
                final int spanEnd = spanned.getSpanEnd(span);
                if (spanStart == spanEnd) continue;

                final int spanFlag = spanned.getSpanFlags(span);

                mSpans[mSize] = span;
                mSpanStarts[mSize] = spanStart;
                mSpanEnds[mSize] = spanEnd;
                mSpanFlags[mSize] = spanFlag;

                mSize++;
            }
        }

        // cleanup extra spans left over from previous init() call
        if (mSize < oldSize) {
            // oldSize was > 0, therefore mSpans != null
            Arrays.fill(mSpans, mSize, oldSize, null);
        }
    }

    /**
     * Returns true if there are spans intersecting the given interval.
     *
     * @param end must be strictly greater than start
     */
    public boolean hasSpansIntersecting(int start, int end) {
        for (int i = 0; i < mSize; i++) {
            // equal test is valid since both intervals are not empty by construction
            if (mSpanStarts[i] >= end || mSpanEnds[i] <= start) continue;
            return true;
        }
        return false;
    }

    /**
     * Similar to {@link Spanned#nextSpanTransition(int, int, Class)}
     */
    public int getNextTransition(int start, int limit) {
        for (int i = 0; i < mSize; i++) {
            final int spanStart = mSpanStarts[i];
            final int spanEnd = mSpanEnds[i];
            if (spanStart > start && spanStart < limit)
                limit = spanStart;
            if (spanEnd > start && spanEnd < limit)
                limit = spanEnd;
        }
        return limit;
    }

    /**
     * Removes all internal references to the spans to avoid memory leaks.
     */
    public void recycle() {
        if (mSpans != null) {
            Arrays.fill(mSpans, 0, mSize, null);
        }
        sPool.release(this);
    }
}
