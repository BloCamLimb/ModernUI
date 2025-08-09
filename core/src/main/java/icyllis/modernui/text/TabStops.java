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

package icyllis.modernui.text;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.style.TabStopSpan;

import java.util.Arrays;
import java.util.List;

public class TabStops {

    private float[] mStops;
    private int mNumStops;
    private float mTabWidth;

    public TabStops(float[] stops, float tabWidth) {
        mStops = stops;
        mNumStops = stops == null ? 0 : stops.length;
        mTabWidth = tabWidth;
    }

    public TabStops(float tabWidth, @NonNull List<?> spans) {
        reset(tabWidth, spans);
    }

    public void reset(float tabWidth, @NonNull List<?> spans) {
        mTabWidth = tabWidth;

        int ns = 0;
        float[] stops = mStops;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < spans.size(); i++) {
            Object o = spans.get(i);
            if (o instanceof TabStopSpan) {
                if (stops == null) {
                    stops = new float[2];
                } else if (ns == stops.length) {
                    float[] newStops = new float[ns << 1];
                    System.arraycopy(stops, 0, newStops, 0, ns);
                    stops = newStops;
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
        mNumStops = ns;
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
