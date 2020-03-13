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

package icyllis.modernui.gui.component.scroll;

import icyllis.modernui.gui.master.GlobalModuleManager;

import java.util.function.Consumer;

/**
 * Control scroll window to scroll smoothly
 */
public class ScrollController {

    private float startValue;

    private float value;

    private float targetValue;

    private float startTime;

    private float duration = 2f;

    private Consumer<Float> receiver;

    public ScrollController(Consumer<Float> receiver) {
        this.receiver = receiver;
    }

    public void update(float currentTime) {
        if (value != targetValue) {
            float p = Math.min((currentTime - startTime) / duration, 1);
            p = (float) Math.sin(p * Math.PI / 2);
            value = startValue + (targetValue - startValue) * p;
            receiver.accept(value);
        }
    }

    public void setTargetValue(float newTargetValue) {
        startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
        startValue = value;
        targetValue = newTargetValue;
        if (Math.abs(targetValue - value) > 60) {
            duration = 1.5f;
        } else {
            duration = 2.0f;
        }
    }

    public void setTargetValueDirect(float newTargetValue) {
        startValue = value = targetValue = newTargetValue;
    }

    public float getTargetValue() {
        return targetValue;
    }
}
