/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.SystemTheme;
import icyllis.modernui.view.*;

import java.util.List;

/**
 * A view group that allows the view hierarchy placed within it to be scrolled.
 * Scroll view may have only one direct child placed within it.
 * To add multiple views within the scroll view, make
 * the direct child you add a view group, for example {@link LinearLayout}, and
 * place additional views within that LinearLayout.
 *
 * <p>Never add a {@link RecyclerView} or {@link ListView} to
 * a scroll view. Doing so results in poor user interface performance and a poor user
 * experience.</p>
 */
public class ScrollView extends FrameLayout {

    /**
     * When flinging the stretch towards scrolling content, it should destretch quicker than the
     * fling would normally do. The visual effect of flinging the stretch looks strange as little
     * appears to happen at first and then when the stretch disappears, the content starts
     * scrolling quickly.
     */
    private static final float FLING_DESTRETCH_FACTOR = 4f;

    private final Rect mTempRect = new Rect();

    private final OverScroller mScroller;

    /**
     * Tracks the state of the top edge glow.
     * <p>
     * Even though this field is practically final, we cannot make it final because there are apps
     * setting it via reflection and they need to keep working until they target Q.
     */
    private final EdgeEffect mEdgeGlowTop;

    /**
     * Tracks the state of the bottom edge glow.
     * <p>
     * Even though this field is practically final, we cannot make it final because there are apps
     * setting it via reflection and they need to keep working until they target Q.
     */
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

    private final int mOverscrollDistance;
    private final int mOverflingDistance;

    private final float mVerticalScrollFactor;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;

    public ScrollView(Context context) {
        super(context);
        mScroller = new OverScroller();
        mEdgeGlowTop = new EdgeEffect();
        mEdgeGlowBottom = new EdgeEffect();
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverflingDistance = configuration.getScaledOverflingDistance();
        mVerticalScrollFactor = configuration.getScaledVerticalScrollFactor();

        setVerticalScrollBarEnabled(true);
        ShapeDrawable thumb = new ShapeDrawable();
        thumb.setShape(ShapeDrawable.VLINE);
        thumb.setStroke(dp(4), SystemTheme.modulateColor(SystemTheme.COLOR_FOREGROUND, 0.25f));
        thumb.setCornerRadius(1);
        setVerticalScrollbarThumbDrawable(thumb);
        ShapeDrawable track = new ShapeDrawable();
        track.setShape(ShapeDrawable.VLINE);
        track.setStroke(dp(4), 0x40808080);
        track.setSize(dp(4), -1);
        track.setCornerRadius(1);
        setVerticalScrollbarTrackDrawable(track);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    /**
     * Sets the edge effect color for both top and bottom edge effects.
     *
     * @param color The color for the edge effects.
     * @see #setTopEdgeEffectColor(int)
     * @see #setBottomEdgeEffectColor(int)
     * @see #getTopEdgeEffectColor()
     * @see #getBottomEdgeEffectColor()
     */
    public void setEdgeEffectColor(@ColorInt int color) {
        setTopEdgeEffectColor(color);
        setBottomEdgeEffectColor(color);
    }

    /**
     * Sets the bottom edge effect color.
     *
     * @param color The color for the bottom edge effect.
     * @see #setTopEdgeEffectColor(int)
     * @see #setEdgeEffectColor(int)
     * @see #getTopEdgeEffectColor()
     * @see #getBottomEdgeEffectColor()
     */
    public void setBottomEdgeEffectColor(@ColorInt int color) {
        mEdgeGlowBottom.setColor(color);
    }

    /**
     * Sets the top edge effect color.
     *
     * @param color The color for the top edge effect.
     * @see #setBottomEdgeEffectColor(int)
     * @see #setEdgeEffectColor(int)
     * @see #getTopEdgeEffectColor()
     * @see #getBottomEdgeEffectColor()
     */
    public void setTopEdgeEffectColor(@ColorInt int color) {
        mEdgeGlowTop.setColor(color);
    }

    /**
     * Returns the top edge effect color.
     *
     * @return The top edge effect color.
     * @see #setEdgeEffectColor(int)
     * @see #setTopEdgeEffectColor(int)
     * @see #setBottomEdgeEffectColor(int)
     * @see #getBottomEdgeEffectColor()
     */
    @ColorInt
    public int getTopEdgeEffectColor() {
        return mEdgeGlowTop.getColor();
    }

    /**
     * Returns the bottom edge effect color.
     *
     * @return The bottom edge effect color.
     * @see #setEdgeEffectColor(int)
     * @see #setTopEdgeEffectColor(int)
     * @see #setBottomEdgeEffectColor(int)
     * @see #getTopEdgeEffectColor()
     */
    @ColorInt
    public int getBottomEdgeEffectColor() {
        return mEdgeGlowBottom.getColor();
    }

    /**
     * Sets the top edge effect blend mode, the default is {@link EdgeEffect#DEFAULT_BLEND_MODE}.
     *
     * @param blendMode The blend mode for the top edge effect.
     */
    public void setTopEdgeEffectBlendMode(@Nullable BlendMode blendMode) {
        mEdgeGlowTop.setBlendMode(blendMode);
    }

    /**
     * Sets the bottom edge effect blend mode, the default is {@link EdgeEffect#DEFAULT_BLEND_MODE}.
     *
     * @param blendMode The blend mode for the bottom edge effect.
     */
    public void setBottomEdgeEffectBlendMode(@Nullable BlendMode blendMode) {
        mEdgeGlowBottom.setBlendMode(blendMode);
    }

    @Nullable
    public BlendMode getTopEdgeEffectBlendMode() {
        return mEdgeGlowTop.getBlendMode();
    }

    @Nullable
    public BlendMode getBottomEdgeEffectBlendMode() {
        return mEdgeGlowBottom.getBlendMode();
    }

    @Override
    public void addView(@NonNull View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child);
    }

