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

import icyllis.modernui.gui.master.IMouseListener;

public abstract class ScrollGroup implements IMouseListener {

    protected final ScrollWindow<?> window;

    protected float x1, y1;

    protected float x2, y2;

    protected float centerX;

    protected float height;

    protected boolean mouseHovered = false;

    /**
     * Must specify height in constructor
     */
    public ScrollGroup(ScrollWindow<?> window) {
        this.window = window;
    }

    public ScrollGroup(ScrollWindow<?> window, int height) {
        this.window = window;
        this.height = height;
    }

    /**
     * Called when layout
     */
    public void onLayout(float left, float right, float y) {
        this.x1 = left;
        this.x2 = right;
        this.centerX = (left + right) / 2f;
        this.y1 = y;
        this.y2 = y + height;
    }

    public final float getHeight() {
        return height;
    }

    public final float getTop() {
        return y1;
    }

    public final float getBottom() {
        return y2;
    }

    public abstract void updateVisible(float top, float bottom);

    public abstract void draw(float time);

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
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

    private boolean isMouseInArea(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    protected void onMouseHoverEnter() {

    }

    protected void onMouseHoverExit() {

    }

    @Override
    public final boolean isMouseHovered() {
        return mouseHovered;
    }

}
