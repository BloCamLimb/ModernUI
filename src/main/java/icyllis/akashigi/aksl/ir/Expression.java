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

package icyllis.akashigi.aksl.ir;

import icyllis.akashigi.aksl.Operator;

import javax.annotation.Nonnull;

/**
 * Abstract superclass of all expressions.
 */
public abstract class Expression extends Node {

    public static final int KIND_FIRST = Statement.KIND_LAST + 1;
    public static final int
            KIND_BINARY = KIND_FIRST,
            KIND_CONDITIONAL = KIND_FIRST + 1,
            KIND_CONSTANT = KIND_FIRST + 2,
            KIND_CONSTRUCTOR_ARRAY = KIND_FIRST + 3,
            KIND_CONSTRUCTOR_ARRAY_CAST = KIND_FIRST + 4,
            KIND_CONSTRUCTOR_COMPOUND = KIND_FIRST + 5,
            KIND_CONSTRUCTOR_COMPOUND_CAST = KIND_FIRST + 6,
            KIND_CONSTRUCTOR_DIAGONAL_MATRIX = KIND_FIRST + 7,
            KIND_CONSTRUCTOR_MATRIX_RESIZE = KIND_FIRST + 8,
            KIND_CONSTRUCTOR_SCALAR_CAST = KIND_FIRST + 9,
            KIND_CONSTRUCTOR_SPLAT = KIND_FIRST + 10,
            KIND_CONSTRUCTOR_STRUCT = KIND_FIRST + 11,
            KIND_FIELD = KIND_FIRST + 12,
            KIND_FUNCTION_CALL = KIND_FIRST + 13,
            KIND_FUNCTION_REFERENCE = KIND_FIRST + 14,
            KIND_INDEX = KIND_FIRST + 15,
            KIND_POISON = KIND_FIRST + 16,
            KIND_POSTFIX = KIND_FIRST + 17,
            KIND_PREFIX = KIND_FIRST + 18,
            KIND_SWIZZLE = KIND_FIRST + 19,
            KIND_TYPE_REFERENCE = KIND_FIRST + 20,
            KIND_VARIABLE_REFERENCE = KIND_FIRST + 21;
    public static final int KIND_LAST = KIND_VARIABLE_REFERENCE;

    private final Type mType;

    protected Expression(int position, int kind, Type type) {
        super(position, kind);
        assert (kind >= KIND_FIRST && kind <= KIND_LAST);
        mType = type;
    }

    public final int kind() {
        return mKind;
    }

    @Nonnull
    public Type type() {
        assert (mType != null);
        return mType;
    }

    public final boolean isAnyConstructor() {
        return kind() >= KIND_CONSTRUCTOR_ARRAY && kind() <= KIND_CONSTRUCTOR_STRUCT;
    }

    public final boolean isIntLiteral() {
        return kind() == KIND_CONSTANT && type().isInteger();
    }

    public final boolean isFloatLiteral() {
        return kind() == KIND_CONSTANT && type().isFloat();
    }

    public final boolean isBooleanLiteral() {
        return kind() == KIND_CONSTANT && type().isBoolean();
    }

    /**
     * @see Type.CoercionCost
     */
    public long coercionCost(Type target) {
        return type().coercionCost(target);
    }

    @Nonnull
    @Override
    public final String toString() {
        return toString(Operator.PRECEDENCE_TOP_LEVEL);
    }

    @Nonnull
    public abstract String toString(int parentPrecedence);
}
