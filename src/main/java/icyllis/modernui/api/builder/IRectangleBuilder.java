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

import icyllis.modernui.gui.element.IBase;
import icyllis.modernui.gui.element.Rectangle;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IRectangleBuilder {

    /**
     * Set element relative position to given window size.
     * Set element size relative to given window size.
     * Set color(RGBA) for rectangle in hex.
     * @param x x position
     * @param y x position
     * @param w element width
     * @param h element height
     * @param RGBA color
     * @return builder
     */
    IRectangleBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, Function<Integer, Float> w, Function<Integer, Float> h, int RGBA);

    /**
     * Build this element to pool
     */
    void buildToPool(Consumer<IBase> pool);

    /**
     * Build this element to pool with modifiers
     */
    void buildToPool(Consumer<IBase> pool, Consumer<Rectangle> consumer);
}
