/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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
import icyllis.modernui.lifecycle.ViewModelStoreOwner;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewParent;
import org.apache.logging.log4j.Marker;

import javax.annotation.Nonnull;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * Modified from Android Open Source Project.
 */
final class FragmentStateManager {

    private static final Marker MARKER = FragmentManager.MARKER;

    private final FragmentLifecycleCallbacksDispatcher mDispatcher;
    private final FragmentStore mFragmentStore;
    @Nonnull
    private final Fragment mFragment;

    private boolean mMovingToState = false;
    private int mFragmentManagerState = Fragment.INITIALIZING;

    /**
     * Create a FragmentStateManager from a brand-new Fragment instance.
     *
     * @param dispatcher    Dispatcher for any lifecycle callbacks triggered by this class
     * @param fragmentStore FragmentStore handling all Fragments
     * @param fragment      The Fragment to manage
     */
    FragmentStateManager(@Nonnull FragmentLifecycleCallbacksDispatcher dispatcher,
                         @Nonnull FragmentStore fragmentStore, @Nonnull Fragment fragment) {
        mDispatcher = dispatcher;
        mFragmentStore = fragmentStore;
        mFragment = fragment;
    }

    @Nonnull
    Fragment getFragment() {
        return mFragment;
    }

    /**
     * Set the state of the FragmentManager. This will be used by
     * {@link #computeExpectedState()} to limit the max state of the Fragment.
     *
     * @param state one of the constants in {@link Fragment}
     */
    void setFragmentManagerState(int state) {
        mFragmentManagerState = state;
    }

