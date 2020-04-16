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

package icyllis.modernui.gui.master;

import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.math.DelayedRunnable;

import java.util.ArrayList;
import java.util.List;

public abstract class AnimationControl {

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    // 0 = close, 1 = opening, 2 = open, 3 = closing
    private int openState = 0;

    private boolean lockState = false;

    private boolean prepareToOpen = false;

    private boolean prepareToClose = false;

    public AnimationControl() {

    }

    public void update() {
        if (prepareToOpen && openState == 0) {
            openState = 1;
            List<Animation> list = new ArrayList<>();
            createOpenAnimations(list);
            float d = 0;
            for (Animation animation : list) {
                d = Math.max(d, animation.getDuration());
            }
            if (d > 0) {
                list.forEach(manager::addAnimation);
                manager.scheduleRunnable(new DelayedRunnable(() -> setOpenState(true), (int) d));
            } else {
                setOpenState(true);
            }
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            openState = 3;
            List<Animation> list = new ArrayList<>();
            createCloseAnimations(list);
            float d = 0;
            for (Animation animation : list) {
                d = Math.max(d, animation.getDuration());
            }
            if (d > 0) {
                list.forEach(manager::addAnimation);
                manager.scheduleRunnable(new DelayedRunnable(() -> setOpenState(false), (int) d));
            } else {
                setOpenState(false);
            }
            prepareToClose = false;
        }
    }

    /**
     * Create open animations and will be auto set open state to 2 on last animation finished
     *
     * @param list a list of animations
     */
    protected abstract void createOpenAnimations(List<Animation> list);

    /**
     * Create close animations and will be auto set open state to 0 on last animation finished
     *
     * @param list a list of animations
     */
    protected abstract void createCloseAnimations(List<Animation> list);

    public final void startOpenAnimation() {
        if (!lockState && openState != 2) {
            prepareToOpen = true;
            prepareToClose = false;
        }
    }

    public final void startCloseAnimation() {
        if (!lockState && openState != 0) {
            prepareToClose = true;
            prepareToOpen = false;
        }
    }

    private void setOpenState(boolean fullOpen) {
        this.openState = fullOpen ? 2 : 0;
    }

    public final boolean isAnimationOpen() {
        return openState != 0;
    }

    public final void setLockState(boolean lock) {
        this.lockState = lock;
    }

    public final boolean canChangeState() {
        return !lockState;
    }
}
