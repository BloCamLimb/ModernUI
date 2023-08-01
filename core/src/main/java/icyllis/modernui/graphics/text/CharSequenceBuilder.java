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

import icyllis.modernui.annotation.NonNull;
import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.Objects;

/**
 * Fast approach to compare with CharSequences or build CharSequences that match
 * {@link String}'s hashCode() and equals().
 */
public class CharSequenceBuilder extends CharArrayList implements CharSequence {

    static {
        int[] codePoints = {0x1f469, 0x1f3fc, 0x200d, 0x2764, 0xfe0f,
                0x200d, 0x1f48b, 0x200d, 0x1f469, 0x1f3fd};
        var builder = new CharSequenceBuilder();
        for (int cp : codePoints) {
            builder.addCodePoint(cp);
        }
        String string = new String(codePoints, 0, codePoints.length);
        if (builder.hashCode() != string.hashCode() ||
                builder.hashCode() != builder.toString().hashCode()) {
            throw new RuntimeException("Bad String.hashCode() implementation");
        }
    }

    public CharSequenceBuilder() {
    }

    public void addChars(@NonNull char[] buf, int start, int end) {
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

    public void addString(@NonNull String s) {
        int offset = size;
        int length = s.length();
        size(offset + length);
        s.getChars(0, length, a, offset);
    }

    public void addCharSequence(@NonNull CharSequence s) {
        int offset = size;
        int length = s.length();
        size(offset + length);
        char[] buf = a;
        for (int i = 0; i < length; i++) {
            buf[offset + i] = s.charAt(i);
        }
    }

    @NonNull
    public CharSequenceBuilder updateChars(@NonNull char[] buf, int start, int end) {
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
    @NonNull
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
        if (o instanceof CharSequence csq) {
            int s = size();
            if (s != csq.length()) return false;
            char[] buf = a;
            for (int i = 0; i < s; i++)
                if (buf[i] != csq.charAt(i))
                    return false;
            return true;
        }
        return false;
    }
}
