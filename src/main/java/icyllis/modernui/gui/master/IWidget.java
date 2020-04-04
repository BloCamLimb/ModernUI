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

/**
 * Widget has its rect area and position, also can listen mouse events as default, most widely used in gui
 */
public interface IWidget extends IElement, IMouseListener {

    /**
     * Set widget position, or use layout for multiple widgets
     * This position is not always left top position, or be center position..
     * But must be matched to all the four methods below
     * @param x x pos
     * @param y y pos
     */
    void setPos(float x, float y);

    /**
     * Get width
     * @return width
     */
    float getWidth();

    /**
     * Get height
     * @return height
     */
    float getHeight();

    /**
     * Get left (x1)
     * @return left
     */
    float getLeft();

    /**
     * Get right (x2)
     * @return right
     */
    float getRight();

    /**
     * Get top (y1)
     * @return top
     */
    float getTop();

    /**
     * Get bottom (y2)
     * @return bottom
     */
    float getBottom();

}
