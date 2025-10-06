/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class TabStops {

    protected float[] mStops;
    protected int mNumStops;
    protected float mTabWidth;

    protected TabStops() {
    }

    public TabStops(float[] stops, float tabWidth) {
        mStops = stops;
        mNumStops = stops == null ? 0 : stops.length;
        mTabWidth = tabWidth;
    }

    public float nextTab(float width) {
        final int ns = mNumStops;
        if (ns > 0) {
            float[] stops = mStops;
            for (int i = 0; i < ns; ++i) {
                float stop = stops[i];
                if (stop > width) {
                    return stop;
                }
            }
        }
        return nextDefaultStop(width, mTabWidth);
    }

    /**
     * Returns the position of next tab stop.
     */
    public static float nextDefaultStop(float width, float tabWidth) {
        return (int) (width / tabWidth + 1) * tabWidth;
    }
}
