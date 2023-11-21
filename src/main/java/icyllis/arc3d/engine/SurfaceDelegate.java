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

/**
 * The {@link SurfaceDelegate} targets a {@link IGpuSurface} with three instantiation
 * methods: deferred, lazy-callback and wrapped.
 * <p>
 * Target: The backing GPU texture or render target that referenced by this surface.
 * <p>
 * Instantiate: Create new GPU surfaces or find surfaces in {@link ResourceCache}
 * when they are actually required on flush.
 * <p>
 * BackingFit: Indicates whether a backing store needs to be an exact match or
 * can be larger than is strictly necessary. True: Exact; False: Approx. See
 * {@link #FLAG_APPROX_FIT}, the default is exact.
 * <p>
 * UseAllocator:
 * <ul>
 *     <li>False: This surface will be instantiated outside the allocator (e.g.
 *     for surfaces that are instantiated in on-flush callbacks).</li>
 *     <li>True: {@link SurfaceAllocator} should instantiate this surface.</li>
 * </ul>
 * <p>
 * Use {@link SurfaceProvider} to obtain {@link SurfaceDelegate} objects.
 * <p>
 * Note: the object itself is also used as the scratch key, see {@link #hashCode()}
 * and {@link #equals(Object)}
 */
public abstract class SurfaceDelegate extends RefCnt implements ISurface {

    /**
     * For wrapped resources, 'mFormat' and 'mDimensions' will always be filled in from the
     * wrapped resource.
     */
    final BackendFormat mFormat;
    int mWidth;
    int mHeight;

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
     * DeferredProvider: For TextureProxies created in a deferred list recording thread it is
     * possible for the uniqueKey to be cleared on the backing Texture while the uniqueKey
     * remains on the surface. A 'mDeferredProvider' of 'true' loosens up asserts that the key of an
     * instantiated uniquely-keyed texture is also always set on the backing {@link GpuTexture}.
     * <p>
     * In many cases these flags aren't actually known until the surface has been instantiated.
     * However, Engine frequently needs to change its behavior based on these settings. For
     * internally create proxies we will know these properties ahead of time. For wrapped
     * proxies we will copy the properties off of the {@link GpuTexture}. For lazy proxies we
     * force the call sites to provide the required information ahead of time. At
     * instantiation time we verify that the assumed properties match the actual properties.
     *
     * @see IGpuSurface#FLAG_BUDGETED
     * @see IGpuSurface#FLAG_APPROX_FIT
     * @see IGpuSurface#FLAG_SKIP_ALLOCATOR
     */
    int mSurfaceFlags;

    LazyInstantiateCallback mLazyInstantiateCallback;

    /**
     * Set from the backing resource for wrapped resources.
     */
    final Object mUniqueID;

    int mTaskTargetCount = 0;
    boolean mIsDeferredListTarget = false;

    // Deferred version and lazy-callback version
    SurfaceDelegate(BackendFormat format,
                    int width, int height,
                    int surfaceFlags) {
        assert (format != null);
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mSurfaceFlags = surfaceFlags;
        if (format.isExternal()) {
            mSurfaceFlags |= IGpuSurface.FLAG_READ_ONLY;
        }
        mUniqueID = this;
    }

    // Wrapped version
    SurfaceDelegate(@SharedPtr IGpuSurface surface,
                    int surfaceFlags) {
        assert (surface != null);
        mFormat = surface.getBackendFormat();
        mWidth = surface.getWidth();
        mHeight = surface.getHeight();
        mSurfaceFlags = surface.getSurfaceFlags() | surfaceFlags;
        assert (mSurfaceFlags & IGpuSurface.FLAG_APPROX_FIT) == 0;
        mUniqueID = surface; // converting from unique resource ID to a surface ID
    }

    public static class LazyCallbackResult {

        @SharedPtr
        public IGpuSurface mSurface;
        /**
         * Some lazy callbacks want to set their own (or no key) on the {@link GpuTexture}
         * they return. Others want the {@link GpuTexture}'s key to be kept in sync with the surface's
         * key. This flag controls the key relationship between proxies and their targets.
         * <ul>
         *     <li>False: Don't key the {@link GpuTexture} with the surface's key. The lazy
         *     instantiation callback is free to return a {@link GpuTexture} that already
         *     has a unique key unrelated to the surface's key.</li>
         *     <li>True: Keep the {@link GpuTexture}'s unique key in sync with the surface's
         *     unique key. The {@link GpuTexture} returned from the lazy instantiation callback
         *     must not have a unique key or have the same same unique key as the surface.
         *     If the surface is later assigned a key it is in turn assigned to the
         *     {@link GpuTexture}.</li>
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

        public LazyCallbackResult(@SharedPtr IGpuSurface surface) {
            mSurface = surface;
        }

        public LazyCallbackResult(@SharedPtr IGpuSurface surface,
                                  boolean syncTargetKey,
                                  boolean releaseCallback) {
            mSurface = surface;
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
         * Specifies the expected properties of the {@link IGpuSurface} returned by a lazy instantiation
         * callback. The dimensions will be negative in the case of a lazy-most surface.
         */
        LazyCallbackResult onLazyInstantiate(ResourceProvider provider,
                                             BackendFormat format,
                                             int width, int height,
                                             int sampleCount,
                                             int surfaceFlags,
                                             String label);

        @Override
        default void close() {
        }
    }

    /**
     * Returns true if the surface has a lazy callback and not instantiated.
     */
    public abstract boolean isLazy();

    /**
     * Returns true if the surface has a lazy callback, not instantiated,
     * loose fit and dimension is not known.
     */
    public final boolean isLazyMost() {
        boolean result = mWidth < 0;
        assert (result == (mHeight < 0));
        assert (!result || isLazy());
        return result;
    }

