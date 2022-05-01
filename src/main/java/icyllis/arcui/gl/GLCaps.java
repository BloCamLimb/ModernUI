/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.gl;

import icyllis.arcui.core.ImageInfo;
import icyllis.arcui.hgi.*;
import org.lwjgl.opengl.EXTWindowRectangles;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL45C.*;
import static org.lwjgl.opengl.GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY;

public final class GLCaps extends Caps {

    private final int[] mProgramBinaryFormats;

    private final int mMaxFragmentUniformVectors;
    private float mMaxTextureMaxAnisotropy = 1.f;
    private final boolean mSupportsProtected = false;
    private boolean mFBFetchRequiresEnablePerSample;

    // see GLTypes, default is GLFormat.UNKNOWN
    private final GLFormat[] mColorTypeToFormatTable = new GLFormat[ImageInfo.COLOR_LAST + 1];

    /**
     * All required ARB extensions from OpenGL 3.3 to OpenGL 4.5
     */
    public static final String[] REQUIRED_ARB_EXTENSIONS = {
            "ARB_blend_func_extended",
            "ARB_sampler_objects",
            "ARB_explicit_attrib_location",
            "ARB_instanced_arrays",
            "ARB_texture_swizzle",
            "ARB_tessellation_shader",
            "ARB_draw_indirect",
            "ARB_ES2_compatibility",
            "ARB_get_program_binary",
            "ARB_base_instance",
            "ARB_texture_storage",
            "ARB_internalformat_query",
            "ARB_invalidate_subdata",
            "ARB_multi_draw_indirect",
            "ARB_explicit_uniform_location",
            "ARB_vertex_attrib_binding",
            "ARB_ES3_compatibility",
            "ARB_clear_texture",
            "ARB_buffer_storage",
            "ARB_texture_barrier",
            "ARB_direct_state_access"
    };

