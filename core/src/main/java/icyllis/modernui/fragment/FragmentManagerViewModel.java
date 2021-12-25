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
import icyllis.modernui.lifecycle.ViewModelProvider;
import icyllis.modernui.lifecycle.ViewModelStore;
import org.apache.logging.log4j.Marker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * FragmentManagerViewModel is the always up-to-date view of the Fragment's
 * non configuration state
 */
final class FragmentManagerViewModel extends ViewModel {

    private static final Marker MARKER = FragmentManager.MARKER;

    private static final ViewModelProvider.Factory FACTORY = new ViewModelProvider.Factory() {
        @Nonnull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@Nonnull Class<T> modelClass) {
            FragmentManagerViewModel viewModel = new FragmentManagerViewModel();
            return (T) viewModel;
        }
    };

    @Nonnull
    static FragmentManagerViewModel getInstance(ViewModelStore viewModelStore) {
        return new ViewModelProvider(viewModelStore, FACTORY).get(FragmentManagerViewModel.class);
    }

    private final HashMap<String, Fragment> mRetainedFragments = new HashMap<>();
    private final HashMap<String, FragmentManagerViewModel> mChildViewModels = new HashMap<>();
    private final HashMap<String, ViewModelStore> mViewModelStores = new HashMap<>();

    // Only used when mStateAutomaticallySaved is true
    private boolean mHasBeenCleared = false;

    // Flag set by the FragmentManager to indicate when we should allow
    // changes to the set of retained fragments
    private boolean mIsStateSaved = false;

    /**
     * FragmentManagerViewModel assumed that the ViewModel is added to
     * an appropriate {@link ViewModelStore} that has the same lifecycle as the
     * FragmentManager and that {@link #onCleared()} indicates that the Fragment's host
     * is being permanently destroyed
     */
    FragmentManagerViewModel() {
    }

    /**
     * Set whether the FragmentManager has saved its state
     *
     * @param isStateSaved Whether the FragmentManager has saved its state
     */
    void setIsStateSaved(boolean isStateSaved) {
        mIsStateSaved = isStateSaved;
    }

    @Override
    protected void onCleared() {
        if (FragmentManager.DEBUG) {
            LOGGER.debug(MARKER, "onCleared called for " + this);
        }
        mHasBeenCleared = true;
    }

    boolean isCleared() {
        return mHasBeenCleared;
    }

    void addRetainedFragment(@Nonnull Fragment fragment) {
        if (!mIsStateSaved) {
            mRetainedFragments.putIfAbsent(fragment.mWho, fragment);
        }
    }

    @Nullable
    Fragment findRetainedFragmentByWho(@Nonnull String who) {
        return mRetainedFragments.get(who);
    }

    @Nonnull
    Collection<Fragment> getRetainedFragments() {
        return new ArrayList<>(mRetainedFragments.values());
    }

    boolean shouldDestroy(@Nonnull Fragment fragment) {
        return mHasBeenCleared || !mRetainedFragments.containsKey(fragment.mWho);
    }

    void removeRetainedFragment(@Nonnull Fragment fragment) {
        if (!mIsStateSaved) {
            mRetainedFragments.remove(fragment.mWho);
        }
    }

    @Nonnull
    FragmentManagerViewModel getChildViewModel(@Nonnull Fragment f) {
        return mChildViewModels.computeIfAbsent(f.mWho, i -> new FragmentManagerViewModel());
    }

    @Nonnull
    ViewModelStore getViewModelStore(@Nonnull Fragment f) {
        return mViewModelStores.computeIfAbsent(f.mWho, i -> new ViewModelStore());
    }

    void clearViewModelState(@Nonnull Fragment f) {
        // Clear and remove the Fragment's child non config state
        FragmentManagerViewModel childViewModel = mChildViewModels.remove(f.mWho);
        if (childViewModel != null) {
            childViewModel.onCleared();
        }
        // Clear and remove the Fragment's ViewModelStore
        ViewModelStore viewModelStore = mViewModelStores.remove(f.mWho);
        if (viewModelStore != null) {
            viewModelStore.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FragmentManagerViewModel that = (FragmentManagerViewModel) o;

        return mRetainedFragments.equals(that.mRetainedFragments)
                && mChildViewModels.equals(that.mChildViewModels)
                && mViewModelStores.equals(that.mViewModelStores);
    }

    @Override
    public int hashCode() {
        int result = mRetainedFragments.hashCode();
        result = 31 * result + mChildViewModels.hashCode();
        result = 31 * result + mViewModelStores.hashCode();
        return result;
    }

    @Nonnull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("FragmentManagerViewModel{");
        s.append(Integer.toHexString(super.hashCode()));
        s.append("} Fragments (");
        Iterator<Fragment> fragmentIterator = mRetainedFragments.values().iterator();
        while (fragmentIterator.hasNext()) {
            s.append(fragmentIterator.next());
            if (fragmentIterator.hasNext()) {
                s.append(',').append(' ');
            }
        }
        s.append(") ChildViewModels (");
        Iterator<String> it = mChildViewModels.keySet().iterator();
        while (it.hasNext()) {
            s.append(it.next());
            if (it.hasNext()) {
                s.append(',').append(' ');
            }
        }
        s.append(") ViewModelStores (");
        it = mViewModelStores.keySet().iterator();
        while (it.hasNext()) {
            s.append(it.next());
            if (it.hasNext()) {
                s.append(',').append(' ');
            }
        }
        s.append(')');
        return s.toString();
    }
}
