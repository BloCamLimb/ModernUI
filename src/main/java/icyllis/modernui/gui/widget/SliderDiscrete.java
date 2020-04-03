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

import icyllis.modernui.gui.master.IElement;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class SliderDiscrete extends Slider implements IElement, IGuiEventListener {

    private int segment;

    private int maxSegment;

    private Consumer<Integer> receiver;

    private Runnable applier = () -> {};

    /**
     * Constructor
     * @param width render width
     * @param segment current value, range [0, maxSegment], must be integer
     * @param maxSegment max segment, if you need 3 values, so there are 2 segments
     * @param receiver receive new segment value
     */
    public SliderDiscrete(float width, int segment, int maxSegment, Consumer<Integer> receiver) {
        super(width);
        this.segment = segment;
        this.maxSegment = maxSegment;
        this.receiver = receiver;
        updateSlideOffset();
    }

    public SliderDiscrete setApplier(Runnable r) {
        applier = r;
        return this;
    }

    @Override
    protected void slideToOffset(float offset) {
        int prev = segment;
        float p = MathHelper.clamp(offset / getMaxSlideOffset(), 0, 1);
        segment = Math.round(p * maxSegment);
        if (prev != segment) {
            updateSlideOffset();
            receiver.accept(segment);
        }
    }

    @Override
    protected void onStopDragging() {
        applier.run();
    }

    private void updateSlideOffset() {
        if (maxSegment == 0) {
            slideOffset = getMaxSlideOffset();
        } else {
            slideOffset = getMaxSlideOffset() * segment / maxSegment;
        }
    }
}
