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

import icyllis.arc3d.Rect2f;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.RoundRectGeoProc;
import icyllis.modernui.annotation.NonNull;

public class RoundRectOp extends MeshDrawOp {

    private Buffer mVertexBuffer;
    private int mBaseVertex;

    private Buffer mInstanceBuffer;
    private int mBaseInstance;

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
