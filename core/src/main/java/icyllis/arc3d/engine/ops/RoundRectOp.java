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

package icyllis.arc3d.engine.ops;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.RoundRectGeoProc;
import icyllis.modernui.annotation.NonNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class RoundRectOp extends MeshDrawOp {

    @SharedPtr
    private Buffer mVertexBuffer;
    private int mBaseVertex;

    @SharedPtr
    private Buffer mInstanceBuffer;
    private int mBaseInstance;

    private float[] mColor;
    private Rect2f mLocalRect;
    private float mCornerRadius;
    private float mStrokeRadius;
    private Matrix mViewMatrix;

    public RoundRectOp(float[] color, Rect2f localRect, float cornerRadius, float strokeRadius, Matrix viewMatrix) {
        mColor = color;
        mLocalRect = localRect;
        mCornerRadius = cornerRadius;
        mStrokeRadius = strokeRadius;
        mViewMatrix = viewMatrix;
    }

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {
        OpsRenderPass opsRenderPass = state.getOpsRenderPass();
        opsRenderPass.bindPipeline(getPipelineInfo(), getPipelineState(), chainBounds);
        opsRenderPass.bindTextures(null);
        opsRenderPass.bindBuffers(null, mVertexBuffer, mInstanceBuffer);
        opsRenderPass.drawInstanced(getInstanceCount(), mBaseInstance, getVertexCount(), mBaseVertex);
    }

    @NonNull
    @Override
    protected PipelineInfo onCreatePipelineInfo(SurfaceProxyView writeView, int pipelineFlags) {
        return new PipelineInfo(writeView,
                new RoundRectGeoProc(false), null, null, null,
                null, pipelineFlags);
    }

    @Override
    public int getVertexCount() {
        return 4;
    }

    @Override
    public int getInstanceCount() {
        return 1;
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
        vertexData.putFloat(-1).putFloat(1); // LL
        vertexData.putFloat(1).putFloat(1); // LR
        vertexData.putFloat(-1).putFloat(-1); // UL
        vertexData.putFloat(1).putFloat(-1); // UR
        ByteBuffer instanceData = target.makeInstanceWriter(this);
        instanceData.putFloat(mColor[0]);
        instanceData.putFloat(mColor[1]);
        instanceData.putFloat(mColor[2]);
        instanceData.putFloat(mColor[3]);
        // local rect
        instanceData.putFloat(mLocalRect.width());
        instanceData.putFloat(mLocalRect.centerX());
        instanceData.putFloat(mLocalRect.height());
        instanceData.putFloat(mLocalRect.centerY());
        // radii
        instanceData.putFloat(mCornerRadius).putFloat(mStrokeRadius);
        mViewMatrix.store(MemoryUtil.memAddress(instanceData));
    }

    @Override
    public void onEndFlush() {
        super.onEndFlush();
        mVertexBuffer = Resource.move(mVertexBuffer);
        mInstanceBuffer = Resource.move(mInstanceBuffer);
    }
}
