/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.aksl.lex;

import java.util.Arrays;

/**
 * Tables representing a deterministic finite automaton for matching regular expressions.
 */
public class DFA {

    public static final int INVALID = -1;

    // maps chars to the row index of mTransitions, as multiple characters may map to the same row.
    // starting from state s and looking at char c, the new state is
    // mTransitions[mCharMappings[c]][s].
    public final int[] mCharMappings;

    // one row per character mapping, one column per state
    public final int[][] mTransitions;

    // contains, for each state, the token id we should report when matching ends in that state
    // (INVALID for no match)
    public final int[] mAccepts;

    public DFA(int[] charMappings, int[][] transitions, int[] accepts) {
        mCharMappings = charMappings;
        mTransitions = transitions;
        mAccepts = accepts;
    }

    @Override
    public String toString() {
        return "DFA{" +
                "mCharMappings=" + Arrays.toString(mCharMappings) +
                ", mTransitions=" + Arrays.toString(mTransitions) +
                ", mAccepts=" + Arrays.toString(mAccepts) +
                '}';
    }
}
