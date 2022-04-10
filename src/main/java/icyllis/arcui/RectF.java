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
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui;

import javax.annotation.Nonnull;

/**
 * RectF holds four float coordinates describing the upper and lower bounds
 * of a rectangle. (left, top, right, bottom). These fields can be accessed
 * directly. Use width() and height() to retrieve the rectangle's width and
 * height.
 * <p>
 * Rect may be created from outer bounds or from position, width, and
 * height. Rect describes an area; if its right is less than or equal
 * to its left, or if its bottom is less than or equal to its top,
 * it is considered empty.
 */
@SuppressWarnings("unused")
public final class RectF {

    public float left;
    public float top;
    public float right;
    public float bottom;

    /**
     * Create a new empty RectF. All coordinates are initialized to 0.
     */
    public RectF() {
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
    public RectF(float left, float top, float right, float bottom) {
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
    public RectF(RectF r) {
        left = r.left;
        top = r.top;
        right = r.right;
        bottom = r.bottom;
    }

    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle (which is left unmodified).
     *
     * @param r the rectangle whose coordinates are copied into the new
     *          rectangle
     */
    public RectF(Rect r) {
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
     * Returns true if all values in the rectangle are finite.
     *
     * @return true if no member is infinite or NaN
     */
    public boolean isFinite() {
        return Float.isFinite(left) && Float.isFinite(top) && Float.isFinite(right) && Float.isFinite(bottom);
    }

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    public float width() {
        return right - left;
    }

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    public float height() {
        return bottom - top;
    }

    /**
     * @return the horizontal center of the rectangle. This does not check for
     * a valid rectangle (i.e. left <= right)
     */
    public float centerX() {
        return (left + right) * 0.5f;
    }

    /**
     * @return the vertical center of the rectangle. This does not check for
     * a valid rectangle (i.e. top <= bottom)
     */
    public float centerY() {
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
    public void set(float left, float top, float right, float bottom) {
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
    public void set(RectF src) {
        this.left = src.left;
        this.top = src.top;
        this.right = src.right;
        this.bottom = src.bottom;
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
    public void offset(float dx, float dy) {
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
    public void offsetTo(float newLeft, float newTop) {
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
    public void inset(float dx, float dy) {
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
    public void inset(float left, float top, float right, float bottom) {
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
    public void inset(RectF insets) {
        left += insets.left;
        top += insets.top;
        right -= insets.right;
        bottom -= insets.bottom;
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
    public void adjust(float left, float top, float right, float bottom) {
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
    public void adjust(RectF adjusts) {
        left += adjusts.left;
        top += adjusts.top;
        right += adjusts.right;
        bottom += adjusts.bottom;
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
     * If the rectangle specified by left,top,right,bottom intersects this
     * rectangle, return true and set this rectangle to that intersection,
     * otherwise return false and do not change this rectangle. Note: To
     * just test for intersection, use {@link #intersects(RectF, RectF)}.
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
    public boolean intersect(float left, float top, float right, float bottom) {
        float tmpL = Math.max(this.left, left);
        float tmpT = Math.max(this.top, top);
        float tmpR = Math.min(this.right, right);
        float tmpB = Math.min(this.bottom, bottom);
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
    public boolean intersect(RectF r) {
        return intersect(r.left, r.top, r.right, r.bottom);
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
     * @see #inset(float, float, float, float) but without checking if the rects overlap.
     */
    public void intersectNoCheck(float left, float top, float right, float bottom) {
        this.left = Math.max(this.left, left);
        this.top = Math.max(this.top, top);
        this.right = Math.min(this.right, right);
        this.bottom = Math.min(this.bottom, bottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(float, float, float, float) but without checking if the rects overlap.
     */
    public void intersectNoCheck(RectF r) {
        intersectNoCheck(r.left, r.top, r.right, r.bottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(float, float, float, float) but without checking if the rects overlap.
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
    public boolean intersect(RectF a, RectF b) {
        float tmpL = Math.max(a.left, b.left);
        float tmpT = Math.max(a.top, b.top);
        float tmpR = Math.min(a.right, b.right);
        float tmpB = Math.min(a.bottom, b.bottom);
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
    public boolean intersects(float left, float top, float right, float bottom) {
        float tmpL = Math.max(this.left, left);
        float tmpT = Math.max(this.top, top);
        float tmpR = Math.min(this.right, right);
        float tmpB = Math.min(this.bottom, bottom);
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
    public boolean intersects(RectF r) {
        return intersects(r.left, r.top, r.right, r.bottom);
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
     * use {@link #intersect(RectF)} or {@link #intersect(RectF, RectF)}.
     *
     * @param a the first rectangle being tested for intersection
     * @param b the second rectangle being tested for intersection
     * @return true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified.
     */
    public static boolean intersects(RectF a, RectF b) {
        float tmpL = Math.max(a.left, b.left);
        float tmpT = Math.max(a.top, b.top);
        float tmpR = Math.min(a.right, b.right);
        float tmpB = Math.min(a.bottom, b.bottom);
        return tmpR > tmpL && tmpB > tmpT;
    }

    /**
     * Set the dst integer Rect by rounding this rectangle's coordinates
     * to their nearest integer values.
     */
    public void round(Rect dst) {
        dst.set(Math.round(left), Math.round(top),
                Math.round(right), Math.round(bottom));
    }

    /**
     * Set the dst integer Rect by rounding "in" this rectangle, choosing the
     * ceiling of top and left, and the floor of right and bottom.
     */
    public void roundIn(Rect dst) {
        dst.set((int) Math.ceil(left), (int) Math.ceil(top),
                (int) Math.floor(right), (int) Math.floor(bottom));
    }

    /**
     * Set the dst integer Rect by rounding "out" this rectangle, choosing the
     * floor of top and left, and the ceiling of right and bottom.
     */
    public void roundOut(Rect dst) {
        dst.set((int) Math.floor(left), (int) Math.floor(top),
                (int) Math.ceil(right), (int) Math.ceil(bottom));
    }

    /**
     * Set the dst rectangle by rounding this rectangle's coordinates
     * to their nearest integer values.
     */
    public void round(RectF dst) {
        dst.set(Math.round(left), Math.round(top),
                Math.round(right), Math.round(bottom));
    }

    /**
     * Set the dst rectangle by rounding "in" this rectangle, choosing the
     * ceiling of top and left, and the floor of right and bottom.
     */
    public void roundIn(RectF dst) {
        dst.set((float) Math.ceil(left), (float) Math.ceil(top),
                (float) Math.floor(right), (float) Math.floor(bottom));
    }

    /**
     * Set the dst rectangle by rounding "out" this rectangle, choosing the
     * floor of top and left, and the ceiling of right and bottom.
     */
    public void roundOut(RectF dst) {
        dst.set((float) Math.floor(left), (float) Math.floor(top),
                (float) Math.ceil(right), (float) Math.ceil(bottom));
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param left   the left edge being unioned with this rectangle
     * @param top    the top edge being unioned with this rectangle
     * @param right  the right edge being unioned with this rectangle
     * @param bottom the bottom edge being unioned with this rectangle
     */
    public void join(float left, float top, float right, float bottom) {
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
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public void join(RectF r) {
        join(r.left, r.top, r.right, r.bottom);
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public void join(Rect r) {
        join(r.left, r.top, r.right, r.bottom);
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param left   the left edge being unioned with this rectangle
     * @param top    the top edge being unioned with this rectangle
     * @param right  the right edge being unioned with this rectangle
     * @param bottom the bottom edge being unioned with this rectangle
     */
    public void joinNoCheck(float left, float top, float right, float bottom) {
        this.left = Math.min(this.left, left);
        this.top = Math.min(this.top, top);
        this.right = Math.max(this.right, right);
        this.bottom = Math.max(this.bottom, bottom);
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    public void joinNoCheck(RectF r) {
        joinNoCheck(r.left, r.top, r.right, r.bottom);
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
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
     * Update this rectangle to enclose itself and the [x,y] coordinate.
     *
     * @param x The x coordinate of the point to add to the rectangle
     * @param y The y coordinate of the point to add to the rectangle
     */
    public void join(float x, float y) {
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
     * and/or top > bottom). This can be called if
     * the edges are computed separately, and may have crossed over each other.
     * If the edges are already correct (i.e. left <= right and top <= bottom)
     * then nothing is done.
     */
    public void sort() {
        if (left > right) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RectF r = (RectF) o;
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(left);
        result = 31 * result + Float.floatToIntBits(top);
        result = 31 * result + Float.floatToIntBits(right);
        result = 31 * result + Float.floatToIntBits(bottom);
        return result;
    }

    @Nonnull
    @Override
    public String toString() {
        return "RectF(" + left + ", " + top + ", "
                + right + ", " + bottom + ")";
    }
}
