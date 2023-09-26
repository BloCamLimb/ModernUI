/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;

/**
 * This class represents a 3x3 matrix and a 2D transformation, its components
 * correspond to x, y, and w of a 4x4 matrix, where z is discarded.
 * <p>
 * This class also computes a type mask to simplify some operations, while
 * {@link Matrix3} and {@link Matrix4} do not have this feature.
 * <p>
 * The memory layout (order of components) is the same as GLSL's column-major and
 * HLSL's row-major, this is just a difference in naming and writing.
 *
 * @see Matrix3
 * @see Matrix4
 */
//TODO type mask and others are WIP
@SuppressWarnings("unused")
public class Matrix implements Cloneable {

    /**
     * TypeMask
     * <p>
     * Enum of bit fields for mask returned by getType().
     * Used to identify the complexity of Matrix, for optimizations.
     */
    public static final int
            kIdentity_Mask = 0,          // identity; all bits clear
            kTranslate_Mask = 0x01,      // translation
            kScale_Mask = 0x02,          // scale
            kAffine_Mask = 0x04,         // shear or rotate
            kPerspective_Mask = 0x08;    // perspective
    /**
     * Set if the matrix will map a rectangle to another rectangle. This
     * can be true if the matrix is scale-only, or rotates a multiple of
     * 90 degrees.
     * <p>
     * This bit will be set on identity matrices
     */
    private static final int AxisAligned_Mask = 0x10;
    /**
     * Set if the perspective bit is valid even though the rest of
     * the matrix is Unknown.
     */
    private static final int OnlyPerspectiveValid_Mask = 0x40;
    private static final int Unknown_Mask = 0x80;

    // sequential matrix elements, m(ij) (row, column)
    // directly using primitives will be faster than array in Java
    // [m11 m12 m14]
    // [m21 m22 m24]
    // [m41 m42 m44] <- [m41 m42] represents the origin
    protected float m11;
    protected float m12;
    protected float m14;
    protected float m21;
    protected float m22;
    protected float m24;
    protected float m41;
    protected float m42;
    protected float m44;

    protected int mTypeMask;

    /**
     * Create a new identity matrix.
     */
    public Matrix() {
        m11 = m22 = m44 = 1.0f;
        mTypeMask = kIdentity_Mask | AxisAligned_Mask;
    }

    public Matrix(Matrix m) {
        set(m);
    }

    /**
     * Create a new identity matrix.
     *
     * @return an identity matrix
     */
    @Nonnull
    public static Matrix identity() {
        return new Matrix();
    }

    /**
     * Create a new matrix from the given elements.
     *
     * @param scaleX the value of m11
     * @param shearY the value of m12
     * @param persp0 the value of m13
     * @param shearX the value of m21
     * @param scaleY the value of m22
     * @param persp1 the value of m23
     * @param transX the value of m31
     * @param transY the value of m32
     * @param persp2 the value of m33
     * @return the matrix
     */
    @Nonnull
    public static Matrix makeAll(float scaleX, float shearY, float persp0,
                                 float shearX, float scaleY, float persp1,
                                 float transX, float transY, float persp2) {
        final Matrix m = new Matrix();
        m.setAll(scaleX, shearY, persp0, shearX, scaleY, persp1, transX, transY, persp2);
        return m;
    }

    public float m11() {
        return m11;
    }

    public float m12() {
        return m12;
    }

    public float m14() {
        return m14;
    }

    public float m21() {
        return m21;
    }

    public float m22() {
        return m22;
    }

    public float m24() {
        return m24;
    }

    public float m41() {
        return m41;
    }

    public float m42() {
        return m42;
    }

    public float m44() {
        return m44;
    }

    /**
     * Returns scale factor multiplied by x-axis input, contributing to x-axis output.
     * With mapPoints(), scales Point along the x-axis.
     *
     * @return horizontal scale factor
     */
    public float getScaleX() {
        return m11;
    }

