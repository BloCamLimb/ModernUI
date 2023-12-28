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

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.core.Color;

import java.util.Arrays;

public class TestBlendMode {

    // blend results (RGB) from Adobe Photoshop
    // layer2 (src) ( 33, 150, 243)  opacity 80%   %mode
    // layer1 (dst) (233,  30,  99)  opacity 100%  normal (src_over)
    public static final int[] PHOTOSHOP_RESULTS = getPhotoshopResults();

    private static int[] getPhotoshopResults() {
        var m = new int[BlendMode.COUNT];
        Arrays.fill(m, -1);
        m[BlendMode.SRC_OVER.ordinal()] = 0x497ed6; // normal src_over
        m[BlendMode.DARKEN.ordinal()] = 0x491e63;
        m[BlendMode.MULTIPLY.ordinal()] = 0x47145f;
        m[BlendMode.COLOR_BURN.ordinal()] = 0x73065d;
        m[BlendMode.LINEAR_BURN.ordinal()] = 0x370659;
        m[BlendMode.LIGHTEN.ordinal()] = 0xe97ed6;
        m[BlendMode.SCREEN.ordinal()] = 0xeb88da;
        m[BlendMode.COLOR_DODGE.ordinal()] = 0xfb40e0;
        m[BlendMode.LINEAR_DODGE.ordinal()] = 0xfb96e0;
        m[BlendMode.OVERLAY.ordinal()] = 0xdc22ab;
        m[BlendMode.SOFT_LIGHT.ordinal()] = 0xdd258e;
        m[BlendMode.HARD_LIGHT.ordinal()] = 0x5f3dd4;
        m[BlendMode.VIVID_LIGHT.ordinal()] = 0xb723e0;
        m[BlendMode.LINEAR_LIGHT.ordinal()] = 0x5141e0;
        m[BlendMode.PIN_LIGHT.ordinal()] = 0x6329cd;
        m[BlendMode.HARD_MIX.ordinal()] = 0xfb06e0;
        m[BlendMode.DIFFERENCE.ordinal()] = 0xcf6687;
        m[BlendMode.EXCLUSION.ordinal()] = 0xd3798f;
        m[BlendMode.HUE.ordinal()] = 0x3668bd;
        m[BlendMode.SATURATION.ordinal()] = 0xec1c62;
        m[BlendMode.COLOR.ordinal()] = 0x3368c1;
        m[BlendMode.LUMINOSITY.ordinal()] = 0xfb3779;
        // missing: darker color, lighter color, subtract, divide
        return m;
    }

    public static void main(String[] args) {
        int src = Color.argb(204, 33, 150, 243);
        int dst = Color.argb(255, 233, 30, 99);
        for (int i = 0; i < BlendMode.COUNT; i++) {
            blend(BlendMode.mode(i), src, dst);
        }
    }

    public static void blend(BlendMode mode, int src, int dst) {
        int result = Color.blend(mode, src, dst);
        int a = Color.alpha(result);
        int Ra = Color.red(result), Ga = Color.green(result), Ba = Color.blue(result);
        System.out.printf("AG %s (%d, %d, %d, %d)\n", mode,
                a, Ra, Ga, Ba);
        result = PHOTOSHOP_RESULTS[mode.ordinal()];
        if (result != -1) {
            assert a == 255;
            int Rb = Color.red(result), Gb = Color.green(result), Bb = Color.blue(result);
            // Photoshop has errors
            if (Math.abs(Ra - Rb) > 1 || Math.abs(Ga - Gb) > 1 || Math.abs(Ba - Bb) > 1) {
                System.err.printf("PS %s mismatch (%d, %d, %d)\n", mode,
                        Rb, Gb, Bb);
            } else {
                System.out.printf("PS %s (%d, %d, %d, %d)\n", mode,
                        a, Rb, Gb, Bb);
            }
        }
    }
}
