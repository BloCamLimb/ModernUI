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

/**
 * Represents a 3x3 matrix.
 */
public class Matrix3 implements Cloneable {

    // matrix elements, m(ij) (row, column)
    // directly use primitive type will be faster than array in Java
    protected float m11;
    protected float m12;
    protected float m13;
    protected float m21;
    protected float m22;
    protected float m23;
    protected float m31;
    protected float m32;
    protected float m33;
}
