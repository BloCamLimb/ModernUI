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

import icyllis.modern.api.animation.IAlphaAnimation;

import java.util.function.Supplier;

public class AlphaAnimation implements IAnimation, IAlphaAnimation, Supplier<Float> {

    /** final **/
    private final float targetAlpha;

    /** setting **/
    private float startTime = 0, startAlpha, fixedTiming;

    /** calculation **/
    private float alpha;

    private boolean pre = false, finish = false;

    public AlphaAnimation(float targetAlpha) {
        this.targetAlpha = targetAlpha;
    }

    @Override
    public IAlphaAnimation translate(float t) {
        startAlpha = targetAlpha + t;
        return this;
    }

    @Override
    public IAlphaAnimation delay(float t) {
        startTime += t;
        pre = true;
        return this;
    }

    @Override
    public IAlphaAnimation fixedTiming(float t) {
        fixedTiming = t;
        return this;
    }

    @Override
    public void update(float currentTime) {
        if(pre) {
            if(currentTime >= startTime) {
                pre = false;
            } else {
                return;
            }
        }
        float d = currentTime - startTime;
        alpha = startAlpha + (targetAlpha - startAlpha) * (d / fixedTiming);
        if(alpha >= targetAlpha) {
            alpha = targetAlpha;
            finish = true;
        }
    }

    @Override
    public boolean isFinish() {
        return finish;
    }

    @Override
    public Float get() {
        return alpha;
    }
}
