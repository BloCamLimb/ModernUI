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

import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;

/**
 * Base class for mesh-drawing {@link DrawOp DrawOps}.
 */
public abstract class MeshDrawOp extends DrawOp implements Mesh {

    private PipelineInfo mPipelineInfo;
    private GraphicsPipelineState mPipelineState;

    public MeshDrawOp() {
    }

    public PipelineInfo getPipelineInfo() {
        return mPipelineInfo;
    }

    public GraphicsPipelineState getPipelineState() {
        return mPipelineState;
    }

    @Override
    public int getVertexSize() {
        return mPipelineInfo.geomProc().vertexStride();
    }

    @Override
    public int getInstanceSize() {
        return mPipelineInfo.geomProc().instanceStride();
    }

    @Override
    public void onPrePrepare(RecordingContext context,
                             SurfaceView writeView,
                             int pipelineFlags) {
        assert (mPipelineInfo == null);
        mPipelineInfo = onCreatePipelineInfo(writeView, pipelineFlags);
        mPipelineState = context.findOrCreateGraphicsPipelineState(mPipelineInfo);
    }

    @Override
    public final void onPrepare(OpFlushState state,
                                SurfaceView writeView,
                                int pipelineFlags) {
        if (mPipelineInfo == null) {
            mPipelineInfo = onCreatePipelineInfo(writeView, pipelineFlags);
        }
        if (mPipelineState == null) {
            mPipelineState = state.findOrCreateGraphicsPipelineState(mPipelineInfo);
        }
        onPrepareDraws(state);
    }

    @Nonnull
    protected abstract PipelineInfo onCreatePipelineInfo(SurfaceView writeView,
                                                         int pipelineFlags);

    protected abstract void onPrepareDraws(MeshDrawTarget target);
}
