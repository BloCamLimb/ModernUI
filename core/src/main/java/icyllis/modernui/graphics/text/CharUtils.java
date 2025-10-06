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

package icyllis.modernui.graphics.text;

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.nio.CharBuffer;

public final class CharUtils {

    //TODO may be replaced by a SpinLock
    private static final char[][] sTemp = new char[4][];

    private CharUtils() {
    }

    /**
     * Returns a temporary char buffer.
     *
     * @param len the length of the buffer
     * @return a char buffer
     * @hidden
     * @see #recycle(char[]) recycle the buffer
     */
    @ApiStatus.Internal
    @NonNull
    public static char[] obtain(int len) {
        if (len > 2000)
            return new char[len];

        char[] buf = null;

        synchronized (sTemp) {
            final char[][] pool = sTemp;
            for (int i = pool.length - 1; i >= 0; --i) {
                if ((buf = pool[i]) != null && buf.length >= len) {
                    pool[i] = null;
                    break;
                }
            }
        }

        if (buf == null || buf.length < len)
            buf = new char[len];
        else if (buf.length > len)
            buf[len] = '\0'; // Modern UI: avoid tainting

        return buf;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static void recycle(@NonNull char[] temp) {
        if (temp.length > 2000)
            return;

        synchronized (sTemp) {
            final char[][] pool = sTemp;
            for (int i = 0; i < pool.length; ++i) {
                if (pool[i] == null) {
                    pool[i] = temp;
                    break;
                }
            }
        }
    }

    /**
     * Copies a block of characters efficiently, extending to {@link String#getChars(int, int, char[], int)}.
     * Typically, when the given <var>s</var> is one of the following classes:<br>
     * {@link String}, {@link StringBuffer}, {@link StringBuilder}, {@link CharBuffer},
     * {@link GetChars}.
     *
     * @throws IndexOutOfBoundsException if out of range
     */
    public static void getChars(@NonNull CharSequence s, int srcBegin, int srcEnd,
                                @NonNull char[] dst, int dstBegin) {
        if (s instanceof String)
            ((String) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof GetChars)
            ((GetChars) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof StringBuffer)
            ((StringBuffer) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof StringBuilder)
            ((StringBuilder) s).getChars(srcBegin, srcEnd, dst, dstBegin);
        else if (s instanceof CharBuffer buf)
            buf.get(buf.position() + srcBegin, dst, dstBegin, srcEnd - srcBegin); // Java 13
        else {
            if (srcBegin > srcEnd)
                throw new IndexOutOfBoundsException("srcBegin " + srcBegin + ", srcEnd " + srcEnd);
            for (int i = srcBegin; i < srcEnd; i++)
                dst[dstBegin++] = s.charAt(i);
        }
    }
}
