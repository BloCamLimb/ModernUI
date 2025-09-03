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

package icyllis.modernui.text;

/**
 * Implement this interface if your {@link CharSequence} has a {@link String#getChars(int, int, char[], int)}
 * method like the one in {@link String} that is faster than calling {@link CharSequence#charAt(int)} multiple times.
 */
public interface GetChars extends CharSequence {

    /**
     * @throws IndexOutOfBoundsException if out of range
     */
    void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin);
}
