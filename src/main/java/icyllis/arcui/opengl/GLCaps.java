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

package icyllis.arcui.opengl;

import icyllis.arcui.core.Image;
import icyllis.arcui.core.ImageInfo;
import icyllis.arcui.engine.*;
import org.jetbrains.annotations.VisibleForTesting;
import org.lwjgl.opengl.EXTWindowRectangles;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.Arrays;

import static icyllis.arcui.opengl.GLCore.*;
import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL46C.GL_MAX_TEXTURE_MAX_ANISOTROPY;

public final class GLCaps extends Caps {

    final int[] mProgramBinaryFormats;

    final int mMaxFragmentUniformVectors;
    float mMaxTextureMaxAnisotropy = 1.f;
    final boolean mSupportsProtected = false;
    boolean mFBFetchRequiresEnablePerSample;
    private boolean mSkipErrorChecks = false;

    // see GLTypes
    private final FormatInfo[] mFormatTable =
            new FormatInfo[GLTypes.LAST_COLOR_FORMAT + 1];
    private final int[] mColorTypeToFormatTable =
            new int[ImageInfo.LAST_COLOR + 1];

    // may contain null values that representing invalid
    private final GLBackendFormat[] mColorTypeToBackendFormat =
            new GLBackendFormat[ImageInfo.LAST_COLOR + 1];
    private final GLBackendFormat[] mCompressionTypeToBackendFormat =
            new GLBackendFormat[Image.LAST_COMPRESSION + 1];

    /**
     * All required ARB extensions from OpenGL 3.3 to OpenGL 4.5
     */
    public static final String[] REQUIRED_EXTENSIONS = {
            "ARB_blend_func_extended",
            "ARB_sampler_objects",
            "ARB_explicit_attrib_location",
            "ARB_instanced_arrays",
            "ARB_texture_swizzle",
            "ARB_draw_indirect",
            "ARB_ES2_compatibility",
            "ARB_get_program_binary",
            "ARB_base_instance",
            "ARB_texture_storage",
            "ARB_internalformat_query",
            "ARB_shading_language_420pack",
            "ARB_invalidate_subdata",
            "ARB_multi_draw_indirect",
            "ARB_explicit_uniform_location",
            "ARB_vertex_attrib_binding",
            "ARB_ES3_compatibility",
            "ARB_clear_texture",
            "ARB_buffer_storage",
            "ARB_enhanced_layouts",
            "ARB_texture_barrier",
            "ARB_direct_state_access"
    };

