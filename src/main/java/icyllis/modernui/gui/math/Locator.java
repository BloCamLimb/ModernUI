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
import icyllis.modernui.gui.master.Widget;

public class Locator {

    public static final Locator CENTER = new Locator();

    @Expose
    private Align9D align = Align9D.CENTER;

    @Expose
    private float xOffset = 0;

    @Expose
    private float yOffset = 0;

    public Locator() {

    }

    public void setAlign(Align9D align) {
        this.align = align;
    }

    public void setHorizontalOffset(float horizontalOffset) {
        this.xOffset = horizontalOffset;
    }

    public void setVerticalOffset(float verticalOffset) {
        this.yOffset = verticalOffset;
    }

    public void locate(Widget widget, int width, int height) {
        float x = 0;
        int horizontalAlign = align.ordinal() % 3;
        switch (horizontalAlign) {
            case 0:
                x += xOffset;
                break;
            case 1:
                x += width / 2f + xOffset;
                break;
            case 2:
                x += width + xOffset;
                break;
        }
        float y = 0;
        int verticalAlign = align.ordinal() / 3;
        switch (verticalAlign) {
            case 0:
                y += yOffset;
                break;
            case 1:
                y += height / 2f + yOffset;
                break;
            case 2:
                y += height + yOffset;
                break;
        }
        widget.locate(x, y);
    }

}