    /**
     * Compute the state that the Fragment should be in given the internal
     * state of the Fragment and the signals passed into FragmentStateManager.
     *
     * @return the state that the Fragment should be in
     */
    int computeExpectedState() {
        // If the FragmentManager is null, disallow changing the state at all
        if (mFragment.mFragmentManager == null) {
            return mFragment.mState;
        }
        // Assume the Fragment can go as high as the FragmentManager's state
        int maxState = mFragmentManagerState;

        // Don't allow the Fragment to go above its max lifecycle state
        switch (mFragment.mMaxState) {
            case RESUMED:
                // maxState can't go any higher than RESUMED, so there's nothing to do here
                break;
            case STARTED:
                maxState = Math.min(maxState, Fragment.STARTED);
                break;
            case CREATED:
                maxState = Math.min(maxState, Fragment.CREATED);
                break;
            case INITIALIZED:
                maxState = Math.min(maxState, Fragment.ATTACHED);
                break;
            default:
                maxState = Math.min(maxState, Fragment.INITIALIZING);
        }

        // For fragments that are created from a layout using the <fragment> tag (mFromLayout)
        if (mFragment.mFromLayout) {
            if (mFragment.mInLayout) {
                // Move them immediately to VIEW_CREATED when they are
                // actually added to the layout (mInLayout).
                maxState = Math.max(mFragmentManagerState, Fragment.VIEW_CREATED);
                // But don't move to higher than VIEW_CREATED until the view is added to its parent
                // and the LayoutInflater call has returned
                if (mFragment.mView != null && mFragment.mView.getParent() == null) {
                    maxState = Fragment.VIEW_CREATED;
                }
            } else {
                if (mFragmentManagerState < Fragment.ACTIVITY_CREATED) {
                    // But while they are not in the layout, don't allow their
                    // state to progress upward until the FragmentManager state
                    // is at least ACTIVITY_CREATED. This ensures they get the onInflate()
                    // callback before being attached or created.
                    maxState = Math.min(maxState, mFragment.mState);
                } else {
                    // Once the FragmentManager state is at least ACTIVITY_CREATED
                    // their state can progress up to CREATED as we assume that
                    // they are not ever going to be in layout
                    maxState = Math.min(maxState, Fragment.CREATED);
                }
            }
        }
        // Fragments that are not currently added will sit in the CREATED state.
        if (!mFragment.mAdded) {
            maxState = Math.min(maxState, Fragment.CREATED);
        }
        SpecialEffectsController.Operation.LifecycleImpact awaitingEffect = null;
        if (mFragment.mContainer != null) {
            SpecialEffectsController controller = SpecialEffectsController.getOrCreateController(
                    mFragment.mContainer, mFragment.getParentFragmentManager());
            awaitingEffect = controller.getAwaitingCompletionLifecycleImpact(this);
        }
        if (awaitingEffect == SpecialEffectsController.Operation.LifecycleImpact.ADDING) {
            // Fragments awaiting their enter effects cannot proceed beyond that state
            maxState = Math.min(maxState, Fragment.AWAITING_ENTER_EFFECTS);
        } else if (awaitingEffect == SpecialEffectsController.Operation.LifecycleImpact.REMOVING) {
            // Fragments that are in the process of being removed shouldn't go below that state
            maxState = Math.max(maxState, Fragment.AWAITING_EXIT_EFFECTS);
        } else if (mFragment.mRemoving) {
            if (mFragment.isInBackStack()) {
                // Fragments on the back stack shouldn't go higher than CREATED
                maxState = Math.min(maxState, Fragment.CREATED);
            } else {
                // While removing a fragment, we always move to INITIALIZING
                maxState = Math.min(maxState, Fragment.INITIALIZING);
            }
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (mFragment.mDeferStart && mFragment.mState < Fragment.STARTED) {
            maxState = Math.min(maxState, Fragment.ACTIVITY_CREATED);
        }
        if (FragmentManager.DEBUG) {
            LOGGER.trace(MARKER, "computeExpectedState() of " + maxState + " for "
                    + mFragment);
        }
        return maxState;
    }

    void moveToExpectedState() {
        if (mMovingToState) {
            if (FragmentManager.DEBUG) {
                LOGGER.trace(MARKER, "Ignoring re-entrant call to moveToExpectedState() for {}", mFragment);
            }
            return;
        }
        try {
            mMovingToState = true;

            int newState;
            while ((newState = computeExpectedState()) != mFragment.mState) {
                if (newState > mFragment.mState) {
                    // Moving upward
                    int nextStep = mFragment.mState + 1;
                    switch (nextStep) {
                        case Fragment.ATTACHED -> attach();
                        case Fragment.CREATED -> create();
                        case Fragment.VIEW_CREATED -> {
                            ensureInflatedView();
                            createView();
                        }
                        case Fragment.AWAITING_EXIT_EFFECTS -> activityCreated();
                        case Fragment.ACTIVITY_CREATED -> {
                            if (mFragment.mView != null && mFragment.mContainer != null) {
                                SpecialEffectsController controller = SpecialEffectsController
                                        .getOrCreateController(mFragment.mContainer,
                                                mFragment.getParentFragmentManager());
                                int visibility = mFragment.mView.getVisibility();
                                SpecialEffectsController.Operation.State finalState =
                                        SpecialEffectsController.Operation.State.from(visibility);
                                controller.enqueueAdd(finalState, this);
                            }
                            mFragment.mState = Fragment.ACTIVITY_CREATED;
                        }
                        case Fragment.STARTED -> start();
                        case Fragment.AWAITING_ENTER_EFFECTS -> mFragment.mState = Fragment.AWAITING_ENTER_EFFECTS;
                        case Fragment.RESUMED -> resume();
                    }
                } else {
                    // Moving downward
                    int nextStep = mFragment.mState - 1;
                    switch (nextStep) {
                        case Fragment.AWAITING_ENTER_EFFECTS -> pause();
                        case Fragment.STARTED -> mFragment.mState = Fragment.STARTED;
                        case Fragment.ACTIVITY_CREATED -> stop();
                        case Fragment.AWAITING_EXIT_EFFECTS -> {
                            if (FragmentManager.DEBUG) {
                                LOGGER.info(MARKER, "movefrom ACTIVITY_CREATED: " + mFragment);
                            }
                            if (mFragment.mView != null && mFragment.mContainer != null) {
                                SpecialEffectsController controller = SpecialEffectsController
                                        .getOrCreateController(mFragment.mContainer,
                                                mFragment.getParentFragmentManager());
                                controller.enqueueRemove(this);
                            }
                            mFragment.mState = Fragment.AWAITING_EXIT_EFFECTS;
                        }
                        case Fragment.VIEW_CREATED -> {
                            mFragment.mInLayout = false;
                            mFragment.mState = Fragment.VIEW_CREATED;
                        }
                        case Fragment.CREATED -> {
                            destroyFragmentView();
                            mFragment.mState = Fragment.CREATED;
                        }
                        case Fragment.ATTACHED -> destroy();
                        case Fragment.INITIALIZING -> detach();
                    }
                }
            }
            if (mFragment.mHiddenChanged) {
                if (mFragment.mView != null && mFragment.mContainer != null) {
                    // Get the controller and enqueue the show/hide
                    SpecialEffectsController controller = SpecialEffectsController
                            .getOrCreateController(mFragment.mContainer,
                                    mFragment.getParentFragmentManager());
                    if (mFragment.mHidden) {
                        controller.enqueueHide(this);
                    } else {
                        controller.enqueueShow(this);
                    }
                }
                if (mFragment.mFragmentManager != null) {
                    mFragment.mFragmentManager.invalidateMenuForFragment(mFragment);
                }
                mFragment.mHiddenChanged = false;
                mFragment.onHiddenChanged(mFragment.mHidden);
            }
        } finally {
            mMovingToState = false;
        }
    }

    void ensureInflatedView() {
        if (mFragment.mFromLayout && mFragment.mInLayout && !mFragment.mPerformedCreateView) {
            if (FragmentManager.DEBUG) {
                LOGGER.info(MARKER, "moveto CREATE_VIEW: " + mFragment);
            }
            mFragment.performCreateView(/*mFragment.performGetLayoutInflater(
                    mFragment.mSavedFragmentState), */null, mFragment.mSavedFragmentState);
            if (mFragment.mView != null) {
                //mFragment.mView.setSaveFromParentEnabled(false);
                mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
                if (mFragment.mHidden) mFragment.mView.setVisibility(View.GONE);
                mFragment.performViewCreated();
                mDispatcher.dispatchOnFragmentViewCreated(
                        mFragment, mFragment.mView, mFragment.mSavedFragmentState, false);
                mFragment.mState = Fragment.VIEW_CREATED;
            }
        }
    }

    void attach() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "moveto ATTACHED: " + mFragment);
        }
        mFragment.mHost = mFragment.mFragmentManager.getHost();
        mFragment.mParentFragment = mFragment.mFragmentManager.getParent();
        mDispatcher.dispatchOnFragmentPreAttached(mFragment, false);
        mFragment.performAttach();
        mDispatcher.dispatchOnFragmentAttached(mFragment, false);
    }

    void create() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "moveto CREATED: " + mFragment);
        }
        if (!mFragment.mIsCreated) {
            mDispatcher.dispatchOnFragmentPreCreated(
                    mFragment, mFragment.mSavedFragmentState, false);
            mFragment.performCreate(mFragment.mSavedFragmentState);
            mDispatcher.dispatchOnFragmentCreated(
                    mFragment, mFragment.mSavedFragmentState, false);
        } else {
            mFragment.restoreChildFragmentState(mFragment.mSavedFragmentState);
            mFragment.mState = Fragment.CREATED;
        }
    }

    void createView() {
        if (mFragment.mFromLayout) {
            // This case is handled by ensureInflatedView(), so there's nothing
            // else we need to do here.
            return;
        }
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "moveto CREATE_VIEW: {}", mFragment);
        }
        ViewGroup container = null;
        if (mFragment.mContainer != null) {
            container = mFragment.mContainer;
        } else if (mFragment.mContainerId != 0) {
            if (mFragment.mContainerId == View.NO_ID) {
                throw new IllegalArgumentException("Cannot create fragment " + mFragment
                        + " for a container view with no id");
            }
            FragmentContainer fragmentContainer = mFragment.mFragmentManager.getContainer();
            container = (ViewGroup) fragmentContainer.onFindViewById(mFragment.mContainerId);
            if (container == null && !mFragment.mRestored) {
                throw new IllegalArgumentException("No view found for id 0x"
                        + Integer.toHexString(mFragment.mContainerId)
                        + " for fragment " + mFragment);
            }
        }
        mFragment.mContainer = container;
        mFragment.performCreateView(/*layoutInflater, */container, mFragment.mSavedFragmentState);
        if (mFragment.mView != null) {
            //mFragment.mView.setSaveFromParentEnabled(false);
            mFragment.mView.setTag(R.id.fragment_container_view_tag, mFragment);
            if (container != null) {
                addViewToContainer();
            }
            if (mFragment.mHidden) {
                mFragment.mView.setVisibility(View.GONE);
            }
            // How I wish we could use doOnAttach
            /*if (mFragment.mView.isAttachedToWindow()) {
                ViewCompat.requestApplyInsets(mFragment.mView);
            } else {
                final View fragmentView = mFragment.mView;
                fragmentView.addOnAttachStateChangeListener(
                        new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View v) {
                                fragmentView.removeOnAttachStateChangeListener(this);
                                ViewCompat.requestApplyInsets(fragmentView);
                            }

                            @Override
                            public void onViewDetachedFromWindow(View v) {
                            }
                        });
            }*/
            mFragment.performViewCreated();
            mDispatcher.dispatchOnFragmentViewCreated(
                    mFragment, mFragment.mView, mFragment.mSavedFragmentState, false);
            int postOnViewCreatedVisibility = mFragment.mView.getVisibility();
            float postOnViewCreatedAlpha = mFragment.mView.getAlpha();
            mFragment.setPostOnViewCreatedAlpha(postOnViewCreatedAlpha);
            if (mFragment.mContainer != null && postOnViewCreatedVisibility == View.VISIBLE) {
                // Save the focused view if one was set via requestFocus()
                View focusedView = mFragment.mView.findFocus();
                if (focusedView != null) {
                    mFragment.setFocusedView(focusedView);
                    if (FragmentManager.DEBUG) {
                        LOGGER.trace(MARKER, "requestFocus: Saved focused view {} for Fragment {}",
                                focusedView, mFragment);
                    }
                }
                // Set the view alpha to 0
                mFragment.mView.setAlpha(0f);
            }
        }
        mFragment.mState = Fragment.VIEW_CREATED;
    }

    void activityCreated() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "moveto ACTIVITY_CREATED: " + mFragment);
        }
        mFragment.performActivityCreated();
    }

    void start() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "moveto STARTED: " + mFragment);
        }
        mFragment.performStart();
        mDispatcher.dispatchOnFragmentStarted(mFragment, false);
    }

    void resume() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "moveto RESUMED: " + mFragment);
        }
        View focusedView = mFragment.getFocusedView();
        if (focusedView != null && isFragmentViewChild(focusedView)) {
            boolean success = focusedView.requestFocus();
            if (FragmentManager.DEBUG) {
                LOGGER.trace(MARKER, "requestFocus: Restoring focused view "
                        + focusedView + " " + (success ? "succeeded" : "failed") + " on Fragment "
                        + mFragment + " resulting in focused view " + mFragment.mView.findFocus());
            }
        }
        mFragment.setFocusedView(null);
        mFragment.performResume();
        mDispatcher.dispatchOnFragmentResumed(mFragment, false);
        mFragment.mSavedFragmentState = null;
        /*mFragment.mSavedViewState = null;
        mFragment.mSavedViewRegistryState = null;*/
    }

    private boolean isFragmentViewChild(@Nonnull View view) {
        if (view == mFragment.mView) {
            return true;
        }
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == mFragment.mView) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    void pause() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "movefrom RESUMED: " + mFragment);
        }
        mFragment.performPause();
        mDispatcher.dispatchOnFragmentPaused(mFragment, false);
    }

    void stop() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "movefrom STARTED: " + mFragment);
        }
        mFragment.performStop();
        mDispatcher.dispatchOnFragmentStopped(mFragment, false);
    }

    void destroyFragmentView() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "movefrom CREATE_VIEW: " + mFragment);
        }
        // In cases where we never got up to AWAITING_EXIT_EFFECTS, we
        // need to manually remove the view from the container to reverse
        // what we did in createView()
        if (mFragment.mContainer != null && mFragment.mView != null) {
            mFragment.mContainer.removeView(mFragment.mView);
        }
        mFragment.performDestroyView();
        mDispatcher.dispatchOnFragmentViewDestroyed(mFragment, false);
        mFragment.mContainer = null;
        mFragment.mView = null;
        // Set here to ensure that Observers are called after
        // the Fragment's view is set to null
        mFragment.mViewLifecycleOwner = null;
        mFragment.mViewLifecycleOwnerLiveData.setValue(null);
        mFragment.mInLayout = false;
    }

    void destroy() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "movefrom CREATED: " + mFragment);
        }
        boolean beingRemoved = mFragment.mRemoving && !mFragment.isInBackStack();
        boolean shouldDestroy = beingRemoved
                || mFragmentStore.getViewModel().shouldDestroy(mFragment);
        if (shouldDestroy) {
            FragmentHostCallback<?> host = mFragment.mHost;
            boolean shouldClear;
            if (host instanceof ViewModelStoreOwner) {
                shouldClear = mFragmentStore.getViewModel().isCleared();
            } else {
                shouldClear = true;
            }
            if (beingRemoved || shouldClear) {
                mFragmentStore.getViewModel().clearViewModelState(mFragment);
            }
            mFragment.performDestroy();
            mDispatcher.dispatchOnFragmentDestroyed(mFragment, false);
            mFragmentStore.makeInactive(this);
        } else {
            mFragment.mState = Fragment.ATTACHED;
        }
    }

    void detach() {
        if (FragmentManager.DEBUG) {
            LOGGER.info(MARKER, "movefrom ATTACHED: " + mFragment);
        }
        mFragment.performDetach();
        mDispatcher.dispatchOnFragmentDetached(
                mFragment, false);
        mFragment.mState = Fragment.INITIALIZING;
        mFragment.mHost = null;
        mFragment.mParentFragment = null;
        mFragment.mFragmentManager = null;
        boolean beingRemoved = mFragment.mRemoving && !mFragment.isInBackStack();
        if (beingRemoved || mFragmentStore.getViewModel().shouldDestroy(mFragment)) {
            if (FragmentManager.DEBUG) {
                LOGGER.info(MARKER, "initState called for fragment: " + mFragment);
            }
            mFragment.initState();
        }
    }

    void addViewToContainer() {
        // Ensure that our new Fragment is placed in the right index
        // based on its relative position to Fragments already in the
        // same container
        int index = mFragmentStore.findFragmentIndexInContainer(mFragment);
        mFragment.mContainer.addView(mFragment.mView, index);
    }
}
