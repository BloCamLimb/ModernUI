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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import javax.annotation.Nullable;

import static icyllis.arc3d.vulkan.VKCore.*;
import static org.lwjgl.util.vma.Vma.*;

/**
 * AMD vulkan memory.
 */
public class VulkanMemoryAllocator implements AutoCloseable {

    private /*VmaAllocator*/ long mAllocator;

    private VulkanMemoryAllocator(long allocator) {
        mAllocator = allocator;
    }

    @Nullable
    public static VulkanMemoryAllocator make(VkInstance instance,
                                             VkPhysicalDevice physicalDevice,
                                             VkDevice device,
                                             int physicalDeviceVersion) {
        try (var stack = MemoryStack.stackPush()) {
            var pVulkanFunctions = VmaVulkanFunctions.calloc(stack)
                    .set(instance, device);
            var pCreateInfo = VmaAllocatorCreateInfo.calloc(stack)
                    .physicalDevice(physicalDevice)
                    .device(device)
                    .pVulkanFunctions(pVulkanFunctions)
                    .instance(instance);
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

    public int allocateMemoryForBuffer(long buffer,
                                       VulkanAllocation allocation) {
        return -1;
    }
}
