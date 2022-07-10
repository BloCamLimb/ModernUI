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

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;

import javax.annotation.Nonnull;

/**
 * Draw underline or strikethrough.
 */
public class TextRenderEffect {

    /**
     * Offset from the string's baseline as which to draw the underline
     */
    private static final float UNDERLINE_OFFSET = 0.5f;

    /**
     * Offset from the string's baseline as which to draw the strikethrough line
     */
    private static final float STRIKETHROUGH_OFFSET = -3.5f;

    /**
     * Thickness of the underline
     */
    private static final float UNDERLINE_THICKNESS = 1.0f;

    /**
     * Thickness of the strikethrough line
     */
    private static final float STRIKETHROUGH_THICKNESS = 1.0f;

    /**
     * Offset Z to ensure that effects render over characters in 3D world
     */
    public static final float EFFECT_DEPTH = 0.01f;

    /*
     * Start X offset of this effect to the start x of the whole text
     */
    //protected final float start;

    /*
     * End X offset of this effect to the start x of the whole text
     */
    //protected final float end;

    /*
     * The color in 0xRRGGBB format, or {@link FormattingStyle#NO_COLOR}
     */
    //protected final int color;

    /*
     * Combination of {@link #UNDERLINE_MASK} and {@link #STRIKETHROUGH_MASK}
     */
    //private final byte type;

    private TextRenderEffect() {
    }

    public static void drawUnderline(@Nonnull VertexConsumer builder, float start, float end, float y, int r, int g,
                                     int b, int a) {
        y += UNDERLINE_OFFSET;
        builder.vertex(start, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        builder.vertex(end, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        builder.vertex(end, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        builder.vertex(start, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
    }

    public static void drawUnderline(@Nonnull Matrix4f matrix, @Nonnull VertexConsumer builder, float start,
                                     float end, float y, int r, int g, int b, int a, int light) {
        y += UNDERLINE_OFFSET;
        builder.vertex(matrix, start, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).uv(0, 1).uv2(light).endVertex();
        builder.vertex(matrix, end, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).uv(1, 1).uv2(light).endVertex();
        builder.vertex(matrix, end, y, EFFECT_DEPTH).color(r, g, b, a).uv(1, 0).uv2(light).endVertex();
        builder.vertex(matrix, start, y, EFFECT_DEPTH).color(r, g, b, a).uv(0, 0).uv2(light).endVertex();
    }

    public static void drawStrikethrough(@Nonnull VertexConsumer builder, float start, float end, float y, int r,
                                         int g, int b, int a) {
        y += STRIKETHROUGH_OFFSET;
        builder.vertex(start, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        builder.vertex(end, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        builder.vertex(end, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        builder.vertex(start, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
    }

    public static void drawStrikethrough(@Nonnull Matrix4f matrix, @Nonnull VertexConsumer builder, float start,
                                         float end, float y, int r, int g, int b, int a, int light) {
        y += STRIKETHROUGH_OFFSET;
        builder.vertex(matrix, start, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).uv(0, 1).uv2(light).endVertex();
        builder.vertex(matrix, end, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).uv(1, 1).uv2(light).endVertex();
        builder.vertex(matrix, end, y, EFFECT_DEPTH).color(r, g, b, a).uv(1, 0).uv2(light).endVertex();
        builder.vertex(matrix, start, y, EFFECT_DEPTH).color(r, g, b, a).uv(0, 0).uv2(light).endVertex();
    }

    /*@Nonnull
    public static EffectRenderInfo underline(float start, float end, int color) {
        return new EffectRenderInfo(start, end, color, UNDERLINE);
    }

    @Nonnull
    public static EffectRenderInfo strikethrough(float start, float end, int color) {
        return new EffectRenderInfo(start, end, color, STRIKETHROUGH);
    }*/

    /*private static RenderType getRenderType(boolean seeThrough) {
        return seeThrough ? seeThroughType : normalType;
    }*/
}
