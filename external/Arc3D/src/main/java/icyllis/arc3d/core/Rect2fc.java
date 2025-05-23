/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * Interface to a read-only view of a rectangle in float coordinates.
 * This does not mean that the rectangle is immutable, it only implies that
 * a method should not change the state of the rectangle.
 * <p>
 * {@code Rect2f const &rect}
 *
 * @author BloCamLimb
 * @see Rect2f
 */
//TODO consider removing this interface and only describing mutability via Contract
public sealed interface Rect2fc permits Rect2f {
    // one implementation is fast

    /**
     * Returns true if left is equal to or greater than right, or if top is equal
     * to or greater than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or negative
     */
    @Contract(pure = true)
    boolean isEmpty();

    /**
     * Returns true if left is equal to or less than right, or if top is equal
     * to or less than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or positive
     */
    @Contract(pure = true)
    boolean isSorted();

    /**
     * Returns true if all values in the rectangle are finite.
     *
     * @return true if no member is infinite or NaN
     */
    @Contract(pure = true)
    boolean isFinite();

    /**
     * Returns the rectangle's left.
     */
    @Contract(pure = true)
    float x();

    /**
     * Return the rectangle's top.
     */
    @Contract(pure = true)
    float y();

    /**
     * Returns the rectangle's left.
     */
    @Contract(pure = true)
    float left();

    /**
     * Return the rectangle's top.
     */
    @Contract(pure = true)
    float top();

    /**
     * Return the rectangle's right.
     */
    @Contract(pure = true)
    float right();

    /**
     * Return the rectangle's bottom.
     */
    @Contract(pure = true)
    float bottom();

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    @Contract(pure = true)
    float width();

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    @Contract(pure = true)
    float height();

    /**
     * @return the horizontal center of the rectangle. This does not check for
     * a valid rectangle (i.e. left <= right)
     */
    @Contract(pure = true)
    float centerX();

    /**
     * @return the vertical center of the rectangle. This does not check for
     * a valid rectangle (i.e. top <= bottom)
     */
    @Contract(pure = true)
    float centerY();

    /**
     * Stores the coordinates from this into dst.
     *
     * @param dst the rectangle to store
     */
    @Contract(mutates = "param")
    void store(@NonNull Rect2f dst);

    /**
     * Stores the coordinates from this into dst.
     *
     * @param dst the rectangle to store
     */
    @Contract(mutates = "param")
    void store(@NonNull Rect2i dst);

    /**
     * Returns true if this rectangle intersects the specified rectangle.
     * In no event is this rectangle modified. To record the intersection,
     * use intersect().
     *
     * @param left   the left side of the rectangle being tested for intersection
     * @param top    the top of the rectangle being tested for intersection
     * @param right  the right side of the rectangle being tested for
     *               intersection
     * @param bottom the bottom of the rectangle being tested for intersection
     * @return true if the specified rectangle intersects this rectangle. In
     * no event is this rectangle modified.
     */
    @Contract(pure = true)
    boolean intersects(float left, float top, float right, float bottom);

    /**
     * Returns true if this rectangle intersects the specified rectangle.
     * In no event is this rectangle modified.
     *
     * @param r the rectangle being tested for intersection
     * @return true if the specified rectangle intersects this rectangle. In
     * no event is this rectangle modified.
     */
    @Contract(pure = true)
    boolean intersects(@NonNull Rect2fc r);

    /**
     * Returns true if this rectangle intersects the specified rectangle.
     * In no event is this rectangle modified. To record the intersection,
     * use intersect().
     *
     * @param r the rectangle being tested for intersection
     * @return true if the specified rectangle intersects this rectangle. In
     * no event is this rectangle modified.
     */
    @Contract(pure = true)
    boolean intersects(@NonNull Rect2ic r);

    /**
     * Returns true if (x,y) is inside the rectangle. The left and top are
     * considered to be inside, while the right and bottom are not. This means
     * that for a (x,y) to be contained: left <= x < right and top <= y < bottom.
     * An empty rectangle never contains any point.
     *
     * @param x the X coordinate of the point being tested for containment
     * @param y the Y coordinate of the point being tested for containment
     * @return true if (x,y) are contained by the rectangle, where containment
     * means left <= x < right and top <= y < bottom
     */
    @Contract(pure = true)
    boolean contains(float x, float y);

    /**
     * Returns true if the 4 specified sides of a rectangle are inside or equal
     * to this rectangle. i.e. is this rectangle a superset of the specified
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param left   the left side of the rectangle being tested for containment
     * @param top    the top of the rectangle being tested for containment
     * @param right  the right side of the rectangle being tested for containment
     * @param bottom the bottom of the rectangle being tested for containment
     * @return true if the 4 specified sides of a rectangle are inside or
     * equal to this rectangle
     */
    @Contract(pure = true)
    boolean contains(float left, float top, float right, float bottom);

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    @Contract(pure = true)
    boolean contains(@NonNull Rect2fc r);

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    @Contract(pure = true)
    boolean contains(@NonNull Rect2ic r);

    /**
     * Set the dst integer Rect by rounding this rectangle's coordinates
     * to their nearest integer values.
     */
    @Contract(mutates = "param")
    void round(@NonNull Rect2i dst);

    /**
     * Set the dst integer Rect by rounding "in" this rectangle, choosing the
     * ceiling of top and left, and the floor of right and bottom.
     */
    @Contract(mutates = "param")
    void roundIn(@NonNull Rect2i dst);

    /**
     * Set the dst integer Rect by rounding "out" this rectangle, choosing the
     * floor of top and left, and the ceiling of right and bottom.
     */
    @Contract(mutates = "param")
    void roundOut(@NonNull Rect2i dst);

    /**
     * Set the dst rectangle by rounding this rectangle's coordinates
     * to their nearest integer values.
     */
    @Contract(mutates = "param")
    void round(@NonNull Rect2f dst);

    /**
     * Set the dst rectangle by rounding "in" this rectangle, choosing the
     * ceiling of top and left, and the floor of right and bottom.
     */
    @Contract(mutates = "param")
    void roundIn(@NonNull Rect2f dst);

    /**
     * Set the dst rectangle by rounding "out" this rectangle, choosing the
     * floor of top and left, and the ceiling of right and bottom.
     */
    @Contract(mutates = "param")
    void roundOut(@NonNull Rect2f dst);
}
