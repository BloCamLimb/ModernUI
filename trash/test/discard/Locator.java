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

package icyllis.modernui.test.discard;

import com.google.gson.annotations.Expose;

import javax.annotation.Nonnull;

/**
 * Locate a point on gui screen
 */
@Deprecated
public class Locator {

    @Expose
    private Align9D align = Align9D.CENTER;

    @Expose
    private float xOffset = 0;

    @Expose
    private float yOffset = 0;

    public Locator() {

    }

    public Locator(Align9D align, float xOffset, float yOffset) {
        this.align = align;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public Locator(float xOffset, float yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public void setAlign(Align9D align) {
        this.align = align;
    }

    public void setXOffset(float offset) {
        xOffset = offset;
    }

    public void setYOffset(float offset) {
        yOffset = offset;
    }

    public void translateXOffset(float delta) {
        xOffset += delta;
    }

    public void translateYOffset(float delta) {
        yOffset += delta;
    }

    public float getXOffset() {
        return xOffset;
    }

    public float getYOffset() {
        return yOffset;
    }

    public void locate(@Nonnull Widget widget, int width, int height) {
        float x = align.getAlignedX(xOffset, width);
        float y = align.getAlignedY(yOffset, height);
        widget.locate(x, y);
    }

}
