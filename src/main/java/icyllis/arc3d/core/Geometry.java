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
            final float X0, final float Y0, final float X1, final float Y1,
            final float X2, final float Y2, final float X3, final float Y3,
            final float[] roots, final int off) {

        // find the parameter value `t` where curvature is zero
        // P(t) = (1-t)^3 * b0 + 3*t * (1-t)^2 * b1 + 3*t^2 * (1-t) * b2 + t^3 * b3
        // let curvature(t) = |P'(t) cross P''(t)| / |P'(t)^3| = 0
        float Ax = X1 - X0;
        float Ay = Y1 - Y0;
        float Bx = X2 - 2 * X1 + X0;
        float By = Y2 - 2 * Y1 + Y0;
        float Cx = X3 + 3 * (X1 - X2) - X0;
        float Cy = Y3 + 3 * (Y1 - Y2) - Y0;

        return findUnitQuadRoots(
                Bx * Cy - By * Cx,
                Ax * Cy - Ay * Cx,
                Ax * By - Ay * Bx,
                roots, off);
    }

    static void eval_cubic_derivative(
            final float X0, final float Y0, final float X1, final float Y1,
            final float X2, final float Y2, final float X3, final float Y3,
            final float t,
            final float[] dst, final int off
    ) {

        float Ax = X3 + 3 * (X1 - X2) - X0;
        float Ay = Y3 + 3 * (Y1 - Y2) - Y0;
        float Bx = 2 * (X2 - (X1 + X1) + X0);
        float By = 2 * (Y2 - (Y1 + Y1) + Y0);
        float Cx = (X1 - X0);
        float Cy = (Y1 - Y0);

        dst[off]   = (Ax * t + Bx) * t + Cx;
        dst[off+1] = (Ay * t + By) * t + Cy;
    }

    public static void evalCubicAt(
            final float X0, final float Y0, final float X1, final float Y1,
            final float X2, final float Y2, final float X3, final float Y3,
            final float t,
            @Nullable final float[] loc, final int locOff,
            @Nullable final float[] tangent, final int tangentOff,
            @Nullable final float[] curvature, final int curvatureOff
    ) {
        assert t >= 0 && t <= 1;

        float Ax = X3 + 3 * (X1 - X2) - X0;
        float Ay = Y3 + 3 * (Y1 - Y2) - Y0;

        if (loc != null) {
            float Bx = 3 * (X2 - (X1 + X1) + X0);
            float By = 3 * (Y2 - (Y1 + Y1) + Y0);
            float Cx = 3 * (X1 - X0);
            float Cy = 3 * (Y1 - Y0);

            loc[locOff]   = ((Ax * t + Bx) * t + Cx) * t + X0;
            loc[locOff+1] = ((Ay * t + By) * t + Cy) * t + Y0;
        }

        if (tangent != null) {
            // The derivative equation returns a zero tangent vector when t is 0 or 1, and the
            // adjacent control point is equal to the end point. In this case, use the
            // next control point or the end points to compute the tangent.
            if ((t == 0 && X0 == X1 && Y0 == Y1) ||
                    (t == 1 && X2 == X3 && Y2 == Y3)) {
                float Tx;
                float Ty;
                if (t == 0) {
                    Tx = X2 - X0;
                    Ty = Y2 - Y0;
                } else {
                    Tx = X3 - X1;
                    Ty = Y3 - Y1;
                }
                if (Tx == 0 && Ty == 0) {
                    tangent[tangentOff]   = X3 - X0;
                    tangent[tangentOff+1] = Y3 - Y0;
                } else {
                    tangent[tangentOff]   = Tx;
                    tangent[tangentOff+1] = Ty;
                }
            } else {
                eval_cubic_derivative(
                        X0, Y0,
                        X1, Y1,
                        X2, Y2,
                        X3, Y3,
                        t,
                        tangent, tangentOff
                );
            }
        }

        if (curvature != null) {
            float Bx = (X2 - (X1 + X1) + X0);
            float By = (Y2 - (Y1 + Y1) + Y0);

            curvature[curvatureOff]   = Ax * t + Bx;
            curvature[curvatureOff+1] = Ay * t + By;
        }
    }

    public static void chopCubicAt(
            final float X0, final float Y0, final float X1, final float Y1,
            final float X2, final float Y2, final float X3, final float Y3,
            final float t,
            final float[] dst, final int off
    ) {
        assert t >= 0 && t <= 1;

        if (t == 1) {
            dst[off]    = X0;
            dst[off+1]  = Y0;
            dst[off+2]  = X1;
            dst[off+3]  = Y1;
            dst[off+4]  = X2;
            dst[off+5]  = Y2;
            dst[off+6]  = X3;
            dst[off+7]  = Y3;
            dst[off+8]  = X3;
            dst[off+9]  = Y3;
            dst[off+10] = X3;
            dst[off+11] = Y3;
            dst[off+12] = X3;
            dst[off+13] = Y3;
            return;
        }

        float abx = MathUtil.lerpStable(X0, X1, t);
        float aby = MathUtil.lerpStable(Y0, Y1, t);
        float bcx = MathUtil.lerpStable(X1, X2, t);
        float bcy = MathUtil.lerpStable(Y1, Y2, t);
        float cdx = MathUtil.lerpStable(X2, X3, t);
        float cdy = MathUtil.lerpStable(Y2, Y3, t);
        float abcx = MathUtil.lerpStable(abx, bcx, t);
        float abcy = MathUtil.lerpStable(aby, bcy, t);
        float bcdx = MathUtil.lerpStable(bcx, cdx, t);
        float bcdy = MathUtil.lerpStable(bcy, cdy, t);
        float abcdx = MathUtil.lerpStable(abcx, bcdx, t);
        float abcdy = MathUtil.lerpStable(abcy, bcdy, t);

        dst[off]    = X0;
        dst[off+1]  = Y0;
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
        dst[off+12] = X3;
        dst[off+13] = Y3;
    }

    protected Geometry() {
        throw new UnsupportedOperationException();
    }
}
