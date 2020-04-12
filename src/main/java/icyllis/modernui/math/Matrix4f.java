/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.math;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nonnull;

/**
 * 4x4 float matrix
 */
public class Matrix4f {

    private float[] data = new float[16];

    public Matrix4f() {

    }

    public Matrix4f(float[] data) {
        this.data = data;
    }

    public void set(int x, int y, float f) {
        data[x + (y << 2)] = f;
    }

    public float get(int x, int y) {
        return data[x + (y << 2)];
    }

    public float[] getData() {
        return data;
    }

    public void translate(float x, float y, float z) {
        float[] v = new float[]{x, y, z};
        for (int i = 0; i < 3; i++) {
            set(3, i, get(3, i) + v[i]);
        }
    }

    public void multiply(Matrix4f matrix4f) {
        float[] data = new float[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    data[j + (i << 2)] += get(i, k) * matrix4f.get(k, j);
                }
            }
        }
        this.data = data;
    }

    @Nonnull
    public static Matrix4f getMVPMatrixFromGL() {
        float[] mv = new float[16];
        float[] pj = new float[16];
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, mv);
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, pj);
        Matrix4f matrix4f = new Matrix4f(mv);
        matrix4f.multiply(new Matrix4f(pj));
        return matrix4f;
    }
}
