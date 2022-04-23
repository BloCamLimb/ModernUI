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

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

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
public final class ImageInfo {

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

    @ApiStatus.Internal
    public static int bytesPerPixel(int ct) {
        return switch (ct) {
            case ColorType.UNKNOWN -> 0;
            case ColorType.ALPHA_8,
                    ColorType.R_8,
                    ColorType.GRAY_8 -> 1;
            case ColorType.BGR_565,
                    ColorType.ABGR_4444,
                    ColorType.BGRA_4444,
                    ColorType.ARGB_4444,
                    ColorType.GRAY_F16,
                    ColorType.R_F16,
                    ColorType.R_16,
                    ColorType.ALPHA_16,
                    ColorType.ALPHA_F16,
                    ColorType.GRAY_ALPHA_88,
                    ColorType.RG_88 -> 2;
            case ColorType.RGB_888 -> 3;
            case ColorType.RGBA_8888,
                    ColorType.RG_F16,
                    ColorType.RG_1616,
                    ColorType.GRAY_8xxx,
                    ColorType.ALPHA_8xxx,
                    ColorType.BGRA_1010102,
                    ColorType.RGBA_1010102,
                    ColorType.BGRA_8888,
                    ColorType.RGB_888x,
                    ColorType.RGBA_8888_SRGB -> 4;
            case ColorType.RGBA_F16,
                    ColorType.RGBA_16161616,
                    ColorType.RGBA_F16_CLAMPED -> 8;
            case ColorType.RGBA_F32,
                    ColorType.ALPHA_F32xxx -> 16;
            default -> throw new IllegalArgumentException();
        };
    }

    private int mWidth;
    private int mHeight;
    private final int mColorInfo;

    /**
     * Creates an empty ImageInfo with {@link ColorType#UNKNOWN},
     * {@link AlphaType#UNKNOWN}, and a width and height of zero.
     */
    public ImageInfo() {
        this(0, 0, 0);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height,
     * {@link ColorType#UNKNOWN} and {@link AlphaType#UNKNOWN}.
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
     * Internal resize for optimization purposes.
     */
    void resize(int width, int height) {
        mWidth = width;
        mHeight = height;
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
     * Returns zero if colorType is {@link ColorType#UNKNOWN}.
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
                colorType(mColorInfo) != ColorType.UNKNOWN &&
                alphaType(mColorInfo) != AlphaType.UNKNOWN;
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
