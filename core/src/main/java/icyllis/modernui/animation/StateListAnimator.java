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

package icyllis.modernui.animation;

import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Lets you define a number of Animators that will run on the attached View depending on the View's
 * drawable state.
 *
 * @since 3.3.1
 */
public class StateListAnimator implements Cloneable {

    private ArrayList<int[]> mSpecs = new ArrayList<>();
    private ArrayList<Animator> mAnimators = new ArrayList<>();
    private int mLastMatch = -1;
    private Animator mRunningAnimator;
    private WeakReference<View> mViewRef;
    private Animator.AnimatorListener mAnimatorListener;

    public StateListAnimator() {
        initAnimatorListener();
    }

    private void initAnimatorListener() {
        mAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(@Nonnull Animator animation, boolean isReverse) {
                animation.setTarget(null);
                if (mRunningAnimator == animation) {
                    mRunningAnimator = null;
                }
            }
        };
    }

    /**
     * Associates the given animator with the provided drawable state spec so that it will be run
     * when the View's drawable state matches the spec.
     *
     * @param spec     The drawable state spec to match against
     * @param animator The animator to run when the spec match
     */
    public void addState(@Nonnull int[] spec, @Nonnull Animator animator) {
        animator.addListener(mAnimatorListener);
        mSpecs.add(spec);
        mAnimators.add(animator);
    }

    @Nullable
    private View getTarget() {
        return mViewRef == null ? null : mViewRef.get();
    }

    /**
     * Called by View
     */
    @ApiStatus.Internal
    public void setTarget(@Nullable View view) {
        final View current = getTarget();
        if (current == view) {
            return;
        }
        if (current != null) {
            clearTarget();
        }
        if (view != null) {
            mViewRef = new WeakReference<>(view);
        }
    }

    private void clearTarget() {
        for (Animator animator : mAnimators) {
            animator.setTarget(null);
        }
        mViewRef = null;
        mLastMatch = -1;
        mRunningAnimator = null;
    }

    /**
     * Called by View
     */
    @ApiStatus.Internal
    public void setState(@Nonnull int[] state) {
        int match = -1;
        final int size = mSpecs.size();
        for (int i = 0; i < size; i++) {
            final int[] spec = mSpecs.get(i);
            if (StateSet.stateSetMatches(spec, state)) {
                match = i;
                break;
            }
        }
        if (match == mLastMatch) {
            return;
        }
        if (mLastMatch != -1) {
            cancel();
        }
        mLastMatch = match;
        if (match != -1) {
            start(mAnimators.get(match));
        }
    }

    private void start(@Nonnull Animator animator) {
        animator.setTarget(getTarget());
        mRunningAnimator = animator;
        mRunningAnimator.start();
    }

    private void cancel() {
        if (mRunningAnimator != null) {
            mRunningAnimator.cancel();
            mRunningAnimator = null;
        }
    }

    /**
     * If there is an animation running for a recent state change, ends it.
     * <p>
     * This causes the animation to assign the end value(s) to the View.
     */
    public void jumpToCurrentState() {
        if (mRunningAnimator != null) {
            mRunningAnimator.end();
        }
    }

    @Override
    public StateListAnimator clone() {
        try {
            StateListAnimator clone = (StateListAnimator) super.clone();
            final int size = mSpecs.size();
            clone.mSpecs = new ArrayList<>(size);
            clone.mAnimators = new ArrayList<>(size);
            clone.mLastMatch = -1;
            clone.mRunningAnimator = null;
            clone.mViewRef = null;
            clone.initAnimatorListener();
            for (int i = 0; i < size; i++) {
                final Animator animatorClone = mAnimators.get(i).clone();
                animatorClone.removeListener(mAnimatorListener);
                clone.addState(mSpecs.get(i), animatorClone);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
