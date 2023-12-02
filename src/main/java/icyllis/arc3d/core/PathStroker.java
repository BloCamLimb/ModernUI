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

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * PathStroker is a {@link PathConsumer} that converts paths by stroking paths.
 * This is invoked when a {@link Path} is drawn in a canvas with the
 * {@link Paint#STROKE} bit set in the paint. The new path consists of
 * contours, and the style change from thick stroke to fill.
 *
 * @author BloCamLimb
 */
public class PathStroker implements PathConsumer {

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

    // (m0,m1) is normal
    // (m2,m3) is unit normal
    private final float[] mNormal = new float[4];

    public void init(@Nonnull PathConsumer out,
                     float radius,
                     int cap,
                     int join,
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

    private boolean preJoinTo(float x, float y, boolean isLine) {
        assert mSegmentCount >= 0;

        float dxs = (x - mPrevX) * mResScale;
        float dys = (y - mPrevY) * mResScale;
        // normalize (dx,dy)
        double dmag = Math.sqrt(
                (double) dxs * (double) dxs +
                        (double) dys * (double) dys
        );
        double dscale = 1.0 / dmag;
        float tx = (float) (dxs * dscale);
        float ty = (float) (dys * dscale);
        if (Point.isDegenerate(tx, ty)) {
            if (mCapStyle == Paint.CAP_BUTT) {
                return false;
            }
            /* Square caps and round caps draw even if the segment length is zero.
               Since the zero length segment has no direction, set the orientation
               to upright as the default orientation */
            mNormal[0] = mRadius;
            mNormal[1] = 0;
            mNormal[2] = 1;
            mNormal[3] = 0;
        } else {
            // Rotate CCW
            mNormal[0] = ty * mRadius;
            mNormal[1] = -tx * mRadius;
            mNormal[2] = ty;
            mNormal[3] = -tx;
        }

        if (mSegmentCount == 0) {
            mFirstNormalX = mNormal[0];
            mFirstNormalY = mNormal[1];
            mFirstUnitNormalX = mNormal[2];
            mFirstUnitNormalY = mNormal[3];
            mFirstOuterX = mPrevX + mFirstNormalX;
            mFirstOuterY = mPrevY + mFirstNormalY;

            mOuter.moveTo(mFirstOuterX, mFirstOuterY);
            mInner.moveTo(mPrevX - mFirstNormalX, mPrevY - mFirstNormalY);
        } else {
            mJoiner.join(
                    mOuter, mInner,
                    mPrevUnitNormalX, mPrevUnitNormalY,
                    mPrevX, mPrevY,
                    mNormal[2], mNormal[3],
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
        mPrevNormalX = mNormal[0];
        mPrevNormalY = mNormal[1];
        mPrevUnitNormalX = mNormal[2];
        mPrevUnitNormalY = mNormal[3];
        mSegmentCount++;
    }

    @Override
    public void lineTo(float x, float y) {
        boolean degenerate =
                Point.isApproxEqual(mPrevX, mPrevY, x, y, MathUtil.EPS);
        if (degenerate && mCapStyle == Paint.CAP_BUTT) {
            return;
        }
        if (degenerate && mJoinCompleted) {
            return;
        }
        if (preJoinTo(x, y, true)) {
            mOuter.lineTo(x + mNormal[0], y + mNormal[1]);
            mInner.lineTo(x - mNormal[0], y - mNormal[1]);
            postJoinTo(x, y);
        }
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {

    }

    @Override
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {

    }

    @Override
    public void closePath() {
        finish(true, mPrevIsLine);
    }

    @Override
    public void pathDone() {
        finish(false, mPrevIsLine);
    }

    // finish the current contour
    private void finish(boolean close, boolean isLine) {
        if (mSegmentCount > 0) {
            if (close) {

            } else {
                mCapper.cap(
                        mOuter,
                        mPrevX, mPrevY,
                        mPrevNormalX, mPrevNormalY
                );
                mInner.reversePop(mOuter);
                mCapper.cap(
                        mOuter,
                        mFirstX, mFirstY,
                        -mFirstNormalX, -mFirstNormalY
                );
                mOuter.closePath();
            }
        }
        mSegmentCount = -1;
    }

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

        @Contract(pure = true)
        static boolean isClockwise(float beforeX, float beforeY,
                                   float afterX, float afterY) {
            return beforeX * afterY > beforeY * afterX;
        }

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
                angleType = dot >= 1f - MathUtil.EPS ? 0 : 1;
            } else {
                angleType = dot <= MathUtil.EPS - 1f ? 2 : 3;
            }
            if (angleType == 0) {
                // 0 degrees, no need to join
                return;
            }

            float afterX = afterUnitNormalX;
            float afterY = afterUnitNormalY;

            if (angleType == 2) {
                // 180 degrees
                currIsLine = false;
            } else {
                boolean doMiter = true;
                float midX = 0;
                float midY = 0;

                boolean ccw = !isClockwise(beforeUnitNormalX, beforeUnitNormalY, afterX, afterY);

                /*  Before we enter the world of square-roots and divides,
                    check if we're trying to join an upright right angle
                    (common case for stroking rectangles). If so, special case
                    that (for speed an accuracy).
                    Note: we only need to check one normal if dot==0
                */
                if (0 == dot && invMiterLimit <= MathUtil.INV_SQRT2) {
                    midX = (beforeUnitNormalX + afterX) * radius;
                    midY = (beforeUnitNormalY + afterY) * radius;
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
                        if (angleType == 3) {
                            // (90,180) degrees
                            if (ccw) {
                                midX = beforeUnitNormalY - afterY;
                                midY = afterX - beforeUnitNormalX;
                            } else {
                                midX = afterY - beforeUnitNormalY;
                                midY = beforeUnitNormalX - afterX;
                            }
                        } else {
                            // (0,90] degrees
                            midX = beforeUnitNormalX + afterX;
                            midY = beforeUnitNormalY + afterY;
                        }

                        // normalize mid-vector
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

            afterX *= radius;
            afterY *= radius;
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
            if (MathUtil.isApproxZero(dot)) {
                angleType = 4;
            } else if (dot >= 0) {
                angleType = dot >= 1f - MathUtil.EPS ? 0 : 1;
            } else {
                angleType = dot <= MathUtil.EPS - 1f ? 2 : 3;
            }
            if (angleType == 0) {
                // 0 degrees, no need to join
                return;
            }

            boolean cw = isClockwise(
                    beforeUnitNormalX, beforeUnitNormalY,
                    afterUnitNormalX, afterUnitNormalY
            );
            if (!cw) {
                var tmp = outer;
                outer = inner;
                inner = tmp;
                beforeUnitNormalX = -beforeUnitNormalX;
                beforeUnitNormalY = -beforeUnitNormalY;
                afterUnitNormalX = -afterUnitNormalX;
                afterUnitNormalY = -afterUnitNormalY;
            }

            if (angleType == 1) {
                // (0,90) degrees, add one fast approx arc
            } else if (angleType == 2) {
                // 180 degrees, add two approx arcs
                /*final float Cmx = Capper.C * normalX;
                final float Cmy = Capper.C * normalY;
                path.cubicTo(
                        pivotX + normalX - Cmy, pivotY + normalY + Cmx,
                        pivotX - normalY + Cmx, pivotY + normalX + Cmy,
                        pivotX - normalY, pivotY + normalX
                );
                path.cubicTo(
                        pivotX - normalY - Cmx, pivotY + normalX - Cmy,
                        pivotX - normalX - Cmy, pivotY - normalY + Cmx,
                        pivotX - normalX, pivotY - normalY
                );*/
            } else if (angleType == 3) {
                // (90,180) degrees, add two fast approx arcs
            } else {
                // 90 degrees, add one approx arc
            }

            float afterX = afterUnitNormalX * radius;
            float afterY = afterUnitNormalY * radius;
            inner.lineTo(pivotX - afterX, pivotY - afterY);
        }

        static void doBezierApproxForArc() {

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
