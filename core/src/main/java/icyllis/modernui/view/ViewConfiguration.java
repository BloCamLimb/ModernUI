/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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
     * Defines the height of the horizontal scrollbar and the width of the vertical scrollbar in
     * dips
     * @hidden
     */
    @ApiStatus.Internal
    public static final int SCROLL_BAR_SIZE = 4;
    /** @hidden */
    @ApiStatus.Internal
    public static int sScrollBarSize = SCROLL_BAR_SIZE;

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
     * @hidden
     */
    @ApiStatus.Internal
    public static final int TOUCH_SLOP = 4;
    /** @hidden */
    @ApiStatus.Internal
    public static int sTouchSlop = TOUCH_SLOP;
    /** @hidden */
    @ApiStatus.Internal
    public static int sHoverSlop = TOUCH_SLOP;

    /**
     * Defines the minimum size of the touch target for a scrollbar in dips
     * @hidden
     */
    @ApiStatus.Internal
    public static final int MIN_SCROLLBAR_TOUCH_TARGET = 16;
    /** @hidden */
    @ApiStatus.Internal
    public static int sMinScrollbarTouchTarget = MIN_SCROLLBAR_TOUCH_TARGET;

    /**
     * Minimum velocity to initiate a fling, as measured in dips per second
     * @hidden
     */
    @ApiStatus.Internal
    public static final int MINIMUM_FLING_VELOCITY = 50;
    /** @hidden */
    @ApiStatus.Internal
    public static int sMinimumFlingVelocity = MINIMUM_FLING_VELOCITY;

    /**
     * Maximum velocity to initiate a fling, as measured in dips per second
     * @hidden
     */
    @ApiStatus.Internal
    public static final int MAXIMUM_FLING_VELOCITY = 8000;
    /** @hidden */
    @ApiStatus.Internal
    public static int sMaximumFlingVelocity = MAXIMUM_FLING_VELOCITY;

    /**
     * The coefficient of friction applied to flings/scrolls.
     * @hidden
     */
    @ApiStatus.Internal
    public static final float SCROLL_FRICTION = 0.015f;
    /** @hidden */
    @ApiStatus.Internal
    public static float sScrollFriction = SCROLL_FRICTION;

    /**
     * Max distance in dips to overscroll for edge effects
     * @hidden
     */
    @ApiStatus.Internal
    public static final int OVERSCROLL_DISTANCE = 0;
    /** @hidden */
    @ApiStatus.Internal
    public static int sOverscrollDistance = OVERSCROLL_DISTANCE;

    /**
     * Max distance in dips to overfling for edge effects
     * @hidden
     */
    @ApiStatus.Internal
    public static final int OVERFLING_DISTANCE = 12;
    /** @hidden */
    @ApiStatus.Internal
    public static int sOverflingDistance = OVERFLING_DISTANCE;

    /**
     * Amount to scroll in response to a horizontal {@link MotionEvent#ACTION_SCROLL} event,
     * in dips per axis value.
     * @hidden
     */
    @ApiStatus.Internal
    public static final float HORIZONTAL_SCROLL_FACTOR = 64;
    /** @hidden */
    @ApiStatus.Internal
    public static float sHorizontalScrollFactor = HORIZONTAL_SCROLL_FACTOR;

    /**
     * Amount to scroll in response to a vertical {@link MotionEvent#ACTION_SCROLL} event,
     * in dips per axis value.
     * @hidden
     */
    @ApiStatus.Internal
    public static final float VERTICAL_SCROLL_FACTOR = 64;
    /** @hidden */
    @ApiStatus.Internal
    public static float sVerticalScrollFactor = VERTICAL_SCROLL_FACTOR;

    /**
     * Defines the duration in milliseconds before an end of a long press causes a tooltip to be
     * hidden.
     * @hidden
     */
    @ApiStatus.Internal
    public static final int LONG_PRESS_TOOLTIP_HIDE_TIMEOUT = 1500;
    /** @hidden */
    @ApiStatus.Internal
    public static int sLongPressTooltipHideTimeout = LONG_PRESS_TOOLTIP_HIDE_TIMEOUT;

    /**
     * Defines the duration in milliseconds before a hover event causes a tooltip to be shown.
     * @hidden
     */
    @ApiStatus.Internal
    public static final int HOVER_TOOLTIP_SHOW_TIMEOUT = 500;
    /** @hidden */
    @ApiStatus.Internal
    public static int sHoverTooltipShowTimeout = HOVER_TOOLTIP_SHOW_TIMEOUT;

    /**
     * Defines the duration in milliseconds before mouse inactivity causes a tooltip to be hidden.
     * @hidden
     */
    @ApiStatus.Internal
    public static final int HOVER_TOOLTIP_HIDE_TIMEOUT = 30000;
    /** @hidden */
    @ApiStatus.Internal
    public static int sHoverTooltipHideTimeout = HOVER_TOOLTIP_HIDE_TIMEOUT;

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
    private final int mPagingTouchSlop;
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
        mScrollbarSize = (int) (density * sScrollBarSize + 0.5f);

        mTouchSlop = (int) (density * sTouchSlop + 0.5f);
        mHoverSlop = (int) (density * sHoverSlop + 0.5f);
        mMinScrollbarTouchTarget = (int) (density * sMinScrollbarTouchTarget + 0.5f);
        mPagingTouchSlop = mTouchSlop * 2;

        mMinimumFlingVelocity = (int) (density * sMinimumFlingVelocity + 0.5f);
        mMaximumFlingVelocity = (int) (density * sMaximumFlingVelocity + 0.5f);

        mVerticalScrollFactor = (int) (density * sVerticalScrollFactor + 0.5f);
        mHorizontalScrollFactor = (int) (density * sHorizontalScrollFactor + 0.5f);

        mOverscrollDistance = (int) (density * sOverscrollDistance + 0.5f);
        mOverflingDistance = (int) (density * sOverflingDistance + 0.5f);
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
        final int density = (int) (metrics.density * DisplayMetrics.DENSITY_DEFAULT);

        ViewConfiguration configuration = sConfigurations.get(density);
        if (configuration == null) {
            configuration = new ViewConfiguration(context);
            sConfigurations.put(density, configuration);
        }

        return configuration;
    }

    /**
     * Removes cached ViewConfiguration instances, so that we can ensure `get` constructs a new
     * ViewConfiguration instance.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static void resetCache() {
        sConfigurations.clear();
    }

    /**
     * Sets the ViewConfiguration cached instance for a given Context.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static void setInstance(@NonNull Context context, ViewConfiguration instance) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final int density = (int) (metrics.density * DisplayMetrics.DENSITY_DEFAULT);
        sConfigurations.put(density, instance);
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
     * @return The height of the horizontal scrollbar and the width of the vertical
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
     * @return Distance in pixels a touch can wander before we think the user is scrolling a full
     * page
     */
    public int getScaledPagingTouchSlop() {
        return mPagingTouchSlop;
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
     * The amount of friction applied to scrolls and flings.
     *
     * @return A scalar dimensionless value representing the coefficient of
     *         friction.
     */
    public static float getScrollFriction() {
        return sScrollFriction;
    }

    /**
     * @return the duration in milliseconds before an end of a long press causes a tooltip to be
     * hidden
     */
    @ApiStatus.Internal
    public static int getLongPressTooltipHideTimeout() {
        return sLongPressTooltipHideTimeout;
    }

    /**
     * @return the duration in milliseconds before a hover event causes a tooltip to be shown
     */
    @ApiStatus.Internal
    public static int getHoverTooltipShowTimeout() {
        return sHoverTooltipShowTimeout;
    }

    /**
     * @return the duration in milliseconds before mouse inactivity causes a tooltip to be hidden.
     */
    @ApiStatus.Internal
    public static int getHoverTooltipHideTimeout() {
        return sHoverTooltipHideTimeout;
    }
}
