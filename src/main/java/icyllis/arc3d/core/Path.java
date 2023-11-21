/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

public class Path {

    /**
     * The winding rule constant for specifying an even-odd rule
     * for determining the interior of a path.<br>
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;
    /**
     * The winding rule constant for specifying a non-zero rule
     * for determining the interior of a path.<br>
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    private static final byte
            VERB_MOVETO = PathIterator.VERB_MOVETO,
            VERB_LINETO = PathIterator.VERB_LINETO,
            VERB_QUADTO = PathIterator.VERB_QUADTO,
            VERB_CUBICTO = PathIterator.VERB_CUBICTO,
            VERB_CLOSE = PathIterator.VERB_CLOSE;

    // the empty instance is always shared, it won't be modified
    static final PathRef EMPTY_REF = new PathRef();

    @SharedPtr
    private PathRef mRef;

    private int mWindingRule;

    private boolean mHasInitialPoint;

    /**
     * Creates an empty Path with a default winding rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        mRef = RefCnt.create(EMPTY_REF);
        mWindingRule = WIND_NON_ZERO;
    }

    public final Path moveTo(float x, float y) {
        editor(1, 2)
                .addVerb(VERB_MOVETO)
                .addPoint(x, y);
        mHasInitialPoint = true;
        return this;
    }

    public final Path moveToRel(float dx, float dy) {
        final float px, py;
        final int n = mRef.mNumCoords;
        if (n > 1) {
            px = mRef.mCoords[n - 2];
            py = mRef.mCoords[n - 1];
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
    public final Path lineToRel(float dx, float dy) {
        int n = mRef.mNumCoords;
        if (n > 1) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
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

    public final Path quadToRel(float dx1, float dy1,
                                float dx2, float dy2) {
        int n = mRef.mNumCoords;
        if (n > 1) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
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

    public final Path cubicToRel(float dx1, float dy1,
                                 float dx2, float dy2,
                                 float dx3, float dy3) {
        int n = mRef.mNumCoords;
        if (n > 1) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
            return cubicTo(px + dx1, py + dy1, px + dx2, py + dy2, px + dx3, py + dy3);
        }
        assert false;
        return this;
    }

    // make a deep copy of PathRef if shared, grow buffers if needed
    private PathRef editor(int incVerbs, int incPoints) {
        assert incVerbs >= 0 && incPoints >= 0;
        if (mRef.unique()) {
            mRef.grow(incVerbs, incPoints);
        } else {
            PathRef copy = new PathRef();
            copy.copy(mRef, incVerbs, incPoints);
            mRef = RefCnt.move(mRef, copy);
        }
        return mRef;
    }
}