    /**
     * Returns scale factor multiplied by y-axis input, contributing to y-axis output.
     * With mapPoints(), scales Point along the y-axis.
     *
     * @return vertical scale factor
     */
    public float getScaleY() {
        return m22;
    }

    /**
     * Returns scale factor multiplied by x-axis input, contributing to y-axis output.
     * With mapPoints(), shears Point along the y-axis.
     * Shearing both axes can rotate Point.
     *
     * @return vertical shear factor
     */
    public float getShearY() {
        return m12;
    }

    /**
     * Returns scale factor multiplied by y-axis input, contributing to x-axis output.
     * With mapPoints(), shears Point along the x-axis.
     * Shearing both axes can rotate Point.
     *
     * @return horizontal shear factor
     */
    public float getShearX() {
        return m21;
    }

    /**
     * Returns translation contributing to x-axis output.
     * With mapPoints(), moves Point along the x-axis.
     *
     * @return horizontal translation factor
     */
    public float getTranslateX() {
        return m41;
    }

    /**
     * Returns translation contributing to y-axis output.
     * With mapPoints(), moves Point along the y-axis.
     *
     * @return vertical translation factor
     */
    public float getTranslateY() {
        return m42;
    }

    /**
     * Returns factor scaling input x-axis relative to input y-axis.
     *
     * @return input x-axis perspective factor
     */
    public float getPerspX() {
        return m14;
    }

    /**
     * Returns factor scaling input y-axis relative to input x-axis.
     *
     * @return input y-axis perspective factor
     */
    public float getPerspY() {
        return m24;
    }

    /**
     * Returns a bit field describing the transformations the matrix may
     * perform. The bit field is computed conservatively, so it may include
     * false positives. For example, when Perspective_Mask is set, all
     * other bits are set.
     *
     * @return Identity_Mask, or combinations of: Translate_Mask, Scale_Mask,
     * Affine_Mask, Perspective_Mask
     */
    public int getType() {
        if ((mTypeMask & Unknown_Mask) != 0) {
            mTypeMask = computeTypeMask();
        }
        // only return the public masks
        return (mTypeMask & 0xF);
    }

    /**
     * Returns true if this matrix is identity.
     *
     * @return {@code true} if this matrix is identity.
     */
    public boolean isIdentity() {
        return getType() == kIdentity_Mask;
    }

    /**
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scales, translates, or both.
     */
    public boolean isScaleTranslate() {
        return (getType() & ~(kScale_Mask | kTranslate_Mask)) == 0;
    }

    /**
     * Returns whether this matrix is identity, or translates.
     *
     * @return {@code true} if this matrix is identity, or translates
     */
    public boolean isTranslate() {
        return (getType() & ~(kTranslate_Mask)) == 0;
    }

    /**
     * Returns whether this matrix transforms rect to another rect. If true, this matrix is identity,
     * or/and scales, or mirrors on axes. In all cases, this matrix is affine and may also have translation.
     *
     * @return true if this matrix transform one rect into another
     */
    public boolean isAxisAligned() {
        if ((mTypeMask & Unknown_Mask) != 0) {
            mTypeMask = computeTypeMask();
        }
        return (mTypeMask & AxisAligned_Mask) != 0;
    }

    /**
     * Returns true if SkMatrix contains only translation, rotation, reflection, and
     * scale. Scale may differ along rotated axes.
     * Returns false if SkMatrix skewing, perspective, or degenerate forms that collapse
     * to a line or point.
     * <p>
     * Preserves right angles, but not requiring that the arms of the angle
     * retain equal lengths.
     *
     * @return true if Matrix only rotates, scales, translates
     */
    public boolean preservesRightAngles() {
        int mask = getType();

        if (mask <= kTranslate_Mask) {
            // identity, translate and/or scale
            return true;
        }
        if ((mask & kPerspective_Mask) != 0) {
            return false;
        }

        float mx = m11;
        float my = m22;
        float sx = m21;
        float sy = m12;

        // check if upper-left 2x2 of matrix is degenerate
        if (MathUtil.isApproxZero(mx * my - sx * sy)) {
            return false;
        }

        return MathUtil.isApproxZero(mx * sx + sy * my);
    }

