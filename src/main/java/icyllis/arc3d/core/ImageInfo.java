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

package icyllis.arc3d.core;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Describes pixel dimensions and encoding.
 * <p>
 * ImageInfo contains dimensions, the pixel integral width and height. It encodes
 * how pixel bits describe alpha, transparency; color components red, blue, and green.
 */
public final class ImageInfo {

    @Size(min = 0)
    private int width;
    @Size(min = 0)
    private int height;
    @ColorInfo.ColorType
    private final short colorType;
    @ColorInfo.AlphaType
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
                                 @ColorInfo.ColorType int ct, @ColorInfo.AlphaType int at, @Nullable ColorSpace cs) {
        return new ImageInfo(width, height, ct, at, cs);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height, {@link ColorInfo#CT_UNKNOWN},
     * {@link ColorInfo#AT_UNKNOWN}, with ColorSpace set to null.
     * <p>
     * Returned ImageInfo as part of source does not draw, and as part of destination
     * can not be drawn to.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    @Nonnull
    public static ImageInfo makeUnknown(@Size(min = 0) int width, @Size(min = 0) int height) {
        return new ImageInfo(width, height, ColorInfo.CT_UNKNOWN, ColorInfo.AT_UNKNOWN, null);
    }

    /**
     * Creates an empty ImageInfo with {@link ColorInfo#CT_UNKNOWN},
     * {@link ColorInfo#AT_UNKNOWN}, and a width and height of zero.
     */
    public ImageInfo() {
        this(0, 0);
    }

    /**
     * Creates ImageInfo from integral dimensions width and height,
     * {@link ColorInfo#CT_UNKNOWN} and {@link ColorInfo#AT_UNKNOWN}.
     * <p>
     * Returned ImageInfo as part of source does not draw, and as part of destination
     * can not be drawn to.
     *
     * @param width  pixel column count; must be zero or greater
     * @param height pixel row count; must be zero or greater
     */
    public ImageInfo(@Size(min = 0) int width, @Size(min = 0) int height) {
        this(width, height, ColorInfo.CT_UNKNOWN, ColorInfo.AT_UNKNOWN);
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
                     @ColorInfo.ColorType int colorType, @ColorInfo.AlphaType int alphaType) {
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
                     @ColorInfo.ColorType int ct, @ColorInfo.AlphaType int at, @Nullable ColorSpace cs) {
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
    @ColorInfo.ColorType
    public int colorType() {
        return colorType;
    }

    /**
     * Returns alpha type.
     *
     * @return alpha type
     */
    @ColorInfo.AlphaType
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
     * Returns zero if colorType is {@link ColorInfo#CT_UNKNOWN}.
     *
     * @return bytes in pixel, bpp
     */
    public int bytesPerPixel() {
        return ColorInfo.bytesPerPixel(colorType());
    }

    /**
     * Returns minimum bytes per row, computed from pixel width() and ColorType, which
     * specifies bytesPerPixel().
     *
     * @return width() times bytesPerPixel() as integer
     */
    public long minRowBytes() {
        return width() * (long) bytesPerPixel();
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
        return alphaType == ColorInfo.AT_OPAQUE ||
                (ColorInfo.colorTypeChannelFlags(colorType) & Color.COLOR_CHANNEL_FLAG_ALPHA) == 0;
    }

    /**
     * Returns if ImageInfo describes an empty area of pixels by checking if
     * width and height is greater than zero, and ColorInfo is valid.
     *
     * @return true if both dimension and ColorInfo is valid
     */
    public boolean isValid() {
        return width > 0 && height > 0 &&
                width <= (Integer.MAX_VALUE >> 2) &&
                height <= (Integer.MAX_VALUE >> 2) &&
                colorType != ColorInfo.CT_UNKNOWN &&
                alphaType != ColorInfo.AT_UNKNOWN;
    }

    public long computeByteSize(long rowBytes) {
        if (height == 0) {
            return 0;
        }
        //TODO possible need to check for 64-bit signed overflow?
        return (height - 1) * rowBytes + minRowBytes();
    }

    public long computeMinByteSize() {
        return computeByteSize(minRowBytes());
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
    public ImageInfo makeAlphaType(@ColorInfo.AlphaType int newAlphaType) {
        return new ImageInfo(width, height, colorType, newAlphaType, colorSpace);
    }

    /**
     * Creates ImageInfo with same ColorSpace, AlphaType, width, and height,
     * with ColorType set to newColorType.
     *
     * @return created ImageInfo
     */
    @Nonnull
    public ImageInfo makeColorType(@ColorInfo.ColorType int newColorType) {
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
