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

import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.BaseAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class MenuAdapter extends BaseAdapter {

    MenuBuilder mAdapterMenu;

    private int mExpandedIndex = -1;

    private boolean mForceShowIcon;
    private final boolean mOverflowOnly;

    public MenuAdapter(MenuBuilder menu, boolean overflowOnly) {
        mAdapterMenu = menu;
        mOverflowOnly = overflowOnly;
        findExpandedIndex();
    }

    public boolean getForceShowIcon() {
        return mForceShowIcon;
    }

    public void setForceShowIcon(boolean forceShow) {
        mForceShowIcon = forceShow;
    }

    @Override
    public int getCount() {
        ArrayList<MenuItemImpl> items = mOverflowOnly ?
                mAdapterMenu.getNonActionItems() : mAdapterMenu.getVisibleItems();
        if (mExpandedIndex < 0) {
            return items.size();
        }
        return items.size() - 1;
    }

    public MenuBuilder getAdapterMenu() {
        return mAdapterMenu;
    }

    @Nonnull
    @Override
    public MenuItemImpl getItem(int position) {
        ArrayList<MenuItemImpl> items = mOverflowOnly ?
                mAdapterMenu.getNonActionItems() : mAdapterMenu.getVisibleItems();
        if (mExpandedIndex >= 0 && position >= mExpandedIndex) {
            position++;
        }
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        // Since a menu item's ID is optional, we'll use the position as an
        // ID for the item in the AdapterView
        return position;
    }

    @Nonnull
    @Override
    public View getView(int position, @Nullable View convertView, @Nonnull ViewGroup parent) {
        /*if (convertView == null) {
            convertView = new ListMenuItemView();
        }

        final int currGroupId = getItem(position).getGroupId();
        final int prevGroupId =
                position - 1 >= 0 ? getItem(position - 1).getGroupId() : currGroupId;
        // Show a divider if adjacent items are in different groups.
        ((ListMenuItemView) convertView)
                .setGroupDividerEnabled(mAdapterMenu.isGroupDividerEnabled()
                        && (currGroupId != prevGroupId));

        MenuView.ItemView itemView = (MenuView.ItemView) convertView;
        if (mForceShowIcon) {
            ((ListMenuItemView) convertView).setForceShowIcon(true);
        }
        itemView.initialize(getItem(position), 0);*/
        return convertView;
    }

    void findExpandedIndex() {
        final MenuItemImpl expandedItem = mAdapterMenu.getExpandedItem();
        if (expandedItem != null) {
            final ArrayList<MenuItemImpl> items = mAdapterMenu.getNonActionItems();
            final int count = items.size();
            for (int i = 0; i < count; i++) {
                final MenuItemImpl item = items.get(i);
                if (item == expandedItem) {
                    mExpandedIndex = i;
                    return;
                }
            }
        }
        mExpandedIndex = -1;
    }

    @Override
    public void notifyDataSetChanged() {
        findExpandedIndex();
        super.notifyDataSetChanged();
    }
}
