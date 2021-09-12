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

package icyllis.modernui.textmc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import icyllis.modernui.text.TexturedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StandardGlyphRender extends BaseGlyphRender {

    /**
     * The immutable glyph to render
     */
    @Nullable
    private final TexturedGlyph mGlyph;

    public StandardGlyphRender(int stripIndex, float offsetX, float advance, int decoration,
                               @Nullable TexturedGlyph glyph) {
        super(stripIndex, offsetX, advance, decoration);
        mGlyph = glyph;
    }

    @Override
    public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g,
                          int b, int a, float res) {
        TexturedGlyph glyph = mGlyph;
        if (glyph == null) {
            return;
        }
        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        RenderSystem.bindTexture(glyph.texture);
        x += mOffsetX;
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
    public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nonnull CharSequence input,
                          float x, float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
        TexturedGlyph glyph = mGlyph;
        if (glyph == null) {
            return;
        }
        VertexConsumer builder = source.getBuffer(TextRenderType.getOrCreate(glyph.texture, seeThrough));
        x += mOffsetX;
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
    public float getAdvance() {
        return mGlyph.getAdvance();
    }*/

    /*public float drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, int color, float x, float y, int
    r, int g, int b, int a) {
        if (color != -1) {
            r = color >> 16 & 0xff;
            g = color >> 8 & 0xff;
            b = color & 0xff;
        }
        for (TexturedGlyph glyph : glyphs) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            x = glyph.drawGlyph(builder, x, y, r, g, b, a);
            builder.finishDrawing();
            WorldVertexBufferUploader.draw(builder);
        }
        return x;
    }

    @Nonnull
    public static CodePointInfo ofText(TexturedGlyph[] glyphs, int color) {
        return new CodePointInfo(glyphs, color);
    }

    @Nonnull
    public static DigitRenderInfo ofDigit(TexturedGlyph[] digits, int color, int[] indexMap) {
        return new DigitRenderInfo(digits, color, indexMap);
    }

    @Nonnull
    public static ObfuscatedInfo ofObfuscated(TexturedGlyph[] digits, int color, int count) {
        return new ObfuscatedInfo(digits, color, count);
    }*/
}
