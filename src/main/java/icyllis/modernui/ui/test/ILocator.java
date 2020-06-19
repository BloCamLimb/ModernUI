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

import javax.annotation.Nullable;

/**
 * Locate a two-dimensional vector point on screen or define a rect size (w*h)
 * Although the return type is float, but it's better to be an integer
 */
@Deprecated
public interface ILocator {

    /**
     * Get located x
     *
     * @param prev      previous widget if present
     * @param hostWidth parent (host) width,
     *                  or game main window scaled width
     * @return located x
     */
    float getLocatedX(@Nullable Widget prev, float hostWidth);

    /**
     * Get located y
     *
     * @param prev       previous widget if present
     * @param hostHeight parent (host) height,
     *                   or game main window scaled height
     * @return located y
     */
    float getLocatedY(@Nullable Widget prev, float hostHeight);

    /**
     * Get sized width
     *
     * @param prev      previous widget if present
     * @param hostWidth parent (host) height,
     *                  or game main window scaled width
     * @return sized w
     */
    float getSizedW(@Nullable Widget prev, float hostWidth);

    /**
     * Get sized height
     *
     * @param prev       previous widget if present
     * @param hostHeight parent (host) height,
     *                   or game main window scaled height
     * @return sized h
     */
    float getSizedH(@Nullable Widget prev, float hostHeight);
}
