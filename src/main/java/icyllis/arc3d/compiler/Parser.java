/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.parser.Lexer;
import icyllis.arc3d.compiler.parser.Token;
import icyllis.arc3d.compiler.tree.*;
import it.unimi.dsi.fastutil.longs.*;
import org.lwjgl.util.spvc.Spv;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Consumes AkSL text and invokes DSL functions to instantiate the program.
 */
public class Parser {

    private final ShaderCompiler mCompiler;

    private final ExecutionModel mModel;
    private final CompileOptions mOptions;

    private final char[] mSource;
    private final Lexer mLexer;

    private final LongStack mPushback = new LongArrayList();

    public Parser(ShaderCompiler compiler, ExecutionModel model, CompileOptions options, char[] source) {
        // ideally we can break long text into pieces, but shader code should not be too long
        if (source.length > 0x7FFFFE) {
            throw new IllegalArgumentException("Source code is too long, " + source.length + " > 8,388,606");
        }
        mCompiler = Objects.requireNonNull(compiler);
        mOptions = Objects.requireNonNull(options);
        mModel = Objects.requireNonNull(model);
        mSource = source;
        mLexer = new Lexer(source);
    }

    @Nullable
    public TranslationUnit parse(ModuleUnit parent) {
        Objects.requireNonNull(parent);
        TranslationUnit();
        return null;
    }

