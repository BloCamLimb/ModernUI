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
 *   Copyright (C) 2010 The Android Open Source Project
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

import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.view.*;
import icyllis.modernui.view.menu.ShowableListMenu;
import org.jetbrains.annotations.ApiStatus;

/**
 * A ListPopupWindow anchors itself to a host view and displays a
 * list of choices.
 *
 * <p>ListPopupWindow contains a number of tricky behaviors surrounding
 * positioning, scrolling parents to fit the dropdown, interacting
 * sanely with the IME if present, and others.
 *
 * @see Spinner
 */
public class ListPopupWindow implements ShowableListMenu {

    /**
     * This value controls the length of time that the user
     * must leave a pointer down without scrolling to expand
     * the autocomplete dropdown list to cover the IME.
     */
    private static final int EXPAND_LIST_TIMEOUT = 250;

    /**
     * The provided prompt view should appear above list content.
     *
     * @see #setPromptPosition(int)
     * @see #getPromptPosition()
     * @see #setPromptView(View)
     */
    public static final int POSITION_PROMPT_ABOVE = 0;

    /**
     * The provided prompt view should appear below list content.
     *
     * @see #setPromptPosition(int)
     * @see #getPromptPosition()
     * @see #setPromptView(View)
     */
    public static final int POSITION_PROMPT_BELOW = 1;

    private Context mContext;
    private ListAdapter mAdapter;
    private DropDownListView mDropDownList;

    private int mDropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mDropDownWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mDropDownHorizontalOffset;
    private int mDropDownVerticalOffset;
    private boolean mDropDownVerticalOffsetSet;
    private boolean mOverlapAnchor;
    private boolean mOverlapAnchorSet;

    private int mDropDownGravity = Gravity.NO_GRAVITY;

    private boolean mDropDownAlwaysVisible = false;
    private boolean mForceIgnoreOutsideTouch = false;
    int mListItemExpandMaximum = Integer.MAX_VALUE;

    private View mPromptView;
    private int mPromptPosition = POSITION_PROMPT_ABOVE;

    private DataSetObserver mObserver;

    private View mDropDownAnchorView;

    private Drawable mDropDownListHighlight;

    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;

    private final ResizePopupRunnable mResizePopupRunnable = new ResizePopupRunnable();
    private final PopupTouchInterceptor mTouchInterceptor = new PopupTouchInterceptor();
    private final ListSelectorHider mHideSelector = new ListSelectorHider();
    private Runnable mShowDropDownRunnable;

    private final Rect mTempRect = new Rect();

    /**
     * Optional anchor-relative bounds to be used as the transition epicenter.
     * When {@code null}, the anchor bounds are used as the epicenter.
     */
    private Rect mEpicenterBounds;

    private boolean mModal;

    PopupWindow mPopup;

    @AttrRes
    private static final ResourceId DEF_STYLE_ATTR =
            ResourceId.attr(R.ns, R.attr.listPopupWindowStyle);

    public ListPopupWindow(@NonNull Context context) {
        this(context, null);
    }

