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

import icyllis.arc3d.core.*;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Deferred, lazy-callback or wrapped a render target.
 */
@Deprecated
@VisibleForTesting
public final class RenderTargetProxy extends SurfaceProxy {

    private int mSampleCount;
    private final Rect2i mResolveRect = new Rect2i();

    // for deferred single color target
    // if MSAA, this is the resolve target
    //TODO instantiate this
    private ImageViewProxy mColorImageProxy;

    // Deferred version - no data
    // single color target
    public RenderTargetProxy(BackendFormat format,
                      int width, int height,
                      int sampleCount,
                      int surfaceFlags) {
        super(format, width, height, surfaceFlags);
        assert (width > 0 && height > 0); // non-lazy
        mSampleCount = sampleCount;
        /*mColorImageProxy = new ImageProxy(format,
                width, height,
                surfaceFlags);*/
        //FIXME
    }

    // Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
    public RenderTargetProxy(BackendFormat format,
                      int width, int height,
                      int sampleCount,
                      int surfaceFlags,
                      LazyInstantiateCallback callback) {
        super(format, width, height, surfaceFlags);
        mSampleCount = sampleCount;
        mLazyInstantiateCallback = Objects.requireNonNull(callback);
        // A "fully" lazy proxy's width and height are not known until instantiation time.
        // So fully lazy proxies are created with width and height < 0. Regular lazy proxies must be
        // created with positive widths and heights. The width and height are set to 0 only after a
        // failed instantiation. The former must be "approximate" fit while the latter can be either.
        assert (width < 0 && height < 0 && (surfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) ||
                (width > 0 && height > 0);
    }

    // Wrapped
    public RenderTargetProxy(GpuRenderTarget renderTarget, int surfaceFlags) {
        super(renderTarget, surfaceFlags);
        mGpuSurface = renderTarget;
        mSampleCount = renderTarget.getSampleCount();
    }

    @Override
    protected void deallocate() {
        mGpuSurface = RefCnt.move(mGpuSurface);
        mColorImageProxy = RefCnt.move(mColorImageProxy);
    }

    @Override
    public boolean isLazy() {
        return mGpuSurface == null && mLazyInstantiateCallback != null;
    }

    @Override
    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mGpuSurface != null) {
            return mGpuSurface.getWidth();
        }
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            return ISurface.getApproxSize(mWidth);
        }
        return mWidth;
    }

    @Override
    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mGpuSurface != null) {
            return mGpuSurface.getHeight();
        }
        if ((mSurfaceFlags & ISurface.FLAG_APPROX_FIT) != 0) {
            return ISurface.getApproxSize(mHeight);
        }
        return mHeight;
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    public void setResolveRect(int left, int top, int right, int bottom) {
        assert isManualMSAAResolve();
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            mResolveRect.setEmpty();
        } else {
            assert right > left && bottom > top;
            assert left >= 0 && right <= getBackingWidth() && top >= 0 && bottom <= getBackingHeight();
            mResolveRect.join(left, top, right, bottom);
        }
    }

    public boolean needsResolve() {
        assert mResolveRect.isEmpty() || isManualMSAAResolve();
        return isManualMSAAResolve() && !mResolveRect.isEmpty();
    }

    public Rect2ic getResolveRect() {
        assert isManualMSAAResolve();
        return mResolveRect;
    }

    @Override
    public UniqueID getBackingUniqueID() {
        if (mGpuSurface != null) {
            return mGpuSurface.getUniqueID();
        }
        return mUniqueID;
    }

    @Override
    public boolean isInstantiated() {
        return mGpuSurface != null;
    }

    @Override
    public boolean instantiate(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return false;
        }
        return mGpuSurface != null;
    }

    @Override
    public void clear() {
        assert mGpuSurface != null;
        mGpuSurface.unref();
        mGpuSurface = null;
    }

    @Override
    public boolean shouldSkipAllocator() {
        if ((mSurfaceFlags & ISurface.FLAG_SKIP_ALLOCATOR) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        return mGpuSurface != null;
    }

    @Override
    public boolean isBackingWrapped() {
        return mGpuSurface != null;
    }

    @Nullable
    @Override
    public GpuSurface getGpuSurface() {
        return mGpuSurface;
    }

    @Nullable
    @Override
    public Image getGpuImage() {
        return mGpuSurface.asImage();
    }

    @Nullable
    @Override
    public GpuRenderTarget getGpuRenderTarget() {
        return (GpuRenderTarget) mGpuSurface;
    }

    @Override
    public ImageViewProxy asImageProxy() {
        return mColorImageProxy;
    }

    @Override
    public RenderTargetProxy asRenderTargetProxy() {
        return this;
    }

    @Override
    public boolean doLazyInstantiation(ResourceProvider resourceProvider) {
        assert isLazy();

        @SharedPtr
        GpuRenderTarget surface = null;

        boolean releaseCallback = false;
        int width = isLazyMost() ? -1 : getWidth();
        int height = isLazyMost() ? -1 : getHeight();
        LazyCallbackResult result = mLazyInstantiateCallback.onLazyInstantiate(resourceProvider,
                mFormat,
                width, height,
                getSampleCount(),
                mSurfaceFlags,
                "");
        if (result != null) {
            surface = (GpuRenderTarget) result.mSurface;
            releaseCallback = result.mReleaseCallback;
        }
        if (surface == null) {
            mWidth = mHeight = 0;
            return false;
        }

        if (isLazyMost()) {
            // This was a lazy-most proxy. We need to fill in the width & height. For normal
            // lazy proxies we must preserve the original width & height since that indicates
            // the content area.
            mWidth = surface.getWidth();
            mHeight = surface.getHeight();
        }

        assert getWidth() <= surface.getWidth();
        assert getHeight() <= surface.getHeight();

        mGpuSurface = RefCnt.move(mGpuSurface, surface);
        if (releaseCallback) {
            mLazyInstantiateCallback = null;
        }

        return true;
    }

    @NonNull
    @Override
    IResourceKey computeScratchKey() {
        assert mColorImageProxy != null;
        //TODO check flags
        /*return new GpuRenderTarget.ResourceKey().compute(
                getBackingWidth(), getBackingHeight(),
                mFormat,
                mColorImageProxy.mSurfaceFlags,
                mFormat,
                mColorImageProxy.mSurfaceFlags,
                null, 0,
                mSampleCount,
                mSurfaceFlags
        );*/
        return null;
    }

    @Nullable
    @SharedPtr
    @Override
    GpuSurface createSurface(ResourceProvider resourceProvider) {
        assert mColorImageProxy != null;
        //TODO check flags
        /*return resourceProvider.createRenderTarget(
                mWidth, mHeight,
                mFormat,
                mColorImageProxy.mSurfaceFlags,
                mFormat,
                mColorImageProxy.mSurfaceFlags,
                null, 0,
                mSampleCount,
                mSurfaceFlags,
                ""
        );*/
        return null;
    }
}
