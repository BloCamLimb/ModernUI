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
 * Copyright (C) 2014 The Android Open Source Project
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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

import static com.ibm.icu.lang.UCharacter.GraphemeClusterBreak.*;

/**
 * This class handles grapheme cluster break.
 * <p>
 * It is important to recognize that what the user thinks of as a
 * &quot;character&quot;-a basic unit of a writing system for a language-
 * may not be just a single Unicode code point. Instead, that basic
 * unit may be made up of multiple Unicode code points. To avoid
 * ambiguity with the computer use of the term character, this is
 * called a user-perceived character. For example, &quot;G&quot; + grave-accent
 * is a user-perceived character: users think of it as a single
 * character, yet is actually represented by two Unicode code points.
 * These user-perceived characters are approximated by what is called
 * a grapheme cluster, which can be determined programmatically.
 */
public final class GraphemeBreak {

    /**
     * Compute the valid cursor after offset or the limit of the context, whichever is less.
     */
    public static final int AFTER = 0;

    /**
     * Compute the valid cursor at or after the offset or the limit of the context, whichever is less.
     */
    public static final int AT_OR_AFTER = 1;

    /**
     * Compute the valid cursor before offset or the start of the context, whichever is greater.
     */
    public static final int BEFORE = 2;

    /**
     * Compute the valid cursor at or before offset or the start of the context, whichever is greater.
     */
    public static final int AT_OR_BEFORE = 3;

    /**
     * Return offset if the cursor at offset is valid, or -1 if it isn't.
     */
    public static final int AT = 4;

    /**
     * Config value, true to use ICU GCB, otherwise this
     */
    public static boolean sUseICU = true;

    private GraphemeBreak() {
    }

    /**
     * Returns the next cursor position in the run.
     * <p>
     * This avoids placing the cursor between surrogates, between characters that form conjuncts,
     * between base characters and combining marks, or within a reordering cluster.
     *
     * <p>
     * ContextStart and offset are relative to the start of text.
     * The context is the shaping context for cursor movement, generally the bounds of the metric
     * span enclosing the cursor in the direction of movement.
     *
     * <p>
     * If op is {@link #AT} and the offset is not a valid cursor position, this
     * returns -1.  Otherwise this will never return a value before contextStart or after
     * contextStart + contextLength.
     *
     * @param text          the text
     * @param locale        the text's locale
     * @param contextStart  the start of the context
     * @param contextLength the length of the context
     * @param offset        the cursor position to move from
     * @param op            how to move the cursor
     * @return the offset of the next position or -1
     */
    public static int getTextRunCursor(@Nonnull char[] text, @Nonnull Locale locale, int contextStart,
                                       int contextLength, int offset, int op) {
        int contextEnd = contextStart + contextLength;
        if (((contextStart | contextEnd | offset | (contextEnd - contextStart)
                | (offset - contextStart) | (contextEnd - offset)
                | (text.length - contextEnd) | op) < 0)
                || op > AT) {
            throw new IndexOutOfBoundsException();
        }
        return sUseICU ? getTextRunCursorICU(new CharArrayIterator(text, contextStart, contextEnd), locale, offset, op)
                : getTextRunCursorImpl(null, text, contextStart, contextLength, offset, op);
    }

    /**
     * Returns the next cursor position in the run.
     * <p>
     * This avoids placing the cursor between surrogates, between characters that form conjuncts,
     * between base characters and combining marks, or within a reordering cluster.
     *
     * <p>
     * ContextStart, contextEnd, and offset are relative to the start of
     * text.  The context is the shaping context for cursor movement, generally
     * the bounds of the metric span enclosing the cursor in the direction of
     * movement.
     *
     * <p>
     * If op is {@link #AT} and the offset is not a valid cursor position, this
     * returns -1.  Otherwise this will never return a value before contextStart or after
     * contextEnd.
     *
     * @param text         the text
     * @param locale       the text's locale
     * @param contextStart the start of the context
     * @param contextEnd   the end of the context
     * @param offset       the cursor position to move from
     * @param op           how to move the cursor
     * @return the offset of the next position, or -1
     */
    public static int getTextRunCursor(@Nonnull CharSequence text, @Nonnull Locale locale, int contextStart,
                                       int contextEnd, int offset, int op) {
        if (text instanceof String || text instanceof SpannedString ||
                text instanceof SpannableString) {
            return getTextRunCursor(text.toString(), locale, contextStart, contextEnd,
                    offset, op);
        }
        final int contextLen = contextEnd - contextStart;
        final char[] buf = new char[contextLen];
        TextUtils.getChars(text, contextStart, contextEnd, buf, 0);
        offset = getTextRunCursor(buf, locale, 0, contextLen, offset - contextStart, op);
        return offset == -1 ? -1 : offset + contextStart;
    }

