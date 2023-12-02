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

    public static final byte
            VERB_MOVETO = PathIterator.VERB_MOVETO,
            VERB_LINETO = PathIterator.VERB_LINETO,
            VERB_QUADTO = PathIterator.VERB_QUADTO,
            VERB_CUBICTO = PathIterator.VERB_CUBICTO,
            VERB_CLOSE = PathIterator.VERB_CLOSE;

    private static final byte[] EMPTY_VERBS = {};
    private static final float[] EMPTY_COORDS = {};

    byte[] mVerbs;
    float[] mCoords; // x0 y0 x1 y1 x2 y2 ...

    int mNumVerbs;
    int mNumCoords;

    private int mWindingRule;

    private boolean mHasInitialPoint;

    /**
     * Creates an empty Path with a default winding rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        mVerbs = EMPTY_VERBS;
        mCoords = EMPTY_COORDS;
        mWindingRule = WIND_NON_ZERO;
    }

    /**
     * Creates a deep copy of an existing Path object.
     */
    @SuppressWarnings("IncompleteCopyConstructor")
    public Path(@Nonnull Path other) {
        // trim internal arrays
        mNumVerbs = other.mNumVerbs;
        mVerbs = Arrays.copyOf(other.mVerbs, other.mNumVerbs);
        mNumCoords = other.mNumCoords;
        mCoords = Arrays.copyOf(other.mCoords, other.mNumCoords);
        mWindingRule = other.mWindingRule;
        mHasInitialPoint = other.mHasInitialPoint;
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
        editor(1, 2)
                .addVerb(VERB_MOVETO)
                .addPoint(x, y);
        mHasInitialPoint = true;
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
        final int n = mNumCoords;
        if (n != 0) {
            px = mCoords[n - 2];
            py = mCoords[n - 1];
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
        if (mHasInitialPoint) {
            editor(1, 2)
                    .addVerb(VERB_LINETO)
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
        int n = mNumCoords;
        if (n != 0) {
            float px = mCoords[n - 2];
            float py = mCoords[n - 1];
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
        if (mHasInitialPoint) {
            editor(1, 4)
                    .addVerb(VERB_QUADTO)
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
        int n = mNumCoords;
        if (n != 0) {
            float px = mCoords[n - 2];
            float py = mCoords[n - 1];
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
        if (mHasInitialPoint) {
            editor(1, 6)
                    .addVerb(VERB_CUBICTO)
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
        int n = mNumCoords;
        if (n != 0) {
            float px = mCoords[n - 2];
            float py = mCoords[n - 1];
            cubicTo(px + dx1, py + dy1, px + dx2, py + dy2, px + dx3, py + dy3);
        } else {
            throw new IllegalStateException("No first point");
        }
    }

    @Override
    public void closePath() {
        int count = mNumVerbs;
        if (count != 0) {
            switch (mVerbs[count - 1]) {
                case VERB_MOVETO:
                case VERB_LINETO:
                case VERB_QUADTO:
                case VERB_CUBICTO: {
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
        mHasInitialPoint = false;
    }

    @Override
    public void pathDone() {
    }

    public PathIterator getPathIterator() {
        return this.new Iterator();
    }

    public class Iterator implements PathIterator {

        private int mVerbPos;
        private int mCoordPos;

        @Override
        public int next(float[] coords) {
            if (mVerbPos == mNumVerbs) {
                return VERB_DONE;
            }
            byte verb = mVerbs[mVerbPos++];
            switch (verb) {
                case VERB_MOVETO -> {
                    if (mVerbPos == mNumVerbs) {
                        return VERB_DONE;
                    }
                    coords[0] = mCoords[mCoordPos++];
                    coords[1] = mCoords[mCoordPos++];
                }
                case VERB_LINETO -> {
                    coords[0] = mCoords[mCoordPos++];
                    coords[1] = mCoords[mCoordPos++];
                }
                case VERB_QUADTO -> {
                    coords[0] = mCoords[mCoordPos++];
                    coords[1] = mCoords[mCoordPos++];
                    coords[2] = mCoords[mCoordPos++];
                    coords[3] = mCoords[mCoordPos++];
                }
                case VERB_CUBICTO -> {
                    System.arraycopy(
                            mCoords, mCoordPos,
                            coords, 0, 6
                    );
                    mCoordPos += 6;
                }
            }
            return verb;
        }
    }

    /**
     * Iterates the Path and feeds the given consumer.
     */
    public void forEach(@Nonnull PathConsumer action) {
        byte[] vs = mVerbs;
        float[] cs = mCoords;
        int vi = 0;
        int ci = 0;
        int n = mNumVerbs;
        ITR:
        while (vi < n) {
            switch (vs[vi++]) {
                case PathIterator.VERB_MOVETO -> {
                    if (vi == n) {
                        break ITR;
                    }
                    action.moveTo(
                            cs[ci++], cs[ci++]
                    );
                }
                case PathIterator.VERB_LINETO -> action.lineTo(
                        cs[ci++], cs[ci++]
                );
                case PathIterator.VERB_QUADTO -> {
                    action.quadTo(
                            cs[ci], cs[ci + 1],
                            cs[ci + 2], cs[ci + 3]
                    );
                    ci += 4;
                }
                case PathIterator.VERB_CUBICTO -> {
                    action.cubicTo(
                            cs[ci], cs[ci + 1],
                            cs[ci + 2], cs[ci + 3],
                            cs[ci + 4], cs[ci + 5]
                    );
                    ci += 6;
                }
                case PathIterator.VERB_CLOSE -> action.closePath();
            }
        }
        action.pathDone();
    }

    void reversePop(@Nonnull PathConsumer out) {
        byte[] vs = mVerbs;
        float[] cs = mCoords;
        int vi = mNumVerbs;
        int ci = mNumCoords - 2;
        ITR:
        while (vi != 0) {
            switch (vs[--vi]) {
                case VERB_MOVETO -> {
                    break ITR;
                }
                case VERB_LINETO -> {
                    ci -= 2;
                    out.lineTo(
                            cs[ci], cs[ci + 1]
                    );
                }
                case VERB_QUADTO -> {
                    ci -= 4;
                    out.quadTo(
                            cs[ci], cs[ci + 1],
                            cs[ci + 2], cs[ci + 3]
                    );
                }
                case VERB_CUBICTO -> {
                    ci -= 6;
                    out.cubicTo(
                            cs[ci], cs[ci + 1],
                            cs[ci + 2], cs[ci + 3],
                            cs[ci + 4], cs[ci + 5]
                    );
                }
            }
        }
        mNumVerbs = 0;
        mNumCoords = 0;
    }

    private Path editor(int incVerbs, int incCoords) {
        assert incVerbs >= 0 && incCoords >= 0;
        if (mNumVerbs + incVerbs > mVerbs.length) {
            mVerbs = growVerbs(mVerbs, incVerbs);
        }
        if (mNumCoords + incCoords > mCoords.length) {
            mCoords = growCoords(mCoords, incCoords);
        }
        return this;
    }

    Path addVerb(byte verb) {
        mVerbs[mNumVerbs++] = verb;
        return this;
    }

    Path addPoint(float x, float y) {
        mCoords[mNumCoords++] = x;
        mCoords[mNumCoords++] = y;
        return this;
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
}
