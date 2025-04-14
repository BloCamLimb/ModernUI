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

package icyllis.arc3d.test;

import icyllis.arc3d.sketch.GeometryUtils;
import icyllis.arc3d.core.MathUtil;

import java.util.Arrays;

public class TestConicToQuads {

    public static void main(String[] args) {
        float radius = 400;
        float x0 = 0;
        float y0 = 0;
        float x1 = radius;
        float y1 = 0;
        float x2 = radius;
        float y2 = radius;
        int level = GeometryUtils.computeConicToQuadsLevel(
                x0, y0, x1, y1, x2, y2, MathUtil.INV_SQRT2, 0.25f
        );
        System.out.println("Level: " + level);
        float[] dst = new float[4 * (1 << level) + 2];
        int quads = GeometryUtils.computeConicToQuads(
                x0, y0, x1, y1, x2, y2, MathUtil.INV_SQRT2, dst, 0, level
        );
        System.out.println("Quads: " + quads);
        System.out.println(Arrays.toString(dst));
        System.out.println(computeRadiusForMaxLevel());
    }

    // 4220.2324
    public static float computeRadiusForMaxLevel() {
        float low = 1, high = 6000;
        while (low <= high) {
            float mid = (low + high) * 0.5f;
            int level = GeometryUtils.computeConicToQuadsLevel(
                    0, 0, mid, 0, mid, mid, MathUtil.INV_SQRT2, 1
            );
            if (level < GeometryUtils.MAX_CONIC_TO_QUADS_LEVEL)
                low = mid + Math.ulp(mid);
            else
                high = mid - Math.ulp(mid);
        }
        return low;
    }
}
