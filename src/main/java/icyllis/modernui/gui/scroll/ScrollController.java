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

package icyllis.modernui.gui.scroll;

import icyllis.modernui.gui.animation.IInterpolator;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

/**
 * Control scroll window to scroll smoothly
 */
public class ScrollController {

    @Nonnull
    private final IScrollHost master;

    private float startValue;

    private float value;

    private float targetValue;

    private float startTime;

    private float duration = 2f;

    public ScrollController(@Nonnull IScrollHost host) {
        this.master = host;
    }

    public void update(float currentTime) {
        if (value != targetValue) {
            float p = Math.min((currentTime - startTime) / duration, 1);
            p = IInterpolator.DECELERATE.getInterpolation(p);
            value = MathHelper.lerp(p, startValue, targetValue);
            master.callbackScrollAmount(value);
        }
    }

    private void setTargetValue(float newTargetValue) {
        startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
        startValue = value;
        targetValue = Math.round(newTargetValue);
        float dis = Math.abs(targetValue - value);
        if (dis > 60) {
            duration = dis / 30f;
        } else {
            duration = 3.0f;
        }
    }

    private void setTargetValueDirect(float newTargetValue) {
        startValue = value = targetValue = newTargetValue;
        master.callbackScrollAmount(value);
    }

    public void scrollSmooth(float delta) {
        float amount = MathHelper.clamp(targetValue + delta, 0, master.getMaxScrollAmount());
        setTargetValue(amount);
    }

    public void scrollDirect(float delta) {
        float amount = Math.round(MathHelper.clamp(targetValue + delta, 0, master.getMaxScrollAmount()) * 2.0f) / 2.0f;
        setTargetValueDirect(amount);
    }
}
