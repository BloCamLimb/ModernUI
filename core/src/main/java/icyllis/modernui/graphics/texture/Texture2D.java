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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL 2D texture objects at low-level. The OpenGL texture
 * associated with this object may be changed by recycling.
 */
public class Texture2D extends Texture {

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
     * @param internalFormat how image data stored in GPU
     * @param mipmapLevel    max mipmap level, min is 0
     * @see #init(int, int, int, int)
     */
    @Deprecated
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
     * @param internalFormat sized internal format used to store the image in GPU
     * @param mipmapLevel    max mipmap level, min is 0
     */
    public void init(int internalFormat, int width, int height, int mipmapLevel) {
        final int texture = get();
        glTextureParameteri(texture, GL_TEXTURE_BASE_LEVEL, 0);
        glTextureParameteri(texture, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        glTextureParameteri(texture, GL_TEXTURE_MIN_LOD, 0);
        glTextureParameteri(texture, GL_TEXTURE_MAX_LOD, mipmapLevel);
        glTextureParameterf(texture, GL_TEXTURE_LOD_BIAS, 0.0f);
        glTextureStorage2D(texture, mipmapLevel, internalFormat, width, height);
    }

    /**
     * Upload image data to GPU. Alignment for {@link icyllis.modernui.platform.Bitmap} is 1 (byte-aligned).
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

    public void swizzle(int r, int g, int b, int a) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer mask = stack.mallocInt(4);
            mask.put(r).put(g).put(b).put(a).rewind();
            glTextureParameteriv(get(), GL_TEXTURE_SWIZZLE_RGBA, mask);
        }
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
    public int getWidth() {
        return glGetTextureParameteri(get(), GL_TEXTURE_WIDTH);
    }

    public int getHeight() {
        return glGetTextureParameteri(get(), GL_TEXTURE_HEIGHT);
    }
}
