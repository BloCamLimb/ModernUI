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

import icyllis.modernui.font.glyph.TexturedGlyph;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * The key to fast render digit
 */
public class DigitGlyphInfo extends GlyphRenderInfo {

    /**
     * A reference of cached array in GlyphManager, 0-9 textured glyphs (in that order)
     */
    private final TexturedGlyph[] glyphs;

    public DigitGlyphInfo(TexturedGlyph[] glyphs, TextRenderEffect effect, int stringIndex, float offsetX) {
        super(effect, stringIndex, offsetX);
        this.glyphs = glyphs;
    }

    @Override
    public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a) {
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        glyphs[raw.charAt(stringIndex) - 48].drawGlyph(builder, x + offsetX, y, r, g, b, a);
        builder.finishDrawing();
        WorldVertexBufferUploader.draw(builder);
    }

    @Override
    public void drawGlyph(Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, @Nonnull String raw, float x, float y, int r, int g, int b, int a, boolean transparent, int light) {
        glyphs[raw.charAt(stringIndex) - 48].drawGlyph(matrix, buffer, x + offsetX, y, r, g, b, a, transparent, light);
    }

    @Override
    public float getAdvance() {
        return glyphs[0].advance;
    }

    /*@Override
    public float drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, int color, float x, float y, int r, int g, int b, int a) {
        if (this.color != -1) {
            r = this.color >> 16 & 0xff;
            g = this.color >> 8 & 0xff;
            b = this.color & 0xff;
        }
        for (int i : indexArray) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            x = glyphs[raw.charAt(i) - '0'].drawGlyph(builder, x, y, r, g, b, a);
            builder.finishDrawing();
            WorldVertexBufferUploader.draw(builder);
        }
        return x;
    }*/
}
