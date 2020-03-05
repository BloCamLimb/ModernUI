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

public class DisposableSinAnimation implements IAnimation {

    protected boolean discard = false;

    protected Consumer<Float> receiver;

    protected float value;

    protected float initValue;

    protected float targetValue;

    protected float startTime = GlobalModuleManager.INSTANCE.getAnimationTime();

    protected float duration;

    public DisposableSinAnimation(float initValue, float targetValue, float duration, Consumer<Float> receiver) {
        this.value = this.initValue = initValue;
        this.targetValue = targetValue;
        this.duration = duration;
        this.receiver = receiver;
    }

    @Override
    public void update(float currentTime) {
        if (currentTime <= startTime) {
            return;
        }
        float d = currentTime - startTime;
        float p = d / duration;
        float sin = (float) Math.sin(p * Math.PI / 2);
        value = initValue + (targetValue - initValue) * sin;
        if (p >= 1) {
            value = targetValue;
            discard = true;
        }
        receiver.accept(value);
    }

    @Override
    public boolean shouldRemove() {
        return discard;
    }
}
