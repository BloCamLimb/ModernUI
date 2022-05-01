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

import icyllis.arcui.core.Color;
import icyllis.arcui.core.Image;
import org.lwjgl.system.NativeType;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.EXTTextureStorage.*;
import static org.lwjgl.opengl.GL45C.*;

public final class GLUtil {

    /**
     * Single Alpha and Gray channel are deprecated in Modern OpenGL core profile,
     * they should be replaced by single Red channel. But it may be still supported
     * by some extensions, using builtin swizzle.
     *
     * @param format see GLTypes
     * @return see Color
     */
    public static int glFormatChannels(GLFormat format) {
        return switch (format) {
            case UNKNOWN,
                    DEPTH24_STENCIL8,
                    STENCIL_INDEX16,
                    STENCIL_INDEX8 -> 0;
            case RGBA8,
                    RGBA16,
                    COMPRESSED_RGBA8_BC1,
                    SRGB8_ALPHA8,
                    RGBA4,
                    RGB10_A2,
                    RGBA16F,
                    BGRA8 -> Color.RGBA_CHANNEL_FLAGS;
            case R8,
                    R16,
                    R16F -> Color.RED_CHANNEL_FLAG;
            case ALPHA8 -> Color.ALPHA_CHANNEL_FLAG;
            case LUMINANCE8,
                    LUMINANCE16F -> Color.GRAY_CHANNEL_FLAG;
            case LUMINANCE8_ALPHA8 -> Color.GRAY_ALPHA_CHANNEL_FLAGS;
            case RGB565,
                    COMPRESSED_RGB8_BC1,
                    COMPRESSED_RGB8_ETC2,
                    RGB8 -> Color.RGB_CHANNEL_FLAGS;
            case RG8,
                    RG16F,
                    RG16 -> Color.RG_CHANNEL_FLAGS;
        };
    }

    /**
     * @param glFormat see GL45C, EXTTextureStorage, EXTTextureCompressionS3TC
     * @return see GLTypes
     */
    public static GLFormat glFormatFromEnum(@NativeType("GLenum") int glFormat) {
        return switch (glFormat) {
            case GL_RGBA8 -> GLFormat.RGBA8;
            case GL_R8 -> GLFormat.R8;
            case GL_ALPHA8_EXT -> GLFormat.ALPHA8;
            case GL_LUMINANCE8_EXT -> GLFormat.LUMINANCE8;
            case GL_LUMINANCE8_ALPHA8_EXT -> GLFormat.LUMINANCE8_ALPHA8;
            case GL_BGRA8_EXT -> GLFormat.BGRA8;
            case GL_RGB565 -> GLFormat.RGB565;
            case GL_RGBA16F -> GLFormat.RGBA16F;
            case GL_LUMINANCE16F_EXT -> GLFormat.LUMINANCE16F;
            case GL_R16F -> GLFormat.R16F;
            case GL_RGB8 -> GLFormat.RGB8;
            case GL_RG8 -> GLFormat.RG8;
            case GL_RGB10_A2 -> GLFormat.RGB10_A2;
            case GL_RGBA4 -> GLFormat.RGBA4;
            case GL_SRGB8_ALPHA8 -> GLFormat.SRGB8_ALPHA8;
            case GL_COMPRESSED_RGB8_ETC2 -> GLFormat.COMPRESSED_RGB8_ETC2;
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> GLFormat.COMPRESSED_RGB8_BC1;
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> GLFormat.COMPRESSED_RGBA8_BC1;
            case GL_R16 -> GLFormat.R16;
            case GL_RG16 -> GLFormat.RG16;
            case GL_RGBA16 -> GLFormat.RGBA16;
            case GL_RG16F -> GLFormat.RG16F;
            case GL_STENCIL_INDEX8 -> GLFormat.STENCIL_INDEX8;
            case GL_STENCIL_INDEX16 -> GLFormat.STENCIL_INDEX16;
            case GL_DEPTH24_STENCIL8 -> GLFormat.DEPTH24_STENCIL8;
            default -> GLFormat.UNKNOWN;
        };
    }

