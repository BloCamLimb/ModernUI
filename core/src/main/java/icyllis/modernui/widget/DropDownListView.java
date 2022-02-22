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

import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;

/**
 * Wrapper class for a ListView. This wrapper can hijack the focus to
 * make sure the list uses the appropriate drawables and states when
 * displayed on screen within a drop down. The focus is never actually
 * passed to the drop down in this mode; the list only looks focused.
 *
 * @hide
 */
public class DropDownListView extends ListView {
    /*
     * WARNING: This is a workaround for a touch mode issue.
     *
     * Touch mode is propagated lazily to windows. This causes problems in
     * the following scenario:
     * - Type something in the AutoCompleteTextView and get some results
     * - Move down with the d-pad to select an item in the list
     * - Move up with the d-pad until the selection disappears
     * - Type more text in the AutoCompleteTextView *using the soft keyboard*
     *   and get new results; you are now in touch mode
     * - The selection comes back on the first item in the list, even though
     *   the list is supposed to be in touch mode
     *
     * Using the soft keyboard triggers the touch mode change but that change
     * is propagated to our window only after the first list layout, therefore
     * after the list attempts to resurrect the selection.
     *
     * The trick to work around this issue is to pretend the list is in touch
     * mode when we know that the selection should not appear, that is when
     * we know the user moved the selection away from the list.
     *
     * This boolean is set to true whenever we explicitly hide the list's
     * selection and reset to false whenever we know the user moved the
     * selection back to the list.
     *
     * When this boolean is true, isInTouchMode() returns true, otherwise it
     * returns super.isInTouchMode().
     */
    private boolean mListSelectionHidden;

    /**
     * True if this wrapper should fake focus.
     */
    private boolean mHijackFocus;

    /**
     * Whether to force drawing of the pressed state selector.
     */
    private boolean mDrawsInPressedState;

    /**
     * Runnable posted when we are awaiting hover event resolution. When set,
     * drawable state changes are postponed.
     */
    private ResolveHoverRunnable mResolveHoverRunnable;

    /**
     * Creates a new list view wrapper.
     */
    public DropDownListView(boolean hijackFocus) {
        mHijackFocus = hijackFocus;
    }

    @Override
    boolean shouldShowSelector() {
        return isHovered() || super.shouldShowSelector();
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent ev) {
        if (mResolveHoverRunnable != null) {
            // Resolved hover event as hover => touch transition.
            mResolveHoverRunnable.cancel();
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onHoverEvent(@Nonnull MotionEvent ev) {
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_HOVER_EXIT && mResolveHoverRunnable == null) {
            // This may be transitioning to TOUCH_DOWN. Postpone drawable state
            // updates until either the next frame or the next touch event.
            mResolveHoverRunnable = new ResolveHoverRunnable();
            mResolveHoverRunnable.post();
        }

        // Allow the super class to handle hover state management first.
        final boolean handled = super.onHoverEvent(ev);

        if (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE) {
            final int position = pointToPosition((int) ev.getX(), (int) ev.getY());
            if (position != INVALID_POSITION && position != mSelectedPosition) {
                final View hoveredItem = getChildAt(position - getFirstVisiblePosition());
                if (hoveredItem.isEnabled()) {
                    // Force a focus so that the proper selector state gets
                    // used when we update.
                    requestFocus();

                    positionSelector(position, hoveredItem);
                    setSelectedPositionInt(position);
                    setNextSelectedPositionInt(position);
                }
                updateSelectorState();
            }
        } else {
            // Do not cancel the selected position if the selection is visible
            // by other means.
            if (!super.shouldShowSelector()) {
                setSelectedPositionInt(INVALID_POSITION);
                setNextSelectedPositionInt(INVALID_POSITION);
            }
        }

        return handled;
    }

    @Override
    protected void drawableStateChanged() {
        if (mResolveHoverRunnable == null) {
            super.drawableStateChanged();
        }
    }

    /**
     * Handles forwarded events.
     *
     * @return whether the event was handled
     */
    public boolean onForwardedEvent(@Nonnull MotionEvent event) {
        boolean handledEvent = true;
        boolean clearPressedItem = false;

        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
                handledEvent = false;
                break;
            case MotionEvent.ACTION_UP:
                handledEvent = false;
                // $FALL-THROUGH$
            case MotionEvent.ACTION_MOVE:
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                final int position = pointToPosition(x, y);
                if (position == INVALID_POSITION) {
                    clearPressedItem = true;
                    break;
                }

                final View child = getChildAt(position - getFirstVisiblePosition());
                setPressedItem(child, position, x, y);
                handledEvent = true;

                if (action == MotionEvent.ACTION_UP) {
                    final long id = getItemIdAtPosition(position);
                    performItemClick(child, position, id);
                }
                break;
        }

        // Failure to handle the event cancels forwarding.
        if (!handledEvent || clearPressedItem) {
            clearPressedItem();
        }

        // Manage automatic scrolling.
        /*if (handledEvent) {
            if (mScrollHelper == null) {
                mScrollHelper = new AbsListViewAutoScroller(this);
            }
            mScrollHelper.setEnabled(true);
            mScrollHelper.onTouch(this, event);
        } else if (mScrollHelper != null) {
            mScrollHelper.setEnabled(false);
        }*/

        return handledEvent;
    }

