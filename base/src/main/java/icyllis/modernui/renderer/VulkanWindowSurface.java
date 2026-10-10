/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.renderer;

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.sketch.Surface;
import icyllis.arc3d.vulkan.VKUtil;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanWindowSurface extends WindowSurface {

    private long mVkSurface; // VkSurfaceKHR

    private Surface[] mSwapchainSurfaces;
    private final VulkanManager mVulkanManager;
    private final RecordingContext mRecordingContext;

    public VulkanWindowSurface(RecordingContext recordingContext, long window,
                               VulkanManager manager) {
        long vkSurface = MemoryUtil.NULL;
        try (var stack = stackPush()) {
            var pSurface = stack.mallocLong(1);
            var res = SDLVulkan.SDL_Vulkan_CreateSurface(
                    window, manager.getInstance(), null, pSurface);
            if (!res) {
                throw new IllegalStateException("Cannot create vulkan window surface: " +
                        SDLError.SDL_GetError());
            }

            vkSurface = pSurface.get(0);

            var pSupportPresent = stack.mallocInt(1);
            var result = vkGetPhysicalDeviceSurfaceSupportKHR(
                    manager.getPhysicalDevice(), manager.getGraphicsQueueIndex(),
                    vkSurface, pSupportPresent);
            if (result != VK_SUCCESS) {
                throw new IllegalStateException("Failed to get surface support");
            }
            //TODO support dedicated present queue in Arc3D
            if (pSupportPresent.get(0) != VK_TRUE) {
                throw new IllegalStateException("Presentation queue != graphics queue");
            }

            mVkSurface = vkSurface;
            vkSurface = MemoryUtil.NULL;
        } finally {
            if (vkSurface != MemoryUtil.NULL ) {
                SDLVulkan.SDL_Vulkan_DestroySurface(
                        manager.getInstance(), vkSurface, null
                );
            }
        }

        mRecordingContext = recordingContext;
        mVulkanManager = manager;
    }

    @Override
    public long configure(int clientWidth, int clientHeight,
                          @ColorInfo.ColorType int desiredColorType,
                          ColorSpace desiredColorSpace) {
        int result;
        try (var stack = stackPush()) {
            var surfCaps = VkSurfaceCapabilitiesKHR.malloc(stack);
            result = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    mVulkanManager.getPhysicalDevice(),
                    mVkSurface,
                    surfCaps
            );
            if (result != VK_SUCCESS) {
                return 0;
            }

            var pFormatCount = stack.mallocInt(1);
            result = vkGetPhysicalDeviceSurfaceFormatsKHR(mVulkanManager.getPhysicalDevice(), mVkSurface, pFormatCount, null);
            if (result != VK_SUCCESS) {
                return 0;
            }
            int formatCount = pFormatCount.get(0);

            VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.calloc(formatCount, stack);
            result = vkGetPhysicalDeviceSurfaceFormatsKHR(mVulkanManager.getPhysicalDevice(), mVkSurface, pFormatCount, surfFormats);
            if (result != VK_SUCCESS) {
                return 0;
            }

           /* for (int i = 0; i < surfFormats.limit(); i++) {
                surfFormats.position(i);
                LOGGER.info("Surface format {}: format {} space {}", i,
                        VKUtil.vkFormatName(surfFormats.format()),
                        surfFormats.colorSpace());
            }*/

            // this is always supported, we will composite into swapchain
            int imageUsageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

            if ((surfCaps.supportedUsageFlags() & VK_IMAGE_USAGE_TRANSFER_DST_BIT) != 0) {
                imageUsageFlags |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
            }
        }

        return 0;
    }

    public int acquireNextImage() {
        return 0;
    }

    @Override
    public Surface getCurrentSurface() {
        return null;
    }
}
