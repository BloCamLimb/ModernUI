/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.*;
import org.lwjgl.system.MemoryUtil;

import java.util.Random;

public class TestLowpPixelLoad {

    public static void main(String[] args) {
        int width = 64, height = 64;
        var info = ImageInfo.make(width, height, ColorInfo.CT_BGR_565, ColorInfo.AT_UNPREMUL, null);
        var pixels = MemoryUtil.nmemAlloc(info.computeMinByteSize());
        var random = new Random();
        for (int i = 0, e = (int) info.computeMinByteSize(); i < e; i += 2) {
            MemoryUtil.memPutShort(pixels + i, (short)random.nextInt(65536));
        }
        Pixmap originalPixmap = new Pixmap(
                info,
                null,
                pixels,
                info.minRowBytes()
        );
        int[] colorTypes = {ColorInfo.CT_R_8, ColorInfo.CT_RG_88, ColorInfo.CT_RGB_888, ColorInfo.CT_RGBX_8888,
                ColorInfo.CT_RGBA_8888, ColorInfo.CT_BGRA_8888, ColorInfo.CT_ABGR_8888, ColorInfo.CT_ARGB_8888,
                ColorInfo.CT_GRAY_8, ColorInfo.CT_GRAY_ALPHA_88, ColorInfo.CT_ALPHA_8,
                ColorInfo.CT_R_16, ColorInfo.CT_RG_1616, ColorInfo.CT_RGBA_16161616, ColorInfo.CT_ALPHA_16,
                ColorInfo.CT_R_F16, ColorInfo.CT_RG_F16, ColorInfo.CT_RGBA_F16, ColorInfo.CT_ALPHA_F16,
                ColorInfo.CT_RGBA_F32, ColorInfo.CT_BGR_565, ColorInfo.CT_RGBA_1010102, ColorInfo.CT_BGRA_1010102};
        for (int ct : colorTypes) {
            testForColorType(ct, originalPixmap);
        }
        MemoryUtil.nmemFree(pixels);

        for (int i = 0; i < 64; i++) {
            int a = (int) (i * (63/255.0f) + .5f);
            int b = (i * 21 + 42) / 85;
            if (a != b) {
                throw new IllegalStateException();
            }
        }
    }

    public static void testForColorType(int ct, Pixmap originalPixmap) {
        var newInfo = originalPixmap.getInfo().makeColorType(ct);
        var newPixels = MemoryUtil.nmemAlloc(newInfo.computeMinByteSize());
        Pixmap convertedPixmap = new Pixmap(
                newInfo, null, newPixels, newInfo.minRowBytes()
        );
        boolean res = PixelUtils.convertPixels(originalPixmap, convertedPixmap);
        assert res;

        PixelUtils.PixelOp load = PixelUtils.loadOp(ct);
        float[] color4f = new float[4];
        for (int y = newInfo.height() - 1; y >= 0; y--) {
            for (int x = newInfo.width() - 1; x >= 0; x--) {
                int colA = convertedPixmap.getColor(x, y);
                load.op(convertedPixmap.getBase(), convertedPixmap.getAddress(x, y), color4f);
                int colB = Color.argb(color4f[3], color4f[0], color4f[1], color4f[2]);
                if (colA != colB) {
                    throw new IllegalStateException(
                            String.format("ct: %s, got: %X, expected: %X", ColorInfo.colorTypeToString(ct), colA, colB)
                    );
                }
            }
        }

        MemoryUtil.nmemFree(newPixels);
    }
}
