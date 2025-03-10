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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.PopupMenu;
import icyllis.modernui.widget.PopupWindow;

public class MenuPopupHelper implements MenuHelper {

    private static final int TOUCH_EPICENTER_SIZE_DP = 48;

    private final Context mContext;

    // Immutable cached popup menu properties.
    private final MenuBuilder mMenu;
    private final boolean mOverflowOnly;

    // Mutable cached popup menu properties.
    private View mAnchorView;
    private int mDropDownGravity = Gravity.START;
    private boolean mForceShowIcon;
    private MenuPresenter.Callback mPresenterCallback;

    private MenuPopup mPopup;
    private PopupWindow.OnDismissListener mOnDismissListener;

    /**
     * Listener used to proxy dismiss callbacks to the helper's owner.
     */
    private final PopupWindow.OnDismissListener mInternalOnDismissListener = this::onDismiss;

    public MenuPopupHelper(@NonNull Context context, @NonNull MenuBuilder menu) {
        this(context, menu, null, false);
    }

    public MenuPopupHelper(@NonNull Context context, @NonNull MenuBuilder menu, View anchorView) {
        this(context, menu, anchorView, false);
    }

    public MenuPopupHelper(@NonNull Context context, @NonNull MenuBuilder menu, View anchorView, boolean overflowOnly) {
        mContext = context;
        mMenu = menu;
        mAnchorView = anchorView;
        mOverflowOnly = overflowOnly;
    }

