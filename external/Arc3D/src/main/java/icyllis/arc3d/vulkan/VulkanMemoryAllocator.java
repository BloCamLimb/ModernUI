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

import icyllis.arc3d.engine.Engine.BufferUsageFlags;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.*;
import org.lwjgl.util.vma.*;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.util.vma.Vma.*;

/**
 * <a href="https://github.com/GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator">AMD Vulkan Memory Allocator</a>
 */
public class VulkanMemoryAllocator implements AutoCloseable {

    /**
     * Force to use dedicated allocation.
     */
    public static final int
            kDedicatedAllocation_AllocFlag = 0x1;
    /**
     * Use persistent mapping.
     */
    public static final int
            kPersistentlyMapped_AllocFlag = 0x2;
    /**
     * Used for memoryless attachments on TBDR GPU.
     */
    public static final int
            kLazyAllocation_AllocFlag = 0x4;

    protected /*VmaAllocator*/ long mAllocator;

    public VulkanMemoryAllocator(long allocator) {
        mAllocator = allocator;
    }

    @Nullable
    public static VulkanMemoryAllocator make(VkInstance instance,
                                             VkPhysicalDevice physicalDevice,
                                             VkDevice device,
                                             int physicalDeviceVersion,
                                             @NativeType("VkDeviceSize") long largeHeapBlockSize) {
        // Vulkan 1.1 is required
        if (!(physicalDeviceVersion >= VK_API_VERSION_1_1)) {
            return null;
        }
        try (var stack = MemoryStack.stackPush()) {
            var pVulkanFunctions = VmaVulkanFunctions.calloc(stack)
                    .set(instance, device);
            // allow VMA to use dedicated allocation whenever it's preferred
            // a block size of 64MB is reasonable for mixed 2D and 3D applications
            var pCreateInfo = VmaAllocatorCreateInfo.calloc(stack)
                    .flags(VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT)
                    .physicalDevice(physicalDevice)
                    .device(device)
                    .preferredLargeHeapBlockSize(largeHeapBlockSize != 0 ? largeHeapBlockSize : 64 * 1024 * 1024)
                    .pVulkanFunctions(pVulkanFunctions)
                    .instance(instance)
                    .vulkanApiVersion(physicalDeviceVersion);
            var pAllocator = stack.pointers(VK_NULL_HANDLE);
            if (vmaCreateAllocator(pCreateInfo, pAllocator) != VK_SUCCESS) {
                return null;
            }
            return new VulkanMemoryAllocator(pAllocator.get(0));
        }
    }

    @Override
    public void close() {
        if (mAllocator != VK_NULL_HANDLE) {
            vmaDestroyAllocator(mAllocator);
        }
        mAllocator = VK_NULL_HANDLE;
    }

    public boolean allocateBufferMemory(VulkanDevice device,
                                        @NativeType("VkBuffer") long buffer,
                                        int usageFlags,
                                        int allocFlags,
                                        VulkanAllocation outAllocInfo) {
        try (var stack = MemoryStack.stackPush()) {
            int flags = 0;
            if ((allocFlags & kDedicatedAllocation_AllocFlag) != 0) {
                flags |= VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT;
            }
            if ((allocFlags & kPersistentlyMapped_AllocFlag) != 0) {
                assert (usageFlags & BufferUsageFlags.kHostVisible) != 0;
                flags |= VMA_ALLOCATION_CREATE_MAPPED_BIT;
            }

            int requiredFlags;
            int preferredFlags;
            if ((usageFlags & BufferUsageFlags.kUpload) != 0) {
                // upload heap
                assert (usageFlags & BufferUsageFlags.kAccessMask) == BufferUsageFlags.kHostVisible;
                requiredFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                        VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
                preferredFlags = 0;
            } else if ((usageFlags & BufferUsageFlags.kReadback) != 0) {
                // download heap
                assert (usageFlags & BufferUsageFlags.kAccessMask) == BufferUsageFlags.kHostVisible;
                requiredFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
                preferredFlags = VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
            } else if ((usageFlags & BufferUsageFlags.kAccessMask) == BufferUsageFlags.kDeviceLocal) {
                // GPU-only memory
                requiredFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
                if ((allocFlags & kLazyAllocation_AllocFlag) != 0) {
                    preferredFlags = VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
                } else {
                    preferredFlags = 0;
                }
            } else {
                // otherwise it's host visible, write-combined
                requiredFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                        VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
                if ((usageFlags & BufferUsageFlags.kDeviceLocal) != 0) {
                    preferredFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
                } else {
                    preferredFlags = 0;
                }
            }

            var pCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                    .flags(flags)
                    .usage(VMA_MEMORY_USAGE_UNKNOWN)
                    .requiredFlags(requiredFlags)
                    .preferredFlags(preferredFlags)
                    .memoryTypeBits(0)
                    .pool(VK_NULL_HANDLE)
                    .pUserData(MemoryUtil.NULL);
            var pAllocation = stack.pointers(VK_NULL_HANDLE);
            var pAllocationInfo = VmaAllocationInfo.malloc(stack);
            var result = vmaAllocateMemoryForBuffer(
                    mAllocator, buffer, pCreateInfo, pAllocation, pAllocationInfo
            );
            if (device.checkResult(result)) {
                if (result == VK_SUCCESS) {
                    getAllocInfo(stack, pAllocation.get(0), pAllocationInfo, outAllocInfo);
                    return true;
                }
                vmaFreeMemory(mAllocator, pAllocation.get(0));
            }
            return false;
        }
    }

