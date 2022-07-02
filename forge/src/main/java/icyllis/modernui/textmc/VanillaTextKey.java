/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;

/**
 * Wraps a String and acts as the key into cache. The hashCode() and equals() methods consider all ASCII digits
 * to be equal when hashing and comparing Key objects together. Therefore, Strings which only differ in their digits
 * will be all hashed together into the same entry. The draw method will then substitute the correct digit glyph on
 * the fly. This special digit handling gives a significant speedup on the F3 debug screen.
 *
 * @see net.minecraft.ChatFormatting
 * @see CompositeTextKey
 * @since 2.0
 */
public class VanillaTextKey {

    /**
     * A reference of the String which this Key is indexing.
     * This string contains TextFormatting codes.
     */
    private String mStr;

    /**
     * Reference to vanilla's {@link Style}, we extract the value that will only affect the rendering effect
     * of the string, and store it as an integer
     */
    private int mStyle;

    /**
     * Precomputed hash code, default is 0. May hash collision.
     */
    private int mHash;

    public VanillaTextKey() {
    }

    /**
     * Copy constructor
     */
    private VanillaTextKey(@Nonnull VanillaTextKey key) {
        mStr = key.mStr;
        mStyle = key.mStyle;
        mHash = key.mHash;
    }

    /**
     * Update current str.
     *
     * @param str the string
     */
    public VanillaTextKey update(@Nonnull String str) {
        mStr = str;
        mStyle = 0;
        mHash = 0;
        return this;
    }

    /**
     * Update current str.
     *
     * @param str the string
     */
    public VanillaTextKey update(@Nonnull String str, @Nonnull Style style) {
        mStr = str;
        mStyle = CharacterStyle.getFlags(style);
        mHash = 0;
        return this;
    }

    /**
     * Compare str against another object (specifically, the object's string representation as returned by toString).
     * All ASCII digits are considered equal by this method, as long as they are at the same index within the string.
     *
     * @return true if the strings are the identical, or only differ in their ASCII digits
     */
    @Override
    public boolean equals(Object o) {
        // we never compare ourselves, so no identity check
        if (getClass() != o.getClass()) {
            return false;
        }

        // check lightweight value first
        if (mStyle != ((VanillaTextKey) o).mStyle) {
            return false;
        }

        final String s = mStr;
        /* Calling toString on a String object simply returns itself so no new object allocation is performed */
        final String other = o.toString();

        if (s.length() != other.length()) {
            return false;
        }

        /*
         * true if a section mark character was last seen. In this case, if the next character is a digit, it must
         * not be considered equal to any other digit. This forces any string that differs in formatting codes only to
         * have a separate entry in the cache.
         */
        boolean formatting = false;

        char c1;
        char c2;

        for (int i = 0, e = s.length(); i < e; i++) {
            c1 = s.charAt(i);
            c2 = other.charAt(i);
            // fast digit replacement
            if (c1 != c2 && (formatting || c1 > '9' || c1 < '0' || c2 > '9' || c2 < '0')) {
                return false;
            }
            formatting = (c1 == ChatFormatting.PREFIX_CODE);
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
        int h = mHash;

        if (h == 0) {
            final String s = mStr;

            /*
             * true if a section mark character was last seen. In this case, if the next character is a digit, it must
             * not be considered equal to any other digit. This forces any string that differs in formatting codes
             * only to
             * have a separate entry in the cache.
             */
            boolean formatting = false;

            char c;

            for (int i = 0, e = s.length(); i < e; i++) {
                c = s.charAt(i);
                // fast digit replacement
                if (!formatting && c <= '9' && c >= '0') {
                    c = '0';
                }
                h = 31 * h + c;
                formatting = (c == ChatFormatting.PREFIX_CODE);
            }

            mHash = h = 31 * h + mStyle;
        }

        return h;
    }

    /**
     * Returns the contained String object within this Key.
     *
     * @return the str object
     */
    @Override
    public String toString() {
        return mStr;
    }

    /**
     * Returns a copy of this key
     *
     * @return copied key
     */
    public VanillaTextKey copy() {
        return new VanillaTextKey(this);
    }
}
