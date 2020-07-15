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

import icyllis.modernui.font.glyph.GlyphManager;
import icyllis.modernui.font.glyph.TexturedGlyph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;

public class TextProcessState {

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


    private int defaultFontStyle;
    private int currentFontStyle;

    private int defaultColor;
    private int currentColor;

    private boolean defaultStrikethrough;
    private boolean currentStrikethrough;

    private float strikethroughStart;

    private boolean defaultUnderline;
    private boolean currentUnderline;

    private float underlineStart;

    private boolean defaultObfuscated;
    private boolean currentObfuscated;

    private boolean digitMode;

    /**
     * @see GlyphManager#lookupDigits(int, int)
     */
    private TexturedGlyph[] digitGlyphs;

    private final IntArrayList digitIndexList = new IntArrayList();

    private int obfuscatedCount;

    private float advance;

    /**
     * Update style and set default values
     *
     * @param style s
     */
    public void updateState(@Nonnull Style style) {
        if (style.getColor() != null) {
            defaultColor = style.getColor().func_240742_a_();
        } else {
            defaultColor = NO_COLOR;
        }
        currentColor = defaultColor;

        defaultFontStyle = PLAIN;
        if (style.getBold()) {
            defaultFontStyle |= BOLD;
        }
        if (style.getItalic()) {
            defaultFontStyle |= ITALIC;
        }
        currentFontStyle = defaultFontStyle;

        defaultUnderline = style.getUnderlined();
        currentUnderline = defaultUnderline;
        underlineStart = 0;

        defaultStrikethrough = style.getStrikethrough();
        currentStrikethrough = defaultStrikethrough;
        strikethroughStart = 0;

        defaultObfuscated = style.getObfuscated();
        currentObfuscated = defaultObfuscated;

        digitMode = false;
        digitGlyphs = null;
        digitIndexList.clear();
        obfuscatedCount = 0;
        advance = 0;
    }

    /**
     * Increase advance for current node
     *
     * @param adv a
     */
    public void addAdvance(float adv) {
        advance += adv;
    }

    public boolean setDefaultFontStyle() {
        if (currentFontStyle != defaultFontStyle) {
            currentFontStyle = defaultFontStyle;
            return true;
        }
        return false;
    }

    /**
     * Set current bold
     *
     * @param b b
     * @return if bold changed
     */
    public boolean setBold(boolean b) {
        if (b) {
            b = (currentFontStyle & BOLD) == 0;
            currentFontStyle |= BOLD;
        } else {
            b = (currentFontStyle & BOLD) != 0;
            currentFontStyle &= ~BOLD;
        }
        return b;
    }

    /**
     * Set current italic
     *
     * @param b b
     * @return if italic changed
     */
    public boolean setItalic(boolean b) {
        if (b) {
            b = (currentFontStyle & ITALIC) == 0;
            currentFontStyle |= ITALIC;
        } else {
            b = (currentFontStyle & ITALIC) != 0;
            currentFontStyle &= ~ITALIC;
        }
        return b;
    }

    public boolean setDefaultColor() {
        return setColor(defaultColor);
    }

    /**
     * Set current color
     *
     * @param color c
     * @return if color changed
     */
    public boolean setColor(int color) {
        if (currentColor != color) {
            currentColor = color;
            return true;
        }
        return false;
    }

    public boolean setDigitMode(boolean b) {
        if (digitMode != b) {
            digitMode = b;
            return true;
        }
        return false;
    }

    public void setDigitGlyphs(TexturedGlyph[] digitGlyphs) {
        this.digitGlyphs = digitGlyphs;
    }

    public void addDigitIndex(int i) {
        digitIndexList.add(i);
    }

    public boolean isDigitMode() {
        return digitMode;
    }

    public boolean hasDigit() {
        return !digitIndexList.isEmpty();
    }

    public TexturedGlyph[] getDigitGlyphs() {
        return digitGlyphs;
    }

    public int[] toDigitIndexArray() {
        return digitIndexList.toArray(new int[0]);
    }

    public boolean setDefaultStrikethrough() {
        return setStrikethrough(defaultStrikethrough);
    }

    public boolean setStrikethrough(boolean b) {
        if (currentStrikethrough != b) {
            currentStrikethrough = b;
            if (b) {
                strikethroughStart = advance;
            }
            return true;
        }
        return false;
    }

    public boolean setDefaultUnderline() {
        return setUnderline(defaultUnderline);
    }

    public boolean setUnderline(boolean b) {
        if (currentUnderline != b) {
            currentUnderline = b;
            if (b) {
                underlineStart = advance;
            }
            return true;
        }
        return false;
    }

    public boolean setDefaultObfuscated() {
        return setObfuscated(defaultObfuscated);
    }

    public boolean setObfuscated(boolean b) {
        if (currentObfuscated != b) {
            currentObfuscated = b;
            if (b) {
                obfuscatedCount = 0;
            }
            return true;
        }
        return false;
    }

    public void addObfuscatedCount() {
        obfuscatedCount++;
    }

    public int getObfuscatedCount() {
        return obfuscatedCount;
    }

    public float getAdvance() {
        return advance;
    }

    public int getFontStyle() {
        return currentFontStyle;
    }

    public int getColor() {
        return currentColor;
    }

    public boolean isObfuscated() {
        return currentObfuscated;
    }

    public float getStrikethroughStart() {
        return strikethroughStart;
    }

    public float getUnderlineStart() {
        return underlineStart;
    }

    public boolean isStrikethrough() {
        return currentStrikethrough;
    }

    public boolean isUnderline() {
        return currentUnderline;
    }
}
