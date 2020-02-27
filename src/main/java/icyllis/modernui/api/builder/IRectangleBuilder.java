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

package icyllis.modernui.api.builder;

import icyllis.modernui.gui.element.Rectangle;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IRectangleBuilder {

    /**
     * Set element relative position to window center. (0,0) will be at crosshair
     * @param x x position
     * @param y y position
     * @return builder
     */
    IRectangleBuilder setPos(float x, float y);

    /**
     * Set element relative position to given window size.
     * @param x x position
     * @param y x position
     * @return builder
     */
    IRectangleBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y);

    /**
     * Set initial constant alpha value, default is 1.0f.
     * You don't need this method if you create animation for alpha.
     * @param a alpha
     * @return builder
     */
    IRectangleBuilder setAlpha(float a);

    /**
     * Set color for rectangle in hex code
     * @param rgb RGB
     * @return builder
     */
    IRectangleBuilder setColor(int rgb);

    /**
     * Set color for rectangle in float
     * @param r red
     * @param g green
     * @param b blue
     * @return builder
     */
    IRectangleBuilder setColor(float r, float g, float b);

    /**
     * Set element size (relativity undefined)
     * @param w element width
     * @param h element height
     * @return builder
     */
    IRectangleBuilder setSize(float w, float h);

    /**
     * Set element size relative to given window size.
     * @param w element width
     * @param h element height
     * @return builder
     */
    IRectangleBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h);

    void buildToPool();

    void buildToPool(Consumer<Rectangle> consumer);
}