    /**
     * Returns whether this matrix contains perspective elements.
     *
     * @return true if this matrix is in most general form
     */
    public boolean hasPerspective() {
        if ((mTypeMask & Unknown_Mask) != 0 &&
                (mTypeMask & OnlyPerspectiveValid_Mask) == 0) {
            mTypeMask = computePerspectiveTypeMask();
        }
        return (mTypeMask & kPerspective_Mask) != 0;
    }

    /**
     * Returns true if this matrix contains only translation, rotation, reflection, and
     * uniform scale. Returns false if this matrix contains different scales, skewing,
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
    public boolean isSimilarity() {
        // if identity or translate matrix
        int mask = getType();
        if (mask <= kTranslate_Mask) {
            return true;
        }
        if ((mask & kPerspective_Mask) != 0) {
            return false;
        }

        float mx = m11;
        float my = m22;
        // if no shear, can just compare scale factors
        if ((mask & kAffine_Mask) == 0) {
            return !MathUtil.isApproxZero(mx) && MathUtil.isApproxEqual(Math.abs(mx), Math.abs(my));
        }
        float sx = m21;
        float sy = m12;

        // check if upper-left 2x2 of matrix is degenerate
        if (MathUtil.isApproxZero(mx * my - sx * sy)) {
            return false;
        }

        // upper 2x2 is rotation/reflection + uniform scale if basis vectors
        // are 90 degree rotations of each other
        return (MathUtil.isApproxEqual(mx, my) && MathUtil.isApproxEqual(sx, -sy))
                || (MathUtil.isApproxEqual(mx, -my) && MathUtil.isApproxEqual(sx, sy));
    }

    /**
     * Pre-multiply this matrix by the given matrix.
     * (mat3 * this)
     *
     * @param mat the matrix to multiply
     */
    public void preConcat(@Nonnull Matrix mat) {
        final float f11 = mat.m11 * m11 + mat.m12 * m21 + mat.m14 * m41;
        final float f12 = mat.m11 * m12 + mat.m12 * m22 + mat.m14 * m42;
        final float f13 = mat.m11 * m14 + mat.m12 * m24 + mat.m14 * m44;
        final float f21 = mat.m21 * m11 + mat.m22 * m21 + mat.m24 * m41;
        final float f22 = mat.m21 * m12 + mat.m22 * m22 + mat.m24 * m42;
        final float f23 = mat.m21 * m14 + mat.m22 * m24 + mat.m24 * m44;
        final float f31 = mat.m41 * m11 + mat.m42 * m21 + mat.m44 * m41;
        final float f32 = mat.m41 * m12 + mat.m42 * m22 + mat.m44 * m42;
        final float f33 = mat.m41 * m14 + mat.m42 * m24 + mat.m44 * m44;
        m11 = f11;
        m12 = f12;
        m14 = f13;
        m21 = f21;
        m22 = f22;
        m24 = f23;
        m41 = f31;
        m42 = f32;
        m44 = f33;
    }

    /**
     * Post-multiply this matrix by the given matrix.
     * (this * mat3)
     *
     * @param mat the matrix to multiply
     */
    public void postConcat(@Nonnull Matrix mat) {
        final float f11 = m11 * mat.m11 + m12 * mat.m21 + m14 * mat.m41;
        final float f12 = m11 * mat.m12 + m12 * mat.m22 + m14 * mat.m42;
        final float f13 = m11 * mat.m14 + m12 * mat.m24 + m14 * mat.m44;
        final float f21 = m21 * mat.m11 + m22 * mat.m21 + m24 * mat.m41;
        final float f22 = m21 * mat.m12 + m22 * mat.m22 + m24 * mat.m42;
        final float f23 = m21 * mat.m14 + m22 * mat.m24 + m24 * mat.m44;
        final float f31 = m41 * mat.m11 + m42 * mat.m21 + m44 * mat.m41;
        final float f32 = m41 * mat.m12 + m42 * mat.m22 + m44 * mat.m42;
        final float f33 = m41 * mat.m14 + m42 * mat.m24 + m44 * mat.m44;
        m11 = f11;
        m12 = f12;
        m14 = f13;
        m21 = f21;
        m22 = f22;
        m24 = f23;
        m41 = f31;
        m42 = f32;
        m44 = f33;
    }

