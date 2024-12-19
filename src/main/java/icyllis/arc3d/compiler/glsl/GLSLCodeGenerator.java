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

package icyllis.arc3d.compiler.glsl;

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.analysis.Analysis;
import icyllis.arc3d.compiler.tree.*;
import icyllis.arc3d.core.MathUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.spvc.Spv;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Standard GLSL code generator for OpenGL 3.3 or above and Vulkan 1.0 or above (Vulkan GLSL).
 * <p>
 * A GLSL shader is a UTF-8 encoded string. However, our compiler only outputs ASCII characters.
 */
//TODO WIP
public final class GLSLCodeGenerator extends CodeGenerator {

    public final TargetApi mOutputTarget;
    public final GLSLVersion mOutputVersion;

    private Output mOutput;
    private int mIndentation = 0;
    private boolean mAtLineStart = false;

    private final boolean mPrettyPrint;

    public GLSLCodeGenerator(@NonNull Context context,
                             @NonNull TranslationUnit translationUnit,
                             @NonNull ShaderCaps shaderCaps) {
        super(context, translationUnit);
        mOutputTarget = Objects.requireNonNullElse(shaderCaps.mTargetApi, TargetApi.OPENGL_4_5);
        mOutputVersion = Objects.requireNonNullElse(shaderCaps.mGLSLVersion, GLSLVersion.GLSL_450);
        mPrettyPrint = !getContext().getOptions().mMinifyCode;
    }

    @Override
    public @Nullable ByteBuffer generateCode() {
        CodeBuffer body = new CodeBuffer();
        mOutput = body;
        // Write all the program elements except for functions.
        for (var e : mTranslationUnit) {
            switch (e.getKind()) {
                case GLOBAL_VARIABLE -> writeGlobalVariableDecl(((GlobalVariableDecl) e).getVariableDecl());
                case INTERFACE_BLOCK -> writeInterfaceBlock((InterfaceBlock) e);
                case FUNCTION_PROTOTYPE -> writeFunctionPrototype((FunctionPrototype) e);
                case STRUCT_DEFINITION -> writeStructDefinition((StructDefinition) e);
            }
        }
        // Emit prototypes for every built-in function; these aren't always added in perfect order.
        for (var e : mTranslationUnit.getSharedElements()) {
            if (e instanceof FunctionDefinition functionDef) {
                writeFunctionDecl(functionDef.getFunctionDecl());
                writeLine(';');
            }
        }
        // Write the functions last.
        // Why don't we write things in their original order? Because the Inliner likes to move function
        // bodies around. After inlining, code can inadvertently move upwards, above ProgramElements
        // that the code relies on.
        for (var e : mTranslationUnit) {
            if (e instanceof FunctionDefinition functionDef) {
                writeFunctionDefinition(functionDef);
            }
        }

        NativeOutput output = new NativeOutput(MathUtil.align4(body.size() + 20));
        mOutput = output;
        write(mOutputVersion.mVersionDecl);
        output.writeString(body.elements(), body.size());
        mOutput = null;
        if (getContext().getErrorHandler().errorCount() != 0) {
            return null;
        }
        return output.detach();
    }

    private void write(char c) {
        assert c != '\n' && c <= 0x7F;
        if (mAtLineStart && mPrettyPrint) {
            for (int i = 0; i < mIndentation; i++) {
                mOutput.writeString8("    ");
            }
        }
        mOutput.write(c);
        mAtLineStart = false;
    }

    private void write(@NonNull String s) {
        if (s.isEmpty()) {
            return;
        }
        if (mAtLineStart && mPrettyPrint) {
            for (int i = 0; i < mIndentation; i++) {
                mOutput.writeString8("    ");
            }
        }
        if (!mOutput.writeString8(s)) {
            getContext().error(Position.NO_POS, "invalid string '" + s + "'");
        }
        mAtLineStart = false;
    }

    private void writeLine() {
        mOutput.write('\n');
        mAtLineStart = true;
    }

    private void writeLine(char c) {
        write(c);
        writeLine();
    }

    private void writeLine(@NonNull String s) {
        write(s);
        writeLine();
    }

    private void finishLine() {
        if (!mAtLineStart) {
            writeLine();
        }
    }

    private void writeIdentifier(@NonNull String identifier) {
        write(identifier);
    }

