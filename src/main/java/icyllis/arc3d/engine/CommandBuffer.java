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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.graphene.DrawPass;

import java.util.ArrayList;

/**
 * Backend-specific command buffer, render thread only.
 */
public abstract class CommandBuffer {

    @SharedPtr
    private final ArrayList<Buffer> mTrackingBuffers = new ArrayList<>();

    public void moveAndTrackGpuBuffer(@SharedPtr Buffer buffer) {
        mTrackingBuffers.add(buffer);
    }

    public boolean addRenderPass(RenderPassDesc renderPassDesc,
                                 DrawPass drawPass) {
        return false;
    }

    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   GpuRenderTarget renderTarget) {
        return false;
    }

    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   Image[] colorAttachments,
                                   Image[] resolveAttachments,
                                   Image depthStencilAttachment) {
        return false;
    }

    public void endRenderPass() {

    }

    public abstract boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline);

    /**
     * Records a non-indexed draw to current command buffer.
     *
     * @param vertexCount the number of vertices to draw
     * @param baseVertex  the index of the first vertex to draw
     */
    public abstract void draw(int vertexCount, int baseVertex);

    /**
     * Records an indexed draw to current command buffer.
     *
     * @param indexCount the number of vertices to draw
     * @param baseIndex  the base index within the index buffer
     * @param baseVertex the value added to the vertex index before indexing into the vertex buffer
     */
    public abstract void drawIndexed(int indexCount, int baseIndex,
                                     int baseVertex);

    /**
     * Records a non-indexed draw to current command buffer.
     *
     * @param instanceCount the number of instances to draw
     * @param baseInstance  the instance ID of the first instance to draw
     * @param vertexCount   the number of vertices to draw
     * @param baseVertex    the index of the first vertex to draw
     */
    public abstract void drawInstanced(int instanceCount, int baseInstance,
                                       int vertexCount, int baseVertex);

    /**
     * Records an indexed draw to current command buffer.
     *
     * @param indexCount    the number of vertices to draw
     * @param baseIndex     the base index within the index buffer
     * @param instanceCount the number of instances to draw
     * @param baseInstance  the instance ID of the first instance to draw
     * @param baseVertex    the value added to the vertex index before indexing into the vertex buffer
     */
    public abstract void drawIndexedInstanced(int indexCount, int baseIndex,
                                              int instanceCount, int baseInstance,
                                              int baseVertex);
}
