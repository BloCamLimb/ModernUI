/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler;

/**
 * Shader capabilities for our DSL compiler.
 */
public class ShaderCaps {

    public TargetApi mTargetApi = TargetApi.OPENGL_4_5;
    public GLSLVersion mGLSLVersion = GLSLVersion.GLSL_450;
    public SPIRVVersion mSPIRVVersion = SPIRVVersion.SPIRV_1_0;

    @Override
    public String toString() {
        return "ShaderCaps{" +
                "mTargetApi=" + mTargetApi +
                ", mGLSLVersion=" + mGLSLVersion +
                ", mSPIRVVersion=" + mSPIRVVersion +
                '}';
    }
}
