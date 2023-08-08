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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.util.SparseArray;
import org.jetbrains.annotations.ApiStatus;

/**
 * Contains methods to standard constants used in the UI for timeouts, sizes, and distances.
 */
public class ViewConfiguration {

    /**
     * Defines the width of the horizontal scrollbar and the height of the vertical scrollbar in
     * dips
     */
    public static final int SCROLL_BAR_SIZE = 8;

    /**
     * Defines the length of the fading edges in dips
     */
    private static final int FADING_EDGE_LENGTH = 12;

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
    private static final int DEFAULT_LONG_PRESS_TIMEOUT = 1000;

    /**
     * Defines the duration in milliseconds we will wait to see if a touch event
     * is a tap or a scroll. If the user does not move within this interval, it is
     * considered to be a tap.
     */
    private static final int TAP_TIMEOUT = 100;

    /**
     * Inset in dips to look for touchable content when the user touches the edge of the screen
     */
    private static final int EDGE_SLOP = 12;

    /**
     * Distance a touch can wander before we think the user is scrolling in dips.
     * Note that this value defined here is only used as a fallback by legacy/misbehaving
     * applications that do not provide a Context for determining density/configuration-dependent
     * values.
     */
    public static final int TOUCH_SLOP = 8;

    /**
     * Defines the minimum size of the touch target for a scrollbar in dips
     */
    public static final int MIN_SCROLLBAR_TOUCH_TARGET = 16;

    /**
     * Minimum velocity to initiate a fling, as measured in dips per second
     */
    public static final int MINIMUM_FLING_VELOCITY = 50;

    /**
     * Maximum velocity to initiate a fling, as measured in dips per second
     */
    public static final int MAXIMUM_FLING_VELOCITY = 8000;

    /**
     * Max distance in dips to overscroll for edge effects
     */
    public static final int OVERSCROLL_DISTANCE = 0;

    /**
     * Max distance in dips to overfling for edge effects
     */
    public static final int OVERFLING_DISTANCE = 12;

    /**
     * Amount to scroll in response to a horizontal {@link MotionEvent#ACTION_SCROLL} event,
     * in dips per axis value.
     */
    public static final float HORIZONTAL_SCROLL_FACTOR = 64;

    /**
     * Amount to scroll in response to a vertical {@link MotionEvent#ACTION_SCROLL} event,
     * in dips per axis value.
     */
    public static final float VERTICAL_SCROLL_FACTOR = 64;

    /**
     * Defines the duration in milliseconds before an end of a long press causes a tooltip to be
     * hidden.
     */
    private static final int LONG_PRESS_TOOLTIP_HIDE_TIMEOUT = 1500;

    /**
     * Defines the duration in milliseconds before a hover event causes a tooltip to be shown.
     */
    private static final int HOVER_TOOLTIP_SHOW_TIMEOUT = 500;

    /**
     * Defines the duration in milliseconds before mouse inactivity causes a tooltip to be hidden.
     */
    private static final int HOVER_TOOLTIP_HIDE_TIMEOUT = 30000;

    private final int mEdgeSlop;
    private final int mFadingEdgeLength;
    private final int mMinimumFlingVelocity;
    private final int mMaximumFlingVelocity;
    private final int mScrollbarSize;
    private final int mTouchSlop;
    private volatile int mMinScalingSpan;
    private final int mHoverSlop;
    private final int mMinScrollbarTouchTarget;
    private volatile int mDoubleTapTouchSlop;
    private volatile int mPagingTouchSlop;
    private volatile int mDoubleTapSlop;
    private volatile int mWindowTouchSlop;
    private final int mOverscrollDistance;
    private final int mOverflingDistance;
    private final float mVerticalScrollFactor;
    private final float mHorizontalScrollFactor;

    static final SparseArray<ViewConfiguration> sConfigurations =
            new SparseArray<>(2);

