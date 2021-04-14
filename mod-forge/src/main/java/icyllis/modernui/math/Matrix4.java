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
 * Represents a 4x4 matrix.
 */
@SuppressWarnings("unused")
public class Matrix4 implements Cloneable {

    private static final float[] sMultiMat = new float[28];

    // matrix elements, m(ij) (row, column)
    // directly use primitive type will be faster than array in Java
    protected float m11;
    protected float m12;
    protected float m13;
    protected float m14;
    protected float m21;
    protected float m22;
    protected float m23;
    protected float m24;
    protected float m31;
    protected float m32;
    protected float m33;
    protected float m34;
    protected float m41;
    protected float m42;
    protected float m43;
    protected float m44;

    /**
     * Create a zero matrix.
     */
    public Matrix4() {
    }

    /**
     * Create a matrix from an array of elements, the ordering is
     * in row major form.
     * <table border="1">
     *   <tr>
     *     <td>a[0]</th>
     *     <td>a[1]</th>
     *     <td>a[2]</th>
     *     <td>a[3]</th>
     *   </tr>
     *   <tr>
     *     <td>a[4]</th>
     *     <td>a[5]</th>
     *     <td>a[6]</th>
     *     <td>a[7]</th>
     *   </tr>
     *   <tr>
     *     <td>a[8]</th>
     *     <td>a[9]</th>
     *     <td>a[10]</th>
     *     <td>a[11]</th>
     *   </tr>
     *   <tr>
     *     <td>a[12]</th>
     *     <td>a[13]</th>
     *     <td>a[14]</th>
     *     <td>a[15]</th>
     *   </tr>
     * </table>
     *
     * @param arr the data to copy from
     */
    public Matrix4(@Nonnull float... arr) {
        set(arr);
    }

    /**
     * Create a copy of {@code mat} if it's not null, or a zero matrix otherwise.
     *
     * @param mat the matrix to copy from
     * @return a copy of the matrix
     */
    @Nonnull
    public static Matrix4 copy(@Nullable Matrix4 mat) {
        return mat == null ? new Matrix4() : mat.clone();
    }

    /**
     * Create a new identity matrix.
     *
     * @return an identity matrix
     */
    @Nonnull
    public static Matrix4 identity() {
        Matrix4 mat = new Matrix4();
        mat.m11 = mat.m22 = mat.m33 = mat.m44 = 1.0f;
        return mat;
    }

    /**
     * Create an orthographic projection matrix.
     *
     * @param left   the left frustum plane
     * @param right  the right frustum plane
     * @param bottom the bottom frustum plane
     * @param top    the top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makeOrthographic(float left, float right, float bottom, float top, float near, float far) {
        Matrix4 mat = new Matrix4();
        float invRL = 1.0f / (right - left);
        float invTB = 1.0f / (top - bottom);
        float invNF = 1.0f / (near - far);
        mat.m11 = 2.0f * invRL;
        mat.m22 = 2.0f * invTB;
        mat.m33 = 2.0f * invNF;
        mat.m41 = -(right + left) * invRL;
        mat.m42 = -(top + bottom) * invTB;
        mat.m43 = (near + far) * invNF;
        mat.m44 = 1.0f;
        return mat;
    }

    /**
     * Create an orthographic projection matrix. The left plane and top plane
     * of it is considered to be 0.
     *
     * @param width  the distance from right frustum plane to left frustum plane
     * @param height the distance from bottom frustum plane to top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makeOrthographic(float width, float height, float near, float far) {
        Matrix4 mat = new Matrix4();
        float invNF = 1.0f / (near - far);
        mat.m11 = 2.0f / width;
        mat.m22 = 2.0f / height;
        mat.m33 = 2.0f * invNF;
        mat.m41 = -1.0f;
        mat.m42 = -1.0f;
        mat.m43 = (near + far) * invNF;
        mat.m44 = 1.0f;
        return mat;
    }

    /**
     * Create a perspective projection matrix.
     *
     * @param left   the left frustum plane
     * @param right  the right frustum plane
     * @param bottom the bottom frustum plane
     * @param top    the top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makePerspective(float left, float right, float bottom, float top, float near, float far) {
        Matrix4 mat = new Matrix4();
        float invRL = 1.0f / (right - left);
        float invTB = 1.0f / (top - bottom);
        float invNF = 1.0f / (near - far);
        float tNear = 2.0f * near;
        mat.m11 = tNear * invRL;
        mat.m22 = tNear * invTB;
        mat.m31 = (right + left) * invRL;
        mat.m32 = (top + bottom) * invTB;
        mat.m33 = (near + far) * invNF;
        mat.m34 = -1.0f;
        mat.m43 = tNear * far * invNF;
        return mat;
    }

    /**
     * Create a perspective projection matrix.
     *
     * @param fov    the angle of field of view in radians (0,PI)
     * @param aspect aspect ratio of the view (width / height)
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makePerspective(float fov, float aspect, float near, float far) {
        Matrix4 mat = new Matrix4();
        float y = 1.0f / MathUtil.tan(fov * 0.5f);
        float invNF = 1.0f / (near - far);
        mat.m11 = y / aspect;
        mat.m22 = y;
        mat.m33 = (near + far) * invNF;
        mat.m34 = -1.0f;
        mat.m43 = 2.0f * far * near * invNF;
        return mat;
    }

    /**
     * Add each element of the given matrix to the corresponding element of this matrix.
     *
     * @param other the matrix to add to
     */
    public void add(@Nonnull Matrix4 other) {
        m11 += other.m11;
        m12 += other.m12;
        m13 += other.m13;
        m14 += other.m14;
        m21 += other.m21;
        m22 += other.m22;
        m23 += other.m23;
        m24 += other.m24;
        m31 += other.m31;
        m32 += other.m32;
        m33 += other.m33;
        m34 += other.m34;
        m41 += other.m41;
        m42 += other.m42;
        m43 += other.m43;
        m44 += other.m44;
    }

