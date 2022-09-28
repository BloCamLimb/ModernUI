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

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.Rect2f;
import icyllis.akashigi.core.Rect2i;
import icyllis.akashigi.engine.*;

public class GLOpsRenderPass extends OpsRenderPass {

    private final GLServer mServer;

    private GLCommandBuffer mCmdBuffer;

    private int mColorLoadOp;
    private int mColorStoreOp;
    private int mStencilLoadOp;
    private int mStencilStoreOp;
    private float[] mClearColor;

    public GLOpsRenderPass(GLServer server) {
        mServer = server;
    }

    @Override
    protected Server getServer() {
        return mServer;
    }

    public GLOpsRenderPass set(RenderTarget rt,
                               Rect2i bounds, int origin,
                               int colorLoadOp, int colorStoreOp,
                               int stencilLoadOp, int stencilStoreOp,
                               float[] clearColor) {
        set(rt, origin);
        mColorLoadOp = colorLoadOp;
        mColorStoreOp = colorStoreOp;
        mStencilLoadOp = stencilLoadOp;
        mStencilStoreOp = stencilStoreOp;
        mClearColor = clearColor;
        return this;
    }

    @Override
    public void begin() {
        super.begin();
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mCmdBuffer = mServer.beginRenderPass(glRenderTarget, mColorLoadOp, mStencilLoadOp, mClearColor);
    }

    @Override
    public void end() {
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mServer.endRenderPass(glRenderTarget, mColorStoreOp, mStencilStoreOp);
        super.end();
    }

    @Override
    protected boolean onBindPipeline(ProgramInfo programInfo, Rect2f drawBounds) {
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        return mCmdBuffer.flushPipeline(glRenderTarget, programInfo);
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
}
