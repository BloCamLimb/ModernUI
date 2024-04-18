/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

    public void begin() {
        try (var stack = MemoryStack.stackPush()) {
            var beginInfo = VkCommandBufferBeginInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                    .pInheritanceInfo(null);
            _CHECK_ERROR_(vkBeginCommandBuffer(mCommandBuffer, beginInfo));
        }
    }

    public void end() {
        vkEndCommandBuffer(mCommandBuffer);
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

    public void bindVertexBuffers() {
        //vkCmdBindVertexBuffers();
    }

    public boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline) {
        //TODO
        return false;
    }

    public void addDrawPass(DrawPass drawPass) {
        int[] data = drawPass.getCommandData();
        for (int i = 0, e = drawPass.getCommandSize(); i < e; i++) {
            switch (data[i]) {
                case DrawCommandList.CMD_BIND_GRAPHICS_PIPELINE -> {
                    int pipelineIndex = data[i + 1];
                    if (!bindGraphicsPipeline(drawPass.getPipeline(pipelineIndex))) {
                        return;
                    }
                    i += 2;
                }
                case DrawCommandList.CMD_DRAW -> {
                    int vertexCount = data[i + 1];
                    int baseVertex = data[i + 2];
                    draw(vertexCount, baseVertex);
                    i += 3;
                }
                case DrawCommandList.CMD_DRAW_INDEXED -> {
                    int indexCount = data[i + 1];
                    int baseIndex = data[i + 2];
                    int baseVertex = data[i + 3];
                    drawIndexed(indexCount, baseIndex, baseVertex);
                    i += 4;
                }
                case DrawCommandList.CMD_DRAW_INSTANCED -> {
                    int instanceCount = data[i + 1];
                    int baseInstance = data[i + 2];
                    int vertexCount = data[i + 3];
                    int baseVertex = data[i + 4];
                    drawInstanced(instanceCount, baseInstance, vertexCount, baseVertex);
                    i += 5;
                }
                case DrawCommandList.CMD_DRAW_INDEXED_INSTANCED -> {
                    int indexCount = data[i + 1];
                    int baseIndex = data[i + 2];
                    int instanceCount = data[i + 3];
                    int baseInstance = data[i + 4];
                    int baseVertex = data[i + 5];
                    drawIndexedInstanced(indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
                    i += 6;
                }
            }
        }
        //TODO track resources
    }

    public boolean isRecording() {
        return mIsRecording;
    }
}
