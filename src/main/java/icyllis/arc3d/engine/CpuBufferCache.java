/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

/**
 * A cache object that can be shared by multiple {@link BufferAllocPool} instances. It caches
 * cpu buffer allocations to avoid reallocating them.
 * <p>
 * <b>NOTE:</b> You must call {@link #releaseAll()} when this cache is no longer used.
 */
public class CpuBufferCache {

    private final CpuBuffer[] mBuffers;

    public CpuBufferCache(int cacheSize) {
        mBuffers = new CpuBuffer[cacheSize];
    }

    @SharedPtr
    public CpuBuffer makeBuffer(int size) {
        assert (size > 0);
        CpuBuffer result = null;
        if (size <= BufferAllocPool.DEFAULT_BUFFER_SIZE) {
            int i = 0;
            for (; i < mBuffers.length && mBuffers[i] != null; ++i) {
                assert (mBuffers[i].size() == BufferAllocPool.DEFAULT_BUFFER_SIZE);
                if (mBuffers[i].unique()) {
                    result = mBuffers[i];
                }
            }
            if (result == null && i < mBuffers.length) {
                mBuffers[i] = result = new CpuBuffer(BufferAllocPool.DEFAULT_BUFFER_SIZE);
            }
        }
        if (result == null) {
            return new CpuBuffer(size);
        }
        return RefCnt.create(result);
    }

    public void releaseAll() {
        for (int i = 0; i < mBuffers.length && mBuffers[i] != null; ++i) {
            mBuffers[i].unref();
            mBuffers[i] = null;
        }
    }
}
