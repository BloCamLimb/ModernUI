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

import icyllis.modernui.animation.Animator;
import icyllis.modernui.annotation.CallSuper;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.lifecycle.*;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

/**
 * A Fragment is a piece of an application's user interface or behavior
 * that can be placed in a host object.  Interaction with fragments
 * is done through {@link FragmentManager}, which can be obtained via
 * {@link Fragment#getParentFragmentManager()} and other implementation
 * dependent methods.
 * <p>
 * For more information about using fragments, read the Android Fragments
 * developer guide.
 */
public class Fragment implements LifecycleOwner, ViewModelStoreOwner {

    static final Object USE_DEFAULT_TRANSITION = new Object();

    static final int INITIALIZING = -1;          // Not yet attached.
    static final int ATTACHED = 0;               // Attached to the host.
    static final int CREATED = 1;                // Created.
    static final int VIEW_CREATED = 2;           // View Created.
    static final int AWAITING_EXIT_EFFECTS = 3;  // Downward state, awaiting exit effects
    static final int ACTIVITY_CREATED = 4;       // Fully created, not started.
    static final int STARTED = 5;                // Created and started, not resumed.
    static final int AWAITING_ENTER_EFFECTS = 6; // Upward state, awaiting enter effects
    static final int RESUMED = 7;                // Created started and resumed.

    int mState = INITIALIZING;

    // When instantiated from saved state, this is the saved state.
    DataSet mSavedFragmentState;

    // Internal unique name for this fragment
    @Nonnull
    String mWho = UUID.randomUUID().toString();

    // Construction arguments;
    DataSet mArguments;

    // Boolean indicating whether this Fragment is the primary navigation fragment
    private Boolean mIsPrimaryNavigationFragment = null;

    // True if the fragment is in the list of added fragments.
    boolean mAdded;

    // If set this fragment is being removed from its activity.
    boolean mRemoving;

    // Set to true if this fragment was instantiated from a layout file.
    boolean mFromLayout;

    // Set to true when the view has actually been inflated in its layout.
    boolean mInLayout;

    // True if this fragment has been restored from previously saved state.
    boolean mRestored;

    // True if performCreateView has been called and a matching call to performDestroyView
    // has not yet happened.
    boolean mPerformedCreateView;

    // Number of active back stack entries this fragment is in.
    int mBackStackNesting;

    // The fragment manager we are associated with.  Set as soon as the
    // fragment is used in a transaction; cleared after it has been removed
    // from all transactions.
    FragmentManager mFragmentManager;

    // Host this fragment is attached to.
    FragmentHostCallback<?> mHost;

    // Private fragment manager for child fragments inside this one.
    @Nonnull
    FragmentManager mChildFragmentManager = new FragmentManager();

    // If this Fragment is contained in another Fragment, this is that container.
    Fragment mParentFragment;

    // The optional identifier for this fragment -- either the container ID if it
    // was dynamically added to the view hierarchy, or the ID supplied in
    // layout.
    int mFragmentId;

    // When a fragment is being dynamically added to the view hierarchy, this
    // is the identifier of the parent container it is being added to.
    int mContainerId;

    // The optional named tag for this fragment -- usually used to find
    // fragments that are not part of the layout.
    @Nullable
    String mTag;

    // Set to true when the app has requested that this fragment be hidden
    // from the user.
    boolean mHidden;

    // Set to true when the app has requested that this fragment be deactivated.
    boolean mDetached;

    // If set this fragment would like its instance retained across
    // configuration changes.
    boolean mRetainInstance;

    // If set this fragment changed its mRetainInstance while it was detached
    boolean mRetainInstanceChangedWhileDetached;

    // If set this fragment has menu items to contribute.
    boolean mHasMenu;

    // Set to true to allow the fragment's menu to be shown.
    boolean mMenuVisible = true;

    // Used to verify that subclasses call through to super class.
    private boolean mCalled;

    // The parent container of the fragment after dynamically added to UI.
    ViewGroup mContainer;

    // The View generated for this fragment.
    View mView;

    // Whether this fragment should defer starting until after other fragments
    // have been started and their loaders are finished.
    boolean mDeferStart;

    // Hint provided by the app that this fragment is currently visible to the user.
    boolean mUserVisibleHint = true;

    // The animation and transition information for the fragment. This will be null
    // unless the elements are explicitly accessed and should remain null for Fragments
    // without Views.
    AnimationInfo mAnimationInfo;

    // True if the View was added, and its animation has yet to be run. This could
    // also indicate that the fragment view hasn't been made visible, even if there is no
    // animation for this fragment.
    boolean mIsNewlyAdded;

    // True if mHidden has been changed and the animation should be scheduled.
    boolean mHiddenChanged;

    // The alpha of the view when the view was added and then postponed. If the value is less
    // than zero, this means that the view's add was canceled and should not participate in
    // removal animations.
    float mPostponedAlpha;

    // Keep track of whether this Fragment has run performCreate(). Retained instance
    // fragments can have mRetaining set to true without going through creation, so we must
    // track it separately.
    boolean mIsCreated;

    // Max Lifecycle state this Fragment can achieve.
    Lifecycle.State mMaxState = Lifecycle.State.RESUMED;

