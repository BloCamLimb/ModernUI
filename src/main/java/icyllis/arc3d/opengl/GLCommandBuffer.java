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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.DrawCommandList;
import icyllis.arc3d.granite.DrawPass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL32C.*;

/**
 * The main command buffer of OpenGL context. The commands executed on {@link GLCommandBuffer} are
 * mostly the same as that on {@link GLDevice}, but {@link GLCommandBuffer} assumes some values
 * and will not handle dirty context.
 *
 * @see GLDevice#beginRenderPass
 */
public final class GLCommandBuffer extends CommandBuffer {

    private static final int
            TriState_Disabled = 0,
            TriState_Enabled = 1,
            TriState_Unknown = 2;

    private final GLDevice mDevice;

    private int mHWViewportWidth;
    private int mHWViewportHeight;

    private int mHWScissorTest;
    private int mHWColorWrite;

    private int mHWScissorX;
    private int mHWScissorY;
    private int mHWScissorWidth;
    private int mHWScissorHeight;

    private int mHWFramebuffer;
    @SharedPtr
    private GLRenderTarget mHWRenderTarget;

    @SharedPtr
    private GLProgram mHWProgram;
    @SharedPtr
    private GLVertexArray mHWVertexArray;
    private boolean mHWVertexArrayInvalid;

    @SharedPtr
    private final GLUniformBuffer[] mBoundUniformBuffers;

    private GLGraphicsPipeline mGraphicsPipeline;

    private int mPrimitiveType;

    private int mIndexType;
    private long mIndexBufferOffset;

    //TODO shall we track refcnt here?
    @RawPtr
    private final GLBuffer[] mActiveVertexBuffers = new GLBuffer[Caps.MAX_VERTEX_BINDINGS];
    private final long[] mActiveVertexOffsets = new long[Caps.MAX_VERTEX_BINDINGS];

    private byte mColorOps;
    private byte mStencilOps;

    private long mSubmitFence;

    GLCommandBuffer(GLDevice device) {
        mDevice = device;

        mBoundUniformBuffers = new GLUniformBuffer[4];
    }

