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

import icyllis.modernui.gui.master.IDraggable;
import icyllis.modernui.gui.master.IHost;
import icyllis.modernui.gui.master.IKeyboardListener;
import icyllis.modernui.gui.master.IWidget;

import javax.annotation.Nullable;
import java.util.function.Function;

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

    @Deprecated
    @Override
    public final void locate(float px, float py) {
        throw new RuntimeException();
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

    protected void onMouseHoverEnter(double mouseX, double mouseY) {}

    protected void onMouseHoverExit() {}

    @Override
    public int getWindowWidth() {
        return host.getWindowWidth();
    }

    @Override
    public int getWindowHeight() {
        return host.getWindowHeight();
    }

    @Override
    public double getAbsoluteMouseX() {
        return host.getAbsoluteMouseX();
    }

    @Override
    public double getAbsoluteMouseY() {
        return host.getAbsoluteMouseY();
    }

    @Override
    public double getRelativeMouseX() {
        return host.getRelativeMouseX();
    }

    @Override
    public double getRelativeMouseY() {
        return host.getRelativeMouseY();
    }

    @Override
    public float toAbsoluteX(float rx) {
        return host.toAbsoluteX(rx);
    }

    @Override
    public float toAbsoluteY(float ry) {
        return host.toAbsoluteY(ry);
    }

    @Override
    public int getElapsedTicks() {
        return host.getElapsedTicks();
    }

    @Override
    public void refocusMouseCursor() {
        host.refocusMouseCursor();
    }

    @Override
    public void setDraggable(@Nullable IDraggable draggable) {
        host.setDraggable(draggable);
    }

    @Nullable
    @Override
    public IDraggable getDraggable() {
        return host.getDraggable();
    }

    @Override
    public void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        host.setKeyboardListener(keyboardListener);
    }

    @Nullable
    @Override
    public IKeyboardListener getKeyboardListener() {
        return host.getKeyboardListener();
    }
}
