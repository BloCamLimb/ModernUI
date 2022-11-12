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
import java.util.Objects;

/**
 * Fast approach to compare with CharSequences or build CharSequences that match
 * {@link String}'s hashCode() and equals().
 *
 * @author BloCamLimb
 */
public class CharSequenceBuilder extends CharArrayList implements CharSequence {

    public CharSequenceBuilder() {
    }

    public void addChars(@Nonnull char[] buf, int start, int end) {
        Objects.checkFromToIndex(start, end, buf.length);
        int offset = size;
        int length = end - start;
        size(offset + length);
        System.arraycopy(buf, start, a, offset, length);
    }

    /**
     * @param codePoint a code point
     * @return char count
     */
    public int addCodePoint(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) {
            add((char) codePoint);
            return 1;
        } else {
            add(Character.highSurrogate(codePoint));
            add(Character.lowSurrogate(codePoint));
            return 2;
        }
    }

    public void addString(@Nonnull String s) {
        int offset = size;
        int length = s.length();
        size(offset + length);
        s.getChars(0, length, a, offset);
    }

    public void addCharSequence(@Nonnull CharSequence s) {
        int offset = size;
        int length = s.length();
        size(offset + length);
        char[] buf = a;
        for (int i = 0; i < length; i++) {
            buf[offset + i] = s.charAt(i);
        }
    }

    @Nonnull
    public CharSequenceBuilder updateChars(@Nonnull char[] buf, int start, int end) {
        Objects.checkFromToIndex(start, end, buf.length);
        int length = end - start;
        size(length);
        System.arraycopy(buf, start, a, 0, length);
        return this;
    }

    @Override
    public int length() {
        return size;
    }

    @Override
    public char charAt(int index) {
        return getChar(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a new string
     */
    @Nonnull
    @Override
    public String toString() {
        return new String(a, 0, size);
    }

    /**
     * Same as {@link String#hashCode()}.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        char[] buf = a;
        int h = 0, s = size();
        for (int i = 0; i < s; i++)
            h = 31 * h + buf[i];
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CharSequence seq) {
            int s = size();
            if (s != seq.length()) return false;
            char[] buf = a;
            for (int i = 0; i < s; i++)
                if (buf[i] != seq.charAt(i))
                    return false;
            return true;
        }
        return false;
    }
}