    LifecycleRegistry mLifecycleRegistry;

    // This is initialized in performCreateView and unavailable outside the
    // onCreateView/onDestroyView lifecycle
    @Nullable
    FragmentViewLifecycleOwner mViewLifecycleOwner;

    MutableLiveData<LifecycleOwner> mViewLifecycleOwnerLiveData = new MutableLiveData<>();

    ViewModelProvider.Factory mDefaultFactory;

    /**
     * Constructor used by the default {@link FragmentFactory}. You must
     * {@link FragmentManager#setFragmentFactory(FragmentFactory) set a custom FragmentFactory}
     * if you want to use a non-default constructor to ensure that your constructor
     * is called when the fragment is re-instantiated.
     *
     * <p>It is strongly recommended to supply arguments with {@link #setArguments}
     * and later retrieved by the Fragment with {@link #getArguments}. These arguments
     * are automatically saved and restored alongside the Fragment.
     *
     * <p>Applications should generally not implement a constructor. Prefer
     * {@link #onAttach()} instead. It is the first place application code can run where
     * the fragment is ready to be used - the point where the fragment is actually associated with
     * its context. Some applications may also want to implement {@link #onInflate} to retrieve
     * attributes from a layout resource, although note this happens when the fragment is attached.
     */
    public Fragment() {
        initLifecycle();
    }

