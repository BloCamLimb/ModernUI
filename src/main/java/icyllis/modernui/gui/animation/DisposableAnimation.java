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

import icyllis.modernui.api.animation.IAnimationBuilder;
import icyllis.modernui.api.animation.MotionType;

import java.util.function.Consumer;
import java.util.function.Function;

public class DisposableAnimation implements IAnimationBuilder {

    protected float value;

    protected float initValue;

    protected Function<Integer, Float> GWtBI, GWtBT;

    protected float targetValue, startTime, fixedTiming;

    protected MotionType motionType = MotionType.UNIFORM;

    protected boolean finish = false;

    private Consumer<Float> receiver;

    public DisposableAnimation(float startTime, Consumer<Float> receiver) {
        this.startTime = startTime;
        this.receiver = receiver;
    }

    public void update(float currentTime) {
        if (currentTime <= startTime) {
            return;
        }
        switch (motionType) {
            case UNIFORM:
                updateUniform(currentTime);
                break;
            case SINE:
                updateSine(currentTime);
                break;
        }
        receiver.accept(value);
    }

    private void updateUniform(float currentTime) {
        float d = currentTime - startTime;
        value = initValue + (targetValue - initValue) * (d / fixedTiming);
        if (value >= targetValue) {
            value = targetValue;
            finish = true;
        }
    }

    private void updateSine(float currentTime) {
        float d = currentTime - startTime;
        float p = d / fixedTiming;
        float sin = (float) Math.sin(p * Math.PI / 2f);
        value = initValue + (targetValue - initValue) * sin;
        if (p >= 1) {
            value = targetValue;
            finish = true;
        }
    }

    public boolean isFinish() {
        return finish;
    }

    @Override
    public IAnimationBuilder setInit(float init) {
        initValue = init;
        GWtBI = s -> init;
        return this;
    }

    @Override
    public IAnimationBuilder setTarget(float target) {
        targetValue = target;
        GWtBT = s -> target;
        return this;
    }

    @Override
    public IAnimationBuilder setInit(Function<Integer, Float> init) {
        GWtBI = init;
        return this;
    }

    @Override
    public IAnimationBuilder setTarget(Function<Integer, Float> target) {
        GWtBT = target;
        return this;
    }

    @Override
    public IAnimationBuilder setDelay(float delay) {
        startTime += delay;
        return this;
    }

    @Override
    public IAnimationBuilder setTiming(float timing) {
        fixedTiming = timing;
        return this;
    }

    @Override
    public IAnimationBuilder setMotion(MotionType type) {
        motionType = type;
        return this;
    }
}
