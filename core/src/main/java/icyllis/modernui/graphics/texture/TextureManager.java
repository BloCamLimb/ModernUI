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
import icyllis.modernui.graphics.GLWrapper;
import icyllis.modernui.platform.NativeImage;
import icyllis.modernui.platform.RenderCore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * This class maintains OpenGL 2D textures decoded from local client resources.
 */
public class TextureManager {

    private static final TextureManager INSTANCE = new TextureManager();

    public static final int CACHE_MASK = 0x1;
    public static final int MIPMAP_MASK = 0x2;

    private Map<String, Map<String, GLTexture>> mTextures = new HashMap<>();
    private final Object mLock = new Object();

    private TextureManager() {
    }

    /**
     * @return the global texture manager instance
     */
    public static TextureManager getInstance() {
        return INSTANCE;
    }

    // internal use
    public void reload() {
        synchronized (mLock) {
            // implicitly release textures, see cleaner
            mTextures.clear();
            mTextures = new HashMap<>();
        }
    }

    /**
     * Get or create an OpenGL 2D texture from the given resource.
     *
     * @param namespace the application namespace
     * @param subPath   the sub path to the resource, parent is 'textures'
     * @return texture
     */
    @Nonnull
    public GLTexture getOrCreate(@Nonnull String namespace, @Nonnull String subPath) {
        return getOrCreate(namespace, "textures/" + subPath, CACHE_MASK | MIPMAP_MASK);
    }

    /**
     * Get or create an OpenGL 2D texture from the given resource. {@link #CACHE_MASK} will use
     * cache or create into the cache. {@link #MIPMAP_MASK} will generate mipmaps for
     * the resource texture.
     *
     * @param namespace the application namespace
     * @param path      the path to the resource
     * @param flags     behavior flags
     * @return texture
     */
    @Nonnull
    public GLTexture getOrCreate(@Nonnull String namespace, @Nonnull String path, int flags) {
        final GLTexture texture;
        synchronized (mLock) {
            Map<String, GLTexture> cache = null;
            if ((flags & CACHE_MASK) != 0) {
                cache = mTextures.computeIfAbsent(namespace, n -> new HashMap<>());
                GLTexture entry = cache.get(path);
                if (entry != null) {
                    return entry;
                }
            }
            texture = new GLTexture(GLWrapper.GL_TEXTURE_2D);
            if (cache != null) {
                cache.put(path, texture);
            }
        }
        try (InputStream stream = ModernUI.get().getResourceAsStream(namespace, path)) {
            NativeImage image = NativeImage.decode(null, stream);
            create(texture, image, (flags & MIPMAP_MASK) != 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    /**
     * Create the texture from stream, cache will not be used, stream will be closed
     * automatically.
     *
     * @param stream resource stream
     * @param mipmap should generate mipmaps
     * @return texture
     */
    @Nonnull
    public GLTexture create(@Nonnull InputStream stream, boolean mipmap) {
        GLTexture texture = new GLTexture(GLWrapper.GL_TEXTURE_2D);
        try (stream) {
            NativeImage image = NativeImage.decode(null, stream);
            create(texture, image, mipmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    /**
     * Create the texture from channel, cache will not be used, channel will be closed
     * automatically.
     *
     * @param channel resource channel
     * @param mipmap  should generate mipmaps
     * @return texture
     */
    @Nonnull
    public GLTexture create(@Nonnull ReadableByteChannel channel, boolean mipmap) {
        GLTexture texture = new GLTexture(GLWrapper.GL_TEXTURE_2D);
        try (channel) {
            NativeImage image = NativeImage.decode(null, channel);
            create(texture, image, mipmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    private void create(@Nonnull GLTexture texture, @Nonnull NativeImage image, boolean mipmap) {
        int width = image.getWidth();
        int height = image.getHeight();
        texture.setDimension(width, height, 1);
        if (RenderCore.isOnRenderThread()) {
            texture.allocate2D(image.getInternalGlFormat(), width, height, mipmap ? 4 : 0);
            texture.upload(0, 0, 0, width, height, 0,
                    0, 0, 1, image.getGlFormat(), GLWrapper.GL_UNSIGNED_BYTE, image.getPixels());
            texture.setFilter(true, true);
            if (mipmap) {
                texture.generateMipmap();
            }
            image.close();
        } else {
            if (mipmap) {
                RenderCore.recordRenderCall(() -> {
                    int w = image.getWidth();
                    int h = image.getHeight();
                    texture.allocate2D(image.getInternalGlFormat(), w, h, 4);
                    texture.upload(0, 0, 0, w, h, 0,
                            0, 0, 1, image.getGlFormat(), GLWrapper.GL_UNSIGNED_BYTE, image.getPixels());
                    texture.setFilter(true, true);
                    texture.generateMipmap();
                    image.close();
                });
            } else {
                RenderCore.recordRenderCall(() -> {
                    int w = image.getWidth();
                    int h = image.getHeight();
                    texture.allocate2D(image.getInternalGlFormat(), w, h, 0);
                    texture.upload(0, 0, 0, w, h, 0,
                            0, 0, 1, image.getGlFormat(), GLWrapper.GL_UNSIGNED_BYTE, image.getPixels());
                    texture.setFilter(true, true);
                    image.close();
                });
            }
        }
    }
}
