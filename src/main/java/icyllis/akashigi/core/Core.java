/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.core;

/**
 * Shared constants, enums and utilities for Core.
 */
public final class Core {

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
    public static final class CompressionType {

        /**
         * Public values.
         */
        public static final int
                kNone = 0;
        public static final int
                kETC2_RGB8_UNORM = 1;
        public static final int
                kBC1_RGB8_UNORM = 2,
                kBC1_RGBA8_UNORM = 3;
        public static final int
                kLast = kBC1_RGBA8_UNORM;

        private CompressionType() {
        }
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
    public static final class AlphaType {

        /**
         * Public values.
         */
        public static final int
                kUnknown = 0,   // uninitialized
                kOpaque = 1,    // pixel is opaque
                kPremul = 2,    // pixel components are premultiplied by alpha
                kUnpremul = 3;  // pixel components are unassociated with alpha
        public static final int kLast = kUnpremul;

        private AlphaType() {
        }
    }

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
    public static class ColorType {

        public static final int
                kUnknown = 0,           // uninitialized
                kAlpha_8 = 1,           // pixel with alpha in 8-bit byte
                kBGR_565 = 2,           // pixel with 5 bits red, 6 bits green, 5 bits blue, in 16-bit word
                kABGR_4444 = 3,         // pixel with 4 bits for alpha, blue, red, green; in 16-bit word
                kRGBA_8888 = 4,         // pixel with 8 bits for red, green, blue, alpha; in 32-bit word
                kRGBA_8888_SRGB = 5,
                kRGB_888x = 6,          // pixel with 8 bits each for red, green, blue; in 32-bit word
                kRG_88 = 7,             // pixel with 8 bits for red and green; in 16-bit word
                kBGRA_8888 = 8,         // pixel with 8 bits for blue, green, red, alpha; in 32-bit word
                kRGBA_1010102 = 9,      // 10 bits for red, green, blue; 2 bits for alpha; in 32-bit word
                kBGRA_1010102 = 10,     // 10 bits for blue, green, red; 2 bits for alpha; in 32-bit word
                kGray_8 = 11,           // pixel with grayscale level in 8-bit byte
                kAlpha_F16 = 12,        // pixel with a half float for alpha
                kRGBA_F16 = 13,         // pixel with half floats for red, green, blue, alpha; in 64-bit word
                kRGBA_F16_Clamped = 14, // pixel with half floats [0,1] for red, green, blue, alpha; in 64-bit word
                kRGBA_F32 = 15,         // pixel using C float for red, green, blue, alpha; in 128-bit word
                kAlpha_16 = 16,         // pixel with a little endian uint16_t for alpha
                kRG_1616 = 17,          // pixel with a little endian uint16_t for red and green
                kRG_F16 = 18,           // pixel with a half float for red and green
                kRGBA_16161616 = 19,    // pixel with a little endian uint16_t for red, green, blue and alpha
                kR_8 = 20;
        /**
         * Aliases.
         */
        public static final int
                kRGB_565 = kBGR_565,
                kSRGBA_8888 = kRGBA_8888_SRGB,
                kRGBA_F16Norm = kRGBA_F16_Clamped,
                kR8_unorm = kR_8;
        /**
         * The following 6 color types are just for reading from - not for rendering to.
         */
        public static final int
                kR8G8_unorm = kRG_88,
                kA16_float = kAlpha_F16,
                kR16G16_float = kRG_F16,
                kA16_unorm = kAlpha_16,
                kR16G16_unorm = kRG_1616,
                kR16G16B16A16_unorm = kRGBA_16161616;

        protected ColorType() {
        }

        /**
         * @return bpp
         */
        public static int bytesPerPixel(int colorType) {
            return switch (colorType) {
                case kUnknown -> 0;
                case kAlpha_8,
                        kR_8,
                        kGray_8 -> 1;
                case kBGR_565,
                        kABGR_4444,
                        kAlpha_16,
                        kAlpha_F16,
                        kRG_88 -> 2;
                case kRGBA_8888,
                        kRG_F16,
                        kRG_1616,
                        kBGRA_1010102,
                        kRGBA_1010102,
                        kBGRA_8888,
                        kRGB_888x,
                        kRGBA_8888_SRGB -> 4;
                case kRGBA_F16,
                        kRGBA_16161616,
                        kRGBA_F16_Clamped -> 8;
                case kRGBA_F32 -> 16;
                default -> throw new AssertionError(colorType);
            };
        }
    }

    /**
     * Surface flags.
     */
    public static class SurfaceFlags {

        public static final int kNone = 0;
        /**
         * Indicates whether an allocation should count against a cache budget. Budgeted when
         * set, otherwise not budgeted.
         */
        public static final int kBudgeted = 1;
        /**
         * Indicates whether a backing store needs to be an exact match or can be larger than
         * is strictly necessary. Loose fit when set, otherwise exact fit.
         */
        public static final int kLooseFit = 1 << 1;
        /**
         * Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
         * when set, otherwise mipmaps are not allocated.
         */
        public static final int kMipmapped = 1 << 2;
        /**
         * Used to say whether a surface can be rendered to, whether a texture can be used as
         * color attachments. Renderable when set, otherwise not renderable.
         */
        public static final int kRenderable = 1 << 3;
        /**
         * Used to say whether texture is backed by protected memory. Protected when set, otherwise
         * not protected.
         */
        public static final int kProtected = 1 << 4;

        protected SurfaceFlags() {
        }
    }

    protected Core() {
    }
}