    private void writeModifiers(@NonNull Modifiers modifiers) {
        Layout layout = modifiers.layout();
        if (layout != null) {
            write(layout.toString(mPrettyPrint));
        }

        int flags = modifiers.flags();
        if ((flags & Modifiers.kFlat_Flag) != 0) {
            write("flat ");
        }
        if ((flags & Modifiers.kNoPerspective_Flag) != 0) {
            write("noperspective ");
        }
        if ((flags & Modifiers.kConst_Flag) != 0) {
            write("const ");
        }
        if ((flags & Modifiers.kUniform_Flag) != 0) {
            write("uniform ");
        }
        switch (flags & (Modifiers.kIn_Flag | Modifiers.kOut_Flag)) {
            case Modifiers.kIn_Flag -> write("in ");
            case Modifiers.kOut_Flag -> write("out ");
            case Modifiers.kIn_Flag | Modifiers.kOut_Flag -> write("inout ");
        }
        if ((flags & Modifiers.kCoherent_Flag) != 0) {
            write("coherent ");
        }
        if ((flags & Modifiers.kVolatile_Flag) != 0) {
            write("volatile ");
        }
        if ((flags & Modifiers.kRestrict_Flag) != 0) {
            write("restrict ");
        }
        if ((flags & Modifiers.kReadOnly_Flag) != 0) {
            write("readonly ");
        }
        if ((flags & Modifiers.kWriteOnly_Flag) != 0) {
            write("writeonly ");
        }
        if ((flags & Modifiers.kBuffer_Flag) != 0) {
            write("buffer ");
        }

        if ((flags & Modifiers.kWorkgroup_Flag) != 0) {
            write("shared ");
        }
    }

    private @NonNull String getTypeName(@NonNull Type type) {
        switch (type.getTypeKind()) {
            case Type.kScalar_TypeKind -> {
                if (type.isFloat() && type.getWidth() == 32) {
                    return "float";
                }
                if (type.isSigned()) {
                    assert type.getWidth() == 32;
                    return "int";
                }
                if (type.isUnsigned()) {
                    assert type.getWidth() == 32;
                    return "uint";
                }
                return type.getName();
            }
            case Type.kVector_TypeKind -> {
                Type component = type.getComponentType();
                String result;
                if (component.isFloat() && component.getWidth() == 32) {
                    result = "vec";
                } else if (component.isSigned()) {
                    result = "ivec";
                } else if (component.isUnsigned()) {
                    result = "uvec";
                } else if (component.isBoolean()) {
                    result = "bvec";
                } else {
                    assert false;
                    result = "";
                }
                return result + type.getRows();
            }
            case Type.kMatrix_TypeKind -> {
                Type component = type.getComponentType();
                String result;
                if (component.isFloat() && component.getWidth() == 32) {
                    result = "mat";
                } else {
                    assert false;
                    result = "";
                }
                result += type.getCols();
                if (type.getCols() != type.getRows()) {
                    result += "x" + type.getRows();
                }
                return result;
            }
            case Type.kArray_TypeKind -> {
                String baseName = getTypeName(type.getElementType());
                return Type.getArrayName(baseName, type.getArraySize());
            }
            default -> {
                return type.getName();
            }
        }
    }

    private void writeType(@NonNull Type type) {
        writeIdentifier(getTypeName(type));
    }

    private @NonNull String getTypePrecision(@NonNull Type type) {
        if (getContext().getOptions().mUsePrecisionQualifiers) {
            switch (type.getTypeKind()) {
                case Type.kScalar_TypeKind -> {
                    if (type.getWidth() == 32) {
                        if (type.getMinWidth() == 32) {
                            return "highp ";
                        }
                        if (type.getMinWidth() == 16) {
                            return getContext().getOptions().mForceHighPrecision ? "highp " : "mediump ";
                        }
                    }
                    return "";
                }
                case Type.kVector_TypeKind,
                     Type.kMatrix_TypeKind -> {
                    return getTypePrecision(type.getComponentType());
                }
                case Type.kArray_TypeKind -> {
                    return getTypePrecision(type.getElementType());
                }
            }
        }
        return "";
    }

    private void writeTypePrecision(@NonNull Type type) {
        write(getTypePrecision(type));
    }

    private void writeGlobalVariableDecl(VariableDecl decl) {
        if (decl.getVariable().getModifiers().layoutBuiltin() == -1) {
            writeVariableDecl(decl);
            finishLine();
        }
    }

