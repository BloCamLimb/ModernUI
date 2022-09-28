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

package icyllis.akashigi.sksl;

import icyllis.akashigi.sksl.ir.Type;

/**
 * Operators.
 */
public enum Operator {
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    SHL,
    SHR,
    LOGICALNOT,
    LOGICALAND,
    LOGICALOR,
    LOGICALXOR,
    BITWISENOT,
    BITWISEAND,
    BITWISEOR,
    BITWISEXOR,
    EQ,
    EQEQ,
    NEQ,
    LT,
    GT,
    LTEQ,
    GTEQ,
    PLUSEQ,
    MINUSEQ,
    STAREQ,
    SLASHEQ,
    PERCENTEQ,
    SHLEQ,
    SHREQ,
    BITWISEANDEQ,
    BITWISEOREQ,
    BITWISEXOREQ,
    PLUSPLUS,
    MINUSMINUS,
    COMMA;

    /**
     * {@code Precedence}.
     *
     * <h5>Enum values:</h5>
     */
    public static final int
            Precedence_Parentheses = 1,
            Precedence_Postfix = 2,
            Precedence_Prefix = 3,
            Precedence_Multiplicative = 4,
            Precedence_Additive = 5,
            Precedence_Shift = 6,
            Precedence_Relational = 7,
            Precedence_Equality = 8,
            Precedence_BitwiseAnd = 9,
            Precedence_BitwiseXor = 10,
            Precedence_BitwiseOr = 11,
            Precedence_LogicalAnd = 12,
            Precedence_LogicalXor = 13,
            Precedence_LogicalOr = 14,
            Precedence_Ternary = 15,
            Precedence_Assignment = 16,
            Precedence_Sequence = 17,
            Precedence_TopLevel = Precedence_Sequence;

    Operator() {
    }

    public boolean isEquality() {
        return this == EQEQ || this == NEQ;
    }

    public int getBinaryPrecedence() {
        return switch (this) {
            case STAR,
                    SLASH,
                    PERCENT -> Precedence_Multiplicative;
            case PLUS,
                    MINUS -> Precedence_Additive;
            case SHL,
                    SHR -> Precedence_Shift;
            case LT,
                    GT,
                    LTEQ,
                    GTEQ -> Precedence_Relational;
            case EQEQ,
                    NEQ -> Precedence_Equality;
            case BITWISEAND -> Precedence_BitwiseAnd;
            case BITWISEXOR -> Precedence_BitwiseXor;
            case BITWISEOR -> Precedence_BitwiseOr;
            case LOGICALAND -> Precedence_LogicalAnd;
            case LOGICALXOR -> Precedence_LogicalXor;
            case LOGICALOR -> Precedence_LogicalOr;
            case EQ,
                    PLUSEQ,
                    MINUSEQ,
                    STAREQ,
                    SLASHEQ,
                    PERCENTEQ,
                    SHLEQ, SHREQ,
                    BITWISEANDEQ,
                    BITWISEXOREQ,
                    BITWISEOREQ -> Precedence_Assignment;
            case COMMA -> Precedence_Sequence;
            default -> throw new IllegalStateException();
        };
    }

    // Returns the operator name surrounded by the expected whitespace for a tidy binary expression.
    public String operatorName() {
        return switch (this) {
            case PLUS -> " + ";
            case MINUS -> " - ";
            case STAR -> " * ";
            case SLASH -> " / ";
            case PERCENT -> " % ";
            case SHL -> " << ";
            case SHR -> " >> ";
            case LOGICALNOT -> "!";
            case LOGICALAND -> " && ";
            case LOGICALOR -> " || ";
            case LOGICALXOR -> " ^^ ";
            case BITWISENOT -> "~";
            case BITWISEAND -> " & ";
            case BITWISEOR -> " | ";
            case BITWISEXOR -> " ^ ";
            case EQ -> " = ";
            case EQEQ -> " == ";
            case NEQ -> " != ";
            case LT -> " < ";
            case GT -> " > ";
            case LTEQ -> " <= ";
            case GTEQ -> " >= ";
            case PLUSEQ -> " += ";
            case MINUSEQ -> " -= ";
            case STAREQ -> " *= ";
            case SLASHEQ -> " /= ";
            case PERCENTEQ -> " %= ";
            case SHLEQ -> " <<= ";
            case SHREQ -> " >>= ";
            case BITWISEANDEQ -> " &= ";
            case BITWISEOREQ -> " |= ";
            case BITWISEXOREQ -> " ^= ";
            case PLUSPLUS -> "++";
            case MINUSMINUS -> "--";
            case COMMA -> ", ";
        };
    }

