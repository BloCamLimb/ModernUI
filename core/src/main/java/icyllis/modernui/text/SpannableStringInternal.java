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

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

import java.util.*;
import java.util.stream.IntStream;

// modified version of https://android.googlesource.com/
abstract class SpannableStringInternal implements Spanned, GetChars {

    private static final int START = 0;
    private static final int END = 1;
    private static final int FLAGS = 2;
    private static final int COLUMNS = 3;

    private final String mText;
    private Object[] mSpans;
    private int[] mSpanData;
    private int mSpanCount;

    SpannableStringInternal(CharSequence source, int start, int end, boolean ignoreNoCopySpan) {
        if (start == 0 && end == source.length())
            mText = source.toString();
        else
            mText = source.toString().substring(start, end);

        mSpans = ObjectArrays.EMPTY_ARRAY;
        // Invariant: mSpanData.length = mSpans.length * COLUMNS
        mSpanData = IntArrays.EMPTY_ARRAY;

        if (source instanceof Spanned) {
            if (source instanceof SpannableStringInternal) {
                copySpansFromInternal((SpannableStringInternal) source, start, end, ignoreNoCopySpan);
            } else {
                copySpansFromSpanned((Spanned) source, start, end, ignoreNoCopySpan);
            }
        }
    }

    /**
     * Copies another {@link Spanned} object's spans between [start, end] into this object.
     *
     * @param src              Source object to copy from.
     * @param start            Start index in the source object.
     * @param end              End index in the source object.
     * @param ignoreNoCopySpan whether to copy NoCopySpans in the {@code source}
     */
    private void copySpansFromSpanned(@NonNull Spanned src, int start, int end, boolean ignoreNoCopySpan) {
        List<Object> spans = src.getSpans(start, end, Object.class);

        for (Object span : spans) {
            if (ignoreNoCopySpan && span instanceof NoCopySpan) {
                continue;
            }
            int st = src.getSpanStart(span);
            int en = src.getSpanEnd(span);
            int fl = src.getSpanFlags(span);

            if (st < start)
                st = start;
            if (en > end)
                en = end;

            setSpan(span, st - start, en - start, fl, false/*enforceParagraph*/);
        }
    }

    /**
     * Copies a {@link SpannableStringInternal} object's spans between [start, end] into this
     * object.
     *
     * @param src              Source object to copy from.
     * @param start            Start index in the source object.
     * @param end              End index in the source object.
     * @param ignoreNoCopySpan copy NoCopySpan for backward compatible reasons.
     */
    private void copySpansFromInternal(@NonNull SpannableStringInternal src, int start, int end,
                                       boolean ignoreNoCopySpan) {
        int count = 0;
        final int[] srcData = src.mSpanData;
        final Object[] srcSpans = src.mSpans;
        final int limit = src.mSpanCount;
        boolean hasNoCopySpan = false;

        for (int i = 0; i < limit; i++) {
            int spanStart = srcData[i * COLUMNS + START];
            int spanEnd = srcData[i * COLUMNS + END];
            if (isOutOfCopyRange(start, end, spanStart, spanEnd)) continue;
            if (srcSpans[i] instanceof NoCopySpan) {
                hasNoCopySpan = true;
                if (ignoreNoCopySpan) {
                    continue;
                }
            }
            count++;
        }

        if (count == 0) return;

        if (!hasNoCopySpan && start == 0 && end == src.length()) {
            mSpans = new Object[src.mSpans.length];
            mSpanData = new int[src.mSpanData.length];
            mSpanCount = src.mSpanCount;
            System.arraycopy(src.mSpans, 0, mSpans, 0, src.mSpans.length);
            System.arraycopy(src.mSpanData, 0, mSpanData, 0, mSpanData.length);
        } else {
            mSpanCount = count;
            mSpans = new Object[mSpanCount];
            mSpanData = new int[mSpans.length * COLUMNS];
            for (int i = 0, j = 0; i < limit; i++) {
                int spanStart = srcData[i * COLUMNS + START];
                int spanEnd = srcData[i * COLUMNS + END];
                if (isOutOfCopyRange(start, end, spanStart, spanEnd)
                        || (ignoreNoCopySpan && srcSpans[i] instanceof NoCopySpan)) {
                    continue;
                }
                if (spanStart < start) spanStart = start;
                if (spanEnd > end) spanEnd = end;

                mSpans[j] = srcSpans[i];
                mSpanData[j * COLUMNS + START] = spanStart - start;
                mSpanData[j * COLUMNS + END] = spanEnd - start;
                mSpanData[j * COLUMNS + FLAGS] = srcData[i * COLUMNS + FLAGS];
                j++;
            }
        }
    }

