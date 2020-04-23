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

package icyllis.modernui.gui.master;

import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Widget implements IWidget {

    private final Module module;

    @Expose
    @Nullable
    private Locator locator;

    @Expose
    private Align9D align = Align9D.TOP_LEFT;

    protected float x1, y1;

    protected float x2, y2;

    @Expose
    protected float width, height;

    protected boolean listening = true;

    private boolean mouseHovered = false;

    public Widget(Module module) {
        this.module = module;
    }

    public Widget(Module module, float width, float height) {
        this.module = module;
        this.width = width;
        this.height = height;
    }

    @Override
    public void locate(float px, float py) {
        int horizontalAlign = align.ordinal() % 3;
        switch (horizontalAlign) {
            case 0:
                x1 = px;
                break;
            case 1:
                x1 = px - width / 2f;
                break;
            case 2:
                x1 = px - width;
                break;
        }
        x2 = x1 + width;

        int verticalAlign = align.ordinal() / 3;
        switch (verticalAlign) {
            case 0:
                y1 = py;
                break;
            case 1:
                y1 = py - height / 2f;
                break;
            case 2:
                y1 = py - height;
                break;
        }
        y2 = y1 + height;
    }

    public void setLocator(@Nonnull Locator locator) {
        this.locator = locator;
    }

    public void setAlign(Align9D align) {
        this.align = align;
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
    public void resize(int width, int height) {
        if (locator != null) {
            locator.locate(this, width, height);
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (listening) {
            boolean prev = isMouseHovered();
            setMouseHovered(isMouseInArea(mouseX, mouseY));
            if (prev != isMouseHovered()) {
                if (isMouseHovered()) {
                    onMouseHoverEnter();
                } else {
                    onMouseHoverExit();
                }
            }
            return isMouseHovered();
        }
        return false;
    }

    @Override
    public final void setMouseHoverExit() {
        if (isMouseHovered()) {
            setMouseHovered(false);
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

    }

    protected void onMouseHoverExit() {

    }

    public Module getModule() {
        return module;
    }

    public void setMouseHovered(boolean mouseHovered) {
        this.mouseHovered = mouseHovered;
    }
}
