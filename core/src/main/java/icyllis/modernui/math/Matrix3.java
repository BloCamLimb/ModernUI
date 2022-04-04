/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.math;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a 3x3 row-major matrix.
 */
@SuppressWarnings("unused")
public class Matrix3 implements Cloneable {

    // sequential matrix elements, m(ij) (row, column)
    // directly using primitives will be faster than array in Java
    // [m11 m12 m13]
    // [m21 m22 m23]
    // [m31 m32 m33] <- [m31 m32] represents the origin
    float m11;
    float m12;
    float m13;
    float m21;
    float m22;
    float m23;
    float m31;
    float m32;
    float m33;

    /**
     * Create a zero matrix.
     */
    public Matrix3() {
    }

    /**
     * Create a new identity matrix.
     *
     * @return an identity matrix
     */
    @Nonnull
    public static Matrix3 identity() {
        final Matrix3 mat = new Matrix3();
        mat.m11 = mat.m22 = mat.m33 = 1.0f;
        return mat;
    }

    /**
     * Returns translation contributing to x-axis output.
     *
     * @return horizontal translation factor
     */
    public float getTranslateX() {
        return m31;
    }

    /**
     * Returns translation contributing to y-axis output.
     *
     * @return vertical translation factor
     */
    public float getTranslateY() {
        return m32;
    }

    /**
     * Pre-multiply this matrix by the given matrix.
     * (mat3 * this)
     *
     * @param mat the matrix to multiply
     */
    public void preMul(@Nonnull Matrix3 mat) {
        final float f11 = mat.m11 * m11 + mat.m12 * m21 + mat.m13 * m31;
        final float f12 = mat.m11 * m12 + mat.m12 * m22 + mat.m13 * m32;
        final float f13 = mat.m11 * m13 + mat.m12 * m23 + mat.m13 * m33;
        final float f21 = mat.m21 * m11 + mat.m22 * m21 + mat.m23 * m31;
        final float f22 = mat.m21 * m12 + mat.m22 * m22 + mat.m23 * m32;
        final float f23 = mat.m21 * m13 + mat.m22 * m23 + mat.m23 * m33;
        final float f31 = mat.m31 * m11 + mat.m32 * m21 + mat.m33 * m31;
        final float f32 = mat.m31 * m12 + mat.m32 * m22 + mat.m33 * m32;
        final float f33 = mat.m31 * m13 + mat.m32 * m23 + mat.m33 * m33;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m31 = f31;
        m32 = f32;
        m33 = f33;
    }

    /**
     * Post-multiply this matrix by the given matrix.
     * (this * mat3)
     *
     * @param mat the matrix to multiply
     */
    public void postMul(@Nonnull Matrix3 mat) {
        final float f11 = m11 * mat.m11 + m12 * mat.m21 + m13 * mat.m31;
        final float f12 = m11 * mat.m12 + m12 * mat.m22 + m13 * mat.m32;
        final float f13 = m11 * mat.m13 + m12 * mat.m23 + m13 * mat.m33;
        final float f21 = m21 * mat.m11 + m22 * mat.m21 + m23 * mat.m31;
        final float f22 = m21 * mat.m12 + m22 * mat.m22 + m23 * mat.m32;
        final float f23 = m21 * mat.m13 + m22 * mat.m23 + m23 * mat.m33;
        final float f31 = m31 * mat.m11 + m32 * mat.m21 + m33 * mat.m31;
        final float f32 = m31 * mat.m12 + m32 * mat.m22 + m33 * mat.m32;
        final float f33 = m31 * mat.m13 + m32 * mat.m23 + m33 * mat.m33;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m31 = f31;
        m32 = f32;
        m33 = f33;
    }

    /**
     * Set this matrix to the identity matrix.
     */
    public void setIdentity() {
        m11 = 1.0f;
        m12 = 0.0f;
        m13 = 0.0f;
        m21 = 0.0f;
        m22 = 1.0f;
        m23 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = 1.0f;
    }

    /**
     * Set this matrix elements to be given matrix.
     *
     * @param mat the matrix to copy from
     */
    public void set(@Nonnull Matrix3 mat) {
        m11 = mat.m11;
        m12 = mat.m12;
        m13 = mat.m13;
        m21 = mat.m21;
        m22 = mat.m22;
        m23 = mat.m23;
        m31 = mat.m31;
        m32 = mat.m32;
        m33 = mat.m33;
    }

