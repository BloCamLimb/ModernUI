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

import org.lwjgl.system.MemoryUtil;

import java.lang.ref.Cleaner;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL 2D texture objects at low-level. The OpenGL texture
 * associated with this object may be changed by recycling.
 */
public class Texture2D extends GLTexture {

    /**
     * Creates an instance of Texture2D on application side from any thread.
     */
    public Texture2D() {
    }

    @Override
    public final int getTarget() {
        return GL_TEXTURE_2D;
    }

    /**
     * Binds this texture to TEXTURE_2D target.
     */
    public void bind() {
        bindTexture(GL_TEXTURE_2D, get());
    }

    /**
     * Specifies this texture and allocates GPU memory dynamically (compatibility).
     * The image data will be undefined after calling this method, unless an upload.
     * <p>
     * This method can be called multiple times, which represents re-specifying this texture.
     *
     * @param internalFormat sized internal format used for the image on GPU side
     * @param maxLevel       max mipmap level, min is 0
     * @see #initCore(int, int, int, int)
     */
    public void initCompat(int internalFormat, int width, int height, int maxLevel) {
        if (maxLevel < 0) {
            throw new IllegalArgumentException();
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException();
        }
        bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, maxLevel);

        // null ptr represents not modifying the image data, but allocates memory
        for (int level = 0; level <= maxLevel; level++) {
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
     * @param internalFormat sized internal format used for the image on GPU side
     * @param maxLevel       max mipmap level, min is 0
     */
    public void initCore(int internalFormat, int width, int height, int maxLevel) {
        if (maxLevel < 0) {
            throw new IllegalArgumentException();
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException();
        }
        final int texture = get();
        // mipmap generation is from (baseLevel + 1) to max level
        glTextureParameteri(texture, GL_TEXTURE_BASE_LEVEL, 0);
        glTextureParameteri(texture, GL_TEXTURE_MAX_LEVEL, maxLevel);

        // min lod is 0 because we generate mipmap based at level 0
        // so there's no larger images
        glTextureParameteri(texture, GL_TEXTURE_MIN_LOD, 0);
        glTextureParameteri(texture, GL_TEXTURE_MAX_LOD, maxLevel);

        // the number of levels is maxLevel + 1, because it's [0, maxLevel]
        glTextureStorage2D(texture, maxLevel + 1, internalFormat, width, height);
    }

    /**
     * Regenerate the texture to the new size.
     *
     * @param width  new width of the texture
     * @param height new height of the texture
     * @param copy   true to copy the level 0 image data from the old one to the new one
     */
    public void resize(int width, int height, boolean copy) {
        int oldTex = get();
        int oldWidth = getWidth(0);
        int oldHeight = getHeight(0);
        if (width == oldWidth && height == oldHeight) {
            return;
        }
        int internalFormat = glGetTextureLevelParameteri(oldTex, 0, GL_TEXTURE_INTERNAL_FORMAT);
        int maxLevel = glGetTextureParameteri(oldTex, GL_TEXTURE_MAX_LEVEL);

        Cleaner.Cleanable cleanup = recreate();
        initCompat(internalFormat, width, height, maxLevel);
        if (copy) {
            glCopyImageSubData(oldTex, GL_TEXTURE_2D, 0, 0, 0, 0,
                    get(), GL_TEXTURE_2D, 0, 0, 0, 0,
                    Math.min(width, oldWidth), Math.min(height, oldHeight), 1);
            if (maxLevel > 0) {
                generateMipmap();
            }
        }
        if (cleanup != null) {
            cleanup.clean();
        }
    }

    /**
     * Clear the image with zeros.
     *
     * @param level the level of the image
     */
    public void clear(int level) {
        nglClearTexImage(get(), level, GL_RED, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
    }

    public void clear(int level, int x, int y, int width, int height) {
        nglClearTexSubImage(get(), level, x, y, 0, width, height, 1, GL_RED, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
    }

    /**
     * Upload image data to GPU. {@link icyllis.modernui.platform.Bitmap} is byte-aligned.
     *
     * @param level     the level for the image
     * @param rowLength row length if data width is not equal to texture width, or 0
     * @param alignment pixel row alignment 1, 2, 4, 8
     * @param format    the format of the data to upload, one of GL_RED, GL_RG, GL_RGB,
     *                  GL_BGR, GL_RGBA, GL_BGRA, GL_DEPTH_COMPONENT, and GL_STENCIL_INDEX.
     * @param type      the type of the data to upload, for example, unsigned byte
     * @param pixels    the native pointer of pixels data
     */
    public void upload(int level, int x, int y, int width, int height, int rowLength, int skipRows,
                       int skipPixels, int alignment, int format, int type, long pixels) {
        glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, skipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, skipPixels);
        glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
        nglTextureSubImage2D(get(), level, x, y, width, height, format, type, pixels);
    }

    /**
     * Set wrap mode.
     */
    public void setWrap(int wrapS, int wrapT) {
        final int texture = get();
        glTextureParameteri(texture, GL_TEXTURE_WRAP_S, wrapS);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_T, wrapT);
    }

    /**
     * Set filter mode. When mipmap = true, sampling between mipmaps is always linear.
     *
     * @see #setFilter(int, int)
     */
    public void setFilter(boolean linear, boolean mipmap) {
        final int texture = get();
        if (linear) {
            if (mipmap) {
                glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            } else {
                glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            }
            glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            if (mipmap) {
                glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            } else {
                glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            }
            glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void setFilter(int minFilter, int magFilter) {
        final int texture = get();
        glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, minFilter);
        glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, magFilter);
    }

    // eg: swizzleRGBA(GL_ONE, GL_ONE, GL_ONE, GL_RED)
    // then red channel will be read as alpha channel by <shader>, RGB is always 1
    public void swizzleRGBA(int... rgbaMask) {
        glTextureParameteriv(get(), GL_TEXTURE_SWIZZLE_RGBA, rgbaMask);
    }

    /**
     * Generates mipmaps.
     */
    public void generateMipmap() {
        glGenerateTextureMipmap(get());
    }

    /**
     * Query texture width from GPU.
     *
     * @return texture width
     */
    public int getWidth(int level) {
        return glGetTextureLevelParameteri(get(), level, GL_TEXTURE_WIDTH);
    }

    public int getHeight(int level) {
        return glGetTextureLevelParameteri(get(), level, GL_TEXTURE_HEIGHT);
    }
}
