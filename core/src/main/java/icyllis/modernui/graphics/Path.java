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
public class Path extends icyllis.arc3d.core.Path {

    /**
     * Creates an empty Path with a default winding rule of {@link #WIND_NON_ZERO}.
     */
    public Path() {
        super();
    }

    /**
     * Creates a copy of an existing Path object.
     */
    public Path(@NonNull Path path) {
        super(path);
    }

    /**
     * Resets the path to empty.
     * <p>
     * If internal storage is shared, unref it.
     * Otherwise, preserves internal storage.
     */
    @Override
    public void reset() {
        super.reset();
    }

    /**
     * Resets the path to empty.
     * <p>
     * If internal storage is shared, create new internal storage.
     * Otherwise, preserves internal storage.
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * Resets the path to empty and removes internal storage.
     */
    @Override
    public void recycle() {
        super.recycle();
    }

    public final void relativeMoveTo(float dx, float dy) {
        super.moveToRel(dx, dy);
    }

    /**
     * Adds a line from the last point to the specified vector (dx, dy).
     *
     * @param dx the offset from last point to line end on x-axis
     * @param dy the offset from last point to line end on y-axis
     */
    public final void relativeLineTo(float dx, float dy) {
        super.lineToRel(dx, dy);
    }

    public final void relativeQuadTo(float dx1, float dy1,
                              float dx2, float dy2) {
        super.quadToRel(dx1, dy1, dx2, dy2);
    }

    public final void relativeCubicTo(float dx1, float dy1,
                               float dx2, float dy2,
                               float dx3, float dy3) {
        super.cubicToRel(dx1, dy1, dx2, dy2, dx3, dy3);
    }
}
