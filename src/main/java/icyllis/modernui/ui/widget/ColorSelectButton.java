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

package icyllis.modernui.ui.widget;

import com.google.gson.annotations.Expose;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.Widget;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.ui.test.Locator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Used for selecting a color
 */
public class ColorSelectButton extends Widget {

    private float r, g, b;
    private int color;

    @Nullable
    private Consumer<ColorSelectButton> callback;

    private boolean selected = false;

    private float frameAlpha = 0;

    public ColorSelectButton(IHost host, Builder builder) {
        super(host, builder);
        setColor(builder.color);
    }

    public ColorSelectButton setCallback(@Nullable Consumer<ColorSelectButton> c) {
        callback = c;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        //canvas.setColor(r, g, b, 0.8f);
        canvas.drawRect(x1, y1, x2, y2);
        if (frameAlpha > 0) {
            //canvas.setColor(1, 1, 1, frameAlpha);
            canvas.drawRectOutline(x1, y1, x2, y2, 0.51f);
        }
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (callback != null) {
            callback.accept(this);
        }
        return true;
    }

    @Override
    public void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        if (!selected) {
            frameAlpha = 0.5f;
        }
    }

    @Override
    public void onMouseHoverExit() {
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
        this.r = Color3i.getRedFrom(color);
        this.g = Color3i.getGreenFrom(color);
        this.b = Color3i.getBlueFrom(color);
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

        @Nonnull
        @Override
        public ColorSelectButton build(IHost host) {
            return new ColorSelectButton(host, this);
        }
    }
}
