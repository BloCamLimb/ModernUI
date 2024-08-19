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

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * PathStroker is a {@link PathConsumer} that converts paths by stroking paths.
 * This is invoked when a {@link Path} is drawn in a canvas with the
 * {@link Paint#STROKE} bit set in the paint. The new path consists of
 * closed contours, and the style change from thick stroke to fill.
 *
 * @author BloCamLimb
 */
//TODO review tangent, implement inside and outside stroke for closed paths
public class PathStroker implements PathConsumer {

    private static final boolean DEBUG = false;

    private PathConsumer mOuter;
    private final Path mInner = new Path();

    // stroke radius, half the stroke width
    private float mRadius;
    private float mInvMiterLimit;
    private float mResScale;
    private float mInvResScale;
    private float mInvResScaleSquared;

    private int mCapStyle;
    private int mJoinStyle;

    private Capper mCapper;
    private Joiner mJoiner;

    private float mFirstX;
    private float mFirstY;
    private float mPrevX;
    private float mPrevY;

    private float mFirstNormalX;
    private float mFirstNormalY;
    private float mPrevNormalX;
    private float mPrevNormalY;

    private float mFirstUnitNormalX;
    private float mFirstUnitNormalY;
    private float mPrevUnitNormalX;
    private float mPrevUnitNormalY;

    private float mFirstOuterX;
    private float mFirstOuterY;

    // previous join was not degenerate
    private boolean mJoinCompleted;

    private int mSegmentCount;

    private boolean mPrevIsLine;

    public void init(@Nonnull PathConsumer out,
                     float radius,
                     @Paint.Cap int cap,
                     @Paint.Join int join,
                     float miterLimit,
                     float resScale) {
        assert out != this;
        mOuter = out;
        mRadius = radius;

        if (join == Paint.JOIN_MITER) {
            if (miterLimit <= 1) {
                join = Paint.JOIN_BEVEL;
            } else {
                mInvMiterLimit = 1 / miterLimit;
            }
        }

        mCapStyle = cap;
        mJoinStyle = join;
        mCapper = Capper.get(cap);
        mJoiner = Joiner.get(join);

        mSegmentCount = -1;
        mPrevIsLine = false;

        mResScale = resScale;
        mInvResScale = 1 / (resScale * 4);
        mInvResScaleSquared = mInvResScale * mInvResScale;
    }

    private void log(int result, QuadState pp, @PrintFormat String format, Object... args) {
        System.out.printf("[%d] ", mRecursionDepth);
        System.out.printf(format, args);
        String resultStr = switch (result) {
            case INTERSECT_SUBDIVIDE -> "Subdivide";
            case INTERSECT_DEGENERATE -> "Degenerate";
            case INTERSECT_QUADRATIC -> "Quadratic";
            default -> throw new AssertionError();
        };
        System.out.printf("\n  %s t=(%g,%g)\n", resultStr, pp.t_from, pp.t_to);
    }

    @Override
    public void moveTo(float x, float y) {
        if (mSegmentCount > 0) {
            finish(false, false);
        }
        mSegmentCount = 0;
        mFirstX = mPrevX = x;
        mFirstY = mPrevY = y;
        mJoinCompleted = false;
    }

    private static final int NORMAL_X = 0;
    private static final int NORMAL_Y = 1;
    private static final int UNIT_NORMAL_X = 2;
    private static final int UNIT_NORMAL_Y = 3;
    private final float[] mNormal = new float[4];

    private boolean preJoinTo(float x, float y, boolean isLine) {
        assert mSegmentCount >= 0;

        mNormal[0] = (x - mPrevX) * mResScale;
        mNormal[1] = (y - mPrevY) * mResScale;
        if (!Point.normalize(mNormal, 0)) {
            if (mCapStyle == Paint.CAP_BUTT) {
                return false;
            }
            /* Square caps and round caps draw even if the segment length is zero.
               Since the zero length segment has no direction, set the orientation
               to upright as the default orientation */
            mNormal[NORMAL_X] = mRadius;
            mNormal[NORMAL_Y] = 0;
            mNormal[UNIT_NORMAL_X] = 1;
            mNormal[UNIT_NORMAL_Y] = 0;
        } else {
            // Rotate CCW
            float newX = mNormal[1];
            float newY = -mNormal[0];
            mNormal[NORMAL_X] = newX * mRadius;
            mNormal[NORMAL_Y] = newY * mRadius;
            mNormal[UNIT_NORMAL_X] = newX;
            mNormal[UNIT_NORMAL_Y] = newY;
        }

        if (mSegmentCount == 0) {
            mFirstNormalX = mNormal[NORMAL_X];
            mFirstNormalY = mNormal[NORMAL_Y];
            mFirstUnitNormalX = mNormal[UNIT_NORMAL_X];
            mFirstUnitNormalY = mNormal[UNIT_NORMAL_Y];
            mFirstOuterX = mPrevX + mFirstNormalX;
            mFirstOuterY = mPrevY + mFirstNormalY;

            mOuter.moveTo(mFirstOuterX, mFirstOuterY);
            mInner.moveTo(mPrevX - mFirstNormalX, mPrevY - mFirstNormalY);
        } else {
            mJoiner.join(
                    mOuter, mInner,
                    mPrevUnitNormalX, mPrevUnitNormalY,
                    mPrevX, mPrevY,
                    mNormal[UNIT_NORMAL_X], mNormal[UNIT_NORMAL_Y],
                    mRadius,
                    mInvMiterLimit,
                    mPrevIsLine,
                    isLine
            );
        }
        mPrevIsLine = isLine;
        return true;
    }

    private void postJoinTo(float x, float y) {
        mJoinCompleted = true;
        mPrevX = x;
        mPrevY = y;
        mPrevNormalX = mNormal[NORMAL_X];
        mPrevNormalY = mNormal[NORMAL_Y];
        mPrevUnitNormalX = mNormal[UNIT_NORMAL_X];
        mPrevUnitNormalY = mNormal[UNIT_NORMAL_Y];
        mSegmentCount++;
    }

    @Override
    public void lineTo(float x, float y) {
        boolean degenerate = Point.isApproxEqual(
                mPrevX, mPrevY,
                x, y,
                MathUtil.EPS * mInvResScale
        );
        if (degenerate && mCapStyle == Paint.CAP_BUTT) {
            return;
        }
        if (degenerate && mJoinCompleted) {
            return;
        }
        if (preJoinTo(x, y, true)) {
            mOuter.lineTo(x + mNormal[NORMAL_X], y + mNormal[NORMAL_Y]);
            mInner.lineTo(x - mNormal[NORMAL_X], y - mNormal[NORMAL_Y]);
            postJoinTo(x, y);
        }
    }

    // the state of the quad stroke
    static class QuadState {

        // the stroked quad parallel to the original curve
        float q0x; // Quad[0], start point
        float q0y;
        float q1x; // Quad[1], control point
        float q1y;
        float q2x; // Quad[2], end point
        float q2y;

        float tan0x; // a point tangent to Quad[0]
        float tan0y;
        float tan2x; // a point tangent to Quad[2]
        float tan2y;

        // a segment of the original curve
        float t_from;
        float t_mid;
        float t_to;

        // state to share common points across depths
        boolean set0;
        boolean set2;

        // set if coincident tangents have opposite directions
        boolean opposite_tangents;

        // return false if from and to are too close to have a unique middle
        boolean init(float from, float to) {
            t_from = from;
            t_mid = (from + to) * 0.5f;
            t_to = to;
            set0 = set2 = false;
            return from < t_mid && t_mid < to;
        }

        boolean init0(QuadState pp) {
            if (!init(pp.t_from, pp.t_mid)) {
                return false;
            }
            q0x = pp.q0x;
            q0y = pp.q0y;
            tan0x = pp.tan0x;
            tan0y = pp.tan0y;
            set0 = true;
            return true;
        }

        boolean init2(QuadState pp) {
            if (!init(pp.t_mid, pp.t_to)) {
                return false;
            }
            q2x = pp.q2x;
            q2y = pp.q2y;
            tan2x = pp.tan2x;
            tan2y = pp.tan2y;
            set2 = true;
            return true;
        }
    }

    static final int MAX_TANGENT_RECURSION_DEPTH = 15;
    static final int MAX_CUBIC_RECURSION_DEPTH = 24;
    static final int MAX_QUAD_RECURSION_DEPTH = 33;

    // stack depth
    private int mRecursionDepth;
    private boolean mFoundTangents;

    // simulate the stack
    private final QuadState[] mQuadStack =
            new QuadState[MAX_QUAD_RECURSION_DEPTH + 1];

    {
        for (int i = 0; i < mQuadStack.length; i++) {
            mQuadStack[i] = new QuadState();
        }
    }

    // use sign-opposite values later to flip perpendicular axis
    private static final int STROKE_TYPE_OUTER = 1;
    private static final int STROKE_TYPE_INNER = -1;

    private int mStrokeType;

    private void initStroke(int type, QuadState pp, float tFrom, float tTo) {
        assert type == STROKE_TYPE_OUTER ||
                type == STROKE_TYPE_INNER;
        mStrokeType = type;
        mFoundTangents = false;
        mRecursionDepth = 0;
        pp.init(tFrom, tTo);
    }

    // the first 4 points (8 floats) for curve points
    // the next 12 points (24 floats) for method results
    private final float[] mCurve = new float[8 + 24];

    // load quadratic curve
    private float[] getQuad(
            float x1, float y1,
            float x2, float y2
    ) {
        var c = mCurve;
        c[0]=mPrevX;
        c[1]=mPrevY;
        c[2]=x1;
        c[3]=y1;
        c[4]=x2;
        c[5]=y2;
        return c;
    }

    // load cubic curve
    private float[] getCubic(
            float x1, float y1,
            float x2, float y2,
            float x3, float y3
    ) {
        var c = mCurve;
        c[0]=mPrevX;
        c[1]=mPrevY;
        c[2]=x1;
        c[3]=y1;
        c[4]=x2;
        c[5]=y2;
        c[6]=x3;
        c[7]=y3;
        return c;
    }

    /* Given quad, see if all there points are in a line.
       Return true if the inside point is close to a line connecting the outermost points.

       Find the outermost point by looking for the largest difference in X or Y.
       Since the XOR of the indices is 3  (0 ^ 1 ^ 2)
       the missing index equals: outer_1 ^ outer_2 ^ 3
     */
    private static boolean quad_in_line(float[] quad) {
        float pMax = -1;
        int outer1 = 0;
        int outer2 = 0;
        for (int index = 0; index < 2; ++index) {
            for (int inner = index + 1; inner < 3; ++inner) {
                float testMax = Math.max(
                        Math.abs(quad[inner << 1] - quad[index << 1]),
                        Math.abs(quad[inner << 1 | 1] - quad[index << 1 | 1])
                );
                if (pMax < testMax) {
                    outer1 = index;
                    outer2 = inner;
                    pMax = testMax;
                }
            }
        }
        assert outer1 >= 0;
        assert outer2 >= 1;
        assert outer1 < outer2;
        int mid = outer1 ^ outer2 ^ 3;
        float lineSlop =  pMax * pMax *
                0.000005f; // this multiplier is pulled out of the air
        return Point.distanceToLineSegmentBetweenSq(
                quad[mid << 1],   quad[mid << 1 | 1],
                quad[outer1 << 1],quad[outer1 << 1 | 1],
                quad[outer2 << 1],quad[outer2 << 1 | 1]
        ) <= lineSlop;
    }

    /*  Given a cubic, determine if all four points are in a line.
        Return true if the inner points is close to a line connecting the outermost points.

        Find the outermost point by looking for the largest difference in X or Y.
        Given the indices of the outermost points, and that outer_1 is greater than outer_2,
        this table shows the index of the smaller of the remaining points:

                          outer_2
                      0    1    2    3
          outer_1     ----------------
             0     |  -    2    1    1
             1     |  -    -    0    0
             2     |  -    -    -    0
             3     |  -    -    -    -

        If outer_1 == 0 and outer_2 == 1, the smaller of the remaining indices (2 and 3) is 2.

        This table can be collapsed to: (1 + (2 >> outer_2)) >> outer_1

        Given three indices (outer_1 outer_2 mid_1) from 0..3, the remaining index is:

                   mid_2 == (outer_1 ^ outer_2 ^ mid_1)
     */
    private static boolean cubic_in_line(float[] cubic) {
        float pMax = -1;
        int outer1 = 0;
        int outer2 = 0;
        for (int index = 0; index < 3; ++index) {
            for (int inner = index + 1; inner < 4; ++inner) {
                float testMax = Math.max(
                        Math.abs(cubic[inner << 1] - cubic[index << 1]),
                        Math.abs(cubic[inner << 1 | 1] - cubic[index << 1 | 1])
                );
                if (pMax < testMax) {
                    outer1 = index;
                    outer2 = inner;
                    pMax = testMax;
                }
            }
        }
        assert outer1 >= 0;
        assert outer2 >= 1;
        assert outer1 < outer2;
        int mid1 = (1 + (2 >> outer2)) >> outer1;
        assert (outer1 != mid1 && outer2 != mid1);
        int mid2 = outer1 ^ outer2 ^ mid1;
        assert mid2 >= 1;
        assert (mid2 != outer1 && mid2 != outer2 && mid2 != mid1);
        assert (((1 << outer1) | (1 << outer2) | (1 << mid1) | (1 << mid2)) == 0x0f);
        float lineSlop = pMax * pMax *
                0.00001f;  // this multiplier is pulled out of the air
        return Point.distanceToLineSegmentBetweenSq(
                cubic[mid1 << 1], cubic[mid1 << 1 | 1],
                cubic[outer1 << 1], cubic[outer1 << 1 | 1],
                cubic[outer2 << 1], cubic[outer2 << 1 | 1]
        ) <= lineSlop && Point.distanceToLineSegmentBetweenSq(
                cubic[mid2 << 1], cubic[mid2 << 1 | 1],
                cubic[outer1 << 1], cubic[outer1 << 1 | 1],
                cubic[outer2 << 1], cubic[outer2 << 1 | 1]
        ) <= lineSlop;
    }

    private static final int INTERSECT_SUBDIVIDE = 0;
    private static final int INTERSECT_DEGENERATE = 1;
    private static final int INTERSECT_QUADRATIC = 2;

    // Find the intersection of the stroke tangents to construct a stroke quad.
    // Return whether the stroke is a degenerate (a line), a quad, or must be split.
    // Optionally compute the quad's control point.
    private int intersect_ray(
            QuadState pp, boolean computeControlPoint
    ) {
        float startX = pp.q0x;
        float startY = pp.q0y;
        float endX = pp.q2x;
        float endY = pp.q2y;

        float aLenX = pp.tan0x - startX;
        float aLenY = pp.tan0y - startY;
        float bLenX = pp.tan2x - endX;
        float bLenY = pp.tan2y - endY;

        float denom = Point.crossProduct(
                aLenX, aLenY,
                bLenX, bLenY
        );
        if (denom == 0 || !Float.isFinite(denom)) {
            pp.opposite_tangents = Point.dotProduct(
                    aLenX, aLenY,
                    bLenX, bLenY
            ) < 0;
            if (DEBUG) {
                log(INTERSECT_DEGENERATE, pp, "denom == 0");
            }
            return INTERSECT_DEGENERATE;
        }
        pp.opposite_tangents = false;

        float ab0x = startX - endX;
        float ab0y = startY - endY;

        float numerA = Point.crossProduct(
                bLenX, bLenY,
                ab0x, ab0y
        );
        float numerB = Point.crossProduct(
                aLenX, aLenY,
                ab0x, ab0y
        );

        if ((numerA >= 0) == (numerB >= 0)) {
            // if the control point is outside the quad ends
            // if the perpendicular distances from the quad points to the opposite tangent line
            // are small, a straight line is good enough
            float dist1 = Point.distanceToLineSegmentBetweenSq(
                    startX, startY,
                    endX, endY,
                    pp.tan2x, pp.tan2y
            );
            float dist2 = Point.distanceToLineSegmentBetweenSq(
                    endX, endY,
                    startX, startY,
                    pp.tan0x, pp.tan0y
            );
            if (Math.max(dist1, dist2) <= mInvResScaleSquared) {
                if (DEBUG) {
                    log(INTERSECT_DEGENERATE, pp,
                            "max(dist1=%g, dist2=%g) <= mInvResScaleSquared", dist1, dist2);
                }
                return INTERSECT_DEGENERATE;
            }
            if (DEBUG) {
                log(INTERSECT_SUBDIVIDE, pp,
                        "(numerA=%g >= 0) == (numerB=%g >= 0)", numerA, numerB);
            }
            return INTERSECT_SUBDIVIDE;
        }

        // check to see if the denominator is teeny relative to the numerator
        // if the offset by one will be lost, the ratio is too large
        numerA /= denom;

        boolean validDivide = numerA > numerA - 1;
        if (validDivide) {
            if (computeControlPoint) {
                // the intersection of the tangents need not be on the tangent segment
                // so 0 <= numerA <= 1 is not necessarily true
                pp.q1x = startX * (1 - numerA) + pp.tan0x * numerA;
                pp.q1y = startY * (1 - numerA) + pp.tan0y * numerA;
            }
            if (DEBUG) {
                log(INTERSECT_QUADRATIC, pp,
                        "(numerA=%g >= 0) != (numerB=%g >= 0)", numerA, numerB);
            }
            return INTERSECT_QUADRATIC;
        }

        pp.opposite_tangents = Point.dotProduct(
                aLenX, aLenY,
                bLenX, bLenY
        ) < 0;
        // if the lines are parallel, straight line is good enough
        if (DEBUG) {
            log(INTERSECT_DEGENERATE, pp,
                    "ApproxZero(denom=%g)", denom);
        }
        return INTERSECT_DEGENERATE;
    }

    // Given a point on the curve and its derivative, scale the derivative by the radius, and
    // compute the perpendicular point and its tangent.
    // 0-7 CURVE POINTS
    // 8,9 POINT
    // 10,11 TANGENT
    // 12,13 RAY POINT
    // 14,15 RAY TANGENT
    private void set_perpendicular_ray(float[] v) {
        // normalize tangent
        if (!Point.setLength(v, 8+2, mRadius)) {
            v[8+2] = mRadius;
            v[8+3] = 0;
        }
        // go opposite ways for outer, inner
        float axis = mStrokeType;
        // ray point
        v[8+4] = v[8]   + axis * v[8+3];
        v[8+5] = v[8+1] - axis * v[8+2];
        // ray tangent = ray point + tangent
        v[8+6] = v[8+4] + v[8+2];
        v[8+7] = v[8+5] + v[8+3];
    }

    // Given a quad and t, return the point on curve, its perpendicular, and the perpendicular tangent.
    // 0-7 CURVE POINTS
    // 8,9 POINT at T
    // 10,11 TANGENT at T
    // 12,13 RAY POINT
    // 14,15 RAY TANGENT
    private void quad_perpendicular_ray(float[] quad, float t) {
        GeometryUtils.evalQuadAt(
                quad, 0,
                t, quad, /*pos*/ 8, quad, /*tangent*/ 8+2
        );
        if (quad[8+2] == 0 && quad[8+3] == 0) {
            quad[8+2] = quad[4] - quad[0];
            quad[8+3] = quad[5] - quad[1];
        }
        set_perpendicular_ray(quad);
    }

    // Given a cubic and t, return the point on curve, its perpendicular, and the perpendicular tangent.
    // 0-7 CURVE POINTS
    // 8,9 POINT at T
    // 10,11 TANGENT at T
    // 12,13 RAY POINT
    // 14,15 RAY TANGENT
    private void cubic_perpendicular_ray(float[] cubic, float t) {
        GeometryUtils.evalCubicAt(
                cubic, 0,
                t, cubic, /*pos*/ 8, cubic, /*tangent*/ 8+2
        );
        if (cubic[8+2] == 0 && cubic[8+3] == 0) {
            int c = 0;
            if (MathUtil.isApproxZero(t)) {
                // P2 - P0
                cubic[8+2] = cubic[4] - cubic[0];
                cubic[8+3] = cubic[5] - cubic[1];
            } else if (MathUtil.isApproxEqual(t, 1)) {
                // P3 - P1
                cubic[8+2] = cubic[6] - cubic[2];
                cubic[8+3] = cubic[7] - cubic[3];
            } else {
                // If the cubic inflection falls on the cusp, subdivide the cubic
                // to find the tangent at that point.
                GeometryUtils.chopCubicAt(
                        cubic, 0,
                        cubic, 8+4,
                        t
                );
                // CHOPPED_P3 - CHOPPED_P2
                cubic[8+2] = cubic[8+4+6] - cubic[8+4+4];
                cubic[8+3] = cubic[8+4+7] - cubic[8+4+5];
                if (cubic[8+2] == 0 && cubic[8+3] == 0) {
                    // CHOPPED_P3 - CHOPPED_P1
                    cubic[8+2] = cubic[8+4+6] - cubic[8+4+2];
                    cubic[8+3] = cubic[8+4+7] - cubic[8+4+3];
                    c = 8+4;
                }
            }
            if (cubic[8+2] == 0 && cubic[8+3] == 0) {
                // P3 - P0
                cubic[8+2] = cubic[c+6] - cubic[c];
                cubic[8+3] = cubic[c+7] - cubic[c+1];
            }
        }
        set_perpendicular_ray(cubic);
    }

    // Given a quad and a t range, find the start and end if they haven't been found already.
    private void quad_quad_ends(float[] quad, QuadState pp) {
        if (!pp.set0) {
            quad_perpendicular_ray(quad, pp.t_from);
            pp.q0x = quad[8+4];
            pp.q0y = quad[8+5];
            pp.tan0x = quad[8+6];
            pp.tan0y = quad[8+7];
            pp.set0 = true;
        }
        if (!pp.set2) {
            quad_perpendicular_ray(quad, pp.t_to);
            pp.q2x = quad[8+4];
            pp.q2y = quad[8+5];
            pp.tan2x = quad[8+6];
            pp.tan2y = quad[8+7];
            pp.set2 = true;
        }
    }

    // Given a cubic and a t range, find the start and end if they haven't been found already.
    private void cubic_quad_ends(float[] cubic, QuadState pp) {
        if (!pp.set0) {
            cubic_perpendicular_ray(cubic, pp.t_from);
            pp.q0x = cubic[8+4];
            pp.q0y = cubic[8+5];
            pp.tan0x = cubic[8+6];
            pp.tan0y = cubic[8+7];
            pp.set0 = true;
        }
        if (!pp.set2) {
            cubic_perpendicular_ray(cubic, pp.t_to);
            pp.q2x = cubic[8+4];
            pp.q2y = cubic[8+5];
            pp.tan2x = cubic[8+6];
            pp.tan2y = cubic[8+7];
            pp.set2 = true;
        }
    }

    private int check_quad_quad(float[] quad, QuadState pp) {
        // get the quadratic approximation of the stroke
        quad_quad_ends(quad, pp);
        var result = intersect_ray(pp, true);
        if (result != INTERSECT_QUADRATIC) {
            return result;
        }
        // project a ray from the curve to the stroke
        quad_perpendicular_ray(quad, pp.t_mid);
        return check_close_enough(
                pp,
                quad[8+4], quad[8+5], // perpendicular ray
                quad[8]  , quad[8+1], // curve point
                quad
        );
    }

    private int check_quad_cubic(float[] cubic, QuadState pp) {
        // get the quadratic approximation of the stroke
        cubic_quad_ends(cubic, pp);
        var result = intersect_ray(pp, true);
        if (result != INTERSECT_QUADRATIC) {
            return result;
        }
        // project a ray from the curve to the stroke
        cubic_perpendicular_ray(cubic, pp.t_mid);
        return check_close_enough(
                pp,
                cubic[8+4], cubic[8+5], // perpendicular ray
                cubic[8]  , cubic[8+1], // curve point
                cubic
        );
    }

    // Given a cubic and a t-range, determine if the stroke can be described by a quadratic.
    private int find_tangents(float[] cubic, QuadState pp) {
        cubic_quad_ends(cubic, pp);
        return intersect_ray(pp, false);
    }

    private static boolean sharp_angle(QuadState pp, float[] v) {
        float ax = pp.q1x - pp.q0x;
        float ay = pp.q1y - pp.q0y;
        float bx = pp.q1x - pp.q2x;
        float by = pp.q1y - pp.q2y;
        float aLen = Point.lengthSq(ax, ay);
        float bLen = Point.lengthSq(bx, by);
        if (aLen > bLen) {
            float t = ax;
            ax = bx;
            bx = t;
            t = ay;
            ay = by;
            by = t;
            bLen = aLen;
        }
        v[8] = ax;
        v[9] = ay;
        if (Point.setLength(v, 8, bLen)) {
            return Point.dotProduct(v[8], v[9], bx, by) > 0;
        }
        return false;
    }

    // Return true if the point is conservatively not within the bounds of the quadratic curve
    private boolean quick_reject(
            QuadState pp,
            float x, float y
    ) {
        float xMin = MathUtil.min(pp.q0x, pp.q1x, pp.q2x);
        if (x + mInvResScale < xMin) {
            return true;
        }
        float xMax = MathUtil.max(pp.q0x, pp.q1x, pp.q2x);
        if (x - mInvResScale > xMax) {
            return true;
        }
        float yMin = MathUtil.min(pp.q0y, pp.q1y, pp.q2y);
        if (y + mInvResScale < yMin) {
            return true;
        }
        float yMax = MathUtil.max(pp.q0y, pp.q1y, pp.q2y);
        return y - mInvResScale > yMax;
    }

    private int check_close_enough(
            QuadState pp,
            float ray0x, float ray0y, // perpendicular ray
            float ray1x, float ray1y, // curve point
            float[] v
    ) {
        // measure the distance from the curve to the quad-stroke midpoint, compare to radius
        GeometryUtils.evalQuadAt(
                pp.q0x, pp.q0y, pp.q1x, pp.q1y, pp.q2x, pp.q2y,
                0.5f, v, 8
        );
        if (Point.distanceToSq(ray0x, ray0y, v[8], v[9]) <= mInvResScaleSquared) {
            // if the difference is small
            if (sharp_angle(pp, v)) {
                if (DEBUG) {
                    log(INTERSECT_SUBDIVIDE, pp,
                            "sharp_angle (1) =%g,%g, %g,%g, %g,%g",
                            pp.q0x, pp.q0y,
                            pp.q1x, pp.q1y,
                            pp.q2x, pp.q2y);
                }
                return INTERSECT_SUBDIVIDE;
            }
            if (DEBUG) {
                log(INTERSECT_QUADRATIC, pp,
                        "points_within_dist(ray[0]=%g,%g, strokeMid=%g,%g, mInvResScale=%g)",
                        ray0x, ray0y, v[8], v[9], mInvResScale);
            }
            return INTERSECT_QUADRATIC;
        }

        // measure the distance to quad's bounds
        if (quick_reject(pp, ray0x, ray0y)) {
            // if far, subdivide
            if (DEBUG) {
                log(INTERSECT_SUBDIVIDE, pp,
                        "!quick_reject(stroke=(%g,%g %g,%g %g,%g), ray[0]=%g,%g)",
                        pp.q0x, pp.q0y,
                        pp.q1x, pp.q1y,
                        pp.q2x, pp.q2y,
                        ray0x, ray0y);
            }
            return INTERSECT_SUBDIVIDE;
        }

        // measure the curve ray distance to the quad-stroke
        int nRoots;
        {
            // Intersect the line with the quad and return the t values on the quad where the line crosses.
            float dx = ray1x - ray0x;
            float dy = ray1y - ray0y;
            float A = (pp.q2y - ray0y) * dx - (pp.q2x - ray0x) * dy;
            float B = (pp.q1y - ray0y) * dx - (pp.q1x - ray0x) * dy;
            float C = (pp.q0y - ray0y) * dx - (pp.q0x - ray0x) * dy;
            A += C - 2 * B; // A = a - 2*b + c
            B -= C;         // B = -(b - c)
            nRoots = GeometryUtils.findUnitQuadRoots(A, 2 * B, C, v, 8);
        }
        if (nRoots != 1) {
            if (DEBUG) {
                log(INTERSECT_SUBDIVIDE, pp, "nRoots=%d != 1", nRoots);
            }
            return INTERSECT_SUBDIVIDE;
        }
        float t = v[8];
        GeometryUtils.evalQuadAt(
                pp.q0x, pp.q0y, pp.q1x, pp.q1y, pp.q2x, pp.q2y,
                t, v, 8
        );
        float error = mInvResScale * (1 - Math.abs(t - 0.5f) * 2);
        if (Point.distanceToSq(ray0x, ray0y, v[8], v[9]) <= (error * error)) {
            // if the difference is small, we're done
            if (sharp_angle(pp, v)) {
                if (DEBUG) {
                    log(INTERSECT_SUBDIVIDE, pp,
                            "sharp_angle (2) =%g,%g, %g,%g, %g,%g",
                            pp.q0x, pp.q0y,
                            pp.q1x, pp.q1y,
                            pp.q2x, pp.q2y);
                }
                return INTERSECT_SUBDIVIDE;
            }
            if (DEBUG) {
                log(INTERSECT_QUADRATIC, pp,
                        "points_within_dist(ray[0]=%g,%g, quadPt=%g,%g, error=%g)",
                        ray0x, ray0y, v[8], v[9], error);
            }
            return INTERSECT_QUADRATIC;
        }
        // otherwise, subdivide
        if (DEBUG) {
            log(INTERSECT_SUBDIVIDE, pp,
                    "fallthrough");
        }
        return INTERSECT_SUBDIVIDE;
    }

    private void emitDegenerateLine(QuadState pp) {
        PathConsumer path = mStrokeType == STROKE_TYPE_OUTER ? mOuter : mInner;
        path.lineTo(pp.q2x, pp.q2y);
    }

    private boolean strokeQuad(float[] quad, QuadState pp) {
        var result = check_quad_quad(quad, pp);
        if (result == INTERSECT_QUADRATIC) {
            PathConsumer path = mStrokeType == STROKE_TYPE_OUTER ? mOuter : mInner;
            path.quadTo(pp.q1x, pp.q1y, pp.q2x, pp.q2y);
            return true;
        }
        if (result == INTERSECT_DEGENERATE) {
            emitDegenerateLine(pp);
            return true;
        }
        if (++mRecursionDepth > MAX_QUAD_RECURSION_DEPTH) {
            return false;
        }
        QuadState mid = mQuadStack[mRecursionDepth];
        mid.init0(pp);
        if (!strokeQuad(quad, mid)) {
            return false;
        }
        mid.init2(pp);
        if (!strokeQuad(quad, mid)) {
            return false;
        }
        --mRecursionDepth;
        return true;
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {
        boolean degenerateAB = Point.isDegenerate(
                x1 - mPrevX,
                y1 - mPrevY
        );
        boolean degenerateBC = Point.isDegenerate(
                x2 - x1,
                y2 - y1
        );

        if (degenerateAB & degenerateBC) {
            // Degenerate into a point.
            // If the stroke consists of a moveTo followed by a degenerate curve, treat it
            // as if it were followed by a zero-length line. Lines without length
            // can have square and round end caps.
            lineTo(x2, y2);
            return;
        }

        if (degenerateAB | degenerateBC) {
            // Degenerate into a line.
            lineTo(x2, y2);
            return;
        }

        float[] quad = getQuad(x1, y1, x2, y2);
        if (quad_in_line(quad)) {
            float t = GeometryUtils.findQuadMaxCurvature(
                    quad, 0
            );
            if (t <= 0 || t >= 1) {
                // Degenerate into a line.
                lineTo(x2, y2);
                return;
            }
            GeometryUtils.evalQuadAt(
                    quad, 0,
                    quad, 8,
                    t
            );
            lineTo(quad[8], quad[9]);
            var saveJoiner = mJoiner;
            mJoiner = Joiner.get(Paint.JOIN_ROUND);
            lineTo(x2, y2);
            mJoiner = saveJoiner;
            return;
        }

        if (preJoinTo(x1, y1, false)) {
            assert mRecursionDepth == 0;
            QuadState pp = mQuadStack[0];
            initStroke(STROKE_TYPE_OUTER, pp, 0, 1);
            strokeQuad(quad, pp);
            initStroke(STROKE_TYPE_INNER, pp, 0, 1);
            strokeQuad(quad, pp);
            assert mRecursionDepth == 0;

            // compute normal BC
            quad[8] = (x2 - x1) * mResScale;
            quad[9] = (y2 - y1) * mResScale;
            if (Point.normalize(quad, 8)) {
                // Rotate CCW
                // newX = oldY, newY = -oldX
                float newX = quad[9];
                float newY = -quad[8];
                mNormal[NORMAL_X] = newX * mRadius;
                mNormal[NORMAL_Y] = newY * mRadius;
                mNormal[UNIT_NORMAL_X] = newX;
                mNormal[UNIT_NORMAL_Y] = newY;
            } // else use normal AB, see preJoinTo

            postJoinTo(x2, y2);
        } else {
            lineTo(x2, y2);
        }
    }

    private boolean strokeCubic(float[] cubic, QuadState pp) {
        if (!mFoundTangents) {
            var result = find_tangents(cubic, pp);
            if (result != INTERSECT_QUADRATIC) {
                if (result == INTERSECT_DEGENERATE
                        || Point.distanceToSq(pp.q0x, pp.q0y, pp.q2x, pp.q2y)
                        <= mInvResScaleSquared) {
                    cubic_perpendicular_ray(cubic, pp.t_mid);
                    if (Point.distanceToLineSegmentBetweenSq(
                            cubic[8+4], cubic[8+5],
                            pp.q0x, pp.q0y, pp.q2x, pp.q2y
                    ) <= mInvResScaleSquared) {
                        emitDegenerateLine(pp);
                        return true;
                    }
                }
            } else {
                mFoundTangents = true;
            }
        }
        if (mFoundTangents) {
            var result = check_quad_cubic(cubic, pp);
            if (result == INTERSECT_QUADRATIC) {
                PathConsumer path = mStrokeType == STROKE_TYPE_OUTER ? mOuter : mInner;
                path.quadTo(pp.q1x, pp.q1y, pp.q2x, pp.q2y);
                return true;
            }
            if (result == INTERSECT_DEGENERATE) {
                if (!pp.opposite_tangents) {
                    emitDegenerateLine(pp);
                    return true;
                }
            }
        }
        if (!Float.isFinite(pp.q2x) || !Float.isFinite(pp.q2y)) {
            return false;
        }
        if (++mRecursionDepth > (mFoundTangents ? MAX_CUBIC_RECURSION_DEPTH : MAX_TANGENT_RECURSION_DEPTH)) {
            return false;
        }
        QuadState mid = mQuadStack[mRecursionDepth];
        if (!mid.init0(pp)) {
            emitDegenerateLine(pp);
            --mRecursionDepth;
            return true;
        }
        if (!strokeCubic(cubic, mid)) {
            return false;
        }
        if (!mid.init2(pp)) {
            emitDegenerateLine(pp);
            --mRecursionDepth;
            return true;
        }
        if (!strokeCubic(cubic, mid)) {
            return false;
        }
        --mRecursionDepth;
        return true;
    }

    @Override
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        boolean degenerateAB = Point.isDegenerate(
                x1 - mPrevX,
                y1 - mPrevY
        );
        boolean degenerateBC = Point.isDegenerate(
                x2 - x1,
                y2 - y1
        );
        boolean degenerateCD = Point.isDegenerate(
                x3 - x2,
                y3 - y2
        );

        if (degenerateAB & degenerateBC & degenerateCD) {
            // Degenerate into a point.
            // If the stroke consists of a moveTo followed by a degenerate curve, treat it
            // as if it were followed by a zero-length line. Lines without length
            // can have square and round end caps.
            lineTo(x3, y3);
            return;
        }

        if ((degenerateAB?1:0) + (degenerateBC?1:0) + (degenerateCD?1:0) == 2) {
            // Degenerate into a line.
            lineTo(x3, y3);
            return;
        }

        float[] cubic = getCubic(x1, y1, x2, y2, x3, y3);
        if (cubic_in_line(cubic)) {
            // degenerate into 1 to 4 lines, round join if > 1
            // 8,9,10 for t-values
            // (12,13) for evalCubicAt
            int count = GeometryUtils.findCubicMaxCurvature(
                    cubic, 0,
                    cubic, 8
            );
            boolean any = false;
            var saveJoiner = mJoiner;
            // Now loop over the t-values, and reject any that evaluate to either end-point
            for (int index = 0; index < count; ++index) {
                float t = cubic[8 + index];
                if (t <= 0 || t >= 1) {
                    continue;
                }
                GeometryUtils.evalCubicAt(
                        cubic, 0,
                        cubic, 12,
                        t
                );
                float evalX = cubic[12];
                float evalY = cubic[13];
                // not P0 or P3
                if (evalX != cubic[0] && evalY != cubic[1] &&
                        evalX != cubic[6] && evalY != cubic[7]) {
                    lineTo(evalX, evalY);
                    if (!any) {
                        mJoiner = Joiner.get(Paint.JOIN_ROUND);
                        any = true;
                    }
                }
            }
            lineTo(x3, y3);
            if (any) {
                mJoiner = saveJoiner;
            }
            return;
        }

        final float tangentX, tangentY;
        if (degenerateAB) {
            tangentX = x2;
            tangentY = y2;
        } else {
            tangentX = x1;
            tangentY = y1;
        }
        if (preJoinTo(tangentX, tangentY, false)) {
            int infCount = GeometryUtils.findCubicInflectionPoints(
                    cubic, 0,
                    cubic, 8
            );
            // save ts in advance
            float t0 = cubic[8];
            float t1 = cubic[9];
            float lastT = 0;
            for (int index = 0; index <= infCount; ++index) {
                float nextT;
                if (index < infCount) {
                    assert index == 0 || index == 1;
                    if (index == 0) {
                        nextT = t0;
                    } else {
                        nextT = t1;
                    }
                } else {
                    nextT = 1;
                }
                assert mRecursionDepth == 0;
                QuadState pp = mQuadStack[0];
                initStroke(STROKE_TYPE_OUTER, pp, lastT, nextT);
                strokeCubic(cubic, pp);
                initStroke(STROKE_TYPE_INNER, pp, lastT, nextT);
                strokeCubic(cubic, pp);
                assert mRecursionDepth == 0;
                lastT = nextT;
            }
            float cusp = GeometryUtils.findCubicCusp(
                    cubic, 0
            );
            if (cusp > 0) {
                //TODO
            }

            // compute normal CD
            // emit the join even if one stroke succeeded but the last one failed
            // this avoids reversing an inner stroke with a partial path followed by another moveto
            //assert (!degenerateAB || !degenerateCD);
            if (degenerateAB) {
                // use AC instead
                degenerateAB = Point.isDegenerate(x2 - mPrevX, y2 - mPrevY);
            }
            if (degenerateCD) {
                // use BD instead
                degenerateCD = Point.isDegenerate(x3 - x1, y3 - y1);
                cubic[8] = (x3 - x1) * mResScale;
                cubic[9] = (y3 - y1) * mResScale;
            } else {
                // use CD
                cubic[8] = (x3 - x2) * mResScale;
                cubic[9] = (y3 - y2) * mResScale;
            }
            if (!degenerateAB && !degenerateCD &&
                    Point.normalize(cubic, 8)) {
                // Rotate CCW
                // newX = oldY, newY = -oldX
                float newX = cubic[9];
                float newY = -cubic[8];
                mNormal[NORMAL_X] = newX * mRadius;
                mNormal[NORMAL_Y] = newY * mRadius;
                mNormal[UNIT_NORMAL_X] = newX;
                mNormal[UNIT_NORMAL_Y] = newY;
            } // else use normal AB, see preJoinTo

            postJoinTo(x3, y3);
        } else {
            lineTo(x3, y3);
        }
    }

    @Override
    public void close() {
        finish(true, mPrevIsLine);
    }

    @Override
    public void done() {
        finish(false, mPrevIsLine);
        mOuter = null;
        assert mInner.isEmpty();
    }

    // finish the current contour
    private void finish(boolean close, boolean isLine) {
        if (mSegmentCount > 0) {
            if (close) {
                mJoiner.join(
                        mOuter, mInner,
                        mPrevUnitNormalX, mPrevUnitNormalY,
                        mPrevX, mPrevY,
                        mFirstUnitNormalX, mFirstUnitNormalY,
                        mRadius,
                        mInvMiterLimit,
                        mPrevIsLine,
                        isLine
                );
                mOuter.close();

                mInner.reversePop(mOuter, true);
                mOuter.close();
            } else {
                mCapper.cap(
                        mOuter,
                        mPrevX, mPrevY,
                        mPrevNormalX, mPrevNormalY
                );
                mInner.reversePop(mOuter, false);
                mCapper.cap(
                        mOuter,
                        mFirstX, mFirstY,
                        -mFirstNormalX, -mFirstNormalY
                );
                mOuter.close();
            }
        }
        mSegmentCount = -1;
    }

    /**
     * @author BloCamLimb
     */
    public interface Capper {

        void cap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY
        );

        static Capper get(int cap) {
            return switch (cap) {
                case Paint.CAP_BUTT -> Capper::doButtCap;
                case Paint.CAP_ROUND -> Capper::doRoundCap;
                case Paint.CAP_SQUARE -> Capper::doSquareCap;
                default -> throw new AssertionError(cap);
            };
        }

        static void doButtCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY) {
            // clockwise
            path.lineTo(pivotX - normalX, pivotY - normalY);
        }

        // degree = 90, kappa = (4/3) * (sqrt(2) - 1)
        //float C = 4 * (MathUtil.SQRT2 - 1) / 3;

        // we find a better approximation, see
        // https://spencermortensen.com/articles/bezier-circle/
        // radius 5102 pixels to get 1 pixel error
        float C = 0.5519150244935105707435627f;

        static void doRoundCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY) {
            // two 1/4 circular arcs, clockwise
            final float Cmx = C * normalX;
            final float Cmy = C * normalY;
            path.cubicTo(
                    pivotX + normalX - Cmy, pivotY + normalY + Cmx,
                    pivotX - normalY + Cmx, pivotY + normalX + Cmy,
                    pivotX - normalY, pivotY + normalX
            );
            path.cubicTo(
                    pivotX - normalY - Cmx, pivotY + normalX - Cmy,
                    pivotX - normalX - Cmy, pivotY - normalY + Cmx,
                    pivotX - normalX, pivotY - normalY
            );
        }

        static void doSquareCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY) {
            // clockwise
            path.lineTo(pivotX + normalX - normalY, pivotY + normalY + normalX);
            path.lineTo(pivotX - normalX - normalY, pivotY - normalY + normalX);
            path.lineTo(pivotX - normalX, pivotY - normalY);
        }
    }

    /**
     * @author BloCamLimb
     */
    public interface Joiner {

        void join(
                PathConsumer outer,
                PathConsumer inner,
                float beforeUnitNormalX,
                float beforeUnitNormalY,
                float pivotX,
                float pivotY,
                float afterUnitNormalX,
                float afterUnitNormalY,
                float radius,
                float invMiterLimit,
                boolean prevIsLine,
                boolean currIsLine
        );

        static Joiner get(int join) {
            return switch (join) {
                case Paint.JOIN_MITER -> Joiner::doMiterJoin;
                case Paint.JOIN_ROUND -> Joiner::doRoundJoin;
                case Paint.JOIN_BEVEL -> Joiner::doBevelJoin;
                default -> throw new AssertionError(join);
            };
        }

        // assumes the origin is top left, y-down
        // then the counter-clockwise direction is the reverse direction (inner)
        @Contract(pure = true)
        static boolean isCCW(float beforeX, float beforeY,
                             float afterX, float afterY) {
            return Point.crossProduct(beforeX, beforeY, afterX, afterY) <= 0;
        }

        int ANGLE_NEARLY_0 = 0;   // 0 degrees
        int ANGLE_ACUTE = 1;      // (0,90) degrees
        int ANGLE_NEARLY_180 = 2; // 180 degrees
        int ANGLE_OBTUSE = 3;     // (90,180) degrees
        int ANGLE_NEARLY_90 = 4;  // 90 degrees

        static void doMiterJoin(
                PathConsumer outer,
                PathConsumer inner,
                float beforeUnitNormalX,
                float beforeUnitNormalY,
                float pivotX,
                float pivotY,
                float afterUnitNormalX,
                float afterUnitNormalY,
                float radius,
                float invMiterLimit,
                boolean prevIsLine,
                boolean currIsLine) {
            float dot = Point.dotProduct(
                    beforeUnitNormalX, beforeUnitNormalY,
                    afterUnitNormalX, afterUnitNormalY
            );
            // 0 - 0 degrees
            // 1 - (0,90] degrees
            // 2 - 180 degrees
            // 3 - (90,180) degrees
            int angleType;
            if (dot >= 0) {
                angleType = dot >= 1f - MathUtil.EPS ? ANGLE_NEARLY_0 : ANGLE_ACUTE;
            } else {
                angleType = dot <= MathUtil.EPS - 1f ? ANGLE_NEARLY_180 : ANGLE_OBTUSE;
            }
            if (angleType == ANGLE_NEARLY_0) {
                // 0 degrees, no need to join
                return;
            }

            if (angleType == ANGLE_NEARLY_180) {
                // 180 degrees
                currIsLine = false;
            } else {
                boolean doMiter = true;
                float midX = 0;
                float midY = 0;

                boolean ccw = isCCW(
                        beforeUnitNormalX, beforeUnitNormalY,
                        afterUnitNormalX, afterUnitNormalY
                );

                /*  Before we enter the world of square-roots and divides,
                    check if we're trying to join an upright right angle
                    (common case for stroking rectangles). If so, special case
                    that (for speed an accuracy).
                    Note: we only need to check one normal if dot==0
                */
                if (0 == dot && invMiterLimit <= MathUtil.INV_SQRT2) {
                    midX = (beforeUnitNormalX + afterUnitNormalX) * radius;
                    midY = (beforeUnitNormalY + afterUnitNormalY) * radius;
                } else {
                    /*  midLength = radius / sinHalfAngle
                        if (midLength > miterLimit * radius) abort
                        if (radius / sinHalf > miterLimit * radius) abort
                        if (1 / sinHalf > miterLimit) abort
                        if (1 / miterLimit > sinHalf) abort
                        My dotProd is opposite sign, since it is built from normals and not tangents
                        hence 1 + dot instead of 1 - dot in the formula
                    */
                    float sinHalfAngle = (float) Math.sqrt((1 + dot) * 0.5);
                    if (sinHalfAngle < invMiterLimit) {
                        currIsLine = false;
                        doMiter = false;
                    } else {
                        // choose the most accurate way to form the initial mid-vector
                        if (angleType == ANGLE_OBTUSE) {
                            // (90,180) degrees, sharp
                            if (ccw) {
                                midX = beforeUnitNormalY - afterUnitNormalY;
                                midY = afterUnitNormalX - beforeUnitNormalX;
                            } else {
                                midX = afterUnitNormalY - beforeUnitNormalY;
                                midY = beforeUnitNormalX - afterUnitNormalX;
                            }
                        } else {
                            // (0,90] degrees, shallow
                            midX = beforeUnitNormalX + afterUnitNormalX;
                            midY = beforeUnitNormalY + afterUnitNormalY;
                        }

                        // normalize mid-vector to (radius / sinHalfAngle)
                        double dmag = Math.sqrt(
                                (double) midX * (double) midX +
                                        (double) midY * (double) midY
                        );
                        double dscale = radius / sinHalfAngle / dmag;
                        midX = (float) (midX * dscale);
                        midY = (float) (midY * dscale);
                    }
                }

                if (doMiter) {
                    outer.lineTo(pivotX + midX, pivotY + midY);
                }
            }

            float afterX = afterUnitNormalX * radius;
            float afterY = afterUnitNormalY * radius;
            if (!currIsLine) {
                outer.lineTo(pivotX + afterX, pivotY + afterY);
            }
            inner.lineTo(pivotX - afterX, pivotY - afterY);
        }

        static void doRoundJoin(
                PathConsumer outer,
                PathConsumer inner,
                float beforeUnitNormalX,
                float beforeUnitNormalY,
                float pivotX,
                float pivotY,
                float afterUnitNormalX,
                float afterUnitNormalY,
                float radius,
                float invMiterLimit,
                boolean prevIsLine,
                boolean currIsLine) {
            float dot = Point.dotProduct(
                    beforeUnitNormalX, beforeUnitNormalY,
                    afterUnitNormalX, afterUnitNormalY
            );
            // 0 - 0 degrees
            // 1 - (0,90) degrees
            // 2 - 180 degrees
            // 3 - (90,180) degrees
            // 4 - 90 degrees
            int angleType;
            if (-MathUtil.EPS <= dot && dot <= MathUtil.EPS) {
                angleType = ANGLE_NEARLY_90;
            } else if (dot >= 0) {
                angleType = dot >= 1f - MathUtil.EPS ? ANGLE_NEARLY_0 : ANGLE_ACUTE;
            } else {
                angleType = dot <= MathUtil.EPS - 1f ? ANGLE_NEARLY_180 : ANGLE_OBTUSE;
            }
            if (angleType == ANGLE_NEARLY_0) {
                // 0 degrees, no need to join
                return;
            }

            boolean ccw = isCCW(
                    beforeUnitNormalX, beforeUnitNormalY,
                    afterUnitNormalX, afterUnitNormalY
            );
            if (ccw) {
                var tmp = outer;
                outer = inner;
                inner = tmp;
                beforeUnitNormalX = -beforeUnitNormalX;
                beforeUnitNormalY = -beforeUnitNormalY;
                afterUnitNormalX = -afterUnitNormalX;
                afterUnitNormalY = -afterUnitNormalY;
            }

            float afterX;
            float afterY;
            if (angleType == ANGLE_ACUTE) {
                // (0,90) degrees, add one fast approx arc
                doBezierApproxForArc(
                        outer,
                        beforeUnitNormalX,
                        beforeUnitNormalY,
                        pivotX,
                        pivotY,
                        afterUnitNormalX,
                        afterUnitNormalY,
                        radius,
                        ccw
                );
                afterX = afterUnitNormalX * radius;
                afterY = afterUnitNormalY * radius;
            } else if (angleType == ANGLE_NEARLY_90) {
                // 90 degrees, add one approx arc
                afterX = afterUnitNormalX * radius;
                afterY = afterUnitNormalY * radius;
                doBezierApproxForArc(
                        outer,
                        beforeUnitNormalX * radius,
                        beforeUnitNormalY * radius,
                        pivotX,
                        pivotY,
                        afterX,
                        afterY,
                        ccw ? -Capper.C : Capper.C
                );
            } else {
                // split the arc into 2 arcs spanning the same angle
                float unitNormalX;
                float unitNormalY;
                if (ccw) {
                    unitNormalX = beforeUnitNormalY - afterUnitNormalY;
                    unitNormalY = afterUnitNormalX - beforeUnitNormalX;
                } else {
                    unitNormalX = afterUnitNormalY - beforeUnitNormalY;
                    unitNormalY = beforeUnitNormalX - afterUnitNormalX;
                }
                double dmag = Math.sqrt(
                        (double) unitNormalX * (double) unitNormalX +
                                (double) unitNormalY * (double) unitNormalY
                );
                double dscale = 1.0 / dmag;
                unitNormalX = (float) (unitNormalX * dscale);
                unitNormalY = (float) (unitNormalY * dscale);
                if (angleType == ANGLE_OBTUSE) {
                    // (90,180) degrees, add two fast approx arcs
                    doBezierApproxForArc(
                            outer,
                            beforeUnitNormalX,
                            beforeUnitNormalY,
                            pivotX,
                            pivotY,
                            unitNormalX,
                            unitNormalY,
                            radius,
                            ccw
                    );
                    doBezierApproxForArc(
                            outer,
                            unitNormalX,
                            unitNormalY,
                            pivotX,
                            pivotY,
                            afterUnitNormalX,
                            afterUnitNormalY,
                            radius,
                            ccw
                    );
                    afterX = afterUnitNormalX * radius;
                    afterY = afterUnitNormalY * radius;
                } else {
                    // 180 degrees, add two approx arcs
                    float normalX = unitNormalX * radius;
                    float normalY = unitNormalY * radius;
                    afterX = afterUnitNormalX * radius;
                    afterY = afterUnitNormalY * radius;
                    doBezierApproxForArc(
                            outer,
                            beforeUnitNormalX * radius,
                            beforeUnitNormalY * radius,
                            pivotX,
                            pivotY,
                            normalX,
                            normalY,
                            ccw ? -Capper.C : Capper.C
                    );
                    doBezierApproxForArc(
                            outer,
                            normalX,
                            normalY,
                            pivotX,
                            pivotY,
                            afterX,
                            afterY,
                            ccw ? -Capper.C : Capper.C
                    );
                }
            }

            inner.lineTo(pivotX - afterX, pivotY - afterY);
        }

        // fast approximation for arcs (span < 90 degrees)
        // radius 3663 pixels to get 1 pixel error
        static void doBezierApproxForArc(
                PathConsumer path,
                float beforeUnitNormalX,
                float beforeUnitNormalY,
                float pivotX,
                float pivotY,
                float afterUnitNormalX,
                float afterUnitNormalY,
                float radius,
                boolean ccw) {
            // dot = cos(a)
            float halfCosAngle = Point.dotProduct(
                    beforeUnitNormalX, beforeUnitNormalY,
                    afterUnitNormalX, afterUnitNormalY
            ) * 0.5f;
            // C = 4/3 * tan(a/4)
            //   = 4/3 * sin(a/2) / (1 + cos(a/2))
            // sin(a/2) = sqrt((1 - cos(a)) / 2)
            // cos(a/2) = sqrt((1 + cos(a)) / 2)
            float C = (float) ((4.0 / 3.0) * Math.sqrt(0.5 - halfCosAngle) /
                    (1.0 + Math.sqrt(0.5 + halfCosAngle)));
            doBezierApproxForArc(
                    path,
                    beforeUnitNormalX * radius,
                    beforeUnitNormalY * radius,
                    pivotX,
                    pivotY,
                    afterUnitNormalX * radius,
                    afterUnitNormalY * radius,
                    ccw ? -C : C
            );
        }

        static void doBezierApproxForArc(
                PathConsumer path,
                float beforeX,
                float beforeY,
                float pivotX,
                float pivotY,
                float afterX,
                float afterY,
                float k) {
            float x0 = pivotX + beforeX;
            float y0 = pivotY + beforeY;
            float x1 = x0 - k * beforeY;
            float y1 = y0 + k * beforeX;
            float x3 = pivotX + afterX;
            float y3 = pivotY + afterY;
            float x2 = x3 + k * afterY;
            float y2 = y3 - k * afterX;
            path.cubicTo(
                    x1, y1,
                    x2, y2,
                    x3, y3
            );
        }

        static void doBevelJoin(
                PathConsumer outer,
                PathConsumer inner,
                float beforeUnitNormalX,
                float beforeUnitNormalY,
                float pivotX,
                float pivotY,
                float afterUnitNormalX,
                float afterUnitNormalY,
                float radius,
                float invMiterLimit,
                boolean prevIsLine,
                boolean currIsLine) {
            float afterX = afterUnitNormalX * radius;
            float afterY = afterUnitNormalY * radius;
            outer.lineTo(pivotX + afterX, pivotY + afterY);
            inner.lineTo(pivotX - afterX, pivotY - afterY);
        }
    }
}
