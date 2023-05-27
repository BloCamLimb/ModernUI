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

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.util.SparseArray;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An abstract base class for spinner widgets.
 */
public abstract class AbsSpinner extends AdapterView<SpinnerAdapter> {

    SpinnerAdapter mAdapter;

    int mWidthMeasureSpec;
    int mHeightMeasureSpec;

    int mSelectionPaddingLeft;
    int mSelectionPaddingTop;
    int mSelectionPaddingRight;
    int mSelectionPaddingBottom;
    int mSpinnerPaddingLeft;
    int mSpinnerPaddingTop;
    int mSpinnerPaddingRight;
    int mSpinnerPaddingBottom;

    final RecycleBin mRecycler = new RecycleBin();
    private DataSetObserver mDataSetObserver;

    /**
     * Temporary frame to hold a child View's frame rectangle
     */
    private Rect mTouchFrame;

    AbsSpinner(Context context) {
        super(context);
        setFocusable(true);
        setWillNotDraw(true);
    }

    /**
     * The Adapter is used to provide the data which backs this Spinner.
     * It also provides methods to transform spinner items based on their position
     * relative to the selected item.
     *
     * @param adapter The SpinnerAdapter to use for this Spinner
     */
    @Override
    public void setAdapter(@Nullable SpinnerAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            resetList();
        }

        mAdapter = adapter;

        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        if (mAdapter != null) {
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            int position = mItemCount > 0 ? 0 : INVALID_POSITION;

            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);

            if (mItemCount == 0) {
                // Nothing selected
                checkSelectionChanged();
            }

        } else {
            checkFocus();
            resetList();
            // Nothing selected
            checkSelectionChanged();
        }

        requestLayout();
    }

    /**
     * Clear out all children from the list
     */
    void resetList() {
        mDataChanged = false;
        mNeedSync = false;

        removeAllViewsInLayout();
        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        setSelectedPositionInt(INVALID_POSITION);
        setNextSelectedPositionInt(INVALID_POSITION);
        invalidate();
    }

    /**
     * Figure out the dimensions of this Spinner. The width comes from
     * the widthMeasureSpec as Spinners can't have their width set to
     * UNSPECIFIED. The height is based on the height of the selected item
     * plus padding.
     *
     * @see #measure(int, int)
     */
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        mWidthMeasureSpec = widthMeasureSpec;
        mHeightMeasureSpec = heightMeasureSpec;

        mSpinnerPaddingLeft = Math.max(mPaddingLeft, mSelectionPaddingLeft);
        mSpinnerPaddingTop = Math.max(mPaddingTop, mSelectionPaddingTop);
        mSpinnerPaddingRight = Math.max(mPaddingRight, mSelectionPaddingRight);
        mSpinnerPaddingBottom = Math.max(mPaddingBottom, mSelectionPaddingBottom);

        if (mDataChanged) {
            handleDataChanged();
        }

        int preferredWidth = 0;
        int preferredHeight = 0;
        boolean needsMeasuring = true;

        int selectedPosition = getSelectedItemPosition();
        if (selectedPosition >= 0 && mAdapter != null && selectedPosition < mAdapter.getCount()) {
            // Try looking in the recycler. (Maybe we were measured once already)
            View view = mRecycler.get(selectedPosition);
            if (view == null) {
                // Make a new one
                view = mAdapter.getView(selectedPosition, null, this);
            }

            // Put in recycler for re-measuring and/or layout
            mRecycler.put(selectedPosition, view);

            if (view.getLayoutParams() == null) {
                mBlockLayoutRequests = true;
                view.setLayoutParams(generateDefaultLayoutParams());
                mBlockLayoutRequests = false;
            }
            measureChild(view, widthMeasureSpec, heightMeasureSpec);

            preferredWidth = view.getMeasuredWidth() + mSpinnerPaddingLeft + mSpinnerPaddingRight;
            preferredHeight = view.getMeasuredHeight() + mSpinnerPaddingTop + mSpinnerPaddingBottom;

            needsMeasuring = false;
        }

        if (needsMeasuring) {
            // No views -- just use padding
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                preferredWidth = mSpinnerPaddingLeft + mSpinnerPaddingRight;
            }
            preferredHeight = mSpinnerPaddingTop + mSpinnerPaddingBottom;
        }

        preferredWidth = Math.max(preferredWidth, getSuggestedMinimumWidth());
        preferredHeight = Math.max(preferredHeight, getSuggestedMinimumHeight());

        final int widthSize = resolveSizeAndState(preferredWidth, widthMeasureSpec, 0);
        final int heightSize = resolveSizeAndState(preferredHeight, heightMeasureSpec, 0);

        setMeasuredDimension(widthSize, heightSize);
    }

    @Nonnull
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    void recycleAllViews() {
        final int childCount = getChildCount();
        final int position = mFirstPosition;

        // All views go in recycler
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            int index = position + i;
            mRecycler.put(index, v);
        }
    }

    /**
     * Jump directly to a specific item in the adapter data.
     */
    public void setSelection(int position, boolean animate) {
        // Animate only if requested position is already on screen somewhere
        boolean shouldAnimate = animate && mFirstPosition <= position &&
                position <= mFirstPosition + getChildCount() - 1;
        setSelectionInt(position, shouldAnimate);
    }

    @Override
    public void setSelection(int position) {
        setNextSelectedPositionInt(position);
        requestLayout();
        invalidate();
    }

    /**
     * Makes the item at the supplied position selected.
     *
     * @param position Position to select
     * @param animate  Should the transition be animated
     */
    void setSelectionInt(int position, boolean animate) {
        if (position != mOldSelectedPosition) {
            mBlockLayoutRequests = true;
            int delta = position - mSelectedPosition;
            setNextSelectedPositionInt(position);
            positionViews(delta, animate);
            mBlockLayoutRequests = false;
        }
    }

    abstract void positionViews(int delta, boolean animate);

    @Override
    public View getSelectedView() {
        if (mItemCount > 0 && mSelectedPosition >= 0) {
            return getChildAt(mSelectedPosition - mFirstPosition);
        } else {
            return null;
        }
    }

    /**
     * Override to prevent spamming ourselves with layout requests
     * as we place views
     *
     * @see View#requestLayout()
     */
    @Override
    public void requestLayout() {
        if (!mBlockLayoutRequests) {
            super.requestLayout();
        }
    }

    @Override
    public SpinnerAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public int getCount() {
        return mItemCount;
    }

    /**
     * Maps a point to a position in the list.
     *
     * @param x X in local coordinate
     * @param y Y in local coordinate
     * @return The position of the item which contains the specified point, or
     * {@link #INVALID_POSITION} if the point does not intersect an item.
     */
    public int pointToPosition(int x, int y) {
        Rect frame = mTouchFrame;
        if (frame == null) {
            mTouchFrame = new Rect();
            frame = mTouchFrame;
        }

        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return mFirstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }

    class RecycleBin {

        private final SparseArray<View> mScrapHeap = new SparseArray<>();

        void put(int position, @Nonnull View v) {
            mScrapHeap.put(position, v);
        }

        @Nullable
        View get(int position) {
            return mScrapHeap.remove(position);
        }

        void clear() {
            final SparseArray<View> heap = mScrapHeap;
            for (int i = 0, e = heap.size(); i < e; i++) {
                final View v = heap.valueAt(i);
                removeDetachedView(v, true);
            }
            heap.clear();
        }
    }
}