    /**
     * Subtract each element of the given matrix from the corresponding element of this matrix.
     *
     * @param other the matrix to subtract from
     */
    public void subtract(@Nonnull Matrix4 other) {
        m11 -= other.m11;
        m12 -= other.m12;
        m13 -= other.m13;
        m14 -= other.m14;
        m21 -= other.m21;
        m22 -= other.m22;
        m23 -= other.m23;
        m24 -= other.m24;
        m31 -= other.m31;
        m32 -= other.m32;
        m33 -= other.m33;
        m34 -= other.m34;
        m41 -= other.m41;
        m42 -= other.m42;
        m43 -= other.m43;
        m44 -= other.m44;
    }

    /**
     * Multiply each element of this matrix by a factor.
     *
     * @param s the factor to multiply.
     */
    public void multiply(float s) {
        if (s == 1.0f)
            return;
        m11 *= s;
        m12 *= s;
        m13 *= s;
        m14 *= s;
        m21 *= s;
        m22 *= s;
        m23 *= s;
        m24 *= s;
        m31 *= s;
        m32 *= s;
        m33 *= s;
        m34 *= s;
        m41 *= s;
        m42 *= s;
        m43 *= s;
        m44 *= s;
    }

    /**
     * Set this matrix to be the result of pre-multiplying matrix A
     * by matrix B.
     *
     * @param a left hand side matrix
     * @param b right hand side matrix
     */
    public void setMultiply(@Nonnull Matrix4 a, @Nonnull Matrix4 b) {
        if (b.isIdentity()) {
            if (this != a) set(a);
            return;
        }
        final float f11 = a.m11 * b.m11 + a.m12 * b.m21 + a.m13 * b.m31 + a.m14 * b.m41;
        final float f12 = a.m11 * b.m12 + a.m12 * b.m22 + a.m13 * b.m32 + a.m14 * b.m42;
        final float f13 = a.m11 * b.m13 + a.m12 * b.m23 + a.m13 * b.m33 + a.m14 * b.m43;
        final float f14 = a.m11 * b.m14 + a.m12 * b.m24 + a.m13 * b.m34 + a.m14 * b.m44;
        final float f21 = a.m21 * b.m11 + a.m22 * b.m21 + a.m23 * b.m31 + a.m24 * b.m41;
        final float f22 = a.m21 * b.m12 + a.m22 * b.m22 + a.m23 * b.m32 + a.m24 * b.m42;
        final float f23 = a.m21 * b.m13 + a.m22 * b.m23 + a.m23 * b.m33 + a.m24 * b.m43;
        final float f24 = a.m21 * b.m14 + a.m22 * b.m24 + a.m23 * b.m34 + a.m24 * b.m44;
        final float f31 = a.m31 * b.m11 + a.m32 * b.m21 + a.m33 * b.m31 + a.m34 * b.m41;
        final float f32 = a.m31 * b.m12 + a.m32 * b.m22 + a.m33 * b.m32 + a.m34 * b.m42;
        final float f33 = a.m31 * b.m13 + a.m32 * b.m23 + a.m33 * b.m33 + a.m34 * b.m43;
        final float f34 = a.m31 * b.m14 + a.m32 * b.m24 + a.m33 * b.m34 + a.m34 * b.m44;
        final float f41 = a.m41 * b.m11 + a.m42 * b.m21 + a.m43 * b.m31 + a.m44 * b.m41;
        final float f42 = a.m41 * b.m12 + a.m42 * b.m22 + a.m43 * b.m32 + a.m44 * b.m42;
        final float f43 = a.m41 * b.m13 + a.m42 * b.m23 + a.m43 * b.m33 + a.m44 * b.m43;
        final float f44 = a.m41 * b.m14 + a.m42 * b.m24 + a.m43 * b.m34 + a.m44 * b.m44;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m24 = f24;
        m31 = f31;
        m32 = f32;
        m33 = f33;
        m34 = f34;
        m41 = f41;
        m42 = f42;
        m43 = f43;
        m44 = f44;
    }

