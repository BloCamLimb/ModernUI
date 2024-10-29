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

package icyllis.arc3d.vulkan;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

/**
 * Holds Vulkan memory allocation information.
 */
public class VulkanAllocation {

    /**
     * Memory flags.
     */
    public static final int
            kHostVisible_Flag = 0x1,            // memory is host visible (mappable)
            kHostCoherent_Flag = 0x2,           // memory is host coherent (flushed to device after mapping)
            kLazilyAllocated_Flag = 0x4;        // memory is created with lazy allocation

    // device memory block
    public /*VkDeviceMemory*/ long mMemory = VK_NULL_HANDLE;
    public /*VkDeviceSize*/ long mOffset = 0;
    public /*VkDeviceSize*/ long mSize = 0;
    public long mMappedPointer = MemoryUtil.NULL;   // pointer to persistently mapped data
    public int mMemoryFlags = 0;                    // property flags for memory allocation
    public long mAllocation = VK_NULL_HANDLE;       // handle to memory allocated via VulkanMemoryAllocator

    public void set(VulkanAllocation alloc) {
        mMemory = alloc.mMemory;
        mOffset = alloc.mOffset;
        mSize = alloc.mSize;
        mMappedPointer = alloc.mMappedPointer;
        mMemoryFlags = alloc.mMemoryFlags;
        mAllocation = alloc.mAllocation;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(mMemory);
        result = 31 * result + Long.hashCode(mOffset);
        result = 31 * result + Long.hashCode(mSize);
        result = 31 * result + mMemoryFlags;
        return result;
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
    public String toString() {
        return "VulkanAllocation{" +
                "mMemory=0x" + Long.toHexString(mMemory) +
                ", mOffset=" + mOffset +
                ", mSize=" + mSize +
                ", mMappedPointer=0x" + Long.toHexString(mMappedPointer) +
                ", mMemoryFlags=0x" + Integer.toHexString(mMemoryFlags) +
                ", mAllocation=0x" + Long.toHexString(mAllocation) +
                '}';
    }
}
