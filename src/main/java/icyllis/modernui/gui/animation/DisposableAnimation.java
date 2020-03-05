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

public class DisposableAnimation implements IAnimation {

    protected float value;

    protected Function<Integer, Float> fakeInitValue, fakeTargetValue;

    protected float initValue, targetValue, startTime = GlobalModuleManager.INSTANCE.getAnimationTime(), fixedTiming;

    protected boolean isVertical = false;

    protected boolean finish = false;

    protected Consumer<Float> receiver;

    protected Consumer<Function<Integer, Float>> relativeReceiver;

    public DisposableAnimation(float startTime, Consumer<Float> receiver, Consumer<Function<Integer, Float>> relativeReceiver) {
        this.receiver = receiver;
        this.relativeReceiver = relativeReceiver;
    }

    public void update(float currentTime) {
        if (currentTime <= startTime) {
            return;
        }
        updateUniform(currentTime);
        receiver.accept(value);
    }

    public void resize(int width, int height) {
        if (isVertical) {
            initValue = fakeInitValue.apply(height);
            targetValue = fakeTargetValue.apply(height);
        } else {
            initValue = fakeInitValue.apply(width);
            targetValue = fakeTargetValue.apply(width);
        }
    }

    public void buildResize(int width, int height) {
        resize(width, height);
        value = initValue;
    }

    protected void updateUniform(float currentTime) {
        float d = currentTime - startTime;
        value = initValue + (targetValue - initValue) * (d / fixedTiming);
        if (value >= targetValue) {
            value = targetValue;
            finish = true;
            relativeReceiver.accept(fakeTargetValue);
        }
    }

    protected void updateSine(float currentTime) {
        float d = currentTime - startTime;
        float p = d / fixedTiming;
        float sin = (float) Math.sin(p * Math.PI / 2f);
        value = initValue + (targetValue - initValue) * sin;
        if (p >= 1) {
            value = targetValue;
            finish = true;
            relativeReceiver.accept(fakeTargetValue);
        }
    }

    @Override
    public boolean shouldRemove() {
        return finish;
    }
}
