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

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link Raster} is similar to Bitmap, they are all "raster" (pixel map), but this class
 * wraps Java2D's {@link java.awt.image.Raster}. Pixels are allocated on the Java heap,
 * and can be used with Java2D's software renderer.
 */
public class Raster {

    /**
     * Describes the usage of Raster.
     */
    @ApiStatus.Internal
    @MagicConstant(intValues = {
            FORMAT_UNKNOWN,
            FORMAT_GRAY_8,
            FORMAT_GRAY_16,
            FORMAT_RGB_565,
            FORMAT_RGB_888
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {
    }

    public static final int FORMAT_UNKNOWN = 0;
    /**
     * Grayscale, one channel, 8-bit per channel.
     */
    public static final int FORMAT_GRAY_8 = 1; // not endian-aware
    /**
     * Grayscale, one channel, 16-bit per channel.
     * <p>
     * Represented as a short on Java heap (native order).
     */
    public static final int FORMAT_GRAY_16 = 2; // not endian-aware
    /**
     * RGB, three channels; red is 5 bits, green is 6 bits, blue is 5 bits.
     * <p>
     * Represented as a short on Java heap (native order).
     * <p>
     * To GPU: RGB_565 (R5G6B5), R is in most-significant bits.
     */
    public static final int FORMAT_RGB_565 = 3; // not endian-aware
    /**
     * RGB, three channels, 8-bit per channel.
     * <p>
     * Represented as three bytes on Java heap.
     * <p>
     * To GPU: BGR_888 (B8G8R8), B is in the lowest address.
     */
    public static final int FORMAT_RGB_888 = 4; // not endian-aware

    // Other BufferedImage types are endian-aware

    @Nullable
    protected final BufferedImage mBufImg;
    protected volatile Pixmap mPixmap;
    protected final Pixels mPixels;

    public Raster(@Nullable BufferedImage bufImg, @Nonnull ImageInfo info,
                  @Nullable Object data, int baseOffset, int rowStride) {
        mBufImg = bufImg;
        mPixmap = new Pixmap(info, data, baseOffset, rowStride);
        mPixels = new Pixels(info.width(), info.height(), data, baseOffset, rowStride, /*freeFn*/ null);
    }

    @Nonnull
    public static Raster createRaster(@Size(min = 1, max = 32768) int width,
                                      @Size(min = 1, max = 32768) int height,
                                      @Format int format) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be positive");
        }
        if (width > 32768 || height > 32768) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be less than or equal to 32768");
        }
        final int ct, at, rowStride;
        final int imageType = switch (format) {
            case FORMAT_GRAY_8 -> {
                ct = ColorInfo.CT_GRAY_8;
                at = ColorInfo.AT_OPAQUE;
                rowStride = width;
                yield BufferedImage.TYPE_BYTE_GRAY;
            }
            case FORMAT_GRAY_16 -> {
                //TODO add GRAY_16 color type
                ct = ColorInfo.CT_UNKNOWN;
                at = ColorInfo.AT_OPAQUE;
                rowStride = width << 1;
                yield BufferedImage.TYPE_USHORT_GRAY;
            }
            case FORMAT_RGB_565 -> {
                ct = ColorInfo.CT_RGB_565;
                at = ColorInfo.AT_OPAQUE;
                rowStride = width << 1;
                yield BufferedImage.TYPE_USHORT_565_RGB;
            }
            case FORMAT_RGB_888 -> {
                //TODO add BGR_888 color type
                ct = ColorInfo.CT_UNKNOWN;
                at = ColorInfo.AT_OPAQUE;
                rowStride = width * 3;
                if (rowStride * height < 0) {
                    throw new IllegalArgumentException("Image is too large");
                }
                yield BufferedImage.TYPE_3BYTE_BGR;
            }
            case FORMAT_UNKNOWN -> {
                ct = ColorInfo.CT_UNKNOWN;
                at = ColorInfo.AT_UNKNOWN;
                rowStride = 0;
                yield BufferedImage.TYPE_CUSTOM;
            }
            default -> throw new IllegalArgumentException("Unrecognized format " + format);
        };
        var info = new ImageInfo(width, height, ct, at);
        final BufferedImage bufImg;
        final Object data;
        final int baseOffset;
        if (imageType != BufferedImage.TYPE_CUSTOM) {
            bufImg = new BufferedImage(width, height, imageType);
            // steal backing array
            data = switch (imageType) {
                case BufferedImage.TYPE_BYTE_GRAY, BufferedImage.TYPE_3BYTE_BGR -> {
                    DataBufferByte dataBuffer =
                            (DataBufferByte) bufImg.getRaster().getDataBuffer();
                    assert dataBuffer.getNumBanks() == 1;
                    baseOffset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
                    yield dataBuffer.getData(); // byte[]
                }
                case BufferedImage.TYPE_USHORT_GRAY, BufferedImage.TYPE_USHORT_565_RGB -> {
                    DataBufferUShort dataBuffer =
                            (DataBufferUShort) bufImg.getRaster().getDataBuffer();
                    assert dataBuffer.getNumBanks() == 1;
                    baseOffset = Unsafe.ARRAY_SHORT_BASE_OFFSET;
                    yield dataBuffer.getData(); // short[]
                }
                default -> {
                    assert false;
                    baseOffset = 0;
                    yield null;
                }
            };
        } else {
            bufImg = null;
            data = null;
            baseOffset = 0;
        }
        return new Raster(bufImg, info, data, baseOffset, rowStride);
    }

    public int getFormat() {
        if (mBufImg == null) {
            return FORMAT_UNKNOWN;
        }
        return switch (mBufImg.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY -> FORMAT_GRAY_8;
            case BufferedImage.TYPE_USHORT_GRAY -> FORMAT_GRAY_16;
            case BufferedImage.TYPE_USHORT_565_RGB -> FORMAT_RGB_565;
            case BufferedImage.TYPE_3BYTE_BGR -> FORMAT_RGB_888;
            default -> {
                assert false;
                yield FORMAT_UNKNOWN;
            }
        };
    }

    @Nonnull
    public ImageInfo getInfo() {
        return mPixmap.getInfo();
    }

    public int getWidth() {
        return mPixmap.getWidth();
    }

    public int getHeight() {
        return mPixmap.getHeight();
    }

    @ColorInfo.ColorType
    public int getColorType() {
        return mPixmap.getColorType();
    }

    @ColorInfo.AlphaType
    public int getAlphaType() {
        return mPixmap.getAlphaType();
    }

    @Nullable
    public ColorSpace getColorSpace() {
        return mPixmap.getColorSpace();
    }

    /**
     * Peek the current pixmap.
     */
    public Pixmap getPixmap() {
        return mPixmap;
    }

    // won't affect ref cnt
    @RawPtr
    public Pixels getPixels() {
        return mPixels;
    }
}
