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

/**
 * Locate a two-dimensional vector point on screen or define a rect size (w*h)
 * Although the return type is float, but it's better to be an integer
 * There are three implementations to use
 */
public interface ILocator {

    /**
     * Get located x
     *
     * @param width parent element (host) width,
     *              or screen width (AKA scaled game window width / framebuffer width)
     * @return located x
     */
    float getLocatedX(float width);

    /**
     * Get located y
     *
     * @param height parent element (host) height,
     *               or screen height (AKA scaled game window height / framebuffer height)
     * @return located y
     */
    float getLocatedY(float height);
}
