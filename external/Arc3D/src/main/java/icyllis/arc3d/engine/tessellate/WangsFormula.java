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

package icyllis.arc3d.engine.tessellate;

import icyllis.arc3d.core.MathUtil;

/**
 * Wang's formula specifies a depth D to which it is sufficient to subdivide a given
 * degree N Bézier curve in order to ensure that the control points of each new segment
 * lie on a straight line to within some pre-specified tolerance EPS:
 * <pre>
 *  M = max([length( p[i+2] - 2p[i+1] + p[i] ) for (0 &le; i &le; n-2)])
 *  D = log4( N*(N-1)*M / (8*EPS) )
 * </pre>
 * Wang, Guo-Zhao (1984), The subdivision method for finding the intersection between
 * two Bézier curves or surfaces, Zhejiang University Journal.
 */
public class WangsFormula {

    // this is the second power of the factor without M and EPS
    // Degree = 2, (N * (N - 1) / 8)^2
    private static final float N2_P2_F = 0.0625f;
    // Degree = 3, (N * (N - 1) / 8)^2
    private static final float N3_P2_F = 0.5625f;

    // Returns Wang's formula, raised to the 4th power, specialized for a quadratic curve.
    public static float quadratic_p4(
            final float precision,
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2
    ) {
        // precision = 1/tolerance
        final float Mx = x2 - 2 * x1 + x0; // [p0, p1, p2]
        final float My = y2 - 2 * y1 + y0;
        return (Mx * Mx + My * My) *
                (N2_P2_F * (precision * precision));
    }

    // Returns Wang's formula specialized for a quadratic curve.
    public static float quadratic(
            final float precision,
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2
    ) {
        return (float) Math.sqrt(Math.sqrt(
                quadratic_p4(
                        precision,
                        x0, y0,
                        x1, y1,
                        x2, y2
                )
        ));
    }

    // Returns the log2 value of Wang's formula specialized for a quadratic curve, rounded up to the
    // next int.
    public static int quadratic_log2(
            final float precision,
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2
    ) {
        // ceilLog16(x) == ceil(log2(sqrt(sqrt(x))))
        // ceilLog16(x) == ceil(log4(sqrt(x)))
        return MathUtil.ceilLog16(
                quadratic_p4(
                        precision,
                        x0, y0,
                        x1, y1,
                        x2, y2
                )
        );
    }

    // Returns Wang's formula, raised to the 4th power, specialized for a cubic curve.
    public static float cubic_p4(
            final float precision,
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3
    ) {
        // precision = 1/tolerance
        final float Mx0 = x2 - 2 * x1 + x0; // [p0, p1, p2]
        final float My0 = y2 - 2 * y1 + y0;
        final float Mx1 = x3 - 2 * x2 + x1; // [p1, p2, p3]
        final float My1 = y3 - 2 * y2 + y1;
        return Math.max(
                Mx0 * Mx0 + My0 * My0,
                Mx1 * Mx1 + My1 * My1
        ) *
                (N3_P2_F * (precision * precision));
    }

    // Returns Wang's formula specialized for a cubic curve.
    public static float cubic(
            final float precision,
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3
    ) {
        return (float) Math.sqrt(Math.sqrt(
                cubic_p4(
                        precision,
                        x0, y0,
                        x1, y1,
                        x2, y2,
                        x3, y3
                )
        ));
    }

    // Returns the log2 value of Wang's formula specialized for a cubic curve, rounded up to the next
    // int.
    public static int cubic_log2(
            final float precision,
            final float x0, final float y0,
            final float x1, final float y1,
            final float x2, final float y2,
            final float x3, final float y3
    ) {
        // ceilLog16(x) == ceil(log2(sqrt(sqrt(x))))
        // ceilLog16(x) == ceil(log4(sqrt(x)))
        return MathUtil.ceilLog16(
                cubic_p4(
                        precision,
                        x0, y0,
                        x1, y1,
                        x2, y2,
                        x3, y3
                )
        );
    }

    // Returns the maximum number of line segments a cubic with the given device-space bounding box size
    // would ever need to be divided into, raised to the 4th power. This is simply a special case of the
    // cubic formula where we maximize its value by placing control points on specific corners of the
    // bounding box.
    public static float worst_cubic_p4(
            final float precision,
            final float devWidth, float devHeight
    ) {
        return 4 *
                (N3_P2_F * (precision * precision)) *
                (devWidth * devWidth + devHeight * devHeight);
    }

    // Returns the maximum number of line segments a cubic with the given device-space bounding box size
    // would ever need to be divided into.
    public static float worst_cubic(
            final float precision,
            final float devWidth, float devHeight
    ) {
        return (float) Math.sqrt(Math.sqrt(
                worst_cubic_p4(
                        precision,
                        devWidth, devHeight
                )
        ));
    }

    // Returns the maximum log2 number of line segments a cubic with the given device-space bounding box
    // size would ever need to be divided into.
    public static int worst_cubic_log2(
            final float precision,
            final float devWidth, float devHeight
    ) {
        // ceilLog16(x) == ceil(log2(sqrt(sqrt(x))))
        // ceilLog16(x) == ceil(log4(sqrt(x)))
        return MathUtil.ceilLog16(
                worst_cubic_p4(
                        precision,
                        devWidth, devHeight
                )
        );
    }

    protected WangsFormula() {
        throw new UnsupportedOperationException();
    }
}
