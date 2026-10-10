/*
 * ModernUI.
 * Copyright (C) 2021-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.animation;

/**
 * @deprecated use {@link icyllis.modernui.view.LayoutTransition} since 3.14
 */
@Deprecated
public class LayoutTransition {

    public static final int CHANGE_APPEARING = 0;

    public static final int CHANGE_DISAPPEARING = 1;

    public static final int APPEARING = 2;

    public static final int DISAPPEARING = 3;

    public static final int CHANGING = 4;

    private static final int FLAG_APPEARING = 0x01;
    private static final int FLAG_DISAPPEARING = 0x02;
    private static final int FLAG_CHANGE_APPEARING = 0x04;
    private static final int FLAG_CHANGE_DISAPPEARING = 0x08;
    private static final int FLAG_CHANGING = 0x10;

    private static final long DEFAULT_DURATION = 300;

    private static final TimeInterpolator sAppearingInterpolator = TimeInterpolator.ACCELERATE_DECELERATE;
    private static final TimeInterpolator sDisappearingInterpolator = TimeInterpolator.ACCELERATE_DECELERATE;
    private static final TimeInterpolator sChangingAppearingInterpolator = TimeInterpolator.DECELERATE;
    private static final TimeInterpolator sChangingDisappearingInterpolator = TimeInterpolator.DECELERATE;
    private static final TimeInterpolator sChangingInterpolator = TimeInterpolator.DECELERATE;

    private Animator mDisappearingAnim;
    private Animator mAppearingAnim;
    private Animator mChangingAppearingAnim;
    private Animator mChangingDisappearingAnim;
    private Animator mChangingAnim;

    private long mChangingAppearingDuration = DEFAULT_DURATION;
    private long mChangingDisappearingDuration = DEFAULT_DURATION;
    private long mChangingDuration = DEFAULT_DURATION;
    private long mAppearingDuration = DEFAULT_DURATION;
    private long mDisappearingDuration = DEFAULT_DURATION;

    private long mAppearingDelay = DEFAULT_DURATION;
    private long mDisappearingDelay = 0;
    private long mChangingAppearingDelay = 0;
    private long mChangingDisappearingDelay = DEFAULT_DURATION;
    private long mChangingDelay = 0;

    private long mChangingAppearingStagger = 0;
    private long mChangingDisappearingStagger = 0;
    private long mChangingStagger = 0;

    private TimeInterpolator mAppearingInterpolator = sAppearingInterpolator;
    private TimeInterpolator mDisappearingInterpolator = sDisappearingInterpolator;
    private TimeInterpolator mChangingAppearingInterpolator = sChangingAppearingInterpolator;
    private TimeInterpolator mChangingDisappearingInterpolator = sChangingDisappearingInterpolator;
    private TimeInterpolator mChangingInterpolator = sChangingInterpolator;

    private int mTransitionTypes = FLAG_CHANGE_APPEARING | FLAG_CHANGE_DISAPPEARING |
            FLAG_APPEARING | FLAG_DISAPPEARING;

    private boolean mAnimateParentHierarchy = true;

    public LayoutTransition() {
    }

    public void setDuration(long duration) {
        mChangingAppearingDuration = duration;
        mChangingDisappearingDuration = duration;
        mChangingDuration = duration;
        mAppearingDuration = duration;
        mDisappearingDuration = duration;
    }

    public void enableTransitionType(int transitionType) {
        switch (transitionType) {
            case APPEARING -> mTransitionTypes |= FLAG_APPEARING;
            case DISAPPEARING -> mTransitionTypes |= FLAG_DISAPPEARING;
            case CHANGE_APPEARING -> mTransitionTypes |= FLAG_CHANGE_APPEARING;
            case CHANGE_DISAPPEARING -> mTransitionTypes |= FLAG_CHANGE_DISAPPEARING;
            case CHANGING -> mTransitionTypes |= FLAG_CHANGING;
        }
    }

    public void disableTransitionType(int transitionType) {
        switch (transitionType) {
            case APPEARING -> mTransitionTypes &= ~FLAG_APPEARING;
            case DISAPPEARING -> mTransitionTypes &= ~FLAG_DISAPPEARING;
            case CHANGE_APPEARING -> mTransitionTypes &= ~FLAG_CHANGE_APPEARING;
            case CHANGE_DISAPPEARING -> mTransitionTypes &= ~FLAG_CHANGE_DISAPPEARING;
            case CHANGING -> mTransitionTypes &= ~FLAG_CHANGING;
        }
    }

