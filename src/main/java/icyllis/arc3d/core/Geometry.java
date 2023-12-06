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

import javax.annotation.Nullable;

/**
 * Geometry helper class.
 */
public class Geometry {

    static int valid_divide(float numer, float denom,
                            final float[] ratio, final int off) {
        float r = numer / denom;
        if (!Float.isFinite(r)) {
            return 0;
        }
        if (r == 0) {
            // catch underflow
            return 0;
        }
        ratio[off] = r;
        return 1;
    }

    static int valid_unit_divide(float numer, float denom,
                                 final float[] ratio, final int off) {
        if (numer < 0) {
            numer = -numer;
            denom = -denom;
        }

        if (denom == 0 || numer == 0 || numer >= denom) {
            return 0;
        }

        float r = numer / denom;
        if (Float.isNaN(r)) {
            return 0;
        }
        assert r >= 0 && r < 1;
        if (r == 0) {
            // catch underflow
            return 0;
        }
        ratio[off] = r;
        return 1;
    }

    public static int findQuadRoots(final float A, final float B, final float C,
                                    final float[] roots, final int off) {
        if (A == 0.0f) {
            return valid_divide(-C, B, roots, off);
        }

        // use doubles so we don't overflow temporarily trying to compute R
        double dis = (double) B * B - 4.0d * A * C;
        if (dis < 0) {
            return 0;
        }
        float R = (float) Math.sqrt(dis);
        if (!Float.isFinite(R)) {
            return 0;
        }

        int ret = off;

        float Q;
        if (B < 0) {
            Q = -(B - R) / 2;
            ret += valid_divide(Q, A, roots, ret);
            ret += valid_divide(C, Q, roots, ret);
        } else {
            Q = -(B + R) / 2;
            ret += valid_divide(C, Q, roots, ret);
            ret += valid_divide(Q, A, roots, ret);
        }

        if (ret - off == 2 && roots[off] == roots[off + 1]) {
            return 1; // skip the multiple root
        }
        return ret - off;
    }

    // roots are sorted
    public static int findUnitQuadRoots(final float A, final float B, final float C,
                                        final float[] roots, final int off) {
        if (A == 0.0f) {
            return valid_unit_divide(-C, B, roots, off);
        }

        // use doubles so we don't overflow temporarily trying to compute R
        double dis = (double) B * B - 4.0d * A * C;
        if (dis < 0) {
            return 0;
        }
        float R = (float) Math.sqrt(dis);
        if (!Float.isFinite(R)) {
            return 0;
        }

        int ret = off;

        float Q = (B < 0) ? -(B - R) / 2 : -(B + R) / 2;
        ret += valid_unit_divide(Q, A, roots, ret);
        ret += valid_unit_divide(C, Q, roots, ret);

        if (ret - off == 2) {
            if (roots[off] > roots[off + 1]) {
                float tmp = roots[off];
                roots[off] = roots[off + 1];
                roots[off + 1] = tmp;
            } else if (roots[off] == roots[off + 1]) {
                ret--; // skip the multiple root
            }
        }
        return ret - off;
    }

    public static int findCubicInflectionPoints(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float[] roots, final int off) {

        // find the parameter value `t` where curvature is zero
        // P(t) = (1-t)^3 * b0 + 3*t * (1-t)^2 * b1 + 3*t^2 * (1-t) * b2 + t^3 * b3
        // let curvature k(t) = |P'(t) cross P''(t)| / |P'(t)^3| = 0
        final float Ax = x1 - x0;
        final float Ay = y1 - y0;
        final float Bx = x2 - 2 * x1 + x0;
        final float By = y2 - 2 * y1 + y0;
        final float Cx = x3 + 3 * (x1 - x2) - x0;
        final float Cy = y3 + 3 * (y1 - y2) - y0;

        return findUnitQuadRoots(
                Bx * Cy - By * Cx,
                Ax * Cy - Ay * Cx,
                Ax * By - Ay * Bx,
                roots, off);
    }

