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
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.BitSet;
import java.util.List;

/**
 * Represents a node in the parse tree of a regular expression.
 */
@FunctionalInterface
public interface RegexNode {

    /**
     * Creates NFA states for this node, with a successful match against this node resulting in a
     * transition to all of the states in the {@code next} vector.
     */
    IntList createStates(NFA nfa, IntList next);

    @Nonnull
    static RegexNode Char(char c) {
        return new Char(c);
    }

    @Nonnull
    static RegexNode Range(char start, char end) {
        return new Range(start, end);
    }

    @Nonnull
    static RegexNode Range(RegexNode start, RegexNode end) {
        return new Range(((Char) start).mChar, ((Char) end).mChar);
    }

    @Nonnull
    static RegexNode CharClass(List<RegexNode> clazz) {
        return new CharClass(clazz, false);
    }

    @Nonnull
    static RegexNode CharClass(List<RegexNode> clazz, boolean negative) {
        return new CharClass(clazz, negative);
    }

    /**
     * Concatenation: XY (X -> Y -> Next)
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Concat(RegexNode x, RegexNode y) {
        return (nfa, next) -> {
            IntList right = y.createStates(nfa, next);
            return x.createStates(nfa, right);
        };
    }

    /**
     * Alternation: X|Y (X -> Next, Y -> Next)
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Union(RegexNode x, RegexNode y) {
        return (nfa, next) -> {
            var result = new IntArrayList(x.createStates(nfa, next));
            result.addAll(y.createStates(nfa, next));
            return result;
        };
    }

    /**
     * Wildcard.
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Dot() {
        return (nfa, next) -> {
            int state = nfa.add(NFAState.Filter(ch -> ch != '\n', next));
            return IntList.of(state);
        };
    }

    /**
     * Closure.
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Star(RegexNode x) {
        return (nfa, next) -> {
            var cycle = new IntArrayList(next);
            int state = nfa.add(NFAState.PLACEHOLDER);
            cycle.add(state);
            var result = new IntArrayList(x.createStates(nfa, cycle));
            result.addAll(next);
            nfa.remap(state, result);
            return result;
        };
    }

    @Nonnull
    @Contract(pure = true)
    static RegexNode Plus(RegexNode x) {
        return (nfa, next) -> {
            var cycle = new IntArrayList(next);
            int state = nfa.add(NFAState.PLACEHOLDER);
            cycle.add(state);
            IntList result = x.createStates(nfa, cycle);
            nfa.remap(state, result);
            return result;
        };
    }

    /**
     * X -> Next, Next -> Next
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Ques(RegexNode x) {
        return (nfa, next) -> {
            var result = new IntArrayList(x.createStates(nfa, next));
            result.addAll(next);
            return result;
        };
    }

    class Char implements RegexNode {

        public final char mChar;

        private Char(char c) {
            mChar = c;
        }

        @Override
        public IntList createStates(NFA nfa, IntList next) {
            int state = nfa.add(NFAState.Filter(ch -> ch == mChar, next));
            return IntList.of(state);
        }
    }

    class Range implements RegexNode {

        public final char mStart;
        public final char mEnd;

        private Range(char start, char end) {
            if (start > end) {
                throw new IllegalStateException(
                        String.format("character range '%c'-'%c' is out of order", start, end));
            }
            mStart = start;
            mEnd = end;
        }

        @Override
        public IntList createStates(NFA nfa, IntList next) {
            int state = nfa.add(NFAState.Filter(ch -> ch >= mStart && ch <= mEnd, next));
            return IntList.of(state);
        }
    }

    class CharClass implements RegexNode {

        public final BitSet mTable = new BitSet();
        public final boolean mNegative;

        private CharClass(List<RegexNode> clazz, boolean negative) {
            mNegative = negative;
            for (RegexNode x : clazz) {
                if (x instanceof Char node) {
                    mTable.set(node.mChar);
                } else if (x instanceof Range node) {
                    mTable.set(node.mStart, node.mEnd);
                } else if (x instanceof CharClass node) {
                    assert !node.mNegative;
                    mTable.or(node.mTable);
                } else {
                    throw new AssertionError(x);
                }
            }
        }

        @Override
        public IntList createStates(NFA nfa, IntList next) {
            int state = nfa.add(NFAState.Filter(ch -> mTable.get(ch) ^ mNegative, next));
            return IntList.of(state);
        }
    }
}
