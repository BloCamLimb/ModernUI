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

import org.jspecify.annotations.Nullable;
import org.lwjgl.system.NativeType;

import java.util.function.LongConsumer;

/**
 * This class is the smart container for pixel memory.<br>
 * This class may be shared/accessed between multiple threads.
 */
public class Pixels extends RefCnt {

    protected final int mWidth;
    protected final int mHeight;
    protected final Object mBase;
    protected final long mAddress;
    protected final int mRowBytes;
    protected final LongConsumer mFreeFn;

    protected boolean mImmutable;

    /**
     * Creates {@link Pixels} from width, height.
     * <var>rowBytes</var> should be width times bpp, or larger.
     * <var>freeFn</var> is used to free the <var>address</var>.
     *
     * @param base     array if heap buffer; may be null
     * @param address  address if native buffer, or array base offset; may be NULL
     * @param rowBytes size of one row of buffer; width times bpp, or larger
     * @param freeFn   free function for native buffer; may be null
     */
    public Pixels(int width,
                  int height,
                  @Nullable Object base,
                  @NativeType("void *") long address,
                  int rowBytes,
                  @Nullable LongConsumer freeFn) {
        mWidth = width;
        mHeight = height;
        mBase = base;
        mAddress = address;
        mRowBytes = rowBytes;
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

    /**
     * The array if heap buffer; may be null.
     */
    @Nullable
    public Object getBase() {
        return mBase;
    }

    /**
     * The address if native buffer, or array base offset; may be NULL.
     */
    public long getAddress() {
        return mAddress;
    }

    /**
     * The size, in bytes, between the start of one pixel row/scanline and the next in buffer,
     * including any unused padding between them. This value must be at least the width multiplied
     * by the bytes-per-pixel, where the bytes-per-pixel depends on the color type.
     */
    public int getRowBytes() {
        return mRowBytes;
    }

    /**
     * Returns true if this container is marked as immutable, meaning that the
     * contents of its pixels will not change for the lifetime of the container.
     */
    public boolean isImmutable() {
        return mImmutable;
    }

    /**
     * Marks this container is immutable, meaning that the contents of its
     * pixels will not change for the lifetime of the container. This state can
     * be set on a container, but it cannot be cleared once it is set.
     */
    public void setImmutable() {
        mImmutable = true;
    }

    @Override
    public String toString() {
        return "Pixels{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                ", mBase=" + mBase +
                ", mAddress=0x" + Long.toHexString(mAddress) +
                ", mRowBytes=" + mRowBytes +
                ", mImmutable=" + mImmutable +
                '}';
    }
}
