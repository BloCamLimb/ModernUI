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
     * View scale factor, depends on user preference or display device.
     */
    private float mViewScale = 1.0f;

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
}
