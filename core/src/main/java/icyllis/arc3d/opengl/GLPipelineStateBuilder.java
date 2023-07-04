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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.*;
import icyllis.arc3d.SharedPtr;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;

import static icyllis.arc3d.opengl.GLCore.*;

public class GLPipelineStateBuilder extends ProgramBuilder {

    private final GLEngine mEngine;

    private final VaryingHandler mVaryingHandler;
    private final GLUniformHandler mUniformHandler;

    private String mVertSource;
    private String mFragSource;

    private GLPipelineStateBuilder(GLEngine engine,
                                   PipelineDesc desc,
                                   PipelineInfo pipelineInfo) {
        super(desc, pipelineInfo);
        mEngine = engine;
        mVaryingHandler = new VaryingHandler(this);
        mUniformHandler = new GLUniformHandler(this);
    }

    @Nonnull
    public static GLPipelineState createPipelineState(final GLEngine engine,
                                                      final PipelineDesc desc,
                                                      final PipelineInfo pipelineInfo) {
        return new GLPipelineState(engine, CompletableFuture.supplyAsync(() -> {
            GLPipelineStateBuilder builder = new GLPipelineStateBuilder(engine, desc, pipelineInfo);
            builder.buildAsync();
            return builder;
        }));
    }

    private void buildAsync() {
        if (!emitAndInstallProcs()) {
            return;
        }
        mVaryingHandler.finish();
        mVertSource = mVS.toString();
        mFragSource = mFS.toString();

        //TODO debug only, will remove
        String allShaders = String.format("""
                // Vertex GLSL
                %s
                // Fragment GLSL
                %s
                """, mVertSource, mFragSource);
        System.out.printf("%s: %s\n", Thread.currentThread(), allShaders);
    }

    boolean finish(GLPipelineState result) {
        if (mVertSource == null || mFragSource == null) {
            return false;
        }
        int program = glCreateProgram();
        if (program == 0) {
            return false;
        }

        PrintWriter errorWriter = mEngine.getContext().getErrorWriter();

        int frag = glCompileAndAttachShader(program, GL_FRAGMENT_SHADER, mFragSource,
                mEngine.getPipelineStateCache().getStates(), errorWriter);
        if (frag == 0) {
            glDeleteProgram(program);
            return false;
        }

        int vert = glCompileAndAttachShader(program, GL_VERTEX_SHADER, mVertSource,
                mEngine.getPipelineStateCache().getStates(), errorWriter);
        if (vert == 0) {
            glDeleteProgram(program);
            glDeleteShader(frag);
            return false;
        }

        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            try {
                String allShaders = String.format("""
                        // Vertex GLSL
                        %s
                        // Fragment GLSL
                        %s
                        """, mVertSource, mFragSource);
                String log = glGetProgramInfoLog(program).trim();
                GLCore.handleCompileError(errorWriter, allShaders, log);
                return false;
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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pLength = stack.mallocInt(1);
            IntBuffer pBinaryFormat = stack.mallocInt(1);
            glGetProgramiv(program, GL_PROGRAM_BINARY_LENGTH, pLength);
            System.out.println(pLength.get(0));
            if (pLength.get(0) > 0) {
                ByteBuffer pBinary = stack.malloc(pLength.get(0));
                glGetProgramBinary(program, pLength, pBinaryFormat, pBinary);
                System.out.println(pBinaryFormat.get(0));
            }
        }

        //TODO share vertex arrays
        @SharedPtr
        GLVertexArray vertexArray = GLVertexArray.make(mEngine, mPipelineInfo.geomProc());
        if (vertexArray == null) {
            glDeleteProgram(program);
            return false;
        }

        result.init(new GLProgram(mEngine, program),
                vertexArray,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl);
        return true;
    }

    @Override
    public Caps caps() {
        return mEngine.getCaps();
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
