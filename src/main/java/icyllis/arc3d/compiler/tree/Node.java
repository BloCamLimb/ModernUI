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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Position;
import org.jspecify.annotations.NonNull;

/**
 * Represents a node in the AST. The AST is a fully-resolved version of the program
 * (all types determined, everything validated), ready for code generation.
 */
public abstract class Node {

    public enum ElementKind {
        EXTENSION(TopLevelElement.class),
        FUNCTION_DEFINITION(FunctionDefinition.class),
        FUNCTION_PROTOTYPE(FunctionPrototype.class),
        GLOBAL_VARIABLE(GlobalVariableDecl.class),
        INTERFACE_BLOCK(InterfaceBlock.class),
        MODIFIERS(TopLevelElement.class),
        STRUCT_DEFINITION(StructDefinition.class);

        private final Class<? extends TopLevelElement> mType;

        ElementKind(Class<? extends TopLevelElement> type) {
            mType = type;
        }

        public Class<? extends TopLevelElement> getType() {
            return mType;
        }
    }

    public enum SymbolKind {
        ANONYMOUS_FIELD(AnonymousField.class),
        FUNCTION_DECL(FunctionDecl.class),
        TYPE(Type.class),
        VARIABLE(Variable.class);

        private final Class<? extends Symbol> mType;

        SymbolKind(Class<? extends Symbol> type) {
            mType = type;
        }

        public Class<? extends Symbol> getType() {
            return mType;
        }
    }

    public enum StatementKind {
        BLOCK(BlockStatement.class),
        BREAK(BreakStatement.class),
        CONTINUE(ContinueStatement.class),
        DISCARD(DiscardStatement.class),
        DO_LOOP(Statement.class),
        EMPTY(EmptyStatement.class),
        EXPRESSION(ExpressionStatement.class),
        FOR_LOOP(ForLoop.class),
        IF(IfStatement.class),
        RETURN(ReturnStatement.class),
        SWITCH(SwitchStatement.class),
        SWITCH_CASE(SwitchCase.class),
        VARIABLE_DECL(VariableDecl.class);

        private final Class<? extends Statement> mType;

        StatementKind(Class<? extends Statement> type) {
            mType = type;
        }

        public Class<? extends Statement> getType() {
            return mType;
        }
    }

    public enum ExpressionKind {
        BINARY(BinaryExpression.class),
        CONDITIONAL(ConditionalExpression.class),
        CONSTRUCTOR_ARRAY(ConstructorArray.class),
        CONSTRUCTOR_ARRAY_CAST(ConstructorArrayCast.class),
        CONSTRUCTOR_COMPOUND(ConstructorCompound.class),
        CONSTRUCTOR_COMPOUND_CAST(ConstructorCompoundCast.class),
        CONSTRUCTOR_DIAGONAL_MATRIX(ConstructorDiagonalMatrix.class),
        CONSTRUCTOR_MATRIX_RESIZE(ConstructorMatrixResize.class),
        CONSTRUCTOR_SCALAR_CAST(ConstructorScalarCast.class),
        CONSTRUCTOR_STRUCT(ConstructorStruct.class),
        CONSTRUCTOR_VECTOR_SPLAT(ConstructorVectorSplat.class),
        FIELD_ACCESS(FieldAccess.class),
        FUNCTION_CALL(FunctionCall.class),
        FUNCTION_REFERENCE(FunctionReference.class),
        INDEX(IndexExpression.class),
        LITERAL(Literal.class),
        POISON(Poison.class),
        POSTFIX(PostfixExpression.class),
        PREFIX(PrefixExpression.class),
        SWIZZLE(Swizzle.class),
        TYPE_REFERENCE(TypeReference.class),
        VARIABLE_REFERENCE(VariableReference.class);

        private final Class<? extends Expression> mType;

        ExpressionKind(Class<? extends Expression> type) {
            mType = type;
        }

        public Class<? extends Expression> getType() {
            return mType;
        }
    }

    /**
     * Position of this element within the module being compiled, for error reporting purposes.
     *
     * @see Position#range(int, int)
     */
    public int mPosition;

    protected Node(int position) {
        mPosition = position;
    }

    public final int getStartOffset() {
        assert (mPosition != -1);
        return Position.getStartOffset(mPosition);
    }

    public final int getEndOffset() {
        assert (mPosition != -1);
        return Position.getEndOffset(mPosition);
    }

    /**
     * Visit this AST with a given visitor.
     *
     * @return true to stop recursion and propagate true up the stack, false to continue
     */
    public abstract boolean accept(@NonNull TreeVisitor visitor);

    /**
     * @return a string representation of this AST node
     */
    @NonNull
    @Override
    public abstract String toString();
}
