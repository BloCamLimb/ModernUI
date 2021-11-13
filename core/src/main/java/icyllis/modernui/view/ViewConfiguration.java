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
     * Defines the duration in milliseconds of the pressed state in child
     * components.
     */
    private static final int PRESSED_STATE_DURATION = 64;

    /**
     * Distance a touch can wander before we think the user is scrolling in dips.
     * Note that this value defined here is only used as a fallback by legacy/misbehaving
     * applications that do not provide a Context for determining density/configuration-dependent
     * values.
     * <p>
     * To alter this value, see the configuration resource config_viewConfigurationTouchSlop
     * in frameworks/base/core/res/res/values/config.xml or the appropriate device resource overlay.
     * It may be appropriate to tweak this on a device-specific basis in an overlay based on
     * the characteristics of the touch panel and firmware.
     */
    private static final int TOUCH_SLOP = 8;

    /**
     * View scale factor, depends on user preference or display device.
     */
    private float mViewScale = 1.0f;

    private int mTouchSlop = TOUCH_SLOP;

    public ViewConfiguration() {
    }

    /**
     * Returns the global configuration.
     *
     * @return the global view configuration
     */
    public static ViewConfiguration get() {
        return sInstance;
    }

    /**
     * @return the duration in milliseconds of the pressed state in child
     * components.
     */
    public static int getPressedStateDuration() {
        return PRESSED_STATE_DURATION;
    }

    @ApiStatus.Internal
    public void setViewScale(float scale) {
        mViewScale = scale;
    }

    public float getViewScale() {
        return mViewScale;
    }

    /**
     * Get the size in pixels that matches the view layout standards.
     *
     * @param sip scaling-independent pixel
     * @return size in pixels
     */
    public int getViewSize(float sip) {
        return Math.round(sip * mViewScale);
    }

    /**
     * Get the size in pixels that matches the text layout standards.
     *
     * @param sip scaling-independent pixel
     * @return size in pixels
     */
    public int getTextSize(float sip) {
        return Math.round(sip * mViewScale);
    }

    @ApiStatus.Internal
    public void setTouchSlop(int touchSlop) {
        mTouchSlop = touchSlop;
    }

    /**
     * @return Distance in pixels a touch can wander before we think the user is scrolling
     */
    public int getScaledTouchSlop() {
        return mTouchSlop;
    }
}
