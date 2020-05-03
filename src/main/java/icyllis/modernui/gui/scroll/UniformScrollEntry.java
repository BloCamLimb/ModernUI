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

import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.math.Align9D;

import javax.annotation.Nonnull;

/**
 * Entry in uniform scroll group with same height
 */
public abstract class UniformScrollEntry extends Widget {

    protected final IScrollHost window;

    protected float centerX;

    public UniformScrollEntry(@Nonnull IScrollHost window, float width, float height) {
        super(window, new Widget.Builder().setWidth(width).setHeight(height).setAlign(Align9D.TOP_CENTER));
        this.window = window;
    }

    public UniformScrollEntry(@Nonnull IScrollHost window, float width, float height, Align9D align) {
        super(window, new Widget.Builder().setWidth(width).setHeight(height).setAlign(align));
        this.window = window;
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        centerX = px;
    }

    /*public void onLayout(float left, float right, float y) {
        this.x1 = left;
        this.x2 = right;
        this.width = right - left;
        this.centerX = (left + right) / 2f;
        this.y1 = y;
        this.y2 = y + height;
    }*/

    @Nonnull
    @Override
    public Class<? extends Builder> getBuilder() {
        return Widget.Builder.class;
    }
}
