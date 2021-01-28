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

import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrays;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;

abstract class SpannableStringInternal {

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
        mSpanData = IntArrays.EMPTY_ARRAY;

        if (source instanceof Spanned) {
            if (source instanceof SpannableStringInternal) {
                copySpansFromInternal((SpannableStringInternal) source, start, end, ignoreNoCopySpan);
            } else {
                copySpansFromSpanned((Spanned) source, start, end, ignoreNoCopySpan);
            }
        }
    }

    private void copySpansFromSpanned(@Nonnull Spanned src, int start, int end, boolean ignoreNoCopySpan) {
        Object[] spans = src.getSpans(start, end, Object.class);

        for (Object span : spans) {
            if (ignoreNoCopySpan && span instanceof NoCopySpan)
                continue;

            int st = src.getSpanStart(span);
            int en = src.getSpanEnd(span);
            int fl = src.getSpanFlags(span);

            if (st < start)
                st = start;
            if (en > end)
                en = end;

            setSpan(span, st - start, en - start, fl, false);
        }
    }

    private void copySpansFromInternal(@Nonnull SpannableStringInternal src, int start, int end, boolean ignoreNoCopySpan) {
        int count = 0;
        final int[] srcData = src.mSpanData;
        final Object[] srcSpans = src.mSpans;
        final int limit = src.mSpanCount;
        boolean hasNoCopySpan = false;

        for (int i = 0; i < limit; i++) {
            int spanStart = srcData[i * COLUMNS + START];
            int spanEnd = srcData[i * COLUMNS + END];
            if (spanStart > end || spanEnd < start)
                continue;
            if (spanStart != spanEnd && start != end)
                if (spanStart == end || spanEnd == start)
                    continue;
            if (srcSpans[i] instanceof NoCopySpan) {
                hasNoCopySpan = true;
                if (ignoreNoCopySpan)
                    continue;
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

                if (spanStart > end || spanEnd < start)
                    continue;

                if (spanStart != spanEnd && start != end)
                    if (spanStart == end || spanEnd == start)
                        continue;

                if (ignoreNoCopySpan && srcSpans[i] instanceof NoCopySpan)
                    continue;

                if (spanStart < start)
                    spanStart = start;
                if (spanEnd > end)
                    spanEnd = end;

                mSpans[j] = srcSpans[i];
                mSpanData[j * COLUMNS + START] = spanStart - start;
                mSpanData[j * COLUMNS + END] = spanEnd - start;
                mSpanData[j * COLUMNS + FLAGS] = srcData[i * COLUMNS + FLAGS];
                j++;
            }
        }
    }

    void setSpan(Object span, int start, int end, int flags) {
        setSpan(span, start, end, flags, true);
    }

    private boolean isIndexFollowsNextLine(int index) {
        return index != 0 && index != length() && charAt(index - 1) != '\n';
    }

    private void setSpan(Object span, final int start, final int end, int flags, boolean enforceParagraph) {
        Arrays.ensureFromTo(length(), start, end);

        if ((flags & Spannable.SPAN_PARAGRAPH) == Spannable.SPAN_PARAGRAPH) {
            if (isIndexFollowsNextLine(start)) {
                if (!enforceParagraph)
                    return;
                throw new RuntimeException("PARAGRAPH span must start at paragraph boundary"
                        + " (" + start + " follows " + charAt(start - 1) + ")");
            }

            if (isIndexFollowsNextLine(end)) {
                if (!enforceParagraph)
                    return;
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

        if (mSpanCount + 1 >= mSpans.length) {
            Object[] newSpans = new Object[mSpanCount + (mSpanCount >> 1)];
            int[] newData = new int[newSpans.length * COLUMNS];

            System.arraycopy(mSpans, 0, newSpans, 0, mSpanCount);
            System.arraycopy(mSpanData, 0, newData, 0, mSpanCount * COLUMNS);

            mSpans = newSpans;
            mSpanData = newData;
        }

        mSpans[mSpanCount] = span;
        mSpanData[mSpanCount * COLUMNS + START] = start;
        mSpanData[mSpanCount * COLUMNS + END] = end;
        mSpanData[mSpanCount * COLUMNS + FLAGS] = flags;
        mSpanCount++;

        if (this instanceof Spannable)
            sendSpanAdded(span, start, end);
    }

    void removeSpan(Object span) {
        removeSpan(span, 0);
    }

    public void removeSpan(Object span, int flags) {
        int count = mSpanCount;
        Object[] spans = mSpans;
        int[] data = mSpanData;

        for (int i = count - 1; i >= 0; i--) {
            if (spans[i] == span) {
                int ost = data[i * COLUMNS + START];
                int oen = data[i * COLUMNS + END];

                int c = count - (i + 1);

                System.arraycopy(spans, i + 1, spans, i, c);
                System.arraycopy(data, (i + 1) * COLUMNS,
                        data, i * COLUMNS, c * COLUMNS);

                mSpanCount--;

                if ((flags & Spanned.SPAN_INTERMEDIATE) == 0) {
                    sendSpanRemoved(span, ost, oen);
                }
                return;
            }
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public <T> T[] getSpans(int start, int end, @Nonnull Class<T> type) {
        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        int found = 0;
        Object[] tem = null;
        Object first = null;

        for (int i = 0; i < count; i++) {
            int spanStart = data[i * COLUMNS + START];
            int spanEnd = data[i * COLUMNS + END];

            if (spanStart > end || spanEnd < start)
                continue;

            if (spanStart != spanEnd && start != end)
                if (spanStart == end || spanEnd == start)
                    continue;

            if (type != Object.class && !type.isInstance(spans[i]))
                continue;

            if (found == 0) {
                first = spans[i];
                found++;
            } else {
                if (found == 1) {
                    tem = (Object[]) Array.newInstance(type, count - i + 1);
                    tem[0] = first;
                }
                final int priority = data[i * COLUMNS + FLAGS] & Spanned.SPAN_PRIORITY;
                if (priority != 0) {
                    int j = 0;
                    for (; j < found; j++)
                        if (priority > (getSpanFlags(tem[j]) & Spanned.SPAN_PRIORITY))
                            break;

                    System.arraycopy(tem, j, tem, j + 1, found - j);
                    tem[j] = spans[i];
                    found++;
                } else
                    tem[found++] = spans[i];
            }
        }

        if (found == 0)
            return (T[]) (type == Object.class ? ObjectArrays.EMPTY_ARRAY : Array.newInstance(type, 0));
        if (found == 1) {
            tem = (Object[]) Array.newInstance(type, 1);
            tem[0] = first;
            return (T[]) tem;
        }
        if (found == tem.length)
            return (T[]) tem;

        Object[] r = (Object[]) Array.newInstance(type, found);
        System.arraycopy(tem, 0, r, 0, found);
        return (T[]) r;
    }

    public int getSpanStart(Object span) {
        final Object[] spans = mSpans;
        for (int i = mSpanCount - 1; i >= 0; i--)
            if (spans[i] == span)
                return mSpanData[i * COLUMNS + START];
        return -1;
    }

    public int getSpanEnd(Object span) {
        final Object[] spans = mSpans;
        for (int i = mSpanCount - 1; i >= 0; i--)
            if (spans[i] == span)
                return mSpanData[i * COLUMNS + END];
        return -1;
    }

    public int getSpanFlags(Object span) {
        final Object[] spans = mSpans;
        for (int i = mSpanCount - 1; i >= 0; i--)
            if (spans[i] == span)
                return mSpanData[i * COLUMNS + FLAGS];
        return 0;
    }

    public int nextSpanTransition(int start, int limit, @Nonnull Class<?> type) {
        final int count = mSpanCount;
        final Object[] spans = mSpans;
        final int[] data = mSpanData;

        for (int i = 0; i < count; i++) {
            int st = data[i * COLUMNS + START];
            int en = data[i * COLUMNS + END];

            if (st > start && st < limit && (type == Object.class || type.isInstance(spans[i])))
                limit = st;
            if (en > start && en < limit && (type == Object.class || type.isInstance(spans[i])))
                limit = en;
        }

        return limit;
    }

    private void sendSpanAdded(Object span, int start, int end) {
        final SpanWatcher[] watchers = getSpans(start, end, SpanWatcher.class);
        for (SpanWatcher spanWatcher : watchers)
            spanWatcher.onSpanAdded((Spannable) this, span, start, end);
    }

    private void sendSpanRemoved(Object span, int start, int end) {
        final SpanWatcher[] watchers = getSpans(start, end, SpanWatcher.class);
        for (SpanWatcher spanWatcher : watchers)
            spanWatcher.onSpanRemoved((Spannable) this, span, start, end);
    }

    private void sendSpanChanged(Object span, int s, int e, int st, int en) {
        final SpanWatcher[] watchers = getSpans(Math.min(s, st), Math.max(e, en),
                SpanWatcher.class);
        for (SpanWatcher spanWatcher : watchers)
            spanWatcher.onSpanChanged((Spannable) this, span, s, e, st, en);
    }

    @Nonnull
    @Override
    public final String toString() {
        return mText;
    }

    public final int length() {
        return mText.length();
    }

    public final char charAt(int index) {
        return mText.charAt(index);
    }

    public final void getChars(int start, int end, char[] dest, int off) {
        mText.getChars(start, end, dest, off);
    }

    // Same as SpannableStringBuilder
    @Override
    public boolean equals(Object o) {
        if (o instanceof Spanned &&
                toString().equals(o.toString())) {
            final Spanned other = (Spanned) o;
            // Check span data
            final Object[] otherSpans = other.getSpans(0, other.length(), Object.class);
            final Object[] thisSpans = getSpans(0, length(), Object.class);
            if (mSpanCount == otherSpans.length) {
                for (int i = 0; i < mSpanCount; ++i) {
                    final Object thisSpan = thisSpans[i];
                    final Object otherSpan = otherSpans[i];
                    if (thisSpan == this) {
                        if (other != otherSpan ||
                                getSpanStart(thisSpan) != other.getSpanStart(otherSpan) ||
                                getSpanEnd(thisSpan) != other.getSpanEnd(otherSpan) ||
                                getSpanFlags(thisSpan) != other.getSpanFlags(otherSpan)) {
                            return false;
                        }
                    } else if (!thisSpan.equals(otherSpan) ||
                            getSpanStart(thisSpan) != other.getSpanStart(otherSpan) ||
                            getSpanEnd(thisSpan) != other.getSpanEnd(otherSpan) ||
                            getSpanFlags(thisSpan) != other.getSpanFlags(otherSpan)) {
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
