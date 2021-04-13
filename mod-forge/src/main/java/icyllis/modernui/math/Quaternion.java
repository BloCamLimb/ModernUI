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
 * Represents a Quaternion.
 */
@SuppressWarnings("unused")
public class Quaternion implements Cloneable {

    private float x;
    private float y;
    private float z;
    private float w;

    private Quaternion() {
    }

    /**
     * Create a copy of {@code quat} if it's not null, or a identity quaternion
     * otherwise.
     *
     * @param quat the quaternion to copy from
     * @return a copy of the quaternion
     */
    @Nonnull
    public static Quaternion copy(@Nullable Quaternion quat) {
        return quat == null ? identity() : quat.clone();
    }

    /**
     * Create a new identity quaternion.
     *
     * @return an identity quaternion
     */
    @Nonnull
    public static Quaternion identity() {
        Quaternion quat = new Quaternion();
        quat.w = 1.0f;
        return quat;
    }

    /**
     * Create a quaternion from the given Euler rotation angles in radians.
     *
     * @param bankX     the Euler pitch angle in radians. (rotation about the X axis)
     * @param headingY  the Euler yaw angle in radians. (rotation about the Y axis)
     * @param attitudeZ the Euler roll angle in radians. (rotation about the Z axis)
     * @return the resulting quaternion
     */
    @Nonnull
    public static Quaternion fromEuler(float bankX, float headingY, float attitudeZ) {
        Quaternion quat = new Quaternion();
        float angle = bankX * 0.5f;
        final float sinBankX = MathUtil.sin(angle);
        final float cosBankX = MathUtil.cos(angle);
        angle = headingY * 0.5f;
        final float sinHeadingY = MathUtil.sin(angle);
        final float cosHeadingY = MathUtil.cos(angle);
        angle = attitudeZ * 0.5f;
        final float sinAttitudeZ = MathUtil.sin(angle);
        final float cosAttitudeZ = MathUtil.cos(angle);

        // variables used to reduce multiplication calls.
        final float cosHeading_cosAttitude = cosHeadingY * cosAttitudeZ;
        final float sinHeading_sinAttitude = sinHeadingY * sinAttitudeZ;
        final float cosHeading_sinAttitude = cosHeadingY * sinAttitudeZ;
        final float sinHeading_cosAttitude = sinHeadingY * cosAttitudeZ;

        quat.w = cosHeading_cosAttitude * cosBankX - sinHeading_sinAttitude * sinBankX;
        quat.x = cosHeading_cosAttitude * sinBankX + sinHeading_sinAttitude * cosBankX;
        quat.y = sinHeading_cosAttitude * cosBankX + cosHeading_sinAttitude * sinBankX;
        quat.z = cosHeading_sinAttitude * cosBankX - sinHeading_cosAttitude * sinBankX;
        return quat;
    }

    /**
     * Create a quaternion from the given axis and angle. The axis must be
     * a normalized vector.
     *
     * @param axisX x-coordinate of rotation axis
     * @param axisY y-coordinate of rotation axis
     * @param axisZ z-coordinate of rotation axis
     * @param angle rotation angle in radians
     * @return the resulting quaternion
     */
    @Nonnull
    public static Quaternion fromAxisAngle(float axisX, float axisY, float axisZ, float angle) {
        Quaternion quat = new Quaternion();
        final float halfAngle = 0.5f * angle;
        final float sin = MathUtil.sin(halfAngle);
        quat.x = axisX * sin;
        quat.y = axisY * sin;
        quat.z = axisZ * sin;
        quat.w = MathUtil.cos(halfAngle);
        return quat;
    }

    /**
     * Set all values of this quaternion using the given components.
     */
    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Set all values of this quaternion using the given quaternion.
     */
    public void set(@Nonnull Quaternion quat) {
        x = quat.x;
        y = quat.y;
        z = quat.z;
        w = quat.w;
    }

    /**
     * Multiply this quaternion by a factor.
     *
     * @param s the factor to multiply.
     */
    public void multiply(float s) {
        x *= s;
        y *= s;
        z *= s;
        w *= s;
    }

