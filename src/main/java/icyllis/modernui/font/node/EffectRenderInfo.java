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

import javax.annotation.Nonnull;

/**
 * Draw underline or strikethrough
 */
public class EffectRenderInfo {

    /**
     * Bit flag used with renderStyle to request the underline style
     */
    public static final byte UNDERLINE     = 1;
    /**
     * Bit flag used with renderStyle to request the strikethrough style
     */
    public static final byte STRIKETHROUGH = 2;

    /**
     * Offset from the string's baseline as which to draw the underline (in pixels)
     */
    private static final float UNDERLINE_OFFSET     = 0.5f;
    /**
     * Offset from the string's baseline as which to draw the strikethrough line (in pixels)
     */
    private static final float STRIKETHROUGH_OFFSET = -3.0f;


    private final float start;

    private final float end;

    private final int color;

    private final byte type;

    public EffectRenderInfo(float start, float end, int color, byte type) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.type = type;
    }

    public void drawEffect(@Nonnull IVertexBuilder builder, float x, float y, int r, int g, int b, int a) {
        if (color != -1) {
            r = color >> 16 & 0xff;
            g = color >> 8 & 0xff;
            b = color & 0xff;
        }
        x += start;
        if (type == UNDERLINE) {
            y += UNDERLINE_OFFSET;
            builder.pos(x, y + 1, 0).color(r, g, b, a).endVertex();
            builder.pos(x + end, y + 1, 0).color(r, g, b, a).endVertex();
            builder.pos(x + end, y + 1, 0).color(r, g, b, a).endVertex();
            builder.pos(x, y, 0).color(r, g, b, a).endVertex();
        } else if (type == STRIKETHROUGH) {
            y += STRIKETHROUGH_OFFSET;
            builder.pos(x, y + 1, 0).color(r, g, b, a).endVertex();
            builder.pos(x + end, y + 1, 0).color(r, g, b, a).endVertex();
            builder.pos(x + end, y + 1, 0).color(r, g, b, a).endVertex();
            builder.pos(x, y, 0).color(r, g, b, a).endVertex();
        }
    }
}
