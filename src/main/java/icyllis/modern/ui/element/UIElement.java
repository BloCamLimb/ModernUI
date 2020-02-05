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

package icyllis.modern.ui.element;

import icyllis.modern.api.element.IBaseBuilder;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class UIElement<T extends IBaseBuilder> implements IBaseBuilder<T> {

    /**
     * Game Window to Base X/Y/W/H
     */
    protected Function<Integer, Float> GWtBX, GWtBY, GWtBW, GWtBH;

    /**
     * Logical X/Y/W/H
     */
    protected Supplier<Float> renderX, renderY, sizeW, sizeH;

    protected Supplier<Float> alpha;

    public UIElement() {
        alpha = () -> 1.0f;
    }

    public abstract void draw();

    public void resize(int width, int height) {
        float x = GWtBX.apply(width);
        float y = GWtBY.apply(height);
        float w = GWtBW.apply(width);
        float h = GWtBH.apply(height);
        renderX = () -> x;
        renderY = () -> y;
        sizeW = () -> w;
        sizeH = () -> h;
    }

    @Override
    public T setAbsPos(float x, float y) {
        GWtBX = w -> x;
        GWtBY = h -> y;
        return (T) this;
    }

    @Override
    public T setAbsPos(Function<Integer, Float> x, Function<Integer, Float> y) {
        GWtBX = x;
        GWtBY = y;
        return (T) this;
    }

    @Override
    public T setRelPos(float x, float y) {
        GWtBX = w -> w / 2f + x;
        GWtBY = h -> h / 2f + y;
        return (T) this;
    }

    @Override
    public T setSize(float w, float h) {
        GWtBW = g -> w;
        GWtBH = g -> h;
        return (T) this;
    }

    @Override
    public T setSize(Function<Integer, Float> w, Function<Integer, Float> h) {
        GWtBW = w;
        GWtBH = h;
        return (T) this;
    }

    @Override
    public T setAlpha(float a) {
        alpha = () -> a;
        return (T) this;
    }
}
