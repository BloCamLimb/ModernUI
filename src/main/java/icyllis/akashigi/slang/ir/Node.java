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

package icyllis.akashigi.slang.ir;

import icyllis.akashigi.slang.analysis.NodeVisitor;

import javax.annotation.Nonnull;

/**
 * Represents a node in the AST. The AST is a fully-resolved version of the program
 * (all types determined, everything validated), ready for code generation.
 */
public abstract class Node {

    public enum ElementKind {
        EXTENSION(Element.class),
        FUNCTION_DEFINITION(FunctionDefinition.class),
        FUNCTION_PROTOTYPE(Element.class),
        GLOBAL_VAR(Element.class),
        INTERFACE_BLOCK(Element.class),
        MODIFIERS(Element.class),
        STRUCT_DEFINITION(Element.class);

        private final Class<? extends Element> mType;

        ElementKind(Class<? extends Element> type) {
            mType = type;
        }

        public Class<? extends Element> getType() {
            return mType;
        }
    }

    public enum SymbolKind {
        ANONYMOUS_FIELD(AnonymousField.class),
        FUNCTION(Function.class),
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
        BLOCK(Statement.class),
        BREAK(Statement.class),
        CONTINUE(Statement.class),
        DISCARD(Statement.class),
        DO(Statement.class),
        EXPRESSION(Statement.class),
        FOR(Statement.class),
        IF(Statement.class),
        NOP(Statement.class),
        RETURN(Statement.class),
        SWITCH(Statement.class),
        SWITCH_CASE(Statement.class),
        VAR_DECLARATION(Statement.class);

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
        CONSTRUCTOR_MATRIX_MATRIX(ConstructorMatrixMatrix.class),
        CONSTRUCTOR_MATRIX_SCALAR(ConstructorMatrixScalar.class),
        CONSTRUCTOR_SCALAR_CAST(ConstructorScalarCast.class),
        CONSTRUCTOR_STRUCT(ConstructorStruct.class),
        CONSTRUCTOR_VECTOR_SCALAR(ConstructorVectorScalar.class),
        FIELD_ACCESS(FieldExpression.class),
        FUNCTION_CALL(FunctionCall.class),
        FUNCTION_REFERENCE(FunctionReference.class),
        INDEX(Expression.class),
        LITERAL(Literal.class),
        POISON(Expression.class),
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
     * Position of this element within the program being compiled, for error reporting purposes.
     *
     * @see #makeRange(int, int)
     */
    public int mPosition;

    protected Node(int position) {
        mPosition = position;
    }

    public final int getStartOffset() {
        assert (mPosition != -1);
        return (mPosition & 0xFFFFFF);
    }

    public final int getEndOffset() {
        assert (mPosition != -1);
        return (mPosition & 0xFFFFFF) + (mPosition >>> 24);
    }

    public static int getStartOffset(int position) {
        return (position & 0xFFFFFF);
    }

    public static int getEndOffset(int position) {
        return (position & 0xFFFFFF) + (position >>> 24);
    }

    /**
     * Pack a range into a position, the position is valid only if
     * {@code 0 <= start && start <= end && end <= 0x7FFFFF}.
     * <ul>
     * <li>0-24 bits: start offset, signed, -1 means invalid</li>
     * <li>24-32 bits: length, unsigned, saturate at 0xFF</li>
     * </ul>
     *
     * @return the position
     */
    public static int makeRange(int start, int end) {
        if ((start | end - start | 0x7FFFFF - end) < 0) {
            return -1;
        }
        return start | Math.min(end - start, 0xFF) << 24;
    }

    /**
     * Visit this AST with a given visitor.
     *
     * @return true to stop recursion and propagate true up the stack, false to continue
     */
    public abstract boolean accept(@Nonnull NodeVisitor visitor);

    /**
     * @return a string representation of this AST node
     */
    @Nonnull
    @Override
    public abstract String toString();
}