    /**
     * Reset this matrix to the identity.
     */
    public void setIdentity() {
        m11 = 1.0f;
        m12 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 1.0f;
        m24 = 0.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m44 = 1.0f;
        mTypeMask = kIdentity_Mask | AxisAligned_Mask;
    }

    /**
     * Store the values of the given matrix into this matrix.
     *
     * @param m the matrix to copy from
     */
    public void set(@Nonnull Matrix m) {
        m11 = m.m11;
        m12 = m.m12;
        m14 = m.m14;
        m21 = m.m21;
        m22 = m.m22;
        m24 = m.m24;
        m41 = m.m41;
        m42 = m.m42;
        m44 = m.m44;
        mTypeMask = m.mTypeMask;
    }

    /**
     * Sets all values from parameters.
     *
     * @param scaleX horizontal scale factor to store
     * @param shearX horizontal skew factor to store
     * @param transX horizontal translation to store
     * @param shearY vertical skew factor to store
     * @param scaleY vertical scale factor to store
     * @param transY vertical translation to store
     * @param persp0 input x-axis values perspective factor to store
     * @param persp1 input y-axis values perspective factor to store
     * @param persp2 perspective scale factor to store
     */
    public void setAll(float scaleX, float shearY, float persp0,
                       float shearX, float scaleY, float persp1,
                       float transX, float transY, float persp2) {
        m11 = scaleX;
        m12 = shearY;
        m14 = persp0;
        m21 = shearX;
        m22 = scaleY;
        m24 = persp1;
        m41 = transX;
        m42 = transY;
        m44 = persp2;
        mTypeMask = Matrix.Unknown_Mask;
    }

    /**
     * Get this matrix data, store them into an address (UNSAFE).
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store
     */
    public void store(long p) {
        MemoryUtil.memPutFloat(p, m11);
        MemoryUtil.memPutFloat(p + 4, m12);
        MemoryUtil.memPutFloat(p + 8, m14);
        MemoryUtil.memPutFloat(p + 12, m21);
        MemoryUtil.memPutFloat(p + 16, m22);
        MemoryUtil.memPutFloat(p + 20, m24);
        MemoryUtil.memPutFloat(p + 24, m41);
        MemoryUtil.memPutFloat(p + 28, m42);
        MemoryUtil.memPutFloat(p + 32, m44);
    }

    /**
     * Get this matrix data, store them into an address (UNSAFE).
     * The data matches std140 layout so it is not tightly packed.
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store, must be aligned
     */
    public void storeAligned(long p) {
        MemoryUtil.memPutFloat(p, m11);
        MemoryUtil.memPutFloat(p + 4, m12);
        MemoryUtil.memPutFloat(p + 8, m14);
        MemoryUtil.memPutFloat(p + 16, m21);
        MemoryUtil.memPutFloat(p + 20, m22);
        MemoryUtil.memPutFloat(p + 24, m24);
        MemoryUtil.memPutFloat(p + 32, m41);
        MemoryUtil.memPutFloat(p + 36, m42);
        MemoryUtil.memPutFloat(p + 40, m44);
    }

    /**
     * Return the determinant of this matrix.
     *
     * @return the determinant
     */
    public float determinant() {
        return (m11 * m22 - m12 * m21) * m44 +
                (m14 * m21 - m11 * m24) * m42 +
                (m12 * m24 - m14 * m22) * m41;
    }

    /**
     * Compute the trace of this matrix.
     *
     * @return the trace of this matrix
     */
    public float trace() {
        return m11 + m22 + m44;
    }

