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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modern.api.animation.IAnimationBuilder;
import icyllis.modern.api.element.IColorBuilder;
import icyllis.modern.ui.master.DrawTools;
import icyllis.modern.ui.master.GlobalAnimationManager;

import java.util.function.Consumer;

public class UIRectangle extends UIElement<IColorBuilder> implements IColorBuilder {

    private float colorR, colorG, colorB;

    public UIRectangle() {

    }

    @Override
    public void draw() {
        DrawTools.fillRectWithColor(renderX, renderY, renderX + sizeW, renderY + sizeH, colorR, colorG, colorB, alpha);
    }

    @Override
    public IColorBuilder setColor(int rgb) {
        float r = (rgb >> 16 & 255) / 255.0f;
        float g = (rgb >> 8 & 255) / 255.0f;
        float b = (rgb & 255) / 255.0f;
        colorR = r;
        colorG = g;
        colorB = b;
        return this;
    }

    @Override
    public IColorBuilder applyToX(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.create(animation, a -> renderX = a);
        return this;
    }

    @Override
    public IColorBuilder applyToA(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.create(animation, a -> alpha = a);
        return this;
    }

    @Override
    public IColorBuilder applyToW(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.create(animation, a -> sizeW = a);
        return this;
    }
}
