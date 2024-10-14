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
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Immutable structure that pairs ImageInfo with pixels and row bytes.
 * <p>
 * This class does not try to manage the lifetime of pixels, see {@link Pixels}.
 */
public class Pixmap {

    @Nonnull
    protected final ImageInfo mInfo;
    @Nullable
    protected final WeakReference<Object> mBase;
    protected final long mAddress;
    protected final int mRowBytes;

    /**
     * Creates {@link Pixmap} from info width, height, AlphaType, ColorType and ColorSpace.
     * <var>rowBytes</var> should be info.width() times info.bytesPerPixel(), or larger.
     * <p>
     * No parameter checking is performed; it is up to the caller to ensure that
     * <var>address</var> and <var>rowBytes</var> agree with <var>info</var>.
     * <p>
     * The memory lifetime of pixels is managed by the caller.
     *
     * @param info     width, height, AlphaType, ColorType and ColorSpace
     * @param base     array if heap buffer; may be null
     * @param address  address if native buffer, or array base offset; may be NULL
     * @param rowBytes size of one row of buffer; width times bpp, or larger
     */
    public Pixmap(@Nonnull ImageInfo info,
                  @Nullable Object base,
                  @NativeType("const void *") long address,
                  int rowBytes) {
        this(info, base != null ? new WeakReference<>(base) : null, address, rowBytes);
    }

    /**
     * Reinterprets an existing {@link Pixmap} with <var>newInfo</var>.
     */
    public Pixmap(@Nonnull ImageInfo newInfo,
                  @Nonnull Pixmap oldPixmap) {
        this(newInfo, oldPixmap.mBase, oldPixmap.mAddress, oldPixmap.mRowBytes);
    }

    Pixmap(@Nonnull ImageInfo info,
           @Nullable WeakReference<Object> base,
           @NativeType("const void *") long address,
           int rowBytes) {
        mInfo = Objects.requireNonNull(info);
        mBase = base;
        mAddress = address;
        mRowBytes = rowBytes;
    }

    /**
     * Returns width, height, AlphaType, ColorType, and ColorSpace.
     */
    @Nonnull
    public ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Returns pixel count in each pixel row. Should be equal or less than:
     * rowBytes() / info().bytesPerPixel().
     *
     * @return pixel width in ImageInfo
     */
    public int getWidth() {
        return mInfo.width();
    }

    /**
     * Returns pixel row count.
     *
     * @return pixel height in ImageInfo
     */
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

    /**
     * Returns ColorSpace, the range of colors, associated with ImageInfo.
     */
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
     * The address if native buffer, the base address corresponding to the pixel origin,
     * or array base offset; may be NULL. See {@link sun.misc.Unsafe}.
     */
    public long getAddress() {
        return mAddress;
    }

    /**
     * The size, in bytes, between the start of one pixel row/scanline and the next in buffer,
     * including any unused padding between them. This value must be at least the width multiplied
     * by the bytes-per-pixel, where the bytes-per-pixel depends on the color type.
     * <p>
     * Returns zero if colorType() is unknown. It is up to the caller to ensure that row bytes is
     * a useful value.
     */
    public int getRowBytes() {
        return mRowBytes;
    }

    /**
     * Returns address at (x, y). Returns NULL if {@link #getAddress()} is NULL.
     * <p>
     * Input is not validated, and return value is undefined if ColorType is unknown.
     *
     * @param x column index, zero or greater, and less than width()
     * @param y row index, zero or greater, and less than height()
     * @return readable generic pointer/offset to pixel
     */
    public long getAddress(int x, int y) {
        assert x < getWidth();
        assert y < getHeight();
        long addr = mAddress;
        if (addr != MemoryUtil.NULL) {
            addr += (long) y * mRowBytes + (long) x * mInfo.bytesPerPixel();
        }
        return addr;
    }

    @Nullable
    public Pixmap makeSubset(@Nonnull Rect2ic subset) {
        var r = new Rect2i(0, 0, getWidth(), getHeight());
        if (!r.intersect(subset)) {
            return null;
        }

        assert (r.x() < getWidth());
        assert (r.y() < getHeight());

        return new Pixmap(
                getInfo().makeWH(r.width(), r.height()), mBase,
                getAddress(r.x(), r.y()), mRowBytes
        );
    }

