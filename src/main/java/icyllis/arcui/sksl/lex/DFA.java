/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl.lex;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

/**
 * Tables representing a deterministic finite automaton for matching regular expressions.
 */
public class DFA {

    // maps chars to the row index of fTransitions, as multiple characters may map to the same row.
    // starting from state s and looking at char c, the new state is
    // fTransitions[fCharMappings[c]][s].
    IntList fCharMappings;

    // one row per character mapping, one column per state
    List<IntList> fTransitions;

    // contains, for each state, the token id we should report when matching ends in that state (-1
    // for no match)
    IntList fAccepts;

    public DFA(IntList fCharMappings, List<IntList> fTransitions, IntList fAccepts) {
        this.fCharMappings = fCharMappings;
        this.fTransitions = fTransitions;
        this.fAccepts = fAccepts;
    }
}
