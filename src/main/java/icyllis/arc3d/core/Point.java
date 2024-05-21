/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import org.jetbrains.annotations.Contract;

public class Point {

    /**
     * Returns true if the given vector is degenerate or infinite.
     * If so, the vector cannot be normalized.
     */
    @Contract(pure = true)
    public static boolean isDegenerate(
            final float dx, final float dy
    ) {
        return !Float.isFinite(dx) || !Float.isFinite(dy) || (dx == 0 && dy == 0);
    }

    /**
     * Returns true if two points are equal.
     */
    @Contract(pure = true)
    public static boolean equals(
            final float x1, final float y1,
            final float x2, final float y2
    ) {
        return isDegenerate(x1 - x2, y1 - y2);
    }

    /**
     * Returns true if two points are equal within the given tolerance.
     */
    @Contract(pure = true)
    public static boolean isApproxEqual(
            final float x1, final float y1,
            final float x2, final float y2,
            final float tolerance
    ) {
        assert tolerance >= 0;
        return Math.abs(x2 - x1) <= tolerance && Math.abs(y2 - y1) <= tolerance;
    }

    /**
     * Returns the dot product of vector a and vector b.
     *
     * @return product of input magnitudes and cosine of the angle between them
     */
    @Contract(pure = true)
    public static float dotProduct(
            final float ax, final float ay,
            final float bx, final float by
    ) {
        return ax * bx + ay * by;
    }

    /**
     * Returns the cross product of vector a and vector b.
     * <p>
     * a and b form three-dimensional vectors with z-axis value equal to zero. The
     * cross product is a three-dimensional vector with x-axis and y-axis values equal
     * to zero. The cross product z-axis component is returned.
     *
     * @return area spanned by vectors signed by angle direction
     */
    @Contract(pure = true)
    public static float crossProduct(
            final float ax, final float ay,
            final float bx, final float by
    ) {
        return ax * by - ay * bx;
    }

    public static boolean normalize(final float[] pos, final int off) {
        return setLength(pos, off, 1);
    }

    /**
     * Sets vector to (x, y) scaled to <var>length</var>, if possible. If new
     * length is zero or NaN, returns false; otherwise returns true.
     * <p>
     * The vector is given by (v[off], v[off+1]), <var>off</var> must be even.
     * <var>length</var> must be >= 0.
     */
    public static boolean setLength(final float[] pos, final int off, final float length) {
        double x = pos[off];
        double y = pos[off+1];
        double dmag = Math.sqrt(x * x + y * y);
        double dscale = (double) length / dmag;
        float newX = (float) (x * dscale);
        float newY = (float) (y * dscale);
        if (isDegenerate(newX, newY)) {
            return false;
        }
        pos[off] = newX;
        pos[off+1] = newY;
        return true;
    }

    @Contract(pure = true)
    public static float length(
            final float x, final float y
    ) {
        return (float) Math.sqrt((double) x * x + (double) y * y);
    }

    @Contract(pure = true)
    public static float lengthSq(
            final float x, final float y
    ) {
        return x * x + y * y;
    }

    @Contract(pure = true)
    public static float distanceTo(
            final float ax, final float ay,
            final float bx, final float by
    ) {
        return length(ax - bx, ay - by);
    }

    @Contract(pure = true)
    public static float distanceToSq(
            final float ax, final float ay,
            final float bx, final float by
    ) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    public static float distanceToLineBetweenSq(
            final float px, final float py,
            final float ax, final float ay,
            final float bx, final float by
    ) {
        float ux = bx - ax;
        float uy = by - ay;
        float vx = px - ax;
        float vy = py - ay;

        float uLengthSq = ux * ux + uy * uy;
        float det = ux * vy - uy * vx;
        float temp = det / uLengthSq * det;
        // It's possible we have a degenerate segment, or we're so far away it looks degenerate
        // In this case, return squared distance to point A.
        if (!Float.isFinite(temp)) {
            return vx * vx + vy * vy;
        }
        return temp;
    }

    public static float distanceToLineBetween(
            final float px, final float py,
            final float ax, final float ay,
            final float bx, final float by
    ) {
        return (float) Math.sqrt(
                distanceToLineBetweenSq(px, py, ax, ay, bx, by)
        );
    }

    public static float distanceToLineSegmentBetweenSq(
            final float px, final float py,
            final float ax, final float ay,
            final float bx, final float by
    ) {
        // See comments to distanceToLineBetweenSq. If the projection of c onto
        // u is between a and b then this returns the same result as that
        // function. Otherwise, it returns the distance to the closest of a and
        // b. Let the projection of v onto u be v'.  There are three cases:
        //    1. v' points opposite to u. c is not between a and b and is closer
        //       to a than b.
        //    2. v' points along u and has magnitude less than y. c is between
        //       a and b and the distance to the segment is the same as distance
        //       to the line ab.
        //    3. v' points along u and has greater magnitude than u. c is not
        //       between a and b and is closer to b than a.
        // v' = (u dot v) * u / |u|. So if (u dot v)/|u| is less than zero we're
        // in case 1. If (u dot v)/|u| is > |u| we are in case 3. Otherwise,
        // we're in case 2. We actually compare (u dot v) to 0 and |u|^2 to
        // avoid a sqrt to compute |u|.

        float ux = bx - ax;
        float uy = by - ay;
        float vx = px - ax;
        float vy = py - ay;

        float uDotV = ux * vx + uy * vy;

        if (uDotV <= 0) {
            // closest point is point A
            return vx * vx + vy * vy;
        }

        float uLengthSq = ux * ux + uy * uy;

        if (uDotV >= uLengthSq) {
            // closest point is point B
            return distanceToSq(bx, by, px, py);
        }

        // closest point is inside segment
        float det = ux * vy - uy * vx;
        float temp = det / uLengthSq * det;
        // It's possible we have a degenerate segment, or we're so far away it looks degenerate
        // In this case, return squared distance to point A.
        if (!Float.isFinite(temp)) {
            return vx * vx + vy * vy;
        }
        return temp;
    }

    public static float distanceToLineSegmentBetween(
            final float px, final float py,
            final float ax, final float ay,
            final float bx, final float by
    ) {
        return (float) Math.sqrt(
                distanceToLineSegmentBetweenSq(px, py, ax, ay, bx, by)
        );
    }
}
