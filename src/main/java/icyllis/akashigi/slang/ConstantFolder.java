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

import icyllis.akashigi.slang.analysis.Analysis;
import icyllis.akashigi.slang.tree.*;

import javax.annotation.Nullable;
import java.util.OptionalLong;

/**
 * Performs constant folding on AST expressions. This simplifies expressions containing
 * compile-time constants, such as replacing `Literal(2) + Literal(2)` with `Literal(4)`.
 */
public class ConstantFolder {

    /**
     * Returns the value if it is an int literal or const int variable with a known value.
     */
    public static OptionalLong getConstantInt(Expression value) {
        Expression expr = getConstantValueForVariable(value);
        if (expr.isIntLiteral()) {
            return OptionalLong.of(((Literal) expr).getIntegerValue());
        }
        return OptionalLong.empty();
    }

    /**
     * If the expression is a const variable with a known compile-time constant value, returns that
     * value. If not, returns the original expression as-is.
     */
    public static Expression getConstantValueForVariable(Expression value) {
        Expression expr = getConstantValueOrNullForVariable(value);
        return expr != null ? expr : value;
    }

    /**
     * If the expression is a const variable with a known compile-time constant value, returns a
     * clone of that value. If not, returns the original expression as-is.
     */
    public static Expression makeConstantValueForVariable(int pos, Expression value) {
        Expression expr = getConstantValueOrNullForVariable(value);
        return expr != null ? expr.clone(pos) : value;
    }

    /**
     * If the expression is a const variable with a known compile-time constant value, returns that
     * value. If not, returns null.
     */
    @Nullable
    public static Expression getConstantValueOrNullForVariable(Expression value) {
        for (;;) {
            if (!(value instanceof VariableReference r) ||
                    r.getReferenceKind() != VariableReference.kRead_ReferenceKind) {
                break;
            }
            Variable variable = r.getVariable();
            if ((variable.getModifiers().flags() & Modifiers.kConst_Flag) == 0) {
                break;
            }
            value = variable.initialValue();
            if (value == null) {
                // Function parameters can be const but won't have an initial value.
                break;
            }
            if (Analysis.isCompileTimeConstant(value)) {
                return value;
            }
        }
        // We didn't find a compile-time constant at the end.
        return null;
    }

    /**
     * Simplifies the binary expression `left OP right`. Returns null if it can't be simplified.
     */
    @Nullable
    public static Expression fold(int pos, Expression left, Operator op,
                                  Expression right, Type resultType) {
        // Replace constant variables with their literal values.
        left = getConstantValueForVariable(left);
        right = getConstantValueForVariable(right);

        // If this is the assignment operator, and both sides are the same trivial expression, this is
        // self-assignment (i.e., `var = var`) and can be reduced to just a variable reference (`var`).
        // This can happen when other parts of the assignment are optimized away.
        if (op == Operator.ASSIGN && Analysis.isSameExpressionTree(left, right)) {
            return right.clone(pos);
        }

        // Simplify the expression when both sides are constant Boolean literals.
        if (left.isBooleanLiteral() && right.isBooleanLiteral()) {
            boolean leftVal  = ((Literal) left ).getBooleanValue();
            boolean rightVal = ((Literal) right).getBooleanValue();
            boolean result;
            switch (op) {
                case LOGICAL_AND: result = leftVal &  rightVal; break;
                case LOGICAL_OR:  result = leftVal |  rightVal; break;
                case LOGICAL_XOR: result = leftVal ^  rightVal; break;
                case EQ:          result = leftVal == rightVal; break;
                case NE:          result = leftVal != rightVal; break;
                default: return null;
            }
            return Literal.makeBoolean(pos, result);
        }

        return null;
    }
}
