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

import javax.annotation.Nonnull;

class FragmentStateManager {

    private final FragmentLifecycleCallbacksDispatcher mDispatcher;
    private final FragmentStore mFragmentStore;
    @Nonnull
    private final Fragment mFragment;

    private boolean mMovingToState = false;
    private int mFragmentManagerState = Fragment.INITIALIZING;

    /**
     * Create a FragmentStateManager from a brand new Fragment instance.
     *
     * @param dispatcher Dispatcher for any lifecycle callbacks triggered by this class
     * @param fragmentStore FragmentStore handling all Fragments
     * @param fragment The Fragment to manage
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

    void moveToExpectedState() {

    }
}
