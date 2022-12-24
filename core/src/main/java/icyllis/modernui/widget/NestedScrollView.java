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

import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.FMath;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.view.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * NestedScrollView is just like {@link ScrollView}, but it supports acting
 * as both a nested scrolling parent and child on both new and old versions of Android.
 * Nested scrolling is enabled by default.
 */
//FIXME buggy
public class NestedScrollView extends FrameLayout {

    static final int ANIMATED_SCROLL_GAP = 250;

    static final float MAX_SCROLL_FACTOR = 0.5f;

    private static final int DEFAULT_SMOOTH_SCROLL_DURATION = 250;

    /**
     * Interface definition for a callback to be invoked when the scroll
     * X or Y positions of a view change.
     *
     * <p>This version of the interface works on all versions of Android, back to API v4.</p>
     *
     * @see #setOnScrollChangeListener(OnScrollChangeListener)
     */
    @FunctionalInterface
    public interface OnScrollChangeListener {

        /**
         * Called when the scroll position of a view changes.
         *
         * @param v          The view whose scroll position has changed.
         * @param scrollX    Current horizontal scroll origin.
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                            int oldScrollX, int oldScrollY);
    }

    private long mLastScroll;

    private final Rect mTempRect = new Rect();
    private final OverScroller mScroller;

    private final EdgeEffect mEdgeGlowTop;

    private final EdgeEffect mEdgeGlowBottom;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;

    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;
    private boolean mIsLaidOut = false;

    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    private View mChildToScrollTo = null;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts their finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * When set to true, the scroll view measure its child to make it fill the currently
     * visible area.
     */
    private boolean mFillViewport;

    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;

    private final int mTouchSlop;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;

    private int mLastScrollerY;

    private OnScrollChangeListener mOnScrollChangeListener;

    public NestedScrollView() {
        mEdgeGlowTop = new EdgeEffect();
        mEdgeGlowBottom = new EdgeEffect();

        mScroller = new OverScroller();
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get();
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        // ...because why else would you be using this widget?
        setNestedScrollingEnabled(true);

        setVerticalScrollBarEnabled(true);
        setVerticalScrollbarThumbDrawable(new Drawable() {
            private int mAlpha = 255;

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.get();
                paint.setRGBA(84, 190, 196, (int) (mAlpha * 0.5));
                Rect bounds = getBounds();
                canvas.drawRoundRect(bounds.left + 1, bounds.top + 1, bounds.right - 1, bounds.bottom - 1,
                        bounds.width() / 2f - 1, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                this.mAlpha = alpha;
            }
        });
        setVerticalScrollbarTrackDrawable(new Drawable() {
            private int mAlpha = 255;

            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.get();
                paint.setRGBA(128, 128, 128, (int) (mAlpha * 0.75));
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(3);
                Rect bounds = getBounds();
                canvas.drawRoundRect(bounds.left + 1, bounds.top + 1, bounds.right - 1, bounds.bottom - 1,
                        bounds.width() / 2f - 1, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                this.mAlpha = alpha;
            }
        });
    }

    // NestedScrollingParent3

    @Override
    public void onNestedScroll(@Nonnull View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type, @Nonnull int[] consumed) {
        final int oldScrollY = getScrollY();
        scrollBy(0, dyUnconsumed);
        final int myConsumed = getScrollY() - oldScrollY;

        if (consumed != null) {
            consumed[1] += myConsumed;
        }
        final int myUnconsumed = dyUnconsumed - myConsumed;

        dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null, type, consumed);
    }

    @Override
    public boolean onStartNestedScroll(@Nonnull View child, @Nonnull View target, int axes,
                                       int type) {
        return (axes & SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@Nonnull View child, @Nonnull View target, int axes,
                                       int type) {
        super.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onStopNestedScroll(@Nonnull View target, int type) {
        super.onStopNestedScroll(target, type);
        stopNestedScroll(type);
    }

    @Override
    public void onNestedPreScroll(@Nonnull View target, int dx, int dy, @Nonnull int[] consumed,
                                  int type) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type);
    }

