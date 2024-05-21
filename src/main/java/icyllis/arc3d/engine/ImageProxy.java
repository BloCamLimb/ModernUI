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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * The {@link ImageProxy} implements the proxy pattern for {@link Image},
 * it targets an {@link Image} with three instantiation methods: deferred,
 * lazy-callback and wrapped.
 * <p>
 * Deferred version takes an {@link ImageDesc}, and will be instantiated
 * as needed automatically.
 * <p>
 * Lazy-callback version takes an {@link ImageDesc}, but will be instantiated
 * via a user-defined callback. There is a special case where the extent of the
 * image is uncertain before the callback is invoked. This is known as lazy-most,
 * and the extent of {@link ImageDesc} can be arbitrary (1x1x1 by convention).
 * <p>
 * Wrapped version takes an existing {@link Image} without further instantiation.
 * <p>
 * Target: The backing GPU {@link Image} that referenced by this proxy.
 * <p>
 * Instantiate: Find or create GPU {@link Image} via {@link ResourceProvider}
 * when they are actually required on flush.
 * <p>
 * BackingFit: Indicates whether a backing store needs to be an exact match or
 * can be larger than is strictly necessary. True: Exact; False: Approx. See
 * {@link ISurface#FLAG_APPROX_FIT}, the default is exact.
 * <p>
 * UseAllocator:
 * <ul>
 *     <li>False: This surface will be instantiated outside the allocator (e.g.
 *     for surfaces that are instantiated in on-flush callbacks).</li>
 *     <li>True: {@link SurfaceAllocator} should instantiate this surface.</li>
 * </ul>
 * <p>
 * Threading: Proxies may be created on the owner thread of a {@link Context},
 * and change the reference count through {@link #ref()} and {@link #unref()}.
 * The proxy may be disposed on either the owner thread of its creating context,
 * or the owner thread of its immediate context.
 *
 * @see ImageProxyView
 * @see ImageProxyCache
 */
public final class ImageProxy extends RefCnt {

    ImageDesc mDesc;

    /**
     * For deferred textures it will be null until the backing store is instantiated.
     * For wrapped textures it will point to the wrapped resource.
     */
    @SharedPtr
    Image mImage;

    /**
     * BackingFit: Indicates whether a backing store needs to be an exact match or can be
     * larger than is strictly necessary. Always approx for lazy-callback resources;
     * always exact for wrapped resources.
     * <p>
     * Budgeted: Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     * <p>
     * UseAllocator:
     * <ul>
     *     <li>False: This surface will be instantiated outside the allocator (e.g.
     *     for proxies that are instantiated in on-flush callbacks).</li>
     *     <li>True: {@link SurfaceAllocator} should instantiate this surface.</li>
     * </ul>
     * <p>
     * DeferredProvider: For {@link ImageProxy}s created in a deferred list recording thread it is
     * possible for the uniqueKey to be cleared on the backing Texture while the uniqueKey
     * remains on the surface. A 'mDeferredProvider' of 'true' loosens up asserts that the key of an
     * instantiated uniquely-keyed texture is also always set on the backing {@link Image}.
     * <p>
     * In many cases these flags aren't actually known until the surface has been instantiated.
     * However, Engine frequently needs to change its behavior based on these settings. For
     * internally create proxies we will know these properties ahead of time. For wrapped
     * proxies we will copy the properties off of the {@link Image}. For lazy proxies we
     * force the call sites to provide the required information ahead of time. At
     * instantiation time we verify that the assumed properties match the actual properties.
     *
     * @see ISurface#FLAG_BUDGETED
     * @see ISurface#FLAG_APPROX_FIT
     * @see ISurface#FLAG_SKIP_ALLOCATOR
     */
    int mSurfaceFlags;

    LazyInstantiateCallback mLazyInstantiateCallback;

    /**
     * Set from the backing resource for wrapped resources.
     */
    final UniqueID mUniqueID;

    boolean mIsPromiseProxy = false;

    /**
     * This tracks the mipmap status at the proxy level and is thus somewhat distinct from the
     * backing Texture's mipmap status. In particular, this status is used to determine when
     * mipmap levels need to be explicitly regenerated during the execution of a DAG of opsTasks.
     * <p>
     * Only meaningful if {@link #isUserMipmapped} returns true.
     */
    boolean mMipmapsDirty = true;

    /**
     * Should the target's unique key be synced with ours.
     */
    boolean mSyncTargetKey = true;

    IUniqueKey mUniqueKey;
    /**
     * Only set when 'mUniqueKey' is non-null.
     */
    ImageProxyCache mImageProxyCache;

    /**
     * Deferred version - no data
     */
    public ImageProxy(ImageDesc desc,
                      int surfaceFlags) {
        assert (desc.isValid());
        mDesc = desc;
        mSurfaceFlags = surfaceFlags;
        mUniqueID = new UniqueID();
        //assert (width > 0 && height > 0); // non-lazy
    }

    /**
     * Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
     */
    public ImageProxy(ImageDesc desc,
                      int surfaceFlags,
                      LazyInstantiateCallback callback) {
        assert (desc.isValid());
        mDesc = desc;
        mSurfaceFlags = surfaceFlags;
        mUniqueID = new UniqueID();
        mLazyInstantiateCallback = Objects.requireNonNull(callback);
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        /*assert (width < 0 && height < 0 && (surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) ||
                (width > 0 && height > 0);*/
    }

    /**
     * Wrapped version - shares the UniqueID of the passed image.
     * <p>
     * Takes UseAllocator because even though this is already instantiated it still can participate
     * in allocation by having its backing resource recycled to other uninstantiated proxies or
     * not depending on UseAllocator.
     */
    public ImageProxy(@SharedPtr Image image,
                      int surfaceFlags) {
        assert (image != null);
        mDesc = image.getDesc();
        mSurfaceFlags = image.getSurfaceFlags() | surfaceFlags;
        assert (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0;
        mUniqueID = image.getUniqueID(); // converting from unique resource ID to a surface ID
        mMipmapsDirty = image.isMipmapped() && image.isMipmapsDirty();
        assert (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0;
        assert (image.isMipmapped()) == ((mSurfaceFlags & ISurface.FLAG_MIPMAPPED) != 0);
        assert (image.getBudgetType() == Engine.BudgetType.Budgeted) == ((mSurfaceFlags & ISurface.FLAG_BUDGETED) != 0);
        assert (image.getBudgetType() == Engine.BudgetType.Budgeted) == isBudgeted();
        mImage = image; // std::move
        if (image.getUniqueKey() != null) {
            assert (image.getContext() != null);
            mImageProxyCache = image.getContext().getSurfaceProvider();
            mImageProxyCache.adoptUniqueKeyFromSurface(this, image);
        }
    }

    public static class LazyCallbackResult {

        @SharedPtr
        public Image mImage;
        /**
         * Some lazy callbacks want to set their own (or no key) on the {@link Image}
         * they return. Others want the {@link Image}'s key to be kept in sync with the surface's
         * key. This flag controls the key relationship between proxies and their targets.
         * <ul>
         *     <li>False: Don't key the {@link Image} with the surface's key. The lazy
         *     instantiation callback is free to return a {@link Image} that already
         *     has a unique key unrelated to the surface's key.</li>
         *     <li>True: Keep the {@link Image}'s unique key in sync with the surface's
         *     unique key. The {@link Image} returned from the lazy instantiation callback
         *     must not have a unique key or have the same same unique key as the surface.
         *     If the surface is later assigned a key it is in turn assigned to the
         *     {@link Image}.</li>
         * </ul>
         */
        public boolean mSyncTargetKey = true;
        /**
         * Should the callback be disposed of after it has returned or preserved until the surface
         * is freed. Only honored if 'mSurface' is not-null. If it is null the callback is preserved.
         */
        public boolean mReleaseCallback = true;

        public LazyCallbackResult() {
        }

        public LazyCallbackResult(@SharedPtr Image image) {
            mImage = image;
        }

        public LazyCallbackResult(@SharedPtr Image image,
                                  boolean syncTargetKey,
                                  boolean releaseCallback) {
            mImage = image;
            mSyncTargetKey = syncTargetKey;
            mReleaseCallback = releaseCallback;
        }
    }

    /**
     * Lazy-callback function.
     */
    @FunctionalInterface
    public interface LazyInstantiateCallback extends AutoCloseable {

        /**
         * Specifies the expected properties of the {@link GpuSurface} returned by a lazy instantiation
         * callback. The dimensions will be negative in the case of a lazy-most surface.
         */
        LazyCallbackResult onLazyInstantiate(ResourceProvider provider,
                                             ImageDesc desc,
                                             boolean budgeted,
                                             String label);

        @Override
        default void close() {
        }
    }

    @Nullable
    @SharedPtr
    public static ImageProxy make(ImageDesc desc, boolean budgeted) {
        if (!desc.isValid()) {
            return null;
        }
        return new ImageProxy(desc, budgeted ? ISurface.FLAG_BUDGETED : 0);
    }

    @Nullable
    @SharedPtr
    public static ImageProxy make(Context context,
                                  byte imageType,
                                  int colorType,
                                  int width, int height,
                                  int depthOrArraySize,
                                  int imageFlags) {
        var desc = context.getCaps().getDefaultColorImageDesc(imageType, colorType, width, height, depthOrArraySize,
                imageFlags);
        return make(desc, true);
    }

    @Override
    protected void deallocate() {
        // Due to the order of cleanup the Texture this proxy may have wrapped may have gone away
        // at this point. Zero out the pointer so the cache invalidation code doesn't try to use it.
        mImage = RefCnt.move(mImage);

        if (mLazyInstantiateCallback != null) {
            mLazyInstantiateCallback.close();
            mLazyInstantiateCallback = null;
        }

        // In DDL-mode, uniquely keyed proxies keep their key even after their originating
        // proxy provider has gone away. In that case there is no-one to send the invalid key
        // message to (Note: in this case we don't want to remove its cached resource).
        if (mUniqueKey != null && mImageProxyCache != null) {
            mImageProxyCache.processInvalidUniqueKey(mUniqueKey, this, false);
        } else {
            assert (mImageProxyCache == null);
        }
    }

    /**
     * Returns true if the surface has a lazy callback and not instantiated.
     */
    public boolean isLazy() {
        return mImage == null && mLazyInstantiateCallback != null;
    }

    /**
     * Returns true if the surface has a lazy callback, not instantiated,
     * loose fit and dimension is not known.
     */
    public final boolean isLazyMost() {
        /*boolean result = mWidth < 0;
        assert (result == (mHeight < 0));
        assert (!result || isLazy());
        return result;*/
        //TODO
        return false;
    }

    /**
     * Returns the logical width of this surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the desired width of the surface
     */
    public final int getWidth() {
        assert (!isLazyMost());
        return mDesc.getWidth();
    }

    /**
     * Returns the logical height of this surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the desired height of the surface
     */
    public final int getHeight() {
        assert (!isLazyMost());
        return mDesc.getHeight();
    }

    /**
     * Returns the physical width of the backing surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the width of the backing store
     */
    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mImage != null) {
            return mImage.getWidth();
        }
        /*if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            return ISurface.getApproxSize(mWidth);
        }*/
        //TODO
        return getWidth();
    }

    /**
     * Returns the physical height of the backing surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the height of the backing store
     */
    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mImage != null) {
            return mImage.getHeight();
        }
        /*if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            return ISurface.getApproxSize(mHeight);
        }*/
        //TODO
        return getHeight();
    }

    /**
     * If set to exact or approx size is equal to exact size. Must call when not lazy-most.
     * Equivalent to getWidth() == getBackingWidth() && getHeight() == getBackingHeight();
     *
     * @return true if backing fit is (as if) exact
     * @see #isUserExact()
     */
    public final boolean isExact() {
        assert (!isLazyMost());
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0) {
            // user-set Exact
            return true;
        }
        // equivalent to Exact
        //TODO
        /*return mWidth == ISurface.getApproxSize(mWidth) &&
                mHeight == ISurface.getApproxSize(mHeight);*/
        return true;
    }

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     * If this surface it non-renderable, this method always returns one.
     *
     * @return the number of samples, greater than (multisample) or equal to one
     */
    public int getSampleCount() {
        return mDesc.getSampleCount();
    }

    @Nonnull
    public ImageDesc getDesc() {
        return mDesc;
    }

    /**
     * The contract for the unique ID is:
     * <ul>
     * <li>For wrapped resources:
     * the unique ID will match that of the wrapped resource</li>
     * <li>For deferred resources:
     *  <ul>
     *  <li>The unique ID will be different from the real resource, when it is allocated</li>
     *  <li>The surface's unique ID will not change across the instantiates call</li>
     *  </ul>
     * </li>
     * <li> The unique IDs of the proxies and the resources draw from the same pool</li>
     * </ul>
     * What this boils down to is that the unique ID of a surface can be used to consistently
     * track/identify a surface but should never be used to distinguish between
     * resources and proxies - <b>beware!</b>
     *
     * @return a reference for identity hash map
     */
    public final UniqueID getUniqueID() {
        return mUniqueID;
    }

    public UniqueID getBackingUniqueID() {
        if (mImage != null) {
            return mImage.getUniqueID();
        }
        return mUniqueID;
    }

    /**
     * Returns true if the backing store is instantiated.
     */
    public boolean isInstantiated() {
        return mImage != null;
    }

    /**
     * Instantiates the backing store, if necessary.
     *
     * @param resourceProvider the resource provider to create textures
     * @return success or not
     */
    public boolean instantiate(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return false;
        }
        if (mImage != null) {
            assert mUniqueKey == null ||
                    mImage.getUniqueKey() != null && mImage.getUniqueKey().equals(mUniqueKey);
            return true;
        }

        assert ((mSurfaceFlags & ISurface.FLAG_MIPMAPPED) == 0) ||
                ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0);

        final Image image = resourceProvider.findOrCreateImage(mDesc,
                (mSurfaceFlags & ISurface.FLAG_BUDGETED) != 0, "");
        if (image == null) {
            return false;
        }

        // If there was an invalidation message pending for this key, we might have just processed it,
        // causing the key (stored on this proxy) to become invalid.
        if (mUniqueKey != null) {
            resourceProvider.assignUniqueKeyToResource(mUniqueKey, image);
        }

        assert mImage == null;
        mImage = image;

        return true;
    }

    /**
     * De-instantiate. Called after instantiated.
     */
    public void clear() {
        assert mImage != null;
        mImage.unref();
        mImage = null;
    }

    /**
     * Does the resource count against the resource budget?
     * <p>
     * Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     */
    public final boolean isBudgeted() {
        return (mSurfaceFlags & ISurface.FLAG_BUDGETED) != 0;
    }

    /**
     * The pixel values of this surface's texture cannot be modified (e.g. doesn't support write
     * pixels or MIP map level regen). Read-only proxies also bypass interval tracking and
     * assignment in ResourceAllocator.
     */
    public final boolean isReadOnly() {
        return (mSurfaceFlags & ISurface.FLAG_READ_ONLY) != 0;
    }

    public final boolean isProtected() {
        return (mSurfaceFlags & ISurface.FLAG_PROTECTED) != 0;
    }

    @ApiStatus.Internal
    public final boolean isUserExact() {
        return (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0;
    }

    /**
     * Retrieves the amount of GPU memory that will be or currently is used by this resource
     * in bytes. It is approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of GPU memory used in bytes
     */
    public long getMemorySize() {
        //TODO
        return 0;
    }

    /**
     * Proxies that are already instantiated and whose backing texture cannot be recycled to
     * instantiate other proxies do not need to be considered by {@link SurfaceAllocator}.
     */
    public final boolean shouldSkipAllocator() {
        if ((mSurfaceFlags & ISurface.FLAG_SKIP_ALLOCATOR) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        if (mImage == null) {
            return false;
        }
        // If this resource is already allocated and not recyclable then the resource allocator does
        // not need to do anything with it.
        return mImage.getScratchKey() == null;
    }

    /**
     * Return the texture proxy's unique key. It will be null if the proxy doesn't have one.
     */
    @Nullable
    public final IUniqueKey getUniqueKey() {
        return mUniqueKey;
    }

    /**
     * Returns whether the backing store references the wrapped object.
     * Always false if not instantiated.
     */
    public boolean isBackingWrapped() {
        return mImage != null && mImage.isWrapped();
    }

    @Nullable
    public Image getImage() {
        return mImage;
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
        if (mImage != null) {
            return mImage.asImage().isMipmapped();
        }
        return (mSurfaceFlags & ISurface.FLAG_MIPMAPPED) != 0;
    }

    public final boolean isMipmapsDirty() {
        return mMipmapsDirty && isUserMipmapped();
    }

    public final void setMipmapsDirty(boolean dirty) {
        assert isUserMipmapped();
        mMipmapsDirty = dirty;
    }

    /**
     * Returns the Mipmapped value of the proxy from creation time regardless of whether it has
     * been instantiated or not.
     */
    public final boolean isUserMipmapped() {
        return (mSurfaceFlags & ISurface.FLAG_MIPMAPPED) != 0;
    }

    /*@Nonnull
    @Override
    IScratchKey computeScratchKey() {
        int computeFlags = ((mSurfaceFlags & (ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED)) |
                (isMipmapped() ? ISurface.FLAG_MIPMAPPED : 0));
        return new Image.ScratchKey().compute(
                mFormat,
                getBackingWidth(),
                getBackingHeight(),
                1,
                computeFlags
        );
    }*/

    /*@ApiStatus.Internal
    public final void makeUserExact(boolean allocatedCaseOnly) {
        assert !isLazyMost();
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0) {
            return;
        }

        final Image texture = getGpuImage();
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
        mSurfaceFlags &= ~ISurface.FLAG_APPROX_FIT;
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
    }*/

    /*@Nullable
    @SharedPtr
    @Override
    Image createSurface(ResourceProvider resourceProvider) {
        assert ((mSurfaceFlags & ISurface.FLAG_MIPMAPPED) == 0 ||
                (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0);
        assert !isLazy();
        assert mImage == null;

        return resourceProvider.createTexture(mWidth, mHeight,
                mFormat,
                getSampleCount(),
                mSurfaceFlags,
                "");
    }*/

    public final boolean doLazyInstantiation(ResourceProvider resourceProvider) {
        assert isLazy();

        @SharedPtr
        Image image = null;
        if (mUniqueKey != null) {
            image = resourceProvider.findByUniqueKey(mUniqueKey);
        }

        boolean syncTargetKey = true;
        boolean releaseCallback = false;
        if (image == null) {
            int width = isLazyMost() ? -1 : getWidth();
            int height = isLazyMost() ? -1 : getHeight();
            LazyCallbackResult result = mLazyInstantiateCallback.onLazyInstantiate(resourceProvider,
                    mDesc,
                    (mSurfaceFlags & ISurface.FLAG_BUDGETED) != 0,
                    "");
            if (result != null) {
                image = (Image) result.mImage;
                syncTargetKey = result.mSyncTargetKey;
                releaseCallback = result.mReleaseCallback;
            }
        }
        if (image == null) {
            //mWidth = mHeight = 0;
            return false;
        }

        if (isLazyMost()) {
            // This was a lazy-most proxy. We need to fill in the width & height. For normal
            // lazy proxies we must preserve the original width & height since that indicates
            // the content area.
            /*mWidth = image.getWidth();
            mHeight = image.getHeight();*/
        }

        assert getWidth() <= image.getWidth();
        assert getHeight() <= image.getHeight();

        mSyncTargetKey = syncTargetKey;
        if (syncTargetKey) {
            if (mUniqueKey != null) {
                if (image.getUniqueKey() == null) {
                    // If 'texture' is newly created, attach the unique key
                    resourceProvider.assignUniqueKeyToResource(mUniqueKey, image);
                } else {
                    // otherwise we had better have reattached to a cached version
                    assert image.getUniqueKey().equals(mUniqueKey);
                }
            } else {
                assert image.getUniqueKey() == null;
            }
        }

        assert mImage == null;
        mImage = image;
        if (releaseCallback) {
            mLazyInstantiateCallback.close();
            mLazyInstantiateCallback = null;
        }

        return true;
    }

    @ApiStatus.Internal
    public void setIsPromiseProxy() {
        mIsPromiseProxy = true;
    }
}
