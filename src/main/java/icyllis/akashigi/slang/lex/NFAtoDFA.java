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

import it.unimi.dsi.fastutil.ints.*;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Converts a nondeterministic finite automaton to a deterministic finite automaton. Since NFAs and
 * DFAs differ only in that an NFA allows multiple states at the same time, we can find each
 * possible combination of simultaneous NFA states and give this combination a label. These labelled
 * nodes are our DFA nodes, since we can only be in one such unique set of NFA states at a time.
 * <p>
 * As an NFA can end up in multiple accept states at the same time (for instance, the token "while"
 * is valid for both WHILE and IDENTIFIER), we disambiguate by preferring the first matching regex
 * (in terms of the order in which they were added to the NFA).
 */
public class NFAtoDFA {

    public static final char START_CHAR = 0x09;
    public static final char END_CHAR = 0x7E;

    private final NFA mNFA;

    private final Map<IntList, DFAState> mStates = new HashMap<>(); // NFA states to DFA state
    private final List<IntList> mTransitions = new ArrayList<>(); // char to DFA states
    private final IntList mCharMappings = new IntArrayList();
    private final IntList mAccepts = new IntArrayList();

    public NFAtoDFA(NFA NFA) {
        mNFA = NFA;
    }

    /**
     * Returns a DFA created from the NFA.
     */
    @Nonnull
    public DFA convert() {
        // create state 0, the "reject" state
        getOrCreate(IntLists.emptyList());
        // create a state representing being in all of the NFA's start states at once
        var startStates = new IntArrayList(mNFA.mStartStates);
        startStates.sort(null);
        // this becomes state 1, our start state
        DFAState start = getOrCreate(startStates);
        scanState(start);

        computeMappings();

        int[][] transitions = new int[mTransitions.size()][];
        Arrays.setAll(transitions, i -> mTransitions.get(i).toIntArray());

        return new DFA(mCharMappings.toIntArray(), transitions, mAccepts.toIntArray());
    }

    /**
     * Returns an existing DFA state with the given NFA states, or creates a new one and returns it.
     */
    @Nonnull
    private DFAState getOrCreate(IntList states) {
        DFAState state = mStates.get(states);
        if (state == null) {
            int id = mStates.size();
            state = new DFAState(id, states);
            mStates.put(states, state);
            return state;
        }
        return state;
    }

    private void add(int index, IntList states) {
        NFAState state = mNFA.get(index);
        if (state instanceof NFAState.Remapped remapped) {
            for (int nextState : remapped.mStates) {
                add(nextState, states);
            }
        } else if (!states.contains(index)) {
            states.add(index);
        }
    }

    void addTransition(char c, int start, int next) {
        while (mTransitions.size() <= c) {
            mTransitions.add(new IntArrayList());
        }
        IntList row = mTransitions.get(c);
        while (row.size() <= start) {
            row.add(DFA.INVALID);
        }
        row.set(start, next);
    }

    void scanState(@Nonnull DFAState state) {
        state.mScanned = true;
        for (char c = START_CHAR; c <= END_CHAR; ++c) {
            var next = new IntArrayList();
            int bestAccept = Integer.MAX_VALUE;
            for (int id : state.mStates) {
                NFAState nfaState = mNFA.get(id);
                if (nfaState.accept(c)) {
                    for (int nextId : nfaState.getNext()) {
                        if (mNFA.get(nextId) instanceof NFAState.Accept end) {
                            bestAccept = Math.min(bestAccept, end.mToken);
                        }
                        add(nextId, next);
                    }
                }
            }
            next.sort(null);
            DFAState nextState = getOrCreate(next);
            addTransition(c, state.mId, nextState.mId);
            if (bestAccept != Integer.MAX_VALUE) {
                while (mAccepts.size() <= nextState.mId) {
                    mAccepts.add(DFA.INVALID);
                }
                mAccepts.set(nextState.mId, bestAccept);
            }
            if (!nextState.mScanned) {
                scanState(nextState);
            }
        }
    }

    // collapse rows with the same transitions to a single row. This is common, as each row
    // represents a character and often there are many characters for which all transitions are
    // identical (e.g. [0-9] are treated the same way by all lexer rules)
    void computeMappings() {
        // mappings[<input row>] = <output row>
        List<IntList> uniques = new ArrayList<>();
        // this could be done more efficiently, but O(n^2) is plenty fast for our purposes
        for (IntList transition : mTransitions) {
            int found = uniques.indexOf(transition);
            if (found == -1) {
                found = uniques.size();
                uniques.add(transition);
            }
            mCharMappings.add(found);
        }
        mTransitions.clear();
        mTransitions.addAll(uniques);
    }
}
