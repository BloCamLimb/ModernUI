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

package icyllis.modernui.view.menu;

import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nullable;

/**
 * A MenuPresenter is responsible for building views for a Menu object.
 * It takes over some responsibility from the old style monolithic MenuBuilder class.
 */
public interface MenuPresenter {

    /**
     * Called by menu implementation to notify another component of open/close events.
     */
    interface Callback {

        /**
         * Called when a menu is closing.
         */
        void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing);

        /**
         * Called when a submenu opens. Useful for notifying the application
         * of menu state so that it does not attempt to hide the action bar
         * while a submenu is open or similar.
         *
         * @param subMenu Submenu currently being opened
         * @return true if the Callback will handle presenting the submenu, false if
         * the presenter should attempt to do so.
         */
        boolean onOpenSubMenu(MenuBuilder subMenu);
    }

    /**
     * Initializes this presenter for the given context and menu.
     * <p>
     * This method is called by MenuBuilder when a presenter is added. See
     * {@link MenuBuilder#addMenuPresenter(MenuPresenter)}.
     *
     * @param menu the menu to host, or {@code null} to clear the hosted menu
     */
    void initForMenu(@Nullable MenuBuilder menu);

    /**
     * Retrieve a MenuView to display the menu specified in
     * {@link #initForMenu(MenuBuilder)}.
     *
     * @param root Intended parent of the MenuView.
     * @return A freshly created MenuView.
     */
    MenuView getMenuView(ViewGroup root);

    /**
     * Update the menu UI in response to a change. Called by
     * MenuBuilder during the normal course of operation.
     *
     * @param cleared true if the menu was entirely cleared
     */
    void updateMenuView(boolean cleared);

    /**
     * Set a callback object that will be notified of menu events
     * related to this specific presentation.
     *
     * @param cb Callback that will be notified of future events
     */
    void setCallback(Callback cb);

    /**
     * Called by Menu implementations to indicate that a submenu item
     * has been selected. An active Callback should be notified, and
     * if applicable the presenter should present the submenu.
     *
     * @param subMenu SubMenu being opened
     * @return true if the event was handled, false otherwise.
     */
    boolean onSubMenuSelected(SubMenuBuilder subMenu);

    /**
     * Called by Menu implementations to indicate that a menu or submenu is
     * closing. Presenter implementations should close the representation
     * of the menu indicated as necessary and notify a registered callback.
     *
     * @param menu               the menu or submenu that is closing
     * @param allMenusAreClosing {@code true} if all displayed menus and
     *                           submenus are closing, {@code false} if only
     *                           the specified menu is closing
     */
    void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing);

    /**
     * Called by Menu implementations to flag items that will be shown as actions.
     *
     * @return true if this presenter changed the action status of any items.
     */
    boolean flagActionItems();

    /**
     * Called when a menu item with a collapsable action view should expand its action view.
     *
     * @param menu Menu containing the item to be expanded
     * @param item Item to be expanded
     * @return true if this presenter expanded the action view, false otherwise.
     */
    boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item);

    /**
     * Called when a menu item with a collapsable action view should collapse its action view.
     *
     * @param menu Menu containing the item to be collapsed
     * @param item Item to be collapsed
     * @return true if this presenter collapsed the action view, false otherwise.
     */
    boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item);

    /**
     * Returns an ID for determining how to save/restore instance state.
     *
     * @return a valid ID value.
     */
    int getId();
}
