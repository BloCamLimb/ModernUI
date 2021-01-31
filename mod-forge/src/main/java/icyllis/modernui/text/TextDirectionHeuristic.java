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
 * Interface for objects that use a heuristic for guessing at the paragraph direction by examining text.
 */
public interface TextDirectionHeuristic {

    /**
     * Guess if a chars array is in the RTL direction or not.
     *
     * @param array the char array.
     * @param start start index, inclusive.
     * @param count the length to check, must not be negative and not greater than
     *              {@code array.length - start}.
     * @return true if all chars in the range are to be considered in a RTL direction,
     * false otherwise.
     */
    boolean isRtl(char[] array, int start, int count);

    /**
     * Guess if a {@code CharSequence} is in the RTL direction or not.
     *
     * @param cs    the CharSequence.
     * @param start start index, inclusive.
     * @param count the length to check, must not be negative and not greater than
     *              {@code CharSequence.length() - start}.
     * @return true if all chars in the range are to be considered in a RTL direction,
     * false otherwise.
     */
    boolean isRtl(CharSequence cs, int start, int count);
}
