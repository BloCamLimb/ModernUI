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

import com.ibm.icu.util.ULocale;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.text.LayoutPiece;
import icyllis.modernui.graphics.text.ShapedText;
import icyllis.modernui.text.style.*;
import icyllis.modernui.util.Parcel;
import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

import java.nio.CharBuffer;
import java.util.*;

public final class TextUtils {

    private static final char[][] sTemp = new char[4][];

    // Zero-width character used to fill ellipsized strings when codepoint length must be preserved.
    static final char ELLIPSIS_FILLER = '\uFEFF'; // ZERO WIDTH NO-BREAK SPACE

    //TODO: Based on CLDR data, these need to be localized for Dzongkha (dz) and perhaps
    // Hong Kong Traditional Chinese (zh-Hant-HK), but that may need to depend on the actual word
    // being ellipsized and not the locale.
    private static final String ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)

    private static final char[] ELLIPSIS_NORMAL_ARRAY = ELLIPSIS_NORMAL.toCharArray();

    private TextUtils() {
    }

    @NonNull
    public static String getEllipsisString(@NonNull TextUtils.TruncateAt method) {
        return ELLIPSIS_NORMAL;
    }

    //TODO temp, remove in future
    @ApiStatus.Internal
    @NonNull
    public static char[] getEllipsisChars(@NonNull TextUtils.TruncateAt method) {
        return ELLIPSIS_NORMAL_ARRAY;
    }

    /**
     * Returns a temporary char buffer.
     *
     * @param len the length of the buffer
     * @return a char buffer
     * @hidden
     * @see #recycle(char[]) recycle the buffer
     */
    @ApiStatus.Internal
    @NonNull
    public static char[] obtain(int len) {
        if (len > 2000)
            return new char[len];

        char[] buf = null;

        synchronized (sTemp) {
            final char[][] pool = sTemp;
            for (int i = pool.length - 1; i >= 0; --i) {
                if ((buf = pool[i]) != null && buf.length >= len) {
                    pool[i] = null;
                    break;
                }
            }
        }

        if (buf == null || buf.length < len)
            buf = new char[len];

        return buf;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static void recycle(@NonNull char[] temp) {
        if (temp.length > 2000)
            return;

        synchronized (sTemp) {
            final char[][] pool = sTemp;
            for (int i = 0; i < pool.length; ++i) {
                if (pool[i] == null) {
                    pool[i] = temp;
                    break;
                }
            }
        }
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
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            } else {
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
     */
    public static void getChars(@NonNull CharSequence s, int srcBegin, int srcEnd,
                                @NonNull char[] dst, int dstBegin) {
        if (s instanceof String)
            ((String) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof GetChars)
            ((GetChars) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof StringBuffer)
            ((StringBuffer) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof StringBuilder)
            ((StringBuilder) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof CharBuffer buf)
            buf.get(buf.position() + srcBegin, dst, dstBegin, srcEnd - srcBegin); // Java 13
        else {
            for (int i = srcBegin; i < srcEnd; i++)
                dst[dstBegin++] = s.charAt(i);
        }
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
     */
    public static String substring(CharSequence source, int start, int end) {
        if (source instanceof String)
            return ((String) source).substring(start, end);
        if (source instanceof StringBuilder)
            return ((StringBuilder) source).substring(start, end);
        if (source instanceof StringBuffer)
            return ((StringBuffer) source).substring(start, end);
        if (source instanceof SpannableStringInternal)
            return source.toString().substring(start, end);

        char[] temp = obtain(end - start);
        getChars(source, start, end, temp, 0);
        String ret = new String(temp, 0, end - start);
        recycle(temp);

        return ret;
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

    public static int indexOf(@NonNull CharSequence s, char ch, int start, int end) {
        final Class<? extends CharSequence> c = s.getClass();

        if (s instanceof GetChars || c == StringBuffer.class ||
                c == StringBuilder.class || c == String.class ||
                s instanceof CharBuffer) {
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
                c == StringBuilder.class || c == String.class ||
                s instanceof CharBuffer) {
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
            TRAILING_MARGIN_SPAN           = FIRST_SPAN + 29,
            LAST_SPAN                      = TRAILING_MARGIN_SPAN;
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
            dest.writeString8(cs.toString());

            final List<Object> os = sp.getSpans(0, cs.length(), Object.class);
            for (final Object o : os) {
                Object target = o;

                if (target instanceof CharacterStyle) {
                    target = ((CharacterStyle) target).getUnderlying();
                }

                if (target instanceof ParcelableSpan span) {
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
            dest.writeString8(cs.toString());
        }
    }

    @Nullable
    public static CharSequence createFromParcel(@NonNull Parcel p) {
        int type = p.readInt();
        if (type == 0)
            return null;
        final String s = p.readString8();
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
                case ABSOLUTE_SIZE_SPAN -> readSpan(p, sp, new AbsoluteSizeSpan(p));
                case LOCALE_SPAN -> readSpan(p, sp, new LocaleSpan(p));
                case LINE_BACKGROUND_SPAN -> readSpan(p, sp, new LineBackgroundSpan.Standard(p));
                case TRAILING_MARGIN_SPAN -> readSpan(p, sp, new TrailingMarginSpan.Standard(p));
            }
        }
        return sp;
    }

    private static void readSpan(Parcel p, Spannable sp, Object o) {
        sp.setSpan(o, p.readInt(), p.readInt(), p.readInt());
    }

    /**
     * Draw a run of text, all in a single direction, with optional context for complex text
     * shaping.
     * <p>
     * See {@link #drawTextRun(Canvas, CharSequence, int, int, int, int, float, float, boolean, TextPaint)} for
     * more details. This method uses a character array rather than CharSequence to represent the
     * string.
     *
     * @param canvas       the canvas
     * @param text         the text to render
     * @param start        the start of the text to render. Data before this position can be used for
     *                     shaping context.
     * @param end          the end of the text to render. Data at or after this position can be used for
     *                     shaping context.
     * @param contextStart the index of the start of the shaping context
     * @param contextEnd   the index of the end of the shaping context
     * @param x            the x position at which to draw the text
     * @param y            the y position at which to draw the text
     * @param isRtl        whether the run is in RTL direction
     * @param paint        the paint
     */
    public static void drawTextRun(@NonNull Canvas canvas, @NonNull char[] text, int start, int end,
                                   int contextStart, int contextEnd, float x, float y, boolean isRtl,
                                   @NonNull TextPaint paint) {
        if ((start | end | contextStart | contextEnd | start - contextStart | end - start
                | contextEnd - end | text.length - contextEnd) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (start == end) {
            return;
        }
        ShapedText.doLayoutRun(
                text, contextStart, contextEnd,
                start, end, isRtl, paint.getInternalPaint(), null,
                (piece, offsetX) -> drawTextRun(canvas, piece, x + offsetX, y, paint)
        );
    }

    /**
     * Draw a run of text, all in a single direction, with optional context for complex text
     * shaping.
     * <p>
     * The run of text includes the characters from {@code start} to {@code end} in the text. In
     * addition, the range {@code contextStart} to {@code contextEnd} is used as context for the
     * purpose of complex text shaping, such as Arabic text potentially shaped differently based on
     * the text next to it.
     * <p>
     * All text outside the range {@code contextStart..contextEnd} is ignored. The text between
     * {@code start} and {@code end} will be laid out and drawn. The context range is useful for
     * contextual shaping, e.g. Kerning, Arabic contextual form.
     * <p>
     * The direction of the run is explicitly specified by {@code isRtl}. Thus, this method is
     * suitable only for runs of a single direction. Alignment of the text is as determined by the
     * Paint's TextAlign value. Further, {@code 0 <= contextStart <= start <= end <= contextEnd
     * <= text.length} must hold on entry.
     *
     * @param canvas       the canvas
     * @param text         the text to render
     * @param start        the start of the text to render. Data before this position can be used for
     *                     shaping context.
     * @param end          the end of the text to render. Data at or after this position can be used for
     *                     shaping context.
     * @param contextStart the index of the start of the shaping context
     * @param contextEnd   the index of the end of the shaping context
     * @param x            the x position at which to draw the text
     * @param y            the y position at which to draw the text
     * @param isRtl        whether the run is in RTL direction
     * @param paint        the paint
     * @see #drawTextRun(Canvas, char[], int, int, int, int, float, float, boolean, TextPaint)
     */
    public static void drawTextRun(@NonNull Canvas canvas, @NonNull CharSequence text, int start, int end,
                                   int contextStart, int contextEnd, float x, float y, boolean isRtl,
                                   @NonNull TextPaint paint) {
        if ((start | end | contextStart | contextEnd | start - contextStart | end - start
                | contextEnd - end | text.length() - contextEnd) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (start == end) {
            return;
        }
        final int len = contextEnd - contextStart;
        final char[] buf = obtain(len);
        getChars(text, contextStart, contextEnd, buf, 0);
        ShapedText.doLayoutRun(
                buf, 0, len,
                start - contextStart, end - contextStart, isRtl, paint.getInternalPaint(), null,
                (piece, offsetX) -> drawTextRun(canvas, piece, x + offsetX, y, paint)
        );
        recycle(buf);
    }

    /**
     * Draw a layout piece, the base unit to draw a text.
     *
     * @param piece the layout piece to draw
     * @param x     the horizontal position at which to draw the text between runs
     * @param y     the vertical baseline of the line of text
     * @param paint the paint used to draw the text, only color will be taken
     * @see TextUtils#drawTextRun
     */
    @ApiStatus.Internal
    static void drawTextRun(@NonNull Canvas canvas, @NonNull LayoutPiece piece,
                            float x, float y, @NonNull Paint paint) {
        //TODO this bounds check is not correct, this is logical bounds not visual pixel bounds
        if (piece.getAdvance() == 0 || (piece.getGlyphs().length == 0)
                || canvas.quickReject(x, y + piece.getAscent(),
                x + piece.getAdvance(), y + piece.getDescent())) {
            return;
        }
        final int nGlyphs = piece.getGlyphCount();
        if (nGlyphs == 0) {
            return;
        }
        var lastFont = piece.getFont(0);
        int lastPos = 0;
        int currPos = 1;
        for (; currPos < nGlyphs; currPos++) {
            var curFont = piece.getFont(currPos);
            if (lastFont != curFont) {
                canvas.drawGlyphs(piece.getGlyphs(), lastPos,
                        piece.getPositions(), lastPos << 1, currPos - lastPos,
                        lastFont, x, y, paint);
                lastFont = curFont;
                lastPos = currPos;
            }
        }
        canvas.drawGlyphs(piece.getGlyphs(), lastPos,
                piece.getPositions(), lastPos << 1, currPos - lastPos,
                lastFont, x, y, paint);
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
    @NonNull
    private static CharSequence ellipsize(@NonNull CharSequence text, @NonNull TextPaint paint,
                                          float avail, @NonNull TruncateAt where, boolean preserveLength,
                                          @Nullable EllipsizeCallback callback,
                                          @NonNull TextDirectionHeuristic textDir, @NonNull String ellipsis) {

        final int len = text.length();

        final float ellipsisWidth;

        MeasuredParagraph mt = null;
        try {
            mt = MeasuredParagraph.buildForStaticLayout(paint, null, ellipsis, 0, ellipsis.length(), textDir, false,
                    null);
            ellipsisWidth = mt.getAdvance(0, ellipsis.length());
        } finally {
            if (mt != null) {
                mt.recycle();
                mt = null;
            }
        }

        try {
            mt = MeasuredParagraph.buildForStaticLayout(paint, null, text, 0, text.length(), textDir, false, null);
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

    /**
     * Returns a CharSequence concatenating the specified CharSequences,
     * retaining their spans if any.
     * <p>
     * If there are no parameters, an empty string will be returned.
     * <p>
     * If the number of parameters is exactly one, that parameter is returned if it is not null.
     * Otherwise, the string <code>"null"</code> is returned.
     * <p>
     * If the number of parameters is at least two, any null CharSequence among the parameters is
     * treated as if it was the string <code>"null"</code>.
     * <p>
     * If there are paragraph spans in the source CharSequences that satisfy paragraph boundary
     * requirements in the sources but would no longer satisfy them in the concatenated
     * CharSequence, they may get extended in the resulting CharSequence or not retained.
     */
    @NonNull
    public static CharSequence concat(@NonNull CharSequence... elements) {
        if (elements.length == 0) {
            return "";
        }

        CharSequence first = elements[0];
        if (elements.length == 1) {
            return first == null ? "null" : first;
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
     * If there are no parameters, an empty string will be returned.
     * <p>
     * If the number of parameters is exactly one, that parameter is returned if it is not null.
     * Otherwise, the string <code>"null"</code> is returned.
     * <p>
     * If the number of parameters is at least two, any null CharSequence among the parameters is
     * treated as if it was the string <code>"null"</code>.
     * <p>
     * If there are paragraph spans in the source CharSequences that satisfy paragraph boundary
     * requirements in the sources but would no longer satisfy them in the concatenated
     * CharSequence, they may get extended in the resulting CharSequence or not retained.
     */
    @NonNull
    public static CharSequence concat(@NonNull Iterable<? extends CharSequence> elements) {
        Iterator<? extends CharSequence> it = elements.iterator();
        if (!it.hasNext()) {
            return "";
        }

        CharSequence first = it.next();
        if (!it.hasNext()) {
            return first == null ? "null" : first;
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

    @NonNull
    public static CharSequence join(@NonNull CharSequence delimiter,
                                    @NonNull CharSequence... elements) {
        if (elements.length == 0) {
            return "";
        }

        CharSequence first = elements[0];
        if (elements.length == 1) {
            return first == null ? "null" : first;
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

    @NonNull
    public static CharSequence join(@NonNull CharSequence delimiter,
                                    @NonNull Iterable<? extends CharSequence> elements) {
        Iterator<? extends CharSequence> it = elements.iterator();
        if (!it.hasNext()) {
            return "";
        }

        CharSequence first = it.next();
        if (!it.hasNext()) {
            return first == null ? "null" : first;
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
        // fast path for reference equality
        if (a == b)
            return 0;
        int m = a.length(), n = b.length();
        // fast path for either of the two is zero-length
        if (m == 0 || n == 0)
            return m | n;
        return m < n
                ? distance0(b, a, n, m)
                : distance0(a, b, m, n);
    }

    private static int distance0(@NonNull CharSequence a, @NonNull CharSequence b,
                                 int m, int n) {
        // assert m >= n;
        int i, j, w, c;
        int[] d = new int[n + 1];
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
