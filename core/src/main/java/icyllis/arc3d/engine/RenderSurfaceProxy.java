/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
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
public final class RenderSurfaceProxy extends SurfaceProxy {

    @SharedPtr
    private RenderSurface mSurface;
    private int mSampleCount;

    RenderSurfaceProxy(BackendFormat format, int width, int height, int surfaceFlags) {
        super(format, width, height, surfaceFlags);
        assert hashCode() == System.identityHashCode(this);
    }

    @Override
    protected void deallocate() {
        mSurface = move(mSurface);
    }

    @Override
    public boolean isLazy() {
        return mSurface == null && mLazyInstantiateCallback != null;
    }

    @Override
    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mSurface != null) {
            return mSurface.getWidth();
        }
        if ((mSurfaceFlags & Surface.FLAG_APPROX_FIT) != 0) {
            return ResourceProvider.makeApprox(mWidth);
        }
        return mWidth;
    }

    @Override
    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mSurface != null) {
            return mSurface.getHeight();
        }
        if ((mSurfaceFlags & Surface.FLAG_APPROX_FIT) != 0) {
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
        if (mSurface != null) {
            return mSurface;
        }
        return mUniqueID;
    }

    @Override
    public boolean isInstantiated() {
        return mSurface != null;
    }

    @Override
    public boolean instantiate(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return false;
        }
        return mSurface != null;
    }

    @Override
    public void clear() {
        assert mSurface != null;
        mSurface.unref();
        mSurface = null;
    }

    @Override
    public boolean shouldSkipAllocator() {
        if ((mSurfaceFlags & Surface.FLAG_SKIP_ALLOCATOR) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        return mSurface != null;
    }

    @Override
    public boolean isBackingWrapped() {
        return mSurface != null;
    }

    @Nullable
    @Override
    public Surface peekSurface() {
        return mSurface;
    }

    @Nullable
    @Override
    public RenderTarget peekRenderTarget() {
        return mSurface != null ? mSurface.getRenderTarget() : null;
    }

    @Override
    public boolean doLazyInstantiation(ResourceProvider resourceProvider) {
        assert isLazy();

        @SharedPtr
        RenderSurface surface = null;

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
            surface = (RenderSurface) result.mSurface;
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

        mSurface = move(mSurface, surface);
        if (releaseCallback) {
            mLazyInstantiateCallback = null;
        }

        return true;
    }
}
