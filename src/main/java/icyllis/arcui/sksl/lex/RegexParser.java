/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl.lex;

import java.util.ArrayDeque;

/**
 * Turns a simple regular expression into a parse tree. The regular expression syntax supports only
 * the basic quantifiers ('*', '+', and '?'), alternation ('|'), character sets ('[a-z]'), and
 * groups ('()').
 */
public class RegexParser {

    private static final char END = '\0';

    private String mSource;
    private int mIndex;
    private final ArrayDeque<RegexNode> mStack = new ArrayDeque<>();

    public RegexNode parse(String source) {
        mSource = source;
        mIndex = 0;
        assert (mStack.isEmpty());
        regex();
        assert (mStack.size() == 1);
        assert (mIndex == source.length());
        return pop();
    }

    private char peek() {
        if (mIndex >= mSource.length()) {
            return END;
        }
        return mSource.charAt(mIndex);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalStateException(
                    String.format("expected '%c' at index %d, but found '%c'", c, mIndex, peek()));
        }
        ++mIndex;
    }

    private RegexNode pop() {
        return mStack.pop();
    }

    /**
     * Matches a char literal, parenthesized group, character set, or dot ('.').
     */
    private void term() {
        switch (peek()) {
            case '(' -> group();
            case '[' -> set();
            case '.' -> dot();
            default -> literal();
        }
    }

    /**
     * Matches a term followed by an optional quantifier ('*', '+', or '?').
     */
    private void quantifiedTerm() {
        term();
        switch (peek()) {
            case '*' -> {
                mStack.push(new RegexNode(RegexNode.Kind_Star, pop()));
                ++mIndex;
            }
            case '+' -> {
                mStack.push(new RegexNode(RegexNode.Kind_Plus, pop()));
                ++mIndex;
            }
            case '?' -> {
                mStack.push(new RegexNode(RegexNode.Kind_Question, pop()));
                ++mIndex;
            }
        }
    }

    /**
     * Matches a sequence of quantifiedTerms.
     */
    private void sequence() {
        quantifiedTerm();
        for (; ; ) {
            switch (peek()) {
                case END: // fall through
                case '|': // fall through
                case ')':
                    return;
                default:
                    sequence();
                    RegexNode right = pop();
                    RegexNode left = pop();
                    mStack.push(new RegexNode(RegexNode.Kind_Concat, left, right));
                    break;
            }
        }
    }

    /**
     * Returns a node representing the given escape character (e.g. escapeSequence('n') returns a
     * node which matches a newline character).
     */
    private RegexNode escapeSequence(char c) {
        return switch (c) {
            case 'n' -> new RegexNode(RegexNode.Kind_Char, '\n');
            case 'r' -> new RegexNode(RegexNode.Kind_Char, '\r');
            case 't' -> new RegexNode(RegexNode.Kind_Char, '\t');
            case 's' -> new RegexNode(RegexNode.Kind_Charset, " \t\n\r");
            default -> new RegexNode(RegexNode.Kind_Char, c);
        };
    }

    /**
     * Matches a literal character or escape sequence.
     */
    private void literal() {
        char c = peek();
        if (c == '\\') {
            ++mIndex;
            mStack.push(escapeSequence(peek()));
        } else {
            mStack.push(new RegexNode(RegexNode.Kind_Char, c));
        }
        ++mIndex;
    }

    /**
     * Matches a dot ('.').
     */
    private void dot() {
        expect('.');
        mStack.push(new RegexNode(RegexNode.Kind_Dot));
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
     * Matches a literal character, escape sequence, or character range from a character set.
     */
    private void setItem() {
        literal();
        if (peek() == '-') {
            ++mIndex;
            if (peek() == ']') {
                mStack.push(new RegexNode(RegexNode.Kind_Char, '-'));
            } else {
                literal();
                RegexNode end = pop();
                assert (end.mKind == RegexNode.Kind_Char);
                RegexNode start = pop();
                assert (start.mKind == RegexNode.Kind_Char);
                mStack.push(new RegexNode(RegexNode.Kind_Range, start, end));
            }
        }
    }

    /**
     * Matches a character set.
     */
    private void set() {
        expect('[');
        int depth = mStack.size();
        RegexNode set = new RegexNode(RegexNode.Kind_Charset);
        if (peek() == '^') {
            ++mIndex;
            set.mPayload = 1;
        } else {
            set.mPayload = 0;
        }
        for (; ; ) {
            switch (peek()) {
                case ']' -> {
                    ++mIndex;
                    while (mStack.size() > depth) {
                        set.mChildren.add(pop());
                    }
                    mStack.push(set);
                    return;
                }
                case END -> throw new IllegalStateException("unterminated character set");
                default -> setItem();
            }
        }
    }

    private void regex() {
        sequence();
        switch (peek()) {
            case '|': {
                ++mIndex;
                regex();
                RegexNode right = pop();
                RegexNode left = pop();
                mStack.push(new RegexNode(RegexNode.Kind_Or, left, right));
                break;
            }
            case END: // fall through
            case ')':
                return;
            default:
                throw new IllegalStateException();
        }
    }
}
