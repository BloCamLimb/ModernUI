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

package icyllis.arcui.core;

import icyllis.arcui.engine.DataUtils;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static icyllis.arcui.core.MathUtil.*;
import static org.lwjgl.system.MemoryUtil.memGetFloat;

/**
 * Represents a 4x4 row-major matrix.
 */
@SuppressWarnings("unused")
public class Matrix4 implements Cloneable {

    public static final long OFFSET;

    static {
        try {
            OFFSET = DataUtils.UNSAFE.objectFieldOffset(Matrix4.class.getDeclaredField("m11"));
        } catch (Exception e) {
            throw new UnsupportedOperationException("No UNSAFE", e);
        }
    }

    // sequential matrix elements, m(ij) (row, column)
    // directly using primitives will be faster than array in Java
    // [m11 m12 m13 m14]
    // [m21 m22 m23 m24]
    // [m31 m32 m33 m34]
    // [m41 m42 m43 m44] <- [m41 m42 m43] represents the origin
    public float m11;
    public float m12;
    public float m13;
    public float m14;
    public float m21;
    public float m22;
    public float m23;
    public float m24;
    public float m31;
    public float m32;
    public float m33;
    public float m34;
    public float m41;
    public float m42;
    public float m43;
    public float m44;

    /**
     * Create a zero matrix.
     *
     * @see #identity()
     */
    public Matrix4() {
    }

    /**
     * Create a matrix from an array of elements in row-major.
     *
     * @param a the array to create from
     * @see #set(float[])
     */
    public Matrix4(@Nonnull float... a) {
        set(a);
    }

    /**
     * Create a matrix copied from an existing matrix.
     *
     * @param mat the matrix to create from
     * @see #set(Matrix4)
     */
    public Matrix4(@Nonnull Matrix4 mat) {
        set(mat);
    }

    /**
     * Create a copy of {@code mat} if not null, otherwise a new identity matrix.
     * Note: we assume null is identity.
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
        final Matrix4 mat = new Matrix4();
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
        final Matrix4 mat = new Matrix4();
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
        final Matrix4 mat = new Matrix4();
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
        final Matrix4 mat = new Matrix4();
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
        final Matrix4 mat = new Matrix4();
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
    public static Matrix4 makeTranslate(float x, float y, float z) {
        final Matrix4 mat = new Matrix4();
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
     * @param mat the addend
     */
    public void add(@Nonnull Matrix4 mat) {
        m11 += mat.m11;
        m12 += mat.m12;
        m13 += mat.m13;
        m14 += mat.m14;
        m21 += mat.m21;
        m22 += mat.m22;
        m23 += mat.m23;
        m24 += mat.m24;
        m31 += mat.m31;
        m32 += mat.m32;
        m33 += mat.m33;
        m34 += mat.m34;
        m41 += mat.m41;
        m42 += mat.m42;
        m43 += mat.m43;
        m44 += mat.m44;
    }

    /**
     * Subtract each element of the given matrix from the corresponding element of this matrix.
     *
     * @param mat the subtrahend
     */
    public void sub(@Nonnull Matrix4 mat) {
        m11 -= mat.m11;
        m12 -= mat.m12;
        m13 -= mat.m13;
        m14 -= mat.m14;
        m21 -= mat.m21;
        m22 -= mat.m22;
        m23 -= mat.m23;
        m24 -= mat.m24;
        m31 -= mat.m31;
        m32 -= mat.m32;
        m33 -= mat.m33;
        m34 -= mat.m34;
        m41 -= mat.m41;
        m42 -= mat.m42;
        m43 -= mat.m43;
        m44 -= mat.m44;
    }

