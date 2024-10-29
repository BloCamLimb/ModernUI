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

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.engine.trash.PipelineKey_old;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL43C.*;

/**
 * Stores some capabilities of an OpenGL device.
 * <p>
 * OpenGL 3.3 or OpenGL ES 3.0 is the minimum requirement.
 */
public abstract class GLCaps extends Caps {

    /**
     * Contains missing extensions on the last creation of GPU.
     * No synchronization.
     */
    public static final List<String> MISSING_EXTENSIONS = new ArrayList<>();

    GLUtil.GLVendor mVendor;
    GLUtil.GLDriver mDriver;

    int mMaxFragmentUniformVectors;
    float mMaxTextureMaxAnisotropy = 1.f;
    boolean mSupportsProtected = false;
    boolean mSkipErrorChecks = false;
    int mMaxLabelLength;

    boolean mDebugSupport;

    boolean mBufferStorageSupport;

    boolean mDrawElementsBaseVertexSupport;
    boolean mBaseInstanceSupport;
    boolean mVertexAttribBindingSupport;
    boolean mProgramBinarySupport;
    boolean mProgramParameterSupport;
    boolean mCopyImageSupport;
    boolean mDSASupport;
    boolean mSPIRVSupport = false;
    boolean mViewCompatibilityClassSupport = false;
    boolean mTexStorageSupport;
    boolean mInvalidateFramebufferSupport;
    final boolean mVolatileContext;

    int[] mProgramBinaryFormats;

    public static final int
            INVALIDATE_BUFFER_TYPE_NULL_DATA = 1,
            INVALIDATE_BUFFER_TYPE_INVALIDATE = 2;
    int mInvalidateBufferType;
    int mGLSLVersion;

    /**
     * OpenGL texture format table.
     *
     * @see GLUtil#glFormatToIndex(int)
     */
    final FormatInfo[] mFormatTable =
            new FormatInfo[GLUtil.LAST_COLOR_FORMAT_INDEX + 1];

    // may contain GL_NONE(0) values that representing unsupported
    private final int[] mColorTypeToBackendFormat =
            new int[ColorInfo.CT_COUNT];
    private final GLBackendFormat[] mCompressionTypeToBackendFormat =
            new GLBackendFormat[ColorInfo.COMPRESSION_COUNT];

    GLCaps(ContextOptions options) {
        super(options);
        MISSING_EXTENSIONS.clear();
        mVolatileContext = options.mVolatileContext;
        // we currently don't use ARB_clip_control
        //TODO we need to make this a context option,
        // zero to one and reversed-Z is helpful in 3D rendering
        mDepthClipNegativeOneToOne = true;
    }

