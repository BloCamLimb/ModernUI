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

import icyllis.akashigi.core.RefCnt;
import icyllis.akashigi.core.SharedPtr;
import icyllis.akashigi.engine.*;

import static icyllis.akashigi.engine.Engine.*;
import static icyllis.akashigi.opengl.GLCore.*;

/**
 * The main command buffer of OpenGL context. The commands executed on {@link GLCommandBuffer} are
 * mostly the same as that on {@link GLServer}, but {@link GLCommandBuffer} assumes some values
 * and will not handle dirty context.
 *
 * @see GLServer#beginRenderPass(GLRenderTarget, int, int, float[])
 */
public class GLCommandBuffer {

    private static final int
            TriState_Disabled = 0,
            TriState_Enabled = 1,
            TriState_Unknown = 2;

    private final GLServer mServer;

    private int mHWViewportWidth;
    private int mHWViewportHeight;

    private int mHWScissorTest;
    private int mHWColorWrite;

    private int mHWScissorX;
    private int mHWScissorY;
    private int mHWScissorWidth;
    private int mHWScissorHeight;

    private int mHWFramebuffer;
    private GLRenderTarget mHWRenderTarget;

    @SharedPtr
    private GLPipeline mHWPipeline;
    private int mHWProgram;
    private int mHWVertexArray;

    GLCommandBuffer(GLServer server) {
        mServer = server;
    }

    void resetStates(int states) {
        if ((states & GLBackendState_RenderTarget) != 0) {
            mHWFramebuffer = 0;
            mHWRenderTarget = null;
        }

        if ((states & GLBackendState_Pipeline) != 0) {
            mHWPipeline = RefCnt.move(mHWPipeline);
            mHWProgram = 0;
            mHWVertexArray = 0;
        }

        if ((states & GLBackendState_View) != 0) {
            mHWScissorTest = TriState_Unknown;
            mHWScissorX = -1;
            mHWScissorY = -1;
            mHWScissorWidth = -1;
            mHWScissorHeight = -1;
            mHWViewportWidth = -1;
            mHWViewportHeight = -1;
        }

        if ((states & GLBackendState_Misc) != 0) {
            mHWColorWrite = TriState_Unknown;
        }
    }

    /**
     * Flush viewport.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     */
    public void flushViewport(int width, int height) {
        assert (width >= 0 && height >= 0);
        if (width != mHWViewportWidth || height != mHWViewportHeight) {
            glViewportIndexedf(0, 0.0f, 0.0f, width, height);
            glDepthRangeIndexed(0, 0.0f, 1.0f);
            mHWViewportWidth = width;
            mHWViewportHeight = height;
        }
    }

    /**
     * Flush scissor.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     * @param origin the surface origin
     * @see Engine#SurfaceOrigin_UpperLeft
     * @see Engine#SurfaceOrigin_LowerLeft
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
        if (origin == SurfaceOrigin_UpperLeft) {
            scissorY = scissorTop;
        } else {
            assert (origin == SurfaceOrigin_LowerLeft);
            scissorY = height - scissorBottom;
        }
        assert (scissorY >= 0);
        if (scissorLeft != mHWScissorX ||
                scissorY != mHWScissorY ||
                scissorWidth != mHWScissorWidth ||
                scissorHeight != mHWScissorHeight) {
            glScissorIndexed(0, scissorLeft, scissorY,
                    scissorWidth, scissorHeight);
            mHWScissorX = scissorLeft;
            mHWScissorY = scissorY;
            mHWScissorWidth = scissorWidth;
            mHWScissorHeight = scissorHeight;
        }
    }

    /**
     * Flush scissor test.
     *
     * @param enable whether to enable scissor test
     */
    public void flushScissorTest(boolean enable) {
        if (enable) {
            if (mHWScissorTest != TriState_Enabled) {
                glEnablei(GL_SCISSOR_TEST, 0);
                mHWScissorTest = TriState_Enabled;
            }
        } else {
            if (mHWScissorTest != TriState_Disabled) {
                glDisablei(GL_SCISSOR_TEST, 0);
                mHWScissorTest = TriState_Disabled;
            }
        }
    }

    /**
     * Flush color mask for draw buffer 0.
     *
     * @param enable whether to write color
     */
    public void flushColorWrite(boolean enable) {
        if (enable) {
            if (mHWColorWrite != TriState_Enabled) {
                glColorMaski(0, true, true, true, true);
                mHWColorWrite = TriState_Enabled;
            }
        } else {
            if (mHWColorWrite != TriState_Disabled) {
                glColorMaski(0, false, false, false, false);
                mHWColorWrite = TriState_Disabled;
            }
        }
    }

    /**
     * Bind raw framebuffer and flush render target to be invalid.
     */
    public void bindFramebuffer(int framebuffer) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        mHWRenderTarget = null;
    }

    /**
     * Flush framebuffer and viewport at the same time.
     *
     * @param target raw ptr to render target
     */
    public void flushRenderTarget(GLRenderTarget target) {
        if (target == null) {
            mHWRenderTarget = null;
        } else {
            int framebuffer = target.getFramebuffer();
            if (mHWFramebuffer != framebuffer ||
                    mHWRenderTarget != target) {
                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
                mHWFramebuffer = framebuffer;
                mHWRenderTarget = target;
                flushViewport(target.getWidth(), target.getHeight());
            }
            target.bindStencil();
        }
    }

    @SuppressWarnings("AssertWithSideEffects")
    public boolean flushPipeline(GLRenderTarget target, ProgramInfo programInfo) {
        GLPipelineState pipelineState = mServer.getPipelineBuilder().findOrCreatePipelineState(programInfo);
        if (pipelineState == null) {
            return false;
        }
        GLPipeline pipeline = pipelineState.getPipeline();
        if (mHWPipeline != pipeline) {
            // active program will not be deleted, so no collision
            assert (pipeline.getProgram() != mHWProgram);
            assert (pipeline.getVertexArray() != mHWVertexArray);
            glUseProgram(pipeline.getProgram());
            glBindVertexArray(pipeline.getVertexArray());
            mHWPipeline = RefCnt.create(mHWPipeline, pipeline);
            assert ((mHWProgram = pipeline.getProgram()) != 0);
            assert ((mHWVertexArray = pipeline.getVertexArray()) != 0);
        }

        flushRenderTarget(target);
        return true;
    }
}
