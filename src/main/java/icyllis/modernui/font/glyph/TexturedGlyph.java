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

package icyllis.modernui.font.glyph;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import icyllis.modernui.font.node.TextRenderType;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;

import javax.annotation.Nonnull;

/**
 * This class holds information for a glyph about its pre-rendered image in an OpenGL texture. The texture coordinates in
 * this class are normalized in the standard 0.0 - 1.0 OpenGL range.
 *
 * @since 2.0
 */
public class TexturedGlyph {

    /**
     * Render type for render type buffer system.
     */
    private final TextRenderType renderType;

    /**
     * The horizontal advance in pixels of this glyph, including font spacing!
     * This will be used for layout or trim.
     */
    public final float advance;

    /**
     * The offset to baseline that specified when drawing.
     * This will be used for drawing offset Y.
     */
    private final float baseline;

    /**
     * The total width of this glyph image.
     * In pixels, due to gui scaling, we convert it into float.
     * This will be used for drawing size.
     */
    private final float width;

    /**
     * The total height of this glyph image.
     * In pixels, due to gui scaling, we convert it into float.
     * This will be used for drawing size.
     */
    private final float height;

    /**
     * The horizontal texture coordinate of the upper-left corner.
     */
    private final float u1;

    /**
     * The vertical texture coordinate of the upper-left corner.
     */
    private final float v1;

    /**
     * The horizontal texture coordinate of the lower-right corner.
     */
    private final float u2;

    /**
     * The vertical texture coordinate of the lower-right corner.
     */
    private final float v2;

    public TexturedGlyph(int textureName, float advance, float baseline, float width, float height, float u1, float v1, float u2, float v2) {
        renderType = TextRenderType.getOrCacheType(textureName);
        this.advance = advance;
        this.baseline = baseline;
        this.width = width;
        this.height = height;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
    }

    public float drawGlyph(@Nonnull IVertexBuilder builder, float x, float y, int r, int g, int b, int a) {
        RenderSystem.bindTexture(renderType.textureName);
        y += baseline;
        builder.pos(x, y, 0).color(r, g, b, a).tex(u1, v1).endVertex();
        builder.pos(x, y + height, 0).color(r, g, b, a).tex(u1, v2).endVertex();
        builder.pos(x + width, y + height, 0).color(r, g, b, a).tex(u2, v2).endVertex();
        builder.pos(x + width, y, 0).color(r, g, b, a).tex(u2, v1).endVertex();
        return x + advance;
    }

    public float drawGlyph(Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, float x, float y, int r, int g, int b, int a, int packedLight) {
        IVertexBuilder builder = buffer.getBuffer(renderType);
        y += baseline;
        builder.pos(matrix, x, y, 0).color(r, g, b, a).tex(u1, v1).lightmap(packedLight).endVertex();
        builder.pos(matrix, x, y + height, 0).color(r, g, b, a).tex(u1, v2).lightmap(packedLight).endVertex();
        builder.pos(matrix, x + width, y + height, 0).color(r, g, b, a).tex(u2, v2).lightmap(packedLight).endVertex();
        builder.pos(matrix, x + width, y, 0).color(r, g, b, a).tex(u2, v1).lightmap(packedLight).endVertex();
        return x + advance;
    }
}
