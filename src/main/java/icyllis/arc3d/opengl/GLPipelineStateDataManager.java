/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.SLType;
import icyllis.arc3d.engine.UniformDataManager;
import icyllis.arc3d.engine.shading.UniformHandler;

import java.util.List;

/**
 * Uploads a UBO for a Uniform Interface Block with std140 layout.
 */
public class GLPipelineStateDataManager extends UniformDataManager {

    private int mRTWidth;
    private int mRTHeight;
    private boolean mRTFlipY;

    /**
     * Created by {@link GLPipelineState}.
     *
     * @param uniforms    the uniforms
     * @param uniformSize the uniform block size in bytes
     */
    GLPipelineStateDataManager(List<UniformHandler.UniformInfo> uniforms, int uniformSize) {
        super(uniforms.size(), uniformSize);
        for (int i = 0, e = uniforms.size(); i < e; i++) {
            UniformHandler.UniformInfo uniformInfo = uniforms.get(i);
            assert ((uniformInfo.mOffset & 0xFFFFFF) == uniformInfo.mOffset);
            assert (MathUtil.isAlign4(uniformInfo.mOffset));
            assert (SLType.canBeUniformValue(uniformInfo.mVariable.getType()));
            mUniforms[i] = uniformInfo.mOffset | (uniformInfo.mVariable.getType() << 24);
        }
    }

    /**
     * Set the orthographic projection vector.
     */
    public void setProjection(int u, int width, int height, boolean flipY) {
        if (width != mRTWidth || height != mRTHeight || flipY != mRTFlipY) {
            if (flipY) {
                set4f(u, 2.0f / width, -1.0f, -2.0f / height, 1.0f);
            } else {
                set4f(u, 2.0f / width, -1.0f, 2.0f / height, -1.0f);
            }
            mRTWidth = width;
            mRTHeight = height;
            mRTFlipY = flipY;
        }
    }

    //TODO upload to UBO
}