    /**
     * Returns the logical width of this surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the desired width of the surface
     */
    public final int getWidth() {
        assert (!isLazyMost());
        return mWidth;
    }

    /**
     * Returns the logical height of this surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the desired height of the surface
     */
    public final int getHeight() {
        assert (!isLazyMost());
        return mHeight;
    }

    /**
     * Returns the physical width of the backing surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the width of the backing store
     */
    public abstract int getBackingWidth();

    /**
     * Returns the physical height of the backing surface.
     * The result is undefined if {@link #isLazyMost()} returns true.
     *
     * @return the height of the backing store
     */
    public abstract int getBackingHeight();

    /**
     * If set to exact or approx size is equal to exact size. Must call when not lazy-most.
     * Equivalent to getWidth() == getBackingWidth() && getHeight() == getBackingHeight();
     *
     * @return true if backing fit is (as if) exact
     * @see #isUserExact()
     */
    public final boolean isExact() {
        assert (!isLazyMost());
        if ((mSurfaceFlags & IGpuSurface.FLAG_APPROX_FIT) == 0) {
            // user-set Exact
            return true;
        }
        // equivalent to Exact
        return mWidth == ResourceProvider.makeApprox(mWidth) &&
                mHeight == ResourceProvider.makeApprox(mHeight);
    }

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     * If this surface it non-renderable, this method always returns one.
     *
     * @return the number of samples, greater than (multisample) or equal to one
     */
    public abstract int getSampleCount();

    /**
     * @return the backend format of the surface
     */
    @Nonnull
    public final BackendFormat getBackendFormat() {
        return mFormat;
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
    public final Object getUniqueID() {
        return mUniqueID;
    }

    public abstract Object getBackingUniqueID();

    /**
     * Returns true if the backing store is instantiated.
     */
    public abstract boolean isInstantiated();

    /**
     * Instantiates the backing store, if necessary.
     *
     * @param resourceProvider the resource provider to create textures
     * @return success or not
     */
    public abstract boolean instantiate(ResourceProvider resourceProvider);

    /**
     * De-instantiate. Called after instantiated.
     */
    public abstract void clear();

    /**
     * Proxies that are already instantiated and whose backing texture cannot be recycled to
     * instantiate other proxies do not need to be considered by {@link SurfaceAllocator}.
     */
    public abstract boolean shouldSkipAllocator();

    /**
     * Returns whether the backing store references the wrapped object.
     * Always false if not instantiated.
     */
    public abstract boolean isBackingWrapped();

    /**
     * Called when this task becomes a target of a {@link RenderTask}.
     */
    public final void isUsedAsTaskTarget() {
        mTaskTargetCount++;
    }

    /**
     * How many render tasks has this surface been the target of?
     */
    public final int getTaskTargetCount() {
        return mTaskTargetCount;
    }

    @Nullable
    public abstract IGpuSurface getGpuSurface();

    /**
     * If this is a texturable surface and the surface is already instantiated, return its
     * backing {@link GpuTexture}; if not, return null.
     */
    @Nullable
    public GpuTexture getGpuTexture() {
        return null;
    }

    /**
     * If this is a renderable surface and the surface is already instantiated, return its
     * backing {@link GpuRenderTarget}; if not, return null.
     */
    @Nullable
    public GpuRenderTarget getGpuRenderTarget() {
        return null;
    }

    /**
     * Does the resource count against the resource budget?
     * <p>
     * Always true for lazy-callback resources;
     * set from the backing resource for wrapped resources;
     * only meaningful if 'mLazyInstantiateCallback' is non-null.
     */
    public final boolean isBudgeted() {
        return (mSurfaceFlags & IGpuSurface.FLAG_BUDGETED) != 0;
    }

    /**
     * The pixel values of this surface's texture cannot be modified (e.g. doesn't support write
     * pixels or MIP map level regen). Read-only proxies also bypass interval tracking and
     * assignment in ResourceAllocator.
     */
    public final boolean isReadOnly() {
        return (mSurfaceFlags & IGpuSurface.FLAG_READ_ONLY) != 0;
    }

    public final boolean isProtected() {
        return (mSurfaceFlags & IGpuSurface.FLAG_PROTECTED) != 0;
    }

    public final boolean isManualMSAAResolve() {
        return (mSurfaceFlags & IGpuSurface.FLAG_MANUAL_MSAA_RESOLVE) != 0;
    }

    public final boolean wrapsGLDefaultFB() {
        return (mSurfaceFlags & IGpuSurface.FLAG_GL_WRAP_DEFAULT_FB) != 0;
    }

    public final boolean wrapsVkSecondaryCB() {
        return (mSurfaceFlags & IGpuSurface.FLAG_VK_WRAP_SECONDARY_CB) != 0;
    }

    public final boolean isDeferredListTarget() {
        return mIsDeferredListTarget;
    }

    @ApiStatus.Internal
    public void setIsDeferredListTarget() {
        mIsDeferredListTarget = true;
    }

    @ApiStatus.Internal
    public final boolean isUserExact() {
        return (mSurfaceFlags & IGpuSurface.FLAG_APPROX_FIT) == 0;
    }

    public TextureDelegate asTexture() {
        return null;
    }

    /**
     * Retrieves the amount of GPU memory that will be or currently is used by this resource
     * in bytes. It is approximate since we aren't aware of additional padding or copies made
     * by the driver.
     *
     * @return the amount of GPU memory used in bytes
     */
    public long getMemorySize() {
        return 0;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @ApiStatus.Internal
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean doLazyInstantiation(ResourceProvider resourceProvider);
}
