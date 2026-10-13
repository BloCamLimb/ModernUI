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

import icyllis.arc3d.engine.CommandBuffer;
import icyllis.arc3d.engine.Image;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.Recording;
import icyllis.arc3d.vulkan.VulkanBackendSemaphore;
import icyllis.modernui.system.Looper;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;

import java.util.ArrayList;

import static icyllis.modernui.util.Log.LOGGER;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanRenderPipeline extends RenderPipeline {

    private final VulkanManager mVulkanManager;
    private final VkQueue mPresentQueue;

    public VulkanRenderPipeline(@NonNull Looper renderLooper,
                                @NonNull ImmediateContext immediateContext,
                                @NonNull VulkanManager vulkanManager) {
        super(renderLooper, immediateContext);
        mVulkanManager = vulkanManager;
        try (var stack = stackPush()) {
            var pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(vulkanManager.getDevice(), vulkanManager.getGraphicsQueueIndex(), 0, pQueue);
            mPresentQueue = new VkQueue(pQueue.get(0), vulkanManager.getDevice());
        }
    }

    @Override
    public WindowSurface createWindowSurface(long window) {
        return new VulkanWindowSurface(mUiRecordingContext, window, mVulkanManager, mPresentQueue);
    }

    @Override
    public boolean requiresPerSurfaceRecording() {
        // we use batching, don't need per-surface
        return false;
    }

    @Override
    public long getAdditionalWindowFlags() {
        return SDLVideo.SDL_WINDOW_VULKAN;
    }

    @Override
    public void doRender(FrameTask[] tasks) {
        ImmediateContext context = mImmediateContext;
        ArrayList<VulkanWindowSurface> surfaces = new ArrayList<>();
        ArrayList<VulkanBackendSemaphore> acquireSemaphores = new ArrayList<>();
        for (var task : tasks) {
            VulkanWindowSurface surface = (VulkanWindowSurface) task.surface;
            if (surface != null) {
                surfaces.add(surface);
            }
        }
        CommandBuffer currentCmdBuffer = null;
        if (!surfaces.isEmpty()) {
            currentCmdBuffer = context.getQueueManager().getCurrentCommandBuffer(true);
            for (VulkanWindowSurface surface : surfaces) {
                var acquireSem = new VulkanBackendSemaphore(surface.getCurrentAcquireSemaphore());
                currentCmdBuffer.waitSemaphore(acquireSem);
                acquireSemaphores.add(acquireSem);
            }
        }
        boolean taskAdded = false;
        for (var task : tasks) {
            Recording recording = task.surfaceBoundRecording;
            if (recording != null) {
                if (context.addTask(recording)) {
                    taskAdded = true;
                } else {
                    LOGGER.error("Failed to add draw commands");
                }
                recording.close();
                task.surfaceBoundRecording = null;
            }
        }

        if (!surfaces.isEmpty()) {
            currentCmdBuffer.addFinishedCallback(success -> {
                for (var sem : acquireSemaphores) {
                    vkDestroySemaphore(mVulkanManager.getDevice(), sem.vkSemaphore(), null);
                }
            });
            for (VulkanWindowSurface surface : surfaces) {
                Image dstImage = surface.getCurrentSurface().getBackingTarget().getImage();
                currentCmdBuffer.prepareSurfaceForStateUpdate(
                        dstImage, null, true
                );
                currentCmdBuffer
                        .signalSemaphore(surface.getCurrentRenderCompleteSemaphore());
            }
        }

        if (taskAdded || !surfaces.isEmpty()) {
            context.submit();
        }

        if (!surfaces.isEmpty()) {
            int surfaceCount = surfaces.size();
            try (var stack = stackPush()) {
                var waitSem = stack.mallocLong(surfaceCount);
                var indices = stack.mallocInt(surfaceCount);
                var swapchains = stack.mallocLong(surfaceCount);

                for (VulkanWindowSurface surface : surfaces) {
                    waitSem.put(
                            surface.getCurrentRenderCompleteSemaphore().vkSemaphore()
                    );
                    indices.put(
                            surface.getCurrentImageIndex()
                    );
                    swapchains.put(
                            surface.getSwapchain()
                    );
                }
                waitSem.flip();
                indices.flip();
                swapchains.flip();

                VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack)
                        .sType$Default()
                        .pWaitSemaphores(waitSem)
                        .swapchainCount(surfaceCount)
                        .pSwapchains(swapchains)
                        .pImageIndices(indices);

                vkQueuePresentKHR(
                        mPresentQueue,
                        present);
            }

            for (VulkanWindowSurface surface : surfaces) {
                surface.acquireNextImage();
                surface.notifyPresentationCompleted();
            }
        }
    }
}
