/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.engine.Caps;
import icyllis.arc3d.engine.ContextOptions;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public abstract class VKCaps extends Caps {

    public VKCaps(ContextOptions options,
                  VkPhysicalDevice physDev,
                  int physicalDeviceVersion,
                  VkPhysicalDeviceFeatures2 deviceFeatures2,
                  VKCapabilitiesInstance capabilitiesInstance,
                  VKCapabilitiesDevice capabilitiesDevice) {
        super(options);

        ShaderCaps shaderCaps = mShaderCaps;
        shaderCaps.mTargetApi = TargetApi.VULKAN_1_0;
        shaderCaps.mGLSLVersion = GLSLVersion.GLSL_450;

        try (var stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
            VKCore.vkGetPhysicalDeviceProperties(physDev, properties);

            if (Integer.compareUnsigned(physicalDeviceVersion,
                    VKCore.VK_MAKE_VERSION(1, 3, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_6;
            } else if (Integer.compareUnsigned(physicalDeviceVersion,
                    VKCore.VK_MAKE_VERSION(1, 2, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_5;
            } else if (Integer.compareUnsigned(physicalDeviceVersion,
                    VKCore.VK_MAKE_VERSION(1, 1, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_3;
            } else {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_0;
            }
        }
    }
}
