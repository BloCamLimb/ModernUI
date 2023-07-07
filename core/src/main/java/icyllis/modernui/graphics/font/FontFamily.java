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

import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.util.*;
import java.util.function.Function;

public class FontFamily {

    @UnmodifiableView
    private static final Map<String, FontFamily> sSystemFontMap;
    @UnmodifiableView
    private static final Map<String, String> sSystemFontAliases;

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
        map.computeIfAbsent(Font.SANS_SERIF, mapping);
        map.computeIfAbsent(Font.SERIF, mapping);
        map.computeIfAbsent(Font.MONOSPACED, mapping);

        sSystemFontMap = Collections.unmodifiableMap(map);
        sSystemFontAliases = Collections.unmodifiableMap(aliases);
    }

    @UnmodifiableView
    public static Map<String, FontFamily> getSystemFontMap() {
        return sSystemFontMap;
    }

    @UnmodifiableView
    public static Map<String, String> getSystemFontAliases() {
        return sSystemFontAliases;
    }

    @Nullable
    public static FontFamily getSystemFontWithAlias(String name) {
        return sSystemFontMap.get(sSystemFontAliases.getOrDefault(name, name));
    }

    // root name
    private final String mFamilyName;

    private Font mFont;
    private Font mBold;
    private Font mItalic;
    private Font mBoldItalic;

    @ApiStatus.Internal
    public FontFamily(String name) {
        mFamilyName = name;
    }

    @ApiStatus.Internal
    public FontFamily(Font font) {
        this(font.getFamily(Locale.ROOT));
        mFont = font;
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
