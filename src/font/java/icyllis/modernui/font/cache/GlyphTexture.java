/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.font.cache;

import net.minecraft.client.renderer.RenderType;

/**
 * This class holds information for a glyph about its pre-rendered image in an OpenGL texture. The texture coordinates in
 * this class are normalized in the standard 0.0 - 1.0 OpenGL range.
 */
public class GlyphTexture {

    /**
     * The OpenGL texture ID that contains this glyph image.
     */
    public int textureName;

    /**
     * Cached render type for render type buffer system.
     */
    public RenderType renderType;

    /**
     * The width in pixels of the glyph image.
     */
    public int width;

    /**
     * The height in pixels of the glyph image.
     */
    int height;

    /**
     * The horizontal texture coordinate of the upper-left corner.
     */
    float u1;

    /**
     * The vertical texture coordinate of the upper-left corner.
     */
    float v1;

    /**
     * The horizontal texture coordinate of the lower-right corner.
     */
    float u2;

    /**
     * The vertical texture coordinate of the lower-right corner.
     */
    float v2;
}
