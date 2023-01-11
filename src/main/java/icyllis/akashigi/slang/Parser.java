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

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.ir.*;
import icyllis.akashigi.slang.lex.Lexer;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

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

    private final ModuleKind mKind;
    private final ModuleOptions mOptions;

    private final String mSource;
    private final Lexer mLexer;

    // current parse depth, used to enforce a recursion limit to try to keep us from overflowing the
    // stack on pathological inputs
    private int mDepth = 0;
    private final LongArrayList mPushback = new LongArrayList();

    public Parser(Compiler compiler, ModuleKind kind, ModuleOptions options, String source) {
        // ideally we can break long text into pieces, but shader code should not be too long
        if (source.length() > 0x7FFFFE) {
            throw new IllegalArgumentException("Source code is too long, " + source.length() + " > 8,388,606");
        }
        mCompiler = Objects.requireNonNull(compiler);
        mOptions = Objects.requireNonNull(options);
        mKind = Objects.requireNonNull(kind);
        mSource = source;
        mLexer = new Lexer(source);
    }

    @Nullable
    public Program parse(Module parent) {
        Objects.requireNonNull(parent);
        ErrorHandler errorHandler = mCompiler.getErrorHandler();
        DSL.start(mKind, mOptions, parent);
        DSL.setErrorHandler(errorHandler);
        errorHandler.setSource(mSource);
        //TODO declarations
        errorHandler.setSource(null);
        DSL.end();
        return null;
    }

    @Nullable
    public Module parseModule(Module parent) {
        Objects.requireNonNull(parent);
        ErrorHandler errorHandler = mCompiler.getErrorHandler();
        DSL.startModule(mKind, mOptions, parent);
        DSL.setErrorHandler(errorHandler);
        errorHandler.setSource(mSource);
        //TODO declarations
        final Module result;
        if (DSL.getErrorHandler().getErrorCount() == 0) {
            result = new Module();
            result.mParent = parent;
            result.mSymbols = ThreadContext.getInstance().getSymbolTable();
            result.mElements = ThreadContext.getInstance().getUniqueElements();
        } else {
            result = null;
        }
        errorHandler.setSource(null);
        DSL.end();
        return result;
    }

    /**
     * Return the next token, including whitespace tokens, from the parse stream.
     */
    private long nextRawToken() {
        final long token;
        if (!mPushback.isEmpty()) {
            // Retrieve the token from the pushback buffer.
            token = mPushback.removeLong(0);
        } else {
            // Fetch a token from the lexer.
            token = mLexer.next();

            if (kind(token) == Lexer.TK_RESERVED) {
                error(token, "'" + text(token) + "' is a reserved keyword");
                // reduces additional follow-up errors
                return (token & ~0xFFFF) | Lexer.TK_IDENTIFIER;
            }
        }
        return token;
    }

    private static int kind(long token) {
        return (int) (token & 0xFFFF);
    }

    // @formatter:off
    private static boolean isWhitespace(int kind) {
        return switch (kind) {
            case Lexer.TK_WHITESPACE,
                    Lexer.TK_LINE_COMMENT,
                    Lexer.TK_BLOCK_COMMENT -> true;
            default -> false;
        };
    }
    // @formatter:on

    /**
     * Return the next non-whitespace token from the parse stream.
     */
    // @formatter:off
    private long nextToken() {
        for (;;) {
            long token = nextRawToken();
            if (!isWhitespace(kind(token))) {
                return token;
            }
        }
    }
    // @formatter:on

    /**
     * Push a token back onto the parse stream, so that it is the next one read. Only a single level
     * of pushback is supported (that is, it is an error to call pushback() twice in a row without
     * an intervening nextToken()).
     */
    private void pushback(long token) {
        mPushback.add(token);
    }

    /**
     * Returns the next non-whitespace token without consuming it from the stream.
     */
    private long peek() {
        if (mPushback.isEmpty()) {
            long token = nextToken();
            mPushback.add(token);
            return token;
        }
        return mPushback.getLong(0);
    }

    @Nonnull
    private String text(long token) {
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        return mSource.substring(start, end);
    }

    // see Node.mPosition
    private int position(long token) {
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        return Node.makeRange(start, end);
    }

    private void error(long token, String msg) {
        int start = (int) (token >> 16) & 0xFFFFFF;
        int end = (int) (token >>> 40);
        error(start, end, msg);
    }

    private void error(int start, int end, String msg) {
        ThreadContext.getInstance().error(start, end, msg);
    }

    /**
     * Checks to see if the next token is of the specified type. If so, stores it in result (if
     * result is non-null) and returns true. Otherwise, pushes it back and returns -1.
     */
    private long checkNext(int kind) {
        long next = peek();
        if (kind(next) == kind) {
            return nextToken();
        }
        return -1;
    }

    /**
     * Behaves like checkNext(TK_IDENTIFIER), but also verifies that identifier is not a builtin
     * type. If the token was actually a builtin type, false is returned (the next token is not
     * considered to be an identifier).
     */
    private long checkIdentifier() {
        long result = checkNext(Lexer.TK_IDENTIFIER);
        if (result == -1) {
            return -1;
        }
        if (ThreadContext.getInstance().getSymbolTable().isBuiltinType(text(result))) {
            pushback(result);
            return -1;
        }
        return result;
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
        if (ThreadContext.getInstance().getSymbolTable().isBuiltinType(text(token))) {
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

    private boolean declaration() {
        long token = peek();
        if (kind(token) == Lexer.TK_SEMICOLON) {
            nextToken();
            error(token, "expected a declaration, but found ';'");
            return false;
        }

        return false;
    }

    /**
     * LAYOUT LPAREN IDENTIFIER (EQ INT_LITERAL)? (COMMA IDENTIFIER (EQ INT_LITERAL)?)* RPAREN
     */
    private Layout layout() {
        if (checkNext(Lexer.TK_LAYOUT) == -1) {
            return null;
        }
        expect(Lexer.TK_LPAREN, "'('");
        int flags = 0;
        int location = 0;
        int component = 0;
        int index = 0;
        int binding = 0;
        int offset = 0;
        int align = 0;
        int set = 0;
        int inputAttachmentIndex = 0;
        int builtin = 0;
        for (;;) {
            long t = nextToken();
            String text = text(t);
            int pos = position(t);
            switch (text) {
                case "origin_upper_left" -> flags = Layout.flag(flags, Layout.kOriginUpperLeft_Flag, text, pos);
                case "pixel_center_integer" -> flags = Layout.flag(flags, Layout.kPixelCenterInteger_Flag, text, pos);
                case "early_fragment_tests" -> flags = Layout.flag(flags, Layout.kEarlyFragmentTests_Flag, text, pos);
                case "blend_support_all_equations" ->
                        flags = Layout.flag(flags, Layout.kBlendSupportAllEquations_Flag, text, pos);
                case "push_constant" -> flags = Layout.flag(flags, Layout.kPushConstant_Flag, text, pos);
            }
        }
    }

    private int layoutInt() {
        expect(Lexer.TK_EQ, "'='");

        return -1;
    }

    @Nullable
    private Expression operatorRight(Expression left, Operator op, Supplier<Expression> rightFn) {
        Expression right = rightFn.get();
        if (right == null) {
            return null;
        }
        int pos = Node.makeRange(left.getStartOffset(), right.getEndOffset());
        return BinaryExpression.convert(pos, left, op, right);
    }

    /**
     * assignmentExpression (COMMA assignmentExpression)*
     */
    @Nullable
    private Expression expression() {
        Expression result = assignmentExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_COMMA) {
            if ((result = operatorRight(result, Operator.COMMA, this::assignmentExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * conditional_expression (assignment_operator assignment_expression)*
     */
    // @formatter:off
    @Nullable
    private Expression assignmentExpression() {
        Expression result = conditionalExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (kind(peek())) {
                case Lexer.TK_EQ        -> Operator.    ASSIGN;
                case Lexer.TK_PLUSEQ    -> Operator.ADD_ASSIGN;
                case Lexer.TK_MINUSEQ   -> Operator.SUB_ASSIGN;
                case Lexer.TK_STAREQ    -> Operator.MUL_ASSIGN;
                case Lexer.TK_SLASHEQ   -> Operator.DIV_ASSIGN;
                case Lexer.TK_PERCENTEQ -> Operator.MOD_ASSIGN;
                case Lexer.TK_LTLTEQ    -> Operator.SHL_ASSIGN;
                case Lexer.TK_GTGTEQ    -> Operator.SHR_ASSIGN;
                case Lexer.TK_AMPEQ     -> Operator.AND_ASSIGN;
                case Lexer.TK_PIPEEQ    -> Operator. OR_ASSIGN;
                case Lexer.TK_CARETEQ   -> Operator.XOR_ASSIGN;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op, this::assignmentExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    /**
     * logicalOrExpression ('?' expression ':' assignmentExpression)?
     */
    @Nullable
    private Expression conditionalExpression() {
        Expression base = logicalOrExpression();
        if (base == null) {
            return null;
        }
        if (checkNext(Lexer.TK_QUES) == -1) {
            return base;
        }
        Expression trueExpr = expression();
        if (trueExpr == null) {
            return null;
        }
        expect(Lexer.TK_COLON, "':'");
        Expression falseExpr = assignmentExpression();
        if (falseExpr == null) {
            return null;
        }
        int pos = Node.makeRange(base.getStartOffset(), falseExpr.getEndOffset());
        return ConditionalExpression.convert(pos, base, trueExpr, falseExpr);
    }

    /**
     * logicalXorExpression ('||' logicalXorExpression)*
     */
    @Nullable
    private Expression logicalOrExpression() {
        Expression result = logicalXorExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_PIPEPIPE) {
            if ((result = operatorRight(result, Operator.LOGICAL_OR, this::logicalXorExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * logicalAndExpression ('^^' logicalAndExpression)*
     */
    @Nullable
    private Expression logicalXorExpression() {
        Expression result = logicalAndExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_CARETCARET) {
            if ((result = operatorRight(result, Operator.LOGICAL_XOR, this::logicalAndExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * bitwiseOrExpression ('&&' bitwiseOrExpression)*
     */
    @Nullable
    private Expression logicalAndExpression() {
        Expression result = bitwiseOrExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_AMPAMP) {
            if ((result = operatorRight(result, Operator.LOGICAL_AND, this::bitwiseOrExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * bitwiseXorExpression ('|' bitwiseXorExpression)*
     */
    @Nullable
    private Expression bitwiseOrExpression() {
        Expression result = bitwiseXorExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_PIPE) {
            if ((result = operatorRight(result, Operator.BITWISE_OR, this::bitwiseXorExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * bitwiseAndExpression ('^' bitwiseAndExpression)*
     */
    @Nullable
    private Expression bitwiseXorExpression() {
        Expression result = bitwiseAndExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_CARET) {
            if ((result = operatorRight(result, Operator.BITWISE_XOR, this::bitwiseAndExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * equalityExpression ('&' equalityExpression)*
     */
    @Nullable
    private Expression bitwiseAndExpression() {
        Expression result = equalityExpression();
        if (result == null) {
            return null;
        }
        while (kind(peek()) == Lexer.TK_AMP) {
            if ((result = operatorRight(result, Operator.BITWISE_AND, this::equalityExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * relationalExpression (('==' | '!=') relationalExpression)*
     */
    @Nullable
    private Expression equalityExpression() {
        Expression result = relationalExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (kind(peek())) {
                case Lexer.TK_EQEQ -> Operator.EQ;
                case Lexer.TK_BANGEQ -> Operator.NE;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op, this::relationalExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }

    /**
     * shiftExpression (('&lt' | '&gt' | '&lt=' | '&gt=') shiftExpression)*
     */
    @Nullable
    private Expression relationalExpression() {
        Expression result = shiftExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (kind(peek())) {
                case Lexer.TK_LT -> Operator.LT;
                case Lexer.TK_GT -> Operator.GT;
                case Lexer.TK_LTEQ -> Operator.LE;
                case Lexer.TK_GTEQ -> Operator.GE;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op, this::shiftExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }

    /**
     * additiveExpression (('&lt&lt' | '&gt&gt') additiveExpression)*
     */
    @Nullable
    private Expression shiftExpression() {
        Expression result = additiveExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (kind(peek())) {
                case Lexer.TK_LTLT -> Operator.SHL;
                case Lexer.TK_GTGT -> Operator.SHR;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op, this::additiveExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }

    /**
     * multiplicativeExpression (('+' | '-') multiplicativeExpression)*
     */
    @Nullable
    private Expression additiveExpression() {
        Expression result = multiplicativeExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (kind(peek())) {
                case Lexer.TK_PLUS -> Operator.ADD;
                case Lexer.TK_MINUS -> Operator.SUB;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op, this::multiplicativeExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }

    /**
     * unaryExpression (('*' | '/' | '%') unaryExpression)*
     */
    @Nullable
    private Expression multiplicativeExpression() {
        Expression result = unaryExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (kind(peek())) {
                case Lexer.TK_STAR -> Operator.MUL;
                case Lexer.TK_SLASH -> Operator.DIV;
                case Lexer.TK_PERCENT -> Operator.MOD;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op, this::unaryExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }

    /**
     * postfix_expression | (PLUSPLUS | MINUSMINUS | PLUS | MINUS | BANG | TILDE) unary_expression
     */
    @Nullable
    private Expression unaryExpression() {
        long start = peek();
        Operator op = switch (kind(start)) {
            case Lexer.TK_PLUSPLUS -> Operator.INC;
            case Lexer.TK_MINUSMINUS -> Operator.DEC;
            case Lexer.TK_PLUS -> Operator.ADD;
            case Lexer.TK_MINUS -> Operator.SUB;
            case Lexer.TK_BANG -> Operator.LOGICAL_NOT;
            case Lexer.TK_TILDE -> Operator.BITWISE_NOT;
            default -> null;
        };
        if (op != null) {
            nextToken();
            Expression base = unaryExpression();
            if (base == null) {
                return null;
            }
            int position = (int) (start >> 16) & 0xFFFFFF;
            position = Node.makeRange(position, base.getEndOffset());
            return PrefixExpression.convert(position, op, base);
        }
        return postfixExpression();
    }

    /**
     * primary_expression (LBRACKET expression? RBRACKET | DOT IDENTIFIER | LPAREN arguments RPAREN |
     * PLUSPLUS | MINUSMINUS | COLONCOLON IDENTIFIER)?
     */
    @Nullable
    private Expression postfixExpression() {
        Expression result = primaryExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            long t = peek();
            switch (kind(t)) {
                case Lexer.TK_LBRACKET:
                case Lexer.TK_DOT:
                case Lexer.TK_LPAREN:
                case Lexer.TK_PLUSPLUS:
                case Lexer.TK_MINUSMINUS:
                    // ..
                    break;
                default:
                    return result;
            }
        }
    }

    /**
     * IDENTIFIER | INTLITERAL | FLOATLITERAL | BOOLEANLITERAL | '(' expression ')'
     */
    @Nullable
    private Expression primaryExpression() {
        long t = peek();
        switch (kind(t)) {
            case Lexer.TK_IDENTIFIER -> {
                String identifier = identifier();
                return ThreadContext.getInstance().convertIdentifier(position(t), identifier);
            }
            case Lexer.TK_INTLITERAL -> {
                return intLiteral();
            }
            case Lexer.TK_FLOATLITERAL -> {
                return floatLiteral();
            }
            case Lexer.TK_TRUE, Lexer.TK_FALSE -> {
                return booleanLiteral();
            }
            case Lexer.TK_LPAREN -> {
                nextToken();
                Expression result = expression();
                if (result != null) {
                    expect(Lexer.TK_RPAREN, "')' to complete expression");
                    result.mPosition = Node.makeRange(result.getStartOffset(), mLexer.position());
                    return result;
                }
            }
            default -> {
                nextToken();
                error(t, "expected expression, but found '" + text(t) + "'");
                throw new RuntimeException();
            }
        }
        return null;
    }

    /**
     * IDENTIFIER
     */
    @Nonnull
    private String identifier() {
        long token = expect(Lexer.TK_IDENTIFIER, "identifier");
        return text(token);
    }

    /**
     * INTLITERAL
     */
    @Nullable
    private Literal intLiteral() {
        long token = expect(Lexer.TK_INTLITERAL, "integer literal");
        String s = text(token);
        if (s.endsWith("u") || s.endsWith("U")) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            long value = Long.decode(s);
            if (value <= 0xFFFF_FFFFL) {
                return Literal.makeInteger(
                        position(token),
                        value);
            }
            error(token, "integer value is too large: " + s);
            return null;
        } catch (NumberFormatException e) {
            error(token, "invalid integer value: " + e.getMessage());
            return null;
        }
    }

    /**
     * FLOATLITERAL
     */
    @Nullable
    private Literal floatLiteral() {
        long token = expect(Lexer.TK_FLOATLITERAL, "float literal");
        String s = text(token);
        try {
            float value = Float.parseFloat(s);
            if (Float.isFinite(value)) {
                return Literal.makeFloat(
                        position(token),
                        value);
            }
            error(token, "floating-point value is too large: " + s);
            return null;
        } catch (NumberFormatException e) {
            error(token, "invalid floating-point value: " + e.getMessage());
            return null;
        }
    }

    /**
     * TRUE | FALSE
     */
    @Nullable
    private Literal booleanLiteral() {
        long token = nextToken();
        return switch (kind(token)) {
            case Lexer.TK_TRUE -> Literal.makeBoolean(
                    position(token),
                    true);
            case Lexer.TK_FALSE -> Literal.makeBoolean(
                    position(token),
                    false);
            default -> {
                error(token, "expected 'true' or 'false', but found '" +
                        text(token) + "'");
                yield null;
            }
        };
    }
}
