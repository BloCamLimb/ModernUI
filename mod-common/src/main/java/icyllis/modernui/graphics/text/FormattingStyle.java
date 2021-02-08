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

package icyllis.modernui.graphics.text;

import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Identifies the location and value of a formatting info in the original string
 * This is for text processing only, will be destroyed after processing.
 */
public class FormattingStyle {

    /**
     * Bit flag used with fontStyle to request the plain (normal) style
     */
    public static final byte PLAIN  = Font.PLAIN;
    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final byte BOLD   = Font.BOLD;
    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final byte ITALIC = Font.ITALIC;

    private static final byte FONT_STYLE_MASK    = BOLD | ITALIC;
    /**
     * Bit mask representing underline effect
     */
    private static final byte UNDERLINE_MASK     = 1 << 2;
    /**
     * Bit mask representing strikethrough effect
     */
    private static final byte STRIKETHROUGH_MASK = 1 << 3;
    /**
     * Bit mask representing obfuscated characters rendering
     */
    private static final byte OBFUSCATED_MASK    = 1 << 4;

    /**
     * Represent to use default color
     */
    public static final int NO_COLOR = -1;


    /**
     * The index into the original string (i.e. with formatting codes) for the location of this formatting.
     */
    public final int stringIndex;

    /**
     * The index into the stripped string (i.e. with no formatting codes) of where this formatting would have appeared
     */
    public final int stripIndex;

    /**
     * The color in 0xRRGGBB format; {@link #NO_COLOR} to reset default (original parameter) color
     */
    private final int color;

    /*
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *        11  FONT_STYLE
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *     1      OBFUSCATED
     * |--------|
     */
    /**
     * Combination of {@link #PLAIN}, {@link #BOLD}, {@link #ITALIC}, {@link #UNDERLINE_MASK},
     * {@link #STRIKETHROUGH_MASK} and {@link #OBFUSCATED_MASK} specifying font specific styles.
     */
    private final byte flags;

    public FormattingStyle(int stringIndex, int stripIndex, Style style) {
        this.stringIndex = stringIndex;
        this.stripIndex = stripIndex;
        color = getColor(style);
        flags = getFlags(style);
    }

    public static int getColor(@Nonnull Style style) {
        return style.getColor() == null ? NO_COLOR : style.getColor().getValue();
    }

    /**
     * The color in 0xRRGGBB format; {@link #NO_COLOR} to reset default (original parameter) color
     *
     * @return color
     */
    public int getColor() {
        return color;
    }

    public static byte getFlags(@Nonnull Style style) {
        byte v = PLAIN;
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
     * Combination of {@link #PLAIN}, {@link #BOLD}, and {@link #ITALIC} specifying font specific styles.
     *
     * @return font style, same as Font class
     */
    public int getFontStyle() {
        return flags & FONT_STYLE_MASK;
    }

    /**
     * Represent whether to use underline effect
     *
     * @return underline
     */
    public boolean isUnderline() {
        return (flags & UNDERLINE_MASK) != 0;
    }

    /**
     * Represent whether to use strikethrough effect
     *
     * @return strikethrough
     */
    public boolean isStrikethrough() {
        return (flags & STRIKETHROUGH_MASK) != 0;
    }

    /**
     * Represent to use obfuscated (random monospaced) characters
     *
     * @return obfuscated
     */
    public boolean isObfuscated() {
        return (flags & OBFUSCATED_MASK) != 0;
    }

    /**
     * Check if layout style equals, excluding effects
     *
     * @param s obj
     * @return if layout style equals
     */
    public boolean layoutStyleEquals(@Nonnull FormattingStyle s) {
        return flags == s.flags;
    }
}
