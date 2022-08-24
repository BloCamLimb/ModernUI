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

package icyllis.arctic.engine;

import icyllis.arctic.core.SharedPtr;

/**
 * A cache object that can be shared by multiple {@link BufferAllocPool} instances. It caches
 * cpu buffer allocations to avoid reallocating them.
 * <p>
 * <b>NOTE:</b> You must call {@link #releaseAll()} when this cache is no longer used.
 */
public class CpuBufferCache implements AutoCloseable {

    private final CpuBuffer[] mBuffers;

    public CpuBufferCache(int maxBuffersToCache) {
        mBuffers = new CpuBuffer[maxBuffersToCache];
    }

    @Override
    public void close() {
        releaseAll();
    }

    @SharedPtr
    public CpuBuffer makeBuffer(int size) {
        assert (size > 0);
        CpuBuffer result = null;
        if (size == BufferAllocPool.DEFAULT_BUFFER_SIZE) {
            int i = 0;
            for (; i < mBuffers.length && mBuffers[i] != null; ++i) {
                assert (mBuffers[i].size() == BufferAllocPool.DEFAULT_BUFFER_SIZE);
                if (mBuffers[i].unique()) {
                    result = mBuffers[i];
                }
            }
            if (result == null && i < mBuffers.length) {
                mBuffers[i] = result = new CpuBuffer(size);
            }
        }
        if (result == null) {
            return new CpuBuffer(size);
        }
        result.ref();
        return result;
    }

    public void releaseAll() {
        for (int i = 0; i < mBuffers.length && mBuffers[i] != null; ++i) {
            mBuffers[i].unref();
            mBuffers[i] = null;
        }
    }
}
