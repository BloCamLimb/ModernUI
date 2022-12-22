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

/**
 * A nondeterministic finite automaton for matching regular expressions. The NFA is initialized with
 * a number of regular expressions, and then matches a string against all of them simultaneously.
 */
public class NFA {

    public int mRegexCount = 0;
    public ArrayList<NFAState> mStates = new ArrayList<>();
    public IntArrayList mStartStates = new IntArrayList();

    /**
     * Adds a new regular expression to the set of expressions matched by this automaton, returning
     * its index.
     */
    public int addRegex(@Nonnull RegexNode regex) {
        IntList accept = new IntArrayList();
        // we reserve token 0 for END_OF_FILE, so this starts at 1
        accept.add(addState(new NFAState(++mRegexCount)));
        IntList startStates = regex.createStates(this, accept);
        mStartStates.addAll(startStates);
        return mStartStates.size() - 1;
    }

    /**
     * Adds a new state to the NFA, returning its index.
     */
    public int addState(@Nonnull NFAState s) {
        mStates.add(s);
        return mStates.size() - 1;
    }

    /**
     * Matches a string against all of the regexes added to this NFA. Returns the index of the first
     * (in addRegex order) matching expression, or -1 if no match. This is relatively slow and used
     * only for debugging purposes; the NFA should be converted to a DFA before actual use.
     */
    public int match(@Nonnull String s) {
        var states = mStartStates;
        for (int i = 0; i < s.length(); ++i) {
            var next = new IntArrayList();
            for (int id : states) {
                if (mStates.get(id).accept(s.charAt(i))) {
                    for (int nextId : mStates.get(id).mNext) {
                        if (mStates.get(nextId).mKind != NFAState.Kind_Remapped) {
                            next.add(nextId);
                        } else {
                            next.addAll(mStates.get(nextId).mData);
                        }
                    }
                }
            }
            if (next.isEmpty()) {
                return -1;
            }
            states = next;
        }
        int accept = -1;
        for (int id : states) {
            if (mStates.get(id).mKind == NFAState.Kind_Accept) {
                int result = mStates.get(id).mData.getInt(0);
                if (accept == -1 || result < accept) {
                    accept = result;
                }
            }
        }
        return accept;
    }
}