    /**
     * Transpose this matrix.
     */
    public void transpose() {
        final float f12 = m21;
        final float f13 = m41;
        final float f21 = m12;
        final float f23 = m42;
        final float f31 = m14;
        final float f32 = m24;
        m12 = f12;
        m14 = f13;
        m21 = f21;
        m24 = f23;
        m41 = f31;
        m42 = f32;
    }

    /**
     * Compute the inverse of this matrix. This matrix will be inverted
     * if it is invertible, otherwise it keeps the same as before.
     *
     * @return {@code true} if this matrix is invertible.
     */
    public boolean invert() {
        return invert(this);
    }

    /**
     * Compute the inverse of this matrix. The matrix will be inverted
     * if this matrix is invertible, otherwise it keeps the same as before.
     *
     * @param mat the destination matrix
     * @return {@code true} if this matrix is invertible.
     */
    public boolean invert(@Nonnull Matrix mat) {
        float a = m11 * m22 - m12 * m21;
        float b = m14 * m21 - m11 * m24;
        float c = m12 * m24 - m14 * m22;
        // calc the determinant
        float det = a * m44 + b * m42 + c * m41;
        if (MathUtil.isApproxZero(det)) {
            return false;
        }
        // calc algebraic cofactor and transpose
        det = 1.0f / det;
        float f11 = (m22 * m44 - m42 * m24) * det; // 11
        float f12 = (m42 * m14 - m12 * m44) * det; // -21
        float f21 = (m41 * m24 - m21 * m44) * det; // -12
        float f22 = (m11 * m44 - m41 * m14) * det; // 22
        float f31 = (m21 * m42 - m41 * m22) * det; // 13
        float f32 = (m41 * m12 - m11 * m42) * det; // -23
        m11 = f11;
        m12 = f12;
        m14 = c * det;
        m21 = f21;
        m22 = f22;
        m24 = b * det;
        m41 = f31;
        m42 = f32;
        m44 = a * det;
        return true;
    }

    /**
     * Translates this matrix by given changes. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void preTranslate(float dx, float dy) {
        int type = getType();
        if (type <= kTranslate_Mask) {
            m41 += dx;
            m42 += dy;
        } else {
            m41 += dx * m11 + dy * m21;
            m42 += dx * m12 + dy * m22;
            m44 += dx * m14 + dy * m24;
        }
        if (m41 != 0 || m42 != 0) {
            mTypeMask |= kTranslate_Mask;
        } else {
            mTypeMask &= ~kTranslate_Mask;
        }
    }

    /**
     * Post-translates this matrix by given changes. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void postTranslate(float dx, float dy) {
        int type = getType();
        if (type <= kTranslate_Mask) {
            m41 += dx;
            m42 += dy;
        } else {
            m11 += dx * m14;
            m12 += dy * m14;
            m21 += dx * m24;
            m22 += dy * m24;
            m41 += dx * m44;
            m42 += dy * m44;
        }
        if (m41 != 0 || m42 != 0) {
            mTypeMask |= kTranslate_Mask;
        } else {
            mTypeMask &= ~kTranslate_Mask;
        }
    }

    /**
     * Set this matrix to be a simple translation matrix.
     *
     * @param x the x-component of the translation
     * @param y the y-component of the translation
     */
    public void setTranslate(float x, float y) {
        m11 = 1.0f;
        m12 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 1.0f;
        m24 = 0.0f;
        m41 = x;
        m42 = y;
        m44 = 1.0f;
        if (x != 0 || y != 0) {
            mTypeMask = kTranslate_Mask | AxisAligned_Mask;
        } else {
            mTypeMask = kIdentity_Mask | AxisAligned_Mask;
        }
    }

    /**
     * Apply scaling to <code>this</code> matrix by scaling the base axes by the given x,
     * y and z factors and store the result in <code>dest</code>.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix,
     * then the new matrix will be <code>S * M</code> (row-major). So when transforming a
     * vector <code>v</code> with the new matrix by using <code>v * S * M</code>,
     * the scaling will be applied first.
     *
     * @param sx the x-component of the scale
     * @param sy the y-component of the scale
     */
    public void preScale(float sx, float sy) {
        m11 *= sx;
        m12 *= sx;
        m14 *= sx;
        m21 *= sy;
        m22 *= sy;
        m24 *= sy;
        mTypeMask = Unknown_Mask;
    }