    /**
     * Pre-multiply this matrix by a 4x4 matrix, whose top left 3x3 is the given
     * 3x3 matrix, and forth row and column are identity. This is useful when
     * multiplying a quaternion, see {@link #rotate(Quaternion)}
     *
     * @param mat the matrix to multiply.
     */
    public void multiply(@Nonnull Matrix3 mat) {
        if (mat.isIdentity())
            return;
        final float f11 = m11 * mat.m11 + m12 * mat.m21 + m13 * mat.m31;
        final float f12 = m11 * mat.m12 + m12 * mat.m22 + m13 * mat.m32;
        final float f13 = m11 * mat.m13 + m12 * mat.m23 + m13 * mat.m33;
        final float f21 = m21 * mat.m11 + m22 * mat.m21 + m23 * mat.m31;
        final float f22 = m21 * mat.m12 + m22 * mat.m22 + m23 * mat.m32;
        final float f23 = m21 * mat.m13 + m22 * mat.m23 + m23 * mat.m33;
        final float f31 = m31 * mat.m11 + m32 * mat.m21 + m33 * mat.m31;
        final float f32 = m31 * mat.m12 + m32 * mat.m22 + m33 * mat.m32;
        final float f33 = m31 * mat.m13 + m32 * mat.m23 + m33 * mat.m33;
        final float f41 = m41 * mat.m11 + m42 * mat.m21 + m43 * mat.m31;
        final float f42 = m41 * mat.m12 + m42 * mat.m22 + m43 * mat.m32;
        final float f43 = m41 * mat.m13 + m42 * mat.m23 + m43 * mat.m33;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m31 = f31;
        m32 = f32;
        m33 = f33;
        m41 = f41;
        m42 = f42;
        m43 = f43;
    }

    /**
     * Pre-multiply this matrix by some other matrix.
     *
     * @param mat the matrix to multiply.
     */
    public void multiply(@Nonnull Matrix4 mat) {
        setMultiply(this, mat);
    }

    /**
     * Post-multiply this matrix by some other matrix.
     *
     * @param mat the matrix to post-multiply.
     */
    public void postMultiply(@Nonnull Matrix4 mat) {
        setMultiply(mat, this);
    }

    /**
     * Set this matrix to the zero matrix.
     */
    public void setZero() {
        m11 = 0.0f;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 0.0f;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = 0.0f;
        m34 = 0.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m43 = 0.0f;
        m44 = 0.0f;
    }

    /**
     * Set this matrix to the identity matrix.
     */
    public void setIdentity() {
        m11 = 1.0f;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 1.0f;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = 1.0f;
        m34 = 0.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m43 = 0.0f;
        m44 = 1.0f;
    }

    /**
     * Set this matrix elements from an array.
     *
     * @param arr the array to copy from
     * @see #Matrix4(float...)
     */
    public void set(@Nonnull float[] arr) {
        if (arr.length < 16)
            throw new IllegalArgumentException("The array length must be at least 16");
        m11 = arr[0];
        m12 = arr[1];
        m13 = arr[2];
        m14 = arr[3];
        m21 = arr[4];
        m22 = arr[5];
        m23 = arr[6];
        m24 = arr[7];
        m31 = arr[8];
        m32 = arr[9];
        m33 = arr[10];
        m34 = arr[11];
        m41 = arr[12];
        m42 = arr[13];
        m43 = arr[14];
        m44 = arr[15];
    }

    /**
     * Set this matrix elements to be some other matrix.
     *
     * @param mat the matrix to copy from
     */
    public void set(@Nonnull Matrix4 mat) {
        m11 = mat.m11;
        m12 = mat.m12;
        m13 = mat.m13;
        m14 = mat.m14;
        m21 = mat.m21;
        m22 = mat.m22;
        m23 = mat.m23;
        m24 = mat.m24;
        m31 = mat.m31;
        m32 = mat.m32;
        m33 = mat.m33;
        m34 = mat.m34;
        m41 = mat.m41;
        m42 = mat.m42;
        m43 = mat.m43;
        m44 = mat.m44;
    }

    /**
     * Put this matrix data into an array.
     *
     * @param arr the array to store
     */
    public void put(@Nonnull float[] arr) {
        if (arr.length < 16)
            throw new IllegalArgumentException("The array length must be at least 16");
        arr[0] = m11;
        arr[1] = m12;
        arr[2] = m13;
        arr[3] = m14;
        arr[4] = m21;
        arr[5] = m22;
        arr[6] = m23;
        arr[7] = m24;
        arr[8] = m31;
        arr[9] = m32;
        arr[10] = m33;
        arr[11] = m34;
        arr[12] = m41;
        arr[13] = m42;
        arr[14] = m43;
        arr[15] = m44;
    }

