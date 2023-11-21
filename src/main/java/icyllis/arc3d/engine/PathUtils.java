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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.Point;
import icyllis.arc3d.engine.tessellate.WangsFormula;

public class PathUtils {

    public static final int MAX_CHOPS_PER_CURVE = 10;
    // We guarantee that no quad or cubic will ever produce more than this many points
    public static final int MAX_POINTS_PER_CURVE = 1 << MAX_CHOPS_PER_CURVE;

    private static final float MIN_CURVE_TOLERANCE = 1.0e-4f;

    /**
     * Returns the maximum number of points required when using a recursive chopping algorithm to
     * linearize the quadratic Bezier to the given error tolerance.
     * This is a power of two and will not exceed {@link #MAX_POINTS_PER_CURVE}.
     *
     * @see #generateQuadraticPoints(float, float, float, float, float, float, float, float[], int, int)
     */
    public static int countQuadraticPoints(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float tol
    ) {
        assert tol >= MIN_CURVE_TOLERANCE;
        int chops = WangsFormula.quadratic_log2(
                1.0f / tol,
                x0, y0, x1, y1, x2, y2
        );
        return 1 << Math.min(chops, MAX_POINTS_PER_CURVE);
    }

    // Tessellate, dst holds repeated x and y
    // Note: off and rem are in floats, not in points (x,y)
    // return value is also in floats
    public static int generateQuadraticPoints(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float tolSq,
            float[] dst, int off, int rem
    ) {
        if (rem < 4 ||
                (Point.distanceToLineSegmentSq(x1, y1, x0, y0, x2, y2)) < tolSq) {
            dst[off] = x2;
            dst[off + 1] = y2;
            return 2;
        }

        final float qx0 = (x0 + x1) * 0.5f;
        final float qy0 = (y0 + y1) * 0.5f;
        final float qx1 = (x1 + x2) * 0.5f;
        final float qy1 = (y1 + y2) * 0.5f;

        final float rx0 = (qx0 + qx1) * 0.5f;
        final float ry0 = (qy0 + qy1) * 0.5f;

        rem >>= 2;
        int ret = off;
        ret += generateQuadraticPoints(
                x0, y0,
                qx0, qy0,
                rx0, ry0,
                tolSq,
                dst, ret, rem
        );
        ret += generateQuadraticPoints(
                rx0, ry0,
                qx1, qy1,
                x2, y2,
                tolSq,
                dst, ret, rem
        );
        return ret - off;
    }

    /**
     * Returns the maximum number of points required when using a recursive chopping algorithm to
     * linearize the cubic Bezier to the given error tolerance.
     * This is a power of two and will not exceed {@link #MAX_POINTS_PER_CURVE}.
     *
     * @see #generateCubicPoints(float, float, float, float, float, float, float, float, float, float[], int, int)
     */
    public static int countCubicPoints(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float tol
    ) {
        assert tol >= MIN_CURVE_TOLERANCE;
        int chops = WangsFormula.cubic_log2(
                1.0f / tol,
                x0, y0, x1, y1, x2, y2, x3, y3
        );
        return 1 << Math.min(chops, MAX_POINTS_PER_CURVE);
    }

    // Tessellate, dst holds repeated x and y
    // Note: off and rem are in floats, not in points (x,y)
    // return value is also in floats
    public static int generateCubicPoints(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float tolSq,
            float[] dst, int off, int rem
    ) {
        if (rem < 4 ||
                (Point.distanceToLineSegmentSq(x1, y1, x0, y0, x3, y3) < tolSq &&
                        Point.distanceToLineSegmentSq(x2, y2, x0, y0, x3, y3) < tolSq)) {
            dst[off] = x3;
            dst[off + 1] = y3;
            return 2;
        }

        final float qx0 = (x0 + x1) * 0.5f;
        final float qy0 = (y0 + y1) * 0.5f;
        final float qx1 = (x1 + x2) * 0.5f;
        final float qy1 = (y1 + y2) * 0.5f;
        final float qx2 = (x2 + x3) * 0.5f;
        final float qy2 = (y2 + y3) * 0.5f;

        final float rx0 = (qx0 + qx1) * 0.5f;
        final float ry0 = (qy0 + qy1) * 0.5f;
        final float rx1 = (qx1 + qx2) * 0.5f;
        final float ry1 = (qy1 + qy2) * 0.5f;

        final float sx0 = (rx0 + rx1) * 0.5f;
        final float sy0 = (ry0 + ry1) * 0.5f;

        rem >>= 2;
        int ret = off;
        ret += generateCubicPoints(
                x0, y0,
                qx0, qy0,
                rx0, ry0,
                sx0, sy0,
                tolSq,
                dst, ret, rem
        );
        ret += generateCubicPoints(
                sx0, sy0,
                rx1, ry1,
                qx2, qy2,
                x3, y3,
                tolSq,
                dst, ret, rem
        );
        return ret - off;
    }

    protected PathUtils() {
        throw new UnsupportedOperationException();
    }
}
