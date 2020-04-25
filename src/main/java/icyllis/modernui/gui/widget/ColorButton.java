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

import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Color3f;
import icyllis.modernui.gui.math.Locator;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Used for selecting a color
 */
public class ColorButton extends Widget {

    private float r, g, b;
    private int color;

    private Consumer<ColorButton> callback = c -> {};

    private boolean selected = false;

    private float frameAlpha = 0;

    public ColorButton(Module module, Builder builder) {
        super(module, builder);
        setColor(builder.color);
    }

    public ColorButton setCallback(Consumer<ColorButton> c) {
        callback = c;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(r, g, b, 0.8f);
        canvas.drawRect(x1, y1, x2, y2);
        if (frameAlpha > 0) {
            canvas.setRGBA(1, 1, 1, frameAlpha);
            canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        }
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        callback.accept(this);
        return true;
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

    public void setColor(int color) {
        this.color = color;
        this.r = Color3f.getRedFrom(color);
        this.g = Color3f.getGreenFrom(color);
        this.b = Color3f.getBlueFrom(color);
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        @Expose
        public final int color;

        public Builder(int color, int size) {
            this.color = color;
            super.setWidth(size);
            super.setHeight(size);
        }

        @Deprecated
        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Deprecated
        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Override
        public ColorButton build(Module module) {
            return new ColorButton(module, this);
        }
    }
}
