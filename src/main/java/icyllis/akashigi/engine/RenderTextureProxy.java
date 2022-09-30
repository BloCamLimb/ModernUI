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

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Subclass of {@link TextureProxy} that also provides render target info.
 */
//TODO
@VisibleForTesting
public final class RenderTextureProxy extends TextureProxy {

    private final int mSampleCount;

    // Deferred version - no data
    RenderTextureProxy(BackendFormat format,
                       int width, int height,
                       int sampleCount,
                       int surfaceFlags) {
        super(format, width, height, surfaceFlags);
        mSampleCount = sampleCount;
    }

    // Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
    RenderTextureProxy(BackendFormat format,
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
}