    public static int getTextRunCursor(@Nonnull String text, @Nonnull Locale locale, int contextStart, int contextEnd,
                                       int offset, int op) {
        if (((contextStart | contextEnd | offset | (contextEnd - contextStart)
                | (offset - contextStart) | (contextEnd - offset)
                | (text.length() - contextEnd) | op) < 0)
                || op > AT) {
            throw new IndexOutOfBoundsException();
        }
        return sUseICU ? getTextRunCursorICU(new StringCharacterIterator(text, contextStart, contextEnd, contextStart),
                locale, offset, op) :
                getTextRunCursorImpl(null, text.toCharArray(), contextStart, contextEnd - contextStart,
                        offset, op);
    }

    public static void getTextRuns(@Nonnull char[] text, @Nonnull Locale locale, int contextStart, int contextEnd,
                                   @Nonnull RunConsumer consumer) {
        if (sUseICU) {
            final BreakIterator breaker = BreakIterator.getCharacterInstance(locale);
            breaker.setText(new CharArrayIterator(text, contextStart, contextEnd));
            int prevOffset = contextStart;
            int offset;
            while ((offset = breaker.following(prevOffset)) != BreakIterator.DONE) {
                consumer.onRun(prevOffset, offset);
                prevOffset = offset;
            }
        } else {
            final int count = contextEnd - contextStart;
            int prevOffset = contextStart;
            int offset;
            while ((offset = getTextRunCursorImpl(null, text, contextStart, count, prevOffset, AFTER))
                    != prevOffset) {
                consumer.onRun(prevOffset, offset);
                prevOffset = offset;
            }
        }
    }

    @FunctionalInterface
    public interface RunConsumer {

        void onRun(int start, int end);
    }

    public static int getTextRunCursorICU(CharacterIterator text, Locale locale, int offset, int op) {
        final int oof = offset;
        BreakIterator breaker = BreakIterator.getCharacterInstance(locale);
        breaker.setText(text);
        switch (op) {
            case AFTER:
                offset = breaker.following(offset);
                break;
            case AT_OR_AFTER:
                if (!breaker.isBoundary(offset))
                    offset = breaker.following(offset);
                break;
            case BEFORE:
                offset = breaker.preceding(offset);
                break;
            case AT_OR_BEFORE:
                if (!breaker.isBoundary(offset))
                    offset = breaker.preceding(offset);
                break;
            case AT:
                if (!breaker.isBoundary(offset))
                    return -1;
                break;
        }
        return offset == BreakIterator.DONE ? oof : offset;
    }