    @Override
    public boolean onNestedFling(
            @Nonnull View target, float velocityX, float velocityY, boolean consumed) {
        if (!consumed) {
            dispatchNestedFling(0, velocityY, true);
            fling((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(@Nonnull View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    // ScrollView import

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    /**
     * @return The maximum amount this scroll view will scroll in response to
     * an arrow event.
     */
    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * getHeight());
    }

    @Override
    public void addView(@Nonnull View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child);
    }

    @Override
    public void addView(@Nonnull View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(@Nonnull View child, @Nonnull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, params);
    }

    @Override
    public void addView(@Nonnull View child, int index, @Nonnull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index, params);
    }

    /**
     * Register a callback to be invoked when the scroll X or Y positions of
     * this view change.
     * <p>This version of the method works on all versions of Android, back to API v4.</p>
     *
     * @param l The listener to notify when the scroll X or Y position changes.
     * @see View#getScrollX()
     * @see View#getScrollY()
     */
    public void setOnScrollChangeListener(@Nullable OnScrollChangeListener l) {
        mOnScrollChangeListener = l;
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private boolean canScroll() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            return childSize > parentSpace;
        }
        return false;
    }

    /**
     * Indicates whether this ScrollView's content is stretched to fill the viewport.
     *
     * @return True if the content fills the viewport, false otherwise.
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }

    /**
     * Set whether this ScrollView should stretch its content height to fill the viewport or not.
     *
     * @param fillViewport True to stretch the content's height to the viewport's
     *                     boundaries, false otherwise.
     */
    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }

    /**
     * Set whether arrow scrolling will animate its transition.
     *
     * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
     */
    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport) {
            return;
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childSize = child.getMeasuredHeight();
            int parentSpace = getMeasuredHeight()
                    - getPaddingTop()
                    - getPaddingBottom()
                    - lp.topMargin
                    - lp.bottomMargin;

            if (childSize < parentSpace) {
                int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(parentSpace, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(@Nonnull KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(@Nonnull KeyEvent event) {
        mTempRect.setEmpty();

        if (!canScroll()) {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEY_ESCAPE) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_DOWN);
                return nextFocused != null
                        && nextFocused != this
                        && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEY_UP:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_UP);
                    } else {
                        handled = fullScroll(View.FOCUS_UP);
                    }
                    break;
                case KeyEvent.KEY_DOWN:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_DOWN);
                    } else {
                        handled = fullScroll(View.FOCUS_DOWN);
                    }
                    break;
                case KeyEvent.KEY_SPACE:
                    pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                    break;
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY
                    || y >= child.getBottom() - scrollY
                    || x < child.getLeft()
                    || x >= child.getRight());
        }
        return false;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(@Nonnull MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and they are moving their finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE -> {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from their original down touch.
                 */

                final int y = (int) ev.getY();
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop
                        && (getNestedScrollAxes() & SCROLL_AXIS_VERTICAL) == 0) {
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    mNestedYOffset = 0;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            case MotionEvent.ACTION_DOWN -> {
                final int y = (int) ev.getY();
                if (!inChild((int) ev.getX(), y)) {
                    mIsBeingDragged = stopGlowAnimations(ev) || !mScroller.isFinished();
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionY = y;

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We also want to catch the edge glow and start dragging
                 * if one is being animated. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                 */
                mScroller.computeScrollOffset();
                mIsBeingDragged = stopGlowAnimations(ev) || !mScroller.isFinished();
                startNestedScroll(SCROLL_AXIS_VERTICAL, TYPE_TOUCH);
            }
            case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                /* Release the drag */
                mIsBeingDragged = false;
                recycleVelocityTracker();
                if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange())) {
                    postInvalidateOnAnimation();
                }
                stopNestedScroll(TYPE_TOUCH);
            }
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }

        MotionEvent vtev = ev.copy();
        vtev.offsetLocation(0, mNestedYOffset);

        switch (action) {
            case MotionEvent.ACTION_DOWN -> {
                if (getChildCount() == 0) {
                    return false;
                }
                if (mIsBeingDragged) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    abortAnimatedScroll();
                }

                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                startNestedScroll(SCROLL_AXIS_VERTICAL, TYPE_TOUCH);
            }
            case MotionEvent.ACTION_MOVE -> {
                final int y = (int) ev.getY();
                int deltaY = mLastMotionY - y;
                deltaY -= releaseVerticalGlow(deltaY, ev.getX());
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Start with nested pre scrolling
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset,
                            TYPE_TOUCH)) {
                        deltaY -= mScrollConsumed[1];
                        mNestedYOffset += mScrollOffset[1];
                    }

                    // Scroll to follow the motion event
                    mLastMotionY = y - mScrollOffset[1];

                    final int oldY = getScrollY();
                    final int range = getScrollRange();
                    final int overscrollMode = getOverScrollMode();
                    boolean canOverscroll = overscrollMode == View.OVER_SCROLL_ALWAYS
                            || (overscrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    // Calling overScrollByCompat will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    boolean clearVelocityTracker =
                            overScrollByCompat(0, deltaY, 0, getScrollY(), 0, range, 0,
                                    ViewConfiguration.get().getScaledOverscrollDistance(), true) && !hasNestedScrollingParent(TYPE_TOUCH);

                    final int scrolledDeltaY = getScrollY() - oldY;
                    final int unconsumedY = deltaY - scrolledDeltaY;

                    mScrollConsumed[1] = 0;

                    dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset,
                            TYPE_TOUCH, mScrollConsumed);

                    mLastMotionY -= mScrollOffset[1];
                    mNestedYOffset += mScrollOffset[1];

                    if (canOverscroll) {
                        deltaY -= mScrollConsumed[1];
                        final int pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            mEdgeGlowTop.onPullDistance((float) -deltaY / getHeight(),
                                    ev.getX() / getWidth());
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > range) {
                            mEdgeGlowBottom.onPullDistance((float) deltaY / getHeight(),
                                    1.f - ev.getX() / getWidth());
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                        if (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()) {
                            postInvalidateOnAnimation();
                            clearVelocityTracker = false;
                        }
                    }
                    if (clearVelocityTracker) {
                        // Break our velocity if we hit a scroll barrier.
                        mVelocityTracker.clear();
                    }
                }
            }
            case MotionEvent.ACTION_UP -> {
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                if ((Math.abs(initialVelocity) >= mMinimumVelocity)) {
                    if (!edgeEffectFling(initialVelocity)
                            && !dispatchNestedPreFling(0, -initialVelocity)) {
                        dispatchNestedFling(0, -initialVelocity, true);
                        fling(-initialVelocity);
                    }
                } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                        getScrollRange())) {
                    postInvalidateOnAnimation();
                }
                endDrag();
            }
            case MotionEvent.ACTION_CANCEL -> {
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                            getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                }
                endDrag();
            }
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();

        return true;
    }

    private boolean edgeEffectFling(int velocityY) {
        boolean consumed = true;
        if (mEdgeGlowTop.getDistance() != 0) {
            mEdgeGlowTop.onAbsorb(velocityY);
        } else if (mEdgeGlowBottom.getDistance() != 0) {
            mEdgeGlowBottom.onAbsorb(-velocityY);
        } else {
            consumed = false;
        }
        return consumed;
    }

    /**
     * This stops any edge glow animation that is currently running by applying a
     * 0 length pull at the displacement given by the provided MotionEvent. On pre-S devices,
     * this method does nothing, allowing any animating edge effect to continue animating and
     * returning <code>false</code> always.
     *
     * @param e The motion event to use to indicate the finger position for the displacement of
     *          the current pull.
     * @return <code>true</code> if any edge effect had an existing effect to be drawn ond the
     * animation was stopped or <code>false</code> if no edge effect had a value to display.
     */
    private boolean stopGlowAnimations(MotionEvent e) {
        boolean stopped = false;
        if (mEdgeGlowTop.getDistance() != 0) {
            mEdgeGlowTop.onPullDistance(0, e.getY() / getHeight());
            stopped = true;
        }
        if (mEdgeGlowBottom.getDistance() != 0) {
            mEdgeGlowBottom.onPullDistance(0, 1 - e.getY() / getHeight());
            stopped = true;
        }
        return stopped;
    }

    @Override
    public boolean onGenericMotionEvent(@Nonnull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL && !mIsBeingDragged) {
            final float axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            final int delta = Math.round(axisValue * ViewConfiguration.get().getVerticalScrollFactor());
            if (Math.abs(axisValue) > 0.9 && Math.abs(delta) * 6 > mMinimumVelocity) {
                int deltaY = FMath.clamp(delta * 6, -mMaximumVelocity, mMaximumVelocity);
                if (!edgeEffectFling(deltaY)
                        && !dispatchNestedPreFling(0, -deltaY)) {
                    dispatchNestedFling(0, -deltaY, true);
                    fling(-deltaY);
                }
                return true;
            } else {
                smoothScrollBy(-delta, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    boolean overScrollByCompat(int deltaX, int deltaY,
                               int scrollX, int scrollY,
                               int scrollRangeX, int scrollRangeY,
                               int maxOverScrollX, int maxOverScrollY,
                               boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        if (clampedY && !hasNestedScrollingParent(TYPE_NON_TOUCH)) {
            mScroller.springBack(newScrollX, newScrollY, 0, 0, 0, getScrollRange());
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            scrollRange = Math.max(0, childSize - parentSpace);
        }
        return scrollRange;
    }

    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     *                 if topFocus is true, or at the bottom of the bounds if topFocus is
     *                 false
     * @param top      the top offset of the bounds in which a focusable must be
     *                 found
     * @param bottom   the bottom offset of the bounds in which a focusable must
     *                 be found
     * @return the next focusable component in the bounds or null if none can
     * be found
     */
    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        for (View view : focusables) {
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (top < viewTop) && (viewBottom < bottom);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            (topFocus && viewTop < focusCandidate.getTop())
                                    || (!topFocus && viewBottom > focusCandidate.getBottom());

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    /**
     * <p>Handles scrolling in response to a "page up/down" shortcut press. This
     * method will scroll the view by one page up or down and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_UP}
     *                  to go one page up or
     *                  {@link View#FOCUS_DOWN} to go one page down
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean pageScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        if (down) {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                int bottom = view.getBottom() + lp.bottomMargin + getPaddingBottom();
                if (mTempRect.top + height > bottom) {
                    mTempRect.top = bottom - height;
                }
            }
        } else {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0) {
                mTempRect.top = 0;
            }
        }
        mTempRect.bottom = mTempRect.top + height;

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the top or bottom and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link #FOCUS_UP}
     *                  to go the top of the view or
     *                  {@link #FOCUS_DOWN} to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int direction) {
        boolean down = direction == FOCUS_DOWN;
        int height = getHeight();

        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                NestedScrollView.LayoutParams lp = (LayoutParams) view.getLayoutParams();
                mTempRect.bottom = view.getBottom() + lp.bottomMargin + getPaddingBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    /**
     * <p>Scrolls the view to make the area defined by <code>top</code> and
     * <code>bottom</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this ScrollView.</p>
     *
     * @param direction the scroll direction: {@link #FOCUS_UP}
     *                  to go upward, {@link #FOCUS_DOWN} to downward
     * @param top       the top offset of the new area to be made visible
     * @param bottom    the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == FOCUS_UP;

        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? (top - containerTop) : (bottom - containerBottom);
            doScrollY(delta);
        }

        if (newFocused != findFocus()) newFocused.requestFocus(direction);

        return handled;
    }

    /**
     * Handle scrolling in response to an up or down arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     *                  pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJump = getMaxScrollAmount();

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump, getHeight())) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
            nextFocused.requestFocus(direction);
        } else {
            // no new focus
            int scrollDelta = maxJump;

            if (direction == View.FOCUS_UP && getScrollY() < scrollDelta) {
                scrollDelta = getScrollY();
            } else if (direction == View.FOCUS_DOWN) {
                if (getChildCount() > 0) {
                    View child = getChildAt(0);
                    NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int daBottom = child.getBottom() + lp.bottomMargin;
                    int screenBottom = getScrollY() + getHeight() - getPaddingBottom();
                    scrollDelta = Math.min(daBottom - screenBottom, maxJump);
                }
            }
            if (scrollDelta == 0) {
                return false;
            }
            doScrollY(direction == View.FOCUS_DOWN ? scrollDelta : -scrollDelta);
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is off-screen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS, so we are
            // sure to
            // get it)
            final int descendantFocusability = getDescendantFocusability();  // save
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability);  // restore
        }
        return true;
    }

    /**
     * @return whether the descendant of this scroll view is scrolled off
     * screen.
     */
    private boolean isOffScreen(View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0, getHeight());
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     * pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return (mTempRect.bottom + delta) >= getScrollY()
                && (mTempRect.top - delta) <= (getScrollY() + height);
    }

    /**
     * Smooth scroll by a Y delta
     *
     * @param delta the number of pixels to scroll by on the Y axis
     */
    private void doScrollY(int delta) {
        if (delta != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(0, delta);
            } else {
                scrollBy(0, delta);
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        smoothScrollBy(dx, dy, DEFAULT_SMOOTH_SCROLL_DURATION, false);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx               the number of pixels to scroll by on the X axis
     * @param dy               the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    public final void smoothScrollBy(int dx, int dy, int scrollDurationMs) {
        smoothScrollBy(dx, dy, scrollDurationMs, false);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx                  the number of pixels to scroll by on the X axis
     * @param dy                  the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs    the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    private void smoothScrollBy(int dx, int dy, int scrollDurationMs, boolean withNestedScrolling) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            View child = getChildAt(0);
            NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childSize = child.getHeight() + lp.topMargin + lp.bottomMargin;
            int parentSpace = getHeight() - getPaddingTop() - getPaddingBottom();
            final int scrollY = getScrollY();
            final int maxY = Math.max(0, childSize - parentSpace);
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;
            mScroller.startScroll(getScrollX(), scrollY, 0, dy, scrollDurationMs);
            runAnimatedScroll(withNestedScrolling);
        } else {
            if (!mScroller.isFinished()) {
                abortAnimatedScroll();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, false);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x                the position where to scroll on the X axis
     * @param y                the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    public final void smoothScrollTo(int x, int y, int scrollDurationMs) {
        smoothScrollTo(x, y, scrollDurationMs, false);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x                   the position where to scroll on the X axis
     * @param y                   the position where to scroll on the Y axis
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    void smoothScrollTo(int x, int y, boolean withNestedScrolling) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, withNestedScrolling);
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x                   the position where to scroll on the X axis
     * @param y                   the position where to scroll on the Y axis
     * @param scrollDurationMs    the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    void smoothScrollTo(int x, int y, int scrollDurationMs, boolean withNestedScrolling) {
        smoothScrollBy(x - getScrollX(), y - getScrollY(), scrollDurationMs, withNestedScrolling);
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    @Override
    public int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int parentSpace = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0) {
            return parentSpace;
        }

        View child = getChildAt(0);
        NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int scrollRange = child.getBottom() + lp.bottomMargin;
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - parentSpace);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    protected void measureChild(@Nonnull View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft()
                + getPaddingRight(), lp.width);

        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(@Nonnull View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {

        if (mScroller.isFinished()) {
            return;
        }

        mScroller.computeScrollOffset();
        final int y = mScroller.getCurrY();
        int unconsumed = y - mLastScrollerY;
        mLastScrollerY = y;

        // Nested Scrolling Pre Pass
        mScrollConsumed[1] = 0;
        dispatchNestedPreScroll(0, unconsumed, mScrollConsumed, null,
                TYPE_NON_TOUCH);
        unconsumed -= mScrollConsumed[1];

        final int range = getScrollRange();

        if (unconsumed != 0) {
            // Internal Scroll
            final int oldScrollY = getScrollY();
            overScrollByCompat(0, unconsumed, getScrollX(), oldScrollY, 0, range, 0, 0, false);
            final int scrolledByMe = getScrollY() - oldScrollY;
            unconsumed -= scrolledByMe;

            // Nested Scrolling Post Pass
            mScrollConsumed[1] = 0;
            dispatchNestedScroll(0, scrolledByMe, 0, unconsumed, mScrollOffset,
                    TYPE_NON_TOUCH, mScrollConsumed);
            unconsumed -= mScrollConsumed[1];
        }

        if (unconsumed != 0) {
            final int mode = getOverScrollMode();
            final boolean canOverscroll = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
            if (canOverscroll) {
                if (unconsumed < 0) {
                    if (mEdgeGlowTop.isFinished()) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    if (mEdgeGlowBottom.isFinished()) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            abortAnimatedScroll();
        }

        if (!mScroller.isFinished()) {
            postInvalidateOnAnimation();
        } else {
            stopNestedScroll(TYPE_NON_TOUCH);
        }
    }

    /**
     * If either of the vertical edge glows are currently active, this consumes part or all of
     * deltaY on the edge glow.
     *
     * @param deltaY The pointer motion, in pixels, in the vertical direction, positive
     *               for moving down and negative for moving up.
     * @param x      The vertical position of the pointer.
     * @return The amount of <code>deltaY</code> that has been consumed by the
     * edge glow.
     */
    private int releaseVerticalGlow(int deltaY, float x) {
        // First allow releasing existing overscroll effect:
        float consumed = 0;
        float displacement = x / getWidth();
        float pullDistance = (float) deltaY / getHeight();
        if (mEdgeGlowTop.getDistance() != 0) {
            consumed = -mEdgeGlowTop.onPullDistance(-pullDistance, displacement);
            if (mEdgeGlowTop.getDistance() == 0) {
                mEdgeGlowTop.onRelease();
            }
        } else if (mEdgeGlowBottom.getDistance() != 0) {
            consumed = mEdgeGlowBottom.onPullDistance(pullDistance, 1 - displacement);
            if (mEdgeGlowBottom.getDistance() == 0) {
                mEdgeGlowBottom.onRelease();
            }
        }
        int pixelsConsumed = Math.round(consumed * getHeight());
        if (pixelsConsumed != 0) {
            invalidate();
        }
        return pixelsConsumed;
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling) {
        if (participateInNestedScrolling) {
            startNestedScroll(SCROLL_AXIS_VERTICAL, TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(TYPE_NON_TOUCH);
        }
        mLastScrollerY = getScrollY();
        postInvalidateOnAnimation();
    }

    private void abortAnimatedScroll() {
        mScroller.abortAnimation();
        stopNestedScroll(TYPE_NON_TOUCH);
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    private void scrollToChild(@Nonnull View child) {
        child.getDrawingRect(mTempRect);

        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);

        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);

        if (scrollDelta != 0) {
            scrollBy(0, scrollDelta);
        }
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect      The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        final boolean scroll = delta != 0;
        if (scroll) {
            if (immediate) {
                scrollBy(0, delta);
            } else {
                smoothScrollBy(0, delta);
            }
        }
        return scroll;
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        int actualScreenBottom = screenBottom;

        int fadingEdge = 0;

        //TODO: screenTop should be incremented by fadingEdge * getTopFadingEdgeStrength (but for
        // the target scroll distance).
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        //TODO: screenBottom should be decremented by fadingEdge * getBottomFadingEdgeStrength (but
        // for the target scroll distance).
        // leave room for bottom fading edge as long as rect isn't at very bottom
        View child = getChildAt(0);
        final NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (rect.bottom < child.getHeight() + lp.topMargin + lp.bottomMargin) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += (rect.top - screenTop);
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += (rect.bottom - screenBottom);
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = child.getBottom() + lp.bottomMargin;
            int distanceToBottom = bottom - actualScreenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= (screenBottom - rect.bottom);
            } else {
                // entire rect at top
                scrollYDelta -= (screenTop - rect.top);
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mIsLayoutDirty) {
            scrollToChild(focused);
        } else {
            // The child may not be laid out yet, we can't compute the scroll yet
            mChildToScrollTo = focused;
        }
        super.requestChildFocus(child, focused);
    }

    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     * <p>
     * This is more expensive than the default {@link ViewGroup}
     * implementation, otherwise this behavior might have been made the default.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
                                                  Rect previouslyFocusedRect) {

        // convert from forward / backward notation to up / down / left / right
        // (ugh).
        if (direction == View.FOCUS_FORWARD) {
            direction = View.FOCUS_DOWN;
        } else if (direction == View.FOCUS_BACKWARD) {
            direction = View.FOCUS_UP;
        }

        final View nextFocus = previouslyFocusedRect == null
                ? FocusFinder.getInstance().findNextFocus(this, null, direction)
                : FocusFinder.getInstance().findNextFocusFromRect(
                this, previouslyFocusedRect, direction);

        if (nextFocus == null) {
            return false;
        }

        if (isOffScreen(nextFocus)) {
            return false;
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
                                                 boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(),
                child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        if (!mIsLaidOut) {
            // Make sure current scrollY position falls into the scroll range.  If it doesn't,
            // scroll such that it does.
            int childSize = 0;
            if (getChildCount() > 0) {
                View child = getChildAt(0);
                NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                childSize = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            }
            int parentSpace = b - t - getPaddingTop() - getPaddingBottom();
            int currentScrollY = getScrollY();
            int newScrollY = clamp(currentScrollY, parentSpace, childSize);
            if (newScrollY != currentScrollY) {
                scrollTo(getScrollX(), newScrollY);
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(getScrollX(), getScrollY());
        mIsLaidOut = true;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mIsLaidOut = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused) {
            return;
        }

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
        }
    }

    /**
     * Return true if child is a descendant of parent, (or equal to the parent).
     */
    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityY) {
        if (getChildCount() > 0) {

            mScroller.fling(getScrollX(), getScrollY(), // start
                    0, velocityY, // velocities
                    0, 0, // x
                    Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                    0, 0); // overscroll
            runAnimatedScroll(true);
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();
        stopNestedScroll(TYPE_TOUCH);

        mEdgeGlowTop.onRelease();
        mEdgeGlowBottom.onRelease();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            final NestedScrollView.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int parentSpaceHorizontal = getWidth() - getPaddingLeft() - getPaddingRight();
            int childSizeHorizontal = child.getWidth() + lp.leftMargin + lp.rightMargin;
            int parentSpaceVertical = getHeight() - getPaddingTop() - getPaddingBottom();
            int childSizeVertical = child.getHeight() + lp.topMargin + lp.bottomMargin;
            x = clamp(x, parentSpaceHorizontal, childSizeHorizontal);
            y = clamp(y, parentSpaceVertical, childSizeVertical);
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    @Override
    public void onDrawForeground(@Nonnull Canvas canvas) {
        super.onDrawForeground(canvas);
        final int scrollY = getScrollY();
        if (!mEdgeGlowTop.isFinished()) {
            final int restoreCount = canvas.save();
            int width = getWidth();
            int height = getHeight();
            int xTranslation = 0;
            int yTranslation = Math.min(0, scrollY);
            if (getClipToPadding()) {
                width -= getPaddingLeft() + getPaddingRight();
                xTranslation += getPaddingLeft();
                height -= getPaddingTop() + getPaddingBottom();
                yTranslation += getPaddingTop();
            }
            canvas.translate(xTranslation, yTranslation);
            mEdgeGlowTop.setSize(width, height);
            if (mEdgeGlowTop.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
        if (!mEdgeGlowBottom.isFinished()) {
            final int restoreCount = canvas.save();
            int width = getWidth();
            int height = getHeight();
            int xTranslation = 0;
            int yTranslation = Math.max(getScrollRange(), scrollY) + height;
            if (getClipToPadding()) {
                width -= getPaddingLeft() + getPaddingRight();
                xTranslation += getPaddingLeft();
                height -= getPaddingTop() + getPaddingBottom();
                yTranslation -= getPaddingBottom();
            }
            canvas.translate(xTranslation - width, yTranslation);
            canvas.rotate(180, width, 0);
            mEdgeGlowBottom.setSize(width, height);
            if (mEdgeGlowBottom.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
            return 0;
        }
        if ((my + n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
            return child - my;
        }
        return n;
    }
}
