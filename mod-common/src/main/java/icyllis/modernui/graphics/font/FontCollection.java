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

package icyllis.modernui.graphics.font;

import com.google.common.base.Preconditions;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import icyllis.modernui.ModernUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;

/**
 * The FontCollection specifies a set of font families that can be used
 * in Paint. This determines how text appears when drawn and measured.
 */
public class FontCollection {

    @Nonnull
    public static final FontCollection SANS_SERIF;
    @Nonnull
    public static final FontCollection SERIF;
    @Nonnull
    public static final FontCollection MONOSPACED;

    @Nullable
    static final Font sBuiltInFont;
    @Nonnull
    static final Font sSansSerifFont;

    static boolean sJavaTooOld;

    private static final List<String> sFontFamilyNames;
    static final List<Font> sAllFontFamilies = new ArrayList<>();
    static final Map<String, FontCollection> sSystemFontMap = new HashMap<>();

    static {
        checkJava();
        /* Use Java's logical font as the default initial font if user does not override it in some configuration file */
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
        Font sansSerif = null;
        for (String family : families) {
            Font font = new Font(family, Font.PLAIN, 1);
            sAllFontFamilies.add(font);
            if (family.equals(Font.SANS_SERIF)) {
                sansSerif = font;
            }
        }
        Preconditions.checkState(sansSerif != null, "Sans Serif font is missing");
        sSansSerifFont = sansSerif;

        sFontFamilyNames = Arrays.asList(families);

        Font builtIn = null;
        if (!sJavaTooOld) {
            try (InputStream stream = FontCollection.class.getResourceAsStream("/assets/modernui/font/biliw.otf")) {
                builtIn = Font.createFont(Font.TRUETYPE_FONT, stream);
                sAllFontFamilies.add(builtIn);
            } catch (FontFormatException | IOException e) {
                ModernUI.LOGGER.warn(GlyphManager.MARKER, "Built-in font failed to load", e);
            } catch (NullPointerException e) {
                ModernUI.LOGGER.warn(GlyphManager.MARKER, "Built-in font was missing", e);
            }
        }
        sBuiltInFont = builtIn;

        for (Font fontFamily : sAllFontFamilies) {
            FontCollection fontCollection = new FontCollection(new Font[]{fontFamily});
            String family = fontFamily.getFamily(Locale.ROOT);
            fontCollection = sSystemFontMap.putIfAbsent(family, fontCollection);
            if (fontCollection != null) {
                ModernUI.LOGGER.error(GlyphManager.MARKER, "Duplicated font family: {}", family);
            }
        }

        SANS_SERIF = sSystemFontMap.get(Font.SANS_SERIF);
        SERIF = sSystemFontMap.get(Font.SERIF);
        MONOSPACED = sSystemFontMap.get(Font.MONOSPACED);
        Preconditions.checkState(SANS_SERIF != null, "Sans Serif font is missing");
        Preconditions.checkState(SERIF != null, "Serif font is missing");
        Preconditions.checkState(MONOSPACED != null, "Monospaced font is missing");
    }

    private static void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "Java version is missing");
        } else {
            try {
                int majorNumber = Integer.parseInt(javaVersion.split("\\.")[0]);
                if (majorNumber < 11) {
                    ModernUI.get().warnSetup("warning.modernui.old_java", "11.0.9", javaVersion);
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                ModernUI.LOGGER.warn(GlyphManager.MARKER, "Failed to check major java version: {}", javaVersion, e);
            }
            if (javaVersion.startsWith("1.8")) {
                try {
                    int update = Integer.parseInt(javaVersion.split("_")[1].split("-")[0]);
                    if (update < 201) {
                        sJavaTooOld = true;
                        ModernUI.LOGGER.warn(GlyphManager.MARKER, "Java {} is too old to use external fonts", javaVersion);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    ModernUI.LOGGER.warn(ModernUI.MARKER, "Failed to check java version update: {}", javaVersion, e);
                }
            }
        }
    }

    public static List<String> getFontFamilyNames() {
        return sFontFamilyNames;
    }

    public static FontCollection getSystemFont(@Nonnull String familyName) {
        FontCollection t = sSystemFontMap.get(familyName);
        return t == null ? SANS_SERIF : t;
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

    public static boolean isVariationSelector(int c) {
        return (c >= 0xE0100 && c <= 0xE01FF) || (c >= 0xFE00 && c <= 0xFE0F);
    }

    private final Font[] mFonts;

    FontCollection(Font[] fonts) {
        mFonts = fonts;
    }

    @Nonnull
    List<Run> itemize(@Nonnull char[] text, @Nonnull Locale locale) {
        if (text.length == 0)
            return Collections.emptyList();
        return null;
    }

    // font run, child of style run
    public static class Run {

    }
}
