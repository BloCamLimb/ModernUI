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

package icyllis.modernui.graphics.math;

import icyllis.modernui.math.Matrix4;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class Camera {

    private List<Matrix4> mMatrix4s = new ArrayList<>();

    public Camera(Matrix4 matrix4) {
        this.mMatrix4s.add(matrix4);
    }

    public Matrix4 getMatrix() {
        int i = mMatrix4s.size() - 1;
        return mMatrix4s.get(i);
    }

    public void push() {
        mMatrix4s.add(getMatrix());
    }

    public void pop() {
        int i = mMatrix4s.size() - 1;
        mMatrix4s.remove(i);
    }

    public void translate(float x, float y, float z) {

    }


    @Nonnull
    public static Camera getCameraFromGL() {
        return new Camera(Matrix4.getMVPMatrixFromGL());
    }
}
