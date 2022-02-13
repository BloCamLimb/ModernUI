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

import icyllis.modernui.math.Rect;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.MenuItem;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;

import javax.annotation.Nullable;

/**
 * Base class for a menu popup abstraction - i.e., some type of menu, housed in a popup window
 * environment.
 */
public abstract class MenuPopup implements ShowableListMenu, MenuPresenter, AdapterView.OnItemClickListener {

    private Rect mEpicenterBounds;

    public abstract void setForceShowIcon(boolean forceShow);

    /**
     * Adds the given menu to the popup, if it is capable of displaying submenus within itself.
     * If menu is the first menu shown, it won't be displayed until show() is called.
     * If the popup was already showing, adding a submenu via this method will cause that new
     * submenu to be shown immediately (that is, if this MenuPopup implementation is capable of
     * showing its own submenus).
     *
     * @param menu
     */
    public abstract void addMenu(MenuBuilder menu);

    public abstract void setGravity(int dropDownGravity);

    public abstract void setAnchorView(View anchor);

    public abstract void setHorizontalOffset(int x);

    public abstract void setVerticalOffset(int y);

    /**
     * Specifies the anchor-relative bounds of the popup's transition
     * epicenter.
     *
     * @param bounds anchor-relative bounds
     */
    public void setEpicenterBounds(Rect bounds) {
        mEpicenterBounds = bounds;
    }

    /**
     * @return anchor-relative bounds of the popup's transition epicenter
     */
    public Rect getEpicenterBounds() {
        return mEpicenterBounds;
    }

    /**
     * Set whether a title entry should be shown in the popup menu (if a title exists for the
     * menu).
     *
     * @param showTitle
     */
    public abstract void setShowTitle(boolean showTitle);

    /**
     * Set a listener to receive a callback when the popup is dismissed.
     *
     * @param listener Listener that will be notified when the popup is dismissed.
     */
    public abstract void setOnDismissListener(PopupWindow.OnDismissListener listener);

    @Override
    public void initForMenu(@Nullable MenuBuilder menu) {
        // Don't need to do anything; we added as a presenter in the constructor.
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        throw new UnsupportedOperationException("MenuPopups manage their own views");
    }

    @Override
    public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    @Override
    public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListAdapter outerAdapter = (ListAdapter) parent.getAdapter();
        MenuAdapter wrappedAdapter = toMenuAdapter(outerAdapter);

        // Use the position from the outer adapter so that if a header view was added, we don't get
        // an off-by-1 error in position.
        wrappedAdapter.mAdapterMenu.performItemAction((MenuItem) outerAdapter.getItem(position), 0);
    }

    /**
     * Measures the width of the given menu view.
     *
     * @param view The view to measure.
     * @return The width.
     */
    protected static int measureIndividualMenuWidth(ListAdapter adapter, ViewGroup parent,
                                                    int maxAllowedWidth) {
        // Menus don't tend to be long, so this is more valid than it looks.
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (parent == null) {
                parent = new FrameLayout();
            }

            itemView = adapter.getView(i, itemView, parent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();
            if (itemWidth >= maxAllowedWidth) {
                return maxAllowedWidth;
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }

    /**
     * Converts the given ListAdapter originating from a menu, to a MenuAdapter, accounting for
     * the possibility of the parameter adapter actually wrapping the MenuAdapter. (That could
     * happen if a header view was added on the menu.)
     *
     * @param adapter
     * @return
     */
    protected static MenuAdapter toMenuAdapter(ListAdapter adapter) {
        if (adapter instanceof HeaderViewListAdapter) {
            return (MenuAdapter) ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        }
        return (MenuAdapter) adapter;
    }

    /**
     * Returns whether icon spacing needs to be preserved for the given menu, based on whether any
     * of its items contains an icon.
     *
     * NOTE: This should only be used for non-overflow-only menus, because this method does not
     * take into account whether the menu items are being shown as part of the popup or or being
     * shown as actions in the action bar.
     *
     * @param menu
     * @return Whether to preserve icon spacing.
     */
    protected static boolean shouldPreserveIconSpacing(MenuBuilder menu) {
        boolean preserveIconSpacing = false;
        final int count = menu.size();

        for (int i = 0; i < count; i++) {
            MenuItem childItem = menu.getItem(i);
            if (childItem.isVisible() && childItem.getIcon() != null) {
                preserveIconSpacing = true;
                break;
            }
        }

        return preserveIconSpacing;
    }
}
