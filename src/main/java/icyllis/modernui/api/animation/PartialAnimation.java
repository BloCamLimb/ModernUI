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

package icyllis.modernui.api.animation;

import icyllis.modernui.gui.master.GlobalModuleManager;

/**
 * A single animation depend on current value
 */
public class PartialAnimation implements IAnimation {

    private final float duration;

    private final boolean useSine;

    private float startTime;

    private Applier applier;

    private boolean finish = false;

    public PartialAnimation(float duration) {
        this(duration, false);
    }

    public PartialAnimation(float duration, boolean useSine) {
        this.duration = duration;
        this.useSine = useSine;
        startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
    }

    public PartialAnimation applyTo(Applier applier, float currentValue) {
        this.applier = applier;
        float p = 1 - (applier.targetValue - currentValue) / (applier.targetValue - applier.initValue);
        if (useSine) {
            p = (float) (Math.asin(p) * 2 / Math.PI);
        }
        startTime = startTime - duration * p;
        return this;
    }

    @Override
    public void update(float currentTime) {
        float p = Math.min((currentTime - startTime) / duration, 1);
        if (useSine) {
            p = (float) Math.sin(p * Math.PI / 2);
        }
        applier.apply(p);
        if (p == 1) {
            finish = true;
        }
    }

    @Override
    public boolean shouldRemove() {
        return finish;
    }
}
