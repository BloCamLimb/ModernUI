/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test.widget;

import com.google.gson.annotations.Expose;
import icyllis.modernui.test.discard.IHost;
import icyllis.modernui.test.discard.Widget;
import icyllis.modernui.test.discard.Align9D;
import icyllis.modernui.test.discard.Locator;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

@Deprecated
public class SliderDiscrete extends Slider {

    private int segment;

    private final int maxSegment;

    private final int minValue;

    private final int maxValue;

    private IListener listener;

    public SliderDiscrete(IHost host, Builder builder) {
        super(host, builder);
        minValue = builder.minValue;
        maxValue = builder.maxValue;
        this.maxSegment = (maxValue - minValue) / builder.stepSize;
    }

    /**
     * Callback constructor
     * @param value init value
     * @param listener value listener
     * @return instance
     */
    public SliderDiscrete buildCallback(int value, @Nonnull IListener listener) {
        value = MathHelper.clamp(value, minValue, maxValue);
        double p = (value - minValue) / (double) (maxValue - minValue);
        segment = (int) Math.round(p * maxSegment);
        this.listener = listener;
        updateSlideOffset();
        return this;
    }

    @Override
    protected void slideToOffset(double offset) {
        int prev = segment;
        double p = MathHelper.clamp(offset / getMaxSlideOffset(), 0.0, 1.0);
        segment = (int) Math.round(p * maxSegment);
        if (prev != segment) {
            updateSlideOffset();
            int value = minValue + segment;
            listener.onSliderChanged(this, value);
        }
    }

    @Override
    protected void onStopDragging() {
        listener.onSliderStopChange(this, minValue + segment);
    }

    private void updateSlideOffset() {
        if (maxSegment == 0) {
            slideOffset = getMaxSlideOffset();
        } else {
            slideOffset = getMaxSlideOffset() * segment / maxSegment;
        }
    }

    public static class Builder extends Widget.Builder {

        @Expose
        private final int minValue;

        @Expose
        private final int maxValue;

        @Expose
        private int stepSize = 1;

        public Builder(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            super.setHeight(3);
        }

        public Builder setStepSize(int stepSize) {
            this.stepSize = Math.max(1, stepSize);
            return this;
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Deprecated
        @Override
        public Builder setHeight(float height) {
            throw new RuntimeException();
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Nonnull
        @Override
        public SliderDiscrete build(IHost host) {
            return new SliderDiscrete(host, this);
        }
    }

    /**
     * Receive slider values
     */
    public interface IListener {

        /**
         * Called as long as slider was dragged
         * @param slider slider
         * @param value new value
         */
        void onSliderChanged(SliderDiscrete slider, int value);

        /**
         * Called when stopped dragging
         * @param slider slider
         * @param value current value
         */
        void onSliderStopChange(SliderDiscrete slider, int value);
    }
}