    @Override
    public void addView(@NonNull View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(@NonNull View child, @NonNull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, params);
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index, params);
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
     * Indicates this ScrollView whether it should stretch its content height to fill
     * the viewport or not.
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
            final View child = getChildAt(0);
            final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final int widthPadding = mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin;
            final int heightPadding = mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin;

            final int desiredHeight = getMeasuredHeight() - heightPadding;
            if (child.getMeasuredHeight() < desiredHeight) {
                final int childWidthMeasureSpec = getChildMeasureSpec(
                        widthMeasureSpec, widthPadding, lp.width);
                final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        desiredHeight, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
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
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        mTempRect.setEmpty();

        /*if (!canScroll()) {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_DOWN);
                return nextFocused != null
                        && nextFocused != this
                        && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }*/

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEY_UP -> handled = arrowScroll(View.FOCUS_UP);
                case KeyEvent.KEY_DOWN -> handled = arrowScroll(View.FOCUS_DOWN);
                case KeyEvent.KEY_HOME -> handled = fullScroll(View.FOCUS_UP);
                case KeyEvent.KEY_END -> handled = fullScroll(View.FOCUS_DOWN);
                case KeyEvent.KEY_SPACE -> handled = pageScroll(event.isShiftPressed() ? View.FOCUS_UP :
                        View.FOCUS_DOWN);
                case KeyEvent.KEY_PAGE_UP -> handled = pageScroll(View.FOCUS_UP);
                case KeyEvent.KEY_PAGE_DOWN -> handled = pageScroll(View.FOCUS_DOWN);
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = mScrollY;
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
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and they is moving their finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        if (super.onInterceptTouchEvent(ev)) {
            return true;
        }

        /*
         * Don't try to intercept touch if we can't scroll anyway.
         */
        if (getScrollY() == 0 && !canScrollVertically(1)) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE -> {

                /*
                 * Locally do absolute value. mLastMotionY is set to the y value
                 * of the down event.
                 */
                final int activePointerId = mActivePointerId;
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int y = (int) ev.getY();
                final int yDiff = Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop && (getNestedScrollAxes() & SCROLL_AXIS_VERTICAL) == 0) {
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
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                 */
                mScroller.computeScrollOffset();
                mIsBeingDragged = !mScroller.isFinished()/* || !mEdgeGlowBottom.isFinished()
                        || !mEdgeGlowTop.isFinished()*/;
                // Modern UI: do not intercept if edge effects are not finished
                // If user clicks too fast, edge effects will not finish, and events will always be intercepted

                // Catch the edge effect if it is active.
                if (!mEdgeGlowTop.isFinished()) {
                    mEdgeGlowTop.onPullDistance(0f, ev.getX() / getWidth());
                }
                if (!mEdgeGlowBottom.isFinished()) {
                    mEdgeGlowBottom.onPullDistance(0f, 1f - ev.getX() / getWidth());
                }
                startNestedScroll(SCROLL_AXIS_VERTICAL, TYPE_TOUCH);
            }
            case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                recycleVelocityTracker();
                if (mScroller.springBack(mScrollX, mScrollY, 0, 0, 0, getScrollRange())) {
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

    private boolean shouldDisplayEdgeEffects() {
        return getOverScrollMode() != OVER_SCROLL_NEVER;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        MotionEvent vtev = ev.copy();

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);

        switch (action) {
            case MotionEvent.ACTION_DOWN -> {
                if (getChildCount() == 0) {
                    vtev.recycle();
                    return false;
                }
                if (!mScroller.isFinished()) {
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
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                startNestedScroll(SCROLL_AXIS_VERTICAL, TYPE_TOUCH);
            }
            case MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == MotionEvent.INVALID_POINTER_ID) {
                    break;
                }
                final int y = (int) ev.getY();
                int deltaY = mLastMotionY - y;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset, TYPE_TOUCH)) {
                    deltaY -= mScrollConsumed[1];
                    vtev.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }
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
                    // Scroll to follow the motion event
                    mLastMotionY = y - mScrollOffset[1];

                    final int oldY = mScrollY;
                    final int range = getScrollRange();
                    final int overscrollMode = getOverScrollMode();
                    boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                            (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    final float displacement = ev.getX() / getWidth();
                    if (canOverscroll) {
                        int consumed = 0;
                        if (deltaY < 0 && mEdgeGlowBottom.getDistance() != 0f) {
                            consumed = Math.round(getHeight()
                                    * mEdgeGlowBottom.onPullDistance((float) deltaY / getHeight(),
                                    1 - displacement));
                        } else if (deltaY > 0 && mEdgeGlowTop.getDistance() != 0f) {
                            consumed = Math.round(-getHeight()
                                    * mEdgeGlowTop.onPullDistance((float) -deltaY / getHeight(),
                                    displacement));
                        }
                        deltaY -= consumed;
                    }

                    // Calling overScrollBy will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    if (overScrollBy(0, deltaY, 0, mScrollY, 0, range, 0, mOverscrollDistance, true)
                            && !hasNestedScrollingParent(TYPE_TOUCH)) {
                        // Break our velocity if we hit a scroll barrier.
                        mVelocityTracker.clear();
                    }

                    final int scrolledDeltaY = mScrollY - oldY;
                    final int unconsumedY = deltaY - scrolledDeltaY;
                    if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset, TYPE_TOUCH,
                            mScrollConsumed)) {
                        mLastMotionY -= mScrollOffset[1];
                        vtev.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                    } else if (canOverscroll && deltaY != 0f) {
                        final int pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            mEdgeGlowTop.onPullDistance((float) -deltaY / getHeight(),
                                    displacement);
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > range) {
                            mEdgeGlowBottom.onPullDistance((float) deltaY / getHeight(),
                                    1.f - displacement);
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                        if (shouldDisplayEdgeEffects()
                                && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished())) {
                            postInvalidateOnAnimation();
                        }
                    }
                }
            }
            case MotionEvent.ACTION_UP -> {
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity();

                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        flingWithNestedDispatch(-initialVelocity);
                    } else if (mScroller.springBack(mScrollX, mScrollY, 0, 0, 0,
                            getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                    endDrag();
                }
            }
            case MotionEvent.ACTION_CANCEL -> {
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(mScrollX, mScrollY, 0, 0, 0, getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                    endDrag();
                }
            }
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            final float axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            final int delta = Math.round(axisValue * mVerticalScrollFactor);
            if (Math.abs(axisValue) > 0.9 && Math.abs(delta) * 6 > mMinimumVelocity) {
                int deltaY = MathUtil.clamp(delta * 6, -mMaximumVelocity, mMaximumVelocity);
                flingWithNestedDispatch(-deltaY);
                return true;
            } else if (smoothScrollBy(-delta)) {
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            final int oldX = mScrollX;
            final int oldY = mScrollY;
            mScrollX = scrollX;
            mScrollY = scrollY;
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (clampedY) {
                mScroller.springBack(mScrollX, mScrollY, 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
        awakenScrollBars();
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0,
                    child.getHeight() - (getHeight() - mPaddingBottom - mPaddingTop));
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

                final boolean viewIsFullyContained = (top < viewTop) &&
                        (viewBottom < bottom);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            (topFocus && viewTop < focusCandidate.getTop()) ||
                                    (!topFocus && viewBottom > focusCandidate
                                            .getBottom());

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
                if (mTempRect.top + height > view.getBottom()) {
                    mTempRect.top = view.getBottom() - height;
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
     * @param direction the scroll direction: {@link View#FOCUS_UP}
     *                  to go the top of the view or
     *                  {@link View#FOCUS_DOWN} to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int direction) {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                mTempRect.bottom = view.getBottom() + mPaddingBottom;
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
     * @param direction the scroll direction: {@link View#FOCUS_UP}
     *                  to go upward, {@link View#FOCUS_DOWN} to downward
     * @param top       the top offset of the new area to be made visible
     * @param bottom    the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == View.FOCUS_UP;

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

        final int maxJump = (int) (getHeight() * 0.5f);

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
                    int daBottom = getChildAt(0).getBottom();
                    int screenBottom = getScrollY() + getHeight() - mPaddingBottom;
                    if (daBottom - screenBottom < maxJump) {
                        scrollDelta = daBottom - screenBottom;
                    }
                }
            }
            if (scrollDelta == 0) {
                return false;
            }
            doScrollY(direction == View.FOCUS_DOWN ? scrollDelta : -scrollDelta);
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is offscreen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
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
     * @return whether the descendant of this scroll view is scrolled offscreen.
     */
    private boolean isOffScreen(@NonNull View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0, getHeight());
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     * pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(@NonNull View descendant, int delta, int height) {
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
                smoothScrollBy(delta);
            } else {
                scrollBy(0, delta);
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param delta the number of pixels to scroll by on the Y axis
     * @return if actually scrolled
     */
    public final boolean smoothScrollBy(int delta) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return false;
        }
        delta = Math.max(0, Math.min(mScroller.getFinalY() + delta, getScrollRange())) - mScrollY;
        if (delta != 0) {
            mScroller.startScroll(mScrollX, mScrollY, 0, delta);
            postInvalidateOnAnimation();
            return true;
        }
        return false;
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int y) {
        smoothScrollBy(y - mScrollY);
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = getHeight() - mPaddingBottom - mPaddingTop;
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = getChildAt(0).getBottom();
        final int scrollY = mScrollY;
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected void measureChild(@NonNull View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, mPaddingLeft
                + mPaddingRight, lp.width);
        final int verticalPadding = mPaddingTop + mPaddingBottom;
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - verticalPadding),
                MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(@NonNull View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int usedTotal = mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin +
                heightUsed;
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - usedTotal),
                MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            // This is called at drawing time by ViewGroup.  We don't want to
            // re-show the scrollbars at this point, which scrollTo will do,
            // so we replicate most of scrollTo here.
            //
            //         It's a little odd to call onScrollChanged from inside the drawing.
            //
            //         It is, except when you remember that computeScroll() is used to
            //         animate scrolling. So unless we want to defer the onScrollChanged()
            //         until the end of the animated scrolling, we don't really have a
            //         choice here.
            //
            //         I agree.  The alternative, which I think would be worse, is to post
            //         something and tell the subclasses later.  This is bad because there
            //         will be a window where mScrollX/Y is different from what the app
            //         thinks it is.
            //
            int oldX = mScrollX;
            int oldY = mScrollY;
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            //TODO consumeFlingInStretch is buggy, do we need it? see also HorizontalScrollView
            //int deltaY = consumeFlingInStretch(y - oldY);

            if (oldX != x || oldY != y) {
                final int range = getScrollRange();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, range,
                        0, mOverflingDistance, false);
                onScrollChanged(mScrollX, mScrollY, oldX, oldY);

                if (canOverscroll) {
                    if (y < 0 && oldY >= 0) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (y > range && oldY <= range) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }

            if (!awakenScrollBars()) {
                // Keep on drawing until the animation has finished.
                postInvalidateOnAnimation();
            }
        }
    }

    /**
     * Used by consumeFlingInHorizontalStretch() and consumeFlinInVerticalStretch() for
     * consuming deltas from EdgeEffects
     * @param unconsumed The unconsumed delta that the EdgeEffets may consume
     * @return The unconsumed delta after the EdgeEffects have had an opportunity to consume.
     */
    private int consumeFlingInStretch(int unconsumed) {
        if (unconsumed > 0 && mEdgeGlowTop != null && mEdgeGlowTop.getDistance() != 0f) {
            int size = getHeight();
            float deltaDistance = -unconsumed * FLING_DESTRETCH_FACTOR / size;
            int consumed = Math.round(-size / FLING_DESTRETCH_FACTOR
                    * mEdgeGlowTop.onPullDistance(deltaDistance, 0.5f));
            if (consumed != unconsumed) {
                mEdgeGlowTop.finish();
            }
            return unconsumed - consumed;
        }
        if (unconsumed < 0 && mEdgeGlowBottom != null && mEdgeGlowBottom.getDistance() != 0f) {
            int size = getHeight();
            float deltaDistance = unconsumed * FLING_DESTRETCH_FACTOR / size;
            int consumed = Math.round(size / FLING_DESTRETCH_FACTOR
                    * mEdgeGlowBottom.onPullDistance(deltaDistance, 0.5f));
            if (consumed != unconsumed) {
                mEdgeGlowBottom.finish();
            }
            return unconsumed - consumed;
        }
        return unconsumed;
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    public void scrollToDescendant(@NonNull View child) {
        if (!mIsLayoutDirty) {
            child.getDrawingRect(mTempRect);

            /* Offset from child's local coordinates to ScrollView coordinates */
            offsetDescendantRectToMyCoords(child, mTempRect);

            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);

            if (scrollDelta != 0) {
                scrollBy(0, scrollDelta);
            }
        } else {
            mChildToScrollTo = child;
        }
    }

    /**
     * If rect is offscreen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect      The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        if (delta != 0) {
            if (immediate) {
                scrollBy(0, delta);
            } else {
                smoothScrollBy(delta);
            }
            return true;
        }
        return false;
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
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
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
        if (focused != null && focused.getRevealOnFocusHint()) {
            if (!mIsLayoutDirty) {
                scrollToDescendant(focused);
            } else {
                // The child may not be laid out yet, we can't compute the scroll yet
                mChildToScrollTo = focused;
            }
        }
        super.requestChildFocus(child, focused);
    }

    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off-screen.
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

        final View nextFocus = previouslyFocusedRect == null ?
                FocusFinder.getInstance().findNextFocus(this, null, direction) :
                FocusFinder.getInstance().findNextFocusFromRect(this,
                        previouslyFocusedRect, direction);

        if (nextFocus == null) {
            return false;
        }

        if (isOffScreen(nextFocus)) {
            return false;
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(@NonNull View child, @NonNull Rect rectangle,
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
            scrollToDescendant(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        if (!isLaidOut()) {
            final int childHeight = (getChildCount() > 0) ? getChildAt(0).getMeasuredHeight() : 0;
            final int scrollRange = Math.max(0,
                    childHeight - (b - t - mPaddingBottom - mPaddingTop));

            // Don't forget to clamp
            if (mScrollY > scrollRange) {
                mScrollY = scrollRange;
            } else if (mScrollY < 0) {
                mScrollY = 0;
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(mScrollX, mScrollY);
    }

    @Override
    protected void onSizeChanged(int w, int h, int prevWidth, int prevHeight) {
        super.onSizeChanged(w, h, prevWidth, prevHeight);

        View currentFocused = findFocus();
        if (currentFocused == null || currentFocused == this) {
            return;
        }

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, 0, prevHeight)) {
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
            int height = getHeight() - mPaddingBottom - mPaddingTop;
            int bottom = getChildAt(0).getHeight();

            mScroller.fling(mScrollX, mScrollY, 0, velocityY, 0, 0, 0,
                    Math.max(0, bottom - height), 0, height / 2);

            postInvalidateOnAnimation();
        }
    }

    private void flingWithNestedDispatch(int velocityY) {
        final boolean canFling = (mScrollY > 0 || velocityY > 0) &&
                (mScrollY < getScrollRange() || velocityY < 0);
        if (!dispatchNestedPreFling(0, velocityY)) {
            final boolean consumed = dispatchNestedFling(0, velocityY, canFling);
            if (canFling) {
                fling(velocityY);
            } else if (!consumed) {
                if (!mEdgeGlowTop.isFinished()) {
                    if (shouldAbsorb(mEdgeGlowTop, -velocityY)) {
                        mEdgeGlowTop.onAbsorb(-velocityY);
                    } else {
                        fling(velocityY);
                    }
                } else if (!mEdgeGlowBottom.isFinished()) {
                    if (shouldAbsorb(mEdgeGlowBottom, velocityY)) {
                        mEdgeGlowBottom.onAbsorb(velocityY);
                    } else {
                        fling(velocityY);
                    }
                }
            }
        }
    }

    /**
     * Returns true if edgeEffect should call onAbsorb() with veclocity or false if it should
     * animate with a fling. It will animate with a fling if the velocity will remove the
     * EdgeEffect through its normal operation.
     *
     * @param edgeEffect The EdgeEffect that might absorb the velocity.
     * @param velocity   The velocity of the fling motion
     * @return true if the velocity should be absorbed or false if it should be flung.
     */
    private boolean shouldAbsorb(EdgeEffect edgeEffect, int velocity) {
        if (velocity > 0) {
            return true;
        }
        float distance = edgeEffect.getDistance() * getHeight();

        // This is flinging without the spring, so let's see if it will fling past the overscroll
        float flingDistance = (float) mScroller.getSplineFlingDistance(-velocity);

        return flingDistance < distance;
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();

        if (shouldDisplayEdgeEffects()) {
            mEdgeGlowTop.onRelease();
            mEdgeGlowBottom.onRelease();
        }
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
            x = clamp(x, getWidth() - mPaddingRight - mPaddingLeft, child.getWidth());
            y = clamp(y, getHeight() - mPaddingBottom - mPaddingTop, child.getHeight());
            if (x != mScrollX || y != mScrollY) {
                super.scrollTo(x, y);
            }
        }
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return (axes & SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        super.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(SCROLL_AXIS_VERTICAL, type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed, int type, @NonNull int[] consumed) {
        final int oldScrollY = mScrollY;
        scrollBy(0, dyUnconsumed);
        final int myConsumed = mScrollY - oldScrollY;
        final int myUnconsumed = dyUnconsumed - myConsumed;
        consumed[1] += myConsumed;
        dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null, type, consumed);
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (!consumed) {
            flingWithNestedDispatch((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public void onDrawForeground(@NonNull Canvas canvas) {
        super.onDrawForeground(canvas);
        if (shouldDisplayEdgeEffects()) {
            final int scrollY = mScrollY;
            final boolean clipToPadding = getClipToPadding();
            if (!mEdgeGlowTop.isFinished()) {
                canvas.save();
                final int width;
                final int height;
                final float translateX;
                final float translateY;
                if (clipToPadding) {
                    width = getWidth() - mPaddingLeft - mPaddingRight;
                    height = getHeight() - mPaddingTop - mPaddingBottom;
                    translateX = mPaddingLeft;
                    translateY = mPaddingTop;
                } else {
                    width = getWidth();
                    height = getHeight();
                    translateX = 0;
                    translateY = 0;
                }
                canvas.translate(translateX, Math.min(0, scrollY) + translateY);
                mEdgeGlowTop.setSize(width, height);
                if (mEdgeGlowTop.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restore();
            }
            if (!mEdgeGlowBottom.isFinished()) {
                canvas.save();
                final int width;
                final int height;
                final float translateX;
                final float translateY;
                if (clipToPadding) {
                    width = getWidth() - mPaddingLeft - mPaddingRight;
                    height = getHeight() - mPaddingTop - mPaddingBottom;
                    translateX = mPaddingLeft;
                    translateY = mPaddingTop;
                } else {
                    width = getWidth();
                    height = getHeight();
                    translateX = 0;
                    translateY = 0;
                }
                canvas.translate(-width + translateX,
                        Math.max(getScrollRange(), scrollY) + height + translateY);
                canvas.rotate(180, width, 0);
                mEdgeGlowBottom.setSize(width, height);
                if (mEdgeGlowBottom.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restore();
            }
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
