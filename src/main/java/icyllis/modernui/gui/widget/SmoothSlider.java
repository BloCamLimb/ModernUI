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

package icyllis.modernui.gui.widget;

import icyllis.modernui.gui.element.IElement;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class SmoothSlider extends Slider implements IElement, IGuiEventListener {

    private Consumer<Double> receiver;

    public SmoothSlider(float width, double initPercent, Consumer<Double> receiver) {
        super(width);
        this.slideOffset = getMaxSlideOffset() * MathHelper.clamp(initPercent, 0, 1);
        this.receiver = receiver;
    }

    @Override
    protected void slideToOffset(float offset) {
        slideOffset = MathHelper.clamp(offset, 0, getMaxSlideOffset());
        double slidePercent = slideOffset / getMaxSlideOffset();
        receiver.accept(slidePercent);
    }
}
