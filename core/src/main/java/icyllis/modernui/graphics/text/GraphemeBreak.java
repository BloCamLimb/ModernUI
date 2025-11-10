/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.CharacterIterator;
import java.util.Locale;

import static com.ibm.icu.lang.UCharacter.GraphemeClusterBreak;

/**
 * This class handles grapheme cluster break.
 * <p>
 * It is important to recognize that what the user thinks of as a
 * &quot;character&quot; (a basic unit of a writing system for a language)
 * may not be just a single Unicode code point. Instead, that basic
 * unit may be made up of multiple Unicode code points. To avoid
 * ambiguity with the computer use of the term character, this is
 * called a user-perceived character. For example, &quot;G&quot; + grave-accent
 * is a user-perceived character: users think of it as a single
 * character, yet is actually represented by two Unicode code points.
 * These user-perceived characters are approximated by what is called
 * a grapheme cluster, which can be determined programmatically.
 * <p>
 * This class provides a tailored version of extended grapheme clusters.
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
     * Config value, true to use ICU4J for testing, false to use this tailored version.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static boolean sUseICU = false;

    private GraphemeBreak() {
    }

    public static int getTextRunCursorICU(@Nonnull CharacterIterator text, @Nonnull Locale locale,
                                          int offset, int op) {
        final int original = offset;
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
        return offset == BreakIterator.DONE ? original : offset;
    }

    public static int getTextRunCursorImpl(@Nullable float[] advances, @Nonnull CharSequence buf, int start, int count,
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
    public static boolean isGraphemeBreak(@Nullable float[] advances, @Nonnull CharSequence buf, int start, int count,
                                          final int offset) {
        // This implementation closely follows Unicode Standard Annex #29 on
        // Unicode Text Segmentation (http://www.unicode.org/reports/tr29/),
        // implementing a tailored version of extended grapheme clusters.
        // The GB rules refer to section 3.1.1, Grapheme Cluster Boundary Rules.

        // Rule GB1, sot ÷; Rule GB2, ÷ eot
        if (offset <= start || offset >= start + count) {
            return true;
        }
        if (Character.isLowSurrogate(buf.charAt(offset))) {
            // Don't break a surrogate pair, but a lonely trailing surrogate pair is a break
            return !Character.isHighSurrogate(buf.charAt(offset - 1));
        }
        int c1;
        int c2;
        int offset_back = offset;
        // Forward once, and back

        char _c1;
        char _c2;

        // NEXT
        c2 = _c1 = buf.charAt(offset_back++);
        if (Character.isHighSurrogate(_c1)) {
            if (offset_back != start + count &&
                    Character.isLowSurrogate(_c2 = buf.charAt(offset_back))) {
                ++offset_back;
                c2 = Character.toCodePoint(_c1, _c2);
            }
        }

        offset_back = offset;

        // PREV
        c1 = _c1 = buf.charAt(--offset_back);
        if (Character.isLowSurrogate(_c1)) {
            if (offset_back > start &&
                    Character.isHighSurrogate(_c2 = buf.charAt(offset_back - 1))) {
                --offset_back;
                c1 = Character.toCodePoint(_c2, _c1);
            }
        }

        int p1 = tailoredGraphemeClusterBreak(c1);
        int p2 = tailoredGraphemeClusterBreak(c2);

        // Rule GB3, CR x LF
        if (p1 == GraphemeClusterBreak.CR && p2 == GraphemeClusterBreak.LF) {
            return false;
        }
        // Rule GB4, (Control | CR | LF) ÷
        if (p1 == GraphemeClusterBreak.CONTROL || p1 == GraphemeClusterBreak.CR || p1 == GraphemeClusterBreak.LF) {
            return true;
        }
        // Rule GB5, ÷ (Control | CR | LF)
        if (p2 == GraphemeClusterBreak.CONTROL || p2 == GraphemeClusterBreak.CR || p2 == GraphemeClusterBreak.LF) {
            return true;
        }
        // Rule GB6, L x ( L | V | LV | LVT )
        if (p1 == GraphemeClusterBreak.L && (p2 == GraphemeClusterBreak.L || p2 == GraphemeClusterBreak.V || p2 == GraphemeClusterBreak.LV || p2 == GraphemeClusterBreak.LVT)) {
            return false;
        }
        // Rule GB7, ( LV | V ) x ( V | T )
        if ((p1 == GraphemeClusterBreak.LV || p1 == GraphemeClusterBreak.V) && (p2 == GraphemeClusterBreak.V || p2 == GraphemeClusterBreak.T)) {
            return false;
        }
        // Rule GB8, ( LVT | T ) x T
        if ((p1 == GraphemeClusterBreak.LVT || p1 == GraphemeClusterBreak.T) && p2 == GraphemeClusterBreak.T) {
            return false;
        }

        // This is used to decide font-dependent grapheme clusters. If we don't have the advance
        // information, we become conservative in grapheme breaking and assume that it has no advance.
        boolean c2_has_advance = (advances != null && advances[offset - start] != 0.0F);

        // All the following rules are font-dependent, in the way that if we know c2 has an advance,
        // we definitely know that it cannot form a grapheme with the character(s) before it. So we
        // make the decision in favor a grapheme break early.
        if (c2_has_advance) {
            return true;
        }

        // Rule GB9, x (Extend | ZWJ); Rule GB9a, x SpacingMark; Rule GB9b, Prepend x
        if (p2 == GraphemeClusterBreak.EXTEND || p2 == GraphemeClusterBreak.ZWJ || p2 == GraphemeClusterBreak.SPACING_MARK || p1 == GraphemeClusterBreak.PREPEND) {
            return false;
        }

        // Tailored version of Rule GB11
        // \p{Extended_Pictographic} Extend* ZWJ x \p{Extended_Pictographic}
        if (offset_back > start && p1 == GraphemeClusterBreak.ZWJ &&
                UCharacter.hasBinaryProperty(c2, UProperty.EXTENDED_PICTOGRAPHIC)) {
            int offset_backback = offset_back;
            int p0;

            int c0 = _c1 = buf.charAt(--offset_backback);
            if (Character.isLowSurrogate(_c1)) {
                if (offset_backback > start &&
                        Character.isHighSurrogate(_c2 = buf.charAt(offset_backback - 1))) {
                    --offset_backback;
                    c0 = Character.toCodePoint(_c2, _c1);
                }
            }

            p0 = tailoredGraphemeClusterBreak(c0);
            while (p0 == GraphemeClusterBreak.EXTEND && offset_backback > start) {
                c0 = _c1 = buf.charAt(--offset_backback);
                if (Character.isLowSurrogate(_c1)) {
                    if (offset_backback > start &&
                            Character.isHighSurrogate(_c2 = buf.charAt(offset_backback - 1))) {
                        --offset_backback;
                        c0 = Character.toCodePoint(_c2, _c1);
                    }
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
        if (p1 == GraphemeClusterBreak.REGIONAL_INDICATOR && p2 == GraphemeClusterBreak.REGIONAL_INDICATOR) {
            if (advances != null) {
                // We have advances information. But if we are here, we already know c2 has no advance.
                // So we should definitely disallow a break.
                return false;
            } else {
                // Look at up to 1000 code units.
                final int lookback_barrier = Math.max(start, offset_back - 1000);
                int offset_backback = offset_back;
                while (offset_backback > lookback_barrier) {
                    int c0 = _c1 = buf.charAt(--offset_backback);
                    if (Character.isLowSurrogate(_c1)) {
                        if (offset_backback > lookback_barrier &&
                                Character.isHighSurrogate(_c2 = buf.charAt(offset_backback - 1))) {
                            --offset_backback;
                            c0 = Character.toCodePoint(_c2, _c1);
                        }
                    }
                    if (tailoredGraphemeClusterBreak(c0) != GraphemeClusterBreak.REGIONAL_INDICATOR) {
                        offset_backback += Character.charCount(c0);
                        break;
                    }
                }
                // The number 4 comes from the number of code units in a whole flag.
                return (offset - offset_backback) % 4 == 0;
            }
        }
        // Cluster Indic syllables together (tailoring of UAX #29).
        // Immediately after each virama (that is not just a pure killer) followed by a letter, we
        // disallow grapheme breaks (if we are here, we don't know about advances, or we already know
        // that c2 has no advance).
        if (UCharacter.getIntPropertyValue(c1, UProperty.CANONICAL_COMBINING_CLASS) == 9  // virama
                && !isPureKiller(c1) &&
                UCharacter.getIntPropertyValue(c2, UProperty.GENERAL_CATEGORY) != UCharacterCategory.OTHER_LETTER) {
            return false;
        }
        // Rule GB999, Any ÷ Any
        return true;
    }

    // Returns true for all characters whose IndicSyllabicCategory is Pure_Killer.
    // From http://www.unicode.org/Public/9.0.0/ucd/IndicSyllabicCategory.txt
    public static boolean isPureKiller(int c) {
        return (c == 0x0E3A || c == 0x0E4E || c == 0x0F84 || c == 0x103A || c == 0x1714 ||
                c == 0x1734 || c == 0x17D1 || c == 0x1BAA || c == 0x1BF2 || c == 0x1BF3 ||
                c == 0xA806 || c == 0xA953 || c == 0xABED || c == 0x11134 || c == 0x112EA ||
                c == 0x1172B);
    }

    // @formatter:off
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
            return GraphemeClusterBreak.EXTEND;
        // THAI CHARACTER SARA AM is treated as a normal letter by most other implementations: they
        // allow a grapheme break before it.
        else if (c == 0x0E33)
            return GraphemeClusterBreak.OTHER;
        else
            return UCharacter.getIntPropertyValue(c, UProperty.GRAPHEME_CLUSTER_BREAK);
    }
    // @formatter:on
}
