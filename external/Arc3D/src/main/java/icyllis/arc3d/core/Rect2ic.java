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

import javax.annotation.Nonnull;

/**
 * Interface to a read-only view of a rectangle in integer coordinates.
 * This does not mean that the rectangle is immutable, it only implies that
 * a method should not change the state of the rectangle.
 * <p>
 * {@code Rect2i const &rect}
 *
 * @author BloCamLimb
 * @see Rect2i
 */
public sealed interface Rect2ic permits Rect2i {

    /**
     * Returns true if left is equal to or greater than right, or if top is equal
     * to or greater than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or negative
     */
    boolean isEmpty();

    /**
     * Returns true if left is equal to or less than right, or if top is equal
     * to or less than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or positive
     */
    boolean isSorted();

    /**
     * Returns the rectangle's left.
     */
    int x();

    /**
     * Return the rectangle's top.
     */
    int y();

    /**
     * Returns the rectangle's left.
     */
    int left();

    /**
     * Return the rectangle's top.
     */
    int top();

    /**
     * Return the rectangle's right.
     */
    int right();

    /**
     * Return the rectangle's bottom.
     */
    int bottom();

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    int width();

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    int height();

    /**
     * Stores the coordinates from this into dst.
     *
     * @param dst the rectangle to store
     */
    void store(@Nonnull Rect2i dst);

    /**
     * Stores the coordinates from this into dst.
     *
     * @param dst the rectangle to store
     */
    void store(@Nonnull Rect2f dst);

    /**
     * Returns true if this rectangle intersects the specified rectangle.
     * In no event is this rectangle modified.
     *
     * @param r the rectangle being tested for intersection
     * @return true if the specified rectangle intersects this rectangle. In
     * no event is this rectangle modified.
     */
    boolean intersects(Rect2ic r);

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
    boolean contains(int x, int y);

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
    boolean contains(float x, float y);

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    boolean contains(Rect2ic r);

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    boolean contains(Rect2fc r);
}
