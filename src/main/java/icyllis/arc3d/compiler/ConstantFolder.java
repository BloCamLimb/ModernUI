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

import icyllis.arc3d.compiler.analysis.Analysis;
import icyllis.arc3d.compiler.tree.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.DoubleUnaryOperator;

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
            if (!variable.getModifiers().isConst()) {
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

    //FIXME signed/unsigned arithmetic

    /**
     * Simplifies the binary expression `left OP right`. Returns null if it can't be simplified.
     */
    @Nullable
    public static Expression fold(@NonNull Context context, int pos,
                                  Expression left, Operator op,
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
            return Literal.makeBoolean(context, pos, result);
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
            return Literal.makeBoolean(context, pos, /*value=*/true);
        }

        if (op == Operator.NE && Analysis.isSameExpressionTree(left, right)) {
            // With != comparison, if both sides are the same trivial expression, this is
            // self-comparison and is always false. (We are not concerned with NaN.)
            return Literal.makeBoolean(context, pos, /*value=*/false);
        }

        Type leftType = left.getType();
        Type rightType = right.getType();

        switch (op) {
            case DIV, DIV_ASSIGN, MOD, MOD_ASSIGN -> {
                int components = rightType.getComponents();
                for (int i = 0; i < components; ++i) {
                    OptionalDouble value = right.getConstantValue(i);
                    if (value.isPresent() && value.getAsDouble() == 0.0) {
                        context.error(pos, "division by zero");
                        return null;
                    }
                }
            }
        }

        // Perform full constant folding when both sides are compile-time constants.
        boolean leftSideIsConstant = Analysis.isCompileTimeConstant(left);
        boolean rightSideIsConstant = Analysis.isCompileTimeConstant(right);

        if (leftSideIsConstant && rightSideIsConstant) {
            // Handle pairs of integer literals.
            if (left.isIntLiteral() && right.isIntLiteral()) {
                long leftVal = ((Literal) left).getIntegerValue();
                long rightVal = ((Literal) right).getIntegerValue();

                final long resultVal;
                switch (op) {
                    case ADD -> resultVal = leftVal + rightVal;
                    case SUB -> resultVal = leftVal - rightVal;
                    case MUL -> resultVal = leftVal * rightVal;
                    case DIV -> resultVal = leftVal / rightVal;
                    case MOD -> resultVal = leftVal % rightVal;
                    case BITWISE_AND -> resultVal = leftVal & rightVal;
                    case BITWISE_OR  -> resultVal = leftVal | rightVal;
                    case BITWISE_XOR -> resultVal = leftVal ^ rightVal;
                    case EQ -> resultVal = leftVal == rightVal ? 1 : 0;
                    case NE -> resultVal = leftVal != rightVal ? 1 : 0;
                    case GT -> resultVal = leftVal > rightVal ? 1 : 0;
                    case GE -> resultVal = leftVal >= rightVal ? 1 : 0;
                    case LT -> resultVal = leftVal < rightVal ? 1 : 0;
                    case LE -> resultVal = leftVal <= rightVal ? 1 : 0;
                    case SHL -> {
                        if (rightVal >= 0 && rightVal < leftType.getWidth()) {
                            resultVal = leftVal << rightVal;
                        } else {
                            // undefined behavior in GLSL
                            context.warning(pos, "shift value out of range");
                            return null;
                        }
                    }
                    case SHR -> {
                        if (rightVal >= 0 && rightVal < leftType.getWidth()) {
                            resultVal = leftVal >> rightVal;
                        } else {
                            // undefined behavior in GLSL
                            context.warning(pos, "shift value out of range");
                            return null;
                        }
                    }
                    default -> {
                        return null;
                    }
                }
                return makeConstant(pos, resultVal, resultType);
            }
        }

        return null;
    }

    @Nullable
    private static Expression makeConstant(int pos, double result, Type resultType) {
        if (resultType.isNumeric()) {
            // use !(a && b) to catch NaN values
            if (!(result >= resultType.getMinValue() && result <= resultType.getMaxValue())) {
                // The value is outside the range or is NaN (all if-checks fail); do not optimize.
                return null;
            }
        }
        return Literal.make(pos, result, resultType);
    }

    /**
     * Simplifies the unary expression `OP base`. Returns null if it can't be simplified.
     */
    @Nullable
    public static Expression fold(@NonNull Context context, int pos,
                                  @NonNull Operator op, Expression base) {
        // Replace constant variables with their literal values.
        Expression value = getConstantValueForVariable(base);
        Type type = value.getType();
        switch (op) {
            case ADD -> { // positive
                assert (!type.isArray());
                assert (type.getComponentType().isNumeric());
                value.mPosition = pos;
                return value;
            }
            case SUB -> { // negative
                assert (!type.isArray());
                assert (type.getComponentType().isNumeric());
                return fold_negation(context, pos, value);
            }
            case LOGICAL_NOT -> {
                assert (type.isBoolean());
                switch (value.getKind()) {
                    case LITERAL -> {
                        // Convert !true to false, !false to true.
                        Literal literal = (Literal) value;
                        return Literal.makeBoolean(pos, !literal.getBooleanValue(), value.getType());
                    }
                    case PREFIX -> {
                        // Convert `!(!expression)` into `expression`.
                        PrefixExpression prefix = (PrefixExpression) value;
                        if (prefix.getOperator() == Operator.LOGICAL_NOT) {
                            prefix.getOperand().mPosition = pos;
                            return prefix.getOperand();
                        }
                    }
                    // binary expression cannot be folded
                }
            }
            case BITWISE_NOT -> {
                assert (!type.isArray());
                assert (type.getComponentType().isInteger());
                switch (value.getKind()) {
                    case LITERAL, CONSTRUCTOR_VECTOR_SPLAT, CONSTRUCTOR_COMPOUND -> {
                        // Convert ~vecN(1, 2, ...) to vecN(~1, ~2, ...).
                        Expression expr = apply_to_components(context, pos, value, v -> ~(long) v);
                        if (expr != null) {
                            return expr;
                        }
                    }
                    case PREFIX -> {
                        // Convert `~(~expression)` into `expression`.
                        PrefixExpression prefix = (PrefixExpression) value;
                        if (prefix.getOperator() == Operator.BITWISE_NOT) {
                            prefix.getOperand().mPosition = pos;
                            return prefix.getOperand();
                        }
                    }
                }
            }
            case INC, DEC -> {
                assert (type.isNumeric());
            }
            default -> throw new AssertionError(op);
        }
        return null;
    }

    @Nullable
    private static Expression fold_negation(Context context,
                                            int pos,
                                            Expression base) {
        Expression value = getConstantValueForVariable(base);
        switch (value.getKind()) {
            case LITERAL, CONSTRUCTOR_VECTOR_SPLAT, CONSTRUCTOR_COMPOUND -> {
                // Convert `-vecN(literal, ...)` into `vecN(-literal, ...)`.
                Expression expr = apply_to_components(context, pos, value, v -> -v);
                if (expr != null) {
                    return expr;
                }
            }
            case PREFIX -> {
                // Convert `-(-expression)` into `expression`.
                PrefixExpression prefix = (PrefixExpression) value;
                if (prefix.getOperator() == Operator.SUB) {
                    return prefix.getOperand().clone(pos);
                }
            }
            case CONSTRUCTOR_ARRAY -> {
                // Convert `-array[N](literal, ...)` into `array[N](-literal, ...)`.
                if (Analysis.isCompileTimeConstant(value)) {
                    ConstructorArray ctor = (ConstructorArray) value;
                    Expression[] ctorArgs = ctor.getArguments();
                    Expression[] newArgs = new Expression[ctorArgs.length];
                    for (int i = 0; i < ctorArgs.length; i++) {
                        Expression arg = ctorArgs[i];
                        Expression folded = fold_negation(context, pos, arg);
                        if (folded == null) {
                            folded = new PrefixExpression(pos, Operator.SUB, arg.clone());
                        }
                        newArgs[i] = folded;
                    }
                    return ConstructorArray.make(pos, ctor.getType(), newArgs);
                }
            }
            case CONSTRUCTOR_DIAGONAL_MATRIX ->  {
                // Convert `-matrix(literal)` into `matrix(-literal)`.
                if (Analysis.isCompileTimeConstant(value)) {
                    ConstructorDiagonalMatrix ctor = (ConstructorDiagonalMatrix) value;
                    Expression folded = fold_negation(context, pos, ctor.getArgument());
                    if (folded != null) {
                        return ConstructorDiagonalMatrix.make(pos, ctor.getType(),
                                folded);
                    }
                }
            }
        }
        return null;
    }

    private static Expression apply_to_components(Context context,
                                                  int pos,
                                                  Expression expr,
                                                  DoubleUnaryOperator op) {
        int components = expr.getType().getComponents();
        if (components > 16) {
            // The expression has more slots than we expected.
            return null;
        }
        double[] values = new double[components];
        Type componentType = expr.getType().getComponentType();

        for (int index = 0; index < components; ++index) {
            OptionalDouble value = expr.getConstantValue(index);
            if (value.isPresent()) {
                values[index] = op.applyAsDouble(value.getAsDouble());
                if (componentType.checkLiteralOutOfRange(context, pos, values[index])) {
                    // We can't simplify the expression if the new value is out-of-range for the type.
                    return null;
                }
            } else {
                // There's a non-constant element; we can't simplify this expression.
                return null;
            }
        }
        return ConstructorCompound.makeFromConstants(context, pos, expr.getType(), values);
    }
}
