/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A factory for creating {@link SurfaceProxy}-derived objects. This class may be used on
 * the creating thread of {@link RecordingContext}.
 */
//TODO still WIP
public final class ImageProxyCache {

    private final Context mContext;

    // This holds the texture proxies that have unique keys. The resourceCache does not get a ref
    // on these proxies, but they must send a message to the resourceCache when they are deleted.
    private final Object2ObjectOpenHashMap<IUniqueKey, ImageViewProxy> mUniquelyKeyedProxies;

    ImageProxyCache(RecordingContext context) {
        mContext = context;
        /*if (context instanceof ImmediateContext) {
            mDirect = (ImmediateContext) context;
        } else {
            mDirect = null; // deferred
        }*/

        mUniquelyKeyedProxies = new Object2ObjectOpenHashMap<>();
    }

    /**
     * Assigns a unique key to a texture. The texture will be findable via this key using
     * {@link #findProxyByUniqueKey()}. It is an error if an existing texture already has a key.
     */
    public boolean assignUniqueKeyToProxy(IUniqueKey key, ImageViewProxy proxy) {
        assert key != null;
        if (mContext.isDeviceLost() || proxy == null) {
            return false;
        }

        // Only the provider that created a texture should be assigning unique keys to it.
        //assert isDeferredProvider() == ((proxy.mSurfaceFlags & ISurface.FLAG_DEFERRED_PROVIDER) != 0);

        // If there is already a Resource with this key then the caller has violated the
        // normal usage pattern of uniquely keyed resources (e.g., they have created one w/o
        // first seeing if it already existed in the cache).
        //assert mDirect == null || mDirect.getResourceCache().findAndRefUniqueResource(key) == null;

        // multiple proxies can't get the same key
        assert !mUniquelyKeyedProxies.containsKey(key);
        //TODO set

        mUniquelyKeyedProxies.put(key, proxy);
        return true;
    }

    public void dropUniqueRefs() {
    }

    public void dropUniqueRefsOlderThan(long nanoTime) {

    }

    /**
     * Sets the unique key of the provided texture to the unique key of the GPU texture.
     * The GPU texture must have a valid unique key.
     */
    public void adoptUniqueKeyFromSurface(ImageViewProxy proxy, GpuSurface texture) {
        //TODO
    }

    public void processInvalidUniqueKey(Object key, ImageViewProxy proxy, boolean invalidateResource) {
    }

