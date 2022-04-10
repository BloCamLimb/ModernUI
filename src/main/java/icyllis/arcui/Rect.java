/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui;

import javax.annotation.Nonnull;

/**
 * Rect holds four integer coordinates describing the upper and lower bounds
 * of a rectangle (left, top, right bottom). These fields can be accessed
 * directly. Use width() and height() to retrieve the rectangle's width and
 * height.
 * <p>
 * Rect may be created from outer bounds or from position, width, and
 * height. Rect describes an area; if its right is less than or equal
 * to its left, or if its bottom is less than or equal to its top,
 * it is considered empty.
 * <p>
 * Note that the right and bottom coordinates are exclusive. This means
 * a Rect being drawn untransformed onto a {@link Canvas} will draw into
 * the column and row described by its left and top coordinates, but not
 * those of its bottom and right.
 */
@SuppressWarnings("unused")
public final class Rect {

    public int left;
    public int top;
    public int right;
    public int bottom;

    /**
     * Create a new Rect with all coordinates initialized to 0.
     */
    public Rect() {
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
    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle (which is left unmodified).
     *
     * @param r the rectangle whose coordinates are copied into the new
     *          rectangle
     */
    public Rect(Rect r) {
        left = r.left;
        top = r.top;
        right = r.right;
        bottom = r.bottom;
    }

    /**
     * Returns true if left is equal to or greater than right, or if top is equal
     * to or greater than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or negative
     */
    public boolean isEmpty() {
        return right <= left || bottom <= top;
    }

    /**
     * Returns true if left is equal to or less than right, or if top is equal
     * to or less than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or positive
     */
    public boolean isSorted() {
        return left <= right && top <= bottom;
    }

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    public int width() {
        return right - left;
    }

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    public int height() {
        return bottom - top;
    }

    /**
     * @return the horizontal center of the rectangle. If the computed value
     * is fractional, this method returns the largest integer that is
     * less than the computed value.
     */
    public int centerX() {
        return (left + right) >> 1;
    }

    /**
     * @return the vertical center of the rectangle. If the computed value
     * is fractional, this method returns the largest integer that is
     * less than the computed value.
     */
    public int centerY() {
        return (top + bottom) >> 1;
    }

    /**
     * @return the exact horizontal center of the rectangle as a float.
     */
    public float exactCenterX() {
        return (left + right) * 0.5f;
    }

    /**
     * @return the exact vertical center of the rectangle as a float.
     */
    public float exactCenterY() {
        return (top + bottom) * 0.5f;
    }

    /**
     * Set the rectangle to (0,0,0,0)
     */
    public void setEmpty() {
        left = right = top = bottom = 0;
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
    public void set(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * Copy the coordinates from src into this rectangle.
     *
     * @param src the rectangle whose coordinates are copied into this
     *            rectangle.
     */
    public void set(Rect src) {
        this.left = src.left;
        this.top = src.top;
        this.right = src.right;
        this.bottom = src.bottom;
    }

    /**
     * Offset the rectangle by adding dx to its left and right coordinates, and
     * adding dy to its top and bottom coordinates.
     *
     * @param dx the amount to add to the rectangle's left and right coordinates
     * @param dy the amount to add to the rectangle's top and bottom coordinates
     */
    public void offset(int dx, int dy) {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    /**
     * Offset the rectangle to a specific (left, top) position,
     * keeping its width and height the same.
     *
     * @param newLeft the new "left" coordinate for the rectangle
     * @param newTop  the new "top" coordinate for the rectangle
     */
    public void offsetTo(int newLeft, int newTop) {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
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
    public void inset(int dx, int dy) {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    /**
     * Insets the rectangle on all sides specified by the insets.
     *
     * @param left   the amount to add from the rectangle's left
     * @param top    the amount to add from the rectangle's top
     * @param right  the amount to subtract from the rectangle's right
     * @param bottom the amount to subtract from the rectangle's bottom
     */
    public void inset(int left, int top, int right, int bottom) {
        this.left += left;
        this.top += top;
        this.right -= right;
        this.bottom -= bottom;
    }

    /**
     * Insets the rectangle on all sides specified by the dimensions of the {@code insets}
     * rectangle.
     *
     * @param insets the rectangle specifying the insets on all side.
     */
    public void inset(Rect insets) {
        left += insets.left;
        top += insets.top;
        right -= insets.right;
        bottom -= insets.bottom;
    }

    /**
     * Adjusts the rectangle on all sides specified by the values.
     *
     * @param left   the amount to add from the rectangle's left
     * @param top    the amount to add from the rectangle's top
     * @param right  the amount to add from the rectangle's right
     * @param bottom the amount to add from the rectangle's bottom
     */
    public void adjust(int left, int top, int right, int bottom) {
        this.left += left;
        this.top += top;
        this.right += right;
        this.bottom += bottom;
    }

    /**
     * Adjusts the rectangle on all sides specified by the values.
     *
     * @param adjusts the rectangle specifying the adjusts on all side.
     */
    public void adjust(Rect adjusts) {
        left += adjusts.left;
        top += adjusts.top;
        right += adjusts.right;
        bottom += adjusts.bottom;
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
    public boolean contains(int x, int y) {
        return x >= left && x < right && y >= top && y < bottom;
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
    public boolean contains(float x, float y) {
        return x >= left && x < right && y >= top && y < bottom;
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
    public boolean contains(int left, int top, int right, int bottom) {
        // check for empty first
        return this.left < this.right && this.top < this.bottom
                // now check for containment
                && this.left <= left && this.top <= top
                && this.right >= right && this.bottom >= bottom;
    }

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    public boolean contains(Rect r) {
        // check for empty first
        return this.left < this.right && this.top < this.bottom
                // now check for containment
                && left <= r.left && top <= r.top && right >= r.right && bottom >= r.bottom;
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
    public boolean contains(float left, float top, float right, float bottom) {
        // check for empty first
        return this.left < this.right && this.top < this.bottom
                // now check for containment
                && this.left <= left && this.top <= top
                && this.right >= right && this.bottom >= bottom;
    }

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    public boolean contains(RectF r) {
        // check for empty first
        return this.left < this.right && this.top < this.bottom
                // now check for containment
                && left <= r.left && top <= r.top && right >= r.right && bottom >= r.bottom;
    }

    /**
     * If the rectangle specified by left,top,right,bottom intersects this
     * rectangle, return true and set this rectangle to that intersection,
     * otherwise return false and do not change this rectangle. Note: To
     * just test for intersection, use {@link #intersects(Rect, Rect)}.
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
    public boolean intersect(int left, int top, int right, int bottom) {
        int tmpL = Math.max(this.left, left);
        int tmpT = Math.max(this.top, top);
        int tmpR = Math.min(this.right, right);
        int tmpB = Math.min(this.bottom, bottom);
        if (tmpR <= tmpL || tmpB <= tmpT) {
            return false;
        }
        this.left = tmpL;
        this.top = tmpT;
        this.right = tmpR;
        this.bottom = tmpB;
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
    public boolean intersect(Rect r) {
        return intersect(r.left, r.top, r.right, r.bottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(int, int, int, int) but without checking if the rects overlap.
     */
    public void intersectNoCheck(int left, int top, int right, int bottom) {
        this.left = Math.max(this.left, left);
        this.top = Math.max(this.top, top);
        this.right = Math.min(this.right, right);
        this.bottom = Math.min(this.bottom, bottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(int, int, int, int) but without checking if the rects overlap.
     */
    public void intersectNoCheck(Rect r) {
        intersectNoCheck(r.left, r.top, r.right, r.bottom);
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
    public boolean intersect(Rect a, Rect b) {
        int tmpL = Math.max(a.left, b.left);
        int tmpT = Math.max(a.top, b.top);
        int tmpR = Math.min(a.right, b.right);
        int tmpB = Math.min(a.bottom, b.bottom);
        if (tmpR <= tmpL || tmpB <= tmpT) {
            return false;
        }
        left = tmpL;
        top = tmpT;
        right = tmpR;
        bottom = tmpB;
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
    public boolean intersects(int left, int top, int right, int bottom) {
        int tmpL = Math.max(this.left, left);
        int tmpT = Math.max(this.top, top);
        int tmpR = Math.min(this.right, right);
        int tmpB = Math.min(this.bottom, bottom);
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
    public boolean intersects(Rect r) {
        return intersects(r.left, r.top, r.right, r.bottom);
    }

    /**
     * Returns true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified. To record the intersection,
     * use {@link #intersect(Rect)} or {@link #intersect(Rect, Rect)}.
     *
     * @param a the first rectangle being tested for intersection
     * @param b the second rectangle being tested for intersection
     * @return true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified.
     */
    public static boolean intersects(Rect a, Rect b) {
        int tmpL = Math.max(a.left, b.left);
        int tmpT = Math.max(a.top, b.top);
        int tmpR = Math.min(a.right, b.right);
        int tmpB = Math.min(a.bottom, b.bottom);
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
    public void join(int left, int top, int right, int bottom) {
        // do nothing if the params are empty
        if (left >= right || top >= bottom) {
            return;
        }
        if (this.left < this.right && this.top < this.bottom) {
            if (this.left > left) this.left = left;
            if (this.top > top) this.top = top;
            if (this.right < right) this.right = right;
            if (this.bottom < bottom) this.bottom = bottom;
        } else {
            // if we are empty, just assign
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    /**
     * Update this Rect to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public void join(Rect r) {
        join(r.left, r.top, r.right, r.bottom);
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
    public void joinNoCheck(int left, int top, int right, int bottom) {
        this.left = Math.min(this.left, left);
        this.top = Math.min(this.top, top);
        this.right = Math.max(this.right, right);
        this.bottom = Math.max(this.bottom, bottom);
    }

    /**
     * Update this Rect to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public void joinNoCheck(Rect r) {
        joinNoCheck(r.left, r.top, r.right, r.bottom);
    }

    /**
     * Update this Rect to enclose itself and the [x,y] coordinate.
     *
     * @param x The x coordinate of the point to add to the rectangle
     * @param y The y coordinate of the point to add to the rectangle
     */
    public void join(int x, int y) {
        if (left < right && top < bottom) {
            if (x < left) {
                left = x;
            } else if (x > right) {
                right = x;
            }
            if (y < top) {
                top = y;
            } else if (y > bottom) {
                bottom = y;
            }
        } else {
            // still empty
            left = right = x;
            top = bottom = y;
        }
    }

    /**
     * Swap top/bottom or left/right if there are flipped (i.e. left > right
     * and/or top > bottom). This can be called if the edges are computed
     * separately, and may have crossed over each other. If the edges are
     * already correct (i.e. left <= right and top <= bottom) then nothing is done.
     */
    public void sort() {
        if (left > right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rect rect = (Rect) o;
        return left == rect.left && top == rect.top && right == rect.right && bottom == rect.bottom;
    }

    @Override
    public int hashCode() {
        int result = left;
        result = 31 * result + top;
        result = 31 * result + right;
        result = 31 * result + bottom;
        return result;
    }

    @Nonnull
    @Override
    public String toString() {
        return "Rect(" + left + ", " +
                top + ", " + right +
                ", " + bottom + ")";
    }
}
