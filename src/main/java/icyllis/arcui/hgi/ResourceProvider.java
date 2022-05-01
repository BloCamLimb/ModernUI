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

package icyllis.arcui.hgi;

import icyllis.arcui.core.Image;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A factory for arbitrary resource types.
 */
@NotThreadSafe
public final class ResourceProvider {

    public static final int MIN_SCRATCH_TEXTURE_SIZE = 16;

    private Server mServer;
    private ResourceCache mCache;

    public ResourceProvider(Server server, ResourceCache cache) {
        mServer = server;
        mCache = cache;
    }

    /**
     * Finds a resource in the cache, based on the specified key. Prior to calling this, the caller
     * must be sure that if a resource of exists in the cache with the given unique key then it is
     * of type T. If the resource is no longer used, then {@link Resource#unref()} must be called.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Resource> T findByUniqueKey(Object key) {
        return mCache == null ? null : (T) mCache.findAndRefUniqueResource(key);
    }

    /**
     * Finds a texture that approximately matches the descriptor. Will be at least as large in width
     * and height as desc specifies. If renderable is kYes then the GrTexture will also be a
     * GrRenderTarget. The texture's format and sample count will always match the request.
     * The contents of the texture are undefined.
     */
    public Texture createApproxTexture(int width, int height,
                                       BackendFormat format,
                                       int textureType,
                                       boolean renderable,
                                       int renderTargetSampleCnt,
                                       boolean isProtected) {
        if (mServer == null) {
            return null;
        }

        // Currently, we don't recycle compressed textures as scratch. Additionally, all compressed
        // textures should be created through the createCompressedTexture function.
        assert format.getCompressionType() == Image.COMPRESSION_TYPE_NONE;

        return null;
    }

    /**
     * Drops this provider, called by {@link DirectContext}.
     */
    public void discard() {
        mServer = null;
        mCache = null;
    }

    public static int makeApprox(int size) {
        size = Math.max(MIN_SCRATCH_TEXTURE_SIZE, size);

        // isPowerOfTwo
        if ((size & (size - 1)) == 0) {
            return size;
        }

        // ceilingPowerOfTwo
        int ceilPow2 = 1 << -Integer.numberOfLeadingZeros(size - 1);
        if (size <= 1 << 10) {
            return ceilPow2;
        }

        int floorPow2 = ceilPow2 >> 1;
        int mid = floorPow2 + (floorPow2 >> 1);

        if (size <= mid) {
            return mid;
        }
        return ceilPow2;
    }
}
