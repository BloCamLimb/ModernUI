/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.core;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * This class holds {@link Path} data.
 * <br>This class can be shared/accessed between multiple threads.
 * <p>
 * There is no native resources need to be tracked and released.
 * The reference count is only used to check whether these heavy buffers
 * are uniquely referenced by a Path object, otherwise when any of these
 * shared Path objects are modified, new buffers will be created.
 *
 * @see RefCnt#unique()
 */
public class PathRef extends RefCnt {

    private static final byte[] EMPTY_VERBS = {};
    private static final float[] EMPTY_COORDS = {};

    byte[] mVerbs;
    float[] mCoords; // x0 y0 x1 y1 x2 y2 ...

    int mNumVerbs;
    int mNumCoords;

    public PathRef() {
        this(EMPTY_VERBS, EMPTY_COORDS);
    }

    public PathRef(int numVerbs, int numCoords) {
        this(new byte[numVerbs], new float[numCoords]);
    }

    public PathRef(byte[] verbs, float[] coords) {
        mVerbs = verbs;
        mCoords = coords;
    }

    @Override
    protected void deallocate() {
        // noop
    }

    PathRef addVerb(byte verb) {
        mVerbs[mNumVerbs++] = verb;
        return this;
    }

    PathRef addPoint(float x, float y) {
        mCoords[mNumCoords++] = x;
        mCoords[mNumCoords++] = y;
        return this;
    }

    void copy(PathRef other, int incVerbs, int incCoords) {
        resetToSize(other.mNumVerbs, other.mNumCoords, incVerbs, incCoords);
    }

    void grow(int incVerbs, int incCoords) {
        if (incVerbs > 0)
            mVerbs = growVerbs(mVerbs, incVerbs);
        if (incCoords > 0)
            mCoords = growCoords(mCoords, incCoords);
    }

    @Nonnull
    private static byte[] growVerbs(@Nonnull byte[] buf, int inc) {
        final int cap = buf.length, grow;
        if (cap < 10) {
            grow = 10;
        } else if (cap > 500) {
            grow = Math.max(250, cap >> 3);
        } else {
            grow = cap >> 1;
        }
        return Arrays.copyOf(buf, cap + Math.max(grow, inc));
    }

    @Nonnull
    private static float[] growCoords(@Nonnull float[] buf, int inc) {
        final int cap = buf.length, grow;
        if (cap < 20) {
            grow = 20;
        } else if (cap > 1000) {
            grow = Math.max(500, cap >> 3);
        } else {
            grow = cap >> 1;
        }
        return Arrays.copyOf(buf, cap + Math.max(grow, inc));
    }

    private void resetToSize(int numVerbs, int numCoords,
                             int incVerbs, int incCoords) {
        int allocVerbs = numVerbs + incVerbs;
        if (allocVerbs > mVerbs.length) // pre-allocate
            mVerbs = Arrays.copyOf(mVerbs, allocVerbs);
        mNumVerbs = numVerbs;
        int allocCoords = numCoords + incCoords;
        if (allocCoords > mCoords.length) // pre-allocate
            mCoords = Arrays.copyOf(mCoords, allocCoords);
        mNumCoords = numCoords;
    }
}
