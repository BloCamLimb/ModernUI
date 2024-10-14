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

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.lex.Lexer;
import icyllis.arc3d.compiler.lex.Token;
import icyllis.arc3d.compiler.tree.*;
import it.unimi.dsi.fastutil.longs.*;
import org.lwjgl.util.spvc.Spv;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Consumes Arc3D shading language source text and invokes DSL functions to
 * instantiate the AST (parsed IR). A Parser object is not reusable.
 */
public class Parser {

    private final ShaderCompiler mCompiler;

    private final ShaderKind mKind;
    private final CompileOptions mOptions;

    private final char[] mSource;
    private final int mSourceOffset;
    private final int mSourceLength;
    private final Lexer mLexer;

    private final LongStack mPushback = new LongArrayList(1);

    private LinkedHashMap<String, String> mExtensions;
    private ArrayList<Map.Entry<String, Boolean>> mIncludes;

    private ArrayList<TopLevelElement> mUniqueElements;

    public Parser(ShaderCompiler compiler, ShaderKind kind, CompileOptions options,
                  char[] source, int offset, int length) {
        mCompiler = Objects.requireNonNull(compiler);
        mOptions = Objects.requireNonNull(options);
        mKind = Objects.requireNonNull(kind);
        mSource = source;
        mSourceOffset = offset;
        mSourceLength = length;
        mLexer = new Lexer(source, offset, length);
        if (options.mExtensions != null && !options.mExtensions.isEmpty()) {
            mExtensions = new LinkedHashMap<>(options.mExtensions);
        }
    }

    /**
     * Preprocess directives. A directive can only appear before any declaration.
     * <p>
     * #version 3-digits (core|es)?<br>
     * The version number has no semantics impact, but must be 300 at least.
     * <p>
     * #extension extension_name : behavior<br>
     * The extension has no semantics impact when generating SPIR-V,
     * but it's retained when generating GLSL for some GLSL only extensions.
     * A new behavior will override the earlier one for the same extension name.
     * <p>
     * #include &lt;file&gt;<br>
     * #include "file"<br>
     * #import &lt;file&gt;<br>
     * #import "file"<br>
     * This method returns the include files as a list of (file,boolean) pairs. The second
     * boolean value of true represents it's angle-bracketed (system include), otherwise it's
     * double-quoted (local include). An implementation should normalize the file name and
     * compile the include files first (via a new Parser's {@link #parseModule}).
     * The return list is not deduplicated.
     * <p>
     * #pragma<br>
     * The whole line is silently ignored.
     * <p>
     * Note that version, extension, and include must appear in order. Other directives can
     * cause compilation errors. This process is optional.
     *
     * @return include files (can be empty) or null if there's an error
     */
    @Nullable
    public List<Map.Entry<String, Boolean>> preprocess() {
        mIncludes = new ArrayList<>();
        Directives();
        Context context = mCompiler.getContext();
        if (context.getErrorHandler().errorCount() == 0) {
            return mIncludes;
        }
        return null;
    }

    @Nullable
    public TranslationUnit parse(ModuleUnit parent) {
        Objects.requireNonNull(parent);
        mUniqueElements = new ArrayList<>();
        CompilationUnit();
        Context context = mCompiler.getContext();
        assert !context.isModule();
        assert !context.isBuiltin();
        final TranslationUnit result;
        if (context.getErrorHandler().errorCount() == 0) {
            result = new TranslationUnit(
                    mSource,
                    mSourceOffset,
                    mSourceLength,
                    mKind,
                    mOptions,
                    context.getTypes(),
                    context.getSymbolTable(),
                    mUniqueElements,
                    mExtensions != null
                            ? new ArrayList<>(mExtensions.entrySet())
                            : new ArrayList<>()
            );
        } else {
            result = null;
        }
        mUniqueElements = null;
        return result;
    }

