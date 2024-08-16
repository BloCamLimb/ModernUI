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

import icyllis.arc3d.engine.Context;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//TODO
public abstract class Image extends RefCnt {

    protected final ImageInfo mInfo;

    protected Image(@Nonnull ImageInfo info) {
        if (info.isEmpty()) {
            throw new IllegalArgumentException();
        }
        mInfo = info;
    }

    @Nonnull
    public ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Returns the full width of this image.
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return mInfo.width();
    }

    /**
     * Returns the full height of this image.
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return mInfo.height();
    }

    public void getBounds(Rect2i bounds) {
        bounds.set(0, 0, mInfo.width(), mInfo.height());
    }

    public void getBounds(Rect2f bounds) {
        bounds.set(0, 0, mInfo.width(), mInfo.height());
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

    public boolean isAlphaOnly() {
        return ColorInfo.colorTypeIsAlphaOnly(getColorType());
    }

    @ApiStatus.Internal
    @Nullable
    public Context getContext() {
        return null;
    }

    public abstract boolean isValid(@Nullable Context context);

    @ApiStatus.Internal
    public boolean isRasterBacked() {
        return false;
    }

    @ApiStatus.Internal
    public boolean isGpuBacked() {
        return false;
    }

    public long getGpuMemorySize() {
        return 0;
    }
}
