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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.UniformHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class manages a GPU program and records per-program information. It also records the vertex
 * and instance attribute layouts that are to be used with the program.
 * <p>
 * This class holds onto a {@link GLProgram} object that we use for draws. Besides storing the actual
 * {@link GLProgram} object, this class is also responsible handling all uniforms, buffers, samplers,
 * and other similar objects that are used along with the GLProgram and GLVertexArray in the draw.
 * This includes both allocating and freeing these objects, as well as updating their values.
 * <p>
 * Supports OpenGL 3.3 and OpenGL 4.5.
 */
//TODO set and bind textures
public class GLGraphicsPipeline extends GraphicsPipeline {

    @SharedPtr
    private GLProgram mProgram;
    @SharedPtr
    private GLVertexArray mVertexArray;

    private GLUniformDataManager mDataManager;

    // the installed effects, unique ptr
    private GeometryProcessor.ProgramImpl mGPImpl;

    private int mNumTextureSamplers;

    private CompletableFuture<GLGraphicsPipelineBuilder> mAsyncWork;

    GLGraphicsPipeline(GLDevice device,
                       byte primitiveType,
                       CompletableFuture<GLGraphicsPipelineBuilder> asyncWork) {
        super(device, primitiveType);
        mAsyncWork = asyncWork;
    }

    void init(@SharedPtr GLProgram program,
              @SharedPtr GLVertexArray vertexArray,
              List<UniformHandler.UniformInfo> uniforms,
              int uniformSize,
              List<UniformHandler.UniformInfo> samplers,
              GeometryProcessor.ProgramImpl gpImpl) {
        mProgram = program;
        mVertexArray = vertexArray;
        mGPImpl = gpImpl;
        mDataManager = new GLUniformDataManager(uniforms, uniformSize);
        mNumTextureSamplers = samplers.size();
    }

    public void discard() {
        if (mAsyncWork != null) {
            mAsyncWork.cancel(true);
            mAsyncWork = null;
        }
        if (mProgram != null) {
            mProgram.discard();
            if (mVertexArray.unique()) {
                mVertexArray.discard();
            }
        }
    }

    @Override
    protected void deallocate() {
        mProgram = RefCnt.move(mProgram);
        mVertexArray = RefCnt.move(mVertexArray);
        mDataManager = RefCnt.move(mDataManager);
    }

    private void checkAsyncWork() {
        boolean success = mAsyncWork.join().finish(this);
        var stats = getDevice().getPipelineCache().getStats();
        if (success) {
            stats.incNumCompilationSuccesses();
        } else {
            stats.incNumCompilationFailures();
        }
        mAsyncWork = null;
    }

    public boolean bindPipeline(GLCommandBuffer commandBuffer) {
        if (mAsyncWork != null) {
            checkAsyncWork();
        }
        if (mProgram != null) {
            assert (mVertexArray != null);
            commandBuffer.bindPipeline(mProgram, mVertexArray);
            return true;
        }
        return false;
    }

    public boolean bindUniforms(GLCommandBuffer commandBuffer,
                                PipelineInfo pipelineInfo,
                                int width, int height) {
        mDataManager.setProjection(0, width, height,
                pipelineInfo.origin() == Engine.SurfaceOrigin.kLowerLeft);
        mGPImpl.setData(mDataManager, pipelineInfo.geomProc());
        //TODO FP and upload

        return mDataManager.bindAndUploadUniforms(getDevice(), commandBuffer);
    }

    /**
     * Binds all geometry processor and fragment processor textures.
     */
    public boolean bindTextures(GLCommandBuffer commandBuffer,
                                PipelineInfo pipelineInfo,
                                ImageProxy[] geomTextures) {
        int unit = 0;
        for (int i = 0, n = pipelineInfo.geomProc().numTextureSamplers(); i < n; i++) {
            GLImage texture = (GLImage) geomTextures[i].getGpuImage();
            commandBuffer.bindTextureSampler(unit++, texture,
                    pipelineInfo.geomProc().textureSamplerState(i),
                    pipelineInfo.geomProc().textureSamplerSwizzle(i));
        }
        //TODO bind FP textures

        assert unit == mNumTextureSamplers;
        return true;
    }

    /**
     * Binds all geometric buffers.
     */
    public void bindBuffers(@Nullable @RawPtr GpuBuffer indexBuffer,
                            @Nullable @RawPtr GpuBuffer vertexBuffer,
                            long vertexOffset,
                            @Nullable @RawPtr GpuBuffer instanceBuffer,
                            long instanceOffset) {
        if (indexBuffer != null) {
            bindIndexBuffer((GLBuffer) indexBuffer);
        }
        if (vertexBuffer != null) {
            bindVertexBuffer((GLBuffer) vertexBuffer, vertexOffset);
        }
        if (instanceBuffer != null) {
            bindInstanceBuffer((GLBuffer) instanceBuffer, instanceOffset);
        }
    }

    public int getVertexStride() {
        return mVertexArray.getVertexStride();
    }

    public int getInstanceStride() {
        return mVertexArray.getInstanceStride();
    }

    /**
     * Set element buffer (index buffer).
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the element buffer object, raw ptr
     */
    public void bindIndexBuffer(@Nonnull @RawPtr GLBuffer buffer) {
        if (mVertexArray != null) {
            mVertexArray.bindIndexBuffer(buffer);
        }
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next vertex data, in bytes, is determined in constructor.
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first vertex data to the head of the buffer, in bytes
     */
    public void bindVertexBuffer(@Nonnull @RawPtr GLBuffer buffer, long offset) {
        if (mVertexArray != null) {
            mVertexArray.bindVertexBuffer(buffer, offset);
        }
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next instance data, in bytes, is determined in constructor.
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first instance data to the head of the buffer, in bytes
     */
    public void bindInstanceBuffer(@Nonnull @RawPtr GLBuffer buffer, long offset) {
        if (mVertexArray != null) {
            mVertexArray.bindInstanceBuffer(buffer, offset);
        }
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }
}
