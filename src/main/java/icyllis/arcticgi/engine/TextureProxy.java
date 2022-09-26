/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.SharedPtr;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

import static icyllis.arcticgi.engine.Engine.*;

public class TextureProxy extends SurfaceProxy {

    LazyInstantiateCallback mLazyInstantiateCallback;

    boolean mIsDDLTarget = false;
    boolean mIsPromiseProxy = false;

    int mTaskTargetCount = 0;

    /**
     * For deferred proxies it will be null until the proxy is instantiated.
     * For wrapped proxies it will point to the wrapped resource.
     */
    @SharedPtr
    protected Texture mTexture;

    /**
     * This tracks the mipmap status at the proxy level and is thus somewhat distinct from the
     * backing Texture's mipmap status. In particular, this status is used to determine when
     * mipmap levels need to be explicitly regenerated during the execution of a DAG of opsTasks.
     * <p>
     * Only meaningful if {@link #isProxyMipmapped} returns true.
     */
    boolean mMipmapsDirty = true;

    /**
     * Should the target's unique key be synced with ours.
     */
    boolean mSyncTargetKey = true;

    Object mUniqueKey;
    /**
     * Only set when 'mUniqueKey' is non-null.
     */
    ProxyProvider mProxyProvider;

    /**
     * Deferred version - no data
     */
    public TextureProxy(BackendFormat format,
                        int width, int height,
                        int surfaceFlags) {
        super(format, width, height, surfaceFlags);
        assert (width > 0 && height > 0); // non-lazy
    }

    /**
     * Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
     */
    public TextureProxy(BackendFormat format,
                        int width, int height,
                        int surfaceFlags,
                        LazyInstantiateCallback callback) {
        super(format, width, height, surfaceFlags);
        mLazyInstantiateCallback = callback;
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        assert (width < 0 && height < 0 && (surfaceFlags & SurfaceFlag_BackingFit) == 0) ||
                (width > 0 && height > 0);
    }

    /**
     * Wrapped version - shares the UniqueID of the passed texture.
     * <p>
     * Takes UseAllocator because even though this is already instantiated it still can participate
     * in allocation by having its backing resource recycled to other uninstantiated proxies or
     * not depending on UseAllocator.
     */
    public TextureProxy(@SharedPtr Texture texture,
                        int surfaceFlags) {
        super(texture, surfaceFlags);
        mMipmapsDirty = texture.isMipmapsDirty();
        assert (mSurfaceFlags & SurfaceFlag_BackingFit) != 0;
        assert (mFormat.getTextureType() == texture.getTextureType());
        assert (texture.isMipmapped()) == ((mSurfaceFlags & SurfaceFlag_Mipmapped) != 0);
        assert (texture.getBudgetType() == BudgetType_Budgeted) == ((mSurfaceFlags & SurfaceFlag_Budgeted) != 0);
        assert (texture.getTextureType() != TextureType_External) || ((mSurfaceFlags & SurfaceFlag_ReadOnly) != 0);
        mTexture = texture; // std::move
        if (texture.getUniqueKey() != null) {
            assert (texture.getContext() != null);
            mProxyProvider = texture.getContext().getProxyProvider();
            mProxyProvider.adoptUniqueKeyFromSurface(this, texture);
        }
    }

    public static class LazyCallbackResult {

        public Texture mTexture;
        /**
         * LazyInstantiationKeyMode:
         * <ul>
         *     <li>False: Don't key the {@link Texture} with the proxy's key. The lazy
         *     instantiation callback is free to return a {@link Texture} that already
         *     has a unique key unrelated to the proxy's key.</li>
         *     <li>True: Keep the {@link Texture}'s unique key in sync with the proxy's
         *     unique key. The {@link Texture} returned from the lazy instantiation callback
         *     must not have a unique key or have the same same unique key as the proxy.
         *     If the proxy is later assigned a key it is in turn assigned to the
         *     {@link Texture}.</li>
         * </ul>
         */
        public boolean mKeyMode = true;
        /**
         * Should the callback be disposed of after it has returned or preserved until the proxy
         * is freed. Only honored if mTexture is not-null. If it is null the callback is preserved.
         */
        public boolean mReleaseCallback = true;
    }

