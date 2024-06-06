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

import icyllis.arc3d.core.*;

import java.util.ArrayList;

/**
 * Backend-specific command buffer, executing thread only.
 */
public abstract class CommandBuffer {

    private final ArrayList<@SharedPtr Buffer> mTrackingBuffers = new ArrayList<>();

    public void moveAndTrackGpuBuffer(@SharedPtr Buffer buffer) {
        mTrackingBuffers.add(buffer);
    }

    public void begin() {
    }

    public void end() {
    }

    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   FramebufferDesc framebufferDesc,
                                   Rect2ic renderPassBounds,
                                   float[] clearColors,
                                   float clearDepth,
                                   int clearStencil) {
        return false;
    }

    public void endRenderPass() {

    }

    public abstract boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline);

    public abstract void setScissor(int left, int top, int right, int bottom);

    /**
     * @param indexType see {@link Engine.IndexType}
     */
    public abstract void bindIndexBuffer(int indexType,
                                         @RawPtr Buffer buffer,
                                         long offset);

    public abstract void bindVertexBuffer(int binding,
                                          @RawPtr Buffer buffer,
                                          long offset);

    public abstract void bindUniformBuffer(int binding,
                                           @RawPtr Buffer buffer,
                                           long offset,
                                           long size);

    /**
     * Bind combined texture sampler.
     *
     * @param readSwizzle see {@link Swizzle}
     */
    public abstract void bindTextureSampler(int binding, @RawPtr Image texture,
                                            @RawPtr Sampler sampler, short readSwizzle);

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

    public boolean checkFinished() {
        return false;
    }

    public void waitUntilFinished() {

    }
}
