/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.fragment;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.lifecycle.*;

class FragmentViewLifecycleOwner implements LifecycleOwner, ViewModelStoreOwner {

    private final Fragment mFragment;
    private final ViewModelStore mViewModelStore;

    private ViewModelProvider.Factory mDefaultFactory;

    private LifecycleRegistry mLifecycleRegistry;

    FragmentViewLifecycleOwner(@NonNull Fragment fragment, @NonNull ViewModelStore viewModelStore) {
        mFragment = fragment;
        mViewModelStore = viewModelStore;
    }

    /**
     * Initializes the underlying Lifecycle if it hasn't already been created.
     */
    void initialize() {
        if (mLifecycleRegistry == null) {
            mLifecycleRegistry = new LifecycleRegistry(this);
        }
    }

    /**
     * @return True if the Lifecycle has been initialized.
     */
    boolean isInitialized() {
        return mLifecycleRegistry != null;
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        initialize();
        return mLifecycleRegistry;
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        initialize();
        return mViewModelStore;
    }

    void setCurrentState(@NonNull Lifecycle.State state) {
        mLifecycleRegistry.setCurrentState(state);
    }

    void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
        mLifecycleRegistry.handleLifecycleEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        ViewModelProvider.Factory currentFactory =
                mFragment.getDefaultViewModelProviderFactory();

        if (!currentFactory.equals(mFragment.mDefaultFactory)) {
            mDefaultFactory = currentFactory;
            return currentFactory;
        }

        return mDefaultFactory;
    }
}