    static void eval_cubic_derivative(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float t,
            final float[] dst, final int off
    ) {

        float Ax = x3 + 3 * (x1 - x2) - x0;
        float Ay = y3 + 3 * (y1 - y2) - y0;
        float Bx = 2 * (x2 - (x1 + x1) + x0);
        float By = 2 * (y2 - (y1 + y1) + y0);
        float Cx = (x1 - x0);
        float Cy = (y1 - y0);

        dst[off]   = (Ax * t + Bx) * t + Cx;
        dst[off+1] = (Ay * t + By) * t + Cy;
    }

    static void eval_cubic_second_derivative(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float t,
            final float[] dst, final int off
    ) {

        float Ax = x3 + 3 * (x1 - x2) - x0;
        float Ay = y3 + 3 * (y1 - y2) - y0;
        float Bx = (x2 - (x1 + x1) + x0);
        float By = (y2 - (y1 + y1) + y0);

        dst[off]   = Ax * t + Bx;
        dst[off+1] = Ay * t + By;
    }

    public static void evalCubicAt(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float t,
            @Nullable final float[] pos, final int posOff,
            @Nullable final float[] tangent, final int tangentOff,
            @Nullable final float[] curvature, final int curvatureOff
    ) {
        assert t >= 0 && t <= 1;

        float Ax = x3 + 3 * (x1 - x2) - x0;
        float Ay = y3 + 3 * (y1 - y2) - y0;

        if (pos != null) {
            float Bx = 3 * (x2 - (x1 + x1) + x0);
            float By = 3 * (y2 - (y1 + y1) + y0);
            float Cx = 3 * (x1 - x0);
            float Cy = 3 * (y1 - y0);

            pos[posOff]   = ((Ax * t + Bx) * t + Cx) * t + x0;
            pos[posOff+1] = ((Ay * t + By) * t + Cy) * t + y0;
        }

        if (tangent != null) {
            // The derivative equation returns a zero tangent vector when t is 0 or 1, and the
            // adjacent control point is equal to the end point. In this case, use the
            // next control point or the end points to compute the tangent.
            if ((t == 0 && x0 == x1 && y0 == y1) ||
                    (t == 1 && x2 == x3 && y2 == y3)) {
                float Tx;
                float Ty;
                if (t == 0) {
                    Tx = x2 - x0;
                    Ty = y2 - y0;
                } else {
                    Tx = x3 - x1;
                    Ty = y3 - y1;
                }
                if (Tx == 0 && Ty == 0) {
                    tangent[tangentOff]   = x3 - x0;
                    tangent[tangentOff+1] = y3 - y0;
                } else {
                    tangent[tangentOff]   = Tx;
                    tangent[tangentOff+1] = Ty;
                }
            } else {
                // inline eval_cubic_derivative
                float Bx = 2 * (x2 - (x1 + x1) + x0);
                float By = 2 * (y2 - (y1 + y1) + y0);
                float Cx = (x1 - x0);
                float Cy = (y1 - y0);

                tangent[tangentOff]   = (Ax * t + Bx) * t + Cx;
                tangent[tangentOff+1] = (Ay * t + By) * t + Cy;
            }
        }

        if (curvature != null) {
            // inline eval_cubic_second_derivative
            float Bx = (x2 - (x1 + x1) + x0);
            float By = (y2 - (y1 + y1) + y0);

            curvature[curvatureOff]   = Ax * t + Bx;
            curvature[curvatureOff+1] = Ay * t + By;
        }
    }

