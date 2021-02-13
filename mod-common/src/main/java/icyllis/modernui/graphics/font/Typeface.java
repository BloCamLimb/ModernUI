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
import icyllis.modernui.ModernUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;

/**
 * The Typeface specifies the topmost layer object that can be used
 * in Paint, which contain a set of font families. This determines
 * how text appears when drawn and measured.
 */
public class Typeface {

    @Nonnull
    public static final Typeface SANS_SERIF;
    @Nonnull
    public static final Typeface SERIF;
    @Nonnull
    public static final Typeface MONOSPACED;

    @Nullable
    static final Font sBuiltInFont;
    @Nonnull
    static final Font sSansSerifFont;

    static boolean sJavaTooOld;

    private static final List<String> sFontFamilyNames;
    static final List<Font> sAllFontFamilies = new ArrayList<>();
    static final Map<String, Typeface> sSystemFontMap = new HashMap<>();

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
            try (InputStream stream = Typeface.class.getResourceAsStream("/assets/modernui/font/biliw.otf")) {
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
            Typeface typeface = new Typeface(fontCollection);
            String family = fontFamily.getFamily(Locale.ROOT);
            typeface = sSystemFontMap.put(family, typeface);
            if (typeface != null) {
                ModernUI.LOGGER.warn(GlyphManager.MARKER, "Duplicated font family: {}", family);
            }
        }

        SANS_SERIF = sSystemFontMap.get(Font.SANS_SERIF);
        SERIF = sSystemFontMap.get(Font.SERIF);
        MONOSPACED = sSystemFontMap.get(Font.MONOSPACED);
        Preconditions.checkState(SANS_SERIF != null, "Sans Serif font is missing");
        Preconditions.checkState(SERIF != null, "Serif font is missing");
        Preconditions.checkState(MONOSPACED != null, "Monospaced font is missing");
    }

    private final FontCollection mFontCollection;

    private Typeface(FontCollection fontCollection) {
        mFontCollection = fontCollection;
    }

    // internal use
    public FontCollection getFontCollection() {
        return mFontCollection;
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
                        Typeface.sJavaTooOld = true;
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

    public static Typeface getSystemTypeface(@Nonnull String familyName) {
        Typeface t = sSystemFontMap.get(familyName);
        return t == null ? Typeface.SANS_SERIF : t;
    }
}
