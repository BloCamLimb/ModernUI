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

package icyllis.modernui.gui.test;

import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.master.AnimationControl;
import icyllis.modernui.gui.master.IWidget;

@Deprecated
public abstract class AnimatedWidget extends AnimationControl implements IWidget {

    protected float x1, y1;

    protected float x2, y2;

    protected float width, height;

    protected boolean listening = true;

    protected boolean mouseHovered = false;

    public AnimatedWidget() {

    }

    public AnimatedWidget(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void setPos(float x, float y) {
        this.x1 = x;
        this.x2 = x + width;
        this.y1 = y;
        this.y2 = y + height;
    }

    @Override
    public final float getLeft() {
        return x1;
    }

    @Override
    public final float getRight() {
        return x2;
    }

    @Override
    public final float getTop() {
        return y1;
    }

    @Override
    public final float getBottom() {
        return y2;
    }

    @Override
    public final float getWidth() {
        return width;
    }

    @Override
    public final float getHeight() {
        return height;
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (listening) {
            boolean prev = mouseHovered;
            mouseHovered = isMouseInArea(mouseX, mouseY);
            if (prev != mouseHovered) {
                if (mouseHovered) {
                    onMouseHoverEnter();
                } else {
                    onMouseHoverExit();
                }
            }
            return mouseHovered;
        }
        return false;
    }

    @Override
    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    private boolean isMouseInArea(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public final boolean isMouseHovered() {
        return mouseHovered;
    }

    protected void onMouseHoverEnter() {
        this.startOpenAnimation();
    }

    protected void onMouseHoverExit() {
        this.startCloseAnimation();
    }
}
