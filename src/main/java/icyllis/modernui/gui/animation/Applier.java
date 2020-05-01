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

package icyllis.modernui.gui.animation;

import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Smooth applier without value changed suddenly
 */
public class Applier {

    private final float startValue;
    private final float endValue;

    private float initValue;
    private float targetValue;

    private final Supplier<Float> getter;
    private final Consumer<Float> setter;

    private IInterpolator interpolator = IInterpolator.LINEAR;

    public Applier(float startValue, float endValue, Supplier<Float> getter, Consumer<Float> setter) {
        this.startValue = startValue;
        this.endValue = endValue;
        this.getter = getter;
        this.setter = setter;
    }

    public Applier setInterpolator(IInterpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public void record(boolean inverted, boolean restart) {
        if (restart) {
            if (inverted) {
                initValue = endValue;
            } else {
                initValue = startValue;
            }
        } else {
            initValue = getter.get();
        }
        if (inverted) {
            targetValue = startValue;
        } else {
            targetValue = endValue;
        }
    }

    public void update(float progress) {
        progress = interpolator.getInterpolation(progress);
        float value = MathHelper.lerp(progress, initValue, targetValue);
        setter.accept(value);
    }

    /*public static class Resizable extends Applier {

        private Function<Integer, Float> initResizer;

        private Function<Integer, Float> targetResizer;

        private Consumer<Function<Integer, Float>> resizerSetter;

        private boolean useHeight;

        public Resizable(Function<Integer, Float> initResizer, Function<Integer, Float> targetResizer, Consumer<Float> resultSetter, Consumer<Function<Integer, Float>> resizerSetter, boolean useHeight) {
            super(0, 0, resultSetter);
            this.initResizer = initResizer;
            this.targetResizer = targetResizer;
            this.resizerSetter = resizerSetter;
            this.useHeight = useHeight;
        }

        @Override
        public void resize(int width, int height) {
            initValue = initResizer.apply(useHeight ? height : width);
            targetValue = targetResizer.apply(useHeight ? height : width);
        }

        @Override
        public void apply(float progress) {
            super.apply(progress);
            if (progress == 1) {
                resizerSetter.accept(targetResizer);
            }
        }
    }*/
}
