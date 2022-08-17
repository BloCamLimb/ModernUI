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

import icyllis.arcui.engine.*;
import icyllis.arcui.engine.shading.*;

import javax.annotation.Nullable;

import static icyllis.arcui.opengl.GLCore.*;

public class GLProgramBuilder extends ProgramBuilder {

    private final GLServer mServer;
    private final VaryingHandler mVaryingHandler;
    private final GLUniformHandler mUniformHandler;

    private GLProgramBuilder(GLServer server, ProgramDesc desc, ProgramInfo programInfo) {
        super(desc, programInfo);
        mServer = server;
        mVaryingHandler = new VaryingHandler(this);
        mUniformHandler = new GLUniformHandler(this);
    }

    @Nullable
    static GLProgram createProgram(DirectContext dContext,
                                   final ProgramDesc desc,
                                   final ProgramInfo programInfo) {
        GLServer glServer = (GLServer) dContext.getServer();

        GLProgramBuilder builder = new GLProgramBuilder(glServer, desc, programInfo);
        if (!builder.emitAndInstallProcs()) {
            return null;
        }
        return builder.finish();
    }

    @Nullable
    private GLProgram finish() {
        int program = glCreateProgram();
        if (program == 0) {
            return null;
        }

        varyingHandler().finish();
        String vertSource = mVS.finish(shaderCaps(), EngineTypes.Vertex_ShaderFlag);
        String fragSource = mFS.finish(shaderCaps(), EngineTypes.Fragment_ShaderFlag);

        ShaderErrorHandler errorHandler = mServer.getContext().getShaderErrorHandler();

        int frag = glCompileAndAttachShader(program, GL_FRAGMENT_SHADER, fragSource,
                mServer.getPipelineBuilder().stats(), errorHandler);
        if (frag == 0) {
            glDeleteProgram(program);
            return null;
        }

        int vert = glCompileAndAttachShader(program, GL_VERTEX_SHADER, vertSource,
                mServer.getPipelineBuilder().stats(), errorHandler);
        if (vert == 0) {
            glDeleteProgram(program);
            glDeleteShader(frag);
            return null;
        }

        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            try {
                String allShaders = String.format("""
                        // Vertex GLSL
                        %s
                        // Fragment GLSL
                        %s
                        """, vertSource, fragSource);
                String log = glGetProgramInfoLog(program).trim();
                errorHandler.compileError(allShaders, log);
                return null;
            } finally {
                glDeleteProgram(program);
                glDeleteShader(frag);
                glDeleteShader(vert);
            }
        }

        //TODO remove this and replace with a function
        String allShaders = String.format("""
                        // Vertex GLSL
                        %s
                        // Fragment GLSL
                        %s
                        """, vertSource, fragSource);
        System.out.println(allShaders);

        glDeleteShader(frag);
        glDeleteShader(vert);

        return GLProgram.make(mServer,
                program,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl);
    }

    @Override
    public Caps caps() {
        return mServer.getCaps();
    }

    @Override
    public UniformHandler uniformHandler() {
        return mUniformHandler;
    }

    @Override
    public VaryingHandler varyingHandler() {
        return mVaryingHandler;
    }
}
