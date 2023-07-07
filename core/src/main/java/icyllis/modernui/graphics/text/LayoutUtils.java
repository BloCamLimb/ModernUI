/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

public class LayoutUtils {

    public static boolean isStretchableSpace(char c) {
        return c == ' ';
    }

    /**
     * For the purpose of layout, a word break is a boundary with no
     * kerning or complex script processing. This is necessarily a
     * heuristic, but should be accurate most of the time.
     */
    public static boolean isWordBreakAfter(char c) {
        if (c == ' ' || (0x2000 <= c && c <= 0x200A) || c == 0x3000) {
            // spaces
            return true;
        }
        // Break layout context before and after BiDi control character.
        if ((0x2066 <= c && c <= 0x2069) || (0x202A <= c && c <= 0x202E) || c == 0x200E ||
                c == 0x200F) {
            return true;
        }
        // Note: kana is not included, as sophisticated fonts may kern kana
        return false;
    }

    public static boolean isWordBreakBefore(char c) {
        // CJK ideographs (and yijing hexagram symbols)
        return isWordBreakAfter(c) || (0x3400 <= c && c <= 0x9FFF);
    }

    /**
     * Return offset of previous word break. It is either < offset or == start.
     */
    public static int getPrevWordBreakForCache(char[] buf, int start, int end, int offset) {
        if (offset <= start) return start;
        if (offset > end) offset = end;
        if (isWordBreakBefore(buf[offset - 1])) {
            return offset - 1;
        }
        for (int i = offset - 1; i > start; i--) {
            if (isWordBreakBefore(buf[i]) || isWordBreakAfter(buf[i - 1])) {
                return i;
            }
        }
        return start;
    }

    /**
     * Return offset of next word break. It is either > offset or == end.
     */
    public static int getNextWordBreakForCache(char[] buf, int start, int end, int offset) {
        if (offset >= end) return end;
        if (offset < start) offset = start;
        if (isWordBreakAfter(buf[offset])) {
            return offset + 1;
        }
        for (int i = offset + 1; i < end; i++) {
            // No need to check isWordBreakAfter(chars[i - 1]) since it is checked
            // in previous iteration.  Note that isWordBreakBefore returns true
            // whenever isWordBreakAfter returns true.
            if (isWordBreakBefore(buf[i])) {
                return i;
            }
        }
        return end;
    }
}
