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

import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.lifecycle.Lifecycle;
import icyllis.modernui.lifecycle.LifecycleOwner;
import icyllis.modernui.lifecycle.ViewModelStore;
import icyllis.modernui.lifecycle.ViewModelStoreOwner;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewParent;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * Interface for interacting with {@link Fragment} objects inside a host.
 * <p>
 * You should never instantiate a FragmentManager directly, but instead
 * operate via the APIs on {@link Fragment}, or {@link FragmentController} to
 * retrieve an instance.
 * <p>
 * For more information about using fragments, read the Android Fragments
 * developer guide.
 */
public final class FragmentManager {

    static final boolean DEBUG = true;
    static final boolean TRACE = true;

    static final Marker MARKER = MarkerManager.getMarker("FragmentManager");

    // Constant IDs for Fragment package.
    static final int fragment_container_view_tag = 0x02020001;
    static final int visible_removing_fragment_view_tag = 0x02020002;
    static final int special_effects_controller_view_tag = 0x02020003;

    /**
     * Flag for {@link #popBackStack(String, int)}
     * and {@link #popBackStack(int, int)}: If set, and the name or ID of
     * a back stack entry has been supplied, then all matching entries will
     * be consumed until one that doesn't match is found or the bottom of
     * the stack is reached.  Otherwise, all entries up to but not including that entry
     * will be removed.
     */
    public static final int POP_BACK_STACK_INCLUSIVE = 1;

    /**
     * Representation of an entry on the fragment back stack, as created
     * with {@link FragmentTransaction#addToBackStack(String)
     * FragmentTransaction.addToBackStack()}.  Entries can later be
     * retrieved with {@link FragmentManager#getBackStackEntryAt(int)
     * FragmentManager.getBackStackEntryAt()}.
     *
     * <p>Note that you should never hold on to a BackStackEntry object;
     * the identifier as returned by {@link #getId} is the only thing that
     * will be persisted across activity instances.
     */
    public interface BackStackEntry {
        /**
         * Return the unique identifier for the entry.  This is the only
         * representation of the entry that will persist across activity
         * instances.
         */
        int getId();

        /**
         * Get the name that was supplied to
         * {@link FragmentTransaction#addToBackStack(String)
         * FragmentTransaction.addToBackStack(String)} when creating this entry.
         */
        @Nullable
        String getName();
    }

    /**
     * Interface to watch for changes to the back stack.
     */
    @FunctionalInterface
    public interface OnBackStackChangedListener {
        /**
         * Called whenever the contents of the back stack change.
         */
        @UiThread
        void onBackStackChanged();
    }

    private final ArrayList<OpGenerator> mPendingActions = new ArrayList<>();
    private boolean mExecutingActions;

