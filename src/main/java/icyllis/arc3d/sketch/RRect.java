/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.core.Size;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.system.MemoryUtil;

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
@NullMarked
public class RRect {

    // these are compile-time constants
    @ApiStatus.Internal
    public static final int kSizeOf = 52;
    @ApiStatus.Internal
    public static final int kAlignOf = 4;

    /**
     * Type describes possible specializations of RRect. Each Type is
     * exclusive; a RRect may only have one type.
     * <p>
     * Type members become progressively less restrictive; larger values of
     * Type have more degrees of freedom than smaller values.
     */
    public static final int
            kEmpty_Type = 0, // zero width or height
            kRect_Type = 1, // non-zero width and height, and zeroed radii
            kOval_Type = 2, // non-zero width and height filled with radii
            kSimple_Type = 3, // non-zero width and height with equal radii
            kNineSlice_Type = 4, // non-zero width and height with axis-aligned radii
            kComplex_Type = 5; // non-zero width and height with arbitrary radii
    @ApiStatus.Internal
    public static final int
            kLast_Type = kComplex_Type;

    /**
     * The radii are stored: top-left, top-right, bottom-right, bottom-left.
     */
    public static final int
            kUpperLeftX = 0,
            kUpperLeftY = 1,
            kUpperRightX = 2,
            kUpperRightY = 3,
            kLowerRightX = 4,
            kLowerRightY = 5,
            kLowerLeftX = 6,
            kLowerLeftY = 7;

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
    public RRect() {
    }

    /**
     * Initializes to copy of other bounds and corner radii.
     */
    public RRect(RRect other) {
        set(other);
    }

