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

import icyllis.arc3d.core.*;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * The {@link TextureProxy} targets an actual {@link GpuImageBase} with three instantiation
 * methods: deferred, lazy-callback and wrapped. Multiple {@link TextureProxy} objects
 * may target the same {@link GpuImageBase} based on dependencies and actual usage.
 * See {@link SurfaceProxy} for more info.
 * <p>
 * Use {@link SurfaceProvider} to obtain {@link TextureProxy} objects.
 * <p>
 * This class can only be used on the creating thread of/on a single {@link RecordingContext},
 * and later used by {@link DirectContext} (render thread).
 * <p>
 * Note: the object itself is also used as the scratch key, see {@link #hashCode()}
 * and {@link #equals(Object)}
 */
public class TextureProxy extends SurfaceProxy implements IScratchKey {

    boolean mIsPromiseProxy = false;

    /**
     * For deferred textures it will be null until the backing store is instantiated.
     * For wrapped textures it will point to the wrapped resource.
     */
    @SharedPtr
    GpuTexture mGpuTexture;

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
    SurfaceProvider mSurfaceProvider;

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
        mLazyInstantiateCallback = Objects.requireNonNull(callback);
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        assert (width < 0 && height < 0 && (surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) ||
                (width > 0 && height > 0);
    }

    /**
     * Wrapped version - shares the UniqueID of the passed texture.
     * <p>
     * Takes UseAllocator because even though this is already instantiated it still can participate
     * in allocation by having its backing resource recycled to other uninstantiated proxies or
     * not depending on UseAllocator.
     */
    public TextureProxy(@SharedPtr GpuTexture texture,
                        int surfaceFlags) {
        super(texture, surfaceFlags);
        mMipmapsDirty = texture.isMipmapped() && texture.isMipmapsDirty();
        assert (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0;
        assert (mFormat.isExternal() == texture.isExternal());
        assert (texture.isMipmapped()) == ((mSurfaceFlags & ISurface.FLAG_MIPMAPPED) != 0);
        assert (texture.getBudgetType() == Engine.BudgetType.Budgeted) == ((mSurfaceFlags & ISurface.FLAG_BUDGETED) != 0);
        assert (!texture.isExternal()) || ((mSurfaceFlags & ISurface.FLAG_READ_ONLY) != 0);
        assert (texture.getBudgetType() == Engine.BudgetType.Budgeted) == isBudgeted();
        assert (!texture.isExternal() || isReadOnly());
        mGpuTexture = texture; // std::move
        if (texture.getUniqueKey() != null) {
            assert (texture.getContext() != null);
            mSurfaceProvider = texture.getContext().getSurfaceProvider();
            mSurfaceProvider.adoptUniqueKeyFromSurface(this, texture);
        }
    }

    @Override
    protected void deallocate() {
        // Due to the order of cleanup the Texture this proxy may have wrapped may have gone away
        // at this point. Zero out the pointer so the cache invalidation code doesn't try to use it.
        mGpuTexture = RefCnt.move(mGpuTexture);

        if (mLazyInstantiateCallback != null) {
            mLazyInstantiateCallback.close();
            mLazyInstantiateCallback = null;
        }

        // In DDL-mode, uniquely keyed proxies keep their key even after their originating
        // proxy provider has gone away. In that case there is no-one to send the invalid key
        // message to (Note: in this case we don't want to remove its cached resource).
        if (mUniqueKey != null && mSurfaceProvider != null) {
            mSurfaceProvider.processInvalidUniqueKey(mUniqueKey, this, false);
        } else {
            assert (mSurfaceProvider == null);
        }
    }

    @Override
    public boolean isLazy() {
        return mGpuTexture == null && mLazyInstantiateCallback != null;
    }

    @Override
    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mGpuTexture != null) {
            return mGpuTexture.getWidth();
        }
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            return ISurface.getApproxSize(mWidth);
        }
        return mWidth;
    }

    @Override
    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mGpuTexture != null) {
            return mGpuTexture.getHeight();
        }
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            return ISurface.getApproxSize(mHeight);
        }
        return mHeight;
    }

    @Override
    public int getSampleCount() {
        return 1;
    }

    @Override
    public Object getBackingUniqueID() {
        if (mGpuTexture != null) {
            return mGpuTexture;
        }
        return mUniqueID;
    }

    @Override
    public boolean isInstantiated() {
        return mGpuTexture != null;
    }

    @Override
    public boolean instantiate(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return false;
        }
        if (mGpuTexture != null) {
            assert mUniqueKey == null ||
                    mGpuTexture.getUniqueKey() != null && mGpuTexture.getUniqueKey().equals(mUniqueKey);
            return true;
        }

        assert ((mSurfaceFlags & ISurface.FLAG_MIPMAPPED) == 0) ||
                ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0);

        final GpuTexture texture = resourceProvider.createTexture(mWidth, mHeight, mFormat,
                getSampleCount(), mSurfaceFlags, "");
        if (texture == null) {
            return false;
        }

        // If there was an invalidation message pending for this key, we might have just processed it,
        // causing the key (stored on this proxy) to become invalid.
        if (mUniqueKey != null) {
            resourceProvider.assignUniqueKeyToResource(mUniqueKey, texture);
        }

        assert mGpuTexture == null;
        assert texture.getBackendFormat().equals(mFormat);
        mGpuTexture = texture;

        return true;
    }

    @Override
    public void clear() {
        assert mGpuTexture != null;
        mGpuTexture.unref();
        mGpuTexture = null;
    }

    @Override
    public final boolean shouldSkipAllocator() {
        if ((mSurfaceFlags & ISurface.FLAG_SKIP_ALLOCATOR) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        if (mGpuTexture == null) {
            return false;
        }
        // If this resource is already allocated and not recyclable then the resource allocator does
        // not need to do anything with it.
        return mGpuTexture.getScratchKey() == null;
    }

    /**
     * Return the texture proxy's unique key. It will be null if the proxy doesn't have one.
     */
    @Nullable
    public final IUniqueKey getUniqueKey() {
        return mUniqueKey;
    }

    public void setResolveRect(int left, int top, int right, int bottom) {
        throw new UnsupportedOperationException();
    }

    public boolean needsResolve() {
        throw new UnsupportedOperationException();
    }

    public Rect2ic getResolveRect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBackingWrapped() {
        return mGpuTexture != null && mGpuTexture.isWrapped();
    }

    @Nullable
    @Override
    public GpuSurface getGpuSurface() {
        return mGpuTexture;
    }

    @Nullable
    @Override
    public GpuTexture getGpuTexture() {
        return mGpuTexture;
    }

    @Override
    public long getMemorySize() {
        // use user params
        return GpuImageBase.computeSize(mFormat, mWidth, mHeight, getSampleCount(),
                (mSurfaceFlags & ISurface.FLAG_MIPMAPPED) != 0,
                (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0);
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
        if (mGpuTexture != null) {
            return mGpuTexture.isMipmapped();
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

    /**
     * If true then the texture does not support MIP maps and only supports clamp wrap mode.
     */
    public final boolean hasRestrictedSampling() {
        return mFormat.isExternal();
    }

    /**
     * Same as {@link GpuImageBase.ScratchKey} for {@link SurfaceAllocator}.
     */
    @Override
    public int hashCode() {
        int result = getBackingWidth();
        result = 31 * result + getBackingHeight();
        result = 31 * result + mFormat.getFormatKey();
        result = 31 * result + ((mSurfaceFlags & (ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED)) |
                (isMipmapped() ? ISurface.FLAG_MIPMAPPED : 0));
        return result;
    }

    /**
     * Same as {@link GpuImageBase.ScratchKey} for {@link SurfaceAllocator}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GpuImageBase.ScratchKey key) {
            // ResourceProvider
            return key.mWidth == getBackingWidth() &&
                    key.mHeight == getBackingHeight() &&
                    key.mFormat == mFormat.getFormatKey() &&
                    key.mFlags == ((mSurfaceFlags & (ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED)) |
                            (isMipmapped() ? ISurface.FLAG_MIPMAPPED : 0));
        } else if (o instanceof TextureProxy proxy) {
            // ResourceAllocator
            return proxy.getBackingWidth() == getBackingWidth() &&
                    proxy.getBackingHeight() == getBackingHeight() &&
                    proxy.mFormat.getFormatKey() == mFormat.getFormatKey() &&
                    proxy.isMipmapped() == isMipmapped() &&
                    (proxy.mSurfaceFlags & (ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED)) ==
                            (mSurfaceFlags & (ISurface.FLAG_RENDERABLE | ISurface.FLAG_PROTECTED));
        }
        return false;
    }

    @Override
    public TextureProxy asTexture() {
        return this;
    }

    @ApiStatus.Internal
    public final void makeUserExact(boolean allocatedCaseOnly) {
        assert !isLazyMost();
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0) {
            return;
        }

        final GpuTexture texture = getGpuTexture();
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
    }

    @SharedPtr
    GpuTexture createGpuTexture(ResourceProvider resourceProvider) {
        assert ((mSurfaceFlags & ISurface.FLAG_MIPMAPPED) == 0 ||
                (mSurfaceFlags & ISurface.FLAG_APPROX_FIT) == 0);
        assert !isLazy();
        assert mGpuTexture == null;

        return resourceProvider.createTexture(mWidth, mHeight,
                mFormat,
                getSampleCount(),
                mSurfaceFlags,
                "");
    }

    @Override
    public final boolean doLazyInstantiation(ResourceProvider resourceProvider) {
        assert isLazy();

        @SharedPtr
        GpuTexture textureResource = null;
        if (mUniqueKey != null) {
            textureResource = resourceProvider.findByUniqueKey(mUniqueKey);
        }

        boolean syncTargetKey = true;
        boolean releaseCallback = false;
        if (textureResource == null) {
            int width = isLazyMost() ? -1 : getWidth();
            int height = isLazyMost() ? -1 : getHeight();
            LazyCallbackResult result = mLazyInstantiateCallback.onLazyInstantiate(resourceProvider,
                    mFormat,
                    width, height,
                    getSampleCount(),
                    mSurfaceFlags,
                    "");
            if (result != null) {
                textureResource = (GpuTexture) result.mSurface;
                syncTargetKey = result.mSyncTargetKey;
                releaseCallback = result.mReleaseCallback;
            }
        }
        if (textureResource == null) {
            mWidth = mHeight = 0;
            return false;
        }

        if (isLazyMost()) {
            // This was a lazy-most proxy. We need to fill in the width & height. For normal
            // lazy proxies we must preserve the original width & height since that indicates
            // the content area.
            mWidth = textureResource.getWidth();
            mHeight = textureResource.getHeight();
        }

        assert getWidth() <= textureResource.getWidth();
        assert getHeight() <= textureResource.getHeight();

        mSyncTargetKey = syncTargetKey;
        if (syncTargetKey) {
            if (mUniqueKey != null) {
                if (textureResource.getUniqueKey() == null) {
                    // If 'texture' is newly created, attach the unique key
                    resourceProvider.assignUniqueKeyToResource(mUniqueKey, textureResource);
                } else {
                    // otherwise we had better have reattached to a cached version
                    assert textureResource.getUniqueKey().equals(mUniqueKey);
                }
            } else {
                assert textureResource.getUniqueKey() == null;
            }
        }

        assert mGpuTexture == null;
        mGpuTexture = textureResource;
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
