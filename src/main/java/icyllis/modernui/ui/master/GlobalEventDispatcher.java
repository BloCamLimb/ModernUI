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

package icyllis.modernui.ui.master;

import java.util.LinkedList;
import java.util.List;

public final class GlobalEventDispatcher {

    private final List<ViewRootImpl> windows;

    // scaled mouseX, mouseY on screen
    double mouseX;
    double mouseY;

    // mouse hovered views, no matter whether the view is enabled or not
    private final LinkedList<View> route = new LinkedList<>();

    GlobalEventDispatcher(List<ViewRootImpl> windows) {
        this.windows = windows;
    }

    void onCursorPosEvent(double x, double y) {
        mouseX = x;
        mouseY = y;

        route.clear();
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onCursorPosEvent(route, x, y)) {
                break;
            }
        }
    }
}
