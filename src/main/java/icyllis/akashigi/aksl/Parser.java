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

package icyllis.akashigi.aksl;

import icyllis.akashigi.aksl.ir.*;
import icyllis.akashigi.aksl.lex.Lexer;
import icyllis.akashigi.aksl.lex.NFAtoDFA;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Consumes AkSL text and invokes DSL functions to instantiate the program.
 */
public class Parser {

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
    private final ModuleSettings mSettings;
    private final int mKind;

    // current parse stream
    private final String mText;
    private int mScanOffset;
    private long mScanToken;

    // current parse depth, used to enforce a recursion limit to try to keep us from overflowing the
    // stack on pathological inputs
    private int mDepth = 0;
    private long mPushback = -1;

    public Parser(Compiler compiler, ModuleSettings settings, int kind, String text) {
        // ideally we can break long text into pieces, but shader code should not be too long
        if (text.length() > 0x7FFFFE) {
            throw new IllegalArgumentException("Source code is too long, " + text.length() + " > 8,388,606");
        }
        mCompiler = Objects.requireNonNull(compiler);
        mSettings = Objects.requireNonNull(settings);
        mKind = kind;
        mText = text;
        mScanOffset = 0;
        mScanToken = -1;
    }

    /**
     * Return the next token, including whitespace tokens, from the parse stream.
     */
    private long nextRawToken() {
        final long token;
        if (mPushback != -1) {
            // Retrieve the token from the pushback buffer.
            assert (mPushback == mScanToken);
            token = mPushback;
            mPushback = -1;
        } else {
            // Fetch a token from the lexer.
            // Note that we cheat here: normally a lexer needs to worry about the case
            // where a token has a prefix which is not itself a valid token - for instance,
            // maybe we have a valid token 'while', but 'w', 'wh', etc. are not valid
            // tokens. Our grammar doesn't have this property, so we can simplify the logic
            // a bit.
            final int startOffset = mScanOffset;
            int state = 1;
            boolean eof = false;
            for (; ; ) {
                if (mScanOffset >= mText.length()) {
                    if (startOffset == mText.length() || Lexer.ACCEPTS[state] == Lexer.TK_NONE) {
                        eof = true;
                    }
                    break;
                }
                int c = (mText.charAt(mScanOffset) - NFAtoDFA.START_CHAR);
                if (c < 0 || c > NFAtoDFA.END_CHAR - NFAtoDFA.START_CHAR) {
                    // Choose '\e' as invalid char which is greater than start char,
                    // and should not appear in actual input.
                    c = 27 - NFAtoDFA.START_CHAR;
                }
                int newState = Lexer.getTransition(Lexer.MAPPINGS[c], state);
                if (newState == 0) {
                    break;
                }
                state = newState;
                ++mScanOffset;
            }
            final int endOffset;
            int kind;
            if (eof) {
                mScanOffset = startOffset;
                endOffset = startOffset;
                kind = Lexer.TK_END_OF_FILE;
            } else {
                endOffset = mScanOffset;
                kind = Lexer.ACCEPTS[state];

                // Some tokens are always invalid, so we detect and report them here.
                switch (kind) {
                    case Lexer.TK_RESERVED -> {
                        error(startOffset, mScanOffset,
                                "'" + mText.substring(startOffset, endOffset) + "' is a reserved keyword");
                        kind = Lexer.TK_IDENTIFIER;  // reduces additional follow-up errors
                    }
                    case Lexer.TK_BAD_OCTAL -> error(startOffset, endOffset,
                            "'" + mText.substring(startOffset, endOffset) + "' is not a valid octal number");
                }
            }
            // encode token state, we've checked text length in the constructor
            // 0-16 bits: token kind
            // 16-40 bits: start offset
            // 40-64 bits: end offset
            token = (kind & 0xFFFF) | (long) startOffset << 16 | (long) endOffset << 40;
            assert (token != -1);
            mScanToken = token;
        }

        return token;
    }

    private static int kind(long token) {
        return (int) (token & 0xFFFF);
    }

    private static boolean isWhitespace(int kind) {
        return switch (kind) {
            case Lexer.TK_WHITESPACE, Lexer.TK_LINE_COMMENT, Lexer.TK_BLOCK_COMMENT -> true;
            default -> false;
        };
    }

    /**
     * Return the next non-whitespace token from the parse stream.
     */
    private long nextToken() {
        for (; ; ) {
            long token = nextRawToken();
            if (!isWhitespace(kind(token))) {
                return token;
            }
        }
    }

    /**
     * Push a token back onto the parse stream, so that it is the next one read. Only a single level
     * of pushback is supported (that is, it is an error to call pushback() twice in a row without
     * an intervening nextToken()).
     */
    private void pushback(long token) {
        if (mPushback != -1) {
            throw new IllegalStateException();
        }
        assert (token == mScanToken);
        mPushback = token;
    }

    /**
     * Returns the next non-whitespace token without consuming it from the stream.
     */
    private long peek() {
        if (mPushback == -1) {
            long token = nextToken();
            assert (token == mScanToken);
            mPushback = token;
            return token;
        }
        assert (mPushback == mScanToken);
        return mPushback;
    }

