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

package icyllis.modern.ui.animation;

import icyllis.modern.api.animation.IAnimationBuilder;
import icyllis.modern.api.animation.MotionType;

import java.util.function.Supplier;

public class UniversalAnimation implements Supplier<Float>, IAnimationBuilder {

    private float value;

    private final float initValue;

    private float targetValue, startTime, fixedTiming = 1.0f;

    private MotionType motionType = MotionType.UNIFORM;

    private boolean finish = false;

    public UniversalAnimation(float startTime, float init) {
        this.startTime = startTime;
        initValue = targetValue = init;
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
    public Float get() {
        return value;
    }

    @Override
    public IAnimationBuilder setTarget(float target) {
        targetValue = target;
        return this;
    }

    @Override
    public IAnimationBuilder setTranslate(float translate) {
        targetValue = initValue + translate;
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
