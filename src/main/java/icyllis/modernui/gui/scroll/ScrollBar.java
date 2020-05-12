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

import icyllis.modernui.gui.master.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is a part of scroll window
 * Not a widget
 */
public class ScrollBar implements IDrawable, IMouseListener, IDraggable {

    private final GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private final IScrollable master;

    public final int barThickness = 5;

    private float x, y;

    private float barY;

    private float barLength;

    private float height;

    private boolean visible;

    private float brightness = 0.5f;

    private float startTime = 0;

    private boolean barHovered = false;

    private boolean isDragging = false;

    //private double draggingY = 0;

    public ScrollBar(IScrollable scrollable) {
        this.master = scrollable;
    }

    @Override
    public void draw(Canvas canvas, float time) {
        if (!barHovered && !isDragging && brightness > 0.5f) {
            if (time > startTime) {
                float change = (startTime - time) / 40.0f;
                brightness = Math.max(0.75f + change, 0.5f);
            }
        }
        if (!visible) {
            return;
        }
        canvas.setRGBA(0.063f, 0.063f, 0.063f, 0.157f);
        canvas.drawRect(x, y, x + barThickness, y + height);
        canvas.setRGBA(brightness, brightness, brightness, 0.5f);
        canvas.drawRect(x, barY, x + barThickness, barY + barLength);
    }

    /*public void draw(float currentTime) {
        if (!barHovered && !isDragging && brightness > 0.5f) {
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
    }*/

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    protected void wake() {
        brightness = 0.75f;
        startTime = manager.getAnimationTime() + 10.0f;
    }

    public void setBarLength(float percentage) {
        this.barLength = (int) (height * percentage);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public float getMaxDragLength() {
        return height - barLength;
    }

    public void setBarOffset(float percentage) {
        barY = y + getMaxDragLength() * percentage;
        wake();
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (visible) {
            boolean prev = barHovered;
            barHovered = isMouseOnBar(mouseX, mouseY);
            if (prev != barHovered) {
                if (barHovered) {
                    wake();
                } else {
                    startTime = manager.getAnimationTime() + 10.0f;
                }
            }
            return barHovered;
        }
        return false;
    }

    @Override
    public void setMouseHoverExit() {
        if (barHovered) {
            barHovered = false;
            startTime = manager.getAnimationTime() + 10.0f;
        }
    }

    private boolean isMouseOnBar(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + barThickness && mouseY >= barY && mouseY <= barY + barLength;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (visible && mouseButton == 0) {
            if (barHovered) {
                isDragging = true;
                //draggingY = mouseY;
                master.setDraggable(this);
                return true;
            } else {
                boolean inWidth = mouseX >= x && mouseX <= x + barThickness;
                if (inWidth) {
                    if (mouseY >= y && mouseY < barY) {
                        float mov = transformPosToAmount((float) (barY - mouseY));
                        master.scrollSmooth(-Math.min(60f, mov));
                        return true;
                    } else if (mouseY > barY + barLength && mouseY <= y + height) {
                        float mov = transformPosToAmount((float) (mouseY - barY - barLength));
                        master.scrollSmooth(Math.min(60f, mov));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * This shouldn't be called (
     */
    @Override
    public boolean isMouseHovered() {
        return barHovered;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    @Override
    public void stopMouseDragging() {
        if (visible && isDragging) {
            isDragging = false;
            startTime = manager.getAnimationTime() + 10.0f;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (visible && isDragging) {
            /*if (barY + deltaY >= y && barY - y + deltaY <= getMaxDragLength()) {
                draggingY += deltaY;
            }
            if (mouseY == draggingY) {
                window.scrollDirect(transformPosToAmount((float) deltaY));
            }*/
            if (mouseY >= y && mouseY <= y + height) {
                master.scrollDirect(transformPosToAmount((float) deltaY));
            }
            return true;
        }
        return false;
    }

    /**
     * Transform pos to scroll amount
     * @param relativePos relative move (pos)
     */
    public float transformPosToAmount(float relativePos) {
        return master.getMaxScrollAmount() * relativePos / getMaxDragLength();
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public interface IScrollable {

        void setDraggable(@Nonnull IDraggable draggable);

        void scrollSmooth(float delta);

        void scrollDirect(float delta);

        float getMaxScrollAmount();
    }
}
