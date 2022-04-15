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

package icyllis.arcui.graphics;

/**
 * Constants independent of graphics API.
 */
public final class Types {

    /**
     * Possible 3D APIs that may be used by Arc UI.
     */
    public static final int
            OPENGL = 0, // OpenGL 4.5 core profile
            VULKAN = 1; // Vulkan 1.1

    /**
     * The type of texture. Backends other than GL currently only use the 2D value but the type must
     * still be known at the API-neutral layer as it used to determine whether MIP maps, render-ability,
     * and sampling parameters are legal for proxies that will be instantiated with wrapped textures.
     */
    public static final int
            TEXTURE_TYPE_NONE = 0,
            TEXTURE_TYPE_2D = 1,
            TEXTURE_TYPE_RECTANGLE = 2; // Rectangle uses un-normalized texture coordinates.

    public static final int
            SHADER_TYPE_VERTEX = 0,
            SHADER_TYPE_GEOMETRY = 1,
            SHADER_TYPE_FRAGMENT = 2;

    public static final int
            SHADER_FLAG_VERTEX = 1,
            SHADER_FLAG_TESS_CONTROL = 1 << 1,
            SHADER_FLAG_TESS_EVALUATION = 1 << 2,
            SHADER_FLAG_GEOMETRY = 1 << 3,
            SHADER_FLAG_FRAGMENT = 1 << 4;
}
