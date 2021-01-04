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

package icyllis.modernui.font;

import icyllis.modernui.graphics.math.TextAlign;

import javax.annotation.Nullable;

@Deprecated
public interface IFontRenderer {

    /**
     * Render a single-line string to the screen using the current OpenGL color. The (x,y) coordinates are of the upper-left
     * corner of the string's bounding box, rather than the baseline position as is typical with fonts. This function will also
     * add the string to the cache so the next drawString() call with the same string is faster.
     *
     * @param str string to draw
     * @param startX start x pos
     * @param startY start y pos
     * @param r red 0-255
     * @param g green 0-255
     * @param b blue 0-255
     * @param a alpha 0-255
     * @param align 0-left 0.25-center 0.5-right
     * @return formatted text width
     */
    float drawString(@Nullable String str, float startX, float startY, int r, int g, int b, int a, TextAlign align);

    /**
     * Return the width of a string in pixels. Used for centering strings.
     *
     * @param str string with formatting codes
     * @return the width of the text that should render on screen
     */
    float getStringWidth(String str);

    /**
     * Return the number of characters in a string that will completely fit inside the specified width when rendered.
     *
     * @param str   the String to analyze
     * @param width the desired string width (in GUI coordinate system)
     * @return the number of characters from str that will fit inside width
     */
    int sizeStringToWidth(String str, float width);

    /**
     * Trim a string so that it fits in the specified width when rendered, optionally reversing the string
     *
     * @param str     the String to trim
     * @param width   the desired string width (in GUI coordinate system)
     * @param reverse if true, the returned string will also be reversed
     * @return the trimmed and optionally reversed string
     */
    String trimStringToWidth(String str, float width, boolean reverse);
}
