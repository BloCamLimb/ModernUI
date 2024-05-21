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
import org.lwjgl.system.libc.LibCString;

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
     * Mem copy row by row.
     */
    public static void copyImage(long src, long srcRowBytes,
                                 long dst, long dstRowBytes,
                                 long minRowBytes, int rows) {
        if (srcRowBytes < minRowBytes || dstRowBytes < minRowBytes) {
            throw new IllegalArgumentException();
        }
        if (srcRowBytes == minRowBytes && dstRowBytes == minRowBytes) {
            LibCString.nmemcpy(dst, src, minRowBytes * rows);
        } else {
            while (rows-- != 0) {
                LibCString.nmemcpy(dst, src, minRowBytes);
                src += srcRowBytes;
                dst += dstRowBytes;
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
}
