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

import org.lwjgl.system.NativeType;
import org.lwjgl.system.jni.JNINativeInterface;

import javax.annotation.Nullable;
import java.util.function.LongConsumer;

/**
 * This class is the smart container for pixel memory.<br>
 * This class may be shared/accessed between multiple threads.
 */
public class PixelRef extends RefCnt {

    protected final int mWidth;
    protected final int mHeight;
    protected final Object mBase;
    protected final long mAddress;
    protected final int mRowStride;
    protected final LongConsumer mFreeFn;

    protected boolean mImmutable;

    /**
     * Creates PixelRef from width, height.
     * <var>rowStride</var> should be width times bpp, or larger.
     * <var>freeFn</var> is used to free the <var>address</var>.
     *
     * @param base      heap buffer; may be null
     * @param address   native buffer or 0; may be NULL
     * @param rowStride size of one row of buffer; width times bpp, or larger
     * @param freeFn    free function for native buffer; may be null
     */
    public PixelRef(int width,
                    int height,
                    @Nullable @NativeType("jarray") Object base,
                    @NativeType("void *") long address,
                    int rowStride,
                    @Nullable LongConsumer freeFn) {
        mWidth = width;
        mHeight = height;
        mBase = base;
        mAddress = address;
        mRowStride = rowStride;
        mFreeFn = freeFn;
    }

    @Override
    protected void deallocate() {
        if (mFreeFn != null) {
            mFreeFn.accept(mAddress);
        }
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @Nullable
    public Object getBase() {
        return mBase;
    }

    public long getAddress() {
        return mAddress;
    }

    public int getRowStride() {
        return mRowStride;
    }

    /**
     * Returns true if this ref is marked as immutable, meaning that the
     * contents of its pixels will not change for the lifetime of the ref.
     */
    public boolean isImmutable() {
        return mImmutable;
    }

    /**
     * Marks this ref is immutable, meaning that the contents of its
     * pixels will not change for the lifetime of the ref. This state can
     * be set on a ref, but it cannot be cleared once it is set.
     */
    public void setImmutable() {
        mImmutable = true;
    }

    @Override
    public String toString() {
        return "PixelRef{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mBase=" + mBase +
                ", mAddress=0x" + Long.toHexString(mAddress) +
                ", mRowStride=" + mRowStride +
                ", mImmutable=" + mImmutable +
                '}';
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
        if (clazz == int[].class) {
            elems = JNINativeInterface.nGetIntArrayElements((int[]) base, 0);
        } else if (clazz == short[].class) {
            elems = JNINativeInterface.nGetShortArrayElements((short[]) base, 0);
        } else if (clazz == byte[].class) {
            elems = JNINativeInterface.nGetByteArrayElements((byte[]) base, 0);
        } else if (clazz == float[].class) {
            elems = JNINativeInterface.nGetFloatArrayElements((float[]) base, 0);
        } else {
            throw new UnsupportedOperationException();
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
        if (clazz == int[].class) {
            JNINativeInterface.nReleaseIntArrayElements((int[]) base, elems, mode);
        } else if (clazz == short[].class) {
            JNINativeInterface.nReleaseShortArrayElements((short[]) base, elems, mode);
        } else if (clazz == byte[].class) {
            JNINativeInterface.nReleaseByteArrayElements((byte[]) base, elems, mode);
        } else if (clazz == float[].class) {
            JNINativeInterface.nReleaseFloatArrayElements((float[]) base, elems, mode);
        } else {
            throw new UnsupportedOperationException();
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
        if (clazz == int[].class) {
            JNINativeInterface.nGetIntArrayRegion((int[]) base, start, len, buf);
        } else if (clazz == short[].class) {
            JNINativeInterface.nGetShortArrayRegion((short[]) base, start, len, buf);
        } else if (clazz == byte[].class) {
            JNINativeInterface.nGetByteArrayRegion((byte[]) base, start, len, buf);
        } else if (clazz == float[].class) {
            JNINativeInterface.nGetFloatArrayRegion((float[]) base, start, len, buf);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
