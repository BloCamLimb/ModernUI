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

import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.math.Rect;

/**
 * Controls the scroll bar rendering for View only.
 */
final class ScrollCache implements AnimationHandler.FrameCallback {

    /**
     * Scrollbars are not visible
     */
    static final int OFF = 0;

    /**
     * Scrollbars are visible
     */
    static final int ON = 1;

    /**
     * Scrollbars are fading away
     */
    static final int FADING = 2;

    private final View mHost;

    /**
     * The current state of the scrollbars: ON, OFF, or FADING
     */
    int mState = OFF;

    ScrollBar mScrollBar;

    int mScrollBarSize = 12;

    boolean mFadeScrollBars = true;
    long mFadeStartTime;
    long mFadeDuration = 500;

    final Rect mScrollBarBounds = new Rect();
    final Rect mScrollBarTouchBounds = new Rect();

    ScrollCache(View host) {
        mHost = host;
    }

    @Override
    public void doAnimationFrame(long frameTime) {
        mState = FADING;
        mHost.invalidate();
        AnimationHandler.getInstance().unregister(this);
    }
}
