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

package icyllis.arcticgi.sksl;

import icyllis.arcticgi.sksl.lex.*;

import javax.annotation.Nonnull;

/**
 * Parsing domain-specific language, consumes SkSL text and invokes DSL functions to instantiate the program.
 */
public class DSLParser {

    public static final byte
            LAYOUT_TOKEN_LOCATION = 0,
            LAYOUT_TOKEN_OFFSET = 1,
            LAYOUT_TOKEN_BINDING = 2,
            LAYOUT_TOKEN_INDEX = 3,
            LAYOUT_TOKEN_SET = 4,
            LAYOUT_TOKEN_BUILTIN = 5,
            LAYOUT_TOKEN_INPUT_ATTACHMENT_INDEX = 6,
            LAYOUT_TOKEN_ORIGIN_UPPER_LEFT = 7,
            LAYOUT_TOKEN_BLEND_SUPPORT_ALL_EQUATIONS = 8,
            LAYOUT_TOKEN_PUSH_CONSTANT = 9,
            LAYOUT_TOKEN_COLOR = 10;

    private final Compiler mCompiler;
    private final ProgramSettings mSettings;
    private boolean mEncounteredFatalError;
    private final byte mKind;
    private final String mText;

    private int mLexerToken;
    private int mLexerOffset;
    private int mLexerLength;

    // current parse depth, used to enforce a recursion limit to try to keep us from overflowing the
    // stack on pathological inputs
    private int mDepth;
    private boolean mPushback;

    public DSLParser(Compiler compiler, ProgramSettings settings, byte kind, String text) {
        mCompiler = compiler;
        mSettings = settings;
        mKind = kind;
        mText = text;
    }

    /**
     * Return the next token, including whitespace tokens, from the parse stream.
     */
    private int nextRawToken() {
        int token;
        if (mPushback) {
            // Retrieve the token from the pushback buffer.
            token = mLexerToken;
            mPushback = false;
        } else {
            // Fetch a token from the lexer.

            // note that we cheat here: normally a lexer needs to worry about the case
            // where a token has a prefix which is not itself a valid token - for instance,
            // maybe we have a valid token 'while', but 'w', 'wh', etc. are not valid
            // tokens. Our grammar doesn't have this property, so we can simplify the logic
            // a bit.
            int startOffset = mLexerOffset;
            int state = 1;
            boolean eof = false;
            for (; ; ) {
                if (mLexerOffset >= mText.length()) {
                    if (startOffset == mText.length() || Lexer.ACCEPTS[state] == DFA.INVALID) {
                        eof = true;
                    }
                    break;
                }
                int c = (mText.charAt(mLexerOffset) - NFAtoDFA.START_CHAR);
                if (c >= NFAtoDFA.END_CHAR - NFAtoDFA.START_CHAR) {
                    c = Lexer.INVALID_CHAR;
                }
                int newState = Lexer.getTransition(Lexer.MAPPINGS[c], state);
                if (newState == 0) {
                    break;
                }
                state = newState;
                ++mLexerOffset;
            }
            if (eof) {
                mLexerOffset = startOffset;
                mLexerLength = 0;
                token = Lexer.TK_END_OF_FILE;
            } else {
                mLexerLength = mLexerOffset - startOffset;
                token = Lexer.ACCEPTS[state];
            }

            // Some tokens are always invalid, so we detect and report them here.
            switch (token) {
                case Lexer.TK_RESERVED -> {
                    error(startOffset, mLexerOffset,
                            "'" + mText.substring(startOffset, mLexerOffset) + "' is a reserved word");
                    token = Lexer.TK_IDENTIFIER;  // reduces additional follow-up errors
                }
                case Lexer.TK_BAD_OCTAL -> error(startOffset, mLexerOffset,
                        "'" + mText.substring(startOffset, mLexerOffset) + "' is not a valid octal number");
            }

            mLexerToken = token;
        }

        return token;
    }

    private static boolean isWhitespace(int token) {
        return switch (token) {
            case Lexer.TK_WHITESPACE, Lexer.TK_LINE_COMMENT, Lexer.TK_BLOCK_COMMENT -> true;
            default -> false;
        };
    }

    /**
     * Return the next non-whitespace token from the parse stream.
     */
    private int nextToken() {
        for (; ; ) {
            int token = nextRawToken();
            if (!isWhitespace(token)) {
                return token;
            }
        }
    }

    /**
     * Push a token back onto the parse stream, so that it is the next one read. Only a single level
     * of pushback is supported (that is, it is an error to call pushback() twice in a row without
     * an intervening nextToken()).
     */
    private void pushback(int token) {
        assert !mPushback && token == mLexerToken;
        mPushback = true;
    }

    /**
     * Returns the next non-whitespace token without consuming it from the stream.
     */
    private int peek() {
        if (!mPushback) {
            mPushback = true;
            int token = nextToken();
            assert token == mLexerToken;
            return token;
        }
        return mLexerToken;
    }

    /**
     * Checks to see if the next token is of the specified type. If so, stores it in result (if
     * result is non-null) and returns true. Otherwise, pushes it back and returns false.
     */
    private boolean checkNext(int token) {
        if (mPushback && token != mLexerToken) {
            return false;
        }
        int next = nextToken();
        if (next == token) {
            return true;
        }
        pushback(token);
        return false;
    }

    /**
     * Reads the next non-whitespace token and generates an error if it is not the expected type.
     * The 'expected' string is part of the error message, which reads:
     * <p>
     * "expected <expected>, but found '<actual text>'"
     * <p>
     * If 'result' is non-null, it is set to point to the token that was read.
     * Returns true if the read token was as expected, false otherwise.
     */
    private boolean expect(int token, String expected) {
        int next = nextToken();
        if (next == token) {
            return true;
        } else {
            error(next, "expected " + expected + ", but found '" + text(next) + "'");
            mEncounteredFatalError = true;
            return false;
        }
    }

    /**
     * If the next token is a newline, consumes it and returns true. If not, returns false.
     */
    private boolean expectNewline() {
        int token = nextRawToken();
        if (token == Lexer.TK_WHITESPACE) {
            // The lexer doesn't distinguish newlines from other forms of whitespace, so we check
            // for newlines by searching through the token text.
            String tokenText = text(token);
            if (tokenText.indexOf('\r') != -1 ||
                    tokenText.indexOf('\n') != -1) {
                return true;
            }
        }
        // We didn't find a newline.
        pushback(token);
        return false;
    }

    @Nonnull
    private String text(int token) {
        assert token == mLexerToken;
        return mText.substring(mLexerOffset - mLexerLength, mLexerOffset);
    }

    private void error(int token, String msg) {
        assert token == mLexerToken;
        error(mLexerOffset - mLexerLength, mLexerOffset, msg);
    }

    private void error(int start, int end, String msg) {
        //TODO
    }
}
