/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.font.process;

import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;

/**
 * Identifies the location and value of a formatting info in the original string
 * This is for text processing only, will be destroyed after processing.
 */
public class FormattingStyle {

    /**
     * Bit flag used with fontStyle to request the plain (normal) style
     */
    public static final byte PLAIN  = 0;
    /**
     * Bit flag used with fontStyle to request the bold style
     */
    public static final byte BOLD   = 1;
    /**
     * Bit flag used with fontStyle to request the italic style
     */
    public static final byte ITALIC = 1 << 1;

    /**
     * Represent to use default color
     */
    public static final int NO_COLOR = -1;

    /**
     * Bit mask representing underline effect
     */
    private static final byte UNDERLINE_MASK = 1;
    /**
     * Bit mask representing strikethrough effect
     */
    private static final byte STRIKETHROUGH_MASK = 1 << 1;
    /**
     * Bit mask representing obfuscated characters rendering
     */
    private static final byte OBFUSCATED_MASK = 1 << 2;


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

    /**
     * Combination of {@link #PLAIN}, {@link #BOLD}, and {@link #ITALIC} specifying font specific styles.
     */
    private final byte fontStyle;

    /**
     * Combination of Underline (1), Strikethrough (2), Obfuscated (4)
     */
    private final byte effect;

    public FormattingStyle(int stringIndex, int stripIndex, Style style) {
        this.stringIndex = stringIndex;
        this.stripIndex = stripIndex;
        color = getColor(style);
        fontStyle = getFontStyle(style);
        effect = getEffect(style);
    }

    public static int getColor(@Nonnull Style style) {
        return style.getColor() == null ? NO_COLOR : style.getColor().func_240742_a_();
    }

    /**
     * The color in 0xRRGGBB format; {@link #NO_COLOR} to reset default (original parameter) color
     *
     * @return color
     */
    public int getColor() {
        return color;
    }

    public static byte getFontStyle(@Nonnull Style style) {
        byte v = PLAIN;
        if (style.getBold()) {
            v |= BOLD;
        }
        if (style.getItalic()) {
            v |= ITALIC;
        }
        return v;
    }

    /**
     * Combination of {@link #PLAIN}, {@link #BOLD}, and {@link #ITALIC} specifying font specific styles.
     *
     * @return font style
     */
    public byte getFontStyle() {
        return fontStyle;
    }

    public static byte getEffect(@Nonnull Style style) {
        byte v = 0;
        if (style.getUnderlined()) {
            v |= UNDERLINE_MASK;
        }
        if (style.getStrikethrough()) {
            v |= STRIKETHROUGH_MASK;
        }
        if (style.getObfuscated()) {
            v |= OBFUSCATED_MASK;
        }
        return v;
    }

    public boolean isUnderline() {
        return (effect & UNDERLINE_MASK) != 0;
    }

    public boolean isStrikethrough() {
        return (effect & STRIKETHROUGH_MASK) != 0;
    }

    /**
     * Represent to use obfuscated (random monospaced) characters
     *
     * @return obfuscated
     */
    public boolean isObfuscated() {
        return (effect & OBFUSCATED_MASK) != 0;
    }
}