    // Returns the operator name without any surrounding whitespace.
    public String tightOperatorName() {
        return switch (this) {
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case PERCENT -> "%";
            case SHL -> "<<";
            case SHR -> ">>";
            case LOGICALNOT -> "!";
            case LOGICALAND -> "&&";
            case LOGICALOR -> "||";
            case LOGICALXOR -> "^^";
            case BITWISENOT -> "~";
            case BITWISEAND -> "&";
            case BITWISEOR -> "|";
            case BITWISEXOR -> "^";
            case EQ -> "=";
            case EQEQ -> "==";
            case NEQ -> "!=";
            case LT -> "<";
            case GT -> ">";
            case LTEQ -> "<=";
            case GTEQ -> ">=";
            case PLUSEQ -> "+=";
            case MINUSEQ -> "-=";
            case STAREQ -> "*=";
            case SLASHEQ -> "/=";
            case PERCENTEQ -> "%=";
            case SHLEQ -> "<<=";
            case SHREQ -> ">>=";
            case BITWISEANDEQ -> "&=";
            case BITWISEOREQ -> "|=";
            case BITWISEXOREQ -> "^=";
            case PLUSPLUS -> "++";
            case MINUSMINUS -> "--";
            case COMMA -> ",";
        };
    }

    // Returns true if op is '=' or any compound assignment operator ('+=', '-=', etc.)
    public boolean isAssignment() {
        return switch (this) {
            case EQ,
                    PLUSEQ,
                    MINUSEQ,
                    STAREQ,
                    SLASHEQ,
                    PERCENTEQ,
                    SHLEQ,
                    SHREQ,
                    BITWISEOREQ,
                    BITWISEXOREQ,
                    BITWISEANDEQ -> true;
            default -> false;
        };
    }

    // Given a compound assignment operator, returns the non-assignment version of the operator
    // (e.g. '+=' becomes '+')
    public Operator removeAssignment() {
        return switch (this) {
            case PLUSEQ -> PLUS;
            case MINUSEQ -> MINUS;
            case STAREQ -> STAR;
            case SLASHEQ -> SLASH;
            case PERCENTEQ -> PERCENT;
            case SHLEQ -> SHL;
            case SHREQ -> SHR;
            case BITWISEOREQ -> BITWISEOR;
            case BITWISEXOREQ -> BITWISEXOR;
            case BITWISEANDEQ -> BITWISEAND;
            default -> this;
        };
    }

    /**
     * Defines the set of relational (comparison) operators:
     * <  <=  >  >=
     */
    public boolean isRelational() {
        return switch (this) {
            case LT,
                    GT,
                    LTEQ,
                    GTEQ -> true;
            default -> false;
        };
    }

    /**
     * Defines the set of operators which are only valid on integral types:
     * <<  <<=  >>  >>=  &  &=  |  |=  ^  ^=  %  %=
     */
    public boolean isOnlyValidForIntegralTypes() {
        return switch (this) {
            case SHL,
                    SHR,
                    BITWISEAND,
                    BITWISEOR,
                    BITWISEXOR,
                    PERCENT,
                    SHLEQ,
                    SHREQ,
                    BITWISEANDEQ,
                    BITWISEOREQ,
                    BITWISEXOREQ,
                    PERCENTEQ -> true;
            default -> false;
        };
    }

    /**
     * Defines the set of operators which perform vector/matrix math.
     * +  +=  -  -=  *  *=  /  /=  %  %=  <<  <<=  >>  >>=  &  &=  |  |=  ^  ^=
     */
    public boolean isValidForMatrixOrVector() {
        return switch (this) {
            case PLUS,
                    MINUS,
                    STAR,
                    SLASH,
                    PERCENT,
                    SHL,
                    SHR,
                    BITWISEAND,
                    BITWISEOR,
                    BITWISEXOR,
                    PLUSEQ,
                    MINUSEQ,
                    STAREQ,
                    SLASHEQ,
                    PERCENTEQ,
                    SHLEQ,
                    SHREQ,
                    BITWISEANDEQ,
                    BITWISEOREQ,
                    BITWISEXOREQ -> true;
            default -> false;
        };
    }

    private boolean isMatrixMultiply(Type left, Type right) {
        if (this != STAR && this != STAREQ) {
            return false;
        }
        if (left.isMatrix()) {
            return right.isMatrix() || right.isVector();
        }
        return left.isVector() && right.isMatrix();
    }

