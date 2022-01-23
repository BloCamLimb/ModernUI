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

import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Callback interface for listening to fragment state changes that happen
 * within a given FragmentManager.
 */
@SuppressWarnings("unused")
public interface FragmentLifecycleCallbacks {
    /**
     * Called right before the fragment's {@link Fragment#onAttach()} method is called.
     * This is a good time to inject any required dependencies or perform other configuration
     * for the fragment before any of the fragment's lifecycle methods are invoked.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentPreAttached(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has been attached to its host. Its host will have had
     * <code>onAttachFragment</code> called before this call happens.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentAttached(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called right before the fragment's {@link Fragment#onCreate(DataSet)} method is called.
     * This is a good time to inject any required dependencies or perform other configuration
     * for the fragment.
     *
     * @param fm                 Host FragmentManager
     * @param f                  Fragment changing state
     * @param savedInstanceState Saved instance bundle from a previous instance
     */
    default void onFragmentPreCreated(@Nonnull FragmentManager fm, @Nonnull Fragment f,
                                      @Nullable DataSet savedInstanceState) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onCreate(DataSet)}. This will only happen once for any given
     * fragment instance, though the fragment may be attached and detached multiple times.
     *
     * @param fm                 Host FragmentManager
     * @param f                  Fragment changing state
     * @param savedInstanceState Saved instance bundle from a previous instance
     */
    default void onFragmentCreated(@Nonnull FragmentManager fm, @Nonnull Fragment f,
                                   @Nullable DataSet savedInstanceState) {
    }

    /**
     * Called after the fragment has returned a non-null view from the FragmentManager's
     * request to {@link Fragment#onCreateView(ViewGroup, DataSet)}.
     *
     * @param fm                 Host FragmentManager
     * @param f                  Fragment that created and owns the view
     * @param v                  View returned by the fragment
     * @param savedInstanceState Saved instance bundle from a previous instance
     */
    default void onFragmentViewCreated(@Nonnull FragmentManager fm, @Nonnull Fragment f,
                                       @Nonnull View v, @Nullable DataSet savedInstanceState) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onStart()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentStarted(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onResume()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentResumed(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onPause()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentPaused(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onStop()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentStopped(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onSaveInstanceState(DataSet)}.
     *
     * @param fm       Host FragmentManager
     * @param f        Fragment changing state
     * @param outState Saved state bundle for the fragment
     */
    default void onFragmentSaveInstanceState(@Nonnull FragmentManager fm, @Nonnull Fragment f,
                                             @Nonnull DataSet outState) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onDestroyView()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentViewDestroyed(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onDestroy()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentDestroyed(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }

    /**
     * Called after the fragment has returned from the FragmentManager's call to
     * {@link Fragment#onDetach()}.
     *
     * @param fm Host FragmentManager
     * @param f  Fragment changing state
     */
    default void onFragmentDetached(@Nonnull FragmentManager fm, @Nonnull Fragment f) {
    }
}