    public boolean isTransitionTypeEnabled(int transitionType) {
        return switch (transitionType) {
            case APPEARING -> (mTransitionTypes & FLAG_APPEARING) == FLAG_APPEARING;
            case DISAPPEARING -> (mTransitionTypes & FLAG_DISAPPEARING) == FLAG_DISAPPEARING;
            case CHANGE_APPEARING -> (mTransitionTypes & FLAG_CHANGE_APPEARING) == FLAG_CHANGE_APPEARING;
            case CHANGE_DISAPPEARING -> (mTransitionTypes & FLAG_CHANGE_DISAPPEARING) == FLAG_CHANGE_DISAPPEARING;
            case CHANGING -> (mTransitionTypes & FLAG_CHANGING) == FLAG_CHANGING;
            default -> false;
        };
    }

    public void setStartDelay(int transitionType, long delay) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDelay = delay;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDelay = delay;
            case CHANGING -> mChangingDelay = delay;
            case APPEARING -> mAppearingDelay = delay;
            case DISAPPEARING -> mDisappearingDelay = delay;
        }
    }

    public long getStartDelay(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDelay;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDelay;
            case CHANGING -> mChangingDelay;
            case APPEARING -> mAppearingDelay;
            case DISAPPEARING -> mDisappearingDelay;
            default -> 0;
        };
    }

    public void setDuration(int transitionType, long duration) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDuration = duration;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDuration = duration;
            case CHANGING -> mChangingDuration = duration;
            case APPEARING -> mAppearingDuration = duration;
            case DISAPPEARING -> mDisappearingDuration = duration;
        }
    }

    public long getDuration(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDuration;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDuration;
            case CHANGING -> mChangingDuration;
            case APPEARING -> mAppearingDuration;
            case DISAPPEARING -> mDisappearingDuration;
            default -> 0;
        };
    }

    public void setStagger(int transitionType, long duration) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingStagger = duration;
            case CHANGE_DISAPPEARING -> mChangingDisappearingStagger = duration;
            case CHANGING -> mChangingStagger = duration;
            // noop other cases
        }
    }

    public long getStagger(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingStagger;
            case CHANGE_DISAPPEARING -> mChangingDisappearingStagger;
            case CHANGING -> mChangingStagger;
            default -> 0;
        };
    }

    public void setInterpolator(int transitionType, TimeInterpolator interpolator) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingInterpolator = interpolator;
            case CHANGE_DISAPPEARING -> mChangingDisappearingInterpolator = interpolator;
            case CHANGING -> mChangingInterpolator = interpolator;
            case APPEARING -> mAppearingInterpolator = interpolator;
            case DISAPPEARING -> mDisappearingInterpolator = interpolator;
        }
    }

    public TimeInterpolator getInterpolator(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingInterpolator;
            case CHANGE_DISAPPEARING -> mChangingDisappearingInterpolator;
            case CHANGING -> mChangingInterpolator;
            case APPEARING -> mAppearingInterpolator;
            case DISAPPEARING -> mDisappearingInterpolator;
            default -> null;
        };
    }

    public void setAnimator(int transitionType, Animator animator) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingAnim = animator;
            case CHANGE_DISAPPEARING -> mChangingDisappearingAnim = animator;
            case CHANGING -> mChangingAnim = animator;
            case APPEARING -> mAppearingAnim = animator;
            case DISAPPEARING -> mDisappearingAnim = animator;
        }
    }

    public Animator getAnimator(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingAnim;
            case CHANGE_DISAPPEARING -> mChangingDisappearingAnim;
            case CHANGING -> mChangingAnim;
            case APPEARING -> mAppearingAnim;
            case DISAPPEARING -> mDisappearingAnim;
            default -> null;
        };
    }

    public void setAnimateParentHierarchy(boolean animateParentHierarchy) {
        mAnimateParentHierarchy = animateParentHierarchy;
    }

    public boolean getAnimateParentHierarchy() {
        return mAnimateParentHierarchy;
    }
}