    /**
     * Calculate the determinant of this matrix. A matrix is invertible
     * if its determinant is not equal to zero.
     *
     * @return the determinant of this matrix.
     */
    public float determinant() {
        return (m11 * m22 - m12 * m21) * (m33 * m44 - m34 * m43) -
                (m11 * m23 - m13 * m21) * (m32 * m44 - m34 * m42) +
                (m11 * m24 - m14 * m21) * (m32 * m43 - m33 * m42) +
                (m12 * m23 - m13 * m22) * (m31 * m44 - m34 * m41) -
                (m12 * m24 - m14 * m22) * (m31 * m43 - m33 * m41) +
                (m13 * m24 - m14 * m23) * (m31 * m42 - m32 * m41);
    }

    /**
     * Calculate the trace of this matrix.
     *
     * @return the trace of this matrix
     */
    public float trace() {
        return m11 + m22 + m33 + m44;
    }

    /**
     * Calculate the transpose of this matrix.
     */
    public void transpose() {
        transpose(this);
    }

    /**
     * Calculate the transpose of this matrix and store the result to result matrix.
     *
     * @param result the matrix for storing the result.
     */
    public void transpose(@Nonnull Matrix4 result) {
        float t = m21;
        result.m21 = m12;
        result.m12 = t;
        t = m31;
        result.m31 = m13;
        result.m13 = t;
        t = m32;
        result.m32 = m23;
        result.m23 = t;
        t = m41;
        result.m41 = m14;
        result.m14 = t;
        t = m42;
        result.m42 = m24;
        result.m24 = t;
        t = m43;
        result.m43 = m34;
        result.m34 = t;
    }

    /**
     * Calculate the adjugate matrix of this matrix, namely the transpose of
     * the algebraic cofactor matrix of this matrix.
     */
    public void adjugate() {
        adjugate(this);
    }

    /**
     * Calculate the adjugate matrix of this matrix, namely the transpose of
     * the algebraic cofactor matrix of this matrix, and store the result to
     * result matrix.
     *
     * @param result the matrix for storing the result.
     */
    public void adjugate(@Nonnull Matrix4 result) {
        // det of [row1,row2,column1,column2]
        float det12 = m11 * m22 - m12 * m21;
        float det13 = m11 * m23 - m13 * m21;
        float det14 = m11 * m24 - m14 * m21;
        float det23 = m12 * m23 - m13 * m22;
        float det24 = m12 * m24 - m14 * m22;
        float det34 = m13 * m24 - m14 * m23;

        // calc algebraic cofactor
        final float f31 = m42 * det34 - m43 * det24 + m44 * det23;
        final float f32 = -m41 * det34 + m43 * det14 - m44 * det13;
        final float f33 = m41 * det24 - m42 * det14 + m44 * det12;
        final float f34 = -m41 * det23 + m42 * det13 - m43 * det12;
        final float f41 = -m32 * det34 + m33 * det24 - m34 * det23;
        final float f42 = m31 * det34 - m33 * det14 + m34 * det13;
        final float f43 = -m31 * det24 + m32 * det14 - m34 * det12;
        final float f44 = m31 * det23 - m32 * det13 + m33 * det12;

        // det of [row3,row4,column1,column2]
        det12 = m31 * m42 - m32 * m41;
        det13 = m31 * m43 - m33 * m41;
        det14 = m31 * m44 - m34 * m41;
        det23 = m32 * m43 - m33 * m42;
        det24 = m32 * m44 - m34 * m42;
        det34 = m33 * m44 - m34 * m43;

        // calc algebraic cofactor
        final float f11 = m22 * det34 - m23 * det24 + m24 * det23;
        final float f12 = -m21 * det34 + m23 * det14 - m24 * det13;
        final float f13 = m21 * det24 - m22 * det14 + m24 * det12;
        final float f14 = -m21 * det23 + m22 * det13 - m23 * det12;
        final float f21 = -m12 * det34 + m13 * det24 - m14 * det23;
        final float f22 = m11 * det34 - m13 * det14 + m14 * det13;
        final float f23 = -m11 * det24 + m12 * det14 - m14 * det12;
        final float f24 = m11 * det23 - m12 * det13 + m13 * det12;

        // transpose cofactor matrix
        result.m11 = f11;
        result.m21 = f12;
        result.m31 = f13;
        result.m41 = f14;
        result.m12 = f21;
        result.m22 = f22;
        result.m32 = f23;
        result.m42 = f24;
        result.m13 = f31;
        result.m23 = f32;
        result.m33 = f33;
        result.m43 = f34;
        result.m14 = f41;
        result.m24 = f42;
        result.m34 = f43;
        result.m44 = f44;
    }

