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

/**
 * Iterates {@link Path} elements.
 */
public interface PathIterator {

    /**
     * The winding rule constant for specifying an even-odd rule
     * for determining the interior of a path.<br>
     * The even-odd rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments an odd number of times.
     */
    int WIND_EVEN_ODD = 0;
    /**
     * The winding rule constant for specifying a non-zero rule
     * for determining the interior of a path.<br>
     * The non-zero rule specifies that a point lies inside the
     * path if a ray drawn in any direction from that point to
     * infinity is crossed by path segments a different number
     * of times in the counter-clockwise direction than the
     * clockwise direction.
     */
    int WIND_NON_ZERO = 1;

    /**
     * Basic commands of path segments.
     */
    int
            VERB_MOVE = 0,  // returns 1 point
            VERB_LINE = 1,  // returns 1 point
            VERB_QUAD = 2,  // returns 2 points
            VERB_CUBIC = 4, // returns 3 points
            VERB_CLOSE = 5, // returns 0 points
            VERB_DONE = 6;

    /**
     * Returns next verb, and advances iterator.
     * If there are no more elements, returns {@link #VERB_DONE}.
     * <p>
     * Zero to three points are stored in <var>coords</var>, depending on the
     * command type.
     * <p>
     * A float array of length >= 6 must be passed in and can be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of float x,y coordinates.
     * <ul>
     *     <li>MOVE returns 1 point</li>
     *     <li>LINE returns 1 point</li>
     *     <li>QUAD returns 2 points</li>
     *     <li>CUBIC returns 3 points</li>
     *     <li>CLOSE does not return any points</li>
     * </ul>
     *
     * @param coords array for point data describing returned verb, can be null
     * @return next verb
     */
    int next(float[] coords, int offset);
}
