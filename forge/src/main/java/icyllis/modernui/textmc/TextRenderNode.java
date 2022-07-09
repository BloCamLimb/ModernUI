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

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import icyllis.modernui.graphics.font.GLBakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * The render node contains all glyph layout information and rendering information.
 */
public class TextRenderNode {

    private static final Random RANDOM = new Random();

    /**
     * Sometimes naive, too simple.
     * <p>
     * This singleton cannot be placed in the cache!
     */
    public static final TextRenderNode EMPTY = new TextRenderNode(
            new GLBakedGlyph[0], new int[0], new float[0], new float[0], new int[0], new int[0], 0, false) {
        @Nonnull
        @Override
        public TextRenderNode get() {
            throw new UnsupportedOperationException("Singleton!");
        }

        @Override
        public boolean tick(int lifespan) {
            throw new UnsupportedOperationException("Singleton!");
        }

        @Override
        public float drawText(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable String raw,
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
    public static volatile float sBaselineOffset = BASELINE_OFFSET;

    /**
     * All baked glyphs for rendering, empty glyphs have been removed from this array.
     * The order is visually left-to-right (i.e. in visual order).
     */
    private final GLBakedGlyph[] mGlyphs;

    /**
     * Glyphs to relative char indices of the strip string (without formatting codes).
     * For vanilla layout ({@link VanillaTextKey} and {@link TextLayoutEngine#lookupVanillaNode(String)}),
     * these will be adjusted to string index (with formatting codes).
     * Same indexing with {@link #mGlyphs}, in visual order.
     */
    private final int[] mCharIndices;

    /**
     * Position x1 y1 x2 y2... relative to the same point, for rendering glyphs.
     * Same indexing with {@link #mGlyphs}, align to left, in visual order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    private final float[] mPositions;

    /**
     * The length and order are relative to the raw string (with formatting codes).
     * Only grapheme cluster bounds have advances, others are zeros. For example:
     * [13.57, 0, 14.26, 0, 0]. {@link #mGlyphs}.length may less than grapheme cluster
     * count (invisible glyphs are removed). Logical order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    private final float[] mAdvances;

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
     *   1        FAST_DIGIT_REPLACEMENT
     *  1         NO_COLOR_SPECIFIED
     * |--------|
     */
    /**
     * Glyph rendering flags. Same indexing with {@link #mGlyphs}, in visual order.
     */
    private final int[] mFlags;

    /**
     * Strip indices that are boundaries for Unicode line breaking, in logical order.
     * 0 is not included. Last value is always the text length (without formatting codes).
     */
    private final int[] mLineBoundaries;

    /**
     * Total advance of this text node.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    public final float mAdvance;

    /**
     * Precomputed value that indicates whether flags array contains any text effect flag.
     */
    private final boolean mHasEffect;

    /**
     * Elapsed time in seconds since last use.
     */
    private transient int mTimer = 0;

    private TextRenderNode(@Nonnull TextRenderNode node) {
        mGlyphs = node.mGlyphs;
        mCharIndices = node.mCharIndices;
        mPositions = node.mPositions;
        mAdvances = node.mAdvances;
        mFlags = node.mFlags;
        mLineBoundaries = node.mLineBoundaries;
        mAdvance = node.mAdvance;
        mHasEffect = node.mHasEffect;
    }

    public TextRenderNode(@Nonnull GLBakedGlyph[] glyphs, @Nonnull int[] charIndices, @Nonnull float[] positions,
                          @Nonnull float[] advances, @Nonnull int[] flags, @Nonnull int[] lineBoundaries,
                          float advance, boolean hasEffect) {
        mGlyphs = glyphs;
        mCharIndices = charIndices;
        mPositions = positions;
        mAdvances = advances;
        mFlags = flags;
        mLineBoundaries = lineBoundaries;
        mAdvance = advance;
        mHasEffect = hasEffect;
        assert mGlyphs.length != mCharIndices.length;
        assert mGlyphs.length * 2 != mPositions.length;
        assert mGlyphs.length != mFlags.length;
    }

    /**
     * Make a new empty node. For those have no rendering info but store them into cache.
     *
     * @return a new empty node
     */
    @Nonnull
    public static TextRenderNode makeEmpty() {
        return new TextRenderNode(EMPTY);
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
    public boolean tick(int lifespan) {
        return ++mTimer > lifespan;
    }

    public float drawText(@Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, @Nullable String raw,
                          float x, float y, int r, int g, int b, int a, boolean isShadow, boolean seeThrough,
                          int colorBackground, int packedLight, float res) {
        if (mGlyphs.length == 0) {
            return mAdvance;
        }
        final int startR = r;
        final int startG = g;
        final int startB = b;

        y += sBaselineOffset;

        int texture = -1;
        VertexConsumer builder = null;

        for (int i = 0; i < mGlyphs.length; i++) {
            GLBakedGlyph glyph = mGlyphs[i];
            final int flags = mFlags[i];
            if ((flags & CharacterStyle.NO_COLOR_SPECIFIED) != 0) {
                r = startR;
                g = startG;
                b = startB;
            } else {
                r = flags >> 16 & 0xff;
                g = flags >> 8 & 0xff;
                b = flags & 0xff;
                if (isShadow) {
                    r >>= 2;
                    g >>= 2;
                    b >>= 2;
                }
            }
            final float rx;
            final float ry;
            if (raw != null && (flags & CharacterStyle.FAST_DIGIT_REPLACEMENT) != 0) {
                var chars = (TextLayoutEngine.FastCharSet) glyph;
                int fastIndex = raw.charAt(mCharIndices[i]) - '0';
                if (fastIndex < 0 || fastIndex > 9) {
                    continue;
                }
                glyph = chars.glyphs[fastIndex];
                if (fastIndex != 0) {
                    rx = x + mPositions[i << 1] + glyph.x / res + chars.offsets[fastIndex];
                } else {
                    // 0 is standard, no additional offset
                    rx = x + mPositions[i << 1] + glyph.x / res;
                }
            } else if ((flags & CharacterStyle.OBFUSCATED) != 0) {
                var chars = (TextLayoutEngine.FastCharSet) glyph;
                int fastIndex = RANDOM.nextInt(chars.glyphs.length);
                glyph = chars.glyphs[fastIndex];
                if (fastIndex != 0) {
                    rx = x + mPositions[i << 1] + glyph.x / res + chars.offsets[fastIndex];
                } else {
                    // 0 is standard, no additional offset
                    rx = x + mPositions[i << 1] + glyph.x / res;
                }
            } else {
                rx = x + mPositions[i << 1] + glyph.x / res;
            }
            ry = y + mPositions[(i << 1) + 1] + glyph.y / res;
            final float w = glyph.width / res;
            final float h = glyph.height / res;
            if (texture != glyph.texture) {
                texture = glyph.texture;
                builder = source.getBuffer(TextRenderType.getOrCreate(texture, seeThrough));
            }
            assert builder != null;
            builder.vertex(matrix, rx, ry, 0)
                    .color(r, g, b, a)
                    .uv(glyph.u1, glyph.v1)
                    .uv2(packedLight)
                    .endVertex();
            builder.vertex(matrix, rx, ry + h, 0)
                    .color(r, g, b, a)
                    .uv(glyph.u1, glyph.v2)
                    .uv2(packedLight)
                    .endVertex();
            builder.vertex(matrix, rx + w, ry + h, 0)
                    .color(r, g, b, a)
                    .uv(glyph.u2, glyph.v2)
                    .uv2(packedLight)
                    .endVertex();
            builder.vertex(matrix, rx + w, ry, 0)
                    .color(r, g, b, a)
                    .uv(glyph.u2, glyph.v1)
                    .uv2(packedLight)
                    .endVertex();
        }

        builder = null;

        if (mHasEffect) {
            builder = source.getBuffer(EffectRenderType.getRenderType(seeThrough));
            for (int i = 0; i < mGlyphs.length; i++) {
                final int flags = mFlags[i];
                if ((flags & CharacterStyle.EFFECT_MASK) == 0) {
                    continue;
                }
                if ((flags & CharacterStyle.NO_COLOR_SPECIFIED) != 0) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = flags >> 16 & 0xff;
                    g = flags >> 8 & 0xff;
                    b = flags & 0xff;
                    if (isShadow) {
                        r >>= 2;
                        g >>= 2;
                        b >>= 2;
                    }
                }
                final float rx1 = x + mPositions[i << 1];
                final float rx2 = x + ((i + 1 == mGlyphs.length) ? mAdvance : mPositions[(i + 1) << 1]);
                if ((flags & CharacterStyle.UNDERLINE_MASK) != 0) {
                    TextRenderEffect.drawUnderline(matrix, builder, rx1, rx2, y, r, g, b, a, packedLight);
                }
                if ((flags & CharacterStyle.STRIKETHROUGH_MASK) != 0) {
                    TextRenderEffect.drawStrikethrough(matrix, builder, rx1, rx2, y, r, g, b, a, packedLight);
                }
            }
        }

        if ((colorBackground & 0xFF000000) != 0) {
            y -= sBaselineOffset;
            a = colorBackground >>> 24;
            r = colorBackground >> 16 & 0xff;
            g = colorBackground >> 8 & 0xff;
            b = colorBackground & 0xff;
            if (builder == null) {
                builder = source.getBuffer(EffectRenderType.getRenderType(seeThrough));
            }
            builder.vertex(matrix, x - 1, y + 9, TextRenderEffect.EFFECT_DEPTH)
                    .color(r, g, b, a).uv(0, 1).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + mAdvance + 1, y + 9, TextRenderEffect.EFFECT_DEPTH)
                    .color(r, g, b, a).uv(1, 1).uv2(packedLight).endVertex();
            builder.vertex(matrix, x + mAdvance + 1, y, TextRenderEffect.EFFECT_DEPTH)
                    .color(r, g, b, a).uv(1, 0).uv2(packedLight).endVertex();
            builder.vertex(matrix, x - 1, y, TextRenderEffect.EFFECT_DEPTH)
                    .color(r, g, b, a).uv(0, 0).uv2(packedLight).endVertex();
        }

        return mAdvance;
    }

    /**
     * All baked glyphs for rendering, empty glyphs have been removed from this array.
     * The order is visually left-to-right (i.e. in visual order).
     */
    public GLBakedGlyph[] getGlyphs() {
        return mGlyphs;
    }

    /**
     * Glyphs to relative char indices of the strip string (without formatting codes). However,
     * for vanilla layout {@link VanillaTextKey} and {@link TextLayoutEngine#lookupVanillaNode(String)},
     * these will be adjusted to string index (with formatting codes).
     * Same indexing with {@link #mGlyphs}, in visual order.
     */
    public int[] getCharIndices() {
        return mCharIndices;
    }

    /**
     * Position x1 y1 x2 y2... relative to the same point, for rendering glyphs.
     * Same indexing with {@link #mGlyphs}, align to left, in visual order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    public float[] getPositions() {
        return mPositions;
    }

    /**
     * The length and order are relative to the raw string (with formatting codes).
     * Only grapheme cluster bounds have advances, others are zeros. For example:
     * [13.57, 0, 14.26, 0, 0]. {@link #mGlyphs}.length may less than grapheme cluster
     * count (invisible glyphs are removed). Logical order.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    public float[] getAdvances() {
        return mAdvances;
    }

    /**
     * Returns the number of chars (i.e. the length of char array) of the full stripped
     * string (without formatting codes).
     *
     * @return length of the text
     */
    public int getLength() {
        return mAdvances.length;
    }

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
     *   1        FAST_DIGIT_REPLACEMENT
     *  1         NO_COLOR_SPECIFIED
     * |--------|
     */

    /**
     * Glyph rendering flags. Same indexing with {@link #mGlyphs}, in visual order.
     */
    public int[] getFlags() {
        return mFlags;
    }

    /**
     * Strip indices that are boundaries for Unicode line breaking, in logical order.
     * 0 is not included. Last value is always the text length (without formatting codes).
     */
    public int[] getLineBoundaries() {
        return mLineBoundaries;
    }

    /**
     * Total advance of this text node.
     * <p>
     * Note the values are scaled to Minecraft GUI coordinates.
     */
    public float getAdvance() {
        return mAdvance;
    }

    /**
     * Precomputed value that indicates whether flags array contains any text effect flag.
     */
    public boolean hasEffect() {
        return mHasEffect;
    }

    /**
     * @return measurable memory size in bytes of this object
     */
    public int getMemorySize() {
        int size = 0;
        int n = mGlyphs.length;
        size += 32 + (((n + 1) >> 1) << 4); // glyphs + charIndices
        size += 16 + (((n + 1) >> 1) << 3); // flags
        size += 16 + (n << 3); // positions
        size += 16 + (((mAdvances.length + 1) >> 1) << 3);
        size += 16 + (((mLineBoundaries.length + 1) >> 1) << 3);
        return size + 24;
    }

    @Override
    public String toString() {
        return "TextRenderNode{" +
                "glyphs=" + mGlyphs.length +
                ",length=" + mAdvances.length +
                ",charIndices=" + toIntString(mCharIndices) +
                ",positions=" + toPosString(mPositions) +
                ",advances=" + toFloatString(mAdvances) +
                ",flags=" + toFlagString(mFlags) +
                ",lineBoundaries" + toIntString(mLineBoundaries) +
                ",advance=" + mAdvance +
                ",hasEffect=" + mHasEffect +
                '}';
    }

    @Nonnull
    private static String toIntString(@Nonnull int[] a) {
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(',');
        }
    }

    @Nonnull
    private static String toFloatString(@Nonnull float[] a) {
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(',');
        }
    }

    @Nonnull
    private static String toPosString(@Nonnull float[] a) {
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append("x=");
            b.append(a[i++]);
            b.append(",y=");
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(',');
        }
    }

    @Nonnull
    private static String toFlagString(@Nonnull int[] a) {
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append("0x");
            b.append(Integer.toHexString(a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(',');
        }
    }
}
