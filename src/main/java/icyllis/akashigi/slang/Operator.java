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

import icyllis.akashigi.slang.tree.Type;

/**
 * Operators.
 */
// @formatter:off
public enum Operator {
    ADD,            // +, unary positive or binary add
    SUB,            // -, unary negative or binary sub
    MUL,            // *
    DIV,            // /
    MOD,            // %
    SHL,            // <<
    SHR,            // >>
    LOGICAL_NOT,    // !
    LOGICAL_AND,    // &&
    LOGICAL_OR,     // ||
    LOGICAL_XOR,    // ^^
    BITWISE_NOT,    // ~
    BITWISE_AND,    // &
    BITWISE_OR,     // |
    BITWISE_XOR,    // ^
    ASSIGN,         // =
    EQ,             // ==
    NE,             // !=
    LT,             // <
    GT,             // >
    LE,             // <=
    GE,             // >=
    ADD_ASSIGN,     // +=
    SUB_ASSIGN,     // -=
    MUL_ASSIGN,     // *=
    DIV_ASSIGN,     // /=
    MOD_ASSIGN,     // %=
    SHL_ASSIGN,     // <<=
    SHR_ASSIGN,     // >>=
    AND_ASSIGN,     // &=
    OR_ASSIGN,      // |=
    XOR_ASSIGN,     // ^=
    INC,            // ++, prefix increment or postfix increment
    DEC,            // --, prefix decrement or postfix decrement
    COMMA;          // ,

    /**
     * {@code OperatorPrecedence}.
     *
     * <h5>Enum values:</h5>
     */
    public static final int
            PRECEDENCE_POSTFIX        = 2,  // ++ --
            PRECEDENCE_PREFIX         = 3,  // ++ -- + - ~ !
            PRECEDENCE_MULTIPLICATIVE = 4,  // * / %
            PRECEDENCE_ADDITIVE       = 5,  // + -
            PRECEDENCE_SHIFT          = 6,  // << >>
            PRECEDENCE_RELATIONAL     = 7,  // < > <= >=
            PRECEDENCE_EQUALITY       = 8,  // == !=
            PRECEDENCE_BITWISE_AND    = 9,  // &
            PRECEDENCE_BITWISE_XOR    = 10, // ^
            PRECEDENCE_BITWISE_OR     = 11, // |
            PRECEDENCE_LOGICAL_AND    = 12, // &&
            PRECEDENCE_LOGICAL_XOR    = 13, // ^^
            PRECEDENCE_LOGICAL_OR     = 14, // ||
            PRECEDENCE_CONDITIONAL    = 15, // ?:
            PRECEDENCE_ASSIGNMENT     = 16, // = += -= *= /= %= <<= >>= &= ^= |=
            PRECEDENCE_SEQUENCE       = 17; // ,

    Operator() {
    }

    public int getBinaryPrecedence() {
        return switch (this) {
            case MUL,
                 DIV,
                 MOD -> PRECEDENCE_MULTIPLICATIVE;
            case ADD,
                 SUB -> PRECEDENCE_ADDITIVE;
            case SHL,
                 SHR -> PRECEDENCE_SHIFT;
            case LT,
                 GT,
                 LE,
                 GE -> PRECEDENCE_RELATIONAL;
            case EQ,
                 NE -> PRECEDENCE_EQUALITY;
            case BITWISE_AND -> PRECEDENCE_BITWISE_AND;
            case BITWISE_XOR -> PRECEDENCE_BITWISE_XOR;
            case BITWISE_OR  -> PRECEDENCE_BITWISE_OR;
            case LOGICAL_AND -> PRECEDENCE_LOGICAL_AND;
            case LOGICAL_XOR -> PRECEDENCE_LOGICAL_XOR;
            case LOGICAL_OR  -> PRECEDENCE_LOGICAL_OR;
            case ASSIGN,
                 ADD_ASSIGN,
                 SUB_ASSIGN,
                 MUL_ASSIGN,
                 DIV_ASSIGN,
                 MOD_ASSIGN,
                 SHL_ASSIGN,
                 SHR_ASSIGN,
                 AND_ASSIGN,
                 OR_ASSIGN,
                 XOR_ASSIGN -> PRECEDENCE_ASSIGNMENT;
            case COMMA -> PRECEDENCE_SEQUENCE;
            default -> throw new AssertionError(this);
        };
    }

