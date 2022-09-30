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

import icyllis.akashigi.core.SharedPtr;
import icyllis.akashigi.engine.*;
import icyllis.akashigi.engine.shading.*;

import javax.annotation.Nullable;

import static icyllis.akashigi.opengl.GLCore.*;

public class GLPipelineStateBuilder extends ProgramBuilder {

    private final GLServer mServer;

    private final VaryingHandler mVaryingHandler;
    private final GLUniformHandler mUniformHandler;

    private GLPipelineStateBuilder(GLServer server,
                                   PipelineDesc desc,
                                   PipelineInfo pipelineInfo) {
        super(desc, pipelineInfo);
        mServer = server;
        mVaryingHandler = new VaryingHandler(this);
        mUniformHandler = new GLUniformHandler(this);
    }

    @Nullable
    public static GLPipelineState createPipelineState(GLServer server,
                                                      final PipelineDesc desc,
                                                      final PipelineInfo pipelineInfo) {

        GLPipelineStateBuilder builder = new GLPipelineStateBuilder(server, desc, pipelineInfo);
        if (!builder.emitAndInstallProcs()) {
            return null;
        }
        return builder.build();
    }

    @Nullable
    private GLPipelineState build() {
        int program = glCreateProgram();
        if (program == 0) {
            return null;
        }

        mVaryingHandler.finish();
        String vertSource = mVS.build();
        String fragSource = mFS.build();

        ShaderErrorHandler errorHandler = mServer.getContext().getShaderErrorHandler();

        int frag = glCompileAndAttachShader(program, GL_FRAGMENT_SHADER, fragSource,
                mServer.getPipelineBuilder().getStates(), errorHandler);
        if (frag == 0) {
            glDeleteProgram(program);
            return null;
        }

        int vert = glCompileAndAttachShader(program, GL_VERTEX_SHADER, vertSource,
                mServer.getPipelineBuilder().getStates(), errorHandler);
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

        // the shaders can be detached after the linking
        glDetachShader(program, vert);
        glDetachShader(program, frag);

        glDeleteShader(frag);
        glDeleteShader(vert);

        //TODO debug only, will remove
        String allShaders = String.format("""
                        // Vertex GLSL
                        %s
                        // Fragment GLSL
                        %s
                        """, vertSource, fragSource);
        System.out.println(allShaders);

        @SharedPtr
        GLPipeline pipeline = GLPipeline.make(mServer, mPipelineInfo.geomProc(), program);
        if (pipeline == null) {
            glDeleteProgram(program);
            return null;
        }

        return new GLPipelineState(mServer,
                pipeline,
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
