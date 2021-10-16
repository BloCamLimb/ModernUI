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
 * View config, including methods to standards used in UI.
 */
public class ViewConfig {

    private static final ViewConfig sInstance = new ViewConfig();

    /**
     * View scale, determined by user preference or depends on your device
     */
    private float mViewScale = 1;

    private ViewConfig() {
    }

    /**
     * @return global view config
     */
    public static ViewConfig get() {
        return sInstance;
    }

    public void setViewScale(float scale) {
        mViewScale = scale;
    }

    public float getViewScale() {
        return mViewScale;
    }

    /**
     * Get the size in pixels that matches the layout standards.
     *
     * @param sip scaling-independent pixel
     * @return size in pixels
     */
    public int getViewSize(float sip) {
        return Math.round(sip * mViewScale);
    }

    public int getTextSize(float sip) {
        return Math.round(sip * mViewScale);
    }
}