    /**
     * Multiply this quaternion by some other quaternion.
     *
     * @param other the quaternion to multiply with
     */
    public void multiply(@Nonnull Quaternion other) {
        set(w * other.x + x * other.w + y * other.z - z * other.y,
                w * other.y - x * other.z + y * other.w + z * other.x,
                w * other.z + x * other.y - y * other.x + z * other.w,
                w * other.w - x * other.x - y * other.y - z * other.z);
    }

    /**
     * Calculate the magnitude of this quaternion, namely the 2-norm
     * (euclidean norm) or the length.
     *
     * @return the magnitude of this quaternion
     */
    public float length() {
        return MathUtil.sqrt(w * w + x * x + y * y + z * z);
    }

    public float lengthSquared() {
        return w * w + x * x + y * y + z * z;
    }

    /**
     * Calculate the inverse of this quaternion. If rotational, it will produce
     * a the inverse rotation.
     */
    public void inverse() {
        final float sq = lengthSquared();
        if (MathUtil.approxEqual(sq, 1.0f))
            conjugate();
        else {
            final float invSq = 1.0f / sq;
            w *= invSq;
            x = -x * invSq;
            y = -y * invSq;
            z = -z * invSq;
        }
    }

    /**
     * Normalize this quaternion to unit length.
     */
    public void normalize() {
        final float sq = lengthSquared();
        if (sq < 1.0e-6f) {
            setIdentity();
        } else {
            final float invNorm = MathUtil.fastInvSqrt(sq);
            x *= invNorm;
            y *= invNorm;
            z *= invNorm;
            w *= invNorm;
        }
    }

    /**
     * Calculate whether this quaternion is approximately equivalent to a identity quaternion.
     *
     * @return {@code true} if this quaternion is identity.
     */
    public boolean isIdentity() {
        return x == 0.0f && y == 0.0f && z == 0.0f && w == 1.0f;
    }

    /***
     * Set this quaternion to the identity quaternion.
     */
    public void setIdentity() {
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
        w = 1.0f;
    }

    /**
     * Conjugate this quaternion <code>[-x, -y, -z, w]</code>.
     */
    public void conjugate() {
        x = -x;
        y = -y;
        z = -z;
    }

    /**
     * Transform this quaternion to a normalized 3x3 column matrix representing
     * the rotation.
     *
     * @return the resulting matrix
     */
    @Nonnull
    public Matrix3 toMatrix3() {
        final float sq = lengthSquared();
        if (sq < 1.0e-6f) {
            return Matrix3.identity();
        }
        final float is;
        if (MathUtil.approxEqual(sq, 1.0f)) {
            is = 2.0f;
        } else {
            is = 2.0f / sq;
        }
        Matrix3 mat = new Matrix3();
        final float xs = is * x;
        final float ys = is * y;
        final float zs = is * z;

        final float xx = x * xs;
        final float xy = x * ys;
        final float xz = x * zs;
        final float xw = xs * w;
        final float yy = y * ys;
        final float yz = y * zs;
        final float yw = ys * w;
        final float zz = z * zs;
        final float zw = zs * w;

        mat.m11 = 1.0f - (yy + zz);
        mat.m22 = 1.0f - (xx + zz);
        mat.m33 = 1.0f - (xx + yy);

        mat.m21 = xy - zw;
        mat.m31 = xz + yw;
        mat.m12 = xy + zw;
        mat.m32 = yz - xw;
        mat.m13 = xz - yw;
        mat.m23 = yz + xw;
        return mat;
    }

    /**
     * Transform this quaternion to a normalized 4x4 column matrix representing
     * the rotation.
     *
     * @return the resulting matrix
     */
    @Nonnull
    public Matrix4 toMatrix4() {
        final float sq = lengthSquared();
        if (sq < 1.0e-6f) {
            return Matrix4.identity();
        }
        final float is;
        if (MathUtil.approxEqual(sq, 1.0f)) {
            is = 2.0f;
        } else {
            is = 2.0f / sq;
        }
        Matrix4 mat = new Matrix4();
        final float xs = is * x;
        final float ys = is * y;
        final float zs = is * z;

        final float xx = x * xs;
        final float xy = x * ys;
        final float xz = x * zs;
        final float xw = xs * w;
        final float yy = y * ys;
        final float yz = y * zs;
        final float yw = ys * w;
        final float zz = z * zs;
        final float zw = zs * w;

        mat.m11 = 1.0f - (yy + zz);
        mat.m22 = 1.0f - (xx + zz);
        mat.m33 = 1.0f - (xx + yy);

        mat.m21 = xy - zw;
        mat.m31 = xz + yw;
        mat.m12 = xy + zw;
        mat.m32 = yz - xw;
        mat.m13 = xz - yw;
        mat.m23 = yz + xw;

        mat.m44 = 1.0f;
        return mat;
    }

