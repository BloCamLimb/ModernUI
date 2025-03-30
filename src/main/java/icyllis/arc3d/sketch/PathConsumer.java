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

/**
 * Accepts {@link Path} elements.
 */
public interface PathConsumer {

    /**
     * Accepts a point to the path consumer by moving to the specified
     * point {@code (x,y)}.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    void moveTo(float x, float y);

    /**
     * Accepts a point to the path by drawing a straight line from the
     * current point to the new specified point {@code (x,y)}.
     *
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    void lineTo(float x, float y);

    /**
     * Accepts a curved segment, defined by two new points, to the path by
     * drawing a quadratic Bézier curve that intersects both the current
     * point and the specified point {@code (x2,y2)}, using the specified
     * point {@code (x1,y1)} as a quadratic control point.
     *
     * @param x1 the X coordinate of the quadratic control point
     * @param y1 the Y coordinate of the quadratic control point
     * @param x2 the X coordinate of the final end point
     * @param y2 the Y coordinate of the final end point
     */
    void quadTo(float x1, float y1,
                float x2, float y2);

    // forward arguments
    default void quadTo(float[] pts, int off) {
        quadTo(pts[off], pts[off+1], pts[off+2], pts[off+3]);
    }

    /**
     * Accepts a curved segment, defined by three new points, to the path by
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
    void cubicTo(float x1, float y1,
                 float x2, float y2,
                 float x3, float y3);

    // forward arguments
    default void cubicTo(float[] pts, int off) {
        cubicTo(pts[off], pts[off+1], pts[off+2], pts[off+3], pts[off+4], pts[off+5]);
    }

    /**
     * Closes the current contour by drawing a straight line back to
     * the point of the last {@link #moveTo}.  If the path is already
     * closed then this method has no effect.
     */
    void close();

    /**
     * Called after the last segment of the last sub-path when the
     * iteration of the path segments is completely done.
     */
    void done();
}
