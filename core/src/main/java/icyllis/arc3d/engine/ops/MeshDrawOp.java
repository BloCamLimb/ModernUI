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

import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;

/**
 * Base class for mesh-drawing {@link DrawOp}.
 */
public abstract class MeshDrawOp extends DrawOp implements Mesh {

    private PipelineInfo mPipelineInfo;
    private PipelineState mPipelineState;

    public MeshDrawOp() {
    }

    public PipelineInfo getPipelineInfo() {
        return mPipelineInfo;
    }

    public PipelineState getPipelineState() {
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
                             SurfaceProxyView writeView,
                             int pipelineFlags) {
        assert (mPipelineInfo == null);
        mPipelineInfo = onCreatePipelineInfo(writeView, pipelineFlags);
        mPipelineState = context.findOrCreatePipelineState(mPipelineInfo);
    }

    @Override
    public final void onPrepare(OpFlushState state,
                                SurfaceProxyView writeView,
                                int pipelineFlags) {
        if (mPipelineInfo == null) {
            mPipelineInfo = onCreatePipelineInfo(writeView, pipelineFlags);
        }
        if (mPipelineState == null) {
            mPipelineState = state.findOrCreatePipelineState(mPipelineInfo);
        }
        onPrepareDraws(state);
    }

    @Nonnull
    protected abstract PipelineInfo onCreatePipelineInfo(SurfaceProxyView writeView,
                                                         int pipelineFlags);

    protected abstract void onPrepareDraws(MeshDrawTarget target);
}
