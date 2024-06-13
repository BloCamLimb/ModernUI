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

import icyllis.arc3d.core.Rect2ic;
import icyllis.arc3d.engine.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static icyllis.arc3d.vulkan.VKCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class VulkanCommandBuffer extends CommandBuffer {

    protected final VkCommandBuffer mCommandBuffer;
    protected boolean mIsRecording = false;

    public VulkanCommandBuffer(VkDevice device, long handle) {
        mCommandBuffer = new VkCommandBuffer(handle, device);
    }

    @Override
    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   FramebufferDesc framebufferDesc,
                                   Rect2ic renderPassBounds,
                                   float[] clearColors,
                                   float clearDepth,
                                   int clearStencil) {
        return false;
    }

    public boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline) {
        //TODO
        return false;
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {

    }

    @Override
    public void setScissor(int x, int y, int width, int height) {

    }

    @Override
    public void bindIndexBuffer(int indexType, Buffer buffer, long offset) {
        //vkCmdBindIndexBuffer();
    }

    @Override
    public void bindVertexBuffer(int binding, Buffer buffer, long offset) {
        // record each binding here, and bindVertexBuffers() together
    }

    @Override
    public void bindUniformBuffer(int binding, Buffer buffer, long offset, long size) {

    }

    @Override
    public void bindTextureSampler(int binding, Image texture, Sampler sampler, short readSwizzle) {

    }

    @Override
    public void draw(int vertexCount, int baseVertex) {
        drawInstanced(1, 0, vertexCount, baseVertex);
    }

    @Override
    public void drawIndexed(int indexCount, int baseIndex, int baseVertex) {
        drawIndexedInstanced(indexCount, baseIndex, 1, 0, baseVertex);
    }

    @Override
    public void drawInstanced(int instanceCount, int baseInstance,
                              int vertexCount, int baseVertex) {
        vkCmdDraw(mCommandBuffer,
                vertexCount,
                instanceCount,
                baseVertex,
                baseInstance);
    }

    @Override
    public void drawIndexedInstanced(int indexCount, int baseIndex,
                                     int instanceCount, int baseInstance,
                                     int baseVertex) {
        vkCmdDrawIndexed(mCommandBuffer,
                indexCount,
                instanceCount,
                baseIndex,
                baseVertex,
                baseInstance);
    }

    @Override
    public void endRenderPass() {

    }

    @Override
    protected void begin() {
        try (var stack = MemoryStack.stackPush()) {
            var beginInfo = VkCommandBufferBeginInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                    .pInheritanceInfo(null);
            _CHECK_ERROR_(vkBeginCommandBuffer(mCommandBuffer, beginInfo));
        }
    }

    @Override
    protected boolean submit(QueueManager queueManager) {
        vkEndCommandBuffer(mCommandBuffer);
        return false;
    }

    @Override
    protected boolean checkFinishedAndReset() {
        return false;
    }

    @Override
    protected void waitUntilFinished() {
    }

    public void bindVertexBuffers() {
        //vkCmdBindVertexBuffers();
    }

    public boolean isRecording() {
        return mIsRecording;
    }
}
