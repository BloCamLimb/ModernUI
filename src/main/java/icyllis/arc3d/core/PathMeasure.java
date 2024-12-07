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

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.intellij.lang.annotations.MagicConstant;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.CheckReturnValue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link PathMeasure} provides measurements on segments of path contours
 * (lines, quadratic curves, cubic curves), such as determining the length of the path,
 * and/or finding the position and tangent along it.
 * <p>
 * A {@link PathMeasure} object can be reused for measuring different {@link Path}
 * objects, by calling {@link #reset(Path, boolean, float)}.
 */
public class PathMeasure {

    @MagicConstant(flags = {MATRIX_FLAG_GET_POSITION, MATRIX_FLAG_GET_TANGENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatrixFlags {
    }

    public static final int MATRIX_FLAG_GET_POSITION = 0x1;
    public static final int MATRIX_FLAG_GET_TANGENT = 0x2;
    public static final int MATRIX_FLAG_GET_POS_AND_TAN = MATRIX_FLAG_GET_POSITION | MATRIX_FLAG_GET_TANGENT;

    private final Path mPath = new Path();
    private float mTolerance;
    private boolean mForceClose;

    private Path.RawIterator mIterator;

    private static final int SEGMENT_COLUMNS = 3;

    private static final int SEGMENT_DISTANCE = 0;
    private static final int SEGMENT_COORD_INDEX = 1;
    private static final int SEGMENT_T_AND_TYPE = 2;

    private static final int SEGMENT_TYPE_SHIFT = 30;
    private static final int SEGMENT_T_MASK = (1 << SEGMENT_TYPE_SHIFT) - 1;
    private static final int MAX_T_VALUE = SEGMENT_T_MASK; // unsigned

    // segment type
    // fits in 2 bits, just use (field >>> 30)
    private static final int SEGMENT_LINE = 0;
    private static final int SEGMENT_QUAD = 1;
    private static final int SEGMENT_CUBIC = 3;

    // the backing array holding segments
    // each segment is 3 sequential values
    // [0] = total distance up to this point, float in int bits
    // [1] = coord index to coords array
    // [2] = bitfield
    //       lower 30 bits is parameter T value in integers
    //       higher 2 bits is segment type
    private final IntArrayList mSegments = new IntArrayList();
    // the backing array holding x,y pairs
    private final FloatArrayList mCoords = new FloatArrayList();

    private float mContourLength;
    private boolean mContourClosed;

    protected final float[] mTmp = new float[28];

    /**
     * Create an empty PathMeasure object.
     */
    public PathMeasure() {
    }

    /**
     * This constructor assumes that <var>resScale</var> is 1.
     *
     * @see #PathMeasure(Path, boolean, float)
     */
    public PathMeasure(Path path, boolean forceClose) {
        reset(path, forceClose);
    }

    /**
     * Create a PathMeasure object associated with the specified <var>path</var> object.
     * <p>
     * This constructor makes a fast copy of <var>path</var>, then any modifications
     * to <var>path</var> will not affect subsequent measurements.
     * <p>
     * <var>resScale</var> controls the precision of the measurement. For example, if you
     * draw the measured path onto Canvas with a scale of 5.0, then <var>resScale</var>
     * should also be set to 5.0. Values greater than 1 will increase precision, but also
     * slow down the computation.
     * <p>
     * If <var>forceClose</var> is true, then the path will be considered as "closed"
     * even if its contour was not explicitly closed.
     */
    public PathMeasure(Path path, boolean forceClose, float resScale) {
        reset(path, forceClose, resScale);
    }

    /**
     * Resets this {@link PathMeasure} object to its initial state, just like calling
     * {@link #PathMeasure()} or {@link #reset(Path, boolean, float)} with an empty path.
     * This method releases the internal reference to the original path.
     */
    public void reset() {
        // mPath is a fast copy of the given path, we would like to recycle it
        mPath.recycle();

        mIterator = null;
        // just clear segments, because we check against it
        // coords are values, no need to clear
        mSegments.clear();
    }

    /**
     * This method assumes that <var>resScale</var> is 1.
     *
     * @see #reset(Path, boolean, float)
     */
    public boolean reset(Path path, boolean forceClose) {
        return reset(path, forceClose, 1);
    }

    /**
     * Resets this {@link PathMeasure} object with a new path.
     * <p>
     * This method makes a fast copy of <var>path</var>, then any modifications
     * to <var>path</var> will not affect subsequent measurements.
     * <p>
     * <var>resScale</var> controls the precision of the measurement. For example, if you
     * draw the measured path onto Canvas with a scale of 5.0, then <var>resScale</var>
     * should also be set to 5.0. Values greater than 1 will increase precision, but also
     * slow down the computation.
     * <p>
     * If <var>forceClose</var> is true, then the path will be considered as "closed"
     * even if its contour was not explicitly closed.
     *
     * @return true if there is a valid contour
     */
    public boolean reset(Path path, boolean forceClose, float resScale) {
        if (path != null && path.isFinite()) {
            mPath.set(path);
            // use 0.5 instead of 0.25
            mTolerance = 0.5f / resScale;
            mForceClose = forceClose;

            mIterator = mPath.new RawIterator();
            return nextContour();
        } else {
            reset();
            return false;
        }
    }

    /**
     * Move to the next contour in the path and compute contour segments.
     * Return true if one exists, or false if we're done with the path.
     * If this method returns false, then {@link #reset()} is called.
     *
     * @see #hasContour()
     */
    public boolean nextContour() {
        if (mIterator == null) {
            return false;
        }
        while (mIterator.hasNext()) {
            if (computeSegments()) {
                return true;
            }
        }
        // release if we're done
        reset();
        return false;
    }

    /**
     * Returns true if there is a valid contour (length greater than 0).
     */
    public boolean hasContour() {
        return !mSegments.isEmpty();
    }

    /**
     * Returns the length of the current contour.
     */
    public float getContourLength() {
        return hasContour() ? mContourLength : 0;
    }

    /**
     * Returns true if the current contour is closed.
     */
    public boolean isContourClosed() {
        return hasContour() && mContourClosed;
    }

    /**
     * Clamps <var>distance</var> between 0 and {@link #getContourLength()}, and then
     * computes the corresponding position and tangent vector (un-normalized).
     * Returns false if there is no contour, or a zero-length path was specified,
     * in which case <var>position</var> and <var>tangent</var> are unchanged.
     *
     * @param distance    the distance along the current contour to sample
     * @param position    if non-null, returns the sampled position
     * @param positionOff the starting index for position array
     * @param tangent     if non-null, returns the sampled tangent vector
     * @param tangentOff  the starting index for tangent array
     * @return success or not
     */
    @CheckReturnValue
    public boolean getPosTan(float distance,
            float @Nullable[] position, int positionOff,
            float @Nullable[] tangent, int tangentOff) {
        if (!hasContour()) {
            return false;
        }
        if (Float.isNaN(distance)) {
            return false;
        }

        float length = mContourLength;
        assert length > 0;
        distance = MathUtil.clamp(distance, 0, length);

        int segIndex = distanceToSegment(distance);
        float t = mTmp[0];
        if (!Float.isFinite(t)) {
            return false;
        }

        computePosAndTan(segIndex, t, position, positionOff, tangent, tangentOff);
        return true;
    }

    /**
     * Clamps <var>distance</var> between 0 and {@link #getContourLength()}, and then
     * computes the corresponding matrix (by calling {@link #getPosTan}).
     * Returns false if there is no contour, or a zero-length path was specified,
     * in which case <var>matrix</var> is unchanged.
     *
     * @param distance the distance along the current contour to sample
     * @param matrix   if non-null, returns the transformation
     * @param flags    what aspects should be returned in the matrix
     * @return success or not
     */
    @CheckReturnValue
    public boolean getMatrix(float distance,
                             @Nullable Matrix matrix,
                             @MatrixFlags int flags) {
        if (!getPosTan(distance,
                (flags & MATRIX_FLAG_GET_POSITION) != 0 ? mTmp : null, 0,
                (flags & MATRIX_FLAG_GET_TANGENT) != 0 ? mTmp : null, 2)) {
            return false;
        }

        if (matrix != null) {
            // add check for tangent vector normalization
            if ((flags & MATRIX_FLAG_GET_TANGENT) != 0 && Point.normalize(mTmp, 2)) {
                matrix.setSinCos(mTmp[3], mTmp[2]);
            } else {
                matrix.setIdentity();
            }
            if ((flags & MATRIX_FLAG_GET_POSITION) != 0) {
                matrix.postTranslate(mTmp[0], mTmp[1]);
            }
        }

        return true;
    }

    /**
     * Given a start and end distance, return in dst the intervening segment(s).
     * If the segment is zero-length, return false, else return true.
     * <var>startDistance</var> and <var>endDistance</var> are clamped between
     * 0 and {@link #getContourLength()}. If <var>startDistance</var> &gt;
     * <var>endDistance</var> then return false (and leave <var>dst</var> untouched).
     * Begin the segment with a moveTo if <var>startWithMoveTo</var> is true.
     *
     * @param startDistance   the start distance along the current contour
     * @param endDistance     the end distance along the current contour
     * @param dst             a path consumer that accepts path segments
     * @param startWithMoveTo true to add moveTo
     * @return success or not
     */
    @CheckReturnValue
    public boolean getSegment(float startDistance, float endDistance,
                              PathConsumer dst, boolean startWithMoveTo) {
        if (!hasContour()) {
            return false;
        }

        float length = mContourLength;
        assert length > 0;
        if (startDistance < 0) {
            startDistance = 0;
        }
        if (endDistance > length) {
            endDistance = length;
        }
        if (!(startDistance <= endDistance)) {   // catch NaN values as well
            return false;
        }
        assert !mSegments.isEmpty();

        int segIndex = distanceToSegment(startDistance);
        float startT = mTmp[0];
        if (!Float.isFinite(startT)) {
            return false;
        }

        final int endSegIndex = distanceToSegment(endDistance);
        final float endT = mTmp[0];
        if (!Float.isFinite(endT)) {
            return false;
        }

        assert segIndex <= endSegIndex;

        if (startWithMoveTo) {
            computePosAndTan(segIndex, startT, mTmp, 0, null, 0);
            dst.moveTo(mTmp[0], mTmp[1]);
        }

        final int[] segments = mSegments.elements();
        final int endCoordIndex = getSegmentCoordIndex(segments, endSegIndex);
        if (getSegmentCoordIndex(segments, segIndex) == endCoordIndex) {
            segmentTo(segIndex, startT, endT, dst);
        } else {
            do {
                segmentTo(segIndex, startT, 1, dst);
                int coordIndex = getSegmentCoordIndex(segments, segIndex);
                do {
                    ++segIndex;
                } while (getSegmentCoordIndex(segments, segIndex) == coordIndex);
                startT = 0;
            } while (getSegmentCoordIndex(segments, segIndex) < endCoordIndex);
            segmentTo(segIndex, 0, endT, dst);
        }

        return true;
    }

    /**
     * Returns the tolerance, this is affected by <var>resScale</var>.
     */
    public float getTolerance() {
        return mTolerance;
    }

    private boolean computeSegments() {
        float distance = 0;
        boolean hasClose = mForceClose;
        boolean hasMoveTo = false;

        mSegments.clear();
        var coords = mCoords;
        coords.clear();
        int coordIndex = -2;

        var iter = mIterator;
        byte verb;
        while ((verb = iter.next()) != PathIterator.VERB_DONE) {
            if (hasMoveTo && verb == PathIterator.VERB_MOVE) {
                break;
            }
            switch (verb) {
                case PathIterator.VERB_MOVE:
                    coordIndex += 2;
                    coords.add(iter.x0());
                    coords.add(iter.y0());
                    hasMoveTo = true;
                    break;

                case PathIterator.VERB_LINE: {
                    assert (hasMoveTo);
                    float prevD = distance;
                    distance = compute_line_segment(
                            iter.x0(), iter.y0(),
                            iter.x1(), iter.y1(),
                            distance,
                            coordIndex
                    );
                    if (distance > prevD) {
                        coords.add(iter.x1());
                        coords.add(iter.y1());
                        coordIndex += 2;
                    }
                } break;

                case PathIterator.VERB_QUAD: {
                    assert (hasMoveTo);
                    float prevD = distance;
                    distance = compute_quad_segments(
                            iter.x0(), iter.y0(),
                            iter.x1(), iter.y1(),
                            iter.x2(), iter.y2(),
                            distance,
                            0, MAX_T_VALUE,
                            coordIndex
                    );
                    if (distance > prevD) {
                        coords.add(iter.x1());
                        coords.add(iter.y1());
                        coords.add(iter.x2());
                        coords.add(iter.y2());
                        coordIndex += 4;
                    }
                } break;

                case PathIterator.VERB_CUBIC: {
                    assert (hasMoveTo);
                    float prevD = distance;
                    distance = compute_cubic_segments(
                            iter.x0(), iter.y0(),
                            iter.x1(), iter.y1(),
                            iter.x2(), iter.y2(),
                            iter.x3(), iter.y3(),
                            distance,
                            0, MAX_T_VALUE,
                            coordIndex
                    );
                    if (distance > prevD) {
                        coords.add(iter.x1());
                        coords.add(iter.y1());
                        coords.add(iter.x2());
                        coords.add(iter.y2());
                        coords.add(iter.x3());
                        coords.add(iter.y3());
                        coordIndex += 6;
                    }
                } break;

                case PathIterator.VERB_CLOSE:
                    hasClose = true;
                    break;
            }
        }

        if (!Float.isFinite(distance)) {
            return false;
        }
        if (mSegments.isEmpty()) {
            return false;
        }

        if (hasClose) {
            float prevD = distance;
            float firstX = coords.getFloat(0);
            float firstY = coords.getFloat(1);
            distance = compute_line_segment(
                    coords.getFloat(coordIndex), coords.getFloat(coordIndex | 1),
                    firstX, firstY,
                    distance,
                    coordIndex
            );
            if (distance > prevD) {
                coords.add(firstX);
                coords.add(firstY);
            }
        }

        mContourLength = distance;
        mContourClosed = hasClose;

        return true;
    }

    private float compute_line_segment(
            float x0, float y0,
            float x1, float y1,
            float distance,
            int coordIndex
    ) {
        float d = Point.distanceTo(x0, y0, x1, y1);
        float prevD = distance;
        distance += d;
        if (distance > prevD) {
            mSegments.add(Float.floatToIntBits(distance));
            mSegments.add(coordIndex);
            mSegments.add(MAX_T_VALUE | (SEGMENT_LINE << SEGMENT_TYPE_SHIFT));
        }
        return distance;
    }

    private static boolean large_t_span(int tSpan) {
        // 2^(30-10) precision
        return (tSpan >> 10) != 0;
    }

    // Return true if not close enough to a line
    private static boolean check_quad(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float tolerance
    ) {
        // diff = (a/4 + b/2 + c/4) - (a/2 + c/2)
        // diff = -a/4 + b/2 - c/4
        float dx = 0.5f * x1 -
                0.5f * (0.5f * (x0 + x2));
        float dy = 0.5f * y1 -
                0.5f * (0.5f * (y0 + y2));

        float dist = Math.max(Math.abs(dx), Math.abs(dy));
        return dist > tolerance;
    }

    private float compute_quad_segments(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float distance,
            int tMin, int tMax,
            int coordIndex
    ) {
        if (large_t_span(tMax - tMin) &&
                check_quad(x0, y0, x1, y1, x2, y2, mTolerance)) {
            // subdivide at middle
            int tMid = (tMin + tMax) >>> 1;

            float abx  = x0  * 0.5f + x1  * 0.5f;
            float aby  = y0  * 0.5f + y1  * 0.5f;
            float bcx  = x1  * 0.5f + x2  * 0.5f;
            float bcy  = y1  * 0.5f + y2  * 0.5f;
            float abcx = abx * 0.5f + bcx * 0.5f;
            float abcy = aby * 0.5f + bcy * 0.5f;

            distance = compute_quad_segments(
                    x0, y0,
                    abx, aby,
                    abcx, abcy,
                    distance,
                    tMin, tMid,
                    coordIndex
            );
            distance = compute_quad_segments(
                    abcx, abcy,
                    bcx, bcy,
                    x2, y2,
                    distance,
                    tMid, tMax,
                    coordIndex
            );
        } else {
            float d = Point.distanceTo(x0, y0, x2, y2);
            float prevD = distance;
            distance += d;
            if (distance > prevD) {
                mSegments.add(Float.floatToIntBits(distance));
                mSegments.add(coordIndex);
                mSegments.add(tMax | (SEGMENT_QUAD << SEGMENT_TYPE_SHIFT));
            }
        }
        return distance;
    }

    // Return true if not close enough to a line
    private static boolean check_cubic(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float tolerance
    ) {
        // fast approximation
        float x = MathUtil.lerp(x0, x3, 1f / 3);
        float y = MathUtil.lerp(y0, y3, 1f / 3);
        float dist = Math.max(Math.abs(x - x1), Math.abs(y - y1));
        if (dist > tolerance) {
            return true;
        }
        x = MathUtil.lerp(x0, x3, 2f / 3);
        y = MathUtil.lerp(y0, y3, 2f / 3);
        dist = Math.max(Math.abs(x - x2), Math.abs(y - y2));
        return dist > tolerance;
    }

    private float compute_cubic_segments(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float distance,
            int tMin, int tMax,
            int coordIndex
    ) {
        if (large_t_span(tMax - tMin) &&
                check_cubic(x0, y0, x1, y1, x2, y2, x3, y3, mTolerance)) {
            // subdivide at middle
            int tMid = (tMin + tMax) >>> 1;

            float abx   = x0   * 0.5f + x1   * 0.5f;
            float aby   = y0   * 0.5f + y1   * 0.5f;
            float bcx   = x1   * 0.5f + x2   * 0.5f;
            float bcy   = y1   * 0.5f + y2   * 0.5f;
            float cdx   = x2   * 0.5f + x3   * 0.5f;
            float cdy   = y2   * 0.5f + y3   * 0.5f;
            float abcx  = abx  * 0.5f + bcx  * 0.5f;
            float abcy  = aby  * 0.5f + bcy  * 0.5f;
            float bcdx  = bcx  * 0.5f + cdx  * 0.5f;
            float bcdy  = bcy  * 0.5f + cdy  * 0.5f;
            float abcdx = abcx * 0.5f + bcdx * 0.5f;
            float abcdy = abcy * 0.5f + bcdy * 0.5f;

            distance = compute_cubic_segments(
                    x0, y0,
                    abx, aby,
                    abcx, abcy,
                    abcdx, abcdy,
                    distance,
                    tMin, tMid,
                    coordIndex
            );
            distance = compute_cubic_segments(
                    abcdx, abcdy,
                    bcdx, bcdy,
                    cdx, cdy,
                    x3, y3,
                    distance,
                    tMid, tMax,
                    coordIndex
            );
        } else {
            float d = Point.distanceTo(x0, y0, x3, y3);
            float prevD = distance;
            distance += d;
            if (distance > prevD) {
                mSegments.add(Float.floatToIntBits(distance));
                mSegments.add(coordIndex);
                mSegments.add(tMax | (SEGMENT_CUBIC << SEGMENT_TYPE_SHIFT));
            }
        }
        return distance;
    }

    private static float getSegmentDistance(int[] segments, int segIndex) {
        int d = segments[segIndex * SEGMENT_COLUMNS + SEGMENT_DISTANCE];
        return Float.intBitsToFloat(d);
    }

    private static int getSegmentCoordIndex(int[] segments, int segIndex) {
        return segments[segIndex * SEGMENT_COLUMNS + SEGMENT_COORD_INDEX];
    }

    private static float getSegmentT(int[] segments, int segIndex) {
        int t = segments[segIndex * SEGMENT_COLUMNS + SEGMENT_T_AND_TYPE];
        return (t & SEGMENT_T_MASK) * (1.0f / MAX_T_VALUE);
    }

    private static int getSegmentType(int[] segments, int segIndex) {
        int t = segments[segIndex * SEGMENT_COLUMNS + SEGMENT_T_AND_TYPE];
        return t >>> SEGMENT_TYPE_SHIFT;
    }

    private void computePosAndTan(int segIndex,
                                  float t,
                                  float[] pos, int posOff,
                                  float[] tangent, int tangentOff
    ) {
        int ci = getSegmentCoordIndex(mSegments.elements(), segIndex);
        float[] pts = mCoords.elements();
        int segType = getSegmentType(mSegments.elements(), segIndex);
        switch (segType) {
            case SEGMENT_LINE -> {
                if (pos != null) {
                    // pos.x = mix(p0.x, p1.x, t)
                    // pos.y = mix(p0.y, p1.y, t)
                    pos[posOff]   = MathUtil.lerp(pts[ci]  , pts[ci+2], t);
                    pos[posOff+1] = MathUtil.lerp(pts[ci+1], pts[ci+3], t);
                }
                if (tangent != null) {
                    // tan.x = p1.x - p0.x
                    // tan.y = p1.y - p0.y
                    tangent[tangentOff]   = pts[ci+2] - pts[ci]  ;
                    tangent[tangentOff+1] = pts[ci+3] - pts[ci+1];
                }
            }
            case SEGMENT_QUAD -> {
                GeometryUtils.evalQuadAt(
                        pts, ci,
                        t,
                        pos, posOff,
                        tangent, tangentOff
                );
            }
            case SEGMENT_CUBIC -> {
                GeometryUtils.evalCubicAt(
                        pts, ci,
                        t,
                        pos, posOff,
                        tangent, tangentOff
                );
            }
            default -> {
                assert false;
            }
        }
    }

    // return seg index
    private int distanceToSegment(float distance) {
        assert distance >= 0 && distance <= mContourLength;

        int index = 0;
        int[] segments = mSegments.elements();
        if (!mSegments.isEmpty()) {
            assert mSegments.size() % SEGMENT_COLUMNS == 0;
            int low = 0, high = (mSegments.size() / SEGMENT_COLUMNS) - 1;

            while (low < high) {
                int mid = (low + high) >>> 1;
                if (getSegmentDistance(segments, mid) < distance) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }

            if (getSegmentDistance(segments, high) < distance) {
                index = high + 1;
            } else {
                index = high;
            }
        }

        float startT = 0;
        float startD = 0;
        if (index > 0) {
            startD = getSegmentDistance(segments, index - 1);
            if (getSegmentCoordIndex(segments, index) == getSegmentCoordIndex(segments, index - 1)) {
                startT = getSegmentT(segments, index - 1);
            }
        }

        assert (getSegmentT(segments, index) > startT);
        assert (distance >= startD);
        assert (getSegmentDistance(segments, index) > startD);

        mTmp[0] = startT + (getSegmentT(segments, index) - startT) *
                (distance - startD) /
                (getSegmentDistance(segments, index) - startD);
        return index;
    }

    private void segmentTo(int segIndex, float startT, float endT, PathConsumer dst) {
        assert (startT >= 0 && startT <= 1);
        assert (endT >= 0 && endT <= 1);
        assert (startT <= endT);

        if (startT == endT) {
            return;
        }

        int ci = getSegmentCoordIndex(mSegments.elements(), segIndex);
        float[] pts = mCoords.elements();
        int segType = getSegmentType(mSegments.elements(), segIndex);

        switch (segType) {
            case SEGMENT_LINE -> {
                if (endT == 1) {
                    dst.lineTo(pts[ci+2], pts[ci+3]);
                } else {
                    dst.lineTo(
                            MathUtil.lerp(pts[ci]  , pts[ci+2], endT),
                            MathUtil.lerp(pts[ci+1], pts[ci+3], endT)
                    );
                }
            }
            case SEGMENT_QUAD -> {
                if (startT == 0) {
                    if (endT == 1) {
                        dst.quadTo(pts, ci + 2);
                    } else {
                        GeometryUtils.chopQuadAt(pts, ci, mTmp, 0, endT);
                        dst.quadTo(mTmp, 2);
                    }
                } else {
                    GeometryUtils.chopQuadAt(pts, ci, mTmp, 0, startT);
                    if (endT == 1) {
                        dst.quadTo(mTmp, 6);
                    } else {
                        GeometryUtils.chopQuadAt(mTmp, 4, mTmp, 10, (endT - startT) / (1 - startT));
                        dst.quadTo(mTmp, 12);
                    }
                }
            }
            case SEGMENT_CUBIC -> {
                if (startT == 0) {
                    if (endT == 1) {
                        dst.cubicTo(pts, ci + 2);
                    } else {
                        GeometryUtils.chopCubicAt(pts, ci, mTmp, 0, endT);
                        dst.cubicTo(mTmp, 2);
                    }
                } else {
                    GeometryUtils.chopCubicAt(pts, ci, mTmp, 0, startT);
                    if (endT == 1) {
                        dst.cubicTo(mTmp, 8);
                    } else {
                        GeometryUtils.chopCubicAt(mTmp, 6, mTmp, 14, (endT - startT) / (1 - startT));
                        dst.cubicTo(mTmp, 16);
                    }
                }
            }
        }
    }
}
