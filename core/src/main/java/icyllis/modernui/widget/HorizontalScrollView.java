/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

import icyllis.modernui.R;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.*;

import java.util.List;

/**
 * Layout container for a view hierarchy that can be scrolled by the user,
 * allowing it to be larger than the physical display.  A HorizontalScrollView
 * is a {@link FrameLayout}, meaning you should place one child in it
 * containing the entire contents to scroll; this child may itself be a layout
 * manager with a complex hierarchy of objects.  A child that is often used
 * is a {@link LinearLayout} in a horizontal orientation, presenting a horizontal
 * array of top-level items that the user can scroll through.
 *
 * <p>The {@link TextView} class also
 * takes care of its own scrolling, so does not require a HorizontalScrollView, but
 * using the two together is possible to achieve the effect of a text view
 * within a larger container.
 *
 * <p>HorizontalScrollView only supports horizontal scrolling. For vertical scrolling,
 * use either {@link ScrollView} or {@link ListView}.
 */
// Modified from Android
public class HorizontalScrollView extends FrameLayout {

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
     * Tracks the state of the left edge glow.
     * <p>
     * Even though this field is practically final, we cannot make it final because there are apps
     * setting it via reflection and they need to keep working until they target Q.
     */
    private final EdgeEffect mEdgeGlowLeft;

    /**
     * Tracks the state of the bottom edge glow.
     * <p>
     * Even though this field is practically final, we cannot make it final because there are apps
     * setting it via reflection and they need to keep working until they target Q.
     */
    private final EdgeEffect mEdgeGlowRight;

    /**
     * Position of the last motion event.
     */
    private int mLastMotionX;

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

    private final float mHorizontalScrollFactor;

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;

