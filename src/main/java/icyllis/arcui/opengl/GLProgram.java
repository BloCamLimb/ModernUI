/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.opengl;

import icyllis.arcui.engine.GeometryProcessor;
import icyllis.arcui.opengl.GLUniformHandler.GLSamplerInfo;
import icyllis.arcui.opengl.GLUniformHandler.GLUniformInfo;

import java.util.List;

/**
 * This class manages a GPU program and records per-program information. It also records the vertex
 * and instance attribute layouts that are to be used with the program.
 */
//TODO
public class GLProgram implements AutoCloseable {

    private final GLProgramDataManager mProgramDataManager;

    private GLProgram(GLServer server,
                      int programID,
                      List<GLUniformInfo> uniforms,
                      int uniformSize,
                      List<GLSamplerInfo> textureSamplers,
                      GeometryProcessor.ProgramImpl gpImpl) {
        if (uniforms.isEmpty()) {
            mProgramDataManager = null;
        } else {
            mProgramDataManager = new GLProgramDataManager(uniforms, uniformSize);
        }
    }

    static GLProgram make(GLServer server,
                          int programID,
                          List<GLUniformInfo> uniforms,
                          int uniformSize,
                          List<GLSamplerInfo> textureSamplers,
                          GeometryProcessor.ProgramImpl gpImpl) {
        return new GLProgram(server,
                programID,
                uniforms,
                uniformSize,
                textureSamplers,
                gpImpl);
    }

    @Override
    public void close() {
        if (mProgramDataManager != null) {
            mProgramDataManager.close();
        }
    }

    public void discard() {
    }
}
