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

import javax.annotation.Nullable;
import java.util.function.LongConsumer;

/**
 * This class is the smart container for pixel memory.<br>
 * This class may be shared/accessed between multiple threads.
 */
public class PixelRef extends RefCnt {

    protected final int mWidth;
    protected final int mHeight;
    protected final long mAddress;
    protected final int mRowStride;
    protected final LongConsumer mFreeFn;

    protected boolean mImmutable;

    /**
     * Creates PixelRef from width, height.
     * <var>rowStride</var> should be width times bpp, or larger.
     * <var>freeFn</var> is used to free the <var>address</var>.
     *
     * @param address   address of pixel buffer, may be NULL
     * @param rowStride size of one row of buffer; width times bpp, or larger
     * @param freeFn    free function for native buffer; may be null
     */
    public PixelRef(int width,
                    int height,
                    @NativeType("void *") long address,
                    int rowStride,
                    @Nullable LongConsumer freeFn) {
        mWidth = width;
        mHeight = height;
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
                ", mAddress=0x" + Long.toHexString(mAddress) +
                ", mRowStride=" + mRowStride +
                ", mImmutable=" + mImmutable +
                '}';
    }
}
