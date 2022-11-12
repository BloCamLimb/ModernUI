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
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;

/**
 * A rendering glyph.
 */
@Deprecated
public abstract class GlyphRender {

    /**
     * Change to params color.
     */
    public static final int USE_INPUT_COLOR = CharacterStyle.IMPLICIT_COLOR_MASK;

    /**
     * Keep the color state not to change.
     */
    public static final int COLOR_NO_CHANGE = 0x40000000;

    /**
     * Constructor assignment is stripIndex, and it will be adjusted to stringIndex later.
     */
    public int mStringIndex;

    /**
     * Offset X to the start of the text, it will be adjusted in RTL layout.
     * Normalized to Minecraft GUI system.
     */
    public float mOffsetX;

    /**
     * Laid-out horizontal advance in context. Normalized to Minecraft GUI system.
     */
    private final float mAdvance;

    /**
     * Rendering flags, will be inserted later.
     *
     * @see CharacterStyle
     */
    /*
     * lower 24 bits - color
     * higher 8 bits
     * |--------|
     *         1  BOLD
     *        1   ITALIC
     *        11  FONT_STYLE
     *       1    UNDERLINE
     *      1     STRIKETHROUGH
     *      11    EFFECT
     *     1      OBFUSCATED
     *     11111  LAYOUT
     *    1       FORMATTING_CODE
     *   1        COLOR_NO_CHANGE (GlyphRender)
     *  1         USE_PARAM_COLOR
     *  1 111111  CHARACTER_STYLE
     * |--------|
     */
    public int mFlags = COLOR_NO_CHANGE;

    public GlyphRender(int stripIndex, float offsetX, float advance, int decoration) {
        mStringIndex = stripIndex;
        mOffsetX = offsetX;
        mAdvance = advance;
        mFlags |= decoration;
    }

