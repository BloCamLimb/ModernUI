/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Matrix3;
import icyllis.arc3d.core.Matrix4;
import icyllis.arc3d.core.Matrix4c;
import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.core.Rect2i;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.CheckReturnValue;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * This class represents a 3x3 matrix and a 2D transformation, its components
 * correspond to x, y, and w of a 4x4 matrix, where z is discarded.
 * <p>
 * This class also computes a type mask to simplify some operations, while
 * {@link Matrix3} and {@link Matrix4} do not have this feature.
 * <p>
 * The memory layout (order of components) is the same as GLSL's column-major and
 * HLSL's row-major, this is just a difference in naming and writing.
 *
 * @see Matrix3
 * @see Matrix4
 */
@SuppressWarnings("unused")
public non-sealed class Matrix implements Matrixc, Cloneable {

    /**
     * Set if the matrix will map a rectangle to another rectangle. This
     * can be true if the matrix is scale-only, or rotates a multiple of
     * 90 degrees.
     * <p>
     * This bit will be set on identity matrices
     */
    private static final int kAxisAligned_Mask = 0x10;
    private static final int kAxisAligned_Shift = 4;
    /**
     * Set if the perspective bit is valid even though the rest of
     * the matrix is Unknown.
     */
    private static final int kOnlyPerspectiveValid_Mask = 0x40;
    private static final int kUnknown_Mask = 0x80;

    private static final Matrixc IDENTITY = new Matrix();

    // sequential matrix elements, m(ij) (row, column)
    // directly using primitives will be faster than array in Java (before Vector API)
    // [m11 m12 m14]
    // [m21 m22 m24]
    // [m41 m42 m44] <- [m41 m42] represents the origin
    protected float m11; // scaleX
    protected float m12; // shearY
    protected float m14; // persp0
    protected float m21; // shearX
    protected float m22; // scaleY
    protected float m24; // persp1
    protected float m41; // transX
    protected float m42; // transY
    protected float m44; // persp2

    private int mTypeMask;

    /**
     * Create a new identity matrix.
     */
    public Matrix() {
        m11 = m22 = m44 = 1.0f;
        mTypeMask = kIdentity_Mask | kAxisAligned_Mask;
    }

    /**
     * Create a new matrix copied from the given matrix.
     */
    public Matrix(@NonNull Matrixc m) {
        m.store(this);
    }

    /**
     * Create a new matrix from the given elements.
     * The order matches GLSL's column major.
     *
     * @param scaleX the value of m11
     * @param shearY the value of m12
     * @param persp0 the value of m14
     * @param shearX the value of m21
     * @param scaleY the value of m22
     * @param persp1 the value of m24
     * @param transX the value of m41
     * @param transY the value of m42
     * @param persp2 the value of m44
     */
    public Matrix(float scaleX, float shearY, float persp0,
                  float shearX, float scaleY, float persp1,
                  float transX, float transY, float persp2) {
        set(scaleX, shearY, persp0, shearX, scaleY, persp1, transX, transY, persp2);
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
    public Matrix(@NonNull Matrix4 m) {
        set(m);
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
    public Matrix(@NonNull Matrix4c m) {
        set(m);
    }

    /**
     * Returns a read-only identity matrix.
     *
     * @return an identity matrix
     */
    public static @NonNull Matrixc identity() {
        return IDENTITY;
    }

    /**
     * Creates a new translate-only matrix.
     *
     * @return a translation matrix
     */
    public static @NonNull Matrix makeTranslate(float dx, float dy) {
        var matrix = new Matrix();
        matrix.setTranslate(dx, dy);
        return matrix;
    }

    /**
     * Creates a new scale-only matrix.
     *
     * @return a scaling matrix
     */
    public static @NonNull Matrix makeScale(float sx, float sy) {
        var matrix = new Matrix();
        matrix.setScale(sx, sy);
        return matrix;
    }

    /**
     * Same as {@link #getScaleX()}.
     */
    public float m11() {
        return m11;
    }

    /**
     * Same as {@link #getShearY()}.
     */
    public float m12() {
        return m12;
    }

    /**
     * Same as {@link #getPerspX()}.
     */
    public float m14() {
        return m14;
    }

    /**
     * Same as {@link #getShearX()}.
     */
    public float m21() {
        return m21;
    }

    /**
     * Same as {@link #getScaleY()}.
     */
    public float m22() {
        return m22;
    }

    /**
     * Same as {@link #getPerspY()}.
     */
    public float m24() {
        return m24;
    }

    /**
     * Same as {@link #getTranslateX()}.
     */
    public float m41() {
        return m41;
    }

    /**
     * Same as {@link #getTranslateY()}.
     */
    public float m42() {
        return m42;
    }

    /**
     * Returns the last element of the matrix, the perspective bias.
     */
    public float m44() {
        return m44;
    }

    /**
     * Returns scale factor multiplied by x-axis input, contributing to x-axis output.
     * With mapPoints(), scales Point along the x-axis.
     *
     * @return horizontal scale factor
     */
    public float getScaleX() {
        return m11;
    }

    /**
     * Returns scale factor multiplied by y-axis input, contributing to y-axis output.
     * With mapPoints(), scales Point along the y-axis.
     *
     * @return vertical scale factor
     */
    public float getScaleY() {
        return m22;
    }

    /**
     * Returns scale factor multiplied by x-axis input, contributing to y-axis output.
     * With mapPoints(), shears Point along the y-axis.
     * Shearing both axes can rotate Point.
     *
     * @return vertical shear factor
     */
    public float getShearY() {
        return m12;
    }

    /**
     * Returns scale factor multiplied by y-axis input, contributing to x-axis output.
     * With mapPoints(), shears Point along the x-axis.
     * Shearing both axes can rotate Point.
     *
     * @return horizontal shear factor
     */
    public float getShearX() {
        return m21;
    }

    /**
     * Returns translation contributing to x-axis output.
     * With mapPoints(), moves Point along the x-axis.
     *
     * @return horizontal translation factor
     */
    public float getTranslateX() {
        return m41;
    }

    /**
     * Returns translation contributing to y-axis output.
     * With mapPoints(), moves Point along the y-axis.
     *
     * @return vertical translation factor
     */
    public float getTranslateY() {
        return m42;
    }

    /**
     * Returns factor scaling input x-axis relative to input y-axis.
     *
     * @return input x-axis perspective factor
     */
    public float getPerspX() {
        return m14;
    }

    /**
     * Returns factor scaling input y-axis relative to input x-axis.
     *
     * @return input y-axis perspective factor
     */
    public float getPerspY() {
        return m24;
    }

    /**
     * Returns a bit field describing the transformations the matrix may
     * perform. The bit field is computed conservatively, so it may include
     * false positives. For example, when Perspective_Mask is set, all
     * other bits are set.
     *
     * @return Identity_Mask, or combinations of: Translate_Mask, Scale_Mask,
     * Affine_Mask, Perspective_Mask
     */
    public int getType() {
        if ((mTypeMask & kUnknown_Mask) != 0) {
            mTypeMask = computeTypeMask();
        }
        // only return the public masks
        return (mTypeMask & 0xF);
    }

    /**
     * Returns true if this matrix is identity.
     *
     * @return {@code true} if this matrix is identity.
     */
    public boolean isIdentity() {
        return getType() == kIdentity_Mask;
    }

    /**
     * Returns whether this matrix at most scales and translates.
     *
     * @return {@code true} if this matrix is scales, translates, or both.
     */
    public boolean isScaleTranslate() {
        return (getType() & ~(kScale_Mask | kTranslate_Mask)) == 0;
    }

    /**
     * Returns whether this matrix is identity, or translates.
     *
     * @return {@code true} if this matrix is identity, or translates
     */
    public boolean isTranslate() {
        return (getType() & ~(kTranslate_Mask)) == 0;
    }

    /**
     * Returns whether this matrix transforms rect to another rect. If true, this matrix is identity,
     * or/and scales, or mirrors on axes. In all cases, this matrix is affine and may also have translation.
     *
     * @return true if this matrix transform one rect into another
     */
    public boolean isAxisAligned() {
        if ((mTypeMask & kUnknown_Mask) != 0) {
            mTypeMask = computeTypeMask();
        }
        return (mTypeMask & kAxisAligned_Mask) != 0;
    }

    /**
     * Returns true if this matrix contains only translation, rotation, reflection, and
     * scale. Scale may differ along rotated axes.<br>
     * Returns false if this matrix shearing, perspective, or degenerate forms that collapse
     * to a line or point.
     * <p>
     * Preserves right angles, but not requiring that the arms of the angle
     * retain equal lengths.
     *
     * @return true if this matrix only rotates, scales, translates
     */
    public boolean preservesRightAngles() {
        int mask = getType();

        if (mask <= kTranslate_Mask) {
            // identity, translate and/or scale
            return true;
        }
        if ((mask & kPerspective_Mask) != 0) {
            return false;
        }

        assert (mask & (kAffine_Mask | kScale_Mask)) != 0;

        float mx = m11;
        float my = m22;
        float sx = m21;
        float sy = m12;

        // check if upper-left 2x2 of matrix is degenerate
        float det22 = mx * my - sx * sy;
        if (MathUtil.isApproxZero(det22)) {
            return false;
        }

        // upper 2x2 is scale + rotation/reflection if basis vectors are orthogonal
        return MathUtil.isApproxZero(mx * sx + sy * my);
    }

    /**
     * Returns whether this matrix contains perspective elements.
     *
     * @return true if this matrix is in most general form
     */
    public boolean hasPerspective() {
        if ((mTypeMask & kUnknown_Mask) != 0 &&
                (mTypeMask & kOnlyPerspectiveValid_Mask) == 0) {
            mTypeMask = computePerspectiveTypeMask();
        }
        return (mTypeMask & kPerspective_Mask) != 0;
    }

    /**
     * Returns true if this matrix contains only translation, rotation, reflection, and
     * uniform scale. Returns false if this matrix contains different scales, shearing,
     * perspective, or degenerate forms that collapse to a line or point.
     * <p>
     * Describes that the matrix makes rendering with and without the matrix are
     * visually alike; a transformed circle remains a circle. Mathematically, this is
     * referred to as similarity of a Euclidean space, or a similarity transformation.
     * <p>
     * Preserves right angles, keeping the arms of the angle equal lengths.
     *
     * @return true if this matrix only rotates, uniformly scales, translates
     */
    public boolean isSimilarity() {
        // if identity or translate matrix
        int mask = getType();
        if (mask <= kTranslate_Mask) {
            return true;
        }
        if ((mask & kPerspective_Mask) != 0) {
            return false;
        }

        float mx = m11;
        float my = m22;
        // if no shear, can just compare scale factors
        if ((mask & kAffine_Mask) == 0) {
            return !MathUtil.isApproxZero(mx) &&
                    MathUtil.isApproxEqual(Math.abs(mx), Math.abs(my));
        }
        float sx = m21;
        float sy = m12;

        // check if upper-left 2x2 of matrix is degenerate
        float det22 = mx * my - sx * sy;
        if (MathUtil.isApproxZero(det22)) {
            return false;
        }

        // upper 2x2 is rotation/reflection + uniform scale if basis vectors
        // are 90 degree rotations of each other
        return (MathUtil.isApproxEqual(mx, my) && MathUtil.isApproxEqual(sx, -sy)) ||
                (MathUtil.isApproxEqual(mx, -my) && MathUtil.isApproxEqual(sx, sy));
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
    public void preConcat(@NonNull Matrixc lhs) {
        int bMask = getType();
        if (bMask == kIdentity_Mask) {
            set(lhs);
            return;
        }
        int aMask = lhs.getType();
        if (aMask == kIdentity_Mask) {
            return;
        }
        if (((aMask | bMask) & (kAffine_Mask | kPerspective_Mask)) == 0) {
            // both are ScaleTranslate
            setScaleTranslate(
                    /*m11*/ lhs.m11() * m11,
                    /*m22*/ lhs.m22() * m22,
                    /*m41*/ lhs.m41() * m11 + m41,
                    /*m42*/ lhs.m42() * m22 + m42
            );
            return;
        }
        final float f11;
        final float f12;
        final float f14;
        final float f21;
        final float f22;
        final float f24;
        final float f41;
        final float f42;
        final float f44;
        if (((aMask | bMask) & kPerspective_Mask) == 0) {
            // both have no perspective
            f11 = lhs.m11() * m11 + lhs.m12() * m21;
            f12 = lhs.m11() * m12 + lhs.m12() * m22;
            f14 = 0;
            f21 = lhs.m21() * m11 + lhs.m22() * m21;
            f22 = lhs.m21() * m12 + lhs.m22() * m22;
            f24 = 0;
            f41 = lhs.m41() * m11 + lhs.m42() * m21 + m41;
            f42 = lhs.m41() * m12 + lhs.m42() * m22 + m42;
            f44 = 1;
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            f11 = lhs.m11() * m11 + lhs.m12() * m21 + lhs.m14() * m41;
            f12 = lhs.m11() * m12 + lhs.m12() * m22 + lhs.m14() * m42;
            f14 = lhs.m11() * m14 + lhs.m12() * m24 + lhs.m14() * m44;
            f21 = lhs.m21() * m11 + lhs.m22() * m21 + lhs.m24() * m41;
            f22 = lhs.m21() * m12 + lhs.m22() * m22 + lhs.m24() * m42;
            f24 = lhs.m21() * m14 + lhs.m22() * m24 + lhs.m24() * m44;
            f41 = lhs.m41() * m11 + lhs.m42() * m21 + lhs.m44() * m41;
            f42 = lhs.m41() * m12 + lhs.m42() * m22 + lhs.m44() * m42;
            f44 = lhs.m41() * m14 + lhs.m42() * m24 + lhs.m44() * m44;
            mTypeMask = kUnknown_Mask;
        }
        m11 = f11;
        m12 = f12;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m24 = f24;
        m41 = f41;
        m42 = f42;
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
    public void postConcat(@NonNull Matrixc rhs) {
        int aMask = getType();
        if (aMask == kIdentity_Mask) {
            set(rhs);
            return;
        }
        int bMask = rhs.getType();
        if (bMask == kIdentity_Mask) {
            return;
        }
        if (((aMask | bMask) & (kAffine_Mask | kPerspective_Mask)) == 0) {
            // both are ScaleTranslate
            setScaleTranslate(
                    /*m11*/ m11 * rhs.m11(),
                    /*m22*/ m22 * rhs.m22(),
                    /*m41*/ m41 * rhs.m11() + rhs.m41(),
                    /*m42*/ m42 * rhs.m22() + rhs.m42()
            );
            return;
        }
        final float f11;
        final float f12;
        final float f14;
        final float f21;
        final float f22;
        final float f24;
        final float f41;
        final float f42;
        final float f44;
        if (((aMask | bMask) & kPerspective_Mask) == 0) {
            // both have no perspective
            f11 = m11 * rhs.m11() + m12 * rhs.m21();
            f12 = m11 * rhs.m12() + m12 * rhs.m22();
            f14 = 0;
            f21 = m21 * rhs.m11() + m22 * rhs.m21();
            f22 = m21 * rhs.m12() + m22 * rhs.m22();
            f24 = 0;
            f41 = m41 * rhs.m11() + m42 * rhs.m21() + rhs.m41();
            f42 = m41 * rhs.m12() + m42 * rhs.m22() + rhs.m42();
            f44 = 1;
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            f11 = m11 * rhs.m11() + m12 * rhs.m21() + m14 * rhs.m41();
            f12 = m11 * rhs.m12() + m12 * rhs.m22() + m14 * rhs.m42();
            f14 = m11 * rhs.m14() + m12 * rhs.m24() + m14 * rhs.m44();
            f21 = m21 * rhs.m11() + m22 * rhs.m21() + m24 * rhs.m41();
            f22 = m21 * rhs.m12() + m22 * rhs.m22() + m24 * rhs.m42();
            f24 = m21 * rhs.m14() + m22 * rhs.m24() + m24 * rhs.m44();
            f41 = m41 * rhs.m11() + m42 * rhs.m21() + m44 * rhs.m41();
            f42 = m41 * rhs.m12() + m42 * rhs.m22() + m44 * rhs.m42();
            f44 = m41 * rhs.m14() + m42 * rhs.m24() + m44 * rhs.m44();
            mTypeMask = kUnknown_Mask;
        }
        m11 = f11;
        m12 = f12;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m24 = f24;
        m41 = f41;
        m42 = f42;
        m44 = f44;
    }

    /**
     * Reset this matrix to the identity.
     */
    public void setIdentity() {
        m11 = 1.0f;
        m12 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 1.0f;
        m24 = 0.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m44 = 1.0f;
        mTypeMask = kIdentity_Mask | kAxisAligned_Mask;
    }

    /**
     * Reset this matrix with scale and translate elements.
     *
     * @param sx horizontal scale factor to store
     * @param sy vertical scale factor to store
     * @param tx horizontal translation to store
     * @param ty vertical translation to store
     */
    public void setScaleTranslate(float sx, float sy, float tx, float ty) {
        m11 = sx;
        m12 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = sy;
        m24 = 0.0f;
        m41 = tx;
        m42 = ty;
        m44 = 1.0f;
        int mask = 0;
        if (sx != 1 || sy != 1) {
            mask |= kScale_Mask;
        }
        if (tx != 0.0f || ty != 0.0f) {
            mask |= kTranslate_Mask;
        }
        if (sx != 0 && sy != 0) {
            mask |= kAxisAligned_Mask;
        }
        mTypeMask = mask;
    }

    /**
     * Set the scaleX value.
     */
    public void m11(float scaleX) {
        m11 = scaleX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the shearY value.
     */
    public void m12(float shearY) {
        m12 = shearY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the persp0 value.
     */
    public void m14(float persp0) {
        m14 = persp0;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the shearX value.
     */
    public void m21(float shearX) {
        m21 = shearX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the scaleY value.
     */
    public void m22(float scaleY) {
        m22 = scaleY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the persp1 value.
     */
    public void m24(float persp1) {
        m24 = persp1;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the transX value.
     */
    public void m41(float transX) {
        m41 = transX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the transY value.
     */
    public void m42(float transY) {
        m42 = transY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the persp2 value.
     */
    public void m44(float persp2) {
        m44 = persp2;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the scaleX value.
     */
    public void setScaleX(float scaleX) {
        m11 = scaleX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the scaleY value.
     */
    public void setScaleY(float scaleY) {
        m22 = scaleY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the shearY value.
     */
    public void setShearY(float shearY) {
        m12 = shearY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the shearX value.
     */
    public void setShearX(float shearX) {
        m21 = shearX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the transX value.
     */
    public void setTranslateX(float transX) {
        m41 = transX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the transY value.
     */
    public void setTranslateY(float transY) {
        m42 = transY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the perspY value.
     */
    public void setPerspY(float perspY) {
        m24 = perspY;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the perspX value.
     */
    public void setPerspX(float perspX) {
        m14 = perspX;
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Store the values of the given matrix into this matrix.
     *
     * @param m the matrix to copy from
     */
    public void set(@NonNull Matrixc m) {
        m.store(this);
    }

    /**
     * Sets all values from parameters.
     *
     * @param scaleX horizontal scale factor to store
     * @param shearX horizontal shear factor to store
     * @param transX horizontal translation to store
     * @param shearY vertical shear factor to store
     * @param scaleY vertical scale factor to store
     * @param transY vertical translation to store
     * @param persp0 input x-axis values perspective factor to store
     * @param persp1 input y-axis values perspective factor to store
     * @param persp2 perspective scale factor to store
     */
    public void set(float scaleX, float shearY, float persp0,
                    float shearX, float scaleY, float persp1,
                    float transX, float transY, float persp2) {
        m11 = scaleX;
        m12 = shearY;
        m14 = persp0;
        m21 = shearX;
        m22 = scaleY;
        m24 = persp1;
        m41 = transX;
        m42 = transY;
        m44 = persp2;
        mTypeMask = kUnknown_Mask;
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
    public void set(@NonNull Matrix4 m) {
        set(m.m11, m.m12, m.m14,
                m.m21, m.m22, m.m24,
                m.m41, m.m42, m.m44);
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
    public void set(@NonNull Matrix4c m) {
        set(m.m11(), m.m12(), m.m14(),
                m.m21(), m.m22(), m.m24(),
                m.m41(), m.m42(), m.m44());
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a the array to copy from
     */
    public void set(float @NonNull [] a) {
        m11 = a[0];
        m12 = a[1];
        m14 = a[2];
        m21 = a[3];
        m22 = a[4];
        m24 = a[5];
        m41 = a[6];
        m42 = a[7];
        m44 = a[8];
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a      the array to copy from
     * @param offset the element offset
     */
    public void set(float @NonNull [] a, int offset) {
        m11 = a[offset];
        m12 = a[offset + 1];
        m14 = a[offset + 2];
        m21 = a[offset + 3];
        m22 = a[offset + 4];
        m24 = a[offset + 5];
        m41 = a[offset + 6];
        m42 = a[offset + 7];
        m44 = a[offset + 8];
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a the array to copy from
     */
    public void set(@NonNull ByteBuffer a) {
        int offset = a.position();
        m11 = a.getFloat(offset);
        m12 = a.getFloat(offset + 4);
        m14 = a.getFloat(offset + 8);
        m21 = a.getFloat(offset + 12);
        m22 = a.getFloat(offset + 16);
        m24 = a.getFloat(offset + 20);
        m41 = a.getFloat(offset + 24);
        m42 = a.getFloat(offset + 28);
        m44 = a.getFloat(offset + 32);
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param a the array to copy from
     */
    public void set(@NonNull FloatBuffer a) {
        int offset = a.position();
        m11 = a.get(offset);
        m12 = a.get(offset + 1);
        m14 = a.get(offset + 2);
        m21 = a.get(offset + 3);
        m22 = a.get(offset + 4);
        m24 = a.get(offset + 5);
        m41 = a.get(offset + 6);
        m42 = a.get(offset + 7);
        m44 = a.get(offset + 8);
        mTypeMask = kUnknown_Mask;
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
        m14 = MemoryUtil.memGetFloat(p + 8);
        m21 = MemoryUtil.memGetFloat(p + 12);
        m22 = MemoryUtil.memGetFloat(p + 16);
        m24 = MemoryUtil.memGetFloat(p + 20);
        m41 = MemoryUtil.memGetFloat(p + 24);
        m42 = MemoryUtil.memGetFloat(p + 28);
        m44 = MemoryUtil.memGetFloat(p + 32);
        mTypeMask = kUnknown_Mask;
    }

    /**
     * Store this matrix elements to the given matrix.
     *
     * @param dst the matrix to store
     */
    public void store(@NonNull Matrix dst) {
        dst.m11 = m11;
        dst.m12 = m12;
        dst.m14 = m14;
        dst.m21 = m21;
        dst.m22 = m22;
        dst.m24 = m24;
        dst.m41 = m41;
        dst.m42 = m42;
        dst.m44 = m44;
        dst.mTypeMask = mTypeMask;
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the array to store into
     */
    public void store(float @NonNull [] a) {
        a[0] = m11;
        a[1] = m12;
        a[2] = m14;
        a[3] = m21;
        a[4] = m22;
        a[5] = m24;
        a[6] = m41;
        a[7] = m42;
        a[8] = m44;
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a      the array to store into
     * @param offset the element offset
     */
    public void store(float @NonNull [] a, int offset) {
        a[offset] = m11;
        a[offset + 1] = m12;
        a[offset + 2] = m14;
        a[offset + 3] = m21;
        a[offset + 4] = m22;
        a[offset + 5] = m24;
        a[offset + 6] = m41;
        a[offset + 7] = m42;
        a[offset + 8] = m44;
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    public void store(@NonNull ByteBuffer a) {
        int offset = a.position();
        a.putFloat(offset, m11);
        a.putFloat(offset + 4, m12);
        a.putFloat(offset + 8, m14);
        a.putFloat(offset + 12, m21);
        a.putFloat(offset + 16, m22);
        a.putFloat(offset + 20, m24);
        a.putFloat(offset + 24, m41);
        a.putFloat(offset + 28, m42);
        a.putFloat(offset + 32, m44);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     * The data matches std140 layout so it is not tightly packed.
     *
     * @param a the pointer of the array to store
     */
    public void storeAligned(@NonNull ByteBuffer a) {
        int offset = a.position();
        a.putFloat(offset, m11);
        a.putFloat(offset + 4, m12);
        a.putFloat(offset + 8, m14);
        a.putFloat(offset + 16, m21);
        a.putFloat(offset + 20, m22);
        a.putFloat(offset + 24, m24);
        a.putFloat(offset + 32, m41);
        a.putFloat(offset + 36, m42);
        a.putFloat(offset + 40, m44);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param a the pointer of the array to store
     */
    public void store(@NonNull FloatBuffer a) {
        int offset = a.position();
        a.put(offset, m11);
        a.put(offset + 1, m12);
        a.put(offset + 2, m14);
        a.put(offset + 3, m21);
        a.put(offset + 4, m22);
        a.put(offset + 5, m24);
        a.put(offset + 6, m41);
        a.put(offset + 7, m42);
        a.put(offset + 8, m44);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     * The data matches std140 layout so it is not tightly packed.
     *
     * @param a the pointer of the array to store
     */
    public void storeAligned(@NonNull FloatBuffer a) {
        int offset = a.position();
        a.put(offset, m11);
        a.put(offset + 1, m12);
        a.put(offset + 2, m14);
        a.put(offset + 4, m21);
        a.put(offset + 5, m22);
        a.put(offset + 6, m24);
        a.put(offset + 8, m41);
        a.put(offset + 9, m42);
        a.put(offset + 10, m44);
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
        MemoryUtil.memPutFloat(p + 8, m14);
        MemoryUtil.memPutFloat(p + 12, m21);
        MemoryUtil.memPutFloat(p + 16, m22);
        MemoryUtil.memPutFloat(p + 20, m24);
        MemoryUtil.memPutFloat(p + 24, m41);
        MemoryUtil.memPutFloat(p + 28, m42);
        MemoryUtil.memPutFloat(p + 32, m44);
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
        MemoryUtil.memPutFloat(p + 8, m14);
        MemoryUtil.memPutFloat(p + 16, m21);
        MemoryUtil.memPutFloat(p + 20, m22);
        MemoryUtil.memPutFloat(p + 24, m24);
        MemoryUtil.memPutFloat(p + 32, m41);
        MemoryUtil.memPutFloat(p + 36, m42);
        MemoryUtil.memPutFloat(p + 40, m44);
    }

    /**
     * Converts this 3x3 matrix to 4x4 matrix, the third row and column are identity.
     * <pre>{@code
     * [ a b c ]      [ a b 0 c ]
     * [ d e f ]  ->  [ d e 0 f ]
     * [ g h i ]      [ 0 0 1 0 ]
     *                [ g h 0 i ]
     * }</pre>
     */
    public void toMatrix4(@NonNull Matrix4 dest) {
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = 0;
        dest.m14 = m14;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = 0;
        dest.m24 = m24;
        dest.m31 = 0;
        dest.m32 = 0;
        dest.m33 = 1;
        dest.m34 = 0;
        dest.m41 = m41;
        dest.m42 = m42;
        dest.m43 = 0;
        dest.m44 = m44;
    }

    /**
     * Converts this 3x3 matrix to 4x4 matrix, the third row and column are identity.
     * <pre>{@code
     * [ a b c ]      [ a b 0 c ]
     * [ d e f ]  ->  [ d e 0 f ]
     * [ g h i ]      [ 0 0 1 0 ]
     *                [ g h 0 i ]
     * }</pre>
     */
    public @NonNull Matrix4 toMatrix4() {
        Matrix4 m = new Matrix4();
        toMatrix4(m);
        return m;
    }

    /**
     * Return the determinant of this matrix.
     *
     * @return the determinant
     */
    public float determinant() {
        double det;
        if (hasPerspective()) {
            double a = (double) m11 * m22 - (double) m12 * m21;
            double b = (double) m14 * m21 - (double) m11 * m24;
            double c = (double) m12 * m24 - (double) m14 * m22;
            det = a * m44 + b * m42 + c * m41;
        } else {
            det = (double) m11 * m22 - (double) m12 * m21;
        }
        return (float) det;
    }

    /**
     * Compute the trace of this matrix.
     *
     * @return the trace of this matrix
     */
    public float trace() {
        return m11 + m22 + m44;
    }

    /**
     * Compute the inverse of this matrix. This matrix will be inverted
     * if this matrix is invertible, otherwise its values will be preserved.
     *
     * @return {@code true} if this matrix is invertible.
     */
    @CheckReturnValue
    public boolean invert() {
        return invert(this);
    }

    /**
     * Compute the inverse of this matrix. The <var>dest</var> matrix will be
     * the inverse of this matrix if this matrix is invertible, otherwise its
     * values will be preserved.
     *
     * @param dest the destination matrix, may be null
     * @return {@code true} if this matrix is invertible.
     */
    public boolean invert(@Nullable Matrix dest) {
        int mask = getType();
        if (mask == kIdentity_Mask) {
            if (dest != null) {
                dest.setIdentity();
            }
            return true;
        }
        if ((mask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            return invertScaleTranslate(mask, dest);
        } else if ((mask & kPerspective_Mask) != 0) {
            return invertPerspective(dest);
        } else {
            return invertAffine(dest);
        }
    }

    private boolean invertScaleTranslate(int mask, Matrix dest) {
        if ((mask & kScale_Mask) != 0) {
            float invX = 1.0f / m11;
            float invY = 1.0f / m22;
            // Denormalized (non-zero) scale factors will overflow when inverted, in which case
            // the inverse matrix would not be finite, so return false.
            if (!Float.isFinite(invX) || !Float.isFinite(invY)) {
                return false;
            }
            float f41 = (float) ((double) -m41 / m11);
            float f42 = (float) ((double) -m42 / m22);
            if (!Float.isFinite(f41) || !Float.isFinite(f42)) {
                return false;
            }
            if (dest != null) {
                dest.m11 = invX;
                dest.m12 = 0;
                dest.m14 = 0;
                dest.m21 = 0;
                dest.m22 = invY;
                dest.m24 = 0;
                dest.m41 = f41;
                dest.m42 = f42;
                dest.m44 = 1;
                dest.mTypeMask = mask | kAxisAligned_Mask;
            }
        } else {
            // translate only
            if (!Float.isFinite(m41) || !Float.isFinite(m42)) {
                return false;
            }
            if (dest != null) {
                dest.setTranslate(-m41, -m42);
            }
        }
        return true;
    }

    private boolean invertPerspective(Matrix dest) {
        double a = (double) m11 * m22 - (double) m12 * m21;
        double b = (double) m14 * m21 - (double) m11 * m24;
        double c = (double) m12 * m24 - (double) m14 * m22;
        // calc the determinant
        double det = a * m44 + b * m42 + c * m41;
        if (MathUtil.isApproxZero((float) det, 1.0e-15f)) {
            return false;
        }
        // calc algebraic cofactor and transpose
        det = 1.0 / det;
        float f11 = (float) ((m22 * m44 - m42 * m24) * det); // 11
        float f12 = (float) ((m42 * m14 - m12 * m44) * det); // -21
        float f21 = (float) ((m41 * m24 - m21 * m44) * det); // -12
        float f22 = (float) ((m11 * m44 - m41 * m14) * det); // 22
        float f41 = (float) ((m21 * m42 - m41 * m22) * det); // 13
        float f42 = (float) ((m41 * m12 - m11 * m42) * det); // -23
        float f14 = (float) (c * det);
        float f24 = (float) (b * det);
        float f44 = (float) (a * det);
        if (!MathUtil.isFinite(
                f11, f12, f14,
                f21, f22, f24,
                f41, f42, f44
        )) {
            // not finite, NaN or infinity
            return false;
        }
        if (dest != null) {
            dest.m11 = f11;
            dest.m12 = f12;
            dest.m14 = f14;
            dest.m21 = f21;
            dest.m22 = f22;
            dest.m24 = f24;
            dest.m41 = f41;
            dest.m42 = f42;
            dest.m44 = f44;
            dest.mTypeMask = mTypeMask;
        }
        return true;
    }

    private boolean invertAffine(Matrix dest) {
        // not perspective
        double det = (double) m11 * m22 - (double) m12 * m21;
        if (MathUtil.isApproxZero((float) det, 1.0e-15f)) {
            return false;
        }
        det = 1.0f / det;
        float f11 = (float) (m22 * det);
        float f12 = (float) (-m12 * det);
        float f21 = (float) (-m21 * det);
        float f22 = (float) (m11 * det);
        float f41 = (float) ((m21 * m42 - m41 * m22) * det); // 13
        float f42 = (float) ((m41 * m12 - m11 * m42) * det); // -23
        if (!MathUtil.isFinite(
                f11, f12,
                f21, f22,
                f41, f42
        )) {
            // not finite, NaN or infinity
            return false;
        }
        if (dest != null) {
            dest.m11 = f11;
            dest.m12 = f12;
            dest.m14 = 0;
            dest.m21 = f21;
            dest.m22 = f22;
            dest.m24 = 0;
            dest.m41 = f41;
            dest.m42 = f42;
            dest.m44 = 1;
            dest.mTypeMask = mTypeMask;
        }
        return true;
    }

    /**
     * Translates this matrix by given changes. This is equivalent to
     * pre-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void preTranslate(float dx, float dy) {
        int mask = getType();
        if ((mask & kPerspective_Mask) != 0) {
            m41 += dx * m11 + dy * m21;
            m42 += dx * m12 + dy * m22;
            m44 += dx * m14 + dy * m24;
            mTypeMask = kUnknown_Mask;
            return;
        }
        if (mask <= kTranslate_Mask) {
            m41 += dx;
            m42 += dy;
        } else {
            m41 += dx * m11 + dy * m21;
            m42 += dx * m12 + dy * m22;
        }
        if (m41 != 0 || m42 != 0) {
            mTypeMask |= kTranslate_Mask;
        } else {
            mTypeMask &= ~kTranslate_Mask;
        }
    }

    /**
     * Post-translates this matrix by given changes. This is equivalent to
     * post-multiplying by a translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void postTranslate(float dx, float dy) {
        int mask = getType();
        if ((mask & kPerspective_Mask) != 0) {
            m11 += dx * m14;
            m12 += dy * m14;
            m21 += dx * m24;
            m22 += dy * m24;
            m41 += dx * m44;
            m42 += dy * m44;
            mTypeMask = kUnknown_Mask;
            return;
        }
        m41 += dx;
        m42 += dy;
        if (m41 != 0 || m42 != 0) {
            mTypeMask |= kTranslate_Mask;
        } else {
            mTypeMask &= ~kTranslate_Mask;
        }
    }

    /**
     * Set this matrix to be a simple translation matrix.
     *
     * @param dx the x-component of the translation
     * @param dy the y-component of the translation
     */
    public void setTranslate(float dx, float dy) {
        m11 = 1.0f;
        m12 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = 1.0f;
        m24 = 0.0f;
        m41 = dx;
        m42 = dy;
        m44 = 1.0f;
        if (dx != 0 || dy != 0) {
            mTypeMask = kTranslate_Mask | kAxisAligned_Mask;
        } else {
            mTypeMask = kIdentity_Mask | kAxisAligned_Mask;
        }
    }

    /**
     * Apply scaling to <code>this</code> matrix by scaling the base axes by the given x,
     * y and z factors.
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
        if (sx == 1 && sy == 1) {
            return;
        }
        int mask = getType();
        if (mask == kIdentity_Mask) {
            setScale(sx, sy);
            return;
        }
        if ((mask & (kAffine_Mask | kPerspective_Mask)) == 0) {
            setScaleTranslate(
                    sx * m11,
                    sy * m22,
                    m41,
                    m42
            );
            return;
        }
        m11 *= sx;
        m12 *= sx;
        m21 *= sy;
        m22 *= sy;
        if ((mask & kPerspective_Mask) == 0) {
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            m14 *= sx;
            m24 *= sy;
            mTypeMask = kUnknown_Mask;
        }
    }

    /**
     * Post-multiply scaling to <code>this</code> matrix by scaling the base axes by the given x,
     * y and z factors.
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
        if (sx == 1 && sy == 1) {
            return;
        }
        int mask = getType();
        if (mask == kIdentity_Mask) {
            setScale(sx, sy);
            return;
        }
        if ((mask & (kAffine_Mask | kPerspective_Mask)) == 0) {
            setScaleTranslate(
                    m11 * sx,
                    m22 * sy,
                    m41 * sx,
                    m42 * sy
            );
            return;
        }
        m11 *= sx;
        m21 *= sx;
        m41 *= sx;
        m12 *= sy;
        m22 *= sy;
        m42 *= sy;
        if ((mask & kPerspective_Mask) == 0) {
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            mTypeMask = kUnknown_Mask;
        }
    }

    /**
     * Set this matrix to scale by sx and sy about at pivot point at (0, 0).
     *
     * @param sx horizontal scale factor
     * @param sy vertical scale factor
     */
    public void setScale(float sx, float sy) {
        m11 = sx;
        m12 = 0.0f;
        m14 = 0.0f;
        m21 = 0.0f;
        m22 = sy;
        m24 = 0.0f;
        m41 = 0.0f;
        m42 = 0.0f;
        m44 = 1.0f;
        if (sx == 1 && sy == 1) {
            mTypeMask = kIdentity_Mask | kAxisAligned_Mask;
        } else {
            mTypeMask = kScale_Mask |
                    (sx != 0 && sy != 0 ? kAxisAligned_Mask : 0);
        }
    }

    /**
     * Set this matrix to scale by sx and sy, about a pivot point at (px, py).
     * The pivot point is unchanged when mapped with this matrix.
     *
     * @param sx horizontal scale factor
     * @param sy vertical scale factor
     * @param px pivot on x-axis
     * @param py pivot on y-axis
     */
    public void setScale(float sx, float sy, float px, float py) {
        if (sx == 1 && sy == 1) {
            setIdentity();
        } else {
            setScaleTranslate(sx, sy, px - sx * px, py - sy * py);
        }
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
    public void preRotate(float angle) {
        if (angle == 0) {
            return;
        }
        int mask = getType();
        if (mask == kIdentity_Mask) {
            setRotate(angle);
            return;
        }
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f11 = c * m11 + s * m21;
        final double f12 = c * m12 + s * m22;
        final double f14 = c * m14 + s * m24;
        m21 = (float) (c * m21 - s * m11);
        m22 = (float) (c * m22 - s * m12);
        m24 = (float) (c * m24 - s * m14);
        m11 = (float) f11;
        m12 = (float) f12;
        m14 = (float) f14;
        if ((mask & kPerspective_Mask) == 0) {
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            mTypeMask = kUnknown_Mask;
        }
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
    public void postRotate(float angle) {
        if (angle == 0) {
            return;
        }
        int mask = getType();
        if (mask == kIdentity_Mask) {
            setRotate(angle);
            return;
        }
        final double s = Math.sin(angle);
        final double c = Math.cos(angle);
        final double f12 = c * m12 + s * m11;
        final double f22 = c * m22 + s * m21;
        final double f42 = c * m42 + s * m41;
        m11 = (float) (c * m11 - s * m12);
        m21 = (float) (c * m21 - s * m22);
        m41 = (float) (c * m41 - s * m42);
        m12 = (float) f12;
        m22 = (float) f22;
        m42 = (float) f42;
        if ((mask & kPerspective_Mask) == 0) {
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            mTypeMask = kUnknown_Mask;
        }
    }

    /**
     * Set this matrix to rotate by radians about a pivot point at (0, 0).
     * Positive radians rotates clockwise.
     *
     * @param angle angle of axes relative to upright axes
     */
    public void setRotate(float angle) {
        if (angle == 0) {
            setIdentity();
        } else {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            setSinCos(
                    MathUtil.isApproxZero(s) ? 0.0f : s,
                    MathUtil.isApproxZero(c) ? 0.0f : c
            );
        }
    }

    /**
     * Set this matrix to rotate by radians about a pivot point at (px, py).
     * The pivot point is unchanged when mapped with this matrix.
     * <p>
     * Positive radians rotates clockwise.
     *
     * @param angle angle of axes relative to upright axes
     * @param px    pivot on x-axis
     * @param py    pivot on y-axis
     */
    public void setRotate(float angle, float px, float py) {
        if (angle == 0) {
            setIdentity();
        } else {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            setSinCos(
                    MathUtil.isApproxZero(s) ? 0.0f : s,
                    MathUtil.isApproxZero(c) ? 0.0f : c,
                    px, py
            );
        }
    }

    /**
     * Set this matrix to rotate by sinValue and cosValue, about a pivot point at (0, 0).
     *
     * @param sin rotation vector x-axis component
     * @param cos rotation vector y-axis component
     */
    public void setSinCos(float sin, float cos) {
        m11 = cos;
        m12 = sin;
        m14 = 0;
        m21 = -sin;
        m22 = cos;
        m24 = 0;
        m41 = 0;
        m42 = 0;
        m44 = 1;
        mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
    }

    /**
     * Set this matrix to rotate by sinValue and cosValue, about a pivot point at (px, py).
     * The pivot point is unchanged when mapped with this matrix.
     *
     * @param sin rotation vector x-axis component
     * @param cos rotation vector y-axis component
     * @param px  pivot on x-axis
     * @param py  pivot on y-axis
     */
    public void setSinCos(float sin, float cos, float px, float py) {
        double omc = 1 - cos;
        m11 = cos;
        m12 = sin;
        m14 = 0;
        m21 = -sin;
        m22 = cos;
        m24 = 0;
        m41 = (float) (omc * px + sin * py);
        m42 = (float) (omc * py - sin * px);
        m44 = 1;
        mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
    }

    /**
     * Pre-multiplied this matrix by a shearing matrix (sx, sy) about pivot point (0, 0).
     *
     * @param sx horizontal shear factor
     * @param sy vertical shear factor
     */
    public void preShear(float sx, float sy) {
        int mask = getType();
        if (mask == kIdentity_Mask) {
            setShear(sx, sy);
            return;
        }
        final float f11;
        final float f12;
        final float f14;
        final float f21;
        final float f22;
        final float f24;
        if ((mask & kPerspective_Mask) == 0) {
            f11 = m11 + sy * m21;
            f12 = m12 + sy * m22;
            f14 = 0;
            f21 = sx * m11 + m21;
            f22 = sx * m12 + m22;
            f24 = 0;
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            f11 = m11 + sy * m21;
            f12 = m12 + sy * m22;
            f14 = m14 + sy * m24;
            f21 = sx * m11 + m21;
            f22 = sx * m12 + m22;
            f24 = sx * m14 + m24;
            mTypeMask = kUnknown_Mask;
        }
        m11 = f11;
        m12 = f12;
        m14 = f14;
        m21 = f21;
        m22 = f22;
        m24 = f24;
    }

    /**
     * Post-multiplied this matrix by a shearing matrix (sx, sy) about pivot point (0, 0).
     *
     * @param sx horizontal shear factor
     * @param sy vertical shear factor
     */
    public void postShear(float sx, float sy) {
        int mask = getType();
        if (mask == kIdentity_Mask) {
            setShear(sx, sy);
            return;
        }
        final float f11;
        final float f12;
        final float f21;
        final float f22;
        final float f41;
        final float f42;
        if ((mask & kPerspective_Mask) == 0) {
            // both have no perspective
            f11 = m11 + m12 * sx;
            f12 = m11 * sy + m12;
            f21 = m21 + m22 * sx;
            f22 = m21 * sy + m22;
            f41 = m41 + m42 * sx;
            f42 = m41 * sy + m42;
            mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
        } else {
            f11 = m11 + m12 * sx;
            f12 = m11 * sy + m12;
            f21 = m21 + m22 * sx;
            f22 = m21 * sy + m22;
            f41 = m41 + m42 * sx;
            f42 = m41 * sy + m42;
            mTypeMask = kUnknown_Mask;
        }
        m11 = f11;
        m12 = f12;
        m21 = f21;
        m22 = f22;
        m41 = f41;
        m42 = f42;
    }

    /**
     * Set this matrix to shear by kx and ky, about a pivot point at (0, 0).
     *
     * @param sx horizontal shear factor
     * @param sy vertical shear factor
     */
    public void setShear(float sx, float sy) {
        m11 = 1;
        m12 = sy;
        m14 = 0;
        m21 = sx;
        m22 = 1;
        m24 = 0;
        m41 = 0;
        m42 = 0;
        m44 = 1;
        mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
    }

    /**
     * Set this matrix to shear by kx and ky, about a pivot point at (px, py).
     * The pivot point is unchanged when mapped with this matrix.
     *
     * @param sx horizontal shear factor
     * @param sy vertical shear factor
     * @param px pivot on x-axis
     * @param py pivot on y-axis
     */
    public void setShear(float sx, float sy, float px, float py) {
        m11 = 1;
        m12 = sy;
        m14 = 0;
        m21 = sx;
        m22 = 1;
        m24 = 0;
        m41 = -sx * py;
        m42 = -sy * px;
        m44 = 1;
        mTypeMask = kOnlyPerspectiveValid_Mask | kUnknown_Mask;
    }

    /**
     * Sets dst to bounds of src corners mapped by this matrix.
     * Returns true if mapped corners are dst corners.
     */
    //@formatter:off
    public boolean mapRect(@NonNull Rect2fc src, @NonNull Rect2f dst) {
        int typeMask = getType();
        final float left   = src.left();
        final float top    = src.top();
        final float right  = src.right();
        final float bottom = src.bottom();
        if (typeMask <= kTranslate_Mask) {
            dst.mLeft   = left   + m41;
            dst.mTop    = top    + m42;
            dst.mRight  = right  + m41;
            dst.mBottom = bottom + m42;
            return true;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            float x1 = left   * m11 + m41;
            float y1 = top    * m22 + m42;
            float x2 = right  * m11 + m41;
            float y2 = bottom * m22 + m42;
            dst.mLeft   = Math.min(x1, x2);
            dst.mTop    = Math.min(y1, y2);
            dst.mRight  = Math.max(x1, x2);
            dst.mBottom = Math.max(y1, y2);
            return true;
        }
        float x1 = m11 * left +  m21 * top    + m41;
        float y1 = m12 * left +  m22 * top    + m42;
        float x2 = m11 * right + m21 * top    + m41;
        float y2 = m12 * right + m22 * top    + m42;
        float x3 = m11 * left +  m21 * bottom + m41;
        float y3 = m12 * left +  m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * left  + m24 * top    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left  + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dst.mLeft   = MathUtil.min(x1, x2, x3, x4);
        dst.mTop    = MathUtil.min(y1, y2, y3, y4);
        dst.mRight  = MathUtil.max(x1, x2, x3, x4);
        dst.mBottom = MathUtil.max(y1, y2, y3, y4);
        return (typeMask & kAxisAligned_Mask) != 0;
    }
    //@formatter:on

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param dst the round values
     */
    //@formatter:off
    public void mapRect(float left, float top, float right, float bottom, @NonNull Rect2i dst) {
        int typeMask = getType();
        if (typeMask <= kTranslate_Mask) {
            dst.mLeft   = Math.round(left   + m41);
            dst.mTop    = Math.round(top    + m42);
            dst.mRight  = Math.round(right  + m41);
            dst.mBottom = Math.round(bottom + m42);
            return;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            dst.mLeft =   Math.round(left   * m11 + m41);
            dst.mTop =    Math.round(top    * m22 + m42);
            dst.mRight =  Math.round(right  * m11 + m41);
            dst.mBottom = Math.round(bottom * m22 + m42);
            dst.sort();
            return;
        }
        float x1 = m11 * left +  m21 * top    + m41;
        float y1 = m12 * left +  m22 * top    + m42;
        float x2 = m11 * right + m21 * top    + m41;
        float y2 = m12 * right + m22 * top    + m42;
        float x3 = m11 * left +  m21 * bottom + m41;
        float y3 = m12 * left +  m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * left  + m24 * top    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left  + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dst.mLeft   = Math.round(MathUtil.min(x1, x2, x3, x4));
        dst.mTop    = Math.round(MathUtil.min(y1, y2, y3, y4));
        dst.mRight  = Math.round(MathUtil.max(x1, x2, x3, x4));
        dst.mBottom = Math.round(MathUtil.max(y1, y2, y3, y4));
    }
    //@formatter:on

    /**
     * Map a rectangle points in the X-Y plane to get the maximum bounds.
     *
     * @param dst the round out values
     */
    //@formatter:off
    public void mapRectOut(float left, float top, float right, float bottom, @NonNull Rect2i dst) {
        int typeMask = getType();
        if (typeMask <= kTranslate_Mask) {
            dst.mLeft   = (int) Math.floor(left   + m41);
            dst.mTop    = (int) Math.floor(top    + m42);
            dst.mRight  = (int) Math.ceil (right  + m41);
            dst.mBottom = (int) Math.ceil (bottom + m42);
            return;
        }
        if ((typeMask & ~(kScale_Mask | kTranslate_Mask)) == 0) {
            dst.mLeft =   (int) Math.floor(left   * m11 + m41);
            dst.mTop =    (int) Math.floor(top    * m22 + m42);
            dst.mRight =  (int) Math.ceil (right  * m11 + m41);
            dst.mBottom = (int) Math.ceil (bottom * m22 + m42);
            dst.sort();
            return;
        }
        float x1 = m11 * left +  m21 * top    + m41;
        float y1 = m12 * left +  m22 * top    + m42;
        float x2 = m11 * right + m21 * top    + m41;
        float y2 = m12 * right + m22 * top    + m42;
        float x3 = m11 * left +  m21 * bottom + m41;
        float y3 = m12 * left +  m22 * bottom + m42;
        float x4 = m11 * right + m21 * bottom + m41;
        float y4 = m12 * right + m22 * bottom + m42;
        if ((typeMask & kPerspective_Mask) != 0) {
            float w;
            w = 1.0f / (m14 * left  + m24 * top    + m44);
            x1 *= w;
            y1 *= w;
            w = 1.0f / (m14 * right + m24 * top    + m44);
            x2 *= w;
            y2 *= w;
            w = 1.0f / (m14 * left  + m24 * bottom + m44);
            x3 *= w;
            y3 *= w;
            w = 1.0f / (m14 * right + m24 * bottom + m44);
            x4 *= w;
            y4 *= w;
        }
        dst.mLeft   = (int) Math.floor(MathUtil.min(x1, x2, x3, x4));
        dst.mTop    = (int) Math.floor(MathUtil.min(y1, y2, y3, y4));
        dst.mRight  = (int) Math.ceil (MathUtil.max(x1, x2, x3, x4));
        dst.mBottom = (int) Math.ceil (MathUtil.max(y1, y2, y3, y4));
    }
    //@formatter:on

    /**
     * Maps src point array of length count to dst point array of equal or greater
     * length. Points are mapped by multiplying each point by this matrix. Given:
     * <pre>
     *  | A B C |        | x |
     *  | D E F |,  pt = | y |
     *  | G H I |        | 1 |
     * </pre>
     * where
     * <pre>
     *  for (i = 0; i < count; ++i) {
     *      x = src[srcPos + (i << 1)]
     *      y = src[srcPos + (i << 1) + 1]
     *  }
     * </pre>
     * each dst point is computed as:
     * <pre>
     *  |A B C| |x|                               Ax+By+C   Dx+Ey+F
     *  |D E F| |y| = |Ax+By+C Dx+Ey+F Gx+Hy+I| = ------- , -------
     *  |G H I| |1|                               Gx+Hy+I   Gx+Hy+I
     * </pre>
     * <p>
     * src and dst may point to the same array.
     *
     * @param src   points to transform
     * @param dst   array for mapped points
     * @param count number of points to transform
     */
    public void mapPoints(float[] src, int srcPos, float[] dst, int dstPos, int count) {
        int mask = getType();
        if (mask == kIdentity_Mask) {
            if (src != dst && count > 0) {
                System.arraycopy(src, srcPos, dst, dstPos, count << 1);
            }
        } else if (mask <= kTranslate_Mask) {
            mapPoints1(src, srcPos, dst, dstPos, count);
        } else if (mask <= (kTranslate_Mask | kScale_Mask)) {
            mapPoints2(src, srcPos, dst, dstPos, count);
        } else if (mask <= (kTranslate_Mask | kScale_Mask | kAffine_Mask)) {
            mapPoints4(src, srcPos, dst, dstPos, count);
        } else {
            mapPoints8(src, srcPos, dst, dstPos, count);
        }
    }

    //@formatter:off
    private void mapPoints1(float[] src, int srcPos, float[] dst, int dstPos, int count) {
        float tx = m41;
        float ty = m42;
        for (int i = 0; i < count; ++i) {
            dst[dstPos    ] = src[srcPos    ] + tx;
            dst[dstPos + 1] = src[srcPos + 1] + ty;
            srcPos += 2;
            dstPos += 2;
        }
    }

    private void mapPoints2(float[] src, int srcPos, float[] dst, int dstPos, int count) {
        float sx = m11;
        float sy = m22;
        float tx = m41;
        float ty = m42;
        for (int i = 0; i < count; ++i) {
            dst[dstPos    ] = sx * src[srcPos    ] + tx;
            dst[dstPos + 1] = sy * src[srcPos + 1] + ty;
            srcPos += 2;
            dstPos += 2;
        }
    }

    private void mapPoints4(float[] src, int srcPos, float[] dst, int dstPos, int count) {
        float sx = m11;
        float sy = m22;
        float kx = m21;
        float ky = m12;
        float tx = m41;
        float ty = m42;
        for (int i = 0; i < count; ++i) {
            float p0 = src[srcPos    ];
            float p1 = src[srcPos + 1];
            float x = sx * p0 + kx * p1 + tx;
            float y = ky * p0 + sy * p1 + ty;
            dst[dstPos    ] = x;
            dst[dstPos + 1] = y;
            srcPos += 2;
            dstPos += 2;
        }
    }

    private void mapPoints8(float[] src, int srcPos, float[] dst, int dstPos, int count) {
        float m11 = this.m11;
        float m12 = this.m12;
        float m14 = this.m14;
        float m21 = this.m21;
        float m22 = this.m22;
        float m24 = this.m24;
        float m41 = this.m41;
        float m42 = this.m42;
        float m44 = this.m44;
        for (int i = 0; i < count; ++i) {
            float p0 = src[srcPos    ];
            float p1 = src[srcPos + 1];
            float x = m11 * p0 + m21 * p1 + m41;
            float y = m12 * p0 + m22 * p1 + m42;
            float w = m14 * p0 + m24 * p1 + m44;
            if (w != 0) {
                w = 1 / w;
            }
            dst[dstPos    ] = x * w;
            dst[dstPos + 1] = y * w;
            srcPos += 2;
            dstPos += 2;
        }
    }
    //@formatter:on

    /**
     * Returns the minimum scaling factor of this matrix by decomposing the scaling and
     * shearing elements.<br>
     * Returns -1 if scale factor overflows or this matrix contains perspective.
     *
     * @return minimum scale factor
     */
    public float getMinScale() {
        int mask = getType();
        if (mask == kIdentity_Mask) {
            return 1;
        }
        if ((mask & kPerspective_Mask) != 0) {
            return -1;
        }
        if ((mask & kAffine_Mask) == 0) {
            return Math.min(
                    Math.abs(m11),
                    Math.abs(m22)
            );
        }
        // ignore the translation part of the matrix, just look at 2x2 portion.
        // compute singular values, take largest or smallest abs value.
        // [a b; b c] = A^T*A
        float a = m11 * m11 + m12 * m12;
        float b = m11 * m21 + m22 * m12;
        float c = m21 * m21 + m22 * m22;
        // eigenvalues of A^T*A are the squared singular values of A.
        // characteristic equation is det((A^T*A) - l*I) = 0
        // l^2 - (a + c)l + (ac-b^2)
        // solve using quadratic equation (divisor is non-zero since l^2 has 1 coeff
        // and roots are guaranteed to be pos and real).
        float result;
        // if upper left 2x2 is orthogonal save some math
        if (MathUtil.isApproxZero(b)) {
            result = Math.min(a, c);
        } else {
            float amc = a - c;
            float x = (float) (Math.sqrt(amc * amc + 4 * b * b) * 0.5);
            result = (a + c) * 0.5f - x;
        }
        if (!Float.isFinite(result)) {
            return -1;
        }
        // Due to the floating point inaccuracy, there might be an error in a, b, c
        // calculated by dot, further deepened by subsequent arithmetic operations
        // on them. Therefore, we allow and cap the nearly-zero negative values.
        return (float) Math.sqrt(
                Math.abs(result)
        );
    }

    /**
     * Returns the maximum scaling factor of this matrix by decomposing the scaling and
     * shearing elements.<br>
     * Returns -1 if scale factor overflows or this matrix contains perspective.
     *
     * @return maximum scale factor
     */
    public float getMaxScale() {
        // similar to getMinScale()
        int mask = getType();
        if (mask == kIdentity_Mask) {
            return 1;
        }
        if ((mask & kPerspective_Mask) != 0) {
            return -1;
        }
        if ((mask & kAffine_Mask) == 0) {
            return Math.max(
                    Math.abs(m11),
                    Math.abs(m22)
            );
        }
        float a = m11 * m11 + m12 * m12;
        float b = m11 * m21 + m22 * m12;
        float c = m21 * m21 + m22 * m22;
        float result;
        if (MathUtil.isApproxZero(b)) {
            result = Math.max(a, c);
        } else {
            float amc = a - c;
            float x = (float) (Math.sqrt(amc * amc + 4 * b * b) * 0.5);
            result = (a + c) * 0.5f + x;
        }
        if (!Float.isFinite(result)) {
            return -1;
        }
        return (float) Math.sqrt(
                Math.abs(result)
        );
    }

    private static float computeMinScale(double m11, double m21, double m12, double m22) {
        double s1 = m11 * m11 + m21 * m21 + m12 * m12 + m22 * m22;

        double e = m11 * m11 + m21 * m21 - m12 * m12 - m22 * m22;
        double f = m11 * m12 + m21 * m22;
        double s2 = Math.sqrt(e * e + 4 * f * f);

        return (float) Math.sqrt(0.5 * (s1 - s2));
    }

    private static float computeMaxScale(double m11, double m21, double m12, double m22) {
        double s1 = m11 * m11 + m21 * m21 + m12 * m12 + m22 * m22;

        double e = m11 * m11 + m21 * m21 - m12 * m12 - m22 * m22;
        double f = m11 * m12 + m21 * m22;
        double s2 = Math.sqrt(e * e + 4 * f * f);

        return (float) Math.sqrt(0.5 * (s1 + s2));
    }

    private float computeMinScale(double px, double py) {
        double x = m11 * px + m21 * py + m41;
        double y = m12 * px + m22 * py + m42;
        double w = m14 * px + m24 * py + m44;

        double dxdu = m11;
        double dxdv = m21;
        double dydu = m12;
        double dydv = m22;
        double dwdu = m14;
        double dwdv = m24;

        double invW2 = 1.0 / (w * w);
        // non-persp has invW2 = 1, w = 1, dwdu = 0, dwdv = 0
        double dfdu = (w * dxdu - x * dwdu) * invW2; // non-persp -> dxdu -> m11
        double dfdv = (w * dxdv - x * dwdv) * invW2; // non-persp -> dxdv -> m21
        double dgdu = (w * dydu - y * dwdu) * invW2; // non-persp -> dydu -> m12
        double dgdv = (w * dydv - y * dwdv) * invW2; // non-persp -> dydv -> m22

        return computeMinScale(dfdu, dfdv, dgdu, dgdv);
    }

    /**
     * Returns the minimum scaling factor of this matrix by decomposing the scaling and
     * shearing elements. When this matrix has perspective, the scaling factor is specific
     * to the given point <var>p</var>.<br>
     * Returns -1 if scale factor overflows.
     *
     * @param px the x-coord of point
     * @param py the y-coord of point
     * @return minimum scale factor
     */
    public float getMinScale(float px, float py) {
        if (!hasPerspective()) {
            return getMinScale();
        }
        return computeMinScale(px, py);
    }

    /**
     * Returns the maximum scaling factor of this matrix by decomposing the scaling and
     * shearing elements. When this matrix has perspective, the scaling factor is specific
     * to the given point <var>p</var>.<br>
     * Returns -1 if scale factor overflows.
     *
     * @param px the x-coord of point
     * @param py the y-coord of point
     * @return maximum scale factor
     */
    public float getMaxScale(float px, float py) {
        if (!hasPerspective()) {
            return getMaxScale();
        }

        double x = m11 * px + m21 * py + m41;
        double y = m12 * px + m22 * py + m42;
        double w = m14 * px + m24 * py + m44;

        double dxdu = m11;
        double dxdv = m21;
        double dydu = m12;
        double dydv = m22;
        double dwdu = m14;
        double dwdv = m24;

        double invW2 = 1.0 / (w * w);
        // non-persp has invW2 = 1, w = 1, dwdu = 0, dwdv = 0
        double dfdu = (w * dxdu - x * dwdu) * invW2; // non-persp -> dxdu -> m11
        double dfdv = (w * dxdv - x * dwdv) * invW2; // non-persp -> dxdv -> m21
        double dgdu = (w * dydu - y * dwdu) * invW2; // non-persp -> dydu -> m12
        double dgdv = (w * dydv - y * dwdv) * invW2; // non-persp -> dydv -> m22

        return computeMaxScale(dfdu, dfdv, dgdu, dgdv);
    }

    /**
     * Returns the differential area scale factor for a local point 'p' that will be transformed
     * by 'm' (which may have perspective). If 'm' does not have perspective, this scale factor is
     * constant regardless of 'p'; when it does have perspective, it is specific to that point.
     * <p>
     * This can be crudely thought of as "device pixel area" / "local pixel area" at 'p'.
     * <p>
     * Returns positive infinity if the transformed homogeneous point has w <= 0.
     * <p>
     * The return value is equivalent to {@link #getMinScale(float, float)} times
     * {@link #getMaxScale(float, float)}.
     *
     * @param px the x-coord of point
     * @param py the y-coord of point
     */
    public float differentialAreaScale(float px, float py) {
        //              [m11 m21 m41]                                 [f(u,v)]
        // Assuming M = [m12 m22 m42], define the projected p'(u,v) = [g(u,v)] where
        //              [m14 m24 m44]
        //                                                        [x]     [u]
        // f(u,v) = x(u,v) / w(u,v), g(u,v) = y(u,v) / w(u,v) and [y] = M*[v]
        //                                                        [w]     [1]
        //
        // Then the differential scale factor between p = (u,v) and p' is |det J|,
        // where J is the Jacobian for p': [df/du dg/du]
        //                                 [df/dv dg/dv]
        // and df/du = (w*dx/du - x*dw/du)/w^2,   dg/du = (w*dy/du - y*dw/du)/w^2
        //     df/dv = (w*dx/dv - x*dw/dv)/w^2,   dg/dv = (w*dy/dv - y*dw/dv)/w^2
        //
        // From here, |det J| can be rewritten as |det J'/w^3|, where
        //      [x     y     w    ]   [x   y   w  ]
        // J' = [dx/du dy/du dw/du] = [m11 m12 m14]
        //      [dx/dv dy/dv dw/dv]   [m21 m22 m24]
        double x = (double) m11 * px + (double) m21 * py + (double) m41;
        double y = (double) m12 * px + (double) m22 * py + (double) m42;
        double w = (double) m14 * px + (double) m24 * py + (double) m44;

        if (w <= MathUtil.EPS) {
            // Reaching the discontinuity of xy/w and where the point would clip to w >= 0
            return Float.POSITIVE_INFINITY;
        }

        double dxdu = m11;
        double dxdv = m21;
        double dydu = m12;
        double dydv = m22;
        double dwdu = m14;
        double dwdv = m24;

        double detJ = x * (dydu * dwdv - dwdu * dydv) +
                y * (dwdu * dxdv - dxdu * dwdv) +
                w * (dxdu * dydv - dydu * dxdv);
        double denom = 1.0 / w;
        denom = denom * denom * denom;  // 1/w^3
        return (float) (Math.abs(detJ * denom));
    }

    /**
     * Return the minimum distance needed to move in local (pre-transform) space to ensure that the
     * transformed coordinates are at least 1px away from the original mapped point. This minimum
     * distance is specific to the given local 'bounds' since the scale factors change with
     * perspective.
     * <p>
     * If the bounds will be clipped by the w=0 plane or otherwise is ill-conditioned, this will
     * return positive infinity.
     */
    public float localAARadius(Rect2fc bounds) {
        return localAARadius(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    /**
     * Return the minimum distance needed to move in local (pre-transform) space to ensure that the
     * transformed coordinates are at least 1px away from the original mapped point. This minimum
     * distance is specific to the given local 'bounds' since the scale factors change with
     * perspective.
     * <p>
     * If the bounds will be clipped by the w=0 plane or otherwise is ill-conditioned, this will
     * return positive infinity.
     */
    public float localAARadius(float left, float top, float right, float bottom) {
        float min;
        if ((getType() & kPerspective_Mask) == 0) {
            min = getMinScale();
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
     * Sets matrix to scale and translate src rect to dst rect. Returns false if
     * src is empty, and sets matrix to identity. Returns true if dst is empty,
     * and sets matrix to:
     * <pre>
     * | 0 0 0 |
     * | 0 0 0 |
     * | 0 0 1 |
     * </pre>
     *
     * @param src rect to map from
     * @param dst rect to map to
     */
    public void setRectToRect(Rect2fc src, Rect2fc dst) {
        if (src.isEmpty()) {
            setIdentity();
            return;
        }

        if (dst.isEmpty()) {
            m11 = 0.0f;
            m12 = 0.0f;
            m14 = 0.0f;
            m21 = 0.0f;
            m22 = 0.0f;
            m24 = 0.0f;
            m41 = 0.0f;
            m42 = 0.0f;
            m44 = 1.0f;
            mTypeMask = kScale_Mask;
        } else {
            float sx = dst.width() / src.width();
            float sy = dst.height() / src.height();
            float tx = dst.left() - src.left() * sx;
            float ty = dst.top() - src.top() * sy;
            setScaleTranslate(sx, sy, tx, ty);
        }
    }

    /**
     * If the last column of the matrix is [0, 0, not_one]^T, we will treat the matrix as if it
     * is in perspective, even though it stills behaves like its affine. If we divide everything
     * by the not_one value, then it will behave the same, but will be treated as affine,
     * and therefore faster (e.g. clients can forward-difference calculations).
     */
    public void normalizePerspective() {
        if (m44 != 1 && m14 == 0 && m24 == 0) {
            if (m44 != 0) {
                double inv = 1.0 / m44;
                m11 = (float) (m11 * inv);
                m12 = (float) (m12 * inv);
                m21 = (float) (m21 * inv);
                m22 = (float) (m22 * inv);
                m41 = (float) (m41 * inv);
                m42 = (float) (m42 * inv);
                m44 = 1.0f;
            }
            mTypeMask = kUnknown_Mask;
        }
    }

    /**
     * Returns true if all elements of the matrix are finite. Returns false if any
     * element is infinity, or NaN.
     *
     * @return true if matrix has only finite elements
     */
    public boolean isFinite() {
        return MathUtil.isFinite(
                m11, m12, m14,
                m21, m22, m24,
                m41, m42, m44);
    }

    private static int floatTo2sCompliment(float x) {
        int bits = Float.floatToRawIntBits(x);
        if (bits < 0) {
            return -(bits & 0x7FFFFFFF);
        }
        return bits;
    }

    private int computeTypeMask() {
        int mask = 0;

        if (m14 != 0 || m24 != 0 || m44 != 1) {
            // Once it is determined that this is a perspective transform,
            // all other flags are moot as far as optimizations are concerned.
            return kTranslate_Mask |
                    kScale_Mask |
                    kAffine_Mask |
                    kPerspective_Mask;
        }

        if (m41 != 0 || m42 != 0) {
            mask |= kTranslate_Mask;
        }

        int m00 = floatTo2sCompliment(m11);
        int m01 = floatTo2sCompliment(m21);
        int m10 = floatTo2sCompliment(m12);
        int m11 = floatTo2sCompliment(m22);

        if ((m01 | m10) != 0) {
            // The shear components may be scale-inducing, unless we are dealing
            // with a pure rotation.  Testing for a pure rotation is expensive,
            // so we opt for being conservative by always setting the scale bit.
            // along with affine.
            // By doing this, we are also ensuring that matrices have the same
            // type masks as their inverses.
            mask |= kAffine_Mask | kScale_Mask;

            // For axis aligned, in the affine case, we only need check that
            // the primary diagonal is all zeros and that the secondary diagonal
            // is all non-zero.

            // map non-zero to 1
            m01 = m01 != 0 ? 1 : 0;
            m10 = m10 != 0 ? 1 : 0;

            int dp0 = (m00 | m11) == 0 ? 1 : 0; // true if both are 0
            int ds1 = (m01 & m10);              // true if both are 1
            mask |= (dp0 & ds1) << kAxisAligned_Shift;
        } else {
            // Only test for scale explicitly if not affine, since affine sets the
            // scale bit.
            if (((m00 ^ 0x3f800000) | (m11 ^ 0x3f800000)) != 0) {
                mask |= kScale_Mask;
            }

            // Not affine, therefore we already know secondary diagonal is
            // all zeros, so we just need to check that primary diagonal is
            // all non-zero.

            // map non-zero to 1
            m00 = m00 != 0 ? 1 : 0;
            m11 = m11 != 0 ? 1 : 0;

            // record if the (p)rimary diagonal is all non-zero
            mask |= (m00 & m11) << kAxisAligned_Shift;
        }

        return mask;
    }

    private int computePerspectiveTypeMask() {
        if (m14 != 0 || m24 != 0 || m44 != 1) {
            // If this is a perspective transform, we return true for all other
            // transform flags - this does not disable any optimizations, respects
            // the rule that the type mask must be conservative, and speeds up
            // type mask computation.
            return kTranslate_Mask |
                    kScale_Mask |
                    kAffine_Mask |
                    kPerspective_Mask;
        }

        return kOnlyPerspectiveValid_Mask | kUnknown_Mask;
    }

    /**
     * Returns whether this matrix elements are equivalent to given matrix.
     *
     * @param m the matrix to compare.
     * @return {@code true} if this matrix is equivalent to other matrix.
     */
    public boolean isApproxEqual(@NonNull Matrix m) {
        return MathUtil.isApproxEqual(m11, m.m11) &&
                MathUtil.isApproxEqual(m12, m.m12) &&
                MathUtil.isApproxEqual(m14, m.m14) &&
                MathUtil.isApproxEqual(m21, m.m21) &&
                MathUtil.isApproxEqual(m22, m.m22) &&
                MathUtil.isApproxEqual(m24, m.m24) &&
                MathUtil.isApproxEqual(m41, m.m41) &&
                MathUtil.isApproxEqual(m42, m.m42) &&
                MathUtil.isApproxEqual(m44, m.m44);
    }

    /**
     * Returns whether two matrices' elements are equal, using
     * <code>==</code> operator. Note <code>-0.0f == 0.0f</code> is true.
     * {@link #getType()} is ignored.
     * <p>
     * Keep consistent with {@link #equals(Object)}.
     */
    public static boolean equals(@NonNull Matrixc a, @NonNull Matrixc b) {
        return a.m11() == b.m11() &&
                a.m12() == b.m12() &&
                a.m14() == b.m14() &&
                a.m21() == b.m21() &&
                a.m22() == b.m22() &&
                a.m24() == b.m24() &&
                a.m41() == b.m41() &&
                a.m42() == b.m42() &&
                a.m44() == b.m44();
    }

    @Override
    public int hashCode() {
        int result = (m11 != 0.0f ? Float.floatToIntBits(m11) : 0);
        result = 31 * result + (m12 != 0.0f ? Float.floatToIntBits(m12) : 0);
        result = 31 * result + (m14 != 0.0f ? Float.floatToIntBits(m14) : 0);
        result = 31 * result + (m21 != 0.0f ? Float.floatToIntBits(m21) : 0);
        result = 31 * result + (m22 != 0.0f ? Float.floatToIntBits(m22) : 0);
        result = 31 * result + (m24 != 0.0f ? Float.floatToIntBits(m24) : 0);
        result = 31 * result + (m41 != 0.0f ? Float.floatToIntBits(m41) : 0);
        result = 31 * result + (m42 != 0.0f ? Float.floatToIntBits(m42) : 0);
        result = 31 * result + (m44 != 0.0f ? Float.floatToIntBits(m44) : 0);
        return result;
    }

    /**
     * Returns whether this matrix elements are equal to another matrix, using
     * <code>==</code> operator. Note <code>-0.0f == 0.0f</code> is true.
     * {@link #getType()} is ignored.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the o values.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Matrixc m)) {
            return false;
        }
        return Matrix.equals(this, m);
    }

    @Override
    public String toString() {
        return String.format("""
                        Matrix:
                        %10.6f %10.6f %10.6f
                        %10.6f %10.6f %10.6f
                        %10.6f %10.6f %10.6f""",
                m11, m12, m14,
                m21, m22, m24,
                m41, m42, m44);
    }

    /**
     * @return a copy of this matrix
     */
    @Override
    public @NonNull Matrix clone() {
        try {
            return (Matrix) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
