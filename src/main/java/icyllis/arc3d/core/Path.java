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

import javax.annotation.Nonnull;
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
 * Path::updateBoundsCache to make Path thread safe.
 */
public class Path implements PathConsumer {

    /**
     * The winding rule constant for specifying an even-odd rule
     * for determining the interior of a path.<br>
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;
    /**
     * The winding rule constant for specifying a non-zero rule
     * for determining the interior of a path.<br>
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    /**
     * Clockwise direction for adding closed contours, assumes the origin is top left, y-down.
     */
    public static final int DIRECTION_CW = 0;
    /**
     * Counter-clockwise direction for adding closed contours, assumes the origin is top left, y-down.
     */
    public static final int DIRECTION_CCW = 1;
    private static final byte DIRECTION_UNKNOWN = 2;

    public static final byte
            VERB_MOVE = PathIterator.VERB_MOVE,
            VERB_LINE = PathIterator.VERB_LINE,
            VERB_QUAD = PathIterator.VERB_QUAD,
            VERB_CUBIC = PathIterator.VERB_CUBIC,
            VERB_CLOSE = PathIterator.VERB_CLOSE;

    private static final byte CONVEXITY_CONVEX = 0;
    private static final byte CONVEXITY_CONCAVE = 1;
    private static final byte CONVEXITY_UNKNOWN = 2;

    public static final int APPROXIMATE_ARC_WITH_CUBICS = 0;
    public static final int APPROXIMATE_CONIC_WITH_QUADS = 1;

    @SharedPtr
    private Ref mRef;

    private int mLastMoveToIndex;

    private byte mConvexity;
    private byte mFirstDirection;
    private byte mWindingRule;

    /**
     * Creates an empty Path with a default winding rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        mRef = null;
        resetFields();
    }

    /**
     * Creates a copy of an existing Path object.
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
        mWindingRule = WIND_NON_ZERO;
        mConvexity = CONVEXITY_UNKNOWN;
        mFirstDirection = DIRECTION_UNKNOWN;
    }

    private void copyFields(@Nonnull Path other) {
        mLastMoveToIndex = other.mLastMoveToIndex;
        mConvexity = other.mConvexity;
        mFirstDirection = other.mFirstDirection;
        mWindingRule = other.mWindingRule;
    }

    /**
     * Resets the path to empty.
     * <p>
     * If internal storage is shared, unref it.
     * Otherwise, preserves internal storage.
     */
    public void reset() {
        if (mRef != null) {
            if (mRef.unique()) {
                mRef.mVerbSize = 0;
                mRef.mCoordSize = 0;
            } else {
                mRef = RefCnt.move(mRef);
            }
        }
        resetFields();
    }

    /**
     * Resets the path to empty.
     * <p>
     * If internal storage is shared, create new internal storage.
     * Otherwise, preserves internal storage.
     */
    public void clear() {
        if (mRef != null) {
            if (mRef.unique()) {
                mRef.mVerbSize = 0;
                mRef.mCoordSize = 0;
            } else {
                int verbSize = mRef.mVerbSize;
                int coordSize = mRef.mCoordSize;
                mRef = RefCnt.move(mRef, new Ref(verbSize, coordSize));
            }
        }
        resetFields();
    }

    /**
     * Resets the path to empty and removes internal storage.
     */
    public void recycle() {
        mRef = RefCnt.move(mRef);
        resetFields();
    }

    /**
     * Trims the internal storage to current size.
     */
    public void trimToSize() {
        if (mRef != null) {
            if (mRef.unique()) {
                mRef.trimToSize();
            } else {
                mRef = RefCnt.move(mRef, new Ref(mRef));
            }
        }
    }

