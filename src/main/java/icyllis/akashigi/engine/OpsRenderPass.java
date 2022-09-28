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

import icyllis.akashigi.core.Rect2f;
import icyllis.akashigi.core.SharedPtr;
import icyllis.akashigi.engine.ops.Op;

/**
 * The {@link OpsRenderPass} is a series of commands (draws, clears, and discards), which all target the
 * same render target. It is possible that these commands execute immediately (OpenGL), or get buffered
 * up for later execution (Vulkan). {@link Op Ops} execute into a {@link OpsRenderPass}.
 */
//TODO
public abstract class OpsRenderPass {

    /**
     * DrawPipelineStatus.
     */
    private static final int
            DrawPipelineStatus_NotConfigured = 0,
            DrawPipelineStatus_Configured = 1,
            DrawPipelineStatus_FailedToBind = 2;

    private int mDrawPipelineStatus = DrawPipelineStatus_NotConfigured;

    protected RenderTarget mRenderTarget;
    protected int mOrigin;

    // Backends may defer binding of certain buffers if their draw API requires a buffer, or if
    // their bind methods don't support base values.
    @SharedPtr
    protected Buffer mActiveIndexBuffer;
    @SharedPtr
    protected Buffer mActiveVertexBuffer;
    @SharedPtr
    protected Buffer mActiveInstanceBuffer;

    public OpsRenderPass() {
        this(null, Engine.SurfaceOrigin_UpperLeft);
    }

    public OpsRenderPass(RenderTarget rt, int origin) {
        mRenderTarget = rt;
        mOrigin = origin;
    }

    public void begin() {
        mDrawPipelineStatus = DrawPipelineStatus_NotConfigured;
    }

    public void end() {
        resetActiveBuffers();
    }

    /**
     * Clear the owned render target. Clears the full target if 'scissor' is disabled, otherwise it
     * is restricted to 'scissor'.
     */
    public void clearColor(int left, int top, int right, int bottom,
                           float red, float green, float blue, float alpha) {
        assert (mRenderTarget != null);
        mDrawPipelineStatus = DrawPipelineStatus_NotConfigured;
    }

    /**
     * Same as {@link #clearColor} but modifies the stencil.
     */
    public void clearStencil(int left, int top, int right, int bottom, boolean insideMask) {
        assert (mRenderTarget != null);
        mDrawPipelineStatus = DrawPipelineStatus_NotConfigured;
    }

    /**
     * Updates the internal pipeline state for drawing with the provided {@link ProgramInfo}. Enters an
     * internal "bad" state if the pipeline could not be set.
     *
     * @param programInfo the pipeline state
     * @param drawBounds  the draw's sub-area of the render target
     */
    public void bindPipeline(ProgramInfo programInfo, Rect2f drawBounds) {
        assert (programInfo.origin() == mOrigin);

        resetActiveBuffers();

        if (!onBindPipeline(programInfo, drawBounds)) {
            mDrawPipelineStatus = DrawPipelineStatus_FailedToBind;
            return;
        }

        mDrawPipelineStatus = DrawPipelineStatus_Configured;
    }

    protected void set(RenderTarget rt, int origin) {
        assert (mRenderTarget == null);
        mRenderTarget = rt;
        mOrigin = origin;
    }

    protected abstract Server getServer();

    protected abstract boolean onBindPipeline(ProgramInfo programInfo, Rect2f drawBounds);

    private void resetActiveBuffers() {
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer);
    }
}
