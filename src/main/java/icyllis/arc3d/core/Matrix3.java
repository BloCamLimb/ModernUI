/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import javax.annotation.Nonnull;

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
    @Nonnull
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
}
