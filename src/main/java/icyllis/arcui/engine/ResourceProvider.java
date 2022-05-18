/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import icyllis.arcui.core.ImageInfo;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A factory for arbitrary resource types, render thread only.
 */
@NotThreadSafe
public final class ResourceProvider {

    public static final int MIN_SCRATCH_TEXTURE_SIZE = 16;
    private static final int MAGIC_TOLERANCE = 1024;

    private final Server mServer;
    private final ResourceCache mCache;

    ResourceProvider(Server server, ResourceCache cache) {
        mServer = server;
        mCache = cache;
    }

    /**
     * Map 'size' to a larger multiple of 2. Values <= 'MAGIC_TOLERANCE' will pop up to
     * the next power of 2. Those above 'MAGIC_TOLERANCE' will only go up half the floor power of 2.
     */
    public static int makeApprox(int size) {
        size = Math.max(MIN_SCRATCH_TEXTURE_SIZE, size);

        // isPowerOfTwo
        if ((size & (size - 1)) == 0) {
            return size;
        }

        // ceilingPowerOfTwo
        int ceilPow2 = 1 << -Integer.numberOfLeadingZeros(size - 1);
        if (size <= MAGIC_TOLERANCE) {
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
     * of type T. If the resource is no longer used, then {@link Resource#unref()} must be called.
     */
    @Nullable
    @SmartPtr
    @SuppressWarnings("unchecked")
    public <T extends Resource> T findByUniqueKey(ResourceKey key) {
        return mServer.getContext().isDropped() ? null : (T) mCache.findAndRefUniqueResource(key);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Textures

    /**
     * Finds or creates a texture that approximately matches the descriptor. Will be at least as large
     * in width and height as desc specifies. The texture's format will always match the request. The
     * contents of the texture are undefined. It always has NO mipmaps and is always budgeted.
     *
     * @param width       the desired width of the texture to be created
     * @param height      the desired height of the texture to be created
     * @param format      the backend format for the texture
     * @param isProtected should the texture be created as protected
     */
    @Nullable
    @SmartPtr
    public Texture createApproxTexture(int width, int height,
                                       BackendFormat format,
                                       boolean isProtected) {
        assert mServer.getContext().isOnOwnerThread();
        if (mServer.getContext().isDropped()) {
            return null;
        }

        // Currently, we don't recycle compressed textures as scratch. Additionally, all compressed
        // textures should be created through the createCompressedTexture function.
        assert !format.isCompressed();

        if (!mServer.getCaps().validateTextureParams(width, height, format)) {
            return null;
        }

        width = makeApprox(width);
        height = makeApprox(height);

        final Texture texture = findAndRefScratchTexture(width, height, format, Types.MIPMAPPED_NO, isProtected);
        if (texture != null) {
            return texture;
        }

        return mServer.createTexture(width, height,
                format,
                Types.MIPMAPPED_NO,
                true,
                isProtected);
    }

    /**
     * Finds or creates a potentially loose fit texture with the provided data. The color type must be valid
     * for the format and also describe the texel data. This will ensure any conversions that
     * need to get applied to the data before upload are applied. It always has NO mipmaps and is
     * always budgeted. Row size in bytes is rowLength times bpp of colorType if rowLength is not
     * zero, otherwise baseWidth times bpp.
     *
     * @param width        the desired width of the texture to be created
     * @param height       the desired height of the texture to be created
     * @param format       the backend format for the texture
     * @param isProtected  should the texture be created as protected
     * @param rowLength    row length in pixels if data width is greater than texture width,
     *                     or 0
     * @param alignment    pixel row alignment in bytes, must be one of 1 (tightly packed),
     *                     2, 4 or 8
     * @param dstColorType the format and type of the use of the texture, used to validate
     *                     srcColorType with texture's internal format
     * @param srcColorType the format and type of the texel data to upload
     * @param pixels       the native pointer to the off-heap texel data for base level image
     */
    @Nullable
    @SmartPtr
    public Texture createApproxTexture(int width, int height,
                                       BackendFormat format,
                                       boolean isProtected,
                                       int rowLength,
                                       int alignment,
                                       int dstColorType,
                                       int srcColorType,
                                       long pixels) {
        assert mServer.getContext().isOnOwnerThread();
        if (mServer.getContext().isDropped()) {
            return null;
        }

        if (alignment != 1 && alignment != 2 && alignment != 4 && alignment != 8) {
            return null;
        }
        if (srcColorType == ImageInfo.COLOR_UNKNOWN) {
            return null;
        }
        if (pixels == MemoryUtil.NULL) {
            return null;
        }

        final Texture texture = createApproxTexture(width, height, format, isProtected);
        if (texture == null) {
            return null;
        }

        return writePixels(texture, width, height, rowLength, alignment, dstColorType, srcColorType, pixels);
    }

    /**
     * Finds or creates a texture that exactly matches the descriptor. The texture's format will
     * always match the request. The contents of the texture are undefined.
     *
     * @param width       the width of the texture to be created
     * @param height      the height of the texture to be created
     * @param format      the backend format for the texture
     * @param mipmapped   should the texture be allocated with mipmaps
     * @param budgeted    should the texture count against the resource cache budget
     * @param isProtected should the texture be created as protected
     */
    @Nullable
    @SmartPtr
    public Texture createTexture(int width, int height,
                                 BackendFormat format,
                                 boolean mipmapped,
                                 boolean budgeted,
                                 boolean isProtected) {
        assert mServer.getContext().isOnOwnerThread();
        if (mServer.getContext().isDropped()) {
            return null;
        }

        // Currently, we don't recycle compressed textures as scratch. Additionally, all compressed
        // textures should be created through the createCompressedTexture function.
        assert !format.isCompressed();

        if (!mServer.getCaps().validateTextureParams(width, height, format)) {
            return null;
        }

        final Texture texture = findAndRefScratchTexture(width, height, format, mipmapped, isProtected);
        if (texture != null) {
            if (!budgeted) {
                texture.makeBudgeted(false);
            }
            return texture;
        }

        return mServer.createTexture(width, height,
                format,
                mipmapped,
                budgeted,
                isProtected);
    }

    /**
     * Finds or creates an exact fit texture with initial data to upload. The color type must be valid
     * for the format and also describe the texel data. This will ensure any conversions that
     * need to get applied to the data before upload are applied. Row size in bytes is rowLength
     * times bpp of colorType if rowLength is not zero, otherwise baseWidth times bpp.
     *
     * @param width        the width of the texture to be created
     * @param height       the height of the texture to be created
     * @param format       the backend format for the texture
     * @param mipmapped    should the texture be allocated with mipmaps
     * @param budgeted     should the texture count against the resource cache budget
     * @param isProtected  should the texture be created as protected
     * @param rowLength    row length in pixels if data width is greater than texture width,
     *                     or 0
     * @param alignment    pixel row alignment in bytes, must be one of 1 (tightly packed),
     *                     2, 4 or 8
     * @param dstColorType the format and type of the use of the texture, used to validate
     *                     srcColorType with texture's internal format
     * @param srcColorType the format and type of the texel data to upload
     * @param pixels       the native pointer to the off-heap texel data for base level image
     */
    @Nullable
    @SmartPtr
    public Texture createTexture(int width, int height,
                                 BackendFormat format,
                                 boolean mipmapped,
                                 boolean budgeted,
                                 boolean isProtected,
                                 int rowLength,
                                 int alignment,
                                 int dstColorType,
                                 int srcColorType,
                                 long pixels) {
        assert mServer.getContext().isOnOwnerThread();
        if (mServer.getContext().isDropped()) {
            return null;
        }

        if (alignment != 1 && alignment != 2 && alignment != 4 && alignment != 8) {
            return null;
        }
        if (srcColorType == ImageInfo.COLOR_UNKNOWN) {
            return null;
        }
        if (pixels == MemoryUtil.NULL) {
            return null;
        }

        final Texture texture = createTexture(width, height, format, mipmapped, budgeted, isProtected);
        if (texture == null) {
            return null;
        }

        return writePixels(texture, width, height, rowLength, alignment, dstColorType, srcColorType, pixels);
    }

    /**
     * ResourceProvider may be asked to "create" a new texture with initial pixel data to populate
     * it. In implementation, it may pull an existing texture from ResourceCache and then write the
     * pixel data to the texture. It takes a width/height for the base level because we may be
     * using an approximate-sized scratch texture. On success the texture is returned and null
     * on failure.
     */
    @Nullable
    @SmartPtr
    private Texture writePixels(Texture texture,
                                int baseWidth,
                                int baseHeight,
                                int rowLength,
                                int alignment,
                                int dstColorType,
                                int srcColorType,
                                long pixels) {
        assert !mServer.getContext().isDropped();
        assert texture != null;
        assert srcColorType != ImageInfo.COLOR_UNKNOWN;
        assert pixels != MemoryUtil.NULL;

        // we request client to calculate bpp itself, because we don't support type conversion
        int actualRowLength = rowLength != 0 ? rowLength : baseWidth;
        if (actualRowLength < baseWidth) {
            texture.unref();
            return null;
        }
        int actualColorType = (int) mServer.getCaps().getSupportedWriteColorType(
                dstColorType,
                texture.getBackendFormat(),
                srcColorType);
        if (actualColorType != srcColorType) {
            texture.unref();
            return null;
        }
        boolean result = mServer.writePixels(texture, 0, 0, baseWidth, baseHeight,
                actualRowLength, alignment, dstColorType, actualColorType, pixels);
        assert result;

        return texture;
    }

    /**
     * Search the cache for a scratch texture matching the provided arguments. Failing that
     * it returns null. If non-null, the resulting texture is always budgeted.
     */
    @Nullable
    @SmartPtr
    public Texture findAndRefScratchTexture(ResourceKey key) {
        assert mServer.getContext().isOnOwnerThread();
        assert !mServer.getContext().isDropped();
        assert key != null;

        Resource resource = mCache.findAndRefScratchResource(key);
        if (resource != null) {
            mServer.getStats().incNumScratchTexturesReused();
            return (Texture) resource;
        }
        return null;
    }

    /**
     * Search the cache for a scratch texture matching the provided arguments. Failing that
     * it returns null. If non-null, the resulting texture is always budgeted.
     */
    @Nullable
    @SmartPtr
    public Texture findAndRefScratchTexture(int width, int height,
                                            BackendFormat format,
                                            boolean mipmapped,
                                            boolean isProtected) {
        assert mServer.getContext().isOnOwnerThread();
        assert !mServer.getContext().isDropped();
        assert !format.isCompressed();
        assert mServer.getCaps().validateTextureParams(width, height, format);

        ResourceKey key = Surface.computeScratchKeyTLS(format, width, height, 1, mipmapped, isProtected);
        return findAndRefScratchTexture(key);
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
     * @param texture the backend texture must be single sample
     * @return a managed, non-cacheable render target, or null if failed
     */
    @Nullable
    @SmartPtr
    public RenderTarget wrapRenderableBackendTexture(BackendTexture texture,
                                                     int sampleCount,
                                                     boolean ownership) {
        if (mServer.getContext().isDropped()) {
            return null;
        }
        return mServer.wrapRenderableBackendTexture(texture, sampleCount, ownership);
    }

    public void assignUniqueKeyToResource(ResourceKey key, Resource resource) {
        assert mServer.getContext().isOnOwnerThread();
        if (mServer.getContext().isDropped() || resource == null) {
            return;
        }
        resource.setUniqueKey(key);
    }
}
