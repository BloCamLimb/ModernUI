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

/**
 * Constants of OpenGL pipeline.
 */
public final class GLTypes {

    /**
     * The supported GL formats represented as an enum.
     * <p>
     * Single Alpha and Gray channel are deprecated in Modern OpenGL core profile,
     * they should be replaced by single Red channel. But it may be still supported
     * by some extensions, using builtin swizzle.
     */
    public static final int
            FORMAT_UNKNOWN = 0,
            FORMAT_RGBA8 = 1,
            FORMAT_R8 = 2,
            FORMAT_ALPHA8 = 3,
            FORMAT_LUMINANCE8 = 4,
            FORMAT_LUMINANCE8_ALPHA8 = 5,
            FORMAT_BGRA8 = 6,
            FORMAT_RGB565 = 7,
            FORMAT_RGBA16F = 8,
            FORMAT_R16F = 9,
            FORMAT_RGB8 = 10,
            FORMAT_RG8 = 11,
            FORMAT_RGB10_A2 = 12,
            FORMAT_RGBA4 = 13,
            FORMAT_SRGB8_ALPHA8 = 14,
            FORMAT_COMPRESSED_RGB8_ETC2 = 15,
            FORMAT_COMPRESSED_RGB8_BC1 = 16,
            FORMAT_COMPRESSED_RGBA8_BC1 = 17,
            FORMAT_R16 = 18,
            FORMAT_RG16 = 19,
            FORMAT_RGBA16 = 20,
            FORMAT_RG16F = 21,
            FORMAT_LUMINANCE16F = 22,
            FORMAT_STENCIL_INDEX8 = 23,
            FORMAT_STENCIL_INDEX16 = 24,
            FORMAT_DEPTH24_STENCIL8 = 25;

    private GLTypes() {
    }
}
