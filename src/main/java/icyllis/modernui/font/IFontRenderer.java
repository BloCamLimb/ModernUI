/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.font;

public interface IFontRenderer {

    /**
     * Draw string
     *
     * @param str formatted text
     * @param startX x pos
     * @param startY y pos
     * @param r red 0-1
     * @param g green 0-1
     * @param b blue 0-1
     * @param a alpha 0-1
     * @param align 0-left 0.25-center 0.5-right
     * @return formatted text width
     */
    float drawString(String str, float startX, float startY, float r, float g, float b, float a, float align);

    /**
     * Get string width
     *
     * @param str unformatted text
     * @return formatted text width
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
