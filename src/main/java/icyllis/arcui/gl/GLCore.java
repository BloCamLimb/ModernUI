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
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.NativeType;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.EXTTextureStorage.*;

/**
 * Provides native interfaces of OpenGL 4.5 core and user-defined utilities.
 */
public final class GLCore extends GL45C {

    @SuppressWarnings("StatementWithEmptyBody")
    public static void clearErrors() {
        while (glGetError() != GL_NO_ERROR)
            ;
    }

    /**
     * @param format see GLTypes
     * @return see Color
     */
    public static int glFormatChannels(int format) {
        return switch (format) {
            case GLTypes.FORMAT_UNKNOWN,
                    GLTypes.FORMAT_DEPTH24_STENCIL8,
                    GLTypes.FORMAT_STENCIL_INDEX16,
                    GLTypes.FORMAT_STENCIL_INDEX8 -> 0;
            case GLTypes.FORMAT_RGBA8,
                    GLTypes.FORMAT_RGBA16,
                    GLTypes.FORMAT_COMPRESSED_RGBA8_BC1,
                    GLTypes.FORMAT_SRGB8_ALPHA8,
                    GLTypes.FORMAT_RGBA4,
                    GLTypes.FORMAT_RGB10_A2,
                    GLTypes.FORMAT_RGBA16F,
                    GLTypes.FORMAT_BGRA8 -> Color.RGBA_CHANNEL_FLAGS;
            case GLTypes.FORMAT_R8,
                    GLTypes.FORMAT_R16,
                    GLTypes.FORMAT_R16F -> Color.RED_CHANNEL_FLAG;
            case GLTypes.FORMAT_ALPHA8 -> Color.ALPHA_CHANNEL_FLAG;
            case GLTypes.FORMAT_LUMINANCE8,
                    GLTypes.FORMAT_LUMINANCE16F -> Color.GRAY_CHANNEL_FLAG;
            case GLTypes.FORMAT_LUMINANCE8_ALPHA8 -> Color.GRAY_ALPHA_CHANNEL_FLAGS;
            case GLTypes.FORMAT_RGB565,
                    GLTypes.FORMAT_COMPRESSED_RGB8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGB8_ETC2,
                    GLTypes.FORMAT_RGB8 -> Color.RGB_CHANNEL_FLAGS;
            case GLTypes.FORMAT_RG8,
                    GLTypes.FORMAT_RG16F,
                    GLTypes.FORMAT_RG16 -> Color.RG_CHANNEL_FLAGS;
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * @param glFormat see GL45C, EXTTextureStorage, EXTTextureCompressionS3TC
     * @return see GLTypes
     */
    public static int glFormatFromEnum(@NativeType("GLenum") int glFormat) {
        return switch (glFormat) {
            case GL_RGBA8 -> GLTypes.FORMAT_RGBA8;
            case GL_R8 -> GLTypes.FORMAT_R8;
            case GL_ALPHA8_EXT -> GLTypes.FORMAT_ALPHA8;
            case GL_LUMINANCE8_EXT -> GLTypes.FORMAT_LUMINANCE8;
            case GL_LUMINANCE8_ALPHA8_EXT -> GLTypes.FORMAT_LUMINANCE8_ALPHA8;
            case GL_BGRA8_EXT -> GLTypes.FORMAT_BGRA8;
            case GL_RGB565 -> GLTypes.FORMAT_RGB565;
            case GL_RGBA16F -> GLTypes.FORMAT_RGBA16F;
            case GL_LUMINANCE16F_EXT -> GLTypes.FORMAT_LUMINANCE16F;
            case GL_R16F -> GLTypes.FORMAT_R16F;
            case GL_RGB8 -> GLTypes.FORMAT_RGB8;
            case GL_RG8 -> GLTypes.FORMAT_RG8;
            case GL_RGB10_A2 -> GLTypes.FORMAT_RGB10_A2;
            case GL_RGBA4 -> GLTypes.FORMAT_RGBA4;
            case GL_SRGB8_ALPHA8 -> GLTypes.FORMAT_SRGB8_ALPHA8;
            case GL_COMPRESSED_RGB8_ETC2 -> GLTypes.FORMAT_COMPRESSED_RGB8_ETC2;
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> GLTypes.FORMAT_COMPRESSED_RGB8_BC1;
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> GLTypes.FORMAT_COMPRESSED_RGBA8_BC1;
            case GL_R16 -> GLTypes.FORMAT_R16;
            case GL_RG16 -> GLTypes.FORMAT_RG16;
            case GL_RGBA16 -> GLTypes.FORMAT_RGBA16;
            case GL_RG16F -> GLTypes.FORMAT_RG16F;
            case GL_STENCIL_INDEX8 -> GLTypes.FORMAT_STENCIL_INDEX8;
            case GL_STENCIL_INDEX16 -> GLTypes.FORMAT_STENCIL_INDEX16;
            case GL_DEPTH24_STENCIL8 -> GLTypes.FORMAT_DEPTH24_STENCIL8;
            default -> GLTypes.FORMAT_UNKNOWN;
        };
    }

    /**
     * Returns either the sized internal format or compressed internal format of the GLFormat.
     */
    public static int glFormatToEnum(int format) {
        return switch (format) {
            case GLTypes.FORMAT_RGBA8 -> GL_RGBA8;
            case GLTypes.FORMAT_R8 -> GL_R8;
            case GLTypes.FORMAT_ALPHA8 -> GL_ALPHA8_EXT;
            case GLTypes.FORMAT_LUMINANCE8 -> GL_LUMINANCE8_EXT;
            case GLTypes.FORMAT_LUMINANCE8_ALPHA8 -> GL_LUMINANCE8_ALPHA8_EXT;
            case GLTypes.FORMAT_BGRA8 -> GL_BGRA8_EXT;
            case GLTypes.FORMAT_RGB565 -> GL_RGB565;
            case GLTypes.FORMAT_RGBA16F -> GL_RGBA16F;
            case GLTypes.FORMAT_LUMINANCE16F -> GL_LUMINANCE16F_EXT;
            case GLTypes.FORMAT_R16F -> GL_R16F;
            case GLTypes.FORMAT_RGB8 -> GL_RGB8;
            case GLTypes.FORMAT_RG8 -> GL_RG8;
            case GLTypes.FORMAT_RGB10_A2 -> GL_RGB10_A2;
            case GLTypes.FORMAT_RGBA4 -> GL_RGBA4;
            case GLTypes.FORMAT_SRGB8_ALPHA8 -> GL_SRGB8_ALPHA8;
            case GLTypes.FORMAT_COMPRESSED_RGB8_ETC2 -> GL_COMPRESSED_RGB8_ETC2;
            case GLTypes.FORMAT_COMPRESSED_RGB8_BC1 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case GLTypes.FORMAT_COMPRESSED_RGBA8_BC1 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case GLTypes.FORMAT_R16 -> GL_R16;
            case GLTypes.FORMAT_RG16 -> GL_RG16;
            case GLTypes.FORMAT_RGBA16 -> GL_RGBA16;
            case GLTypes.FORMAT_RG16F -> GL_RG16F;
            case GLTypes.FORMAT_STENCIL_INDEX8 -> GL_STENCIL_INDEX8;
            case GLTypes.FORMAT_STENCIL_INDEX16 -> GL_STENCIL_INDEX16;
            case GLTypes.FORMAT_DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8;
            case GLTypes.FORMAT_UNKNOWN -> GL_NONE;
            default -> throw new IllegalArgumentException();
        };
    }

    public static int glFormatCompressionType(int format) {
        return switch (format) {
            case GLTypes.FORMAT_COMPRESSED_RGB8_ETC2 -> Image.COMPRESSION_ETC2_RGB8_UNORM;
            case GLTypes.FORMAT_COMPRESSED_RGB8_BC1 -> Image.COMPRESSION_BC1_RGB8_UNORM;
            case GLTypes.FORMAT_COMPRESSED_RGBA8_BC1 -> Image.COMPRESSION_BC1_RGBA8_UNORM;
            default -> Image.COMPRESSION_NONE;
        };
    }

    public static int glFormatBytesPerBlock(int format) {
        return switch (format) {
            case GLTypes.FORMAT_RGBA8,
                    GLTypes.FORMAT_DEPTH24_STENCIL8,
                    GLTypes.FORMAT_RG16F,
                    GLTypes.FORMAT_RG16,
                    GLTypes.FORMAT_SRGB8_ALPHA8,
                    GLTypes.FORMAT_RGB10_A2,
                    // We assume the GPU stores this format 4 byte aligned
                    GLTypes.FORMAT_RGB8,
                    GLTypes.FORMAT_BGRA8 -> 4;
            case GLTypes.FORMAT_R8,
                    GLTypes.FORMAT_STENCIL_INDEX8,
                    GLTypes.FORMAT_LUMINANCE8,
                    GLTypes.FORMAT_ALPHA8 -> 1;
            case GLTypes.FORMAT_LUMINANCE8_ALPHA8,
                    GLTypes.FORMAT_STENCIL_INDEX16,
                    GLTypes.FORMAT_R16,
                    GLTypes.FORMAT_RGBA4,
                    GLTypes.FORMAT_RG8,
                    GLTypes.FORMAT_R16F,
                    GLTypes.FORMAT_LUMINANCE16F,
                    GLTypes.FORMAT_RGB565 -> 2;
            case GLTypes.FORMAT_RGBA16F,
                    GLTypes.FORMAT_RGBA16,
                    GLTypes.FORMAT_COMPRESSED_RGBA8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGB8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGB8_ETC2 -> 8;
            case GLTypes.FORMAT_UNKNOWN -> 0;
            default -> throw new IllegalArgumentException();
        };
    }

    public static int glFormatStencilBits(int format) {
        return switch (format) {
            case GLTypes.FORMAT_STENCIL_INDEX8,
                    GLTypes.FORMAT_DEPTH24_STENCIL8 -> 8;
            case GLTypes.FORMAT_STENCIL_INDEX16 -> 16;
            case GLTypes.FORMAT_COMPRESSED_RGB8_ETC2,
                    GLTypes.FORMAT_COMPRESSED_RGB8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGBA8_BC1,
                    GLTypes.FORMAT_RGBA8,
                    GLTypes.FORMAT_R8,
                    GLTypes.FORMAT_ALPHA8,
                    GLTypes.FORMAT_LUMINANCE8,
                    GLTypes.FORMAT_LUMINANCE8_ALPHA8,
                    GLTypes.FORMAT_BGRA8,
                    GLTypes.FORMAT_RGB565,
                    GLTypes.FORMAT_RGBA16F,
                    GLTypes.FORMAT_R16F,
                    GLTypes.FORMAT_LUMINANCE16F,
                    GLTypes.FORMAT_RGB8,
                    GLTypes.FORMAT_RG8,
                    GLTypes.FORMAT_RGB10_A2,
                    GLTypes.FORMAT_RGBA4,
                    GLTypes.FORMAT_SRGB8_ALPHA8,
                    GLTypes.FORMAT_R16,
                    GLTypes.FORMAT_RG16,
                    GLTypes.FORMAT_RGBA16,
                    GLTypes.FORMAT_RG16F,
                    GLTypes.FORMAT_UNKNOWN -> 0;
            default -> throw new IllegalArgumentException();
        };
    }

    public static boolean glFormatIsPackedDepthStencil(int format) {
        return switch (format) {
            case GLTypes.FORMAT_DEPTH24_STENCIL8 -> true;
            case GLTypes.FORMAT_COMPRESSED_RGB8_ETC2,
                    GLTypes.FORMAT_COMPRESSED_RGB8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGBA8_BC1,
                    GLTypes.FORMAT_RGBA8,
                    GLTypes.FORMAT_R8,
                    GLTypes.FORMAT_ALPHA8,
                    GLTypes.FORMAT_LUMINANCE8,
                    GLTypes.FORMAT_LUMINANCE8_ALPHA8,
                    GLTypes.FORMAT_BGRA8,
                    GLTypes.FORMAT_RGB565,
                    GLTypes.FORMAT_RGBA16F,
                    GLTypes.FORMAT_R16F,
                    GLTypes.FORMAT_LUMINANCE16F,
                    GLTypes.FORMAT_RGB8,
                    GLTypes.FORMAT_RG8,
                    GLTypes.FORMAT_RGB10_A2,
                    GLTypes.FORMAT_RGBA4,
                    GLTypes.FORMAT_SRGB8_ALPHA8,
                    GLTypes.FORMAT_R16,
                    GLTypes.FORMAT_RG16,
                    GLTypes.FORMAT_RGBA16,
                    GLTypes.FORMAT_RG16F,
                    GLTypes.FORMAT_STENCIL_INDEX8,
                    GLTypes.FORMAT_STENCIL_INDEX16,
                    GLTypes.FORMAT_UNKNOWN -> false;
            default -> throw new IllegalArgumentException();
        };
    }

    public static boolean glFormatIsSRGB(int format) {
        return switch (format) {
            case GLTypes.FORMAT_SRGB8_ALPHA8 -> true;
            case GLTypes.FORMAT_COMPRESSED_RGB8_ETC2,
                    GLTypes.FORMAT_COMPRESSED_RGB8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGBA8_BC1,
                    GLTypes.FORMAT_RGBA8,
                    GLTypes.FORMAT_R8,
                    GLTypes.FORMAT_ALPHA8,
                    GLTypes.FORMAT_LUMINANCE8,
                    GLTypes.FORMAT_LUMINANCE8_ALPHA8,
                    GLTypes.FORMAT_BGRA8,
                    GLTypes.FORMAT_RGB565,
                    GLTypes.FORMAT_RGBA16F,
                    GLTypes.FORMAT_R16F,
                    GLTypes.FORMAT_LUMINANCE16F,
                    GLTypes.FORMAT_RGB8,
                    GLTypes.FORMAT_RG8,
                    GLTypes.FORMAT_RGB10_A2,
                    GLTypes.FORMAT_RGBA4,
                    GLTypes.FORMAT_R16,
                    GLTypes.FORMAT_RG16,
                    GLTypes.FORMAT_RGBA16,
                    GLTypes.FORMAT_RG16F,
                    GLTypes.FORMAT_STENCIL_INDEX8,
                    GLTypes.FORMAT_STENCIL_INDEX16,
                    GLTypes.FORMAT_DEPTH24_STENCIL8,
                    GLTypes.FORMAT_UNKNOWN -> false;
            default -> throw new IllegalArgumentException();
        };
    }

    public static boolean glFormatIsCompressed(int format) {
        return switch (format) {
            case GLTypes.FORMAT_COMPRESSED_RGB8_ETC2,
                    GLTypes.FORMAT_COMPRESSED_RGB8_BC1,
                    GLTypes.FORMAT_COMPRESSED_RGBA8_BC1 -> true;
            case GLTypes.FORMAT_RGBA8,
                    GLTypes.FORMAT_R8,
                    GLTypes.FORMAT_ALPHA8,
                    GLTypes.FORMAT_LUMINANCE8,
                    GLTypes.FORMAT_LUMINANCE8_ALPHA8,
                    GLTypes.FORMAT_BGRA8,
                    GLTypes.FORMAT_RGB565,
                    GLTypes.FORMAT_RGBA16F,
                    GLTypes.FORMAT_R16F,
                    GLTypes.FORMAT_LUMINANCE16F,
                    GLTypes.FORMAT_RGB8,
                    GLTypes.FORMAT_RG8,
                    GLTypes.FORMAT_RGB10_A2,
                    GLTypes.FORMAT_RGBA4,
                    GLTypes.FORMAT_SRGB8_ALPHA8,
                    GLTypes.FORMAT_R16,
                    GLTypes.FORMAT_RG16,
                    GLTypes.FORMAT_RGBA16,
                    GLTypes.FORMAT_RG16F,
                    GLTypes.FORMAT_STENCIL_INDEX8,
                    GLTypes.FORMAT_STENCIL_INDEX16,
                    GLTypes.FORMAT_DEPTH24_STENCIL8,
                    GLTypes.FORMAT_UNKNOWN -> false;
            default -> throw new IllegalArgumentException();
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

    private GLCore() {
    }
}
