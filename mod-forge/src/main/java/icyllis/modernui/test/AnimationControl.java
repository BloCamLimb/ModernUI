/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test;

import icyllis.modernui.animation.AnimatorSet;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.test.Animation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Control animation in two states, any state cannot be cancelled when it is in progress.
 *
 * @deprecated use {@link ObjectAnimator} and {@link AnimatorSet}
 */
@Deprecated
public class AnimationControl implements Animation.IListener {

    @Nullable
    private final Animation[] openArray;

    @Nullable
    private final Animation[] closeArray;

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
        if (openList != null) {
            openArray = openList.toArray(new Animation[0]);
        } else {
            openArray = null;
        }
        if (closeList != null) {
            closeArray = closeList.toArray(new Animation[0]);
        } else {
            closeArray = null;
        }
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
     * Call this at the beginning of onDraw()
     */
    public void update() {
        if (prepareToOpen && openState == 0) {
            openState = 1;
            if (openArray != null) {
                for (Animation a : openArray) {
                    a.startFull();
                }
            }
            if (openArray == null || openArray.length == 0) {
                openState = 2;
            }
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            openState = 3;
            if (closeArray != null) {
                for (Animation a : closeArray) {
                    a.startFull();
                }
            }
            if (closeArray == null || closeArray.length == 0) {
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
