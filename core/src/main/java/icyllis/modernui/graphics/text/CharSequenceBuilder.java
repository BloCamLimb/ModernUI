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
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * A hashable {@link StringBuilder} to compare with {@link CharSequence} or build
 * {@link String}. This class matches {@link String}'s hashCode() and equals().
 * Note that String's hashCode() does not match the hashCode() of List and Array.
 *
 * @hidden
 */
// this class should not extend CharArrayList, internal use only
@ApiStatus.Internal
public class CharSequenceBuilder extends CharArrayList implements CharSequence, GetChars {

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
            throw new AssertionError("Bad String.hashCode() implementation");
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
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        Objects.checkFromToIndex(srcBegin, srcEnd, size);
        System.arraycopy(a, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    @Override
    public int length() {
        return size;
    }

    @Override
    public char charAt(int index) {
        return getChar(index);
    }

    /**
     * @return a new string
     */
    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, size);
        return new String(a, start, end - start);
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

    /**
     * Same as {@link String#contentEquals(CharSequence)}.
     */
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
