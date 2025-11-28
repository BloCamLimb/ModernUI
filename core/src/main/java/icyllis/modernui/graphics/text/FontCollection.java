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

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

@ApiStatus.Internal
public class FontCollection {

    // 0b0000 0000 0000 0000 0000 0001 1100 0000
    public static final int GC_M_MASK =
            UCharacterProperty.getMask(UCharacterCategory.COMBINING_SPACING_MARK) |
                    UCharacterProperty.getMask(UCharacterCategory.ENCLOSING_MARK) |
                    UCharacterProperty.getMask(UCharacterCategory.NON_SPACING_MARK);

    // Characters where we want to continue using existing font run for (or stick to the next run if
    // they start a string), even if the font does not support them explicitly. These are handled
    // properly by HarfBuzz (JDK11+) even if the font does not explicitly support them and it's
    // usually meaningless to switch to a different font to display them.
    public static boolean doesNotNeedFontSupport(int c) {
        return c == 0x00AD                       // SOFT HYPHEN
                || c == 0x034F                   // COMBINING GRAPHEME JOINER
                || c == 0x061C                   // ARABIC LETTER MARK
                || (0x200C <= c && c <= 0x200F)  // ZERO WIDTH NON-JOINER..RIGHT-TO-LEFT MARK
                || (0x202A <= c && c <= 0x202E)  // LEFT-TO-RIGHT EMBEDDING..RIGHT-TO-LEFT OVERRIDE
                || (0x2066 <= c && c <= 0x2069)  // LEFT-TO-RIGHT ISOLATE..POP DIRECTIONAL ISOLATE
                || c == 0xFEFF                   // BYTE ORDER MARK
                || isVariationSelector(c);
    }

    public static final int REPLACEMENT_CHARACTER = 0xFFFD;

    // Characters where we want to continue using existing font run instead of
    // recomputing the best match in the fallback list.
    public static boolean isStickyWhitelisted(int c) {
        return switch (c) {
            case '!', ',', '-', '.', ':', ';', '?',
                    0x00A0,  // NBSP
                    0x2010,  // HYPHEN
                    0x2011,  // NB_HYPHEN
                    0x202F,  // NNBSP
                    0x2640,  // FEMALE_SIGN,
                    0x2642,  // MALE_SIGN,
                    0x2695   // STAFF_OF_AESCULAPIUS
                    -> true;
            default -> false;
        };
    }

    public static boolean isCombining(int c) {
        return (UCharacterProperty.getMask(UCharacter.getType(c)) & GC_M_MASK) != 0;
    }

    // Returns true if the given code point is a variation selector.
    public static boolean isVariationSelector(int c) {
        return UCharacter.hasBinaryProperty(c, UProperty.VARIATION_SELECTOR);
    }

    public static boolean isEmojiBreak(int prevCh, int ch) {
        return !(Emoji.isEmojiModifier(ch) ||
                (Emoji.isRegionalIndicatorSymbol(prevCh) && Emoji.isRegionalIndicatorSymbol(ch)) ||
                ch == Emoji.COMBINING_ENCLOSING_KEYCAP ||
                Emoji.isTagSpecChar(ch) ||
                ch == Emoji.ZERO_WIDTH_JOINER ||
                prevCh == Emoji.ZERO_WIDTH_JOINER);
    }

    // an array of base fonts
    @NonNull
    private final List<FontFamily> mFamilies;
    private final BitSet mExclusiveEastAsianBits;

    public FontCollection(@NonNull FontFamily... families) {
        this(families, null);
    }

    /**
     * <var>exclusiveEastAsianBits</var> determines which families in the <var>families</var>
     * array will be used as exclusive East Asian families. This means for non East Asian text,
     * such families will be skipped.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public FontCollection(@NonNull FontFamily[] families, @Nullable BitSet exclusiveEastAsianBits) {
        if (families.length == 0) {
            throw new IllegalArgumentException("families cannot be empty");
        }
        mFamilies = List.of(families); // array copy and null-check
        mExclusiveEastAsianBits = exclusiveEastAsianBits;
    }

    /**
     * Perform the itemization.
     */
    @NonNull
    public List<Run> itemize(@NonNull char[] text, int offset, int limit) {
        return itemize(text, offset, limit, limit - offset);
    }

