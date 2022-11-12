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

package icyllis.modernui.textmc;

import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Identifies the location and value of a formatting style in the original string.
 * Instances exist only during layout and will be destroyed after processing.
 *
 * @see Style
 * @see net.minecraft.ChatFormatting
 */
public class CharacterStyle {

    /*
     * lower 24 bits - 0xRRGGBB color
     * higher 8 bits
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *        11  FONT_STYLE_MASK
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *      11    EFFECT_MASK
     *     1      OBFUSCATED
     *     11111  LAYOUT_MASK
     *    1       DIGIT_REPLACEMENT
     *   1        BITMAP_REPLACEMENT
     *  1         IMPLICIT_COLOR
     * |--------|
     */
    /**
     * Bit flag used with fontStyle to request the normal style
     */
    public static final int NORMAL = 0;

    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final int BOLD_MASK = 0x1000000;

    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final int ITALIC_MASK = 0x2000000;

    public static final int FONT_STYLE_MASK = BOLD_MASK | ITALIC_MASK;

    /**
     * Bit mask representing underline effect
     */
    public static final int UNDERLINE_MASK = 0x4000000;

    /**
     * Bit mask representing strikethrough effect
     */
    public static final int STRIKETHROUGH_MASK = 0x8000000;

    public static final int EFFECT_MASK = UNDERLINE_MASK | STRIKETHROUGH_MASK;

    /**
     * Bit mask representing obfuscated characters rendering
     */
    public static final int OBFUSCATED_MASK = 0x10000000;

    public static final int LAYOUT_MASK = FONT_STYLE_MASK | EFFECT_MASK | OBFUSCATED_MASK;

    /**
     * Represent to use fast digit replacement. Then advances will not change, just
     * replace their ASCII backed glyphs with new input string.
     */
    public static final int FAST_DIGIT_REPLACEMENT = 0x20000000;

    /**
     * Represent to use a color emoji or a bitmap glyph.
     */
    public static final int BITMAP_REPLACEMENT = 0x40000000;

    /**
     * 0xRRGGBB color mask.
     */
    public static final int COLOR_MASK = 0xFFFFFF;

    /**
     * Bit mask representing to use input color, then lower 24 bits are ignored.
     */
    public static final int IMPLICIT_COLOR_MASK = 0x80000000;

    /**
     * Full color means RGB mask with {@link #IMPLICIT_COLOR_MASK}.
     * If it has {@link #IMPLICIT_COLOR_MASK}, then lower 24 bits are ignored (and it should be 0x000000).
     */
    public static final int FULL_COLOR_MASK = COLOR_MASK | IMPLICIT_COLOR_MASK;

    /**
     * The index into the original string (i.e. with formatting codes) for the location of this formatting.
     */
    public final int mStringIndex;

    /**
     * The index into the stripped string (i.e. with no formatting codes) of where this formatting would have appeared.
     */
    public final int mStripIndex;

    /**
     * The style flags. Combination of {@link #NORMAL}, {@link #BOLD_MASK}, {@link #ITALIC_MASK},
     * {@link #UNDERLINE_MASK}, {@link #STRIKETHROUGH_MASK}, {@link #OBFUSCATED_MASK} and {@link #FULL_COLOR_MASK}.
     */
    private final int mFlags;

    @Deprecated
    public CharacterStyle(int stringIndex, int stripIndex, Style style) {
        mStringIndex = stringIndex;
        mStripIndex = stripIndex;
        mFlags = flatten(style);
    }

    /**
     * Pack the given style to an int value that can produce a visual change.
     * That is, full color, bold, italic, underline, strikethrough and obfuscated.
     *
     * @param style style to flatten
     * @return a packed integer
     * @see #isAppearanceAffecting(Style, Style)
     */
    public static int flatten(@Nonnull Style style) {
        int v = NORMAL;
        if (style.getColor() == null) {
            v |= IMPLICIT_COLOR_MASK;
        } else {
            // RGB - 24 bit
            v |= style.getColor().getValue() & COLOR_MASK;
        }
        if (style.isBold()) {
            v |= BOLD_MASK;
        }
        if (style.isItalic()) {
            v |= ITALIC_MASK;
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
     * Returns if two styles can produce a visual change. That is, appearance flags
     * or font collection are different.
     *
     * @see #flatten(Style)
     */
    public static boolean isAppearanceAffecting(@Nonnull Style a, @Nonnull Style b) {
        return a != b && (a.isBold() != b.isBold() ||
                a.isItalic() != b.isItalic() ||
                a.isUnderlined() != b.isUnderlined() ||
                a.isStrikethrough() != b.isStrikethrough() ||
                a.isObfuscated() != b.isObfuscated() ||
                !Objects.equals(a.getColor(), b.getColor()) ||
                !Objects.equals(a.getFont(), b.getFont()));
    }

    /**
     * The color in 0xRRGGBB format; {@link #IMPLICIT_COLOR_MASK} to use param color.
     * If it has {@link #IMPLICIT_COLOR_MASK}, then lower 24 bits are ignored (and it should be 0x000000).
     *
     * @return the color with additional flag
     */
    public int getFullColor() {
        return mFlags & FULL_COLOR_MASK;
    }

    public boolean isBold() {
        return (mFlags & BOLD_MASK) != 0;
    }

    public boolean isItalic() {
        return (mFlags & ITALIC_MASK) != 0;
    }

    /**
     * Combination of {@link #NORMAL}, {@link #BOLD_MASK}, and {@link #ITALIC_MASK} specifying font specific styles.
     *
     * @return font style
     */
    public int getFontStyle() {
        return mFlags & FONT_STYLE_MASK;
    }

    /**
     * Represent whether to render underline effect
     *
     * @return underlined
     */
    public boolean isUnderlined() {
        return (mFlags & UNDERLINE_MASK) != 0;
    }

    /**
     * Represent whether to render strikethrough effect
     *
     * @return strikethrough
     */
    public boolean isStrikethrough() {
        return (mFlags & STRIKETHROUGH_MASK) != 0;
    }

    /**
     * Get effect flags. {@link #UNDERLINE_MASK} and {@link #STRIKETHROUGH_MASK}.
     *
     * @return effect
     */
    public int getEffect() {
        return mFlags & EFFECT_MASK;
    }

    /**
     * Represent to use obfuscated (random monospaced) characters each draw.
     *
     * @return obfuscated
     */
    public boolean isObfuscated() {
        return (mFlags & OBFUSCATED_MASK) != 0;
    }

    /**
     * Whether this style is from formatting codes (non-printing chars), or a {@link Style} object.
     *
     * @return formatting code
     */
    public boolean isFormattingCode() {
        return false;
    }

    /**
     * Check if font style not equals.
     *
     * @param s obj
     * @return if font style not equals
     */
    public boolean isMetricAffecting(@Nonnull CharacterStyle s) {
        return (mFlags & FONT_STYLE_MASK) != (s.mFlags & FONT_STYLE_MASK);
    }

    /**
     * Check if layout style not equals, excluding effects.
     *
     * @param s obj
     * @return if layout style not equals
     */
    public boolean isLayoutAffecting(@Nonnull CharacterStyle s) {
        return (mFlags & LAYOUT_MASK) != (s.mFlags & LAYOUT_MASK);
    }

    @Override
    public String toString() {
        return "CharacterStyle{" +
                "stringIndex=" + mStringIndex +
                ",stripIndex=" + mStripIndex +
                ",flags=0x" + Integer.toHexString(mFlags) +
                '}';
    }
}
