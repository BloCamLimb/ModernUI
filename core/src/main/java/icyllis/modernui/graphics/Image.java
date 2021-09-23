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

import icyllis.modernui.graphics.texture.GLTexture;
import icyllis.modernui.graphics.texture.TextureManager;

import javax.annotation.Nonnull;

/**
 * This class represents OpenGL 2D textures at high-level, which is used for drawing
 * and processing in the application layer.
 */
public class Image {

    private final GLTexture mTexture;

    public Image() {
        mTexture = new GLTexture(GLWrapper.GL_TEXTURE_2D);
    }

    public Image(@Nonnull GLTexture texture) {
        mTexture = texture;
    }

    /**
     * Creates a new image object representing the target resource image.
     *
     * @param namespace the application namespace
     * @param subPath   the sub path to the resource
     * @return image
     */
    @Nonnull
    public static Image create(@Nonnull String namespace, @Nonnull String subPath) {
        return new Image(TextureManager.getInstance().getOrCreate(namespace, subPath));
    }

    /**
     * Returns the backing texture.
     *
     * @return OpenGL texture
     */
    @Nonnull
    public final GLTexture getTexture() {
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
