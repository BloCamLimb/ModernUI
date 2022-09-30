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
     * Updates the internal pipeline state for drawing with the provided {@link PipelineInfo}. Enters an
     * internal "bad" state if the pipeline could not be set.
     *
     * @param pipelineInfo the pipeline state
     * @param drawBounds   the draw's sub-area of the render target
     */
    public void bindPipeline(PipelineInfo pipelineInfo, Rect2f drawBounds) {
        assert (pipelineInfo.origin() == mOrigin);

        if (!onBindPipeline(pipelineInfo, drawBounds)) {
            mDrawPipelineStatus = DrawPipelineStatus_FailedToBind;
            return;
        }

        mDrawPipelineStatus = DrawPipelineStatus_Configured;
    }

    /**
     * Binds textures for the geometry processor. Texture bindings are dynamic state and therefore
     * not set during {@link #bindPipeline(PipelineInfo, Rect2f)}. If the current program uses textures,
     * then the client must call this method before drawing. The geometry processor textures may also
     * be updated between draws by calling this method again with a different array for textures.
     * <p>
     * Note that this method is only used for GP using texture. If GP does not use texture but FP does,
     * they will be automatically set during {@link #bindPipeline(PipelineInfo, Rect2f)}, and this is
     * a no-op. Otherwise, this method must be called if the GP uses textures.
     * <p>
     * Currently, we do not use multitexturing on GP, but we may use multitexturing on FP.
     *
     * @param geomTexture the raw ptr to texture at binding 0, nonnull
     */
    public final void bindTextures(TextureProxy geomTexture) {

    }

    /**
     * Binds geometric buffers to current command buffer.
     *
     * @param indexBuffer    the index buffer if using indexed rendering, or null
     * @param vertexBuffer   the vertex buffer, nonnull
     * @param instanceBuffer the instance buffer if using instanced rendering, or null
     */
    public final void bindBuffers(@SharedPtr GBuffer indexBuffer,
                                  @SharedPtr GBuffer vertexBuffer,
                                  @SharedPtr GBuffer instanceBuffer) {
        if (vertexBuffer == null) {
            mDrawPipelineStatus = DrawPipelineStatus_FailedToBind;
            return;
        }
        if (mDrawPipelineStatus == DrawPipelineStatus_Configured) {
            onBindBuffers(indexBuffer, vertexBuffer, instanceBuffer);
        } else {
            assert (mDrawPipelineStatus == DrawPipelineStatus_FailedToBind);
        }
    }

    /**
     * Records a non-indexed draw to current command buffer.
     *
     * @param vertexCount the number of vertices to draw
     * @param baseVertex  the index of the first vertex to draw
     */
    public final void draw(int vertexCount, int baseVertex) {
        if (mDrawPipelineStatus == DrawPipelineStatus_Configured) {
            onDraw(vertexCount, baseVertex);
            getServer().getStats().incNumDraws();
        } else {
            assert (mDrawPipelineStatus == DrawPipelineStatus_FailedToBind);
            getServer().getStats().incNumFailedDraws();
        }
    }

    /**
     * Records an indexed draw to current command buffer.
     *
     * @param indexCount the number of vertices to draw
     * @param baseIndex  the base index within the index buffer
     * @param baseVertex the value added to the vertex index before indexing into the vertex buffer
     */
    public final void drawIndexed(int indexCount, int baseIndex,
                                  int baseVertex) {
        if (mDrawPipelineStatus == DrawPipelineStatus_Configured) {
            onDrawIndexed(indexCount, baseIndex, baseVertex);
            getServer().getStats().incNumDraws();
        } else {
            assert (mDrawPipelineStatus == DrawPipelineStatus_FailedToBind);
            getServer().getStats().incNumFailedDraws();
        }
    }

    /**
     * Records a non-indexed draw to current command buffer.
     *
     * @param instanceCount the number of instances to draw
     * @param baseInstance  the instance ID of the first instance to draw
     * @param vertexCount   the number of vertices to draw
     * @param baseVertex    the index of the first vertex to draw
     */
    public final void drawInstanced(int instanceCount, int baseInstance,
                                    int vertexCount, int baseVertex) {
        if (mDrawPipelineStatus == DrawPipelineStatus_Configured) {
            onDrawInstanced(instanceCount, baseInstance, vertexCount, baseVertex);
            getServer().getStats().incNumDraws();
        } else {
            assert (mDrawPipelineStatus == DrawPipelineStatus_FailedToBind);
            getServer().getStats().incNumFailedDraws();
        }
    }

    /**
     * Records an indexed draw to current command buffer.
     *
     * @param indexCount    the number of vertices to draw
     * @param baseIndex     the base index within the index buffer
     * @param instanceCount the number of instances to draw
     * @param baseInstance  the instance ID of the first instance to draw
     * @param baseVertex    the value added to the vertex index before indexing into the vertex buffer
     */
    public final void drawIndexedInstanced(int indexCount, int baseIndex,
                                           int instanceCount, int baseInstance,
                                           int baseVertex) {
        if (mDrawPipelineStatus == DrawPipelineStatus_Configured) {
            onDrawIndexedInstanced(indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
            getServer().getStats().incNumDraws();
        } else {
            assert (mDrawPipelineStatus == DrawPipelineStatus_FailedToBind);
            getServer().getStats().incNumFailedDraws();
        }
    }

    protected void set(RenderTarget rt, int origin) {
        assert (mRenderTarget == null);
        mRenderTarget = rt;
        mOrigin = origin;
    }

    protected abstract Server getServer();

    protected abstract boolean onBindPipeline(PipelineInfo pipelineInfo, Rect2f drawBounds);

    protected abstract void onBindBuffers(@SharedPtr GBuffer indexBuffer,
                                          @SharedPtr GBuffer vertexBuffer,
                                          @SharedPtr GBuffer instanceBuffer);

    protected abstract void onDraw(int vertexCount, int baseVertex);

    protected abstract void onDrawIndexed(int indexCount, int baseIndex,
                                          int baseVertex);

    protected abstract void onDrawInstanced(int instanceCount, int baseInstance,
                                            int vertexCount, int baseVertex);

    protected abstract void onDrawIndexedInstanced(int indexCount, int baseIndex,
                                                   int instanceCount, int baseInstance,
                                                   int baseVertex);
}
