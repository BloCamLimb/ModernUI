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

package icyllis.modernui.graphics.texture;

import icyllis.modernui.ModernUI;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import java.lang.ref.Cleaner.Cleanable;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL 2D texture objects at low-level. The OpenGL texture
 * associated with this object may be changed by recycling. Losing the
 * reference of this object will delete the texture. The more underlying
 * way is directly using OpenGL texture name.
 */
@SuppressWarnings("unused")
public class Texture2D implements AutoCloseable {

    @Nullable
    private Ref mRef;

    /**
     * Creates an instance of Texture2D on application side from any thread.
     */
    public Texture2D() {
    }

    /**
     * Returns the OpenGL texture object name represented by this object.
     * It will be generated if it's unassigned. This operation does not
     * allocate GPU memory unless {@link #init} called.
     *
     * @return texture object name
     */
    public int getId() {
        if (mRef == null)
            mRef = new Ref(this);
        return mRef.id;
    }

    /**
     * Binds this texture to TEXTURE_2D target.
     */
    public void bind() {
        bindTexture(GL_TEXTURE_2D, getId());
    }

    /**
     * Specifies this texture and allocates GPU memory dynamically (compatibility).
     * The image data will be undefined after calling this method, unless an upload.
     * <p>
     * This method can be called multiple times, which represents re-specifying this texture.
     *
     * @param internalFormat how image data stored in GPU
     * @param mipmapLevel    max mipmap level, min is 0
     * @see #init(int, int, int, int)
     */
    public void initCompat(int internalFormat, int width, int height, int mipmapLevel) {
        bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, mipmapLevel);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f);

        // null ptr represents not modifying the image data, but allocating enough memory
        for (int level = 0; level <= mipmapLevel; level++) {
            nglTexImage2D(GL_TEXTURE_2D, level, internalFormat, width >> level,
                    height >> level, 0, GL_RED, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        }
    }

    /**
     * Specifies this texture with an immutable storage unless deleted (core-profile).
     * The image data will be undefined after calling this method, unless an upload.
     * <p>
     * For automatic mipmap generation, you may need manually clear the mipmap data later,
     * otherwise the content may not be replaced and keep the previous undefined data,
     * especially for translucent texture.
     * <p>
     * When using mipmap, texture size must be power of two, and at least 2^mipmapLevel
     *
     * @param internalFormat how image data stored in GPU
     * @param mipmapLevel    max mipmap level, min is 0
     */
    public void init(int internalFormat, int width, int height, int mipmapLevel) {
        bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, mipmapLevel);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f);
        glTexStorage2D(GL_TEXTURE_2D, mipmapLevel, internalFormat, width, height);
    }

    /**
     * Upload image data to GPU. Alignment for {@link icyllis.modernui.platform.Bitmap} is 1 (byte-aligned).
     *
     * @param level     the level for the image
     * @param rowLength row length if data width is not equal to texture width, or 0
     * @param alignment pixel row alignment 1, 2, 4, 8
     * @param format    the format of the data to upload
     * @param type      the type of the data to upload
     * @param pixels    the pixels data pointer
     */
    public void upload(int level, int x, int y, int width, int height, int rowLength, int skipRows,
                       int skipPixels, int alignment, int format, int type, long pixels) {
        bind();
        glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, skipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, skipPixels);
        glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
        nglTexSubImage2D(GL_TEXTURE_2D, level, x, y, width, height, format, type, pixels);
    }

    /**
     * Set wrap mode. This texture must be bound first.
     */
    public void setWrap(int wrapS, int wrapT) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);
    }

    /**
     * Set filter mode. When mipmap = true, sampling between mipmaps is always linear.
     * This texture must be bound first.
     *
     * @see #setFilter(int, int)
     */
    public void setFilter(boolean linear, boolean mipmap) {
        if (linear) {
            if (mipmap) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            }
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            if (mipmap) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            }
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    // This texture must be bound first.
    public void setFilter(int minFilter, int magFilter) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter);
    }

    /**
     * Generates mipmaps. This texture must be bound first.
     */
    public void generateMipmap() {
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    /**
     * Query texture width from GPU. This texture must be bound first.
     *
     * @return texture width
     */
    public int getWidth() {
        return glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WIDTH);
    }

    // This texture must be bound first.
    public int getHeight() {
        return glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_HEIGHT);
    }

    @Override
    public final void close() {
        if (mRef != null) {
            mRef.cleanup.clean();
            mRef = null;
        }
    }

    private static final class Ref implements Runnable {

        private final int id;
        private final Cleanable cleanup;

        private Ref(Texture2D owner) {
            id = glGenTextures();
            cleanup = ModernUI.cleaner().register(owner, this);
        }

        @Override
        public void run() {
            // if (id == INVALID_ID)
            // cleanup is synchronized, this method only called once
            deleteTextureAsync(GL_TEXTURE_2D, id);
            // id = INVALID_ID;
        }
    }
}
