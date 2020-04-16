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

import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Used for selecting a color
 */
public class ColorButton extends Widget {

    private final float r, g, b;

    private final int color;

    private final Consumer<ColorButton> leftClickFunc;

    private boolean selected;

    private float frameAlpha = 0;

    /**
     * Constructor
     * @param color RGB
     */
    public ColorButton(Module module, float size, int color, Consumer<ColorButton> leftClick, boolean selected) {
        super(module, size, size);
        this.r = (color >> 16 & 0xff) / 255f;
        this.g = (color >> 8 & 0xff) / 255f;
        this.b = (color & 0xff) / 255f;
        this.color = color;
        this.leftClickFunc = leftClick;
        this.selected = selected;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(r, g, b, 0.8f);
        canvas.drawRect(x1, y1, x2, y2);
        if (frameAlpha > 0) {
            canvas.setRGBA(1, 1, 1, frameAlpha);
            canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            leftClickFunc.accept(this);
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        if (!selected) {
            frameAlpha = 0.5f;
        }
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (!selected) {
            frameAlpha = 0;
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            frameAlpha = 1.0f;
        } else {
            frameAlpha = 0;
        }
    }

    public int getColor() {
        return color;
    }
}
