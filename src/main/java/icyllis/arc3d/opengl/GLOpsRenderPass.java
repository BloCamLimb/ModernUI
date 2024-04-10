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

import static icyllis.arc3d.engine.Engine.PrimitiveType;
import static org.lwjgl.opengl.GL11C.*;

public final class GLOpsRenderPass extends OpsRenderPass {

    private final GLDevice mDevice;

    private GLCommandBuffer mCmdBuffer;
    private GLGraphicsPipelineState mPipelineState;

    private byte mColorOps;
    private byte mStencilOps;
    private float[] mClearColor;

    private int mPrimitiveType;

    @SharedPtr
    private GpuBuffer mActiveIndexBuffer;
    @SharedPtr
    private GpuBuffer mActiveVertexBuffer;
    @SharedPtr
    private GpuBuffer mActiveInstanceBuffer;

    private int mIndexType;
    private int mVertexStreamOffset;
    private int mInstanceStreamOffset;

    public GLOpsRenderPass(GLDevice device) {
        mDevice = device;
    }

    @Override
    protected GLDevice getDevice() {
        return mDevice;
    }

    public GLOpsRenderPass set(GpuRenderTarget renderTarget,
                               Rect2i bounds, int origin,
                               byte colorOps,
                               byte stencilOps,
                               float[] clearColor) {
        set(renderTarget, origin);
        mColorOps = colorOps;
        mStencilOps = stencilOps;
        mClearColor = clearColor;
        return this;
    }

