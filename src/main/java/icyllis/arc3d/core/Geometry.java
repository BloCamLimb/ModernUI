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

    // returns true if (a <= b <= c) || (a >= b >= c)
    static boolean between(float a, float b, float c) {
        return (a - b) * (c - b) <= 0;
    }

    /**
     * Given a Conic representing a circular arc that does not exceed 90 degrees.
     * Subdivide it into two quadratic Bézier curves. The weight must be sqrt(2)/2.
     */
    public static void subdivideQuadrantConicToQuads(
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float[] dst, final int off
    ) {
        float scale = 1 / (1 + MathUtil.INV_SQRT2);
        float t0x = x0 * scale;
        float t0y = y0 * scale;
        float t1x = x1 * (MathUtil.INV_SQRT2 * scale);
        float t1y = y1 * (MathUtil.INV_SQRT2 * scale);
        float t2x = x2 * scale;
        float t2y = y2 * scale;

        float p1x = t0x + t1x;
        float p1y = t0y + t1y;
        float p3x = t1x + t2x;
        float p3y = t1y + t2y;
        float p2x = 0.5f * t0x + t1x + 0.5f * t2x;
        float p2y = 0.5f * t0y + t1y + 0.5f * t2y;

        dst[off] = x0;
        dst[off+1] = y0;
        dst[off+2] = p1x;
        dst[off+3] = p1y;
        dst[off+4] = p2x;
        dst[off+5] = p2y;
        dst[off+6] = p3x;
        dst[off+7] = p3y;
        dst[off+8] = x2;
        dst[off+9] = y2;
    }

    protected Geometry() {
        throw new UnsupportedOperationException();
    }
}
