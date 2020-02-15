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

import java.util.function.Consumer;

/**
 * Returnable, Real-time
 */
public class TriggeredAnimation extends DisposableAnimation {

    private boolean running = false;
    private boolean status = false;

    public TriggeredAnimation(Consumer<Float> receiver) {
        super(Integer.MAX_VALUE, receiver);
    }

    @Override
    public void update(float currentTime) {
        if (running && startTime == Integer.MAX_VALUE) {
            calculateStartTime(currentTime);
        }
        super.update(currentTime);
        if (finish) {
            startTime = Integer.MAX_VALUE;
            running = false;
        }
    }

    private void calculateStartTime(float currentTime) {
        float p;
        if (status) {
            switch (motionType) {
                case UNIFORM:
                    p = 1 - (targetValue - value) / (targetValue - initValue);
                    startTime = currentTime - fixedTiming * p;
                    break;
                case SINE:
                    p = 1 - (targetValue - value) / (targetValue - initValue);
                    p = (float) (Math.asin(p) * 2 / Math.PI);
                    startTime = currentTime - fixedTiming * p;
                    break;
            }
        } else {
            switch (motionType) {
                case UNIFORM:
                    p = (targetValue - value) / (targetValue - initValue);
                    startTime = currentTime - fixedTiming * p;
                    break;
                case SINE:
                    p = (targetValue - value) / (targetValue - initValue);
                    p = (float) (Math.asin(p) * 2 / Math.PI);
                    startTime = currentTime - fixedTiming * p;
                    break;
            }
        }
    }

    public void setStatus(boolean b) {
        if (status != b) {
            running = true;
            status = b;
        }
    }

    @Override
    public boolean isFinish() {
        return false;
    }
}
