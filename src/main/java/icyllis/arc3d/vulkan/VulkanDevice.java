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

import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.engine.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static org.lwjgl.vulkan.VK11.*;

public class VulkanDevice extends Device {

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

    @Nullable
    public static VulkanDevice make(@Nonnull VulkanBackendContext context,
                                    ContextOptions options) {
        if (context.mInstance == null ||
                context.mPhysicalDevice == null ||
                context.mDevice == null ||
                context.mQueue == null) {
            return null;
        }

        VulkanCaps caps;
        try (var stack = MemoryStack.stackPush()) {
            final VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2
                    .calloc(stack)
                    .sType$Default();
            vkGetPhysicalDeviceProperties2(context.mPhysicalDevice, properties2);
            final VkPhysicalDeviceProperties properties = properties2.properties();
            caps = new VulkanCaps(options,
                    context.mPhysicalDevice,
                    properties.apiVersion(),
                    context.mDeviceFeatures2,
                    context.mInstance.getCapabilities(),
                    context.mDevice.getCapabilities());
        }

        VulkanMemoryAllocator allocator = context.mMemoryAllocator;
        if (allocator == null) {
            return null;
        }

        return new VulkanDevice(options, caps,
                context, allocator);
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

    @Override
    public ResourceProvider makeResourceProvider(Context context, long maxResourceBudget) {
        return new VulkanResourceProvider(this, context, maxResourceBudget);
    }

    @Nullable
    @Override
    protected GpuRenderTarget onCreateRenderTarget(int width, int height, int sampleCount, int numColorTargets, @Nullable Image[] colorTargets, @Nullable Image[] resolveTargets, @Nullable int[] mipLevels, @Nullable Image depthStencilTarget, int surfaceFlags) {
        return null;
    }

    @Nullable
    @Override
    protected GpuRenderTarget onWrapRenderableBackendTexture(BackendImage texture, int sampleCount, boolean ownership) {
        return null;
    }

    @Nullable
    @Override
    public GpuRenderTarget onWrapBackendRenderTarget(BackendRenderTarget backendRenderTarget) {
        return null;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(ImageProxyView writeView, Rect2i contentBounds, byte colorOps, byte stencilOps, float[] clearColor, Set<SurfaceProxy> sampledTextures, int pipelineFlags) {
        return null;
    }

    @Override
    protected void onResolveRenderTarget(GpuRenderTarget renderTarget, int resolveLeft, int resolveTop, int resolveRight, int resolveBottom) {

    }

    @Override
    public long insertFence() {
        return 0;
    }

    @Override
    public boolean checkFence(long fence) {
        return false;
    }

    @Override
    public void deleteFence(long fence) {

    }

    @Override
    public void addFinishedCallback(FlushInfo.FinishedCallback callback) {

    }

    @Override
    public void checkFinishedCallbacks() {

    }

    @Override
    public void waitForQueue() {

    }
}
