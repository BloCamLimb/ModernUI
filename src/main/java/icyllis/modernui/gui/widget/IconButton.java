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
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;

import javax.annotation.Nonnull;

public class IconButton extends Button {

    private final Icon icon;

    public IconButton(IHost host, Builder builder) {
        super(host, builder);
        this.icon = builder.icon;
    }

    @Override
    public IconButton setDefaultClickable(boolean b) {
        super.setDefaultClickable(b);
        return this;
    }

    @Override
    public IconButton setCallback(Runnable r) {
        super.setCallback(r);
        return this;
    }

    @Override
    public IconButton setOnetimeCallback(Runnable r) {
        super.setOnetimeCallback(r);
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        canvas.setRGBA(brightness, brightness, brightness, 1.0f);
        canvas.drawIcon(icon, x1, y1, x2, y2);
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        @Expose
        protected final Icon icon;

        public Builder(@Nonnull Icon icon) {
            this.icon = icon;
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

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
        public IconButton build(IHost host) {
            return new IconButton(host, this);
        }
    }
}
