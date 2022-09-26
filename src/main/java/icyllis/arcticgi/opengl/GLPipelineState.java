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
import icyllis.arcticgi.engine.GeometryProcessor;
import icyllis.arcticgi.engine.shading.UniformHandler;

import java.util.List;

/**
 * This class holds onto a {@link GLPipeline} object that we use for draws. Besides storing the actual
 * {@link GLPipeline} object, this class is also responsible handling all uniforms, buffers, samplers,
 * and other similar objects that are used along with the GLProgram and GLVertexArray in the draw.
 * This includes both allocating and freeing these objects, as well as updating their values.
 */
//TODO set and bind UBO, VBO, IBO, textures
public class GLPipelineState {

    @SharedPtr
    private GLPipeline mPipeline;

    private GLPipelineStateDataManager mDataManager;

    // the installed effects, unique ptr
    private final GeometryProcessor.ProgramImpl mGPImpl;

    GLPipelineState(GLServer server,
                    @SharedPtr GLPipeline pipeline,
                    List<UniformHandler.UniformInfo> uniforms,
                    int uniformSize,
                    List<UniformHandler.UniformInfo> samplers,
                    GeometryProcessor.ProgramImpl gpImpl) {
        mPipeline = pipeline;
        mGPImpl = gpImpl;
        if (!uniforms.isEmpty()) {
            mDataManager = new GLPipelineStateDataManager(uniforms, uniformSize);
        }
    }

    public void discard() {
        mPipeline.discard();
    }

    public void reset() {
        mPipeline = RefCnt.move(mPipeline);
        mDataManager = RefCnt.move(mDataManager);
    }

    public GLPipeline getPipeline() {
        return mPipeline;
    }
}