    @Nullable
    public ModuleUnit parseModule(ModuleUnit parent) {
        Objects.requireNonNull(parent);
        TranslationUnit();
        final ModuleUnit result;
        if (mCompiler.getContext().getErrorHandler().getNumErrors() == 0) {
            result = new ModuleUnit();
            result.mParent = parent;
            result.mSymbols = mCompiler.getContext().getSymbolTable();
            result.mElements = mCompiler.getContext().getUniqueElements();
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Return the next token, including whitespace tokens, from the parse stream.
     */
    private long nextRawToken() {
        final long token;
        if (!mPushback.isEmpty()) {
            // Retrieve the token from the pushback buffer.
            token = mPushback.popLong();
        } else {
            // Fetch a token from the lexer.
            token = mLexer.next();

            if (Token.kind(token) == Lexer.TK_RESERVED) {
                error(token, "'" + text(token) + "' is a reserved keyword");
                // reduces additional follow-up errors
                return Token.replace(token, Lexer.TK_IDENTIFIER);
            }
        }
        return token;
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
            if (!isWhitespace(Token.kind(token))) {
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
        mPushback.push(token);
    }

    /**
     * Returns the next non-whitespace token without consuming it from the stream.
     */
    private long peek() {
        if (mPushback.isEmpty()) {
            long token = nextToken();
            mPushback.push(token);
            return token;
        }
        return mPushback.topLong();
    }

    private boolean peek(int kind) {
        return Token.kind(peek()) == kind;
    }

    @Nonnull
    private String text(long token) {
        int offset = Token.offset(token);
        int length = Token.length(token);
        return new String(mSource, offset, length);
    }

    private int position(long token) {
        int offset = Token.offset(token);
        return Position.range(offset, offset + Token.length(token));
    }

    private void error(long token, String msg) {
        int offset = Token.offset(token);
        error(offset, offset + Token.length(token), msg);
    }

    private void error(int start, int end, String msg) {
        mCompiler.getContext().error(start, end, msg);
    }

    private void warning(long token, String msg) {
        int offset = Token.offset(token);
        warning(offset, offset + Token.length(token), msg);
    }

    private void warning(int start, int end, String msg) {
        mCompiler.getContext().warning(start, end, msg);
    }

    // Returns the range from `start` to the current parse position.
    private int rangeFromOffset(int startOffset) {
        int endOffset = mPushback.isEmpty()
                ? mLexer.offset()
                : Token.offset(mPushback.topLong());
        return Position.range(startOffset, endOffset);
    }

    private int rangeFrom(int startPos) {
        return rangeFromOffset(Position.getStartOffset(startPos));
    }

    private int rangeFrom(long startToken) {
        return rangeFromOffset(Token.offset(startToken));
    }

    /**
     * Checks to see if the next token is of the specified type. If so, consumes it
     * and returns true. Otherwise, pushes it back and returns false.
     */
    private boolean checkNext(int kind) {
        long next = peek();
        if (Token.kind(next) == kind) {
            nextToken();
            return true;
        }
        return false;
    }

    /**
     * Behaves like checkNext(TK_IDENTIFIER), but also verifies that identifier is not a builtin
     * type. If the token was actually a builtin type, false is returned (the next token is not
     * considered to be an identifier).
     */
    private long checkIdentifier() {
        long next = peek();
        if (Token.kind(next) == Lexer.TK_IDENTIFIER &&
                !mCompiler.getContext().getSymbolTable().isBuiltinType(text(next))) {
            return nextToken();
        }
        return -1;
    }

    /**
     * Reads the next non-whitespace token and generates an error if it is not the expected type.
     * The {@code expected} string is part of the error message, which reads:
     * <p>
     * "expected [expected], but found '[actual text]'"
     */
    private long expect(int kind, String expected) {
        long next = nextToken();
        if (Token.kind(next) != kind) {
            String msg = "expected " + expected + ", but found '" + text(next) + "'";
            error(next, msg);
            throw new IllegalStateException(msg);
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
        if (mCompiler.getContext().getSymbolTable().isBuiltinType(text(token))) {
            String msg = "expected an identifier, but found type '" + text(token) + "'";
            error(token, msg);
            throw new IllegalStateException(msg);
        }
        return token;
    }

    /**
     * If the next token is a newline, consumes it and returns true. If not, returns false.
     */
    private boolean expectNewline() {
        long token = nextRawToken();
        if (Token.kind(token) == Lexer.TK_WHITESPACE) {
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

    private void TranslationUnit() {
        for (;;) {
            switch (Token.kind(peek())) {
                case Lexer.TK_END_OF_FILE -> {
                    return;
                }
                case Lexer.TK_INVALID -> {
                    error(peek(), "invalid token");
                    return;
                }
                default -> {
                    try {
                        if (!GlobalDeclaration()) {
                            return;
                        }
                    } catch (IllegalStateException e) {
                        // fatal error
                        return;
                    }
                }
            }
        }
    }

    private boolean GlobalDeclaration() {
        long start = peek();
        if (Token.kind(start) == Lexer.TK_SEMICOLON) {
            // empty declaration
            nextToken();
            return true;
        }
        Modifiers modifiers = modifiers();
        long peek = peek();

        if (peek == Lexer.TK_IDENTIFIER &&
                !mCompiler.getContext().getSymbolTable().isType(text(peek))) {
            //TODO interface block
            return true;
        }

        if (peek == Lexer.TK_SEMICOLON) {
            nextToken();
            return true;
        }

        if (peek == Lexer.TK_STRUCT) {
            StructDeclaration();
            return true;
        }

        Type type = TypeSpecifier(modifiers);
        if (type == null) {
            return false;
        }

        long name = expectIdentifier();

        if (checkNext(Lexer.TK_LPAREN)) {
            return FunctionDeclarationRest(position(start), modifiers, type, name);
        } else {
            GlobalVarDeclarationRest(position(start), modifiers, type, name);
            return true;
        }
    }

    private boolean FunctionDeclarationRest(int start,
                                            Modifiers modifiers,
                                            Type returnType,
                                            long name) {
        List<Variable> parameters = new ArrayList<>();
        if (!peek(Lexer.TK_RPAREN)) {
            if (peek(Lexer.TK_IDENTIFIER) && "void".equals(text(peek()))) {
                // '(void)' means no parameters.
                nextToken();
            } else {
                do {
                    Variable parameter = Parameter();
                    if (parameter == null) {
                        return false;
                    }
                    parameters.add(parameter);
                } while (checkNext(Lexer.TK_COMMA));
            }
        }
        expect(Lexer.TK_RPAREN, "')' to complete parameter list");

        FunctionDecl decl = FunctionDecl.convert(
                mCompiler.getContext(),
                rangeFrom(start),
                modifiers,
                text(name),
                parameters,
                returnType
        );

        Context context = mCompiler.getContext();
        if (peek(Lexer.TK_SEMICOLON)) {
            nextToken();
            if (decl == null) {
                return false;
            }
            context.getUniqueElements().add(
                    new FunctionPrototype(decl.mPosition, decl, context.isBuiltin())
            );
            return true;
        } else {
            mCompiler.getContext().
                    enterScope();
            try {
                if (decl != null) {
                    for (Variable param : decl.getParameters()) {
                        context.getSymbolTable().insert(mCompiler.getContext(), param);
                    }
                }

                long blockStart = peek();
                BlockStatement block = ScopedBlock();

                if (decl == null || block == null) {
                    return false;
                }

                int pos = rangeFrom(blockStart);
                FunctionDefinition function = FunctionDefinition.convert(
                        mCompiler.getContext(),
                        pos,
                        decl,
                        false,
                        block
                );

                if (function == null) {
                    return false;
                }
                decl.setDefinition(function);
                context.getUniqueElements().add(function);
                return true;
            } finally {
                mCompiler.getContext()
                        .leaveScope();
            }
        }
    }

    /**
     * Parameter declaration.
     */
    @Nullable
    private Variable Parameter() {
        int pos = position(peek());
        Modifiers modifiers = modifiers();
        Type type = TypeSpecifier(modifiers);
        if (type == null) {
            return null;
        }
        long name = checkIdentifier();
        String nameText = "";
        if (name != -1) {
            nameText = text(name);
        }
        type = ArraySpecifier(pos, type);
        if (type == null) {
            return null;
        }
        return Variable.convert(
                mCompiler.getContext(),
                rangeFrom(pos),
                modifiers,
                type,
                nameText,
                Variable.kParameter_Storage
        );
    }

    private BlockStatement ScopedBlock() {
        long start = expect(Lexer.TK_LBRACE, "'{'");
        List<Statement> statements = new ArrayList<>();
        for (;;) {
            if (checkNext(Lexer.TK_RBRACE)) {
                int pos = rangeFrom(start);
                return BlockStatement.makeBlock(pos, statements);
            } else {
                Statement statement = Statement();
                if (statement != null) {
                    statements.add(statement);
                }
            }
        }
    }

    private void GlobalVarDeclarationRest(int pos,
                                          Modifiers modifiers,
                                          Type baseType,
                                          long name) {
        boolean first = true;
        do {
            if (first) {
                first = false;
            } else {
                name = expectIdentifier();
            }
            Type type = ArraySpecifier(pos, baseType);
            if (type == null) {
                return;
            }
            Expression init = null;
            if (checkNext(Lexer.TK_EQ)) {
                init = AssignmentExpression();
                if (init == null) {
                    return;
                }
            }

            VariableDecl variableDecl = VariableDecl.convert(
                    mCompiler.getContext(),
                    rangeFrom(pos),
                    modifiers,
                    type,
                    text(name),
                    Variable.kGlobal_Storage,
                    init);
            if (variableDecl != null) {
                mCompiler.getContext().getUniqueElements().add(
                        new GlobalVariableDecl(variableDecl)
                );
            }
        } while (checkNext(Lexer.TK_COMMA));

        expect(Lexer.TK_SEMICOLON, "';' to complete global variable declaration");
    }

    @Nullable
    private Expression operatorRight(Expression left, Operator op,
                                     @Nonnull java.util.function.Function<Parser, Expression> rightFn) {
        nextToken();
        Expression right = rightFn.apply(this);
        if (right == null) {
            return null;
        }
        int pos = Position.range(left.getStartOffset(), right.getEndOffset());
        Expression result = BinaryExpression.convert(mCompiler.getContext(), pos, left, op, right);
        if (result != null) {
            return result;
        }
        return Poison.make(mCompiler.getContext(), pos);
    }

    /**
     * <pre>{@literal
     * Expression
     *     : AssignmentExpression
     *     | Expression COMMA AssignmentExpression
     * }</pre>
     */
    @Nullable
    private Expression Expression() {
        Expression result = AssignmentExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_COMMA)) {
            if ((result = operatorRight(result, Operator.COMMA,
                    Parser::AssignmentExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * AssignmentExpression
     *     : ConditionalExpression
     *     | ConditionalExpression AssignmentOperator AssignmentExpression
     *
     * AssignmentOperator
     *     : EQ
     *     | PLUSEQ
     *     | MINUSEQ
     *     | STAREQ
     *     | SLASHEQ
     *     | PERCENTEQ
     *     | AMPEQ
     *     | PIPEEQ
     *     | CARETEQ
     *     | LTLTEQ
     *     | GTGTEQ
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression AssignmentExpression() {
        Expression result = ConditionalExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (Token.kind(peek())) {
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
                if ((result = operatorRight(result, op,
                        Parser::AssignmentExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    @Nonnull
    private Expression expressionOrPoison(int pos, @Nullable Expression expr) {
        if (expr == null) {
            expr = Poison.make(mCompiler.getContext(), pos);
        }
        return expr;
    }

    /**
     * <pre>{@literal
     * ConditionalExpression
     *     : LogicalOrExpression
     *     | LogicalOrExpression QUES Expression COLON AssignmentExpression
     * }</pre>
     */
    @Nullable
    private Expression ConditionalExpression() {
        Expression base = LogicalOrExpression();
        if (base == null) {
            return null;
        }
        if (!peek(Lexer.TK_QUES)) {
            return base;
        }
        nextToken();
        Expression whenTrue = Expression();
        if (whenTrue == null) {
            return null;
        }
        expect(Lexer.TK_COLON, "':'");
        Expression whenFalse = AssignmentExpression();
        if (whenFalse == null) {
            return null;
        }
        int pos = Position.range(base.getStartOffset(), whenFalse.getEndOffset());
        return expressionOrPoison(pos,
                ConditionalExpression.convert(mCompiler.getContext(), pos, base, whenTrue, whenFalse));
    }

    /**
     * <pre>{@literal
     * LogicalOrExpression
     *     : LogicalXorExpression
     *     | LogicalOrExpression PIPEPIPE LogicalXorExpression
     * }</pre>
     */
    @Nullable
    private Expression LogicalOrExpression() {
        Expression result = LogicalXorExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_PIPEPIPE)) {
            if ((result = operatorRight(result, Operator.LOGICAL_OR,
                    Parser::LogicalXorExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * LogicalXorExpression
     *     : LogicalAndExpression
     *     | LogicalXorExpression CARETCARET LogicalAndExpression
     * }</pre>
     */
    @Nullable
    private Expression LogicalXorExpression() {
        Expression result = LogicalAndExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_CARETCARET)) {
            if ((result = operatorRight(result, Operator.LOGICAL_XOR,
                    Parser::LogicalAndExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * LogicalAndExpression
     *     : BitwiseOrExpression
     *     | LogicalAndExpression AMPAMP BitwiseOrExpression
     * }</pre>
     */
    @Nullable
    private Expression LogicalAndExpression() {
        Expression result = BitwiseOrExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_AMPAMP)) {
            if ((result = operatorRight(result, Operator.LOGICAL_AND,
                    Parser::BitwiseOrExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * BitwiseOrExpression
     *     : BitwiseXorExpression
     *     | BitwiseOrExpression PIPE BitwiseXorExpression
     * }</pre>
     */
    @Nullable
    private Expression BitwiseOrExpression() {
        Expression result = BitwiseXorExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_PIPE)) {
            if ((result = operatorRight(result, Operator.BITWISE_OR,
                    Parser::BitwiseXorExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * BitwiseXorExpression
     *     : BitwiseAndExpression
     *     | BitwiseXorExpression CARET BitwiseAndExpression
     * }</pre>
     */
    @Nullable
    private Expression BitwiseXorExpression() {
        Expression result = BitwiseAndExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_CARET)) {
            if ((result = operatorRight(result, Operator.BITWISE_XOR,
                    Parser::BitwiseAndExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * BitwiseAndExpression
     *     : EqualityExpression
     *     | BitwiseAndExpression AMP EqualityExpression
     * }</pre>
     */
    @Nullable
    private Expression BitwiseAndExpression() {
        Expression result = EqualityExpression();
        if (result == null) {
            return null;
        }
        while (peek(Lexer.TK_AMP)) {
            if ((result = operatorRight(result, Operator.BITWISE_AND,
                    Parser::EqualityExpression)) == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * <pre>{@literal
     * EqualityExpression
     *     : RelationalExpression
     *     | EqualityExpression EQEQ RelationalExpression
     *     | EqualityExpression BANGEQ RelationalExpression
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression EqualityExpression() {
        Expression result = RelationalExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (Token.kind(peek())) {
                case Lexer.TK_EQEQ   -> Operator.EQ;
                case Lexer.TK_BANGEQ -> Operator.NE;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op,
                        Parser::RelationalExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    /**
     * <pre>{@literal
     * RelationalExpression
     *     : ShiftExpression
     *     | RelationalExpression LT ShiftExpression
     *     | RelationalExpression GT ShiftExpression
     *     | RelationalExpression LTEQ ShiftExpression
     *     | RelationalExpression GTEQ ShiftExpression
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression RelationalExpression() {
        Expression result = ShiftExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (Token.kind(peek())) {
                case Lexer.TK_LT   -> Operator.LT;
                case Lexer.TK_GT   -> Operator.GT;
                case Lexer.TK_LTEQ -> Operator.LE;
                case Lexer.TK_GTEQ -> Operator.GE;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op,
                        Parser::ShiftExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    /**
     * <pre>{@literal
     * ShiftExpression
     *     : AdditiveExpression
     *     | ShiftExpression LTLT AdditiveExpression
     *     | ShiftExpression GTGT AdditiveExpression
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression ShiftExpression() {
        Expression result = AdditiveExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (Token.kind(peek())) {
                case Lexer.TK_LTLT -> Operator.SHL;
                case Lexer.TK_GTGT -> Operator.SHR;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op,
                        Parser::AdditiveExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    /**
     * <pre>{@literal
     * AdditiveExpression
     *     : MultiplicativeExpression
     *     | AdditiveExpression PLUS MultiplicativeExpression
     *     | AdditiveExpression MINUS MultiplicativeExpression
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression AdditiveExpression() {
        Expression result = MultiplicativeExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (Token.kind(peek())) {
                case Lexer.TK_PLUS  -> Operator.ADD;
                case Lexer.TK_MINUS -> Operator.SUB;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op,
                        Parser::MultiplicativeExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    /**
     * <pre>{@literal
     * MultiplicativeExpression
     *     : UnaryExpression
     *     | MultiplicativeExpression STAR UnaryExpression
     *     | MultiplicativeExpression SLASH UnaryExpression
     *     | MultiplicativeExpression PERCENT UnaryExpression
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression MultiplicativeExpression() {
        Expression result = UnaryExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            Operator op = switch (Token.kind(peek())) {
                case Lexer.TK_STAR    -> Operator.MUL;
                case Lexer.TK_SLASH   -> Operator.DIV;
                case Lexer.TK_PERCENT -> Operator.MOD;
                default -> null;
            };
            if (op != null) {
                if ((result = operatorRight(result, op,
                        Parser::UnaryExpression)) == null) {
                    return null;
                }
            } else {
                return result;
            }
        }
    }
    // @formatter:on

    /**
     * <pre>{@literal
     * UnaryExpression
     *     : PrimaryExpression Selector* (PLUSPLUS | MINUSMINUS)?
     *     | PLUSPLUS UnaryExpression
     *     | MINUSMINUS UnaryExpression
     *     | UnaryOperator UnaryExpression
     *
     * UnaryOperator
     *     : PLUS
     *     | MINUS
     *     | BANG
     *     | TILDE
     * }</pre>
     */
    // @formatter:off
    @Nullable
    private Expression UnaryExpression() {
        long prefix = peek();
        Operator op = switch (Token.kind(prefix)) {
            case Lexer.TK_PLUSPLUS   -> Operator.INC;
            case Lexer.TK_MINUSMINUS -> Operator.DEC;
            case Lexer.TK_PLUS       -> Operator.ADD;
            case Lexer.TK_MINUS      -> Operator.SUB;
            case Lexer.TK_BANG       -> Operator.LOGICAL_NOT;
            case Lexer.TK_TILDE      -> Operator.BITWISE_NOT;
            default -> null;
        };
        if (op != null) {
            nextToken();
            Expression base = UnaryExpression();
            if (base == null) {
                return null;
            }
            int pos = Position.range(Token.offset(prefix), base.getEndOffset());
            return expressionOrPoison(pos,
                    PrefixExpression.convert(mCompiler.getContext(), pos, op, base));
        }
        return PostfixExpression();
    }
    // @formatter:on

    /**
     * <pre>{@literal
     * PostfixExpression
     *     : PrimaryExpression
     *     | PostfixExpression LBRACKET Expression? RBRACKET
     *     | PostfixExpression LPAREN (VOID | Expression)? RPAREN
     *     | PostfixExpression DOT IDENTIFIER
     *     | PostfixExpression PLUSPLUS
     *     | PostfixExpression MINUSMINU
     * }</pre>
     */
    @Nullable
    private Expression PostfixExpression() {
        Expression result = PrimaryExpression();
        if (result == null) {
            return null;
        }
        for (;;) {
            long t = peek();
            switch (Token.kind(t)) {
                case Lexer.TK_LBRACKET -> {
                    nextToken();
                    Expression index = null;
                    if (!peek(Lexer.TK_RBRACKET)) {
                        index = Expression();
                        if (index == null) {
                            return null;
                        }
                    }
                    expect(Lexer.TK_RBRACKET, "']' to complete array specifier");
                    int pos = rangeFrom(result.mPosition);
                    result = expressionOrPoison(pos,
                            IndexExpression.convert(mCompiler.getContext(), pos, result, index));
                }
                case Lexer.TK_DOT -> {
                    nextToken();
                    // swizzle mask, field access, method reference
                    long name = expect(Lexer.TK_IDENTIFIER, "identifier");
                    String text = text(name);
                    int pos = rangeFrom(result.mPosition);
                    int namePos = rangeFrom(name);
                    result = expressionOrPoison(pos,
                            FieldAccess.convert(mCompiler.getContext(), pos, result, namePos, text));
                }
                case Lexer.TK_LPAREN -> {
                    nextToken();
                    // constructor call, function call, method call
                    List<Expression> args = new ArrayList<>();
                    if (!peek(Lexer.TK_RPAREN)) {
                        if (peek(Lexer.TK_IDENTIFIER) && "void".equals(text(peek()))) {
                            // '(void)' means no arguments.
                            nextToken();
                        } else {
                            do {
                                Expression expr = AssignmentExpression();
                                if (expr == null) {
                                    return null;
                                }
                                args.add(expr);
                            } while (checkNext(Lexer.TK_COMMA));
                        }
                    }
                    expect(Lexer.TK_RPAREN, "')' to complete invocation");
                    int pos = rangeFrom(result.mPosition);
                    result = expressionOrPoison(pos,
                            FunctionCall.convert(mCompiler.getContext(), pos, result, args));
                }
                case Lexer.TK_PLUSPLUS, Lexer.TK_MINUSMINUS -> {
                    nextToken();
                    Operator op = Token.kind(t) == Lexer.TK_PLUSPLUS
                            ? Operator.INC
                            : Operator.DEC;
                    int pos = rangeFrom(result.mPosition);
                    result = expressionOrPoison(pos,
                            PostfixExpression.convert(mCompiler.getContext(), pos, result, op));
                }
                default -> {
                    return result;
                }
            }
        }
    }

    /**
     * <pre>{@literal
     * PrimaryExpression
     *     : IDENTIFIER
     *     | INTLITERAL
     *     | FLOATLITERAL
     *     | BOOLEANLITERAL
     *     | LPAREN Expression RPAREN
     * }</pre>
     */
    @Nullable
    private Expression PrimaryExpression() {
        long t = peek();
        return switch (Token.kind(t)) {
            case Lexer.TK_IDENTIFIER -> {
                nextToken();
                yield mCompiler.getContext().convertIdentifier(position(t), text(t));
            }
            case Lexer.TK_INTLITERAL -> IntLiteral();
            case Lexer.TK_FLOATLITERAL -> FloatLiteral();
            case Lexer.TK_TRUE, Lexer.TK_FALSE -> BooleanLiteral();
            case Lexer.TK_LPAREN -> {
                nextToken();
                Expression result = Expression();
                if (result != null) {
                    expect(Lexer.TK_RPAREN, "')' to complete expression");
                    result.mPosition = rangeFrom(t);
                    yield result;
                }
                yield null;
            }
            default -> {
                nextToken();
                error(t, "expected identifier, literal constant or parenthesized expression, but found '" +
                        text(t) + "'");
                throw new IllegalStateException();
            }
        };
    }

    /**
     * INTLITERAL
     */
    @Nullable
    private Literal IntLiteral() {
        long token = expect(Lexer.TK_INTLITERAL, "integer literal");
        String s = text(token);
        if (s.endsWith("u") || s.endsWith("U")) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            long value = Long.decode(s);
            if (value <= 0xFFFF_FFFFL) {
                return Literal.makeInteger(
                        mCompiler.getContext(),
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
    private Literal FloatLiteral() {
        long token = expect(Lexer.TK_FLOATLITERAL, "float literal");
        String s = text(token);
        try {
            float value = Float.parseFloat(s);
            if (Float.isFinite(value)) {
                return Literal.makeFloat(
                        mCompiler.getContext(),
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
    private Literal BooleanLiteral() {
        long token = nextToken();
        return switch (Token.kind(token)) {
            case Lexer.TK_TRUE -> Literal.makeBoolean(
                    mCompiler.getContext(),
                    position(token),
                    true);
            case Lexer.TK_FALSE -> Literal.makeBoolean(
                    mCompiler.getContext(),
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
     * <pre>{@literal
     * Modifiers
     *     : (LAYOUT Layout)?
     *       (UNIFORM | CONST | IN | OUT | INOUT | SMOOTH | FLAT |
     *        NOPERSPECTIVE | SUBROUTINE | PURE | INLINE | NOINLINE |
     *        READONLY | WRITEONLY | BUFFER | WORKGROUP)*
     * }</pre>
     */
    @Nonnull
    private Modifiers modifiers() {
        long start = peek();
        Modifiers modifiers = new Modifiers(Position.NO_POS);
        if (checkNext(Lexer.TK_LAYOUT)) {
            Layout(modifiers);
        }
        for (;;) {
            int mask = switch (Token.kind(peek())) {
                case Lexer.TK_SMOOTH -> Modifiers.kSmooth_Flag;
                case Lexer.TK_FLAT -> Modifiers.kFlat_Flag;
                case Lexer.TK_NOPERSPECTIVE -> Modifiers.kNoPerspective_Flag;
                case Lexer.TK_CONST -> Modifiers.kConst_Flag;
                case Lexer.TK_UNIFORM -> Modifiers.kUniform_Flag;
                case Lexer.TK_IN -> Modifiers.kIn_Flag;
                case Lexer.TK_OUT -> Modifiers.kOut_Flag;
                case Lexer.TK_INOUT -> Modifiers.kIn_Flag | Modifiers.kOut_Flag;
                case Lexer.TK_READONLY -> Modifiers.kReadOnly_Flag;
                case Lexer.TK_WRITEONLY -> Modifiers.kWriteOnly_Flag;
                case Lexer.TK_BUFFER -> Modifiers.kBuffer_Flag;
                case Lexer.TK_WORKGROUP -> Modifiers.kWorkgroup_Flag;
                case Lexer.TK_SUBROUTINE -> Modifiers.kSubroutine_Flag;
                case Lexer.TK_PURE -> Modifiers.kPure_Flag;
                case Lexer.TK_INLINE -> Modifiers.kInline_Flag;
                case Lexer.TK_NOINLINE -> Modifiers.kNoInline_Flag;
                default -> 0;
            };
            if (mask == 0) {
                break;
            }
            long token = nextToken();
            modifiers.setFlag(mCompiler.getContext(), mask, position(token));
        }
        modifiers.mPosition = rangeFrom(start);
        return modifiers;
    }

    /**
     * <pre>{@literal
     * Layout
     *     : LPAREN SingleLayout (COMMA SingleLayout)* RPAREN
     * SingleLayout
     *     : IDENTIFIER (EQ (INTLITERAL | IDENTIFIER))?
     * }</pre>
     */
    private void Layout(Modifiers modifiers) {
        expect(Lexer.TK_LPAREN, "'('");
        do {
            long name = expect(Lexer.TK_IDENTIFIER, "identifier");
            String text = text(name);
            int pos = position(name);
            int mask = switch (text) {
                case "origin_upper_left" ->Layout.kOriginUpperLeft_LayoutFlag;
                case "pixel_center_integer" ->Layout.kPixelCenterInteger_LayoutFlag;
                case "early_fragment_tests" ->Layout.kEarlyFragmentTests_LayoutFlag;
                case "blend_support_all_equations" ->Layout.kBlendSupportAllEquations_LayoutFlag;
                case "push_constant" ->Layout.kPushConstant_LayoutFlag;
                case "location" -> Layout.kLocation_LayoutFlag;
                case "component" -> Layout.kComponent_LayoutFlag;
                case "index" -> Layout.kIndex_LayoutFlag;
                case "binding" ->Layout.kBinding_LayoutFlag;
                case "offset" ->Layout.kOffset_LayoutFlag;
                case "align" -> Layout.kAlign_LayoutFlag;
                case "set" -> Layout.kSet_LayoutFlag;
                case "input_attachment_index" ->Layout.kInputAttachmentIndex_LayoutFlag;
                case "builtin" -> Layout.kBuiltin_LayoutFlag;
                default -> 0;
            };
            if (mask != 0) {
                modifiers.setLayoutFlag(mCompiler.getContext(), mask, text, pos);
                Layout layout = modifiers.layout();
                switch (mask) {
                    case Layout.kLocation_LayoutFlag -> layout.mLocation = LayoutInt();
                    case Layout.kComponent_LayoutFlag -> layout.mComponent = LayoutInt();
                    case Layout.kIndex_LayoutFlag -> layout.mIndex = LayoutInt();
                    case Layout.kBinding_LayoutFlag -> layout.mBinding = LayoutInt();
                    case Layout.kOffset_LayoutFlag -> layout.mOffset = LayoutInt();
                    case Layout.kAlign_LayoutFlag -> layout.mAlign = LayoutInt();
                    case Layout.kSet_LayoutFlag -> layout.mSet = LayoutInt();
                    case Layout.kInputAttachmentIndex_LayoutFlag -> layout.mInputAttachmentIndex = LayoutInt();
                    case Layout.kBuiltin_LayoutFlag -> layout.mBuiltin = LayoutBuiltin();
                }
            } else {
                warning(name, "unrecognized layout qualifier '" + text + "'");
                if (checkNext(Lexer.TK_EQ)) {
                    nextToken();
                }
            }
        } while (checkNext(Lexer.TK_COMMA));
        expect(Lexer.TK_RPAREN, "')'");
    }

    private int LayoutInt() {
        expect(Lexer.TK_EQ, "'='");
        long token = expect(Lexer.TK_INTLITERAL, "integer literal");
        return LayoutIntValue(token);
    }

    private int LayoutBuiltin() {
        expect(Lexer.TK_EQ, "'='");
        if (peek(Lexer.TK_INTLITERAL)) {
            long token = nextToken();
            return LayoutIntValue(token);
        }
        long name = expectIdentifier();
        String text = text(name);
        return switch (text) {
            case "position" -> Spv.SpvBuiltInPosition;
            case "vertex_index" -> Spv.SpvBuiltInVertexIndex;
            case "instance_index" -> Spv.SpvBuiltInInstanceIndex;
            case "frag_coord" -> Spv.SpvBuiltInFragCoord;
            case "front_facing" -> Spv.SpvBuiltInFrontFacing;
            case "sample_mask" -> Spv.SpvBuiltInSampleMask; // in/out
            case "frag_depth" -> Spv.SpvBuiltInFragDepth;
            case "num_workgroups" -> Spv.SpvBuiltInNumWorkgroups;
            case "workgroup_id" -> Spv.SpvBuiltInWorkgroupId;
            case "local_invocation_id" -> Spv.SpvBuiltInLocalInvocationId;
            case "global_invocation_id" -> Spv.SpvBuiltInGlobalInvocationId;
            case "local_invocation_index" -> Spv.SpvBuiltInLocalInvocationIndex;
            default -> {
                error(name, "unrecognized built-in name '" + text + "'");
                yield -1;
            }
        };
    }

    private int LayoutIntValue(long token) {
        String s = text(token);
        if (s.endsWith("u") || s.endsWith("U")) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            long value = Long.decode(s);
            if (value <= Integer.MAX_VALUE) {
                return (int) value;
            }
            error(token, "integer value is too large: " + s);
            return -1;
        } catch (NumberFormatException e) {
            error(token, "invalid integer value: " + e.getMessage());
            return -1;
        }
    }

    /**
     * <pre>{@literal
     * TypeSpecifier
     *     : IDENTIFIER ArraySpecifier*
     * }</pre>
     */
    @Nullable
    private Type TypeSpecifier(Modifiers modifiers) {
        long start = expect(Lexer.TK_IDENTIFIER, "a type name");
        String name = text(start);
        var symbol = mCompiler.getContext().getSymbolTable().find(name);
        if (symbol == null) {
            error(start, "no type named '" + name + "'");
            return mCompiler.getContext().getTypes().mPoison;
        }
        if (!(symbol instanceof Type result)) {
            error(start, "symbol '" + name + "' is not a type");
            return mCompiler.getContext().getTypes().mPoison;
        }
        if (result.isInterfaceBlock()) {
            error(start, "expected a type, found interface block '" + name + "'");
            return mCompiler.getContext().getTypes().mInvalid;
        }
        result = ArraySpecifier(position(start), result);
        return result;
    }

    @Nullable
    private Type ArraySpecifier(int startPos, Type type) {
        Context context = mCompiler.getContext();
        while (peek(Lexer.TK_LBRACKET)) {
            nextToken();
            Expression size = null;
            if (!peek(Lexer.TK_RBRACKET)) {
                size = Expression();
                if (size == null) {
                    return null;
                }
            }
            expect(Lexer.TK_RBRACKET, "']' to complete array specifier");
            int pos = rangeFrom(startPos);
            final int arraySize;
            if (size != null) {
                arraySize = type.convertArraySize(mCompiler.getContext(), pos, size);
            } else {
                if (!type.isUsableInArray(mCompiler.getContext(), pos)) {
                    arraySize = 0;
                } else {
                    arraySize = Type.kUnsizedArray;
                }
            }
            if (arraySize == 0) {
                type = context.getTypes().mPoison;
            } else {
                type = context.getSymbolTable().getArrayType(type, arraySize);
            }
        }
        return type;
    }

    /**
     * <pre>{@literal
     * VarDeclarationRest
     *     : IDENTIFIER ArraySpecifier* (EQ AssignmentExpression)? (COMMA
     *       IDENTIFIER ArraySpecifier* (EQ AssignmentExpression)?)* SEMICOLON
     * }</pre>
     */
    @Nullable
    private Statement VarDeclarationRest(int pos, Modifiers modifiers, Type baseType) {
        long name = expectIdentifier();
        Type type = ArraySpecifier(pos, baseType);
        if (type == null) {
            return null;
        }
        Expression init = null;
        if (checkNext(Lexer.TK_EQ)) {
            init = AssignmentExpression();
            if (init == null) {
                return null;
            }
        }
        Statement result = VariableDecl.convert(
                mCompiler.getContext(),
                rangeFrom(name),
                modifiers,
                type,
                text(name),
                Variable.kLocal_Storage,
                init
        );

        while (checkNext(Lexer.TK_COMMA)) {
            name = expectIdentifier();
            type = ArraySpecifier(pos, baseType);
            if (type == null) {
                break;
            }
            init = null;
            if (checkNext(Lexer.TK_EQ)) {
                init = AssignmentExpression();
                if (init == null) {
                    break;
                }
            }
            Statement next = VariableDecl.convert(
                    mCompiler.getContext(),
                    rangeFrom(name),
                    modifiers,
                    type,
                    text(name),
                    Variable.kLocal_Storage,
                    init
            );

            result = BlockStatement.makeCompound(result, next);
        }
        expect(Lexer.TK_SEMICOLON, "';' to complete local variable declaration");
        pos = rangeFrom(pos);
        return statementOrEmpty(pos, result);
    }

    @Nullable
    private Statement VarDeclarationOrExpressionStatement() {
        long peek = peek();
        if (Token.kind(peek) == Lexer.TK_CONST) {
            int pos = position(peek);
            Modifiers modifiers = modifiers();
            Type type = TypeSpecifier(modifiers);
            if (type == null) {
                return null;
            }
            return VarDeclarationRest(pos, modifiers, type);
        }
        if (mCompiler.getContext().getSymbolTable().isType(text(peek))) {
            int pos = position(peek);
            Modifiers modifiers = new Modifiers(pos);
            Type type = TypeSpecifier(modifiers);
            if (type == null) {
                return null;
            }
            return VarDeclarationRest(pos, modifiers, type);
        }
        return ExpressionStatement();
    }

    /**
     * <pre>{@literal
     * ExpressionStatement
     *     : Expression SEMICOLON
     * }</pre>
     */
    @Nullable
    private Statement ExpressionStatement() {
        Expression expr = Expression();
        if (expr == null) {
            return null;
        }
        expect(Lexer.TK_SEMICOLON, "';' to complete expression statement");
        int pos = expr.mPosition;
        return statementOrEmpty(pos, ExpressionStatement.convert(mCompiler.getContext(), expr));
    }

    /**
     * <pre>{@literal
     * StructDeclaration
     *     : STRUCT IDENTIFIER LBRACE VarDeclaration+ RBRACE
     *
     * VarDeclaration
     *     : Modifiers? Type VarDeclarator (COMMA VarDeclarator)* SEMICOLON
     *
     * VarDeclarator
     *     : IDENTIFIER (LBRACKET ConditionalExpression RBRACKET)*
     * }</pre>
     */
    private Type StructDeclaration() {
        long start = peek();
        expect(Lexer.TK_STRUCT, "'struct'");
        long typeName = expectIdentifier();
        expect(Lexer.TK_LBRACE, "'{'");
        return null;
    }

    @Nonnull
    private Statement statementOrEmpty(int pos, @Nullable Statement stmt) {
        if (stmt == null) {
            stmt = new EmptyStatement(pos);
        }
        return stmt;
    }

    @Nullable
    private Statement Statement() {
        return switch (Token.kind(peek())) {
            case Lexer.TK_BREAK -> {
                long start = nextToken();
                expect(Lexer.TK_SEMICOLON, "';' after 'break'");
                yield BreakStatement.make(rangeFrom(start));
            }
            case Lexer.TK_CONTINUE -> {
                long start = nextToken();
                expect(Lexer.TK_SEMICOLON, "';' after 'continue'");
                yield ContinueStatement.make(rangeFrom(start));
            }
            case Lexer.TK_DISCARD -> {
                long start = nextToken();
                expect(Lexer.TK_SEMICOLON, "';' after 'discard'");
                int pos = rangeFrom(start);
                yield statementOrEmpty(pos, DiscardStatement.convert(mCompiler.getContext(), pos));
            }
            case Lexer.TK_RETURN -> {
                long start = nextToken();
                Expression expression = null;
                if (!peek(Lexer.TK_SEMICOLON)) {
                    expression = Expression();
                    if (expression == null) {
                        yield null;
                    }
                }
                expect(Lexer.TK_SEMICOLON, "';' to complete return expression");
                yield ReturnStatement.make(rangeFrom(start), expression);
            }
            case Lexer.TK_IF -> IfStatement();
            case Lexer.TK_FOR -> ForStatement();
            case Lexer.TK_SWITCH -> SwitchStatement();
            case Lexer.TK_SEMICOLON -> {
                long t = nextToken();
                yield new EmptyStatement(position(t));
            }
            case Lexer.TK_CONST, Lexer.TK_IDENTIFIER -> VarDeclarationOrExpressionStatement();
            default -> ExpressionStatement();
        };
    }

    /**
     * <pre>{@literal
     * IfStatement
     *     : IF LPAREN Expression RPAREN Statement (ELSE Statement)?
     * }</pre>
     */
    @Nullable
    private Statement IfStatement() {
        long start = expect(Lexer.TK_IF, "'if'");
        expect(Lexer.TK_LPAREN, "'('");
        Expression test = Expression();
        if (test == null) {
            return null;
        }
        expect(Lexer.TK_RPAREN, "')'");
        Statement whenTrue = Statement();
        if (whenTrue == null) {
            return null;
        }
        Statement whenFalse = null;
        if (checkNext(Lexer.TK_ELSE)) {
            whenFalse = Statement();
            if (whenFalse == null) {
                return null;
            }
        }
        int pos = rangeFrom(start);
        return statementOrEmpty(pos,
                IfStatement.convert(mCompiler.getContext(), pos, test, whenTrue, whenFalse));
    }

    /**
     * <pre>{@literal
     * SwitchStatement
     *     : SWITCH LPAREN Expression RPAREN LBRACE Statement* RBRACE
     * }</pre>
     */
    private Statement SwitchStatement() {
        return null;
    }

    /**
     * <pre>{@literal
     * ForStatement
     *     : FOR LPAREN ForInit SEMICOLON Expression? SEMICOLON Expression? RPAREN Statement
     *
     * ForInit
     *     : (DeclarationStatement | ExpressionStatement)?
     * }</pre>
     */
    @Nullable
    private Statement ForStatement() {
        long start = expect(Lexer.TK_FOR, "'for'");
        expect(Lexer.TK_LPAREN, "'('");
        mCompiler.getContext()
                .enterScope();
        try {
            Statement init = null;
            if (peek(Lexer.TK_SEMICOLON)) {
                // An empty init-statement.
                nextToken();
            } else {
                init = VarDeclarationOrExpressionStatement();
                if (init == null) {
                    return null;
                }
            }

            Expression cond = null;
            if (!peek(Lexer.TK_SEMICOLON)) {
                cond = Expression();
                if (cond == null) {
                    return null;
                }
            }

            expect(Lexer.TK_SEMICOLON, "';' to complete condition statement");

            Expression step = null;
            if (!peek(Lexer.TK_SEMICOLON)) {
                step = Expression();
                if (step == null) {
                    return null;
                }
            }

            expect(Lexer.TK_RPAREN, "')' to complete 'for' statement");

            Statement statement = Statement();
            if (statement == null) {
                return null;
            }

            int pos = rangeFrom(start);

            return statementOrEmpty(pos, ForLoop.convert(
                    mCompiler.getContext(),
                    pos,
                    init,
                    cond,
                    step,
                    statement
            ));
        } finally {
            mCompiler.getContext()
                    .leaveScope();
        }
    }
}
