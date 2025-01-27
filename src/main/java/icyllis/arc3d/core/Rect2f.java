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
 * The {@link Rect2f} holds four float coordinates describing the upper and
 * lower bounds of a rectangle. (left, top, right, bottom). These fields can
 * be accessed directly. Use width() and height() to retrieve the rectangle's
 * width and height.
 * <p>
 * Rect may be created from outer bounds or from position, width, and height.
 * Rect describes an area; if its right is less than or equal to its left,
 * or if its bottom is less than or equal to its top, it is considered empty.
 */
@SuppressWarnings("unused")
public non-sealed class Rect2f implements Rect2fc {

    private static final Rect2fc EMPTY = new Rect2f();

    public float mLeft;
    public float mTop;
    public float mRight;
    public float mBottom;

    /**
     * Create a new empty rectangle. All coordinates are initialized to 0.
     */
    public Rect2f() {
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
    public Rect2f(float left, float top, float right, float bottom) {
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
    public Rect2f(@NonNull Rect2fc r) {
        r.store(this);
    }

    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle (which is left unmodified).
     *
     * @param r the rectangle whose coordinates are copied into the new
     *          rectangle
     */
    public Rect2f(@NonNull Rect2ic r) {
        r.store(this);
    }

    /**
     * Returns a read-only empty rect.
     *
     * @return an empty rect
     */
    @NonNull
    public static Rect2fc empty() {
        return EMPTY;
    }

    /**
     * Returns true if left is equal to or greater than right, or if top is equal
     * to or greater than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or negative
     */
    @Contract(pure = true)
    public final boolean isEmpty() {
        // will return true if any values are NaN
        return !(mLeft < mRight && mTop < mBottom);
    }

    /**
     * Returns true if left is equal to or less than right, or if top is equal
     * to or less than bottom. Call sort() to reverse rectangles with negative
     * width() or height().
     *
     * @return true if width() or height() are zero or positive
     */
    @Contract(pure = true)
    public final boolean isSorted() {
        return mLeft <= mRight && mTop <= mBottom;
    }

    /**
     * Returns true if all values in the rectangle are finite.
     *
     * @return true if no member is infinite or NaN
     */
    @Contract(pure = true)
    public final boolean isFinite() {
        return MathUtil.isFinite(mLeft, mTop, mRight, mBottom);
    }

    /**
     * Returns the rectangle's left.
     */
    @Contract(pure = true)
    public final float x() {
        return mLeft;
    }

    /**
     * Return the rectangle's top.
     */
    @Contract(pure = true)
    public final float y() {
        return mTop;
    }

    /**
     * Returns the rectangle's left.
     */
    @Contract(pure = true)
    public final float left() {
        return mLeft;
    }

    /**
     * Return the rectangle's top.
     */
    @Contract(pure = true)
    public final float top() {
        return mTop;
    }

    /**
     * Return the rectangle's right.
     */
    @Contract(pure = true)
    public final float right() {
        return mRight;
    }

    /**
     * Return the rectangle's bottom.
     */
    @Contract(pure = true)
    public final float bottom() {
        return mBottom;
    }

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    @Contract(pure = true)
    public final float width() {
        return mRight - mLeft;
    }

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    @Contract(pure = true)
    public final float height() {
        return mBottom - mTop;
    }

    /**
     * @return the horizontal center of the rectangle. This does not check for
     * a valid rectangle (i.e. left <= right)
     */
    @Contract(pure = true)
    public final float centerX() {
        return (float) (((double) mLeft + mRight) * 0.5);
    }

    /**
     * @return the vertical center of the rectangle. This does not check for
     * a valid rectangle (i.e. top <= bottom)
     */
    @Contract(pure = true)
    public final float centerY() {
        return (float) (((double) mTop + mBottom) * 0.5);
    }

    /**
     * @return width()/2 without intermediate overflow or underflow.
     */
    @Contract(pure = true)
    public final float halfWidth() {
        return (float) (((double) -mLeft + mRight) * 0.5);
    }

    /**
     * @return height()/2 without intermediate overflow or underflow.
     */
    @Contract(pure = true)
    public final float halfHeight() {
        return (float) (((double) -mTop + mBottom) * 0.5);
    }

    /**
     * Set the rectangle to (0,0,0,0)
     */
    @Contract(mutates = "this")
    public final void setEmpty() {
        mLeft = mRight = mTop = mBottom = 0;
    }

    /**
     * Copy the coordinates from this into r.
     *
     * @param dst the rectangle to store
     */
    @Contract(mutates = "param")
    public void store(@NonNull Rect2f dst) {
        dst.mLeft = mLeft;
        dst.mTop = mTop;
        dst.mRight = mRight;
        dst.mBottom = mBottom;
    }

    @Contract(mutates = "param")
    public void store(@NonNull Rect2i dst) {
        dst.mLeft = (int) mLeft;
        dst.mTop = (int) mTop;
        dst.mRight = (int) mRight;
        dst.mBottom = (int) mBottom;
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
    @Contract(mutates = "this")
    public final void set(float left, float top, float right, float bottom) {
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
    @Contract(mutates = "this")
    public final void set(Rect2fc src) {
        src.store(this);
    }

    /**
     * Copy the coordinates from src into this rectangle.
     *
     * @param src the rectangle whose coordinates are copied into this
     *            rectangle.
     */
    @Contract(mutates = "this")
    public final void set(Rect2ic src) {
        src.store(this);
    }

    /**
     * Sets to bounds of <var>pts</var> array with <var>count</var> points. Returns
     * false if <var>pts</var> array contains an infinity or NaN; in this case
     * sets rect to (0, 0, 0, 0).
     *
     * @param pts    pts array
     * @param offset starting offset
     * @param count  number of points
     * @return true if all values are finite
     */
    @Contract(mutates = "this")
    public final boolean setBounds(float[] pts, int offset, int count) {
        if (count <= 0) {
            setEmpty();
            return true;
        }

        float minX, minY;
        float maxX, maxY;

        minX = maxX = pts[offset++];
        minY = maxY = pts[offset++];
        count--;

        float prodX, prodY;
        prodX = 0 * minX;
        prodY = 0 * minY;

        // auto vectorization
        for (int i = 0; i < count; ++i) {
            float x = pts[offset++];
            float y = pts[offset++];
            prodX *= x;
            prodY *= y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        if (prodX == 0 && prodY == 0) {
            set(minX, minY, maxX, maxY);
            return true;
        } else {
            setEmpty();
            return false;
        }
    }

    /**
     * Sets to bounds of <var>pts</var> array with <var>count</var> points. If
     * <var>pts</var> array contains an infinity or NaN, all rect values are set to NaN.
     *
     * @param pts    pts array
     * @param offset starting offset
     * @param count  number of points
     */
    @Contract(mutates = "this")
    public final void setBoundsNoCheck(float[] pts, int offset, int count) {
        if (!setBounds(pts, offset, count)) {
            set(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
        }
    }

    /**
     * Offset the rectangle by adding dx to its left and right coordinates, and
     * adding dy to its top and bottom coordinates.
     *
     * @param dx the amount to add to the rectangle's left and right coordinates
     * @param dy the amount to add to the rectangle's top and bottom coordinates
     */
    @Contract(mutates = "this")
    public final void offset(float dx, float dy) {
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
    @Contract(mutates = "this")
    public final void offsetTo(float newLeft, float newTop) {
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
    @Contract(mutates = "this")
    public final void inset(float dx, float dy) {
        mLeft += dx;
        mTop += dy;
        mRight -= dx;
        mBottom -= dy;
    }

    /**
     * Outset the rectangle by (dx,dy).
     *
     * @param dx the amount to subtract(add) from the rectangle's left(right)
     * @param dy the amount to subtract(add) from the rectangle's top(bottom)
     */
    @Contract(mutates = "this")
    public final void outset(float dx, float dy) {
        mLeft -= dx;
        mTop -= dy;
        mRight += dx;
        mBottom += dy;
    }

    /**
     * Insets the rectangle on all sides specified by the insets.
     *
     * @param left   the amount to add from the rectangle's left
     * @param top    the amount to add from the rectangle's top
     * @param right  the amount to subtract from the rectangle's right
     * @param bottom the amount to subtract from the rectangle's bottom
     */
    @Contract(mutates = "this")
    public final void inset(float left, float top, float right, float bottom) {
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
    @Contract(mutates = "this")
    public final void inset(Rect2fc insets) {
        mLeft += insets.left();
        mTop += insets.top();
        mRight -= insets.right();
        mBottom -= insets.bottom();
    }

    /**
     * Insets the rectangle on all sides specified by the dimensions of the {@code insets}
     * rectangle.
     *
     * @param insets the rectangle specifying the insets on all side.
     */
    @Contract(mutates = "this")
    public final void inset(Rect2ic insets) {
        mLeft += insets.left();
        mTop += insets.top();
        mRight -= insets.right();
        mBottom -= insets.bottom();
    }

    /**
     * Adjusts the rectangle on all sides specified by the values.
     *
     * @param left   the amount to add from the rectangle's left
     * @param top    the amount to add from the rectangle's top
     * @param right  the amount to add from the rectangle's right
     * @param bottom the amount to add from the rectangle's bottom
     */
    @Contract(mutates = "this")
    public final void adjust(float left, float top, float right, float bottom) {
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
    @Contract(mutates = "this")
    public final void adjust(Rect2fc adjusts) {
        mLeft += adjusts.left();
        mTop += adjusts.top();
        mRight += adjusts.right();
        mBottom += adjusts.bottom();
    }

    /**
     * Adjusts the rectangle on all sides specified by the values.
     *
     * @param adjusts the rectangle specifying the adjusts on all side.
     */
    @Contract(mutates = "this")
    public final void adjust(Rect2ic adjusts) {
        mLeft += adjusts.left();
        mTop += adjusts.top();
        mRight += adjusts.right();
        mBottom += adjusts.bottom();
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
    @Contract(pure = true)
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
    @Contract(pure = true)
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
    @Contract(pure = true)
    public final boolean contains(@NonNull Rect2fc r) {
        // check for empty first
        return mLeft < mRight && mTop < mBottom
                // now check for containment
                && mLeft <= r.left() && mTop <= r.top() && mRight >= r.right() && mBottom >= r.bottom();
    }

    /**
     * Returns true if the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r the rectangle being tested for containment.
     * @return true if the specified rectangle r is inside or equal to this
     * rectangle
     */
    @Contract(pure = true)
    public final boolean contains(@NonNull Rect2ic r) {
        // check for empty first
        return mLeft < mRight && mTop < mBottom
                // now check for containment
                && mLeft <= r.left() && mTop <= r.top() && mRight >= r.right() && mBottom >= r.bottom();
    }

    // Evaluate A-B. If the difference shape cannot be represented as a rectangle then false is
    // returned and 'out' is set to the largest rectangle contained in said shape. If true is
    // returned then A-B is representable as a rectangle, which is stored in 'out'.
    public static boolean subtract(Rect2fc a, Rect2fc b, Rect2f out) {
        assert out != b;
        if (a.isEmpty() || b.isEmpty() || !intersects(a, b)) {
            // Either already empty, or subtracting the empty rect, or there's no intersection, so
            // in all cases the answer is A.
            if (out != a) {
                out.set(a);
            }
            return true;
        }

        // 4 rectangles to consider. If the edge in A is contained in B, the resulting difference can
        // be represented exactly as a rectangle. Otherwise the difference is the largest subrectangle
        // that is disjoint from B:
        // 1. Left part of A:   (A.left,  A.top,    B.left,  A.bottom)
        // 2. Right part of A:  (B.right, A.top,    A.right, A.bottom)
        // 3. Top part of A:    (A.left,  A.top,    A.right, B.top)
        // 4. Bottom part of A: (A.left,  B.bottom, A.right, A.bottom)
        //
        // Depending on how B intersects A, there will be 1 to 4 positive areas:
        //  - 4 occur when A contains B
        //  - 3 occur when B intersects a single edge
        //  - 2 occur when B intersects at a corner, or spans two opposing edges
        //  - 1 occurs when B spans two opposing edges and contains a 3rd, resulting in an exact rect
        //  - 0 occurs when B contains A, resulting in the empty rect
        //
        // Compute the relative areas of the 4 rects described above. Since each subrectangle shares
        // either the width or height of A, we only have to divide by the other dimension, which avoids
        // overflow on int32 types, and even if the float relative areas overflow to infinity, the
        // comparisons work out correctly and (one of) the infinitely large subrects will be chosen.
        float aHeight = a.height();
        float aWidth = a.width();
        float leftArea = 0.f, rightArea = 0.f, topArea = 0.f, bottomArea = 0.f;
        int positiveCount = 0;
        if (b.left() > a.left()) {
            leftArea = (b.left() - a.left()) / aWidth;
            positiveCount++;
        }
        if (a.right() > b.right()) {
            rightArea = (a.right() - b.right()) / aWidth;
            positiveCount++;
        }
        if (b.top() > a.top()) {
            topArea = (b.top() - a.top()) / aHeight;
            positiveCount++;
        }
        if (a.bottom() > b.bottom()) {
            bottomArea = (a.bottom() - b.bottom()) / aHeight;
            positiveCount++;
        }

        if (positiveCount == 0) {
            assert (b.contains(a));
            out.setEmpty();
            return true;
        }

        if (out != a) {
            out.set(a);
        }
        if (leftArea > rightArea && leftArea > topArea && leftArea > bottomArea) {
            // Left chunk of A, so the new right edge is B's left edge
            out.mRight = b.left();
        } else if (rightArea > topArea && rightArea > bottomArea) {
            // Right chunk of A, so the new left edge is B's right edge
            out.mLeft = b.right();
        } else if (topArea > bottomArea) {
            // Top chunk of A, so the new bottom edge is B's top edge
            out.mBottom = b.top();
        } else {
            // Bottom chunk of A, so the new top edge is B's bottom edge
            assert (bottomArea > 0.f);
            out.mTop = b.bottom();
        }

        // If we have 1 valid area, the disjoint shape is representable as a rectangle.
        assert (!intersects(out, b));
        return positiveCount == 1;
    }

    /**
     * If the rectangle specified by left,top,right,bottom intersects this
     * rectangle, return true and set this rectangle to that intersection,
     * otherwise return false and do not change this rectangle. Note: To
     * just test for intersection, use {@link #intersects(Rect2fc, Rect2fc)}.
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
    @Contract(mutates = "this")
    public final boolean intersect(float left, float top, float right, float bottom) {
        float tmpL = Math.max(mLeft, left);
        float tmpT = Math.max(mTop, top);
        float tmpR = Math.min(mRight, right);
        float tmpB = Math.min(mBottom, bottom);
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
    @Contract(mutates = "this")
    public final boolean intersect(Rect2fc r) {
        return intersect(r.left(), r.top(), r.right(), r.bottom());
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
    @Contract(mutates = "this")
    public final boolean intersect(Rect2ic r) {
        return intersect(r.left(), r.top(), r.right(), r.bottom());
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(float, float, float, float) but without checking if the rects overlap.
     */
    @Contract(mutates = "this")
    public final void intersectNoCheck(float left, float top, float right, float bottom) {
        mLeft = Math.max(mLeft, left);
        mTop = Math.max(mTop, top);
        mRight = Math.min(mRight, right);
        mBottom = Math.min(mBottom, bottom);
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(float, float, float, float) but without checking if the rects overlap.
     */
    @Contract(mutates = "this")
    public final void intersectNoCheck(Rect2fc r) {
        intersectNoCheck(r.left(), r.top(), r.right(), r.bottom());
    }

    /**
     * If the specified rectangle intersects this rectangle, set this rectangle to that
     * intersection, otherwise set this rectangle to the empty rectangle.
     *
     * @see #inset(float, float, float, float) but without checking if the rects overlap.
     */
    @Contract(mutates = "this")
    public final void intersectNoCheck(Rect2ic r) {
        intersectNoCheck(r.left(), r.top(), r.right(), r.bottom());
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
    @Contract(mutates = "this")
    public final boolean intersect(Rect2fc a, Rect2fc b) {
        float tmpL = Math.max(a.left(), b.left());
        float tmpT = Math.max(a.top(), b.top());
        float tmpR = Math.min(a.right(), b.right());
        float tmpB = Math.min(a.bottom(), b.bottom());
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
    @Contract(pure = true)
    public final boolean intersects(float left, float top, float right, float bottom) {
        float tmpL = Math.max(mLeft, left);
        float tmpT = Math.max(mTop, top);
        float tmpR = Math.min(mRight, right);
        float tmpB = Math.min(mBottom, bottom);
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
    @Contract(pure = true)
    public final boolean intersects(@NonNull Rect2fc r) {
        return intersects(r.left(), r.top(), r.right(), r.bottom());
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
    @Contract(pure = true)
    public final boolean intersects(@NonNull Rect2ic r) {
        return intersects(r.left(), r.top(), r.right(), r.bottom());
    }

    /**
     * Returns true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified. To record the intersection,
     * use {@link #intersect(Rect2fc)} or {@link #intersect(Rect2fc, Rect2fc)}.
     *
     * @param a the first rectangle being tested for intersection
     * @param b the second rectangle being tested for intersection
     * @return true if the two specified rectangles intersect. In no event are
     * either of the rectangles modified.
     */
    public static boolean intersects(Rect2fc a, Rect2fc b) {
        float tmpL = Math.max(a.left(), b.left());
        float tmpT = Math.max(a.top(), b.top());
        float tmpR = Math.min(a.right(), b.right());
        float tmpB = Math.min(a.bottom(), b.bottom());
        return tmpR > tmpL && tmpB > tmpT;
    }

    /**
     * Returns true if the rectangles have a nonzero area of overlap. It assumed that rects can be
     * infinitely small but not "inverted".
     */
    public static boolean rectsOverlap(Rect2fc a, Rect2fc b) {
        assert (!a.isFinite() || (a.left() <= a.right() && a.top() <= a.bottom()));
        assert (!b.isFinite() || (b.left() <= b.right() && b.top() <= b.bottom()));
        return a.right() > b.left() && a.bottom() > b.top() && b.right() > a.left() && b.bottom() > a.top();
    }

    /**
     * Returns true if the rectangles overlap or share an edge or corner. It assumed that rects can be
     * infinitely small but not "inverted".
     */
    public static boolean rectsTouchOrOverlap(Rect2fc a, Rect2fc b) {
        assert (!a.isFinite() || (a.left() <= a.right() && a.top() <= a.bottom()));
        assert (!b.isFinite() || (b.left() <= b.right() && b.top() <= b.bottom()));
        return a.right() >= b.left() && a.bottom() >= b.top() && b.right() >= a.left() && b.bottom() >= a.top();
    }

    /**
     * Set the dst integer Rect by rounding this rectangle's coordinates
     * to their nearest integer values.
     */
    @Contract(mutates = "param")
    public final void round(@NonNull Rect2i dst) {
        dst.set(Math.round(mLeft), Math.round(mTop),
                Math.round(mRight), Math.round(mBottom));
    }

    /**
     * Set the dst integer Rect by rounding "in" this rectangle, choosing the
     * ceiling of top and left, and the floor of right and bottom.
     */
    @Contract(mutates = "param")
    public final void roundIn(@NonNull Rect2i dst) {
        dst.set((int) Math.ceil(mLeft), (int) Math.ceil(mTop),
                (int) Math.floor(mRight), (int) Math.floor(mBottom));
    }

    /**
     * Set the dst integer Rect by rounding "out" this rectangle, choosing the
     * floor of top and left, and the ceiling of right and bottom.
     */
    @Contract(mutates = "param")
    public final void roundOut(@NonNull Rect2i dst) {
        dst.set((int) Math.floor(mLeft), (int) Math.floor(mTop),
                (int) Math.ceil(mRight), (int) Math.ceil(mBottom));
    }

    /**
     * Set the dst rectangle by rounding this rectangle's coordinates
     * to their nearest integer values.
     */
    @Contract(mutates = "param")
    public final void round(@NonNull Rect2f dst) {
        dst.set(Math.round(mLeft), Math.round(mTop),
                Math.round(mRight), Math.round(mBottom));
    }

    /**
     * Set the dst rectangle by rounding "in" this rectangle, choosing the
     * ceiling of top and left, and the floor of right and bottom.
     */
    @Contract(mutates = "param")
    public final void roundIn(@NonNull Rect2f dst) {
        dst.set((float) Math.ceil(mLeft), (float) Math.ceil(mTop),
                (float) Math.floor(mRight), (float) Math.floor(mBottom));
    }

    /**
     * Set the dst rectangle by rounding "out" this rectangle, choosing the
     * floor of top and left, and the ceiling of right and bottom.
     */
    @Contract(mutates = "param")
    public final void roundOut(@NonNull Rect2f dst) {
        dst.set((float) Math.floor(mLeft), (float) Math.floor(mTop),
                (float) Math.ceil(mRight), (float) Math.ceil(mBottom));
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
    @Contract(mutates = "this")
    public final void join(float left, float top, float right, float bottom) {
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
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    @Contract(mutates = "this")
    public final void join(Rect2fc r) {
        join(r.left(), r.top(), r.right(), r.bottom());
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    @Contract(mutates = "this")
    public final void join(Rect2ic r) {
        join(r.left(), r.top(), r.right(), r.bottom());
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
    @Contract(mutates = "this")
    public final void joinNoCheck(float left, float top, float right, float bottom) {
        mLeft = Math.min(mLeft, left);
        mTop = Math.min(mTop, top);
        mRight = Math.max(mRight, right);
        mBottom = Math.max(mBottom, bottom);
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    @Contract(mutates = "this")
    public final void joinNoCheck(Rect2fc r) {
        joinNoCheck(r.left(), r.top(), r.right(), r.bottom());
    }

    /**
     * Update this rectangle to enclose itself and the specified rectangle. If the
     * specified rectangle is empty, nothing is done. If this rectangle is empty
     * it is set to the specified rectangle. No check is performed to see if
     * either rectangle is empty.
     *
     * @param r the rectangle being unioned with this rectangle
     */
    @Contract(mutates = "this")
    public final void joinNoCheck(Rect2ic r) {
        joinNoCheck(r.left(), r.top(), r.right(), r.bottom());
    }

    /**
     * Update this rectangle to enclose itself and the [x,y] coordinate.
     *
     * @param x The x coordinate of the point to add to the rectangle
     * @param y The y coordinate of the point to add to the rectangle
     */
    @Contract(mutates = "this")
    public final void join(float x, float y) {
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
     * and/or top > bottom). This can be called if
     * the edges are computed separately, and may have crossed over each other.
     * If the edges are already correct (i.e. left <= right and top <= bottom)
     * then nothing is done.
     */
    @Contract(mutates = "this")
    public final void sort() {
        if (mLeft > mRight) {
            float temp = mLeft;
            mLeft = mRight;
            mRight = temp;
        }
        if (mTop > mBottom) {
            float temp = mTop;
            mTop = mBottom;
            mBottom = temp;
        }
    }

    @Override
    public int hashCode() {
        int result = (mLeft != 0.0f ? Float.floatToIntBits(mLeft) : 0);
        result = 31 * result + (mTop != 0.0f ? Float.floatToIntBits(mTop) : 0);
        result = 31 * result + (mRight != 0.0f ? Float.floatToIntBits(mRight) : 0);
        result = 31 * result + (mBottom != 0.0f ? Float.floatToIntBits(mBottom) : 0);
        return result;
    }

    /**
     * Returns true if all members in a: Left, Top, Right, and Bottom; are
     * equal to the corresponding members in b.
     * <p>
     * a and b are not equal if either contain NaN. a and b are equal if members
     * contain zeroes with different signs.
     *
     * @param o rect to compare
     * @return true if members are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rect2fc r)) {
            return false;
        }
        return mLeft == r.left() && mTop == r.top() &&
                mRight == r.right() && mBottom == r.bottom();
    }

    @Override
    public String toString() {
        return "Rect2f(" + mLeft + ", " + mTop + ", "
                + mRight + ", " + mBottom + ")";
    }
}
