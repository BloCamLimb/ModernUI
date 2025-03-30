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

package icyllis.arc3d.engine.trash.ops;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.sketch.Matrix;
import icyllis.arc3d.sketch.Matrixc;
import icyllis.arc3d.granite.geom.AnalyticSimpleBoxStep;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

@Deprecated
public class RectOp extends MeshDrawOp {

    private Buffer mVertexBuffer;
    private int mBaseVertex;

    private Buffer mInstanceBuffer;
    private int mBaseInstance;

    private final int mColor;
    private final Rect2f mLocalRect;
    private final float mStrokeRadius;
    private final float mStrokePos;
    private final Matrix mViewMatrix;
    private final int mGPFlags;

    private int mNumInstances = 1;

    public RectOp(int argb, Rect2f localRect, float strokeRadius, float strokePos,
                  Matrixc viewMatrix, boolean stroke, boolean aa) {
        mColor = argb;
        mLocalRect = localRect;
        mStrokeRadius = strokeRadius;
        mStrokePos = strokePos;
        if (!viewMatrix.isIdentity()) {
            mViewMatrix = new Matrix(viewMatrix);
        } else {
            mViewMatrix = null;
        }
        int gpFlags = 0;
        /*if (aa) {
            gpFlags |= SDFRectGeoProc.FLAG_ANTIALIASING;
        }
        if (stroke) {
            gpFlags |= SDFRectGeoProc.FLAG_STROKE;
        }
        if (mViewMatrix != null) {
            gpFlags |= SDFRectGeoProc.FLAG_INSTANCED_MATRIX;
        }*/
        mGPFlags = gpFlags;
        if (mViewMatrix != null) {
            mViewMatrix.mapRect(localRect, this);
        } else {
            set(localRect);
        }
        setBoundsFlags(aa, false);
    }

    @Override
    protected boolean onMayChain(@NonNull Op __) {
        var op = (RectOp) __;
        if (op.mGPFlags == mGPFlags) {
            mNumInstances++;
            return true;
        }
        return false;
    }

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {
        OpsRenderPass opsRenderPass = state.getOpsRenderPass();
        opsRenderPass.bindPipeline(getPipelineInfo(), getPipelineState(), chainBounds);
        opsRenderPass.bindTextures(null);
        opsRenderPass.bindBuffers(null, Engine.IndexType.kUByte, mVertexBuffer, 0, mInstanceBuffer, 0);
        opsRenderPass.drawInstanced(getInstanceCount(), mBaseInstance, getVertexCount(), mBaseVertex);
    }

    @NonNull
    @Override
    protected GraphicsPipelineDesc_Old onCreatePipelineInfo(ImageProxyView writeView, int pipelineFlags) {
        return new GraphicsPipelineDesc_Old(writeView,
                new AnalyticSimpleBoxStep(false), null, null, null,
                null, pipelineFlags);
    }

    @Override
    public int getVertexCount() {
        return 4;
    }

    @Override
    public int getInstanceCount() {
        return mNumInstances;
    }

    @Override
    public void setVertexBuffer(@SharedPtr Buffer buffer, int baseVertex, int actualVertexCount) {
        assert mVertexBuffer == null;
        mVertexBuffer = buffer;
        mBaseVertex = baseVertex;
    }

    @Override
    public void setInstanceBuffer(@SharedPtr Buffer buffer, int baseInstance, int actualInstanceCount) {
        assert mInstanceBuffer == null;
        mInstanceBuffer = buffer;
        mBaseInstance = baseInstance;
    }

    @Override
    protected void onPrepareDraws(MeshDrawTarget target) {
        ByteBuffer vertexData = target.makeVertexWriter(this);
        if (vertexData == null) {
            return;
        }
        vertexData.putFloat(-1).putFloat(1); // LL
        vertexData.putFloat(1).putFloat(1); // LR
        vertexData.putFloat(-1).putFloat(-1); // UL
        vertexData.putFloat(1).putFloat(-1); // UR
        ByteBuffer instanceData = target.makeInstanceWriter(this);
        if (instanceData == null) {
            return;
        }
        for (Op it = this; it != null; it = it.nextInChain()) {
            var op = (RectOp) it;
            int color = op.mColor;
            instanceData.put((byte) (color >> 16));
            instanceData.put((byte) (color >> 8));
            instanceData.put((byte) (color));
            instanceData.put((byte) (color >> 24));
            // local rect
            instanceData.putFloat(op.mLocalRect.width() / 2f);
            instanceData.putFloat(op.mLocalRect.centerX());
            instanceData.putFloat(op.mLocalRect.height() / 2f);
            instanceData.putFloat(op.mLocalRect.centerY());
            // radii
            //if ((op.mGPFlags & SDFRectGeoProc.FLAG_STROKE) != 0) {
                instanceData.putFloat(op.mStrokeRadius).putFloat(op.mStrokePos);
            //}
            //if ((op.mGPFlags & SDFRectGeoProc.FLAG_INSTANCED_MATRIX) != 0) {
                op.mViewMatrix.store(MemoryUtil.memAddress(instanceData));
                instanceData.position(instanceData.position() + 36);
            //}
        }
    }
}