    private void writeInterfaceBlock(@NonNull InterfaceBlock block) {
        Type blockType = block.getVariable().getType().getElementType();
        boolean hasNonBuiltin = false;
        for (var f : blockType.getFields()) {
            if ((f.modifiers().layoutFlags() & Layout.kBuiltin_LayoutFlag) == 0) {
                hasNonBuiltin = true;
                break;
            }
        }
        if (!hasNonBuiltin) {
            // Blocks that only contain builtin variables do not need to exist
            return;
        }
        writeModifiers(block.getVariable().getModifiers());
        writeType(blockType);
        writeLine(" {");
        mIndentation++;
        for (var f : blockType.getFields()) {
            if ((f.modifiers().layoutFlags() & Layout.kBuiltin_LayoutFlag) == 0) {
                writeModifiers(f.modifiers());
                writeTypePrecision(f.type());
                writeType(f.type());
                write(' ');
                writeIdentifier(f.name());
                writeLine(';');
            }
        }
        mIndentation--;
        write('}');
        if (!block.getInstanceName().isEmpty()) {
            write(' ');
            writeIdentifier(block.getInstanceName());
            if (block.getArraySize() > 0) {
                write('[');
                write(Integer.toString(block.getArraySize()));
                write(']');
            }
        }
        writeLine(';');
    }

