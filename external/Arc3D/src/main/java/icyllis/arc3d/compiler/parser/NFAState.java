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

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnull;
import java.util.function.IntPredicate;

public interface NFAState {

    boolean accept(char c);

    // states we transition to upon a successful match from this state
    @Unmodifiable
    IntList next();

    @Nonnull
    static NFAState Accept(int token) {
        return new Accept(token);
    }

    @Nonnull
    static NFAState Filter(IntPredicate filter, @Unmodifiable IntList next) {
        return new Filter(filter, next);
    }

    @Nonnull
    static NFAState Replace(@Unmodifiable IntList shadow) {
        return new Replace(shadow);
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
        public IntList next() {
            return IntList.of();
        }
    }

    /**
     * Matches a character that satisfies a character filter.
     */
    class Filter implements NFAState {

        private final IntPredicate mFilter;
        @Unmodifiable
        private final IntList mNext;

        private Filter(IntPredicate filter, @Unmodifiable IntList next) {
            mFilter = filter;
            mNext = next;
        }

        @Override
        public boolean accept(char c) {
            return mFilter.test(c);
        }

        @Override
        public IntList next() {
            return mNext;
        }
    }

    /**
     * A state which serves as a placeholder for the states indicated in 'mShadow'. When we
     * transition to this state, we instead transition to all of the 'mShadow' states.
     */
    class Replace implements NFAState {

        @Unmodifiable
        public final IntList mShadow;

        private Replace(@Unmodifiable IntList shadow) {
            mShadow = shadow;
        }

        @Override
        public boolean accept(char c) {
            throw new IllegalStateException();
        }

        @Override
        public IntList next() {
            throw new IllegalStateException();
        }
    }
}
