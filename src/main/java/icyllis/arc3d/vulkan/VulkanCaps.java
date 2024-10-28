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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.ShaderCaps;
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.engine.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK11.*;

public abstract class VulkanCaps extends Caps {

    public VulkanCaps(ContextOptions options,
                      VkPhysicalDevice physDev,
                      int physicalDeviceVersion,
                      VkPhysicalDeviceFeatures2 deviceFeatures2,
                      VKCapabilitiesInstance capabilitiesInstance,
                      VKCapabilitiesDevice capabilitiesDevice) {
        super(options);

        mDepthClipNegativeOneToOne = false;

        ShaderCaps shaderCaps = mShaderCaps;
        shaderCaps.mTargetApi = TargetApi.VULKAN_1_0;
        shaderCaps.mGLSLVersion = GLSLVersion.GLSL_450;

        try (var stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
            vkGetPhysicalDeviceProperties(physDev, properties);
            VkPhysicalDeviceLimits limits = properties.limits();

            if (Integer.compareUnsigned(physicalDeviceVersion,
                    VK_MAKE_VERSION(1, 3, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_6;
            } else if (Integer.compareUnsigned(physicalDeviceVersion,
                    VK_MAKE_VERSION(1, 2, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_5;
            } else if (Integer.compareUnsigned(physicalDeviceVersion,
                    VK_MAKE_VERSION(1, 1, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_3;
            } else {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_0;
            }

            mMaxTextureSize = (int) Math.min(
                    Integer.toUnsignedLong(limits.maxImageDimension2D()), Integer.MAX_VALUE);
        }
    }

    public boolean hasUnifiedMemory() {
        return false;
    }

    static class ColorTypeInfo {

        int mColorType = ColorInfo.CT_UNKNOWN;

        static final int
                UPLOAD_DATA_FLAG = 0x1,
                RENDERABLE_FLAG = 0x2;
        int mFlags = 0;

        short mReadSwizzle = Swizzle.RGBA;
        short mWriteSwizzle = Swizzle.RGBA;

        @Override
        public String toString() {
            return "ColorTypeInfo{" +
                    "colorType=" + ColorInfo.colorTypeToString(mColorType) +
                    ", flags=0x" + Integer.toHexString(mFlags) +
                    ", readSwizzle=" + Swizzle.toString(mReadSwizzle) +
                    ", writeSwizzle=" + Swizzle.toString(mWriteSwizzle) +
                    '}';
        }
    }

    static class FormatInfo {

        /*VkFormatFeatureFlags*/ int mOptimalTilingFeatures = 0;
        /*VkFormatFeatureFlags*/ int mLinearTilingFeatures = 0;

        int[] mColorSampleCounts = {};

        ColorTypeInfo[] mColorTypeInfos = {};
    }
}