    private void initLifecycle() {
        mLifecycleRegistry = new LifecycleRegistry(this);
        // The default factory depends on the SavedStateRegistry, so it
        // needs to be reset when the SavedStateRegistry is reset
        mDefaultFactory = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     */
    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Get a {@link LifecycleOwner} that represents the {@link #getView() Fragment's View}
     * lifecycle. In most cases, this mirrors the lifecycle of the Fragment itself, but in cases
     * of {@link FragmentTransaction#detach(Fragment) detached} Fragments, the lifecycle of the
     * Fragment can be considerably longer than the lifecycle of the View itself.
     * <p>
     * Namely, the lifecycle of the Fragment's View is:
     * <ol>
     * <li>{@link Lifecycle.Event#ON_CREATE created} after {@link #onViewStateRestored(DataSet)}</li>
     * <li>{@link Lifecycle.Event#ON_START started} after {@link #onStart()}</li>
     * <li>{@link Lifecycle.Event#ON_RESUME resumed} after {@link #onResume()}</li>
     * <li>{@link Lifecycle.Event#ON_PAUSE paused} before {@link #onPause()}</li>
     * <li>{@link Lifecycle.Event#ON_STOP stopped} before {@link #onStop()}</li>
     * <li>{@link Lifecycle.Event#ON_DESTROY destroyed} before {@link #onDestroyView()}</li>
     * </ol>
     * <p>
     * The first method where it is safe to access the view lifecycle is
     * {@link #onCreateView(ViewGroup, DataSet)} under the condition that you must
     * return a non-null view (an IllegalStateException will be thrown if you access the view
     * lifecycle but don't return a non-null view).
     * <p>The view lifecycle remains valid through the call to {@link #onDestroyView()}, after which
     * {@link #getView()} will return null, the view lifecycle will be destroyed, and this method
     * will throw an IllegalStateException. Consider using
     * {@link #getViewLifecycleOwnerLiveData()} or {@link FragmentTransaction#runOnCommit(Runnable)}
     * to receive a callback for when the Fragment's view lifecycle is available.
     * <p>
     * This should only be called on the main thread.
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     *
     * @return A {@link LifecycleOwner} that represents the {@link #getView() Fragment's View}
     * lifecycle.
     * @throws IllegalStateException if the {@link #getView() Fragment's View is null}.
     */
    @UiThread
    @Nonnull
    public LifecycleOwner getViewLifecycleOwner() {
        if (mViewLifecycleOwner == null) {
            throw new IllegalStateException("Can't access the Fragment View's LifecycleOwner when "
                    + "getView() is null i.e., before onCreateView() or after onDestroyView()");
        }
        return mViewLifecycleOwner;
    }

    /**
     * Retrieve a {@link LiveData} which allows you to observe the
     * {@link #getViewLifecycleOwner() lifecycle of the Fragment's View}.
     * <p>
     * This will be set to the new {@link LifecycleOwner} after {@link #onCreateView} returns a
     * non-null View and will set to null after {@link #onDestroyView()}.
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     *
     * @return A LiveData that changes in sync with {@link #getViewLifecycleOwner()}.
     */
    @Nonnull
    public LiveData<LifecycleOwner> getViewLifecycleOwnerLiveData() {
        return mViewLifecycleOwnerLiveData;
    }

    /**
     * Returns the {@link ViewModelStore} associated with this Fragment
     * <p>
     * Overriding this method is no longer supported and this method will be made
     * <code>final</code> in a future version of Fragment.
     *
     * @return a {@code ViewModelStore}
     * @throws IllegalStateException if called before the Fragment is attached i.e., before
     *                               onAttach().
     */
    @Nonnull
    @Override
    public ViewModelStore getViewModelStore() {
        if (mFragmentManager == null) {
            throw new IllegalStateException("Can't access ViewModels from detached fragment");
        }
        if (getMinimumMaxLifecycleState() == Lifecycle.State.INITIALIZED.ordinal()) {
            throw new IllegalStateException("Calling getViewModelStore() before a Fragment "
                    + "reaches onCreate() when using setMaxLifecycle(INITIALIZED) is not "
                    + "supported");
        }
        return mFragmentManager.getViewModelStore(this);
    }

    private int getMinimumMaxLifecycleState() {
        if (mMaxState == Lifecycle.State.INITIALIZED || mParentFragment == null) {
            return mMaxState.ordinal();
        }
        return Math.min(mMaxState.ordinal(), mParentFragment.getMinimumMaxLifecycleState());
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@link #getArguments() Fragment's arguments} when this is first called will be used
     * as the defaults to any {@link SavedStateHandle} passed to a view model
     * created using this factory.</p>
     */
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mFragmentManager == null) {
            throw new IllegalStateException("Can't access ViewModels from detached fragment");
        }
        return mDefaultFactory;
    }

    final boolean isInBackStack() {
        return mBackStackNesting > 0;
    }

    /**
     * Subclasses can not override equals().
     */
    @Override
    public final boolean equals(@Nullable Object o) {
        return super.equals(o);
    }

    /**
     * Subclasses can not override hashCode().
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Nonnull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(128);
        s.append(getClass().getSimpleName())
                .append('{')
                .append(Integer.toHexString(hashCode()))
                .append('}')
                .append(" (")
                .append(mWho);
        if (mFragmentId != 0) {
            s.append(" id=0x")
                    .append(Integer.toHexString(mFragmentId));
        }
        if (mTag != null) {
            s.append(" tag=")
                    .append(mTag);
        }
        return s.append(')').toString();
    }

    /**
     * Return the identifier this fragment is known by.  This is either
     * the android:id value supplied in a layout or the container view ID
     * supplied when adding the fragment.
     */
    public final int getId() {
        return mFragmentId;
    }

    /**
     * Get the tag name of the fragment, if specified.
     */
    @Nullable
    public final String getTag() {
        return mTag;
    }

    /**
     * Supply the construction arguments for this fragment.
     * The arguments supplied here will be retained across fragment destroy and
     * creation.
     * <p>This method cannot be called if the fragment is added to a FragmentManager and
     * if {@link #isStateSaved()} would return true.</p>
     */
    public void setArguments(@Nullable DataSet args) {
        if (mFragmentManager != null && isStateSaved()) {
            throw new IllegalStateException("Fragment already added and state has been saved");
        }
        mArguments = args;
    }

    /**
     * Return the arguments supplied when the fragment was instantiated,
     * if any.
     */
    @Nullable
    public final DataSet getArguments() {
        return mArguments;
    }

    /**
     * Return the arguments supplied when the fragment was instantiated.
     *
     * @throws IllegalStateException if no arguments were supplied to the Fragment.
     * @see #getArguments()
     */
    @Nonnull
    public final DataSet requireArguments() {
        DataSet arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("Fragment " + this + " does not have any arguments.");
        }
        return arguments;
    }

    /**
     * Returns true if this fragment is added and its state has already been saved
     * by its host. Any operations that would change saved state should not be performed
     * if this method returns true, and some operations such as {@link #setArguments(DataSet)}
     * will fail.
     *
     * @return true if this fragment's state has already been saved by its host
     */
    public final boolean isStateSaved() {
        if (mFragmentManager == null) {
            return false;
        }
        return mFragmentManager.isStateSaved();
    }

    /**
     * Set the initial saved state that this Fragment should restore itself
     * from when first being constructed, as returned by
     * {@link FragmentManager#saveFragmentInstanceState(Fragment)
     * FragmentManager.saveFragmentInstanceState}.
     *
     * @param state The state the fragment should be restored from.
     */
    public void setInitialSavedState(@Nullable DataSet state) {
        if (mFragmentManager != null) {
            throw new IllegalStateException("Fragment already added");
        }
        mSavedFragmentState = state;
    }

    /**
     * Return the host object of this fragment. May return {@code null} if the fragment
     * isn't currently being hosted.
     *
     * @see #requireHost()
     */
    @Nullable
    public final Object getHost() {
        return mHost == null ? null : mHost.onGetHost();
    }

    /**
     * Return the host object of this fragment.
     *
     * @throws IllegalStateException if not currently associated with a host.
     * @see #getHost()
     */
    @Nonnull
    public final Object requireHost() {
        Object host = getHost();
        if (host == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to a host.");
        }
        return host;
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this fragment's activity.
     *
     * <p>If this Fragment is a child of another Fragment, the FragmentManager
     * returned here will be the parent's {@link #getChildFragmentManager()}.
     *
     * @throws IllegalStateException if not associated with a transaction or host.
     */
    @Nonnull
    public final FragmentManager getParentFragmentManager() {
        FragmentManager fragmentManager = mFragmentManager;
        if (fragmentManager == null) {
            throw new IllegalStateException(
                    "Fragment " + this + " not associated with a fragment manager.");
        }
        return fragmentManager;
    }

    /**
     * Return a private FragmentManager for placing and managing Fragments
     * inside this Fragment.
     */
    @Nonnull
    public final FragmentManager getChildFragmentManager() {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " has not been attached yet.");
        }
        return mChildFragmentManager;
    }

    /**
     * Returns the parent Fragment containing this Fragment.  If this Fragment
     * is attached directly to an Activity, returns null.
     */
    @Nullable
    public final Fragment getParentFragment() {
        return mParentFragment;
    }

    /**
     * Returns the parent Fragment containing this Fragment.
     *
     * @throws IllegalStateException if this Fragment is attached directly to an Activity or
     *                               other Fragment host.
     * @see #getParentFragment()
     */
    @Nonnull
    public final Fragment requireParentFragment() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            Object host = getHost();
            if (host == null) {
                throw new IllegalStateException("Fragment " + this + " is not attached to"
                        + " any Fragment or host");
            } else {
                throw new IllegalStateException("Fragment " + this + " is not a child Fragment, it"
                        + " is directly attached to " + host);
            }
        }
        return parentFragment;
    }

    /**
     * Return true if the fragment is currently added to its activity.
     */
    public final boolean isAdded() {
        return mHost != null && mAdded;
    }

    /**
     * Return true if the fragment has been explicitly detached from the UI.
     * That is, {@link FragmentTransaction#detach(Fragment)
     * FragmentTransaction.detach(Fragment)} has been used on it.
     */
    public final boolean isDetached() {
        return mDetached;
    }

    /**
     * Return true if this fragment is currently being removed from its
     * activity.  This is  <em>not</em> whether its activity is finishing, but
     * rather whether it is in the process of being removed from its activity.
     */
    public final boolean isRemoving() {
        return mRemoving;
    }

    /**
     * Return <code>true</code> if this fragment or any of its ancestors are currently being removed
     * from its activity.  This is <em>not</em> whether its activity is finishing, but rather
     * whether it, or its ancestors are in the process of being removed from its activity.
     */
    final boolean isRemovingParent() {
        Fragment parent = getParentFragment();
        return parent != null && (parent.isRemoving() || parent.isRemovingParent());
    }

    /**
     * Return true if the layout is included as part of an activity view
     * hierarchy via the &lt;fragment&gt; tag.  This will always be true when
     * fragments are created through the &lt;fragment&gt; tag, <em>except</em>
     * in the case where an old fragment is restored from a previous state and
     * it does not appear in the layout of the current state.
     */
    public final boolean isInLayout() {
        return mInLayout;
    }

    /**
     * Return true if the fragment is in the resumed state.  This is true
     * for the duration of {@link #onResume()} and {@link #onPause()} as well.
     */
    public final boolean isResumed() {
        return mState >= RESUMED;
    }

    /**
     * Return true if the fragment is currently visible to the user.  This means
     * it: (1) has been added, (2) has its view attached to the window, and
     * (3) is not hidden.
     */
    public final boolean isVisible() {
        return isAdded() && !isHidden() && mView != null
                && mView.isAttachedToWindow() && mView.getVisibility() == View.VISIBLE;
    }

    /**
     * Return true if the fragment has been hidden.  By default, fragments
     * are shown.  You can find out about changes to this state with
     * {@link #onHiddenChanged}.  Note that the hidden state is orthogonal
     * to other states -- that is, to be visible to the user, a fragment
     * must be both started and not hidden.
     */
    public final boolean isHidden() {
        return mHidden;
    }

    final boolean hasOptionsMenu() {
        return mHasMenu;
    }

    final boolean isMenuVisible() {
        return mMenuVisible && (mFragmentManager == null
                || mFragmentManager.isParentMenuVisible(mParentFragment));
    }

    /**
     * Called when the hidden state (as returned by {@link #isHidden()} of
     * the fragment has changed.  Fragments start out not hidden; this will
     * be called whenever the fragment changes state from that.
     *
     * @param hidden True if the fragment is now hidden, false otherwise.
     */
    @UiThread
    public void onHiddenChanged(boolean hidden) {
    }

    /**
     * Report that this fragment would like to participate in populating
     * the options menu by receiving a call to {@link #onCreateOptionsMenu}
     * and related methods.
     *
     * @param hasMenu If true, the fragment has menu items to contribute.
     */
    public void setHasOptionsMenu(boolean hasMenu) {
        if (mHasMenu != hasMenu) {
            mHasMenu = hasMenu;
        }
    }

    /**
     * Set a hint for whether this fragment's menu should be visible.  This
     * is useful if you know that a fragment has been placed in your view
     * hierarchy so that the user can not currently seen it, so any menu items
     * it has should also not be shown.
     *
     * @param menuVisible The default is true, meaning the fragment's menu will
     *                    be shown as usual.  If false, the user will not see the menu.
     */
    public void setMenuVisibility(boolean menuVisible) {
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
        }
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(DataSet)} will be called after this.
     */
    @UiThread
    @CallSuper
    public void onAttach() {
        mCalled = true;
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach()} and before
     * {@link #onCreateView(ViewGroup, DataSet)}.
     *
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, add a {@link LifecycleObserver} on the
     * activity's Lifecycle, removing it when it receives the
     * {@link Lifecycle.State#CREATED} callback.
     *
     * <p>Any restored child fragments will be created before the base
     * <code>Fragment.onCreate</code> method returns.</p>
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @UiThread
    @CallSuper
    public void onCreate(@Nullable DataSet savedInstanceState) {
        mCalled = true;
        restoreChildFragmentState(savedInstanceState);
        if (!mChildFragmentManager.isStateAtLeast(Fragment.CREATED)) {
            mChildFragmentManager.dispatchCreate();
        }
    }

    /**
     * Restore the state of the child FragmentManager. Called by either
     * {@link #onCreate(DataSet)} for non-retained instance fragments or by
     * {@link FragmentManager#moveToState(Fragment, int, int, int, boolean)}
     * for retained instance fragments.
     *
     * <p><strong>Post-condition:</strong> if there were child fragments to restore,
     * the child FragmentManager will be instantiated and brought to the {@link #CREATED} state.
     * </p>
     *
     * @param savedInstanceState the savedInstanceState potentially containing fragment info
     */
    void restoreChildFragmentState(@Nullable DataSet savedInstanceState) {
        /*if (savedInstanceState != null) {
            Parcelable p = savedInstanceState.getParcelable(
                    FragmentActivity.FRAGMENTS_TAG);
            if (p != null) {
                mChildFragmentManager.restoreSaveState(p);
                mChildFragmentManager.dispatchCreate();
            }
        }*/
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(DataSet)} and {@link #onViewCreated(View, DataSet)}.
     *
     * <p>It is recommended to <strong>only</strong> inflate the layout in this method and move
     * logic that operates on the returned View to {@link #onViewCreated(View, DataSet)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @UiThread
    @Nullable
    public View onCreateView(@Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        return null;
    }

    /**
     * Called immediately after {@link #onCreateView(ViewGroup, DataSet)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     *
     * @param view               The View returned by {@link #onCreateView(ViewGroup, DataSet)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @UiThread
    public void onViewCreated(@Nonnull View view, @Nullable DataSet savedInstanceState) {
    }

    /**
     * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}),
     * if provided.
     *
     * @return The fragment's root view, or null if it has no layout.
     */
    @Nullable
    public View getView() {
        return mView;
    }

    /**
     * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}).
     *
     * @throws IllegalStateException if no view was returned by {@link #onCreateView}.
     * @see #getView()
     */
    @Nonnull
    public final View requireView() {
        View view = getView();
        if (view == null) {
            throw new IllegalStateException("Fragment " + this + " did not return a View from"
                    + " onCreateView() or this was called before onCreateView().");
        }
        return view;
    }

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after {@link #onViewCreated(View, DataSet)} and before {@link #onStart()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @UiThread
    @CallSuper
    public void onViewStateRestored(@Nullable DataSet savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when the Fragment is visible to the user.
     */
    @UiThread
    @CallSuper
    public void onStart() {
        mCalled = true;
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     */
    @UiThread
    @CallSuper
    public void onResume() {
        mCalled = true;
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance if its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(DataSet)},
     * {@link #onCreateView(ViewGroup, DataSet)}, and
     * {@link #onViewCreated(View, DataSet)}.
     *
     * <p>Note however: <em>this method may be called
     * at any time before {@link #onDestroy()}</em>.  There are many situations
     * where a fragment may be mostly torn down (such as when placed on the
     * back stack with no UI showing), but its state will not be saved until
     * its owning activity actually needs to save its state.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @UiThread
    public void onSaveInstanceState(@Nonnull DataSet outState) {
    }

    /**
     * Callback for when the primary navigation state of this Fragment has changed. This can be
     * the result of the {@link #getParentFragmentManager()}  containing FragmentManager} having its
     * primary navigation fragment changed via {@link FragmentTransaction#setPrimaryNavigationFragment}
     * or due to the primary navigation fragment changing in a parent FragmentManager.
     *
     * @param isPrimaryNavigationFragment True if and only if this Fragment and any
     *                                    {@link #getParentFragment() parent fragment} is set as the primary navigation
     *                                    fragment via {@link FragmentTransaction#setPrimaryNavigationFragment}.
     */
    @UiThread
    public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {
    }

    /**
     * Called when the Fragment is no longer resumed.
     */
    @UiThread
    @CallSuper
    public void onPause() {
        mCalled = true;
    }

    /**
     * Called when the Fragment is no longer started.
     */
    @UiThread
    @CallSuper
    public void onStop() {
        mCalled = true;
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved, but before it has been removed from its parent.
     */
    @UiThread
    @CallSuper
    public void onDestroyView() {
        mCalled = true;
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @UiThread
    @CallSuper
    public void onDestroy() {
        mCalled = true;
    }

    /**
     * Called by the fragment manager once this fragment has been removed,
     * so that we don't have any left-over state if the application decides
     * to re-use the instance.  This only clears state that the framework
     * internally manages, not things the application sets.
     */
    void initState() {
        initLifecycle();
        mWho = UUID.randomUUID().toString();
        mAdded = false;
        mRemoving = false;
        mFromLayout = false;
        mInLayout = false;
        mRestored = false;
        mBackStackNesting = 0;
        mFragmentManager = null;
        mChildFragmentManager = new FragmentManager();
        mHost = null;
        mFragmentId = 0;
        mContainerId = 0;
        mTag = null;
        mHidden = false;
        mDetached = false;
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @UiThread
    @CallSuper
    public void onDetach() {
        mCalled = true;
    }


    /**
     * Print the Fragments's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd     The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state.  This will be
     *               closed for you after you return.
     * @param args   additional arguments to the dump request.
     */
    public void dump(@Nonnull String prefix, @Nullable FileDescriptor fd,
                     @Nonnull PrintWriter writer, @Nullable String... args) {
        writer.print(prefix);
        writer.print("mFragmentId=#");
        writer.print(Integer.toHexString(mFragmentId));
        writer.print(" mContainerId=#");
        writer.print(Integer.toHexString(mContainerId));
        writer.print(" mTag=");
        writer.println(mTag);
        writer.print(prefix);
        writer.print("mState=");
        writer.print(mState);
        writer.print(" mWho=");
        writer.print(mWho);
        writer.print(" mBackStackNesting=");
        writer.println(mBackStackNesting);
        writer.print(prefix);
        writer.print("mAdded=");
        writer.print(mAdded);
        writer.print(" mRemoving=");
        writer.print(mRemoving);
        writer.print(" mFromLayout=");
        writer.print(mFromLayout);
        writer.print(" mInLayout=");
        writer.println(mInLayout);
        writer.print(prefix);
        writer.print("mHidden=");
        writer.print(mHidden);
        writer.print(" mDetached=");
        writer.print(mDetached);
        writer.print(" mMenuVisible=");
        writer.print(mMenuVisible);
        writer.print(" mHasMenu=");
        writer.println(mHasMenu);
        writer.print(prefix);
        writer.print("mRetainInstance=");
        writer.print(mRetainInstance);
        if (mFragmentManager != null) {
            writer.print(prefix);
            writer.print("mFragmentManager=");
            writer.println(mFragmentManager);
        }
        if (mHost != null) {
            writer.print(prefix);
            writer.print("mHost=");
            writer.println(mHost);
        }
        if (mParentFragment != null) {
            writer.print(prefix);
            writer.print("mParentFragment=");
            writer.println(mParentFragment);
        }
        if (mArguments != null) {
            writer.print(prefix);
            writer.print("mArguments=");
            writer.println(mArguments);
        }
        if (mSavedFragmentState != null) {
            writer.print(prefix);
            writer.print("mSavedFragmentState=");
            writer.println(mSavedFragmentState);
        }
        if (mContainer != null) {
            writer.print(prefix);
            writer.print("mContainer=");
            writer.println(mContainer);
        }
        if (mView != null) {
            writer.print(prefix);
            writer.print("mView=");
            writer.println(mView);
        }
        writer.print(prefix);
        writer.println("Child " + mChildFragmentManager + ":");
        mChildFragmentManager.dump(prefix + "  ", fd, writer, args);
    }

    @Nullable
    Fragment findFragmentByWho(@Nonnull String who) {
        if (who.equals(mWho)) {
            return this;
        }
        return mChildFragmentManager.findFragmentByWho(who);
    }

    @Nonnull
    FragmentContainer createFragmentContainer() {
        return new FragmentContainer() {
            @Override
            @Nullable
            public View onFindViewById(int id) {
                if (mView == null) {
                    throw new IllegalStateException("Fragment " + Fragment.this
                            + " does not have a view");
                }
                return mView.findViewById(id);
            }

            @Override
            public boolean onHasView() {
                return mView != null;
            }
        };
    }

    void performAttach() {
        mChildFragmentManager.attachController(mHost, createFragmentContainer(), this);
        mState = ATTACHED;
        mCalled = false;
        onAttach();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onAttach()");
        }
        mFragmentManager.dispatchOnAttachFragment(this);
        mChildFragmentManager.dispatchAttach();
    }

    void performCreate(DataSet savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mState = CREATED;
        mCalled = false;
        mLifecycleRegistry.addObserver(new LifecycleObserver() {
            @Override
            public void onStateChanged(@Nonnull LifecycleOwner source,
                                       @Nonnull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_STOP) {
                    if (mView != null) {
                        mView.cancelPendingInputEvents();
                    }
                }
            }
        });
        onCreate(savedInstanceState);
        mIsCreated = true;
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onCreate()");
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    void performCreateView(@Nullable ViewGroup container,
                           @Nullable DataSet savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mPerformedCreateView = true;
        mViewLifecycleOwner = new FragmentViewLifecycleOwner(this, getViewModelStore());
        mView = onCreateView(container, savedInstanceState);
        if (mView != null) {
            // Initialize the view lifecycle
            mViewLifecycleOwner.initialize();
            // Tell the fragment's new view about it before we tell anyone listening
            // to mViewLifecycleOwnerLiveData and before onViewCreated, so that calls to
            // ViewTree get() methods return something meaningful
            /*ViewTreeLifecycleOwner.set(mView, mViewLifecycleOwner);
            ViewTreeViewModelStoreOwner.set(mView, mViewLifecycleOwner);
            ViewTreeSavedStateRegistryOwner.set(mView, mViewLifecycleOwner);*/
            // Then inform any Observers of the new LifecycleOwner
            mViewLifecycleOwnerLiveData.setValue(mViewLifecycleOwner);
        } else {
            if (mViewLifecycleOwner.isInitialized()) {
                throw new IllegalStateException("Called getViewLifecycleOwner() but "
                        + "onCreateView() returned null");
            }
            mViewLifecycleOwner = null;
        }
    }

    void performViewCreated() {
        // since calling super.onViewCreated() is not required, we do not need to set and check the
        // `mCalled` flag
        onViewCreated(mView, mSavedFragmentState);
        mChildFragmentManager.dispatchViewCreated();
    }

    void performActivityCreated(DataSet savedInstanceState) {
        mChildFragmentManager.noteStateNotSaved();
        mState = AWAITING_EXIT_EFFECTS;
        //restoreViewState();
        mChildFragmentManager.dispatchActivityCreated();
    }

    void performStart() {
        mChildFragmentManager.noteStateNotSaved();
        mChildFragmentManager.execPendingActions(true);
        mState = STARTED;
        mCalled = false;
        onStart();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onStart()");
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        if (mView != null) {
            assert mViewLifecycleOwner != null;
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }
        mChildFragmentManager.dispatchStart();
    }

    void performResume() {
        mChildFragmentManager.noteStateNotSaved();
        mChildFragmentManager.execPendingActions(true);
        mState = RESUMED;
        mCalled = false;
        onResume();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onResume()");
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        if (mView != null) {
            assert mViewLifecycleOwner != null;
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }
        mChildFragmentManager.dispatchResume();
    }

    void noteStateNotSaved() {
        mChildFragmentManager.noteStateNotSaved();
    }

    void performPrimaryNavigationFragmentChanged() {
        boolean isPrimaryNavigationFragment = mFragmentManager.isPrimaryNavigation(this);
        // Only send out the callback / dispatch if the state has changed
        if (mIsPrimaryNavigationFragment == null
                || mIsPrimaryNavigationFragment != isPrimaryNavigationFragment) {
            mIsPrimaryNavigationFragment = isPrimaryNavigationFragment;
            onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment);
            mChildFragmentManager.dispatchPrimaryNavigationFragmentChanged();
        }
    }

    void performPause() {
        mChildFragmentManager.dispatchPause();
        if (mView != null) {
            assert mViewLifecycleOwner != null;
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        mState = AWAITING_ENTER_EFFECTS;
        mCalled = false;
        onPause();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onPause()");
        }
    }

    void performStop() {
        mChildFragmentManager.dispatchStop();
        if (mView != null) {
            assert mViewLifecycleOwner != null;
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        }
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mState = ACTIVITY_CREATED;
        mCalled = false;
        onStop();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onStop()");
        }
    }

    void performDestroyView() {
        mChildFragmentManager.dispatchDestroyView();
        if (mView != null) {
            assert mViewLifecycleOwner != null;
            if (mViewLifecycleOwner.getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.CREATED)) {
                mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
            }
        }
        mState = CREATED;
        mCalled = false;
        onDestroyView();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onDestroyView()");
        }
        mPerformedCreateView = false;
    }

    void performDestroy() {
        mChildFragmentManager.dispatchDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        mState = ATTACHED;
        mCalled = false;
        mIsCreated = false;
        onDestroy();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onDestroy()");
        }
    }

    void performDetach() {
        mState = INITIALIZING;
        mCalled = false;
        onDetach();
        if (!mCalled) {
            throw new IllegalStateException("Fragment " + this
                    + " did not call through to super.onDetach()");
        }

        // Destroy the child FragmentManager if we still have it here.
        // This is normally done in performDestroy(), but is done here
        // specifically if the Fragment is retained.
        if (!mChildFragmentManager.isDestroyed()) {
            mChildFragmentManager.dispatchDestroy();
            mChildFragmentManager = new FragmentManager();
        }
    }

    void setOnStartEnterTransitionListener(OnStartEnterTransitionListener listener) {
        ensureAnimationInfo();
        if (listener == mAnimationInfo.mStartEnterTransitionListener) {
            return;
        }
        if (listener != null && mAnimationInfo.mStartEnterTransitionListener != null) {
            throw new IllegalStateException("Trying to set a replacement "
                    + "startPostponedEnterTransition on " + this);
        }
        if (mAnimationInfo.mEnterTransitionPostponed) {
            mAnimationInfo.mStartEnterTransitionListener = listener;
        }
        if (listener != null) {
            listener.startListening();
        }
    }

    private AnimationInfo ensureAnimationInfo() {
        if (mAnimationInfo == null) {
            mAnimationInfo = new AnimationInfo();
        }
        return mAnimationInfo;
    }

    void setAnimations(int enter, int exit, int popEnter, int popExit) {
        if (mAnimationInfo == null && enter == 0 && exit == 0 && popEnter == 0 && popExit == 0) {
            return; // no change!
        }
        ensureAnimationInfo().mEnterAnim = enter;
        ensureAnimationInfo().mExitAnim = exit;
        ensureAnimationInfo().mPopEnterAnim = popEnter;
        ensureAnimationInfo().mPopExitAnim = popExit;
    }

    int getEnterAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mEnterAnim;
    }

    int getExitAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mExitAnim;
    }

    int getPopEnterAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mPopEnterAnim;
    }

    int getPopExitAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mPopExitAnim;
    }

    boolean getPopDirection() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mIsPop;
    }

    void setPopDirection(boolean isPop) {
        if (mAnimationInfo == null) {
            return; // no change!
        }
        ensureAnimationInfo().mIsPop = isPop;
    }

    int getNextTransition() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mNextTransition;
    }

    void setNextTransition(int nextTransition) {
        if (mAnimationInfo == null && nextTransition == 0) {
            return; // no change!
        }
        ensureAnimationInfo();
        mAnimationInfo.mNextTransition = nextTransition;
    }

    @Nonnull
    ArrayList<String> getSharedElementSourceNames() {
        if (mAnimationInfo == null || mAnimationInfo.mSharedElementSourceNames == null) {
            return new ArrayList<>();
        }
        return mAnimationInfo.mSharedElementSourceNames;
    }

    @Nonnull
    ArrayList<String> getSharedElementTargetNames() {
        if (mAnimationInfo == null || mAnimationInfo.mSharedElementTargetNames == null) {
            return new ArrayList<>();
        }
        return mAnimationInfo.mSharedElementTargetNames;
    }

    void setSharedElementNames(@Nullable ArrayList<String> sharedElementSourceNames,
                               @Nullable ArrayList<String> sharedElementTargetNames) {
        ensureAnimationInfo();
        mAnimationInfo.mSharedElementSourceNames = sharedElementSourceNames;
        mAnimationInfo.mSharedElementTargetNames = sharedElementTargetNames;
    }

    View getAnimatingAway() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mAnimatingAway;
    }

    void setAnimatingAway(View view) {
        ensureAnimationInfo().mAnimatingAway = view;
    }

    void setAnimator(Animator animator) {
        ensureAnimationInfo().mAnimator = animator;
    }

    Animator getAnimator() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mAnimator;
    }

    void setPostOnViewCreatedAlpha(float alpha) {
        ensureAnimationInfo().mPostOnViewCreatedAlpha = alpha;
    }

    float getPostOnViewCreatedAlpha() {
        if (mAnimationInfo == null) {
            return 1f;
        }
        return mAnimationInfo.mPostOnViewCreatedAlpha;
    }

    void setFocusedView(View view) {
        ensureAnimationInfo().mFocusedView = view;
    }

    View getFocusedView() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mFocusedView;
    }

    boolean isPostponed() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mEnterTransitionPostponed;
    }

    boolean isHideReplaced() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mIsHideReplaced;
    }

    void setHideReplaced(boolean replaced) {
        ensureAnimationInfo().mIsHideReplaced = replaced;
    }

    /**
     * Used internally to be notified when {@link #startPostponedEnterTransition()} has
     * been called. This listener will only be called once and then be removed from the
     * listeners.
     */
    interface OnStartEnterTransitionListener {
        void onStartEnterTransition();

        void startListening();
    }

    /**
     * Contains all the animation and transition information for a fragment. This will only
     * be instantiated for Fragments that have Views.
     */
    static class AnimationInfo {
        // Non-null if the fragment's view hierarchy is currently animating away,
        // meaning we need to wait a bit on completely destroying it.  This is the
        // view that is animating.
        View mAnimatingAway;

        // Non-null if the fragment's view hierarchy is currently animating away with an
        // animator instead of an animation.
        Animator mAnimator;

        // If app requests the animation direction, this is what to use
        boolean mIsPop;

        // All possible animations
        int mEnterAnim;
        int mExitAnim;
        int mPopEnterAnim;
        int mPopExitAnim;

        // If app has requested a specific transition, this is the one to use.
        int mNextTransition;

        // If app has requested a specific set of shared element objects, this is the one to use.
        ArrayList<String> mSharedElementSourceNames;
        ArrayList<String> mSharedElementTargetNames;

        Object mEnterTransition = null;
        Object mReturnTransition = USE_DEFAULT_TRANSITION;
        Object mExitTransition = null;
        Object mReenterTransition = USE_DEFAULT_TRANSITION;
        Object mSharedElementEnterTransition = null;
        Object mSharedElementReturnTransition = USE_DEFAULT_TRANSITION;
        Boolean mAllowReturnTransitionOverlap;
        Boolean mAllowEnterTransitionOverlap;

        float mPostOnViewCreatedAlpha = 1f;
        View mFocusedView = null;

        // True when postponeEnterTransition has been called and startPostponeEnterTransition
        // hasn't been called yet.
        boolean mEnterTransitionPostponed;

        // Listener to wait for startPostponeEnterTransition. After being called, it will
        // be set to null
        OnStartEnterTransitionListener mStartEnterTransitionListener;

        // True if the View was hidden, but the transition is handling the hide
        boolean mIsHideReplaced;
    }
}
