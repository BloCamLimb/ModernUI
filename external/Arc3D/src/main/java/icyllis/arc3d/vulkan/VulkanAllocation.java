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

package icyllis.arc3d.vulkan;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

/**
 * Holds Vulkan memory allocation information.
 */
public class VulkanAllocation {

    /**
     * Memory flags.
     */
    public static final int
            VISIBLE_FLAG = 0x1,             // memory is host visible (mappable)
            COHERENT_FLAG = 0x2,            // memory is host coherent (flushed to device after mapping)
            LAZILY_ALLOCATED_FLAG = 0x4;    // memory is created with lazy allocation

    // device memory block
    public long mMemory = VK_NULL_HANDLE;       // can be VK_NULL_HANDLE if is an RT and is borrowed
    public long mOffset = 0;
    public long mSize = 0;                      // can be indeterminate if texture uses borrow semantics
    public int mMemoryFlags = 0;                // property flags for memory allocation
    public long mAllocation = VK_NULL_HANDLE;   // handle to memory allocated via VulkanMemoryAllocator

    public void set(VulkanAllocation alloc) {
        mMemory = alloc.mMemory;
        mOffset = alloc.mOffset;
        mSize = alloc.mSize;
        mMemoryFlags = alloc.mMemoryFlags;
        mAllocation = alloc.mAllocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VulkanAllocation vkAlloc = (VulkanAllocation) o;
        if (mMemory != vkAlloc.mMemory) return false;
        if (mOffset != vkAlloc.mOffset) return false;
        if (mSize != vkAlloc.mSize) return false;
        return mMemoryFlags == vkAlloc.mMemoryFlags;
    }

    @Override
    public int hashCode() {
        int result = (int) (mMemory ^ (mMemory >>> 32));
        result = 31 * result + (int) (mOffset ^ (mOffset >>> 32));
        result = 31 * result + (int) (mSize ^ (mSize >>> 32));
        result = 31 * result + mMemoryFlags;
        return result;
    }
}
