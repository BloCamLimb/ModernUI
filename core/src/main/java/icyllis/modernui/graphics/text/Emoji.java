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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

/**
 * A utility class for Emoji.
 */
public final class Emoji {

    public static final int ZERO_WIDTH_JOINER = 0x200D;

    public static final int COMBINING_ENCLOSING_KEYCAP = 0x20E3;

    public static final int VARIATION_SELECTOR_15 = 0xFE0E; // text style
    public static final int VARIATION_SELECTOR_16 = 0xFE0F; // emoji style

    public static final int CANCEL_TAG = 0xE007F;

    /**
     * Returns true if the character has Emoji property.
     */
    public static boolean isEmoji(int codePoint) {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI);
    }

    /**
     * Returns true if the character has emoji presentation by default.
     */
    public static boolean isEmojiPresentation(int codePoint) {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_PRESENTATION);
    }

    /**
     * Returns true if the given code point is emoji modifier.
     */
    public static boolean isEmojiModifier(int codePoint) {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER);
    }

    /**
     * Returns true if the given code point is emoji modifier base.
     *
     * @param codePoint codepoint to check
     * @return true if is emoji modifier base
     */
    public static boolean isEmojiModifierBase(int codePoint) {
        // These two characters were removed from Emoji_Modifier_Base in Emoji 4.0, but we need to
        // keep them as emoji modifier bases since there are fonts and user-generated text out there
        // that treats these as potential emoji bases.
        if (codePoint == 0x1F91D || codePoint == 0x1F93C) {
            return true;
        }
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER_BASE);
    }

    /**
     * Returns true if the given code point is regional indicator symbol.
     */
    public static boolean isRegionalIndicatorSymbol(int codePoint) {
        return 0x1F1E6 <= codePoint && codePoint <= 0x1F1FF;
    }

    /**
     * Returns true if the character can be a base character of COMBINING ENCLOSING KEYCAP.
     */
    public static boolean isKeycapBase(int codePoint) {
        return ('0' <= codePoint && codePoint <= '9') || codePoint == '#' || codePoint == '*';
    }

    /**
     * Returns true if the character can be a part of tag_spec in emoji tag sequence.
     * <p>
     * Note that 0xE007F (CANCEL TAG) is not included.
     */
    public static boolean isTagSpecChar(int codePoint) {
        return 0xE0020 <= codePoint && codePoint <= 0xE007E;
    }
}
