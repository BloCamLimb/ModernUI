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

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.PipelineKey;
import icyllis.arc3d.engine.GraphicsPipelineDesc_Old;
import icyllis.arc3d.engine.shading.*;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL20C.*;

public class GLGraphicsPipelineBuilder extends GraphicsPipelineBuilder {

    private final GLDevice mDevice;

    private final VaryingHandler mVaryingHandler;
    private final GLUniformHandler mUniformHandler;

    private ByteBuffer mFinalizedVertSource;
    private ByteBuffer mFinalizedFragSource;

    private GLGraphicsPipelineBuilder(GLDevice device,
                                      PipelineKey desc,
                                      GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        super(desc, graphicsPipelineDesc, device.getCaps());
        mDevice = device;
        mVaryingHandler = new VaryingHandler(this);
        mUniformHandler = new GLUniformHandler(this);
    }

    @Nonnull
    public static GLGraphicsPipeline createGraphicsPipeline(
            final GLDevice device,
            final PipelineKey desc,
            final GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        return new GLGraphicsPipeline(device, graphicsPipelineDesc.primitiveType(), CompletableFuture.supplyAsync(() -> {
            GLGraphicsPipelineBuilder builder = new GLGraphicsPipelineBuilder(device, desc, graphicsPipelineDesc);
            builder.build();
            return builder;
        }));
    }

    private void build() {
        if (!emitAndInstallProcs()) {
            return;
        }
        mVaryingHandler.finish();
        mFinalizedVertSource = mVS.toUTF8();
        mFinalizedFragSource = mFS.toUTF8();
        mVS = null;
        mFS = null;
    }

    boolean finish(GLGraphicsPipeline dest) {
        if (mFinalizedVertSource == null ||
                mFinalizedFragSource == null) {
            return false;
        }
        GLInterface gl = mDevice.getGL();
        int program = gl.glCreateProgram();
        if (program == 0) {
            return false;
        }

        int frag = GLUtil.glCompileShader(mDevice, GL_FRAGMENT_SHADER, mFinalizedFragSource,
                mDevice.getPipelineCache().getStats());
        if (frag == 0) {
            gl.glDeleteProgram(program);
            return false;
        }

        int vert = GLUtil.glCompileShader(mDevice, GL_VERTEX_SHADER, mFinalizedVertSource,
                mDevice.getPipelineCache().getStats());
        if (vert == 0) {
            gl.glDeleteProgram(program);
            gl.glDeleteShader(frag);
            return false;
        }

        gl.glAttachShader(program, vert);
        gl.glAttachShader(program, frag);

        gl.glLinkProgram(program);

        if (gl.glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            try {
                String log = gl.glGetProgramInfoLog(program);
                GLUtil.handleLinkError(mDevice.getContext().getLogger(),
                        new String[]{
                                "Vertex GLSL",
                                "Fragment GLSL"},
                        new String[]{
                                MemoryUtil.memUTF8(mFinalizedVertSource),
                                MemoryUtil.memUTF8(mFinalizedFragSource)},
                        log);
                return false;
            } finally {
                Reference.reachabilityFence(mFinalizedVertSource);
                Reference.reachabilityFence(mFinalizedFragSource);
                gl.glDeleteProgram(program);
                gl.glDeleteShader(frag);
                gl.glDeleteShader(vert);
            }
        }

        // the shaders can be detached after the linking
        gl.glDetachShader(program, vert);
        gl.glDetachShader(program, frag);

        gl.glDeleteShader(frag);
        gl.glDeleteShader(vert);

        /*try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pLength = stack.mallocInt(1);
            IntBuffer pBinaryFormat = stack.mallocInt(1);
            glGetProgramiv(program, GL_PROGRAM_BINARY_LENGTH, pLength);
            int len = pLength.get(0);
            System.out.println(len);
            if (len > 0) {
                ByteBuffer pBinary = stack.malloc(len);
                glGetProgramBinary(program, pLength, pBinaryFormat, pBinary);
                System.out.println(pBinaryFormat.get(0));
                //System.out.println(MemoryUtil.memUTF8(pBinary));
                *//*for (int i = 0; i < len; ) {
                    System.out.print(pBinary.get(i++) & 0xFF);
                    System.out.print(", ");
                    if ((i & 31) == 0) {
                        System.out.println();
                    }
                }*//*
            }
        }*/

        //TODO share vertex arrays
        @SharedPtr
        GLVertexArray vertexArray = GLVertexArray.make(mDevice, mGraphicsPipelineDesc.geomProc());
        if (vertexArray == null) {
            gl.glDeleteProgram(program);
            return false;
        }

        dest.init(new GLProgram(mDevice, program),
                vertexArray,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl);
        return true;
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
