/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

import icyllis.arcticgi.core.RefCnt;
import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.EngineTypes;
import icyllis.arcticgi.engine.ProgramInfo;

import static icyllis.arcticgi.opengl.GLCore.*;

/**
 * The main command buffer of the OpenGL context.
 */
public class GLCommandBuffer {

    private static final int
            TriState_Disabled = 0,
            TriState_Enabled = 1,
            TriState_Undefined = 2;

    private final GLServer mServer;

    private int mCachedViewportWidth;
    private int mCachedViewportHeight;

    private int mCachedScissorTest;

    private int mCachedScissorX;
    private int mCachedScissorY;
    private int mCachedScissorWidth;
    private int mCachedScissorHeight;

    private int mCachedFramebufferID;
    private int mCachedRenderTargetUniqueID;

    @SharedPtr
    private GLPipeline mCachedPipeline;
    private int mCachedProgram;
    private int mCachedVertexArray;

    GLCommandBuffer(GLServer server) {
        mServer = server;
    }

    /**
     * Flush viewport.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     */
    public void flushViewport(int width, int height) {
        assert (width >= 0 && height >= 0);
        if (width != mCachedViewportWidth || height != mCachedViewportHeight) {
            glViewportIndexedf(0, 0.0f, 0.0f, width, height);
            glDepthRangeIndexed(0, 0.0f, 1.0f);
            mCachedViewportWidth = width;
            mCachedViewportHeight = height;
        }
    }

    /**
     * Flush scissor.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     * @param origin the surface origin
     * @see EngineTypes#SurfaceOrigin_TopLeft
     * @see EngineTypes#SurfaceOrigin_BottomLeft
     */
    public void flushScissorRect(int width, int height, int origin,
                                 int scissorLeft, int scissorTop,
                                 int scissorRight, int scissorBottom) {
        assert (width >= 0 && height >= 0);
        final int scissorWidth = scissorRight - scissorLeft;
        final int scissorHeight = scissorBottom - scissorTop;
        assert (scissorLeft >= 0 && scissorTop >= 0 &&
                scissorWidth >= 0 && scissorWidth <= width &&
                scissorHeight >= 0 && scissorHeight <= height);
        final int scissorY;
        if (origin == EngineTypes.SurfaceOrigin_TopLeft) {
            scissorY = scissorTop;
        } else {
            assert (origin == EngineTypes.SurfaceOrigin_BottomLeft);
            scissorY = height - scissorBottom;
        }
        assert (scissorY >= 0);
        if (scissorLeft != mCachedScissorX ||
                scissorY != mCachedScissorY ||
                scissorWidth != mCachedScissorWidth ||
                scissorHeight != mCachedScissorHeight) {
            glScissorIndexed(0, scissorLeft, scissorY,
                    scissorWidth, scissorHeight);
            mCachedScissorX = scissorLeft;
            mCachedScissorY = scissorY;
            mCachedScissorWidth = scissorWidth;
            mCachedScissorHeight = scissorHeight;
        }
    }

    /**
     * Flush scissor test.
     *
     * @param enabled whether to enable scissor test
     */
    public void flushScissorTest(boolean enabled) {
        if (enabled) {
            if (mCachedScissorTest != TriState_Enabled) {
                glEnablei(GL_SCISSOR_TEST, 0);
                mCachedScissorTest = TriState_Enabled;
            }
        } else {
            if (mCachedScissorTest != TriState_Disabled) {
                glDisablei(GL_SCISSOR_TEST, 0);
                mCachedScissorTest = TriState_Disabled;
            }
        }
    }

    /**
     * Bind raw framebuffer and flush render target to be invalid.
     */
    public void bindFramebuffer(int framebuffer) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        mCachedRenderTargetUniqueID = 0;
    }

    /**
     * Flush framebuffer and viewport at the same time.
     *
     * @param target raw ptr to render target
     */
    public void flushRenderTarget(GLRenderTarget target) {
        if (target == null) {
            mCachedRenderTargetUniqueID = 0;
        } else {
            flushRenderTarget(target, target.getSampleCount() > 1);
        }
    }

    /**
     * Flush framebuffer and viewport at the same time.
     *
     * @param target  raw ptr to render target
     * @param useMSAA whether to use multisample target
     */
    public void flushRenderTarget(GLRenderTarget target, boolean useMSAA) {
        int framebuffer = target.getFramebuffer(useMSAA);
        if (mCachedFramebufferID != framebuffer ||
                mCachedRenderTargetUniqueID != target.getUniqueID()) {
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            mCachedFramebufferID = framebuffer;
            mCachedRenderTargetUniqueID = target.getUniqueID();
            flushViewport(target.getWidth(), target.getHeight());
        }
        target.bindStencil(useMSAA);
    }

    @SuppressWarnings("AssertWithSideEffects")
    public boolean flushPipeline(GLRenderTarget target, ProgramInfo programInfo) {
        GLPipelineState pipelineState = mServer.getPipelineBuilder().findOrCreatePipelineState(programInfo);
        if (pipelineState == null) {
            return false;
        }
        GLPipeline pipeline = pipelineState.getPipeline();
        if (mCachedPipeline != pipeline) {
            // active program will not be deleted, so no collision
            assert (pipeline.getProgram() != mCachedProgram);
            assert (pipeline.getVertexArray() != mCachedVertexArray);
            glUseProgram(pipeline.getProgram());
            glBindVertexArray(pipeline.getVertexArray());
            mCachedPipeline = RefCnt.assign(mCachedPipeline, pipeline);
            assert ((mCachedProgram = pipeline.getProgram()) != 0);
            assert ((mCachedVertexArray = pipeline.getVertexArray()) != 0);
        }

        flushRenderTarget(target);
        return true;
    }
}
