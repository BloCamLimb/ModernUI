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
import icyllis.modernui.font.glyph.TexturedGlyph;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class StringRenderInfo {

    private final TexturedGlyph[] glyphs;

    /**
     * RGB color
     */
    private final int color;

    private final boolean obfuscated;

    public StringRenderInfo(TexturedGlyph[] glyphs, int color, boolean obfuscated) {
        this.glyphs = glyphs;
        this.color = color;
        this.obfuscated = obfuscated;
    }

    public float drawString(@Nonnull BufferBuilder builder, float x, float y, int r, int g, int b, int a) {
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
}
