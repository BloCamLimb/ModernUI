/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import javax.annotation.*;

/**
 * Interface to a read-only view of a 4x4 matrix for 3D transformation.
 * This does not mean that the matrix is immutable, it only implies that
 * a method should not change the state of the matrix.
 * <p>
 * {@code Matrix4 const &matrix}
 *
 * @author BloCamLimb
 * @see Matrix4
 */
public sealed interface Matrix4c permits Matrix4 {
    // one implementation is fast

    float m11();

    float m12();

    float m13();

    float m14();

    float m21();

    float m22();

    float m23();

    float m24();

    float m31();

    float m32();

    float m33();

    float m34();

    float m41();

    float m42();

    float m43();

    float m44();

    /**
     * Store this matrix elements to the given matrix.
     *
     * @param m the matrix to store
     */
    void store(@Nonnull Matrix4 m);

    /**
     * Compute the inverse of this matrix. The matrix will be inverted
     * if this matrix is invertible, otherwise it keeps the same as before.
     *
     * @param dest the destination matrix
     * @return {@code true} if this matrix is invertible.
     */
    boolean invert(@Nullable Matrix4 dest);

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
    boolean isAxisAligned();

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRect(@Nonnull Rect2f r);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRect(@Nonnull Rect2fc r, @Nonnull Rect2f dest);
}