    /**
     * Draw the glyph of this info.
     *
     * @param builder vertex builder
     * @param input   needed by {@link DigitGlyphRender}
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     * @param res     resolution level
     */
    public abstract void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r,
                                   int g, int b, int a, float res);

    /**
     * Draw the glyph of this info.
     *
     * @param matrix     matrix
     * @param source     buffer source
     * @param input      needed by {@link DigitGlyphRender}
     * @param x          start x of the whole text
     * @param y          start y of the whole text
     * @param r          final red
     * @param g          final green
     * @param b          final blue
     * @param a          final alpha
     * @param seeThrough if see-through type
     * @param light      packed light
     * @param res        resolution level
     */
    public abstract void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source,
                                   @Nullable CharSequence input, float x, float y, int r, int g, int b, int a,
                                   boolean seeThrough, int light, float res);

    /**
     * Draw the effect of this info
     *
     * @param builder vertex builder
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     */
    public final void drawEffect(@Nonnull VertexConsumer builder, float x, float y, int r, int g, int b, int a) {
        if ((mFlags & CharacterStyle.EFFECT_MASK) != 0) {
            x += mOffsetX;
            if ((mFlags & CharacterStyle.UNDERLINE_MASK) != 0) {
                TextRenderEffect.drawUnderline(builder, x, x + mAdvance, y, r, g, b, a);
            }
            if ((mFlags & CharacterStyle.STRIKETHROUGH_MASK) != 0) {
                TextRenderEffect.drawStrikethrough(builder, x, x + mAdvance, y, r, g, b, a);
            }
        }
    }

    /**
     * Draw the effect of this info.
     *
     * @param matrix  matrix
     * @param builder vertex builder
     * @param x       start x of the whole text
     * @param y       start y of the whole text
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     * @param light   packed light
     */
    public final void drawEffect(@Nonnull Matrix4f matrix, @Nonnull VertexConsumer builder, float x, float y, int r,
                                 int g, int b, int a, int light) {
        if ((mFlags & CharacterStyle.EFFECT_MASK) != 0) {
            x += mOffsetX;
            if ((mFlags & CharacterStyle.UNDERLINE_MASK) != 0) {
                TextRenderEffect.drawUnderline(matrix, builder, x, x + mAdvance, y, r, g, b, a, light);
            }
            if ((mFlags & CharacterStyle.STRIKETHROUGH_MASK) != 0) {
                TextRenderEffect.drawStrikethrough(matrix, builder, x, x + mAdvance, y, r, g, b, a, light);
            }
        }
    }

    /**
     * Get the glyph advance of this info
     *
     * @return advance
     */
    public float getAdvance() {
        return mAdvance;
    }

    @Override
    public String toString() {
        return "BaseGlyphRender{" +
                "stringIndex=" + mStringIndex +
                ", offsetX=" + mOffsetX +
                ", advance=" + mAdvance +
                ", flags=0x" + Integer.toHexString(mFlags) +
                '}';
    }

    @Deprecated
    static
    class StandardGlyphRender extends GlyphRender {

        /**
         * The immutable glyph to render
         */
        @Nullable
        private final GLBakedGlyph mGlyph;

        public StandardGlyphRender(int stripIndex, float offsetX, float advance, int decoration,
                                   @Nullable GLBakedGlyph glyph) {
            super(stripIndex, offsetX, advance, decoration);
            mGlyph = glyph;
        }

        @Override
        public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g,
                              int b, int a, float res) {
            GLBakedGlyph glyph = mGlyph;
            if (glyph == null) {
                return;
            }
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            RenderSystem.bindTexture(glyph.texture);
            x += mOffsetX;
            final float w;
            final float h;
            if (TextLayoutProcessor.sAlignPixels) {
                x += Math.round(glyph.x * res) / res;
                y += Math.round(glyph.y * res) / res;
                w = Math.round(glyph.width * res) / res;
                h = Math.round(glyph.height * res) / res;
            } else {
                x += glyph.x / res;
                y += glyph.y / res;
                w = glyph.width / res;
                h = glyph.height / res;
            }
            builder.vertex(x, y, 0).color(r, g, b, a).uv(glyph.u1, glyph.v1).endVertex();
            builder.vertex(x, y + h, 0).color(r, g, b, a).uv(glyph.u1, glyph.v2).endVertex();
            builder.vertex(x + w, y + h, 0).color(r, g, b, a).uv(glyph.u2, glyph.v2).endVertex();
            builder.vertex(x + w, y, 0).color(r, g, b, a).uv(glyph.u2, glyph.v1).endVertex();
            BufferUploader.drawWithShader(builder.end());
        }

        @Override
        public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable CharSequence input,
                              float x, float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
            GLBakedGlyph glyph = mGlyph;
            if (glyph == null) {
                return;
            }
            VertexConsumer builder = source.getBuffer(TextRenderType.getOrCreate(glyph.texture,
                    seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL));
            x += mOffsetX;
            x += glyph.x / res;
            y += glyph.y / res;
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

    /**
     * The key to fast render digit. When given text is String, this is enabled to draw numbers.
     *
     * @see VanillaLayoutKey
     */
    @Deprecated
    static
    class DigitGlyphRender extends GlyphRender {

        /**
         * A reference of cached array in GlyphManager, 0-9 textured glyphs (in that order)
         */
        @Nonnull
        private final Map.Entry<GLBakedGlyph[], float[]> mDigits;

        public DigitGlyphRender(int stripIndex, float offsetX, float advance, int decoration,
                                @Nonnull Map.Entry<GLBakedGlyph[], float[]> digits) {
            super(stripIndex, offsetX, advance, decoration);
            mDigits = digits;
        }

        @Override
        public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g,
                              int b, int a, float res) {
            int idx = input.charAt(mStringIndex) - '0';
            if (idx < 0 || idx >= 10)
                return;
            GLBakedGlyph glyph = mDigits.getKey()[idx];
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            RenderSystem.bindTexture(glyph.texture);
            x += mOffsetX;
            // 0 is standard, no need to offset
            if (idx != 0) {
                x += mDigits.getValue()[idx];
            }
            final float w;
            final float h;
            if (TextLayoutProcessor.sAlignPixels) {
                x += Math.round(glyph.x * res) / res;
                y += Math.round(glyph.y * res) / res;
                w = Math.round(glyph.width * res) / res;
                h = Math.round(glyph.height * res) / res;
            } else {
                x += glyph.x / res;
                y += glyph.y / res;
                w = glyph.width / res;
                h = glyph.height / res;
            }
            builder.vertex(x, y, 0).color(r, g, b, a).uv(glyph.u1, glyph.v1).endVertex();
            builder.vertex(x, y + h, 0).color(r, g, b, a).uv(glyph.u1, glyph.v2).endVertex();
            builder.vertex(x + w, y + h, 0).color(r, g, b, a).uv(glyph.u2, glyph.v2).endVertex();
            builder.vertex(x + w, y, 0).color(r, g, b, a).uv(glyph.u2, glyph.v1).endVertex();
            BufferUploader.drawWithShader(builder.end());
        }

        @Override
        public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable CharSequence input,
                              float x, float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
            int idx = input != null ? input.charAt(mStringIndex) - '0' : 0;
            if (idx < 0 || idx >= 10)
                return;
            GLBakedGlyph glyph = mDigits.getKey()[idx];
            VertexConsumer builder = source.getBuffer(TextRenderType.getOrCreate(glyph.texture,
                    seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL));
            x += mOffsetX;
            // 0 is standard, no need to offset
            if (idx != 0) {
                x += mDigits.getValue()[idx];
            }
            x += glyph.x / res;
            y += glyph.y / res;
            final float w = glyph.width / res;
            final float h = glyph.height / res;
            builder.vertex(matrix, x, y, 0).color(r, g, b, a).uv(glyph.u1, glyph.v1).uv2(light).endVertex();
            builder.vertex(matrix, x, y + h, 0).color(r, g, b, a).uv(glyph.u1, glyph.v2).uv2(light).endVertex();
            builder.vertex(matrix, x + w, y + h, 0).color(r, g, b, a).uv(glyph.u2, glyph.v2).uv2(light).endVertex();
            builder.vertex(matrix, x + w, y, 0).color(r, g, b, a).uv(glyph.u2, glyph.v1).uv2(light).endVertex();
        }

        /*@Override
        public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g,
        int b,
                              int a, float res) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            mDigits[input.charAt(mStringIndex) - 48].drawGlyph(builder, x + mOffsetX, y, r, g, b, a);
            builder.end();
            BufferUploader.end(builder);
        }

        @Override
        public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nonnull CharSequence input,
        float x,
                              float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
            mDigits[input.charAt(mStringIndex) - 48].drawGlyph(matrix, source, x + mOffsetX, y, r, g, b, a, seeThrough,
                    light);
        }

        @Override
        public float getAdvance() {
            return mDigits[0].getAdvance();
        }*/

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

    @Deprecated
    static
    class RandomGlyphRender extends GlyphRender {

        private static final Random RANDOM = new Random();

        /**
         * Array of glyphs with same advance
         */
        @Nonnull
        private final Map.Entry<GLBakedGlyph[], float[]> mGlyphs;

        public RandomGlyphRender(int stripIndex, float offsetX, float advance, int decoration,
                                 @Nonnull Map.Entry<GLBakedGlyph[], float[]> glyphs) {
            super(stripIndex, advance, offsetX, decoration);
            mGlyphs = glyphs;
        }

        @Override
        public void drawGlyph(@Nonnull BufferBuilder builder, @Nonnull String input, float x, float y, int r, int g,
                              int b, int a, float res) {
            int idx = RANDOM.nextInt(mGlyphs.getKey().length);
            GLBakedGlyph glyph = mGlyphs.getKey()[idx];
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            RenderSystem.bindTexture(glyph.texture);
            x += mOffsetX;
            // 0 is standard, no need to offset
            if (idx != 0) {
                x += mGlyphs.getValue()[idx];
            }
            final float w;
            final float h;
            if (TextLayoutProcessor.sAlignPixels) {
                x += Math.round(glyph.x * res) / res;
                y += Math.round(glyph.y * res) / res;
                w = Math.round(glyph.width * res) / res;
                h = Math.round(glyph.height * res) / res;
            } else {
                x += glyph.x / res;
                y += glyph.y / res;
                w = glyph.width / res;
                h = glyph.height / res;
            }
            builder.vertex(x, y, 0).color(r, g, b, a).uv(glyph.u1, glyph.v1).endVertex();
            builder.vertex(x, y + h, 0).color(r, g, b, a).uv(glyph.u1, glyph.v2).endVertex();
            builder.vertex(x + w, y + h, 0).color(r, g, b, a).uv(glyph.u2, glyph.v2).endVertex();
            builder.vertex(x + w, y, 0).color(r, g, b, a).uv(glyph.u2, glyph.v1).endVertex();
            BufferUploader.drawWithShader(builder.end());
        }

        @Override
        public void drawGlyph(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable CharSequence input,
                              float x, float y, int r, int g, int b, int a, boolean seeThrough, int light, float res) {
            int idx = RANDOM.nextInt(mGlyphs.getKey().length);
            GLBakedGlyph glyph = mGlyphs.getKey()[idx];
            VertexConsumer builder = source.getBuffer(TextRenderType.getOrCreate(glyph.texture,
                    seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL));
            x += mOffsetX;
            // 0 is standard, no need to offset
            if (idx != 0) {
                // already normalized to resolution
                x += mGlyphs.getValue()[idx];
            }
            x += glyph.x / res;
            y += glyph.y / res;
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
            glyphs[RANDOM.nextInt(glyphs.length)].drawGlyph(matrix, source, x + mOffsetX, y, r, g, b, a, seeThrough,
            light);
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
}
