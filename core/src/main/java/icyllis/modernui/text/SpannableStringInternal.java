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
import icyllis.modernui.graphics.text.GetChars;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.*;
import java.util.stream.IntStream;

// modified version of https://android.googlesource.com/
@SuppressWarnings("ForLoopReplaceableByForEach")
abstract sealed class SpannableStringInternal implements Spanned, GetChars
        permits SpannableString, SpannedString {

    private static final int START = 0;
    private static final int END = 1;
    private static final int FLAGS = 2;
    private static final int COLUMNS = 3;

    private final String mText;
    private Object[] mSpans;
    private int[] mSpanData;
    private int mSpanCount;

    // Modern UI added:
    // when span count is less than 16, linear search is faster and has a small footprint;
    // otherwise, use open-addressing hash map to handle a large number of spans
    private static final int HASHING_THRESHOLD = 16;
    @Nullable
    private Reference2IntOpenHashMap<Object> mIndexOfSpan;

    // Modern UI changed: optimize substring, never copy NoCopySpan,
    // never copy PARAGRAPH span if not at paragraph boundary
    SpannableStringInternal(@NonNull CharSequence source, int start, int end) {
        if (start == 0 && end == source.length())
            mText = source.toString();
        else
            mText = TextUtils.substring(source, start, end);

        mSpans = ObjectArrays.EMPTY_ARRAY;
        // Invariant: mSpanData.length = mSpans.length * COLUMNS
        mSpanData = IntArrays.EMPTY_ARRAY;

        if (source instanceof Spanned) {
            if (source instanceof SpannableStringInternal) {
                copySpansFromInternal((SpannableStringInternal) source, start, end);
            } else {
                copySpansFromSpanned((Spanned) source, start, end);
            }
        }
    }

    /**
     * Copies another {@link Spanned} object's spans between [start, end] into this object.
     *
     * @param src              Source object to copy from.
     * @param start            Start index in the source object.
     * @param end              End index in the source object.
     */
    private void copySpansFromSpanned(@NonNull Spanned src, int start, int end) {
        List<Object> spans = src.getSpans(start, end, Object.class);

        for (int i = 0; i < spans.size(); i++) {
            Object span = spans.get(i);
            if (span instanceof NoCopySpan) {
                continue;
            }
            int st = src.getSpanStart(span);
            int en = src.getSpanEnd(span);
            int fl = src.getSpanFlags(span);

            if (st < start)
                st = start;
            if (en > end)
                en = end;

            setSpan(false, span, st - start, en - start, fl, false/*enforceParagraph*/);
        }
    }

    /**
     * Copies a {@link SpannableStringInternal} object's spans between [start, end] into this
     * object.
     *
     * @param src              Source object to copy from.
     * @param start            Start index in the source object.
     * @param end              End index in the source object.
     */
    private void copySpansFromInternal(@NonNull SpannableStringInternal src, int start, int end) {
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
                continue;
            }
            count++;
        }

        if (count == 0) return;

        if (!hasNoCopySpan && start == 0 && end == src.length()) {
            // Modern UI: also trim the array to size
            assert count == src.mSpanCount;
            mSpanCount = src.mSpanCount;
            mSpans = new Object[mSpanCount];
            mSpanData = new int[mSpans.length * COLUMNS];
            System.arraycopy(src.mSpans, 0, mSpans, 0, mSpans.length);
            System.arraycopy(src.mSpanData, 0, mSpanData, 0, mSpanData.length);
            if (src.mIndexOfSpan != null) {
                mIndexOfSpan = new Reference2IntOpenHashMap<>(src.mIndexOfSpan);
            }
        } else {
            mSpans = new Object[count];
            mSpanData = new int[mSpans.length * COLUMNS];
            int j = 0;
            for (int i = 0; i < limit; i++) {
                int spanStart = srcData[i * COLUMNS + START];
                int spanEnd = srcData[i * COLUMNS + END];
                int spanFlags = srcData[i * COLUMNS + FLAGS];
                if (isOutOfCopyRange(start, end, spanStart, spanEnd)
                        || (srcSpans[i] instanceof NoCopySpan)) {
                    continue;
                }
                // Modern UI added: paragraph boundary check
                if ((spanFlags & Spannable.SPAN_PARAGRAPH) == Spannable.SPAN_PARAGRAPH) {
                    if (isIndexFollowsNextLine(spanStart) || isIndexFollowsNextLine(spanEnd)) {
                        continue;
                    }
                }
                if (spanStart < start)
                    spanStart = start;
                if (spanEnd > end)
                    spanEnd = end;

                mSpans[j] = srcSpans[i];
                mSpanData[j * COLUMNS + START] = spanStart - start;
                mSpanData[j * COLUMNS + END] = spanEnd - start;
                mSpanData[j * COLUMNS + FLAGS] = spanFlags;
                j++;
            }
            assert j <= count;
            mSpanCount = j;
            if (j > HASHING_THRESHOLD) {
                mIndexOfSpan = new Reference2IntOpenHashMap<>(j);
                mIndexOfSpan.defaultReturnValue(-1);
                for (int i = 0; i < j; i++) {
                    mIndexOfSpan.put(mSpans[i], i);
                }
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

    private boolean isIndexFollowsNextLine(int index) {
        return index != 0 && index != length() && charAt(index - 1) != '\n';
    }

    final void setSpan(boolean send, @NonNull Object span, int start, int end, int flags,
                       boolean enforceParagraph) {
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

        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        // send is false only in constructor, we assume there are no duplicate span objects,
        // so not only sendSpanChanged can be skipped, the whole block can be skipped
        if (send) {
            int i;
            if (mIndexOfSpan != null) {
                i = mIndexOfSpan.getInt(span);
            } else {
                for (i = count - 1; i >= 0; i--) {
                    if (spans[i] == span) {
                        break;
                    }
                }
            }
            if (i != -1) {
                int ic = i * COLUMNS;
                int ost = data[ic + START];
                int oen = data[ic + END];

                data[ic + START] = start;
                data[ic + END] = end;
                data[ic + FLAGS] = flags;

                sendSpanChanged(span, ost, oen, start, end);
                return;
            }
        }

        if (count == spans.length) {
            int newSize;
            if (count == 0) {
                newSize = 10;
            } else if (count < 1000) {
                // grow 50%
                newSize = count + (count >> 1);
            } else {
                // grow 25%
                newSize = count + (count >> 2);
            }
            Object[] newSpans = new Object[newSize];
            int[] newData = new int[newSize * COLUMNS];

            System.arraycopy(spans, 0, newSpans, 0, count);
            System.arraycopy(data, 0, newData, 0, count * COLUMNS);

            mSpans = newSpans;
            mSpanData = newData;
        }

        int ic = count * COLUMNS;
        mSpans[count] = span;
        mSpanData[ic + START] = start;
        mSpanData[ic + END] = end;
        mSpanData[ic + FLAGS] = flags;
        mSpanCount++;
        if (mIndexOfSpan != null) {
            mIndexOfSpan.put(span, count);
        } else if (mSpanCount > HASHING_THRESHOLD) {
            mIndexOfSpan = new Reference2IntOpenHashMap<>(mSpanCount);
            mIndexOfSpan.defaultReturnValue(-1);
            for (int i = 0; i < mSpanCount; i++) {
                mIndexOfSpan.put(mSpans[i], i);
            }
        }

        if (send) {
            sendSpanAdded(span, start, end);
        }
    }

    void removeSpan(@NonNull Object span, int flags) {
        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        // Modern UI changed:
        int i;
        if (mIndexOfSpan != null) {
            i = mIndexOfSpan.removeInt(span);
        } else {
            for (i = count - 1; i >= 0; i--) {
                if (spans[i] == span) {
                    break;
                }
            }
        }

        if (i != -1) {
            int ost = data[i * COLUMNS + START];
            int oen = data[i * COLUMNS + END];

            int c = count - (i + 1);

            if (c != 0) {
                System.arraycopy(spans, i + 1, spans, i, c);
                System.arraycopy(data, (i + 1) * COLUMNS,
                        data, i * COLUMNS, c * COLUMNS);
            }

            mSpanCount--;
            // Modern UI added: also null out reference to avoid leak
            spans[mSpanCount] = null;
            // Modern UI changed:
            if (mIndexOfSpan != null) {
                for (int j = i; j < mSpanCount; j++) {
                    mIndexOfSpan.put(spans[j], j);
                }
            }

            if ((flags & Spanned.SPAN_INTERMEDIATE) == 0) {
                sendSpanRemoved(span, ost, oen);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T> List<T> getSpans(int start, int end, Class<? extends T> type,
                                @Nullable List<T> dest) {
        if (dest != null) {
            dest.clear();
        }
        if (mSpanCount == 0) {
            return dest != null ? dest : Collections.emptyList();
        }

        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        final boolean check = type != null && type != Object.class;

        int found = 0;
        T first = null;

        for (int i = 0, ic = 0;
             i < count;
             i++, ic += COLUMNS) {
            int spanStart = data[ic + START];
            int spanEnd = data[ic + END];

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

            if (dest != null || found > 0) {
                if (dest == null) {
                    dest = new ArrayList<>();
                    dest.add(first);
                }

                final int priority = data[ic + FLAGS] & Spanned.SPAN_PRIORITY;
                if (priority != 0) {
                    int j = 0;
                    for (; j < found; j++) {
                        int p = getSpanFlags(dest.get(j)) & Spanned.SPAN_PRIORITY;
                        if (priority > p) {
                            break;
                        }
                    }
                    dest.add(j, (T) spans[i]);
                } else {
                    dest.add((T) spans[i]);
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

    @SuppressWarnings("unchecked")
    final <T> boolean getSpans(int start, int end, Class<? extends T> type,
                               boolean ignoreEmptySpans, @NonNull SpanSet<T> dest) {
        dest.clear();
        if (mSpanCount == 0) {
            return false;
        }

        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        final boolean check = type != null && type != Object.class;

        for (int i = 0, ic = 0;
             i < count;
             i++, ic += COLUMNS) {
            int spanStart = data[ic + START];
            int spanEnd = data[ic + END];

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

            final int flags = data[ic + FLAGS];
            final int priority = flags & Spanned.SPAN_PRIORITY;
            if (priority != 0) {
                int j = 0;
                for (; j < dest.size(); j++) {
                    int p = dest.spanFlags[j] & Spanned.SPAN_PRIORITY;
                    if (priority > p) {
                        break;
                    }
                }
                dest.add(j, (T) spans[i], spanStart, spanEnd, flags);
            } else {
                dest.add((T) spans[i], spanStart, spanEnd, flags);
            }
        }

        return !dest.isEmpty();
    }

    // Modern UI added:
    private int indexOfSpan(@NonNull Object span) {
        int i;
        if (mIndexOfSpan != null) {
            i = mIndexOfSpan.getInt(span);
        } else {
            final Object[] spans = mSpans;
            for (i = mSpanCount - 1; i >= 0; i--) {
                if (spans[i] == span) {
                    break;
                }
            }
        }
        return i;
    }

    @Override
    public int getSpanStart(@NonNull Object span) {
        int i = indexOfSpan(span);
        if (i != -1) {
            return mSpanData[i * COLUMNS + START];
        }
        return -1;
    }

    @Override
    public int getSpanEnd(@NonNull Object span) {
        int i = indexOfSpan(span);
        if (i != -1) {
            return mSpanData[i * COLUMNS + END];
        }
        return -1;
    }

    @Override
    public int getSpanFlags(@NonNull Object span) {
        int i = indexOfSpan(span);
        if (i != -1) {
            return mSpanData[i * COLUMNS + FLAGS];
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
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onSpanAdded((Spannable) this, span, start, end);
        }
    }

    private void sendSpanRemoved(Object span, int start, int end) {
        final List<SpanWatcher> watchers = getSpans(start, end, SpanWatcher.class);
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onSpanRemoved((Spannable) this, span, start, end);
        }
    }

    private void sendSpanChanged(Object span, int s, int e, int st, int en) {
        final List<SpanWatcher> watchers = getSpans(Math.min(s, st), Math.max(e, en),
                SpanWatcher.class);
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).onSpanChanged((Spannable) this, span, s, e, st, en);
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
