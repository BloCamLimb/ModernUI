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

import icyllis.modernui.R;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewParent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Accessors for finding a view tree-local {@link LifecycleOwner} that reports the lifecycle for
 * the given view.
 */
public class ViewTreeLifecycleOwner {

    private ViewTreeLifecycleOwner() {
        // No instances
    }

    /**
     * Set the {@link LifecycleOwner} responsible for managing the given {@link View}.
     * Calls to {@link #get(View)} from this view or descendants will return {@code lifecycleOwner}.
     *
     * <p>This should only be called by constructs such as activities or fragments that manage
     * a view tree and reflect their own lifecycle through a {@link LifecycleOwner}. Callers
     * should only set a {@link LifecycleOwner} that will be <em>stable.</em> The associated
     * lifecycle should report that it is destroyed if the view tree is removed and is not
     * guaranteed to later become reattached to a window.</p>
     *
     * @param view           Root view managed by lifecycleOwner
     * @param lifecycleOwner LifecycleOwner representing the manager of the given view
     */
    public static void set(@Nonnull View view, @Nullable LifecycleOwner lifecycleOwner) {
        view.setTag(R.id.view_tree_lifecycle_owner, lifecycleOwner);
    }

    /**
     * Retrieve the {@link LifecycleOwner} responsible for managing the given {@link View}.
     * This may be used to scope work or heavyweight resources associated with the view
     * that may span cycles of the view becoming detached and reattached from a window.
     *
     * @param view View to fetch a {@link LifecycleOwner} for
     * @return The {@link LifecycleOwner} responsible for managing this view and/or some subset
     * of its ancestors
     */
    @Nullable
    public static LifecycleOwner get(@Nonnull View view) {
        LifecycleOwner found = (LifecycleOwner) view.getTag(R.id.view_tree_lifecycle_owner);
        if (found != null) return found;
        ViewParent parent = view.getParent();
        while (found == null && parent instanceof final View parentView) {
            found = (LifecycleOwner) parentView.getTag(R.id.view_tree_lifecycle_owner);
            parent = parentView.getParent();
        }
        return found;
    }
}