    /**
     * Perform the itemization.
     */
    @NonNull
    public List<Run> itemize(@NonNull char[] text, int offset, int limit, int runLimit) {
        if (offset < 0 || offset > limit || limit > text.length || runLimit < 0) {
            throw new IllegalArgumentException();
        }
        if (offset == limit) {
            return Collections.emptyList();
        }

        final List<Run> result = new ArrayList<>();

        Run lastRun = null;
        List<FontFamily> lastFamilies = null;

        int nextCh;
        int prevCh = 0;
        int next = offset;
        int index = offset;

        char _c1 = text[index];
        char _c2;
        if (Character.isHighSurrogate(_c1) && index + 1 < limit) {
            _c2 = text[index + 1];
            if (Character.isLowSurrogate(_c2)) {
                nextCh = Character.toCodePoint(_c1, _c2);
                ++index;
            } else {
                nextCh = REPLACEMENT_CHARACTER;
            }
        } else if (Character.isSurrogate(_c1)) {
            nextCh = REPLACEMENT_CHARACTER;
        } else {
            nextCh = _c1;
        }
        ++index;

        boolean running = true;
        do {
            int ch = nextCh;
            int pos = next;
            next = index;

            if (index < limit) {
                _c1 = text[index];
                if (Character.isHighSurrogate(_c1) && index + 1 < limit) {
                    _c2 = text[index + 1];
                    if (Character.isLowSurrogate(_c2)) {
                        nextCh = Character.toCodePoint(_c1, _c2);
                        ++index;
                    } else {
                        nextCh = REPLACEMENT_CHARACTER;
                    }
                } else if (Character.isSurrogate(_c1)) {
                    nextCh = REPLACEMENT_CHARACTER;
                } else {
                    nextCh = _c1;
                }
                ++index;
            } else {
                running = false;
            }

            boolean shouldContinueRun = false;
            if (doesNotNeedFontSupport(ch)) {
                // Always continue if the character is a format character not needed to be in the font.
                shouldContinueRun = true;
            } else if (lastFamilies != null && !lastFamilies.isEmpty() &&
                    (isStickyWhitelisted(ch) || isCombining(ch))) {
                // Continue using existing font as long as it has coverage and is whitelisted.

                if (lastFamilies.get(0).isColorEmojiFamily()) {
                    // If the last family is color emoji font, find the longest family.
                    for (FontFamily family : lastFamilies) {
                        shouldContinueRun |= family.hasGlyph(ch);
                    }
                } else {
                    shouldContinueRun = lastFamilies.get(0).hasGlyph(ch);
                }
            }

            if (!shouldContinueRun) {
                List<FontFamily> families = getFamilyForChar(ch,
                        running && isVariationSelector(nextCh) ? nextCh : 0);
                final boolean breakRun;
                if (pos == 0 || lastFamilies == null || lastFamilies.isEmpty()) {
                    breakRun = true;
                } else {
                    if (lastFamilies.get(0).isColorEmojiFamily()) {
                        List<FontFamily> intersection = new ArrayList<>(families);
                        intersection.retainAll(lastFamilies);
                        if (intersection.isEmpty()) {
                            breakRun = true; // None of last family can draw the given char.
                        } else {
                            breakRun = isEmojiBreak(prevCh, ch);
                            if (!breakRun) {
                                // To select sequence supported families, update family indices with the
                                // intersection between the supported families between prev char and
                                // current char.
                                families = intersection;
                                lastFamilies = intersection;
                                lastRun.families = intersection;
                            }
                        }
                    } else {
                        breakRun = families.get(0) != lastFamilies.get(0);
                    }
                }

                if (breakRun) {
                    int start = pos;
                    // Workaround for combining marks and emoji modifiers until we implement
                    // per-cluster font selection: if a combining mark or an emoji modifier is found in
                    // a different font that also supports the previous character, attach previous
                    // character to the new run. U+20E3 COMBINING ENCLOSING KEYCAP, used in emoji, is
                    // handled properly by this since it's a combining mark too.
                    if (pos != 0 &&
                            (isCombining(ch) || (Emoji.isEmojiModifier(ch) && Emoji.isEmojiModifierBase(prevCh)))) {
                        for (FontFamily family : families) {
                            if (family.hasGlyph(prevCh)) {
                                int prevLength = Character.charCount(prevCh);
                                if (lastRun != null) {
                                    lastRun.limit -= prevLength;
                                    if (lastRun.start == lastRun.limit) {
                                        result.remove(lastRun);
                                    }
                                }
                                start -= prevLength;
                                break;
                            }
                        }
                    }
                    if (lastFamilies == null || lastFamilies.isEmpty()) {
                        // This is the first family ever assigned. We are either seeing the very first
                        // character (which means start would already be zero), or we have only seen
                        // characters that don't need any font support (which means we need to adjust
                        // start to be 0 to include those characters).
                        start = offset;
                    }
                    Run run = new Run(families, start, 0);
                    result.add(run);
                    lastRun = run;
                    lastFamilies = families;
                }
            }
            prevCh = ch;
            if (lastRun != null) {
                lastRun.limit = next;
            }

            // Stop searching the remaining characters if the result length gets runMax + 2.
            // When result.size gets runMax + 2 here, the run between [0, runMax) was finalized.
            // If the result.size() equals to runMax, the run may be still expanding.
            // if the result.size() equals to runMax + 2, the last run may be removed and the last run
            // may be extended the previous run with above workaround.
            if (result.size() >= 2 && runLimit == result.size() - 2) {
                break;
            }
        } while (running);

        if (lastFamilies == null || lastFamilies.isEmpty()) {
            // No character needed any font support, so it doesn't really matter which font they end up
            // getting displayed in. We put the whole string in one run, using the first font.
            result.add(new Run(Collections.singletonList(mFamilies.get(0)), offset, limit));
        }
        return result;
    }

