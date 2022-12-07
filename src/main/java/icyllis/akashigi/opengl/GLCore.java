/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.*;
import icyllis.akashigi.engine.ShaderErrorHandler;
import icyllis.akashigi.engine.ThreadSafePipelineBuilder;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.NativeType;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;

/**
 * Provides native interfaces of OpenGL 4.5 core and user-defined utilities.
 */
public final class GLCore extends GL45C {

    public static void glClearErrors() {
        //noinspection StatementWithEmptyBody
        while (glGetError() != GL_NO_ERROR)
            ;
    }

    /**
     * @see #glFormatToIndex(int)
     */
    public static final int LAST_COLOR_FORMAT_INDEX = 17;
    public static final int LAST_FORMAT_INDEX = 20;

    /**
     * Lists all supported OpenGL texture formats and converts to table index.
     * 0 is reserved for unsupported formats.
     *
     * @see #glIndexToFormat(int)
     */
    public static int glFormatToIndex(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8 -> 1;
            case GL_R8 -> 2;
            case GL_RGB565 -> 3;
            case GL_RGBA16F -> 4;
            case GL_R16F -> 5;
            case GL_RGB8 -> 6;
            case GL_RG8 -> 7;
            case GL_RGB10_A2 -> 8;
            case GL_RGBA4 -> 9;
            case GL_SRGB8_ALPHA8 -> 10;
            case GL_COMPRESSED_RGB8_ETC2 -> 11;
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> 12;
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> 13;
            case GL_R16 -> 14;
            case GL_RG16 -> 15;
            case GL_RGBA16 -> 16;
            case GL_RG16F -> 17;            // LAST_COLOR_FORMAT_INDEX
            case GL_STENCIL_INDEX8 -> 18;
            case GL_STENCIL_INDEX16 -> 19;
            case GL_DEPTH24_STENCIL8 -> 20; // LAST_FORMAT_INDEX
            default -> 0;
        };
    }

    /**
     * Reverse of {@link #glFormatToIndex(int)}.
     */
    @NativeType("GLenum")
    public static int glIndexToFormat(int index) {
        return switch (index) {
            case 1 -> GL_RGBA8;
            case 2 -> GL_R8;
            case 3 -> GL_RGB565;
            case 4 -> GL_RGBA16F;
            case 5 -> GL_R16F;
            case 6 -> GL_RGB8;
            case 7 -> GL_RG8;
            case 8 -> GL_RGB10_A2;
            case 9 -> GL_RGBA4;
            case 10 -> GL_SRGB8_ALPHA8;
            case 11 -> GL_COMPRESSED_RGB8_ETC2;
            case 12 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case 13 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case 14 -> GL_R16;
            case 15 -> GL_RG16;
            case 16 -> GL_RGBA16;
            case 17 -> GL_RG16F;
            case 18 -> GL_STENCIL_INDEX8;
            case 19 -> GL_STENCIL_INDEX16;
            case 20 -> GL_DEPTH24_STENCIL8;
            case 0 -> 0;
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * @see Color#COLOR_CHANNEL_FLAGS_RGBA
     */
    public static int glFormatChannels(int format) {
        return switch (format) {
            case GL_RGBA8,
                    GL_RGBA16,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                    GL_SRGB8_ALPHA8,
                    GL_RGBA4,
                    GL_RGB10_A2,
                    GL_RGBA16F -> Color.COLOR_CHANNEL_FLAGS_RGBA;
            case GL_R8,
                    GL_R16,
                    GL_R16F -> Color.COLOR_CHANNEL_FLAG_RED;
            case GL_RGB565,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGB8_ETC2,
                    GL_RGB8 -> Color.COLOR_CHANNEL_FLAGS_RGB;
            case GL_RG8,
                    GL_RG16F,
                    GL_RG16 -> Color.COLOR_CHANNEL_FLAGS_RG;
            default -> 0;
        };
    }

    /**
     * Consistent with {@link #glFormatToIndex(int)}
     */
    public static boolean glFormatIsSupported(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8,
                    GL_R8,
                    GL_RGB565,
                    GL_RGBA16F,
                    GL_R16F,
                    GL_RGB8,
                    GL_RG8,
                    GL_RGB10_A2,
                    GL_RGBA4,
                    GL_SRGB8_ALPHA8,
                    GL_COMPRESSED_RGB8_ETC2,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                    GL_R16,
                    GL_RG16,
                    GL_RGBA16,
                    GL_RG16F,
                    GL_STENCIL_INDEX8,
                    GL_STENCIL_INDEX16,
                    GL_DEPTH24_STENCIL8 -> true;
            default -> false;
        };
    }

    /**
     * @see Core.CompressionType#kNone
     */
    public static int glFormatCompressionType(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_COMPRESSED_RGB8_ETC2 -> Core.CompressionType.kETC2_RGB8_UNORM;
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> Core.CompressionType.kBC1_RGB8_UNORM;
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> Core.CompressionType.kBC1_RGBA8_UNORM;
            default -> Core.CompressionType.kNone;
        };
    }

    public static int glFormatBytesPerBlock(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8,
                    GL_DEPTH24_STENCIL8,
                    GL_RG16F,
                    GL_RG16,
                    GL_SRGB8_ALPHA8,
                    GL_RGB10_A2,
                    // We assume the GPU stores this format 4 byte aligned
                    GL_RGB8 -> 4;
            case GL_R8,
                    GL_STENCIL_INDEX8 -> 1;
            case GL_STENCIL_INDEX16,
                    GL_R16,
                    GL_RGBA4,
                    GL_RG8,
                    GL_R16F,
                    GL_RGB565 -> 2;
            case GL_RGBA16F,
                    GL_RGBA16,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGB8_ETC2 -> 8;
            default -> 0;
        };
    }

    public static int glFormatStencilBits(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_STENCIL_INDEX8,
                    GL_DEPTH24_STENCIL8 -> 8;
            case GL_STENCIL_INDEX16 -> 16;
            default -> 0;
        };
    }

    public static boolean glFormatIsPackedDepthStencil(@NativeType("GLenum") int format) {
        return format == GL_DEPTH24_STENCIL8;
    }

    public static boolean glFormatIsSRGB(@NativeType("GLenum") int format) {
        return format == GL_SRGB8_ALPHA8;
    }

    public static boolean glFormatIsCompressed(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_COMPRESSED_RGB8_ETC2,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> true;
            default -> false;
        };
    }

    public static String glFormatName(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8 -> "RGBA8";
            case GL_R8 -> "R8";
            case GL_RGB565 -> "RGB565";
            case GL_RGBA16F -> "RGBA16F";
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

    public static int glCompileAndAttachShader(int program,
                                               int type,
                                               String source,
                                               ThreadSafePipelineBuilder.Stats stats,
                                               ShaderErrorHandler errorHandler) {
        // Specify GLSL source to the driver.
        int shader = glCreateShader(type);
        if (shader == 0) {
            return 0;
        }
        glShaderSource(shader, source);

        glCompileShader(shader);
        stats.incShaderCompilations();

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader).trim();
            glDeleteShader(shader);
            errorHandler.handleCompileError(source, log);
            return 0;
        }

        // Attach the shader, but defer deletion until after we have linked the program.
        glAttachShader(program, shader);
        return shader;
    }

    private GLCore() {
    }
}
