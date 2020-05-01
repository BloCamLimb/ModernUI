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
import icyllis.modernui.gui.math.DelayedTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AnimationControl {

    @Nullable
    private final List<Animation> openList;

    @Nullable
    private final List<Animation> closeList;

    // 0 = close, 1 = opening, 2 = open, 3 = closing
    private int openState = 0;

    private boolean lockState = false;

    private boolean prepareToOpen = false;

    private boolean prepareToClose = false;

    public AnimationControl(@Nullable List<Animation> openList, @Nullable List<Animation> closeList) {
        this.openList = openList;
        this.closeList = closeList;
        Optional<Animation> d;
        if (openList != null) {
            d = openList.stream().max(Comparator.comparing(Animation::getDuration));
            d.ifPresent(animation -> animation.addListener(new Animation.IAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation, boolean isReverse) {
                    setOpenState(true);
                }
            }));
        }
        if (closeList != null) {
            d = closeList.stream().max(Comparator.comparing(Animation::getDuration));
            d.ifPresent(animation -> animation.addListener(new Animation.IAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation, boolean isReverse) {
                    setOpenState(false);
                }
            }));
        }
    }

    public void update() {
        if (prepareToOpen && openState == 0) {
            openState = 1;
            if (openList != null) {
                openList.forEach(Animation::restart);
            }
            if (openList == null || openList.isEmpty()) {
                setOpenState(true);
            }
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            openState = 3;
            if (closeList != null) {
                closeList.forEach(Animation::restart);
            }
            if (closeList == null || closeList.isEmpty()) {
                setOpenState(false);
            }
            prepareToClose = false;
        }
    }

    public final void startOpenAnimation() {
        if (!lockState) {
            prepareToOpen = true;
            prepareToClose = false;
        }
    }

    public final void startCloseAnimation() {
        if (!lockState) {
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
