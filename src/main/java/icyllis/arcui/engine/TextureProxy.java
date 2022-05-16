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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class delays the acquisition of a {@link Texture} until they are actually required.
 */
public final class TextureProxy extends SurfaceProxy {

    // For deferred proxies it will be null until the proxy is instantiated.
    // For wrapped proxies it will point to the wrapped resource.
    // For render target proxies it will be single sampled, or as resolve attachment
    Texture mTexture;

    LazyInstantiateCallback mLazyInstantiateCallback;

    // Deferred version
    public TextureProxy(BackendFormat format,
                        int width, int height,
                        boolean mipmapped,
                        boolean backingFit,
                        boolean budgeted,
                        int surfaceFlags,
                        boolean useAllocator,
                        boolean deferredProvider) {
        super(format, width, height, mipmapped, backingFit, budgeted,
                surfaceFlags, useAllocator, deferredProvider);
        // non-lazy
        assert width > 0 && height > 0;
    }

    // Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
    public TextureProxy(BackendFormat format,
                        int width, int height,
                        boolean mipmapped,
                        boolean backingFit,
                        boolean budgeted,
                        int surfaceFlags,
                        boolean useAllocator,
                        boolean deferredProvider,
                        LazyInstantiateCallback callback) {
        super(format, width, height, mipmapped, backingFit, budgeted,
                surfaceFlags, useAllocator, deferredProvider);
        mLazyInstantiateCallback = callback;
        // An "fully" proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        assert (width < 0 && height < 0 && !backingFit) ||
                (width > 0 && height > 0);
    }

    // Wrapped version - shares the UniqueID of the passed surface.
    // Takes UseAllocator because even though this is already instantiated it still can participate
    // in allocation by having its backing resource recycled to other uninstantiated proxies or
    // not depending on UseAllocator.
    public TextureProxy(Texture texture,
                        boolean useAllocator,
                        boolean deferredProvider) {
        super(texture.getBackendFormat(), texture.getWidth(), texture.getHeight(),
                texture.isMipmapped(), Types.BACKING_FIT_EXACT,
                texture.getBudgetType() == Types.BUDGET_TYPE_BUDGETED,
                texture.getFlags(), useAllocator, deferredProvider);
        mTexture = texture;
        mMipmapsDirty = texture.areMipmapsDirty();
        mUniqueID = texture.getUniqueID(); // converting from unique resource ID to a proxy ID
        if (texture.mUniqueKey != null) {
            mProxyProvider = texture.requireContext().getProxyProvider();
            mProxyProvider.adoptUniqueKeyFromSurface(this, texture);
        }
    }

    @Override
    protected void onFree() {
        // Due to the order of cleanup the Surface this proxy may have wrapped may have gone away
        // at this point. Zero out the pointer so the cache invalidation code doesn't try to use it.
        if (mTexture != null) {
            mTexture.unref();
        }
        mTexture = null;

        super.onFree();
    }

    @Override
    public boolean isLazy() {
        return mTexture == null && mLazyInstantiateCallback != null;
    }

    @Override
    public int getBackingWidth() {
        assert !isLazyLazy();
        if (mTexture != null) {
            return mTexture.getWidth();
        }
        if (mBackingFit == Types.BACKING_FIT_EXACT) {
            return mWidth;
        }
        return ResourceProvider.makeApprox(mWidth);
    }

    @Override
    public int getBackingHeight() {
        assert !isLazyLazy();
        if (mTexture != null) {
            return mTexture.getHeight();
        }
        if (mBackingFit == Types.BACKING_FIT_EXACT) {
            return mHeight;
        }
        return ResourceProvider.makeApprox(mHeight);
    }

    @Override
    public int getBackingUniqueID() {
        if (mTexture != null) {
            return mTexture.getUniqueID();
        }
        return mUniqueID;
    }

    @Override
    public boolean instantiate(ResourceProvider provider) {
        if (isLazy()) {
            return false;
        }
        return false;
    }

    @Override
    public void clear() {
        assert mTexture != null;
        mTexture.unref();
        mTexture = null;
    }

    @Override
    public boolean isInstantiated() {
        return mTexture != null;
    }

    @Nullable
    @Override
    public Texture peekTexture() {
        return mTexture;
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Override
    public boolean isMipmapped() {
        if (mTexture != null) {
            return mTexture.isMipmapped();
        }
        return mMipmapped;
    }

    @Nonnull
    @Override
    protected ResourceKey computeScratchKey(Caps caps) {
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

        public Texture mTexture;
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
                                  boolean fit,
                                  boolean mipmapped,
                                  boolean budgeted,
                                  boolean isProtected);
    }
}
