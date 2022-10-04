/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine.ops;

import icyllis.akashigi.core.Rect2f;
import icyllis.akashigi.engine.*;
import icyllis.akashigi.engine.geom.RoundRectProcessor;

//TODO
public class RoundRectOp extends MeshDrawOp {

    private Buffer mVertexBuffer;
    private int mBaseVertex;

    private Buffer mInstanceBuffer;
    private int mBaseInstance;

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {
        OpsRenderPass opsRenderPass = state.getOpsRenderPass();
        opsRenderPass.bindPipeline(getPipelineInfo(), chainBounds);
        opsRenderPass.bindTextures(null);
        opsRenderPass.bindBuffers(null, mVertexBuffer, mInstanceBuffer);
        opsRenderPass.drawInstanced(getInstanceCount(), mBaseInstance, getVertexCount(), mBaseVertex);
    }

    @Override
    protected PipelineInfo onCreatePipelineInfo(SurfaceProxyView writeView,
                                                int pipelineFlags) {
        return new PipelineInfo(writeView,
                new RoundRectProcessor(false),
                null,
                null,
                null,
                null,
                pipelineFlags);
    }

    @Override
    public int getVertexCount() {
        return 4;
    }

    @Override
    public int getVertexSize() {
        return getPipelineInfo().geomProc().vertexStride();
    }

    @Override
    public int getInstanceCount() {
        return 1;
    }

    @Override
    public int getInstanceSize() {
        return getPipelineInfo().geomProc().instanceStride();
    }

    @Override
    public void setVertexBuffer(Buffer buffer, int baseVertex, int actualVertexCount) {
        mVertexBuffer = buffer;
        mBaseVertex = baseVertex;
    }

    @Override
    public void setInstanceBuffer(Buffer buffer, int baseInstance, int actualInstanceCount) {
        mInstanceBuffer = buffer;
        mBaseInstance = baseInstance;
    }

    @Override
    protected void onPrepareDraws(MeshDrawTarget target) {
        long vertexData = target.makeVertexSpace(this);
        long instanceData = target.makeInstanceSpace(this);
    }
}
