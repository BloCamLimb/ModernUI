/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.ColorSpace;

public class TestColorSpace {

    public static void main(String[] args) {
        var cs = (ColorSpace.Rgb) ColorSpace.get(ColorSpace.Named.SRGB);
        float[] v = {0.4f, 0.8f, 0.7f};
        {
            float[] linear = v.clone();
            Color.GammaToLinear(linear);
            float lum = Color.luminance(linear);
            System.out.println(lum);
            System.out.println(Color.LinearToGamma(lum));
        }
        {
            float lum = cs.toXyz(v)[1];
            System.out.println(lum);
            System.out.println(cs.fromLinear(lum, lum, lum)[0]);
        }
        {
            float lum = 0.299f * v[0] + 0.587f * v[1] + 0.114f * v[2];
            // ????
            System.out.println(lum);
        }
    }
}
