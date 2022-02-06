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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.memPutFloat;

/**
 * Represents a 4x4 row-major matrix. Note that GLSL is column-major.
 */
@SuppressWarnings("unused")
public class Matrix4 implements Cloneable {

    // matrix elements, m(ij) (row, column)
    // directly use primitive type will be faster than array in Java
    float m11;
    float m12;
    float m13;
    float m14;
    float m21;
    float m22;
    float m23;
    float m24;
    float m31;
    float m32;
    float m33;
    float m34;
    float m41;
    float m42;
    float m43;
    float m44;

    /**
     * Create a zero matrix.
     *
     * @see #identity()
     */
    public Matrix4() {
    }

    /**
     * Create a matrix from an array of elements, the ordering is
     * in row-major form.
     *
     * @param a the array to create from
     * @see #set(float[])
     */
    public Matrix4(@Nonnull float... a) {
        set(a);
    }

    /**
     * Create a matrix from an existing matrix.
     *
     * @param mat the matrix to create from
     */
    public Matrix4(@Nonnull Matrix4 mat) {
        set(mat);
    }

    /**
     * Create a copy of {@code mat} if it's not null, or a new identity matrix otherwise.
     *
     * @param mat the matrix to copy from
     * @return a copy of the matrix
     */
    @Nonnull
    public static Matrix4 copy(@Nullable Matrix4 mat) {
        return mat == null ? identity() : mat.copy();
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
     * are considered to be 0.
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
        mat.m22 = -2.0f / height;
        mat.m33 = 2.0f * invNF;
        mat.m41 = -1.0f;
        mat.m42 = 1.0f;
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
        float y = (float) (1.0 / Math.tan(fov * 0.5));
        float invNF = 1.0f / (near - far);
        mat.m11 = y / aspect;
        mat.m22 = y;
        mat.m33 = (near + far) * invNF;
        mat.m34 = -1.0f;
        mat.m43 = 2.0f * far * near * invNF;
        return mat;
    }

    /**
     * Create a new translation transformation matrix.
     *
     * @param x the x-component of the translation
     * @param y the y-component of the translation
     * @param z the z-component of the translation
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makeTranslation(float x, float y, float z) {
        Matrix4 mat = new Matrix4();
        mat.m11 = 1.0f;
        mat.m22 = 1.0f;
        mat.m33 = 1.0f;
        mat.m41 = x;
        mat.m42 = y;
        mat.m43 = z;
        mat.m44 = 1.0f;
        return mat;
    }

    /**
     * Add each element of the given matrix to the corresponding element of this matrix.
     *
     * @param o the addend
     */
    public void add(@Nonnull Matrix4 o) {
        m11 += o.m11;
        m12 += o.m12;
        m13 += o.m13;
        m14 += o.m14;
        m21 += o.m21;
        m22 += o.m22;
        m23 += o.m23;
        m24 += o.m24;
        m31 += o.m31;
        m32 += o.m32;
        m33 += o.m33;
        m34 += o.m34;
        m41 += o.m41;
        m42 += o.m42;
        m43 += o.m43;
        m44 += o.m44;
    }

    /**
     * Subtract each element of the given matrix from the corresponding element of this matrix.
     *
     * @param o the subtrahend
     */
    public void sub(@Nonnull Matrix4 o) {
        m11 -= o.m11;
        m12 -= o.m12;
        m13 -= o.m13;
        m14 -= o.m14;
        m21 -= o.m21;
        m22 -= o.m22;
        m23 -= o.m23;
        m24 -= o.m24;
        m31 -= o.m31;
        m32 -= o.m32;
        m33 -= o.m33;
        m34 -= o.m34;
        m41 -= o.m41;
        m42 -= o.m42;
        m43 -= o.m43;
        m44 -= o.m44;
    }

    /**
     * Pre-multiply this matrix by a 4x4 matrix, whose top left 3x3 is the given
     * 3x3 matrix, and forth row and column are identity. (mat3 * this)
     *
     * @param mat the matrix to multiply
     */
    public void multiply(@Nonnull Matrix3 mat) {
        final float f11 = mat.m11 * m11 + mat.m12 * m21 + mat.m13 * m31;
        final float f12 = mat.m11 * m12 + mat.m12 * m22 + mat.m13 * m32;
        final float f13 = mat.m11 * m13 + mat.m12 * m23 + mat.m13 * m33;
        final float f14 = mat.m11 * m14 + mat.m12 * m24 + mat.m13 * m34;
        final float f21 = mat.m21 * m11 + mat.m22 * m21 + mat.m23 * m31;
        final float f22 = mat.m21 * m12 + mat.m22 * m22 + mat.m23 * m32;
        final float f23 = mat.m21 * m13 + mat.m22 * m23 + mat.m23 * m33;
        final float f24 = mat.m21 * m14 + mat.m22 * m24 + mat.m23 * m34;
        final float f31 = mat.m31 * m11 + mat.m32 * m21 + mat.m33 * m31;
        final float f32 = mat.m31 * m12 + mat.m32 * m22 + mat.m33 * m32;
        final float f33 = mat.m31 * m13 + mat.m32 * m23 + mat.m33 * m33;
        final float f34 = mat.m31 * m14 + mat.m32 * m24 + mat.m33 * m34;
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
    }

