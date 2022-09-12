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

import icyllis.arcticgi.core.*;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <code>TextureProxy</code> targets a {@link Texture} with three instantiation
 * methods: Deferred, Lazy-callback and Wrapped.
 * <p>
 * Target: The backing texture that referenced by this proxy.
 * <p>
 * Instantiate: Create new textures or find textures in {@link ResourceCache}
 * when they are actually required on flush.
 * <p>
 * BackingFit: Indicates whether a backing store needs to be an exact match or can be
 * larger than is strictly necessary. True: Exact; False: Approx.
 * <p>
 * UseAllocator:
 * <ul>
 *     <li>False: This proxy will be instantiated outside the allocator (e.g.
 *     for proxies that are instantiated in on-flush callbacks).</li>
 *     <li>True: {@link ResourceAllocator} should instantiate this proxy.</li>
 * </ul>
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
 * <p>
 * Use {@link ProxyProvider} to obtain {@link TextureProxy} objects.
 */
public class TextureProxy extends SurfaceProxy {

    /**
     * For wrapped resources, 'mFormat' and 'mDimensions' will always be filled in from the
     * wrapped resource.
     */
    final BackendFormat mFormat;
    int mWidth;
    int mHeight;

    /**
     * BackingFit: Indicates whether a backing store needs to be an exact match or can be
     * larger than is strictly necessary. False: Approx; True: Exact.
     * <p>
     * Always Approx for lazy-callback resources;
     * always Exact for wrapped resources.
     *
     * @see CoreTypes#BackingFit_Approx
     * @see CoreTypes#BackingFit_Exact
     */
    boolean mBackingFit;
    /**
     * Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     *
     * @see CoreTypes#Budgeted_No
     * @see CoreTypes#Budgeted_Yes
     */
    boolean mBudgeted;
    /**
     * UseAllocator:
     * <ul>
     *     <li>False: This proxy will be instantiated outside the allocator (e.g.
     *     for proxies that are instantiated in on-flush callbacks).</li>
     *     <li>True: {@link ResourceAllocator} should instantiate this proxy.</li>
     * </ul>
     */
    boolean mUseAllocator;

    /**
     * Set from the backing resource for wrapped resources.
     */
    final int mUniqueID;

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
     * In many cases these flags aren't actually known until the proxy has been instantiated.
     * However, Engine frequently needs to change its behavior based on these settings. For
     * internally create proxies we will know these properties ahead of time. For wrapped
     * proxies we will copy the properties off of the {@link Texture}. For lazy proxies we
     * force the call sites to provide the required information ahead of time. At
     * instantiation time we verify that the assumed properties match the actual properties.
     */
    protected int mSurfaceFlags;

    /**
     * @see EngineTypes#Mipmapped_No
     * @see EngineTypes#Mipmapped_Yes
     */
    boolean mMipmapped;

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

    /**
     * For TextureProxies created in a DDL recording thread it is possible for the uniqueKey
     * to be cleared on the backing Texture while the uniqueKey remains on the proxy.
     * A 'mDeferredProvider' of TRUE loosens up asserts that the key of an instantiated
     * uniquely-keyed textureProxy is also always set on the backing Texture.
     */
    boolean mDeferredProvider;

    Object mUniqueKey;
    /**
     * Only set when 'mUniqueKey' is non-null.
     */
    ProxyProvider mProxyProvider;

    /**
     * Deferred version - no data
     *
     * @param deferredProvider A DDL recorder has its own proxy provider and proxy cache.
     */
    protected TextureProxy(BackendFormat format,
                           int width, int height,
                           boolean mipmapped,
                           boolean backingFit,
                           boolean budgeted,
                           int surfaceFlags,
                           boolean useAllocator,
                           boolean deferredProvider) {
        assert (format != null);
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mMipmapped = mipmapped;
        mBackingFit = backingFit;
        mBudgeted = budgeted;
        mSurfaceFlags = surfaceFlags;
        mUseAllocator = useAllocator;
        mDeferredProvider = deferredProvider;
        if (format.textureType() == EngineTypes.TextureType_External) {
            mSurfaceFlags |= EngineTypes.InternalSurfaceFlag_ReadOnly;
        }
        mUniqueID = GpuResource.createUniqueID();
        assert (width > 0 && height > 0); // non-lazy
    }

