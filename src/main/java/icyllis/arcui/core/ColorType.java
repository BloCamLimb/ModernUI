/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.core;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Color types.
 * <p>
 * Describes a layout of pixel data in CPU memory. A pixel may be an alpha mask, a grayscale,
 * RGB, or ARGB. It specifies the channels, their type, and width. It does not refer to a texture
 * format and the mapping to texture formats may be many-to-many. It does not specify the sRGB
 * encoding of the stored values. The components are listed in order of where they appear in
 * memory. In other words the first component listed is in the low bits and the last component in
 * the high bits.
 */
@MagicConstant(intValues = {
        ColorType.UNKNOWN,
        ColorType.ALPHA_8,
        ColorType.BGR_565,
        ColorType.ABGR_4444,
        ColorType.RGBA_8888,
        ColorType.RGB_888x,
        ColorType.RG_88,
        ColorType.BGRA_8888,
        ColorType.RGBA_1010102,
        ColorType.BGRA_1010102,
        ColorType.GRAY_8,
        ColorType.ALPHA_F16,
        ColorType.RGBA_F16,
        ColorType.RGBA_F16_CLAMPED,
        ColorType.RGBA_F32,
        ColorType.ALPHA_16,
        ColorType.RG_1616,
        ColorType.RG_F16,
        ColorType.RGBA_16161616,
        ColorType.RGB_565,
        ColorType.RGBA_F16_NORM,
        ColorType.R8G8_UNORM,
        ColorType.A16_UNORM,
        ColorType.A16G16_UNORM,
        ColorType.A16_FLOAT,
        ColorType.R16G16_FLOAT,
        ColorType.R16G16B16A16_UNORM
})
@Retention(RetentionPolicy.SOURCE)
public @interface ColorType {

    /**
     * Public API values.
     */
    int
            UNKNOWN = 0,          // uninitialized
            ALPHA_8 = 1,          // pixel with alpha in 8-bit byte
            BGR_565 = 2,          // pixel with 5 bits red, 6 bits green, 5 bits blue, in 16-bit word
            ABGR_4444 = 3,        // pixel with 4 bits for alpha, blue, red, green; in 16-bit word
            RGBA_8888 = 4,        // pixel with 8 bits for red, green, blue, alpha; in 32-bit word
            RGBA_8888_SRGB = 5,
            RGB_888x = 6,         // pixel with 8 bits each for red, green, blue; in 32-bit word
            RG_88 = 7,            // pixel with 8 bits for red and green; in 16-bit word
            BGRA_8888 = 8,        // pixel with 8 bits for blue, green, red, alpha; in 32-bit word
            RGBA_1010102 = 9,     // 10 bits for red, green, blue; 2 bits for alpha; in 32-bit word
            BGRA_1010102 = 10,    // 10 bits for blue, green, red; 2 bits for alpha; in 32-bit word
            GRAY_8 = 11,          // pixel with grayscale level in 8-bit byte
            GRAY_ALPHA_88 = 12,
            ALPHA_F16 = 13,       // pixel with a half float for alpha
            RGBA_F16 = 14,        // pixel with half floats for red, green, blue, alpha; in 64-bit word
            RGBA_F16_CLAMPED = 15,// pixel with half floats in [0,1] for red, green, blue, alpha; in 64-bit word
            RGBA_F32 = 16;        // pixel using C float for red, green, blue, alpha; in 128-bit word
    int
            ALPHA_16 = 17,        // pixel with a little endian uint16_t for alpha
            RG_1616 = 18,         // pixel with a little endian uint16_t for red and green
            RG_F16 = 19,          // pixel with a half float for red and green
            RGBA_16161616 = 20;   // pixel with a little endian uint16_t for red, green, blue and alpha
    /**
     * Aliases.
     */
    int
            RGB_565 = BGR_565,
            RGBA_F16_NORM = RGBA_F16_CLAMPED,
            R8G8_UNORM = RG_88,
            A16_UNORM = ALPHA_16,
            A16G16_UNORM = RG_1616,
            A16_FLOAT = ALPHA_F16,
            R16G16_FLOAT = RG_F16,
            R16G16B16A16_UNORM = RGBA_16161616;
    // Unusual types that come up after reading back in cases where we are reassigning the meaning
    // of a texture format's channels to use for a particular color format but have to read back the
    // data to a full RGBA quadruple. (e.g. using a R8 texture format as A8 color type but the API
    // only supports reading to RGBA8.)
    @ApiStatus.Internal
    int
            ALPHA_8xxx = 21,
            ALPHA_F32xxx = 22,
            GRAY_8xxx = 23;
    // Types used to initialize backend textures.
    @ApiStatus.Internal
    int
            RGB_888 = 24,
            R_8 = 25,
            R_16 = 26,
            R_F16 = 27,
            GRAY_F16 = 28,
            BGRA_4444 = 29,
            ARGB_4444 = 30; // see COLOR_ABGR_4444 for public usage
}
