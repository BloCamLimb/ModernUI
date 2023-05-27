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

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.view.Menu;
import icyllis.modernui.view.MenuItem;
import icyllis.modernui.view.SubMenu;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;

/**
 * The model for a sub menu, which is an extension of the menu.  Most methods are proxied to
 * the parent menu.
 */
public class SubMenuBuilder extends MenuBuilder implements SubMenu {

    private final MenuBuilder mParentMenu;
    private final MenuItemImpl mItem;

    public SubMenuBuilder(Context context, MenuBuilder parentMenu, MenuItemImpl item) {
        super(context);
        mParentMenu = parentMenu;
        mItem = item;
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        mParentMenu.setQwertyMode(isQwerty);
    }

    @Override
    public boolean isQwertyMode() {
        return mParentMenu.isQwertyMode();
    }

    @Override
    public void setShortcutsVisible(boolean shortcutsVisible) {
        mParentMenu.setShortcutsVisible(shortcutsVisible);
    }

    @Override
    public boolean isShortcutsVisible() {
        return mParentMenu.isShortcutsVisible();
    }

    public Menu getParentMenu() {
        return mParentMenu;
    }

    @Nonnull
    @Override
    public MenuItem getItem() {
        return mItem;
    }

    @Override
    public void setCallback(Callback callback) {
        mParentMenu.setCallback(callback);
    }

    @Override
    public MenuBuilder getRootMenu() {
        return mParentMenu.getRootMenu();
    }

    @Override
    boolean dispatchMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return super.dispatchMenuItemSelected(menu, item) ||
                mParentMenu.dispatchMenuItemSelected(menu, item);
    }

    @Nonnull
    @Override
    public SubMenu setIcon(Drawable icon) {
        mItem.setIcon(icon);
        return this;
    }

    @Nonnull
    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        return (SubMenu) super.setHeaderIconInt(icon);
    }

    @Nonnull
    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        return (SubMenu) super.setHeaderTitleInt(title);
    }

    @Nonnull
    @Override
    public SubMenu setHeaderView(View view) {
        return (SubMenu) super.setHeaderViewInt(view);
    }

    @Override
    public boolean expandItemActionView(MenuItemImpl item) {
        return mParentMenu.expandItemActionView(item);
    }

    @Override
    public boolean collapseItemActionView(MenuItemImpl item) {
        return mParentMenu.collapseItemActionView(item);
    }

    @Override
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        mParentMenu.setGroupDividerEnabled(groupDividerEnabled);
    }

    @Override
    public boolean isGroupDividerEnabled() {
        return mParentMenu.isGroupDividerEnabled();
    }
}