    /**
     * Checks if [spanStart, spanEnd] interval is excluded from [start, end].
     *
     * @return True if excluded, false if included.
     */
    private static boolean isOutOfCopyRange(int start, int end, int spanStart, int spanEnd) {
        if (spanStart > end || spanEnd < start) return true;
        if (spanStart != spanEnd && start != end) {
            return spanStart == end || spanEnd == start;
        }
        return false;
    }

    public final void setSpan(@NonNull Object span, int start, int end, int flags) {
        setSpan(span, start, end, flags, true);
    }

    private boolean isIndexFollowsNextLine(int index) {
        return index != 0 && index != length() && charAt(index - 1) != '\n';
    }

    // compare 'start' then 'end'
    private int binarySearch(int start, int end) {
        int low = 0, high = mSpanCount - 1;
        final int[] data = mSpanData;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final int base = mid * COLUMNS;
            int cmp = Integer.compare(data[base + START], start);
            if (cmp == 0)
                cmp = Integer.compare(data[base + END], end);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return low;
    }

    private void setSpan(@NonNull Object span, int start, int end, int flags, boolean enforceParagraph) {
        Objects.requireNonNull(span, "span");
        if ((start | end - start | length() - end) < 0) {
            throw new IndexOutOfBoundsException(
                    String.format("Range [%d, %d) out of bounds for length %d",
                            start, end, length())
            );
        }

        if ((flags & Spannable.SPAN_PARAGRAPH) == Spannable.SPAN_PARAGRAPH) {
            if (isIndexFollowsNextLine(start)) {
                if (!enforceParagraph) {
                    // do not set the span
                    return;
                }
                throw new RuntimeException("PARAGRAPH span must start at paragraph boundary"
                        + " (" + start + " follows " + charAt(start - 1) + ")");
            }

            if (isIndexFollowsNextLine(end)) {
                if (!enforceParagraph) {
                    // do not set the span
                    return;
                }
                throw new RuntimeException("PARAGRAPH span must end at paragraph boundary"
                        + " (" + end + " follows " + charAt(end - 1) + ")");
            }
        }

        int count = mSpanCount;
        Object[] spans = mSpans;
        int[] data = mSpanData;

        for (int i = 0; i < count; i++) {
            if (spans[i] == span) {
                int ost = data[i * COLUMNS + START];
                int oen = data[i * COLUMNS + END];

                data[i * COLUMNS + START] = start;
                data[i * COLUMNS + END] = end;
                data[i * COLUMNS + FLAGS] = flags;

                sendSpanChanged(span, ost, oen, start, end);
                return;
            }
        }

        if (count == spans.length) {
            int newSize = count == 0 ? 10 : count + (count >> 1);
            Object[] newSpans = new Object[newSize];
            int[] newData = new int[newSize * COLUMNS];

            System.arraycopy(spans, 0, newSpans, 0, count);
            System.arraycopy(data, 0, newData, 0, count * COLUMNS);

            mSpans = newSpans;
            mSpanData = newData;
        }

        int pos = binarySearch(start, end);
        int base = pos * COLUMNS;
        if (pos != mSpanCount) {
            System.arraycopy(mSpans, pos,
                    mSpans, pos + 1,
                    mSpanCount - pos);
            System.arraycopy(mSpanData, base,
                    mSpanData, base + COLUMNS,
                    mSpanCount * COLUMNS - base);
        }
        mSpans[pos] = span;
        mSpanData[base + START] = start;
        mSpanData[base + END] = end;
        mSpanData[base + FLAGS] = flags;
        mSpanCount++;

        if (this instanceof Spannable) {
            sendSpanAdded(span, start, end);
        }
    }

