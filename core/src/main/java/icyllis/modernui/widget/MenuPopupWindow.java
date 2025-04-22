/*
 * Modern UI.
 * Copyright (C) 2022-2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2015 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.widget;

import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.transition.Transition;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MenuItem;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.menu.ListMenuItemView;
import icyllis.modernui.view.menu.MenuAdapter;
import icyllis.modernui.view.menu.MenuBuilder;
import org.jetbrains.annotations.ApiStatus;

/**
 * A MenuPopupWindow represents the popup window for menu.
 * <p>
 * MenuPopupWindow is mostly same as ListPopupWindow, but it has customized
 * behaviors specific to menus.
 *
 * @hidden
 */
@ApiStatus.Internal
public class MenuPopupWindow extends ListPopupWindow implements MenuItemHoverListener {

    private MenuItemHoverListener mHoverListener;

    public MenuPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
                           @Nullable @AttrRes ResourceId defStyleAttr,
                           @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @NonNull
    @Override
    DropDownListView createDropDownListView(Context context, boolean hijackFocus) {
        MenuDropDownListView view = new MenuDropDownListView(context, hijackFocus);
        view.setHoverListener(this);
        return view;
    }

    public void setEnterTransition(Transition enterTransition) {
        mPopup.setEnterTransition(enterTransition);
    }

    public void setExitTransition(Transition exitTransition) {
        mPopup.setExitTransition(exitTransition);
    }

    public void setHoverListener(MenuItemHoverListener hoverListener) {
        mHoverListener = hoverListener;
    }

    /**
     * Set whether this window is touch modal or if outside touches will be sent to
     * other windows behind it.
     */
    public void setTouchModal(boolean touchModal) {
        mPopup.setTouchModal(touchModal);
    }

    @Override
    public void onItemHoverEnter(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
        // Forward up the chain
        if (mHoverListener != null) {
            mHoverListener.onItemHoverEnter(menu, item);
        }
    }

    @Override
    public void onItemHoverExit(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
        // Forward up the chain
        if (mHoverListener != null) {
            mHoverListener.onItemHoverExit(menu, item);
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static class MenuDropDownListView extends DropDownListView {

        final int mAdvanceKey;
        final int mRetreatKey;

        private MenuItemHoverListener mHoverListener;
        private MenuItem mHoveredMenuItem;

        public MenuDropDownListView(Context context, boolean hijackFocus) {
            super(context, hijackFocus);

            //TODO RTL
            /*if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                mAdvanceKey = KeyEvent.KEY_LEFT;
                mRetreatKey = KeyEvent.KEY_RIGHT;
            } else {*/
            mAdvanceKey = KeyEvent.KEY_RIGHT;
            mRetreatKey = KeyEvent.KEY_LEFT;
            //}
        }

        public void setHoverListener(MenuItemHoverListener hoverListener) {
            mHoverListener = hoverListener;
        }

        public void clearSelection() {
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
        }

        @Override
        public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
            ListMenuItemView selectedItem = (ListMenuItemView) getSelectedView();
            if (selectedItem != null && keyCode == mAdvanceKey) {
                if (selectedItem.isEnabled() && selectedItem.getItemData().hasSubMenu()) {
                    performItemClick(
                            selectedItem,
                            getSelectedItemPosition(),
                            getSelectedItemId());
                }
                return true;
            } else if (selectedItem != null && keyCode == mRetreatKey) {
                setSelectedPositionInt(INVALID_POSITION);
                setNextSelectedPositionInt(INVALID_POSITION);

                // Close only the top-level menu.
                final ListAdapter adapter = getAdapter();
                final MenuAdapter menuAdapter;
                if (adapter instanceof HeaderViewListAdapter) {
                    menuAdapter =
                            (MenuAdapter) ((HeaderViewListAdapter) adapter).getWrappedAdapter();
                } else {
                    menuAdapter = (MenuAdapter) adapter;
                }
                menuAdapter.getAdapterMenu().close(false /* closeAllMenus */);
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onHoverEvent(@NonNull MotionEvent ev) {
            // Dispatch any changes in hovered item index to the listener.
            if (mHoverListener != null) {
                // The adapter may be wrapped. Adjust the index if necessary.
                final int headersCount;
                final MenuAdapter menuAdapter;
                final ListAdapter adapter = getAdapter();
                if (adapter instanceof final HeaderViewListAdapter headerAdapter) {
                    headersCount = headerAdapter.getHeadersCount();
                    menuAdapter = (MenuAdapter) headerAdapter.getWrappedAdapter();
                } else {
                    headersCount = 0;
                    menuAdapter = (MenuAdapter) adapter;
                }

                // Find the menu item for the view at the event coordinates.
                MenuItem menuItem = null;
                if (ev.getAction() != MotionEvent.ACTION_HOVER_EXIT) {
                    final int position = pointToPosition((int) ev.getX(), (int) ev.getY());
                    if (position != INVALID_POSITION) {
                        final int itemPosition = position - headersCount;
                        if (itemPosition >= 0 && itemPosition < menuAdapter.getCount()) {
                            menuItem = menuAdapter.getItem(itemPosition);
                        }
                    }
                }

                final MenuItem oldMenuItem = mHoveredMenuItem;
                if (oldMenuItem != menuItem) {
                    final MenuBuilder menu = menuAdapter.getAdapterMenu();
                    if (oldMenuItem != null) {
                        mHoverListener.onItemHoverExit(menu, oldMenuItem);
                    }

                    mHoveredMenuItem = menuItem;

                    if (menuItem != null) {
                        mHoverListener.onItemHoverEnter(menu, menuItem);
                    }
                }
            }

            return super.onHoverEvent(ev);
        }
    }
}
