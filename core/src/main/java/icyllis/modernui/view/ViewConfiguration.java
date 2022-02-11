/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view;

import org.jetbrains.annotations.ApiStatus;

/**
 * Contains methods to standard constants used in the UI for timeouts, sizes, and distances.
 */
public class ViewConfiguration {

    private static final ViewConfiguration sInstance = new ViewConfiguration();

    /**
     * Defines the width of the horizontal scrollbar and the height of the vertical scrollbar in
     * dips
     */
    private static final int SCROLL_BAR_SIZE = 8;

    /**
     * Duration of the fade when scrollbars fade away in milliseconds
     */
    private static final int SCROLL_BAR_FADE_DURATION = 500;

    /**
     * Default delay before the scrollbars fade in milliseconds
     */
    private static final int SCROLL_BAR_DEFAULT_DELAY = 600;

    /**
     * Defines the duration in milliseconds of the pressed state in child
     * components.
     */
    private static final int PRESSED_STATE_DURATION = 64;

    /**
     * Defines the default duration in milliseconds before a press turns into
     * a long press
     */
    private static final int DEFAULT_LONG_PRESS_TIMEOUT = 400;

    /**
     * Defines the duration in milliseconds we will wait to see if a touch event
     * is a tap or a scroll. If the user does not move within this interval, it is
     * considered to be a tap.
     */
    private static final int TAP_TIMEOUT = 100;

    /**
     * Distance a touch can wander before we think the user is scrolling in dips.
     * Note that this value defined here is only used as a fallback by legacy/misbehaving
     * applications that do not provide a Context for determining density/configuration-dependent
     * values.
     */
    private static final int TOUCH_SLOP = 8;

    /**
     * Defines the minimum size of the touch target for a scrollbar in dips
     */
    private static final int MIN_SCROLLBAR_TOUCH_TARGET = 16;

    /**
     * Minimum velocity to initiate a fling, as measured in dips per second
     */
    private static final int MINIMUM_FLING_VELOCITY = 50;

    /**
     * Maximum velocity to initiate a fling, as measured in dips per second
     */
    private static final int MAXIMUM_FLING_VELOCITY = 8000;

    /**
     * Max distance in dips to overscroll for edge effects
     */
    private static final int OVERSCROLL_DISTANCE = 0;

    /**
     * Max distance in dips to overfling for edge effects
     */
    private static final int OVERFLING_DISTANCE = 12;

    /**
     * Amount to scroll in response to a horizontal {@link MotionEvent#ACTION_SCROLL} event,
     * in dips per axis value.
     */
    private static final float HORIZONTAL_SCROLL_FACTOR = 64;

    /**
     * Amount to scroll in response to a vertical {@link MotionEvent#ACTION_SCROLL} event,
     * in dips per axis value.
     */
    private static final float VERTICAL_SCROLL_FACTOR = 64;

    /**
     * View scale factor, depends on user preference or display device.
     */
    private float mViewScale = 1.0f;
    private float mTextScale = 1.0f;
    private float mScaledTextScale = 1.0f;

    private int mEdgeSlop;
    private int mFadingEdgeLength;
    private int mMinimumFlingVelocity = MINIMUM_FLING_VELOCITY;
    private int mScaledMinimumFlingVelocity = MINIMUM_FLING_VELOCITY;
    private int mMaximumFlingVelocity = MAXIMUM_FLING_VELOCITY;
    private int mScaledMaximumFlingVelocity = MAXIMUM_FLING_VELOCITY;
    private int mScrollbarSize = SCROLL_BAR_SIZE;
    private int mScaledScrollbarSize = SCROLL_BAR_SIZE;
    private int mTouchSlop = TOUCH_SLOP;
    private int mScaledTouchSlop = TOUCH_SLOP;
    private int mMinScalingSpan;
    private int mHoverSlop;
    private int mMinScrollbarTouchTarget = MIN_SCROLLBAR_TOUCH_TARGET;
    private int mScaledMinScrollbarTouchTarget = MIN_SCROLLBAR_TOUCH_TARGET;
    private int mDoubleTapTouchSlop;
    private int mPagingTouchSlop;
    private int mDoubleTapSlop;
    private int mWindowTouchSlop;
    private int mOverscrollDistance = OVERSCROLL_DISTANCE;
    private int mScaledOverscrollDistance = OVERSCROLL_DISTANCE;
    private int mOverflingDistance = OVERFLING_DISTANCE;
    private int mScaledOverflingDistance = OVERFLING_DISTANCE;
    private float mVerticalScrollFactor = VERTICAL_SCROLL_FACTOR;
    private float mScaledVerticalScrollFactor = VERTICAL_SCROLL_FACTOR;
    private float mHorizontalScrollFactor = HORIZONTAL_SCROLL_FACTOR;
    private float mScaledHorizontalScrollFactor = HORIZONTAL_SCROLL_FACTOR;

