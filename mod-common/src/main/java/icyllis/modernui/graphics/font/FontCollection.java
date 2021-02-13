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

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;

import java.awt.*;

/**
 * Find the best matching fonts for a text, split it to font runs
 */
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
}
