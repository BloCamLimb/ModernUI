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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.view.*;
import icyllis.modernui.view.menu.ShowableListMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A view that displays one child at a time and lets the user pick among them.
 * The items in the Spinner come from the {@link Adapter} associated with
 * this view.
 *
 * <p>See the <a href="https://developer.android.com/guide/topics/ui/controls/spinner">Spinners</a> guide.</p>
 */
public class Spinner extends AbsSpinner {

    // Only measure this many items to get a decent max width.
    private static final int MAX_ITEMS_MEASURED = 15;

    private final Rect mTempRect = new Rect();

    private final DropdownPopup mPopup;
    int mDropDownWidth;

    /**
     * Forwarding listener used to implement drag-to-open.
     */
    private final ForwardingListener mForwardingListener;

    private int mGravity;

    public Spinner() {
        mPopup = new DropdownPopup();
        //mPopup.setOverlapAnchor(true);
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
        mDropDownWidth = LayoutParams.WRAP_CONTENT;
        mForwardingListener = new ForwardingListener(this) {
            @Override
            public ShowableListMenu getPopup() {
                return mPopup;
            }

            @Override
            public boolean onForwardingStarted() {
                if (!mPopup.isShowing()) {
                    mPopup.show(getTextDirection(), getTextAlignment());
                }
                return true;
            }
        };
        mGravity = Gravity.START | Gravity.CENTER_VERTICAL;
        setClickable(true);
    }

    /**
     * Set the background drawable for the spinner's popup window of choices.
     *
     * @param background Background drawable
     */
    public void setPopupBackgroundDrawable(@Nullable Drawable background) {
        mPopup.setBackgroundDrawable(background);
    }

    /**
     * Get the background drawable for the spinner's popup window of choices.
     *
     * @return Background drawable
     */
    @Nullable
    public Drawable getPopupBackground() {
        return mPopup.getBackground();
    }

    /**
     * Set a vertical offset in pixels for the spinner's popup window of choices.
     *
     * @param pixels Vertical offset in pixels
     */
    public void setDropDownVerticalOffset(int pixels) {
        mPopup.setVerticalOffset(pixels);
    }

    /**
     * Get the configured vertical offset in pixels for the spinner's popup window of choices.
     *
     * @return Vertical offset in pixels
     */
    public int getDropDownVerticalOffset() {
        return mPopup.getVerticalOffset();
    }

    /**
     * Set a horizontal offset in pixels for the spinner's popup window of choices.
     *
     * @param pixels Horizontal offset in pixels
     */
    public void setDropDownHorizontalOffset(int pixels) {
        mPopup.setHorizontalOffset(pixels);
    }

    /**
     * Get the configured horizontal offset in pixels for the spinner's popup window of choices.
     *
     * @return Horizontal offset in pixels
     */
    public int getDropDownHorizontalOffset() {
        return mPopup.getHorizontalOffset();
    }

    /**
     * Set the width of the spinner's popup window of choices in pixels. This value
     * may also be set to {@link LayoutParams#MATCH_PARENT}
     * to match the width of the Spinner itself, or
     * {@link LayoutParams#WRAP_CONTENT} to wrap to the measured size
     * of contained dropdown list items.
     *
     * @param pixels Width in pixels, WRAP_CONTENT, or MATCH_PARENT
     */
    public void setDropDownWidth(int pixels) {
        mDropDownWidth = pixels;
    }