    void initFormatTable(boolean textureStorageSupported,
                         boolean EXT_texture_compression_s3tc) {
        final int nonMSAARenderFlags = FormatInfo.COLOR_ATTACHMENT_FLAG;
        final int msaaRenderFlags = nonMSAARenderFlags | FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG;

        // Reserved for undefined
        mFormatTable[0] = new FormatInfo();

        // Format: RGBA8
        {
            final int format = GL_RGBA8;
            FormatInfo info = mFormatTable[1] = new FormatInfo();
            assert (getFormatInfo(format) == info && GLUtil.glIndexToFormat(1) == format);
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = format;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ColorInfo.CT_RGBA_8888;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = format;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            // Format: RGBA8, Surface: kRGBA_8888
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGBA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGBA_8888, format);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA8, Surface: kRGBA_8888, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA8, Surface: kRGBA_8888, Data: kBGRA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_BGRA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }
            }

            // Format: RGBA8, Surface: kBGRA_8888
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_BGRA_8888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_BGRA_8888, format);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA8, Surface: kBGRA_8888, Data: kBGRA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_BGRA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }

                // Format: RGBA8, Surface: kBGRA_8888, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: RGBA8, Surface: kRGB_888x
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[2] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGB_888x;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.RGB1;

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                // Format: RGBA8, Surface: kRGB_888x, Data: kRGBA_888x
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGB_888x;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: R8
        {
            FormatInfo info = mFormatTable[2] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R8;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ColorInfo.CT_R_8;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_R8;

            info.mColorTypeInfos = new ColorTypeInfo[3];
            // Format: R8, Surface: kR_8
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_R_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_R_8, GL_R8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R8, Surface: kR_8, Data: kR_8
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_R_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kR_8, Data: kR_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_R_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: R8, Surface: kAlpha_8
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_ALPHA_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                setColorTypeFormat(ColorInfo.CT_ALPHA_8, GL_R8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R8, Surface: kAlpha_8, Data: kAlpha_8
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_ALPHA_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kAlpha_8, Data: kAlpha_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_ALPHA_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: R8, Surface: kGray_8
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[2] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_GRAY_8;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("rrr1");
                setColorTypeFormat(ColorInfo.CT_GRAY_8, GL_R8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R8, Surface: kGray_8, Data: kGray_8
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_GRAY_8;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R8, Surface: kGray_8, Data: kGray_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_GRAY_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGB565
        {
            FormatInfo info = mFormatTable[3] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB565;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT_5_6_5;
            info.mDefaultColorType = ColorInfo.CT_RGB_565;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RGB565;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: RGB565, Surface: kBGR_565
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGB_565;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGB_565, GL_RGB565);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB565, Surface: kBGR_565, Data: kBGR_565
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGB_565;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT_5_6_5;
                    ioFormat.mExternalTexImageFormat = GL_RGB;
                    ioFormat.mExternalReadFormat = GL_RGB;
                }

                // Format: RGB565, Surface: kBGR_565, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGBA16F
        {
            FormatInfo info = mFormatTable[4] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_RGBA16F;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ColorInfo.CT_RGBA_F16;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RGBA16F;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            // Format: RGBA16F, Surface: kRGBA_F16
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGBA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGBA_F16, GL_RGBA16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA16F, Surface: kRGBA_F16, Data: kRGBA_F16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA16F, Surface: kRGBA_F16, Data: kRGBA_F32
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Format: RGBA16F, Surface: kRGBA_F16_Clamped
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGBA_F16_CLAMPED;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGBA_F16_CLAMPED, GL_RGBA16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGBA16F, Surface: kRGBA_F16_Clamped, Data: kRGBA_F16_Clamped
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_F16_CLAMPED;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGBA16F, Surface: kRGBA_F16_Clamped, Data: kRGBA_F32
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: R16F
        {
            FormatInfo info = mFormatTable[5] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_R16F;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ColorInfo.CT_R_F16;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_R16F;

            // Format: R16F, Surface: kAlpha_F16
            info.mColorTypeInfos = new ColorTypeInfo[1];
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_ALPHA_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                setColorTypeFormat(ColorInfo.CT_ALPHA_F16, GL_R16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R16F, Surface: kAlpha_F16, Data: kAlpha_F16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_ALPHA_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R16F, Surface: kAlpha_F16, Data: kAlpha_F32xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_ALPHA_F32xxx;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGB8
        {
            FormatInfo info = mFormatTable[6] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB8;
            info.mDefaultExternalFormat = GL_RGB;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ColorInfo.CT_RGB_888;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RGB8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: RGB8, Surface: kRGB_888
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGB_888;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGB_888, GL_RGB8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB8, Surface: kRGB_888, Data: kRGB_888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGB_888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGB;
                    ioFormat.mExternalReadFormat = GL_RGB;
                }

                // Format: RGB8, Surface: kRGB_888, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RG8
        {
            FormatInfo info = mFormatTable[7] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG8;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ColorInfo.CT_RG_88;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RG8;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            // Format: RG8, Surface: kRG_88
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RG_88;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RG_88, GL_RG8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RG8, Surface: kRG_88, Data: kRG_88
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RG_88;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: RG8, Surface: kRG_88, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }

            // Added by Arc3D, this is useful for grayscale PNG image rendering.
            // Format: RG8, Surface: kGrayAlpha_88
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_GRAY_ALPHA_88;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("rrrg");
                setColorTypeFormat(ColorInfo.CT_GRAY_ALPHA_88, GL_RG8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                // Format: RG8, Surface: kGrayAlpha_88, Data: kGrayAlpha_88
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_GRAY_ALPHA_88;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }
            }
        }

        // Format: RGB10_A2
        {
            FormatInfo info = mFormatTable[8] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGB10_A2;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
            info.mDefaultColorType = ColorInfo.CT_RGBA_1010102;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RGB10_A2;

            info.mColorTypeInfos = new ColorTypeInfo[2];
            // Format: RGB10_A2, Surface: kRGBA_1010102
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGBA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGBA_1010102, GL_RGB10_A2);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_1010102
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_1010102;
                    ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: RGB10_A2, Surface: kRGBA_1010102, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
            //------------------------------------------------------------------
            // Format: RGB10_A2, Surface: kBGRA_1010102
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_BGRA_1010102;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_BGRA_1010102, GL_RGB10_A2);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kBGRA_1010102
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_BGRA_1010102;
                    ioFormat.mExternalType = GL_UNSIGNED_INT_2_10_10_10_REV;
                    ioFormat.mExternalTexImageFormat = GL_BGRA;
                    ioFormat.mExternalReadFormat = GL_BGRA;
                }

                // Format: RGB10_A2, Surface: kBGRA_1010102, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: SRGB8_ALPHA8
        {
            FormatInfo info = mFormatTable[9] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_SRGB8_ALPHA8;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_BYTE;
            info.mDefaultColorType = ColorInfo.CT_RGBA_8888_SRGB;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_SRGB8_ALPHA8;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGBA_8888_SRGB;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGBA_8888_SRGB, GL_SRGB8_ALPHA8);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[1];
                // Format: SRGB8_ALPHA8, Surface: kRGBA_8888_SRGB, Data: kRGBA_8888_SRGB
                {
                    // GL does not do srgb<->rgb conversions when transferring between cpu and gpu.
                    // Thus, the external format is GL_RGBA.
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888_SRGB;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: COMPRESSED_RGB8_ETC2
        {
            FormatInfo info = mFormatTable[10] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB8_ETC2;

            mCompressionTypeToBackendFormat[ColorInfo.COMPRESSION_ETC2_RGB8_UNORM] =
                    GLBackendFormat.make(GL_COMPRESSED_RGB8_ETC2);

            // There are no support ColorTypes for this format
        }

        // Format: COMPRESSED_RGB8_BC1
        {
            FormatInfo info = mFormatTable[11] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            if (EXT_texture_compression_s3tc) {
                info.mFlags = FormatInfo.TEXTURABLE_FLAG;

                mCompressionTypeToBackendFormat[ColorInfo.COMPRESSION_BC1_RGB8_UNORM] =
                        GLBackendFormat.make(GL_COMPRESSED_RGB_S3TC_DXT1_EXT);
            }

            // There are no support ColorTypes for this format
        }

        // Format: COMPRESSED_RGBA8_BC1
        {
            FormatInfo info = mFormatTable[12] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForTexture = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            if (EXT_texture_compression_s3tc) {
                info.mFlags = FormatInfo.TEXTURABLE_FLAG;

                mCompressionTypeToBackendFormat[ColorInfo.COMPRESSION_BC1_RGBA8_UNORM] =
                        GLBackendFormat.make(GL_COMPRESSED_RGBA_S3TC_DXT1_EXT);
            }

            // There are no support ColorTypes for this format
        }

        // Format: R16
        {
            FormatInfo info = mFormatTable[13] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_R16;
            info.mDefaultExternalFormat = GL_RED;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ColorInfo.CT_R_16;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_R16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: R16, Surface: kAlpha_16
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_ALPHA_16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                ctInfo.mReadSwizzle = Swizzle.make("000r");
                ctInfo.mWriteSwizzle = Swizzle.make("a000");
                setColorTypeFormat(ColorInfo.CT_ALPHA_16, GL_R16);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: R16, Surface: kAlpha_16, Data: kAlpha_16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_ALPHA_16;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalTexImageFormat = GL_RED;
                    ioFormat.mExternalReadFormat = GL_RED;
                }

                // Format: R16, Surface: kAlpha_16, Data: kAlpha_8xxx
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_ALPHA_8xxx;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RG16
        {
            FormatInfo info = mFormatTable[14] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RG16;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ColorInfo.CT_RG_1616;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RG16;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: GL_RG16, Surface: kRG_1616
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RG_1616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RG_1616, GL_RG16);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: GL_RG16, Surface: kRG_1616, Data: kRG_1616
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RG_1616;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: GL_RG16, Surface: kRG_1616, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format: RGBA16
        {
            FormatInfo info = mFormatTable[15] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_NORMALIZED_FIXED_POINT;
            info.mInternalFormatForRenderbuffer = GL_RGBA16;
            info.mDefaultExternalFormat = GL_RGBA;
            info.mDefaultExternalType = GL_UNSIGNED_SHORT;
            info.mDefaultColorType = ColorInfo.CT_RGBA_16161616;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RGBA16;

            // Format: GL_RGBA16, Surface: kRGBA_16161616
            info.mColorTypeInfos = new ColorTypeInfo[1];
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RGBA_16161616;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RGBA_16161616, GL_RGBA16);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_16161616
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_16161616;
                    ioFormat.mExternalType = GL_UNSIGNED_SHORT;
                    ioFormat.mExternalTexImageFormat = GL_RGBA;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }

                // Format: GL_RGBA16, Surface: kRGBA_16161616, Data: kRGBA_8888
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_8888;
                    ioFormat.mExternalType = GL_UNSIGNED_BYTE;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }

        // Format:RG16F
        {
            FormatInfo info = mFormatTable[16] = new FormatInfo();
            info.mFormatType = FormatInfo.FORMAT_TYPE_FLOAT;
            info.mInternalFormatForRenderbuffer = GL_RG16F;
            info.mDefaultExternalFormat = GL_RG;
            info.mDefaultExternalType = GL_HALF_FLOAT;
            info.mDefaultColorType = ColorInfo.CT_RG_F16;
            info.mFlags = FormatInfo.TEXTURABLE_FLAG | FormatInfo.TRANSFERS_FLAG;
            info.mFlags |= msaaRenderFlags;
            if (textureStorageSupported) {
                info.mFlags |= FormatInfo.TEXTURE_STORAGE_FLAG;
            }
            info.mInternalFormatForTexture = GL_RG16F;

            info.mColorTypeInfos = new ColorTypeInfo[1];
            // Format: GL_RG16F, Surface: kRG_F16
            {
                ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                ctInfo.mColorType = ColorInfo.CT_RG_F16;
                ctInfo.mFlags = ColorTypeInfo.UPLOAD_DATA_FLAG | ColorTypeInfo.RENDERABLE_FLAG;
                setColorTypeFormat(ColorInfo.CT_RG_F16, GL_RG16F);

                // External IO ColorTypes:
                ctInfo.mExternalIOFormats = new ExternalIOFormat[2];
                // Format: GL_RG16F, Surface: kRG_F16, Data: kRG_F16
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[0] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RG_F16;
                    ioFormat.mExternalType = GL_HALF_FLOAT;
                    ioFormat.mExternalTexImageFormat = GL_RG;
                    ioFormat.mExternalReadFormat = GL_RG;
                }

                // Format: GL_RG16F, Surface: kRG_F16, Data: kRGBA_F32
                {
                    ExternalIOFormat ioFormat = ctInfo.mExternalIOFormats[1] = new ExternalIOFormat();
                    ioFormat.mColorType = ColorInfo.CT_RGBA_F32;
                    ioFormat.mExternalType = GL_FLOAT;
                    ioFormat.mExternalTexImageFormat = 0;
                    ioFormat.mExternalReadFormat = GL_RGBA;
                }
            }
        }
    }

    boolean validateFormatTable() {
        // Validate, skip UNKNOWN
        for (int index = 1; index < mFormatTable.length; ++index) {
            FormatInfo info = mFormatTable[index];
            // Make sure we didn't set fbo attachable with msaa and not fbo attachable
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_WITH_MSAA_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) == 0) {
                assert false;
            }
            // Make sure all renderbuffer formats can also be texture formats
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0 &&
                    (info.mFlags & FormatInfo.TEXTURABLE_FLAG) == 0) {
                assert false;
            }

            // Make sure we set all the formats' FormatType
            if (info.mFormatType == FormatInfo.FORMAT_TYPE_UNKNOWN) {
                assert false;
            }

            // All texturable format should have their internal formats
            if ((info.mFlags & FormatInfo.TEXTURABLE_FLAG) != 0 &&
                    info.mInternalFormatForTexture == 0) {
                assert false;
            }

            // All renderable format should have their internal formats
            if ((info.mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0 &&
                    info.mInternalFormatForRenderbuffer == 0) {
                assert false;
            }

            // Make sure if we added a ColorTypeInfo we filled it out
            for (ColorTypeInfo ctInfo : info.mColorTypeInfos) {
                if (ctInfo.mColorType == ColorInfo.CT_UNKNOWN) {
                    assert false;
                }
                // Seems silly to add a color type if we don't support any flags on it
                if (ctInfo.mFlags == 0) {
                    assert false;
                }
                // Make sure if we added any ExternalIOFormats we filled it out
                for (ExternalIOFormat ioInfo : ctInfo.mExternalIOFormats) {
                    if (ioInfo.mColorType == ColorInfo.CT_UNKNOWN) {
                        assert false;
                    }
                }
            }
        }
        return true;
    }

    void applyDriverWorkaround() {
        var workarounds = mDriverBugWorkarounds;
    }

    FormatInfo getFormatInfo(@NativeType("GLenum") int format) {
        return mFormatTable[GLUtil.glFormatToIndex(format)];
    }

    private void setColorTypeFormat(int colorType, int format) {
        assert GLUtil.glFormatIsSupported(format);
        mColorTypeToBackendFormat[colorType] = format;
    }

    public GLUtil.GLVendor getVendor() {
        return mVendor;
    }

    public GLUtil.GLDriver getDriver() {
        return mDriver;
    }

    /**
     * Returns true if OpenGL ES (embedded system),
     * returns false if OpenGL (desktop, core profile).
     */
    public abstract boolean isGLES();

    /**
     * Modern OpenGL means OpenGL 4.5 is supported, so all of the following are supported:
     * <ul>
     *     <li>ARB_ES2_compatibility</li>
     *     <li>ARB_get_program_binary</li>
     *     <li>ARB_base_instance</li>
     *     <li>ARB_texture_storage</li>
     *     <li>ARB_internalformat_query</li>
     *     <li>ARB_shading_language_420pack</li>
     *     <li>ARB_invalidate_subdata</li>
     *     <li>ARB_explicit_uniform_location</li>
     *     <li>ARB_vertex_attrib_binding</li>
     *     <li>ARB_ES3_compatibility</li>
     *     <li>ARB_clear_texture</li>
     *     <li>ARB_buffer_storage</li>
     *     <li>ARB_enhanced_layouts</li>
     *     <li>ARB_texture_barrier</li>
     *     <li>ARB_direct_state_access</li>
     * </ul>
     * Arc3D requires OpenGL 3.3 at least.
     */
    public boolean hasDSASupport() {
        return mDSASupport;
    }

    public int getInvalidateBufferType() {
        return mInvalidateBufferType;
    }

    public boolean hasDebugSupport() {
        return mDebugSupport;
    }

    public boolean hasDrawElementsBaseVertexSupport() {
        return mDrawElementsBaseVertexSupport;
    }

    public boolean hasBaseInstanceSupport() {
        return mBaseInstanceSupport;
    }

    public boolean hasVertexAttribBindingSupport() {
        return mVertexAttribBindingSupport;
    }

    public boolean hasCopyImageSupport() {
        return mCopyImageSupport;
    }

    public boolean hasBufferStorageSupport() {
        return mBufferStorageSupport;
    }

    public boolean hasSPIRVSupport() {
        return mSPIRVSupport;
    }

    public boolean hasProgramBinarySupport() {
        return mProgramBinarySupport;
    }

    public boolean hasVolatileContext() {
        return mVolatileContext;
    }

    public boolean hasInvalidateFramebufferSupport() {
        return mInvalidateFramebufferSupport;
    }

    @Nullable
    public int[] getProgramBinaryFormats() {
        return mProgramBinarySupport ? mProgramBinaryFormats.clone() : null;
    }

    /**
     * Returns the minimum GLSL version that supported by the OpenGL device,
     * this is based on OpenGL version. May return 300, 310, 320 for es profile,
     * 330 or above for core profile.
     * <p>
     * The effective GLSL version that used by our pipeline and shader builder
     * is {@link ShaderCaps#mGLSLVersion}.
     */
    public int getGLSLVersion() {
        return mGLSLVersion;
    }

    @Override
    public boolean isFormatTexturable(BackendFormat format) {
        return isFormatTexturable(format.getGLFormat());
    }

    public boolean isFormatTexturable(int format) {
        return (getFormatInfo(format).mFlags & FormatInfo.TEXTURABLE_FLAG) != 0;
    }

    public boolean isTextureStorageCompatible(int format) {
        return (getFormatInfo(format).mFlags & FormatInfo.TEXTURE_STORAGE_FLAG) != 0;
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
        if (format.isExternal()) {
            return false;
        }
        int f = format.getGLFormat();
        if ((getFormatInfo(f).colorTypeFlags(colorType) & ColorTypeInfo.RENDERABLE_FLAG) == 0) {
            return false;
        }
        return isFormatRenderable(f, sampleCount);
    }

    @Override
    public boolean isFormatRenderable(BackendFormat format, int sampleCount) {
        if (format.isExternal()) {
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

        if (sampleCount <= 1) {
            return formatInfo.mColorSampleCounts[0] == 1 ? 1 : 0;
        }

        for (int count : formatInfo.mColorSampleCounts) {
            if (count >= sampleCount) {
                return count;
            }
        }
        return 0;
    }

    @Nullable
    @Override
    public ImageDesc getDefaultColorImageDesc(int imageType,
                                              int colorType,
                                              int width, int height,
                                              int depthOrArraySize,
                                              int mipLevelCount, int sampleCount,
                                              int imageFlags) {
        //TODO depth and array size
        //TODO log errors
        if (width < 1 || height < 1 || depthOrArraySize < 1 ||
                mipLevelCount < 0 || sampleCount < 0) {
            return null;
        }
        //TODO make texture sample counts and renderbuffer sample counts
        int format = mColorTypeToBackendFormat[colorType];
        FormatInfo formatInfo = getFormatInfo(format);
        if ((imageFlags & ISurface.FLAG_PROTECTED) != 0) {
            return null;
        }
        final int depth;
        final int arraySize;
        switch (imageType) {
            case Engine.ImageType.k3D:
                depth = depthOrArraySize;
                arraySize = 1;
                break;
            case Engine.ImageType.k2DArray, Engine.ImageType.kCubeArray:
                depth = 1;
                arraySize = depthOrArraySize;
                break;
            default:
                depth = arraySize = 1;
                break;
        }
        final int target;
        if ((imageFlags & (ISurface.FLAG_SAMPLED_IMAGE | ISurface.FLAG_STORAGE_IMAGE)) != 0) {
            final int maxSize = maxTextureSize();
            if (width > maxSize || height > maxSize || !formatInfo.isTexturable()) {
                return null;
            }
            target = GL_TEXTURE_2D;
        } else if ((imageFlags & ISurface.FLAG_RENDERABLE) != 0) {
            final int maxSize = maxRenderTargetSize();
            if (width > maxSize || height > maxSize) {
                return null;
            }
            //TODO if cannot make renderbuffer, create texture instead
            target = GL_RENDERBUFFER;
        } else {
            return null;
        }
        int maxMipLevels = DataUtils.computeMipLevelCount(width, height, depth);
        if (mipLevelCount == 0) {
            mipLevelCount = (imageFlags & ISurface.FLAG_MIPMAPPED) != 0
                    ? maxMipLevels
                    : 1; // only base level
        } else {
            mipLevelCount = Math.min(mipLevelCount, maxMipLevels);
        }
        if ((imageFlags & ISurface.FLAG_RENDERABLE) != 0) {
            if ((formatInfo.colorTypeFlags(colorType) & ColorTypeInfo.RENDERABLE_FLAG) == 0) {
                return null;
            }
            sampleCount = getRenderTargetSampleCount(sampleCount, format);
            if (sampleCount == 0) {
                return null;
            }
        } else {
            sampleCount = 1;
        }
        if (sampleCount > 1 && mipLevelCount > 1) {
            return null;
        }
        // ignore MEMORYLESS flag
        return new GLImageDesc(target,
                format, width, height,
                depth, arraySize,
                mipLevelCount, sampleCount,
                imageFlags);
    }

    @Nullable
    @Override
    public ImageDesc getDefaultDepthStencilImageDesc(int depthBits, int stencilBits,
                                                     int width, int height,
                                                     int sampleCount, int imageFlags) {
        if (depthBits < 0 || depthBits > 32) {
            return null;
        }
        if (stencilBits < 0 || stencilBits > 8) {
            return null;
        }
        depthBits = MathUtil.align8(depthBits);
        stencilBits = MathUtil.align8(stencilBits);
        int depthStencilFormat = 0;
        if (stencilBits == 8) {
            switch (depthBits) {
                case 0 -> depthStencilFormat = GL_STENCIL_INDEX8;
                case 8, 16, 24 -> depthStencilFormat = GL_DEPTH24_STENCIL8;
                case 32 -> depthStencilFormat = GL_DEPTH32F_STENCIL8;
            }
        } else {
            assert stencilBits == 0;
            switch (depthBits) {
                case 8, 16 -> depthStencilFormat = GL_DEPTH_COMPONENT16;
                case 24 -> depthStencilFormat = GL_DEPTH_COMPONENT24;
                case 32 -> depthStencilFormat = GL_DEPTH_COMPONENT32F;
            }
        }
        if (depthStencilFormat == 0) {
            assert depthBits == 0;
            return null;
        }
        // these 6 formats are "Req. format" is OpenGL spec

        //TODO 2D texture version, 2D array texture version
        return new GLImageDesc(GL_RENDERBUFFER,
                depthStencilFormat, width, height, 1, 1, 1, sampleCount, imageFlags);
    }

    @Nullable
    @Override
    public ImageDesc getImageDescForSampledCopy(ImageDesc src,
                                                int width, int height,
                                                int depthOrArraySize,
                                                int imageFlags) {
        if (!(src instanceof GLImageDesc glSrc)) {
            return null;
        }
        //TODO
        final int maxSize = maxTextureSize();
        if (width > maxSize || height > maxSize) {
            return null;
        }
        int mipLevelCount;
        int maxMipLevels = DataUtils.computeMipLevelCount(width, height, 1);
        mipLevelCount = (imageFlags & ISurface.FLAG_MIPMAPPED) != 0
                ? maxMipLevels
                : 1; // only base level
        return new GLImageDesc(
                GL_TEXTURE_2D, glSrc.getGLFormat(),
                width, height, 1, 1,
                mipLevelCount, 1, imageFlags | ISurface.FLAG_SAMPLED_IMAGE
        );
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
    protected BackendFormat onGetDefaultBackendFormat(int colorType) {
        return null;//mColorTypeToBackendFormat[colorType];
    }

    @Nullable
    @Override
    public BackendFormat getCompressedBackendFormat(int compressionType) {
        return mCompressionTypeToBackendFormat[compressionType];
    }

    @Nonnull
    @Override
    public PipelineKey_old makeDesc(PipelineKey_old desc,
                                    GpuRenderTarget renderTarget,
                                    final GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        return PipelineKey_old.build(desc, graphicsPipelineDesc, this);
    }

    @Nonnull
    @Override
    public PipelineKey makeGraphicsPipelineKey(PipelineKey old,
                                               PipelineDesc pipelineDesc,
                                               RenderPassDesc renderPassDesc) {
        if (old instanceof GLGraphicsPipelineKey pipelineKey) {
            pipelineKey.mPipelineDesc = pipelineDesc;
            return pipelineKey;
        } else {
            GLGraphicsPipelineKey pipelineKey = new GLGraphicsPipelineKey();
            pipelineKey.mPipelineDesc = pipelineDesc;
            return pipelineKey;
        }
    }

    @Override
    protected short onGetReadSwizzle(ImageDesc desc, int colorType) {
        final FormatInfo formatInfo = getFormatInfo(desc.getGLFormat());
        for (final ColorTypeInfo ctInfo : formatInfo.mColorTypeInfos) {
            if (ctInfo.mColorType == colorType) {
                return ctInfo.mReadSwizzle;
            }
        }
        assert false;
        return Swizzle.RGBA;
    }

    @Override
    public short getWriteSwizzle(ImageDesc desc, int colorType) {
        final FormatInfo formatInfo = getFormatInfo(desc.getGLFormat());
        for (final ColorTypeInfo ctInfo : formatInfo.mColorTypeInfos) {
            if (ctInfo.mColorType == colorType) {
                return ctInfo.mWriteSwizzle;
            }
        }
        assert false;
        return Swizzle.RGBA;
    }

    @Override
    public IResourceKey computeImageKey(ImageDesc desc,
                                        IResourceKey recycle) {
        if (desc instanceof GLImageDesc glDesc) {
            return new GLImage.ResourceKey(glDesc);
        }
        return null;
    }

    @Override
    public long getSupportedWriteColorType(int dstColorType, ImageDesc dstDesc, int srcColorType) {
        // We first try to find a supported write pixels ColorType that matches the data's
        // srcColorType. If that doesn't exist we will use any supported ColorType.
        int fallbackCT = ColorInfo.CT_UNKNOWN;
        final FormatInfo formatInfo = getFormatInfo(dstDesc.getGLFormat());
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
                    if (ioInfo.mExternalTexImageFormat != 0) {
                        if (ioInfo.mColorType == srcColorType) {
                            return srcColorType | (transferOffsetAlignment << 32);
                        }
                        // Currently we just pick the first supported format that we find as our
                        // fallback.
                        if (fallbackCT == ColorInfo.CT_UNKNOWN) {
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
        if (compression != ColorInfo.COMPRESSION_NONE) {
            return (DataUtils.compressionTypeIsOpaque(compression) ?
                    ColorInfo.CT_RGB_888x :
                    ColorInfo.CT_RGBA_8888); // alignment = 0
        }

        // We first try to find a supported read pixels ColorType that matches the requested
        // dstColorType. If that doesn't exist we will use any valid read pixels ColorType.
        int fallbackColorType = ColorInfo.CT_UNKNOWN;
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
                        if (fallbackColorType == ColorInfo.CT_UNKNOWN) {
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
        } else {
            // NVIDIA uses threaded driver then error checks can be very slow
            mSkipErrorChecks = (mDriver == GLUtil.GLDriver.NVIDIA);
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
     * Gets the default external format to use with glTex[Sub]Image... when the data pointer is null.
     */
    public int getFormatDefaultExternalFormat(int format) {
        return getFormatInfo(format).mDefaultExternalFormat;
    }

    /**
     * Gets the default external type to use with glTex[Sub]Image... when the data pointer is null.
     */
    public int getFormatDefaultExternalType(int format) {
        return getFormatInfo(format).mDefaultExternalType;
    }

    public int getPixelsExternalFormat(int format, int dstColorType, int srcColorType, boolean write) {
        return getFormatInfo(format).externalFormat(
                dstColorType, srcColorType, write);
    }

    public int getPixelsExternalType(int format, int dstColorType, int srcColorType) {
        return getFormatInfo(format).externalType(
                dstColorType, srcColorType);
    }

    public boolean canCopyImage(@NativeType("GLenum") int srcFormat, int srcSampleCount,
                                @NativeType("GLenum") int dstFormat, int dstSampleCount) {
        if (!mCopyImageSupport) {
            return false;
        }
        if ((dstSampleCount > 1 || srcSampleCount > 1) &&
                dstSampleCount != srcSampleCount) {
            return false;
        }
        if (srcFormat == dstFormat) {
            return true;
        }
        if (mViewCompatibilityClassSupport) {
            return getFormatInfo(srcFormat).mViewCompatibilityClass ==
                    getFormatInfo(dstFormat).mViewCompatibilityClass;
        }
        return false;
    }

    public boolean canCopyTexSubImage(@NativeType("GLenum") int srcFormat,
                                      @NativeType("GLenum") int dstFormat) {
        // channels should be compatible
        if (getFormatDefaultExternalType(dstFormat) !=
                getFormatDefaultExternalType(srcFormat)) {
            return false;
        }
        if (GLUtil.glFormatIsSRGB(dstFormat) != GLUtil.glFormatIsSRGB(srcFormat)) {
            return false;
        }
        return (getFormatInfo(srcFormat).mFlags & FormatInfo.COLOR_ATTACHMENT_FLAG) != 0;
    }

    /**
     * Skip checks for GL errors, shader compilation success, program link success.
     */
    public boolean skipErrorChecks() {
        return mSkipErrorChecks;
    }

    public int maxLabelLength() {
        return mMaxLabelLength;
    }

    public float maxTextureMaxAnisotropy() {
        return mMaxTextureMaxAnisotropy;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeFormatTable) {
        return "GLCaps{" +
                "mProgramBinaryFormats=" + Arrays.toString(mProgramBinaryFormats) +
                ", mMaxFragmentUniformVectors=" + mMaxFragmentUniformVectors +
                ", mMaxTextureMaxAnisotropy=" + mMaxTextureMaxAnisotropy +
                ", mSupportsProtected=" + mSupportsProtected +
                ", mSkipErrorChecks=" + mSkipErrorChecks +
                ", mMaxLabelLength=" + mMaxLabelLength +
                ", mDebugSupport=" + mDebugSupport +
                ", mBufferStorageSupport=" + mBufferStorageSupport +
                ", mDrawElementsBaseVertexSupport=" + mDrawElementsBaseVertexSupport +
                ", mBaseInstanceSupport=" + mBaseInstanceSupport +
                ", mDSASupport=" + mDSASupport +
                ", mInvalidateBufferType=" + mInvalidateBufferType +
                (includeFormatTable ? ", mFormatTable=" + Arrays.toString(mFormatTable) : "") +
                ", mColorTypeToBackendFormat=" + Arrays.toString(mColorTypeToBackendFormat) +
                ", mCompressionTypeToBackendFormat=" + Arrays.toString(mCompressionTypeToBackendFormat) +
                ", mShaderCaps=" + mShaderCaps +
                ", mAnisotropySupport=" + mAnisotropySupport +
                ", mGpuTracingSupport=" + mGpuTracingSupport +
                ", mConservativeRasterSupport=" + mConservativeRasterSupport +
                ", mTransferPixelsToRowBytesSupport=" + mTransferPixelsToRowBytesSupport +
                ", mMustSyncGpuDuringDiscard=" + mMustSyncGpuDuringDiscard +
                ", mTextureBarrierSupport=" + mTextureBarrierSupport +
                ", mDynamicStateArrayGeometryProcessorTextureSupport=" + mDynamicStateArrayGeometryProcessorTextureSupport +
                ", mBlendEquationSupport=" + mBlendEquationSupport +
                ", mMapBufferFlags=" + mMapBufferFlags +
                ", mMaxRenderTargetSize=" + mMaxRenderTargetSize +
                ", mMaxPreferredRenderTargetSize=" + mMaxPreferredRenderTargetSize +
                ", mMaxVertexAttributes=" + mMaxVertexAttributes +
                ", mMaxTextureSize=" + mMaxTextureSize +
                ", mInternalMultisampleCount=" + mInternalMultisampleCount +
                ", mMaxPushConstantsSize=" + mMaxPushConstantsSize +
                '}';
    }

    static class ExternalIOFormat {

        int mColorType = ColorInfo.CT_UNKNOWN;

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

        @Override
        public String toString() {
            return "ExternalIOFormat{" +
                    "colorType=" + ColorInfo.colorTypeToString(mColorType) +
                    ", externalType=0x" + Integer.toHexString(mExternalType) +
                    ", externalTexImageFormat=0x" + Integer.toHexString(mExternalTexImageFormat) +
                    ", externalReadFormat=0x" + Integer.toHexString(mExternalReadFormat) +
                    '}';
        }
    }

    static class ColorTypeInfo {

        int mColorType = ColorInfo.CT_UNKNOWN;

        static final int
                UPLOAD_DATA_FLAG = 0x1,
                RENDERABLE_FLAG = 0x2;
        int mFlags = 0;

        short mReadSwizzle = Swizzle.RGBA;
        short mWriteSwizzle = Swizzle.RGBA;

        ExternalIOFormat[] mExternalIOFormats;

        public int externalFormat(int srcColorType, boolean write) {
            for (ExternalIOFormat ioFormat : mExternalIOFormats) {
                if (ioFormat.mColorType == srcColorType) {
                    if (write) {
                        return ioFormat.mExternalTexImageFormat;
                    } else {
                        return ioFormat.mExternalReadFormat;
                    }
                }
            }
            return 0;
        }

        public int externalType(int srcColorType) {
            for (ExternalIOFormat ioFormat : mExternalIOFormats) {
                if (ioFormat.mColorType == srcColorType) {
                    return ioFormat.mExternalType;
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            return "ColorTypeInfo{" +
                    "colorType=" + ColorInfo.colorTypeToString(mColorType) +
                    ", flags=0x" + Integer.toHexString(mFlags) +
                    ", readSwizzle=" + Swizzle.toString(mReadSwizzle) +
                    ", writeSwizzle=" + Swizzle.toString(mWriteSwizzle) +
                    ", externalIOFormats=" + Arrays.toString(mExternalIOFormats) +
                    '}';
        }
    }

    static class FormatInfo {

        /**
         * COLOR_ATTACHMENT_FLAG: even if the format cannot be a RenderTarget, we can still attach
         * it to a framebuffer for blitting or reading pixels.
         * <p>
         * TRANSFERS_FLAG: pixel buffer objects supported in/out of this format.
         */
        static final int
                TEXTURABLE_FLAG = 0x01,
                COLOR_ATTACHMENT_FLAG = 0x02,
                COLOR_ATTACHMENT_WITH_MSAA_FLAG = 0x04,
                TEXTURE_STORAGE_FLAG = 0x08,
                TRANSFERS_FLAG = 0x10;
        int mFlags = 0;

        static final int
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
        int mDefaultColorType = ColorInfo.CT_UNKNOWN;

        // OpenGL 4.3 ViewCompatibilityClass
        int mViewCompatibilityClass = 0;

        int[] mColorSampleCounts = {};

        ColorTypeInfo[] mColorTypeInfos = {};

        public boolean isTexturable() {
            return (mFlags & FormatInfo.TEXTURABLE_FLAG) != 0;
        }

        public int colorTypeFlags(int colorType) {
            for (ColorTypeInfo info : mColorTypeInfos) {
                if (info.mColorType == colorType) {
                    return info.mFlags;
                }
            }
            return 0;
        }

        public int externalFormat(int dstColorType, int srcColorType, boolean write) {
            for (ColorTypeInfo info : mColorTypeInfos) {
                if (info.mColorType == dstColorType) {
                    return info.externalFormat(srcColorType, write);
                }
            }
            return 0;
        }

        public int externalType(int dstColorType, int srcColorType) {
            for (ColorTypeInfo info : mColorTypeInfos) {
                if (info.mColorType == dstColorType) {
                    return info.externalType(srcColorType);
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            return "FormatInfo{" +
                    "flags=0x" + Integer.toHexString(mFlags) +
                    ", formatType=" + mFormatType +
                    ", internalFormatForTexture=" + mInternalFormatForTexture +
                    ", internalFormatForRenderbuffer=" + mInternalFormatForRenderbuffer +
                    ", defaultExternalFormat=" + mDefaultExternalFormat +
                    ", defaultExternalType=" + mDefaultExternalType +
                    ", defaultColorType=" + ColorInfo.colorTypeToString(mDefaultColorType) +
                    ", colorSampleCounts=" + Arrays.toString(mColorSampleCounts) +
                    ", colorTypeInfos=" + Arrays.toString(mColorTypeInfos) +
                    '}';
        }
    }
}
