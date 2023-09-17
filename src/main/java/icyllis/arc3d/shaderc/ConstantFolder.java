/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.shaderc;

import icyllis.arc3d.shaderc.analysis.Analysis;
import icyllis.arc3d.shaderc.tree.*;

import javax.annotation.Nullable;
import java.util.OptionalDouble;
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

        // If the left side is a Boolean literal, apply short-circuit optimizations.
        if (left.isBooleanLiteral()) {
            boolean leftVal = ((Literal) left).getBooleanValue();

            // When the literal is on the left, we can sometimes eliminate the other expression entirely.
            if ((op == Operator.LOGICAL_AND && !leftVal) ||  // (false && expr) -> (false)
                (op == Operator.LOGICAL_OR  && leftVal)) {   // (true  || expr) -> (true)
                return left.clone(pos);
            }

            // We can't eliminate the right-side expression via short-circuit, but we might still be able to
            // simplify away a no-op expression.

            // Detect no-op Boolean expressions and optimize them away.
            if ((op == Operator.LOGICAL_AND)             ||  // (expr && true)  -> (expr)
                (op == Operator.LOGICAL_OR)              ||  // (expr || false) -> (expr)
                (op == Operator.LOGICAL_XOR && !leftVal) ||  // (expr ^^ false) -> (expr)
                (op == Operator.EQ          && leftVal)  ||  // (expr == true)  -> (expr)
                (op == Operator.NE          && !leftVal)) {  // (expr != false) -> (expr)

                return right.clone(pos);
            }

            return null;
        }

        // If the right side is a Boolean literal...
        if (right.isBooleanLiteral()) {
            boolean rightVal = ((Literal) right).getBooleanValue();
            // ... and the left side has no side effects...
            if (!Analysis.hasSideEffects(left)) {
                // We can reverse the expressions and short-circuit optimizations are still valid.

                // When the literal is on the left, we can sometimes eliminate the other expression entirely.
                if ((op == Operator.LOGICAL_AND && !rightVal) ||  // (false && expr) -> (false)
                    (op == Operator.LOGICAL_OR  && rightVal)) {   // (true  || expr) -> (true)

                    return right.clone(pos);
                }
            }

            // Detect no-op Boolean expressions and optimize them away.
            if ((op == Operator.LOGICAL_AND && rightVal)  ||  // (expr && true)  -> (expr)
                (op == Operator.LOGICAL_OR  && !rightVal) ||  // (expr || false) -> (expr)
                (op == Operator.LOGICAL_XOR && !rightVal) ||  // (expr ^^ false) -> (expr)
                (op == Operator.EQ          && rightVal)  ||  // (expr == true)  -> (expr)
                (op == Operator.NE          && !rightVal)) {  // (expr != false) -> (expr)

                return left.clone(pos);
            }

            return null;
        }

        if (op == Operator.EQ && Analysis.isSameExpressionTree(left, right)) {
            // With == comparison, if both sides are the same trivial expression, this is
            // self-comparison and is always true. (We are not concerned with NaN.)
            return Literal.makeBoolean(pos, /*value=*/true);
        }

        if (op == Operator.NE && Analysis.isSameExpressionTree(left, right)) {
            // With != comparison, if both sides are the same trivial expression, this is
            // self-comparison and is always false. (We are not concerned with NaN.)
            return Literal.makeBoolean(pos, /*value=*/false);
        }

        switch (op) {
            case DIV, DIV_ASSIGN, MOD, MOD_ASSIGN -> {
                int components = right.getType().getComponents();
                for (int i = 0; i < components; ++i) {
                    OptionalDouble value = right.getConstantValue(i);
                    if (value.isPresent() && value.getAsDouble() == 0.0) {
                        ThreadContext.getInstance().error(pos, "division by zero");
                        return null;
                    }
                }
            }
        }

        return null;
    }
}
