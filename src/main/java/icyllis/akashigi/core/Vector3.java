/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.core;

import javax.annotation.Nonnull;

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
    public void set(@Nonnull Vector3 v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    /**
     * Add each component of a vector to this vector.
     *
     * @param v the vector to add
     */
    public void add(@Nonnull Vector3 v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    /**
     * Subtract each component of a vector from this vector.
     *
     * @param v the vector to subtract
     */
    public void subtract(@Nonnull Vector3 v) {
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
    public void multiply(@Nonnull Vector3 v) {
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
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public float lengthSquared() {
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
    public float dot(@Nonnull Vector3 v) {
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
    public void cross(@Nonnull Vector3 v) {
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
    public void minComponent(@Nonnull Vector3 v) {
        x = Math.min(x, v.x);
        y = Math.min(y, v.y);
        z = Math.min(z, v.z);
    }

    /**
     * Calculate component-wise maximum of this and the given vector.
     */
    public void maxComponent(@Nonnull Vector3 v) {
        x = Math.max(x, v.x);
        y = Math.max(y, v.y);
        z = Math.max(z, v.z);
    }

    /**
     * Normalize this vector to unit length, or <code>[1, 0, 0]</code> if this is zero.
     */
    public void normalize() {
        final float sq = lengthSquared();
        if (sq < FMath.EPS) {
            x = 1.0f;
            y = 0.0f;
            z = 0.0f;
        } else {
            final float invNorm = (float) (1.0f / Math.sqrt(sq));
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
            l = (float) (1.0f / Math.sqrt(x * x + z * z));
            float t = x;
            x = -z * l;
            y = 0.0f;
            z = t * l;
        } else {
            l = (float) (1.0f / Math.sqrt(y * y + z * z));
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
    public void projection(@Nonnull Vector3 v) {
        final float sq = lengthSquared();
        if (sq < 1.0e-6f) {
            setZero();
        } else {
            final float c = (float) (dot(v) / Math.sqrt(sq));
            x = v.x * c;
            y = v.y * c;
            z = v.z * c;
        }
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

    @Nonnull
    public Vector3 copy() {
        return new Vector3(x, y, z);
    }
}
