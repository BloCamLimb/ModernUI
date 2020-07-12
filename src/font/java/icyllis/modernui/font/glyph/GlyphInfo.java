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

package icyllis.modernui.font.glyph;

public class GlyphInfo implements Comparable<GlyphInfo> {

    /**
     * The index into the original string (i.e. with color codes) for the character that generated this glyph.
     */
    int stringIndex;

    /**
     * Texture ID and position/size of the glyph's pre-rendered image within the cache texture.
     */
    TexturedGlyph texture;

    /**
     * Glyph's horizontal position (in pixels) relative to the entire string's baseline
     */
    int x;

    /**
     * Glyph's vertical position (in pixels) relative to the entire string's baseline
     */
    int y;

    /**
     * Glyph's horizontal advance (in pixels) used for strikethrough and underline effects
     */
    public float advance;

    /**
     * Allows arrays of Glyph objects to be sorted. Performs numeric comparison on stringIndex.
     *
     * @param o the other Glyph object being compared with this one
     * @return either -1, 0, or 1 if this < other, this == other, or this > other
     */
    @Override
    public int compareTo(GlyphInfo o) {
        return Integer.compare(stringIndex, o.stringIndex);
    }
}