    /**
     * Sets dst to bounds of src corners mapped by Matrix.
     * Returns true if mapped corners are dst corners.
     */
    //@formatter:off
    public final boolean mapRect(Rect2f src, Rect2f dst) {
        int typeMask = getType();
        if (typeMask <= kTranslate_Mask) {
            dst.mLeft   = src.mLeft   + m41;
            dst.mTop    = src.mTop    + m42;
            dst.mRight  = src.mRight  + m41;
            dst.mBottom = src.mBottom + m42;
            return true;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            dst.mLeft =   src.mLeft   * m11 + m41;
            dst.mTop =    src.mTop    * m22 + m42;
            dst.mRight =  src.mRight  * m11 + m41;
            dst.mBottom = src.mBottom * m22 + m42;
            return true;
        }
        float x1 = m11 * src.mLeft +  m21 * src.mTop    + m41;
        float y1 = m12 * src.mLeft +  m22 * src.mTop    + m42;
        float x2 = m11 * src.mRight + m21 * src.mTop    + m41;
        float y2 = m12 * src.mRight + m22 * src.mTop    + m42;
        float x3 = m11 * src.mLeft +  m21 * src.mBottom + m41;
        float y3 = m12 * src.mLeft +  m22 * src.mBottom + m42;
        float x4 = m11 * src.mRight + m21 * src.mBottom + m41;
        float y4 = m12 * src.mRight + m22 * src.mBottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * src.mLeft  + m24 * src.mTop    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * src.mRight + m24 * src.mTop    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * src.mLeft  + m24 * src.mBottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * src.mRight + m24 * src.mBottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dst.mLeft   = MathUtil.min(x1, x2, x3, x4);
        dst.mTop    = MathUtil.min(y1, y2, y3, y4);
        dst.mRight  = MathUtil.max(x1, x2, x3, x4);
        dst.mBottom = MathUtil.max(y1, y2, y3, y4);
        return (typeMask & AxisAligned_Mask) != 0;
    }
    //@formatter:on

