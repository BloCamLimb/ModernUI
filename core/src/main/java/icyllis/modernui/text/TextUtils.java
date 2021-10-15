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

import com.ibm.icu.util.ULocale;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Locale;

public final class TextUtils {

    private static final Object sLock = new Object();
    private static char[] sTemp = null;

    // Zero-width character used to fill ellipsized strings when codepoint length must be preserved.
    static final char ELLIPSIS_FILLER = '\uFEFF'; // ZERO WIDTH NO-BREAK SPACE

    //TODO: Based on CLDR data, these need to be localized for Dzongkha (dz) and perhaps
    // Hong Kong Traditional Chinese (zh-Hant-HK), but that may need to depend on the actual word
    // being ellipsized and not the locale.
    private static final String ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)

    @Nonnull
    public static String getEllipsisString(@Nonnull TextUtils.TruncateAt method) {
        return ELLIPSIS_NORMAL;
    }

    /**
     * Obtain a temporary char buffer.
     *
     * @param len the length of the buffer
     * @return a char buffer
     * @see #recycle(char[]) recycle the buffer
     */
    @Nonnull
    public static char[] obtain(int len) {
        char[] buf;

        synchronized (sLock) {
            buf = sTemp;
            sTemp = null;
        }

        if (buf == null || buf.length < len)
            buf = new char[len];

        return buf;
    }

    public static void recycle(@Nonnull char[] temp) {
        if (temp.length > 1000)
            return;

        synchronized (sLock) {
            sTemp = temp;
        }
    }

    public static void getChars(@Nonnull CharSequence s, int srcBegin, int srcEnd,
                                @Nonnull char[] dst, int dstBegin) {
        final Class<? extends CharSequence> c = s.getClass();
        if (c == String.class)
            ((String) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (c == StringBuffer.class)
            ((StringBuffer) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (c == StringBuilder.class)
            ((StringBuilder) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof GetChars)
            ((GetChars) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else {
            for (int i = srcBegin; i < srcEnd; i++)
                dst[dstBegin++] = s.charAt(i);
        }
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
    @Nonnull
    public static <T> T[] removeEmptySpans(@Nonnull T[] spans, @Nonnull Spanned spanned, @Nonnull Class<T> clazz) {
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

        if (copy == null) {
            return spans;
        }
        if (count == copy.length) {
            return copy;
        }
        T[] result = (T[]) Array.newInstance(clazz, count);
        System.arraycopy(copy, 0, result, 0, count);
        return result;
    }

    public static int indexOf(CharSequence s, char ch) {
        return indexOf(s, ch, 0);
    }

    public static int indexOf(CharSequence s, char ch, int start) {
        if (s instanceof String) {
            return ((String) s).indexOf(ch, start);
        }
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
                for (int i = 0; i < count; i++) {
                    if (temp[i] == ch) {
                        recycle(temp);
                        return i + start;
                    }
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

    public static int lastIndexOf(CharSequence s, char ch) {
        return lastIndexOf(s, ch, s.length() - 1);
    }

    public static int lastIndexOf(CharSequence s, char ch, int last) {
        Class<? extends CharSequence> c = s.getClass();

        if (c == String.class)
            return ((String) s).lastIndexOf(ch, last);

        return lastIndexOf(s, ch, 0, last);
    }

    public static int lastIndexOf(CharSequence s, char ch,
                                  int start, int last) {
        if (last < 0)
            return -1;
        if (last >= s.length())
            last = s.length() - 1;

        int end = last + 1;

        Class<? extends CharSequence> c = s.getClass();

        if (s instanceof GetChars || c == StringBuffer.class ||
                c == StringBuilder.class || c == String.class) {
            char[] temp = obtain(500);

            while (start < end) {
                int segstart = end - 500;
                if (segstart < start)
                    segstart = start;

                getChars(s, segstart, end, temp, 0);

                int count = end - segstart;
                for (int i = count - 1; i >= 0; i--) {
                    if (temp[i] == ch) {
                        recycle(temp);
                        return i + segstart;
                    }
                }

                end = segstart;
            }

            recycle(temp);
            return -1;
        }

        for (int i = end - 1; i >= start; i--)
            if (s.charAt(i) == ch)
                return i;

        return -1;
    }

    /**
     * Where to truncate.
     */
    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE // not supported
    }

    @FunctionalInterface
    public interface EllipsizeCallback {

        /**
         * This method is called to report that the specified region of
         * text was ellipsized away by a call to {@link #ellipsize}.
         */
        void ellipsized(int start, int end);
    }

    /**
     * Returns the original text if it fits in the specified width
     * given the properties of the specified Paint,
     * or, if it does not fit, a truncated
     * copy with ellipsis character added at the specified edge or center.
     */
    @Nonnull
    public static CharSequence ellipsize(@Nonnull CharSequence text, @Nonnull FontPaint p,
                                         float avail, @Nonnull TruncateAt where) {
        return ellipsize(text, p, avail, where, false, null);
    }

    /**
     * Returns the original text if it fits in the specified width
     * given the properties of the specified Paint,
     * or, if it does not fit, a copy with ellipsis character added
     * at the specified edge or center.
     * If <code>preserveLength</code> is specified, the returned copy
     * will be padded with zero-width spaces to preserve the original
     * length and offsets instead of truncating.
     * If <code>callback</code> is non-null, it will be called to
     * report the start and end of the ellipsized range.  TextDirection
     * is determined by the first strong directional character.
     */
    @Nonnull
    public static CharSequence ellipsize(@Nonnull CharSequence text, @Nonnull FontPaint paint,
                                         float avail, @Nonnull TruncateAt where,
                                         boolean preserveLength, @Nullable EllipsizeCallback callback) {
        return ellipsize(text, paint, avail, where, preserveLength, callback,
                TextDirectionHeuristics.FIRSTSTRONG_LTR, getEllipsisString(where));
    }

    /**
     * Returns the original text if it fits in the specified width
     * given the properties of the specified Paint,
     * or, if it does not fit, a copy with ellipsis character added
     * at the specified edge or center.
     * If <code>preserveLength</code> is specified, the returned copy
     * will be padded with zero-width spaces to preserve the original
     * length and offsets instead of truncating.
     * If <code>callback</code> is non-null, it will be called to
     * report the start and end of the ellipsized range.
     *
     * @hide
     */
    @Nonnull
    private static CharSequence ellipsize(@Nonnull CharSequence text, @Nonnull FontPaint paint,
                                          float avail, @Nonnull TruncateAt where, boolean preserveLength,
                                          @Nullable EllipsizeCallback callback,
                                          @Nonnull TextDirectionHeuristic textDir, @Nonnull String ellipsis) {

        final int len = text.length();

        final float ellipsisWidth;

        MeasuredParagraph mt = null;
        try {
            mt = MeasuredParagraph.buildForStaticLayout(paint, ellipsis, 0, ellipsis.length(), textDir, false, null);
            ellipsisWidth = mt.getAdvance(0, ellipsis.length());
        } finally {
            if (mt != null) {
                mt.recycle();
                mt = null;
            }
        }

        try {
            mt = MeasuredParagraph.buildForStaticLayout(paint, text, 0, text.length(), textDir, false, null);
            float width = mt.getAdvance(0, text.length());

            if (width <= avail) {
                if (callback != null) {
                    callback.ellipsized(0, 0);
                }

                return text;
            }

            avail -= ellipsisWidth;

            int left = 0;
            int right = len;
            if (avail >= 0) {
                if (where == TruncateAt.START) {
                    right = len - mt.breakText(len, false, avail);
                } else if (where == TruncateAt.END) {
                    left = mt.breakText(len, true, avail);
                } else { // MIDDLE
                    right = len - mt.breakText(len, false, avail / 2);
                    avail -= mt.getAdvance(right, len);
                    left = mt.breakText(right, true, avail);
                }
            }

            if (callback != null) {
                callback.ellipsized(left, right);
            }

            final char[] buf = mt.getChars();
            Spanned sp = text instanceof Spanned ? (Spanned) text : null;

            final int removed = right - left;
            final int remaining = len - removed;
            if (preserveLength) {
                if (remaining > 0 && removed >= ellipsis.length()) {
                    ellipsis.getChars(0, ellipsis.length(), buf, left);
                    left += ellipsis.length();
                } // else skip the ellipsis
                for (int i = left; i < right; i++) {
                    buf[i] = ELLIPSIS_FILLER;
                }
                String s = new String(buf, 0, len);
                if (sp == null) {
                    return s;
                }
                SpannableString ss = new SpannableString(s);
                copySpansFrom(sp, 0, len, Object.class, ss, 0);
                return ss;
            }

            if (remaining == 0) {
                return "";
            }

            if (sp == null) {
                return String.valueOf(buf, 0, left) +
                        ellipsis +
                        String.valueOf(buf, right, len - right);
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(text, 0, left);
            ssb.append(ellipsis);
            ssb.append(text, right, len);
            return ssb;
        } finally {
            if (mt != null) {
                mt.recycle();
            }
        }
    }

    private static final String[] sBinaryCompacts = new String[]{" bytes", " KB", " MB", " GB"};

    @Nonnull
    public static String binaryCompact(int num) {
        if (num == 0)
            return "0 bytes";
        int i = (Integer.SIZE - 1 - Integer.numberOfLeadingZeros(num)) / 10;
        return num / (1 << (i * 10)) + sBinaryCompacts[i];
    }

    /**
     * Copies the spans from the region <code>start...end</code> in
     * <code>source</code> to the region
     * <code>destoff...destoff+end-start</code> in <code>dest</code>.
     * Spans in <code>source</code> that begin before <code>start</code>
     * or end after <code>end</code> but overlap this range are trimmed
     * as if they began at <code>start</code> or ended at <code>end</code>.
     *
     * @throws IndexOutOfBoundsException if any of the copied spans
     *                                   are out of range in <code>dest</code>.
     */
    public static void copySpansFrom(@Nonnull Spanned source, int start, int end,
                                     @Nullable Class<?> kind,
                                     @Nonnull Spannable dest, int destoff) {
        if (kind == null) {
            kind = Object.class;
        }

        Object[] spans = source.getSpans(start, end, kind);

        if (spans != null) {
            for (Object span : spans) {
                int st = source.getSpanStart(span);
                int en = source.getSpanEnd(span);
                int fl = source.getSpanFlags(span);

                if (st < start)
                    st = start;
                if (en > end)
                    en = end;

                dest.setSpan(span, st - start + destoff, en - start + destoff,
                        fl);
            }
        }
    }

    // Returns true if the character's presence could affect RTL layout.
    //
    // In order to be fast, the code is intentionally rough and quite conservative in its
    // considering inclusion of any non-BMP or surrogate characters or anything in the bidi
    // blocks or any bidi formatting characters with a potential to affect RTL layout.
    static boolean couldAffectRtl(char c) {
        return (0x0590 <= c && c <= 0x08FF) ||  // RTL scripts
                c == 0x200E ||  // Bidi format character
                c == 0x200F ||  // Bidi format character
                (0x202A <= c && c <= 0x202E) ||  // Bidi format characters
                (0x2066 <= c && c <= 0x2069) ||  // Bidi format characters
                (0xD800 <= c && c <= 0xDFFF) ||  // Surrogate pairs
                (0xFB1D <= c && c <= 0xFDFF) ||  // Hebrew and Arabic presentation forms
                (0xFE70 <= c && c <= 0xFEFE);  // Arabic presentation forms
    }

    /**
     * Return the layout direction for a given Locale
     *
     * @param locale the Locale for which we want the layout direction. Can be null.
     * @return the layout direction. This may be one of:
     * {@link View#LAYOUT_DIRECTION_LTR} or
     * {@link View#LAYOUT_DIRECTION_RTL}.
     *
     * Be careful: this code will need to be updated when vertical scripts will be supported
     */
    public static int getLayoutDirectionFromLocale(@Nullable Locale locale) {
        return (locale != null && !locale.equals(Locale.ROOT)
                && ULocale.forLocale(locale).isRightToLeft())
                ? View.LAYOUT_DIRECTION_RTL
                : View.LAYOUT_DIRECTION_LTR;
    }
}