    ViewConfiguration() {
    }

    /**
     * Returns the global configuration.
     *
     * @return the global view configuration
     */
    public static ViewConfiguration get() {
        return sInstance;
    }

    @ApiStatus.Internal
    public void setViewScale(float scale) {
        if (mViewScale == scale) {
            return;
        }
        mViewScale = scale;
        mScaledTextScale = scale * mTextScale;

        mScaledScrollbarSize = dp(mScrollbarSize);
        mScaledTouchSlop = dp(mTouchSlop);
        mScaledMinScrollbarTouchTarget = dp(mMinScrollbarTouchTarget);
        mScaledMinimumFlingVelocity = dp(mMinimumFlingVelocity);
        mScaledMaximumFlingVelocity = dp(mMaximumFlingVelocity);
        mScaledOverscrollDistance = dp(mOverscrollDistance);
        mScaledOverflingDistance = dp(mOverflingDistance);
        mScaledVerticalScrollFactor = dp(mVerticalScrollFactor);
        mScaledHorizontalScrollFactor = dp(mHorizontalScrollFactor);
    }

    @ApiStatus.Internal
    public void setTextScale(float scale) {
        if (mTextScale == scale) {
            return;
        }
        mTextScale = scale;
        mScaledTextScale = scale * mViewScale;
    }

    /**
     * @return the current view scale is used to convert scale-independent pixels to pixels
     */
    public float getViewScale() {
        return mViewScale;
    }

    /**
     * @return the current scaled text scale is used to convert scale-independent pixels to pixels
     */
    public float getTextScale() {
        return mTextScale;
    }

    /**
     * Get the size in pixels that matches the view layout standards.
     *
     * @param v scaling-independent pixel, relative to other views
     * @return converted size in pixels
     */
    public int dp(float v) {
        return (int) (v * mViewScale + 0.5f);
    }

    /**
     * Get the size in pixels that matches the text layout standards.
     *
     * @param v scaling-independent pixel, relative to other texts
     * @return converted size in pixels
     */
    public int sp(float v) {
        return (int) (v * mScaledTextScale + 0.5f);
    }

    /**
     * @return Duration of the fade when scrollbars fade away in milliseconds
     */
    public static int getScrollBarFadeDuration() {
        return SCROLL_BAR_FADE_DURATION;
    }

    /**
     * @return Default delay before the scrollbars fade in milliseconds
     */
    public static int getScrollDefaultDelay() {
        return SCROLL_BAR_DEFAULT_DELAY;
    }

    /**
     * @return the duration in milliseconds of the pressed state in child
     * components.
     */
    public static int getPressedStateDuration() {
        return PRESSED_STATE_DURATION;
    }

    /**
     * @return the duration in milliseconds before a press turns into
     * a long press
     */
    public static int getLongPressTimeout() {
        return DEFAULT_LONG_PRESS_TIMEOUT;
    }

    /**
     * @return the duration in milliseconds we will wait to see if a touch event
     * is a tap or a scroll. If the user does not move within this interval, it is
     * considered to be a tap.
     */
    public static int getTapTimeout() {
        return TAP_TIMEOUT;
    }

