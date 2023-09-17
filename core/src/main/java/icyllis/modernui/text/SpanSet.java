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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;

/**
 * A cached set of spans. Caches the result of {@link Spanned#getSpans(int, int, Class, java.util.List)} and then
 * provides faster access to {@link Spanned#nextSpanTransition(int, int, Class)}.
 * <p>
 * Fields are public for a convenient direct access (read only).
 * <p>
 * Note that empty spans are ignored by this class by default.
 */
public class SpanSet<E> extends ObjectArrayList<E> {

    final Class<? extends E> mType;
    final boolean mIgnoreEmptySpans;

    public int[] mSpanStarts;
    public int[] mSpanEnds;
    public int[] mSpanFlags;

    public SpanSet(@NonNull Class<? extends E> type) {
        this(type, true);
    }

    public SpanSet(@NonNull Class<? extends E> type, boolean ignoreEmptySpans) {
        mType = type;
        mIgnoreEmptySpans = ignoreEmptySpans;
    }

    void add(int index, E span, int start, int end, int flags) {
        grow(size + 1);
        if (index != size) {
            System.arraycopy(mSpanStarts, index, mSpanStarts, index + 1, size - index);
            System.arraycopy(mSpanEnds, index, mSpanEnds, index + 1, size - index);
            System.arraycopy(mSpanFlags, index, mSpanFlags, index + 1, size - index);
        }
        mSpanStarts[index] = start;
        mSpanEnds[index] = end;
        mSpanFlags[index] = flags;
        add(span);
    }

    private void grow(int length) {
        if (mSpanStarts == null) {
            length = Math.max(length, 10);
        } else if (mSpanStarts.length < length) {
            length = Math.max(length, mSpanStarts.length + (mSpanStarts.length >> 1));
        } else {
            length = 0;
        }
        if (length > 0) {
            // These arrays may end up being too large because of the discarded empty spans
            mSpanStarts = new int[length];
            mSpanEnds = new int[length];
            mSpanFlags = new int[length];
        }
    }

    public boolean init(@NonNull Spanned spanned, int start, int limit) {
        spanned.getSpans(start, limit, mType, this);
        final int length = size();

        if (length > 0) {
            if (!(spanned instanceof SpannableStringInternal)) {
                grow(length);

                int size = 0;
                for (Iterator<E> it = iterator(); it.hasNext(); ) {
                    E span = it.next();
                    final int spanStart = spanned.getSpanStart(span);
                    final int spanEnd = spanned.getSpanEnd(span);
                    if (mIgnoreEmptySpans && spanStart == spanEnd) {
                        it.remove();
                        continue;
                    }

                    final int spanFlag = spanned.getSpanFlags(span);

                    mSpanStarts[size] = spanStart;
                    mSpanEnds[size] = spanEnd;
                    mSpanFlags[size] = spanFlag;

                    size++;
                }
            }
            return size > 0;
        }
        return false;
    }

    /**
     * Returns true if there are spans intersecting the given interval.
     *
     * @param end must be strictly greater than start
     */
    public boolean hasSpansIntersecting(int start, int end) {
        for (int i = 0; i < size(); i++) {
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
        for (int i = 0; i < size(); i++) {
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
        clear();
    }
}
