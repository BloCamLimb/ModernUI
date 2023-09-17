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
import org.lwjgl.system.MemoryUtil;

/**
 * Represents an immutable block of native CPU memory.
 * <p>
 * The instances are atomic reference counted, and may be used as shared pointers.
 */
public final class CpuBuffer extends RefCnt {

    private final int mSize;
    private final long mData;

    public CpuBuffer(int size) {
        assert (size > 0);
        mSize = size;
        mData = MemoryUtil.nmemAllocChecked(size);
    }

    /**
     * Size of the buffer in bytes.
     */
    public int size() {
        return mSize;
    }

    public long data() {
        return mData;
    }

    @Override
    protected void dispose() {
        MemoryUtil.nmemFree(mData);
    }
}
