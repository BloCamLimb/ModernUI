/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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
     * Type describes possible specializations of RoundRect. Each Type is
     * exclusive; a RoundRect may only have one type.
     * <p>
     * Type members become progressively less restrictive; larger values of
     * Type have more degrees of freedom than smaller values.
     */
    public static final int
            kEmpty_Type = 0,
            kRect_Type = 1,
            kEllipse_Type = 2,
            kSimple_Type = 3,
            kNineSlice_Type = 4,
            kComplex_Type = 5,
            kLast_Type = kComplex_Type;

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
    public float mRadiusUlx;
    public float mRadiusUly;
    public float mRadiusUrx;
    public float mRadiusUry;
    public float mRadiusLrx;
    public float mRadiusLry;
    public float mRadiusLlx;
    public float mRadiusLly;

    public int mType = kEmpty_Type;

    public RoundRect() {
    }

    public RoundRect(Rect2fc other) {
        setRect(other.left(), other.top(), other.right(), other.bottom());
    }

    public RoundRect(RoundRect other) {
        mLeft = other.mLeft;
        mTop = other.mTop;
        mRight = other.mRight;
        mBottom = other.mBottom;
        mRadiusUlx = other.mRadiusUlx;
        mRadiusUly = other.mRadiusUly;
        mRadiusUrx = other.mRadiusUrx;
        mRadiusUry = other.mRadiusUry;
        mRadiusLrx = other.mRadiusLrx;
        mRadiusLry = other.mRadiusLry;
        mRadiusLlx = other.mRadiusLlx;
        mRadiusLly = other.mRadiusLly;
        mType = other.mType;
    }

    /**
     * Returns the rectangle's left.
     */
    public final float left() {
        return mLeft;
    }

    /**
     * Return the rectangle's top.
     */
    public final float top() {
        return mTop;
    }

    /**
     * Return the rectangle's right.
     */
    public final float right() {
        return mRight;
    }

    /**
     * Return the rectangle's bottom.
     */
    public final float bottom() {
        return mBottom;
    }

    /**
     * @return the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    public final float width() {
        return mRight - mLeft;
    }

    /**
     * @return the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    public final float height() {
        return mBottom - mTop;
    }

    public int getType() {
        return mType;
    }

    public boolean isEmpty() {
        return getType() == kEmpty_Type;
    }

    public boolean isRect() {
        return getType() == kRect_Type;
    }

    public boolean isEllipse() {
        return getType() == kEllipse_Type;
    }

    public boolean isComplex() {
        return getType() == kComplex_Type;
    }

    public float getSimpleRadiusX() {
        assert !isComplex();
        return mRadiusUlx;
    }

    public float getRadiusUlx() {
        return mRadiusUlx;
    }

    public float getRadiusUly() {
        return mRadiusUly;
    }

    public float getRadiusUrx() {
        return mRadiusUrx;
    }

    public float getRadiusUry() {
        return mRadiusUry;
    }

    public float getRadiusLrx() {
        return mRadiusLrx;
    }

    public float getRadiusLry() {
        return mRadiusLry;
    }

    public float getRadiusLlx() {
        return mRadiusLlx;
    }

    public float getRadiusLly() {
        return mRadiusLly;
    }

    public final void getRect(Rect2f dest) {
        dest.set(mLeft, mTop, mRight, mBottom);
    }

    public void getBounds(Rect2f dest) {
        dest.set(mLeft, mTop, mRight, mBottom);
    }

    public void setEmpty() {
        mLeft = mRight = mTop = mBottom = 0;
        mRadiusUlx = mRadiusUly = mRadiusUrx = mRadiusUry =
                mRadiusLrx = mRadiusLry = mRadiusLlx = mRadiusLly = 0;
        mType = kEmpty_Type;
    }

    public void setRect(float left, float top, float right, float bottom) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        mRadiusUlx = mRadiusUly = mRadiusUrx = mRadiusUry =
                mRadiusLrx = mRadiusLry = mRadiusLlx = mRadiusLly = 0;
        mType = kRect_Type;
    }

    public void setEllipse(float left, float top, float right, float bottom) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        float radiusX = (float) (((double) right - left) * 0.5);
        float radiusY = (float) (((double) bottom - top) * 0.5);

        if (radiusX == 0 || radiusY == 0) {
            mRadiusUlx = mRadiusUly = mRadiusUrx = mRadiusUry =
                    mRadiusLrx = mRadiusLry = mRadiusLlx = mRadiusLly = 0;
            mType = kRect_Type;
        } else {
            mRadiusUlx = radiusX;
            mRadiusUly = radiusY;
            mRadiusUrx = radiusX;
            mRadiusUry = radiusY;
            mRadiusLrx = radiusX;
            mRadiusLry = radiusY;
            mRadiusLlx = radiusX;
            mRadiusLly = radiusY;
            mType = kEllipse_Type;
        }
    }

    /**
     * Initializes Rect. If the passed in rect is not finite or empty the round rect will be fully
     * initialized and false is returned. Otherwise, just Rect is initialized and true is returned.
     */
    private boolean initRect(float l, float t, float r, float b) {
        if ((0 * mLeft * mTop * mRight * mBottom) == 0) { // check finite
            mLeft = Math.min(l, r);
            mTop = Math.min(t, b);
            mRight = Math.max(l, r);
            mBottom = Math.max(t, b);
            if (!(mLeft < mRight && mTop < mBottom)) { // check empty or NaN
                mRadiusUlx = mRadiusUly = mRadiusUrx = mRadiusUry =
                        mRadiusLrx = mRadiusLry = mRadiusLlx = mRadiusLly = 0;
                mType = kEmpty_Type;
                return false;
            }
            return true;
        }
        setEmpty();
        return false;
    }
}
