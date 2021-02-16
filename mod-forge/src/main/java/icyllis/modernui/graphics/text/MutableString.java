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

package icyllis.modernui.graphics.text;

import it.unimi.dsi.fastutil.chars.CharArrayList;

import javax.annotation.Nonnull;

/**
 * Used for fast lookup
 */
public class MutableString implements CharSequence {

    public final CharArrayList chars = new CharArrayList();

    public void addString(@Nonnull String str) {
        chars.size(chars.size() + str.length());
        str.getChars(0, str.length(), chars.elements(), chars.size());
    }

    public void addChar(char c) {
        chars.add(c);
    }

    public void addCodePoint(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) {
            chars.add((char) codePoint);
        } else {
            chars.add(Character.highSurrogate(codePoint));
            chars.add(Character.lowSurrogate(codePoint));
        }
    }

    public boolean isEmpty() {
        return chars.isEmpty();
    }

    public void clear() {
        chars.clear();
    }

    @Override
    public int length() {
        return chars.size();
    }

    @Override
    public char charAt(int index) {
        return chars.getChar(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new String(chars.elements(), start, end - start);
    }

    /**
     * This method won't be called when querying/lookup
     * But new a String when caching a text node as a reference
     *
     * @return reference str key
     */
    @Nonnull
    @Override
    public String toString() {
        return new String(chars.toCharArray());
    }
}