    /**
     * Lazy-callback function.
     */
    @FunctionalInterface
    public interface LazyInstantiateCallback {

        /**
         * Specifies the expected properties of the {@link Texture} returned by a lazy instantiation
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

    @Override
    protected void dispose() {
        // Due to the order of cleanup the Texture this proxy may have wrapped may have gone away
        // at this point. Zero out the pointer so the cache invalidation code doesn't try to use it.
        mTexture = Resource.move(mTexture);

        // In DDL-mode, uniquely keyed proxies keep their key even after their originating
        // proxy provider has gone away. In that case there is no-one to send the invalid key
        // message to (Note: in this case we don't want to remove its cached resource).
        if (mUniqueKey != null && mProxyProvider != null) {
            mProxyProvider.processInvalidUniqueKey(mUniqueKey, this, false);
        } else {
            assert (mProxyProvider == null);
        }
    }

    /**
     * @return true if it has a lazy callback and not instantiated
     */
    public boolean isLazy() {
        return mTexture == null && mLazyInstantiateCallback != null;
    }

    /**
     * @return true if it has a lazy callback, not instantiated, backing fit is approx and dimension is not known
     */
    public final boolean isLazyMost() {
        boolean result = mWidth < 0;
        assert (result == (mHeight < 0)) && (!result || isLazy());
        return result;
    }

    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mTexture != null) {
            return mTexture.getWidth();
        }
        if ((mSurfaceFlags & SurfaceFlag_BackingFit) != 0) {
            return mWidth;
        }
        return ResourceProvider.makeApprox(mWidth);
    }

    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mTexture != null) {
            return mTexture.getHeight();
        }
        if ((mSurfaceFlags & SurfaceFlag_BackingFit) != 0) {
            return mHeight;
        }
        return ResourceProvider.makeApprox(mHeight);
    }

    /**
     * If set to exact or approx size is equal to exact size. Must call when not fully lazy.
     *
     * @return true if backing fit is (as if) exact
     * @see #isProxyExact()
     */
    public final boolean isExact() {
        assert !isLazyMost();
        if ((mSurfaceFlags & SurfaceFlag_BackingFit) != 0) {
            return true;
        }
        return mWidth == ResourceProvider.makeApprox(mWidth) &&
                mHeight == ResourceProvider.makeApprox(mHeight);
    }

    public Object getBackingUniqueID() {
        if (mTexture != null) {
            return mTexture;
        }
        return mUniqueID;
    }

    /**
     * Actually instantiate the backing stores, if necessary. Render thread only.
     *
     * @param provider the resource provider to create textures
     * @return success or not
     */
    public boolean instantiate(ResourceProvider provider) {
        if (isLazy()) {
            return false;
        }
        if (mTexture != null) {
            assert mUniqueKey == null ||
                    mTexture.mUniqueKey != null && mTexture.mUniqueKey.equals(mUniqueKey);
            return true;
        }

        assert ((mSurfaceFlags & SurfaceFlag_Mipmapped) == 0) ||
                ((mSurfaceFlags & SurfaceFlag_BackingFit) != 0);

        final Texture texture;
        if ((mSurfaceFlags & SurfaceFlag_BackingFit) == 0) {
            texture = provider.createApproxTexture(mWidth, mHeight, mFormat, isProtected());
        } else {
            texture = provider.createTexture(mWidth, mHeight, mFormat, mSurfaceFlags);
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

    /**
     * De-instantiate. Called after instantiated.
     */
    public void clear() {
        assert mTexture != null;
        mTexture.unref();
        mTexture = null;
    }

    /**
     * Proxies that are already instantiated and whose backing texture cannot be recycled to
     * instantiate other proxies do not need to be considered by {@link ResourceAllocator}.
     */
    public final boolean canSkipResourceAllocator() {
        if ((mSurfaceFlags & SurfaceFlag_SkipAllocator) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        Texture texture = peekTexture();
        if (texture == null) {
            return false;
        }
        // If this resource is already allocated and not recyclable then the resource allocator does
        // not need to do anything with it.
        return texture.mScratchKey == null;
    }

    /**
     * Return the texture proxy's unique key. It will be null if the proxy doesn't have one.
     */
    @Nullable
    public final Object getUniqueKey() {
        return mUniqueKey;
    }

    public boolean isInstantiated() {
        return mTexture != null;
    }

    /**
     * Called when this task becomes a target of a {@link RenderTask}.
     */
    public final void isUsedAsTaskTarget() {
        ++mTaskTargetCount;
    }

    /**
     * How many render tasks has this proxy been the target of?
     */
    public final int getTaskTargetCount() {
        return mTaskTargetCount;
    }

    /**
     * If this proxy is already instantiated, return its backing {@link Texture};
     * if not, return null.
     */
    @Nullable
    @Override
    public Texture peekTexture() {
        return mTexture;
    }

    /**
     * Does the resource count against the resource budget?
     * <p>
     * Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     */
    public final boolean isBudgeted() {
        return (mSurfaceFlags & SurfaceFlag_Budgeted) != 0;
    }

    /**
     * The pixel values of this proxy's texture cannot be modified (e.g. doesn't support write
     * pixels or MIP map level regen). Read-only proxies also bypass interval tracking and
     * assignment in ResourceAllocator.
     */
    public final boolean isReadOnly() {
        return (mSurfaceFlags & SurfaceFlag_ReadOnly) != 0;
    }

    public final boolean isProtected() {
        return (mSurfaceFlags & SurfaceFlag_Protected) != 0;
    }

    /**
     * Retrieves the amount of server memory that will be or currently is used by this resource
     * in bytes. It is approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of server memory used in bytes
     */
    public long getMemorySize() {
        // use proxy params
        return Texture.computeSize(mFormat, mWidth, mHeight, 1,
                (mSurfaceFlags & SurfaceFlag_Mipmapped) != 0,
                (mSurfaceFlags & SurfaceFlag_BackingFit) == 0);
    }

    public final boolean isDDLTarget() {
        return mIsDDLTarget;
    }

    public final boolean isPromiseProxy() {
        return mIsPromiseProxy;
    }

    /**
     * If we are instantiated and have a target, return the mip state of that target. Otherwise,
     * returns the proxy's mip state from creation time. This is useful for lazy proxies which may
     * claim to not need mips at creation time, but the instantiation happens to give us a mipmapped
     * target. In that case we should use that for our benefit to avoid possible copies/mip
     * generation later.
     */
    @Override
    public boolean isMipmapped() {
        if (mTexture != null) {
            return mTexture.isMipmapped();
        }
        return (mSurfaceFlags & SurfaceFlag_Mipmapped) != 0;
    }

    public final boolean areMipmapsDirty() {
        return mMipmapsDirty && isProxyMipmapped();
    }

    public final void markMipmapsDirty() {
        assert isProxyMipmapped();
        mMipmapsDirty = true;
    }

    public final void markMipmapsClean() {
        assert isProxyMipmapped();
        mMipmapsDirty = false;
    }

    /**
     * Returns the Mipmapped value of the proxy from creation time regardless of whether it has
     * been instantiated or not.
     */
    public final boolean isProxyMipmapped() {
        return (mSurfaceFlags & SurfaceFlag_Mipmapped) != 0;
    }

    public final int getTextureType() {
        return mFormat.getTextureType();
    }

    /**
     * If true then the texture does not support MIP maps and only supports clamp wrap mode.
     */
    public final boolean hasRestrictedSampling() {
        return textureTypeHasRestrictedSampling(mFormat.getTextureType());
    }

    @Override
    public final TextureProxy asTextureProxy() {
        return this;
    }

    /**
     * Same as {@link Texture.ScratchKey} for {@link ResourceAllocator}.
     */
    @Override
    public int hashCode() {
        int result = getBackingWidth();
        result = 31 * result + getBackingHeight();
        result = 31 * result + mFormat.getKey();
        result = 31 * result + ((isMipmapped() ? 1 : 0) | (isProtected() ? 2 : 0) | (1 << 2));
        return result;
    }

    /**
     * Same as {@link Texture.ScratchKey} for {@link ResourceAllocator}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Texture.ScratchKey key) {
            // ResourceCache
            return key.mWidth == getBackingWidth() &&
                    key.mHeight == getBackingHeight() &&
                    key.mFormat == mFormat.getKey() &&
                    key.mFlags == ((isMipmapped() ? 1 : 0) | (isProtected() ? 2 : 0) | (1 << 2));
        } else if (o instanceof TextureProxy proxy) {
            // ResourceAllocator
            return proxy.getBackingWidth() == getBackingWidth() &&
                    proxy.getBackingHeight() == getBackingHeight() &&
                    proxy.mFormat.getKey() == mFormat.getKey() &&
                    proxy.isMipmapped() == isMipmapped() &&
                    proxy.isProtected() == isProtected();
        }
        return false;
    }

    // DO NOT ABUSE!!
    @ApiStatus.Internal
    public final boolean isProxyExact() {
        return (mSurfaceFlags & SurfaceFlag_BackingFit) != 0;
    }

    // DO NOT ABUSE!!
    @ApiStatus.Internal
    public final void makeProxyExact(boolean allocatedCaseOnly) {
        assert !isLazyMost();
        if ((mSurfaceFlags & SurfaceFlag_BackingFit) != 0) {
            return;
        }

        final Texture texture = peekTexture();
        if (texture != null) {
            // The Approx but already instantiated case. Setting the proxy's width & height to
            // the instantiated width & height could have side-effects going forward, since we're
            // obliterating the area of interest information. This call only used
            // when converting an SpecialImage to an Image so the proxy shouldn't be
            // used for additional draws.
            mWidth = texture.getWidth();
            mHeight = texture.getHeight();
            return;
        }

        // In the post-implicit-allocation world we can't convert this proxy to be exact fit
        // at this point. With explicit allocation switching this to exact will result in a
        // different allocation at flush time. With implicit allocation, allocation would occur
        // at draw time (rather than flush time) so this pathway was encountered less often (if
        // at all).
        if (allocatedCaseOnly) {
            return;
        }

        // The Approx uninstantiated case. Making this proxy be exact should be okay.
        // It could mess things up if prior decisions were based on the approximate size.
        mSurfaceFlags |= SurfaceFlag_BackingFit;
        // If GpuMemorySize is used when caching specialImages for the image filter DAG. If it has
        // already been computed we want to leave it alone so that amount will be removed when
        // the special image goes away. If it hasn't been computed yet it might as well compute the
        // exact amount.
    }

    // Once the dimensions of a fully-lazy proxy are decided, and before it gets instantiated, the
    // client can use this optional method to specify the proxy's dimensions. (A proxy's dimensions
    // can be less than the GPU surface that backs it. e.g., BackingFit_Approx.) Otherwise,
    // the proxy's dimensions will be set to match the underlying GPU surface upon instantiation.
    @ApiStatus.Internal
    public final void setLazyDimension(int width, int height) {
        assert isLazyMost();
        assert width > 0 && height > 0;
        mWidth = width;
        mHeight = height;
    }

    @ApiStatus.Internal
    public boolean doLazyInstantiation(ResourceProvider provider) {
        return false;
    }

    @ApiStatus.Internal
    public void setIsDDLTarget() {
        mIsDDLTarget = true;
    }

    @ApiStatus.Internal
    public void setIsPromiseProxy() {
        mIsPromiseProxy = true;
    }
}
