/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A factory for creating {@link SurfaceProxy}-derived objects. This class may be used on
 * the creating thread of {@link RecordingContext}.
 */
public final class SurfaceProvider {

    private final RecordingContext mContext;
    private final DirectContext mDirect;

    // This holds the texture proxies that have unique keys. The resourceCache does not get a ref
    // on these proxies, but they must send a message to the resourceCache when they are deleted.
    private final Object2ObjectOpenHashMap<IUniqueKey, ImageProxy> mUniquelyKeyedProxies;

    SurfaceProvider(RecordingContext context) {
        mContext = context;
        if (context instanceof DirectContext) {
            mDirect = (DirectContext) context;
        } else {
            mDirect = null; // deferred
        }

        mUniquelyKeyedProxies = new Object2ObjectOpenHashMap<>();
    }

    /**
     * Assigns a unique key to a texture. The texture will be findable via this key using
     * {@link #findProxyByUniqueKey()}. It is an error if an existing texture already has a key.
     */
    public boolean assignUniqueKeyToProxy(IUniqueKey key, ImageProxy proxy) {
        assert key != null;
        if (mContext.isDiscarded() || proxy == null) {
            return false;
        }

        // Only the provider that created a texture should be assigning unique keys to it.
        assert isDeferredProvider() == ((proxy.mSurfaceFlags & ISurface.FLAG_DEFERRED_PROVIDER) != 0);

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
     * Sets the unique key of the provided texture to the unique key of the GPU texture.
     * The GPU texture must have a valid unique key.
     */
    public void adoptUniqueKeyFromSurface(ImageProxy proxy, GpuSurface texture) {
        //TODO
    }

    public void processInvalidUniqueKey(Object key, ImageProxy proxy, boolean invalidateResource) {
    }

    /**
     * Create a lazy {@link ImageProxy} without any data.
     *
     * @see ImageProxy
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_PROTECTED
     * @see ISurface#FLAG_SKIP_ALLOCATOR
     */
    @Nullable
    @SharedPtr
    public ImageProxy createTexture(BackendFormat format,
                                    int width, int height,
                                    int surfaceFlags) {
        assert mContext.isOwnerThread();
        if (mContext.isDiscarded()) {
            return null;
        }

        if (format.isCompressed()) {
            // Deferred proxies for compressed textures are not supported.
            return null;
        }

        if (!mContext.getCaps().validateSurfaceParams(width, height, format, 1, surfaceFlags)) {
            return null;
        }

        if (isDeferredProvider()) {
            surfaceFlags |= ISurface.FLAG_DEFERRED_PROVIDER;
        } else {
            assert (surfaceFlags & ISurface.FLAG_DEFERRED_PROVIDER) == 0;
        }

        return new ImageProxy(format, width, height, surfaceFlags);
    }

    /**
     * Creates a lazy {@link ImageProxy} for the pixel map.
     *
     * @param pixelMap     pixel map
     * @param pixelRef     raw ptr to pixel ref, must be immutable
     * @param dstColorType a color type for surface usage, see {@link ImageInfo}
     * @param surfaceFlags flags described as follows
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     */
    @Nullable
    @SharedPtr
    public ImageProxy createTextureFromPixels(@Nonnull PixelMap pixelMap,
                                              @Nonnull @RawPtr PixelRef pixelRef,
                                              int dstColorType,
                                              int surfaceFlags) {
        mContext.checkOwnerThread();
        assert ((surfaceFlags & ISurface.FLAG_APPROX_FIT) == 0) ||
                ((surfaceFlags & ISurface.FLAG_MIPMAPPED) == 0);
        if (mContext.isDiscarded()) {
            return null;
        }
        if (!pixelMap.getInfo().isValid()) {
            return null;
        }
        if (!pixelRef.isImmutable()) {
            return null;
        }
        var format = mContext.getCaps()
                .getDefaultBackendFormat(dstColorType, /*renderable*/ false);
        if (format == null) {
            return null;
        }
        var srcColorType = pixelMap.getColorType();
        var width = pixelMap.getWidth();
        var height = pixelMap.getHeight();
        @SharedPtr
        var texture = createLazyTexture(format, width, height, surfaceFlags,
                new PixelsCallback(pixelRef, srcColorType, dstColorType));
        if (texture == null) {
            return null;
        }
        if (!isDeferredProvider()) {
            texture.doLazyInstantiation(mDirect.getResourceProvider());
        }
        return texture;
    }

    private static final class PixelsCallback implements SurfaceProxy.LazyInstantiateCallback {

        private PixelRef mPixelRef;
        private final int mSrcColorType;
        private final int mDstColorType;

        public PixelsCallback(PixelRef pixelRef, int srcColorType, int dstColorType) {
            mPixelRef = RefCnt.create(pixelRef);
            mSrcColorType = srcColorType;
            mDstColorType = dstColorType;
        }

        @Override
        public SurfaceProxy.LazyCallbackResult onLazyInstantiate(
                ResourceProvider provider,
                BackendFormat format,
                int width, int height,
                int sampleCount,
                int surfaceFlags,
                String label) {
            //TODO implement fast pixel transfer from heap array
            assert mPixelRef.getBase() == null;
            @SharedPtr
            GpuImage texture = provider.createTexture(
                    width, height,
                    format,
                    sampleCount,
                    surfaceFlags,
                    mDstColorType,
                    mSrcColorType,
                    mPixelRef.getRowStride(),
                    mPixelRef.getAddress(),
                    label);
            close();
            return new SurfaceProxy.LazyCallbackResult(texture);
        }

        @Override
        public void close() {
            mPixelRef = RefCnt.move(mPixelRef);
        }
    }

    /**
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_PROTECTED
     * @see ISurface#FLAG_SKIP_ALLOCATOR
     */
    @Nullable
    @SharedPtr
    public RenderTargetProxy createRenderTexture(BackendFormat format,
                                                 int width, int height,
                                                 int sampleCount,
                                                 int surfaceFlags) {
        assert mContext.isOwnerThread();
        if (mContext.isDiscarded()) {
            return null;
        }

        if (format.isCompressed()) {
            // Deferred proxies for compressed textures are not supported.
            return null;
        }

        if (!mContext.getCaps().validateSurfaceParams(width, height, format, sampleCount, surfaceFlags)) {
            return null;
        }

        if (isDeferredProvider()) {
            surfaceFlags |= ISurface.FLAG_DEFERRED_PROVIDER;
        } else {
            assert (surfaceFlags & ISurface.FLAG_DEFERRED_PROVIDER) == 0;
        }

        return new RenderTargetProxy(format, width, height, sampleCount, surfaceFlags);
    }

    /**
     * Create a RenderTarget that wraps a backend texture and is both texturable and renderable.
     * <p>
     * The texture must be single sampled and will be used as the color attachment 0 of the non-MSAA
     * render target. If <code>sampleCount</code> is > 1, the underlying API uses separate MSAA render
     * buffers then a MSAA render buffer is created that resolves to the texture.
     */
    @Nullable
    @SharedPtr
    public RenderTargetProxy wrapRenderableBackendTexture(BackendImage texture,
                                                          int sampleCount,
                                                          boolean ownership,
                                                          boolean cacheable,
                                                          Runnable releaseCallback) {
        if (mContext.isDiscarded()) {
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

    @Nullable
    @SharedPtr
    public RenderTargetProxy wrapBackendRenderTarget(BackendRenderTarget backendRenderTarget,
                                                     Runnable rcReleaseCB) {
        if (mContext.isDiscarded()) {
            return null;
        }

        // This is only supported on a direct Context.
        if (mDirect == null) {
            return null;
        }

        @SharedPtr
        var fsr = mDirect.getResourceProvider()
                .wrapBackendRenderTarget(backendRenderTarget);
        if (fsr == null) {
            return null;
        }

        return new RenderTargetProxy(fsr, 0);
    }

    /**
     * Creates a texture that will be instantiated by a user-supplied callback during flush.
     * The width and height must either both be greater than 0 or both less than or equal to zero. A
     * non-positive value is a signal that the width height are currently unknown. The texture will
     * not be renderable.
     *
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_PROTECTED
     * @see ISurface#FLAG_READ_ONLY
     * @see ISurface#FLAG_SKIP_ALLOCATOR
     */
    @Nullable
    @SharedPtr
    public ImageProxy createLazyTexture(BackendFormat format,
                                        int width, int height,
                                        int surfaceFlags,
                                        SurfaceProxy.LazyInstantiateCallback callback) {
        mContext.checkOwnerThread();
        if (mContext.isDiscarded()) {
            return null;
        }
        assert (width <= 0 && height <= 0)
                || (width > 0 && height > 0);
        Objects.requireNonNull(callback);
        if (format == null || format.getBackend() != mContext.getBackend()) {
            return null;
        }
        if (width > mContext.getCaps().maxTextureSize() ||
                height > mContext.getCaps().maxTextureSize()) {
            return null;
        }
        if (isDeferredProvider()) {
            surfaceFlags |= ISurface.FLAG_DEFERRED_PROVIDER;
        } else {
            assert (surfaceFlags & ISurface.FLAG_DEFERRED_PROVIDER) == 0;
        }
        return new ImageProxy(format, width, height, surfaceFlags, callback);
    }

    public boolean isDeferredProvider() {
        return mDirect == null;
    }
}
