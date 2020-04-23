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

package icyllis.modernui.gui.layout;

import icyllis.modernui.gui.master.IWidget;

import java.util.Collections;
import java.util.List;

public class WidgetLayout {

    private List<? extends IWidget> widgets;

    private Direction direction;

    private float spacing;

    public WidgetLayout(List<? extends IWidget> widgets, Direction direction, float spacing) {
        this.widgets = widgets;
        this.direction = direction;
        this.spacing = spacing;
    }

    public void layout(float x, float y) {
        float x1 = x;
        float y1 = y;
        float t = 0;
        switch (direction) {
            case HORIZONTAL_POSITIVE:
                for (IWidget widget : widgets) {
                    widget.locate(x1, y1);
                    x1 = widget.getRight() + spacing;
                }
                break;
            case HORIZONTAL_CENTER:
                for (IWidget widget : widgets) {
                    t += widget.getWidth() + spacing;
                }
                t -= spacing;
                x1 -= t / 2f;
                for (IWidget widget : widgets) {
                    widget.locate(x1, y1);
                    x1 += widget.getWidth() + spacing;
                }
                break;
            case HORIZONTAL_NEGATIVE:
                Collections.reverse(widgets);
                for (IWidget widget : widgets) {
                    x1 -= widget.getWidth();
                    widget.locate(x1, y1);
                    x1 = widget.getLeft() - spacing;
                }
                break;
            case VERTICAL_POSITIVE:
                for (IWidget widget : widgets) {
                    widget.locate(x1, y1);
                    y1 = widget.getBottom() + spacing;
                }
                break;
            case VERTICAL_CENTER:
                for (IWidget widget : widgets) {
                    t += widget.getHeight() + spacing;
                }
                t -= spacing;
                y1 -= t / 2f;
                for (IWidget widget : widgets) {
                    widget.locate(x1, y1);
                    y1 += widget.getHeight() + spacing;
                }
                break;
            case VERTICAL_NEGATIVE:
                Collections.reverse(widgets);
                for (IWidget widget : widgets) {
                    y1 -= widget.getHeight();
                    widget.locate(x1, y1);
                    y1 = widget.getTop() - spacing;
                }
                break;
        }
    }

    public enum Direction {
        HORIZONTAL_POSITIVE,
        HORIZONTAL_CENTER,
        HORIZONTAL_NEGATIVE,
        VERTICAL_POSITIVE,
        VERTICAL_CENTER,
        VERTICAL_NEGATIVE
    }
}
