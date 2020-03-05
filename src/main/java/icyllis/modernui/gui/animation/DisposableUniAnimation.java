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

public class DisposableUniAnimation implements IAnimation {

    protected float startTime;

    protected float value;

    protected float initValue;

    protected float targetValue;

    protected float duration;

    protected Consumer<Float> receiver;

    protected boolean discard = false;

    protected Runnable onFinish = () -> {};

    public DisposableUniAnimation(float initValue, float targetValue, float duration, Consumer<Float> receiver) {
        this.value = this.initValue = initValue;
        this.targetValue = targetValue;
        this.duration = duration;
        this.receiver = receiver;
        this.startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
    }

    public DisposableUniAnimation(float initValue, float currentValue, float targetValue, float duration, Consumer<Float> receiver) {
        this.initValue = initValue;
        this.value = currentValue;
        this.targetValue = targetValue;
        this.duration = duration;
        this.receiver = receiver;
        float p = 1 - (targetValue - currentValue) / (targetValue - initValue);
        this.startTime = GlobalModuleManager.INSTANCE.getAnimationTime() - duration * p;
    }

    public DisposableUniAnimation withDelay(float time) {
        startTime = startTime + time;
        return this;
    }

    public DisposableUniAnimation onFinish(Runnable r) {
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
            value = targetValue;
            receiver.accept(value);
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