    /**
     * Returns either the sized internal format or compressed internal format of the GrGLFormat.
     */
    public static int glFormatToEnum(GLFormat format) {
        return switch (format) {
            case RGBA8 -> GL_RGBA8;
            case R8 -> GL_R8;
            case ALPHA8 -> GL_ALPHA8_EXT;
            case LUMINANCE8 -> GL_LUMINANCE8_EXT;
            case LUMINANCE8_ALPHA8 -> GL_LUMINANCE8_ALPHA8_EXT;
            case BGRA8 -> GL_BGRA8_EXT;
            case RGB565 -> GL_RGB565;
            case RGBA16F -> GL_RGBA16F;
            case LUMINANCE16F -> GL_LUMINANCE16F_EXT;
            case R16F -> GL_R16F;
            case RGB8 -> GL_RGB8;
            case RG8 -> GL_RG8;
            case RGB10_A2 -> GL_RGB10_A2;
            case RGBA4 -> GL_RGBA4;
            case SRGB8_ALPHA8 -> GL_SRGB8_ALPHA8;
            case COMPRESSED_RGB8_ETC2 -> GL_COMPRESSED_RGB8_ETC2;
            case COMPRESSED_RGB8_BC1 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case COMPRESSED_RGBA8_BC1 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case R16 -> GL_R16;
            case RG16 -> GL_RG16;
            case RGBA16 -> GL_RGBA16;
            case RG16F -> GL_RG16F;
            case STENCIL_INDEX8 -> GL_STENCIL_INDEX8;
            case STENCIL_INDEX16 -> GL_STENCIL_INDEX16;
            case DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8;
            case UNKNOWN -> GL_NONE;
        };
    }

    public static int glFormatCompressionType(GLFormat format) {
        return switch (format) {
            case COMPRESSED_RGB8_ETC2 -> Image.COMPRESSION_TYPE_ETC2_RGB8_UNORM;
            case COMPRESSED_RGB8_BC1 -> Image.COMPRESSION_TYPE_BC1_RGB8_UNORM;
            case COMPRESSED_RGBA8_BC1 -> Image.COMPRESSION_TYPE_BC1_RGBA8_UNORM;
            default -> Image.COMPRESSION_TYPE_NONE;
        };
    }

    public static int glFormatBytesPerBlock(GLFormat format) {
        return switch (format) {
            case RGBA8,
                    DEPTH24_STENCIL8,
                    RG16F,
                    RG16,
                    SRGB8_ALPHA8,
                    RGB10_A2,
                    // We assume the GPU stores this format 4 byte aligned
                    RGB8,
                    BGRA8 -> 4;
            case R8,
                    STENCIL_INDEX8,
                    LUMINANCE8,
                    ALPHA8 -> 1;
            case LUMINANCE8_ALPHA8,
                    STENCIL_INDEX16,
                    R16,
                    RGBA4,
                    RG8,
                    R16F,
                    LUMINANCE16F,
                    RGB565 -> 2;
            case RGBA16F,
                    RGBA16,
                    COMPRESSED_RGBA8_BC1,
                    COMPRESSED_RGB8_BC1,
                    COMPRESSED_RGB8_ETC2 -> 8;
            case UNKNOWN -> 0;
        };
    }

    public static int glFormatStencilBits(GLFormat format) {
        return switch (format) {
            case STENCIL_INDEX8,
                    DEPTH24_STENCIL8 -> 8;
            case STENCIL_INDEX16 -> 16;
            case COMPRESSED_RGB8_ETC2,
                    COMPRESSED_RGB8_BC1,
                    COMPRESSED_RGBA8_BC1,
                    RGBA8,
                    R8,
                    ALPHA8,
                    LUMINANCE8,
                    LUMINANCE8_ALPHA8,
                    BGRA8,
                    RGB565,
                    RGBA16F,
                    R16F,
                    LUMINANCE16F,
                    RGB8,
                    RG8,
                    RGB10_A2,
                    RGBA4,
                    SRGB8_ALPHA8,
                    R16,
                    RG16,
                    RGBA16,
                    RG16F,
                    UNKNOWN -> 0;
        };
    }

