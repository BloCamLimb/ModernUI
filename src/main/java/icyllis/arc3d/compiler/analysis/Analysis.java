/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.analysis;

import icyllis.arc3d.compiler.Modifiers;
import icyllis.arc3d.compiler.Operator;
import icyllis.arc3d.compiler.tree.*;

import java.util.Arrays;

public final class Analysis {

    /**
     * Determines if `expr` is a compile-time constant (composed of just constructors and literals).
     */
    public static boolean isCompileTimeConstant(Expression expr) {
        class IsCompileTimeConstantVisitor extends NodeVisitor {
            @Override
            public boolean visitLiteral(Literal expr) {
                // Literals are compile-time constants.
                return false;
            }

            @Override
            protected boolean visitExpression(Expression expr) {
                return switch (expr.getKind()) {
                    case CONSTRUCTOR_ARRAY,
                            CONSTRUCTOR_COMPOUND,
                            CONSTRUCTOR_MATRIX_MATRIX,
                            CONSTRUCTOR_MATRIX_SCALAR,
                            CONSTRUCTOR_STRUCT,
                            CONSTRUCTOR_VECTOR_SCALAR ->
                        // Constructors might be compile-time constants.
                            super.visitExpression(expr);
                    // This expression isn't a compile-time constant.
                    default -> true;
                };
            }
        }
        IsCompileTimeConstantVisitor visitor = new IsCompileTimeConstantVisitor();
        return !expr.accept(visitor);
    }

    public static boolean updateVariableRefKind(Expression expr, int refKind) {
        return true;
    }

    /**
     * Returns true if both expression trees are the same. Used by the optimizer to look for
     * self-assignment or self-comparison; won't necessarily catch complex cases. Rejects
     * expressions that may cause side effects.
     */
    public static boolean isSameExpressionTree(Expression left, Expression right) {
        if (left.getKind() != right.getKind() ||
                !left.getType().matches(right.getType())) {
            return false;
        }

        // This isn't a fully exhaustive list of expressions by any stretch of the imagination; for
        // instance, `x[y+1] = x[y+1]` isn't detected because we don't look at BinaryExpressions.
        // Since this is intended to be used for optimization purposes, handling the common cases is
        // sufficient.
        switch (left.getKind()) {
            case LITERAL:
                return ((Literal) left).getValue() == ((Literal) right).getValue();

            case CONSTRUCTOR_ARRAY:
            case CONSTRUCTOR_ARRAY_CAST:
            case CONSTRUCTOR_COMPOUND:
            case CONSTRUCTOR_COMPOUND_CAST:
            case CONSTRUCTOR_MATRIX_MATRIX:
            case CONSTRUCTOR_MATRIX_SCALAR:
            case CONSTRUCTOR_SCALAR_CAST:
            case CONSTRUCTOR_STRUCT:
            case CONSTRUCTOR_VECTOR_SCALAR: {
                if (left.getKind() != right.getKind()) {
                    return false;
                }
                Expression[] lhsArgs = ((ConstructorCall) left).getArguments();
                Expression[] rhsArgs = ((ConstructorCall) right).getArguments();
                if (lhsArgs.length != rhsArgs.length) {
                    return false;
                }
                for (int i = 0; i < lhsArgs.length; ++i) {
                    if (!isSameExpressionTree(lhsArgs[i], rhsArgs[i])) {
                        return false;
                    }
                }
                return true;
            }
            case FIELD_ACCESS: {
                var leftExpr = (FieldExpression) left;
                var rightExpr = (FieldExpression) right;
                return leftExpr.getFieldIndex() == rightExpr.getFieldIndex() &&
                        isSameExpressionTree(leftExpr.getBase(), rightExpr.getBase());
            }

            /*case INDEX: {
                var leftExpr = ( ArrayExpression) left;
                var rightExpr = (ArrayExpression) right;
                return IsSameExpressionTree( * left.as < IndexExpression > ().index(),
                                        *right.as<IndexExpression> ().index()) &&
                IsSameExpressionTree( * left.as < IndexExpression > ().base(),
                                        *right.as<IndexExpression> ().base());
            }*/

            case PREFIX: {
                var leftExpr = (PrefixExpression) left;
                var rightExpr = (PrefixExpression) right;
                return (leftExpr.getOperator() == rightExpr.getOperator()) &&
                        isSameExpressionTree(leftExpr.getOperand(), rightExpr.getOperand());
            }

            case SWIZZLE: {
                var leftExpr = (Swizzle) left;
                var rightExpr = (Swizzle) right;
                return Arrays.equals(leftExpr.getComponents(), rightExpr.getComponents()) &&
                        isSameExpressionTree(leftExpr.getBase(), rightExpr.getBase());
            }

            case VARIABLE_REFERENCE:
                return ((VariableReference) left).getVariable() ==
                        ((VariableReference) right).getVariable();

            default:
                return false;
        }
    }

    /**
     * Determines if `expr` has any side effects. (Is the expression state-altering or pure?)
     */
    public static boolean hasSideEffects(Expression expr) {
        class HasSideEffectsVisitor extends NodeVisitor {
            @Override
            public boolean visitFunctionCall(FunctionCall expr) {
                return (expr.getFunction().getModifiers().flags() & Modifiers.kPure_Flag) == 0;
            }

            @Override
            public boolean visitPrefix(PrefixExpression expr) {
                return expr.getOperator() == Operator.INC ||
                        expr.getOperator() == Operator.DEC;
            }

            @Override
            public boolean visitPostfix(PostfixExpression expr) {
                return expr.getOperator() == Operator.INC ||
                        expr.getOperator() == Operator.DEC;
            }

            @Override
            public boolean visitBinary(BinaryExpression expr) {
                return expr.getOperator().isAssignment();
            }
        }
        return expr.accept(new HasSideEffectsVisitor());
    }
}