    private void writeFunctionDecl(FunctionDecl decl) {
        writeTypePrecision(decl.getReturnType());
        writeType(decl.getReturnType());
        write(' ');
        writeIdentifier(decl.getMangledName());
        write('(');
        var parameters = decl.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            var param = parameters.get(i);
            if (i != 0) {
                if (mPrettyPrint) {
                    write(", ");
                } else {
                    write(',');
                }
            }
            writeModifiers(param.getModifiers());
            if (param.getName().isEmpty()) {
                Type type = param.getType();
                writeTypePrecision(type);
                writeType(type);
            } else {
                Type baseType = param.getType();
                int arraySize = 0;
                if (baseType.isArray()) {
                    arraySize = baseType.getArraySize();
                    baseType = baseType.getElementType();
                }
                writeTypePrecision(baseType);
                writeType(baseType);
                write(' ');
                writeIdentifier(param.getName());
                if (arraySize > 0) {
                    write('[');
                    write(Integer.toString(arraySize));
                    write(']');
                }
            }
        }
        write(')');
    }

    private void writeFunctionDefinition(FunctionDefinition f) {
        writeFunctionDecl(f.getFunctionDecl());
        writeLine(" {");
        mIndentation++;

        for (var stmt : f.getBody().getStatements()) {
            if (!stmt.isEmpty()) {
                writeStatement(stmt);
                finishLine();
            }
        }

        mIndentation--;
        writeLine('}');
    }

    private void writeFunctionPrototype(FunctionPrototype f) {
        writeFunctionDecl(f.getFunctionDecl());
        writeLine(';');
    }

    private void writeStructDefinition(StructDefinition s) {
        Type type = s.getType();
        write("struct ");
        writeIdentifier(type.getName());
        writeLine(" {");
        mIndentation++;
        for (var f : type.getFields()) {
            writeModifiers(f.modifiers());
            Type baseType = f.type();
            int arraySize = 0;
            if (baseType.isArray()) {
                arraySize = baseType.getArraySize();
                baseType = baseType.getElementType();
            }
            writeTypePrecision(baseType);
            writeType(baseType);
            write(' ');
            writeIdentifier(f.name());
            if (arraySize > 0) {
                write('[');
                write(Integer.toString(arraySize));
                write(']');
            }
            writeLine(';');
        }
        mIndentation--;
        writeLine("};");
    }

    private void writeExpression(Expression expr, int parentPrecedence) {
        switch (expr.getKind()) {
            case LITERAL -> writeLiteral((Literal) expr);
            case PREFIX -> writePrefixExpression((PrefixExpression) expr, parentPrecedence);
            case POSTFIX -> writePostfixExpression((PostfixExpression) expr, parentPrecedence);
            case BINARY -> writeBinaryExpression((BinaryExpression) expr, parentPrecedence);
            case CONDITIONAL -> writeConditionalExpression((ConditionalExpression) expr, parentPrecedence);
            case VARIABLE_REFERENCE -> writeVariableReference((VariableReference) expr);
            case INDEX -> writeIndexExpression((IndexExpression) expr);
            case FIELD_ACCESS -> writeFieldAccess((FieldAccess) expr);
            case SWIZZLE -> writeSwizzle((Swizzle) expr);
            case FUNCTION_CALL -> writeFunctionCall((FunctionCall) expr);
            case CONSTRUCTOR_ARRAY_CAST -> writeExpression(((ConstructorArrayCast) expr).getArgument(), parentPrecedence);
            case CONSTRUCTOR_SCALAR_CAST,
                 CONSTRUCTOR_COMPOUND_CAST -> writeConstructorCast((ConstructorCall) expr, parentPrecedence);
            case CONSTRUCTOR_ARRAY,
                 CONSTRUCTOR_COMPOUND,
                 CONSTRUCTOR_VECTOR_SPLAT,
                 CONSTRUCTOR_DIAGONAL_MATRIX,
                 CONSTRUCTOR_MATRIX_RESIZE,
                 CONSTRUCTOR_STRUCT -> writeConstructorCall((ConstructorCall) expr);
            default -> getContext().error(expr.mPosition, "unsupported expression: " + expr.getKind());
        }
    }

    private String getBuiltinName(int builtin, String name) {
        return switch (builtin) {
            case Spv.SpvBuiltInPosition -> "gl_Position";
            case Spv.SpvBuiltInVertexId -> "gl_VertexID";
            case Spv.SpvBuiltInInstanceId -> "gl_InstanceID";
            case Spv.SpvBuiltInVertexIndex -> "gl_VertexIndex";
            case Spv.SpvBuiltInInstanceIndex -> "gl_InstanceIndex";
            case Spv.SpvBuiltInFragCoord -> "gl_FragCoord";
            default -> name;
        };
    }

    private void writeLiteral(Literal literal) {
        write(literal.toString());
    }

    private void writePrefixExpression(PrefixExpression p,
                                                  int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_PREFIX >= parentPrecedence);
        if (needsParens) {
            write('(');
        }
        write(p.getOperator().toString());
        writeExpression(p.getOperand(), Operator.PRECEDENCE_PREFIX);
        if (needsParens) {
            write(')');
        }
    }

    private void writePostfixExpression(PostfixExpression p,
                                        int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_POSTFIX >= parentPrecedence);
        if (needsParens) {
            write('(');
        }
        writeExpression(p.getOperand(), Operator.PRECEDENCE_POSTFIX);
        write(p.getOperator().toString());
        if (needsParens) {
            write(')');
        }
    }

    private void writeBinaryExpression(BinaryExpression expr, int parentPrecedence) {
        Expression left = expr.getLeft();
        Expression right = expr.getRight();
        Operator op = expr.getOperator();
        int precedence = op.getBinaryPrecedence();
        boolean needsParens = (precedence >= parentPrecedence);
        if (needsParens) {
            write('(');
        }
        writeExpression(left, precedence);
        write(mPrettyPrint ? op.getPrettyName() : op.toString());
        writeExpression(right, precedence);
        if (needsParens) {
            write(')');
        }
    }

    private void writeConditionalExpression(ConditionalExpression expr,
                                            int parentPrecedence) {
        boolean needsParens = (Operator.PRECEDENCE_CONDITIONAL >= parentPrecedence);
        if (needsParens) {
            write('(');
        }
        writeExpression(expr.getCondition(), Operator.PRECEDENCE_CONDITIONAL);
        if (mPrettyPrint) {
            write(" ? ");
        } else {
            write('?');
        }
        writeExpression(expr.getWhenTrue(), Operator.PRECEDENCE_CONDITIONAL);
        if (mPrettyPrint) {
            write(" : ");
        } else {
            write(':');
        }
        writeExpression(expr.getWhenFalse(), Operator.PRECEDENCE_CONDITIONAL);
        if (needsParens) {
            write(')');
        }
    }

    private void writeVariableReference(VariableReference ref) {
        var variable = ref.getVariable();
        write(getBuiltinName(variable.getModifiers().layoutBuiltin(), variable.getName()));
    }

    private void writeIndexExpression(IndexExpression expr) {
        writeExpression(expr.getBase(), Operator.PRECEDENCE_POSTFIX);
        write('[');
        writeExpression(expr.getIndex(), Operator.PRECEDENCE_EXPRESSION);
        write(']');
    }

    private void writeFieldAccess(FieldAccess expr) {
        if (!expr.isAnonymousBlock()) {
            writeExpression(expr.getBase(), Operator.PRECEDENCE_POSTFIX);
            write('.');
        }
        var field = expr.getBase().getType().getFields().get(expr.getFieldIndex());
        write(getBuiltinName(field.modifiers().layoutBuiltin(), field.name()));
    }

    private void writeSwizzle(Swizzle swizzle) {
        writeExpression(swizzle.getBase(), Operator.PRECEDENCE_POSTFIX);
        write('.');
        for (var component : swizzle.getComponents()) {
            switch (component) {
                case Swizzle.X -> write('x');
                case Swizzle.Y -> write('y');
                case Swizzle.Z -> write('z');
                case Swizzle.W -> write('w');
                default -> throw new IllegalStateException();
            }
        }
    }

    private void writeFunctionCall(FunctionCall call) {
        final var function = call.getFunction();
        final var arguments = call.getArguments();
        switch (function.getIntrinsicKind()) {
            case IntrinsicList.kFma -> {
                assert arguments.length == 3;
                if ((mOutputVersion.isCoreProfile() && !mOutputVersion.isAtLeast(GLSLVersion.GLSL_400)) ||
                        (mOutputVersion.isESProfile() && !mOutputVersion.isAtLeast(GLSLVersion.GLSL_320_ES))) {
                    write("((");
                    writeExpression(arguments[0], Operator.PRECEDENCE_SEQUENCE);
                    if (mPrettyPrint) {
                        write(") * (");
                    } else {
                        write(")*(");
                    }
                    writeExpression(arguments[1], Operator.PRECEDENCE_SEQUENCE);
                    if (mPrettyPrint) {
                        write(") + (");
                    } else {
                        write(")+(");
                    }
                    writeExpression(arguments[2], Operator.PRECEDENCE_SEQUENCE);
                    write("))");
                    return;
                }
            }
            case IntrinsicList.kSaturate -> {
                assert arguments.length == 1;
                write("clamp(");
                writeExpression(arguments[0], Operator.PRECEDENCE_SEQUENCE);
                if (mPrettyPrint) {
                    write(", 0.0, 1.0)");
                } else {
                    write(",0.0,1.0)");
                }
                return;
            }
        }
        writeIdentifier(function.getMangledName());
        write('(');
        for (int i = 0; i < arguments.length; i++) {
            if (i != 0) {
                if (mPrettyPrint) {
                    write(", ");
                } else {
                    write(',');
                }
            }
            writeExpression(arguments[i], Operator.PRECEDENCE_SEQUENCE);
        }
        write(')');
    }

    private void writeConstructorCall(ConstructorCall ctor) {
        writeType(ctor.getType());
        write('(');
        final var arguments = ctor.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            if (i != 0) {
                if (mPrettyPrint) {
                    write(", ");
                } else {
                    write(',');
                }
            }
            writeExpression(arguments[i], Operator.PRECEDENCE_SEQUENCE);
        }
        write(')');
    }

    private void writeConstructorCast(ConstructorCall ctor, int parentPrecedence) {
        var argument = ctor.getArgument();
        var argComponentType = argument.getType().getComponentType();
        var resultComponentType = ctor.getType().getComponentType();
        if (argComponentType.getScalarKind() == resultComponentType.getScalarKind() &&
                argComponentType.getWidth() == resultComponentType.getWidth()) {
            writeExpression(argument, parentPrecedence);
            return;
        }
        writeConstructorCall(ctor);
    }

    private void writeStatement(Statement stmt) {
        switch (stmt.getKind()) {
            case BLOCK -> writeBlockStatement((BlockStatement) stmt);
            case RETURN -> writeReturnStatement((ReturnStatement) stmt);
            case IF -> writeIfStatement((IfStatement) stmt);
            case SWITCH -> writeSwitchStatement((SwitchStatement) stmt);
            case FOR_LOOP -> writeForLoop((ForLoop) stmt);
            case VARIABLE_DECL -> writeVariableDecl((VariableDecl) stmt);
            case EXPRESSION -> writeExpressionStatement((ExpressionStatement) stmt);
            case BREAK -> write("break;");
            case CONTINUE -> write("continue;");
            case DISCARD -> write("discard;");
            default -> getContext().error(stmt.mPosition, "unsupported statement: " + stmt.getKind());
        }
    }

    private void writeBlockStatement(BlockStatement b) {
        boolean isScope = b.isScoped() || b.isEmpty();
        if (isScope) {
            writeLine('{');
            mIndentation++;
        }
        for (var stmt : b.getStatements()) {
            if (!stmt.isEmpty()) {
                writeStatement(stmt);
                finishLine();
            }
        }
        if (isScope) {
            mIndentation--;
            write('}');
        }
    }

    private void writeReturnStatement(ReturnStatement s) {
        write("return");
        if (s.getExpression() != null) {
            write(' ');
            writeExpression(s.getExpression(), Operator.PRECEDENCE_EXPRESSION);
        }
        write(';');
    }

    private void writeIfStatement(IfStatement s) {
        write("if (");
        writeExpression(s.getCondition(), Operator.PRECEDENCE_EXPRESSION);
        write(") ");
        writeStatement(s.getWhenTrue());
        if (s.getWhenFalse() != null) {
            write(" else ");
            writeStatement(s.getWhenFalse());
        }
    }

    private void writeSwitchStatement(SwitchStatement s) {
        write("switch (");
        writeExpression(s.getInit(), Operator.PRECEDENCE_EXPRESSION);
        writeLine(") {");
        mIndentation++;
        // If a switch contains only a `default` case and nothing else, this confuses some drivers and
        // can lead to a crash. Adding a real case before the default seems to work around the bug,
        // and doesn't change the meaning of the switch.
        if (s.getCases().size() == 1 && ((SwitchCase)s.getCases().get(0)).isDefault()) {
            writeLine("case 0:");
        }

        // The GLSL spec insists that the last case in a switch statement must have an associated
        // statement. In practice, the Apple GLSL compiler crashes if that statement is a no-op, such as
        // a semicolon or an empty brace pair.
        // It also crashes if we put two `break` statements in a row. To work around this while honoring
        // the rules of the standard, we inject an extra break if and only if the last switch-case block
        // is empty.
        boolean foundEmptyCase = false;

        for (var stmt : s.getCases()) {
            var c = (SwitchCase) stmt;
            if (c.isDefault()) {
                writeLine("default:");
            } else {
                write("case ");
                write(Long.toString(c.getValue()));
                writeLine(':');
            }
            if (c.getStatement().isEmpty()) {
                foundEmptyCase = true;
            } else {
                foundEmptyCase = false;
                mIndentation++;
                writeStatement(c.getStatement());
                finishLine();
                mIndentation--;
            }
        }
        if (foundEmptyCase) {
            mIndentation++;
            writeLine("break;");
            mIndentation--;
        }
        mIndentation--;
        finishLine();
        write('}');
    }

    private void writeForLoop(ForLoop f) {
        if (f.getInit() == null && f.getCondition() != null && f.getStep() == null) {
            write("while (");
            writeExpression(f.getCondition(), Operator.PRECEDENCE_EXPRESSION);
            write(") ");
            writeStatement(f.getStatement());
            return;
        }

        write("for (");
        if (f.getInit() != null && !f.getInit().isEmpty()) {
            writeStatement(f.getInit());
            if (mPrettyPrint && f.getCondition() != null) {
                write(' ');
            }
        } else {
            if (mPrettyPrint && f.getCondition() != null) {
                write("; ");
            } else {
                write(';');
            }
        }
        if (f.getCondition() != null) {
            writeExpression(f.getCondition(), Operator.PRECEDENCE_EXPRESSION);
        }
        if (mPrettyPrint && f.getStep() != null) {
            write("; ");
        } else {
            write(';');
        }
        if (f.getStep() != null) {
            writeExpression(f.getStep(), Operator.PRECEDENCE_EXPRESSION);
        }
        write(") ");
        writeStatement(f.getStatement());
    }

    private void writeVariableDecl(VariableDecl decl) {
        var variable = decl.getVariable();
        writeModifiers(variable.getModifiers());
        Type baseType = variable.getType();
        int arraySize = 0;
        if (baseType.isArray()) {
            arraySize = baseType.getArraySize();
            baseType = baseType.getElementType();
        }
        writeTypePrecision(baseType);
        writeType(baseType);
        write(' ');
        writeIdentifier(variable.getName());
        if (arraySize > 0) {
            write('[');
            write(Integer.toString(arraySize));
            write(']');
        }
        if (decl.getInit() != null) {
            if (mPrettyPrint) {
                write(" = ");
            } else {
                write('=');
            }
            writeExpression(decl.getInit(), Operator.PRECEDENCE_EXPRESSION);
        }
        write(';');
    }

    private void writeExpressionStatement(ExpressionStatement s) {
        if (getContext().getOptions().mOptimizationLevel >= 1 &&
                !Analysis.hasSideEffects(s.getExpression())) {
            return;
        }
        writeExpression(s.getExpression(), Operator.PRECEDENCE_STATEMENT);
        write(';');
    }
}
