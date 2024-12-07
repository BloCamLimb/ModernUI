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
import org.lwjgl.system.MemoryUtil;

public class Matrix3 {

    public float m11;
    public float m12;
    public float m13;
    public float m21;
    public float m22;
    public float m23;
    public float m31;
    public float m32;
    public float m33;

    public Matrix3() {
    }

    /**
     * Create a new identity matrix.
     *
     * @return an identity matrix
     */
    @NonNull
    public static Matrix3 identity() {
        final Matrix3 m = new Matrix3();
        m.m11 = m.m22 = m.m33 = 1.0f;
        return m;
    }

    /**
     * Reset this matrix to the identity.
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
     * Get this matrix data, store them into an address (UNSAFE).
     * NOTE: This method does not perform memory security checks.
     *
     * @param p the pointer of the array to store
     */
    public void store(long p) {
        MemoryUtil.memPutFloat(p, m11);
        MemoryUtil.memPutFloat(p + 4, m12);
        MemoryUtil.memPutFloat(p + 8, m13);
        MemoryUtil.memPutFloat(p + 12, m21);
        MemoryUtil.memPutFloat(p + 16, m22);
        MemoryUtil.memPutFloat(p + 20, m23);
        MemoryUtil.memPutFloat(p + 24, m31);
        MemoryUtil.memPutFloat(p + 28, m32);
        MemoryUtil.memPutFloat(p + 32, m33);
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
        MemoryUtil.memPutFloat(p + 8, m13);
        MemoryUtil.memPutFloat(p + 16, m21);
        MemoryUtil.memPutFloat(p + 20, m22);
        MemoryUtil.memPutFloat(p + 24, m23);
        MemoryUtil.memPutFloat(p + 32, m31);
        MemoryUtil.memPutFloat(p + 36, m32);
        MemoryUtil.memPutFloat(p + 40, m33);
    }
}