    public GLCaps(ContextOptions options, GLCapabilities caps) {
        super(options);
        // we must have OpenGL 4.5, if not, test ARBs.
        if (!caps.OpenGL45) {
            // we don't check CONTEXT_PROFILE_MASK, we assume it's always core profile.
            if (!caps.OpenGL32) {
                throw new AssertionError("OpenGL 3.2 core profile is unavailable");
            }
            if (!caps.OpenGL33) {
                if (!caps.GL_ARB_blend_func_extended) {
                    throw new AssertionError("ARB_blend_func_extended is unavailable");
                }
                if (!caps.GL_ARB_sampler_objects) {
                    throw new AssertionError("ARB_sampler_objects is unavailable");
                }
                if (!caps.GL_ARB_explicit_attrib_location) {
                    throw new AssertionError("ARB_explicit_attrib_location is unavailable");
                }
                if (!caps.GL_ARB_instanced_arrays) {
                    throw new AssertionError("ARB_instanced_arrays is unavailable");
                }
                if (!caps.GL_ARB_texture_swizzle) {
                    throw new AssertionError("ARB_texture_swizzle is unavailable");
                }
            }
            if (!caps.OpenGL40) {
                if (!caps.GL_ARB_tessellation_shader) {
                    throw new AssertionError("ARB_tessellation_shader is unavailable");
                }
                if (!caps.GL_ARB_draw_indirect) {
                    throw new AssertionError("ARB_draw_indirect is unavailable");
                }
            }
            if (!caps.OpenGL41) {
                if (!caps.GL_ARB_ES2_compatibility) {
                    throw new AssertionError("ARB_ES2_compatibility is unavailable");
                }
                if (!caps.GL_ARB_get_program_binary) {
                    throw new AssertionError("ARB_get_program_binary is unavailable");
                }
            }
            if (!caps.OpenGL42) {
                if (!caps.GL_ARB_base_instance) {
                    throw new AssertionError("ARB_base_instance is unavailable");
                }
                if (!caps.GL_ARB_texture_storage) {
                    throw new AssertionError("ARB_texture_storage is unavailable");
                }
                if (!caps.GL_ARB_internalformat_query) {
                    throw new AssertionError("ARB_internalformat_query is unavailable");
                }
            }
            if (!caps.OpenGL43) {
                if (!caps.GL_ARB_invalidate_subdata) {
                    throw new AssertionError("ARB_invalidate_subdata is unavailable");
                }
                if (!caps.GL_ARB_multi_draw_indirect) {
                    throw new AssertionError("ARB_multi_draw_indirect is unavailable");
                }
                if (!caps.GL_ARB_explicit_uniform_location) {
                    throw new AssertionError("ARB_explicit_uniform_location is unavailable");
                }
                if (!caps.GL_ARB_vertex_attrib_binding) {
                    throw new AssertionError("ARB_vertex_attrib_binding is unavailable");
                }
                if (!caps.GL_ARB_ES3_compatibility) {
                    throw new AssertionError("ARB_ES3_compatibility is unavailable");
                }
            }
            if (!caps.OpenGL44) {
                if (!caps.GL_ARB_clear_texture) {
                    throw new AssertionError("ARB_clear_texture is unavailable");
                }
                if (!caps.GL_ARB_buffer_storage) {
                    throw new AssertionError("ARB_buffer_storage is unavailable");
                }
            }
            if (!caps.GL_ARB_texture_barrier) {
                throw new AssertionError("ARB_texture_barrier is unavailable");
            }
            if (!caps.GL_ARB_direct_state_access) {
                throw new AssertionError("ARB_direct_state_access is unavailable");
            }
        }

        mMaxFragmentUniformVectors = glGetInteger(GL_MAX_FRAGMENT_UNIFORM_VECTORS);
        mMaxVertexAttributes = glGetInteger(GL_MAX_VERTEX_ATTRIBS);

        mTransferPixelsToRowBytesSupport = true;

        // When we are abandoning the context we cannot call into GL thus we should skip any sync work.
        mMustSyncGpuDuringDiscard = false;

        if (caps.GL_EXT_shader_framebuffer_fetch) {
            mShaderCaps.mFBFetchNeedsCustomOutput = true;
            mShaderCaps.mFBFetchSupport = true;
            mShaderCaps.mFBFetchColorName = "gl_LastFragData[0]";
            mShaderCaps.mFBFetchExtensionString = "GL_EXT_shader_framebuffer_fetch";
            mFBFetchRequiresEnablePerSample = false;
        }

        mShaderCaps.mMaxTessellationSegments = glGetInteger(GL_MAX_TESS_GEN_LEVEL);
        mShaderCaps.mVersionDeclString = "#version 450\n";
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer range = stack.mallocInt(2);
            int bits = glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, range);
            mShaderCaps.mFloatIs32Bits = range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;
            bits = glGetShaderPrecisionFormat(GL_VERTEX_SHADER, GL_HIGH_FLOAT, range);
            mShaderCaps.mFloatIs32Bits &= range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;

