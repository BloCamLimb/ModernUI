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

import icyllis.modernui.api.animation.IAnimationBuilder;
import icyllis.modernui.api.element.IRectangleBuilder;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalAnimationManager;

import java.util.function.Consumer;
import java.util.function.Function;

public class Rectangle extends Base implements IRectangleBuilder {

    protected Function<Integer, Float> GWtBW, GWtBH;

    protected float sizeW, sizeH;

    private float colorR, colorG, colorB;

    public Rectangle() {

    }

    @Override
    public void draw() {
        DrawTools.fillRectWithColor(renderX, renderY, renderX + sizeW, renderY + sizeH, colorR, colorG, colorB, alpha);
    }

    @Override
    public IRectangleBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y) {
        GWtBX = x;
        GWtBY = y;
        return this;
    }

    @Override
    public IRectangleBuilder setPos(float x, float y) {
        GWtBX = w -> w / 2f + x;
        GWtBY = h -> h / 2f + y;
        return this;
    }

    @Override
    public IRectangleBuilder setAlpha(float a) {
        alpha = a;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        float w = GWtBW.apply(width);
        float h = GWtBH.apply(height);
        sizeW = w;
        sizeH = h;
    }

    @Override
    public IRectangleBuilder setColor(int rgb) {
        float r = (rgb >> 16 & 255) / 255.0f;
        float g = (rgb >> 8 & 255) / 255.0f;
        float b = (rgb & 255) / 255.0f;
        colorR = r;
        colorG = g;
        colorB = b;
        return this;
    }

    @Override
    public IRectangleBuilder setSize(float w, float h) {
        GWtBW = g -> w;
        GWtBH = g -> h;
        return this;
    }

    @Override
    public IRectangleBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h) {
        GWtBW = w;
        GWtBH = h;
        return this;
    }

    @Override
    public IRectangleBuilder applyToX(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.create(animation, a -> renderX = a);
        return this;
    }

    @Override
    public IRectangleBuilder applyToA(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.create(animation, a -> alpha = a);
        return this;
    }

    @Override
    public IRectangleBuilder applyToW(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.create(animation, a -> sizeW = a);
        return this;
    }
}
