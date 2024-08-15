/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.engine.ShaderCaps;
import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.GL_MAX_RENDERBUFFER_SIZE;
import static org.lwjgl.opengl.GL41C.*;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY;

/**
 * OpenGL desktop implementation of OpenGL.
 */
public final class GLCaps_GL extends GLCaps implements GLInterface {

    private final boolean mShaderBinarySupport;
    private boolean mTextureBarrierNV;
    private boolean mSpecializeShaderARB;

    @VisibleForTesting
    public GLCaps_GL(ContextOptions options, Object capabilities) {
        super(options);
        GLCapabilities caps = (GLCapabilities) capabilities;
        // OpenGL 3.3 is the minimum requirement
        if (!caps.OpenGL33) {
            throw new UnsupportedOperationException("OpenGL 3.3 is unavailable");
        }

        /*if (!caps.GL_ARB_draw_elements_base_vertex) {
                    missingExtensions.add("ARB_draw_elements_base_vertex");
                }*/

        if (!caps.OpenGL41) {
            // macOS supports this
            if (!caps.GL_ARB_viewport_array) {
                //missingExtensions.add("ARB_viewport_array");
            }
        }
        if (!caps.OpenGL44) {
            if (!caps.GL_ARB_clear_texture) {
                //missingExtensions.add("ARB_clear_texture");
            }
        }
        if (caps.OpenGL45 || caps.GL_ARB_texture_barrier) {
            mTextureBarrierSupport = true;
            mTextureBarrierNV = false;
        } else if (caps.GL_NV_texture_barrier) {
            // macOS supports this
            mTextureBarrierSupport = true;
            mTextureBarrierNV = true;
            options.mLogger.info("Use NV_texture_barrier");
        } else {
            mTextureBarrierSupport = false;
        }

        mDebugSupport = caps.OpenGL43 || caps.GL_KHR_debug;
        // OpenGL 3.2
        mDrawElementsBaseVertexSupport = true;
        mBaseInstanceSupport = caps.OpenGL42 || caps.GL_ARB_base_instance;
        mCopyImageSupport = caps.OpenGL43 || caps.GL_ARB_copy_image;
        // macOS supports this
        mTexStorageSupport = caps.OpenGL42 || caps.GL_ARB_texture_storage;
        mViewCompatibilityClassSupport = caps.OpenGL43 || caps.GL_ARB_internalformat_query2;
        mShaderBinarySupport = caps.OpenGL41 || caps.GL_ARB_ES2_compatibility;
        mProgramBinarySupport = caps.OpenGL41 || caps.GL_ARB_get_program_binary;
        mProgramParameterSupport = mProgramBinarySupport;
        mVertexAttribBindingSupport = caps.OpenGL43 || caps.GL_ARB_vertex_attrib_binding;
        mBufferStorageSupport = caps.OpenGL44 || caps.GL_ARB_buffer_storage;
        // our attachment points are consistent with draw buffers
        mMaxColorAttachments = Math.min(Math.min(
                        glGetInteger(GL_MAX_DRAW_BUFFERS),
                        glGetInteger(GL_MAX_COLOR_ATTACHMENTS)),
                MAX_COLOR_TARGETS);
        mMinUniformBufferOffsetAlignment = glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
        mMinStorageBufferOffsetAlignment = glGetInteger(GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT);

        String versionString = glGetString(GL_VERSION);
        String vendorString = glGetString(GL_VENDOR);
        mVendor = GLUtil.findVendor(vendorString);
        mDriver = GLUtil.findDriver(mVendor, vendorString, versionString);
        options.mLogger.info("Identified OpenGL vendor: {}", mVendor);
        options.mLogger.info("Identified OpenGL driver: {}", mDriver);

        // macOS supports this
        if (caps.OpenGL41 || caps.GL_ARB_ES2_compatibility) {
            mMaxFragmentUniformVectors = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_VECTORS);
        } else {
            mMaxFragmentUniformVectors = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS) / 4;
        }
        mMaxVertexAttributes = Math.min(MAX_VERTEX_ATTRIBUTES, glGetInteger(GL_MAX_VERTEX_ATTRIBS));
        if (mVertexAttribBindingSupport) {
            mMaxVertexBindings = Math.min(MAX_VERTEX_BINDINGS, glGetInteger(GL_MAX_VERTEX_ATTRIB_BINDINGS));
        }

        if (caps.OpenGL43 || caps.GL_ARB_invalidate_subdata) {
            mInvalidateBufferType = INVALIDATE_BUFFER_TYPE_INVALIDATE;
        } else {
            mInvalidateBufferType = INVALIDATE_BUFFER_TYPE_NULL_DATA;
        }

        // DSA-like extensions must be supported as well
        mDSASupport = caps.OpenGL45 ||
                (caps.GL_ARB_direct_state_access && mInvalidateBufferType == INVALIDATE_BUFFER_TYPE_INVALIDATE);

        mTransferPixelsToRowBytesSupport = true;

        // When we are abandoning the context we cannot call into GL thus we should skip any sync work.
        mMustSyncGpuDuringDiscard = false;

        if (mDebugSupport) {
            mMaxLabelLength = glGetInteger(GL_MAX_LABEL_LENGTH);
        } else {
            mMaxLabelLength = 0;
        }

        ShaderCaps shaderCaps = mShaderCaps;
        // target API is just for validation
        if (caps.OpenGL45) {
            shaderCaps.mTargetApi = TargetApi.OPENGL_4_5;
        } else if (caps.OpenGL43) {
            shaderCaps.mTargetApi = TargetApi.OPENGL_4_3;
        } else {
            shaderCaps.mTargetApi = TargetApi.OPENGL_3_3;
        }
        final int glslVersion;
        if (caps.OpenGL46) {
            glslVersion = 460;
        } else if (caps.OpenGL45) {
            glslVersion = 450;
        } else if (caps.OpenGL44) {
            glslVersion = 440;
        } else if (caps.OpenGL43) {
            glslVersion = 430;
        } else if (caps.OpenGL42) {
            glslVersion = 420;
        } else if (caps.OpenGL41) {
            glslVersion = 410;
        } else if (caps.OpenGL40) {
            glslVersion = 400;
        } else {
            glslVersion = 330;
        }
        mGLSLVersion = glslVersion;
        // round down the version
        if (glslVersion >= 450) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_450;
        } else if (glslVersion == 440) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_440;
        } else if (glslVersion == 430) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_430;
        } else if (glslVersion == 420) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_420;
        } else if (glslVersion >= 400) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_400;
        } else {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_330;
        }
        initGLSL(caps, shaderCaps.mGLSLVersion);

        // OpenGL 3.3
        shaderCaps.mDualSourceBlendingSupport = true;

        if (caps.GL_NV_conservative_raster) {
            mConservativeRasterSupport = true;
        }

        // Protect ourselves against tracking huge amounts of texture state.
        shaderCaps.mMaxFragmentSamplers = Math.min(32, glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));

        if (caps.GL_NV_blend_equation_advanced_coherent) {
            mBlendEquationSupport = Caps.BlendEquationSupport.ADVANCED_COHERENT;
            shaderCaps.mAdvBlendEqInteraction = ShaderCaps.Automatic_AdvBlendEqInteraction;
        } else if (caps.GL_KHR_blend_equation_advanced_coherent) {
            mBlendEquationSupport = Caps.BlendEquationSupport.ADVANCED_COHERENT;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.GeneralEnable_AdvBlendEqInteraction;
        } else if (caps.GL_NV_blend_equation_advanced) {
            mBlendEquationSupport = Caps.BlendEquationSupport.ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.Automatic_AdvBlendEqInteraction;
        } else if (caps.GL_KHR_blend_equation_advanced) {
            mBlendEquationSupport = Caps.BlendEquationSupport.ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.GeneralEnable_AdvBlendEqInteraction;
        }

        mAnisotropySupport = caps.OpenGL46 ||
                caps.GL_ARB_texture_filter_anisotropic ||
                caps.GL_EXT_texture_filter_anisotropic;
        if (mAnisotropySupport) {
            mMaxTextureMaxAnisotropy = GL11C.glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        }

        mMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        mMaxRenderTargetSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);
        mMaxPreferredRenderTargetSize = mMaxRenderTargetSize;

        mGpuTracingSupport = caps.GL_EXT_debug_marker;

        mDynamicStateArrayGeometryProcessorTextureSupport = true;

        if (mProgramBinarySupport) {
            int count = glGetInteger(GL_NUM_PROGRAM_BINARY_FORMATS);
            if (count > 0) {
                mProgramBinaryFormats = new int[count];
                GL11C.glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, mProgramBinaryFormats);
            } else {
                mProgramBinarySupport = false;
            }
        }
        if (mShaderBinarySupport && (caps.OpenGL46 || caps.GL_ARB_gl_spirv)) {
            int count = GL11C.glGetInteger(GL_NUM_SHADER_BINARY_FORMATS);
            if (count > 0) {
                int[] shaderBinaryFormats = new int[count];
                GL11C.glGetIntegerv(GL_SHADER_BINARY_FORMATS, shaderBinaryFormats);
                for (int format : shaderBinaryFormats) {
                    if (format == GL46C.GL_SHADER_BINARY_FORMAT_SPIR_V) {
                        mSPIRVSupport = true;
                        mSpecializeShaderARB = !caps.OpenGL46;
                        break;
                    }
                }
            }
        }
        if (mSPIRVSupport) {
            shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_0;
        }

        initFormatTable(caps);
        assert validateFormatTable();

        applyDriverWorkaround();

        finishInitialization(options);
    }

    private void initGLSL(GLCapabilities caps, GLSLVersion version) {
        ShaderCaps shaderCaps = mShaderCaps;

        // Desktop
        shaderCaps.mPreferFlatInterpolation = true;
        // GLSL 130
        shaderCaps.mNoPerspectiveInterpolationSupport = true;
        // Desktop
        shaderCaps.mVertexIDSupport = true;
        // GLSL 330
        shaderCaps.mInfinitySupport = true;
        // Desktop
        shaderCaps.mNonConstantArrayIndexSupport = true;
        // GLSL 400
        shaderCaps.mBitManipulationSupport = version.isAtLeast(GLSLVersion.GLSL_400);
        // GLSL 400
        shaderCaps.mFMASupport = version.isAtLeast(GLSLVersion.GLSL_400);
        // GLSL 400
        shaderCaps.mTextureQueryLod = version.isAtLeast(GLSLVersion.GLSL_400);

        shaderCaps.mUseUniformBinding = caps.OpenGL42;
        shaderCaps.mUseVaryingLocation = caps.OpenGL44;
        shaderCaps.mUseBlockMemberOffset = caps.OpenGL44;
    }

    void initFormatTable(GLCapabilities caps) {
        super.initFormatTable(mTexStorageSupport, caps.GL_EXT_texture_compression_s3tc);

        final int nonMSAARenderFlags = FormatInfo.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        // Format: RGB565
        if (caps.OpenGL42 || caps.GL_ARB_ES2_compatibility) {
            // macOS supports this
            FormatInfo info = getFormatInfo(GL41C.GL_RGB565);
            info.mFlags |= FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
        }

        // Format: RGB8
        {
            FormatInfo info = getFormatInfo(GL11C.GL_RGB8);
            // Even in OpenGL 4.6 GL_RGB8 is required to be color renderable but not required to be
            // a supported render buffer format. Since we usually use render buffers for MSAA on
            // we don't support MSAA for GL_RGB8.
            if ((caps.OpenGL43 || caps.GL_ARB_internalformat_query2) &&
                    GL42C.glGetInternalformati(GL30C.GL_RENDERBUFFER, GL11C.GL_RGB8,
                            GL43C.GL_INTERNALFORMAT_SUPPORTED) == GL11C.GL_TRUE) {
                info.mFlags |= msaaRenderFlags;
            } else {
                info.mFlags |= nonMSAARenderFlags;
            }
        }

        // Format: COMPRESSED_RGB8_ETC2
        if (caps.OpenGL43 || caps.GL_ARB_ES3_compatibility) {
            FormatInfo info = getFormatInfo(GL43C.GL_COMPRESSED_RGB8_ETC2);
            info.mFlags |= FormatInfo.TEXTURABLE_FLAG;
        }

        // Init samples
        for (FormatInfo info : mFormatTable) {
            if (mCopyImageSupport && mViewCompatibilityClassSupport && info.mInternalFormatForTexture != 0) {
                info.mViewCompatibilityClass = GL42C.glGetInternalformati(
                        GL11C.GL_TEXTURE_2D, info.mInternalFormatForTexture, GL43C.GL_VIEW_COMPATIBILITY_CLASS
                );
            }
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0) {
                // We assume that MSAA rendering is supported only if we support non-MSAA rendering.
                assert (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0;
                // macOS supports this
                if (caps.OpenGL42 || caps.GL_ARB_internalformat_query) {
                    int glFormat = info.mInternalFormatForRenderbuffer;
                    int count = GL42C.glGetInternalformati(GL30C.GL_RENDERBUFFER, glFormat, GL42C.GL_NUM_SAMPLE_COUNTS);
                    if (count > 0) {
                        try (MemoryStack stack = MemoryStack.stackPush()) {
                            IntBuffer temp = stack.mallocInt(count);
                            GL42C.glGetInternalformativ(GL30C.GL_RENDERBUFFER, glFormat, GL13C.GL_SAMPLES, temp);
                            // GL has a concept of MSAA rasterization with a single sample, but we do not.
                            if (temp.get(count - 1) == 1) {
                                --count;
                                assert (count == 0 || temp.get(count - 1) > 1);
                            }
                            info.mColorSampleCounts = new int[count + 1];
                            // We initialize our supported values with 1 (no msaa) and reverse the order
                            // returned by GL so that the array is ascending.
                            info.mColorSampleCounts[0] = 1;
                            for (int j = 0; j < count; ++j) {
                                info.mColorSampleCounts[j + 1] = temp.get(count - j - 1);
                            }
                        }
                    }
                } else {
                    int maxSampleCnt = Math.max(1, GL11C.glGetInteger(GL30C.GL_MAX_SAMPLES));
                    int count = 5; // [1, 2, 4, 8, 16]
                    for (; count > 0; --count) {
                        if ((1 << (count - 1)) <= maxSampleCnt) {
                            break;
                        }
                    }
                    if (count > 0) {
                        info.mColorSampleCounts = new int[count];
                        for (int i = 0; i < count; i++) {
                            info.mColorSampleCounts[i] = 1 << i;
                        }
                    }
                }
            } else if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0) {
                info.mColorSampleCounts = new int[1];
                info.mColorSampleCounts[0] = 1;
            }
        }
    }

    @Override
    public void glEnable(int cap) {
        GL11C.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        GL11C.glDisable(cap);
    }

    @Override
    public int glGenTextures() {
        return GL11C.glGenTextures();
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GL11C.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format,
                             int type, long pixels) {
        GL11C.nglTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                int type, long pixels) {
        GL11C.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glDeleteTextures(int texture) {
        GL11C.glDeleteTextures(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GL11C.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GL11C.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11C.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11C.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GL11C.nglDrawElements(mode, count, type, indices);
    }

    @Override
    public int glGetError() {
        return GL11C.glGetError();
    }

    @Nullable
    @Override
    public String glGetString(int name) {
        return GL11C.glGetString(name);
    }

    @Override
    public int glGetInteger(int pname) {
        return GL11C.glGetInteger(pname);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GL11C.glScissor(x, y, width, height);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11C.glViewport(x, y, width, height);
    }

    @Override
    public void glActiveTexture(int texture) {
        GL13C.glActiveTexture(texture);
    }

    @Override
    public void glBlendEquation(int mode) {
        GL14C.glBlendEquation(mode);
    }

    @Override
    public int glGenBuffers() {
        return GL15C.glGenBuffers();
    }

    @Override
    public void glDeleteBuffers(int buffer) {
        GL15C.glDeleteBuffers(buffer);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15C.glBindBuffer(target, buffer);
    }

    @Override
    public void glBufferData(int target, long size, long data, int usage) {
        GL15C.nglBufferData(target, size, data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, long size, long data) {
        GL15C.nglBufferSubData(target, offset, size, data);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return GL15C.glUnmapBuffer(target);
    }

    @Override
    public int glCreateProgram() {
        return GL20C.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20C.glDeleteProgram(program);
    }

    @Override
    public int glCreateShader(int type) {
        return GL20C.glCreateShader(type);
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20C.glDeleteShader(shader);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20C.glAttachShader(program, shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GL20C.glDetachShader(program, shader);
    }

    @Override
    public void glShaderSource(int shader, int count, long strings, long length) {
        GL20C.nglShaderSource(shader, count, strings, length);
    }

    @Override
    public void glCompileShader(int shader) {
        GL20C.glCompileShader(shader);
    }

    @Override
    public void glLinkProgram(int program) {
        GL20C.glLinkProgram(program);
    }

    @Override
    public void glUseProgram(int program) {
        GL20C.glUseProgram(program);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GL20C.glGetShaderi(shader, pname);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GL20C.glGetProgrami(program, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GL20C.glGetShaderInfoLog(shader);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return GL20C.glGetProgramInfoLog(program);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20C.glEnableVertexAttribArray(index);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20C.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GL30C.glVertexAttribIPointer(index, size, type, stride, pointer);
    }

    @Override
    public int glGenVertexArrays() {
        return GL30C.glGenVertexArrays();
    }

    @Override
    public void glDeleteVertexArrays(int array) {
        GL30C.glDeleteVertexArrays(array);
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30C.glBindVertexArray(array);
    }

    @Override
    public int glGenFramebuffers() {
        return GL30C.glGenFramebuffers();
    }

    @Override
    public void glDeleteFramebuffers(int framebuffer) {
        GL30C.glDeleteFramebuffers(framebuffer);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GL30C.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GL30C.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GL30C.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public int glGenRenderbuffers() {
        return GL30C.glGenRenderbuffers();
    }

    @Override
    public void glDeleteRenderbuffers(int renderbuffer) {
        GL30C.glDeleteRenderbuffers(renderbuffer);
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GL30C.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GL30C.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        GL30C.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public long glMapBufferRange(int target, long offset, long length, int access) {
        return GL30C.nglMapBufferRange(target, offset, length, access);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        GL31C.glDrawArraysInstanced(mode, first, count, instancecount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
        GL31C.glDrawElementsInstanced(mode, count, type, indices, instancecount);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        GL31C.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        return GL32C.glFenceSync(condition, flags);
    }

    @Override
    public void glDeleteSync(long sync) {
        GL32C.nglDeleteSync(sync);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        return GL32C.nglClientWaitSync(sync, flags, timeout);
    }

    @Override
    public int glGenSamplers() {
        return GL33C.glGenSamplers();
    }

    @Override
    public void glDeleteSamplers(int sampler) {
        GL33C.glDeleteSamplers(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        GL33C.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param) {
        GL33C.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param) {
        GL33C.glSamplerParameterf(sampler, pname, param);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        GL33C.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        GL32C.nglDrawElementsBaseVertex(mode, count, type, indices, basevertex);
    }

    @Override
    public void glDrawElementsInstancedBaseVertex(int mode, int count, int type, long indices, int instancecount,
                                                  int basevertex) {
        GL32C.nglDrawElementsInstancedBaseVertex(mode, count, type, indices, instancecount, basevertex);
    }

    @Override
    public void glShaderBinary(IntBuffer shaders, int binaryformat, ByteBuffer binary) {
        assert mShaderBinarySupport;
        GL41C.glShaderBinary(shaders, binaryformat, binary);
    }

    @Override
    public void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance) {
        assert mBaseInstanceSupport;
        GL42C.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
    }

    @Override
    public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices,
                                                              int instancecount, int basevertex, int baseinstance) {
        assert mBaseInstanceSupport;
        GL42C.nglDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex,
                baseinstance);
    }

    @Override
    public void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        assert mTexStorageSupport;
        GL42C.glTexStorage2D(target, levels, internalformat, width, height);
    }

    @Override
    public void glInvalidateBufferSubData(int buffer, long offset, long length) {
        assert mInvalidateBufferType == INVALIDATE_BUFFER_TYPE_INVALIDATE;
        GL43C.glInvalidateBufferSubData(buffer, offset, length);
    }

    @Override
    public void glObjectLabel(int identifier, int name, int length, long label) {
        assert mDebugSupport;
        GL43C.nglObjectLabel(identifier, name, length, label);
    }

    @Override
    public void glObjectLabel(int identifier, int name, CharSequence label) {
        assert mDebugSupport;
        GL43C.glObjectLabel(identifier, name, label);
    }

    @Override
    public void glBindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        assert mVertexAttribBindingSupport;
        GL43C.glBindVertexBuffer(bindingindex, buffer, offset, stride);
    }

    @Override
    public void glVertexAttribFormat(int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        assert mVertexAttribBindingSupport;
        GL43C.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
    }

    @Override
    public void glVertexAttribIFormat(int attribindex, int size, int type, int relativeoffset) {
        assert mVertexAttribBindingSupport;
        GL43C.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
    }

    @Override
    public void glVertexAttribBinding(int attribindex, int bindingindex) {
        assert mVertexAttribBindingSupport;
        GL43C.glVertexAttribBinding(attribindex, bindingindex);
    }

    @Override
    public void glVertexBindingDivisor(int bindingindex, int divisor) {
        assert mVertexAttribBindingSupport;
        GL43C.glVertexBindingDivisor(bindingindex, divisor);
    }

    @Override
    public void glBufferStorage(int target, long size, long data, int flags) {
        assert mBufferStorageSupport;
        GL44C.nglBufferStorage(target, size, data, flags);
    }

    @Override
    public void glTextureBarrier() {
        assert mTextureBarrierSupport;
        if (mTextureBarrierNV) {
            NVTextureBarrier.glTextureBarrierNV();
        } else {
            GL45C.glTextureBarrier();
        }
    }

    @Override
    public int glCreateBuffers() {
        assert mDSASupport;
        return GL45C.glCreateBuffers();
    }

    @Override
    public void glNamedBufferData(int buffer, long size, long data, int usage) {
        assert mDSASupport;
        GL45C.nglNamedBufferData(buffer, size, data, usage);
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, long size, long data) {
        assert mDSASupport;
        GL45C.nglNamedBufferSubData(buffer, offset, size, data);
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long length, int access) {
        assert mDSASupport;
        return GL45C.nglMapNamedBufferRange(buffer, offset, length, access);
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        assert mDSASupport;
        return GL45C.glUnmapNamedBuffer(buffer);
    }

    @Override
    public void glNamedBufferStorage(int buffer, long size, long data, int flags) {
        assert mDSASupport;
        GL45C.nglNamedBufferStorage(buffer, size, data, flags);
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset,
                                         long size) {
        assert mDSASupport;
        GL45C.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
    }

    @Override
    public int glCreateTextures(int target) {
        assert mDSASupport;
        return GL45C.glCreateTextures(target);
    }

    @Override
    public void glTextureParameteri(int texture, int pname, int param) {
        assert mDSASupport;
        GL45C.glTextureParameteri(texture, pname, param);
    }

    @Override
    public void glTextureStorage2D(int texture, int levels, int internalformat, int width, int height) {
        assert mDSASupport;
        GL45C.glTextureStorage2D(texture, levels, internalformat, width, height);
    }

    @Override
    public int glCreateVertexArrays() {
        assert mDSASupport;
        return GL45C.glCreateVertexArrays();
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        assert mDSASupport;
        GL45C.glEnableVertexArrayAttrib(vaobj, index);
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized,
                                          int relativeoffset) {
        assert mDSASupport;
        GL45C.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        assert mDSASupport;
        GL45C.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        assert mDSASupport;
        GL45C.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        assert mDSASupport;
        GL45C.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
    }

    @Override
    public void glBindTextureUnit(int unit, int texture) {
        assert mDSASupport;
        GL45C.glBindTextureUnit(unit, texture);
    }

    @Override
    public void glSpecializeShader(int shader, CharSequence pEntryPoint, IntBuffer pConstantIndex,
                                   IntBuffer pConstantValue) {
        assert mSPIRVSupport;
        if (mSpecializeShaderARB) {
            ARBGLSPIRV.glSpecializeShaderARB(shader, pEntryPoint, pConstantIndex, pConstantValue);
        } else {
            GL46C.glSpecializeShader(shader, pEntryPoint, pConstantIndex, pConstantValue);
        }
    }
}
