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

import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Interface to a read-only view of a 3x3 matrix for 2D transformation.
 * This does not mean that the matrix is immutable, it only implies that
 * a method should not change the state of the matrix.
 * <p>
 * {@code Matrix const &matrix}
 *
 * @author BloCamLimb
 * @see Matrix
 */
public sealed interface Matrixc permits Matrix {
    // one implementation is fast

    /**
     * TypeMask
     * <p>
     * Enum of bit fields for mask returned by {@link #getType()}.
     * Used to identify the complexity of Matrix, for optimizations.
     */
    int
            kIdentity_Mask = 0,          // identity; all bits clear
            kTranslate_Mask = 0x01,      // translation
            kScale_Mask = 0x02,          // scale
            kAffine_Mask = 0x04,         // shear or rotate
            kPerspective_Mask = 0x08;    // perspective

    /**
     * Returns a bit field describing the transformations the matrix may
     * perform. The bit field is computed conservatively, so it may include
     * false positives. For example, when Perspective_Mask is set, all
     * other bits are set.
     *
     * @return Identity_Mask, or combinations of: Translate_Mask, Scale_Mask,
     * Affine_Mask, Perspective_Mask
     */
    int getType();

    /**
     * Returns true if this matrix is identity.
     *
     * @return {@code true} if this matrix is identity.
     */
    boolean isIdentity();

    /**
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scales, translates, or both.
     */
    boolean isScaleTranslate();

    /**
     * Returns whether this matrix is identity, or translates.
     *
     * @return {@code true} if this matrix is identity, or translates
     */
    boolean isTranslate();

    /**
     * Returns whether this matrix transforms rect to another rect. If true, this matrix is identity,
     * or/and scales, or mirrors on axes. In all cases, this matrix is affine and may also have translation.
     *
     * @return true if this matrix transform one rect into another
     */
    boolean isAxisAligned();

    /**
     * Returns true if this matrix contains only translation, rotation, reflection, and
     * scale. Scale may differ along rotated axes.<br>
     * Returns false if this matrix shearing, perspective, or degenerate forms that collapse
     * to a line or point.
     * <p>
     * Preserves right angles, but not requiring that the arms of the angle
     * retain equal lengths.
     *
     * @return true if this matrix only rotates, scales, translates
     */
    boolean preservesRightAngles();

    /**
     * Returns whether this matrix contains perspective elements.
     *
     * @return true if this matrix is in most general form
     */
    boolean hasPerspective();

    /**
     * Returns true if this matrix contains only translation, rotation, reflection, and
     * uniform scale. Returns false if this matrix contains different scales, shearing,
     * perspective, or degenerate forms that collapse to a line or point.
     * <p>
     * Describes that the matrix makes rendering with and without the matrix are
     * visually alike; a transformed circle remains a circle. Mathematically, this is
     * referred to as similarity of a Euclidean space, or a similarity transformation.
     * <p>
     * Preserves right angles, keeping the arms of the angle equal lengths.
     *
     * @return true if this matrix only rotates, uniformly scales, translates
     */
    boolean isSimilarity();

    /**
     * Same as {@link #getScaleX()}.
     */
    float m11();

    /**
     * Same as {@link #getShearY()}.
     */
    float m12();

    /**
     * Same as {@link #getPerspX()}.
     */
    float m14();

    /**
     * Same as {@link #getShearX()}.
     */
    float m21();

    /**
     * Same as {@link #getScaleY()}.
     */
    float m22();

    /**
     * Same as {@link #getPerspY()}.
     */
    float m24();

    /**
     * Same as {@link #getTranslateX()}.
     */
    float m41();

    /**
     * Same as {@link #getTranslateY()}.
     */
    float m42();

    /**
     * Returns the last element of the matrix, the perspective bias.
     */
    float m44();

    /**
     * Returns scale factor multiplied by x-axis input, contributing to x-axis output.
     * With mapPoints(), scales Point along the x-axis.
     *
     * @return horizontal scale factor
     */
    float getScaleX();

    /**
     * Returns scale factor multiplied by y-axis input, contributing to y-axis output.
     * With mapPoints(), scales Point along the y-axis.
     *
     * @return vertical scale factor
     */
    float getScaleY();

    /**
     * Returns scale factor multiplied by x-axis input, contributing to y-axis output.
     * With mapPoints(), shears Point along the y-axis.
     * Shearing both axes can rotate Point.
     *
     * @return vertical shear factor
     */
    float getShearY();

    /**
     * Returns scale factor multiplied by y-axis input, contributing to x-axis output.
     * With mapPoints(), shears Point along the x-axis.
     * Shearing both axes can rotate Point.
     *
     * @return horizontal shear factor
     */
    float getShearX();

    /**
     * Returns translation contributing to x-axis output.
     * With mapPoints(), moves Point along the x-axis.
     *
     * @return horizontal translation factor
     */
    float getTranslateX();