    /**
     * Get the configured width of the spinner's popup window of choices in pixels.
     * The returned value may also be {@link LayoutParams#MATCH_PARENT}
     * meaning the popup window will match the width of the Spinner itself, or
     * {@link LayoutParams#WRAP_CONTENT} to wrap to the measured size
     * of contained dropdown list items.
     *
     * @return Width in pixels, WRAP_CONTENT, or MATCH_PARENT
     */
    public int getDropDownWidth() {
        return mDropDownWidth;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setEnabled(enabled);
        }
    }

    /**
     * Describes how the selected item view is positioned. Currently, only the horizontal component
     * is used. The default is {@link Gravity#START}.
     *
     * @param gravity See {@link Gravity}
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.START;
            }
            mGravity = gravity;
            requestLayout();
        }
    }

    /**
     * Describes how the selected item view is positioned. The default is {@link Gravity#START}.
     *
     * @return A {@link Gravity Gravity} value
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Sets the {@link SpinnerAdapter} used to provide the data which backs
     * this Spinner.
     * <p>
     * Spinner overrides {@link Adapter#getViewTypeCount()} on the
     * Adapter associated with this view. Calling
     * {@link Adapter#getItemViewType(int) getItemViewType(int)} on the object
     * returned from {@link #getAdapter()} will always return 0. Calling
     * {@link Adapter#getViewTypeCount() getViewTypeCount()} will always return
     * 1. Attempting to set an adapter with more than one view type will throw an
     * {@link IllegalArgumentException}.
     *
     * @param adapter the adapter to set
     * @throws IllegalArgumentException if the adapter has more than one view type
     * @see AbsSpinner#setAdapter(SpinnerAdapter)
     */
    @Override
    public void setAdapter(@Nullable SpinnerAdapter adapter) {
        super.setAdapter(adapter);

        mRecycler.clear();

        if (adapter != null && adapter.getViewTypeCount() != 1) {
            throw new IllegalArgumentException("Spinner adapter view type count must be 1");
        }

        mPopup.setAdapter(new DropDownAdapter(adapter));
    }

    @Override
    public int getBaseline() {
        View child = null;

        if (getChildCount() > 0) {
            child = getChildAt(0);
        } else if (mAdapter != null && mAdapter.getCount() > 0) {
            child = makeView(0, false);
            mRecycler.put(0, child);
        }

        if (child != null) {
            final int childBaseline = child.getBaseline();
            return childBaseline >= 0 ? child.getTop() + childBaseline : -1;
        } else {
            return -1;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    /**
     * <p>A spinner does not support item click events. Calling this method
     * will raise an exception.</p>
     * <p>Instead use {@link AdapterView#setOnItemSelectedListener}.
     *
     * @param l this listener will be ignored
     */
    @Override
    public void setOnItemClickListener(@Nullable OnItemClickListener l) {
        throw new UnsupportedOperationException("setOnItemClickListener cannot be used with a spinner.");
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        if (mForwardingListener != null && mForwardingListener.onTouch(this, event)) {
            return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mPopup != null && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            final int measuredWidth = getMeasuredWidth();
            setMeasuredDimension(Math.min(Math.max(measuredWidth,
                                    measureContentWidth(getAdapter(), getBackground())),
                            MeasureSpec.getSize(widthMeasureSpec)),
                    getMeasuredHeight());
        }
    }

    /**
     * @see #onLayout(boolean, int, int, int, int)
     * <p>
     * Creates and positions all views
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mInLayout = true;
        positionViews(0, false);
        mInLayout = false;
    }

    /**
     * Creates and positions all views for this Spinner.
     *
     * @param delta Change in the selected position. +1 means selection is moving to the right,
     *              so views are scrolling to the left. -1 means selection is moving to the left.
     */
    @Override
    void positionViews(int delta, boolean animate) {
        int childrenLeft = mSpinnerPaddingLeft;
        int childrenWidth = getWidth() - mSpinnerPaddingLeft - mSpinnerPaddingRight;

        if (mDataChanged) {
            handleDataChanged();
        }

        // Handle the empty set by removing all views
        if (mItemCount == 0) {
            resetList();
            return;
        }

        if (mNextSelectedPosition >= 0) {
            setSelectedPositionInt(mNextSelectedPosition);
        }

        recycleAllViews();

        // Clear out old views
        removeAllViewsInLayout();

        // Make selected view and position it
        mFirstPosition = mSelectedPosition;

        if (mAdapter != null) {
            View sel = makeView(mSelectedPosition, true);
            int width = sel.getMeasuredWidth();
            int selectedOffset = childrenLeft;
            final int layoutDirection = getLayoutDirection();
            final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
            switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.CENTER_HORIZONTAL -> selectedOffset = childrenLeft + (childrenWidth / 2) - (width / 2);
                case Gravity.RIGHT -> selectedOffset = childrenLeft + childrenWidth - width;
            }
            sel.offsetLeftAndRight(selectedOffset);
        }

        // Flush any cached views that did not get reused above
        mRecycler.clear();

        invalidate();

        checkSelectionChanged();

        mDataChanged = false;
        mNeedSync = false;
        setNextSelectedPositionInt(mSelectedPosition);
    }

    /**
     * Obtain a view, either by pulling an existing view from the recycler or
     * by getting a new one from the adapter. If we are animating, make sure
     * there is enough information in the view's layout parameters to animate
     * from the old to new positions.
     *
     * @param position Position in the spinner for the view to obtain
     * @param addChild true to add the child to the spinner, false to obtain and configure only.
     * @return A view for the given position
     */
    private View makeView(int position, boolean addChild) {
        View child;

        if (!mDataChanged) {
            child = mRecycler.get(position);
            if (child != null) {
                // Position the view
                setUpChild(child, addChild);

                return child;
            }
        }

        // Nothing found in the recycler -- ask the adapter for a view
        child = mAdapter.getView(position, null, this);

        // Position the view
        setUpChild(child, addChild);

        return child;
    }

    /**
     * Helper for makeAndAddView to set the position of a view
     * and fill out its layout paramters.
     *
     * @param child    The view to position
     * @param addChild true if the child should be added to the Spinner during setup
     */
    private void setUpChild(@Nonnull View child, boolean addChild) {

        // Respect layout params that are already in the view. Otherwise
        // make some up...
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp == null) {
            lp = generateDefaultLayoutParams();
        }

        addViewInLayout(child, 0, lp);

        child.setSelected(hasFocus());
        child.setEnabled(isEnabled());

        // Get measure specs
        int childHeightSpec = ViewGroup.getChildMeasureSpec(mHeightMeasureSpec,
                mSpinnerPaddingTop + mSpinnerPaddingBottom, lp.height);
        int childWidthSpec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec,
                mSpinnerPaddingLeft + mSpinnerPaddingRight, lp.width);

        // Measure child
        child.measure(childWidthSpec, childHeightSpec);

        int childLeft;
        int childRight;

        // Position vertically based on gravity setting
        int childTop = mSpinnerPaddingTop
                + ((getMeasuredHeight() - mSpinnerPaddingBottom -
                mSpinnerPaddingTop - child.getMeasuredHeight()) / 2);
        int childBottom = childTop + child.getMeasuredHeight();

        int width = child.getMeasuredWidth();
        childLeft = 0;
        childRight = childLeft + width;

        child.layout(childLeft, childTop, childRight, childBottom);

        if (!addChild) {
            removeViewInLayout(child);
        }
    }

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();

        if (!handled) {
            if (!mPopup.isShowing()) {
                mPopup.show(getTextDirection(), getTextAlignment());
            }
        }

        return true;
    }

    int measureContentWidth(@Nullable SpinnerAdapter adapter, @Nullable Drawable background) {
        if (adapter == null) {
            return 0;
        }

        int width = 0;
        View itemView = null;
        int itemType = 0;
        final int widthMeasureSpec =
                MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec =
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.UNSPECIFIED);

        // Make sure the number of items we'll measure is capped. If it's a huge data set
        // with wildly varying sizes, oh well.
        int start = Math.max(0, getSelectedItemPosition());
        final int end = Math.min(adapter.getCount(), start + MAX_ITEMS_MEASURED);
        final int count = end - start;
        start = Math.max(0, start - (MAX_ITEMS_MEASURED - count));
        for (int i = start; i < end; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }
            itemView = adapter.getView(i, itemView, this);
            if (itemView.getLayoutParams() == null) {
                itemView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }

        // Add background padding to measured width
        if (background != null) {
            background.getPadding(mTempRect);
            width += mTempRect.left + mTempRect.right;
        }

        return width;
    }

    /**
     * <p>Wrapper class for an Adapter. Transforms the embedded Adapter instance
     * into a ListAdapter.</p>
     */
    private static class DropDownAdapter implements ListAdapter, SpinnerAdapter {

        private final SpinnerAdapter mAdapter;
        private final ListAdapter mListAdapter;

        /**
         * Creates a new ListAdapter wrapper for the specified adapter.
         *
         * @param adapter the SpinnerAdapter to transform into a ListAdapter
         */
        public DropDownAdapter(@Nullable SpinnerAdapter adapter) {
            mAdapter = adapter;

            if (adapter instanceof ListAdapter) {
                mListAdapter = (ListAdapter) adapter;
            } else {
                mListAdapter = null;
            }
        }

        @Override
        public int getCount() {
            return mAdapter == null ? 0 : mAdapter.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mAdapter == null ? null : mAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return mAdapter == null ? -1 : mAdapter.getItemId(position);
        }

        @Nullable
        @Override
        public View getView(int position, View convertView, @Nonnull ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

        @Nullable
        @Override
        public View getDropDownView(int position, View convertView, @Nonnull ViewGroup parent) {
            return mAdapter == null ? null : mAdapter.getDropDownView(position, convertView, parent);
        }

        @Override
        public boolean hasStableIds() {
            return mAdapter != null && mAdapter.hasStableIds();
        }

        @Override
        public void registerDataSetObserver(@Nonnull DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(@Nonnull DataSetObserver observer) {
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(observer);
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        @Override
        public boolean areAllItemsEnabled() {
            final ListAdapter adapter = mListAdapter;
            if (adapter != null) {
                return adapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        @Override
        public boolean isEnabled(int position) {
            final ListAdapter adapter = mListAdapter;
            if (adapter != null) {
                return adapter.isEnabled(position);
            } else {
                return true;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return mAdapter == null || mAdapter.isEmpty();
        }
    }

    private class DropdownPopup extends ListPopupWindow {

        private ListAdapter mAdapter;

        public DropdownPopup() {
            setAnchorView(Spinner.this);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener((parent, v, position, id) -> {
                Spinner.this.setSelection(position);
                if (mOnItemClickListener != null) {
                    Spinner.this.performItemClick(v, position, mAdapter.getItemId(position));
                }
                dismiss();
            });
        }

        @Override
        public void setAdapter(@Nullable ListAdapter adapter) {
            super.setAdapter(adapter);
            mAdapter = adapter;
        }

        void computeContentWidth() {
            final Drawable background = getBackground();
            int hOffset = 0;
            if (background != null) {
                background.getPadding(mTempRect);
                hOffset = isLayoutRtl() ? mTempRect.right : -mTempRect.left;
            } else {
                mTempRect.left = mTempRect.right = 0;
            }

            final int spinnerPaddingLeft = Spinner.this.getPaddingLeft();
            final int spinnerPaddingRight = Spinner.this.getPaddingRight();
            final int spinnerWidth = Spinner.this.getWidth();

            if (mDropDownWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                int contentWidth = measureContentWidth(
                        (SpinnerAdapter) mAdapter, getBackground());
                final int contentWidthLimit =
                        getRootView().getMeasuredWidth() - mTempRect.left - mTempRect.right;
                if (contentWidth > contentWidthLimit) {
                    contentWidth = contentWidthLimit;
                }
                setContentWidth(Math.max(
                        contentWidth, spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight));
            } else if (mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                setContentWidth(spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight);
            } else {
                setContentWidth(mDropDownWidth);
            }

            if (isLayoutRtl()) {
                hOffset += spinnerWidth - spinnerPaddingRight - getWidth();
            } else {
                hOffset += spinnerPaddingLeft;
            }
            setHorizontalOffset(hOffset);
        }

        public void show(int textDirection, int textAlignment) {
            computeContentWidth();
            super.show();
            final ListView listView = getListView();
            assert listView != null;
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setTextDirection(textDirection);
            listView.setTextAlignment(textAlignment);
            setSelection(Spinner.this.getSelectedItemPosition());
        }
    }
}
