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

package icyllis.modernui.api.element;

import icyllis.modernui.api.animation.IAnimationBuilder;

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
     * @param x given game window width, return x position
     * @param y given game window height, return y position
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
     * Set element size
     * @param w element width
     * @param h element height
     * @return builder
     */
    IRectangleBuilder setSize(float w, float h);

    /**
     * Set element size
     * @param w given game window width, return element width
     * @param h given game window height, return element height
     * @return builder
     */
    IRectangleBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h);

    IRectangleBuilder applyToX(Consumer<IAnimationBuilder> animation);

    IRectangleBuilder applyToA(Consumer<IAnimationBuilder> animation);

    IRectangleBuilder applyToW(Consumer<IAnimationBuilder> animation);
}
