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
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextRenderNode {

    /**
     * Vertical adjustment to string position.
     */
    private static final int BASELINE_OFFSET = 7;


    private final StringRenderInfo[] stringInfos;

    @Nullable
    private final EffectRenderInfo[] effectInfos;

    public final float advance;

    public TextRenderNode(StringRenderInfo[] stringInfos, @Nullable EffectRenderInfo[] effectInfos, float advance) {
        this.stringInfos = stringInfos;
        this.effectInfos = effectInfos;
        this.advance = advance;
    }

    public void drawText(@Nonnull BufferBuilder builder, float x, float y, int r, int g, int b, int a) {
        float bx = x;
        y += BASELINE_OFFSET;
        RenderSystem.enableTexture();
        for (StringRenderInfo info : stringInfos) {
            x = info.drawString(builder, x, y, r, g, b, a);
        }
        if (effectInfos != null) {
            RenderSystem.disableTexture();
            for (EffectRenderInfo info : effectInfos) {
                builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                info.drawEffect(builder, bx, y, r, g, b, a);
                builder.finishDrawing();
                WorldVertexBufferUploader.draw(builder);
            }
        }
    }
}
