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

import icyllis.modernui.api.animation.IAnimation;
import icyllis.modernui.gui.master.GlobalModuleManager;

import java.util.function.Consumer;
import java.util.function.Function;

public class DpsResizerUniAnimation implements IAnimation {

    protected float startTime;

    protected float value;

    protected Function<Integer, Float> initResizer;

    protected Function<Integer, Float> targetResizer;

    protected float initValue;

    protected float targetValue;

    protected float duration;

    protected boolean isVertical;

    protected Consumer<Float> receiver;

    protected Consumer<Function<Integer, Float>> resizerReceiver;

    protected boolean discard = false;

    protected Runnable onFinish = () -> {};

    public DpsResizerUniAnimation(float duration, boolean isVertical, Consumer<Float> receiver, Consumer<Function<Integer, Float>> resizerReceiver) {
        this.isVertical = isVertical;
        this.duration = duration;
        this.receiver = receiver;
        this.resizerReceiver = resizerReceiver;
        this.startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
    }

    public DpsResizerUniAnimation init(Function<Integer, Float> initResizer, Function<Integer, Float> targetResizer) {
        this.initResizer = initResizer;
        this.targetResizer = targetResizer;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        this.value = this.initValue = initResizer.apply(isVertical ? height : width);
        this.targetValue = targetResizer.apply(isVertical ? height : width);
    }

    public DpsResizerUniAnimation withDelay(float time) {
        startTime = startTime + time;
        return this;
    }

    public DpsResizerUniAnimation onFinish(Runnable r) {
        onFinish = r;
        return this;
    }

    @Override
    public void update(float currentTime) {
        if (currentTime <= startTime) {
            return;
        }
        float p = (currentTime - startTime) / duration;
        value = initValue + (targetValue - initValue) * p;
        if (p >= 1) {
            receiver.accept(targetValue);
            resizerReceiver.accept(targetResizer);
            onFinish.run();
            discard = true;
        } else {
            receiver.accept(value);
        }
    }

    @Override
    public boolean shouldRemove() {
        return discard;
    }
}
