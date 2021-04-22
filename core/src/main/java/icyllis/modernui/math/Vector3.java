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

/**
 * Represents a three-dimensional vector.
 */
@SuppressWarnings("unused")
public class Vector3 implements Cloneable {

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
     * Transform this vector by a 4x4 transformation matrix.
     *
     * @param mat the matrix to transform from
     */
    public void transform(@Nonnull Matrix4 mat) {
        mat.transform(this);
    }

    /**
     * Sort components of this in ascending order.
     */
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

        Vector3 vector3 = (Vector3) o;

        if (!MathUtil.exactlyEqual(vector3.x, x)) return false;
        if (!MathUtil.exactlyEqual(vector3.y, y)) return false;
        return MathUtil.exactlyEqual(vector3.z, z);
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        result = 31 * result + (z != +0.0f ? Float.floatToIntBits(z) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Vector3{" + "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    @Nonnull
    public Vector3 copy() {
        try {
            return (Vector3) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
