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

package icyllis.modernui.fragment;

import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.core.CancellationSignal;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * A SpecialEffectsController that hooks into the existing Fragment APIs to run
 * animations and transitions.
 */
class DefaultSpecialEffectsController extends SpecialEffectsController {

    DefaultSpecialEffectsController(@Nonnull ViewGroup container) {
        super(container);
    }

    @Override
    void executeOperations(@Nonnull List<Operation> operations, boolean isPop) {
        // Shared element transitions are done between the first fragment leaving and
        // the last fragment coming in. Finding these operations is the first priority
        Operation firstOut = null;
        Operation lastIn = null;
        for (final Operation operation : operations) {
            Operation.State currentState = Operation.State.from(operation.getFragment().mView);
            switch (operation.getFinalState()) {
                case GONE:
                case INVISIBLE:
                case REMOVED:
                    if (currentState == Operation.State.VISIBLE && firstOut == null) {
                        // The firstOut Operation is the first Operation moving from VISIBLE
                        firstOut = operation;
                    }
                    break;
                case VISIBLE:
                    if (currentState != Operation.State.VISIBLE) {
                        // The last Operation that moves to VISIBLE is the lastIn Operation
                        lastIn = operation;
                    }
                    break;
            }
        }
        if (FragmentManager.TRACE) {
            LOGGER.info(FragmentManager.MARKER, "Executing operations from " + firstOut + " to " + lastIn);
        }

        // Now iterate through the operations, collecting the set of animations
        // and transitions that need to be executed
        List<AnimationInfo> animations = new ArrayList<>();
        List<TransitionInfo> transitions = new ArrayList<>();
        final List<Operation> awaitingContainerChanges = new ArrayList<>(operations);

        for (final Operation operation : operations) {
            // Create the animation CancellationSignal
            CancellationSignal animCancellationSignal = new CancellationSignal();
            operation.markStartedSpecialEffect(animCancellationSignal);
            // Add the animation special effect
            animations.add(new AnimationInfo(operation, animCancellationSignal, isPop));

            // Create the transition CancellationSignal
            CancellationSignal transitionCancellationSignal = new CancellationSignal();
            operation.markStartedSpecialEffect(transitionCancellationSignal);
            // Add the transition special effect
            transitions.add(new TransitionInfo(operation, transitionCancellationSignal, isPop,
                    isPop ? operation == firstOut : operation == lastIn));

            // Ensure that if the Operation is synchronously complete, we still
            // apply the container changes before the Operation completes
            operation.addCompletionListener(() -> {
                if (awaitingContainerChanges.contains(operation)) {
                    awaitingContainerChanges.remove(operation);
                    applyContainerChanges(operation);
                }
            });
        }

        // Start transition special effects
        Object2BooleanMap<Operation> startedTransitions = startTransitions(transitions,
                awaitingContainerChanges, isPop, firstOut, lastIn);

        // Start animation special effects
        startAnimations(animations, awaitingContainerChanges, startedTransitions);

        for (final Operation operation : awaitingContainerChanges) {
            applyContainerChanges(operation);
        }
        awaitingContainerChanges.clear();
        if (FragmentManager.TRACE) {
            LOGGER.info(FragmentManager.MARKER,
                    "Completed executing operations from " + firstOut + " to " + lastIn);
        }
    }