    /**
     * Pre-multiply this matrix by a 4x4 matrix, whose top left 3x3 is the given
     * 3x3 matrix, and forth row and column are identity. (mat3 * this)
     *
     * @param mat the matrix to multiply
     */
    public void preMul(@Nonnull Matrix3 mat) {
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
     * 3x3 matrix, and forth row and column are identity. (this * Matrix4(mat3))
     *
     * @param mat the matrix to multiply with
     */
    public void postMul(@Nonnull Matrix3 mat) {
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
    public void preMul(@Nonnull Matrix4 mat) {
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
    public void postMul(@Nonnull Matrix4 mat) {
        final float f11 = m11 * mat.m11 + m12 * mat.m21 + m13 * mat.m31 + m14 * mat.m41;
        final float f12 = m11 * mat.m12 + m12 * mat.m22 + m13 * mat.m32 + m14 * mat.m42;
        final float f13 = m11 * mat.m13 + m12 * mat.m23 + m13 * mat.m33 + m14 * mat.m43;
        final float f14 = m11 * mat.m14 + m12 * mat.m24 + m13 * mat.m34 + m14 * mat.m44;
        final float f21 = m21 * mat.m11 + m22 * mat.m21 + m23 * mat.m31 + m24 * mat.m41;
        final float f22 = m21 * mat.m12 + m22 * mat.m22 + m23 * mat.m32 + m24 * mat.m42;
        final float f23 = m21 * mat.m13 + m22 * mat.m23 + m23 * mat.m33 + m24 * mat.m43;
        final float f24 = m21 * mat.m14 + m22 * mat.m24 + m23 * mat.m34 + m24 * mat.m44;
        final float f31 = m31 * mat.m11 + m32 * mat.m21 + m33 * mat.m31 + m34 * mat.m41;
        final float f32 = m31 * mat.m12 + m32 * mat.m22 + m33 * mat.m32 + m34 * mat.m42;
        final float f33 = m31 * mat.m13 + m32 * mat.m23 + m33 * mat.m33 + m34 * mat.m43;
        final float f34 = m31 * mat.m14 + m32 * mat.m24 + m33 * mat.m34 + m34 * mat.m44;
        final float f41 = m41 * mat.m11 + m42 * mat.m21 + m43 * mat.m31 + m44 * mat.m41;
        final float f42 = m41 * mat.m12 + m42 * mat.m22 + m43 * mat.m32 + m44 * mat.m42;
        final float f43 = m41 * mat.m13 + m42 * mat.m23 + m43 * mat.m33 + m44 * mat.m43;
        final float f44 = m41 * mat.m14 + m42 * mat.m24 + m43 * mat.m34 + m44 * mat.m44;
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
     * Clear this matrix to zero.
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
     * Clear this matrix to identity.
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
     * Set this matrix elements from an array.
     *
     * @param a the array to copy from
     * @see #Matrix4(float...)
     */
    public void set(@Nonnull ByteBuffer a) {
        m11 = a.getFloat();
        m12 = a.getFloat();
        m13 = a.getFloat();
        m14 = a.getFloat();
        m21 = a.getFloat();
        m22 = a.getFloat();
        m23 = a.getFloat();
        m24 = a.getFloat();
        m31 = a.getFloat();
        m32 = a.getFloat();
        m33 = a.getFloat();
        m34 = a.getFloat();
        m41 = a.getFloat();
        m42 = a.getFloat();
        m43 = a.getFloat();
        m44 = a.getFloat();
    }

    /**
     * Set this matrix elements from an array.
     *
     * @param a the array to copy from
     * @see #Matrix4(float...)
     */
    public void set(@Nonnull FloatBuffer a) {
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
     * Set this matrix elements from an address. (UNSAFE).
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to copy from
     */
    public void set(long p) {
        m11 = memGetFloat(p);
        m12 = memGetFloat(p + 4);
        m13 = memGetFloat(p + 8);
        m14 = memGetFloat(p + 12);
        m21 = memGetFloat(p + 16);
        m22 = memGetFloat(p + 20);
        m23 = memGetFloat(p + 24);
        m24 = memGetFloat(p + 28);
        m31 = memGetFloat(p + 32);
        m32 = memGetFloat(p + 36);
        m33 = memGetFloat(p + 40);
        m34 = memGetFloat(p + 44);
        m41 = memGetFloat(p + 48);
        m42 = memGetFloat(p + 52);
        m43 = memGetFloat(p + 56);
        m44 = memGetFloat(p + 60);
    }

    /**
     * Set this matrix elements to be given matrix.
     *
     * @param mat the matrix to store
     */
    public void put(@Nonnull Matrix4 mat) {
        mat.set(this);
    }

    /**
     * Get this matrix data, store them into an array.
     *
     * @param a the array to store
     */
    public void put(@Nonnull float[] a) {
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
    public void put(@Nonnull ByteBuffer a) {
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
     * Get this matrix data, store them into an array.
     *
     * @param a the pointer of the array to store
     */
    public void put(@Nonnull FloatBuffer a) {
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
     * Get this matrix data, store them into an address (UNSAFE).
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store
     */
    public void put(long p) {
        final Unsafe unsafe = DataUtils.UNSAFE;
        final long src = OFFSET;
        unsafe.putLong(null, p, unsafe.getLong(this, src));
        unsafe.putLong(null, p + 8, unsafe.getLong(this, src + 8));
        unsafe.putLong(null, p + 16, unsafe.getLong(this, src + 16));
        unsafe.putLong(null, p + 24, unsafe.getLong(this, src + 24));
        unsafe.putLong(null, p + 32, unsafe.getLong(this, src + 32));
        unsafe.putLong(null, p + 40, unsafe.getLong(this, src + 40));
        unsafe.putLong(null, p + 48, unsafe.getLong(this, src + 48));
        unsafe.putLong(null, p + 56, unsafe.getLong(this, src + 56));
    }

    /**
     * Compute the determinant of this matrix.
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
     * Compute the trace of this matrix.
     *
     * @return the trace of this matrix
     */
    public float trace() {
        return m11 + m22 + m33 + m44;
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
    public boolean invert(@Nonnull Matrix4 mat) {
        float b00 = m11 * m22 - m12 * m21;
        float b01 = m11 * m23 - m13 * m21;
        float b02 = m11 * m24 - m14 * m21;
        float b03 = m12 * m23 - m13 * m22;
        float b04 = m12 * m24 - m14 * m22;
        float b05 = m13 * m24 - m14 * m23;
        float b06 = m31 * m42 - m32 * m41;
        float b07 = m31 * m43 - m33 * m41;
        float b08 = m31 * m44 - m34 * m41;
        float b09 = m32 * m43 - m33 * m42;
        float b10 = m32 * m44 - m34 * m42;
        float b11 = m33 * m44 - m34 * m43;
        // calc the determinant
        float det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06;
        if (approxZero(det)) {
            return false;
        }
        // calc algebraic cofactor and transpose
        det = 1.0f / det;
        b00 *= det;
        b01 *= det;
        b02 *= det;
        b03 *= det;
        b04 *= det;
        b05 *= det;
        b06 *= det;
        b07 *= det;
        b08 *= det;
        b09 *= det;
        b10 *= det;
        b11 *= det;
        final float f11 = m22 * b11 - m23 * b10 + m24 * b09;
        final float f12 = m23 * b08 - m21 * b11 - m24 * b07;
        final float f13 = m21 * b10 - m22 * b08 + m24 * b06;
        final float f14 = m22 * b07 - m21 * b09 - m23 * b06;
        final float f21 = m13 * b10 - m12 * b11 - m14 * b09;
        final float f22 = m11 * b11 - m13 * b08 + m14 * b07;
        final float f23 = m12 * b08 - m11 * b10 - m14 * b06;
        final float f24 = m11 * b09 - m12 * b07 + m13 * b06;
        final float f31 = m42 * b05 - m43 * b04 + m44 * b03;
        final float f32 = m43 * b02 - m41 * b05 - m44 * b01;
        final float f33 = m41 * b04 - m42 * b02 + m44 * b00;
        final float f34 = m42 * b01 - m41 * b03 - m43 * b00;
        final float f41 = m33 * b04 - m32 * b05 - m34 * b03;
        final float f42 = m31 * b05 - m33 * b02 + m34 * b01;
        final float f43 = m32 * b02 - m31 * b04 - m34 * b00;
        final float f44 = m31 * b03 - m32 * b01 + m33 * b00;
        mat.m11 = f11;
        mat.m21 = f12;
        mat.m31 = f13;
        mat.m41 = f14;
        mat.m12 = f21;
        mat.m22 = f22;
        mat.m32 = f23;
        mat.m42 = f24;
        mat.m13 = f31;
        mat.m23 = f32;
        mat.m33 = f33;
        mat.m43 = f34;
        mat.m14 = f41;
        mat.m24 = f42;
        mat.m34 = f43;
        mat.m44 = f44;
        return true;
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
        float y = (float) (1.0 / Math.tan(fov * 0.5));
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
     * Translates this matrix by given vector. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     */
    public void preTranslateX(float dx) {
        m41 += dx * m11;
        m42 += dx * m12;
        m43 += dx * m13;
        m44 += dx * m14;
    }

    /**
     * Post-translates this matrix by given vector. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     */
    public void postTranslateX(float dx) {
        m11 += dx * m14;
        m21 += dx * m24;
        m31 += dx * m34;
        m41 += dx * m44;
    }

    /**
     * Translates this matrix by given vector. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dy the y-component of the translation
     */
    public void preTranslateY(float dy) {
        m41 += dy * m21;
        m42 += dy * m22;
        m43 += dy * m23;
        m44 += dy * m24;
    }

    /**
     * Post-translates this matrix by given vector. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dy the y-component of the translation
     */
    public void postTranslateY(float dy) {
        m12 += dy * m14;
        m22 += dy * m24;
        m32 += dy * m34;
        m42 += dy * m44;
    }

    /**
     * Translates this matrix by given vector. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dz the z-component of the translation
     */
    public void preTranslateZ(float dz) {
        m41 += dz * m31;
        m42 += dz * m32;
        m43 += dz * m33;
        m44 += dz * m34;
    }

    /**
     * Post-translates this matrix by given vector. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dz the z-component of the translation
     */
    public void postTranslateZ(float dz) {
        m13 += dz * m14;
        m23 += dz * m24;
        m33 += dz * m34;
        m43 += dz * m44;
    }

    /**
     * Translates this matrix by given changes. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     * @param dz the z-component of the translation
     */
    public void preTranslate(float dx, float dy, float dz) {
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
    public void preTranslate(float dx, float dy) {
        m41 += dx * m11 + dy * m21;
        m42 += dx * m12 + dy * m22;
        m43 += dx * m13 + dy * m23;
        m44 += dx * m14 + dy * m24;
    }

    /**
     * Post-translates this matrix by given changes. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     * @param dz the z-component of the translation
     */
    public void postTranslate(float dx, float dy, float dz) {
        m11 += dx * m14;
        m12 += dy * m14;
        m13 += dz * m14;
        m21 += dx * m24;
        m22 += dy * m24;
        m23 += dz * m24;
        m31 += dx * m34;
        m32 += dy * m34;
        m33 += dz * m34;
        m41 += dx * m44;
        m42 += dy * m44;
        m43 += dz * m44;
    }

    /**
     * Post-translates this matrix by given changes. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void postTranslate(float dx, float dy) {
        m11 += dx * m14;
        m12 += dy * m14;
        m21 += dx * m24;
        m22 += dy * m24;
        m31 += dx * m34;
        m32 += dy * m34;
        m41 += dx * m44;
        m42 += dy * m44;
    }

    /**
     * Sets this matrix to a translation matrix by given components.
     *
     * @param x the x-component of the translation
     * @param y the y-component of the translation
     * @param z the z-component of the translation
     */
    public void setTranslate(float x, float y, float z) {
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
    public void preScaleX(float s) {
        m11 *= s;
        m12 *= s;
        m13 *= s;
        m14 *= s;
    }

    /**
     * Post-scales this matrix by given vector. This is equivalent to
     * post-multiplying by a scale matrix.
     *
     * @param s the x-component of the scale
     */
    public void postScaleX(float s) {
        m11 *= s;
        m21 *= s;
        m31 *= s;
        m41 *= s;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param s the y-component of the scale
     */
    public void preScaleY(float s) {
        m21 *= s;
        m22 *= s;
        m23 *= s;
        m24 *= s;
    }

    /**
     * Post-scales this matrix by given vector. This is equivalent to
     * post-multiplying by a scale matrix.
     *
     * @param s the y-component of the scale
     */
    public void postScaleY(float s) {
        m12 *= s;
        m22 *= s;
        m32 *= s;
        m42 *= s;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param s the x-component of the scale
     */
    public void preScaleZ(float s) {
        m31 *= s;
        m32 *= s;
        m33 *= s;
        m34 *= s;
    }

    /**
     * Post-scales this matrix by given vector. This is equivalent to
     * post-multiplying by a scale matrix.
     *
     * @param s the x-component of the scale
     */
    public void postScaleZ(float s) {
        m13 *= s;
        m23 *= s;
        m33 *= s;
        m43 *= s;
    }

    /**
     * Scales this matrix by given vector. This is equivalent to
     * pre-multiplying by a scale matrix.
     *
     * @param sx the x-component of the scale
     * @param sy the y-component of the scale
     * @param sz the z-component of the scale
     */
    public void preScale(float sx, float sy, float sz) {
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
    public void preScale(float sx, float sy) {
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
     * Post-scales this matrix by given vector. This is equivalent to
     * post-multiplying by a scale matrix.
     *
     * @param sx the x-component of the scale
     * @param sy the y-component of the scale
     * @param sz the z-component of the scale
     */
    public void postScale(float sx, float sy, float sz) {
        m11 *= sx;
        m21 *= sx;
        m31 *= sx;
        m41 *= sx;
        m12 *= sy;
        m22 *= sy;
        m32 *= sy;
        m42 *= sy;
        m13 *= sz;
        m23 *= sz;
        m33 *= sz;
        m43 *= sz;
    }

    /**
     * Post-scales this matrix by given vector. This is equivalent to
     * post-multiplying by a scale matrix.
     *
     * @param sx the x-component of the scale
     * @param sy the y-component of the scale
     */
    public void postScale(float sx, float sy) {
        m11 *= sx;
        m21 *= sx;
        m31 *= sx;
        m41 *= sx;
        m12 *= sy;
        m22 *= sy;
        m32 *= sy;
        m42 *= sy;
    }

    /**
     * Sets this matrix to a scaling matrix by given components.
     *
     * @param x the x-component of the scale
     * @param y the y-component of the scale
     * @param z the z-component of the scale
     */
    public void setScale(float x, float y, float z) {
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
     * Rotates this matrix about the X-axis with the given angle in radians.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to pre-multiplying by a rotation matrix.
     *
     * @param angle the rotation angle in radians.
     */
    public void preRotateX(float angle) {
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        final float f21 = c * m21 + s * m31;
        final float f22 = c * m22 + s * m32;
        final float f23 = c * m23 + s * m33;
        final float f24 = c * m24 + s * m34;
        m31 = c * m31 - s * m21;
        m32 = c * m32 - s * m22;
        m33 = c * m33 - s * m23;
        m34 = c * m34 - s * m24;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m24 = f24;
    }

    /**
     * Post-rotates this matrix about the X-axis with the given angle in radians.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to post-multiplying by a rotation matrix.
     *
     * @param angle the rotation angle in radians.
     */
    public void postRotateX(float angle) {
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        final float f13 = c * m13 + s * m12;
        final float f23 = c * m23 + s * m22;
        final float f33 = c * m33 + s * m32;
        final float f43 = c * m43 + s * m42;
        m12 = c * m12 - s * m13;
        m22 = c * m22 - s * m23;
        m32 = c * m32 - s * m33;
        m42 = c * m42 - s * m43;
        m13 = f13;
        m23 = f23;
        m33 = f33;
        m43 = f43;
    }

    /**
     * Rotates this matrix about the Y-axis with the given angle in radians.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to pre-multiplying by a rotation matrix.
     *
     * @param angle the rotation angle in radians.
     */
    public void preRotateY(float angle) {
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        final float f11 = c * m11 - s * m31;
        final float f12 = c * m12 - s * m32;
        final float f13 = c * m13 - s * m33;
        final float f14 = c * m14 - s * m34;
        m31 = c * m31 + s * m11;
        m32 = c * m32 + s * m12;
        m33 = c * m33 + s * m13;
        m34 = c * m34 + s * m14;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
    }

    /**
     * Post-rotates this matrix about the Y-axis with the given angle in radians.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to post-multiplying by a rotation matrix.
     *
     * @param angle the rotation angle in radians.
     */
    public void postRotateY(float angle) {
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        final float f13 = c * m13 - s * m11;
        final float f23 = c * m23 - s * m21;
        final float f33 = c * m33 - s * m31;
        final float f43 = c * m43 - s * m41;
        m11 = c * m11 + s * m13;
        m21 = c * m21 + s * m23;
        m31 = c * m31 + s * m33;
        m41 = c * m41 + s * m43;
        m13 = f13;
        m23 = f23;
        m33 = f33;
        m43 = f43;
    }

    /**
     * Rotates this matrix about the Z-axis with the given angle in radians.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to pre-multiplying by a rotation matrix.
     *
     * @param angle the rotation angle in radians.
     */
    public void preRotateZ(float angle) {
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        final float f11 = c * m11 + s * m21;
        final float f12 = c * m12 + s * m22;
        final float f13 = c * m13 + s * m23;
        final float f14 = c * m14 + s * m24;
        m21 = c * m21 - s * m11;
        m22 = c * m22 - s * m12;
        m23 = c * m23 - s * m13;
        m24 = c * m24 - s * m14;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
    }

    /**
     * Post-rotates this matrix about the Z-axis with the given angle in radians.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to post-multiplying by a rotation matrix.
     *
     * @param angle the rotation angle in radians.
     */
    public void postRotateZ(float angle) {
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        final float f12 = c * m12 + s * m11;
        final float f22 = c * m22 + s * m21;
        final float f32 = c * m32 + s * m31;
        final float f42 = c * m42 + s * m41;
        m11 = c * m11 - s * m12;
        m21 = c * m21 - s * m22;
        m31 = c * m31 - s * m32;
        m41 = c * m41 - s * m42;
        m12 = f12;
        m22 = f22;
        m32 = f32;
        m42 = f42;
    }

    /**
     * Rotates this matrix from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order and using chained rotation per axis:
     * <ul>
     *  <li>x - pitch - {@link #preRotateX(float)}</li>
     *  <li>y - yaw   - {@link #preRotateY(float)}</li>
     *  <li>z - roll  - {@link #preRotateZ(float)}</li>
     * </ul>
     * </p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     *
     * @param x the Euler pitch angle in radians. (rotation about the X axis)
     * @param y the Euler yaw angle in radians. (rotation about the Y axis)
     * @param z the Euler roll angle in radians. (rotation about the Z axis)
     * @see #preRotateY(float)
     * @see #preRotateZ(float)
     * @see #preRotateX(float)
     */
    public void preRotate(float x, float y, float z) {
        // same as using Quaternion, 48 multiplications
        preRotateX(x);
        preRotateY(y);
        preRotateZ(z);
    }

    /**
     * Post-rotates this matrix from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order and using chained rotation per axis:
     * <ul>
     *  <li>x - pitch - {@link #postRotateX(float)}</li>
     *  <li>y - yaw   - {@link #postRotateY(float)}</li>
     *  <li>z - roll  - {@link #postRotateZ(float)}</li>
     * </ul>
     * </p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     *
     * @param x the Euler pitch angle in radians. (rotation about the X axis)
     * @param y the Euler yaw angle in radians. (rotation about the Y axis)
     * @param z the Euler roll angle in radians. (rotation about the Z axis)
     * @see #postRotateY(float)
     * @see #postRotateZ(float)
     * @see #postRotateX(float)
     */
    public void postRotate(float x, float y, float z) {
        // same as using Quaternion, 48 multiplications
        postRotateX(x);
        postRotateY(y);
        postRotateZ(z);
    }

    /**
     * Rotates this matrix about an arbitrary axis. The axis must be a
     * normalized (unit) vector. If the axis is X, Y or Z, use axis-specified
     * methods to rotate this matrix which are faster.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     *
     * @param x     x-coordinate of rotation axis
     * @param y     y-coordinate of rotation axis
     * @param z     z-coordinate of rotation axis
     * @param angle rotation angle in radians
     * @see #preRotateY(float)
     * @see #preRotateZ(float)
     * @see #preRotateX(float)
     */
    public void preRotate(float x, float y, float z, float angle) {
        if (angle == 0)
            return;
        // 52 multiplications
        angle *= 0.5f;
        final float s = (float) Math.sin(angle);
        final float c = (float) Math.cos(angle);
        x *= s;
        y *= s;
        z *= s;
        final float xs = 2.0f * x;
        final float ys = 2.0f * y;
        final float zs = 2.0f * z;

        final float xx = x * xs;
        final float xy = x * ys;
        final float xz = x * zs;
        final float xw = xs * c;
        final float yy = y * ys;
        final float yz = y * zs;
        final float yw = ys * c;
        final float zz = z * zs;
        final float zw = zs * c;

        x = 1.0f - (yy + zz);
        y = xy + zw;
        z = xz - yw;
        final float f11 = x * m11 + y * m21 + z * m31;
        final float f12 = x * m12 + y * m22 + z * m32;
        final float f13 = x * m13 + y * m23 + z * m33;
        final float f14 = x * m14 + y * m24 + z * m34;

        x = xy - zw;
        y = 1.0f - (xx + zz);
        z = yz + xw;
        final float f21 = x * m11 + y * m21 + z * m31;
        final float f22 = x * m12 + y * m22 + z * m32;
        final float f23 = x * m13 + y * m23 + z * m33;
        final float f24 = x * m14 + y * m24 + z * m34;

        x = xz + yw;
        y = yz - xw;
        z = 1.0f - (xx + yy);
        final float f31 = x * m11 + y * m21 + z * m31;
        final float f32 = x * m12 + y * m22 + z * m32;
        final float f33 = x * m13 + y * m23 + z * m33;
        final float f34 = x * m14 + y * m24 + z * m34;

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
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param r the rectangle to transform
     */
    public void mapRect(@Nonnull RectF r) {
        float x1 = m11 * r.left + m21 * r.top + m41;
        float y1 = m12 * r.left + m22 * r.top + m42;
        float x2 = m11 * r.right + m21 * r.top + m41;
        float y2 = m12 * r.right + m22 * r.top + m42;
        float x3 = m11 * r.left + m21 * r.bottom + m41;
        float y3 = m12 * r.left + m22 * r.bottom + m42;
        float x4 = m11 * r.right + m21 * r.bottom + m41;
        float y4 = m12 * r.right + m22 * r.bottom + m42;
        if (!isAffine()) {
            // project
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
        r.left = min(x1, x2, x3, x4);
        r.top = min(y1, y2, y3, y4);
        r.right = max(x1, x2, x3, x4);
        r.bottom = max(y1, y2, y3, y4);
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param r   the rectangle to transform
     * @param out the round values
     */
    public void mapRect(@Nonnull RectF r, @Nonnull Rect out) {
        float x1 = m11 * r.left + m21 * r.top + m41;
        float y1 = m12 * r.left + m22 * r.top + m42;
        float x2 = m11 * r.right + m21 * r.top + m41;
        float y2 = m12 * r.right + m22 * r.top + m42;
        float x3 = m11 * r.left + m21 * r.bottom + m41;
        float y3 = m12 * r.left + m22 * r.bottom + m42;
        float x4 = m11 * r.right + m21 * r.bottom + m41;
        float y4 = m12 * r.right + m22 * r.bottom + m42;
        if (!isAffine()) {
            // project
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
        out.left = Math.round(min(x1, x2, x3, x4));
        out.top = Math.round(min(y1, y2, y3, y4));
        out.right = Math.round(max(x1, x2, x3, x4));
        out.bottom = Math.round(max(y1, y2, y3, y4));
    }

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param r   the rectangle to transform
     * @param out the round out values
     */
    public void mapRectOut(@Nonnull RectF r, @Nonnull Rect out) {
        float x1 = m11 * r.left + m21 * r.top + m41;
        float y1 = m12 * r.left + m22 * r.top + m42;
        float x2 = m11 * r.right + m21 * r.top + m41;
        float y2 = m12 * r.right + m22 * r.top + m42;
        float x3 = m11 * r.left + m21 * r.bottom + m41;
        float y3 = m12 * r.left + m22 * r.bottom + m42;
        float x4 = m11 * r.right + m21 * r.bottom + m41;
        float y4 = m12 * r.right + m22 * r.bottom + m42;
        if (!isAffine()) {
            // project
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
        out.left = (int) Math.floor(min(x1, x2, x3, x4));
        out.top = (int) Math.floor(min(y1, y2, y3, y4));
        out.right = (int) Math.ceil(max(x1, x2, x3, x4));
        out.bottom = (int) Math.ceil(max(y1, y2, y3, y4));
    }

    /**
     * Map a point in the X-Y plane.
     *
     * @param p the point to transform
     */
    public void mapPoint(@Nonnull float[] p) {
        if (isAffine()) {
            final float x = m11 * p[0] + m21 * p[1] + m41;
            final float y = m12 * p[0] + m22 * p[1] + m42;
            p[0] = x;
            p[1] = y;
        } else {
            final float x = m11 * p[0] + m21 * p[1] + m41;
            final float y = m12 * p[0] + m22 * p[1] + m42;
            float w = 1.0f / (m14 * p[0] + m24 * p[1] + m44);
            p[0] = x * w;
            p[1] = y * w;
        }
    }

    public void mapVec3(float[] vec) {
        final float x = m11 * vec[0] + m21 * vec[1] + m41 * vec[2];
        final float y = m12 * vec[0] + m22 * vec[1] + m42 * vec[2];
        final float w = m14 * vec[0] + m24 * vec[1] + m44 * vec[2];
        vec[0] = x;
        vec[1] = y;
        vec[2] = w;
    }

    /**
     * Returns whether this matrix is seen as an affine transformation.
     * Otherwise, there's a perspective projection.
     *
     * @return {@code true} if this matrix is affine.
     */
    public boolean isAffine() {
        return approxZero(m14, m24, m34) && approxEqual(m44, 1.0f);
    }

    /**
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scale, translate, or both.
     */
    public boolean isScaleTranslate() {
        return isAffine() &&
                approxZero(m12, m13, m21) &&
                approxZero(m23, m31, m32);
    }

    /**
     * Returns whether this matrix transforms rect to another rect. If true, this matrix is identity,
     * or/and scales, or/and rotates round Z axis a multiple of 90 degrees, or mirrors on axes.
     * In all cases, this matrix is affine and may also have translation.
     * <p>
     * For example:
     * <pre>{@code
     *      Matrix4 matrix = Matrix4.identity();
     *      matrix.translate(3, 5, 7);
     *      matrix.scale(2, 3, 4);
     *      matrix.rotateX(MathUtil.PI_DIV_4);
     *      matrix.rotateZ(MathUtil.PI_DIV_2);
     * }
     * </pre>
     *
     * @return true if this matrix transform one rect into another
     */
    public boolean isAxisAligned() {
        return isAffine() && (
                (approxZero(m11) && approxZero(m22) && !approxZero(m12) && !approxZero(m21)) ||
                        (approxZero(m12) && approxZero(m21) && !approxZero(m11) && !approxZero(m22))
        );
    }

    /**
     * If the last column of the matrix is [0, 0, 0, not_one]^T, we will treat the matrix as if it
     * is in perspective, even though it stills behaves like its affine. If we divide everything
     * by the not_one value, then it will behave the same, but will be treated as affine,
     * and therefore faster (e.g. clients can forward-difference calculations).
     */
    public void normalizePerspective() {
        if (m44 != 1 && m44 != 0 && m14 == 0 && m24 == 0 && m34 == 0) {
            float inv = 1.0f / m44;
            m11 *= inv;
            m12 *= inv;
            m13 *= inv;
            m21 *= inv;
            m22 *= inv;
            m23 *= inv;
            m31 *= inv;
            m32 *= inv;
            m33 *= inv;
            m41 *= inv;
            m42 *= inv;
            m43 *= inv;
            m44 = 1.0f;
        }
    }

    public boolean hasPerspective() {
        return !isAffine();
    }

    public boolean hasTranslation() {
        return !(approxZero(m41, m42, m43) && approxEqual(m44, 1.0f));
    }

    /**
     * Returns whether this matrix is approximately equivalent to an identity matrix.
     *
     * @return {@code true} if this matrix is identity.
     */
    public boolean isIdentity() {
        return approxZero(m12, m13, m14) &&
                approxZero(m21, m23, m24) &&
                approxZero(m31, m32, m34) &&
                approxZero(m41, m42, m43) &&
                approxEqual(m11, m22, m33, m44, 1.0f);
    }

    public boolean equal(@Nonnull Matrix4 mat) {
        return m11 == mat.m11 &&
                m12 == mat.m12 &&
                m13 == mat.m13 &&
                m14 == mat.m14 &&
                m21 == mat.m21 &&
                m22 == mat.m22 &&
                m23 == mat.m23 &&
                m24 == mat.m24 &&
                m31 == mat.m31 &&
                m32 == mat.m32 &&
                m33 == mat.m33 &&
                m34 == mat.m34 &&
                m41 == mat.m41 &&
                m42 == mat.m42 &&
                m43 == mat.m43 &&
                m44 == mat.m44;
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
        return approxEqual(m11, mat.m11) &&
                approxEqual(m12, mat.m12) &&
                approxEqual(m13, mat.m13) &&
                approxEqual(m14, mat.m14) &&
                approxEqual(m21, mat.m21) &&
                approxEqual(m22, mat.m22) &&
                approxEqual(m23, mat.m23) &&
                approxEqual(m24, mat.m24) &&
                approxEqual(m31, mat.m31) &&
                approxEqual(m32, mat.m32) &&
                approxEqual(m33, mat.m33) &&
                approxEqual(m34, mat.m34) &&
                approxEqual(m41, mat.m41) &&
                approxEqual(m42, mat.m42) &&
                approxEqual(m43, mat.m43) &&
                approxEqual(m44, mat.m44);
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

        if (Float.floatToIntBits(mat.m11) != Float.floatToIntBits(m11)) return false;
        if (Float.floatToIntBits(mat.m12) != Float.floatToIntBits(m12)) return false;
        if (Float.floatToIntBits(mat.m13) != Float.floatToIntBits(m13)) return false;
        if (Float.floatToIntBits(mat.m14) != Float.floatToIntBits(m14)) return false;
        if (Float.floatToIntBits(mat.m21) != Float.floatToIntBits(m21)) return false;
        if (Float.floatToIntBits(mat.m22) != Float.floatToIntBits(m22)) return false;
        if (Float.floatToIntBits(mat.m23) != Float.floatToIntBits(m23)) return false;
        if (Float.floatToIntBits(mat.m24) != Float.floatToIntBits(m24)) return false;
        if (Float.floatToIntBits(mat.m31) != Float.floatToIntBits(m31)) return false;
        if (Float.floatToIntBits(mat.m32) != Float.floatToIntBits(m32)) return false;
        if (Float.floatToIntBits(mat.m33) != Float.floatToIntBits(m33)) return false;
        if (Float.floatToIntBits(mat.m34) != Float.floatToIntBits(m34)) return false;
        if (Float.floatToIntBits(mat.m41) != Float.floatToIntBits(m41)) return false;
        if (Float.floatToIntBits(mat.m42) != Float.floatToIntBits(m42)) return false;
        if (Float.floatToIntBits(mat.m43) != Float.floatToIntBits(m43)) return false;
        return Float.floatToIntBits(mat.m44) == Float.floatToIntBits(m44);
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(m11);
        result = 31 * result + Float.floatToIntBits(m12);
        result = 31 * result + Float.floatToIntBits(m13);
        result = 31 * result + Float.floatToIntBits(m14);
        result = 31 * result + Float.floatToIntBits(m21);
        result = 31 * result + Float.floatToIntBits(m22);
        result = 31 * result + Float.floatToIntBits(m23);
        result = 31 * result + Float.floatToIntBits(m24);
        result = 31 * result + Float.floatToIntBits(m31);
        result = 31 * result + Float.floatToIntBits(m32);
        result = 31 * result + Float.floatToIntBits(m33);
        result = 31 * result + Float.floatToIntBits(m34);
        result = 31 * result + Float.floatToIntBits(m41);
        result = 31 * result + Float.floatToIntBits(m42);
        result = 31 * result + Float.floatToIntBits(m43);
        result = 31 * result + Float.floatToIntBits(m44);
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
     * @return a deep copy of this matrix
     */
    @Nonnull
    public Matrix4 copy() {
        try {
            return (Matrix4) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
