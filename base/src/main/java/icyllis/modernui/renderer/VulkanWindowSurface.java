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
import icyllis.arc3d.core.ColorSpaces;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.engine.ISurface;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.vulkan.VKUtil;
import icyllis.arc3d.vulkan.VulkanBackendImage;
import icyllis.arc3d.vulkan.VulkanBackendSemaphore;
import icyllis.arc3d.vulkan.VulkanImageDesc;
import icyllis.arc3d.vulkan.VulkanImageMutableState;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanWindowSurface extends WindowSurface {

    private final long mVkSurface; // VkSurfaceKHR
    private long mSwapchain = VK_NULL_HANDLE; // VkSwapchainKHR

    private VulkanBackendSemaphore[] mRenderCompleteSemaphores;
    private GraniteSurface[] mSwapchainSurfaces;
    private final VulkanManager mVulkanManager;
    private final RecordingContext mRecordingContext;

    private final VkQueue mPresentQueue;

    private int mCurrentImageIndex;
    private long mCurrentAcquireSemaphore;

    public VulkanWindowSurface(RecordingContext recordingContext, long window,
                               VulkanManager manager, VkQueue presentQueue) {
        long vkSurface = VK_NULL_HANDLE;
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
            mPresentQueue = presentQueue;

            mVkSurface = vkSurface;
            vkSurface = VK_NULL_HANDLE;
        } finally {
            if (vkSurface != VK_NULL_HANDLE) {
                SDLVulkan.SDL_Vulkan_DestroySurface(
                        manager.getInstance(), vkSurface, null
                );
            }
        }

        mRecordingContext = recordingContext;
        mVulkanManager = manager;
    }

    @Override
    public boolean configure(int clientWidth, int clientHeight,
                             int desiredColorMode) {
        int result;
        try (var stack = stackPush()) {
            var surfCaps = VkSurfaceCapabilitiesKHR.malloc(stack);
            result = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    mVulkanManager.getPhysicalDevice(),
                    mVkSurface,
                    surfCaps
            );
            if (result != VK_SUCCESS) {
                return false;
            }

            var pCount = stack.mallocInt(1);
            result = vkGetPhysicalDeviceSurfaceFormatsKHR(mVulkanManager.getPhysicalDevice(), mVkSurface, pCount, null);
            if (result != VK_SUCCESS) {
                return false;
            }
            int formatCount = pCount.get(0);

            VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.calloc(formatCount, stack);
            result = vkGetPhysicalDeviceSurfaceFormatsKHR(mVulkanManager.getPhysicalDevice(), mVkSurface, pCount, surfFormats);
            if (result != VK_SUCCESS) {
                return false;
            }

            int bestFormat = VK_FORMAT_UNDEFINED;
            int bestColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
            //TODO this is a logic for COLOR_MODE_NORMAL, impl WCG and HDR in future
            // also use SRGB format in 4.0
            for (int i = 0; i < surfFormats.limit(); i++) {
                surfFormats.position(i);
                if (surfFormats.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    int format = surfFormats.format();
                    if (format == VK_FORMAT_R8G8B8A8_UNORM) {
                        bestFormat = format;
                        break;
                    } else if (format == VK_FORMAT_B8G8R8A8_UNORM) {
                        bestFormat = format;
                    }
                }
            }

            // this is always supported, we will composite into swapchain
            int imageUsageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

            if ((surfCaps.supportedUsageFlags() & VK_IMAGE_USAGE_TRANSFER_DST_BIT) != 0) {
                imageUsageFlags |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
            }

            int minImageCount = surfCaps.minImageCount() + 1;
            minImageCount = Math.min(minImageCount, surfCaps.maxImageCount());
            var currentExtent = surfCaps.currentExtent();

            long oldSwapchain = mSwapchain;

            VkSwapchainCreateInfoKHR swapchainCI = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType$Default()
                    .surface(mVkSurface)
                    .minImageCount(minImageCount)
                    .imageFormat(bestFormat)
                    .imageColorSpace(bestColorSpace)
                    .imageUsage(imageUsageFlags)
                    .preTransform(surfCaps.currentTransform())
                    .imageArrayLayers(1)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .presentMode(VK_PRESENT_MODE_FIFO_KHR)
                    .oldSwapchain(oldSwapchain)
                    .clipped(true)
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            swapchainCI.imageExtent(currentExtent);

            var pSwapchain = stack.mallocLong(1);
            result = vkCreateSwapchainKHR(mVulkanManager.getDevice(), swapchainCI, null, pSwapchain);
            if (result != VK_SUCCESS) {
                return false;
            }

            mSwapchain = pSwapchain.get(0);

            if (mSwapchainSurfaces != null) {
                for (var surface : mSwapchainSurfaces) {
                    surface.unref();
                }
                mSwapchainSurfaces = null;
            }

            if (oldSwapchain != VK_NULL_HANDLE) {
                vkQueueWaitIdle(mPresentQueue);

                vkDestroySwapchainKHR(mVulkanManager.getDevice(),
                        oldSwapchain, null);
            }

            if (mRenderCompleteSemaphores != null) {
                for (var semaphore : mRenderCompleteSemaphores) {
                    vkDestroySemaphore(mVulkanManager.getDevice(), semaphore.vkSemaphore(), null);
                }
                mRenderCompleteSemaphores = null;
            }

            result = vkGetSwapchainImagesKHR(mVulkanManager.getDevice(), mSwapchain, pCount, null);
            if (result != VK_SUCCESS) {
                return false;
            }

            int imageCount = pCount.get(0);

            var swapchainImages = stack.mallocLong(imageCount);
            result = vkGetSwapchainImagesKHR(mVulkanManager.getDevice(), mSwapchain, pCount, swapchainImages);
            if (result != VK_SUCCESS) {
                return false;
            }

            int engineImageFormat = VKUtil.vkFormatToImageFormat(bestFormat);

            VulkanImageDesc imageDesc = new VulkanImageDesc(
                    0, VK_IMAGE_TYPE_2D, bestFormat,
                    VK_IMAGE_TILING_OPTIMAL, imageUsageFlags,
                    VK_SHARING_MODE_EXCLUSIVE, Engine.ImageType.k2D,
                    /*viewFormat*/ engineImageFormat,
                    currentExtent.width(), currentExtent.height(),
                    1, 1,
                    1, 1,
                    ISurface.FLAG_RENDERABLE
            );
            //TODO match CT and CS
            ImageInfo imageInfo = ImageInfo.make(
                    currentExtent.width(), currentExtent.height(), ColorInfo.CT_RGBA_8888,
                    ColorInfo.AT_PREMUL, ColorSpaces.SRGB
            );

            mSwapchainSurfaces = new GraniteSurface[imageCount];

            for (int i = 0; i < imageCount; i++) {
                VulkanBackendImage backendImage = new VulkanBackendImage(
                        swapchainImages.get(i), imageDesc,
                        new VulkanImageMutableState(VK_IMAGE_LAYOUT_UNDEFINED, mVulkanManager.getGraphicsQueueIndex()),
                        null
                );
                mSwapchainSurfaces[i] = GraniteSurface.wrapBackendImage(
                        mRecordingContext, backendImage, imageInfo,
                        Engine.SurfaceOrigin.kUpperLeft,
                        null, "Swapchain" + i
                );
                if (mSwapchainSurfaces[i] == null) {
                    return false;
                }
            }

            mRenderCompleteSemaphores = new VulkanBackendSemaphore[imageCount];

            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType$Default();
            var pSemaphore = stack.mallocLong(1);
            for (int i = 0; i < imageCount; i++) {
                vkCreateSemaphore(mVulkanManager.getDevice(),
                        semaphoreCreateInfo,
                        null,
                        pSemaphore);
                mRenderCompleteSemaphores[i] = new VulkanBackendSemaphore(pSemaphore.get(0));
            }
        }

        mNeedsReconfigure = false;
        acquireNextImage();

        return !mNeedsReconfigure;
    }

    public void acquireNextImage() {
        try (var stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo
                    .calloc(stack)
                    .sType$Default();
            var pAcquireSemaphore = stack.mallocLong(1);
            vkCreateSemaphore(mVulkanManager.getDevice(),
                    semaphoreCreateInfo, null, pAcquireSemaphore);
            mCurrentAcquireSemaphore = pAcquireSemaphore.get(0);

            var pImageIndex = stack.mallocInt(1);
            int result = vkAcquireNextImageKHR(mVulkanManager.getDevice(),
                    mSwapchain, VKUtil.UINT64_MAX, mCurrentAcquireSemaphore,
                    VK_NULL_HANDLE, pImageIndex);
            mCurrentImageIndex = pImageIndex.get(0);

            if (result == VK_SUBOPTIMAL_KHR || result == VK_ERROR_OUT_OF_DATE_KHR) {
                mNeedsReconfigure = true;
            }
        }
    }

    public long getSwapchain() {
        return mSwapchain;
    }

    public long getCurrentAcquireSemaphore() {
        return mCurrentAcquireSemaphore;
    }

    public int getCurrentImageIndex() {
        return mCurrentImageIndex;
    }

    @Override
    public GraniteSurface getCurrentSurface() {
        return mSwapchainSurfaces[mCurrentImageIndex];
    }

    public VulkanBackendSemaphore getCurrentRenderCompleteSemaphore() {
        return mRenderCompleteSemaphores[mCurrentImageIndex];
    }
}