    /**
     * Calculate the adjugate matrix of this matrix, the determinant of this
     * matrix will be returned as well.
     *
     * @return the determinant of this matrix.
     */
    public float adjugateAndDet() {
        return adjugateAndDet(this);
    }

    /**
     * Calculate the adjugate matrix of this matrix, namely the transpose of
     * the algebraic cofactor matrix of this matrix, and store the result to
     * result matrix. The determinant of this matrix will be returned as well.
     *
     * @param result the matrix for storing the result.
     * @return the determinant of this matrix.
     */
    public float adjugateAndDet(@Nonnull Matrix4 result) {
        // det of [row1,row2,column1,column2]
        final float det1_12 = m11 * m22 - m12 * m21;
        final float det1_13 = m11 * m23 - m13 * m21;
        final float det1_14 = m11 * m24 - m14 * m21;
        final float det1_23 = m12 * m23 - m13 * m22;
        final float det1_24 = m12 * m24 - m14 * m22;
        final float det1_34 = m13 * m24 - m14 * m23;

        // calc algebraic cofactor
        final float f31 = m42 * det1_34 - m43 * det1_24 + m44 * det1_23;
        final float f32 = -m41 * det1_34 + m43 * det1_14 - m44 * det1_13;
        final float f33 = m41 * det1_24 - m42 * det1_14 + m44 * det1_12;
        final float f34 = -m41 * det1_23 + m42 * det1_13 - m43 * det1_12;
        final float f41 = -m32 * det1_34 + m33 * det1_24 - m34 * det1_23;
        final float f42 = m31 * det1_34 - m33 * det1_14 + m34 * det1_13;
        final float f43 = -m31 * det1_24 + m32 * det1_14 - m34 * det1_12;
        final float f44 = m31 * det1_23 - m32 * det1_13 + m33 * det1_12;

        // det of [row3,row4,column1,column2]
        final float det3_12 = m31 * m42 - m32 * m41;
        final float det3_13 = m31 * m43 - m33 * m41;
        final float det3_14 = m31 * m44 - m34 * m41;
        final float det3_23 = m32 * m43 - m33 * m42;
        final float det3_24 = m32 * m44 - m34 * m42;
        final float det3_34 = m33 * m44 - m34 * m43;

        // calc algebraic cofactor
        final float f11 = m22 * det3_34 - m23 * det3_24 + m24 * det3_23;
        final float f12 = -m21 * det3_34 + m23 * det3_14 - m24 * det3_13;
        final float f13 = m21 * det3_24 - m22 * det3_14 + m24 * det3_12;
        final float f14 = -m21 * det3_23 + m22 * det3_13 - m23 * det3_12;
        final float f21 = -m12 * det3_34 + m13 * det3_24 - m14 * det3_23;
        final float f22 = m11 * det3_34 - m13 * det3_14 + m14 * det3_13;
        final float f23 = -m11 * det3_24 + m12 * det3_14 - m14 * det3_12;
        final float f24 = m11 * det3_23 - m12 * det3_13 + m13 * det3_12;

        // transpose cofactor matrix
        result.m11 = f11;
        result.m21 = f12;
        result.m31 = f13;
        result.m41 = f14;
        result.m12 = f21;
        result.m22 = f22;
        result.m32 = f23;
        result.m42 = f24;
        result.m13 = f31;
        result.m23 = f32;
        result.m33 = f33;
        result.m43 = f34;
        result.m14 = f41;
        result.m24 = f42;
        result.m34 = f43;
        result.m44 = f44;

        return det1_12 * det3_34 - det1_13 * det3_24 + det1_14 * det3_23 +
                det1_23 * det3_14 - det1_24 * det3_13 + det1_34 * det3_12;
    }

    /**
     * Calculate the inverse of this matrix. This matrix will be the inverse matrix
     * if it is invertible, otherwise it will be the adjugate matrix.
     *
     * @return {@code true} if this matrix is invertible.
     */
    public boolean inverse() {
        return inverse(this);
    }

    /**
     * Calculate the inverse of this matrix. The result matrix will be the inverse
     * matrix if it is invertible, otherwise it will be the adjugate matrix.
     *
     * @return {@code true} if this matrix is invertible.
     */
    public boolean inverse(@Nonnull Matrix4 result) {
        float det = adjugateAndDet(result);
        if (MathUtil.approxZero(det))
            return false;
        if (!MathUtil.approxEqual(det, 1.0f))
            result.multiply(1.0f / det);
        return true;
    }

    /**
     * Calculate whether this matrix is invertible.
     *
     * @return {@code true} if this matrix is invertible
     */
    public boolean invertible() {
        return !MathUtil.approxZero(determinant());
    }

    /**
     * Rotate this matrix by the given quaternion's rotation matrix.
     *
     * @param q the quaternion to multiply with.
     */
    public void rotate(@Nonnull Quaternion q) {
        if (q.lengthSquared() < 1.0e-6f)
            // mul an identity matrix, no change
            return;
        multiply(q.toMatrix3());
    }

