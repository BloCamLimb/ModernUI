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

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Turns a simple regular expression into a parse tree. The regular expression syntax supports only
 * the basic quantifiers ('*', '+', and '?'), alternation ('|'), character classes ('[a-z]'), and
 * groups ('()').
 */
public class RegexParser {

    private static final char EOF = '\0';

    private final Deque<RegexNode> mStack = new ArrayDeque<>();
    private String mRegex;
    private int mIndex;

    @Nonnull
    public RegexNode parse(@Nonnull String regex) {
        mRegex = regex;
        mIndex = 0;
        assert (mStack.size() == 0);
        regex();
        assert (mStack.size() == 1);
        assert (mIndex == regex.length());
        return pop();
    }

    private char peek() {
        if (mIndex >= mRegex.length()) {
            return EOF;
        }
        return mRegex.charAt(mIndex);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalStateException(
                    String.format("expected '%c' at index %d, but found '%c'", c, mIndex, peek()));
        }
        ++mIndex;
    }

    private void push(@Nonnull RegexNode node) {
        mStack.push(node);
    }

    @Nonnull
    private RegexNode pop() {
        return mStack.pop();
    }

    /**
     * Matches a char literal, parenthesized group, character class, or dot ('.').
     */
    private void atom() {
        switch (peek()) {
            case '(' -> group();
            case '[' -> clazz();
            case '.' -> dot();
            default -> literal();
        }
    }

    /**
     * Matches a term followed by an optional quantifier ('*', '+', or '?').
     */
    private void factor() {
        atom();
        switch (peek()) {
            case '*' -> {
                push(RegexNode.Star(pop()));
                ++mIndex;
            }
            case '+' -> {
                push(RegexNode.Plus(pop()));
                ++mIndex;
            }
            case '?' -> {
                push(RegexNode.Ques(pop()));
                ++mIndex;
            }
        }
    }

    /**
     * Matches a sequence of factors.
     */
    private void sequence() {
        factor();
        for (;;) {
            switch (peek()) {
                case EOF: // fall through
                case '|': // fall through
                case ')':
                    return;
                default:
                    sequence();
                    RegexNode y = pop();
                    RegexNode x = pop();
                    push(RegexNode.Concat(x, y));
                    break;
            }
        }
    }

    /**
     * Returns a node representing the given escape character (e.g. escape('n') returns a
     * node which matches a line feed character).
     */
    private RegexNode escape(char c) {
        return switch (c) {
            case 't' -> RegexNode.Char('\t');
            case 'n' -> RegexNode.Char('\n');
            case 'r' -> RegexNode.Char('\r');
            case 's' -> RegexNode.CharClass(List.of(
                    RegexNode.Char('\t'),
                    RegexNode.Char('\n'),
                    RegexNode.Char('\r'),
                    RegexNode.Char('\040')));
            case 'd' -> RegexNode.Range('0', '9');
            case 'w' -> RegexNode.CharClass(List.of(
                    RegexNode.Range('a', 'z'),
                    RegexNode.Range('A', 'Z'),
                    RegexNode.Range('0', '9'),
                    RegexNode.Char('_')));
            default -> RegexNode.Char(c);
        };
    }

    /**
     * Matches a literal character or escape sequence.
     */
    private void literal() {
        char c = peek();
        if (c == '\\') {
            ++mIndex;
            push(escape(peek()));
        } else {
            push(RegexNode.Char(c));
        }
        ++mIndex;
    }

    /**
     * Matches a dot ('.').
     */
    private void dot() {
        expect('.');
        push(RegexNode.Dot());
    }

    /**
     * Matches a parenthesized group.
     */
    private void group() {
        expect('(');
        regex();
        expect(')');
    }

    /**
     * Matches a literal character, escape sequence, or character range from a character class.
     */
    private void item() {
        literal();
        if (peek() == '-') {
            ++mIndex;
            if (peek() == ']') {
                push(RegexNode.Char('-'));
            } else {
                literal();
                RegexNode end = pop();
                RegexNode start = pop();
                push(RegexNode.Range(start, end));
            }
        }
    }

    /**
     * Matches a character class.
     */
    private void clazz() {
        expect('[');
        int depth = mStack.size();
        boolean negative;
        if (peek() == '^') {
            ++mIndex;
            negative = true;
        } else {
            negative = false;
        }
        for (;;) {
            switch (peek()) {
                case ']' -> {
                    ++mIndex;
                    int n = mStack.size() - depth;
                    List<RegexNode> clazz = new ArrayList<>(n);
                    while (n-- > 0) {
                        clazz.add(pop());
                    }
                    push(RegexNode.CharClass(clazz, negative));
                    return;
                }
                case EOF -> throw new IllegalStateException("unterminated character class");
                default -> item();
            }
        }
    }

    private void regex() {
        sequence();
        switch (peek()) {
            case '|': {
                ++mIndex;
                regex();
                RegexNode y = pop();
                RegexNode x = pop();
                push(RegexNode.Union(x, y));
                break;
            }
            case EOF: // fall through
            case ')':
                return;
            default:
                throw new IllegalStateException();
        }
    }
}
