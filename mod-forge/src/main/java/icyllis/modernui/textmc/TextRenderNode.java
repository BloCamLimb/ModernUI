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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * The complete node that including layout results and rendering information.
 */
public class TextRenderNode {

    /**
     * Sometimes naive, too simple.
     * In this case, input string must be empty.
     */
    public static final TextRenderNode EMPTY = new TextRenderNode(new BaseGlyphRender[0], 0, false) {

        @Override
        public float drawText(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g,
                              int b, int a, float res) {
            return 0;
        }

        @Override
        public float drawText(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable CharSequence raw,
                              float x, float y, int r, int g, int b, int a, boolean isShadow, boolean seeThrough,
                              int colorBackground, int packedLight, float res) {
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

    private static final int MAX_LIFESPAN_TICKS = 256; // 12.8 s


    /**
     * All laid-out glyphs and their render info.
     */
    @Nonnull
    public final BaseGlyphRender[] mGlyphs;

    /**
     * Total advance of this text node.
     * Normalized to Minecraft GUI system.
     */
    public final float mAdvance;

    private final boolean mHasEffect;

    transient int mLifespan = MAX_LIFESPAN_TICKS;

    public TextRenderNode(@Nonnull BaseGlyphRender[] glyphs, float advance, boolean hasEffect) {
        mGlyphs = glyphs;
        mAdvance = advance;
        mHasEffect = hasEffect;
    }

    @Nonnull
    public TextRenderNode get() {
        mLifespan = MAX_LIFESPAN_TICKS;
        return this;
    }

    public boolean tick() {
        return --mLifespan < 0;
    }

    public float drawText(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b,
                          int a, float res) {
        final int startR = r;
        final int startG = g;
        final int startB = b;

        y += BASELINE_OFFSET;
        x -= 1;
        RenderSystem.enableTexture();

        for (BaseGlyphRender glyph : mGlyphs) {
            if ((glyph.mFlags & BaseGlyphRender.COLOR_NO_CHANGE) == 0) {
                int color = glyph.mFlags;
                if ((color & BaseGlyphRender.USE_INPUT_COLOR) != 0) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = color >> 16 & 0xff;
                    g = color >> 8 & 0xff;
                    b = color & 0xff;
                }
            }
            glyph.drawGlyph(builder, raw, x, y, r, g, b, a, res);
        }

        if (mHasEffect) {
            r = startR;
            g = startG;
            b = startB;
            x += 1;
            RenderSystem.disableTexture();
            builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (BaseGlyphRender glyph : mGlyphs) {
                if ((glyph.mFlags & BaseGlyphRender.COLOR_NO_CHANGE) == 0) {
                    int color = glyph.mFlags;
                    if ((color & BaseGlyphRender.USE_INPUT_COLOR) != 0) {
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
        return mAdvance;
    }

    public float drawText(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable CharSequence raw,
                          float x, float y, int r, int g, int b, int a, boolean isShadow, boolean seeThrough,
                          int colorBackground, int packedLight, float res) {
        final int startR = r;
        final int startG = g;
        final int startB = b;
        // performance impact
        if (source instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) source).endBatch(Sheets.signSheet());
        }

        y += VANILLA_BASELINE_OFFSET;
        x -= 1;

        for (BaseGlyphRender glyph : mGlyphs) {
            if ((glyph.mFlags & BaseGlyphRender.COLOR_NO_CHANGE) == 0) {
                int color = glyph.mFlags;
                if ((color & BaseGlyphRender.USE_INPUT_COLOR) != 0) {
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
            glyph.drawGlyph(matrix, source, raw, x, y, r, g, b, a, seeThrough, packedLight, res);
        }

        VertexConsumer builder = null;
        x += 1;

        if (mHasEffect) {
            r = startR;
            g = startG;
            b = startB;
            builder = source.getBuffer(EffectRenderType.getRenderType(seeThrough));
            for (BaseGlyphRender glyph : mGlyphs) {
                if ((glyph.mFlags & BaseGlyphRender.COLOR_NO_CHANGE) == 0) {
                    int color = glyph.mFlags;
                    if ((color & BaseGlyphRender.USE_INPUT_COLOR) != 0) {
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

        if ((colorBackground & 0xFF000000) != 0) {
            y -= VANILLA_BASELINE_OFFSET;
            a = colorBackground >>> 24;
            r = colorBackground >> 16 & 0xff;
            g = colorBackground >> 8 & 0xff;
            b = colorBackground & 0xff;
            if (builder == null) {
                builder = source.getBuffer(EffectRenderType.getRenderType(seeThrough));
            }
            builder.vertex(matrix, x - 1, y + 9, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + mAdvance + 1, y + 9, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + mAdvance + 1, y, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
            builder.vertex(matrix, x - 1, y, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv2(packedLight).endVertex();
        }

        return mAdvance;
    }

    @Override
    public String toString() {
        return "TextRenderNode{" +
                "mGlyphs=" + Arrays.toString(mGlyphs) +
                ", mAdvance=" + mAdvance +
                ", mHasEffect=" + mHasEffect +
                '}';
    }
}
