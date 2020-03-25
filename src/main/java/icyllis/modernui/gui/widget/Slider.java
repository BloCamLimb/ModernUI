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

package icyllis.modernui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.element.IElement;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public abstract class Slider implements IElement, IGuiEventListener {

    protected float x, y;

    protected double slideOffset;

    protected float width;

    protected boolean isDragging = false;

    protected boolean thumbHovered = false;

    public Slider(float width) {
        this.width = width;
    }

    @Override
    public void draw(float currentTime) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + 3, 0.0D).color(160, 160, 160, 255).endVertex();
        bufferBuilder.pos(x + slideOffset, y + 3, 0.0D).color(160, 160, 160, 255).endVertex();
        bufferBuilder.pos(x + slideOffset, y, 0.0D).color(160, 160, 160, 255).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(160, 160, 160, 255).endVertex();
        tessellator.draw();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x + slideOffset, y + 3, 0.0D).color(80, 80, 80, 220).endVertex();
        bufferBuilder.pos(x + width, y + 3, 0.0D).color(80, 80, 80, 220).endVertex();
        bufferBuilder.pos(x + width, y, 0.0D).color(80, 80, 80, 220).endVertex();
        bufferBuilder.pos(x + slideOffset, y, 0.0D).color(80, 80, 80, 220).endVertex();
        tessellator.draw();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int cc = (thumbHovered || isDragging) ? 255 : 204;
        bufferBuilder.pos(x + slideOffset, y + 4, 0.0D).color(cc, cc, cc, 255).endVertex();
        bufferBuilder.pos(x + slideOffset + 4, y + 4, 0.0D).color(cc, cc, cc, 255).endVertex();
        bufferBuilder.pos(x + slideOffset + 4, y - 1, 0.0D).color(cc, cc, cc, 255).endVertex();
        bufferBuilder.pos(x + slideOffset, y - 1, 0.0D).color(cc, cc, cc, 255).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        thumbHovered = mouseX >= x + slideOffset && mouseX <= x + slideOffset + 4 && mouseY >= y - 1 && mouseY <= y + 4;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (thumbHovered) {
                isDragging = true;
                return true;
            } else {
                boolean inY = mouseY >= y && mouseY <= y + 3;
                if (inY) {
                    if (mouseX >= x && mouseX <= x + slideOffset) {
                        slideToOffset((float) (mouseX - x - 2));
                        mouseMoved(mouseX, mouseY);
                        return true;
                    } else if (mouseX >= x + slideOffset + 4 && mouseX <= x + width) {
                        slideToOffset((float) (mouseX - x - 2));
                        mouseMoved(mouseX, mouseY);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && isDragging) {
            isDragging = false;
            onStopDragging();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (isDragging) {
            slideToOffset((float) (mouseX - x - 2));
            return true;
        }
        return false;
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    protected void onStopDragging() {

    }

    protected abstract void slideToOffset(float offset);

    protected float getMaxSlideOffset() {
        return width - 4;
    }
}
