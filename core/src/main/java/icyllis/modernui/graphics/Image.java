/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.graphics.engine.RecordingContext;
import icyllis.modernui.graphics.engine.SurfaceProxyView;
import icyllis.modernui.graphics.opengl.*;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * {@code Image} describes a two-dimensional array of pixels to draw. The pixels are
 * located in GPU memory as a GPU texture.
 * <ul>
 * <li>{@code Image} cannot be modified after it is created.</li>
 * <li>{@code Image} width and height are greater than zero.</li>
 * <li>{@code Image} may be created from {@link Bitmap}, Surface, Picture
 * and GPU texture.</li>
 * </ul>
 */
//TODO wip
public final class Image {

    private ImageInfo mInfo;

    private RecordingContext mContext;
    private SurfaceProxyView mView;

    private final GLTextureCompat mTexture;

    @ApiStatus.Experimental
    public Image() {
        mTexture = new GLTextureCompat(GLCore.GL_TEXTURE_2D);
    }

    @ApiStatus.Experimental
    public Image(@Nonnull GLTextureCompat texture) {
        mTexture = texture;
        mInfo = ImageInfo.make(texture.getWidth(), texture.getHeight(), 0, 0, null);
    }

    /**
     * Creates a new image object representing the target resource image.
     * You should use a single image as the UI texture to avoid each icon creating its own image.
     * Underlying resources are automatically released.
     *
     * @param ns      the application namespace
     * @param subPath the sub path to the resource
     * @return the image
     */
    @Nonnull
    public static Image create(@Nonnull String ns, @Nonnull String subPath) {
        return new Image(GLTextureManager.getInstance().getOrCreate(ns, "textures/" + subPath,
                GLTextureManager.CACHE_MASK | GLTextureManager.MIPMAP_MASK));
    }

    /**
     * Returns the {@link ImageInfo} describing the width, height, color type, alpha type
     * and color space of this image.
     *
     * @return image info
     */
    public ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Returns the view width of this image (as its texture).
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return mInfo.width();
    }

    /**
     * Returns the view height of this image (as its texture).
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return mInfo.height();
    }

    /**
     * Returns the backing texture.
     *
     * @return OpenGL texture
     */
    @ApiStatus.Experimental
    @Nonnull
    public GLTextureCompat getTexture() {
        return mTexture;
    }
}
