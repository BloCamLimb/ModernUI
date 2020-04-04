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

package icyllis.modernui.gui.scroll;

import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IDraggable;
import icyllis.modernui.gui.master.IMouseListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * This is a part of scroll window
 */
public class ScrollBar implements IMouseListener, IDraggable {

    private final GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private final ScrollWindow<?> window;

    public final int barThickness = 5;

    private float x, y;

    private float barY;

    private float barLength;

    private float maxLength;

    private boolean visible;

    private float brightness = 0.5f;

    private float startTime = 0;

    private boolean mouseHovered = false;

    private boolean isDragging = false;

    private double draggingY = 0;

    public ScrollBar(ScrollWindow<?> window) {
        this.window = window;
    }

    public void draw(float currentTime) {
        if (!mouseHovered && brightness > 0.5f) {
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

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + maxLength, 0.0D).color(16, 16, 16, 40).endVertex();
        bufferBuilder.pos(x + barThickness, y + maxLength, 0.0D).color(16, 16, 16, 40).endVertex();
        bufferBuilder.pos(x + barThickness, y, 0.0D).color(16, 16, 16, 40).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(16, 16, 16, 40).endVertex();
        tessellator.draw();

        int b = (int) (brightness * 255);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, barY + barLength, 0.0D).color(b, b, b, 128).endVertex();
        bufferBuilder.pos(x + barThickness, barY + barLength, 0.0D).color(b, b, b, 128).endVertex();
        bufferBuilder.pos(x + barThickness, barY, 0.0D).color(b, b, b, 128).endVertex();
        bufferBuilder.pos(x, barY, 0.0D).color(b, b, b, 128).endVertex();
        tessellator.draw();
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    protected void wake() {
        brightness = 0.75f;
        startTime = manager.getAnimationTime() + 10.0f;
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

    public void setBarOffset(float percentage) {
        barY = y + getMaxDragLength() * percentage;
        wake();
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (visible) {
            boolean prev = mouseHovered;
            mouseHovered = isMouseOnBar(mouseX, mouseY);
            if (prev != mouseHovered) {
                if (mouseHovered) {
                    wake();
                } else {
                    startTime = manager.getAnimationTime() + 10.0f;
                }
            }
            return mouseHovered;
        }
        return false;
    }

    @Override
    public void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            startTime = manager.getAnimationTime() + 10.0f;
        }
    }

    private boolean isMouseOnBar(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + barThickness && mouseY >= barY && mouseY <= barY + barLength;
    }

    @Override
    public boolean mouseClicked(int mouseButton) {
        double mouseX = manager.getMouseX();
        double mouseY = manager.getMouseY();
        if (visible && mouseButton == 0) {
            if (mouseHovered) {
                isDragging = true;
                draggingY = mouseY;
                window.setDraggable(this);
                return true;
            } else {
                boolean inWidth = mouseX >= x && mouseX <= x + barThickness;
                if (inWidth) {
                    if (mouseY >= y && mouseY < barY) {
                        float mov = transformPosToAmount((float) (barY - mouseY));
                        window.scrollSmooth(-Math.min(60f, mov));
                        return true;
                    } else if (mouseY > barY + barLength && mouseY <= y + maxLength) {
                        float mov = transformPosToAmount((float) (mouseY - barY - barLength));
                        window.scrollSmooth(Math.min(60f, mov));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMouseHovered() {
        return mouseHovered;
    }

    @Override
    public boolean mouseReleased(int mouseButton) {
        if (visible && mouseButton == 0 && isDragging) {
            isDragging = false;
            window.setDraggable(null);
            return true;
        }
        return false;
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (visible && isDragging) {
            if (barY >= y && barY - y <= getMaxDragLength()) {
                draggingY += deltaY;
            }
            if (mouseY == draggingY) {
                window.scrollDirect(transformPosToAmount((float) deltaY));
            }
        }
    }

    /**
     * Transform pos to scroll amount
     * @param relativePos relative move (pos)
     */
    public float transformPosToAmount(float relativePos) {
        return window.getMaxScrollAmount() * relativePos / getMaxDragLength();
    }

    public void setMaxLength(float maxLength) {
        this.maxLength = maxLength;
    }

}
