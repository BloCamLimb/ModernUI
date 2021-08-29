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
 * @see FontAtlas
 * @since 2.0
 */
public class TexturedGlyph {

    /**
     * The OpenGL texture ID that contains this glyph image.
     */
    public int texture;

    /**
     * The horizontal offset to baseline.
     */
    public int offsetX;

    /**
     * The vertical offset to baseline.
     */
    public int offsetY;

    /**
     * The total width of this glyph image in pixels.
     */
    public int width;

    /**
     * The total height of this glyph image in pixels.
     */
    public int height;

    /**
     * The horizontal texture coordinate of the upper-left corner.
     */
    public float u1;

    /**
     * The vertical texture coordinate of the upper-left corner.
     */
    public float v1;

    /**
     * The horizontal texture coordinate of the lower-right corner.
     */
    public float u2;

    /**
     * The vertical texture coordinate of the lower-right corner.
     */
    public float v2;

    public TexturedGlyph() {
    }

    @Override
    public String toString() {
        return "TexturedGlyph{" + "texture=" + texture +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                ", width=" + width +
                ", height=" + height +
                ", u1=" + u1 +
                ", v1=" + v1 +
                ", u2=" + u2 +
                ", v2=" + v2 +
                '}';
    }
}
