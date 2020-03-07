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

import java.util.function.Consumer;
import java.util.function.Function;

public class Applier {

    protected float initValue;

    protected float targetValue;

    private Consumer<Float> resultSetter;

    public Applier(float initValue, float targetValue, Consumer<Float> resultSetter) {
        this.initValue = initValue;
        this.targetValue = targetValue;
        this.resultSetter = resultSetter;
    }

    public void resize(int width, int height) {

    }

    public void apply(float progress) {
        float value = initValue + (targetValue - initValue) * progress;
        resultSetter.accept(value);
    }

    public static class Resizable extends Applier {

        private Function<Integer, Float> initResizer;

        private Function<Integer, Float> targetResizer;

        private Consumer<Function<Integer, Float>> resizerSetter;

        private boolean useHeight;

        public Resizable(float initValue, float targetValue, Consumer<Float> resultSetter, Consumer<Function<Integer, Float>> resizerSetter, boolean useHeight) {
            super(initValue, targetValue, resultSetter);
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
    }
}
