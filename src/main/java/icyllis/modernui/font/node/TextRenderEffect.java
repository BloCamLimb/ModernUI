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
import net.minecraft.util.math.vector.Matrix4f;

import javax.annotation.Nonnull;

/**
 * Draw underline or strikethrough
 */
public class TextRenderEffect {

    /**
     * Underline style type
     */
    private static final byte UNDERLINE_MASK     = 1;
    /**
     * Strikethrough style type
     */
    private static final byte STRIKETHROUGH_MASK = 2;

    public static final TextRenderEffect UNDERLINE               = new TextRenderEffect(UNDERLINE_MASK);
    public static final TextRenderEffect STRIKETHROUGH           = new TextRenderEffect(STRIKETHROUGH_MASK);
    public static final TextRenderEffect UNDERLINE_STRIKETHROUGH = new TextRenderEffect((byte) (UNDERLINE_MASK | STRIKETHROUGH_MASK));

    /**
     * Offset from the string's baseline as which to draw the underline
     */
    private static final float UNDERLINE_OFFSET     = 1.5f;
    /**
     * Offset from the string's baseline as which to draw the strikethrough line
     */
    private static final float STRIKETHROUGH_OFFSET = -3.0f;

    /**
     * Thickness of the underline
     */
    private static final float UNDERLINE_THICKNESS     = 1.0f;
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

    /**
     * Combination of {@link #UNDERLINE_MASK} and {@link #STRIKETHROUGH_MASK}
     */
    private final byte type;

    private TextRenderEffect(byte type) {
        this.type = type;
    }

    public void drawEffect(@Nonnull IVertexBuilder builder, float start, float end, float y, int r, int g, int b, int a) {
        if ((type & UNDERLINE_MASK) != 0) {
            y += UNDERLINE_OFFSET;
            builder.pos(start, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
            builder.pos(end, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
            builder.pos(end, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
            builder.pos(start, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        }
        if ((type & STRIKETHROUGH_MASK) != 0) {
            y += STRIKETHROUGH_OFFSET;
            builder.pos(start, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
            builder.pos(end, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).endVertex();
            builder.pos(end, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
            builder.pos(start, y, EFFECT_DEPTH).color(r, g, b, a).endVertex();
        }
    }

    public void drawEffect(Matrix4f matrix, @Nonnull IVertexBuilder builder, float start, float end, float y, int r, int g, int b, int a, int light) {
        if ((type & UNDERLINE_MASK) != 0) {
            y += UNDERLINE_OFFSET;
            builder.pos(matrix, start, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
            builder.pos(matrix, end, y + UNDERLINE_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
            builder.pos(matrix, end, y, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
            builder.pos(matrix, start, y, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
        }
        if ((type & STRIKETHROUGH_MASK) != 0) {
            y += STRIKETHROUGH_OFFSET;
            builder.pos(matrix, start, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
            builder.pos(matrix, end, y + STRIKETHROUGH_THICKNESS, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
            builder.pos(matrix, end, y, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
            builder.pos(matrix, start, y, EFFECT_DEPTH).color(r, g, b, a).lightmap(light).endVertex();
        }
    }

    /*@Nonnull
    public static EffectRenderInfo underline(float start, float end, int color) {
        return new EffectRenderInfo(start, end, color, UNDERLINE);
    }

    @Nonnull
    public static EffectRenderInfo strikethrough(float start, float end, int color) {
        return new EffectRenderInfo(start, end, color, STRIKETHROUGH);
    }*/

    @Override
    public String toString() {
        return "TextRenderEffect{" +
                "type=" + type +
                '}';
    }
}
