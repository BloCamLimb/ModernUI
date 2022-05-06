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

import icyllis.arcui.core.Image;
import icyllis.arcui.core.ImageInfo;
import icyllis.arcui.hgi.*;
import org.lwjgl.opengl.EXTWindowRectangles;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.Arrays;

import static icyllis.arcui.gl.GLCore.*;
import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY;

public final class GLCaps extends Caps {

    final int[] mProgramBinaryFormats;

    final int mMaxFragmentUniformVectors;
    float mMaxTextureMaxAnisotropy = 1.f;
    final boolean mSupportsProtected = false;
    boolean mFBFetchRequiresEnablePerSample;

    // see GLTypes
    final FormatInfo[] mFormatTable = new FormatInfo[GLTypes.FORMAT_LAST_COLOR + 1];
    final int[] mColorTypeToFormatTable = new int[ImageInfo.COLOR_LAST + 1];

    // may contain null values which represent invalid/unsupported
    final GLBackendFormat[] mColorTypeToBackendFormat =
            new GLBackendFormat[ImageInfo.COLOR_LAST + 1];
    final GLBackendFormat[] mCompressionTypeToBackendFormat =
            new GLBackendFormat[Image.COMPRESSION_LAST + 1];

    /**
     * All required ARB extensions from OpenGL 3.3 to OpenGL 4.5
     */
    public static final String[] REQUIRED_EXTENSION_LIST = {
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

        final int nonMSAARenderFlags = FormatInfo.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        mFormatTable[GLTypes.FORMAT_UNKNOWN] = new FormatInfo();
        Arrays.fill(mColorTypeToFormatTable, GLTypes.FORMAT_UNKNOWN);

        // Format: RGBA8
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGBA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGBA8;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_8888;
            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            int ctIdx = 0;
            // Format: RGBA8, Surface: kRGBA_8888
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_8888] = GLTypes.FORMAT_RGBA8;

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
                mColorTypeToFormatTable[ImageInfo.COLOR_BGRA_8888] = GLTypes.FORMAT_RGBA8;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_R8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R8;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_R_8;
            info.mFlags |= FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            int ctIdx = 0;
            // Format: R8, Surface: kR_8
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_R_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_R_8] = GLTypes.FORMAT_R8;

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
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_8] = GLTypes.FORMAT_R8;

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
                ctInfo.mReadSwizzle = Swizzle.make("rrr1");
                mColorTypeToFormatTable[ImageInfo.COLOR_GRAY_8] = GLTypes.FORMAT_R8;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_ALPHA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_ALPHA_8;
            info.mInternalFormatForTexture = 0;
        }

        // Format: LUMINANCE8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_LUMINANCE8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_8;
            info.mInternalFormatForTexture = 0;
        }

        // Format: LUMINANCE8_ALPHA8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_LUMINANCE8_ALPHA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_ALPHA_88;
            info.mInternalFormatForTexture = 0;
        }

        // Format: BGRA8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_BGRA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_BGRA_8888;
            info.mInternalFormatForTexture = 0;
        }

        // Format: RGB565
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGB565] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB565;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT_5_6_5;
            info.mDefaultColorType = ImageInfo.COLOR_BGR_565;
            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB565;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGB565, Surface: kBGR_565
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGR_565;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_BGR_565] = GLTypes.FORMAT_RGB565;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGBA16F] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_RGBA16F;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_F16;

            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA16F;

            int flags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            int ctIdx = 0;
            // Format: RGBA16F, Surface: kRGBA_F16
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_F16;
                ctInfo.mFlags = flags;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_F16] = GLTypes.FORMAT_RGBA16F;

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
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_F16_CLAMPED] = GLTypes.FORMAT_RGBA16F;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_R16F] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_R16F;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_R_F16;

            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R16F;

            // Format: R16F, Surface: kAlpha_F16
            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_F16] = GLTypes.FORMAT_R16F;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_LUMINANCE16F] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_F16;
            info.mInternalFormatForTexture = 0;
        }

        // Format: RGB8
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGB8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB8;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGB_888;
            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            // Even in OpenGL 4.6 GL_RGB8 is required to be color renderable but not required to be
            // a supported render buffer format. Since we usually use render buffers for MSAA on
            // non-ES GL we don't support MSAA for GL_RGB8.
            if (glGetInternalformati(GL_RENDERBUFFER, GL_RGB8, GL_INTERNALFORMAT_SUPPORTED) == GL_TRUE) {
                info.mFlags |= msaaRenderFlags;
            } else {
                info.mFlags |= nonMSAARenderFlags;
            }
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGB8, Surface: kRGB_888x
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGB_888x;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGB_888x] = GLTypes.FORMAT_RGB8;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RG8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG8;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RG_88;
            info.mFlags |= FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RG8, Surface: kRG_88
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_88;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RG_88] = GLTypes.FORMAT_RG8;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGB10_A2] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB10_A2;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_1010102;
            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB10_A2;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            int ctIdx = 0;
            // Format: RGB10_A2, Surface: kRGBA_1010102
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_1010102] = GLTypes.FORMAT_RGB10_A2;

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
                mColorTypeToFormatTable[ImageInfo.COLOR_BGRA_1010102] = GLTypes.FORMAT_RGB10_A2;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGBA4] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGBA4;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT_4_4_4_4;
            info.mDefaultColorType = ImageInfo.COLOR_ABGR_4444;
            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA4;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGBA4, Surface: kABGR_4444
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ABGR_4444;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_ABGR_4444] = GLTypes.FORMAT_RGBA4;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_SRGB8_ALPHA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_SRGB8_ALPHA8;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_8888_SRGB;

            // We may modify the default external format below.
            info.mDefaultExternalFormat = GL_RGBA;

            info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_SRGB8_ALPHA8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_8888_SRGB] = GLTypes.FORMAT_SRGB8_ALPHA8;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGB8_BC1] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            if (caps.GL_EXT_texture_compression_s3tc) {
                info.mFlags = FormatInfo.TEXTURE_FLAG;

                mCompressionTypeToBackendFormat[Image.COMPRESSION_BC1_RGB8_UNORM] =
                        new GLBackendFormat(GL_COMPRESSED_RGB_S3TC_DXT1_EXT, GL_TEXTURE_2D);
            }

            // There are no support ColorTypes for this format
        }

        // Format: COMPRESSED_RGBA8_BC1
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGBA8_BC1] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            if (caps.GL_EXT_texture_compression_s3tc) {
                info.mFlags = FormatInfo.TEXTURE_FLAG;

                mCompressionTypeToBackendFormat[Image.COMPRESSION_BC1_RGBA8_UNORM] =
                        new GLBackendFormat(GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_TEXTURE_2D);
            }

            // There are no support ColorTypes for this format
        }

        // Format: COMPRESSED_RGB8_ETC2
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGB8_ETC2] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB8_ETC2;
            info.mFlags = FormatInfo.TEXTURE_FLAG;

            mCompressionTypeToBackendFormat[Image.COMPRESSION_ETC2_RGB8_UNORM] =
                    new GLBackendFormat(GL_COMPRESSED_RGB8_ETC2, GL_TEXTURE_2D);

            // There are no support ColorTypes for this format
        }

        // Format: R16
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_R16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R16;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_R_16;

            info.mFlags = FormatInfo.TEXTURE_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.TRANSFERS_FLAG;

            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: R16, Surface: kAlpha_16
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_16] = GLTypes.FORMAT_R16;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RG16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG16;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_RG_1616;

            info.mFlags = FormatInfo.TEXTURE_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.TRANSFERS_FLAG;

            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: GL_RG16, Surface: kRG_1616
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_1616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RG_1616] = GLTypes.FORMAT_RG16;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGBA16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;

            info.mInternalFormatForRenderbuffer = GL_RGBA16;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_16161616;

            info.mFlags = FormatInfo.TEXTURE_FLAG | msaaRenderFlags;
            info.mFlags |= FormatInfo.TRANSFERS_FLAG;

            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA16;

            // Format: GL_RGBA16, Surface: kRGBA_16161616
            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_16161616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_16161616] = GLTypes.FORMAT_RGBA16;

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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RG16F] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_RG16F;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ImageInfo.COLOR_RG_F16;

            info.mFlags |= FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;

            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG16F;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: GL_RG16F, Surface: kRG_F16
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDER_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_RG_F16] = GLTypes.FORMAT_RG16F;

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
        for (FormatInfo info : mFormatTable) {
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0) {
                // We assume that MSAA rendering is supported only if we support non-MSAA rendering.
                assert (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0;
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
            } else if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0) {
                info.mColorSampleCounts = new int[1];
                info.mColorSampleCounts[0] = 1;
            }
        }

        for (int ct = 0; ct < mColorTypeToFormatTable.length; ct++) {
            int format = mColorTypeToFormatTable[ct];
            if (format == GLTypes.FORMAT_UNKNOWN) {
                continue;
            }
            mColorTypeToBackendFormat[ct] = new GLBackendFormat(glFormatToEnum(format), GL_TEXTURE_2D);
        }

        // Validate, skip UNKNOWN
        for (int format = 1; format <= GLTypes.FORMAT_LAST_COLOR; ++format) {
            FormatInfo info = mFormatTable[format];
            // Make sure we didn't set fbo attachable with msaa and not fbo attachable
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) == 0) {
                throw new AssertionError();
            }
            // Make sure all renderbuffer formats can also be texture formats
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.TEXTURE_FLAG) == 0) {
                throw new AssertionError();
            }

            // Make sure we set all the formats' FormatType
            if (info.mFormatType == FormatInfo.FORMAT_TYPE_UNKNOWN) {
                throw new AssertionError();
            }

            // Only compressed format doesn't support glTexStorage
            if ((info.mFlags & FormatInfo.TEXTURE_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.USE_TEX_STORAGE_FLAG) == 0 &&
                    !glFormatIsCompressed(format)) {
                throw new AssertionError();
            }

            // All texture format should have their internal formats
            if ((info.mFlags & FormatInfo.TEXTURE_FLAG) != 0 &&
                    info.mInternalFormatForTexture == 0) {
                throw new AssertionError();
            }

            // Make sure if we added a ColorTypeInfo we filled it out
            for (var ctInfo : info.mColorTypeInfos) {
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
    public boolean isFormatTexturable(BackendFormat format) {
        return isFormatTexturable(format.getGLFormat());
    }

    public boolean isFormatTexturable(int format) {
        return (mFormatTable[format].mFlags & FormatInfo.TEXTURE_FLAG) != 0;
    }

    @Override
    public int getMaxRenderTargetSampleCount(BackendFormat format) {
        return getMaxRenderTargetSampleCount(format.getGLFormat());
    }

    public int getMaxRenderTargetSampleCount(int format) {
        int[] table = mFormatTable[format].mColorSampleCounts;
        if (table.length == 0) {
            return 0;
        }
        return table[table.length - 1];
    }

    @Override
    public boolean isFormatRenderable(BackendFormat format, int sampleCount, int colorType) {
        if (format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            return false;
        }
        int f = format.getGLFormat();
        if ((mFormatTable[f].getColorTypeFlags(colorType) & ColorTypeInfo.RENDER_FLAG) == 0) {
            return false;
        }
        return isFormatRenderable(f, sampleCount);
    }

    @Override
    public boolean isFormatRenderable(BackendFormat format, int sampleCount) {
        if (format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            return false;
        }
        return isFormatRenderable(format.getGLFormat(), sampleCount);
    }

    public boolean isFormatRenderable(int format, int sampleCount) {
        return sampleCount <= getMaxRenderTargetSampleCount(format);
    }

    @Override
    public int getRenderTargetSampleCount(BackendFormat format, int sampleCount) {
        return getRenderTargetSampleCount(format.getGLFormat(), sampleCount);
    }

    public int getRenderTargetSampleCount(int format, int sampleCount) {
        FormatInfo formatInfo = mFormatTable[format];
        if (formatInfo.mColorTypeInfos.length == 0) {
            return 0;
        }

        if (sampleCount == 1) {
            return formatInfo.mColorSampleCounts[0] == 1 ? 1 : 0;
        }

        for (int count : formatInfo.mColorSampleCounts) {
            if (count >= sampleCount) {
                return count;
            }
        }
        return 0;
    }

    @Override
    public boolean onFormatCompatible(BackendFormat format, int colorType) {
        FormatInfo formatInfo = mFormatTable[format.getGLFormat()];
        for (var info : formatInfo.mColorTypeInfos) {
            if (info.mColorType == colorType) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    protected BackendFormat onDefaultBackendFormat(int colorType) {
        return mColorTypeToBackendFormat[colorType];
    }

    @Nullable
    @Override
    public BackendFormat getCompressedBackendFormat(int compressionType) {
        return mCompressionTypeToBackendFormat[compressionType];
    }

    @Override
    public long getSupportedWriteColorType(int dstColorType, BackendFormat dstFormat, int srcColorType) {
        // We first try to find a supported write pixels ColorType that matches the data's
        // srcColorType. If that doesn't exists we will use any supported ColorType.
        int fallbackCT = ImageInfo.COLOR_UNKNOWN;
        FormatInfo formatInfo = mFormatTable[dstFormat.getGLFormat()];
        boolean foundSurfaceCT = false;
        long transferOffsetAlignment = 0;
        if ((formatInfo.mFlags & FormatInfo.TRANSFERS_FLAG) != 0) {
            transferOffsetAlignment = 1;
        }
        for (int i = 0; !foundSurfaceCT && i < formatInfo.mColorTypeInfos.length; ++i) {
            if (formatInfo.mColorTypeInfos[i].mColorType == dstColorType) {
                ColorTypeInfo ctInfo = formatInfo.mColorTypeInfos[i];
                foundSurfaceCT = true;
                for (var ioInfo : ctInfo.mExternalIOFormats) {
                    if (ioInfo.mExternalTexImageFormat != 0) {
                        if (ioInfo.mColorType == srcColorType) {
                            return srcColorType | (transferOffsetAlignment << 32);
                        }
                        // Currently we just pick the first supported format that we find as our
                        // fallback.
                        if (fallbackCT == ImageInfo.COLOR_UNKNOWN) {
                            fallbackCT = ioInfo.mColorType;
                        }
                    }
                }
            }
        }
        return srcColorType | (transferOffsetAlignment << 32);
    }

    public static int getExternalTypeAlignment(int type) {
        // This switch is derived from a table titled "Pixel data type parameter values and the
        // corresponding GL data types" in the OpenGL spec (Table 8.2 in OpenGL 4.5).
        return switch (type) {
            case GL_UNSIGNED_BYTE,
                    GL_BYTE,
                    GL_UNSIGNED_BYTE_2_3_3_REV,
                    GL_UNSIGNED_BYTE_3_3_2 -> 1;
            case GL_UNSIGNED_SHORT,
                    GL_SHORT,
                    GL_UNSIGNED_SHORT_1_5_5_5_REV,
                    GL_UNSIGNED_SHORT_4_4_4_4_REV,
                    GL_UNSIGNED_SHORT_5_6_5_REV,
                    GL_UNSIGNED_SHORT_5_5_5_1,
                    GL_UNSIGNED_SHORT_4_4_4_4,
                    GL_UNSIGNED_SHORT_5_6_5,
                    GL_HALF_FLOAT -> 2;
            case GL_UNSIGNED_INT,
                    GL_FLOAT_32_UNSIGNED_INT_24_8_REV,
                    GL_UNSIGNED_INT_5_9_9_9_REV,
                    GL_UNSIGNED_INT_10F_11F_11F_REV,
                    GL_UNSIGNED_INT_24_8,
                    GL_UNSIGNED_INT_10_10_10_2,
                    GL_UNSIGNED_INT_8_8_8_8_REV,
                    GL_UNSIGNED_INT_8_8_8_8,
                    GL_UNSIGNED_INT_2_10_10_10_REV,
                    GL_FLOAT,
                    GL_INT -> 4;
            default -> 0;
        };
    }

    @Override
    protected long onSupportedReadColorType(int srcColorType, BackendFormat srcFormat, int dstColorType) {
        int compression = srcFormat.getCompressionType();
        if (compression != Image.COMPRESSION_NONE) {
            return (DataUtils.compressionTypeIsOpaque(compression) ?
                    ImageInfo.COLOR_RGB_888x :
                    ImageInfo.COLOR_RGBA_8888); // alignment = 0
        }

        // We first try to find a supported read pixels ColorType that matches the requested
        // dstColorType. If that doesn't exist we will use any valid read pixels ColorType.
        int fallbackColorType = ImageInfo.COLOR_UNKNOWN;
        long fallbackTransferOffsetAlignment = 0;
        FormatInfo formatInfo = mFormatTable[srcFormat.getGLFormat()];
        for (ColorTypeInfo ctInfo : formatInfo.mColorTypeInfos) {
            if (ctInfo.mColorType == srcColorType) {
                for (ExternalIOFormat ioInfo : ctInfo.mExternalIOFormats) {
                    if (ioInfo.mExternalReadFormat != 0) {
                        long transferOffsetAlignment = 0;
                        if ((formatInfo.mFlags & FormatInfo.TRANSFERS_FLAG) != 0) {
                            transferOffsetAlignment = getExternalTypeAlignment(ioInfo.mExternalType);
                        }
                        if (ioInfo.mColorType == dstColorType) {
                            return dstColorType | (transferOffsetAlignment << 32);
                        }
                        // Currently, we just pick the first supported format that we find as our
                        // fallback.
                        if (fallbackColorType == ImageInfo.COLOR_UNKNOWN) {
                            fallbackColorType = ioInfo.mColorType;
                            fallbackTransferOffsetAlignment = transferOffsetAlignment;
                        }
                    }
                }
                break;
            }
        }
        return fallbackColorType | (fallbackTransferOffsetAlignment << 32);
    }
}

class ExternalIOFormat {

    int mColorType = ImageInfo.COLOR_UNKNOWN;

    /**
     * The external format and type are to be used when uploading/downloading data using
     * data of mColorType and uploading to a texture of a given GLFormat and its
     * intended ColorType. The mExternalTexImageFormat is the format to use for TexImage
     * calls. The mExternalReadFormat is used when calling ReadPixels. If either is zero
     * that signals that either TexImage or ReadPixels is not supported for the combination
     * of format and color types.
     */
    int mExternalType = 0;
    int mExternalTexImageFormat = 0;
    int mExternalReadFormat = 0;
}

class ColorTypeInfo {

    int mColorType = ImageInfo.COLOR_UNKNOWN;

    public static final int
            UPLOAD_DATA_FLAG = 0x1,
            RENDER_FLAG = 0x2;
    int mFlags = 0;

    short mReadSwizzle = Swizzle.RGBA;
    short mWriteSwizzle = Swizzle.RGBA;

    ExternalIOFormat[] mExternalIOFormats;
}

class FormatInfo {

    /**
     * COLOR_ATTACHMENT_FLAG: even if the format cannot be a RenderTarget, we can still attach
     * it to a framebuffer for blitting or reading pixels.
     * <p>
     * TRANSFERS_FLAG: pixel buffer objects supported in/out of this format.
     */
    public static final int
            TEXTURE_FLAG = 0x01,
            COLOR_ATTACHMENT_FLAG = 0x02,
            COLOR_ATTACHMENT_WITH_MSAA_FLAG = 0x04,
            USE_TEX_STORAGE_FLAG = 0x08,
            TRANSFERS_FLAG = 0x10;
    int mFlags = 0;

    public static final int
            FORMAT_TYPE_UNKNOWN = 0,
            FORMAT_TYPE_NORMALIZED_FIXED_POINT = 1,
            FORMAT_TYPE_FLOAT = 2;
    int mFormatType = FORMAT_TYPE_UNKNOWN;

    // Value to use as the "internalformat" argument to glTexImage or glTexStorage. It is
    // initialized in coordination with the presence/absence of the kUseTexStorage flag. In
    // other words, it is only guaranteed to be compatible with glTexImage if the flag is not
    // set and or with glTexStorage if the flag is set.
    int mInternalFormatForTexture = 0;

    // Value to use as the "internalformat" argument to glRenderbufferStorageMultisample...
    int mInternalFormatForRenderbuffer = 0;

    // Default values to use along with fInternalFormatForTexImageOrStorage for function
    // glTexImage2D when not input providing data (passing nullptr) or when clearing it by
    // uploading a block of solid color data. Not defined for compressed formats.
    int mDefaultExternalFormat = 0;
    int mDefaultExternalType = 0;
    // When the above two values are used to initialize a texture by uploading cleared data to
    // it the data should be of this color type.
    int mDefaultColorType = ImageInfo.COLOR_UNKNOWN;

    int[] mColorSampleCounts = {};

    ColorTypeInfo[] mColorTypeInfos = {};

    public int getColorTypeFlags(int colorType) {
        for (ColorTypeInfo info : mColorTypeInfos) {
            if (info.mColorType == colorType) {
                return info.mFlags;
            }
        }
        return 0;
    }
}
