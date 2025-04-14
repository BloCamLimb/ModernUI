/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.animation;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.MathUtil;

/**
 * An interpolator that can traverse a Bezier curve that extends from
 * <code>(0, 0)</code> to <code>(1, 1)</code>. The x coordinate along the curve
 * is the input value and the output is the y coordinate of the line at that point.
 */
public class BezierInterpolator implements TimeInterpolator {

    private static final float PRECISION = 0.002f;

    private final float[] mXs; // x coordinates in the line
    private final float[] mYs; // y coordinates in the line

    /**
     * Create an interpolator for a quadratic Bezier curve. The end points
     * <code>(0, 0)</code> and <code>(1, 1)</code> are assumed.
     *
     * @param controlX The x coordinate of the quadratic Bezier control point.
     * @param controlY The y coordinate of the quadratic Bezier control point.
     */
    public BezierInterpolator(float controlX, float controlY) {
        // linearize
        int numPoints = icyllis.arc3d.granite.PathUtils.countQuadraticPoints(
                0, 0, controlX, controlY, 1f, 1f,
                PRECISION
        );
        float[] coords = new float[numPoints << 1];
        int numCoords = icyllis.arc3d.granite.PathUtils.generateQuadraticPoints(
                0, 0, controlX, controlY, 1f, 1f,
                PRECISION * PRECISION,
                coords, 0, coords.length
        );
        float[] xs = new float[1 + (numCoords >> 1)];
        float[] ys = new float[1 + (numCoords >> 1)];
        // initial point is (0,0)
        for (int i = 0, p = 1; i < numCoords; i += 2, p++) {
            xs[p] = coords[i];
            ys[p] = coords[i + 1];
        }
        mXs = xs;
        mYs = ys;
    }

    /**
     * Create an interpolator for a cubic Bezier curve.  The end points
     * <code>(0, 0)</code> and <code>(1, 1)</code> are assumed.
     *
     * @param controlX1 The x coordinate of the first control point of the cubic Bezier.
     * @param controlY1 The y coordinate of the first control point of the cubic Bezier.
     * @param controlX2 The x coordinate of the second control point of the cubic Bezier.
     * @param controlY2 The y coordinate of the second control point of the cubic Bezier.
     */
    public BezierInterpolator(float controlX1, float controlY1, float controlX2, float controlY2) {
        // linearize
        int numPoints = icyllis.arc3d.granite.PathUtils.countCubicPoints(
                0, 0, controlX1, controlY1, controlX2, controlY2, 1f, 1f,
                PRECISION
        );
        float[] coords = new float[numPoints << 1];
        int numCoords = icyllis.arc3d.granite.PathUtils.generateCubicPoints(
                0, 0, controlX1, controlY1, controlX2, controlY2, 1f, 1f,
                PRECISION * PRECISION,
                coords, 0, coords.length
        );
        float[] xs = new float[1 + (numCoords >> 1)];
        float[] ys = new float[1 + (numCoords >> 1)];
        // initial point is (0,0)
        for (int i = 0, p = 1; i < numCoords; i += 2, p++) {
            xs[p] = coords[i];
            ys[p] = coords[i + 1];
        }
        mXs = xs;
        mYs = ys;
    }

    private BezierInterpolator(float[] xs, float[] ys) {
        mXs = xs;
        mYs = ys;
    }

    /**
     * Create an interpolator for two cubic Bezier curves.  The end points
     * <code>(0, 0)</code> and <code>(1, 1)</code> are assumed.
     * <p>
     * The first cubic Bezier is formed by <code>(0, 0)</code>, <code>(controlX1, controlY1)</code>,
     * <code>(controlX2, controlY2)</code>, <code>(controlX3, controlY3)</code>;
     * and the second cubic Bezier is formed by <code>(controlX3, controlY3)</code>,
     * <code>(controlX4, controlY4)</code>, <code>(controlX5, controlY5)</code>, <code>(1, 1)</code>
     */
    @NonNull
    public static BezierInterpolator createTwoCubic(float controlX1, float controlY1,
                                                    float controlX2, float controlY2,
                                                    float controlX3, float controlY3,
                                                    float controlX4, float controlY4,
                                                    float controlX5, float controlY5) {
        // linearize
        int numPoints1 = icyllis.arc3d.granite.PathUtils.countCubicPoints(
                0, 0, controlX1, controlY1, controlX2, controlY2, controlX3, controlY3,
                PRECISION
        );
        int numPoints2 = icyllis.arc3d.granite.PathUtils.countCubicPoints(
                controlX3, controlY3, controlX4, controlY4, controlX5, controlY5, 1f, 1f,
                PRECISION
        );
        float[] coords = new float[(numPoints1 << 1) + (numPoints2 << 1)];
        int numCoords1 = icyllis.arc3d.granite.PathUtils.generateCubicPoints(
                0, 0, controlX1, controlY1, controlX2, controlY2, controlX3, controlY3,
                PRECISION * PRECISION,
                coords, 0, numPoints1 << 1
        );
        int numCoords2 = icyllis.arc3d.granite.PathUtils.generateCubicPoints(
                controlX3, controlY3, controlX4, controlY4, controlX5, controlY5, 1f, 1f,
                PRECISION * PRECISION,
                coords, numCoords1, numPoints2 << 1
        );
        float[] xs = new float[1 + (numCoords1 >> 1) + (numCoords2 >> 1)];
        float[] ys = new float[1 + (numCoords1 >> 1) + (numCoords2 >> 1)];
        // initial point is (0,0)
        for (int i = 0, p = 1; i < numCoords1 + numCoords2; i += 2, p++) {
            xs[p] = coords[i];
            ys[p] = coords[i + 1];
        }
        return new BezierInterpolator(xs, ys);
    }

    /**
     * Using the curve in this interpolator that can be described as
     * <code>y = f(x)</code>, finds the y coordinate of the line given <code>t</code>
     * as the x coordinate. Values less than 0 will always return 0 and values greater
     * than 1 will always return 1.
     *
     * @param t Treated as the x coordinate along the line.
     * @return The y coordinate of the Path along the line where x = <code>t</code>.
     * @see TimeInterpolator#getInterpolation(float)
     */
    @Override
    public float getInterpolation(float t) {
        if (t <= 0) {
            return 0;
        } else if (t >= 1) {
            return 1;
        }
        int low = 0;
        int high = mXs.length - 1;

        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (t < mXs[mid]) {
                high = mid;
            } else {
                low = mid;
            }
        }

        float xRange = mXs[high] - mXs[low];
        if (xRange == 0) {
            return mYs[low];
        }

        float tInRange = t - mXs[low];
        float fraction = tInRange / xRange;

        return MathUtil.lerp(mYs[low], mYs[high], fraction);
    }
}