    public boolean allocateImageMemory(VulkanDevice device,
                                       @NativeType("VkImage") long image,
                                       int allocFlags,
                                       VulkanAllocation outAllocInfo) {
        try (var stack = MemoryStack.stackPush()) {
            int flags = 0;
            if ((allocFlags & kDedicatedAllocation_AllocFlag) != 0) {
                flags |= VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT;
            }

            int requiredFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
            int preferredFlags;
            if ((allocFlags & kLazyAllocation_AllocFlag) != 0) {
                preferredFlags = VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
            } else {
                preferredFlags = 0;
            }

            var pCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                    .flags(flags)
                    .usage(VMA_MEMORY_USAGE_UNKNOWN)
                    .requiredFlags(requiredFlags)
                    .preferredFlags(preferredFlags)
                    .memoryTypeBits(0)
                    .pool(VK_NULL_HANDLE)
                    .pUserData(MemoryUtil.NULL);
            var pAllocation = stack.pointers(VK_NULL_HANDLE);
            var pAllocationInfo = VmaAllocationInfo.malloc(stack);
            var result = vmaAllocateMemoryForImage(
                    mAllocator, image, pCreateInfo, pAllocation, pAllocationInfo
            );
            if (device.checkResult(result)) {
                if (result == VK_SUCCESS) {
                    getAllocInfo(stack, pAllocation.get(0), pAllocationInfo, outAllocInfo);
                    return true;
                }
                vmaFreeMemory(mAllocator, pAllocation.get(0));
            }
            return false;
        }
    }

    public void freeMemory(VulkanAllocation allocInfo) {
        assert allocInfo.mAllocation != VK_NULL_HANDLE;
        vmaFreeMemory(mAllocator, allocInfo.mAllocation);
    }

    // populate allocation info
    private void getAllocInfo(MemoryStack stack,
                              @NativeType("VmaAllocation") long allocation,
                              VmaAllocationInfo allocationInfo,
                              VulkanAllocation outAllocInfo) {
        final int memFlags;
        {
            var pMemFlags = stack.mallocInt(1);
            vmaGetMemoryTypeProperties(
                    mAllocator, allocationInfo.memoryType(), pMemFlags
            );
            memFlags = pMemFlags.get(0);
        }

        int flags = 0;
        if ((memFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
            flags |= VulkanAllocation.kHostVisible_Flag;
        }
        if ((memFlags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0) {
            flags |= VulkanAllocation.kHostCoherent_Flag;
        }
        if ((memFlags & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) != 0) {
            flags |= VulkanAllocation.kLazilyAllocated_Flag;
        }

        outAllocInfo.mMemory = allocationInfo.deviceMemory();
        outAllocInfo.mOffset = allocationInfo.offset();
        outAllocInfo.mSize = allocationInfo.size();
        outAllocInfo.mMappedPointer = allocationInfo.pMappedData();
        outAllocInfo.mMemoryFlags = flags;
        outAllocInfo.mAllocation = allocation;
    }
}