    /**
     * Determines the operand and result types of a binary expression. Returns true if the
     * expression is legal, false otherwise. If false, the values of the out parameters are
     * undefined.
     * <p>
     * outTypes: left type, right type, result type
     */
    public boolean determineBinaryType(Context context,
                                       Type left,
                                       Type right,
                                       Type[] outTypes) {
        assert outTypes.length >= 3;
        final boolean allowNarrowing = context.mSettings.mAllowNarrowingConversions;
        switch (this) {
            case EQ -> {  // left = right
                if (left.isVoid()) {
                    return false;
                }
                outTypes[0] = left;
                outTypes[1] = left;
                outTypes[2] = left;
                return right.canCoerceTo(left, allowNarrowing);
            }
            case EQEQ, // left == right
                    NEQ -> {  // left != right
                if (left.isVoid() || left.isOpaque()) {
                    return false;
                }
                long rightToLeft = right.coercionCost(left),
                        leftToRight = left.coercionCost(right);
                if (Type.CoercionCost.compare(rightToLeft, leftToRight) < 0) {
                    if (Type.CoercionCost.isPossible(rightToLeft, allowNarrowing)) {
                        outTypes[0] = left;
                        outTypes[1] = left;
                        outTypes[2] = context.mTypes.mBool;
                        return true;
                    }
                } else {
                    if (Type.CoercionCost.isPossible(leftToRight, allowNarrowing)) {
                        outTypes[0] = right;
                        outTypes[1] = right;
                        outTypes[2] = context.mTypes.mBool;
                        return true;
                    }
                }
                return false;
            }
            case LOGICALOR, // left || right
                    LOGICALAND,  // left && right
                    LOGICALXOR -> {  // left ^^ right
                outTypes[0] = context.mTypes.mBool;
                outTypes[1] = context.mTypes.mBool;
                outTypes[2] = context.mTypes.mBool;
                return left.canCoerceTo(context.mTypes.mBool, allowNarrowing) &&
                        right.canCoerceTo(context.mTypes.mBool, allowNarrowing);
            }
            case COMMA -> {  // left, right
                if (left.isOpaque() || right.isOpaque()) {
                    return false;
                }
                outTypes[0] = left;
                outTypes[1] = right;
                outTypes[2] = right;
                return true;
            }
        }

        // Boolean types only support the operators listed above (, = == != || && ^^).
        // If we've gotten this far with a boolean, we have an unsupported operator.
        final Type leftComponentType = left.componentType();
        final Type rightComponentType = right.componentType();
        if (leftComponentType.isBoolean() || rightComponentType.isBoolean()) {
            return false;
        }

        boolean isAssignment = isAssignment();
        if (isMatrixMultiply(left, right)) {  // left * right
            // Determine final component type.
            if (!determineBinaryType(context, left.componentType(), right.componentType(),
                    outTypes)) {
                return false;
            }
            // Convert component type to compound.
            outTypes[0] = outTypes[0].toCompound(context, left.columns(), left.rows());
            outTypes[1] = outTypes[1].toCompound(context, right.columns(), right.rows());
            int leftColumns = left.columns(), leftRows = left.rows();
            int rightColumns = right.columns(), rightRows = right.rows();
            if (right.isVector()) {
                // `matrix * vector` treats the vector as a column vector; we need to transpose it.
                int t = leftColumns;
                leftColumns = rightColumns;
                rightColumns = t;
                assert (rightColumns == 1);
            }
            if (rightColumns > 1) {
                outTypes[2] = outTypes[2].toCompound(context, rightColumns, leftRows);
            } else {
                // The result was a column vector. Transpose it back to a row.
                outTypes[2] = outTypes[2].toCompound(context, leftRows, rightColumns);
            }
            if (isAssignment && (outTypes[2].columns() != leftColumns ||
                    outTypes[2].rows() != leftRows)) {
                return false;
            }
            return leftColumns == rightRows;
        }

        boolean leftIsVectorOrMatrix = left.isVector() || left.isMatrix();
        boolean validMatrixOrVectorOp = isValidForMatrixOrVector();

        if (leftIsVectorOrMatrix && validMatrixOrVectorOp && right.isScalar()) {
            // Determine final component type.
            if (!determineBinaryType(context, left.componentType(), right,
                    outTypes)) {
                return false;
            }
            // Convert component type to compound.
            outTypes[0] = outTypes[0].toCompound(context, left.columns(), left.rows());
            if (!isRelational()) {
                outTypes[2] = outTypes[2].toCompound(context, left.columns(), left.rows());
            }
            return true;
        }

        boolean rightIsVectorOrMatrix = right.isVector() || right.isMatrix();

        if (!isAssignment && rightIsVectorOrMatrix && validMatrixOrVectorOp && left.isScalar()) {
            // Determine final component type.
            if (!determineBinaryType(context, left, right.componentType(),
                    outTypes)) {
                return false;
            }
            // Convert component type to compound.
            outTypes[1] = outTypes[1].toCompound(context, right.columns(), right.rows());
            if (!isRelational()) {
                outTypes[2] = outTypes[2].toCompound(context, right.columns(), right.rows());
            }
            return true;
        }

        long rightToLeftCost = right.coercionCost(left);
        long leftToRightCost = isAssignment ? Type.CoercionCost.impossible()
                : left.coercionCost(right);

        if ((left.isScalar() && right.isScalar()) || (leftIsVectorOrMatrix && validMatrixOrVectorOp)) {
            if (isOnlyValidForIntegralTypes()) {
                if (!leftComponentType.isInteger() || !rightComponentType.isInteger()) {
                    return false;
                }
            }
            if (Type.CoercionCost.isPossible(rightToLeftCost, allowNarrowing) &&
                    Type.CoercionCost.compare(rightToLeftCost, leftToRightCost) < 0) {
                // Right-to-Left conversion is possible and cheaper
                outTypes[0] = left;
                outTypes[1] = left;
                outTypes[2] = left;
            } else if (Type.CoercionCost.isPossible(leftToRightCost, allowNarrowing)) {
                // Left-to-Right conversion is possible (and at least as cheap as Right-to-Left)
                outTypes[0] = right;
                outTypes[1] = right;
                outTypes[2] = right;
            } else {
                return false;
            }
            if (isRelational()) {
                outTypes[2] = context.mTypes.mBool.type();
            }
            return true;
        }
        return false;
    }
}
