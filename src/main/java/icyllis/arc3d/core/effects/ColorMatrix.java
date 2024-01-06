/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core.effects;

import icyllis.arc3d.core.Size;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * This class represents a 5x4 affine matrix that transforms 4-component colors.
 * The memory layout (order of components) is the same as GLSL's column-major.
 * Interpret first 16 elements as mat4 m and last 4 elements as vec4 v, then
 * <code>newColor = m * color + v;</code>
 */
public class ColorMatrix {

    private static final int kScaleR = 0;
    private static final int kScaleG = 5;
    private static final int kScaleB = 10;
    private static final int kScaleA = 15;

    private static final int kTransR = 16;
    private static final int kTransG = 17;
    private static final int kTransB = 18;
    private static final int kTransA = 19;

    //   R   G   B   A
    // [m11 m12 m13 m14]
    // [m21 m22 m23 m24]
    // [m31 m32 m33 m34]
    // [m41 m42 m43 m44]
    // [m51 m52 m53 m54] <- [m51 m52 m53 m54] represents the origin
    private final float[] mMat = new float[20];

    /**
     * Create an identity matrix.
     */
    public ColorMatrix() {
        mMat[kScaleR] = 1;
        mMat[kScaleG] = 1;
        mMat[kScaleB] = 1;
        mMat[kScaleA] = 1;
    }

    /**
     * Create a matrix from an array of elements in row-major.
     *
     * @param src the array to create from
     * @see #set(float[])
     */
    public ColorMatrix(@Size(20) float... src) {
        set(src);
    }

    /**
     * Create a matrix copied from an existing matrix.
     *
     * @param src the matrix to create from
     */
    public ColorMatrix(@Nonnull ColorMatrix src) {
        set(src);
    }

    /**
     * Reset this matrix to the identity.
     */
    public void setIdentity() {
        float[] mat = mMat;
        for (int i = 0; i < 20; i += 5) {
            mat[i] = 1;
            mat[i + 1] = 0;
            mat[i + 2] = 0;
            mat[i + 3] = 0;
            mat[i + 4] = 0;
        }
    }