    private void startAnimations(@Nonnull List<AnimationInfo> animationInfos,
                                 @Nonnull List<Operation> awaitingContainerChanges,
                                 @Nonnull Object2BooleanMap<Operation> startedTransitions) {
        final ViewGroup container = getContainer();

        for (final AnimationInfo animationInfo : animationInfos) {
            if (animationInfo.isVisibilityUnchanged()) {
                // No change in visibility, so we can immediately complete the animation
                animationInfo.completeSpecialEffect();
                continue;
            }
            final Animator animator = animationInfo.getAnimator();
            if (animator == null) {
                // No Animator or Animation, so we can immediately complete the animation
                animationInfo.completeSpecialEffect();
                continue;
            }

            // First make sure we haven't already started a Transition for this Operation
            final Operation operation = animationInfo.getOperation();
            final Fragment fragment = operation.getFragment();
            boolean startedTransition = startedTransitions.getBoolean(operation);
            if (startedTransition) {
                if (FragmentManager.TRACE) {
                    LOGGER.info(FragmentManager.MARKER, "Ignoring Animator set on "
                            + fragment + " as this Fragment was involved in a Transition.");
                }
                animationInfo.completeSpecialEffect();
                continue;
            }

            final boolean isHideOperation = operation.getFinalState() == Operation.State.GONE;
            if (isHideOperation) {
                // We don't want to immediately applyState() to hide operations as that
                // immediately stops the Animator. Instead we'll applyState() manually
                // when the Animator ends.
                awaitingContainerChanges.remove(operation);
            }
            final View viewToAnimate = fragment.mView;
            container.startViewTransition(viewToAnimate);
            animator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationEnd(@Nonnull Animator anim) {
                    container.endViewTransition(viewToAnimate);
                    if (isHideOperation) {
                        // Specifically for hide operations with Animator, we can't
                        // applyState until the Animator finishes
                        operation.getFinalState().applyState(viewToAnimate);
                    }
                    animationInfo.completeSpecialEffect();
                    if (FragmentManager.TRACE) {
                        LOGGER.info(FragmentManager.MARKER, "Animator from operation " + operation + " has "
                                + "ended.");
                    }
                }
            });
            animator.setTarget(viewToAnimate);
            animator.start();
            if (FragmentManager.TRACE) {
                LOGGER.info(FragmentManager.MARKER, "Animator from operation " + operation + " has "
                        + "started.");
            }
            // Listen for cancellation and use that to cancel the Animator
            CancellationSignal signal = animationInfo.getSignal();
            signal.setOnCancelListener(() -> {
                animator.end();
                if (FragmentManager.TRACE) {
                    LOGGER.info(FragmentManager.MARKER, "Animator from operation " + operation + " has "
                            + "been canceled.");
                }
            });
        }
    }

    @Nonnull
    private Object2BooleanMap<Operation> startTransitions(
            @Nonnull List<TransitionInfo> transitionInfos, @Nonnull List<Operation> awaitingContainerChanges,
            final boolean isPop, @Nullable final Operation firstOut, @Nullable final Operation lastIn) {
        Object2BooleanMap<Operation> startedTransitions = new Object2BooleanOpenHashMap<>();

        // There were no transitions at all, so we can just complete all of them
        for (TransitionInfo transitionInfo : transitionInfos) {
            startedTransitions.put(transitionInfo.getOperation(), false);
            transitionInfo.completeSpecialEffect();
        }
        return startedTransitions;
    }

    static void applyContainerChanges(@Nonnull Operation operation) {
        View view = operation.getFragment().mView;
        operation.getFinalState().applyState(view);
    }

    private static class SpecialEffectsInfo {

        @Nonnull
        private final Operation mOperation;
        @Nonnull
        private final CancellationSignal mSignal;

        SpecialEffectsInfo(@Nonnull Operation operation, @Nonnull CancellationSignal signal) {
            mOperation = operation;
            mSignal = signal;
        }

        @Nonnull
        Operation getOperation() {
            return mOperation;
        }

        @Nonnull
        CancellationSignal getSignal() {
            return mSignal;
        }

        boolean isVisibilityUnchanged() {
            Operation.State currentState = Operation.State.from(
                    mOperation.getFragment().mView);
            Operation.State finalState = mOperation.getFinalState();
            return currentState == finalState || (currentState != Operation.State.VISIBLE
                    && finalState != Operation.State.VISIBLE);
        }

        void completeSpecialEffect() {
            mOperation.completeSpecialEffect(mSignal);
        }
    }

    private static class AnimationInfo extends SpecialEffectsInfo {

        private static final Animator
                fragment_open_enter,
                fragment_open_exit,
                fragment_close_enter,
                fragment_close_exit,
                fragment_fade_enter,
                fragment_fade_exit;

        static {
            fragment_open_enter = ObjectAnimator.ofFloat(null, View.ALPHA, 0, 1);
            fragment_open_enter.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
            fragment_open_enter.setDuration(300);

            fragment_open_exit = ObjectAnimator.ofFloat(null, View.ALPHA, 1, 0);
            fragment_open_exit.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
            fragment_open_exit.setDuration(300);

            fragment_close_enter = fragment_open_enter;
            fragment_close_exit = fragment_open_exit;

            fragment_fade_enter = ObjectAnimator.ofFloat(null, View.ALPHA, 0, 1);
            fragment_fade_enter.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
            fragment_fade_enter.setDuration(220);

            fragment_fade_exit = ObjectAnimator.ofFloat(null, View.ALPHA, 1, 0);
            fragment_fade_exit.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
            fragment_fade_exit.setDuration(150);
        }

        private final boolean mIsPop;
        private boolean mLoadedAnim = false;
        @Nullable
        private Animator mAnimator;

        AnimationInfo(@Nonnull Operation operation, @Nonnull CancellationSignal signal,
                      boolean isPop) {
            super(operation, signal);
            mIsPop = isPop;
        }

        @Nullable
        Animator getAnimator() {
            if (mLoadedAnim) {
                return mAnimator;
            }
            mAnimator = loadAnimator(getOperation().getFragment(),
                    getOperation().getFinalState() == Operation.State.VISIBLE, mIsPop);
            mLoadedAnim = true;
            return mAnimator;
        }

        @Nullable
        private static Animator loadAnimator(@Nonnull Fragment fragment, boolean enter, boolean isPop) {
            int transit = fragment.getNextTransition();
            int nextAnim = getNextAnim(fragment, enter, isPop);
            // Clear the Fragment animations
            fragment.setAnimations(0, 0, 0, 0);
            // We do not need to keep up with the removing Fragment after we get its next animation.
            // If transactions do not allow reordering, this will always be true and the visible
            // removing fragment will be cleared. If reordering is allowed, this will only be true
            // after all records in a transaction have been executed and the visible removing
            // fragment has the correct animation, so it is time to clear it.
            if (fragment.mContainer != null
                    && fragment.mContainer.getTag(FragmentManager.visible_removing_fragment_view_tag) != null) {
                fragment.mContainer.setTag(FragmentManager.visible_removing_fragment_view_tag, null);
            }
            // If there is a transition on the container, clear those set on the fragment
            if (fragment.mContainer != null && fragment.mContainer.getLayoutTransition() != null) {
                return null;
            }

            Animator animator = fragment.onCreateAnimator(transit, enter, nextAnim);
            if (animator != null) {
                return animator;
            }

            if (nextAnim == 0 && transit != 0) {
                switch (transit) {
                    case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                        return enter ? fragment_open_enter.clone() : fragment_open_exit.clone();
                    case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                        return enter ? fragment_close_enter.clone() : fragment_close_exit.clone();
                    case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                        return enter ? fragment_fade_enter.clone() : fragment_fade_exit.clone();
                    case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN:
                        /*animAttr = enter
                                ? toActivityTransitResId(context, android.R.attr.activityOpenEnterAnimation)
                                : toActivityTransitResId(context, android.R.attr.activityOpenExitAnimation);*/
                        break;
                    case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE:
                        /*animAttr = enter
                                ? toActivityTransitResId(context,
                                android.R.attr.activityCloseEnterAnimation)
                                : toActivityTransitResId(context,
                                android.R.attr.activityCloseExitAnimation);*/
                        break;
                }
            }

            return null;
        }

        private static int getNextAnim(Fragment fragment, boolean enter, boolean isPop) {
            if (isPop) {
                if (enter) {
                    return fragment.getPopEnterAnim();
                } else {
                    return fragment.getPopExitAnim();
                }
            } else {
                if (enter) {
                    return fragment.getEnterAnim();
                } else {
                    return fragment.getExitAnim();
                }
            }
        }
    }

    //TODO transition framework
    private static class TransitionInfo extends SpecialEffectsInfo {

        TransitionInfo(@Nonnull Operation operation, @Nonnull CancellationSignal signal,
                       boolean isPop, boolean providesSharedElementTransition) {
            super(operation, signal);
        }
    }
}
