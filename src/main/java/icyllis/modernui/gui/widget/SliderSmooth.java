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

import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.master.IHost;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

public class SliderSmooth extends Slider {

    private double value;

    private final double minValue;

    private final double maxValue;

    private final float stepSize;

    private IListener listener;

    public SliderSmooth(IHost host, Builder builder) {
        super(host, builder);
        minValue = builder.minValue;
        maxValue = builder.maxValue;
        stepSize = builder.stepSize;
    }

    /**
     * Callback constructor
     * @param value init value
     * @param listener value listener
     * @return instance
     */
    public SliderSmooth buildCallback(double value, @Nonnull IListener listener) {
        value = MathHelper.clamp(value, minValue, maxValue);
        double p = (value - minValue) / (maxValue - minValue);
        this.slideOffset = getMaxSlideOffset() * p;
        this.listener = listener;
        return this;
    }

    @Override
    protected void onStopDragging() {
        listener.onSliderStopChange(value);
    }

    @Override
    protected void slideToOffset(double offset) {
        double prev = slideOffset;
        slideOffset = MathHelper.clamp(offset, 0, getMaxSlideOffset());
        if (prev != slideOffset) {
            double slidePercent = slideOffset / getMaxSlideOffset();
            value = MathHelper.lerp(slidePercent, minValue, maxValue);
            if (stepSize > 0) {
                value = stepSize * (Math.round(value / stepSize));
            }
            listener.onSliderChanged(value);
        }
    }

    public static class Builder extends Widget.Builder {

        @Expose
        protected final double minValue;

        @Expose
        protected final double maxValue;

        @Expose
        protected float stepSize = 0.01f;

        public Builder(double minValue, double maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            super.setHeight(3);
        }

        public Builder setStepSize(float stepSize) {
            this.stepSize = Math.max(0.0000001f, stepSize);
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
            super.setHeight(height);
            return this;
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
        public SliderSmooth build(IHost host) {
            return new SliderSmooth(host, this);
        }
    }

    /**
     * Receive slider values
     */
    public interface IListener {

        /**
         * Called as long as slider was dragged
         * @param value new value
         */
        void onSliderChanged(double value);

        /**
         * Called when stopped dragging
         * @param value
         */
        void onSliderStopChange(double value);
    }
}