    /**
     * Store the values of the given matrix into this matrix.
     *
     * @param src the matrix to copy from
     */
    public void set(@Nonnull ColorMatrix src) {
        set(src.mMat, 0);
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param src the array to copy from
     */
    public void set(@Size(20) float[] src) {
        set(src, 0);
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param src    the array to copy from
     * @param offset the element offset
     */
    public void set(@Size(20) float[] src, int offset) {
        System.arraycopy(src, offset, mMat, 0, 20);
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param src the array to copy from
     */
    public void set(@Nonnull ByteBuffer src) {
        int offset = src.position();
        for (int i = 0; i < 20; i++) {
            mMat[i] = src.getFloat(offset);
            offset += 4;
        }
    }

    /**
     * Set the values in the matrix using a float array that contains
     * the matrix elements in row-major order.
     *
     * @param src the array to copy from
     */
    public void set(@Nonnull FloatBuffer src) {
        src.get(src.position(), mMat);
    }

    /**
     * Store this matrix elements to the given matrix.
     *
     * @param dst the matrix to store
     */
    public void store(@Nonnull ColorMatrix dst) {
        store(dst.mMat, 0);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param dst the array to store into
     */
    public void store(@Size(20) float[] dst) {
        store(dst, 0);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param dst    the array to store into
     * @param offset the element offset
     */
    public void store(@Size(20) float[] dst, int offset) {
        System.arraycopy(mMat, 0, dst, offset, 20);
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param dst the pointer of the array to store
     */
    public void store(@Nonnull ByteBuffer dst) {
        int offset = dst.position();
        for (int i = 0; i < 20; i++) {
            dst.putFloat(offset, mMat[i]);
            offset += 4;
        }
    }

    /**
     * Store this matrix into the give float array in row-major order.
     *
     * @param dst the pointer of the array to store
     */
    public void store(@Nonnull FloatBuffer dst) {
        dst.put(dst.position(), mMat);
    }

    /**
     * Set this matrix to scale by the specified values.
     */
    public void setScale(float scaleR, float scaleG, float scaleB, float scaleA) {
        Arrays.fill(mMat, 0);
        mMat[kScaleR] = scaleR;
        mMat[kScaleG] = scaleG;
        mMat[kScaleB] = scaleB;
        mMat[kScaleA] = scaleA;
    }

    /**
     * Set the rotation around the red color axis by the specified radians.
     *
     * @param angle the rotation angle in radians
     */
    public void setRotateR(float angle) {
        setIdentity();
        if (angle != 0) {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            mMat[kScaleG] = mMat[kScaleB] = c;
            mMat[6] = s;
            mMat[9] = -s;
        }
    }

    /**
     * Set the rotation around the green color axis by the specified radians.
     *
     * @param angle the rotation angle in radians
     */
    public void setRotateG(float angle) {
        setIdentity();
        if (angle != 0) {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            mMat[kScaleR] = mMat[kScaleB] = c;
            mMat[2] = -s;
            mMat[8] = s;
        }
    }

    /**
     * Set the rotation around the blue color axis by the specified radians.
     *
     * @param angle the rotation angle in radians
     */
    public void setRotateB(float angle) {
        setIdentity();
        if (angle != 0) {
            float s = (float) Math.sin(angle);
            float c = (float) Math.cos(angle);
            mMat[kScaleR] = mMat[kScaleG] = c;
            mMat[1] = s;
            mMat[4] = -s;
        }
    }

    public void setTranslate(float transR, float transG, float transB, float transA) {
        float[] mat = mMat;
        for (int i = 0; i < 15; i += 5) {
            mat[i] = 1;
            mat[i + 1] = 0;
            mat[i + 2] = 0;
            mat[i + 3] = 0;
            mat[i + 4] = 0;
        }
        mat[kScaleA] = 1;
        mat[kTransR] = transR;
        mat[kTransG] = transG;
        mat[kTransB] = transB;
        mat[kTransA] = transA;
    }

    public void preConcat(@Size(20) float[] lhs) {
        set_concat(mMat, lhs, mMat);
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
    public void preConcat(@Nonnull ColorMatrix lhs) {
        set_concat(mMat, lhs.mMat, mMat);
    }

    public void postConcat(@Size(20) float[] rhs) {
        set_concat(mMat, mMat, rhs);
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
    public void postConcat(@Nonnull ColorMatrix rhs) {
        set_concat(mMat, mMat, rhs.mMat);
    }

    public void setConcat(@Size(20) float[] lhs, @Size(20) float[] rhs) {
        set_concat(mMat, lhs, rhs);
    }

    /**
     * Set this matrix to the concatenation of the two specified matrices,
     * such that the resulting matrix has the same effect as applying
     * <code>lhs</code> first and then applying <code>rhs</code>.
     * <p>
     * It is legal for either <code>lhs</code> or <code>rhs</code> to be the
     * same matrix as <code>this</code>.
     * </p>
     */
    public void setConcat(@Nonnull ColorMatrix lhs, @Nonnull ColorMatrix rhs) {
        set_concat(mMat, lhs.mMat, rhs.mMat);
    }

    private static void set_concat(@Size(20) float[] result, @Size(20) float[] lhs, @Size(20) float[] rhs) {
        float[] target;
        if (lhs == result || rhs == result) {
            target = new float[20];
        } else {
            target = result;
        }

        for (int i = 0; i < 16; i += 4) {
            for (int j = 0; j < 4; j++) {
                target[i + j] = lhs[i] * rhs[j] +
                        lhs[i + 1] * rhs[j + 4] +
                        lhs[i + 2] * rhs[j + 8] +
                        lhs[i + 3] * rhs[j + 12];
            }
        }
        for (int j = 0; j < 4; j++) {
            target[16 + j] = lhs[16] * rhs[j] +
                    lhs[17] * rhs[j + 4] +
                    lhs[18] * rhs[j + 8] +
                    lhs[19] * rhs[j + 12] +
                    rhs[j + 16];
        }

        if (target != result) {
            System.arraycopy(target, 0, result, 0, 20);
        }
    }

    /**
     * Set the matrix to affect the saturation of colors.
     *
     * @param sat A value of 0 maps the color to gray-scale. 1 is identity.
     */
    public void setSaturation(float sat) {
        float[] m = mMat;
        Arrays.fill(m, 0);

        final float R = 0.213f * (1 - sat);
        final float G = 0.715f * (1 - sat);
        final float B = 0.072f * (1 - sat);

        m[0] = R + sat; m[1] = R;       m[2] = R;
        m[4] = G;       m[5] = G + sat; m[6] = G;
        m[8] = B;       m[9] = B;       m[10] = B + sat;
        m[kScaleA] = 1;
    }

    /**
     * @return the backing array
     */
    public float[] elements() {
        return mMat;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mMat);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColorMatrix m)) {
            return false;
        }
        // we don't use Arrays.equals(), since that considers NaN == NaN
        for (int i = 0; i < 20; i++) {
            if (mMat[i] != m.mMat[i]) {
                return false;
            }
        }
        return true;
    }
}
