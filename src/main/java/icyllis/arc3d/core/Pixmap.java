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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import sun.misc.Unsafe;

import java.util.Objects;

import static icyllis.arc3d.core.PixelUtils.UNSAFE;

/**
 * Immutable structure that pairs ImageInfo with pixels and row bytes.
 * <p>
 * This class does not try to manage the lifetime of pixels, unless it's backed
 * by a heap array, use {@link PixelRef} to manage the native pixel memory.
 */
public class Pixmap {

    @NonNull
    protected final ImageInfo mInfo;
    @Nullable
    protected final Object mBase;
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
    public Pixmap(@NonNull ImageInfo info,
                  @Nullable Object base,
                  @NativeType("const void *") long address,
                  int rowBytes) {
        mInfo = Objects.requireNonNull(info);
        mBase = base;
        mAddress = address;
        mRowBytes = rowBytes;
    }

    /**
     * Reinterprets an existing {@link Pixmap} with <var>newInfo</var>.
     */
    public Pixmap(@NonNull ImageInfo newInfo,
                  @NonNull Pixmap oldPixmap) {
        this(newInfo, oldPixmap.mBase, oldPixmap.mAddress, oldPixmap.mRowBytes);
    }

    /**
     * Returns width, height, AlphaType, ColorType, and ColorSpace.
     */
    @NonNull
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
        return mBase;
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
     * Returns address/offset at (x, y). Returns NULL if {@link #getAddress()} is NULL.
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

    /**
     * Make a pixmap width, height, pixel address to intersection of this with subset,
     * if intersection is not empty; and return the new pixmap. Otherwise, return null.
     *
     * @param subset bounds to intersect with SkPixmap
     * @return a pixmap if intersection of this and subset is not empty
     */
    @Nullable
    public Pixmap makeSubset(@NonNull Rect2ic subset) {
        var r = new Rect2i(0, 0, getWidth(), getHeight());
        if (!r.intersect(subset)) {
            return null;
        }

        assert (r.x() < getWidth());
        assert (r.y() < getHeight());

        return new Pixmap(
                getInfo().makeWH(r.width(), r.height()), getBase(),
                getAddress(r.x(), r.y()), getRowBytes()
        );
    }

    /**
     * Returns origin of pixels within Pixels pix. Pixmap bounds is always contained
     * by Pixels bounds, which may be the same size or larger. Multiple Pixmap
     * can share the same Pixels instance, where each Pixmap has different bounds.
     * <p>
     * The returned origin added to Pixmap dimensions equals or is smaller than the
     * Pixels dimensions.
     * <p>
     * Returns (0, 0) if pixels is NULL.
     */
    public void getPixelOrigin(long pix,
                               @Size(2) int @NonNull [] origin) {
        long addr = getAddress();
        long rb = getRowBytes();
        if (pix == MemoryUtil.NULL || rb == 0) {
            origin[0] = 0;
            origin[1] = 0;
        } else {
            assert addr >= pix;
            long off = addr - pix;
            origin[0] = (int) ((off % rb) / getInfo().bytesPerPixel());
            origin[1] = (int) (off / rb);
        }
    }

    /**
     * Gets the pixel value at (x, y), and converts it to {@link ColorInfo#CT_BGRA_8888_NATIVE},
     * {@link ColorInfo#AT_UNPREMUL}, and {@link ColorSpace.Named#SRGB}.
     * <p>
     * Input is not validated: out of bounds values of x or y trigger an assertion error;
     * and returns undefined values or may crash. Fails if color type is unknown or
     * pixel data is NULL.
     * <p>
     * If the max bits per channel for the color type is greater than 8, or colors are premultiplied,
     * then color precision may be lost in the conversion. Otherwise, precision will not be lost.
     * If the color space is not sRGB, then this method will perform color space transformation,
     * which can be slow.
     *
     * @param x column index, zero or greater, and less than width()
     * @param y row index, zero or greater, and less than height()
     * @return pixel converted to unpremultiplied color
     * @see #getColor4f(int, int, float[])
     */
    @ColorInt
    public int getColor(int x, int y) {
        assert getAddress() != MemoryUtil.NULL;
        assert x < getWidth();
        assert y < getHeight();
        Object base = getBase();
        long addr = getAddress(x, y);
        var ct = getColorType();
        var at = getAlphaType();
        var cs = getColorSpace();
        if (at == ColorInfo.AT_PREMUL || (cs != null && !cs.isSrgb())) {
            var srcInfo = new ImageInfo(1, 1, ct, at, cs);
            var dstInfo = new ImageInfo(1, 1, ColorInfo.CT_BGRA_8888_NATIVE,
                    ColorInfo.AT_UNPREMUL, ColorSpace.get(ColorSpace.Named.SRGB));
            int[] col = new int[1];
            boolean res = PixelUtils.convertPixels(
                    srcInfo, base, addr, getRowBytes(),
                    dstInfo, col, Unsafe.ARRAY_INT_BASE_OFFSET, getRowBytes()
            );
            assert res;
            return col[0];
        } else {
            // no alpha type and color space conversion
            return PixelUtils.load(ct)
                    .load(base, addr);
        }
    }

    /**
     * Gets the pixel at (x, y), and converts it to {@link ColorInfo#CT_RGBA_F32}.
     * This method will not perform alpha type or color space transformation,
     * the resulting color has {@link #getAlphaType()} and is in {@link #getColorSpace()}.
     * <p>
     * Input is not validated: out of bounds values of x or y trigger an assertion error;
     * and returns undefined values or may crash. Fails if color type is unknown or
     * pixel data is NULL.
     *
     * @param x   column index, zero or greater, and less than width()
     * @param y   row index, zero or greater, and less than height()
     * @param dst pixel converted to float color
     */
    public void getColor4f(int x, int y, @Size(4) float @NonNull [] dst) {
        assert getAddress() != MemoryUtil.NULL;
        assert x < getWidth();
        assert y < getHeight();
        PixelUtils.loadOp(getColorType())
                .op(getBase(), getAddress(x, y), dst);
    }

    /**
     * Sets the pixel at (x, y), from {@link ColorInfo#CT_RGBA_F32} to {@link #getColorType()}.
     * This method will not perform alpha type or color space transformation,
     * the given color should have {@link #getAlphaType()} and be in {@link #getColorSpace()}.
     * <p>
     * Input is not validated: out of bounds values of x or y trigger an assertion error;
     * and returns undefined values or may crash. Fails if color type is unknown or
     * pixel data is NULL.
     *
     * @param x   column index, zero or greater, and less than width()
     * @param y   row index, zero or greater, and less than height()
     * @param src float color to set
     */
    public void setColor4f(int x, int y, @Size(4) float @NonNull [] src) {
        assert getAddress() != MemoryUtil.NULL;
        assert x < getWidth();
        assert y < getHeight();
        PixelUtils.storeOp(getColorType())
                .op(getBase(), getAddress(x, y), src);
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
    public boolean readPixels(@NonNull Pixmap dst, int srcX, int srcY) {
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
    public boolean writePixels(@NonNull Pixmap src, int dstX, int dstY) {
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

    /**
     * Writes color to pixels bounded by subset; returns true on success.
     * if subset is null, writes colors pixels inside bounds(). Returns false if
     * {@link #getColorType()} is unknown, if subset is not null and does
     * not intersect bounds(), or if subset is null and bounds() is empty.
     * <p>
     * This method will not perform alpha type or color space transformation,
     * the given color should have {@link #getAlphaType()} and be in {@link #getColorSpace()}.
     *
     * @param color  float color to write
     * @param subset bounding box of pixels to write; may be null
     * @return true if pixels are changed
     */
    public boolean clear(@Size(4) float @NonNull [] color,
                         @Nullable Rect2ic subset) {
        var ct = getColorType();
        if (ct == ColorInfo.CT_UNKNOWN) {
            return false;
        }

        var clip = new Rect2i(0, 0, getWidth(), getHeight());
        if (subset != null && !clip.intersect(subset)) {
            return false;
        }

        Object base = getBase();
        int bpp = ColorInfo.bytesPerPixel(ct);

        // RGBA_F32 is the only type with a bpp of 16
        if (ct == ColorInfo.CT_RGBA_F32) {
            assert bpp == 16;
            if (Float.floatToRawIntBits(color[0]) == 0 &&
                    Float.floatToRawIntBits(color[1]) == 0 &&
                    Float.floatToRawIntBits(color[2]) == 0 &&
                    Float.floatToRawIntBits(color[3]) == 0) {
                // fill with zeros
                long rowBytes = (long) clip.width() * 16;
                if (base != null) {
                    for (int y = clip.mTop; y < clip.mBottom; ++y) {
                        UNSAFE.setMemory(base,
                                getAddress(clip.x(), y), rowBytes, (byte) 0);
                    }
                } else {
                    for (int y = clip.mTop; y < clip.mBottom; ++y) {
                        MemoryUtil.memSet(
                                getAddress(clip.x(), y), 0, rowBytes);
                    }
                }
            } else {
                for (int y = clip.mTop; y < clip.mBottom; ++y) {
                    long addr = getAddress(clip.x(), y);
                    for (int i = 0, e = clip.width(); i < e; ++i) {
                        PixelUtils.store_RGBA_F32(base, addr, color);
                        addr += 16;
                    }
                }
            }
            return true;
        }

        assert bpp >= 1 && bpp <= 8;

        try (var stack = MemoryStack.stackPush()) {
            // convert to ct
            long dst = stack.nmalloc(8, 8);
            PixelUtils.storeOp(ct)
                    .op(null, dst, color);

            boolean fast = true;
            byte v0 = UNSAFE.getByte(dst);
            for (int i = 1; i < bpp; ++i) {
                byte v = UNSAFE.getByte(dst + i);
                if (v != v0) {
                    fast = false;
                    break;
                }
            }
            if (fast) {
                // fill with value
                long rowBytes = (long) clip.width() * bpp;
                if (base != null) {
                    for (int y = clip.mTop; y < clip.mBottom; ++y) {
                        UNSAFE.setMemory(base,
                                getAddress(clip.x(), y), rowBytes, v0);
                    }
                } else {
                    for (int y = clip.mTop; y < clip.mBottom; ++y) {
                        MemoryUtil.memSet(
                                getAddress(clip.x(), y), v0 & 0xff, rowBytes);
                    }
                }
            } else if (ct == ColorInfo.CT_RGB_888) {
                // RGB_888 is the only type where bpp is not a power of 2
                assert bpp == 3;
                byte v1 = UNSAFE.getByte(dst + 1);
                byte v2 = UNSAFE.getByte(dst + 2);
                for (int y = clip.mTop; y < clip.mBottom; ++y) {
                    long addr = getAddress(clip.x(), y);
                    for (int i = 0, e = clip.width(); i < e; ++i) {
                        UNSAFE.putByte(base, addr, v0);
                        UNSAFE.putByte(base, addr + 1, v1);
                        UNSAFE.putByte(base, addr + 2, v2);
                        addr += 3;
                    }
                }
            } else if (bpp == 2) {
                short value = UNSAFE.getShort(dst);
                for (int y = clip.mTop; y < clip.mBottom; ++y) {
                    long addr = getAddress(clip.x(), y);
                    PixelUtils.setPixel16(base, addr, value, clip.width());
                }
            } else if (bpp == 4) {
                int value = UNSAFE.getInt(dst);
                for (int y = clip.mTop; y < clip.mBottom; ++y) {
                    long addr = getAddress(clip.x(), y);
                    PixelUtils.setPixel32(base, addr, value, clip.width());
                }
            } else if (bpp == 8) {
                long value = UNSAFE.getLong(dst);
                for (int y = clip.mTop; y < clip.mBottom; ++y) {
                    long addr = getAddress(clip.x(), y);
                    PixelUtils.setPixel64(base, addr, value, clip.width());
                }
            } else {
                assert false;
            }

            return true;
        }
    }

    @Override
    public String toString() {
        return "Pixmap{" +
                "mInfo=" + mInfo +
                ", mBase=" + mBase +
                ", mAddress=0x" + Long.toHexString(mAddress) +
                ", mRowBytes=" + mRowBytes +
                '}';
    }
}
