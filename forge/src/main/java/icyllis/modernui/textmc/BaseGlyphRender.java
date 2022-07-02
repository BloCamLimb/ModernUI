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

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A rendering glyph. There is no better way to optimize, an instance takes up 40 bytes.
 */
abstract class BaseGlyphRender {

    /**
     * Change to params color.
     */
    public static final int USE_INPUT_COLOR = CharacterStyle.NO_COLOR_SPECIFIED;

    /**
     * Keep the color state not to change.
     */
    public static final int COLOR_NO_CHANGE = 0x40000000;

    /**
     * Constructor assignment is stripIndex, and it will be adjusted to stringIndex later.
     */
    public int mStringIndex;

    /**
     * Offset X to the start of the text, it will be adjusted in RTL layout.
     * Normalized to Minecraft GUI system.
     */
    public float mOffsetX;

    /**
     * Laid-out horizontal advance in context. Normalized to Minecraft GUI system.
     */
    private final float mAdvance;

    /**
     * Rendering flags, will be inserted later.
     *
     * @see CharacterStyle
     */
    /*
     * lower 24 bits - color
     * higher 8 bits
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *        11  FONT_STYLE
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *      11    EFFECT
     *     1      OBFUSCATED
     *     11111  LAYOUT
     *    1       FORMATTING_CODE
     *   1        COLOR_NO_CHANGE (GlyphRender)
     *  1         USE_PARAM_COLOR
     *  1 111111  CHARACTER_STYLE
     * |--------|
     */
    public int mFlags = COLOR_NO_CHANGE;

    public BaseGlyphRender(int stripIndex, float offsetX, float advance, int decoration) {
        mStringIndex = stripIndex;
        mOffsetX = offsetX;
        mAdvance = advance;
        mFlags |= decoration;
    }

    /**
     * Draw the glyph of this info.
     *
     * @param builder vertex builder
     * @param input   needed by {@link DigitGlyphRender}
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     * @param res     resolution level
     */
    public abstract void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r,
                                   int g, int b, int a, float res);

    /**
     * Draw the glyph of this info.
     *
     * @param matrix     matrix
     * @param source     buffer source
     * @param input      needed by {@link DigitGlyphRender}
     * @param x          start x of the whole text
     * @param y          start y of the whole text
     * @param r          final red
     * @param g          final green
     * @param b          final blue
     * @param a          final alpha
     * @param seeThrough if see-through type
     * @param light      packed light
     * @param res        resolution level
     */
    public abstract void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source,
                                   @Nullable CharSequence input, float x, float y, int r, int g, int b, int a,
                                   boolean seeThrough, int light, float res);

    /**
     * Draw the effect of this info
     *
     * @param builder vertex builder
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     */
    public final void drawEffect(@Nonnull VertexConsumer builder, float x, float y, int r, int g, int b, int a) {
        if ((mFlags & CharacterStyle.EFFECT_MASK) != 0) {
            x += mOffsetX;
            if ((mFlags & CharacterStyle.UNDERLINE_MASK) != 0) {
                TextRenderEffect.drawUnderline(builder, x, x + mAdvance, y, r, g, b, a);
            }
            if ((mFlags & CharacterStyle.STRIKETHROUGH_MASK) != 0) {
                TextRenderEffect.drawStrikethrough(builder, x, x + mAdvance, y, r, g, b, a);
            }
        }
    }

    /**
     * Draw the effect of this info.
     *
     * @param matrix  matrix
     * @param builder vertex builder
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     * @param light   packed light
     */
    public final void drawEffect(@Nonnull Matrix4f matrix, @Nonnull VertexConsumer builder, float x, float y, int r,
                                 int g, int b, int a, int light) {
        if ((mFlags & CharacterStyle.EFFECT_MASK) != 0) {
            x += mOffsetX;
            if ((mFlags & CharacterStyle.UNDERLINE_MASK) != 0) {
                TextRenderEffect.drawUnderline(matrix, builder, x, x + mAdvance, y, r, g, b, a, light);
            }
            if ((mFlags & CharacterStyle.STRIKETHROUGH_MASK) != 0) {
                TextRenderEffect.drawStrikethrough(matrix, builder, x, x + mAdvance, y, r, g, b, a, light);
            }
        }
    }

    /**
     * Get the glyph advance of this info
     *
     * @return advance
     */
    public float getAdvance() {
        return mAdvance;
    }

    @Override
    public String toString() {
        return "BaseGlyphRender{" +
                "stringIndex=" + mStringIndex +
                ", offsetX=" + mOffsetX +
                ", advance=" + mAdvance +
                ", flags=0x" + Integer.toHexString(mFlags) +
                '}';
    }
}
