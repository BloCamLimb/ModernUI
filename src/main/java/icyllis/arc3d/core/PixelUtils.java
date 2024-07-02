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

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteOrder;

/**
 * Utilities to access and convert pixels, heap and native.
 */
public class PixelUtils {

    static final sun.misc.Unsafe UNSAFE = getUnsafe();

    // we assume little-endian and do conversion if we're on big-endian machines
    public static final boolean NATIVE_BIG_ENDIAN =
            (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);

    private static sun.misc.Unsafe getUnsafe() {
        try {
            var field = MemoryUtil.class.getDeclaredField("UNSAFE");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            throw new AssertionError("No MemoryUtil.UNSAFE", e);
        }
    }

    /**
     * Copy memory row by row.
     */
    public static void copyImage(long src, long srcRowBytes,
                                 long dst, long dstRowBytes,
                                 long trimRowBytes, int rowCount) {
        if (srcRowBytes < trimRowBytes || dstRowBytes < trimRowBytes) {
            throw new IllegalArgumentException();
        }
        if (srcRowBytes == trimRowBytes && dstRowBytes == trimRowBytes) {
            MemoryUtil.memCopy(src, dst, trimRowBytes * rowCount);
        } else {
            while (rowCount-- != 0) {
                MemoryUtil.memCopy(src, dst, trimRowBytes);
                src += srcRowBytes;
                dst += dstRowBytes;
            }
        }
    }

    /**
     * Copy memory row by row, allowing heap to off-heap copy.
     */
    public static void copyImage(Object srcBase, long srcAddr, long srcRowBytes,
                                 Object dstBase, long dstAddr, long dstRowBytes,
                                 long trimRowBytes, int rowCount) {
        if (srcBase == null && dstBase == null) {
            copyImage(srcAddr, srcRowBytes, dstAddr, dstRowBytes, trimRowBytes, rowCount);
        } else {
            if (srcRowBytes < trimRowBytes || dstRowBytes < trimRowBytes) {
                throw new IllegalArgumentException();
            }
            if (srcRowBytes == trimRowBytes && dstRowBytes == trimRowBytes) {
                UNSAFE.copyMemory(srcBase, srcAddr, dstBase, dstAddr, trimRowBytes * rowCount);
            } else {
                while (rowCount-- != 0) {
                    UNSAFE.copyMemory(srcBase, srcAddr, dstBase, dstAddr, trimRowBytes);
                    srcAddr += srcRowBytes;
                    dstAddr += dstRowBytes;
                }
            }
        }
    }

    public static void setPixel8(Object base, long addr,
                                 byte value, int count) {
        long wideValue = (long) value << 8 | value;
        wideValue |= wideValue << 16;
        wideValue |= wideValue << 32;
        while (count >= 8) {
            UNSAFE.putLong(base, addr, wideValue);
            addr += 8;
            count -= 8;
        }
        while (count-- != 0) {
            UNSAFE.putByte(base, addr, value);
            addr += 1;
        }
    }

    public static void setPixel16(Object base, long addr,
                                  short value, int count) {
        if (NATIVE_BIG_ENDIAN) {
            value = Short.reverseBytes(value);
        }
        long wideValue = (long) value << 16 | value;
        wideValue |= wideValue << 32;
        while (count >= 4) {
            UNSAFE.putLong(base, addr, wideValue);
            addr += 8;
            count -= 4;
        }
        while (count-- != 0) {
            UNSAFE.putShort(base, addr, value);
            addr += 2;
        }
    }

    public static void setPixel32(Object base, long addr,
                                  int value, int count) {
        if (NATIVE_BIG_ENDIAN) {
            value = Integer.reverseBytes(value);
        }
        long wideValue = (long) value << 32 | value;
        while (count >= 2) {
            UNSAFE.putLong(base, addr, wideValue);
            addr += 8;
            count -= 2;
        }
        if (count != 0) {
            assert count == 1;
            UNSAFE.putInt(base, addr, value);
        }
    }

