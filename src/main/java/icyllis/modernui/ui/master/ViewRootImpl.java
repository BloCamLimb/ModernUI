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

import icyllis.modernui.graphics.math.Point;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The top of a view hierarchy, implementing the needed protocol between View
 * and the UIManager.
 */
public final class ViewRootImpl implements IViewParent {

    public boolean startDragAndDrop(@Nonnull View view, @Nullable Object data, @Nullable View.DragShadow shadow, int flags) {
        final UIManager uiManager = UIManager.INSTANCE;

        Point center = new Point();
        if (shadow == null) {
            shadow = new View.DragShadow(view);
            if (view.isMouseHovered()) {
                // default strategy
                center.x = (int) uiManager.getViewMouseX(view);
                center.y = (int) uiManager.getViewMouseY(view);
            } else {
                shadow.onProvideShadowCenter(center);
            }
        } else {
            shadow.onProvideShadowCenter(center);
        }

    }

    @Nullable
    @Override
    public IViewParent getParent() {
        return null;
    }

    @Override
    public void requestLayout() {

    }

    @Override
    public float getScrollX() {
        return 0;
    }

    @Override
    public float getScrollY() {
        return 0;
    }
}
