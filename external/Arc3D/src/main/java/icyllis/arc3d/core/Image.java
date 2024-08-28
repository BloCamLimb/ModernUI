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

/**
 * Image describes a two-dimensional array of pixels to draw. The pixels may be
 * decoded in a raster bitmap, encoded in a Picture or compressed data stream,
 * or located in GPU memory as a GPU texture.
 * <p>
 * Image cannot be modified after it is created. Image may allocate additional
 * storage as needed; for instance, an encoded Image may decode when drawn.
 * <p>
 * Image width and height are greater than zero. Creating an Image with zero width
 * or height returns Image equal to null.
 * <p>
 * Image may be created from Bitmap, Pixmap, Surface, Picture, encoded streams,
 * GPU texture, YUV_ColorSpace data, or hardware buffer. Encoded streams supported
 * include BMP, GIF, JPEG, PNG, WBMP, TGA, PSD, HDR, PNM, PIC, TIFF.
 */
public abstract class Image extends RefCnt {

    protected final ImageInfo mInfo;
    protected final UniqueID mUniqueID;

    protected Image(@Nonnull ImageInfo info) {
        if (info.isEmpty()) {
            throw new IllegalArgumentException();
        }
        mInfo = info;
        mUniqueID = new UniqueID();
    }

    @Nonnull
    public final ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Returns the full width of this image.
     *
     * @return image width in pixels
     */
    public final int getWidth() {
        return mInfo.width();
    }

    /**
     * Returns the full height of this image.
     *
     * @return image height in pixels
     */
    public final int getHeight() {
        return mInfo.height();
    }

    public final void getBounds(@Nonnull Rect2i bounds) {
        bounds.set(0, 0, mInfo.width(), mInfo.height());
    }

    public final void getBounds(@Nonnull Rect2f bounds) {
        bounds.set(0, 0, mInfo.width(), mInfo.height());
    }

    /**
     * Returns object unique to image. Image contents cannot change after Image is
     * created. Any operation to create a new Image will receive generate a new
     * unique object.
     *
     * @return unique identifier
     */
    @Nonnull
    public final UniqueID getUniqueID() {
        return mUniqueID;
    }

    @ColorInfo.ColorType
    public final int getColorType() {
        return mInfo.colorType();
    }

    @ColorInfo.AlphaType
    public final int getAlphaType() {
        return mInfo.alphaType();
    }

    @Nullable
    public final ColorSpace getColorSpace() {
        return mInfo.colorSpace();
    }

    public final boolean isAlphaOnly() {
        return ColorInfo.colorTypeIsAlphaOnly(getColorType());
    }

    @ApiStatus.Internal
    @Nullable
    public Context getContext() {
        return null;
    }

    @ApiStatus.Internal
    public boolean isRasterBacked() {
        return false;
    }

    @ApiStatus.Internal
    public boolean isTextureBacked() {
        return false;
    }

    public long getTextureSize() {
        return 0;
    }
}
