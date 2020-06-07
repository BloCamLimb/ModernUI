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

package icyllis.modernui.ui.layout;

import icyllis.modernui.ui.test.IViewRect;

/**
 * Identify a 2-dimensional vector point on parent view as pivot point,
 * and identify width and height to form a view rect area
 */
public interface ILayout {

    /**
     * Get layout x on screen
     *
     * @param prev   previous view or parent view
     * @param parent parent view or root view
     * @return view x
     */
    int getLayoutX(IViewRect prev, IViewRect parent);

    /**
     * Get layout y on screen
     *
     * @param prev   previous view or parent view
     * @param parent parent view or root view
     * @return view y
     */
    int getLayoutY(IViewRect prev, IViewRect parent);

    /**
     * Get layout width on screen
     *
     * @param prev   previous view or parent view
     * @param parent parent view or root view
     * @return view width
     */
    int getLayoutWidth(IViewRect prev, IViewRect parent);

    /**
     * Get layout height on screen
     *
     * @param prev   previous view or parent view
     * @param parent parent view or root view
     * @return view height
     */
    int getLayoutHeight(IViewRect prev, IViewRect parent);
}
