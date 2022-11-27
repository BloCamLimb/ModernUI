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

import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.util.*;
import java.util.function.Function;

public class FontFamily {

    private static final Map<String, FontFamily> sSystemFontMap;

    static {
        // Use Java's logical font as the default initial font if user does not override it in some configuration files
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();

        Map<String, FontFamily> map = new HashMap<>();
        Function<String, FontFamily> mapping = s -> new FontFamily(new Font(s, Font.PLAIN, 1));

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
        for (String family : families) {
            map.computeIfAbsent(family, mapping);
        }
        map.computeIfAbsent(Font.SANS_SERIF, mapping);
        map.computeIfAbsent(Font.SERIF, mapping);
        map.computeIfAbsent(Font.MONOSPACED, mapping);

        sSystemFontMap = Collections.unmodifiableMap(map);
    }

    @UnmodifiableView
    public static Map<String, FontFamily> getSystemFontMap() {
        return sSystemFontMap;
    }

    private Font mPlain;
    private Font mBold;
    private Font mItalic;
    private Font mBoldItalic;

    public FontFamily(Font font) {
        if (font != null) {
            mPlain = font.deriveFont(Font.PLAIN);
            mBold = font.deriveFont(Font.BOLD);
            mItalic = font.deriveFont(Font.ITALIC);
            mBoldItalic = font.deriveFont(Font.BOLD | Font.ITALIC);
        }
    }

    public Font getClosestMatch(int style) {
        return switch (style) {
            case Font.PLAIN -> mPlain;
            case Font.BOLD -> mBold;
            case Font.ITALIC -> mItalic;
            case Font.BOLD | Font.ITALIC -> mBoldItalic;
            default -> null;
        };
    }

    public boolean hasGlyph(int codePoint) {
        return mPlain.canDisplay(codePoint);
    }

    public boolean hasGlyph(int codePoint, int variationSelector) {
        // no public API
        return hasGlyph(codePoint);
    }

    public String getFamilyName() {
        return mPlain.getFamily(Locale.ROOT);
    }

    @Override
    public int hashCode() {
        return mPlain != null ? mPlain.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FontFamily that = (FontFamily) o;
        return Objects.equals(mPlain, that.mPlain);
    }
}