    private void dirtyAfterEdit() {
        mConvexity = CONVEXITY_UNKNOWN;
        mFirstDirection = DIRECTION_UNKNOWN;
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
        mLastMoveToIndex = countCoords();
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
        int n = countCoords();
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
     * Adds a line from the last point to the specified vector (dx, dy).
     *
     * @param dx the offset from last point to line end on x-axis
     * @param dy the offset from last point to line end on y-axis
     */
    public void lineToRel(float dx, float dy) {
        int n = countCoords();
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
     */
    public void quadToRel(float dx1, float dy1,
                          float dx2, float dy2) {
        int n = countCoords();
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
     */
    public void cubicToRel(float dx1, float dy1,
                           float dx2, float dy2,
                           float dx3, float dy3) {
        int n = countCoords();
        if (n != 0) {
            float px = mRef.mCoords[n - 2];
            float py = mRef.mCoords[n - 1];
            cubicTo(px + dx1, py + dy1, px + dx2, py + dy2, px + dx3, py + dy3);
        } else {
            throw new IllegalStateException("No first point");
        }
    }

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
        return mRef != null ? mRef.mVerbSize : 0;
    }

    /**
     * Returns the number of coordinates in path. This is always an even number.
     *
     * @return size of coord list
     */
    public int countCoords() {
        return mRef != null ? mRef.mCoordSize : 0;
    }

    /**
     * Returns minimum and maximum axes values of coordinates. Returns empty
     * if path contains no points.
     * <p>
     * Returned bounds includes all points added to path, including points
     * associated with MOVE that define empty contours.
     * <p>
     * The return value is cached and recalculated only after path is changed.
     *
     * @return reference to bounds of all points, read-only
     */
    @Nonnull
    public Rect2fc getBounds() {
        return mRef != null ? mRef.getBounds() : Rect2f.empty();
    }

    public PathIterator getPathIterator() {
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
     * Returns the approximate byte size of path in memory.
     */
    public long getMemorySize() {
        long size = 24;
        if (mRef != null) {
            size += mRef.getMemorySize();
        }
        return size;
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
        if (mRef == null) {
            mRef = new Ref();
        } else {
            if (!mRef.unique()) {
                mRef = RefCnt.move(mRef, new Ref(mRef));
            }
            mRef.mBounds.set(0, 0, -1, -1);
        }
        return mRef;
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

        transient volatile int mUsageCnt = 1;

        // unsorted = dirty
        final Rect2f mBounds = new Rect2f(0, 0, -1, -1);

        byte[] mVerbs;
        float[] mCoords; // x0 y0 x1 y1 x2 y2 ...

        int mVerbSize;
        int mCoordSize;

        Ref() {
            this(EMPTY_VERBS, EMPTY_COORDS);
        }

        Ref(int verbSize, int coordSize) {
            assert coordSize % 2 == 0;
            mVerbs = new byte[verbSize];
            mCoords = new float[coordSize];
        }

        Ref(byte[] verbs, float[] coords) {
            assert coords.length % 2 == 0;
            mVerbs = verbs;
            mCoords = coords;
        }

        @SuppressWarnings("IncompleteCopyConstructor")
        Ref(@Nonnull Ref other) {
            mBounds.set(other.mBounds);
            mVerbs = Arrays.copyOf(other.mVerbs, other.mVerbSize);
            mCoords = Arrays.copyOf(other.mCoords, other.mCoordSize);
            mVerbSize = other.mVerbSize;
            mCoordSize = other.mCoordSize;
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
        }

        boolean unique() {
            return (int) USAGE_CNT.getAcquire(this) == 1;
        }

        public void ref() {
            var refCnt = (int) USAGE_CNT.getAndAddAcquire(this, 1);
            assert refCnt > 0;
        }

        public void unref() {
            var refCnt = (int) USAGE_CNT.getAndAdd(this, -1);
            assert refCnt > 0;
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
            if (mVerbSize < mVerbs.length) {
                mVerbs = Arrays.copyOf(mVerbs, mVerbSize);
            }
            if (mCoordSize < mCoords.length) {
                mCoords = Arrays.copyOf(mCoords, mCoordSize);
            }
        }

        Ref addVerb(byte verb) {
            int coords = switch (verb) {
                case VERB_MOVE, VERB_LINE -> 2;
                case VERB_QUAD -> 4;
                case VERB_CUBIC -> 6;
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

        Rect2fc getBounds() {
            if (!mBounds.isSorted()) {
                mBounds.setBounds(mCoords, 0, mCoordSize >> 1);
            }
            return mBounds;
        }

        long getMemorySize() {
            long size = 16 + 24 + 16 + 16;
            size += 16 + MathUtil.align8(mVerbs.length);
            assert mCoords.length % 2 == 0;
            size += 16 + ((long) mCoords.length << 2);
            return size;
        }
    }
}
