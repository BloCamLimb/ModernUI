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

import icyllis.arc3d.compiler.CompileOptions;
import icyllis.arc3d.compiler.ModuleLoader;
import icyllis.arc3d.compiler.ShaderCompiler;
import icyllis.arc3d.compiler.ShaderKind;
import icyllis.arc3d.compiler.TranslationUnit;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.BlendInfo;
import icyllis.arc3d.engine.DepthStencilSettings;
import icyllis.arc3d.engine.PipelineDesc;
import icyllis.arc3d.engine.VertexInputLayout;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL31C.GL_INVALID_INDEX;

public class GLGraphicsPipelineBuilder {

    private final GLDevice mDevice;
    private final PipelineDesc mPipelineDesc;

    private ByteBuffer mFinalizedVertGLSL;
    private ByteBuffer mFinalizedFragGLSL;
    private ByteBuffer mFinalizedVertSPIRV;
    private ByteBuffer mFinalizedFragSPIRV;

    private byte mPrimitiveType;
    private VertexInputLayout mInputLayout;
    private String mInputLayoutLabel;
    private BlendInfo mBlendInfo;
    private DepthStencilSettings mDepthStencilSettings;
    private PipelineDesc.UniformBlockInfo[] mUniformBlockInfos;
    private PipelineDesc.SamplerInfo[] mSamplerInfos;
    private String mPipelineLabel;

    private GLGraphicsPipelineBuilder(GLDevice device,
                                      PipelineDesc pipelineDesc) {
        mDevice = device;
        mPipelineDesc = pipelineDesc;
    }

    @NonNull
    @SharedPtr
    public static GLGraphicsPipeline createGraphicsPipeline(
            final GLDevice device,
            final PipelineDesc desc) {
        return new GLGraphicsPipeline(device,
                CompletableFuture.supplyAsync(() -> {
                    GLGraphicsPipelineBuilder builder = new GLGraphicsPipelineBuilder(device, desc);
                    builder.build();
                    return builder;
                }));
    }