    public static void chopCubicAt(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3,
            final float t,
            final float[] dst, final int off
    ) {
        assert t >= 0 && t <= 1;

        if (t == 1) {
            dst[off]    = x0;
            dst[off+1]  = y0;
            dst[off+2]  = x1;
            dst[off+3]  = y1;
            dst[off+4]  = x2;
            dst[off+5]  = y2;
            dst[off+6]  = x3;
            dst[off+7]  = y3;

            dst[off+8]  = x3;
            dst[off+9]  = y3;
            dst[off+10] = x3;
            dst[off+11] = y3;
            dst[off+12] = x3;
            dst[off+13] = y3;
            return;
        }

        float abx   = MathUtil.lerpStable(x0,   x1,   t);
        float aby   = MathUtil.lerpStable(y0,   y1,   t);
        float bcx   = MathUtil.lerpStable(x1,   x2,   t);
        float bcy   = MathUtil.lerpStable(y1,   y2,   t);
        float cdx   = MathUtil.lerpStable(x2,   x3,   t);
        float cdy   = MathUtil.lerpStable(y2,   y3,   t);
        float abcx  = MathUtil.lerpStable(abx,  bcx,  t);
        float abcy  = MathUtil.lerpStable(aby,  bcy,  t);
        float bcdx  = MathUtil.lerpStable(bcx,  cdx,  t);
        float bcdy  = MathUtil.lerpStable(bcy,  cdy,  t);
        float abcdx = MathUtil.lerpStable(abcx, bcdx, t);
        float abcdy = MathUtil.lerpStable(abcy, bcdy, t);

        dst[off]    = x0;
        dst[off+1]  = y0;
        dst[off+2]  = abx;
        dst[off+3]  = aby;
        dst[off+4]  = abcx;
        dst[off+5]  = abcy;
        dst[off+6]  = abcdx;
        dst[off+7]  = abcdy;
        dst[off+8]  = bcdx;
        dst[off+9]  = bcdy;
        dst[off+10] = cdx;
        dst[off+11] = cdy;
        dst[off+12] = x3;
        dst[off+13] = y3;
    }

    // radius 4220.2324 to get 1 pixel error
    public static final int MAX_CONIC_TO_QUADS_LEVEL = 5;

    /**
     * Return the log2 number of quadratic Bézier curves needed to approximate the conic
     * with a sequence of quadratic Bézier curves. Will be 0 to 5.
     */
    public static int computeConicToQuadsLevel(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float w1,
            final float tol
    ) {
        // "High order approximation of conic sections by quadratic splines"
        // by Michael Floater, 1993
        float a = w1 - 1;
        float k = a / (4 * (2 + a));
        float x = k * (x0 - 2 * x1 + x2);
        float y = k * (y0 - 2 * y1 + y2);

        int level = MathUtil.ceilLog16((x * x + y * y) / (tol * tol));

        return Math.min(level, MAX_CONIC_TO_QUADS_LEVEL);
    }

    /**
     * Chop the conic into N quads, stored continuously in <var>dst</var>, where
     * N = 1 << level. The amount of storage needed is (4 * N + 2)
     *
     * @param w1    conic weight
     * @param level 0 to 5 (1 to 32 quad curves)
     * @return actual number of quad curves
     */
    public static int computeConicToQuads(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float w1,
            final float[] dst, final int off,
            int level
    ) {
        if (level < 0 || level > MAX_CONIC_TO_QUADS_LEVEL) {
            throw new IllegalArgumentException();
        }
        dst[off] = x0;
        dst[off+1] = y0;
        int count = subdivideConic(
                x0, y0,
                x1, y1,
                x2, y2,
                w1,
                dst, off + 2,
                level
        );
        if (count == ~8) {
            // special case
            level = 1;
            count = 8;
        } else {
            assert 4 * (1 << level) == count;
        }
        float prod = 0;
        for (int i = off; i < off + count + 2; i++) {
            prod *= dst[i];
        }
        if (prod != 0) {
            // if we generated a non-finite, pin ourselves to the middle of the hull,
            // as our first and last are already on the first/last pts of the hull.
            for (int i = off + 2; i < off + count; i += 2) {
                dst[i] = x1;
                dst[i+1] = y1;
            }
        }
        return 1 << level;
    }