    /**
     * Compute the determinant of this matrix.
     *
     * @return the determinant of this matrix
     */
    public float determinant() {
        return (m11 * m22 - m12 * m21) * m33 +
                (m13 * m21 - m11 * m23) * m32 +
                (m12 * m23 - m13 * m22) * m31;
    }

    /**
     * Compute the trace of this matrix.
     *
     * @return the trace of this matrix
     */
    public float trace() {
        return m11 + m22 + m33;
    }

    /**
     * Compute the transpose of this matrix.
     */
    public void transpose() {
        float t = m21;
        m21 = m12;
        m12 = t;
        t = m31;
        m31 = m13;
        m13 = t;
        t = m32;
        m32 = m23;
        m23 = t;
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
    public boolean invert(@Nonnull Matrix3 mat) {
        float a = m11 * m22 - m12 * m21;
        float b = m13 * m21 - m11 * m23;
        float c = m12 * m23 - m13 * m22;
        // calc the determinant
        float det = a * m33 + b * m32 + c * m31;
        if (MathUtil.approxZero(det)) {
            return false;
        }
        // calc algebraic cofactor and transpose
        det = 1.0f / det;
        float f11 = (m22 * m33 - m32 * m23) * det; // 11
        float f12 = (m32 * m13 - m12 * m33) * det; // -21
        float f21 = (m31 * m23 - m21 * m33) * det; // -12
        float f22 = (m11 * m33 - m31 * m13) * det; // 22
        float f31 = (m21 * m32 - m31 * m22) * det; // 13
        float f32 = (m31 * m12 - m11 * m32) * det; // -23
        m11 = f11;
        m12 = f12;
        m13 = c * det;
        m21 = f21;
        m22 = f22;
        m23 = b * det;
        m31 = f31;
        m32 = f32;
        m33 = a * det;
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
        m31 += dx * m11 + dy * m21;
        m32 += dx * m12 + dy * m22;
        m33 += dx * m13 + dy * m23;
    }

    /**
     * Post-translates this matrix by given changes. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void postTranslate(float dx, float dy) {
        m11 += dx * m13;
        m12 += dy * m13;
        m21 += dx * m23;
        m22 += dy * m23;
        m31 += dx * m33;
        m32 += dy * m33;
    }

    /**
     * Returns whether this matrix is seen as an affine transformation.
     * Otherwise, there's a perspective projection.
     *
     * @return {@code true} if this matrix is affine.
     */
    public boolean isAffine() {
        return MathUtil.approxZero(m13) &&
                MathUtil.approxZero(m23) &&
                MathUtil.approxEqual(m33, 1.0f);
    }

    /**
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scales, translates, or both.
     */
    public boolean isScaleTranslate() {
        return MathUtil.approxZero(m12, m21, m13, m23) && MathUtil.approxEqual(m33, 1.0f);
    }

    /**
     * Returns whether this matrix is identity, or translates.
     *
     * @return {@code true} if this matrix is identity, or translates
     */
    public boolean isTranslate() {
        return MathUtil.approxZero(m12, m21, m13, m23) && MathUtil.approxEqual(m11, m22, m33, 1.0f);
    }

    /**
     * Returns whether this matrix transforms rect to another rect. If true, this matrix is identity,
     * or/and scales, or mirrors on axes. In all cases, this matrix is affine and may also have translation.
     *
     * @return true if this matrix transform one rect into another
     */
    public boolean isAxisAligned() {
        return isAffine() &&
                ((MathUtil.approxZero(m11) &&
                        MathUtil.approxZero(m22) &&
                        !MathUtil.approxZero(m12) &&
                        !MathUtil.approxZero(m21)) ||
                        (MathUtil.approxZero(m12) &&
                                MathUtil.approxZero(m21) &&
                                !MathUtil.approxZero(m11) &&
                                !MathUtil.approxZero(m22)));
    }

    /**
     * If the last column of the matrix is [0, 0, not_one]^T, we will treat the matrix as if it
     * is in perspective, even though it stills behaves like its affine. If we divide everything
     * by the not_one value, then it will behave the same, but will be treated as affine,
     * and therefore faster (e.g. clients can forward-difference calculations).
     */
    public void normalizePerspective() {
        if (m33 != 1 && m33 != 0 && m13 == 0 && m23 == 0) {
            float inv = 1.0f / m33;
            m11 *= inv;
            m12 *= inv;
            m21 *= inv;
            m22 *= inv;
            m31 *= inv;
            m32 *= inv;
            m33 = 1.0f;
        }
    }

    /**
     * Calculate whether this matrix is approximately equivalent to an identity matrix.
     *
     * @return {@code true} if this matrix is identity.
     */
    public boolean isIdentity() {
        return MathUtil.approxZero(m12, m13, m21) &&
                MathUtil.approxZero(m23, m31, m32) &&
                MathUtil.approxEqual(m11, m22, m33, 1.0f);
    }

    public boolean isEqual(@Nonnull Matrix4 mat) {
        return m11 == mat.m11 &&
                m12 == mat.m12 &&
                m13 == mat.m13 &&
                m21 == mat.m21 &&
                m22 == mat.m22 &&
                m23 == mat.m23 &&
                m31 == mat.m31 &&
                m32 == mat.m32 &&
                m33 == mat.m33;
    }

    /**
     * Returns whether this matrix is equivalent to given matrix.
     *
     * @param mat the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    public boolean approxEqual(@Nullable Matrix3 mat) {
        if (mat == this)
            return true;
        if (mat == null)
            return false;
        return MathUtil.approxEqual(m11, mat.m11) &&
                MathUtil.approxEqual(m12, mat.m12) &&
                MathUtil.approxEqual(m13, mat.m13) &&
                MathUtil.approxEqual(m21, mat.m21) &&
                MathUtil.approxEqual(m22, mat.m22) &&
                MathUtil.approxEqual(m23, mat.m23) &&
                MathUtil.approxEqual(m31, mat.m31) &&
                MathUtil.approxEqual(m32, mat.m32) &&
                MathUtil.approxEqual(m33, mat.m33);
    }

    /**
     * Returns whether this matrix is exactly equal to some other object.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj values.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Matrix3 mat = (Matrix3) o;

        if (!MathUtil.exactlyEqual(mat.m11, m11)) return false;
        if (!MathUtil.exactlyEqual(mat.m12, m12)) return false;
        if (!MathUtil.exactlyEqual(mat.m13, m13)) return false;
        if (!MathUtil.exactlyEqual(mat.m21, m21)) return false;
        if (!MathUtil.exactlyEqual(mat.m22, m22)) return false;
        if (!MathUtil.exactlyEqual(mat.m23, m23)) return false;
        if (!MathUtil.exactlyEqual(mat.m31, m31)) return false;
        if (!MathUtil.exactlyEqual(mat.m32, m32)) return false;
        return MathUtil.exactlyEqual(mat.m33, m33);
    }

    @Override
    public int hashCode() {
        int result = (m11 != 0.0f ? Float.floatToIntBits(m11) : 0);
        result = 31 * result + (m12 != 0.0f ? Float.floatToIntBits(m12) : 0);
        result = 31 * result + (m13 != 0.0f ? Float.floatToIntBits(m13) : 0);
        result = 31 * result + (m21 != 0.0f ? Float.floatToIntBits(m21) : 0);
        result = 31 * result + (m22 != 0.0f ? Float.floatToIntBits(m22) : 0);
        result = 31 * result + (m23 != 0.0f ? Float.floatToIntBits(m23) : 0);
        result = 31 * result + (m31 != 0.0f ? Float.floatToIntBits(m31) : 0);
        result = 31 * result + (m32 != 0.0f ? Float.floatToIntBits(m32) : 0);
        result = 31 * result + (m33 != 0.0f ? Float.floatToIntBits(m33) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("""
                        Matrix3:
                        %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f
                        """,
                m11, m12, m13,
                m21, m22, m23,
                m31, m32, m33);
    }

    /**
     * @return a deep copy of this matrix
     */
    @Nonnull
    public Matrix3 copy() {
        try {
            return (Matrix3) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
