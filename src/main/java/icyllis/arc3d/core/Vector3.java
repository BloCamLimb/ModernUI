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

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a three-dimensional vector.
 */
@SuppressWarnings("unused")
public class Vector3 {

    // coordinate components
    public float x;
    public float y;
    public float z;

    /**
     * Create a zero vector
     */
    public Vector3() {
    }

    /**
     * Create a vector with given components.
     *
     * @param x the x-component of the vector
     * @param y the y-component of the vector
     * @param z the z-component of the vector
     */
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Set all values of this vector using the given components.
     *
     * @param x the x-component to set
     * @param y the y-component to set
     * @param z the z-component to set
     */
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Set all values of this vector using the given vector.
     *
     * @param v the vector to copy from
     */
    public void set(@NonNull Vector3 v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    /**
     * Add each component of a vector to this vector.
     *
     * @param v the vector to add
     */
    public void add(@NonNull Vector3 v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    /**
     * Subtract each component of a vector from this vector.
     *
     * @param v the vector to subtract
     */
    public void subtract(@NonNull Vector3 v) {
        x -= v.x;
        y -= v.y;
        z -= v.z;
    }

    /**
     * Multiply this vector by a factor.
     *
     * @param s the factor to multiply.
     */
    public void multiply(float s) {
        x *= s;
        y *= s;
        z *= s;
    }

    /**
     * Multiply each component of the vector by given multipliers
     *
     * @param mx the x-component multiplier
     * @param my the y-component multiplier
     * @param mz the z-component multiplier
     */
    public void multiply(float mx, float my, float mz) {
        x *= mx;
        y *= my;
        z *= mz;
    }

    /**
     * Multiply each component of the vector by the given vector.
     *
     * @param v the vector to multiply with
     */
    public void multiply(@NonNull Vector3 v) {
        x *= v.x;
        y *= v.y;
        z *= v.z;
    }

    /**
     * Returns the magnitude of this vector, namely the 2-norm
     * (euclidean norm) or the length.
     *
     * @return the magnitude of this quaternion
     */
    public float length() {
        return MathUtil.sqrt(x * x + y * y + z * z);
    }

    public float lengthSq() {
        return x * x + y * y + z * z;
    }

    /**
     * Returns the dot product of this vector with the given x, y and z components.
     */
    public float dot(float x, float y, float z) {
        return this.x * x + this.y * y + this.z * z;
    }

    /**
     * Returns the dot product of this vector with the given vector.
     *
     * @param v the vector to dot product with.
     */
    public float dot(@NonNull Vector3 v) {
        return x * v.x + y * v.y + z * v.z;
    }

    /**
     * Cross product of this vector with the given x, y and z components.
     */
    public void cross(float x, float y, float z) {
        final float f = this.y * z - this.z * y;
        final float g = this.z * x - this.x * z;
        final float h = this.x * y - this.y * x;
        this.x = f;
        this.y = g;
        this.z = h;
    }

    /**
     * Cross product of this vector with the given vector.
     *
     * @param v the vector to cross product with.
     */
    public void cross(@NonNull Vector3 v) {
        if (this == v) {
            // same direction, sin = 0
            setZero();
            return;
        }
        final float x = this.y * v.z - this.z * v.y;
        final float y = this.z * v.x - this.x * v.z;
        final float z = this.x * v.y - this.y * v.x;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Calculate component-wise minimum of this and the given vector.
     */
    public void minComponent(@NonNull Vector3 v) {
        x = Math.min(x, v.x);
        y = Math.min(y, v.y);
        z = Math.min(z, v.z);
    }

    /**
     * Calculate component-wise maximum of this and the given vector.
     */
    public void maxComponent(@NonNull Vector3 v) {
        x = Math.max(x, v.x);
        y = Math.max(y, v.y);
        z = Math.max(z, v.z);
    }

    /**
     * Returns whether this vector is normalized.
     *
     * @return {@code true} if is normalized, {@code false} otherwise
     */
    public boolean isNormalized() {
        return MathUtil.isApproxEqual(lengthSq(), 1.0f);
    }

    /**
     * Normalize this vector to unit length, or <code>[1, 0, 0]</code> if this is zero.
     */
    public void normalize() {
        final float sq = lengthSq();
        if (sq < 1.0e-6f) {
            x = 1.0f;
            y = 0.0f;
            z = 0.0f;
        } else {
            final double invNorm = 1.0f / Math.sqrt(sq);
            x *= invNorm;
            y *= invNorm;
            z *= invNorm;
        }
    }

    /**
     * Set all values of this vector to zero.
     */
    public void setZero() {
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }

    /**
     * Negate this vector <code>[-x, -y, -z]</code>.
     */
    public void negate() {
        x = -x;
        y = -y;
        z = -z;
    }

    /**
     * Returns the sum of all the vector components.
     *
     * @return the sum of all the vector components
     */
    public float sum() {
        return x + y + z;
    }

    /**
     * Returns the product of all the vector components.
     *
     * @return the product of all the vector components
     */
    public float product() {
        return x * y * z;
    }

    /**
     * Calculate an arbitrary unit vector perpendicular to this,
     * this vector must be normalized.
     */
    public void perpendicular() {
        final float l;
        if (Math.abs(x) >= Math.abs(y)) {
            l = 1.0f / MathUtil.sqrt(x * x + z * z);
            float t = x;
            x = -z * l;
            y = 0.0f;
            z = t * l;
        } else {
            l = 1.0f / MathUtil.sqrt(y * y + z * z);
            float t = y;
            x = 0.0f;
            y = z * l;
            z = -t * l;
        }
    }

    /**
     * Calculate the projection of the vector on the given vector.
     *
     * @param v the vector to project on
     */
    public void projection(@NonNull Vector3 v) {
        final float sq = lengthSq();
        if (sq < 1.0e-6f) {
            setZero();
        } else {
            final float c = dot(v) / MathUtil.sqrt(sq);
            x = v.x * c;
            y = v.y * c;
            z = v.z * c;
        }
    }

    /**
     * Create a rotational quaternion clockwise about this axis with given angle in radians.
     * This vector must be normalized.
     *
     * @param angle rotation angle in radians
     * @return a quaternion represents the rotation
     */
    @NonNull
    public Quaternion rotation(float angle) {
        return Quaternion.makeAxisAngle(this, angle);
    }

    /**
     * Transform this vector by a 4x4 transformation matrix.
     *
     * @param mat the matrix to transform from
     */
    public void transform(@NonNull Matrix4 mat) {
        mat.preTransform(this);
    }

    /**
     * Pre-transform this vector by a 4x4 transformation matrix.
     *
     * @param mat the matrix to transform from
     */
    public void preTransform(@NonNull Matrix4 mat) {
        mat.postTransform(this);
    }

    /**
     * Transform this vector by a quaternion rotation.
     *
     * @param q the quaternion to rotate the vector by
     */
    public void transform(@NonNull Quaternion q) {
        // formula quat * vec * quat^-1, 32 multiplications
        // since this vector is 3-dimensional, we can optimize it as follows:
        // vec + 2.0 * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec)
        final float f = q.y * z - q.z * y + q.w * x;
        final float g = q.z * x - q.x * z + q.w * y;
        final float h = q.x * y - q.y * x + q.w * z;
        x += (q.y * h - q.z * g) * 2.0f;
        y += (q.z * f - q.x * h) * 2.0f;
        z += (q.x * g - q.y * f) * 2.0f;
    }

    /**
     * Sort components of this in ascending order.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void sort() {
        float t;
        if (x > y) {
            t = x;
            x = y;
            y = t;
        }
        if (y > z) {
            t = y;
            y = z;
            z = t;
        }
        if (x > y) {
            t = x;
            x = y;
            y = t;
        }
    }

    /**
     * Reverse the order of components. <code>[z,y,x]</code>
     */
    public void reverse() {
        float t = x;
        x = z;
        z = t;
    }

    /**
     * Returns whether this vector is equivalent to given vector.
     *
     * @param v the quaternion to compare.
     * @return {@code true} if this vector is equivalent to other.
     */
    public boolean equivalent(@NonNull Vector3 v) {
        if (this == v) return true;
        return MathUtil.isApproxEqual(x, v.x) &&
                MathUtil.isApproxEqual(y, v.y) &&
                MathUtil.isApproxEqual(z, v.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vector3 vec = (Vector3) o;

        if (Float.floatToIntBits(vec.x) != Float.floatToIntBits(x)) return false;
        if (Float.floatToIntBits(vec.y) != Float.floatToIntBits(y)) return false;
        return Float.floatToIntBits(vec.z) == Float.floatToIntBits(z);
    }

    @Override
    public int hashCode() {
        int result = (x != 0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != 0.0f ? Float.floatToIntBits(y) : 0);
        result = 31 * result + (z != 0.0f ? Float.floatToIntBits(z) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Vector3(" + x +
                ", " + y +
                ", " + z +
                ')';
    }

    @NonNull
    public Vector3 copy() {
        return new Vector3(x, y, z);
    }
}
