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

package icyllis.modernui.ui.animation;

import icyllis.modernui.ui.animation.Animation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Control animation in two states
 * Any state cannot be cancelled when it is in progress
 * This is not needed in most cases
 */
public class AnimationControl implements Animation.IListener {

    @Nullable
    private final List<Animation> openList;

    @Nullable
    private final List<Animation> closeList;

    // 0 = close, 1 = opening, 2 = open, 3 = closing
    private int openState = 0;

    private boolean lockState = false;

    private boolean prepareToOpen = false;

    private boolean prepareToClose = false;

    @Nullable
    private final Animation lastOpenAnimation;

    @Nullable
    private final Animation lastCloseAnimation;

    public AnimationControl(@Nullable List<Animation> openList, @Nullable List<Animation> closeList) {
        this.openList = openList;
        this.closeList = closeList;
        if (openList != null) {
            Optional<Animation> d = openList.stream().max(Comparator.comparing(Animation::getDuration));
            lastOpenAnimation = d.orElse(null);
        } else {
            lastOpenAnimation = null;
        }
        if (closeList != null) {
            Optional<Animation> d = closeList.stream().max(Comparator.comparing(Animation::getDuration));
            lastCloseAnimation = d.orElse(null);
        } else {
            lastCloseAnimation = null;
        }
        if (lastOpenAnimation != null) {
            lastOpenAnimation.listen(this);
        }
        if (lastCloseAnimation != null) {
            lastCloseAnimation.listen(this);
        }
    }

    @Override
    public void onAnimationEnd(@Nonnull Animation animation, boolean isReverse) {
        if (animation == lastOpenAnimation) {
            openState = 2;
        } else if (animation == lastCloseAnimation) {
            openState = 0;
        }
    }

    /**
     * Call this at the beginning of draw()
     */
    public void update() {
        if (prepareToOpen && openState == 0) {
            openState = 1;
            if (openList != null) {
                openList.forEach(Animation::startFull);
            }
            if (openList == null || openList.isEmpty()) {
                openState = 2;
            }
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            openState = 3;
            if (closeList != null) {
                closeList.forEach(Animation::startFull);
            }
            if (closeList == null || closeList.isEmpty()) {
                openState = 0;
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

    public final boolean isAnimationOpen() {
        return openState != 0;
    }

    public final void setLockState(boolean lock) {
        this.lockState = lock;
    }

    public final boolean isUnlockState() {
        return !lockState;
    }
}