    @Nullable
    public ModuleUnit parseModule(ModuleUnit parent) {
        Objects.requireNonNull(parent);
        mUniqueElements = new ArrayList<>();
        CompilationUnit();
        Context context = mCompiler.getContext();
        assert context.isModule();
        final ModuleUnit result;
        if (context.getErrorHandler().errorCount() == 0) {
            result = new ModuleUnit();
            result.mParent = parent;
            result.mSymbols = context.getSymbolTable();
            result.mElements = mUniqueElements;
        } else {
            result = null;
        }
        mUniqueElements = null;
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

            if (Token.kind(token) == Token.TK_RESERVED) {
                error(token, "'" + text(token) + "' is a reserved keyword");
                // reduces additional follow-up errors
                return Token.replace(token, Token.TK_IDENTIFIER);
            }
        }
        return token;
    }

    /**
     * Return the next non-whitespace token from the parse stream, including newlines.
     * Pp refers to preprocessor, this is used only in preprocessing.
     */
    // @formatter:off
    private long nextPpToken() {
        for (;;) {
            long token = nextRawToken();
            switch (Token.kind(token)) {
                case Token.TK_WHITESPACE:
                case Token.TK_LINE_COMMENT:
                case Token.TK_BLOCK_COMMENT:
                    break;
                default:
                    return token;
            }
        }
    }
    // @formatter:on

    /**
     * Return the next non-whitespace token from the parse stream.
     */
    // @formatter:off
    private long nextToken() {
        for (;;) {
            long token = nextRawToken();
            switch (Token.kind(token)) {
                case Token.TK_NEWLINE:
                case Token.TK_WHITESPACE:
                case Token.TK_LINE_COMMENT:
                case Token.TK_BLOCK_COMMENT:
                    break;
                default:
                    return token;
            }
        }
    }
    // @formatter:on

    /**
     * Push a token back onto the parse stream, so that it is the next one read.
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
        return text(offset, length);
    }

    @Nonnull
    private String text(int offset, int length) {
        if (length == 0) {
            return "EOF";
        }
        return new String(mSource, offset + mSourceOffset, length);
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
     * type. If the token was actually a builtin type, {@link Token#NO_TOKEN} is returned (the
     * next token is not considered to be an identifier).
     */
    private long checkIdentifier() {
        long next = peek();
        if (Token.kind(next) == Token.TK_IDENTIFIER &&
                !mCompiler.getContext().getSymbolTable().isBuiltinType(text(next))) {
            return nextToken();
        }
        return Token.NO_TOKEN;
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
            throw new FatalError(msg);
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
        long token = expect(Token.TK_IDENTIFIER, "an identifier");
        if (mCompiler.getContext().getSymbolTable().isBuiltinType(text(token))) {
            String msg = "expected an identifier, but found type '" + text(token) + "'";
            error(token, msg);
            throw new FatalError(msg);
        }
        return token;
    }

    private void Directives() {
        // ideally we can break long text into pieces, but shader code should not be too long
        if (mSourceLength > 0x7FFFFE) {
            mCompiler.getContext().error(Position.NO_POS,
                    "source code is too long, " + mSourceLength + " > 8,388,606 chars");
            return;
        }
        boolean first = true;
        for (;;) {
            switch (Token.kind(peek())) {
                case Token.TK_INVALID -> {
                    error(peek(), "invalid token");
                    return;
                }
                case Token.TK_HASH -> {
                    if (!Directive(first)) {
                        return;
                    }
                    first = false;
                }
                default -> {
                    return;
                }
            }
        }
    }

    // this method never throw FatalError, returns false instead
    private boolean Directive(boolean first) {
        long hash = nextPpToken();
        long directive = nextPpToken();
        if (Token.kind(directive) == Token.TK_NEWLINE ||
                Token.kind(directive) == Token.TK_END_OF_FILE) {
            // empty directive
            return true;
        }
        String text = text(directive);
        if (Token.kind(directive) != Token.TK_IDENTIFIER) {
            error(directive, "expected a directive name, but found '" + text + "'");
            return false;
        }
        switch (text) {
            case "version": {
                if (!first) {
                    mCompiler.getContext().error(rangeFrom(hash),
                            "version directive must appear before anything else");
                }
                long version = nextPpToken();
                if (Token.kind(version) != Token.TK_INTLITERAL) {
                    error(version, "version must be an integer literal");
                    return false;
                }
                final int ver;
                try {
                    ver = Integer.parseInt(text(version));
                } catch (NumberFormatException e) {
                    error(version, "invalid version number");
                    return false;
                }
                final String validProfile;
                switch (ver) {
                    case 300, 310, 320 -> validProfile = "es";
                    case 330, 400, 410, 420, 430, 440, 450, 460 -> validProfile = "core";
                    default -> {
                        error(version, "unsupported version number " + ver);
                        return false;
                    }
                }
                long profile = nextPpToken();
                if (Token.kind(profile) == Token.TK_NEWLINE ||
                        Token.kind(profile) == Token.TK_END_OF_FILE) {
                    if (validProfile.equals("es")) {
                        error(profile, "expected the es profile");
                        return false;
                    }
                    return true;
                }
                if (Token.kind(profile) != Token.TK_IDENTIFIER) {
                    error(profile, "expected a profile name");
                    return false;
                }
                String profileText = text(profile);
                if (!validProfile.equals(profileText)) {
                    switch (profileText) {
                        case "es" -> error(profile,
                                "only version 300, 310, and 320 support the es profile");
                        case "core" -> error(profile,
                                "only version 330, 400, 410, 420, 430, 440, 450, and 460 support the core profile");
                        default -> error(profile, "unsupported profile");
                    }
                }
                break;
            }
            case "extension": {
                long extension = nextPpToken();
                if (Token.kind(extension) != Token.TK_IDENTIFIER) {
                    error(extension, "expected an extension name");
                    return false;
                }
                long colon = nextPpToken();
                if (Token.kind(colon) != Token.TK_COLON) {
                    error(colon, "expected ':'");
                    return false;
                }
                long behavior = nextPpToken();
                String behaviorText = text(behavior);
                switch (behaviorText) {
                    case "disable", "require", "enable", "warn" -> {
                    }
                    default -> {
                        error(behavior, "unsupported behavior");
                        return false;
                    }
                }
                if (!mIncludes.isEmpty()) {
                    mCompiler.getContext().error(rangeFrom(hash),
                            "extension directive must appear before any include directive");
                }
                if (mExtensions == null) {
                    mExtensions = new LinkedHashMap<>();
                }
                // override
                mExtensions.put(text(extension), behaviorText);
                break;
            }
            case "include", "import": {
                long left = nextPpToken();
                if (Token.kind(left) == Token.TK_STRINGLITERAL) {
                    int offset = Token.offset(left);
                    int length = Token.length(left);
                    // remove quotes
                    String file = text(offset + 1, length - 2);
                    mIncludes.add(Map.entry(file, Boolean.FALSE));
                } else if (Token.kind(left) == Token.TK_LT) {
                    long right;
                    CYCLE:
                    for (;;) {
                        right = nextPpToken();
                        switch (Token.kind(right)) {
                            case Token.TK_NEWLINE:
                            case Token.TK_END_OF_FILE:
                                error(right, "expected right angle bracket");
                                return false;
                            case Token.TK_GT:
                                break CYCLE;
                            default:
                                break;
                        }
                    }
                    int offset = Token.offset(left);
                    // remove the first angle
                    String file = text(offset + 1, Token.offset(right) - offset - 1);
                    mIncludes.add(Map.entry(file, Boolean.TRUE));
                } else {
                    error(left, "expected quoted string or angle-bracketed string");
                    return false;
                }
                break;
            }
            default:
                mCompiler.getContext().error(rangeFrom(hash),
                        "unsupported directive '" + text + "'");
            case "pragma":
                for (;;) {
                    long token = nextPpToken();
                    switch (Token.kind(token)) {
                        case Token.TK_NEWLINE:
                        case Token.TK_END_OF_FILE:
                            return true;
                        default:
                            break;
                    }
                }
        }
        long end = nextPpToken();
        return switch (Token.kind(end)) {
            case Token.TK_NEWLINE, Token.TK_END_OF_FILE -> true;
            default -> {
                // this guarantees that the next directive starts with newline
                error(end, "a directive must end with newline");
                yield false;
            }
        };
    }

    private void CompilationUnit() {
        // ideally we can break long text into pieces, but shader code should not be too long
        if (mSourceLength > 0x7FFFFE) {
            mCompiler.getContext().error(Position.NO_POS,
                    "source code is too long, " + mSourceLength + " > 8,388,606 chars");
            return;
        }
        boolean beforeDeclaration = true;
        for (;;) {
            switch (Token.kind(peek())) {
                case Token.TK_END_OF_FILE -> {
                    return;
                }
                case Token.TK_INVALID -> {
                    error(peek(), "invalid token");
                    return;
                }
                case Token.TK_HASH -> {
                    error(peek(), "directive must appear before any declaration");
                    return;
                }
                case Token.TK_USING -> UsingDirective(beforeDeclaration);
                default -> {
                    beforeDeclaration = false;
                    try {
                        if (!GlobalDeclaration()) {
                            return;
                        }
                    } catch (FatalError e) {
                        // fatal error
                        return;
                    }
                }
            }
        }
    }

    /**
     * <pre>{@literal
     * UsingDirective
     *     : USING IDENTIFIER EQ IDENTIFIER SEMICOLON
     * }</pre>
     */
    private void UsingDirective(boolean beforeDeclaration) {
        // type alias
        long start = nextRawToken();
        if (!beforeDeclaration) {
            error(start, "'using' directive must appear before any other declaration");
        }
        long left = expectIdentifier();
        expect(Token.TK_EQ, "'='");
        long right = expect(Token.TK_IDENTIFIER, "a type name");
        final Context context = mCompiler.getContext();
        final String name = text(right);
        final Type type;
        if (context.getSymbolTable().find(name) instanceof Type existing) {
            type = existing;
        } else {
            error(right, "no type named '" + name + "'");
            type = context.getTypes().mPoison;
        }
        expect(Token.TK_SEMICOLON, "';'");
        context.getSymbolTable().insert(context,
                Type.makeAliasType(position(left), text(left), type.resolve()));
    }

    private boolean GlobalDeclaration() {
        long start = peek();
        if (Token.kind(start) == Token.TK_SEMICOLON) {
            // empty declaration, no elements
            nextToken();
            return true;
        }
        Modifiers modifiers = Modifiers();
        long peek = peek();

        if (Token.kind(peek) == Token.TK_IDENTIFIER &&
                !mCompiler.getContext().getSymbolTable().isType(text(peek))) {
            // we have an identifier that's not a type, could be the start of an interface block
            return InterfaceBlock(modifiers);
        }

        if (Token.kind(peek) == Token.TK_SEMICOLON) {
            nextToken();
            //TODO pure modifiers decl
            return true;
        }

        if (Token.kind(peek) == Token.TK_STRUCT) {
            Type type = StructDeclaration();
            if (type != null) {
                long name = checkIdentifier();
                if (name != Token.NO_TOKEN) {
                    GlobalVarDeclarationRest(rangeFrom(name), modifiers, type, name);
                } else {
                    expect(Token.TK_SEMICOLON, "';' to complete structure definition");
                }
            }
            return true;
        }

        Type type = TypeSpecifier(modifiers);
        if (type == null) {
            return false;
        }

        long name = expectIdentifier();

        if (checkNext(Token.TK_LPAREN)) {
            return FunctionDeclarationRest(position(start), modifiers, type, name);
        } else {
            GlobalVarDeclarationRest(position(start), modifiers, type, name);
            return true;
        }
    }

    private boolean InterfaceBlock(@Nonnull Modifiers modifiers) {
        long name = expectIdentifier();
        String blockName = text(name);
        int pos = rangeFrom(modifiers.mPosition);
        if (!peek(Token.TK_LBRACE)) {
            error(name, "no type named '" + blockName + "'");
            return false;
        }
        nextToken();
        Context context = mCompiler.getContext();
        List<Type.Field> fields = new ArrayList<>();
        do {
            int startPos = position(peek());
            Modifiers fieldModifiers = Modifiers();
            Type baseType = TypeSpecifier(fieldModifiers);
            if (baseType == null) {
                return false;
            }
            do {
                long fieldName = expectIdentifier();
                Type fieldType = ArraySpecifier(startPos, baseType);
                if (fieldType == null) {
                    return false;
                }
                if (checkNext(Token.TK_EQ)) {
                    Expression init = AssignmentExpression();
                    if (init == null) {
                        return false;
                    }
                    context.error(init.mPosition, "initializers are not permitted in interface blocks");
                }
                fields.add(new Type.Field(
                        rangeFrom(startPos),
                        fieldModifiers,
                        fieldType,
                        text(fieldName)
                ));
            } while (checkNext(Token.TK_COMMA));

            expect(Token.TK_SEMICOLON, "';' to complete member declaration");
        } while (!checkNext(Token.TK_RBRACE));

        Type type = Type.makeStructType(
                context,
                pos,
                blockName,
                fields,
                /*interfaceBlock*/ true);
        context.getSymbolTable().insert(context, type);

        long instanceName = checkIdentifier();
        String instanceNameText = "";
        if (instanceName != Token.NO_TOKEN) {
            instanceNameText = text(instanceName);
            type = ArraySpecifier(pos, type);
            if (type == null) {
                return false;
            }
        }
        expect(Token.TK_SEMICOLON, "';' to complete interface block");

        InterfaceBlock block = InterfaceBlock.convert(context, pos, modifiers, type, instanceNameText);
        if (block != null) {
            mUniqueElements.add(block);
            return true;
        }
        return false;
    }

    private boolean FunctionDeclarationRest(int start,
                                            Modifiers modifiers,
                                            Type returnType,
                                            long name) {
        List<Variable> parameters = new ArrayList<>();
        if (!peek(Token.TK_RPAREN)) {
            if (peek(Token.TK_IDENTIFIER) && "void".equals(text(peek()))) {
                // '(void)' means no parameters.
                nextToken();
            } else {
                do {
                    Variable parameter = Parameter();
                    if (parameter == null) {
                        return false;
                    }
                    parameters.add(parameter);
                } while (checkNext(Token.TK_COMMA));
            }
        }
        expect(Token.TK_RPAREN, "')' to complete parameter list");

        FunctionDecl decl = FunctionDecl.convert(
                mCompiler.getContext(),
                rangeFrom(start),
                modifiers,
                text(name),
                parameters,
                returnType
        );

        Context context = mCompiler.getContext();
        if (peek(Token.TK_SEMICOLON)) {
            nextToken();
            if (decl == null) {
                return false;
            }
            mUniqueElements.add(
                    new FunctionPrototype(decl.mPosition, decl, context.isBuiltin())
            );
            return true;
        } else {
            mCompiler.getContext()
                    .enterScope();
            try {
                if (decl != null) {
                    for (Variable param : decl.getParameters()) {
                        context.getSymbolTable().insert(mCompiler.getContext(), param);
                    }
                }

                long blockStart = peek();
                BlockStatement block = ScopedBlock();

                if (decl == null) {
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
                mUniqueElements.add(function);
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
        Modifiers modifiers = Modifiers();
        Type type = TypeSpecifier(modifiers);
        if (type == null) {
            return null;
        }
        long name = checkIdentifier();
        String nameText = "";
        if (name != Token.NO_TOKEN) {
            nameText = text(name);
            type = ArraySpecifier(pos, type);
            if (type == null) {
                return null;
            }
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
        long start = expect(Token.TK_LBRACE, "'{'");
        List<Statement> statements = new ArrayList<>();
        for (;;) {
            if (checkNext(Token.TK_RBRACE)) {
                int pos = rangeFrom(start);
                return BlockStatement.makeBlock(pos, statements);
            } else if (peek(Token.TK_END_OF_FILE)) {
                error(peek(), "expected '}', but found end of file");
                return null;
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
            if (checkNext(Token.TK_EQ)) {
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
                mUniqueElements.add(
                        new GlobalVariableDecl(variableDecl)
                );
            }
        } while (checkNext(Token.TK_COMMA));

        expect(Token.TK_SEMICOLON, "';' to complete global variable declaration");
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
        while (peek(Token.TK_COMMA)) {
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
                case Token.TK_EQ        -> Operator.    ASSIGN;
                case Token.TK_PLUSEQ    -> Operator.ADD_ASSIGN;
                case Token.TK_MINUSEQ   -> Operator.SUB_ASSIGN;
                case Token.TK_STAREQ    -> Operator.MUL_ASSIGN;
                case Token.TK_SLASHEQ   -> Operator.DIV_ASSIGN;
                case Token.TK_PERCENTEQ -> Operator.MOD_ASSIGN;
                case Token.TK_LTLTEQ    -> Operator.SHL_ASSIGN;
                case Token.TK_GTGTEQ    -> Operator.SHR_ASSIGN;
                case Token.TK_AMPEQ     -> Operator.AND_ASSIGN;
                case Token.TK_PIPEEQ    -> Operator. OR_ASSIGN;
                case Token.TK_CARETEQ   -> Operator.XOR_ASSIGN;
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
        if (!peek(Token.TK_QUES)) {
            return base;
        }
        nextToken();
        Expression whenTrue = Expression();
        if (whenTrue == null) {
            return null;
        }
        expect(Token.TK_COLON, "':'");
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
        while (peek(Token.TK_PIPEPIPE)) {
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
        while (peek(Token.TK_CARETCARET)) {
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
        while (peek(Token.TK_AMPAMP)) {
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
        while (peek(Token.TK_PIPE)) {
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
        while (peek(Token.TK_CARET)) {
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
        while (peek(Token.TK_AMP)) {
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
                case Token.TK_EQEQ   -> Operator.EQ;
                case Token.TK_BANGEQ -> Operator.NE;
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
                case Token.TK_LT   -> Operator.LT;
                case Token.TK_GT   -> Operator.GT;
                case Token.TK_LTEQ -> Operator.LE;
                case Token.TK_GTEQ -> Operator.GE;
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
                case Token.TK_LTLT -> Operator.SHL;
                case Token.TK_GTGT -> Operator.SHR;
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
                case Token.TK_PLUS  -> Operator.ADD;
                case Token.TK_MINUS -> Operator.SUB;
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
                case Token.TK_STAR    -> Operator.MUL;
                case Token.TK_SLASH   -> Operator.DIV;
                case Token.TK_PERCENT -> Operator.MOD;
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
            case Token.TK_PLUSPLUS   -> Operator.INC;
            case Token.TK_MINUSMINUS -> Operator.DEC;
            case Token.TK_PLUS       -> Operator.ADD;
            case Token.TK_MINUS      -> Operator.SUB;
            case Token.TK_BANG       -> Operator.LOGICAL_NOT;
            case Token.TK_TILDE      -> Operator.BITWISE_NOT;
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
                case Token.TK_LBRACKET -> {
                    nextToken();
                    Expression index = null;
                    if (!peek(Token.TK_RBRACKET)) {
                        index = Expression();
                        if (index == null) {
                            return null;
                        }
                    }
                    expect(Token.TK_RBRACKET, "']' to complete array specifier");
                    int pos = rangeFrom(result.mPosition);
                    result = expressionOrPoison(pos,
                            IndexExpression.convert(mCompiler.getContext(), pos, result, index));
                }
                case Token.TK_DOT -> {
                    nextToken();
                    // swizzle mask, field access, method reference
                    long name = expect(Token.TK_IDENTIFIER, "identifier");
                    String text = text(name);
                    int pos = rangeFrom(result.mPosition);
                    int namePos = rangeFrom(name);
                    result = expressionOrPoison(pos,
                            FieldAccess.convert(mCompiler.getContext(), pos, result, namePos, text));
                }
                case Token.TK_LPAREN -> {
                    nextToken();
                    // constructor call, function call, method call
                    List<Expression> args = new ArrayList<>();
                    if (!peek(Token.TK_RPAREN)) {
                        if (peek(Token.TK_IDENTIFIER) && "void".equals(text(peek()))) {
                            // '(void)' means no arguments.
                            nextToken();
                        } else {
                            do {
                                Expression expr = AssignmentExpression();
                                if (expr == null) {
                                    return null;
                                }
                                args.add(expr);
                            } while (checkNext(Token.TK_COMMA));
                        }
                    }
                    expect(Token.TK_RPAREN, "')' to complete invocation");
                    int pos = rangeFrom(result.mPosition);
                    result = expressionOrPoison(pos,
                            FunctionCall.convert(mCompiler.getContext(), pos, result, args));
                }
                case Token.TK_PLUSPLUS, Token.TK_MINUSMINUS -> {
                    nextToken();
                    Operator op = Token.kind(t) == Token.TK_PLUSPLUS
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
            case Token.TK_IDENTIFIER -> {
                nextToken();
                yield mCompiler.getContext().convertIdentifier(position(t), text(t));
            }
            case Token.TK_INTLITERAL -> IntLiteral();
            case Token.TK_FLOATLITERAL -> FloatLiteral();
            case Token.TK_STRINGLITERAL -> {
                nextToken();
                error(t, "string literal '" + text(t) + "' is not permitted");
                yield null;
            }
            case Token.TK_TRUE, Token.TK_FALSE -> BooleanLiteral();
            case Token.TK_LPAREN -> {
                nextToken();
                Expression result = Expression();
                if (result != null) {
                    expect(Token.TK_RPAREN, "')' to complete expression");
                    result.mPosition = rangeFrom(t);
                    yield result;
                }
                yield null;
            }
            default -> {
                nextToken();
                error(t, "expected identifier, literal constant or parenthesized expression, but found '" +
                        text(t) + "'");
                throw new FatalError();
            }
        };
    }

    /**
     * INTLITERAL
     */
    @Nullable
    private Literal IntLiteral() {
        long token = expect(Token.TK_INTLITERAL, "integer literal");
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
        long token = expect(Token.TK_FLOATLITERAL, "float literal");
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
            case Token.TK_TRUE -> Literal.makeBoolean(
                    mCompiler.getContext(),
                    position(token),
                    true);
            case Token.TK_FALSE -> Literal.makeBoolean(
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
    private Modifiers Modifiers() {
        long start = peek();
        Modifiers modifiers = new Modifiers(Position.NO_POS);
        if (checkNext(Token.TK_LAYOUT)) {
            Layout(modifiers);
        }
        for (;;) {
            int mask = switch (Token.kind(peek())) {
                case Token.TK_SMOOTH -> Modifiers.kSmooth_Flag;
                case Token.TK_FLAT -> Modifiers.kFlat_Flag;
                case Token.TK_NOPERSPECTIVE -> Modifiers.kNoPerspective_Flag;
                case Token.TK_CONST -> Modifiers.kConst_Flag;
                case Token.TK_UNIFORM -> Modifiers.kUniform_Flag;
                case Token.TK_IN -> Modifiers.kIn_Flag;
                case Token.TK_OUT -> Modifiers.kOut_Flag;
                case Token.TK_INOUT -> Modifiers.kIn_Flag | Modifiers.kOut_Flag;
                case Token.TK_COHERENT -> Modifiers.kCoherent_Flag;
                case Token.TK_VOLATILE -> Modifiers.kVolatile_Flag;
                case Token.TK_RESTRICT -> Modifiers.kRestrict_Flag;
                case Token.TK_READONLY -> Modifiers.kReadOnly_Flag;
                case Token.TK_WRITEONLY -> Modifiers.kWriteOnly_Flag;
                case Token.TK_BUFFER -> Modifiers.kBuffer_Flag;
                case Token.TK_WORKGROUP -> Modifiers.kWorkgroup_Flag;
                case Token.TK_SUBROUTINE -> Modifiers.kSubroutine_Flag;
                case Token.TK_PURE -> Modifiers.kPure_Flag;
                case Token.TK_INLINE -> Modifiers.kInline_Flag;
                case Token.TK_NOINLINE -> Modifiers.kNoInline_Flag;
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
        expect(Token.TK_LPAREN, "'('");
        do {
            long name = expect(Token.TK_IDENTIFIER, "identifier");
            String text = text(name);
            int pos = position(name);
            int mask = switch (text) {
                case "origin_upper_left" -> Layout.kOriginUpperLeft_LayoutFlag;
                case "pixel_center_integer" -> Layout.kPixelCenterInteger_LayoutFlag;
                case "early_fragment_tests" -> Layout.kEarlyFragmentTests_LayoutFlag;
                case "blend_support_all_equations" -> Layout.kBlendSupportAllEquations_LayoutFlag;
                case "push_constant" -> Layout.kPushConstant_LayoutFlag;
                case "std140" -> Layout.kStd140_LayoutFlag;
                case "std430" -> Layout.kStd430_LayoutFlag;
                case "location" -> Layout.kLocation_LayoutFlag;
                case "component" -> Layout.kComponent_LayoutFlag;
                case "index" -> Layout.kIndex_LayoutFlag;
                case "binding" -> Layout.kBinding_LayoutFlag;
                case "offset" -> Layout.kOffset_LayoutFlag;
                case "set" -> Layout.kSet_LayoutFlag;
                case "input_attachment_index" -> Layout.kInputAttachmentIndex_LayoutFlag;
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
                    case Layout.kSet_LayoutFlag -> layout.mSet = LayoutInt();
                    case Layout.kInputAttachmentIndex_LayoutFlag -> layout.mInputAttachmentIndex = LayoutInt();
                    case Layout.kBuiltin_LayoutFlag -> layout.mBuiltin = LayoutBuiltin();
                }
            } else {
                int builtin = findBuiltinValue(text);
                if (builtin != -1) {
                    modifiers.setLayoutFlag(mCompiler.getContext(), Layout.kBuiltin_LayoutFlag, text, pos);
                    modifiers.layout().mBuiltin = builtin;
                } else {
                    warning(name, "unrecognized layout qualifier '" + text + "'");
                    if (checkNext(Token.TK_EQ)) {
                        nextToken();
                    }
                }
            }
        } while (checkNext(Token.TK_COMMA));
        expect(Token.TK_RPAREN, "')'");
    }

    private int LayoutInt() {
        expect(Token.TK_EQ, "'='");
        long token = expect(Token.TK_INTLITERAL, "integer literal");
        return LayoutIntValue(token);
    }

    private int LayoutBuiltin() {
        expect(Token.TK_EQ, "'='");
        if (peek(Token.TK_INTLITERAL)) {
            long token = nextToken();
            return LayoutIntValue(token);
        }
        long name = expectIdentifier();
        String text = text(name);
        int builtin = findBuiltinValue(text);
        if (builtin == -1) {
            error(name, "unrecognized built-in name '" + text + "'");
        }
        return builtin;
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

    private static int findBuiltinValue(@Nonnull String text) {
        return switch (text) {
            case "position" -> Spv.SpvBuiltInPosition;
            case "vertex_id" -> Spv.SpvBuiltInVertexId;
            case "instance_id" -> Spv.SpvBuiltInInstanceId;
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
            default -> -1;
        };
    }

    /**
     * <pre>{@literal
     * TypeSpecifier
     *     : IDENTIFIER ArraySpecifier*
     * }</pre>
     */
    @Nullable
    private Type TypeSpecifier(Modifiers modifiers) {
        long start = expect(Token.TK_IDENTIFIER, "a type name");
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
        while (peek(Token.TK_LBRACKET)) {
            nextToken();
            Expression size = null;
            if (!peek(Token.TK_RBRACKET)) {
                size = Expression();
                if (size == null) {
                    return null;
                }
            }
            expect(Token.TK_RBRACKET, "']' to complete array specifier");
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
        if (checkNext(Token.TK_EQ)) {
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

        while (checkNext(Token.TK_COMMA)) {
            name = expectIdentifier();
            type = ArraySpecifier(pos, baseType);
            if (type == null) {
                break;
            }
            init = null;
            if (checkNext(Token.TK_EQ)) {
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
        expect(Token.TK_SEMICOLON, "';' to complete local variable declaration");
        pos = rangeFrom(pos);
        return statementOrEmpty(pos, result);
    }

    @Nullable
    private Statement VarDeclarationOrExpressionStatement() {
        long peek = peek();
        if (Token.kind(peek) == Token.TK_CONST) {
            int pos = position(peek);
            Modifiers modifiers = Modifiers();
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
        expect(Token.TK_SEMICOLON, "';' to complete expression statement");
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
        expect(Token.TK_STRUCT, "'struct'");
        long typeName = expectIdentifier();
        expect(Token.TK_LBRACE, "'{'");
        Context context = mCompiler.getContext();
        List<Type.Field> fields = new ArrayList<>();
        do {
            int startPos = position(peek());
            Modifiers fieldModifiers = Modifiers();
            Type baseType = TypeSpecifier(fieldModifiers);
            if (baseType == null) {
                return null;
            }
            do {
                long fieldName = expectIdentifier();
                Type fieldType = ArraySpecifier(startPos, baseType);
                if (fieldType == null) {
                    return null;
                }
                if (checkNext(Token.TK_EQ)) {
                    Expression init = AssignmentExpression();
                    if (init == null) {
                        return null;
                    }
                    context.error(init.mPosition, "initializers are not permitted in structures");
                }
                fields.add(new Type.Field(
                        rangeFrom(startPos),
                        fieldModifiers,
                        fieldType,
                        text(fieldName)
                ));
            } while (checkNext(Token.TK_COMMA));

            expect(Token.TK_SEMICOLON, "';' to complete member declaration");
        } while (!checkNext(Token.TK_RBRACE));
        StructDefinition definition = StructDefinition.convert(context,
                rangeFrom(start),
                text(typeName),
                fields);
        if (definition == null) {
            return null;
        }
        mUniqueElements.add(definition);
        return definition.getType();
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
            case Token.TK_BREAK -> {
                long start = nextToken();
                expect(Token.TK_SEMICOLON, "';' after 'break'");
                yield BreakStatement.make(rangeFrom(start));
            }
            case Token.TK_CONTINUE -> {
                long start = nextToken();
                expect(Token.TK_SEMICOLON, "';' after 'continue'");
                yield ContinueStatement.make(rangeFrom(start));
            }
            case Token.TK_DISCARD -> {
                long start = nextToken();
                expect(Token.TK_SEMICOLON, "';' after 'discard'");
                int pos = rangeFrom(start);
                yield statementOrEmpty(pos, DiscardStatement.convert(mCompiler.getContext(), pos));
            }
            case Token.TK_RETURN -> {
                long start = nextToken();
                Expression expression = null;
                if (!peek(Token.TK_SEMICOLON)) {
                    expression = Expression();
                    if (expression == null) {
                        yield null;
                    }
                }
                expect(Token.TK_SEMICOLON, "';' to complete return expression");
                yield ReturnStatement.make(rangeFrom(start), expression);
            }
            case Token.TK_IF -> IfStatement();
            case Token.TK_FOR -> ForStatement();
            case Token.TK_SWITCH -> SwitchStatement();
            case Token.TK_LBRACE -> {
                mCompiler.getContext()
                        .enterScope();
                try {
                    yield ScopedBlock();
                } finally {
                    mCompiler.getContext()
                            .leaveScope();
                }
            }
            case Token.TK_SEMICOLON -> {
                long t = nextToken();
                yield new EmptyStatement(position(t));
            }
            case Token.TK_CONST, Token.TK_IDENTIFIER -> VarDeclarationOrExpressionStatement();
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
        long start = expect(Token.TK_IF, "'if'");
        expect(Token.TK_LPAREN, "'('");
        Expression test = Expression();
        if (test == null) {
            return null;
        }
        expect(Token.TK_RPAREN, "')'");
        Statement whenTrue = Statement();
        if (whenTrue == null) {
            return null;
        }
        Statement whenFalse = null;
        if (checkNext(Token.TK_ELSE)) {
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
     * SwitchCaseBody
     *     : COLON Statement*
     * }</pre>
     */
    private boolean SwitchCaseBody(List<Expression> values,
                                   List<Statement> caseBlocks,
                                   Expression caseValue) {
        expect(Token.TK_COLON, "':'");
        ArrayList<Statement> statements = new ArrayList<>();
        while (!peek(Token.TK_RBRACE) &&
                !peek(Token.TK_CASE) &&
                !peek(Token.TK_DEFAULT)) {
            Statement s = Statement();
            if (s == null) {
                return false;
            }
            statements.add(s);
        }
        values.add(caseValue);
        caseBlocks.add(BlockStatement.make(Position.NO_POS,
                statements, false));
        return true;
    }

    /**
     * <pre>{@literal
     * SwitchStatement
     *     : SWITCH LPAREN Expression RPAREN LBRACE Statement* RBRACE
     * }</pre>
     */
    private Statement SwitchStatement() {
        long start = expect(Token.TK_SWITCH, "'switch'");
        expect(Token.TK_LPAREN, "'('");
        Expression init = Expression();
        if (init == null) {
            return null;
        }
        expect(Token.TK_RPAREN, "')'");
        expect(Token.TK_LBRACE, "'{'");

        ArrayList<Expression> values = new ArrayList<>();
        ArrayList<Statement> caseBlocks = new ArrayList<>();

        //TODO symbol table inside switch block

        while (checkNext(Token.TK_CASE)) {
            Expression caseValue = Expression();
            if (caseValue == null) {
                return null;
            }
            if (!SwitchCaseBody(values, caseBlocks, caseValue)) {
                return null;
            }
        }
        //TODO allow default label to be other than last, need to update the rest part of compiler
        if (peek(Token.TK_DEFAULT)) {
            long defaultToken = nextToken();
            if (!SwitchCaseBody(values, caseBlocks, null)) {
                return null;
            }
            if (peek(Token.TK_CASE)) {
                error(defaultToken, "'default' should be the last case");
                return null;
            }
        }
        expect(Token.TK_RBRACE, "'}'");

        int pos = rangeFrom(start);
        return statementOrEmpty(pos, SwitchStatement.convert(mCompiler.getContext(),
                pos, init, values, caseBlocks));
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
        long start = expect(Token.TK_FOR, "'for'");
        expect(Token.TK_LPAREN, "'('");
        mCompiler.getContext()
                .enterScope();
        try {
            Statement init = null;
            if (peek(Token.TK_SEMICOLON)) {
                // An empty init-statement.
                nextToken();
            } else {
                init = VarDeclarationOrExpressionStatement();
                if (init == null) {
                    return null;
                }
            }

            Expression cond = null;
            if (!peek(Token.TK_SEMICOLON)) {
                cond = Expression();
                if (cond == null) {
                    return null;
                }
            }

            expect(Token.TK_SEMICOLON, "';' to complete condition statement");

            Expression step = null;
            if (!peek(Token.TK_SEMICOLON)) {
                step = Expression();
                if (step == null) {
                    return null;
                }
            }

            expect(Token.TK_RPAREN, "')' to complete 'for' statement");

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
