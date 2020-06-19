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

import icyllis.modernui.ui.test.IWidget;

import javax.annotation.Nonnull;

@Deprecated
public abstract class ScrollGroup implements IWidget {

    protected final IScrollHost window;

    protected float centerX;

    protected float y1, y2;

    protected float height;

    /**
     * Must specify height in constructor
     */
    public ScrollGroup(@Nonnull IScrollHost window) {
        this.window = window;
    }

    public void locate(float px, float py) {
        centerX = px;
        y1 = py;
        y2 = y1 + height;
    }

    /*public void onLayout(float left, float right, float y) {
        this.x1 = left;
        this.x2 = right;
        this.width = right - left;
        this.centerX = (left + right) / 2f;
        this.y1 = y;
        this.y2 = y + height;
    }*/

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public float getLeft() {
        return 0;
    }

    @Override
    public float getRight() {
        return 0;
    }

    @Override
    public float getTop() {
        return y1;
    }

    @Override
    public float getBottom() {
        return y2;
    }

    public abstract void updateVisible(float top, float bottom);

}
