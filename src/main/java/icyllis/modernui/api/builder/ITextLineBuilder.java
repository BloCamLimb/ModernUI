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

import java.util.function.Function;
import java.util.function.Supplier;

public interface ITextLineBuilder {

    /**
     * Set relative position to window size.
     * Set text content.
     * Set text alignment: 0 = left, 0.25 = center, 0.5 = right
     * Set text color in hex
     * Set scaling.
     * @param x x position
     * @param y y position
     * @param text text
     * @param align text align
     * @param RGBA color
     * @param scale scale
     * @return builder
     */
    ITextLineBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, Supplier<String> text, float align, int RGBA, float scale);
}