    private void build() {
        var info = mPipelineDesc.createGraphicsPipelineInfo(mDevice);
        mPrimitiveType = info.mPrimitiveType;
        mInputLayout = info.mInputLayout;
        mInputLayoutLabel = info.mInputLayoutLabel;
        mBlendInfo = info.mBlendInfo;
        mDepthStencilSettings = info.mDepthStencilSettings;
        mUniformBlockInfos = info.mUniformBlockInfos;
        mSamplerInfos = info.mSamplerInfos;
        /*mFinalizedVertSource = toUTF8(info.mVertSource);
        mFinalizedFragSource = toUTF8(info.mFragSource);*/
        mPipelineLabel = info.mPipelineLabel;
        ShaderCompiler compiler = new ShaderCompiler();
        CompileOptions options = new CompileOptions();
        options.mUsePrecisionQualifiers = mDevice.getCaps().shaderCaps().mUsePrecisionModifiers;

        boolean success = true;
        if (!info.mFragSource.isEmpty()) {
            TranslationUnit fragmentProgram = compiler.parse(info.mFragSource, ShaderKind.FRAGMENT,
                    options, ModuleLoader.getInstance().loadCommonModule(compiler));
            if (fragmentProgram == null) {
                GLUtil.handleCompileError(mDevice.getLogger(),
                        info.mFragSource, compiler.getErrorMessage());
                success = false;
            } else {
                if (mDevice.getCaps().hasSPIRVSupport()) {
                    mFinalizedFragSPIRV = compiler.generateSPIRV(fragmentProgram, mDevice.getCaps().shaderCaps());
                } else {
                    mFinalizedFragGLSL = compiler.generateGLSL(fragmentProgram, mDevice.getCaps().shaderCaps());
                }
                if (mFinalizedFragGLSL == null && mFinalizedFragSPIRV == null) {
                    GLUtil.handleCompileError(mDevice.getLogger(),
                            info.mFragSource, compiler.getErrorMessage());
                    success = false;
                }
            }
        } else if (mDevice.getCaps().isGLES()) {
            // OpenGL ES requires both a vertex shader and fragment shader, create a simple shader
            String trivialFragGLSL = mDevice.getCaps().shaderCaps().mGLSLVersion.mVersionDecl + """
                    void main() {
                    }
                    """;
            mFinalizedFragGLSL = BufferUtils.createByteBuffer(trivialFragGLSL.length());
            MemoryUtil.memUTF8(trivialFragGLSL, false, mFinalizedFragGLSL);
        }
        if (success) {
            TranslationUnit vertexProgram = compiler.parse(info.mVertSource, ShaderKind.VERTEX,
                    options, ModuleLoader.getInstance().loadCommonModule(compiler));
            if (vertexProgram == null) {
                GLUtil.handleCompileError(mDevice.getLogger(),
                        info.mVertSource, compiler.getErrorMessage());
            } else {
                if (mDevice.getCaps().hasSPIRVSupport()) {
                    mFinalizedVertSPIRV = compiler.generateSPIRV(vertexProgram, mDevice.getCaps().shaderCaps());
                } else {
                    mFinalizedVertGLSL = compiler.generateGLSL(vertexProgram, mDevice.getCaps().shaderCaps());
                }
                if (mFinalizedVertGLSL == null && mFinalizedVertSPIRV == null) {
                    GLUtil.handleCompileError(mDevice.getLogger(),
                            info.mVertSource, compiler.getErrorMessage());
                }
            }
        }
        /*CompletableFuture.runAsync(() -> {
            String filename = mPipelineLabel.replaceAll("/", ".");
            try {
                Files.writeString(Path.of(filename + ".vert"), info.mVertSource,
                        StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (var channel = FileChannel.open(Path.of(filename + ".vert.glsl"),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                var src = mFinalizedVertGLSL.slice();
                while (src.hasRemaining()) {
                    channel.write(src);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Files.writeString(Path.of(filename + ".frag"), info.mFragSource,
                        StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (var channel = FileChannel.open(Path.of(filename + ".frag.glsl"),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                var src = mFinalizedFragGLSL.slice();
                while (src.hasRemaining()) {
                    channel.write(src);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/
    }

    /*@NonNull
    public static ByteBuffer toUTF8(StringBuilder shaderString) {
        // we assume ASCII only, so 1 byte per char
        int len = shaderString.length();
        ByteBuffer buffer = BufferUtils.createByteBuffer(len);
        len = 0;
            len += MemoryUtil.memUTF8(shaderString, false, buffer, len);
        assert len == buffer.capacity() && len == buffer.remaining();
        return buffer;
    }*/

    boolean finish(GLGraphicsPipeline dest) {
        if (mFinalizedVertGLSL == null && mFinalizedVertSPIRV == null) {
            return false;
        }
        GLInterface gl = mDevice.getGL();
        int program = gl.glCreateProgram();
        if (program == 0) {
            return false;
        }

        // fragment shader is optional in OpenGL
        int frag = 0;
        if (mFinalizedFragGLSL != null || mFinalizedFragSPIRV != null) {
            if (mFinalizedFragSPIRV != null) {
                frag = GLUtil.glSpecializeShader(mDevice, GL_FRAGMENT_SHADER, mFinalizedFragSPIRV,
                        "main", mDevice.getSharedResourceCache().getStats());
            } else {
                frag = GLUtil.glCompileShader(mDevice, GL_FRAGMENT_SHADER, mFinalizedFragGLSL,
                        mDevice.getSharedResourceCache().getStats());
            }
            if (frag == 0) {
                gl.glDeleteProgram(program);
                return false;
            }
        }

        int vert;
        if (mFinalizedVertSPIRV != null) {
            vert = GLUtil.glSpecializeShader(mDevice, GL_VERTEX_SHADER, mFinalizedVertSPIRV,
                    "main", mDevice.getSharedResourceCache().getStats());
        } else {
            vert = GLUtil.glCompileShader(mDevice, GL_VERTEX_SHADER, mFinalizedVertGLSL,
                    mDevice.getSharedResourceCache().getStats());
        }
        if (vert == 0) {
            gl.glDeleteProgram(program);
            if (frag != 0) {
                gl.glDeleteShader(frag);
            }
            return false;
        }

        gl.glAttachShader(program, vert);
        if (frag != 0) {
            gl.glAttachShader(program, frag);
        }

        gl.glLinkProgram(program);

        if (gl.glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            try {
                String log = gl.glGetProgramInfoLog(program);
                if (mFinalizedVertGLSL != null) {
                    GLUtil.handleLinkError(mDevice.getLogger(),
                            new String[]{
                                    "Vertex GLSL",
                                    "Fragment GLSL"},
                            new String[]{
                                    MemoryUtil.memUTF8(mFinalizedVertGLSL),
                                    MemoryUtil.memUTF8Safe(mFinalizedFragGLSL)},
                            log);
                } else {
                    mDevice.getLogger().error("Program linking error: {}", log);
                }
                return false;
            } finally {
                Reference.reachabilityFence(mFinalizedVertGLSL);
                Reference.reachabilityFence(mFinalizedFragGLSL);
                gl.glDeleteProgram(program);
                if (frag != 0) {
                    gl.glDeleteShader(frag);
                }
                gl.glDeleteShader(vert);
            }
        }

        // the shaders can be detached after the linking
        gl.glDetachShader(program, vert);
        if (frag != 0) {
            gl.glDetachShader(program, frag);
            gl.glDeleteShader(frag);
        }
        // the shaders can be marked for deletion after the linking
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

        @SharedPtr
        GLVertexArray vertexArray = mDevice.findOrCreateVertexArray(
                mInputLayout,
                mInputLayoutLabel);
        if (vertexArray == null) {
            gl.glDeleteProgram(program);
            return false;
        }

        if (mDevice.getCaps().hasDebugSupport()) {
            String label = mPipelineLabel;
            if (label != null && !label.isEmpty()) {
                label = "Arc3D_PIPE_" + label;
                label = label.substring(0, Math.min(label.length(),
                        mDevice.getCaps().maxLabelLength()));
                gl.glObjectLabel(GL43C.GL_PROGRAM, program, label);
            }
        }

        // Setup layout bindings if < OpenGL 4.2
        if (!mDevice.getCaps().shaderCaps().mUseUniformBinding) {
            if (mUniformBlockInfos != null) {
                for (var info : mUniformBlockInfos) {
                    if (info.mVisibility != 0) {
                        int index = gl.glGetUniformBlockIndex(program, info.mBlockName);
                        assert index != GL_INVALID_INDEX;
                        gl.glUniformBlockBinding(program, index, info.mBinding);
                    }
                }
            }
            // Assign texture units to sampler uniforms one time up front
            if (mSamplerInfos != null && mSamplerInfos.length > 0) {
                // We can bind program here, since we will bind this pipeline immediately
                gl.glUseProgram(program);
                for (var info : mSamplerInfos) {
                    if (info.mVisibility != 0) {
                        int location = gl.glGetUniformLocation(program, info.mName);
                        assert location != -1;
                        gl.glUniform1i(location, info.mBinding); // <- binding is just the texture unit (index)
                    }
                }
            }
        }

        dest.init(new GLProgram(mDevice, program),
                vertexArray,
                mPrimitiveType,
                mBlendInfo,
                mDepthStencilSettings/*,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl*/);
        return true;
    }
}
