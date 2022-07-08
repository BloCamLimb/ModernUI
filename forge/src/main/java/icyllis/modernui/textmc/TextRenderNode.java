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
import icyllis.modernui.graphics.font.GLBakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * The render node contains all glyph layout information and rendering information.
 */
//TODO emoji, grapheme break
public class TextRenderNode {

    /**
     * Sometimes naive, too simple.
     * <p>
     * This singleton cannot be placed in the cache!
     */
    public static final TextRenderNode EMPTY = new TextRenderNode(new BaseGlyphRender[0], 0, false) {

        @Nonnull
        @Override
        public TextRenderNode get() {
            throw new UnsupportedOperationException("Singleton!");
        }

        @Override
        public boolean tick() {
            throw new UnsupportedOperationException("Singleton!");
        }

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
    public static volatile float sVanillaBaselineOffset = 7;

    /**
     * Time in seconds to recycle this node.
     */
    public static volatile int sLifespan = 12;

    /**
     * All laid-out glyphs and their render info.
     */
    @Nonnull
    public final BaseGlyphRender[] mDGlyphs;

    /**
     * All baked glyphs for rendering, empty glyphs have been removed from this array.
     * The order is visually left-to-right (i.e. in visual order).
     */
    private GLBakedGlyph[] mGlyphs;

    /**
     * Glyphs to relative char indices of the raw string (with formatting codes).
     * Same indexing with {@link #mGlyphs}, visual order to logical order.
     */
    private int[] mStringIndices;

    /**
     * Position x1 y1 x2 y2... relative to the same point, for rendering mGlyphs.
     * Same indexing with {@link #mGlyphs}, align to left, in visual order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    private float[] mPositions;

    /**
     * The length and order are relative to the raw string (with formatting codes).
     * Only grapheme cluster bounds have advances, others are zeros. For example:
     * [13.57, 0, 14.26, 0, 0]. {@link #mGlyphs}.length may less than grapheme cluster
     * count (invisible glyphs are removed). Logical order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    private float[] mAdvances;

    /**
     * Glyph rendering flags. Same indexing with {@link #mGlyphs}, visual order.
     */
    /*
     * lower 24 bits - RGB color, ignored when has USE_PARAM_COLOR bit
     * higher 8 bits
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *     1      OBFUSCATED
     *    1       FORMATTING_CODE
     *   1        FAST_DIGIT
     *  1         USE_PARAM_COLOR
     * |--------|
     */
    private int[] mFlags;

    /**
     * Total advance of this text node.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    public final float mAdvance;

    private final boolean mHasEffect;

    /**
     * Elapsed time in seconds since last use.
     */
    private int mTimer = 0;

    public TextRenderNode(@Nonnull BaseGlyphRender[] glyphs, float advance, boolean hasEffect) {
        mDGlyphs = glyphs;
        mAdvance = advance;
        mHasEffect = hasEffect;
    }

    /**
     * Cache access.
     *
     * @return this with timer reset
     */
    @Nonnull
    public TextRenderNode get() {
        mTimer = 0;
        return this;
    }

    /**
     * Cache access. Increment internal timer by one second.
     *
     * @return true to recycle
     */
    public boolean tick() {
        return ++mTimer > sLifespan;
    }

    public float drawText(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b,
                          int a, float res) {
        if (mDGlyphs.length == 0) {
            return 0;
        }
        final int startR = r;
        final int startG = g;
        final int startB = b;

        y += BASELINE_OFFSET;
        RenderSystem.enableTexture();

        for (BaseGlyphRender glyph : mDGlyphs) {
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
            RenderSystem.disableTexture();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (BaseGlyphRender glyph : mDGlyphs) {
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
        if (mDGlyphs.length == 0) {
            return 0;
        }
        final int startR = r;
        final int startG = g;
        final int startB = b;
        // performance impact
        if (source instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) source).endBatch(Sheets.signSheet());
        }

        y += sVanillaBaselineOffset;

        for (BaseGlyphRender glyph : mDGlyphs) {
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

        if (mHasEffect) {
            r = startR;
            g = startG;
            b = startB;
            builder = source.getBuffer(EffectRenderType.getRenderType(seeThrough));
            for (BaseGlyphRender glyph : mDGlyphs) {
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
            y -= sVanillaBaselineOffset;
            a = colorBackground >>> 24;
            r = colorBackground >> 16 & 0xff;
            g = colorBackground >> 8 & 0xff;
            b = colorBackground & 0xff;
            if (builder == null) {
                builder = source.getBuffer(EffectRenderType.getRenderType(seeThrough));
            }
            builder.vertex(matrix, x - 1, y + 9, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv(0, 1).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + mAdvance + 1, y + 9, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv(1, 1).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + mAdvance + 1, y, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv(1, 0).uv2(packedLight).endVertex();
            builder.vertex(matrix, x - 1, y, TextRenderEffect.EFFECT_DEPTH).color(r, g, b, a).uv(0, 0).uv2(packedLight).endVertex();
        }

        return mAdvance;
    }

    @Override
    public String toString() {
        return "TextRenderNode{" +
                "glyphs=" + Arrays.toString(mDGlyphs) +
                ", advance=" + mAdvance +
                ", hasEffect=" + mHasEffect +
                '}';
    }
}
