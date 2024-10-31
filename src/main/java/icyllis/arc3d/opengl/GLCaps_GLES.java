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

import icyllis.arc3d.compiler.GLSLVersion;
import icyllis.arc3d.compiler.TargetApi;
import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.opengles.*;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.nio.*;

import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.opengles.GLES30.*;
import static org.lwjgl.opengles.GLES31.GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT;
import static org.lwjgl.opengles.GLES32.GL_MAX_LABEL_LENGTH;

public final class GLCaps_GLES extends GLCaps implements GLInterface {

    private boolean mDrawElementsBaseVertexEXT;
    private boolean mCopyImageSubDataEXT;

    @VisibleForTesting
    public GLCaps_GLES(ContextOptions options, Object capabilities) {
        super(options);
        GLESCapabilities caps = (GLESCapabilities) capabilities;
        // OpenGL ES 3.0 is the minimum requirement
        if (!caps.GLES30) {
            throw new UnsupportedOperationException("OpenGL ES 3.0 is unavailable");
        }

        if (caps.GL_NV_texture_barrier) {
            mTextureBarrierSupport = true;
            options.mLogger.info("Use NV_texture_barrier");
        } else {
            mTextureBarrierSupport = false;
        }
        mDSASupport = false;

        mDebugSupport = caps.GLES32;
        if (caps.GLES32) {
            mDrawElementsBaseVertexSupport = true;
            mDrawElementsBaseVertexEXT = false;
        } else if (caps.GL_EXT_draw_elements_base_vertex) {
            mDrawElementsBaseVertexSupport = true;
            mDrawElementsBaseVertexEXT = true;
        } else {
            mDrawElementsBaseVertexSupport = false;
        }
        mBaseInstanceSupport = caps.GL_EXT_base_instance;
        if (caps.GLES32) {
            mCopyImageSupport = true;
            mCopyImageSubDataEXT = false;
        } else if (caps.GL_EXT_copy_image) {
            mCopyImageSupport = true;
            mCopyImageSubDataEXT = true;
        } else {
            mCopyImageSupport = false;
        }
        // textureStorageSupported - OpenGL ES 3.0
        mTexStorageSupport = true;
        mViewCompatibilityClassSupport = false;
        // OpenGL ES 3.0
        mProgramBinarySupport = true;
        mProgramParameterSupport = true;
        mVertexAttribBindingSupport = caps.GLES31;
        mBufferStorageSupport = caps.GL_EXT_buffer_storage;
        // our attachment points are consistent with draw buffers
        mMaxColorAttachments = Math.min(Math.min(
                        glGetInteger(GL_MAX_DRAW_BUFFERS),
                        glGetInteger(GL_MAX_COLOR_ATTACHMENTS)),
                MAX_COLOR_TARGETS);
        mMinUniformBufferOffsetAlignment = glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
        mMinStorageBufferOffsetAlignment = glGetInteger(GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT);

        String versionString = GLES20.glGetString(GL_VERSION);
        String vendorString = GLES20.glGetString(GL_VENDOR);
        mVendor = GLUtil.findVendor(vendorString);
        mDriver = GLUtil.findDriver(mVendor, vendorString, versionString);
        options.mLogger.info("Identified OpenGL vendor: {}", mVendor);
        options.mLogger.info("Identified OpenGL driver: {}", mDriver);

        mMaxFragmentUniformVectors = GLES20.glGetInteger(GL_MAX_FRAGMENT_UNIFORM_VECTORS);
        mMaxVertexAttributes = Math.min(32, GLES20.glGetInteger(GL_MAX_VERTEX_ATTRIBS));

        mInvalidateBufferType = INVALIDATE_BUFFER_TYPE_NULL_DATA;
        mInvalidateFramebufferSupport = true;

        mTransferPixelsToRowBytesSupport = true;

        // When we are abandoning the context we cannot call into GL thus we should skip any sync work.
        mMustSyncGpuDuringDiscard = false;

        if (mDebugSupport) {
            mMaxLabelLength = GLES20.glGetInteger(GL_MAX_LABEL_LENGTH);
        } else {
            mMaxLabelLength = 0;
        }

        ShaderCaps shaderCaps = mShaderCaps;
        // target API is just for validation
        if (caps.GLES31) {
            shaderCaps.mTargetApi = TargetApi.OPENGL_ES_3_1;
        } else {
            shaderCaps.mTargetApi = TargetApi.OPENGL_ES_3_0;
        }
        final int glslVersion;
        if (caps.GLES32) {
            glslVersion = 320;
            options.mLogger.info("Using OpenGL ES 3.2 and GLSL 3.20");
        } else if (caps.GLES31) {
            glslVersion = 310;
            options.mLogger.info("Using OpenGL ES 3.1 and GLSL 3.10");
        } else {
            glslVersion = 300;
            options.mLogger.info("Using OpenGL ES 3.0 and GLSL 3.00");
        }
        mGLSLVersion = glslVersion;
        if (glslVersion == 320) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_320_ES;
        } else if (glslVersion == 310) {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_310_ES;
        } else {
            shaderCaps.mGLSLVersion = GLSLVersion.GLSL_300_ES;
        }
        initGLSL(caps, shaderCaps.mGLSLVersion);