    @Override
    public void begin() {
        super.begin();
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mCmdBuffer = mDevice.beginRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps,
                mClearColor);
    }

    @Override
    public void end() {
        mActiveIndexBuffer = RefCnt.move(mActiveIndexBuffer);
        mActiveVertexBuffer = RefCnt.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = RefCnt.move(mActiveInstanceBuffer);
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mDevice.endRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps);
        super.end();
    }

    @Override
    protected boolean onBindPipeline(PipelineInfo pipelineInfo,
                                     GraphicsPipelineState pipelineState,
                                     Rect2fc drawBounds) {
        mActiveIndexBuffer = RefCnt.move(mActiveIndexBuffer);
        mActiveVertexBuffer = RefCnt.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = RefCnt.move(mActiveInstanceBuffer);

        mPipelineState = (GLGraphicsPipelineState) pipelineState;
        if (mPipelineState == null) {
            return false;
        }
        mPrimitiveType = switch (pipelineInfo.primitiveType()) {
            case PrimitiveType.PointList -> GL_POINTS;
            case PrimitiveType.LineList -> GL_LINES;
            case PrimitiveType.LineStrip -> GL_LINE_STRIP;
            case PrimitiveType.TriangleList -> GL_TRIANGLES;
            case PrimitiveType.TriangleStrip -> GL_TRIANGLE_STRIP;
            default -> throw new AssertionError();
        };

        //TODO flush RT again?
        if (!mPipelineState.bindPipeline(mCmdBuffer)) {
            return false;
        }

        return mPipelineState.bindUniforms(mCmdBuffer, pipelineInfo,
                mRenderTarget.getWidth(), mRenderTarget.getHeight());
    }

    @Override
    public void clearColor(int left, int top, int right, int bottom,
                           float red, float green, float blue, float alpha) {
        super.clearColor(left, top, right, bottom,
                red, green, blue, alpha);
    }

    @Override
    public void clearStencil(int left, int top, int right, int bottom, boolean insideMask) {
        super.clearStencil(left, top, right, bottom, insideMask);
    }

    @Override
    protected void onBindBuffers(@RawPtr GpuBuffer indexBuffer, int indexType,
                                 @RawPtr GpuBuffer vertexBuffer, int vertexStreamOffset,
                                 @RawPtr GpuBuffer instanceBuffer, int instanceStreamOffset) {
        assert (mPipelineState != null);
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mPipelineState.bindBuffers(indexBuffer, vertexBuffer, vertexStreamOffset,
                    instanceBuffer, instanceStreamOffset);
        } else if (indexBuffer == null || mDevice.getCaps().hasDrawElementsBaseVertexSupport()) {
            // bind instance buffer on drawInstanced()
            mPipelineState.bindBuffers(indexBuffer, vertexBuffer, vertexStreamOffset,
                    null, 0);
            mInstanceStreamOffset = instanceStreamOffset;
        } else {
            // bind vertex buffer on drawIndexed()
            mPipelineState.bindIndexBuffer((GLBuffer) indexBuffer);
            mVertexStreamOffset = vertexStreamOffset;
            mInstanceStreamOffset = instanceStreamOffset;
        }
        mActiveIndexBuffer = RefCnt.create(mActiveIndexBuffer, indexBuffer);
        mActiveVertexBuffer = RefCnt.create(mActiveVertexBuffer, vertexBuffer);
        mActiveInstanceBuffer = RefCnt.create(mActiveInstanceBuffer, instanceBuffer);
        mIndexType = switch (indexType) {
            case Engine.IndexType.kUByte -> GL_UNSIGNED_BYTE;
            case Engine.IndexType.kUShort -> GL_UNSIGNED_SHORT;
            case Engine.IndexType.kUInt -> GL_UNSIGNED_INT;
            default -> throw new AssertionError();
        };
    }

    @Override
    protected void onDraw(int vertexCount, int baseVertex) {
        mDevice.getGL().glDrawArrays(mPrimitiveType, baseVertex, vertexCount);
    }

    @Override
    protected void onDrawIndexed(int indexCount, int baseIndex,
                                 int baseVertex) {
        if (mDevice.getCaps().hasDrawElementsBaseVertexSupport()) {
            mDevice.getGL().glDrawElementsBaseVertex(mPrimitiveType, indexCount,
                    mIndexType, baseIndex, baseVertex);
        } else {
            long vertexOffset = (long) baseVertex * mPipelineState.getVertexStride() + mVertexStreamOffset;
            mPipelineState.bindVertexBuffer((GLBuffer) mActiveVertexBuffer, vertexOffset);
            mDevice.getGL().glDrawElements(mPrimitiveType, indexCount,
                    mIndexType, baseIndex);
        }
    }

    @Override
    protected void onDrawInstanced(int instanceCount, int baseInstance,
                                   int vertexCount, int baseVertex) {
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawArraysInstancedBaseInstance(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount, baseInstance);
        } else {
            long instanceOffset = (long) baseInstance * mPipelineState.getInstanceStride() + mInstanceStreamOffset;
            mPipelineState.bindInstanceBuffer((GLBuffer) mActiveInstanceBuffer, instanceOffset);
            mDevice.getGL().glDrawArraysInstanced(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount);
        }
    }

    @Override
    protected void onDrawIndexedInstanced(int indexCount, int baseIndex,
                                          int instanceCount, int baseInstance,
                                          int baseVertex) {
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                    mIndexType, baseIndex, instanceCount, baseVertex, baseInstance);
        } else {
            long instanceOffset = (long) baseInstance * mPipelineState.getInstanceStride() + mInstanceStreamOffset;
            mPipelineState.bindInstanceBuffer((GLBuffer) mActiveInstanceBuffer, instanceOffset);
            if (mDevice.getCaps().hasDrawElementsBaseVertexSupport()) {
                mDevice.getGL().glDrawElementsInstancedBaseVertex(mPrimitiveType, indexCount,
                        mIndexType, baseIndex, instanceCount, baseVertex);
            } else {
                long vertexOffset = (long) baseVertex * mPipelineState.getVertexStride() + mVertexStreamOffset;
                mPipelineState.bindVertexBuffer((GLBuffer) mActiveVertexBuffer, vertexOffset);
                mDevice.getGL().glDrawElementsInstanced(mPrimitiveType, indexCount,
                        mIndexType, baseIndex, instanceCount);
            }
        }
    }
}
