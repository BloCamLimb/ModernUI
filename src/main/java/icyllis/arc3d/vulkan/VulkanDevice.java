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

import icyllis.arc3d.engine.*;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.vulkan.VK11.*;

public abstract class VulkanDevice extends Device {

    private VkPhysicalDevice mPhysicalDevice;
    private VkDevice mDevice;
    private VulkanMemoryAllocator mMemoryAllocator;
    private boolean mProtectedContext;
    private int mQueueIndex;

    public VulkanDevice(ContextOptions options, VulkanCaps caps,
                        VulkanBackendContext backendContext,
                        VulkanMemoryAllocator memoryAllocator) {
        super(Engine.BackendApi.kVulkan, options, caps);
        mMemoryAllocator = memoryAllocator;
        mPhysicalDevice = backendContext.mPhysicalDevice;
        mDevice = backendContext.mDevice;
        mQueueIndex = backendContext.mGraphicsQueueIndex;
    }

    public VkDevice vkDevice() {
        return mDevice;
    }

    public VkPhysicalDevice vkPhysicalDevice() {
        return mPhysicalDevice;
    }

    public int getQueueIndex() {
        return mQueueIndex;
    }

    public VulkanMemoryAllocator getMemoryAllocator() {
        return mMemoryAllocator;
    }

    public boolean checkResult(int result) {
        if (result == VK_SUCCESS || result > 0) {
            return true;
        }
        switch (result) {
            case VK_ERROR_DEVICE_LOST -> mDeviceIsLost = true;
            case VK_ERROR_OUT_OF_DEVICE_MEMORY,
                 VK_ERROR_OUT_OF_HOST_MEMORY -> mOutOfMemoryEncountered = true;
        }
        return false;
    }

    public boolean isProtectedContext() {
        return mProtectedContext;
    }
}