    public final void removeSpan(@NonNull Object span) {
        removeSpan(span, 0);
    }

    public final void removeSpan(@NonNull Object span, int flags) {
        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == span) {
                int ost = data[i * COLUMNS + START];
                int oen = data[i * COLUMNS + END];

                int c = count - (i + 1);

                if (c != 0) {
                    System.arraycopy(spans, i + 1, spans, i, c);
                    System.arraycopy(data, (i + 1) * COLUMNS,
                            data, i * COLUMNS, c * COLUMNS);
                }

                mSpanCount--;

                if ((flags & Spanned.SPAN_INTERMEDIATE) == 0) {
                    sendSpanRemoved(span, ost, oen);
                }
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public final <T> List<T> getSpans(int start, int end, Class<? extends T> type,
                                      @Nullable List<T> dest) {
        if (dest != null) {
            dest.clear();
        }
        if (mSpanCount == 0) {
            return dest != null ? dest : Collections.emptyList();
        }

        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        final boolean check = type != null && type != Object.class;
        final SpanSet<T> ss;
        final boolean ignoreEmptySpans;
        if (dest instanceof SpanSet<T>) {
            ss = (SpanSet<T>) dest;
            ignoreEmptySpans = ss.mIgnoreEmptySpans;
        } else {
            ss = null;
            ignoreEmptySpans = false;
        }

        int found = 0;
        T first = null;

        int limit = binarySearch(end + 1, 0);

        for (int i = 0, base = 0;
             i < limit;
             i++, base += COLUMNS) {
            int spanStart = data[base + START];
            int spanEnd = data[base + END];

            if (spanStart > end || spanEnd < start) {
                continue;
            }

            if (spanStart != spanEnd && start != end) {
                if (spanStart == end || spanEnd == start) {
                    continue;
                }
            }

            if (check && !type.isInstance(spans[i])) {
                continue;
            }

            if (ignoreEmptySpans && spanStart == spanEnd) {
                continue;
            }

            if (dest != null || found > 0) {
                if (dest == null) {
                    dest = new ArrayList<>();
                    dest.add(first);
                }

                final int priority = data[base + FLAGS] & Spanned.SPAN_PRIORITY;
                if (priority != 0) {
                    int j = 0;
                    for (; j < found; j++) {
                        final int jPriority;
                        if (ss != null) {
                            jPriority = ss.mSpanFlags[j] & Spanned.SPAN_PRIORITY;
                        } else {
                            jPriority = getSpanFlags(dest.get(j)) & Spanned.SPAN_PRIORITY;
                        }
                        if (priority > jPriority) {
                            break;
                        }
                    }
                    if (ss != null) {
                        ss.add(j, (T) spans[i], spanStart, spanEnd, data[base + FLAGS]);
                    } else {
                        dest.add(j, (T) spans[i]);
                    }
                } else {
                    if (ss != null) {
                        ss.add((T) spans[i], spanStart, spanEnd, data[base + FLAGS]);
                    } else {
                        dest.add((T) spans[i]);
                    }
                }
            } else {
                assert found == 0;
                assert first == null;
                first = (T) spans[i];
            }
            found++;
        }

        if (dest != null) {
            assert found == dest.size();
            return dest;
        } else if (found == 0) {
            return Collections.emptyList();
        } else {
            assert found == 1;
            assert first != null;
            return List.of(first);
        }
    }

    @Override
    public int getSpanStart(@NonNull Object span) {
        final Object[] spans = mSpans;
        for (int i = mSpanCount - 1; i >= 0; i--) {
            if (spans[i] == span) {
                return mSpanData[i * COLUMNS + START];
            }
        }
        return -1;
    }

    @Override
    public int getSpanEnd(@NonNull Object span) {
        final Object[] spans = mSpans;
        for (int i = mSpanCount - 1; i >= 0; i--) {
            if (spans[i] == span) {
                return mSpanData[i * COLUMNS + END];
            }
        }
        return -1;
    }

    @Override
    public int getSpanFlags(@NonNull Object span) {
        final Object[] spans = mSpans;
        for (int i = mSpanCount - 1; i >= 0; i--) {
            if (spans[i] == span) {
                return mSpanData[i * COLUMNS + FLAGS];
            }
        }
        return 0;
    }

    @Override
    public int nextSpanTransition(int start, int limit, @Nullable Class<?> type) {
        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        final boolean any = type == null || type == Object.class;

        for (int i = 0; i < count; i++) {
            final int st = data[i * COLUMNS + START];
            final int en = data[i * COLUMNS + END];

            if (st > start && st < limit && (any || type.isInstance(spans[i])))
                limit = st;
            if (en > start && en < limit && (any || type.isInstance(spans[i])))
                limit = en;
        }
        return limit;
    }

    private void sendSpanAdded(Object span, int start, int end) {
        final List<SpanWatcher> watchers = getSpans(start, end, SpanWatcher.class);
        for (SpanWatcher watcher : watchers) {
            watcher.onSpanAdded((Spannable) this, span, start, end);
        }
    }

    private void sendSpanRemoved(Object span, int start, int end) {
        final List<SpanWatcher> watchers = getSpans(start, end, SpanWatcher.class);
        for (SpanWatcher watcher : watchers) {
            watcher.onSpanRemoved((Spannable) this, span, start, end);
        }
    }

    private void sendSpanChanged(Object span, int s, int e, int st, int en) {
        final List<SpanWatcher> watchers = getSpans(Math.min(s, st), Math.max(e, en),
                SpanWatcher.class);
        for (SpanWatcher watcher : watchers) {
            watcher.onSpanChanged((Spannable) this, span, s, e, st, en);
        }
    }

    @NonNull
    @Override
    public final String toString() {
        return mText;
    }

    @Override
    public final int length() {
        return mText.length();
    }

    @Override
    public final char charAt(int index) {
        return mText.charAt(index);
    }

    @Override
    public final void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        mText.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    public boolean isEmpty() {
        return mText.isEmpty();
    }

    @NonNull
    public IntStream chars() {
        return mText.chars();
    }

    @NonNull
    public IntStream codePoints() {
        return mText.codePoints();
    }

    // Same as SpannableStringBuilder
    @Override
    public boolean equals(Object o) {
        if (o instanceof final Spanned other &&
                toString().equals(o.toString())) {
            // Check span data
            final List<?> otherSpans = other.getSpans(0, other.length(), Object.class);
            final List<?> spans = getSpans(0, length(), Object.class);
            if (otherSpans.isEmpty() && spans.isEmpty()) {
                return true;
            } else if (!otherSpans.isEmpty() && !spans.isEmpty() &&
                    otherSpans.size() == spans.size()) {
                // Do not check mSpanCount anymore for safety
                for (int i = 0; i < spans.size(); ++i) {
                    final Object span = spans.get(i);
                    final Object otherSpan = otherSpans.get(i);
                    if (span == this) {
                        if (other != otherSpan ||
                                getSpanStart(span) != other.getSpanStart(otherSpan) ||
                                getSpanEnd(span) != other.getSpanEnd(otherSpan) ||
                                getSpanFlags(span) != other.getSpanFlags(otherSpan)) {
                            return false;
                        }
                    } else if (!span.equals(otherSpan) ||
                            getSpanStart(span) != other.getSpanStart(otherSpan) ||
                            getSpanEnd(span) != other.getSpanEnd(otherSpan) ||
                            getSpanFlags(span) != other.getSpanFlags(otherSpan)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // Same as SpannableStringBuilder
    @Override
    public int hashCode() {
        int hash = toString().hashCode();
        hash = hash * 31 + mSpanCount;
        for (int i = 0; i < mSpanCount; ++i) {
            Object span = mSpans[i];
            if (span != this) {
                hash = hash * 31 + span.hashCode();
            }
            hash = hash * 31 + getSpanStart(span);
            hash = hash * 31 + getSpanEnd(span);
            hash = hash * 31 + getSpanFlags(span);
        }
        return hash;
    }
}
