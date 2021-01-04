/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

import icyllis.modernui.lifecycle.ILifecycleOwner;
import icyllis.modernui.lifecycle.IViewModelStoreOwner;
import icyllis.modernui.lifecycle.Lifecycle;
import icyllis.modernui.lifecycle.ViewModelStore;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewRootImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Part of a UI, used to manage specified view creation and its logic
 * A UI can contain multiple fragments, which are controlled by UIManager
 * to determine whether they are instantiated and enabled.
 * Different fragments can communicate with each other,
 * and there can be transition animation when switching etc.
 */
public class Fragment implements ILifecycleOwner, IViewModelStoreOwner {

    // Internal unique identifier
    @Nonnull
    UUID mUUID = UUID.randomUUID();

    // The fragment manager we are associated with.  Set as soon as the
    // fragment is used in a transaction; cleared after it has been removed
    // from all transactions.
    FragmentManagerImpl mFragmentManager;

    // Private fragment manager for child fragments inside of this one.
    @Nonnull
    FragmentManagerImpl mChildFragmentManager = new FragmentManagerImpl();

    // If this Fragment is contained in another Fragment, this is that container.
    @Nullable
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

    // The View generated for this fragment.
    @Nullable
    View mView;

    Lifecycle mLifecycle;

    @Nullable
    FragmentViewLifecycleOwner mViewLifecycleOwner;

    /**
     * Create the view belong to this fragment.
     * <p>
     * If this fragment is main fragment of a UI, this method
     * should create the main view of the UI, and can't be null.
     *
     * @return view instance or null
     */
    @Nullable
    public View onCreateView() {
        return null;
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
     * Returns the {@link ViewModelStore} associated with this Fragment
     *
     * @return a {@code ViewModelStore}
     * @throws IllegalStateException if called before the Fragment is attached i.e., before
     * onAttach().
     */
    @Nonnull
    @Override
    public final ViewModelStore getViewModelStore() {
        if (mFragmentManager == null) {
            throw new IllegalStateException("Can't access ViewModels from detached fragment");
        }
        return mFragmentManager.getViewModelStore();
    }

    /**
     * Returns the parent Fragment containing this Fragment. If this Fragment
     * is attached directly to an Activity, returns null.
     */
    @Nullable
    public final Fragment getParentFragment() {
        return mParentFragment;
    }

    @Nonnull
    @Override
    public final Lifecycle getLifecycle() {
        return mLifecycle;
    }

    /**
     * Get a {@link ILifecycleOwner} that represents the {@link #getView() Fragment's View}
     * lifecycle. In most cases, this mirrors the lifecycle of the Fragment itself, but in cases
     * of {@link FragmentTransaction#detach(Fragment) detached} Fragments, the lifecycle of the
     * Fragment can be considerably longer than the lifecycle of the View itself.
     *
     * @return A {@link ILifecycleOwner} that represents the {@link #getView() Fragment's View}
     * lifecycle.
     * @throws IllegalStateException if the {@link #getView() Fragment's View is null}.
     */
    @Nonnull
    public final ILifecycleOwner getViewLifecycleOwner() {
        if (mViewLifecycleOwner == null) {
            throw new IllegalStateException("Can't access the Fragment View's LifecycleOwner when "
                    + "the View of this Fragment is currently unavailable");
        }
        return mViewLifecycleOwner;
    }
}