            bits = glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_MEDIUM_FLOAT, range);
            mShaderCaps.mHalfIs32Bits = range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;
            bits = glGetShaderPrecisionFormat(GL_VERTEX_SHADER, GL_MEDIUM_FLOAT, range);
            mShaderCaps.mHalfIs32Bits &= range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;
        }
        mShaderCaps.mDualSourceBlendingSupport = true;

        if (caps.GL_NV_conservative_raster) {
            mConservativeRasterSupport = true;
        }

        mShaderCaps.mMaxFragmentSamplers = Math.min(32, glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));

        if (caps.GL_NV_blend_equation_advanced_coherent) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED_COHERENT;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_AUTOMATIC;
        } else if (caps.GL_KHR_blend_equation_advanced_coherent) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED_COHERENT;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_GENERAL_ENABLE;
        } else if (caps.GL_NV_blend_equation_advanced) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_AUTOMATIC;
        } else if (caps.GL_KHR_blend_equation_advanced) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_GENERAL_ENABLE;
        }

        mAnisoSupport = caps.OpenGL46 ||
                caps.GL_ARB_texture_filter_anisotropic ||
                caps.GL_EXT_texture_filter_anisotropic;
        if (mAnisoSupport) {
            mMaxTextureMaxAnisotropy = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        }

        mMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        mMaxRenderTargetSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);
        mMaxPreferredRenderTargetSize = mMaxRenderTargetSize;

        mGpuTracingSupport = caps.GL_EXT_debug_marker;

        if (caps.GL_EXT_window_rectangles) {
            mMaxWindowRectangles = glGetInteger(EXTWindowRectangles.GL_MAX_WINDOW_RECTANGLES_EXT);
        }

        mNativeDrawIndirectSupport = true;

        mDynamicStateArrayGeometryProcessorTextureSupport = true;

        int count = glGetInteger(GL_NUM_PROGRAM_BINARY_FORMATS);
        if (count == 0) {
            throw new AssertionError("No program binary formats are available");
        }
        mProgramBinaryFormats = new int[count];
        glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, mProgramBinaryFormats);

        //// FORMAT

        final int nonMSAARenderFlags = GLFormat.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | GLFormat.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        // Format: RGBA8
        {
            GLFormat info = GLFormat.RGBA8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGBA8;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_8888;
            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            int ctIdx = 0;
            // Format: RGBA8, Surface: kRGBA_8888
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_8888] = GLFormat.RGBA8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGBA8, Surface: kRGBA_8888, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
                // Format: RGBA8, Surface: kRGBA_8888, Data: kBGRA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGRA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_BGRA; // TODO: review
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }
            }

            // Format: RGBA8, Surface: kBGRA_8888
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGRA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_BGRA_8888] = GLFormat.RGBA8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGBA8, Surface: kBGRA_8888, Data: kBGRA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGRA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }

                // Format: RGBA8, Surface: kBGRA_8888, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: RGBA8, Surface: kRGB_888x
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGB_888x;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.RGB1;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                int ioIdx = 0;
                // Format: RGBA8, Surface: kRGB_888x, Data: kRGBA_888x
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGB_888x;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: R8
        {
            GLFormat info = GLFormat.R8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R8;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_R_8;
            info.mFlags |= GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            int ctIdx = 0;
            // Format: R8, Surface: kR_8
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_R_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_R_8] = GLFormat.R8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: R8, Surface: kR_8, Data: kR_8
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_R_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kR_8, Data: kR_8xxx
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_R_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: R8, Surface: kAlpha_8
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                ctInfo.mReadSwizzle = Swizzle.pack("000r");
                ctInfo.mWriteSwizzle = Swizzle.pack("a000");
                mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_8] = GLFormat.R8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: R8, Surface: kAlpha_8, Data: kAlpha_8
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kAlpha_8, Data: kAlpha_8xxx
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: R8, Surface: kGray_8
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_GRAY_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.pack("rrr1");
                mColorTypeToFormatTable[ImageInfo.COLOR_GRAY_8] = GLFormat.R8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: R8, Surface: kGray_8, Data: kGray_8
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_GRAY_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kGray_8, Data: kGray_8xxx
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_GRAY_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: ALPHA8, DEPRECATED
        {
            GLFormat info = GLFormat.ALPHA8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_ALPHA_8;
            info.mInternalFormatForTexture = 0;
        }

        // Format: LUMINANCE8, DEPRECATED
        {
            GLFormat info = GLFormat.LUMINANCE8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_8;
            info.mInternalFormatForTexture = 0;
        }

        // Format: LUMINANCE8_ALPHA8, DEPRECATED
        {
            GLFormat info = GLFormat.LUMINANCE8_ALPHA8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_ALPHA_88;
            info.mInternalFormatForTexture = 0;
        }

        // Format: BGRA8, DEPRECATED
        {
            GLFormat info = GLFormat.BGRA8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_BGRA_8888;
            info.mInternalFormatForTexture = 0;
        }

        // Format: RGB565
        {
            GLFormat info = GLFormat.RGB565;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB565;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT_5_6_5;
            info.mDefaultColorType = ImageInfo.COLOR_BGR_565;
            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB565;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGB565, Surface: kBGR_565
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGR_565;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_BGR_565] = GLFormat.RGB565;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGB565, Surface: kBGR_565, Data: kBGR_565
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGR_565;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT_5_6_5;
                    ioFormat.mExternalTexImageFormat = GL_RGB;
                    ioFormat.mExternalReadFormat = GL_RGB;
                }

                // Format: RGB565, Surface: kBGR_565, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGBA16F
        {
            GLFormat info = GLFormat.RGBA16F;
            info.mFormatType = GLFormat.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_RGBA16F;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_F16;

            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA16F;

            int flags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            int ctIdx = 0;
            // Format: RGBA16F, Surface: kRGBA_F16
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_F16;
                ctInfo.mFlags = flags;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_F16] = GLFormat.RGBA16F;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGBA16F, Surface: kRGBA_F16, Data: kRGBA_F16
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA16F, Surface: kRGBA_F16, Data: kRGBA_F32
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: RGBA16F, Surface: kRGBA_F16_Clamped
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_F16_CLAMPED;
                ctInfo.mFlags = flags;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_F16_CLAMPED] = GLFormat.RGBA16F;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGBA16F, Surface: kRGBA_F16_Clamped, Data: kRGBA_F16_Clamped
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F16_CLAMPED;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA16F, Surface: kRGBA_F16_Clamped, Data: kRGBA_F32
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: R16F
        {
            GLFormat info = GLFormat.R16F;
            info.mFormatType = GLFormat.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_R16F;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_R_F16;

            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R16F;

            // Format: R16F, Surface: kAlpha_F16
            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                ctInfo.mReadSwizzle = Swizzle.pack("000r");
                ctInfo.mWriteSwizzle = Swizzle.pack("a000");
                mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_F16] = GLFormat.R16F;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: R16F, Surface: kAlpha_F16, Data: kAlpha_F16
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R16F, Surface: kAlpha_F16, Data: kAlpha_F32xxx
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_F32xxx;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: LUMINANCE16F, DEPRECATED
        {
            GLFormat info = GLFormat.LUMINANCE16F;
            info.mFormatType = GLFormat.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_F16;
            info.mInternalFormatForTexture = 0;
        }

        // Format: RGB8
        {
            GLFormat info = GLFormat.RGB8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB8;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGB_888;
            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            // Even in OpenGL 4.6 GL_RGB8 is required to be color renderable but not required to be
            // a supported render buffer format. Since we usually use render buffers for MSAA on
            // non-ES GL we don't support MSAA for GL_RGB8.
            if (glGetInternalformati(GL_RENDERBUFFER, GL_RGB8, GL_INTERNALFORMAT_SUPPORTED) == GL_TRUE) {
                info.mFlags |= msaaRenderFlags;
            } else {
                info.mFlags |= nonMSAARenderFlags;
            }
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGB8, Surface: kRGB_888x
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGB_888x;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;

                int idx = ImageInfo.COLOR_RGB_888x;
                if (mColorTypeToFormatTable[idx] == GLFormat.UNKNOWN) {
                    mColorTypeToFormatTable[ImageInfo.COLOR_RGB_888x] = GLFormat.RGB8;
                }

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGB8, Surface: kRGB_888x, Data: kRGB_888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGB_888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGB;
                    ioFormat.mExternalReadFormat = 0;
                }

                // Format: RGB8, Surface: kRGB_888x, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RG8
        {
            GLFormat info = GLFormat.RG8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG8;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RG_88;
            info.mFlags |= GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RG8, Surface: kRG_88
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_88;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RG_88] = GLFormat.RG8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RG8, Surface: kRG_88, Data: kRG_88
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RG_88;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: RG8, Surface: kRG_88, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGB10_A2
        {
            GLFormat info = GLFormat.RGB10_A2;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB10_A2;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_1010102;
            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB10_A2;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            int ctIdx = 0;
            // Format: RGB10_A2, Surface: kRGBA_1010102
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_1010102] = GLFormat.RGB10_A2;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_1010102
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_1010102;
                    ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
            //------------------------------------------------------------------
            // Format: RGB10_A2, Surface: kBGRA_1010102
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGRA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_BGRA_1010102] = GLFormat.RGB10_A2;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kBGRA_1010102
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGRA_1010102;
                    ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                    ioFormat.mExternalTexImageFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }

                // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGBA4
        {
            GLFormat info = GLFormat.RGBA4;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGBA4;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT_4_4_4_4;
            info.mDefaultColorType = ImageInfo.COLOR_ABGR_4444;
            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA4;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGBA4, Surface: kABGR_4444
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ABGR_4444;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_ABGR_4444] = GLFormat.RGBA4;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: RGBA4, Surface: kABGR_4444, Data: kABGR_4444
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ABGR_4444;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT_4_4_4_4;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA4, Surface: kABGR_4444, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: SRGB8_ALPHA8
        {
            GLFormat info = GLFormat.SRGB8_ALPHA8;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_SRGB8_ALPHA8;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_8888_SRGB;

            // We may modify the default external format below.
            info.mDefaultExternalFormat = GL_RGBA;

            info.mFlags = GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_SRGB8_ALPHA8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_8888_SRGB] = GLFormat.SRGB8_ALPHA8;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                int ioIdx = 0;

                // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB, Data: kRGBA_8888_SRGB
                {
                    // GL does not do srgb<->rgb conversions when transferring between cpu and gpu.
                    // Thus, the external format is GL_RGBA. See below for note about ES2.0 and
                    // glTex[Sub]Image.

                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: COMPRESSED_RGB8_BC1
        {
            GLFormat info = GLFormat.COMPRESSED_RGB8_BC1;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            if (caps.GL_EXT_texture_compression_s3tc) {
                info.mFlags = GLFormat.TEXTURE_FLAG;
            }

            // There are no support GrColorTypes for this format
        }

        // Format: COMPRESSED_RGBA8_BC1
        {
            GLFormat info = GLFormat.COMPRESSED_RGBA8_BC1;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            if (caps.GL_EXT_texture_compression_s3tc) {
                info.mFlags = GLFormat.TEXTURE_FLAG;
            }

            // There are no support GrColorTypes for this format
        }

        // Format: COMPRESSED_RGB8_ETC2
        {
            GLFormat info = GLFormat.COMPRESSED_RGB8_ETC2;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB8_ETC2;
            info.mFlags = GLFormat.TEXTURE_FLAG;

            // There are no support GrColorTypes for this format
        }

        // Format: R16
        {
            GLFormat info = GLFormat.R16;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R16;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_R_16;

            info.mFlags = GLFormat.TEXTURE_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.TRANSFERS_FLAG;

            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: R16, Surface: kAlpha_16
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                ctInfo.mReadSwizzle = Swizzle.pack("000r");
                ctInfo.mWriteSwizzle = Swizzle.pack("a000");
                mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_16] = GLFormat.R16;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: R16, Surface: kAlpha_16, Data: kAlpha_16
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_16;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R16, Surface: kAlpha_16, Data: kAlpha_8xxx
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RG16
        {
            GLFormat info = GLFormat.RG16;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG16;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_RG_1616;

            info.mFlags = GLFormat.TEXTURE_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.TRANSFERS_FLAG;

            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: GL_RG16, Surface: kRG_1616
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_1616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RG_1616] = GLFormat.RG16;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: GL_RG16, Surface: kRG_1616, Data: kRG_1616
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RG_1616;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: GL_RG16, Surface: kRG_1616, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGBA16
        {
            GLFormat info = GLFormat.RGBA16;
            info.mFormatType = GLFormat.FORMAT_TYPE_NORMALIZED_FIXED_POINT;

            info.mInternalFormatForRenderbuffer = GL_RGBA16;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_16161616;

            info.mFlags = GLFormat.TEXTURE_FLAG | msaaRenderFlags;
            info.mFlags |= GLFormat.TRANSFERS_FLAG;

            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA16;

            // Format: GL_RGBA16, Surface: kRGBA_16161616
            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_16161616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_16161616] = GLFormat.RGBA16;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_16161616
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_16161616;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_8888
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format:RG16F
        {
            GLFormat info = GLFormat.RG16F;
            info.mFormatType = GLFormat.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_RG16F;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_RG_F16;

            info.mFlags |= GLFormat.TEXTURE_FLAG | GLFormat.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;

            info.mFlags |= GLFormat.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG16F;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: GL_RG16F, Surface: kRG_F16
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RG_F16] = GLFormat.RG16F;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                int ioIdx = 0;
                // Format: GL_RG16F, Surface: kRG_F16, Data: kRG_F16
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RG_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: GL_RG16F, Surface: kRG_F16, Data: kRGBA_F32
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Init samples
        for (GLFormat info : GLFormat.COLOR_TABLE) {
            if ((info.mFlags & GLFormat.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0) {
                // We assume that MSAA rendering is supported only if we support non-MSAA rendering.
                assert (info.mFlags & GLFormat.COLOR_ATTACHMENT_FLAG) != 0;
                int glFormat = info.mInternalFormatForRenderbuffer;
                count = glGetInternalformati(GL_RENDERBUFFER, glFormat, GL_NUM_SAMPLE_COUNTS);
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
            } else if ((info.mFlags & GLFormat.COLOR_ATTACHMENT_FLAG) != 0) {
                info.mColorSampleCounts = new int[1];
                info.mColorSampleCounts[0] = 1;
            }
        }

        // Validate
        for (GLFormat format : GLFormat.COLOR_TABLE) {
            if (format == GLFormat.UNKNOWN) {
                continue;
            }
            // Make sure we didn't set fbo attachable with msaa and not fbo attachable
            if ((format.mFlags & GLFormat.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0 &&
                    (format.mFlags & GLFormat.COLOR_ATTACHMENT_FLAG) == 0) {
                throw new AssertionError();
            }
            // Make sure all renderbuffer formats can also be texture formats
            if ((format.mFlags & GLFormat.COLOR_ATTACHMENT_FLAG) != 0 &&
                    (format.mFlags & GLFormat.TEXTURE_FLAG) == 0) {
                throw new AssertionError();
            }

            // Make sure we set all the formats' FormatType
            if (format.mFormatType == GLFormat.FORMAT_TYPE_UNKNOWN) {
                throw new AssertionError();
            }

            // Only compressed format doesn't support glTexStorage
            if ((format.mFlags & GLFormat.TEXTURE_FLAG) != 0 &&
                    (format.mFlags & GLFormat.USE_TEX_STORAGE_FLAG) == 0 &&
                    !GLUtil.glFormatIsCompressed(format)) {
                throw new AssertionError();
            }

            // All texture format should have their internal formats
            if ((format.mFlags & GLFormat.TEXTURE_FLAG) != 0 &&
                    format.mInternalFormatForTexture == 0) {
                throw new AssertionError();
            }

            // Make sure if we added a ColorTypeInfo we filled it out
            for (var ctInfo : format.mColorTypeInfos) {
                if (ctInfo.mColorType == ImageInfo.COLOR_UNKNOWN) {
                    throw new AssertionError();
                }
                // Seems silly to add a color type if we don't support any flags on it
                if (ctInfo.mFlags == 0) {
                    throw new AssertionError();
                }
                // Make sure if we added any ExternalIOFormats we filled it out
                for (var ioInfo : ctInfo.mExternalIOFormats) {
                    if (ioInfo.mColorType == ImageInfo.COLOR_UNKNOWN) {
                        throw new AssertionError();
                    }
                }
            }
        }

        finishInitialization(options);

        // For now these two are equivalent, but we could have dst read in shader via some other method.
        mShaderCaps.mDstReadInShaderSupport = mShaderCaps.mFBFetchSupport;
    }

    @Override
    public boolean isColorFormat(BackendFormat format) {
        return isColorFormat(format.getGLFormat());
    }

    public boolean isColorFormat(GLFormat format) {
        return (format.mFlags & GLFormat.TEXTURE_FLAG) != 0;
    }

    @Override
    public int getMaxRenderTargetSampleCount(BackendFormat format) {
        return getMaxRenderTargetSampleCount(format.getGLFormat());
    }

    public int getMaxRenderTargetSampleCount(GLFormat format) {
        int[] table = format.mColorSampleCounts;
        if (table.length == 0) {
            return 0;
        }
        return table[table.length - 1];
    }

    @Override
    public boolean isRenderFormat(BackendFormat format, int sampleCount, int colorType) {
        if (format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            return false;
        }
        GLFormat f = format.getGLFormat();
        if ((f.getColorTypeFlags(colorType) & ColorTypeInfo.RENDER_FLAG) == 0) {
            return false;
        }
        return isRenderFormat(f, sampleCount);
    }

    @Override
    public boolean isRenderFormat(BackendFormat format, int sampleCount) {
        if (format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            return false;
        }
        return isRenderFormat(format.getGLFormat(), sampleCount);
    }

    public boolean isRenderFormat(GLFormat format, int sampleCount) {
        return sampleCount <= getMaxRenderTargetSampleCount(format);
    }

    @Override
    public int getRenderTargetSampleCount(BackendFormat format, int sampleCount) {
        return getRenderTargetSampleCount(format.getGLFormat(), sampleCount);
    }

    public int getRenderTargetSampleCount(GLFormat format, int sampleCount) {
        if (format.mColorTypeInfos.length == 0) {
            return 0;
        }

        if (sampleCount == 1) {
            return format.mColorSampleCounts[0] == 1 ? 1 : 0;
        }

        for (int count : format.mColorSampleCounts) {
            if (count >= sampleCount) {
                return count;
            }
        }
        return 0;
    }
}