    public static int getTextRunCursorImpl(@Nullable float[] advances, @Nonnull char[] buf, int start, int count,
                                           int offset, int op) {
        switch (op) {
            case AFTER:
                if (offset < start + count) {
                    offset++;
                }
                // fallthrough
            case AT_OR_AFTER:
                while (!isGraphemeBreak(advances, buf, start, count, offset)) {
                    offset++;
                }
                break;
            case BEFORE:
                if (offset > start) {
                    offset--;
                }
                // fallthrough
            case AT_OR_BEFORE:
                while (!isGraphemeBreak(advances, buf, start, count, offset)) {
                    offset--;
                }
                break;
            case AT:
                if (!isGraphemeBreak(advances, buf, start, count, offset)) {
                    offset = -1;
                }
                break;
        }
        return offset;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isGraphemeBreak(@Nullable float[] advances, @Nonnull char[] buf, int start, int count, final int offset) {
        // This implementation closely follows Unicode Standard Annex #29 on
        // Unicode Text Segmentation (http://www.unicode.org/reports/tr29/),
        // implementing a tailored version of extended grapheme clusters.
        // The GB rules refer to section 3.1.1, Grapheme Cluster Boundary Rules.

        // Rule GB1, sot ÷; Rule GB2, ÷ eot
        if (offset <= start || offset >= start + count) {
            return true;
        }
        if (Character.isLowSurrogate(buf[offset])) {
            // Don't break a surrogate pair, but a lonely trailing surrogate pair is a break
            return !Character.isHighSurrogate(buf[offset - 1]);
        }
        int c1;
        int c2;
        int offsetBack = offset;

        char _c1;
        char _c2;
        // NEXT ONCE

        c2 = _c1 = buf[offsetBack++];
        if (Character.isHighSurrogate(_c1))
            if (offsetBack != start + count &&
                    Character.isLowSurrogate(_c2 = buf[offsetBack])) {
                ++offsetBack;
                c2 = Character.toCodePoint(_c1, _c2);
            }

        offsetBack = offset;

        // PREV
        c1 = _c1 = buf[--offsetBack];
        if (Character.isLowSurrogate(_c1))
            if (offsetBack > start &&
                    Character.isHighSurrogate(_c2 = buf[offsetBack - 1])) {
                --offsetBack;
                c1 = Character.toCodePoint(_c2, _c1);
            }

        int p1 = tailoredGraphemeClusterBreak(c1);
        int p2 = tailoredGraphemeClusterBreak(c2);

        // Rule GB3, CR x LF
        if (p1 == CR && p2 == LF) {
            return false;
        }
        // Rule GB4, (Control | CR | LF) ÷
        if (p1 == CONTROL || p1 == CR || p1 == LF) {
            return true;
        }
        // Rule GB5, ÷ (Control | CR | LF)
        if (p2 == CONTROL || p2 == CR || p2 == LF) {
            return true;
        }
        // Rule GB6, L x ( L | V | LV | LVT )
        if (p1 == L && (p2 == L || p2 == V || p2 == LV || p2 == LVT)) {
            return false;
        }
        // Rule GB7, ( LV | V ) x ( V | T )
        if ((p1 == LV || p1 == V) && (p2 == V || p2 == T)) {
            return false;
        }
        // Rule GB8, ( LVT | T ) x T
        if ((p1 == LVT || p1 == T) && p2 == T) {
            return false;
        }

        // This is used to decide font-dependent grapheme clusters. If we don't have the advance
        // information, we become conservative in grapheme breaking and assume that it has no advance.
        boolean c2_has_advance = (advances != null && advances[offset - start] != 0.0);

        // All the following rules are font-dependent, in the way that if we know c2 has an advance,
        // we definitely know that it cannot form a grapheme with the character(s) before it. So we
        // make the decision in favor a grapheme break early.
        if (c2_has_advance) {
            return true;
        }

        // Rule GB9, x (Extend | ZWJ); Rule GB9a, x SpacingMark; Rule GB9b, Prepend x
        if (p2 == EXTEND || p2 == ZWJ || p2 == SPACING_MARK || p1 == PREPEND) {
            return false;
        }

        // Tailored version of Rule GB11
        // \p{Extended_Pictographic} Extend* ZWJ x \p{Extended_Pictographic}
        if (offsetBack > start && p1 == ZWJ &&
                UCharacter.hasBinaryProperty(c2, UProperty.EXTENDED_PICTOGRAPHIC)) {
            int offsetBack1 = offsetBack;
            int p0;

            int c0 = _c1 = buf[--offsetBack1];
            if (Character.isLowSurrogate(_c1))
                if (offsetBack1 > start &&
                        Character.isHighSurrogate(_c2 = buf[offsetBack1 - 1])) {
                    --offsetBack1;
                    c0 = Character.toCodePoint(_c2, _c1);
                }

            p0 = tailoredGraphemeClusterBreak(c0);
            while (p0 == EXTEND && offsetBack1 > start) {
                c0 = _c1 = buf[--offsetBack1];
                if (Character.isLowSurrogate(_c1))
                    if (offsetBack1 > start &&
                            Character.isHighSurrogate(_c2 = buf[offsetBack1 - 1])) {
                        --offsetBack1;
                        c0 = Character.toCodePoint(_c2, _c1);
                    }
                p0 = tailoredGraphemeClusterBreak(c0);
            }
            if (UCharacter.hasBinaryProperty(c0, UProperty.EXTENDED_PICTOGRAPHIC)) {
                return false;
            }
        }

        // Tailored version of Rule GB12 and Rule GB13 that look at even-odd cases.
        // sot   (RI RI)*  RI x RI
        // [^RI] (RI RI)*  RI x RI
        //
        // If we have font information, we have already broken the cluster if and only if the second
        // character had no advance, which means a ligature was formed. If we don't, we look back like
        // UAX #29 recommends, but only up to 1000 code units.
        if (p1 == REGIONAL_INDICATOR && p2 == REGIONAL_INDICATOR) {
            if (advances != null) {
                // We have advances information. But if we are here, we already know c2 has no advance.
                // So we should definitely disallow a break.
                return false;
            } else {
                // Look at up to 1000 code units.
                final int backBarrier = Math.max(start, offsetBack - 1000);
                int offsetBack1 = offsetBack;
                while (offsetBack1 > backBarrier) {
                    int c0 = _c1 = buf[--offsetBack1];
                    if (Character.isLowSurrogate(_c1))
                        if (offsetBack1 > backBarrier &&
                                Character.isHighSurrogate(_c2 = buf[offsetBack1 - 1])) {
                            --offsetBack1;
                            c0 = Character.toCodePoint(_c2, _c1);
                        }
                    if (tailoredGraphemeClusterBreak(c0) != REGIONAL_INDICATOR) {
                        offsetBack1 += Character.charCount(c0);
                        break;
                    }
                }
                // The number 4 comes from the number of code units in a whole flag.
                return (offset - offsetBack1) % 4 == 0;
            }
        }
        // Cluster Indic syllables together (tailoring of UAX #29).
        // Immediately after each virama (that is not just a pure killer) followed by a letter, we
        // disallow grapheme breaks (if we are here, we don't know about advances, or we already know
        // that c2 has no advance). Or Rule GB999, Any ÷ Any
        return UCharacter.getIntPropertyValue(c1, UProperty.CANONICAL_COMBINING_CLASS) != 9  // virama
                || isPureKiller(c1) ||
                UCharacter.getIntPropertyValue(c2, UProperty.GENERAL_CATEGORY) != UCharacterCategory.OTHER_LETTER;
    }

    // Returns true for all characters whose IndicSyllabicCategory is Pure_Killer.
    // From http://www.unicode.org/Public/9.0.0/ucd/IndicSyllabicCategory.txt
    public static boolean isPureKiller(int c) {
        return (c == 0x0E3A || c == 0x0E4E || c == 0x0F84 || c == 0x103A || c == 0x1714 ||
                c == 0x1734 || c == 0x17D1 || c == 0x1BAA || c == 0x1BF2 || c == 0x1BF3 ||
                c == 0xA806 || c == 0xA953 || c == 0xABED || c == 0x11134 || c == 0x112EA ||
                c == 0x1172B);
    }

    public static int tailoredGraphemeClusterBreak(int c) {
        // Characters defined as Control that we want to treat them as Extend.
        // These are curated manually.
        if (c == 0x00AD                      // SHY
                || c == 0x061C                   // ALM
                || c == 0x180E                   // MONGOLIAN VOWEL SEPARATOR
                || c == 0x200B                   // ZWSP
                || c == 0x200E                   // LRM
                || c == 0x200F                   // RLM
                || (0x202A <= c && c <= 0x202E)  // LRE, RLE, PDF, LRO, RLO
                || ((c | 0xF) == 0x206F)         // WJ, invisible math operators, LRI, RLI, FSI, PDI,
                // and the deprecated invisible format controls
                || c == 0xFEFF                   // BOM
                || ((c | 0x7F) == 0xE007F))      // recently undeprecated tag characters in Plane 14
            return EXTEND;
            // THAI CHARACTER SARA AM is treated as a normal letter by most other implementations: they
            // allow a grapheme break before it.
        else if (c == 0x0E33)
            return OTHER;
        else
            return UCharacter.getIntPropertyValue(c, UProperty.GRAPHEME_CLUSTER_BREAK);
    }
}
