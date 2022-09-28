/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.RefCnt;
import icyllis.akashigi.core.SharedPtr;

import javax.annotation.Nullable;

import static icyllis.akashigi.engine.Engine.*;

/**
 * Lazy-callback or wrapped a render target (no texture access).
 */
//TODO
public final class RenderTargetProxy extends SurfaceProxy {

    private RenderTarget mTarget;
    private int mSampleCount;

    RenderTargetProxy(BackendFormat format, int width, int height, int surfaceFlags) {
        super(format, width, height, surfaceFlags);
    }

    @Override
    protected void dispose() {
    }

    @Override
    public boolean isLazy() {
        return mTarget == null && mLazyInstantiateCallback != null;
    }

    @Override
    public int getBackingWidth() {
        assert (!isLazyMost());
        if (mTarget != null) {
            return mTarget.getWidth();
        }
        if ((mSurfaceFlags & SurfaceFlag_LooseFit) != 0) {
            return ResourceProvider.makeApprox(mWidth);
        }
        return mWidth;
    }

    @Override
    public int getBackingHeight() {
        assert (!isLazyMost());
        if (mTarget != null) {
            return mTarget.getHeight();
        }
        if ((mSurfaceFlags & SurfaceFlag_LooseFit) != 0) {
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
        if (mTarget != null) {
            return mTarget;
        }
        return mUniqueID;
    }

    @Override
    public boolean isInstantiated() {
        return mTarget != null;
    }

    @Override
    public boolean instantiate(ResourceProvider resourceProvider) {
        if (isLazy()) {
            return false;
        }
        return mTarget != null;
    }

    @Override
    public void clear() {
        assert mTarget != null;
        mTarget.unref();
        mTarget = null;
    }

    @Override
    public boolean shouldSkipAllocator() {
        if ((mSurfaceFlags & SurfaceFlag_SkipAllocator) != 0) {
            // Usually an atlas or onFlush proxy
            return true;
        }
        return mTarget != null;
    }

    @Nullable
    @Override
    public RenderTarget peekRenderTarget() {
        return mTarget;
    }

    @Override
    boolean doLazyInstantiation(ResourceProvider resourceProvider) {
        assert isLazy();

        @SharedPtr
        RenderTarget target = null;

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
            target = (RenderTarget) result.mSurface;
            releaseCallback = result.mReleaseCallback;
        }
        if (target == null) {
            mWidth = mHeight = 0;
            return false;
        }
        assert target.getTexture() == null;

        if (isLazyMost()) {
            // This was a lazy-most proxy. We need to fill in the width & height. For normal
            // lazy proxies we must preserve the original width & height since that indicates
            // the content area.
            mWidth = target.getWidth();
            mHeight = target.getHeight();
        }

        assert getWidth() <= target.getWidth();
        assert getHeight() <= target.getHeight();

        mTarget = RefCnt.move(mTarget, target);
        if (releaseCallback) {
            mLazyInstantiateCallback = null;
        }

        return true;
    }
}