    private final FragmentStore mFragmentStore = new FragmentStore();
    ArrayList<BackStackRecord> mBackStack;
    private ArrayList<Fragment> mCreatedMenus;
    private OnBackPressedDispatcher mOnBackPressedDispatcher;
    private final OnBackPressedCallback mOnBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            FragmentManager.this.handleOnBackPressed();
        }
    };

    private final AtomicInteger mBackStackIndex = new AtomicInteger();

    private ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    private final FragmentLifecycleCallbacksDispatcher mLifecycleCallbacksDispatcher =
            new FragmentLifecycleCallbacksDispatcher(this);
    private final CopyOnWriteArrayList<FragmentOnAttachListener> mOnAttachListeners =
            new CopyOnWriteArrayList<>();

    int mCurState = Fragment.INITIALIZING;
    private FragmentHostCallback<?> mHost;
    private FragmentContainer mContainer;
    private Fragment mParent;
    @Nullable
    Fragment mPrimaryNav;
    private FragmentFactory mFragmentFactory = null;
    private static final FragmentFactory sHostFragmentFactory = new FragmentFactory();
    private SpecialEffectsControllerFactory mSpecialEffectsControllerFactory = null;
    private static final SpecialEffectsControllerFactory sDefaultSpecialEffectsControllerFactory =
            DefaultSpecialEffectsController::new;

    private boolean mNeedMenuInvalidate;
    private boolean mStateSaved;
    private boolean mStopped;
    private boolean mDestroyed;
    private boolean mHavePendingDeferredStart;

    // Temporary vars for removing redundant operations in BackStackRecords:
    private ArrayList<BackStackRecord> mTmpRecords;
    private BooleanArrayList mTmpIsPop;
    private ArrayList<Fragment> mTmpAddedFragments;

    private FragmentManagerViewModel mViewModel;

    private final Runnable mExecCommit = () -> execPendingActions(true);

    /**
     * Package private.
     */
    FragmentManager() {
    }

    private void throwException(@Nonnull RuntimeException ex) {
        LOGGER.error(MARKER, "FragmentManager throws an exception", ex);
        PrintWriter w = new PrintWriter(new LogWriter(MARKER));
        if (mHost != null) {
            try {
                mHost.onDump("  ", null, w);
            } catch (Exception e) {
                LOGGER.error(MARKER, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", null, w);
            } catch (Exception e) {
                LOGGER.error(MARKER, "Failed dumping state", e);
            }
        }
        throw ex;
    }

    /**
     * Start a series of edit operations on the Fragments associated with
     * this FragmentManager.
     *
     * <p>Note: A fragment transaction can only be created/committed prior
     * to an activity saving its state.  If you try to commit a transaction
     * after <code>onSaveInstanceState()</code> and prior to a following
     * <code>onStart</code> or <code>onResume()</code>, you will get an error.
     * This is because the framework takes care of saving your current fragments
     * in the state, and if changes are made after the state is saved then they
     * will be lost.</p>
     */
    @Nonnull
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    /**
     * After a {@link FragmentTransaction} is committed with
     * {@link FragmentTransaction#commit FragmentTransaction.commit()}, it
     * is scheduled to be executed asynchronously on the process's main thread.
     * If you want to immediately executing any such pending operations, you
     * can call this function (only from the main thread) to do so.  Note that
     * all callbacks and other related behavior will be done from within this
     * call, so be careful about where this is called from.
     *
     * <p>If you are committing a single transaction that does not modify the
     * fragment back stack, strongly consider using
     * {@link FragmentTransaction#commitNow()} instead. This can help avoid
     * unwanted side effects when other code in your app has pending committed
     * transactions that expect different timing.</p>
     * <p>
     * This also forces the start of any postponed Transactions where
     * {@link Fragment#postponeEnterTransition()} has been called.
     *
     * @return Returns true if there were any pending transactions to be
     * executed.
     */
    public boolean executePendingTransactions() {
        boolean updates = execPendingActions(true);
        forcePostponedTransactions();
        return updates;
    }

    private void updateOnBackPressedCallbackEnabled() {
        // Always enable the callback if we have pending actions
        // as we don't know if they'll change the back stack entry count.
        // See handleOnBackPressed() for more explanation
        synchronized (mPendingActions) {
            if (!mPendingActions.isEmpty()) {
                mOnBackPressedCallback.setEnabled(true);
                return;
            }
        }
        // This FragmentManager needs to have a back stack for this to be enabled
        // And the parent fragment, if it exists, needs to be the primary navigation
        // fragment.
        mOnBackPressedCallback.setEnabled(getBackStackEntryCount() > 0
                && isPrimaryNavigation(mParent));
    }

    /**
     * Recursively check up the FragmentManager hierarchy of primary
     * navigation Fragments to ensure that all the parent Fragments are the
     * primary navigation Fragment for their associated FragmentManager
     */
    boolean isPrimaryNavigation(@Nullable Fragment parent) {
        // If the parent is null, then we're at the root host,
        // and we're always the primary navigation
        if (parent == null) {
            return true;
        }
        FragmentManager parentFragmentManager = parent.mFragmentManager;
        Fragment primaryNavigationFragment = parentFragmentManager
                .getPrimaryNavigationFragment();
        // The parent Fragment needs to be the primary navigation Fragment
        // and, if it has a parent itself, that parent also needs to be
        // the primary navigation fragment, recursively up the stack
        return parent.equals(primaryNavigationFragment)
                && isPrimaryNavigation(parentFragmentManager.mParent);
    }

    /**
     * Recursively check up the FragmentManager hierarchy of Fragments to see
     * if the menus are all visible.
     */
    boolean isParentMenuVisible(@Nullable Fragment parent) {
        if (parent == null) {
            return true;
        }

        return parent.isMenuVisible();
    }

    /**
     * Recursively check up the FragmentManager hierarchy of Fragments to see
     * if the fragment is hidden.
     */
    boolean isParentHidden(@Nullable Fragment parent) {
        if (parent == null) {
            return false;
        }

        return parent.isHidden();
    }

    void handleOnBackPressed() {
        // First, execute any pending actions to make sure we're in an
        // up-to-date view of the world just in case anyone is queuing
        // up transactions that change the back stack then immediately
        // calling onBackPressed()
        execPendingActions(true);
        if (mOnBackPressedCallback.isEnabled()) {
            // We still have a back stack, so we can pop
            popBackStackImmediate();
        } else {
            // Sigh. Due to FragmentManager's asynchronicity, we can
            // get into cases where we *think* we can handle the back
            // button but because of frame perfect dispatch, we fell
            // on our face. Since our callback is disabled, we can
            // re-trigger the onBackPressed() to dispatch to the next
            // enabled callback
            mOnBackPressedDispatcher.onBackPressed();
        }
    }

    /**
     * Pop the top state off the back stack. This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     */
    public void popBackStack() {
        enqueueAction(new PopBackStackState(null, -1, 0), false);
    }

    /**
     * Like {@link #popBackStack()}, but performs the operation immediately
     * inside the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     *
     * @return Returns true if there was something popped, else false.
     */
    public boolean popBackStackImmediate() {
        return popBackStackImmediate(null, -1, 0);
    }

    /**
     * Pop the last fragment transition from the manager's fragment
     * back stack.
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param name  If non-null, this is the name of a previous back state
     *              to look for; if found, all states up to that state will be popped.  The
     *              {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
     *              the named state itself is popped. If null, only the top state is popped.
     * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
     */
    public void popBackStack(@Nullable final String name, final int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    /**
     * Like {@link #popBackStack(String, int)}, but performs the operation immediately
     * inside the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     *
     * @return Returns true if there was something popped, else false.
     */
    public boolean popBackStackImmediate(@Nullable String name, int flags) {
        return popBackStackImmediate(name, -1, flags);
    }

    /**
     * Pop all back stack states up to the one with the given identifier.
     * This function is asynchronous -- it enqueues the
     * request to pop, but the action will not be performed until the application
     * returns to its event loop.
     *
     * @param id    Identifier of the stated to be popped. If no identifier exists,
     *              false is returned.
     *              The identifier is the number returned by
     *              {@link FragmentTransaction#commit() FragmentTransaction.commit()}.  The
     *              {@link #POP_BACK_STACK_INCLUSIVE} flag can be used to control whether
     *              the named state itself is popped.
     * @param flags Either 0 or {@link #POP_BACK_STACK_INCLUSIVE}.
     */
    public void popBackStack(final int id, final int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new PopBackStackState(null, id, flags), false);
    }

    /**
     * Like {@link #popBackStack(int, int)}, but performs the operation immediately
     * inside the call.  This is like calling {@link #executePendingTransactions()}
     * afterwards without forcing the start of postponed Transactions.
     *
     * @return Returns true if there was something popped, else false.
     */
    public boolean popBackStackImmediate(int id, int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackImmediate(null, id, flags);
    }

    /**
     * Used by all public popBackStackImmediate methods, this executes pending transactions and
     * returns true if the pop action did anything, regardless of what other pending
     * transactions did.
     *
     * @return true if the pop operation did anything or false otherwise.
     */
    private boolean popBackStackImmediate(@Nullable String name, int id, int flags) {
        execPendingActions(false);
        ensureExecReady(true);

        if (mPrimaryNav != null // We have a primary nav fragment
                && id < 0 // No valid id (since they're local)
                && name == null) { // no name to pop to (since they're local)
            final FragmentManager childManager = mPrimaryNav.getChildFragmentManager();
            if (childManager.popBackStackImmediate()) {
                // We did something, just not to this specific FragmentManager. Return true.
                return true;
            }
        }

        boolean executePop = popBackStackState(mTmpRecords, mTmpIsPop, name, id, flags);
        if (executePop) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();
        return executePop;
    }

    /**
     * Return the number of entries currently in the back stack.
     */
    public int getBackStackEntryCount() {
        return mBackStack != null ? mBackStack.size() : 0;
    }

    /**
     * Return the BackStackEntry at index <var>index</var> in the back stack;
     * entries start index 0 being the bottom of the stack.
     */
    @Nonnull
    public BackStackEntry getBackStackEntryAt(int index) {
        return mBackStack.get(index);
    }

    /**
     * Add a new listener for changes to the fragment back stack.
     */
    public void addOnBackStackChangedListener(@Nonnull OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners == null) {
            mBackStackChangeListeners = new ArrayList<>();
        }
        mBackStackChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added with
     * {@link #addOnBackStackChangedListener(OnBackStackChangedListener)}.
     */
    public void removeOnBackStackChangedListener(@Nonnull OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners != null) {
            mBackStackChangeListeners.remove(listener);
        }
    }

    /**
     * Put a reference to a fragment in a DataSet.  This DataSet can be
     * persisted as saved state, and when later restoring
     * {@link #getFragment(DataSet, String)} will return the current
     * instance of the same fragment.
     *
     * @param bundle   The bundle in which to put the fragment reference.
     * @param key      The name of the entry in the bundle.
     * @param fragment The Fragment whose reference is to be stored.
     */
    public void putFragment(@Nonnull DataSet bundle, @Nonnull String key,
                            @Nonnull Fragment fragment) {
        if (fragment.mFragmentManager != this) {
            throwException(new IllegalStateException("Fragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        bundle.putString(key, fragment.mWho);
    }

    /**
     * Retrieve the current Fragment instance for a reference previously
     * placed with {@link #putFragment(DataSet, String, Fragment)}.
     *
     * @param bundle The bundle from which to retrieve the fragment reference.
     * @param key    The name of the entry in the bundle.
     * @return Returns the current Fragment instance that is associated with
     * the given reference.
     */
    @Nullable
    public Fragment getFragment(@Nonnull DataSet bundle, @Nonnull String key) {
        String who = bundle.getString(key);
        if (who == null) {
            return null;
        }
        Fragment f = findActiveFragment(who);
        if (f == null) {
            throwException(new IllegalStateException("Fragment no longer exists for key "
                    + key + ": unique id " + who));
        }
        return f;
    }

    /**
     * Find a {@link Fragment} associated with the given {@link View}.
     * <p>
     * This method will locate the {@link Fragment} associated with this view. This is automatically
     * populated for the View returned by {@link Fragment#onCreateView} and its children.
     *
     * @param view the view to search from
     * @param <F>  the fragment type
     * @return the locally scoped {@link Fragment} to the given view
     * @throws IllegalStateException if the given view does not correspond with a
     *                               {@link Fragment}.
     * @throws ClassCastException    if the given type parameter is wrong
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <F extends Fragment> F findFragment(@Nonnull View view) {
        Fragment fragment = findViewFragment(view);
        if (fragment == null) {
            throw new IllegalStateException("View " + view + " does not have a Fragment set");
        }
        return (F) fragment;
    }

    /**
     * Recurse up the view hierarchy, looking for the Fragment
     *
     * @param view the view to search from
     * @return the locally scoped {@link Fragment} to the given view, if found
     */
    @Nullable
    private static Fragment findViewFragment(@Nonnull View view) {
        for (; ; ) {
            Fragment fragment = getViewFragment(view);
            if (fragment != null) {
                return fragment;
            }
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                view = (View) parent;
            } else {
                return null;
            }
        }
    }

    /**
     * Check if this view has an associated Fragment
     *
     * @param view the view to search from
     * @return the locally scoped {@link Fragment} to the given view, if found
     */
    @Nullable
    static Fragment getViewFragment(@Nonnull View view) {
        Object tag = view.getTag(fragment_container_view_tag);
        if (tag instanceof Fragment) {
            return (Fragment) tag;
        }
        return null;
    }

    void onContainerAvailable(@Nonnull FragmentContainerView container) {
        for (FragmentStateManager fragmentStateManager :
                mFragmentStore.getActiveFragmentStateManagers()) {
            Fragment fragment = fragmentStateManager.getFragment();
            if (fragment.mContainerId == container.getId() && fragment.mView != null
                    && fragment.mView.getParent() == null) {
                fragment.mContainer = container;
                fragmentStateManager.addViewToContainer();
            }
        }
    }

    /**
     * Recurse up the view hierarchy, looking for a FragmentManager
     *
     * @param view the view to search from
     * @return The containing {@link FragmentManager} of the given view.
     * @throws IllegalStateException if there is no Fragment associated with the view.
     */
    @Nonnull
    static FragmentManager findFragmentManager(@Nonnull View view) {
        // Search the view ancestors for a Fragment
        Fragment fragment = findViewFragment(view);
        // If there is a Fragment in the hierarchy, get its childFragmentManager
        if (fragment != null) {
            if (!fragment.isAdded()) {
                throw new IllegalStateException("The Fragment " + fragment + " that owns View "
                        + view + " has already been destroyed. Nested fragments should always "
                        + "use the child FragmentManager.");
            }
            return fragment.getChildFragmentManager();
        } else {
            throw new IllegalStateException("View " + view + " is not associated with a Fragment");
        }
    }

    /**
     * Get a list of all fragments that are currently added to the FragmentManager.
     * This may include those that are hidden as well as those that are shown.
     * This will not include any fragments only in the back stack, or fragments that
     * are detached or removed.
     * <p>
     * The order of the fragments in the list is the order in which they were
     * added or attached.
     *
     * @return A list of all fragments that are added to the FragmentManager.
     */
    @Nonnull
    public List<Fragment> getFragments() {
        return mFragmentStore.getFragments();
    }

    @Nonnull
    ViewModelStore getViewModelStore(@Nonnull Fragment f) {
        return mViewModel.getViewModelStore(f);
    }

    @Nonnull
    private FragmentManagerViewModel getChildViewModel(@Nonnull Fragment f) {
        return mViewModel.getChildViewModel(f);
    }

    //TODO review
    void addRetainedFragment(@Nonnull Fragment f) {
        mViewModel.addRetainedFragment(f);
    }

    void removeRetainedFragment(@Nonnull Fragment f) {
        mViewModel.removeRetainedFragment(f);
    }

    /**
     * This is used by FragmentController to get the Active fragments.
     *
     * @return A list of active fragments in the fragment manager, including those that are in the
     * back stack.
     */
    @Nonnull
    List<Fragment> getActiveFragments() {
        return mFragmentStore.getActiveFragments();
    }

    /**
     * Used by FragmentController to get the number of Active Fragments.
     *
     * @return The number of active fragments.
     */
    int getActiveFragmentCount() {
        return mFragmentStore.getActiveFragmentCount();
    }

    /**
     * Returns true if the final <code>onDestroy()</code>
     * call has been made on the FragmentManager's Activity, so this instance is now dead.
     */
    public boolean isDestroyed() {
        return mDestroyed;
    }

    @Nonnull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (mParent != null) {
            Class<?> cls = mParent.getClass();
            sb.append(cls.getSimpleName());
            sb.append("{");
            sb.append(Integer.toHexString(System.identityHashCode(mParent)));
            sb.append("}");
        } else if (mHost != null) {
            Class<?> cls = mHost.getClass();
            sb.append(cls.getSimpleName());
            sb.append("{");
            sb.append(Integer.toHexString(System.identityHashCode(mHost)));
            sb.append("}");
        } else {
            sb.append("null");
        }
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Print the FragmentManager's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd     The raw file descriptor that the dump is being sent to.
     * @param writer A PrintWriter to which the dump is to be set.
     * @param args   Additional arguments to the dump request.
     */
    public void dump(@Nonnull String prefix, @Nullable FileDescriptor fd,
                     @Nonnull PrintWriter writer, @Nullable String... args) {
        String innerPrefix = prefix + "    ";

        mFragmentStore.dump(prefix, fd, writer, args);

        int count;
        if (mCreatedMenus != null) {
            count = mCreatedMenus.size();
            if (count > 0) {
                writer.print(prefix);
                writer.println("Fragments Created Menus:");
                for (int i = 0; i < count; i++) {
                    Fragment f = mCreatedMenus.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }

        if (mBackStack != null) {
            count = mBackStack.size();
            if (count > 0) {
                writer.print(prefix);
                writer.println("Back Stack:");
                for (int i = 0; i < count; i++) {
                    BackStackRecord bs = mBackStack.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(bs.toString());
                    bs.dump(innerPrefix, writer);
                }
            }
        }

        writer.print(prefix);
        writer.println("Back Stack Index: " + mBackStackIndex.get());

        synchronized (mPendingActions) {
            count = mPendingActions.size();
            if (count > 0) {
                writer.print(prefix);
                writer.println("Pending Actions:");
                for (int i = 0; i < count; i++) {
                    OpGenerator r = mPendingActions.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(r);
                }
            }
        }

        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(mContainer);
        if (mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(mCurState);
        writer.print(" mStateSaved=");
        writer.print(mStateSaved);
        writer.print(" mStopped=");
        writer.print(mStopped);
        writer.print(" mDestroyed=");
        writer.println(mDestroyed);
        if (mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(mNeedMenuInvalidate);
        }
    }

    void performPendingDeferredStart(@Nonnull FragmentStateManager fragmentStateManager) {
        Fragment f = fragmentStateManager.getFragment();
        if (f.mDeferStart) {
            if (mExecutingActions) {
                // Wait until we're done executing our pending transactions
                mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            fragmentStateManager.moveToExpectedState();
        }
    }

    boolean isStateAtLeast(int state) {
        return mCurState >= state;
    }

    /**
     * Allows for changing the draw order on a container, if the container is a
     * FragmentContainerView.
     */
    void setExitAnimationOrder(@Nonnull Fragment f, boolean isPop) {
        ViewGroup container = getFragmentContainer(f);
        if (container != null) {
            if (container instanceof FragmentContainerView) {
                ((FragmentContainerView) container).setDrawDisappearingViewsLast(!isPop);
            }
        }
    }

    /**
     * Changes the state of the fragment manager to {@code newState}. If the fragment manager
     * changes state or {@code always} is {@code true}, any fragments within it have their
     * states updated as well.
     *
     * @param newState The new state for the fragment manager
     * @param always   If {@code true}, all fragments update their state, even
     *                 if {@code newState} matches the current fragment manager's state.
     */
    void moveToState(int newState, boolean always) {
        if (mHost == null && newState != Fragment.INITIALIZING) {
            throw new IllegalStateException("No activity");
        }

        if (!always && newState == mCurState) {
            return;
        }

        mCurState = newState;
        mFragmentStore.moveToExpectedState();
        startPendingDeferredFragments();

        if (mNeedMenuInvalidate && mHost != null && mCurState == Fragment.RESUMED) {
            //mHost.onSupportInvalidateOptionsMenu();
            mNeedMenuInvalidate = false;
        }
    }

    private void startPendingDeferredFragments() {
        for (FragmentStateManager fragmentStateManager :
                mFragmentStore.getActiveFragmentStateManagers()) {
            performPendingDeferredStart(fragmentStateManager);
        }
    }

    /**
     * For a given Fragment, get any existing FragmentStateManager found in the
     * {@link FragmentStore} or create a brand new FragmentStateManager if one does
     * not exist.
     *
     * @param f The Fragment to create a FragmentStateManager for
     * @return A valid FragmentStateManager
     */
    @Nonnull
    FragmentStateManager createOrGetFragmentStateManager(@Nonnull Fragment f) {
        FragmentStateManager existing = mFragmentStore.getFragmentStateManager(f.mWho);
        if (existing != null) {
            return existing;
        }
        FragmentStateManager fragmentStateManager = new FragmentStateManager(
                mLifecycleCallbacksDispatcher, mFragmentStore, f);
        // Catch the FragmentStateManager up to our current state
        fragmentStateManager.setFragmentManagerState(mCurState);
        return fragmentStateManager;
    }

    @Nonnull
    FragmentStateManager addFragment(@Nonnull Fragment fragment) {
        if (TRACE) LOGGER.info(MARKER, "add: " + fragment);
        FragmentStateManager fragmentStateManager = createOrGetFragmentStateManager(fragment);
        fragment.mFragmentManager = this;
        mFragmentStore.makeActive(fragmentStateManager);
        if (!fragment.mDetached) {
            mFragmentStore.addFragment(fragment);
            fragment.mRemoving = false;
            if (fragment.mView == null) {
                fragment.mHiddenChanged = false;
            }
            if (isMenuAvailable(fragment)) {
                mNeedMenuInvalidate = true;
            }
        }
        return fragmentStateManager;
    }

    void removeFragment(@Nonnull Fragment fragment) {
        if (TRACE) {
            LOGGER.info(MARKER, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        }
        final boolean inactive = !fragment.isInBackStack();
        if (!fragment.mDetached || inactive) {
            mFragmentStore.removeFragment(fragment);
            if (isMenuAvailable(fragment)) {
                mNeedMenuInvalidate = true;
            }
            fragment.mRemoving = true;
            setVisibleRemovingFragment(fragment);
        }
    }

    /**
     * Marks a fragment as hidden to be later animated.
     *
     * @param fragment The fragment to be shown.
     */
    void hideFragment(@Nonnull Fragment fragment) {
        if (TRACE) LOGGER.info(MARKER, "hide: " + fragment);
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
            setVisibleRemovingFragment(fragment);
        }
    }

    /**
     * Marks a fragment as shown to be later animated.
     *
     * @param fragment The fragment to be shown.
     */
    void showFragment(@Nonnull Fragment fragment) {
        if (TRACE) LOGGER.info(MARKER, "show: " + fragment);
        if (fragment.mHidden) {
            fragment.mHidden = false;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    void detachFragment(@Nonnull Fragment fragment) {
        if (TRACE) LOGGER.info(MARKER, "detach: " + fragment);
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                // We are not already in back stack, so need to remove the fragment.
                if (TRACE) LOGGER.info(MARKER, "remove from detach: " + fragment);
                mFragmentStore.removeFragment(fragment);
                if (isMenuAvailable(fragment)) {
                    mNeedMenuInvalidate = true;
                }
                setVisibleRemovingFragment(fragment);
            }
        }
    }

    void attachFragment(@Nonnull Fragment fragment) {
        if (TRACE) LOGGER.info(MARKER, "attach: " + fragment);
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                mFragmentStore.addFragment(fragment);
                if (TRACE) LOGGER.info(MARKER, "add from attach: " + fragment);
                if (isMenuAvailable(fragment)) {
                    mNeedMenuInvalidate = true;
                }
            }
        }
    }

    /**
     * Finds a fragment that was identified by the given id either when inflated
     * from XML or as the container ID when added in a transaction.  This first
     * searches through fragments that are currently added to the manager's
     * activity; if no such fragment is found, then all fragments currently
     * on the back stack associated with this ID are searched.
     *
     * @return The fragment if found or null otherwise.
     */
    @Nullable
    public Fragment findFragmentById(int id) {
        return mFragmentStore.findFragmentById(id);
    }

    /**
     * Finds a fragment that was identified by the given tag either when inflated
     * from XML or as supplied when added in a transaction.  This first
     * searches through fragments that are currently added to the manager's
     * activity; if no such fragment is found, then all fragments currently
     * on the back stack are searched.
     * <p>
     * If provided a {@code null} tag, this method returns null.
     *
     * @param tag the tag used to search for the fragment
     * @return The fragment if found or null otherwise.
     */
    @Nullable
    public Fragment findFragmentByTag(@Nullable String tag) {
        return mFragmentStore.findFragmentByTag(tag);
    }

    @Nullable
    Fragment findFragmentByWho(@Nonnull String who) {
        return mFragmentStore.findFragmentByWho(who);
    }

    @Nullable
    Fragment findActiveFragment(@Nonnull String who) {
        return mFragmentStore.findActiveFragment(who);
    }

    private void checkStateLoss() {
        if (isStateSaved()) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
    }

    /**
     * Returns {@code true} if the FragmentManager's state has already been saved
     * by its host. Any operations that would change saved state should not be performed
     * if this method returns true. For example, any popBackStack() method, such as
     * {@link #popBackStackImmediate()} or any FragmentTransaction using
     * {@link FragmentTransaction#commit()} instead of
     * {@link FragmentTransaction#commitAllowingStateLoss()} will change
     * the state and will result in an error.
     *
     * @return true if this FragmentManager's state has already been saved by its host
     */
    public boolean isStateSaved() {
        // See saveAllState() for the explanation of this.  We do this for
        // all platform versions, to keep our behavior more consistent between
        // them.
        return mStateSaved || mStopped;
    }

    /**
     * Adds an action to the queue of pending actions.
     *
     * @param action         the action to add
     * @param allowStateLoss whether to allow loss of state information
     * @throws IllegalStateException if the activity has been destroyed
     */
    void enqueueAction(@Nonnull OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            if (mHost == null) {
                if (mDestroyed) {
                    throw new IllegalStateException("FragmentManager has been destroyed");
                } else {
                    throw new IllegalStateException("FragmentManager has not been attached to a "
                            + "host.");
                }
            }
            checkStateLoss();
        }
        synchronized (mPendingActions) {
            if (mHost == null) {
                if (allowStateLoss) {
                    // This FragmentManager isn't attached, so drop the entire transaction.
                    return;
                }
                throw new IllegalStateException("Activity has been destroyed");
            }
            mPendingActions.add(action);
            scheduleCommit();
        }
    }

    /**
     * Schedules the execution when one hasn't been scheduled already. This should happen
     * the first time {@link #enqueueAction(OpGenerator, boolean)} is called or when
     * a postponed transaction has been started with
     * {@link Fragment#startPostponedEnterTransition()}
     */
    void scheduleCommit() {
        synchronized (mPendingActions) {
            boolean pendingReady = mPendingActions.size() == 1;
            if (pendingReady) {
                mHost.mHandler.removeCallbacks(mExecCommit);
                mHost.mHandler.post(mExecCommit);
                updateOnBackPressedCallbackEnabled();
            }
        }
    }

    int allocBackStackIndex() {
        return mBackStackIndex.getAndIncrement();
    }

    /**
     * Broken out from exec*, this prepares for gathering and executing operations.
     *
     * @param allowStateLoss true if state loss should be ignored or false if it should be
     *                       checked.
     */
    private void ensureExecReady(boolean allowStateLoss) {
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }

        if (mHost == null) {
            if (mDestroyed) {
                throw new IllegalStateException("FragmentManager has been destroyed");
            } else {
                throw new IllegalStateException("FragmentManager has not been attached to a host.");
            }
        }

        if (!mHost.mHandler.isCurrentThread()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }

        if (!allowStateLoss) {
            checkStateLoss();
        }

        if (mTmpRecords == null) {
            mTmpRecords = new ArrayList<>();
            mTmpIsPop = new BooleanArrayList();
        }
    }

    void execSingleAction(@Nonnull OpGenerator action, boolean allowStateLoss) {
        if (allowStateLoss && (mHost == null || mDestroyed)) {
            // This FragmentManager isn't attached, so drop the entire transaction.
            return;
        }
        ensureExecReady(allowStateLoss);
        if (action.generateOps(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();
    }

    /**
     * Broken out of exec*, this cleans up the mExecutingActions and the temporary structures
     * used in executing operations.
     */
    private void cleanupExec() {
        mExecutingActions = false;
        mTmpIsPop.clear();
        mTmpRecords.clear();
    }

    /**
     * Only call from main thread!
     */
    boolean execPendingActions(boolean allowStateLoss) {
        ensureExecReady(allowStateLoss);

        boolean didSomething = false;
        while (generateOpsForPendingActions(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
            didSomething = true;
        }

        updateOnBackPressedCallbackEnabled();
        doPendingDeferredStart();
        mFragmentStore.burpActive();

        return didSomething;
    }

    /**
     * Remove redundant BackStackRecord operations and executes them. This method merges operations
     * of proximate records that allow reordering. See
     * {@link FragmentTransaction#setReorderingAllowed(boolean)}.
     * <p>
     * For example, a transaction that adds to the back stack and then another that pops that
     * back stack record will be optimized to remove the unnecessary operation.
     * <p>
     * Likewise, two transactions committed that are executed at the same time will be optimized
     * to remove the redundant operations as well as two pop operations executed together.
     *
     * @param records     The records pending execution
     * @param isRecordPop The direction that these records are being run.
     */
    private void removeRedundantOperationsAndExecute(@Nonnull ArrayList<BackStackRecord> records,
                                                     @Nonnull BooleanArrayList isRecordPop) {
        if (records.isEmpty()) {
            return;
        }

        if (records.size() != isRecordPop.size()) {
            throw new IllegalStateException("Internal error with the back stack records");
        }

        final int numRecords = records.size();
        int startIndex = 0;
        for (int recordNum = 0; recordNum < numRecords; recordNum++) {
            final boolean canReorder = records.get(recordNum).mReorderingAllowed;
            if (!canReorder) {
                // execute all previous transactions
                if (startIndex != recordNum) {
                    executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                }
                // execute all pop operations that don't allow reordering together or
                // one add operation
                int reorderingEnd = recordNum + 1;
                if (isRecordPop.getBoolean(recordNum)) {
                    while (reorderingEnd < numRecords
                            && isRecordPop.getBoolean(reorderingEnd)
                            && !records.get(reorderingEnd).mReorderingAllowed) {
                        reorderingEnd++;
                    }
                }
                executeOpsTogether(records, isRecordPop, recordNum, reorderingEnd);
                startIndex = reorderingEnd;
                recordNum = reorderingEnd - 1;
            }
        }
        if (startIndex != numRecords) {
            executeOpsTogether(records, isRecordPop, startIndex, numRecords);
        }
    }

    /**
     * Executes a subset of a list of BackStackRecords, all of which either allow reordering or
     * do not allow ordering.
     *
     * @param records     A list of BackStackRecords that are to be executed
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex  The index of the first record in <code>records</code> to be executed
     * @param endIndex    One more than the final record index in <code>records</code> to be executed.
     */
    private void executeOpsTogether(@Nonnull ArrayList<BackStackRecord> records,
                                    @Nonnull BooleanArrayList isRecordPop, int startIndex, int endIndex) {
        final boolean allowReordering = records.get(startIndex).mReorderingAllowed;
        boolean addToBackStack = false;
        if (mTmpAddedFragments == null) {
            mTmpAddedFragments = new ArrayList<>();
        } else {
            mTmpAddedFragments.clear();
        }
        mTmpAddedFragments.addAll(mFragmentStore.getFragments());
        Fragment oldPrimaryNav = getPrimaryNavigationFragment();
        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.getBoolean(recordNum);
            if (!isPop) {
                oldPrimaryNav = record.expandOps(mTmpAddedFragments, oldPrimaryNav);
            } else {
                oldPrimaryNav = record.trackAddedFragmentsInPop(mTmpAddedFragments, oldPrimaryNav);
            }
            addToBackStack = addToBackStack || record.mAddToBackStack;
        }
        mTmpAddedFragments.clear();

        if (!allowReordering && mCurState >= Fragment.CREATED) {
            // When reordering isn't allowed, we may be operating on Fragments that haven't
            // been made active
            for (int index = startIndex; index < endIndex; index++) {
                BackStackRecord record = records.get(index);
                for (FragmentTransaction.Op op : record.mOps) {
                    Fragment fragment = op.mFragment;
                    if (fragment != null && fragment.mFragmentManager != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        mFragmentStore.makeActive(fragmentStateManager);
                    }
                }
            }
        }
        executeOps(records, isRecordPop, startIndex, endIndex);

        // The last operation determines the overall direction, this ensures that operations
        // such as push, push, pop, push are correctly considered a push
        boolean isPop = isRecordPop.getBoolean(endIndex - 1);
        // Ensure that Fragments directly affected by operations
        // are moved to their expected state in operation order
        for (int index = startIndex; index < endIndex; index++) {
            BackStackRecord record = records.get(index);
            if (isPop) {
                // Pop operations get applied in reverse order
                for (int opIndex = record.mOps.size() - 1; opIndex >= 0; opIndex--) {
                    FragmentTransaction.Op op = record.mOps.get(opIndex);
                    Fragment fragment = op.mFragment;
                    if (fragment != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        fragmentStateManager.moveToExpectedState();
                    }
                }
            } else {
                for (FragmentTransaction.Op op : record.mOps) {
                    Fragment fragment = op.mFragment;
                    if (fragment != null) {
                        FragmentStateManager fragmentStateManager =
                                createOrGetFragmentStateManager(fragment);
                        fragmentStateManager.moveToExpectedState();
                    }
                }
            }

        }
        // And only then do we move all other fragments to the current state
        moveToState(mCurState, true);
        Set<SpecialEffectsController> changedControllers = collectChangedControllers(
                records, startIndex, endIndex);
        for (SpecialEffectsController controller : changedControllers) {
            controller.updateOperationDirection(isPop);
            controller.markPostponedState();
            controller.executePendingOperations();
        }

        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final BackStackRecord record = records.get(recordNum);
            isPop = isRecordPop.getBoolean(recordNum);
            if (isPop && record.mIndex >= 0) {
                record.mIndex = -1;
            }
            record.runOnCommitRunnables();
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    }

    @Nonnull
    private Set<SpecialEffectsController> collectChangedControllers(
            @Nonnull ArrayList<BackStackRecord> records, int startIndex, int endIndex) {
        Set<SpecialEffectsController> controllers = new HashSet<>();
        for (int index = startIndex; index < endIndex; index++) {
            BackStackRecord record = records.get(index);
            for (FragmentTransaction.Op op : record.mOps) {
                Fragment fragment = op.mFragment;
                if (fragment != null) {
                    ViewGroup container = fragment.mContainer;
                    if (container != null) {
                        controllers.add(SpecialEffectsController.getOrCreateController(
                                container, this));
                    }
                }
            }
        }
        return controllers;
    }

    /**
     * Run the operations in the BackStackRecords, either to push or pop.
     *
     * @param records     The list of records whose operations should be run.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex  The index of the first entry in records to run.
     * @param endIndex    One past the index of the final entry in records to run.
     */
    private static void executeOps(@Nonnull ArrayList<BackStackRecord> records,
                                   @Nonnull BooleanArrayList isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            final BackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.getBoolean(i);
            if (isPop) {
                record.bumpBackStackNesting(-1);
                record.executePopOps();
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }

    /**
     * Set a Fragment that is visibly being removed from the screen to a tag on its container.
     * If a Fragment with the same container is already set, the previously added
     * Fragment has its exit animation updated to the correct exit animation (either exit or
     * pop_exit).
     */
    private void setVisibleRemovingFragment(@Nonnull Fragment f) {
        ViewGroup container = getFragmentContainer(f);
        if (container != null &&
                f.getEnterAnim() + f.getExitAnim() + f.getPopEnterAnim() + f.getPopExitAnim() > 0) {
            if (container.getTag(visible_removing_fragment_view_tag) == null) {
                container.setTag(visible_removing_fragment_view_tag, f);
            }
            f.setPopDirection(f.getPopDirection());
        }
    }

    @Nullable
    private ViewGroup getFragmentContainer(@Nonnull Fragment f) {
        // If there's already a container, just return it
        if (f.mContainer != null) {
            return f.mContainer;
        }
        // If the fragment has no containerId we should return null immediately.
        if (f.mContainerId <= 0) {
            return null;
        }
        // This will be false if a child fragment is added to its parent's childFragmentManager
        // before a view is created for Parent. In all other cases (adding a fragment to an
        // FragmentActivity's fragmentManager, adding a child fragment to a parent that has a view),
        // it should be true.
        if (mContainer.onHasView()) {
            View view = mContainer.onFindViewById(f.mContainerId);
            // We should handle the case where the container may not be a ViewGroup
            if (view instanceof ViewGroup) {
                return (ViewGroup) view;
            }
        }
        return null;
    }

    /**
     * Starts all postponed transactions regardless of whether they are ready or not.
     */
    private void forcePostponedTransactions() {
        Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
        for (SpecialEffectsController controller : controllers) {
            controller.forcePostponedExecutePendingOperations();
        }
    }

    /**
     * Ends the animations of fragments so that they immediately reach the end state.
     * This is used prior to saving the state so that the correct state is saved.
     */
    private void endAnimatingAwayFragments() {
        Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
        for (SpecialEffectsController controller : controllers) {
            controller.forceCompleteAllOperations();
        }
    }

    @Nonnull
    private Set<SpecialEffectsController> collectAllSpecialEffectsController() {
        Set<SpecialEffectsController> controllers = new HashSet<>();
        for (FragmentStateManager fragmentStateManager :
                mFragmentStore.getActiveFragmentStateManagers()) {
            ViewGroup container = fragmentStateManager.getFragment().mContainer;
            if (container != null) {
                controllers.add(SpecialEffectsController.getOrCreateController(container,
                        getSpecialEffectsControllerFactory()));
            }
        }
        return controllers;
    }

    /**
     * Adds all records in the pending actions to records and whether they are add or pop
     * operations to isPop. After executing, the pending actions will be empty.
     *
     * @param records All pending actions will generate BackStackRecords added to this.
     *                This contains the transactions, in order, to execute.
     * @param isPop   All pending actions will generate booleans to add to this. This contains
     *                an entry for each entry in records to indicate whether it is a
     *                pop action.
     */
    private boolean generateOpsForPendingActions(@Nonnull ArrayList<BackStackRecord> records,
                                                 @Nonnull BooleanArrayList isPop) {
        boolean didSomething = false;
        synchronized (mPendingActions) {
            if (mPendingActions.isEmpty()) {
                return false;
            }
            try {
                for (OpGenerator generator : mPendingActions) {
                    didSomething |= generator.generateOps(records, isPop);
                }
            } finally {
                // Whether generateOps succeeds or not, we clear the pending actions
                // to avoid re-processing the same set of actions a second time
                mPendingActions.clear();
                mHost.mHandler.removeCallbacks(mExecCommit);
            }
        }
        return didSomething;
    }

    private void doPendingDeferredStart() {
        if (mHavePendingDeferredStart) {
            mHavePendingDeferredStart = false;
            startPendingDeferredFragments();
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void reportBackStackChanged() {
        if (mBackStackChangeListeners != null) {
            for (int i = 0; i < mBackStackChangeListeners.size(); i++) {
                mBackStackChangeListeners.get(i).onBackStackChanged();
            }
        }
    }

    void addBackStackState(@Nonnull BackStackRecord state) {
        if (mBackStack == null) {
            mBackStack = new ArrayList<>();
        }
        mBackStack.add(state);
    }

    boolean popBackStackState(@Nonnull ArrayList<BackStackRecord> records,
                              @Nonnull BooleanArrayList isRecordPop, @Nullable String name, int id, int flags) {
        int index = findBackStackIndex(name, id, (flags & POP_BACK_STACK_INCLUSIVE) != 0);
        if (index < 0) {
            return false;
        }
        for (int i = mBackStack.size() - 1; i >= index; i--) {
            records.add(mBackStack.remove(i));
            isRecordPop.add(true);
        }
        return true;
    }

    /**
     * Find the index in the back stack associated with the given name / id.
     * <p>
     * When <code>inclusive</code> is <code>true</code>, the index of the matching record
     * will be returned. When it is <code>false</code>, the index of the record directly
     * after it will be returned. In cases where you are doing an inclusive search and
     * multiple records have the same name / id, the index returned includes all
     * consecutive matches following the first match.
     *
     * @param name      The name set via {@link FragmentTransaction#addToBackStack(String)}. Use
     *                  <code>null</code> if you do not want to search by name.
     * @param id        The id returned by {@link FragmentTransaction#commit()}. Use
     *                  <code>-1</code> if you do not want to search by id.
     * @param inclusive Whether to include the record specified by name or id.
     */
    private int findBackStackIndex(@Nullable String name, int id, boolean inclusive) {
        if (mBackStack == null || mBackStack.isEmpty()) {
            return -1;
        }
        if (name == null && id < 0) {
            if (inclusive) {
                return 0;
            } else {
                return mBackStack.size() - 1;
            }
        } else {
            // If a name or ID is specified, look for that place in
            // the stack.
            int index = mBackStack.size() - 1;
            while (index >= 0) {
                BackStackRecord bss = mBackStack.get(index);
                if (name != null && name.equals(bss.getName())) {
                    break;
                }
                if (id >= 0 && id == bss.mIndex) {
                    break;
                }
                index--;
            }
            if (index < 0) {
                return index;
            }
            if (inclusive) {
                // Consume all following entries that match.
                while (index > 0) {
                    BackStackRecord bss = mBackStack.get(index - 1);
                    if ((name != null && name.equals(bss.getName()))
                            || (id >= 0 && id == bss.mIndex)) {
                        index--;
                        continue;
                    }
                    break;
                }
            } else if (index == mBackStack.size() - 1) {
                // For a non-inclusive search, finding the last record
                // is the same as finding nothing at all since the
                // matching record itself is not included
                return -1;
            } else {
                // Non-inclusive, so skip the actual matching record
                index++;
            }
            return index;
        }
    }

    @Nonnull
    FragmentHostCallback<?> getHost() {
        return mHost;
    }

    @Nullable
    Fragment getParent() {
        return mParent;
    }

    @Nonnull
    FragmentContainer getContainer() {
        return mContainer;
    }

    @Nonnull
    FragmentStore getFragmentStore() {
        return mFragmentStore;
    }

    void attachController(@Nonnull FragmentHostCallback<?> host, @Nonnull FragmentContainer container,
                          @Nullable final Fragment parent) {
        if (mHost != null) {
            throw new IllegalStateException("Already attached");
        }
        mHost = host;
        mContainer = container;
        mParent = parent;

        if (host instanceof FragmentOnAttachListener) {
            addFragmentOnAttachListener((FragmentOnAttachListener) host);
        }

        if (mParent != null) {
            // Since the callback depends on us being the primary navigation fragment,
            // update our callback now that we have a parent so that we have the correct
            // state by default
            updateOnBackPressedCallbackEnabled();
        }
        // Set up the OnBackPressedCallback
        if (host instanceof OnBackPressedDispatcherOwner dispatcherOwner) {
            mOnBackPressedDispatcher = dispatcherOwner.getOnBackPressedDispatcher();
            LifecycleOwner owner = parent != null ? parent : dispatcherOwner;
            mOnBackPressedDispatcher.addCallback(owner, mOnBackPressedCallback);
        }

        // Get the FragmentManagerViewModel
        if (parent != null) {
            mViewModel = parent.mFragmentManager.getChildViewModel(parent);
        } else if (host instanceof ViewModelStoreOwner) {
            ViewModelStore viewModelStore = ((ViewModelStoreOwner) host).getViewModelStore();
            mViewModel = FragmentManagerViewModel.getInstance(viewModelStore);
        } else {
            throw new IllegalStateException();
        }
        // Ensure that the state is in sync with FragmentManager
        mViewModel.setIsStateSaved(isStateSaved());
        mFragmentStore.setViewModel(mViewModel);
    }

    void noteStateNotSaved() {
        // A fragment added via the <fragment> tag can have noteStateNotSaved() called
        // by its parent fragment before attachController() has been called. In this case,
        // we should early return as the state not being saved is the default.
        if (mHost == null) {
            return;
        }
        mStateSaved = false;
        mStopped = false;
        mViewModel.setIsStateSaved(false);
        for (Fragment fragment : mFragmentStore.getFragments()) {
            if (fragment != null) {
                fragment.noteStateNotSaved();
            }
        }
    }

    void dispatchAttach() {
        mStateSaved = false;
        mStopped = false;
        mViewModel.setIsStateSaved(false);
        dispatchStateChange(Fragment.ATTACHED);
    }

    void dispatchCreate() {
        mStateSaved = false;
        mStopped = false;
        mViewModel.setIsStateSaved(false);
        dispatchStateChange(Fragment.CREATED);
    }

    void dispatchViewCreated() {
        dispatchStateChange(Fragment.VIEW_CREATED);
    }

    void dispatchActivityCreated() {
        mStateSaved = false;
        mStopped = false;
        mViewModel.setIsStateSaved(false);
        dispatchStateChange(Fragment.ACTIVITY_CREATED);
    }

    void dispatchStart() {
        mStateSaved = false;
        mStopped = false;
        mViewModel.setIsStateSaved(false);
        dispatchStateChange(Fragment.STARTED);
    }

    void dispatchResume() {
        mStateSaved = false;
        mStopped = false;
        mViewModel.setIsStateSaved(false);
        dispatchStateChange(Fragment.RESUMED);
    }

    void dispatchPause() {
        dispatchStateChange(Fragment.STARTED);
    }

    void dispatchStop() {
        mStopped = true;
        mViewModel.setIsStateSaved(true);
        dispatchStateChange(Fragment.ACTIVITY_CREATED);
    }

    void dispatchDestroyView() {
        dispatchStateChange(Fragment.CREATED);
    }

    void dispatchDestroy() {
        mDestroyed = true;
        execPendingActions(true);
        endAnimatingAwayFragments();
        dispatchStateChange(Fragment.INITIALIZING);
        mHost = null;
        mContainer = null;
        mParent = null;
        if (mOnBackPressedDispatcher != null) {
            // mOnBackPressedDispatcher can hold a reference to the host,
            // so we need to null it out to prevent memory leaks
            mOnBackPressedCallback.remove();
            mOnBackPressedDispatcher = null;
        }
    }

    private void dispatchStateChange(int nextState) {
        try {
            mExecutingActions = true;
            mFragmentStore.dispatchStateChange(nextState);
            moveToState(nextState, false);
            Set<SpecialEffectsController> controllers = collectAllSpecialEffectsController();
            for (SpecialEffectsController controller : controllers) {
                controller.forceCompleteAllOperations();
            }
        } finally {
            mExecutingActions = false;
        }
        execPendingActions(true);
    }

    void setPrimaryNavigationFragment(@Nullable Fragment f) {
        if (f != null && (!f.equals(findActiveFragment(f.mWho))
                || (f.mHost != null && f.mFragmentManager != this))) {
            throw new IllegalArgumentException("Fragment " + f
                    + " is not an active fragment of FragmentManager " + this);
        }
        Fragment previousPrimaryNav = mPrimaryNav;
        mPrimaryNav = f;
        dispatchParentPrimaryNavigationFragmentChanged(previousPrimaryNav);
        dispatchParentPrimaryNavigationFragmentChanged(mPrimaryNav);
    }

    private void dispatchParentPrimaryNavigationFragmentChanged(@Nullable Fragment f) {
        if (f != null && f.equals(findActiveFragment(f.mWho))) {
            f.performPrimaryNavigationFragmentChanged();
        }
    }

    void dispatchPrimaryNavigationFragmentChanged() {
        updateOnBackPressedCallbackEnabled();
        // Dispatch the change event to this FragmentManager's primary navigation fragment
        dispatchParentPrimaryNavigationFragmentChanged(mPrimaryNav);
    }

    /**
     * Return the currently active primary navigation fragment for this FragmentManager.
     * The primary navigation fragment is set by fragment transactions using
     * {@link FragmentTransaction#setPrimaryNavigationFragment(Fragment)}.
     *
     * <p>The primary navigation fragment's
     * {@link Fragment#getChildFragmentManager() child FragmentManager} will be called first
     * to process delegated navigation actions such as {@link #popBackStack()} if no ID
     * or transaction name is provided to pop to.</p>
     *
     * @return the fragment designated as the primary navigation fragment
     */
    @Nullable
    public Fragment getPrimaryNavigationFragment() {
        return mPrimaryNav;
    }

    void setMaxLifecycle(@Nonnull Fragment f, @Nonnull Lifecycle.State state) {
        if (!f.equals(findActiveFragment(f.mWho))
                || (f.mHost != null && f.mFragmentManager != this)) {
            throw new IllegalArgumentException("Fragment " + f
                    + " is not an active fragment of FragmentManager " + this);
        }
        f.mMaxState = state;
    }

    /**
     * Set a {@link FragmentFactory} for this FragmentManager that will be used
     * to create new Fragment instances from this point onward.
     * <p>
     * The {@link Fragment#getChildFragmentManager() child FragmentManager} of all Fragments
     * in this FragmentManager will also use this factory if one is not explicitly set.
     *
     * @param fragmentFactory the factory to use to create new Fragment instances
     * @see #getFragmentFactory()
     */
    public void setFragmentFactory(@Nonnull FragmentFactory fragmentFactory) {
        mFragmentFactory = fragmentFactory;
    }

    /**
     * Gets the current {@link FragmentFactory} used to instantiate new Fragment instances.
     * <p>
     * If no factory has been explicitly set on this FragmentManager via
     * {@link #setFragmentFactory(FragmentFactory)}, the FragmentFactory of the
     * {@link Fragment#getParentFragmentManager() parent FragmentManager} will be returned.
     *
     * @return the current FragmentFactory
     */
    @Nonnull
    public FragmentFactory getFragmentFactory() {
        if (mFragmentFactory != null) {
            return mFragmentFactory;
        }
        if (mParent != null) {
            // This can't call setFragmentFactory since we need to
            // compute this each time getFragmentFactory() is called
            // so that if the parent's FragmentFactory changes, we
            // pick the change up here.
            return mParent.mFragmentManager.getFragmentFactory();
        }
        return sHostFragmentFactory;
    }

    /**
     * Set a {@link SpecialEffectsControllerFactory} for this FragmentManager that will be used
     * to create new SpecialEffectsController instances from this point onward.
     *
     * @param specialEffectsControllerFactory the factory to use to create new
     *                                        SpecialEffectsController instances.
     */
    void setSpecialEffectsControllerFactory(
            @Nonnull SpecialEffectsControllerFactory specialEffectsControllerFactory) {
        mSpecialEffectsControllerFactory = specialEffectsControllerFactory;
    }

    /**
     * Gets the current {@link SpecialEffectsControllerFactory} used to instantiate new
     * SpecialEffectsController instances.
     *
     * @return the current SpecialEffectsControllerFactory
     */
    @Nonnull
    SpecialEffectsControllerFactory getSpecialEffectsControllerFactory() {
        if (mSpecialEffectsControllerFactory != null) {
            return mSpecialEffectsControllerFactory;
        }
        if (mParent != null) {
            // This can't call setSpecialEffectsControllerFactory since we need to
            // compute this each time getSpecialEffectsControllerFactory() is called
            // so that if the parent's SpecialEffectsControllerFactory changes, we
            // pick the change up here.
            return mParent.mFragmentManager.getSpecialEffectsControllerFactory();
        }
        return sDefaultSpecialEffectsControllerFactory;
    }

    @Nonnull
    FragmentLifecycleCallbacksDispatcher getLifecycleCallbacksDispatcher() {
        return mLifecycleCallbacksDispatcher;
    }

    /**
     * Registers a {@link FragmentLifecycleCallbacks} to listen to fragment lifecycle events
     * happening in this FragmentManager. All registered callbacks will be automatically
     * unregistered when this FragmentManager is destroyed.
     *
     * @param cb        Callbacks to register
     * @param recursive true to automatically register this callback for all child FragmentManagers
     */
    public void registerFragmentLifecycleCallbacks(@Nonnull FragmentLifecycleCallbacks cb,
                                                   boolean recursive) {
        mLifecycleCallbacksDispatcher.registerFragmentLifecycleCallbacks(cb, recursive);
    }

    /**
     * Unregisters a previously registered {@link FragmentLifecycleCallbacks}. If the callback
     * was not previously registered this call has no effect. All registered callbacks will be
     * automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to unregister
     */
    public void unregisterFragmentLifecycleCallbacks(@Nonnull FragmentLifecycleCallbacks cb) {
        mLifecycleCallbacksDispatcher.unregisterFragmentLifecycleCallbacks(cb);
    }

    /**
     * Add a {@link FragmentOnAttachListener} that should receive a call to
     * {@link FragmentOnAttachListener#onAttachFragment(FragmentManager, Fragment)} when a
     * new Fragment is attached to this FragmentManager.
     *
     * @param listener Listener to add
     */
    public void addFragmentOnAttachListener(@Nonnull FragmentOnAttachListener listener) {
        mOnAttachListeners.add(listener);
    }

    /**
     * Dispatch {@link FragmentOnAttachListener#onAttachFragment(FragmentManager, Fragment)} to
     * each listener registered via {@link #addFragmentOnAttachListener(FragmentOnAttachListener)}.
     *
     * @param fragment The Fragment that was attached
     */
    void dispatchOnAttachFragment(@Nonnull Fragment fragment) {
        for (FragmentOnAttachListener listener : mOnAttachListeners) {
            listener.onAttachFragment(this, fragment);
        }
    }

    /**
     * Remove a {@link FragmentOnAttachListener} that was previously added via
     * {@link #addFragmentOnAttachListener(FragmentOnAttachListener)}. It will no longer
     * get called when a new Fragment is attached.
     *
     * @param listener Listener to remove
     */
    public void removeFragmentOnAttachListener(@Nonnull FragmentOnAttachListener listener) {
        mOnAttachListeners.remove(listener);
    }

    void dispatchOnHiddenChanged() {
        for (Fragment fragment : mFragmentStore.getActiveFragments()) {
            if (fragment != null) {
                fragment.onHiddenChanged(fragment.isHidden());
                fragment.mChildFragmentManager.dispatchOnHiddenChanged();
            }
        }
    }

    // Checks if fragments that belong to this fragment manager (or their children) have menus,
    // and if they are visible.
    boolean checkForMenus() {
        boolean hasMenu = false;
        for (Fragment fragment : mFragmentStore.getActiveFragments()) {
            if (fragment != null) {
                hasMenu = isMenuAvailable(fragment);
            }
            if (hasMenu) {
                return true;
            }
        }
        return false;
    }

    private boolean isMenuAvailable(@Nonnull Fragment f) {
        return (f.mHasMenu && f.mMenuVisible) || f.mChildFragmentManager.checkForMenus();
    }

    void invalidateMenuForFragment(@Nonnull Fragment f) {
        if (f.mAdded && isMenuAvailable(f)) {
            mNeedMenuInvalidate = true;
        }
    }

    static int reverseTransit(int transit) {
        return switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN -> FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE -> FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE -> FragmentTransaction.TRANSIT_FRAGMENT_FADE;
            case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN -> FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE;
            case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE -> FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN;
            default -> 0;
        };
    }

    /**
     * An add or pop transaction to be scheduled for the UI thread.
     */
    interface OpGenerator {
        /**
         * Generate transactions to add to {@code records} and whether the transaction is
         * an add or pop to {@code isRecordPop}.
         * <p>
         * records and isRecordPop must be added equally so that each transaction in records
         * matches the boolean for whether it is a pop in isRecordPop.
         *
         * @param records     A list to add transactions to.
         * @param isRecordPop A list to add whether the transactions added to <code>records</code> is
         *                    a pop transaction.
         * @return true if something was added or false otherwise.
         */
        boolean generateOps(@Nonnull ArrayList<BackStackRecord> records,
                            @Nonnull BooleanArrayList isRecordPop);
    }

    /**
     * A pop operation OpGenerator. This will be run on the UI thread and will generate the
     * transactions that will be popped if anything can be popped.
     */
    private class PopBackStackState implements OpGenerator {

        final String mName;
        final int mId;
        final int mFlags;

        PopBackStackState(@Nullable String name, int id, int flags) {
            mName = name;
            mId = id;
            mFlags = flags;
        }

        @Override
        public boolean generateOps(@Nonnull ArrayList<BackStackRecord> records,
                                   @Nonnull BooleanArrayList isRecordPop) {
            if (mPrimaryNav != null // We have a primary nav fragment
                    && mId < 0 // No valid id (since they're local)
                    && mName == null) { // no name to pop to (since they're local)
                final FragmentManager childManager = mPrimaryNav.getChildFragmentManager();
                if (childManager.popBackStackImmediate()) {
                    // We didn't add any operations for this FragmentManager even though
                    // a child did do work.
                    return false;
                }
            }
            return popBackStackState(records, isRecordPop, mName, mId, mFlags);
        }
    }
}
