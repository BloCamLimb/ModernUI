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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayDeque;

/**
 * Backend-specific command buffer, executing thread only.
 */
public abstract class CommandBuffer {

    private final ObjectArrayList<@SharedPtr Resource> mTrackingUsageResources = new ObjectArrayList<>();
    private final ObjectArrayList<@SharedPtr ManagedResource> mTrackingManagedResources = new ObjectArrayList<>();
    private final ObjectArrayList<@SharedPtr Resource> mTrackingCommandBufferResources = new ObjectArrayList<>();

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();

    /**
     * Begin render pass. If successful, {@link #endRenderPass()} must be called.
     *
     * @param renderPassDesc   descriptor to create a render pass
     * @param framebufferDesc  descriptor to create a framebuffer
     * @param renderPassBounds content bounds of this render pass
     * @param clearColors      clear color for each color attachment
     * @param clearDepth       clear depth
     * @param clearStencil     clear stencil (unsigned)
     * @return success or not
     */
    public abstract boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                            FramebufferDesc framebufferDesc,
                                            Rect2ic renderPassBounds,
                                            float[] clearColors,
                                            float clearDepth,
                                            int clearStencil);

    /**
     * Set viewport, must be called after {@link #beginRenderPass}.
     */
    public abstract void setViewport(int x, int y, int width, int height);

    /**
     * Set scissor, must be called after {@link #beginRenderPass}.
     */
    public abstract void setScissor(int x, int y, int width, int height);

    /**
     * Bind graphics pipeline. Due to async compiling, it may fail.
     * Render pass scope, caller must track the pipeline.
     *
     * @param graphicsPipeline the pipeline object
     * @return success or not
     */
    public abstract boolean bindGraphicsPipeline(@RawPtr GraphicsPipeline graphicsPipeline);

    /**
     * Render pass scope, caller must track the buffer.
     *
     * @param indexType see {@link Engine.IndexType}
     */
    public abstract void bindIndexBuffer(int indexType,
                                         @RawPtr Buffer buffer,
                                         long offset);

    /**
     * Render pass scope, caller must track the buffer.
     */
    public abstract void bindVertexBuffer(int binding,
                                          @RawPtr Buffer buffer,
                                          long offset);

    /**
     * Render pass scope, caller must track the buffer.
     */
    public abstract void bindUniformBuffer(int binding,
                                           @RawPtr Buffer buffer,
                                           long offset,
                                           long size);

    /**
     * Bind texture view and sampler to the same binding point (combined image sampler).
     * Render pass scope, caller must track the image and sampler.
     *
     * @param binding     the binding index
     * @param texture     the texture image
     * @param sampler     the sampler state
     * @param swizzle the swizzle of the texture view for shader read, see {@link Swizzle}
     */
    public abstract void bindTextureSampler(int binding, @RawPtr Image texture,
                                            @RawPtr Sampler sampler, short swizzle);

    /**
     * Records a non-indexed draw to current command buffer.
     * Render pass scope.
     *
     * @param vertexCount the number of vertices to draw
     * @param baseVertex  the index of the first vertex to draw
     */
    public abstract void draw(int vertexCount, int baseVertex);

    /**
     * Records an indexed draw to current command buffer.
     * For OpenGL ES, if base vertex is unavailable, gl_VertexID always begins at 0.
     * Render pass scope.
     *
     * @param indexCount the number of vertices to draw
     * @param baseIndex  the base index within the index buffer
     * @param baseVertex the value added to the vertex index before indexing into the vertex buffer
     */
    public abstract void drawIndexed(int indexCount, int baseIndex,
                                     int baseVertex);

    /**
     * Records a non-indexed draw to current command buffer.
     * For OpenGL, regardless of the baseInstance value, gl_InstanceID always begins at 0.
     * Render pass scope.
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
     * For OpenGL ES, if base vertex is unavailable, gl_VertexID always begins at 0.
     * For OpenGL, regardless of the baseInstance value, gl_InstanceID always begins at 0.
     * Render pass scope.
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

    /**
     * End the current render pass.
     */
    public abstract void endRenderPass();

    /**
     * Performs a buffer-to-buffer copy.
     * <p>
     * Can only be used outside render passes.
     * <p>
     * The caller must track resources if success.
     */
    public final boolean copyBuffer(@RawPtr Buffer srcBuffer,
                                    @RawPtr Buffer dstBuffer,
                                    long srcOffset,
                                    long dstOffset,
                                    long size) {
        assert srcBuffer != null && dstBuffer != null;
        return onCopyBuffer(srcBuffer, dstBuffer, srcOffset, dstOffset, size);
    }

    protected abstract boolean onCopyBuffer(@RawPtr Buffer srcBuffer,
                                            @RawPtr Buffer dstBuffer,
                                            long srcOffset,
                                            long dstOffset,
                                            long size);

    /**
     * Performs a buffer-to-image copy.
     * <p>
     * Can only be used outside render passes.
     * <p>
     * The caller must track resources if success.
     */
    public final boolean copyBufferToImage(@RawPtr Buffer srcBuffer,
                                           @RawPtr Image dstImage,
                                           int srcColorType,
                                           int dstColorType,
                                           BufferImageCopyData[] copyData) {
        assert srcBuffer != null && dstImage != null && copyData.length > 0;
        if (!dstImage.isSampledImage() && !dstImage.isStorageImage()) {
            //TODO support copy to render buffer
            return false;
        }
        return onCopyBufferToImage(srcBuffer, dstImage, srcColorType, dstColorType, copyData);
    }

    protected abstract boolean onCopyBufferToImage(@RawPtr Buffer srcBuffer,
                                                   @RawPtr Image dstImage,
                                                   int srcColorType,
                                                   int dstColorType,
                                                   BufferImageCopyData[] copyData);

    /**
     * Perform an image-to-image copy, with the specified regions. Scaling is
     * not allowed.
     * <p>
     * If their dimensions are same and formats are compatible, then this method will
     * attempt to perform copy. Otherwise, this method will attempt to perform blit,
     * which may include format conversion.
     * <p>
     * Only mipmap level <var>level</var> of 2D images will be copied, without any
     * multisampled buffer and depth/stencil buffer.
     * <p>
     * Can only be used outside render passes.
     * <p>
     * The caller must track resources if success.
     *
     * @return success or not
     */
    public final boolean copyImage(@RawPtr Image srcImage,
                                   int srcL, int srcT, int srcR, int srcB,
                                   @RawPtr Image dstImage,
                                   int dstX, int dstY,
                                   int mipLevel) {
        assert srcImage != null && dstImage != null;
        return onCopyImage(srcImage, srcL, srcT, srcR, srcB, dstImage, dstX, dstY, mipLevel);
    }

    protected abstract boolean onCopyImage(@RawPtr Image srcImage,
                                           int srcL, int srcT, int srcR, int srcB,
                                           @RawPtr Image dstImage,
                                           int dstX, int dstY,
                                           int mipLevel);

    /**
     * Takes a Usage ref on the Resource that will be released when the command buffer
     * has finished execution.
     * <p>
     * This is mostly commonly used for host-visible Buffers and shared Resources.
     *
     * @param resource the resource to move
     */
    public final void trackResource(@SharedPtr Resource resource) {
        if (resource == null) {
            return;
        }
        mTrackingUsageResources.add(resource);
    }

    /**
     * Takes a ref on the ManagedResource that will be released when the command buffer
     * has finished execution.
     *
     * @param resource the resource to move
     */
    public final void trackResource(@SharedPtr ManagedResource resource) {
        if (resource == null) {
            return;
        }
        mTrackingManagedResources.add(resource);
    }

    /**
     * Takes a CommandBuffer ref on the Resource that will be released when the command buffer
     * has finished execution.
     * <p>
     * CommandBuffer ref allows a Resource to be returned to ResourceCache for reuse while
     * the CommandBuffer is still executing on the GPU. This is most commonly used for
     * GPU-only Resources.
     *
     * @param resource the resource to move
     */
    public final void trackCommandBufferResource(@SharedPtr Resource resource) {
        if (resource == null) {
            return;
        }
        resource.refCommandBuffer();
        mTrackingCommandBufferResources.add(resource);
        resource.unref();
    }

    // called by Queue, begin command buffer
    protected abstract void begin();

    // called by Queue, end command buffer and submit to queue
    protected abstract boolean submit(QueueManager queueManager);

    // called by Queue
    protected abstract boolean checkFinishedAndReset();

    /**
     * Blocks the current thread and waits for GPU to finish outstanding works.
     */
    // called by Queue, waitForQueue()
    protected abstract void waitUntilFinished();

    // called by subclass
    protected final void callFinishedCallbacks(boolean success) {
        for (var callback : mFinishedCallbacks) {
            callback.onFinished(success);
        }
        mFinishedCallbacks.clear();
    }

    // called by subclass
    protected final void releaseResources() {
        mTrackingUsageResources.forEach(Resource::unref);
        mTrackingUsageResources.clear();
        mTrackingManagedResources.forEach(RefCnt::unref);
        mTrackingManagedResources.clear();
        mTrackingCommandBufferResources.forEach(Resource::unrefCommandBuffer);
        mTrackingCommandBufferResources.clear();
    }
}
