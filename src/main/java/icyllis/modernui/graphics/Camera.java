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

package icyllis.modernui.graphics;

import icyllis.modernui.gui.math.Matrix4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Camera {

    private List<Matrix4f> matrix4fs = new ArrayList<>();

    public Camera(Matrix4f matrix4f) {
        this.matrix4fs.add(matrix4f);
    }

    public Matrix4f getMatrix() {
        int i = matrix4fs.size() - 1;
        return matrix4fs.get(i);
    }

    public void push() {
        matrix4fs.add(getMatrix());
    }

    public void pop() {
        int i = matrix4fs.size() - 1;
        matrix4fs.remove(i);
    }

    public void translate(float x, float y, float z) {

    }


    @Nonnull
    public static Camera getCameraFromGL() {
        return new Camera(Matrix4f.getMVPMatrixFromGL());
    }
}
