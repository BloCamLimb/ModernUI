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

import icyllis.modernui.lifecycle.ViewModel;
import icyllis.modernui.lifecycle.ViewModelStore;
import icyllis.modernui.lifecycle.ViewModelStoreOwner;
import icyllis.modernui.util.DataSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Provides integration points with a {@link FragmentManager} for a fragment host.
 * <p>
 * It is the responsibility of the host to take care of the Fragment's lifecycle.
 * The methods provided by {@link FragmentController} are for that purpose.
 *
 * @see #createController(FragmentHostCallback)
 */
public final class FragmentController {

    @Nonnull
    private final FragmentHostCallback<?> mHost;

    /**
     * Returns a {@link FragmentController}.
     */
    @Nonnull
    public static FragmentController createController(@Nonnull FragmentHostCallback<?> callbacks) {
        return new FragmentController(Objects.requireNonNull(callbacks));
    }

    private FragmentController(@Nonnull FragmentHostCallback<?> callbacks) {
        mHost = callbacks;
    }

    /**
     * Returns a {@link FragmentManager} for this controller.
     */
    @Nonnull
    public FragmentManager getFragmentManager() {
        return mHost.mFragmentManager;
    }

    /**
     * Returns a fragment with the given identifier.
     */
    @Nullable
    public Fragment findFragmentByWho(@Nonnull String who) {
        return mHost.mFragmentManager.findFragmentByWho(who);
    }

    /**
     * Returns the number of active fragments.
     */
    public int getActiveFragmentCount() {
        return mHost.mFragmentManager.getActiveFragmentCount();
    }

    /**
     * Returns the list of active fragments.
     */
    @Nonnull
    public List<Fragment> getActiveFragments() {
        return mHost.mFragmentManager.getActiveFragments();
    }

    /**
     * Attaches the host to the FragmentManager for this controller. The host must be
     * attached before the FragmentManager can be used to manage Fragments.
     */
    public void attachHost(@Nullable Fragment parent) {
        mHost.mFragmentManager.attachController(mHost, mHost, parent);
    }

    /**
     * Marks the fragment state as unsaved. This allows for "state loss" detection.
     */
    public void noteStateNotSaved() {
        mHost.mFragmentManager.noteStateNotSaved();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the create state.
     * <p>Call when Fragments should be created.
     *
     * @see Fragment#onCreate(DataSet)
     */
    public void dispatchCreate() {
        mHost.mFragmentManager.dispatchCreate();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the activity created state.
     * <p>Call when Fragments should be informed their host has been created.
     */
    public void dispatchActivityCreated() {
        mHost.mFragmentManager.dispatchActivityCreated();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the start state.
     * <p>Call when Fragments should be started.
     *
     * @see Fragment#onStart()
     */
    public void dispatchStart() {
        mHost.mFragmentManager.dispatchStart();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the resume state.
     * <p>Call when Fragments should be resumed.
     *
     * @see Fragment#onResume()
     */
    public void dispatchResume() {
        mHost.mFragmentManager.dispatchResume();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the pause state.
     * <p>Call when Fragments should be paused.
     *
     * @see Fragment#onPause()
     */
    public void dispatchPause() {
        mHost.mFragmentManager.dispatchPause();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the stop state.
     * <p>Call when Fragments should be stopped.
     *
     * @see Fragment#onStop()
     */
    public void dispatchStop() {
        mHost.mFragmentManager.dispatchStop();
    }

    /**
     * Moves all Fragments managed by the controller's FragmentManager
     * into the destroy view state.
     * <p>Call when the Fragment's views should be destroyed.
     *
     * @see Fragment#onDestroyView()
     */
    public void dispatchDestroyView() {
        mHost.mFragmentManager.dispatchDestroyView();
    }

    /**
     * Moves Fragments managed by the controller's FragmentManager
     * into the destroy state.
     * <p>
     * If the {@link FragmentHostCallback} is an instance of {@link ViewModelStoreOwner},
     * then retained Fragments and any other non configuration state such as any
     * {@link ViewModel} attached to Fragments will only be destroyed if
     * {@link ViewModelStore#clear()} is called prior to this method.
     * <p>Call when Fragments should be destroyed.
     *
     * @see Fragment#onDestroy()
     */
    public void dispatchDestroy() {
        mHost.mFragmentManager.dispatchDestroy();
    }

    /**
     * Execute any pending actions for the Fragments managed by the
     * controller's FragmentManager.
     * <p>Call when queued actions can be performed [eg when the
     * Fragment moves into a start or resume state].
     *
     * @return {@code true} if queued actions were performed
     */
    public boolean execPendingActions() {
        return mHost.mFragmentManager.execPendingActions(true);
    }
}
