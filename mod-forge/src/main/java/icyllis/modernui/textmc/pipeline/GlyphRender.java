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

package icyllis.modernui.textmc.pipeline;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import icyllis.modernui.textmc.CharacterStyleCarrier;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nonnull;

/**
 * A rendering glyph. There is no better way to optimize, an instance takes up 40 bytes.
 */
public abstract class GlyphRender {

    /**
     * Change to params color.
     */
    public static final int USE_INPUT_COLOR = CharacterStyleCarrier.USE_PARAM_COLOR;

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
     * Horizontal advance. Normalized to Minecraft GUI system.
     */
    public final float mAdvance;

    /**
     * Rendering flags, will be inserted later.
     */
    public int mFlags = COLOR_NO_CHANGE;

    public GlyphRender(int stripIndex, float offsetX, float advance, int decoration) {
        mStringIndex = stripIndex;
        mOffsetX = offsetX;
        mAdvance = advance;
        mFlags |= decoration;
    }

    /**
     * Draw the glyph of this info.
     *
     * @param builder vertex builder
     * @param raw     needed by {@link DigitGlyphRender}
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     */
    public abstract void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a);

    /**
     * Draw the glyph of this info.
     *
     * @param matrix     matrix
     * @param buffer     buffer source
     * @param raw        needed by {@link DigitGlyphRender}
     * @param x          start x of the whole text
     * @param y          start y of the whole text
     * @param r          final red
     * @param g          final green
     * @param b          final blue
     * @param a          final alpha
     * @param seeThrough is see through type
     * @param light      packed light
     */
    public abstract void drawGlyph(Matrix4f matrix, @Nonnull MultiBufferSource buffer, @Nonnull CharSequence raw, float x, float y, int r, int g, int b, int a, boolean seeThrough, int light);

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
        if (effect != TextRenderEffect.NO_EFFECT) {
            x += mOffsetX;
            if (effect == TextRenderEffect.UNDERLINE)
                TextRenderEffect.Underline.drawEffect(builder, x, x + getAdvance(), y, r, g, b, a);
            else if (effect == TextRenderEffect.STRIKETHROUGH)
                TextRenderEffect.Strikethrough.drawEffect(builder, x, x + getAdvance(), y, r, g, b, a);
            else if (effect == TextRenderEffect.UNDERLINE_STRIKETHROUGH)
                TextRenderEffect.UnderlineStrikethrough.drawEffect(builder, x, x + getAdvance(), y, r, g, b, a);
        }
    }

    /**
     * Draw the effect of this info
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
    public final void drawEffect(Matrix4f matrix, @Nonnull VertexConsumer builder, float x, float y, int r, int g, int b, int a, int light) {
        if (effect != TextRenderEffect.NO_EFFECT) {
            x += mOffsetX;
            if (effect == TextRenderEffect.UNDERLINE)
                TextRenderEffect.Underline.drawEffect(matrix, builder, x, x + getAdvance(), y, r, g, b, a, light);
            else if (effect == TextRenderEffect.STRIKETHROUGH)
                TextRenderEffect.Strikethrough.drawEffect(matrix, builder, x, x + getAdvance(), y, r, g, b, a, light);
            else if (effect == TextRenderEffect.UNDERLINE_STRIKETHROUGH)
                TextRenderEffect.UnderlineStrikethrough.drawEffect(matrix, builder, x, x + getAdvance(), y, r, g, b, a, light);
        }
    }

    /**
     * Get the glyph advance of this info
     *
     * @return advance
     */
    public abstract float getAdvance();
}
