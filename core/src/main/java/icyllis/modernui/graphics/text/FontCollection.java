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

    // an array of base fonts
    @NonNull
    private final List<FontFamily> mFamilies;

    public FontCollection(@NonNull FontFamily... families) {
        if (families.length == 0) {
            throw new IllegalArgumentException("Font set cannot be empty");
        }
        // we use font indices array to save memory
        // the number of families in a FontCollection cannot exceed byte.max
        if (families.length > Byte.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        mFamilies = List.of(families);
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
        FontFamily lastFamily = null;

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
            } else if (Character.isSurrogate(_c1)) {
                nextCh = REPLACEMENT_CHARACTER;
            } else {
                nextCh = _c1;
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
                    } else if (Character.isSurrogate(_c1)) {
                        nextCh = REPLACEMENT_CHARACTER;
                    } else {
                        nextCh = _c1;
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
            } else if (lastFamily != null && (isStickyWhitelisted(ch) || isCombining(ch))) {
                // Continue using existing font as long as it has coverage and is whitelisted.
                shouldContinueRun = lastFamily.hasGlyph(ch);
            }

            if (!shouldContinueRun) {
                FontFamily family = getFamilyForChar(ch, isVariationSelector(nextCh) ? nextCh : 0);
                if (pos == 0 || family != lastFamily) {
                    int start = pos;
                    // Workaround for combining marks and emoji modifiers until we implement
                    // per-cluster font selection: if a combining mark or an emoji modifier is found in
                    // a different font that also supports the previous character, attach previous
                    // character to the new run. U+20E3 COMBINING ENCLOSING KEYCAP, used in emoji, is
                    // handled properly by this since it's a combining mark too.
                    if (pos != 0 &&
                            (isCombining(ch) || (Emoji.isEmojiModifier(ch) && Emoji.isEmojiModifierBase(prevCh)))) {
                        int prevLength = Character.charCount(prevCh);
                        if (lastRun != null) {
                            lastRun.limit -= prevLength;
                            if (lastRun.start == lastRun.limit) {
                                result.remove(lastRun);
                            }
                        }
                        start -= prevLength;
                    }
                    if (lastFamily == null) {
                        // This is the first family ever assigned. We are either seeing the very first
                        // character (which means start would already be zero), or we have only seen
                        // characters that don't need any font support (which means we need to adjust
                        // start to be 0 to include those characters).
                        start = offset;
                    }
                    Run run = new Run(family, start, 0);
                    result.add(run);
                    lastRun = run;
                    lastFamily = family;
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

        if (lastFamily == null) {
            // No character needed any font support, so it doesn't really matter which font they end up
            // getting displayed in. We put the whole string in one run, using the first font.
            result.add(new Run(mFamilies.get(0), offset, limit));
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
    public static final int FIRST_FONT_SCORE = 0xFFFFFFFF;

    private int calcCoverageScore(int ch, int vs, FontFamily family) {
        if (!family.hasGlyph(ch, vs)) {
            return UNSUPPORTED_FONT_SCORE;
        }
        if ((vs == 0) && mFamilies.get(0) == family) {
            return FIRST_FONT_SCORE;
        }
        boolean colorEmojiRequest;
        switch (vs) {
            case Emoji.VARIATION_SELECTOR_15 -> colorEmojiRequest = false;
            case Emoji.VARIATION_SELECTOR_16 -> colorEmojiRequest = true;
            default -> {
                return 1;
            }
        }
        return colorEmojiRequest == family.isColorEmojiFamily() ? 2 : 1;
    }

    private FontFamily getFamilyForChar(int ch, int vs) {
        int bestScore = UNSUPPORTED_FONT_SCORE;
        FontFamily bestFamily = null;
        for (FontFamily family : mFamilies) {
            int score = calcCoverageScore(ch, vs, family);
            if (score == FIRST_FONT_SCORE) {
                return family;
            }
            if (score != UNSUPPORTED_FONT_SCORE && score > bestScore) {
                bestScore = score;
                bestFamily = family;
            }
        }
        if (bestFamily != null) {
            return bestFamily;
        }
        { // our convention is to use sans serif first
            FontFamily family = FontFamily.SANS_SERIF;
            if (family.hasGlyph(ch, vs)) {
                return family;
            }
        }
        for (FontFamily family : FontFamily.getSystemFontMap().values()) {
            if (family.hasGlyph(ch, vs)) {
                return family;
            }
        }
        return mFamilies.get(0);
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
        StringBuilder s = new StringBuilder();
        s.append('{');
        for (int i = 0, e = mFamilies.size(); i < e; i++) {
            if (i > 0) {
                s.append(',');
            }
            s.append(mFamilies.get(i).getFamilyName());
        }
        return s.append('}').toString();
    }

    public static final class Run {

        private final FontFamily family;
        private final int start;
        private int limit;

        Run(FontFamily family, int start, int limit) {
            this.family = family;
            this.start = start;
            this.limit = limit;
        }

        // font family
        public FontFamily family() {
            return family;
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
