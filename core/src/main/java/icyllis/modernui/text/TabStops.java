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

package icyllis.modernui.text;

import icyllis.modernui.text.style.TabStopSpan;

import javax.annotation.Nullable;
import java.util.Arrays;

public class TabStops {

    private float[] mStops;
    private int mNumStops;
    private float mTabWidth;

    public TabStops(@Nullable float[] stops, float tabWidth) {
        mStops = stops;
        mNumStops = stops == null ? 0 : stops.length;
        mTabWidth = tabWidth;
    }

    public TabStops(float tabWidth, Object[] spans) {
        reset(tabWidth, spans);
    }

    void reset(float tabWidth, Object[] spans) {
        mTabWidth = tabWidth;

        int ns = 0;
        if (spans != null) {
            float[] stops = mStops;
            for (Object o : spans) {
                if (o instanceof TabStopSpan) {
                    if (stops == null) {
                        stops = new float[10];
                    } else if (ns == stops.length) {
                        float[] nstops = new float[ns * 2];
                        System.arraycopy(stops, 0, nstops, 0, ns);
                        stops = nstops;
                    }
                    stops[ns++] = ((TabStopSpan) o).getTabStop();
                }
            }
            if (ns > 1) {
                Arrays.sort(stops, 0, ns);
            }
            if (stops != mStops) {
                mStops = stops;
            }
        }
        mNumStops = ns;
    }

    // return next tab width
    float nextTab(float currWidth) {
        final int ns = mNumStops;
        if (ns > 0) {
            float[] stops = mStops;
            for (int i = 0; i < ns; ++i) {
                float stop = stops[i];
                if (stop > currWidth) {
                    return stop;
                }
            }
        }
        return nextDefaultStop(currWidth, mTabWidth);
    }

    /**
     * Returns the position of next tab stop.
     */
    public static float nextDefaultStop(float currWidth, float tabWidth) {
        return (int) (currWidth / tabWidth + 1) * tabWidth;
    }
}
