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

package icyllis.modernui.ui.test;

@Deprecated
public interface IViewRect {

    /**
     * Get view width
     *
     * @return width
     */
    int getWidth();

    /**
     * Get view height
     *
     * @return height
     */
    int getHeight();

    /**
     * Get view relative x1
     *
     * @return left
     */
    int getLeft();

    /**
     * Get view relative y1
     *
     * @return top
     */
    int getTop();

    /**
     * Get view relative x2
     *
     * @return right
     */
    int getRight();

    /**
     * Get view relative y2
     *
     * @return bottom
     */
    int getBottom();
}