    @ApiStatus.Internal
    public void setScrollbarSize(int scrollbarSize) {
        mScrollbarSize = scrollbarSize;
        mScaledScrollbarSize = dp(scrollbarSize);
    }

    @ApiStatus.Internal
    public void setTouchSlop(int touchSlop) {
        mTouchSlop = touchSlop;
        mScaledTouchSlop = dp(touchSlop);
    }

    @ApiStatus.Internal
    public void setMinScrollbarTouchTarget(int minScrollbarTouchTarget) {
        mMinScrollbarTouchTarget = minScrollbarTouchTarget;
        mScaledMinScrollbarTouchTarget = dp(minScrollbarTouchTarget);
    }

    @ApiStatus.Internal
    public void setMinimumFlingVelocity(int minimumFlingVelocity) {
        mMinimumFlingVelocity = minimumFlingVelocity;
        mScaledMinimumFlingVelocity = dp(minimumFlingVelocity);
    }

    @ApiStatus.Internal
    public void setMaximumFlingVelocity(int maximumFlingVelocity) {
        mMaximumFlingVelocity = maximumFlingVelocity;
        mScaledMaximumFlingVelocity = dp(maximumFlingVelocity);
    }

    @ApiStatus.Internal
    public void setOverscrollDistance(int overscrollDistance) {
        mOverscrollDistance = overscrollDistance;
        mScaledOverscrollDistance = dp(overscrollDistance);
    }

    @ApiStatus.Internal
    public void setOverflingDistance(int overflingDistance) {
        mOverflingDistance = overflingDistance;
        mScaledOverflingDistance = dp(overflingDistance);
    }

    @ApiStatus.Internal
    public void setVerticalScrollFactor(float verticalScrollFactor) {
        mVerticalScrollFactor = verticalScrollFactor;
        mScaledVerticalScrollFactor = dp(verticalScrollFactor);
    }

    @ApiStatus.Internal
    public void setHorizontalScrollFactor(float horizontalScrollFactor) {
        mHorizontalScrollFactor = horizontalScrollFactor;
        mScaledHorizontalScrollFactor = dp(horizontalScrollFactor);
    }

    /**
     * @return The width of the horizontal scrollbar and the height of the vertical
     * scrollbar in pixels
     */
    public int getScaledScrollBarSize() {
        return mScaledScrollbarSize;
    }

    /**
     * @return Distance in pixels a touch can wander before we think the user is scrolling
     */
    public int getScaledTouchSlop() {
        return mScaledTouchSlop;
    }

    /**
     * @return the minimum size of the scrollbar thumb's touch target in pixels
     */
    public int getScaledMinScrollbarTouchTarget() {
        return mScaledMinScrollbarTouchTarget;
    }

    /**
     * @return Minimum velocity to initiate a fling, as measured in pixels per second.
     */
    public int getScaledMinimumFlingVelocity() {
        return mScaledMinimumFlingVelocity;
    }

    /**
     * @return Maximum velocity to initiate a fling, as measured in pixels per second.
     */
    public int getScaledMaximumFlingVelocity() {
        return mScaledMaximumFlingVelocity;
    }

    /**
     * @return The maximum distance a View should overscroll by when showing edge effects (in
     * pixels).
     */
    public int getScaledOverscrollDistance() {
        return mScaledOverscrollDistance;
    }

    /**
     * @return The maximum distance a View should overfling by when showing edge effects (in
     * pixels).
     */
    public int getScaledOverflingDistance() {
        return mScaledOverflingDistance;
    }

    /**
     * @return Amount to scroll in response to a horizontal {@link MotionEvent#ACTION_SCROLL} event.
     * Multiply this by the event's axis value to obtain the number of pixels to be scrolled.
     */
    public float getScaledHorizontalScrollFactor() {
        return mScaledHorizontalScrollFactor;
    }

    /**
     * @return Amount to scroll in response to a vertical {@link MotionEvent#ACTION_SCROLL} event.
     * Multiply this by the event's axis value to obtain the number of pixels to be scrolled.
     */
    public float getScaledVerticalScrollFactor() {
        return mScaledVerticalScrollFactor;
    }
}
