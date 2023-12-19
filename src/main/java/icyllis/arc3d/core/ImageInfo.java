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

package icyllis.arc3d.core;

import icyllis.arc3d.engine.Engine;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Describes pixel dimensions and encoding.
 * <p>
 * ImageInfo contains dimensions, the pixel integral width and height. It encodes
 * how pixel bits describe alpha, transparency; color components red, blue, and green.
 * <p>
 * ColorInfo is used to interpret a color (GPU side): color type + alpha type.
 * The color space is always sRGB, no color space transformation is needed.
 * <p>
 * ColorInfo are implemented as ints to reduce object allocation. This class
 * is provided to pack and unpack the &lt;color type, alpha type&gt; tuple
 * into the int.
 */
public final class ImageInfo {

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
     * memory, except for {@link #CT_RGB_565}. In other words the first component listed is in the
     * low bits and the last component in the high bits, reverse for {@link #CT_RGB_565}.
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
     * Creates a color info based on the supplied color type and alpha type.
     *
     * @param colorType the color type of the color info
     * @param alphaType the alpha type of the color info
     * @return the color info based on color type and alpha type
     */
    public static int makeColorInfo(int colorType, int alphaType) {
        assert ((alphaType & ~3) == 0);
        return colorType | (alphaType << 16);
    }

    /**
     * Extracts the color type from the supplied color info.
     *
     * @param colorInfo the color info to extract the color type from
     * @return the color type defined in the supplied color info
     */
    public static int colorType(int colorInfo) {
        assert ((colorInfo & ~0x3001F) == 0);
        return colorInfo & 0xFFFF;
    }

    /**
     * Extracts the alpha type from the supplied color info.
     *
     * @param colorInfo the color info to extract the alpha type from
     * @return the alpha type defined in the supplied color info
     */
    public static int alphaType(int colorInfo) {
        assert ((colorInfo & ~0x3001F) == 0);
        return colorInfo >>> 16;
    }

    /**
     * Creates new ColorInfo with same AlphaType, with ColorType set to newColorType.
     */
    public static int makeColorType(int colorInfo, int newColorType) {
        return makeColorInfo(newColorType, alphaType(colorInfo));
    }

    /**
     * Creates new ColorInfo with same ColorType, with AlphaType set to newAlphaType.
     */
    public static int makeAlphaType(int colorInfo, int newAlphaType) {
        return makeColorInfo(colorType(colorInfo), newAlphaType);
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
                throw new AssertionError("ct is not valid");
        }
        return at;
    }

    @Size(min = 0)
    private int width;
    @Size(min = 0)
    private int height;
    @ColorType
    private final short colorType;
    @AlphaType
    private final short alphaType;
    @Nullable
    private final ColorSpace colorSpace;

    /**
     * Creates ImageInfo from integral dimensions width and height, ColorType ct,
     * AlphaType at, and optionally ColorSpace cs.
     * <p>
     * If ColorSpace cs is null and ImageInfo is part of drawing source: ColorSpace
     * defaults to sRGB, mapping into Surface ColorSpace.
     * <p>
     * Parameters are not validated to see if their values are legal, or that the
     * combination is supported.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     * @param cs     range of colors; may be null
     */
    @Nonnull
    public static ImageInfo make(@Size(min = 0) int width, @Size(min = 0) int height,
                                 @ColorType int ct, @AlphaType int at, @Nullable ColorSpace cs) {
        return new ImageInfo(width, height, ct, at, cs);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height, {@link #CT_UNKNOWN},
     * {@link #AT_UNKNOWN}, with ColorSpace set to null.
     * <p>
     * Returned ImageInfo as part of source does not draw, and as part of destination
     * can not be drawn to.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    @Nonnull
    public static ImageInfo makeUnknown(@Size(min = 0) int width, @Size(min = 0) int height) {
        return new ImageInfo(width, height, CT_UNKNOWN, AT_UNKNOWN, null);
    }

    /**
     * Creates an empty ImageInfo with {@link #CT_UNKNOWN},
     * {@link #AT_UNKNOWN}, and a width and height of zero.
     */
    public ImageInfo() {
        this(0, 0);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height,
     * {@link #CT_UNKNOWN} and {@link #AT_UNKNOWN}.
     * <p>
     * Returned ImageInfo as part of source does not draw, and as part of destination
     * can not be drawn to.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    public ImageInfo(@Size(min = 0) int width, @Size(min = 0) int height) {
        this(width, height, CT_UNKNOWN, AT_UNKNOWN);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height, colorType and
     * alphaType.
     * <p>
     * Parameters are not validated to see if their values are legal, or that the
     * combination is supported.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    public ImageInfo(@Size(min = 0) int width, @Size(min = 0) int height,
                     @ColorType int colorType, @AlphaType int alphaType) {
        this(width, height, colorType, alphaType, null);
    }

    /**
     * Creates ImageInfo from integral dimensions and ColorInfo,
     * <p>
     * Parameters are not validated to see if their values are legal, or that the
     * combination is supported.
     *
     * @param w pixel column count; must be zero or greater
     * @param h pixel row count; must be zero or greater
     */
    @ApiStatus.Internal
    public ImageInfo(@Size(min = 0) int w, @Size(min = 0) int h,
                     @ColorType int ct, @AlphaType int at, @Nullable ColorSpace cs) {
        width = w;
        height = h;
        colorType = (short) ct;
        alphaType = (short) at;
        colorSpace = cs;
    }

    /**
     * Internal resize for optimization purposes. ImageInfo should be created immutable.
     */
    @ApiStatus.Internal
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns pixel count in each row.
     *
     * @return pixel width
     */
    public int width() {
        return width;
    }

    /**
     * Returns pixel row count.
     *
     * @return pixel height
     */
    public int height() {
        return height;
    }

    /**
     * Returns color type.
     *
     * @return color type
     */
    public int colorType() {
        return colorType;
    }

    /**
     * Returns alpha type.
     *
     * @return alpha type
     */
    public int alphaType() {
        return alphaType;
    }

    /**
     * Returns ColorSpace, the range of colors.
     *
     * @return ColorSpace, or null
     */
    @Nullable
    public ColorSpace colorSpace() {
        return colorSpace;
    }

    /**
     * Returns number of bytes per pixel required by ColorType.
     * Returns zero if colorType is {@link #CT_UNKNOWN}.
     *
     * @return bytes in pixel, bpp
     */
    public int bytesPerPixel() {
        return bytesPerPixel(colorType());
    }

    /**
     * Returns minimum bytes per row, computed from pixel width() and ColorType, which
     * specifies bytesPerPixel().
     *
     * @return width() times bytesPerPixel() as integer
     */
    public int minRowBytes() {
        return width() * bytesPerPixel();
    }

    /**
     * Returns if ImageInfo describes an empty area of pixels by checking if either
     * width or height is zero or smaller.
     *
     * @return true if either dimension is zero or smaller
     */
    public boolean isEmpty() {
        return width <= 0 && height <= 0;
    }

    /**
     * Returns true if AlphaType is set to hint that all pixels are opaque; their
     * alpha value is implicitly or explicitly 1.0.
     * <p>
     * Does not check if ColorType allows alpha, or if any pixel value has
     * transparency.
     *
     * @return true if AlphaType is Opaque
     */
    public boolean isOpaque() {
        return alphaType == AT_OPAQUE ||
                (Engine.colorTypeChannelFlags(colorType) & Color.COLOR_CHANNEL_FLAG_ALPHA) == 0;
    }

    /**
     * Returns if ImageInfo describes an empty area of pixels by checking if
     * width and height is greater than zero, and ColorInfo is valid.
     *
     * @return true if both dimension and ColorInfo is valid
     */
    public boolean isValid() {
        return width > 0 && height > 0 &&
                width <= 32767 && height <= 32767 &&
                colorType != CT_UNKNOWN &&
                alphaType != AT_UNKNOWN;
    }

    /**
     * Creates ImageInfo with the same ColorType and AlphaType,
     * with dimensions set to width and height.
     *
     * @param newWidth  pixel column count; must be zero or greater
     * @param newHeight pixel row count; must be zero or greater
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeWH(int newWidth, int newHeight) {
        return new ImageInfo(newWidth, newHeight, colorType, alphaType, colorSpace);
    }

    /**
     * Creates ImageInfo with same ColorSpace, ColorType, width, and height,
     * with AlphaType set to newAlphaType.
     *
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeAlphaType(@AlphaType int newAlphaType) {
        return new ImageInfo(width, height, colorType, newAlphaType, colorSpace);
    }

    /**
     * Creates ImageInfo with same ColorSpace, AlphaType, width, and height,
     * with ColorType set to newColorType.
     *
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeColorType(@ColorType int newColorType) {
        return new ImageInfo(width, height, newColorType, alphaType, colorSpace);
    }

    /**
     * Creates ImageInfo with same ColorType, AlphaType, width, and height,
     * with ColorSpace set to newColorSpace.
     *
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeColorSpace(@Nullable ColorSpace newColorSpace) {
        return new ImageInfo(width, height, colorType, alphaType, newColorSpace);
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        result = 31 * result + (int) colorType;
        result = 31 * result + (int) alphaType;
        result = 31 * result + Objects.hashCode(colorSpace);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ImageInfo ii)
            return width == ii.width &&
                    height == ii.height &&
                    colorType == ii.colorType &&
                    alphaType == ii.alphaType &&
                    Objects.equals(colorSpace, ii.colorSpace);
        return false;
    }

    @Override
    public String toString() {
        return '{' +
                "dimensions=" + width + "x" + height +
                ", colorType=" + colorType +
                ", alphaType=" + alphaType +
                ", colorSpace=" + colorSpace +
                '}';
    }
}
