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

package icyllis.modernui.graphics;

import icyllis.arc3d.SharedPtr;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

//TODO
public class Path {

    /**
     * The winding rule constant for specifying an even-odd rule
     * for determining the interior of a path.<br>
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    public static final int WIND_EVEN_ODD = 0;
    /**
     * The winding rule constant for specifying a non-zero rule
     * for determining the interior of a path.<br>
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    public static final int WIND_NON_ZERO = 1;

    private static final byte
            VERB_MOVETO = 0,
            VERB_LINETO = 1,
            VERB_QUADTO = 2,
            VERB_CUBICTO = 3,
            VERB_CLOSE = 4;

    @SharedPtr
    private Ref mRef;

    private int mWindingRule;

    private boolean mHasInitialPoint;

    /**
     * Creates an empty Path with a default winding rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        mRef = RefCnt.create(Ref.EMPTY);
        mWindingRule = WIND_NON_ZERO;
    }

    public final Path moveTo(float x, float y) {
        editor(1, 2)
                .addVerb(VERB_MOVETO)
                .addPoint(x, y);
        mHasInitialPoint = true;
        return this;
    }

    public final Path relativeMoveTo(float dx, float dy) {
        final float px, py;
        final int n = mRef.mNumPoints;
        if (n > 1) {
            px = mRef.mPoints[n - 2];
            py = mRef.mPoints[n - 1];
        } else {
            assert false;
            px = py = 0;
        }
        return moveTo(px + dx, py + dy);
    }

    /**
     * Adds a line from the last point to the specified point (x, y).
     *
     * @param x the end of a line on x-axis
     * @param y the end of a line on y-axis
     */
    public final Path lineTo(float x, float y) {
        if (mHasInitialPoint)
            editor(1, 2)
                    .addVerb(VERB_LINETO)
                    .addPoint(x, y);
        else assert false;
        return this;
    }

    /**
     * Adds a line from the last point to the specified vector (dx, dy).
     *
     * @param dx the offset from last point to line end on x-axis
     * @param dy the offset from last point to line end on y-axis
     */
    public final Path relativeLineTo(float dx, float dy) {
        int n = mRef.mNumPoints;
        if (n > 1) {
            float px = mRef.mPoints[n - 2];
            float py = mRef.mPoints[n - 1];
            return lineTo(px + dx, py + dy);
        }
        assert false;
        return this;
    }

    /**
     * Adds a curved segment to the path, defined by two new points, by
     * drawing a Quadratic curve that intersects both the current
     * coordinates and the specified coordinates {@code (x2, y2)},
     * using the specified point {@code (x1, y1)} as a quadratic
     * parametric control point.
     *
     * @param x1 the X coordinate of the quadratic control point
     * @param y1 the Y coordinate of the quadratic control point
     * @param x2 the X coordinate of the final end point
     * @param y2 the Y coordinate of the final end point
     */
    public final Path quadTo(float x1, float y1,
                             float x2, float y2) {
        if (mHasInitialPoint)
            editor(1, 4)
                    .addVerb(VERB_QUADTO)
                    .addPoint(x1, y1)
                    .addPoint(x2, y2);
        else assert false;
        return this;
    }

    public final Path relativeQuadTo(float dx1, float dy1,
                                     float dx2, float dy2) {
        int n = mRef.mNumPoints;
        if (n > 1) {
            float px = mRef.mPoints[n - 2];
            float py = mRef.mPoints[n - 1];
            return quadTo(px + dx1, py + dy1, px + dx2, py + dy2);
        }
        assert false;
        return this;
    }

    public final Path cubicTo(float x1, float y1,
                              float x2, float y2,
                              float x3, float y3) {
        if (mHasInitialPoint)
            editor(1, 6)
                    .addVerb(VERB_CUBICTO)
                    .addPoint(x1, y1)
                    .addPoint(x2, y2)
                    .addPoint(x3, y3);
        else assert false;
        return this;
    }

    public final Path relativeCubicTo(float dx1, float dy1,
                                      float dx2, float dy2,
                                      float dx3, float dy3) {
        int n = mRef.mNumPoints;
        if (n > 1) {
            float px = mRef.mPoints[n - 2];
            float py = mRef.mPoints[n - 1];
            return cubicTo(px + dx1, py + dy1, px + dx2, py + dy2, px + dx3, py + dy3);
        }
        assert false;
        return this;
    }

    // make a deep copy of PathRef if shared, grow buffers if needed
    private Ref editor(int incVerbs, int incPoints) {
        assert incVerbs >= 0 && incPoints >= 0;
        if (mRef.unique()) {
            mRef.grow(incVerbs, incPoints);
        } else {
            Ref copy = new Ref(this);
            copy.copy(mRef, incVerbs, incPoints);
            mRef = RefCnt.move(mRef, copy);
        }
        return mRef;
    }

    /**
     * This class holds heavy buffers of Path.
     * <br>This class can be shared/accessed between multiple threads.
     * <p>
     * There is no native resources need to be tracked and released.
     * The reference count is only used to check whether these heavy buffers
     * are uniquely referenced by a Path object, otherwise when any of these
     * shared Path objects are modified, new buffers will be created.
     *
     * @see RefCnt#unique()
     */
    @ApiStatus.Internal
    public static final class Ref extends RefCnt implements Runnable {

        // the empty instance is always shared, it won't be modified
        private static final Ref EMPTY = new Ref();

        private static final byte[] EMPTY_VERBS = {};
        private static final float[] EMPTY_POINTS = {};

        private byte[] mVerbs;
        private float[] mPoints; // x0 y0 x1 y1 x2 y2 ...

        private int mNumVerbs;
        private int mNumPoints;

        private Ref() {
            mVerbs = EMPTY_VERBS;
            mPoints = EMPTY_POINTS;
        }

        private Ref(Path owner) {
            this(owner, EMPTY_VERBS, EMPTY_POINTS);
        }

        private Ref(Path owner, int numVerbs, int numPoints) {
            this(owner, new byte[numVerbs], new float[numPoints]);
        }

        private Ref(Path owner, byte[] verbs, float[] points) {
            Core.registerCleanup(owner, this);
            mVerbs = verbs;
            mPoints = points;
        }

        @Override
        protected void deallocate() {
            // noop
        }

        private Ref addVerb(byte verb) {
            mVerbs[mNumVerbs++] = verb;
            return this;
        }

        private Ref addPoint(float x, float y) {
            mPoints[mNumPoints++] = x;
            mPoints[mNumPoints++] = y;
            return this;
        }

        private void copy(Ref other, int incVerbs, int incPoints) {
            resetToSize(other.mNumVerbs, other.mNumPoints, incVerbs, incPoints);
        }

        private void grow(int incVerbs, int incPoints) {
            if (incVerbs > 0)
                mVerbs = growVerbs(mVerbs, incVerbs);
            if (incPoints > 0)
                mPoints = growPoints(mPoints, incPoints);
        }

        @NonNull
        private static byte[] growVerbs(@NonNull byte[] buf, int inc) {
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

        @NonNull
        private static float[] growPoints(@NonNull float[] buf, int inc) {
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
            if (allocCoords > mPoints.length) // pre-allocate
                mPoints = Arrays.copyOf(mPoints, allocCoords);
            mNumPoints = numCoords;
        }

        @Override
        public void run() {
            unref();
        }
    }
}
