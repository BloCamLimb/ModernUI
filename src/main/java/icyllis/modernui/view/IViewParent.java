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

package icyllis.modernui.view;

import javax.annotation.Nullable;

/**
 * Defines an object that can act as a parent of a view
 */
public interface IViewParent {

    /**
     * Returns the parent of this ViewParent
     *
     * @return the parent or {@code null} if parent is ViewRoot
     */
    @Nullable
    IViewParent getParent();

    /**
     * Called when something has changed which has invalidated the layout of a
     * child of this view parent. This will schedule a layout pass of the view tree.
     */
    void requestLayout();

    /**
     * The scroll offset in horizontal direction, used for view coordinate transformation.
     */
    float getScrollX();

    /**
     * The scroll offset in vertical direction, used for view coordinate transformation.
     */
    float getScrollY();
}