    /**
     * Returns the operator name surrounded by the expected whitespace for a tidy binary expression.
     */
    public String getPrettyName() {
        return switch (this) {
            case ADD         -> " + ";
            case SUB         -> " - ";
            case MUL         -> " * ";
            case DIV         -> " / ";
            case MOD         -> " % ";
            case SHL         -> " << ";
            case SHR         -> " >> ";
            case LOGICAL_NOT -> "!";
            case LOGICAL_AND -> " && ";
            case LOGICAL_OR  -> " || ";
            case LOGICAL_XOR -> " ^^ ";
            case BITWISE_NOT -> "~";
            case BITWISE_AND -> " & ";
            case BITWISE_OR  -> " | ";
            case BITWISE_XOR -> " ^ ";
            case ASSIGN      -> " = ";
            case EQ          -> " == ";
            case NE          -> " != ";
            case LT          -> " < ";
            case GT          -> " > ";
            case LE          -> " <= ";
            case GE          -> " >= ";
            case ADD_ASSIGN  -> " += ";
            case SUB_ASSIGN  -> " -= ";
            case MUL_ASSIGN  -> " *= ";
            case DIV_ASSIGN  -> " /= ";
            case MOD_ASSIGN  -> " %= ";
            case SHL_ASSIGN  -> " <<= ";
            case SHR_ASSIGN  -> " >>= ";
            case AND_ASSIGN  -> " &= ";
            case OR_ASSIGN   -> " |= ";
            case XOR_ASSIGN  -> " ^= ";
            case INC         -> "++";
            case DEC         -> "--";
            case COMMA       -> ", ";
        };
    }

    /**
     * Returns the operator name without any surrounding whitespace.
     */
    @Override
    public String toString() {
        return switch (this) {
            case ADD         -> "+";
            case SUB         -> "-";
            case MUL         -> "*";
            case DIV         -> "/";
            case MOD         -> "%";
            case SHL         -> "<<";
            case SHR         -> ">>";
            case LOGICAL_NOT -> "!";
            case LOGICAL_AND -> "&&";
            case LOGICAL_OR  -> "||";
            case LOGICAL_XOR -> "^^";
            case BITWISE_NOT -> "~";
            case BITWISE_AND -> "&";
            case BITWISE_OR  -> "|";
            case BITWISE_XOR -> "^";
            case ASSIGN      -> "=";
            case EQ          -> "==";
            case NE          -> "!=";
            case LT          -> "<";
            case GT          -> ">";
            case LE          -> "<=";
            case GE          -> ">=";
            case ADD_ASSIGN  -> "+=";
            case SUB_ASSIGN  -> "-=";
            case MUL_ASSIGN  -> "*=";
            case DIV_ASSIGN  -> "/=";
            case MOD_ASSIGN  -> "%=";
            case SHL_ASSIGN  -> "<<=";
            case SHR_ASSIGN  -> ">>=";
            case AND_ASSIGN  -> "&=";
            case OR_ASSIGN   -> "|=";
            case XOR_ASSIGN  -> "^=";
            case INC         -> "++";
            case DEC         -> "--";
            case COMMA       -> ",";
        };
    }

    public boolean isEquality() {
        return this == EQ || this == NE;
    }

    /**
     * Defines the set of relational (comparison) operators:
     * <  <=  >  >=
     */
    public boolean isRelational() {
        return switch (this) {
            case LT,
                 GT,
                 LE,
                 GE -> true;
            default -> false;
        };
    }

    /**
     * Returns true if op is '=' or any compound assignment operator ('+=', '-=', etc.)
     */
    public boolean isAssignment() {
        return switch (this) {
            case ASSIGN,
                 ADD_ASSIGN,
                 SUB_ASSIGN,
                 MUL_ASSIGN,
                 DIV_ASSIGN,
                 MOD_ASSIGN,
                 SHL_ASSIGN,
                 SHR_ASSIGN,
                 AND_ASSIGN,
                 OR_ASSIGN,
                 XOR_ASSIGN -> true;
            default -> false;
        };
    }

