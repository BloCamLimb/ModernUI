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

package icyllis.arc3d.core.effects;

import icyllis.arc3d.core.*;

import javax.annotation.Nullable;

public class ColorMatrixColorFilter extends ColorFilter {

    private final float[] mMatrix = new float[20];
    private final boolean mAlphaUnchanged;

    public ColorMatrixColorFilter(@Size(20) float[] matrix) {
        mAlphaUnchanged = MathUtil.isApproxZero(
                matrix[3], matrix[7], matrix[11], matrix[19]
        ) && MathUtil.isApproxEqual(matrix[15], 1);
        System.arraycopy(matrix, 0, mMatrix, 0, 20);
    }

    @Nullable
    public static ColorFilter make(@Size(20) float[] matrix) {
        float prod = 0;
        for (int i = 0; i < 20; ++i) {
            prod *= matrix[i];
        }
        if (prod != 0) {
            // NaN or infinite
            return null;
        }
        return new ColorMatrixColorFilter(matrix);
    }

    public float[] getMatrix() {
        return mMatrix;
    }

    @Override
    public boolean isAlphaUnchanged() {
        return mAlphaUnchanged;
    }

    @Override
    public void filterColor4f(float[] col, float[] out) {
        float a = 1.0f / col[3];
        if (!Float.isFinite(a)) { // NaN or Inf
            a = 0;
        }
        // unpremul, multiply and clamp 01
        float[] m = mMatrix;
        final float x = MathUtil.clamp(
                m[0] * col[0] * a + m[4] * col[1] * a + m[8] * col[2] * a + m[12] * col[3] + m[16], 0, 1);
        final float y = MathUtil.clamp(
                m[1] * col[0] * a + m[5] * col[1] * a + m[9] * col[2] * a + m[13] * col[3] + m[17], 0, 1);
        final float z = MathUtil.clamp(
                m[2] * col[0] * a + m[6] * col[1] * a + m[10] * col[2] * a + m[14] * col[3] + m[18], 0, 1);
        final float w = MathUtil.clamp(
                m[3] * col[0] * a + m[7] * col[1] * a + m[11] * col[2] * a + m[15] * col[3] + m[19], 0, 1);
        out[0] = x * w;
        out[1] = y * w;
        out[2] = z * w;
        out[3] = w;
    }
}
