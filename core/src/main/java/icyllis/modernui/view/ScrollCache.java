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
 * GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.view;

import icyllis.modernui.graphics.Rect;

/**
 * <p>ScrollCache holds various fields used by a View when scrolling
 * is supported. This avoids keeping too many unused fields in most
 * instances of View.</p>
 */
final class ScrollCache implements Runnable {

    /**
     * Scrollbars are not visible
     */
    public static final int OFF = 0;

    /**
     * Scrollbars are visible
     */
    public static final int ON = 1;

    /**
     * Scrollbars are fading away
     */
    public static final int FADING = 2;

    private final View mHost;

    /**
     * The current state of the scrollbars: ON, OFF, or FADING
     */
    int mState = OFF;

    ScrollBar mScrollBar;

    int mScrollBarSize;
    int mScrollBarMinTouchTarget;

    boolean mFadeScrollBars = true;

    long mFadeStartTime;

    public int fadingEdgeLength;
    int mDefaultDelayBeforeFade;
    int mFadeDuration;

    final Rect mScrollBarBounds = new Rect();
    final Rect mScrollBarTouchBounds = new Rect();

    static final int NOT_DRAGGING = 0;
    static final int DRAGGING_VERTICAL_SCROLL_BAR = 1;
    static final int DRAGGING_HORIZONTAL_SCROLL_BAR = 2;

    int mScrollBarDraggingState = NOT_DRAGGING;

    float mScrollBarDraggingPos;

    ScrollCache(View host) {
        mHost = host;
        ViewConfiguration cfg = ViewConfiguration.get(host.getContext());
        mScrollBarSize = cfg.getScaledScrollbarSize();
        mScrollBarMinTouchTarget = cfg.getScaledMinScrollbarTouchTarget();
        mDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay();
        mFadeDuration = ViewConfiguration.getScrollBarFadeDuration();
    }

    @Override
    public void run() {
        mState = FADING;
        mHost.invalidate();
    }

    public static int getThumbLength(int size, int thickness, int extent, int range) {
        // Avoid the tiny thumb.
        final int minLength = thickness * 2;
        int length = Math.round((float) size * extent / range);
        if (length < minLength) {
            length = minLength;
        }
        return length;
    }

    public static int getThumbOffset(int size, int thumbLength, int extent, int range, int offset) {
        // Avoid the too-big thumb.
        int thumbOffset = Math.round((float) (size - thumbLength) * offset / (range - extent));
        if (thumbOffset > size - thumbLength) {
            thumbOffset = size - thumbLength;
        }
        return thumbOffset;
    }
}
