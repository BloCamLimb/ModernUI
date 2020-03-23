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

package icyllis.modernui.gui.component.scroll;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.window.ScrollWindow;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Vertical
 */
public class ScrollBar implements IGuiEventListener {

    public int barThickness = 5;

    protected final ScrollWindow<?> window;

    protected float x, y;

    private float renderY;

    protected float barLength;

    protected float maxLength;

    protected boolean visible;

    protected float brightness = 0.5f;

    protected float startTime = 0;

    protected boolean mouseHovered = false;

    protected boolean isDragging = false;

    public ScrollBar(ScrollWindow<?> window) {
        this.window = window;
    }

    public void draw(float currentTime) {

        if (brightness > 0.5f && !mouseHovered) {
            if (currentTime > startTime) {
                float change = (startTime - currentTime) / 40.0f;
                brightness = Math.max(0.75f + change, 0.5f);
            }
        }

        if (!visible) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        RenderSystem.color4f(0.06f, 0.06f, 0.06f, 0.15f);

        renderRect(tessellator, bufferBuilder, y, maxLength);

        RenderSystem.color4f(brightness, brightness, brightness, 0.5f);

        renderRect(tessellator, bufferBuilder, renderY, barLength);

        RenderSystem.clearCurrentColor();
    }

    private void renderRect(Tessellator tessellator, BufferBuilder bufferBuilder, float renderY, float barLength) {
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(x, renderY + barLength, 0.0D).endVertex();
        bufferBuilder.pos(x + barThickness, renderY + barLength, 0.0D).endVertex();
        bufferBuilder.pos(x + barThickness, renderY, 0.0D).endVertex();
        bufferBuilder.pos(x, renderY, 0.0D).endVertex();
        tessellator.draw();
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    protected void wake() {
        brightness = 0.75f;
        startTime = GlobalModuleManager.INSTANCE.getAnimationTime() + 10.0f;
    }

    public void setBarLength(float percentage) {
        this.barLength = (int) (maxLength * percentage);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public float getMaxDragLength() {
        return maxLength - barLength;
    }

    public boolean isMouseOnBar(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + barThickness && mouseY >= renderY && mouseY <= renderY + barLength;
    }

    public void setBarOffset(float percentage) {
        renderY = y + getMaxDragLength() * percentage;
        wake();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (visible) {
            boolean prev = mouseHovered;
            mouseHovered = isMouseOnBar(mouseX, mouseY);
            if (prev != mouseHovered) {
                if (mouseHovered) {
                    wake();
                } else {
                    startTime = GlobalModuleManager.INSTANCE.getAnimationTime() + 10.0f;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (visible && mouseButton == 0) {
            if (mouseHovered) {
                isDragging = true;
                return true;
            } else {
                boolean inWidth = mouseX >= x && mouseX <= x + barThickness;
                if (inWidth) {
                    if (mouseY >= y && mouseY < renderY) {
                        float mov = transformPosToAmount((float) (renderY - mouseY));
                        window.scrollSmoothly(-Math.min(60f, mov));
                        return true;
                    } else if (mouseY > renderY + barLength && mouseY <= y + maxLength) {
                        float mov = transformPosToAmount((float) (mouseY - renderY - barLength));
                        window.scrollSmoothly(Math.min(60f, mov));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (visible && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double rmx, double rmy) {
        if (visible && isDragging) {
            window.scrollDirectly(transformPosToAmount((float) rmy));
            return true;
        }
        return false;
    }

    /**
     * Transform pos to scroll amount
     * @param rm relative move (pos)
     */
    public float transformPosToAmount(float rm) {
        return window.getMaxScrollAmount() * rm / getMaxDragLength();
    }

    public void setMaxLength(float maxLength) {
        this.maxLength = maxLength;
    }

}
