/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2006 The Android Open Source Project
 *   Copyright (c) 2013 Etsy
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.util.Log;
import icyllis.modernui.util.SparseArray;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import org.intellij.lang.annotations.MagicConstant;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Arrays;

/**
 * A view that shows item in two-dimensional scrolling staggered grid. The items in the
 * grid come from the {@link ListAdapter} associated with this view.
 *
 * @since 3.12.1
 */
// Maintained by Modern UI
public class StaggeredGridView extends AbsListView {
    //TODO vertical padding is wrong at top, when scroll up to top;
    // focus/selection movement

    private static final Marker MARKER = MarkerFactory.getMarker("StaggeredGridView");

    private static final boolean DBG = false;

    /**
     * Disables stretching.
     *
     * @see #setStretchMode(int)
     */
    public static final int NO_STRETCH = GridView.NO_STRETCH;
    /**
     * Stretches the spacing between columns.
     *
     * @see #setStretchMode(int)
     */
    public static final int STRETCH_SPACING = GridView.STRETCH_SPACING;
    /**
     * Stretches columns.
     *
     * @see #setStretchMode(int)
     */
    public static final int STRETCH_COLUMN_WIDTH = GridView.STRETCH_COLUMN_WIDTH;

    /**
     * Creates as many columns as can fit on screen.
     *
     * @see #setNumColumns(int)
     */
    public static final int AUTO_FIT = GridView.AUTO_FIT;

    private int mNumColumns;

    private int mHorizontalSpacing = 0;
    private int mRequestedHorizontalSpacing;
    private int mVerticalSpacing = 0;
    private int mStretchMode = STRETCH_COLUMN_WIDTH;
    private int mColumnWidth;
    private int mRequestedColumnWidth;
    private int mRequestedNumColumns = AUTO_FIT;

    private int mGravity = Gravity.START;

    private boolean mNeedColumnSync;

    static class LayoutRecord {
        int column;
        int height;
        boolean isHeaderOrFooter;
    }

    private SparseArray<LayoutRecord> mPositionData = new SparseArray<>();

    /**
     * The location of the top of each top item added in each column.
     */
    private int[] mColumnTops;

    /**
     * The location of the bottom of each bottom item added in each column.
     */
    private int[] mColumnBottoms;

    public StaggeredGridView(Context context) {
        super(context);
    }

    @Override
    public ListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        resetList();
        mRecycler.clear();
        mAdapter = adapter;

        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        // AbsListView#setAdapter will update choice mode states.
        super.setAdapter(adapter);

        if (mAdapter != null) {
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            mDataChanged = true;
            checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());