    @VisibleForTesting
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
                if (!caps.GL_ARB_shading_language_420pack) {
                    throw new AssertionError("GL_ARB_shading_language_420pack is unavailable");
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
                if (!caps.GL_ARB_enhanced_layouts) {
                    throw new AssertionError("ARB_enhanced_layouts is unavailable");
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

        initGLSL(caps);
        ShaderCaps shaderCaps = mShaderCaps;

        // OpenGL 3.3
        shaderCaps.mDualSourceBlendingSupport = true;
        // Desktop
        shaderCaps.mShaderDerivativeSupport = true;
        // OpenGL 3.0
        shaderCaps.mIntegerSupport = true;
        // GLSL 130
        shaderCaps.mNonSquareMatrixSupport = true;
        // GLSL 130
        shaderCaps.mInverseHyperbolicSupport = true;

        if (caps.GL_NV_conservative_raster) {
            mConservativeRasterSupport = true;
        }

        // GLSL 130
        shaderCaps.mRewriteSwitchStatements = false;
        // Protect ourselves against tracking huge amounts of texture state.
        shaderCaps.mMaxFragmentSamplers = Math.min(32, glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));

        if (caps.GL_NV_blend_equation_advanced_coherent) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED_COHERENT;
            shaderCaps.mAdvBlendEqInteraction = ShaderCaps.Automatic_AdvBlendEqInteraction;
        } else if (caps.GL_KHR_blend_equation_advanced_coherent) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED_COHERENT;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.GeneralEnable_AdvBlendEqInteraction;
        } else if (caps.GL_NV_blend_equation_advanced) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.Automatic_AdvBlendEqInteraction;
        } else if (caps.GL_KHR_blend_equation_advanced) {
            mBlendEquationSupport = BlendEquationSupport.ADVANCED;
            mShaderCaps.mAdvBlendEqInteraction = ShaderCaps.GeneralEnable_AdvBlendEqInteraction;
        }

        // On many GPUs, map memory is very expensive, so we effectively disable it here by setting the
        // threshold to the maximum unless the client gives us a hint that map memory is cheap.
        if (mBufferMapThreshold < 0) {
            mBufferMapThreshold = Integer.MAX_VALUE;
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

        if (caps.GL_EXT_window_rectangles) {
            mMaxWindowRectangles = glGetInteger(EXTWindowRectangles.GL_MAX_WINDOW_RECTANGLES_EXT);
        }

        mNativeDrawIndirectSupport = true;

        mDynamicStateArrayGeometryProcessorTextureSupport = true;

        int count = glGetInteger(GL_NUM_PROGRAM_BINARY_FORMATS);
        /*if (count == 0) {
            throw new AssertionError("No program binary formats are available");
        }*/
        mProgramBinaryFormats = new int[count];
        if (count > 0) {
            glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, mProgramBinaryFormats);
        }

        initFormatTable(caps);

        finishInitialization(options);

        // For now these two are equivalent, but we could have dst read in shader via some other method.
        mShaderCaps.mDstReadInShaderSupport = mShaderCaps.mFBFetchSupport;
    }

    private void initGLSL(GLCapabilities caps) {
        ShaderCaps shaderCaps = mShaderCaps;
        if (caps.GL_EXT_shader_framebuffer_fetch) {
            shaderCaps.mFBFetchNeedsCustomOutput = true;
            shaderCaps.mFBFetchSupport = true;
            shaderCaps.mFBFetchColorName = "gl_LastFragData[0]";
            shaderCaps.mFBFetchExtensionString = "GL_EXT_shader_framebuffer_fetch";
            mFBFetchRequiresEnablePerSample = false;
        }

        // GLSL 130
        shaderCaps.mFlatInterpolationSupport = true;
        // Desktop
        shaderCaps.mPreferFlatInterpolation = true;
        // GLSL 130
        shaderCaps.mNoPerspectiveInterpolationSupport = true;
        // GLSL 400
        shaderCaps.mSampleMaskSupport = true;

        shaderCaps.mVersionDeclString = "#version 450 core\n";
        // Desktop
        shaderCaps.mVertexIDSupport = true;
        // GLSL 330
        shaderCaps.mInfinitySupport = true;
        // Desktop
        shaderCaps.mNonConstantArrayIndexSupport = true;
        // GLSL 400
        shaderCaps.mBitManipulationSupport = true;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer range = stack.mallocInt(2);
            int bits = glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, range);
            shaderCaps.mFloatIs32Bits &= range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;
            bits = glGetShaderPrecisionFormat(GL_VERTEX_SHADER, GL_HIGH_FLOAT, range);
            shaderCaps.mFloatIs32Bits &= range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;

            bits = glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_MEDIUM_FLOAT, range);
            shaderCaps.mHalfIs32Bits &= range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;
            bits = glGetShaderPrecisionFormat(GL_VERTEX_SHADER, GL_MEDIUM_FLOAT, range);
            shaderCaps.mHalfIs32Bits &= range.get(0) >= 127 && range.get(1) >= 127 && bits >= 23;
        }

        shaderCaps.mHasLowFragmentPrecision = false;
        // GLSL 400
        shaderCaps.mBuiltinFMASupport = true;
        // GLSL 150
        shaderCaps.mBuiltinDeterminantSupport = true;
    }

    private void initFormatTable(GLCapabilities caps) {
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            // Format: RGBA8, Surface: kRGBA_8888
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGBA_8888, GLTypes.FORMAT_RGBA8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA8, Surface: kRGBA_8888, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA8, Surface: kRGBA_8888, Data: kBGRA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGRA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }
            }

            // Format: RGBA8, Surface: kBGRA_8888
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGRA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_BGRA_8888, GLTypes.FORMAT_RGBA8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA8, Surface: kBGRA_8888, Data: kBGRA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGRA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }

                // Format: RGBA8, Surface: kBGRA_8888, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: RGBA8, Surface: kRGB_888x
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[2] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGB_888X;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.RGB1;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                // Format: RGBA8, Surface: kRGB_888x, Data: kRGBA_888x
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGB_888X;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            // Format: R8, Surface: kR_8
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_R_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_R_8, GLTypes.FORMAT_R8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R8, Surface: kR_8, Data: kR_8
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_R_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kR_8, Data: kR_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_R_8XXX;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: R8, Surface: kAlpha_8
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                setColorTypeFormat(ImageInfo.COLOR_ALPHA_8, GLTypes.FORMAT_R8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R8, Surface: kAlpha_8, Data: kAlpha_8
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kAlpha_8, Data: kAlpha_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_8XXX;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: R8, Surface: kGray_8
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[2] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_GRAY_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("rrr1");
                setColorTypeFormat(ImageInfo.COLOR_GRAY_8, GLTypes.FORMAT_R8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R8, Surface: kGray_8, Data: kGray_8
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_GRAY_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kGray_8, Data: kGray_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_GRAY_8XXX;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: ALPHA8, DEPRECATED
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_ALPHA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = 0;
            info.mDefaultExternalFormat = GL_ALPHA;
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
            info.mDefaultExternalFormat = GL_BGRA;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB565;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: RGB565, Surface: kBGR_565
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGR_565;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_BGR_565, GLTypes.FORMAT_RGB565);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB565, Surface: kBGR_565, Data: kBGR_565
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGR_565;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT_5_6_5;
                    ioFormat.mExternalWriteFormat = GL_RGB;
                    ioFormat.mExternalReadFormat = GL_RGB;
                }

                // Format: RGB565, Surface: kBGR_565, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA16F;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            // Format: RGBA16F, Surface: kRGBA_F16
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGBA_F16, GLTypes.FORMAT_RGBA16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA16F, Surface: kRGBA_F16, Data: kRGBA_F16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA16F, Surface: kRGBA_F16, Data: kRGBA_F32
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: RGBA16F, Surface: kRGBA_F16_Clamped
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_F16_CLAMPED;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGBA_F16_CLAMPED, GLTypes.FORMAT_RGBA16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA16F, Surface: kRGBA_F16_Clamped, Data: kRGBA_F16_Clamped
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F16_CLAMPED;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA16F, Surface: kRGBA_F16_Clamped, Data: kRGBA_F32
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R16F;

            // Format: R16F, Surface: kAlpha_F16
            info.mColorTypeInfos = new ColorTypeInfo[1];
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                setColorTypeFormat(ImageInfo.COLOR_ALPHA_F16, GLTypes.FORMAT_R16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R16F, Surface: kAlpha_F16, Data: kAlpha_F16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalWriteFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R16F, Surface: kAlpha_F16, Data: kAlpha_F32xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_F32XXX;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGB8
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_RGB8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB8;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGB_888;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            // Even in OpenGL 4.6 GL_RGB8 is required to be color renderable but not required to be
            // a supported render buffer format. Since we usually use render buffers for MSAA on
            // non-ES GL we don't support MSAA for GL_RGB8.
            if (glGetInternalformati(GL_RENDERBUFFER, GL_RGB8, GL_INTERNALFORMAT_SUPPORTED) == GL_TRUE) {
                info.mFlags |= msaaRenderFlags;
            } else {
                info.mFlags |= nonMSAARenderFlags;
            }
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: RGB8, Surface: kRGB_888x
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGB_888X;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGB_888X, GLTypes.FORMAT_RGB8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB8, Surface: kRGB_888x, Data: kRGB_888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGB_888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RGB;
                    ioFormat.mExternalReadFormat = 0;
                }

                // Format: RGB8, Surface: kRGB_888x, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: RG8, Surface: kRG_88
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_88;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RG_88, GLTypes.FORMAT_RG8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RG8, Surface: kRG_88, Data: kRG_88
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RG_88;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: RG8, Surface: kRG_88, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGB10_A2;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            // Format: RGB10_A2, Surface: kRGBA_1010102
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGBA_1010102, GLTypes.FORMAT_RGB10_A2);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_1010102
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_1010102;
                    ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
            //------------------------------------------------------------------
            // Format: RGB10_A2, Surface: kBGRA_1010102
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_BGRA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_BGRA_1010102, GLTypes.FORMAT_RGB10_A2);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kBGRA_1010102
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_BGRA_1010102;
                    ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                    ioFormat.mExternalWriteFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }

                // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA4;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: RGBA4, Surface: kABGR_4444
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ABGR_4444;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_ABGR_4444, GLTypes.FORMAT_RGBA4);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA4, Surface: kABGR_4444, Data: kABGR_4444
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ABGR_4444;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT_4_4_4_4;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA4, Surface: kABGR_4444, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: SRGB8_ALPHA8
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_SRGB8_ALPHA8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_SRGB8_ALPHA8;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_SRGB8_ALPHA8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGBA_8888_SRGB, GLTypes.FORMAT_SRGB8_ALPHA8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB, Data: kRGBA_8888_SRGB
                {
                    // GL does not do srgb<->rgb conversions when transferring between cpu and gpu.
                    // Thus, the external format is GL_RGBA.
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888_SRGB;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: COMPRESSED_RGB8_ETC2
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGB8_ETC2] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB8_ETC2;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG;

            mCompressionTypeToBackendFormat[Image.COMPRESSION_ETC2_RGB8_UNORM] =
                    BackendFormat.makeGL(GL_COMPRESSED_RGB8_ETC2, EngineTypes.TextureType_2D);

            // There are no support ColorTypes for this format
        }

        // Format: COMPRESSED_RGB8_BC1
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGB8_BC1] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            if (caps.GL_EXT_texture_compression_s3tc) {
                info.mFlags = FormatInfo.TEXTURABLE_FLAG;

                mCompressionTypeToBackendFormat[Image.COMPRESSION_BC1_RGB8_UNORM] =
                        BackendFormat.makeGL(GL_COMPRESSED_RGB_S3TC_DXT1_EXT, EngineTypes.TextureType_2D);
            }

            // There are no support ColorTypes for this format
        }

        // Format: COMPRESSED_RGBA8_BC1
        {
            FormatInfo info = mFormatTable[GLTypes.FORMAT_COMPRESSED_RGBA8_BC1] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            if (caps.GL_EXT_texture_compression_s3tc) {
                info.mFlags = FormatInfo.TEXTURABLE_FLAG;

                mCompressionTypeToBackendFormat[Image.COMPRESSION_BC1_RGBA8_UNORM] =
                        BackendFormat.makeGL(GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, EngineTypes.TextureType_2D);
            }

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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_R16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: R16, Surface: kAlpha_16
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_ALPHA_16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                setColorTypeFormat(ImageInfo.COLOR_ALPHA_16, GLTypes.FORMAT_R16);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R16, Surface: kAlpha_16, Data: kAlpha_16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_16;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalWriteFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R16, Surface: kAlpha_16, Data: kAlpha_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_ALPHA_8XXX;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: GL_RG16, Surface: kRG_1616
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_1616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RG_1616, GLTypes.FORMAT_RG16);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: GL_RG16, Surface: kRG_1616, Data: kRG_1616
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RG_1616;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalWriteFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: GL_RG16, Surface: kRG_1616, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RGBA16;

            // Format: GL_RGBA16, Surface: kRGBA_16161616
            info.mColorTypeInfos = new ColorTypeInfo[1];
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RGBA_16161616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RGBA_16161616, GLTypes.FORMAT_RGBA16);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_16161616
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_16161616;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalWriteFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalWriteFormat = 0;
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
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            info.mInternalFormatForTexture = GL_RG16F;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: GL_RG16F, Surface: kRG_F16
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ImageInfo.COLOR_RG_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ImageInfo.COLOR_RG_F16, GLTypes.FORMAT_RG16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: GL_RG16F, Surface: kRG_F16, Data: kRG_F16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RG_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalWriteFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: GL_RG16F, Surface: kRG_F16, Data: kRGBA_F32
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ImageInfo.COLOR_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalWriteFormat = 0;
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

        // Init samples
        for (FormatInfo info : mFormatTable) {
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0) {
                // We assume that MSAA rendering is supported only if we support non-MSAA rendering.
                assert (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0;
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
            } else if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0) {
                info.mColorSampleCounts = new int[1];
                info.mColorSampleCounts[0] = 1;
            }
        }

        // Validate, skip UNKNOWN
        for (int format = 1; format <= GLTypes.LAST_COLOR_FORMAT; ++format) {
            FormatInfo info = getFormatInfo(format);
            // Make sure we didn't set fbo attachable with msaa and not fbo attachable
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) == 0) {
                throw new AssertionError();
            }
            // Make sure all renderbuffer formats can also be texture formats
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.TEXTURABLE_FLAG) == 0) {
                throw new AssertionError();
            }

            // Make sure we set all the formats' FormatType
            if (info.mFormatType == FormatInfo.FORMAT_TYPE_UNKNOWN) {
                throw new AssertionError();
            }

            // All texturable format should have their internal formats
            if ((info.mFlags & FormatInfo.TEXTURABLE_FLAG) != 0 &&
                    info.mInternalFormatForTexture == 0) {
                throw new AssertionError();
            }

            // All renderable format should have their internal formats
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0 &&
                    info.mInternalFormatForRenderbuffer == 0) {
                throw new AssertionError();
            }

            // Only compressed format doesn't support glTexStorage
            if ((info.mFlags & FormatInfo.TEXTURABLE_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.TEXTURE_STORAGE_FLAG) == 0 &&
                    !glFormatIsCompressed(format)) {
                throw new AssertionError();
            }

            // Only compressed format doesn't support renderbuffer
            if (info.mInternalFormatForTexture != info.mInternalFormatForRenderbuffer &&
                    !glFormatIsCompressed(format)) {
                throw new AssertionError();
            }

            // Make sure if we added a ColorTypeInfo we filled it out
            for (ColorTypeInfo ctInfo : info.mColorTypeInfos) {
                if (ctInfo.mColorType == ImageInfo.COLOR_UNKNOWN) {
                    throw new AssertionError();
                }
                // Seems silly to add a color type if we don't support any flags on it
                if (ctInfo.mFlags == 0) {
                    throw new AssertionError();
                }
                // Make sure if we added any ExternalIOFormats we filled it out
                for (ExternalIOFormat ioInfo : ctInfo.mExternalIOFormats) {
                    if (ioInfo.mColorType == ImageInfo.COLOR_UNKNOWN) {
                        throw new AssertionError();
                    }
                }
            }
        }
    }

    public FormatInfo getFormatInfo(int format) {
        return mFormatTable[format];
    }

    private void setColorTypeFormat(int colorType, int format) {
        assert format != GLTypes.FORMAT_UNKNOWN &&
                mColorTypeToFormatTable[colorType] == GLTypes.FORMAT_UNKNOWN;
        mColorTypeToFormatTable[colorType] = format;
        mColorTypeToBackendFormat[colorType] = BackendFormat.makeGL(glFormatToEnum(format), EngineTypes.TextureType_2D);
    }

    @Override
    public boolean isFormatTexturable(BackendFormat format) {
        return isFormatTexturable(format.getGLFormat());
    }

    public boolean isFormatTexturable(int format) {
        return (getFormatInfo(format).mFlags & FormatInfo.TEXTURABLE_FLAG) != 0;
    }

    @Override
    public int getMaxRenderTargetSampleCount(BackendFormat format) {
        return getMaxRenderTargetSampleCount(format.getGLFormat());
    }

    public int getMaxRenderTargetSampleCount(int format) {
        int[] table = getFormatInfo(format).mColorSampleCounts;
        if (table.length == 0) {
            return 0;
        }
        return table[table.length - 1];
    }

    @Override
    public boolean isFormatRenderable(int colorType, BackendFormat format, int sampleCount) {
        if (format.textureType() == EngineTypes.TextureType_External) {
            return false;
        }
        int f = format.getGLFormat();
        if ((getFormatInfo(f).getColorTypeFlags(colorType) & ColorTypeInfo.RENDERABLE_FLAG) == 0) {
            return false;
        }
        return isFormatRenderable(f, sampleCount);
    }

    @Override
    public boolean isFormatRenderable(BackendFormat format, int sampleCount) {
        if (format.textureType() == EngineTypes.TextureType_External) {
            return false;
        }
        return isFormatRenderable(format.getGLFormat(), sampleCount);
    }

    public boolean isFormatRenderable(int format, int sampleCount) {
        return sampleCount <= getMaxRenderTargetSampleCount(format);
    }

    @Override
    public int getRenderTargetSampleCount(int sampleCount, BackendFormat format) {
        return getRenderTargetSampleCount(sampleCount, format.getGLFormat());
    }

    public int getRenderTargetSampleCount(int sampleCount, int format) {
        FormatInfo formatInfo = getFormatInfo(format);
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
    public boolean onFormatCompatible(int colorType, BackendFormat format) {
        FormatInfo formatInfo = getFormatInfo(format.getGLFormat());
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

    @Nonnull
    @Override
    public ProgramDesc makeDesc(ProgramDesc desc,
                                RenderTarget renderTarget,
                                final ProgramInfo programInfo) {
        return ProgramDesc.build(desc, programInfo, this);
    }

    @Override
    public long getSupportedWriteColorType(int dstColorType, BackendFormat dstFormat, int srcColorType) {
        // We first try to find a supported write pixels ColorType that matches the data's
        // srcColorType. If that doesn't exist we will use any supported ColorType.
        int fallbackCT = ImageInfo.COLOR_UNKNOWN;
        final FormatInfo formatInfo = getFormatInfo(dstFormat.getGLFormat());
        boolean foundSurfaceCT = false;
        long transferOffsetAlignment = 0;
        if ((formatInfo.mFlags & FormatInfo.TRANSFERS_FLAG) != 0) {
            transferOffsetAlignment = 1;
        }
        for (int i = 0; !foundSurfaceCT && i < formatInfo.mColorTypeInfos.length; ++i) {
            if (formatInfo.mColorTypeInfos[i].mColorType == dstColorType) {
                final ColorTypeInfo ctInfo = formatInfo.mColorTypeInfos[i];
                foundSurfaceCT = true;
                for (final ExternalIOFormat ioInfo : ctInfo.mExternalIOFormats) {
                    if (ioInfo.mExternalWriteFormat != 0) {
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
        return fallbackCT | (transferOffsetAlignment << 32);
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
                    ImageInfo.COLOR_RGB_888X :
                    ImageInfo.COLOR_RGBA_8888); // alignment = 0
        }

        // We first try to find a supported read pixels ColorType that matches the requested
        // dstColorType. If that doesn't exist we will use any valid read pixels ColorType.
        int fallbackColorType = ImageInfo.COLOR_UNKNOWN;
        long fallbackTransferOffsetAlignment = 0;
        FormatInfo formatInfo = getFormatInfo(srcFormat.getGLFormat());
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

    @Override
    protected void onApplyOptionsOverrides(ContextOptions options) {
        super.onApplyOptionsOverrides(options);
        if (options.mSkipGLErrorChecks == Boolean.FALSE) {
            mSkipErrorChecks = false;
        } else if (options.mSkipGLErrorChecks == Boolean.TRUE) {
            mSkipErrorChecks = true;
        }
    }

    /**
     * Gets the internal format to use with glTexImage...() and glTexStorage...(). May be sized or
     * base depending upon the GL. Not applicable to compressed textures.
     */
    public int getTextureInternalFormat(int format) {
        return getFormatInfo(format).mInternalFormatForTexture;
    }

    /**
     * Gets the internal format to use with glRenderbufferStorageMultisample...(). May be sized or
     * base depending upon the GL. Not applicable to compressed textures.
     */
    public int getRenderbufferInternalFormat(int format) {
        return getFormatInfo(format).mInternalFormatForRenderbuffer;
    }

    /**
     * Gets the default external type to use with glTex[Sub]Image... when the data pointer is null.
     */
    public int getFormatDefaultExternalType(int format) {
        return getFormatInfo(format).mDefaultExternalType;
    }

    /**
     * Skip checks for GL errors, shader compilation success, program link success.
     */
    public boolean skipErrorChecks() {
        return mSkipErrorChecks;
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
    int mExternalWriteFormat = 0;
    int mExternalReadFormat = 0;
}

class ColorTypeInfo {

    int mColorType = ImageInfo.COLOR_UNKNOWN;

    public static final int
            UPLOAD_DATA_FLAG = 0x1,
            RENDERABLE_FLAG = 0x2;
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
            TEXTURABLE_FLAG = 0x01,
            COLOR_ATTACHMENT_FLAG = 0x02,
            COLOR_ATTACHMENT_WITH_MSAA_FLAG = 0x04,
            TEXTURE_STORAGE_FLAG = 0x08,
            TRANSFERS_FLAG = 0x10;
    int mFlags = 0;

    public static final int
            FORMAT_TYPE_UNKNOWN = 0,
            FORMAT_TYPE_NORMALIZED_FIXED_POINT = 1,
            FORMAT_TYPE_FLOAT = 2;
    int mFormatType = FORMAT_TYPE_UNKNOWN;

    // Value to use as the "internalformat" argument to glTexImage or glTexStorage. It is
    // initialized in coordination with the presence/absence of the UseTexStorage flag. In
    // other words, it is only guaranteed to be compatible with glTexImage if the flag is not
    // set and or with glTexStorage if the flag is set.
    int mInternalFormatForTexture = 0;

    // Value to use as the "internalformat" argument to glRenderbufferStorageMultisample...
    int mInternalFormatForRenderbuffer = 0;

    // Default values to use along with mInternalFormatForTexture for function
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