    @Nonnull
    private String text(long token) {
        assert (token == mScanToken) : "current stream";
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        return mText.substring(start, end);
    }

    // see Node.mPosition
    private int position(long token) {
        assert (token == mScanToken) : "current stream";
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        return Node.range(start, end);
    }

    private void error(long token, String msg) {
        assert (token == mScanToken) : "current stream";
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        error(start, end, msg);
    }

    private void warning(long token, String msg) {
        assert (token == mScanToken) : "current stream";
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        warning(start, end, msg);
    }

    private void error(int start, int end, String msg) {
        ThreadContext.getInstance().getErrorHandler().error(start, end, msg);
    }

    private void warning(int start, int end, String msg) {
        ThreadContext.getInstance().getErrorHandler().warning(start, end, msg);
    }

    /**
     * Checks to see if the next token is of the specified type. If so, stores it in result (if
     * result is non-null) and returns true. Otherwise, pushes it back and returns false.
     */
    private boolean checkNext(int kind) {
        if (mPushback != -1 && kind(mPushback) != kind) {
            return false;
        }
        long next = peek();
        if (kind(next) == kind) {
            assert (next == mScanToken);
            return true;
        }
        return false;
    }

    /**
     * Behaves like checkNext(TK_IDENTIFIER), but also verifies that identifier is not a builtIn
     * type. If the token was actually a builtIn type, false is returned (the next token is not
     * considered to be an identifier).
     */
    private boolean checkIdentifier() {
        if (!checkNext(Lexer.TK_IDENTIFIER)) {
            return false;
        }
        long token = peek();
        if (ThreadContext.getInstance().getSymbolTable().isBuiltInType(text(token))) {
            return false;
        }
        assert (token == mScanToken);
        return true;
    }

    /**
     * Reads the next non-whitespace token and generates an error if it is not the expected type.
     * The 'expected' string is part of the error message, which reads:
     * <p>
     * "expected [expected], but found '[actual text]'"
     */
    private long expect(int kind, String expected) {
        long next = nextToken();
        if (kind(next) != kind) {
            error(next, "expected " + expected + ", but found '" +
                    text(next) + "'");
            throw new IllegalStateException();
        }
        return next;
    }

    /**
     * Behaves like expect(TK_IDENTIFIER), but also verifies that identifier is not a type.
     * If the token was actually a type, generates an error message of the form:
     * <p>
     * "expected an identifier, but found type 'float2'"
     */
    private long expectIdentifier() {
        long token = expect(Lexer.TK_IDENTIFIER, "an identifier");
        if (ThreadContext.getInstance().getSymbolTable().isBuiltInType(text(token))) {
            error(token, "expected an identifier, but found type '" +
                    text(token) + "'");
            throw new IllegalStateException();
        }
        return token;
    }

    /**
     * If the next token is a newline, consumes it and returns true. If not, returns false.
     */
    private boolean expectNewline() {
        long token = nextRawToken();
        if (kind(token) == Lexer.TK_WHITESPACE) {
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

    /**
     * INT_LITERAL
     */
    @Nullable
    private Literal intLiteral() {
        long token = expect(Lexer.TK_INT_LITERAL, "integer literal");
        String s = text(token);
        if (s.endsWith("u") || s.endsWith("U")) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            int value = Integer.decode(s);
            return Literal.makeInt(
                    ThreadContext.getInstance(),
                    position(token),
                    value);
        } catch (NumberFormatException e) {
            error(token, "invalid integer value: " + e.getMessage());
            return null;
        }
    }

    /**
     * FLOAT_LITERAL
     */
    @Nullable
    private Literal floatLiteral() {
        long token = expect(Lexer.TK_FLOAT_LITERAL, "float literal");
        String s = text(token);
        try {
            float value = Float.parseFloat(s);
            return Literal.makeFloat(
                    ThreadContext.getInstance(),
                    position(token),
                    value);
        } catch (NumberFormatException e) {
            error(token, "invalid floating-point value: " + e.getMessage());
            return null;
        }
    }

    /**
     * TRUE_LITERAL | FALSE_LITERAL
     */
    @Nullable
    private Literal boolLiteral() {
        long token = nextToken();
        return switch (kind(token)) {
            case Lexer.TK_TRUE_LITERAL -> Literal.makeBoolean(
                    ThreadContext.getInstance(),
                    position(token),
                    true);
            case Lexer.TK_FALSE_LITERAL -> Literal.makeBoolean(
                    ThreadContext.getInstance(),
                    position(token),
                    false);
            default -> {
                error(token, "expected 'true' or 'false', but found '" +
                        text(token) + "'");
                yield null;
            }
        };
    }

    /**
     * IDENTIFIER
     */
    @Nonnull
    private String identifier() {
        long token = expect(Lexer.TK_IDENTIFIER, "identifier");
        return text(token);
    }

    private Expression primaryExpression() {
        long t = peek();
        switch (kind(t)) {
            case Lexer.TK_IDENTIFIER ->{}
        }
        return null;
    }

    /**
     * LAYOUT LPAREN IDENTIFIER (EQ INT_LITERAL)? (COMMA IDENTIFIER (EQ INT_LITERAL)?)* RPAREN
     */
    private Layout layout() {
        return Layout.empty();
    }
}
