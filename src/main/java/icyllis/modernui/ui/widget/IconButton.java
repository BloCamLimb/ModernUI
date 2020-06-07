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

package icyllis.modernui.ui.widget;

import com.google.gson.annotations.Expose;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.graphics.renderer.Icon;
import icyllis.modernui.ui.layout.Align9D;
import icyllis.modernui.ui.test.Locator;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.Widget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IconButton extends Button {

    private final Icon icon;

    public IconButton(IHost host, Builder builder) {
        super(host, builder);
        this.icon = builder.icon;
    }

    @Override
    public IconButton buildCallback(@Nullable Runnable r) {
        super.buildCallback(r);
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
        canvas.setRGBA(getModulatedBrightness(), getModulatedBrightness(), getModulatedBrightness(), 1.0f);
        canvas.drawIcon(icon, x1, y1, x2, y2);
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
