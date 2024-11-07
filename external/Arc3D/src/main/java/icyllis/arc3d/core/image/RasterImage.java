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

package icyllis.arc3d.core.image;

import icyllis.arc3d.core.*;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RasterImage extends Image {

    public static final int COPY_MODE_IF_MUTABLE = 0;
    public static final int COPY_MODE_ALWAYS = 1;
    public static final int COPY_MODE_NEVER = 2;

    final Pixmap mPixmap;
    @SharedPtr
    Pixels mPixels;

    /**
     * @param pixmap pixel map
     * @param pixels raw ptr to pixel ref
     */
    public RasterImage(@Nonnull Pixmap pixmap,
                       @Nonnull @RawPtr Pixels pixels,
                       boolean mayBeMutable) {
        super(pixmap.getInfo());
        if (!(mayBeMutable || pixels.isImmutable())) {
            throw new IllegalArgumentException();
        }
        mPixmap = pixmap;
        mPixels = RefCnt.create(pixels);
    }

    @Nullable
    @SharedPtr
    public static Image makeFromBitmap(@Nonnull Pixmap pixmap,
                                       @RawPtr Pixels pixels) {
        return makeFromRasterBitmap(pixmap, pixels, COPY_MODE_IF_MUTABLE);
    }

    @Nullable
    @SharedPtr
    public static Image makeFromRasterBitmap(@Nonnull Pixmap pixmap,
                                             @RawPtr Pixels pixels,
                                             int copyMode) {
        if (pixels == null) {
            return null;
        }
        if (!pixmap.getInfo().isValid() || pixmap.getRowBytes() < pixmap.getInfo().minRowBytes()) {
            return null;
        }
        if (pixels.getAddress() == MemoryUtil.NULL && pixels.getBase() == null) {
            return null;
        }
        if (copyMode == COPY_MODE_ALWAYS || (!pixels.isImmutable() && copyMode != COPY_MODE_NEVER)) {
            long size = pixmap.getInfo().computeByteSize(pixmap.getRowBytes());
            if (size < 0) {
                return null;
            }
            long addr = MemoryUtil.nmemAlloc(size);
            if (addr == MemoryUtil.NULL) {
                return null;
            }
            PixelUtils.copyImage(
                    pixels.getBase(),
                    pixels.getAddress(),
                    pixmap.getRowBytes(),
                    null,
                    addr,
                    pixmap.getRowBytes(),
                    pixmap.getRowBytes(),
                    pixmap.getHeight()
            );
            Pixmap newPixmap = new Pixmap(pixmap.getInfo(),
                    null, addr, pixmap.getRowBytes());
            Pixels newPixels = new Pixels(pixmap.getWidth(), pixmap.getHeight(),
                    null, addr, pixmap.getRowBytes(), MemoryUtil::nmemFree);
            newPixels.setImmutable();
            Image result = new RasterImage(newPixmap, newPixels, false);
            newPixels.unref();
            return result;
        }
        return new RasterImage(pixmap, pixels, copyMode == COPY_MODE_NEVER);
    }

    @Override
    protected void deallocate() {
        mPixels = RefCnt.move(mPixels);
    }

    @Override
    public boolean isRasterBacked() {
        return true;
    }
}
