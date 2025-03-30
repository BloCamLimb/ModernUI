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

import icyllis.arc3d.sketch.Matrix;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

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
public sealed interface Matrix4c extends Cloneable permits Matrix4 {
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
    void store(@NonNull Matrix4 m);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the array to store into
     */
    void store(float @NonNull [] a);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a      the array to store into
     * @param offset the element offset
     */
    void store(float @NonNull [] a, int offset);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    void store(@NonNull ByteBuffer a);

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    void store(@NonNull FloatBuffer a);

    /**
     * Store this matrix into the given address in GLSL column-major or
     * HLSL row-major order.
     *
     * @param p the pointer of the array to store
     */
    void store(long p);

    /**
     * Store this matrix as 2D matrix into the given address in GLSL column-major or
     * HLSL row-major order, NOT vec4 aligned.
     * <p>
     * Equivalent to call {@link #toMatrix()} and {@link Matrix#store(long)}.
     *
     * @param p the pointer of the array to store
     */
    void storeAs2D(long p);

    /**
     * Store this matrix as 2D matrix into the given address in GLSL column-major or
     * HLSL row-major order, NOT vec4 aligned.
     * <p>
     * Equivalent to call {@link #toMatrix()} and {@link Matrix#storeAligned(long)}.
     *
     * @param p the pointer of the array to store
     */
    void storeAs2DAligned(long p);

    /**
     * Return the determinant of this matrix.
     *
     * @return the determinant
     */
    float determinant();

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
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scale, translate, or both.
     */
    boolean isScaleTranslate();

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRect(@NonNull Rect2f r);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRect(@NonNull Rect2fc r, @NonNull Rect2f dest);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRect(@NonNull Rect2fc r, @NonNull Rect2i dest);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRect(@NonNull Rect2ic r, @NonNull Rect2i dest);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRectOut(@NonNull Rect2ic r, @NonNull Rect2i dest);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRectOut(@NonNull Rect2fc r, @NonNull Rect2i dest);

    /**
     * Map the four corners of 'r' and return the bounding box of those points. The four corners of
     * 'r' are assumed to have z = 0 and w = 1. If the matrix has perspective, the returned
     * rectangle will be the bounding box of the projected points after being clipped to w > 0.
     */
    void mapRectIn(@NonNull Rect2fc r, @NonNull Rect2i dest);

    boolean hasPerspective();

    /**
     * Return the minimum distance needed to move in local (pre-transform) space to ensure that the
     * transformed coordinates are at least 1px away from the original mapped point. This minimum
     * distance is specific to the given local 'bounds' since the scale factors change with
     * perspective.
     * <p>
     * If the bounds will be clipped by the w=0 plane or otherwise is ill-conditioned, this will
     * return positive infinity.
     */
    float localAARadius(Rect2fc bounds);

    /**
     * Return the minimum distance needed to move in local (pre-transform) space to ensure that the
     * transformed coordinates are at least 1px away from the original mapped point. This minimum
     * distance is specific to the given local 'bounds' since the scale factors change with
     * perspective.
     * <p>
     * If the bounds will be clipped by the w=0 plane or otherwise is ill-conditioned, this will
     * return positive infinity.
     */
    float localAARadius(float left, float top, float right, float bottom);

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the third row and column are discarded.
     * <pre>{@code
     * [ a b x c ]      [ a b c ]
     * [ d e x f ]  ->  [ d e f ]
     * [ x x x x ]      [ g h i ]
     * [ g h x i ]
     * }</pre>
     */
    void toMatrix(@NonNull Matrix dest);

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the third row and column are discarded.
     * <pre>{@code
     * [ a b x c ]      [ a b c ]
     * [ d e x f ]  ->  [ d e f ]
     * [ x x x x ]      [ g h i ]
     * [ g h x i ]
     * }</pre>
     */
    @NonNull
    Matrix toMatrix();

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the fourth row and column are discarded.
     * <pre>{@code
     * [ a b c x ]      [ a b c ]
     * [ d e f x ]  ->  [ d e f ]
     * [ g h i x ]      [ g h i ]
     * [ x x x x ]
     * }</pre>
     */
    void toMatrix3(@NonNull Matrix3 dest);

    /**
     * Converts this 4x4 matrix to 3x3 matrix, the fourth row and column are discarded.
     * <pre>{@code
     * [ a b c x ]      [ a b c ]
     * [ d e f x ]  ->  [ d e f ]
     * [ g h i x ]      [ g h i ]
     * [ x x x x ]
     * }</pre>
     */
    @NonNull
    Matrix3 toMatrix3();

    /**
     * Returns whether this matrix is approximately equal to given matrix.
     *
     * @param m the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    boolean isApproxEqual(@NonNull Matrix4 m);

    @NonNull
    Matrix4 clone();
}
