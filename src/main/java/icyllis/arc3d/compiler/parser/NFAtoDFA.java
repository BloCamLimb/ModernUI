/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.parser;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Unmodifiable;

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
        getOrCreate(IntList.of());
        // create a state representing being in all of the NFA's start states at once
        var n = new IntArrayList(mNFA.mStartStates);
        n.sort(null);
        // this becomes state 1, our start state
        DFAState start = getOrCreate(n);
        traverse(start);

        computeMappings();

        int[][] transitions = new int[mTransitions.size()][];
        Arrays.setAll(transitions, i -> mTransitions.get(i).toIntArray());

        return new DFA(mCharMappings.toIntArray(), transitions, mAccepts.toIntArray());
    }

    /**
     * Returns an existing DFA state with the given NFA states, or creates a new one and returns it.
     */
    @Nonnull
    private DFAState getOrCreate(@Unmodifiable IntList states) {
        DFAState result = mStates.get(states);
        if (result == null) {
            int index = mStates.size();
            result = new DFAState(index, states);
            mStates.put(states, result);
        }
        return result;
    }

    private void add(int index, IntList states) {
        NFAState state = mNFA.get(index);
        if (state instanceof NFAState.Replace replace) {
            for (int i : replace.mShadow) {
                add(i, states);
            }
        } else if (!states.contains(index)) {
            states.add(index);
        }
    }

    private void addTransition(char c, int curr, int next) {
        while (mTransitions.size() <= c) {
            mTransitions.add(new IntArrayList());
        }
        IntList row = mTransitions.get(c);
        while (row.size() <= curr) {
            row.add(DFA.INVALID);
        }
        row.set(curr, next);
    }

    private void traverse(@Nonnull DFAState curr) {
        curr.mScanned = true;
        for (char c = START_CHAR; c <= END_CHAR; ++c) {
            var n = new IntArrayList();
            int best = Integer.MAX_VALUE;
            for (int index : curr.mStates) {
                NFAState state = mNFA.get(index);
                if (state.accept(c)) {
                    for (int i : state.next()) {
                        if (mNFA.get(i) instanceof NFAState.Accept e) {
                            best = Math.min(best, e.mToken);
                        }
                        add(i, n);
                    }
                }
            }
            n.sort(null);
            DFAState next = getOrCreate(n);
            addTransition(c, curr.mIndex, next.mIndex);
            if (best != Integer.MAX_VALUE) {
                while (mAccepts.size() <= next.mIndex) {
                    mAccepts.add(DFA.INVALID);
                }
                mAccepts.set(next.mIndex, best);
            }
            if (!next.mScanned) {
                traverse(next);
            }
        }
    }

    // collapse rows with the same transitions to a single row. This is common, as each row
    // represents a character and often there are many characters for which all transitions are
    // identical (e.g. [0-9] are treated the same way by all lexer rules)
    private void computeMappings() {
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
