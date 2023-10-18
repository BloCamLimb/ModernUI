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

import icyllis.arc3d.core.SharedPtr;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;

/**
 * Lazy-callback or wrapped a render target (no texture access).
 */
//TODO
@VisibleForTesting
public final class RenderTarget extends Surface {

    @SharedPtr
    private GpuRenderTarget mRenderTarget;
    private int mSampleCount;

    RenderTarget(BackendFormat format, int width, int height, int surfaceFlags) {
        super(format, width, height, surfaceFlags);
        assert hashCode() == System.identityHashCode(this);
    }

    RenderTarget(GpuRenderTarget renderTarget, int surfaceFlags) {
        super(renderTarget, surfaceFlags);
        mRenderTarget = renderTarget;
        mSampleCount = renderTarget.getSampleCount();
    }

    @Override
    protected void deallocate() {
        mRenderTarget = move(mRenderTarget);
    }

    @Override
    public boolean isLazy() {
        return mRenderTarget == null && mLazyInstantiateCallback != null;
    }

    @Override
    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mRenderTarget != null) {
            return mRenderTarget.getWidth();
        }
        if ((mSurfaceFlags & IGpuSurface.FLAG_APPROX_FIT) != 0) {
            return ResourceProvider.makeApprox(mWidth);
        }
        return mWidth;
    }

    @Override
    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mRenderTarget != null) {
            return mRenderTarget.getHeight();
        }
        if ((mSurfaceFlags & IGpuSurface.FLAG_APPROX_FIT) != 0) {
            return ResourceProvider.makeApprox(mHeight);
        }
        return mHeight;
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    @Override
    public Object getBackingUniqueID() {
        if (mRenderTarget != null) {
            return mRenderTarget;
        }
        return mUniqueID;
    }

    @Override
    public boolean isInstantiated() {
        return mRenderTarget != null;
    }

    @Override
    public boolean instantiate(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return false;
        }
        return mRenderTarget != null;
    }

    @Override
    public void clear() {
        assert mRenderTarget != null;
        mRenderTarget.unref();
        mRenderTarget = null;
    }

    @Override
    public boolean shouldSkipAllocator() {
        if ((mSurfaceFlags & IGpuSurface.FLAG_SKIP_ALLOCATOR) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        return mRenderTarget != null;
    }

    @Override
    public boolean isBackingWrapped() {
        return mRenderTarget != null;
    }

    @Nullable
    @Override
    public IGpuSurface getGpuSurface() {
        return mRenderTarget;
    }

    @Nullable
    @Override
    public GpuRenderTarget getGpuRenderTarget() {
        return mRenderTarget != null ? mRenderTarget.asRenderTarget() : null;
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

        mRenderTarget = move(mRenderTarget, surface);
        if (releaseCallback) {
            mLazyInstantiateCallback = null;
        }

        return true;
    }
}
