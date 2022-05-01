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

import javax.annotation.Nonnull;

/**
 * This class delays the acquisition of a {@link Texture} and other {@link Surface Surfaces}
 * as a render target until they are actually required. Note that framebuffers and render
 * passes are provided by {@link Server}.
 * <p>
 * I would recommend Google's skia to use this instead of Texture[RenderTarget]Proxy diamond.
 * Ping E. Greg Daniel, Google Software Engineer
 */
public final class RenderTargetProxy extends SurfaceProxy {

    // non-null if MSAA is enabled, null otherwise
    Surface mMSAAColorBuffer;
    // non-null if depth/stencil is needed, null otherwise
    // MSAA or not depends on 'mMSAAColorBuffer' is non-null (MSAA) or null (single sample)
    Surface mDepthStencilBuffer;

    LazyInstantiateCallback mLazyInstantiateCallback;

    public RenderTargetProxy(BackendFormat format,
                             int width, int height,
                             boolean mipmapped,
                             int mipmapStatus,
                             boolean backingFit,
                             boolean budgeted,
                             int surfaceFlags,
                             boolean useAllocator,
                             boolean deferredProvider) {
        super(format, width, height, mipmapped, mipmapStatus, backingFit, budgeted, surfaceFlags, useAllocator, deferredProvider);
    }

    @Override
    public boolean isLazy() {
        return mTarget == null && mLazyInstantiateCallback != null;
    }

    @Override
    public boolean instantiate(ResourceProvider provider) {
        return false;
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Nonnull
    @Override
    protected Object computeScratchKey(Caps caps) {
        return null;
    }

    @Override
    protected boolean createSurface(ResourceProvider provider, ResourceAllocator.Register register) {
        return false;
    }

    @Override
    public void doLazyInstantiation(ResourceProvider provider) {

    }

    public static class LazyCallbackResult {

        public Texture mColorBuffer;
        public Surface mMSAAColorBuffer;
        public Surface mDepthStencilBuffer;
        /**
         * LazyInstantiationKeyMode:
         * <ul>
         *     <li>False: Don't key the Surface with the proxy's key. The lazy instantiation
         *     callback is free to return a Surface that already has a unique key unrelated
         *     to the proxy's key.</li>
         *     <li>True: Keep the Surface's unique key in sync with the proxy's unique key.
         *     The Surface returned from the lazy instantiation callback must not have a
         *     unique key or have the same same unique key as the proxy. If the proxy is
         *     later assigned a key it is in turn assigned to the Surface.</li>
         * </ul>
         */
        public boolean mKeyMode = true;
        /**
         * Should the callback be disposed of after it has returned or preserved until the proxy
         * is freed. Only honored if fSurface is not-null. If it is null the callback is preserved.
         */
        public boolean mReleaseCallback = true;
    }

    @FunctionalInterface
    public interface LazyInstantiateCallback {

        /**
         * Specifies the expected properties of the Surface returned by a lazy instantiation
         * callback. The dimensions will be negative in the case of a fully lazy proxy.
         */
        LazyCallbackResult invoke(ResourceProvider provider,
                                  BackendFormat format,
                                  int width, int height,
                                  int samples,
                                  boolean fit,
                                  boolean mipmapped,
                                  boolean budgeted,
                                  boolean isProtected);
    }
}