            int position = lookForSelectablePosition(0, true);
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);

            if (mItemCount == 0) {
                // Nothing selected
                checkSelectionChanged();
            }
        } else {
            checkFocus();
            // Nothing selected
            checkSelectionChanged();
        }

        requestLayout();
    }

    /**
     * The list is empty. Clear everything out.
     */
    @Override
    void resetList() {
        // The parent's resetList() will remove all views from the layout so we need to
        // cleanup the state of our footers and headers
        //TODO
        //clearRecycledState(mHeaderViewInfos);
        //clearRecycledState(mFooterViewInfos);

        super.resetList();

        mLayoutMode = LAYOUT_NORMAL;
    }

    @Override
    int lookForSelectablePosition(int position, boolean lookDown) {
        final ListAdapter adapter = mAdapter;
        if (adapter == null || isInTouchMode()) {
            return INVALID_POSITION;
        }

        if (position < 0 || position >= mItemCount) {
            return INVALID_POSITION;
        }
        return position;
    }

    @Override
    void fillGap(boolean down) {
        final int count = getChildCount();
        if (down) {
            // fill down from the top of the position below our last
            int position = mFirstPosition + count;
            final int startOffset = getChildTop(position);
            fillDown(position, startOffset);
        } else {
            // fill up from the bottom of the position above our first.
            int position = mFirstPosition - 1;
            final int startOffset = getChildBottom(position);
            fillUp(position, startOffset);
        }
        if (down) {
            correctTooHigh(getChildCount());
        } else {
            correctTooLow(getChildCount());
        }
        // fix vertical gaps when hitting the top after a column count change
        if (!down) {
            // only when scrolling back up
            alignTops();
        }
    }

    private View fillDown(int pos, int nextTop) {
        if (DBG) Log.LOGGER.debug(MARKER, "fillDown - pos:{} nextTop:{}", pos, nextTop);

        View selectedView = null;

        int end = getHeight();
        if (hasBooleanFlag(CLIP_TO_PADDING_MASK)) {
            end -= mListPadding.bottom;
        }

        while (nextTop < end && pos < mItemCount) {
            boolean selected = pos == mSelectedPosition;
            View child = makeAndAddView(pos, nextTop, true, selected);
            if (selected) {
                selectedView = child;
            }
            pos++;
            nextTop = getNextChildDownsTop(pos); // = child.getBottom();
        }

        return selectedView;
    }

    private View fillUp(int pos, int nextBottom) {
        if (DBG) Log.LOGGER.debug(MARKER, "fillUp - position:{} nextBottom:{}", pos, nextBottom);

        View selectedView = null;

        int end = 0;
        if (hasBooleanFlag(CLIP_TO_PADDING_MASK)) {
            end = mListPadding.top;
        }

        while ((nextBottom > end || getLowestPositionedTop() > end) && pos >= 0) {
            boolean selected = pos == mSelectedPosition;
            View child = makeAndAddView(pos, nextBottom, false, selected);
            if (selected) {
                selectedView = child;
            }
            pos--;
            nextBottom = getNextChildUpsBottom(pos);
            if (DBG) Log.LOGGER.debug(MARKER, "fillUp next - position:{} nextBottom:{}", pos, nextBottom);
        }

        mFirstPosition = pos + 1;
        return selectedView;
    }

    /**
     * Fills the list from top to bottom, starting with mFirstPosition
     *
     * @param nextTop The location where the top of the first item should be
     *                drawn
     * @return The view that is currently selected
     */
    private View fillFromTop(int nextTop) {
        mFirstPosition = Math.min(mFirstPosition, mSelectedPosition);
        mFirstPosition = Math.min(mFirstPosition, mItemCount - 1);
        if (mFirstPosition < 0) {
            mFirstPosition = 0;
        }
        return fillDown(mFirstPosition, nextTop);
    }

    @Override
    int findMotionRow(int y) {
        int childCount = getChildCount();
        if (childCount > 0) {
            // always from the top
            for (int i = 0; i < childCount; i++) {
                View v = getChildAt(i);
                if (y <= v.getBottom()) {
                    return mFirstPosition + i;
                }
            }
        }
        return INVALID_POSITION;
    }

    /**
     * Put a specific item at a specific location on the screen and then build
     * up and down from there.
     *
     * @param position The reference view to use as the starting point
     * @param top      Pixel offset from the top of this view to the top of the
     *                 reference view.
     * @return The selected view, or null if the selected view is outside the
     * visible area.
     */
    private View fillSpecific(int position, int top) {
        boolean tempIsSelected = position == mSelectedPosition;
        View temp = makeAndAddView(position, top, true, tempIsSelected);
        // Possibly changed again in fillUp if we add rows above this one.
        mFirstPosition = position;

        View above;
        View below;

        int nextBottom = getNextChildUpsBottom(position - 1);
        int nextTop = getNextChildDownsTop(position + 1);

        above = fillUp(position - 1, nextBottom);
        // This will correct for the top of the first view not touching the top of the list
        adjustViewsUpOrDown();
        below = fillDown(position + 1, nextTop);
        int childCount = getChildCount();
        if (childCount > 0) {
            correctTooHigh(childCount);
        }

        if (tempIsSelected) {
            return temp;
        } else if (above != null) {
            return above;
        } else {
            return below;
        }
    }

    /**
     * Check if we have dragged the bottom of the list too high (we have pushed the
     * top element off the top of the screen when we did not need to). Correct by sliding
     * everything back down.
     *
     * @param childCount Number of children
     */
    private void correctTooHigh(int childCount) {
        // First see if the last item is visible. If it is not, it is OK for the
        // top of the list to be pushed up.
        int lastPosition = mFirstPosition + childCount - 1;
        if (lastPosition == mItemCount - 1 && childCount > 0) {

            // ... and its bottom edge
            final int lastBottom = getLowestChildBottom();

            // This is bottom of our drawable area
            final int end = (getBottom() - getTop()) - getListPaddingBottom();

            // This is how far the bottom edge of the last view is from the bottom of the
            // drawable area
            int bottomOffset = end - lastBottom;

            final int firstTop = getHighestChildTop();

            // Make sure we are 1) Too high, and 2) Either there are more rows above the
            // first row or the first row is scrolled off the top of the drawable area
            if (bottomOffset > 0 && (mFirstPosition > 0 || firstTop < getListPaddingTop())) {
                if (mFirstPosition == 0) {
                    // Don't pull the top too far down
                    bottomOffset = Math.min(bottomOffset, getListPaddingTop() - firstTop);
                }
                // Move everything down
                offsetChildrenTopAndBottom(bottomOffset);
                if (mFirstPosition > 0) {
                    // Fill the gap that was opened above mFirstPosition with more rows, if
                    // possible
                    int previousPosition = mFirstPosition - 1;
                    fillUp(previousPosition, getNextChildUpsBottom(previousPosition));
                    // Close up the remaining gap
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    /**
     * Check if we have dragged the bottom of the list too low (we have pushed the
     * bottom element off the bottom of the screen when we did not need to). Correct by sliding
     * everything back up.
     *
     * @param childCount Number of children
     */
    private void correctTooLow(int childCount) {
        // First see if the first item is visible. If it is not, it is OK for the
        // bottom of the list to be pushed down.
        if (mFirstPosition == 0 && childCount > 0) {

            // ... and its top edge
            final int firstTop = getHighestChildTop();

            // This is top of our drawable area
            final int start = getListPaddingTop();

            // This is bottom of our drawable area
            final int end = (getTop() - getBottom()) - getListPaddingBottom();

            // This is how far the top edge of the first view is from the top of the
            // drawable area
            int topOffset = firstTop - start;
            final int lastBottom = getLowestChildBottom();

            int lastPosition = mFirstPosition + childCount - 1;

            // Make sure we are 1) Too low, and 2) Either there are more rows below the
            // last row or the last row is scrolled off the bottom of the drawable area
            if (topOffset > 0) {
                if (lastPosition < mItemCount - 1 || lastBottom > end) {
                    if (lastPosition == mItemCount - 1) {
                        // Don't pull the bottom too far up
                        topOffset = Math.min(topOffset, lastBottom - end);
                    }
                    // Move everything up
                    offsetChildrenTopAndBottom(-topOffset);
                    if (lastPosition < mItemCount - 1) {
                        // Fill the gap that was opened below the last position with more rows, if
                        // possible
                        int nextPosition = lastPosition + 1;
                        fillDown(nextPosition, getNextChildDownsTop(nextPosition));
                        // Close up the remaining gap
                        adjustViewsUpOrDown();
                    }
                } else if (lastPosition == mItemCount - 1) {
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    private boolean determineColumns(int availableSpace) {
        final int requestedHorizontalSpacing = mRequestedHorizontalSpacing;
        final int requestedColumnWidth = mRequestedColumnWidth;
        boolean didNotInitiallyFit = false;

        if (mRequestedNumColumns == AUTO_FIT) {
            if (requestedColumnWidth > 0) {
                // Client told us to pick the number of columns
                mNumColumns = (availableSpace + requestedHorizontalSpacing) /
                        (requestedColumnWidth + requestedHorizontalSpacing);
            } else {
                // Just make up a number if we don't have enough info
                mNumColumns = GridView.DEFAULT_COLUMNS;
            }
        } else {
            // We picked the columns
            mNumColumns = mRequestedNumColumns;
        }

        if (mNumColumns <= 0) {
            mNumColumns = 1;
        }

        int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth)
                - ((mNumColumns - 1) * requestedHorizontalSpacing);

        if (spaceLeftOver < 0) {
            didNotInitiallyFit = true;
        }

        switch (mStretchMode) {
            case NO_STRETCH:
                // Nobody stretches
                mColumnWidth = requestedColumnWidth;
                mHorizontalSpacing = requestedHorizontalSpacing;
                break;

            case STRETCH_COLUMN_WIDTH:
                // Stretch the columns
                mColumnWidth = requestedColumnWidth + spaceLeftOver / mNumColumns;
                if (mNumColumns > 1) {
                    mHorizontalSpacing = requestedHorizontalSpacing;
                } else {
                    mHorizontalSpacing = 0;
                }
                break;

            case STRETCH_SPACING:
                // Stretch the spacing between columns
                mColumnWidth = requestedColumnWidth;
                if (mNumColumns > 1) {
                    mHorizontalSpacing = requestedHorizontalSpacing
                            + spaceLeftOver / (mNumColumns - 1);
                } else {
                    mHorizontalSpacing = spaceLeftOver;
                }
                break;
        }

        return didNotInitiallyFit;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mNeedColumnSync) {
            mNeedColumnSync = false;
            // super will call rememberSyncState()
            int syncPosition = Math.min(mSyncPosition, getCount() - 1);

            SparseArray<LayoutRecord> oldPositionData = mPositionData;
            mPositionData = new SparseArray<>();

            for (int pos = 0; pos < syncPosition; pos++) {
                final LayoutRecord old = oldPositionData.get(pos);
                if (old == null) {
                    break;
                }

                final LayoutRecord rec = getOrCreateRecord(pos);
                final int height = old.height;
                rec.height = height;

                int top;
                int bottom;
                // check for headers
                if (old.isHeaderOrFooter) {
                    // the next top is the bottom for that column
                    top = getLowestPositionedBottom();
                    bottom = top + height;

                    for (int i = 0; i < mNumColumns; i++) {
                        mColumnTops[i] = top;
                        mColumnBottoms[i] = bottom;
                    }

                    rec.isHeaderOrFooter = true;
                } else {
                    // what's the next column down ?
                    final int column = getHighestPositionedBottomColumn();
                    // the next top is the bottom for that column
                    top = mColumnBottoms[column];
                    bottom = top + height + getChildTopMargin(pos) + getChildBottomMargin();

                    mColumnTops[column] = top;
                    mColumnBottoms[column] = bottom;

                    rec.column = column;
                }
            }

            // our sync position will be displayed in this column
            final int syncColumn = getHighestPositionedBottomColumn();
            getOrCreateRecord(syncPosition).column = syncColumn;

            // we want to offset from height of the sync position
            // minus the offset
            int syncToBottom = mColumnBottoms[syncColumn];
            int offset = -syncToBottom + mSpecificTop;
            // offset all columns by
            if (offset != 0) {
                for (int i = 0; i < mNumColumns; i++) {
                    mColumnTops[i] += offset;
                    mColumnBottoms[i] += offset;
                }
            }

            // stash our bottoms in our tops - though these will be copied back to the bottoms
            System.arraycopy(mColumnBottoms, 0, mColumnTops, 0, mNumColumns);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Sets up mListPadding
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED) {
            if (mColumnWidth > 0) {
                widthSize = mColumnWidth + mListPadding.left + mListPadding.right;
            } else {
                widthSize = mListPadding.left + mListPadding.right;
            }
            widthSize += getVerticalScrollbarWidth();
        }

        int availableSpace = widthSize - mListPadding.left - mListPadding.right;
        boolean didNotInitiallyFit = determineColumns(availableSpace);

        if (mColumnTops == null || mColumnTops.length != mNumColumns) {
            mColumnTops = new int[mNumColumns];
            mColumnBottoms = new int[mNumColumns];
            initColumnOffsets();
            if (getCount() > 0 && !mPositionData.isEmpty()) {
                mNeedColumnSync = true;
                // column count is changed due to size change,
                // onSizeChanged() will be called later
            }
        }

        int childHeight = 0;
        int childState = 0;

        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
        final int count = mItemCount;
        if (count > 0 && (heightMode == MeasureSpec.UNSPECIFIED ||
                heightMode == MeasureSpec.AT_MOST)) {
            final View child = obtainView(0, mIsScrap);

            measureScrapChild(child, 0, heightSize);

            childHeight = child.getMeasuredHeight();
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            if (mRecycler.shouldRecycleViewType(
                    ((LayoutParams) child.getLayoutParams()).viewType)) {
                mRecycler.addScrapView(child, 0);
            }
        }

        if (heightMode == MeasureSpec.UNSPECIFIED ||
                heightMode == MeasureSpec.AT_MOST) {
            // just conservatively measure the first child
            int ourSize = mListPadding.top + mListPadding.bottom + childHeight +
                    getVerticalFadingEdgeLength() * 2;
            if (heightMode == MeasureSpec.AT_MOST) {
                heightSize = Math.min(ourSize, heightSize);
            } else {
                heightSize = ourSize;
            }
        }

        if (widthMode == MeasureSpec.AT_MOST) {
            if (didNotInitiallyFit) {
                widthSize |= MEASURED_STATE_TOO_SMALL;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
        mWidthMeasureSpec = widthMeasureSpec;
    }

    private void measureScrapChild(@NonNull View child, int position, int heightHint) {
        LayoutParams p = (LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (LayoutParams) generateDefaultLayoutParams();
            child.setLayoutParams(p);
        }
        p.viewType = mAdapter.getItemViewType(position);
        p.isEnabled = mAdapter.isEnabled(position);
        p.forceAdd = true;

        int childHeightSpec = ViewGroup.getChildMeasureSpec(
                MeasureSpec.makeMeasureSpec(heightHint, MeasureSpec.UNSPECIFIED), 0, p.height);
        int childWidthSpec = ViewGroup.getChildMeasureSpec(
                MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);

        child.measure(childWidthSpec, childHeightSpec);

        // Sync with GridView behavior
        child.forceLayout();
    }

    @Override
    protected void layoutChildren() {
        final boolean blockLayoutRequests = mBlockLayoutRequests;
        if (blockLayoutRequests) {
            return;
        }

        mBlockLayoutRequests = true;

        try {
            super.layoutChildren();

            invalidate();

            if (mAdapter == null) {
                resetList();
                invokeOnItemScrollListener();
                return;
            }

            final int childrenTop = mListPadding.top;
            final int childrenBottom = getHeight() - mListPadding.bottom;
            final int childCount = getChildCount();

            int index;
            int delta = 0;

            View sel;
            View oldSel = null;
            View oldFirst = null;
            View newSel = null;

            // Remember stuff we will need down below
            switch (mLayoutMode) {
                case LAYOUT_SET_SELECTION:
                    index = mNextSelectedPosition - mFirstPosition;
                    if (index >= 0 && index < childCount) {
                        newSel = getChildAt(index);
                    }
                    break;
                case LAYOUT_FORCE_TOP:
                case LAYOUT_FORCE_BOTTOM:
                case LAYOUT_SPECIFIC:
                case LAYOUT_SYNC:
                    break;
                case LAYOUT_MOVE_SELECTION:
                default:
                    // Remember the previously selected view
                    index = mSelectedPosition - mFirstPosition;
                    if (index >= 0 && index < childCount) {
                        oldSel = getChildAt(index);
                    }

                    // Remember the previous first child
                    oldFirst = getChildAt(0);

                    if (mNextSelectedPosition >= 0) {
                        delta = mNextSelectedPosition - mSelectedPosition;
                    }

                    // Caution: newSel might be null
                    newSel = getChildAt(index + delta);
            }


            boolean dataChanged = mDataChanged;
            if (dataChanged) {
                handleDataChanged();
            }

            // Handle the empty set by removing all views that are visible
            // and calling it a day
            if (mItemCount == 0) {
                resetList();
                invokeOnItemScrollListener();
                return;
            } else if (mItemCount != mAdapter.getCount()) {
                throw new IllegalStateException("The content of the adapter has changed but "
                        + "ExtendableListView did not receive a notification. Make sure the content of "
                        + "your adapter is not modified from a background thread, but only "
                        + "from the UI thread. [in ExtendableListView(" + getId() + ", " + getClass()
                        + ") with Adapter(" + mAdapter.getClass() + ")]");
            }

            setSelectedPositionInt(mNextSelectedPosition);

            // Pull all children into the RecycleBin.
            // These views will be reused if possible
            final int firstPosition = mFirstPosition;
            final RecycleBin recycleBin = mRecycler;

            if (dataChanged) {
                for (int i = 0; i < childCount; i++) {
                    recycleBin.addScrapView(getChildAt(i), firstPosition + i);
                }
            } else {
                recycleBin.fillActiveViews(childCount, firstPosition);
            }

            // Clear out old views
            detachAllViewsFromParent();
            recycleBin.removeSkippedScrap();
            // copy the tops into the bottom
            // since we're going to redo a layout pass that will draw down from
            // the top
            System.arraycopy(mColumnTops, 0, mColumnBottoms, 0, mNumColumns);

            switch (mLayoutMode) {
                case LAYOUT_SET_SELECTION:
                    // unsupported
                    sel = null;
                    break;
                case LAYOUT_FORCE_TOP:
                    mFirstPosition = 0;
                    initColumnOffsets();
                    mPositionData.clear();
                    adjustViewsUpOrDown();
                    sel = fillFromTop(childrenTop);
                    adjustViewsUpOrDown();
                    break;
                case LAYOUT_FORCE_BOTTOM:
                    sel = fillUp(mItemCount - 1, childrenBottom);
                    adjustViewsUpOrDown();
                    break;
                case LAYOUT_SPECIFIC:
                    final int selectedPosition = reconcileSelectedPosition();
                    sel = fillSpecific(selectedPosition, mSpecificTop);
                    break;
                case LAYOUT_SYNC:
                    sel = fillSpecific(mSyncPosition, mSpecificTop);
                    break;
                case LAYOUT_MOVE_SELECTION:
                    // unsupported
                    sel = null;
                    break;
                default:
                    if (childCount == 0) {
                        setSelectedPositionInt(mAdapter == null || isInTouchMode() ?
                                INVALID_POSITION : 0);
                        sel = fillFromTop(childrenTop);
                    } else {
                        if (mSelectedPosition >= 0 && mSelectedPosition < mItemCount) {
                            sel = fillSpecific(mSelectedPosition, oldSel == null ?
                                    childrenTop : oldSel.getTop());
                        } else if (mFirstPosition < mItemCount) {
                            sel = fillSpecific(mFirstPosition, oldFirst == null ?
                                    childrenTop : oldFirst.getTop());
                        } else {
                            sel = fillSpecific(0, childrenTop);
                        }
                    }
                    break;
            }

            // Flush any cached views that did not get reused above
            recycleBin.scrapActiveViews();

            if (sel != null) {
                positionSelector(INVALID_POSITION, sel);
                mSelectedTop = sel.getTop();
            } else {
                final boolean inTouchMode = mTouchMode > TOUCH_MODE_DOWN
                        && mTouchMode < TOUCH_MODE_SCROLL;
                if (inTouchMode) {
                    // If the user's finger is down, select the motion position.
                    final View child = getChildAt(mMotionPosition - mFirstPosition);
                    if (child != null) {
                        positionSelector(mMotionPosition, child);
                    }
                } else if (mSelectedPosition != INVALID_POSITION) {
                    // If we had previously positioned the selector somewhere,
                    // put it back there. It might not match up with the data,
                    // but it's transitioning out so it's not a big deal.
                    final View child = getChildAt(mSelectorPosition - mFirstPosition);
                    if (child != null) {
                        positionSelector(mSelectorPosition, child);
                    }
                } else {
                    // Otherwise, clear selection.
                    mSelectedTop = 0;
                    mSelectorRect.setEmpty();
                }
            }

            mLayoutMode = LAYOUT_NORMAL;
            mDataChanged = false;
            if (mPositionScrollAfterLayout != null) {
                post(mPositionScrollAfterLayout);
                mPositionScrollAfterLayout = null;
            }
            mNeedSync = false;
            setNextSelectedPositionInt(mSelectedPosition);

            updateScrollIndicators();

            if (mItemCount > 0) {
                checkSelectionChanged();
            }

            invokeOnItemScrollListener();
        } finally {
            mBlockLayoutRequests = false;
        }
    }

    /**
     * Obtains the view and adds it to our list of children. The view can be
     * made fresh, converted from an unused view, or used as is if it was in
     * the recycle bin.
     *
     * @param position     logical position in the list
     * @param flow         {@code true} to align top edge to y, {@code false} to align
     *                     bottom edge to y
     * @param selected     {@code true} if the position is selected, {@code false}
     *                     otherwise
     * @return the view that was added
     */
    private View makeAndAddView(int position, int y, boolean flow, boolean selected) {
        // Common logic to determine the column index and left
        int childrenLeft;
        if (mAdapter.getItemViewType(position) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            LayoutRecord rec = getOrCreateRecord(position);
            rec.isHeaderOrFooter = true;
            // similar to ListView
            childrenLeft = mListPadding.left;
        } else {
            // do we already have a column for this child position?
            int column = getPositionColumn(position);
            // we don't have the column or it no longer fits in our grid
            final int columnCount = mNumColumns;
            if (column < 0 || column >= columnCount) {
                // if we're going down -
                // get the highest positioned (lowest value)
                // column bottom
                if (flow) {
                    column = getHighestPositionedBottomColumn();
                } else {
                    column = getLowestPositionedTopColumn();
                }
            }
            LayoutRecord rec = getOrCreateRecord(position);
            rec.column = column;
            // similar to GridView
            if (isLayoutRtl()) {
                childrenLeft = getWidth() - mListPadding.right -
                        (mHorizontalSpacing + mColumnWidth) * (mNumColumns - column) + mHorizontalSpacing;
            } else {
                childrenLeft = mListPadding.left +
                        (mHorizontalSpacing + mColumnWidth) * column;
            }
            if (DBG)
                Log.LOGGER.debug(MARKER, "onChildCreated position:{} is in column:{}, childrenLeft:{}",
                    position, column, childrenLeft);
        }

        if (!mDataChanged) {
            // Try to use an existing view for this position
            final View activeView = mRecycler.getActiveView(position);
            if (activeView != null) {
                // Found it -- we're using an existing child
                // This just needs to be positioned
                setupChild(activeView, position, y, flow, childrenLeft, selected, true);
                return activeView;
            }
        }

        // Make a new view for this position, or convert an unused view if
        // possible.
        final View child = obtainView(position, mIsScrap);

        // This needs to be positioned and measured.
        setupChild(child, position, y, flow, childrenLeft, selected, mIsScrap[0]);

        return child;
    }

    /**
     * Adds a view as a child and make sure it is measured (if necessary) and
     * positioned properly.
     *
     * @param child              the view to add
     * @param position           the position of this child
     * @param y                  the y position relative to which this view will be positioned
     * @param flowDown           {@code true} to align top edge to y, {@code false} to
     *                           align bottom edge to y
     * @param childrenLeft       left edge where children should be positioned
     * @param selected           {@code true} if the position is selected, {@code false}
     *                           otherwise
     * @param isAttachedToWindow {@code true} if the view is already attached
     *                           to the window, e.g. whether it was reused, or
     *                           {@code false} otherwise
     */
    private void setupChild(@NonNull View child, int position, int y, boolean flowDown, int childrenLeft,
                            boolean selected, boolean isAttachedToWindow) {
        final boolean isSelected = selected && shouldShowSelector();
        final boolean updateChildSelected = isSelected != child.isSelected();
        final int mode = mTouchMode;
        final boolean isPressed = mode > TOUCH_MODE_DOWN && mode < TOUCH_MODE_SCROLL
                && mMotionPosition == position;
        final boolean updateChildPressed = isPressed != child.isPressed();
        final boolean needToMeasure = !isAttachedToWindow || updateChildSelected
                || child.isLayoutRequested();

        // Respect layout params that are already in the view. Otherwise make
        // some up...
        LayoutParams p = (LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (LayoutParams) generateDefaultLayoutParams();
        }
        p.viewType = mAdapter.getItemViewType(position);
        p.isEnabled = mAdapter.isEnabled(position);

        // Set up view state before attaching the view, since we may need to
        // rely on the jumpDrawablesToCurrentState() call that occurs as part
        // of view attachment.
        if (updateChildSelected) {
            child.setSelected(isSelected);
        }

        if (updateChildPressed) {
            child.setPressed(isPressed);
        }

        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(mCheckStates.get(position));
            } else {
                child.setActivated(mCheckStates.get(position));
            }
        }

        if ((isAttachedToWindow && !p.forceAdd) || (p.recycledHeaderFooter
                && p.viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER)) {
            attachViewToParent(child, flowDown ? -1 : 0, p);

            // If the view was previously attached for a different position,
            // then manually jump the drawables.
            if (isAttachedToWindow
                    && (((AbsListView.LayoutParams) child.getLayoutParams()).scrappedFromPosition)
                    != position) {
                child.jumpDrawablesToCurrentState();
            }
        } else {
            p.forceAdd = false;
            if (p.viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                p.recycledHeaderFooter = true;
            }
            addViewInLayout(child, flowDown ? -1 : 0, p, true);
            // add view in layout will reset the RTL properties. We have to re-resolve them
            child.resolveRtlPropertiesIfNeeded();
        }

        if (needToMeasure) {
            final int childWidthSpec;
            final int childHeightSpec;
            if (p.viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                // Similar to ListView
                childWidthSpec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec,
                        mListPadding.left + mListPadding.right, p.width);
                final int lpHeight = p.height;
                if (lpHeight > 0) {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
                } else {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(),
                            MeasureSpec.UNSPECIFIED);
                }
            } else {
                // Similar to GridView
                childHeightSpec = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);

                childWidthSpec = ViewGroup.getChildMeasureSpec(
                        MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);
            }
            child.measure(childWidthSpec, childHeightSpec);

            if (DBG)
                Log.LOGGER.debug(MARKER, "onMeasureChild AFTER position:{} h:{}", position, child.getMeasuredHeight());
        } else {
            cleanupLayoutState(child);
        }

        final int w = child.getMeasuredWidth();
        final int h = child.getMeasuredHeight();

        getOrCreateRecord(position).height = h;

        int childLeft;
        final int childTop = flowDown ? y : y - h;

        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                childLeft = childrenLeft;
                break;
            case Gravity.CENTER_HORIZONTAL:
                childLeft = childrenLeft + ((mColumnWidth - w) / 2);
                break;
            case Gravity.RIGHT:
                childLeft = childrenLeft + mColumnWidth - w;
                break;
            default:
                childLeft = childrenLeft;
                break;
        }

        if (needToMeasure) {
            final int childRight = childLeft + w;

            if (p.viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                // offset the top and bottom of all our columns
                // if it's the footer we want it below the lowest child bottom
                int gridChildTop;
                int gridChildBottom;

                if (flowDown) {
                    gridChildTop = getLowestPositionedBottom();
                    gridChildBottom = gridChildTop + h;
                } else {
                    gridChildBottom = getHighestPositionedTop();
                    gridChildTop = gridChildBottom - h;
                }

                for (int i = 0; i < mNumColumns; i++) {
                    updateColumnTopIfNeeded(i, gridChildTop);
                    updateColumnBottomIfNeeded(i, gridChildBottom);
                }

                child.layout(childLeft, gridChildTop, childRight, gridChildBottom);
            } else {
                // stash the bottom and the top if it's higher positioned
                int column = getPositionColumn(position);

                int gridChildTop;
                int gridChildBottom;

                int childTopMargin = getChildTopMargin(position);
                int childBottomMargin = getChildBottomMargin();
                int verticalMargins = childTopMargin + childBottomMargin;

                if (flowDown) {
                    gridChildTop = mColumnBottoms[column]; // the next items top is the last items bottom
                    gridChildBottom = gridChildTop + (h + verticalMargins);
                } else {
                    gridChildBottom = mColumnTops[column]; // the bottom of the next column up is our top
                    gridChildTop = gridChildBottom - (h + verticalMargins);
                }

                if (DBG)
                    Log.LOGGER.debug(MARKER, "onLayoutChild position:{} column:{} gridChildTop:{} gridChildBottom:{}",
                            position, column, gridChildTop, gridChildBottom);

                // we also know the column of this view so let's stash it in the
                // view's layout params
                p.column = column;

                updateColumnBottomIfNeeded(column, gridChildBottom);
                updateColumnTopIfNeeded(column, gridChildTop);

                // subtract the margins before layout
                gridChildTop += childTopMargin;
                gridChildBottom -= childBottomMargin;

                child.layout(childLeft, gridChildTop, childRight, gridChildBottom);
            }
        } else {
            if (p.viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                // offset the top and bottom of all our columns
                // if it's the footer we want it below the lowest child bottom
                int gridChildTop;
                int gridChildBottom;

                if (flowDown) {
                    gridChildTop = getLowestPositionedBottom();
                    gridChildBottom = gridChildTop + h;
                } else {
                    gridChildBottom = getHighestPositionedTop();
                    gridChildTop = gridChildBottom - h;
                }

                for (int i = 0; i < mNumColumns; i++) {
                    updateColumnTopIfNeeded(i, gridChildTop);
                    updateColumnBottomIfNeeded(i, gridChildBottom);
                }

                child.offsetLeftAndRight(childLeft - child.getLeft());
                child.offsetTopAndBottom(gridChildTop - child.getTop());
            } else {
                // stash the bottom and the top if it's higher positioned
                int column = getPositionColumn(position);

                int gridChildTop;
                int gridChildBottom;

                int childTopMargin = getChildTopMargin(position);
                int childBottomMargin = getChildBottomMargin();
                int verticalMargins = childTopMargin + childBottomMargin;

                if (flowDown) {
                    gridChildTop = mColumnBottoms[column]; // the next items top is the last items bottom
                    gridChildBottom = gridChildTop + (h + verticalMargins);
                } else {
                    gridChildBottom = mColumnTops[column]; // the bottom of the next column up is our top
                    gridChildTop = gridChildBottom - (h + verticalMargins);
                }

                if (DBG) Log.LOGGER.debug(MARKER, "onOffsetChild position:" + position +
                        " column:" + column +
                        " childTop:" + childTop +
                        " gridChildTop:" + gridChildTop +
                        " gridChildBottom:" + gridChildBottom);

                // we also know the column of this view so let's stash it in the
                // view's layout params
                p.column = column;

                updateColumnBottomIfNeeded(column, gridChildBottom);
                updateColumnTopIfNeeded(column, gridChildTop);

                child.offsetLeftAndRight(childLeft - child.getLeft());
                child.offsetTopAndBottom(gridChildTop + childTopMargin - child.getTop());
            }
        }
    }

    private int getChildTopMargin(final int position) {
        int limit;
        if (getHeaderViewsCount() > 0) {
            limit = 1;
        } else {
            limit = mNumColumns;
        }
        // spacing applied after the first row
        return position < limit ? 0 : mVerticalSpacing;
    }

    private int getChildBottomMargin() {
        return 0;
    }

    private void updateColumnTopIfNeeded(int column, int childTop) {
        if (childTop < mColumnTops[column]) {
            mColumnTops[column] = childTop;
        }
    }

    private void updateColumnBottomIfNeeded(int column, int childBottom) {
        if (childBottom > mColumnBottoms[column]) {
            mColumnBottoms[column] = childBottom;
        }
    }

    private void alignTops() {
        if (mFirstPosition == getHeaderViewsCount()) {
            // we're showing all the views before the header views
            int[] nonHeaderTops = getHighestNonHeaderTops();
            // we should now have our non header tops
            // align them
            boolean isAligned = true;
            int highestColumn = -1;
            int highestTop = Integer.MAX_VALUE;
            for (int i = 0; i < nonHeaderTops.length; i++) {
                // are they all aligned
                if (isAligned && i > 0 && nonHeaderTops[i] != highestTop) {
                    isAligned = false; // not all the tops are aligned
                }
                // what's the highest
                if (nonHeaderTops[i] < highestTop) {
                    highestTop = nonHeaderTops[i];
                    highestColumn = i;
                }
            }

            // skip the rest.
            if (isAligned) return;

            // we've got the highest column - lets align the others
            for (int i = 0; i < nonHeaderTops.length; i++) {
                if (i != highestColumn) {
                    // there's a gap in this column
                    int offset = highestTop - nonHeaderTops[i];
                    offsetChildrenTopAndBottom(offset, i);
                }
            }
            invalidate();
        }
    }

    private int[] getHighestNonHeaderTops() {
        int[] nonHeaderTops = new int[mNumColumns];
        int childCount = getChildCount();
        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child != null) {
                    // is this child's top the highest non
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    // is it a child that isn't a header
                    if (lp.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER &&
                            child.getTop() < nonHeaderTops[lp.column]) {
                        nonHeaderTops[lp.column] = child.getTop();
                    }
                }
            }
        }
        return nonHeaderTops;
    }

    @Override
    protected void detachViewsFromParent(int start, int count) {
        super.detachViewsFromParent(start, count);
        // go through our remaining views and sync the top and bottom stash.

        // Repair the top and bottom column boundaries from the views we still have
        Arrays.fill(mColumnTops, Integer.MAX_VALUE);
        Arrays.fill(mColumnBottoms, 0);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                final LayoutParams childParams = (LayoutParams) child.getLayoutParams();
                if (childParams.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    int column = childParams.column;
                    int position = childParams.scrappedFromPosition;
                    final int childTop = child.getTop();
                    if (childTop < mColumnTops[column]) {
                        mColumnTops[column] = childTop - getChildTopMargin(position);
                    }
                    final int childBottom = child.getBottom();
                    if (childBottom > mColumnBottoms[column]) {
                        mColumnBottoms[column] = childBottom + getChildBottomMargin();
                    }
                } else {
                    // the header and footer here
                    final int childTop = child.getTop();
                    final int childBottom = child.getBottom();

                    for (int col = 0; col < mNumColumns; col++) {
                        if (childTop < mColumnTops[col]) {
                            mColumnTops[col] = childTop;
                        }
                        if (childBottom > mColumnBottoms[col]) {
                            mColumnBottoms[col] = childBottom;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setSelection(int position) {
        setSelectionFromTop(position, 0);
    }

    @Override
    void setSelectionInt(int position) {
        setNextSelectedPositionInt(position);

        if (mPositionScroller != null) {
            mPositionScroller.stop();
        }

        layoutChildren();

        // it's unlikely that scrollbars don't need to be awakened
        awakenScrollBars();
    }

    @NonNull
    private LayoutRecord getOrCreateRecord(int position) {
        LayoutRecord rec = mPositionData.get(position);
        if (rec == null) {
            rec = new LayoutRecord();
            mPositionData.put(position, rec);
        }
        return rec;
    }

    private int getPositionColumn(final int position) {
        LayoutRecord rec = mPositionData.get(position);
        return rec != null ? rec.column : -1;
    }

    private void initColumnOffsets() {
        int paddingTop = getListPaddingTop();
        Arrays.fill(mColumnTops, paddingTop);
        Arrays.fill(mColumnBottoms, paddingTop);
    }

    //// DOWN position

    private int getHighestPositionedBottom() {
        final int column = getHighestPositionedBottomColumn();
        return mColumnBottoms[column];
    }

    private int getHighestPositionedBottomColumn() {
        int highestColumn = -1;
        int highestPositionedBottom = Integer.MAX_VALUE;
        // go forwards and reverse on RTL
        int columnCount = mNumColumns;
        boolean isLayoutRtl = isLayoutRtl();
        // the highest positioned bottom is the one with the lowest value
        for (int i = 0; i < columnCount; i++) {
            int column = isLayoutRtl ? columnCount - 1 - i : i;
            int bottom = mColumnBottoms[column];
            if (bottom < highestPositionedBottom) {
                highestPositionedBottom = bottom;
                highestColumn = column;
            }
        }
        return highestColumn;
    }

    private int getLowestPositionedBottom() {
        int lowestPositionedBottom = Integer.MIN_VALUE;
        int columnCount = mNumColumns;
        // the lowest positioned bottom is the one with the highest value
        for (int i = 0; i < columnCount; i++) {
            int bottom = mColumnBottoms[i];
            if (bottom > lowestPositionedBottom) {
                lowestPositionedBottom = bottom;
            }
        }
        return lowestPositionedBottom;
    }

    //// UP position

    private int getLowestPositionedTop() {
        final int column = getLowestPositionedTopColumn();
        return mColumnTops[column];
    }

    private int getLowestPositionedTopColumn() {
        int lowestColumn = -1;
        int lowestPositionedTop = Integer.MIN_VALUE;
        // go backwards and reverse on RTL
        int columnCount = mNumColumns;
        boolean isLayoutRtl = isLayoutRtl();
        // the lowest positioned top is the one with the highest value
        for (int i = columnCount - 1; i >= 0; i--) {
            int column = isLayoutRtl ? columnCount - 1 - i : i;
            int top = mColumnTops[column];
            if (top > lowestPositionedTop) {
                lowestPositionedTop = top;
                lowestColumn = column;
            }
        }
        return lowestColumn;
    }

    private int getHighestPositionedTop() {
        int highestPositionedTop = Integer.MAX_VALUE;
        int columnCount = mNumColumns;
        // the highest positioned top is the one with the lowest value
        for (int i = 0; i < columnCount; i++) {
            int top = mColumnTops[i];
            if (top < highestPositionedTop) {
                highestPositionedTop = top;
            }
        }
        return highestPositionedTop;
    }

    @Override
    public void offsetChildrenTopAndBottom(int offset) {
        super.offsetChildrenTopAndBottom(offset);
        if (offset != 0) {
            for (int i = 0; i < mNumColumns; i++) {
                mColumnTops[i] += offset;
                mColumnBottoms[i] += offset;
            }
        }
        if (DBG) Log.LOGGER.debug(MARKER, "offsetChildrenTopAndBottom: {} tops:{} bottoms:{}", offset,
                Arrays.toString(mColumnTops), Arrays.toString(mColumnBottoms));
    }

    void offsetChildrenTopAndBottom(final int offset, final int column) {
        if (DBG) Log.LOGGER.debug(MARKER, "offsetChildrenTopAndBottom: {} column:{}", offset, column);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View v = getChildAt(i);
            if (v != null) {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (lp.column == column) {
                    v.offsetTopAndBottom(offset);
                }
            }
        }
        if (offset != 0) {
            mColumnTops[column] += offset;
            mColumnBottoms[column] += offset;
        }
    }

    private int getChildTop(final int position) {
        if (mAdapter.getItemViewType(position) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            int count = getChildCount();
            int paddingTop = 0;
            if (hasBooleanFlag(CLIP_TO_PADDING_MASK)) {
                paddingTop = getListPaddingTop();
            }
            return count > 0 ? getChildAt(count - 1).getBottom() : paddingTop;
        } else {
            final int column = getPositionColumn(position);
            if (column == -1) {
                return getHighestPositionedBottom();
            }
            return mColumnBottoms[column];
        }
    }

    private int getChildBottom(final int position) {
        if (mAdapter.getItemViewType(position) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            int count = getChildCount();
            int paddingBottom = 0;
            if (hasBooleanFlag(CLIP_TO_PADDING_MASK)) {
                paddingBottom = getListPaddingBottom();
            }
            return count > 0 ? getChildAt(0).getTop() : getHeight() - paddingBottom;
        } else {
            final int column = getPositionColumn(position);
            if (column == -1) {
                return getLowestPositionedTop();
            }
            return mColumnTops[column];
        }
    }

    private int getNextChildDownsTop(final int position) {
        if (mAdapter.getItemViewType(position) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            final int count = getChildCount();
            return count > 0 ? getChildAt(count - 1).getBottom() : 0;
        } else {
            return getHighestPositionedBottom();
        }
    }

    private int getNextChildUpsBottom(final int position) {
        if (mAdapter.getItemViewType(position) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            final int count = getChildCount();
            return count > 0 ? getChildAt(0).getTop() : 0;
        } else {
            return getLowestPositionedTop();
        }
    }

    @Override
    int getFirstChildTop() {
        if (mAdapter.getItemViewType(mFirstPosition) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            return super.getFirstChildTop();
        }
        return getLowestPositionedTop();
    }

    @Override
    int getHighestChildTop() {
        if (mAdapter.getItemViewType(mFirstPosition) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            return super.getHighestChildTop();
        }
        return getHighestPositionedTop();
    }

    @Override
    int getLastChildBottom() {
        final int lastPosition = mFirstPosition + (getChildCount() - 1);
        if (mAdapter.getItemViewType(lastPosition) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            return super.getLastChildBottom();
        }
        return getHighestPositionedBottom();
    }

    @Override
    int getLowestChildBottom() {
        final int lastPosition = mFirstPosition + (getChildCount() - 1);
        if (mAdapter.getItemViewType(lastPosition) == ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
            return super.getLowestChildBottom();
        }
        return getLowestPositionedBottom();
    }

    @Override
    public void setStackFromBottom(boolean stackFromBottom) {
        if (stackFromBottom) {
            Log.LOGGER.warn(MARKER, "StaggeredGridView does not support setStackFromBottom(true)");
            return;
        }
        super.setStackFromBottom(false);
    }

    /**
     * Set the gravity for this grid. Gravity describes how the child views
     * are horizontally aligned. Defaults to {@link Gravity#START}.
     *
     * @param gravity the gravity to apply to this grid's children
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Describes how the child views are horizontally aligned. Defaults to Gravity.LEFT
     *
     * @return the gravity that will be applied to this grid's children
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Set the amount of horizontal (x) spacing to place between each item
     * in the grid.
     *
     * @param horizontalSpacing The amount of horizontal space between items,
     *                          in pixels.
     */
    public void setHorizontalSpacing(int horizontalSpacing) {
        if (horizontalSpacing != mRequestedHorizontalSpacing) {
            mRequestedHorizontalSpacing = horizontalSpacing;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Returns the amount of horizontal spacing currently used between each item in the grid.
     *
     * <p>This is only accurate for the current layout. If {@link #setHorizontalSpacing(int)}
     * has been called but layout is not yet complete, this method may return a stale value.
     * To get the horizontal spacing that was explicitly requested use
     * {@link #getRequestedHorizontalSpacing()}.</p>
     *
     * @return Current horizontal spacing between each item in pixels
     * @see #setHorizontalSpacing(int)
     * @see #getRequestedHorizontalSpacing()
     */
    public int getHorizontalSpacing() {
        return mHorizontalSpacing;
    }

    /**
     * Returns the requested amount of horizontal spacing between each item in the grid.
     *
     * <p>The value returned may have been supplied during inflation as part of a style,
     * the default GridView style, or by a call to {@link #setHorizontalSpacing(int)}.
     * If layout is not yet complete or if GridView calculated a different horizontal spacing
     * from what was requested, this may return a different value from
     * {@link #getHorizontalSpacing()}.</p>
     *
     * @return The currently requested horizontal spacing between items, in pixels
     * @see #setHorizontalSpacing(int)
     * @see #getHorizontalSpacing()
     */
    public int getRequestedHorizontalSpacing() {
        return mRequestedHorizontalSpacing;
    }

    /**
     * Set the amount of vertical (y) spacing to place between each item
     * in the grid.
     *
     * @param verticalSpacing The amount of vertical space between items,
     *                        in pixels.
     * @see #getVerticalSpacing()
     */
    public void setVerticalSpacing(int verticalSpacing) {
        if (verticalSpacing != mVerticalSpacing) {
            mVerticalSpacing = verticalSpacing;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Returns the amount of vertical spacing between each item in the grid.
     *
     * @return The vertical spacing between items in pixels
     * @see #setVerticalSpacing(int)
     */
    public int getVerticalSpacing() {
        return mVerticalSpacing;
    }

    /**
     * Control how items are stretched to fill their space.
     *
     * @param stretchMode Either {@link #NO_STRETCH},
     *                    {@link #STRETCH_SPACING}, or {@link #STRETCH_COLUMN_WIDTH}.
     */
    public void setStretchMode(@MagicConstant(intValues = {
            NO_STRETCH,
            STRETCH_SPACING,
            STRETCH_COLUMN_WIDTH
    }) int stretchMode) {
        if (stretchMode != mStretchMode) {
            mStretchMode = stretchMode;
            requestLayoutIfNecessary();
        }
    }

    @MagicConstant(intValues = {
            NO_STRETCH,
            STRETCH_SPACING,
            STRETCH_COLUMN_WIDTH
    })
    public int getStretchMode() {
        return mStretchMode;
    }

    /**
     * Set the width of columns in the grid.
     *
     * @param columnWidth The column width, in pixels.
     */
    public void setColumnWidth(int columnWidth) {
        if (columnWidth != mRequestedColumnWidth) {
            mRequestedColumnWidth = columnWidth;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Return the width of a column in the grid.
     *
     * <p>This may not be valid yet if a layout is pending.</p>
     *
     * @return The column width in pixels
     * @see #setColumnWidth(int)
     * @see #getRequestedColumnWidth()
     */
    public int getColumnWidth() {
        return mColumnWidth;
    }

    /**
     * Return the requested width of a column in the grid.
     *
     * <p>This may not be the actual column width used. Use {@link #getColumnWidth()}
     * to retrieve the current real width of a column.</p>
     *
     * @return The requested column width in pixels
     * @see #setColumnWidth(int)
     * @see #getColumnWidth()
     */
    public int getRequestedColumnWidth() {
        return mRequestedColumnWidth;
    }

    /**
     * Set the number of columns in the grid
     *
     * @param numColumns The desired number of columns.
     */
    public void setNumColumns(int numColumns) {
        if (numColumns != mRequestedNumColumns) {
            mRequestedNumColumns = numColumns;
            requestLayoutIfNecessary();
        }
    }

    /**
     * Get the number of columns in the grid.
     * Returns {@link #AUTO_FIT} if the Grid has never been laid out.
     *
     * @see #setNumColumns(int)
     */
    public int getNumColumns() {
        return mNumColumns;
    }

    /**
     * Make sure views are touching the top or bottom edge, as appropriate for
     * our gravity
     */
    private void adjustViewsUpOrDown() {
        final int childCount = getChildCount();

        if (childCount > 0) {
            int delta;
            // Uh-oh -- we came up short. Slide all views up to make them
            // align with the top
            delta = getHighestChildTop() - getListPaddingTop();
            if (delta < 0) {
                // We only are looking to see if we are too low, not too high
                delta = 0;
            }

            if (delta != 0) {
                offsetChildrenTopAndBottom(-delta);
            }
        }
    }

    @Override
    protected int computeVerticalScrollExtent() {
        final int count = getChildCount();
        if (count > 0) {
            int area = getLowestChildBottom() - getHighestChildTop();
            return Math.min(getHeight(), area);
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        int count = getChildCount();
        if (mFirstPosition >= 0 && count > 0) {
            int top = getHighestChildTop();
            int area = getLowestChildBottom() - top;
            return Math.round(mFirstPosition * (float) area / count - top);
        }
        return 0;
    }

    @Override
    protected int computeVerticalScrollRange() {
        int count = getChildCount();
        if (count > 0) {
            int area = getLowestChildBottom() - getHighestChildTop();
            return (int) ((float) area / count * mItemCount);
        }
        return 0;
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new StaggeredGridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    @NonNull
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return new StaggeredGridView.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof StaggeredGridView.LayoutParams;
    }

    /**
     * Extends AbsListView.LayoutParams to provide column position.
     */
    public static class LayoutParams extends AbsListView.LayoutParams {

        /**
         * The final column index of the view.
         */
        int column = -1;

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h, viewType);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
