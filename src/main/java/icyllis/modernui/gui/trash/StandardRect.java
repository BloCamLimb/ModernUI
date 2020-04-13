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

package icyllis.modernui.gui.trash;

import icyllis.modernui.gui.master.DrawTools;

import java.util.function.Function;

/**
 * Example
 */
@Deprecated
public class StandardRect extends Element {

    public Function<Integer, Float> wResizer, hResizer;

    /**
     * Logical size
     */
    public float sizeW, sizeH;

    public float colorR, colorG, colorB, opacity;

    public StandardRect(Function<Integer, Float> x, Function<Integer, Float> y, Function<Integer, Float> w, Function<Integer, Float> h, int RGBA) {
        super(x, y);
        this.wResizer = w;
        this.hResizer = h;
        this.opacity = (RGBA >> 24 & 255) / 255.0f;
        this.colorR = (RGBA >> 16 & 255) / 255.0f;
        this.colorG = (RGBA >> 8 & 255) / 255.0f;
        this.colorB = (RGBA & 255) / 255.0f;
    }

    @Override
    public void draw(float time) {
        //DrawTools.fillRectWithColor(x, y, x + sizeW, y + sizeH, colorR, colorG, colorB, opacity);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        sizeW = wResizer.apply(width);
        sizeH = hResizer.apply(height);
    }
}
