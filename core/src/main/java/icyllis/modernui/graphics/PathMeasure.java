/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.Objects;

/**
 * {@link PathMeasure} provides measurements on segments of path contours
 * (lines, quadratic curves, cubic curves), such as determining the length of the path,
 * and/or finding the position and tangent along it.
 * <p>
 * A {@link PathMeasure} object can be reused for measuring different {@link Path}
 * objects, by calling {@link #reset(Path, boolean, float)}.
 */
public class PathMeasure extends icyllis.arc3d.core.PathMeasure {

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
    public PathMeasure(@Nullable Path path, boolean forceClose) {
        super(path, forceClose);
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
    public PathMeasure(@Nullable Path path, boolean forceClose, float resScale) {
        super(path, forceClose, resScale);
    }

    /**
     * Resets this {@link PathMeasure} object to its initial state, just like calling
     * {@link #PathMeasure()} or {@link #reset(Path, boolean, float)} with an empty path.
     * This method releases the internal reference to the original path.
     */
    public void reset() {
        super.reset();
    }

    /**
     * This method assumes that <var>resScale</var> is 1.
     *
     * @see #reset(Path, boolean, float)
     */
    public boolean reset(@Nullable Path path, boolean forceClose) {
        return super.reset(path, forceClose);
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
    public boolean reset(@Nullable Path path, boolean forceClose, float resScale) {
        return super.reset(path, forceClose, resScale);
    }

    /**
     * Move to the next contour in the path and compute contour segments.
     * Return true if one exists, or false if we're done with the path.
     * If this method returns false, then {@link #reset()} is called.
     *
     * @see #hasContour()
     */
    public boolean nextContour() {
        return super.nextContour();
    }

    /**
     * Returns the length of the current contour.
     */
    public float getLength() {
        return super.getContourLength();
    }

    /**
     * Returns true if the current contour is closed.
     */
    public boolean isClosed() {
        return super.isContourClosed();
    }

    /**
     * Clamps <var>distance</var> between 0 and {@link #getContourLength()}, and then
     * computes the corresponding position and tangent vector (un-normalized).
     * Returns false if there is no contour, or a zero-length path was specified,
     * in which case <var>position</var> and <var>tangent</var> are unchanged.
     *
     * @param distance the distance along the current contour to sample
     * @param position if non-null, returns the sampled position
     * @param tangent  if non-null, returns the sampled tangent vector
     * @return success or not
     */
    @CheckReturnValue
    public boolean getPosTan(float distance,
                             @Nullable float[] position,
                             @Nullable float[] tangent) {
        return super.getPosTan(distance, position, 0, tangent, 0);
    }

    /**
     * Clamps <var>distance</var> between 0 and {@link #getContourLength()}, and then
     * computes the corresponding position and tangent vector (un-normalized).
     * Returns false if there is no contour, or a zero-length path was specified,
     * in which case <var>position</var> and <var>tangent</var> are unchanged.
     *
     * @param distance the distance along the current contour to sample
     * @param position if non-null, returns the sampled position
     * @param tangent  if non-null, returns the sampled tangent vector
     * @return success or not
     */
    @CheckReturnValue
    public boolean getPosTan(float distance,
                             @Nullable PointF position,
                             @Nullable PointF tangent) {
        boolean result = super.getPosTan(distance,
                position != null ? mTmp : null, 0,
                tangent != null ? mTmp : null, 2);
        if (result) {
            if (position != null) {
                position.set(mTmp[0], mTmp[1]);
            }
            if (tangent != null) {
                tangent.set(mTmp[2], mTmp[3]);
            }
        }
        return result;
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
        return super.getMatrix(distance, matrix, flags);
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
     * @param dst             a path that accepts path segments
     * @param startWithMoveTo true to add moveTo
     * @return success or not
     */
    @CheckReturnValue
    public boolean getSegment(float startDistance, float endDistance,
                              @NonNull Path dst, boolean startWithMoveTo) {
        Objects.requireNonNull(dst);
        return super.getSegment(startDistance, endDistance, dst, startWithMoveTo);
    }
}
