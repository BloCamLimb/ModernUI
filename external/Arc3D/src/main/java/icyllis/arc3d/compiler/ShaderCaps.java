/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler;

/**
 * Shader capabilities for our DSL compiler.
 */
public class ShaderCaps {

    public TargetApi mTargetApi;
    public GLSLVersion mGLSLVersion;
    public SPIRVVersion mSPIRVVersion;

    /**
     * GLSL 400 or GLSL 320 ES.
     */
    public boolean mFMASupport = true;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("ShaderCaps:\n");
        dump("", b);
        return b.toString();
    }

    public void dump(String prefix, StringBuilder out) {
        out.append(prefix).append("TargetAPI: ").append(mTargetApi).append('\n');
        out.append(prefix).append("GLSLVersion: ").append(mGLSLVersion).append('\n');
        out.append(prefix).append("SPIRVVersion: ").append(mSPIRVVersion).append('\n');
        out.append(prefix).append("FMASupport: ").append(mFMASupport).append('\n');
    }
}
