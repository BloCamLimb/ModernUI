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

package icyllis.modernui.lifecycle;

import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewParent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Accessors for finding a view tree-local {@link ViewModelStoreOwner} that allows access to a
 * {@link ViewModelStore} for the given view.
 */
public class ViewTreeViewModelStoreOwner {

    private static final int view_tree_view_model_store_owner = 0x03020002;

    private ViewTreeViewModelStoreOwner() {
        // No instances
    }

    /**
     * Set the {@link ViewModelStoreOwner} associated with the given {@link View}.
     * Calls to {@link #get(View)} from this view or descendants will return
     * {@code viewModelStoreOwner}.
     *
     * <p>This should only be called by constructs such as activities or fragments that manage
     * a view tree and retain state through a {@link ViewModelStoreOwner}. Callers
     * should only set a {@link ViewModelStoreOwner} that will be <em>stable.</em> The associated
     * {@link ViewModelStore} should be cleared if the view tree is removed and is not
     * guaranteed to later become reattached to a window.</p>
     *
     * @param view                Root view associated with the viewModelStoreOwner
     * @param viewModelStoreOwner ViewModelStoreOwner associated with the given view
     */
    public static void set(@Nonnull View view, @Nullable ViewModelStoreOwner viewModelStoreOwner) {
        view.setTag(view_tree_view_model_store_owner, viewModelStoreOwner);
    }

    /**
     * Retrieve the {@link ViewModelStoreOwner} associated with the given {@link View}.
     * This may be used to retain state associated with this view across configuration changes.
     *
     * @param view View to fetch a {@link ViewModelStoreOwner} for
     * @return The {@link ViewModelStoreOwner} associated with this view and/or some subset
     * of its ancestors
     */
    @Nullable
    public static ViewModelStoreOwner get(@Nonnull View view) {
        ViewModelStoreOwner found = (ViewModelStoreOwner) view.getTag(view_tree_view_model_store_owner);
        if (found != null) return found;
        ViewParent parent = view.getParent();
        while (found == null && parent instanceof final View parentView) {
            found = (ViewModelStoreOwner) parentView.getTag(view_tree_view_model_store_owner);
            parent = parentView.getParent();
        }
        return found;
    }
}
