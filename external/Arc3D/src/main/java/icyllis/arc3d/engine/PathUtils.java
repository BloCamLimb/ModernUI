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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.Point;
import icyllis.arc3d.engine.tessellate.WangsFormula;

/**
 * Tessellate paths.
 */
public class PathUtils {

    // When tessellating curved paths into linear segments, this defines the maximum distance in screen
    // space which a segment may deviate from the mathematically correct value. Above this value, the
    // segment will be subdivided.
    // This value was chosen to approximate the super-sampling accuracy of the raster path (16 samples,
    // or one quarter pixel).
    public static final float DEFAULT_TOLERANCE = 0.25f;

    public static final int MAX_CHOPS_PER_CURVE = 10;
    // We guarantee that no quad or cubic will ever produce more than this many points
    public static final int MAX_POINTS_PER_CURVE = 1 << MAX_CHOPS_PER_CURVE;

    private static final float MIN_CURVE_TOLERANCE = 1.0e-4f;

    /**
     * Returns the maximum number of points required when using a recursive chopping algorithm to
     * linearize the quadratic Bezier to the given error tolerance.
     * This is a power of two and will not exceed {@link #MAX_POINTS_PER_CURVE}.
     *
     * @see #generateQuadraticPoints
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
        return 1 << Math.min(chops, MAX_CHOPS_PER_CURVE);
    }

    /**
     * Tessellate, <var>dst</var> holds repeated x and y coordinates.
     * Note: <var>off</var> and <var>rem</var> are in floats, not in points (x,y).
     * Return value is also in floats.
     *
     * @param tolSq square of tolerance
     * @param off   starting index in dst
     * @param rem   max number of point coordinates
     * @return actual number of point coordinates
     */
    public static int generateQuadraticPoints(
            final float p0x, final float p0y,
            final float p1x, final float p1y,
            final float p2x, final float p2y,
            final float tolSq,
            float[] dst, int off, int rem
    ) {
        if (rem < 4 ||
                (Point.distanceToLineSegmentBetweenSq(p1x, p1y, p0x, p0y, p2x, p2y)) < tolSq) {
            dst[off] = p2x;
            dst[off + 1] = p2y;
            return 2;
        }

        final float q0x = (p0x + p1x) * 0.5f;
        final float q0y = (p0y + p1y) * 0.5f;
        final float q1x = (p1x + p2x) * 0.5f;
        final float q1y = (p1y + p2y) * 0.5f;

        final float r0x = (q0x + q1x) * 0.5f;
        final float r0y = (q0y + q1y) * 0.5f;

        rem >>= 1;
        int ret = off;
        ret += generateQuadraticPoints(
                p0x, p0y,
                q0x, q0y,
                r0x, r0y,
                tolSq,
                dst, ret, rem
        );
        ret += generateQuadraticPoints(
                r0x, r0y,
                q1x, q1y,
                p2x, p2y,
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
     * @see #generateCubicPoints
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
        return 1 << Math.min(chops, MAX_CHOPS_PER_CURVE);
    }

    /**
     * Tessellate, <var>dst</var> holds repeated x and y coordinates.
     * Note: <var>off</var> and <var>rem</var> are in floats, not in points (x,y).
     * Return value is also in floats.
     *
     * @param tolSq square of tolerance
     * @param off   starting index in dst
     * @param rem   max number of point coordinates
     * @return actual number of point coordinates
     */
    public static int generateCubicPoints(
            final float p0x, final float p0y,
            final float p1x, final float p1y,
            final float p2x, final float p2y,
            final float p3x, final float p3y,
            final float tolSq,
            float[] dst, int off, int rem
    ) {
        if (rem < 4 ||
                (Point.distanceToLineSegmentBetweenSq(p1x, p1y, p0x, p0y, p3x, p3y) < tolSq &&
                        Point.distanceToLineSegmentBetweenSq(p2x, p2y, p0x, p0y, p3x, p3y) < tolSq)) {
            dst[off] = p3x;
            dst[off + 1] = p3y;
            return 2;
        }

        final float q0x = (p0x + p1x) * 0.5f;
        final float q0y = (p0y + p1y) * 0.5f;
        final float q1x = (p1x + p2x) * 0.5f;
        final float q1y = (p1y + p2y) * 0.5f;
        final float q2x = (p2x + p3x) * 0.5f;
        final float q2y = (p2y + p3y) * 0.5f;

        final float r0x = (q0x + q1x) * 0.5f;
        final float r0y = (q0y + q1y) * 0.5f;
        final float r1x = (q1x + q2x) * 0.5f;
        final float r1y = (q1y + q2y) * 0.5f;

        final float s0x = (r0x + r1x) * 0.5f;
        final float s0y = (r0y + r1y) * 0.5f;

        rem >>= 1;
        int ret = off;
        ret += generateCubicPoints(
                p0x, p0y,
                q0x, q0y,
                r0x, r0y,
                s0x, s0y,
                tolSq,
                dst, ret, rem
        );
        ret += generateCubicPoints(
                s0x, s0y,
                r1x, r1y,
                q2x, q2y,
                p3x, p3y,
                tolSq,
                dst, ret, rem
        );
        return ret - off;
    }

    protected PathUtils() {
        throw new UnsupportedOperationException();
    }
}