    public static void setPixel64(Object base, long addr,
                                  long value, int count) {
        if (NATIVE_BIG_ENDIAN) {
            value = Long.reverseBytes(value);
        }
        while (count-- != 0) {
            UNSAFE.putLong(base, addr, value);
            addr += 8;
        }
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    //@formatter:off
    public static void load(@ColorInfo.ColorType int ct, Object base, long addr, float[] dst) {
        switch (ct) {
            case ColorInfo.CT_RGB_565 -> {
                short val = UNSAFE.getShort(base, addr);
                dst[0] = ((val >>> 11)     ) * (1.0f / 31);
                dst[1] = ((val >>>  5) & 63) * (1.0f / 63);
                dst[2] = ((val       ) & 31) * (1.0f / 31);
                dst[3] = 1.0f;
            }
            case ColorInfo.CT_RGB_888, ColorInfo.CT_RGB_888x -> {
                dst[0] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+0)) * (1.0f / 255);
                dst[1] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+1)) * (1.0f / 255);
                dst[2] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+2)) * (1.0f / 255);
                dst[3] = 1.0f;
            }
            case ColorInfo.CT_RGBA_8888 -> {
                dst[0] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+0)) * (1.0f / 255);
                dst[1] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+1)) * (1.0f / 255);
                dst[2] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+2)) * (1.0f / 255);
                dst[3] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+3)) * (1.0f / 255);
            }
            case ColorInfo.CT_GRAY_8 -> {
                float y = Byte.toUnsignedInt(UNSAFE.getByte(base, addr)) * (1.0f / 255);
                dst[0] = dst[1] = dst[2] = y;
                dst[3] = 1.0f;
            }
            case ColorInfo.CT_GRAY_ALPHA_88 -> {
                float y = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+0)) * (1.0f / 255);
                dst[0] = dst[1] = dst[2] = y;
                dst[3] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr+1)) * (1.0f / 255);
            }
            case ColorInfo.CT_ALPHA_8 -> {
                dst[0] = dst[1] = dst[2] = 0.0f;
                dst[3] = Byte.toUnsignedInt(UNSAFE.getByte(base, addr)) * (1.0f / 255);
            }
            case ColorInfo.CT_RGBA_F32 -> {
                dst[0] = UNSAFE.getFloat(base, addr+0 );
                dst[1] = UNSAFE.getFloat(base, addr+4 );
                dst[2] = UNSAFE.getFloat(base, addr+8 );
                dst[3] = UNSAFE.getFloat(base, addr+12);
            }
        }
    }
    //@formatter:on

    @SuppressWarnings("PointlessArithmeticExpression")
    //@formatter:off
    public static void store(@ColorInfo.ColorType int ct, Object base, long addr, float[] src) {
        switch (ct) {
            case ColorInfo.CT_RGB_565 -> {
                short val = (short) ((int) (src[0] * 31 + .5f) << 11 |
                                     (int) (src[1] * 63 + .5f) << 5  |
                                     (int) (src[2] * 31 + .5f)       );
                UNSAFE.putShort(base, addr, val);
            }
            case ColorInfo.CT_RGB_888 -> {
                UNSAFE.putByte(base, addr+0, (byte) (src[0] * 255 + .5f));
                UNSAFE.putByte(base, addr+1, (byte) (src[1] * 255 + .5f));
                UNSAFE.putByte(base, addr+2, (byte) (src[2] * 255 + .5f));
            }
            case ColorInfo.CT_RGB_888x -> {
                UNSAFE.putByte(base, addr+0, (byte) (src[0] * 255 + .5f));
                UNSAFE.putByte(base, addr+1, (byte) (src[1] * 255 + .5f));
                UNSAFE.putByte(base, addr+2, (byte) (src[2] * 255 + .5f));
                UNSAFE.putByte(base, addr+3, (byte) (         255      ));
            }
            case ColorInfo.CT_RGBA_8888 -> {
                UNSAFE.putByte(base, addr+0, (byte) (src[0] * 255 + .5f));
                UNSAFE.putByte(base, addr+1, (byte) (src[1] * 255 + .5f));
                UNSAFE.putByte(base, addr+2, (byte) (src[2] * 255 + .5f));
                UNSAFE.putByte(base, addr+3, (byte) (src[3] * 255 + .5f));
            }
            case ColorInfo.CT_GRAY_8 -> {
                float y = src[0]*0.2126f + src[1]*0.7152f + src[2]*0.0722f;
                UNSAFE.putByte(base, addr, (byte) (y * 255 + .5f));
            }
            case ColorInfo.CT_GRAY_ALPHA_88 -> {
                float y = src[0]*0.2126f + src[1]*0.7152f + src[2]*0.0722f;
                UNSAFE.putByte(base, addr+0, (byte) (y      * 255 + .5f));
                UNSAFE.putByte(base, addr+1, (byte) (src[3] * 255 + .5f));
            }
            case ColorInfo.CT_ALPHA_8 -> {
                UNSAFE.putByte(base, addr, (byte) (src[3] * 255 + .5f));
            }
            case ColorInfo.CT_RGBA_F32 -> {
                UNSAFE.putFloat(base, addr+0 , src[0]);
                UNSAFE.putFloat(base, addr+4 , src[1]);
                UNSAFE.putFloat(base, addr+8 , src[2]);
                UNSAFE.putFloat(base, addr+12, src[3]);
            }
        }
    }
    //@formatter:on

    // Do NOT change these flags
    public static final int
            kColorSpaceXformFlagUnpremul = 0x1,
            kColorSpaceXformFlagLinearize = 0x2,
            kColorSpaceXformFlagGamutTransform = 0x4,
            kColorSpaceXformFlagEncode = 0x8,
            kColorSpaceXformFlagPremul = 0x10;

    /**
     * Performs color type, alpha type, and color space conversion,
     * addresses must be aligned.
     */
    public static boolean convertPixels(ImageInfo srcInfo, Object srcBase,
                                        long srcAddr, long srcRowBytes,
                                        ImageInfo dstInfo, Object dstBase,
                                        long dstAddr, long dstRowBytes) {
        if (!srcInfo.isValid() || !dstInfo.isValid() ||
                srcInfo.width() != dstInfo.width() ||
                srcInfo.height() != dstInfo.height()) {
            return false;
        }
        if (srcRowBytes < srcInfo.minRowBytes() ||
                dstRowBytes < dstInfo.minRowBytes()) {
            return false;
        }
        if (srcRowBytes % srcInfo.bytesPerPixel() != 0 ||
                dstRowBytes % dstInfo.bytesPerPixel() != 0) {
            return false;
        }

        ColorSpace srcCS = srcInfo.colorSpace();
        @ColorInfo.ColorType var srcCT = srcInfo.colorType();
        @ColorInfo.AlphaType var srcAT = srcInfo.alphaType();
        ColorSpace dstCS = dstInfo.colorSpace();
        @ColorInfo.ColorType var dstCT = dstInfo.colorType();
        @ColorInfo.AlphaType var dstAT = dstInfo.alphaType();

        if (dstAT == ColorInfo.AT_OPAQUE) {
            dstAT = srcAT;
        }

        if (srcCS == null) {
            srcCS = ColorSpace.get(ColorSpace.Named.SRGB);
        }
        if (dstCS == null) {
            dstCS = srcCS;
        }

        boolean csXform = !srcCS.equals(dstCS);

        int flags = 0;

        if (csXform || srcAT != dstAT) {
            if (srcAT == ColorInfo.AT_PREMUL) {
                flags |= kColorSpaceXformFlagUnpremul;
            }
            if (srcAT != ColorInfo.AT_OPAQUE && dstAT == ColorInfo.AT_PREMUL) {
                flags |= kColorSpaceXformFlagPremul;
            }
        }

        if (ColorInfo.colorTypeIsAlphaOnly(srcCT) &&
                ColorInfo.colorTypeIsAlphaOnly(dstCT)) {
            csXform = false;
            flags = 0;
        }

        int width = srcInfo.width();
        int height = srcInfo.height();

        // We can copy the pixels when no color type, alpha type, or color space changes.
        if (srcCT == dstCT && !csXform && flags == 0) {
            copyImage(srcBase, srcAddr, srcRowBytes,
                    dstBase, dstAddr, dstRowBytes,
                    srcInfo.minRowBytes(), height);
            return true;
        }

        int srcBpp = srcInfo.bytesPerPixel();
        int dstBpp = dstInfo.bytesPerPixel();

        float[] col = new float[4];

        var connector = csXform ? ColorSpace.connect(srcCS, dstCS) : null;

        for (int j = 0; j < height; j++) {
            long nextSrcAddr = srcAddr + srcRowBytes;
            long nextDstAddr = dstAddr + dstRowBytes;
            for (int i = 0; i < width; i++) {
                load(srcCT, srcBase, srcAddr, col);
                if ((flags & kColorSpaceXformFlagUnpremul) != 0) {
                    float scale = 1.0f / col[3];
                    if (!Float.isFinite(scale)) { // NaN or Inf
                        scale = 0;
                    }
                    col[0] *= scale;
                    col[1] *= scale;
                    col[2] *= scale;
                }
                if (connector != null) {
                    connector.transform(col);
                }
                if ((flags & kColorSpaceXformFlagPremul) != 0) {
                    float scale = col[3];
                    col[0] *= scale;
                    col[1] *= scale;
                    col[2] *= scale;
                }
                store(dstCT, dstBase, dstAddr, col);
                srcAddr += srcBpp;
                dstAddr += dstBpp;
            }
            srcAddr = nextSrcAddr;
            dstAddr = nextDstAddr;
        }

        return true;
    }
}
