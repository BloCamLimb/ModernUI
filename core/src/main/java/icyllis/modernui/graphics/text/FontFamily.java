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
            var fonts = java.awt.Font.createFonts(file);
            return createFamily(fonts, register);
        } catch (java.awt.FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static FontFamily createFamily(@NonNull InputStream stream, boolean register) {
        try {
            var fonts = java.awt.Font.createFonts(stream);
            return createFamily(fonts, register);
        } catch (java.awt.FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static FontFamily createFamily(@NonNull java.awt.Font[] fonts, boolean register) {
        FontFamily family = new FontFamily(fonts[0]);
        if (register) {
            String name = family.getFamilyName();
            sSystemFontMap.putIfAbsent(name, family);
            String alias = family.getFamilyName(Locale.getDefault());
            if (!name.equals(alias)) {
                sSystemFontAliases.putIfAbsent(alias, name);
            }
            for (var font : fonts) {
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont(font);
            }
        }
        return family;
    }

    private final Font mFont;
    private Font mBold;
    private Font mItalic;
    private Font mBoldItalic;

    private final boolean mIsColorEmoji;

    public FontFamily(Font font) {
        mFont = Objects.requireNonNull(font);
        mIsColorEmoji = font instanceof EmojiFont;
    }

    private FontFamily(@NonNull java.awt.Font font) {
        mFont = new StandardFont(font);
        mBold = new StandardFont(font.deriveFont(java.awt.Font.BOLD));
        mItalic = new StandardFont(font.deriveFont(java.awt.Font.ITALIC));
        mBoldItalic = new StandardFont(font.deriveFont(java.awt.Font.BOLD | java.awt.Font.ITALIC));
        mIsColorEmoji = false;
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
                '}';
    }
}
