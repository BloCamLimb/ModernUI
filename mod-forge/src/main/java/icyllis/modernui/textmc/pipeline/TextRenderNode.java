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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import icyllis.modernui.textmc.GlyphManagerForge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * The complete node, including final rendering results and layout information
 */
public class TextRenderNode {

    /**
     * Sometimes naive, too simple
     */
    public static final TextRenderNode EMPTY = new TextRenderNode(new GlyphRender[0], 0, false) {

        @Override
        public float drawText(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a) {
            return 0;
        }

        @Override
        public float drawText(Matrix4f matrix, MultiBufferSource buffer, @Nonnull CharSequence raw, float x, float y,
                              int r, int g, int b, int a, boolean isShadow, boolean seeThrough, int colorBackground, int packedLight) {
            return 0;
        }
    };

    /**
     * Vertical adjustment to string position.
     */
    public static final int BASELINE_OFFSET = 7;

    /**
     * Vertical adjustment to string position.
     */
    public static final int VANILLA_BASELINE_OFFSET = 6;


    /**
     * All glyphs to render.
     */
    public final GlyphRender[] glyphs;

    /*
     * Switch current color
     */
    //private final ColorStateInfo[] colors;

    /**
     * Total advance of this text node.
     */
    public final float advance;

    private final boolean hasEffect;

    public TextRenderNode(GlyphRender[] glyphs, float advance, boolean hasEffect) {
        this.glyphs = glyphs;
        //this.colors = colors;
        this.advance = advance;
        this.hasEffect = hasEffect;
    }

    public float drawText(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a) {
        final int startR = r;
        final int startG = g;
        final int startB = b;

        y += BASELINE_OFFSET;
        x -= GlyphManagerForge.GLYPH_OFFSET;
        RenderSystem.enableTexture();

        for (GlyphRender glyph : glyphs) {
            if (glyph.color != GlyphRender.COLOR_NO_CHANGE) {
                int color = glyph.color;
                if (color == GlyphRender.USE_INPUT_COLOR) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = color >> 16 & 0xff;
                    g = color >> 8 & 0xff;
                    b = color & 0xff;
                }
            }
            glyph.drawGlyph(builder, raw, x, y, r, g, b, a);
        }

        if (hasEffect) {
            r = startR;
            g = startG;
            b = startB;
            x += GlyphManagerForge.GLYPH_OFFSET;
            RenderSystem.disableTexture();
            builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (GlyphRender glyph : glyphs) {
                if (glyph.color != GlyphRender.COLOR_NO_CHANGE) {
                    int color = glyph.color;
                    if (color == GlyphRender.USE_INPUT_COLOR) {
                        r = startR;
                        g = startG;
                        b = startB;
                    } else {
                        r = color >> 16 & 0xff;
                        g = color >> 8 & 0xff;
                        b = color & 0xff;
                    }
                }
                glyph.drawEffect(builder, x, y, r, g, b, a);
            }
            builder.end();
            BufferUploader.end(builder);
        }
        return advance;
    }

    public float drawText(Matrix4f matrix, MultiBufferSource buffer, @Nonnull CharSequence raw, float x, float y,
                          int r, int g, int b, int a, boolean isShadow, boolean seeThrough, int colorBackground, int packedLight) {
        final int startR = r;
        final int startG = g;
        final int startB = b;
        //TODO mixin GameRenderer to disable depth test
        if (buffer instanceof MultiBufferSource.BufferSource)
            ((MultiBufferSource.BufferSource) buffer).endBatch(Sheets.signSheet());

        y += VANILLA_BASELINE_OFFSET;
        x -= GlyphManagerForge.GLYPH_OFFSET;

        for (GlyphRender glyph : glyphs) {
            if (glyph.color != GlyphRender.COLOR_NO_CHANGE) {
                int color = glyph.color;
                if (color == GlyphRender.USE_INPUT_COLOR) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = color >> 16 & 0xff;
                    g = color >> 8 & 0xff;
                    b = color & 0xff;
                    if (isShadow) {
                        r >>= 2;
                        g >>= 2;
                        b >>= 2;
                    }
                }
            }
            glyph.drawGlyph(matrix, buffer, raw, x, y, r, g, b, a, seeThrough, packedLight);
        }

        VertexConsumer builder = null;
        x += GlyphManagerForge.GLYPH_OFFSET;

        if (hasEffect) {
            r = startR;
            g = startG;
            b = startB;
            builder = buffer.getBuffer(EffectRenderType.getRenderType(seeThrough));
            for (GlyphRender glyph : glyphs) {
                if (glyph.color != GlyphRender.COLOR_NO_CHANGE) {
                    int color = glyph.color;
                    if (color == GlyphRender.USE_INPUT_COLOR) {
                        r = startR;
                        g = startG;
                        b = startB;
                    } else {
                        r = color >> 16 & 0xff;
                        g = color >> 8 & 0xff;
                        b = color & 0xff;
                        if (isShadow) {
                            r >>= 2;
                            g >>= 2;
                            b >>= 2;
                        }
                    }
                }
                glyph.drawEffect(matrix, builder, x, y, r, g, b, a, packedLight);
            }
        }

        if (colorBackground != 0) {
            y -= VANILLA_BASELINE_OFFSET;
            a = colorBackground >> 24 & 0xff;
            r = colorBackground >> 16 & 0xff;
            g = colorBackground >> 8 & 0xff;
            b = colorBackground & 0xff;
            if (builder == null)
                builder = buffer.getBuffer(EffectRenderType.getRenderType(seeThrough));
            builder.vertex(matrix, x - 1, y + 9, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + advance + 1, y + 9, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + advance + 1, y, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
            builder.vertex(matrix, x - 1, y, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
        }

        return advance;
    }
}
