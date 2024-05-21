/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes pixel dimensions and encoding.
 * <p>
 * ColorInfo is used to interpret a color: color type + alpha type + color space.
 */
public final class ColorInfo {

    //@formatter:off
    @ApiStatus.Internal
    @MagicConstant(intValues = {
            COMPRESSION_NONE,
            COMPRESSION_ETC2_RGB8_UNORM,
            COMPRESSION_BC1_RGB8_UNORM,
            COMPRESSION_BC1_RGBA8_UNORM
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CompressionType {
    }

    /**
     * Compression types.
     * <table>
     *   <tr>
     *     <th>Core</th>
     *     <th>GL_COMPRESSED_*</th>
     *     <th>VK_FORMAT_*_BLOCK</th>
     *   </tr>
     *   <tr>
     *     <td>ETC2_RGB8_UNORM</td>
     *     <td>RGB8_ETC2</td>
     *     <td>ETC2_R8G8B8_UNORM</td>
     *   </tr>
     *   <tr>
     *     <td>BC1_RGB8_UNORM</td>
     *     <td>RGB_S3TC_DXT1_EXT</td>
     *     <td>BC1_RGB_UNORM</td>
     *   </tr>
     *   <tr>
     *     <td>BC1_RGBA8_UNORM</td>
     *     <td>RGBA_S3TC_DXT1_EXT</td>
     *     <td>BC1_RGBA_UNORM</td>
     *   </tr>
     * </table>
     */
    public static final int
            COMPRESSION_NONE            = 0,
            COMPRESSION_ETC2_RGB8_UNORM = 1,
            COMPRESSION_BC1_RGB8_UNORM  = 2,
            COMPRESSION_BC1_RGBA8_UNORM = 3,
            COMPRESSION_COUNT           = 4;

    /**
     * Describes how to interpret the alpha component of a pixel.
     */
    @ApiStatus.Internal
    @MagicConstant(intValues = {
            AT_UNKNOWN,
            AT_OPAQUE,
            AT_PREMUL,
            AT_UNPREMUL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AlphaType {
    }

    /**
     * Alpha types.
     * <p>
     * Describes how to interpret the alpha component of a pixel. A pixel may
     * be opaque, or alpha, describing multiple levels of transparency.
     * <p>
     * In simple blending, alpha weights the source color and the destination
     * color to create a new color. If alpha describes a weight from zero to one:
     * <p>
     * result color = source color * alpha + destination color * (1 - alpha)
     * <p>
     * In practice alpha is encoded in two or more bits, where 1.0 equals all bits set.
     * <p>
     * RGB may have alpha included in each component value; the stored
     * value is the original RGB multiplied by alpha. Premultiplied color
     * components improve performance, but it will reduce the image quality.
     * The usual practice is to premultiply alpha in the GPU, since they were
     * converted into floating-point values.
     */
    public static final int
            AT_UNKNOWN  = 0, // uninitialized
            AT_OPAQUE   = 1, // pixel is opaque
            AT_PREMUL   = 2, // pixel components are premultiplied by alpha
            AT_UNPREMUL = 3; // pixel components are unassociated with alpha

    /**
     * Describes how pixel bits encode color.
     */
    @ApiStatus.Internal
    @MagicConstant(intValues = {
            CT_UNKNOWN,
            CT_RGB_565,
            CT_R_8,
            CT_RG_88,
            CT_RGB_888,
            CT_RGB_888x,
            CT_RGBA_8888,
            CT_BGRA_8888,
            CT_RGBA_8888_SRGB,
            CT_RGBA_1010102,
            CT_BGRA_1010102,
            CT_R_16,
            CT_R_F16,
            CT_RG_1616,
            CT_RG_F16,
            CT_RGBA_16161616,
            CT_RGBA_F16,
            CT_RGBA_F16_CLAMPED,
            CT_RGBA_F32,
            CT_ALPHA_8,
            CT_ALPHA_16,
            CT_ALPHA_F16,
            CT_GRAY_8,
            CT_GRAY_ALPHA_88,
            CT_R5G6B5_UNORM,
            CT_R8G8_UNORM,
            CT_A16_UNORM,
            CT_A16_FLOAT,
            CT_A16G16_UNORM,
            CT_R16G16_FLOAT,
            CT_R16G16B16A16_UNORM,
            CT_R_8xxx,
            CT_ALPHA_8xxx,
            CT_ALPHA_F32xxx,
            CT_GRAY_8xxx
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorType {
    }

    /**
     * Color types.
     * <p>
     * Describes a layout of pixel data in CPU or GPU memory. A pixel may be an alpha mask, a grayscale,
     * RGB, or RGBA. It specifies the channels, their type, and width. It does not refer to a texture
     * format and the mapping to texture formats may be many-to-many. It does not specify the sRGB
     * encoding of the stored values. The components are listed in order of where they appear in
     * memory, except for packed formats {@link #CT_RGB_565}, {@link #CT_RGBA_1010102} and
     * {@link #CT_BGRA_1010102}, the first component appear in most-significant bits.
     * <p>
     * Note: Skia doesn't support big-endian machines, because SkColorType doesn't match GrColorType.
     * We support that because we don't use bit shifts for most operations. When needed, we check
     * {@link PixelUtils#NATIVE_BIG_ENDIAN} and do conversion.
     */
    public static final int
            CT_UNKNOWN          = 0, // uninitialized
            CT_RGB_565          = 1, // pixel with 5 bits blue, 6 bits green, 5 bits red; in 16-bit word
            CT_R_8              = 2, // pixel with 8 bits for red
            CT_RG_88            = 3; // pixel with 8 bits for red, green; in 16-bit word
    @ApiStatus.Internal
    public static final int
            CT_RGB_888          = 4; // pixel with 8 bits for red, green, blue; in 24-bit word
    public static final int
            CT_RGB_888x         = 5, // pixel with 8 bits for red, green, blue; in 32-bit word
            CT_RGBA_8888        = 6, // pixel with 8 bits for red, green, blue, alpha; in 32-bit word
            CT_BGRA_8888        = 7; // pixel with 8 bits for blue, green, red, alpha; in 32-bit word
    @ApiStatus.Internal
    public static final int
            CT_RGBA_8888_SRGB   = 8;
    public static final int
            CT_RGBA_1010102     = 9,  // 10 bits for red, green, blue; 2 bits for alpha; in 32-bit word
            CT_BGRA_1010102     = 10; // 10 bits for blue, green, red; 2 bits for alpha; in 32-bit word
    @ApiStatus.Internal
    public static final int
            CT_R_16             = 11, // pixel with uint16_t for red
            CT_R_F16            = 12, // pixel with float16_t for red
            CT_RG_1616          = 13, // pixel with uint16_t for red and green
            CT_RG_F16           = 14; // pixel with float16_t for red and green
    public static final int
            CT_RGBA_16161616    = 15, // pixel with uint16_t for red, green, blue, alpha; in 64-bit word
            CT_RGBA_F16         = 16, // pixel with float16_t for red, green, blue, alpha; in 64-bit word
            CT_RGBA_F16_CLAMPED = 17, // pixel with float16_t for red, green, blue, alpha; in 64-bit word (manual)
            CT_RGBA_F32         = 18, // pixel with float32_t for red, green, blue, alpha; in 128-bit word
            CT_ALPHA_8          = 19, // pixel with uint8_t for alpha (000r)
            CT_ALPHA_16         = 20, // pixel with uint16_t for alpha (000r)
            CT_ALPHA_F16        = 21, // pixel with float16_t for alpha (000r)
            CT_GRAY_8           = 22; // pixel with uint8_t for grayscale level (rrr1)
    @ApiStatus.Internal
    public static final int
            CT_GRAY_ALPHA_88    = 23; // for PNG
    /**
     * Aliases.
     */
    public static final int
            CT_R5G6B5_UNORM       = CT_RGB_565,
            CT_R8G8_UNORM         = CT_RG_88,
            CT_A16_UNORM          = CT_ALPHA_16,
            CT_A16_FLOAT          = CT_ALPHA_F16,
            CT_A16G16_UNORM       = CT_RG_1616,
            CT_R16G16_FLOAT       = CT_RG_F16,
            CT_R16G16B16A16_UNORM = CT_RGBA_16161616;
    /**
     * Unusual types that come up after reading back in cases where we are reassigning the meaning
     * of a texture format's channels to use for a particular color format but have to read back the
     * data to a full RGBA quadruple. (e.g. using a R8 texture format as A8 color type but the API
     * only supports reading to RGBA8.)
     */
    @ApiStatus.Internal
    public static final int
            CT_R_8xxx       = 24,
            CT_ALPHA_8xxx   = 25,
            CT_ALPHA_F32xxx = 26,
            CT_GRAY_8xxx    = 27;
    @ApiStatus.Internal
    public static final int
            CT_COUNT        = 28;
    //@formatter:on

    /**
     * Returns the number of bytes required to store a pixel.
     *
     * @return bytes per pixel
     */
    public static int bytesPerPixel(@ColorType int ct) {
        return switch (ct) {
            case CT_UNKNOWN -> 0;
            case CT_R_8,
                    CT_ALPHA_8,
                    CT_GRAY_8 -> 1;
            case CT_RGB_565,
                    CT_RG_88,
                    CT_R_16,
                    CT_R_F16,
                    CT_ALPHA_16,
                    CT_ALPHA_F16,
                    CT_GRAY_ALPHA_88 -> 2;
            case CT_RGB_888 -> 3;
            case CT_RGB_888x,
                    CT_RGBA_8888,
                    CT_BGRA_8888,
                    CT_BGRA_1010102,
                    CT_RGBA_1010102,
                    CT_RG_1616,
                    CT_RG_F16,
                    CT_R_8xxx,
                    CT_ALPHA_8xxx,
                    CT_GRAY_8xxx,
                    CT_RGBA_8888_SRGB -> 4;
            case CT_RGBA_16161616,
                    CT_RGBA_F16,
                    CT_RGBA_F16_CLAMPED -> 8;
            case CT_RGBA_F32,
                    CT_ALPHA_F32xxx -> 16;
            default -> throw new AssertionError(ct);
        };
    }

    /**
     * Returns a valid AlphaType for <var>ct</var>. If there is more than one valid
     * AlphaType, returns <var>at</var>, if valid.
     *
     * @return a valid AlphaType
     * @throws IllegalArgumentException <var>at</var> is unknown, <var>ct</var> is not
     *                                  unknown, and <var>ct</var> has alpha channel.
     */
    @AlphaType
    public static int validateAlphaType(@ColorType int ct, @AlphaType int at) {
        switch (ct) {
            case CT_UNKNOWN:
                at = AT_UNKNOWN;
                break;
            case CT_ALPHA_8:
            case CT_ALPHA_16:
            case CT_ALPHA_F16:
                if (at == AT_UNPREMUL) {
                    at = AT_PREMUL;
                }
                // fallthrough
            case CT_GRAY_ALPHA_88:
            case CT_RGBA_8888:
            case CT_BGRA_8888:
            case CT_RGBA_8888_SRGB:
            case CT_RGBA_1010102:
            case CT_BGRA_1010102:
            case CT_RGBA_F16:
            case CT_RGBA_F16_CLAMPED:
            case CT_RGBA_F32:
            case CT_RGBA_16161616:
                if (at != AT_OPAQUE && at != AT_PREMUL && at != AT_UNPREMUL) {
                    throw new IllegalArgumentException("at is unknown");
                }
                break;
            case CT_GRAY_8:
            case CT_R_8:
            case CT_RG_88:
            case CT_RGB_565:
            case CT_RGB_888:
            case CT_RGB_888x:
            case CT_R_16:
            case CT_R_F16:
            case CT_RG_1616:
            case CT_RG_F16:
                at = AT_OPAQUE;
                break;
            default:
                throw new AssertionError(ct);
        }
        return at;
    }

    @ApiStatus.Internal
    public static int colorTypeChannelFlags(@ColorType int ct) {
        return switch (ct) {
            case CT_UNKNOWN -> 0;
            case CT_ALPHA_8,
                    CT_ALPHA_16,
                    CT_ALPHA_F16,
                    CT_ALPHA_8xxx,
                    CT_ALPHA_F32xxx -> Color.COLOR_CHANNEL_FLAG_ALPHA;
            case CT_RGB_565,
                    CT_RGB_888,
                    CT_RGB_888x -> Color.COLOR_CHANNEL_FLAGS_RGB;
            case CT_RGBA_16161616,
                    CT_RGBA_F32,
                    CT_RGBA_F16_CLAMPED,
                    CT_RGBA_F16,
                    CT_BGRA_1010102,
                    CT_RGBA_1010102,
                    CT_BGRA_8888,
                    CT_RGBA_8888_SRGB,
                    CT_RGBA_8888 -> Color.COLOR_CHANNEL_FLAGS_RGBA;
            case CT_RG_88,
                    CT_RG_1616,
                    CT_RG_F16 -> Color.COLOR_CHANNEL_FLAGS_RG;
            case CT_GRAY_8,
                    CT_GRAY_8xxx -> Color.COLOR_CHANNEL_FLAG_GRAY;
            case CT_R_8,
                    CT_R_16,
                    CT_R_F16,
                    CT_R_8xxx -> Color.COLOR_CHANNEL_FLAG_RED;
            case CT_GRAY_ALPHA_88 -> Color.COLOR_CHANNEL_FLAG_GRAY | Color.COLOR_CHANNEL_FLAG_ALPHA;
            default -> throw new AssertionError(ct);
        };
    }

    //@formatter:off
    public static String colorTypeToString(@ColorType int ct) {
        return switch (ct) {
            case CT_UNKNOWN             -> "UNKNOWN";
            case CT_R_8                 -> "R_8";
            case CT_ALPHA_8             -> "ALPHA_8";
            case CT_GRAY_8              -> "GRAY_8";
            case CT_RGB_565             -> "RGB_565";
            case CT_RG_88               -> "RG_88";
            case CT_R_16                -> "R_16";
            case CT_R_F16               -> "R_F16";
            case CT_ALPHA_16            -> "ALPHA_16";
            case CT_ALPHA_F16           -> "ALPHA_F16";
            case CT_GRAY_ALPHA_88       -> "GRAY_ALPHA_88";
            case CT_RGB_888             -> "RGB_888";
            case CT_RGB_888x            -> "RGB_888x";
            case CT_RGBA_8888           -> "RGBA_8888";
            case CT_BGRA_8888           -> "BGRA_8888";
            case CT_BGRA_1010102        -> "BGRA_1010102";
            case CT_RGBA_1010102        -> "RGBA_1010102";
            case CT_RG_1616             -> "RG_1616";
            case CT_RG_F16              -> "RG_F16";
            case CT_R_8xxx              -> "R_8xxx";
            case CT_ALPHA_8xxx          -> "ALPHA_8xxx";
            case CT_GRAY_8xxx           -> "GRAY_8xxx";
            case CT_RGBA_8888_SRGB      -> "RGBA_8888_SRGB";
            case CT_RGBA_16161616       -> "RGBA_16161616";
            case CT_RGBA_F16            -> "RGBA_F16";
            case CT_RGBA_F16_CLAMPED    -> "RGBA_F16_CLAMPED";
            case CT_RGBA_F32            -> "RGBA_F32";
            case CT_ALPHA_F32xxx        -> "ALPHA_F32xxx";
            default -> throw new AssertionError(ct);
        };
    }
    //@formatter:on

    private ColorInfo() {
    }
}
