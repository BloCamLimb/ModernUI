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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.engine.ShaderCaps;
import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.NVTextureBarrier;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL46C.*;

public final class GLCaps_GL46C extends GLCaps implements GLInterface {

    private boolean mTextureBarrierNV;

    @VisibleForTesting
    public GLCaps_GL46C(ContextOptions options, Object capabilities) {
        super(options);
        GLCapabilities caps = (GLCapabilities) capabilities;
        List<String> missingExtensions = MISSING_EXTENSIONS;
        missingExtensions.clear();
        if (!caps.OpenGL30) {
            throw new UnsupportedOperationException("OpenGL 3.0 is unavailable");
        }
        if (!caps.OpenGL31) {
            if (!caps.GL_ARB_uniform_buffer_object) {
                missingExtensions.add("ARB_uniform_buffer_object");
            }
            if (!caps.GL_ARB_copy_buffer) {
                missingExtensions.add("ARB_copy_buffer");
            }
            if (!caps.GL_ARB_draw_instanced) {
                missingExtensions.add("ARB_draw_instanced");
            }
        }
        if (!caps.OpenGL32) {
            if (!caps.GL_ARB_sync) {
                missingExtensions.add("ARB_sync");
            }
        }
        if (!caps.OpenGL33) {
            if (!caps.GL_ARB_sampler_objects) {
                missingExtensions.add("ARB_sampler_objects");
            }
            if (!caps.GL_ARB_explicit_attrib_location) {
                missingExtensions.add("ARB_explicit_attrib_location");
            }
            if (!caps.GL_ARB_instanced_arrays) {
                missingExtensions.add("ARB_instanced_arrays");
            }
            if (!caps.GL_ARB_texture_swizzle) {
                missingExtensions.add("ARB_texture_swizzle");
            }
        }
        // OpenGL 3.3 is the minimum requirement
        // Note that having these extensions does not mean OpenGL 3.3 is available
        // But these are required and they are available in OpenGL ES 3.0
        if (!missingExtensions.isEmpty()) {
            throw new UnsupportedOperationException("Missing required extensions: " + missingExtensions);
        }

        /*if (!caps.GL_ARB_draw_elements_base_vertex) {
                    missingExtensions.add("ARB_draw_elements_base_vertex");
                }*/

        if (!caps.OpenGL41) {
            // macOS supports this
            if (!caps.GL_ARB_viewport_array) {
                missingExtensions.add("ARB_viewport_array");
            }
        }
        if (!caps.OpenGL43) {
            if (!caps.GL_ARB_vertex_attrib_binding) {
                missingExtensions.add("ARB_vertex_attrib_binding");
            }
        }
        if (!caps.OpenGL44) {
            if (!caps.GL_ARB_clear_texture) {
                missingExtensions.add("ARB_clear_texture");
            }
        }
        if (caps.OpenGL45 || caps.GL_ARB_texture_barrier) {
            mTextureBarrierSupport = true;
            mTextureBarrierNV = false;
        } else if (caps.GL_NV_texture_barrier) {
            mTextureBarrierSupport = true;
            mTextureBarrierNV = true;
            GLUtil.info(options, "Use NV_texture_barrier");
        } else {
            mTextureBarrierSupport = false;
        }
        mDSASupport = caps.OpenGL45 || caps.GL_ARB_direct_state_access;

        mDebugSupport = caps.OpenGL43 || caps.GL_KHR_debug;
        mBaseInstanceSupport = caps.OpenGL42 || caps.GL_ARB_base_instance;
        mCopyImageSupport = caps.OpenGL43 || caps.GL_ARB_copy_image;
        mViewCompatibilityClassSupport = caps.OpenGL43 || caps.GL_ARB_internalformat_query2;
        mProgramBinarySupport = caps.OpenGL41 || caps.GL_ARB_get_program_binary;
        mProgramParameterSupport = mProgramBinarySupport;
        mBufferStorageSupport = caps.OpenGL44 || caps.GL_ARB_buffer_storage;

        String versionString = glGetString(GL_VERSION);
        String vendorString = glGetString(GL_VENDOR);
        mVendor = GLUtil.findVendor(vendorString);
        mDriver = GLUtil.findDriver(mVendor, vendorString, versionString);
        GLUtil.info(options, "Identified vendor: " + mVendor);
        GLUtil.info(options, "Identified driver: " + mDriver);

        // macOS supports this
        if (caps.OpenGL41 || caps.GL_ARB_ES2_compatibility) {
            mMaxFragmentUniformVectors = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_VECTORS);
        } else {
            mMaxFragmentUniformVectors = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS) / 4;
        }
        mMaxVertexAttributes = Math.min(32, glGetInteger(GL_MAX_VERTEX_ATTRIBS));

        if (caps.OpenGL43 || caps.GL_ARB_invalidate_subdata) {
            mInvalidateBufferType = INVALIDATE_BUFFER_TYPE_INVALIDATE;
        } else {
            mInvalidateBufferType = INVALIDATE_BUFFER_TYPE_NULL_DATA;
        }

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
        shaderCaps.mDualSourceBlendingSupport = caps.OpenGL33 || caps.GL_ARB_blend_func_extended;

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
            mMaxTextureMaxAnisotropy = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY);
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
        // we don't use ARB_GL_SPIRV, so check OpenGL 4.6 support
        if (caps.OpenGL46) {
            int count = glGetInteger(GL_NUM_SHADER_BINARY_FORMATS);
            if (count > 0) {
                int[] shaderBinaryFormats = new int[count];
                glGetIntegerv(GL_SHADER_BINARY_FORMATS, shaderBinaryFormats);
                for (int format : shaderBinaryFormats) {
                    if (format == GL_SHADER_BINARY_FORMAT_SPIR_V) {
                        mSPIRVSupport = true;
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
        // macOS supports this
        boolean textureStorageSupported =
                caps.OpenGL42 || caps.GL_ARB_texture_storage;
        super.initFormatTable(textureStorageSupported, caps.GL_EXT_texture_compression_s3tc);

        final int nonMSAARenderFlags = FormatInfo.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        // Format: RGB565
        if (caps.OpenGL42 || caps.GL_ARB_ES2_compatibility) {
            // macOS supports this
            FormatInfo info = getFormatInfo(GL_RGB565);
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
        }

        // Format: RGB8
        {
            FormatInfo info = getFormatInfo(GL_RGB8);
            // Even in OpenGL 4.6 GL_RGB8 is required to be color renderable but not required to be
            // a supported render buffer format. Since we usually use render buffers for MSAA on
            // we don't support MSAA for GL_RGB8.
            if ((caps.OpenGL43 || caps.GL_ARB_internalformat_query2) &&
                    glGetInternalformati(GL_RENDERBUFFER, GL_RGB8, GL_INTERNALFORMAT_SUPPORTED) == GL_TRUE) {
                info.mFlags |= msaaRenderFlags;
            } else {
                info.mFlags |= nonMSAARenderFlags;
            }
        }

        // Format: COMPRESSED_RGB8_ETC2
        if (caps.OpenGL43 || caps.GL_ARB_ES3_compatibility) {
            FormatInfo info = getFormatInfo(GL_COMPRESSED_RGB8_ETC2);
            info.mFlags = FormatInfo.TEXTURABLE_FLAG;
        }

        // Init samples
        for (FormatInfo info : mFormatTable) {
            if (mCopyImageSupport && mViewCompatibilityClassSupport && info.mInternalFormatForTexture != 0) {
                info.mViewCompatibilityClass = glGetInternalformati(
                        GL_TEXTURE_2D, info.mInternalFormatForTexture, GL_VIEW_COMPATIBILITY_CLASS
                );
            }
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0) {
                // We assume that MSAA rendering is supported only if we support non-MSAA rendering.
                assert (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0;
                // macOS supports this
                if (caps.OpenGL42 || caps.GL_ARB_internalformat_query) {
                    int glFormat = info.mInternalFormatForRenderbuffer;
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
                } else {
                    int maxSampleCnt = Math.max(1, glGetInteger(GL_MAX_SAMPLES));
                    int count = 4; // [1, 2, 4, 8]
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
        if (mTextureBarrierNV) {
            NVTextureBarrier.glTextureBarrierNV();
        } else {
            glTextureBarrier();
        }
    }
}
