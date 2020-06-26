/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.ui.master.View;

import javax.annotation.Nonnull;

/**
 * This class encapsulated methods to handle events and draw the scroll bar.
 * Scroll bar is never a view in UI, but including view's logic.
 * Scroll bar should be the same level as the view it's in.
 * To control the scroll amount, use {@link Scroller}
 *
 * @since 1.6
 */
public class ScrollBar extends View {

    @Nonnull
    private final ICallback callback;

    private float barY;

    private float barLength;

    private float brightness = 0.5f;

    private float startTime = 0;

    private boolean barHovered = false;

    private boolean isDragging = false;

    private double accDragging = 0;

    public ScrollBar(@Nonnull ICallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        if (!barHovered && !isDragging && brightness > 0.5f) {
            if (canvas.getDrawingTime() > startTime) {
                float change = (startTime - canvas.getDrawingTime()) / 2000.0f;
                brightness = Math.max(0.75f + change, 0.5f);
            }
        }
        canvas.setColor(16, 16, 16, 40);
        canvas.drawRect(getLeft(), getTop(), getRight(), getBottom());
        int br = (int) (brightness * 255.0f);
        canvas.setColor(br, br, br, 128);
        canvas.drawRect(getLeft(), barY, getRight(), barY + barLength);
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

    private void wake() {
        brightness = 0.75f;
        startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
    }

    public void setBarLength(float percentage) {
        this.barLength = (int) (getHeight() * percentage);
    }

    private float getMaxDragLength() {
        return getHeight() - barLength;
    }

    public void setBarOffset(float percentage) {
        barY = getMaxDragLength() * percentage + getTop();
        wake();
    }

    /*@Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (visible) {

            return barHovered;
        }
        return false;
    }*/

    /*@Override
    protected boolean onUpdateMouseHover(int mouseX, int mouseY) {
        boolean prev = barHovered;
        barHovered = isMouseOnBar(mouseY);
        if (prev != barHovered) {
            if (barHovered) {
                wake();
            } else {
                startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
            }
        }
        return super.onUpdateMouseHover(mouseX, mouseY);
    }*/

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (barHovered) {
            barHovered = false;
            startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
        }
    }

    private boolean isMouseOnBar(double mouseY) {
        return mouseY >= barY && mouseY <= barY + barLength;
    }

    @Override
    protected boolean onMouseLeftClicked(int mouseX, int mouseY) {
        if (barHovered) {
            isDragging = true;
            UIManager.INSTANCE.setDragging(this);
        } else {
            if (mouseY < barY) {
                float mov = transformPosToAmount((float) (barY - mouseY));
                //controller.scrollSmoothBy(-Math.min(60f, mov));
            } else if (mouseY > barY + barLength) {
                float mov = transformPosToAmount((float) (mouseY - barY - barLength));
                //controller.scrollSmoothBy(Math.min(60f, mov));
            }
        }
        return true;
    }

    @Override
    protected void onStopDragging() {
        super.onStopDragging();
        if (isDragging) {
            isDragging = false;
            startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
        }
    }

    @Override
    protected boolean onMouseDragged(int mouseX, int mouseY, double deltaX, double deltaY) {
        /*if (barY + deltaY >= y && barY - y + deltaY <= getMaxDragLength()) {
            draggingY += deltaY;
        }
        if (mouseY == draggingY) {
            window.scrollDirect(transformPosToAmount((float) deltaY));
        }*/
        if (mouseY >= getTop() && mouseY <= getBottom()) {
            accDragging += deltaY;
            int i = (int) (accDragging * 2.0);
            if (i != 0) {
                //controller.scrollDirectBy(transformPosToAmount(i / 2.0f));
                accDragging -= i / 2.0f;
            }
        }
        return true;
    }

    /**
     * Transform pos to scroll amount
     *
     * @param relativePos relative move (pos)
     */
    private float transformPosToAmount(float relativePos) {
        return 0;//view.getMaxScrollAmount() / getMaxDragLength() * relativePos;
    }

    public interface ICallback {

    }
}
