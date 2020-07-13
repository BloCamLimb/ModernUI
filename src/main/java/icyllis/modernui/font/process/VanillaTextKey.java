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

package icyllis.modernui.font.process;

import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps a String and acts as the key into cache. The hashCode() and equals() methods consider all ASCII digits
 * to be equal when hashing and comparing Key objects together. Therefore, Strings which only differ in their digits will
 * be all hashed together into the same entry. The draw method will then substitute the correct digit glyph on
 * the fly. This special digit handling gives a significant speedup on the F3 debug screen.
 * <p>
 * For vanilla only, because Modern UI can generate render node directly.
 *
 * @see net.minecraft.util.text.Style
 * @see net.minecraft.util.text.TextFormatting
 * @since 2.0
 */
public class VanillaTextKey {

    /**
     * A copy of the String which this Key is indexing. A copy is used to avoid creating a strong reference to the original
     * passed into draw method. When the original String is no longer needed by Minecraft, it will be garbage collected
     * and the WeakHashMaps in StringCache will allow this Key object and its associated Entry object to be garbage
     * collected as well. This string contains TextFormatting codes.
     */
    private String str;

    /**
     * Reference to vanilla's {@link Style}, we extract the value that will only affect the rendering effect
     * of the string, and store it as an integer
     *
     * @see #parseStyle(Style)
     */
    private int style;

    public VanillaTextKey() {

    }

    private VanillaTextKey(String str, int style) {
        this.str = str;
        this.style = style;
    }

    /**
     * Update current str and style value, parse vanilla's style to an integer
     *
     * @param str   str
     * @param style style
     */
    public void updateKey(String str, @Nullable Style style) {
        this.str = str;
        // text formatting may render same as style, but we can't parse them easily
        this.style = style == null ? -1 : parseStyle(style);
    }

    public static int parseStyle(@Nonnull Style style) {
        int r = 0;
        if (style.getColor() != null) {
            // RGB - 24 bit
            r |= style.getColor().func_240742_a_();
        }
        if (style.getBold()) {
            r |= 1 << 24;
        }
        if (style.getItalic()) {
            r |= 1 << 25;
        }
        if (style.getStrikethrough()) {
            r |= 1 << 26;
        }
        if (style.getUnderlined()) {
            r |= 1 << 27;
        }
        if (style.getObfuscated()) {
            r |= 1 << 28;
        }
        return r;
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

        /* First check if styles are equal */
        if (style != (((VanillaTextKey) o).style)) {
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
        boolean formatting = false;

        for (int index = 0; index < length; index++) {
            char c1 = str.charAt(index);
            char c2 = other.charAt(index);

            if (c1 != c2 && (c1 < '0' || c1 > '9' || c2 < '0' || c2 > '9' || formatting)) {
                return false;
            }
            formatting = (c1 == '\u00a7');
        }

        return true;
    }

    /**
     * Computes a hash code on str in the same manner as the String class, except all ASCII digits hash as '0'
     *
     * @return the augmented hash code on str
     */
    @Override
    public int hashCode() {
        int code = 0;
        final String str = this.str;
        final int length = str.length();

        /*
         * true if a section mark character was last seen. In this case, if the next character is a digit, it must
         * not be considered equal to any other digit. This forces any string that differs in formatting codes only to
         * have a separate entry in the cache.
         */
        boolean formatting = false;

        for (int index = 0; index < length; index++) {
            char c = str.charAt(index);
            if (c >= '0' && c <= '9' && !formatting) {
                c = '0';
            }
            code = code * 31 + c;
            formatting = (c == '\u00a7');
        }

        return code * 31 + style;
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

    /**
     * Returns a copy of this key
     *
     * @return copied key
     */
    public VanillaTextKey copy() {
        return new VanillaTextKey(str, style);
    }
}
