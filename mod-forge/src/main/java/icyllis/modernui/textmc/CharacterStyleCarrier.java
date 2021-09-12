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

package icyllis.modernui.textmc;

import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;

/**
 * Identifies the location and value of a formatting style in the original string.
 * Instances exist only during layout and will be destroyed after processing.
 */
public class CharacterStyleCarrier {

    /*
     * higher 8 bits
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *        11  FONT_STYLE
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *     1      OBFUSCATED
     *  1         USE_PARAM_COLOR
     * |--------|
     */
    /**
     * Bit flag used with fontStyle to request the normal style
     */
    public static final int NORMAL = 0;

    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final int BOLD = 0x1000000;

    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final int ITALIC = 0x2000000;

    private static final int FONT_STYLE_MASK = BOLD | ITALIC;

    /**
     * Bit mask representing underline effect
     */
    public static final int UNDERLINE_MASK = 0x4000000;

    /**
     * Bit mask representing strikethrough effect
     */
    public static final int STRIKETHROUGH_MASK = 0x8000000;

    public static final int DECORATION_MASK = UNDERLINE_MASK | STRIKETHROUGH_MASK;

    /**
     * Bit mask representing obfuscated characters rendering
     */
    private static final int OBFUSCATED_MASK = 0x10000000;

    private static final int LAYOUT_MASK = FONT_STYLE_MASK | DECORATION_MASK | OBFUSCATED_MASK;

    /**
     * Bit mask representing to use param color.
     */
    public static final int USE_PARAM_COLOR = 0x80000000;

    private static final int COLOR_MASK = 0x80FFFFFF;

    /**
     * The index into the original string (i.e. with formatting codes) for the location of this formatting.
     */
    public final int mStringIndex;

    /**
     * The index into the stripped string (i.e. with no formatting codes) of where this formatting would have appeared
     */
    public final int mStripIndex;

    /**
     * The style flags. Combination of {@link #NORMAL}, {@link #BOLD}, {@link #ITALIC}, {@link #UNDERLINE_MASK},
     * {@link #STRIKETHROUGH_MASK} and {@link #OBFUSCATED_MASK}.
     */
    private final int mFlags;

    public CharacterStyleCarrier(int stringIndex, int stripIndex, Style style) {
        mStringIndex = stringIndex;
        mStripIndex = stripIndex;
        mFlags = getFlags(style);
    }

    /**
     * Normalize the given style to an int value that can produce a visual change.
     *
     * @param style style to parse
     * @return result
     */
    public static int getFlags(@Nonnull Style style) {
        int v = NORMAL;
        if (style.getColor() == null) {
            v |= USE_PARAM_COLOR;
        } else {
            // RGB - 24 bit
            v |= style.getColor().getValue() & 0xFFFFFF;
        }
        if (style.isBold()) {
            v |= BOLD;
        }
        if (style.isItalic()) {
            v |= ITALIC;
        }
        if (style.isUnderlined()) {
            v |= UNDERLINE_MASK;
        }
        if (style.isStrikethrough()) {
            v |= STRIKETHROUGH_MASK;
        }
        if (style.isObfuscated()) {
            v |= OBFUSCATED_MASK;
        }
        return v;
    }

    /**
     * The color in 0xRRGGBB format; {@link #USE_PARAM_COLOR} to use param color.
     *
     * @return color
     */
    public int getColor() {
        return mFlags & COLOR_MASK;
    }

    /**
     * Combination of {@link #NORMAL}, {@link #BOLD}, and {@link #ITALIC} specifying font specific styles.
     *
     * @return font style, same as Font class
     */
    public int getFontStyle() {
        return mFlags & FONT_STYLE_MASK;
    }

    /**
     * Represent whether to use underline effect
     *
     * @return underline
     */
    public boolean isUnderline() {
        return (mFlags & UNDERLINE_MASK) != 0;
    }

    /**
     * Represent whether to use strikethrough effect
     *
     * @return strikethrough
     */
    public boolean isStrikethrough() {
        return (mFlags & STRIKETHROUGH_MASK) != 0;
    }

    /**
     * Represent to use obfuscated (random monospaced) characters
     *
     * @return obfuscated
     */
    public boolean isObfuscated() {
        return (mFlags & OBFUSCATED_MASK) != 0;
    }

    /**
     * Get decoration flags.
     *
     * @return decoration
     */
    public int getDecoration() {
        return mFlags & DECORATION_MASK;
    }

    /**
     * Check if layout style not equals, excluding effects
     *
     * @param s obj
     * @return if layout style equals
     */
    public boolean isLayoutAffecting(@Nonnull CharacterStyleCarrier s) {
        return (mFlags & LAYOUT_MASK) != (s.mFlags & LAYOUT_MASK);
    }

    @Override
    public String toString() {
        return "CharacterStyleCarrier{" +
                "stringIndex=" + mStringIndex +
                ", stripIndex=" + mStripIndex +
                ", flags=0x" + Integer.toHexString(mFlags) +
                '}';
    }
}