    /**
     * Sets rect to bounds of rect corners mapped by Matrix.
     * Returns true if mapped corners are dst corners.
     */
    public final boolean mapRect(Rect2f rect) {
        return mapRect(rect, rect);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(@Nonnull Rect2f r, @Nonnull Rect2i out) {
        mapRect(r.mLeft, r.mTop, r.mRight, r.mBottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(@Nonnull Rect2i r, @Nonnull Rect2i out) {
        mapRect(r.mLeft, r.mTop, r.mRight, r.mBottom, out);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param out the round values
     */
    public void mapRect(float l, float t, float r, float b, @Nonnull Rect2i out) {
        float x1 = m11 * l + m21 * t + m41;
        float y1 = m12 * l + m22 * t + m42;
        float x2 = m11 * r + m21 * t + m41;
        float y2 = m12 * r + m22 * t + m42;
        float x3 = m11 * l + m21 * b + m41;
        float y3 = m12 * l + m22 * b + m42;
        float x4 = m11 * r + m21 * b + m41;
        float y4 = m12 * r + m22 * b + m42;
        if (hasPerspective()) {
            // project
            float w = 1.0f / (m14 * l + m24 * t + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r + m24 * t + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * l + m24 * b + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r + m24 * b + m44);
            x4 *= w;
            y4 *= w;
        }
        out.mLeft = Math.round(MathUtil.min(x1, x2, x3, x4));
        out.mTop = Math.round(MathUtil.min(y1, y2, y3, y4));
        out.mRight = Math.round(MathUtil.max(x1, x2, x3, x4));
        out.mBottom = Math.round(MathUtil.max(y1, y2, y3, y4));
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param result the round out values
     */
    public void mapRectOut(@Nonnull Rect2i r, @Nonnull Rect2i result) {
        mapRectOut(r.mLeft, r.mTop, r.mRight, r.mBottom, result);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param result the round out values
     */
    public void mapRectOut(float l, float t, float r, float b, @Nonnull Rect2i result) {
        float x1 = m11 * l + m21 * t + m41;
        float y1 = m12 * l + m22 * t + m42;
        float x2 = m11 * r + m21 * t + m41;
        float y2 = m12 * r + m22 * t + m42;
        float x3 = m11 * l + m21 * b + m41;
        float y3 = m12 * l + m22 * b + m42;
        float x4 = m11 * r + m21 * b + m41;
        float y4 = m12 * r + m22 * b + m42;
        if (hasPerspective()) {
            // project
            float w = 1.0f / (m14 * l + m24 * t + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r + m24 * t + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * l + m24 * b + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r + m24 * b + m44);
            x4 *= w;
            y4 *= w;
        }
        result.mLeft = (int) Math.floor(MathUtil.min(x1, x2, x3, x4));
        result.mTop = (int) Math.floor(MathUtil.min(y1, y2, y3, y4));
        result.mRight = (int) Math.ceil(MathUtil.max(x1, x2, x3, x4));
        result.mBottom = (int) Math.ceil(MathUtil.max(y1, y2, y3, y4));
    }

    public void mapPoint(float[] p) {
        float x1 = m11 * p[0] + m21 * p[1] + m41;
        float y1 = m12 * p[0] + m22 * p[1] + m42;
        // project
        float w = 1.0f / (m14 * p[0] + m24 * p[1] + m44);
        x1 *= w;
        y1 *= w;
        p[0] = x1;
        p[1] = y1;
    }

    /**
     * If the last column of the matrix is [0, 0, not_one]^T, we will treat the matrix as if it
     * is in perspective, even though it stills behaves like its affine. If we divide everything
     * by the not_one value, then it will behave the same, but will be treated as affine,
     * and therefore faster (e.g. clients can forward-difference calculations).
     */
    public void normalizePerspective() {
        if (m44 != 1 && m44 != 0 && m14 == 0 && m24 == 0) {
            float inv = 1.0f / m44;
            m11 *= inv;
            m12 *= inv;
            m21 *= inv;
            m22 *= inv;
            m41 *= inv;
            m42 *= inv;
            m44 = 1.0f;
        }
    }

    public boolean equal(@Nonnull Matrix mat) {
        return m11 == mat.m11 &&
                m12 == mat.m12 &&
                m14 == mat.m14 &&
                m21 == mat.m21 &&
                m22 == mat.m22 &&
                m24 == mat.m24 &&
                m41 == mat.m41 &&
                m42 == mat.m42 &&
                m44 == mat.m44;
    }

    /**
     * Returns true if this matrix is exactly equal to the given matrix.
     * In this case, -0.f is not equal to 0.f, NaN is equal to NaN.
     *
     * @param m the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    public boolean equals(@Nonnull Matrix m) {
        if (this == m) return true;
        if (Float.floatToIntBits(m.m11) != Float.floatToIntBits(m11)) return false;
        if (Float.floatToIntBits(m.m12) != Float.floatToIntBits(m12)) return false;
        if (Float.floatToIntBits(m.m14) != Float.floatToIntBits(m14)) return false;
        if (Float.floatToIntBits(m.m21) != Float.floatToIntBits(m21)) return false;
        if (Float.floatToIntBits(m.m22) != Float.floatToIntBits(m22)) return false;
        if (Float.floatToIntBits(m.m24) != Float.floatToIntBits(m24)) return false;
        if (Float.floatToIntBits(m.m41) != Float.floatToIntBits(m41)) return false;
        if (Float.floatToIntBits(m.m42) != Float.floatToIntBits(m42)) return false;
        return Float.floatToIntBits(m.m44) == Float.floatToIntBits(m44);
    }

    private int computeTypeMask() {
        int mask = 0;

        if (m14 != 0 || m24 != 0 || m44 != 1) {
            // Once it is determined that this is a perspective transform,
            // all other flags are moot as far as optimizations are concerned.
            return kTranslate_Mask |
                    kScale_Mask |
                    kAffine_Mask |
                    kPerspective_Mask;
        }

        if (m41 != 0 || m42 != 0) {
            mask |= kTranslate_Mask;
        }

        boolean shearX = m21 != 0;
        boolean shearY = m12 != 0;

        if (shearX || shearY) {
            // The shear components may be scale-inducing, unless we are dealing
            // with a pure rotation.  Testing for a pure rotation is expensive,
            // so we opt for being conservative by always setting the scale bit.
            // along with affine.
            // By doing this, we are also ensuring that matrices have the same
            // type masks as their inverses.
            mask |= kAffine_Mask | kScale_Mask;

            // For axis aligned, in the affine case, we only need check that
            // the primary diagonal is all zeros and that the secondary diagonal
            // is all non-zero.
            if (shearX && shearY && m11 == 0 && m22 == 0) {
                mask |= AxisAligned_Mask;
            }
        } else {
            // Only test for scale explicitly if not affine, since affine sets the
            // scale bit.
            if (m11 != 1 || m22 != 1) {
                mask |= kScale_Mask;
            }

            // Not affine, therefore we already know secondary diagonal is
            // all zeros, so we just need to check that primary diagonal is
            // all non-zero.

            // record if the (p)rimary diagonal is all non-zero
            if (m11 != 0 && m22 != 0) {
                mask |= AxisAligned_Mask;
            }
        }

        return mask;
    }

    private int computePerspectiveTypeMask() {
        if (m14 != 0 || m24 != 0 || m44 != 1) {
            // If this is a perspective transform, we return true for all other
            // transform flags - this does not disable any optimizations, respects
            // the rule that the type mask must be conservative, and speeds up
            // type mask computation.
            return kTranslate_Mask |
                    kScale_Mask |
                    kAffine_Mask |
                    kPerspective_Mask;
        }

        return OnlyPerspectiveValid_Mask | Unknown_Mask;
    }

    /**
     * Returns whether this matrix is equivalent to given matrix.
     *
     * @param m the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    public boolean isApproxEqual(@Nonnull Matrix m) {
        return MathUtil.isApproxEqual(m11, m.m11) &&
                MathUtil.isApproxEqual(m12, m.m12) &&
                MathUtil.isApproxEqual(m14, m.m14) &&
                MathUtil.isApproxEqual(m21, m.m21) &&
                MathUtil.isApproxEqual(m22, m.m22) &&
                MathUtil.isApproxEqual(m24, m.m24) &&
                MathUtil.isApproxEqual(m41, m.m41) &&
                MathUtil.isApproxEqual(m42, m.m42) &&
                MathUtil.isApproxEqual(m44, m.m44);
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(m11);
        result = 31 * result + Float.floatToIntBits(m12);
        result = 31 * result + Float.floatToIntBits(m14);
        result = 31 * result + Float.floatToIntBits(m21);
        result = 31 * result + Float.floatToIntBits(m22);
        result = 31 * result + Float.floatToIntBits(m24);
        result = 31 * result + Float.floatToIntBits(m41);
        result = 31 * result + Float.floatToIntBits(m42);
        result = 31 * result + Float.floatToIntBits(m44);
        return result;
    }

    /**
     * Returns whether this matrix is exactly equal to another matrix.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the o values.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Matrix m)) {
            return false;
        }
        return m11 == m.m11 &&
                m12 == m.m12 &&
                m14 == m.m14 &&
                m21 == m.m21 &&
                m22 == m.m22 &&
                m24 == m.m24 &&
                m41 == m.m41 &&
                m42 == m.m42 &&
                m44 == m.m44;
    }

    @Override
    public String toString() {
        return String.format("""
                        Matrix3:
                        %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f
                        """,
                m11, m12, m14,
                m21, m22, m24,
                m41, m42, m44);
    }

    /**
     * @return a deep copy of this matrix
     */
    @Nonnull
    @Override
    public Matrix clone() {
        try {
            return (Matrix) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
