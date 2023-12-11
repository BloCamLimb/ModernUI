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
                mRef.mNumVerbs = 0;
                mRef.mNumCoords = 0;
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
                mRef.mNumVerbs = 0;
                mRef.mNumCoords = 0;
            } else {
                int numVerbs = mRef.mNumVerbs;
                int numCoords = mRef.mNumCoords;
                mRef = RefCnt.move(mRef, new Ref(numVerbs, numCoords));
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

    /**
     * Adds a point to the path by moving to the specified point {@code (x,y)}.
     * A new contour begins at {@code (x,y)}.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    @Override
    public void moveTo(float x, float y) {
        mLastMoveToIndex = numCoords();
        editor(1, 2)
                .addVerb(VERB_MOVE)
                .addPoint(x, y);
    }

    /**
     * Relative version of "move to".
     *
     * @param dx offset from last point to contour start on x-axis
     * @param dy offset from last point to contour start on y-axis
     * @throws IllegalStateException Path is empty
     */
    public void moveToRel(float dx, float dy) {
        final float px, py;
        final int n = numCoords();
        if (n != 0) {
            px = mRef.mCoords[n - 2];
            py = mRef.mCoords[n - 1];
        } else {
            throw new IllegalStateException("No first point");
        }
        moveTo(px + dx, py + dy);
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
            editor(1, 2)
                    .addVerb(VERB_LINE)
                    .addPoint(x, y);
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
        int n = numCoords();
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
            editor(1, 4)
                    .addVerb(VERB_QUAD)
                    .addPoint(x1, y1)
                    .addPoint(x2, y2);
        } else {
            throw new IllegalStateException("No initial point");
        }
    }

    /**
     * Relative version of "quad to".
     */
    public void quadToRel(float dx1, float dy1,
                          float dx2, float dy2) {
        int n = numCoords();
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
            editor(1, 6)
                    .addVerb(VERB_CUBIC)
                    .addPoint(x1, y1)
                    .addPoint(x2, y2)
                    .addPoint(x3, y3);
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
        int n = numCoords();
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
        int count = numVerbs();
        if (count != 0) {
            switch (mRef.mVerbs[count - 1]) {
                case VERB_MOVE:
                case VERB_LINE:
                case VERB_QUAD:
                case VERB_CUBIC: {
                    editor(1, 0)
                            .addVerb(VERB_CLOSE);
                    break;
                }
                case VERB_CLOSE:
                    break;
                default:
                    throw new AssertionError();
            }
        }
        mLastMoveToIndex = ~mLastMoveToIndex;
    }

    @Override
    public void pathDone() {
    }

    public int numVerbs() {
        return mRef != null ? mRef.mNumVerbs : 0;
    }

    public int numCoords() {
        return mRef != null ? mRef.mNumCoords : 0;
    }

    public PathIterator getPathIterator() {
        return this.new Iterator();
    }

    private class Iterator implements PathIterator {

        private final int numVerbs = numVerbs();
        private int verbPos;
        private int coordPos;

        @Override
        public int next(float[] coords, int offset) {
            if (verbPos == numVerbs) {
                return VERB_DONE;
            }
            byte verb = mRef.mVerbs[verbPos++];
            switch (verb) {
                case VERB_MOVE -> {
                    if (verbPos == numVerbs) {
                        return VERB_DONE;
                    }
                    coords[offset] = mRef.mCoords[coordPos++];
                    coords[offset + 1] = mRef.mCoords[coordPos++];
                }
                case VERB_LINE -> {
                    coords[offset] = mRef.mCoords[coordPos++];
                    coords[offset + 1] = mRef.mCoords[coordPos++];
                }
                case VERB_QUAD -> {
                    System.arraycopy(
                            mRef.mCoords, coordPos,
                            coords, offset, 4
                    );
                    coordPos += 4;
                }
                case VERB_CUBIC -> {
                    System.arraycopy(
                            mRef.mCoords, coordPos,
                            coords, offset, 6
                    );
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
        int n = numVerbs();
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

    // ignore the last point of the contour
    // there must be moveTo() for the contour
    void reversePop(@Nonnull PathConsumer out) {
        assert mRef != null;
        byte[] vs = mRef.mVerbs;
        float[] cs = mRef.mCoords;
        int vi = mRef.mNumVerbs;
        int ci = mRef.mNumCoords - 2;
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

    private Ref editor(int incVerbs, int incCoords) {
        assert incVerbs >= 0 && incCoords >= 0;
        if (mRef == null) {
            mRef = new Ref(incVerbs, incCoords);
        } else {
            if (mRef.unique()) {
                mRef.reserve(incVerbs, incCoords);
            } else {
                mRef = RefCnt.move(mRef, new Ref(mRef, incVerbs, incCoords));
            }
        }
        return mRef;
    }

    @Nonnull
    private static byte[] growVerbs(@Nonnull byte[] old, int minGrow) {
        final int cap = old.length, grow;
        if (cap < 10) {
            grow = 10;
        } else if (cap > 500) {
            grow = Math.max(250, cap >> 3);
        } else {
            grow = cap >> 1;
        }
        return Arrays.copyOf(old, cap + Math.max(grow, minGrow));
    }

    @Nonnull
    private static float[] growCoords(@Nonnull float[] old, int minGrow) {
        final int cap = old.length, grow;
        if (cap < 20) {
            grow = 20;
        } else if (cap > 1000) {
            grow = Math.max(500, cap >> 3);
        } else {
            grow = cap >> 1;
        }
        return Arrays.copyOf(old, cap + Math.max(grow, minGrow));
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

        byte[] mVerbs;
        float[] mCoords; // x0 y0 x1 y1 x2 y2 ...

        int mNumVerbs;
        int mNumCoords;

        Ref() {
            this(EMPTY_VERBS, EMPTY_COORDS);
        }

        Ref(int numVerbs, int numCoords) {
            this(new byte[numVerbs], new float[numCoords]);
        }

        Ref(byte[] verbs, float[] coords) {
            mVerbs = verbs;
            mCoords = coords;
        }

        @SuppressWarnings("IncompleteCopyConstructor")
        Ref(@Nonnull Ref other) {
            mVerbs = Arrays.copyOf(other.mVerbs, other.mNumVerbs);
            mCoords = Arrays.copyOf(other.mCoords, other.mNumCoords);
            mNumVerbs = other.mNumVerbs;
            mNumCoords = other.mNumCoords;
        }

        Ref(@Nonnull Ref other, int incVerbs, int incCoords) {
            this(other.mNumVerbs + incVerbs, other.mNumCoords + incCoords);
            System.arraycopy(other.mVerbs, 0, mVerbs, 0, other.mNumVerbs);
            System.arraycopy(other.mCoords, 0, mCoords, 0, other.mNumCoords);
            mNumVerbs = other.mNumVerbs;
            mNumCoords = other.mNumCoords;
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
            if (mNumVerbs + incVerbs > mVerbs.length) {
                mVerbs = growVerbs(mVerbs, incVerbs);
            }
            if (mNumCoords + incCoords > mCoords.length) {
                mCoords = growCoords(mCoords, incCoords);
            }
        }

        void trimToSize() {
            if (mNumVerbs < mVerbs.length) {
                mVerbs = Arrays.copyOf(mVerbs, mNumVerbs);
            }
            if (mNumCoords < mCoords.length) {
                mCoords = Arrays.copyOf(mCoords, mNumCoords);
            }
        }

        Ref addVerb(byte verb) {
            mVerbs[mNumVerbs++] = verb;
            return this;
        }

        Ref addPoint(float x, float y) {
            mCoords[mNumCoords++] = x;
            mCoords[mNumCoords++] = y;
            return this;
        }
    }
}
