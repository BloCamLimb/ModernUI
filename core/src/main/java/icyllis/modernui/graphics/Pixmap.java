/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics;

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeType;

import java.util.Objects;

/**
 * Pixmap provides a utility to pair ImageInfo with pixels and row bytes.
 * Pixmap is a low level class which provides convenience functions to access
 * raster destinations.
 * <p>
 * Pixmap does not try to manage the lifetime of the pixel memory.
 *
 * @see Bitmap
 */
@ApiStatus.Internal
public sealed class Pixmap permits Bitmap {

    final ImageInfo mInfo;
    private final long mPixels;
    private final int mRowStride;

    /**
     * Creates Pixmap from info width, height, AlphaType, and ColorType.
     * <var>addr</var> points to pixels, or nullptr. <var>rowBytes</var> should be
     * info.width() times info.bytesPerPixel(), or larger.
     * <p>
     * No parameter checking is performed; it is up to the caller to ensure that
     * <var>addr</var> and <var>rowBytes</var> agree with info.
     * <p>
     * The memory lifetime of pixels is managed by the caller. When Pixmap
     * becomes phantom-reachable, <var>addr</var> is unaffected.
     *
     * @param info      width, height, AlphaType, ColorType of ImageInfo
     * @param addr      pointer to pixels allocated by caller; may be null
     * @param rowStride size of one row of addr; width times pixel size, or larger
     */
    public Pixmap(@NonNull ImageInfo info,
                  @NativeType("const void *") long addr,
                  int rowStride) {
        mInfo = Objects.requireNonNull(info);
        mPixels = addr;
        mRowStride = rowStride;
    }

    public ImageInfo getInfo() {
        return mInfo;
    }

    public long getPixels() {
        return mPixels;
    }

    public int getRowStride() {
        return mRowStride;
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

    public ColorSpace getColorSpace() {
        return mInfo.colorSpace();
    }

    @Override
    public String toString() {
        return "Pixmap{" +
                "info=" + mInfo +
                ", address=0x" + Long.toHexString(mPixels) +
                ", rowStride=" + mRowStride +
                '}';
    }
}
