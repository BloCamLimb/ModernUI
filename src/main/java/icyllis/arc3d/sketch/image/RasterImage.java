/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch.image;

import icyllis.arc3d.core.*;
import icyllis.arc3d.sketch.Image;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public class RasterImage extends Image {

    public static final int COPY_MODE_IF_MUTABLE = 0;
    public static final int COPY_MODE_ALWAYS = 1;
    public static final int COPY_MODE_NEVER = 2;

    final Pixmap mPixmap;
    @SharedPtr
    PixelRef mPixelRef;

    /**
     * @param pixmap pixel map
     * @param pixelRef raw ptr to pixel ref
     */
    public RasterImage(@NonNull Pixmap pixmap,
                       @NonNull @RawPtr PixelRef pixelRef,
                       boolean mayBeMutable) {
        super(pixmap.getInfo());
        if (!(mayBeMutable || pixelRef.isImmutable())) {
            throw new IllegalArgumentException();
        }
        mPixmap = pixmap;
        mPixelRef = RefCnt.create(pixelRef);
    }

    @Nullable
    @SharedPtr
    public static Image makeFromBitmap(@NonNull Pixmap pixmap,
                                       @RawPtr PixelRef pixelRef) {
        return makeFromRasterBitmap(pixmap, pixelRef, COPY_MODE_IF_MUTABLE);
    }

    @Nullable
    @SharedPtr
    public static Image makeFromRasterBitmap(@NonNull Pixmap pixmap,
                                             @RawPtr PixelRef pixelRef,
                                             int copyMode) {
        if (pixelRef == null) {
            return null;
        }
        if (!pixmap.getInfo().isValid() || pixmap.getRowBytes() < pixmap.getInfo().minRowBytes()) {
            return null;
        }
        if (pixelRef.getAddress() == MemoryUtil.NULL && pixelRef.getBase() == null) {
            return null;
        }
        if (copyMode == COPY_MODE_ALWAYS || (!pixelRef.isImmutable() && copyMode != COPY_MODE_NEVER)) {
            long size = pixmap.getInfo().computeByteSize(pixmap.getRowBytes());
            if (size < 0) {
                return null;
            }
            long addr = MemoryUtil.nmemAlloc(size);
            if (addr == MemoryUtil.NULL) {
                return null;
            }
            PixelUtils.copyImage(
                    pixelRef.getBase(),
                    pixelRef.getAddress(),
                    pixmap.getRowBytes(),
                    null,
                    addr,
                    pixmap.getRowBytes(),
                    pixmap.getRowBytes(),
                    pixmap.getHeight()
            );
            Pixmap newPixmap = new Pixmap(pixmap.getInfo(),
                    null, addr, pixmap.getRowBytes());
            PixelRef newPixelRef = new PixelRef(pixmap.getWidth(), pixmap.getHeight(),
                    null, addr, pixmap.getRowBytes(), MemoryUtil::nmemFree);
            newPixelRef.setImmutable();
            Image result = new RasterImage(newPixmap, newPixelRef, false);
            newPixelRef.unref();
            return result;
        }
        return new RasterImage(pixmap, pixelRef, copyMode == COPY_MODE_NEVER);
    }

    @Override
    protected void deallocate() {
        mPixelRef = RefCnt.move(mPixelRef);
    }

    @Override
    public boolean isRasterBacked() {
        return true;
    }
}
