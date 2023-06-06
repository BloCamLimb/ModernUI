/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.graphics.*;

import javax.annotation.Nullable;

/**
 * Provides common resources with cache.
 */
public final class ResourceProvider {

    public static final int MIN_SCRATCH_TEXTURE_SIZE = 16;

    private final Server mServer;
    private final ResourceCache mCache;

    // lookup key
    private final Texture.ScratchKey mTextureScratchKey = new Texture.ScratchKey();

    ResourceProvider(Server server, ResourceCache cache) {
        mServer = server;
        mCache = cache;
    }

    /**
     * Map <code>size</code> to a larger multiple of 2. Values <= 1024 will pop up to
     * the next power of 2. Those above 1024 will only go up half the floor power of 2.
     * <p>
     * Possible values: 16, 32, 64, 128, 256, 512, 1024, 1536, 2048, 3072, 4096, 6144, 8192
     */
    public static int makeApprox(int size) {
        size = Math.max(MIN_SCRATCH_TEXTURE_SIZE, size);

        if (MathUtil.isPow2(size)) {
            return size;
        }

        int ceilPow2 = MathUtil.ceilPow2(size);
        if (size <= 1024) {
            return ceilPow2;
        }

        int floorPow2 = ceilPow2 >> 1;
        int mid = floorPow2 + (floorPow2 >> 1);

        if (size <= mid) {
            return mid;
        }
        return ceilPow2;
    }

