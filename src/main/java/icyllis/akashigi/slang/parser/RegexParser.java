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

package icyllis.akashigi.slang.parser;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Turns a simple regular expression into a parse tree. The regular expression syntax supports only
 * the basic quantifiers ('*', '+', and '?'), alternation ('|'), character classes ('[a-z]'), and
 * groups ('()').
 */
public class RegexParser {

    private static final char EOF = '\0';

    private final Deque<RegexNode> mStack = new ArrayDeque<>();

    private String mSource;
    private int mOffset;

    @Nonnull
    public RegexNode parse(@Nonnull String source) {
        mSource = source;
        mOffset = 0;
        assert (mStack.size() == 0);
        regex();
        assert (mStack.size() == 1);
        assert (mOffset == source.length());
        return pop();
    }

    private char peek() {
        if (mOffset >= mSource.length()) {
            return EOF;
        }
        return mSource.charAt(mOffset);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalStateException(
                    String.format("expected '%c' at index %d, but found '%c'", c, mOffset, peek()));
        }
        ++mOffset;
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
                ++mOffset;
            }
            case '+' -> {
                push(RegexNode.Plus(pop()));
                ++mOffset;
            }
            case '?' -> {
                push(RegexNode.Ques(pop()));
                ++mOffset;
            }
        }
    }

    /**
     * Matches a sequence of factors.
     */
    // @formatter:off
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
    // @formatter:on

    /**
     * Returns a node representing the given escape character (e.g. escape('n') returns a
     * node which matches a line feed character).
     */
    private RegexNode escape(char c) {
        return switch (c) {
            case 't' -> RegexNode.Char('\t');
            case 'n' -> RegexNode.Char('\n');
            case 'r' -> RegexNode.Char('\r');
            case 's' -> RegexNode.CharClass(
                    RegexNode.Char('\t'),
                    RegexNode.Char('\n'),
                    RegexNode.Char('\r'),
                    RegexNode.Char('\040'));
            case 'd' -> RegexNode.Range('0', '9');
            case 'w' -> RegexNode.CharClass(
                    RegexNode.Range('a', 'z'),
                    RegexNode.Range('A', 'Z'),
                    RegexNode.Range('0', '9'),
                    RegexNode.Char('_'));
            default -> RegexNode.Char(c);
        };
    }

    /**
     * Matches a literal character or escape sequence.
     */
    private void literal() {
        char c = peek();
        if (c == '\\') {
            ++mOffset;
            push(escape(peek()));
        } else {
            push(RegexNode.Char(c));
        }
        ++mOffset;
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
            ++mOffset;
            if (peek() == ']') {
                push(RegexNode.Char('-'));
            } else {
                literal();
                RegexNode en = pop();
                RegexNode st = pop();
                push(RegexNode.Range(st, en));
            }
        }
    }

    /**
     * Matches a character class.
     */
    // @formatter:off
    private void clazz() {
        expect('[');
        int depth = mStack.size();
        boolean negative;
        if (peek() == '^') {
            ++mOffset;
            negative = true;
        } else {
            negative = false;
        }
        for (;;) {
            switch (peek()) {
                case ']' -> {
                    ++mOffset;
                    int n = mStack.size() - depth;
                    RegexNode[] clazz = new RegexNode[n];
                    for (int i = 0; i < n; i++) {
                        clazz[i] = pop();
                    }
                    push(RegexNode.CharClass(clazz, negative));
                    return;
                }
                case EOF -> throw new IllegalStateException("unterminated character class");
                default -> item();
            }
        }
    }
    // @formatter:on

    private void regex() {
        sequence();
        switch (peek()) {
            case '|': {
                ++mOffset;
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
