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

import icyllis.arc3d.core.Rect2i;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;

/**
 * Subclass of {@link TextureDelegate} that also provides render target info.
 */
//TODO
@VisibleForTesting
public final class RenderTextureDelegate extends TextureDelegate {

    private final int mSampleCount;
    private final Rect2i mResolveRect = new Rect2i();

    // Deferred version - no data
    RenderTextureDelegate(BackendFormat format,
                          int width, int height,
                          int sampleCount,
                          int surfaceFlags) {
        super(format, width, height, surfaceFlags);
        mSampleCount = sampleCount;
    }

    // Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
    RenderTextureDelegate(BackendFormat format,
                          int width, int height,
                          int sampleCount,
                          int surfaceFlags,
                          LazyInstantiateCallback callback) {
        super(format, width, height, surfaceFlags, callback);
        mSampleCount = sampleCount;
    }

    @Override
    public int getSampleCount() {
        return mSampleCount;
    }

    @Nullable
    @Override
    public GpuRenderTarget getGpuRenderTarget() {
        return mGpuTexture != null ? mGpuTexture.asRenderTarget() : null;
    }

    @Override
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

    @Override
    public boolean needsResolve() {
        assert mResolveRect.isEmpty() || isManualMSAAResolve();
        return isManualMSAAResolve() && !mResolveRect.isEmpty();
    }

    @Override
    public Rect2i getResolveRect() {
        assert isManualMSAAResolve();
        return mResolveRect;
    }
}
