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
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * This class maintains OpenGL 2D textures decoded from local client resources.
 *
 * @deprecated to be replaced by {@link icyllis.modernui.graphics.engine.ProxyProvider}
 */
@Deprecated
@ApiStatus.Internal
public class GLTextureManager {

    private static final GLTextureManager INSTANCE = new GLTextureManager();

    public static final int CACHE_MASK = 0x1;
    public static final int MIPMAP_MASK = 0x2;

    private final Object mLock = new Object();
    private Map<String, Map<String, GLTextureCompat>> mTextures = new HashMap<>();

    private GLTextureManager() {
    }

    /**
     * @return the global texture manager instance
     */
    public static GLTextureManager getInstance() {
        return INSTANCE;
    }

    // internal use
    public void reload() {
        synchronized (mLock) {
            // see Cleaner
            mTextures.clear();
            mTextures = new HashMap<>();
        }
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
    public GLTextureCompat getOrCreate(@Nonnull String namespace, @Nonnull String path, int flags) {
        final GLTextureCompat texture;
        if ((flags & CACHE_MASK) != 0) {
            synchronized (mLock) {
                Map<String, GLTextureCompat> cache = mTextures.computeIfAbsent(namespace, n -> new HashMap<>());
                GLTextureCompat entry = cache.get(path);
                if (entry != null) {
                    return entry;
                } else {
                    texture = new GLTextureCompat(GLCore.GL_TEXTURE_2D);
                    cache.put(path, texture);
                }
            }
        } else {
            texture = new GLTextureCompat(GLCore.GL_TEXTURE_2D);
        }
        try (InputStream stream = ModernUI.getInstance().getResourceStream(namespace, path)) {
            var opts = new BitmapFactory.Options();
            opts.inPreferredFormat = Bitmap.Format.RGBA_8888;
            Bitmap bitmap = BitmapFactory.decodeStream(stream, opts);
            assert bitmap != null;
            createFromBitmap(texture, bitmap, (flags & MIPMAP_MASK) != 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    /**
     * Create the texture from stream, cache will not be used, stream will be closed
     * automatically.
     *
     * @param stream    resource stream
     * @param mipmapped should generate mipmaps
     * @return texture
     */
    @Nonnull
    public GLTextureCompat create(@Nonnull InputStream stream, boolean mipmapped) {
        GLTextureCompat texture = new GLTextureCompat(GLCore.GL_TEXTURE_2D);
        try (stream) {
            var opts = new BitmapFactory.Options();
            opts.inPreferredFormat = Bitmap.Format.RGBA_8888;
            Bitmap bitmap = BitmapFactory.decodeStream(stream, opts);
            assert bitmap != null;
            createFromBitmap(texture, bitmap, mipmapped);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    /**
     * Create the texture from channel, cache will not be used, channel will be closed
     * automatically.
     *
     * @param channel   resource channel
     * @param mipmapped should generate mipmaps
     * @return texture
     */
    @Nonnull
    public GLTextureCompat create(@Nonnull ReadableByteChannel channel, boolean mipmapped) {
        GLTextureCompat texture = new GLTextureCompat(GLCore.GL_TEXTURE_2D);
        try (channel) {
            var opts = new BitmapFactory.Options();
            opts.inPreferredFormat = Bitmap.Format.RGBA_8888;
            Bitmap bitmap = BitmapFactory.decodeChannel(channel, opts);
            assert bitmap != null;
            createFromBitmap(texture, bitmap, mipmapped);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texture;
    }

    private void createFromBitmap(@Nonnull GLTextureCompat texture, @Nonnull Bitmap bitmap, boolean mipmapped) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        texture.setDimension(width, height, 1);
        int rowPixels = bitmap.getRowStride() / bitmap.getChannelCount();
        if (Core.isOnRenderThread()) {
            texture.allocate2D(bitmap.getInternalGlFormat(), width, height, mipmapped ? 4 : 0);
            texture.upload(0, 0, 0, width, height, rowPixels,
                    0, 0, 1, bitmap.getExternalGlFormat(), GLCore.GL_UNSIGNED_BYTE, bitmap.getPixels());
            texture.setFilter(true, true);
            if (mipmapped) {
                texture.generateMipmap();
            }
            bitmap.close();
        } else {
            if (mipmapped) {
                Core.postOnRenderThread(() -> {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    texture.allocate2D(bitmap.getInternalGlFormat(), w, h, 4);
                    texture.upload(0, 0, 0, w, h, rowPixels,
                            0, 0, 1, bitmap.getExternalGlFormat(), GLCore.GL_UNSIGNED_BYTE, bitmap.getPixels());
                    texture.setFilter(true, true);
                    texture.generateMipmap();
                    bitmap.close();
                });
            } else {
                Core.postOnRenderThread(() -> {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    texture.allocate2D(bitmap.getInternalGlFormat(), w, h, 0);
                    texture.upload(0, 0, 0, w, h, rowPixels,
                            0, 0, 1, bitmap.getExternalGlFormat(), GLCore.GL_UNSIGNED_BYTE, bitmap.getPixels());
                    texture.setFilter(true, true);
                    bitmap.close();
                });
            }
        }
    }
}