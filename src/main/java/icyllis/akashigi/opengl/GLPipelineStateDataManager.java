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

import icyllis.akashigi.core.SLType;
import icyllis.akashigi.engine.UniformDataManager;
import icyllis.akashigi.engine.shading.UniformHandler;

import java.util.List;

/**
 * Uploads a UBO for a Uniform Interface Block with std140 layout.
 */
public class GLPipelineStateDataManager extends UniformDataManager {

    /**
     * Created by {@link GLPipelineState}.
     *
     * @param uniforms    the uniforms
     * @param uniformSize the uniform block size in bytes
     */
    GLPipelineStateDataManager(List<UniformHandler.UniformInfo> uniforms, int uniformSize) {
        super(uniforms.size(), uniformSize);
        for (int i = 0; i < uniforms.size(); i++) {
            UniformHandler.UniformInfo uniformInfo = uniforms.get(i);
            assert ((uniformInfo.mOffset & 0xFFFFFF) == uniformInfo.mOffset);
            assert (SLType.canBeUniformValue(uniformInfo.mVariable.getType()));
            mUniforms[i] = uniformInfo.mOffset | (uniformInfo.mVariable.getType() << 24);
        }
    }

    public void setProjection(int u, int width, int height, boolean flip) {
        if (flip) {
            set4f(u, 2.0f / width, -1.0f, -2.0f / height, 1.0f);
        } else {
            set4f(u, 2.0f / width, -1.0f, 2.0f / height, -1.0f);
        }
    }

    //TODO upload to UBO
}