    /**
     * Post-multiply this matrix by a 4x4 matrix, whose top left 3x3 is the given
     * 3x3 matrix, and forth row and column are identity. (this * mat3)
     *
     * @param mat the matrix to multiply with
     */
    public void postMultiply(@Nonnull Matrix3 mat) {
        float f1 = m11 * mat.m11 + m12 * mat.m21 + m13 * mat.m31;
        float f2 = m11 * mat.m12 + m12 * mat.m22 + m13 * mat.m32;
        float f3 = m11 * mat.m13 + m12 * mat.m23 + m13 * mat.m33;
        m11 = f1;
        m12 = f2;
        m13 = f3;
        f1 = m21 * mat.m11 + m22 * mat.m21 + m23 * mat.m31;
        f2 = m21 * mat.m12 + m22 * mat.m22 + m23 * mat.m32;
        f3 = m21 * mat.m13 + m22 * mat.m23 + m23 * mat.m33;
        m21 = f1;
        m22 = f2;
        m23 = f3;
        f1 = m31 * mat.m11 + m32 * mat.m21 + m33 * mat.m31;
        f2 = m31 * mat.m12 + m32 * mat.m22 + m33 * mat.m32;
        f3 = m31 * mat.m13 + m32 * mat.m23 + m33 * mat.m33;
        m31 = f1;
        m32 = f2;
        m33 = f3;
        f1 = m41 * mat.m11 + m42 * mat.m21 + m43 * mat.m31;
        f2 = m41 * mat.m12 + m42 * mat.m22 + m43 * mat.m32;
        f3 = m41 * mat.m13 + m42 * mat.m23 + m43 * mat.m33;
        m41 = f1;
        m42 = f2;
        m43 = f3;
    }

