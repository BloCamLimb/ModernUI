/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.IntBuffer;

import static icyllis.modernui.graphics.opengl.GLCore.*;

/**
 * Represents OpenGL texture objects at low-level.
 */
public class GLTextureCompat extends GLObjectCompat {

    private static final int HEIGHT_SHIFT = Long.SIZE / 3;
    private static final int DEPTH_SHIFT = HEIGHT_SHIFT << 1;
    private static final int SIZE_MASK = (1 << HEIGHT_SHIFT) - 1;

    private final int target;
    private long dimension;

    public GLTextureCompat(int target) {
        this.target = target;
    }

    /**
     * Returns the OpenGL texture object name currently associated with this
     * object, or create and initialize it if not available. It may change in
     * the future if it is explicitly deleted.
     *
     * @return OpenGL texture object
     */
    @Override
    public final int get() {
        if (ref == null) {
            final int texture = glGenTextures();
            if (target == GL_TEXTURE_2D) {
                final int p = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, texture);
                glBindTexture(GL_TEXTURE_2D, p);
            } else {
                final int p = glGetInteger(GL_TEXTURE_BINDING_2D_MULTISAMPLE);
                glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texture);
                glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, p);
            }
            ref = new Ref(this, texture);
        }
        return ref.mId;
    }

    /**
     * Returns the immutable target used to create this texture.
     * <p>
     * This method can be called from any thread.
     *
     * @return texture target
     */
    public final int getTarget() {
        return target;
    }

    /**
     * Returns the cached width of level 0 of this texture, when available.
     * <p>
     * When not allocated the return value is always 0. This value may change after recycling.
     * <p>
     * This method can be called from any thread.
     *
     * @return texture width
     */
    public final int getWidth() {
        return (int) (dimension & SIZE_MASK);
    }

    /**
     * Returns the cached height of level 0 of this texture, when available.
     * <p>
     * When not allocated the return value is always 0. This value may change after recycling.
     * <p>
     * This method can be called from any thread.
     *
     * @return texture height
     */
    public final int getHeight() {
        return (int) (dimension >> HEIGHT_SHIFT & SIZE_MASK);
    }

    /**
     * Returns the cached depth of level 0 of this texture, when available.
     * <p>
     * When not allocated the return value is always 0. This value may change after recycling.
     * <p>
     * This method can be called from any thread.
     *
     * @return texture depth
     */
    public final int getDepth() {
        return (int) (dimension >> DEPTH_SHIFT & SIZE_MASK);
    }

    /**
     * Sets the dimension if texture will be from known sources before allocating,
     * for asynchronous operations. This value will be verified on next allocating.
     * This method does nothing on the texture.
     *
     * @param width  texture width
     * @param height texture height
     * @param depth  texture depth
     */
    public final void setDimension(int width, int height, int depth) {
        long dim = 0;
        dim |= width;
        dim |= (long) (height & SIZE_MASK) << HEIGHT_SHIFT;
        dim |= (long) (depth & SIZE_MASK) << DEPTH_SHIFT;
        dimension = dim;
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
    public void allocate2D(int internalFormat, int width, int height, int maxLevel) {
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

        long dim = 0;
        dim |= (glGetTextureLevelParameteri(texture, 0, GL_TEXTURE_WIDTH) & SIZE_MASK);
        dim |= (long) (glGetTextureLevelParameteri(texture, 0, GL_TEXTURE_HEIGHT) & SIZE_MASK) << HEIGHT_SHIFT;
        dim |= 1L << DEPTH_SHIFT;
        if (dimension != 0 && dimension != dim) {
            ModernUI.LOGGER.warn(GLCore.MARKER, "Inconsistent 2D texture dimension, set 0x{} but allocated 0x{}",
                    Long.toHexString(dimension), Long.toHexString(dim));
        }
        dimension = dim;
    }

    /**
     * Specifies this texture and allocates video memory dynamically (compatibility).
     * The image data will be undefined after calling this method, unless an upload.
     * <p>
     * This method can be called multiple times, which represents re-specifying this texture.
     *
     * @param internalFormat sized internal format used for the image on GPU side
     * @param maxLevel       max mipmap level, min is 0
     * @see #allocate2D(int, int, int, int)
     */
    public void allocate2DCompat(int internalFormat, int width, int height, int maxLevel) {
        if (target != GL_TEXTURE_2D) {
            throw new IllegalStateException();
        }
        if (maxLevel < 0) {
            throw new IllegalArgumentException();
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException();
        }
        final int p = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, maxLevel);

        for (int level = 0; level <= maxLevel; level++) {
            nglTexImage2D(GL_TEXTURE_2D, level, internalFormat, width >> level,
                    height >> level, 0, GL_RED, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        }

        long dim = 0;
        dim |= (glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH) & SIZE_MASK);
        dim |= (long) (glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT) & SIZE_MASK) << HEIGHT_SHIFT;
        dim |= 1L << DEPTH_SHIFT;
        if (dimension != 0 && dimension != dim) {
            ModernUI.LOGGER.warn(GLCore.MARKER, "Inconsistent 2D(M) texture dimension, set 0x{} but allocated 0x{}",
                    Long.toHexString(dimension), Long.toHexString(dim));
        }
        dimension = dim;

        glBindTexture(GL_TEXTURE_2D, p);
    }

    /**
     * Specifies multisample 2D texture.
     *
     * @param internalFormat sized internal format
     * @param samples        number of samples, min is 1
     */
    public void allocate2DMS(int internalFormat, int width, int height, int samples) {
        if (target != GL_TEXTURE_2D_MULTISAMPLE) {
            throw new IllegalStateException();
        }
        if (samples < 1) {
            throw new IllegalArgumentException();
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException();
        }
        final int texture = get();
        glTextureStorage2DMultisample(texture, samples, internalFormat, width, height, true);

        long dim = 0;
        dim |= (glGetTextureLevelParameteri(texture, 0, GL_TEXTURE_WIDTH) & SIZE_MASK);
        dim |= (long) (glGetTextureLevelParameteri(texture, 0, GL_TEXTURE_HEIGHT) & SIZE_MASK) << HEIGHT_SHIFT;
        dim |= 1L << DEPTH_SHIFT;
        if (dimension != 0 && dimension != dim) {
            ModernUI.LOGGER.warn(GLCore.MARKER, "Inconsistent 2DMS texture dimension, set 0x{} but allocated 0x{}",
                    Long.toHexString(dimension), Long.toHexString(dim));
        }
        dimension = dim;
    }

    /**
     * Upload image data to GPU.
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
     * Upload image data to GPU.
     *
     * @param level     the level for the image
     * @param rowLength row length if data width is not equal to texture width, or 0
     * @param alignment pixel row alignment 1, 2, 4, 8
     * @param format    the format of the data to upload, one of GL_RED, GL_RG, GL_RGB,
     *                  GL_BGR, GL_RGBA, GL_BGRA, GL_DEPTH_COMPONENT, and GL_STENCIL_INDEX.
     * @param type      the type of the data to upload, for example, unsigned byte
     * @param pixels    the native pointer of pixels data
     */
    public void uploadCompat(int level, int x, int y, int width, int height, int rowLength, int skipRows,
                             int skipPixels, int alignment, int format, int type, long pixels) {
        glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, skipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, skipPixels);
        glPixelStorei(GL_UNPACK_ALIGNMENT, alignment);
        final int p = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(target, get());
        nglTexSubImage2D(target, level, x, y, width, height, format, type, pixels);
        glBindTexture(target, p);
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
     * Set wrap mode.
     */
    public void setWrapS(int wrapS) {
        glTextureParameteri(get(), GL_TEXTURE_WRAP_S, wrapS);
    }

    /**
     * Set wrap mode.
     */
    public void setWrapT(int wrapT) {
        glTextureParameteri(get(), GL_TEXTURE_WRAP_T, wrapT);
    }

    /**
     * Set wrap mode.
     */
    public void setWrapR(int wrapR) {
        glTextureParameteri(get(), GL_TEXTURE_WRAP_R, wrapR);
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
     * Set wrap mode.
     */
    public void setWrapMode(int wrapS, int wrapT, int wrapR) {
        final int texture = get();
        glTextureParameteri(texture, GL_TEXTURE_WRAP_S, wrapS);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_T, wrapT);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_R, wrapR);
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

    public void setFilterCompat(int minFilter, int magFilter) {
        final int p = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(target, get());
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
        glBindTexture(target, p);
    }

    /**
     * Swizzle RGBA read by shader programs.
     * <p>
     * For example: <code>swizzleRGBA(GL_ONE, GL_ONE, GL_ONE, GL_RED)</code>.
     * Then red channel will be read as alpha channel by shader, RGB is always 1
     *
     * @param r color masks
     */
    public void setSwizzle(int r, int g, int b, int a) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(4);
            buffer.put(r).put(g).put(b).put(a).flip();
            glTextureParameteriv(get(), GL_TEXTURE_SWIZZLE_RGBA, buffer);
        }
    }

    /**
     * Swizzle RGBA read by shader programs.
     * <p>
     * For example: <code>swizzleRGBA(GL_ONE, GL_ONE, GL_ONE, GL_RED)</code>.
     * Then red channel will be read as alpha channel by shader, RGB is always 1
     *
     * @param r color masks
     */
    public void setSwizzleCompat(int r, int g, int b, int a) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(4);
            buffer.put(r).put(g).put(b).put(a).flip();
            final int p = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(target, get());
            glTexParameteriv(target, GL_TEXTURE_SWIZZLE_RGBA, buffer);
            glBindTexture(target, p);
        }
    }

    /**
     * Generates mipmaps.
     */
    public void generateMipmap() {
        glGenerateTextureMipmap(get());
    }

    /**
     * Generates mipmaps.
     */
    public void generateMipmapCompat() {
        final int p = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(target, get());
        glGenerateMipmap(target);
        glBindTexture(target, p);
    }

    @Override
    public void close() {
        super.close();
        dimension = 0;
    }

    /*
     * Regenerate the 2D texture to the new size.
     *
     * @param width  new width of the texture
     * @param height new height of the texture
     * @param copy   true to copy the level 0 image data from the old one to the new one
     */
    /*public void resize(int width, int height, boolean copy) {
        if (!isCreated()) {
            return;
        }
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        if ((oldWidth | oldHeight) == 0) {
            return;
        }
        if (width == oldWidth && height == oldHeight) {
            return;
        }
        int texture = get();
        int internalFormat = getInternalFormat();
        int maxLevel = glGetTextureParameteri(texture, GL_TEXTURE_MAX_LEVEL);

        Cleaner.Cleanable cleanup = recreate();
        allocate2DCompat(internalFormat, width, height, maxLevel);
        if (copy) {
            glCopyImageSubData(texture, GL_TEXTURE_2D, 0, 0, 0, 0,
                    get(), GL_TEXTURE_2D, 0, 0, 0, 0,
                    Math.min(width, oldWidth), Math.min(height, oldHeight), 1);
            if (maxLevel > 0) {
                generateMipmap();
            }
        }
        if (cleanup != null) {
            cleanup.clean();
        }
    }*/

    public int getInternalFormat() {
        return glGetTextureLevelParameteri(get(), 0, GL_TEXTURE_INTERNAL_FORMAT);
    }

    /*
     * Re-create the OpenGL texture and returns the cleanup action for the previous one.
     * You should call the cleanup action if you will not touch the previous texture any more.
     * Otherwise, it will be cleaned when this Texture object become phantom-reachable.
     *
     * @return cleanup action, null if this texture was recycled or never initialized
     */
    /*@Nullable
    public final Cleaner.Cleanable recreate() {
        final var r = ref;
        ref = new Ref(this);
        dimension = 0;
        return r != null ? r.mCleanup : null;
    }*/

    private static final class Ref extends GLObjectCompat.Ref {

        private Ref(@Nonnull GLTextureCompat owner, int id) {
            super(owner, id);
        }

        @Override
        public void run() {
            Core.postOnRenderThread(() -> glDeleteTextures(mId));
        }
    }
}
