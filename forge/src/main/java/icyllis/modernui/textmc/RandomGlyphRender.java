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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import icyllis.modernui.text.TexturedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

class RandomGlyphRender extends BaseGlyphRender {

    private static final Random RANDOM = new Random();

    /**
     * Array of glyphs with same advance
     */
    @Nonnull
    private final Pair<TexturedGlyph[], float[]> mGlyphs;

    public RandomGlyphRender(int stripIndex, float offsetX, float advance, int decoration,
                             @Nonnull Pair<TexturedGlyph[], float[]> glyphs) {
        super(stripIndex, advance, offsetX, decoration);
        mGlyphs = glyphs;
    }

    @Override
    public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g,
                          int b, int a, float res) {
        int idx = RANDOM.nextInt(10);
        TexturedGlyph glyph = mGlyphs.getLeft()[idx];
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        RenderSystem.bindTexture(glyph.texture);
        x += mOffsetX;
        if (idx != 0) {
            x += mGlyphs.getRight()[idx];
        }
        x += glyph.offsetX / res;
        y += glyph.offsetY / res;
        final float w = glyph.width / res;
        final float h = glyph.height / res;
        builder.vertex(x, y, 0).color(r, g, b, a).uv(glyph.u1, glyph.v1).endVertex();
        builder.vertex(x, y + h, 0).color(r, g, b, a).uv(glyph.u1, glyph.v2).endVertex();
        builder.vertex(x + w, y + h, 0).color(r, g, b, a).uv(glyph.u2, glyph.v2).endVertex();
        builder.vertex(x + w, y, 0).color(r, g, b, a).uv(glyph.u2, glyph.v1).endVertex();
        builder.end();
        BufferUploader.end(builder);
    }

    @Override
    public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable CharSequence input,
                          float x, float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
        int idx = RANDOM.nextInt(10);
        TexturedGlyph glyph = mGlyphs.getLeft()[idx];
        VertexConsumer builder = source.getBuffer(TextRenderType.getOrCreate(glyph.texture, seeThrough));
        x += mOffsetX;
        if (idx != 0) {
            x += mGlyphs.getRight()[idx];
        }
        x += glyph.offsetX / res;
        y += glyph.offsetY / res;
        final float w = glyph.width / res;
        final float h = glyph.height / res;
        builder.vertex(matrix, x, y, 0).color(r, g, b, a).uv(glyph.u1, glyph.v1).uv2(light).endVertex();
        builder.vertex(matrix, x, y + h, 0).color(r, g, b, a).uv(glyph.u1, glyph.v2).uv2(light).endVertex();
        builder.vertex(matrix, x + w, y + h, 0).color(r, g, b, a).uv(glyph.u2, glyph.v2).uv2(light).endVertex();
        builder.vertex(matrix, x + w, y, 0).color(r, g, b, a).uv(glyph.u2, glyph.v1).uv2(light).endVertex();
    }

    /*@Override
    public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g, int
    b, int a, float res) {
        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        glyphs[RANDOM.nextInt(glyphs.length)].drawGlyph(builder, x + mOffsetX, y, r, g, b, a);
        builder.end();
        BufferUploader.end(builder);
    }

    @Override
    public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nonnull CharSequence input,
    float x, float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
        glyphs[RANDOM.nextInt(glyphs.length)].drawGlyph(matrix, source, x + mOffsetX, y, r, g, b, a, seeThrough, light);
    }

    @Override
    public float getAdvance() {
        return glyphs[0].getAdvance();
    }*/

    /*@Override
    public float drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, int color, float x, float y, int r,
    int g, int b, int a) {
        if (this.color != -1) {
            r = this.color >> 16 & 0xff;
            g = this.color >> 8 & 0xff;
            b = this.color & 0xff;
        }
        for (int i = 0; i < count; i++) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            x = glyphs[RANDOM.nextInt(10)].drawGlyph(builder, x, y, r, g, b, a);
            builder.finishDrawing();
            WorldVertexBufferUploader.draw(builder);
        }
        return x;
    }*/
}
