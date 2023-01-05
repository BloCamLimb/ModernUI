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

package icyllis.akashigi.slang.lex;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A nondeterministic finite automaton for matching regular expressions. The NFA is initialized with
 * a number of regular expressions, and then matches a string against all of them simultaneously.
 */
public class NFA {

    private final List<NFAState> mStates = new ArrayList<>();
    private int mTokenIndex = 0;

    final IntList mStartStates = new IntArrayList();

    /**
     * Adds a new regular expression to the set of expressions matched by this automaton.
     */
    public void add(@Nonnull RegexNode n) {
        // we reserve token 0 for END_OF_FILE, so this starts at 1
        int token = ++mTokenIndex;
        int index = add(NFAState.Accept(token));
        mStartStates.addAll(n.createStates(this, IntList.of(index)));
    }

    /**
     * Adds a new state to the NFA, returning its index.
     */
    public int add(NFAState s) {
        mStates.add(s);
        return mStates.size() - 1;
    }

    /**
     * Gets a NFA state from its index.
     */
    public NFAState get(int index) {
        return mStates.get(index);
    }

    public void remap(int index, IntList states) {
        assert mStates.get(index) == NFAState.PLACEHOLDER;
        mStates.set(index, NFAState.Remapped(states));
    }

    /**
     * Matches a string against all of the regexes added to this NFA. Returns the index of the first
     * (in addRegex order) matching expression, or -1 if no match. This is relatively slow and used
     * only for debugging purposes; the NFA should be converted to a DFA before actual use.
     */
    public int match(@Nonnull String s) {
        IntList states = mStartStates;
        for (int i = 0; i < s.length(); ++i) {
            var next = new IntArrayList();
            for (int id : states) {
                NFAState state = get(id);
                if (state.accept(s.charAt(i))) {
                    for (int nextId : state.getNext()) {
                        if (get(nextId) instanceof NFAState.Remapped remapped) {
                            next.addAll(remapped.mStates);
                        } else {
                            next.add(nextId);
                        }
                    }
                }
            }
            if (next.isEmpty()) {
                return DFA.INVALID;
            }
            states = next;
        }
        int accept = DFA.INVALID;
        for (int id : states) {
            if (get(id) instanceof NFAState.Accept end) {
                if (accept == DFA.INVALID || end.mToken < accept) {
                    accept = end.mToken;
                }
            }
        }
        return accept;
    }
}
