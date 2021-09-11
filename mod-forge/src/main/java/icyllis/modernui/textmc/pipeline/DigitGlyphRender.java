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

/**
 * The key to fast render digit
 */
public class DigitGlyphRender extends GlyphRender {

    /**
     * A reference of cached array in GlyphManager, 0-9 textured glyphs (in that order)
     */
    private final TexturedGlyph[] mDigits;

    public DigitGlyphRender(int stripIndex, float offsetX, float advance, int decoration,
                            @Nonnull TexturedGlyph[] digits) {
        super(stripIndex, offsetX, advance, decoration);
        mDigits = digits;
    }

    @Override
    public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b,
                          int a) {
        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        mDigits[raw.charAt(mStringIndex) - 48].drawGlyph(builder, x + mOffsetX, y, r, g, b, a);
        builder.end();
        BufferUploader.end(builder);
    }

    @Override
    public void drawGlyph(Matrix4f matrix, @Nonnull MultiBufferSource buffer, @Nonnull CharSequence raw, float x,
                          float y, int r, int g, int b, int a, boolean seeThrough, int light) {
        mDigits[raw.charAt(mStringIndex) - 48].drawGlyph(matrix, buffer, x + mOffsetX, y, r, g, b, a, seeThrough,
                light);
    }

    @Override
    public float getAdvance() {
        return mDigits[0].getAdvance();
    }

    /*@Override
    public float drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, int color, float x, float y, int r,
    int g, int b, int a) {
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
