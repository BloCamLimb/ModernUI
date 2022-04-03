/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
@SuppressWarnings({"MagicConstant", "unused"})
@Immutable
public final class ImageInfo {

    /**
     * Describes how to interpret the alpha component of a pixel.
     */
    @MagicConstant(intValues = {ALPHA_UNKNOWN, ALPHA_OPAQUE, ALPHA_PREMULTIPLIED, ALPHA_STRAIGHT})
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
            ALPHA_UNKNOWN = 0,          // uninitialized
            ALPHA_OPAQUE = 1,           // pixel is opaque
            ALPHA_PREMULTIPLIED = 2,    // pixel components are premultiplied by alpha
            ALPHA_STRAIGHT = 3;         // pixel components are independent of alpha

    /**
     * Describes how pixel bits encode color.
     */
    @MagicConstant(intValues = {COLOR_UNKNOWN,
            COLOR_ALPHA_8,
            COLOR_BGR_565,
            COLOR_ABGR_4444,
            COLOR_RGBA_8888,
            COLOR_RGB_888x,
            COLOR_RG_88,
            COLOR_BGRA_8888,
            COLOR_RGBA_1010102,
            COLOR_BGRA_1010102,
            COLOR_GRAY_8,
            COLOR_ALPHA_F16,
            COLOR_RGBA_F16,
            COLOR_RGBA_F16_CLAMPED,
            COLOR_RGBA_F32,
            COLOR_ALPHA_16,
            COLOR_RG_1616,
            COLOR_RG_F16,
            COLOR_RGBA_16161616,
            COLOR_RGB_565,
            COLOR_RGBA_F16_NORM,
            COLOR_R8G8_UNORM,
            COLOR_A16_UNORM,
            COLOR_A16G16_UNORM,
            COLOR_A16_FLOAT,
            COLOR_R16G16_FLOAT,
            COLOR_R16G16B16A16_UNORM
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorType {
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
    public static final int
            COLOR_UNKNOWN = 0,          // uninitialized
            COLOR_ALPHA_8 = 1,          // pixel with alpha in 8-bit byte
            COLOR_BGR_565 = 2,          // pixel with 5 bits red, 6 bits green, 5 bits blue, in 16-bit word
            COLOR_ABGR_4444 = 3,        // pixel with 4 bits for alpha, blue, red, green; in 16-bit word
            COLOR_RGBA_8888 = 4,        // pixel with 8 bits for red, green, blue, alpha; in 32-bit word
            COLOR_RGBA_8888_SRGB = 5,
            COLOR_RGB_888x = 6,         // pixel with 8 bits each for red, green, blue; in 32-bit word
            COLOR_RG_88 = 7,            // pixel with 8 bits for red and green; in 16-bit word
            COLOR_BGRA_8888 = 8,        // pixel with 8 bits for blue, green, red, alpha; in 32-bit word
            COLOR_RGBA_1010102 = 9,     // 10 bits for red, green, blue; 2 bits for alpha; in 32-bit word
            COLOR_BGRA_1010102 = 10,    // 10 bits for blue, green, red; 2 bits for alpha; in 32-bit word
            COLOR_GRAY_8 = 11,          // pixel with grayscale level in 8-bit byte
            COLOR_GRAY_ALPHA_88 = 12,
            COLOR_ALPHA_F16 = 13,       // pixel with a half float for alpha
            COLOR_RGBA_F16 = 14,        // pixel with half floats for red, green, blue, alpha; in 64-bit word
            COLOR_RGBA_F16_CLAMPED = 15,// pixel with half floats in [0,1] for red, green, blue, alpha; in 64-bit word
            COLOR_RGBA_F32 = 16;        // pixel using C float for red, green, blue, alpha; in 128-bit word
    public static final int
            COLOR_ALPHA_16 = 17,        // pixel with a little endian uint16_t for alpha
            COLOR_RG_1616 = 18,         // pixel with a little endian uint16_t for red and green
            COLOR_RG_F16 = 19,          // pixel with a half float for red and green
            COLOR_RGBA_16161616 = 20;   // pixel with a little endian uint16_t for red, green, blue and alpha
    /**
     * Aliases.
     */
    public static final int
            COLOR_RGB_565 = COLOR_BGR_565,
            COLOR_RGBA_F16_NORM = COLOR_RGBA_F16_CLAMPED,
            COLOR_R8G8_UNORM = COLOR_RG_88,
            COLOR_A16_UNORM = COLOR_ALPHA_16,
            COLOR_A16G16_UNORM = COLOR_RG_1616,
            COLOR_A16_FLOAT = COLOR_ALPHA_F16,
            COLOR_R16G16_FLOAT = COLOR_RG_F16,
            COLOR_R16G16B16A16_UNORM = COLOR_RGBA_16161616;
    // Unusual types that come up after reading back in cases where we are reassigning the meaning
    // of a texture format's channels to use for a particular color format but have to read back the
    // data to a full RGBA quadruple. (e.g. using a R8 texture format as A8 color type but the API
    // only supports reading to RGBA8.)
    @ApiStatus.Internal
    public static final int
            COLOR_ALPHA_8xxx = 21,
            COLOR_ALPHA_F32xxx = 22,
            COLOR_GRAY_8xxx = 23;
    // Types used to initialize backend textures.
    @ApiStatus.Internal
    public static final int
            COLOR_RGB_888 = 24,
            COLOR_R_8 = 25,
            COLOR_R_16 = 26,
            COLOR_R_F16 = 27,
            COLOR_GRAY_F16 = 28,
            COLOR_BGRA_4444 = 29,
            COLOR_ARGB_4444 = 30; // see COLOR_ABGR_4444 for public usage

    /**
     * Creates a color info based on the supplied color type and alpha type.
     *
     * @param ct the color type of the color info
     * @param at the alpha type of the color info
     * @return the color info based on color type and alpha type
     */
    public static int makeColorInfo(@ColorType int ct, @AlphaType int at) {
        return ct | (at << 12);
    }

    /**
     * Extracts the color type from the supplied color info.
     *
     * @param colorInfo the color info to extract the color type from
     * @return the color type defined in the supplied color info
     */
    @ColorType
    public static int colorType(int colorInfo) {
        return colorInfo & 0xFFF;
    }

    /**
     * Extracts the alpha type from the supplied color info.
     *
     * @param colorInfo the color info to extract the alpha type from
     * @return the alpha type defined in the supplied color info
     */
    @AlphaType
    public static int alphaType(int colorInfo) {
        return (colorInfo >> 12) & 0xFFF;
    }

    /**
     * Creates new ColorInfo with same ColorType, with AlphaType set to newAlphaType.
     */
    public static int makeAlphaType(int colorInfo, @AlphaType int newAlphaType) {
        return makeColorInfo(colorType(colorInfo), newAlphaType);
    }

    /**
     * Creates new ColorInfo with same AlphaType, with ColorType set to newColorType.
     */
    public static int makeColorType(int colorInfo, @ColorType int newColorType) {
        return makeColorInfo(newColorType, alphaType(colorInfo));
    }

    public static int bytesPerPixel(@ColorType int ct) {
        return switch (ct) {
            case COLOR_UNKNOWN -> 0;
            case COLOR_ALPHA_8,
                    COLOR_R_8,
                    COLOR_GRAY_8 -> 1;
            case COLOR_BGR_565,
                    COLOR_ABGR_4444,
                    COLOR_BGRA_4444,
                    COLOR_ARGB_4444,
                    COLOR_GRAY_F16,
                    COLOR_R_F16,
                    COLOR_R_16,
                    COLOR_ALPHA_16,
                    COLOR_ALPHA_F16,
                    COLOR_GRAY_ALPHA_88,
                    COLOR_RG_88 -> 2;
            case COLOR_RGB_888 -> 3;
            case COLOR_RGBA_8888,
                    COLOR_RG_F16,
                    COLOR_RG_1616,
                    COLOR_GRAY_8xxx,
                    COLOR_ALPHA_8xxx,
                    COLOR_BGRA_1010102,
                    COLOR_RGBA_1010102,
                    COLOR_BGRA_8888,
                    COLOR_RGB_888x,
                    COLOR_RGBA_8888_SRGB -> 4;
            case COLOR_RGBA_F16,
                    COLOR_RGBA_16161616,
                    COLOR_RGBA_F16_CLAMPED -> 8;
            case COLOR_RGBA_F32,
                    COLOR_ALPHA_F32xxx -> 16;
            default -> throw new IllegalArgumentException();
        };
    }

    private final int mWidth;
    private final int mHeight;
    private final int mColorInfo;

    /**
     * Creates an empty ImageInfo with {@link #COLOR_UNKNOWN},
     * {@link #ALPHA_UNKNOWN}, and a width and height of zero.
     */
    public ImageInfo() {
        this(0, 0, 0);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height,
     * {@link #COLOR_UNKNOWN} and {@link #ALPHA_UNKNOWN}.
     * <p>
     * Returned ImageInfo as part of source does not draw, and as part of destination
     * can not be drawn to.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    public ImageInfo(int width, int height) {
        this(width, height, 0);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height, ColorType ct,
     * AlphaType at.
     * <p>
     * Parameters are not validated to see if their values are legal, or that the
     * combination is supported.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    public ImageInfo(int width, int height, @ColorType int ct, @AlphaType int at) {
        this(width, height, makeColorInfo(ct, at));
    }

    /**
     * Creates ImageInfo from integral dimensions and ColorInfo,
     * <p>
     * Parameters are not validated to see if their values are legal, or that the
     * combination is supported.
     *
     * @param width     pixel column count; must be zero or greater
     * @param height    pixel row count; must be zero or greater
     * @param colorInfo the pixel encoding consisting of ColorType, AlphaType
     */
    ImageInfo(int width, int height, int colorInfo) {
        mWidth = width;
        mHeight = height;
        mColorInfo = colorInfo;
    }

    /**
     * Returns pixel count in each row.
     *
     * @return pixel width
     */
    public int width() {
        return mWidth;
    }

    /**
     * Returns pixel row count.
     *
     * @return pixel height
     */
    public int height() {
        return mHeight;
    }

    /**
     * Returns color type.
     *
     * @return color type
     */
    @ColorType
    public int colorType() {
        return colorType(mColorInfo);
    }

    /**
     * Returns alpha type.
     *
     * @return alpha type
     */
    @AlphaType
    public int alphaType() {
        return alphaType(mColorInfo);
    }

    /**
     * Returns the dimensionless ColorInfo that represents the same color type,
     * alpha type as this ImageInfo.
     */
    public int colorInfo() {
        return mColorInfo;
    }

    /**
     * Returns number of bytes per pixel required by ColorType.
     * Returns zero if colorType is {@link #COLOR_UNKNOWN}.
     *
     * @return bytes in pixel
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
        return mWidth * bytesPerPixel();
    }

    /**
     * Returns if ImageInfo describes an empty area of pixels by checking if either
     * width or height is zero or smaller.
     *
     * @return true if either dimension is zero or smaller
     */
    public boolean isEmpty() {
        return mWidth <= 0 && mHeight <= 0;
    }

    /**
     * Returns if ImageInfo describes an empty area of pixels by checking if
     * width and height is greater than zero, and ColorInfo is valid.
     *
     * @return true if both dimension and ColorInfo is valid
     */
    public boolean isValid() {
        return mWidth > 0 && mHeight > 0 &&
                colorType(mColorInfo) != COLOR_UNKNOWN &&
                alphaType(mColorInfo) != ALPHA_UNKNOWN;
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
        return new ImageInfo(newWidth, newHeight, mColorInfo);
    }

    /**
     * Creates ImageInfo with same ColorType, width, and height, with AlphaType set to newAlphaType.
     *
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeAlphaType(@AlphaType int newAlphaType) {
        return new ImageInfo(mWidth, mHeight, makeAlphaType(mColorInfo, newAlphaType));
    }

    /**
     * Creates ImageInfo with same AlphaType, width, and height, with ColorType set to newColorType.
     *
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeColorType(@ColorType int newColorType) {
        return new ImageInfo(mWidth, mHeight, makeColorType(mColorInfo, newColorType));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageInfo imageInfo = (ImageInfo) o;

        if (mWidth != imageInfo.mWidth) return false;
        if (mHeight != imageInfo.mHeight) return false;
        return mColorInfo == imageInfo.mColorInfo;
    }

    @Override
    public int hashCode() {
        int result = mWidth;
        result = 31 * result + mHeight;
        result = 31 * result + mColorInfo;
        return result;
    }
}
