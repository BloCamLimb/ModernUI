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

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * SurfaceProxy targets a {@link Texture} or {@link RenderTarget} with three
 * instantiation methods: Deferred, Lazy-callback and Wrapped.
 * <p>
 * Target: The backing surface that referenced by this proxy.
 * <p>
 * Instantiate: Create new surfaces or find surfaces from {@link ResourceCache}
 * when needed for primary command buffers.
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
 *     <li>False: Don't key the Surface with the proxy's key. The lazy instantiation
 *     callback is free to return a Surface that already has a unique key unrelated
 *     to the proxy's key.</li>
 *     <li>True: Keep the Surface's unique key in sync with the proxy's unique key.
 *     The Surface returned from the lazy instantiation callback must not have a
 *     unique key or have the same same unique key as the proxy. If the proxy is
 *     later assigned a key it is in turn assigned to the Surface.</li>
 * </ul>
 * <p>
 * Use {@link ProxyProvider} to obtain <code>SurfaceProxy</code> objects.
 */
public abstract sealed class SurfaceProxy permits TextureProxy, RenderTargetProxy {

    private static final VarHandle REF_CNT;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            REF_CNT = lookup.findVarHandle(SurfaceProxy.class, "mRefCnt", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile int mRefCnt = 1;

    // for wrapped resources, 'mFormat' and 'mDimensions' will always be filled in from the
    // wrapped resource
    final BackendFormat mFormat;
    int mWidth;
    int mHeight;

    /**
     * BackingFit: Indicates whether a backing store needs to be an exact match or can be
     * larger than is strictly necessary. True: Exact; False: Approx.
     * <p>
     * Always Approx for lazy-callback resources, always exact for wrapped resources.
     */
    boolean mBackingFit;
    // always true for lazy-callback resources
    // set from the backing resource for wrapped resources
    // only meaningful if 'mLazyInstantiateCallback' is non-null
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

    // set from the backing resource for wrapped resources
    int mUniqueID;

    boolean mIsDeferredTarget = false;
    boolean mIsPromiseProxy = false;

    int mTaskTargetCount = 0;

    // In many cases these flags aren't actually known until the proxy has been instantiated.
    // However, engine frequently needs to change its behavior based on these settings. For
    // internally create proxies we will know these properties ahead of time. For wrapped
    // proxies we will copy the properties off of the Surface. For lazy proxies we force the
    // call sites to provide the required information ahead of time. At instantiation time
    // we verify that the assumed properties match the actual properties.
    int mSurfaceFlags;

    boolean mMipmapped;

    // This tracks the mipmap status at the proxy level and is thus somewhat distinct from the
    // backing Texture's mipmap status. In particular, this status is used to determine when
    // mipmap levels need to be explicitly regenerated during the execution of a DAG of opsTasks.
    boolean mMipmapsDirty = true; // only valid if isProxyMipmapped=true

    // Should the target's unique key be synced with ours.
    boolean mSyncTargetKey = true;

    // For TextureProxies created in a deferred recording thread it is possible for the uniqueKey
    // to be cleared on the backing Texture while the uniqueKey remains on the proxy.
    // A 'DeferredProvider' of TRUE loosens up asserts that the key of an instantiated
    // uniquely-keyed textureProxy is also always set on the backing Texture.
    boolean mDeferredProvider;

    ResourceKey mUniqueKey;
    // only set when 'mUniqueKey' is valid
    ProxyProvider mProxyProvider;

    protected SurfaceProxy(BackendFormat format,
                           int width, int height,
                           boolean mipmapped,
                           boolean backingFit,
                           boolean budgeted,
                           int surfaceFlags,
                           boolean useAllocator,
                           boolean deferredProvider) {
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mMipmapped = mipmapped;
        mBackingFit = backingFit;
        mBudgeted = budgeted;
        mSurfaceFlags = surfaceFlags;
        mUseAllocator = useAllocator;
        mDeferredProvider = deferredProvider;
        if (format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            mSurfaceFlags |= Types.INTERNAL_SURFACE_FLAG_READ_ONLY;
        }
    }

    /**
     * @return true if this proxy is uniquely referenced by the client pipeline
     */
    public final boolean unique() {
        // std::memory_order_acquire, maybe volatile?
        return (int) REF_CNT.getAcquire(this) == 1;
    }

    /**
     * Increases the reference count by 1 on the client pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    public final void ref() {
        // stronger than std::memory_order_relaxed
        REF_CNT.getAndAddRelease(this, 1);
    }

    /**
     * Decreases the reference count by 1 on the client pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    public final void unref() {
        // stronger than std::memory_order_acq_rel
        if ((int) REF_CNT.getAndAdd(this, -1) == 1) {
            onFree();
        }
    }

    /**
     * This must be used with caution. It is only valid to call this when 'threadIsolatedTestCnt'
     * refs are known to be isolated to the current thread. That is, it is known that there are at
     * least 'threadIsolatedTestCnt' refs for which no other thread may make a balancing unref()
     * call. Assuming the contract is followed, if this returns false then no other thread has
     * ownership of this. If it returns true then another thread *may* have ownership.
     */
    public final boolean isRefCntLT(int threadIsolatedTestCnt) {
        int cnt = (int) REF_CNT.getAcquire(this);
        // If this fails then the above contract has been violated.
        assert (cnt >= threadIsolatedTestCnt);
        return cnt <= threadIsolatedTestCnt;
    }

    protected void onFree() {
        // In DDL-mode, uniquely keyed proxies keep their key even after their originating
        // proxy provider has gone away. In that case there is no-one to send the invalid key
        // message to (Note: in this case we don't want to remove its cached resource).
        if (mUniqueKey != null && mProxyProvider != null) {
            mProxyProvider.processInvalidUniqueKey(mUniqueKey, this, false);
        } else {
            assert mProxyProvider == null;
        }
    }

    /**
     * @return true if it has a lazy callback and not instantiated
     */
    public abstract boolean isLazy();

    /**
     * @return true if it has a lazy callback, not instantiated, backing fit is approx and dimension is not known
     */
    public final boolean isFullyLazy() {
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

    public abstract int getBackingWidth();

    public abstract int getBackingHeight();

    /**
     * If set to exact or approx size is equal to exact size. Must call when not fully lazy.
     *
     * @return true if backing fit is (as if) exact
     * @see #isProxyExact()
     */
    public final boolean isExact() {
        assert !isFullyLazy();
        if (mBackingFit == Types.BACKING_FIT_EXACT) {
            return true;
        }
        return mWidth == ResourceProvider.makeApprox(mWidth) &&
                mHeight == ResourceProvider.makeApprox(mHeight);
    }

    /**
     * @return the backend format of this surface proxy
     */
    @Nonnull
    public final BackendFormat getBackendFormat() {
        return mFormat;
    }

    public final boolean isFormatCompressed() {
        return mFormat.isCompressed();
    }

    /**
     * The contract for the uniqueID is:
     * <ul>
     * <li>for wrapped resources:
     * the uniqueID will match that of the wrapped resource</li>
     * <li>
     * for deferred resources:
     *  <ul>
     *  <li>the uniqueID will be different from the real resource, when it is allocated</li>
     *  <li>the proxy's uniqueID will not change across the instantiates call</li>
     *  </ul>
     * </li>
     * <li>
     * the uniqueIDs of the proxies and the resources draw from the same pool</li>
     * </ul>
     * What this boils down to is that the uniqueID of a proxy can be used to consistently
     * track/identify a proxy but should never be used to distinguish between
     * resources and proxies - beware!
     */
    public final int getUniqueID() {
        return mUniqueID;
    }

    public abstract int getBackingUniqueID();

    /**
     * Actually instantiate the backing stores, if necessary. Render thread only.
     *
     * @param provider the resource provider to create surfaces
     * @return success or not
     */
    public abstract boolean instantiate(ResourceProvider provider);

    /**
     * De-instantiate. Called after instantiated.
     */
    public abstract void clear();

    /**
     * Proxies that are already instantiated and whose backing surface cannot be recycled to
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
    public final ResourceKey getUniqueKey() {
        return mUniqueKey;
    }

    public abstract boolean isInstantiated();

    /**
     * Called when this task becomes a target of a RenderTask.
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
     * If this is a texture proxy and the proxy is already instantiated, return its backing
     * Texture; if not, return null.
     */
    @Nullable
    public abstract Texture peekTexture();

    /**
     * Does the resource count against the resource budget?
     */
    public final boolean isBudgeted() {
        return mBudgeted;
    }

    /**
     * The pixel values of this proxy's surface cannot be modified (e.g. doesn't support write
     * pixels or MIP map level regen). Read-only proxies also bypass interval tracking and
     * assignment in ResourceAllocator.
     */
    public final boolean isReadOnly() {
        return (mSurfaceFlags & Types.INTERNAL_SURFACE_FLAG_READ_ONLY) != 0;
    }

    public final boolean isProtected() {
        return (mSurfaceFlags & Types.INTERNAL_SURFACE_FLAG_PROTECTED) != 0;
    }

    /**
     * Retrieves the amount of server memory that will be or currently is used by this resource
     * in bytes. It is approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of server memory used in bytes
     */
    public abstract long getMemorySize();

    public final boolean isDeferredTarget() {
        return mIsDeferredTarget;
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
    public abstract boolean isMipmapped();

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
        return mFormat.getTextureType();
    }

    /**
     * If true then the texture does not support MIP maps and only supports clamp wrap mode.
     */
    public final boolean hasRestrictedSampling() {
        return Types.textureTypeHasRestrictedSampling(mFormat.getTextureType());
    }

    /**
     * For {@link ResourceAllocator}.
     */
    @ApiStatus.Internal
    @Nonnull
    public abstract ResourceKey computeScratchKey(Caps caps);

    /**
     * For {@link ResourceAllocator}.
     */
    @ApiStatus.Internal
    public abstract boolean instantiateSurface(ResourceProvider provider, ResourceAllocator.Register register);

    // DO NOT ABUSE!!
    @ApiStatus.Internal
    public final boolean isProxyExact() {
        return mBackingFit == Types.BACKING_FIT_EXACT;
    }

    // DO NOT ABUSE!!
    @ApiStatus.Internal
    public final void makeProxyExact(boolean allocatedCaseOnly) {
        assert !isFullyLazy();
        if (mBackingFit == Types.BACKING_FIT_EXACT) {
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
        mBackingFit = Types.BACKING_FIT_EXACT;
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
        assert isFullyLazy();
        assert width > 0 && height > 0;
        mWidth = width;
        mHeight = height;
    }

    @ApiStatus.Internal
    public abstract void doLazyInstantiation(ResourceProvider provider);

    @ApiStatus.Internal
    public void setIsDeferredTarget() {
        mIsDeferredTarget = true;
    }

    @ApiStatus.Internal
    public void setIsPromiseProxy() {
        mIsPromiseProxy = true;
    }
}
