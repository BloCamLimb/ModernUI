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

package icyllis.modernui.test.discard;

/**
 * This is an really basic interface and can be used everywhere
 */
@Deprecated
public interface IElement {

    /**
     * Draw content you want, called every frame
     * You'd better do animations update at the top of lines
     *
     * @param time elapsed time from a gui open
     *                    unit: floating point ticks, 20.0 ticks = 1 second
     */
    void draw(float time);

    /**
     * This is global method.
     *
     * Called when game window size changed, used to reset position, use layout for multiple elements
     *
     * @param width scaled game window width
     * @param height scaled game window height
     */
    default void resize(int width, int height) {}

    /**
     * This is global method.
     *
     * Ticks something you like, used by % calculation to update gui values or state
     *
     * @param ticks elapsed ticks from a gui open, 20 tick = 1 second
     */
    default void tick(int ticks) {}

}
