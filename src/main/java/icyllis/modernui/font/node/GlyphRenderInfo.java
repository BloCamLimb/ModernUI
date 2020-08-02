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

package icyllis.modernui.font.node;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GlyphRenderInfo {

    /**
     * Glyph effect
     */
    @Nullable
    public TextRenderEffect effect;

    /**
     * RGB color node
     */
    @Nullable
    public Integer color;

    /**
     * First assignment is stripIndex, and it will be adjusted to stringIndex later.
     */
    public int stringIndex;

    /**
     * Offset X to the start of the text, it will be adjusted in RTL layout
     */
    public float offsetX;

    public GlyphRenderInfo(@Nullable TextRenderEffect effect, int stringIndex, float offsetX) {
        this.effect = effect;
        this.stringIndex = stringIndex;
        this.offsetX = offsetX;
    }

    /**
     * Draw the glyph of this info.
     *
     * @param builder vertex builder
     * @param raw     needed by {@link DigitGlyphInfo}
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
     *  @param matrix matrix
     * @param buffer buffer source
     * @param raw    needed by {@link DigitGlyphInfo}
     * @param x      start x of the whole text
     * @param y      start y of the whole text
     * @param r      final red
     * @param g      final green
     * @param b      final blue
     * @param a      final alpha
     * @param transparent
     * @param light  packed light
     */
    public abstract void drawGlyph(Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, @Nonnull String raw, float x, float y, int r, int g, int b, int a, boolean transparent, int light);

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
    public void drawEffect(@Nonnull IVertexBuilder builder, float x, float y, int r, int g, int b, int a) {
        if (effect != null) {
            x += offsetX;
            effect.drawEffect(builder, x, x + getAdvance(), y, r, g, b, a);
        }
    }

    /**
     * Draw the effect of this info
     *
     * @param matrix matrix
     * @param builder vertex builder
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     * @param light  packed light
     */
    public void drawEffect(Matrix4f matrix, @Nonnull IVertexBuilder builder, float x, float y, int r, int g, int b, int a, int light) {
        if (effect != null) {
            x += offsetX;
            effect.drawEffect(matrix, builder, x, x + getAdvance(), y, r, g, b, a, light);
        }
    }

    /**
     * Get the glyph advance of this info
     *
     * @return advance
     */
    public abstract float getAdvance();
}