    /**
     * Given a compound assignment operator, returns the non-assignment version of the operator
     * (e.g. '+=' becomes '+')
     */
    public Operator removeAssignment() {
        return switch (this) {
            case ADD_ASSIGN -> ADD;
            case SUB_ASSIGN -> SUB;
            case MUL_ASSIGN -> MUL;
            case DIV_ASSIGN -> DIV;
            case MOD_ASSIGN -> MOD;
            case SHL_ASSIGN -> SHL;
            case SHR_ASSIGN -> SHR;
            case AND_ASSIGN -> BITWISE_AND;
            case OR_ASSIGN  -> BITWISE_OR;
            case XOR_ASSIGN -> BITWISE_XOR;
            default -> this;
        };
    }

    /**
     * Defines the set of operators which are only valid on integral types:
     * %  %=  <<  <<=  >>  >>=  ~  &  &=  |  |=  ^  ^=
     */
    public boolean isOnlyValidForIntegers() {
        return switch (this) {
            case MOD,
                 SHL,
                 SHR,
                 BITWISE_NOT, // Google forgot this
                 BITWISE_AND,
                 BITWISE_OR,
                 BITWISE_XOR,
                 MOD_ASSIGN,
                 SHL_ASSIGN,
                 SHR_ASSIGN,
                 AND_ASSIGN,
                 OR_ASSIGN,
                 XOR_ASSIGN -> true;
            default -> false;
        };
    }

    /**
     * Defines the set of operators which perform vector/matrix math.
     * +  +=  -  -=  *  *=  /  /=  %  %=  <<  <<=  >>  >>=  &  &=  |  |=  ^  ^=
     */
    public boolean isValidForVectorOrMatrix() {
        return switch (this) {
            case ADD,
                 SUB,
                 MUL,
                 DIV,
                 MOD,
                 SHL,
                 SHR,
                 BITWISE_AND,
                 BITWISE_OR,
                 BITWISE_XOR,
                 ADD_ASSIGN,
                 SUB_ASSIGN,
                 MUL_ASSIGN,
                 DIV_ASSIGN,
                 MOD_ASSIGN,
                 SHL_ASSIGN,
                 SHR_ASSIGN,
                 AND_ASSIGN,
                 OR_ASSIGN,
                 XOR_ASSIGN -> true;
            default -> false;
        };
    }

    private boolean isMatrixMultiply(Type left, Type right) {
        if (this != MUL && this != MUL_ASSIGN) {
            return false;
        }
        // M * M, M * V, V * M
        if (left.isMatrix()) {
            return right.isMatrix() || right.isVector();
        }
        return left.isVector() && right.isMatrix();
    }

