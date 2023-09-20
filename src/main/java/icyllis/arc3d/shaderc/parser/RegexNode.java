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

package icyllis.arc3d.shaderc.parser;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnull;
import java.util.BitSet;

/**
 * Represents a node in the parse tree of a regular expression.
 */
@FunctionalInterface
public interface RegexNode {

    /**
     * Creates NFA states for this node, with a successful match against this node resulting in a
     * transition next all the states in the {@code next} list.
     */
    @Unmodifiable
    IntList transition(NFA nfa, @Unmodifiable IntList next);

    /**
     * Match a character.
     */
    @Nonnull
    static RegexNode Char(char c) {
        return new Char(c);
    }

    /**
     * Match a character range.
     */
    @Nonnull
    static RegexNode Range(char start, char end) {
        return new Range(start, end);
    }

    /**
     * Match a character range.
     */
    @Nonnull
    static RegexNode Range(RegexNode start, RegexNode end) {
        try {
            return new Range(((Char) start).mChar, ((Char) end).mChar);
        } catch (ClassCastException e) {
            throw new IllegalStateException("character range contains non-literal characters", e);
        }
    }

    /**
     * Match a character class.
     */
    @Nonnull
    static RegexNode CharClass(RegexNode... clazz) {
        return new CharClass(clazz, false);
    }

    /**
     * Match a character class.
     */
    @Nonnull
    static RegexNode CharClass(RegexNode[] clazz, boolean exclusive) {
        return new CharClass(clazz, exclusive);
    }

    /**
     * Concatenation: XY (X -> Y -> Next)
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Concat(RegexNode x, RegexNode y) {
        return (nfa, next) -> x.transition(nfa, y.transition(nfa, next));
    }

    /**
     * Alternation: X|Y (X -> Next, Y -> Next)
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Union(RegexNode x, RegexNode y) {
        return (nfa, next) -> {
            IntList xn = x.transition(nfa, next);
            IntList yn = y.transition(nfa, next);
            int[] result = new int[xn.size() + yn.size()];
            xn.getElements(0, result, 0, xn.size());
            yn.getElements(0, result, xn.size(), yn.size());
            return IntList.of(result);
        };
    }

    /**
     * Wildcard, excluding LF.
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
     * Kleene closure: X -> X, X -> Next, Next -> Next.
     * Match between zero, one and unlimited times.
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Star(RegexNode x) {
        return (nfa, next) -> {
            int[] loop = new int[next.size() + 1];
            next.getElements(0, loop, 0, next.size());
            int state = nfa.add(NFAState.PLACEHOLDER);
            loop[next.size()] = state;
            IntList left = x.transition(nfa, IntList.of(loop));
            int[] result = new int[left.size() + next.size()];
            left.getElements(0, result, 0, left.size());
            next.getElements(0, result, left.size(), next.size());
            return nfa.replace(state, IntList.of(result));
        };
    }

    /**
     * X -> X, X -> Next.
     * Match between one and unlimited times.
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Plus(RegexNode x) {
        return (nfa, next) -> {
            int[] loop = new int[next.size() + 1];
            next.getElements(0, loop, 0, next.size());
            int state = nfa.add(NFAState.PLACEHOLDER);
            loop[next.size()] = state;
            IntList result = x.transition(nfa, IntList.of(loop));
            return nfa.replace(state, result);
        };
    }

    /**
     * X -> Next, Next -> Next.
     * Match between zero and one times.
     */
    @Nonnull
    @Contract(pure = true)
    static RegexNode Ques(RegexNode x) {
        return (nfa, next) -> {
            IntList left = x.transition(nfa, next);
            int[] result = new int[left.size() + next.size()];
            left.getElements(0, result, 0, left.size());
            next.getElements(0, result, left.size(), next.size());
            return IntList.of(result);
        };
    }

    class Char implements RegexNode {

        public final char mChar;

        private Char(char c) {
            mChar = c;
        }

        @Override
        public IntList transition(NFA nfa, @Unmodifiable IntList next) {
            int state = nfa.add(NFAState.Filter(ch -> ch == mChar, next));
            return IntList.of(state);
        }

        @Override
        public String toString() {
            return "Char(0x" + Integer.toHexString(mChar) + ")";
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
        public IntList transition(NFA nfa, @Unmodifiable IntList next) {
            int state = nfa.add(NFAState.Filter(ch -> ch >= mStart && ch <= mEnd, next));
            return IntList.of(state);
        }

        @Override
        public String toString() {
            return "Range(0x" + Integer.toHexString(mStart) +
                    ", 0x" + Integer.toHexString(mEnd) + ")";
        }
    }

    class CharClass extends BitSet implements RegexNode {

        public final boolean mExclusive;

        private CharClass(RegexNode[] clazz, boolean exclusive) {
            mExclusive = exclusive;
            for (RegexNode x : clazz) {
                if (x instanceof Char node) {
                    set(node.mChar);
                } else if (x instanceof Range node) {
                    set(node.mStart, node.mEnd);
                } else if (x instanceof CharClass node) {
                    if (node.mExclusive) {
                        assert false;
                        xor(node);
                    } else {
                        or(node);
                    }
                } else {
                    throw new AssertionError(x);
                }
            }
        }

        @Override
        public IntList transition(NFA nfa, @Unmodifiable IntList next) {
            int state = nfa.add(NFAState.Filter(ch -> get(ch) ^ mExclusive, next));
            return IntList.of(state);
        }

        @Override
        public String toString() {
            return "CharClass(" + (mExclusive ? "^" : "") + super.toString() + ")";
        }
    }
}