    // returns true if (a <= b <= c) || (a >= b >= c)
    static boolean between(float a, float b, float c) {
        return (a - b) * (c - b) <= 0;
    }

    static int subdivideConic(
            final float p0x, final float p0y,
            final float p1x, final float p1y,
            final float p2x, final float p2y,
            float w1,
            final float[] dst, final int off,
            int level
    ) {
        assert level >= 0;
        if (level == 0) {
            dst[off] = p1x;
            dst[off+1] = p1y;
            dst[off+2] = p2x;
            dst[off+3] = p2y;
            return 4;
        }

        // observe that scale will always be smaller than 1 because w1 > 0.
        final float scale = 1 / (1 + w1);

        // The subdivided control points below are the sums of the following three terms. Because the
        // terms are multiplied by something <1, and the resulting control points lie within the
        // control points of the original then the terms and the sums below will not overflow. Note
        // that w1 * scale approaches 1 as w1 becomes very large.
        final float t0x = p0x * scale;
        final float t0y = p0y * scale;
        final float t1x = p1x * (w1 * scale);
        final float t1y = p1y * (w1 * scale);
        final float t2x = p2x * scale;
        final float t2y = p2y * scale;

        // Calculate the subdivided control points
        float q1x = t0x + t1x;
        float q1y = t0y + t1y;
        // p2 = (t0 + 2*t1 + t2) / 2. Divide the terms by 2 before the sum to keep the sum for p2
        // from overflowing.
        float q2x = 0.5f * t0x + t1x + 0.5f * t2x;
        float q2y = 0.5f * t0y + t1y + 0.5f * t2y;
        float q3x = t1x + t2x;
        float q3y = t1y + t2y;

        if (level == MAX_CONIC_TO_QUADS_LEVEL) {
            // If an extreme weight generates many quads
            // check to see if the first chop generates a pair of lines
            if (Point.equals(q1x, q1y, q2x, q2y) &&
                    Point.equals(q2x, q2y, q3x, q3y)) {
                // make lines
                dst[off] =   q1x;
                dst[off+1] = q1y;
                dst[off+2] = q1x;
                dst[off+3] = q1y;
                dst[off+4] = q1x;
                dst[off+5] = q1y;

                dst[off+6] = p2x;
                dst[off+7] = p2y;
                return ~8;
            }
        }

        // Update w.
        w1 = MathUtil.sqrt(0.5f + w1 * 0.5f);

        if (between(p0y, p1y, p2y)) {
            // If the input is monotonic and the output is not, the scan converter hangs.
            // Ensure that the chopped conics maintain their y-order.
            float midY = q2y;
            if (!between(p0y, midY, p2y)) {
                // If the computed midpoint is outside the ends, move it to the closer one.
                q2y = Math.abs(midY - p0y) < Math.abs(midY - p2y) ? p0y : p2y;
            }
            if (!between(p0y, q1y, q2y)) {
                // If the 1st control is not between the start and end, put it at the start.
                // This also reduces the quad to a line.
                q1y = p0y;
            }
            if (!between(q2y, q3y, p2y)) {
                // If the 2nd control is not between the start and end, put it at the end.
                // This also reduces the quad to a line.
                q3y = p2y;
            }
            // Verify that all five points are in order.
            assert (between(p0y, q1y, q2y));
            assert (between(q1y, q2y, q3y));
            assert (between(q2y, q3y, p2y));
        }

        --level;
        int ret = off;
        ret += subdivideConic(
                p0x, p0y,
                q1x, q1y,
                q2x, q2y,
                w1,
                dst, ret,
                level
        );
        ret += subdivideConic(
                q2x, q2y,
                q3x, q3y,
                p2x, p2y,
                w1,
                dst, ret,
                level
        );
        return ret - off;
    }

    protected Geometry() {
        throw new UnsupportedOperationException();
    }
}
