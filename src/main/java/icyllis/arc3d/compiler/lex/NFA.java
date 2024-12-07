/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.lex;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.checkerframework.checker.nullness.qual.NonNull;

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
    public void add(@NonNull RegexNode node) {
        // we reserve token 0 for END_OF_FILE, so this starts at 1
        int token = ++mTokenIndex;
        int state = add(NFAState.Accept(token));
        mStartStates.addAll(node.transition(this, IntList.of(state)));
    }

    /**
     * Adds a new state to the NFA, returning its index.
     */
    public int add(NFAState state) {
        int index = mStates.size();
        mStates.add(state);
        return index;
    }

    /**
     * Gets a NFA state from its index.
     */
    public NFAState get(int index) {
        return mStates.get(index);
    }

    /**
     * When we transition to the NFA state which is given by {@code index},
     * we instead transition to all of the {@code shadow} states.
     */
    public IntList replace(int index, IntList shadow) {
        assert (mStates.get(index) == null);
        mStates.set(index, NFAState.Replace(shadow));
        return shadow;
    }

    /**
     * Matches a string against all of the regexes added to this NFA. Returns the index of the first
     * (in addRegex order) matching expression, or -1 if no match. This is relatively slow and used
     * only for debugging purposes; the NFA should be converted to a DFA before actual use.
     */
    public int match(@NonNull String s) {
        IntList states = mStartStates;
        for (int p = 0; p < s.length(); ++p) {
            var n = new IntArrayList();
            for (int index : states) {
                NFAState state = get(index);
                if (state.accept(s.charAt(p))) {
                    for (int i : state.next()) {
                        if (get(i) instanceof NFAState.Replace replace) {
                            n.addAll(replace.mShadow);
                        } else {
                            n.add(i);
                        }
                    }
                }
            }
            if (n.isEmpty()) {
                return DFA.INVALID;
            }
            states = n;
        }
        int accept = DFA.INVALID;
        for (int index : states) {
            if (get(index) instanceof NFAState.Accept e) {
                if (accept == DFA.INVALID || e.mToken < accept) {
                    accept = e.mToken;
                }
            }
        }
        return accept;
    }
}
