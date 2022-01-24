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

import it.unimi.dsi.fastutil.chars.CharArrayList;

import javax.annotation.Nonnull;

/**
 * Used for fast lookup.
 */
class CharSequenceBuilder implements CharSequence {

    public final CharArrayList mChars = new CharArrayList();

    public void addChar(char c) {
        mChars.add(c);
    }

    public void addCodePoint(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) {
            mChars.add((char) codePoint);
        } else {
            mChars.add(Character.highSurrogate(codePoint));
            mChars.add(Character.lowSurrogate(codePoint));
        }
    }

    public void addString(@Nonnull String str) {
        int offset = mChars.size();
        mChars.size(mChars.size() + str.length());
        str.getChars(0, str.length(), mChars.elements(), offset);
    }

    public CharArrayList trimChars() {
        mChars.trim();
        return mChars;
    }

    public void clear() {
        mChars.clear();
    }

    @Override
    public int length() {
        return mChars.size();
    }

    @Override
    public char charAt(int index) {
        return mChars.getChar(index);
    }

    public boolean isEmpty() {
        return mChars.isEmpty();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
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
        return new String(mChars.toCharArray());
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() != CharArrayList.class) return false;

        CharArrayList that = (CharArrayList) o;
        return mChars.equals(that);
    }

    @Override
    public int hashCode() {
        return mChars.hashCode();
    }
}
