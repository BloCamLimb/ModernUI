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

import icyllis.modernui.R;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.core.CancellationSignal;
import icyllis.modernui.fragment.SpecialEffectsController.Operation.State;
import icyllis.modernui.math.Rect;
import icyllis.modernui.transition.Transition;
import icyllis.modernui.transition.TransitionManager;
import icyllis.modernui.transition.TransitionSet;
import icyllis.modernui.util.ArrayMap;
import icyllis.modernui.view.OneShotPreDrawListener;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        final Object2BooleanMap<Operation> startedTransitions = new Object2BooleanOpenHashMap<>();
        // First verify that we can run all transitions together
        boolean hasTransition = false;
        for (TransitionInfo transitionInfo : transitionInfos) {
            if (transitionInfo.isVisibilityUnchanged()) {
                // No change in visibility, so we can skip this TransitionInfo
                continue;
            }
            if (transitionInfo.getTransition() != null ||
                    transitionInfo.getSharedElementTransition() != null) {
                hasTransition = true;
                break;
            }
        }
        if (!hasTransition) {
            // There were no transitions at all, so we can just complete all of them
            for (TransitionInfo transitionInfo : transitionInfos) {
                startedTransitions.put(transitionInfo.getOperation(), false);
                transitionInfo.completeSpecialEffect();
            }
            return startedTransitions;
        }

        // Every transition needs to target at least one View so that they
        // don't interfere with one another. This is the view we use
        // in cases where there are no real views to target
        final View nonExistentView = new View();

        // Now find the shared element transition if it exists
        TransitionSet sharedElementTransition = null;
        View firstOutEpicenterView = null;
        boolean hasLastInEpicenter = false;
        final Rect lastInEpicenterRect = new Rect();
        ArrayList<View> sharedElementFirstOutViews = new ArrayList<>();
        ArrayList<View> sharedElementLastInViews = new ArrayList<>();
        ArrayMap<String, String> sharedElementNameMapping = new ArrayMap<>();
        for (final TransitionInfo transitionInfo : transitionInfos) {
            Transition transition = transitionInfo.getSharedElementTransition();
            // Compute the shared element transition between the firstOut and lastIn Fragments
            if (transition != null && firstOut != null && lastIn != null) {
                // swapSharedElementTargets requires wrapping this in a TransitionSet
                TransitionSet transitionSet = new TransitionSet();
                transitionSet.addTransition(transition.clone());
                sharedElementTransition = transitionSet;
                // The exiting shared elements default to the source names from the
                // last in fragment
                ArrayList<String> exitingNames = lastIn.getFragment()
                        .getSharedElementSourceNames();
                // But if we're doing multiple transactions, we may need to re-map
                // the names from the first out fragment
                ArrayList<String> firstOutSourceNames = firstOut.getFragment()
                        .getSharedElementSourceNames();
                ArrayList<String> firstOutTargetNames = firstOut.getFragment()
                        .getSharedElementTargetNames();
                // We do this by iterating through each first out target,
                // seeing if there is a match from the last in sources
                for (int index = 0; index < firstOutTargetNames.size(); index++) {
                    int nameIndex = exitingNames.indexOf(firstOutTargetNames.get(index));
                    if (nameIndex != -1) {
                        // If we found a match, replace the last in source name
                        // with the first out source name
                        exitingNames.set(nameIndex, firstOutSourceNames.get(index));
                    }
                }
                ArrayList<String> enteringNames = lastIn.getFragment()
                        .getSharedElementTargetNames();
                SharedElementCallback exitingCallback;
                SharedElementCallback enteringCallback;
                if (!isPop) {
                    // Forward transitions have firstOut fragment exiting and the
                    // lastIn fragment entering
                    exitingCallback = firstOut.getFragment().getExitTransitionCallback();
                    enteringCallback = lastIn.getFragment().getEnterTransitionCallback();
                } else {
                    // A pop is the reverse: the firstOut fragment is entering and the
                    // lastIn fragment is exiting
                    exitingCallback = firstOut.getFragment().getEnterTransitionCallback();
                    enteringCallback = lastIn.getFragment().getExitTransitionCallback();
                }
                int numSharedElements = exitingNames.size();
                for (int i = 0; i < numSharedElements; i++) {
                    String exitingName = exitingNames.get(i);
                    String enteringName = enteringNames.get(i);
                    sharedElementNameMapping.put(exitingName, enteringName);
                }

                if (FragmentManager.TRACE) {
                    LOGGER.info(FragmentManager.MARKER, ">>> entering view names <<<");
                    for (String name : enteringNames) {
                        LOGGER.info(FragmentManager.MARKER, "Name: " + name);
                    }
                    LOGGER.info(FragmentManager.MARKER, ">>> exiting view names <<<");
                    for (String name : exitingNames) {
                        LOGGER.info(FragmentManager.MARKER, "Name: " + name);
                    }
                }

                // Find all of the Views from the firstOut fragment that are
                // part of the shared element transition
                final ArrayMap<String, View> firstOutViews = new ArrayMap<>();
                findNamedViews(firstOutViews, firstOut.getFragment().mView);
                firstOutViews.retainAll(exitingNames);
                if (exitingCallback != null) {
                    if (FragmentManager.TRACE) {
                        LOGGER.info(FragmentManager.MARKER,
                                "Executing exit callback for operation " + firstOut);
                    }
                    // Give the SharedElementCallback a chance to override the default mapping
                    exitingCallback.onMapSharedElements(exitingNames, firstOutViews);
                    for (int i = exitingNames.size() - 1; i >= 0; i--) {
                        String name = exitingNames.get(i);
                        View view = firstOutViews.get(name);
                        if (view == null) {
                            sharedElementNameMapping.remove(name);
                        } else if (!name.equals(view.getTransitionName())) {
                            String targetValue = sharedElementNameMapping.remove(name);
                            sharedElementNameMapping.put(view.getTransitionName(),
                                    targetValue);
                        }
                    }
                } else {
                    // Only keep the mapping of elements that were found in the firstOut Fragment
                    sharedElementNameMapping.retainAll(firstOutViews.keySet());
                }

                // Find all of the Views from the lastIn fragment that are
                // part of the shared element transition
                final ArrayMap<String, View> lastInViews = new ArrayMap<>();
                findNamedViews(lastInViews, lastIn.getFragment().mView);
                lastInViews.retainAll(enteringNames);
                lastInViews.retainAll(sharedElementNameMapping.values());
                if (enteringCallback != null) {
                    if (FragmentManager.TRACE) {
                        LOGGER.info(FragmentManager.MARKER,
                                "Executing enter callback for operation " + lastIn);
                    }
                    // Give the SharedElementCallback a chance to override the default mapping
                    enteringCallback.onMapSharedElements(enteringNames, lastInViews);
                    for (int i = enteringNames.size() - 1; i >= 0; i--) {
                        String name = enteringNames.get(i);
                        View view = lastInViews.get(name);
                        if (view == null) {
                            String key = FragmentTransition.findKeyForValue(
                                    sharedElementNameMapping, name);
                            if (key != null) {
                                sharedElementNameMapping.remove(key);
                            }
                        } else if (!name.equals(view.getTransitionName())) {
                            String key = FragmentTransition.findKeyForValue(
                                    sharedElementNameMapping, name);
                            if (key != null) {
                                sharedElementNameMapping.put(key,
                                        view.getTransitionName());
                            }
                        }
                    }
                } else {
                    // Only keep the mapping of elements that were found in the lastIn Fragment
                    FragmentTransition.retainValues(sharedElementNameMapping, lastInViews);
                }

                // Now make a final pass through the Views list to ensure they
                // don't still have elements that were removed from the mapping
                retainMatchingViews(firstOutViews, sharedElementNameMapping.keySet());
                retainMatchingViews(lastInViews, sharedElementNameMapping.values());

                // Now make a final pass through the Views list to ensure they
                // don't still have elements that were removed from the mapping
                retainMatchingViews(firstOutViews, sharedElementNameMapping.keySet());
                retainMatchingViews(lastInViews, sharedElementNameMapping.values());

                if (sharedElementNameMapping.isEmpty()) {
                    // We couldn't find any valid shared element mappings, so clear out
                    // the shared element transition information entirely
                    sharedElementTransition = null;
                    sharedElementFirstOutViews.clear();
                    sharedElementLastInViews.clear();
                } else {
                    // Call through to onSharedElementStart() before capturing the
                    // starting values for the shared element transition
                    FragmentTransition.callSharedElementStartEnd(
                            lastIn.getFragment(), firstOut.getFragment(), isPop,
                            firstOutViews, true);
                    // Trigger the onSharedElementEnd callback in the next frame after
                    // the starting values are captured and before capturing the end states
                    OneShotPreDrawListener.add(getContainer(), () -> FragmentTransition.callSharedElementStartEnd(
                            lastIn.getFragment(), firstOut.getFragment(), isPop,
                            lastInViews, false));

                    sharedElementFirstOutViews.addAll(firstOutViews.values());

                    // Compute the epicenter of the firstOut transition
                    if (!exitingNames.isEmpty()) {
                        String epicenterViewName = exitingNames.get(0);
                        firstOutEpicenterView = firstOutViews.get(epicenterViewName);
                        FragmentTransition.setEpicenter(sharedElementTransition,
                                firstOutEpicenterView);
                    }

                    sharedElementLastInViews.addAll(lastInViews.values());

                    // Compute the epicenter of the lastIn transition
                    if (!enteringNames.isEmpty()) {
                        String epicenterViewName = enteringNames.get(0);
                        final View lastInEpicenterView = lastInViews.get(epicenterViewName);
                        if (lastInEpicenterView != null) {
                            hasLastInEpicenter = true;
                            // We can't set the epicenter here directly since the View might
                            // not have been laid out as of yet, so instead we set a Rect as
                            // the epicenter and compute the bounds one frame later
                            OneShotPreDrawListener.add(getContainer(),
                                    () -> lastInEpicenterView.getBoundsOnScreen(lastInEpicenterRect));
                        }
                    }

                    // Now set the transition's targets to only the firstOut Fragment's views
                    // It'll be swapped to the lastIn Fragment's views after the
                    // transition is started
                    FragmentTransition.setSharedElementTargets(sharedElementTransition,
                            nonExistentView, sharedElementFirstOutViews);
                    // After the swap to the lastIn Fragment's view (done below), we
                    // need to clean up those targets. We schedule this here so that it
                    // runs directly after the swap
                    FragmentTransition.scheduleRemoveTargets(sharedElementTransition,
                            null, null, null, null,
                            sharedElementTransition, sharedElementLastInViews);
                    // Both the firstOut and lastIn Operations are now associated
                    // with a Transition
                    startedTransitions.put(firstOut, true);
                    startedTransitions.put(lastIn, true);
                }
            }
        }
        ArrayList<View> enteringViews = new ArrayList<>();
        // These transitions run together, overlapping one another
        Transition mergedTransition = null;
        // These transitions run only after all of the other transitions complete
        Transition mergedNonOverlappingTransition = null;
        // Now iterate through the set of transitions and merge them together
        for (final TransitionInfo transitionInfo : transitionInfos) {
            if (transitionInfo.isVisibilityUnchanged()) {
                // No change in visibility, so we can immediately complete the transition
                startedTransitions.put(transitionInfo.getOperation(), false);
                transitionInfo.completeSpecialEffect();
                continue;
            }
            Transition transition = transitionInfo.getTransition();
            if (transition != null) {
                transition = transition.clone();
            }
            Operation operation = transitionInfo.getOperation();
            boolean involvedInSharedElementTransition = sharedElementTransition != null
                    && (operation == firstOut || operation == lastIn);
            if (transition == null) {
                // Nothing more to do if the transition is null
                if (!involvedInSharedElementTransition) {
                    // Only complete the transition if this fragment isn't involved
                    // in the shared element transition (as otherwise we need to wait
                    // for that to finish)
                    startedTransitions.put(operation, false);
                    transitionInfo.completeSpecialEffect();
                }
            } else {
                // Target the Transition to *only* the set of transitioning views
                final ArrayList<View> transitioningViews = new ArrayList<>();
                captureTransitioningViews(transitioningViews,
                        operation.getFragment().mView);
                if (involvedInSharedElementTransition) {
                    // Remove all of the shared element views from the transition
                    if (operation == firstOut) {
                        transitioningViews.removeAll(sharedElementFirstOutViews);
                    } else {
                        transitioningViews.removeAll(sharedElementLastInViews);
                    }
                }
                if (transitioningViews.isEmpty()) {
                    transition.addTarget(nonExistentView);
                } else {
                    FragmentTransition.addTargets(transition, transitioningViews);
                    FragmentTransition.scheduleRemoveTargets(transition,
                            transition, transitioningViews,
                            null, null, null, null);
                    if (operation.getFinalState() == State.GONE) {
                        // We're hiding the Fragment. This requires a bit of extra work
                        // First, we need to avoid immediately applying the container change as
                        // that will stop the Transition from occurring.
                        awaitingContainerChanges.remove(operation);
                        // Then schedule the actual hide of the fragment's view,
                        // essentially doing what applyState() would do for us
                        ArrayList<View> transitioningViewsToHide =
                                new ArrayList<>(transitioningViews);
                        transitioningViewsToHide.remove(operation.getFragment().mView);
                        FragmentTransition.scheduleHideFragmentView(transition,
                                operation.getFragment().mView, transitioningViewsToHide);
                        // This OneShotPreDrawListener gets fired before the delayed start of
                        // the Transition and changes the visibility of any exiting child views
                        // that *ARE NOT* shared element transitions. The TransitionManager then
                        // properly considers exiting views and marks them as disappearing,
                        // applying a transition and a listener to take proper actions once the
                        // transition is complete.
                        OneShotPreDrawListener.add(getContainer(),
                                () -> FragmentTransition.setViewVisibility(transitioningViews, View.INVISIBLE));
                    }
                }
                if (operation.getFinalState() == State.VISIBLE) {
                    enteringViews.addAll(transitioningViews);
                    if (hasLastInEpicenter) {
                        FragmentTransition.setEpicenter(transition, lastInEpicenterRect);
                    }
                } else {
                    FragmentTransition.setEpicenter(transition, firstOutEpicenterView);
                }
                startedTransitions.put(operation, true);
                // Now determine how this transition should be merged together
                if (transitionInfo.isOverlapAllowed()) {
                    // Overlap is allowed, so add them to the mergeTransition set
                    mergedTransition = FragmentTransition.mergeTransitionsTogether(
                            mergedTransition, transition, null);
                } else {
                    // Overlap is not allowed, add them to the mergedNonOverlappingTransition
                    mergedNonOverlappingTransition = FragmentTransition.mergeTransitionsTogether(
                            mergedNonOverlappingTransition, transition, null);
                }
            }
        }

        // Make sure that the mergedNonOverlappingTransition set
        // runs after the mergedTransition set is complete
        mergedTransition = FragmentTransition.mergeTransitionsInSequence(mergedTransition,
                mergedNonOverlappingTransition, sharedElementTransition);

        // If there's no transitions playing together, no non-overlapping transitions,
        // and no shared element transitions, mergedTransition will be null and
        // there's nothing else we need to do
        if (mergedTransition == null) {
            return startedTransitions;
        }

        // Now set up our completion signal on the completely merged transition set
        for (final TransitionInfo transitionInfo : transitionInfos) {
            if (transitionInfo.isVisibilityUnchanged()) {
                // No change in visibility, so we've already completed the transition
                continue;
            }
            Object transition = transitionInfo.getTransition();
            Operation operation = transitionInfo.getOperation();
            boolean involvedInSharedElementTransition = sharedElementTransition != null
                    && (operation == firstOut || operation == lastIn);
            if (transition != null || involvedInSharedElementTransition) {
                // If the container has never been laid out, transitions will not start so
                // so lets instantly complete them.
                if (!getContainer().isLaidOut()) {
                    if (FragmentManager.TRACE) {
                        LOGGER.info(FragmentManager.MARKER,
                                "SpecialEffectsController: Container " + getContainer()
                                        + " has not been laid out. Completing operation "
                                        + operation);
                    }
                    transitionInfo.completeSpecialEffect();
                } else {
                    FragmentTransition.setListenerForTransitionEnd(
                            mergedTransition,
                            transitionInfo.getSignal(),
                            () -> {
                                transitionInfo.completeSpecialEffect();
                                if (FragmentManager.TRACE) {
                                    LOGGER.info(FragmentManager.MARKER,
                                            "Transition for operation " + operation + "has "
                                                    + "completed");
                                }
                            });
                }
            }
        }
        // Transitions won't run if the container isn't laid out so
        // we can return early here to avoid doing unnecessary work.
        if (!getContainer().isLaidOut()) {
            return startedTransitions;
        }
        // First, hide all of the entering views so they're in
        // the correct initial state
        FragmentTransition.setViewVisibility(enteringViews, View.INVISIBLE);
        ArrayList<String> inNames = FragmentTransition.prepareSetNameOverridesReordered(sharedElementLastInViews);
        if (FragmentManager.TRACE) {
            LOGGER.info(FragmentManager.MARKER, ">>>>> Beginning transition <<<<<");
            LOGGER.info(FragmentManager.MARKER, ">>>>> SharedElementFirstOutViews <<<<<");
            for (View view : sharedElementFirstOutViews) {
                LOGGER.info(FragmentManager.MARKER,
                        "View: " + view + " Name: " + view.getTransitionName());
            }
            LOGGER.info(FragmentManager.MARKER, ">>>>> SharedElementLastInViews <<<<<");
            for (View view : sharedElementLastInViews) {
                LOGGER.info(FragmentManager.MARKER, "View: " + view + " Name: "
                        + view.getTransitionName());
            }
        }
        // Now actually start the transition
        TransitionManager.beginDelayedTransition(getContainer(), mergedTransition);
        FragmentTransition.setNameOverridesReordered(getContainer(), sharedElementFirstOutViews,
                sharedElementLastInViews, inNames, sharedElementNameMapping);
        // Then, show all of the entering views, putting them into
        // the correct final state
        FragmentTransition.setViewVisibility(enteringViews, View.VISIBLE);
        FragmentTransition.swapSharedElementTargets(sharedElementTransition,
                sharedElementFirstOutViews, sharedElementLastInViews);
        return startedTransitions;
    }

    /**
     * Retain only the shared element views that have a transition name that is in
     * the set of transition names.
     *
     * @param sharedElementViews The map of shared element transitions that should be filtered.
     * @param transitionNames    The set of transition names to be retained.
     */
    void retainMatchingViews(@Nonnull ArrayMap<String, View> sharedElementViews,
                             @Nonnull Collection<String> transitionNames) {
        sharedElementViews.entrySet().removeIf(entry -> !transitionNames.contains(entry.getValue().getTransitionName()));
    }

    /**
     * Gets the Views in the hierarchy affected by entering and exiting transitions.
     *
     * @param transitioningViews This View will be added to transitioningViews if it has a
     *                           transition name, is VISIBLE and a normal View, or a ViewGroup with
     *                           {@link ViewGroup#isTransitionGroup()} true.
     * @param view               The base of the view hierarchy to look in.
     */
    void captureTransitioningViews(@Nonnull ArrayList<View> transitioningViews, View view) {
        if (view instanceof ViewGroup viewGroup) {
            if (viewGroup.isTransitionGroup()) {
                if (!transitioningViews.contains(view)) {
                    transitioningViews.add(viewGroup);
                }
            } else {
                int count = viewGroup.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = viewGroup.getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        captureTransitioningViews(transitioningViews, child);
                    }
                }
            }
        } else {
            if (!transitioningViews.contains(view)) {
                transitioningViews.add(view);
            }
        }
    }

    /**
     * Finds all views that have transition names in the hierarchy under the given view and
     * stores them in {@code namedViews} map with the name as the key.
     */
    void findNamedViews(@Nonnull Map<String, View> namedViews, @Nonnull View view) {
        String transitionName = view.getTransitionName();
        if (transitionName != null) {
            namedViews.put(transitionName, view);
        }
        if (view instanceof ViewGroup viewGroup) {
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = viewGroup.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    findNamedViews(namedViews, child);
                }
            }
        }
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
                    && fragment.mContainer.getTag(R.id.visible_removing_fragment_view_tag) != null) {
                fragment.mContainer.setTag(R.id.visible_removing_fragment_view_tag, null);
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

    private static class TransitionInfo extends SpecialEffectsInfo {

        @Nullable
        private final Transition mTransition;
        private final boolean mOverlapAllowed;
        @Nullable
        private final Transition mSharedElementTransition;

        TransitionInfo(@Nonnull Operation operation,
                       @Nonnull CancellationSignal signal, boolean isPop,
                       boolean providesSharedElementTransition) {
            super(operation, signal);
            if (operation.getFinalState() == Operation.State.VISIBLE) {
                mTransition = isPop
                        ? operation.getFragment().getReenterTransition()
                        : operation.getFragment().getEnterTransition();
                // Entering transitions can choose to run after all exit
                // transitions complete, rather than overlapping with them
                mOverlapAllowed = isPop
                        ? operation.getFragment().getAllowReturnTransitionOverlap()
                        : operation.getFragment().getAllowEnterTransitionOverlap();
            } else {
                mTransition = isPop
                        ? operation.getFragment().getReturnTransition()
                        : operation.getFragment().getExitTransition();
                // Removing Fragments always overlap other transitions
                mOverlapAllowed = true;
            }
            if (providesSharedElementTransition) {
                if (isPop) {
                    mSharedElementTransition =
                            operation.getFragment().getSharedElementReturnTransition();
                } else {
                    mSharedElementTransition =
                            operation.getFragment().getSharedElementEnterTransition();
                }
            } else {
                mSharedElementTransition = null;
            }
        }

        @Nullable
        Transition getTransition() {
            return mTransition;
        }

        boolean isOverlapAllowed() {
            return mOverlapAllowed;
        }

        @Nullable
        Transition getSharedElementTransition() {
            return mSharedElementTransition;
        }
    }
}
