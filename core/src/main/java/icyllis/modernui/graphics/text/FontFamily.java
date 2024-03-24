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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@ApiStatus.Internal
public final class FontFamily {

    public static final FontFamily SANS_SERIF;
    public static final FontFamily SERIF;
    public static final FontFamily MONOSPACED;

    private static final ConcurrentHashMap<String, FontFamily> sSystemFontMap;
    private static final ConcurrentHashMap<String, String> sSystemFontAliases;

    @UnmodifiableView
    private static final Map<String, FontFamily> sSystemFontMapView;
    @UnmodifiableView
    private static final Map<String, String> sSystemFontAliasesView;

    // typical characters in East Asian scripts
    private static final int[] EAST_ASIAN_TEST_CHARS = {
            0x1100, 0x1101, // Hangul Jamo
            0x2E80, 0x2E81, // CJK Radicals Supplement
            0x2F00, 0x2F01, // Kangxi Radicals
            0x3000, 0x3001, // CJK Symbols and Punctuation
            0x3041, 0x3042, // Hiragana
            0x30A1, 0x30A2, // Katakana
            0x3111, 0x3112, // Bopomofo
            0x3131, 0x3132, // Hangul Compatibility Jamo
            0x3190, 0x3191, // Kanbun
            0x31A0, 0x31A1, // Bopomofo Extended
            0x31C0, 0x31C1, // CJK Strokes
            0x31F0, 0x31F1, // Katakana Phonetic Extensions
            0x3200, 0x3201, // Enclosed CJK Letters and Months
            0x3300, 0x3301, // CJK Compatibility
            0xF900, 0xF901, // CJK Compatibility Ideographs
            0x16F00, 0x16F01, // Miao
            0x17000, 0x17001, // Tangut
            0x18800, 0x18801, // Tangut Components
            0x18B00, 0x18B01, // Khitan Small Script
            0x1B000, 0x1B001, // Kana Supplement
            0x1B170, 0x1B171, // Nushu
    };

    static {
        // Use Java's logical font as the default initial font if user does not override it in some configuration files
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();

        ConcurrentHashMap<String, FontFamily> map = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();

        Locale defaultLocale = Locale.getDefault();
        Function<String, FontFamily> mapping =
                name -> new FontFamily(new java.awt.Font(name, java.awt.Font.PLAIN, 1));
        for (String name : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames(Locale.ROOT)) {
            if (!map.containsKey(name)) {
                FontFamily family = mapping.apply(name);
                map.put(name, family);
                String alias = family.getFamilyName(defaultLocale);
                if (!name.equals(alias)) {
                    aliases.put(alias, name);
                }
            }
        }
        SANS_SERIF = map.computeIfAbsent(java.awt.Font.SANS_SERIF, mapping);
        SERIF = map.computeIfAbsent(java.awt.Font.SERIF, mapping);
        MONOSPACED = map.computeIfAbsent(java.awt.Font.MONOSPACED, mapping);

        sSystemFontMap = map;
        sSystemFontAliases = aliases;
        sSystemFontMapView = Collections.unmodifiableMap(map);
        sSystemFontAliasesView = Collections.unmodifiableMap(aliases);
    }

    @UnmodifiableView
    public static Map<String, FontFamily> getSystemFontMap() {
        return sSystemFontMapView;
    }

    @UnmodifiableView
    public static Map<String, String> getSystemFontAliases() {
        return sSystemFontAliasesView;
    }

    @Nullable
    public static FontFamily getSystemFontWithAlias(String name) {
        return sSystemFontMapView.get(sSystemFontAliasesView.getOrDefault(name, name));
    }

