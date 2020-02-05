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

import icyllis.modern.api.animation.IAnimationBuilder;
import icyllis.modern.api.element.IColorBuilder;
import icyllis.modern.ui.animation.UniversalAnimation;
import icyllis.modern.ui.master.DrawTools;
import icyllis.modern.ui.master.GlobalAnimationManager;
import icyllis.modern.ui.master.GlobalElementBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIColorRect extends UIElement<IColorBuilder> implements IColorBuilder {

    private Supplier<Float> colorR, colorG, colorB;

    public UIColorRect() {

    }

    @Override
    public void draw() {
        float x = renderX.get();
        float y = renderY.get();
        DrawTools.fillRectWithColor(x, y, x + sizeW.get(), y + sizeH.get(), colorR.get(), colorG.get(), colorB.get(), alpha.get());
    }

    @Override
    public IColorBuilder setColor(int rgb) {
        float r = (float)(rgb >> 16 & 255) / 255.0F;
        float g = (float)(rgb >> 8 & 255) / 255.0F;
        float b = (float)(rgb & 255) / 255.0F;
        colorR = () -> r;
        colorG = () -> g;
        colorB = () -> b;
        return this;
    }

    @Override
    public IColorBuilder applyToX(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.scheduleAnimationBuild(() -> renderX = GlobalAnimationManager.INSTANCE.create(animation, renderX.get()));
        return this;
    }

    @Override
    public IColorBuilder applyToA(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.scheduleAnimationBuild(() -> alpha = GlobalAnimationManager.INSTANCE.create(animation, alpha.get()));
        return this;
    }

    @Override
    public IColorBuilder applyToW(Consumer<IAnimationBuilder> animation) {
        GlobalAnimationManager.INSTANCE.scheduleAnimationBuild(() -> sizeW = GlobalAnimationManager.INSTANCE.create(animation, sizeW.get()));
        return this;
    }
}