        shaderCaps.mDualSourceBlendingSupport = caps.GL_EXT_blend_func_extended;

        if (caps.GL_NV_conservative_raster) {
            mConservativeRasterSupport = true;
        }

        // Protect ourselves against tracking huge amounts of texture state.
        shaderCaps.mMaxFragmentSamplers = Math.min(32, GLES20.glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));

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

        mAnisotropySupport = caps.GL_EXT_texture_filter_anisotropic;
        if (mAnisotropySupport) {
            mMaxTextureMaxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        }

        mMaxTextureSize = GLES20.glGetInteger(GL_MAX_TEXTURE_SIZE);
        mMaxRenderTargetSize = GLES20.glGetInteger(GL_MAX_RENDERBUFFER_SIZE);
        mMaxPreferredRenderTargetSize = mMaxRenderTargetSize;

        mGpuTracingSupport = caps.GL_EXT_debug_marker;

        mDynamicStateArrayGeometryProcessorTextureSupport = true;

        if (mProgramBinarySupport) {
            int count = GLES20.glGetInteger(GL_NUM_PROGRAM_BINARY_FORMATS);
            if (count > 0) {
                mProgramBinaryFormats = new int[count];
                GLES20.glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, mProgramBinaryFormats);
            } else {
                mProgramBinarySupport = false;
            }
        }
        mSPIRVSupport = false;

        initFormatTable(caps);
        assert validateFormatTable();

        applyDriverWorkaround();

        finishInitialization(options);
    }

    private void initGLSL(GLESCapabilities caps, GLSLVersion version) {
        ShaderCaps shaderCaps = mShaderCaps;

        shaderCaps.mPreferFlatInterpolation = mVendor != GLUtil.GLVendor.QUALCOMM;
        shaderCaps.mNoPerspectiveInterpolationSupport = false;
        // GLSL 300 ES
        shaderCaps.mVertexIDSupport = true;
        // GLSL 300 ES
        shaderCaps.mInfinitySupport = true;
        // GLSL 300 ES
        shaderCaps.mNonConstantArrayIndexSupport = true;
        // GLSL 310 ES
        shaderCaps.mBitManipulationSupport = version.isAtLeast(GLSLVersion.GLSL_310_ES);
        // GLSL 320 ES
        shaderCaps.mFMASupport = version.isAtLeast(GLSLVersion.GLSL_320_ES);

        shaderCaps.mUseUniformBinding = caps.GLES31;
        shaderCaps.mUseVaryingLocation = caps.GLES32;
        shaderCaps.mUseBlockMemberOffset = false; // Vulkan only
        shaderCaps.mUsePrecisionModifiers = true;
    }

    void initFormatTable(GLESCapabilities caps) {
        // textureStorageSupported - OpenGL ES 3.0
        super.initFormatTable(true, caps.GL_EXT_texture_compression_s3tc);

        final int nonMSAARenderFlags = FormatInfo.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        // Format: RGB565
        {
            FormatInfo info = getFormatInfo(GL_RGB565);
            info.mFlags |= FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
        }

        // Format: RGB8
        {
            FormatInfo info = mFormatTable[6];
            info.mFlags |= msaaRenderFlags;
        }

        // Format: COMPRESSED_RGB8_ETC2
        {
            FormatInfo info = getFormatInfo(GL_COMPRESSED_RGB8_ETC2);
            info.mFlags |= FormatInfo.TEXTURABLE_FLAG;
        }

        // Init samples
        for (FormatInfo info : mFormatTable) {
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0) {
                // We assume that MSAA rendering is supported only if we support non-MSAA rendering.
                assert (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0;
                int glFormat = info.mInternalFormatForRenderbuffer;
                // OpenGL ES 3.0
                int count = glGetInternalformati(GL_RENDERBUFFER, glFormat, GL_NUM_SAMPLE_COUNTS);
                if (count > 0) {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        IntBuffer temp = stack.mallocInt(count);
                        glGetInternalformativ(GL_RENDERBUFFER, glFormat, GL_SAMPLES, temp);
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
            } else if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0) {
                info.mColorSampleCounts = new int[1];
                info.mColorSampleCounts[0] = 1;
            }
        }
    }

    @Override
    public boolean isGLES() {
        return true;
    }

    @Override
    public void glEnable(int cap) {
        GLES20.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        GLES20.glDisable(cap);
    }

    @Override
    public void glFrontFace(int mode) {
        GLES20.glFrontFace(mode);
    }

    @Override
    public void glLineWidth(float width) {
        GLES20.glLineWidth(width);
    }

    @Override
    public int glGenTextures() {
        return GLES20.glGenTextures();
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexParameteriv(int target, int pname, IntBuffer params) {
        GLES20.glTexParameteriv(target, pname, params);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format,
                             int type, long pixels) {
        GLES20.nglTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                int type, long pixels) {
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width,
                                    int height) {
        GLES20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glDeleteTextures(int texture) {
        GLES20.glDeleteTextures(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GLES20.glBindTexture(target, texture);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GLES20.glPixelStorei(pname, param);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GLES20.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GLES20.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glDepthFunc(int func) {
        GLES20.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GLES20.glDepthMask(flag);
    }

    @Override
    public void glStencilOp(int sfail, int dpfail, int dppass) {
        GLES20.glStencilOp(sfail, dpfail, dppass);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        GLES20.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask) {
        GLES20.glStencilMask(mask);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GLES20.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glFlush() {
        GLES20.glFlush();
    }

    @Override
    public void glFinish() {
        GLES20.glFinish();
    }

    @Override
    public int glGetError() {
        return GLES20.glGetError();
    }

    @Nullable
    @Override
    public String glGetString(int name) {
        return GLES20.glGetString(name);
    }

    @Override
    public int glGetInteger(int pname) {
        return GLES20.glGetInteger(pname);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GLES20.glScissor(x, y, width, height);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }

    @Override
    public void glActiveTexture(int texture) {
        GLES20.glActiveTexture(texture);
    }

    @Override
    public void glBlendEquation(int mode) {
        GLES20.glBlendEquation(mode);
    }

    @Override
    public int glGenBuffers() {
        return GLES20.glGenBuffers();
    }

    @Override
    public void glDeleteBuffers(int buffer) {
        GLES20.glDeleteBuffers(buffer);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GLES20.glBindBuffer(target, buffer);
    }

    @Override
    public void glBufferData(int target, long size, long data, int usage) {
        GLES20.nglBufferData(target, size, data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, long size, long data) {
        GLES20.nglBufferSubData(target, offset, size, data);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return GLES30.glUnmapBuffer(target);
    }

    @Override
    public void glDrawBuffers(int[] bufs) {
        GLES30.glDrawBuffers(bufs);
    }

    @Override
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        GLES20.glStencilOpSeparate(face, sfail, dpfail, dppass);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        GLES20.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask) {
        GLES20.glStencilMaskSeparate(face, mask);
    }

    @Override
    public int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    @Override
    public int glCreateShader(int type) {
        return GLES20.glCreateShader(type);
    }

    @Override
    public void glDeleteShader(int shader) {
        GLES20.glDeleteShader(shader);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GLES20.glDetachShader(program, shader);
    }

    @Override
    public void glShaderSource(int shader, int count, long strings, long length) {
        GLES20.nglShaderSource(shader, count, strings, length);
    }

    @Override
    public void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    @Override
    public void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    @Override
    public void glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GLES20.glGetShaderi(shader, pname);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GLES20.glGetProgrami(program, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GLES20.glGetShaderInfoLog(shader);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return GLES20.glGetProgramInfoLog(program);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return GLES20.glGetUniformLocation(program, name);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        GLES20.glUniform1i(location, v0);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GLES30.glVertexAttribIPointer(index, size, type, stride, pointer);
    }

    @Override
    public int glGenVertexArrays() {
        return GLES30.glGenVertexArrays();
    }

    @Override
    public void glDeleteVertexArrays(int array) {
        GLES30.glDeleteVertexArrays(array);
    }

    @Override
    public void glBindVertexArray(int array) {
        GLES30.glBindVertexArray(array);
    }

    @Override
    public int glGenFramebuffers() {
        return GLES20.glGenFramebuffers();
    }

    @Override
    public void glDeleteFramebuffers(int framebuffer) {
        GLES20.glDeleteFramebuffers(framebuffer);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GLES20.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return GLES20.glCheckFramebufferStatus(target);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1,
                                  int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value) {
        GLES30.glClearBufferiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {
        GLES30.glClearBufferfv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {
        GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GLES30.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GLES30.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public int glGenRenderbuffers() {
        return GLES20.glGenRenderbuffers();
    }

    @Override
    public void glDeleteRenderbuffers(int renderbuffer) {
        GLES20.glDeleteRenderbuffers(renderbuffer);
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GLES20.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GLES20.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public long glMapBufferRange(int target, long offset, long length, int access) {
        return GLES30.nglMapBufferRange(target, offset, length, access);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        GLES30.glDrawArraysInstanced(mode, first, count, instancecount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
        GLES30.glDrawElementsInstanced(mode, count, type, indices, instancecount);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        GLES30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public int glGetUniformBlockIndex(int program, CharSequence uniformBlockName) {
        return GLES30.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public long glFenceSync(int condition, int flags) {
        return GLES30.glFenceSync(condition, flags);
    }

    @Override
    public void glDeleteSync(long sync) {
        GLES30.glDeleteSync(sync);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        return GLES30.glClientWaitSync(sync, flags, timeout);
    }

    @Override
    public int glGenSamplers() {
        return GLES30.glGenSamplers();
    }

    @Override
    public void glDeleteSamplers(int sampler) {
        GLES30.glDeleteSamplers(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        GLES30.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param) {
        GLES30.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param) {
        GLES30.glSamplerParameterf(sampler, pname, param);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        assert mDrawElementsBaseVertexSupport;
        if (mDrawElementsBaseVertexEXT) {
            EXTDrawElementsBaseVertex.nglDrawElementsBaseVertexEXT(mode, count, type, indices, basevertex);
        } else {
            GLES32.nglDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        }
    }

    @Override
    public void glDrawElementsInstancedBaseVertex(int mode, int count, int type, long indices,
                                                  int instancecount, int basevertex) {
        assert mDrawElementsBaseVertexSupport;
        if (mDrawElementsBaseVertexEXT) {
            EXTDrawElementsBaseVertex.nglDrawElementsInstancedBaseVertexEXT(mode, count, type, indices,
                    instancecount, basevertex);
        } else {
            GLES32.nglDrawElementsInstancedBaseVertex(mode, count, type, indices,
                    instancecount, basevertex);
        }
    }

    @Override
    public void glShaderBinary(IntBuffer shaders, int binaryformat, ByteBuffer binary) {
        GLES20.glShaderBinary(shaders, binaryformat, binary);
    }

    @Override
    public void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance) {
        assert mBaseInstanceSupport;
        EXTBaseInstance.glDrawArraysInstancedBaseInstanceEXT(mode, first, count, instancecount, baseinstance);
    }

    @Override
    public void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices,
                                                              int instancecount, int basevertex, int baseinstance) {
        assert mBaseInstanceSupport;
        EXTBaseInstance.nglDrawElementsInstancedBaseVertexBaseInstanceEXT(mode, count, type, indices, instancecount,
                basevertex, baseinstance);
    }

    @Override
    public void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        GLES30.glTexStorage2D(target, levels, internalformat, width, height);
    }

    @Override
    public void glInvalidateBufferSubData(int buffer, long offset, long length) {
        assert false;
    }

    @Override
    public void glInvalidateFramebuffer(int target, IntBuffer attachments) {
        GLES30.glInvalidateFramebuffer(target, attachments);
    }

    @Override
    public void glCopyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ,
                                   int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ,
                                   int srcWidth, int srcHeight, int srcDepth) {
        assert mCopyImageSupport;
        if (mCopyImageSubDataEXT) {
            EXTCopyImage.glCopyImageSubDataEXT(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget,
                    dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth);
        } else {
            GLES32.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel,
                    dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth);
        }
    }

    @Override
    public void glObjectLabel(int identifier, int name, int length, long label) {
        assert mDebugSupport;
        GLES32.nglObjectLabel(identifier, name, length, label);
    }

    @Override
    public void glObjectLabel(int identifier, int name, CharSequence label) {
        assert mDebugSupport;
        GLES32.glObjectLabel(identifier, name, label);
    }

    @Override
    public void glBindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        assert mVertexAttribBindingSupport;
        GLES31.glBindVertexBuffer(bindingindex, buffer, offset, stride);
    }

    @Override
    public void glVertexAttribFormat(int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        GLES31.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
    }

    @Override
    public void glVertexAttribIFormat(int attribindex, int size, int type, int relativeoffset) {
        GLES31.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
    }

    @Override
    public void glVertexAttribBinding(int attribindex, int bindingindex) {
        GLES31.glVertexAttribBinding(attribindex, bindingindex);
    }

    @Override
    public void glVertexBindingDivisor(int bindingindex, int divisor) {
        GLES31.glVertexBindingDivisor(bindingindex, divisor);
    }

    @Override
    public void glBufferStorage(int target, long size, long data, int flags) {
        assert mBufferStorageSupport;
        EXTBufferStorage.nglBufferStorageEXT(target, size, data, flags);
    }

    @Override
    public void glTextureBarrier() {
        assert mTextureBarrierSupport;
        NVTextureBarrier.glTextureBarrierNV();
    }

    @Override
    public int glCreateBuffers() {
        assert false;
        return 0;
    }

    @Override
    public void glNamedBufferData(int buffer, long size, long data, int usage) {
        assert false;
    }

    @Override
    public void glNamedBufferSubData(int buffer, long offset, long size, long data) {
        assert false;
    }

    @Override
    public long glMapNamedBufferRange(int buffer, long offset, long length, int access) {
        assert false;
        return 0;
    }

    @Override
    public boolean glUnmapNamedBuffer(int buffer) {
        assert false;
        return false;
    }

    @Override
    public void glNamedBufferStorage(int buffer, long size, long data, int flags) {
        assert false;
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset,
                                         long size) {
        assert false;
    }

    @Override
    public int glCreateTextures(int target) {
        assert false;
        return 0;
    }

    @Override
    public void glTextureParameteri(int texture, int pname, int param) {
        assert false;
    }

    @Override
    public void glTextureParameteriv(int texture, int pname, IntBuffer params) {
        assert false;
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int xoffset, int yoffset, int width, int height,
                                    int format, int type, long pixels) {
        assert false;
    }

    @Override
    public void glTextureStorage2D(int texture, int levels, int internalformat, int width, int height) {
        assert false;
    }

    @Override
    public int glCreateVertexArrays() {
        assert false;
        return 0;
    }

    @Override
    public void glEnableVertexArrayAttrib(int vaobj, int index) {
        assert false;
    }

    @Override
    public void glVertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, boolean normalized,
                                          int relativeoffset) {
        assert false;
    }

    @Override
    public void glVertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        assert false;
    }

    @Override
    public void glVertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        assert false;
    }

    @Override
    public void glVertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        assert false;
    }

    @Override
    public void glBindTextureUnit(int unit, int texture) {
        assert false;
    }

    @Override
    public void glSpecializeShader(int shader, CharSequence pEntryPoint, IntBuffer pConstantIndex,
                                   IntBuffer pConstantValue) {
        assert false;
    }
}