    /**
     * Create a lazy {@link ImageViewProxy} without any data.
     *
     * @see ImageViewProxy
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_PROTECTED
     * @see ISurface#FLAG_SKIP_ALLOCATOR
     */
    @Deprecated
    @Nullable
    @SharedPtr
    public ImageViewProxy createTexture(BackendFormat format,
                                        int width, int height,
                                        int surfaceFlags) {
        assert mContext.isOwnerThread();
        if (mContext.isDeviceLost()) {
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

        //FIXME
        //return new ImageProxy(format, width, height, surfaceFlags);
        return null;
    }

    /**
     * Creates a lazy {@link ImageViewProxy} for the pixel map.
     *
     * @param pixmap     pixel map
     * @param pixels     raw ptr to pixel ref, must be immutable
     * @param dstColorType a color type for surface usage, see {@link ImageDesc}
     * @param surfaceFlags flags described as follows
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     */
    @Deprecated
    @Nullable
    @SharedPtr
    public ImageViewProxy createTextureFromPixels(@NonNull Pixmap pixmap,
                                                  @NonNull @RawPtr Pixels pixels,
                                                  int dstColorType,
                                                  int surfaceFlags) {
        mContext.checkOwnerThread();
        assert ((surfaceFlags & ISurface.FLAG_APPROX_FIT) == 0) ||
                ((surfaceFlags & ISurface.FLAG_MIPMAPPED) == 0);
        if (mContext.isDeviceLost()) {
            return null;
        }
        if (!pixmap.getInfo().isValid()) {
            return null;
        }
        if (!pixels.isImmutable()) {
            return null;
        }
        var format = mContext.getCaps()
                .getDefaultBackendFormat(dstColorType, /*renderable*/ false);
        if (format == null) {
            return null;
        }
        var srcColorType = pixmap.getColorType();
        var width = pixmap.getWidth();
        var height = pixmap.getHeight();
        @SharedPtr
        var texture = createLazyTexture(format, width, height, surfaceFlags,
                new PixelsCallback(pixels, srcColorType, dstColorType));
        if (texture == null) {
            return null;
        }
        if (!isDeferredProvider()) {
            //texture.doLazyInstantiation(mDirect.getResourceProvider());
        }
        return texture;
    }

    @Deprecated
    private static final class PixelsCallback implements SurfaceProxy.LazyInstantiateCallback {

        private Pixels mPixels;
        private final int mSrcColorType;
        private final int mDstColorType;

        public PixelsCallback(Pixels pixels, int srcColorType, int dstColorType) {
            mPixels = RefCnt.create(pixels);
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
            assert mPixels.getBase() == null;
            /*@SharedPtr
            Image texture = provider.createTexture(
                    width, height,
                    format,
                    sampleCount,
                    surfaceFlags,
                    mDstColorType,
                    mSrcColorType,
                    mPixels.getRowStride(),
                    mPixels.getAddress(),
                    label);*/
            close();
            //return new SurfaceProxy.LazyCallbackResult(texture);
            return null;
        }

        @Override
        public void close() {
            mPixels = RefCnt.move(mPixels);
        }
    }

    /**
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_MIPMAPPED
     * @see ISurface#FLAG_PROTECTED
     * @see ISurface#FLAG_SKIP_ALLOCATOR
     */
    @Deprecated
    @Nullable
    @SharedPtr
    public RenderTargetProxy createRenderTexture(BackendFormat format,
                                                 int width, int height,
                                                 int sampleCount,
                                                 int surfaceFlags) {
        assert mContext.isOwnerThread();
        if (mContext.isDeviceLost()) {
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
    @Deprecated
    @Nullable
    @SharedPtr
    public RenderTargetProxy wrapRenderableBackendTexture(BackendImage texture,
                                                          int sampleCount,
                                                          boolean ownership,
                                                          boolean cacheable,
                                                          Runnable releaseCallback) {
        if (mContext.isDeviceLost()) {
            return null;
        }

        /*// This is only supported on a direct Context.
        if (mDirect == null) {
            return null;
        }*/

        sampleCount = mContext.getCaps().getRenderTargetSampleCount(sampleCount, texture.getBackendFormat());
        assert sampleCount > 0;
        //TODO
        return null;
    }

    @Deprecated
    @Nullable
    @SharedPtr
    public RenderTargetProxy wrapBackendRenderTarget(BackendRenderTarget backendRenderTarget,
                                                     Runnable rcReleaseCB) {
        if (mContext.isDeviceLost()) {
            return null;
        }

        /*// This is only supported on a direct Context.
        if (mDirect == null) {
            return null;
        }

        @SharedPtr
        var fsr = mDirect.getResourceProvider()
                .wrapBackendRenderTarget(backendRenderTarget);
        if (fsr == null) {
            return null;
        }

        return new RenderTargetProxy(fsr, 0);*/
        return null;
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
    @Deprecated
    @Nullable
    @SharedPtr
    public ImageViewProxy createLazyTexture(BackendFormat format,
                                            int width, int height,
                                            int surfaceFlags,
                                            SurfaceProxy.LazyInstantiateCallback callback) {
        mContext.checkOwnerThread();
        if (mContext.isDeviceLost()) {
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
        //FIXME
        //return new ImageProxy(format, width, height, surfaceFlags, callback);
        return null;
    }

    public boolean isDeferredProvider() {
        return false;
    }
}
