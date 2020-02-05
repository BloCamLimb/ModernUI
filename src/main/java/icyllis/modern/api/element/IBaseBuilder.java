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

package icyllis.modern.api.element;

import java.util.function.Function;

public interface IBaseBuilder<T extends IBaseBuilder> {

    /**
     * Set element absolute position
     * @param x x position
     * @param y y position
     * @return builder
     */
    T setAbsPos(float x, float y);

    /**
     * Set element absolute position
     * @param x given game window width, return x position
     * @param y given game window height, return y position
     * @return builder
     */
    T setAbsPos(Function<Integer, Float> x, Function<Integer, Float> y);

    /**
     * Set element relative position to window center. (0,0) will be at crosshair
     * @param x x position
     * @param y y position
     * @return builder
     */
    T setRelPos(float x, float y);

    /**
     * Set element size
     * @param w element width
     * @param h element height
     * @return builder
     */
    T setSize(float w, float h);

    /**
     * Set element size
     * @param w given game window width, return element width
     * @param h given game window height, return element height
     * @return builder
     */
    T setSize(Function<Integer, Float> w, Function<Integer, Float> h);

    /**
     * Set initial alpha value, default is 1.0f
     * @param a alpha
     * @return builder
     */
    T setAlpha(float a);
}
