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

package icyllis.arc3d.core;

/**
 * The {@link Rect2i} holds four integer coordinates describing the upper and
 * lower bounds of a rectangle (left, top, right bottom). These fields can
 * be accessed directly. Use width() and height() to retrieve the rectangle's
 * width and height.
 * <p>
 * Rect may be created from outer bounds or from position, width, and height.
 * Rect describes an area; if its right is less than or equal to its left,
 * or if its bottom is less than or equal to its top, it is considered empty.
 * <p>
 * Note that the right and bottom coordinates are exclusive. This means a
 * {@link Rect2i} being drawn untransformed onto a {@link Canvas} will
 * draw into the column and row described by its left and top coordinates,
 * but not those of its bottom and right.
 */
@SuppressWarnings("unused")
public class Rect2i {

    public int mLeft;
    public int mTop;
    public int mRight;
    public int mBottom;

    /**
     * Create a new rectangle with all coordinates initialized to 0.
     */
    public Rect2i() {
    }

    /**
     * Create a new rectangle with the specified coordinates. Note: no range
     * checking is performed, so the caller should ensure that left <= right and
     * top <= bottom.
     *
     * @param left   the X coordinate of the left side of the rectangle
     * @param top    the Y coordinate of the top of the rectangle
     * @param right  the X coordinate of the right side of the rectangle
     * @param bottom the Y coordinate of the bottom of the rectangle
     */
    public Rect2i(int left, int top, int right, int bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle (which is left unmodified).
     *
     * @param r the rectangle whose coordinates are copied into the new
     *          rectangle
     */
    public Rect2i(Rect2i r) {
        mLeft = r.mLeft;
        mTop = r.mTop;
        mRight = r.mRight;
        mBottom = r.mBottom;
    }

    /**
     * Returns true if left is equal to or greater than right, or if top is equal
     * to or greater than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or negative
     */
    public final boolean isEmpty() {
        return mRight <= mLeft || mBottom <= mTop;
    }

    /**
     * Returns true if left is equal to or less than right, or if top is equal
     * to or less than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or positive
     */
    public final boolean isSorted() {
        return mLeft <= mRight && mTop <= mBottom;
    }

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    public final int width() {
        return mRight - mLeft;
    }

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    public final int height() {
        return mBottom - mTop;
    }

    /**
     * @return the horizontal center of the rectangle. If the computed value
     * is fractional, this method returns the largest integer that is
     * less than the computed value.
     */
    public final int centerX() {
        return (mLeft + mRight) >> 1;
    }

    /**
     * @return the vertical center of the rectangle. If the computed value
     * is fractional, this method returns the largest integer that is
     * less than the computed value.
     */
    public final int centerY() {
        return (mTop + mBottom) >> 1;
    }

    /**
     * @return the exact horizontal center of the rectangle as a float.
     */
    public final float exactCenterX() {
        return (mLeft + mRight) * 0.5f;
    }

    /**
     * @return the exact vertical center of the rectangle as a float.
     */
    public final float exactCenterY() {
        return (mTop + mBottom) * 0.5f;
    }

    /**
     * Set the rectangle to (0,0,0,0)
     */
    public final void setEmpty() {
        mLeft = mRight = mTop = mBottom = 0;
    }

    /**
     * Set the rectangle's coordinates to the specified values. Note: no range
     * checking is performed, so it is up to the caller to ensure that
     * left <= right and top <= bottom.
     *
     * @param left   the X coordinate of the left side of the rectangle
     * @param top    the Y coordinate of the top of the rectangle
     * @param right  the X coordinate of the right side of the rectangle
     * @param bottom the Y coordinate of the bottom of the rectangle
     */
    public final void set(int left, int top, int right, int bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    /**
     * Copy the coordinates from src into this rectangle.
     *
     * @param src the rectangle whose coordinates are copied into this
     *            rectangle.
     */
    public final void set(Rect2i src) {
        mLeft = src.mLeft;
        mTop = src.mTop;
        mRight = src.mRight;
        mBottom = src.mBottom;
    }

    /**
     * Offset the rectangle by adding dx to its left and right coordinates, and
     * adding dy to its top and bottom coordinates.
     *
     * @param dx the amount to add to the rectangle's left and right coordinates
     * @param dy the amount to add to the rectangle's top and bottom coordinates
     */
    public final void offset(int dx, int dy) {
        mLeft += dx;
        mTop += dy;
        mRight += dx;
        mBottom += dy;
    }

    /**
     * Offset the rectangle to a specific (left, top) position,
     * keeping its width and height the same.
     *
     * @param newLeft the new "left" coordinate for the rectangle
     * @param newTop  the new "top" coordinate for the rectangle
     */
    public final void offsetTo(int newLeft, int newTop) {
        mRight += newLeft - mLeft;
        mBottom += newTop - mTop;
        mLeft = newLeft;
        mTop = newTop;
    }

    /**
     * Inset the rectangle by (dx,dy). If dx is positive, then the sides are
     * moved inwards, making the rectangle narrower. If dx is negative, then the
     * sides are moved outwards, making the rectangle wider. The same holds true
     * for dy and the top and bottom.
     *
     * @param dx the amount to add(subtract) from the rectangle's left(right)
     * @param dy the amount to add(subtract) from the rectangle's top(bottom)
     */
    public final void inset(int dx, int dy) {
        mLeft += dx;
        mTop += dy;
        mRight -= dx;
        mBottom -= dy;
    }

    /**
     * Insets the rectangle on all sides specified by the insets.
     *
     * @param left   the amount to add from the rectangle's left
     * @param top    the amount to add from the rectangle's top
     * @param right  the amount to subtract from the rectangle's right
     * @param bottom the amount to subtract from the rectangle's bottom
     */
    public final void inset(int left, int top, int right, int bottom) {
        mLeft += left;
        mTop += top;
        mRight -= right;
        mBottom -= bottom;
    }

    /**
     * Insets the rectangle on all sides specified by the dimensions of the {@code insets}
     * rectangle.
     *
     * @param insets the rectangle specifying the insets on all side.
     */
    public final void inset(Rect2i insets) {
        mLeft += insets.mLeft;
        mTop += insets.mTop;
        mRight -= insets.mRight;
        mBottom -= insets.mBottom;
    }

    /**
     * Adjusts the rectangle on all sides specified by the values.
     *
     * @param left   the amount to add from the rectangle's left
     * @param top    the amount to add from the rectangle's top
     * @param right  the amount to add from the rectangle's right
     * @param bottom the amount to add from the rectangle's bottom
     */
    public final void adjust(int left, int top, int right, int bottom) {
        mLeft += left;
        mTop += top;
        mRight += right;
        mBottom += bottom;
    }

    /**
     * Adjusts the rectangle on all sides specified by the values.
     *
     * @param adjusts the rectangle specifying the adjusts on all side.
     */
    public final void adjust(Rect2i adjusts) {
        mLeft += adjusts.mLeft;
        mTop += adjusts.mTop;
        mRight += adjusts.mRight;
        mBottom += adjusts.mBottom;
    }

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
    public final boolean contains(int x, int y) {
        return x >= mLeft && x < mRight && y >= mTop && y < mBottom;
    }

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
    public final boolean contains(float x, float y) {
        return x >= mLeft && x < mRight && y >= mTop && y < mBottom;
    }

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
    public final boolean contains(int left, int top, int right, int bottom) {
        // check for empty first
        return mLeft < mRight && mTop < mBottom
                // now check for containment
                && mLeft <= left && mTop <= top
                && mRight >= right && mBottom >= bottom;
    }

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    public final boolean contains(Rect2i r) {
        // check for empty first
        return mLeft < mRight && mTop < mBottom
                // now check for containment
                && mLeft <= r.mLeft && mTop <= r.mTop && mRight >= r.mRight && mBottom >= r.mBottom;
    }

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
    public final boolean contains(float left, float top, float right, float bottom) {
        // check for empty first
        return mLeft < mRight && mTop < mBottom
                // now check for containment
                && mLeft <= left && mTop <= top
                && mRight >= right && mBottom >= bottom;
    }

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    public final boolean contains(Rect2f r) {
        // check for empty first
        return mLeft < mRight && mTop < mBottom
                // now check for containment
                && mLeft <= r.mLeft && mTop <= r.mTop && mRight >= r.mRight && mBottom >= r.mBottom;
    }

    /**
     * If the rectangle specified by left,top,right,bottom intersects this
     * rectangle, return true and set this rectangle to that intersection,
     * otherwise return false and do not change this rectangle. Note: To
     * just test for intersection, use {@link #intersects(Rect2i, Rect2i)}.
     *
     * @param left   the left side of the rectangle being intersected with this
     *               rectangle
     * @param top    the top of the rectangle being intersected with this rectangle
     * @param right  the right side of the rectangle being intersected with this
     *               rectangle.
     * @param bottom the bottom of the rectangle being intersected with this
     *               rectangle.
     * @return true if the specified rectangle and this rectangle intersect
     * (and this rectangle is then set to that intersection) else
     * return false and do not change this rectangle.
     */
    public final boolean intersect(int left, int top, int right, int bottom) {
        int tmpL = Math.max(mLeft, left);
        int tmpT = Math.max(mTop, top);
        int tmpR = Math.min(mRight, right);
        int tmpB = Math.min(mBottom, bottom);
        if (tmpR <= tmpL || tmpB <= tmpT) {
            return false;
        }
        mLeft = tmpL;
        mTop = tmpT;
        mRight = tmpR;
        mBottom = tmpB;
        return true;
    }

    /**
     * If the specified rectangle intersects this rectangle, return true and set
     * this rectangle to that intersection, otherwise return false and do not
     * change this rectangle. To just test for intersection, use intersects().
     *
     * @param r the rectangle being intersected with this rectangle.
     * @return true if the specified rectangle and this rectangle intersect
     * (and this rectangle is then set to that intersection) else
     * return false and do not change this rectangle.
     */
    public final boolean intersect(Rect2i r) {
        return intersect(r.mLeft, r.mTop, r.mRight, r.mBottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(int, int, int, int) but without checking if the rects overlap.
     */
    public final void intersectNoCheck(int left, int top, int right, int bottom) {
        mLeft = Math.max(mLeft, left);
        mTop = Math.max(mTop, top);
        mRight = Math.min(mRight, right);
        mBottom = Math.min(mBottom, bottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(int, int, int, int) but without checking if the rects overlap.
     */
    public final void intersectNoCheck(Rect2i r) {
        intersectNoCheck(r.mLeft, r.mTop, r.mRight, r.mBottom);
    }

    /**
     * If rectangles a and b intersect, return true and set this rectangle to
     * that intersection, otherwise return false and do not change this
     * rectangle. To just test for intersection, use intersects().
     *
     * @param a the first rectangle being intersected with
     * @param b the second rectangle being intersected with
     * @return true if the two specified rectangles intersect. If they do, set
     * this rectangle to that intersection. If they do not, return
     * false and do not change this rectangle.
     */
    public final boolean intersect(Rect2i a, Rect2i b) {
        int tmpL = Math.max(a.mLeft, b.mLeft);
        int tmpT = Math.max(a.mTop, b.mTop);
        int tmpR = Math.min(a.mRight, b.mRight);
        int tmpB = Math.min(a.mBottom, b.mBottom);
        if (tmpR <= tmpL || tmpB <= tmpT) {
            return false;
        }
        mLeft = tmpL;
        mTop = tmpT;
        mRight = tmpR;
        mBottom = tmpB;
        return true;
    }

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
    public final boolean intersects(int left, int top, int right, int bottom) {
        int tmpL = Math.max(mLeft, left);
        int tmpT = Math.max(mTop, top);
        int tmpR = Math.min(mRight, right);
        int tmpB = Math.min(mBottom, bottom);
        return tmpR > tmpL && tmpB > tmpT;
    }

    /**
     * Returns true if this rectangle intersects the specified rectangle.
     * In no event is this rectangle modified. To record the intersection,
     * use intersect().
     *
     * @param r the rectangle being tested for intersection
     * @return true if the specified rectangle intersects this rectangle. In
     * no event is this rectangle modified.
     */
    public final boolean intersects(Rect2i r) {
        return intersects(r.mLeft, r.mTop, r.mRight, r.mBottom);
    }

    /**
     * Returns true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified. To record the intersection,
     * use {@link #intersect(Rect2i)} or {@link #intersect(Rect2i, Rect2i)}.
     *
     * @param a the first rectangle being tested for intersection
     * @param b the second rectangle being tested for intersection
     * @return true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified.
     */
    public static boolean intersects(Rect2i a, Rect2i b) {
        int tmpL = Math.max(a.mLeft, b.mLeft);
        int tmpT = Math.max(a.mTop, b.mTop);
        int tmpR = Math.min(a.mRight, b.mRight);
        int tmpB = Math.min(a.mBottom, b.mBottom);
        return tmpR > tmpL && tmpB > tmpT;
    }

    /**
     * Update this Rect to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param left   the left edge being unioned with this rectangle
     * @param top    the top edge being unioned with this rectangle
     * @param right  the right edge being unioned with this rectangle
     * @param bottom the bottom edge being unioned with this rectangle
     */
    public final void join(int left, int top, int right, int bottom) {
        // do nothing if the params are empty
        if (left >= right || top >= bottom) {
            return;
        }
        if (mLeft < mRight && mTop < mBottom) {
            if (mLeft > left) mLeft = left;
            if (mTop > top) mTop = top;
            if (mRight < right) mRight = right;
            if (mBottom < bottom) mBottom = bottom;
        } else {
            // if we are empty, just assign
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
        }
    }

    /**
     * Update this Rect to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public final void join(Rect2i r) {
        join(r.mLeft, r.mTop, r.mRight, r.mBottom);
    }

    /**
     * Update this Rect to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param left   the left edge being unioned with this rectangle
     * @param top    the top edge being unioned with this rectangle
     * @param right  the right edge being unioned with this rectangle
     * @param bottom the bottom edge being unioned with this rectangle
     */
    public final void joinNoCheck(int left, int top, int right, int bottom) {
        mLeft = Math.min(mLeft, left);
        mTop = Math.min(mTop, top);
        mRight = Math.max(mRight, right);
        mBottom = Math.max(mBottom, bottom);
    }

    /**
     * Update this Rect to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public final void joinNoCheck(Rect2i r) {
        joinNoCheck(r.mLeft, r.mTop, r.mRight, r.mBottom);
    }

    /**
     * Update this Rect to enclose itself and the [x,y] coordinate.
     *
     * @param x The x coordinate of the point to add to the rectangle
     * @param y The y coordinate of the point to add to the rectangle
     */
    public final void join(int x, int y) {
        if (mLeft < mRight && mTop < mBottom) {
            if (x < mLeft) {
                mLeft = x;
            } else if (x > mRight) {
                mRight = x;
            }
            if (y < mTop) {
                mTop = y;
            } else if (y > mBottom) {
                mBottom = y;
            }
        } else {
            // still empty
            mLeft = mRight = x;
            mTop = mBottom = y;
        }
    }

    /**
     * Swap top/bottom or left/right if there are flipped (i.e. left > right
     * and/or top > bottom). This can be called if the edges are computed
     * separately, and may have crossed over each other. If the edges are
     * already correct (i.e. left <= right and top <= bottom) then nothing is done.
     */
    public final void sort() {
        if (mLeft > mRight) {
            int temp = mLeft;
            mLeft = mRight;
            mRight = temp;
        }
        if (mTop > mBottom) {
            int temp = mTop;
            mTop = mBottom;
            mBottom = temp;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rect2i rect = (Rect2i) o;
        return mLeft == rect.mLeft && mTop == rect.mTop && mRight == rect.mRight && mBottom == rect.mBottom;
    }

    @Override
    public int hashCode() {
        int result = mLeft;
        result = 31 * result + mTop;
        result = 31 * result + mRight;
        result = 31 * result + mBottom;
        return result;
    }

    @Override
    public String toString() {
        return "Rect(" + mLeft + ", " +
                mTop + ", " + mRight +
                ", " + mBottom + ")";
    }
}
