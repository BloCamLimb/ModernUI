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
 * This is an really basic interface that represents a drawable element in gui
 * But also can be used for update animations. A.K.A Frame Event Listener
 * And can also listen resize and tick events from current module
 */
public interface IDrawable {

    /**
     * Draw content you want, called every frame
     * You have to do animations update at the top of lines
     *
     * @param canvas The canvas provided by module, used to draw everything
     * @param time elapsed time from a gui open
     *                    unit: floating point ticks, 20.0 ticks = 1 second
     */
    void draw(Canvas canvas, float time);

    /**
     * Called when game window size changed, used to reset position, use layout for multiple elements
     *
     * @param width scaled game window width
     * @param height scaled game window height
     */
    default void resize(int width, int height) {}

    /**
     * Ticks something you like, used by % calculation to update gui values or state
     *
     * @param ticks elapsed ticks from a gui open, 20 tick = 1 second
     */
    default void tick(int ticks) {}

    /**
     * Get current alpha for global shader uniform
     *
     * @return alpha
     */
    default float getAlpha() {
        return 1.0f;
    }

    /**
     * Set current alpha, which called from module
     *
     * @param alpha specific alpha
     */
    default void setAlpha(float alpha) {}

}
