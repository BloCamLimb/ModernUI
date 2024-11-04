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

import org.jetbrains.annotations.Contract;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    public static void copyImage(long srcAddr, long srcRowBytes,
                                 long dstAddr, long dstRowBytes,
                                 long trimRowBytes, int rowCount) {
        copyImage(srcAddr, srcRowBytes,
                dstAddr, dstRowBytes,
                trimRowBytes, rowCount, false);
    }

    /**
     * Copy memory row by row, allowing vertical flip.
     */
    public static void copyImage(long srcAddr, long srcRowBytes,
                                 long dstAddr, long dstRowBytes,
                                 long trimRowBytes, int rowCount, boolean flipY) {
        if (srcRowBytes < trimRowBytes || dstRowBytes < trimRowBytes || trimRowBytes < 0) {
            throw new IllegalArgumentException();
        }
        // benchmark shows that memcpy is faster than Unsafe.copyMemory at bigger size, on OpenJDK 21
        if (srcRowBytes == trimRowBytes && dstRowBytes == trimRowBytes && !flipY) {
            LibCString.nmemcpy(dstAddr, srcAddr, trimRowBytes * rowCount);
        } else {
            if (flipY) {
                dstAddr += dstRowBytes * (rowCount - 1);
                dstRowBytes = -dstRowBytes;
            }
            for (int i = 0; i < rowCount; ++i) {
                LibCString.nmemcpy(dstAddr, srcAddr, trimRowBytes);
                srcAddr += srcRowBytes;
                dstAddr += dstRowBytes;
            }
        }
    }

    /**
     * Copy memory row by row, allowing heap to off-heap copy.
     */
    public static void copyImage(Object srcBase, long srcAddr, long srcRowBytes,
                                 Object dstBase, long dstAddr, long dstRowBytes,
                                 long trimRowBytes, int rowCount) {
        copyImage(srcBase, srcAddr, srcRowBytes,
                dstBase, dstAddr, dstRowBytes,
                trimRowBytes, rowCount, false);
    }

    /**
     * Copy memory row by row, allowing heap to off-heap copy and vertical flip.
     */
    public static void copyImage(Object srcBase, long srcAddr, long srcRowBytes,
                                 Object dstBase, long dstAddr, long dstRowBytes,
                                 long trimRowBytes, int rowCount, boolean flipY) {
        if (srcBase == null && dstBase == null) {
            copyImage(srcAddr, srcRowBytes, dstAddr, dstRowBytes, trimRowBytes, rowCount, flipY);
        } else {
            if (srcRowBytes < trimRowBytes || dstRowBytes < trimRowBytes || trimRowBytes < 0) {
                throw new IllegalArgumentException();
            }
            if (srcRowBytes == trimRowBytes && dstRowBytes == trimRowBytes && !flipY) {
                UNSAFE.copyMemory(srcBase, srcAddr, dstBase, dstAddr, trimRowBytes * rowCount);
            } else {
                if (flipY) {
                    dstAddr += dstRowBytes * (rowCount - 1);
                    dstRowBytes = -dstRowBytes;
                }
                for (int i = 0; i < rowCount; ++i) {
                    UNSAFE.copyMemory(srcBase, srcAddr, dstBase, dstAddr, trimRowBytes);
                    srcAddr += srcRowBytes;
                    dstAddr += dstRowBytes;
                }
            }
        }
    }

    /**
     * Pack Alpha8 format to B/W format.
     */
    public static void packA8ToBW(Object srcBase, long srcAddr, int srcRowBytes,
                                  Object dstBase, long dstAddr, int dstRowBytes,
                                  int width, int height) {
        int octets = width >> 3;
        int leftover = width & 7;

        assert (srcRowBytes >= width);
        assert (dstRowBytes >= ((width + 7) >> 3));

        for (int y = 0; y < height; ++y) {
            long nextSrcAddr = srcAddr + srcRowBytes;
            long nextDstAddr = dstAddr + dstRowBytes;
            for (int i = 0; i < octets; ++i) {
                int bits = 0;
                for (int j = 0; j < 8; ++j) {
                    bits <<= 1;
                    int v = (UNSAFE.getByte(srcBase, srcAddr + j) & 0xFF) >> 7;
                    bits |= v;
                }
                UNSAFE.putByte(dstBase, dstAddr, (byte) bits);
                srcAddr += 8;
                dstAddr += 1;
            }
            if (leftover > 0) {
                int bits = 0;
                int shift = 7;
                for (int j = 0; j < leftover; ++j, --shift) {
                    bits |= (UNSAFE.getByte(srcBase, srcAddr + j) & 0xFF) >> 7 << shift;
                }
                UNSAFE.putByte(dstBase, dstAddr, (byte) bits);
            }
            srcAddr = nextSrcAddr;
            dstAddr = nextDstAddr;
        }
    }

    /**
     * Unpack B/W format to Alpha8 format.
     */
    public static void unpackBWToA8(Object srcBase, long srcAddr, int srcRowBytes,
                                    Object dstBase, long dstAddr, int dstRowBytes,
                                    int width, int height) {
        assert (srcRowBytes >= ((width + 7) >> 3));
        assert (dstRowBytes >= width);

        for (int y = 0; y < height; ++y) {
            long nextSrcAddr = srcAddr + srcRowBytes;
            long nextDstAddr = dstAddr + dstRowBytes;
            int x = width;
            while (x > 0) {
                int mask = UNSAFE.getByte(srcBase, srcAddr) & 0xFF;
                for (int shift = 7; shift >= 0 && x != 0; --shift, --x) {
                    UNSAFE.putByte(dstBase, dstAddr, (mask & (1 << shift)) != 0 ? (byte) ~0 : 0);
                    dstAddr += 1;
                }
                srcAddr += 1;
            }
            srcAddr = nextSrcAddr;
            dstAddr = nextDstAddr;
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

    /**
     * Load pixel value in low precision.
     */
    @FunctionalInterface
    public interface PixelLoad {
        @ColorInt
        int load(Object base, long addr);
    }

    private static final int[] lut5;
    private static final int[] lut6;

    static {
        int[] lu5 = new int[1 << 5];
        int[] lu6 = new int[1 << 6];
        for (int i = 0; i < 1 << 5; i++) {
            lu5[i] = (int) (i * (255/31.0f) + .5f);
        }
        for (int i = 0; i < 1 << 6; i++) {
            lu6[i] = (int) (i * (255/63.0f) + .5f);
        }
        lut5 = lu5;
        lut6 = lu6;
    }

    //@formatter:off
    public static int load_BGR_565(Object base, long addr) {
        int val = UNSAFE.getShort(base, addr);
        int b = lut5[(val       ) & 31];
        int g = lut6[(val >>>  5) & 63];
        int r = lut5[(val >>> 11) & 31];
        return b | g << 8 | r << 16 | 0xff000000;
    }
    public static int load_RGBA_1010102(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        int r = (int) (((val       ) & 0x3ff) * (255.0f/1023) + .5f);
        int g = (int) (((val >>> 10) & 0x3ff) * (255.0f/1023) + .5f);
        int b = (int) (((val >>> 20) & 0x3ff) * (255.0f/1023) + .5f);
        int a = (int) (((val >>> 30)        ) * (255.0f/   3) + .5f);
        return b | g << 8 | r << 16 | a << 24;
    }
    public static int load_BGRA_1010102(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        int r = (int) (((val >>> 20) & 0x3ff) * (255.0f/1023) + .5f);
        int g = (int) (((val >>> 10) & 0x3ff) * (255.0f/1023) + .5f);
        int b = (int) (((val       ) & 0x3ff) * (255.0f/1023) + .5f);
        int a = (int) (((val >>> 30)        ) * (255.0f/   3) + .5f);
        return b | g << 8 | r << 16 | a << 24;
    }
    public static int load_R_8(Object base, long addr) {
        int val = UNSAFE.getByte(base, addr);
        return val << 16 | 0xff000000;
    }
    public static int load_RG_88(Object base, long addr) {
        int val = UNSAFE.getShort(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            return val << 8 | 0xff000000;
        } else {
            return (val & 0xff00) | (val & 0xff) << 16 | 0xff000000;
        }
    }
    @SuppressWarnings("PointlessArithmeticExpression")
    public static int load_RGB_888(Object base, long addr) {
        int r = UNSAFE.getByte(base, addr+0) & 0xff;
        int g = UNSAFE.getByte(base, addr+1) & 0xff;
        int b = UNSAFE.getByte(base, addr+2) & 0xff;
        return b | g << 8 | r << 16 | 0xff000000;
    }
    public static int load_RGBX_8888(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            return val >>> 8 | 0xff000000;
        } else {
            return (val & 0xff00) | (val & 0xff) << 16 | ((val >>> 16) & 0xff) | 0xff000000;
        }
    }
    public static int load_RGBA_8888(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            return val >>> 8 | val << 24;
        } else {
            return (val & 0xff00ff00) | (val & 0xff) << 16 | ((val >>> 16) & 0xff);
        }
    }
    public static int load_BGRA_8888(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            return Integer.reverseBytes(val);
        } else {
            return val;
        }
    }
    public static int load_ABGR_8888(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            return (val & 0xff00ff00) | (val & 0xff) << 16 | ((val >>> 16) & 0xff);
        } else {
            return val >>> 8 | (val & 0xff) << 24;
        }
    }
    public static int load_ARGB_8888(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            return val;
        } else {
            return Integer.reverseBytes(val);
        }
    }
    public static int load_GRAY_8(Object base, long addr) {
        int val = UNSAFE.getByte(base, addr) & 0xff;
        return val | val << 8 | val << 16 | 0xff000000;
    }
    public static int load_GRAY_ALPHA_88(Object base, long addr) {
        int val = UNSAFE.getShort(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            int lum = val & 0xff00;
            return lum << 8 | lum | lum >>> 8 | (val & 0xff) << 24;
        } else {
            int lum = val & 0xff;
            return (val << 16) | (lum << 8) | lum;
        }
    }
    public static int load_ALPHA_8(Object base, long addr) {
        int val = UNSAFE.getByte(base, addr);
        return val << 24;
    }
    public static int load_R_16(Object base, long addr) {
        int val = (int) ((UNSAFE.getShort(base, addr) & 0xffff) * (255/65535.0f) + .5f);
        return val << 16 | 0xff000000;
    }
    public static int load_RG_1616(Object base, long addr) {
        int val = UNSAFE.getInt(base, addr);
        int r, g;
        if (NATIVE_BIG_ENDIAN) {
            r = (int) (((val >>> 16)         ) * (255/65535.0f) + .5f);
            g = (int) (((val       ) & 0xffff) * (255/65535.0f) + .5f);
        } else {
            r = (int) (((val       ) & 0xffff) * (255/65535.0f) + .5f);
            g = (int) (((val >>> 16)         ) * (255/65535.0f) + .5f);
        }
        return g << 8 | r << 16 | 0xff000000;
    }
    public static int load_RGBA_16161616(Object base, long addr) {
        long val = UNSAFE.getLong(base, addr);
        int r, g, b, a;
        if (NATIVE_BIG_ENDIAN) {
            r = (int) (((val >>> 48)         ) * (255/65535.0f) + .5f);
            g = (int) (((val >>> 32) & 0xffff) * (255/65535.0f) + .5f);
            b = (int) (((val >>> 16) & 0xffff) * (255/65535.0f) + .5f);
            a = (int) (((val       ) & 0xffff) * (255/65535.0f) + .5f);
        } else {
            r = (int) (((val       ) & 0xffff) * (255/65535.0f) + .5f);
            g = (int) (((val >>> 16) & 0xffff) * (255/65535.0f) + .5f);
            b = (int) (((val >>> 32) & 0xffff) * (255/65535.0f) + .5f);
            a = (int) (((val >>> 48)         ) * (255/65535.0f) + .5f);
        }
        return b | g << 8 | r << 16 | a << 24;
    }
    public static int load_ALPHA_16(Object base, long addr) {
        int val = (int) ((UNSAFE.getShort(base, addr) & 0xffff) * (255/65535.0f) + .5f);
        return val << 24;
    }
    public static int load_R_F16(Object base, long addr) {
        int val = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr)) * 255 + .5f);
        return val << 16 | 0xff000000;
    }
    @SuppressWarnings("PointlessArithmeticExpression")
    public static int load_RG_F16(Object base, long addr) {
        int r = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr+0)) * 255 + .5f);
        int g = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr+2)) * 255 + .5f);
        return g << 8 | r << 16 | 0xff000000;
    }
    @SuppressWarnings("PointlessArithmeticExpression")
    public static int load_RGBA_F16(Object base, long addr) {
        int r = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr+0)) * 255 + .5f);
        int g = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr+2)) * 255 + .5f);
        int b = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr+4)) * 255 + .5f);
        int a = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr+6)) * 255 + .5f);
        return b | g << 8 | r << 16 | a << 24;
    }
    public static int load_ALPHA_F16(Object base, long addr) {
        int val = (int) (MathUtil.halfToFloat(UNSAFE.getShort(base, addr)) * 255 + .5f);
        return val << 24;
    }
    @SuppressWarnings("PointlessArithmeticExpression")
    public static int load_RGBA_F32(Object base, long addr) {
        int r = (int) (UNSAFE.getFloat(base, addr +  0) * 255 + .5f);
        int g = (int) (UNSAFE.getFloat(base, addr +  4) * 255 + .5f);
        int b = (int) (UNSAFE.getFloat(base, addr +  8) * 255 + .5f);
        int a = (int) (UNSAFE.getFloat(base, addr + 12) * 255 + .5f);
        return b | g << 8 | r << 16 | a << 24;
    }

    /**
     * Load a pixel value in low precision.
     */
    @Nonnull
    @Contract(pure = true)
    public static PixelLoad load(@ColorInfo.ColorType int ct) {
        return switch (ct) {
            case ColorInfo.CT_BGR_565       -> PixelUtils::load_BGR_565;
            case ColorInfo.CT_RGBA_1010102  -> PixelUtils::load_RGBA_1010102;
            case ColorInfo.CT_BGRA_1010102  -> PixelUtils::load_BGRA_1010102;
            case ColorInfo.CT_R_8           -> PixelUtils::load_R_8;
            case ColorInfo.CT_RG_88         -> PixelUtils::load_RG_88;
            case ColorInfo.CT_RGB_888       -> PixelUtils::load_RGB_888;
            case ColorInfo.CT_RGBX_8888     -> PixelUtils::load_RGBX_8888;
            case ColorInfo.CT_RGBA_8888     -> PixelUtils::load_RGBA_8888;
            case ColorInfo.CT_BGRA_8888     -> PixelUtils::load_BGRA_8888;
            case ColorInfo.CT_ABGR_8888     -> PixelUtils::load_ABGR_8888;
            case ColorInfo.CT_ARGB_8888     -> PixelUtils::load_ARGB_8888;
            case ColorInfo.CT_GRAY_8        -> PixelUtils::load_GRAY_8;
            case ColorInfo.CT_GRAY_ALPHA_88 -> PixelUtils::load_GRAY_ALPHA_88;
            case ColorInfo.CT_ALPHA_8       -> PixelUtils::load_ALPHA_8;
            case ColorInfo.CT_R_16          -> PixelUtils::load_R_16;
            case ColorInfo.CT_RG_1616       -> PixelUtils::load_RG_1616;
            case ColorInfo.CT_RGBA_16161616 -> PixelUtils::load_RGBA_16161616;
            case ColorInfo.CT_ALPHA_16      -> PixelUtils::load_ALPHA_16;
            case ColorInfo.CT_R_F16         -> PixelUtils::load_R_F16;
            case ColorInfo.CT_RG_F16        -> PixelUtils::load_RG_F16;
            case ColorInfo.CT_RGBA_F16      -> PixelUtils::load_RGBA_F16;
            case ColorInfo.CT_ALPHA_F16     -> PixelUtils::load_ALPHA_F16;
            case ColorInfo.CT_RGBA_F32      -> PixelUtils::load_RGBA_F32;
            default -> throw new AssertionError(ct);
        };
    }
    //@formatter:on

    /**
     * Store pixel value in low precision.
     */
    @FunctionalInterface
    public interface PixelStore {
        void store(Object base, long addr, @ColorInt int src);
    }

    //@formatter:off
    public static void store_BGR_565(Object base, long addr, int src) {
        int r = (src >>> 16) & 0xff;
        int g = (src >>>  8) & 0xff;
        int b = (src       ) & 0xff;
        // Round from [0,255] to [0,31] or [0,63], as if x * (31/255.0f) + 0.5f.
        // (Don't feel like you need to find some fundamental truth in these...
        // they were brute-force searched.)
        r = (r *  9 + 36) / 74; //  9/74 ≈ 31/255, plus 36/74, about half.
        g = (g * 21 + 42) / 85; // 21/85 = 63/255 exactly.
        b = (b *  9 + 36) / 74;
        UNSAFE.putShort(base, addr, (short) (b | g << 5 | r << 11));
    }
    public static void store_R_8(Object base, long addr, int src) {
        UNSAFE.putByte(base, addr, (byte) (src >>> 16));
    }
    public static void store_RG_88(Object base, long addr, int src) {
        if (NATIVE_BIG_ENDIAN) {
            // ARGB -> RG
            UNSAFE.putShort(base, addr, (short) (src >>> 8));
        } else {
            // ARGB -> GR
            UNSAFE.putShort(base, addr, (short) (((src >>> 16) & 0xff) | (src & 0xff00)));
        }
    }
    @SuppressWarnings("PointlessArithmeticExpression")
    public static void store_RGB_888(Object base, long addr, int src) {
        UNSAFE.putByte(base, addr+0, (byte) (src >>> 16));
        UNSAFE.putByte(base, addr+1, (byte) (src >>>  8));
        UNSAFE.putByte(base, addr+2, (byte) (src       ));
    }
    public static void store_RGBX_8888(Object base, long addr, int src) {
        if (NATIVE_BIG_ENDIAN) {
            // ARGB -> RGBX
            UNSAFE.putInt(base, addr, src << 8 | 0xff);
        } else {
            // ARGB -> XBGR
            UNSAFE.putInt(base, addr, (src & 0xff00) | (src & 0xff) << 16 | ((src >>> 16) & 0xff) | 0xff000000);
        }
    }
    public static void store_RGBA_8888(Object base, long addr, int src) {
        if (NATIVE_BIG_ENDIAN) {
            // ARGB -> RGBA
            UNSAFE.putInt(base, addr, src << 8 | src >>> 24);
        } else {
            // ARGB -> ABGR
            UNSAFE.putInt(base, addr, (src & 0xff00ff00) | (src & 0xff) << 16 | ((src >>> 16) & 0xff));
        }
    }
    public static void store_BGRA_8888(Object base, long addr, int src) {
        if (NATIVE_BIG_ENDIAN) {
            UNSAFE.putInt(base, addr, Integer.reverseBytes(src));
        } else {
            UNSAFE.putInt(base, addr, src);
        }
    }
    public static void store_ABGR_8888(Object base, long addr, int src) {
        if (NATIVE_BIG_ENDIAN) {
            // ARGB -> ABGR
            UNSAFE.putInt(base, addr, (src & 0xff00ff00) | (src & 0xff) << 16 | ((src >>> 16) & 0xff));
        } else {
            // ARGB -> RGBA
            UNSAFE.putInt(base, addr, src << 8 | src >>> 24);
        }
    }
    public static void store_ARGB_8888(Object base, long addr, int src) {
        if (NATIVE_BIG_ENDIAN) {
            UNSAFE.putInt(base, addr, src);
        } else {
            UNSAFE.putInt(base, addr, Integer.reverseBytes(src));
        }
    }
    public static void store_GRAY_8(Object base, long addr, int src) {
        float y = ((src >>> 16) & 0xff) * 0.2126f +
                  ((src >>>  8) & 0xff) * 0.7152f +
                  ((src       ) & 0xff) * 0.0722f;
        UNSAFE.putByte(base, addr, (byte) (y + .5f));
    }
    public static void store_GRAY_ALPHA_88(Object base, long addr, int src) {
        float y = ((src >>> 16) & 0xff) * 0.2126f +
                  ((src >>>  8) & 0xff) * 0.7152f +
                  ((src       ) & 0xff) * 0.0722f;
        if (NATIVE_BIG_ENDIAN) {
            UNSAFE.putShort(base, addr, (short) ((int) (y + .5f) << 8 | src >>> 24));
        } else {
            UNSAFE.putShort(base, addr, (short) ((int) (y + .5f) | ((src >>> 16) & 0xff00)));
        }
    }
    public static void store_ALPHA_8(Object base, long addr, int src) {
        UNSAFE.putByte(base, addr, (byte) (src >>> 24));
    }

    /**
     * Store a pixel value in low precision.
     */
    @Nonnull
    @Contract(pure = true)
    public static PixelStore store(@ColorInfo.ColorType int ct) {
        return switch (ct) {
            case ColorInfo.CT_BGR_565       -> PixelUtils::store_BGR_565;
            case ColorInfo.CT_R_8           -> PixelUtils::store_R_8;
            case ColorInfo.CT_RG_88         -> PixelUtils::store_RG_88;
            case ColorInfo.CT_RGB_888       -> PixelUtils::store_RGB_888;
            case ColorInfo.CT_RGBX_8888     -> PixelUtils::store_RGBX_8888;
            case ColorInfo.CT_RGBA_8888     -> PixelUtils::store_RGBA_8888;
            case ColorInfo.CT_BGRA_8888     -> PixelUtils::store_BGRA_8888;
            case ColorInfo.CT_ABGR_8888     -> PixelUtils::store_ABGR_8888;
            case ColorInfo.CT_ARGB_8888     -> PixelUtils::store_ARGB_8888;
            case ColorInfo.CT_GRAY_8        -> PixelUtils::store_GRAY_8;
            case ColorInfo.CT_GRAY_ALPHA_88 -> PixelUtils::store_GRAY_ALPHA_88;
            case ColorInfo.CT_ALPHA_8       -> PixelUtils::store_ALPHA_8;
            default -> throw new AssertionError(ct);
        };
    }
    //@formatter:on

    /**
     * Load or store pixel value in high precision.
     */
    @FunctionalInterface
    public interface PixelOp {
        void op(Object base, long addr, /*Color4f*/ float[] col);
    }

    //@formatter:off
    public static void load_BGR_565(Object base, long addr, float[] dst) {
        int val = UNSAFE.getShort(base, addr);
        dst[0] = (val & (31<<11)) * (1.0f / (31<<11));
        dst[1] = (val & (63<< 5)) * (1.0f / (63<< 5));
        dst[2] = (val & (31    )) * (1.0f / (31    ));
        dst[3] = 1.0f;
    }

    public static void load_RGBA_1010102(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        dst[0] = ((val       ) & 0x3ff) * (1.0f/1023);
        dst[1] = ((val >>> 10) & 0x3ff) * (1.0f/1023);
        dst[2] = ((val >>> 20) & 0x3ff) * (1.0f/1023);
        dst[3] = ((val >>> 30)        ) * (1.0f/   3);
    }

    public static void load_BGRA_1010102(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        dst[0] = ((val >>> 20) & 0x3ff) * (1.0f/1023);
        dst[1] = ((val >>> 10) & 0x3ff) * (1.0f/1023);
        dst[2] = ((val       ) & 0x3ff) * (1.0f/1023);
        dst[3] = ((val >>> 30)        ) * (1.0f/   3);
    }

    public static void load_R_8(Object base, long addr, float[] dst) {
        dst[0] = (UNSAFE.getByte(base, addr) & 0xff) * (1/255.0f);
        dst[1] = dst[2] = 0.0f;
        dst[3] = 1.0f;
    }

    public static void load_RG_88(Object base, long addr, float[] dst) {
        int val = UNSAFE.getShort(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>> 8) & 0xff) * (1/255.0f);
            dst[1] = ((val      ) & 0xff) * (1/255.0f);
        } else {
            dst[0] = ((val      ) & 0xff) * (1/255.0f);
            dst[1] = ((val >>> 8) & 0xff) * (1/255.0f);
        }
        dst[2] = 0.0f;
        dst[3] = 1.0f;
    }

    public static void load_RGB_888(Object base, long addr, float[] dst) {
        for (int i = 0; i < 3; i++) {
            dst[i] = (UNSAFE.getByte(base, addr+i) & 0xff) * (1/255.0f);
        }
        dst[3] = 1.0f;
    }

    public static void load_RGBX_8888(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>> 24)       ) * (1/255.0f);
            dst[1] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[2] = ((val >>>  8) & 0xff) * (1/255.0f);
        } else {
            dst[0] = ((val       ) & 0xff) * (1/255.0f);
            dst[1] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[2] = ((val >>> 16) & 0xff) * (1/255.0f);
        }
        dst[3] = 1.0f;
    }

    public static void load_RGBA_8888(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>> 24)       ) * (1/255.0f);
            dst[1] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[2] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[3] = ((val       ) & 0xff) * (1/255.0f);
        } else {
            dst[0] = ((val       ) & 0xff) * (1/255.0f);
            dst[1] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[2] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[3] = ((val >>> 24)       ) * (1/255.0f);
        }
    }

    public static void load_BGRA_8888(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[1] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[2] = ((val >>> 24)       ) * (1/255.0f);
            dst[3] = ((val       ) & 0xff) * (1/255.0f);
        } else {
            dst[0] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[1] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[2] = ((val       ) & 0xff) * (1/255.0f);
            dst[3] = ((val >>> 24)       ) * (1/255.0f);
        }
    }

    public static void load_ABGR_8888(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val       ) & 0xff) * (1/255.0f);
            dst[1] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[2] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[3] = ((val >>> 24)       ) * (1/255.0f);
        } else {
            dst[0] = ((val >>> 24)       ) * (1/255.0f);
            dst[1] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[2] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[3] = ((val       ) & 0xff) * (1/255.0f);
        }
    }

    public static void load_ARGB_8888(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[1] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[2] = ((val       ) & 0xff) * (1/255.0f);
            dst[3] = ((val >>> 24)       ) * (1/255.0f);
        } else {
            dst[0] = ((val >>>  8) & 0xff) * (1/255.0f);
            dst[1] = ((val >>> 16) & 0xff) * (1/255.0f);
            dst[2] = ((val >>> 24)       ) * (1/255.0f);
            dst[3] = ((val       ) & 0xff) * (1/255.0f);
        }
    }

    public static void load_GRAY_8(Object base, long addr, float[] dst) {
        float y = (UNSAFE.getByte(base, addr) & 0xff) * (1/255.0f);
        dst[0] = dst[1] = dst[2] = y;
        dst[3] = 1.0f;
    }

    public static void load_GRAY_ALPHA_88(Object base, long addr, float[] dst) {
        int val = UNSAFE.getShort(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            float y = ((val >>> 8) & 0xff) * (1/255.0f);
            dst[0] = dst[1] = dst[2] = y;
            dst[3] = ((val      ) & 0xff) * (1/255.0f);
        } else {
            float y = ((val      ) & 0xff) * (1/255.0f);
            dst[0] = dst[1] = dst[2] = y;
            dst[3] = ((val >>> 8) & 0xff) * (1/255.0f);
        }
    }

    public static void load_ALPHA_8(Object base, long addr, float[] dst) {
        dst[0] = dst[1] = dst[2] = 0.0f;
        dst[3] = (UNSAFE.getByte(base, addr) & 0xff) * (1/255.0f);
    }

    public static void load_R_16(Object base, long addr, float[] dst) {
        dst[0] = (UNSAFE.getShort(base, addr) & 0xffff) * (1/65535.0f);
        dst[1] = dst[2] = 0.0f;
        dst[3] = 1.0f;
    }

    public static void load_RG_1616(Object base, long addr, float[] dst) {
        int val = UNSAFE.getInt(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>> 16)         ) * (1/65535.0f);
            dst[1] = ((val       ) & 0xffff) * (1/65535.0f);
        } else {
            dst[0] = ((val       ) & 0xffff) * (1/65535.0f);
            dst[1] = ((val >>> 16)         ) * (1/65535.0f);
        }
        dst[2] = 0.0f;
        dst[3] = 1.0f;
    }

    public static void load_RGBA_16161616(Object base, long addr, float[] dst) {
        long val = UNSAFE.getLong(base, addr);
        if (NATIVE_BIG_ENDIAN) {
            dst[0] = ((val >>> 48)         ) * (1/65535.0f);
            dst[1] = ((val >>> 32) & 0xffff) * (1/65535.0f);
            dst[2] = ((val >>> 16) & 0xffff) * (1/65535.0f);
            dst[3] = ((val       ) & 0xffff) * (1/65535.0f);
        } else {
            dst[0] = ((val       ) & 0xffff) * (1/65535.0f);
            dst[1] = ((val >>> 16) & 0xffff) * (1/65535.0f);
            dst[2] = ((val >>> 32) & 0xffff) * (1/65535.0f);
            dst[3] = ((val >>> 48)         ) * (1/65535.0f);
        }
    }

    public static void load_ALPHA_16(Object base, long addr, float[] dst) {
        dst[0] = dst[1] = dst[2] = 0.0f;
        dst[3] = (UNSAFE.getShort(base, addr) & 0xffff) * (1/65535.0f);
    }

    public static void load_R_F16(Object base, long addr, float[] dst) {
        dst[0] = MathUtil.halfToFloat(UNSAFE.getShort(base, addr));
        dst[1] = dst[2] = 0.0f;
        dst[3] = 1.0f;
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public static void load_RG_F16(Object base, long addr, float[] dst) {
        dst[0] = MathUtil.halfToFloat(UNSAFE.getShort(base, addr+0));
        dst[1] = MathUtil.halfToFloat(UNSAFE.getShort(base, addr+2));
        dst[2] = 0.0f;
        dst[3] = 1.0f;
    }

    public static void load_RGBA_F16(Object base, long addr, float[] dst) {
        for (int i = 0; i < 4; i++) {
            dst[i] = MathUtil.halfToFloat(UNSAFE.getShort(base, addr + (i<<1)));
        }
    }

    public static void load_ALPHA_F16(Object base, long addr, float[] dst) {
        dst[0] = dst[1] = dst[2] = 0.0f;
        dst[3] = MathUtil.halfToFloat(UNSAFE.getShort(base, addr));
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public static void load_RGBA_F32(Object base, long addr, float[] dst) {
        dst[0] = UNSAFE.getFloat(base, addr +  0);
        dst[1] = UNSAFE.getFloat(base, addr +  4);
        dst[2] = UNSAFE.getFloat(base, addr +  8);
        dst[3] = UNSAFE.getFloat(base, addr + 12);
    }

    /**
     * Load a pixel value in high precision.
     */
    @Nonnull
    @Contract(pure = true)
    public static PixelOp loadOp(@ColorInfo.ColorType int ct) {
        return switch (ct) {
            case ColorInfo.CT_BGR_565       -> PixelUtils::load_BGR_565;
            case ColorInfo.CT_RGBA_1010102  -> PixelUtils::load_RGBA_1010102;
            case ColorInfo.CT_BGRA_1010102  -> PixelUtils::load_BGRA_1010102;
            case ColorInfo.CT_R_8           -> PixelUtils::load_R_8;
            case ColorInfo.CT_RG_88         -> PixelUtils::load_RG_88;
            case ColorInfo.CT_RGB_888       -> PixelUtils::load_RGB_888;
            case ColorInfo.CT_RGBX_8888     -> PixelUtils::load_RGBX_8888;
            case ColorInfo.CT_RGBA_8888     -> PixelUtils::load_RGBA_8888;
            case ColorInfo.CT_BGRA_8888     -> PixelUtils::load_BGRA_8888;
            case ColorInfo.CT_ABGR_8888     -> PixelUtils::load_ABGR_8888;
            case ColorInfo.CT_ARGB_8888     -> PixelUtils::load_ARGB_8888;
            case ColorInfo.CT_GRAY_8        -> PixelUtils::load_GRAY_8;
            case ColorInfo.CT_GRAY_ALPHA_88 -> PixelUtils::load_GRAY_ALPHA_88;
            case ColorInfo.CT_ALPHA_8       -> PixelUtils::load_ALPHA_8;
            case ColorInfo.CT_R_16          -> PixelUtils::load_R_16;
            case ColorInfo.CT_RG_1616       -> PixelUtils::load_RG_1616;
            case ColorInfo.CT_RGBA_16161616 -> PixelUtils::load_RGBA_16161616;
            case ColorInfo.CT_ALPHA_16      -> PixelUtils::load_ALPHA_16;
            case ColorInfo.CT_R_F16         -> PixelUtils::load_R_F16;
            case ColorInfo.CT_RG_F16        -> PixelUtils::load_RG_F16;
            case ColorInfo.CT_RGBA_F16      -> PixelUtils::load_RGBA_F16;
            case ColorInfo.CT_ALPHA_F16     -> PixelUtils::load_ALPHA_F16;
            case ColorInfo.CT_RGBA_F32      -> PixelUtils::load_RGBA_F32;
            default -> throw new AssertionError(ct);
        };
    }
    //@formatter:on

    //@formatter:off
    public static void store_BGR_565(Object base, long addr, float[] src) {
        int val = (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 31 + .5f)      |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 63 + .5f) << 5 |
                  (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 31 + .5f) << 11;
        UNSAFE.putShort(base, addr, (short) val);
    }

    public static void store_RGBA_1010102(Object base, long addr, float[] src) {
        int val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 1023 + .5f)       |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 1023 + .5f) << 10 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 1023 + .5f) << 20 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) *    3 + .5f) << 30;
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_BGRA_1010102(Object base, long addr, float[] src) {
        int val = (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 1023 + .5f)       |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 1023 + .5f) << 10 |
                  (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 1023 + .5f) << 20 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) *    3 + .5f) << 30;
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_R_8(Object base, long addr, float[] src) {
        byte val = (byte) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f);
        UNSAFE.putByte(base, addr, val);
    }

    public static void store_RG_88(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) << 8 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f)      ;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f)      |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) << 8 ;
        }
        UNSAFE.putShort(base, addr, (short) val);
    }

    public static void store_RGB_888(Object base, long addr, float[] src) {
        for (int i = 0; i < 3; i++) {
            UNSAFE.putByte(base, addr+i, (byte) (MathUtil.clamp(src[i], 0.0f, 1.0f) * 255 + .5f));
        }
    }

    public static void store_RGBX_8888(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) << 24 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  255;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f)       |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  255 << 24;
        }
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_RGBA_8888(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) << 24 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f)       ;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f)       |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f) << 24 ;
        }
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_BGRA_8888(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) << 24 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f)       ;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f)       |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f) << 24 ;
        }
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_ABGR_8888(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f)       |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f) << 24 ;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) << 24 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f)       ;
        }
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_ARGB_8888(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f)       |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f) << 24 ;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 255 + .5f) <<  8 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 255 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 255 + .5f) << 24 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f)       ;
        }
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_GRAY_8(Object base, long addr, float[] src) {
        float y = MathUtil.clamp(src[0], 0.0f, 1.0f) * 0.2126f +
                  MathUtil.clamp(src[1], 0.0f, 1.0f) * 0.7152f +
                  MathUtil.clamp(src[2], 0.0f, 1.0f) * 0.0722f;
        UNSAFE.putByte(base, addr, (byte) (y * 255 + .5f));
    }

    public static void store_GRAY_ALPHA_88(Object base, long addr, float[] src) {
        float y = MathUtil.clamp(src[0], 0.0f, 1.0f) * 0.2126f +
                  MathUtil.clamp(src[1], 0.0f, 1.0f) * 0.7152f +
                  MathUtil.clamp(src[2], 0.0f, 1.0f) * 0.0722f;
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (y * 255 + .5f) << 8 |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f)     ;
        } else {
            val = (int) (y * 255 + .5f)      |
                  (int) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f) << 8;
        }
        UNSAFE.putShort(base, addr, (short) val);
    }

    public static void store_ALPHA_8(Object base, long addr, float[] src) {
        byte val = (byte) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 255 + .5f);
        UNSAFE.putByte(base, addr, val);
    }

    public static void store_R_16(Object base, long addr, float[] src) {
        short val = (short) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 65535 + .5f);
        UNSAFE.putShort(base, addr, val);
    }

    public static void store_RG_1616(Object base, long addr, float[] src) {
        int val;
        if (NATIVE_BIG_ENDIAN) {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 65535 + .5f) << 16 |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 65535 + .5f)       ;
        } else {
            val = (int) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 65535 + .5f)       |
                  (int) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 65535 + .5f) << 16 ;
        }
        UNSAFE.putInt(base, addr, val);
    }

    public static void store_RGBA_16161616(Object base, long addr, float[] src) {
        long val;
        if (NATIVE_BIG_ENDIAN) {
            val = (long) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 65535 + .5f) << 48 |
                  (long) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 65535 + .5f) << 32 |
                  (long) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 65535 + .5f) << 16 |
                  (long) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 65535 + .5f)       ;
        } else {
            val = (long) (MathUtil.clamp(src[0], 0.0f, 1.0f) * 65535 + .5f)       |
                  (long) (MathUtil.clamp(src[1], 0.0f, 1.0f) * 65535 + .5f) << 16 |
                  (long) (MathUtil.clamp(src[2], 0.0f, 1.0f) * 65535 + .5f) << 32 |
                  (long) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 65535 + .5f) << 48 ;
        }
        UNSAFE.putLong(base, addr, val);
    }

    public static void store_ALPHA_16(Object base, long addr, float[] src) {
        short val = (short) (MathUtil.clamp(src[3], 0.0f, 1.0f) * 65535 + .5f);
        UNSAFE.putShort(base, addr, val);
    }

    public static void store_R_F16(Object base, long addr, float[] src) {
        UNSAFE.putShort(base, addr, MathUtil.floatToHalf(src[0]));
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public static void store_RG_F16(Object base, long addr, float[] src) {
        UNSAFE.putShort(base, addr+0, MathUtil.floatToHalf(src[0]));
        UNSAFE.putShort(base, addr+2, MathUtil.floatToHalf(src[1]));
    }

    public static void store_RGBA_F16(Object base, long addr, float[] src) {
        for (int i = 0; i < 4; i++) {
            UNSAFE.putShort(base, addr + (i<<1), MathUtil.floatToHalf(src[i]));
        }
    }

    public static void store_ALPHA_F16(Object base, long addr, float[] src) {
        UNSAFE.putShort(base, addr, MathUtil.floatToHalf(src[3]));
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public static void store_RGBA_F32(Object base, long addr, float[] src) {
        UNSAFE.putFloat(base, addr +  0, src[0]);
        UNSAFE.putFloat(base, addr +  4, src[1]);
        UNSAFE.putFloat(base, addr +  8, src[2]);
        UNSAFE.putFloat(base, addr + 12, src[3]);
    }

    /**
     * Store a pixel value in high precision.
     */
    @Nonnull
    @Contract(pure = true)
    public static PixelOp storeOp(@ColorInfo.ColorType int ct) {
        return switch (ct) {
            case ColorInfo.CT_BGR_565       -> PixelUtils::store_BGR_565;
            case ColorInfo.CT_RGBA_1010102  -> PixelUtils::store_RGBA_1010102;
            case ColorInfo.CT_BGRA_1010102  -> PixelUtils::store_BGRA_1010102;
            case ColorInfo.CT_R_8           -> PixelUtils::store_R_8;
            case ColorInfo.CT_RG_88         -> PixelUtils::store_RG_88;
            case ColorInfo.CT_RGB_888       -> PixelUtils::store_RGB_888;
            case ColorInfo.CT_RGBX_8888     -> PixelUtils::store_RGBX_8888;
            case ColorInfo.CT_RGBA_8888     -> PixelUtils::store_RGBA_8888;
            case ColorInfo.CT_BGRA_8888     -> PixelUtils::store_BGRA_8888;
            case ColorInfo.CT_ABGR_8888     -> PixelUtils::store_ABGR_8888;
            case ColorInfo.CT_ARGB_8888     -> PixelUtils::store_ARGB_8888;
            case ColorInfo.CT_GRAY_8        -> PixelUtils::store_GRAY_8;
            case ColorInfo.CT_GRAY_ALPHA_88 -> PixelUtils::store_GRAY_ALPHA_88;
            case ColorInfo.CT_ALPHA_8       -> PixelUtils::store_ALPHA_8;
            case ColorInfo.CT_R_16          -> PixelUtils::store_R_16;
            case ColorInfo.CT_RG_1616       -> PixelUtils::store_RG_1616;
            case ColorInfo.CT_RGBA_16161616 -> PixelUtils::store_RGBA_16161616;
            case ColorInfo.CT_ALPHA_16      -> PixelUtils::store_ALPHA_16;
            case ColorInfo.CT_R_F16         -> PixelUtils::store_R_F16;
            case ColorInfo.CT_RG_F16        -> PixelUtils::store_RG_F16;
            case ColorInfo.CT_RGBA_F16      -> PixelUtils::store_RGBA_F16;
            case ColorInfo.CT_ALPHA_F16     -> PixelUtils::store_ALPHA_F16;
            case ColorInfo.CT_RGBA_F32      -> PixelUtils::store_RGBA_F32;
            default -> throw new AssertionError(ct);
        };
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
     * Performs color type, alpha type, and color space conversion.
     * Addresses (offsets) must be aligned to bytes-per-pixel, scaling is not allowed.
     */
    public static boolean convertPixels(@Nonnull Pixmap src, @Nonnull Pixmap dst) {
        return convertPixels(src.getInfo(), src.getBase(), src.getAddress(), src.getRowBytes(),
                dst.getInfo(), dst.getBase(), dst.getAddress(), dst.getRowBytes(), false);
    }

    /**
     * Performs color type, alpha type, color space, and origin conversion.
     * Addresses (offsets) must be aligned to bytes-per-pixel (except for non-power-of-two),
     * scaling is not allowed.
     */
    public static boolean convertPixels(@Nonnull Pixmap src, @Nonnull Pixmap dst, boolean flipY) {
        return convertPixels(src.getInfo(), src.getBase(), src.getAddress(), src.getRowBytes(),
                dst.getInfo(), dst.getBase(), dst.getAddress(), dst.getRowBytes(), flipY);
    }

    /**
     * Performs color type, alpha type, and color space conversion.
     * Addresses (offsets) must be aligned to bytes-per-pixel, scaling is not allowed.
     */
    public static boolean convertPixels(@Nonnull ImageInfo srcInfo, Object srcBase,
                                        long srcAddr, long srcRowBytes,
                                        @Nonnull ImageInfo dstInfo, Object dstBase,
                                        long dstAddr, long dstRowBytes) {
        return convertPixels(srcInfo, srcBase, srcAddr, srcRowBytes,
                dstInfo, dstBase, dstAddr, dstRowBytes, false);
    }

    /**
     * Performs color type, alpha type, color space, and origin conversion.
     * Addresses (offsets) must be aligned to bytes-per-pixel (except for non-power-of-two),
     * scaling is not allowed.
     */
    public static boolean convertPixels(@Nonnull ImageInfo srcInfo, Object srcBase,
                                        long srcAddr, long srcRowBytes,
                                        @Nonnull ImageInfo dstInfo, Object dstBase,
                                        long dstAddr, long dstRowBytes,
                                        boolean flipY) {
        if (!srcInfo.isValid() || !dstInfo.isValid()) {
            return false;
        }
        if (srcInfo.width() != dstInfo.width() ||
                srcInfo.height() != dstInfo.height()) {
            return false;
        }
        if ((srcBase == null && srcAddr == 0) ||
                (dstBase == null && dstAddr == 0)) {
            return false;
        }
        if (srcRowBytes < srcInfo.minRowBytes() ||
                dstRowBytes < dstInfo.minRowBytes()) {
            return false;
        }
        int srcBpp = srcInfo.bytesPerPixel();
        int dstBpp = dstInfo.bytesPerPixel();
        if (srcRowBytes % srcBpp != 0 ||
                dstRowBytes % dstBpp != 0) {
            return false;
        }

        ColorSpace srcCS = srcInfo.colorSpace();
        @ColorInfo.ColorType var srcCT = srcInfo.colorType();
        @ColorInfo.AlphaType var srcAT = srcInfo.alphaType();
        ColorSpace dstCS = dstInfo.colorSpace();
        @ColorInfo.ColorType var dstCT = dstInfo.colorType();
        @ColorInfo.AlphaType var dstAT = dstInfo.alphaType();

        // Opaque outputs are treated as the same alpha type as the source input.
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
                    srcInfo.minRowBytes(), height, flipY);
            return true;
        }

        if (flipY) {
            dstAddr += dstRowBytes * (height - 1);
            dstRowBytes = -dstRowBytes;
        }

        if (flags == 0 && !csXform &&
                ColorInfo.maxBitsPerChannel(srcCT) <= 8 &&
                ColorInfo.maxBitsPerChannel(dstCT) <= 8) {
            // low precision pipeline
            final PixelLoad load = load(srcCT);
            final PixelStore store = store(dstCT);

            for (int i = 0; i < height; i++) {
                long nextSrcAddr = srcAddr + srcRowBytes;
                long nextDstAddr = dstAddr + dstRowBytes;
                for (int j = 0; j < width; j++) {
                    store.store(dstBase, dstAddr, load.load(srcBase, srcAddr));
                    srcAddr += srcBpp;
                    dstAddr += dstBpp;
                }
                srcAddr = nextSrcAddr;
                dstAddr = nextDstAddr;
            }
        } else {
            // high precision pipeline
            final PixelOp load = loadOp(srcCT);
            final boolean unpremul = (flags & kColorSpaceXformFlagUnpremul) != 0;
            final ColorSpace.Connector connector = csXform ? ColorSpace.connect(srcCS, dstCS) : null;
            final boolean premul = (flags & kColorSpaceXformFlagPremul) != 0;
            final PixelOp store = storeOp(dstCT);

            float[] col = new float[4];
            for (int i = 0; i < height; i++) {
                long nextSrcAddr = srcAddr + srcRowBytes;
                long nextDstAddr = dstAddr + dstRowBytes;
                for (int j = 0; j < width; j++) {
                    load.op(srcBase, srcAddr, col);
                    if (unpremul) {
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
                    if (premul) {
                        float scale = col[3];
                        col[0] *= scale;
                        col[1] *= scale;
                        col[2] *= scale;
                    }
                    store.op(dstBase, dstAddr, col);
                    srcAddr += srcBpp;
                    dstAddr += dstBpp;
                }
                srcAddr = nextSrcAddr;
                dstAddr = nextDstAddr;
            }
        }

        return true;
    }
}
