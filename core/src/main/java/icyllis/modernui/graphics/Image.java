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

import icyllis.modernui.graphics.opengl.GLCore;
import icyllis.modernui.graphics.opengl.GLTextureCompat;
import icyllis.modernui.graphics.opengl.TextureManager;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * This class is associated with an OpenGL 2D texture object or a Vulkan 2D image object,
 * used with API drawing and processing methods.
 */
//TODO wip
public class Image {

    private final GLTextureCompat mTexture;

    @ApiStatus.Experimental
    public Image() {
        mTexture = new GLTextureCompat(GLCore.GL_TEXTURE_2D);
    }

    @ApiStatus.Experimental
    public Image(@Nonnull GLTextureCompat texture) {
        mTexture = texture;
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
        return new Image(TextureManager.getInstance().getOrCreate(ns, "textures/" + subPath,
                TextureManager.CACHE_MASK | TextureManager.MIPMAP_MASK));
    }

    /**
     * Returns the backing texture.
     *
     * @return OpenGL texture
     */
    @ApiStatus.Experimental
    @Nonnull
    public final GLTextureCompat getTexture() {
        return mTexture;
    }

    /**
     * Returns the full width of this image (as its texture).
     *
     * @return image width in pixels
     */
    public final int getWidth() {
        return mTexture.getWidth();
    }

    /**
     * Returns the full height of this image (as its texture).
     *
     * @return image height in pixels
     */
    public final int getHeight() {
        return mTexture.getHeight();
    }
}
