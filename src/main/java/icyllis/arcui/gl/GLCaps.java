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

import static org.lwjgl.opengl.GL45C.*;
import static org.lwjgl.opengl.GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY;

public final class GLCaps extends Caps {

    private final int[] mProgramBinaryFormats;

    private final int mMaxFragmentUniformVectors;
    private float mMaxTextureMaxAnisotropy = 1.f;
    private final boolean mSupportsProtected = false;
    private boolean mFBFetchRequiresEnablePerSample;

    private final FormatInfo[] mFormatTable = new FormatInfo[GLTypes.FORMAT_LAST_COLOR + 1];

    // see GLTypes, default is GLTypes.FORMAT_UNKNOWN
    private final int[] mColorTypeToFormatTable = new int[ImageInfo.COLOR_LAST + 1];

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
            mBlendEquationSupport = BLEND_EQUATION_ADVANCED_COHERENT;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_AUTOMATIC;
        } else if (caps.GL_KHR_blend_equation_advanced_coherent) {
            mBlendEquationSupport = BLEND_EQUATION_ADVANCED_COHERENT;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_GENERAL_ENABLE;
        } else if (caps.GL_NV_blend_equation_advanced) {
            mBlendEquationSupport = BLEND_EQUATION_ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.ADV_BLEND_EQ_INTERACTION_AUTOMATIC;
        } else if (caps.GL_KHR_blend_equation_advanced) {
            mBlendEquationSupport = BLEND_EQUATION_ADVANCED;
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

        final int fpRenderFlags = msaaRenderFlags;

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
            info.mInternalFormatForTexImageOrStorage = GL_RGBA8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            int ctIdx = 0;
            // Format: RGBA8, Surface: kRGBA_8888
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
                }
            }

            // Format: RGBA8, Surface: kBGRA_8888
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGRA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
            info.mInternalFormatForTexImageOrStorage = GL_R8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            int ctIdx = 0;
            // Format: R8, Surface: kR_8
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_R_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.pack("000r");
                ctInfo.mWriteSwizzle = Swizzle.pack("a000");
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
            info.mInternalFormatForTexImageOrStorage = 0;
        }

        // Format: LUMINANCE8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_LUMINANCE8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_8;
            info.mInternalFormatForTexImageOrStorage = 0;
        }

        // Format: LUMINANCE8_ALPHA8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_LUMINANCE8_ALPHA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_GRAY_ALPHA_88;
            info.mInternalFormatForTexImageOrStorage = 0;
        }

        // Format: BGRA8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_BGRA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = 0;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_BGRA_8888;
            info.mInternalFormatForTexImageOrStorage = 0;
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
            info.mInternalFormatForTexImageOrStorage = GL_RGB565;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGB565, Surface: kBGR_565
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGR_565;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
            info.mFlags |= fpRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexImageOrStorage = GL_RGBA16F;

            int flags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;

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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
            info.mFlags |= fpRenderFlags;
            info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
            info.mInternalFormatForTexImageOrStorage = GL_R16F;

            // Format: R16F, Surface: kAlpha_F16
            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.pack("000r");
                ctInfo.mWriteSwizzle = Swizzle.pack("a000");
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
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
            info.mInternalFormatForTexImageOrStorage = 0;
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
            info.mInternalFormatForTexImageOrStorage = GL_RGB8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            int ctIdx = 0;
            // Format: RGB8, Surface: kRGB_888x
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGB_888x;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;

                int idx = ImageInfo.COLOR_RGB_888x;
                if (mColorTypeToFormatTable[idx] == GLTypes.FORMAT_UNKNOWN) {
                    mColorTypeToFormatTable[ImageInfo.COLOR_RGB_888x] = GLTypes.FORMAT_RGB8;
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
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RG8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG8;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RG_88;
            bool rg8Support = false;
            if (GR_IS_GR_GL(standard)) {
                rg8Support = version >= GL_VER(3, 0) || ctxInfo.hasExtension("GL_ARB_texture_rg");
            } else if (GR_IS_GL_ES(standard)) {
                rg8Support = version >= GL_VER(3, 0) || ctxInfo.hasExtension("GL_EXT_texture_rg");
            } else if (GR_IS_GR_WEBGL(standard)) {
                rg8Support = version >= GL_VER(2, 0);
            }
            if (rg8Support) {
                info.mFlags |= FormatInfo.TEXTURE_FLAG
                        | FormatInfo.TRANSFERS_FLAG
                        | msaaRenderFlags;
                if (texStorageSupported) {
                    info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                    info.mInternalFormatForTexImageOrStorage = GL_RG8;
                }
            }
            if (!(info.mFlags & FormatInfo.USE_TEX_STORAGE_FLAG)) {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_RG8 : GL_RG;
            }
            if (rg8Support) {
                info.mColorTypeInfoCount = 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                // Format: RG8, Surface: kRG_88
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_RG_88;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_RG_88] = GLTypes.FORMAT_RG8);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: RG8, Surface: kRG_88, Data: kRG_88
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_RG_88;
                        ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                        ioFormat.mExternalTexImageFormat = GL_RG;
                        ioFormat.mExternalReadFormat = 0;
                        if (GR_IS_GR_GL(standard) && !formatWorkarounds.mDisallowDirectRG8ReadPixels) {
                            ioFormat.mExternalReadFormat = GL_RG;
                        }
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
        }

        // Format: RGB10_A2
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGB10_A2] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB10_A2;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_1010102;
            if (GR_IS_GR_GL(standard) ||
                    (GR_IS_GL_ES(standard) && version >= GL_VER(3, 0))) {
                info.mFlags = FormatInfo.TEXTURE_FLAG
                        | FormatInfo.TRANSFERS_FLAG
                        | msaaRenderFlags;
            } else if (GR_IS_GL_ES(standard) &&
                    ctxInfo.hasExtension("GL_EXT_texture_type_2_10_10_10_REV")) {
                info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
            } // No WebGL support

            if (texStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_RGB10_A2;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_RGB10_A2 : GL_RGBA;
            }

            if (SkToBool(info.mFlags & FormatInfo.TEXTURE_FLAG)) {
                bool supportsBGRAColorType = GR_IS_GR_GL(standard) &&
                        (version >= GL_VER(1, 2) || ctxInfo.hasExtension("GL_EXT_bgra"));

                info.mColorTypeInfoCount = supportsBGRAColorType ? 2 : 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                // Format: RGB10_A2, Surface: kRGBA_1010102
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_RGBA_1010102;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_1010102] = GLTypes.FORMAT_RGB10_A2);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_1010102
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_RGBA_1010102;
                        ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                        ioFormat.mExternalTexImageFormat = GL_RGBA;
                        ioFormat.mExternalReadFormat = GL_RGBA;
                        // Not guaranteed by ES/WebGL.
                        ioFormat.mRequiresImplementationReadQuery = false;
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
                if (supportsBGRAColorType) {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_BGRA_1010102;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_BGRA_1010102] = GLTypes.FORMAT_RGB10_A2);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                            ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kBGRA_1010102
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_BGRA_1010102;
                        ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                        ioFormat.mExternalTexImageFormat = GL_BGRA;
                        ioFormat.mExternalReadFormat =
                                formatWorkarounds.mDisallowBGRA8ReadPixels ? 0 : GL_BGRA;
                        // Not guaranteed by ES/WebGL.
                        ioFormat.mRequiresImplementationReadQuery = false;
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
            if (GR_IS_GR_GL(standard)) {
                if (version >= GL_VER(4, 2)) {
                    info.mFlags |= msaaRenderFlags;
                }
            } else if (GR_IS_GL_ES(standard)) {
                info.mFlags |= msaaRenderFlags;
            } else if (GR_IS_GR_WEBGL(standard)) {
                info.mFlags |= msaaRenderFlags;
            }
            if (texStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_RGBA4;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_RGBA4 : GL_RGBA;
            }

            info.mColorTypeInfoCount = 1;
            info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
            int ctIdx = 0;
            // Format: RGBA4, Surface: kABGR_4444
            {
                var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ABGR_4444;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                mColorTypeToFormatTable[ImageInfo.COLOR_ABGR_4444] = GLTypes.FORMAT_RGBA4);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormatCount = 2;
                ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                    ctInfo.mExternalIOFormatCount);
                int ioIdx = 0;
                // Format: RGBA4, Surface: kABGR_4444, Data: kABGR_4444
                {
                    var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ABGR_4444;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT_4_4_4_4;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                    // Not guaranteed by ES/WebGL.
                    ioFormat.mRequiresImplementationReadQuery = false;
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
            bool srgb8Alpha8TexStorageSupported = texStorageSupported;
            bool srgb8Alpha8TextureSupport = false;
            bool srgb8Alpha8RenderTargetSupport = false;
            if (GR_IS_GR_GL(standard)) {
                if (version >= GL_VER(3, 0)) {
                    srgb8Alpha8TextureSupport = true;
                    srgb8Alpha8RenderTargetSupport = true;
                } else if (ctxInfo.hasExtension("GL_EXT_texture_sRGB")) {
                    srgb8Alpha8TextureSupport = true;
                    if (ctxInfo.hasExtension("GL_ARB_framebuffer_sRGB") ||
                            ctxInfo.hasExtension("GL_EXT_framebuffer_sRGB")) {
                        srgb8Alpha8RenderTargetSupport = true;
                    }
                }
            } else if (GR_IS_GL_ES(standard)) {
                if (version >= GL_VER(3, 0) || ctxInfo.hasExtension("GL_EXT_sRGB")) {
                    srgb8Alpha8TextureSupport = true;
                    srgb8Alpha8RenderTargetSupport = true;
                }
                if (version < GL_VER(3, 0)) {
                    // ES 2.0 requires that the external format matches the internal format.
                    info.mDefaultExternalFormat = GL_SRGB_ALPHA;
                    // There is no defined interaction between GL_EXT_sRGB and GL_EXT_texture_storage.
                    srgb8Alpha8TexStorageSupported = false;
                }
            } else if (GR_IS_GR_WEBGL(standard)) {
                // sRGB extension should be on most WebGL 1.0 contexts, although sometimes under 2
                // names.
                if (version >= GL_VER(2, 0) || ctxInfo.hasExtension("GL_EXT_sRGB") ||
                        ctxInfo.hasExtension("EXT_sRGB")) {
                    srgb8Alpha8TextureSupport = true;
                    srgb8Alpha8RenderTargetSupport = true;
                }
                if (version < GL_VER(2, 0)) {
                    // WebGL 1.0 requires that the external format matches the internal format.
                    info.mDefaultExternalFormat = GL_SRGB_ALPHA;
                    // There is no extension to WebGL 1 that adds glTexStorage.
                    SkASSERT(!srgb8Alpha8TexStorageSupported);
                }
            }

            if (srgb8Alpha8TextureSupport) {
                info.mFlags = FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
                if (srgb8Alpha8RenderTargetSupport) {
                    info.mFlags |= formatWorkarounds.mDisableSRGBRenderWithMSAAForMacAMD
                            ? nonMSAARenderFlags
                            : msaaRenderFlags;
                }
            }
            if (srgb8Alpha8TexStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_SRGB8_ALPHA8;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_SRGB8_ALPHA8 : GL_SRGB_ALPHA;
            }

            if (srgb8Alpha8TextureSupport) {
                info.mColorTypeInfoCount = 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_8888_SRGB] = GLTypes.FORMAT_SRGB8_ALPHA8);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 1;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;

                    // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB, Data: kRGBA_8888_SRGB
                    {
                        // GL does not do srgb<->rgb conversions when transferring between cpu and gpu.
                        // Thus, the external format is GL_RGBA. See below for note about ES2.0 and
                        // glTex[Sub]Image.
                        GrGLenum texImageExternalFormat = GL_RGBA;

                        // OpenGL ES 2.0 + GL_EXT_sRGB allows GL_SRGB_ALPHA to be specified as the
                        // <format> param to Tex(Sub)Image. ES 2.0 requires the <internalFormat> and
                        // <format> params to match. Thus, on ES 2.0 we will use GL_SRGB_ALPHA as the
                        // <format> param. On OpenGL and ES 3.0+ GL_SRGB_ALPHA does not work for the
                        // <format> param to glTexImage.
                        if (GR_IS_GL_ES(standard) && version == GL_VER(2, 0)) {
                            texImageExternalFormat = GL_SRGB_ALPHA;
                        }
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                        ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                        ioFormat.mExternalTexImageFormat = texImageExternalFormat;
                        ioFormat.mExternalReadFormat = GL_RGBA;
                    }
                }
            }
        }

        // Format: COMPRESSED_RGB8_BC1
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGB8_BC1] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexImageOrStorage = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            if (GR_IS_GR_GL(standard) || GR_IS_GL_ES(standard)) {
                if (ctxInfo.hasExtension("GL_EXT_texture_compression_s3tc")) {
                    info.mFlags = FormatInfo.TEXTURE_FLAG;
                }
            } // No WebGL support

            // There are no support GrColorTypes for this format
        }

        // Format: COMPRESSED_RGBA8_BC1
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGBA8_BC1] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexImageOrStorage = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            if (GR_IS_GR_GL(standard) || GR_IS_GL_ES(standard)) {
                if (ctxInfo.hasExtension("GL_EXT_texture_compression_s3tc")) {
                    info.mFlags = FormatInfo.TEXTURE_FLAG;
                }
            } // No WebGL support

            // There are no support GrColorTypes for this format
        }

        // Format: COMPRESSED_RGB8_ETC2
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGB8_ETC2] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexImageOrStorage = GL_COMPRESSED_RGB8_ETC2;
            if (!formatWorkarounds.mDisallowETC2Compression) {
                if (GR_IS_GR_GL(standard)) {
                    if (version >= GL_VER(4, 3) ||
                            ctxInfo.hasExtension("GL_ARB_ES3_compatibility")) {
                        info.mFlags = FormatInfo.TEXTURE_FLAG;
                    }
                } else if (GR_IS_GL_ES(standard)) {
                    if (version >= GL_VER(3, 0) ||
                            ctxInfo.hasExtension("GL_OES_compressed_ETC2_RGB8_texture")) {
                        info.mFlags = FormatInfo.TEXTURE_FLAG;
                    }
                } // No WebGL support
            }

            // There are no support GrColorTypes for this format
        }

        // Format: R16
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_R16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R16;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_R_16;
            bool r16Supported = false;
            if (!formatWorkarounds.mDisallowTextureUnorm16) {
                if (GR_IS_GR_GL(standard)) {
                    r16Supported = version >= GL_VER(3, 0) ||
                            ctxInfo.hasExtension("GL_ARB_texture_rg");
                } else if (GR_IS_GL_ES(standard)) {
                    r16Supported = ctxInfo.hasExtension("GL_EXT_texture_norm16");
                }  // No WebGL support
            }

            if (r16Supported) {
                info.mFlags = FormatInfo.TEXTURE_FLAG | msaaRenderFlags;
                if (!formatWorkarounds.mDisallowUnorm16Transfers) {
                    info.mFlags |= FormatInfo.TRANSFERS_FLAG;
                }
            }

            if (texStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_R16;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_R16 : GL_RED;
            }

            if (r16Supported) {
                info.mColorTypeInfoCount = 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                // Format: R16, Surface: kAlpha_16
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_ALPHA_16;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    ctInfo.mReadSwizzle = Swizzle.pack("000r");
                    ctInfo.mWriteSwizzle = Swizzle.pack("a000");
                    mColorTypeToFormatTable[ImageInfo.COLOR_ALPHA_16] = GLTypes.FORMAT_R16);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: R16, Surface: kAlpha_16, Data: kAlpha_16
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_ALPHA_16;
                        ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                        ioFormat.mExternalTexImageFormat = GL_RED;
                        ioFormat.mExternalReadFormat = GL_RED;
                        // Not guaranteed by ES/WebGL.
                        ioFormat.mRequiresImplementationReadQuery = false;
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
        }

        // Format: RG16
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RG16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexImageOrStorage =
                    texImageSupportsSizedInternalFormat ? GL_RG16 : GL_RG;
            info.mInternalFormatForRenderbuffer = GL_RG16;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_RG_1616;
            bool rg16Supported = false;
            if (!formatWorkarounds.mDisallowTextureUnorm16) {
                if (GR_IS_GR_GL(standard)) {
                    rg16Supported = version >= GL_VER(3, 0) ||
                            ctxInfo.hasExtension("GL_ARB_texture_rg");
                } else if (GR_IS_GL_ES(standard)) {
                    rg16Supported = ctxInfo.hasExtension("GL_EXT_texture_norm16");
                }  // No WebGL support
            }

            if (rg16Supported) {
                info.mFlags = FormatInfo.TEXTURE_FLAG | msaaRenderFlags;
                if (!formatWorkarounds.mDisallowUnorm16Transfers) {
                    info.mFlags |= FormatInfo.TRANSFERS_FLAG;
                }
            }

            if (texStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_RG16;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_RG16 : GL_RG;
            }

            if (rg16Supported) {
                info.mColorTypeInfoCount = 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                // Format: GL_RG16, Surface: kRG_1616
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_RG_1616;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_RG_1616] = GLTypes.FORMAT_RG16);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: GL_RG16, Surface: kRG_1616, Data: kRG_1616
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_RG_1616;
                        ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                        ioFormat.mExternalTexImageFormat = GL_RG;
                        ioFormat.mExternalReadFormat = GL_RG;
                        // Not guaranteed by ES/WebGL.
                        ioFormat.mRequiresImplementationReadQuery = false;
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
        }

        // Format: RGBA16
        {
            bool rgba16Support = false;
            if (!formatWorkarounds.mDisallowTextureUnorm16) {
                if (GR_IS_GR_GL(standard)) {
                    rgba16Support = version >= GL_VER(3, 0);
                } else if (GR_IS_GL_ES(standard)) {
                    rgba16Support = ctxInfo.hasExtension("GL_EXT_texture_norm16");
                }  // No WebGL support
            }

            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGBA16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;

            info.mInternalFormatForRenderbuffer = GL_RGBA16;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_16161616;
            if (rgba16Support) {
                info.mFlags = FormatInfo.TEXTURE_FLAG | msaaRenderFlags;
                if (!formatWorkarounds.mDisallowUnorm16Transfers) {
                    info.mFlags |= FormatInfo.TRANSFERS_FLAG;
                }
            }

            if (texStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_RGBA16;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_RGBA16 : GL_RGBA;
            }

            if (rgba16Support) {
                // Format: GL_RGBA16, Surface: kRGBA_16161616
                info.mColorTypeInfoCount = 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_RGBA_16161616;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_RGBA_16161616] = GLTypes.FORMAT_RGBA16);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_16161616
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_RGBA_16161616;
                        ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                        ioFormat.mExternalTexImageFormat = GL_RGBA;
                        ioFormat.mExternalReadFormat = GL_RGBA;
                        // Not guaranteed by ES/WebGL.
                        ioFormat.mRequiresImplementationReadQuery = false;
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
        }

        // Format:RG16F
        {
            bool rg16FTextureSupport = false;
            bool rg16FRenderTargetSupport = false;
            if (GR_IS_GR_GL(standard)) {
                if (version >= GL_VER(3, 0) || ctxInfo.hasExtension("GL_ARB_texture_float")) {
                    rg16FTextureSupport = true;
                    rg16FRenderTargetSupport = true;
                }
            } else if (GR_IS_GL_ES(standard)) {
                // It seems possible that a combination of GL_EXT_texture_rg and
                // GL_EXT_color_buffer_half_float might add this format to ES 2.0 but it is not entirely
                // clear. The latter mentions interaction but that may only be for renderbuffers as
                // neither adds the texture format explicitly.
                // GL_OES_texture_format_half_float makes no reference to RG formats.
                if (version >= GL_VER(3, 0)) {
                    rg16FTextureSupport = true;
                    rg16FRenderTargetSupport = version >= GL_VER(3, 2) ||
                            ctxInfo.hasExtension("GL_EXT_color_buffer_float") ||
                            ctxInfo.hasExtension("GL_EXT_color_buffer_half_float");
                }
            } else if (GR_IS_GR_WEBGL(standard)) {
                if (version >= GL_VER(2, 0)) {
                    rg16FTextureSupport = true;
                    rg16FRenderTargetSupport = ctxInfo.hasExtension("GL_EXT_color_buffer_half_float") ||
                            ctxInfo.hasExtension("EXT_color_buffer_half_float") ||
                            ctxInfo.hasExtension("GL_EXT_color_buffer_float") ||
                            ctxInfo.hasExtension("EXT_color_buffer_float");
                }
            }

            FormatInfo info = mFormatTable[GLTypes.FORMAT_RG16F] = new FormatInfo();
            info.mFormatType = FormatType::kFloat;
            info.mInternalFormatForRenderbuffer = GL_RG16F;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = halfFloatType;
            info.mDefaultColorType = ImageInfo.COLOR_RG_F16;
            if (rg16FTextureSupport) {
                info.mFlags |= FormatInfo.TEXTURE_FLAG | FormatInfo.TRANSFERS_FLAG;
                if (rg16FRenderTargetSupport) {
                    info.mFlags |= fpRenderFlags;
                }
            }

            if (texStorageSupported) {
                info.mFlags |= FormatInfo.USE_TEX_STORAGE_FLAG;
                info.mInternalFormatForTexImageOrStorage = GL_RG16F;
            } else {
                info.mInternalFormatForTexImageOrStorage =
                        texImageSupportsSizedInternalFormat ? GL_RG16F : GL_RG;
            }

            if (rg16FTextureSupport) {
                info.mColorTypeInfoCount = 1;
                info.mColorTypeInfos = std::make_unique < ColorTypeInfo[]>(info.mColorTypeInfoCount);
                int ctIdx = 0;
                // Format: GL_RG16F, Surface: kRG_F16
                {
                    var ctInfo = info.mColorTypeInfos[ctIdx++] = new ColorTypeInfo();
                    ctInfo.mColorType = ImageInfo.COLOR_RG_F16;
                    ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                    mColorTypeToFormatTable[ImageInfo.COLOR_RG_F16] = GLTypes.FORMAT_RG16F);

                    // External IO ColorTypes:
                    ctInfo.mExternalIOFormatCount = 2;
                    ctInfo.mExternalIOFormats = std::make_unique < ColorTypeInfo::ExternalIOFormats[]>(
                        ctInfo.mExternalIOFormatCount);
                    int ioIdx = 0;
                    // Format: GL_RG16F, Surface: kRG_F16, Data: kRG_F16
                    {
                        var ioFormat = ctInfo.mExternalIOFormats[ioIdx++] = new ExternalIOFormat();
                        ioFormat.mColorType = ImageInfo.COLOR_RG_F16;
                        ioFormat.mExternalType = halfFloatType;
                        ioFormat.mExternalTexImageFormat = GL_RG;
                        ioFormat.mExternalReadFormat = GL_RG;
                        // Not guaranteed by ES/WebGL.
                        ioFormat.mRequiresImplementationReadQuery = false;
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
        }

        finishInitialization(options);

        // For now these two are equivalent, but we could have dst read in shader via some other method.
        mShaderCaps.mDstReadInShaderSupport = mShaderCaps.mFBFetchSupport;
    }

    @Override
    public boolean isTextureFormat(BackendFormat format) {
        return isTextureFormat(format.getGLFormat());
    }

    /**
     * @see GLTypes
     */
    public boolean isTextureFormat(int format) {

    }

    private static class ExternalIOFormat {

        int mColorType = ImageInfo.COLOR_UNKNOWN;

        /**
         * The external format and type are to be used when uploading/downloading data using
         * data of fColorType and uploading to a texture of a given GrGLFormat and its
         * intended GrColorType. The fExternalTexImageFormat is the format to use for TexImage
         * calls. The fExternalReadFormat is used when calling ReadPixels. If either is zero
         * that signals that either TexImage or ReadPixels is not supported for the combination
         * of format and color types.
         */
        int mExternalType = 0;
        int mExternalTexImageFormat = 0;
        int mExternalReadFormat = 0;
        /**
         * Must check whether GL_IMPLEMENTATION_COLOR_READ_FORMAT and _TYPE match
         * fExternalReadFormat and fExternalType before using with glReadPixels.
         */
        boolean mRequiresImplementationReadQuery = false;
    }

    private static class ColorTypeInfo {

        int mColorType = ImageInfo.COLOR_UNKNOWN;

        public static final int
                UPLOAD_DATA_FLAG = 0x1,
                RENDERABLE_FLAG = 0x2;
        int mFlags = 0;

        short mReadSwizzle = Swizzle.RGBA;
        short mWriteSwizzle = Swizzle.RGBA;

        ExternalIOFormat[] mExternalIOFormats;
    }

    private static class FormatInfo {

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

        // Not defined for uncompressed formats. Passed to glCompressedTexImage...
        int mCompressedInternalFormat = 0;

        // Value to use as the "internalformat" argument to glTexImage or glTexStorage. It is
        // initialized in coordination with the presence/absence of the kUseTexStorage flag. In
        // other words, it is only guaranteed to be compatible with glTexImage if the flag is not
        // set and or with glTexStorage if the flag is set.
        int mInternalFormatForTexImageOrStorage = 0;

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

        private static final ColorTypeInfo[] EMPTY_CT_INFOS = {};
        ColorTypeInfo[] mColorTypeInfos = EMPTY_CT_INFOS;
    }
}
