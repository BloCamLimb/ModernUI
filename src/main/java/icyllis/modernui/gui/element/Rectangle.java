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

import icyllis.modernui.gui.master.DrawTools;

import java.util.function.Function;

public class Rectangle extends Base {

    public Function<Integer, Float> fakeW, fakeH;

    /**
     * Logical size
     */
    public float sizeW, sizeH;

    public float colorR, colorG, colorB;

    public Rectangle(Function<Integer, Float> x, Function<Integer, Float> y, Function<Integer, Float> w, Function<Integer, Float> h, float r, float g, float b, float a) {
        this.fakeX = x;
        this.fakeY = y;
        this.fakeW = w;
        this.fakeH = h;
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.alpha = a;
    }

    @Override
    public void draw() {
        DrawTools.fillRectWithColor(renderX, renderY, renderX + sizeW, renderY + sizeH, colorR, colorG, colorB, alpha);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sizeW = fakeW.apply(width);
        sizeH = fakeH.apply(height);
    }

}