    void submit() {

        mSubmitFence = mDevice.getGL().glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    void checkFinishedAndReset() {
        if (mSubmitFence == 0) {
            return;
        }
        int status = mDevice.getGL().glClientWaitSync(mSubmitFence, 0, 0);
        if (status == GL_CONDITION_SATISFIED ||
                status == GL_ALREADY_SIGNALED) {

            mDevice.getGL().glDeleteSync(mSubmitFence);
        }
    }

    void resetStates(int states) {
        if ((states & Engine.GLBackendState.kRenderTarget) != 0) {
            //TODO 0?
            mHWFramebuffer = 0;
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        }

        if ((states & Engine.GLBackendState.kPipeline) != 0) {
            mHWProgram = RefCnt.move(mHWProgram);
            mHWVertexArray = RefCnt.move(mHWVertexArray);
            mHWVertexArrayInvalid = true;
            for (int i = 0; i < mBoundUniformBuffers.length; i++) {
                mBoundUniformBuffers[i] = RefCnt.move(mBoundUniformBuffers[i]);
            }
        }

        if ((states & Engine.GLBackendState.kView) != 0) {
            mHWScissorTest = TriState_Unknown;
            mHWScissorX = -1;
            mHWScissorY = -1;
            mHWScissorWidth = -1;
            mHWScissorHeight = -1;
            mHWViewportWidth = -1;
            mHWViewportHeight = -1;
        }

        if ((states & Engine.GLBackendState.kMisc) != 0) {
            mHWColorWrite = TriState_Unknown;
        }
    }

    /**
     * Flush viewport.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     */
    public void flushViewport(int width, int height) {
        assert (width >= 0 && height >= 0);
        if (width != mHWViewportWidth || height != mHWViewportHeight) {
            /*glViewportIndexedf(0, 0.0f, 0.0f, width, height);
            glDepthRangeIndexed(0, 0.0f, 1.0f);*/
            mDevice.getGL().glViewport(0, 0, width, height);
            mHWViewportWidth = width;
            mHWViewportHeight = height;
        }
    }

    /**
     * Flush scissor.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     * @param origin the surface origin
     * @see Engine.SurfaceOrigin
     */
    public void flushScissorRect(int width, int height, int origin,
                                 int scissorLeft, int scissorTop,
                                 int scissorRight, int scissorBottom) {
        assert (width >= 0 && height >= 0);
        final int scissorWidth = scissorRight - scissorLeft;
        final int scissorHeight = scissorBottom - scissorTop;
        assert (scissorLeft >= 0 && scissorTop >= 0 &&
                scissorWidth >= 0 && scissorWidth <= width &&
                scissorHeight >= 0 && scissorHeight <= height);
        final int scissorY;
        if (origin == Engine.SurfaceOrigin.kUpperLeft) {
            scissorY = scissorTop;
        } else {
            assert (origin == Engine.SurfaceOrigin.kLowerLeft);
            scissorY = height - scissorBottom;
        }
        assert (scissorY >= 0);
        if (scissorLeft != mHWScissorX ||
                scissorY != mHWScissorY ||
                scissorWidth != mHWScissorWidth ||
                scissorHeight != mHWScissorHeight) {
            mDevice.getGL().glScissor(scissorLeft, scissorY,
                    scissorWidth, scissorHeight);
            mHWScissorX = scissorLeft;
            mHWScissorY = scissorY;
            mHWScissorWidth = scissorWidth;
            mHWScissorHeight = scissorHeight;
        }
    }

    /**
     * Flush scissor test.
     *
     * @param enable whether to enable scissor test
     */
    public void flushScissorTest(boolean enable) {
        if (enable) {
            if (mHWScissorTest != TriState_Enabled) {
                mDevice.getGL().glEnable(GL_SCISSOR_TEST);
                mHWScissorTest = TriState_Enabled;
            }
        } else {
            if (mHWScissorTest != TriState_Disabled) {
                mDevice.getGL().glDisable(GL_SCISSOR_TEST);
                mHWScissorTest = TriState_Disabled;
            }
        }
    }

    /**
     * Flush color mask for draw buffer 0.
     *
     * @param enable whether to write color
     */
    public void flushColorWrite(boolean enable) {
        if (enable) {
            if (mHWColorWrite != TriState_Enabled) {
                mDevice.getGL().glColorMask(true, true, true, true);
                mHWColorWrite = TriState_Enabled;
            }
        } else {
            if (mHWColorWrite != TriState_Disabled) {
                mDevice.getGL().glColorMask(false, false, false, false);
                mHWColorWrite = TriState_Disabled;
            }
        }
    }

    /**
     * Bind raw framebuffer and flush render target to be invalid.
     */
    public void bindFramebuffer(int framebuffer) {
        mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        mHWRenderTarget = null;
    }

    /**
     * Flush framebuffer and viewport at the same time.
     *
     * @param target raw ptr to render target
     */
    public void flushRenderTarget(@Nullable @RawPtr GLRenderTarget target) {
        if (target == null) {
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        } else {
            int framebuffer = target.getRenderFramebuffer();
            if (mHWFramebuffer != framebuffer ||
                    mHWRenderTarget != target) {
                mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
                mHWFramebuffer = framebuffer;
                mHWRenderTarget = RefCnt.create(mHWRenderTarget, target);
                flushViewport(target.getWidth(), target.getHeight());
            }
            target.bindStencil();
        }
    }

    public void bindPipeline(@Nonnull @RawPtr GLProgram program,
                             @Nonnull @RawPtr GLVertexArray vertexArray) {
        if (mHWProgram != program) {
            // active program will not be deleted, so no collision
            mDevice.getGL().glUseProgram(program.getProgram());
            bindVertexArray(vertexArray);
            mHWProgram = RefCnt.create(mHWProgram, program);
        }
    }

    public void bindVertexArray(int vertexArray) {
        mDevice.getGL().glBindVertexArray(vertexArray);
        mHWVertexArray = RefCnt.move(mHWVertexArray);
        mHWVertexArrayInvalid = true;
    }

    public void bindVertexArray(@Nullable @RawPtr GLVertexArray vertexArray) {
        if (mHWVertexArrayInvalid ||
                mHWVertexArray != vertexArray) {
            // active vertex array will not be deleted, so no collision
            mDevice.getGL().glBindVertexArray(vertexArray == null ? 0 : vertexArray.getHandle());
            mHWVertexArray = RefCnt.create(mHWVertexArray, vertexArray);
            mHWVertexArrayInvalid = false;
        }
    }

    public void bindUniformBuffer(@Nonnull @RawPtr GLUniformBuffer uniformBuffer) {
        int index = uniformBuffer.getBinding();
        if (mBoundUniformBuffers[index] != uniformBuffer) {
            mDevice.getGL().glBindBufferBase(GL_UNIFORM_BUFFER, index, uniformBuffer.getHandle());
            mBoundUniformBuffers[index] = RefCnt.create(mBoundUniformBuffers[index], uniformBuffer);
        }
    }

    /**
     * Bind texture view and sampler to the same binding point.
     *
     * @param bindingUnit  the binding index (texture unit)
     * @param texture      the texture image
     * @param samplerState the sampler state for creating sampler, see {@link SamplerState}
     * @param readSwizzle  the swizzle of the texture view for shader read, see {@link Swizzle}
     */
    public void bindTextureSampler(int bindingUnit, GLTexture texture,
                                   int samplerState, short readSwizzle) {
        assert (texture != null && texture.isSampledImage());
        if (SamplerState.isMipmapped(samplerState)) {
            if (!texture.isMipmapped()) {
                assert (!SamplerState.isAnisotropy(samplerState));
                samplerState = SamplerState.resetMipmapMode(samplerState);
            } else {
                assert (!texture.isMipmapsDirty());
            }
        }
        mDevice.bindTextureSampler(bindingUnit, texture, samplerState, readSwizzle);
    }

    /*@Override
    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   GpuRenderTarget renderTarget) {
        GLRenderTarget glRenderTarget = (GLRenderTarget) renderTarget;
        mColorOps = renderPassDesc.mColorOps;
        mStencilOps = renderPassDesc.mDepthStencilOps;
        mDevice.beginRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps,
                renderPassDesc.mClearColor);
        return true;
    }*/

    @Override
    public void endRenderPass() {
        /*mActiveIndexBuffer = RefCnt.move(mActiveIndexBuffer);
        mActiveVertexBuffer = RefCnt.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = RefCnt.move(mActiveInstanceBuffer);*/
        GLRenderTarget glRenderTarget = mHWRenderTarget;
        mDevice.endRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps);
    }