    /**
     * Transform this quaternion to a normalized 3x3 column matrix representing
     * the rotation. If recycle matrix is null, a new matrix will be returned.
     *
     * @param recycle a matrix for storing result if you want to recycle it
     * @return the resulting matrix
     */
    @Nonnull
    public Matrix3 toMatrix(@Nullable Matrix3 recycle) {
        if (recycle == null)
            return toMatrix3();
        final float sq = lengthSquared();
        if (sq < 1.0e-6f) {
            recycle.setIdentity();
            return recycle;
        }
        final float inv;
        if (MathUtil.approxEqual(sq, 1.0f)) {
            inv = 2.0f;
        } else {
            inv = 2.0f / sq;
        }
        final float xs = inv * x;
        final float ys = inv * y;
        final float zs = inv * z;

        final float xx = x * xs;
        final float xy = x * ys;
        final float xz = x * zs;
        final float xw = xs * w;
        final float yy = y * ys;
        final float yz = y * zs;
        final float yw = ys * w;
        final float zz = z * zs;
        final float zw = zs * w;

        recycle.m11 = 1.0f - (yy + zz);
        recycle.m21 = xy - zw;
        recycle.m31 = xz + yw;

        recycle.m12 = xy + zw;
        recycle.m22 = 1.0f - (xx + zz);
        recycle.m32 = yz - xw;

        recycle.m13 = xz - yw;
        recycle.m23 = yz + xw;
        recycle.m33 = 1.0f - (xx + yy);
        return recycle;
    }

    /**
     * Transform this quaternion to a normalized 4x4 column matrix representing
     * the rotation. If recycle matrix is null, a new matrix will be returned.
     *
     * @param recycle a matrix for storing result if you want to recycle it
     * @return the resulting matrix
     */
    @Nonnull
    public Matrix4 toMatrix(@Nullable Matrix4 recycle) {
        if (recycle == null)
            return toMatrix4();
        final float sq = lengthSquared();
        if (sq < 1.0e-6f) {
            recycle.setIdentity();
            return recycle;
        }
        final float inv;
        if (MathUtil.approxEqual(sq, 1.0f)) {
            inv = 2.0f;
        } else {
            inv = 2.0f / sq;
        }
        final float xs = inv * x;
        final float ys = inv * y;
        final float zs = inv * z;

        final float xx = x * xs;
        final float xy = x * ys;
        final float xz = x * zs;
        final float xw = xs * w;
        final float yy = y * ys;
        final float yz = y * zs;
        final float yw = ys * w;
        final float zz = z * zs;
        final float zw = zs * w;

        recycle.m11 = 1.0f - (yy + zz);
        recycle.m21 = xy - zw;
        recycle.m31 = xz + yw;
        recycle.m41 = 0.0f;

        recycle.m12 = xy + zw;
        recycle.m22 = 1.0f - (xx + zz);
        recycle.m32 = yz - xw;
        recycle.m42 = 0.0f;

        recycle.m13 = xz - yw;
        recycle.m23 = yz + xw;
        recycle.m33 = 1.0f - (xx + yy);
        recycle.m43 = 0.0f;

        recycle.m14 = 0.0f;
        recycle.m24 = 0.0f;
        recycle.m34 = 0.0f;
        recycle.m44 = 1.0f;
        return recycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quaternion that = (Quaternion) o;

        if (!MathUtil.exactEqual(that.x, x)) return false;
        if (!MathUtil.exactEqual(that.y, y)) return false;
        if (!MathUtil.exactEqual(that.z, z)) return false;
        return MathUtil.exactEqual(that.w, w);
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        result = 31 * result + (z != +0.0f ? Float.floatToIntBits(z) : 0);
        result = 31 * result + (w != +0.0f ? Float.floatToIntBits(w) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Quaternion{" + "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }

    @Override
    public Quaternion clone() {
        try {
            return (Quaternion) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