    @NonNull
    public static FontFamily createFamily(@NonNull File file, boolean register) {
        try {
            var fonts = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file);
            return createFamily(fonts, register);
        } catch (java.awt.FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static FontFamily createFamily(@NonNull InputStream stream, boolean register) {
        try {
            var fonts = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream);
            return createFamily(fonts, register);
        } catch (java.awt.FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static FontFamily createFamily(@NonNull java.awt.Font font, boolean register) {
        FontFamily family = new FontFamily(font);
        if (register) {
            String name = family.getFamilyName();
            sSystemFontMap.putIfAbsent(name, family);
            String alias = family.getFamilyName(Locale.getDefault());
            if (!Objects.equals(name, alias)) {
                sSystemFontAliases.putIfAbsent(alias, name);
            }
            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .registerFont(font);
        }
        return family;
    }

    @NonNull
    public static FontFamily[] createFamilies(@NonNull File file, boolean register)
            throws java.awt.FontFormatException, IOException {
        var fonts = java.awt.Font.createFonts(file);
        return createFamilies(fonts, register);
    }

    @NonNull
    public static FontFamily[] createFamilies(@NonNull InputStream stream, boolean register)
            throws java.awt.FontFormatException, IOException {
        var fonts = java.awt.Font.createFonts(stream);
        return createFamilies(fonts, register);
    }

    @NonNull
    private static FontFamily[] createFamilies(@NonNull java.awt.Font[] fonts, boolean register) {
        FontFamily[] families = new FontFamily[fonts.length];
        for (int i = 0; i < fonts.length; i++) {
            families[i] = new FontFamily(fonts[i]);
        }
        if (register) {
            Locale defaultLocale = Locale.getDefault();
            for (var family : families) {
                String name = family.getFamilyName();
                sSystemFontMap.putIfAbsent(name, family);
                String alias = family.getFamilyName(defaultLocale);
                if (!Objects.equals(name, alias)) {
                    sSystemFontAliases.putIfAbsent(alias, name);
                }
            }
            var ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (var font : fonts) {
                ge.registerFont(font);
            }
        }
        return families;
    }

    private final Font mFont;
    private Font mBold;
    private Font mItalic;
    private Font mBoldItalic;

    private final boolean mIsEastAsian;
    private final boolean mIsColorEmoji;

    public FontFamily(Font font) {
        mFont = Objects.requireNonNull(font);
        if (font instanceof OutlineFont) {
            // use factory method instead
            throw new IllegalArgumentException();
        }
        mIsEastAsian = false;
        mIsColorEmoji = font instanceof EmojiFont;
    }

    private FontFamily(@NonNull java.awt.Font font) {
        mFont = new OutlineFont(font);
        mBold = new OutlineFont(font.deriveFont(java.awt.Font.BOLD));
        mItalic = new OutlineFont(font.deriveFont(java.awt.Font.ITALIC));
        mBoldItalic = new OutlineFont(font.deriveFont(java.awt.Font.BOLD | java.awt.Font.ITALIC));
        mIsEastAsian = isEastAsianFont(font);
        mIsColorEmoji = false;
    }

    /**
     * Returns true if the font is very likely to be an East Asian font.
     */
    private static boolean isEastAsianFont(java.awt.Font font) {
        for (int ch : EAST_ASIAN_TEST_CHARS) {
            if (font.canDisplay(ch)) {
                return true;
            }
        }
        // CJK Unified Ideographs Extension A
        // Yijing Hexagram Symbols
        // CJK Unified Ideographs
        // Yi Syllables
        // Yi Radicals
        // Lisu
        for (int ch = 0x3400; ch < 0xA500; ch += 256) {
            if (font.canDisplay(ch)) {
                return true;
            }
        }
        // Hangul Syllables
        // Hangul Jamo Extended-B
        for (int ch = 0xAC00; ch < 0xD800; ch += 256) {
            if (font.canDisplay(ch)) {
                return true;
            }
        }
        // Other CJK Unified Ideographs Extensions are ignored
        return false;
    }

    public Font getClosestMatch(int style) {
        return switch (style) {
            case FontPaint.NORMAL -> mFont;
            case FontPaint.BOLD -> mBold != null ? mBold : mFont;
            case FontPaint.ITALIC -> mItalic != null ? mItalic : mFont;
            case FontPaint.BOLD | FontPaint.ITALIC -> mBoldItalic != null ? mBoldItalic : mFont;
            default -> null;
        };
    }

    /**
     * Returns true if the family is very likely to be an East Asian family.
     * Even true, the family can still be used for other scripts.
     */
    public boolean isEastAsianFamily() {
        return mIsEastAsian;
    }

    public boolean isColorEmojiFamily() {
        return mIsColorEmoji;
    }

    public boolean hasGlyph(int ch) {
        return mFont.hasGlyph(ch, 0);
    }

    public boolean hasGlyph(int ch, int vs) {
        return mFont.hasGlyph(ch, vs);
    }

    public String getFamilyName() {
        return mFont.getFamilyName();
    }

    public String getFamilyName(Locale locale) {
        return mFont.getFamilyName(locale);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mFont);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FontFamily that = (FontFamily) o;
        return Objects.equals(mFont, that.mFont);
    }

    @Override
    public String toString() {
        return "FontFamily{" +
                "mFont=" + mFont +
                ", mBold=" + mBold +
                ", mItalic=" + mItalic +
                ", mBoldItalic=" + mBoldItalic +
                ", mIsEastAsian=" + mIsEastAsian +
                ", mIsColorEmoji=" + mIsColorEmoji +
                '}';
    }
}
