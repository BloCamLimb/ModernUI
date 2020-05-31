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

package icyllis.modernui.ui.test;

import java.util.function.Consumer;

/**
 * Use this as fewer as possible
 */
@Deprecated
public class HSiAnimation {

    protected float startTime;

    protected float value;

    protected float initValue;

    protected float targetValue;

    protected float duration;

    protected Consumer<Float> receiver;

    protected boolean finish = false;

    private boolean running = false;

    private boolean status = false;

    private boolean finishSwitch = false;

    public HSiAnimation(float initValue, float targetValue, float duration, Consumer<Float> receiver) {
        value = this.initValue = initValue;
        this.targetValue = targetValue;
        this.duration = duration;
        this.receiver = receiver;
        startTime = -1;
    }

    public void update(float currentTime) {
        if (running) {
            if (startTime == -1) {
                calculateStartTime(currentTime);
            } else {
                updateUniform(currentTime);
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
        p = (targetValue - value) / (targetValue - initValue);
        if (status)
            p = 1 - p;
        startTime = currentTime - duration * p;
    }

    protected void updateUniform(float currentTime) {
        if (status) {
            value = initValue + (targetValue - initValue) * ((currentTime - startTime) / duration);
            if (value >= targetValue) {
                value = targetValue;
                finish = true;
            }
        } else {
            value = targetValue + (initValue - targetValue) * ((currentTime - startTime) / duration);
            if (value <= initValue) {
                value = initValue;
                finish = true;
            }
        }
        receiver.accept(value);
    }

    public void setStatus(Boolean t) {
        if (!status && t) {
            running = true;
            status = true;
        } else if (status && !t) {
            if (running) {
                finishSwitch = true;
            } else {
                running = true;
                status = false;
            }
        }
    }
}