    public void setOnDismissListener(@Nullable PopupWindow.OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    /**
     * Sets the view to which the popup window is anchored.
     * <p>
     * Changes take effect on the next call to show().
     *
     * @param anchor the view to which the popup window should be anchored
     */
    public void setAnchorView(@NonNull View anchor) {
        mAnchorView = anchor;
    }

    /**
     * Sets whether the popup menu's adapter is forced to show icons in the
     * menu item views.
     * <p>
     * Changes take effect on the next call to show().
     * <p>
     * This method should not be accessed directly outside the framework, please use
     * {@link PopupMenu#setForceShowIcon(boolean)} instead.
     *
     * @param forceShowIcon {@code true} to force icons to be shown, or
     *                      {@code false} for icons to be optionally shown
     */
    public void setForceShowIcon(boolean forceShowIcon) {
        mForceShowIcon = forceShowIcon;
        if (mPopup != null) {
            mPopup.setForceShowIcon(forceShowIcon);
        }
    }

    /**
     * Sets the alignment of the popup window relative to the anchor view.
     * <p>
     * Changes take effect on the next call to show().
     *
     * @param gravity alignment of the popup relative to the anchor
     */
    public void setGravity(int gravity) {
        mDropDownGravity = gravity;
    }

    /**
     * @return alignment of the popup relative to the anchor
     */
    public int getGravity() {
        return mDropDownGravity;
    }

    public void show() {
        if (!tryShow()) {
            throw new IllegalStateException("MenuPopupHelper cannot be used without an anchor");
        }
    }

    public void show(int x, int y) {
        if (!tryShow(x, y)) {
            throw new IllegalStateException("MenuPopupHelper cannot be used without an anchor");
        }
    }

    @NonNull
    public MenuPopup getPopup() {
        if (mPopup == null) {
            mPopup = createPopup();
        }
        return mPopup;
    }

    /**
     * Attempts to show the popup anchored to the view specified by {@link #setAnchorView(View)}.
     *
     * @return {@code true} if the popup was shown or was already showing prior to calling this
     * method, {@code false} otherwise
     */
    public boolean tryShow() {
        if (isShowing()) {
            return true;
        }

        if (mAnchorView == null) {
            return false;
        }

        showPopup(0, 0, false, false);
        return true;
    }

    /**
     * Shows the popup menu and makes a best-effort to anchor it to the
     * specified (x,y) coordinate relative to the anchor view.
     * <p>
     * Additionally, the popup's transition epicenter (see
     * {@link PopupWindow#setEpicenterBounds(Rect)} will be
     * centered on the specified coordinate, rather than using the bounds of
     * the anchor view.
     * <p>
     * If the popup's resolved gravity is {@link Gravity#LEFT}, this will
     * display the popup with its top-left corner at (x,y) relative to the
     * anchor view. If the resolved gravity is {@link Gravity#RIGHT}, the
     * popup's top-right corner will be at (x,y).
     * <p>
     * If the popup cannot be displayed fully on-screen, this method will
     * attempt to scroll the anchor view's ancestors and/or offset the popup
     * such that it may be displayed fully on-screen.
     *
     * @param x x coordinate relative to the anchor view
     * @param y y coordinate relative to the anchor view
     * @return {@code true} if the popup was shown or was already showing prior
     * to calling this method, {@code false} otherwise
     */
    public boolean tryShow(int x, int y) {
        if (isShowing()) {
            return true;
        }

        if (mAnchorView == null) {
            return false;
        }

        showPopup(x, y, true, true);
        return true;
    }

    /**
     * Creates the popup and assigns cached properties.
     *
     * @return an initialized popup
     */
    @NonNull
    private MenuPopup createPopup() {
        /*final WindowManager windowManager = ((Activity)mContext).getWindowManager();
        final Rect maxWindowBounds = windowManager.getMaximumWindowMetrics().getBounds();

        final int smallestWidth = Math.min(maxWindowBounds.width(), maxWindowBounds.height());
        final int minSmallestWidthCascading = (int) (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DP, 720, mContext.getResources().getDisplayMetrics()
        ) + 0.5f);*/
        final boolean enableCascadingSubmenus = true/*smallestWidth >= minSmallestWidthCascading*/;

        final MenuPopup popup;
        if (enableCascadingSubmenus) {
            popup = new CascadingMenuPopup(mContext, mAnchorView, mOverflowOnly);
        } else {
            popup = new StandardMenuPopup(mContext, mMenu, mAnchorView, mOverflowOnly);
        }

        // Assign immutable properties.
        popup.addMenu(mMenu);
        popup.setOnDismissListener(mInternalOnDismissListener);

        // Assign mutable properties. These may be reassigned later.
        popup.setAnchorView(mAnchorView);
        popup.setCallback(mPresenterCallback);
        popup.setForceShowIcon(mForceShowIcon);
        popup.setGravity(mDropDownGravity);

        return popup;
    }

    private void showPopup(int xOffset, int yOffset, boolean useOffsets, boolean showTitle) {
        final MenuPopup popup = getPopup();
        popup.setShowTitle(showTitle);

        if (useOffsets) {
            // If the resolved drop-down gravity is RIGHT, the popup's right
            // edge will be aligned with the anchor view. Adjust by the anchor
            // width such that the top-right corner is at the X offset.
            final int hgrav = Gravity.getAbsoluteGravity(mDropDownGravity,
                    mAnchorView.getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (hgrav == Gravity.RIGHT) {
                xOffset -= mAnchorView.getWidth();
            }

            popup.setHorizontalOffset(xOffset);
            popup.setVerticalOffset(yOffset);

            // Set the transition epicenter to be roughly finger (or mouse
            // cursor) sized and centered around the offset position. This
            // will give the appearance that the window is emerging from
            // the touch point.
            final float density = mContext.getResources().getDisplayMetrics().density;
            final int halfSize = (int) (TOUCH_EPICENTER_SIZE_DP * density / 2);
            final Rect epicenter = new Rect(xOffset - halfSize, yOffset - halfSize,
                    xOffset + halfSize, yOffset + halfSize);
            popup.setEpicenterBounds(epicenter);
        }

        popup.show();
    }

    /**
     * Dismisses the popup, if showing.
     */
    @Override
    public void dismiss() {
        if (isShowing()) {
            mPopup.dismiss();
        }
    }

    /**
     * Called after the popup has been dismissed.
     * <p>
     * <strong>Note:</strong> Subclasses should call the super implementation
     * last to ensure that any necessary tear down has occurred before the
     * listener specified by {@link #setOnDismissListener(PopupWindow.OnDismissListener)}
     * is called.
     */
    protected void onDismiss() {
        mPopup = null;

        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    public boolean isShowing() {
        return mPopup != null && mPopup.isShowing();
    }

    @Override
    public void setPresenterCallback(@Nullable MenuPresenter.Callback cb) {
        mPresenterCallback = cb;
        if (mPopup != null) {
            mPopup.setCallback(cb);
        }
    }
}
