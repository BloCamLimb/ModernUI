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

package icyllis.modernui.lifecycle;

import icyllis.modernui.annotation.NonNull;

/**
 * A scope that owns {@link ViewModelStore}.
 * <p>
 * A responsibility of an implementation of this interface is to retain owned ViewModelStore
 * during the configuration changes and call {@link ViewModelStore#clear()}, when this scope is
 * going to be destroyed.
 */
public interface ViewModelStoreOwner {

    /**
     * Returns the {@link ViewModelStore} of the provider.
     *
     * @return a {@code ViewModelStore}
     */
    @NonNull
    ViewModelStore getViewModelStore();

    /**
     * Returns the default {@link ViewModelProvider.Factory} that should be
     * used when no custom {@code Factory} is provided to the
     * {@link ViewModelProvider} constructors, such as
     * {@link ViewModelProvider#ViewModelProvider(ViewModelStoreOwner)}.
     *
     * @return a {@code ViewModelProvider.Factory}
     */
    default ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        return null;
    }
}