    public static boolean glFormatIsPackedDepthStencil(GLFormat format) {
        return switch (format) {
            case DEPTH24_STENCIL8 -> true;
            case COMPRESSED_RGB8_ETC2,
                    COMPRESSED_RGB8_BC1,
                    COMPRESSED_RGBA8_BC1,
                    RGBA8,
                    R8,
                    ALPHA8,
                    LUMINANCE8,
                    LUMINANCE8_ALPHA8,
                    BGRA8,
                    RGB565,
                    RGBA16F,
                    R16F,
                    LUMINANCE16F,
                    RGB8,
                    RG8,
                    RGB10_A2,
                    RGBA4,
                    SRGB8_ALPHA8,
                    R16,
                    RG16,
                    RGBA16,
                    RG16F,
                    STENCIL_INDEX8,
                    STENCIL_INDEX16,
                    UNKNOWN -> false;
        };
    }

    public static boolean glFormatIsSRGB(GLFormat format) {
        return switch (format) {
            case SRGB8_ALPHA8 -> true;
            case COMPRESSED_RGB8_ETC2,
                    COMPRESSED_RGB8_BC1,
                    COMPRESSED_RGBA8_BC1,
                    RGBA8,
                    R8,
                    ALPHA8,
                    LUMINANCE8,
                    LUMINANCE8_ALPHA8,
                    BGRA8,
                    RGB565,
                    RGBA16F,
                    R16F,
                    LUMINANCE16F,
                    RGB8,
                    RG8,
                    RGB10_A2,
                    RGBA4,
                    R16,
                    RG16,
                    RGBA16,
                    RG16F,
                    STENCIL_INDEX8,
                    STENCIL_INDEX16,
                    DEPTH24_STENCIL8,
                    UNKNOWN -> false;
        };
    }

    public static boolean glFormatIsCompressed(GLFormat format) {
        return switch (format) {
            case COMPRESSED_RGB8_ETC2,
                    COMPRESSED_RGB8_BC1,
                    COMPRESSED_RGBA8_BC1 -> true;
            case RGBA8,
                    R8,
                    ALPHA8,
                    LUMINANCE8,
                    LUMINANCE8_ALPHA8,
                    BGRA8,
                    RGB565,
                    RGBA16F,
                    R16F,
                    LUMINANCE16F,
                    RGB8,
                    RG8,
                    RGB10_A2,
                    RGBA4,
                    SRGB8_ALPHA8,
                    R16,
                    RG16,
                    RGBA16,
                    RG16F,
                    STENCIL_INDEX8,
                    STENCIL_INDEX16,
                    DEPTH24_STENCIL8,
                    UNKNOWN -> false;
        };
    }

    public static String glFormatName(@NativeType("GLenum") int glFormat) {
        return switch (glFormat) {
            case GL_RGBA8 -> "RGBA8";
            case GL_R8 -> "R8";
            case GL_ALPHA8_EXT -> "ALPHA8";
            case GL_LUMINANCE8_EXT -> "LUMINANCE8";
            case GL_LUMINANCE8_ALPHA8_EXT -> "LUMINANCE8_ALPHA8";
            case GL_BGRA8_EXT -> "BGRA8";
            case GL_RGB565 -> "RGB565";
            case GL_RGBA16F -> "RGBA16F";
            case GL_LUMINANCE16F_EXT -> "LUMINANCE16F";
            case GL_R16F -> "R16F";
            case GL_RGB8 -> "RGB8";
            case GL_RG8 -> "RG8";
            case GL_RGB10_A2 -> "RGB10_A2";
            case GL_RGBA4 -> "RGBA4";
            case GL_RGBA32F -> "RGBA32F";
            case GL_SRGB8_ALPHA8 -> "SRGB8_ALPHA8";
            case GL_COMPRESSED_RGB8_ETC2 -> "ETC2";
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> "RGB8_BC1";
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> "RGBA8_BC1";
            case GL_R16 -> "R16";
            case GL_RG16 -> "RG16";
            case GL_RGBA16 -> "RGBA16";
            case GL_RG16F -> "RG16F";
            case GL_STENCIL_INDEX8 -> "STENCIL_INDEX8";
            case GL_STENCIL_INDEX16 -> "STENCIL_INDEX16";
            case GL_DEPTH24_STENCIL8 -> "DEPTH24_STENCIL8";
            default -> "Unknown";
        };
    }

    private GLUtil() {
    }
}
