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

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

/**
 * A cached set of spans. Caches the result of {@link Spanned#getSpans(int, int, Class, java.util.List)} and then
 * provides faster access to {@link Spanned#nextSpanTransition(int, int, Class)}. Also retrieves
 * spanStart, spanEnd, spanFlags simultaneously with constant complexity.
 * <ul>
 * <li>Fields are public for a convenient direct access (read only).</li>
 * <li>Empty spans are ignored by default.</li>
 * <li>Cannot be used as normal ArrayList (you can only consume this object after init).</li>
 * </ul>
 */
@ApiStatus.Experimental
public class SpanSet<E> extends ArrayList<E> {

    private final Class<? extends E> mType;
    private final boolean mIgnoreEmptySpans;

    public int[] spanStarts;
    public int[] spanEnds;
    public int[] spanFlags;

    public SpanSet(@NonNull Class<? extends E> type) {
        this(type, true);
    }

    public SpanSet(@NonNull Class<? extends E> type, boolean ignoreEmptySpans) {
        mType = type;
        mIgnoreEmptySpans = ignoreEmptySpans;
    }

    void add(E span, int start, int end, int flags) {
        int size = size();
        grow(size + 1);
        spanStarts[size] = start;
        spanEnds[size] = end;
        spanFlags[size] = flags;
        add(span);
    }

    void add(int index, E span, int start, int end, int flags) {
        int size = size();
        grow(size + 1);
        if (index != size) {
            System.arraycopy(spanStarts, index, spanStarts, index + 1, size - index);
            System.arraycopy(spanEnds, index, spanEnds, index + 1, size - index);
            System.arraycopy(spanFlags, index, spanFlags, index + 1, size - index);
        }
        spanStarts[index] = start;
        spanEnds[index] = end;
        spanFlags[index] = flags;
        add(index, span);
    }

    private void grow(int length) {
        if (spanStarts == null) {
            length = Math.max(length, 10);
        } else if (spanStarts.length < length) {
            length = Math.max(length, spanStarts.length + (spanStarts.length >> 1));
        } else {
            length = 0;
        }
        if (length > 0) {
            // These arrays may end up being too large because of the discarded empty spans
            spanStarts = new int[length];
            spanEnds = new int[length];
            spanFlags = new int[length];
        }
    }

    /**
     * @return true if non-empty
     */
    public boolean init(@NonNull Spanned spanned, int start, int limit) {
        // Proxy classes
        if (spanned instanceof Layout.SpannedEllipsizer)
            spanned = ((Layout.SpannedEllipsizer) spanned).mSpanned;
        if (spanned instanceof PrecomputedText)
            spanned = ((PrecomputedText) spanned).getText();

        if (spanned instanceof SpannableStringInternal) {
            return ((SpannableStringInternal) spanned).getSpans(start, limit,
                    mType, mIgnoreEmptySpans, this);
        }
        spanned.getSpans(start, limit, mType, this);
        final int length = size();

        if (length > 0) {
            grow(length);

            int i = 0;
            while (i < size()) {
                E span = get(i);
                final int spanStart = spanned.getSpanStart(span);
                final int spanEnd = spanned.getSpanEnd(span);
                if (mIgnoreEmptySpans && spanStart == spanEnd) {
                    remove(i);
                    continue;
                }

                final int spanFlag = spanned.getSpanFlags(span);

                spanStarts[i] = spanStart;
                spanEnds[i] = spanEnd;
                spanFlags[i] = spanFlag;

                i++;
            }
            assert i == size();
            return i != 0;
        }
        return false;
    }

    public int getSpanStart(int index) {
        assert index >= 0 && index < size();
        return spanStarts[index];
    }

    public int getSpanEnd(int index) {
        assert index >= 0 && index < size();
        return spanEnds[index];
    }

    public int getSpanFlags(int index) {
        assert index >= 0 && index < size();
        return spanFlags[index];
    }

    /**
     * Returns true if there are spans intersecting the given interval.
     *
     * @param end must be strictly greater than start
     */
    public boolean hasSpansIntersecting(int start, int end) {
        for (int i = 0; i < size(); i++) {
            // equal test is valid since both intervals are not empty by construction
            if (spanStarts[i] >= end || spanEnds[i] <= start) continue;
            return true;
        }
        return false;
    }

    /**
     * Similar to {@link Spanned#nextSpanTransition(int, int, Class)}
     */
    public int getNextTransition(int start, int limit) {
        for (int i = 0; i < size(); i++) {
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
        clear();
    }
}
