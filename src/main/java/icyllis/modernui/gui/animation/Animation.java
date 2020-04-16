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

import com.google.common.collect.Lists;
import icyllis.modernui.gui.master.GlobalModuleManager;

import java.util.Arrays;
import java.util.List;

public class Animation implements IAnimation {

    private final float duration;

    private final boolean useSine;

    private float startTime;

    private List<Applier> appliers;

    private boolean finish = false;

    private Runnable finishRunnable = () -> {};

    /**
     * New animation use uniform motion
     * @param duration floating point ticks
     */
    public Animation(float duration) {
        this(duration, false);
    }

    public Animation(float duration, boolean useSine) {
        this.duration = duration;
        this.useSine = useSine;
        startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
    }

    public Animation applyTo(Applier... appliers) {
        this.appliers = Lists.newArrayList(appliers);
        return this;
    }

    public Animation withDelay(float delay) {
        startTime += delay;
        return this;
    }

    public Animation onFinish(Runnable runnable) {
        finishRunnable = runnable;
        if (appliers == null) {
            appliers = Lists.newArrayList();
        }
        return this;
    }

    @Override
    public void update(float time) {
        if (time <= startTime) {
            return;
        }
        float p = Math.min((time - startTime) / duration, 1);
        if (useSine) {
            p = (float) Math.sin(p * Math.PI / 2);
        }
        float progress = p;
        appliers.forEach(e -> e.apply(progress));
        if (progress == 1) {
            finish = true;
            finishRunnable.run();
        }
    }

    public float getDuration() {
        return duration;
    }

    @Override
    public boolean shouldRemove() {
        return finish;
    }
}
