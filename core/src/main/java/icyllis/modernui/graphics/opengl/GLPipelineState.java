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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.graphics.RefCnt;
import icyllis.modernui.graphics.SharedPtr;
import icyllis.modernui.graphics.engine.*;
import icyllis.modernui.graphics.engine.shading.UniformHandler;

import java.util.List;

/**
 * This class holds onto a {@link GLPipeline} object that we use for draws. Besides storing the actual
 * {@link GLPipeline} object, this class is also responsible handling all uniforms, buffers, samplers,
 * and other similar objects that are used along with the GLProgram and GLVertexArray in the draw.
 * This includes both allocating and freeing these objects, as well as updating their values.
 */
//TODO set and bind textures
public class GLPipelineState {

    @SharedPtr
    private GLPipeline mPipeline;

    private GLPipelineStateDataManager mDataManager;

    // the installed effects, unique ptr
    private final GeometryProcessor.ProgramImpl mGPImpl;

    private final int mNumTextureSamplers;

    GLPipelineState(GLServer server,
                    @SharedPtr GLPipeline pipeline,
                    List<UniformHandler.UniformInfo> uniforms,
                    int uniformSize,
                    List<UniformHandler.UniformInfo> samplers,
                    GeometryProcessor.ProgramImpl gpImpl) {
        mPipeline = pipeline;
        mGPImpl = gpImpl;
        mDataManager = new GLPipelineStateDataManager(uniforms, uniformSize);
        mNumTextureSamplers = samplers.size();
    }

    public void discard() {
        mPipeline.discard();
    }

    public void destroy() {
        mPipeline = RefCnt.move(mPipeline);
        mDataManager = RefCnt.move(mDataManager);
    }

    public void bindPipeline(GLCommandBuffer commandBuffer) {
        commandBuffer.bindPipeline(mPipeline);
    }

    public void bindUniforms(GLCommandBuffer commandBuffer,
                             PipelineInfo pipelineInfo,
                             int width, int height) {
        mDataManager.setProjection(0, width, height,
                pipelineInfo.origin() == Engine.SurfaceOrigin.kLowerLeft);
        mGPImpl.setData(mDataManager, pipelineInfo.geomProc());
        //TODO FP and upload
    }

    /**
     * Binds all geometry processor and fragment processor textures.
     */
    public boolean bindTextures(GLCommandBuffer commandBuffer,
                                PipelineInfo pipelineInfo,
                                TextureProxy[] geomTextures) {
        int nextTexSamplerIdx = 0;
        for (int i = 0, e = pipelineInfo.geomProc().numTextureSamplers(); i < e; i++) {
            GLTexture texture = (GLTexture) geomTextures[i].peekTexture();
            if (!commandBuffer.bindTexture(texture, nextTexSamplerIdx++,
                    pipelineInfo.geomProc().textureSamplerState(i))) {
                return false;
            }
        }
        //TODO bind FP textures

        assert nextTexSamplerIdx == mNumTextureSamplers;
        return true;
    }

    /**
     * Binds all geometric buffers.
     */
    public void bindBuffers(Buffer indexBuffer,
                            Buffer vertexBuffer,
                            Buffer instanceBuffer) {
        if (indexBuffer instanceof GLBuffer glIndexBuffer) {
            mPipeline.bindIndexBuffer(glIndexBuffer);
        }
        if (vertexBuffer instanceof GLBuffer glVertexBuffer) {
            mPipeline.bindVertexBuffer(glVertexBuffer, 0);
        }
        if (instanceBuffer instanceof GLBuffer glInstanceBuffer) {
            mPipeline.bindInstanceBuffer(glInstanceBuffer, 0);
        }
    }
}
