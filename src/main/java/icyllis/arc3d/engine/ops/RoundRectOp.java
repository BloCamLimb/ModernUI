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

package icyllis.arc3d.engine.ops;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.SDFRoundRectGeoProc;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

//TODO
public class RoundRectOp extends MeshDrawOp {

    private GPUBuffer mVertexBuffer;
    private int mBaseVertex;

    private GPUBuffer mInstanceBuffer;
    private int mBaseInstance;

    private float[] mColor;
    private Rect2f mLocalRect;
    private float mCornerRadius;
    private float mStrokeRadius;
    private Matrix mViewMatrix;
    private boolean mStroke;

    private int mNumInstances = 1;

    public RoundRectOp(float[] color, Rect2f localRect, float cornerRadius, float strokeRadius, Matrix viewMatrix,
                       boolean stroke) {
        mColor = color;
        mLocalRect = localRect;
        mCornerRadius = cornerRadius;
        mStrokeRadius = strokeRadius;
        mViewMatrix = viewMatrix;
        mStroke = stroke;
        viewMatrix.mapRect(localRect, this);
    }

    @Override
    protected boolean onMayChain(@Nonnull Op __) {
        var op = (RoundRectOp) __;
        if (op.mStroke == mStroke) {
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
        opsRenderPass.bindBuffers(null, mVertexBuffer, mInstanceBuffer);
        opsRenderPass.drawInstanced(getInstanceCount(), mBaseInstance, getVertexCount(), mBaseVertex);
    }

    @Nonnull
    @Override
    protected @NotNull PipelineInfo onCreatePipelineInfo(SurfaceView writeView, int pipelineFlags) {
        return new PipelineInfo(writeView,
                new SDFRoundRectGeoProc(mStroke), null, null, null,
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
    public void setVertexBuffer(@SharedPtr GPUBuffer buffer, int baseVertex, int actualVertexCount) {
        assert mVertexBuffer == null;
        mVertexBuffer = buffer;
        mBaseVertex = baseVertex;
    }

    @Override
    public void setInstanceBuffer(@SharedPtr GPUBuffer buffer, int baseInstance, int actualInstanceCount) {
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
            var op = (RoundRectOp) it;
            instanceData.putFloat(op.mColor[0]);
            instanceData.putFloat(op.mColor[1]);
            instanceData.putFloat(op.mColor[2]);
            instanceData.putFloat(op.mColor[3]);
            // local rect
            instanceData.putFloat(op.mLocalRect.width() / 2f);
            instanceData.putFloat(op.mLocalRect.centerX());
            instanceData.putFloat(op.mLocalRect.height() / 2f);
            instanceData.putFloat(op.mLocalRect.centerY());
            // radii
            instanceData.putFloat(op.mCornerRadius).putFloat(op.mStrokeRadius);
            op.mViewMatrix.store(MemoryUtil.memAddress(instanceData));
            instanceData.position(instanceData.position() + 36);
        }
    }
}
