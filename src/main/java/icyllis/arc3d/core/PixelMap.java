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
import java.lang.ref.WeakReference;

/**
 * Immutable structure that pairs ImageInfo with pixels and row stride.
 * <p>
 * This class does not try to manage the lifetime of pixels, see {@link PixelRef}.
 */
public class PixelMap {

    protected final ImageInfo mInfo;
    protected final WeakReference<Object> mBase;
    protected final long mAddress;
    protected final int mRowStride;

    /**
     * Creates PixelMap from info width, height, AlphaType, ColorType and ColorSpace.
     * <var>rowStride</var> should be info.width() times info.bytesPerPixel(), or larger.
     * <p>
     * No parameter checking is performed; it is up to the caller to ensure that
     * <var>address</var> and <var>rowStride</var> agree with <var>info</var>.
     * <p>
     * The memory lifetime of pixels is managed by the caller.
     *
     * @param info      width, height, AlphaType, ColorType and ColorSpace
     * @param base      array if heap buffer; may be null
     * @param address   address if native buffer, or array base offset; may be NULL
     * @param rowStride size of one row of buffer; width times bpp, or larger
     */
    public PixelMap(ImageInfo info,
                    @Nullable @NativeType("jarray") Object base,
                    @NativeType("const void *") long address,
                    int rowStride) {
        mInfo = info;
        mBase = new WeakReference<>(base);
        mAddress = address;
        mRowStride = rowStride;
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

    @Nullable
    public Object getBase() {
        return mBase.get();
    }

    public long getAddress() {
        return mAddress;
    }

    public int getRowStride() {
        return mRowStride;
    }

    public boolean clear(float red, float green, float blue, float alpha,
                         @Nullable Rect2ic subset) {
        if (getColorType() == ImageInfo.CT_UNKNOWN) {
            return false;
        }

        var clip = new Rect2i(0, 0, getWidth(), getHeight());
        if (subset != null && !clip.intersect(subset)) {
            return true;
        }

        if (getColorType() == ImageInfo.CT_RGBA_8888) {
            int c = ((int) (alpha * 255.0f + 0.5f) << 24) |
                    ((int) (blue * 255.0f + 0.5f) << 16) |
                    ((int) (green * 255.0f + 0.5f) << 8) |
                    (int) (red * 255.0f + 0.5f);

            Object base = getBase();
            for (int y = clip.mTop; y < clip.mBottom; ++y) {
                long addr = getAddress() + (long) y * getRowStride() + (long) clip.x() << 2;
                PixelUtils.setPixel32(base, addr, c, clip.width());
            }

            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "PixelMap{" +
                "mInfo=" + mInfo +
                ", mBase=" + mBase.get() +
                ", mAddress=0x" + Long.toHexString(mAddress) +
                ", mRowStride=" + mRowStride +
                '}';
    }
}
