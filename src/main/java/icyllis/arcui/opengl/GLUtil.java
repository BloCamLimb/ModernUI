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

import icyllis.arcui.core.Color;
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
    public static int getGLFormatChannels(int format) {
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
    public static int getGLFormatFromGLEnum(@NativeType("GLenum") int glFormat) {
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

    private GLUtil() {
    }
}
