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

package icyllis.modernui.gui.master;

import icyllis.modernui.gui.math.Color3f;
import icyllis.modernui.gui.math.Locator;

import javax.annotation.Nullable;

//TODO Experimental
public enum LayoutEditingGui {
    INSTANCE;

    @Nullable
    private Widget hoveredWidget;

    private final Canvas canvas = new Canvas();

    private boolean working = false;

    private boolean dragging = false;

    public void setHoveredWidget(@Nullable Object obj) {
        if (!dragging) {
            if (obj == null) {
                hoveredWidget = null;
            } else if (obj instanceof Widget) {
                hoveredWidget = (Widget) obj;
            }
        }
    }

    public boolean iterateWorking() {
        working = !working;
        return working;
    }

    public void draw() {
        if (!working) {
            return;
        }
        if (hoveredWidget != null) {
            canvas.setLineAntiAliasing(true);
            canvas.setColor(Color3f.BLUE_C);
            canvas.drawRectLines(hoveredWidget.getLeft() - 2, hoveredWidget.getTop() - 2, hoveredWidget.getRight() + 2, hoveredWidget.getBottom() + 2);
            canvas.setLineAntiAliasing(false);
        }
    }

    public boolean mouseClicked(int button) {
        if (working && button == 0) {
            if (hoveredWidget != null) {
                dragging = true;
                return true;
            }
        }
        return false;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public boolean mouseDragged(double dx, double dy) {
        if (dragging && hoveredWidget != null) {
            Locator l = hoveredWidget.getLocator();
            if (l != null) {
                l.translateXOffset((float) dx);
                l.translateYOffset((float) dy);
                hoveredWidget.relocate();
                return true;
            }
        }
        return false;
    }
}
