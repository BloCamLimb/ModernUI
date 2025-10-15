/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.text.CharUtils;
import icyllis.modernui.graphics.text.GetChars;
import icyllis.modernui.graphics.text.LayoutCache;
import icyllis.modernui.text.style.*;
import icyllis.modernui.util.Parcel;
import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ForLoopReplaceableByForEach")
public final class TextUtils {

    // Zero-width character used to fill ellipsized strings when codepoint length must be preserved.
    static final char ELLIPSIS_FILLER = '\uFEFF'; // ZERO WIDTH NO-BREAK SPACE

    //TODO: Based on CLDR data, these need to be localized for Dzongkha (dz) and perhaps
    // Hong Kong Traditional Chinese (zh-Hant-HK), but that may need to depend on the actual word
    // being ellipsized and not the locale.
    private static final String ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)

    private static final char[] ELLIPSIS_NORMAL_ARRAY = ELLIPSIS_NORMAL.toCharArray();

    private TextUtils() {
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @NonNull
    public static char[] getEllipsisChars(@NonNull TextUtils.TruncateAt method) {
        return ELLIPSIS_NORMAL_ARRAY;
    }

    public static CharSequence stringOrSpannedString(CharSequence source) {
        if (source == null)
            return null;
        if (source instanceof SpannedString)
            return source;
        if (source instanceof Spanned)
            return new SpannedString(source);

        return source.toString();
    }

    /**
     * Returns true if the char sequence is null or 0-length.
     *
     * @param csq the char sequence to be examined
     * @return true if csq is null or zero length
     */
    public static boolean isEmpty(@Nullable CharSequence csq) {
        return csq == null || csq.isEmpty();
    }

    public static boolean contentEquals(@Nullable CharSequence a, @Nullable CharSequence b) {
        if (a == b) return true;
        int length;
        if (a != null && b != null) {
            if (a instanceof String) {
                return ((String) a).contentEquals(b);
            } else if ((length = a.length()) == b.length()) {
                for (int i = 0; i < length; i++) {
                    if (a.charAt(i) != b.charAt(i)) return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Copies a block of characters efficiently.
     *
     * @throws IndexOutOfBoundsException if out of range
     */
    public static void getChars(@NonNull CharSequence s, int srcBegin, int srcEnd,
                                @NonNull char[] dst, int dstBegin) {
        CharUtils.getChars(s, srcBegin, srcEnd, dst, dstBegin);
    }

    /**
     * Removes empty spans from the <code>spans</code> list.
     * <p>
     * When parsing a Spanned using {@link Spanned#nextSpanTransition(int, int, Class)}, empty spans
     * will (correctly) create span transitions, and calling getSpans on a slice of text bounded by
     * one of these transitions will (correctly) include the empty overlapping span.
     * <p>
     * However, these empty spans should not be taken into account when laying-out or rendering the
     * string and this method provides a way to filter getSpans' results accordingly.
     *
     * @param spans   A list of spans retrieved using {@link Spanned#getSpans(int, int, Class)} from
     *                the <code>spanned</code>
     * @param spanned The Spanned from which spans were extracted
     * @return A subset of spans where empty spans ({@link Spanned#getSpanStart(Object)}  ==
     * {@link Spanned#getSpanEnd(Object)} have been removed. The initial order is preserved
     */
    //TODO Consider removing this inefficient method and using SpanSet instead.
    @ApiStatus.Internal
    @NonNull
    public static <T> List<T> removeEmptySpans(@NonNull List<T> spans, @NonNull Spanned spanned) {
        List<T> copy = null;

        for (int i = 0; i < spans.size(); i++) {
            final T span = spans.get(i);
            final int start = spanned.getSpanStart(span);
            final int end = spanned.getSpanEnd(span);

            if (start == end) {
                if (copy == null) {
                    copy = new ArrayList<>(i);
                    for (int j = 0; j < i; j++) {
                        copy.add(spans.get(j));
                    }
                }
            } else {
                if (copy != null) {
                    copy.add(span);
                }
            }
        }

        if (copy == null) {
            return spans;
        }
        return copy;
    }

    /**
     * Create a new String object containing the given range of characters
     * from the source string.  This is different than simply calling
     * {@link CharSequence#subSequence(int, int) CharSequence.subSequence}
     * in that it does not preserve any style runs in the source sequence,
     * allowing a more efficient implementation.
     *
     * @throws IndexOutOfBoundsException if out of range
     */
    @NonNull
    public static String substring(@NonNull CharSequence source, int start, int end) {
        if (source instanceof String)
            return ((String) source).substring(start, end);
        if (source instanceof SpannableStringInternal || source instanceof PrecomputedText)
            return source.toString().substring(start, end);
        if (source instanceof SpannableStringBuilder)
            return ((SpannableStringBuilder) source).substring(start, end);
        if (source instanceof StringBuilder)
            return ((StringBuilder) source).substring(start, end);
        if (source instanceof StringBuffer)
            return ((StringBuffer) source).substring(start, end);
        if (source instanceof CharBuffer)
            return ((CharBuffer) source).slice(start, end - start).toString(); // Java 13

        char[] temp = CharUtils.obtain(end - start);
        CharUtils.getChars(source, start, end, temp, 0);
        String ret = new String(temp, 0, end - start);
        CharUtils.recycle(temp);

        return ret;
    }

    public static int indexOf(@NonNull CharSequence s, char ch) {
        return indexOf(s, ch, 0);
    }

    public static int indexOf(@NonNull CharSequence s, char ch, int start) {
        return indexOf(s, ch, start, s.length());
    }

    public static int indexOf(@NonNull CharSequence s, char ch, int start, int end) {
        int len = s.length();
        if (end == len && (s instanceof String || s instanceof SpannableStringInternal)) {
            return s.toString().indexOf(ch, start);
        }
        if (start >= len)
            return -1;
        if (start < 0)
            start = 0;
        final Class<? extends CharSequence> c = s.getClass();

        if (s instanceof GetChars || c == StringBuffer.class ||
                c == StringBuilder.class || c == String.class ||
                s instanceof CharBuffer) {
            char[] temp = CharUtils.obtain(500);

            while (start < end) {
                int segend = start + 500;
                if (segend > end)
                    segend = end;

                CharUtils.getChars(s, start, segend, temp, 0);

                int count = segend - start;
                for (int i = 0; i < count; i++) {
                    if (temp[i] == ch) {
                        CharUtils.recycle(temp);
                        return i + start;
                    }
                }

                start = segend;
            }

            CharUtils.recycle(temp);
            return -1;
        }

        for (int i = start; i < end; i++)
            if (s.charAt(i) == ch)
                return i;

        return -1;
    }

    public static int lastIndexOf(@NonNull CharSequence s, char ch) {
        return lastIndexOf(s, ch, s.length() - 1);
    }

    public static int lastIndexOf(@NonNull CharSequence s, char ch, int last) {
        return lastIndexOf(s, ch, 0, last);
    }

    public static int lastIndexOf(@NonNull CharSequence s, char ch,
                                  int start, int last) {
        if (start == 0 && (s instanceof String || s instanceof SpannableStringInternal)) {
            return s.toString().lastIndexOf(ch, last);
        }
        if (last < 0)
            return -1;
        int len = s.length();
        int end = last + 1;
        if (end > len)
            end = len;

        Class<? extends CharSequence> c = s.getClass();

        if (s instanceof GetChars || c == StringBuffer.class ||
                c == StringBuilder.class || c == String.class ||
                s instanceof CharBuffer) {
            char[] temp = CharUtils.obtain(500);

            while (start < end) {
                int segstart = end - 500;
                if (segstart < start)
                    segstart = start;

                CharUtils.getChars(s, segstart, end, temp, 0);

                int count = end - segstart;
                for (int i = count - 1; i >= 0; i--) {
                    if (temp[i] == ch) {
                        CharUtils.recycle(temp);
                        return i + segstart;
                    }
                }

                end = segstart;
            }

            CharUtils.recycle(temp);
            return -1;
        }

        for (int i = end - 1; i >= start; i--)
            if (s.charAt(i) == ch)
                return i;

        return -1;
    }

    //@formatter:off
    @ApiStatus.Internal
    public static final int
            FIRST_SPAN                     = 1,
            ALIGNMENT_SPAN                 = FIRST_SPAN,
            FOREGROUND_COLOR_SPAN          = FIRST_SPAN + 1,
            RELATIVE_SIZE_SPAN             = FIRST_SPAN + 2,
            SCALE_X_SPAN                   = FIRST_SPAN + 3,
            STRIKETHROUGH_SPAN             = FIRST_SPAN + 4,
            UNDERLINE_SPAN                 = FIRST_SPAN + 5,
            STYLE_SPAN                     = FIRST_SPAN + 6,
            BULLET_SPAN                    = FIRST_SPAN + 7,
            QUOTE_SPAN                     = FIRST_SPAN + 8,
            LEADING_MARGIN_SPAN            = FIRST_SPAN + 9,
            URL_SPAN                       = FIRST_SPAN + 10,
            BACKGROUND_COLOR_SPAN          = FIRST_SPAN + 11,
            TYPEFACE_SPAN                  = FIRST_SPAN + 12,
            SUPERSCRIPT_SPAN               = FIRST_SPAN + 13,
            SUBSCRIPT_SPAN                 = FIRST_SPAN + 14,
            ABSOLUTE_SIZE_SPAN             = FIRST_SPAN + 15,
            TEXT_APPEARANCE_SPAN           = FIRST_SPAN + 16,
            ANNOTATION                     = FIRST_SPAN + 17,
            SUGGESTION_SPAN                = FIRST_SPAN + 18,
            SPELL_CHECK_SPAN               = FIRST_SPAN + 19,
            SUGGESTION_RANGE_SPAN          = FIRST_SPAN + 20,
            EASY_EDIT_SPAN                 = FIRST_SPAN + 21,
            LOCALE_SPAN                    = FIRST_SPAN + 22,
            TTS_SPAN                       = FIRST_SPAN + 23,
            ACCESSIBILITY_CLICKABLE_SPAN   = FIRST_SPAN + 24,
            ACCESSIBILITY_URL_SPAN         = FIRST_SPAN + 25,
            LINE_BACKGROUND_SPAN           = FIRST_SPAN + 26,
            LINE_HEIGHT_SPAN               = FIRST_SPAN + 27,
            ACCESSIBILITY_REPLACEMENT_SPAN = FIRST_SPAN + 28,
            LAST_SPAN                      = ACCESSIBILITY_REPLACEMENT_SPAN;
    //@formatter:on

    /**
     * Flatten a {@link CharSequence} and whatever styles can be copied across processes
     * into the output.
     */
    public static void writeToParcel(@Nullable CharSequence cs,
                                     @NonNull Parcel dest, int flags) {
        if (cs == null) {
            dest.writeInt(0);
        } else if (cs instanceof Spanned sp) {
            dest.writeInt(2);
            dest.writeString(cs.toString());

            final List<Object> os = sp.getSpans(0, cs.length(), Object.class);
            for (int i = 0; i < os.size(); i++) {
                final Object o = os.get(i);
                if (o instanceof ParcelableSpan span) {
                    final int id = span.getSpanTypeId();
                    if (id < FIRST_SPAN || id > LAST_SPAN) {
                        throw new AssertionError(id);
                    } else {
                        dest.writeInt(id);
                        span.writeToParcel(dest, flags);
                        dest.writeInt(sp.getSpanStart(o));
                        dest.writeInt(sp.getSpanEnd(o));
                        dest.writeInt(sp.getSpanFlags(o));
                    }
                }
            }
            dest.writeInt(0);
        } else {
            dest.writeInt(1);
            dest.writeString(cs.toString());
        }
    }

    @Nullable
    public static CharSequence createFromParcel(@NonNull Parcel p) {
        int type = p.readInt();
        if (type == 0)
            return null;
        final String s = p.readString();
        if (type == 1)
            return s;
        assert type == 2 && s != null;
        final var sp = new SpannableString(s);
        while ((type = p.readInt()) != 0) {
            switch (type) {
                case ALIGNMENT_SPAN -> readSpan(p, sp, new AlignmentSpan.Standard(p));
                case FOREGROUND_COLOR_SPAN -> readSpan(p, sp, new ForegroundColorSpan(p));
                case RELATIVE_SIZE_SPAN -> readSpan(p, sp, new RelativeSizeSpan(p));
                case STRIKETHROUGH_SPAN -> readSpan(p, sp, new StrikethroughSpan(p));
                case UNDERLINE_SPAN -> readSpan(p, sp, new UnderlineSpan(p));
                case STYLE_SPAN -> readSpan(p, sp, new StyleSpan(p));
                case LEADING_MARGIN_SPAN -> readSpan(p, sp, new LeadingMarginSpan.Standard(p));
                case URL_SPAN -> readSpan(p, sp, new URLSpan(p));
                case BACKGROUND_COLOR_SPAN -> readSpan(p, sp, new BackgroundColorSpan(p));
                case TYPEFACE_SPAN -> readSpan(p, sp, new TypefaceSpan(p));
                case SUPERSCRIPT_SPAN -> readSpan(p, sp, new SuperscriptSpan(p));
                case SUBSCRIPT_SPAN -> readSpan(p, sp, new SubscriptSpan(p));
                case ABSOLUTE_SIZE_SPAN -> readSpan(p, sp, new AbsoluteSizeSpan(p));
                case LOCALE_SPAN -> readSpan(p, sp, new LocaleSpan(p));
                case LINE_BACKGROUND_SPAN -> readSpan(p, sp, new LineBackgroundSpan.Standard(p));
            }
        }
        return sp;
    }

    private static void readSpan(Parcel p, Spannable sp, Object o) {
        sp.setSpan(o, p.readInt(), p.readInt(), p.readInt());
    }

    /**
     * Debugging tool to print the spans in a CharSequence.  The output will
     * be printed one span per line.  If the CharSequence is not a Spanned,
     * then the entire string will be printed on a single line.
     */
    public static void dumpSpans(CharSequence cs, PrintWriter printer, String prefix) {
        if (cs instanceof Spanned sp) {
            List<?> os = sp.getSpans(0, cs.length(), Object.class);

            for (int i = 0; i < os.size(); i++) {
                Object o = os.get(i);
                int st = sp.getSpanStart(o);
                int en = sp.getSpanEnd(o);
                int fl = sp.getSpanFlags(o);
                printer.println(prefix + substring(cs, st, en) + ": "
                        + Integer.toHexString(System.identityHashCode(o))
                        + " " + o.getClass().getCanonicalName()
                        + " (" + st + "-" + en + ") fl=#" + Integer.toHexString(fl));
            }
        } else {
            printer.println(prefix + cs + ": (no spans)");
        }
    }

    /**
     * Where to truncate.
     */
    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        @Deprecated
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
    @NonNull
    public static CharSequence ellipsize(@NonNull CharSequence text, @NonNull TextPaint p,
                                         float avail, @NonNull TruncateAt where) {
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
    @NonNull
    public static CharSequence ellipsize(@NonNull CharSequence text, @NonNull TextPaint paint,
                                         float avail, @NonNull TruncateAt where,
                                         boolean preserveLength, @Nullable EllipsizeCallback callback) {
        return ellipsize(text, paint, avail, where, preserveLength, callback,
                TextDirectionHeuristics.FIRSTSTRONG_LTR, getEllipsisChars(where));
    }

    @NonNull
    public static CharSequence ellipsize(@NonNull CharSequence text, @NonNull TextPaint paint,
                                         float avail, @NonNull TruncateAt where,
                                         boolean preserveLength, @Nullable EllipsizeCallback callback,
                                         @NonNull TextDirectionHeuristic textDir) {
        return ellipsize(text, paint, avail, where, preserveLength, callback,
                textDir, getEllipsisChars(where));
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
     * @hidden
     */
    @NonNull
    private static CharSequence ellipsize(@NonNull CharSequence text, @NonNull TextPaint paint,
                                          float avail, @NonNull TruncateAt where, boolean preserveLength,
                                          @Nullable EllipsizeCallback callback,
                                          @NonNull TextDirectionHeuristic textDir, @NonNull char[] ellipsis) {

        final int len = text.length();

        MeasuredParagraph mt = null;
        try {
            mt = MeasuredParagraph.buildForStaticLayout(paint, null, text, 0, text.length(), textDir, false, null);
            float width = mt.getAdvance(0, text.length());

            if (width <= avail) {
                if (callback != null) {
                    callback.ellipsized(0, 0);
                }

                return text;
            }

            // NB: ellipsis string is considered as Force LTR
            float ellipsisWidth = LayoutCache.getOrCreate(ellipsis, 0, ellipsis.length,
                    0, ellipsis.length, false, paint.getInternalPaint(), 0).getAdvance();
            avail -= ellipsisWidth;

            int left = 0;
            int right = len;
            if (avail >= 0) {
                if (where == TruncateAt.START) {
                    right = len - mt.breakText(len, false, avail);
                } else if (where == TruncateAt.END || where == TruncateAt.MARQUEE) {
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
                if (remaining > 0 && removed >= ellipsis.length) {
                    System.arraycopy(ellipsis, 0, buf, left, ellipsis.length);
                    left += ellipsis.length;
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
                StringBuilder sb = new StringBuilder(remaining + ellipsis.length);
                sb.append(buf, 0, left);
                sb.append(ellipsis);
                sb.append(buf, right, len - right);
                return sb.toString();
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(text, 0, left);
            ssb.append(CharBuffer.wrap(ellipsis));
            ssb.append(text, right, len);
            return ssb;
        } finally {
            if (mt != null) {
                mt.recycle();
            }
        }
    }

    static int breakText(float[] advances, int limit, boolean forwards, float width) {
        if (forwards) {
            int i = 0;
            while (i < limit) {
                width -= advances[i];
                if (width < 0.0f) break;
                i++;
            }
            return i;
        } else {
            int i = limit - 1;
            while (i >= 0) {
                width -= advances[i];
                if (width < 0.0f) break;
                i--;
            }
            while (i < limit - 1 && advances[i + 1] == 0.0f) {
                i++;
            }
            return limit - i - 1;
        }
    }

    /**
     * Returns a CharSequence concatenating the specified CharSequences,
     * retaining their spans if any.
     * <p>
     * If there are no elements, an empty string will be returned.
     * <p>
     * If the number of elements is exactly one, that element is returned, even if it
     * is null or mutable.
     * <p>
     * If the number of elements is at least two, any null CharSequence among the elements is
     * treated as if it was the string <code>"null"</code>, and a new String or SpannedString is
     * returned.
     * <p>
     * If there are paragraph spans in the source CharSequences that satisfy paragraph boundary
     * requirements in the sources but would no longer satisfy them in the concatenated
     * CharSequence, they may get extended in the resulting CharSequence or not retained.
     *
     * @since 3.10.1
     */
    public static CharSequence concat(@NonNull CharSequence... elements) {
        if (elements.length == 0) {
            return "";
        }

        CharSequence first = elements[0];
        if (elements.length == 1) {
            return first;
        }

        boolean spanned = first instanceof Spanned;
        for (int i = 1; !spanned && i < elements.length; i++) {
            spanned = elements[i] instanceof Spanned;
        }

        if (spanned) {
            final SpannableStringBuilder ssb = new SpannableStringBuilder();
            for (CharSequence piece : elements) {
                ssb.append(piece == null ? "null" : piece);
            }
            return new SpannedString(ssb);
        } else {
            // join() is faster
            return String.join("", elements);
        }
    }

    /**
     * Returns a CharSequence concatenating the specified CharSequences,
     * retaining their spans if any.
     * <p>
     * If there are no elements, an empty string will be returned.
     * <p>
     * If the number of elements is exactly one, that element is returned, even if it
     * is null or mutable.
     * <p>
     * If the number of elements is at least two, any null CharSequence among the elements is
     * treated as if it was the string <code>"null"</code>, and a new String or SpannedString is
     * returned.
     * <p>
     * If there are paragraph spans in the source CharSequences that satisfy paragraph boundary
     * requirements in the sources but would no longer satisfy them in the concatenated
     * CharSequence, they may get extended in the resulting CharSequence or not retained.
     *
     * @since 3.10.1
     */
    public static CharSequence concat(@NonNull Iterable<? extends CharSequence> elements) {
        Iterator<? extends CharSequence> it = elements.iterator();
        if (!it.hasNext()) {
            return "";
        }

        CharSequence first = it.next();
        if (!it.hasNext()) {
            return first;
        }

        boolean spanned = first instanceof Spanned;
        while (!spanned && it.hasNext()) {
            spanned = it.next() instanceof Spanned;
        }

        if (spanned) {
            final SpannableStringBuilder ssb = new SpannableStringBuilder();
            for (CharSequence piece : elements) {
                ssb.append(piece == null ? "null" : piece);
            }
            return new SpannedString(ssb);
        } else {
            // join() is faster
            return String.join("", elements);
        }
    }

    /**
     * Returns a CharSequence composed of copies of the <var>elements</var> joined together
     * with the specified <var>delimiter</var>.
     * <p>
     * If there are no elements, an empty string will be returned. Otherwise, returns a new
     * CharSequence. Any null value will be replaced with the string <code>"null"</code>.
     * <p>
     * Unlike Android, this method retains their spans if any. If you want to ignore all the
     * spans, use {@link String#join(CharSequence, CharSequence...)} instead.
     * <p>
     * If there are paragraph spans in the source CharSequences that satisfy paragraph boundary
     * requirements in the sources but would no longer satisfy them in the concatenated
     * CharSequence, they may get extended in the resulting CharSequence or not retained.
     *
     * @param delimiter the delimiter that separates each element, may be {@link Spanned}
     * @param elements  an array of char sequences to join together, may be {@link Spanned}
     * @return a String or SpannedString
     * @since 3.10.1
     */
    @NonNull
    public static CharSequence join(@NonNull CharSequence delimiter,
                                    @NonNull CharSequence... elements) {
        if (elements.length == 0) {
            return "";
        }

        CharSequence first = elements[0];
        if (elements.length == 1) {
            return first instanceof Spanned
                    ? SpannedString.valueOf(first)
                    : String.valueOf(first);
        }

        boolean spanned = first instanceof Spanned ||
                delimiter instanceof Spanned;
        for (int i = 1; !spanned && i < elements.length; i++) {
            spanned = elements[i] instanceof Spanned;
        }

        if (spanned) {
            final SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(first == null ? "null" : first);
            for (int i = 1; i < elements.length; i++) {
                ssb.append(delimiter);
                CharSequence piece = elements[i];
                ssb.append(piece == null ? "null" : piece);
            }
            return new SpannedString(ssb);
        } else {
            return String.join(delimiter, elements);
        }
    }

    /**
     * Returns a CharSequence composed of copies of the <var>elements</var> joined together
     * with the specified <var>delimiter</var>.
     * <p>
     * If there are no elements, an empty string will be returned. Otherwise, returns a new
     * CharSequence. Any null value will be replaced with the string <code>"null"</code>.
     * <p>
     * Unlike Android, this method retains their spans if any. If you want to ignore all the
     * spans, use {@link String#join(CharSequence, Iterable)} instead.
     * <p>
     * If there are paragraph spans in the source CharSequences that satisfy paragraph boundary
     * requirements in the sources but would no longer satisfy them in the concatenated
     * CharSequence, they may get extended in the resulting CharSequence or not retained.
     *
     * @param delimiter the delimiter that separates each element, may be {@link Spanned}
     * @param elements  an iterable of char sequences to join together, may be {@link Spanned}
     * @return a String or SpannedString
     * @since 3.10.1
     */
    @NonNull
    public static CharSequence join(@NonNull CharSequence delimiter,
                                    @NonNull Iterable<? extends CharSequence> elements) {
        Iterator<? extends CharSequence> it = elements.iterator();
        if (!it.hasNext()) {
            return "";
        }

        CharSequence first = it.next();
        if (!it.hasNext()) {
            return first instanceof Spanned
                    ? SpannedString.valueOf(first)
                    : String.valueOf(first);
        }

        boolean spanned = first instanceof Spanned ||
                delimiter instanceof Spanned;
        while (!spanned && it.hasNext()) {
            spanned = it.next() instanceof Spanned;
        }

        if (spanned) {
            final SpannableStringBuilder ssb = new SpannableStringBuilder();
            it = elements.iterator();
            it.next();
            ssb.append(first == null ? "null" : first);
            do {
                ssb.append(delimiter);
                CharSequence piece = it.next();
                ssb.append(piece == null ? "null" : piece);
            } while (it.hasNext());
            return new SpannedString(ssb);
        } else {
            return String.join(delimiter, elements);
        }
    }

    private static final String[] sBinaryCompacts = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB"};

    @NonNull
    public static String binaryCompact(long num) {
        if (num <= 0)
            return "0 bytes";
        if (num < 1024)
            return num + " bytes";
        int i = (63 - Long.numberOfLeadingZeros(num)) / 10;
        return String.format("%.2f %s",
                (double) num / (1L << (i * 10)),
                sBinaryCompacts[i]);
    }

    public static void binaryCompact(@NonNull Appendable a, long num) {
        try {
            if (num <= 0) {
                a.append("0 bytes");
            } else if (num < 1024) {
                if (a instanceof StringBuilder) {
                    ((StringBuilder) a).append(num);
                } else {
                    a.append(String.valueOf(num));
                }
                a.append(" bytes");
            } else {
                int i = (63 - Long.numberOfLeadingZeros(num)) / 10;
                new Formatter(a).format("%.2f %s",
                        (double) num / (1L << (i * 10)),
                        sBinaryCompacts[i]);
            }
        } catch (IOException ignored) {
        }
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
    public static void copySpansFrom(@NonNull Spanned source, int start, int end,
                                     @Nullable Class<?> type,
                                     @NonNull Spannable dest, int destoff) {
        if (type == null) {
            type = Object.class;
        }

        List<?> spans = source.getSpans(start, end, type);

        for (int i = 0; i < spans.size(); i++) {
            Object span = spans.get(i);
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

    /**
     * Returns true if the character's presence could affect RTL layout
     * (require BiDi analysis).
     * <p>
     * In order to be fast, the code is intentionally rough and quite conservative in its
     * considering inclusion of any non-BMP or surrogate characters or anything in the bidi
     * blocks or any bidi formatting characters with a potential to affect RTL layout.
     * See {@link #requiresBidi(char[], int, int)} for stricter version.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean couldAffectRtl(char c) {
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
     * Returns true if the character's presence could affect RTL layout
     * (require BiDi analysis).
     * <p>
     * Since this calls couldAffectRtl() above, it's also quite conservative, in the way that
     * it may return 'true' (needs bidi) although careful consideration may tell us it should
     * return 'false' (does not need bidi).
     *
     * @hidden
     */
    @ApiStatus.Internal
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean couldAffectRtl(char[] text,
                                         int start,
                                         int limit) {
        for (int i = start; i < limit; i++) {
            if (couldAffectRtl(text[i])) {
                return true;
            }
        }
        return false;
    }

    // See ICU Bidi.requiresBidi()
    // Added RIGHT_TO_LEFT_ISOLATE, but is ARABIC_NUMBER needed?
    static final int RTL_MASK = 1 << UCharacter.RIGHT_TO_LEFT |
            1 << UCharacter.RIGHT_TO_LEFT_ARABIC |
            1 << UCharacter.RIGHT_TO_LEFT_EMBEDDING |
            1 << UCharacter.RIGHT_TO_LEFT_OVERRIDE |
            1 << UCharacter.RIGHT_TO_LEFT_ISOLATE |
            1 << UCharacter.ARABIC_NUMBER;

    /**
     * Similar to {@link com.ibm.icu.text.Bidi#requiresBidi(char[], int, int)},
     * but this fixes the issue where it did not consider SMP characters.
     * <p>
     * This method can carefully determine whether BiDi analysis
     * is needed (i.e. containing multiple BiDi runs or only one RTL run),
     * when LTR or DEFAULT_LTR algorithm is used.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean requiresBidi(char[] text,
                                       int start,
                                       int limit) {
        for (int i = start, cp; i < limit; ) {
            char c1;
            cp = c1 = text[i++];
            if (Character.isHighSurrogate(c1) && i < limit) {
                char c2;
                if (Character.isLowSurrogate(c2 = text[i])) {
                    cp = Character.toCodePoint(c1, c2);
                    i++;
                }
            }
            if (((1 << UCharacter.getDirection(cp)) & RTL_MASK) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the layout direction for a given Locale
     *
     * @param locale the Locale for which we want the layout direction. Can be null.
     * @return the layout direction. This may be one of:
     * {@link View#LAYOUT_DIRECTION_LTR} or
     * {@link View#LAYOUT_DIRECTION_RTL}.
     * <p>
     * Be careful: this code will need to be updated when vertical scripts will be supported
     */
    @View.ResolvedLayoutDir
    public static int getLayoutDirectionFromLocale(@Nullable Locale locale) {
        return (locale != null && !locale.equals(Locale.ROOT)
                && ULocale.forLocale(locale).isRightToLeft())
                ? View.LAYOUT_DIRECTION_RTL
                : View.LAYOUT_DIRECTION_LTR;
    }

    /**
     * Replace all invalid surrogate pairs with 'U+FFFD' for the given UTF-16 string.
     * Return the given string as-is if it was validated, or a new string.
     */
    @NonNull
    public static String validateSurrogatePairs(@NonNull String text) {
        final int n = text.length();
        StringBuilder b = null; // lazy init
        char c1, c2;
        for (int i = 0; i < n; i++) {
            c1 = text.charAt(i);
            if (Character.isHighSurrogate(c1) && i + 1 < n) {
                c2 = text.charAt(i + 1);
                if (Character.isLowSurrogate(c2)) {
                    if (b != null)
                        b.append(c1).append(c2);
                    i++;
                } else {
                    if (b == null) {
                        b = new StringBuilder(n);
                        b.append(text, 0, i);
                    }
                    b.append('\uFFFD');
                }
            } else if (Character.isSurrogate(c1)) {
                if (b == null) {
                    b = new StringBuilder(n);
                    b.append(text, 0, i);
                }
                b.append('\uFFFD');
            } else if (b != null) {
                b.append(c1);
            }
        }
        return b != null ? b.toString() : text;
    }

    /**
     * Find the Levenshtein distance between <var>a</var> and <var>b</var>.
     * This algorithm has a time complexity of O(m*n) and a space complexity of O(n),
     * where m is the length of <var>a</var> and n is the length of <var>b</var>.
     * <p>
     * This method only works for Unicode BMP characters without taking into account
     * grapheme clusters.
     *
     * @return the Levenshtein distance in chars (u16)
     * @since 3.7
     */
    public static int distance(@NonNull CharSequence a, @NonNull CharSequence b) {
        return distance(a, b, null);
    }

    public static int distance(@NonNull CharSequence a, @NonNull CharSequence b,
                               @Nullable int[] supp) {
        // fast path for reference equality
        if (a == b)
            return 0;
        int m = a.length(), n = b.length();
        // fast path for either of the two is zero-length
        if (m == 0 || n == 0)
            return m | n;
        return m < n
                ? distance0(b, a, n, m, supp)
                : distance0(a, b, m, n, supp);
    }

    private static int distance0(@NonNull CharSequence a, @NonNull CharSequence b,
                                 int m, int n, @Nullable int[] supp) {
        // assert m >= n;
        int i, j, w, c;
        int[] d = supp != null && supp.length >= n + 1 ? supp : new int[n + 1];
        for (j = 1; j <= n; j++)
            d[j] = j;
        for (i = 1; i <= m; i++) {
            d[0] = i;
            w = i - 1;
            for (j = 1; j <= n; j++) {
                c = Math.min(Math.min(d[j], d[j - 1]) + 1,
                        a.charAt(i - 1) == b.charAt(j - 1) ? w : w + 1);
                w = d[j];
                d[j] = c;
            }
        }
        return d[n];
    }
}
