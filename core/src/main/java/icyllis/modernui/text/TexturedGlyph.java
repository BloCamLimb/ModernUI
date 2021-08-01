/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text;

/**
 * This class holds information for a glyph about its pre-rendered image in an
 * OpenGL texture. The glyph must be laid-out so that it has something to render
 * in a context.
 *
 * @see GlyphManager
 * @since 2.0
 */
public class TexturedGlyph {

    /**
     * The OpenGL texture ID that contains this glyph image.
     */
    int texture;

    /**
     * The horizontal offset to baseline.
     */
    int offsetX;

    /**
     * The vertical offset to baseline.
     */
    int offsetY;

    /**
     * The total width of this glyph image in pixels.
     */
    int width;

    /**
     * The total height of this glyph image in pixels.
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
