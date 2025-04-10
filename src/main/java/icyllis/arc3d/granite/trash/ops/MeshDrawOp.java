/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.trash.ops;

import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.granite.trash.Mesh;
import icyllis.arc3d.granite.trash.MeshDrawTarget;
import icyllis.arc3d.granite.trash.OpFlushState;
import icyllis.arc3d.granite.RecordingContext;
import org.jspecify.annotations.NonNull;

/**
 * Base class for mesh-drawing {@link DrawOp DrawOps}.
 */
@Deprecated
public abstract class MeshDrawOp extends DrawOp implements Mesh {

    private GraphicsPipelineDesc_Old mGraphicsPipelineDesc;
    private GraphicsPipeline mPipelineState;

    public MeshDrawOp() {
    }

    public GraphicsPipelineDesc_Old getPipelineInfo() {
        return mGraphicsPipelineDesc;
    }

    public GraphicsPipeline getPipelineState() {
        return mPipelineState;
    }

    @Override
    public int getVertexSize() {
        return mGraphicsPipelineDesc.geomProc().vertexStride();
    }

    @Override
    public int getInstanceSize() {
        return mGraphicsPipelineDesc.geomProc().instanceStride();
    }

    @Override
    public void onPrePrepare(RecordingContext context,
                             ImageProxyView writeView,
                             int pipelineFlags) {
        assert (mGraphicsPipelineDesc == null);
        mGraphicsPipelineDesc = onCreatePipelineInfo(writeView, pipelineFlags);
        //mPipelineState = context.findOrCreateGraphicsPipeline(mGraphicsPipelineDesc);
    }

    @Override
    public final void onPrepare(OpFlushState state,
                                ImageProxyView writeView,
                                int pipelineFlags) {
        if (mGraphicsPipelineDesc == null) {
            mGraphicsPipelineDesc = onCreatePipelineInfo(writeView, pipelineFlags);
        }
        if (mPipelineState == null) {
            mPipelineState = state.findOrCreateGraphicsPipeline(mGraphicsPipelineDesc);
        }
        onPrepareDraws(state);
    }

    @NonNull
    protected abstract GraphicsPipelineDesc_Old onCreatePipelineInfo(ImageProxyView writeView,
                                                                     int pipelineFlags);

    protected abstract void onPrepareDraws(MeshDrawTarget target);
}
