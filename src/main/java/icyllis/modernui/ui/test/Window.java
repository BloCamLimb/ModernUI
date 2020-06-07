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

package icyllis.modernui.ui.test;

import java.util.function.Function;

/**
 * Window defines an area with uncertain size and position
 */
@Deprecated
public abstract class Window implements IWidget, IHost {

    private final IHost host;

    private final Function<Integer, Float> xResizer, yResizer;

    private final Function<Integer, Float> wResizer, hResizer;

    protected float x1, y1;

    protected float x2, y2;

    protected float width, height;

    private boolean mouseHovered = false;

    public Window(IHost host, Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, Function<Integer, Float> wResizer, Function<Integer, Float> hResizer) {
        this.host = host;
        this.xResizer = xResizer;
        this.yResizer = yResizer;
        this.wResizer = wResizer;
        this.hResizer = hResizer;
    }

    @Override
    public void resize(int width, int height) {
        this.x1 = xResizer.apply(width);
        this.y1 = yResizer.apply(height);
        this.width = wResizer.apply(width);
        this.height = hResizer.apply(height);
        this.x2 = x1 + this.width;
        this.y2 = y1 + this.height;
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
    public boolean updateMouseHover(double mouseX, double mouseY) {
        boolean prev = mouseHovered;
        mouseHovered = isMouseInArea(mouseX, mouseY);
        if (prev != mouseHovered) {
            if (mouseHovered) {
                onMouseHoverEnter(mouseX, mouseY);
            } else {
                onMouseHoverExit();
            }
        }
        return mouseHovered;
    }

    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    private boolean isMouseInArea(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public final boolean isMouseHovered() {
        return mouseHovered;
    }

    public void onMouseHoverEnter(double mouseX, double mouseY) {}

    public void onMouseHoverExit() {}

    @Override
    public final IHost getParent() {
        return host;
    }
}
