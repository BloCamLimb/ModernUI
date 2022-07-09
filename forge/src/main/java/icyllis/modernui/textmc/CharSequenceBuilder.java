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
 * Used for comparing with char sequences, or build char sequences (string) or char arrays.
 *
 * @author BloCamLimb
 */
public class CharSequenceBuilder implements CharSequence {

    public final CharArrayList mChars = new CharArrayList();

    public CharSequenceBuilder() {
    }

    public void addChar(char c) {
        mChars.add(c);
    }

    /**
     * @param codePoint unicode code point
     * @return char count
     */
    public int addCodePoint(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) {
            mChars.add((char) codePoint);
            return 1;
        } else {
            mChars.add(Character.highSurrogate(codePoint));
            mChars.add(Character.lowSurrogate(codePoint));
            return 2;
        }
    }

    public void addString(@Nonnull String str) {
        int offset = mChars.size();
        mChars.size(offset + str.length());
        str.getChars(0, str.length(), mChars.elements(), offset);
    }

    public void addCharSequence(@Nonnull CharSequence seq) {
        int offset = mChars.size();
        int length = seq.length();
        mChars.size(offset + length);
        char[] buf = mChars.elements();
        for (int i = 0; i < length; i++) {
            buf[offset + i] = seq.charAt(i);
        }
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
     * @return new string
     */
    @Nonnull
    @Override
    public String toString() {
        return new String(mChars.elements(), 0, mChars.size());
    }

    /**
     * @return new char array
     */
    @Nonnull
    public char[] toCharArray() {
        return mChars.toCharArray();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CharSequence seq) {
            int s = mChars.size();
            if (s != seq.length()) return false;
            char[] buf = mChars.elements();
            for (int i = 0; i < s; i++)
                if (buf[i] != seq.charAt(i))
                    return false;
            return true;
        }
        return false;
    }

    /**
     * Same as {@link String#hashCode()}.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        char[] buf = mChars.elements();
        int h = 0, s = mChars.size();
        for (int i = 0; i < s; i++)
            h = 31 * h + buf[i];
        return h;
    }
}
