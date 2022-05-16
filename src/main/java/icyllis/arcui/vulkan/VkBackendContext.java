/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.vulkan;

import org.lwjgl.vulkan.*;

/**
 * The BackendContext contains all the base Vulkan objects needed by the VkServer. The assumption
 * is that the client will set these up and pass them to the VkServer constructor. The VkDevice
 * created must support at least one graphics queue, which is passed in as well.
 * The QueueFamilyIndex must match the family of the given queue. It is needed for CommandPool
 * creation, and any BackendObjects handed to us (e.g., for wrapped textures) needs to be created
 * in or transitioned to that family. The refs held by members of this struct must be released
 * (either by deleting the struct or manually releasing the refs) before the underlying Vulkan
 * device and instance are destroyed.
 */
public final class VkBackendContext {

    public VkInstance mInstance;
    public VkPhysicalDevice mPhysicalDevice;
    public VkDevice mDevice;
    public VkQueue mQueue;
    public int mGraphicsQueueIndex;
    // The max api version set here should match the value set in VkApplicationInfo::apiVersion when
    // then VkInstance was created.
    public int mMaxAPIVersion;
    // The client can create their VkDevice with either a VkPhysicalDeviceFeatures or
    // VkPhysicalDeviceFeatures2 struct, thus we have to support taking both. The
    // VkPhysicalDeviceFeatures2 struct is needed, so we know if the client enabled any extension
    // specific features. If mDeviceFeatures2 is not null then we ignore mDeviceFeatures. If both
    // mDeviceFeatures and mDeviceFeatures2 are null we will assume no features are enabled.
    public VkPhysicalDeviceFeatures mDeviceFeatures;
    public VkPhysicalDeviceFeatures2 mDeviceFeatures2;
    // Indicates that we are working with protected content and all CommandPool and Queue operations
    // should be done in a protected context.
    public boolean mProtectedContext;
}
