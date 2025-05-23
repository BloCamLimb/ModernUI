/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * The {@link Path} object contains mutable path elements.
 * <p>
 * Path may be empty, or contain one or more verbs that outline a figure.
 * Path always starts with a move verb to a Cartesian coordinate, and may be
 * followed by additional verbs that add lines or curves. Adding a close verb
 * makes the geometry into a continuous loop, a closed contour. Path may
 * contain any number of contours, each beginning with a move verb.
 * <p>
 * Path contours may contain only a move verb, or may also contain lines,
 * quadratic Béziers, and cubic Béziers. Path contours may be open or closed.
 * <p>
 * When used to draw a filled area, Path describes whether the fill is inside or
 * outside the geometry. Path also describes the winding rule used to fill
 * overlapping contours.
 * <p>
 * Note: Path lazily computes metrics likes bounds and convexity. Call
 * {@link #updateBoundsCache()} to make path thread safe.
 * <p>
 * Note: Path also implements AWT Shape for convenience, but only a
 * few methods are actually implemented.
 */
public class Path implements Shape, java.awt.Shape, PathConsumer {

    /**
     * The fill rule constant for specifying an even-odd rule
     * for determining the interior of a path.<br>
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;
    /**
     * The fill rule constant for specifying a non-zero rule
     * for determining the interior of a path.<br>
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    /**
     * Primitive commands of path segments.
     */
    public static final byte
            VERB_MOVE = PathIterator.SEG_MOVETO,   // returns 1 point
            VERB_LINE = PathIterator.SEG_LINETO,   // returns 1 point
            VERB_QUAD = PathIterator.SEG_QUADTO,   // returns 2 points
            VERB_CUBIC = PathIterator.SEG_CUBICTO, // returns 3 points
            VERB_CLOSE = PathIterator.SEG_CLOSE; // returns 0 points
    @ApiStatus.Internal
    public static final byte
            VERB_DONE = VERB_CLOSE + 1;

    /**
     * Clockwise direction for adding closed contours, assumes the origin is top left, y-down.
     */
    public static final int DIRECTION_CW = 0;
    /**
     * Counter-clockwise direction for adding closed contours, assumes the origin is top left, y-down.
     */
    public static final int DIRECTION_CCW = 1;

    /**
     * Segment constants correspond to each drawing verb type in path; for
     * instance, if path contains only lines, only the Line bit is set.
     *
     * @see #getSegmentMask()
     */
    @MagicConstant(flags = {SEGMENT_LINE, SEGMENT_QUAD, SEGMENT_CUBIC})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SegmentMask {
    }

    public static final int
            SEGMENT_LINE = 1,
            SEGMENT_QUAD = 1 << 1,
            SEGMENT_CUBIC = 1 << 3;

    private static final byte CONVEXITY_CONVEX = 0;
    private static final byte CONVEXITY_CONCAVE = 1;
    private static final byte CONVEXITY_UNKNOWN = 2;

    private static final byte FIRST_DIRECTION_CW = DIRECTION_CW;
    private static final byte FIRST_DIRECTION_CCW = DIRECTION_CCW;
    private static final byte FIRST_DIRECTION_UNKNOWN = 2;

    public static final int APPROXIMATE_ARC_WITH_CUBICS = 0;
    public static final int APPROXIMATE_CONIC_WITH_QUADS = 1;

    @SharedPtr
    PathRef mPathRef;

    private int mLastMoveToIndex;

    private byte mConvexity;
    private byte mFirstDirection;
    private byte mWindingRule;

    /**
     * Creates an empty Path with a default fill rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        mPathRef = RefCnt.create(PathRef.EMPTY);
        resetFields();
    }

    /**
     * Creates a copy of an existing Path object.
     * <p>
     * Internally, the two paths share reference values. The underlying
     * verb array, coordinate array and weights are copied when modified.
     */
    @SuppressWarnings("IncompleteCopyConstructor")
    public Path(@NonNull Path other) {
        mPathRef = RefCnt.create(other.mPathRef);
        copyFields(other);
    }

    /**
     * Resets all fields other than {@link #mPathRef} to their initial 'empty' values.
     */
    private void resetFields() {
        mLastMoveToIndex = ~0;
        mWindingRule = WIND_NON_ZERO;
        mConvexity = CONVEXITY_UNKNOWN;
        mFirstDirection = FIRST_DIRECTION_UNKNOWN;
    }

    private void copyFields(@NonNull Path other) {
        mLastMoveToIndex = other.mLastMoveToIndex;
        mConvexity = other.mConvexity;
        mFirstDirection = other.mFirstDirection;
        mWindingRule = other.mWindingRule;
    }

    /**
     * Returns the rule used to fill path.
     *
     * @return current fill rule
     */
    @Override
    public int getWindingRule() {
        return mWindingRule;
    }

    /**
     * Sets the rule used to fill path. <var>rule</var> is either {@link #WIND_NON_ZERO}
     * or {@link #WIND_EVEN_ODD} .
     */
    public void setWindingRule(int rule) {
        if ((rule & ~1) != 0) {
            throw new IllegalArgumentException();
        }
        assert rule == WIND_NON_ZERO || rule == WIND_EVEN_ODD;
        mWindingRule = (byte) rule;
    }

    /**
     * Creates a copy of an existing Path object.
     * <p>
     * Internally, the two paths share reference values. The underlying
     * verb array, coordinate array and weights are copied when modified.
     */
    public void set(@NonNull Path other) {
        if (other != this) {
            mPathRef = RefCnt.create(mPathRef, other.mPathRef);
            copyFields(other);
        }
    }

    /**
     * Moves contents from other path into this path. This is equivalent to call
     * {@code this.set(other)} and then {@code other.recycle()}.
     */
    public void move(@NonNull Path other) {
        if (other != this) {
            mPathRef = RefCnt.move(mPathRef, other.mPathRef);
            other.mPathRef = RefCnt.create(PathRef.EMPTY);
            copyFields(other);
            other.resetFields();
        }
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #WIND_NON_ZERO}.
     * <p>
     * Preserves internal storage if it's unique, otherwise discards.
     */
    public void reset() {
        if (mPathRef.unique()) {
            mPathRef.reset();
        } else {
            mPathRef = RefCnt.create(mPathRef, PathRef.EMPTY);
        }
        resetFields();
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #WIND_NON_ZERO}.
     * <p>
     * Preserves internal storage if it's unique, otherwise allocates new
     * storage with the same size.
     */
    public void clear() {
        if (mPathRef.unique()) {
            mPathRef.reset();
        } else {
            int verbSize = mPathRef.mVerbSize;
            int coordSize = mPathRef.mCoordSize;
            mPathRef = RefCnt.move(mPathRef, new PathRef(verbSize, coordSize));
        }
        resetFields();
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #WIND_NON_ZERO}.
     * <p>
     * This explicitly discards the internal storage, it is recommended to
     * call when the path object will be no longer used.
     */
    public void release() {
        mPathRef = RefCnt.create(mPathRef, PathRef.EMPTY);
        resetFields();
    }

    /**
     * Trims the internal storage to the current size. This operation can
     * minimize memory usage, see {@link #estimatedByteSize()}.
     */
    public void trimToSize() {
        if (mPathRef.unique()) {
            mPathRef.trimToSize();
        } else {
            mPathRef = RefCnt.move(mPathRef, new PathRef(mPathRef));
        }
    }

    /**
     * Returns true if path has no point and verb. {@link #reset()},
     * {@link #clear()} and {@link #release()} make path empty.
     *
     * @return true if the path contains no verb
     */
    public boolean isEmpty() {
        assert (mPathRef.mVerbSize == 0) == (mPathRef.mCoordSize == 0);
        return mPathRef.mVerbSize == 0;
    }

    /**
     * Returns false for any coordinate value of infinity or NaN.
     *
     * @return true if all point coordinates are finite
     */
    public boolean isFinite() {
        return mPathRef.isFinite();
    }

    private void dirtyAfterEdit() {
        mConvexity = CONVEXITY_UNKNOWN;
        mFirstDirection = FIRST_DIRECTION_UNKNOWN;
    }

    /**
     * Adds a point to the path by moving to the specified point {@code (x,y)}.
     * A new contour begins at {@code (x,y)}.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    @Override
    public void moveTo(float x, float y) {
        mLastMoveToIndex = mPathRef.mCoordSize;
        editor().addVerb(VERB_MOVE)
                .addPoint(x, y);
        dirtyAfterEdit();
    }

    /**
     * Relative version of "move to".
     *
     * @param dx offset from last point to contour start on x-axis
     * @param dy offset from last point to contour start on y-axis
     * @throws IllegalStateException Path is empty
     */
    public void moveToRel(float dx, float dy) {
        int n = mPathRef.mCoordSize;
        if (n != 0) {
            float px = mPathRef.mCoords[n - 2];
            float py = mPathRef.mCoords[n - 1];
            moveTo(px + dx, py + dy);
        } else {
            throw new IllegalStateException("No first point");
        }
    }

    /**
     * Adds a point to the path by drawing a straight line from the
     * current point to the new specified point {@code (x,y)}.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @throws IllegalStateException No contour
     */
    @Override
    public void lineTo(float x, float y) {
        if (mLastMoveToIndex >= 0) {
            editor().addVerb(VERB_LINE)
                    .addPoint(x, y);
            dirtyAfterEdit();
        } else {
            throw new IllegalStateException("No initial point");
        }
    }

    /**
     * Relative version of "line to".
     * <p>
     * Adds a line from the last point to the specified vector (dx, dy).
     *
     * @param dx the offset from last point to line end on x-axis
     * @param dy the offset from last point to line end on y-axis
     */
    public void lineToRel(float dx, float dy) {
        int n = mPathRef.mCoordSize;
        if (n != 0) {
            float px = mPathRef.mCoords[n - 2];
            float py = mPathRef.mCoords[n - 1];
            lineTo(px + dx, py + dy);
        } else {
            throw new IllegalStateException("No first point");
        }
    }

    /**
     * Adds a curved segment, defined by two new points, to the path by
     * drawing a quadratic Bézier curve that intersects both the current
     * point and the specified point {@code (x2,y2)}, using the specified
     * point {@code (x1,y1)} as a quadratic control point.
     *
     * @param x1 the X coordinate of the quadratic control point
     * @param y1 the Y coordinate of the quadratic control point
     * @param x2 the X coordinate of the final end point
     * @param y2 the Y coordinate of the final end point
     */
    @Override
    public void quadTo(float x1, float y1,
                       float x2, float y2) {
        if (mLastMoveToIndex >= 0) {
            editor().addVerb(VERB_QUAD)
                    .addPoint(x1, y1)
                    .addPoint(x2, y2);
            dirtyAfterEdit();
        } else {
            throw new IllegalStateException("No initial point");
        }
    }

    /**
     * Relative version of "quad to".
     * <p>
     * Adds quad from last point towards vector (dx1, dy1), to vector (dx2, dy2).
     *
     * @param dx1 offset from last point to quad control on x-axis
     * @param dy1 offset from last point to quad control on y-axis
     * @param dx2 offset from last point to quad end on x-axis
     * @param dy2 offset from last point to quad end on y-axis
     */
    public void quadToRel(float dx1, float dy1,
                          float dx2, float dy2) {
        int n = mPathRef.mCoordSize;
        if (n != 0) {
            float px = mPathRef.mCoords[n - 2];
            float py = mPathRef.mCoords[n - 1];
            quadTo(px + dx1, py + dy1, px + dx2, py + dy2);
        } else {
            throw new IllegalStateException("No first point");
        }
    }

    /**
     * Adds a curved segment, defined by three new points, to the path by
     * drawing a cubic Bézier curve that intersects both the current
     * point and the specified point {@code (x3,y3)}, using the specified
     * points {@code (x1,y1)} and {@code (x2,y2)} as cubic control points.
     *
     * @param x1 the X coordinate of the first cubic control point
     * @param y1 the Y coordinate of the first cubic control point
     * @param x2 the X coordinate of the second cubic control point
     * @param y2 the Y coordinate of the second cubic control point
     * @param x3 the X coordinate of the final end point
     * @param y3 the Y coordinate of the final end point
     */
    @Override
    public void cubicTo(float x1, float y1,
                        float x2, float y2,
                        float x3, float y3) {
        if (mLastMoveToIndex >= 0) {
            editor().addVerb(VERB_CUBIC)
                    .addPoint(x1, y1)
                    .addPoint(x2, y2)
                    .addPoint(x3, y3);
            dirtyAfterEdit();
        } else {
            throw new IllegalStateException("No initial point");
        }
    }

    /**
     * Relative version of "cubic to".
     * <p>
     * Adds cubic from last point towards vector (dx1, dy1), vector (dx2, dy2),
     * to vector (dx3, dy3).
     *
     * @param dx1 offset from last point to first cubic control on x-axis
     * @param dy1 offset from last point to first cubic control on y-axis
     * @param dx2 offset from last point to second cubic control on x-axis
     * @param dy2 offset from last point to second cubic control on y-axis
     * @param dx3 offset from last point to cubic end on x-axis
     * @param dy3 offset from last point to cubic end on y-axis
     */
    public void cubicToRel(float dx1, float dy1,
                           float dx2, float dy2,
                           float dx3, float dy3) {
        int n = mPathRef.mCoordSize;
        if (n != 0) {
            float px = mPathRef.mCoords[n - 2];
            float py = mPathRef.mCoords[n - 1];
            cubicTo(px + dx1, py + dy1, px + dx2, py + dy2, px + dx3, py + dy3);
        } else {
            throw new IllegalStateException("No first point");
        }
    }

    /**
     * Closes the current contour by drawing a straight line back to
     * the point of the last {@link #moveTo}.  If the path is already
     * closed then this method has no effect.
     */
    @Override
    public void close() {
        int count = countVerbs();
        if (count != 0) {
            switch (mPathRef.mVerbs[count - 1]) {
                case VERB_MOVE:
                case VERB_LINE:
                case VERB_QUAD:
                case VERB_CUBIC: {
                    editor().addVerb(VERB_CLOSE);
                    break;
                }
                case VERB_CLOSE:
                    break;
                default:
                    throw new AssertionError();
            }
        }
        mLastMoveToIndex ^= ~mLastMoveToIndex >> (Integer.SIZE - 1);
    }

    @Override
    public void done() {
    }

    /**
     * Transforms verb array, Point array, and weight by matrix.
     * transform may change verbs and increase their number.
     * Path is replaced by transformed data.
     *
     * @param matrix Matrix to apply to Path
     */
    public void transform(@NonNull Matrixc matrix) {
        transform(matrix, this);
    }

    /**
     * Transforms verb array, point array, and weight by matrix.
     * transform may change verbs and increase their number.
     * Transformed Path replaces dst; if dst is null, original data
     * is replaced.
     *
     * @param matrix Matrix to apply to Path
     * @param dst    overwritten, transformed copy of Path; may be null
     */
    public void transform(@NonNull Matrixc matrix, @Nullable Path dst) {
        if (matrix.isIdentity()) {
            if (dst != null && dst != this) {
                dst.set(this);
            }
            return;
        }

        if (dst == null) {
            dst = this;
        }

        if (matrix.hasPerspective()) {
            //TODO
        } else {
            mPathRef.createTransformedCopy(matrix, dst);

            if (this != dst) {
                dst.mLastMoveToIndex = mLastMoveToIndex;
                dst.mWindingRule = mWindingRule;
            }
        }
    }

    /**
     * Returns the number of verbs added to path.
     *
     * @return size of verb list
     */
    public int countVerbs() {
        return mPathRef.mVerbSize;
    }

    /**
     * Returns the number of points (x,y pairs) in path.
     *
     * @return size of point list
     */
    public int countPoints() {
        assert mPathRef.mCoordSize % 2 == 0;
        return mPathRef.mCoordSize >> 1;
    }

    @Override
    public java.awt.@NonNull Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    /**
     * Returns minimum and maximum axes values of coordinates. Returns empty
     * if path contains no points or is not finite.
     * <p>
     * Returned bounds includes all points added to path, including points
     * associated with MOVE that define empty contours.
     * <p>
     * This method returns a cached result; it is recalculated only after
     * this path is altered.
     *
     * @return bounds of all points
     * @see #isFinite()
     */
    @Override
    public @NonNull Rectangle2D getBounds2D() {
        var bounds = mPathRef.getBounds();
        return new Rectangle2D.Float(bounds.x(), bounds.y(),
                bounds.width(), bounds.height());
    }

    /**
     * Helper method to {@link #getBounds()}, stores the result to dst.
     */
    @Override
    public void getBounds(@NonNull Rect2f dst) {
        mPathRef.getBounds().store(dst);
    }

    @Override
    public boolean contains(double x, double y) {
        //TODO
        return false;
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        //TODO
        return false;
    }

    @Override
    public boolean contains(Rectangle2D r) {
        //TODO
        return false;
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return false;
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return false;
    }

    /**
     * Updates internal bounds so that subsequent calls to {@link #getBounds()}
     * are instantaneous. Unaltered copies of path may also access cached bounds
     * through {@link #getBounds()}.
     * <p>
     * For now, identical to calling {@link #getBounds()} and ignoring the returned
     * value.
     * <p>
     * Call to prepare path subsequently drawn from multiple threads, to avoid
     * a race condition where each draw separately computes the bounds.
     */
    public void updateBoundsCache() {
        mPathRef.updateBounds();
    }

    /**
     * Returns a mask, where each set bit corresponds to a Segment constant
     * if path contains one or more verbs of that type.
     * <p>
     * This method returns a cached result; it is very fast.
     *
     * @return Segment bits or zero
     */
    @SegmentMask
    public int getSegmentMask() {
        return mPathRef.mSegmentMask;
    }

    @Override
    public @NonNull PathIterator getPathIterator() {
        return this.new Iterator();
    }

    @Override
    public @NonNull PathIterator getPathIterator(@Nullable AffineTransform at) {
        return at != null ? this.new TxIterator(at) : this.new Iterator();
    }

    @Override
    public @NonNull PathIterator getPathIterator(@Nullable AffineTransform at,
                                                 double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    private class Iterator implements PathIterator {

        private final int count = countVerbs();
        private int verbPos;
        private int coordPos;

        @Override
        public int getWindingRule() {
            return mWindingRule;
        }

        @Override
        public boolean isDone() {
            return verbPos == count;
        }

        @Override
        public void next() {
            byte verb = mPathRef.mVerbs[verbPos++];
            coordPos += switch (verb) {
                case VERB_MOVE,VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
                default -> 0;
            };
        }

        @Override
        public int currentSegment(float[] coords) {
            byte verb = mPathRef.mVerbs[verbPos];
            int numCoords = switch (verb) {
                case VERB_MOVE,VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
                default -> 0;
            };
            if (numCoords > 0) {
                System.arraycopy(mPathRef.mCoords, coordPos,
                        coords, 0, numCoords);
            }
            return verb;
        }

        @Override
        public int currentSegment(double[] coords) {
            byte verb = mPathRef.mVerbs[verbPos];
            int numCoords = switch (verb) {
                case VERB_MOVE,VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
                default -> 0;
            };
            if (numCoords > 0) {
                for (int i = 0; i < numCoords; i++) {
                    coords[i] = mPathRef.mCoords[coordPos + i];
                }
            }
            return verb;
        }
    }

    private class TxIterator implements PathIterator {

        private final AffineTransform affine;
        private final int count = countVerbs();
        private int verbPos;
        private int coordPos;

        private TxIterator(AffineTransform affine) {
            this.affine = affine;
        }

        @Override
        public int getWindingRule() {
            return mWindingRule;
        }

        @Override
        public boolean isDone() {
            return verbPos == count;
        }

        @Override
        public void next() {
            byte verb = mPathRef.mVerbs[verbPos++];
            coordPos += switch (verb) {
                case VERB_MOVE,VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
                default -> 0;
            };
        }

        @Override
        public int currentSegment(float[] coords) {
            byte verb = mPathRef.mVerbs[verbPos++];
            int numCoords = switch (verb) {
                case VERB_MOVE,VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
                default -> 0;
            };
            if (numCoords > 0) {
                affine.transform(mPathRef.mCoords, coordPos,
                        coords, 0, numCoords / 2);
            }
            return verb;
        }

        @Override
        public int currentSegment(double[] coords) {
            byte verb = mPathRef.mVerbs[verbPos++];
            int numCoords = switch (verb) {
                case VERB_MOVE,VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
                default -> 0;
            };
            if (numCoords > 0) {
                affine.transform(mPathRef.mCoords, coordPos,
                        coords, 0, numCoords / 2);
            }
            return verb;
        }
    }

    /**
     * Iterates the Path and feeds the given consumer.
     */
    @Override
    public void forEach(@NonNull PathConsumer action) {
        int n = countVerbs();
        if (n != 0) {
            byte[] vs = mPathRef.mVerbs;
            float[] cs = mPathRef.mCoords;
            int vi = 0;
            int ci = 0;
            ITR:
            do {
                switch (vs[vi++]) {
                    case VERB_MOVE -> {
                        if (vi == n) {
                            break ITR;
                        }
                        action.moveTo(
                                cs[ci++], cs[ci++]
                        );
                    }
                    case VERB_LINE -> action.lineTo(
                            cs[ci++], cs[ci++]
                    );
                    case VERB_QUAD -> {
                        action.quadTo(cs, ci);
                        ci += 4;
                    }
                    case VERB_CUBIC -> {
                        action.cubicTo(cs, ci);
                        ci += 6;
                    }
                    case VERB_CLOSE -> action.close();
                }
            } while (vi < n);
        }
        action.done();
    }

    /**
     * Low-level access to path elements.
     */
    @ApiStatus.Internal
    public class RawIterator {

        private final int count = countVerbs();
        private int verbPos;
        private int coordPos;
        private int coordOff;
        private int coordInc;

        public boolean hasNext() {
            return verbPos < count;
        }

        public byte next() {
            if (verbPos == count) {
                return VERB_DONE;
            }
            byte verb = mPathRef.mVerbs[verbPos++];
            coordPos += coordInc;
            switch (verb) {
                case VERB_MOVE, VERB_LINE -> coordInc = 2;
                case VERB_QUAD -> coordInc = 4;
                case VERB_CUBIC -> coordInc = 6;
                case VERB_CLOSE -> coordInc = 0;
            }
            // -2 is used to peek the current point
            coordOff = verb == VERB_MOVE ? 0 : -2;
            return verb;
        }

        //TODO use arraycopy
        public float x0() {
            return mPathRef.mCoords[coordPos + coordOff];
        }

        public float y0() {
            return mPathRef.mCoords[coordPos + coordOff + 1];
        }

        public float x1() {
            return mPathRef.mCoords[coordPos + coordOff + 2];
        }

        public float y1() {
            return mPathRef.mCoords[coordPos + coordOff + 3];
        }

        public float x2() {
            return mPathRef.mCoords[coordPos + coordOff + 4];
        }

        public float y2() {
            return mPathRef.mCoords[coordPos + coordOff + 5];
        }

        public float x3() {
            return mPathRef.mCoords[coordPos + coordOff + 6];
        }

        public float y3() {
            return mPathRef.mCoords[coordPos + coordOff + 7];
        }
    }

    /**
     * Returns the estimated byte size of path object in memory.
     * This method does not take into account whether internal storage is shared or not.
     */
    public long estimatedByteSize() {
        long size = 32;
        size += mPathRef.estimatedByteSize();
        return size;
    }

    @Override
    public int hashCode() {
        int hash = mPathRef.hashCode();
        hash = 31 * hash + mWindingRule;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Path other) {
            return mWindingRule == other.mWindingRule && mPathRef.equals(other.mPathRef);
        }
        return false;
    }

    // ignore the last point of the contour
    // there must be moveTo() for the contour
    void reversePop(@NonNull PathConsumer out, boolean addMoveTo) {
        assert mPathRef != null;
        byte[] vs = mPathRef.mVerbs;
        float[] cs = mPathRef.mCoords;
        int vi = mPathRef.mVerbSize;
        int ci = mPathRef.mCoordSize - 2;
        if (addMoveTo) {
            if (ci >= 0) {
                out.moveTo(cs[ci], cs[ci + 1]);
            } else {
                out.moveTo(0, 0);
            }
        }
        ITR:
        while (vi != 0) {
            switch (vs[--vi]) {
                case VERB_MOVE -> {
                    assert vi == 0 && ci == 0;
                    break ITR;
                }
                case VERB_LINE -> {
                    out.lineTo(
                            cs[ci - 2], cs[ci - 1]
                    );
                    ci -= 2;
                }
                case VERB_QUAD -> {
                    out.quadTo(
                            cs[ci - 2], cs[ci - 1],
                            cs[ci - 4], cs[ci - 3]
                    );
                    ci -= 4;
                }
                case VERB_CUBIC -> {
                    out.cubicTo(
                            cs[ci - 2], cs[ci - 1],
                            cs[ci - 4], cs[ci - 3],
                            cs[ci - 6], cs[ci - 5]
                    );
                    ci -= 6;
                }
                default -> {
                    assert false;
                }
            }
        }
        clear();
    }

    private PathRef editor() {
        if (!mPathRef.unique()) {
            mPathRef = RefCnt.move(mPathRef, new PathRef(mPathRef));
        }
        mPathRef.dirtyBounds();
        return mPathRef;
    }

    // the state of the convex computation
    static class ConvexState {

    }

    int computeConvexity() {
        if (!isFinite()) {
            return CONVEXITY_CONCAVE;
        }
        return 0;
    }


    static byte @NonNull[] growVerbs(byte @NonNull[] old, int minGrow) {
        final int oldCap = old.length, grow;
        if (oldCap < 10) {
            grow = 10 - oldCap;
        } else if (oldCap > 500) {
            grow = Math.max(250, oldCap >> 3); // 1.125x
        } else {
            grow = oldCap >> 1; // 1.5x
        }
        int newCap = oldCap + Math.max(grow, minGrow); // may overflow
        if (newCap < 0) {
            newCap = oldCap + minGrow; // may overflow
            if (newCap < 0) {
                throw new IllegalStateException("Path is too big " + oldCap + " + " + minGrow);
            }
            newCap = Integer.MAX_VALUE;
        }
        return Arrays.copyOf(old, newCap);
    }


    static float @NonNull[] growCoords(float @NonNull[] old, int minGrow) {
        final int oldCap = old.length, grow;
        if (oldCap < 20) {
            grow = 20 - oldCap;
        } else if (oldCap > 1000) {
            // align down to 2
            grow = Math.max(500, (oldCap >> 4) << 1); // 1.125x
        } else {
            // align down to 2
            grow = (oldCap >> 2) << 1; // 1.5x
        }
        assert oldCap % 2 == 0 && minGrow % 2 == 0;
        int newCap = oldCap + Math.max(grow, minGrow); // may overflow
        if (newCap < 0) {
            newCap = oldCap + minGrow; // may overflow
            if (newCap < 0) {
                throw new IllegalStateException("Path is too big " + oldCap + " + " + minGrow);
            }
            // align down to 2
            newCap = Integer.MAX_VALUE - 1;
        }
        return Arrays.copyOf(old, newCap);
    }

}
