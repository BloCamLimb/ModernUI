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

package icyllis.akashigi.engine;

import icyllis.akashigi.core.Rect2i;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Tracks the state across all the GrOps (really just the GrDrawOps) in a OpsTask flush.
 */
public class OpFlushState implements MeshDrawTarget {

    private final Server mServer;

    private OpsRenderPass mOpsRenderPass;

    public OpFlushState(Server server,
                        ResourceProvider resourceProvider) {
        mServer = server;
    }

    public Server getServer() {
        return mServer;
    }

    @Override
    public long makeVertexSpace(Mesh mesh) {
        return mServer.getVertexPool().makeSpace(mesh);
    }

    @Override
    public long makeInstanceSpace(Mesh mesh) {
        return mServer.getInstancePool().makeSpace(mesh);
    }

    @Nullable
    @Override
    public ByteBuffer makeVertexWriter(Mesh mesh) {
        return mServer.getVertexPool().makeWriter(mesh);
    }

    @Nullable
    @Override
    public ByteBuffer makeInstanceWriter(Mesh mesh) {
        return mServer.getInstancePool().makeWriter(mesh);
    }

    public OpsRenderPass getOpsRenderPass() {
        return mOpsRenderPass;
    }

    public OpsRenderPass beginOpsRenderPass(RenderTarget renderTarget,
                                            boolean useStencil,
                                            int origin,
                                            Rect2i bounds,
                                            int colorLoadOp, int colorStoreOp,
                                            int stencilLoadOp, int stencilStoreOp,
                                            float[] clearColor) {
        assert (mOpsRenderPass == null);
        OpsRenderPass opsRenderPass = mServer.getOpsRenderPass(renderTarget,
                useStencil, origin, bounds, colorLoadOp, colorStoreOp, stencilLoadOp, stencilStoreOp, clearColor);
        if (opsRenderPass == null) {
            return null;
        }
        mOpsRenderPass = opsRenderPass;
        opsRenderPass.begin();
        return opsRenderPass;
    }
}
