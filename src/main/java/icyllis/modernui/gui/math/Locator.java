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

package icyllis.modernui.gui.math;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import icyllis.modernui.gui.master.Widget;

public class Locator {

    public static final Locator CENTER = new Locator();

    @Expose
    @SerializedName("horizontalAlignment")
    private HorizontalAlign horizontalAlign = HorizontalAlign.CENTER;

    @Expose
    @SerializedName("verticalAlignment")
    private VerticalAlign verticalAlign = VerticalAlign.CENTER;

    @Expose
    @SerializedName("xOffset")
    private float horizontalOffset = 0;

    @Expose
    @SerializedName("yOffset")
    private float verticalOffset = 0;

    public Locator() {

    }

    public void setHorizontalAlign(HorizontalAlign horizontalAlign) {
        this.horizontalAlign = horizontalAlign;
    }

    public void setVerticalAlign(VerticalAlign verticalAlign) {
        this.verticalAlign = verticalAlign;
    }

    public void setHorizontalOffset(float horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    public void setVerticalOffset(float verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public void locate(Widget widget, int width, int height) {
        float x = 0;
        switch (horizontalAlign) {
            case LEFT:
                x += horizontalOffset;
                break;
            case CENTER:
                x += width / 2f + horizontalOffset;
                break;
            case RIGHT:
                x += width + horizontalOffset;
                break;
        }
        float y = 0;
        switch (verticalAlign) {
            case TOP:
                y += verticalOffset;
                break;
            case CENTER:
                y += height / 2f + verticalOffset;
                break;
            case BOTTOM:
                y += height + verticalOffset;
                break;
        }
        widget.setPos(x, y);
    }

    public enum HorizontalAlign {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum VerticalAlign {
        TOP,
        CENTER,
        BOTTOM
    }
}
