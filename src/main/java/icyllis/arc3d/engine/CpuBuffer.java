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
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;

/**
 * A client-side buffer represents an immutable block of native CPU memory.
 * <p>
 * This is only used as "staging buffers" in OpenGL and may not be used for other purposes.
 */
public final class CpuBuffer extends RefCnt {

    private final long mSize;
    private final long mData;

    private CpuBuffer(long size, long data) {
        mSize = size;
        mData = data;
    }

    @Nullable
    @SharedPtr
    public static CpuBuffer make(long size) {
        assert (size > 0);
        long data = MemoryUtil.nmemAlloc(size);
        if (data == MemoryUtil.NULL) {
            return null;
        }
        // je_malloc is 16-byte aligned on 64-bit system,
        // it's safe to use Unsafe to transfer primitive data
        assert MathUtil.isAlign8(data);
        return new CpuBuffer(size, data);
    }

    /**
     * Size of the buffer in bytes.
     */
    public long size() {
        return mSize;
    }

    public long data() {
        return mData;
    }

    @Override
    protected void deallocate() {
        MemoryUtil.nmemFree(mData);
    }
}
