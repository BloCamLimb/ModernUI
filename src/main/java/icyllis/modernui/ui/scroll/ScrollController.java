/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.scroll;

import icyllis.modernui.ui.animation.IInterpolator;
import icyllis.modernui.ui.master.UIManager;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

/**
 * Control scroll view to scroll smoothly
 *
 * @since 1.6 reworked
 */
public class ScrollController {

    @Nonnull
    private final ScrollView view;

    private float startValue;

    private float value;

    private float targetValue;

    private float startTime;

    private float duration = 2.0f;

    public ScrollController(@Nonnull ScrollView view) {
        this.view = view;
    }

    public void update(float currentTime) {
        if (value != targetValue) {
            float p = Math.min((currentTime - startTime) / duration, 1);
            p = IInterpolator.SINE.getInterpolation(p);
            value = MathHelper.lerp(p, startValue, targetValue);
            view.updateScrollAmount(value);
        }
    }

    private void setTargetValue(float newTargetValue) {
        startTime = UIManager.INSTANCE.getDrawingTime();
        startValue = value;
        targetValue = Math.round(newTargetValue);
        float dis = Math.abs(targetValue - value);
        if (dis > 60) {
            duration = dis / 30.0f;
        } else {
            duration = 3.0f;
        }
    }

    private void setTargetValueDirect(float newTargetValue) {
        startValue = value = targetValue = newTargetValue;
        view.updateScrollAmount(value);
    }

    public void scrollSmoothBy(float delta) {
        float amount = MathHelper.clamp(targetValue + delta, 0, view.getMaxScrollAmount());
        setTargetValue(amount);
    }

    public void scrollDirectBy(float delta) {
        // based on a gui scale of 2
        float amount = Math.round(MathHelper.clamp(targetValue + delta, 0, view.getMaxScrollAmount()) * 2.0f) / 2.0f;
        setTargetValueDirect(amount);
    }
}
