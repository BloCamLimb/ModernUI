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

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.function.Function;

public class FontFamily {

    public static final FontFamily SANS_SERIF;
    public static final FontFamily SERIF;
    public static final FontFamily MONOSPACED;

    private static final Map<String, FontFamily> sSystemFontMap;
    private static final Map<String, String> sSystemFontAliases;

    @UnmodifiableView
    private static final Map<String, FontFamily> sSystemFontMapView;
    @UnmodifiableView
    private static final Map<String, String> sSystemFontAliasesView;

    static {
        // Use Java's logical font as the default initial font if user does not override it in some configuration files
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();

        Map<String, FontFamily> map = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();

        Locale defaultLocale = Locale.getDefault();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames(Locale.ROOT)) {
            if (!map.containsKey(name)) {
                FontFamily family = new FontFamily(new Font(name, Font.PLAIN, 1));
                map.put(name, family);
                String alias = family.getFamilyName(defaultLocale);
                if (!name.equals(alias)) {
                    aliases.put(alias, name);
                }
            }
        }
        Function<String, FontFamily> mapping = name ->
                new FontFamily(new Font(name, Font.PLAIN, 1));
        SANS_SERIF = map.computeIfAbsent(Font.SANS_SERIF, mapping);
        SERIF = map.computeIfAbsent(Font.SERIF, mapping);
        MONOSPACED = map.computeIfAbsent(Font.MONOSPACED, mapping);

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
            Font[] fonts = Font.createFonts(file);
            return createFamily(fonts, register);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static FontFamily createFamily(@NonNull InputStream stream, boolean register) {
        try {
            Font[] fonts = Font.createFonts(stream);
            return createFamily(fonts, register);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static FontFamily createFamily(@NonNull Font[] fonts, boolean register) {
        FontFamily family = new FontFamily(fonts[0]);
        if (register) {
            String name = family.getFamilyName();
            String alias = family.getFamilyName(Locale.getDefault());
            sSystemFontMap.putIfAbsent(name, family);
            if (!name.equals(alias)) {
                sSystemFontAliases.putIfAbsent(alias, name);
            }
            for (Font font : fonts) {
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont(font);
            }
        }
        return family;
    }

    // root name
    private final String mFamilyName;

    private Font mFont;
    private Font mBold;
    private Font mItalic;
    private Font mBoldItalic;

    @ApiStatus.Internal
    protected FontFamily(String name) {
        mFamilyName = name;
    }

    private FontFamily(@NonNull Font font) {
        this(font.getFamily(Locale.ROOT));
        mFont = font.deriveFont(Font.PLAIN);
        mBold = font.deriveFont(Font.BOLD);
        mItalic = font.deriveFont(Font.ITALIC);
        mBoldItalic = font.deriveFont(Font.BOLD | Font.ITALIC);
    }

    public Font getClosestMatch(int style) {
        return switch (style) {
            case Font.PLAIN -> mFont;
            case Font.BOLD -> mBold;
            case Font.ITALIC -> mItalic;
            case Font.BOLD | Font.ITALIC -> mBoldItalic;
            default -> null;
        };
    }

    public boolean hasGlyph(int ch) {
        return mFont.canDisplay(ch);
    }

    public boolean hasGlyph(int ch, int vs) {
        // no public API
        return mFont.canDisplay(ch);
    }

    public String getFamilyName() {
        return mFamilyName;
    }

    public String getFamilyName(Locale locale) {
        return mFont.getFamily(locale);
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
        return "FontFamily{" + mFamilyName + '}';
    }
}
