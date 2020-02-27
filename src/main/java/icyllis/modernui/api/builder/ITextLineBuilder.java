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

import java.util.function.Supplier;

public interface ITextLineBuilder {

    /**
     * Set element relative position to window center. (0,0) will be at crosshair
     * @param x x position
     * @param y y position
     * @return builder
     */
    ITextLineBuilder setPos(float x, float y);

    /**
     * Set initial constant alpha value, default is 1.0f.
     * @param a alpha
     * @return builder
     */
    ITextLineBuilder setAlpha(float a);

    ITextLineBuilder text(Supplier<String> text);

    /**
     * Text alignment: 0 = left, 0.25 = center, 0.5 = right
     * @param align align
     */
    ITextLineBuilder align(float align);

    ITextLineBuilder color(Supplier<Integer> color);

    ITextLineBuilder scale(Supplier<Float> scale);

    ITextLineBuilder style();
}
