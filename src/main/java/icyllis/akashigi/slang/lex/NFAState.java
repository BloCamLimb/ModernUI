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

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

import javax.annotation.Nonnull;
import java.util.function.IntPredicate;

public interface NFAState {

    NFAState PLACEHOLDER = NFAState.Remapped(IntLists.emptyList());

    boolean accept(char c);

    // states we transition to upon a successful match from this state
    IntList getNext();

    @Nonnull
    static NFAState Accept(int token) {
        return new Accept(token);
    }

    @Nonnull
    static NFAState Filter(IntPredicate filter, IntList next) {
        return new Filter(filter, next);
    }

    @Nonnull
    static NFAState Remapped(IntList states) {
        return new Remapped(states);
    }

    /**
     * Represents an accepting state - if the NFA ends up in this state, we have successfully
     * matched the token indicated by 'mToken'.
     */
    class Accept implements NFAState {

        public final int mToken;

        private Accept(int token) {
            mToken = token;
        }

        @Override
        public boolean accept(char c) {
            return false;
        }

        @Override
        public IntList getNext() {
            return IntLists.emptyList();
        }
    }

    /**
     * Matches a character that satisfies a character filter.
     */
    class Filter implements NFAState {

        private final IntPredicate mFilter;
        private final IntList mNext;

        private Filter(IntPredicate filter, IntList next) {
            mFilter = filter;
            mNext = next;
        }

        @Override
        public boolean accept(char c) {
            return mFilter.test(c);
        }

        @Override
        public IntList getNext() {
            return mNext;
        }
    }

    /**
     * A state which serves as a placeholder for the states indicated in 'mStates'. When we
     * transition to this state, we instead transition to all of the 'mStates' states.
     */
    class Remapped implements NFAState {

        public final IntList mStates;

        private Remapped(IntList states) {
            mStates = states;
        }

        @Override
        public boolean accept(char c) {
            throw new IllegalStateException();
        }

        @Override
        public IntList getNext() {
            throw new IllegalStateException();
        }
    }
}
