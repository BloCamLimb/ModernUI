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

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Represents a rounded rectangle with a bounds and a pair of radii for each corner.
 * Based on bounds and radii, this class may represent: a degenerate line,
 * a rectangle with sharp corners, a rectangle with one or more rounded corners,
 * a circle, or an ellipse.
 * <p>
 * This class allows implementing CSS properties that describe rounded corners.
 * It may have up to eight different radii, one for each axis on each of its four
 * corners, so the corners may be circular or elliptical.
 * <p>
 * This class may modify the provided parameters when initializing bounds and radii.
 * If either axis radii is zero or less: radii are stored as zero; corner is square.
 * If corner curves overlap, radii are proportionally reduced to fit within bounds.
 */
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
    protected float mLeft;
    protected float mTop;
    protected float mRight;
    protected float mBottom;

    /**
     * The corner radii, upper-left, upper-right, lower-right, lower-left, in that order.
     */
    @Size(8)
    protected final float[] mRadii = new float[8];

    protected int mType = kEmpty_Type;

    /**
     * Initializes bounds at (0, 0), the origin, with zero width and height.
     * Initializes corner radii to (0, 0), and sets type of kEmpty_Type.
     */
    public RoundRect() {
    }

    /**
     * Initializes to copy of other bounds and corner radii.
     */
    public RoundRect(RoundRect other) {
        set(other);
    }

    /**
     * Returns the rectangle's left.
     */
    public final float x() {
        return mLeft;
    }

    /**
     * Return the rectangle's top.
     */
    public final float y() {
        return mTop;
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
     * Returns span on the x-axis. This does not check if result fits in 32-bit float;
     * result may be infinity.
     *
     * @return rect().fRight minus rect().fLeft
     */
    public final float width() {
        return mRight - mLeft;
    }

    /**
     * Returns span on the y-axis. This does not check if result fits in 32-bit float;
     * result may be infinity.
     *
     * @return rect().fBottom minus rect().fTop
     */
    public final float height() {
        return mBottom - mTop;
    }

    /**
     * @return the horizontal center of the rectangle. This does not check for
     * a valid rectangle (i.e. left <= right)
     */
    public final float centerX() {
        return (float) (((double) mLeft + mRight) * 0.5);
    }

    /**
     * @return the vertical center of the rectangle. This does not check for
     * a valid rectangle (i.e. top <= bottom)
     */
    public final float centerY() {
        return (float) (((double) mTop + mBottom) * 0.5);
    }

    /**
     * @return width()/2 without intermediate overflow or underflow.
     */
    public final float halfWidth() {
        return (float) (((double) -mLeft + mRight) * 0.5);
    }

    /**
     * @return height()/2 without intermediate overflow or underflow.
     */
    public final float halfHeight() {
        return (float) (((double) -mTop + mBottom) * 0.5);
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

    public boolean isSimple() {
        return getType() == kSimple_Type;
    }

    public boolean isNineSlice() {
        return getType() == kNineSlice_Type;
    }

    public boolean isComplex() {
        return getType() == kComplex_Type;
    }

    /**
     * Returns top-left corner radii. If type() returns kEmpty_Type, kRect_Type,
     * kEllipse_Type, or kSimple_Type, returns a value representative of all corner radii.
     * If type() returns kNineSlice_Type or kComplex_Type, at least one of the
     * remaining three corners has a different value.
     *
     * @return corner radii for simple types
     */
    public float getSimpleRadiusX() {
        return mRadii[0];
    }

    /**
     * Returns top-left corner radii. If type() returns kEmpty_Type, kRect_Type,
     * kEllipse_Type, or kSimple_Type, returns a value representative of all corner radii.
     * If type() returns kNineSlice_Type or kComplex_Type, at least one of the
     * remaining three corners has a different value.
     *
     * @return corner radii for simple types
     */
    public float getSimpleRadiusY() {
        return mRadii[1];
    }

    /**
     * Returns the corner radii for all four corners, upper-left, upper-right, lower-right,
     * lower-left, in that order. Note that the return value is an unmodifiable view (no copy).
     */
    @Size(8)
    public float[] getRadii() {
        return mRadii;
    }

    /**
     * Copy the values from src into this object.
     */
    public void set(@Nonnull RoundRect src) {
        mLeft = src.mLeft;
        mTop = src.mTop;
        mRight = src.mRight;
        mBottom = src.mBottom;
        System.arraycopy(src.mRadii, 0, mRadii, 0, 8);
        mType = src.mType;
    }

    /**
     * Sets bounds to zero width and height at (0, 0), the origin. Sets
     * corner radii to zero and sets type to kEmpty_Type.
     */
    public void setEmpty() {
        mLeft = mRight = mTop = mBottom = 0;
        Arrays.fill(mRadii, 0);
        mType = kEmpty_Type;
    }

    /**
     * Sets bounds to sorted rect, and sets corner radii to zero.
     * If set bounds has width and height, and sets type to kRect_Type;
     * otherwise, sets type to kEmpty_Type.
     *
     * @param rect bounds to set
     */
    public void setRect(Rect2fc rect) {
        setRect(rect.left(), rect.top(), rect.right(), rect.bottom());
    }

    /**
     * Sets bounds to sorted rect, and sets corner radii to zero.
     * If set bounds has width and height, and sets type to kRect_Type;
     * otherwise, sets type to kEmpty_Type.
     */
    public void setRect(float left, float top, float right, float bottom) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        Arrays.fill(mRadii, 0);
        mType = kRect_Type;

        assert isValid();
    }

    /**
     * Sets bounds to ellipse, x-axis radii to half ellipse.width(), and all y-axis radii
     * to half ellipse.height(). If ellipse bounds is empty, sets to kEmpty_Type.
     * Otherwise, sets to kEllipse_Type.
     *
     * @param ellipse bounds of ellipse
     */
    public void setEllipse(Rect2fc ellipse) {
        setEllipse(ellipse.left(), ellipse.top(), ellipse.right(), ellipse.bottom());
    }

    /**
     * Sets bounds to ellipse, x-axis radii to half ellipse.width(), and all y-axis radii
     * to half ellipse.height(). If ellipse bounds is empty, sets to kEmpty_Type.
     * Otherwise, sets to kEllipse_Type.
     */
    public void setEllipse(float left, float top, float right, float bottom) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        float radiusX = (float) (((double) -mLeft + mRight) * 0.5);
        float radiusY = (float) (((double) -mTop + mBottom) * 0.5);

        if (radiusX == 0 || radiusY == 0) {
            // all the corners will be square
            Arrays.fill(mRadii, 0);
            mType = kRect_Type;
        } else {
            for (int i = 0; i < 8; i += 2) {
                mRadii[i] = radiusX;
                mRadii[i | 1] = radiusY;
            }
            mType = kEllipse_Type;
        }

        assert isValid();
    }

    /**
     * Sets to ellipse at (cx, cy), x-axis radii to radiusX, and all y-axis radii
     * to radiusY. If ellipse bounds is empty, sets to kEmpty_Type.
     * Otherwise, sets to kEllipse_Type.
     * <p>
     * Due to floating-point errors, only this method can guarantee to produce 'circle'
     * type, if radiusX and radiusY are equal.
     *
     * @param cx      ellipse center on the x-axis
     * @param cy      ellipse center on the y-axis
     * @param radiusX x-axis radius of ellipse
     * @param radiusY y-axis radius of ellipse
     */
    public void setEllipseXY(float cx, float cy, float radiusX, float radiusY) {
        if (!initRect(cx - radiusX, cy - radiusY,
                cx + radiusX, cy + radiusY)) {
            return;
        }

        if (radiusX <= 0 || radiusY <= 0) {
            // all the corners will be square
            Arrays.fill(mRadii, 0);
            mType = kRect_Type;
        } else {
            for (int i = 0; i < 8; i += 2) {
                mRadii[i] = radiusX;
                mRadii[i | 1] = radiusY;
            }
            mType = kEllipse_Type;
        }

        assert isValid();
    }

    /**
     * Sets to rounded rectangle with the same radii for all four corners.
     * If rect is empty, sets to kEmpty_Type.
     * Otherwise, if radiusX or radiusY is zero, sets to kRect_Type.
     * Otherwise, if radiusX is at least half rect.width() and radiusY is at least half
     * rect.height(), sets to kEllipse_Type.
     * Otherwise, sets to kSimple_Type.
     *
     * @param rect    bounds of rounded rectangle
     * @param radiusX x-axis radius of corners
     * @param radiusY y-axis radius of corners
     */
    public void setRectXY(Rect2fc rect, float radiusX, float radiusY) {
        setRectXY(rect.left(), rect.top(), rect.right(), rect.bottom(), radiusX, radiusY);
    }

    /**
     * Sets to rounded rectangle with the same radii for all four corners.
     * If rect is empty, sets to kEmpty_Type.
     * Otherwise, if radiusX or radiusY is zero, sets to kRect_Type.
     * Otherwise, if radiusX is at least half rect.width() and radiusY is at least half
     * rect.height(), sets to kEllipse_Type.
     * Otherwise, sets to kSimple_Type.
     *
     * @param radiusX x-axis radius of corners
     * @param radiusY y-axis radius of corners
     */
    public void setRectXY(float left, float top, float right, float bottom,
                          float radiusX, float radiusY) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        if (!Float.isFinite(radiusX) || !Float.isFinite(radiusY)) {
            radiusX = radiusY = 0; // degenerate into a simple rect
        }

        if (width() < (radiusX + radiusX) || height() < (radiusY + radiusY)) {
            // At most one of these two divides will be by zero, and neither numerator is zero.
            double scale = Math.min(
                    (double) width() / ((double) radiusX + radiusX),
                    (double) height() / ((double) radiusY + radiusY)
            );
            assert (scale < 1) : scale;
            radiusX = (float) ((double) radiusX * scale);
            radiusY = (float) ((double) radiusY * scale);
        }

        if (radiusX <= 0 || radiusY <= 0) {
            // all corners are square in this case
            Arrays.fill(mRadii, 0);
            mType = kRect_Type;
            assert isValid();
            return;
        }

        for (int i = 0; i < 8; i += 2) {
            mRadii[i] = radiusX;
            mRadii[i | 1] = radiusY;
        }
        mType = kSimple_Type;
        if (radiusX >= width() * 0.5f && radiusY >= height() * 0.5f) {
            mType = kEllipse_Type;
        }

        assert isValid();
    }

    /**
     * Sets bounds to rect. Sets radii to (leftRad, topRad), (rightRad, topRad),
     * (rightRad, bottomRad), (leftRad, bottomRad).
     * <p>
     * If rect is empty, sets to kEmpty_Type.
     * Otherwise, if leftRad and rightRad are zero, sets to kRect_Type.
     * Otherwise, if topRad and bottomRad are zero, sets to kRect_Type.
     * Otherwise, if leftRad and rightRad are equal and at least half rect.width(), and
     * topRad and bottomRad are equal at least half rect.height(), sets to kEllipse_Type.
     * Otherwise, if leftRad and rightRad are equal, and topRad and bottomRad are equal,
     * sets to kSimple_Type. Otherwise, sets to kNineSlice_Type.
     * <p>
     * Nine patch refers to the nine parts defined by the radii: one center rectangle,
     * four edge patches, and four corner patches.
     *
     * @param rect      bounds of rounded rectangle
     * @param leftRad   left-top and left-bottom x-axis radius
     * @param topRad    left-top and right-top y-axis radius
     * @param rightRad  right-top and right-bottom x-axis radius
     * @param bottomRad left-bottom and right-bottom y-axis radius
     */
    public void setNineSlice(Rect2fc rect, float leftRad, float topRad,
                             float rightRad, float bottomRad) {
        setNineSlice(rect.left(), rect.top(), rect.right(), rect.bottom(),
                leftRad, topRad, rightRad, bottomRad);
    }

    /**
     * Sets bounds to rect. Sets radii to (leftRad, topRad), (rightRad, topRad),
     * (rightRad, bottomRad), (leftRad, bottomRad).
     * <p>
     * If rect is empty, sets to kEmpty_Type.
     * Otherwise, if leftRad and rightRad are zero, sets to kRect_Type.
     * Otherwise, if topRad and bottomRad are zero, sets to kRect_Type.
     * Otherwise, if leftRad and rightRad are equal and at least half rect.width(), and
     * topRad and bottomRad are equal at least half rect.height(), sets to kEllipse_Type.
     * Otherwise, if leftRad and rightRad are equal, and topRad and bottomRad are equal,
     * sets to kSimple_Type. Otherwise, sets to kNineSlice_Type.
     * <p>
     * Nine patch refers to the nine parts defined by the radii: one center rectangle,
     * four edge patches, and four corner patches.
     *
     * @param leftRad   left-top and left-bottom x-axis radius
     * @param topRad    left-top and right-top y-axis radius
     * @param rightRad  right-top and right-bottom x-axis radius
     * @param bottomRad left-bottom and right-bottom y-axis radius
     */
    public void setNineSlice(float left, float top, float right, float bottom,
                             float leftRad, float topRad, float rightRad, float bottomRad) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        if (!MathUtil.isFinite(leftRad, topRad, rightRad, bottomRad)) {
            // degenerate into a simple rect
            Arrays.fill(mRadii, 0);
            mType = kRect_Type;
            assert isValid();
            return;
        }

        leftRad = Math.max(leftRad, 0.0f);
        topRad = Math.max(topRad, 0.0f);
        rightRad = Math.max(rightRad, 0.0f);
        bottomRad = Math.max(bottomRad, 0.0f);

        double scale = 1;
        if (leftRad + rightRad > width()) {
            scale = (double) width() / ((double) leftRad + rightRad);
        }
        if (topRad + bottomRad > height()) {
            scale = Math.min(scale, (double) height() / ((double) topRad + bottomRad));
        }

        if (scale < 1) {
            leftRad = (float) ((double) leftRad * scale);
            topRad = (float) ((double) topRad * scale);
            rightRad = (float) ((double) rightRad * scale);
            bottomRad = (float) ((double) bottomRad * scale);
        }

        if (leftRad == rightRad && topRad == bottomRad) {
            if (leftRad >= width() * 0.5f && topRad >= height() * 0.5f) {
                mType = kEllipse_Type;
            } else if (leftRad == 0 || topRad == 0) {
                // If the left and (by equality check above) right radii are zero then it is a rect.
                // Same goes for top/bottom.
                mType = kRect_Type;
                leftRad = 0;
                topRad = 0;
                rightRad = 0;
                bottomRad = 0;
            } else {
                mType = kSimple_Type;
            }
        } else {
            mType = kNineSlice_Type;
        }

        mRadii[0] = leftRad;
        mRadii[1] = topRad;
        mRadii[2] = rightRad;
        mRadii[3] = topRad;
        mRadii[4] = rightRad;
        mRadii[5] = bottomRad;
        mRadii[6] = leftRad;
        mRadii[7] = bottomRad;
        if (clamp_corner_radii(mRadii)) {
            // degenerate into a simple rect
            mType = kRect_Type;
            assert isValid();
            return;
        }
        if (mType == kNineSlice_Type && !radii_are_nine_slice(mRadii)) {
            mType = kComplex_Type;
        }

        //assert isValid() : this;
    }

    /**
     * Initializes Rect. If the passed in rect is not finite or empty the round rect will be fully
     * initialized and false is returned. Otherwise, just Rect is initialized and true is returned.
     */
    private boolean initRect(float left, float top, float right, float bottom) {
        if (MathUtil.isFinite(left, top, right, bottom)) {
            // set to sorted
            mLeft = Math.min(left, right);
            mTop = Math.min(top, bottom);
            mRight = Math.max(left, right);
            mBottom = Math.max(top, bottom);
            // check empty
            if (mLeft < mRight && mTop < mBottom) {
                // not empty
                return true;
            }
            // empty
            Arrays.fill(mRadii, 0);
            mType = kEmpty_Type;
            return false;
        }
        // infinite or NaN
        setEmpty();
        return false;
    }

    /**
     * Returns bounds. Bounds may have zero width or zero height. Bounds right is
     * greater than or equal to left; bounds bottom is greater than or equal to top.
     * Result is identical to getBounds().
     */
    public final void getRect(Rect2f dest) {
        dest.set(mLeft, mTop, mRight, mBottom);
    }

    /**
     * Returns bounds. Bounds may have zero width or zero height. Bounds right is
     * greater than or equal to left; bounds bottom is greater than or equal to top.
     * Result is identical to {@link #getRect}.
     */
    public void getBounds(Rect2f dest) {
        dest.set(mLeft, mTop, mRight, mBottom);
    }

    public boolean isValid() {
        if (!are_rect_and_radii_valid(mLeft, mTop, mRight, mBottom, mRadii)) {
            return false;
        }

        boolean allRadiiZero = (0 == mRadii[0] && 0 == mRadii[1]);
        boolean allCornersSquare = (0 == mRadii[0] || 0 == mRadii[1]);
        boolean allRadiiSame = true;

        for (int i = 2; i < 8; i += 2) {
            if (0 != mRadii[i] || 0 != mRadii[i | 1]) {
                allRadiiZero = false;
            }

            if (mRadii[i] != mRadii[i - 2] || mRadii[i | 1] != mRadii[i - 1]) {
                allRadiiSame = false;
            }

            if (0 != mRadii[i] && 0 != mRadii[i | 1]) {
                allCornersSquare = false;
            }
        }
        boolean isNineSlice = radii_are_nine_slice(mRadii);

        if (mType < 0 || mType > kLast_Type) {
            return false;
        }

        boolean isRectEmpty = !(mLeft < mRight && mTop < mBottom);
        switch (mType) {
            case kEmpty_Type:
                if (!isRectEmpty || !allRadiiZero || !allRadiiSame || !allCornersSquare) {
                    return false;
                }
                break;
            case kRect_Type:
                if (isRectEmpty || !allRadiiZero || !allRadiiSame || !allCornersSquare) {
                    return false;
                }
                break;
            case kEllipse_Type:
                if (isRectEmpty || allRadiiZero || !allRadiiSame || allCornersSquare) {
                    return false;
                }

                float halfWidth = halfWidth();
                float halfHeight = halfHeight();
                float xError = Math.max(Math.ulp(mLeft), Math.ulp(mRight)) * 0.5f;
                float yError = Math.max(Math.ulp(mTop), Math.ulp(mBottom)) * 0.5f;
                for (int i = 0; i < 8; i += 2) {
                    if (!MathUtil.isApproxEqual(mRadii[i], halfWidth, xError) ||
                            !MathUtil.isApproxEqual(mRadii[i | 1], halfHeight, yError)) {
                        return false;
                    }
                }
                break;
            case kSimple_Type:
                if (isRectEmpty || allRadiiZero || !allRadiiSame || allCornersSquare) {
                    return false;
                }
                break;
            case kNineSlice_Type:
                if (isRectEmpty || allRadiiZero || allRadiiSame || allCornersSquare ||
                        !isNineSlice) {
                    return false;
                }
                break;
            case kComplex_Type:
                if (isRectEmpty || allRadiiZero || allRadiiSame || allCornersSquare ||
                        isNineSlice) {
                    return false;
                }
                break;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (mLeft != 0.0f ? Float.floatToIntBits(mLeft) : 0);
        result = 31 * result + (mTop != 0.0f ? Float.floatToIntBits(mTop) : 0);
        result = 31 * result + (mRight != 0.0f ? Float.floatToIntBits(mRight) : 0);
        result = 31 * result + (mBottom != 0.0f ? Float.floatToIntBits(mBottom) : 0);
        for (float rad : mRadii) {
            result = 31 * result + (rad != 0.0f ? Float.floatToIntBits(rad) : 0);
        }
        return result;
    }

    /**
     * Returns true if all members in a: Left, Top, Right, Bottom, and Radii; are
     * equal to the corresponding members in b.
     * <p>
     * a and b are not equal if either contain NaN. a and b are equal if members
     * contain zeroes with different signs.
     *
     * @param o rrect to compare
     * @return true if members are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoundRect rr)) {
            return false;
        }
        if (mLeft == rr.mLeft && mTop == rr.mTop &&
                mRight == rr.mRight && mBottom == rr.mBottom) {
            for (int i = 0; i < 8; i++) {
                if (mRadii[i] != rr.mRadii[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RoundRect{");
        sb.append("mRect=(");
        sb.append(mLeft);
        sb.append(", ");
        sb.append(mTop);
        sb.append(", ");
        sb.append(mRight);
        sb.append(", ");
        sb.append(mBottom);
        sb.append("), mRadii={");
        int i = 0;
        for (; ; ) {
            sb.append("(");
            sb.append(mRadii[i]);
            sb.append(", ");
            sb.append(mRadii[i | 1]);
            i += 2;
            if (i < 8) {
                sb.append("), ");
            } else {
                sb.append(")}}");
                break;
            }
        }
        return sb.toString();
    }

    private static boolean clamp_corner_radii(float[] radii) {
        boolean allCornersSquare = true;

        // Clamp negative radii to zero
        for (int i = 0; i < 8; i += 2) {
            if (radii[i] <= 0 || radii[i | 1] <= 0) {
                // In this case we are being a little fast & loose. Since one of
                // the radii is 0 the corner is square. However, the other radii
                // could still be non-zero and play in the global scale factor
                // computation.
                radii[i] = 0;
                radii[i | 1] = 0;
            } else {
                allCornersSquare = false;
            }
        }

        return allCornersSquare;
    }

    private static boolean radii_are_nine_slice(float[] radii) {
        // ULX == LLX
        // ULY == URY
        // URX == LRX
        // LLY == LRY
        return radii[0] == radii[6] &&
                radii[1] == radii[3] &&
                radii[2] == radii[4] &&
                radii[7] == radii[5];
    }

    private static boolean is_radius_valid(
            float rad, float min, float max
    ) {
        return (min <= max) && (rad <= max - min) &&
                (min + rad <= max) && (max - rad >= min) &&
                rad >= 0;
    }

    private static boolean are_rect_and_radii_valid(
            float l, float t, float r, float b, float[] radii
    ) {
        if (!MathUtil.isFinite(l, t, r, b)) {
            return false;
        }
        if (!(l <= r && t <= b)) {
            // not sorted
            return false;
        }
        for (int i = 0; i < 8; i += 2) {
            if (!is_radius_valid(radii[i], l, r) ||
                    !is_radius_valid(radii[i | 1], t, b)) {
                return false;
            }
        }
        return true;
    }
}