    /**
     * Returns translation contributing to y-axis output.
     * With mapPoints(), moves Point along the y-axis.
     *
     * @return vertical translation factor
     */
    float getTranslateY();

    /**
     * Returns factor scaling input x-axis relative to input y-axis.
     *
     * @return input x-axis perspective factor
     */
    float getPerspX();

    /**
     * Returns factor scaling input y-axis relative to input x-axis.
     *
     * @return input y-axis perspective factor
     */
    float getPerspY();

    /**
     * Store this matrix elements to the given matrix.
     *
     * @param dst the matrix to store
     */
    void store(@Nonnull Matrix dst);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the array to store into
     */
    void store(@Nonnull float[] a);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a      the array to store into
     * @param offset the element offset
     */
    void store(@Nonnull float[] a, int offset);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    void store(@Nonnull ByteBuffer a);

    /**
     * Store this matrix into the give float array in row-major order.
     * The data matches std140 layout so it is not tightly packed.
     *
     * @param a the pointer of the array to store
     */
    void storeAligned(@Nonnull ByteBuffer a);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    void store(@Nonnull FloatBuffer a);

    /**
     * Store this matrix into the give float array in row-major order.
     * The data matches std140 layout so it is not tightly packed.
     *
     * @param a the pointer of the array to store
     */
    void storeAligned(@Nonnull FloatBuffer a);

    /**
     * Get this matrix data, store them into an address (UNSAFE).
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store
     */
    void store(@NativeType("void *") long p);

    /**
     * Get this matrix data, store them into an address (UNSAFE).
     * The data matches std140 layout so it is not tightly packed.
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store, must be aligned
     */
    void storeAligned(@NativeType("void *") long p);

    /**
     * Converts this 3x3 matrix to 4x4 matrix, the third row and column are identity.
     * <pre>{@code
     * [ a b c ]      [ a b 0 c ]
     * [ d e f ]  ->  [ d e 0 f ]
     * [ g h i ]      [ 0 0 1 0 ]
     *                [ g h 0 i ]
     * }</pre>
     */
    void toMatrix4(@Nonnull Matrix4 dest);

    /**
     * Converts this 3x3 matrix to 4x4 matrix, the third row and column are identity.
     * <pre>{@code
     * [ a b c ]      [ a b 0 c ]
     * [ d e f ]  ->  [ d e 0 f ]
     * [ g h i ]      [ 0 0 1 0 ]
     *                [ g h 0 i ]
     * }</pre>
     */
    @Nonnull
    Matrix4 toMatrix4();

    /**
     * Compute the inverse of this matrix. The <var>dest</var> matrix will be
     * the inverse of this matrix if this matrix is invertible, otherwise its
     * values will be preserved.
     *
     * @param dest the destination matrix, may be null
     * @return {@code true} if this matrix is invertible.
     */
    boolean invert(@Nullable Matrix dest);

    /**
     * Sets rect to bounds of rect corners mapped by this matrix.
     * Returns true if mapped corners are dst corners.
     */
    default boolean mapRect(@Nonnull Rect2f rect) {
        return mapRect(rect, rect);
    }

