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

package icyllis.arctic.opengl;

import icyllis.arctic.core.SLType;
import icyllis.arctic.engine.UniformDataManager;
import icyllis.arctic.opengl.GLUniformHandler.GLUniformInfo;

import java.util.List;

/**
 * Uploads a UBO for a Uniform Interface Block with std140 layout.
 */
public class GLPipelineStateDataManager extends UniformDataManager {

    /**
     * Created by {@link GLPipeline}.
     *
     * @param uniforms    the uniforms
     * @param uniformSize the uniform block size in bytes
     */
    GLPipelineStateDataManager(List<GLUniformInfo> uniforms, int uniformSize) {
        super(uniforms.size(), uniformSize);
        assert !uniforms.isEmpty();
        for (int i = 0; i < uniforms.size(); i++) {
            GLUniformInfo uniformInfo = uniforms.get(i);
            assert ((uniformInfo.mOffset & 0xFFFFFF) == uniformInfo.mOffset);
            assert (SLType.canBeUniformValue(uniformInfo.mVariable.getType()));
            mUniforms[i] = uniformInfo.mOffset | (uniformInfo.mVariable.getType() << 24);
        }
    }

    //TODO upload to UBO
}