    /**
     * Rotate this matrix by a rotation clockwise about the X-axis into this matrix.
     *
     * @param angle the clockwise rotation angle in radians.
     */
    public void rotateX(float angle) {
        final float s = MathUtil.fsin(angle);
        final float c = MathUtil.fcos(angle);
        final float f21 = c * m21 - s * m31;
        final float f22 = c * m22 - s * m32;
        final float f23 = c * m23 - s * m33;
        final float f24 = c * m24 - s * m34;
        m31 = s * m21 + c * m31;
        m32 = s * m22 + c * m32;
        m33 = s * m23 + c * m33;
        m34 = s * m24 + c * m34;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m24 = f24;
    }

    /**
     * Rotate this matrix by a rotation clockwise about the Y-axis into this matrix.
     *
     * @param angle the clockwise rotation angle in radians.
     */
    public void rotateY(float angle) {
        final float s = MathUtil.fsin(angle);
        final float c = MathUtil.fcos(angle);
        final float f11 = c * m11 + s * m31;
        final float f12 = c * m12 + s * m32;
        final float f13 = c * m13 + s * m33;
        final float f14 = c * m14 + s * m34;
        m31 = -s * m11 + c * m31;
        m32 = -s * m12 + c * m32;
        m33 = -s * m13 + c * m33;
        m34 = -s * m14 + c * m34;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
    }

    /**
     * Rotate this matrix by a rotation clockwise about the Z-axis into this matrix.
     *
     * @param angle the clockwise rotation angle in radians.
     */
    public void rotateZ(float angle) {
        final float s = MathUtil.fsin(angle);
        final float c = MathUtil.fcos(angle);
        final float f11 = c * m11 - s * m21;
        final float f12 = c * m12 - s * m22;
        final float f13 = c * m13 - s * m23;
        final float f14 = c * m14 - s * m24;
        m21 = s * m11 + c * m21;
        m22 = s * m12 + c * m22;
        m23 = s * m13 + c * m23;
        m24 = s * m14 + c * m24;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
    }

    /**
     * Calculate whether this matrix is approximately equivalent to a identity matrix.
     *
     * @return {@code true} if this matrix is identity.
     */
    public boolean isIdentity() {
        return MathUtil.approxZero(m12, m13, m14) &&
                MathUtil.approxZero(m21, m23, m24) &&
                MathUtil.approxZero(m31, m32, m34) &&
                MathUtil.approxZero(m41, m42, m43) &&
                MathUtil.approxEqual(m11, m22, m33, m44, 1.0f);
    }

    /**
     * Calculate whether this matrix is approximately equivalent to some other matrix.
     *
     * @param other the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other.
     */
    public boolean isEqual(@Nullable Matrix4 other) {
        if (this == other) return true;
        if (other == null) return false;
        return m11 == other.m11 && m12 == other.m12 && m13 == other.m13 && m14 == other.m14 &&
                m21 == other.m21 && m22 == other.m22 && m23 == other.m23 && m24 == other.m24 &&
                m31 == other.m31 && m32 == other.m32 && m33 == other.m33 && m34 == other.m34 &&
                m41 == other.m41 && m42 == other.m42 && m43 == other.m43 && m44 == other.m44;
    }

