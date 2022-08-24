/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.sksl.lex;

import it.unimi.dsi.fastutil.ints.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;

public class NFAtoDFA {

    public static final char START_CHAR = 9;
    public static final char END_CHAR = 126;

    private final NFA mNFA;

    private final HashMap<IntList, DFAState> mStates = new HashMap<>();
    private final ArrayList<IntArrayList> mTransitions = new ArrayList<>();
    private final IntArrayList mCharMappings = new IntArrayList();
    private final IntArrayList mAccepts = new IntArrayList();

    public NFAtoDFA(NFA NFA) {
        mNFA = NFA;
    }

    /**
     * Returns a DFA created from the NFA.
     */
    public DFA convert() {
        // create state 0, the "reject" state
        getState(IntLists.emptyList());
        // create a state representing being in all of the NFA's start states at once
        var startStates = new IntArrayList(mNFA.mStartStates);
        startStates.sort(IntComparators.NATURAL_COMPARATOR);
        // this becomes state 1, our start state
        DFAState start = getState(startStates);
        scanState(start);

        computeMappings();

        int[][] transitions = new int[mTransitions.size()][];
        for (int i = 0; i < mTransitions.size(); i++) {
            transitions[i] = mTransitions.get(i).toIntArray();
        }

        return new DFA(mCharMappings.toIntArray(), transitions, mAccepts.toIntArray());
    }

    /**
     * Returns an existing state with the given label, or creates a new one and returns it.
     */
    @Nonnull
    private DFAState getState(IntList label) {
        DFAState state = mStates.get(label);
        if (state == null) {
            int id = mStates.size();
            state = new DFAState(id, label);
            mStates.put(label, state);
            return state;
        }
        return state;
    }

    private void add(int nfaState, IntList states) {
        NFAState state = mNFA.mStates.get(nfaState);
        if (state.mKind == NFAState.Kind_Remapped) {
            for (int next : state.mData) {
                add(next, states);
            }
        } else {
            for (int entry : states) {
                if (nfaState == entry) {
                    return;
                }
            }
            states.add(nfaState);
        }
    }

    void addTransition(char c, int start, int next) {
        while (mTransitions.size() <= c) {
            mTransitions.add(new IntArrayList());
        }
        var row = mTransitions.get(c);
        while (row.size() <= start) {
            row.add(DFA.INVALID);
        }
        row.set(start, next);
    }

    void scanState(@Nonnull DFAState state) {
        state.mIsScanned = true;
        for (char c = START_CHAR; c <= END_CHAR; ++c) {
            var next = new IntArrayList();
            int bestAccept = Integer.MAX_VALUE;
            for (int idx : state.mLabel) {
                NFAState nfaState = mNFA.mStates.get(idx);
                if (nfaState.accept(c)) {
                    for (int nextState : nfaState.mNext) {
                        if (mNFA.mStates.get(nextState).mKind == NFAState.Kind_Accept) {
                            bestAccept = Math.min(bestAccept,
                                    mNFA.mStates.get(nextState).mData.getInt(0));
                        }
                        add(nextState, next);
                    }
                }
            }
            next.sort(IntComparators.NATURAL_COMPARATOR);
            DFAState nextState = getState(next);
            addTransition(c, state.mId, nextState.mId);
            if (bestAccept != Integer.MAX_VALUE) {
                while (mAccepts.size() <= nextState.mId) {
                    mAccepts.add(DFA.INVALID);
                }
                mAccepts.set(nextState.mId, bestAccept);
            }
            if (!nextState.mIsScanned) {
                scanState(nextState);
            }
        }
    }

    // collapse rows with the same transitions to a single row. This is common, as each row
    // represents a character and often there are many characters for which all transitions are
    // identical (e.g. [0-9] are treated the same way by all lexer rules)
    void computeMappings() {
        // mappings[<input row>] = <output row>
        ArrayList<IntArrayList> uniques = new ArrayList<>();
        // this could be done more efficiently, but O(n^2) is plenty fast for our purposes
        for (var transition : mTransitions) {
            int found = -1;
            for (int j = 0; j < uniques.size(); ++j) {
                if (uniques.get(j).equals(transition)) {
                    found = j;
                    break;
                }
            }
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
