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

import icyllis.modernui.graphics.SharedPtr;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.*;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static icyllis.arc3d.opengl.GLCore.*;

public class GLPipelineStateBuilder extends ProgramBuilder {

    private final GLDevice mDevice;

    private final VaryingHandler mVaryingHandler;
    private final GLUniformHandler mUniformHandler;

    private GLPipelineStateBuilder(GLDevice device,
                                   PipelineDesc desc,
                                   PipelineInfo pipelineInfo) {
        super(desc, pipelineInfo);
        mDevice = device;
        mVaryingHandler = new VaryingHandler(this);
        mUniformHandler = new GLUniformHandler(this);
    }

    @Nullable
    public static GLPipelineState createPipelineState(GLDevice device,
                                                      final PipelineDesc desc,
                                                      final PipelineInfo pipelineInfo) {

        GLPipelineStateBuilder builder = new GLPipelineStateBuilder(device, desc, pipelineInfo);
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
        String vertSource = mVS.toString();
        String fragSource = mFS.toString();

        PrintWriter pw = mDevice.getContext().getErrorWriter();

        int frag = glCompileAndAttachShader(program, GL_FRAGMENT_SHADER, fragSource,
                mDevice.getPipelineBuilder().getStates(), pw);
        if (frag == 0) {
            glDeleteProgram(program);
            return null;
        }

        int vert = glCompileAndAttachShader(program, GL_VERTEX_SHADER, vertSource,
                mDevice.getPipelineBuilder().getStates(), pw);
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
                GLCore.handleCompileError(pw, allShaders, log);
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

        @SharedPtr
        GLPipeline pipeline = GLPipeline.make(mDevice, mPipelineInfo.geomProc(), program);
        if (pipeline == null) {
            glDeleteProgram(program);
            return null;
        }

        return new GLPipelineState(mDevice,
                pipeline,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl);
    }

    @Override
    public Caps caps() {
        return mDevice.getCaps();
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