    /**
     * Sets whether the list selection is hidden, as part of a workaround for a
     * touch mode issue (see the declaration for mListSelectionHidden).
     *
     * @param hideListSelection {@code true} to hide list selection,
     *                          {@code false} to show
     */
    public void setListSelectionHidden(boolean hideListSelection) {
        mListSelectionHidden = hideListSelection;
    }

    private void clearPressedItem() {
        mDrawsInPressedState = false;
        setPressed(false);
        updateSelectorState();

        final View motionView = getChildAt(mMotionPosition - mFirstPosition);
        if (motionView != null) {
            motionView.setPressed(false);
        }
    }

    private void setPressedItem(@Nonnull View child, int position, float x, float y) {
        mDrawsInPressedState = true;

        // Ordering is essential. First, update the container's pressed state.
        drawableHotspotChanged(x, y);
        if (!isPressed()) {
            setPressed(true);
        }

        // Next, run layout if we need to stabilize child positions.
        if (mDataChanged) {
            layoutChildren();
        }

        // Manage the pressed view based on motion position. This allows us to
        // play nicely with actual touch and scroll events.
        final View motionView = getChildAt(mMotionPosition - mFirstPosition);
        if (motionView != null && motionView != child && motionView.isPressed()) {
            motionView.setPressed(false);
        }
        mMotionPosition = position;

        // Offset for child coordinates.
        final float childX = x - child.getLeft();
        final float childY = y - child.getTop();
        child.drawableHotspotChanged(childX, childY);
        if (!child.isPressed()) {
            child.setPressed(true);
        }

        // Ensure that keyboard focus starts from the last touched position.
        setSelectedPositionInt(position);
        positionSelectorLikeTouch(position, child, x, y);

        // Refresh the drawable state to reflect the new pressed state,
        // which will also update the selector state.
        refreshDrawableState();
    }

    @Override
    boolean touchModeDrawsInPressedState() {
        return mDrawsInPressedState || super.touchModeDrawsInPressedState();
    }

    /**
     * Avoids jarring scrolling effect by ensuring that list elements
     * made of a text view fit on a single line.
     *
     * @param position the item index in the list to get a view for
     * @return the view for the specified item
     */
    @Override
    View obtainView(int position, boolean[] isScrap) {
        View view = super.obtainView(position, isScrap);

        if (view instanceof TextView) {
            ((TextView) view).setHorizontallyScrolling(true);
        }

        return view;
    }

    @Override
    public boolean isInTouchMode() {
        // WARNING: Please read the comment where mListSelectionHidden is declared
        return (mHijackFocus && mListSelectionHidden) || super.isInTouchMode();
    }

    /**
     * Returns the focus state in the drop down.
     *
     * @return true always if hijacking focus
     */
    @Override
    public boolean hasWindowFocus() {
        return mHijackFocus || super.hasWindowFocus();
    }

    /**
     * Returns the focus state in the drop down.
     *
     * @return true always if hijacking focus
     */
    @Override
    public boolean isFocused() {
        return mHijackFocus || super.isFocused();
    }

    /**
     * Returns the focus state in the drop down.
     *
     * @return true always if hijacking focus
     */
    @Override
    public boolean hasFocus() {
        return mHijackFocus || super.hasFocus();
    }

    /**
     * Runnable that forces hover event resolution and updates drawable state.
     */
    private class ResolveHoverRunnable implements Runnable {

        @Override
        public void run() {
            // Resolved hover event as standard hover exit.
            mResolveHoverRunnable = null;
            drawableStateChanged();
        }

        public void cancel() {
            mResolveHoverRunnable = null;
            removeCallbacks(this);
        }

        public void post() {
            DropDownListView.this.post(this);
        }
    }
}