    public HorizontalScrollView(Context context) {
        super(context);
        mScroller = new OverScroller();
        mEdgeGlowLeft = new EdgeEffect(context);
        mEdgeGlowRight = new EdgeEffect(context);
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverflingDistance = configuration.getScaledOverflingDistance();
        mHorizontalScrollFactor = configuration.getScaledHorizontalScrollFactor();

        ColorStateList tint = null;
        final TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(R.ns, R.attr.colorControlNormal, value, true)) {
            tint = context.getResources().loadColorStateList(value, context.getTheme());
        }
        setHorizontalScrollBarEnabled(true);
        ShapeDrawable thumb = new ShapeDrawable();
        thumb.setShape(ShapeDrawable.HLINE);
        thumb.setStroke(dp(4), 0x84ffffff);
        thumb.setCornerRadius(1);
        thumb.setTintList(tint);
        setHorizontalScrollbarThumbDrawable(thumb);
        ShapeDrawable track = new ShapeDrawable();
        track.setShape(ShapeDrawable.HLINE);
        track.setStroke(dp(4), 0x39ffffff);
        track.setSize(dp(4), -1);
        track.setCornerRadius(1);
        track.setTintList(tint);
        setHorizontalScrollbarTrackDrawable(track);
    }

    /*@Override
    protected float getLeftFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getHorizontalFadingEdgeLength();
        if (mScrollX < length) {
            return mScrollX / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getHorizontalFadingEdgeLength();
        final int rightEdge = getWidth() - mPaddingRight;
        final int span = getChildAt(0).getRight() - mScrollX - rightEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }*/

    /**
     * Sets the edge effect color for both left and right edge effects.
     *
     * @param color The color for the edge effects.
     * @see #setLeftEdgeEffectColor(int)
     * @see #setRightEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     * @see #getRightEdgeEffectColor()
     */
    public void setEdgeEffectColor(@ColorInt int color) {
        setLeftEdgeEffectColor(color);
        setRightEdgeEffectColor(color);
    }

    /**
     * Sets the right edge effect color.
     *
     * @param color The color for the right edge effect.
     * @see #setLeftEdgeEffectColor(int)
     * @see #setEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     * @see #getRightEdgeEffectColor()
     */
    public void setRightEdgeEffectColor(@ColorInt int color) {
        mEdgeGlowRight.setColor(color);
    }

    /**
     * Sets the left edge effect color.
     *
     * @param color The color for the left edge effect.
     * @see #setRightEdgeEffectColor(int)
     * @see #setEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     * @see #getRightEdgeEffectColor()
     */
    public void setLeftEdgeEffectColor(@ColorInt int color) {
        mEdgeGlowLeft.setColor(color);
    }

    /**
     * Returns the left edge effect color.
     *
     * @return The left edge effect color.
     * @see #setEdgeEffectColor(int)
     * @see #setLeftEdgeEffectColor(int)
     * @see #setRightEdgeEffectColor(int)
     * @see #getRightEdgeEffectColor()
     */
    @ColorInt
    public int getLeftEdgeEffectColor() {
        return mEdgeGlowLeft.getColor();
    }

    /**
     * Returns the right edge effect color.
     *
     * @return The right edge effect color.
     * @see #setEdgeEffectColor(int)
     * @see #setLeftEdgeEffectColor(int)
     * @see #setRightEdgeEffectColor(int)
     * @see #getLeftEdgeEffectColor()
     */
    @ColorInt
    public int getRightEdgeEffectColor() {
        return mEdgeGlowRight.getColor();
    }

    /**
     * Sets the left edge effect blend mode, the default is {@link EdgeEffect#DEFAULT_BLEND_MODE}.
     *
     * @param blendMode The blend mode for the left edge effect.
     */
    public void setLeftEdgeEffectBlendMode(@Nullable BlendMode blendMode) {
        mEdgeGlowLeft.setBlendMode(blendMode);
    }

    /**
     * Sets the right edge effect blend mode, the default is {@link EdgeEffect#DEFAULT_BLEND_MODE}.
     *
     * @param blendMode The blend mode for the right edge effect.
     */
    public void setRightEdgeEffectBlendMode(@Nullable BlendMode blendMode) {
        mEdgeGlowRight.setBlendMode(blendMode);
    }

    @Nullable
    public BlendMode getLeftEdgeEffectBlendMode() {
        return mEdgeGlowLeft.getBlendMode();
    }

    @Nullable
    public BlendMode getRightEdgeEffectBlendMode() {
        return mEdgeGlowRight.getBlendMode();
    }

    @Override
    public void addView(@NonNull View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }

        super.addView(child);
    }

    @Override
    public void addView(@NonNull View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }

        super.addView(child, index);
    }

    @Override
    public void addView(@NonNull View child, @NonNull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }

        super.addView(child, params);
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }

    /**
     * @return Returns true this HorizontalScrollView can be scrolled
     */
    private boolean canScroll() {
        View child = getChildAt(0);
        if (child != null) {
            int childWidth = child.getWidth();
            return getWidth() < childWidth + mPaddingLeft + mPaddingRight;
        }
        return false;
    }

    /**
     * Indicates whether this HorizontalScrollView's content is stretched to
     * fill the viewport.
     *
     * @return True if the content fills the viewport, false otherwise.
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }

    /**
     * Indicates this HorizontalScrollView whether it should stretch its content width
     * to fill the viewport or not.
     *
     * @param fillViewport True to stretch the content's width to the viewport's
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

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final int widthPadding;
            final int heightPadding;
            final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            widthPadding = mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin;
            heightPadding = mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin;

            int desiredWidth = getMeasuredWidth() - widthPadding;
            if (child.getMeasuredWidth() < desiredWidth) {
                final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        desiredWidth, MeasureSpec.EXACTLY);
                final int childHeightMeasureSpec = getChildMeasureSpec(
                        heightMeasureSpec, heightPadding, lp.height);
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
    public boolean executeKeyEvent(KeyEvent event) {
        mTempRect.setEmpty();

        if (!canScroll()) {
            if (isFocused()) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_RIGHT);
                return nextFocused != null && nextFocused != this &&
                        nextFocused.requestFocus(View.FOCUS_RIGHT);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEY_LEFT:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_LEFT);
                    } else {
                        handled = fullScroll(View.FOCUS_LEFT);
                    }
                    break;
                case KeyEvent.KEY_RIGHT:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_RIGHT);
                    } else {
                        handled = fullScroll(View.FOCUS_RIGHT);
                    }
                    break;
                case KeyEvent.KEY_SPACE:
                    handled = pageScroll(event.isShiftPressed() ? View.FOCUS_LEFT : View.FOCUS_RIGHT);
                    break;
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollX = mScrollX;
            final View child = getChildAt(0);
            return !(y < child.getTop()
                    || y >= child.getBottom()
                    || x < child.getLeft() - scrollX
                    || x >= child.getRight() - scrollX);
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
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
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        if (super.onInterceptTouchEvent(ev)) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE -> {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from their original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX is set to the x value
                 * of the down event.
                 */
                final int activePointerId = mActivePointerId;
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int x = (int) ev.getX();
                final int xDiff = Math.abs(x - mLastMotionX);
                if (xDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionX = x;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            case MotionEvent.ACTION_DOWN -> {
                final int x = (int) ev.getX();
                if (!inChild(x, (int) ev.getY())) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = x;
                mActivePointerId = ev.getPointerId(0);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't.  mScroller.isFinished should be false when
                 * being flinged.
                 */
                mIsBeingDragged = !mScroller.isFinished()/* || !mEdgeGlowLeft.isFinished()
                        || !mEdgeGlowRight.isFinished()*/;
                // Modern UI: do not intercept if edge effects are not finished
                // If user clicks too fast, edge effects will not finish, and events will always be intercepted

                // Catch the edge effect if it is active.
                if (!mEdgeGlowLeft.isFinished()) {
                    mEdgeGlowLeft.onPullDistance(0f, 1f - ev.getY() / getHeight());
                }
                if (!mEdgeGlowRight.isFinished()) {
                    mEdgeGlowRight.onPullDistance(0f, ev.getY() / getHeight());
                }
            }
            case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                if (mScroller.springBack(mScrollX, mScrollY, 0, getScrollRange(), 0, 0)) {
                    postInvalidateOnAnimation();
                }
            }
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
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
                mLastMotionX = (int) ev.getX();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == MotionEvent.INVALID_POINTER_ID) {
                    break;
                }
                final int x = (int) ev.getX();
                int deltaX = mLastMotionX - x;
                if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    mLastMotionX = x;

                    final int oldX = mScrollX;
                    final int range = getScrollRange();
                    final int overscrollMode = getOverScrollMode();
                    final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                            (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                    final float displacement = ev.getY() / getHeight();
                    if (canOverscroll) {
                        int consumed = 0;
                        if (deltaX < 0 && mEdgeGlowRight.getDistance() != 0f) {
                            consumed = Math.round(getWidth()
                                    * mEdgeGlowRight.onPullDistance((float) deltaX / getWidth(),
                                    displacement));
                        } else if (deltaX > 0 && mEdgeGlowLeft.getDistance() != 0f) {
                            consumed = Math.round(-getWidth()
                                    * mEdgeGlowLeft.onPullDistance((float) -deltaX / getWidth(),
                                    1 - displacement));
                        }
                        deltaX -= consumed;
                    }

                    // Calling overScrollBy will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    overScrollBy(deltaX, 0, mScrollX, 0, range, 0,
                            mOverscrollDistance, 0, true);

                    if (canOverscroll && deltaX != 0f) {
                        final int pulledToX = oldX + deltaX;
                        if (pulledToX < 0) {
                            mEdgeGlowLeft.onPullDistance((float) -deltaX / getWidth(),
                                    1.f - displacement);
                            if (!mEdgeGlowRight.isFinished()) {
                                mEdgeGlowRight.onRelease();
                            }
                        } else if (pulledToX > range) {
                            mEdgeGlowRight.onPullDistance((float) deltaX / getWidth(),
                                    displacement);
                            if (!mEdgeGlowLeft.isFinished()) {
                                mEdgeGlowLeft.onRelease();
                            }
                        }
                        if (shouldDisplayEdgeEffects()
                                && (!mEdgeGlowLeft.isFinished() || !mEdgeGlowRight.isFinished())) {
                            postInvalidateOnAnimation();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getXVelocity();

                    if (getChildCount() > 0) {
                        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                            fling(-initialVelocity);
                        } else {
                            if (mScroller.springBack(mScrollX, mScrollY, 0,
                                    getScrollRange(), 0, 0)) {
                                postInvalidateOnAnimation();
                            }
                        }
                    }

                    mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                    mIsBeingDragged = false;
                    recycleVelocityTracker();

                    if (shouldDisplayEdgeEffects()) {
                        mEdgeGlowLeft.onRelease();
                        mEdgeGlowRight.onRelease();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(mScrollX, mScrollY, 0, getScrollRange(), 0, 0)) {
                        postInvalidateOnAnimation();
                    }
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                    mIsBeingDragged = false;
                    recycleVelocityTracker();

                    if (shouldDisplayEdgeEffects()) {
                        mEdgeGlowLeft.onRelease();
                        mEdgeGlowRight.onRelease();
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            if (!mIsBeingDragged) {
                final float axisValue;
                if (event.isShiftPressed()) {
                    axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                } else {
                    axisValue = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                }

                final int delta = Math.round(axisValue * mHorizontalScrollFactor);
                if (Math.abs(axisValue) > 0.9 && Math.abs(delta) * 6 > mMinimumVelocity) {
                    int deltaX = MathUtil.clamp(delta * 6, -mMaximumVelocity, mMaximumVelocity);
                    fling(-deltaX);
                    return true;
                } else if (smoothScrollBy(-delta)) {
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            final int oldX = mScrollX;
            final int oldY = mScrollY;
            mScrollX = scrollX;
            mScrollY = scrollY;
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (clampedX) {
                mScroller.springBack(mScrollX, mScrollY, 0, getScrollRange(), 0, 0);
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
                    child.getWidth() - (getWidth() - mPaddingLeft - mPaddingRight));
        }
        return scrollRange;
    }

    /**
     * <p>
     * Finds the next focusable component that fits in this View's bounds
     * (excluding fading edges) pretending that this View's left is located at
     * the parameter left.
     * </p>
     *
     * @param leftFocus          look for a candidate is the one at the left of the bounds
     *                           if leftFocus is true, or at the right of the bounds if leftFocus
     *                           is false
     * @param left               the left offset of the bounds in which a focusable must be
     *                           found (the fading edge is assumed to start at this position)
     * @param preferredFocusable the View that has highest priority and will be
     *                           returned if it is within my bounds (null is valid)
     * @return the next focusable component in the bounds or null if none can be found
     */
    private View findFocusableViewInMyBounds(final boolean leftFocus,
                                             final int left, View preferredFocusable) {
        /*
         * The fading edge's transparent side should be considered for focus
         * since it's mostly visible, so we divide the actual fading edge length
         * by 2.
         */
        final int fadingEdgeLength = 0;//getHorizontalFadingEdgeLength() / 2;
        final int leftWithoutFadingEdge = left + fadingEdgeLength;
        final int rightWithoutFadingEdge = left + getWidth() - fadingEdgeLength;

        if ((preferredFocusable != null)
                && (preferredFocusable.getLeft() < rightWithoutFadingEdge)
                && (preferredFocusable.getRight() > leftWithoutFadingEdge)) {
            return preferredFocusable;
        }

        return findFocusableViewInBounds(leftFocus, leftWithoutFadingEdge,
                rightWithoutFadingEdge);
    }

    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param leftFocus look for a candidate is the one at the left of the bounds
     *                  if leftFocus is true, or at the right of the bounds if
     *                  leftFocus is false
     * @param left      the left offset of the bounds in which a focusable must be
     *                  found
     * @param right     the right offset of the bounds in which a focusable must
     *                  be found
     * @return the next focusable component in the bounds or null if none can
     * be found
     */
    private View findFocusableViewInBounds(boolean leftFocus, int left, int right) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its left is below the bound's
         * left, and its right is above the bound's right. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        for (View view : focusables) {
            int viewLeft = view.getLeft();
            int viewRight = view.getRight();

            if (left < viewRight && viewLeft < right) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (left < viewLeft) &&
                        (viewRight < right);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            (leftFocus && viewLeft < focusCandidate.getLeft()) ||
                                    (!leftFocus && viewRight > focusCandidate.getRight());

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
     * method will scroll the view by one page left or right and give the focus
     * to the leftmost/rightmost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_LEFT}
     *                  to go one page left or {@link View#FOCUS_RIGHT}
     *                  to go one page right
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean pageScroll(int direction) {
        boolean right = direction == View.FOCUS_RIGHT;
        int width = getWidth();

        if (right) {
            mTempRect.left = getScrollX() + width;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(0);
                if (mTempRect.left + width > view.getRight()) {
                    mTempRect.left = view.getRight() - width;
                }
            }
        } else {
            mTempRect.left = getScrollX() - width;
            if (mTempRect.left < 0) {
                mTempRect.left = 0;
            }
        }
        mTempRect.right = mTempRect.left + width;

        return scrollAndFocus(direction, mTempRect.left, mTempRect.right);
    }

    /**
     * <p>Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the left or right and give the focus
     * to the leftmost/rightmost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_LEFT}
     *                  to go the left of the view or {@link View#FOCUS_RIGHT}
     *                  to go the right
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int direction) {
        boolean right = direction == View.FOCUS_RIGHT;
        int width = getWidth();

        mTempRect.left = 0;
        mTempRect.right = width;

        if (right) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(0);
                mTempRect.right = view.getRight();
                mTempRect.left = mTempRect.right - width;
            }
        }

        return scrollAndFocus(direction, mTempRect.left, mTempRect.right);
    }

    /**
     * <p>Scrolls the view to make the area defined by <code>left</code> and
     * <code>right</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this scrollview.</p>
     *
     * @param direction the scroll direction: {@link View#FOCUS_LEFT}
     *                  to go left {@link View#FOCUS_RIGHT} to right
     * @param left      the left offset of the new area to be made visible
     * @param right     the right offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int left, int right) {
        boolean handled = true;

        int width = getWidth();
        int containerLeft = getScrollX();
        int containerRight = containerLeft + width;
        boolean goLeft = direction == View.FOCUS_LEFT;

        View newFocused = findFocusableViewInBounds(goLeft, left, right);
        if (newFocused == null) {
            newFocused = this;
        }

        if (left >= containerLeft && right <= containerRight) {
            handled = false;
        } else {
            int delta = goLeft ? (left - containerLeft) : (right - containerRight);
            doScrollX(delta);
        }

        if (newFocused != findFocus()) newFocused.requestFocus(direction);

        return handled;
    }

    /**
     * Handle scrolling in response to a left or right arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     *                  pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {

        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJump = (int) (getWidth() * 0.5f);

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump)) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollX(scrollDelta);
            nextFocused.requestFocus(direction);
        } else {
            // no new focus
            int scrollDelta = maxJump;

            if (direction == View.FOCUS_LEFT && getScrollX() < scrollDelta) {
                scrollDelta = getScrollX();
            } else if (direction == View.FOCUS_RIGHT && getChildCount() > 0) {

                int daRight = getChildAt(0).getRight();

                int screenRight = getScrollX() + getWidth();

                if (daRight - screenRight < maxJump) {
                    scrollDelta = daRight - screenRight;
                }
            }
            if (scrollDelta == 0) {
                return false;
            }
            doScrollX(direction == View.FOCUS_RIGHT ? scrollDelta : -scrollDelta);
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is off screen, give
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
     * @return whether the descendant of this scroll view is scrolled off
     * screen.
     */
    private boolean isOffScreen(View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0);
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     * pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int delta) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return (mTempRect.right + delta) >= getScrollX()
                && (mTempRect.left - delta) <= (getScrollX() + getWidth());
    }

    /**
     * Smooth scroll by a X delta
     *
     * @param delta the number of pixels to scroll by on the X axis
     */
    private void doScrollX(int delta) {
        if (delta != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(delta);
            } else {
                scrollBy(delta, 0);
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param delta the number of pixels to scroll by on the X axis
     * @return if actually scrolled
     */
    public final boolean smoothScrollBy(int delta) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return false;
        }
        final int scrollX = mScrollX;
        delta = Math.max(0, Math.min(scrollX + delta, getScrollRange())) - scrollX;
        if (delta != 0) {
            mScroller.startScroll(scrollX, mScrollY, delta, 0);
            postInvalidateOnAnimation();
            return true;
        }
        return false;
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     */
    public final void smoothScrollTo(int x) {
        smoothScrollBy(x - mScrollX);
    }

    /**
     * <p>The scroll range of a scroll view is the overall width of all of its
     * children.</p>
     */
    @Override
    protected int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int contentWidth = getWidth() - mPaddingLeft - mPaddingRight;
        if (count == 0) {
            return contentWidth;
        }

        int scrollRange = getChildAt(0).getRight();
        final int scrollX = mScrollX;
        final int overscrollRight = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overscrollRight) {
            scrollRange += scrollX - overscrollRight;
        }

        return scrollRange;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        final int horizontalPadding = mPaddingLeft + mPaddingRight;
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - horizontalPadding),
                MeasureSpec.UNSPECIFIED);

        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);
        final int usedTotal = mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin +
                widthUsed;
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - usedTotal),
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
            int deltaX = consumeFlingInStretch(x - oldX);

            if (deltaX != 0 || oldY != y) {
                final int range = getScrollRange();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

                overScrollBy(deltaX, y - oldY, oldX, oldY, range, 0,
                        mOverflingDistance, 0, false);
                onScrollChanged(mScrollX, mScrollY, oldX, oldY);

                if (canOverscroll && deltaX != 0) {
                    if (x < 0 && oldX >= 0) {
                        mEdgeGlowLeft.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (x > range && oldX <= range) {
                        mEdgeGlowRight.onAbsorb((int) mScroller.getCurrVelocity());
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
     *
     * @param unconsumed The unconsumed delta that the EdgeEffets may consume
     * @return The unconsumed delta after the EdgeEffects have had an opportunity to consume.
     */
    private int consumeFlingInStretch(int unconsumed) {
        if (unconsumed > 0 && mEdgeGlowLeft != null && mEdgeGlowLeft.getDistance() != 0f) {
            int size = getWidth();
            float deltaDistance = -unconsumed * FLING_DESTRETCH_FACTOR / size;
            int consumed = Math.round(-size / FLING_DESTRETCH_FACTOR
                    * mEdgeGlowLeft.onPullDistance(deltaDistance, 0.5f));
            if (consumed != unconsumed) {
                mEdgeGlowLeft.finish();
            }
            return unconsumed - consumed;
        }
        if (unconsumed < 0 && mEdgeGlowRight != null && mEdgeGlowRight.getDistance() != 0f) {
            int size = getWidth();
            float deltaDistance = unconsumed * FLING_DESTRETCH_FACTOR / size;
            int consumed = Math.round(size / FLING_DESTRETCH_FACTOR
                    * mEdgeGlowRight.onPullDistance(deltaDistance, 0.5f));
            if (consumed != unconsumed) {
                mEdgeGlowRight.finish();
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
    private void scrollToChild(View child) {
        child.getDrawingRect(mTempRect);

        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);

        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);

        if (scrollDelta != 0) {
            scrollBy(scrollDelta, 0);
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
                scrollBy(delta, 0);
            } else {
                smoothScrollBy(delta);
            }
        }
        return scroll;
    }

    /**
     * Compute the amount to scroll in the X direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) return 0;

        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;

        int scrollXDelta = 0;

        if (rect.right > screenRight && rect.left > screenLeft) {
            // need to move right to get it in view: move right just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.width() > width) {
                // just enough to get screen size chunk on
                scrollXDelta += (rect.left - screenLeft);
            } else {
                // get entire rect at right of screen
                scrollXDelta += (rect.right - screenRight);
            }

            // make sure we aren't scrolling beyond the end of our content
            int right = getChildAt(0).getRight();
            int distanceToRight = right - screenRight;
            scrollXDelta = Math.min(scrollXDelta, distanceToRight);

        } else if (rect.left < screenLeft && rect.right < screenRight) {
            // need to move right to get it in view: move right just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.width() > width) {
                // screen size chunk
                scrollXDelta -= (screenRight - rect.right);
            } else {
                // entire rect at left
                scrollXDelta -= (screenLeft - rect.left);
            }

            // make sure we aren't scrolling any further than the left our content
            scrollXDelta = Math.max(scrollXDelta, -getScrollX());
        }
        return scrollXDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (focused != null && focused.getRevealOnFocusHint()) {
            if (!mIsLayoutDirty) {
                scrollToChild(focused);
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
            direction = View.FOCUS_RIGHT;
        } else if (direction == View.FOCUS_BACKWARD) {
            direction = View.FOCUS_LEFT;
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
        int childWidth = 0;
        int childMargins = 0;

        if (getChildCount() > 0) {
            childWidth = getChildAt(0).getMeasuredWidth();
            LayoutParams childParams = (LayoutParams) getChildAt(0).getLayoutParams();
            childMargins = childParams.leftMargin + childParams.rightMargin;
        }

        final int available = r - l - getPaddingLeftWithForeground() -
                getPaddingRightWithForeground() - childMargins;

        final boolean forceLeftGravity = (childWidth > available);

        layoutChildren(l, t, r, b, forceLeftGravity);

        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        if (!isLaidOut()) {
            final int scrollRange = Math.max(0,
                    childWidth - (r - l - mPaddingLeft - mPaddingRight));

            if (isLayoutRtl()) {
                mScrollX = scrollRange - mScrollX;
            } // mScrollX default value is "0" for LTR

            // Don't forget to clamp
            if (mScrollX > scrollRange) {
                mScrollX = scrollRange;
            } else if (mScrollX < 0) {
                mScrollX = 0;
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(mScrollX, mScrollY);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused)
            return;

        final int maxJump = getWidth();

        if (isWithinDeltaOfScreen(currentFocused, maxJump)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollX(scrollDelta);
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
     * @param velocityX The initial velocity in the X direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the left.
     */
    public void fling(int velocityX) {
        if (getChildCount() > 0) {
            int width = getWidth() - mPaddingRight - mPaddingLeft;
            int right = getChildAt(0).getRight() - mPaddingLeft;

            int maxScroll = Math.max(0, right - width);

            boolean shouldFling = false;
            if (mScrollX == 0 && !mEdgeGlowLeft.isFinished()) {
                if (shouldAbsorb(mEdgeGlowLeft, -velocityX)) {
                    mEdgeGlowLeft.onAbsorb(-velocityX);
                } else {
                    shouldFling = true;
                }
            } else if (mScrollX == maxScroll && !mEdgeGlowRight.isFinished()) {
                if (shouldAbsorb(mEdgeGlowRight, velocityX)) {
                    mEdgeGlowRight.onAbsorb(velocityX);
                } else {
                    shouldFling = true;
                }
            } else {
                shouldFling = true;
            }
            if (shouldFling) {
                mScroller.fling(mScrollX, mScrollY, velocityX, 0, 0,
                        maxScroll, 0, 0, width / 2, 0);

                final boolean movingRight = velocityX > 0;

                View currentFocused = findFocus();
                View newFocused = findFocusableViewInMyBounds(movingRight,
                        mScroller.getFinalX(), currentFocused);

                if (newFocused == null) {
                    newFocused = this;
                }

                if (newFocused != currentFocused) {
                    newFocused.requestFocus(movingRight ? View.FOCUS_RIGHT : View.FOCUS_LEFT);
                }
            }

            postInvalidateOnAnimation();
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
        float distance = edgeEffect.getDistance() * getWidth();

        // This is flinging without the spring, so let's see if it will fling past the overscroll
        float flingDistance = (float) mScroller.getSplineFlingDistance(-velocity);

        return flingDistance < distance;
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

    private boolean shouldDisplayEdgeEffects() {
        return getOverScrollMode() != OVER_SCROLL_NEVER;
    }

    @SuppressWarnings({"SuspiciousNameCombination"})
    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        if (shouldDisplayEdgeEffects()) {
            final int scrollX = mScrollX;
            if (!mEdgeGlowLeft.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight() - mPaddingTop - mPaddingBottom;

                canvas.rotate(270);
                canvas.translate(-height + mPaddingTop, Math.min(0, scrollX));
                mEdgeGlowLeft.setSize(height, getWidth());
                if (mEdgeGlowLeft.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowRight.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                final int height = getHeight() - mPaddingTop - mPaddingBottom;

                canvas.rotate(90);
                canvas.translate(-mPaddingTop,
                        -(Math.max(getScrollRange(), scrollX) + width));
                mEdgeGlowRight.setSize(height, width);
                if (mEdgeGlowRight.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }
}
