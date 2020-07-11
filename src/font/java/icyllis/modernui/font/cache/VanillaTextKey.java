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

import net.minecraft.util.text.Style;

/**
 * Wraps a String and acts as the key into cache. The hashCode() and equals() methods consider all ASCII digits
 * to be equal when hashing and comparing Key objects together. Therefore, Strings which only differ in their digits will
 * be all hashed together into the same entry. The draw method will then substitute the correct digit glyph on
 * the fly. This special digit handling gives a significant speedup on the F3 debug screen.
 * <p>
 * For vanilla only, because computing hash is a little heavy now.
 *
 * @see net.minecraft.util.text.Style
 * @see net.minecraft.util.text.TextFormatting
 * @since 2.0
 */
//TODO render style hash only
public class VanillaTextKey {

    /**
     * a copy of the String which this Key is indexing. A copy is used to avoid creating a strong reference to the original
     * passed into draw method. When the original String is no longer needed by Minecraft, it will be garbage collected
     * and the WeakHashMaps in StringCache will allow this Key object and its associated Entry object to be garbage
     * collected as well.
     */
    public String str;

    public Style style;

    public VanillaTextKey() {

    }

    /**
     * Compare str against another object (specifically, the object's string representation as returned by toString).
     * All ASCII digits are considered equal by this method, as long as they are at the same index within the string.
     *
     * @return true if the strings are the identical, or only differ in their ASCII digits
     */
    @Override
    public boolean equals(Object o) {

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final String str = this.str;
        /* Calling toString on a String object simply returns itself so no new object allocation is performed */
        final String other = o.toString();
        final int length = str.length();

        if (length != other.length()) {
            return false;
        }

        /*
         * true if a section mark character was last seen. In this case, if the next character is a digit, it must
         * not be considered equal to any other digit. This forces any string that differs in formatting codes only to
         * have a separate entry in the cache.
         */
        boolean colorCode = false;

        for (int index = 0; index < length; index++) {
            char c1 = str.charAt(index);
            char c2 = other.charAt(index);

            if (c1 != c2 && (c1 < '0' || c1 > '9' || c2 < '0' || c2 > '9' || colorCode)) {
                return false;
            }
            colorCode = (c1 == '\u00a7');
        }

        return style.equals(((VanillaTextKey) o).style);
    }

    /**
     * Computes a hash code on str in the same manner as the String class, except all ASCII digits hash as '0'
     *
     * @return the augmented hash code on str
     */
    @Override
    public int hashCode() {
        int code = 0;
        final int length = str.length();

        /*
         * true if a section mark character was last seen. In this case, if the next character is a digit, it must
         * not be considered equal to any other digit. This forces any string that differs in formatting codes only to
         * have a separate entry in the cache.
         */
        boolean formattingCode = false;

        for (int index = 0; index < length; index++) {
            char c = str.charAt(index);
            if (c >= '0' && c <= '9' && !formattingCode) {
                c = '0';
            }
            code = (code * 31) + c;
            formattingCode = (c == '\u00a7');
        }

        return code * style.hashCode();
    }

    /**
     * Returns the contained String object within this Key.
     *
     * @return the str object
     */
    @Override
    public String toString() {
        return str;
    }
}
