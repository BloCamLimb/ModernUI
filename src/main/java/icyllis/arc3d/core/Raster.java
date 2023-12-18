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

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.jni.JNINativeInterface;

import java.awt.image.BufferedImage;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteOrder;

/**
 * {@link Raster} is similar to Bitmap, they are all "raster" (pixel map), but this class
 * wraps Java2D's {@link java.awt.image.Raster}. Pixels are allocated on the Java heap,
 * and can be used with Java2D's software renderer.
 * <p>
 * This class is required because the byte mapping of Java2D pixels depends on
 * {@link ByteOrder#nativeOrder()} and cannot be accepted by GPU, see
 * {@link ImageInfo#CT_UNKNOWN}.
 */
public class Raster {

    /**
     * Describes the usage of Raster, rather than pixel layout.
     */
    @ApiStatus.Internal
    @MagicConstant(intValues = {
            FORMAT_UNKNOWN,
            FORMAT_GRAY_8,
            FORMAT_GRAY_16,
            FORMAT_RGB_565,
            FORMAT_RGB_888,
            FORMAT_ARGB_8888
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
     */
    public static final int FORMAT_GRAY_16 = 2; // not endian-aware
    /**
     * RGB, three channels; red is 5 bits, green is 6 bits, blue is 5 bits.
     * <p>
     * Represented as a short on Java heap (native order).
     * <p>
     * To GPU: always RGB_565 (R5G6B5), R is in most-significant bits.
     * <p>
     * To Bitmap: need conversion.
     */
    public static final int FORMAT_RGB_565 = 3; // not endian-aware
    /**
     * RGB, three channels, 8-bit per channel.
     * <p>
     * Represented as three bytes on Java heap.
     * <p>
     * To GPU: always BGR_888 (B8G8R8), B is in the lowest address.
     * <p>
     * To Bitmap: need conversion.
     */
    public static final int FORMAT_RGB_888 = 4; // not endian-aware
    /**
     * RGB, with alpha, four channels, 8-bit per channel.
     * <p>
     * Represented as an int on Java heap.
     * <p>
     * To GPU:
     * <ul>
     * <li>Little-endian: BGRA_8888 (B8G8R8A8), B is in the lowest address.</li>
     * <li>Big-endian: need conversion, default to BGRA_8888.</li>
     * </ul>
     * To Bitmap: need conversion.
     */
    public static final int FORMAT_ARGB_8888 = 5;

    protected final ImageInfo mInfo;
    protected final BufferedImage mBufferedImage;

    public Raster(int width, int height, int format) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be positive");
        }
        if (width > 32768 || height > 32768) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be less than or equal to 32768");
        }
        int ct, at;
        int imageType = switch (format) {
            case FORMAT_GRAY_8 -> {
                ct = ImageInfo.CT_GRAY_8;
                at = ImageInfo.AT_OPAQUE;
                yield BufferedImage.TYPE_BYTE_GRAY;
            }
            case FORMAT_GRAY_16 -> {
                //TODO add GRAY_16 color type
                ct = ImageInfo.CT_UNKNOWN;
                at = ImageInfo.AT_OPAQUE;
                yield BufferedImage.TYPE_USHORT_GRAY;
            }
            case FORMAT_RGB_565 -> {
                ct = ImageInfo.CT_RGB_565;
                at = ImageInfo.AT_OPAQUE;
                yield BufferedImage.TYPE_USHORT_565_RGB;
            }
            case FORMAT_RGB_888 -> {
                if (width * height * 3 < 0) {
                    throw new IllegalArgumentException("Image is too large");
                }
                //TODO add BGR_888 color type
                ct = ImageInfo.CT_UNKNOWN;
                at = ImageInfo.AT_OPAQUE;
                yield BufferedImage.TYPE_3BYTE_BGR;
            }
            case FORMAT_ARGB_8888 -> {
                ct = ImageInfo.CT_BGRA_8888;
                at = ImageInfo.AT_UNPREMUL;
                yield BufferedImage.TYPE_INT_ARGB;
            }
            case FORMAT_UNKNOWN -> {
                ct = ImageInfo.CT_UNKNOWN;
                at = ImageInfo.AT_UNKNOWN;
                yield BufferedImage.TYPE_CUSTOM;
            }
            default -> throw new IllegalArgumentException("Unknown format " + format);
        };
        mInfo = new ImageInfo(width, height, ct, at);
        if (imageType != BufferedImage.TYPE_CUSTOM) {
            mBufferedImage = new BufferedImage(width, height, imageType);
        } else {
            mBufferedImage = null;
        }
    }

    public ImageInfo getInfo() {
        return mInfo;
    }

    public int getWidth() {
        return mInfo.width();
    }

    public int getHeight() {
        return mInfo.height();
    }

    public int getFormat() {
        if (mBufferedImage == null) {
            return FORMAT_UNKNOWN;
        }
        return switch (mBufferedImage.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY -> FORMAT_GRAY_8;
            case BufferedImage.TYPE_USHORT_GRAY -> FORMAT_GRAY_16;
            case BufferedImage.TYPE_USHORT_565_RGB -> FORMAT_RGB_565;
            case BufferedImage.TYPE_3BYTE_BGR -> FORMAT_RGB_888;
            case BufferedImage.TYPE_INT_ARGB -> FORMAT_ARGB_8888;
            default -> {
                assert false;
                yield FORMAT_UNKNOWN;
            }
        };
    }

    /**
     * Make a copy of heap buffer into native. Must be released later, via try-finally.
     *
     * @param base heap buffer
     * @return native buffer
     */
    public static long getBaseElements(Object base) {
        // HotSpot always copy
        Class<?> clazz = base.getClass();
        long elems;
        if (clazz == byte[].class) {
            elems = JNINativeInterface.nGetByteArrayElements((byte[]) base, 0);
        } else if (clazz == short[].class) {
            elems = JNINativeInterface.nGetShortArrayElements((short[]) base, 0);
        } else if (clazz == int[].class) {
            elems = JNINativeInterface.nGetIntArrayElements((int[]) base, 0);
        } else {
            throw new AssertionError(clazz);
        }
        return elems;
    }

    /**
     * Release the native buffer returned by {@link #getBaseElements(Object)}.
     *
     * @param base  heap buffer
     * @param elems native buffer
     * @param write true to copy native buffer back to heap buffer
     */
    public static void releaseBaseElements(Object base, long elems, boolean write) {
        Class<?> clazz = base.getClass();
        int mode = write ? 0 : JNINativeInterface.JNI_ABORT;
        if (clazz == byte[].class) {
            JNINativeInterface.nReleaseByteArrayElements((byte[]) base, elems, mode);
        } else if (clazz == short[].class) {
            JNINativeInterface.nReleaseShortArrayElements((short[]) base, elems, mode);
        } else if (clazz == int[].class) {
            JNINativeInterface.nReleaseIntArrayElements((int[]) base, elems, mode);
        } else {
            throw new AssertionError(clazz);
        }
    }

    /**
     * Copy a region of heap buffer into native buffer.
     *
     * @param base  heap buffer
     * @param start start offset
     * @param len   length
     * @param buf   native buffer
     */
    public static void getBaseRegion(Object base, int start, int len, long buf) {
        Class<?> clazz = base.getClass();
        if (clazz == byte[].class) {
            JNINativeInterface.nGetByteArrayRegion((byte[]) base, start, len, buf);
        } else if (clazz == short[].class) {
            JNINativeInterface.nGetShortArrayRegion((short[]) base, start, len, buf);
        } else if (clazz == int[].class) {
            JNINativeInterface.nGetIntArrayRegion((int[]) base, start, len, buf);
        } else {
            throw new AssertionError(clazz);
        }
    }
}