    @Override
    public boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline) {
        /*mActiveIndexBuffer = RefCnt.move(mActiveIndexBuffer);
        mActiveVertexBuffer = RefCnt.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = RefCnt.move(mActiveInstanceBuffer);*/

        mGraphicsPipeline = (GLGraphicsPipeline) graphicsPipeline;
        if (mGraphicsPipeline == null) {
            return false;
        }
        mPrimitiveType = switch (graphicsPipeline.getPrimitiveType()) {
            case Engine.PrimitiveType.PointList -> GL_POINTS;
            case Engine.PrimitiveType.LineList -> GL_LINES;
            case Engine.PrimitiveType.LineStrip -> GL_LINE_STRIP;
            case Engine.PrimitiveType.TriangleList -> GL_TRIANGLES;
            case Engine.PrimitiveType.TriangleStrip -> GL_TRIANGLE_STRIP;
            default -> throw new AssertionError();
        };

        //TODO flush RT again?
        if (!mGraphicsPipeline.bindPipeline(this)) {
            return false;
        }

        /*return mPipelineState.bindUniforms(mCmdBuffer, pipelineInfo,
                mRenderTarget.getWidth(), mRenderTarget.getHeight());*/
        return true;
    }

    @Override
    public void bindIndexBuffer(int indexType, @RawPtr Buffer buffer, long offset) {
        assert (mGraphicsPipeline != null);
        mIndexType = switch (indexType) {
            case Engine.IndexType.kUByte -> GL_UNSIGNED_BYTE;
            case Engine.IndexType.kUShort -> GL_UNSIGNED_SHORT;
            case Engine.IndexType.kUInt -> GL_UNSIGNED_INT;
            default -> throw new AssertionError();
        };
        mGraphicsPipeline.bindIndexBuffer((GLBuffer) buffer);
        mIndexBufferOffset = offset;
    }

    @Override
    public void bindVertexBuffer(int binding, @RawPtr Buffer buffer, long offset) {
        assert (mGraphicsPipeline != null);
        GLBuffer glBuffer = (GLBuffer) buffer;
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            // base instance support covers all cases
            mGraphicsPipeline.bindVertexBuffer(binding, glBuffer, offset);
        } else {
            // bind instance buffer on drawInstanced(), we may rebind vertex buffer on drawIndexed()
            if (mGraphicsPipeline.getVertexInputRate(binding) == VertexInputLayout.INPUT_RATE_VERTEX) {
                mGraphicsPipeline.bindVertexBuffer(binding, glBuffer, offset);
            }
            mActiveVertexBuffers[binding] = glBuffer;
            mActiveVertexOffsets[binding] = offset;
        }
    }

    private long getIndexOffset(int baseIndex) {
        int indexSize = Engine.IndexType.size(mIndexType);
        return (long) baseIndex * indexSize + mIndexBufferOffset;
    }

    @Override
    public void draw(int vertexCount, int baseVertex) {
        mDevice.getGL().glDrawArrays(mPrimitiveType, baseVertex, vertexCount);
    }

    @Override
    public void drawIndexed(int indexCount, int baseIndex,
                            int baseVertex) {
        long indicesOffset = getIndexOffset(baseIndex);
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset, 1, baseVertex, 0);
        } else if (mDevice.getCaps().hasDrawElementsBaseVertexSupport()) {
            mDevice.getGL().glDrawElementsBaseVertex(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset, baseVertex);
        } else {
            int bindings = mGraphicsPipeline.getVertexBindingCount();
            for (int i = 0; i < bindings; i++) {
                assert (mGraphicsPipeline.getVertexInputRate(i) == VertexInputLayout.INPUT_RATE_VERTEX);
                long vertexOffset = (long) baseVertex * mGraphicsPipeline.getVertexStride(i) +
                        mActiveVertexOffsets[i];
                mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], vertexOffset);
            }
            mDevice.getGL().glDrawElements(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset);
        }
    }

    @Override
    public void drawInstanced(int instanceCount, int baseInstance,
                              int vertexCount, int baseVertex) {
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawArraysInstancedBaseInstance(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount, baseInstance);
        } else {
            int bindings = mGraphicsPipeline.getVertexBindingCount();
            for (int i = 0; i < bindings; i++) {
                if (mGraphicsPipeline.getVertexInputRate(i) > VertexInputLayout.INPUT_RATE_VERTEX) {
                    long instanceOffset = (long) baseInstance * mGraphicsPipeline.getVertexStride(i) +
                            mActiveVertexOffsets[i];
                    mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], instanceOffset);
                }
            }
            mDevice.getGL().glDrawArraysInstanced(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount);
        }
    }

    @Override
    public void drawIndexedInstanced(int indexCount, int baseIndex,
                                     int instanceCount, int baseInstance,
                                     int baseVertex) {
        long indicesOffset = getIndexOffset(baseIndex);
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset, instanceCount, baseVertex, baseInstance);
        } else {
            boolean hasBaseVertexSupport = mDevice.getCaps().hasDrawElementsBaseVertexSupport();
            int bindings = mGraphicsPipeline.getVertexBindingCount();
            for (int i = 0; i < bindings; i++) {
                if (mGraphicsPipeline.getVertexInputRate(i) > VertexInputLayout.INPUT_RATE_VERTEX) {
                    long instanceOffset = (long) baseInstance * mGraphicsPipeline.getVertexStride(i) +
                            mActiveVertexOffsets[i];
                    mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], instanceOffset);
                } else if (!hasBaseVertexSupport) {
                    long vertexOffset = (long) baseVertex * mGraphicsPipeline.getVertexStride(i) +
                            mActiveVertexOffsets[i];
                    mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], vertexOffset);
                }
            }
            if (hasBaseVertexSupport) {
                mDevice.getGL().glDrawElementsInstancedBaseVertex(mPrimitiveType, indexCount,
                        mIndexType, indicesOffset, instanceCount, baseVertex);
            } else {
                mDevice.getGL().glDrawElementsInstanced(mPrimitiveType, indexCount,
                        mIndexType, indicesOffset, instanceCount);
            }
        }
    }
}
