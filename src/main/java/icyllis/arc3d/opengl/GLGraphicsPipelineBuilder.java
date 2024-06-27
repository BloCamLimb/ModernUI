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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.PipelineDesc;
import icyllis.arc3d.engine.VertexInputLayout;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.engine.trash.PipelineKey_old;
import icyllis.arc3d.granite.shading.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL20C.*;

public class GLGraphicsPipelineBuilder {

    private final GLDevice mDevice;
    private final PipelineDesc mPipelineDesc;

    private ByteBuffer mFinalizedVertSource;
    private ByteBuffer mFinalizedFragSource;

    private VertexInputLayout mInputLayout;
    private String mPipelineLabel;

    private GLGraphicsPipelineBuilder(GLDevice device,
                                      PipelineDesc pipelineDesc) {
        mDevice = device;
        mPipelineDesc = pipelineDesc;
    }

    @Nonnull
    @SharedPtr
    public static GLGraphicsPipeline createGraphicsPipeline(
            final GLDevice device,
            final PipelineDesc desc) {
        return new GLGraphicsPipeline(device, desc.getPrimitiveType(), desc.getBlendInfo(),
                CompletableFuture.supplyAsync(() -> {
            GLGraphicsPipelineBuilder builder = new GLGraphicsPipelineBuilder(device, desc);
            builder.build();
            return builder;
        }));
    }

    private void build() {
        var info = mPipelineDesc.createGraphicsPipelineInfo(mDevice);
        mPipelineLabel = info.mPipelineLabel;
        mInputLayout = info.mInputLayout;
        mFinalizedVertSource = toUTF8(info.mVertSource);
        mFinalizedFragSource = toUTF8(info.mFragSource);
    }

    @Nonnull
    public static ByteBuffer toUTF8(StringBuilder shaderString) {
        // we assume ASCII only, so 1 byte per char
        int len = shaderString.length();
        ByteBuffer buffer = BufferUtils.createByteBuffer(len);
        len = 0;
            len += MemoryUtil.memUTF8(shaderString, false, buffer, len);
        assert len == buffer.capacity() && len == buffer.remaining();
        return buffer;
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
                mDevice.getSharedResourceCache().getStats());
        if (frag == 0) {
            gl.glDeleteProgram(program);
            return false;
        }

        int vert = GLUtil.glCompileShader(mDevice, GL_VERTEX_SHADER, mFinalizedVertSource,
                mDevice.getSharedResourceCache().getStats());
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
                GLUtil.handleLinkError(mDevice.getLogger(),
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
        GLVertexArray vertexArray = GLVertexArray.make(mDevice,
                mInputLayout,
                mPipelineLabel);
        if (vertexArray == null) {
            gl.glDeleteProgram(program);
            return false;
        }

        dest.init(new GLProgram(mDevice, program),
                vertexArray/*,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl*/);
        return true;
    }
}
