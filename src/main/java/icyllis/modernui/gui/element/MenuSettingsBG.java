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

package icyllis.modernui.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class MenuSettingsBG extends Element {

    private float sizeW, sizeH;

    private int alpha = 0;

    public MenuSettingsBG() {
        super(w -> 24f, h -> 16f);
        moduleManager.addAnimation(new Animation(2, true)
                .applyTo(new Applier(24, 40, this::setX))
                .onFinish(() -> xResizer = w -> 40f));
        moduleManager.addAnimation(new Animation(2)
                .applyTo(new Applier(0, 0.4f, this::setAlpha)));
    }

    private void setX(float x) {
        this.x = x;
    }

    private void setAlpha(float alpha) {
        this.alpha = (int) (255 * alpha);
    }

    @Override
    public void draw(float currentTime) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + sizeH - 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + sizeH - 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x + sizeW, y + 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        bufferBuilder.pos(x, y + 20, 0.0D).color(0, 0, 0, alpha).endVertex();
        tessellator.draw();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int alpha = this.alpha * 2;
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
        super.resize(width, height);
        sizeW = width - 80;
        sizeH = height - 32;
    }
}
