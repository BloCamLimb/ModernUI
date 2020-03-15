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

package icyllis.modernui.gui.element;

/**
 * This interface can be used everywhere
 */
public interface IElement {

    /**
     * Draw content you want, called every frame
     * You'd better do animations update at the top of lines
     * @param currentTime elapsed time from a gui open
     *                    unit: floating point ticks, 20.0 ticks = 1 second
     */
    void draw(float currentTime);

    /**
     * Called when game window size changed
     * {@link Element#xResizer} is an example
     * @param width scaled window width
     * @param height scaled window height
     */
    void resize(int width, int height);

    /**
     * Ticks something you like
     * By % calculation to get better performance if you want to update gui values
     * @param ticks elapsed ticks from a gui open, 20 tick = 1 second
     */
    default void tick(int ticks) {}

    /**
     * The priority of this element, higher value will render last,
     * so it will render at the top, such as you want to render tooltips
     * First module added will render last as well
     * Default order is followed by element build order
     * @return custom priority
     */
    default int priority() {
        return 0;
    }
}