    /**
     * Calculate whether this matrix is exactly equal to some other object.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj values.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Matrix4 mat = (Matrix4) o;

        if (!MathUtil.exactlyEqual(mat.m11, m11)) return false;
        if (!MathUtil.exactlyEqual(mat.m12, m12)) return false;
        if (!MathUtil.exactlyEqual(mat.m13, m13)) return false;
        if (!MathUtil.exactlyEqual(mat.m14, m14)) return false;
        if (!MathUtil.exactlyEqual(mat.m21, m21)) return false;
        if (!MathUtil.exactlyEqual(mat.m22, m22)) return false;
        if (!MathUtil.exactlyEqual(mat.m23, m23)) return false;
        if (!MathUtil.exactlyEqual(mat.m24, m24)) return false;
        if (!MathUtil.exactlyEqual(mat.m31, m31)) return false;
        if (!MathUtil.exactlyEqual(mat.m32, m32)) return false;
        if (!MathUtil.exactlyEqual(mat.m33, m33)) return false;
        if (!MathUtil.exactlyEqual(mat.m34, m34)) return false;
        if (!MathUtil.exactlyEqual(mat.m41, m41)) return false;
        if (!MathUtil.exactlyEqual(mat.m42, m42)) return false;
        if (!MathUtil.exactlyEqual(mat.m43, m43)) return false;
        return MathUtil.exactlyEqual(mat.m44, m44);
    }

    @Override
    public int hashCode() {
        int result = (m11 != +0.0f ? Float.floatToIntBits(m11) : 0);
        result = 31 * result + (m12 != +0.0f ? Float.floatToIntBits(m12) : 0);
        result = 31 * result + (m13 != +0.0f ? Float.floatToIntBits(m13) : 0);
        result = 31 * result + (m14 != +0.0f ? Float.floatToIntBits(m14) : 0);
        result = 31 * result + (m21 != +0.0f ? Float.floatToIntBits(m21) : 0);
        result = 31 * result + (m22 != +0.0f ? Float.floatToIntBits(m22) : 0);
        result = 31 * result + (m23 != +0.0f ? Float.floatToIntBits(m23) : 0);
        result = 31 * result + (m24 != +0.0f ? Float.floatToIntBits(m24) : 0);
        result = 31 * result + (m31 != +0.0f ? Float.floatToIntBits(m31) : 0);
        result = 31 * result + (m32 != +0.0f ? Float.floatToIntBits(m32) : 0);
        result = 31 * result + (m33 != +0.0f ? Float.floatToIntBits(m33) : 0);
        result = 31 * result + (m34 != +0.0f ? Float.floatToIntBits(m34) : 0);
        result = 31 * result + (m41 != +0.0f ? Float.floatToIntBits(m41) : 0);
        result = 31 * result + (m42 != +0.0f ? Float.floatToIntBits(m42) : 0);
        result = 31 * result + (m43 != +0.0f ? Float.floatToIntBits(m43) : 0);
        result = 31 * result + (m44 != +0.0f ? Float.floatToIntBits(m44) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Matrix4:" +
                '\n' + m11 +
                ' ' + m12 +
                ' ' + m13 +
                ' ' + m14 +
                '\n' + m21 +
                ' ' + m22 +
                ' ' + m23 +
                ' ' + m24 +
                '\n' + m31 +
                ' ' + m32 +
                ' ' + m33 +
                ' ' + m34 +
                '\n' + m41 +
                ' ' + m42 +
                ' ' + m43 +
                ' ' + m44 +
                '\n';
    }

    @Override
    public Matrix4 clone() {
        try {
            return (Matrix4) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private static void mulMatrix(@Nonnull float[] a, @Nonnull float[] b) {
        float var4 = b[0];
        float var5 = b[1];
        float var6 = b[2];
        float var7 = b[3];
        float var8 = b[4];
        float var9 = b[1 + 4];
        float var10 = b[2 + 4];
        float var11 = b[3 + 4];
        float var12 = b[8];
        float var13 = b[1 + 8];
        float var14 = b[2 + 8];
        float var15 = b[3 + 8];
        float var16 = b[12];
        float var17 = b[1 + 12];
        float var18 = b[2 + 12];
        float var19 = b[3 + 12];
        float var20 = a[0];
        float var21 = a[4];
        float var22 = a[8];
        float var23 = a[12];
        a[0] = var20 * var4 + var21 * var5 + var22 * var6 + var23 * var7;
        a[4] = var20 * var8 + var21 * var9 + var22 * var10 + var23 * var11;
        a[8] = var20 * var12 + var21 * var13 + var22 * var14 + var23 * var15;
        a[12] = var20 * var16 + var21 * var17 + var22 * var18 + var23 * var19;
        var20 = a[1];
        var21 = a[1 + 4];
        var22 = a[1 + 8];
        var23 = a[1 + 12];
        a[1] = var20 * var4 + var21 * var5 + var22 * var6 + var23 * var7;
        a[1 + 4] = var20 * var8 + var21 * var9 + var22 * var10 + var23 * var11;
        a[1 + 8] = var20 * var12 + var21 * var13 + var22 * var14 + var23 * var15;
        a[1 + 12] = var20 * var16 + var21 * var17 + var22 * var18 + var23 * var19;
        var20 = a[2];
        var21 = a[2 + 4];
        var22 = a[2 + 8];
        var23 = a[2 + 12];
        a[2] = var20 * var4 + var21 * var5 + var22 * var6 + var23 * var7;
        a[2 + 4] = var20 * var8 + var21 * var9 + var22 * var10 + var23 * var11;
        a[2 + 8] = var20 * var12 + var21 * var13 + var22 * var14 + var23 * var15;
        a[2 + 12] = var20 * var16 + var21 * var17 + var22 * var18 + var23 * var19;
        var20 = a[3];
        var21 = a[3 + 4];
        var22 = a[3 + 8];
        var23 = a[3 + 12];
        a[3] = var20 * var4 + var21 * var5 + var22 * var6 + var23 * var7;
        a[3 + 4] = var20 * var8 + var21 * var9 + var22 * var10 + var23 * var11;
        a[3 + 8] = var20 * var12 + var21 * var13 + var22 * var14 + var23 * var15;
        a[3 + 12] = var20 * var16 + var21 * var17 + var22 * var18 + var23 * var19;
    }

    // Strassen algorithm
    private static void multiply(@Nonnull float[] a, @Nonnull float[] b, @Nonnull float[] out) {
        float[] temp = sMultiMat;

        float x1, x2, x3, x4, x5, x6, x7;
        float f11, f12, f21, f22, g11, g12, g21, g22;

        // 1
        f11 = a[0] + a[10];
        f12 = a[1] + a[11];
        f21 = a[4] + a[14];
        f22 = a[5] + a[15];
        g11 = b[0] + b[10];
        g12 = b[1] + b[11];
        g21 = b[4] + b[14];
        g22 = b[5] + b[15];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[0] = x1 + x4 - x5 + x7;
        temp[1] = x3 + x5;
        temp[2] = x2 + x4;
        temp[3] = x1 - x2 + x3 + x6;

        // 2
        f11 = a[8] + a[10];
        f12 = a[9] + a[11];
        f21 = a[12] + a[14];
        f22 = a[13] + a[15];
        g11 = b[0];
        g12 = b[1];
        g21 = b[4];
        g22 = b[5];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[4] = x1 + x4 - x5 + x7;
        temp[5] = x3 + x5;
        temp[6] = x2 + x4;
        temp[7] = x1 - x2 + x3 + x6;

        // 3
        f11 = a[0];
        f12 = a[1];
        f21 = a[4];
        f22 = a[5];
        g11 = b[2] - b[10];
        g12 = b[3] - b[11];
        g21 = b[6] - b[14];
        g22 = b[7] - b[15];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[8] = x1 + x4 - x5 + x7;
        temp[9] = x3 + x5;
        temp[10] = x2 + x4;
        temp[11] = x1 - x2 + x3 + x6;

        // 4
        f11 = a[10];
        f12 = a[11];
        f21 = a[14];
        f22 = a[15];
        g11 = b[10] - b[0];
        g12 = b[11] - b[1];
        g21 = b[12] - b[4];
        g22 = b[13] - b[5];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[12] = x1 + x4 - x5 + x7;
        temp[13] = x3 + x5;
        temp[14] = x2 + x4;
        temp[15] = x1 - x2 + x3 + x6;

        // 5
        f11 = a[0] + a[2];
        f12 = a[1] + a[3];
        f21 = a[4] + a[6];
        f22 = a[5] + a[7];
        g11 = b[10];
        g12 = b[11];
        g21 = b[14];
        g22 = b[15];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[16] = x1 + x4 - x5 + x7;
        temp[17] = x3 + x5;
        temp[18] = x2 + x4;
        temp[19] = x1 - x2 + x3 + x6;

        // 6
        f11 = a[10] - a[0];
        f12 = a[11] - a[1];
        f21 = a[12] - a[4];
        f22 = a[13] - a[5];
        g11 = b[0] + b[2];
        g12 = b[1] + b[3];
        g21 = b[4] + b[6];
        g22 = b[5] + b[7];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[20] = x1 + x4 - x5 + x7;
        temp[21] = x3 + x5;
        temp[22] = x2 + x4;
        temp[23] = x1 - x2 + x3 + x6;

        // 7
        f11 = a[2] - a[10];
        f12 = a[3] - a[11];
        f21 = a[6] - a[14];
        f22 = a[7] - a[15];
        g11 = b[8] + b[10];
        g12 = b[9] + b[11];
        g21 = b[12] + b[14];
        g22 = b[13] + b[15];

        x1 = (f11 + f22) * (g11 + g22);
        x2 = (f21 + f22) * g11;
        x3 = f11 * (g12 - g22);
        x4 = f22 * (g21 - g11);
        x5 = (f11 + f12) * g22;
        x6 = (f21 - f11) * (g11 + g12);
        x7 = (f12 - f22) * (g21 + g22);

        temp[24] = x1 + x4 - x5 + x7;
        temp[25] = x3 + x5;
        temp[26] = x2 + x4;
        temp[27] = x1 - x2 + x3 + x6;

        // out
        out[0] = temp[0] + temp[12] - temp[16] + temp[24];
        out[1] = temp[1] + temp[13] - temp[17] + temp[25];
        out[4] = temp[2] + temp[14] - temp[18] + temp[26];
        out[5] = temp[3] + temp[15] - temp[19] + temp[27];

        out[2] = temp[8] + temp[16];
        out[3] = temp[9] + temp[17];
        out[6] = temp[10] + temp[18];
        out[7] = temp[11] + temp[19];

        out[8] = temp[4] + temp[12];
        out[9] = temp[5] + temp[13];
        out[12] = temp[6] + temp[14];
        out[13] = temp[7] + temp[15];

        out[10] = temp[0] - temp[4] + temp[8] + temp[20];
        out[11] = temp[1] - temp[5] + temp[9] + temp[21];
        out[14] = temp[2] - temp[6] + temp[10] + temp[22];
        out[15] = temp[3] - temp[7] + temp[11] + temp[23];
    }
}
