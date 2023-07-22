/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.vulkan;

import icyllis.arc3d.engine.ManagedResource;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import javax.annotation.Nullable;

import static icyllis.arc3d.vulkan.VKCore.*;

/**
 * VkCommandPool is created with a single primary command buffer and (optional)
 * a set of secondary command buffers. After submission, the command pool enters
 * the in flight state, and we will continuously check whether the GPU has finished
 * the work to recycle the VkCommandPool (and its command buffers).
 */
public class VulkanCommandPool extends ManagedResource {

    private VulkanPrimaryCommandBuffer mPrimaryCommandBuffer;

    private long mCommandPool;
    private boolean mSubmitted;

    private final long[] mSubmitFence = new long[1];

    private VulkanCommandPool(VulkanServer server, long handle,
                              VulkanPrimaryCommandBuffer primaryCommandBuffer) {
        super(server);
        mCommandPool = handle;
        mPrimaryCommandBuffer = primaryCommandBuffer;
    }

    @Nullable
    public static VulkanCommandPool create(VulkanServer server) {
        int cmdPoolCreateFlags = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
        if (server.isProtectedContext()) {
            cmdPoolCreateFlags |= VK_COMMAND_POOL_CREATE_PROTECTED_BIT;
        }
        long commandPool;
        try (var stack = MemoryStack.stackPush()) {
            var pCommandPool = stack.mallocLong(1);
            var result = vkCreateCommandPool(
                    server.device(),
                    VkCommandPoolCreateInfo
                            .malloc(stack)
                            .sType$Default()
                            .pNext(0)
                            .flags(cmdPoolCreateFlags)
                            .queueFamilyIndex(server.getQueueIndex()),
                    null,
                    pCommandPool
            );
            if (result != VK_SUCCESS) {
                return null;
            }
            commandPool = pCommandPool.get(0);
        }
        VulkanPrimaryCommandBuffer primaryCommandBuffer =
                VulkanPrimaryCommandBuffer.create(server, commandPool);
        if (primaryCommandBuffer == null) {
            vkDestroyCommandPool(
                    server.device(),
                    commandPool,
                    null
            );
            return null;
        }
        return new VulkanCommandPool(server, commandPool, primaryCommandBuffer);
    }

    @Override
    protected void deallocate() {
        vkDestroyCommandPool(
                getServer().device(),
                mCommandPool,
                null
        );
    }

    public boolean submit() {
        assert !mPrimaryCommandBuffer.isRecording();

        if (mSubmitFence[0] == VK_NULL_HANDLE) {
            try (var stack = MemoryStack.stackPush()) {
                var result = vkCreateFence(
                        getServer().device(),
                        VkFenceCreateInfo
                                .calloc(stack)
                                .sType$Default(),
                        null,
                        mSubmitFence
                );
                if (result != VK_SUCCESS) {
                    return false;
                }
                mSubmitFence[0] = VK_NULL_HANDLE;
            }
        } else {
            CHECK_ERROR(vkResetFences(
                    getServer().device(),
                    mSubmitFence
            ));
        }

        return false;
    }

    public boolean check() {
        if (!mSubmitted) {
            return false;
        }
        if (mSubmitFence[0] == VK_NULL_HANDLE) {
            return true;
        }
        var result = vkGetFenceStatus(
                getServer().device(),
                mSubmitFence[0]
        );
        if (result == VK_SUCCESS ||
                result == VK_ERROR_DEVICE_LOST) {
            return true;
        }
        if (result == VK_NOT_READY) {
            return false;
        }
        throw new RuntimeException(VKCore.getResultMessage(result));
    }

    public void reset() {
        assert isSubmitted();

        vkResetCommandPool(
                getServer().device(),
                mCommandPool,
                0
        );

        mSubmitted = false;
    }

    public boolean isSubmitted() {
        return mSubmitted;
    }

    @Override
    protected VulkanServer getServer() {
        return (VulkanServer) super.getServer();
    }
}
