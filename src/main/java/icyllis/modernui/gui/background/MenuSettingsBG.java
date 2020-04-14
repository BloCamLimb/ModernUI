/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.background;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.IElement;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class MenuSettingsBG implements IElement {

    private float sizeW, sizeH;

    public MenuSettingsBG() {

    }

    @Override
    public void draw(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        float x = 40, y = 16;
        int alpha = 96;
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + sizeH - 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + sizeH - 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x, y + 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        tessellator.draw();
        alpha = 192;
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(0, 0, 0, alpha).endVertex();
        tessellator.draw();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + sizeH, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + sizeH, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + sizeH - 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x, y + sizeH - 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        tessellator.draw();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0f);
        bufferBuilder.pos(x, y + 20, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferBuilder.pos(x + sizeW, y + 20, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferBuilder.pos(x, y + sizeH - 19.5, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferBuilder.pos(x + sizeW, y + sizeH - 19.5, 0.0D).color(140, 140, 140, 220).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    @Override
    public void resize(int width, int height) {
        sizeW = width - 80;
        sizeH = height - 32;
    }
}
