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

package icyllis.modernui.graphics.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import icyllis.modernui.graphics.text.pipeline.TextRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

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
    private RenderType renderType;
    private RenderType seeThroughType;

    // see getAdvance()
    private float advance;

    /**
     * The offset to baseline that specified when drawing.
     * The value should be a multiple of (1 / guiScale)
     * This will be used for drawing offset X.
     */
    private float baselineX;

    /**
     * The offset to baseline that specified when drawing.
     * The value should be a multiple of (1 / guiScale)
     * This will be used for drawing offset Y.
     */
    private float baselineY;

    /**
     * The total width of this glyph image.
     * In pixels, due to gui scaling, we convert it into float.
     * The value should be a multiple of (1 / guiScale)
     * This will be used for drawing size.
     */
    private float width;

    /**
     * The total height of this glyph image.
     * In pixels, due to gui scaling, we convert it into float.
     * The value should be a multiple of (1 / guiScale)
     * This will be used for drawing size.
     */
    private float height;

    /**
     * The horizontal texture coordinate of the upper-left corner.
     */
    private float u1;

    /**
     * The vertical texture coordinate of the upper-left corner.
     */
    private float v1;

    /**
     * The horizontal texture coordinate of the lower-right corner.
     */
    private float u2;

    /**
     * The vertical texture coordinate of the lower-right corner.
     */
    private float v2;

    /**
     * Allocates but not fills the contents
     */
    public TexturedGlyph() {
        renderType = seeThroughType = Sheets.solidBlockSheet();
    }

    public TexturedGlyph(int textureName, float advance, float baselineX, float baselineY, float width, float height, float u1, float v1, float u2, float v2) {
        init(textureName, advance, baselineX, baselineY, width, height, u1, v1, u2, v2);
    }

    public void init(int textureName, float advance, float baselineX, float baselineY, float width, float height, float u1, float v1, float u2, float v2) {
        if (renderType instanceof TextRenderType)
            throw new IllegalStateException("Already initialized");
        renderType = TextRenderType.getOrCacheType(textureName, false);
        seeThroughType = TextRenderType.getOrCacheType(textureName, true);
        this.advance = advance;
        this.baselineX = baselineX;
        this.baselineY = baselineY;
        this.width = width;
        this.height = height;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
    }

    public void drawGlyph(@Nonnull VertexConsumer builder, float x, float y, int r, int g, int b, int a) {
        if (renderType instanceof TextRenderType) {
            RenderSystem.bindTexture(((TextRenderType) renderType).textureName);
            x += baselineX;
            y += baselineY;
            builder.vertex(x, y, 0).color(r, g, b, a).uv(u1, v1).endVertex();
            builder.vertex(x, y + height, 0).color(r, g, b, a).uv(u1, v2).endVertex();
            builder.vertex(x + width, y + height, 0).color(r, g, b, a).uv(u2, v2).endVertex();
            builder.vertex(x + width, y, 0).color(r, g, b, a).uv(u2, v1).endVertex();
        }
    }

    public void drawGlyph(Matrix4f matrix, @Nonnull MultiBufferSource buffer, float x, float y, int r, int g, int b, int a, boolean seeThrough, int packedLight) {
        VertexConsumer builder = buffer.getBuffer(seeThrough ? seeThroughType : renderType);
        x += baselineX;
        y += baselineY;
        builder.vertex(matrix, x, y, 0).color(r, g, b, a).uv(u1, v1).uv2(packedLight).endVertex();
        builder.vertex(matrix, x, y + height, 0).color(r, g, b, a).uv(u1, v2).uv2(packedLight).endVertex();
        builder.vertex(matrix, x + width, y + height, 0).color(r, g, b, a).uv(u2, v2).uv2(packedLight).endVertex();
        builder.vertex(matrix, x + width, y, 0).color(r, g, b, a).uv(u2, v1).uv2(packedLight).endVertex();
    }

    /**
     * The horizontal advance in high-precision pixels of this glyph.
     */
    public float getAdvance() {
        return advance;
    }
}
