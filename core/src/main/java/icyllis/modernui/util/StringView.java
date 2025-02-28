/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.GetChars;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Special class that represents a region of a String, used for fast
 * HashMap lookup and memory comparison.
 */
@ApiStatus.Experimental
public record StringView(@NonNull String string, int offset, int length)
        implements Comparable<CharSequence>, CharSequence, GetChars {

    public StringView(@NonNull String string, int offset, int length) {
        this.offset = Objects.checkFromIndexSize(offset, length, string.length());
        this.string = string;
        this.length = length;
    }

    @Override
    public char charAt(int index) {
        assert index < length;
        return string.charAt(index + offset);
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        if (start == 0 && end == length) {
            return this;
        }
        return new StringView(string, offset + start, end - start);
    }

    @Override
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        Objects.checkFromToIndex(srcBegin, srcEnd, length);
        string.getChars(offset + srcBegin, offset + srcEnd, dst, dstBegin);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = offset, e = i + length; i < e; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String s) {
            // vectorized
            return length == s.length() &&
                    string.startsWith(s, offset);
        }
        if (obj instanceof StringView sv) {
            // vectorized
            return length == sv.length &&
                    string.regionMatches(offset, sv.string, sv.offset, length);
        }
        if (obj instanceof CharSequence csq) {
            // non-vectorized
            if (length != csq.length()) return false;
            for (int i = 0; i < length; i++)
                if (charAt(i) != csq.charAt(i))
                    return false;
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return string.substring(offset, offset + length);
    }

    @Override
    public int compareTo(@NonNull CharSequence o) {
        if (this == o) {
            return 0;
        }
        // it's unlikely to compare two full strings, so we do a non-vectorized comparison here
        for (int i = 0, s = Math.min(length, o.length()); i < s; i++) {
            char a = charAt(i);
            char b = o.charAt(i);
            if (a != b) {
                return a - b;
            }
        }
        return length - o.length();
    }
}
