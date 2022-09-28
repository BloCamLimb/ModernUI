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

/**
 * User-defined constants of OpenGL.
 */
public final class GLTypes {

    /**
     * The supported GL formats represented as an enum.
     * <p>
     * Alpha and gray formats are deprecated in OpenGL core profile,
     * they should be replaced by R or RG. But they are still available
     * in external formats (CPU side).
     */
    public static final int
            FORMAT_UNKNOWN = 0,
            FORMAT_RGBA8 = 1,
            FORMAT_R8 = 2,
            FORMAT_RGB565 = 3,
            FORMAT_RGBA16F = 4,
            FORMAT_R16F = 5,
            FORMAT_RGB8 = 6,
            FORMAT_RG8 = 7,
            FORMAT_RGB10_A2 = 8,
            FORMAT_RGBA4 = 9,
            FORMAT_SRGB8_ALPHA8 = 10,
            FORMAT_COMPRESSED_RGB8_ETC2 = 11,
            FORMAT_COMPRESSED_RGB8_BC1 = 12,
            FORMAT_COMPRESSED_RGBA8_BC1 = 13,
            FORMAT_R16 = 14,
            FORMAT_RG16 = 15,
            FORMAT_RGBA16 = 16,
            FORMAT_RG16F = 17,
            FORMAT_STENCIL_INDEX8 = 18,
            FORMAT_STENCIL_INDEX16 = 19,
            FORMAT_DEPTH24_STENCIL8 = 20;
    public static final int LAST_COLOR_FORMAT = FORMAT_RG16F;
    public static final int LAST_FORMAT = FORMAT_DEPTH24_STENCIL8;

    private GLTypes() {
    }
}
