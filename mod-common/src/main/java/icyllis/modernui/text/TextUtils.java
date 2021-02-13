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

public final class TextUtils {

    private static final Object sLock = new Object();
    private static char[] sTemp = null;

    @Nonnull
    static char[] obtain(int len) {
        char[] buf;

        synchronized (sLock) {
            buf = sTemp;
            sTemp = null;
        }

        if (buf == null || buf.length < len)
            buf = new char[len];

        return buf;
    }

    static void recycle(@Nonnull char[] temp) {
        if (temp.length > 1000)
            return;

        synchronized (sLock) {
            sTemp = temp;
        }
    }

    public static void getChars(@Nonnull CharSequence s, int start, int end,
                                char[] dest, int destoff) {
        final Class<? extends CharSequence> c = s.getClass();
        if (c == String.class)
            ((String) s).getChars(start, end, dest, destoff);
        else if (c == StringBuffer.class)
            ((StringBuffer) s).getChars(start, end, dest, destoff);
        else if (c == StringBuilder.class)
            ((StringBuilder) s).getChars(start, end, dest, destoff);
        else if (s instanceof GetChars)
            ((GetChars) s).getChars(start, end, dest, destoff);
        else
            for (int i = start; i < end; i++)
                dest[destoff++] = s.charAt(i);
    }

    /**
     * Removes empty spans from the <code>spans</code> array.
     * <p>
     * When parsing a Spanned using {@link Spanned#nextSpanTransition(int, int, Class)}, empty spans
     * will (correctly) create span transitions, and calling getSpans on a slice of text bounded by
     * one of these transitions will (correctly) include the empty overlapping span.
     * <p>
     * However, these empty spans should not be taken into account when layouting or rendering the
     * string and this method provides a way to filter getSpans' results accordingly.
     *
     * @param spans   A list of spans retrieved using {@link Spanned#getSpans(int, int, Class)} from
     *                the <code>spanned</code>
     * @param spanned The Spanned from which spans were extracted
     * @return A subset of spans where empty spans ({@link Spanned#getSpanStart(Object)}  ==
     * {@link Spanned#getSpanEnd(Object)} have been removed. The initial order is preserved
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] removeEmptySpans(@Nonnull T[] spans, Spanned spanned, Class<T> clazz) {
        T[] copy = null;
        int count = 0;

        for (int i = 0; i < spans.length; i++) {
            final T span = spans[i];
            final int start = spanned.getSpanStart(span);
            final int end = spanned.getSpanEnd(span);

            if (start == end) {
                if (copy == null) {
                    copy = (T[]) Array.newInstance(clazz, spans.length - 1);
                    System.arraycopy(spans, 0, copy, 0, i);
                    count = i;
                }
            } else {
                if (copy != null) {
                    copy[count] = span;
                    count++;
                }
            }
        }

        if (copy == null)
            return spans;
        T[] result = (T[]) Array.newInstance(clazz, count);
        System.arraycopy(copy, 0, result, 0, count);
        return result;
    }

    public static int indexOf(CharSequence s, char ch) {
        return indexOf(s, ch, 0);
    }

    public static int indexOf(CharSequence s, char ch, int start) {
        if (s instanceof String)
            return ((String) s).indexOf(ch, start);
        return indexOf(s, ch, start, s.length());
    }

    public static int indexOf(@Nonnull CharSequence s, char ch, int start, int end) {
        final Class<? extends CharSequence> c = s.getClass();

        if (s instanceof GetChars || c == StringBuffer.class ||
                c == StringBuilder.class || c == String.class) {
            char[] temp = obtain(500);

            while (start < end) {
                int segend = start + 500;
                if (segend > end)
                    segend = end;

                getChars(s, start, segend, temp, 0);

                int count = segend - start;
                for (int i = 0; i < count; i++)
                    if (temp[i] == ch) {
                        recycle(temp);
                        return i + start;
                    }

                start = segend;
            }

            recycle(temp);
            return -1;
        }

        for (int i = start; i < end; i++)
            if (s.charAt(i) == ch)
                return i;

        return -1;
    }
}
