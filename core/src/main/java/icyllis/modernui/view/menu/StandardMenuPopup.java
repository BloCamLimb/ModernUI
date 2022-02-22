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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;

import javax.annotation.Nonnull;

import static icyllis.modernui.view.View.dp;
import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class StandardMenuPopup extends MenuPopup implements PopupWindow.OnDismissListener,
        AdapterView.OnItemClickListener, MenuPresenter, View.OnKeyListener {

    private final MenuBuilder mMenu;
    private final MenuAdapter mAdapter;
    private final boolean mOverflowOnly;
    private final int mPopupMaxWidth;
    // The popup window is final in order to couple its lifecycle to the lifecycle of the
    // StandardMenuPopup.
    private final MenuPopupWindow mPopup;

    private final ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Only move the popup if it's showing and non-modal. We don't want
                    // to be moving around the only interactive window, since there's a
                    // good chance the user is interacting with it.
                    if (isShowing() && !mPopup.isModal()) {
                        final View anchor = mShownAnchorView;
                        if (anchor == null || !anchor.isShown()) {
                            dismiss();
                        } else {
                            // Recompute window size and position
                            mPopup.show();
                        }
                    }
                }
            };

    private final View.OnAttachStateChangeListener mAttachStateChangeListener =
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (mTreeObserver != null) {
                        if (!mTreeObserver.isAlive()) mTreeObserver = v.getViewTreeObserver();
                        mTreeObserver.removeOnGlobalLayoutListener(mGlobalLayoutListener);
                    }
                    v.removeOnAttachStateChangeListener(this);
                }
            };

    private PopupWindow.OnDismissListener mOnDismissListener;

    private View mAnchorView;
    private View mShownAnchorView;
    private Callback mPresenterCallback;
    private ViewTreeObserver mTreeObserver;

    /**
     * Whether the popup has been dismissed. Once dismissed, it cannot be opened again.
     */
    private boolean mWasDismissed;

    /**
     * Whether the cached content width value is valid.
     */
    private boolean mHasContentWidth;

    /**
     * Cached content width.
     */
    private int mContentWidth;

    private int mDropDownGravity = Gravity.NO_GRAVITY;

    private boolean mShowTitle;

    public StandardMenuPopup(@Nonnull MenuBuilder menu, @Nonnull View anchorView, boolean overflowOnly) {
        mMenu = menu;
        mOverflowOnly = overflowOnly;
        mAdapter = new MenuAdapter(menu, mOverflowOnly);

        mPopupMaxWidth = anchorView.getRootView().getMeasuredWidth() / 2;

        mAnchorView = anchorView;

        mPopup = new MenuPopupWindow();
        mPopup.setBackgroundDrawable(new Drawable() {
            private final int mRadius = dp(2);

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setColor(0xff303030);
                Rect b = getBounds();
                canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, mRadius, paint);
            }

            @Override
            public boolean getPadding(@Nonnull Rect padding) {
                int r = (int) Math.ceil(mRadius / 2f);
                padding.set(r, r, r, r);
                return true;
            }
        });

        // Present the menu using our context, not the menu builder's context.
        menu.addMenuPresenter(this);
    }

    @Override
    public void setForceShowIcon(boolean forceShow) {
        mAdapter.setForceShowIcon(forceShow);
    }

    @Override
    public void setGravity(int gravity) {
        mDropDownGravity = gravity;
    }

    private boolean tryShow() {
        if (isShowing()) {
            return true;
        }

        if (mWasDismissed || mAnchorView == null) {
            return false;
        }

        mShownAnchorView = mAnchorView;

        mPopup.setOnDismissListener(this);
        mPopup.setOnItemClickListener(this);
        mPopup.setAdapter(mAdapter);
        mPopup.setModal(true);

        final View anchor = mShownAnchorView;
        final boolean addGlobalListener = mTreeObserver == null;
        mTreeObserver = anchor.getViewTreeObserver(); // Refresh to latest
        if (addGlobalListener) {
            mTreeObserver.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
        anchor.addOnAttachStateChangeListener(mAttachStateChangeListener);
        mPopup.setAnchorView(anchor);
        mPopup.setDropDownGravity(mDropDownGravity);

        if (!mHasContentWidth) {
            mContentWidth = measureIndividualMenuWidth(mAdapter, null, mPopupMaxWidth);
            mHasContentWidth = true;
        }

        mPopup.setContentWidth(mContentWidth);
        mPopup.setEpicenterBounds(getEpicenterBounds());
        mPopup.show();

        ListView listView = mPopup.getListView();
        assert listView != null;
        listView.setOnKeyListener(this);

        if (mShowTitle && mMenu.getHeaderTitle() != null) {
            FrameLayout titleItemView = new FrameLayout();
            titleItemView.setMinimumWidth(dp(196));
            titleItemView.setPadding(dp(16), 0, dp(16), 0);
            titleItemView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, dp(48)));

            TextView titleView = new TextView();
            titleView.setText(mMenu.getHeaderTitle());
            titleView.setGravity(Gravity.CENTER_VERTICAL);
            titleView.setSingleLine();
            titleView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            titleItemView.addView(titleView, MATCH_PARENT, WRAP_CONTENT);

            titleItemView.setEnabled(false);
            listView.addHeaderView(titleItemView, null, false);

            // Update to show the title.
            mPopup.show();
        }
        return true;
    }

    @Override
    public void show() {
        if (!tryShow()) {
            throw new IllegalStateException("StandardMenuPopup cannot be used without an anchor");
        }
    }

    @Override
    public void dismiss() {
        if (isShowing()) {
            mPopup.dismiss();
        }
    }

    @Override
    public void addMenu(MenuBuilder menu) {
        // No-op: standard implementation has only one menu which is set in the constructor.
    }

    @Override
    public boolean isShowing() {
        return !mWasDismissed && mPopup.isShowing();
    }

    @Override
    public void onDismiss() {
        mWasDismissed = true;
        mMenu.close();

        if (mTreeObserver != null) {
            if (!mTreeObserver.isAlive()) mTreeObserver = mShownAnchorView.getViewTreeObserver();
            mTreeObserver.removeOnGlobalLayoutListener(mGlobalLayoutListener);
            mTreeObserver = null;
        }
        mShownAnchorView.removeOnAttachStateChangeListener(mAttachStateChangeListener);

        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    @Override
    public void updateMenuView(boolean cleared) {
        mHasContentWidth = false;

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setCallback(Callback cb) {
        mPresenterCallback = cb;
    }

    @Override
    public boolean onSubMenuSelected(@Nonnull SubMenuBuilder subMenu) {
        if (subMenu.hasVisibleItems()) {
            final MenuPopupHelper subPopup = new MenuPopupHelper(subMenu,
                    mShownAnchorView, mOverflowOnly);
            subPopup.setPresenterCallback(mPresenterCallback);
            subPopup.setForceShowIcon(MenuPopup.shouldPreserveIconSpacing(subMenu));

            // Pass responsibility for handling onDismiss to the submenu.
            subPopup.setOnDismissListener(mOnDismissListener);
            mOnDismissListener = null;

            // Close this menu popup to make room for the submenu popup.
            mMenu.close(false /* closeAllMenus */);

            // Show the new sub-menu popup at the same location as this popup.
            int horizontalOffset = mPopup.getHorizontalOffset();
            final int verticalOffset = mPopup.getVerticalOffset();

            // As xOffset of parent menu popup is subtracted with Anchor width for Gravity.RIGHT,
            // So, again to display sub-menu popup in same xOffset, add the Anchor width.
            final int hgrav = Gravity.getAbsoluteGravity(mDropDownGravity,
                    mAnchorView.getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (hgrav == Gravity.RIGHT) {
                horizontalOffset += mAnchorView.getWidth();
            }

            if (subPopup.tryShow(horizontalOffset, verticalOffset)) {
                if (mPresenterCallback != null) {
                    mPresenterCallback.onOpenSubMenu(subMenu);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        // Only care about the (sub)menu we're presenting.
        if (menu != mMenu) return;

        dismiss();
        if (mPresenterCallback != null) {
            mPresenterCallback.onCloseMenu(menu, allMenusAreClosing);
        }
    }

    @Override
    public boolean flagActionItems() {
        return false;
    }

    @Override
    public void setAnchorView(View anchor) {
        mAnchorView = anchor;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        /*if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss();
            return true;
        }*/
        return false;
    }

    @Override
    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    @Override
    public ListView getListView() {
        return mPopup.getListView();
    }

    @Override
    public void setHorizontalOffset(int x) {
        mPopup.setHorizontalOffset(x);
    }

    @Override
    public void setVerticalOffset(int y) {
        mPopup.setVerticalOffset(y);
    }

    @Override
    public void setShowTitle(boolean showTitle) {
        mShowTitle = showTitle;
    }
}
