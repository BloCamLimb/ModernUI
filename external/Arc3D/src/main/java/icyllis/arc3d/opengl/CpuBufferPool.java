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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.CpuBuffer;
import icyllis.arc3d.engine.GpuBufferPool;

import javax.annotation.Nullable;

/**
 * A cache object that can be shared by multiple {@link GpuBufferPool} instances. It caches
 * cpu buffer allocations to avoid reallocating them.
 * <p>
 * <b>NOTE:</b> You must call {@link #releaseAll()} when this cache is no longer used.
 */
public class CpuBufferPool {

    private final CpuBuffer[] mBuffers;

    public CpuBufferPool(int maxCount) {
        mBuffers = new CpuBuffer[maxCount];
    }

    @Nullable
    @SharedPtr
    public CpuBuffer makeBuffer(long size) {
        assert (size > 0);
        CpuBuffer result = null;
        if (size <= GpuBufferPool.DEFAULT_BUFFER_SIZE) {
            int i = 0;
            for (; i < mBuffers.length && mBuffers[i] != null; ++i) {
                assert (mBuffers[i].size() == GpuBufferPool.DEFAULT_BUFFER_SIZE);
                if (mBuffers[i].unique()) {
                    result = mBuffers[i];
                }
            }
            if (result == null && i < mBuffers.length) {
                mBuffers[i] = result = CpuBuffer.make(GpuBufferPool.DEFAULT_BUFFER_SIZE);
            }
        }
        if (result == null) {
            return CpuBuffer.make(size);
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
