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

import icyllis.modernui.graphics.drawable.Drawable;

/**
 * Subclass of {@link Menu} for sub menus.
 * <p>
 * Sub menus do not support item icons, or nested sub menus.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about creating menus, read the
 * <a href="https://developer.android.com/guide/topics/ui/menus.html">Menus</a> developer guide.</p>
 * </div>
 */
public interface SubMenu extends Menu {

    /**
     * Sets the submenu header's title to the title given in <var>title</var>.
     *
     * @param title The character sequence used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    SubMenu setHeaderTitle(CharSequence title);

    /**
     * Sets the submenu header's icon to the icon given in <var>icon</var>
     * {@link Drawable}.
     *
     * @param icon The {@link Drawable} used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    SubMenu setHeaderIcon(Drawable icon);

    /**
     * Sets the header of the submenu to the {@link View} given in
     * <var>view</var>. This replaces the header title and icon (and those
     * replace this).
     *
     * @param view The {@link View} used for the header.
     * @return This SubMenu so additional setters can be called.
     */
    SubMenu setHeaderView(View view);

    /**
     * Clears the header of the submenu.
     */
    void clearHeader();

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     *
     * @param icon The new icon (as a Drawable) to be displayed.
     * @return This SubMenu so additional setters can be called.
     * @see MenuItem#setIcon(Drawable)
     */
    SubMenu setIcon(Drawable icon);

    /**
     * Gets the {@link MenuItem} that represents this submenu in the parent
     * menu.  Use this for setting additional item attributes.
     *
     * @return The {@link MenuItem} that launches the submenu when invoked.
     */
    MenuItem getItem();
}