    ViewConfiguration(@NonNull Context context) {
        final Resources res = context.getResources();
        final DisplayMetrics metrics = res.getDisplayMetrics();

        final float density = metrics.density;

        mEdgeSlop = (int) (density * EDGE_SLOP + 0.5f);
        mFadingEdgeLength = (int) (density * FADING_EDGE_LENGTH + 0.5f);
        mScrollbarSize = (int) (density * SCROLL_BAR_SIZE + 0.5f);

        mTouchSlop = (int) (density * TOUCH_SLOP + 0.5f);
        mHoverSlop = mTouchSlop / 2;
        mMinScrollbarTouchTarget = (int) (density * MIN_SCROLLBAR_TOUCH_TARGET + 0.5f);

        mMinimumFlingVelocity = (int) (density * MINIMUM_FLING_VELOCITY + 0.5f);
        mMaximumFlingVelocity = (int) (density * MAXIMUM_FLING_VELOCITY + 0.5f);

        mVerticalScrollFactor = (int) (density * VERTICAL_SCROLL_FACTOR + 0.5f);
        mHorizontalScrollFactor = (int) (density * HORIZONTAL_SCROLL_FACTOR + 0.5f);

        mOverscrollDistance = (int) (density * OVERSCROLL_DISTANCE + 0.5f);
        mOverflingDistance = (int) (density * OVERFLING_DISTANCE + 0.5f);
    }

    /**
     * Returns a configuration for the specified visual {@link Context}. The configuration depends
     * on various parameters of the {@link Context}, like the dimension of the display or the
     * density of the display.
     *
     * @return the view configuration
     */
    @NonNull
    public static ViewConfiguration get(@NonNull Context context) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final int density = metrics.densityDpi;

        ViewConfiguration configuration = sConfigurations.get(density);
        if (configuration == null) {
            configuration = new ViewConfiguration(context);
            sConfigurations.put(density, configuration);
        }

        return configuration;
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

    /**
     * @return The width of the horizontal scrollbar and the height of the vertical
     * scrollbar in pixels
     */
    public int getScaledScrollbarSize() {
        return mScrollbarSize;
    }

    /**
     * @return Inset in pixels to look for touchable content when the user touches the edge of the
     * screen
     */
    public int getScaledEdgeSlop() {
        return mEdgeSlop;
    }

    /**
     * @return the length of the fading edges in pixels
     */
    public int getScaledFadingEdgeLength() {
        return mFadingEdgeLength;
    }

    /**
     * @return Distance in pixels a touch can wander before we think the user is scrolling
     */
    public int getScaledTouchSlop() {
        return mTouchSlop;
    }

    /**
     * @return Distance in pixels a hover can wander while it is still considered "stationary".
     */
    public int getScaledHoverSlop() {
        return mHoverSlop;
    }

    /**
     * @return the minimum size of the scrollbar thumb's touch target in pixels
     */
    public int getScaledMinScrollbarTouchTarget() {
        return mMinScrollbarTouchTarget;
    }

    /**
     * @return Minimum velocity to initiate a fling, as measured in pixels per second.
     */
    public int getScaledMinimumFlingVelocity() {
        return mMinimumFlingVelocity;
    }

    /**
     * @return Maximum velocity to initiate a fling, as measured in pixels per second.
     */
    public int getScaledMaximumFlingVelocity() {
        return mMaximumFlingVelocity;
    }

    /**
     * @return The maximum distance a View should overscroll by when showing edge effects (in
     * pixels).
     */
    public int getScaledOverscrollDistance() {
        return mOverscrollDistance;
    }

    /**
     * @return The maximum distance a View should overfling by when showing edge effects (in
     * pixels).
     */
    public int getScaledOverflingDistance() {
        return mOverflingDistance;
    }

    /**
     * @return Amount to scroll in response to a vertical {@link MotionEvent#ACTION_SCROLL} event.
     * Multiply this by the event's axis value to obtain the number of pixels to be scrolled.
     */
    public float getScaledVerticalScrollFactor() {
        return mVerticalScrollFactor;
    }

    /**
     * @return Amount to scroll in response to a horizontal {@link MotionEvent#ACTION_SCROLL} event.
     * Multiply this by the event's axis value to obtain the number of pixels to be scrolled.
     */
    public float getScaledHorizontalScrollFactor() {
        return mHorizontalScrollFactor;
    }

    /**
     * @return the duration in milliseconds before an end of a long press causes a tooltip to be
     * hidden
     */
    @ApiStatus.Internal
    public static int getLongPressTooltipHideTimeout() {
        return LONG_PRESS_TOOLTIP_HIDE_TIMEOUT;
    }

    /**
     * @return the duration in milliseconds before a hover event causes a tooltip to be shown
     */
    @ApiStatus.Internal
    public static int getHoverTooltipShowTimeout() {
        return HOVER_TOOLTIP_SHOW_TIMEOUT;
    }

    /**
     * @return the duration in milliseconds before mouse inactivity causes a tooltip to be hidden.
     */
    @ApiStatus.Internal
    public static int getHoverTooltipHideTimeout() {
        return HOVER_TOOLTIP_HIDE_TIMEOUT;
    }
}
