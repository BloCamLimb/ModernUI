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

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * A cached set of spans. Caches the result of {@link Spanned#getSpans(int, int, Class)} and then
 * provides faster access to {@link Spanned#nextSpanTransition(int, int, Class)}.
 * <p>
 * Fields are left public for a convenient direct access.
 * <p>
 * Note that empty spans are ignored by this class.
 */
public class SpanSet<E> {

    private final Class<? extends E> classType;

    public int numberOfSpans;
    public E[] spans;
    public int[] spanStarts;
    public int[] spanEnds;
    public int[] spanFlags;

    public SpanSet(Class<? extends E> type) {
        classType = type;
        numberOfSpans = 0;
    }

    @SuppressWarnings("unchecked")
    public void init(@Nonnull Spanned spanned, int start, int limit) {
        final E[] allSpans = spanned.getSpans(start, limit, classType);
        final int length = allSpans.length;

        if (length > 0 && (spans == null || spans.length < length)) {
            // These arrays may end up being too large because of the discarded empty spans
            spans = (E[]) Array.newInstance(classType, length);
            spanStarts = new int[length];
            spanEnds = new int[length];
            spanFlags = new int[length];
        }

        int prevNumberOfSpans = numberOfSpans;
        numberOfSpans = 0;
        for (final E span : allSpans) {
            final int spanStart = spanned.getSpanStart(span);
            final int spanEnd = spanned.getSpanEnd(span);
            if (spanStart == spanEnd) continue;

            final int spanFlag = spanned.getSpanFlags(span);

            spans[numberOfSpans] = span;
            spanStarts[numberOfSpans] = spanStart;
            spanEnds[numberOfSpans] = spanEnd;
            spanFlags[numberOfSpans] = spanFlag;

            numberOfSpans++;
        }

        // cleanup extra spans left over from previous init() call
        if (numberOfSpans < prevNumberOfSpans) {
            // prevNumberofSpans was > 0, therefore spans != null
            Arrays.fill(spans, numberOfSpans, prevNumberOfSpans, null);
        }
    }

    /**
     * Returns true if there are spans intersecting the given interval.
     *
     * @param end must be strictly greater than start
     */
    public boolean hasSpansIntersecting(int start, int end) {
        for (int i = 0; i < numberOfSpans; i++) {
            // equal test is valid since both intervals are not empty by construction
            if (spanStarts[i] >= end || spanEnds[i] <= start) continue;
            return true;
        }
        return false;
    }

    /**
     * Similar to {@link Spanned#nextSpanTransition(int, int, Class)}
     */
    int getNextTransition(int start, int limit) {
        for (int i = 0; i < numberOfSpans; i++) {
            final int spanStart = spanStarts[i];
            final int spanEnd = spanEnds[i];
            if (spanStart > start && spanStart < limit) limit = spanStart;
            if (spanEnd > start && spanEnd < limit) limit = spanEnd;
        }
        return limit;
    }

    /**
     * Removes all internal references to the spans to avoid memory leaks.
     */
    public void recycle() {
        if (spans != null) {
            Arrays.fill(spans, 0, numberOfSpans, null);
        }
    }
}
