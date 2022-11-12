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
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Wraps a String and acts as the key into cache. The hashCode() and equals() methods
 * consider all ASCII digits to be equal when hashing and comparing Key objects together.
 * Therefore, Strings which only differ in their digits will be all hashed together
 * into the same entry. The draw method will then substitute the correct digit glyph on
 * the fly. This special digit handling gives a significant speedup on the F3 debug screen.
 *
 * @author BloCamLimb
 * @see CharacterStyle
 * @see CompoundLayoutKey
 * @since 2.0
 */
public class VanillaLayoutKey {

    /**
     * A reference of the String which this Key is indexing.
     * This string contains {@link ChatFormatting} codes.
     */
    private String mView;

    /**
     * A reference to the font set which this Key is decorated.
     */
    private ResourceLocation mFont;

    /**
     * A packed field that contains RGB color and appearance-affecting bit flags
     * which this Key is decorated.
     *
     * @see CharacterStyle#flatten(Style)
     */
    private int mCode;

    /**
     * Cached hash code, recalculate when zero.
     */
    private int mHash;

    public VanillaLayoutKey() {
    }

    /**
     * Copy constructor
     */
    private VanillaLayoutKey(@Nonnull VanillaLayoutKey key) {
        mView = key.mView;
        mFont = key.mFont;
        mCode = key.mCode;
        mHash = key.mHash;
    }

    /**
     * Update this key.
     *
     * @param text the string
     */
    public VanillaLayoutKey update(@Nonnull String text, @Nonnull Style style) {
        mView = text;
        mFont = style.getFont();
        mCode = CharacterStyle.flatten(style);
        mHash = 0;
        return this;
    }

    /**
     * Computes a hash code on str in the same manner as the String class,
     * except all ASCII digits hash as '0'
     *
     * @return the augmented hash code on str
     */
    @Override
    public int hashCode() {
        int h = mHash;

        if (h == 0) {
            final String s = mView;

            // true if a section mark character was last seen. In this case, if the next character
            // is a digit, it must not be considered equal to any other digit. This forces any string
            // that differs in formatting codes only to have a separate entry in the cache.
            boolean prefix = false;
            for (int i = 0, e = s.length(); i < e; i++) {
                char c = s.charAt(i);
                // fast digit replacement contract
                if (!prefix && c <= '9' && c >= '0') {
                    c = '0';
                }
                h = 31 * h + c;
                prefix = (c == ChatFormatting.PREFIX_CODE);
            }

            h = 31 * h + mFont.hashCode();
            mHash = h = 31 * h + mCode;
        }

        return h;
    }

    /**
     * Compare str against another object (specifically, the object's string representation as returned by toString).
     * All ASCII digits are considered equal by this method, as long as they are at the same index within the string.
     *
     * @return true if the strings are the identical, or only differ in their ASCII digits
     */
    @Override
    public boolean equals(Object o) {
        if (getClass() != o.getClass()) {
            return false;
        }
        VanillaLayoutKey key = (VanillaLayoutKey) o;

        if (mCode != key.mCode) {
            return false;
        }
        if (!mFont.equals(key.mFont)) {
            return false;
        }

        final String s1 = mView;
        final String s2 = key.mView;

        final int length = s1.length();

        if (length != s2.length()) {
            return false;
        }

        // true if a section mark character was last seen. In this case, if the next character
        // is a digit, it must not be considered equal to any other digit. This forces any string
        // that differs in formatting codes only to have a separate entry in the cache.
        boolean prefix = false;
        for (int i = 0; i < length; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            // fast digit replacement contract
            if (c1 != c2 && (prefix || c1 > '9' || c1 < '0' || c2 > '9' || c2 < '0')) {
                return false;
            }
            prefix = (c1 == ChatFormatting.PREFIX_CODE);
        }

        return true;
    }

    @Override
    public String toString() {
        return "VanillaLayoutKey{" +
                "mView=" + mView +
                ", mFont=" + mFont +
                ", mCode=" + mCode +
                ", mHash=" + mHash +
                '}';
    }

    /**
     * Returns a copy of this key.
     */
    public VanillaLayoutKey copy() {
        return new VanillaLayoutKey(this);
    }
}
