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
import icyllis.arc3d.engine.Context;
import icyllis.arc3d.engine.RecordingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RasterImage extends Image {

    final PixelMap mPixelMap;
    PixelRef mPixelRef;

    /**
     * @param pixelMap pixel map
     * @param pixelRef raw ptr to pixel ref
     */
    public RasterImage(@Nonnull PixelMap pixelMap,
                       @Nonnull @RawPtr PixelRef pixelRef) {
        super(pixelMap.getInfo());
        mPixelMap = pixelMap;
        mPixelRef = RefCnt.create(pixelRef);
    }

    @Override
    protected void deallocate() {
        mPixelRef = RefCnt.move(mPixelRef);
    }

    @Override
    public boolean isValid(@Nullable Context context) {
        return true;
    }

    @Override
    public boolean isRasterBacked() {
        return true;
    }
}
