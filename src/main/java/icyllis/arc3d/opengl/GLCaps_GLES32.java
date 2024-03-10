/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.compiler.GLSLVersion;
import icyllis.arc3d.compiler.TargetApi;
import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.opengles.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengles.GLES32.*;

public final class GLCaps_GLES32 extends GLCaps implements GLInterface {

    @VisibleForTesting
    public GLCaps_GLES32(ContextOptions options, Object capabilities) {
        super(options);
        GLESCapabilities caps = (GLESCapabilities) capabilities;
        MISSING_EXTENSIONS.clear();
        // OpenGL ES 3.0 is the minimum requirement
        if (!caps.GLES30) {
            throw new UnsupportedOperationException("OpenGL ES 3.0 is unavailable");
        }

        if (caps.GL_NV_texture_barrier) {
            mTextureBarrierSupport = true;
            GLUtil.info(options, "Use NV_texture_barrier");
        } else {
            mTextureBarrierSupport = false;
        }
        mDSASupport = false;

        mDebugSupport = caps.GLES32;
        mBaseInstanceSupport = caps.GL_EXT_base_instance;
        mCopyImageSupport = caps.GL_EXT_copy_image;
        mViewCompatibilityClassSupport = false;
        // OpenGL ES 3.0
        mProgramBinarySupport = true;
        mProgramParameterSupport = true;
        mBufferStorageSupport = caps.GL_EXT_buffer_storage;

        String versionString = glGetString(GL_VERSION);
        String vendorString = glGetString(GL_VENDOR);
        mVendor = GLUtil.findVendor(vendorString);
        mDriver = GLUtil.findDriver(mVendor, vendorString, versionString);
        GLUtil.info(options, "Identified vendor: " + mVendor);
        GLUtil.info(options, "Identified driver: " + mDriver);

        mMaxFragmentUniformVectors = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_VECTORS);
        mMaxVertexAttributes = Math.min(32, glGetInteger(GL_MAX_VERTEX_ATTRIBS));

        mInvalidateBufferType = INVALIDATE_BUFFER_TYPE_NULL_DATA;

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
        if (caps.GLES31) {
            shaderCaps.mTargetApi = TargetApi.OPENGL_ES_3_1;
        } else {
            shaderCaps.mTargetApi = TargetApi.OPENGL_ES_3_0;
        }
        final int glslVersion;
        if (caps.GLES32) {
            glslVersion = 320;
        } else if (caps.GLES31) {
            glslVersion = 310;
        } else {
            glslVersion = 300;
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

        mAnisotropySupport = caps.GL_EXT_texture_filter_anisotropic;
        if (mAnisotropySupport) {
            mMaxTextureMaxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
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
                glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, mProgramBinaryFormats);
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
    }

    void initFormatTable(GLESCapabilities caps) {
        // textureStorageSupported - OpenGL ES 3.0
        super.initFormatTable(true, caps.GL_EXT_texture_compression_s3tc);

        final int nonMSAARenderFlags = FormatInfo.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        // Format: RGB565
        {
            FormatInfo info = getFormatInfo(GL_RGB565);
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG;
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

    @Nullable
    @Override
    public String GetString(int name) {
        return glGetString(name);
    }

    @Override
    public int GetInteger(int pname) {
        return glGetInteger(pname);
    }

    @Override
    public void TextureBarrier() {
        assert mTextureBarrierSupport;
        NVTextureBarrier.glTextureBarrierNV();
    }
}
