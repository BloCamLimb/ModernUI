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

package icyllis.arcui.arsl.ir;

/**
 * Abstract supertype of all expressions.
 */
public abstract class Expression extends IRNode {

    public static final int Kind_First = Statement.Kind_Last + 1;
    public static final int
            Kind_Binary = Kind_First,
            Kind_ChildCall = Kind_First + 1,
            Kind_ConstructorArray = Kind_First + 2,
            Kind_ConstructorArrayCast = Kind_First + 3,
            Kind_ConstructorCompound = Kind_First + 4,
            Kind_ConstructorCompoundCast = Kind_First + 5,
            Kind_ConstructorDiagonalMatrix = Kind_First + 6,
            Kind_ConstructorMatrixResize = Kind_First + 7,
            Kind_ConstructorScalarCast = Kind_First + 8,
            Kind_ConstructorSplat = Kind_First + 9,
            Kind_ConstructorStruct = Kind_First + 10,
            Kind_ExternalFunctionCall = Kind_First + 11,
            Kind_ExternalFunctionReference = Kind_First + 12,
            Kind_FieldAccess = Kind_First + 13,
            Kind_FunctionReference = Kind_First + 14,
            Kind_FunctionCall = Kind_First + 15,
            Kind_Index = Kind_First + 16,
            Kind_Literal = Kind_First + 17,
            Kind_MethodReference = Kind_First + 18,
            Kind_Poison = Kind_First + 19,
            Kind_Postfix = Kind_First + 20,
            Kind_Prefix = Kind_First + 21,
            Kind_Setting = Kind_First + 22,
            Kind_Swizzle = Kind_First + 23,
            Kind_Ternary = Kind_First + 24,
            Kind_TypeReference = Kind_First + 25,
            Kind_VariableReference = Kind_First + 26;
    public static final int Kind_Last = Kind_VariableReference;

    private final Type mType;

    protected Expression(int start, int end, int kind, Type type) {
        super(start, end, kind);
        assert (kind >= Kind_First && kind <= Kind_Last);
        mType = type;
    }

    public final int kind() {
        return mKind;
    }

    public Type type() {
        return mType;
    }

    public final boolean isAnyConstructor() {
        return kind() >= Kind_ConstructorArray && kind() <= Kind_ConstructorStruct;
    }

    public final boolean isIntLiteral() {
        return kind() == Kind_Literal && type().isInteger();
    }

    public final boolean isFloatLiteral() {
        return kind() == Kind_Literal && type().isFloat();
    }

    public final boolean isBoolLiteral() {
        return kind() == Kind_Literal && type().isBoolean();
    }

    /**
     * @see Type.CoercionCost
     */
    public long coercionCost(Type target) {
        return type().coercionCost(target);
    }
}
