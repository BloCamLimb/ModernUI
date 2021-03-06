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

package icyllis.modernui.font.pipeline;

@Deprecated
public class ColorState {

    /**
     * In case no text formatting or specific style
     */
    public static final ColorState[] NO_COLOR_STATE = new ColorState[]{new ColorState(-1, -1)};

    /**
     * The index of glyphs rendered in the current string of where this color would appeared.
     * Formatting codes render nothing, in SMP two chars in the string represent a glyph.
     */
    public final int glyphIndex;

    /**
     * The color in 0xRRGGBB format
     */
    public final int color;

    public ColorState(int glyphIndex, int color) {
        this.glyphIndex = glyphIndex;
        this.color = color;
    }
}
