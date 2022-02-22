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

package icyllis.modernui.widget;

import icyllis.modernui.view.MenuItem;
import icyllis.modernui.view.menu.MenuBuilder;

import javax.annotation.Nonnull;

/**
 * An interface notified when a menu item is hovered. Useful for cases when hover should trigger
 * some behavior at a higher level, like managing the opening and closing of submenus.
 */
public interface MenuItemHoverListener {
    
    /**
     * Called when hover exits a menu item.
     * <p>
     * If hover is moving to another item, this method will be called before
     * {@link #onItemHoverEnter(MenuBuilder, MenuItem)} for the newly-hovered item.
     *
     * @param menu the item's parent menu
     * @param item the hovered menu item
     */
    void onItemHoverExit(@Nonnull MenuBuilder menu, @Nonnull MenuItem item);

    /**
     * Called when hover enters a menu item.
     *
     * @param menu the item's parent menu
     * @param item the hovered menu item
     */
    void onItemHoverEnter(@Nonnull MenuBuilder menu, @Nonnull MenuItem item);
}
