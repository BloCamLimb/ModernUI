/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.vulkan;

import org.lwjgl.vulkan.*;

// The BackendContext contains all the base Vulkan objects needed by the GrVkGpu. The assumption
// is that the client will set these up and pass them to the GrVkGpu constructor. The VkDevice
// created must support at least one graphics queue, which is passed in as well.
// The QueueFamilyIndex must match the family of the given queue. It is needed for CommandPool
// creation, and any GrBackendObjects handed to us (e.g., for wrapped textures) needs to be created
// in or transitioned to that family. The refs held by members of this struct must be released
// (either by deleting the struct or manually releasing the refs) before the underlying vulkan
// device and instance are destroyed.
public final class VkBackendContext {

    public VkInstance mInstance;
    public VkPhysicalDevice mPhysicalDevice;
    public VkDevice mDevice;
    public VkQueue mQueue;
    public int mGraphicsQueueIndex;
    public int mMaxAPIVersion;
    public VkPhysicalDeviceFeatures mDeviceFeatures;
    public VkPhysicalDeviceFeatures2 mDeviceFeatures2;
    public boolean mProtectedContext;
}