    // base fonts
    @NonNull
    @Unmodifiable
    public List<FontFamily> getFamilies() {
        return mFamilies;
    }

    public static final int UNSUPPORTED_FONT_SCORE = 0;

    private int calcCoverageScore(int ch, int vs, @NonNull FontFamily family,
                                  boolean isExclusiveEastAsianFamily) {
        if (isExclusiveEastAsianFamily) {
            int script = UScript.getScript(ch);
            if (script > UScript.INHERITED) {
                // East Asian scripts
                switch (script) {
                    case UScript.HAN, UScript.BOPOMOFO, UScript.HIRAGANA, UScript.KATAKANA,
                            UScript.HANGUL, UScript.YI, UScript.NUSHU, UScript.LISU,
                            UScript.MIAO, UScript.TANGUT, UScript.KHITAN_SMALL_SCRIPT:
                        break;
                    default:
                        // skip the family for non East Asian scripts
                        return UNSUPPORTED_FONT_SCORE;
                }
            }
        }
        if (!family.hasGlyph(ch, vs)) {
            return UNSUPPORTED_FONT_SCORE;
        }
        boolean colorEmojiRequest;
        switch (vs) {
            case Emoji.VARIATION_SELECTOR_16 -> colorEmojiRequest = true;
            case Emoji.VARIATION_SELECTOR_15 -> colorEmojiRequest = false;
            default -> {
                return 1;
            }
        }
        return colorEmojiRequest == family.isColorEmojiFamily() ? 2 : 1;
    }

    @NonNull
    private List<FontFamily> getFamilyForChar(int ch, int vs) {
        List<FontFamily> families = null;
        int bestScore = UNSUPPORTED_FONT_SCORE;
        for (int i = 0, e = mFamilies.size(); i < e; i++) {
            FontFamily family = mFamilies.get(i);
            int score = calcCoverageScore(ch, vs, family,
                    mExclusiveEastAsianBits != null && mExclusiveEastAsianBits.get(i));
            if (score != UNSUPPORTED_FONT_SCORE && score >= bestScore) {
                if (families == null) {
                    families = new ArrayList<>(2);
                }
                if (score > bestScore) {
                    families.clear();
                    bestScore = score;
                }
                if (families.size() < 2) {
                    families.add(family);
                }
            }
        }
        if (families != null &&
                !families.get(0).isColorEmojiFamily()) {
            return families;
        }
        for (FontFamily family : FontFamily.getSystemFontMap().values()) {
            int score = calcCoverageScore(ch, vs, family, false);
            if (score != UNSUPPORTED_FONT_SCORE && score >= bestScore) {
                if (families == null) {
                    families = new ArrayList<>(8);
                }
                if (score > bestScore) {
                    families.clear();
                    bestScore = score;
                }
                if (families.size() < 8) {
                    families.add(family);
                }
            }
        }
        if (families != null) {
            return families;
        }
        return Collections.singletonList(mFamilies.get(0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontCollection that = (FontCollection) o;

        return mFamilies.equals(that.mFamilies);
    }

    @Override
    public int hashCode() {
        return mFamilies.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("FontCollection");
        s.append('{').append("mFamilies").append('=').append('[');
        for (int i = 0, e = mFamilies.size(); i < e; i++) {
            if (i > 0) {
                s.append(',').append(' ');
            }
            s.append(mFamilies.get(i).getFamilyName());
        }
        return s.append(']').append(',').append(' ')
                .append("mExclusiveEastAsianBits").append('=')
                .append(mExclusiveEastAsianBits)
                .append('}').toString();
    }

    public static final class Run {

        List<FontFamily> families;
        int start;
        int limit;

        Run(List<FontFamily> families, int start, int limit) {
            this.families = families;
            this.start = start;
            this.limit = limit;
        }

        public Font getBestFont(char[] text, int style) {
            int bestIndex = 0;
            int bestScore = 0;

            if (families.get(0).isColorEmojiFamily() && families.size() > 1) {
                for (int i = 0; i < families.size(); i++) {
                    Font font = families.get(i).getClosestMatch(FontPaint.NORMAL);
                    int score = font.calcGlyphScore(text,
                            start, limit);
                    if (score > bestScore) {
                        bestIndex = i;
                        bestScore = score;
                    }
                }
            }

            return families.get(bestIndex).getClosestMatch(style);
        }

        // start index (inclusive)
        public int start() {
            return start;
        }

        // limit index (exclusive)
        public int limit() {
            return limit;
        }
    }
}