    /**
     * Determines the operand and result types of a binary expression. Returns true if the
     * expression is legal, false otherwise. If false, the values of the out parameters are
     * undefined.
     *
     * @param out left type, right type and result type, respectively
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean determineBinaryType(Type left,
                                       Type right,
                                       Type[] out) {
        assert (out.length >= 3);
        ThreadContext context = ThreadContext.getInstance();
        switch (this) {
            case ASSIGN -> {    // left = right
                if (left.isVoid()) {
                    return false;
                }
                out[0] = left;
                out[1] = left;
                out[2] = left;
                return right.canCoerceTo(left);
            }
            case EQ,            // left == right
                 NE -> {        // left != right
                if (left.isVoid() || left.isOpaque()) {
                    return false;
                }
                long rightToLeft = right.getCoercionCost(left),
                     leftToRight = left.getCoercionCost(right);
                if (Type.CoercionCost.compare(rightToLeft, leftToRight) < 0) {
                    if (Type.CoercionCost.isPossible(rightToLeft)) {
                        out[0] = left;
                        out[1] = left;
                        out[2] = context.getTypes().mBool;
                        return true;
                    }
                } else {
                    if (Type.CoercionCost.isPossible(leftToRight)) {
                        out[0] = right;
                        out[1] = right;
                        out[2] = context.getTypes().mBool;
                        return true;
                    }
                }
                return false;
            }
            case LOGICAL_AND,       // left && right
                 LOGICAL_OR,        // left || right
                 LOGICAL_XOR -> {   // left ^^ right
                out[0] = context.getTypes().mBool;
                out[1] = context.getTypes().mBool;
                out[2] = context.getTypes().mBool;
                return left.canCoerceTo(context.getTypes().mBool) &&
                       right.canCoerceTo(context.getTypes().mBool);
            }
            case COMMA -> {         // left, right
                if (left.isOpaque() || right.isOpaque()) {
                    return false;
                }
                out[0] = left;
                out[1] = right;
                out[2] = right;
                return true;
            }
        }

        // Boolean types only support the operators listed above (, = == != || && ^^).
        // If we've gotten this far with a boolean, we have an unsupported operator.
        final Type leftComponentType = left.getComponentType();
        final Type rightComponentType = right.getComponentType();
        if (leftComponentType.isBoolean() || rightComponentType.isBoolean()) {
            return false;
        }

        boolean isAssignment = isAssignment();
        if (isMatrixMultiply(left, right)) {  // left * right
            // Determine final component type.
            if (!determineBinaryType(leftComponentType, rightComponentType, out)) {
                return false;
            }
            int leftCols = left.getCols(), leftRows = left.getRows();
            int rightCols = right.getCols(), rightRows = right.getRows();
            // Convert component type to compound.
            out[0] = out[2].toCompound(leftCols, leftRows);
            out[1] = out[2].toCompound(rightCols, rightRows);
            if (left.isVector()) {
                // `vector * matrix` treats the vector as a row vector; we need to transpose it.
                int t = leftCols;
                leftCols = leftRows;
                leftRows = t;
                assert (leftRows == 1);
            }
            if (leftRows > 1) {
                out[2] = out[2].toCompound(rightCols, leftRows);
            } else {
                // The result was a row vector. Transpose it back to a column.
                out[2] = out[2].toCompound(leftRows, rightCols);
            }
            if (isAssignment && (out[2].getCols() != leftCols ||
                                 out[2].getRows() != leftRows)) {
                return false;
            }
            return rightRows == leftCols;
        }

        boolean leftIsVectorOrMatrix = (left.isVector() || left.isMatrix())
                && isValidForVectorOrMatrix();

        if (leftIsVectorOrMatrix && right.isScalar()) {
            // Determine final component type.
            if (!determineBinaryType(leftComponentType, right, out)) {
                return false;
            }
            // Convert component type to compound.
            out[0] = out[0].toCompound(left.getCols(), left.getRows());
            assert !isRelational();
            out[2] = out[2].toCompound(left.getCols(), left.getRows());
            return true;
        }

        boolean rightIsVectorOrMatrix = (right.isVector() || right.isMatrix())
                && isValidForVectorOrMatrix();

        if (!isAssignment && rightIsVectorOrMatrix && left.isScalar()) {
            // Determine final component type.
            if (!determineBinaryType(left, rightComponentType, out)) {
                return false;
            }
            // Convert component type to compound.
            out[1] = out[1].toCompound(right.getCols(), right.getRows());
            assert !isRelational();
            out[2] = out[2].toCompound(right.getCols(), right.getRows());
            return true;
        }

        long rightToLeftCost = right.getCoercionCost(left);
        long leftToRightCost = isAssignment ? Type.CoercionCost.impossible()
                                            : left.getCoercionCost(right);

        if ((left.isScalar() && right.isScalar()) || leftIsVectorOrMatrix) {
            if (isOnlyValidForIntegers()) {
                if (!leftComponentType.isInteger() || !rightComponentType.isInteger()) {
                    return false;
                }
            }
            if (Type.CoercionCost.isPossible(rightToLeftCost) &&
                    Type.CoercionCost.compare(rightToLeftCost, leftToRightCost) < 0) {
                // Right-to-Left conversion is possible and cheaper
                out[0] = left;
                out[1] = left;
                out[2] = left;
            } else if (Type.CoercionCost.isPossible(leftToRightCost)) {
                // Left-to-Right conversion is possible (and at least as cheap as Right-to-Left)
                out[0] = right;
                out[1] = right;
                out[2] = right;
            } else {
                return false;
            }
            if (isRelational()) {
                out[2] = context.getTypes().mBool;
            }
            return true;
        }
        return false;
    }
}