    /**
     * Initializes to copy of other bounds.
     */
    public RRect(Rect2fc other) {
        setRect(other);
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

    public boolean isOval() {
        return getType() == kOval_Type;
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
     * lower-left, in that order.
     * <p>
     * Note that: do NOT modify the return value, it is read-only (no copy).
     */
    @Size(8)
    public float[] getRadii() {
        return mRadii;
    }

    public float getRadius(int i) {
        return mRadii[i];
    }

    // unflatten
    @ApiStatus.Internal
    public void set(long p) {
        assert (p & (kAlignOf - 1)) == 0;
        mLeft = MemoryUtil.memGetFloat(p);
        mTop = MemoryUtil.memGetFloat(p + 4);
        mRight = MemoryUtil.memGetFloat(p + 8);
        mBottom = MemoryUtil.memGetFloat(p + 12);
        for (int i = 0; i < 8; i++) {
            p += 4;
            mRadii[i] = MemoryUtil.memGetFloat(p);
        }
        mType = MemoryUtil.memGetInt(p + 4);
    }

    // flatten
    @ApiStatus.Internal
    public void store(long p) {
        assert (p & (kAlignOf - 1)) == 0;
        MemoryUtil.memPutFloat(p, mLeft);
        MemoryUtil.memPutFloat(p + 4, mTop);
        MemoryUtil.memPutFloat(p + 8, mRight);
        MemoryUtil.memPutFloat(p + 12, mBottom);
        // benchmark shows that a custom loop is faster than copyMemory at smaller size
        for (int i = 0; i < 8; i++) {
            p += 4;
            MemoryUtil.memPutFloat(p, mRadii[i]);
        }
        MemoryUtil.memPutInt(p + 4, mType);
    }

    /**
     * Copy the values from src into this object.
     */
    public void set(@NonNull RRect src) {
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
     * Sets bounds to ellipse, x-axis radii to half oval.width(), and all y-axis radii
     * to half oval.height(). If ellipse bounds is empty, sets to kEmpty_Type.
     * Otherwise, sets to kEllipse_Type.
     *
     * @param oval bounds of ellipse
     */
    public void setOval(Rect2fc oval) {
        setOval(oval.left(), oval.top(), oval.right(), oval.bottom());
    }

    /**
     * Sets bounds to ellipse, x-axis radii to half ellipse.width(), and all y-axis radii
     * to half ellipse.height(). If ellipse bounds is empty, sets to kEmpty_Type.
     * Otherwise, sets to kEllipse_Type.
     */
    public void setOval(float left, float top, float right, float bottom) {
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
                mRadii[i + 1] = radiusY;
            }
            mType = kOval_Type;
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
    public void setEllipse(float cx, float cy, float radiusX, float radiusY) {
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
                mRadii[i + 1] = radiusY;
            }
            mType = kOval_Type;
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
            mRadii[i + 1] = radiusY;
        }
        mType = kSimple_Type;
        if (radiusX >= width() * 0.5f && radiusY >= height() * 0.5f) {
            mType = kOval_Type;
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
                mType = kOval_Type;
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
     * Sets bounds to rect. Sets radii array for individual control of all for corners.
     * <p>
     * If rect is empty, sets to kEmpty_Type.
     * Otherwise, if one of each corner radii are zero, sets to kRect_Type.
     * Otherwise, if all x-axis radii are equal and at least half rect.width(), and
     * all y-axis radii are equal at least half rect.height(), sets to kOval_Type.
     * Otherwise, if all x-axis radii are equal, and all y-axis radii are equal,
     * sets to kSimple_Type. Otherwise, sets to kNineSlice_Type.
     *
     * @param rect   bounds of rounded rectangle
     * @param radii corner x-axis and y-axis radii
     */
    public void setRectRadii(Rect2fc rect, float[] radii) {
        setRectRadii(rect.left(), rect.top(), rect.right(), rect.bottom(),
                radii);
    }

    /**
     * Sets bounds to rect. Sets radii array for individual control of all for corners.
     * <p>
     * If rect is empty, sets to kEmpty_Type.
     * Otherwise, if one of each corner radii are zero, sets to kRect_Type.
     * Otherwise, if all x-axis radii are equal and at least half rect.width(), and
     * all y-axis radii are equal at least half rect.height(), sets to kOval_Type.
     * Otherwise, if all x-axis radii are equal, and all y-axis radii are equal,
     * sets to kSimple_Type. Otherwise, sets to kNineSlice_Type.
     *
     * @param radii corner x-axis and y-axis radii
     */
    public void setRectRadii(float left, float top, float right, float bottom,
                             @Size(8) float[] radii) {
        if (!initRect(left, top, right, bottom)) {
            return;
        }

        if (!MathUtil.isFinite(radii, 0, 8)) {
            // degenerate into a simple rect
            Arrays.fill(mRadii, 0);
            mType = kRect_Type;
            assert isValid();
            return;
        }

        System.arraycopy(radii, 0, mRadii, 0, 8);

        if (clamp_corner_radii(mRadii)) {
            mType = kRect_Type;
            assert isValid();
            return;
        }

        scaleRadii();

        if (!isValid()) {
            setRect(left, top, right, bottom);
        }
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

    private boolean scaleRadii() {
        // Proportionally scale down all radii to fit. Find the minimum ratio
        // of a side and the radii on that side (for all four sides) and use
        // that to scale down _all_ the radii. This algorithm is from the
        // W3 spec (http://www.w3.org/TR/css3-background/) section 5.5 - Overlapping
        // Curves:
        // "Let f = min(Li/Si), where i is one of { top, right, bottom, left },
        //   Si is the sum of the two corresponding radii of the corners on side i,
        //   and Ltop = Lbottom = the width of the box,
        //   and Lleft = Lright = the height of the box.
        // If f < 1, then all corner radii are reduced by multiplying them by f."
        double scale = 1.0;

        // The sides of the rectangle may be larger than a float.
        double width = (double)mRight - (double)mLeft;
        double height = (double)mBottom - (double)mTop;
        scale = compute_min_scale(mRadii[0], mRadii[2], width,  scale);
        scale = compute_min_scale(mRadii[3], mRadii[5], height, scale);
        scale = compute_min_scale(mRadii[4], mRadii[6], width,  scale);
        scale = compute_min_scale(mRadii[7], mRadii[1], height, scale);

        flush_to_zero(mRadii, 0,2);
        flush_to_zero(mRadii, 3,5);
        flush_to_zero(mRadii, 4,6);
        flush_to_zero(mRadii, 7,1);

        if (scale < 1.0) {
            adjust_radii(width,  scale, mRadii, 0,2);
            adjust_radii(height, scale, mRadii, 3,5);
            adjust_radii(width,  scale, mRadii, 4,6);
            adjust_radii(height, scale, mRadii, 7,1);
        }

        // adjust radii may set x or y to zero; set companion to zero as well
        clamp_corner_radii(mRadii);

        computeType();

        return scale < 1.0;
    }

    private void computeType() {
        boolean isRectEmpty = !(mLeft < mRight && mTop < mBottom);
        if (isRectEmpty) {
            mType = kEmpty_Type;
            assert isValid();
            return;
        }

        boolean allCornersSquare = (0 == mRadii[0] || 0 == mRadii[1]);
        boolean allRadiiSame = true;

        for (int i = 2; i < 8; i += 2) {
            if (mRadii[i] != mRadii[i - 2] || mRadii[i + 1] != mRadii[i - 1]) {
                allRadiiSame = false;
            }

            if (0 != mRadii[i] && 0 != mRadii[i + 1]) {
                allCornersSquare = false;
            }
        }

        if (allCornersSquare) {
            mType = kEmpty_Type;
            assert isValid();
            return;
        }

        if (allRadiiSame) {
            if (mRadii[0] >= width() * 0.5f &&
                    mRadii[1] >= height() * 0.5f) {
                mType = kOval_Type;
            } else {
                mType = kSimple_Type;
            }
            assert isValid();
            return;
        }

        if (radii_are_nine_slice(mRadii)) {
            mType = kNineSlice_Type;
        } else {
            mType = kComplex_Type;
        }

        if (!isValid()) {
            setRect(mLeft, mTop, mRight, mBottom);
            assert isValid();
        }
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
            if (0 != mRadii[i] || 0 != mRadii[i + 1]) {
                allRadiiZero = false;
            }

            if (mRadii[i] != mRadii[i - 2] || mRadii[i + 1] != mRadii[i - 1]) {
                allRadiiSame = false;
            }

            if (0 != mRadii[i] && 0 != mRadii[i + 1]) {
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
            case kOval_Type:
                if (isRectEmpty || allRadiiZero || !allRadiiSame || allCornersSquare) {
                    return false;
                }

                float halfWidth = halfWidth();
                float halfHeight = halfHeight();
                float xError = Math.max(Math.ulp(mLeft), Math.ulp(mRight)) * 0.5f;
                float yError = Math.max(Math.ulp(mTop), Math.ulp(mBottom)) * 0.5f;
                for (int i = 0; i < 8; i += 2) {
                    if (!MathUtil.isApproxEqual(mRadii[i], halfWidth, xError) ||
                            !MathUtil.isApproxEqual(mRadii[i + 1], halfHeight, yError)) {
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
        if (!(o instanceof RRect rr)) {
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
        StringBuilder sb = new StringBuilder("RRect{");
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
        for (;;) {
            sb.append("(");
            sb.append(mRadii[i]);
            sb.append(", ");
            sb.append(mRadii[i + 1]);
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
            if (radii[i] <= 0 || radii[i + 1] <= 0) {
                // In this case we are being a little fast & loose. Since one of
                // the radii is 0 the corner is square. However, the other radii
                // could still be non-zero and play in the global scale factor
                // computation.
                radii[i] = 0;
                radii[i + 1] = 0;
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
                    !is_radius_valid(radii[i + 1], t, b)) {
                return false;
            }
        }
        return true;
    }

    // These parameters are intentionally double.
    private static double compute_min_scale(double rad1, double rad2, double limit, double curMin) {
        if ((rad1 + rad2) > limit) {
            return Math.max(curMin, limit / (rad1 + rad2));
        }
        return curMin;
    }

    // If we can't distinguish one of the radii relative to the other, force it to zero so it
    // doesn't confuse us later.
    private static void flush_to_zero(float[] radii, int i, int j) {
        assert (radii[i] >= 0);
        assert (radii[j] >= 0);
        if (radii[i] + radii[j] == radii[i]) {
            radii[j] = 0;
        } else if (radii[i] + radii[j] == radii[j]) {
            radii[i] = 0;
        }
    }

    // This code assumes that a and b fit in a float, and therefore the resulting smaller value
    // of a and b will fit in a float. The side of the rectangle may be larger than a float.
    // Scale must be less than or equal to the ratio limit / (*a + *b).
    // This code assumes that NaN and Inf are never passed in.
    private static void adjust_radii(double limit, double scale,
                                     float[] radii, int i, int j) {
        assert scale < 1.0 && scale > 0.0;

        float a = radii[i] = (float)((double)radii[i] * scale);
        float b = radii[j] = (float)((double)radii[j] * scale);

        if (a + b > limit) {
            int maxRadiusIdx;
            float newMinRadius;
            if (a > b) {
                newMinRadius = b;
                maxRadiusIdx = i;
            } else {
                newMinRadius = a;
                maxRadiusIdx = j;
            }

            // newMinRadius must be float in order to give the actual value of the radius.
            // The newMinRadius will always be smaller than limit. The largest that minRadius can be
            // is 1/2 the ratio of minRadius : (minRadius + maxRadius), therefore in the resulting
            // division, minRadius can be no larger than 1/2 limit + ULP.

            float newMaxRadius = (float)(limit - newMinRadius);

            // Reduce newMaxRadius an ulp at a time until it fits. This usually never happens,
            // but if it does it could be 1 or 2 times. In certain pathological cases it could be
            // more. Max iterations seen so far is 17.
            while (newMaxRadius + newMinRadius > limit) {
                newMaxRadius = Math.nextAfter(newMaxRadius, 0.0f);
            }
            radii[maxRadiusIdx] = newMaxRadius;
        }

        assert (radii[i] >= 0.0f && radii[j] >= 0.0f);
        assert (radii[i] + radii[j] <= limit);
    }

    @ApiStatus.Internal
    public static boolean allCornersAreCircular(RRect rr) {
        var radii = rr.mRadii;
        return MathUtil.isApproxEqual(radii[0], radii[1], MathUtil.PATH_TOLERANCE) &&
                MathUtil.isApproxEqual(radii[2], radii[3], MathUtil.PATH_TOLERANCE) &&
                MathUtil.isApproxEqual(radii[4], radii[5], MathUtil.PATH_TOLERANCE) &&
                MathUtil.isApproxEqual(radii[6], radii[7], MathUtil.PATH_TOLERANCE);
    }
}
