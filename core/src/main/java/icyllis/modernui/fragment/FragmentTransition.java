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

import icyllis.modernui.core.CancellationSignal;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.transition.Transition;
import icyllis.modernui.transition.TransitionListener;
import icyllis.modernui.transition.TransitionSet;
import icyllis.modernui.util.ArrayMap;
import icyllis.modernui.view.OneShotPreDrawListener;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Contains the Fragment Transition functionality.
 */
final class FragmentTransition {

    private FragmentTransition() {
    }

    /**
     * Utility to find the String key in {@code map} that maps to {@code value}.
     */
    @Nullable
    static String findKeyForValue(@Nonnull ArrayMap<String, String> map, @Nonnull String value) {
        final int numElements = map.size();
        for (int i = 0; i < numElements; i++) {
            if (value.equals(map.valueAt(i))) {
                return map.keyAt(i);
            }
        }
        return null;
    }

    /**
     * A utility to retain only the mappings in {@code nameOverrides} that have a value
     * that has a key in {@code namedViews}. This is a useful equivalent to
     * {@link ArrayMap#retainAll(Collection)} for values.
     */
    static void retainValues(@Nonnull ArrayMap<String, String> nameOverrides,
                             @Nonnull ArrayMap<String, View> namedViews) {
        for (int i = nameOverrides.size() - 1; i >= 0; i--) {
            final String targetName = nameOverrides.valueAt(i);
            if (!namedViews.containsKey(targetName)) {
                nameOverrides.removeAt(i);
            }
        }
    }

