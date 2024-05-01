/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.test;

import icyllis.arc3d.core.*;

public class TestMatrixAAOutset {

    public static void main(String[] args) {
        Matrix4 matrix = Matrix4.identity();
        matrix.m34 = 1 / 576f;
        matrix.preRotateX(Math.PI / 6);
        Rect2f rect = new Rect2f(-10, -50, 10, 50);
        float aaRadius = matrix.localAARadius(rect);
        System.out.printf("aaRad: %f%n", aaRadius);
    }
}
