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

import icyllis.modernui.transition.Transition;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MenuItem;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.menu.ListMenuItemView;
import icyllis.modernui.view.menu.MenuAdapter;
import icyllis.modernui.view.menu.MenuBuilder;

import javax.annotation.Nonnull;

/**
 * A MenuPopupWindow represents the popup window for menu.
 * <p>
 * MenuPopupWindow is mostly same as ListPopupWindow, but it has customized
 * behaviors specific to menus,
 */
public class MenuPopupWindow extends ListPopupWindow implements MenuItemHoverListener {

    private MenuItemHoverListener mHoverListener;

    public MenuPopupWindow() {
    }

    @Nonnull
    @Override
    DropDownListView createDropDownListView(boolean hijackFocus) {
        MenuDropDownListView view = new MenuDropDownListView(hijackFocus);
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
    public void onItemHoverEnter(@Nonnull MenuBuilder menu, @Nonnull MenuItem item) {
        // Forward up the chain
        if (mHoverListener != null) {
            mHoverListener.onItemHoverEnter(menu, item);
        }
    }

    @Override
    public void onItemHoverExit(@Nonnull MenuBuilder menu, @Nonnull MenuItem item) {
        // Forward up the chain
        if (mHoverListener != null) {
            mHoverListener.onItemHoverExit(menu, item);
        }
    }

    public static class MenuDropDownListView extends DropDownListView {

        final int mAdvanceKey;
        final int mRetreatKey;

        private MenuItemHoverListener mHoverListener;
        private MenuItem mHoveredMenuItem;

        public MenuDropDownListView(boolean hijackFocus) {
            super(hijackFocus);

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
        public boolean onKeyDown(int keyCode, KeyEvent event) {
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
                ((MenuAdapter) getAdapter()).getAdapterMenu().close(false /* closeAllMenus */);
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onHoverEvent(MotionEvent ev) {
            // Dispatch any changes in hovered item index to the listener.
            if (mHoverListener != null) {
                // The adapter may be wrapped. Adjust the index if necessary.
                final int headersCount;
                final MenuAdapter menuAdapter;
                final ListAdapter adapter = getAdapter();
                if (adapter instanceof HeaderViewListAdapter) {
                    final HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter) adapter;
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