    /**
     * Calls the {@link SharedElementCallback#onSharedElementStart(List, List, List)} or
     * {@link SharedElementCallback#onSharedElementEnd(List, List, List)} on the appropriate
     * incoming or outgoing fragment.
     *
     * @param inFragment     The incoming fragment
     * @param outFragment    The outgoing fragment
     * @param isPop          Is the incoming fragment part of a pop transaction?
     * @param sharedElements The shared element Views
     * @param isStart        Call the start or end call on the SharedElementCallback
     */
    static void callSharedElementStartEnd(Fragment inFragment, Fragment outFragment,
                                          boolean isPop, ArrayMap<String, View> sharedElements, boolean isStart) {
        SharedElementCallback sharedElementCallback = isPop
                ? outFragment.getEnterTransitionCallback()
                : inFragment.getEnterTransitionCallback();
        if (sharedElementCallback != null) {
            ArrayList<View> views = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            final int count = sharedElements == null ? 0 : sharedElements.size();
            for (int i = 0; i < count; i++) {
                names.add(sharedElements.keyAt(i));
                views.add(sharedElements.valueAt(i));
            }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null);
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null);
            }
        }
    }

    /**
     * Sets the visibility of all Views in {@code views} to {@code visibility}.
     */
    static void setViewVisibility(ArrayList<View> views, int visibility) {
        if (views == null) {
            return;
        }
        for (int i = views.size() - 1; i >= 0; i--) {
            final View view = views.get(i);
            view.setVisibility(visibility);
        }
    }

    /**
     * Prepares for setting the shared element names by gathering the names of the incoming
     * shared elements and clearing them. {@link #setNameOverridesReordered(View, ArrayList,
     * ArrayList, ArrayList, Map)} must be called after this to complete setting the shared element
     * name overrides. This must be called before
     */
    @Nonnull
    static ArrayList<String> prepareSetNameOverridesReordered(@Nonnull ArrayList<View> sharedElementsIn) {
        final ArrayList<String> names = new ArrayList<>();
        for (final View view : sharedElementsIn) {
            names.add(view.getTransitionName());
            view.setTransitionName(null);
        }
        return names;
    }

    /**
     * Changes the shared element names for the incoming shared elements to match those of the
     * outgoing shared elements. This also temporarily clears the shared element names of the
     * outgoing shared elements. Must be called after
     */
    static void setNameOverridesReordered(@Nonnull final View sceneRoot,
                                          @Nonnull final ArrayList<View> sharedElementsOut,
                                          @Nonnull final ArrayList<View> sharedElementsIn,
                                          @Nonnull final ArrayList<String> inNames,
                                          @Nonnull final Map<String, String> nameOverrides) {
        final int numSharedElements = sharedElementsIn.size();
        final ArrayList<String> outNames = new ArrayList<>();

        for (int i = 0; i < numSharedElements; i++) {
            final View view = sharedElementsOut.get(i);
            final String name = view.getTransitionName();
            outNames.add(name);
            if (name == null) {
                continue;
            }
            view.setTransitionName(null);
            final String inName = nameOverrides.get(name);
            for (int j = 0; j < numSharedElements; j++) {
                if (inName.equals(inNames.get(j))) {
                    sharedElementsIn.get(j).setTransitionName(name);
                    break;
                }
            }
        }

        OneShotPreDrawListener.add(sceneRoot, () -> {
            for (int i = 0; i < numSharedElements; i++) {
                sharedElementsIn.get(i).setTransitionName(inNames.get(i));
                sharedElementsOut.get(i).setTransitionName(outNames.get(i));
            }
        });
    }

    static void setSharedElementTargets(@Nonnull TransitionSet transition,
                                        @Nonnull View nonExistentView,
                                        @Nonnull ArrayList<View> sharedViews) {
        final List<View> views = transition.getTargets();
        views.clear();
        for (final View view : sharedViews) {
            bfsAddViewChildren(views, view);
        }
        views.add(nonExistentView);
        sharedViews.add(nonExistentView);
        addTargets(transition, sharedViews);
    }

    static void setEpicenter(@Nonnull Transition transition, View view) {
        if (view != null) {
            final Rect epicenter = new Rect();
            view.getBoundsOnScreen(epicenter);

            transition.setEpicenterCallback(t -> epicenter);
        }
    }

    static void setEpicenter(Transition transition, final Rect epicenter) {
        if (transition != null) {
            transition.setEpicenterCallback(t -> {
                if (epicenter == null || epicenter.isEmpty()) {
                    return null;
                }
                return epicenter;
            });
        }
    }

    static void addTargets(@Nullable Transition transition, @Nonnull ArrayList<View> views) {
        if (transition == null) {
            return;
        }
        if (transition instanceof TransitionSet set) {
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                addTargets(child, views);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (isNullOrEmpty(targets)) {
                // We can just add the target views
                for (View view : views) {
                    transition.addTarget(view);
                }
            }
        }
    }

    @Nonnull
    static TransitionSet mergeTransitionsTogether(@Nullable Transition transition1, @Nullable Transition transition2,
                                                  @Nullable Transition transition3) {
        TransitionSet transitionSet = new TransitionSet();
        if (transition1 != null) {
            transitionSet.addTransition(transition1);
        }
        if (transition2 != null) {
            transitionSet.addTransition(transition2);
        }
        if (transition3 != null) {
            transitionSet.addTransition(transition3);
        }
        return transitionSet;
    }

    static void scheduleHideFragmentView(@Nonnull Transition exitTransition, final View fragmentView,
                                         final ArrayList<View> exitingViews) {
        exitTransition.addListener(new TransitionListener() {
            @Override
            public void onTransitionStart(@Nonnull Transition transition) {
                // If any of the exiting views are not shared elements, the TransitionManager
                // adds additional listeners to the this transition. If those listeners are
                // DisappearListeners for a view that is going away, they can change the state of
                // views after our onTransitionEnd callback.
                // We need to make sure this listener gets the onTransitionEnd callback last to
                // ensure that exiting views are made visible once the Transition is complete.
                transition.removeListener(this);
                transition.addListener(this);
            }

            @Override
            public void onTransitionEnd(@Nonnull Transition transition) {
                transition.removeListener(this);
                fragmentView.setVisibility(View.GONE);
                for (View exitingView : exitingViews) {
                    exitingView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    static Transition mergeTransitionsInSequence(final Transition exitTransition,
                                                 final Transition enterTransition,
                                                 final Transition sharedElementTransition) {
        // First do exit, then enter, but allow shared element transition to happen
        // during both.
        Transition staggered = null;
        if (exitTransition != null && enterTransition != null) {
            staggered = new TransitionSet()
                    .addTransition(exitTransition)
                    .addTransition(enterTransition)
                    .setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
        } else if (exitTransition != null) {
            staggered = exitTransition;
        } else if (enterTransition != null) {
            staggered = enterTransition;
        }
        if (sharedElementTransition != null) {
            TransitionSet together = new TransitionSet();
            if (staggered != null) {
                together.addTransition(staggered);
            }
            together.addTransition(sharedElementTransition);
            return together;
        } else {
            return staggered;
        }
    }

    static void scheduleRemoveTargets(
            @Nonnull final Transition overallTransition,
            @Nullable final Transition enterTransition, final ArrayList<View> enteringViews,
            @Nullable final Transition exitTransition, final ArrayList<View> exitingViews,
            @Nullable final Transition sharedElementTransition, final ArrayList<View> sharedElementsIn) {
        overallTransition.addListener(new TransitionListener() {
            @Override
            public void onTransitionStart(@Nonnull Transition transition) {
                if (enterTransition != null) {
                    replaceTargets(enterTransition, enteringViews, null);
                }
                if (exitTransition != null) {
                    replaceTargets(exitTransition, exitingViews, null);
                }
                if (sharedElementTransition != null) {
                    replaceTargets(sharedElementTransition, sharedElementsIn, null);
                }
            }

            @Override
            public void onTransitionEnd(@Nonnull Transition transition) {
                transition.removeListener(this);
            }
        });
    }

    static void setListenerForTransitionEnd(@Nonnull final Transition transition,
                                            @Nonnull final CancellationSignal signal,
                                            @Nonnull final Runnable transitionCompleteRunnable) {
        signal.setOnCancelListener(transition::cancel);
        transition.addListener(new TransitionListener() {
            @Override
            public void onTransitionEnd(@Nonnull Transition transition) {
                transitionCompleteRunnable.run();
            }
        });
    }

    static void swapSharedElementTargets(TransitionSet sharedElementTransition,
                                         ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn) {
        if (sharedElementTransition != null) {
            sharedElementTransition.getTargets().clear();
            sharedElementTransition.getTargets().addAll(sharedElementsIn);
            replaceTargets(sharedElementTransition, sharedElementsOut, sharedElementsIn);
        }
    }

    static void replaceTargets(Transition transition, ArrayList<View> oldTargets,
                               ArrayList<View> newTargets) {
        if (transition instanceof TransitionSet set) {
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                replaceTargets(child, oldTargets, newTargets);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (targets.size() == oldTargets.size()
                    && targets.containsAll(oldTargets)) {
                // We have an exact match. We must have added these earlier in addTargets
                final int targetCount = newTargets == null ? 0 : newTargets.size();
                for (int i = 0; i < targetCount; i++) {
                    transition.addTarget(newTargets.get(i));
                }
                for (int i = oldTargets.size() - 1; i >= 0; i--) {
                    transition.removeTarget(oldTargets.get(i));
                }
            }
        }
    }

    /**
     * Uses a breadth-first scheme to add startView and all of its children to views.
     * It won't add a child if it is already in views or if it has a transition name.
     */
    static void bfsAddViewChildren(@Nonnull final List<View> views, @Nonnull final View startView) {
        final int startIndex = views.size();
        if (containedBeforeIndex(views, startView, startIndex)) {
            return; // This child is already in the list, so all its children are also.
        }
        if (startView.getTransitionName() != null) {
            views.add(startView);
        }
        for (int index = startIndex; index < views.size(); index++) {
            final View view = views.get(index);
            if (view instanceof ViewGroup viewGroup) {
                final int childCount = viewGroup.getChildCount();
                for (int childIndex = 0; childIndex < childCount; childIndex++) {
                    final View child = viewGroup.getChildAt(childIndex);
                    if (!containedBeforeIndex(views, child, startIndex)
                            && child.getTransitionName() != null) {
                        views.add(child);
                    }
                }
            }
        }
    }

    /**
     * Does a linear search through views for view, limited to maxIndex.
     */
    private static boolean containedBeforeIndex(final List<View> views, final View view,
                                                final int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            if (views.get(i) == view) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there are any targets based on ID, transition or type.
     */
    private static boolean hasSimpleTarget(@Nonnull Transition transition) {
        return !isNullOrEmpty(transition.getTargetIds())
                || !isNullOrEmpty(transition.getTargetNames())
                || !isNullOrEmpty(transition.getTargetTypes());
    }

    /**
     * Simple utility to detect if a list is null or has no elements.
     */
    private static boolean isNullOrEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
