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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The {@link ImageViewProxy} implements the proxy pattern for {@link Image},
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
 * @see ImageProxyCache
 */
public final class ImageViewProxy extends RefCnt {

    ImageDesc mDesc;

    /**
     * For deferred textures it will be null until the backing store is instantiated.
     * For wrapped textures it will point to the wrapped resource.
     */
    @SharedPtr
    Image mImage;
    int mOrigin;
    short mSwizzle;

    String mLabel;

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
     * DeferredProvider: For {@link ImageViewProxy}s created in a deferred list recording thread it is
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
    boolean mBudgeted;
    boolean mVolatile;
    boolean mLazyDimensions;

    LazyInstantiateCallback mLazyInstantiateCallback;

    /**
     * Deferred version - no data
     */
    ImageViewProxy(ImageDesc desc,
                   int origin, short swizzle,
                   boolean budgeted,
                   String label) {
        mDesc = Objects.requireNonNull(desc);
        mOrigin = origin;
        mSwizzle = swizzle;
        mLabel = label;
        mBudgeted = budgeted;
        mVolatile = false;
        mLazyDimensions = false;
    }

    /**
     * Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
     */
    ImageViewProxy(ImageDesc desc,
                   int origin, short swizzle,
                   boolean budgeted,
                   boolean isVolatile,
                   boolean lazyDimensions,
                   LazyInstantiateCallback callback) {
        mDesc = Objects.requireNonNull(desc);
        mOrigin = origin;
        mSwizzle = swizzle;
        mLabel = "";
        mBudgeted = budgeted;
        mVolatile = isVolatile;
        mLazyDimensions = lazyDimensions;
        mLazyInstantiateCallback = Objects.requireNonNull(callback);
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        /*assert (width < 0 && height < 0 && (surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) ||
                (width > 0 && height > 0);*/
    }

    /**
     * Wrapped version.
     */
    ImageViewProxy(@SharedPtr Image image, int origin, short swizzle) {
        assert (image != null);
        mDesc = image.getDesc();
        mOrigin = origin;
        mSwizzle = swizzle;
        mLabel = image.getLabel();
        mBudgeted = image.isBudgeted();
        mVolatile = false;
        mLazyDimensions = false;
        mImage = image; // std::move
    }

    public static class LazyCallbackResult {

        @SharedPtr
        public Image mImage;
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
                                  boolean releaseCallback) {
            mImage = image;
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
    public static ImageViewProxy make(@NonNull Context context,
                                      @Nullable ImageDesc desc,
                                      int origin, short swizzle,
                                      boolean budgeted,
                                      @Nullable String label) {
        if (desc == null) {
            return null;
        }
        var proxy = new ImageViewProxy(desc, origin, swizzle, budgeted, label);
        if (!budgeted) {
            // Instantiate immediately to avoid races later on if the client starts to use the wrapping
            // object on multiple threads.
            if (!proxy.instantiate(context.getResourceProvider())) {
                proxy.unref();
                return null;
            }
        }
        return proxy;
    }

    @Nullable
    @SharedPtr
    public static ImageViewProxy make(@NonNull Context context,
                                      int imageType,
                                      int colorType,
                                      int width, int height,
                                      int depthOrArraySize,
                                      int imageFlags,
                                      int origin, short swizzle) {
        var desc = context.getCaps().getDefaultColorImageDesc(imageType, colorType, width, height, depthOrArraySize,
                imageFlags);
        if (desc == null) {
            return null;
        }
        return make(context, desc, origin, swizzle, true, "");
    }

    @SharedPtr
    public static ImageViewProxy makeLazy(@NonNull ImageDesc desc,
                                          int origin, short swizzle,
                                          boolean budgeted,
                                          boolean isVolatile,
                                          boolean lazyDimensions,
                                          @NonNull LazyInstantiateCallback callback) {
        return new ImageViewProxy(desc, origin, swizzle, budgeted, isVolatile, lazyDimensions, callback);
    }

    @SharedPtr
    public static ImageViewProxy wrap(@SharedPtr Image image,
                                      int origin, short swizzle) {
        return new ImageViewProxy(image, origin, swizzle);
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
    }

    /**
     * Returns true if the image proxy has a lazy callback.
     */
    public boolean isLazy() {
        return mLazyInstantiateCallback != null;
    }

    /**
     * Returns true if the image proxy has a lazy callback, loose fit and dimension is not known.
     */
    public boolean isLazyMost() {
        boolean result = mLazyDimensions;
        assert (!result || isLazy());
        return result;
    }

    /**
     * Returns the logical width of this surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the desired width of the surface
     */
    public int getWidth() {
        assert (!isLazyMost() || isInstantiated());
        return isInstantiated() ? mImage.getWidth() : mDesc.getWidth();
    }

    /**
     * Returns the logical height of this surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the desired height of the surface
     */
    public int getHeight() {
        assert (!isLazyMost() || isInstantiated());
        return isInstantiated() ? mImage.getHeight() : mDesc.getHeight();
    }

    /**
     * @see Engine.SurfaceOrigin
     */
    public int getOrigin() {
        return mOrigin;
    }

    /**
     * @see Swizzle
     */
    public short getSwizzle() {
        return mSwizzle;
    }

    public void concatSwizzle(short swizzle) {
        mSwizzle = Swizzle.concat(mSwizzle, swizzle);
    }

    /**
     * Replace the view's swizzle.
     */
    public void setSwizzle(short swizzle) {
        mSwizzle = swizzle;
    }

    /**
     * For Promise Images - should the Promise Image be fulfilled every time a Recording that references
     * it is inserted into the Context. True: fulfilled on every insertion call, otherwise only fulfilled once.
     */
    public boolean isVolatile() {
        assert !mVolatile || mLazyInstantiateCallback != null;
        return mVolatile;
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

    @NonNull
    public ImageDesc getDesc() {
        return mDesc;
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
            return true;
        }

        @SharedPtr final Image image = resourceProvider.findOrCreateImage(mDesc,
                mBudgeted, mLabel);
        if (image == null) {
            return false;
        }

        assert mImage == null;
        mImage = image;

        return true;
    }

    public boolean instantiateIfNonLazy(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return true;
        }
        return instantiate(resourceProvider);
    }

