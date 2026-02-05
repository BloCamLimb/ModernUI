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
import org.jetbrains.annotations.ApiStatus;

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
 */
@ApiStatus.Experimental
public class Path extends icyllis.arc3d.sketch.Path {

    /**
     * Creates an empty Path with a default fill rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        super();
    }

    /**
     * Creates a copy of an existing Path object.
     * <p>
     * Internally, the two paths share reference values. The underlying
     * verb array, coordinate array and weights are copied when modified.
     */
    public Path(@NonNull Path path) {
        super(path);
    }

    /**
     * Resets the path to its initial state, clears points and verbs and
     * sets fill rule to {@link #WIND_NON_ZERO}.
     * <p>
     * Preserves internal storage if it's unique, otherwise discards.
     */
    @Override
    public void reset() {
        super.reset();
    }

    //TODO add PathBuilder

//    /**
//     * Resets the path to its initial state, clears points and verbs and
//     * sets fill rule to {@link #WIND_NON_ZERO}.
//     * <p>
//     * Preserves internal storage if it's unique, otherwise allocates new
//     * storage with the same size.
//     */
//    @Override
//    public void clear() {
//        super.clear();
//    }
//
//    /**
//     * Resets the path to its initial state, clears points and verbs and
//     * sets fill rule to {@link #WIND_NON_ZERO}.
//     * <p>
//     * This explicitly discards the internal storage, it is recommended to
//     * call when the path object will be no longer used.
//     */
//    @Override
//    public void release() {
//        super.release();
//    }
//
//    /**
//     * Relative version of "move to".
//     *
//     * @param dx offset from last point to contour start on x-axis
//     * @param dy offset from last point to contour start on y-axis
//     */
//    public final void relativeMoveTo(float dx, float dy) {
//        super.moveToRel(dx, dy);
//    }
//
//    /**
//     * Relative version of "line to".
//     * <p>
//     * Adds a line from the last point to the specified vector (dx, dy).
//     *
//     * @param dx the offset from last point to line end on x-axis
//     * @param dy the offset from last point to line end on y-axis
//     */
//    public final void relativeLineTo(float dx, float dy) {
//        super.lineToRel(dx, dy);
//    }
//
//    /**
//     * Relative version of "quad to".
//     * <p>
//     * Adds quad from last point towards vector (dx1, dy1), to vector (dx2, dy2).
//     *
//     * @param dx1 offset from last point to quad control on x-axis
//     * @param dy1 offset from last point to quad control on y-axis
//     * @param dx2 offset from last point to quad end on x-axis
//     * @param dy2 offset from last point to quad end on y-axis
//     */
//    public final void relativeQuadTo(float dx1, float dy1,
//                                     float dx2, float dy2) {
//        super.quadToRel(dx1, dy1, dx2, dy2);
//    }
//
//    /**
//     * Relative version of "cubic to".
//     * <p>
//     * Adds cubic from last point towards vector (dx1, dy1), vector (dx2, dy2),
//     * to vector (dx3, dy3).
//     *
//     * @param dx1 offset from last point to first cubic control on x-axis
//     * @param dy1 offset from last point to first cubic control on y-axis
//     * @param dx2 offset from last point to second cubic control on x-axis
//     * @param dy2 offset from last point to second cubic control on y-axis
//     * @param dx3 offset from last point to cubic end on x-axis
//     * @param dy3 offset from last point to cubic end on y-axis
//     */
//    public final void relativeCubicTo(float dx1, float dy1,
//                                      float dx2, float dy2,
//                                      float dx3, float dy3) {
//        super.cubicToRel(dx1, dy1, dx2, dy2, dx3, dy3);
//    }

    public final void getBounds(@NonNull RectF out) {
        var data = getPathData();
        out.set(data.getLeft(), data.getTop(), data.getRight(), data.getBottom());
    }
}
