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
import static icyllis.arc3d.opengl.GLCore.*;

public final class GLOpsRenderPass extends OpsRenderPass {

    private final GLDevice mDevice;

    private GLCommandBuffer mCmdBuffer;
    private GLGraphicsPipelineState mPipelineState;

    private byte mColorOps;
    private byte mStencilOps;
    private float[] mClearColor;

    @SharedPtr
    private GpuBuffer mActiveIndexBuffer;
    @SharedPtr
    private GpuBuffer mActiveVertexBuffer;
    @SharedPtr
    private GpuBuffer mActiveInstanceBuffer;

    private int mPrimitiveType;

    public GLOpsRenderPass(GLDevice device) {
        mDevice = device;
    }

    @Override
    protected GLDevice getDevice() {
        return mDevice;
    }

    public GLOpsRenderPass set(GpuRenderTarget rt,
                               Rect2i bounds, int origin,
                               byte colorOps,
                               byte stencilOps,
                               float[] clearColor) {
        set(rt, origin);
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
        mActiveIndexBuffer = GpuResource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = GpuResource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = GpuResource.move(mActiveInstanceBuffer);
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mDevice.endRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps);
        super.end();
    }

    @Override
    protected boolean onBindPipeline(PipelineInfo pipelineInfo,
                                     GraphicsPipelineState pipelineState,
                                     Rect2f drawBounds) {
        mActiveIndexBuffer = GpuResource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = GpuResource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = GpuResource.move(mActiveInstanceBuffer);

        mPipelineState = (GLGraphicsPipelineState) pipelineState;
        if (mPipelineState == null) {
            return false;
        }
        mPrimitiveType = switch (pipelineInfo.primitiveType()) {
            case PrimitiveType.PointList        -> GL_POINTS;
            case PrimitiveType.LineList         -> GL_LINES;
            case PrimitiveType.LineStrip        -> GL_LINE_STRIP;
            case PrimitiveType.TriangleList     -> GL_TRIANGLES;
            case PrimitiveType.TriangleStrip    -> GL_TRIANGLE_STRIP;
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
    protected void onBindBuffers(GpuBuffer indexBuffer,
                                 GpuBuffer vertexBuffer,
                                 GpuBuffer instanceBuffer) {
        assert (mPipelineState != null);
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mPipelineState.bindBuffers(indexBuffer, vertexBuffer, 0, instanceBuffer, 0);
        } else {
            // bind instance buffer on drawInstanced()
            mPipelineState.bindBuffers(indexBuffer, vertexBuffer, 0, null, 0);
        }
        mActiveIndexBuffer = GpuResource.create(mActiveIndexBuffer, indexBuffer);
        mActiveVertexBuffer = GpuResource.create(mActiveVertexBuffer, vertexBuffer);
        mActiveInstanceBuffer = GpuResource.create(mActiveInstanceBuffer, instanceBuffer);
    }

    @Override
    protected void onDraw(int vertexCount, int baseVertex) {
        glDrawArrays(mPrimitiveType, baseVertex, vertexCount);
    }

    @Override
    protected void onDrawIndexed(int indexCount, int baseIndex,
                                 int baseVertex) {
        nglDrawElementsBaseVertex(mPrimitiveType, indexCount,
                GL_UNSIGNED_SHORT, baseIndex, baseVertex);
    }

    @Override
    protected void onDrawInstanced(int instanceCount, int baseInstance,
                                   int vertexCount, int baseVertex) {
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            glDrawArraysInstancedBaseInstance(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount, baseInstance);
        } else {
            long instanceOffset = (long) baseInstance * mPipelineState.getInstanceStride();
            mPipelineState.bindInstanceBuffer((GLBuffer) mActiveInstanceBuffer, instanceOffset);
            glDrawArraysInstanced(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount);
        }
    }

    @Override
    protected void onDrawIndexedInstanced(int indexCount, int baseIndex,
                                          int instanceCount, int baseInstance,
                                          int baseVertex) {
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            nglDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                    GL_UNSIGNED_SHORT, baseIndex, instanceCount, baseVertex, baseInstance);
        } else {
            long instanceOffset = (long) baseInstance * mPipelineState.getInstanceStride();
            mPipelineState.bindInstanceBuffer((GLBuffer) mActiveInstanceBuffer, instanceOffset);
            glDrawElementsInstancedBaseVertex(mPrimitiveType, indexCount,
                    GL_UNSIGNED_SHORT, baseIndex, instanceCount, baseVertex);
        }
    }
}