    public boolean doLazyInstantiation(ResourceProvider resourceProvider) {
        assert isLazy();
        if (mImage != null) {
            return true;
        }

        @SharedPtr
        Image image = null;
        boolean releaseCallback = false;
        LazyCallbackResult result = mLazyInstantiateCallback.onLazyInstantiate(resourceProvider,
                mDesc,
                mBudgeted,
                mLabel);
        if (result != null) {
            image = result.mImage;
            releaseCallback = result.mReleaseCallback;
        }
        if (image == null) {
            return false;
        }

        assert mImage == null;
        mImage = image;
        if (releaseCallback) {
            mLazyInstantiateCallback.close();
            mLazyInstantiateCallback = null;
        }

        return true;
    }

    /**
     * De-instantiate. Called after instantiated.
     */
    public void clear() {
        assert mVolatile && mLazyInstantiateCallback != null;
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
    public boolean isBudgeted() {
        return mBudgeted;
    }

    public boolean isProtected() {
        return mDesc.isProtected();
    }

    /**
     * Retrieves the amount of GPU memory that will be or currently is used by this resource
     * in bytes. It is approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of GPU memory used in bytes
     */
    public long getMemorySize() {
        return DataUtils.computeSize(mDesc);
    }

    /**
     * Returns whether the backing store references the wrapped object.
     * Always false if not instantiated.
     */
    public boolean isBackingWrapped() {
        return mImage != null && mImage.isWrapped();
    }

    public Image getImage() {
        return mImage;
    }

    @SharedPtr
    public Image refImage() {
        return RefCnt.create(mImage);
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
            return mImage.isMipmapped();
        }
        return mDesc.isMipmapped();
    }

    public String getLabel() {
        return mLabel;
    }

    @Override
    public String toString() {
        return "ImageViewProxy{" +
                "mDesc=" + mDesc +
                ", mImage=" + mImage +
                ", mOrigin=" + mOrigin +
                ", mSwizzle=" + Swizzle.toString(mSwizzle) +
                ", mLabel='" + mLabel + '\'' +
                ", mBudgeted=" + mBudgeted +
                ", mVolatile=" + mVolatile +
                ", mLazyDimensions=" + mLazyDimensions +
                ", mLazyInstantiateCallback=" + mLazyInstantiateCallback +
                '}';
    }
}