    /**
     * Sets dst to bounds of src corners mapped by this matrix.
     * Returns true if mapped corners are dst corners.
     */
    boolean mapRect(@Nonnull Rect2fc src, @Nonnull Rect2f dst);

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     */
    default void mapRect(@Nonnull Rect2i r) {
        mapRect(r.mLeft, r.mTop, r.mRight, r.mBottom, r);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    default void mapRect(@Nonnull Rect2fc r, @Nonnull Rect2i out) {
        mapRect(r.left(), r.top(), r.right(), r.bottom(), out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    default void mapRect(@Nonnull Rect2ic r, @Nonnull Rect2i out) {
        mapRect(r.left(), r.top(), r.right(), r.bottom(), out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param dst the round values
     */
    void mapRect(float left, float top, float right, float bottom, @Nonnull Rect2i dst);

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     */
    default void mapRectOut(@Nonnull Rect2i r) {
        mapRectOut(r.mLeft, r.mTop, r.mRight, r.mBottom, r);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param dst the round out values
     */
    default void mapRectOut(@Nonnull Rect2ic r, @Nonnull Rect2i dst) {
        mapRectOut(r.left(), r.top(), r.right(), r.bottom(), dst);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param dst the round out values
     */
    default void mapRectOut(@Nonnull Rect2fc r, @Nonnull Rect2i dst) {
        mapRectOut(r.left(), r.top(), r.right(), r.bottom(), dst);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param dst the round out values
     */
    void mapRectOut(float left, float top, float right, float bottom, @Nonnull Rect2i dst);

    /**
     * @see #mapPoints(float[], int, float[], int, int)
     */
    default void mapPoint(float[] p) {
        mapPoints(p, 0, p, 0, 1);
    }

    /**
     * @see #mapPoints(float[], int, float[], int, int)
     */
    default void mapPoints(float[] pts) {
        mapPoints(pts, 0, pts, 0, pts.length >> 1);
    }

    /**
     * @see #mapPoints(float[], int, float[], int, int)
     */
    default void mapPoints(float[] pts, int count) {
        mapPoints(pts, 0, pts, 0, count);
    }

    /**
     * @see #mapPoints(float[], int, float[], int, int)
     */
    default void mapPoints(float[] pts, int pos, int count) {
        mapPoints(pts, pos, pts, pos, count);
    }

    /**
     * @see #mapPoints(float[], int, float[], int, int)
     */
    default void mapPoints(float[] src, float[] dst, int count) {
        mapPoints(src, 0, dst, 0, count);
    }

    /**
     * Maps src point array of length count to dst point array of equal or greater
     * length. Points are mapped by multiplying each point by this matrix. Given:
     * <pre>
     *  | A B C |        | x |
     *  | D E F |,  pt = | y |
     *  | G H I |        | 1 |
     * </pre>
     * where
     * <pre>
     *  for (i = 0; i < count; ++i) {
     *      x = src[srcPos + (i << 1)]
     *      y = src[srcPos + (i << 1) + 1]
     *  }
     * </pre>
     * each dst point is computed as:
     * <pre>
     *  |A B C| |x|                               Ax+By+C   Dx+Ey+F
     *  |D E F| |y| = |Ax+By+C Dx+Ey+F Gx+Hy+I| = ------- , -------
     *  |G H I| |1|                               Gx+Hy+I   Gx+Hy+I
     * </pre>
     * <p>
     * src and dst may point to the same array.
     *
     * @param src   points to transform
     * @param dst   array for mapped points
     * @param count number of points to transform
     */
    void mapPoints(float[] src, int srcPos, float[] dst, int dstPos, int count);

    /**
     * Returns the minimum scaling factor of this matrix by decomposing the scaling and
     * shearing elements.<br>
     * Returns -1 if scale factor overflows or this matrix contains perspective.
     *
     * @return minimum scale factor
     */
    float getMinScale();

    /**
     * Returns the maximum scaling factor of this matrix by decomposing the scaling and
     * shearing elements.<br>
     * Returns -1 if scale factor overflows or this matrix contains perspective.
     *
     * @return maximum scale factor
     */
    float getMaxScale();

    /**
     * Returns the minimum scaling factor of this matrix by decomposing the scaling and
     * shearing elements. When this matrix has perspective, the scaling factor is specific
     * to the given point <var>p</var>.<br>
     * Returns -1 if scale factor overflows.
     *
     * @param px the x-coord of point
     * @param py the y-coord of point
     * @return minimum scale factor
     */
    float getMinScale(float px, float py);

    /**
     * Returns the maximum scaling factor of this matrix by decomposing the scaling and
     * shearing elements. When this matrix has perspective, the scaling factor is specific
     * to the given point <var>p</var>.<br>
     * Returns -1 if scale factor overflows.
     *
     * @param px the x-coord of point
     * @param py the y-coord of point
     * @return maximum scale factor
     */
    float getMaxScale(float px, float py);

    /**
     * Returns the differential area scale factor for a local point 'p' that will be transformed
     * by 'm' (which may have perspective). If 'm' does not have perspective, this scale factor is
     * constant regardless of 'p'; when it does have perspective, it is specific to that point.
     * <p>
     * This can be crudely thought of as "device pixel area" / "local pixel area" at 'p'.
     * <p>
     * Returns positive infinity if the transformed homogeneous point has w <= 0.
     * <p>
     * The return value is equivalent to {@link #getMinScale(float, float)} times
     * {@link #getMaxScale(float, float)}.
     *
     * @param px the x-coord of point
     * @param py the y-coord of point
     */
    float differentialAreaScale(float px, float py);

    /**
     * Return the minimum distance needed to move in local (pre-transform) space to ensure that the
     * transformed coordinates are at least 1px away from the original mapped point. This minimum
     * distance is specific to the given local 'bounds' since the scale factors change with
     * perspective.
     * <p>
     * If the bounds will be clipped by the w=0 plane or otherwise is ill-conditioned, this will
     * return positive infinity.
     */
    float localAARadius(Rect2fc bounds);

    /**
     * Return the minimum distance needed to move in local (pre-transform) space to ensure that the
     * transformed coordinates are at least 1px away from the original mapped point. This minimum
     * distance is specific to the given local 'bounds' since the scale factors change with
     * perspective.
     * <p>
     * If the bounds will be clipped by the w=0 plane or otherwise is ill-conditioned, this will
     * return positive infinity.
     */
    float localAARadius(float left, float top, float right, float bottom);

    /**
     * Returns true if all elements of the matrix are finite. Returns false if any
     * element is infinity, or NaN.
     *
     * @return true if matrix has only finite elements
     */
    boolean isFinite();

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);

    @Override
    String toString();

    @Nonnull
    Matrix clone();
}
