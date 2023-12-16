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

import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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
 */
public class Path implements PathConsumer {

    @MagicConstant(intValues = {FILL_NON_ZERO, FILL_EVEN_ODD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FillRule {
    }

    /**
     * The fill rule constant for specifying a non-zero rule
     * for determining the interior of a path.<br>
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    public static final int FILL_NON_ZERO = PathIterator.FILL_NON_ZERO;
    /**
     * The fill rule constant for specifying an even-odd rule
     * for determining the interior of a path.<br>
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    public static final int FILL_EVEN_ODD = PathIterator.FILL_EVEN_ODD;

    /**
     * Primitive commands of path segments.
     */
    public static final byte
            VERB_MOVE = PathIterator.VERB_MOVE,   // returns 1 point
            VERB_LINE = PathIterator.VERB_LINE,   // returns 1 point
            VERB_QUAD = PathIterator.VERB_QUAD,   // returns 2 points
            VERB_CUBIC = PathIterator.VERB_CUBIC, // returns 3 points
            VERB_CLOSE = PathIterator.VERB_CLOSE; // returns 0 points

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
    private Ref mRef;

    private int mLastMoveToIndex;

    private byte mConvexity;
    private byte mFirstDirection;
    @FillRule
    private byte mFillRule;

    /**
     * Creates an empty Path with a default fill rule of {@link #FILL_NON_ZERO}.
     */
    public Path() {
        mRef = RefCnt.create(Ref.EMPTY);
        resetFields();
    }

    /**
     * Creates a copy of an existing Path object.
     * <p>
     * Internally, the two paths share reference values. The underlying
     * verb array, coordinate array and weights are copied when modified.
     */
    @SuppressWarnings("IncompleteCopyConstructor")
    public Path(@Nonnull Path other) {
        mRef = RefCnt.create(other.mRef);
        copyFields(other);
    }

    /**
     * Resets all fields other than {@link #mRef} to their initial 'empty' values.
     */
    private void resetFields() {
        mLastMoveToIndex = ~0;
        mFillRule = FILL_NON_ZERO;
        mConvexity = CONVEXITY_UNKNOWN;
        mFirstDirection = FIRST_DIRECTION_UNKNOWN;
    }

    private void copyFields(@Nonnull Path other) {
        mLastMoveToIndex = other.mLastMoveToIndex;
        mConvexity = other.mConvexity;
        mFirstDirection = other.mFirstDirection;
        mFillRule = other.mFillRule;
    }

    /**
     * Returns the rule used to fill path.
     *
     * @return current fill rule
     */
    @FillRule
    public int getFillRule() {
        return mFillRule;
    }

    /**
     * Sets the rule used to fill path. <var>rule</var> is either {@link #FILL_NON_ZERO}
     * or {@link #FILL_EVEN_ODD} .
     */
    public void setFillRule(@FillRule int rule) {
        if ((rule & ~1) != 0) {
            throw new IllegalArgumentException();
        }
        assert rule == FILL_NON_ZERO || rule == FILL_EVEN_ODD;
        mFillRule = (byte) rule;
    }

    /**
     * Creates a copy of an existing Path object.
     * <p>
     * Internally, the two paths share reference values. The underlying
     * verb array, coordinate array and weights are copied when modified.
     */
    public void set(@Nonnull Path other) {
        if (other != this) {
            mRef = RefCnt.create(mRef, other.mRef);
            copyFields(other);
        }
    }

    /**
     * Moves contents from other path into this path. This is equivalent to call
     * {@code this.set(other)} and then {@code other.recycle()}.
     */
    public void move(@Nonnull Path other) {
        if (other != this) {
            mRef = RefCnt.move(mRef, other.mRef);
            other.mRef = RefCnt.create(Ref.EMPTY);
            copyFields(other);
            other.resetFields();
        }
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #FILL_NON_ZERO}.
     * <p>
     * Preserves internal storage if it's unique, otherwise discards.
     */
    public void reset() {
        if (mRef.unique()) {
            mRef.reset();
        } else {
            mRef = RefCnt.create(mRef, Ref.EMPTY);
        }
        resetFields();
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #FILL_NON_ZERO}.
     * <p>
     * Preserves internal storage if it's unique, otherwise allocates new
     * storage with the same size.
     */
    public void clear() {
        if (mRef.unique()) {
            mRef.reset();
        } else {
            int verbSize = mRef.mVerbSize;
            int coordSize = mRef.mCoordSize;
            mRef = RefCnt.move(mRef, new Ref(verbSize, coordSize));
        }
        resetFields();
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #FILL_NON_ZERO}.
     * <p>
     * This explicitly discards the internal storage, it is recommended to
     * call when the path object will be no longer used.
     */
    public void recycle() {
        mRef = RefCnt.create(mRef, Ref.EMPTY);
        resetFields();
    }

    /**
     * Trims the internal storage to the current size. This operation can
     * minimize memory usage, see {@link #estimatedByteSize()}.
     */
    public void trimToSize() {
        if (mRef.unique()) {
            mRef.trimToSize();
        } else {
            mRef = RefCnt.move(mRef, new Ref(mRef));
        }
    }

    /**
     * Returns true if path has no point and verb. {@link #reset()},
     * {@link #clear()} and {@link #recycle()} make path empty.
     *
     * @return true if the path contains no verb
     */
    public boolean isEmpty() {
        assert (mRef.mVerbSize == 0) == (mRef.mCoordSize == 0);
        return mRef.mVerbSize == 0;
    }

    /**
     * Returns false for any coordinate value of infinity or NaN.
     *
     * @return true if all point coordinates are finite
     */
    public boolean isFinite() {
        return mRef.isFinite();
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
        mLastMoveToIndex = mRef.mCoordSize;
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
        int n = mRef.mCoordSize;
        if (n != 0) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
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
        int n = mRef.mCoordSize;
        if (n != 0) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
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
        int n = mRef.mCoordSize;
        if (n != 0) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
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
        int n = mRef.mCoordSize;
        if (n != 0) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
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
    public void closePath() {
        int count = countVerbs();
        if (count != 0) {
            switch (mRef.mVerbs[count - 1]) {
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
    public void pathDone() {
    }

    /**
     * Returns the number of verbs added to path.
     *
     * @return size of verb list
     */
    public int countVerbs() {
        return mRef.mVerbSize;
    }

    /**
     * Returns the number of points (x,y pairs) in path.
     *
     * @return size of point list
     */
    public int countPoints() {
        assert mRef.mCoordSize % 2 == 0;
        return mRef.mCoordSize >> 1;
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
     * @return bounds of all points, read-only
     * @see #isFinite()
     */
    @Nonnull
    public Rect2fc getBounds() {
        return mRef.getBounds();
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
        mRef.updateBounds();
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
        return mRef.mSegmentMask;
    }

    public PathIterator iterator() {
        return this.new Iterator();
    }

    private class Iterator implements PathIterator {

        private final int count = countVerbs();
        private int verbPos;
        private int coordPos;

        @Override
        public int next(float[] coords, int offset) {
            if (verbPos == count) {
                return VERB_DONE;
            }
            byte verb = mRef.mVerbs[verbPos++];
            switch (verb) {
                case VERB_MOVE -> {
                    if (verbPos == count) {
                        return VERB_DONE;
                    }
                    if (coords != null) {
                        coords[offset] = mRef.mCoords[coordPos];
                        coords[offset + 1] = mRef.mCoords[coordPos + 1];
                    }
                    coordPos += 2;
                }
                case VERB_LINE -> {
                    if (coords != null) {
                        coords[offset] = mRef.mCoords[coordPos];
                        coords[offset + 1] = mRef.mCoords[coordPos + 1];
                    }
                    coordPos += 2;
                }
                case VERB_QUAD -> {
                    if (coords != null) {
                        System.arraycopy(
                                mRef.mCoords, coordPos,
                                coords, offset, 4
                        );
                    }
                    coordPos += 4;
                }
                case VERB_CUBIC -> {
                    if (coords != null) {
                        System.arraycopy(
                                mRef.mCoords, coordPos,
                                coords, offset, 6
                        );
                    }
                    coordPos += 6;
                }
            }
            return verb;
        }
    }

    /**
     * Iterates the Path and feeds the given consumer.
     */
    public void forEach(@Nonnull PathConsumer action) {
        int n = countVerbs();
        if (n != 0) {
            byte[] vs = mRef.mVerbs;
            float[] cs = mRef.mCoords;
            int vi = 0;
            int ci = 0;
            ITR:
            do {
                switch (vs[vi++]) {
                    case PathIterator.VERB_MOVE -> {
                        if (vi == n) {
                            break ITR;
                        }
                        action.moveTo(
                                cs[ci++], cs[ci++]
                        );
                    }
                    case PathIterator.VERB_LINE -> action.lineTo(
                            cs[ci++], cs[ci++]
                    );
                    case PathIterator.VERB_QUAD -> {
                        action.quadTo(
                                cs[ci], cs[ci + 1],
                                cs[ci + 2], cs[ci + 3]
                        );
                        ci += 4;
                    }
                    case PathIterator.VERB_CUBIC -> {
                        action.cubicTo(
                                cs[ci], cs[ci + 1],
                                cs[ci + 2], cs[ci + 3],
                                cs[ci + 4], cs[ci + 5]
                        );
                        ci += 6;
                    }
                    case PathIterator.VERB_CLOSE -> action.closePath();
                }
            } while (vi < n);
        }
        action.pathDone();
    }

    /**
     * Returns the estimated byte size of path object in memory.
     * This method does not take into account whether internal storage is shared or not.
     */
    public long estimatedByteSize() {
        long size = 24;
        size += mRef.estimatedByteSize();
        return size;
    }

    @Override
    public int hashCode() {
        int hash = mRef.hashCode();
        hash = 31 * hash + mFillRule;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Path other) {
            return mFillRule == other.mFillRule && mRef.equals(other.mRef);
        }
        return false;
    }

    // ignore the last point of the contour
    // there must be moveTo() for the contour
    void reversePop(@Nonnull PathConsumer out) {
        assert mRef != null;
        byte[] vs = mRef.mVerbs;
        float[] cs = mRef.mCoords;
        int vi = mRef.mVerbSize;
        int ci = mRef.mCoordSize - 2;
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

    private Ref editor() {
        if (!mRef.unique()) {
            mRef = RefCnt.move(mRef, new Ref(mRef));
        }
        mRef.mBounds.set(0, 0, -1, -1);
        return mRef;
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

    @Nonnull
    private static byte[] growVerbs(@Nonnull byte[] old, int minGrow) {
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

    @Nonnull
    private static float[] growCoords(@Nonnull float[] old, int minGrow) {
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

    /**
     * This class holds {@link Path} data.
     * <br>This class can be shared/accessed between multiple threads.
     * <p>
     * The reference count is only used to check whether these internal buffers
     * are uniquely referenced by a Path object, otherwise when any of these
     * shared Path objects are modified, these buffers will be copied.
     *
     * @see #unique()
     */
    static final class Ref implements RefCounted {

        private static final VarHandle USAGE_CNT;

        static {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                USAGE_CNT = lookup.findVarHandle(Ref.class, "mUsageCnt", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        static final byte[] EMPTY_VERBS = {};
        static final float[] EMPTY_COORDS = {};

        static final Ref EMPTY;

        static {
            EMPTY = new Ref();
            EMPTY.updateBounds();
        }

        transient volatile int mUsageCnt = 1;

        // unsorted and finite = dirty
        final Rect2f mBounds = new Rect2f(0, 0, -1, -1);

        byte[] mVerbs;
        float[] mCoords; // x0 y0 x1 y1 x2 y2 ...

        int mVerbSize;
        int mCoordSize;

        @SegmentMask
        byte mSegmentMask;

        Ref() {
            this(EMPTY_VERBS, EMPTY_COORDS);
        }

        Ref(int verbSize, int coordSize) {
            assert coordSize % 2 == 0;
            if (verbSize > 0) {
                assert coordSize > 0;
                mVerbs = new byte[verbSize];
                mCoords = new float[coordSize];
            } else {
                mVerbs = EMPTY_VERBS;
                mCoords = EMPTY_COORDS;
            }
        }

        Ref(byte[] verbs, float[] coords) {
            assert coords.length % 2 == 0;
            mVerbs = verbs;
            mCoords = coords;
        }

        @SuppressWarnings("IncompleteCopyConstructor")
        Ref(@Nonnull Ref other) {
            mBounds.set(other.mBounds);
            // trim
            if (other.mVerbSize > 0) {
                assert other.mCoordSize > 0;
                mVerbs = Arrays.copyOf(other.mVerbs, other.mVerbSize);
                mCoords = Arrays.copyOf(other.mCoords, other.mCoordSize);
            } else {
                mVerbs = EMPTY_VERBS;
                mCoords = EMPTY_COORDS;
            }
            mVerbSize = other.mVerbSize;
            mCoordSize = other.mCoordSize;
            mSegmentMask = other.mSegmentMask;
        }

        Ref(@Nonnull Ref other, int incVerbs, int incCoords) {
            assert incVerbs >= 0 && incCoords >= 0;
            assert incCoords % 2 == 0;
            mVerbs = new byte[other.mVerbSize + incVerbs];
            mCoords = new float[other.mCoordSize + incCoords];
            mBounds.set(other.mBounds);
            System.arraycopy(other.mVerbs, 0, mVerbs, 0, other.mVerbSize);
            System.arraycopy(other.mCoords, 0, mCoords, 0, other.mCoordSize);
            mVerbSize = other.mVerbSize;
            mCoordSize = other.mCoordSize;
            mSegmentMask = other.mSegmentMask;
        }

        boolean unique() {
            return (int) USAGE_CNT.getAcquire(this) == 1
                    && this != EMPTY; // the EMPTY's usage cnt is wild
        }

        public void ref() {
            USAGE_CNT.getAndAddAcquire(this, 1);
        }

        public void unref() {
            USAGE_CNT.getAndAdd(this, -1);
        }

        void reset() {
            mBounds.set(0, 0, -1, -1);
            mSegmentMask = 0;
            mVerbSize = 0;
            mCoordSize = 0;
        }

        void reserve(int incVerbs, int incCoords) {
            assert incVerbs >= 0 && incCoords >= 0;
            assert incCoords % 2 == 0;
            if (mVerbSize > mVerbs.length - incVerbs) { // prevent overflow
                mVerbs = growVerbs(mVerbs, incVerbs);
            }
            if (mCoordSize > mCoords.length - incCoords) { // prevent overflow
                mCoords = growCoords(mCoords, incCoords);
            }
        }

        void trimToSize() {
            if (mVerbSize > 0) {
                assert mCoordSize > 0;
                if (mVerbSize < mVerbs.length) {
                    mVerbs = Arrays.copyOf(mVerbs, mVerbSize);
                }
                if (mCoordSize < mCoords.length) {
                    mCoords = Arrays.copyOf(mCoords, mCoordSize);
                }
            } else {
                mVerbs = EMPTY_VERBS;
                mCoords = EMPTY_COORDS;
            }
        }

        Ref addVerb(byte verb) {
            int coords = switch (verb) {
                case VERB_MOVE -> 2;
                case VERB_LINE -> {
                    mSegmentMask |= SEGMENT_LINE;
                    yield 2;
                }
                case VERB_QUAD -> {
                    mSegmentMask |= SEGMENT_QUAD;
                    yield 4;
                }
                case VERB_CUBIC -> {
                    mSegmentMask |= SEGMENT_CUBIC;
                    yield 6;
                }
                default -> 0;
            };
            reserve(1, coords);
            mVerbs[mVerbSize++] = verb;
            return this;
        }

        Ref addPoint(float x, float y) {
            mCoords[mCoordSize++] = x;
            mCoords[mCoordSize++] = y;
            return this;
        }

        void updateBounds() {
            if (!mBounds.isSorted() && mBounds.isFinite()) {
                assert mCoordSize % 2 == 0;
                mBounds.setBoundsNoCheck(mCoords, 0, mCoordSize >> 1);
            }
        }

        boolean isFinite() {
            updateBounds();
            return mBounds.isFinite();
        }

        Rect2fc getBounds() {
            if (isFinite()) {
                return mBounds;
            }
            return Rect2f.empty();
        }

        long estimatedByteSize() {
            if (this == EMPTY) {
                return 0;
            }
            long size = 16 + 24 + 16 + 16;
            if (mVerbs != EMPTY_VERBS) {
                size += 16 + MathUtil.align8(mVerbs.length);
            }
            if (mCoords != EMPTY_COORDS) {
                assert mCoords.length % 2 == 0;
                size += 16 + ((long) mCoords.length << 2);
            }
            return size;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            for (int i = 0, count = mVerbSize; i < count; i++) {
                hash = 11 * hash + mVerbs[i];
            }
            for (int i = 0, count = mCoordSize; i < count; i++) {
                hash = 11 * hash + Float.floatToIntBits(mCoords[i]);
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Ref other)) {
                return false;
            }
            // quick reject
            if (mSegmentMask != other.mSegmentMask) {
                return false;
            }
            if (mCoordSize != other.mCoordSize) {
                return false;
            }
            if (!Arrays.equals(
                    mVerbs, 0, mVerbSize,
                    other.mVerbs, 0, other.mVerbSize)) {
                return false;
            }
            // use IEEE comparison rather than memory comparison
            for (int i = 0, count = mCoordSize; i < count; i++) {
                if (mCoords[i] != other.mCoords[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