    /**
     * Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
     */
    protected TextureProxy(BackendFormat format,
                           int width, int height,
                           boolean mipmapped,
                           boolean backingFit,
                           boolean budgeted,
                           int surfaceFlags,
                           boolean useAllocator,
                           boolean deferredProvider,
                           LazyInstantiateCallback callback) {
        assert (format != null);
        assert (callback != null);
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mMipmapped = mipmapped;
        mBackingFit = backingFit;
        mBudgeted = budgeted;
        mSurfaceFlags = surfaceFlags;
        mUseAllocator = useAllocator;
        mDeferredProvider = deferredProvider;
        if (format.textureType() == EngineTypes.TextureType_External) {
            mSurfaceFlags |= EngineTypes.InternalSurfaceFlag_ReadOnly;
        }
        mUniqueID = GpuResource.createUniqueID();
        mLazyInstantiateCallback = callback;
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        assert (width < 0 && height < 0 && backingFit == CoreTypes.BackingFit_Approx) ||
                (width > 0 && height > 0);
    }

    /**
     * Wrapped version - shares the UniqueID of the passed texture.
     * <p>
     * Takes UseAllocator because even though this is already instantiated it still can participate
     * in allocation by having its backing resource recycled to other uninstantiated proxies or
     * not depending on UseAllocator.
     */
    protected TextureProxy(Texture texture,
                           boolean useAllocator,
                           boolean deferredProvider) {
        assert (texture != null);
        mFormat = texture.getBackendFormat();
        mWidth = texture.getWidth();
        mHeight = texture.getHeight();
        mMipmapped = texture.isMipmapped();
        mMipmapsDirty = texture.areMipmapsDirty();
        mBackingFit = CoreTypes.BackingFit_Exact;
        mBudgeted = texture.getBudgetType() == EngineTypes.BudgetType_Budgeted;
        mSurfaceFlags = texture.getFlags();
        mUseAllocator = useAllocator;
        mDeferredProvider = deferredProvider;
        assert (mFormat.textureType() == texture.getTextureType());
        if (mFormat.textureType() == EngineTypes.TextureType_External) {
            mSurfaceFlags |= EngineTypes.InternalSurfaceFlag_ReadOnly;
        }
        mTexture = texture; // std::move
        mUniqueID = texture.getUniqueID(); // converting from unique resource ID to a proxy ID
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
        if (mTexture != null) {
            mTexture.unref();
        }
        mTexture = null;

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

    public final int getWidth() {
        return mWidth;
    }

    public final int getHeight() {
        return mHeight;
    }

    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mTexture != null) {
            return mTexture.getWidth();
        }
        if (mBackingFit == CoreTypes.BackingFit_Exact) {
            return mWidth;
        }
        return ResourceProvider.makeApprox(mWidth);
    }

    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mTexture != null) {
            return mTexture.getHeight();
        }
        if (mBackingFit == CoreTypes.BackingFit_Exact) {
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
        if (mBackingFit == CoreTypes.BackingFit_Exact) {
            return true;
        }
        return mWidth == ResourceProvider.makeApprox(mWidth) &&
                mHeight == ResourceProvider.makeApprox(mHeight);
    }

    /**
     * @return the backend format of this proxy
     */
    @Nonnull
    public final BackendFormat getBackendFormat() {
        return mFormat;
    }

    public final boolean isFormatCompressed() {
        return mFormat.isCompressed();
    }

    /**
     * The contract for the unique ID is:
     * <ul>
     * <li>For wrapped resources:
     * the unique ID will match that of the wrapped resource</li>
     * <li>For deferred resources:
     *  <ul>
     *  <li>The unique ID will be different from the real resource, when it is allocated</li>
     *  <li>The proxy's unique ID will not change across the instantiates call</li>
     *  </ul>
     * </li>
     * <li> The unique IDs of the proxies and the resources draw from the same pool</li>
     * </ul>
     * What this boils down to is that the unique ID of a proxy can be used to consistently
     * track/identify a proxy but should never be used to distinguish between
     * resources and proxies - <b>beware!</b>
     */
    public final int getUniqueID() {
        return mUniqueID;
    }

    public int getBackingUniqueID() {
        if (mTexture != null) {
            return mTexture.getUniqueID();
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

        assert !mMipmapped || mBackingFit == CoreTypes.BackingFit_Exact;

        final Texture texture;
        if (mBackingFit == CoreTypes.BackingFit_Approx) {
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
        if (!mUseAllocator) {
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
    public Texture peekTexture() {
        return mTexture;
    }

    /**
     * Does the resource count against the resource budget?
     * <p>
     * Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     *
     * @see CoreTypes#Budgeted_No
     * @see CoreTypes#Budgeted_Yes
     */
    public final boolean isBudgeted() {
        return mBudgeted;
    }

    /**
     * The pixel values of this proxy's texture cannot be modified (e.g. doesn't support write
     * pixels or MIP map level regen). Read-only proxies also bypass interval tracking and
     * assignment in ResourceAllocator.
     */
    public final boolean isReadOnly() {
        return (mSurfaceFlags & EngineTypes.InternalSurfaceFlag_ReadOnly) != 0;
    }

    public final boolean isProtected() {
        return (mSurfaceFlags & EngineTypes.InternalSurfaceFlag_Protected) != 0;
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
        return Texture.computeSize(mFormat, mWidth, mHeight,
                1, mMipmapped, mBackingFit == CoreTypes.BackingFit_Approx);
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
    public boolean isMipmapped() {
        if (mTexture != null) {
            return mTexture.isMipmapped();
        }
        return mMipmapped;
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
        return mMipmapped;
    }

    public final int getTextureType() {
        return mFormat.textureType();
    }

    /**
     * If true then the texture does not support MIP maps and only supports clamp wrap mode.
     */
    public final boolean hasRestrictedSampling() {
        return EngineTypes.textureTypeHasRestrictedSampling(mFormat.textureType());
    }

    /**
     * Same as {@link Surface.ScratchKey} for {@link ResourceAllocator}.
     */
    @Override
    public int hashCode() {
        int result = getBackingWidth();
        result = 31 * result + getBackingHeight();
        result = 31 * result + mFormat.getFormatKey();
        result = 31 * result + ((isMipmapped() ? 1 : 0) | (isProtected() ? 2 : 0) | (1 << 2));
        return result;
    }

    /**
     * Same as {@link Surface.ScratchKey} for {@link ResourceAllocator}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Surface.ScratchKey key) {
            // ResourceCache
            return key.mWidth == getBackingWidth() &&
                    key.mHeight == getBackingHeight() &&
                    key.mFormat == mFormat.getFormatKey() &&
                    key.mFlags == ((isMipmapped() ? 1 : 0) | (isProtected() ? 2 : 0) | (1 << 2));
        } else if (o instanceof TextureProxy proxy) {
            // ResourceAllocator
            return proxy.getBackingWidth() == getBackingWidth() &&
                    proxy.getBackingHeight() == getBackingHeight() &&
                    proxy.mFormat.getFormatKey() == mFormat.getFormatKey() &&
                    proxy.isMipmapped() == isMipmapped() &&
                    proxy.isProtected() == isProtected();
        }
        return false;
    }

    // DO NOT ABUSE!!
    @ApiStatus.Internal
    public final boolean isProxyExact() {
        return mBackingFit == CoreTypes.BackingFit_Exact;
    }

    // DO NOT ABUSE!!
    @ApiStatus.Internal
    public final void makeProxyExact(boolean allocatedCaseOnly) {
        assert !isLazyMost();
        if (mBackingFit == CoreTypes.BackingFit_Exact) {
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
        mBackingFit = CoreTypes.BackingFit_Exact;
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
