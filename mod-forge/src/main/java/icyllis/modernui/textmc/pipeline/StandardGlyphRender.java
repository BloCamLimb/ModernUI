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
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.math.Matrix4f;
import icyllis.modernui.text.TexturedGlyph;
import icyllis.modernui.textmc.CharacterStyleCarrier;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StandardGlyphRender extends GlyphRender {

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
    public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b,
                          int a) {
        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        mGlyph.drawGlyph(builder, x + mOffsetX, y, r, g, b, a);
        builder.end();
        BufferUploader.end(builder);
    }

    @Override
    public void drawGlyph(Matrix4f matrix, @Nonnull MultiBufferSource buffer, @Nonnull CharSequence raw, float x,
                          float y, int r, int g, int b, int a, boolean seeThrough, int light) {
        mGlyph.drawGlyph(matrix, buffer, x + mOffsetX, y, r, g, b, a, seeThrough, light);
    }

    @Override
    public float getAdvance() {
        return mGlyph.getAdvance();
    }

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
