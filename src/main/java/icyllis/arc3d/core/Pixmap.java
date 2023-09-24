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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * This class is the smart container for pixel memory and provides a utility
 * to pair ImageInfo with pixels and row bytes.<br>
 * This class may be shared/accessed between multiple threads.
 */
public class Pixmap extends RefCnt {

    final ImageInfo mInfo;
    final long mPixels;
    // XXX: row stride is always (width * bpp) now
    final int mRowStride;

    @Nonnull
    private final LongConsumer mFreeFn;

    private boolean mImmutable;

    /**
     * Creates Pixmap from info width, height, AlphaType, ColorType and ColorSpace.
     * <var>addr</var> points to pixels, or NULL. <var>rowBytes</var> should be
     * info.width() times info.bytesPerPixel(), or larger. <var>freeFn</var> is
     * used to free the <var>addr</var>.
     * <p>
     * No parameter checking is performed; it is up to the caller to ensure that
     * <var>addr</var> and <var>rowBytes</var> agree with info.
     * <p>
     * The memory lifetime of pixels is managed by the caller. When Pixmap
     * becomes phantom-reachable, <var>addr</var> is unaffected.
     *
     * @param info      width, height, AlphaType, ColorType, ColorSpace of ImageInfo
     * @param addr      pointer to pixels allocated by caller; may be null
     * @param rowStride size of one row of addr; width times pixel size, or larger
     */
    public Pixmap(@Nonnull ImageInfo info,
                  @NativeType("const void *") long addr,
                  int rowStride,
                  @Nonnull LongConsumer freeFn) {
        mInfo = Objects.requireNonNull(info);
        mPixels = addr;
        mRowStride = rowStride;
        mFreeFn = freeFn;
    }

    @Override
    protected void deallocate() {
        mFreeFn.accept(mPixels);
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

    public int getColorType() {
        return mInfo.colorType();
    }

    public int getAlphaType() {
        return mInfo.alphaType();
    }

    @Nullable
    public ColorSpace getColorSpace() {
        return mInfo.colorSpace();
    }

    /**
     * @return the address whether freed or not
     */
    public long getPixels() {
        return mPixels;
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

    @Nonnull
    @Override
    public String toString() {
        return "Pixels{" +
                "mAddress=0x" + Long.toHexString(mPixels) +
                ", mInfo=" + mInfo +
                ", mRowStride=" + mRowStride +
                ", mImmutable=" + mImmutable +
                '}';
    }
}
