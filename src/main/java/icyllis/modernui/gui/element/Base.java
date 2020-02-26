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

import java.util.function.Function;

public abstract class Base {

    /**
     * Game Window to Base X/Y/W/H
     */
    protected Function<Integer, Float> GWtBX = w -> 0f, GWtBY = h -> 0f;

    /**
     * Logical X/Y/W/H
     */
    public float renderX, renderY;

    protected float alpha = 1.0f;

    public Base() {

    }

    public abstract void draw();

    public void resize(int width, int height) {
        float x = GWtBX.apply(width);
        float y = GWtBY.apply(height);
        renderX = x;
        renderY = y;
    }
}
