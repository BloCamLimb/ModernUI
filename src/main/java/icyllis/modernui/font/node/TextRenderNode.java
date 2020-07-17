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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.glyph.GlyphManager;
import icyllis.modernui.font.process.TextProcessRegister;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The complete node, including
 */
public class TextRenderNode {

    /**
     * Vertical adjustment to string position.
     */
    private static final int BASELINE_OFFSET = 7;


    /**
     * All glyphs to render.
     */
    private final IGlyphRenderInfo[] glyphs;

    /**
     * All effects to render.
     */
    @Nullable
    private final EffectRenderInfo[] effects;

    /**
     * Switch current color
     */
    @Nonnull
    private final ColorStateInfo[] colors;

    /**
     * Total advance of this text node.
     */
    public final float advance;

    public TextRenderNode(IGlyphRenderInfo[] glyphs, @Nullable EffectRenderInfo[] effects, @Nullable ColorStateInfo[] colors, float advance) {
        this.glyphs = glyphs;
        this.effects = effects;
        if (colors == null) {
            this.colors = ColorStateInfo.NO_COLOR_STATE;
        } else {
            this.colors = colors;
        }
        this.advance = advance;
    }

    public float drawText(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a) {
        final float startX = x;
        final int startR = r;
        final int startG = g;
        final int startB = b;
        y += BASELINE_OFFSET;
        x -= GlyphManager.GLYPH_OFFSET;
        RenderSystem.enableTexture();
        int colorIndex = 0;
        ColorStateInfo nextColor = colors[colorIndex];
        for (int glyphIndex = 0; glyphIndex < glyphs.length; glyphIndex++) {
            if (nextColor.glyphIndex == glyphIndex) {
                int color = nextColor.color;
                if (color == TextProcessRegister.NO_COLOR) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = color >> 16 & 0xff;
                    g = color >> 8 & 0xff;
                    b = color & 0xff;
                }
                if (++colorIndex < colors.length) {
                    nextColor = colors[colorIndex];
                }
            }
            x = glyphs[glyphIndex].drawString(builder, raw, x, y, r, g, b, a);
        }
        if (effects != null) {
            RenderSystem.disableTexture();
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (EffectRenderInfo effect : effects) {
                effect.drawEffect(builder, startX, y, startR, startG, startB, a);
            }
            builder.finishDrawing();
            WorldVertexBufferUploader.draw(builder);
        }
        return advance;
    }

    public float drawText(Matrix4f matrix, IRenderTypeBuffer buffer, @Nonnull String raw, float x, float y, int r, int g, int b, int a, int packedLight) {
        final float startX = x;
        final int startR = r;
        final int startG = g;
        final int startB = b;
        if (buffer instanceof IRenderTypeBuffer.Impl) {
            ((IRenderTypeBuffer.Impl) buffer).finish();
        }
        y += 6;
        x -= GlyphManager.GLYPH_OFFSET;
        int colorIndex = 0;
        ColorStateInfo nextColor = colors[colorIndex];
        for (int glyphIndex = 0; glyphIndex < glyphs.length; glyphIndex++) {
            if (nextColor.glyphIndex == glyphIndex) {
                int color = nextColor.color;
                if (color == TextProcessRegister.NO_COLOR) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = color >> 16 & 0xff;
                    g = color >> 8 & 0xff;
                    b = color & 0xff;
                }
                if (++colorIndex < colors.length) {
                    nextColor = colors[colorIndex];
                }
            }
            x = glyphs[glyphIndex].drawString(matrix, buffer, raw, x, y, r, g, b, a, packedLight);
        }
        if (effects != null) {
            if (buffer instanceof IRenderTypeBuffer.Impl) {
                ((IRenderTypeBuffer.Impl) buffer).finish();
            }
            RenderSystem.disableTexture();
            RenderSystem.enableCull();
            BufferBuilder builder = Tessellator.getInstance().getBuffer();
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (EffectRenderInfo effect : effects) {
                effect.drawEffect(builder, startX, y, startR, startG, startB, a);
            }
            builder.finishDrawing();
            WorldVertexBufferUploader.draw(builder);
            RenderSystem.disableCull();
        }
        return advance;
    }
}
