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

import icyllis.arcui.core.SharedPtr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class delays the acquisition of a {@link Texture} until they are actually required.
 */
public final class TextureProxy extends SurfaceProxy {

    // For deferred proxies it will be null until the proxy is instantiated.
    // For wrapped proxies it will point to the wrapped resource.
    @SharedPtr
    Texture mTexture;

    LazyInstantiateCallback mLazyInstantiateCallback;

    // Deferred version - no data
    TextureProxy(BackendFormat format,
                 int width, int height,
                 boolean mipmapped,
                 boolean backingFit,
                 boolean budgeted,
                 int surfaceFlags,
                 boolean useAllocator,
                 boolean deferredProvider) {
        super(format, width, height, mipmapped, backingFit, budgeted,
                surfaceFlags, useAllocator, deferredProvider);
        assert width > 0 && height > 0; // non-lazy
    }

    // Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
    TextureProxy(BackendFormat format,
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
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        assert (width < 0 && height < 0 && backingFit == EngineTypes.BACKING_FIT_APPROX) ||
                (width > 0 && height > 0);
    }

    // Wrapped version - shares the UniqueID of the passed surface.
    // Takes UseAllocator because even though this is already instantiated it still can participate
    // in allocation by having its backing resource recycled to other uninstantiated proxies or
    // not depending on UseAllocator.
    TextureProxy(Texture texture,
                 boolean useAllocator,
                 boolean deferredProvider) {
        super(texture.getBackendFormat(), texture.getWidth(), texture.getHeight(),
                texture.isMipmapped(), EngineTypes.BACKING_FIT_EXACT,
                texture.getBudgetType() == EngineTypes.BUDGET_TYPE_BUDGETED,
                texture.getFlags(), useAllocator, deferredProvider);
        mTexture = texture; // std::move
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
        assert !isFullyLazy();
        if (mTexture != null) {
            return mTexture.getWidth();
        }
        if (mBackingFit == EngineTypes.BACKING_FIT_EXACT) {
            return mWidth;
        }
        return ResourceProvider.makeApprox(mWidth);
    }

    @Override
    public int getBackingHeight() {
        assert !isFullyLazy();
        if (mTexture != null) {
            return mTexture.getHeight();
        }
        if (mBackingFit == EngineTypes.BACKING_FIT_EXACT) {
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
        if (mTexture != null) {
            assert mUniqueKey == null ||
                    mTexture.mUniqueKey != null && mTexture.mUniqueKey.equals(mUniqueKey);
            return true;
        }

        assert !mMipmapped || mBackingFit == EngineTypes.BACKING_FIT_EXACT;

        final Texture texture;
        if (mBackingFit == EngineTypes.BACKING_FIT_APPROX) {
            texture = provider.createApproxTexture(mWidth, mHeight, mFormat, isProtected());
        } else {
            texture = provider.createTexture(mWidth, mHeight, mFormat, mMipmapped, mBudgeted, isProtected());
        }
        if (texture == null) {
            return false;
        }

        // If there was an invalidation message pending for this key, we might have just processed it,
        // causing the key (stored on this proxy) to become invalid.
        if (mUniqueKey != null) {
            provider.assignUniqueKeyToResource(mUniqueKey, texture);
        }

        assert mTexture == null;
        assert texture.getBackendFormat().equals(mFormat);
        mTexture = texture;

        return true;
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
        // use proxy params
        return Texture.computeSize(mFormat, mWidth, mHeight,
                1, mMipmapped, mBackingFit == EngineTypes.BACKING_FIT_APPROX);
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
    public ResourceKey computeScratchKey(Caps caps) {
        // use backing store params
        return Surface.computeScratchKey(mFormat, getBackingWidth(), getBackingHeight(),
                1, isMipmapped(), isProtected(), new Surface.Key());
    }

    @Override
    public boolean instantiateSurface(ResourceProvider provider, ResourceAllocator.Register register) {
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
        LazyCallbackResult onLazyInstantiate(ResourceProvider provider,
                                             BackendFormat format,
                                             int width, int height,
                                             boolean fit,
                                             boolean mipmapped,
                                             boolean budgeted,
                                             boolean isProtected);
    }
}
