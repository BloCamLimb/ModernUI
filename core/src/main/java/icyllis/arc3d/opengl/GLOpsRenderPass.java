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

package icyllis.arc3d.opengl;

import icyllis.arc3d.Rect2f;
import icyllis.arc3d.Rect2i;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.SharedPtr;

import static icyllis.arc3d.opengl.GLCore.*;
import static icyllis.arc3d.engine.Engine.*;

public final class GLOpsRenderPass extends OpsRenderPass {

    private final GLEngine mEngine;

    private GLCommandBuffer mCmdBuffer;
    private GLPipelineState mPipelineState;

    private byte mColorOps;
    private byte mStencilOps;
    private float[] mClearColor;

    @SharedPtr
    private Buffer mActiveIndexBuffer;
    @SharedPtr
    private Buffer mActiveVertexBuffer;
    @SharedPtr
    private Buffer mActiveInstanceBuffer;

    private int mPrimitiveType;

    public GLOpsRenderPass(GLEngine engine) {
        mEngine = engine;
    }

    @Override
    protected GLEngine getEngine() {
        return mEngine;
    }

    public GLOpsRenderPass set(RenderTarget fs,
                               Rect2i bounds, int origin,
                               byte colorOps,
                               byte stencilOps,
                               float[] clearColor) {
        set(fs, origin);
        mColorOps = colorOps;
        mStencilOps = stencilOps;
        mClearColor = clearColor;
        return this;
    }

    @Override
    public void begin() {
        super.begin();
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mCmdBuffer = mEngine.beginRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps,
                mClearColor);
    }

    @Override
    public void end() {
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer);
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mEngine.endRenderPass(glRenderTarget,
                mColorOps,
                mStencilOps);
        super.end();
    }

    @Override
    protected boolean onBindPipeline(PipelineInfo pipelineInfo,
                                     PipelineState pipelineState,
                                     Rect2f drawBounds) {
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer);

        mPipelineState = (GLPipelineState) pipelineState;
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
    protected void onBindBuffers(@SharedPtr Buffer indexBuffer,
                                 @SharedPtr Buffer vertexBuffer,
                                 @SharedPtr Buffer instanceBuffer) {
        assert (mPipelineState != null);
        if (mEngine.getCaps().hasBaseInstanceSupport()) {
            mPipelineState.bindBuffers(indexBuffer, vertexBuffer, 0, instanceBuffer, 0);
        } else {
            // bind instance buffer on drawInstanced()
            mPipelineState.bindBuffers(indexBuffer, vertexBuffer, 0, null, 0);
        }
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer, indexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer, vertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer, instanceBuffer);
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
        if (mEngine.getCaps().hasBaseInstanceSupport()) {
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
        if (mEngine.getCaps().hasBaseInstanceSupport()) {
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
