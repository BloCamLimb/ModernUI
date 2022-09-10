/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

/**
 * Subclass of {@link TextureProxy} that also provides render target info.
 */
//TODO
public final class TextureRenderTargetProxy extends TextureProxy {

    private int mSampleCount;

    // Deferred version - no data
    TextureRenderTargetProxy(BackendFormat format,
                             int width, int height,
                             int sampleCount,
                             boolean mipmapped,
                             boolean backingFit,
                             boolean budgeted,
                             int surfaceFlags,
                             boolean useAllocator,
                             boolean deferredProvider) {
        super(format, width, height, mipmapped, backingFit, budgeted,
                surfaceFlags, useAllocator, deferredProvider);
    }

    // Lazy-callback version - takes a new UniqueID from the shared resource/proxy pool.
    TextureRenderTargetProxy(BackendFormat format,
                             int width, int height,
                             int sampleCount,
                             boolean mipmapped,
                             boolean backingFit,
                             boolean budgeted,
                             int surfaceFlags,
                             boolean useAllocator,
                             boolean deferredProvider,
                             LazyInstantiateCallback callback) {
        super(format, width, height, mipmapped, backingFit, budgeted,
                surfaceFlags, useAllocator, deferredProvider, callback);
    }
}