    public ListPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, DEF_STYLE_ATTR);
    }

    public ListPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
                           @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public ListPopupWindow(@NonNull Context context, @Nullable AttributeSet attrs,
                           @Nullable @AttrRes ResourceId defStyleAttr,
                           @Nullable @StyleRes ResourceId defStyleRes) {
        mContext = context;
        mPopup = new PopupWindow(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Sets the adapter that provides the data and the views to represent the data
     * in this popup window.
     *
     * @param adapter The adapter to use to create this window's content.
     */
    public void setAdapter(@Nullable ListAdapter adapter) {
        if (mObserver == null) {
            mObserver = new PopupDataSetObserver();
        } else if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            adapter.registerDataSetObserver(mObserver);
        }

        if (mDropDownList != null) {
            mDropDownList.setAdapter(mAdapter);
        }
    }

    /**
     * Set where the optional prompt view should appear. The default is
     * {@link #POSITION_PROMPT_ABOVE}.
     *
     * @param position A position constant declaring where the prompt should be displayed.
     * @see #POSITION_PROMPT_ABOVE
     * @see #POSITION_PROMPT_BELOW
     */
    public void setPromptPosition(int position) {
        mPromptPosition = position;
    }

    /**
     * @return Where the optional prompt view should appear.
     * @see #POSITION_PROMPT_ABOVE
     * @see #POSITION_PROMPT_BELOW
     */
    public int getPromptPosition() {
        return mPromptPosition;
    }

    /**
     * Set whether this window should be modal when shown.
     *
     * <p>If a popup window is modal, it will receive all touch and key input.
     * If the user touches outside the popup window's content area the popup window
     * will be dismissed.
     *
     * @param modal {@code true} if the popup window should be modal, {@code false} otherwise.
     */
    public void setModal(boolean modal) {
        mModal = modal;
        mPopup.setFocusable(modal);
    }

    /**
     * Returns whether the popup window will be modal when shown.
     *
     * @return {@code true} if the popup window will be modal, {@code false} otherwise.
     */
    public boolean isModal() {
        return mModal;
    }

    /**
     * Forces outside touches to be ignored. Normally if {@link #isDropDownAlwaysVisible()} is
     * false, we allow outside touch to dismiss the dropdown. If this is set to true, then we
     * ignore outside touch even when the drop down is not set to always visible.
     *
     * @hide Used only by AutoCompleteTextView to handle some internal special cases.
     */
    public void setForceIgnoreOutsideTouch(boolean forceIgnoreOutsideTouch) {
        mForceIgnoreOutsideTouch = forceIgnoreOutsideTouch;
    }

    /**
     * Sets whether the drop-down should remain visible under certain conditions.
     * <p>
     * The drop-down will occupy the entire screen below {@link #getAnchorView} regardless
     * of the size or content of the list.  {@link #getBackground()} will fill any space
     * that is not used by the list.
     *
     * @param dropDownAlwaysVisible Whether to keep the drop-down visible.
     * @hide Only used by AutoCompleteTextView under special conditions.
     */
    public void setDropDownAlwaysVisible(boolean dropDownAlwaysVisible) {
        mDropDownAlwaysVisible = dropDownAlwaysVisible;
    }

    /**
     * @return Whether the drop-down is visible under special conditions.
     * @hide Only used by AutoCompleteTextView under special conditions.
     */
    public boolean isDropDownAlwaysVisible() {
        return mDropDownAlwaysVisible;
    }

    /**
     * Sets a drawable to use as the list item selector.
     *
     * @param selector List selector drawable to use in the popup.
     */
    public void setListSelector(Drawable selector) {
        mDropDownListHighlight = selector;
    }

    /**
     * @return The background drawable for the popup window.
     */
    @Nullable
    public Drawable getBackground() {
        return mPopup.getBackground();
    }

    /**
     * Sets a drawable to be the background for the popup window.
     *
     * @param d A drawable to set as the background.
     */
    public void setBackgroundDrawable(@Nullable Drawable d) {
        mPopup.setBackgroundDrawable(d);
    }

    /**
     * Returns the view that will be used to anchor this popup.
     *
     * @return The popup's anchor view
     */
    @Nullable
    public View getAnchorView() {
        return mDropDownAnchorView;
    }

    /**
     * Sets the popup's anchor view. This popup will always be positioned relative to
     * the anchor view when shown.
     *
     * @param anchor The view to use as an anchor.
     */
    public void setAnchorView(@Nullable View anchor) {
        mDropDownAnchorView = anchor;
    }

    /**
     * @return The horizontal offset of the popup from its anchor in pixels.
     */
    public int getHorizontalOffset() {
        return mDropDownHorizontalOffset;
    }

    /**
     * Set the horizontal offset of this popup from its anchor view in pixels.
     *
     * @param offset The horizontal offset of the popup from its anchor.
     */
    public void setHorizontalOffset(int offset) {
        mDropDownHorizontalOffset = offset;
    }

    /**
     * @return The vertical offset of the popup from its anchor in pixels.
     */
    public int getVerticalOffset() {
        if (!mDropDownVerticalOffsetSet) {
            return 0;
        }
        return mDropDownVerticalOffset;
    }

    /**
     * Set the vertical offset of this popup from its anchor view in pixels.
     *
     * @param offset The vertical offset of the popup from its anchor.
     */
    public void setVerticalOffset(int offset) {
        mDropDownVerticalOffset = offset;
        mDropDownVerticalOffsetSet = true;
    }

    /**
     * Specifies the anchor-relative bounds of the popup's transition
     * epicenter.
     *
     * @param bounds anchor-relative bounds, or {@code null} to use default epicenter
     * @see #getEpicenterBounds()
     */
    public void setEpicenterBounds(@Nullable Rect bounds) {
        mEpicenterBounds = bounds != null ? bounds.copy() : null;
    }

    /**
     * Returns bounds which are used as a popup's epicenter
     * of the enter and exit transitions.
     *
     * @return bounds relative to anchor view, or {@code null} if not set
     * @see #setEpicenterBounds(Rect)
     */
    @Nullable
    public Rect getEpicenterBounds() {
        return mEpicenterBounds != null ? mEpicenterBounds.copy() : null;
    }

    /**
     * Set the gravity of the dropdown list. This is commonly used to
     * set gravity to START or END for alignment with the anchor.
     *
     * @param gravity Gravity value to use
     */
    public void setDropDownGravity(int gravity) {
        mDropDownGravity = gravity;
    }

    /**
     * @return The width of the popup window in pixels.
     */
    public int getWidth() {
        return mDropDownWidth;
    }

    /**
     * Sets the width of the popup window in pixels. Can also be {@link ViewGroup.LayoutParams#MATCH_PARENT}
     * or {@link ViewGroup.LayoutParams#WRAP_CONTENT}.
     *
     * @param width Width of the popup window.
     */
    public void setWidth(int width) {
        mDropDownWidth = width;
    }

    /**
     * Sets the width of the popup window by the size of its content. The final width may be
     * larger to accommodate styled window dressing.
     *
     * @param width Desired width of content in pixels.
     */
    public void setContentWidth(int width) {
        Drawable popupBackground = mPopup.getBackground();
        if (popupBackground != null) {
            popupBackground.getPadding(mTempRect);
            mDropDownWidth = mTempRect.left + mTempRect.right + width;
        } else {
            setWidth(width);
        }
    }

    /**
     * @return The height of the popup window in pixels.
     */
    public int getHeight() {
        return mDropDownHeight;
    }

    /**
     * Sets the height of the popup window in pixels. Can also be {@link ViewGroup.LayoutParams#MATCH_PARENT}.
     *
     * @param height Height of the popup window must be a positive value,
     *               {@link ViewGroup.LayoutParams#MATCH_PARENT}, or {@link ViewGroup.LayoutParams#WRAP_CONTENT}.
     * @throws IllegalArgumentException if height is set to negative value
     */
    public void setHeight(int height) {
        mDropDownHeight = height;
    }

    /**
     * Sets a listener to receive events when a list item is clicked.
     *
     * @param clickListener Listener to register
     * @see ListView#setOnItemClickListener(AdapterView.OnItemClickListener)
     */
    public void setOnItemClickListener(@Nullable AdapterView.OnItemClickListener clickListener) {
        mItemClickListener = clickListener;
    }

    /**
     * Sets a listener to receive events when a list item is selected.
     *
     * @param selectedListener Listener to register.
     * @see ListView#setOnItemSelectedListener(AdapterView.OnItemSelectedListener)
     */
    public void setOnItemSelectedListener(@Nullable AdapterView.OnItemSelectedListener selectedListener) {
        mItemSelectedListener = selectedListener;
    }

    /**
     * Set a view to act as a user prompt for this popup window. Where the prompt view will appear
     * is controlled by {@link #setPromptPosition(int)}.
     *
     * @param prompt View to use as an informational prompt.
     */
    public void setPromptView(@Nullable View prompt) {
        boolean showing = isShowing();
        if (showing) {
            removePromptView();
        }
        mPromptView = prompt;
        if (showing) {
            show();
        }
    }

    /**
     * Post a {@link #show()} call to the UI thread.
     */
    public void postShow() {
        Core.getUiHandler().post(mShowDropDownRunnable);
    }

    /**
     * Show the popup list. If the list is already showing, this method
     * will recalculate the popup's size and position.
     */
    @Override
    public void show() {
        // Fixed by Modern UI
        // buildDropDown() needs to know overlapAnchor or not, if set
        if (mOverlapAnchorSet) {
            mPopup.setOverlapAnchor(mOverlapAnchor);
        }
        int height = buildDropDown();

        if (mPopup.isShowing()) {
            if (!mDropDownAnchorView.isAttachedToWindow()) {
                //Don't update position if the anchor view is detached from window.
                return;
            }
            final int widthSpec;
            if (mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The call to PopupWindow's update method below can accept -1 for any
                // value you do not want to update.
                widthSpec = -1;
            } else if (mDropDownWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                widthSpec = mDropDownAnchorView.getWidth();
            } else {
                widthSpec = mDropDownWidth;
            }

            final int heightSpec;
            if (mDropDownHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The call to PopupWindow's update method below can accept -1 for any
                // value you do not want to update.
                heightSpec = height;
                mPopup.setWidth(mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT ?
                        ViewGroup.LayoutParams.MATCH_PARENT : 0);
                mPopup.setHeight(0);
            } else if (mDropDownHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                heightSpec = height;
            } else {
                heightSpec = mDropDownHeight;
            }

            mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && !mDropDownAlwaysVisible);

            mPopup.update(mDropDownAnchorView, mDropDownHorizontalOffset,
                    mDropDownVerticalOffset, (widthSpec < 0) ? -1 : widthSpec,
                    (heightSpec < 0) ? -1 : heightSpec);
            mPopup.getContentView().restoreDefaultFocus();
        } else {
            final int widthSpec;
            if (mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                widthSpec = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                if (mDropDownWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    widthSpec = mDropDownAnchorView.getWidth();
                } else {
                    widthSpec = mDropDownWidth;
                }
            }

            final int heightSpec;
            if (mDropDownHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                heightSpec = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                if (mDropDownHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    heightSpec = height;
                } else {
                    heightSpec = mDropDownHeight;
                }
            }

            mPopup.setWidth(widthSpec);
            mPopup.setHeight(heightSpec);
            mPopup.setIsClippedToScreen(true);

            // use outside touchable to dismiss drop down when touching outside of it, so
            // only set this if the dropdown is not always visible
            mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && !mDropDownAlwaysVisible);
            mPopup.setTouchInterceptor(mTouchInterceptor);
            mPopup.setEpicenterBounds(mEpicenterBounds);
            //mPopup.setOverlapAnchor(mOverlapAnchor); // move to head
            mPopup.showAsDropDown(mDropDownAnchorView, mDropDownHorizontalOffset,
                    mDropDownVerticalOffset, mDropDownGravity);
            mDropDownList.setSelection(ListView.INVALID_POSITION);
            mPopup.getContentView().restoreDefaultFocus();

            if (!mModal || mDropDownList.isInTouchMode()) {
                clearListSelection();
            }
            if (!mModal) {
                Core.getUiHandler().post(mHideSelector);
            }
        }
    }

    /**
     * Dismiss the popup window.
     */
    @Override
    public void dismiss() {
        mPopup.dismiss();
        removePromptView();
        mPopup.setContentView(null);
        mDropDownList = null;
        Core.getUiHandler().removeCallbacks(mResizePopupRunnable);
    }

    /**
     * Remove existing exit transition from PopupWindow and force immediate dismissal.
     *
     * @hide
     */
    public void dismissImmediate() {
        mPopup.setExitTransition(null);
        dismiss();
    }

    /**
     * Set a listener to receive a callback when the popup is dismissed.
     *
     * @param listener Listener that will be notified when the popup is dismissed.
     */
    public void setOnDismissListener(@Nullable PopupWindow.OnDismissListener listener) {
        mPopup.setOnDismissListener(listener);
    }

    private void removePromptView() {
        if (mPromptView != null) {
            final ViewParent parent = mPromptView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mPromptView);
            }
        }
    }

    /**
     * Set the selected position of the list.
     * Only valid when {@link #isShowing()} == {@code true}.
     *
     * @param position List position to set as selected.
     */
    public void setSelection(int position) {
        DropDownListView list = mDropDownList;
        if (isShowing() && list != null) {
            list.setListSelectionHidden(false);
            list.setSelection(position);
            if (list.getChoiceMode() != ListView.CHOICE_MODE_NONE) {
                list.setItemChecked(position, true);
            }
        }
    }

    /**
     * Clear any current list selection.
     * Only valid when {@link #isShowing()} == {@code true}.
     */
    public void clearListSelection() {
        final DropDownListView list = mDropDownList;
        if (list != null) {
            // WARNING: Please read the comment where mListSelectionHidden is declared
            list.setListSelectionHidden(true);
            list.hideSelector();
            list.requestLayout();
        }
    }

    /**
     * @return {@code true} if the popup is currently showing, {@code false} otherwise.
     */
    @Override
    public boolean isShowing() {
        return mPopup.isShowing();
    }

    /**
     * Perform an item click operation on the specified list adapter position.
     *
     * @param position Adapter position for performing the click
     * @return true if the click action could be performed, false if not.
     * (e.g. if the popup was not showing, this method would return false.)
     */
    public boolean performItemClick(int position) {
        if (isShowing()) {
            if (mItemClickListener != null) {
                final DropDownListView list = mDropDownList;
                final View child = list.getChildAt(position - list.getFirstVisiblePosition());
                final ListAdapter adapter = list.getAdapter();
                mItemClickListener.onItemClick(list, child, position, adapter.getItemId(position));
            }
            return true;
        }
        return false;
    }

    /**
     * @return The currently selected item or null if the popup is not showing.
     */
    @Nullable
    public Object getSelectedItem() {
        if (!isShowing()) {
            return null;
        }
        return mDropDownList.getSelectedItem();
    }

    /**
     * @return The position of the currently selected item or {@link ListView#INVALID_POSITION}
     * if {@link #isShowing()} == {@code false}.
     * @see ListView#getSelectedItemPosition()
     */
    public int getSelectedItemPosition() {
        if (!isShowing()) {
            return ListView.INVALID_POSITION;
        }
        return mDropDownList.getSelectedItemPosition();
    }

    /**
     * @return The ID of the currently selected item or {@link ListView#INVALID_ROW_ID}
     * if {@link #isShowing()} == {@code false}.
     * @see ListView#getSelectedItemId()
     */
    public long getSelectedItemId() {
        if (!isShowing()) {
            return ListView.INVALID_ROW_ID;
        }
        return mDropDownList.getSelectedItemId();
    }

    /**
     * @return The View for the currently selected item or null if
     * {@link #isShowing()} == {@code false}.
     * @see ListView#getSelectedView()
     */
    @Nullable
    public View getSelectedView() {
        if (!isShowing()) {
            return null;
        }
        return mDropDownList.getSelectedView();
    }

    /**
     * @return The {@link ListView} displayed within the popup window.
     * Only valid when {@link #isShowing()} == {@code true}.
     */
    @Nullable
    @Override
    public ListView getListView() {
        return mDropDownList;
    }

    @NonNull
    DropDownListView createDropDownListView(Context context, boolean hijackFocus) {
        return new DropDownListView(context, hijackFocus);
    }

    /**
     * <p>Builds the popup window's content and returns the height the popup
     * should have. Returns -1 when the content already exists.</p>
     *
     * @return the content's height or -1 if content already exists
     */
    private int buildDropDown() {
        ViewGroup dropDownView;
        int otherHeights = 0;

        if (mDropDownList == null) {
            /*
             * This Runnable exists for the sole purpose of checking if the view layout has got
             * completed and if so call showDropDown to display the drop down. This is used to show
             * the drop down as soon as possible after user opens up the search dialog, without
             * waiting for the normal UI pipeline to do it's job which is slower than this method.
             */
            mShowDropDownRunnable = () -> {
                // View layout should be all done before displaying the drop down.
                View view = getAnchorView();
                if (view != null && view.isAttachedToWindow()) {
                    show();
                }
            };

            mDropDownList = createDropDownListView(mContext, !mModal);
            if (mDropDownListHighlight != null) {
                mDropDownList.setSelector(mDropDownListHighlight);
            }
            mDropDownList.setAdapter(mAdapter);
            mDropDownList.setOnItemClickListener(mItemClickListener);
            mDropDownList.setFocusable(true);
            mDropDownList.setFocusableInTouchMode(true);
            mDropDownList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    if (position != -1) {
                        DropDownListView dropDownList = mDropDownList;

                        if (dropDownList != null) {
                            dropDownList.setListSelectionHidden(false);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            if (mItemSelectedListener != null) {
                mDropDownList.setOnItemSelectedListener(mItemSelectedListener);
            }

            dropDownView = mDropDownList;

            View hintView = mPromptView;
            if (hintView != null) {
                // if a hint has been specified, we accomodate more space for it and
                // add a text view in the drop down menu, at the bottom of the list
                LinearLayout hintContainer = new LinearLayout(mContext);
                hintContainer.setOrientation(LinearLayout.VERTICAL);

                LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
                );

                switch (mPromptPosition) {
                    case POSITION_PROMPT_BELOW -> {
                        hintContainer.addView(dropDownView, hintParams);
                        hintContainer.addView(hintView);
                    }
                    case POSITION_PROMPT_ABOVE -> {
                        hintContainer.addView(hintView);
                        hintContainer.addView(dropDownView, hintParams);
                    }
                    default -> throw new IllegalStateException();
                }

                // Measure the hint's height to find how much more vertical
                // space we need to add to the drop down's height.
                final int widthSize;
                final int widthMode;
                if (mDropDownWidth >= 0) {
                    widthMode = MeasureSpec.AT_MOST;
                    widthSize = mDropDownWidth;
                } else {
                    widthMode = MeasureSpec.UNSPECIFIED;
                    widthSize = 0;
                }
                final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);
                final int heightSpec = MeasureSpec.UNSPECIFIED;
                hintView.measure(widthSpec, heightSpec);

                hintParams = (LinearLayout.LayoutParams) hintView.getLayoutParams();
                otherHeights = hintView.getMeasuredHeight() + hintParams.topMargin
                        + hintParams.bottomMargin;

                dropDownView = hintContainer;
            }

            mPopup.setContentView(dropDownView);
        } else {
            final View view = mPromptView;
            if (view != null) {
                LinearLayout.LayoutParams hintParams =
                        (LinearLayout.LayoutParams) view.getLayoutParams();
                otherHeights = view.getMeasuredHeight() + hintParams.topMargin
                        + hintParams.bottomMargin;
            }
        }

        // getMaxAvailableHeight() subtracts the padding, so we put it back
        // to get the available height for the whole window.
        final int padding;
        final Drawable background = mPopup.getBackground();
        if (background != null) {
            background.getPadding(mTempRect);
            padding = mTempRect.top + mTempRect.bottom;

            // If we don't have an explicit vertical offset, determine one from
            // the window background so that content will line up.
            if (!mDropDownVerticalOffsetSet) {
                mDropDownVerticalOffset = -mTempRect.top;
            }
        } else {
            mTempRect.setEmpty();
            padding = 0;
        }

        final int maxHeight = mPopup.getMaxAvailableHeight(
                mDropDownAnchorView, mDropDownVerticalOffset);
        if (mDropDownAlwaysVisible || mDropDownHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            return maxHeight + padding;
        }

        final int childWidthSpec = switch (mDropDownWidth) {
            case ViewGroup.LayoutParams.WRAP_CONTENT -> MeasureSpec.makeMeasureSpec(
                    mDropDownAnchorView.getRootView().getMeasuredWidth()
                            - (mTempRect.left + mTempRect.right),
                    MeasureSpec.AT_MOST);
            case ViewGroup.LayoutParams.MATCH_PARENT -> MeasureSpec.makeMeasureSpec(
                    mDropDownAnchorView.getRootView().getMeasuredWidth()
                            - (mTempRect.left + mTempRect.right),
                    MeasureSpec.EXACTLY);
            default -> MeasureSpec.makeMeasureSpec(mDropDownWidth, MeasureSpec.EXACTLY);
        };

        // Add padding only if the list has items in it, that way we don't show
        // the popup if it is not needed.
        final int listContent = mDropDownList.measureHeightOfChildren(childWidthSpec,
                0, DropDownListView.NO_POSITION, maxHeight - otherHeights, -1);
        if (listContent > 0) {
            final int listPadding = mDropDownList.getPaddingTop()
                    + mDropDownList.getPaddingBottom();
            otherHeights += padding + listPadding;
        }

        return listContent + otherHeights;
    }

    @ApiStatus.Internal
    public void setOverlapAnchor(boolean overlap) {
        mOverlapAnchor = overlap;
        mOverlapAnchorSet = true;
    }

    private class PopupDataSetObserver implements DataSetObserver {

        @Override
        public void onChanged() {
            if (isShowing()) {
                // Resize the popup to fit new content
                show();
            }
        }

        @Override
        public void onInvalidated() {
            dismiss();
        }
    }

    private class ListSelectorHider implements Runnable {

        @Override
        public void run() {
            clearListSelection();
        }
    }

    private class ResizePopupRunnable implements Runnable {

        @Override
        public void run() {
            if (mDropDownList != null && mDropDownList.isAttachedToWindow()
                    && mDropDownList.getCount() > mDropDownList.getChildCount()
                    && mDropDownList.getChildCount() <= mListItemExpandMaximum) {
                show();
            }
        }
    }

    private class PopupTouchInterceptor implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            final int x = (int) event.getX();
            final int y = (int) event.getY();

            if (action == MotionEvent.ACTION_DOWN &&
                    mPopup != null && mPopup.isShowing() &&
                    (x >= 0 && x < mPopup.getWidth() && y >= 0 && y < mPopup.getHeight())) {
                Core.getUiHandler().postDelayed(mResizePopupRunnable, EXPAND_LIST_TIMEOUT);
            } else if (action == MotionEvent.ACTION_UP) {
                Core.getUiHandler().removeCallbacks(mResizePopupRunnable);
            }
            return false;
        }
    }
}
