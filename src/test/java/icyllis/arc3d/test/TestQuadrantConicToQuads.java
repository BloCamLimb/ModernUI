/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.test;

import icyllis.arc3d.core.Geometry;

import java.util.Arrays;

public class TestQuadrantConicToQuads {

    public static void main(String[] args) {
        float x0 = 0;
        float y0 = 0;
        float x1 = 5;
        float y1 = 0;
        float x2 = 5;
        float y2 = 5;
        float[] dst = new float[10];
        Geometry.subdivideQuadrantConicToQuads(x0, y0, x1, y1, x2, y2, dst, 0);
        System.out.println(Arrays.toString(dst));
    }
}