    /**
     * Finds a resource in the cache, based on the specified key. Prior to calling this, the caller
     * must be sure that if a resource of exists in the cache with the given unique key then it is
     * of type T. If the resource is no longer used, then {@link GpuResource#unref()} must be called.
     *
     * @param key the resource unique key
     */
    @Nullable
    @SharedPtr
    @SuppressWarnings("unchecked")
    public <T extends GpuResource> T findByUniqueKey(Object key) {
        assert mServer.getContext().isOwnerThread();
        return mServer.getContext().isDiscarded() ? null : (T) mCache.findAndRefUniqueResource(key);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Textures

    /**
     * Finds or creates a texture that matches the descriptor. The texture's format will always
     * match the request. The contents of the texture are undefined.
     * <p>
     * When {@link Surface#FLAG_BUDGETED} is set, the texture will count against the resource
     * cache budget. If {@link Surface#FLAG_APPROX_FIT} is also set, it's always budgeted.
     * <p>
     * When {@link Surface#FLAG_APPROX_FIT} is set, the method returns a potentially approx fit
     * texture that approximately matches the descriptor. Will be at least as large in width and
     * height as desc specifies. In this case, {@link Surface#FLAG_MIPMAPPED} and
     * {@link Surface#FLAG_BUDGETED} are ignored. Otherwise, the method returns an exact fit
     * texture.
     * <p>
     * When {@link Surface#FLAG_MIPMAPPED} is set, the texture will be allocated with mipmaps.
     * If {@link Surface#FLAG_APPROX_FIT} is also set, it always has no mipmaps.
     * <p>
     * When {@link Surface#FLAG_RENDERABLE} is set, the texture can be rendered to and
     * {@link RenderTarget#getFramebufferSet()} will return nonnull. The <code>sampleCount</code>
     * specifies the number of samples to use for rendering.
     * <p>
     * When {@link Surface#FLAG_PROTECTED} is set, the texture will be created as protected.
     *
     * @param width        the desired width of the texture to be created
     * @param height       the desired height of the texture to be created
     * @param format       the backend format for the texture
     * @param sampleCount  the number of samples to use for rendering if renderable is set,
     *                     otherwise this must be 1
     * @param surfaceFlags the combination of the above flags
     * @param label        the label for debugging purposes, can be empty to clear the label,
     *                     or null to leave the label unchanged
     * @see Surface#FLAG_BUDGETED
     * @see Surface#FLAG_APPROX_FIT
     * @see Surface#FLAG_MIPMAPPED
     * @see Surface#FLAG_RENDERABLE
     * @see Surface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public Texture createTexture(int width, int height,
                                 BackendFormat format,
                                 int sampleCount,
                                 int surfaceFlags,
                                 String label) {
        assert mServer.getContext().isOwnerThread();
        if (mServer.getContext().isDiscarded()) {
            return null;
        }

        // Currently, we don't recycle compressed textures as scratch. Additionally, all compressed
        // textures should be created through the createCompressedTexture function.
        assert !format.isCompressed();

        if (!mServer.getCaps().validateSurfaceParams(width, height, format,
                sampleCount, surfaceFlags)) {
            return null;
        }

        if ((surfaceFlags & Surface.FLAG_APPROX_FIT) != 0) {
            width = makeApprox(width);
            height = makeApprox(height);
            surfaceFlags &= Surface.FLAG_RENDERABLE | Surface.FLAG_PROTECTED;
            surfaceFlags |= Surface.FLAG_BUDGETED;
        }

        final Texture texture = findAndRefScratchTexture(width, height, format,
                sampleCount, surfaceFlags, label);
        if (texture != null) {
            if ((surfaceFlags & Surface.FLAG_BUDGETED) == 0) {
                texture.makeBudgeted(false);
            }
            return texture;
        }

        return mServer.createTexture(width, height, format,
                sampleCount, surfaceFlags, label);
    }

    /**
     * Same as {@link #createTexture(int, int, BackendFormat, int, int, String)} but with initial
     * data to upload. The color type must be valid for the format and also describe the texel data.
     * This will ensure any conversions that need to get applied to the data before upload are applied.
     *
     * @param width        the desired width of the texture to be created
     * @param height       the desired height of the texture to be created
     * @param format       the backend format for the texture
     * @param sampleCount  the number of samples to use for rendering if renderable is set,
     *                     otherwise this must be 1
     * @param surfaceFlags the combination of the above flags
     * @param dstColorType the format and type of the use of the texture, used to validate
     *                     srcColorType with texture's internal format
     * @param srcColorType the format and type of the texel data to upload
     * @param rowBytes     row size in bytes if data is greater than proper value
     * @param pixels       the pointer to the texel data for base level image
     * @param label        the label for debugging purposes, can be empty to clear the label,
     *                     or null to leave the label unchanged
     * @see Surface#FLAG_BUDGETED
     * @see Surface#FLAG_APPROX_FIT
     * @see Surface#FLAG_MIPMAPPED
     * @see Surface#FLAG_RENDERABLE
     * @see Surface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public Texture createTexture(int width, int height,
                                 BackendFormat format,
                                 int sampleCount,
                                 int surfaceFlags,
                                 int dstColorType,
                                 int srcColorType,
                                 int rowBytes,
                                 long pixels,
                                 String label) {
        assert mServer.getContext().isOwnerThread();
        if (mServer.getContext().isDiscarded()) {
            return null;
        }

        if (srcColorType == ImageInfo.CT_UNKNOWN ||
                dstColorType == ImageInfo.CT_UNKNOWN) {
            return null;
        }

        int minRowBytes = width * ImageInfo.bytesPerPixel(srcColorType);
        int actualRowBytes = rowBytes > 0 ? rowBytes : minRowBytes;
        if (actualRowBytes < minRowBytes) {
            return null;
        }
        int actualColorType = (int) mServer.getCaps().getSupportedWriteColorType(
                dstColorType,
                format,
                srcColorType);
        if (actualColorType != srcColorType) {
            return null;
        }

        final Texture texture = createTexture(width, height, format,
                sampleCount, surfaceFlags, label);
        if (texture == null) {
            return null;
        }
        if (pixels == 0) {
            return texture;
        }
        boolean result = mServer.writePixels(texture, 0, 0, width, height,
                dstColorType, actualColorType, actualRowBytes, pixels);
        assert result;

        return texture;
    }

    /**
     * Search the cache for a scratch texture matching the provided arguments. Failing that
     * it returns null. If non-null, the resulting texture is always budgeted.
     *
     * @param label the label for debugging purposes, can be empty to clear the label,
     *              or null to leave the label unchanged
     */
    @Nullable
    @SharedPtr
    public Texture findAndRefScratchTexture(Object key, String label) {
        assert mServer.getContext().isOwnerThread();
        assert !mServer.getContext().isDiscarded();
        assert key != null;

        GpuResource resource = mCache.findAndRefScratchResource(key);
        if (resource != null) {
            mServer.getStats().incNumScratchTexturesReused();
            if (label != null) {
                resource.setLabel(label);
            }
            return (Texture) resource;
        }
        return null;
    }

    /**
     * Search the cache for a scratch texture matching the provided arguments. Failing that
     * it returns null. If non-null, the resulting texture is always budgeted.
     *
     * @param label the label for debugging purposes, can be empty to clear the label,
     *              or null to leave the label unchanged
     * @see Surface#FLAG_MIPMAPPED
     * @see Surface#FLAG_RENDERABLE
     * @see Surface#FLAG_PROTECTED
     */
    @Nullable
    @SharedPtr
    public Texture findAndRefScratchTexture(int width, int height,
                                            BackendFormat format,
                                            int sampleCount,
                                            int surfaceFlags,
                                            String label) {
        assert mServer.getContext().isOwnerThread();
        assert !mServer.getContext().isDiscarded();
        assert !format.isCompressed();
        assert mServer.getCaps().validateSurfaceParams(width, height, format,
                sampleCount, surfaceFlags);

        return findAndRefScratchTexture(mTextureScratchKey.compute(
                format,
                width, height,
                sampleCount,
                surfaceFlags), label);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Wrapped Backend Surfaces

    /**
     * This makes the backend texture be renderable. If <code>sampleCount</code> is > 1 and
     * the underlying API uses separate MSAA render buffers then a MSAA render buffer is created
     * that resolves to the texture.
     * <p>
     * Ownership specifies rules for external GPU resources imported into Engine. If false,
     * Engine will assume the client will keep the resource alive and Engine will not free it.
     * If true, Engine will assume ownership of the resource and free it. If this method failed,
     * then ownership doesn't work.
     *
     * @param texture     the backend texture must be single sample
     * @param sampleCount the desired sample count
     * @return a managed, non-recycled render target, or null if failed
     */
    @Nullable
    @SharedPtr
    public Texture wrapRenderableBackendTexture(BackendTexture texture,
                                                int sampleCount,
                                                boolean ownership) {
        if (mServer.getContext().isDiscarded()) {
            return null;
        }
        return mServer.wrapRenderableBackendTexture(texture, sampleCount, ownership);
    }

    /**
     * Returns a buffer.
     *
     * @param size  minimum size of buffer to return.
     * @param usage hint to the graphics subsystem about what the buffer will be used for.
     * @return the buffer if successful, otherwise nullptr.
     * @see Engine.BufferUsageFlags
     */
    @Nullable
    @SharedPtr
    public GpuBuffer createBuffer(int size, int usage) {
        return null;
    }

    public void assignUniqueKeyToResource(Object key, GpuResource resource) {
        assert mServer.getContext().isOwnerThread();
        if (mServer.getContext().isDiscarded() || resource == null) {
            return;
        }
        resource.setUniqueKey(key);
    }
}
