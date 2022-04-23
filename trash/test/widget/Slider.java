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

package icyllis.modernui.test.widget;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.test.discard.IDraggable;
import icyllis.modernui.test.discard.IHost;
import icyllis.modernui.test.discard.Widget;

import javax.annotation.Nonnull;

/**
 * Horizontal slider
 * Slider doesn't store current value
 */
@Deprecated
public abstract class Slider extends Widget implements IDraggable {

    protected double slideOffset;

    protected boolean isDragging = false;

    protected boolean thumbHovered = false;

    public Slider(IHost host, Builder builder) {
        super(host, builder);
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        float cx = (float) (x1 + slideOffset);
        //canvas.setColor(0.63f, 0.63f, 0.63f, 1.0f);
        canvas.drawRect(x1, y1, cx, y2);
        //canvas.setColor(0.315f, 0.315f, 0.315f, 0.863f);
        canvas.drawRect(cx, y1, x2, y2);
        float c = (thumbHovered || isDragging) ? 1.0f : 0.8f;
        //canvas.setColor(c, c, c, 1.0f);
        canvas.drawRect(cx, y1 - 1, cx + 4, y2 + 1);
    }

    /*@Override
    public void draw(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        double cx = x1 + slideOffset;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, y2, 0.0D).color(160, 160, 160, 255).endVertex();
        bufferBuilder.pos(cx, y2, 0.0D).color(160, 160, 160, 255).endVertex();
        bufferBuilder.pos(cx, y1, 0.0D).color(160, 160, 160, 255).endVertex();
        bufferBuilder.pos(x1, y1, 0.0D).color(160, 160, 160, 255).endVertex();
        tessellator.draw();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(cx, y2, 0.0D).color(80, 80, 80, 220).endVertex();
        bufferBuilder.pos(x2, y2, 0.0D).color(80, 80, 80, 220).endVertex();
        bufferBuilder.pos(x2, y1, 0.0D).color(80, 80, 80, 220).endVertex();
        bufferBuilder.pos(cx, y1, 0.0D).color(80, 80, 80, 220).endVertex();
        tessellator.draw();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int cc = (thumbHovered || isDragging) ? 255 : 204;
        bufferBuilder.pos(cx, y2 + 1, 0.0D).color(cc, cc, cc, 255).endVertex();
        bufferBuilder.pos(cx + 4, y2 + 1, 0.0D).color(cc, cc, cc, 255).endVertex();
        bufferBuilder.pos(cx + 4, y1 - 1, 0.0D).color(cc, cc, cc, 255).endVertex();
        bufferBuilder.pos(cx, y1 - 1, 0.0D).color(cc, cc, cc, 255).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
    }*/

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            checkThumb(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (thumbHovered) {
            isDragging = true;
            getParent().setDraggable(this);
            return true;
        } else {
            if (mouseX >= x1 && mouseX <= x1 + slideOffset) {
                slideToOffset(mouseX - x1 - 2);
                checkThumb(mouseX, mouseY);
                if (thumbHovered) {
                    isDragging = true;
                    getParent().setDraggable(this);
                } else {
                    onStopDragging();
                }
                return true;
            } else if (mouseX >= x1 + slideOffset + 4 && mouseX <= x2) {
                slideToOffset(mouseX - x1 - 2);
                checkThumb(mouseX, mouseY);
                if (thumbHovered) {
                    isDragging = true;
                    getParent().setDraggable(this);
                } else {
                    onStopDragging();
                }
                return true;
            }
        }
        return super.onMouseLeftClick(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isDragging) {
            slideToOffset(mouseX - x1 - 2);
            return true;
        }
        return false;
    }

    @Override
    public final void stopMouseDragging() {
        if (isDragging) {
            isDragging = false;
            checkThumb(getParent().getRelativeMouseX(), getParent().getRelativeMouseY());
            onStopDragging();
        }
    }

    @Override
    public void onMouseHoverExit() {
        super.onMouseHoverExit();
        thumbHovered = false;
    }

    private void checkThumb(double mouseX, double mouseY) {
        thumbHovered = mouseX >= x1 + slideOffset && mouseX <= x1 + slideOffset + 4 && mouseY >= y1 - 1 && mouseY <= y2 + 1;
    }

    protected abstract void onStopDragging();

    protected abstract void slideToOffset(double offset);

    protected double getMaxSlideOffset() {
        return width - 4;
    }
}
