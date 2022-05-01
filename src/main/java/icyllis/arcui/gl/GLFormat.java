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
import icyllis.arcui.hgi.Swizzle;

import java.util.Arrays;

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

/**
 * The supported GL formats represented as an enum.
 * <p>
 * Because alpha and gray channel are deprecated in OpenGL core profile, they
 * should be replaced by R8 and RG8 format. But it may be still supported in
 * external IO formats, using a builtin swizzle, but not supported to create
 * textures.
 * <p>
 * As a convenience, stencil formats are also listed here.
 */
public enum GLFormat {
    UNKNOWN,
    RGBA8,
    R8,
    ALPHA8,
    LUMINANCE8,
    LUMINANCE8_ALPHA8,
    BGRA8,
    RGB565,
    RGBA16F,
    R16F,
    RGB8,
    RG8,
    RGB10_A2,
    RGBA4,
    SRGB8_ALPHA8,
    COMPRESSED_RGB8_ETC2,
    COMPRESSED_RGB8_BC1,
    COMPRESSED_RGBA8_BC1,
    R16,
    RG16,
    RGBA16,
    RG16F,
    LUMINANCE16F,
    STENCIL_INDEX8,
    STENCIL_INDEX16,
    DEPTH24_STENCIL8;

    /**
     * A read-only array that lists all color formats for iteration purposes.
     */
    public static final GLFormat[] COLOR_TABLE = Arrays.copyOf(values(), GLFormat.STENCIL_INDEX8.ordinal());

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
        for (var info : mColorTypeInfos) {
            if (info.mColorType == colorType) {
                return info.mFlags;
            }
        }
        return 0;
    }

    public boolean formatSupportsTexStorage() {
        return (mFlags & USE_TEX_STORAGE_FLAG) != 0;
    }

    /**
     * Gets the internal format to use with glTexImage...() and glTexStorage...(). May be sized or
     * base depending upon the GL. Not applicable to compressed textures.
     */
    public int getTexImageOrStorageInternalFormat() {
        return mInternalFormatForTexture;
    }
}
