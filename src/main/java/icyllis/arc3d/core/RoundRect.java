/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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
 * Represents a rounded rectangle with a bounds and a radius for each corner.
 * Based on bounds and radii, this class may represent: a degenerate line,
 * a rectangle with sharp corners, a rectangle with one or more rounded corners,
 * or a circle. Other geometries (like a rectangle with elliptical corners) can
 * only be represented by {@link Path}.
 */
//TODO
public class RoundRect {

    /**
     * The rectangular bounds, see {@link Rect2f}.
     */
    public float mLeft;
    public float mTop;
    public float mRight;
    public float mBottom;

    /**
     * The corner radii, upper-left, upper-right, lower-right, lower-left, in that order.
     */
    public float mRadiusUL;
    public float mRadiusUR;
    public float mRadiusLR;
    public float mRadiusLL;
}
