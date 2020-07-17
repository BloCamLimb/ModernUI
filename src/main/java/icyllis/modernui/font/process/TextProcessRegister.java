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

import icyllis.modernui.font.glyph.TexturedGlyph;
import icyllis.modernui.font.node.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Temporary process results
 */
public class TextProcessRegister {

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
    private int lastColor;

    private boolean defaultStrikethrough;
    private boolean currentStrikethrough;

    private float strikethroughStart;

    private boolean defaultUnderline;
    private boolean currentUnderline;

    private float underlineStart;

    private boolean defaultObfuscated;
    private boolean currentObfuscated;

    private float advance;

    private final List<IGlyphRenderInfo> glyphs  = new ObjectArrayList<>();
    private final List<EffectRenderInfo> effects = new ObjectArrayList<>();
    private final List<ColorStateInfo>   colors  = new ObjectArrayList<>();

    /**
     * Update style and set default values
     *
     * @param style s
     */
    public void beginProcess(@Nonnull Style style) {
        if (style.getColor() != null) {
            defaultColor = style.getColor().func_240742_a_();
            colors.add(new ColorStateInfo(0, defaultColor));
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

        advance = 0;
    }

    public void finishProcess() {
        if (currentStrikethrough) {
            effects.add(EffectRenderInfo.strikethrough(strikethroughStart, advance, currentColor));
            strikethroughStart = advance;
        }
        if (currentUnderline) {
            effects.add(EffectRenderInfo.ofUnderline(underlineStart, advance, currentColor));
            underlineStart = advance;
        }
    }

    @Nullable
    public EffectRenderInfo[] wrapEffects() {
        if (effects.isEmpty()) {
            return null;
        }
        EffectRenderInfo[] r = effects.toArray(new EffectRenderInfo[0]);
        effects.clear();
        return r;
    }

    @Nonnull
    public IGlyphRenderInfo[] wrapGlyphs() {
        IGlyphRenderInfo[] r = glyphs.toArray(new IGlyphRenderInfo[0]);
        glyphs.clear();
        return r;
    }

    @Nullable
    public ColorStateInfo[] wrapColors() {
        if (colors.isEmpty()) {
            return null;
        }
        ColorStateInfo[] r = colors.toArray(new ColorStateInfo[0]);
        colors.clear();
        return r;
    }

    public void applyFormatting(@Nonnull TextFormatting formatting, int glyphIndex) {
        if (formatting.getColor() != null) {
            if (setColor(formatting.getColor())) {
                colors.add(new ColorStateInfo(glyphIndex, currentColor));
                if (currentStrikethrough) {
                    effects.add(EffectRenderInfo.strikethrough(strikethroughStart, advance, lastColor));
                    strikethroughStart = advance;
                }
                if (currentUnderline) {
                    effects.add(EffectRenderInfo.ofUnderline(underlineStart, advance, lastColor));
                    underlineStart = advance;
                }
            }
        } else {
            switch (formatting) {
                case STRIKETHROUGH:
                    setStrikethrough(true);
                    break;
                case UNDERLINE:
                    setUnderline(true);
                    break;
                case BOLD:
                    setBold(true);
                    break;
                case ITALIC:
                    setItalic(true);
                    break;
                case OBFUSCATED:
                    setObfuscated(true);
                    break;
                case RESET: {
                    boolean p = false;
                    if (setDefaultColor()) {
                        colors.add(new ColorStateInfo(glyphIndex, currentColor));
                        if (currentStrikethrough) {
                            effects.add(EffectRenderInfo.strikethrough(strikethroughStart, advance, lastColor));
                            strikethroughStart = advance;
                        }
                        if (currentUnderline) {
                            effects.add(EffectRenderInfo.ofUnderline(underlineStart, advance, lastColor));
                            underlineStart = advance;
                        }
                        p = true;
                    }
                    setDefaultObfuscated();
                    setDefaultFontStyle();
                    if (setDefaultStrikethrough() && !p) {
                        effects.add(EffectRenderInfo.strikethrough(strikethroughStart, advance, currentColor));
                    }
                    if (setDefaultUnderline() && !p) {
                        effects.add(EffectRenderInfo.ofUnderline(underlineStart, advance, currentColor));
                    }
                }
                break;
            }
        }
    }

    /**
     * Increase advance for current node
     *
     * @param adv a
     */
    @Deprecated
    private void addAdvance(float adv) {
        advance += adv;
    }

    public void depositGlyph(TexturedGlyph glyph) {
        glyphs.add(new GlyphRenderInfo(glyph));
        advance += glyph.advance;
    }

    public void depositDigit(int stringIndex, TexturedGlyph[] glyphs) {
        if (currentObfuscated) {
            this.glyphs.add(new ObfuscatedInfo(glyphs));
        } else {
            this.glyphs.add(new DigitRenderInfo(glyphs, stringIndex));
        }
        advance += glyphs[0].advance;
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
    private boolean setBold(boolean b) {
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
            lastColor = currentColor;
            currentColor = color;
            return true;
        }
        return false;
    }

    /*public boolean setDigitMode(boolean b) {
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
    }*/

    public boolean setDefaultStrikethrough() {
        return setStrikethrough(defaultStrikethrough);
    }

    public boolean setStrikethrough(boolean b) {
        if (currentStrikethrough != b) {
            currentStrikethrough = b;
            if (b) {
                strikethroughStart = advance;
            } else {
                return true;
            }
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
            } else {
                return true;
            }
        }
        return false;
    }

    public void setDefaultObfuscated() {
        setObfuscated(defaultObfuscated);
    }

    public void setObfuscated(boolean b) {
        if (currentObfuscated != b) {
            currentObfuscated = b;
            /*if (!b && obfuscatedCount > 0) {
                glyphs.add(GlyphRenderInfo.ofObfuscated(digitGlyphs, currentColor, obfuscatedCount));
                obfuscatedCount = 0;
            }*/
        }
    }

    /*public void addObfuscatedCount() {
        obfuscatedCount++;
    }

    public int getObfuscatedCount() {
        return obfuscatedCount;
    }*/

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

    /*public float getStrikethroughStart() {
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
    }*/
}