    /**
     * Copies a Rect of pixels to dst. Copy starts at (srcX, srcY), and does not
     * exceed Pixmap (width(), height()). dst specifies width, height, ColorType,
     * AlphaType, and ColorSpace of destination.  Returns true if pixels are copied.
     * Returns false if dst address equals nullptr, or dst.rowBytes() is less than
     * dst ImageInfo::minRowBytes.
     * <p>
     * Pixels are copied only if pixel conversion is possible. Returns
     * false if pixel conversion is not possible.
     * <p>
     * srcX and srcY may be negative to copy only top or left of source. Returns
     * false pixmap width() or height() is zero or negative. Returns false if:
     * srcX >= pixmap width(), or if srcY >= pixmap height().
     *
     * @param dst  ImageInfo and pixel address to write to
     * @param srcX column index whose absolute value is less than width()
     * @param srcY row index whose absolute value is less than height()
     * @return true if pixels are copied to dst
     */
    public boolean readPixels(@Nonnull Pixmap dst, int srcX, int srcY) {
        ImageInfo dstInfo = dst.getInfo();
        if (!getInfo().isValid() || !dstInfo.isValid()) {
            return false;
        }

        if (getAddress() == MemoryUtil.NULL ||
                dst.getAddress() == MemoryUtil.NULL ||
                dst.getRowBytes() < dstInfo.minRowBytes()) {
            return false;
        }
        if (srcX < 0 || srcY < 0 ||
                srcX + dstInfo.width() > getWidth() ||
                srcY + dstInfo.height() > getHeight()) {
            return false;
        }

        long srcAddr = getAddress(srcX, srcY);
        ImageInfo srcInfo = getInfo().makeWH(dstInfo.width(), dstInfo.height());
        return PixelUtils.convertPixels(
                srcInfo, getBase(), srcAddr, getRowBytes(),
                dstInfo, dst.getBase(), dst.getAddress(), dst.getRowBytes()
        );
    }

    /**
     * Copies a Rect of pixels from src. Copy starts at (dstX, dstY), and does not exceed
     * (src.width(), src.height()).
     * <p>
     * src specifies width, height, ColorType, AlphaType, ColorSpace, pixel storage,
     * and row bytes of source. src.rowBytes() specifics the gap from one source
     * row to the next. Returns true if pixels are copied. Returns false if:
     * - src pixel storage equals nullptr
     * - src.rowBytes is less than ImageInfo::minRowBytes()
     * <p>
     * Pixels are copied only if pixel conversion is possible. Returns
     * false if pixel conversion is not possible.
     * <p>
     * dstX and dstY may be negative to copy only top or left of source. Returns
     * false if width() or height() is zero or negative.
     * Returns false if dstX >= pixmap width(), or if dstY >= pixmap height().
     *
     * @param src  source Pixmap: ImageInfo, pixels, row bytes
     * @param dstX column index whose absolute value is less than width()
     * @param dstY row index whose absolute value is less than height()
     * @return true if src pixels are copied to pixmap
     */
    public boolean writePixels(@Nonnull Pixmap src, int dstX, int dstY) {
        ImageInfo srcInfo = src.getInfo();
        if (!getInfo().isValid() || !srcInfo.isValid()) {
            return false;
        }

        if (getAddress() == MemoryUtil.NULL ||
                src.getAddress() == MemoryUtil.NULL ||
                src.getRowBytes() < srcInfo.minRowBytes()) {
            return false;
        }
        if (dstX < 0 || dstY < 0 ||
                dstX + srcInfo.width() > getWidth() ||
                dstY + srcInfo.height() > getHeight()) {
            return false;
        }

        long dstAddr = getAddress(dstX, dstY);
        ImageInfo dstInfo = getInfo().makeWH(srcInfo.width(), srcInfo.height());
        return PixelUtils.convertPixels(
                srcInfo, src.getBase(), src.getAddress(), src.getRowBytes(),
                dstInfo, getBase(), dstAddr, getRowBytes()
        );
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
                long addr = getAddress() + (long) y * getRowBytes() + (long) clip.x() << 2;
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
                ", mRowBytes=" + mRowBytes +
                '}';
    }
}
