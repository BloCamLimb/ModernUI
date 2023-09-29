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

package icyllis.modernui.view;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.widget.AdapterView;

/**
 * Extension of {@link Menu} for context menus providing functionality to modify
 * the header of the context menu.
 * <p>
 * Context menus do not support item shortcuts and item icons.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about creating menus, read the
 * <a href="https://developer.android.com/guide/topics/ui/menus">Menus</a> developer guide.</p>
 * </div>
 */
public interface ContextMenu extends Menu {

    /**
     * Sets the context menu header's title to the title given in <var>title</var>.
     *
     * @param title The character sequence used for the title.
     * @return This ContextMenu so additional setters can be called.
     */
    @NonNull
    ContextMenu setHeaderTitle(CharSequence title);

    /**
     * Sets the context menu header's icon to the icon given in <var>icon</var>
     * {@link Drawable}.
     *
     * @param icon The {@link Drawable} used for the icon.
     * @return This ContextMenu so additional setters can be called.
     */
    @NonNull
    ContextMenu setHeaderIcon(Drawable icon);

    /**
     * Sets the header of the context menu to the {@link View} given in
     * <var>view</var>. This replaces the header title and icon (and those
     * replace this).
     *
     * @param view The {@link View} used for the header.
     * @return This ContextMenu so additional setters can be called.
     */
    @NonNull
    ContextMenu setHeaderView(View view);

    /**
     * Clears the header of the context menu.
     */
    void clearHeader();

    /**
     * Additional information regarding the creation of the context menu.  For example,
     * {@link AdapterView}s use this to pass the exact item position within the adapter
     * that initiated the context menu.
     */
    interface ContextMenuInfo {
    }
}
