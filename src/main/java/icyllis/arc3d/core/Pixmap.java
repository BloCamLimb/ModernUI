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

import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Immutable structure that pairs ImageInfo with pixels and row stride.
 * <p>
 * This class does not try to manage the lifetime of pixels, see {@link Pixels}.
 */
public class Pixmap {

    @Nonnull
    protected final ImageInfo mInfo;
    @Nullable
    protected final WeakReference<Object> mBase;
    protected final long mAddress;
    protected final int mRowStride;

    /**
     * Creates {@link Pixmap} from info width, height, AlphaType, ColorType and ColorSpace.
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
    public Pixmap(@Nonnull ImageInfo info,
                  @Nullable Object base,
                  @NativeType("const void *") long address,
                  int rowStride) {
        this(info, base != null ? new WeakReference<>(base) : null, address, rowStride);
    }

    /**
     * Reinterprets an existing {@link Pixmap} with <var>newInfo</var>.
     */
    public Pixmap(@Nonnull ImageInfo newInfo,
                  @Nonnull Pixmap oldPixmap) {
        this(newInfo, oldPixmap.mBase, oldPixmap.mAddress, oldPixmap.mRowStride);
    }

    Pixmap(@Nonnull ImageInfo info,
           @Nullable WeakReference<Object> base,
           @NativeType("const void *") long address,
           int rowStride) {
        mInfo = Objects.requireNonNull(info);
        mBase = base;
        mAddress = address;
        mRowStride = rowStride;
    }

    @Nonnull
    public ImageInfo getInfo() {
        return mInfo;
    }

    public int getWidth() {
        return mInfo.width();
    }

    public int getHeight() {
        return mInfo.height();
    }

    @ColorInfo.ColorType
    public int getColorType() {
        return mInfo.colorType();
    }

    @ColorInfo.AlphaType
    public int getAlphaType() {
        return mInfo.alphaType();
    }

    @Nullable
    public ColorSpace getColorSpace() {
        return mInfo.colorSpace();
    }

    /**
     * The array if heap buffer; may be null.
     */
    @Nullable
    public Object getBase() {
        return mBase != null ? mBase.get() : null;
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
    public int getRowStride() {
        return mRowStride;
    }

    public boolean clear(float red, float green, float blue, float alpha,
                         @Nullable Rect2ic subset) {
        if (getColorType() == ColorInfo.CT_UNKNOWN) {
            return false;
        }

        var clip = new Rect2i(0, 0, getWidth(), getHeight());
        if (subset != null && !clip.intersect(subset)) {
            return true;
        }

        if (getColorType() == ColorInfo.CT_RGBA_8888) {
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
        return "Pixmap{" +
                "mInfo=" + mInfo +
                ", mBase=" + getBase() +
                ", mAddress=0x" + Long.toHexString(mAddress) +
                ", mRowStride=" + mRowStride +
                '}';
    }
}