    /**
     * Pre-multiply this matrix by the given matrix.
     * (mat4 * this)
     *
     * @param mat the matrix to multiply
     */
    public void multiply(@Nonnull Matrix4 mat) {
        final float f11 = mat.m11 * m11 + mat.m12 * m21 + mat.m13 * m31 + mat.m14 * m41;
        final float f12 = mat.m11 * m12 + mat.m12 * m22 + mat.m13 * m32 + mat.m14 * m42;
        final float f13 = mat.m11 * m13 + mat.m12 * m23 + mat.m13 * m33 + mat.m14 * m43;
        final float f14 = mat.m11 * m14 + mat.m12 * m24 + mat.m13 * m34 + mat.m14 * m44;
        final float f21 = mat.m21 * m11 + mat.m22 * m21 + mat.m23 * m31 + mat.m24 * m41;
        final float f22 = mat.m21 * m12 + mat.m22 * m22 + mat.m23 * m32 + mat.m24 * m42;
        final float f23 = mat.m21 * m13 + mat.m22 * m23 + mat.m23 * m33 + mat.m24 * m43;
        final float f24 = mat.m21 * m14 + mat.m22 * m24 + mat.m23 * m34 + mat.m24 * m44;
        final float f31 = mat.m31 * m11 + mat.m32 * m21 + mat.m33 * m31 + mat.m34 * m41;
        final float f32 = mat.m31 * m12 + mat.m32 * m22 + mat.m33 * m32 + mat.m34 * m42;
        final float f33 = mat.m31 * m13 + mat.m32 * m23 + mat.m33 * m33 + mat.m34 * m43;
        final float f34 = mat.m31 * m14 + mat.m32 * m24 + mat.m33 * m34 + mat.m34 * m44;
        final float f41 = mat.m41 * m11 + mat.m42 * m21 + mat.m43 * m31 + mat.m44 * m41;
        final float f42 = mat.m41 * m12 + mat.m42 * m22 + mat.m43 * m32 + mat.m44 * m42;
        final float f43 = mat.m41 * m13 + mat.m42 * m23 + mat.m43 * m33 + mat.m44 * m43;
        final float f44 = mat.m41 * m14 + mat.m42 * m24 + mat.m43 * m34 + mat.m44 * m44;
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
     * Post-multiply this matrix by the given matrix.
     * (this * mat4)
     *
     * @param mat the matrix to multiply
     */
    public void postMultiply(@Nonnull Matrix4 mat) {
        if (this == mat) {
            // need more stack size
            multiply(this);
            return;
        }
        float f1 = m11 * mat.m11 + m12 * mat.m21 + m13 * mat.m31 + m14 * mat.m41;
        float f2 = m11 * mat.m12 + m12 * mat.m22 + m13 * mat.m32 + m14 * mat.m42;
        float f3 = m11 * mat.m13 + m12 * mat.m23 + m13 * mat.m33 + m14 * mat.m43;
        float f4 = m11 * mat.m14 + m12 * mat.m24 + m13 * mat.m34 + m14 * mat.m44;
        m11 = f1;
        m12 = f2;
        m13 = f3;
        m14 = f4;
        f1 = m21 * mat.m11 + m22 * mat.m21 + m23 * mat.m31 + m24 * mat.m41;
        f2 = m21 * mat.m12 + m22 * mat.m22 + m23 * mat.m32 + m24 * mat.m42;
        f3 = m21 * mat.m13 + m22 * mat.m23 + m23 * mat.m33 + m24 * mat.m43;
        f4 = m21 * mat.m14 + m22 * mat.m24 + m23 * mat.m34 + m24 * mat.m44;
        m21 = f1;
        m22 = f2;
        m23 = f3;
        m24 = f4;
        f1 = m31 * mat.m11 + m32 * mat.m21 + m33 * mat.m31 + m34 * mat.m41;
        f2 = m31 * mat.m12 + m32 * mat.m22 + m33 * mat.m32 + m34 * mat.m42;
        f3 = m31 * mat.m13 + m32 * mat.m23 + m33 * mat.m33 + m34 * mat.m43;
        f4 = m31 * mat.m14 + m32 * mat.m24 + m33 * mat.m34 + m34 * mat.m44;
        m31 = f1;
        m32 = f2;
        m33 = f3;
        m34 = f4;
        f1 = m41 * mat.m11 + m42 * mat.m21 + m43 * mat.m31 + m44 * mat.m41;
        f2 = m41 * mat.m12 + m42 * mat.m22 + m43 * mat.m32 + m44 * mat.m42;
        f3 = m41 * mat.m13 + m42 * mat.m23 + m43 * mat.m33 + m44 * mat.m43;
        f4 = m41 * mat.m14 + m42 * mat.m24 + m43 * mat.m34 + m44 * mat.m44;
        m41 = f1;
        m42 = f2;
        m43 = f3;
        m44 = f4;
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
     * @param a the array to copy from
     */
    public void set(@Nonnull float[] a) {
        if (a.length < 16)
            throw new IllegalArgumentException("The array length must be at least 16");
        m11 = a[0];
        m12 = a[1];
        m13 = a[2];
        m14 = a[3];
        m21 = a[4];
        m22 = a[5];
        m23 = a[6];
        m24 = a[7];
        m31 = a[8];
        m32 = a[9];
        m33 = a[10];
        m34 = a[11];
        m41 = a[12];
        m42 = a[13];
        m43 = a[14];
        m44 = a[15];
    }

    /**
     * Set this matrix elements to be given matrix.
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
     * Set this matrix elements from an array.
     *
     * @param a the array to copy from
     * @see #Matrix4(float...)
     */
    public void set(@Nonnull FloatBuffer a) {
        if (a.remaining() < 16)
            throw new IllegalArgumentException("The array length must be at least 16");
        m11 = a.get();
        m12 = a.get();
        m13 = a.get();
        m14 = a.get();
        m21 = a.get();
        m22 = a.get();
        m23 = a.get();
        m24 = a.get();
        m31 = a.get();
        m32 = a.get();
        m33 = a.get();
        m34 = a.get();
        m41 = a.get();
        m42 = a.get();
        m43 = a.get();
        m44 = a.get();
    }

    /**
     * Get this matrix data, store them into an array.
     *
     * @param a the array to store
     */
    public void put(@Nonnull float[] a) {
        if (a.length < 16)
            throw new IllegalArgumentException("The array length must be at least 16");
        a[0] = m11;
        a[1] = m12;
        a[2] = m13;
        a[3] = m14;
        a[4] = m21;
        a[5] = m22;
        a[6] = m23;
        a[7] = m24;
        a[8] = m31;
        a[9] = m32;
        a[10] = m33;
        a[11] = m34;
        a[12] = m41;
        a[13] = m42;
        a[14] = m43;
        a[15] = m44;
    }

    /**
     * Get this matrix data, store them into an array.
     *
     * @param a the pointer of the array to store
     */
    public void put(@Nonnull FloatBuffer a) {
        if (a.remaining() < 16)
            throw new IllegalArgumentException("The array length must be at least 16");
        a.put(m11);
        a.put(m12);
        a.put(m13);
        a.put(m14);
        a.put(m21);
        a.put(m22);
        a.put(m23);
        a.put(m24);
        a.put(m31);
        a.put(m32);
        a.put(m33);
        a.put(m34);
        a.put(m41);
        a.put(m42);
        a.put(m43);
        a.put(m44);
    }

    /**
     * Get this matrix data, store them into an array.
     *
     * @param a the pointer of the array to store
     */
    public void put(@Nonnull ByteBuffer a) {
        if (a.remaining() < 64)
            throw new IllegalArgumentException("The array length must be at least 16");
        a.putFloat(m11);
        a.putFloat(m12);
        a.putFloat(m13);
        a.putFloat(m14);
        a.putFloat(m21);
        a.putFloat(m22);
        a.putFloat(m23);
        a.putFloat(m24);
        a.putFloat(m31);
        a.putFloat(m32);
        a.putFloat(m33);
        a.putFloat(m34);
        a.putFloat(m41);
        a.putFloat(m42);
        a.putFloat(m43);
        a.putFloat(m44);
    }

    /**
     * Get this matrix data, store them into an address (UNSAFE).
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store
     */
    public void put(long p) {
        memPutFloat(p, m11);
        memPutFloat(p + 4, m12);
        memPutFloat(p + 8, m13);
        memPutFloat(p + 12, m14);
        memPutFloat(p + 16, m21);
        memPutFloat(p + 20, m22);
        memPutFloat(p + 24, m23);
        memPutFloat(p + 28, m24);
        memPutFloat(p + 32, m31);
        memPutFloat(p + 36, m32);
        memPutFloat(p + 40, m33);
        memPutFloat(p + 44, m34);
        memPutFloat(p + 48, m41);
        memPutFloat(p + 52, m42);
        memPutFloat(p + 56, m43);
        memPutFloat(p + 60, m44);
    }

    /**
     * Returns the determinant of this matrix. A matrix is invertible
     * if its determinant is not equal to zero.
     *
     * @return the determinant of this matrix
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
     * Returns the trace of this matrix.
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
        float t = m21;
        m21 = m12;
        m12 = t;
        t = m31;
        m31 = m13;
        m13 = t;
        t = m32;
        m32 = m23;
        m23 = t;
        t = m41;
        m41 = m14;
        m14 = t;
        t = m42;
        m42 = m24;
        m24 = t;
        t = m43;
        m43 = m34;
        m34 = t;
    }

    /**
     * Calculate the adjugate matrix of this matrix.
     */
    public void adjugate() {
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
        m11 = f11;
        m21 = f12;
        m31 = f13;
        m41 = f14;
        m12 = f21;
        m22 = f22;
        m32 = f23;
        m42 = f24;
        m13 = f31;
        m23 = f32;
        m33 = f33;
        m43 = f34;
        m14 = f41;
        m24 = f42;
        m34 = f43;
        m44 = f44;
    }

    /**
     * Calculate the inverse of this matrix. This matrix will be inverted
     * if it is invertible, otherwise it keeps the same as before.
     *
     * @return {@code true} if this matrix is invertible.
     */
    public boolean invert() {
        // det of [row1,row2,column1,column2]
        final float det1_12 = m11 * m22 - m12 * m21;
        final float det1_13 = m11 * m23 - m13 * m21;
        final float det1_14 = m11 * m24 - m14 * m21;
        final float det1_23 = m12 * m23 - m13 * m22;
        final float det1_24 = m12 * m24 - m14 * m22;
        final float det1_34 = m13 * m24 - m14 * m23;

        // det of [row3,row4,column1,column2]
        final float det3_12 = m31 * m42 - m32 * m41;
        final float det3_13 = m31 * m43 - m33 * m41;
        final float det3_14 = m31 * m44 - m34 * m41;
        final float det3_23 = m32 * m43 - m33 * m42;
        final float det3_24 = m32 * m44 - m34 * m42;
        final float det3_34 = m33 * m44 - m34 * m43;

        final float det = det1_12 * det3_34 - det1_13 * det3_24 + det1_14 * det3_23 +
                det1_23 * det3_14 - det1_24 * det3_13 + det1_34 * det3_12;
        if (MathUtil.approxZero(det)) {
            return false;
        }

        // calc algebraic cofactor and transpose cofactor matrix
        final float s = 1.0f / det;
        final float f31 = m42 * det1_34 - m43 * det1_24 + m44 * det1_23;
        final float f32 = -m41 * det1_34 + m43 * det1_14 - m44 * det1_13;
        final float f33 = m41 * det1_24 - m42 * det1_14 + m44 * det1_12;
        final float f34 = -m41 * det1_23 + m42 * det1_13 - m43 * det1_12;
        final float f41 = -m32 * det1_34 + m33 * det1_24 - m34 * det1_23;
        final float f42 = m31 * det1_34 - m33 * det1_14 + m34 * det1_13;
        final float f43 = -m31 * det1_24 + m32 * det1_14 - m34 * det1_12;
        final float f44 = m31 * det1_23 - m32 * det1_13 + m33 * det1_12;
        final float f11 = m22 * det3_34 - m23 * det3_24 + m24 * det3_23;
        final float f12 = -m21 * det3_34 + m23 * det3_14 - m24 * det3_13;
        final float f13 = m21 * det3_24 - m22 * det3_14 + m24 * det3_12;
        final float f14 = -m21 * det3_23 + m22 * det3_13 - m23 * det3_12;
        final float f21 = -m12 * det3_34 + m13 * det3_24 - m14 * det3_23;
        final float f22 = m11 * det3_34 - m13 * det3_14 + m14 * det3_13;
        final float f23 = -m11 * det3_24 + m12 * det3_14 - m14 * det3_12;
        final float f24 = m11 * det3_23 - m12 * det3_13 + m13 * det3_12;

        m11 = f11 * s;
        m21 = f12 * s;
        m31 = f13 * s;
        m41 = f14 * s;
        m12 = f21 * s;
        m22 = f22 * s;
        m32 = f23 * s;
        m42 = f24 * s;
        m13 = f31 * s;
        m23 = f32 * s;
        m33 = f33 * s;
        m43 = f34 * s;
        m14 = f41 * s;
        m24 = f42 * s;
        m34 = f43 * s;
        m44 = f44 * s;
        return true;
    }

    /**
     * Calculate the inverse of this matrix.
     *
     * @param out the out matrix
     * @return {@code true} if this matrix is invertible.
     */
    public boolean invert(@Nonnull Matrix4 out) {
        final float a = m11 * m22 - m12 * m21;
        final float b = m11 * m23 - m13 * m21;
        final float c = m11 * m24 - m14 * m21;
        final float d = m12 * m23 - m13 * m22;
        final float e = m12 * m24 - m14 * m22;
        final float f = m13 * m24 - m14 * m23;
        final float g = m31 * m42 - m32 * m41;
        final float h = m31 * m43 - m33 * m41;
        final float i = m31 * m44 - m34 * m41;
        final float j = m32 * m43 - m33 * m42;
        final float k = m32 * m44 - m34 * m42;
        final float l = m33 * m44 - m34 * m43;

        final float det = a * l - b * k + c * j +
                d * i - e * h + f * g;
        if (MathUtil.approxZero(det)) {
            return false;
        }
        // calc algebraic cofactor and transpose cofactor matrix
        final float s = 1.0f / det;
        out.m11 = (m22 * l - m23 * k + m24 * j) * s;
        out.m12 = (-m21 * l + m23 * i - m24 * h) * s;
        out.m13 = (m21 * k - m22 * i + m24 * g) * s;
        out.m14 = (-m21 * j + m22 * h - m23 * g) * s;
        out.m21 = (-m12 * l + m13 * k - m14 * j) * s;
        out.m22 = (m11 * l - m13 * i + m14 * h) * s;
        out.m23 = (-m11 * k + m12 * i - m14 * g) * s;
        out.m24 = (m11 * j - m12 * h + m13 * g) * s;
        out.m31 = (m42 * f - m43 * e + m44 * d) * s;
        out.m32 = (-m41 * f + m43 * c - m44 * b) * s;
        out.m33 = (m41 * e - m42 * c + m44 * a) * s;
        out.m34 = (-m41 * d + m42 * b - m43 * a) * s;
        out.m41 = (-m32 * f + m33 * e - m34 * d) * s;
        out.m42 = (m31 * f - m33 * c + m34 * b) * s;
        out.m43 = (-m31 * e + m32 * c - m34 * a) * s;
        out.m44 = (m31 * d - m32 * b + m33 * a) * s;
        return true;
    }

    /**
     * Returns whether this matrix is invertible.
     *
     * @return {@code true} if this matrix is invertible
     */
    public boolean invertible() {
        return !MathUtil.approxZero(determinant());
    }

    /**
     * Set this matrix to an orthographic projection matrix.
     *
     * @param left   the left frustum plane
     * @param right  the right frustum plane
     * @param bottom the bottom frustum plane
     * @param top    the top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return this
     */
    @Nonnull
    public Matrix4 setOrthographic(float left, float right, float bottom, float top, float near, float far) {
        float invRL = 1.0f / (right - left);
        float invTB = 1.0f / (top - bottom);
        float invNF = 1.0f / (near - far);
        m11 = 2.0f * invRL;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 2.0f * invTB;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = 2.0f * invNF;
        m34 = 0.0f;
        m41 = -(right + left) * invRL;
        m42 = -(top + bottom) * invTB;
        m43 = (near + far) * invNF;
        m44 = 1.0f;
        return this;
    }

    /**
     * Set this matrix to an orthographic projection matrix. The left plane and top plane
     * are considered to be 0.
     *
     * @param width  the distance from right frustum plane to left frustum plane
     * @param height the distance from bottom frustum plane to top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return this
     */
    @Nonnull
    public Matrix4 setOrthographic(float width, float height, float near, float far) {
        float invNF = 1.0f / (near - far);
        m11 = 2.0f / width;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = -2.0f / height;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = 2.0f * invNF;
        m34 = 0.0f;
        m41 = -1.0f;
        m42 = 1.0f;
        m43 = (near + far) * invNF;
        m44 = 1.0f;
        return this;
    }

    /**
     * Set this matrix to a perspective projection matrix.
     *
     * @param left   the left frustum plane
     * @param right  the right frustum plane
     * @param bottom the bottom frustum plane
     * @param top    the top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return this
     */
    @Nonnull
    public Matrix4 setPerspective(float left, float right, float bottom, float top, float near, float far) {
        float invRL = 1.0f / (right - left);
        float invTB = 1.0f / (top - bottom);
        float invNF = 1.0f / (near - far);
        float tNear = 2.0f * near;
        m11 = tNear * invRL;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = tNear * invTB;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = (right + left) * invRL;
        m32 = (top + bottom) * invTB;
        m33 = (near + far) * invNF;
        m34 = -1.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m43 = tNear * far * invNF;
        m44 = 0.0f;
        return this;
    }

    /**
     * Set this matrix to a perspective projection matrix.
     *
     * @param fov    the angle of field of view in radians (0,PI)
     * @param aspect aspect ratio of the view (width / height)
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @return this
     */
    @Nonnull
    public Matrix4 setPerspective(float fov, float aspect, float near, float far) {
        float y = 1.0f / MathUtil.tan(fov * 0.5f);
        float invNF = 1.0f / (near - far);
        m11 = y / aspect;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = y;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = (near + far) * invNF;
        m34 = -1.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m43 = 2.0f * far * near * invNF;
        m44 = 0.0f;
        return this;
    }

    /**
     * Translates this matrix by given changes. This is equivalent to
     * pre-multiplying by a translation matrix. (Tr * this)
     *
     * @param t the translation vector
     */
    public void translate(@Nonnull Vector3 t) {
        translate(t.x, t.y, t.z);
    }

    /**
     * Translates this matrix by given changes. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     * @param dz the z-component of the translation
     */
    public void translate(float dx, float dy, float dz) {
        m41 += dx * m11 + dy * m21 + dz * m31;
        m42 += dx * m12 + dy * m22 + dz * m32;
        m43 += dx * m13 + dy * m23 + dz * m33;
        m44 += dx * m14 + dy * m24 + dz * m34;
    }

    /**
     * Translates this matrix by given changes. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void translateXY(float dx, float dy) {
        m41 += dx * m11 + dy * m21;
        m42 += dx * m12 + dy * m22;
        m43 += dx * m13 + dy * m23;
        m44 += dx * m14 + dy * m24;
    }

    /**
     * Sets this matrix to a translation matrix by given components.
     *
     * @param t the translation vector
     */
    public void setToTranslation(@Nonnull Vector3 t) {
        setToTranslation(t.x, t.y, t.z);
    }

    /**
     * Sets this matrix to a translation matrix by given components.
     *
     * @param x the x-component of the translation
     * @param y the y-component of the translation
     * @param z the z-component of the translation
     */
    public void setToTranslation(float x, float y, float z) {
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
        m41 = x;
        m42 = y;
        m43 = z;
        m44 = 1.0f;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param s the x-component of the scale
     */
    public void scaleX(float s) {
        m11 *= s;
        m12 *= s;
        m13 *= s;
        m14 *= s;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param s the y-component of the scale
     */
    public void scaleY(float s) {
        m21 *= s;
        m22 *= s;
        m23 *= s;
        m24 *= s;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param s the x-component of the scale
     */
    public void scaleZ(float s) {
        m31 *= s;
        m32 *= s;
        m33 *= s;
        m34 *= s;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param s the scale vector
     */
    public void scale(@Nonnull Vector3 s) {
        scale(s.x, s.y, s.z);
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param sx the x-component of the scale
     * @param sy the y-component of the scale
     * @param sz the z-component of the scale
     */
    public void scale(float sx, float sy, float sz) {
        m11 *= sx;
        m12 *= sx;
        m13 *= sx;
        m14 *= sx;
        m21 *= sy;
        m22 *= sy;
        m23 *= sy;
        m24 *= sy;
        m31 *= sz;
        m32 *= sz;
        m33 *= sz;
        m34 *= sz;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param sx the x-component of the scale
     * @param sy the y-component of the scale
     */
    public void scaleXY(float sx, float sy) {
        m11 *= sx;
        m12 *= sx;
        m13 *= sx;
        m14 *= sx;
        m21 *= sy;
        m22 *= sy;
        m23 *= sy;
        m24 *= sy;
    }

    /**
     * Sets this matrix to a scaling matrix by given components.
     *
     * @param s the scale vector
     */
    public void setToScaling(@Nonnull Vector3 s) {
        setToScaling(s.x, s.y, s.z);
    }

    /**
     * Sets this matrix to a scaling matrix by given components.
     *
     * @param x the x-component of the scale
     * @param y the y-component of the scale
     * @param z the z-component of the scale
     */
    public void setToScaling(float x, float y, float z) {
        m11 = x;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = y;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = z;
        m34 = 0.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m43 = 0.0f;
        m44 = 1.0f;
    }

    /**
     * Rotates this matrix clockwise about the X-axis.
     * This is equivalent to pre-multiplying by a rotation matrix.
     *
     * @param angle the clockwise rotation angle in radians.
     */
    public void rotateX(float angle) {
        if (angle == 0.0f)
            return;
        final float s = MathUtil.sin(angle);
        final float c = MathUtil.cos(angle);
        final float f21 = c * m21 + s * m31;
        final float f22 = c * m22 + s * m32;
        final float f23 = c * m23 + s * m33;
        final float f24 = c * m24 + s * m34;
        m31 = -s * m21 + c * m31;
        m32 = -s * m22 + c * m32;
        m33 = -s * m23 + c * m33;
        m34 = -s * m24 + c * m34;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m24 = f24;
    }

    /**
     * Rotates this matrix clockwise about the Y-axis.
     * This is equivalent to pre-multiplying by a rotation matrix.
     *
     * @param angle the clockwise rotation angle in radians.
     */
    public void rotateY(float angle) {
        if (angle == 0.0f)
            return;
        final float s = MathUtil.sin(angle);
        final float c = MathUtil.cos(angle);
        final float f11 = c * m11 - s * m31;
        final float f12 = c * m12 - s * m32;
        final float f13 = c * m13 - s * m33;
        final float f14 = c * m14 - s * m34;
        m31 = s * m11 + c * m31;
        m32 = s * m12 + c * m32;
        m33 = s * m13 + c * m33;
        m34 = s * m14 + c * m34;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
    }

    /**
     * Rotates this matrix clockwise about the Z-axis.
     * This is equivalent to pre-multiplying by a rotation matrix.
     *
     * @param angle the clockwise rotation angle in radians.
     */
    public void rotateZ(float angle) {
        if (angle == 0.0f)
            return;
        final float s = MathUtil.sin(angle);
        final float c = MathUtil.cos(angle);
        final float f11 = c * m11 + s * m21;
        final float f12 = c * m12 + s * m22;
        final float f13 = c * m13 + s * m23;
        final float f14 = c * m14 + s * m24;
        m21 = -s * m11 + c * m21;
        m22 = -s * m12 + c * m22;
        m23 = -s * m13 + c * m23;
        m24 = -s * m14 + c * m24;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
    }

    /**
     * Rotates this matrix clockwise about an arbitrary axis. The axis must be a
     * normalized (unit) vector. If the axis is X, Y or Z, use axis-specified
     * methods to rotate this matrix which are faster.
     *
     * @param axis  the rotation axis
     * @param angle rotation angle in radians
     * @see #rotateY(float)
     * @see #rotateZ(float)
     * @see #rotateX(float)
     */
    public void rotateByAxis(@Nonnull Vector3 axis, float angle) {
        rotateByAxis(axis.x, axis.y, axis.z, angle);
    }

    /**
     * Rotates this matrix clockwise about an arbitrary axis. The axis must be a
     * normalized (unit) vector. If the axis is X, Y or Z, use axis-specified
     * methods to rotate this matrix which are faster.
     *
     * @param x     x-coordinate of rotation axis
     * @param y     y-coordinate of rotation axis
     * @param z     z-coordinate of rotation axis
     * @param angle rotation angle in radians
     * @see #rotateY(float)
     * @see #rotateZ(float)
     * @see #rotateX(float)
     */
    public void rotateByAxis(float x, float y, float z, float angle) {
        if (angle == 0.0f) {
            return;
        }
        angle *= 0.5f;
        final float s = MathUtil.sin(angle);
        x *= s;
        y *= s;
        z *= s;
        float f1 = 2.0f * x;
        float f2 = 2.0f * y;
        float f3 = 2.0f * z;

        float f4 = x * f1;
        float f5 = y * f2;
        float f6 = z * f3;

        final float c = MathUtil.cos(angle);
        float f7 = f3 * c;
        float f8 = f2 * c;
        float f9 = f1 * c;

        float f11 = 1.0f - (f5 + f6);
        float f22 = 1.0f - (f4 + f6);
        float f33 = 1.0f - (f4 + f5);
        f2 *= x;
        x *= f3;
        f3 *= y;

        f4 = f2 - f7;
        f5 = x + f8;
        f6 = f2 + f7;
        f7 = f3 - f9;
        f8 = x - f8;
        f9 = f3 + f9;

        f1 = f11 * m11 + f6 * m21 + f8 * m31;
        f2 = f11 * m12 + f6 * m22 + f8 * m32;
        f3 = f11 * m13 + f6 * m23 + f8 * m33;
        x = f11 * m14 + f6 * m24 + f8 * m34;

        f11 = f4 * m11 + f22 * m21 + f9 * m31;
        f6 = f4 * m12 + f22 * m22 + f9 * m32;
        f8 = f4 * m13 + f22 * m23 + f9 * m33;
        y = f4 * m14 + f22 * m24 + f9 * m34;

        f4 = f5 * m11 + f7 * m21 + f33 * m31;
        f22 = f5 * m12 + f7 * m22 + f33 * m32;
        f9 = f5 * m13 + f7 * m23 + f33 * m33;
        z = f5 * m14 + f7 * m24 + f33 * m34;

        m11 = f1;
        m12 = f2;
        m13 = f3;
        m14 = x;
        m21 = f11;
        m22 = f6;
        m23 = f8;
        m24 = y;
        m31 = f4;
        m32 = f22;
        m33 = f9;
        m34 = z;
    }

    /**
     * Rotate this matrix by the given quaternion's rotation matrix.
     * (quat * this)
     *
     * @param q the quaternion to rotate by.
     */
    public void rotate(@Nonnull Quaternion q) {
        if (q.lengthSquared() >= 1.0e-6f) {
            // not mul an identity matrix
            multiply(q.toMatrix3());
        }
    }

    /**
     * Set this matrix to a rotation matrix by the quaternion's rotation.
     *
     * @param q the quaternion to set by.
     */
    public void setRotation(@Nonnull Quaternion q) {
        q.toMatrix4(this);
    }

    /**
     * Rotates this matrix from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order and using chained rotation per axis:
     * <ul>
     *  <li>x - pitch - {@link #rotateX(float)}</li>
     *  <li>y - yaw   - {@link #rotateY(float)}</li>
     *  <li>z - roll  - {@link #rotateZ(float)}</li>
     * </ul>
     * </p>
     *
     * @param rotationX the Euler pitch angle in radians. (rotation about the X axis)
     * @param rotationY the Euler yaw angle in radians. (rotation about the Y axis)
     * @param rotationZ the Euler roll angle in radians. (rotation about the Z axis)
     * @see #rotateY(float)
     * @see #rotateZ(float)
     * @see #rotateX(float)
     */
    public void rotateByEuler(float rotationX, float rotationY, float rotationZ) {
        // same as using Quaternion, 48 multiplications
        rotateX(rotationX);
        rotateY(rotationY);
        rotateZ(rotationZ);
    }

    /**
     * Transform a four-dimensional row vector by post-multiplication
     * (vec4 * this).
     *
     * @param vec the vector to transform
     */
    public void transform(@Nonnull Vector4 vec) {
        final float x = m11 * vec.x + m21 * vec.y + m31 * vec.z + m41 * vec.w;
        final float y = m12 * vec.x + m22 * vec.y + m32 * vec.z + m42 * vec.w;
        final float z = m13 * vec.x + m23 * vec.y + m33 * vec.z + m43 * vec.w;
        final float w = m14 * vec.x + m24 * vec.y + m34 * vec.z + m44 * vec.w;
        vec.x = x;
        vec.y = y;
        vec.z = z;
        vec.w = w;
    }

    /**
     * Transform a four-dimensional column vector by pre-multiplication
     * (this * vec4).
     *
     * @param vec the vector to transform
     */
    public void preTransform(@Nonnull Vector4 vec) {
        final float x = m11 * vec.x + m12 * vec.y + m13 * vec.z + m14 * vec.w;
        final float y = m21 * vec.x + m22 * vec.y + m23 * vec.z + m24 * vec.w;
        final float z = m31 * vec.x + m32 * vec.y + m33 * vec.z + m34 * vec.w;
        final float w = m41 * vec.x + m42 * vec.y + m43 * vec.z + m44 * vec.w;
        vec.x = x;
        vec.y = y;
        vec.z = z;
        vec.w = w;
    }

    /**
     * Transform a three-dimensional row vector by post-multiplication
     * (vec3 * this, w-component is considered as 1).
     * This should be used with position vectors.
     *
     * @param vec the vector to transform
     */
    public void transform(@Nonnull Vector3 vec) {
        final float x = m11 * vec.x + m21 * vec.y + m31 * vec.z + m41;
        final float y = m12 * vec.x + m22 * vec.y + m32 * vec.z + m42;
        final float z = m13 * vec.x + m23 * vec.y + m33 * vec.z + m43;
        if (isAffine()) {
            vec.x = x;
            vec.y = y;
            vec.z = z;
        } else {
            float w = 1.0f / (m14 * vec.x + m24 * vec.y + m34 * vec.z + m44);
            vec.x = x * w;
            vec.y = y * w;
            vec.z = z * w;
        }
    }

    /**
     * Transform a three-dimensional column vector by pre-multiplication
     * (this * vec3, w-component is considered as 1).
     * This should be used with normal vectors.
     *
     * @param vec the vector to transform
     */
    public void preTransform(@Nonnull Vector3 vec) {
        final float x = m11 * vec.x + m12 * vec.y + m13 * vec.z + m14;
        final float y = m21 * vec.x + m22 * vec.y + m23 * vec.z + m24;
        final float z = m31 * vec.x + m32 * vec.y + m33 * vec.z + m34;
        if (!hasTranslation()) {
            vec.x = x;
            vec.y = y;
            vec.z = z;
        } else {
            float w = 1.0f / (m41 * vec.x + m42 * vec.y + m43 * vec.z + m44);
            vec.x = x * w;
            vec.y = y * w;
            vec.z = z * w;
        }
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param r the rectangle to transform
     */
    public void transform(@Nonnull RectF r) {
        float x1 = m11 * r.left + m21 * r.top + m41;
        float y1 = m12 * r.left + m22 * r.top + m42;
        float x2 = m11 * r.right + m21 * r.top + m41;
        float y2 = m12 * r.right + m22 * r.top + m42;
        float x3 = m11 * r.left + m21 * r.bottom + m41;
        float y3 = m12 * r.left + m22 * r.bottom + m42;
        float x4 = m11 * r.right + m21 * r.bottom + m41;
        float y4 = m12 * r.right + m22 * r.bottom + m42;
        if (!isAffine()) {
            float w = 1.0f / (m14 * r.left + m24 * r.top + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * r.right + m24 * r.top + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * r.left + m24 * r.bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * r.right + m24 * r.bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        r.left = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
        r.top = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
        r.right = Math.max(x1, Math.max(x2, Math.max(x3, x4)));
        r.bottom = Math.max(y1, Math.max(y2, Math.max(y3, y4)));
    }

    /**
     * Map a point in the X-Y plane.
     *
     * @param p the point to transform
     */
    public void transform(@Nonnull PointF p) {
        if (isAffine()) {
            p.set(m11 * p.x + m21 * p.y + m41,
                    m12 * p.x + m22 * p.y + m42);
        } else {
            final float x = m11 * p.x + m21 * p.y + m41;
            final float y = m12 * p.x + m22 * p.y + m42;
            float w = 1.0f / (m14 * p.x + m24 * p.y + m44);
            p.x = x * w;
            p.y = y * w;
        }
    }

    /**
     * Returns whether this matrix is seen as an affine transformation.
     * Otherwise, there's a perspective projection.
     *
     * @return {@code true} if this matrix is affine.
     */
    public boolean isAffine() {
        return MathUtil.approxZero(m14, m24, m34) && MathUtil.approxEqual(m44, 1.0f);
    }

    public boolean hasPerspective() {
        return !isAffine();
    }

    public boolean hasTranslation() {
        return !(MathUtil.approxZero(m41, m42, m43) && MathUtil.approxEqual(m44, 1.0f));
    }

    /**
     * Returns whether this matrix is approximately equivalent to an identity matrix.
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
     * Returns whether this matrix is equivalent to given matrix.
     *
     * @param mat the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    public boolean equivalent(@Nullable Matrix4 mat) {
        if (mat == this)
            return true;
        if (mat == null)
            return false;
        return MathUtil.approxEqual(m11, mat.m11) &&
                MathUtil.approxEqual(m12, mat.m12) &&
                MathUtil.approxEqual(m13, mat.m13) &&
                MathUtil.approxEqual(m14, mat.m14) &&
                MathUtil.approxEqual(m21, mat.m21) &&
                MathUtil.approxEqual(m22, mat.m22) &&
                MathUtil.approxEqual(m23, mat.m23) &&
                MathUtil.approxEqual(m24, mat.m24) &&
                MathUtil.approxEqual(m31, mat.m31) &&
                MathUtil.approxEqual(m32, mat.m32) &&
                MathUtil.approxEqual(m33, mat.m33) &&
                MathUtil.approxEqual(m34, mat.m34) &&
                MathUtil.approxEqual(m41, mat.m41) &&
                MathUtil.approxEqual(m42, mat.m42) &&
                MathUtil.approxEqual(m43, mat.m43) &&
                MathUtil.approxEqual(m44, mat.m44);
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
        int result = (m11 != 0.0f ? Float.floatToIntBits(m11) : 0);
        result = 31 * result + (m12 != 0.0f ? Float.floatToIntBits(m12) : 0);
        result = 31 * result + (m13 != 0.0f ? Float.floatToIntBits(m13) : 0);
        result = 31 * result + (m14 != 0.0f ? Float.floatToIntBits(m14) : 0);
        result = 31 * result + (m21 != 0.0f ? Float.floatToIntBits(m21) : 0);
        result = 31 * result + (m22 != 0.0f ? Float.floatToIntBits(m22) : 0);
        result = 31 * result + (m23 != 0.0f ? Float.floatToIntBits(m23) : 0);
        result = 31 * result + (m24 != 0.0f ? Float.floatToIntBits(m24) : 0);
        result = 31 * result + (m31 != 0.0f ? Float.floatToIntBits(m31) : 0);
        result = 31 * result + (m32 != 0.0f ? Float.floatToIntBits(m32) : 0);
        result = 31 * result + (m33 != 0.0f ? Float.floatToIntBits(m33) : 0);
        result = 31 * result + (m34 != 0.0f ? Float.floatToIntBits(m34) : 0);
        result = 31 * result + (m41 != 0.0f ? Float.floatToIntBits(m41) : 0);
        result = 31 * result + (m42 != 0.0f ? Float.floatToIntBits(m42) : 0);
        result = 31 * result + (m43 != 0.0f ? Float.floatToIntBits(m43) : 0);
        result = 31 * result + (m44 != 0.0f ? Float.floatToIntBits(m44) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("""
                        Matrix4:
                        %10.5f %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f %10.5f
                        %10.5f %10.5f %10.5f %10.5f
                        """,
                m11, m12, m13, m14,
                m21, m22, m23, m24,
                m31, m32, m33, m34,
                m41, m42, m43, m44);
    }

    /**
     * @return a copy of this matrix
     */
    @Nonnull
    public Matrix4 copy() {
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

    // strassen algorithm
    private static void multiply(@Nonnull float[] a, @Nonnull float[] b, @Nonnull float[] out) {
        float[] temp = new float[28];

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
