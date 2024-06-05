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

import org.lwjgl.system.MemoryUtil;

import javax.annotation.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * This class represents a 4x4 matrix and a 3D transformation, using the
 * right-hand rule.
 * <p>
 * The memory layout (order of components) is the same as GLSL's column-major and
 * HLSL's row-major, this is just a difference in naming and writing.
 * A matrix looks like this:
 * <p>
 * [m11  m12  m13  m14]<br>
 * [m21  m22  m23  m24]<br>
 * [m31  m32  m33  m34]<br>
 * [m41  m42  m43  m44]<br>
 * <p>
 * Where [m41 m42 m43] represents the translation, and they are the last four
 * elements in memory.
 *
 * @author BloCamLimb
 * @see Matrix
 */
//TODO replace with Vector API and Primitive Classes
@SuppressWarnings("unused")
public non-sealed class Matrix4 implements Matrix4c, Cloneable {

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
     * Create a matrix copied from an existing matrix.
     *
     * @param m the matrix to create from
     */
    public Matrix4(@Nonnull Matrix4c m) {
        m.store(this);
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
     * Create a copy of {@code mat} if not null, otherwise a new identity matrix.
     * Note: we assume null is identity.
     *
     * @param m the matrix to copy from
     * @return a copy of the matrix
     */
    @Nonnull
    public static Matrix4 copy(@Nullable Matrix4 m) {
        return m == null ? identity() : m.clone();
    }

    /**
     * Create a new identity matrix.
     *
     * @return an identity matrix
     */
    @Nonnull
    public static Matrix4 identity() {
        final Matrix4 m = new Matrix4();
        m.m11 = m.m22 = m.m33 = m.m44 = 1.0f;
        return m;
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
        final Matrix4 m = new Matrix4();
        m.m11 = 1.0f;
        m.m22 = 1.0f;
        m.m33 = 1.0f;
        m.m41 = x;
        m.m42 = y;
        m.m43 = z;
        m.m44 = 1.0f;
        return m;
    }

    /**
     * Create a new scaling transformation matrix.
     *
     * @param x the x-component of the scaling
     * @param y the y-component of the scaling
     * @param z the z-component of the scaling
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makeScale(float x, float y, float z) {
        final Matrix4 m = new Matrix4();
        m.m11 = x;
        m.m22 = y;
        m.m33 = z;
        m.m44 = 1.0f;
        return m;
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
     * @param flipY  whether to flip the projection vertically
     * @return the resulting matrix
     */
    @Nonnull
    public static Matrix4 makeOrthographic(float width, float height, float near, float far, boolean flipY) {
        final Matrix4 mat = new Matrix4();
        float invNF = 1.0f / (near - far);
        mat.m11 = 2.0f / width;
        mat.m22 = flipY ? -2.0f / height : 2.0f / height;
        mat.m33 = 2.0f * invNF;
        mat.m41 = -1.0f;
        mat.m42 = flipY ? 1.0f : -1.0f;
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

    public float m11() {
        return m11;
    }

    public float m12() {
        return m12;
    }

    public float m13() {
        return m13;
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

    public float m23() {
        return m23;
    }

    public float m24() {
        return m24;
    }

    public float m31() {
        return m31;
    }

    public float m32() {
        return m32;
    }

    public float m33() {
        return m33;
    }

    public float m34() {
        return m34;
    }

    public float m41() {
        return m41;
    }

    public float m42() {
        return m42;
    }

    public float m43() {
        return m43;
    }

    public float m44() {
        return m44;
    }

    /**
     * Add each element of the given matrix to the corresponding element of this matrix.
     *
     * @param m the addend
     */
    public void add(@Nonnull Matrix4 m) {
        m11 += m.m11;
        m12 += m.m12;
        m13 += m.m13;
        m14 += m.m14;
        m21 += m.m21;
        m22 += m.m22;
        m23 += m.m23;
        m24 += m.m24;
        m31 += m.m31;
        m32 += m.m32;
        m33 += m.m33;
        m34 += m.m34;
        m41 += m.m41;
        m42 += m.m42;
        m43 += m.m43;
        m44 += m.m44;
    }

    /**
     * Subtract each element of the given matrix from the corresponding element of this matrix.
     *
     * @param m the subtrahend
     */
    public void subtract(@Nonnull Matrix4 m) {
        m11 -= m.m11;
        m12 -= m.m12;
        m13 -= m.m13;
        m14 -= m.m14;
        m21 -= m.m21;
        m22 -= m.m22;
        m23 -= m.m23;
        m24 -= m.m24;
        m31 -= m.m31;
        m32 -= m.m32;
        m33 -= m.m33;
        m34 -= m.m34;
        m41 -= m.m41;
        m42 -= m.m42;
        m43 -= m.m43;
        m44 -= m.m44;
    }

    /**
     * Pre-multiply this matrix by the given <code>lhs</code> matrix.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>L</code> the <code>lhs</code>
     * matrix, then the new matrix will be <code>L * M</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * L * M</code>, the
     * transformation of the left-hand side matrix will be applied first.
     *
     * @param lhs the left-hand side matrix to multiply
     */
    public void preConcat(@Nonnull Matrix4 lhs) {
        // 64 multiplications
        final float f11 = lhs.m11 * m11 + lhs.m12 * m21 + lhs.m13 * m31 + lhs.m14 * m41;
        final float f12 = lhs.m11 * m12 + lhs.m12 * m22 + lhs.m13 * m32 + lhs.m14 * m42;
        final float f13 = lhs.m11 * m13 + lhs.m12 * m23 + lhs.m13 * m33 + lhs.m14 * m43;
        final float f14 = lhs.m11 * m14 + lhs.m12 * m24 + lhs.m13 * m34 + lhs.m14 * m44;
        final float f21 = lhs.m21 * m11 + lhs.m22 * m21 + lhs.m23 * m31 + lhs.m24 * m41;
        final float f22 = lhs.m21 * m12 + lhs.m22 * m22 + lhs.m23 * m32 + lhs.m24 * m42;
        final float f23 = lhs.m21 * m13 + lhs.m22 * m23 + lhs.m23 * m33 + lhs.m24 * m43;
        final float f24 = lhs.m21 * m14 + lhs.m22 * m24 + lhs.m23 * m34 + lhs.m24 * m44;
        final float f31 = lhs.m31 * m11 + lhs.m32 * m21 + lhs.m33 * m31 + lhs.m34 * m41;
        final float f32 = lhs.m31 * m12 + lhs.m32 * m22 + lhs.m33 * m32 + lhs.m34 * m42;
        final float f33 = lhs.m31 * m13 + lhs.m32 * m23 + lhs.m33 * m33 + lhs.m34 * m43;
        final float f34 = lhs.m31 * m14 + lhs.m32 * m24 + lhs.m33 * m34 + lhs.m34 * m44;
        final float f41 = lhs.m41 * m11 + lhs.m42 * m21 + lhs.m43 * m31 + lhs.m44 * m41;
        final float f42 = lhs.m41 * m12 + lhs.m42 * m22 + lhs.m43 * m32 + lhs.m44 * m42;
        final float f43 = lhs.m41 * m13 + lhs.m42 * m23 + lhs.m43 * m33 + lhs.m44 * m43;
        final float f44 = lhs.m41 * m14 + lhs.m42 * m24 + lhs.m43 * m34 + lhs.m44 * m44;
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
     * Pre-multiply this matrix by the given <code>lhs</code> matrix.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>L</code> the <code>lhs</code>
     * matrix, then the new matrix will be <code>L * M</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * L * M</code>, the
     * transformation of the left-hand side matrix will be applied first.
     *
     * @param l11 the m11 element of the left-hand side matrix
     * @param l12 the m12 element of the left-hand side matrix
     * @param l13 the m13 element of the left-hand side matrix
     * @param l14 the m14 element of the left-hand side matrix
     * @param l21 the m21 element of the left-hand side matrix
     * @param l22 the m22 element of the left-hand side matrix
     * @param l23 the m23 element of the left-hand side matrix
     * @param l24 the m24 element of the left-hand side matrix
     * @param l31 the m31 element of the left-hand side matrix
     * @param l32 the m32 element of the left-hand side matrix
     * @param l33 the m33 element of the left-hand side matrix
     * @param l34 the m34 element of the left-hand side matrix
     * @param l41 the m41 element of the left-hand side matrix
     * @param l42 the m42 element of the left-hand side matrix
     * @param l43 the m43 element of the left-hand side matrix
     * @param l44 the m44 element of the left-hand side matrix
     */
    public void preConcat(float l11, float l12, float l13, float l14,
                          float l21, float l22, float l23, float l24,
                          float l31, float l32, float l33, float l34,
                          float l41, float l42, float l43, float l44) {
        // 64 multiplications
        final float f11 = l11 * m11 + l12 * m21 + l13 * m31 + l14 * m41;
        final float f12 = l11 * m12 + l12 * m22 + l13 * m32 + l14 * m42;
        final float f13 = l11 * m13 + l12 * m23 + l13 * m33 + l14 * m43;
        final float f14 = l11 * m14 + l12 * m24 + l13 * m34 + l14 * m44;
        final float f21 = l21 * m11 + l22 * m21 + l23 * m31 + l24 * m41;
        final float f22 = l21 * m12 + l22 * m22 + l23 * m32 + l24 * m42;
        final float f23 = l21 * m13 + l22 * m23 + l23 * m33 + l24 * m43;
        final float f24 = l21 * m14 + l22 * m24 + l23 * m34 + l24 * m44;
        final float f31 = l31 * m11 + l32 * m21 + l33 * m31 + l34 * m41;
        final float f32 = l31 * m12 + l32 * m22 + l33 * m32 + l34 * m42;
        final float f33 = l31 * m13 + l32 * m23 + l33 * m33 + l34 * m43;
        final float f34 = l31 * m14 + l32 * m24 + l33 * m34 + l34 * m44;
        final float f41 = l41 * m11 + l42 * m21 + l43 * m31 + l44 * m41;
        final float f42 = l41 * m12 + l42 * m22 + l43 * m32 + l44 * m42;
        final float f43 = l41 * m13 + l42 * m23 + l43 * m33 + l44 * m43;
        final float f44 = l41 * m14 + l42 * m24 + l43 * m34 + l44 * m44;
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
     * Post-multiply this matrix by the given <code>rhs</code> matrix.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>R</code> the <code>rhs</code>
     * matrix, then the new matrix will be <code>M * R</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * M * R</code>, the
     * transformation of <code>this</code> matrix will be applied first.
     *
     * @param rhs the right-hand side matrix to multiply
     */
    public void postConcat(@Nonnull Matrix4 rhs) {
        // 64 multiplications
        final float f11 = m11 * rhs.m11 + m12 * rhs.m21 + m13 * rhs.m31 + m14 * rhs.m41;
        final float f12 = m11 * rhs.m12 + m12 * rhs.m22 + m13 * rhs.m32 + m14 * rhs.m42;
        final float f13 = m11 * rhs.m13 + m12 * rhs.m23 + m13 * rhs.m33 + m14 * rhs.m43;
        final float f14 = m11 * rhs.m14 + m12 * rhs.m24 + m13 * rhs.m34 + m14 * rhs.m44;
        final float f21 = m21 * rhs.m11 + m22 * rhs.m21 + m23 * rhs.m31 + m24 * rhs.m41;
        final float f22 = m21 * rhs.m12 + m22 * rhs.m22 + m23 * rhs.m32 + m24 * rhs.m42;
        final float f23 = m21 * rhs.m13 + m22 * rhs.m23 + m23 * rhs.m33 + m24 * rhs.m43;
        final float f24 = m21 * rhs.m14 + m22 * rhs.m24 + m23 * rhs.m34 + m24 * rhs.m44;
        final float f31 = m31 * rhs.m11 + m32 * rhs.m21 + m33 * rhs.m31 + m34 * rhs.m41;
        final float f32 = m31 * rhs.m12 + m32 * rhs.m22 + m33 * rhs.m32 + m34 * rhs.m42;
        final float f33 = m31 * rhs.m13 + m32 * rhs.m23 + m33 * rhs.m33 + m34 * rhs.m43;
        final float f34 = m31 * rhs.m14 + m32 * rhs.m24 + m33 * rhs.m34 + m34 * rhs.m44;
        final float f41 = m41 * rhs.m11 + m42 * rhs.m21 + m43 * rhs.m31 + m44 * rhs.m41;
        final float f42 = m41 * rhs.m12 + m42 * rhs.m22 + m43 * rhs.m32 + m44 * rhs.m42;
        final float f43 = m41 * rhs.m13 + m42 * rhs.m23 + m43 * rhs.m33 + m44 * rhs.m43;
        final float f44 = m41 * rhs.m14 + m42 * rhs.m24 + m43 * rhs.m34 + m44 * rhs.m44;
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
     * Post-multiply this matrix by the given <code>rhs</code> matrix.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>R</code> the <code>rhs</code>
     * matrix, then the new matrix will be <code>M * R</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * M * R</code>, the
     * transformation of <code>this</code> matrix will be applied first.
     *
     * @param r11 the m11 element of the right-hand side matrix
     * @param r12 the m12 element of the right-hand side matrix
     * @param r13 the m13 element of the right-hand side matrix
     * @param r14 the m14 element of the right-hand side matrix
     * @param r21 the m21 element of the right-hand side matrix
     * @param r22 the m22 element of the right-hand side matrix
     * @param r23 the m23 element of the right-hand side matrix
     * @param r24 the m24 element of the right-hand side matrix
     * @param r31 the m31 element of the right-hand side matrix
     * @param r32 the m32 element of the right-hand side matrix
     * @param r33 the m33 element of the right-hand side matrix
     * @param r34 the m34 element of the right-hand side matrix
     * @param r41 the m41 element of the right-hand side matrix
     * @param r42 the m42 element of the right-hand side matrix
     * @param r43 the m43 element of the right-hand side matrix
     * @param r44 the m44 element of the right-hand side matrix
     */
    public void postConcat(float r11, float r12, float r13, float r14,
                           float r21, float r22, float r23, float r24,
                           float r31, float r32, float r33, float r34,
                           float r41, float r42, float r43, float r44) {
        // 64 multiplications
        final float f11 = m11 * r11 + m12 * r21 + m13 * r31 + m14 * r41;
        final float f12 = m11 * r12 + m12 * r22 + m13 * r32 + m14 * r42;
        final float f13 = m11 * r13 + m12 * r23 + m13 * r33 + m14 * r43;
        final float f14 = m11 * r14 + m12 * r24 + m13 * r34 + m14 * r44;
        final float f21 = m21 * r11 + m22 * r21 + m23 * r31 + m24 * r41;
        final float f22 = m21 * r12 + m22 * r22 + m23 * r32 + m24 * r42;
        final float f23 = m21 * r13 + m22 * r23 + m23 * r33 + m24 * r43;
        final float f24 = m21 * r14 + m22 * r24 + m23 * r34 + m24 * r44;
        final float f31 = m31 * r11 + m32 * r21 + m33 * r31 + m34 * r41;
        final float f32 = m31 * r12 + m32 * r22 + m33 * r32 + m34 * r42;
        final float f33 = m31 * r13 + m32 * r23 + m33 * r33 + m34 * r43;
        final float f34 = m31 * r14 + m32 * r24 + m33 * r34 + m34 * r44;
        final float f41 = m41 * r11 + m42 * r21 + m43 * r31 + m44 * r41;
        final float f42 = m41 * r12 + m42 * r22 + m43 * r32 + m44 * r42;
        final float f43 = m41 * r13 + m42 * r23 + m43 * r33 + m44 * r43;
        final float f44 = m41 * r14 + m42 * r24 + m43 * r34 + m44 * r44;
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
     * Pre-multiply this matrix by the given <code>lhs</code> matrix. The matrix will be
     * expanded to a 4x4 matrix.
     * <pre>{@code
     * [ a b c ]      [ a b 0 c ]
     * [ d e f ]  ->  [ d e 0 f ]
     * [ g h i ]      [ 0 0 1 0 ]
     *                [ g h 0 i ]
     * }</pre>
     * If <code>M</code> is <code>this</code> matrix and <code>L</code> the <code>lhs</code>
     * matrix, then the new matrix will be <code>L * M</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * L * M</code>, the
     * transformation of the left-hand side matrix will be applied first.
     *
     * @param lhs the left-hand side matrix to multiply
     */
    public void preConcat2D(@Nonnull Matrixc lhs) {
        // 36 multiplications
        final float f11 = lhs.m11() * m11 + lhs.m12() * m21 + lhs.m14() * m41;
        final float f12 = lhs.m11() * m12 + lhs.m12() * m22 + lhs.m14() * m42;
        final float f13 = lhs.m11() * m13 + lhs.m12() * m23 + lhs.m14() * m43;
        final float f14 = lhs.m11() * m14 + lhs.m12() * m24 + lhs.m14() * m44;
        final float f21 = lhs.m21() * m11 + lhs.m22() * m21 + lhs.m24() * m41;
        final float f22 = lhs.m21() * m12 + lhs.m22() * m22 + lhs.m24() * m42;
        final float f23 = lhs.m21() * m13 + lhs.m22() * m23 + lhs.m24() * m43;
        final float f24 = lhs.m21() * m14 + lhs.m22() * m24 + lhs.m24() * m44;
        final float f41 = lhs.m41() * m11 + lhs.m42() * m21 + lhs.m44() * m41;
        final float f42 = lhs.m41() * m12 + lhs.m42() * m22 + lhs.m44() * m42;
        final float f43 = lhs.m41() * m13 + lhs.m42() * m23 + lhs.m44() * m43;
        final float f44 = lhs.m41() * m14 + lhs.m42() * m24 + lhs.m44() * m44;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m24 = f24;
        m41 = f41;
        m42 = f42;
        m43 = f43;
        m44 = f44;
    }

    /**
     * Post-multiply this matrix by the given <code>rhs</code> matrix. The matrix will be
     * expanded to a 4x4 matrix.
     * <pre>{@code
     * [ a b c ]      [ a b 0 c ]
     * [ d e f ]  ->  [ d e 0 f ]
     * [ g h i ]      [ 0 0 1 0 ]
     *                [ g h 0 i ]
     * }</pre>
     * If <code>M</code> is <code>this</code> matrix and <code>R</code> the <code>rhs</code>
     * matrix, then the new matrix will be <code>M * R</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * M * R</code>, the
     * transformation of <code>this</code> matrix will be applied first.
     *
     * @param rhs the right-hand side matrix to multiply
     */
    public void postConcat2D(@Nonnull Matrixc rhs) {
        // 36 multiplications
        final float f11 = m11 * rhs.m11() + m12 * rhs.m21() + m14 * rhs.m41();
        final float f12 = m11 * rhs.m12() + m12 * rhs.m22() + m14 * rhs.m42();
        final float f14 = m11 * rhs.m14() + m12 * rhs.m24() + m14 * rhs.m44();
        final float f21 = m21 * rhs.m11() + m22 * rhs.m21() + m24 * rhs.m41();
        final float f22 = m21 * rhs.m12() + m22 * rhs.m22() + m24 * rhs.m42();
        final float f24 = m21 * rhs.m14() + m22 * rhs.m24() + m24 * rhs.m44();
        final float f31 = m31 * rhs.m11() + m32 * rhs.m21() + m34 * rhs.m41();
        final float f32 = m31 * rhs.m12() + m32 * rhs.m22() + m34 * rhs.m42();
        final float f34 = m31 * rhs.m14() + m32 * rhs.m24() + m34 * rhs.m44();
        final float f41 = m41 * rhs.m11() + m42 * rhs.m21() + m44 * rhs.m41();
        final float f42 = m41 * rhs.m12() + m42 * rhs.m22() + m44 * rhs.m42();
        final float f44 = m41 * rhs.m14() + m42 * rhs.m24() + m44 * rhs.m44();
        m11 = f11;
        m12 = f12;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m24 = f24;
        m31 = f31;
        m32 = f32;
        m34 = f34;
        m41 = f41;
        m42 = f42;
        m44 = f44;
    }

    /**
     * Pre-multiply this matrix by the given <code>lhs</code> matrix. The matrix will be
     * expanded to a 4x4 matrix.
     * <pre>{@code
     * [ a b c ]      [ a b c 0 ]
     * [ d e f ]  ->  [ d e f 0 ]
     * [ g h i ]      [ g h i 0 ]
     *                [ 0 0 0 1 ]
     * }</pre>
     * If <code>M</code> is <code>this</code> matrix and <code>L</code> the <code>lhs</code>
     * matrix, then the new matrix will be <code>L * M</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * L * M</code>, the
     * transformation of the left-hand side matrix will be applied first.
     *
     * @param lhs the left-hand side matrix to multiply
     */
    public void preConcat(@Nonnull Matrix3 lhs) {
        // 36 multiplications
        final float f11 = lhs.m11 * m11 + lhs.m12 * m21 + lhs.m13 * m31;
        final float f12 = lhs.m11 * m12 + lhs.m12 * m22 + lhs.m13 * m32;
        final float f13 = lhs.m11 * m13 + lhs.m12 * m23 + lhs.m13 * m33;
        final float f14 = lhs.m11 * m14 + lhs.m12 * m24 + lhs.m13 * m34;
        final float f21 = lhs.m21 * m11 + lhs.m22 * m21 + lhs.m23 * m31;
        final float f22 = lhs.m21 * m12 + lhs.m22 * m22 + lhs.m23 * m32;
        final float f23 = lhs.m21 * m13 + lhs.m22 * m23 + lhs.m23 * m33;
        final float f24 = lhs.m21 * m14 + lhs.m22 * m24 + lhs.m23 * m34;
        final float f31 = lhs.m31 * m11 + lhs.m32 * m21 + lhs.m33 * m31;
        final float f32 = lhs.m31 * m12 + lhs.m32 * m22 + lhs.m33 * m32;
        final float f33 = lhs.m31 * m13 + lhs.m32 * m23 + lhs.m33 * m33;
        final float f34 = lhs.m31 * m14 + lhs.m32 * m24 + lhs.m33 * m34;
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
     * Post-multiply this matrix by the given <code>rhs</code> matrix. The matrix will be
     * expanded to a 4x4 matrix.
     * <pre>{@code
     * [ a b c ]      [ a b c 0 ]
     * [ d e f ]  ->  [ d e f 0 ]
     * [ g h i ]      [ g h i 0 ]
     *                [ 0 0 0 1 ]
     * }</pre>
     * If <code>M</code> is <code>this</code> matrix and <code>R</code> the <code>rhs</code>
     * matrix, then the new matrix will be <code>M * R</code> (row-major). So when transforming
     * a vector <code>v</code> with the new matrix by using <code>v * M * R</code>, the
     * transformation of <code>this</code> matrix will be applied first.
     *
     * @param rhs the right-hand side matrix to multiply
     */
    public void postConcat(@Nonnull Matrix3 rhs) {
        // 36 multiplications
        final float f11 = m11 * rhs.m11 + m12 * rhs.m21 + m13 * rhs.m31;
        final float f12 = m11 * rhs.m12 + m12 * rhs.m22 + m13 * rhs.m32;
        final float f13 = m11 * rhs.m13 + m12 * rhs.m23 + m13 * rhs.m33;
        final float f21 = m21 * rhs.m11 + m22 * rhs.m21 + m23 * rhs.m31;
        final float f22 = m21 * rhs.m12 + m22 * rhs.m22 + m23 * rhs.m32;
        final float f23 = m21 * rhs.m13 + m22 * rhs.m23 + m23 * rhs.m33;
        final float f31 = m31 * rhs.m11 + m32 * rhs.m21 + m33 * rhs.m31;
        final float f32 = m31 * rhs.m12 + m32 * rhs.m22 + m33 * rhs.m32;
        final float f33 = m31 * rhs.m13 + m32 * rhs.m23 + m33 * rhs.m33;
        final float f41 = m41 * rhs.m11 + m42 * rhs.m21 + m43 * rhs.m31;
        final float f42 = m41 * rhs.m12 + m42 * rhs.m22 + m43 * rhs.m32;
        final float f43 = m41 * rhs.m13 + m42 * rhs.m23 + m43 * rhs.m33;
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
     * Set all elements of this matrix to <code>0</code>.
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
     * Reset this matrix to the identity.
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
     * Store the values of the given matrix into this matrix.
     *
     * @param m the matrix to copy from
     */
    public void set(@Nonnull Matrix4c m) {
        m.store(this);
    }

    /**
     * Set the values within this matrix to the given float values.
     *
     * @param m11 the new value of m11
     * @param m12 the new value of m12
     * @param m13 the new value of m13
     * @param m14 the new value of m14
     * @param m21 the new value of m21
     * @param m22 the new value of m22
     * @param m23 the new value of m23
     * @param m24 the new value of m24
     * @param m31 the new value of m31
     * @param m32 the new value of m32
     * @param m33 the new value of m33
     * @param m34 the new value of m34
     * @param m41 the new value of m41
     * @param m42 the new value of m42
     * @param m43 the new value of m43
     * @param m44 the new value of m44
     */
    public void set(float m11, float m12, float m13, float m14,
                    float m21, float m22, float m23, float m24,
                    float m31, float m32, float m33, float m34,
                    float m41, float m42, float m43, float m44) {
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m14 = m14;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m24 = m24;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        this.m34 = m34;
        this.m41 = m41;
        this.m42 = m42;
        this.m43 = m43;
        this.m44 = m44;
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
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
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a      the array to copy from
     * @param offset the element offset
     */
    public void set(@Nonnull float[] a, int offset) {
        m11 = a[offset];
        m12 = a[offset + 1];
        m13 = a[offset + 2];
        m14 = a[offset + 3];
        m21 = a[offset + 4];
        m22 = a[offset + 5];
        m23 = a[offset + 6];
        m24 = a[offset + 7];
        m31 = a[offset + 8];
        m32 = a[offset + 9];
        m33 = a[offset + 10];
        m34 = a[offset + 11];
        m41 = a[offset + 12];
        m42 = a[offset + 13];
        m43 = a[offset + 14];
        m44 = a[offset + 15];
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a the array to copy from
     */
    public void set(@Nonnull ByteBuffer a) {
        int offset = a.position();
        m11 = a.getFloat(offset);
        m12 = a.getFloat(offset + 4);
        m13 = a.getFloat(offset + 8);
        m14 = a.getFloat(offset + 12);
        m21 = a.getFloat(offset + 16);
        m22 = a.getFloat(offset + 20);
        m23 = a.getFloat(offset + 24);
        m24 = a.getFloat(offset + 28);
        m31 = a.getFloat(offset + 32);
        m32 = a.getFloat(offset + 36);
        m33 = a.getFloat(offset + 40);
        m34 = a.getFloat(offset + 44);
        m41 = a.getFloat(offset + 48);
        m42 = a.getFloat(offset + 52);
        m43 = a.getFloat(offset + 56);
        m44 = a.getFloat(offset + 60);
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a the array to copy from
     */
    public void set(@Nonnull FloatBuffer a) {
        int offset = a.position();
        m11 = a.get(offset);
        m12 = a.get(offset + 1);
        m13 = a.get(offset + 2);
        m14 = a.get(offset + 3);
        m21 = a.get(offset + 4);
        m22 = a.get(offset + 5);
        m23 = a.get(offset + 6);
        m24 = a.get(offset + 7);
        m31 = a.get(offset + 8);
        m32 = a.get(offset + 9);
        m33 = a.get(offset + 10);
        m34 = a.get(offset + 11);
        m41 = a.get(offset + 12);
        m42 = a.get(offset + 13);
        m43 = a.get(offset + 14);
        m44 = a.get(offset + 15);
    }

    /**
     * Set the values in the matrix using an address that contains
     * the matrix elements in row-major order (UNSAFE).
     *
     * @param p the pointer of the array to copy from
     */
    public void set(long p) {
        m11 = MemoryUtil.memGetFloat(p);
        m12 = MemoryUtil.memGetFloat(p + 4);
        m13 = MemoryUtil.memGetFloat(p + 8);
        m14 = MemoryUtil.memGetFloat(p + 12);
        m21 = MemoryUtil.memGetFloat(p + 16);
        m22 = MemoryUtil.memGetFloat(p + 20);
        m23 = MemoryUtil.memGetFloat(p + 24);
        m24 = MemoryUtil.memGetFloat(p + 28);
        m31 = MemoryUtil.memGetFloat(p + 32);
        m32 = MemoryUtil.memGetFloat(p + 36);
        m33 = MemoryUtil.memGetFloat(p + 40);
        m34 = MemoryUtil.memGetFloat(p + 44);
        m41 = MemoryUtil.memGetFloat(p + 48);
        m42 = MemoryUtil.memGetFloat(p + 52);
        m43 = MemoryUtil.memGetFloat(p + 56);
        m44 = MemoryUtil.memGetFloat(p + 60);
    }

    /**
     * Store this matrix elements to the given matrix.
     *
     * @param m the matrix to store
     */
    public void store(@Nonnull Matrix4 m) {
        m.m11 = m11;
        m.m12 = m12;
        m.m13 = m13;
        m.m14 = m14;
        m.m21 = m21;
        m.m22 = m22;
        m.m23 = m23;
        m.m24 = m24;
        m.m31 = m31;
        m.m32 = m32;
        m.m33 = m33;
        m.m34 = m34;
        m.m41 = m41;
        m.m42 = m42;
        m.m43 = m43;
        m.m44 = m44;
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the array to store into
     */
    public void store(@Nonnull float[] a) {
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
     * Store this matrix into the give float array in row-major order.
     *
     * @param a      the array to store into
     * @param offset the element offset
     */
    public void store(@Nonnull float[] a, int offset) {
        a[offset] = m11;
        a[offset + 1] = m12;
        a[offset + 2] = m13;
        a[offset + 3] = m14;
        a[offset + 4] = m21;
        a[offset + 5] = m22;
        a[offset + 6] = m23;
        a[offset + 7] = m24;
        a[offset + 8] = m31;
        a[offset + 9] = m32;
        a[offset + 10] = m33;
        a[offset + 11] = m34;
        a[offset + 12] = m41;
        a[offset + 13] = m42;
        a[offset + 14] = m43;
        a[offset + 15] = m44;
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    public void store(@Nonnull ByteBuffer a) {
        int offset = a.position();
        a.putFloat(offset, m11);
        a.putFloat(offset + 4, m12);
        a.putFloat(offset + 8, m13);
        a.putFloat(offset + 12, m14);
        a.putFloat(offset + 16, m21);
        a.putFloat(offset + 20, m22);
        a.putFloat(offset + 24, m23);
        a.putFloat(offset + 28, m24);
        a.putFloat(offset + 32, m31);
        a.putFloat(offset + 36, m32);
        a.putFloat(offset + 40, m33);
        a.putFloat(offset + 44, m34);
        a.putFloat(offset + 48, m41);
        a.putFloat(offset + 52, m42);
        a.putFloat(offset + 56, m43);
        a.putFloat(offset + 60, m44);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    public void store(@Nonnull FloatBuffer a) {
        int offset = a.position();
        a.put(offset, m11);
        a.put(offset + 1, m12);
        a.put(offset + 2, m13);
        a.put(offset + 3, m14);
        a.put(offset + 4, m21);
        a.put(offset + 5, m22);
        a.put(offset + 6, m23);
        a.put(offset + 7, m24);
        a.put(offset + 8, m31);
        a.put(offset + 9, m32);
        a.put(offset + 10, m33);
        a.put(offset + 11, m34);
        a.put(offset + 12, m41);
        a.put(offset + 13, m42);
        a.put(offset + 14, m43);
        a.put(offset + 15, m44);
    }

    /**
     * Store this matrix into the given address in GLSL column-major or
     * HLSL row-major order.
     *
     * @param p the pointer of the array to store
     */
    public void store(long p) {
        MemoryUtil.memPutFloat(p, m11);
        MemoryUtil.memPutFloat(p + 4, m12);
        MemoryUtil.memPutFloat(p + 8, m13);
        MemoryUtil.memPutFloat(p + 12, m14);
        MemoryUtil.memPutFloat(p + 16, m21);
        MemoryUtil.memPutFloat(p + 20, m22);
        MemoryUtil.memPutFloat(p + 24, m23);
        MemoryUtil.memPutFloat(p + 28, m24);
        MemoryUtil.memPutFloat(p + 32, m31);
        MemoryUtil.memPutFloat(p + 36, m32);
        MemoryUtil.memPutFloat(p + 40, m33);
        MemoryUtil.memPutFloat(p + 44, m34);
        MemoryUtil.memPutFloat(p + 48, m41);
        MemoryUtil.memPutFloat(p + 52, m42);
        MemoryUtil.memPutFloat(p + 56, m43);
        MemoryUtil.memPutFloat(p + 60, m44);
    }

    /**
     * Return the determinant of this matrix.
     *
     * @return the determinant
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
     * Transpose this matrix.
     */
    public void transpose() {
        final float f12 = m21;
        final float f13 = m31;
        final float f14 = m41;
        final float f21 = m12;
        final float f23 = m32;
        final float f24 = m42;
        final float f31 = m13;
        final float f32 = m23;
        final float f34 = m43;
        final float f41 = m14;
        final float f42 = m24;
        final float f43 = m34;
        m12 = f12;
        m13 = f13;
        m14 = f14;
        m21 = f21;
        m23 = f23;
        m24 = f24;
        m31 = f31;
        m32 = f32;
        m34 = f34;
        m41 = f41;
        m42 = f42;
        m43 = f43;
    }

    /**
     * Compute the inverse of this matrix. This matrix will be inverted
     * if it is invertible, otherwise it keeps the same as before.
     *
     * @return {@code true} if this matrix is invertible.
     */
    @CheckReturnValue
    public boolean invert() {
        return invert(this);
    }

    /**
     * Compute the inverse of this matrix. The matrix will be inverted
     * if this matrix is invertible, otherwise it keeps the same as before.
     *
     * @param dest the destination matrix
     * @return {@code true} if this matrix is invertible.
     */
    @CheckReturnValue
    public boolean invert(@Nullable Matrix4 dest) {
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
        if (MathUtil.isApproxZero(det)) {
            return false;
        }
        if (dest != null) {
            // calc algebraic cofactor and transpose
            det = 1.0f / det;
            final float f11 = (m22 * b11 - m23 * b10 + m24 * b09) * det;
            final float f12 = (m23 * b08 - m21 * b11 - m24 * b07) * det;
            final float f13 = (m21 * b10 - m22 * b08 + m24 * b06) * det;
            final float f14 = (m22 * b07 - m21 * b09 - m23 * b06) * det;
            final float f21 = (m13 * b10 - m12 * b11 - m14 * b09) * det;
            final float f22 = (m11 * b11 - m13 * b08 + m14 * b07) * det;
            final float f23 = (m12 * b08 - m11 * b10 - m14 * b06) * det;
            final float f24 = (m11 * b09 - m12 * b07 + m13 * b06) * det;
            final float f31 = (m42 * b05 - m43 * b04 + m44 * b03) * det;
            final float f32 = (m43 * b02 - m41 * b05 - m44 * b01) * det;
            final float f33 = (m41 * b04 - m42 * b02 + m44 * b00) * det;
            final float f34 = (m42 * b01 - m41 * b03 - m43 * b00) * det;
            final float f41 = (m33 * b04 - m32 * b05 - m34 * b03) * det;
            final float f42 = (m31 * b05 - m33 * b02 + m34 * b01) * det;
            final float f43 = (m32 * b02 - m31 * b04 - m34 * b00) * det;
            final float f44 = (m31 * b03 - m32 * b01 + m33 * b00) * det;
            dest.m11 = f11;
            dest.m21 = f12;
            dest.m31 = f13;
            dest.m41 = f14;
            dest.m12 = f21;
            dest.m22 = f22;
            dest.m32 = f23;
            dest.m42 = f24;
            dest.m13 = f31;
            dest.m23 = f32;
            dest.m33 = f33;
            dest.m43 = f34;
            dest.m14 = f41;
            dest.m24 = f42;
            dest.m34 = f43;
            dest.m44 = f44;
        }
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
     * Set this matrix to an orthographic projection matrix.
     *
     * @param width  the distance from right frustum plane to left frustum plane
     * @param height the distance from bottom frustum plane to top frustum plane
     * @param near   the near frustum plane, must be positive
     * @param far    the far frustum plane, must be positive
     * @param flipY  whether to flip the projection vertically
     * @return this
     */
    @Nonnull
    public Matrix4 setOrthographic(float width, float height, float near, float far, boolean flipY) {
        float invNF = 1.0f / (near - far);
        m11 = 2.0f / width;
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = flipY ? -2.0f / height : 2.0f / height;
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = 2.0f * invNF;
        m34 = 0.0f;
        m41 = -1.0f;
        m42 = flipY ? 1.0f : -1.0f;
        m43 = (near + far) * invNF;
        m44 = 1.0f;
        return this;
    }

    /**
     * Set this matrix to be an orthographic projection transformation for a
     * right-handed coordinate system using the given NDC z range.
     *
     * @param left   the distance from the center to the left frustum edge
     * @param right  the distance from the center to the right frustum edge
     * @param bottom the distance from the center to the bottom frustum edge
     * @param top    the distance from the center to the top frustum edge
     * @param near   near clipping plane distance
     * @param far    far clipping plane distance
     * @param signed whether to use Vulkan's and Direct3D's NDC z range of <code>[0..+1]</code> when
     *               <code>false</code> or whether to use OpenGL's NDC z range of <code>[-1..+1]</code> when
     *               <code>true</code>
     */
    public void setOrthographic(float left, float right, float bottom, float top,
                                float near, float far, boolean signed) {
        m11 = 2.0f / (right - left);
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 2.0f / (top - bottom);
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = (signed ? 2.0f : 1.0f) / (near - far);
        m34 = 0.0f;
        m41 = (right + left) / (left - right);
        m42 = (top + bottom) / (bottom - top);
        m43 = (signed ? (far + near) : near) / (near - far);
        m44 = 1.0f;
    }

    /**
     * Set this matrix to be an orthographic projection transformation for a
     * left-handed coordinate system using the given NDC z range.
     *
     * @param left   the distance from the center to the left frustum edge
     * @param right  the distance from the center to the right frustum edge
     * @param bottom the distance from the center to the bottom frustum edge
     * @param top    the distance from the center to the top frustum edge
     * @param near   near clipping plane distance
     * @param far    far clipping plane distance
     * @param signed whether to use Vulkan's and Direct3D's NDC z range of <code>[0..+1]</code> when
     *               <code>false</code> or whether to use OpenGL's NDC z range of <code>[-1..+1]</code> when
     *               <code>true</code>
     */
    public void setOrthographicLH(float left, float right, float bottom, float top,
                                  float near, float far, boolean signed) {
        m11 = 2.0f / (right - left);
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 2.0f / (top - bottom);
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        m33 = (signed ? 2.0f : 1.0f) / (far - near);
        m34 = 0.0f;
        m41 = (right + left) / (left - right);
        m42 = (top + bottom) / (bottom - top);
        m43 = (signed ? (far + near) : near) / (near - far);
        m44 = 1.0f;
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
     * Set this matrix to be a symmetric perspective projection frustum transformation
     * for a right-handed coordinate system using the given NDC z range.
     *
     * @param fov    the field of view in radians (must be greater than zero and less than PI)
     * @param aspect the aspect ratio of the view (i.e. width / height)
     * @param near   the near clipping plane, must be positive
     * @param far    the far clipping plane, must be positive
     * @param signed whether to use Vulkan's and Direct3D's NDC z range of <code>[0..+1]</code> when
     *               <code>false</code> or whether to use OpenGL's NDC z range of <code>[-1..+1]</code> when
     *               <code>true</code>
     */
    public void setPerspective(double fov, double aspect, float near, float far, boolean signed) {
        double h = Math.tan(fov * 0.5);
        m11 = (float) (1.0 / (h * aspect));
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = (float) (1.0 / h);
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        if (far == Float.POSITIVE_INFINITY) {
            m33 = MathUtil.EPS - 1.0f;
            m43 = (MathUtil.EPS - (signed ? 2.0f : 1.0f)) * near;
        } else if (near == Float.POSITIVE_INFINITY) {
            m33 = (signed ? 1.0f : 0.0f) - MathUtil.EPS;
            m43 = ((signed ? 2.0f : 1.0f) - MathUtil.EPS) * far;
        } else {
            m33 = (signed ? (far + near) : far) / (near - far);
            m43 = (signed ? (far + far) : far) * near / (near - far);
        }
        m34 = -1.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m44 = 0.0f;
    }

    /**
     * Set this matrix to be a symmetric perspective projection frustum transformation
     * for a left-handed coordinate system using the given NDC z range.
     *
     * @param fov    the field of view in radians (must be greater than zero and less than PI)
     * @param aspect the aspect ratio of the view (i.e. width / height)
     * @param near   the near clipping plane, must be positive
     * @param far    the far clipping plane, must be positive
     * @param signed whether to use Vulkan's and Direct3D's NDC z range of <code>[0..+1]</code> when
     *               <code>false</code> or whether to use OpenGL's NDC z range of <code>[-1..+1]</code> when
     *               <code>true</code>
     */
    public void setPerspectiveLH(double fov, double aspect, float near, float far, boolean signed) {
        double h = Math.tan(fov * 0.5);
        m11 = (float) (1.0 / (h * aspect));
        m12 = 0.0f;
        m13 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = (float) (1.0 / h);
        m23 = 0.0f;
        m24 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        if (far == Float.POSITIVE_INFINITY) {
            m33 = MathUtil.EPS - 1.0f;
            m43 = (MathUtil.EPS - (signed ? 2.0f : 1.0f)) * near;
        } else if (near == Float.POSITIVE_INFINITY) {
            m33 = (signed ? 1.0f : 0.0f) - MathUtil.EPS;
            m43 = ((signed ? 2.0f : 1.0f) - MathUtil.EPS) * far;
        } else {
            m33 = (signed ? (far + near) : far) / (far - near);
            m43 = (signed ? (far + far) : far) * near / (near - far);
        }
        m34 = 1.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m44 = 0.0f;
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
     * pre-multiplying by a translation matrix. (translation * this)
     *
     * @param t the translation vector
     */
    public void preTranslate(@Nonnull Vector3 t) {
        preTranslate(t.x, t.y, t.z);
    }

    /**
     * Apply a translation to this matrix by translating by the given number of
     * units in x, y and z.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>T</code> the translation
     * matrix, then the new matrix will be <code>T * M</code> (row-major). So when
     * transforming a vector <code>v</code> with the new matrix by using
     * <code>v * T * M</code>, the translation will be applied first.
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
     * Apply a translation to this matrix by translating by the given number of
     * units in x, y and z.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>T</code> the translation
     * matrix, then the new matrix will be <code>T * M</code> (row-major). So when
     * transforming a vector <code>v</code> with the new matrix by using
     * <code>v * T * M</code>, the translation will be applied first.
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
     * post-multiplying by a translation matrix. (this * translation)
     *
     * @param t the translation vector
     */
    public void postTranslate(@Nonnull Vector3 t) {
        postTranslate(t.x, t.y, t.z);
    }

    /**
     * Post-multiply a translation to this matrix by translating by the given number of
     * units in x, y and z.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>T</code> the translation
     * matrix, then the new matrix will be <code>M * T</code> (row-major). So when
     * transforming a vector <code>v</code> with the new matrix by using
     * <code>v * M * T</code>, the translation will be applied last.
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
     * Post-multiply a translation to this matrix by translating by the given number of
     * units in x, y and z.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>T</code> the translation
     * matrix, then the new matrix will be <code>M * T</code> (row-major). So when
     * transforming a vector <code>v</code> with the new matrix by using
     * <code>v * M * T</code>, the translation will be applied last.
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
     * @param t the translation vector
     */
    public void setTranslate(@Nonnull Vector3 t) {
        setTranslate(t.x, t.y, t.z);
    }

    /**
     * Set this matrix to be a simple translation matrix.
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
     * @param s the scale vector
     */
    public void preScale(@Nonnull Vector3 s) {
        preScale(s.x, s.y, s.z);
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
     * @param s the scale vector
     */
    public void postScale(@Nonnull Vector3 s) {
        postScale(s.x, s.y, s.z);
    }

    /**
     * Post-multiply scaling to <code>this</code> matrix by scaling the base axes by the given x,
     * y and z factors and store the result in <code>dest</code>.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix,
     * then the new matrix will be <code>M * S</code> (row-major). So when transforming a
     * vector <code>v</code> with the new matrix by using <code>v * M * S</code>,
     * the scaling will be applied last.
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
     * Post-multiply scaling to <code>this</code> matrix by scaling the base axes by the given x,
     * y and z factors and store the result in <code>dest</code>.
     * <p>
     * If <code>M</code> is <code>this</code> matrix and <code>S</code> the scaling matrix,
     * then the new matrix will be <code>M * S</code> (row-major). So when transforming a
     * vector <code>v</code> with the new matrix by using <code>v * M * S</code>,
     * the scaling will be applied last.
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
     * @param s the scale vector
     */
    public void setScale(@Nonnull Vector3 s) {
        setScale(s.x, s.y, s.z);
    }

    /**
     * Set this matrix to be a simple scale matrix.
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
     * Pre-multiply shearing to <code>this</code> matrix by the given shearing coefficients.
     * <p>
     * This is equivalent to pre-multiplying by a shearing matrix.
     * <table border="1">
     *   <tr>
     *     <td>1</th>
     *     <td>sxy</th>
     *     <td>sxz</th>
     *   </tr>
     *   <tr>
     *     <td>syx</th>
     *     <td>1</th>
     *     <td>syz</th>
     *   </tr>
     *   <tr>
     *     <td>szx</th>
     *     <td>szy</th>
     *     <td>1</th>
     *   </tr>
     * </table>
     *
     * @param sxy the y-component of the shearing in x direction, x is unchanged
     * @param sxz the z-component of the shearing in x direction, x is unchanged
     * @param syx the x-component of the shearing in y direction, y is unchanged
     * @param syz the z-component of the shearing in y direction, y is unchanged
     * @param szx the x-component of the shearing in z direction, z is unchanged
     * @param szy the y-component of the shearing in z direction, z is unchanged
     */
    public void preShear(float sxy, float sxz,
                         float syx, float syz,
                         float szx, float szy) {
        final float f11 = m11 + sxy * m21 + sxz * m31;
        final float f12 = m12 + sxy * m22 + sxz * m32;
        final float f13 = m13 + sxy * m23 + sxz * m33;
        final float f14 = m14 + sxy * m24 + sxz * m34;
        final float f21 = syx * m11 + m21 + syz * m31;
        final float f22 = syx * m12 + m22 + syz * m32;
        final float f23 = syx * m13 + m23 + syz * m33;
        final float f24 = syx * m14 + m24 + syz * m34;
        final float f31 = szx * m11 + szy * m21 + m31;
        final float f32 = szx * m12 + szy * m22 + m32;
        final float f33 = szx * m13 + szy * m23 + m33;
        final float f34 = szx * m14 + szy * m24 + m34;
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
     * Post-multiply shearing to <code>this</code> matrix by the given shearing coefficients.
     * <p>
     * This is equivalent to post-multiplying by a shearing matrix.
     * <table border="1">
     *   <tr>
     *     <td>1</th>
     *     <td>sxy</th>
     *     <td>sxz</th>
     *   </tr>
     *   <tr>
     *     <td>syx</th>
     *     <td>1</th>
     *     <td>syz</th>
     *   </tr>
     *   <tr>
     *     <td>szx</th>
     *     <td>szy</th>
     *     <td>1</th>
     *   </tr>
     * </table>
     *
     * @param sxy the y-component of the shearing in x direction, x is unchanged
     * @param sxz the z-component of the shearing in x direction, x is unchanged
     * @param syx the x-component of the shearing in y direction, y is unchanged
     * @param syz the z-component of the shearing in y direction, y is unchanged
     * @param szx the x-component of the shearing in z direction, z is unchanged
     * @param szy the y-component of the shearing in z direction, z is unchanged
     */
    public void postShear(float sxy, float sxz,
                          float syx, float syz,
                          float szx, float szy) {
        final float f11 = m11 + m12 * syx + m13 * szx;
        final float f12 = m11 * sxy + m12 + m13 * szy;
        final float f13 = m11 * sxz + m12 * syz + m13;
        final float f21 = m21 + m22 * syx + m23 * szx;
        final float f22 = m21 * sxy + m22 + m23 * szy;
        final float f23 = m21 * sxz + m22 * syz + m23;
        final float f31 = m31 + m32 * syx + m33 * szx;
        final float f32 = m31 * sxy + m32 + m33 * szy;
        final float f33 = m31 * sxz + m32 * syz + m33;
        final float f41 = m41 + m42 * syx + m43 * szx;
        final float f42 = m41 * sxy + m42 + m43 * szy;
        final float f43 = m41 * sxz + m42 * syz + m43;
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
     * Pre-multiply 2D shearing to <code>this</code> matrix by the given shearing coefficients.
     *
     * @param sx the x-component of the shearing in y direction, y is unchanged (horizontal shearing)
     * @param sy the y-component of the shearing in x direction, x is unchanged (vertical shearing)
     */
    public void preShear2D(float sx, float sy) {
        final float f11 = m11 + sy * m21;
        final float f12 = m12 + sy * m22;
        final float f13 = m13 + sy * m23;
        final float f14 = m14 + sy * m24;
        final float f21 = sx * m11 + m21;
        final float f22 = sx * m12 + m22;
        final float f23 = sx * m13 + m23;
        final float f24 = sx * m14 + m24;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m24 = f24;
    }

    /**
     * Post-multiply 2D shearing to <code>this</code> matrix by the given shearing coefficients.
     *
     * @param sx the x-component of the shearing in y direction, y is unchanged (horizontal shearing)
     * @param sy the y-component of the shearing in x direction, x is unchanged (vertical shearing)
     */
    public void postShear2D(float sx, float sy) {
        final float f11 = m11 + m12 * sx;
        final float f12 = m11 * sy + m12;
        final float f21 = m21 + m22 * sx;
        final float f22 = m21 * sy + m22;
        final float f31 = m31 + m32 * sx;
        final float f32 = m31 * sy + m32;
        final float f41 = m41 + m42 * sx;
        final float f42 = m41 * sy + m42;
        m11 = f11;
        m12 = f12;
        m21 = f21;
        m22 = f22;
        m31 = f31;
        m32 = f32;
        m41 = f41;
        m42 = f42;
    }

    /**
     * Resets this matrix to a shearing matrix.
     */
    public void setShear(float sxy, float sxz,
                         float syx, float syz,
                         float szx, float szy) {
        m11 = 1.0f;
        m12 = sxy;
        m13 = sxz;
        m14 = 0.0f;
        m21 = syx;
        m22 = 1.0f;
        m23 = syz;
        m24 = 0.0f;
        m31 = szx;
        m32 = szy;
        m33 = 1.0f;
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
     * <table border="1">
     *   <tr>
     *     <td>1</th>
     *     <td>0</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>cos&theta;</th>
     *     <td>sin&theta;</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>-sin&theta;</th>
     *     <td>cos&theta;</th>
     *   </tr>
     * </table>
     *
     * @param angle the rotation angle in radians.
     */
    public void preRotateX(double angle) {
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f21 = c * m21 + s * m31;
        final double f22 = c * m22 + s * m32;
        final double f23 = c * m23 + s * m33;
        final double f24 = c * m24 + s * m34;
        m31 = (float) (c * m31 - s * m21);
        m32 = (float) (c * m32 - s * m22);
        m33 = (float) (c * m33 - s * m23);
        m34 = (float) (c * m34 - s * m24);
        m21 = (float) f21;
        m22 = (float) f22;
        m23 = (float) f23;
        m24 = (float) f24;
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
     * <table border="1">
     *   <tr>
     *     <td>1</th>
     *     <td>0</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>cos&theta;</th>
     *     <td>sin&theta;</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>-sin&theta;</th>
     *     <td>cos&theta;</th>
     *   </tr>
     * </table>
     *
     * @param angle the rotation angle in radians.
     */
    public void postRotateX(double angle) {
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f13 = c * m13 + s * m12;
        final double f23 = c * m23 + s * m22;
        final double f33 = c * m33 + s * m32;
        final double f43 = c * m43 + s * m42;
        m12 = (float) (c * m12 - s * m13);
        m22 = (float) (c * m22 - s * m23);
        m32 = (float) (c * m32 - s * m33);
        m42 = (float) (c * m42 - s * m43);
        m13 = (float) f13;
        m23 = (float) f23;
        m33 = (float) f33;
        m43 = (float) f43;
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
     * <table border="1">
     *   <tr>
     *     <td>cos&theta;</th>
     *     <td>0</th>
     *     <td>-sin&theta;</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>1</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>sin&theta;</th>
     *     <td>0</th>
     *     <td>cos&theta;</th>
     *   </tr>
     * </table>
     *
     * @param angle the rotation angle in radians.
     */
    public void preRotateY(double angle) {
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f11 = c * m11 - s * m31;
        final double f12 = c * m12 - s * m32;
        final double f13 = c * m13 - s * m33;
        final double f14 = c * m14 - s * m34;
        m31 = (float) (s * m11 + c * m31);
        m32 = (float) (s * m12 + c * m32);
        m33 = (float) (s * m13 + c * m33);
        m34 = (float) (s * m14 + c * m34);
        m11 = (float) f11;
        m12 = (float) f12;
        m13 = (float) f13;
        m14 = (float) f14;
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
     * <table border="1">
     *   <tr>
     *     <td>cos&theta;</th>
     *     <td>0</th>
     *     <td>-sin&theta;</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>1</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>sin&theta;</th>
     *     <td>0</th>
     *     <td>cos&theta;</th>
     *   </tr>
     * </table>
     *
     * @param angle the rotation angle in radians.
     */
    public void postRotateY(double angle) {
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f13 = c * m13 - s * m11;
        final double f23 = c * m23 - s * m21;
        final double f33 = c * m33 - s * m31;
        final double f43 = c * m43 - s * m41;
        m11 = (float) (s * m13 + c * m11);
        m21 = (float) (s * m23 + c * m21);
        m31 = (float) (s * m33 + c * m31);
        m41 = (float) (s * m43 + c * m41);
        m13 = (float) f13;
        m23 = (float) f23;
        m33 = (float) f33;
        m43 = (float) f43;
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
     * <table border="1">
     *   <tr>
     *     <td>cos&theta;</th>
     *     <td>sin&theta;</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>-sin&theta;</th>
     *     <td>cos&theta;</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>0</th>
     *     <td>1</th>
     *   </tr>
     * </table>
     *
     * @param angle the rotation angle in radians.
     */
    public void preRotateZ(double angle) {
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f11 = c * m11 + s * m21;
        final double f12 = c * m12 + s * m22;
        final double f13 = c * m13 + s * m23;
        final double f14 = c * m14 + s * m24;
        m21 = (float) (c * m21 - s * m11);
        m22 = (float) (c * m22 - s * m12);
        m23 = (float) (c * m23 - s * m13);
        m24 = (float) (c * m24 - s * m14);
        m11 = (float) f11;
        m12 = (float) f12;
        m13 = (float) f13;
        m14 = (float) f14;
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
     * <table border="1">
     *   <tr>
     *     <td>cos&theta;</th>
     *     <td>sin&theta;</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>-sin&theta;</th>
     *     <td>cos&theta;</th>
     *     <td>0</th>
     *   </tr>
     *   <tr>
     *     <td>0</th>
     *     <td>0</th>
     *     <td>1</th>
     *   </tr>
     * </table>
     *
     * @param angle the rotation angle in radians.
     */
    public void postRotateZ(double angle) {
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f12 = c * m12 + s * m11;
        final double f22 = c * m22 + s * m21;
        final double f32 = c * m32 + s * m31;
        final double f42 = c * m42 + s * m41;
        m11 = (float) (c * m11 - s * m12);
        m21 = (float) (c * m21 - s * m22);
        m31 = (float) (c * m31 - s * m32);
        m41 = (float) (c * m41 - s * m42);
        m12 = (float) f12;
        m22 = (float) f22;
        m32 = (float) f32;
        m42 = (float) f42;
    }

    /**
     * Rotates this matrix from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order and using chained rotation per axis:
     * <ul>
     *  <li>x - pitch - {@link #preRotateX(double)}</li>
     *  <li>y - yaw   - {@link #preRotateY(double)}</li>
     *  <li>z - roll  - {@link #preRotateZ(double)}</li>
     * </ul>
     * </p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to pre-multiplying by three rotation matrices.
     *
     * @param angleX the Euler pitch angle in radians. (rotation about the X axis)
     * @param angleY the Euler yaw angle in radians. (rotation about the Y axis)
     * @param angleZ the Euler roll angle in radians. (rotation about the Z axis)
     * @see #preRotateY(double)
     * @see #preRotateZ(double)
     * @see #preRotateX(double)
     */
    public void preRotate(double angleX, double angleY, double angleZ) {
        // same as using Quaternion, 48 multiplications
        preRotateX(angleX);
        preRotateY(angleY);
        preRotateZ(angleZ);
    }

    /**
     * Post-rotates this matrix from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order and using chained rotation per axis:
     * <ul>
     *  <li>x - pitch - {@link #postRotateX(double)}</li>
     *  <li>y - yaw   - {@link #postRotateY(double)}</li>
     *  <li>z - roll  - {@link #postRotateZ(double)}</li>
     * </ul>
     * </p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to post-multiplying by three rotation matrices.
     *
     * @param angleX the Euler pitch angle in radians. (rotation about the X axis)
     * @param angleY the Euler yaw angle in radians. (rotation about the Y axis)
     * @param angleZ the Euler roll angle in radians. (rotation about the Z axis)
     * @see #postRotateX(double)
     * @see #postRotateY(double)
     * @see #postRotateZ(double)
     */
    public void postRotate(double angleX, double angleY, double angleZ) {
        // same as using Quaternion, 48 multiplications
        postRotateX(angleX);
        postRotateY(angleY);
        postRotateZ(angleZ);
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
     * @param axis  the rotation axis
     * @param angle rotation angle in radians
     * @see #preRotateY(double)
     * @see #preRotateZ(double)
     * @see #preRotateX(double)
     */
    public void preRotate(@Nonnull Vector3 axis, float angle) {
        preRotate(axis.x, axis.y, axis.z, angle);
    }

    /**
     * Rotates this matrix about an arbitrary axis with the given angle in radians.
     * The axis described by the three components must be normalized. If it is
     * known that the rotation axis is X, Y or Z, use axis-specified methods instead.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to pre-multiplying by a quaternion.
     *
     * @param x     x-coordinate of rotation axis
     * @param y     y-coordinate of rotation axis
     * @param z     z-coordinate of rotation axis
     * @param angle rotation angle in radians
     * @see #preRotateX(double)
     * @see #preRotateY(double)
     * @see #preRotateZ(double)
     */
    public void preRotate(double x, double y, double z, double angle) {
        if (angle == 0) {
            return;
        }
        // 52 multiplications, the fastest path
        angle *= 0.5;
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        x *= s;
        y *= s;
        z *= s;
        // we assume the axis is normalized
        final double xs = 2.0 * x;
        final double ys = 2.0 * y;
        final double zs = 2.0 * z;

        final double xx = x * xs;
        final double xy = x * ys;
        final double xz = x * zs;
        final double xw = xs * c;
        final double yy = y * ys;
        final double yz = y * zs;
        final double yw = ys * c;
        final double zz = z * zs;
        final double zw = zs * c;

        x = 1.0 - (yy + zz);
        y = xy + zw;
        z = xz - yw;
        final double f11 = x * m11 + y * m21 + z * m31;
        final double f12 = x * m12 + y * m22 + z * m32;
        final double f13 = x * m13 + y * m23 + z * m33;
        final double f14 = x * m14 + y * m24 + z * m34;

        x = xy - zw;
        y = 1.0 - (xx + zz);
        z = yz + xw;
        final double f21 = x * m11 + y * m21 + z * m31;
        final double f22 = x * m12 + y * m22 + z * m32;
        final double f23 = x * m13 + y * m23 + z * m33;
        final double f24 = x * m14 + y * m24 + z * m34;

        x = xz + yw;
        y = yz - xw;
        z = 1.0 - (xx + yy);
        final double f31 = x * m11 + y * m21 + z * m31;
        final double f32 = x * m12 + y * m22 + z * m32;
        final double f33 = x * m13 + y * m23 + z * m33;
        final double f34 = x * m14 + y * m24 + z * m34;

        m11 = (float) f11;
        m12 = (float) f12;
        m13 = (float) f13;
        m14 = (float) f14;
        m21 = (float) f21;
        m22 = (float) f22;
        m23 = (float) f23;
        m24 = (float) f24;
        m31 = (float) f31;
        m32 = (float) f32;
        m33 = (float) f33;
        m34 = (float) f34;
    }

    /**
     * Rotate this matrix by the given quaternion's rotation matrix.
     * (quat * this)
     *
     * @param q the quaternion to rotate by.
     */
    public void preRotate(@Nonnull Quaternion q) {
        final float sq = q.lengthSq();
        if (sq < 1.0e-6f) {
            return;
        }
        // normalize first
        final float is;
        if (MathUtil.isApproxEqual(sq, 1.0f)) {
            is = 2.0f;
        } else {
            is = 2.0f / sq;
        }
        float xs = is * q.x;
        float ys = is * q.y;
        float zs = is * q.z;

        final float xx = q.x * xs;
        final float xy = q.x * ys;
        final float xz = q.x * zs;
        final float xw = xs * q.w;
        final float yy = q.y * ys;
        final float yz = q.y * zs;
        final float yw = ys * q.w;
        final float zz = q.z * zs;
        final float zw = zs * q.w;

        xs = 1.0f - (yy + zz);
        ys = xy + zw;
        zs = xz - yw;
        final float f11 = xs * m11 + ys * m21 + zs * m31;
        final float f12 = xs * m12 + ys * m22 + zs * m32;
        final float f13 = xs * m13 + ys * m23 + zs * m33;
        final float f14 = xs * m14 + ys * m24 + zs * m34;

        xs = xy - zw;
        ys = 1.0f - (xx + zz);
        zs = yz + xw;
        final float f21 = xs * m11 + ys * m21 + zs * m31;
        final float f22 = xs * m12 + ys * m22 + zs * m32;
        final float f23 = xs * m13 + ys * m23 + zs * m33;
        final float f24 = xs * m14 + ys * m24 + zs * m34;

        xs = xz + yw;
        ys = yz - xw;
        zs = 1.0f - (xx + yy);
        final float f31 = xs * m11 + ys * m21 + zs * m31;
        final float f32 = xs * m12 + ys * m22 + zs * m32;
        final float f33 = xs * m13 + ys * m23 + zs * m33;
        final float f34 = xs * m14 + ys * m24 + zs * m34;

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
     * Post-rotates this matrix about an arbitrary axis with the given angle in radians.
     * The axis described by the three components must be normalized. If it is
     * known that the rotation axis is X, Y or Z, use axis-specified methods instead.
     * <p>
     * When used with a right-handed coordinate system, the produced rotation
     * will rotate a vector counter-clockwise around the rotation axis, when
     * viewing along the negative axis direction towards the origin. When
     * used with a left-handed coordinate system, the rotation is clockwise.
     * <p>
     * This is equivalent to post-multiplying by a quaternion.
     *
     * @param x     x-coordinate of rotation axis
     * @param y     y-coordinate of rotation axis
     * @param z     z-coordinate of rotation axis
     * @param angle rotation angle in radians
     * @see #postRotateX(double)
     * @see #postRotateY(double)
     * @see #postRotateZ(double)
     */
    public void postRotate(double x, double y, double z, double angle) {
        if (angle == 0) {
            return;
        }
        // 52 multiplications, the fastest path
        angle *= 0.5;
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        x *= s;
        y *= s;
        z *= s;
        // we assume the axis is normalized
        final double xs = 2.0 * x;
        final double ys = 2.0 * y;
        final double zs = 2.0 * z;

        final double xx = x * xs;
        final double xy = x * ys;
        final double xz = x * zs;
        final double xw = xs * c;
        final double yy = y * ys;
        final double yz = y * zs;
        final double yw = ys * c;
        final double zz = z * zs;
        final double zw = zs * c;

        final double f11 = 1.0 - (yy + zz);
        final double f12 = xy + zw;
        final double f13 = xz - yw;

        final double f21 = xy - zw;
        final double f22 = 1.0 - (xx + zz);
        final double f23 = yz + xw;

        final double f31 = xz + yw;
        final double f32 = yz - xw;
        final double f33 = 1.0 - (xx + yy);

        x = m11 * f11 + m12 * f21 + m13 * f31;
        y = m11 * f12 + m12 * f22 + m13 * f32;
        z = m11 * f13 + m12 * f23 + m13 * f33;
        m11 = (float) x;
        m12 = (float) y;
        m13 = (float) z;
        x = m21 * f11 + m22 * f21 + m23 * f31;
        y = m21 * f12 + m22 * f22 + m23 * f32;
        z = m21 * f13 + m22 * f23 + m23 * f33;
        m21 = (float) x;
        m22 = (float) y;
        m23 = (float) z;
        x = m31 * f11 + m32 * f21 + m33 * f31;
        y = m31 * f12 + m32 * f22 + m33 * f32;
        z = m31 * f13 + m32 * f23 + m33 * f33;
        m31 = (float) x;
        m32 = (float) y;
        m33 = (float) z;
        x = m41 * f11 + m42 * f21 + m43 * f31;
        y = m41 * f12 + m42 * f22 + m43 * f32;
        z = m41 * f13 + m42 * f23 + m43 * f33;
        m41 = (float) x;
        m42 = (float) y;
        m43 = (float) z;
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
     * Transform a four-dimensional row vector by pre-multiplication
     * (vector * matrix). This is equivalent to (matrix * vector) in GLSL.
     *
     * @param vec the vector to transform
     */
    public void preTransform(@Nonnull Vector4 vec) {
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
     * Transform a four-dimensional column vector by post-multiplication
     * (matrix * vector). This is equivalent to (vector * matrix) in GLSL.
     *
     * @param vec the vector to transform
     */
    public void postTransform(@Nonnull Vector4 vec) {
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
     * Transform a three-dimensional row vector by pre-multiplication
     * (vec3 * this, w-component is considered as 1).
     * This should be used with position vectors.
     *
     * @param vec the vector to transform
     */
    public void preTransform(@Nonnull Vector3 vec) {
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
     * Transform a three-dimensional column vector by post-multiplication
     * (this * vec3, w-component is considered as 1).
     * This should be used with normal vectors.
     *
     * @param vec the vector to transform
     */
    public void postTransform(@Nonnull Vector3 vec) {
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
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRect(@Nonnull Rect2f r) {
        mapRect(r.mLeft, r.mTop, r.mRight, r.mBottom, r);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRect(@Nonnull Rect2fc r, @Nonnull Rect2f dest) {
        mapRect(r.left(), r.top(), r.right(), r.bottom(), dest);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRect(float left, float top, float right, float bottom, @Nonnull Rect2f dest) {
        float x1 = m11 * left + m21 * top + m41;
        float y1 = m12 * left + m22 * top + m42;
        float x2 = m11 * right + m21 * top + m41;
        float y2 = m12 * right + m22 * top + m42;
        float x3 = m11 * left + m21 * bottom + m41;
        float y3 = m12 * left + m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if (hasPerspective()) {
            // project
            float w;
            w = 1.0f / (m14 * left + m24 * top + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dest.mLeft = MathUtil.min(x1, x2, x3, x4);
        dest.mTop = MathUtil.min(y1, y2, y3, y4);
        dest.mRight = MathUtil.max(x1, x2, x3, x4);
        dest.mBottom = MathUtil.max(y1, y2, y3, y4);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRect(@Nonnull Rect2fc r, @Nonnull Rect2i dest) {
        mapRect(r.left(), r.top(), r.right(), r.bottom(), dest);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRect(@Nonnull Rect2ic r, @Nonnull Rect2i dest) {
        mapRect(r.left(), r.top(), r.right(), r.bottom(), dest);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRect(float left, float top, float right, float bottom, @Nonnull Rect2i dest) {
        float x1 = m11 * left + m21 * top + m41;
        float y1 = m12 * left + m22 * top + m42;
        float x2 = m11 * right + m21 * top + m41;
        float y2 = m12 * right + m22 * top + m42;
        float x3 = m11 * left + m21 * bottom + m41;
        float y3 = m12 * left + m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if (hasPerspective()) {
            // project
            float w;
            w = 1.0f / (m14 * left + m24 * top + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dest.mLeft = Math.round(MathUtil.min(x1, x2, x3, x4));
        dest.mTop = Math.round(MathUtil.min(y1, y2, y3, y4));
        dest.mRight = Math.round(MathUtil.max(x1, x2, x3, x4));
        dest.mBottom = Math.round(MathUtil.max(y1, y2, y3, y4));
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRectOut(@Nonnull Rect2ic r, @Nonnull Rect2i dest) {
        mapRectOut(r.left(), r.top(), r.right(), r.bottom(), dest);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRectOut(@Nonnull Rect2fc r, @Nonnull Rect2i dest) {
        mapRectOut(r.left(), r.top(), r.right(), r.bottom(), dest);
    }

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    public void mapRectOut(float left, float top, float right, float bottom, @Nonnull Rect2i dest) {
        float x1 = m11 * left + m21 * top + m41;
        float y1 = m12 * left + m22 * top + m42;
        float x2 = m11 * right + m21 * top + m41;
        float y2 = m12 * right + m22 * top + m42;
        float x3 = m11 * left + m21 * bottom + m41;
        float y3 = m12 * left + m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if (hasPerspective()) {
            // project
            float w;
            w = 1.0f / (m14 * left + m24 * top + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dest.mLeft = (int) Math.floor(MathUtil.min(x1, x2, x3, x4));
        dest.mTop = (int) Math.floor(MathUtil.min(y1, y2, y3, y4));
        dest.mRight = (int) Math.ceil(MathUtil.max(x1, x2, x3, x4));
        dest.mBottom = (int) Math.ceil(MathUtil.max(y1, y2, y3, y4));
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

    public float mapPointX(float x, float y) {
        if (isAffine()) {
            return m11 * x + m21 * y + m41;
        } else {
            final float f = m11 * x + m21 * y + m41;
            float w = 1.0f / (m14 * x + m24 * y + m44);
            return f * w;
        }
    }

    public float mapPointY(float x, float y) {
        if (isAffine()) {
            return m12 * x + m22 * y + m42;
        } else {
            final float f = m12 * x + m22 * y + m42;
            float w = 1.0f / (m14 * x + m24 * y + m44);
            return f * w;
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
        return MathUtil.isApproxZero(m14, m24, m34) && MathUtil.isApproxEqual(m44, 1.0f);
    }

    /**
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scale, translate, or both.
     */
    public boolean isScaleTranslate() {
        return isAffine() &&
                MathUtil.isApproxZero(m12, m13, m21) &&
                MathUtil.isApproxZero(m23, m31, m32);
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
                (MathUtil.isApproxZero(m11) && MathUtil.isApproxZero(m22) && !MathUtil.isApproxZero(m12) && !MathUtil.isApproxZero(m21)) ||
                        (MathUtil.isApproxZero(m12) && MathUtil.isApproxZero(m21) && !MathUtil.isApproxZero(m11) && !MathUtil.isApproxZero(m22))
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
        return !(MathUtil.isApproxZero(m41, m42, m43) && MathUtil.isApproxEqual(m44, 1.0f));
    }

    /**
     * Returns whether this matrix is approximately equivalent to an identity matrix.
     *
     * @return {@code true} if this matrix is identity.
     */
    public boolean isIdentity() {
        return MathUtil.isApproxZero(m12, m13, m14) &&
                MathUtil.isApproxZero(m21, m23, m24) &&
                MathUtil.isApproxZero(m31, m32, m34) &&
                MathUtil.isApproxZero(m41, m42, m43) &&
                MathUtil.isApproxEqual(m11, m22, m33, m44, 1.0f);
    }

    // compute SVD of 2x2 matrix
    private static float computeMinScale(double m11, double m21, double m12, double m22) {
        double s1 = m11 * m11 + m21 * m21 + m12 * m12 + m22 * m22;

        double e = m11 * m11 + m21 * m21 - m12 * m12 - m22 * m22;
        double f = m11 * m12 + m21 * m22;
        double s2 = Math.sqrt(e * e + 4 * f * f);

        // s2 >= 0, so (s1 - s2) <= (s1 + s2) so this always returns min.
        return (float) Math.sqrt(0.5 * (s1 - s2));
    }

    private float computeMinScale(double px, double py) {
        final double x = m11 * px + m21 * py + m41;
        final double y = m12 * px + m22 * py + m42;
        final double z = m13 * px + m23 * py + m43;
        final double w = m14 * px + m24 * py + m44;

        final float dxdu = m11;
        final float dxdv = m21;
        final float dydu = m12;
        final float dydv = m22;
        final float dwdu = m14;
        final float dwdv = m24;

        double invW2 = 1.0 / (w * w);
        // non-persp has invW2 = 1, devP.w = 1, dwdu = 0, dwdv = 0
        double dfdu = (w * dxdu - x * dwdu) * invW2; // non-persp -> dxdu -> m00
        double dfdv = (w * dxdv - x * dwdv) * invW2; // non-persp -> dxdv -> m01
        double dgdu = (w * dydu - y * dwdu) * invW2; // non-persp -> dydu -> m10
        double dgdv = (w * dydv - y * dwdv) * invW2; // non-persp -> dydv -> m11

        return computeMinScale(dfdu, dfdv, dgdu, dgdv);
    }

    public float localAARadius(Rect2fc bounds) {
        return localAARadius(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    public float localAARadius(float left, float top, float right, float bottom) {
        float min;
        if (isAffine()) {
            min = computeMinScale(m11, m21, m12, m22);
        } else {
            // Calculate the minimum scale factor over the 4 corners of the bounding box
            float tl = computeMinScale(left, top);
            float tr = computeMinScale(right, top);
            float br = computeMinScale(right, bottom);
            float bl = computeMinScale(left, bottom);
            min = MathUtil.min(tl, tr, br, bl);
        }

        // Moving 1 from 'p' before transforming will move at least 'min' and at most 'max' from
        // the transformed point. Thus moving between [1/max, 1/min] pre-transformation means post
        // transformation moves between [1,max/min] so using 1/min as the local AA radius ensures that
        // the post-transformed point is at least 1px away from the original.
        float aaRadius = 1.0f / min;
        if (Float.isFinite(aaRadius)) { // check Inf and NaN
            return aaRadius;
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the third row and column are discarded.
     * <pre>{@code
     * [ a b x c ]      [ a b c ]
     * [ d e x f ]  ->  [ d e f ]
     * [ x x x x ]      [ g h i ]
     * [ g h x i ]
     * }</pre>
     */
    public void toMatrix(@Nonnull Matrix dest) {
        dest.set(
                m11, m12, m14,
                m21, m22, m24,
                m41, m42, m44
        );
    }

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the third row and column are discarded.
     * <pre>{@code
     * [ a b x c ]      [ a b c ]
     * [ d e x f ]  ->  [ d e f ]
     * [ x x x x ]      [ g h i ]
     * [ g h x i ]
     * }</pre>
     */
    @Nonnull
    public Matrix toMatrix() {
        return new Matrix(
                m11, m12, m14,
                m21, m22, m24,
                m41, m42, m44
        );
    }

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the fourth row and column are discarded.
     * <pre>{@code
     * [ a b c x ]      [ a b c ]
     * [ d e f x ]  ->  [ d e f ]
     * [ g h i x ]      [ g h i ]
     * [ x x x x ]
     * }</pre>
     */
    public void toMatrix3(@Nonnull Matrix3 dest) {
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = m13;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = m23;
        dest.m31 = m31;
        dest.m32 = m32;
        dest.m33 = m33;
    }

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the fourth row and column are discarded.
     * <pre>{@code
     * [ a b c x ]      [ a b c ]
     * [ d e f x ]  ->  [ d e f ]
     * [ g h i x ]      [ g h i ]
     * [ x x x x ]
     * }</pre>
     */
    @Nonnull
    public Matrix3 toMatrix3() {
        Matrix3 m = new Matrix3();
        toMatrix3(m);
        return m;
    }

    /**
     * Returns whether this matrix is approximately equal to given matrix.
     *
     * @param m the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    public boolean isApproxEqual(@Nonnull Matrix4 m) {
        return MathUtil.isApproxEqual(m11, m.m11) &&
                MathUtil.isApproxEqual(m12, m.m12) &&
                MathUtil.isApproxEqual(m13, m.m13) &&
                MathUtil.isApproxEqual(m14, m.m14) &&
                MathUtil.isApproxEqual(m21, m.m21) &&
                MathUtil.isApproxEqual(m22, m.m22) &&
                MathUtil.isApproxEqual(m23, m.m23) &&
                MathUtil.isApproxEqual(m24, m.m24) &&
                MathUtil.isApproxEqual(m31, m.m31) &&
                MathUtil.isApproxEqual(m32, m.m32) &&
                MathUtil.isApproxEqual(m33, m.m33) &&
                MathUtil.isApproxEqual(m34, m.m34) &&
                MathUtil.isApproxEqual(m41, m.m41) &&
                MathUtil.isApproxEqual(m42, m.m42) &&
                MathUtil.isApproxEqual(m43, m.m43) &&
                MathUtil.isApproxEqual(m44, m.m44);
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(m11);
        result = 31 * result + Float.hashCode(m12);
        result = 31 * result + Float.hashCode(m13);
        result = 31 * result + Float.hashCode(m14);
        result = 31 * result + Float.hashCode(m21);
        result = 31 * result + Float.hashCode(m22);
        result = 31 * result + Float.hashCode(m23);
        result = 31 * result + Float.hashCode(m24);
        result = 31 * result + Float.hashCode(m31);
        result = 31 * result + Float.hashCode(m32);
        result = 31 * result + Float.hashCode(m33);
        result = 31 * result + Float.hashCode(m34);
        result = 31 * result + Float.hashCode(m41);
        result = 31 * result + Float.hashCode(m42);
        result = 31 * result + Float.hashCode(m43);
        result = 31 * result + Float.hashCode(m44);
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
        if (!(o instanceof Matrix4 m)) {
            return false;
        }
        return m11 == m.m11 &&
                m12 == m.m12 &&
                m13 == m.m13 &&
                m14 == m.m14 &&
                m21 == m.m21 &&
                m22 == m.m22 &&
                m23 == m.m23 &&
                m24 == m.m24 &&
                m31 == m.m31 &&
                m32 == m.m32 &&
                m33 == m.m33 &&
                m34 == m.m34 &&
                m41 == m.m41 &&
                m42 == m.m42 &&
                m43 == m.m43 &&
                m44 == m.m44;
    }

    @Override
    public String toString() {
        return String.format("""
                        Matrix4:
                        %10.6f %10.6f %10.6f %10.6f
                        %10.6f %10.6f %10.6f %10.6f
                        %10.6f %10.6f %10.6f %10.6f
                        %10.6f %10.6f %10.6f %10.6f""",
                m11, m12, m13, m14,
                m21, m22, m23, m24,
                m31, m32, m33, m34,
                m41, m42, m43, m44);
    }

    /**
     * @return a copy of this matrix
     */
    @Nonnull
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
