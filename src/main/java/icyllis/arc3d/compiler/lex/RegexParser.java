/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.lex;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Turns a simple regular expression into a parse tree. The regular expression syntax supports only
 * the basic quantifiers ('*', '+', and '?'), alternation ('|'), character classes ('[a-z]'), and
 * groups ('()').
 */
public final class RegexParser {

    private static final char EOF = '\0';

    private final Deque<RegexNode> mStack = new ArrayDeque<>();

    private String mSource;
    private int mOffset;

    public RegexParser() {
    }

    @NonNull
    public RegexNode parse(@NonNull String source) {
        mSource = source;
        mOffset = 0;
        assert (mStack.size() == 0);
        regex();
        assert (mStack.size() == 1);
        assert (mOffset == source.length());
        mSource = null;
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

    private void push(@NonNull RegexNode node) {
        mStack.push(node);
    }

    @NonNull
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
                case EOF, '|', ')' -> {
                    return;
                }
                default -> {
                    sequence();
                    RegexNode y = pop();
                    RegexNode x = pop();
                    push(RegexNode.Concat(x, y));
                }
            }
        }
    }
    // @formatter:on

    private char octal() {
        ++mOffset;
        int n = peek();
        if (((n - '0') | ('7' - n)) >= 0) {
            ++mOffset;
            int m = peek();
            if (((m - '0') | ('7' - m)) >= 0) {
                ++mOffset;
                int o = peek();
                if ((((o - '0') | ('7' - o)) >= 0) && (((n - '0') | ('3' - n)) >= 0)) {
                    return (char) ((n - '0') * 64 + (m - '0') * 8 + (o - '0'));
                }
                --mOffset;
                return (char) ((n - '0') * 8 + (m - '0'));
            }
            --mOffset;
            return (char) (n - '0');
        }
        throw new IllegalStateException("bad octal");
    }

    /**
     * Returns a node representing the given escape character (e.g. escape('n') returns a
     * node which matches a line feed character).
     */
    private RegexNode escape(char c) {
        return switch (c) {
            case '0' -> RegexNode.Char(octal());
            case 't' -> RegexNode.Char('\t'); // HT
            case 'n' -> RegexNode.Char('\n'); // LF
            case 'r' -> RegexNode.Char('\r'); // CR
            case 's' -> RegexNode.CharClass(
                    RegexNode.Char('\011'), // HT
                    RegexNode.Char('\012'), // LF
                    RegexNode.Char('\013'), // VT
                    RegexNode.Char('\014'), // FF
                    RegexNode.Char('\015'), // CR
                    RegexNode.Char(' '));
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
        final int depth = mStack.size();
        final boolean exclusive;
        if (peek() == '^') {
            ++mOffset;
            exclusive = true;
        } else {
            exclusive = false;
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
                    push(RegexNode.CharClass(clazz, exclusive));
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
            case '|' -> {
                ++mOffset;
                regex();
                RegexNode y = pop();
                RegexNode x = pop();
                push(RegexNode.Union(x, y));
            }
            case EOF, ')' -> {
            }
            default -> throw new IllegalStateException();
        }
    }
}
