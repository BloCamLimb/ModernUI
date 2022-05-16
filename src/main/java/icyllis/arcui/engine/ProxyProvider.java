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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;

/**
 * A factory for creating {@link SurfaceProxy}-derived objects. This class may be used from
 * the creating thread of {@link RecordingContext}. If it's deferred, call from recording thread.
 * Otherwise, it's direct, call from render thread.
 */
public final class ProxyProvider {

    private final RecordingContext mContext;
    private final DirectContext mDirect;

    // This holds the texture proxies that have unique keys. The resourceCache does not get a ref
    // on these proxies, but they must send a message to the resourceCache when they are deleted.
    private final Object2ObjectOpenHashMap<ResourceKey, TextureProxy> mUniquelyKeyedProxies;

    ProxyProvider(RecordingContext context) {
        mContext = context;
        if (context instanceof DirectContext) {
            mDirect = (DirectContext) context;
        } else {
            mDirect = null; // deferred
        }

        mUniquelyKeyedProxies = new Object2ObjectOpenHashMap<>();
    }

    /**
     * Assigns a unique key to a proxy. The proxy will be findable via this key using
     * {@link #findProxyByUniqueKey()}. It is an error if an existing proxy already has a key.
     */
    public boolean assignUniqueKeyToProxy(ResourceKey key, TextureProxy proxy) {
        assert key != null;
        if (mContext.isDropped() || proxy == null) {
            return false;
        }

        // Only the proxyProvider that created a proxy should be assigning unique keys to it.
        assert isDeferredProvider() == proxy.mDeferredProvider;

        // If there is already a Resource with this key then the caller has violated the
        // normal usage pattern of uniquely keyed resources (e.g., they have created one w/o
        // first seeing if it already existed in the cache).
        assert mDirect == null || mDirect.getResourceCache().findAndRefUniqueResource(key) == null;

        // multiple proxies can't get the same key
        assert !mUniquelyKeyedProxies.containsKey(key);
        //TODO set

        mUniquelyKeyedProxies.put(key, proxy);
        return true;
    }

    /**
     * Sets the unique key of the provided proxy to the unique key of the surface. The surface must
     * have a valid unique key.
     */
    public void adoptUniqueKeyFromSurface(SurfaceProxy proxy, Texture texture) {
        //TODO
    }

    /**
     * Create a {@link TextureProxy} without any data.
     *
     * @see TextureProxy
     */
    @Nullable
    @SmartPtr
    public TextureProxy createTextureProxy(BackendFormat format,
                                           int width, int height,
                                           boolean mipmapped,
                                           boolean backingFit,
                                           boolean budgeted,
                                           int surfaceFlags,
                                           boolean useAllocator) {
        assert mContext.isOnOwningThread();
        if (mContext.isDropped()) {
            return null;
        }

        if (format.isCompressed()) {
            // Deferred proxies for compressed textures are not supported.
            return null;
        }

        if (!mContext.getCaps().validateTextureParams(format, width, height)) {
            return null;
        }

        return new TextureProxy(format, width, height, mipmapped, backingFit, budgeted,
                surfaceFlags, useAllocator, isDeferredProvider());
    }

    @Nullable
    @SmartPtr
    public RenderTargetProxy createRenderTargetProxy(BackendFormat format,
                                                     int width, int height,
                                                     int sampleCount,
                                                     boolean mipmapped,
                                                     boolean backingFit,
                                                     boolean budgeted,
                                                     int surfaceFlags,
                                                     boolean useAllocator) {
        //TODO
        return null;
    }

    /**
     * Create a RenderTargetProxy that wraps a backend texture and is both texturable and renderable.
     * <p>
     * The texture must be single sampled and will be used as the color attachment 0 of the non-MSAA
     * render target. If <code>sampleCount</code> is > 1, the underlying API uses separate MSAA render
     * buffers then a MSAA render buffer is created that resolves to the texture.
     */
    @Nullable
    @SmartPtr
    public RenderTargetProxy wrapRenderableBackendTexture(BackendTexture texture,
                                                          int sampleCount,
                                                          boolean ownership,
                                                          boolean cacheable,
                                                          Runnable releaseCallback) {
        if (mContext.isDropped()) {
            return null;
        }

        // This is only supported on a direct Context.
        if (mDirect == null) {
            return null;
        }

        sampleCount = mContext.getCaps().getRenderTargetSampleCount(sampleCount, texture.getBackendFormat());
        assert sampleCount > 0;
        //TODO
        return null;
    }

    public boolean isDeferredProvider() {
        return mDirect == null;
    }
}
