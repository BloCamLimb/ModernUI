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

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL 2D texture objects at low-level.
 * The OpenGL texture object associated with this object may be changed
 * by destroying and reallocating.
 */
public class Texture2D implements AutoCloseable {

    private int mId = INVALID_ID;
    private int mTarget;

    /**
     * Creates a 2D texture object on application side from any thread.
     */
    public Texture2D() {
        this(GL_TEXTURE_2D);
    }

    public Texture2D(int target) {
        mTarget = target;
    }

    /**
     * Returns the OpenGL texture object name represented by this object.
     * It will be generated if it's unassigned. This operation does not
     * allocate GPU memory.
     *
     * @return texture object name
     */
    public int getId() {
        if (mId == INVALID_ID)
            mId = glGenTextures();
        return mId;
    }

    public void bind() {
        bindTexture(mTarget, getId());
    }

    /**
     * Resets texture type if this texture has been destroyed or not initialized yet.
     *
     * @param target texture target
     */
    public void setTarget(int target) {
        if (mId == INVALID_ID) {
            mTarget = target;
        }
    }

    public int getTarget() {
        return mTarget;
    }

    /**
     * Specifies this texture and allocates GPU memory dynamically. (Compatibility)
     *
     * @param internalFormat how image data stored on GPU side
     * @param mipmapLevel    max mipmap level, min is 0
     * @see #setStorage(int, int, int, int)
     */
    public void init(int internalFormat, int width, int height, int mipmapLevel) {
        bind();
        glTexParameteri(mTarget, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(mTarget, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        glTexParameteri(mTarget, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(mTarget, GL_TEXTURE_MAX_LOD, mipmapLevel);
        glTexParameterf(mTarget, GL_TEXTURE_LOD_BIAS, 0.0f);

        // null ptr represents not modifying the image data, but allocating enough memory
        for (int level = 0; level <= mipmapLevel; level++) {
            glTexImage2D(mTarget, level, internalFormat, width >> level,
                    height >> level, 0, GL_RED, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        }
    }

    /**
     * Specifies this texture with an immutable storage unless deleted. (Core-profile)
     * <p>
     * For automatic mipmap generation, you may need manually clear the mipmap data later,
     * otherwise the content may not be replaced and keep the previous undefined data,
     * especially for translucent texture.
     * <p>
     * When using mipmap, texture size must be power of two, and at least 2^mipmapLevel
     *
     * @param internalFormat how image data stored on GPU side
     * @param mipmapLevel    max mipmap level, min is 0
     */
    public void setStorage(int internalFormat, int width, int height, int mipmapLevel) {
        bind();
        glTexParameteri(mTarget, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(mTarget, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
        glTexParameteri(mTarget, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(mTarget, GL_TEXTURE_MAX_LOD, mipmapLevel);
        glTexParameterf(mTarget, GL_TEXTURE_LOD_BIAS, 0.0f);
        glTexStorage2D(mTarget, mipmapLevel, internalFormat, width, height);
    }

    /**
     * Upload image data to GPU. Alignment for {@link icyllis.modernui.platform.Bitmap} is 1 (byte-aligned).
     *
     * @param level      the level for the texture
     * @param x          x-coordinate of the texture
     * @param y          y-coordinate of the texture
     * @param width      width
     * @param height     height
     * @param rowLength  row length if data width is not equal to texture width, or 0
     * @param skipRows   skip rows of data
     * @param skipPixels skip pixels of data
     * @param alignment  pixel row alignment 1, 2, 4, 8
     * @param format     the format of the data to upload
     * @param type       the type of the data to upload
     * @param pixels     the pixels data pointer
     */
    public void upload(int level, int x, int y, int width, int height, int rowLength, int skipRows,
                       int skipPixels, int alignment, int format, int type, long pixels) {
        bind();
        glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, skipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, skipPixels);
        glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
        glTexSubImage2D(mTarget, level, x, y, width, height, format, type, pixels);
    }

    public void setWrapMode(int wrapS) {
        glTexParameteri(mTarget, GL_TEXTURE_WRAP_S, wrapS);
    }

    /**
     * Set wrap mode for s, t component. This texture must be bound first.
     * <p>
     * ERROR: if target is GL_TEXTURE_RECTANGLE and either of wrap mode GL_TEXTURE_WRAP_S or GL_TEXTURE_WRAP_T is set to
     * either GL_MIRROR_CLAMP_TO_EDGE, GL_MIRRORED_REPEAT or GL_REPEAT.
     */
    public void setWrapMode(int wrapS, int wrapT) {
        glTexParameteri(mTarget, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(mTarget, GL_TEXTURE_WRAP_T, wrapT);
    }

    public void setWrapMode(int wrapS, int wrapT, int wrapR) {
        glTexParameteri(mTarget, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(mTarget, GL_TEXTURE_WRAP_T, wrapT);
        glTexParameteri(mTarget, GL_TEXTURE_WRAP_R, wrapR);
    }

    // when mipmap = true, linear mipmap will always be used
    public void setFilter(boolean linear, boolean mipmap) {
        if (linear) {
            if (mipmap) {
                glTexParameteri(mTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            } else {
                glTexParameteri(mTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            }
            glTexParameteri(mTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            if (mipmap) {
                glTexParameteri(mTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
            } else {
                glTexParameteri(mTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            }
            glTexParameteri(mTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void setFilter(int minFilter, int magFilter) {
        glTexParameteri(mTarget, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(mTarget, GL_TEXTURE_MAG_FILTER, magFilter);
    }

    public void generateMipmap() {
        glGenerateMipmap(mTarget);
    }

    public void destroy() {
        if (mId != INVALID_ID) {
            deleteTexture(mTarget, mId);
            mId = INVALID_ID;
        }
    }

    @Override
    public void close() throws Exception {
        destroy();
    }
}
