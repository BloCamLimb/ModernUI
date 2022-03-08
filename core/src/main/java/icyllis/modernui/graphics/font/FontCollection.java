/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.font;

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import icyllis.modernui.text.Emoji;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;

public class FontCollection {

    public static final FontCollection SANS_SERIF;
    public static final FontCollection SERIF;
    public static final FontCollection MONOSPACED;
    public static final FontCollection DEFAULT;

    private static final List<String> sFontFamilyNames;

    // internal use
    public static final List<Font> sAllFontFamilies = new ArrayList<>();
    public static final Map<String, FontCollection> sSystemFontMap = new HashMap<>();

    static {
        // Use Java's logical font as the default initial font if user does not override it in some configuration file
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();

        List<Font> fonts = new ArrayList<>();

        try (InputStream stream = new FileInputStream("F:/Torus Regular.otf")) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            fonts.add(font);
        } catch (FontFormatException | IOException ignored) {
        }

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
        Font sansSerif = null;
        for (String family : families) {
            Font font = new Font(family, Font.PLAIN, 1);
            sAllFontFamilies.add(font);
            if (family.equals(Font.SANS_SERIF)) {
                sansSerif = font;
            } else if (family.startsWith("Calibri") ||
                    family.startsWith("Microsoft YaHei UI") ||
                    family.startsWith("STHeiti") ||
                    family.startsWith("Segoe UI") ||
                    family.startsWith("SimHei")) {
                fonts.add(font);
            }
        }
        if (sansSerif == null) {
            sansSerif = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        }

        fonts.add(sansSerif);

        DEFAULT = new FontCollection(fonts.toArray(new Font[0]));

        sFontFamilyNames = List.of(families);

        for (Font font : sAllFontFamilies) {
            String family = font.getFamily(Locale.ROOT);
            if (family.equals(Font.SANS_SERIF)) {
                continue;
            }
            sSystemFontMap.putIfAbsent(family, new FontCollection(new Font[]{font, sansSerif}));
        }

        // no backup strategy
        SANS_SERIF = new FontCollection(new Font[]{sansSerif});
        sSystemFontMap.put(Font.SANS_SERIF, SANS_SERIF);

        FontCollection serif = sSystemFontMap.get(Font.SERIF);
        if (serif == null) {
            serif = new FontCollection(new Font[]{new Font(Font.SERIF, Font.PLAIN, 1)});
            sSystemFontMap.put(Font.SERIF, serif);
        }
        SERIF = serif;

        FontCollection monospaced = sSystemFontMap.get(Font.MONOSPACED);
        if (monospaced == null) {
            monospaced = new FontCollection(new Font[]{new Font(Font.MONOSPACED, Font.PLAIN, 1)});
            sSystemFontMap.put(Font.MONOSPACED, monospaced);
        }
        MONOSPACED = monospaced;
    }

    @Unmodifiable
    public static List<String> getFontFamilyNames() {
        return sFontFamilyNames;
    }

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
    private static final int[] sStickyWhitelist = {
            '!', ',', '-', '.', ':', ';', '?',
            0x00A0,  // NBSP
            0x2010,  // HYPHEN
            0x2011,  // NB_HYPHEN
            0x202F,  // NNBSP
            0x2640,  // FEMALE_SIGN,
            0x2642,  // MALE_SIGN,
            0x2695,  // STAFF_OF_AESCULAPIUS
    };

    public static boolean isStickyWhitelisted(int c) {
        for (int value : sStickyWhitelist)
            if (value == c)
                return true;
        return false;
    }

    public static boolean isCombining(int c) {
        return (UCharacterProperty.getMask(UCharacter.getType(c)) & GC_M_MASK) != 0;
    }

    // Returns true if the given code point is a variation selector.
    public static boolean isVariationSelector(int c) {
        return UCharacter.hasBinaryProperty(c, UProperty.VARIATION_SELECTOR);
    }

    // an array of base fonts
    @Nonnull
    private final List<Font> mFonts;

    public FontCollection(@Nonnull Font[] fonts) {
        if (fonts.length == 0) {
            throw new IllegalArgumentException("Font set cannot be empty");
        }
        mFonts = List.of(fonts);
    }

    // calculate font runs
    public List<Run> itemize(@Nonnull final char[] text, final int offset, final int limit) {
        if (offset < 0 || offset > limit || limit > text.length) {
            throw new IllegalArgumentException();
        }
        if (offset == limit) {
            return Collections.emptyList();
        }

        final List<Run> result = new ArrayList<>();

        Run lastRun = null;
        Font lastFamily = null;

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
                shouldContinueRun = lastFamily.canDisplay(ch);
            }

            if (!shouldContinueRun) {
                Font family = getFamilyForChar(ch);
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
                            lastRun.mEnd -= prevLength;
                            if (lastRun.mStart == lastRun.mEnd) {
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
                lastRun.mEnd = next;
            }

        } while (running);

        if (lastFamily == null) {
            // No character needed any font support, so it doesn't really matter which font they end up
            // getting displayed in. We put the whole string in one run, using the first font.
            result.add(new Run(mFonts.get(0), offset, limit));
        }
        return result;
    }

    // base fonts
    @Nonnull
    public List<Font> getFonts() {
        return mFonts;
    }

    // no scores
    private Font getFamilyForChar(int ch) {
        for (Font font : mFonts) {
            if (font.canDisplay(ch)) {
                return font;
            }
        }
        for (Font font : sAllFontFamilies) {
            if (font.canDisplay(ch)) {
                return font;
            }
        }
        return mFonts.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontCollection that = (FontCollection) o;

        return mFonts.equals(that.mFonts);
    }

    @Override
    public int hashCode() {
        return mFonts.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('{');
        for (int i = 0, e = mFonts.size(); i < e; i++) {
            if (i > 0) {
                s.append(',');
            }
            s.append(mFonts.get(i).getFamily(Locale.ROOT));
        }
        return s.append('}').toString();
    }

    public static class Run {

        final Font mFont;
        final int mStart;
        int mEnd;

        public Run(Font font, int start, int end) {
            mFont = font;
            mStart = start;
            mEnd = end;
        }

        // base font without style and size
        public Font getFont() {
            return mFont;
        }

        // start index (inclusive)
        public int getStart() {
            return mStart;
        }

        // end index (exclusive)
        public int getEnd() {
            return mEnd;
        }
    }
}
