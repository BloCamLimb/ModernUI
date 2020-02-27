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

import icyllis.modernui.api.global.IStatusSetter;

import java.util.function.Consumer;
import java.util.function.Function;

public class HighStatusAnimation extends DisposableAnimation implements IStatusSetter {

    private boolean running = false;
    private boolean status = false;

    private boolean finishSwitch = false;

    public HighStatusAnimation(Consumer<Float> receiver, Consumer<Function<Integer, Float>> relativeReceiver) {
        super(-1, receiver, relativeReceiver);
    }

    @Override
    public void update(float currentTime) {
        if (running) {
            if (startTime == -1) {
                calculateStartTime(currentTime);
            } else {
                super.update(currentTime);
            }
        }
        if (finish) {
            startTime = -1;
            running = false;
            finish = false;
            if (finishSwitch) {
                running = true;
                status = false;
                finishSwitch = false;
            }
        }
    }

    private void calculateStartTime(float currentTime) {
        float p;
        switch (motionType) {
            case UNIFORM:
                p = (targetValue - value) / (targetValue - initValue);
                if (status)
                    p = 1 - p;
                startTime = currentTime - fixedTiming * p;
                break;
            case SINE:
                p = (targetValue - value) / (targetValue - initValue);
                if (status)
                    p = 1 - p;
                p = (float) (Math.asin(p) * 2 / Math.PI);
                startTime = currentTime - fixedTiming * p;
                break;
        }
    }

    @Override
    public void setStatus(boolean b) {
        if (!status && b) {
            running = true;
            status = true;
        } else if (status && !b) {
            if (running) {
                finishSwitch = true;
            } else {
                running = true;
                status = false;
            }
        }
    }

    @Override
    protected void updateUniform(float currentTime) {
        if (status)
            super.updateUniform(currentTime);
        else {
            float d = currentTime - startTime;
            value = targetValue + (initValue - targetValue) * (d / fixedTiming);
            if (value <= initValue) {
                value = initValue;
                finish = true;
                relativeReceiver.accept(fakeTargetValue);
            }
        }
    }

    @Override
    protected void updateSine(float currentTime) {
        if (status)
            super.updateSine(currentTime);
        else {
            float d = currentTime - startTime;
            float p = d / fixedTiming;
            value = targetValue + (initValue - targetValue) * (float) Math.sin(p * Math.PI / 2f);
            if (p >= 1) {
                value = initValue;
                finish = true;
                relativeReceiver.accept(fakeTargetValue);
            }
        }
    }

    @Override
    public boolean isFinish() {
        return false;
    }
}
